#!/bin/sh
#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
#
# The contents of this file are subject to the terms of either the GNU
# General Public License Version 2 only ("GPL") or the Common Development
# and Distribution License("CDDL") (collectively, the "License").  You
# may not use this file except in compliance with the License.  You can
# obtain a copy of the License at
# https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
# or packager/legal/LICENSE.txt.  See the License for the specific
# language governing permissions and limitations under the License.
#
# When distributing the software, include this License Header Notice in each
# file and include the License file at packager/legal/LICENSE.txt.
#
# GPL Classpath Exception:
# Oracle designates this particular file as subject to the "Classpath"
# exception as provided by Oracle in the GPL Version 2 section of the License
# file that accompanied this code.
#
# Modifications:
# If applicable, add the following below the License Header, with the fields
# enclosed by brackets [] replaced by your own identifying information:
# "Portions Copyright [year] [name of copyright owner]"
#
# Contributor(s):
# If you wish your version of this file to be governed by only the CDDL or
# only the GPL Version 2, indicate your decision by adding "[Contributor]
# elects to include this software in this distribution under the [CDDL or GPL
# Version 2] license."  If you don't indicate a single choice of license, a
# recipient has the option to distribute your version of this file under
# either the CDDL, the GPL Version 2 or to extend the choice of license to
# its licensees as provided above.  However, if you add GPL Version 2 code
# and therefore, elected the GPL Version 2 license, then the option applies
# only if the new code is made subject to such option by the copyright
# holder.
#

# This script helps setup services for running PaaS native, kvm or ovm tests.
# setup.sh -r -d <templates-dir> -s jee,javadb,mysql,oracle,apachemodjk,otd,lb native|kvm|ovm2|ovm3
# Examples:
#   1) Plain native setup
#      setup.sh native
#   2) Native with lb service
#      setup.sh -s lb native
#   3) KVM with jee service
#      setup.sh -t /kvm/images -s jee kvm
#   4) KVM with jee,javadb,apachemodjk services
#      setup.sh -t /kvm/images -s jee,javadb,apachemodjk kvm
#   5) OVM with jee service
#      setup.sh -t /ovm/templates -s jee -p pool1 ovm2
#   6) OVM with jee,otd services
#      setup.sh -t /ovm/templates -s jee,otd -p pool1 ovm2
#   7) Recreate domain1 before setting up
#      setup.sh -r native
# Author: Yamini K B
# Date  : 8-FEB-2012

GF_HOME=${GF_HOME:-$S1AS_HOME}
USAGE="Usage: $(basename $0) -r -d <templates-dir> -s jee,javadb,mysql,oracle,apachemodjk,otd,lb native|kvm|ovm2|ovm3"

[ -z "$GF_HOME" ] && echo "Please set GF_HOME or S1AS_HOME" && exit 1;

[ $# -lt 1 ] && echo $USAGE && exit 1;

log ()
{
    echo "\033[33;33m$1\033[0m"
}

err ()
{
    echo "\033[33;31m$1\033[0m"
}

setup_native ()
{
    log "Configuring NATIVE mode...."
    $A start-domain --debug
    $A create-ims-config-native

    IFS_TMP=$IFS
    IFS=","
    for s in $SERVICES
    do
        case $s in
          "lb") log "Creating template for lb service..."
                $A create-template --indexes ServiceType=LB,VirtualizationType=Native LBNative
                ;;
             *) err "Ignoring unknown service $s"
                ;;
        esac
    done
    IFS=$IFS_TMP

    $A stop-domain
    log "Successfully configured NATIVE mode...."
}

