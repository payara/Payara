#!groovy
// Jenkinsfile for building a PR and running a subset of tests against it
def pom
def DOMAIN_NAME
def payaraBuildNumber
def rc
pipeline {
    agent any
    options {
        disableConcurrentBuilds()
    }
    environment {
        Dibbles = "\${Dabbles}"
        Bibbly = "Bibbles"
    }
    tools {
        jdk "zulu-8"
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

                    echo '*#*#*#*#*#*#*#*#*#*#*#*#  Certificates Expiring Soon  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                    rc = sh(returnStatus: true, script: "keytool -list -v -keystore \'${pwd()}/nucleus/admin/template/src/main/resources/config/cacerts.jks\' -storepass \'changeit\' | grep -E \"Valid from:.*`date +'%b' --date='+1 month'`.*`date +'%Y'`|`date +'%b'`.*`date +'%Y'`\"")
                    if(rc != 1) {
                        currentBuild.result = 'UNSTABLE'
                    }
                    echo '*#*#*#*#*#*#*#*#*#*#*#*#  Certificates Expiring Soon  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                }
            }
        }
        stage('Build') {
            steps {
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Building SRC  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                withCredentials([usernameColonPassword(credentialsId: 'JenkinsNexusUser', variable: 'NEXUS_USER')]) {
                    sh """mvn -B -V -ff -e clean install -PQuickBuild \
                    -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/jre/lib/security/cacerts \
                    -Djavax.xml.accessExternalSchema=all -Dbuild.number=${payaraBuildNumber} \
                    -Dsurefire.rerunFailingTestsCount=2"""
                }
                echo '*#*#*#*#*#*#*#*#*#*#*#*#    Built SRC   *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
            }
            post{
                success{
                    archiveArtifacts artifacts: 'appserver/distributions/payara/target/payara.zip', fingerprint: true
                    archiveArtifacts artifacts: 'appserver/extras/payara-micro/payara-micro-distribution/target/payara-micro.jar', fingerprint: true
                    archiveArtifacts allowEmptyArchive: true, artifacts: 'appserver/distributions/payara/target/stage/payara5/glassfish/logs/server.log'
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
                sh """mvn -B -V -ff -e clean test -Pall \
                -Dglassfish.home=\"${pwd()}/appserver/distributions/payara/target/stage/payara5/glassfish\" \
                -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/jre/lib/security/cacerts \
                -Djavax.xml.accessExternalSchema=all \
                -Dsurefire.rerunFailingTestsCount=2 \
                -f appserver/tests/quicklook/pom.xml"""
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
            }
            post {
                always {
                    zip archive: true, dir: "appserver/distributions/payara/target/stage/payara5/glassfish/domains/${DOMAIN_NAME}/logs", glob: 'server.*', zipFile: 'quicklook-log.zip'
                    teardownDomain()
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }
        stage('Run Payara Samples Tests') {
            steps {
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                sh """mvn -V -B -ff clean install -Ppayara-server-managed \
                -Dpayara.version=${pom.version} \
                -Dglassfish.home=\"${pwd()}/appserver/distributions/payara/target/stage/payara5/glassfish\" \
                -Dpayara_domain=${DOMAIN_NAME} \
                -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/jre/lib/security/cacerts \
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
                sh "mvn -B -V -ff -e clean install -Dsurefire.useFile=false \
                -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/jre/lib/security/cacerts \
                -Djavax.xml.accessExternalSchema=all -Dpayara.version=${pom.version} \
                -Dsurefire.rerunFailingTestsCount=2 -Ppayara-server-remote,stable"
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
            }
            post {
                always {
                    zip archive: true, dir: "appserver/distributions/payara/target/stage/payara5/glassfish/domains/${DOMAIN_NAME}/logs", glob: 'server.*', zipFile: 'ee8-samples-log.zip'
                    teardownDomain()
                    junit '**/target/surefire-reports/*.xml'
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
                sh """mvn -B -V -ff -e clean install -Dsurefire.useFile=false \
                -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/jre/lib/security/cacerts \
                -Djavax.xml.accessExternalSchema=all -Dpayara.version=${pom.version} \
                -Dsurefire.rerunFailingTestsCount=2 -Ppayara-server-managed,payara5"""
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
                sh """mvn -B -V -ff -e clean install -Dsurefire.useFile=false \
                -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/jre/lib/security/cacerts \
                -Djavax.xml.accessExternalSchema=all -Dpayara.version=${pom.version} \
                -Dpayara_domain=${DOMAIN_NAME} -Duse.cnHost=true \
                -Dsurefire.rerunFailingTestsCount=2 -Ppayara-server-remote,stable,payara5"""
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
            }
            post {
                always {
                    zip archive: true, dir: "appserver/distributions/payara/target/stage/payara5/glassfish/domains/${DOMAIN_NAME}/logs", glob: 'server.*', zipFile: 'ee7-samples-log.zip'
                    teardownDomain()
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }
    }
}

void setupDomain() {
    echo '*#*#*#*#*#*#*#*#*#*#*#*#  Setting up tests  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
    script{
        ASADMIN = "./appserver/distributions/payara/target/stage/payara5/bin/asadmin"
        DOMAIN_NAME = "test-domain"
    }
    sh "${ASADMIN} create-domain --nopassword ${DOMAIN_NAME}"
    sh "${ASADMIN} start-domain ${DOMAIN_NAME}"
    sh "${ASADMIN} start-database || true"
}

void teardownDomain() {
    echo 'tidying up after tests:'
    sh "${ASADMIN} stop-domain ${DOMAIN_NAME}"
    sh "${ASADMIN} stop-database || true"
    sh "${ASADMIN} delete-domain ${DOMAIN_NAME}"
}
