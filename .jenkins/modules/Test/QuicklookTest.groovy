MPLPostStep('always') {
    testNG(reportFilenamePattern: 'appserver/tests/quicklook/**/*.xml')
}

withMaven(jdk: CFG.jdk, options: [artifactsPublisher(disabled: true)]) {
    sh """mvn -B -V -ff -e clean test --strict-checksums -Pall \
        -Dglassfish.home=\"${pwd()}/${getPayaraDirectoryName(CFG.'build.version')}/glassfish\" \
        -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/jre/lib/security/cacerts \
        -Djavax.xml.accessExternalSchema=all \
        -f appserver/tests/quicklook/pom.xml"""
}
