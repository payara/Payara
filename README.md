![#badassfish](payara-logo-blue.png)

# Payara Server 5

Payara Server 5 is a patched, enhanced, and supported application server derived from GlassFish Server Open Source Edition 5.x.

Payara Server 5 development is coordinated by the Payara Foundation, a UK not-for-profit organisation.

Visit [www.payara.fish](http://www.payara.fish) for full 24/7 support and lots of free resources.

Information about the open source project is on the [GitHub project page](https://payara.github.io/Payara/).

**Full Payara Server 5 and Payara Micro documentation:** https://payara.gitbooks.io/payara-server/content/ 

Payara Server 5 is an open source, cloud-native middleware platform (drop in replacement for GlassFish Server Open Source) supporting reliable and secure deployments of Java EE (Jakarta EE) applications on premise, in the cloud, or hybrid environments. Monthly releases, bug fixes and a 10-year support lifecycle optimizes Payara Server for production deployments. Payara Server is aggressively compatible with common ecosystem components and ensures future compliance with Jakarta EE. Patches include patches to dependent libraries, as required, including Tyrus, Eclipse Link, Jersey and others. 

Payara Server is built and supported by a team of DevOps engineers dedicated to continued development and maintenance of the open source software, and committed to collaboration with the community to ensure Payara Server is the best option for production Java EE applications. Payara Support is available with responsive 24/7 dedicated incident and software support from the best middleware engineers in the industry.

We have added full JCache support, enhanced JBatch functionality, replaced Shoal with Hazelcast for session clustering and are driving development of Payara Micro. A completely new way of running WAR applications on top of an embedded GlassFish core, Payara Micro is ideally suited to cloud and microservice architectures with elastic clustering and no installation. Payara Micro can run WAR applications simply, using:

```Shell
java -jar payara-micro.jar --deploy test.war
```

In summary Payara Server provides:

* Fully Supported Server derived from GlassFish 5.x Open Source Edition
* Drop in Replacement for GlassFish Open Source Edition
* Microprofile 2.0/1.4 api support
* Supports deployment of JavaEE 8 applications
* Production Enhancements including JCache, JBatch, and Hazelcast session clustering
* Payara Micro for cloud and microservice deployments
* Always Open Source dual licensed CDDL/GPL
* Supported by dedicated engineers 24/7
* Incident and Software Support
* Optimised for production and operations
* Quarterly Patch Releases / Bug Fixes

We take GlassFish, support it, add fixes, add customer-requested enhancements and we release it as open source Payara Server.

## Supported JDKs and OSs

Payara Server 5 currently supports the following JDKs:

* Azul Zulu 8
* IBM J9 8 [Payara Blue Only]
* OpenJDK 8
* Oracle JDK 8

Payara Server 5 currently supports the following OSs:

* Windows 7+
* Windows Server 2008+
* Ubuntu 14.04 (Trusty Tahr)+
* Debian 7 (Wheezy)+
* CentOS 6+
* RHEL 5+
* OpenSUSE 42.2+
* SUSE 11.4 & 12.2+
* AIX 7.3 TL3+
* MacOS(OSX) 10.10.5 (Yosemite)+

## Tested Platforms

While not all environments are tested, if it is on a supported JVM we will provide support for customers and address issues reported by the community. Other platforms can always be supported in the future.

Payara Server 5 is currently tested on the following platforms:

|                       |Oracle JDK     |Azul Zulu      |OpenJDK        |IBM J9 |
|---                    |---            |---            |---            |---    |
|Windows 7              |✓              |               |               |       |
|Windows 10             |✓              |               |               |       |
|Windows Server 2016    |✓              |               |               |       |
|Ubuntu 16.04, 17.04    |✓              |✓              |✓              |✓      |
|Debian 8               |✓              |               |               |       |
|CentOS 7               |✓              |               |               |       |
|OpenSUSE 42.2          |✓              |               |               |       |
|AIX 7.3 TL3            |               |               |               |✓      |
|macOS      10.12.5     |✓              |               |               |       |


GlassFish is a trademark of the Eclipse Foundation.
Payara is a trademark of the Payara Foundation.

