# FISH-6470

The Jira ticket having the ID FISH-6470 is reporting an issue concerning the availability of some of the Java 8 cryptographic algorithms, especially the GCM (*Galois Counter Mode*) ones.

The initial description of this issue, as presented by the Jira ticket, was somehow vague and ambiguous and following its scenario didn't allow to reproduce the issue. A refining work of testing and qualification was necessary in order to finnaly come to a more accurate test scenario, able to systematically reproduce the issue.

The present project was used to test the availability and the support of the Java standard cryptographic algorithms on the Payara Platform 5. It consists of the following Maven modules:

* the master POM module named `fish-6470-tests`
* the Maven module named `infra`. This module uses the `docker-composer` maven plugin in order to start two `docker` containers, one running the Payara Micro 5 instance and the another one running the Payara Server 5 instance.
* the Maven module named `micro`. This module exposes a simple JAX-RS endpoint and checks that all the expected Java standard cipher suites are avilable once this endpoint is deployed on the Payara Micro 5 running instance.
* the Maven module named `server`. This module exposes a simple JAX-RS endpoint and checks that all the expected Java standard cipher suites are avilable once this endpoint is deployed on the Payara Server 5 running instance.

In order to run the tests proceed as follows:

```
mvn -DskipTests clean install
mvn verify
```

The first Maven command is instantiating the Docker containers from their standard images, and deploys the test WAR on the running instances. The second Maven command runs the integration tests and displays the results.

An unit tests has been provided as well in the package containing the fix (`org.glassfish.nucleus`). Its name is `JceCipherSuitesTest` and it checks the new method `getSupportedCipherSuites()`, which has been added to the class `org.glassfish.admin.mbeanserver.ssl.SSLClientConfigurator` in order to fix the issue.
