#!groovy
// Jenkinsfile for building a PR and running a subset of tests against it
pipeline {
    agent any
    tools {
        jdk "zulu-8"
    }
    stages {
        stage('My Stage') {
            steps {
                echo '*#*#*#*#*#*#*#*#*#*#*#*# MyStage Start  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
                echo '*#*#*#*#*#*#*#*#*#*#*#*#    MyStage Stop   *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
            }
            post{
                always{
					testFailure()
                }
            }
        }
    }
}

void testFailure() {
    echo 'AAA'
    sh "true"
    echo 'BBB'
    sh "false"
    echo 'CCC'
    sh "true"
	echo 'DDD'
}
