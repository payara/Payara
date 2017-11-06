# Description #
----------

<!--- Brief summary description of the bug or enhancement request -->

## Expected Outcome

<!-- If reporting a bug, give a detailed summary of the expected behavior the server and/or deployed applications SHOULD exhibit after executing the steps described below. If possible quote Java EE specification's sections or link to Glassfish or Payara's official documentation as evidence. -->

<!-- If making an enhancement request, give a detailed explanation of how this new or updated feature should work -->

## Current Outcome

<!-- If reporting a bug, give a detailed summary of the actual behavior the server and/or deployed applications exhibit after executing the steps described below. Please put emphasis on any unwanted results. -->

<!-- If making an enhancement request, explain the drawbacks and disadvantages of the targeted feature (or lack of it) -->

## Steps to reproduce (Only for bug reports) 

<!-- 

Describe the test to reproduce the bug in a series of steps. Make each step simple to follow by describing configuration changes, commands to run or simple instructions; for example:

1 -**  Start the domain

    ./asadmin start-domain payaradomain

2 -**  Configure an HTTP network listener in the admin console:

[Attach screenshots of Payara's Server console, to illustrate]

3 -**  Make changes to the domain.xml configuration:

	<java-config classpath-suffix="" debug-options="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=9009" system-classpath="">
        <jvm-options>-XX:MaxPermSize=512m</jvm-options>
        <jvm-options>-server</jvm-options>
		...
	</java-config>

-->

### Samples

<!-- Include a link to a [SCCE](http://sscce.org/ "Short, Self-Contained, Correct Example") that helps reproduce the issue faster. Structuring a Maven project is strongly recommended if possible -->

## Context (Optional)

<!-- Give details on this issue has affected you, for example: What are you trying to accomplish?
Providing context helps us come up with a solution that is most useful for your scenario. -->

## Environment ##

- **Payara Version**: 4.1.1.x
- **Edition**: <!-- Full/Web/Blue/Micro -->
- **JDK Version**: <!-- 6/7/8 uXX - Oracle/IBM/OpenJDK -->
- **Operating System**: <!-- Windows / Linux / Mac -->
- **Database**: <!-- Oracle/MySQL/MariaDB/PostGres/DB2/SQL Server -->

