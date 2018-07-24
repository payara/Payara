pipeline {
    agent none
    stages {
        stage("Analyse") {
            agent {
                label "sonar"
            }
            tools {
                jdk "zulu-8"
            }
            steps {
                echo "Analysing"
                checkoutAndBuildSource()
            }
        }
    }
   post {
        always {
            hipchatSend (color: 'GRAY', credentialId: 'hipchat2', textFormat: false, message: "${env.JOB_NAME} build # ${env.BUILD_NUMBER} Completed. See <a href='${BUILD_URL}'>Jenkins</a>")
        }
    } 
}

def checkoutAndBuildSource(){
    echo 'JAVA_HOME = ' + JAVA_HOME
    prNo = env.BRANCH_NAME
    script{
        dir('src'){
            deleteDir()
        }
    }
    checkout changelog: false, 
      poll: false, 
      scm: [$class: 'GitSCM', 
      branches: [[name: "*/master"]], 
      doGenerateSubmoduleConfigurations: false, 
      extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'src']], 
    submoduleCfg: [], 
    userRemoteConfigs: [[url: 'https://github.com/payara/Payara.git']]]

    withCredentials([[$class: 'StringBinding', credentialsId: 'jenkins-held-github-api-token-secret', variable: 'githubToken']
                    [$class: 'StringBinding', credentialsId: 'jenkins-held-sonarcloud-token-secret', variable: 'sonarToken']]) {
        dir('src') {
            sh """mvn clean package \
            -DskipTests \
            -Dsonar.organization=payara \
            -Dsonar.host.url=https://sonarcloud.io \
            -Dsonar.pullrequest.provider=github \
            -Dsonar.analysis.mode=preview \
            -Dsonar.pullrequest.github.repository=payara/Payara \
            -Dsonar.pullrequest.github.endpoint=https://api.github.com/ \
            -Dsonar.pullrequest.branch=${env.BRANCH_NAME} \
            -Dsonar.pullrequest.key=${prNo} \
            -Dsonar.pullrequest.base=master \
            -Dsonar.github.oauth=${githubToken} \
            -Dsonar.login=${sonarToken} \
            sonar:sonar"""
        }
    }
}
