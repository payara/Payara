#!groovy
// Jenkinsfile for building a PR and running a subset of tests against it
def pom
def DOMAIN_NAME
def payaraBuildNumber
def buildId
pipeline {
    agent {
        label 'general-purpose'
    }
    options {
        buildDiscarder(logRotator(numToKeepStr: '30', daysToKeepStr: '14'))
    }
    environment {
        MP_METRICS_TAGS='tier=integration'
        MP_CONFIG_CACHE_DURATION=0
        JAVA_HOME = tool("zulu-21")
    }
    tools {
        jdk "zulu-21"
        maven "maven-3.6.3"
    }
    stages {
        stage('Report') {
            steps {
                script {
                    pom = readMavenPom file: 'pom.xml'
                    payaraBuildNumber = "PR${env.ghprbPullId}#${currentBuild.number}"
                    DOMAIN_NAME = "test-domain"
                    echo "Payara pom version is ${pom.version}"
                    echo "Build number is ${payaraBuildNumber}"
                    echo "Domain name is ${DOMAIN_NAME}"
              }
            }
        }
        stage('Build') {
            steps {
                script {
                    echo '*#*#*#*#*#*#*#*#*#*#*#*#  Fetching Build Job Artifacts  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                    // Try using branch parameter instead for PR builds
                    def specificBranchCommitOrTag = env.CHANGE_BRANCH ?: env.BRANCH_NAME
                    def repoOrg = env.CHANGE_FORK ?: 'Payara'

                    // buildId = buildJob.getNumber()
                    buildId = "189"
                    echo '*#*#*#*#*#*#*#*#*#*#*#*#    Fetched Build Job Artifacts   *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                }
            }
            post {
                success{
                    // Get and stash artifacts from the Build job
                    copyArtifacts projectName: 'Build/Build',
                        filter: 'payara-bom.pom,payara-embedded-all.jar,payara-embedded-web.jar,payara-micro.jar,payara-web.zip,payara.zip',
                        selector: specific("${buildId}"),
                        target: 'artifacts/'

                    archiveArtifacts artifacts: 'artifacts/payara.zip', fingerprint: true
                    archiveArtifacts artifacts: 'artifacts/payara-micro.jar', fingerprint: true
                    stash name: 'payara-target', includes: 'artifacts/payara.zip', allowEmpty: true
                    stash name: 'payara-web', includes: 'artifacts/payara-web.zip', allowEmpty: true
                    stash name: 'payara-bom', includes: 'artifacts/payara-bom.pom', allowEmpty: true
                    stash name: 'payara-micro', includes: 'artifacts/payara-micro.jar', allowEmpty: true
                    stash name: 'payara-embedded-all', includes: 'artifacts/payara-embedded-all.jar', allowEmpty: true
                    stash name: 'payara-embedded-web', includes: 'artifacts/payara-embedded-web.jar', allowEmpty: true

                    dir('/home/ubuntu/.m2/repository/'){
                        stash name: 'payara-m2-repository', includes: '**', allowEmpty: true
                    }
                }
                always {
                    archiveArtifacts allowEmptyArchive: true, artifacts: '**/server.log', fingerprint: true
                }
            }
        }
        stage('Run Tests'){
            parallel {
                stage('Quicklook Tests') {
                    agent {
                        label 'general-purpose'
                    }
                    options {
                        retry(3)
                    }
                    steps {
                        setupArtifactsAndM2()
                        setupDomain()

                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        sh """rm  ~/test\\|sa.mv.db  || true"""
                        sh """mvn -B -V -ff -e clean test --strict-checksums -Pall \
                        -Dglassfish.home=\"${pwd()}/payara7/glassfish\" \
                        -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/lib/security/cacerts \
                        -Djavax.xml.accessExternalSchema=all \
                        -Dsurefire.rerunFailingTestsCount=2 \
                        -Dfailsafe.rerunFailingTestsCount=2 \
                        -f appserver/tests/quicklook/pom.xml"""
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                    }
                    post {
                        always {
                            junit 'appserver/tests/quicklook/test-output/QuickLookTests/*.xml'
                            stopDomain()
                        }
                        cleanup {
                            saveLogsAndCleanup 'quicklook-log.zip'
                        }
                    }
                }
                 stage('Payara Samples Tests') {
                     agent {
                         label 'general-purpose'
                    }
                     options {
                         retry(3)
                    }
                     steps {
                         setupArtifactsAndM2()
                         setupDomain()
                         echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                         sh """mvn -V -B -ff clean install --strict-checksums -Ppayara-server-remote,playwright \
                         -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/lib/security/cacerts \
                         -Djavax.xml.accessExternalSchema=all \
                         -Dsurefire.rerunFailingTestsCount=2 \
                         -Dfailsafe.rerunFailingTestsCount=2 \
                         -f appserver/tests/payara-samples """
                         echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                     }
                     post {
                         always {
                             processReportAndStopDomain()
                         }
                         cleanup {
                             saveLogsAndCleanup 'samples-log.zip'
                         }
                     }
                 }
                stage('MicroProfile Config TCK') {
                    agent {
                        label 'general-purpose'
                    }
                    options {
                        retry(3)
                    }
                    steps{
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Checking out MP TCK Runners  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        checkout changelog: false, poll: false, scm: [$class: 'GitSCM',
                            branches: [[name: "*/microprofile-6.1-Payara7"]],
                            userRemoteConfigs: [[url: "https://github.com/payara/MicroProfile-TCK-Runners.git"]]]
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Checked out MP TCK Runners  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'

                        setupArtifactsAndM2()
                        setupDomain()
                        updatePomPayaraVersion("${pom.version}")

                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        sh """mvn -B -V -ff -e clean verify --strict-checksums \
                        -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/lib/security/cacerts \
                        -Djavax.xml.accessExternalSchema=all \
                        -Dpayara_domain=${DOMAIN_NAME} -Dpayara.home="${pwd()}/payara7" \
                        -Dsurefire.rerunFailingTestsCount=2 \
                        -Dfailsafe.rerunFailingTestsCount=2 \
                        -Ppayara-server-remote,full \
                        -f MicroProfile-Config"""
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                    }
                    post {
                        always {
                            processReportAndStopDomain()
                        }
                        cleanup {
                            saveLogsAndCleanup 'mp-tck-log.zip'
                        }
                    }
                }
                stage('MicroProfile Fault Tolerance TCK') {
                    agent {
                        label 'general-purpose'
                    }
                    options {
                        retry(3)
                    }
                    steps{
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Checking out MP TCK Runners  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        checkout changelog: false, poll: false, scm: [$class: 'GitSCM',
                            branches: [[name: "*/microprofile-6.1-Payara7"]],
                            userRemoteConfigs: [[url: "https://github.com/payara/MicroProfile-TCK-Runners.git"]]]
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Checked out MP TCK Runners  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'

                        setupArtifactsAndM2()
                        setupDomain()
                        updatePomPayaraVersion("${pom.version}")

                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        sh """mvn -B -V -ff -e clean verify --strict-checksums \
                        -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/lib/security/cacerts \
                        -Djavax.xml.accessExternalSchema=all \
                        -Dpayara_domain=${DOMAIN_NAME} -Dpayara.home="${pwd()}/payara7" \
                        -Dsurefire.rerunFailingTestsCount=2 \
                        -Dfailsafe.rerunFailingTestsCount=2 \
                        -Ppayara-server-remote,full \
                        -f MicroProfile-Fault-Tolerance"""
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                    }
                    post {
                        always {
                            processReportAndStopDomain()
                        }
                        cleanup {
                            saveLogsAndCleanup 'mp-tck-log.zip'
                        }
                    }
                }
                stage('MicroProfile Health TCK') {
                    agent {
                        label 'general-purpose'
                    }
                    options {
                        retry(3)
                    }
                    steps{
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Checking out MP TCK Runners  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        checkout changelog: false, poll: false, scm: [$class: 'GitSCM',
                            branches: [[name: "*/microprofile-6.1-Payara7"]],
                            userRemoteConfigs: [[url: "https://github.com/payara/MicroProfile-TCK-Runners.git"]]]
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Checked out MP TCK Runners  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'

                        setupArtifactsAndM2()
                        setupDomain()
                        updatePomPayaraVersion("${pom.version}")

                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        sh """mvn -B -V -ff -e clean verify --strict-checksums \
                        -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/lib/security/cacerts \
                        -Djavax.xml.accessExternalSchema=all \
                        -Dpayara_domain=${DOMAIN_NAME} -Dpayara.home="${pwd()}payara7" \
                        -Dsurefire.rerunFailingTestsCount=2 \
                        -Dfailsafe.rerunFailingTestsCount=2 \
                        -Ppayara-server-remote,full \
                        -f MicroProfile-Health"""
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                    }
                    post {
                        always {
                            processReportAndStopDomain()
                        }
                        cleanup {
                            saveLogsAndCleanup 'mp-tck-log.zip'
                        }
                    }
                }
                stage('MicroProfile JWT Auth TCK') {
                    agent {
                        label 'general-purpose'
                    }
                    options {
                        retry(3)
                    }
                    steps{
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Checking out MP TCK Runners  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        checkout changelog: false, poll: false, scm: [$class: 'GitSCM',
                            branches: [[name: "*/microprofile-6.1-Payara7"]],
                            userRemoteConfigs: [[url: "https://github.com/payara/MicroProfile-TCK-Runners.git"]]]
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Checked out MP TCK Runners  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'

                        setupArtifactsAndM2()
                        setupDomain()
                        updatePomPayaraVersion("${pom.version}")

                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        sh """mvn -B -V -ff -e clean verify --strict-checksums \
                        -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/lib/security/cacerts \
                        -Djavax.xml.accessExternalSchema=all \
                        -Dpayara_domain=${DOMAIN_NAME} -Dpayara.home="${pwd()}/payara7" \
                        -Dsurefire.rerunFailingTestsCount=2 \
                        -Dfailsafe.rerunFailingTestsCount=2 \
                        -Ppayara-server-remote,full \
                        -f MicroProfile-JWT-Auth"""
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                    }
                    post {
                        always {
                            processReportAndStopDomain()
                        }
                        cleanup {
                            saveLogsAndCleanup 'mp-tck-log.zip'
                        }
                    }
                }
                stage('MicroProfile Metrics TCK') {
                    agent {
                        label 'general-purpose'
                    }
                    options {
                        retry(3)
                    }
                    steps{
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Checking out MP TCK Runners  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        checkout changelog: false, poll: false, scm: [$class: 'GitSCM',
                            branches: [[name: "*/microprofile-6.1-Payara7"]],
                            userRemoteConfigs: [[url: "https://github.com/payara/MicroProfile-TCK-Runners.git"]]]
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Checked out MP TCK Runners  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'

                        setupArtifactsAndM2()
                        setupDomain()
                        updatePomPayaraVersion("${pom.version}")

                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        sh """mvn -B -V -ff -e clean verify --strict-checksums \
                        -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/lib/security/cacerts \
                        -Djavax.xml.accessExternalSchema=all \
                        -Dpayara_domain=${DOMAIN_NAME} -Dpayara.home="${pwd()}/payara7" \
                        -Dsurefire.rerunFailingTestsCount=2 \
                        -Dfailsafe.rerunFailingTestsCount=2 \
                        -Ppayara-server-remote,full \
                        -f MicroProfile-Metrics"""
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                    }
                    post {
                        always {
                            processReportAndStopDomain()
                        }
                        cleanup {
                            saveLogsAndCleanup 'mp-tck-log.zip'
                        }
                    }
                }
                stage('MicroProfile OpenAPI TCK') {
                    agent {
                        label 'general-purpose'
                    }
                    options {
                        retry(3)
                    }
                    steps{
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Checking out MP TCK Runners  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        checkout changelog: false, poll: false, scm: [$class: 'GitSCM',
                            branches: [[name: "*/microprofile-6.1-Payara7"]],
                            userRemoteConfigs: [[url: "https://github.com/payara/MicroProfile-TCK-Runners.git"]]]
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Checked out MP TCK Runners  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'

                        setupArtifactsAndM2()
                        setupDomain()
                        updatePomPayaraVersion("${pom.version}")

                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        sh """mvn -B -V -ff -e clean verify --strict-checksums \
                        -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/lib/security/cacerts \
                        -Djavax.xml.accessExternalSchema=all \
                        -Dpayara_domain=${DOMAIN_NAME} -Dpayara.home="${pwd()}/payara7" \
                        -Dsurefire.rerunFailingTestsCount=2 \
                        -Dfailsafe.rerunFailingTestsCount=2 \
                        -Drepository.1.id=payara-nexus-snapshots \
                        -Drepository.1.url=https://nexus.dev.payara.fish/repository/payara-snapshots \
                        -Drepository.1.releases.enabled=false \
                        -Drepository.1.snapshots.enabled=true \
                        -Ppayara-server-remote,full \
                        -f MicroProfile-OpenAPI"""
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                    }
                    post {
                        always {
                            processReportAndStopDomain()
                        }
                        cleanup {
                            saveLogsAndCleanup 'mp-tck-log.zip'
                        }
                    }
                }
                stage('MicroProfile OpenTelemetry Tracing TCK') {
                    agent {
                        label 'general-purpose'
                    }
                    options {
                        retry(3)
                    }
                    steps{
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Checking out MP TCK Runners  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        checkout changelog: false, poll: false, scm: [$class: 'GitSCM',
                            branches: [[name: "*/microprofile-6.1-Payara7"]],
                            userRemoteConfigs: [[url: "https://github.com/payara/MicroProfile-TCK-Runners.git"]]]
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Checked out MP TCK Runners  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'

                        setupArtifactsAndM2()
                        setupDomain()
                        updatePomPayaraVersion("${pom.version}")

                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        sh """mvn -B -V -ff -e clean verify --strict-checksums \
                        -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/lib/security/cacerts \
                        -Djavax.xml.accessExternalSchema=all \
                        -Dpayara_domain=${DOMAIN_NAME} -Dpayara.home="${pwd()}/payara7" \
                        -Dsurefire.rerunFailingTestsCount=2 \
                        -Dfailsafe.rerunFailingTestsCount=2 \
                        -Ppayara-server-remote,full \
                        -f MicroProfile-OpenTelemetry-Tracing"""
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                    }
                    post {
                        always {
                            processReportAndStopDomain()
                        }
                        cleanup {
                            saveLogsAndCleanup 'mp-tck-log.zip'
                        }
                    }
                }
                stage('MicroProfile OpenTracing TCK') {
                    agent {
                        label 'general-purpose'
                    }
                    options {
                        retry(3)
                    }
                    steps{
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Checking out MP TCK Runners  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        checkout changelog: false, poll: false, scm: [$class: 'GitSCM',
                            branches: [[name: "*/microprofile-6.1-Payara7"]],
                            userRemoteConfigs: [[url: "https://github.com/payara/MicroProfile-TCK-Runners.git"]]]
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Checked out MP TCK Runners  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'

                        setupArtifactsAndM2()
                        setupDomain()
                        updatePomPayaraVersion("${pom.version}")

                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        sh """mvn -B -V -ff -e clean verify --strict-checksums \
                        -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/lib/security/cacerts \
                        -Djavax.xml.accessExternalSchema=all \
                        -Dpayara_domain=${DOMAIN_NAME} -Dpayara.home="${pwd()}/payara7" \
                        -Dsurefire.rerunFailingTestsCount=2 \
                        -Dfailsafe.rerunFailingTestsCount=2 \
                        -Ppayara-server-remote,full \
                        -f MicroProfile-OpenTracing"""
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                    }
                    post {
                        always {
                            processReportAndStopDomain()
                        }
                        cleanup {
                            saveLogsAndCleanup 'mp-tck-log.zip'
                        }
                    }
                }
                stage('MicroProfile REST Client TCK') {
                    agent {
                        label 'general-purpose'
                    }
                    options {
                        retry(3)
                    }
                    steps{
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Checking out MP TCK Runners  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        checkout changelog: false, poll: false, scm: [$class: 'GitSCM',
                            branches: [[name: "*/microprofile-6.1-Payara7"]],
                            userRemoteConfigs: [[url: "https://github.com/payara/MicroProfile-TCK-Runners.git"]]]
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Checked out MP TCK Runners  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'

                        setupArtifactsAndM2()
                        setupDomain()
                        updatePomPayaraVersion("${pom.version}")

                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        sh """mvn -B -V -ff -e clean verify --strict-checksums \
                        -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/lib/security/cacerts \
                        -Djavax.xml.accessExternalSchema=all \
                        -Dpayara_domain=${DOMAIN_NAME} -Dpayara.home="${pwd()}/payara7" \
                        -Dsurefire.rerunFailingTestsCount=2 \
                        -Dfailsafe.rerunFailingTestsCount=2 \
                        -Ppayara-server-remote,full \
                        -f MicroProfile-Rest-Client"""
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                    }
                    post {
                        always {
                            processReportAndStopDomain()
                        }
                        cleanup {
                            saveLogsAndCleanup 'mp-tck-log.zip'
                        }
                    }
                }
                stage('EE8 Tests') {
                    agent {
                        label 'general-purpose'
                    }
                    options {
                        retry(3)
                    }
                    steps{
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Checking out EE8 tests  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        checkout changelog: false, poll: false, scm: [$class: 'GitSCM',
                            branches: [[name: "*/Payara7"]],
                            userRemoteConfigs: [[url: "https://github.com/payara/patched-src-javaee8-samples.git"]]]
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Checked out EE8 tests  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'

                        setupArtifactsAndM2()
                        setupDomain()
                        updatePomPayaraVersion("${pom.version}")

                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        sh "mvn -B -V -ff -e clean install --strict-checksums -Dsurefire.useFile=false \
                        -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/lib/security/cacerts \
                        -Djavax.xml.accessExternalSchema=all \
                        -Dsurefire.rerunFailingTestsCount=2 \
                        -Dfailsafe.rerunFailingTestsCount=2 \
                        -Ppayara-server-remote,stable"
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                    }
                    post {
                        always {
                            processReportAndStopDomain()
                        }
                        cleanup {
                            saveLogsAndCleanup 'ee8-samples-log.zip'
                        }
                    }
                }
                stage('CargoTracker Tests') {
                    agent {
                        label 'general-purpose'
                    }
                    options {
                        retry(3)
                    }
                    steps{
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Checking out cargoTracker tests  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        checkout changelog: false, poll: false, scm: [$class: 'GitSCM',
                            branches: [[name: "*/Payara7"]],
                            userRemoteConfigs: [[url: "https://github.com/payara/cargoTracker.git"]]]
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Checked out cargoTracker tests  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'

                        setupArtifactsAndM2()
                        setupDomain()
                        updatePomPayaraVersion("${pom.version}")

                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Cleaning CargoTracker Database in /tmp  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        sh "rm -rf /tmp/cargo*"

                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        sh  """mvn -B -V -ff -e clean verify --strict-checksums -Dsurefire.useFile=false \
                         -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/lib/security/cacerts \
                         -Djavax.xml.accessExternalSchema=all \
                         -Dsurefire.rerunFailingTestsCount=2 \
                         -Dfailsafe.rerunFailingTestsCount=2 \
                         -Ppayara-server-remote -DtrimStackTrace=false"""
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                    }
                    post {
                        always {
                            processReportAndStopDomain()
                        }
                        cleanup {
                            saveLogsAndCleanup 'cargotracker-log.zip'
                        }
                    }
                }
                stage('EE7 Tests') {
                    agent {
                        label 'general-purpose'
                    }
                    options {
                        retry(3)
                    }
                    steps{
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Checking out EE7 tests  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        checkout changelog: false, poll: false, scm: [$class: 'GitSCM',
                            branches: [[name: "*/Payara7"]],
                            userRemoteConfigs: [[url: "https://github.com/payara/patched-src-javaee7-samples.git"]]]
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Checked out EE7 tests  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'

                        setupArtifactsAndM2()
                        setupDomain()
                        updatePomPayaraVersion("${pom.version}")

                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        sh """mvn -B -V -ff -e clean install --strict-checksums \
                        -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/lib/security/cacerts \
                        -Djavax.xml.accessExternalSchema=all \
                        -Dpayara_domain=${DOMAIN_NAME} \
                        -Dsurefire.rerunFailingTestsCount=2 \
                        -Dfailsafe.rerunFailingTestsCount=2 \
                        -Ppayara-server-remote,stable"""
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                    }
                    post {
                        always {
                            processReportAndStopDomain()
                        }
                        cleanup {
                            saveLogsAndCleanup 'ee7-samples-log.zip'
                        }
                    }
                }
                stage('Payara Functional Tests') {
                    agent {
                        label 'general-purpose'
                    }
                    options {
                        retry(3)
                    }
                    steps {
                        setupArtifactsAndM2()
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Unstash Micro and Embedded *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        unstash name: 'payara-micro'
                        unstash name: 'payara-embedded-all'
                        unstash name: 'payara-embedded-web'

                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Building dependencies  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        sh """mvn -V -B -ff clean install --strict-checksums \
                        -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/lib/security/cacerts \
                        -Djavax.xml.accessExternalSchema=all \
                        -DskipTests \
                        -f appserver/tests/payara-samples -pl fish.payara.samples:payara-samples \
                        -pl fish.payara.samples:samples-test-utils -pl fish.payara.samples:test-domain-setup \
                        -pl fish.payara.samples:payara-samples-profiled-tests"""

                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test with Payara Micro  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        sh """mvn -V -B -ff clean install --strict-checksums -Ppayara-micro-managed,install-deps \
                        -Dsurefire.rerunFailingTestsCount=2 \
                        -Dfailsafe.rerunFailingTestsCount=2 \
                        -f appserver/tests/functional/payara-micro """

                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test with Payara Embedded  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        sh """mvn -V -B -ff clean verify --strict-checksums -PFullProfile \
                        -Dversion=${pom.version} \
                        -Dsurefire.rerunFailingTestsCount=2 \
                        -Dfailsafe.rerunFailingTestsCount=2 \
                        -f appserver/tests/functional/embeddedtest """

                        sh """mvn -V -B -ff clean verify --strict-checksums -PWebProfile \
                        -Dversion=${pom.version} \
                        -Dsurefire.rerunFailingTestsCount=2 \
                        -Dfailsafe.rerunFailingTestsCount=2 \
                        -f appserver/tests/functional/embeddedtest """

                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running asadmin tests  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        setupDomain()
                        sh """python3 appserver/tests/functional/asadmin/run_all_tests.py \
                        --asadmin ${pwd()}/payara7/bin/asadmin"""
                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                    }
                    post {
                        always {
                            stopDomain()
                        }
                        cleanup {
                            processReport()
                            saveLogsAndCleanup 'asadmin-log.zip'
                        }
                    }
                }
            }
        }
    }
}

void makeDomain() {
    script{
        ASADMIN = "./payara7/bin/asadmin"
        DOMAIN_NAME = "test-domain"
    }
    sh "${ASADMIN} create-domain --nopassword ${DOMAIN_NAME}"
}

void setupDomain() {
    echo '*#*#*#*#*#*#*#*#*#*#*#*#  Unstash distributions  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
    unstash name: 'payara-target'
    echo '*#*#*#*#*#*#*#*#*#*#*#*#  Extract payara.zip  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
    sh 'unzip -q artifacts/payara.zip -d . || true'
    sh 'ls -la'
    sh 'ls -la payara7'
    echo '*#*#*#*#*#*#*#*#*#*#*#*#  Unstash maven repository  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
    echo '*#*#*#*#*#*#*#*#*#*#*#*#  Setting up tests  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
    makeDomain()
    sh "${ASADMIN} start-domain ${DOMAIN_NAME}"
    sh "${ASADMIN} start-database || true"
}

void setupM2RepositoryOnly() {
    echo '*#*#*#*#*#*#*#*#*#*#*#*#  Unstash maven repository  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
    dir('/home/ubuntu/.m2/repository/'){
        unstash name: 'payara-m2-repository'
    }
}

void setupArtifactsAndM2() {
    // Read pom to get version information
    def pom = readMavenPom file: 'pom.xml'
    echo "Using Payara version: ${pom.version}"
    
    echo '*#*#*#*#*#*#*#*#*#*#*#*#  Unstash all artifacts  *#*#*#*#*#**#*#*#*#*#*#*#*#*#*#'
    echo '=== Starting artifact unstashing process ==='
    echo '*#*#*#*#*#*#*#*#*#*#*#*#  Unstash maven repository  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'

    dir('/home/ubuntu/.m2/repository/'){
            unstash name: 'payara-m2-repository'
        }
    
    echo 'Unstaking payara-target...'
    unstash name: 'payara-target'
    echo '✓ payara-target unstashed successfully'
    
    echo 'Unstaking payara-web...'
    unstash name: 'payara-web'
    echo '✓ payara-web unstashed successfully'
    
    echo 'Unstaking payara-bom...'
    unstash name: 'payara-bom'
    echo '✓ payara-bom unstashed successfully'
    
    echo 'Unstaking payara-micro...'
    unstash name: 'payara-micro'
    echo '✓ payara-micro unstashed successfully'
    
    echo 'Unstaking payara-embedded-all...'
    unstash name: 'payara-embedded-all'
    echo '✓ payara-embedded-all unstashed successfully'
    
    echo 'Unstaking payara-embedded-web...'
    unstash name: 'payara-embedded-web'
    echo '✓ payara-embedded-web unstashed successfully'
    
    echo '=== All artifacts unstashed ==='

    sh 'ls -la'
    sh 'ls -la artifacts/'

    echo '*#*#*#*#*#*#*#*#*#*#*#*#  Install artifacts to Maven local repository  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
    echo '=== Starting Maven installation process ==='
    
    echo 'Installing payara-bom.pom...'
    sh "cd artifacts && mvn install:install-file -Dfile=payara-bom.pom -DpomFile=payara-bom.pom -DgroupId=fish.payara.api -DartifactId=payara-bom -Dversion=${pom.version} -Dpackaging=pom"
    echo '✓ payara-bom.pom installed successfully'
    
    echo 'Installing payara-embedded-all.jar...'
    sh "cd artifacts && mvn install:install-file -Dfile=payara-embedded-all.jar -DgroupId=fish.payara.extras -DartifactId=payara-embedded-all -Dversion=${pom.version} -Dpackaging=jar"
    echo '✓ payara-embedded-all.jar installed successfully'
    
    echo 'Installing payara-embedded-web.jar...'
    sh "cd artifacts && mvn install:install-file -Dfile=payara-embedded-web.jar -DgroupId=fish.payara.extras -DartifactId=payara-embedded-web -Dversion=${pom.version} -Dpackaging=jar"
    echo '✓ payara-embedded-web.jar installed successfully'
    
    echo 'Installing payara-micro.jar...'
    sh "cd artifacts && mvn install:install-file -Dfile=payara-micro.jar -DgroupId=fish.payara.extras -DartifactId=payara-micro -Dversion=${pom.version} -Dpackaging=jar"
    echo '✓ payara-micro.jar installed successfully'
    
    echo 'Installing payara.zip...'
    sh "cd artifacts && mvn install:install-file -Dfile=payara.zip -DgroupId=fish.payara.distributions -DartifactId=payara -Dversion=${pom.version} -Dpackaging=zip"
    echo '✓ payara.zip installed successfully'
    
    echo 'Installing payara-web.zip...'
    sh "cd artifacts && mvn install:install-file -Dfile=payara-web.zip -DgroupId=fish.payara.distributions -DartifactId=payara-web -Dversion=${pom.version} -Dpackaging=zip"
    echo '✓ payara-web.zip installed successfully'
}

void processReportAndStopDomain() {
    junit '**/target/*-reports/*.xml'
    sh "${ASADMIN} stop-domain ${DOMAIN_NAME}"
    sh "${ASADMIN} stop-database || true"
}

void processReport() {
    junit '**/target/*-reports/*.xml'
}

void stopDomain() {
    sh "${ASADMIN} stop-domain ${DOMAIN_NAME}"
    sh "${ASADMIN} stop-database || true"
}

void saveLogsAndCleanup(String logArchiveName) {
    zip archive: true, dir: "./payara7/glassfish/domains/${DOMAIN_NAME}/logs", glob: 'server.*', zipFile: logArchiveName
    echo 'tidying up after tests: '
    sh "rm -f -v *.zip"
    sh "${ASADMIN} delete-domain ${DOMAIN_NAME}"
}

void updatePomPayaraVersion(String payaraVersion) {
    echo '*#*#*#*#*#*#*#*#*#*#*#*#  Updating pom.xml payara.version property for Shrinkwrap resolver  *#*#*#*#*#*#*#*#*#*#*#*#'
    echo "Setting payara.version to: ${payaraVersion}"
    sh script: "sed -i \"s/payara\\.version>.*<\\/payara\\.version>/payara\\.version>${payaraVersion}<\\/payara\\.version>/g\" pom.xml", label: "Update pom.xml payara.version property"
}
