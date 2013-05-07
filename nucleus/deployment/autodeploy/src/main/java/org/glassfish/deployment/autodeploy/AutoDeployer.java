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

/*
 * AutoDeployer.java
 *
 *
 * Created on February 19, 2003, 10:21 AM
 */

package org.glassfish.deployment.autodeploy;

import com.sun.enterprise.util.LocalStringManagerImpl;
import java.io.File;

import java.net.URI;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.logging.Level;
import java.util.concurrent.atomic.AtomicBoolean;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.deployment.common.DeploymentUtils;
import org.glassfish.hk2.api.ServiceLocator;

import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.logging.annotation.LoggerInfo;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;

/**
 * Handles the logic of deploying the module/app to the required destination.</br>
 * The destination is specified on the constructor and can be modified by
 * calling setTarget().  The specific directory scanner can be set using
 * setDirectoryScanner, default is AutoDeployDirectoryScanner
 * @author vikas
 * @author tjquinn
 */
public class AutoDeployer {

    private Boolean verify=null;
    private Boolean forceDeploy=null;
    private Boolean enabled=null;
    private Boolean jspPreCompilation=null;
    private boolean renameOnSuccess = true;
    private File directory = null;
    private String virtualServer = null;

    private String target=null;
    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(AutoDeployer.class);
    private DirectoryScanner directoryScanner=null;
    
    private boolean cancelDeployment =false;
    
    private AtomicBoolean inProgress = new AtomicBoolean(false);
    
    private ServiceLocator habitat;

    private File domainRoot = null;
    
    @LogMessagesResourceBundle
    private static final String SHARED_LOGMESSAGE_RESOURCE = "org.glassfish.deployment.LogMessages";

    // Reserve this range [NCLS-DEPLOYMENT-02001, NCLS-DEPLOYMENT-04000]
    // for message ids used in this deployment autodeploy module
    @LoggerInfo(subsystem = "DEPLOYMENT", description="Deployment logger for autodeploy module", publish=true)
    private static final String DEPLOYMENT_LOGGER = "javax.enterprise.system.tools.deployment.autodeploy";

    public static final Logger deplLogger =
        Logger.getLogger(DEPLOYMENT_LOGGER, SHARED_LOGMESSAGE_RESOURCE);

    @LogMessageInfo(message = "Exception caught:  {0}", cause="An exception was caught when the application was autodeployed.", action="See the exception to determine how to fix the error", level="SEVERE")
    private static final String EXCEPTION_CAUGHT = "NCLS-DEPLOYMENT-02025";

    @LogMessageInfo(message = "Autoundeploying application:  {0}", level="INFO")
    private static final String AUTODEPLOYING_APPLICATION = "NCLS-DEPLOYMENT-02026";

    @LogMessageInfo(message = "Selecting file {0} for autodeployment", level="INFO")
    private static final String SELECTING_FILE = "NCLS-DEPLOYMENT-02027";

    /*
     *Represent the result of attempting autodeployment of a single file.
     *PENDING indicates the file could not be opened as an archive,
     *perhaps because the file was still in the process of being copied when
     *the autodeployer tried to work with it.  PENDING could also mean
     *that the file has changed size since the last time the autodeployer
     *checked it.  It's then reasonable to think it might grow further,
     *so autodeployer waits until the next time through to check it again.
     */
    protected static final int DEPLOY_SUCCESS = 1;
    protected static final int DEPLOY_FAILURE = 2;
    protected static final int DEPLOY_PENDING = 3;
    
    private AutodeployRetryManager retryManager;

    private static final boolean DEFAULT_RENAME_ON_SUCCESS = true;
    private static final boolean DEFAULT_FORCE_DEPLOY = true;
    private static final boolean DEFAULT_INCLUDE_SUBDIR = false;
    private static final boolean DEFAULT_ENABLED = true;
    
    static final String STATUS_SUBDIR_PATH = ".autodeploystatus";
    
