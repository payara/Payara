MPLPostStep('always') {
    sh "${pwd()}/payara/bin/asadmin stop-domain ${CFG.domain_name}"
    cleanWs()
}

withMaven(jdk: CFG.jdk, options: [artifactsPublisher(disabled: true)]) {
    sh """mvn -B -V -ff -e clean test --strict-checksums -Pall \
        -Dglassfish.home=\"${pwd()}/appserver/distributions/payara/target/stage/payara5/glassfish\" \
        -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/jre/lib/security/cacerts \
        -Djavax.xml.accessExternalSchema=all \
        -f appserver/tests/quicklook/pom.xml"""
}