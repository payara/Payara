Thank you for downloading Payara Blue 4.1.2.172!

Here are a few short steps to get you started...


0. Prerequisite
===============

Payara Blue 4.1.2.172 requires IBM JDK Version 7, SR9 FP40 or higher, IBM JDK Version 7 Release 1, SR3 FP40 or higher or IBM JDK Version 8 SR3 or higher. 
Check http://www.ibm.com/developerworks/java/jdk/ to download the JDK.


1. Installing Payara Blue
=========================

Installing Payara Blue is just a matter of unzipping the Payara Blue archive in the desired directory. Since you are reading this, you have probably already unzipped Payara Blue. If not, just type the following command in the directory where you want Payara Blue to be installed: jar xvf payara-4.1.2.172.zip


The default domain called 'domain1' is preinstalled and preconfigured.


2. Starting Payara Blue
=======================

The 'asadmin' command-line utility is used to control and manage Payara Blue (start, stop, configure, deploy applications, etc.).

To start Payara Blue, just go in the directory where Payara Blue is located and type:
        On Unix: payara41/bin/asadmin start-domain
        On Windows: payara41\bin\asadmin start-domain

After a few seconds, Payara Blue will be up and ready to accept requests. The default 'domain1' domain is configured to listen on port 8080. In your browser, go to http://localhost:8080 to see the default landing page.

To manage Payara Blue, just go to web administration console: http://localhost:4848


3. Stopping Payara Blue
=======================

To stop Payara Blue, just issue the following command:
        On Unix: payara41/bin/asadmin stop-domain
        On Windows: payara41\bin\asadmin stop-domain


4. Where to go next?
====================

Open the following in your browser: https://payara.gitbooks.io/payara-server/content/. It contains useful information such as the details about the Payara Project, links to the Payara Server Documentation, etc.


Make sure to also check the Payara Server 4.1.2.172 Release Notes as they contain important information: https://payara.gitbooks.io/payara-server/content/release-notes/release-notes-172.html


5. Documentation 
================

Payara Server 4.1.2.172 Release Notes: https://payara.gitbooks.io/payara-server/content/release-notes/release-notes-172.html

Payara Documentation: https://payara.gitbooks.io/payara-server/content/

Payara GitHub Project: https://github.com/payara/Payara

Payara Community: http://www.payara.fish/community 


6. Follow us
============

Make sure to follow @Payara_Fish on Twitter and read The Payara Blog (http://blog.payara.fish/) to get the latest news on Payara.
