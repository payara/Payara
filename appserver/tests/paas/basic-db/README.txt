Steps to run this test:
-----------------------

1. Unzip latest version of glassfish.zip and set S1AS_HOME enviroment variable to point to the extracted GlassFish location.

  For example: export S1AS_HOME=/tmp/glassfish3/glassfish

2. Copy all the jar files from main/appserver/paas and main/nucleus/paas to $S1AS_HOME/modules, something like this in unix:

  cd $WS_HOME/main/appserver/paas
  cp `find . -type f -name "*.jar" | grep -v sources` $S1AS_HOME/modules/
  cd $WS_HOME/main/nucleus/paas
  cp `find . -type f -name "*.jar" | grep -v sources` $S1AS_HOME/modules/

3. [Optional] Setup virtualization enviroment for your GlassFish installation. 

   For example, modify kvm_setup.sh/native_setup.sh to suite your system details and run it.

This step is optional in which case the service(s) required for this PaaS app will be provisioned in non-virtualized environment.

Restart domain after this step.

4. Compile this test : mvn clean compile war:war

5. Deploy the war : asadmin deploy /tmp/basic_db_paas_sample.war
 
Note : Since the unprovisioning is not clean as of now, so in order to re-run the test it is recommendded to delete the GF installation and start fresh from step (1). Also make sure all ASMain processes are killed before you start.

