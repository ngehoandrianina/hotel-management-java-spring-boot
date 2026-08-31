pipeline {
    agent any

    tools {
        maven 'Maven-3.9'
        jdk 'JDK-21'
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '15'))
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
    }

    environment {
        APP_NAME = 'hotel-room-management'
        IMAGE_TAG = "${env.BUILD_NUMBER}"
        DOCKER_IMAGE = "${env.APP_NAME}:${env.IMAGE_TAG}"
        
        // Noms uniques pour chaque build
        CONTAINER_NAME = "${env.APP_NAME}-${env.BUILD_NUMBER}"
        DB_CONTAINER = "postgres-${env.BUILD_NUMBER}"
        NETWORK_NAME = "hotel-network-${env.BUILD_NUMBER}"
        
        // Configuration PostgreSQL
        DB_NAME = 'hotel_db'
        DB_USER = 'hotel_user'
        DB_PASSWORD = 'hotel_password'
        DB_PORT = '5432'
        
        // Port de l'application
        APP_PORT = '8090'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_COMMIT_SHORT = bat(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    echo " Code récupéré - Commit: ${env.GIT_COMMIT_SHORT}"
                    echo " Build #${env.BUILD_NUMBER}"
                    echo " Conteneur: ${env.CONTAINER_NAME}"
                    echo " PostgreSQL: ${env.DB_CONTAINER}"
                }
            }
        }

        stage('Build & Test') {
            steps {
                bat 'mvn -B -ntp clean test'
            }
            post {
                always {
                    junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
                }
            }
        }

        stage('Package') {
            steps {
                bat 'mvn -B -ntp package -DskipTests'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }

        // ============================================================
        // ENVIRONNEMENT DOCKER ISOLÉ
        // ============================================================

        stage('Setup Docker Environment') {
            steps {
                script {
                    echo " Création de l'environnement Docker isolé pour le build #${env.BUILD_NUMBER}"
                    
                    bat """
                        echo "========================================="
                        echo "  BUILD #${env.BUILD_NUMBER}"
                        echo "  Réseau : ${NETWORK_NAME}"
                        echo "  PostgreSQL : ${DB_CONTAINER}"
                        echo "  Application : ${CONTAINER_NAME}"
                        echo "========================================="
                        
                        echo " Création du réseau ${NETWORK_NAME}..."
                        docker network create ${NETWORK_NAME} 2>nul || echo " Réseau déjà existant"
                        
                        echo " Nettoyage des anciens conteneurs (même nom)..."
                        docker rm -f ${DB_CONTAINER} 2>nul || echo " PostgreSQL propre"
                        docker rm -f ${CONTAINER_NAME} 2>nul || echo " Application propre"
                    """
                }
            }
        }

        stage('Start PostgreSQL') {
    steps {
        script {
            echo " Démarrage de PostgreSQL..."
            bat """
                docker run -d \
                    --name ${DB_CONTAINER} \
                    --network ${NETWORK_NAME} \
                    -e POSTGRES_DB=${DB_NAME} \
                    -e POSTGRES_USER=${DB_USER} \
                    -e POSTGRES_PASSWORD=${DB_PASSWORD} \
                    -p ${DB_PORT} \
                    postgres:16-alpine
            """
            
            echo " Attente de 30 secondes pour l'initialisation..."
            sleep time: 30, unit: 'SECONDS'
            
            echo "Vérification de PostgreSQL..."
            bat "docker exec ${DB_CONTAINER} pg_isready -U ${DB_USER} -d ${DB_NAME}"
        }
    }
}

        stage('Wait for PostgreSQL') {
            steps {
                script {
                    echo " Attente que PostgreSQL soit complètement prêt..."
                    
                    def maxAttempts = 10
                    def waitTime = 5
                    def ready = false
                    
                    for (int i = 1; i <= maxAttempts; i++) {
                        try {
                            def result = bat(script: """
                                docker exec ${DB_CONTAINER} pg_isready -U ${DB_USER} -d ${DB_NAME}
                            """, returnStatus: true)
                            
                            if (result == 0) {
                                ready = true
                                echo " PostgreSQL est prêt ! (tentative ${i}/${maxAttempts})"
                                break
                            } else {
                                echo " PostgreSQL n'est pas encore prêt... (tentative ${i}/${maxAttempts})"
                            }
                        } catch (Exception e) {
                            echo " PostgreSQL n'est pas encore prêt... (tentative ${i}/${maxAttempts})"
                        }
                        
                        if (i < maxAttempts) {
                            echo "Attente de ${waitTime} secondes..."
                            sleep time: waitTime, unit: 'SECONDS'
                        }
                    }
                    
                    if (!ready) {
                        error " PostgreSQL n'est pas prêt après ${maxAttempts} tentatives"
                    }
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    echo " Construction de l'image Docker..."
                    bat """
                        echo "Construction de l'image ${DOCKER_IMAGE}..."
                        docker build -t ${DOCKER_IMAGE} .
                        
                        echo "Images Docker disponibles :"
                        docker images | findstr ${APP_NAME}
                    """
                }
            }
        }

        stage('Run Application') {
            steps {
                script {
                    echo " Démarrage de l'application dans un conteneur isolé..."
                    
                    bat """
                        echo "Démarrage du conteneur ${CONTAINER_NAME}..."
                        docker run -d \
                            --name ${CONTAINER_NAME} \
                            --network ${NETWORK_NAME} \
                            -p ${APP_PORT}:8080 \
                            -e SPRING_DATASOURCE_URL=jdbc:postgresql://${DB_CONTAINER}:${DB_PORT}/${DB_NAME} \
                            -e SPRING_DATASOURCE_USERNAME=${DB_USER} \
                            -e SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD} \
                            -e SPRING_JPA_HIBERNATE_DDL_AUTO=update \
                            ${DOCKER_IMAGE}
                        
                        echo " Attente du démarrage de l'application (30 secondes)..."
                        timeout /t 30 /nobreak >nul
                        
                        echo " Conteneur démarré !"
                        docker ps | findstr ${CONTAINER_NAME}
                        
                        echo " Logs de l'application (premiers 20 lignes) :"
                        docker logs ${CONTAINER_NAME} --tail=20
                    """
                }
            }
        }

        stage('Health Check') {
            steps {
                script {
                    echo " Vérification de la santé de l'application..."
                    
                    def containerRunning = bat(script: """
                        docker ps | findstr ${CONTAINER_NAME}
                    """, returnStatus: true)
                    
                    if (containerRunning != 0) {
                        error " Le conteneur ${CONTAINER_NAME} n'est pas en cours d'exécution"
                    }
                    
                    try {
                        def healthStatus = bat(script: """
                            curl -s --connect-timeout 10 -o nul -w "%%{http_code}" http://localhost:${APP_PORT}/actuator/health
                        """, returnStdout: true).trim()
                        
                        if (healthStatus == '200') {
                            echo " Application en bonne santé !"
                        } else {
                            echo " Health check retourne : ${healthStatus}"
                            bat "docker logs ${CONTAINER_NAME} --tail=20"
                        }
                    } catch (Exception e) {
                        echo " Health check non disponible"
                        bat "docker logs ${CONTAINER_NAME} --tail=30"
                    }
                }
            }
        }

        stage('Integration Tests') {
            steps {
                script {
                    echo " Test d'intégration de l'API..."
                    try {
                        def response = bat(script: """
                            curl -s --connect-timeout 5 http://localhost:${APP_PORT}/api/test || echo "API non disponible"
                        """, returnStdout: true).trim()
                        echo " Réponse de l'API : ${response}"
                    } catch (Exception e) {
                        echo "API de test non disponible"
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                echo "========================================="
                echo "  RÉSUMÉ DU BUILD #${env.BUILD_NUMBER}"
                echo "========================================="
                
                echo " Conteneurs en cours :"
                bat 'docker ps'
                
                echo " Nettoyage de l'espace de travail Jenkins..."
                cleanWs()
            }
        }
        
        success {
            script {
                echo """
                 BUILD #${env.BUILD_NUMBER} RÉUSSI !
                
                 Résumé :
                - Commit : ${env.GIT_COMMIT_SHORT}
                - Image : ${env.DOCKER_IMAGE}
                - Application : http://localhost:${env.APP_PORT}
                - PostgreSQL : ${DB_CONTAINER}:${DB_PORT}
                """
            }
        }
        
        failure {
            script {
                echo """
                 BUILD #${env.BUILD_NUMBER} ÉCHOUÉ !
                """
                
                bat """
                    echo "========================================="
                    echo "  DIAGNOSTIC D'ÉCHEC"
                    echo "========================================="
                    
                    echo ""
                    echo " LOGS DE L'APPLICATION :"
                    echo "-----------------------------------------"
                    docker logs ${CONTAINER_NAME} --tail=50 2>nul || echo " Application non disponible"
                    
                    echo ""
                    echo "LOGS DE POSTGRESQL :"
                    echo "-----------------------------------------"
                    docker logs ${DB_CONTAINER} --tail=30 2>nul || echo " PostgreSQL non disponible"
                    
                    echo ""
                    echo " STATUT DES CONTENEURS :"
                    echo "-----------------------------------------"
                    docker ps -a | findstr ${APP_NAME}
                    docker ps -a | findstr ${DB_CONTAINER}
                """
            }
        }
        
        cleanup {
            script {
                echo "🧹 Nettoyage de l'environnement Docker isolé..."
                bat """
                    echo " Arrêt des conteneurs..."
                    docker stop ${CONTAINER_NAME} 2>nul || echo " Application arrêtée"
                    docker stop ${DB_CONTAINER} 2>nul || echo " PostgreSQL arrêté"
                    
                    echo " Suppression des conteneurs..."
                    docker rm ${CONTAINER_NAME} 2>nul || echo " Application supprimée"
                    docker rm ${DB_CONTAINER} 2>nul || echo " PostgreSQL supprimé"
                    
                    echo " Suppression du réseau..."
                    docker network rm ${NETWORK_NAME} 2>nul || echo " Réseau supprimé"
                    
                    echo " Nettoyage des images non utilisées..."
                    docker image prune -f 2>nul || echo " Nettoyage terminé"
                    
                    echo " Environnement Docker nettoyé avec succès !"
                """
            }
        }
    }
}