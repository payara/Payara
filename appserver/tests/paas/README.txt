Steps to run these automated tests:
-----------------------------------

1. Unzip latest version of glassfish.zip and set S1AS_HOME enviroment variable to point to the extracted GlassFish location.

  For example: export S1AS_HOME=/tmp/glassfish3/glassfish

2. Setup virtualization enviroment for your GlassFish installation. 

   For example, run native_setup.sh to configure native IMS config. 

3. GF_EMBEDDED_ENABLE_CLI=true mvn clean verify

