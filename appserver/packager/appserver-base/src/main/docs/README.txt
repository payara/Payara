Thank you for downloading Payara Server 5.193!

Here are a few short steps to get you started...


0. Prerequisite
===============

Payara Server currently supports the following Java Virtual Machines:

* Oracle JDK8 (u231+), Oracle JDK 11 (11.0.5+)
* Azul Zulu JDK8 (u232+), Azul Zulu JDK 11 (11.0.5u10+)
* OpenJDK JDK8 (u232+), OpenJDK 11 (11.0.5+)

For IBM J9 support, please download Payara Blue from http://www.payara.fish/downloads

1. Installing Payara Server
===========================

Installing Payara Server is just a matter of unzipping the Payara Server archive in the desired directory. Since you are reading this, you have probably already unzipped Payara Server. If not, just type the following command in the directory where you want Payara Server to be installed: jar xvf payara-5.193.zip

The default domain called 'domain1' is already installed and preconfigured.


2. Starting Payara Server
=========================

The 'asadmin' command-line utility is used to control and manage Payara Server (start, stop, configure, deploy applications, etc.).

To start Payara Server, just go in the directory where Payara Server is located and type:
        On Unix: payara5/bin/asadmin start-domain
        On Windows: payara5\bin\asadmin start-domain

After a few seconds, Payara Server will be up and ready to accept requests. The default 'domain1' domain is configured to listen on port 8080. In your browser, go to http://localhost:8080 to see the default landing page.

To manage Payara Server, just go to web administration console: http://localhost:4848


3. Stopping Payara Server
=========================

To stop Payara Server, just issue the following command:
        On Unix: payara5/bin/asadmin stop-domain
        On Windows: payara5\bin\asadmin stop-domain


4. Where to go next?
====================

Open the following in your browser: https://docs.payara.fish/. It contains useful information such as the details about the Payara Project, links to the Payara Server Documentation, etc.


Make sure to also check the Payara Server 5.193 Release Notes as they contain important information: https://docs.payara.fish/release-notes/release-notes-193.html


5. Documentation
================

Payara Server 5.193 Release Notes: https://docs.payara.fish/release-notes/release-notes-193.html

Payara Server Documentation: https://docs.payara.fish/

Payara Server GitHub Project: https://github.com/payara/Payara

Payara Community: http://www.payara.fish/community


6. Need support?
============

Migrating to Payara Server or Payara Micro? Our Migration & Project Support is offered through a flat fee per year regardless of the size of your environment and supports an unlimited number of units in development.

Need help with the Payara Platform in production? Payara Enterprise offers software, security, stability, and support directly from Engineers.

Learn more about Payara support options: https://www.payara.fish/support/


7. Follow us
============

Make sure to follow @Payara_Fish on Twitter and read The Payara Blog (http://blog.payara.fish/) to get the latest news on Payara.
