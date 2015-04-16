#!/bin/bash -e
#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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

#check if maven is installed and in the path
which mvn 2>&1 > /dev/null
_status=$?
if [ ${_status} -ne 0 ]; then
    echo "Unable to find mvn in the path."
    echo "Please install Maven version 3.0.3 or above.  http://maven.apache.org/download.html"
    exit "$_status"
fi

#mvn is available in the path.  Now verify that it's the right version.
java_ver=`mvn -version 2>&1 | awk '/^Java version:/ {print substr($3,3,1)}'`
#verify JDK 1.7
echo "Java version: ${java_ver}"
if [ ${java_ver} -lt 7 ]; then
    echo "Please use JDK 1.7"
    exit 1
fi

# mvn -version returns "Maven version:..." in 2.0.9 
# and in >= 2.2.0 and up returns "Apache Maven..."  so need to
# apply awk with different string values.
mvn_ver=`mvn -version 2>&1 | awk '/^Maven version:/ {print $3}'`
if [ "${mvn_ver}" = "" ]; then
    mvn_ver=`mvn -version 2>&1 | awk '/^Apache Maven/ {print $3}'`
fi

# convert to number since shell is unable to compare float"
# e.g. convert x.y.z to xyz
version=`echo $mvn_ver |  sed 's/\.//g'`
#verify that maven version is >= 3.0.3
echo "Maven version: ${version}"
if [ ${version} -lt "303" ]; then
    echo "Please do not use Maven version lower than 3.0.3."
    exit 1
fi

is_scenario(){
    case "$1" in
        "build_re_dev" | \
        "build_re_nightly" | \
        "build_re_weekly" | \
        "promote_dev" | \
        "promote_nightly" | \
        "promote_weekly") echo 1;;
        *) echo 0;;
    esac
}

usage(){
    cat << END_USAGE
Usage: $0 [OPTION]... [ [-Dkey=value]... MAVEN_PHASE... | SCENARIO ]

  GlassFish build wrapper script.
  Verifies that a suitable mvn version (>= 3.0.3) is available in the PATH.
  Also verifies that JDK 1.7 is installed to compile GlassFish.
  
  -r or -Dmaven.local.repo can be specified to supply the local maven repository location,
  otherwise the default is used, which is user's home directory (~/.m2/repository).
  The supplied directory will be created if needed.
  
  The script saves the output in gfbuild.log.

  -r LOCAL_REPO_PATH
                    path to a directory that will be used as a local maven
                    repository.
  -U
                    forces update of SNAPSHOTs
  -h
                    display this help and exit
  MAVEN_PHASE
                    can be Maven phase, e.g.
                    clean
                    compile
                    package
                    install
  SCENARIO
                    build_re_dev
                    build_re_nightly
                    build_re_weekly
                    promote_dev
                    promote_nightly
                    promote_weekly
END_USAGE
    exit 0
}

build_args="$@"
build_arg1=`awk '{print $1}' <<< "$build_args"`

mvn_env=""
#get command options
while getopts r:D:U:h opt
do
    case "$opt" in
        r) maven_repo=$OPTARG;;
	D) mvn_env="$mvn_env -D$OPTARG";;
        U) update="-U"; build_args=`sed s@'-U '@@g <<< "$build_args"`;;
        h | *) usage;;
    esac
done
shift `expr $OPTIND - 1`

MAVEN_OPTS=${MAVEN_OPTS:='-Xmx1024m -Xms256m -XX:MaxPermSize=512m'} 
export MAVEN_OPTS
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

if [ ! -z $build_arg1 ] && [ $(is_scenario $build_arg1) -eq 1 ]
then
    source `dirname $0`/common.sh
    eval "$build_arg1"
else
    if [ "$maven_repo" = "" ]; then
        mvn $mvn_env $update $build_args | tee gfbuild.log
    else
        mvn -Dmaven.repo.local=$maven_repo $mvn_env $update $build_args | tee gfbuild.log
    fi
fi
