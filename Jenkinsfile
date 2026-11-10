
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
                sh 'ant dist'
            }
        }

        stage('SpotBugs Analysis') {
            steps {
                sh '''
                    mkdir -p reports

                    if [ ! -d build ]; then
                        echo "No compiled classes found in build/!"
                        exit 1
                    fi

                    # Analyze only our own classes (build/). The dist jar bundles
                    # jdom, whose optional XPath classes need jaxen (not shipped),
                    # which caused the "missing classes" warning. Libraries go on
                    # the aux classpath so SpotBugs can resolve them without
                    # reporting bugs inside third-party code.
                    AUX_CP=$(find lib -name "*.jar" -type f | paste -sd: -)

                    "$SPOTBUGS_HOME/bin/spotbugs" \
                      -textui \
                      -auxclasspath "$AUX_CP" \
                      -xml:withMessages=reports/spotbugs.xml \
                      -html=reports/spotbugs.html \
                      build
                '''
            }
        }

        stage('Archive Results') {
            steps {
                archiveArtifacts artifacts: 'reports/spotbugs.xml, reports/spotbugs.html',
                                 allowEmptyArchive: false
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'reports/spotbugs.xml, reports/spotbugs.html',
                             allowEmptyArchive: true
        }
    }
}
