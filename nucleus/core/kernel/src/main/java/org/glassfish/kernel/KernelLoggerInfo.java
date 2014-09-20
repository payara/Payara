/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.kernel;

import java.util.logging.Logger;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;
import org.glassfish.logging.annotation.LoggerInfo;

/**
 * Logger information for the internal-api module.
 * @author Tom Mueller
 */
/* Module private */
public class KernelLoggerInfo {
    private static final String LOGMSG_PREFIX = "NCLS-CORE";
    
    @LogMessagesResourceBundle
    private static final String SHARED_LOGMESSAGE_RESOURCE = "org.glassfish.kernel.LogMessages";
    
    @LoggerInfo(subsystem = "CORE", description = "Core Kernel", publish = true)
    private static final String CORE_LOGGER = "javax.enterprise.system.core";
    private static final Logger coreLogger = Logger.getLogger(
                CORE_LOGGER, SHARED_LOGMESSAGE_RESOURCE);

    public static Logger getLogger() {
        return coreLogger;
    }

    @LogMessageInfo(
            message = "Cannot decode parameter {0} = {1}",
            level = "WARNING")
    public static final String cantDecodeParameter = LOGMSG_PREFIX + "-00001";

    @LogMessageInfo(
            message = "Cannot instantiate model for command {0}",
            cause = "The service that implements the command could not be loaded.",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String cantInstantiateCommand = LOGMSG_PREFIX + "-00002";
    
    @LogMessageInfo(
            message = "Exception while running a command",
            cause = "An unexpected exception occurred while running a command.",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String invocationException = LOGMSG_PREFIX + "-00003";

    @LogMessageInfo(
            message = "Unable to get an instance of ClusterExecutor; Cannot dynamically reconfigure instances",
            level = "WARNING")
    public static final String cantGetClusterExecutor  = LOGMSG_PREFIX + "-00004";

    @LogMessageInfo(
            message = "Can't delete local password file: {0}",
            level = "WARNING")
    public static final String cantDeletePasswordFile = LOGMSG_PREFIX + "-00005";  
    
    @LogMessageInfo(
            message = "Can't create local password file: {0}",
            level = "WARNING")
    public static final String cantCreatePasswordFile = LOGMSG_PREFIX + "-00006";
    
    @LogMessageInfo(
            message = "Timeout occurred when processing Admin Console request.",
            cause = "A request for a lock timed out while processing an admin console request.",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String consoleRequestTimeout = LOGMSG_PREFIX + "-00007";
    
    @LogMessageInfo(
            message = "Cannot process admin console request.",
            cause = "InterruptedException occurred while the service thread is running.",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String consoleCannotProcess = LOGMSG_PREFIX + "-00008";
    
    @LogMessageInfo(
            message = "Unable to serve resource: {0}. Cause: {1}",
            cause = "An I/O error occurred while serving a resource request.",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String consoleResourceError = LOGMSG_PREFIX + "-00009";
    
    @LogMessageInfo(
            message = "Resource not found: {0}",
            level = "WARNING")
    public static final String consoleResourceNotFound = LOGMSG_PREFIX + "-00010";
    
    @LogMessageInfo(
            message = "Console cannot be initialized due to an exception.",
            level = "INFO")
    public static final String consoleCannotInitialize = LOGMSG_PREFIX + "-00011";
    
    @LogMessageInfo(
            message = "Cannot write property '{0} = {1}' for AdminService in domain.xml, exception: {2}",
            level = "INFO")
    public static final String consoleCannotWriteProperty = LOGMSG_PREFIX + "-00012";
    
    @LogMessageInfo(
            message = "Shutdown procedure finished",
            level = "INFO")
    public static final String shutdownFinished = LOGMSG_PREFIX + "-00013";
    
    @LogMessageInfo(
            message = "Shutdown required",
            cause = "An unexpected exception occurred while changing run levels.  A shutdown is required.",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String shutdownRequired = LOGMSG_PREFIX + "-00014";
    
    @LogMessageInfo(
            message = "Shutdown requested",
            level = "INFO")
    public static final String shutdownRequested = LOGMSG_PREFIX + "-00015";
    
    @LogMessageInfo(
            message = "Startup service failed to start",
            cause = "An unexpected exception occurred while starting the startup service.  A shutdown is required.",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String startupFailure = LOGMSG_PREFIX + "-00016";
    
    @LogMessageInfo(
            message = "{0} ({1}) startup time : {2} ({3}ms), startup services({4}ms), total({5}ms)",
            level = "INFO")
    public static final String startupEndMessage = LOGMSG_PREFIX + "-00017";
    
    @LogMessageInfo(
            message = "TOTAL TIME INCLUDING CLI: {0}",
            level = "INFO")
    public static final String startupTotalTime = LOGMSG_PREFIX + "-00018";
    
    @LogMessageInfo(
            message = "Shutting down server due to startup exception",
            cause = "An unexpected exception occurred while starting the server.  A shutdown is required.",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String startupFatalException = LOGMSG_PREFIX + "-00019";
    
    @LogMessageInfo(
            message = "Timed out, ignoring some startup service status",
            level = "WARNING")
    public static final String startupWaitTimeout = LOGMSG_PREFIX + "-00020";
    
    @LogMessageInfo(
            message = "Unexpected exception during startup",
            cause = "An unexpected exception occurred while starting the server.",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String startupException = LOGMSG_PREFIX + "-00021";
    
    @LogMessageInfo(
            message = "Loading application {0} done in {1} ms",
            level = "INFO")
    public static final String loadingApplicationTime = LOGMSG_PREFIX + "-00022";
    
    @LogMessageInfo(
            message = "Enable of application {0} completed with a warning: {1}",
            level = "INFO")
    public static final String loadingApplicationWarning = LOGMSG_PREFIX + "-00023";
    
    @LogMessageInfo(
            message = "Error during enabling",
            cause = "An unexpected exception occurred while enabling an application.",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String loadingApplicationErrorEnable = LOGMSG_PREFIX + "-00024";
    
    @LogMessageInfo(
            message = "Error during disabling",
            cause = "An unexpected exception occurred while disabling an application.",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String loadingApplicationErrorDisable = LOGMSG_PREFIX + "-00025";
    
    @LogMessageInfo(
            message = "Exception during lifecycle processing",
            cause = "An unexpected exception occurred during lifecycle processing.",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String lifecycleException = LOGMSG_PREFIX + "-00026";    

    @LogMessageInfo(
            message = "ApplicationMetaDataProvider {0} requires {1} but no other ApplicationMetaDataProvider provides it",
            level = "WARNING")
    public static final String applicationMetaDataProvider = LOGMSG_PREFIX + "-00027";
    
    @LogMessageInfo(
            message = "Inconsistent state - nothing is providing {0} yet it passed validation",
            cause = "An unexpected condition during lifecycle processing.",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String inconsistentLifecycleState = LOGMSG_PREFIX + "-00028";    

    @LogMessageInfo(
            message = "Cannot start container {0}, exception: {1}",
            cause = "An unexpected condition during lifecycle processing.",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String cantStartContainer = LOGMSG_PREFIX + "-00029";    

    @LogMessageInfo(
            message = "Cannot release container {0}, exception {1}",
            level = "INFO")
    public static final String cantReleaseContainer = LOGMSG_PREFIX + "-00030";
    
    @LogMessageInfo(
            message = "Error while closing deployable artifact {0}, exception: {1}",
            cause = "An unexpected exception occurred during lifecycle processing.",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String errorClosingArtifact = LOGMSG_PREFIX + "-00031";    

    @LogMessageInfo(
            message = "Error while expanding archive file",
            cause = "An unexpected exception occurred during lifecycle processing.",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String errorExpandingFile = LOGMSG_PREFIX + "-00032";    

    @LogMessageInfo(
            message = "Cannot find sniffer for module type: {0}",
            cause = "An unexpected condition occurred during lifecycle processing.",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String cantFindSniffer = LOGMSG_PREFIX + "-00033";    

    @LogMessageInfo(
            message = "Cannot find any sniffer for deployed app: {0}",
            cause = "An unexpected condition occurred during lifecycle processing.",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String cantFindSnifferForApp = LOGMSG_PREFIX + "-00034";    

    @LogMessageInfo(
            message = "Exception occurred while satisfying optional package dependencies",
            level = "INFO")
    public static final String exceptionOptionalDepend = LOGMSG_PREFIX + "-00035";
    
    @LogMessageInfo(
            message = "Cannot delete created temporary file {0}",
            level = "WARNING")
    public static final String cantDeleteTempFile = LOGMSG_PREFIX + "-00036";
    
    @LogMessageInfo(
            message = "Source is not a directory, using temporary location {0} ",
            level = "WARNING")
    public static final String sourceNotDirectory = LOGMSG_PREFIX + "-00037";
    
    @LogMessageInfo(
            message = "Cannot find the application type for the artifact at: {0}. Was the container or sniffer removed?",
            cause = "An unexpected condition occurred while loading an application.",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String cantFindApplicationInfo = LOGMSG_PREFIX + "-00038";    

    @LogMessageInfo(
            message = "Exception during application deployment",
            cause = "An unexpected exception occurred while deploying an application.",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String deployException = LOGMSG_PREFIX + "-00039";

    @LogMessageInfo(
            message = "Cannot determine original location for application: {0}",
            cause = "A URL syntax error occurred.",
            action = "Check the application for proper syntax.",
            level = "SEVERE")
    public static final String cantDetermineLocation = LOGMSG_PREFIX + "-00040";    

   @LogMessageInfo(
            message = "Application deployment failed: {0}",
            cause = "The deployment command for an application failed as indicated in the message.",
            action = "Check the application and redeploy.",
            level = "SEVERE")
    public static final String deployFail = LOGMSG_PREFIX + "-00041";    

    @LogMessageInfo(
            message = "IOException while opening deployed artifact",
            cause = "An unexpected exception occurred while deploying an application.",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String exceptionOpenArtifact = LOGMSG_PREFIX + "-00042";    

    @LogMessageInfo(
            message = "Application previously deployed is not at its original location any more: {0}",
            cause = "An unexpected exception occurred while loading an application.",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String notFoundInOriginalLocation = LOGMSG_PREFIX + "-00043";    

    @LogMessageInfo(
            message = "System property called {0} is null, is this intended?",
            level = "WARNING")
    public static final String systemPropertyNull = LOGMSG_PREFIX + "-00044";
    
    @LogMessageInfo(
            message = "Invalid classpath entry for common class loader ignored: {0}, exception: {1}",
            level = "WARNING")
    public static final String invalidClassPathEntry = LOGMSG_PREFIX + "-00045";
    
    @LogMessageInfo(
            message = "Cannot find javadb client jar file, derby jdbc driver will not be available by default.",
            level = "INFO")
    public static final String cantFindDerby = LOGMSG_PREFIX + "-00046";
    
    @LogMessageInfo(
            message = "CommonClassLoaderServiceImpl is unable to process {0} because of an exception: {1}",
            level = "INFO")
    public static final String exceptionProcessingJAR = LOGMSG_PREFIX + "-00047";
    
    @LogMessageInfo(
            message = "Invalid InputStream returned for {0}",
            cause = "Unable to retrieve an entry from the archive.",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String invalidInputStream = LOGMSG_PREFIX + "-00048";    

    @LogMessageInfo(
            message = "Exception while processing {0} inside {1} of size {2}, exception: {3}",
            cause = "An unexpected exception occurred while processing an archive.",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String exceptionWhileParsing = LOGMSG_PREFIX + "-00049";    

    @LogMessageInfo(
            message = "Cannot open sub-archive {0} from {1}",
            cause = "An unexpected exception occurred while processing an archive.",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String cantOpenSubArchive = LOGMSG_PREFIX + "-00050";    

    @LogMessageInfo(
            message = "Cannot close sub archive {0}, exception: {1}",
            cause = "An unexpected exception occurred while closing an archive.",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String exceptionWhileClosing = LOGMSG_PREFIX + "-00051";    

    @LogMessageInfo(
            message = "Exception loading lifecycle module [{0}]; [{1}]",
            cause = "An unexpected exception occurred while loading a lifecycle module.",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String exceptionLoadingLifecycleModule = LOGMSG_PREFIX + "-00052";    

    @LogMessageInfo(
            message = "Lifecycle module [{0}] threw ServerLifecycleException, exception: {1}",
            level = "WARNING")
    public static final String serverLifecycleException = LOGMSG_PREFIX + "-00053";
    
    @LogMessageInfo(
            message = "Lifecycle module [{0}] threw an Exception; please check your lifecycle module. Exception: {1}",
            level = "WARNING")
    public static final String lifecycleModuleException = LOGMSG_PREFIX + "-00054";
    
    @LogMessageInfo(
            message = "GrizzlyService stop-proxy problem",
            level = "WARNING")
    public static final String grizzlyStopProxy = LOGMSG_PREFIX + "-00055";
    
    @LogMessageInfo(
            message = "Unable to start the server. Closing all ports",
            cause = "An unexpected exception occurred while starting the grizzly service.",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String grizzlyCantStart = LOGMSG_PREFIX + "-00056";    

    @LogMessageInfo(
            message = "Exception closing port: {0}, exception: {1}",
            cause = "An unexpected exception occurred while closing a port.",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String grizzlyCloseException = LOGMSG_PREFIX + "-00057";    

    @LogMessageInfo(
            message = "Network listener {0} on port {1} disabled per domain.xml",
            level = "INFO")
    public static final String grizzlyPortDisabled = LOGMSG_PREFIX + "-00058";
    
    @LogMessageInfo(
            message = "GrizzlyService endpoint registration problem",
            level = "WARNING")
    public static final String grizzlyEndpointRegistration = LOGMSG_PREFIX + "-00059";
    
    @LogMessageInfo(
            message = "Skip registering endpoint with non existent virtual server: {0}",
            level = "WARNING")
    public static final String grizzlyNonExistentVS = LOGMSG_PREFIX + "-00060";
    
    @LogMessageInfo(
            message = "Attempting to start the {0} container.",
            level = "INFO")
    public static final String snifferAdapterStartingContainer = LOGMSG_PREFIX + "-00061";
   
    @LogMessageInfo(
            message = "Done with starting {0} container in {1} ms.",
            level = "INFO")
    public static final String snifferAdapterContainerStarted = LOGMSG_PREFIX + "-00062";
   
    @LogMessageInfo(
            message = "Could not start container, no exception provided.",
            cause = "The container could not be started.",
            action = "Ensure the libraries for the container are available.",
            level = "SEVERE")
    public static final String snifferAdapterNoContainer = LOGMSG_PREFIX + "-00063";    

    @LogMessageInfo(
            message = "Exception while starting container {0}, exception: {1}",
            cause = "An exception occurred while attempting to start the container.",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String snifferAdapterExceptionStarting = LOGMSG_PREFIX + "-00064";    

    @LogMessageInfo(
            message = "Exception while mapping the request.",
            cause = "An exception occurred while mapping a request to the container.",
            action = "Please resolve issues mentioned in the stack trace.",
            level = "SEVERE")
    public static final String snifferAdapterExceptionMapping = LOGMSG_PREFIX + "-00065";    

    @LogMessageInfo(
            message = "Cannot add new configuration to the Config element",
            cause = "An exception occurred while adding the container configuration to the domain.xml.",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String exceptionAddContainer = LOGMSG_PREFIX + "-00066";    

    @LogMessageInfo(
            message = "Exception while enabling or disabling the autodeployment of applications",
            cause = "An exception occurred while enabling or disabling the autodeployment of applications.",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String exceptionAutodeployment = LOGMSG_PREFIX + "-00067";    

    @LogMessageInfo(
            message = "Exception while sending an event.",
            cause = "An exception occurred while sending an event.",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String exceptionSendEvent = LOGMSG_PREFIX + "-00068";    

    @LogMessageInfo(
            message = "Exception while dispatching an event",
            level = "WARNING")
    public static final String exceptionDispatchEvent = LOGMSG_PREFIX + "-00069";
    
    @LogMessageInfo(
            message = "An exception occurred while stopping the server, continuing.",
            cause = "An exception occurred while stopping the server.",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String exceptionDuringShutdown = LOGMSG_PREFIX + "-00070";    

    @LogMessageInfo(
            message = "The ManagedJobConfig bean {0} was changed by {1}",
            level = "FINE")
    public static final String changeManagedJobConfig = LOGMSG_PREFIX + "-00071";

    @LogMessageInfo(
                message = "Cleaning Job {0}",
                level = "FINE")
    public static final String cleaningJob = LOGMSG_PREFIX + "-00072";

    @LogMessageInfo(
            message = "Initializing Job Cleanup service",
            level = "FINE")
    public static final String initializingJobCleanup = LOGMSG_PREFIX + "-00073";

    @LogMessageInfo(
                message = "Initializing Managed Config bean",
                level = "FINE")
    public static final String initializingManagedConfigBean = LOGMSG_PREFIX + "-00074";

    @LogMessageInfo(
            message = "Scheduling Cleanup",
            level = "FINE")
    public static final String schedulingCleanup = LOGMSG_PREFIX + "-00075";

    @LogMessageInfo(
            message = "Exception when cleaning jobs caused",
            cause="An exception occured when cleaning the managed jobs",
            action="Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String exceptionCleaningJobs = LOGMSG_PREFIX + "-00076";

    @LogMessageInfo(
            message = "-passwordfile specified, but the actual file was not, ignoring ...",
            cause="A software error is causing an incorrect argument sequence.",
            action="No action necessary.",
            level = "WARNING")
    public static final String optionButNoArg = LOGMSG_PREFIX + "-00077";

    @LogMessageInfo(
            message = "Invalid context root for the admin console application, using default: {0}",
            level = "INFO")
    public static final String invalidContextRoot = LOGMSG_PREFIX + "-00078";

    @LogMessageInfo(
            message = "Admin Console Adapter: context root: {0}",
            level = "INFO")
    public static final String contextRoot = LOGMSG_PREFIX + "-00079";
    
    @LogMessageInfo(
            message = "Failed to configure the ManagedJobConfig bean",
            cause="While running the configure-managed-jobs command, a write transaction to the ManagedJobConfig bean failed.",
            action="Check the system logs and contact Oracle support.",
            level = "WARNING")
    public static final String configFailManagedJobConfig = LOGMSG_PREFIX + "-00080";

    @LogMessageInfo(
            message = "Unable to get the ManagedJobConfig bean.",
            cause="While running the configure-managed-jobs command, access to the ManagedJobConfig bean failed.",
            action="Check the system logs and contact Oracle support.",
            level = "WARNING")
    public static final String getFailManagedJobConfig = LOGMSG_PREFIX + "-00081";

    @LogMessageInfo(
            message = "Exiting after upgrade",
            level = "INFO")
    public static final String exitUpgrade = LOGMSG_PREFIX + "-00082";
    
    @LogMessageInfo(
            message = "Exception while attempting to shutdown after upgrade",
            cause="An exception occured when shutting down the server after an upgrade.",
            action="Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String exceptionUpgrade = LOGMSG_PREFIX + "-00083";

    @LogMessageInfo(
            message = "Cannot find port information from domain.xml",
            cause="No port value is available in the NetworkListener config bean",
            action="Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String noPort = LOGMSG_PREFIX + "-00084";

    @LogMessageInfo(
            message = "Cannot parse port value: {0}, using port 8080",
            cause="There is an invalid port value in the domain.xml file.",
            action="Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String badPort = LOGMSG_PREFIX + "-00085";

    @LogMessageInfo(
            message = "Unknown address {0}",
            cause="There is an invalid address value in the domain.xml file.",
            action="Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String badAddress = LOGMSG_PREFIX + "-00086";

    @LogMessageInfo(
            message = "Grizzly Framework {0} started in: {1}ms - bound to [{2}]",
            level = "INFO")
    public static final String grizzlyStarted = LOGMSG_PREFIX + "-00087";
    
    @LogMessageInfo(
            message = "Exception during postConstruct of DynamicReloadService",
            cause="An unexpected exception occured.",
            action="Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String exceptionDRS = LOGMSG_PREFIX + "-00088";

    @LogMessageInfo(
            message = "Cannot determine host name, will use localhost exclusively",
            cause="An unexpected exception occured.",
            action="Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String exceptionHostname = LOGMSG_PREFIX + "-00089";
  
    @LogMessageInfo(
            message = "Internal Server error: {0}",
            cause="An unexpected exception occured.",
            action="Check the system logs and contact Oracle support.",
            level = "WARNING")
    public static final String exceptionMapper = LOGMSG_PREFIX + "-00090";
  
    @LogMessageInfo(
            message = "Unable to set customized error page",
            cause="An unexpected exception occured.",
            action="Check the system logs and contact Oracle support.",
            level = "WARNING")
    public static final String exceptionMapper2 = LOGMSG_PREFIX + "-00091";

    @LogMessageInfo(
            message = "Server shutdown initiated",
            level = "INFO")
    public static final String serverShutdownInit = LOGMSG_PREFIX + "-00092";
    
    @LogMessageInfo(
            message = "Problem while attempting to install admin console!",
            level = "INFO")
    public static final String adminGuiInstallProblem = LOGMSG_PREFIX + "-00093";
    
    @LogMessageInfo(
            message = "Unable to load checkpoint",
            cause="An unexpected exception occured.",
            action="Check the system logs and contact Oracle support.",
            level = "WARNING")
    public static final String exceptionLoadCheckpoint = LOGMSG_PREFIX + "-00094";

    @LogMessageInfo(
            message = "Resuming command {0} from its last checkpoint.",
            level = "INFO")
    public static final String checkpointAutoResumeStart = LOGMSG_PREFIX + "-00095";
    
    @LogMessageInfo(
            message = "Automatically resumed command {0} finished with exit code {1}. \nMessage: {2}",
            level = "INFO")
    public static final String checkpointAutoResumeDone = LOGMSG_PREFIX + "-00096";

}