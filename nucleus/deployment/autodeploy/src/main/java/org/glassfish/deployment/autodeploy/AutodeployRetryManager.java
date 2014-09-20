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

import com.sun.enterprise.config.serverbeans.DasConfig;
import com.sun.enterprise.util.LocalStringManagerImpl;
import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.api.ActionReport;
import org.glassfish.api.Async;
import org.glassfish.deployment.autodeploy.AutoDeployer.AutodeploymentStatus;
import org.glassfish.deployment.common.DeploymentUtils;
import javax.inject.Inject;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PostConstruct;
import javax.inject.Singleton;

import org.glassfish.logging.annotation.LogMessageInfo;

/**
 * Manages retrying of autodeployed files in case a file is copied slowly.
 * <p>
 * If a file is copied into the autodeploy directory slowly, it can appear there
 * before the copy operation has finished, causing the attempt to autodeploy it to fail.
 * This class encapsulates logic to decide whether to retry the deployment of
 * such files on successive loops through
 * the autodeployer thread, reporting failure only if the candidate file has
 * failed to deploy earlier and has remained stable in size for a 
 * (configurable) period of time.  
 * <p>
 * The main public entry point are the {@link �} method and
 * the {@link reportSuccessfulDeployment}, 
 * {@link reportFailedDeployment}, {@link reportSuccessfulUndeployment}, and
 * {@link reportUnsuccessfulUndeployment} methods.  
 * <p>
 * The client should invoke {@link shouldAttemptDeployment} when it has identified
 * a candidate file for deployment but before trying to deploy that file.  This
 * retry manager will return whether the caller should attempt to deploy the file,
 * at least based on whether there has been a previous unsuccessful attempt to
 * deploy it and, if so, whether the file seems to be stable in size or not.  
 * <p>
 * When the caller actually tries to deploy a file, it must invoke 
 * {@link reportSuccessfulDeployment} or {@link reportFailedDeployment) 
 * so that the retry manager keeps its information about the file up-to-date.
 * Similarly, when the caller tries to undeploy a file it must invoke
 * {@link reportSuccessfulUndeployment} or {@link reportFailedUndeployment}.
 * <P>
 * Internally for each file that has failed to deploy the retry manager records
 * the file's size and the timestamp of the most recent failure and the timestamp at 
 * which that file will be assumed to be fully copied.  At that time the file's
 * retry period will expire.  This retry expiration value is extended each time 
 * the file changes size since the last time it was checked.
 * <p>
 * If AutoDeployer previously reported failures to deploy the file and the 
 * file's size has been stable for its retry expiration time, then the 
 * {@link shouldAttemptDeployment} method returns true to trigger another attempt to
 * deploy the file.  If the autodeployer reports another failed deployment
 * then the retry manager concludes that the file is not simply a slow-copying 
 * file but is truly invalid.  In that case 
 * it throws an exception.  
 * <p>
 * Once the caller reports a successful deployment of a file by invoking
 * {@link reportSuccessfulDeployment} the retry manager discards any record of 
 * that file from its internal data structures.  Similarly the retry manager
 * stops monitoring a file once the autodeployer has made an attempt - 
 * successful or unsuccessful - to undeploy it.
 * <p>
 * An important change from v2 to v3 is the change in the default retry limit.
 * In v2 we could try to open a file as a ZIP file to help decide if it had
 * finished copying, but in v3 we cannot make assumptions about how apps
 * will be packaged (some may be ZIPs, but others may be single files).  We
 * need to provide a balance between prompt reporting of a failed auto-deployment
 * vs. handling the case of a slow copy operation which, for a while, manifests
 * itself as a failed deployment.  
 *
 * @author tjquinn
 */
@Service
@Singleton
public class AutodeployRetryManager implements PostConstruct {
        
    /**
     *Specifies the default value for the retry limit.
     */
    private static final int RETRY_LIMIT_DEFAULT = 4; // 4 seconds but default is really set on DasConfig in config-api

    /** Maps an invalid File to its corresponding Info object. */
    private HashMap<File,Info> invalidFiles = new HashMap<File,Info>();

    private static final LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(AutodeployRetryManager.class);

