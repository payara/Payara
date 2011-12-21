/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

//----------------------------------------------------------------------------
//
// Module:      Configuration.java
//
// Description: JTS configuration management.
//
// Product:     com.sun.jts.CosTransactions
//
// Author: Simon Holdsworth 
//
// Date:        March, 1997 
//
// Copyright (c): 1995-1997 IBM Corp.
//
//   The source code for this program is not published or otherwise divested 
//   of its trade secrets, irrespective of what has been deposited with the 
//   U.S. Copyright Office. 
//
//   This software contains confidential and proprietary information of 
//   IBM Corp.
//----------------------------------------------------------------------------

package com.sun.jts.CosTransactions;

// Import required classes.

import java.io.*;
import java.util.*;

import org.omg.CosTransactions.*;
import org.omg.PortableServer.*;

import com.sun.jts.trace.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.sun.logging.LogDomains;
import com.sun.jts.utils.LogFormatter;

import com.sun.enterprise.transaction.api.TransactionConstants;

/**Provides interaction with the execution environment.
 *
 * @version 0.01
 *
 * @author Simon Holdsworth, IBM Corporation
 *
 * @see
*/
// CHANGE HISTORY
//
// Version By     Change Description
//   0.01  SAJH   Initial implementation.
//------------------------------------------------------------------------------

public class Configuration extends Object {
    private static String serverName = null;
    private static byte[] serverNameByteArray = null;
    private static org.omg.CORBA.ORB orb = null;
    private static Properties prop = null;
    private static TransactionFactory factory = null;
    private static boolean localFactory = false;
    private static boolean recoverable = false;
    private static ProxyChecker checker = null;
    private static LogFile logFile = null;
    private static Hashtable poas = new Hashtable();
    private static String dbLogResource = null;
    private static boolean disableFileLogging = false;

    // for delegated recovery support
    private static Hashtable logPathToServernametable = new Hashtable();
    private static Hashtable logPathToFiletable = new Hashtable();

    private static int retries = -1;
    public static final String COMMIT_ONE_PHASE_DURING_RECOVERY = "commit-one-phase-during-recovery";
    public static final int LAO_PREPARE_OK = TransactionConstants.LAO_PREPARE_OK;
    public final static long COMMIT_RETRY_WAIT = 60000;
    private static boolean isAppClient = true;

/**
   The traceOn would enable/disable JTS wide tracing;
   (Related class: com.sun.jts.trace.TraceUtil)
   - kannan.srinivasan@Sun.COM 27Nov2001
*/
	
	/*
		Logger to log transaction messages
	*/
	static Logger _logger = LogDomains.getLogger(Configuration.class, LogDomains.TRANSACTION_LOGGER);

   private static boolean traceOn = false;

   /**The property key used to specify the directory to which trace files and the
     * error log should be written.
     * <p>
     * The value is <em><b>com.sun.jts.traceDirectory</b></em>.
     * <p>
     * The default value used for this property is the current directory.
     */
    public final static String TRACE_DIRECTORY = "com.sun.jts.traceDirectory"/*#Frozen*/;

    /**The property key used to specify the directory to which transaction log files
     * should be written.
     * <p>
     * The value is <em><b>com.sun.jts.logDirectory</b></em>.
     * <p>
     * The default value used for this property is the "jts" subdirectory from the
     * current directory, if that exists, otherwise the current directory.
     */
    public final static String LOG_DIRECTORY = "com.sun.jts.logDirectory"/*#Frozen*/;

    /**The property key used to specify the resource which will be used to wirte
     * transaction logs.
     * <p>
     * The value is <em><b>com.sun.jts.logResource</b></em>.
     * <p>
     */
    public final static String DB_LOG_RESOURCE = "com.sun.jts.logResource"/*#Frozen*/;

    /**
     * Whether to write warnings and errors to jts.log file
     * if this property has any value, it is active, otherwise it is inactive
     *
     */
    public final static String ERR_LOGGING = "com.sun.jts.errorLogging"/*#Frozen*/;

    /**
     * This property indicates that XA Resources would be passed in via
     * the TM.recover() method, and that the recovery thread would have
     * to wait until the resources are passed in. If not set, the recovery
     * thread would not wait for the XA Resources to be passed in.
     */
    public final static String MANUAL_RECOVERY = "com.sun.jts.ManualRecovery"/*#Frozen*/;

    /**The property key used to specify the number of times the JTS should retry
     * a commit or resync operation before giving up.
     * <p>
     * The value is <em><b>com.sun.jts.commitRetry</b></em>.
     * <p>
     * If this property has no value, retries continue indefinitely.  A value of
     * zero indicates that no retries should be made.
     */
    public final static String COMMIT_RETRY = "com.sun.jts.commitRetry"/*#Frozen*/;

