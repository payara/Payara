Thank you for downloading Payara Server @@@FULL_VERSION@@@!

Here are a few short steps to get you started...


0. Prerequisite
===============

Payara Server currently supports the following Java Virtual Machines:

* Azul Zulu JDK: 11 (11.0.12+), 17 (17.34/17.0.3+)
* Oracle JDK: 11 (11.0.12+), 17 (17.0.3+)
* Amazon Corretto: 11 (11.0.12+), 17 (17.0.3+)
* Adopt Open JDK: 11 (11.0.12+), 17 (17.0.3+)
* Adopt Open JDK with Eclipse Open J9: 11 (11.0.12+), 17 (17.0.3+)
* Any other JVM based on OpenJDK 11.0.12+ or 17.0.3+

The Payara Platform runs on the x64 and arm64 variants of the above JVMs.

1. Installing Payara Server
===========================

Installing Payara Server is just a matter of unzipping the Payara Server archive in the desired directory. Since you are reading this, you have probably already unzipped Payara Server. If not, just type the following command in the directory where you want Payara Server to be installed: jar xvf payara-@@@FULL_VERSION@@@.zip

The default domain called 'domain1' is already installed and preconfigured.


2. Starting Payara Server
=========================

The 'asadmin' command-line utility is used to control and manage Payara Server (start, stop, configure, deploy applications, etc.).

To start Payara Server, just go in the directory where Payara Server is located and type:
        On Unix: payara7/bin/asadmin start-domain
        On Windows: payara7\bin\asadmin start-domain

After a few seconds, Payara Server will be up and ready to accept requests. The default 'domain1' domain is configured to listen on port 8080. In your browser, go to http://localhost:8080 to see the default landing page.

To manage Payara Server, just go to web administration console: http://localhost:4848


3. Stopping Payara Server
=========================

To stop Payara Server, just issue the following command:
        On Unix: payara7/bin/asadmin stop-domain
        On Windows: payara7\bin\asadmin stop-domain


4. Where to go next?
====================

Open the following in your browser: https://docs.payara.fish/. It contains useful information such as the details about the Payara Project, links to the Payara Server Documentation, etc.


Make sure to also check the Payara Server @@@FULL_VERSION@@@ Release Notes as they contain important information: https://docs.payara.fish/docs/@@@FULL_VERSION@@@/release-notes/release-notes-@@@MINOR_VERSION@@@-@@@UPDATE_VERSION@@@.html


5. Documentation
================

Payara Server @@@FULL_VERSION@@@ Release Notes: https://docs.payara.fish/docs/@@@FULL_VERSION@@@/release-notes/release-notes-@@@MINOR_VERSION@@@-@@@UPDATE_VERSION@@@.html

Payara Server Documentation: https://docs.payara.fish/

Payara Server GitHub Project: https://github.com/payara/Payara

Payara Community: http://www.payara.fish/community


6. Are You Using Payara Server for Mission Critical Production Environments?
============
Payara Server Enterprise is optimised for mission critical production systems in any environment: on premise, in the cloud, or hybrid. Payara Server is aggressively compatible with the ecosystem components you're already using, provides broad integration with cloud vendors, and support for Docker and Kubernetes. Development
in collaboration with an industry-leading DevOps team and the global Payara community ensures Payara Server Enterprise is the best option for production Jakarta EE applications today and for the future.

Payara Server Enterprise subscriptions include:

* Choice of support: Migration & Project Support, 24x7, or 10x5 plans
* Monthly releases, bug fixes, and patches
* 10-year software lifecycle
* Access to add-on Payara Accelerator consultancy services

Learn more about Payara Server Enterprise: https://www.payara.fish/support/


7. Follow us
============

Make sure to follow @Payara_Fish on Twitter and read The Payara Tech Blog (http://blog.payara.fish/) to get the latest news on Payara.