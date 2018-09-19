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
                sh 'echo $PATH'
                echo '*#*#*#*#*#*#*#*#*#*#*#*#  Building SRC  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                sh "mvn -V -U -ff -e clean install -PBuildExtras -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/jre/lib/security/cacerts -Djavax.xml.accessExternalSchema=all"
                echo '*#*#*#*#*#*#*#*#*#*#*#*#    Built SRC   *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
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
