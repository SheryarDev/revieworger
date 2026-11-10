
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
                script {
                    // Stop the build loop: the report commit pushed by this
                    // pipeline would otherwise trigger another build.
                    def lastMsg = sh(script: 'git log -1 --pretty=%B', returnStdout: true).trim()
                    if (lastMsg.contains('[skip ci]')) {
                        currentBuild.result = 'NOT_BUILT'
                        error('Last commit is an automated report commit - skipping build.')
                    }
                }
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

        stage('Push Reports to GitHub') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'github-token',
                                                  usernameVariable: 'GIT_USER',
                                                  passwordVariable: 'GIT_TOKEN')]) {
                    sh '''
                        git config user.name "Jenkins CI"
                        git config user.email "jenkins@localhost"

                        REPO_URL="https://SheryarDev:${GITHUB_TOKEN}@github.com/SheryarDev/revieworger.git"

                        # Commit only the reports; build/ and dist/ changes stay local.
                        git add -f reports/spotbugs.xml reports/spotbugs.html

                        if git diff --cached --quiet; then
                            echo "Reports unchanged - nothing to push."
                            exit 0
                        fi

                        git commit -m "Update SpotBugs reports (build #${BUILD_NUMBER}) [skip ci]"
                        git pull --rebase --autostash "$REPO_URL" main
                        git push "$REPO_URL" HEAD:main
                    '''
                }
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
