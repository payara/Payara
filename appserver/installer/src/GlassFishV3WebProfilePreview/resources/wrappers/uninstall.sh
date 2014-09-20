#!/bin/sh
#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
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

#This script is a wrapper script for uninstallation process. This in turn
#invokes Open Installer scripts after validating the environment/parameters.
#Currently it is not localized.

displayJdkError() {
    	echo
    	echo "Could not locate a suitable Java runtime to run this program."
    	echo "Please ensure that you have Java 6 or newer installed on your system"
    	echo "and accessible through PATH or JAVA_HOME or by using -j option with"
    	echo "this program."
}

locateJava() {
    # Search path for locating java
    java_locs="$JAVA_HOME/bin:/bin:/usr/bin:/usr/java/bin:$PATH"
    # Convert colons to spaces
    java_locs=`echo $java_locs | tr ":" " "`

    for j in $java_locs; do
        # Check if version is sufficient
	isJavaVersionOkay ${j}
	if [ $? -eq 0 ]
	then
	#Return 0, if a compatible version of JDK is found
		return 0
	fi
	done
	#Return 1 if no compatible version of JDK is found, after having gone through the list.
	return 1
}


isJavaVersionOkay() {
        major=0
        minor=0
        if [ -x "${1}/java" ]; then
            version=`"${1}/java" -version 2>&1 | grep version | cut -d'"' -f2`
            major=`echo $version | cut -d'.' -f1`
            minor=`echo $version | cut -d'.' -f2`
        fi

        # We want 1.6 or newer
        if [ "$major" -eq "1" -a "$minor" -ge "6" ];  then
            echo "${1}/java"
            return 0
        fi
	return 1
}


parseArgs() {
#parse arguments passed to the script.
#Take care of -s option only, rest of the arguments
#are passed directly to uninstaller and all are validated there.
#The Java Home argument will be validated twice
JAVA_HOME_PASSED=0
while [ $# -gt 0 ]
do
arg="$1"
	case $arg in -s)
	CHECK_FOR_DISPLAY=0
	ARGS=`echo ${ARGS} -p Display-Mode=SILENT  `
	;;
  	-h)
        CHECK_FOR_DISPLAY=0
	ARGS=`echo ${ARGS} -h  `
        ;;
        -help)
        CHECK_FOR_DISPLAY=0
	ARGS=`echo ${ARGS} -h  `
        ;;
 	-j)
        shift
        if [ -z "$1" ]
        then    
                echo "Please provide a valid JDK installation directory along with -j option."
                exit ${JDKPATHARGUMENTVALUE_MISSING}
	fi
	# We are not trapping the returned string like when detecting default jdk.
	isJavaVersionOkay ${1}/bin
	if [ $? -eq 0 ]
	then
		ARGS=`echo ${ARGS} -j ${1} `
		JAVA_HOME_PASSED=1
	else
		displayJdkError
		exit ${COULD_NOT_FIND_COMPATIBLE_USERPROVIDED_JDK}
       	fi
        ;;
 	-l)
        shift
        if [ -d "$1" ]
        then    
		ARGS=`echo ${ARGS} -l ${1} `
	else
                echo "Please provide a valid and complete path to log directory along with -l option."
                exit ${LOGPATHARGUMENTVALUE_MISSING}
	fi
	;;
	*)
		ARGS=`echo ${ARGS} ${arg} `
	esac
shift
done
}
#validate JAVA_HOME, if the user has explicitly passed it using -j, then validate that value also.

#check the environment for the uninstaller to run.
checkUninstallEnv() {
#Check if the image has OI uninstallers.
if [ -f ${CWD}/install/bin/engine-wrapper -a  -f ${CWD}/install/bin/uninstaller ]
then
#Change permissions on OI launcher scripts
	/bin/chmod u+x ${CWD}/install/bin/engine-wrapper
	/bin/chmod u+x ${CWD}/install/bin/uninstaller

else
	echo "Required files for uninstaller missing from this directory. Aborting installation"
	exit ${OI_FILES_MISSING}
fi

#Check if config is available
if [ ! -d ${CWD}/var/install/config/Domain ]
then
	echo "Config directory is missing from this installation. Aborting uninstallation."
	exit ${CONFIG_DIRECTORY_MISSING}
fi

#Check if metadata is available
if [ ! -d ${CWD}/install/metadata ]
then
	echo "metadata directory is missing from this installation. Aborting uninstallation."
	exit ${METADATA_DIRECTORY_MISSING}
fi
}


#Run the uninstaller with required args.
fireUninstaller() {
cd ${CWD}/install/bin
#Pass in any additional arguments passed to the script.
./uninstaller -s ${CWD}/var/install/config/Domain -m file://${CWD}/install/metadata -p Default-Product-ID=Domain -p Pkg-Format=zip -J "-Dorg.openinstaller.provider.configurator.class=org.openinstaller.provider.conf.InstallationConfigurator" ${ARGS} 
EXIT_CODE=$?
if [ -f /bin/rm ]
then
	if [ -d ${CWD}/.org.opensolaris,pkg ]
	then
		/bin/rm -rf ${CWD}/.org.opensolaris,pkg ]
	fi
fi
#Pass the exit code from OI back to the env.

exit ${EXIT_CODE}
}

# Starts here..
# This would also get us the absolute path, in case users want to run 
# this script from other directories.
CURDIR=`dirname $0`
CWD=`cd $CURDIR; pwd`
ARGS=

#Error Codes
OI_FILES_MISSING=101
CONFIG_DIRECTORY_MISSING=102
METADATA_DIRECTORY_MISSING=103
INVALID_LOG_DIRECTORY=104
COULD_NOT_FIND_COMPATIBLE_INSTALLED_JDK=104
COULD_NOT_FIND_COMPATIBLE_USERPROVIDED_JDK=105
JDKPATHARGUMENTVALUE_MISSING=106
LOGPATHARGUMENTVALUE_MISSING=107
DISPLAY_NOT_SET=108


#Parse and validate the args
CHECK_FOR_DISPLAY=1
parseArgs $*

#Validate the JAVA_HOME
#User relies on environment JAVA_HOME value, if it is not explicitly passed
if [ ${JAVA_HOME_PASSED} -eq 0 ]
then
	my_java=`locateJava`
	if [ $? -eq 1 ]; then
	displayJdkError
    	exit ${COULD_NOT_FIND_COMPATIBLE_INSTALLED_JDK}
	fi
my_java_bin=`dirname $my_java`
J_HOME=`dirname $my_java_bin`
ARGS=`echo ${ARGS} -j ${J_HOME} `
fi

#validate the required files and directories and the environment
checkUninstallEnv

#We don't have to check for this variable in Silent Mode
if [ ${CHECK_FOR_DISPLAY} -eq 1 ]
then
        if [ -z "${DISPLAY}" ]
        then
         echo "This program requires DISPLAY environment variable to be set."
         echo "Please re-run after assigning an appropriate value to DISPLAY".
	 exit ${DISPLAY_NOT_SET}
        fi
fi
#Invoke uninstaller with required arguments
fireUninstaller $*

exit $?

