#!/bin/bash
set -x

#SKIP_TESTS="-Dmaven.test.skip=true"

if [ -e trinidad ] ; then
    svn up trinidad
else
    svn co https://svn.apache.org/repos/asf/myfaces/trinidad/trunk trinidad
    cd trinidad
    patch -p0 < ../trinidad.patch
fi

cd trinidad
cd trinidad-build
mvn $SKIP_TESTS install
cd -
mvn $SKIP_TESTS install
#mvn $SKIP_TESTS -f trinidad-api/pom.xml install
#mvn $SKIP_TESTS -f trinidad-impl/pom.xml install
