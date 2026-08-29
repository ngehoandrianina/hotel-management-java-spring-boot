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

    // Définition des variables d'environnement
    environment {
        APP_NAME = 'hotel-room-management'
        DOCKER_REGISTRY = 'votre-registry'  // À remplacer par votre registry
        IMAGE_TAG = "${env.BUILD_NUMBER}"
        // SONAR_TOKEN est défini dans Jenkins Credentials
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
                script {
                    // Correction : 'short' au lieu de 'batort'
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

        stage('Code Coverage (JaCoCo)') {
            steps {
                bat 'mvn -B -ntp jacoco:report'
            }
            post {
                always {
                    // CORRECTION : Utilisation de publishCoverage au lieu de recordCoverage
                    // Ou supprimez cette section si le plugin n'est pas installé
                    script {
                        try {
                            publishCoverage adapters: [jacocoAdapter('**/target/site/jacoco/jacoco.xml')]
                        } catch (Exception e) {
                            echo "Coverage report generation failed: ${e.getMessage()}"
                            echo "Continuing without coverage..."
                        }
                    }
                }
            }
        }

        stage('Static Analysis (SonarQube)') {
            when { 
                expression { return env.SONAR_TOKEN != null && env.SONAR_TOKEN != '' } 
            }
            steps {
                withSonarQubeEnv('SonarQube') {
                    bat """
                        mvn -B -ntp sonar:sonar \\
                        -Dsonar.projectKey=${env.APP_NAME} \\
                        -Dsonar.login=${env.SONAR_TOKEN}
                    """
                }
            }
        }

        stage('Quality Gate') {
            when { 
                expression { return env.SONAR_TOKEN != null && env.SONAR_TOKEN != '' } 
            }
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
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
                bat """
                    docker build -t ${env.DOCKER_REGISTRY}/${env.APP_NAME}:${env.IMAGE_TAG} \\
                                 -t ${env.DOCKER_REGISTRY}/${env.APP_NAME}:latest .
                """
            }
        }

        stage('Push Docker Image') {  // CORRECTION : 'Push' au lieu de 'Pubat'
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
            // Décommentez si vous avez Slack
            // slackSend channel: '#ci-cd', color: 'danger', message: "Echec du build ${env.APP_NAME} #${env.BUILD_NUMBER}"
        }
    }
}