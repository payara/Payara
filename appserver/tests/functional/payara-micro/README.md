# Payara Micro Tests

Starts 2 instances of Payara Micro

On each instance is deployed ClusterJsp. This application should cluster the different instances and share the httpsession.

Open the first instance in a browser, add an attribute to the httpsession with a value.

Open the second instance in a browser, add an attribute to the httpsession with a value, and check for the presence of the first attribute.

Open the first instance in a browser, check for the presence of the second attribute.


## run the tests:

This test depends on some utilities from payara-samples. So it requires to build payara-samples before running this test.

It needs to be run in managed mode, with the command

    mvn clean verify -Ppayara-micro-managed

This will run the test against the current version of Payara Micro, with Arquillian.

Alternatively, you can start specify the location of a different jar for Payara Micro:

    mvn clean verify -Ppayara-micro-managed -Dpayara.microJar=<path>

## Playwright dependencies

By default, this test will check and install Playwright dependencies before running the test. 
The user needs to have the permissions to install such dependencies. 
Alternatively, dependencies can be installed manually. 
Playwright needs the following libraries to run:
    libatk1.0-0
    libatk-bridge2.0-0
    libcups2
    libxkbcommon0
    libatspi2.0-0
    libxcomposite1
    libxdamage1
    libxfixes3
    libxrandr2
    libgbm1
    libpango-1.0-0
    libcairo2
and to run the test without installing the dependencies with maven and Playwright CLI command:
    mvn clean verify -Ppayara-micro-managed -P!install-deps