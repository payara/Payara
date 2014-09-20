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
package com.sun.enterprise.util;

import java.text.MessageFormat;
import java.util.logging.Logger;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;
import org.glassfish.logging.annotation.LoggerInfo;

/**
 * Logger information for the common-util module.
 * @author Tom Mueller
 */
/* Module private */
public class CULoggerInfo {
    private static final String LOGMSG_PREFIX = "NCLS-COMUTIL";

    @LogMessagesResourceBundle
    private static final String SHARED_LOGMESSAGE_RESOURCE = "com.sun.enterprise.util.LogMessages";

    @LoggerInfo(subsystem = "COMMON", description = "Common Utilities", publish = true)
    private static final String UTIL_LOGGER = "javax.enterprise.system.util";
    private static final Logger utilLogger = Logger.getLogger(
                UTIL_LOGGER, SHARED_LOGMESSAGE_RESOURCE);

    public static Logger getLogger() {
        return utilLogger;
    }

    public static String getString(String key) {
        return utilLogger.getResourceBundle().getString(key);
    }

    public static String getString(String key, Object... args) {
        return MessageFormat.format(getString(key), args);
    }

    @LogMessageInfo(
            message = "Failed to process class {0} with bytecode preprocessor {1}",
            cause = "Unknown",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String preprocessFailed = LOGMSG_PREFIX + "-00001";

    @LogMessageInfo(
            message = "Class {0} is being reset to its original state",
            cause = "Unknown",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String resettingOriginal = LOGMSG_PREFIX + "-00002";

    @LogMessageInfo(
            message = "Class {0} is being reset to the last successful preprocessor",
            cause = "Unknown",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String resettingLastGood = LOGMSG_PREFIX + "-00003";

    @LogMessageInfo(
            message = "The supplied preprocessor class {0} is not an instance of org.glassfish.api.BytecodePreprocessor",
            cause = "Unknown",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String invalidType = LOGMSG_PREFIX + "-00004";

    @LogMessageInfo(
            message = "Bytecode preprocessor disabled",
            cause = "Unknown",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String disabled = LOGMSG_PREFIX + "-00005";

    @LogMessageInfo(
            message = "Initialization failed for bytecode preprocessor {0}",
            cause = "Unknown",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String failedInit = LOGMSG_PREFIX + "-00006";

    @LogMessageInfo(
            message = "Error setting up preprocessor",
            cause = "Unknown",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String setupEx = LOGMSG_PREFIX + "-00007";

    @LogMessageInfo(
            message = "Illegal max-entries [{0}]; please check your cache configuration.")
    public static final String illegalMaxEntries = LOGMSG_PREFIX + "-00008";

    @LogMessageInfo(
            message = "Illegal MaxSize value [{0}]")
    public static final String boundedMultiLruCacheIllegalMaxSize = LOGMSG_PREFIX + "-00009";

    @LogMessageInfo(
            message = "Error closing zip file for class path entry {0}",
            level = "INFO")
    public static final String exceptionClosingURLEntry = LOGMSG_PREFIX + "-00010";

    @LogMessageInfo(
            message = "An error occurred while adding URL [{0}] to the EJB class loader. Please check the content of this URL.",
            cause = "An unexpected exception occurred while processing a URL.",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String badUrlEntry = LOGMSG_PREFIX + "-00011";

    @LogMessageInfo(
            message = "The URL entry is missing while contructing the classpath.",
            level = "INFO")
    public static final String missingURLEntry = LOGMSG_PREFIX + "-00012";

    @LogMessageInfo(
            message = "Error closing zip file for duplicate class path entry {0}",
            level = "INFO")
    public static final String exceptionClosingDupUrlEntry = LOGMSG_PREFIX + "-00013";

    @LogMessageInfo(
            message = "Exception in ASURLClassLoader",
            level = "INFO")
    public static final String exceptionInASURLClassLoader = LOGMSG_PREFIX + "-00014";

    @LogMessageInfo(
            message = "ASURLClassLoader {1} was requested to find resource {0} after done was invoked from the following stack trace",
            level = "WARNING")
    public static final String findResourceAfterDone = LOGMSG_PREFIX + "-00015";

    @LogMessageInfo(
            message = "Error: Request made to load class or resource [{0}] on an ASURLClassLoader instance that has already been shutdown. [{1}]",
            level = "WARNING")
    public static final String doneAlreadyCalled = LOGMSG_PREFIX + "-00016";

    @LogMessageInfo(
            message = "{0} actually got transformed",
            level = "INFO")
    public static final String actuallyTransformed = LOGMSG_PREFIX + "-00017";

    @LogMessageInfo(
            message = "ASURLClassLoader {1} was requested to find class {0} after done was invoked from the following stack trace",
            level = "WARNING")
    public static final String findClassAfterDone = LOGMSG_PREFIX + "-00018";

    @LogMessageInfo(
            message = "Illegal call to close() detected",
            level = "WARNING")
    public static final String illegalCloseCall = LOGMSG_PREFIX + "-00019";

    @LogMessageInfo(
            message = "Error processing file with path {0} in {1}",
            cause = "An unexpected exception occurred while processing a file.",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String exceptionProcessingFile = LOGMSG_PREFIX + "-00020";

    @LogMessageInfo(
            message = "Error checking for existing of {0} in {1}",
            cause = "An unexpected exception occurred while checking for the existence of a file.",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String exceptionCheckingFile = LOGMSG_PREFIX + "-00021";

    @LogMessageInfo(
            message = "Error closing an open stream during loader clean-up",
            level = "WARNING")
    public static final String exceptionClosingStream = LOGMSG_PREFIX + "-00022";

    @LogMessageInfo(
            message = "Input stream has been finalized or forced closed without being explicitly closed; stream instantiation reported in following stack trace",
            level = "WARNING")
    public static final String inputStreamFinalized = LOGMSG_PREFIX + "-00023";

    @LogMessageInfo(
            message = "Unable to create client data directory: {0}",
            cause = "An unexpected failure occurred while creating the directory for the file.",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String errorCreatingDirectory = LOGMSG_PREFIX + "-00024";

    @LogMessageInfo(
            message = "Exception in invokeApplicationMain [{0}].",
            cause = "An unexpected exception occurred.",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String exceptionInUtility = LOGMSG_PREFIX + "-00025";

    @LogMessageInfo(
            message = "The main method signature is invalid.",
            cause = "While invoking a main class, an invalid method was found.",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String mainNotValid = LOGMSG_PREFIX + "-00026";

    @LogMessageInfo(
            message = "Error while caching the local string manager - package name may be null.",
            cause = "An unexpected exception occurred.",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String exceptionCachingStringManager = LOGMSG_PREFIX + "-00027";

    @LogMessageInfo(
            message = "Error while constructing the local string manager object.",
            cause = "An unexpected exception occurred.",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String exceptionConstructingStringManager = LOGMSG_PREFIX + "-00028";

    @LogMessageInfo(
            message = "Error in local string manager - resource bundle is probably missing.",
            cause = "An unexpected exception occurred.",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String exceptionResourceBundle = LOGMSG_PREFIX + "-00029";

    @LogMessageInfo(
            message = "Error while formating the local string.",
            level = "WARNING")
    public static final String exceptionWhileFormating = LOGMSG_PREFIX + "-00030";

    @LogMessageInfo(
            message = "Some IOException occurred",
            cause = "An unexpected exception occurred.",
            action = "Check the system logs and contact Oracle support.",
            level = "SEVERE")
    public static final String exceptionIO = LOGMSG_PREFIX + "-00031";

    @LogMessageInfo(
            message = "Attempt to delete {0} failed; the file is reported as non-existent",
            level = "FINE")
    public static final String deleteFailedAbsent = LOGMSG_PREFIX + "-00032";

    @LogMessageInfo(
            message = "Error attempting to delete {0}",
            level = "FINE")
    public static final String deleteFailed = LOGMSG_PREFIX + "-00033";

    @LogMessageInfo(
            message = "Performing gc to try to force file closures",
            level = "FINE")
    public static final String performGC = LOGMSG_PREFIX + "-00034";

    @LogMessageInfo(
            message = "Attempt to rename {0} to {1} succeeded after {2} retries",
            level = "FINE")
    public static final String retryRenameSuccess = LOGMSG_PREFIX + "-00035";

    @LogMessageInfo(
            message = "Attempt to rename {0} to {1} succeeded without any retries",
            level = "FINE")
    public static final String renameInitialSuccess = LOGMSG_PREFIX + "-00036";

    @LogMessageInfo(
            message = "Attempt to rename {0} to {1} failed after {2} retries",
            level = "WARNING")
    public static final String retryRenameFailure = LOGMSG_PREFIX + "-00037";

    @LogMessageInfo(
            message = "Failed to open jar file: {0}",
            level = "WARNING")
    public static final String exceptionJarOpen = LOGMSG_PREFIX + "-00038";

    @LogMessageInfo(
            message = "Attempt to use non-existent auth token {0}",
            level = "WARNING")
    public static final String useNonexistentToken = LOGMSG_PREFIX + "-00039";

    @LogMessageInfo(
            message = "File Lock not released on {0}",
            level = "WARNING")
    public static final String fileLockNotReleased = LOGMSG_PREFIX + "-00040";

    @LogMessageInfo(
            message = "Bad Network Configuration.  DNS can not resolve the "
            + "hostname: \n{0}",
            cause = "The hostname can't be resolved.",
            action = "Set the hostname correctly.",
            level = "WARNING")
    public static final String badNetworkConfig = LOGMSG_PREFIX + "-00041";

    @LogMessageInfo(message = "BundleTracker.removedBundle null bundleID for {0}",
                    level="WARNING")
    public static final String NULL_BUNDLE = LOGMSG_PREFIX + "-00042";


/*


# ClassLoaderUtil messages
classloaderutil.errorReleasingLoader=CMNUTL9001: Error releasing URLClassLoader
classloaderutil.errorClosingJar=CMNUTL9002: Error closing JAR file {0}
classloaderutil.errorGettingField=CMNUTL9003: Error getting information for field {0}
classloaderutil.errorReleasingJarNoName=CMNUTL9005: Error releasing JAR file

## messages used in EJBUtils.java (starts from LDR5100)
appclient.classpath=LDR5102: Could not get classpath for appclient module

enterprise_util.connector_malformed_url=UTIL6003:MalformedURLException in addResourceAdapter().
enterprise_util.excep_in_createorb=UTIL6009:Unexpected Exception in createORB.
enterprise_util.excep_orbmgr_numfmt=UTIL6031:Number Format Exception, Using default value(s).
enterprise_util.excep_in_reading_fragment_size=UTIL6035: Exception converting to integer

*/
}
