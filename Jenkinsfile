#!groovy
//in repo Jenkinsfile
pipeline {
    agent any
    parameters {
        choice(
            choices: '8\n7',
            description: 'Which JDK version you wish to build and test with?',
            name: 'jdkVer')
    }
    stages {
        stage('Build') {
            tools {
                jdk "zulu-${jdkVer}"
            }
            environment {
                MAVEN_OPTS=getMavenOpts()
            }
            steps {
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Building SRC  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                sh "mvn -V -ff -e clean install -PBuildExtras -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/jre/lib/security/cacerts -Djavax.xml.accessExternalSchema=all"
                echo '*#*#*#*#*#*#*#*#*#*#*#*#    Built SRC   *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
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
        stage('Run Test') {
            tools {
                jdk "zulu-${jdkVer}"
            }
            environment {
                MAVEN_OPTS=getMavenOpts()
            }
            steps {
                script{
                    def pom = readMavenPom file: 'pom.xml'
                    echo "Payara pom version is ${pom.version}"
                    echo '*#*#*#*#*#*#*#*#*#*#*#*#  Running test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                    sh """mvn -V -ff -e clean install -Dsurefire.useFile=false -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/jre/lib/security/cacerts
                        -Djavax.xml.accessExternalSchema=all -Dpayara.version=${pom.version} -Dpayara.directory.name=${getPayaraDirectoryName(pom.version)}
                        -Dpayara.version.major=${getMajorVersion(pom.version)} -Ppayara-ci-managed,stable"""
                    echo '*#*#*#*#*#*#*#*#*#*#*#*#  Ran test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                }
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
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
