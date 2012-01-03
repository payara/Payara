Steps to run these automated tests:
-----------------------------------

1. Unzip latest version of glassfish.zip and set S1AS_HOME enviroment variable to point to the extracted GlassFish location.

  For example: export S1AS_HOME=/tmp/glassfish3/glassfish

2. Also set PAAS_TESTS_HOME environment variable to point to the location where paas tests are checked out.

 For example: export PAAS_TESTS_HOME=/tmp/main/appserver/tests/paas

3. Setup virtualization enviroment for your GlassFish installation. 

   For example, run native_setup.sh to configure native IMS config. 

4. When the load balancer is used, specify the load balancer's port 50080'..eg., -DargLine="-Dhttp.port=50080" 

   GF_EMBEDDED_ENABLE_CLI=true mvn clean verify surefire-report:report -DargLine="-Dhttp.port=50080"

   Without lb-plugin just skip the argument part.Deafult port of 28080 will be used.

   GF_EMBEDDED_ENABLE_CLI=true mvn clean verify surefire-report:report

5. When multiple database plugins are present in the modules directory, to register a particular database plugin as the default service provisioning engine, use the register-service-provisioning-engine command. For example,

asadmin register-service-provisioning-engine --type Database --defaultservice=true org.glassfish.paas.mysqldbplugin.MySQLDBPlugin

or

asadmin register-service-provisioning-engine --type Database --defaultservice=true org.glassfish.paas.javadbplugin.DerbyPlugin
