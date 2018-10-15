#!groovy
//in repo Jenkinsfile
def pom
def DOMAIN_NAME='test-domain'
def ASADMIN
def payaraBuildNumber
def mavenOpts='-Xmx1024M -XX:MaxPermSize=512m'
pipeline {
    options {
        disableConcurrentBuilds()
    }
    agent any
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
            tools {
                jdk "zulu-8"
            }
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
            tools {
                jdk "zulu-8"
            }
            steps {
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Setting up tests  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                script{
                    ASADMIN = "./appserver/distributions/payara/target/stage/${getPayaraDirectoryName(pom.version)}/bin/asadmin"
                }
                sh "${ASADMIN} create-domain --nopassword ${DOMAIN_NAME}"
                sh "${ASADMIN} start-domain ${DOMAIN_NAME}"
                sh "${ASADMIN} start-database --dbtype derby || true"
            }
        }
        stage('Run Quicklook Tests') {
            tools {
                jdk "zulu-8"
            }
            steps {
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                sh """mvn -B -V -ff -e clean test \
                -Dglassfish.home=\"${pwd()}/appserver/distributions/payara/target/stage/${getPayaraDirectoryName(pom.version)}/glassfish\" \
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
            tools {
                jdk "zulu-8"
            }
            steps {
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                sh """mvn -B -V -ff -e clean install -Dsurefire.useFile=false \
                -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/jre/lib/security/cacerts \
                -Djavax.xml.accessExternalSchema=all -Dpayara.version=${pom.version} \
                -Dpayara.directory.name=${getPayaraDirectoryName(pom.version)} \
                -Dpayara.version.major=${getMajorVersion(pom.version)} -Ppayara-ci-managed"""
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
                    branches: [[name: "*/JenkinsTest"]],
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
        stage('Run EE7 Tests') {
            tools {
                jdk "zulu-8"
            }
            steps {
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                sh """mvn -B -V -ff -e clean install -Dsurefire.useFile=false \
                -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/jre/lib/security/cacerts \
                -Djavax.xml.accessExternalSchema=all -Dpayara.version=${pom.version} \
                -Dpayara.directory.name=${getPayaraDirectoryName(pom.version)} \
                -Dpayara.version.major=${getMajorVersion(pom.version)} -Ppayara-ci-managed,stable"""
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }
        stage('Build JDK7') {
            tools {
                jdk "zulu-7"
            }
            environment {
                MAVEN_OPTS=mavenOpts
            }
            steps {
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Building SRC JDK7  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                sh """mvn -B -V -ff -e clean install -PQuickBuild \
                -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/jre/lib/security/cacerts \
                -Djavax.xml.accessExternalSchema=all -Dbuild.number=${payaraBuildNumber}"""
                echo '*#*#*#*#*#*#*#*#*#*#*#*#    Built SRC JDK7  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
            }
        }
        stage('Setup for Quicklook Tests') {
            tools {
                jdk "zulu-7"
            }
            environment {
                MAVEN_OPTS=mavenOpts
            }
            steps {
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Setting up tests  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                script{
                    ASADMIN = "./appserver/distributions/payara/target/stage/${getPayaraDirectoryName(pom.version)}/bin/asadmin"
                }
                sh "${ASADMIN} create-domain --nopassword ${DOMAIN_NAME}"
                sh "${ASADMIN} start-domain ${DOMAIN_NAME}"
                sh "${ASADMIN} start-database --dbtype derby || true"
            }
        }
        stage('Run Quicklook Tests JDK7') {
            tools {
                jdk "zulu-7"
            }
            environment {
                MAVEN_OPTS=mavenOpts
            }
            steps {
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                sh """mvn -B -V -ff -e clean test \
                -Dglassfish.home=\"${pwd()}/appserver/distributions/payara/target/stage/${getPayaraDirectoryName(pom.version)}/glassfish\" \
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
        stage('Run cargoTracker Tests JDK7') {
            tools {
                jdk "zulu-7"
            }
            environment {
                MAVEN_OPTS=mavenOpts
            }
            steps {
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                sh """mvn -B -V -ff -e clean install -Dsurefire.useFile=false \
                -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/jre/lib/security/cacerts \
                -Djavax.xml.accessExternalSchema=all -Dpayara.version=${pom.version} \
                -Dpayara.directory.name=${getPayaraDirectoryName(pom.version)} \
                -Dpayara.version.major=${getMajorVersion(pom.version)} -Ppayara-ci-managed"""
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
                    branches: [[name: "*/JenkinsTest"]],
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
        stage('Run EE7 Tests JDK7') {
            tools {
                jdk "zulu-7"
            }
            environment {
                MAVEN_OPTS=mavenOpts
            }
            steps {
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                sh """mvn -B -V -ff -e clean install -Dsurefire.useFile=false \
                -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/jre/lib/security/cacerts \
                -Djavax.xml.accessExternalSchema=all -Dpayara.version=${pom.version} \
                -Dpayara.directory.name=${getPayaraDirectoryName(pom.version)} \
                -Dpayara.version.major=${getMajorVersion(pom.version)} -Ppayara-ci-managed,stable"""
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }
    }
}
