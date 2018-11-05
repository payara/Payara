#!groovy
// Jenkinsfile for building a PR and running a subset of tests against it
def pom
def DOMAIN_NAME
def ASADMIN
def payaraBuildNumber
def mavenOpts='-Xmx1024M -XX:MaxPermSize=512m'
pipeline {
    options {
        disableConcurrentBuilds()
    }
    agent any
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
                setupDomain()
            }
        }
        stage('Run Quicklook Tests') {
            tools {
                jdk "zulu-8"
            }
            steps {
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                sh """mvn -B -V -ff -e clean test \
                -Dglassfish.home=\"${pwd()}/appserver/distributions/payara/target/stage/payara41/glassfish\" \
                -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/jre/lib/security/cacerts \
                -Djavax.xml.accessExternalSchema=all \
                -f appserver/tests/quicklook/pom.xml"""
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
            }
            post {
                always {
                    teardownDomain()
                }
                unstable {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }
        stage('Checkout cargoTracker Tests') {
            steps{
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Checking out cargoTracker tests  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                checkout changelog: false, poll: false, scm: [$class: 'GitSCM',
                    branches: [[name: "*/jenkins"]],
                    extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'cargotracker']],
                    userRemoteConfigs: [[url: "https://github.com/payara/cargoTracker.git"]]]
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Checked out cargoTracker tests  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
            }
        }
        stage('Run cargoTracker Tests') {
            tools {
                jdk "zulu-8"
            }
            steps {
                dir('cargotracker') {
                    echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                    sh """mvn -B -V -ff -e clean install -Dsurefire.useFile=false \
                    -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/jre/lib/security/cacerts \
                    -Djavax.xml.accessExternalSchema=all -Dpayara.version=${pom.version} \
                    -Ppayara-server-managed,payara4"""
                    echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                }
            }
            post {
                unstable {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }
        stage('Checkout EE7 Tests') {
            steps{
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Checking out EE7 tests  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                checkout changelog: false, poll: false, scm: [$class: 'GitSCM',
                    branches: [[name: "*/jenkins"]],
                    extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'EE7-Samples']],
                    userRemoteConfigs: [[url: "https://github.com/payara/patched-src-javaee7-samples.git"]]]
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Checked out EE7 tests  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
            }
        }
        stage('Setup for EE7 Tests') {
            tools {
                jdk "zulu-8"
            }
            steps {
                setupDomain()
            }
        }
        stage('Run EE7 Tests') {
            tools {
                jdk "zulu-8"
            }
            steps {
                dir('EE7-Samples') {
                    echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                    sh """mvn -B -V -ff -e clean install -Dsurefire.useFile=false \
                    -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/jre/lib/security/cacerts \
                    -Djavax.xml.accessExternalSchema=all -Dpayara.version=${pom.version} \
                    -Dpayara_domain=${DOMAIN_NAME} -Duse.cnHost=true \
                    -Ppayara-server-remote,stable,payara4"""
                    echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                }
            }
            post {
                always {
                    teardownDomain()
                }
                unstable {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }
        stage('Build JDK7') {
            tools {
                jdk "zulu-7"
            }
            environment {
                MAVEN_OPTS="${mavenOpts}"
            }
            steps {
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Building SRC JDK7  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                sh """mvn -B -V -ff -e clean install -PQuickBuild \
                -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/jre/lib/security/cacerts \
                -Djavax.xml.accessExternalSchema=all -Dbuild.number=${payaraBuildNumber}"""
                echo '*#*#*#*#*#*#*#*#*#*#*#*#    Built SRC JDK7  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
            }
        }
        stage('Setup for Quicklook Tests JDK7') {
            tools {
                jdk "zulu-7"
            }
            environment {
                MAVEN_OPTS="${mavenOpts}"
            }
            steps {
                setupDomain()
            }
        }
        stage('Run Quicklook Tests JDK7') {
            tools {
                jdk "zulu-7"
            }
            environment {
                MAVEN_OPTS="${mavenOpts}"
            }
            steps {
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                sh """mvn -B -V -ff -e clean test \
                -Dglassfish.home=\"${pwd()}/appserver/distributions/payara/target/stage/payara41/glassfish\" \
                -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/jre/lib/security/cacerts \
                -Djavax.xml.accessExternalSchema=all \
                -f appserver/tests/quicklook/pom.xml"""
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
            }
            post {
                always {
                    teardownDomain()
		}
                unstable {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }
        stage('Run cargoTracker Tests JDK7') {
            tools {
                jdk "zulu-7"
            }
            environment {
                MAVEN_OPTS="${mavenOpts}"
            }
            steps {
                dir('cargotracker') {
                    echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                    sh """mvn -B -V -ff -e clean install -Dsurefire.useFile=false \
                    -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/jre/lib/security/cacerts \
                    -Djavax.xml.accessExternalSchema=all -Dpayara.version=${pom.version} \
                    -Ppayara-server-managed,payara4"""
                    echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                }
            }
            post {
                unstable {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }
        stage('Setup for EE7 Tests JDK7') {
            tools {
                jdk "zulu-7"
            }
            steps {
                setupDomain()
            }
        }
        stage('Run EE7 Tests JDK7') {
            tools {
                jdk "zulu-7"
            }
            environment {
                MAVEN_OPTS="${mavenOpts}"
            }
            steps {
                dir('EE7-Samples') {
                    echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                    sh """mvn -B -V -ff -e clean install -Dsurefire.useFile=false \
                    -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/jre/lib/security/cacerts \
                    -Djavax.xml.accessExternalSchema=all -Dpayara.version=${pom.version} \
                    -Dpayara_domain=${DOMAIN_NAME} -Duse.cnHost=true \
                    -Ppayara-server-remote,stable,payara4"""
                    echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                }
            }
            post {
                always {
                    teardownDomain()
                }
                unstable {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }
    }
}
def void setupDomain() {
    echo '*#*#*#*#*#*#*#*#*#*#*#*#  Setting up tests  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
    script{
        ASADMIN = "./appserver/distributions/payara/target/stage/payara41/bin/asadmin"
        DOMAIN_NAME = "test-domain"
    }
    sh "${ASADMIN} create-domain --nopassword ${DOMAIN_NAME}"
    sh "${ASADMIN} start-domain ${DOMAIN_NAME}"
    sh "${ASADMIN} start-database --dbtype derby || true"
}
def void teardownDomain() {
    echo 'tidying up after tests:'
    sh "${ASADMIN} stop-domain ${DOMAIN_NAME}"
    sh "${ASADMIN} stop-database --dbtype derby || true"
    sh "${ASADMIN} delete-domain ${DOMAIN_NAME}"
}