    /**The property key used to specify whether the JTS should assume a transaction
     * is to be committed or rolled back if an outcome cannot be obtained during
     * recovery.  It should also be used by Resource objects if they cannot obtain
     * an outcome during recovery and cannot make a decision.
     * <p>
     * The value is <em><b>com.sun.jts.heuristicDirection</b></em>.
     * <p>
     * The default is to assume that the transaction should be rolled back.  If the
     * value is '1', the transaction should be committed.
     */
    public final static String HEURISTIC_DIRECTION = "com.sun.jts.heuristicDirection"/*#Frozen*/;

    /**The property key used to specify the number of transactions between keypoint
     * operations on the log.  Keypoint operations reduce the size of the transaction
     * log files.  A larger value for this property (for example, 1000) will result
     * in larger transaction log files, but less keypoint operations, and hence better
     * performance.  a smaller value (e.g. 20) results in smaller log files but
     * slightly reduced performance due to the greater frequency of keypoint
     * operations.
     * <p>
     * The value is <em><b>com.sun.jts.keypointCount</b></em>.
     * <p>
     * The default value for this property is 100.  If the value is specified as
     * zero, then no keypoints are taken.
     */
    public final static String KEYPOINT_COUNT = "com.sun.jts.keypointCount"/*#Frozen*/;

    // Property to specify the instance name
    public final static String INSTANCE_NAME = "com.sun.jts.instancename"/*#Frozen*/;

    /**The property is used to specify the time interval in seconds for which the timeout
     * manager would scan for timedout transactions. A higher value would mean better
     * performance, but at the cost of closeness to which coordinator timeout is effected.
     * <p>
     * The value is <em><b>com.sun.jts.timeoutInterval"</b></em>
     * <p>
     * This needs to be a positive integer value greater than 10. If the value is less than
     * 10, illegal or unspecified a default value of 10 seconds is assumed.
     */
    public final static String TIMEOUT_INTERVAL = "com.sun.jts.timeoutInterval" ;

    /**The default subdirectory in which log and repository files are stored.
     */
    public final static String JTS_SUBDIRECTORY = "jts"/*#Frozen*/;

    /**getDirectory return value which indicates that the required directory was
     * specified and is OK.
     */
    public final static int DIRECTORY_OK    = 0;

    /**getDirectory return value which indicates that the required directory was
     * either not specified or was specified and invalid, and that the default
     * subdirectory exists.  In this case the default subdirectory should be used.
     */
    public final static int DEFAULT_USED    = 1;

    /**getDirectory return value which indicates that the required directory was
     * either not specified or was specified and invalid, and that the default
     * subdirectory does not exist.  In this case the current directory should be
     * used.
     */
    public final static int DEFAULT_INVALID = 2;

    /**The approximate concurrent transactions expected. This is used to set the capacity of  Vectors etc.
    */
    public final static int EXPECTED_CONCURRENT_TRANSACTIONS = 10000;

    /**The approximate concurrent transactions expected. This is used to set the capacity of  Vectors etc.
    */
    public final static int EXPECTED_CONCURRENT_THREADS = 100;

    /**Returns a valid directory for a particular purpose.  If the required
     * directory is not valid, then a default subdirectory of the current directory
     * is tried.  If that is not valid either, then the current directory is used.
     *
     * @param envDir               The environment variable containing the directory.
     * @param defaultSubdirectory  The default subdirectory to use.
     * @param result               A single-element array which will hold a value
     *                             indicating whether the requested directory,
     *                             default subdirectory, or current directory
     *                             had to be used.
     *
     * @return  The directory name.
     *
     * @see
     */
    public static String getDirectory( String envDir,
                                       String defaultSubdirectory,
                                       int[/*1*/] result ) {

        // Get the environment variable value.

        String envValue = null;
        if( prop != null )
            envValue = prop.getProperty(envDir);

        // If the environment variable is not set, or does not refer to a valid
        // directory, then try to use a default.

        result[0] = DIRECTORY_OK;

        if( envValue == null || envValue.length() == 0 ||
            (new File(envValue).exists() && !new File(envValue).isDirectory()) ) {
            result[0] = DEFAULT_USED;

            // If the default subdirectory is not valid, then use the current directory.

            envValue = "."+File.separator+defaultSubdirectory/*#Frozen*/;
            if( new File(envValue).exists() && !new File(envValue).isDirectory() ) {
                result[0] = DEFAULT_INVALID;
            }
        }
	if(_logger.isLoggable(Level.FINE))
	{
		String dirType="";
		switch(result[0]){
		case DEFAULT_INVALID:
			dirType="used default, but is invalid";
			break;
		case DEFAULT_USED :
			dirType="used default";
			break;
		case DIRECTORY_OK:
			dirType="provided in configuration";
			break;
		default:
			dirType="invalid type";
			break;
		}
		_logger.logp(Level.FINE,"Configuration","getDirectory()",
				"Using directory = " + envValue + " : "+dirType);
	}

        return envValue;
    }

