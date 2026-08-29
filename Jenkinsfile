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
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_COMMIT_SHORT = bat(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
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
        // DOCKER LOCAL
        // ============================================================

        stage('Build Docker Image') {
            steps {
                bat """
                    echo "Construction de l'image Docker..."
                    docker build -t ${env.DOCKER_IMAGE} -t ${env.APP_NAME}:latest .
                    docker images | findstr ${env.APP_NAME}
                """
            }
        }

        stage('Run Docker Container') {
            steps {
                bat """
                    echo "Arrêt du conteneur existant..."
                    docker stop ${env.APP_NAME} 2>nul || echo "Conteneur non trouvé"
                    docker rm ${env.APP_NAME} 2>nul || echo "Conteneur non trouvé"
                    
                    echo "Lancement du conteneur sur le port 8080..."
                    docker run -d --name ${env.APP_NAME} -p 8080:8080 ${env.DOCKER_IMAGE}
                    
                    echo "Conteneur démarré !"
                    docker ps | findstr ${env.APP_NAME}
                    
                    echo "Application disponible sur http://localhost:8080"
                """
            }
        }
    }

    post {
        always {
            cleanWs()
        }
        success {
            echo "Build #${env.BUILD_NUMBER} reussi !"
            echo "Image : ${env.DOCKER_IMAGE}"
            echo "Application : http://localhost:8080"
        }
        failure {
            echo " Build #${env.BUILD_NUMBER} en echec - verifier les logs"
        }
    }
}