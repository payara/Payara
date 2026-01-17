# Postbootcommand File Tests

Tests postbootcommand file, restarts Payara for each test.

Run testpostbootcommand.py.

## Setup
Set PAYARA_HOME to the tested Payara. For example:

    PAYARA_HOME=/path-to-my-payara-src/appserver/distributions/payara/target/stage/payara7 ./testpostbootcommand.py

Set MAVEN_HOME if case you don't have maven (mvn) on your PATH:

    MAVEN_HOME=/usr/share/maven ./testpostbootcommand.py

