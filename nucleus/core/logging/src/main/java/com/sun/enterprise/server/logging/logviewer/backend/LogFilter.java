/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.server.logging.logviewer.backend;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.management.Attribute;
import javax.management.AttributeList;

import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.logging.LogLevel;
import org.glassfish.config.support.TranslatedConfigView;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;

import com.sun.common.util.logging.LoggingConfigImpl;
import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.server.logging.LogFacade;
import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.util.SystemPropertyConstants;

/**
 * <p/>
 * LogFilter will be used by Admin Too Log Viewer Front End to filter the
 * records. LogMBean delegates the getLogRecordsUsingQuery method to this
 * class static method.
 *
 * @AUTHOR: Hemanth Puttaswamy and Ken Paulsen
 * @AUTHOR: Carla Mott, Naman Mehta
 */
@Service
public class LogFilter {

    // This is the name of the Results Attribute that we send out to the
    // Admin front end.
    private static final String RESULTS_ATTRIBUTE = "Results";

    // Load the custom level class for query purpose.
    private static final Level[] GF_CUSTOM_LEVELS = new Level[] {
        LogLevel.ALERT,
        LogLevel.EMERGENCY
    };

    private static final String NV_SEPARATOR = ";";

    @Inject
    Domain domain;

    @Inject
    CommandRunner commandRunner;

    @Inject
    ServerEnvironment env;

    @Inject
    private ServiceLocator habitat;

    @Inject
    LoggingConfigImpl loggingConfig;

    private static final Logger LOGGER = LogFacade.LOGGING_LOGGER;


    private static final boolean DEBUG = false;

    /**
     * The public method that Log Viewer Front End will be calling on.
     * The query will be run on records starting from the fromRecord.
     * If any of the elements for the query is null, then that element will
     * not be used for the query.  If the user is interested in viewing only
     * records whose Log Level is SEVERE and WARNING, then the query would
     * look like:
     * <p/>
     * fromDate = null, toDate = null, logLevel = WARNING, onlyLevel = false,
     * listOfModules = null, nameValueMap = null.
     *
     * @param logFileName    The LogFile to use to run the query. If null
     *                       the current server.log will be used. This is
     *                       not the absolute file name, just the fileName
     *                       needs to be passed. We will use the parent
     *                       directory of the previous server.log to
     *                       build the absolute file name.
     * @param fromRecord     The location within the LogFile
     * @param next           True to get the next set of results, false to
     *                       get the previous set
     * @param forward        True to search forward through the log file
     * @param requestedCount The # of desired return values
     * @param fromDate       The lower bound date
     * @param toDate         The upper bound date
     * @param logLevel       The minimum log level to display
     * @param onlyLevel      True to only display messsage for "logLevel"
     * @param listOfModules  List of modules to match
     * @param nameValueMap   NVP's to match
     * @return
     */
    public AttributeList getLogRecordsUsingQuery(
            String logFileName, Long fromRecord, Boolean next, Boolean forward,
            Integer requestedCount, Date fromDate, Date toDate,
            String logLevel, Boolean onlyLevel, List listOfModules,
            Properties nameValueMap, String anySearch) {

        //      Testing code for instance setup
        //return getLogRecordsUsingQuery(logFileName, fromRecord, next, forward, requestedCount,
        //        fromDate, toDate, logLevel, onlyLevel, listOfModules, nameValueMap, anySearch, "in2", false);

        LogFile logFile = null;
        String logFileDetailsForServer = "";

        try {
            logFileDetailsForServer = loggingConfig.getLoggingFileDetails();
            logFileDetailsForServer = TranslatedConfigView.getTranslatedValue(logFileDetailsForServer).toString();
            logFileDetailsForServer = new File(logFileDetailsForServer).getAbsolutePath();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, LogFacade.ERROR_EXECUTING_LOG_QUERY, ex);
            return new AttributeList();
        }


