# Fish-372

FISH-372 is the Jira ID of an enhancement which aims at amending the way that some Payara Micro command line options are working, as follows:

* the `--nocluster` option. This option is an existent one and its current role is to disable the data grid feature. It should be amended such that to disable only the distributed data grid feature, while still allowing the use of the local data grid.
* the `--nohazelcast` option. This option doesn't exist in the current release of the Payara Micro. It will be introduced starting with the release 5.2022.3. Its role is to behave exactly like the `--nocluster` option used to behave previously, i.e. to completely the fata grid feature, be it local or remote.

During the Sprint #12, the Payara Micro code, starting with its 5.2022.3 release, has been modified such that to implement these requirements. A couple of integration tests have been added to the project payara-tests as well. These integration tests have been added to a new maven module, named `fish372-tests`. This was because some issues, related to the source code organization and its compliance with IDE conventions, were preventing their implementation in an existent maven module, like for example `micro-programmatic`, where they would probably belong naturally. These issues are presented in the associated Jira ticket.

The integration tests were subject to different scenarios. The scenario that has been considered in the beginning consisted in starting several instances of Payara Micro, using its API, and deploying to them a WAR, hosting a simple JAX-RS endpoint, performing simple operations with local and distributed caches. These simple operations are meant to be successful or to fail, depending on the combination of command line options, as described later in this document.

This scenario didn't work because using the Payara Micro API while having the Jakarta EE library on the classpath isn't supported by the platform (see FISH-1330). Hence, another scenario has been adopted, as follows:

* bind the `pre-integration-test` maven phase to the `payara-micro-maven-plugin start` goal;
* bind the `post-integration-test` maven phase to the `payara-micro-plugin stop` goal;
* execute the integration tests with the `failsafe` or `surefire` plugin.

While this scenario appears as working on the paper, in practice there are some issues, as follows:

* the goals `payara-micro:start` and `payara-micro:stop` aren't available on the maven command line and the raise:

```
[ERROR] No plugin found for prefix 'payara' in the current project and in the plugin groups [org.apache.maven.plugins, org.codehaus.mojo] available from the repositories [local (/home/nicolas/.m2/repository), payara-nexus-artifacts (https://nexus.payara.fish/repository/payara-artifacts), central (https://repo.maven.apache.org/maven2)] -> [Help 1]
```

* the payara-micro:start goal doesn't provide support to wait for the server start, meaning that, depending on the required time, the integration tests might be executed before the server starts.
* the payara-micro:stop goal raises:

`[ **ERROR** ] Error occurred while terminating payara-micro**java.io.IOException** : **Cannot run program "jps": error=2, No such file or directory**`

These issues have been reported during the sprint but no workarounds have been found.

Besides that, the required features are implemented and work as expected. This is proved by the integration tests provided by the class `PayaraMicroWithNoClusterIT`, as follows:

* `testWithNoClusterFalseShouldSucceed`: this test deploys the test WAR on an instance ran without any option. Hence calling the JAX-RS endpoint succeeds.
* `testWithNocClusterTrueShouldFail`: this test deploys the test WAR on an instance ran with the --nocluster option. Hence, calling the JAX-RS endpoint fails with HTTP 400 while trying to access a distributed data grid.
* `testWithNoHazelcastFalseShouldSucceed`: this test deploys the test WAR on an instance ran without any option. It creates a clustered HashMap and populates it with some data, then it gets the data from another instance. This tests succeeds.
* `testWithNoHazelcastTrueShouldFail`: this test deploys the test WAR on an instance ran with --nohazelcast option and, doing the same operations as the previous one, it fails with HTTP 500: `java.lang.IllegalStateException: ClusteredSingleton.getHazelcastInstance() - Hazelcast is Disabled`.

In order to run the integration tests a simple `mvn clean install` command isn't enough. This because of the synchronization issue between the Payara Micro server and the integration-test phase of maven, as explained above. Hence, a couple of scripts have been provided, such that to deal with that:

* run-tests.sh: runs maven while skipping the execution of the integration tests and the post-integration-test phase. This is meant to build the test WAR, to start the Payara Micro required instances and to leave them running, that's why the post-integration-test phase is skipped. Then the script runs the goal verify while skipping both the pre-integration-test phase, such as to avoid starting the instances again, and the post-integration-test phase, such as to avoid stopping the instances. Finally, the script runs the post-integration-test phase, such as to stop the instances, while skipping the pre-integration-test phase which, if ran, would start again the instances.
* stop-payara-instances.sh: this script gets the Payara Micro instances PIDs and stop the associated processes.

These scripts ar shell scripts but, given that they mostly perform maven commands, it's easy to provide the Windows equivalents. For CI testing, these scripts could be used in freestyle jobs or the associated maven commands should be used in maven jobs.
