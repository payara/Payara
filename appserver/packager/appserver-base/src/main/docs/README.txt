Thank you for downloading GlassFish Server Open Source Edition 4.1!

Here are a few short steps to get you started...


0. Prerequisite
===============

GlassFish 4.1 requires Oracle JDK 7 Update 65+ or Oracle JDK 8 Update 5+. 
Check http://www.oracle.com/technetwork/java/javase/downloads/index.html to download the JDK.


1. Installing GlassFish
=======================

Installing GlassFish is just a matter of unzipping the GlassFish archive in the desired directory. Since you are reading this, you have probably already unzipped GlassFish. If not, just type the following command in the directory where you want GlassFish to be installed : jar xvf glassfish-4.1.zip


The default domain called 'domain1' is installed and preconfigured.


2. Starting GlassFish
=====================

The 'asadmin' command-line utility is used to control and manage GlassFish (start, stop, configure, deploy applications, etc).

To start GlassFish, just go in the directory where GlassFish is located and type:
        On Unix: glassfish4/glassfish/bin asadmin start-domain
        On Windows: glassfish4\glassfish\bin asadmin start-domain

After a few seconds, GlassFish will be up and ready to accept requests. The default 'domain1' domain is configured to listen on port 8080. In your browser, go to http://localhost:8080 to see the default landing page.

To manage GlassFish, just go to web administration console: http://localhost:4848


3. Stopping GlassFish 
=====================

To stop GlassFish, just issue the following command :
        On Unix: glassfish4/glassfish/bin asadmin stop-domain
        On Windows: glassfish4\glassfish\bin asadmin stop-domain


4. Where to go next?
====================

Open the following local file in your browser: glassfish4/glassfish/docs/quickstart.html. It contains useful information such as the details about the pre-configured 'domain1', links to the GlassFish Documentation, etc.


Make sure to also check the GlassFish 4.1 Release Notes as they contains important information : https://glassfish.java.net/docs/4.1/release-notes.pdf

If you are new to Java EE, the Java EE Tutorial (see below) is a good way to learn more. The examples are tailored to run with GlassFish and this will help you get oriented.




5. Documentation 
================

GlassFish 4.1 Releases Notes : https://glassfish.java.net/docs/4.1/release-notes.pdf


GlassFish Documentation : http://glassfish.org/documentation.html

Java EE 7 Tutorial : http://docs.oracle.com/javaee/7/tutorial/doc/home.htm


GlassFish Forum : https://www.java.net/forums/glassfish/glassfish


6. Follow us
============

Make sure to follow @GlassFish and @Java_EE on Twitter and read The Aquarium Blog (https://blogs.oracle.com/TheAquarium) to get the latest news on GlassFish and Java EE.