        if ((logFileName != null)
                && (logFileName.length() != 0)) {
            logFileName = logFileDetailsForServer.substring(0, logFileDetailsForServer.lastIndexOf(File.separator)) + File.separator +
                    logFileName.trim();
            if (new File(logFileName).exists()) {
                logFile = getLogFile(logFileName);
            } else {
                logFileName = logFileDetailsForServer;
                logFile = getLogFile(logFileName);
            }
        } else {
            logFileName = logFileDetailsForServer;
            logFile = getLogFile(logFileName);
        }
        boolean forwd = (forward == null) ? true : forward.booleanValue();
        boolean nxt = (next == null) ? true : next.booleanValue();
        long reqCount = (requestedCount == null) ?
                logFile.getIndexSize() : requestedCount.intValue();
        long startingRecord;
        if (fromRecord == -1) {
            // In this case next/previous (before/after) don't mean much since
            // we don't have a reference record number.  So set before/after
            // according to the direction.
            nxt = forwd;

            // We +1 for reverse so that we see the very end of the file (the
            // query will not go past the "startingRecord", so we have to put
            // it after the end of the file)
            startingRecord = forwd ?
                    (-1) : ((logFile.getLastIndexNumber() + 1) * logFile.getIndexSize());
        } else {
            startingRecord = fromRecord.longValue();
            if (startingRecord < -1) {

                throw new IllegalArgumentException(
                        "fromRecord must be greater than 0!");
            }
        }

