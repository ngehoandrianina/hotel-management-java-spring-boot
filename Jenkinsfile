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

    stages {

        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_COMMIT_batORT = bat(script: 'git rev-parse --batort HEAD', returnStdout: true).trim()
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
                    junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: false
                }
            }
        }

        stage('Code Coverage (JaCoCo)') {
            steps {
                bat 'mvn -B -ntp jacoco:report'
            }
            post {
                always {
                    recordCoverage(
                        tools: [[parser: 'JACOCO', pattern: '**/target/site/jacoco/jacoco.xml']],
                        qualityGates: [[threbatold: 60.0, metric: 'LINE', baseline: 'PROJECT']]
                    )
                }
            }
        }

        stage('Static Analysis (SonarQube)') {
            when { expression { return env.SONAR_TOKEN != null } }
            steps {
                withSonarQubeEnv('SonarQube') {
                    bat """
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
                bat 'mvn -B -ntp package -DskipTests'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                bat """
                    docker build -t ${DOCKER_REGISTRY}/${APP_NAME}:${IMAGE_TAG} \
                                 -t ${DOCKER_REGISTRY}/${APP_NAME}:latest .
                """
            }
        }

        stage('Pubat Docker Image') {
            when { branch 'main' }
            steps {
                bat """
                    echo \$DOCKER_CREDS_PSW | docker login ${DOCKER_REGISTRY} -u \$DOCKER_CREDS_USR --password-stdin
                    docker pubat ${DOCKER_REGISTRY}/${APP_NAME}:${IMAGE_TAG}
                    docker pubat ${DOCKER_REGISTRY}/${APP_NAME}:latest
                """
            }
        }

        stage('Deploy - Staging') {
            when { branch 'develop' }
            steps {
                bat """
                    docker compose -f docker-compose.staging.yml pull
                    docker compose -f docker-compose.staging.yml up -d
                """
            }
        }

        stage('Deploy - Production') {
            when { branch 'main' }
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
            echo "Build #${env.BUILD_NUMBER} reussi pour ${env.GIT_COMMIT_batORT}"
        }
        failure {
            echo "Build #${env.BUILD_NUMBER} en echec - verifier les logs"
            // Exemple : notifications Slack / email
            // slackSend channel: '#ci-cd', color: 'danger', message: "Echec du build ${APP_NAME} #${env.BUILD_NUMBER}"
        }
    }
}
