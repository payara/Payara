# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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

 
das_address=$1
das_port=$2
das_domain=$3

${das_port:-"8080"}
${das_domain:-"domain1"}

echo "Install supplemental software"
sudo apt-get -y -q update
sudo apt-get -y -q upgrade
sudo apt-get -y -q install wget
sudo apt-get -y -q install sun-java6-jdk
sudo apt-get -y -q install unzip
sudo apt-get -y -q install openssh-server
sudo apt-get clean

echo "Installing GlassFish"
cd /opt
sudo mkdir glassfishvm
sudo chmod a+rwx glassfishvm
cd glassfishvm
wget http://$das_address:$das_port/glassfish.zip
unzip glassfish.zip
rm glassfish.zip

echo "Installing vitualization code"
wget http://$das_address:$das_port/vmcluster.jar
mv vmcluster.jar glassfish3/glassfish/modules

#echo "Installing Network reset script"
#mkdir bin
#cd bin
#wget http://$das_address:$das_port/ubuntu/initial-setup.sh
#chmod a+x initial-setup.sh

echo "Installing GlassFish as a startup service"
cd /etc/init.d
#sudo wget http://$das_address:$das_port/ubuntu/glassfish
sudo wget http://$das_address:$das_port/ubuntu/initial-setup.sh
sudo mv initial-setup.sh glassfish
sudo chmod a+x glassfish
sudo update-rc.d glassfish defaults

