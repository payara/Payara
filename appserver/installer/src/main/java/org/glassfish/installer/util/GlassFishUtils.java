/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package org.glassfish.installer.util;

import java.util.logging.Logger;
import java.io.InputStream;
import org.glassfish.installer.conf.Cluster;
import org.glassfish.installer.conf.Domain;
import org.glassfish.installer.conf.Instance;
import org.glassfish.installer.conf.Product;
import org.glassfish.installer.conf.Service;
import org.openinstaller.util.ClassUtils;
import org.openinstaller.util.EnhancedException;
import org.openinstaller.util.ExecuteCommand;
import org.openinstaller.util.InvalidArgumentException;

/** Utility class to be used during GlassFish configuration.
 *
 * @author sathyan
 */
public class GlassFishUtils {

    /* LOGGING */
    private static final Logger LOGGER;

    static {
        LOGGER = Logger.getLogger(ClassUtils.getClassName());
    }
    /* Move it to Resources */
    static public String windowsCopyRightNoticeText =
            "@echo off\n"
            + "REM DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.\n"
            + "REM\n"
            + "REM Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.\n"
            + "REM\n"
            + "REM Use is subject to License Terms\n"
            + "REM\n";

    /* Move it to Resources */
    static public String unixCopyRightNoticeText =
            "#!/bin/sh\n"
            + "#\n"
            + "# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.\n"
            + "#\n"
            + "# Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.\n"
            + "#\n"
            + "# Use is subject to License Terms\n"
            + "#\n";

    /* Validates to make sure that the asadmin command line does not
     * include duplicate port values. Currently HTTP and Admin ports are
     * input by user and seven other ports(refer to this.PortArray[][])
     * have been hard-coded with constant values. This method makes sure
     * that the user-entered values are not duplicated in the assumptions
     * that asadmin makes. If so, then the assumptions(ports) will be
     * incremented by one. Returns the whole of formulated domainproperties
     * to be used in asadmin create-domain command line.
     *  Refer to Issue tracker issue #6173.
     * @param adminPort, DAS admin port.
     * @param httpPort, DAS Instance port.
     * @glassfishPortArray, holds rest of the ports used by a glassfish domain.
     * @return String a colon separated list of ports to be used as a value
     * for --domainproperties of create-domain command.
     */
    static public String getDomainProperties(String adminPort, String httpPort, String[][] glassfishPortArray) {
        String domainProperties = "";
        /* Check admin and http port given by user against
        the list of default ports used by asadmin. */
        for (int i = 0; i < glassfishPortArray.length; i++) {
            if (glassfishPortArray[i][1].equals(adminPort)
                    || glassfishPortArray[i][1].equals(httpPort)) {
                /* Convert string to a number, then add 1
                then convert it back to a string. Update the
                portArray with new port #. */
                Integer newPortNumber = Integer.parseInt(glassfishPortArray[i][1]) + 1;
                glassfishPortArray[i][1] = Integer.toString(newPortNumber);
            }

            // Store the modified array elements into the commandline
            domainProperties =
                    domainProperties + glassfishPortArray[i][0] + "=" + glassfishPortArray[i][1];

            /* Don't add a ":" to the last element :-), though asadmin ignores it,
            Safe not to put junk in commandline.
             */
            if (i < 5) {
                domainProperties = domainProperties + ":";
            }
        }
        return domainProperties;
    }


    /* Gathers asadmin create-domain commandline based on user entered parameters.
     * @param productRef, reference to Product object to get path to admin script
     * @param glassfishDomain, reference to a pre-filled Domain object to get
     * requied attributes of the domain to be created.
     * argument of create-domain.
     * @return String[] that can be passed to Runtime.exec)( to create the domain.
     * 
     */
    static public String[] assembleCreateDomainCommand(Product productRef,
            Domain glassfishDomain) {
        String[] asadminCommandArray = {productRef.getAdminScript(),
            "--user", glassfishDomain.getAdminUser(),
            "--passwordfile", "-",
            "create-domain",
            "--template=" + productRef.getInstallLocation() + "/glassfish/common/templates/gf/appserver-domain.jar",
            "--savelogin",
            "--checkports=" + (glassfishDomain.isCheckPorts() ? "true" : "false"),
            "--adminport", glassfishDomain.getAdminPort(),
            "--instanceport", glassfishDomain.getInstancePort(),
            "--domainproperties=" + glassfishDomain.getDomainProperties(),
            glassfishDomain.getDomainName()};

            return asadminCommandArray;
    }


    /* Gathers asadmin create-local-instance commandline based on user entered parameters.
     * @param productRef, reference to Product object to get path to admin script
     * @param glassfishInstance, reference to a pre-filled Instance object to get
     * requied attrbutes of the instance to be created.
     * @return ExecuteCommand, a pre-assembled executecommand object that can
     * be executed to create the instance.
     */
    static public ExecuteCommand assembleCreateInstanceCommand(Product productRef, Instance glassfishInstance) {
        try {
            ExecuteCommand asadminExecuteCommand = null;
            // Command line to create a clustered instance.
            // Ideally this should be "if (Instance typeof ClusteredInstance) equivalent"

            if (glassfishInstance.getClusterName() != null) {
                asadminExecuteCommand = new ExecuteCommand(new String[]{
                            productRef.getAdminScript(),
                            "--host", glassfishInstance.getServerHostName(),
                            "--port", glassfishInstance.getServerAdminPort(),
                            "create-local-instance",
                            "--cluster", glassfishInstance.getClusterName(),
                            glassfishInstance.getInstanceName()});
            } else {
                asadminExecuteCommand = new ExecuteCommand(new String[]{
                            productRef.getAdminScript(),
                            "--host", glassfishInstance.getServerHostName(),
                            "--port", glassfishInstance.getServerAdminPort(),
                            "create-local-instance",
                            glassfishInstance.getInstanceName()});
            }
            return asadminExecuteCommand;
        } catch (EnhancedException ex) {
        }
        return null;
    }

