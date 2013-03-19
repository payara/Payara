#!/bin/sh
#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2007-2011 Oracle and/or its affiliates. All rights reserved.
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

 

# Sample fake product.
PRODUCTNAME="Domain"
ORIG_ARGS=$@

INST_DIR=/tmp/.launcher.$$
JAVA_LOC=${JAVA_HOME}

# binaries needed on both Solaris, Linux, etc.
CAT=/bin/cat
#CD=/bin/cd
CHMOD=/bin/chmod
CP=/bin/cp
CUT=/bin/cut
DIRNAME=/usr/bin/dirname
CPIO=/bin/cpio
FIND=/usr/bin/find
ECHO=/bin/echo
EGREP=/bin/egrep
ID=/usr/bin/id
MKDIR=/bin/mkdir
_PWD=/bin/pwd
RM=/bin/rm
SED=/bin/sed
SU=/bin/su
TOUCH=/bin/touch
UNAME=uname
XAUTH=/openwin/bin/xauth,/usr/X11R6/bin/xauth

TEXTDOMAINDIR="@LOCALEDIR@"

NAME=`basename $0`
MYDIR=`${DIRNAME} $0`
MYDIR=`cd ${MYDIR}; ${_PWD}`

ENGINE_DIR=${MYDIR}/install

# global settings
JAVA_HOME="$JAVA_HOME"				# java home path
JAVAOPTIONS="-Dorg.openinstaller.provider.configurator.class=org.openinstaller.provider.conf.InstallationConfigurator"			  
INSTALLPROPS=""     # install specific properties

# user options
DRYRUN=
ANSWERFILE=
ALTROOT=
DEBUGLEVEL="INFO"
MEDIALOC=
INSTALLABLES=
LOGDIR=/tmp

#-------------------------------------------------------------------------------
# usage only: define what parameters are available here
# input(s):  exitCode
#-------------------------------------------------------------------------------



usage() {
  ${CAT} <<EOF

Usage: <GlassFish installation program.> [-options]
where options include:
    -a <answerfile>
        run this program in silent mode using the answerfile provided, should be used with -s option.
    -l <Complete path to Log Directory>
        log information goes to this directory
    -q Logging level set to WARNING
    -v Logging level set to FINEST
    -s run this application silent mode
    -j <javahome>
        JRE installation directory to be used by this program.
    -n <savefile>
        run the program in dry-run mode and generate savefile to be used for silent mode
    -h
    -help
        print this help message
EOF
  exit $1
}

#-------------------------------------------------------------------------------
# perform actual operation for the script: install/uninstall
# input(s):  none
# output(s): instCode
#-------------------------------------------------------------------------------
perform() {

ENGINE_OPS="-m file://${MYDIR}/install/metadata/"
ENGINE_OPS="${ENGINE_OPS} -a file://${MYDIR}/install.properties"
ENGINE_OPS="${ENGINE_OPS} -i file://${MYDIR}/Product/"
ENGINE_OPS="${ENGINE_OPS} -p Default-Product-ID=${PRODUCTNAME}"
ENGINE_OPS="${ENGINE_OPS} -p Pkg-Format=zip"
ENGINE_OPS="${ENGINE_OPS} -C ${MYDIR}/pkg-bootstrap.jar:${MYDIR}/pkg-client.jar:${MYDIR}/registration-api.jar:${MYDIR}/registration-impl.jar:${MYDIR}/common-util.jar"


# add ubi-enabled packaging tool location to environment so that it
# is picked up by PH engine.
#
INSTALL_OSTOOLS=${MYDIR}
export INSTALL_OSTOOLS

if [ -n "${DRYRUN}" ] ; then
    ENGINE_OPS="${ENGINE_OPS} -n ${DRYRUN}"
fi

if [ -n "${ANSWERFILE}" ] ; then
    ENGINE_OPS="${ENGINE_OPS} -a ${ANSWERFILE}"
fi

if [ -n "${ALTROOT}" ] ; then
    ENGINE_OPS="${ENGINE_OPS} -R ${ALTROOT}"
fi

if [ -n "${LOGLEVEL}" ] ; then
    ENGINE_OPS="${ENGINE_OPS} -l ${LOGLEVEL}"
fi


if [ -n "${LOGDIR}" ] ; then
    ENGINE_OPS="${ENGINE_OPS} -p Logs-Location=${LOGDIR}"
fi

if [ -n "${JAVA_HOME}" ] ; then
    ENGINE_OPS="${ENGINE_OPS} -j ${JAVA_HOME}"
fi

if [ -n "${INSTALLPROPS}" ] ; then
    ENGINE_OPS="${ENGINE_OPS} ${INSTALLPROPS}"
fi

if [ -n "${INSTALLABLES}" ] ; then
    ENGINE_OPS="${ENGINE_OPS} -i ${INSTALLABLES}"
fi

${ENGINE_DIR}/bin/engine-wrapper -J "${JAVAOPTIONS}" ${ENGINE_OPS}
instCode=$?

}

#-------------------------------------------------------------------------------
# cleanup temporary files
#-------------------------------------------------------------------------------
cleanup() {

if [ ! -d ${INST_DIR} ] ; then
    return
fi

# Preventative measure to not nuke entire system
cd ${INST_DIR}
_pwd=`pwd`

if [ ${_pwd} != "/" ] ; then
    cd /
    echo "Cleaning up temporary environment."
    ${RM} -rf ${INST_DIR}
fi

}


