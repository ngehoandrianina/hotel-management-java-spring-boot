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
        
        // Configuration des tests
        SPRING_PROFILES_ACTIVE = 'test'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_COMMIT_SHORT = bat(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    echo "📦 Code récupéré - Commit: ${env.GIT_COMMIT_SHORT}"
                    echo "🏷️ Build #${env.BUILD_NUMBER}"
                }
            }
        }

        // ============================================================
        // ÉTAPE 1 : TESTS AVEC H2 (SANS DOCKER)
        // ============================================================
        stage('Unit Tests with H2') {
            steps {
                script {
                    echo "🧪 Exécution des tests unitaires avec H2..."
                    bat """
                        echo "Tests unitaires :"
                        mvn -B -ntp test -Dspring.profiles.active=test
                    """
                }
            }
            post {
                always {
                    // Publier les rapports de tests
                    junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
                    
                    // Archiver les rapports HTML (si disponibles)
                    publishHTML([
                        allowMissing: true,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'target/site',
                        reportFiles: 'index.html',
                        reportName: 'Test Reports'
                    ])
                }
            }
        }

        // ============================================================
        // ÉTAPE 2 : TESTS D'INTÉGRATION AVEC DOCKER
        // ============================================================
        stage('Setup Docker Environment') {
            when {
                expression { env.RUN_INTEGRATION_TESTS != 'false' }
            }
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
                        
                        echo "2️⃣ Nettoyage des anciens conteneurs..."
                        docker rm -f ${DB_CONTAINER} 2>nul || echo "✅ PostgreSQL propre"
                        docker rm -f ${CONTAINER_NAME} 2>nul || echo "✅ Application propre"
                    """
                }
            }
        }

        stage('Start PostgreSQL') {
            when {
                expression { env.RUN_INTEGRATION_TESTS != 'false' }
            }
            steps {
                script {
                    echo "🐘 Démarrage de PostgreSQL..."
                    
                    bat """
                        echo "Démarrage de PostgreSQL pour les tests d'intégration..."
                        docker run -d \
                            --name ${DB_CONTAINER} \
                            --network ${NETWORK_NAME} \
                            -e POSTGRES_DB=${DB_NAME} \
                            -e POSTGRES_USER=${DB_USER} \
                            -e POSTGRES_PASSWORD=${DB_PASSWORD} \
                            -p ${DB_PORT} \
                            postgres:16-alpine
                    """
                    
                    echo "⏳ Attente de 30 secondes pour l'initialisation de PostgreSQL..."
                    sleep time: 30, unit: 'SECONDS'
                }
            }
        }

        stage('Wait for PostgreSQL') {
            when {
                expression { env.RUN_INTEGRATION_TESTS != 'false' }
            }
            steps {
                script {
                    echo "⏳ Attente que PostgreSQL soit complètement prêt..."
                    
                    def maxAttempts = 15
                    def waitTime = 5
                    def ready = false
                    
                    for (int i = 1; i <= maxAttempts; i++) {
                        try {
                            def result = bat(script: """
                                docker exec ${DB_CONTAINER} pg_isready -U ${DB_USER} -d ${DB_NAME}
                            """, returnStatus: true)
                            
                            if (result == 0) {
                                ready = true
                                echo "✅ PostgreSQL est prêt ! (tentative ${i}/${maxAttempts})"
                                break
                            }
                        } catch (Exception e) {
                            echo "⏳ PostgreSQL n'est pas encore prêt... (tentative ${i}/${maxAttempts})"
                        }
                        
                        if (i < maxAttempts) {
                            sleep time: waitTime, unit: 'SECONDS'
                        }
                    }
                    
                    if (!ready) {
                        bat "docker logs ${DB_CONTAINER} --tail=20"
                        error "❌ PostgreSQL n'est pas prêt après ${maxAttempts} tentatives"
                    }
                }
            }
        }

        stage('Build Docker Image') {
            when {
                expression { env.RUN_INTEGRATION_TESTS != 'false' }
            }
            steps {
                script {
                    echo "🐳 Construction de l'image Docker..."
                    bat """
                        echo "Construction de l'image ${DOCKER_IMAGE}..."
                        docker build -t ${DOCKER_IMAGE} .
                    """
                }
            }
        }

        stage('Run Application for Integration Tests') {
            when {
                expression { env.RUN_INTEGRATION_TESTS != 'false' }
            }
            steps {
                script {
                    echo "🚀 Démarrage de l'application pour les tests d'intégration..."
                    
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
                    """
                    
                    echo "⏳ Attente du démarrage de l'application (30 secondes)..."
                    sleep time: 30, unit: 'SECONDS'
                    
                    bat """
                        echo "✅ Conteneur démarré !"
                        docker ps | findstr ${CONTAINER_NAME}
                        docker logs ${CONTAINER_NAME} --tail=20
                    """
                }
            }
        }

        stage('Integration Tests') {
            when {
                expression { env.RUN_INTEGRATION_TESTS != 'false' }
            }
            steps {
                script {
                    echo "🧪 Exécution des tests d'intégration..."
                    
                    // Exécuter les tests avec le profil docker
                    bat """
                        echo "Tests d'intégration avec Docker :"
                        mvn -B -ntp test -Dspring.profiles.active=docker \
                            -Dtest=**/*IntegrationTest.java
                    """
                }
            }
            post {
                always {
                    junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
                }
            }
        }

        // ============================================================
        // ÉTAPE 3 : PACKAGE ET DEPLOIEMENT
        // ============================================================
        stage('Package') {
            steps {
                script {
                    echo "📦 Packaging de l'application..."
                    bat 'mvn -B -ntp package -DskipTests'
                }
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }

        stage('Deploy Application') {
            when {
                expression { env.RUN_INTEGRATION_TESTS != 'false' }
            }
            steps {
                script {
                    echo "🚀 Déploiement final..."
                    
                    // Redémarrer l'application avec la version package
                    bat """
                        echo "Redémarrage de l'application avec le jar package..."
                        docker stop ${CONTAINER_NAME} 2>nul || echo "Conteneur arrêté"
                        docker rm ${CONTAINER_NAME} 2>nul || echo "Conteneur supprimé"
                        
                        docker run -d \
                            --name ${CONTAINER_NAME} \
                            --network ${NETWORK_NAME} \
                            -p ${APP_PORT}:8080 \
                            -e SPRING_DATASOURCE_URL=jdbc:postgresql://${DB_CONTAINER}:${DB_PORT}/${DB_NAME} \
                            -e SPRING_DATASOURCE_USERNAME=${DB_USER} \
                            -e SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD} \
                            -e SPRING_JPA_HIBERNATE_DDL_AUTO=update \
                            ${DOCKER_IMAGE}
                    """
                    
                    echo "⏳ Attente du redémarrage..."
                    sleep time: 20, unit: 'SECONDS'
                    
                    echo "✅ Application déployée sur http://localhost:${APP_PORT}"
                }
            }
        }

        stage('Health Check') {
            when {
                expression { env.RUN_INTEGRATION_TESTS != 'false' }
            }
            steps {
                script {
                    echo "🏥 Vérification de la santé de l'application..."
                    
                    try {
                        def healthStatus = bat(script: """
                            curl -s --connect-timeout 10 -o nul -w "%%{http_code}" http://localhost:${APP_PORT}/actuator/health
                        """, returnStdout: true).trim()
                        
                        if (healthStatus == '200') {
                            echo "✅ Application en bonne santé !"
                        } else {
                            echo "⚠️ Health check retourne : ${healthStatus}"
                            bat "docker logs ${CONTAINER_NAME} --tail=30"
                        }
                    } catch (Exception e) {
                        echo "⚠️ Health check non disponible"
                        bat "docker logs ${CONTAINER_NAME} --tail=50"
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
                
                echo "📊 Conteneurs en cours :"
                bat 'docker ps'
                
                echo "🧹 Nettoyage de l'espace de travail Jenkins..."
                cleanWs()
            }
        }
        
        success {
            script {
                echo """
                ✅ BUILD #${env.BUILD_NUMBER} RÉUSSI !
                
                📊 Résumé :
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
                ❌ BUILD #${env.BUILD_NUMBER} ÉCHOUÉ !
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
                """
            }
        }
        
        cleanup {
            when {
                expression { env.RUN_INTEGRATION_TESTS != 'false' }
            }
            steps {
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
}