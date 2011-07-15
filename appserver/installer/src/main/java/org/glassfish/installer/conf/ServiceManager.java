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

/** Manages glassfish services related operations.
 * Operations such as start/stop/delete service are not exposed
 * through installer hence are not implemented in this class yet.
 * This utility class is invoked when the user chooses to create a
 * service as part of setting up glassfish domains.
 * @author sathyan
 */
public class ServiceManager {


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
    private boolean serviceConfigSuccessful;
    /* Logging */
    private static final Logger LOGGER;

    static {
        LOGGER = Logger.getLogger(ClassUtils.getClassName());
    }

    /* Creates the service by invoking asadmin's create-service command.
     * @param associatedDomain, reference to domain object, for now used only
     * to get the name.
     * @param serviceName, name of the service to create.
     * @param serviceProperties, used only on non-windows platforms.
     * @param runningMode, "DRYRUN"/"REAL" "DRYRUN" mode would just return the
     * commandline and not execute it.
     * @returns a Service object, null if the Service creation fails.
     */
    public Service createService(String serviceName, String serviceProperties, String commandLine, Domain associatedDomain, String runningMode) {

        serviceConfigSuccessful = true;

        /* Set service attributes. */
        Service glassfishService = new Service(serviceName, serviceProperties, commandLine);

        /* Gather asadmin create-service commandline. */
        ExecuteCommand asadminExecuteCommand =
                GlassFishUtils.assembleCreateServiceCommand(glassfishService, associatedDomain);

        outputFromRecentRun = "";

        if (asadminExecuteCommand != null) {
            LOGGER.log(Level.INFO, Msg.get("CREATE_SERVICE", new String[]{serviceName}));

            outputFromRecentRun += asadminExecuteCommand.expandCommand(asadminExecuteCommand.getCommand()) + "\n";

            if (runningMode.contains("DRYRUN")) {
                /*
                Do not execute the command, this is useful when the clients just 
                wanted the actual commandline and not execute the command.*/
                return glassfishService;
            }

            try {
                asadminExecuteCommand.setOutputType(ExecuteCommand.ERRORS | ExecuteCommand.NORMAL);
                asadminExecuteCommand.setCollectOutput(true);
                asadminExecuteCommand.execute();
                outputFromRecentRun = asadminExecuteCommand.getAllOutput();
                if (asadminExecuteCommand.getResult() == 1) {
                    serviceConfigSuccessful = false;
                    glassfishService = null;
                }

            } catch (Exception e) {
                LOGGER.log(Level.FINEST, e.getMessage());
                glassfishService = null;
                serviceConfigSuccessful = false;
            }
        } else {
            serviceConfigSuccessful = false;
            outputFromRecentRun = Msg.get("INVALID_CREATE_SERVICE_COMMAND_LINE");
            glassfishService = null;
        }
        LOGGER.log(Level.INFO, outputFromRecentRun);

        return glassfishService;
    }

    public boolean deleteService(Service serviceReference) {
        throw new UnsupportedOperationException(Msg.get("NOT_SUPPORTED_YET"));
    }

    public boolean stopService(Service serviceReference) {
        throw new UnsupportedOperationException(Msg.get("NOT_SUPPORTED_YET"));
    }

    public boolean startService(Service serviceReference) {
        throw new UnsupportedOperationException(Msg.get("NOT_SUPPORTED_YET"));
    }

    public boolean isServiceRunning(Service serviceReference) {
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
        return this.serviceConfigSuccessful;
    }
}
