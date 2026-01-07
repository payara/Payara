# Payara Samples

This repository contains small Payara specific samples for EE related/additional functionality.

## Troubleshooting ##

**Versions**
By default the test run an older version as configured as maven property in the root module.
To use a newer version use `mvn -Dpayara.version=5.192-SNAPSHOT ..`.
If the test code depends on a certain newer version the root module version usually should **not** be changed.
Ask how to handle the case.

**Compilation**
While the root module adds the arquillian setup to the classpath this is only for `test` scope.
To successfully compile code in the `src/main/java` and `src/test/java` folders the corresponding archives need to be added to the module's dependencies.

**Running Tests**
Tests depend on profile and version picked up from maven.
This means running tests is straight forward using maven (`mvn integration-test`) requires additional setup when done from the IDE. 
Note that any test must have a `@Deployment` since otherwise the tests aren't deployed to the server and don't run.

**Profiles and Properties**
* `payara-server-remote`: Connects to a running Payara Server
  * `skipConfig`: Whether to skip applying server config during pre-integration-test phase. Defaults to false.
  * `skipServerStart`: Whether to skip starting the server before applying config during pre-integration-test phase. Defaults to true.
  * `skipServerRestart`: Whether to skip restarting the server after applying config during pre-integration-test phase. Defaults to false.
  * `skipTestConfigCleanup`: Whether to skip cleaning up any config applied to the server for a test after executing it. Defaults to false.
* `payara-server-managed`: Resolves Payara Server via Maven, unpacks it, and controls its start/stop for each test.
    * `skipConfig`: Whether to skip applying server config during pre-integration-test phase. Defaults to false.
    * `skipServerStart`: Whether to skip starting the server before applying config during pre-integration-test phase. Defaults to false.
    * `skipServerRestart`: Whether to skip restarting the server after applying config during pre-integration-test phase. Defaults to false.
    * `skipTestConfigCleanup`: Whether to skip cleaning up any config applied to the server for a test after executing it. Defaults to false.
    * `skipZombieKill`: Whether to skip attempting to kill a running `payara-server-managed` domain before attempting test configuration and execution. Defaults to the value of `skipServerRestart` (false).
* `payara-micro-managed`: Resolves Payara Micro via Maven, and controls its start/stop for each test.
* `web`: Resolve the `fish.payara.distributions:payara-web` instead of the default Platform distribution

**Debugging**
Running tests from the IDE requires to setup the required maven properties for the profile to use and maybe the payara-version.
Using `-Dmaven.surefire.debug` to connect the IDE to tests started from command line works but will **not** pause at a breakpoint since the VM connect to this way is not the VM running the test code. Tests run in the VM of the started Payara server.
Instead build, deploy and start a Payara server in debug from your IDE (or connect to it). 
Then run the tests with `mvn integration-test -Ppayara-server-remote`. As tests get deployed to the started server and run in the server's VM this now pauses at the set breakpoints.
Note that debugging with Payara micro does not work this way.

The control properties detailed above (e.g. `skipConfig`) can be used to help prevent reapplying config and server restarts on debug runs. 

**Injection**
While conceptually valid using `@Inject` in test classes this currently does not work for HK2 instances as `@Inject` will be served by CDI (weld).
Instead use `Globals.getDefaultHabitat()` to get the `ServiceLocator`.

**Test Errors**
As tests run as an application in the server any error occurring when running the actual test method(s) are logged in the `server.log`, not in the surefire report.
