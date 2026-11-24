
pipeline {
    agent any

    tools {
        ant 'Ant'
    }

    environment {
        SPOTBUGS_HOME = '/opt/spotbugs-4.10.2'
        // SpotBugs (ex. 2) + JUnit/JaCoCo/PIT (ex. 3) reports
        REPORT_ARTIFACTS = 'reports/spotbugs.xml, reports/spotbugs.html, reports/junit/*.xml, reports/jacoco/**, reports/jacoco-pseudo/**, reports/pit/**, reports/pit-pseudo/**'
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

        stage('Unit Tests & Coverage') {
            steps {
                // Runs the JUnit suite with the JaCoCo agent attached.
                // JaCoCo comes from tools/jacoco/lib (in the repo); the first run
                // downloads JUnit/PIT into lib-test/, which 'ant clean' keeps.
                sh 'ant coverageReport'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'reports/junit/TEST-*.xml'
                }
            }
        }

        stage('Mutation Testing') {
            steps {
                // PIT mutates data.*, logic.*, io.FileProcessor and
                // io.TableOutputter and reruns the test suite on each mutant.
                sh 'ant pit'
                // same mutants, but only the assertion-free pseudo-tests:
                // high line coverage, very low mutation score
                sh 'ant pit-pseudo'
                // JaCoCo report of the pseudo-tests alone (coverage looks fine...)
                sh 'ant coverageReportPseudo'
            }
        }

        stage('Archive Results') {
            steps {
                archiveArtifacts artifacts: env.REPORT_ARTIFACTS, allowEmptyArchive: false
            }
        }

        stage('Push Reports to GitHub') {
            steps {
                // 'GITHUB_TOKEN' = Jenkins credential of kind "Secret text"
                // (Manage Jenkins -> Credentials). Exposed here as $GITHUB_TOKEN.
                withCredentials([string(credentialsId: 'GITHUB_TOKEN',
                                        variable: 'GITHUB_TOKEN')]) {
                    sh '''
                        set +x
                        git config user.name "Jenkins CI"
                        git config user.email "jenkins@localhost"

                        REPO_URL="https://x-access-token:${GITHUB_TOKEN}@github.com/SheryarDev/revieworger.git"

                        # Commit only the reports; build/ and dist/ changes stay local.
                        git add -f reports/spotbugs.xml reports/spotbugs.html \
                                   reports/junit \
                                   reports/jacoco/report.xml reports/jacoco/report.csv reports/jacoco/html \
                                   reports/jacoco-pseudo/report.xml reports/jacoco-pseudo/html \
                                   reports/pit reports/pit-pseudo

                        if git diff --cached --quiet; then
                            echo "Reports unchanged - nothing to push."
                            exit 0
                        fi

                        git commit -m "Update SpotBugs, coverage and mutation reports (build #${BUILD_NUMBER}) [skip ci]"
                        git pull --rebase --autostash "$REPO_URL" main
                        git push "$REPO_URL" HEAD:main
                    '''
                }
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: env.REPORT_ARTIFACTS, allowEmptyArchive: true
        }
    }
}