    /**
     * Creates a new autodeployer.
     * @param target deployment target for autodeployed applications
     * @param directoryPath directory to be scanned for changes
     * @param virtualServer the virtual server to which to deploy apps to
     * @param habitat hk2 habitat for injection support
     * @throws org.glassfish.deployment.autodeploy.AutoDeploymentException
     */
    public AutoDeployer(
            String target, 
            String directoryPath,
            String virtualServer,
            ServiceLocator habitat) throws AutoDeploymentException {
        this(
            target, 
            directoryPath, 
            virtualServer,
            false /* jspPreCompilation */, 
            false /* verifierEnabled */,
            habitat);
    }
    
   /**
     * Creates a new instance of AutoDeployer
     * @param target the deployment target for autodeployed applications
     * @param directoryPath the directory to scan
     * @param virtualServer the virtual server to deploy to
     * @param jspPrecompilationEnabled whether to precompile JSPs
     * @param verifierEnabled whether to verify applications during deployment
     * @param renameOnSuccess rename the file if deployment is successful
     * @param forceDeploy request that forced deployment occur if the app is already deployed
     * @param enabled whether apps should be enabled upon auto-deployment
    * @param habitat HK2 habitat for use in instantiating properly-init'd DeployCommand and UndeployCommand
    * @throws org.glassfish.deployment.autodeploy.AutoDeploymentException 
     */
    public AutoDeployer(
            String target, 
            String directoryPath, 
            String virtualServer,
            boolean jspPrecompilationEnabled, 
            boolean verifierEnabled,
            boolean renameOnSuccess,
            boolean forceDeploy,
            boolean enabled,
            ServiceLocator habitat) throws AutoDeploymentException {
        
        setHabitat(habitat);
        setTarget(target);
        setDirectory(directoryPath);
        setJspPrecompilationEnabled(jspPrecompilationEnabled);
        setVerifierEnabled(verifierEnabled);
        setRenameOnSuccess(renameOnSuccess);
        setForceDeploy(forceDeploy);
        setVirtualServer(virtualServer);
        setEnabled(enabled);
        setRetryManager(habitat);
    }
    
    public AutoDeployer(
            String target, 
            String directoryPath, 
            String virtualServer,
            boolean jspPrecompilationEnabled, 
            boolean verifierEnabled,
            ServiceLocator habitat) throws AutoDeploymentException {
        this(
            target, 
            directoryPath, 
            virtualServer,
            jspPrecompilationEnabled, 
            verifierEnabled, 
            DEFAULT_RENAME_ON_SUCCESS,
            DEFAULT_FORCE_DEPLOY,
            DEFAULT_ENABLED,
            habitat);
    }

    /**
     * Sets the habitat for use in creating DeployCommand and UndeployCommand
     * instances.
     * @param habitat
     */
    public void setHabitat(ServiceLocator habitat) {
        this.habitat = habitat;
    }
    /**
     * Sets whether or not the precompileJSP option should be requested
     * during autodeployments.
     * @param setting true if JSPs should be precompiled during autodeployments
     */
    public void setJspPrecompilationEnabled(boolean setting) {
        jspPreCompilation = setting;
    }
    
    
    /**
     * Sets the directory to be scanned by the autodeployer.
     * @param directoryPath the directory path to scan
     * @throws org.glassfish.deployment.autodeploy.AutoDeploymentException 
     */
    public void setDirectory(String directoryPath) throws AutoDeploymentException {
        validateAutodeployDirectory(directoryPath);
        this.directory = new File(directoryPath);
    }
    
    /**
     * Sets whether descriptor verification should be requested during
     * autodeployments.
     * @param verify true if verification should occur during autodeployments
     */
    public void setVerifierEnabled(boolean verify) {
        this.verify = verify;
    }
    
    /**
     * Creates the directories for the specified target, except that if the
     * target is/would be a descendant of the base and the base does not exist
     * don't create anything.
     * <p>
     * This helps avoid a race condition in which the user stops the domain
     * (which apparently reports success long before it actually finishes) then
     * deletes the domain.  The delete can run before the server has finished
     * stopping.  In some cases, the autodeployer has run in the meantime and
     * 
     * @param baseDir
     * @param dir
     * @return true if the directory and all intervening ones were created; false otherwise
     */
    boolean mkdirs(final File baseDir, final File targetDir) {
        final URI baseURI = baseDir.toURI().normalize();
        final URI targetURI = targetDir.toURI().normalize();

        /*
         * Go ahead and create all directories if the target falls outside the
         * base OR if the target falls inside the base and the base exists.
         */
        if (baseURI.relativize(targetURI).equals(targetURI) || baseDir.exists()) {
            return targetDir.mkdirs();
        } else {
            /*
             * The target would fall inside the base but the base does not exist.
             */
            return false;
        }
    }

