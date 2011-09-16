This test only involves GlassFish service.

There is no DB or MQ or LB services required for this test.

Steps to run this test:
-----------------------

1. Unzip latest version of glassfish.zip and set S1AS_HOME enviroment variable to point to the extracted GlassFish location.

  For example: export S1AS_HOME=/tmp/glassfish3/glassfish

2. Setup virtualization enviroment for your GlassFish installation. 

   For example, run native_setup.sh to configure native IMS config. Modify kvm_setup.sh to suite your system details and run it.

4. GF_EMBEDDED_ENABLE_CLI=true mvn clean verify

