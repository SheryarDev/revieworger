
pipeline {
    agent any

    tools {
        ant 'Ant'
    }

    environment {
        SPOTBUGS_HOME = '/opt/spotbugs-4.10.2'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Clean') {
            steps {
                sh 'ant clean'
            }
        }

        stage('Build') {
            steps {
                sh 'ant jar'
            }
        }

        stage('SpotBugs Analysis') {
            steps {
                sh '''
                    mkdir -p reports

                    JAR_FILE=$(find dist -name "*.jar" -print -quit)

                    if [ -z "$JAR_FILE" ]; then
                        echo "No JAR file found!"
                        exit 1
                    fi

                    "$SPOTBUGS_HOME/bin/spotbugs" \
                      -textui \
                      -xml:withMessages \
                      -output reports/spotbugs.xml \
                      "$JAR_FILE"
                '''
            }
        }

        stage('Archive Results') {
            steps {
                archiveArtifacts artifacts: 'reports/spotbugs.xml',
                                 allowEmptyArchive: false
            }
        }

        stage('Run Application') {
            steps {
                sh 'ant run'
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'reports/spotbugs.xml',
                             allowEmptyArchive: true
        }
    }
}
