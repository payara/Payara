pipeline {
    agent any
    stages {
        stage("Analyse") {
            tools {
                jdk "zulu-8"
            }
            steps {
                echo "mvn -v"
            }
        }
    }     
}
