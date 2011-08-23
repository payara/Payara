$S1AS_HOME/bin/asadmin start-domain
$S1AS_HOME/bin/asadmin create-emulator --virt-type kvm --emulator-path /usr/bin/kvm --connection-string qemu:///system kvm
$S1AS_HOME/bin/asadmin add-virtualization --type libvirt --emulator kvm
$S1AS_HOME/bin/asadmin create-server-pool --subnet 192.168.1.102/250 --portName "br0" --virtName libvirt cloud
#asadmin create-server-pool --subnet 129.158.193.100/250 --portName "br0" --virtName libvirt cloud
$S1AS_HOME/bin/asadmin create-template --files /space/bhavani/Desktop/ubuntu.img,/space/bhavani/Desktop/ubuntu.xml --indexes ServiceType=JavaEE,VirtualizationType=libvirt ubuntu
$S1AS_HOME/bin/asadmin create-template-user --userid 1000 --groupid 1000 --template ubuntu cloud
$S1AS_HOME/bin/asadmin create-machine --serverPool cloud --networkName localhost local
$S1AS_HOME/bin/asadmin create-machine-user --machine local --userId 1000 --groupId 1000 bhavani
$S1AS_HOME/bin/asadmin stop-domain
