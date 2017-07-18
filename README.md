![#badassfish](https://avatars3.githubusercontent.com/u/7817189?v=3&s=100)

# Payara Server

Payara Server is a patched, enhanced, and supported application server derived from GlassFish Server Open Source Edition 4.x.

Payara Server development is coordinated by the Payara Foundation, a UK Not-for-profit organisation.

Visit [www.payara.fish](http://www.payara.fish) for full 24/7 support and lots of free resources.

Information about the Open Source project is on the [GitHub project page](https://payara.github.io/Payara/).

**Full Payara Server and Payara Micro documentation:** https://payara.gitbooks.io/payara-server/content/

Payara Blue is a drop in replacement for GlassFish Server Open Source specifically for IBM J9, with the peace of mind of quarterly releases containing enhancements, bug fixes, and patches including patches to dependent libraries, as required, including Tyrus, Eclipse Link, Jersey, and others. Our vision is to optimise Payara Blue and make it the best server for production Java applications with responsive 24/7 dedicated incident and software support from the best middleware engineers in the industry.

We have added full JCache support, enhanced JBatch functionality, replaced Shoal with Hazelcast for session clustering, and are driving development of Payara Micro. A completely new way of running WAR applications on top of an embedded GlassFish core, Payara Micro is ideally suited to cloud and microservice architectures with elastic clustering and no installation. Payara Micro can run WAR applications simply using:

```Shell
java -jar payara-micro.jar --deploy test.war
```

In summary Payara Server provides:

* Fully Supported Server derived from GlassFish 4.x Open Source Edition
* Drop in Replacement for GlassFish Open Source Edition
* Production Enhancements including JCache, JBatch, and Hazelcast session clustering.
* Payara Micro for cloud and microservice deployments
* Always Open Source dual licensed CDDL/GPL
* Supported by dedicated engineers 24/7
* Incident and Software Support
* Optimised for production and operations
* Quarterly Patch Releases / Bug Fixes

## Supported JDKs and OSs

Payara Server currently supports the following JDKs:

* Oracle JDK 7/8
* Azul Zulu 7/8
* OpenJDK 7/8
* IBM J9 7/8

Payara Server currently supports the following OSs

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

Payara Server is currently tested on the following platforms:

|                       |Oracle JDK     |Azul Zulu      |OpenJDK        |IBM J9 |
|---                    |---            |---            |---            |---    |
|Windows 7              |✓              |✓              |               |       |
|Windows 8.1            |               |✓              |               |       |
|Windows 10             |✓              |✓              |               |       |
|Windows Server 2008    |               |✓              |               |       |
|Windows Server 2012    |✓              |✓              |               |       |
|Windows Server 2016    |✓              |✓              |               |       |
|Ubuntu 14.04, 16.04    |✓              |✓              |✓              |       |
|Debian 7               |✓              |               |               |       |
|CentOS 6               |✓              |               |               |       |
|RHEL                   |               |               |               |       |
|OpenSUSE 42.2          |✓              |               |               |       |
|SUSE 11.4, 12.2        |✓              |               |               |       |
|AIX 7.3 TL3            |               |               |               |✓      |
|MacOS(OSX) 10.10.5     |✓              |               |               |       |

GlassFish is a trademark of Oracle Corporation.
Payara is a trademark of Payara Foundation.
