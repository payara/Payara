This test verifies the enable and disable feature of a PaaS enabled application

Steps to run this test:
-----------------------

1. Unzip latest version of glassfish.zip and set S1AS_HOME enviroment variable to point to the extracted GlassFish location.

  For example: export S1AS_HOME=/tmp/glassfish3/glassfish

2. Setup virtualization enviroment for your GlassFish installation. 

   For example, modify native_setup.sh to suite your system details and run it.

3. Restart domain after this step.

4. execute : GF_EMBEDDED_ENABLE_CLI=true mvn clean verify surefire-report:report -DargLine="-Dhttp.port=50080"

Note : Make sure all ASMain processes are killed before this test is re-run.