    @Inject
    private DasConfig activeDasConfig;
    
    private int timeout;

    public static final Logger deplLogger =
        org.glassfish.deployment.autodeploy.AutoDeployer.deplLogger;

    @LogMessageInfo(message = "Configured timeout value of {0} second{0,choice,0#seconds|1#second|1<seconds} will be used but seems very large")
    private static final String LARGE_TIMEOUT = "NCLS-DEPLOYMENT-02031";

    @LogMessageInfo(message = "Configured timeout value of {0} second{0,choice,0#seconds|1#second|1<seconds} is too small; using previous value of {1}second {1,choice,0#seconds|1#second|1<seconds}", level="WARNING")
    private static final String SMALL_TIMEOUT = "NCLS-DEPLOYMENT-02032";

    @LogMessageInfo(message = "Could not convert configured timeout value of \"{0}\" to a number; using previous value of {1} second{1,choice,0#seconds|1#second|1<seconds", level="WARNING")
    private static final String INVALID_TIMEOUT = "NCLS-DEPLOYMENT-02033";

    public void postConstruct() {
        setTimeout();
    }
    
    /**
     *Retrieves the Info object describing the specified file.
     *@param File for which the Info object is requested
     *@return Info object for the specified file
     *null if the file is not recorded as invalid
     */
    Info get(File file) {
        Info info = (Info) invalidFiles.get(file);
        return info;
    }

    /**
     *Indicates whether the AutoDeployer should try deploying the specified file.
     *<p>
     *Attempt to deploy the file if this retry manager has no information
     *about the file or if information is present and the file size is
     *unchanged from the previous failure to open.
     *@return if the file should be opened as an archive
     */
    boolean shouldAttemptDeployment(File file) {
        boolean result = true; // default is true in case the file is not being monitored
        String msg = null;
        boolean loggable = deplLogger.isLoggable(Level.FINE);
        Info info = (Info) invalidFiles.get(file);
        if (info != null) {
            result = info.shouldOpen();
            if (loggable) {
                if (result) {
                    msg = localStrings.getLocalString(
                            "enterprise.deployment.autodeploy.try_stable_length", 
                            "file {0} has stable length so it should open as a JAR",
                            file.getAbsolutePath());
                } else {
                    msg = localStrings.getLocalString(
                            "enterprise.deployment.autodeploy.no_try_unstable_length", 
                            "file {0} has an unstable length of {1}; do not retry yet",
                            file.getAbsolutePath(), String.valueOf(file.length()));
                }
            }
            info.update();
        } else {
            if (loggable) {
                msg = localStrings.getLocalString(
                        "enterprise.deployment.autodeploy.try_not_monitored", 
                        "file {0} should be opened as an archive because it is not being monitored as a slowly-growing file",
                        file.getAbsolutePath());
            }
        }
        if (loggable) {
            deplLogger.log(Level.FINE, msg);
        }
        return result;
    }

    AutodeploymentStatus chooseAutodeploymentStatus(ActionReport.ExitCode exitCode, File deployablefile) {
        if (exitCode != ActionReport.ExitCode.FAILURE) {
            return AutodeploymentStatus.forExitCode(exitCode);
        }
        Info info = invalidFiles.get(deployablefile);
        return (info == null) ? AutodeploymentStatus.FAILURE : AutodeploymentStatus.PENDING;
    }

    boolean recordFailedDeployment(File file) throws AutoDeploymentException {
        return recordFailedOpen(file);
    }
    
    boolean recordSuccessfulDeployment(File file) {
        return recordSuccessfulOpen(file);
    }
    
    boolean recordSuccessfulUndeployment(File file) {
        return endMonitoring(file);
    }
    
    boolean recordFailedUndeployment(File file) {
        return endMonitoring(file);
    }
    
    boolean endMonitoring(File file) {
        return (invalidFiles.remove(file) != null);
    }
    
