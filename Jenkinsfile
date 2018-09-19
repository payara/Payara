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
                sh 'printenv'
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Building SRC  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                sh "mvn -V -ff -e clean install -PBuildExtras -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/jre/lib/security/cacerts -Djavax.xml.accessExternalSchema=all"
                echo '*#*#*#*#*#*#*#*#*#*#*#*#    Built SRC   *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
            }
        }
        stage('Checkout Test') {
            tools {
                jdk "zulu-${jdkVer}"
            }
            environment {
                MAVEN_OPTS=getMavenOpts()
            }
            steps{
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Checking out test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                // checkout changelog: false, poll: false, scm: [$class: 'GitSCM',
                //     branches: [[name: "*/master"]],
                //     doGenerateSubmoduleConfigurations: false,
                //     extensions: [
                //         [$class: 'SubmoduleOption',
                //         disableSubmodules: false,
                //         parentCredentials: true,
                //         recursiveSubmodules: true,
                //         reference: '',
                //         trackingSubmodules: false]],
                //     submoduleCfg: [],
                //     userRemoteConfigs: [[url: "https://github.com/payara/patched-src-javaee8-samples.git"]]]
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Checked out test  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
            }
        }
    }
}
def String getMavenOpts(){
    def mavenOpts = '';
    if('7'.equalsIgnoreCase(params.jdkVer)){
      mavenOpts= mavenOpts + ' -Xmx1024M -XX:MaxPermSize=512m';
    }
    return mavenOpts;
}
