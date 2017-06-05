![alt tag](https://avatars3.githubusercontent.com/u/7817189?v=3&s=100)

# Payara Blue

Payara Blue is a patched, enhanced and supported application server derived from GlassFish Server Open Source Edition 4.x, specifically for IBM J9.

Payara Blue development is coordinated by the Payara Foundation, a UK not-for-profit organisation.

Visit [www.payara.fish](http://www.payara.fish) for full 24/7 support and lots of free resources.

Information about the opensource project is on the [GitHub project page](https://payara.github.io/Payara/).

**Full Payara Blue and Payara Blue Micro documentation:** https://payara.gitbooks.io/payara-server/content/ 

Payara Blue is a drop in replacement for GlassFish Server Open Source specifically for IBM J9, with the peace of mind of quarterly releases containing enhancements, bug fixes, and patches including patches to dependent libraries, as required, including Tyrus, Eclipse Link, Jersey, and others. Our vision is to optimise Payara Blue and make it the best server for production Java applications with responsive 24/7 dedicated incident and software support from the best middleware engineers in the industry.

We have added full JCache support, enhanced JBatch functionality, replaced Shoal with Hazelcast for session clustering and are driving development of Payara Blue Micro. A completely new way of running WAR applications on top of an embedded GlassFish core, Payara Blue Micro is ideally suited to cloud and microservice architectures with elastic clustering and no installation. Payara Blue Micro can run WAR applications simply using

```Shell
java -jar payara-micro.jar --deploy test.war
```

In summary Payara Blue provides;

* Fully Supported Server derived from GlassFish 4.x Open Source Edition
* Drop in Replacement for GlassFish Open Source Edition
* Production Enhancements including JCache, JBatch and Hazelcast session clustering.
* Payara Blue Micro for cloud and microservice deployments
* Always Open Source dual licensed CDDL/GPL
* Supported by dedicated engineers 24/7
* Incident and Software Support
* Optimised for production and operations
* Quarterly Patch Releases / Bug Fixes

## Supported JDKs and OSs

Payara Blue currently supports the following JDKs:

* IBM J9 7/8

Payara Blue currently supports the following OSs

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

Payara Blue is currently tested on the following platforms:

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

