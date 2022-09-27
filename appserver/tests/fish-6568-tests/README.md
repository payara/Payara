# FISH-6568

This is the reproducer project for the [FISH-6568](https://payara.atlassian.net/browse/FISH-6568) issue.

It deploys a test WAR exposing a simple JAX-RS API on a Payara Micro 5 server. This simple API uses JPA to persist a test entity into an Oracle 11g XE database.

In order to guarantee predictability, the sample is using testcontainers to instantiate and run two Docker containers, as follows:

- a Docker container running an Oracle 11g XE database image;
- a Docker container running a Payara Micro 5.2022.3-JDK11 image.

An integration test is ran in the verify phase of Maven by the failsafe plugin. It allows to reproduce the issue, when present.

Given the indiscriminate nature of the issue, its reproducibility isn't guarantee. During a two days testing period, I only succeeded to reproduce it once, for several minutes.

In order to run the sample, execute the command mvn clean install. This will execute the integration test which will instantiate the two containers and will deploy the test WAR on the Payara Micro instance. Then, the JAX-RS API will be called and will persiste data into the Oracle databases.

The integration test checks wether:

- the two Docker containers are up and running
- the deployment of teh WAR archive succeeds without raising SAX parser exceptions
- the JPA operation are performed as expected.

This sample requires, of course, a Docker daemon runningon the local box.
