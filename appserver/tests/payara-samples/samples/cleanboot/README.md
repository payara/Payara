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