    private void validateAutodeployDirectory(String autodeployDirPath) throws AutoDeploymentException {
        File autodeployDir = new File(autodeployDirPath);
        validateDirectory(autodeployDir);
        File statusDir = new File(autodeployDir, STATUS_SUBDIR_PATH);
        validateDirectory(statusDir);
    }

    private synchronized File domainRoot() {
        if (domainRoot == null) {
            ServerEnvironment serverEnv = habitat.getService(ServerEnvironment.class);
            domainRoot = serverEnv.getDomainRoot();
        }
        return domainRoot;
    }
    
    private void validateDirectory(File dirFile) throws AutoDeploymentException {
        if ( ! dirFile.exists()) {
            mkdirs(domainRoot(), dirFile);
        } else {
            if ( ! dirFile.isDirectory()) {
                throw new AutoDeploymentException(
                        localStrings.getLocalString(
                            "enterprise.deployment.autodeploy.invalid_source_dir", 
                            "invalid source directory {0}",
                            dirFile));
            }
        }
        if ( ! dirFile.canRead()) {
            throw new AutoDeploymentException(
                    localStrings.getLocalString(
                        "enterprise.deployment.autodeploy.dir_not_readable",
                        "directory {0} not readable",
                        dirFile));
        }
        if ( ! dirFile.canWrite()) {
            throw new AutoDeploymentException(
                    localStrings.getLocalString(
                        "enterprise.deployment.autodeploy.dir_not_writeable",
                        "directory {0} not writable",
                        dirFile));
        }
    }
    
    
    private void setRenameOnSuccess(boolean rename) {
        renameOnSuccess = rename;
    }
    
    private void setForceDeploy(boolean force) {
        forceDeploy = force;
    }
    
    private void setVirtualServer(String vs) {
        virtualServer = vs;
    }
    
    private void setEnabled(boolean setting) {
        enabled = setting;
    }
    
    /**
     * set  DirectoryScanner which will be used for filtering out deployeble component
     * @param ds the new directory scanner to use
     */
    public void setDirectoryScanner(DirectoryScanner ds) {
        directoryScanner=ds;
    }
    
    /**
     * set  target server where the autual deployment will be done
     * @param target 
     */
    public void setTarget(String target) {
        this.target = target;
    }
    
    /**
     * If an archive is successfully autodeployed, file will not be
     * renamed to archive_deployed
     */
    public void disableRenameOnSuccess() {
        renameOnSuccess = false;
    }
    
    /**
     * If an archive is successfully autodeployed will be renamed
     * to archive_deployed
     */
    public void enableRenameOnSuccess() {
        // FIXME - Mahesh
        renameOnSuccess = true;
    }
    
    /**
     *Set whether this AutoDeployer should verify or not.
     *@param verify whether to verify the app during deployment
     */
    public void setVerify(boolean verify) {
        this.verify =  Boolean.valueOf(verify);
    }
    
    /**
     *Set whether this AutoDeployer should precompile JSPs or not.
     *@param jspPreCompilation precompilation setting
     */
    public void setJspPreCompilation(boolean jspPreCompilation) {
        this.jspPreCompilation = Boolean.valueOf(jspPreCompilation);
    }
    
    /**
     * Run through the auto-deployment procedure.
     * <p>
     * Clients should invoke this method to execute the auto-deployer once
     * with the current configurable settings.
     */
    public void run() {
        if (directory.exists()) {
            run(DEFAULT_INCLUDE_SUBDIR);
        } else {
            deplLogger.fine("autodeploy directory does not exist");
        }
    }
    
    public synchronized void run(boolean includeSubdir) {
        markInProgress();
        try {
            deployAll(directory, includeSubdir);
            undeployAll(directory, includeSubdir);
        } catch (AutoDeploymentException e) {
            // print and continue
            LogRecord lr = new LogRecord(Level.SEVERE, EXCEPTION_CAUGHT);
            Object args[] = { e.getMessage() };
            lr.setParameters(args);
            lr.setThrown(e);
            deplLogger.log(lr);
        } finally {
            clearInProgress();
        }
    }