    /* @param installDir root of the installation directory.
     * @return String path to the config file based on OS.
     */
    static public String getGlassfishConfigFilePath(String installDir) {
        if (OSUtils.isWindows()) {
            return installDir + "\\glassfish\\config\\asenv.bat";
        }

        return installDir + "/glassfish/config/asenv.conf";
    }

    /* @param installDir root of the installation directory.
     * @return String path to the domain admin script based on OS.
     */
    static public String getGlassfishAdminScriptPath(String installDir) {
        if (OSUtils.isWindows()) {
            return installDir + "\\glassfish\\bin\\asadmin.bat";
        }
        return installDir + "/glassfish/bin/asadmin";
    }

    /* Gathers asadmin create-service commandline based on user entered parameters.
     * @param serviceRef, reference to Service object to get the required attributes
     * @param associatedDomain, reference to Domain object to get the name of the domain.
     * @return ExecuteCommand, a pre-assembled executecommand object that can
     * be executed to create the service.
     *
     */
    static public ExecuteCommand assembleCreateServiceCommand(Service serviceRef, Domain associatedDomain) {
        // Create a Service Object that sets up the service object and does create.
        ExecuteCommand asadminExecuteCommand = null;
        try {

            if (OSUtils.isWindows()) {
                asadminExecuteCommand = new ExecuteCommand(
                        new String[]{
                            serviceRef.getAssociatedExecutable(),
                            "create-service",
                            "--name", serviceRef.getServiceName(),
                            associatedDomain.getDomainName()});
            } else {
                // Check to see if the service Properties are customized.
                // if not just create the service without --serviceProperties
                if (serviceRef.getServiceProps() != null && serviceRef.getServiceProps().trim().length() > 0) {
                    asadminExecuteCommand = new ExecuteCommand(
                            new String[]{
                                serviceRef.getAssociatedExecutable(),
                                "create-service",
                                "--name", serviceRef.getServiceName(),
                                "--serviceproperties", serviceRef.getServiceProps(),
                                associatedDomain.getDomainName()});
                } else {
                    asadminExecuteCommand = new ExecuteCommand(
                            new String[]{
                                serviceRef.getAssociatedExecutable(),
                                "create-service",
                                "--name", serviceRef.getServiceName(),
                                associatedDomain.getDomainName()});
                }
            }
        } catch (Exception e) {
            asadminExecuteCommand = null;
        }
        return asadminExecuteCommand;
    }

    /* Gathers asadmin create-cluster commandline based on user entered parameters.
     * @param productRef, reference to Product object to get path to admin script
     * @param clusterRef, reference to a pre-filled Cluster object to get
     * requied attrbutes of the Cluster to be created.
     * @param domainRef, reference to a pre-filled domain object to get required
     * attributes of the domain with which this cluster is associated.
     * @return ExecuteCommand, a pre-assembled executecommand object that can
     * be executed to create the cluster.
     */
    static public ExecuteCommand assembleCreateClusterCommand(Product productRef, Domain domainRef, Cluster clusterRef) {
        try {
            ExecuteCommand asadminExecuteCommand = null;
            asadminExecuteCommand = new ExecuteCommand(new String[]{
                        productRef.getAdminScript(),
                        "create-cluster",
                        clusterRef.getClusterName(),
                        "--port",
                        domainRef.getAdminPort()});
            return asadminExecuteCommand;
        } catch (EnhancedException ex) {
        }
        return null;
    }

    /* Gathers asadmin list-domains commandline based on user entered parameters.
     * @param productRef, reference to Product object to get path to admin script
     * @return ExecuteCommand, a pre-assembled executecommand object that can
     * be executed to list the domains.
     */
    static public ExecuteCommand assembleListDomainsCommand(Product productRef) {
        try {
            ExecuteCommand asadminExecuteCommand = null;
            asadminExecuteCommand = new ExecuteCommand(new String[]{
                        productRef.getAdminScript(),
                        "list-domains"});
            return asadminExecuteCommand;
        } catch (EnhancedException ex) {
        }
        return null;
    }

    /* Gathers asadmin start-domain commandline based on user entered parameters.
     * @param productRef, reference to Product object to get path to admin script
     * @param domainName, name of the domain to start.
     * @return ExecuteCommand, a pre-assembled executecommand object that can
     * be executed to start the specified domain.
     */
    static public ExecuteCommand assembleStartDomainCommand(Product productRef, String domainName) {
        try {
            ExecuteCommand asadminExecuteCommand = null;
            asadminExecuteCommand = new ExecuteCommand(new String[]{
                        productRef.getAdminScript(),
                        "start-domain",
                        domainName
                    });
            return asadminExecuteCommand;
        } catch (EnhancedException ex) {
        }
        return null;
    }

    static public boolean isGlassFishInstalledHere(String installDir) {

        return FileUtils.isFileExist(getGlassfishAdminScriptPath(installDir))
                || FileUtils.isFileExist(getGlassfishConfigFilePath(installDir));

    }
}
