MPLPostStep('always') {
    sh "${pwd()}/${getPayaraDirectoryName(CFG.'build.version)}/bin/asadmin stop-domain ${CFG.domain_name}"
    cleanWs()
}

// Perform suite specific test execution
if(CFG.suite.equals("Payara-Samples")) {
    MPLModule('Payara Samples Test', CFG)
} else {
    MPLModule('Quicklook Test', CFG)
}