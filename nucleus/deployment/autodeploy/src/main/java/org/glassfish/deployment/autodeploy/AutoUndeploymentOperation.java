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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
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

import static com.sun.enterprise.util.io.FileUtils.deleteFile;
import static com.sun.enterprise.util.io.FileUtils.liquidate;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.WARNING;
import static org.glassfish.deployment.common.DeploymentProperties.DEFAULT_APP_NAME;

import java.io.File;
import java.util.Properties;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.glassfish.api.admin.AdminCommand;
import org.glassfish.deployment.autodeploy.AutoDeployer.AutodeploymentStatus;
import org.glassfish.deployment.common.DeploymentProperties;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.config.serverbeans.Application;
import com.sun.enterprise.config.serverbeans.Applications;

/**
 * Performs a single auto-undeploy operation for a single file.
 * <p>
 * Note - Use the newInstance static method to obtain a fully-injected operation; it is safer and more convenient than
 * using the no-arg constructor and then invoking init yourself.
 * 
 * @author tjquinn
 */
@Service
@PerLookup
public class AutoUndeploymentOperation extends AutoOperation {
    
    private static final String COMMAND_NAME = "undeploy";
    
    @LogMessageInfo(message = "Attempt to create file {0} failed; no further information.", level = "WARNING")
    private static final String CREATE_FAILED = "NCLS-DEPLOYMENT-02039";

    @Inject
    private Applications apps;
    
    @Inject
    private AutodeployRetryManager retryManager;

    @Inject
    @Named(COMMAND_NAME)
    private AdminCommand undeployCommand;


    /**
     * Creates a new, injected, and initialized AutoUndeploymentOperation object.
     * 
     * @param serviceLocator
     * @param file
     * @param name
     * @param target
     * @return the AutoUndeploymentOperation object
     */
    public static AutoUndeploymentOperation newInstance(ServiceLocator serviceLocator, File file, String name, String target) {
        AutoUndeploymentOperation autoUndeploymentOperation = serviceLocator.getService(AutoUndeploymentOperation.class);
        autoUndeploymentOperation.init(file, name, target);
        
        return autoUndeploymentOperation;
    }

    /**
     * Completes the intialization of the object.
     * 
     * @param appFile
     * @param name
     * @param target
     * @return the AutoUndeployOperation for convenience
     */
    protected AutoUndeploymentOperation init(File appFile, String name, String target) {
        super.init(appFile, prepareUndeployActionProperties(name, target), COMMAND_NAME, undeployCommand);
        
        return this;
    }

    private Properties prepareUndeployActionProperties(String archiveName, String target) {
        DeploymentProperties deploymentProperties = new DeploymentProperties();

        // We need to find the application registration name
        // which is not always the same as archive name
        String applicationName = archiveName;
        for (Application application : apps.getApplications()) {
            String defaultAppName = application.getDeployProperties().getProperty(DEFAULT_APP_NAME);
            if (defaultAppName != null && defaultAppName.equals(archiveName)) {
                applicationName = application.getName();
            }
        }
        
        deploymentProperties.setName(applicationName);
        deploymentProperties.setTarget(target);
        
        return deploymentProperties;
    }

    /**
     * {@inheritDoc}
     */
    protected String getMessageString(AutodeploymentStatus autodeploymentStatus, File file) {
        return localStrings.getLocalString(autodeploymentStatus.undeploymentMessageKey, autodeploymentStatus.undeploymentDefaultMessage, file);
    }

    /**
     * {@inheritDoc}
     */
    protected void markFiles(AutodeploymentStatus autodeploymentStatus, File file) {
        /*
         * Before managing the marker file for the app, see if the autodeployer is responsible for deleting this app file and,
         * if so, delete it.
         * 
         * Normally users will delete the application file themselves. Especially in the case of directories, though, users may
         * create the file ${fileName}_undeployRequested and have the autodeployer delete the file. <p> This avoids problems if
         * the user-initiated deletion of a large file or directory takes longer than the autodeployer cycle time. If a file has
         * been removed from the top-level directory, the autodeployer will see the updated timestamp on the directory and can
         * only decide that this is a new file - at least a newer file - to be autodeployed. <p> By allowing the auto-deployer
         * to manage the deletion of the file the user can avoid this whole scenario and, thereby, avoid accidental attempts to
         * deploy an application that the user wants gone.
         * 
         */
        if (undeployedByRequestFile(file)) {
            cleanupAppAndRequest(file);
        }

        if (autodeploymentStatus.status) {
            markUndeployed(file);
            retryManager.recordSuccessfulUndeployment(file);
        } else {
            markUndeployFailed(file);
            retryManager.recordFailedUndeployment(file);
        }
    }

    private void markUndeployed(File file) {
        try {
            deleteAllMarks(file);
            final File undeployedFile = getUndeployedFile(file);
            if (!undeployedFile.createNewFile()) {
                deplLogger.log(WARNING, CREATE_FAILED, undeployedFile.getAbsolutePath());
            }
        } catch (Exception e) {
            // ignore
        }
    }

    private void markUndeployFailed(File file) {
        try {
            deleteAllMarks(file);
            final File undeployFailedFile = getUndeployFailedFile(file);
            if (!undeployFailedFile.createNewFile()) {
                deplLogger.log(WARNING, CREATE_FAILED, undeployFailedFile.getAbsolutePath());
            }
        } catch (Exception e) {
            // ignore
        }
    }

    private boolean undeployedByRequestFile(File file) {
        return file instanceof AutoDeployedFilesManager.UndeployRequestedFile;
    }

    private void cleanupAppAndRequest(File file) {
        boolean logFine = deplLogger.isLoggable(FINE);

        /*
         * Clean up the application file or directory.
         */
        if (file.isDirectory()) {
            if (logFine) {
                deplLogger.fine("Deleting autodeployed directory " + file.getAbsolutePath() + " by request");
            }
            liquidate(file);
        } else {
            if (logFine) {
                deplLogger.fine("Deleting autodeployed file " + file.getAbsolutePath() + " by request");
            }
            deleteFile(file);
        }

        /*
         * Remove the undeploy request file.
         */
        File requestFile = AutoDeployedFilesManager.appToUndeployRequestFile(file);
        if (logFine) {
            deplLogger.fine("Deleting autodeploy request file " + requestFile.getAbsolutePath());
        }
        
        deleteFile(requestFile);
    }

}
