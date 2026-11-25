
pipeline {
    agent any

    tools {
        ant 'Ant'
    }

    environment {
        SPOTBUGS_HOME = '/opt/spotbugs-4.10.2'
        // SpotBugs (ex. 2) + JUnit/JaCoCo/PIT (ex. 3) reports
        // Same SCC options for every measurement (local + Jenkins)
        SCC_OPTS = '--include-ext java --exclude-dir build,dist,lib,lib-test,build-test,reports,tools,spotbugs,docs'
        REPORT_ARTIFACTS = 'reports/spotbugs.xml, reports/spotbugs.html, reports/junit/*.xml, reports/jacoco/**, reports/jacoco-pseudo/**, reports/pit/**, reports/pit-pseudo/**, reports/scc/**'
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

        stage('Code Metrics - SCC') {
            steps {
                sh '''
                    mkdir -p reports/scc

                    scc $SCC_OPTS . > reports/scc/scc-report.txt
                    scc --format json $SCC_OPTS . > reports/scc/scc-report.json
                    scc --by-file $SCC_OPTS . > reports/scc/scc-by-file.txt

                    cat reports/scc/scc-report.txt
                '''
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
    }

    post {
        always {
            archiveArtifacts artifacts: env.REPORT_ARTIFACTS, allowEmptyArchive: true
        }
    }
}