    /**Sets the name of the server.
     *
     * @param name  The server name.  Non-recoverable servers have null.
     *
     * @return
     *
     * @see
     */
    public static final void setServerName( String name, boolean recoverableServer ) {

        // Store the server name.

        serverName = name;
	      serverNameByteArray = (name == null) ? null : serverName.getBytes();
        recoverable = recoverableServer;
        if(recoverable) {
            RecoveryManager.createRecoveryFile(serverName);
        }

        if(_logger.isLoggable(Level.FINE)) {
	    _logger.logp(Level.FINE,"Configuration" ,"setServerName()",
		    " serverName = " + serverName + "; isRecoverable = " + recoverable);
        }
    }

    /**Returns the name of the server.
     * <p>
     * Non-recoverable servers may not have a name, in which case the method returns
     * null.
     *
     * @param
     *
     * @return  The server name.
     *
     * @see
     */
    public static final String getServerName() {

        // Determine the server name.

        String result = serverName;

        return result;
    }

    /**Sets the name of the server for the given log path. Added for delegated
     * recovery support.
     *
     * @param logPath  Location, where the logs are stored.
     *
     * @param name  The server name.
     *
     * @return
     *
     * @see
     */
    public static final void setServerName(String logPath, String name) {
        logPathToServernametable.put(logPath, name);
    }

    /**Returns the name of the server for the given log path. Added for delegated
     *recovery support.
     *
     * @param logPath location of the log files.
     *
     * @return  The server name.
     *
     * @see
     */
    public static final String getServerName(String logPath) {
        return (String)logPathToServernametable.get(logPath);
    }

    /**Returns a byte array with the name of the server.
     * <p>
     * Non-recoverable servers may not have a name, in which case the method returns
     * null.
     *
     * @param
     *
     * @return  The server name (byte array).
     *
     * @see
     */
    public static final byte [] getServerNameByteArray() {

        // Determine the server name.

        byte [] result = serverNameByteArray;

        return result;
    }

    /**Sets the Properties object to be used for this JTS instance.
     *
     * @param prop  The Properties.
     *
     * @return
     *
     * @see
     */
    public static final void setProperties( Properties newProp ) {

        // Store the Properties object.
        if (prop == null)
            prop = newProp;
        else if (newProp != null)
            prop.putAll(newProp);

	if(_logger.isLoggable(Level.FINE)) {
	      String propertiesList = LogFormatter.convertPropsToString(prop);			   
         _logger.logp(Level.FINE,"Configuration","setProperties()",
		 		" Properties set are :"+ propertiesList);
        }
        if (prop != null) {
            dbLogResource = prop.getProperty(DB_LOG_RESOURCE);
            String retryLimit = prop.getProperty(COMMIT_RETRY);
            int retriesInMinutes;
            if (retryLimit != null) {
               retriesInMinutes = Integer.parseInt(retryLimit,10);
               if ((retriesInMinutes % (COMMIT_RETRY_WAIT / 1000)) == 0)
                   retries = (int)(retriesInMinutes / (COMMIT_RETRY_WAIT / 1000));
               else
                   retries = ((int)((retriesInMinutes / (COMMIT_RETRY_WAIT / 1000)))) + 1;
            }
        }

    }

    /**Returns the value of the given variable.
     *
     * @param envValue  The environment variable required.
     *
     * @return  The value.
     *
     * @see
     */
    public static final String getPropertyValue( String envValue ) {

        // Get the environment variable value.

        String result = null;
        if( prop != null )
		{
            result = prop.getProperty(envValue);
		    if(_logger.isLoggable(Level.FINE))
            {
				_logger.log(Level.FINE,"Property :"+ envValue +
						" has the value : " + result);

            }
		}

        return result;
    }


    /**Sets the identity of the ORB.
     *
     * @param newORB  The ORB.
     *
     * @return
     *
     * @see
     */
    public static final void setORB( org.omg.CORBA.ORB newORB ) {

        // Store the ORB identity.

        orb = newORB;

    }


    /**Returns the identity of the ORB.
     *
     * @param
     *
     * @return  The ORB.
     *
     * @see
     */
    public static final org.omg.CORBA.ORB getORB() {


        // Return the ORB identity.


        return orb;
    }


    /**Sets the identity of the TransactionFactory and indicates if it is local
     * or remote.
     *
     * @param newFactory  The TransactionFactory.
     *
     * @param localFactory Indicates if the factory is local or remote.
     *
     * @return
     *
     * @see
     */
    public static final void setFactory( TransactionFactory newFactory,
                                              boolean localTxFactory ) {

        // Store the factory identity and if it is local or not.

        factory = newFactory;
        localFactory = localTxFactory;
    }