        // TO DO: If the fromRecord count is zero and the fromDate entry is
        // non-null, then the system should take advantage of file Indexing.
        // It should move the file position to the marker where the DateTime
        // query matches.
        try {
            return fetchRecordsUsingQuery(logFile, startingRecord, nxt, forwd,
                    reqCount, fromDate, toDate, logLevel,
                    onlyLevel.booleanValue(), listOfModules, nameValueMap, anySearch);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, LogFacade.ERROR_EXECUTING_LOG_QUERY, ex);
            return new AttributeList();
        }
    }

    public Vector getInstanceLogFileNames(String instanceName) {
        Server targetServer = domain.getServerNamed(instanceName);
        Vector allInstanceFileNames = new Vector();

        if (targetServer.isDas()) {
            String logFileDetailsForServer = "";

            try {
                // getting log file attribute value from logging.properties file
                logFileDetailsForServer = loggingConfig.getLoggingFileDetails();
                logFileDetailsForServer = TranslatedConfigView.getTranslatedValue(logFileDetailsForServer).toString();
                logFileDetailsForServer = new File(logFileDetailsForServer).getAbsolutePath();
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, LogFacade.ERROR_EXECUTING_LOG_QUERY, ex);
                return new Vector();
            }

            File logsDir = new File(logFileDetailsForServer.substring(0, logFileDetailsForServer.lastIndexOf(File.separator)));
            File allLogFileNames[] = logsDir.listFiles();
            for (File file : allLogFileNames) {
                String fileName = file.getName();
                if (file.isFile() && !fileName.equals(".") && !fileName.equals("..")
                        && fileName.contains(".log") && !fileName.contains(".log.")) {
                    allInstanceFileNames.add(fileName);
                }
            }
        } else {
            try {
                // getting log file attribute value from logging.properties file
                String instanceLogFileDetails = getInstanceLogFileDetails(targetServer);
                allInstanceFileNames = new LogFilterForInstance().getInstanceLogFileNames(habitat, targetServer, domain, LOGGER, instanceName, instanceLogFileDetails);
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, LogFacade.ERROR_EXECUTING_LOG_QUERY, ex);
                return new Vector();
            }
        }
        return allInstanceFileNames;
    }

    /*
        This function is used to get log file details from logging.properties file for given target.
     */

    private String getInstanceLogFileDetails(Server targetServer) throws IOException {

        String logFileDetailsForServer = "";
        String targetConfigName = "";

        Cluster clusterForInstance = targetServer.getCluster();
        if (clusterForInstance != null) {
            targetConfigName = clusterForInstance.getConfigRef();
        } else {
            targetConfigName = targetServer.getConfigRef();
        }

        logFileDetailsForServer = loggingConfig.getLoggingFileDetails(targetConfigName);

        return logFileDetailsForServer;

    }

    /*
    * This method is used by LogViewerResource which is used to display raw log data like 'tail -f server.log'.
    */
    public String getLogFileForGivenTarget(String targetServerName) throws IOException {
        Server targetServer = domain.getServerNamed(targetServerName);
        String serverNode = targetServer.getNodeRef();

        if (targetServer.isDas()) {
            // getting log file for DAS from logging.properties and returning the same
            String logFileDetailsForServer = loggingConfig.getLoggingFileDetails();
            logFileDetailsForServer = TranslatedConfigView.getTranslatedValue(logFileDetailsForServer).toString();
            logFileDetailsForServer = new File(logFileDetailsForServer).getAbsolutePath();
            return logFileDetailsForServer;
        } else {
            // getting log file for instance from logging.properties
            String logFileDetailsForInstance = getInstanceLogFileDetails(targetServer);
            Node node = domain.getNodes().getNode(serverNode);
            String loggingDir = "";
            String loggingFile = "";

            // replacing instanceRoot value if it's there
            if (logFileDetailsForInstance.contains("${com.sun.aas.instanceRoot}/logs") && node.getNodeDir() != null) {
                // this code is used if no changes made in logging.properties file
                loggingDir = node.getNodeDir() + File.separator + serverNode
                        + File.separator + targetServerName;
                loggingFile = logFileDetailsForInstance.replace("${com.sun.aas.instanceRoot}", loggingDir);
            } else if (logFileDetailsForInstance.contains("${com.sun.aas.instanceRoot}/logs") && node.getInstallDir() != null) {
                loggingDir = node.getInstallDir() + File.separator + "glassfish" + File.separator + "nodes"
                        + File.separator + serverNode + File.separator + targetServerName;
                loggingFile = logFileDetailsForInstance.replace("${com.sun.aas.instanceRoot}", loggingDir);
            } else {
                loggingFile = logFileDetailsForInstance;
            }

            if (node.isLocal()) {
                // if local just returning log file to view
                return loggingFile;
            } else {
                // if remote then need to download log file on DAS and returning that log file for view
                String logFileName = logFileDetailsForInstance.substring(logFileDetailsForInstance.lastIndexOf(File.separator) + 1, logFileDetailsForInstance.length());
                File instanceFile = null;
                instanceFile = new LogFilterForInstance().downloadGivenInstanceLogFile(habitat, targetServer, domain, LOGGER,
                        targetServerName, env.getDomainRoot().getAbsolutePath(), logFileName, logFileDetailsForInstance);

                return instanceFile.getAbsolutePath();
            }

        }

    }


    public AttributeList getLogRecordsUsingQuery(
            String logFileName, Long fromRecord, Boolean next, Boolean forward,
            Integer requestedCount, Date fromDate, Date toDate,
            String logLevel, Boolean onlyLevel, List listOfModules,
            Properties nameValueMap, String anySearch, String instanceName) {

        Server targetServer = domain.getServerNamed(instanceName);

        File instanceLogFile = null;

        if (targetServer.isDas()) {
            return getLogRecordsUsingQuery(
                    logFileName, fromRecord, next, forward,
                    requestedCount, fromDate, toDate,
                    logLevel, onlyLevel, listOfModules,
                    nameValueMap, anySearch);
        } else {
            // for Instance it's going through this loop. This will use ssh utility to get file from instance machine(remote machine) and
            // store under glassfish/domains/domain1/logs/<instance name>/ directory which is used to get LogFile object.
            // Right now user needs to go through this URL to setup and configure ssh http://wikis.sun.com/display/GlassFish/3.1SSHSetup

            String serverNode = targetServer.getNodeRef();
            Node node = domain.getNodes().getNode(serverNode);
            String loggingDir = "";
            String instanceLogFileName = "";
            try {
                // getting lof file details for given target.
                instanceLogFileName = getInstanceLogFileDetails(targetServer);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, LogFacade.ERROR_EXECUTING_LOG_QUERY, e);
                return new AttributeList();
            }


            if (node.isLocal()) {

                loggingDir = new LogFilterForInstance().getLoggingDirectoryForNode(instanceLogFileName, node, serverNode, instanceName);

                File logsDir = new File(loggingDir);
                File allLogFileNames[] = logsDir.listFiles();


                boolean noFileFound = true;

                if (allLogFileNames != null) { // This check for,  if directory doesn't present or missing on machine. It happens due to bug 16451
                    for (int i = 0; i < allLogFileNames.length; i++) {
                        File file = allLogFileNames[i];
                        String fileName = file.getName();
                        // code to remove . and .. file which is return
                        if (file.isFile() && !fileName.equals(".") && !fileName.equals("..") && fileName.contains(".log")
                                && !fileName.contains(".log.")) {
                            noFileFound = false;
                            break;
                        }
                    }
                }

                if (noFileFound) {
                    // this loop is used when user has changed value for server.log but not restarted the server.
                    loggingDir = new LogFilterForInstance().getLoggingDirectoryForNodeWhenNoFilesFound(instanceLogFileName, node, serverNode, instanceName);

                }

                instanceLogFile = new File(loggingDir + File.separator + logFileName);

                // verifying loggingFile presents or not if not then changing logFileName value to server.log. It means wrong name is coming
                // from GUI to back end code.
                if (!instanceLogFile.exists()) {
                    instanceLogFile = new File(loggingDir + File.separator + "server.log");
                } else if (!instanceLogFile.exists()) {
                    // this code is used when user changes the attributes value(com.sun.enterprise.server.logging.GFFileHandler.file) in
                    // logging.properties file to something else.
                    loggingDir = instanceLogFileName.substring(0, instanceLogFileName.lastIndexOf(File.separator));
                    instanceLogFile = new File(loggingDir + File.separator + logFileName);
                    if (!instanceLogFile.exists()) {
                        instanceLogFile = new File(instanceLogFileName);
                    }
                }

            } else {

                try {
                    // this code is used when the node is not local.
                    instanceLogFile = new LogFilterForInstance().downloadGivenInstanceLogFile(habitat, targetServer,
                            domain, LOGGER, instanceName, env.getDomainRoot().getAbsolutePath(), logFileName, instanceLogFileName);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, LogFacade.ERROR_EXECUTING_LOG_QUERY, e);
                    return new AttributeList();
                }

            }
        }

        LogFile logFile = null;
        File loggingFileExists = new File(instanceLogFile.getAbsolutePath());
        if (!loggingFileExists.exists()) {
            LOGGER.log(Level.WARNING, LogFacade.INSTANCE_LOG_FILE_NOT_FOUND, instanceLogFile.getAbsolutePath());
            return new AttributeList();
        }
        logFile = getLogFile(instanceLogFile.getAbsolutePath());
        boolean forwd = (forward == null) ? true : forward.booleanValue();
        boolean nxt = (next == null) ? true : next.booleanValue();
        long reqCount = (requestedCount == null) ?
                logFile.getIndexSize() : requestedCount.intValue();
        long startingRecord;
        if (fromRecord == -1) {
            // In this case next/previous (before/after) don't mean much since
            // we don't have a reference record number.  So set before/after
            // according to the direction.
            nxt = forwd;

            // We +1 for reverse so that we see the very end of the file (the
            // query will not go past the "startingRecord", so we have to put
            // it after the end of the file)
            startingRecord = forwd ?
                    (-1) : ((logFile.getLastIndexNumber() + 1) * logFile.getIndexSize());
        } else {
            startingRecord = fromRecord.longValue();
            if (startingRecord < -1) {
                throw new IllegalArgumentException(
                        "fromRecord must be greater than 0!");
            }
        }

        // TO DO: If the fromRecord count is zero and the fromDate entry is
        // non-null, then the system should take advantage of file Indexing.
        // It should move the file position to the marker where the DateTime
        // query matches.
        try {
            return fetchRecordsUsingQuery(logFile, startingRecord, nxt, forwd,
                    reqCount, fromDate, toDate, logLevel,
                    onlyLevel.booleanValue(), listOfModules, nameValueMap, anySearch);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, LogFacade.ERROR_EXECUTING_LOG_QUERY, ex);
            return new AttributeList();
        }
    }


    /**
     * Internal method that will be called from getLogRecordsUsingQuery()
     */
    protected AttributeList fetchRecordsUsingQuery(
            LogFile logFile, long startingRecord, boolean next, boolean forward,
            long requestedCount, Date fromDate, Date toDate, String logLevel,
            boolean onlyLevel, List listOfModules, Properties nameValueMap, String anySearch) {
        // If !next, then set to search in reverse
        boolean origForward = forward;
        if (next) {
            startingRecord++;
            forward = true;
        } else {
            forward = false;
        }

        // Setup forward/reverse stuff
        int inc = 1;
        int start = 0;  // depends on length of results (for reverse)
        int end = -1;   // depends on length of results (for forward)
        long searchChunkIncrement = requestedCount;
        if (!forward) {
            inc = -1;
            // Move back to find records before the startingRecord
            // -1 because we still want to see the starting record (only if in
            // "next" mode)
            startingRecord -=
                    ((next) ? (searchChunkIncrement - 1) : (searchChunkIncrement));
            if (startingRecord < 0) {
                // Don't go past the original startingRecord
                searchChunkIncrement += startingRecord;
                startingRecord = 0;
            }
        }

        // Make sure the module names are correct
        //updateModuleList(listOfModules);

        // Keep pulling records to search through until we get enough matches
        List results = new ArrayList();
        List records = null;
        LogFile.LogEntry entry = null;
        while (results.size() < requestedCount) {
            // The following will always return unfiltered forward records
            records = logFile.getLogEntries(
                    startingRecord, searchChunkIncrement);
            if (records == null) {
                break;
            }

            // Determine end/start
            if (forward) {
                end = records.size();
            } else {
                start = records.size() - 1;
            }

            // Loop through the records, filtering and storing the matches
            for (int count = start;
                 (count != end) && (results.size() < requestedCount);
                 count += inc) {
                entry = (LogFile.LogEntry) records.get(count);
                if (allChecks(entry, fromDate, toDate, logLevel, onlyLevel,
                        listOfModules, nameValueMap, anySearch)) {
                    results.add(entry);
                }
            }

            // Update startingRecord / searchChunkIncrement & check for finish
            if (forward) {
                // If the record size is smaller than requested, then there
                // are no more records.
                if (records.size() < searchChunkIncrement) {
                    break;
                }

                // Get starting record BEFORE updating searchChunkIncrement to
                // skip all the records we already saw
                startingRecord += searchChunkIncrement * inc;
                searchChunkIncrement = requestedCount - results.size();
            } else {
                // If we already searched from 0, then there are no more
                if (startingRecord == 0) {
                    break;
                }

                // Get starting record AFTER updating searchChunkIncrement
                searchChunkIncrement = requestedCount - results.size();
                startingRecord += searchChunkIncrement * inc;
                if (startingRecord < 1) {
                    searchChunkIncrement += startingRecord;
                    startingRecord = 0;
                }
            }
        }

        // Deal with previous&forward or next&reverse
        if (next ^ origForward) {
            List reversedResults = new ArrayList();
            // Reverse the results
            for (int count = results.size() - 1; count > -1; count--) {
                reversedResults.add(results.get(count));
            }
            results = reversedResults;
        }

        // Return the matches.  If this is less than requested, then there are
        // no more.
        if (DEBUG) {
            System.out.println("Log filter results size="+results.size() + ", requestedCount=" + requestedCount);
        }
        return convertResultsToTheStructure(results);
    }

    /**
     * This method converts the results to the appropriate structure for
     * LogMBean to return to the Admin Front End.
     * <p/>
     * AttributeList Results contain 2 attributes
     * <p/>
     * Attribute 1: Contains the Header Information, that lists out all the
     * Field Names and Positions
     * Attribute 2: Contains the actual Results, Each Log record is an entry
     * of this result. The LogRecord itself is an ArrayList of
     * all fields.
     */
    private AttributeList convertResultsToTheStructure(List results) {
        if (results == null) {
            return null;
        }
        AttributeList resultsInTemplate = new AttributeList();
        resultsInTemplate.add(LogRecordTemplate.getHeader());
        Iterator iterator = results.iterator();
        ArrayList listOfResults = new ArrayList();
        Attribute resultsAttribute = new Attribute(RESULTS_ATTRIBUTE,
                listOfResults);
        resultsInTemplate.add(resultsAttribute);
        while (iterator.hasNext()) {
            LogFile.LogEntry entry = (LogFile.LogEntry) iterator.next();
            ArrayList logRecord = new ArrayList();
            logRecord.add(Long.valueOf(entry.getRecordNumber()));
            logRecord.add(entry.getLoggedDateTime());
            logRecord.add(entry.getLoggedLevel());
            logRecord.add(entry.getLoggedProduct());
            logRecord.add(entry.getLoggedLoggerName());
            logRecord.add(entry.getLoggedNameValuePairs());
            logRecord.add(entry.getMessageId());
            logRecord.add(entry.getLoggedMessage());
            listOfResults.add(logRecord);
        }
        return resultsInTemplate;
    }


    /**
     * This provides access to the LogFile object.
     */
    public LogFile getLogFile() {
        return _logFile;
    }

    /**
     * This fetches or updates logFileCache entries.
     * <p/>
     * _REVISIT_: We may want to limit the entries here as each logFile
     * takes up so much of memory to maintain indexes
     */
    public LogFile getLogFile(String fileName) {
        // No need to check for null or zero length string as the
        // test is already done before.
        String logFileName = fileName.trim();
        LogFile logFile = (LogFile) logFileCache.get(fileName);
        String parent = null;
        if (logFile == null) {
            try {
                // First check if the fileName provided is an absolute filename
                // if yes, then we don't have to construct the parent element
                // path with the parent.
                if (new File(fileName).exists()) {
                    logFile = new LogFile(fileName);
                    logFileCache.put(fileName, logFile);
                    return logFile;
                }

                // If the absolute path is not provided, the burden of
                // constructing the parent path falls on us. We try
                // using the default parent path used for the current LogFile.
                // assume the user specified the path from the instance root and that is the parent

                parent = System.getProperty(
                        SystemPropertyConstants.INSTANCE_ROOT_PROPERTY);

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, LogFacade.ERROR_EXECUTING_LOG_QUERY, e);
            }
            if (parent != null) {
                // Just use the parent directory from the other server.log
                // file.
                String[] logFileNameParts = {parent, logFileName};
                logFileName = StringUtils.makeFilePath(
                        logFileNameParts, false);
            }
            logFile = new LogFile(logFileName);
            logFileCache.put(fileName, logFile);
        }
        return logFile;
    }

    /**
     * This method accepts the first line of the Log Record and checks
     * to see if it matches the query.
     */
    protected boolean allChecks(LogFile.LogEntry entry,
                                Date fromDate, Date toDate, String queryLevel, boolean onlyLevel,
                                List listOfModules, Properties nameValueMap, String anySearch) {
        if (DEBUG) {
            StringBuffer buf = new StringBuffer();
            buf.append(dateTimeCheck(entry.getLoggedDateTime(), fromDate, toDate));
            buf.append(",");
            buf.append(levelCheck(entry.getLoggedLevel(), queryLevel, onlyLevel));
            buf.append(",");
            buf.append(moduleCheck(entry.getLoggedLoggerName(), listOfModules));
            buf.append(",");
            buf.append(nameValueCheck(entry.getLoggedNameValuePairs(), nameValueMap));
            buf.append(",");
            buf.append(messageDataCheck(entry.getLoggedMessage(), entry.getLoggedNameValuePairs(), anySearch));
            System.out.println("allChecks="+buf.toString()); 
        }
        
        if ((dateTimeCheck(entry.getLoggedDateTime(), fromDate, toDate))
                && (levelCheck(entry.getLoggedLevel(), queryLevel, onlyLevel))
                && (moduleCheck(entry.getLoggedLoggerName(), listOfModules))
                && (nameValueCheck(entry.getLoggedNameValuePairs(), nameValueMap))
                && (messageDataCheck(entry.getLoggedMessage(), entry.getLoggedNameValuePairs(), anySearch))) {
            return true;
        }

        return false;
    }


    protected boolean dateTimeCheck(Date loggedDateTime,
                                    Date fromDateTime, Date toDateTime) {
        if ((fromDateTime == null) || (toDateTime == null)) {
            // If user doesn't specify fromDate and toDate, then S/He is
            // not interested in DateTime filter
            return true;
        }
        // Now do a range check
        if (!(loggedDateTime.before(fromDateTime) ||
                loggedDateTime.after(toDateTime))) {
            return true;
        } 
        
        return false;
    }


    protected boolean levelCheck(
            final String loggedLevel,
            final String queryLevelIn,
            final boolean isOnlyLevelFlag) {
        // If queryLevel is null, that means user is not interested in
        // running the query on the Log Level field.
        if (queryLevelIn == null) {
            return true;
        }
        final String queryLevel = queryLevelIn.trim();
        if (isOnlyLevelFlag) {
            // This means the user is interested in seeing log messages whose
            // log level is equal to what is specified
            return loggedLevel.equals(queryLevel);
        } else {
            try {
                int loggedLevelValue = Level.parse(loggedLevel).intValue();
                int queryLevelValue = Level.parse(queryLevelIn).intValue();
                return (loggedLevelValue >= queryLevelValue);
            } catch(Exception e) {
                return true;                
            }
        }
    }

    protected boolean moduleCheck(String loggerName, List modules) {
        if ((modules == null) || (modules.size() == 0)) {
            return true;
        }
        loggerName = loggerName.trim();        
        Iterator iterator = modules.iterator();
        while (iterator.hasNext()) {
            String module = (String) iterator.next();
            module = module.trim();
            if (loggerName.equals(module)) {
                return true;
            }
        }
        return false;
    }

    protected boolean nameValueCheck(String loggedNameValuePairs,
                                     Properties queriedNameValueMap) {
        if ((queriedNameValueMap == null) || (queriedNameValueMap.size() == 0)) {
            return true;
        }
        if (loggedNameValuePairs == null) {
            // We didn't match the name values...
            return false;
        }
        StringTokenizer nvListTokenizer =
                new StringTokenizer(loggedNameValuePairs, NV_SEPARATOR);
        while (nvListTokenizer.hasMoreTokens()) {
            String nameandvalue = nvListTokenizer.nextToken();
            StringTokenizer nvToken = new StringTokenizer(nameandvalue, "=");
            if (nvToken.countTokens() < 2)
                continue;
            String loggedName = nvToken.nextToken();
            String loggedValue = nvToken.nextToken();

            // Reset the iterator to start from the first entry AGAIN
            // FIXME: Is there any other cleaner way to reset the iterator 
            // position to zero than recreating a new iterator everytime
            Iterator queriedNameValueMapIterator =
                    queriedNameValueMap.entrySet().iterator();

            while (queriedNameValueMapIterator.hasNext()) {
                Map.Entry entry =
                        (Map.Entry) queriedNameValueMapIterator.next();
                if (entry.getKey().equals(loggedName)) {
                    Object value = entry.getValue();
                    // We have a key with multiple values to match.
                    // This will happen if the match condition is like
                    // _ThreadID=10 or _ThreadID=11 
                    // _REVISIT_: There is an opportunity to improve performance
                    // for this search.
                    Iterator iterator = ((java.util.List) value).iterator();
                    while (iterator.hasNext()) {
                        if (((String) iterator.next()).equals(
                                loggedValue)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    protected boolean messageDataCheck(String message, String nvp,
                                       String anySearch) {

        if (anySearch == null || ("").contains(anySearch) || anySearch.length() < 3) {
            return true;
        }

        if ((message != null && message.contains(anySearch)) || (nvp != null && nvp.contains(anySearch))) {
            return true;
        }

        return false;
    }


    static final String[] LOG_LEVELS = {"SEVERE", "WARNING",
            "INFO", "CONFIG", "FINE", "FINER", "FINEST"};

    private static String[] serverLogElements =
            {System.getProperty("com.sun.aas.instanceRoot"), "logs", "server.log"};

    private static String pL =
            System.getProperty("com.sun.aas.processLauncher");
    private static String verboseMode =
            System.getProperty("com.sun.aas.verboseMode", "false");
    private static String defaultLogFile =
            System.getProperty("com.sun.aas.defaultLogFile");
    private LogFile _logFile =
            (pL != null && !verboseMode.equals("true") && defaultLogFile != null) ?
                    new LogFile(defaultLogFile) :
                    new LogFile(StringUtils.makeFilePath(serverLogElements, false));
    private static Hashtable logFileCache = new Hashtable();
}
