GF_HOME=${GF_HOME:-$S1AS_HOME}
export PATH=$GF_HOME/bin:$PATH

asadmin start-domain domain1

asadmin create-ims-config-ovm --connectionstring "http://admin:abc123@sf-x2200-7.india.sun.com:8888;root:abc123"  ovm
asadmin create-server-pool --subnet 10.12.152.25/50 --portname "foobar" --virtualization ovm pool2

asadmin create-template --files $SCRIPTS_DIR/OVM_EL5U6_X86_PVM_GLASSFISH_TINY.tgz --indexes ServiceType=JavaEE,VirtualizationType=OVM GLASSFISH_TINY
asadmin create-template-user --virtualization ovm --userid glassfish --groupid glassfish --template GLASSFISH_TINY glassfish

asadmin create-template --files $SCRIPTS_DIR/OVM_EL5U6_X86_PVM_ORACLEDB.tgz --indexes ServiceType=Database,VirtualizationType=OVM ORACLE_DATABASE
asadmin create-template-user --virtualization ovm --userid oracle --groupid oinstall --template ORACLE_DATABASE oracle

#asadmin create-template --files /space/kshitiz/glassfish/trunk/templates/OVM_JEOS_64_PVM_OTD_LARGE.tgz --indexes ServiceType=LB,VirtualizationType=OVM otd
#asadmin create-template-user --virtualization ovm --userid 1000 --groupid 1000 --template otd cloud
#asadmin create-virtual-cluster --groupnames pool2 --template GLASSFISH_TINY --min 1 demovc

