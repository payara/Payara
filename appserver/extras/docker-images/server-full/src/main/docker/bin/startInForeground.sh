#!/bin/bash
##########################################################################################################
#    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
#    Copyright (c) 2023 Payara Foundation and/or its affiliates. All rights reserved.
#
#    The contents of this file are subject to the terms of either the GNU
#    General Public License Version 2 only ("GPL") or the Common Development
#    and Distribution License("CDDL") (collectively, the "License").  You
#    may not use this file except in compliance with the License.  You can
#    obtain a copy of the License at
#    https://github.com/payara/Payara/blob/main/LICENSE.txt
#    See the License for the specific
#    language governing permissions and limitations under the License.
#
#    When distributing the software, include this License Header Notice in each
#    file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
#
#    GPL Classpath Exception:
#    The Payara Foundation designates this particular file as subject to the "Classpath"
#    exception as provided by the Payara Foundation in the GPL Version 2 section of the License
#    file that accompanied this code.
#
#    Modifications:
#    If applicable, add the following below the License Header, with the fields
#    enclosed by brackets [] replaced by your own identifying information:
#    "Portions Copyright [year] [name of copyright owner]"
#
#    Contributor(s):
#    If you wish your version of this file to be governed by only the CDDL or
#    only the GPL Version 2, indicate your decision by adding "[Contributor]
#    elects to include this software in this distribution under the [CDDL or GPL
#    Version 2] license."  If you don't indicate a single choice of license, a
#    recipient has the option to distribute your version of this file under
#    either the CDDL, the GPL Version 2 or to extend the choice of license to
#    its licensees as provided above.  However, if you add GPL Version 2 code
#    and therefore, elected the GPL Version 2 license, then the option applies
#    only if the new code is made subject to such option by the copyright
#    holder.
##########################################################################################################

##########################################################################################################
#
# This script is to execute Payara Server in foreground, mainly in a docker environment.
# It allows to avoid running 2 instances of JVM, which happens with the start-domain --verbose command.
#
# Usage:
#   Running
#        startInForeground.sh <arguments>
#   is equivalent to running
#        asadmin start-domain <arguments>
#
# It's possible to use any arguments of the start-domain command as arguments to startInForeground.sh
#
# Environment variables used:
#   - $ADMIN_USER - the username to use for the asadmin utility.
#   - $PASSWORD_FILE - the password file to use for the asadmin utility.
#   - $PREBOOT_COMMANDS - the pre boot command file.
#   - $PREBOOT_COMMANDS_FINAL - copy of the pre boot command file.
#   - $POSTBOOT_COMMANDS - the post boot command file.
#   - $POSTBOOT_COMMANDS_FINAL - copy of the post boot command file.
#   - $DOMAIN_NAME - the name of the domain to start.
#   - $JVM_ARGS - extra JVM options to pass to the Payara Server instance.
#   - $AS_ADMIN_MASTERPASSWORD - the master password for the Payara Server instance.
#
# This script executes the asadmin tool which is expected at ~/appserver/bin/asadmin.
#
##########################################################################################################

# Check required variables are set
if [ -z $ADMIN_USER ]; then echo "Variable ADMIN_USER is not set."; exit 1; fi
if [ -z $PASSWORD_FILE ]; then echo "Variable PASSWORD_FILE is not set."; exit 1; fi
if [ -z $PREBOOT_COMMANDS ]; then echo "Variable PREBOOT_COMMANDS is not set."; exit 1; fi
if [ -z $PREBOOT_COMMANDS_FINAL ]; then echo "Variable PREBOOT_COMMANDS_FINAL is not set."; exit 1; fi
if [ -z $POSTBOOT_COMMANDS ]; then echo "Variable POSTBOOT_COMMANDS is not set."; exit 1; fi
if [ -z $POSTBOOT_COMMANDS_FINAL ]; then echo "Variable POSTBOOT_COMMANDS_FINAL is not set."; exit 1; fi
if [ -z $DOMAIN_NAME ]; then echo "Variable DOMAIN_NAME is not set."; exit 1; fi

# The following command gets the command line to be executed by start-domain
# - print the command line to the server with --dry-run, each argument on a separate line
# - remove -read-string argument
# - surround each line except with parenthesis to allow spaces in paths
# - remove lines before and after the command line and squash commands on a single line

# Create pre and post boot command files if they don't exist
touch $PREBOOT_COMMANDS_FINAL
touch $POSTBOOT_COMMANDS_FINAL

OUTPUT=`${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} start-domain --dry-run --prebootcommandfile=${PREBOOT_COMMANDS_FINAL} --postbootcommandfile=${POSTBOOT_COMMANDS_FINAL} $@ $DOMAIN_NAME`
STATUS=$?
if [ "$STATUS" -ne 0 ]
  then
    echo ERROR: $OUTPUT >&2
    exit 1
fi

COMMAND=`echo "$OUTPUT"\
 | sed -n -e '2,/^$/p'\
 | sed "s|glassfish.jar|glassfish.jar $JVM_ARGS |g"`

echo Executing Payara Server with the following command line:
echo $COMMAND | tr ' ' '\n'
echo

# Run the server in foreground - read master password from variable or file or use the default "changeit" password

set +x
if test "$AS_ADMIN_MASTERPASSWORD"x = x -a -f "$PASSWORD_FILE"
  then
    source "$PASSWORD_FILE"
fi
if test "$AS_ADMIN_MASTERPASSWORD"x = x
  then
    AS_ADMIN_MASTERPASSWORD=changeit
fi
echo "AS_ADMIN_MASTERPASSWORD=$AS_ADMIN_MASTERPASSWORD" > /tmp/masterpwdfile
exec ${COMMAND} < /tmp/masterpwdfile
