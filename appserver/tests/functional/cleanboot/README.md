# Cleanboot File Tests

Test a clean boot with Payara Platform.

Creates an instance, starts it, checks the logs for any trace above INFO, stops the instance(s), deletes the instance(s)

Creates a deployment group, create instances for that group, starts the group, checks the logs for any trce above INFO, stops the instances, delete the instances, delete the group

Logs above INFO are collected in files, to be assessed manually (some warnings are known issues and can be ignored)


## Setup

Set PAYARA_HOME to the tested Payara. For example:

    export PAYARA_HOME=/path-to-my-payara-src/appserver/distributions/payara/target/stage/payara7

Install the Pytest plugin:

    pip install pytest-playwright

Install the required browsers:

    playwright install

Note: you may also need to run the command `playwright install-deps`

## run the tests:

    pytest testcleanbootcommand.py

use `--headed` to run with headless=False

use `-s` to see the standard output of the tests

to debug the test, set the environment variable PWDEBUG=1