#!/bin/sh
#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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
export ARTIFACT=paas-console

if [ "$SERVER_DIR" == "" ] ; then
    echo "You must define SERVER_DIR (e.g., export SERVER_DIR=~/src/servers/glassfish3)"
    exit -1
fi

if [ "$MODULE_DIR" == "" ] ; then
    export MODULE_DIR=$SERVER_DIR/glassfish/modules
fi

function usage() {
    echo "install.sh usage:"
    echo "   -c: Do a clean build (defaults to false)"
    echo "   -d: Pause before installing the application to allow for attaching a debugger (defaults to false)"
    exit 0
}

while getopts cd opt
do
    case "$opt" in
        c) CLEAN=clean ;;
        d) DEBUG=true ;;
        ?) usage ;;
    esac
done;

echo Stopping the server
$SERVER_DIR/bin/asadmin undeploy $ARTIFACT
$SERVER_DIR/bin/asadmin stop-domain 

echo Clearing the OSGi cache
rm -rf $SERVER_DIR/glassfish/domains/domain1/osgi-cache
rm -rf $SERVER_DIR/glassfish/domains/domain1/generated
echo Removing any existing demo plugins from $MODULE_DIR
rm $MODULE_DIR/plugin*.jar 2>/dev/null

echo Building....
mvn $CLEAN -o install

if [ "$?" -ne 0 ] ; then
    echo "**** Error: build failed"
    exit -1
fi

echo Installing modules to $MODULE_DIR
cp plugin-system/target/paas.console.plugin-system.jar $MODULE_DIR
for PLUGIN in plugins/* ; do
    PLUGIN=`basename $PLUGIN`
    if [ -d plugins/$PLUGIN ] ; then
        echo "     $PLUGIN..."
        cp plugins/$PLUGIN/target/*.jar $MODULE_DIR 2>/dev/null
    fi
done

echo Starting the server
$SERVER_DIR/bin/asadmin start-domain --debug=true

echo Deploying the application
if [ "$DEBUG" == "true" ] ; then
    read -p "Attach debugger if desired, then press Enter to deploy the web app"
fi
$SERVER_DIR/bin/asadmin deploy --force=true webapp/target/$ARTIFACT/

echo Done.
