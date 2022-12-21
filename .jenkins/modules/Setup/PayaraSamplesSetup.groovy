echo 'Installing Payara for Internals'

withMaven(jdk: CFG.jdk, publisherStrategy: 'EXPLICIT') {
    sh 'mvn install -DskipTests'
}