#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
Steps to run these automated tests:
-----------------------------------

1. Unzip latest version of glassfish.zip and set S1AS_HOME enviroment variable to point to the extracted GlassFish location.

  For example: export S1AS_HOME=/tmp/glassfish4/glassfish

2. Also set PAAS_TESTS_HOME environment variable to point to the location where paas tests are checked out.

 For example: export PAAS_TESTS_HOME=/tmp/main/appserver/tests/paas

3. If you are using Oracle database, Copy the Oracle database plugin jars into $S1AS_HOME/modules.
If you are using MySQL database, copy the MySQL database plugin jars into $S1AS_HOME/modules directory.
Remove $S1AS_HOME/modules/paas.javadbplugin.jar if a Database other than JavaDB is used. Else do the following :

When multiple database plugins are present in the modules directory, to register a particular database plugin as the default service provisioning engine, use the register-service-provisioning-engine command. For example,

asadmin register-service-provisioning-engine --type Database --defaultservice=true org.glassfish.paas.mysqldbplugin.MySQLDBPlugin

or

asadmin register-service-provisioning-engine --type Database --defaultservice=true org.glassfish.paas.javadbplugin.DerbyPlugin

4. [Only for Oracle DB] Copy Oracle jdbc driver (ojdbc14.jar) into $S1AS_HOME/domains/domain1/lib. Ref: http://download.oracle.com/otn/utilities_drivers/jdbc/10205/ojdbc14.jar

5. [Only for MySQL DB] Copy Mysql jdbc driver (mysql-connector-java-5.0.4-bin.jar) into $S1AS_HOME/domains/domain1/lib.

6. [Only for Native mode] Copy downloaded lb.zip under $S1AS_HOME/config directory.
[Only for OVM mode] Download and Copy the necessary OVM related jars into $S1AS_HOME/modules directory.

7. Setup virtualization enviroment for your GlassFish installation. 

   For example, run native_setup.sh to configure native IMS config. Modify kvm_setup.sh to suite your system details and run it. 

This step is optional in which case the service(s) required for this PaaS app will be provisioned in non-virtualized environment.

8. When the load balancer is used, specify the load balancer's port 50080'..eg., -DargLine="-Dhttp.port=50080" 

   GF_EMBEDDED_ENABLE_CLI=true mvn -Dhttps.proxyHost=www-proxy.us.oracle.com -Dhttps.proxyPort=80 clean verify surefire-report:report -DargLine="-Dhttp.port=50080"

   Without lb-plugin just skip the argument part.Deafult port of 28080 will be used.

   GF_EMBEDDED_ENABLE_CLI=true mvn -Dhttps.proxyHost=www-proxy.us.oracle.com -Dhttps.proxyPort=80 clean verify surefire-report:report

   The arguments -Dhttps.proxyHost=www-proxy.us.oracle.com -Dhttps.proxyPort=80 are included for the test 'mq-shared-service-test' to work properly.

