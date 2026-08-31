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
        
        // Configuration PostgreSQL
        DB_HOST = 'my-postgres'
        DB_PORT = '5432'
        DB_NAME = 'hotel'
        DB_USERNAME = 'admin'
        DB_PASSWORD = 'password'
        NETWORK_NAME = 'hotel-network'
        
        // Port de l'application
        APP_PORT = '8090'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_COMMIT_SHORT = bat(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    echo "Code récupéré - Commit: ${env.GIT_COMMIT_SHORT}"
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
        // DOCKER LOCAL AVEC POSTGRESQL
        // ============================================================

        stage('Setup Docker Network') {
            steps {
                script {
                    echo " Configuration du réseau Docker..."
                    bat """
                        echo "Création du réseau ${NETWORK_NAME}..."
                        docker network create ${NETWORK_NAME} 2>nul || echo "Réseau déjà existant"
                    """
                }
            }
        }

                
stage('Setup PostgreSQL') {
    steps {
        script {
            echo 'Vérification de PostgreSQL...'

            bat '''
                docker inspect my-postgres >nul 2>&1

                if errorlevel 1 (
                    echo "Création du conteneur PostgreSQL..."

                    docker run -d ^
                        --name my-postgres ^
                        --network hotel-network ^
                        -e POSTGRES_USER=admin ^
                        -e POSTGRES_PASSWORD=password ^
                        -e POSTGRES_DB=enieditor ^
                        -p 5432:5432 ^
                        postgres:18
                ) else (
                    echo "Le conteneur my-postgres existe déjà."

                    docker start my-postgres >nul 2>&1 || echo "my-postgres est déjà démarré"

                    docker network connect hotel-network my-postgres >nul 2>&1 || echo "my-postgres est déjà connecté à hotel-network"
                )

                echo "Attente de PostgreSQL..."

                :wait_postgres
                docker exec my-postgres pg_isready -U admin -d enieditor >nul 2>&1

                if errorlevel 1 (
                    timeout /t 2 /nobreak >nul
                    goto wait_postgres
                )

                echo "PostgreSQL est prêt !"
            '''
        }
    }
    }

 

        stage('Build Docker Image') {
            steps {
                script {
                    echo " Construction de l'image Docker..."
                    bat """
                        echo "Construction de l'image ${DOCKER_IMAGE}..."
                        docker build -t ${DOCKER_IMAGE} -t ${APP_NAME}:latest .
                        
                        echo "Images Docker disponibles :"
                        docker images | findstr ${APP_NAME}
                    """
                }
            }
        }

        stage('Stop Container') {
            steps {
                script {
                    echo " Arrêt du conteneur existant..."
                    bat """
                        docker stop ${APP_NAME} 2>nul || echo "Conteneur non trouvé"
                        docker rm ${APP_NAME} 2>nul || echo "Conteneur non trouvé"
                    """
                }
            }
        }

        stage('Run Docker Container') {
            steps {
                script {
                    echo " Démarrage du conteneur sur le port ${APP_PORT}..."
                    bat """
                        docker run -d --name ${APP_NAME} \
                            --network ${NETWORK_NAME} \
                            -p ${APP_PORT}:8080 \
                            -e SPRING_DATASOURCE_URL=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME} \
                            -e SPRING_DATASOURCE_USERNAME=${DB_USERNAME} \
                            -e SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD} \
                            -e SPRING_JPA_HIBERNATE_DDL_AUTO=update \
                            ${DOCKER_IMAGE}
                        
                        echo " Attente du démarrage de l'application (20 secondes)..."
                        timeout /t 20
                        
                        echo " Conteneur démarré !"
                        docker ps | findstr ${APP_NAME}
                        
                        echo " Logs de l'application :"
                        docker logs ${APP_NAME} --tail=20
                        
                        echo " Application disponible sur http://localhost:${APP_PORT}"
                    """
                }
            }
        }

        stage('Health Check') {
            steps {
                script {
                    echo " Vérification de la santé de l'application..."
                    
                    // Vérifier que le conteneur est en cours
                    def containerRunning = bat(script: """
                        docker ps | findstr ${APP_NAME}
                    """, returnStatus: true)
                    
                    if (containerRunning != 0) {
                        error " Le conteneur n'est pas en cours d'exécution"
                    }
                    
                    // Tester le health check (optionnel)
                    try {
                        def healthStatus = bat(script: """
                            curl -s -o nul -w "%%{http_code}" http://localhost:${APP_PORT}/actuator/health
                        """, returnStdout: true).trim()
                        
                        if (healthStatus == '200') {
                            echo " Application en bonne santé !"
                        } else {
                            echo " Health check retourne : ${healthStatus}"
                        }
                    } catch (Exception e) {
                        echo "Health check non disponible (actuator non configuré)"
                        // Afficher quand même les logs
                        bat "docker logs ${APP_NAME} --tail=10"
                    }
                }
            }
        }

        stage('Test API') {
            steps {
                script {
                    echo "Test de l'API..."
                    try {
                        def response = bat(script: """
                            curl -s http://localhost:${APP_PORT}/api/health || echo "API non disponible"
                        """, returnStdout: true).trim()
                        echo "Réponse : ${response}"
                    } catch (Exception e) {
                        echo " API health non disponible"
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                echo "Nettoyage de l'espace de travail..."
                cleanWs()
                
                echo "Conteneurs en cours :"
                bat 'docker ps'
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
                - PostgreSQL : ${DB_HOST}:${DB_PORT}
                - Base de données : ${DB_NAME}
                """
            }
        }
        
        failure {
            script {
                echo """
                BUILD #${env.BUILD_NUMBER} ÉCHOUÉ !
                
                Vérifiez les logs pour plus de détails.
                """
                
                // Afficher les logs d'erreur pour diagnostic
                bat """
                    echo "=== LOGS DE L'APPLICATION ==="
                    docker logs ${APP_NAME} --tail=50 2>nul || echo "Application non disponible"
                    
                    echo "=== LOGS DE POSTGRESQL ==="
                    docker logs ${DB_HOST} --tail=20 2>nul || echo "PostgreSQL non disponible"
                    
                    echo "=== CONTENEURS EN COURS ==="
                    docker ps -a
                """
            }
        }
        
    //   cleanup {
    //         script {
    //             // Nettoyage optionnel des conteneurs après le build
    //             // Décommentez si vous voulez arrêter automatiquement
    //             bat """
    //                 echo "Nettoyage des conteneurs..."
    //                 docker stop ${APP_NAME} ${DB_HOST} 2>nul || true
    //                 docker rm ${APP_NAME} ${DB_HOST} 2>nul || true
    //             """
    //         }
    //     }
    }
}