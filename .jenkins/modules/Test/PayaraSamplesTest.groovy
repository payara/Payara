MPLPostStep('always') {
    sh "${pwd()}/payara/bin/asadmin stop-domain ${CFG.domain_name}"
    cleanWs()
}

withMaven(jdk: CFG.jdk, options: [artifactsPublisher(disabled: true)]) {
    sh """mvn -V -B -ff clean install --strict-checksums -Ppayara-server-remote \
        -Dpayara.version=${pom.version} \
        -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/jre/lib/security/cacerts \
        -Djavax.xml.accessExternalSchema=all \
        -f appserver/tests/payara-samples """
}