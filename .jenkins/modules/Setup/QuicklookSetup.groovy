def ASADMIN = "${pwd()}/${getPayaraDirectoryName(CFG.'build.version')}/bin/asadmin"
sh "rm -rf ~/'test|sa.mv.db'"
echo '*#*#*#*#*#*#*#*#*#*#*#*#  Setting up tests  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
sh "${ASADMIN} create-domain --nopassword ${CFG.domain_name}"
sh "${ASADMIN} start-domain ${CFG.domain_name}"
sh "${ASADMIN} start-database || true"
