## Goal

Test for Payara Embedded. 

It compiles the test with the required jar, starts payara embedded, deploy clusterjsp.war and checks for its successful deployment.

## Usage

have the desired payara-embedded jar placed in ${PAYARA_HOME}/appserver/tests/functional/embeddedtest/lib

The jar should have a name following this format: payara-embedded-{web/all}-{payara-version}.jar

example: payara-embedded-all-6.2023.8.RC1.jar

From the location ${PAYARA_HOME}/appserver/tests/functional/embeddedtest, run the command:

> mvn clean -P {FullProfile/WebProfile} -Dpayara.version="6.2023.8.RC1" compile test 

the test is designed to adapt to jdk 8, 11, or 17. 

If needed, the version of jdk used to compile and run can be modified with the properties maven.compiler.source, maven.compiler.target