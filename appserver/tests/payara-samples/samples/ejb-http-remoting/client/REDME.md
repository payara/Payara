# Running Tests (CI Way)

* start Payara locally
* enable `ejb-invoker` app using `set-ejb-invoker-configuration --enabled=true` asadmin command
* run `mvn verify -Ppayara-server-remote`

# Running Tests (Debug)

* start Payara locally
* enable `ejb-invoker` app using `set-ejb-invoker-configuration --enabled=true` asadmin command
* build `server-app` module (sibling to this module) and deploy it manually named `server-app`
* run the tests in IDE (run as JUnit test)