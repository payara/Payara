#!/bin/bash
################################################################################
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
################################################################################

################################################################################
#
# A script to append deploy commands to the post boot command file at
# $PAYARA_HOME/scripts/post-boot-commands.asadmin file. All applications in the
# $DEPLOY_DIR (either files or folders) will be deployed.
# The $POSTBOOT_COMMANDS file can then be used with the start-domain using the
#  --postbootcommandfile parameter to deploy applications on startup.
#
# Usage:
# ./generate_deploy_commands.sh
#
# Optionally, any number of parameters of the asadmin deploy command can be
# specified as parameters to this script.
# E.g., to deploy applications with implicit CDI scanning disabled:
#
# ./generate_deploy_commands.sh --properties=implicitCdiEnabled=false
#
# Environment variables used:
#   - $PREBOOT_COMMANDS - the pre boot command file.
#   - $PREBOOT_COMMANDS_FINAL - copy of the pre boot command file.
#   - $POSTBOOT_COMMANDS - the post boot command file.
#   - $POSTBOOT_COMMANDS_FINAL - copy of the post boot command file.
#
# Note that many parameters to the deploy command can be safely used only when
# a single application exists in the $DEPLOY_DIR directory.
################################################################################

# Check required variables are set
if [ -z $DEPLOY_DIR ]; then echo "Variable DEPLOY_DIR is not set."; exit 1; fi
if [ -z $PREBOOT_COMMANDS ]; then echo "Variable PREBOOT_COMMANDS is not set."; exit 1; fi
if [ -z $PREBOOT_COMMANDS_FINAL ]; then echo "Variable PREBOOT_COMMANDS_FINAL is not set."; exit 1; fi
if [ -z $POSTBOOT_COMMANDS ]; then echo "Variable POSTBOOT_COMMANDS is not set."; exit 1; fi
if [ -z $POSTBOOT_COMMANDS_FINAL ]; then echo "Variable POSTBOOT_COMMANDS_FINAL is not set."; exit 1; fi

# Create pre and post boot command files if they don't exist
touch $PREBOOT_COMMANDS_FINAL
touch $POSTBOOT_COMMANDS_FINAL

# Create copy of POSTBOOT_COMMANDS instead of modifying original and add new line
[ -f  $POSTBOOT_COMMANDS ] && cp $POSTBOOT_COMMANDS $POSTBOOT_COMMANDS_FINAL
echo >> $POSTBOOT_COMMANDS_FINAL

# Create copy of PREBOOT_COMMANDS instead of modifying original and add new line
[ -f  $PREBOOT_COMMANDS ] && cp $PREBOOT_COMMANDS $PREBOOT_COMMANDS_FINAL
echo >> $PREBOOT_COMMANDS_FINAL

deploy() {

  if [ -z $1 ]; then
    echo "No deployment specified";
    exit 1;
  fi

  DEPLOY_STATEMENT="deploy $DEPLOY_PROPS $1"
  if grep -q $1 $POSTBOOT_COMMANDS_FINAL; then
    echo "post boot commands already deploys $1";
  else
    echo "Adding deployment target $1 to post boot commands";
    echo $DEPLOY_STATEMENT >> $POSTBOOT_COMMANDS_FINAL;
  fi
}

# RAR files first
for deployment in $(find $DEPLOY_DIR -mindepth 1 -maxdepth 1 -name "*.rar");
do
  deploy $deployment;
done

# Then every other WAR, EAR, JAR or directory
for deployment in $(find $DEPLOY_DIR -mindepth 1 -maxdepth 1 ! -name "*.rar" -a -name "*.war" -o -name "*.ear" -o -name "*.jar" -o -type d);
do
  deploy $deployment;
done
