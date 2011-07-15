/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.installer.conf;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.installer.util.GlassFishUtils;
import org.openinstaller.util.ClassUtils;
import org.openinstaller.util.ExecuteCommand;
import org.openinstaller.util.Msg;

/** Manages glassfish cluster related operations.
 * Operations such as start/stop/delete cluster are not exposed
 * through installer hence are not implemented in this class yet.
 * This utility class is invoked when the user chooses to create a
 * cluster as part of setting up a clustered glassfish instance.
 * Holds reference to Product object to get product wide information.
 *
 * @author sathyan
 */
public class ClusterManager {

    /* Reference to Product to obtain installation directory
     * and path to administration script.
     */
    private final Product productRef;
    /* Holds asadmin command output including the command line
     * of the recent runs. Gets overwritten on repeated calls to
     * asadmin commands. This text content will be used to construct
     * summary panel that displays the status/results of user configuration
     * actions.
     */
    private String outputFromRecentRun;
    /* Holds status of the configuration. Currently not used as createCluster
     * returns a valid Cluster object when the configuation is successful.
     * Can be used to double check in the calling code to make sure that
     * configuration was indeed successful/failure.
     */
    private boolean clusterConfigSuccessful;
    /* Logging */
    private static final Logger LOGGER;

    static {
        LOGGER = Logger.getLogger(ClassUtils.getClassName());
    }

    public ClusterManager(Product productRef) {
        this.productRef = productRef;
        outputFromRecentRun = null;
    }

    /* @return true/false, the value of the overall configuration status flag. */
    public boolean isClusterConfigSuccessful() {
        return clusterConfigSuccessful;
    }

    /* Creates the cluster by invoking asadmin's create-cluster command.
     * @param domainRef, reference to domain object. Used to check if the domain
     * is up or not. For a cluster to be created the domain should be running.
     * @param clusterName, name of the cluster to create.
     * @param clusterProperties, not currently used. refer to asadmin help create-cluster.
     * @param runningMode, "DRYRUN"/"REAL" "DRYRUN" mode would just return the
     * commandline and not execute it.
     * @returns a Cluster object, null if the cluster creation fails.
     */
    public Cluster createCluster(Domain domainRef, String clusterName, String clusterProperties, String runningMode) {

        DomainManager glassfishDomainManager = new DomainManager(productRef);


        clusterConfigSuccessful = true;

        LOGGER.log(Level.INFO, Msg.get("CHECKIF_DOMAIN_RUNNING", new String[]{domainRef.getDomainName()}));

        if (!glassfishDomainManager.isDomainRunning(domainRef.getDomainName())) {
            outputFromRecentRun = Msg.get("DOMAIN_NOT_RUNNING", new String[]{domainRef.getDomainName()});
            clusterConfigSuccessful = false;
            //TODO Start the domain..
            return null;
        }

        Cluster glassfishCluster = new Cluster(clusterName, clusterProperties);

        /* Create the actual command line for "asadmin create-cluster..." */
        ExecuteCommand asadminExecuteCommand =
                GlassFishUtils.assembleCreateClusterCommand(productRef, domainRef, glassfishCluster);

        outputFromRecentRun = "";

        if (asadminExecuteCommand != null) {
            LOGGER.log(Level.INFO, Msg.get("CREATE_CLUSTER", new String[]{clusterName}));

            /* Include the commandline also in the output. */
            outputFromRecentRun += asadminExecuteCommand.expandCommand(asadminExecuteCommand.getCommand()) + "\n";

            if (runningMode.contains("DRYRUN")) {
                /*
                Do not execute the command, this is useful when the clients just
                wanted the actual commandline and not execute the command.*/
                return glassfishCluster;
            }
            try {
                asadminExecuteCommand.setOutputType(ExecuteCommand.ERRORS | ExecuteCommand.NORMAL);
                asadminExecuteCommand.setCollectOutput(true);
                asadminExecuteCommand.execute();
                outputFromRecentRun += asadminExecuteCommand.getAllOutput();
                if (asadminExecuteCommand.getResult() == 1) {
                    clusterConfigSuccessful = false;
                    glassfishCluster = null;
                }

            } catch (Exception e) {
                LOGGER.log(Level.FINEST, e.getMessage());
                glassfishCluster = null;
                clusterConfigSuccessful = false;
            }
        } else {
            outputFromRecentRun = Msg.get("INVALID_CREATE_CLUSTER_COMMAND_LINE", null);
            clusterConfigSuccessful = false;
            glassfishCluster = null;
        }
        LOGGER.log(Level.INFO, outputFromRecentRun);
        return glassfishCluster;
    }

    /* No need for this functionality in 3.1, hence not implemented yet. */
    public boolean deleteCluster() {
        throw new UnsupportedOperationException(Msg.get("NOT_SUPPORTED_YET"));
    }

    /* No need for this functionality in 3.1, hence not implemented yet. */
    public boolean stopCluster() {
        throw new UnsupportedOperationException(Msg.get("NOT_SUPPORTED_YET"));
    }

    /* No need for this functionality in 3.1, hence not implemented yet. */
    public boolean startCluster() {
        throw new UnsupportedOperationException(Msg.get("NOT_SUPPORTED_YET"));
    }

    /* No need for this functionality in 3.1, hence not implemented yet. */
    public boolean isClusterRunning() {
        throw new UnsupportedOperationException(Msg.get("NOT_SUPPORTED_YET"));
    }

    /* Caller can get the output of recent asadmin command run. This has to be used
     * along with configSuccessful flag to find out the overall status of configuration.
     * @return String the whole of asadmin recent run's output including the command line.
     */
    public String getOutputFromRecentRun() {
        return this.outputFromRecentRun;
    }

    public boolean isConfigSuccessful() {
        return this.clusterConfigSuccessful;
    }
}
