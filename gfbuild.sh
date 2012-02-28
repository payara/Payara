#!/bin/sh
#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

#This shell script invokes mvn to build GFv3.
#First the script will verify that mvn is installed and is in the path.  
#It will also verify JDK 1.6 is installed to compile GFv3.
# Syntax:
# gfbuild.sh [-r <maven-local-repo>] <maven-lifecycle-phase-operand>
# If -r or -Dmaven.local.repo is not specified, then the default maven repository 
# which is user's home directory (~/.m2/repository) is used.  
# When using -r to specify the maven-local-repo, the script checks if the directory
# exists.  The maven repo directory is created if it does not exist.  
# The script saves the build output in the file called gfbuild.log.t
#
# TO DO:
# "gfbuild.sh package"  builds the bundle withIPS packages.
# "gfbuild.sh installer" builds the GFv3 installer.

#check if maven 2 is installed and in the path
which mvn
_status=$?
if [ "$_status" -ne 0 ]; then
    echo "Unable to find mvn in the path.  Please install Maven version 2.0.9 or above.  http://maven.apache.org/download.html"
    exit "$_status"
fi

#mvn is available in the path.  Now verify that it's the right version.
mvn_ver=`mvn -version 2>&1 | awk '/^Maven version:/ {print $3}'`
java_ver=`mvn -version 2>&1 | awk '/^Java version:/ {print substr($3,3,1)}'`
#verify JDK 1.6
if [ "$java_ver" -lt 6 ]; then
    echo "Please use JDK 1.6"
    exit 1
fi

# mvn -version returns "Maven version:..." in 2.0.9 
# and in 2.2.0 and up returns "Apache Maven..."  so need to 
# apply awk with different string values.
mvn_ver=`mvn -version 2>&1 | awk '/^Maven version:/ {print $3}'`
if [ "$mvn_ver" = "" ]; then
    mvn_ver=`mvn -vesrion 2>&1 | awk '/^Apache Maven / {print $3}'`
fi

# convert to number since shell is unable to compare float"
# e.g. convert 2.2.1 to 221
version=`echo $mvn_ver |  sed 's/\.//g'`
#verify that maven version is 2.0.9 or 2.2.1 and greater 
if [ "$version" -lt  "209" ]; then
        echo "Please do not use Maven version lower than 2.0.9."
    exit 1
elif [ "$version" -eq "220" ]; then
        echo "Please do not use Maven version 2.2.0."
    exit 1
fi

mvn_env=""
#get command options
while getopts r:u:b:D:U opt
do
    case "$opt" in
        r) maven_repo=$OPTARG;;
        u) repo_url=$OPTARG;;
        b) build_idl=$OPTARG;;
	D) mvn_env="$mvn_env -D$OPTARG";;
        U) update="-U";;
    esac
done
shift `expr $OPTIND - 1`

MAVEN_OPTS=${MAVEN_OPTS:='-Xmx1024m -Xms256m -XX:MaxPermSize=512m'} 
if [ "$maven_repo" != "" ]; then
    if [ -d "$maven_repo" ]; then
        echo "$maven_repo exists.  Continue with build."
    else
        echo "$maven_repo does not exist.  Create $maven_repo."
        mkdir "$maven_repo"
        _status=$?
	if [ "$_status" -ne 0 ]; then
            echo "Unable to create maven repository at: $maven_repo."
            exit "$_status"
        fi
    fi
fi

if [ "$maven_repo" = "" ]; then
    mvn $mvn_env $update "$@" | tee gfbuild.log
else
    mvn -Dmaven.repo.local=$maven_repo $mvn_env $update "$@" | tee gfbuild.log
fi
