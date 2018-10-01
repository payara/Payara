#!groovy
//in repo Jenkinsfile
def pom
def DOMAIN_NAME='test-domain'
def ASADMIN
def payaraBuildNumber
pipeline {
    parameters{
        choice(
            choices: '8\n7',
            description: 'Which JDK version you wish to build and test with?',
            name: 'jdkVer')
        booleanParam(
            defaultValue: false,
            description: 'Is this Job being triggered by itself?',
            name: 'isRecursive')
    }
    agent any
    stages {
        stage('quick dev stage - delete me.') {
            steps{
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Checking out cargoTracker tests  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                checkout changelog: false, poll: false, scm: [$class: 'GitSCM',
                    branches: [[name: "46509072b2695967a921d1af579a686c7b35b52d"]],
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
            }
        }
        stage('report') {
            steps {
                script{
                    pom = readMavenPom file: 'pom.xml'
                    payaraBuildNumber = "PR${env.CHANGE_ID}#${currentBuild.number}"
                    echo "Payara pom version is ${pom.version}"
                    echo "Build number is ${payaraBuildNumber}"
                    echo "jdkVer = ${jdkVer}"
                    echo "Recursive = ${isRecursive}"
                }
            }
        }
        stage('Build') {
            tools {
                jdk "zulu-${jdkVer}"
            }
            environment {
                MAVEN_OPTS=getMavenOpts()
            }
            steps {
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Building SRC  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                sh """mvn -V -ff -e clean install -PBuildExtras \
                -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/jre/lib/security/cacerts \
                -Djavax.xml.accessExternalSchema=all -Dbuild.number=${payaraBuildNumber}"""
                echo '*#*#*#*#*#*#*#*#*#*#*#*#    Built SRC   *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
            }
        }
        stage('Setup for Quicklook Tests') {
            tools {
                jdk "zulu-${jdkVer}"
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
                jdk "zulu-${jdkVer}"
            }
            environment {
                MAVEN_OPTS=getMavenOpts()
            }
            steps {
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                sh """mvn -V -ff -e clean test \
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
        stage('Checkout EE8 Tests') {
            when{
                expression{ getMajorVersion(pom.version) != '4' }
            }
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
        stage('Run EE8 Tests') {
            when{
                expression{ getMajorVersion(pom.version) != '4' }
            }
            tools {
                jdk "zulu-${jdkVer}"
            }
            environment {
                MAVEN_OPTS=getMavenOpts()
            }
            steps {
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                sh "mvn -V -ff -e clean install -Dsurefire.useFile=false -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/jre/lib/security/cacerts -Djavax.xml.accessExternalSchema=all -Dpayara.version=${pom.version} -Dpayara.directory.name=${getPayaraDirectoryName(pom.version)} -Dpayara.version.major=${getMajorVersion(pom.version)} -Ppayara-ci-managed,stable"
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }
        stage('Checkout cargoTracker Tests') {
            steps{
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Checking out cargoTracker tests  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                checkout changelog: false, poll: false, scm: [$class: 'GitSCM',
                    branches: [[name: "46509072b2695967a921d1af579a686c7b35b52d"]],
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
                jdk "zulu-${jdkVer}"
            }
            environment {
                MAVEN_OPTS=getMavenOpts()
            }
            steps {
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                sh """mvn -V -ff -e clean install -Dsurefire.useFile=false \
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
        stage('Run EE7 Tests') {
            tools {
                jdk "zulu-${jdkVer}"
            }
            environment {
                MAVEN_OPTS=getMavenOpts()
            }
            steps {
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                sh """mvn -V -ff -e clean install -Dsurefire.useFile=false \
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
        stage('Run JDK7 Too'){
            when{
                expression{ getMajorVersion(pom.version) == '4'  && !isRecursive}
            }
            steps{
                //Not sure if this is going to work. Trying anayway.
                build( job: "${env.JOB_NAME}",
                    propagate: false, //whether to propogate errors. Note UNSTABLE propogates as FAILURE
                    wait: true,  //wait for this job to finish before proceeding
                    parameters: [[$class: 'StringParameterValue', name: 'jdkVer', value: jdkVer],
                                [$class: 'BooleanParameterValue', name: 'isRecursive', value: true]])
            }
        }
    }
}
def String getMavenOpts() {
    def mavenOpts = '';
    if('7'.equalsIgnoreCase(params.jdkVer)){
      mavenOpts= mavenOpts + ' -Xmx1024M -XX:MaxPermSize=512m';
    }
    return mavenOpts;
}
def String getMajorVersion(fullVersion) {
    if (fullVersion.startsWith("4")) {
        return "4"
    }else if (fullVersion.startsWith("5")) {
        return "5"
    }else{
        error("unknown major version. Please check pom version")
    }
}
def String getPayaraDirectoryName(fullVersion) {
    if (fullVersion.startsWith("4")) {
        return "payara41"
    }else if (fullVersion.startsWith("5")) {
        return "payara5"
    }else{
        error("unknown major version. Please check pom version")
    }
}
