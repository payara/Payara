Steps to run these automated tests:
-----------------------------------

1. Unzip latest version of glassfish.zip and set S1AS_HOME enviroment variable to point to the extracted GlassFish location.

  For example: export S1AS_HOME=/tmp/glassfish3/glassfish

2. Setup virtualization enviroment for your GlassFish installation.

   For example, run native_setup.sh to configure native IMS config. Modify kvm_setup.sh to suite your system details and run it.

3. When the load balancer is used, specify the load balancer's port 50080'..eg., -DargLine="-Dhttp.port=50080" 

   GF_EMBEDDED_ENABLE_CLI=true mvn clean verify surefire-report:report -DargLine="-Dhttp.port=50080"

   Without lb-plugin just skip the argument part.Deafult port of 28080 will be used.

   GF_EMBEDDED_ENABLE_CLI=true mvn clean verify surefire-report:report