    /**Returns the identity of the TransactionFactory.
     *
     * @param
     *
     * @return  The TransactionFactory.
     *
     * @see
     */
    public static final TransactionFactory getFactory() {

        // Return the TransactionFactory identity.

        return factory;
    }

    /**Determines whether we hava a local factory or a remote factory.
     *
     * @param
     *
     * @return  Indicates whether we have a local factory.
     *
     * @see
     */
    public static final boolean isLocalFactory() {

        // This is a local factory if localFactory is TRUE

        boolean result = localFactory;

        return result;
    }

     /**Determines whether the JTS instance is recoverable.
     *
     * @param
     *
     * @return  Indicates whether the JTS is recoverable.
     *
     * @see
     */
    public static final boolean isRecoverable() {

        // This JTS is recoverable if recoverable is set to TRUE.

        boolean result = recoverable;

        return result;
    }
    /**Sets the identity of the ProxyChecker.
     *
     * @param newChecker  The new ProxyChecker.
     *
     * @return
     *
     * @see
     */
    public static final void setProxyChecker( ProxyChecker newChecker ) {


        // Store the checker identity.

        checker = newChecker;

    }

    /**Returns the identity of the ProxyChecker.
     *
     * @param
     *
     * @return  The ProxyChecker.
     *
     * @see
     */
    public static final ProxyChecker getProxyChecker() {
        return checker;
    }


    /**Sets the identity of the log file for the process.
     *
     * @param logFile  The new LogFile object.
     *
     * @return
     *
     * @see
     */
    public static final void setLogFile( LogFile newLogFile ) {


        // Store the logFile identity.

        logFile = newLogFile;

    }


    /**Returns the identity of the LogFile for the process.
     *
     * @param
     *
     * @return  The LogFile.
     *
     * @see
     */
    public static final LogFile getLogFile() {
        return logFile;
    }

    /**Sets the log file for the given log path. For delegated recovery support.
     *
     * @param logPath  The new LogFile object.
     *
     * @param newLogFile  The new LogFile object.
     *
     * @return
     *
     * @see
     */
    public static final void setLogFile(String logPath, LogFile newLogFile) {
        logPathToFiletable.put(logPath, newLogFile);
    }
    /**Returns the LogFile for the given log path. For delegated recovery support.
     *
     * @param logPath log location.
     *
     * @return  The LogFile.
     *
     * @see
     */
    public static final LogFile getLogFile(String logPath) {
        if (logPath == null) return null;
        return (LogFile)logPathToFiletable.get(logPath);
    }


    /**Sets the identity of the POA to be used for the given types of object.
     *
     * @param type  The type of objects to use the POA.
     * @param POA   The POA object.
     *
     * @return
     *
     * @see
     */
    public static final void setPOA( String type, POA poa ) {
        // Store the mapping.

        poas.put(type,poa);

    }


    /**Returns the identity of the POA to be used for the given type of objects.
     *
     * @param type  The type of objects
     *
     * @return  The POA.
     *
     * @see
     */
    public static final POA getPOA( String type ) {

        POA result = (POA)poas.get(type);

        return result;
    }

    public static final boolean isTraceEnabled() {

        return traceOn;

    }

    public static final void enableTrace() {

	traceOn = true;

    }

    public static final void disableTrace() {

	traceOn = false;

    }

    // START IASRI 4662745
    public static void setKeypointTrigger(int keypoint)
    {
        CoordinatorLogPool.getCoordinatorLog().setKeypointTrigger(keypoint);
    }
    public static void setCommitRetryVar(String commitRetryString)
    {
        // RegisteredResources.setCommitRetryVar(commitRetryString);
        if (commitRetryString != null) {
            int retriesInMinutes = Integer.parseInt(commitRetryString,10);
            if ((retriesInMinutes % (COMMIT_RETRY_WAIT / 1000)) == 0)
                   retries = (int)(retriesInMinutes / (COMMIT_RETRY_WAIT / 1000));
            else
                retries = ((int)(retriesInMinutes / (COMMIT_RETRY_WAIT / 1000))) + 1;

        }
    }
    // END IASRI 4662745
    public static int getRetries() {
       return retries;
    }

    public static void setAsAppClientConatiner(boolean value) {
        isAppClient = value;
    }

    public static boolean isAppClientContainer() {
        return isAppClient;
    }

   public static boolean isDBLoggingEnabled() {
       return (dbLogResource != null);
   }
   
   public static void disableFileLogging() {
       disableFileLogging = true;
   }
 
   public static boolean isFileLoggingDisabled() {
       return disableFileLogging;
   }

}
