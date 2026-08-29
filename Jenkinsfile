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
        DOCKER_REGISTRY = 'votre-registry'  // À remplacer par votre registry
        IMAGE_TAG = "${env.BUILD_NUMBER}"
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

        stage('Build') {
            steps {
                bat 'mvn -B -ntp clean compile'
            }
        }

        stage('Unit & Integration Tests') {
            steps {
                bat 'mvn -B -ntp test'
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

        stage('Build Docker Image') {
            when { 
                expression { return env.DOCKER_REGISTRY != null && env.DOCKER_REGISTRY != '' } 
            }
            steps {
                // CORRECTION : Une seule ligne pour Windows
                bat """
                    docker build -t ${env.DOCKER_REGISTRY}/${env.APP_NAME}:${env.IMAGE_TAG} -t ${env.DOCKER_REGISTRY}/${env.APP_NAME}:latest .
                """
            }
        }

        stage('Push Docker Image') {
            when { 
                branch 'main' 
                expression { return env.DOCKER_REGISTRY != null && env.DOCKER_REGISTRY != '' }
            }
            steps {
                withCredentials([string(credentialsId: 'docker-hub-token', variable: 'DOCKER_PASSWORD')]) {
                    bat """
                        echo ${env.DOCKER_PASSWORD} | docker login ${env.DOCKER_REGISTRY} -u ${env.DOCKER_USERNAME} --password-stdin
                        docker push ${env.DOCKER_REGISTRY}/${env.APP_NAME}:${env.IMAGE_TAG}
                        docker push ${env.DOCKER_REGISTRY}/${env.APP_NAME}:latest
                    """
                }
            }
        }

        stage('Deploy - Staging') {
            when { 
                branch 'develop' 
                expression { return fileExists('docker-compose.staging.yml') }
            }
            steps {
                bat """
                    docker compose -f docker-compose.staging.yml pull
                    docker compose -f docker-compose.staging.yml up -d
                """
            }
        }

        stage('Deploy - Production') {
            when { 
                branch 'main' 
                expression { return fileExists('docker-compose.prod.yml') }
            }
            steps {
                input message: 'Deployer en production ?', ok: 'Deployer'
                bat """
                    docker compose -f docker-compose.prod.yml pull
                    docker compose -f docker-compose.prod.yml up -d
                """
            }
        }
    }

    post {
        always {
            cleanWs()
        }
        success {
            echo "Build #${env.BUILD_NUMBER} reussi pour ${env.GIT_COMMIT_SHORT}"
        }
        failure {
            echo "Build #${env.BUILD_NUMBER} en echec - verifier les logs"
        }
    }
}