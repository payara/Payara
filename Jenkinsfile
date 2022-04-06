#!groovy
// Jenkinsfile for building a PR and running a subset of tests against it
def pom
def DOMAIN_NAME
def payaraBuildNumber
pipeline {
    agent any
    environment {
        MP_METRICS_TAGS='tier=integration'
        MP_CONFIG_CACHE_DURATION=0
    }
    tools {
        jdk "zulu-11"
    }
    stages {
        stage('Report') {
            steps {
                script{
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
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Building SRC  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                withCredentials([usernameColonPassword(credentialsId: 'JenkinsNexusUser', variable: 'NEXUS_USER')]) {
                    sh """mvn -B -V -ff -e clean install --strict-checksums -PQuickBuild \
                    -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/lib/security/cacerts \
                    -Djavax.xml.accessExternalSchema=all -Dbuild.number=${payaraBuildNumber}"""
                }
                echo '*#*#*#*#*#*#*#*#*#*#*#*#    Built SRC   *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
            }
            post{
                success{
                    archiveArtifacts artifacts: 'appserver/distributions/payara/target/payara.zip', fingerprint: true
                    archiveArtifacts artifacts: 'appserver/extras/payara-micro/payara-micro-distribution/target/payara-micro.jar', fingerprint: true
                    archiveArtifacts allowEmptyArchive: true, artifacts: 'appserver/distributions/payara/target/stage/payara6/glassfish/logs/server.log'
                }
            }
        }
        stage('Setup for Quicklook Tests') {
            steps {
                sh "rm -f -v *.zip"
                setupDomain()
            }
        }
        stage('Run Quicklook Tests') {
            steps {
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                sh """mvn -B -V -ff -e clean test --strict-checksums -Pall \
                -Dglassfish.home=\"${pwd()}/appserver/distributions/payara/target/stage/payara6/glassfish\" \
                -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/lib/security/cacerts \
                -Djavax.xml.accessExternalSchema=all \
                -f appserver/tests/quicklook/pom.xml"""
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
            }
            post {
                failure {
                    zip archive: true, dir: "appserver/distributions/payara/target/stage/payara6/glassfish/domains/${DOMAIN_NAME}/logs", glob: 'server.*', zipFile: 'quicklook-log.zip'
                }
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
                cleanup {
                    teardownDomain()
                }
            }
        }
        stage('Run Payara Samples Tests') {
            steps {
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                sh """mvn -V -B -ff clean install --strict-checksums -Ppayara-server-managed \
                -Dpayara.version=${pom.version} \
                -Dglassfish.home=\"${pwd()}/appserver/distributions/payara/target/stage/payara6/glassfish\" \
                -Dcom.sun.aas.productRoot=\"${pwd()}/appserver/distributions/payara/target/stage/payara6/glassfish\" \
                -Dpayara_domain=${DOMAIN_NAME} \
                -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/lib/security/cacerts \
                -Djavax.xml.accessExternalSchema=all \
                -Dsurefire.rerunFailingTestsCount=2 \
                -f appserver/tests/payara-samples """
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
            }
            post {
               always {
                   junit '**/target/surefire-reports/*.xml'
               }
            }
        }
        stage('Checkout MP TCK Runners') {
            steps{
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Checking out MP TCK Runners  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                checkout changelog: false, poll: false, scm: [$class: 'GitSCM',
                    branches: [[name: "*/microprofile-5.0"]],
                    userRemoteConfigs: [[url: "https://github.com/payara/MicroProfile-TCK-Runners.git"]]]
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Checked out MP TCK Runners  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
            }
        }
        stage('Run MP TCK Tests') {
            steps {
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                sh """mvn -B -V -ff -e clean verify --strict-checksums \
                -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/lib/security/cacerts \
                -Djavax.xml.accessExternalSchema=all -Dpayara.version=${pom.version} \
                -Dpayara_domain=${DOMAIN_NAME} -Duse.cnHost=true \
                -Dsurefire.rerunFailingTestsCount=2 -Ppayara-server-managed,payara6"""
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }
        stage('Checkout EE8 Tests') {
            steps{
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Checking out EE8 tests  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                checkout changelog: false, poll: false, scm: [$class: 'GitSCM',
                    branches: [[name: "*/master"]],
                    userRemoteConfigs: [[url: "https://github.com/payara/patched-src-javaee8-samples.git"]]]
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Checked out EE8 tests  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
            }
        }
        stage('Setup for EE8 Tests') {
            steps {
                sh "rm -f -v *.zip"
                setupDomain()
            }
        }
        stage('Run EE8 Tests') {
            steps {
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                sh "mvn -B -V -ff -e clean install --strict-checksums -Dsurefire.useFile=false \
                -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/lib/security/cacerts \
                -Djavax.xml.accessExternalSchema=all -Dpayara.version=${pom.version} \
                -Dsurefire.rerunFailingTestsCount=2 -Ppayara-server-remote,stable"
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
            }
            post {
                failure {
                    zip archive: true, dir: "appserver/distributions/payara/target/stage/payara6/glassfish/domains/${DOMAIN_NAME}/logs", glob: 'server.*', zipFile: 'ee8-samples-log.zip'
                }
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
                cleanup {
                    teardownDomain()
                }
            }
        }
        stage('Checkout CargoTracker Tests') {
            steps{
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Checking out cargoTracker tests  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                checkout changelog: false, poll: false, scm: [$class: 'GitSCM',
                    branches: [[name: "*/master"]],
                    userRemoteConfigs: [[url: "https://github.com/payara/cargoTracker.git"]]]
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Checked out cargoTracker tests  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
            }
        }
        stage('Run CargoTracker Tests') {
            steps {
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Cleaning CargoTracker Database in /tmp  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                sh "rm -rf /tmp/cargo*"

                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                sh """mvn -B -V -ff -e clean install --strict-checksums -Dsurefire.useFile=false \
                -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/lib/security/cacerts \
                -Djavax.xml.accessExternalSchema=all -Dpayara.version=${pom.version} \
                -Dsurefire.rerunFailingTestsCount=2 -Ppayara-server-managed,payara6"""
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }
        stage('Checkout EE7 Tests') {
            steps{
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Checking out EE7 tests  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                checkout changelog: false, poll: false, scm: [$class: 'GitSCM',
                    branches: [[name: "*/master"]],
                    userRemoteConfigs: [[url: "https://github.com/payara/patched-src-javaee7-samples.git"]]]
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Checked out EE7 tests  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
            }
        }
        stage('Setup for EE7 Tests') {
            steps {
                sh "rm -f -v *.zip"
                setupDomain()
            }
        }
        stage('Run EE7 Tests') {
            steps {
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                sh """mvn -B -V -ff -e clean install --strict-checksums -Dsurefire.useFile=false \
                -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/lib/security/cacerts \
                -Djavax.xml.accessExternalSchema=all -Dpayara.version=${pom.version} \
                -Dpayara_domain=${DOMAIN_NAME} -Duse.cnHost=true \
                -Dsurefire.rerunFailingTestsCount=2 -Ppayara-server-remote,stable,payara6"""
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
            }
            post {
                failure {
                    zip archive: true, dir: "appserver/distributions/payara/target/stage/payara6/glassfish/domains/${DOMAIN_NAME}/logs", glob: 'server.*', zipFile: 'ee7-samples-log.zip'
                }
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
                cleanup {
                    teardownDomain()
                }
            }
        }
    }
}

void makeDomain() {
    script{
        ASADMIN = "./appserver/distributions/payara/target/stage/payara6/bin/asadmin"
        DOMAIN_NAME = "test-domain"
    }
    sh "${ASADMIN} create-domain --nopassword ${DOMAIN_NAME}"
}

void setupDomain() {
    echo '*#*#*#*#*#*#*#*#*#*#*#*#  Setting up tests  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
    makeDomain()
    sh "${ASADMIN} start-domain ${DOMAIN_NAME}"
    sh "${ASADMIN} start-database || true"
}

void teardownDomain() {
    echo 'tidying up after tests:'
    sh "${ASADMIN} stop-domain ${DOMAIN_NAME}"
    sh "${ASADMIN} stop-database || true"
    sh "${ASADMIN} delete-domain ${DOMAIN_NAME}"
}