    void init() {
        cancelDeployment = false;
    }

    private void setRetryManager(ServiceLocator habitat) {
        retryManager = habitat.getService(AutodeployRetryManager.class);
    }
    
    private void markInProgress() {
        inProgress.set(true);
    }

    private void clearInProgress() {
        inProgress.set(false);
        notifyAll();
    }
    
    public synchronized void waitUntilIdle() throws InterruptedException {
        while ( ! inProgress.get()) {
            wait();
        }
    }
    
    /**
     * do deployment for all the deployable components in autoDeployDir dir.
     * @return
     */
    private void deployAll(File autoDeployDir, boolean includeSubDir) throws AutoDeploymentException {
        
        
        //create with default scanner
        if(directoryScanner==null) {
            directoryScanner=new AutoDeployDirectoryScanner();
        }
        
        File [] files= null;
        
        //get me all deployable entities
        files= directoryScanner.getAllDeployableModules(autoDeployDir, includeSubDir);
        
        /*
         *To support slowly-copied files, the deploy method returns
         *    DEPLOY_SUCCESS  if the file was successfully autodeployed
         *    DEPLOY_FAILURE  if the file failed to be autodeployed
         *    DEPLOY_PENDING  if the file needs to be tried again later
         *
         *The marker files should be updated only if the result is success or
         *failure.  So for each file make a separate decision
         *about whether to record the result or not based on the result of
         *the deploy method.  Note that the boolean is initialized to true
         *so that if an exception is thrown, the file's marker files will be
         *updated.
         */
        if(files != null && files.length > 0) {
            deplLogger.fine("Deployable files: " + Arrays.toString(files));
            for (int i=0; ((i < files.length) && !cancelDeployment);i++) {
                boolean okToRecordResult = true;
                try {
                    okToRecordResult = (deploy(files[i], autoDeployDir) != AutodeploymentStatus.PENDING);
                } catch (AutoDeploymentException ae) {
                    //ignore and move to next file
                } finally {
                    if(renameOnSuccess && okToRecordResult) {
                        deplLogger.fine("Reporting deployed entity " + files[i].getAbsolutePath());
                        directoryScanner.deployedEntity(autoDeployDir, files[i]);
                    }
                }
            }
        } 
    }
    
    
    
    /**
     * do undeployment for all deleted applications in autoDeployDir dir.
     * @param autoDeployDir the directory to scan for deleted files
     * @throws org.glassfish.deployment.autodeploy.AutoDeploymentException 
     */
    public void undeployAll(File autoDeployDir, boolean includeSubdir) throws AutoDeploymentException {
        
        
        //create with default scanner
        if(directoryScanner==null) {
            directoryScanner=new AutoDeployDirectoryScanner();
        }
        
        File[] apps= null;
        
        //get me all apps
        apps= directoryScanner.getAllFilesForUndeployment(autoDeployDir, includeSubdir);
        
        //deploying all applications
        if(apps !=null) {
            for (int i=0; i< apps.length && !cancelDeployment;i++) {
                try {
                    
                    this.undeploy(apps[i], autoDeployDir, 
                        getNameFromFilePath(autoDeployDir, apps[i]));
                    
                    
                } catch (AutoDeploymentException ae) {
                    //ignore and move to next file
                } finally {
                    // Mark the application as undeployed both in the case of success & failure.
                    directoryScanner.undeployedEntity(autoDeployDir, apps[i]);
                }
            }
        }
        /////////end for apps
    }
    
    private AutodeploymentStatus undeploy(File applicationFile, File autodeployDir,
    String name) throws AutoDeploymentException {
        
        AutoUndeploymentOperation au = AutoUndeploymentOperation.newInstance(
                habitat,
                applicationFile, 
                name, 
                target);
        deplLogger.log(Level.INFO,
                       AUTODEPLOYING_APPLICATION,
                       name);
        return au.run();
        
    }

    /**
     * set cancel flag, which  will ensure that only if there is any current deployment is in process,</br>
     * it will be completed but the deployer will not do any more deployment.
     * @param value the cancel setting
     */
    public void cancel(boolean value){
        cancelDeployment=value;
    }
    
