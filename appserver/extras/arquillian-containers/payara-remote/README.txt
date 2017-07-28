

How to use this container integration?

This implementation provides the Arquillian integration with your remote Payara 
container. You can use your normal Payara deployment to perform your test. The 
integration connects Arquillian to the DAS via the REST Interfaces to Administer 
Payara Server over http or https protocol. This gives you the flexibility to use 
any possible deployment scenarios having your servers either on many different physical 
or virtual nodes, or on the same one.

The properties you can use:

adminHttps: You can use it to specify whether the http or https protocol shall be used 
to access the DAS. The property value can be true or false. If omitted the default value 
is false, meaning the http protocol shall be used.

adminHost: Domain Admin Server (DAS) host address. If  omitted the default value is 
localhost.

adminPort: The port to be used to access Payara Admin API. If omitted the default 
value is 4848.

adminUser: The name of the admin user of your DAS. If omitted, no authentication will 
be used to access the DAS. This case you must have empty password for your admin user of 
your domain. 

adminPassword: The password of the admin user of your DAS. Canot be omitted if you 
declare the admin user. If no authentication will be used to access the DAS you must 
have empty password for your admin user of your domain. 

serverHttps: You can use it to specify whether the http or https protocol shall be used 
to access the target server for deployment, on which your test runs. The property value 
can be true or false. If omitted the default value is false, meaning the http protocol 
shall be used.

target: Payara has a notion of target, which specifies the target to which you are 
deploying. We use the "target" as property key with the same semantics as the standard 
Payara utilities do.

Valid values of the target are:
 
 -  server:  Deploys the component to the default Admin Server instance (on your DAS 
 server). This is the default value if the property is omitted.

 -  instance_name: Deploys the component to the specified stand-alone sever instance, 
 which may be on the same hosts or can be on a different one as the DAS server.

 -  cluster_name: Deploys the component to every server instance in the cluster. They 
 can be on the same or on several other hosts as the DAS server. Note: Arquillion use 
 only one instance to run the test case.
 
Note: The domain name as a target (which is valid for GlasFish) is not a reasonable 
deployment scenario in case of testing.

The HOST address and port numbers of the test server instance used by Arquillian 
(determined by the target property) been retrieved automatically from the DAS server. 
You have to make them accessible for your test environment (consider any firewall or 
proxy configuration).   

The contextroot that will be used to run the tests is also retrieved automatically 
from the DAS server. If you do not have sun-web.xml or glassfish-web.xml file in your 
web application, Payara will use the name of your deployment without the extension 
as contextroot. The same rule is applied for enterprise applications if you do not have 
application.xml file. The jar test-deployments are treated as a web application. You can 
use the above standard GlassFish xml files as normal to declare your contextroot to be 
applied. For more detail, please refer to GlassFish Server 3.1 Administration Guide. You 
should consider the above description to avoid any conflict with your already deployed 
web or enterprise applications in your administrative domain. 


libraries: A comma-separated list of library JAR files. Specify the library JAR  files 
by their relative or absolute paths. Specify relative paths relative to domain-dir/lib/applibs. 
The libraries are made available to the application in the order specified. For more detail, 
please refer to GlassFish Server 3.1 Administration Guide.

properties: Optional keyword-value pairs  that  specify  additional properties  for the 
deployment. The available properties are determined by the implementation of the  component 
that is being deployed. For more detail, please refer to GlassFish Server 3.1 Administration Guide.