#-------------------------------------------------------------------------------
# retrieve bundled JVM from Media based on os and platfo${RM}
# input(s):  none
# output(s): JAVAMEDIAPATH
#-------------------------------------------------------------------------------
getBundledJvm() {
  JAVAMEDIAPATH=""
  case `${UNAME} -s` in
    "SunOS")
       case `${UNAME} -p` in
         "sparc")
           JAVAMEDIAPATH="usr/jdk/instances/@pp.jre.pkg.basedirname@/"
           ;;
         "i386")
           JAVAMEDIAPATH="usr/jdk/instances/@pp.jre.pkg.basedirname@/"
           ;;
         *)
           echo  "Unknown platform, exiting"
	   exit 1
           ;;
       esac
       ;;
    "Linux")
       JAVAMEDIAPATH="usr/java/@pp.jre.rpm.basedirname@"
       ;;
    "HP-UX")
       JAVAMEDIAPATH="HP-UX"
       ;;
    "AIX")
       JAVAMEDIAPATH="AIX"
       ;;
    *)
      "Do not recognize `uname -p` platform, no JVM available"
      exit 1
      ;;
  esac
}

#-------------------------------------------------------------------------------
# login the user as root user
# use the 'su' command to ask for password and run the installer
#-------------------------------------------------------------------------------
loginAsRoot() {
USERID=`${ID} | ${CUT} -d'(' -f1 | ${CUT} -d'=' -f2`
if [ "$USERID" != "0" ]; then
    ${ECHO}
    echo "To use this installer, you will need to be the system's root user. \n"
    if [ -n "$DISPLAY" ]; then
      tmp_file="/tmp/installer_auth_$USER_$DISPLAY"
      touch $tmp_file
      ${CHMOD} 600 $tmp_file
      ${XAUTH} extract - $DISPLAY > $tmp_file
    fi
    status=1;
    retry=3;
    while [ $status = 1 -a ! $retry = 0 ]; do
      echo "Please enter this system's root user password \n"
      ${SU} root -c "${0} ${ORIG_ARGS}"
      status=$?
      retry=`expr $retry - 1`
      ${ECHO} " "
    done
    if [ "$retry" = 0 ]; then
      echo "Administrative privilege is req'd to perform this operation. Exiting.\n"
      exit 1
    fi
    exit
  fi
  unset userId
  unset status
  unset retry
}

useBundledJvm() {

  getBundledJvm
  JAVA_HOME=${BUNDLED_JAVA_JRE_LOC}/${JAVAMEDIAPATH}
  if [ ! -d ${JAVA_HOME} ] ; then
       echo  "${JAVA_HOME} must be the root directory of a valid JVM installation"
       echo  "Please provide JAVA_HOME as argument with -j option and proceed."
       exit 1
  fi
}

#-------------------------------------------------------------------------------
# ****************************** MAIN THREAD ***********************************
#-------------------------------------------------------------------------------

# Linux has no built-in support for long-style getopts so we use the short style only
LONGOPT="h(help)l:(logdir)q(quiet)v(verbose)t(text)n:(dry-run)a:(answerfile)j:(javahome)R:(altroot)J:(jvmoptions)p:(properties)"
SHORTOPT="hl:n:qvta:R:j:J:p:"

export TEXTDOMAINDIR

OS1=`${UNAME} -s`
OS2=`${UNAME} -r`
if [ "${OS1}" = SunOS ] ; then
    case "${OS2}" in
      2.* | 5.7 | 5.8)
        echo  "openInstaller is only supported on Solaris 9 or later"
        exit 1
        ;;
     esac
fi
# Long opt doesnt work on Open Solaris also, so have shortopt for all of OS.
OPTSTRING=${SHORTOPT}

# check arguments
while getopts "${OPTSTRING}" opt ; do
    case "${opt}" in

	a) ANSWERFILE=${OPTARG}
  ;;

	l) LOGDIR=${OPTARG}

	    if [ ! -d ${LOGDIR} -o ! -w ${LOGDIR} ] ; then
		echo  "${LOGDIR} is not a directory or is not writable"
		exit 1
	    fi
	;;

	q) LOGLEVEL=WARNING
	;;
	v) LOGLEVEL=FINEST
	;;
    n) DRYRUN=${OPTARG}
    ;;
	j) JAVA_HOME=${OPTARG}

	    if [ ! -d ${JAVA_HOME} -o ! -r ${JAVA_HOME} ] ; then
		echo  "${JAVA_HOME} must be the root directory of a valid JVM installation"
		exit 1
	    fi
	;;

	J) JAVAOPTIONS=${OPTARG}
	;;
	p) INSTALLPROPS="${INSTALLPROPS} -p ${OPTARG}"
	;;
	?|h) usage
	;;
    esac
done

${ECHO}
echo "Welcome to GlassFish installer"
${ECHO}

# check user for access privileges
#loginAsRoot

trap 'cleanup; exit' 1 2 13 15

# overwrite check if user specify javahome to use
if [ -z "$JAVA_HOME" ]; then
    echo "Creating temporary environment..."
    useBundledJvm
else
    echo  "Using the user defined JAVA_HOME : ${JAVA_HOME}"

fi

echo "Entering setup..."
perform
cleanup
exit $instCode
