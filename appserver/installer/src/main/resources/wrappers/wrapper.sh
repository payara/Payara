#!/bin/sh
#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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

validateSilentFile() {
givenDirPath=`dirname $1`
givenFilePath=`basename $1`
if [ -f $givenDirPath/$givenFilePath ]
then
	absoluteFilePath="`cd $givenDirPath; pwd`/$givenFilePath"
	ARGS=`echo "${ARGS}" "${absoluteFilePath}" `
else
	echo "The silent installation file provided is not accessible. Please rerun this program with an appropriate statefile."
	exit 102
fi
}

validateAnswerFile() {
givenDirPath=`dirname $1`
givenFilePath=`basename $1`
if [ ! -f $givenDirPath/$givenFilePath ]
then
	absoluteFilePath="`cd $givenDirPath; pwd`/$givenFilePath"
	ARGS=`echo "${ARGS}" "${absoluteFilePath}" `
else
	echo "The answer file provided already exists. Please rerun this program by providing a non-existing answer file to be created."
	exit 104
fi
}

locate_java() {

    # Search path for locating java
    java_locs="$JAVA_HOME/bin:/bin:/usr/bin:/usr/java/bin:$PATH"
    # Convert colons to spaces
    java_locs=`echo $java_locs | tr ":" " "`

    for j in $java_locs; do
        # Check if version is sufficient
        major=0
        minor=0
        if [ -x "$j/java" ]; then
            version=`"$j/java" -version 2>&1 | grep version | cut -d'"' -f2`
            major=`echo $version | cut -d'.' -f1`
            minor=`echo $version | cut -d'.' -f2`
        fi

        # We want 1.6 or newer
        if [ "$major" -eq "1" -a "$minor" -ge "7" ];  then
            echo "$j/java"
            return
        fi
        if [ "$major" -gt "1" ];  then
            echo "$j/java"
            return
        fi
    done

    echo ""
}

locate_jar() {

    # Search path for locating jar
    jar_locs="$JAVA_HOME/bin:/bin:/usr/bin:/usr/java/bin:$PATH"
    # Convert colons to spaces
    jar_locs=`echo $jar_locs | tr ":" " "`

    for j in $jar_locs; do
        if [ -x "$j/jar" ]; then
            echo "$j/jar"
	    return
        fi
    done

    echo ""
}

ARGS=""
export ARGS 
_POSIX2_VERSION=199209
export _POSIX2_VERSION

#validate JAVA_HOME, leave full validation to OI.
my_java=`locate_java`

if [ -z "$my_java" ]; then
    echo
    echo "Could not locate a suitable Java runtime."
    echo "Please ensure that you have Java 7 or newer installed on your system"
    echo "and accessible in your PATH or by setting JAVA_HOME"
    exit 105
fi

my_java_bin=`dirname $my_java`
JAVA_HOME=`dirname $my_java_bin`
export JAVA_HOME

my_jar=`locate_jar`

if [ -z "$my_jar" ]; then
    echo
    echo "Could not locate a suitable jar utility."
    echo "Please ensure that you have Java 7 or newer installed on your system"
    echo "and accessible in your PATH or by setting JAVA_HOME"
    exit 105
fi

while [ $# -gt 0 ]
do
arg="$1"
if [ "${arg}" != "-s" ]
then
	ARGS="${ARGS} ${arg} "
fi
	case $arg in -n)
	shift
	if [ -z $1 ]
	then
		echo "Please provide a valid response file along with -n option."
		exit 103
	else
		validateAnswerFile $1
	fi
	;;
	-a)
	shift
	if [ -z $1 ]
	then
		echo "Please provide a valid answer file along with -a option."
		exit 101
	else
		validateSilentFile $1
	fi
	;;
	-R)
		echo "Invalid Argument, -R option is not applicable to this release."
		exit 101
	;;
	-r)
		echo "Invalid Argument, -r option is not applicable to this release."
		exit 101
	;;
	-s)
	ARGS=`echo ${ARGS} -p Display-Mode=SILENT `
	;;
	esac
shift
done
tmp=`mktemp -d -t install.XXXXXX`
if [ $? -ne 0 ]; then
    echo "Unable to create temporary directory, exiting..."
    exit 1
fi
echo "Extracting archive, please wait..."
tail +170l $0 > $tmp/tmp.jar
cd $tmp
$my_jar xvf tmp.jar 
$my_jar xvf ./Product/Packages/Engine.zip 
$my_jar xvf ./Product/Packages/Resources.zip 
$my_jar xvf ./Product/Packages/metadata.zip 
rm tmp.jar
chmod ugo+x product-installer.sh
chmod ugo+x install/bin/engine-wrapper
echo "InstallHome.directory.INSTALL_HOME=$HOME/glassfish4" > install.properties
sh product-installer.sh $ARGS
rm -rf ${tmp}/*
exit $?
