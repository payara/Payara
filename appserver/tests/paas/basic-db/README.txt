This is a Basic PaaS application to test JavaDB database. The test displays some values from a system table in JavaDB database. 

Steps to run this test:
-----------------------

1. Unzip latest version of glassfish.zip and set S1AS_HOME enviroment variable to point to the extracted GlassFish location.

  For example: export S1AS_HOME=/tmp/glassfish3/glassfish

Set S1AS_HOME to /tmp/glassfish3/glassfish.

2. 

(Only for Native mode) : 

Download lb.zip for appropriate platform.

    Windows - http://ejp-152x-232.india.sun.com/lb/windows/lb.zip
    Mac OS [ 64 bit ] - http://ejp-152x-232.india.sun.com/lb/macos/lb.zip
    Ubuntu [ 32 bit ] - http://ejp-152x-232.india.sun.com/lb/ubuntu/lb.zip
    Ubuntu [ 64 bit ] - http://ejp-152x-232.india.sun.com/lb/ubuntu-64/lb.zip

Copy downloaded lb.zip under /tmp. This will be copied to the $S1AS_HOME/config directory while doing (3).

(Only for OVM mode) :

Download and Copy http://ejp-152x-232.india.sun.com/paas.lbplugin-otd.jar to $S1AS_HOME/modules directory.
Download and Copy $VALUE_ADD_WS/virtualization/ovm/target/ovm.jar to $S1AS_HOME/modules directory.
Download and Copy $VALUE_ADD_WS/virtualization/ovmws/target/ovmws.jar to $S1AS_HOME/modules directory.

Unzip $VALUE_ADD_WS/virtualization/ovm-files/target/ovm-files.zip under $S1AS_HOME/.. directory.

Edit the $S1AS_HOME/config/ovm/linux/ips file to add the list of static IPs followed by the hostnames you wish to use. 
Edit the $S1AS_HOME/config/ovm/linux/network file for the network configuration of CPAS.

3. [Optional] Setup virtualization enviroment for your GlassFish installation. 

   For example, modify ovm_setup.sh/kvm_setup.sh/native_setup.sh to suite your system details and run it.

This step is optional in which case the service(s) required for this PaaS app will be provisioned in non-virtualized environment.

4. Restart domain after this step. (Only for native mode)

5. Compile this test : mvn clean compile war:war

6. Deploy the war : asadmin deploy /tmp/basic_db_paas_sample.war

7. Access the application : http://<lb-ip>:50080/basic_db_paas_sample

8. Undeploy the application : asadmin undeploy basic_db_paas_sample
 
Note : Make sure all ASMain processes are killed before this test is re-run.

