#!/bin/python3
import os
import subprocess
import time
import urllib.request
import testpayaralib

# Setup
testpayaralib.PAYARA_PATH_BIN = os.path.abspath(os.environ["PAYARA_HOME"] if "PAYARA_HOME" in os.environ else "../../../../appserver/distributions/payara/target/stage/payara7")
testpayaralib.ATTEMPTS = 30

errors = 0

# Check MP Config. mpconfigvalue1 is set via MP Config, mpconfigvalue2 is set via environment,
# both are read by MP Config
os.environ["mpconfigvalue2"] = "mpvalue2"
errors += testpayaralib.start_deploy_with_postboot("ConfigurableCDI", "ConfigurableCDI", "ConfigurableCDI-1.0-SNAPSHOT", "war", [
		["ConfigurableCDI-1.0-SNAPSHOT/", "Hello World!"],
		["ConfigurableCDI-1.0-SNAPSHOT/rest/ping", "pong"],
		["ConfigurableCDI-1.0-SNAPSHOT/rest/config/value1", "mpvalue1"],
		["ConfigurableCDI-1.0-SNAPSHOT/rest/config/value2", "mpvalue2"]
		],
 		["set-config-property --propertyName=mpconfigvalue1 --propertyValue=mpvalue1"
		])

# Test setup of JDBC Connection Pool via postbootcommand file
# TODO (when Payara will implement this feature): add url configuration via "set-config-property --propertyName=dburl --propertyValue=${com.sun.aas.instanceRoot}/lib/databases/testdb"
# then use url=${MPCONFIG=dburl}
errors += testpayaralib.start_deploy_with_postboot("H2SQLConnection", None, None, None, [
		["http://localhost:4848/management/domain/resources/list-jdbc-connection-pools", "testpool"]
		],
		[
		"create-jdbc-connection-pool --restype=java.sql.Driver --driverclassname='org.h2.jdbcx.JdbcDataSource' --property url=jdbc\\:h2\\:mem\\:test testpool",
		"ping-connection-pool testpool"])

print(str(errors)+ " errors.")
exit(errors)
