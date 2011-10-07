This is a simple Paas test for Oracle/JavaDB/MySQL Database. The test creates 2 tables DEMO_TABLE and BOOKS_TABLE. DEMO_TABLE is used to update and get the last accessed time of the database. BOOKS_TABLE is used to add more books into the database table and display them as a list in the Servlet. The application also displays the version of the database that is used for the test.

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


3. If you are using Oracle database, Copy the Oracle database plugin jars into $S1AS_HOME/modules.
If you are using MySQL database, copy the MySQL database plugin jars into $S1AS_HOME/modules directory.
Remove $S1AS_HOME/modules/paas.javadbplugin.jar if a Database other than JavaDB is used.

4. [Only for Oracle DB] Copy Oracle jdbc driver (ojdbc14.jar) into $S1AS_HOME/domains/domain1/lib. Ref: http://download.oracle.com/otn/utilities_drivers/jdbc/10205/ojdbc14.jar

5. [Optional] Setup virtualization enviroment for your GlassFish installation. 

   For example, modify kvm_setup.sh/native_setup.sh/ovm_setup.sh to suit your system details and run it.

This step is optional in which case the service(s) required for this PaaS app will be provisioned in non-virtualized environment.

6. Restart domain after this step. (Only for Native mode).

7. Compile this test : mvn clean compile war:war

8. Deploy the war : asadmin deploy /tmp/bookstore.war

9. Access the application : http://<lb-ip>:50080/bookstore

10. Undeploy the application : asadmin undeploy bookstore
 
Note : Make sure all ASMain processes are killed before this test is re-run.

