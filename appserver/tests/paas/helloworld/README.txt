This is a simple Hello World application actually picked up from quicklook helloworld cluster test and cloud-enabled to run on a 2 instance GlassFish cluster. The application has been tested on OVM and native mode.

Steps to run this test:
-----------------------

1. Unzip latest version of glassfish.zip and set S1AS_HOME enviroment variable to point to the extracted GlassFish location.

  For example: export S1AS_HOME=/tmp/glassfish3/glassfish

Set S1AS_HOME to /tmp/glassfish3/glassfish.

2. [Optional] 

(Only for OVM mode)

Download and copy $VALUE_ADD_WS/virtualization/ovm/target/ovm.jar to $S1AS_HOME/modules directory.
Download and copy $VALUE_ADD_WS/virtualization/ovmws/target/ovmws.jar to $S1AS_HOME/modules directory.

Unzip $VALUE_ADD_WS/virtualization/ovm-files/target/ovm-files.zip under $S1AS_HOME/.. directory and configure network information:
-Edit the $S1AS_HOME/config/ovm/linux/ips file to add the list of static IPs followed by the hostnames you wish to use. The test needs at least 2 free static IPs.
-Edit the $S1AS_HOME/config/ovm/linux/network file to specify the network configuration (gateway, netmask, DNS etc.) of CPAS.

3. [Optional] Setup virtualization enviroment for your GlassFish installation. 

   Run native_setup.sh to run in native mode.
   Modify ovm_setup.sh to suit your system details (OVM connection string, pool name etc.) and run it.

This step is optional in which case the service(s) required for this PaaS app will be provisioned in non-virtualized environment.

4. [Optional] 

(Only for Native mode)

Restart domain. 

5. To manually test the app, do the following:

a) Compile this test : mvn clean compile war:war
b) Deploy the war : asadmin deploy /tmp/helloworld_sample.war
c) Access the application : http://<instance-ip1>:28080/helloworld_sample/hi.jsp
                            http://<instance-ip2>:28080/helloworld_sample/hi.jsp
d) Undeploy the application : asadmin undeploy helloworld_sample

6. To run the test automatically, run:

GF_EMBEDDED_ENABLE_CLI=true mvn clean verify surefire-report:report | tee run.log
 
Test Duration: It takes approx. 20min for a successful run on OVM setup.

Note : Make sure all ASMain processes are killed before this test is re-run.

