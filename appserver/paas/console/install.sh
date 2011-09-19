#!/bin/bash

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
$SERVER_DIR/bin/asadmin undeploy admin-console
$SERVER_DIR/bin/asadmin stop-domain 

echo Clearing the OSGi cache
rm -rf $SERVER_DIR/glassfish/domains/domain1/osgi-cache
rm -rf $SERVER_DIR/glassfish/domains/domain1/generated
echo Removing any existing demo plugins from $MODULE_DIR
rm $MODULE_DIR/plugin*.jar 2>/dev/null

echo Building....
mvn $CLEAN install

if [ "$?" -ne 0 ] ; then
    echo "**** Error: build failed"
    exit -1
fi

echo Installing modules to $MODULE_DIR
cp plugin-system/target/plugin-system-*.jar $MODULE_DIR
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
$SERVER_DIR/bin/asadmin deploy --force=true webapp/target/admin-console/

echo Done.
