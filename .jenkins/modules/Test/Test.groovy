MPLModulePostStep('always') {
    echo "Cleaning up the workspace"
    sh "${pwd()}/${getPayaraDirectoryName(CFG.'build.version')}/bin/asadmin stop-domain ${CFG.domain_name}"
    cleanWs()
}

MPLModulePostStep('failure') {
    echo "There are test failures, archiving server log"
    archiveArtifacts artifacts: "./${${getPayaraDirectoryName}}/glassfish/domains/${getDomainName()}/logs/server.log"
}

// Perform suite specific test execution
if(CFG.suite.suite_name.equals("Payara-Samples")) {
    MPLModule('Payara Samples Test', CFG)
} else {
    MPLModule('Quicklook Test', CFG)
}