    /**
     * get cancel flag value
     * @return
     */
    public boolean isCancelled(){
        return cancelDeployment;
    }
    
    /**
     *Deploy any type of module.
     *@param deployablefile the file to be deployed
     *@param autodeployDir the directory where the file resides (holdover from earlier impl)
     *@return status of the deployment attempt: DEPLOY_SUCCESS, DEPLOY_FAILURE, or DEPLOY_PENDING 
     *@throws AutoDeploymentException if any invoked method throws an exception
     */
    protected AutodeploymentStatus deploy(File deployablefile, File autodeployDir) throws AutoDeploymentException {
        
        String file=deployablefile.getAbsolutePath();
        if ( ! retryManager.shouldAttemptDeployment(deployablefile)) {
            return AutodeploymentStatus.PENDING;
        }
        deplLogger.log(Level.INFO,
                       SELECTING_FILE,
                       file);


        AutoDeploymentOperation ad = AutoDeploymentOperation.newInstance(
                habitat,
                renameOnSuccess,
                deployablefile, 
                enabled,
                virtualServer,
                forceDeploy,
                verify,
                jspPreCompilation,
                target);
        AutodeploymentStatus adStatus = ad.run();
        return adStatus;
    }
   
    static String getNameFromFilePath(File autodeployDir, File filePath) {   //creating module name as file name
        
        File parent = filePath.getParentFile();
        String moduleName = null;
        while (!parent.getAbsolutePath().equals(autodeployDir.getAbsolutePath())) {
            if (moduleName==null) {
                moduleName = parent.getName();
            } else {
                moduleName = parent.getName()+"_"+moduleName;
            }
            parent = parent.getParentFile();
        }
        if (moduleName==null) {
            moduleName = filePath.getName();
        } else {
            moduleName = moduleName + "_" + filePath.getName();
        }
        int toIndex = moduleName.lastIndexOf('.');
        if (toIndex > 0) {
            moduleName = moduleName.substring(0, toIndex);
        }
        return moduleName;
    }
    
    public enum AutodeploymentStatus {
        SUCCESS(
                true, 
                ActionReport.ExitCode.SUCCESS,
                "enterprise.deployment.autodeploy.successfully_autodeployed",
                "deployment of {0} succeeded",
                "enterprise.deployment.autodeploy.successfully_autoundeployed",
                "undeployment of {0} succeeded"),
        FAILURE(
                false, 
                ActionReport.ExitCode.FAILURE,
                "enterprise.deployment.autodeploy.autodeploy_failed",
                "deployment of {0} failed",
                "enterprise.deployment.autodeploy.autoundeploy_failed",
                "undeployment of {0} failed"),
        WARNING(
                true, 
                ActionReport.ExitCode.WARNING,
                "enterprise.deployment.autodeploy.warning_autodeployed",
                "deployment of {0} succeeded with warning(s)",
                "enterprise.deployment.autodeploy.warning_autoundeployed",
                "undeployment of {0} succeeded with warning(s)"),
        PENDING(
                true,
                ActionReport.ExitCode.SUCCESS,
                "",
                "",
                "",
                "");
        ;

        final boolean status;
        final ActionReport.ExitCode exitCode;
        final String deploymentMessageKey;
        final String undeploymentMessageKey;
        final String deploymentDefaultMessage;
        final String undeploymentDefaultMessage;

        AutodeploymentStatus(
                boolean status, 
                ActionReport.ExitCode exitCode,
                String deploymentMessageKey,
                String deploymentDefaultMessage,
                String undeploymentMessageKey,
                String undeploymentDefaultMessage) {
            this.status = status;
            this.exitCode = exitCode;
            this.deploymentMessageKey = deploymentMessageKey;
            this.deploymentDefaultMessage = deploymentDefaultMessage;
            this.undeploymentMessageKey = undeploymentMessageKey;
            this.undeploymentDefaultMessage = undeploymentDefaultMessage;
        }

        public static AutodeploymentStatus forExitCode(ActionReport.ExitCode exitCode) {
            for (AutodeploymentStatus ds : AutodeploymentStatus.values()) {
                if (ds.exitCode == exitCode) {
                    return ds;
                }
            }
            throw new IllegalArgumentException(exitCode.toString());
        }
    }        

}

