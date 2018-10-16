#!groovy
// Jenkinsfile for building a PR and running a subset of tests against it
def pom
def DOMAIN_NAME='test-domain'
def ASADMIN
def payaraBuildNumber
pipeline {
    options {
        disableConcurrentBuilds()
    }
    agent any
    tools {
        jdk "zulu-8"
    }
    stages {
        stage('report') {
            steps {
                script{
                    pom = readMavenPom file: 'pom.xml'
                    payaraBuildNumber = "PR${env.ghprbPullId}#${currentBuild.number}"
                    echo "Payara pom version is ${pom.version}"
                    echo "Build number is ${payaraBuildNumber}"
                }
            }
        }
        stage('Build') {
            steps {
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Building SRC  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                sh """mvn -B -V -ff -e clean install -PQuickBuild \
                -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/jre/lib/security/cacerts \
                -Djavax.xml.accessExternalSchema=all -Dbuild.number=${payaraBuildNumber}"""
                echo '*#*#*#*#*#*#*#*#*#*#*#*#    Built SRC   *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
            }
            post{
                success{
                    archiveArtifacts artifacts: 'appserver/distributions/payara/target/payara.zip', fingerprint: true
                    archiveArtifacts artifacts: 'appserver/extras/payara-micro/payara-micro-distribution/target/payara-micro.jar', fingerprint: true
                }
            }
        }
        stage('Setup for Quicklook Tests') {
            steps {
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Setting up tests  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                script{
                    ASADMIN = "./appserver/distributions/payara/target/stage/payara5/bin/asadmin"
                }
                sh "${ASADMIN} create-domain --nopassword ${DOMAIN_NAME}"
                sh "${ASADMIN} start-domain ${DOMAIN_NAME}"
                sh "${ASADMIN} start-database --dbtype derby || true"
            }
        }
        stage('Run Quicklook Tests') {
            steps {
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                sh """mvn -B -V -ff -e clean test \
                -Dglassfish.home=\"${pwd()}/appserver/distributions/payara/target/stage/payara5/glassfish\" \
                -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/jre/lib/security/cacerts \
                -Djavax.xml.accessExternalSchema=all \
                -f appserver/tests/quicklook/pom.xml"""
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
            }
            post {
                always {
                    echo 'tidying up after tests:'
                    sh "${ASADMIN} stop-domain ${DOMAIN_NAME}"
                    sh "${ASADMIN} stop-database --dbtype derby || true"
                    junit '**/target/surefire-reports/*.xml'
                    sh "${ASADMIN} delete-domain ${DOMAIN_NAME}"
                }
            }
        }
        stage('Checkout EE8 Tests') {
            steps{
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Checking out EE8 tests  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                checkout changelog: false, poll: false, scm: [$class: 'GitSCM',
                    branches: [[name: "*/master"]],
                    doGenerateSubmoduleConfigurations: false,
                    extensions: [
                        [$class: 'SubmoduleOption',
                        disableSubmodules: false,
                        parentCredentials: true,
                        recursiveSubmodules: true,
                        reference: '',
                        trackingSubmodules: false]],
                    submoduleCfg: [],
                    userRemoteConfigs: [[url: "https://github.com/payara/patched-src-javaee8-samples.git"]]]
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Checked out EE8 tests  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
            }
        }
        stage('Setup for EE8 Tests') {
            steps {
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Setting up tests  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                script{
                    ASADMIN = "./appserver/distributions/payara/target/stage/payara5/bin/asadmin"
                }
                sh "${ASADMIN} create-domain --nopassword ${DOMAIN_NAME}"
                sh "${ASADMIN} start-domain ${DOMAIN_NAME}"
                sh "${ASADMIN} start-database --dbtype derby || true"
            }
        }
        stage('Run EE8 Tests') {
            steps {
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                sh "mvn -B -V -ff -e clean install -Dsurefire.useFile=false -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/jre/lib/security/cacerts -Djavax.xml.accessExternalSchema=all -Dpayara.version=${pom.version} -Dpayara.directory.name=payara5 -Dpayara.version.major=5 -Ppayara-remote"
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
            }
            post {
                always {
                    echo 'tidying up after tests:'
                    sh "${ASADMIN} stop-domain ${DOMAIN_NAME}"
                    sh "${ASADMIN} stop-database --dbtype derby || true"
                    junit '**/target/surefire-reports/*.xml'
                    sh "${ASADMIN} delete-domain ${DOMAIN_NAME}"
                }
            }
        }
        stage('Checkout cargoTracker Tests') {
            steps{
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Checking out cargoTracker tests  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                checkout changelog: false, poll: false, scm: [$class: 'GitSCM',
                    branches: [[name: "*/master"]],
                    doGenerateSubmoduleConfigurations: false,
                    extensions: [
                        [$class: 'SubmoduleOption',
                        disableSubmodules: false,
                        parentCredentials: true,
                        recursiveSubmodules: true,
                        reference: '',
                        trackingSubmodules: false]],
                    submoduleCfg: [],
                    userRemoteConfigs: [[url: "https://github.com/payara/cargoTracker.git"]]]
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Checked out cargoTracker tests  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
            }
        }
        stage('Run cargoTracker Tests') {
            steps {
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                sh """mvn -B -V -ff -e clean install -Dsurefire.useFile=false \
                -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/jre/lib/security/cacerts \
                -Djavax.xml.accessExternalSchema=all -Dpayara.version=${pom.version} \
                -Dpayara.directory.name=payara5 \
                -Dpayara.version.major=5 -Ppayara-ci-managed"""
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
                    doGenerateSubmoduleConfigurations: false,
                    extensions: [
                        [$class: 'SubmoduleOption',
                        disableSubmodules: false,
                        parentCredentials: true,
                        recursiveSubmodules: true,
                        reference: '',
                        trackingSubmodules: false]],
                    submoduleCfg: [],
                    userRemoteConfigs: [[url: "https://github.com/payara/patched-src-javaee7-samples.git"]]]
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Checked out EE7 tests  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
            }
        }
        stage('Setup for EE7 Tests') {
            steps {
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Setting up tests  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                script{
                    ASADMIN = "./appserver/distributions/payara/target/stage/payara5/bin/asadmin"
                }
                sh "${ASADMIN} create-domain --nopassword ${DOMAIN_NAME}"
                sh "${ASADMIN} start-domain ${DOMAIN_NAME}"
                sh "${ASADMIN} start-database --dbtype derby || true"
            }
        }
        stage('Run EE7 Tests') {
            steps {
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                sh """mvn -B -V -ff -e clean install -Dsurefire.useFile=false \
                -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/jre/lib/security/cacerts \
                -Djavax.xml.accessExternalSchema=all -Dpayara.version=${pom.version} \
                -Dpayara.directory.name=payara5 \
                -Dpayara.version.major=5 -Ppayara-server-remote,stable"""
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
            }
            post {
                always {
                    echo 'tidying up after tests:'
                    sh "${ASADMIN} stop-domain ${DOMAIN_NAME}"
                    sh "${ASADMIN} stop-database --dbtype derby || true"
                    junit '**/target/surefire-reports/*.xml'
                    sh "${ASADMIN} delete-domain ${DOMAIN_NAME}"
                }
            }
        }
    }
}
