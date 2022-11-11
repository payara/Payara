MPLPostStep('always') {
    echo "Cleaning up workspace"
    cleanWs()
}

MPLPostStep('failure') {
    echo "There are test failures, archiving server logs to ${CFG.suite.suite_name}-Logs.zip"
    sh "cp -R ./${getPayaraDirectoryName(CFG.'build.version')}/glassfish/domains/${CFG.domain_name}/logs ./${CFG.suite.suite_name}-Logs"
    archiveArtifacts artifacts: "${CFG.suite.suite_name}-Logs"
}

// Perform suite specific test execution
if(CFG.suite.suite_name.equals("Payara-Samples")) {
    MPLModule('Payara Samples Test', CFG)
} else {
    MPLModule('Quicklook Test', CFG)
}