    /**
     *Records the fact that the autodeployer tried but failed to open this file
     *as an archive.
     *@param File the file that could not be interpreted as a legal archive
     *@return true if the file was previously recognized as an invalid one
     *@throws AutoDeploymentException if the file should no longer be retried
     */
    private boolean recordFailedOpen(File file) throws AutoDeploymentException {
        boolean fileAlreadyPresent;
        /*
         *Try to map the file to an existing Info object for it.
         */
        Info info = get(file);
        if ( ! (fileAlreadyPresent = (info != null))) {
            /*
             *This file was not previously noted as invalid.  Create a new
             *entry and add it to the map.
             */
            info = createInfo(file);
            invalidFiles.put(file, info);
            if (deplLogger.isLoggable(Level.FINE)) {
                String msg = localStrings.getLocalString(
                        "enterprise.deployment.autodeploy.begin_monitoring", 
                        "will monitor {0} waiting for its size to be stable size until {1}",
                        file.getAbsolutePath(), new Date(info.retryExpiration).toString());
                deplLogger.log(Level.FINE, msg);
            }
        } else {
            /*
             *The file has previously been recorded as invalid.  Update
             *the recorded info.
             */
            info.update();

            /*
             *If the file is still eligible for later retries, just return.
             *If the file size has been stable too long, then conclude that
             *the file is just an invalid archive and throw an exception
             *to indicate that.
             */
            boolean loggable = deplLogger.isLoggable(Level.FINE);
            if ( ! info.hasRetryPeriodExpired()) {
                /*
                 *Just log that the file is still being monitored.
                 */
                if (loggable) {
                    String msg = localStrings.getLocalString(
                            "enterprise.deployment.autodeploy.continue_monitoring", 
                            "file {0} remains eligible for monitoring until {1}",
                            file.getAbsolutePath(), new Date(info.retryExpiration).toString());
                    deplLogger.log(Level.FINE, msg);
                }
            } else {
                /*
                 *Log that monitoring of this file will end, remove the file from
                 *the map, and throw an exception
                 *with the same message.
                 */
                String msg = localStrings.getLocalString(
                        "enterprise.deployment.autodeploy.abort_monitoring",
                        "File {0} is no longer eligible for retry; its size has been stable for {1} second{1,choice,0#s|1#|1<s} but it is still unrecognized as an archive",
                        file.getAbsolutePath(), 
                        timeout);
                if (loggable) {
                    deplLogger.log(Level.FINE, msg);
                }
                invalidFiles.remove(file);
                throw new AutoDeploymentException(msg);
            }
        }
        return fileAlreadyPresent;
    }

    /**
     *Marks a file as successfully opened and no longer subject to retry.
     *@param File that is no longer invalid
     *@return true if the file had been previously recorded as invalid
     */
    private boolean recordSuccessfulOpen(File file) {
        if (deplLogger.isLoggable(Level.FINE)) {
            String msg = localStrings.getLocalString(
                    "enterprise.deployment.autodeploy.end_monitoring", 
                    "File {0} opened successfully; no need to monitor it further",
                    file.getAbsolutePath());
            deplLogger.log(Level.FINE, msg);
        }
        return (invalidFiles.remove(file)) != null;
    }
    
    private void setTimeout() {
        int newTimeout = timeout;
        String timeoutText = activeDasConfig.getAutodeployRetryTimeout();
        if (timeoutText == null || timeoutText.equals("")) {
            timeout = RETRY_LIMIT_DEFAULT;
            return;
        }
        try {
            int configuredTimeout = Integer.parseInt(timeoutText);
            if (configuredTimeout > 1000) {
                /*
                 * User probably thought the configured value was in milliseconds
                 * instead of seconds.  
                 */
              deplLogger.log(Level.WARNING,
                             LARGE_TIMEOUT,
                             configuredTimeout);
                newTimeout = configuredTimeout;
            } else if (configuredTimeout <= 0) {
              deplLogger.log(Level.WARNING,
                             SMALL_TIMEOUT,
                             new Object[] { configuredTimeout,
                                            timeout });
            } else {
                newTimeout = configuredTimeout;
            }
        } catch (NumberFormatException ex) {
              deplLogger.log(Level.WARNING,
                             INVALID_TIMEOUT,
                             new Object[] { timeoutText,
                                            timeout });
        }
        timeout = newTimeout;
    }
    
