# Payara Micro Jar Based Integration Tests

This module provides plain JUnit tests that use `payara-micro.jar` as only
compile and runtime dependency (+ junit itself).

The setup makes sure the actual delivery is tested and keeps the setup simple
and minimal. Tests do the programmtic equivalent of:

        java -jar payara-micro.jar

The `payara-micro.jar` is referenced using a relative path to the module that
generates the jar. Should the jar not exist run: 

        cd $PAYARA_HOME/appserver/extras/payara-micro/payara-micro-distribution
        mvn install
        
Depending on the status of the build artifacts a full build might be required.

Except from being dependent on a generated jar file the tests in this module
are plain old JUnit tests that can be run using `mvn integration-test`
(or `mvn verify`) or in the IDE as every other JUnit tests.

While this takes full advantage of the simplicity Payara Micro has to offer for 
the user it comes with the downside of not having further APIs and internals 
available at compile time.
Instead reflection has to be used if internal should be verified. 
This module offers the `BeanProxy` utility class to ease reflection usage.
