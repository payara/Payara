$S1AS_HOME/bin/asadmin start-domain --debug
$S1AS_HOME/bin/asadmin add-virtualization --type Native
$S1AS_HOME/bin/asadmin create-server-pool --virtualization Native --subnet 192.168.1.102/250 --portName "br0" Native
#$S1AS_HOME/bin/asadmin create-template --indexes ServiceType=JavaEE,VirtualizationType=Native Native
#$S1AS_HOME/bin/asadmin create-template --indexes ServiceType=Database,VirtualizationType=Native DBNative