setup_kvm ()
{
    log "Configuring KVM...."
    log "Removing stale files..."
    rm -f /tmp/helloworld?.xml
    #rm -rf ~/virt/*
    rm -rf ~/virt/disks/*

    $A start-domain --debug
    $A create-ims-config-libvirt kvm
    $A set virtualizations.libvirt-virtualization.kvm.template-cache-size=0
    $A create-jvm-options -Dorg.glassfish.paas.orchestrator.parallel-provisioning=true
    $A restart-domain --debug
    $A create-server-pool --virtualization kvm --subnet 192.168.122.0/24 --portName "virbr0" cloud

    $A create-machine --serverPool cloud --networkName localhost local
    $A create-machine-user --serverPool cloud --machine local --userId 1000 --groupId 1000 $USER

    IFS_TMP=$IFS
    IFS=","
    for s in $SERVICES
    do
        case $s in
                 "jee") log "Creating template for jee service..."
                         $A create-template --virtualization kvm --files $templates_dir/glassfish.img,$templates_dir/glassfish.xml --indexes ServiceType=JavaEE,VirtualizationType=libvirt glassfish
                         $A create-template-user --virtualization kvm --template glassfish cloud
                         ;;
               "javadb") log "Creating template for javadb service..."
                         $A create-template --virtualization kvm --files $templates_dir/glassfish.img,$templates_dir/glassfish.xml --indexes ServiceType=Database,VirtualizationType=libvirt javadb
                         $A create-template-user --virtualization kvm --template javadb cloud
                         ;;
                "mysql") log "Creating template for mysql service..."
                         $A create-template --virtualization kvm --files $templates_dir/MySQL.img,$templates_dir/MySQL.xml --indexes ServiceType=Database,VirtualizationType=libvirt MySQL
                         $A create-template-user --virtualization kvm --template MySQL mysqluser
                         ;;
               "oracle") log "Creating template for oracle service..."
                         $A create-template --virtualization kvm --files $templates_dir/oracledb.img,$templates_dir/oracledb.xml --indexes ServiceType=Database,VirtualizationType=libvirt oracledb
                         $A create-template-user --virtualization kvm --template oracledb shalinikvm
                         ;;
          "apachemodjk") log "Creating template for apachemodjk service..."
                         $A create-template --virtualization kvm --files $templates_dir/apache.img,$templates_dir/apache.xml --indexes ServiceType=LB,VirtualizationType=libvirt apachemodjk
                         $A create-template-user --virtualization kvm --template apachemodjk cloud
                         ;;
                     *) err "Ignoring unknown service $s"
                         ;;
        esac 
    done
    IFS=$IFS_TMP

    #copying of the template into ~/virt/templates takes about 2 min.
    log "Copying of template(s) into ~/virt/templates in progress..."
    sleep 100
    ls -l ~/virt/templates/glassfish

    $A stop-domain
    log "Successfully configured KVM...."
}

setup_init ()
{
    log "OVM init ...."
    [ -z "$POOL" ] && err "Please specify pool name using -p <pool> option." && exit 3;
    [ -z "$CONNECTION_STRING" ] && echo "Please specify connection string using -c <string> option." && exit 1;
    [ -z "$SUBNET" ] && echo "Please specify subnet using -n <subnet> option." && exit 1;

    # Parse the old style connection string into variables we can use with the
    # new commands. At some point we should probably allow these to be 
    # specified via CLI options, but for now this keeps us compatible with
    # current uses of this script.

    # Connection string looks like:
    # http://adminUser:adminPassword@hostName:port/foo/bar;rootUser:rootPassword
    # Convert most delimeters into colons so it looks like
    # http://adminUser:adminPassword:hostName:port/foo/bar:rootUser:rootPassword
    # Then pick out the fields
    _S=`echo "$CONNECTION_STRING" | tr -s "@;" ":"`
    # Parse out fields
    PROTOCOL=`echo $_S | cut -f 1 -d :`
    OVMUSER=`echo $_S | cut -f 2 -d : | sed -e "s|//||"`
    OVMPASSWORD=`echo $_S | cut -f 3 -d :`
    OVMHOST=`echo $_S | cut -f 4 -d :`
    OVMPORT=`echo $_S | cut -f 5 -d :`
    POOLUSER=`echo $_S | cut -f 6 -d :`
    POOLPASSWORD=`echo $_S | cut -f 7 -d :`
    OVMURL="$PROTOCOL://$OVMHOST:$OVMPORT"

    log "Parsed $CONNECTION_STRING into: "
    log "OVMUSER=$OVMUSER"
    log "OVMPASSWORD=$OVMPASSWORD"
    log "OVMHOST=$OVMHOST"
    log "OVMPORT=$OVMPORT"
    log "POOLUSER=$POOLUSER"
    log "POOLPASSWORD=$POOLPASSWORD"
    log "OVMURL=$OVMURL"

    $A start-domain domain1
    $A set configs.config.server-config.network-config.protocols.protocol.admin-listener.http.request-timeout-seconds=-1
    $A create-jvm-options -Dorg.glassfish.paas.orchestrator.parallel-provisioning=true
}

setup_ovm3 ()
{
    log "Configuring OVM 3.0 ...."
    setup_init

    OVMVERSION=3.0
    VIRTTYPE=OVM30

    setup_ovm
    log "Successfully configured OVM 3.0 ...."
}

setup_ovm2 ()
{
    log "Configuring OVM 2.2 ...."
    setup_init

    OVMVERSION=2.2
    VIRTTYPE=OVM

    setup_ovm
    log "Successfully configured OVM 2.2 ...."
}

setup_ovm ()
{
    _PFILE=/tmp/p$$.txt
    trap 'rm -f $_PFILE' 0 1 15

    # Need to pass passwords vi asadmin password file
    echo "AS_ADMIN_OVMPASSWORD=$OVMPASSWORD" > $_PFILE
    echo "AS_ADMIN_IAASPASSWORD=$POOLPASSWORD" >> $_PFILE

    set -x
    $A --passwordfile $_PFILE create-ims-config-ovm --connectionstring $OVMURL --ovmversion $OVMVERSION --ovmuser $OVMUSER ovm
    $A create-server-pool --subnet $SUBNET --portname $BRIDGE  --virtualization ovm $POOL
    $A --passwordfile $_PFILE create-server-pool-user --virtualization ovm --serverpool $POOL $POOLUSER
    set +x

    rm $_PFILE

    IFS_TMP=$IFS
    IFS=","
    for s in $SERVICES
    do
        case $s in
            "jee") log "Creating template for jee service..."
                    touch $templates_dir/glassfish.tgz
                    $A create-template --files $templates_dir/glassfish.tgz --indexes ServiceType=JavaEE,VirtualizationType=$VIRTTYPE glassfish
                    $A create-template-user --virtualization ovm --template glassfish glassfish
                    ;;
          "oracle") log "Creating template for oracle service..."
                    touch $templates_dir/ORACLEDB.tgz
                    $A create-template --files $templates_dir/ORACLEDB.tgz --indexes ServiceType=Database,VirtualizationType=$VIRTTYPE ORACLE_DATABASE
                    $A create-template-user --virtualization ovm --template ORACLE_DATABASE oracle
                    ;;
           "derby") log "Creating template for derby service..."
                    touch $templates_dir/DERBY_DATABASE.tgz
                    $A create-template --files $templates_dir/DERBY_DATABASE.tgz --indexes ServiceType=Database,VirtualizationType=$VIRTTYPE DERBY_DATABASE
                    $A create-template-user --virtualization ovm --template DERBY_DATABASE glassfish
                    ;;
             "otd") log "Creating template for otd service..."
                    touch $templates_dir/OTD_LARGE.tgz
                    $A create-template --files $templates_dir/OTD_LARGE.tgz --properties vendor-name=otd --indexes ServiceType=LB,VirtualizationType=$VIRTTYPE otd-new
                    $A create-template-user --virtualization ovm --template otd-new cloud
                    ;;
                *) err "Ignoring unknown service $s"
                    ;;
        esac
    done
    IFS=$IFS_TMP

    $A stop-domain
}

log "GlassFish is at $GF_HOME"

A=$GF_HOME/bin/asadmin
BRIDGE="xenbr0"

while getopts rd:s:c:n:p:b: opt
do
  case ${opt} in
    r) log "Recreating domain1..."
       $A stop-domain domain1
       $A delete-domain domain1
       $A create-domain --adminport 4848 --nopassword domain1
       ;;
    s) SERVICES=$OPTARG
       ;;
    d)
       if [ -d "$OPTARG" ]   # Check if dir exists
       then
         templates_dir=$OPTARG
       else
         err "Directory \"$OPTARG\" does not exist."
         exit 2
       fi
       ;;
    c) CONNECTION_STRING=$OPTARG
       ;;
    n) SUBNET=$OPTARG
       ;;
    p) POOL=$OPTARG
       ;;
    b) BRIDGE=$OPTARG
       ;;
    \?) echo $USAGE
        exit 2;;
  esac
done

shift $(($OPTIND - 1))

case "$1" in
  "native") setup_native
            ;;
     "kvm") setup_kvm
            ;;
     "ovm2") setup_ovm2
            ;;
     "ovm3") setup_ovm3
            ;;
         *) echo $USAGE
            exit 2;;
esac

exit 0
