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
// Portions Copyright [2019] Payara Foundation and/or affiliates
package org.glassfish.deployment.autodeploy;

import static org.glassfish.deployment.common.DeploymentProperties.LOG_REPORTED_ERRORS;

import java.io.File;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;

import org.glassfish.api.admin.AdminCommand;
import org.glassfish.deployment.autodeploy.AutoDeployer.AutodeploymentStatus;
import org.glassfish.deployment.common.DeploymentProperties;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.jvnet.hk2.annotations.Service;

/**
 * Performs a single auto-deployment operation for a single file.
 * <p>
 * Note - Use the newInstance static method to obtain a fully-injected operation; it is safer and more convenient than
 * using the no-arg constructor and then invoking init yourself.
 * 
 * @author tjquinn
 */
@Service
@PerLookup
public class AutoDeploymentOperation extends AutoOperation {

    public static final Logger deplLogger = AutoDeployer.deplLogger;

    @LogMessageInfo(message = "Attempt to create file {0} failed; no further information.", level = "WARNING")
    private static final String CREATE_FILE_FAILED = "NCLS-DEPLOYMENT-02034";
    
    /**
     * Creates a fully-injected, ready-to-use AutoDeploymentOperation object.
     * 
     * @param serviceLocator
     * @param file
     * @param virtualServer
     * @param target
     * 
     * @return the injected, initialized AutoDeploymentOperation
     */
    public static AutoDeploymentOperation newInstance(ServiceLocator serviceLocator, File file, String virtualServer, String target, String contextRoot) {

        AutoDeploymentOperation autoDeploymentOperation = (AutoDeploymentOperation) serviceLocator.getService(AutoDeploymentOperation.class);
        autoDeploymentOperation.init(true, file, true, virtualServer, true, true, false, target, contextRoot);
        
        return autoDeploymentOperation;
    }

    /**
     * Creates a fully-injected, ready-to-use AutoDeploymentOperation object.
     * 
     * @param serviceLocator
     * @param writeMarkOnDeployed
     * @param file
     * @param enabled
     * @param virtualServer
     * @param forceDeploy
     * @param verify
     * @param preJspCompilation
     * @param target
     * @return the injected, initialized AutoDeploymentOperation
     */
    static AutoDeploymentOperation newInstance(ServiceLocator serviceLocator, boolean writeMarkOnDeployed, File file, boolean enabled, String virtualServer,
            boolean forceDeploy, boolean verify, boolean preJspCompilation, String target) {

        AutoDeploymentOperation autoDeploymentOperation = (AutoDeploymentOperation) serviceLocator.getService(AutoDeploymentOperation.class);
        autoDeploymentOperation.init(writeMarkOnDeployed, file, enabled, virtualServer, forceDeploy, verify, preJspCompilation, target, null);
        
        return autoDeploymentOperation;
    }

    private boolean writeMarkOnDeployed;

    private static final String COMMAND_NAME = "deploy";

    @Inject
    @Named(COMMAND_NAME)
    private AdminCommand deployCommand;

    @Inject
    private AutodeployRetryManager retryManager;

    /**
     * Completes the initialization of the object.
     * 
     * @param writeMarkOnDeployed
     * @param file
     * @param enabled
     * @param virtualServer
     * @param forceDeploy
     * @param verify
     * @param preJspCompilation
     * @param target
     * @return the object itself for convenience
     */
    protected AutoDeploymentOperation init(boolean writeMarkOnDeployed, File file, boolean enabled, String virtualServer, boolean forceDeploy, boolean verify, boolean preJspCompilation, String target, String contextRoot) {
        super.init(file, getDeployActionProperties(file, enabled, virtualServer, forceDeploy, verify, preJspCompilation, target, contextRoot), COMMAND_NAME, deployCommand);
        this.writeMarkOnDeployed = writeMarkOnDeployed;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    protected String getMessageString(AutodeploymentStatus ds, File file) {
        return localStrings.getLocalString(ds.deploymentMessageKey, ds.deploymentDefaultMessage, file);
    }

    /**
     * {@inheritDoc}
     */
    protected void markFiles(AutodeploymentStatus ds, File file) {
        /*
         * One reason an auto-deployment may fail is if the auto-deployed file is a directory and the directory's contents were
         * not yet complete when the autodeployer detected a change in the top-level directory file's timestamp. Retry a failed
         * autodeploy of a directory for the prescribed retry period or until it succeeds.
         *
         * Similarly, a JAR file - or any legitimate application file - might be copied over a period of time, so an initial
         * attempt to deploy it might fail because the file is incomplete but a later attempt might work.
         */
        if (ds != AutodeploymentStatus.SUCCESS && ds != AutodeploymentStatus.WARNING) {
            try {
                retryManager.recordFailedDeployment(file);
            } catch (AutoDeploymentException ex) {
                /*
                 * The retry manager has concluded that this most recent failure should be the last one.
                 */
                markDeployFailed(file);
                retryManager.endMonitoring(file);
            }
        } else {
            retryManager.recordSuccessfulDeployment(file);
            if (ds.status) {
                if (writeMarkOnDeployed) {
                    markDeployed(file);
                }
            } else {
                markDeployFailed(file);
            }
        }
    }

    // Methods for creating operation status file(s)
    private void markDeployed(File f) {
        try {
            deleteAllMarks(f);
            final File deployedFile = getDeployedFile(f);
            if (!deployedFile.createNewFile()) {
                deplLogger.log(Level.WARNING, CREATE_FILE_FAILED, deployedFile.getAbsolutePath());
            }
        } catch (Exception e) {
            // ignore
        }
    }

    private void markDeployFailed(File f) {
        try {
            deleteAllMarks(f);
            final File deployFailedFile = getDeployFailedFile(f);
            if (!deployFailedFile.createNewFile()) {
                deplLogger.log(Level.WARNING, CREATE_FILE_FAILED, deployFailedFile.getAbsolutePath());
            }
        } catch (Exception e) {
            // ignore
        }
    }

    private static Properties getDeployActionProperties(File deployablefile, boolean enabled, String virtualServer, boolean forceDeploy, boolean verify, boolean jspPreCompilation, String target, String contextRoot) {

        DeploymentProperties deploymentProperties = new DeploymentProperties();
        deploymentProperties.setPath(deployablefile.getAbsolutePath());
        deploymentProperties.setVirtualServers(virtualServer);
        deploymentProperties.setTarget(target);
        deploymentProperties.setContextRoot(contextRoot);
        
        deploymentProperties.setEnabled(enabled);
        deploymentProperties.setForce(forceDeploy);
        deploymentProperties.setVerify(verify);
        deploymentProperties.setPrecompileJSP(jspPreCompilation);
        deploymentProperties.setProperty(LOG_REPORTED_ERRORS, "false");
        
        return deploymentProperties;
    }
}
