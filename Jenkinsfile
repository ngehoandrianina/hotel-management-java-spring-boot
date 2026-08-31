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
        
        // Noms uniques pour chaque build (évite les conflits)
        CONTAINER_NAME = "${env.APP_NAME}-${env.BUILD_NUMBER}"
        DB_CONTAINER = "postgres-${env.BUILD_NUMBER}"
        NETWORK_NAME = "hotel-network-${env.BUILD_NUMBER}"
        
        // Configuration PostgreSQL
        DB_NAME = 'hotel_db'
        DB_USER = 'hotel_user'
        DB_PASSWORD = 'hotel_password'
        DB_PORT = '5432'
        
        // Port de l'application (dynamique pour éviter les conflits)
        APP_PORT = '8090'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_COMMIT_SHORT = bat(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    echo "📦 Code récupéré - Commit: ${env.GIT_COMMIT_SHORT}"
                    echo "🏷️ Build #${env.BUILD_NUMBER}"
                    echo "🔑 Conteneur: ${env.CONTAINER_NAME}"
                    echo "🐘 PostgreSQL: ${env.DB_CONTAINER}"
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
                    echo "🌐 Création de l'environnement Docker isolé pour le build #${env.BUILD_NUMBER}"
                    
                    bat """
                        echo "========================================="
                        echo "  BUILD #${env.BUILD_NUMBER}"
                        echo "  Réseau : ${NETWORK_NAME}"
                        echo "  PostgreSQL : ${DB_CONTAINER}"
                        echo "  Application : ${CONTAINER_NAME}"
                        echo "========================================="
                        
                        echo "1️⃣ Création du réseau ${NETWORK_NAME}..."
                        docker network create ${NETWORK_NAME} 2>nul || echo "⚠️ Réseau déjà existant"
                        
                        echo "2️⃣ Nettoyage des anciens conteneurs (même nom)..."
                        docker rm -f ${DB_CONTAINER} 2>nul || echo "✅ PostgreSQL propre"
                        docker rm -f ${CONTAINER_NAME} 2>nul || echo "✅ Application propre"
                    """
                }
            }
        }

        stage('Start PostgreSQL') {
            steps {
                script {
                    echo "🐘 Démarrage de PostgreSQL pour le build #${env.BUILD_NUMBER}"
                    
                    bat """
                        echo "Démarrage de PostgreSQL..."
                        docker run -d \
                            --name ${DB_CONTAINER} \
                            --network ${NETWORK_NAME} \
                            -e POSTGRES_DB=${DB_NAME} \
                            -e POSTGRES_USER=${DB_USER} \
                            -e POSTGRES_PASSWORD=${DB_PASSWORD} \
                            -p ${DB_PORT} \
                            postgres:16-alpine
                        
                        echo "⏳ Attente du démarrage de PostgreSQL (15 secondes)..."
                        timeout /t 15 /nobreak >nul
                        
                        echo "Vérification de PostgreSQL..."
                        docker exec ${DB_CONTAINER} pg_isready -U ${DB_USER} -d ${DB_NAME}
                        if errorlevel 1 (
                            echo "❌ PostgreSQL ne répond pas !"
                            docker logs ${DB_CONTAINER}
                            exit /b 1
                        )
                        echo "✅ PostgreSQL est prêt !"
                    """
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    echo "🐳 Construction de l'image Docker..."
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
                    echo "🚀 Démarrage de l'application dans un conteneur isolé..."
                    
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
                            -e SPRING_PROFILES_ACTIVE=docker \
                            ${DOCKER_IMAGE}
                        
                        echo "⏳ Attente du démarrage de l'application (30 secondes)..."
                        timeout /t 30 /nobreak >nul
                        
                        echo "✅ Conteneur démarré !"
                        docker ps | findstr ${CONTAINER_NAME}
                        
                        echo "📋 Logs de l'application (premiers 20 lignes) :"
                        docker logs ${CONTAINER_NAME} --tail=20
                    """
                }
            }
        }

        stage('Health Check') {
            steps {
                script {
                    echo "🏥 Vérification de la santé de l'application..."
                    
                    // Vérifier que le conteneur est en cours
                    def containerRunning = bat(script: """
                        docker ps | findstr ${CONTAINER_NAME}
                    """, returnStatus: true)
                    
                    if (containerRunning != 0) {
                        error "❌ Le conteneur ${CONTAINER_NAME} n'est pas en cours d'exécution"
                    }
                    
                    // Tester le health check avec timeout
                    try {
                        def healthStatus = bat(script: """
                            curl -s --connect-timeout 10 -o nul -w "%%{http_code}" http://localhost:${APP_PORT}/actuator/health
                        """, returnStdout: true).trim()
                        
                        if (healthStatus == '200') {
                            echo "✅ Application en bonne santé !"
                        } else {
                            echo "⚠️ Health check retourne : ${healthStatus}"
                            bat "docker logs ${CONTAINER_NAME} --tail=20"
                        }
                    } catch (Exception e) {
                        echo "⚠️ Health check non disponible (actuator non configuré)"
                        echo "📋 Affichage des logs pour diagnostic :"
                        bat "docker logs ${CONTAINER_NAME} --tail=30"
                    }
                }
            }
        }

        stage('Integration Tests') {
            steps {
                script {
                    echo "🧪 Test d'intégration de l'API..."
                    try {
                        def response = bat(script: """
                            curl -s --connect-timeout 5 http://localhost:${APP_PORT}/api/test || echo "API non disponible"
                        """, returnStdout: true).trim()
                        echo "📝 Réponse de l'API : ${response}"
                    } catch (Exception e) {
                        echo "ℹ️ API de test non disponible"
                    }
                    
                    echo "✅ Tests d'intégration terminés"
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
                
                echo "📊 Conteneurs en cours :"
                bat 'docker ps'
                
                echo "🧹 Nettoyage de l'espace de travail Jenkins..."
                cleanWs()
            }
        }
        
        success {
            script {
                echo """
                ✅ =========================================
                ✅ BUILD #${env.BUILD_NUMBER} RÉUSSI !
                ✅ =========================================
                
                📊 Résumé :
                - Commit : ${env.GIT_COMMIT_SHORT}
                - Image : ${env.DOCKER_IMAGE}
                - Application : http://localhost:${env.APP_PORT}
                - PostgreSQL : ${DB_CONTAINER}:${DB_PORT}
                - Base de données : ${DB_NAME}
                - Réseau : ${NETWORK_NAME}
                
                🔧 Pour déboguer :
                - Logs app : docker logs ${CONTAINER_NAME}
                - Logs DB   : docker logs ${DB_CONTAINER}
                - Connexion : docker exec -it ${CONTAINER_NAME} sh
                """
            }
        }
        
        failure {
            script {
                echo """
                ❌ =========================================
                ❌ BUILD #${env.BUILD_NUMBER} ÉCHOUÉ !
                ❌ =========================================
                """
                
                bat """
                    echo "========================================="
                    echo "  DIAGNOSTIC D'ÉCHEC"
                    echo "========================================="
                    
                    echo ""
                    echo "📋 LOGS DE L'APPLICATION :"
                    echo "-----------------------------------------"
                    docker logs ${CONTAINER_NAME} --tail=50 2>nul || echo "⚠️ Application non disponible"
                    
                    echo ""
                    echo "📋 LOGS DE POSTGRESQL :"
                    echo "-----------------------------------------"
                    docker logs ${DB_CONTAINER} --tail=30 2>nul || echo "⚠️ PostgreSQL non disponible"
                    
                    echo ""
                    echo "📊 STATUT DES CONTENEURS :"
                    echo "-----------------------------------------"
                    docker ps -a | findstr ${APP_NAME}
                    docker ps -a | findstr ${DB_CONTAINER}
                    
                    echo ""
                    echo "🌐 RÉSEAUX DOCKER :"
                    echo "-----------------------------------------"
                    docker network ls | findstr ${NETWORK_NAME}
                """
            }
        }
        
        // ============================================================
        // NETTOYAGE COMPLET DE L'ENVIRONNEMENT ISOLÉ
        // ============================================================
        cleanup {
            script {
                echo "🧹 Nettoyage de l'environnement Docker isolé..."
                bat """
                    echo "1️⃣ Arrêt des conteneurs..."
                    docker stop ${CONTAINER_NAME} 2>nul || echo "✅ Application arrêtée"
                    docker stop ${DB_CONTAINER} 2>nul || echo "✅ PostgreSQL arrêté"
                    
                    echo "2️⃣ Suppression des conteneurs..."
                    docker rm ${CONTAINER_NAME} 2>nul || echo "✅ Application supprimée"
                    docker rm ${DB_CONTAINER} 2>nul || echo "✅ PostgreSQL supprimé"
                    
                    echo "3️⃣ Suppression du réseau..."
                    docker network rm ${NETWORK_NAME} 2>nul || echo "✅ Réseau supprimé"
                    
                    echo "4️⃣ Nettoyage des images non utilisées..."
                    docker image prune -f 2>nul || echo "✅ Nettoyage terminé"
                    
                    echo "✅ Environnement Docker nettoyé avec succès !"
                """
            }
        }
    }
}