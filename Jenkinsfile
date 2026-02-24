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
                    echo '*#*#*#*#*#*#*#*#*#*#*#*#  Building SRC  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                    // Try using branch parameter instead for PR builds
                    def specificBranchCommitOrTag = env.CHANGE_BRANCH ?: env.BRANCH_NAME
                    def repoOrg = env.CHANGE_FORK ?: 'Payara'

                    // First build the build job and capture its build number
                    def buildJob = build job: 'Build/Build', wait: true,
                        parameters: [
                            string(name: 'specificBranchCommitOrTag', value: specificBranchCommitOrTag),
                            string(name: 'repoOrg', value: repoOrg),
                            string(name: 'jdkVer', value: 'zulu-21'),
                            string(name: 'stream', value: 'Community'),
                            string(name: 'profiles', value: 'BuildEmbedded'),
                            booleanParam(name: 'skipTests', value: false),
                            string(name: 'multiThread', value: '1')
                        ]
                    buildId = buildJob.getNumber()
                    echo '*#*#*#*#*#*#*#*#*#*#*#*#    Built SRC   *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                }
            }
            post {
                success{
                    // Get and stash artifacts from the Build job
                    copyArtifacts projectName: 'Build/Build',
                        filter: 'payara-bom.pom,payara-embedded-all.jar,payara-embedded-web.jar,payara-micro.jar,payara-web.zip,payara.zip',
                        selector: specific("${buildId}")
                    
                    archiveArtifacts artifacts: 'payara.zip', fingerprint: true
                    archiveArtifacts artifacts: 'payara-micro.jar', fingerprint: true
                    stash name: 'payara-target', includes: 'payara.zip,payara-web.zip', allowEmpty: true
                    stash name: 'payara-micro', includes: 'payara-micro.jar', allowEmpty: true
                    stash name: 'payara-embedded-all', includes: 'payara-embedded-all.jar', allowEmpty: true
                    stash name: 'payara-embedded-web', includes: 'payara-embedded-web.jar', allowEmpty: true
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
                        setupDomain()

                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        sh """rm  ~/test\\|sa.mv.db  || true"""
                        sh """mvn -B -V -ff -e clean test --strict-checksums -Pall \
                        -Dglassfish.home=\"${pwd()}/appserver/distributions/payara/target/stage/payara7/glassfish\" \
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

                        setupDomain()
                        updatePomPayaraVersion("${pom.version}")

                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        sh """mvn -B -V -ff -e clean verify --strict-checksums \
                        -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/lib/security/cacerts \
                        -Djavax.xml.accessExternalSchema=all \
                        -Dpayara_domain=${DOMAIN_NAME} -Dpayara.home="${pwd()}/appserver/distributions/payara/target/stage/payara7" \
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

                        setupDomain()
                        updatePomPayaraVersion("${pom.version}")

                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        sh """mvn -B -V -ff -e clean verify --strict-checksums \
                        -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/lib/security/cacerts \
                        -Djavax.xml.accessExternalSchema=all \
                        -Dpayara_domain=${DOMAIN_NAME} -Dpayara.home="${pwd()}/appserver/distributions/payara/target/stage/payara7" \
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

                        setupDomain()
                        updatePomPayaraVersion("${pom.version}")

                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        sh """mvn -B -V -ff -e clean verify --strict-checksums \
                        -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/lib/security/cacerts \
                        -Djavax.xml.accessExternalSchema=all \
                        -Dpayara_domain=${DOMAIN_NAME} -Dpayara.home="${pwd()}/appserver/distributions/payara/target/stage/payara7" \
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

                        setupDomain()
                        updatePomPayaraVersion("${pom.version}")

                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        sh """mvn -B -V -ff -e clean verify --strict-checksums \
                        -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/lib/security/cacerts \
                        -Djavax.xml.accessExternalSchema=all \
                        -Dpayara_domain=${DOMAIN_NAME} -Dpayara.home="${pwd()}/appserver/distributions/payara/target/stage/payara7" \
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

                        setupDomain()
                        updatePomPayaraVersion("${pom.version}")

                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        sh """mvn -B -V -ff -e clean verify --strict-checksums \
                        -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/lib/security/cacerts \
                        -Djavax.xml.accessExternalSchema=all \
                        -Dpayara_domain=${DOMAIN_NAME} -Dpayara.home="${pwd()}/appserver/distributions/payara/target/stage/payara7" \
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

                        setupDomain()
                        updatePomPayaraVersion("${pom.version}")

                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        sh """mvn -B -V -ff -e clean verify --strict-checksums \
                        -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/lib/security/cacerts \
                        -Djavax.xml.accessExternalSchema=all \
                        -Dpayara_domain=${DOMAIN_NAME} -Dpayara.home="${pwd()}/appserver/distributions/payara/target/stage/payara7" \
                        -Dsurefire.rerunFailingTestsCount=2 \
                        -Dfailsafe.rerunFailingTestsCount=2 \
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

                        setupDomain()
                        updatePomPayaraVersion("${pom.version}")

                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        sh """mvn -B -V -ff -e clean verify --strict-checksums \
                        -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/lib/security/cacerts \
                        -Djavax.xml.accessExternalSchema=all \
                        -Dpayara_domain=${DOMAIN_NAME} -Dpayara.home="${pwd()}/appserver/distributions/payara/target/stage/payara7" \
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

                        setupDomain()
                        updatePomPayaraVersion("${pom.version}")

                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        sh """mvn -B -V -ff -e clean verify --strict-checksums \
                        -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/lib/security/cacerts \
                        -Djavax.xml.accessExternalSchema=all \
                        -Dpayara_domain=${DOMAIN_NAME} -Dpayara.home="${pwd()}/appserver/distributions/payara/target/stage/payara7" \
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

                        setupDomain()
                        updatePomPayaraVersion("${pom.version}")

                        echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                        sh """mvn -B -V -ff -e clean verify --strict-checksums \
                        -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/lib/security/cacerts \
                        -Djavax.xml.accessExternalSchema=all \
                        -Dpayara_domain=${DOMAIN_NAME} -Dpayara.home="${pwd()}/appserver/distributions/payara/target/stage/payara7" \
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
                        setupM2RepositoryOnly()
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
                        --asadmin ${pwd()}/appserver/distributions/payara/target/stage/payara7/bin/asadmin"""
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
        ASADMIN = "./appserver/distributions/payara/target/stage/payara7/bin/asadmin"
        DOMAIN_NAME = "test-domain"
    }
    sh "${ASADMIN} create-domain --nopassword ${DOMAIN_NAME}"
}

void setupDomain() {
    echo '*#*#*#*#*#*#*#*#*#*#*#*#  Unstash distributions  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
    unstash name: 'payara-target'
    echo '*#*#*#*#*#*#*#*#*#*#*#*#  Unstash maven repository  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
    dir('/home/ubuntu/.m2/repository/'){
        unstash name: 'payara-m2-repository'
    }
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
    zip archive: true, dir: "appserver/distributions/payara/target/stage/payara7/glassfish/domains/${DOMAIN_NAME}/logs", glob: 'server.*', zipFile: logArchiveName
    echo 'tidying up after tests: '
    sh "rm -f -v *.zip"
    sh "${ASADMIN} delete-domain ${DOMAIN_NAME}"
}

void updatePomPayaraVersion(String payaraVersion) {
    echo '*#*#*#*#*#*#*#*#*#*#*#*#  Updating pom.xml payara.version property for Shrinkwrap resolver  *#*#*#*#*#*#*#*#*#*#*#*#'
    sh script: "sed -i \"s/payara\\.version>.*<\\/payara\\.version>/payara\\.version>${payaraVersion}<\\/payara\\.version>/g\" pom.xml", label: "Update pom.xml payara.version property"
}
