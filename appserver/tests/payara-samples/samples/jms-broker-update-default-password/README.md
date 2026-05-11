# Payara Samples: Testing JMS Broker default password usage#

This sample shows how to test the default configuration used by the server when starting the default JMS broker instance

To test, you need to use the option: `-Ppayara-server-remote` to call the jms-ping command and create the default structure of the broker instance

The first step is to start the Payara Server on your environment:

```shell
./asadmin start-domain
```

then execute the following command on a different prompt under the current folder:`./jms-broker-update-default-password`

then in a different prompt window execute the following command:

```shell
mvn verify -Dpayara.version=7.2026.6-SNAPSHOT -Dpayara.home=/home/alfv83/projects/Payara/appserver/distributions/payara/target/stage/payara7 -DskipJmsBrokerTest=false -Ppayara-server-remote
```

By default, the property `skipJmsBrokerTest` is set as true to skip this test and not affect the other sample executions and for the `payara.version` property use the current server version installed on your system.