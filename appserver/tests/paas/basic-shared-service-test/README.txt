1. This is a simple PaaS Shared Service test.Re-uses the basic-jpa war with a few additions to the glassfish-services.xml
The test creates 2 shared service: 1 for lb and 1 for db. The application references these services.
The test basically aims at the following commands:
        i. create-shared-service
        ii. delete-shared-service
        iii. start-shared-service
        iv.stop-shared-service
 This test creates a table ZOO_DIRECTORY which displays the a list of animals when called  from the servlet.

2. The context root for this application is "/basic-shared-service-test"

To run the test do the following steps:


1. Unzip latest version of glassfish.zip and set S1AS_HOME enviroment variable to point to the extracted GlassFish location.

    For example: export S1AS_HOME=/tmp/glassfish3/glassfish
    1.a. Ensure that there are no instances of ASMain already running.


2. Setup virtualization enviroment for your GlassFish installation.

   For example, modify native_setup.sh to suite your system details and run it.

3.Ensure that lb.zip is in /tmp/glassfish3/glassfish/config
 If not,
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

 4. When the load balancer is used, specify the load balancer's port 50080'..eg., -DargLine="-Dhttp.port=50080"

   GF_EMBEDDED_ENABLE_CLI=true mvn clean verify surefire-report:report -DargLine="-Dhttp.port=50080"

   Without lb-plugin just skip the argument part.Deafult port of 28080 will be used.

   GF_EMBEDDED_ENABLE_CLI=true mvn clean verify surefire-report:report


