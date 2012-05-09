asadmin create-emulator --virt-type kvm --emulator-path /usr/bin/kvm kvm
#asadmin create-emulator --virt-type qemu --emulator-path /usr/bin/qemu-system-x86_64 qemu
asadmin add-virtualization --type libvirt --emulator kvm
asadmin create-virt-group --subnet 10.132.181.0/24 --portName "cscotun0;br0" --virtName libvirt cloud
asadmin add-template --location /var/lib/libvirt/images/ubuntu.img --xml ... --virtName libvirt ubuntu

#asadmin create-machine --group cloud --mac b8:ac:6f:1f:7d:e8 --emulator xen xen
#asadmin create-machine --group cloud --mac 00:14:4f:70:88:22 cloud-3


asadmin create-machine --group cloud --mac 00:1c:c4:9f:e7:3a cloud-1
asadmin create-machine --group cloud --mac 00:14:4f:4a:ba:16 office
asadmin create-machine --group cloud --mac 00:1c:c4:9f:f2:99 cloud-2

<machines name="local">
        <network-name>localhost</network-name>
        <user name="dochez" user-id="501"></user>
      </machines>
