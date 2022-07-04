def ASADMIN = "${pwd()}/payara/bin/asadmin"

echo '*#*#*#*#*#*#*#*#*#*#*#*#  Setting up tests  *#*#*#*#*#*#*#*#*#*#*#*#*#*#*#'
makeDomain()
sh "${ASADMIN} start-domain ${CFG.domain_name}"
sh "${ASADMIN} start-database || true"