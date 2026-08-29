pipeline {
    agent any

    tools {
        maven 'Maven-3.9'
        jdk 'JDK-21'
    }

    environment {
        APP_NAME        = 'hotel-room-management'
        DOCKER_REGISTRY = credentials('docker-registry-url')
        DOCKER_CREDS    = credentials('docker-registry-credentials')
        IMAGE_TAG       = "${env.BUILD_NUMBER}"
        SONAR_TOKEN     = credentials('sonarqube-token')
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '15'))
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_COMMIT_SHORT = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                }
            }
        }

        stage('Build') {
            steps {
                sh 'mvn -B -ntp clean compile'
            }
        }

        stage('Unit & Integration Tests') {
            steps {
                sh 'mvn -B -ntp test'
            }
            post {
                always {
                    junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: false
                }
            }
        }

        stage('Code Coverage (JaCoCo)') {
            steps {
                sh 'mvn -B -ntp jacoco:report'
            }
            post {
                always {
                    recordCoverage(
                        tools: [[parser: 'JACOCO', pattern: '**/target/site/jacoco/jacoco.xml']],
                        qualityGates: [[threshold: 60.0, metric: 'LINE', baseline: 'PROJECT']]
                    )
                }
            }
        }

        stage('Static Analysis (SonarQube)') {
            when { expression { return env.SONAR_TOKEN != null } }
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh """
                        mvn -B -ntp sonar:sonar \
                        -Dsonar.projectKey=${APP_NAME} \
                        -Dsonar.login=${SONAR_TOKEN}
                    """
                }
            }
        }

        stage('Quality Gate') {
            when { expression { return env.SONAR_TOKEN != null } }
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Package') {
            steps {
                sh 'mvn -B -ntp package -DskipTests'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                sh """
                    docker build -t ${DOCKER_REGISTRY}/${APP_NAME}:${IMAGE_TAG} \
                                 -t ${DOCKER_REGISTRY}/${APP_NAME}:latest .
                """
            }
        }

        stage('Push Docker Image') {
            when { branch 'main' }
            steps {
                sh """
                    echo \$DOCKER_CREDS_PSW | docker login ${DOCKER_REGISTRY} -u \$DOCKER_CREDS_USR --password-stdin
                    docker push ${DOCKER_REGISTRY}/${APP_NAME}:${IMAGE_TAG}
                    docker push ${DOCKER_REGISTRY}/${APP_NAME}:latest
                """
            }
        }

        stage('Deploy - Staging') {
            when { branch 'develop' }
            steps {
                sh """
                    docker compose -f docker-compose.staging.yml pull
                    docker compose -f docker-compose.staging.yml up -d
                """
            }
        }

        stage('Deploy - Production') {
            when { branch 'main' }
            steps {
                input message: 'Deployer en production ?', ok: 'Deployer'
                sh """
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
            // Exemple : notifications Slack / email
            // slackSend channel: '#ci-cd', color: 'danger', message: "Echec du build ${APP_NAME} #${env.BUILD_NUMBER}"
        }
    }
}
