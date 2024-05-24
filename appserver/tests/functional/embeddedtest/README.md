## Goal

Test for Payara Embedded. 

It compiles the test with the required jar, starts payara embedded, deploy clusterjsp.war and checks for its successful deployment.

## Usage

### To run the current version of Payara Embedded:

From the location ${PAYARA_HOME}/appserver/tests/functional/embeddedtest, run the command:

> mvn clean test -P{FullProfile/WebProfile} 

the test is designed to adapt to jdk 8, 11 and higher. 

### To run a specific version of Payara Embedded:

have the desired payara-embedded jar placed in ${PAYARA_HOME}/appserver/tests/functional/embeddedtest/lib

The jar should have a name following this format: payara-embedded-{web/all}-{payara-version}.jar

example: payara-embedded-all-6.2023.8.RC1.jar

From the location ${PAYARA_HOME}/appserver/tests/functional/embeddedtest, run the command:

> mvn validate -P{FullProfile/WebProfile},specificJar -Dpayara.version="6.2023.8.RC1" 

This first command installs the local jar before the compilation

> mvn clean verify -P{FullProfile/WebProfile},specificJar -Dpayara.version="6.2023.8.RC1" 

It is also possible to specify a different location for the embedded jar:

> mvn validate -P{FullProfile/WebProfile},specificJar -Dpayara.version="6.2023.8.RC1" -Djarlocation="/tmp/payara-embedded-all-6.2023.8.RC1.jar"

> mvn clean verify -P{FullProfile/WebProfile},specificJar -Dpayara.version="6.2023.8.RC1" -Djarlocation="/tmp/payara-embedded-all-6.2023.8.RC1.jar"

