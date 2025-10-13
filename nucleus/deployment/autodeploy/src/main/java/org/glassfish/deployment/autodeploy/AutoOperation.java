/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2019-2021] Payara Foundation and/or affiliates
package org.glassfish.deployment.autodeploy;

import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;
import static org.glassfish.deployment.autodeploy.AutoDeployConstants.DEPLOYED;
import static org.glassfish.deployment.autodeploy.AutoDeployConstants.DEPLOY_FAILED;
import static org.glassfish.deployment.autodeploy.AutoDeployConstants.PENDING;
import static org.glassfish.deployment.autodeploy.AutoDeployConstants.UNDEPLOYED;
import static org.glassfish.deployment.autodeploy.AutoDeployConstants.UNDEPLOY_FAILED;
import static org.glassfish.deployment.autodeploy.AutoDeployer.AutodeploymentStatus.FAILURE;

import java.io.File;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import jakarta.inject.Inject;

import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.deployment.autodeploy.AutoDeployer.AutodeploymentStatus;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.internal.api.InternalSystemAdministrator;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.util.LocalStringManagerImpl;

/**
 * Abstract class for operations the AutoDeployer can perform (currently deploy and undeploy).
 * <p>
 * AutoOperation and its subclasses have no-arg constructors so they can be initialized as services and an init method
 * that accepts what might otherwise be constructor arguments.
 * 
 * @author tjquinn
 */
@Service
@PerLookup
public abstract class AutoOperation {

    public static final Logger deplLogger = AutoDeployer.deplLogger;

    @LogMessageInfo(message = "{0}", level = "INFO")
    private static final String INFO_MSG = "NCLS-DEPLOYMENT-02035";

    @LogMessageInfo(message = "{0}", level = "WARNING")
    private static final String WARNING_MSG = "NCLS-DEPLOYMENT-02036";

    @LogMessageInfo(message = "Error occurred: ", cause = "An exception was caught when the operation was attempted", action = "See the exception to determine how to fix the error", level = "SEVERE")
    private static final String EXCEPTION_OCCURRED = "NCLS-DEPLOYMENT-02037";

    @LogMessageInfo(message = "Attempt to delete file {0} failed; no further information.", level = "WARNING")
    private static final String DELETE_FAILED = "NCLS-DEPLOYMENT-02038";

    final static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(AutoDeployer.class);

    /**
     * Used in deleting all marker files for a given app.
     */
    final static private String[] autoDeployFileSuffixes = new String[] { DEPLOYED, DEPLOY_FAILED, UNDEPLOYED, UNDEPLOY_FAILED, PENDING };

    @Inject
    private CommandRunner commandRunner;

    @Inject
    private AutodeployRetryManager retryManager;

    @Inject
    private InternalSystemAdministrator internalSystemAdministrator;
    
    private File file;
    private Properties props;
    private String commandName;
    private AdminCommand command;

    /**
     * Initializes the AutoOperation.
     * 
     * @param file the File of interest
     * @param props command-line options to be passed to the relevant AdminCommand (deploy or undeploy)
     * @param commandName name of the command to execute
     * @param command the AdminCommand descendant to execute
     * @return this same operation
     */
    AutoOperation init(File file, Properties props, String commandName, AdminCommand command) {
        this.file = file;
        this.props = props;
        this.commandName = commandName;
        this.command = command;
        
        return this;
    }

    /**
     * Executes the operation
     * 
     * @return true/false depending on the outcome of the operation
     * @throws org.glassfish.deployment.autodeploy.AutoDeploymentException
     */
    public final AutodeploymentStatus run() throws AutoDeploymentException {
        try {
            ActionReport report = commandRunner.getActionReport("hk2-agent");
            
            commandRunner.getCommandInvocation(commandName, report, internalSystemAdministrator.getSubject())
                         .parameters(getParameters())
                         .execute(command);
            
            AutodeploymentStatus autodeploymentStatus = AutodeploymentStatus.forExitCode(report.getActionExitCode());
            if (autodeploymentStatus.status) {
                deplLogger.log(INFO, INFO_MSG, getMessageString(autodeploymentStatus, file));
            } else {
                if (report.getMessage() != null) {
                    deplLogger.log(WARNING, WARNING_MSG, report.getMessage());
                }
                deplLogger.log(WARNING, WARNING_MSG, getMessageString(autodeploymentStatus, file));
            }
            
            markFiles(autodeploymentStatus, file);
            
            /*
             * Choose the final status to report, based on the outcome of the deployment as well as whether we are now monitoring
             * this file.
             */
            return retryManager.chooseAutodeploymentStatus(report.getActionExitCode(), file);
        } catch (Exception ex) {
            /*
             * Log and continue.
             */
            deplLogger.log(SEVERE, EXCEPTION_OCCURRED, ex);
            return FAILURE;
        }
    }

    /**
     * Marks the files relevant to the specified file appropriately given the outcome of the command as given in the status.
     * 
     * @param ds AutodeploymentStatus indicating the outcome of the operation
     * @param file file of interest
     */
    protected abstract void markFiles(AutodeploymentStatus ds, File file);

    /**
     * Returns the appropriate message string for the given operation and the outcome.
     * 
     * @param ds AutodeploymentStatus value giving the outcome of the operation
     * @param file file of interest
     * @return message string to be logged
     */
    protected abstract String getMessageString(AutodeploymentStatus ds, File file);

    /**
     * Returns a File object for the "deployed" marker file for a given file.
     * 
     * @param file
     * @return File for the "deployed" marker file
     */
    protected File getDeployedFile(File file) {
        return getSuffixedFile(file, DEPLOYED);
    }

    /**
     * Returns a File object for the "deploy failed" marker file for a given file.
     * 
     * @param file
     * @return File for the "deploy failed" marker file
     */
    protected File getDeployFailedFile(File file) {
        return getSuffixedFile(file, DEPLOY_FAILED);
    }

    /**
     * Returns a File object for the "undeployed" marker file for a given file.
     * 
     * @param file
     * @return File for the "undeployed" marker file
     */
    protected File getUndeployedFile(File file) {
        return getSuffixedFile(file, UNDEPLOYED);
    }

    /**
     * Returns a File object for the "undeploy failed" marker file for a given file.
     * 
     * @param file
     * @return File for the "undeploy failed" marker file
     */
    protected File getUndeployFailedFile(File file) {
        return getSuffixedFile(file, UNDEPLOY_FAILED);
    }

    /**
     * Deletes all possible marker files for the file.
     * 
     * @param file the File whose markers should be removed
     */
    protected void deleteAllMarks(File file) {
        try {
            for (String suffix : autoDeployFileSuffixes) {
                final File suffixedFile = getSuffixedFile(file, suffix);
                if (suffixedFile.exists()) {
                    if (!suffixedFile.delete()) {
                        deplLogger.log(WARNING, DELETE_FAILED, suffixedFile.getAbsolutePath());
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
    }
    
    private File getSuffixedFile(File file, String suffix) {
        return new File(file.getAbsolutePath() + suffix);
    }
    
    private ParameterMap getParameters() {
        ParameterMap parameters = new ParameterMap();
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            parameters.set((String) entry.getKey(), (String) entry.getValue());
        }
        
        return parameters;
    }

}
