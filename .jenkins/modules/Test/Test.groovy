MPLPostStep('always') {
    echo "Cleaning up workspace"
    cleanWs()
}

MPLPostStep('failure') {
    echo "There are test failures, archiving server log"
    archiveArtifacts artifacts: "./${getPayaraDirectoryName}/glassfish/domains/${getDomainName()}/logs/server.log"
}

// Perform suite specific test execution
if(CFG.suite.suite_name.equals("Payara-Samples")) {
    MPLModule('Payara Samples Test', CFG)
} else {
    MPLModule('Quicklook Test', CFG)
}
