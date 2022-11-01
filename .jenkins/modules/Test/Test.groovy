MPLPostStep('always') {
    echo "Cleaning up the workspace"
    sh "${pwd()}/${getPayaraDirectoryName(CFG.'build.version')}/bin/asadmin stop-domain ${CFG.domain_name}"
    cleanWs()
}

MPLPostStep('failure') {
    echo "There are test failures, archiving server log"
    archiveArtifacts artifacts: "./${${getPayaraDirectoryName}}/glassfish/domains/${getDomainName()}/logs/server.log"
}

error("Temporary forced failure to test failure post step behaviour")

// Perform suite specific test execution
if(CFG.suite.suite_name.equals("Payara-Samples")) {
    MPLModule('Payara Samples Test', CFG)
} else {
    MPLModule('Quicklook Test', CFG)
}
