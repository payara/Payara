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

**Profiles**
* `payara-server-remote`: 
  connects to a running server (cannot be micro)

* `payara5`: 
  downloads, installs and runs tests on a Payara 5 with `{$payara.version}` (use with `-Dpayara-version=???`)

* `payara4`: 
  downloads, installs and runs tests on a Payara 4 with `{$payara.version}` (use with `-Dpayara-version=???`)

**Debugging**
Running tests from the IDE requires to setup the required maven properties for the profile to use and maybe the payara-version.
Using `-Dmaven.surefire.debug` to connect the IDE to tests started from command line works but will **not** pause at a breakpoint since the VM connect to this way is not the VM running the test code. Tests run in the VM of the started Payara server.
Instead build, deploy and start a Payara server in debug from your IDE (or connect to it). 
Then run the tests with `mvn integration-test -Ppayara-server-remote`. As tests get deployed to the started server and run in the server's VM this now pauses at the set breakpoints.
Note that debugging with Payara micro does not work this way.

**Injection**
While conceptually valid using `@Inject` in test classes this currently does not work for HK2 instances as `@Inject` will be served by CDI (weld).
Instead use `Globals.getDefaultHabitat()` to get the `ServiceLocator`.

**Test Errors**
As tests run as an application in the server any error occurring when running the actual test method(s) are logged in the `server.log`, not in the surefire report.
