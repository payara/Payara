This is a simple Paas test for Oracle Database. The test creates 2 tables DEMO_TABLE and BOOKS_TABLE. DEMO_TABLE is used to update and get the last accessed time of the database. BOOKS_TABLE is used to add more books into the oracle database table and display them as a list in the Servlet. The application also displays the version of the Oracle database that is used for the test.

Steps to run this test:
-----------------------

1. Unzip latest version of glassfish.zip and set S1AS_HOME enviroment variable to point to the extracted GlassFish location.

  For example: export S1AS_HOME=/tmp/glassfish3/glassfish

1.1. (Optional for now) Make deployment lock changes and put in $S1AS_HOME/modules.

2. Copy all the jar files from main/appserver/paas and main/nucleus/paas to $S1AS_HOME/modules, something like this in unix:

  cd $WS_HOME/main/appserver/paas
  cp `find . -type f -name "*.jar" | grep -v sources` $S1AS_HOME/modules/
  cd $WS_HOME/main/nucleus/paas
  cp `find . -type f -name "*.jar" | grep -v sources` $S1AS_HOME/modules/

3. Copy the Oracle database plugin jars into $S1AS_HOME/modules.

4. rm -f $S1AS_HOME/modules/paas.javadbplugin.jar 

5. Copy Oracle jdbc driver (ojdbc14.jar) into $S1AS_HOME/domains/domain1/lib. Ref: http://download.oracle.com/otn/utilities_drivers/jdbc/10205/ojdbc14.jar

6. Follow instructions at http://aseng-wiki.us.oracle.com/asengwiki/display/GlassFish/Oracle+2.2+VM+GlassFish+Plugin

7. [Optional] Setup virtualization enviroment for your GlassFish installation. 

   For example, modify kvm_setup.sh/native_setup.sh/ovm_setup.sh to suit your system details and run it.

This step is optional in which case the service(s) required for this PaaS app will be provisioned in non-virtualized environment.

8. Compile this test : mvn clean compile war:war

9. Deploy the war : asadmin deploy /tmp/basic_db_paas_sample.war
 
Note : Since the unprovisioning is not clean as of now, so in order to re-run the test it is recommended to delete the GF installation and start fresh from step (1). Also make sure all ASMain processes are killed before you start.

