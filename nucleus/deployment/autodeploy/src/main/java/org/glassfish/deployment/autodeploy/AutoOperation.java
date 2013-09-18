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

package org.glassfish.deployment.autodeploy;

import com.sun.enterprise.util.LocalStringManagerImpl;
import java.io.File;
import java.util.Properties;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.deployment.autodeploy.AutoDeployer.AutodeploymentStatus;
import org.glassfish.deployment.common.DeploymentUtils;
import javax.inject.Inject;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.internal.api.InternalSystemAdministrator;

import org.glassfish.logging.annotation.LogMessageInfo;

/**
 * Abstract class for operations the AutoDeployer can perform (currently
 * deploy and undeploy).
 * <p>
 * AutoOperation and its subclasses have no-arg constructors so they can be
 * initialized as services and an init method that accepts what might otherwise
 * be constructor arguments.
 * 
 * @author tjquinn
 */
@Service
@PerLookup
public abstract class AutoOperation {
    
    public static final Logger deplLogger =
        org.glassfish.deployment.autodeploy.AutoDeployer.deplLogger;

    @LogMessageInfo(message = "{0}", level="INFO")
    private static final String INFO_MSG = "NCLS-DEPLOYMENT-02035";

    @LogMessageInfo(message = "{0}", level="WARNING")
    private static final String WARNING_MSG = "NCLS-DEPLOYMENT-02036";

    @LogMessageInfo(message = "Error occurred: ", cause="An exception was caught when the operation was attempted", action="See the exception to determine how to fix the error", level="SEVERE")
    private static final String EXCEPTION_OCCURRED = "NCLS-DEPLOYMENT-02037";

    @LogMessageInfo(message = "Attempt to delete file {0} failed; no further information.", level="WARNING")
    private static final String DELETE_FAILED = "NCLS-DEPLOYMENT-02038";

    final static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(AutoDeployer.class);

    /**
     * Used in deleting all marker files for a given app.
     */
    final static private String [] autoDeployFileSuffixes = new String[] {
            AutoDeployConstants.DEPLOYED,
            AutoDeployConstants.DEPLOY_FAILED,
            AutoDeployConstants.UNDEPLOYED,
            AutoDeployConstants.UNDEPLOY_FAILED,
            AutoDeployConstants.PENDING
        };

    private File file;
    private Properties props;
    private String commandName;
    private AdminCommand command;
    
    @Inject
    private CommandRunner commandRunner;

    @Inject
    private AutodeployRetryManager retryManager;
    
    @Inject
    private InternalSystemAdministrator internalSystemAdministrator;
    
    /**
     * Initializes the AutoOperation.
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
     * Marks the files relevant to the specified file appropriately given the
     * outcome of the command as given in the status.
     * @param ds AutodeploymentStatus indicating the outcome of the operation
     * @param file file of interest
     */
    protected abstract void markFiles(AutodeploymentStatus ds, File file);
    
    /**
     * Returns the appropriate message string for the given operation and the
     * outcome.
     * @param ds AutodeploymentStatus value giving the outcome of the operation
     * @param file file of interest
     * @return message string to be logged
     */
    protected abstract String getMessageString(AutodeploymentStatus ds, File file);

    /**
     * Executes the operation
     * @return true/false depending on the outcome of the operation
     * @throws org.glassfish.deployment.autodeploy.AutoDeploymentException
     */
    final AutodeploymentStatus run() throws AutoDeploymentException {
        try {
            ParameterMap p = new ParameterMap();
            for (Map.Entry<Object,Object> entry : props.entrySet())
                p.set((String)entry.getKey(), (String)entry.getValue());
            ActionReport report = commandRunner.getActionReport("hk2-agent");
            CommandRunner.CommandInvocation inv = commandRunner.getCommandInvocation(commandName, report, internalSystemAdministrator.getSubject());
            inv.parameters(p).execute(command);
            AutodeploymentStatus ds = AutodeploymentStatus.forExitCode(report.getActionExitCode());
            if (ds.status) {
              deplLogger.log(Level.INFO,
                             INFO_MSG,
                             getMessageString(ds, file));
            } else {
                if(report.getMessage() != null){
                    deplLogger.log(Level.WARNING, WARNING_MSG, report.getMessage());
                }
              deplLogger.log(Level.WARNING,
                             WARNING_MSG,
                             getMessageString(ds, file));
            }
            markFiles(ds, file);
            /*
             * Choose the final status to report, based on the outcome of the
             * deployment as well as whether we are now monitoring this file.
             */
            ds = retryManager.chooseAutodeploymentStatus(report.getActionExitCode(), file);
            return ds;
        } catch (Exception ex) {
            /*
             * Log and continue.
             */
            deplLogger.log(Level.SEVERE,
                           EXCEPTION_OCCURRED,
                           ex);
            return AutodeploymentStatus.FAILURE;
        }
    }
    
    private File getSuffixedFile(File f, String suffix) {
        String absPath = f.getAbsolutePath();
        File ret = new File(absPath + suffix);
        return ret;
    }
    
    /**
     * Returns a File object for the "deployed" marker file for a given file.
     * @param f
     * @return File for the "deployed" marker file
     */
    protected File getDeployedFile(File f) {
        return getSuffixedFile(f, AutoDeployConstants.DEPLOYED);
    }
    
    /**
     * Returns a File object for the "deploy failed" marker file for a given file.
     * @param f
     * @return File for the "deploy failed" marker file
     */
    protected File getDeployFailedFile(File f) {
        return getSuffixedFile(f, AutoDeployConstants.DEPLOY_FAILED);
    }
    
    /**
     * Returns a File object for the "undeployed" marker file for a given file.
     * @param f
     * @return File for the "undeployed" marker file
     */
    protected File getUndeployedFile(File f) {
        return getSuffixedFile(f, AutoDeployConstants.UNDEPLOYED);
    }
    
    /**
     * Returns a File object for the "undeploy failed" marker file for a given file.
     * @param f
     * @return File for the "undeploy failed" marker file
     */
    protected File getUndeployFailedFile(File f) {
        return getSuffixedFile(f, AutoDeployConstants.UNDEPLOY_FAILED);
    }
    
    
    /**
     * Deletes all possible marker files for the file.
     * @param f the File whose markers should be removed
     */
    protected void deleteAllMarks(File f) {
        try {
            for (String suffix : autoDeployFileSuffixes) {
                final File suffixedFile = getSuffixedFile(f, suffix);
                if(suffixedFile.exists()){
                    if ( ! suffixedFile.delete()) {
                        deplLogger.log(Level.WARNING,
                                       DELETE_FAILED,
                                       suffixedFile.getAbsolutePath());
                    }
                }
            }
        } catch (Exception e) { 
            //ignore 
        }
    }
    
}
