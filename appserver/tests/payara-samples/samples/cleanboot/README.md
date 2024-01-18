# Cleanboot File Tests

Boots the domain and test the admin console in a browser.

Creates an instance, starts it, checks the logs for any trace above INFO, stops the instance(s), deletes the instance(s)

Creates a deployment group, create instances for that group, starts the group, checks the logs for any trce above INFO, stops the instances, delete the instances, delete the group

Logs above INFO are printed in the output, to be assessed manually (some warnings are known issues and can be ignored). 

Any log above WARNING will fail the test.


## run the tests:

By default, it is run in managed mode, with the command

    mvn clean verify

This will run the test against the current version of Payara Server, with Arquillian.

Alternatively, you can start the domain indepedently and run the test in remote mode:

    mvn clean verify -Ppayara-server-remote

## Playwright dependencies

By default, this test will check and install Playwright dependencies before running the test. 
The user needs to have the permimssions to install such dependencies. 
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
    mvn clean verify -P!install-deps