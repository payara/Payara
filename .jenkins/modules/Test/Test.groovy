MPLPostStep('always') {
    echo "Cleaning up workspace"
    cleanWs()
}

MPLPostStep('failure') {
    echo "There are test failures, archiving server log as ${CFG.suite.suite_name}.log"
    sh "cp ./${getPayaraDirectoryName(CFG.'build.version')}/glassfish/domains/${CFG.domain_name}/logs/server.log ./${CFG.suite.suite_name}.log"
    archiveArtifacts artifacts: "./${CFG.suite.suite_name}.log"
}

// Perform suite specific test execution
if(CFG.suite.suite_name.equals("Payara-Samples")) {
    MPLModule('Payara Samples Test', CFG)
} else {
    MPLModule('Quicklook Test', CFG)
}
