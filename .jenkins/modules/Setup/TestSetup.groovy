withEnv(["JAVA_HOME=${tool CFG.jdk}"]) {
    // Perform the base behaviour
    MPLModule('Test Setup', CFG);

    def ASADMIN = "${pwd()}/${getPayaraDirectoryName(CFG.'build.version')}/bin/asadmin"

    // Payara Samples specific setup
    if(CFG.suite.suite_name.equals("Payara-Samples")) {
        MPLModule('Payara Samples Setup', CFG)
    }

    echo '*#*#*#*#*#*#*#*#*#*#*#*#  Setting up tests  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
    sh "${ASADMIN} create-domain --nopassword ${CFG.domain_name}"
    sh "${ASADMIN} start-domain ${CFG.domain_name}"
    sh "${ASADMIN} start-database || true"
}