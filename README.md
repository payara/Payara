![alt tag](https://avatars3.githubusercontent.com/u/7817189?v=3&s=100)

# Payara 5
This is an experimental branch for including the changes and upcoming modules due to debut with GlassFish 5. The main Payara project continues to exist on the _master_ branch.

## Included upcoming JSR modules

- JSF 2.3 (JSR 372) - Mojarra 2.3
- MVC 1.0 (JSR 371) - Ozark
- CDI 2.0 (JSR 365) - Weld 3
- Java EE Security (JSR 375) - Soteria

For specific versions of the implementations included, look into [appserver/pom.xml](appserver/pom.xml).

# Payara Server

Payara Server is a patched, enhanced and supported application server derived from GlassFish Server Open Source Edition 4.x.

Visit [www.payara.fish](http://www.payara.fish) for full 24/7 support and lots of free resources.

Information about the opensource project is on the [GitHub project page](https://payara.github.io/Payara/).

**Full Payara Server and Payara Micro documentation:** https://payara.gitbooks.io/payara-server/content/ 

Payara Server is a drop in replacement for GlassFish Server Open Source with the peace of mind of quarterly releases containing enhancements, bug fixes and patches including patches to dependent libraries, as required, including Tyrus, Eclipse Link, Jersey and others. Our vision is to optimise Payara Server and make it the best server for production Java applications with responsive 24/7 dedicated incident and software support from the best middleware engineers in the industry.

We have added full JCache support, enhanced JBatch functionality, replaced Shoal with Hazelcast for session clustering and are driving development of Payara Micro. A completely new way of running war applications on top of an embedded GlassFish core. Payara Micro is ideally suited to cloud and microservice architectures with elastic clustering and no installation. Payara Micro can run war applications simply using

```Shell
java -jar payara-micro.jar --deploy test.war
```

In summary Payara Server provides;

* Fully Supported Server derived from GlassFish 4.x Open Source Edition
* Drop in Replacement for GlassFish Open Source Edition
* Production Enhancements including JCache, JBatch and Hazelcast session clustering.
* Payara Micro for cloud and microservice deployments
* Always Open Source dual licensed CDDL/GPL
* Supported by dedicated engineers 24/7
* Incident and Software Support
* Optimised for production and operations
* Quarterly Patch Releases / Bug Fixes

We take GlassFish, support it, add fixes, add enhancements and we release it as open source Payara Server.


GlassFish is a trademark of Oracle Corporation.
Payara is a trademark of Zenthis Limited.