    /**
     * Factory method that creates a new Info object for a given file.
     * @param f the file of interest
     * @return the new Info object
     */
    private Info createInfo(File f) {
        if (f.isDirectory()) {
            return new DirectoryInfo(f);
        } else {
            return new JarInfo(f);
        }
    }
        
    /**
     *Records pertinent information about a file judged to be invalid - that is,
     *unrecognizable as a legal archive:
     *<ul>
     *<li>the file object,
     *<li>the length of the file at the most recent check of the file,
     *<li>the time after which no more retries should be attempted.
     *</ul>
     */
    private abstract class Info {
        
        /** File recorded in this Info instance */
        protected File file = null;

        /** Timestamp after which all retries on this file should stop. */
        protected long retryExpiration = 0;

        /**
         *Creates a new instance of the Info object for the specified file.
         *@param File to be recorded
         */
        public Info(File file) {
            this.file = file;
        }

        /**
         * Reports whether the file represented by this Info object should be
         * opened now, even though past opens have failed.
         * @return
         */
        protected abstract boolean shouldOpen();

        /**
         * Updates whatever data is used to monitor changes to the file and
         * returns whether or not changes have been detected.
         * @return whether changes have been detected
         */
        protected abstract boolean update();
        
        /**
         *Reports whether the retry period for this file has expired.
         *<p>
         *@return if the file should remain as a candidate for retry
         */
        private boolean hasRetryPeriodExpired() {
            return (System.currentTimeMillis() > retryExpiration);
        }
        
        /**
         * Delays the time when retries for this file will expire.
         */
        protected void postponeRetryExpiration() {
            retryExpiration = System.currentTimeMillis() + timeout * 1000L;
        }

    }
    
    private class JarInfo extends Info {

        /** File length the previous time this file was reported as
         * invalid. */
        private long recordedLength;

        public JarInfo(File f) {
            super(f);
            recordedLength = f.length();
            update();
        }

        @Override
        protected boolean shouldOpen() {
            return (file.length() == recordedLength);            
        }

        /**
         *Updates the Info object with the file's length and recomputes (if
         *appropriate) the retry due date and the expiration.
         */
        @Override
        protected boolean update() {
            boolean hasChanged;
            long currentLength = file.length();
            if (hasChanged = (recordedLength != currentLength)) {
                /*
                 *The file's size has changed.  Reset the time for this
                 *file's expiration.
                 */
                postponeRetryExpiration();
            }
            /*
             *In all cases, update the recorded length with the file's
             *actual current length.
             */
            recordedLength = currentLength;
            return hasChanged;
        }

    }
    
    private class DirectoryInfo extends Info {
        
        private long whenScanned;
        
        public DirectoryInfo(File f) {
            super(f);
            whenScanned = 0;
            update();
        }

        @Override
        protected boolean shouldOpen() {
            /*
             * For directories we have no way of knowing - without trying to
             * deploy it - whether it represents a valid archive.  So as far
             * as we know from here, directories should always be opened.
             */
            return true;
        }

        @Override
        protected boolean update() {
            /*
             * For a directory, scan all its files for one that's newer than 
             * the last time we checked.
             */
            
            /*
             * Record the start time of the scan rather than the finish time
             * so we don't inadvertently miss files that were changed 
             * during the scan.
             */
            long newWhenScanned = System.currentTimeMillis();
            boolean hasChanged = isNewerFile(file, whenScanned);
            if (hasChanged) {
                postponeRetryExpiration();
            }
            whenScanned = newWhenScanned;
            return hasChanged;
        }
        
        /**
         * Reports whether the specified file is newer (or contains a newer
         * file) than the timestamp.
         * @param f the file to check
         * @param timestamp moment to compare to
         * @return true if the file is newer or contains a newer file than timestamp
         */
        private boolean isNewerFile(File f, long timestamp) {
            boolean aFileIsNewer = (f.lastModified() > timestamp);
            if ( ! aFileIsNewer) {
                if (f.isDirectory()) {
                    for (File containedFile : f.listFiles()) {
                        if (aFileIsNewer = isNewerFile(containedFile, timestamp)) {
                            break;
                        }
                    }
                }
            }
            return aFileIsNewer;
        }
    }
}
