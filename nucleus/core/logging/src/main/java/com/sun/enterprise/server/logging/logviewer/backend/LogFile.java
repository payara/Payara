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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;

import com.sun.enterprise.server.logging.LogFacade;
import com.sun.enterprise.server.logging.parser.LogParser;
import com.sun.enterprise.server.logging.parser.LogParserFactory;
import com.sun.enterprise.server.logging.parser.LogParserListener;
import com.sun.enterprise.server.logging.parser.ParsedLogRecord;


/**
 * <P>This class encapsulates the log file so that its details are not
 * exposed.  "getLongEntries" returns an unfiltered List of LogEntry objects
 * from the requested record number.  It will always search forward.
 * getIndexSize() returns the number of records between each index.
 * getLastIndexNumber returns the last index.</P>
 *
 * @AUTHOR: Hemanth Puttaswamy and Ken Paulsen
 * <p/>
 * <P>This class also contains an inner class for storing LogEntry
 * objects.</P>
 */
public class LogFile implements java.io.Serializable {

    private static final long serialVersionUID = -2960142541274652618L;
    
    private static SimpleDateFormat SIMPLE_DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    /**
     * Constructor
     */
    public LogFile(String name) {
        _logFileName = name;
        _recordIdx.add(Long.valueOf(0));
    }

    /**
     * This method returns up to _indexSize records starting with the given
     * record number.
     *
     * @param    startingRecord    The starting point to search for LogEntries
     */
    public List getLogEntries(long startingRecord) {
        return getLogEntries(startingRecord, getIndexSize());
    }

    /**
     * This method returns up to _indexSize records starting with the given
     * record number.  It will return up to "maxRecords" records.
     *
     * @param    startingRecord    The starting point to search for LogEntries
     * @param    maxRecords    The maximum number of records to return
     */
    public List getLogEntries(final long startingRecord, final long maxRecords) {
        if (startingRecord < 0) {
            return null;
        }

        // Open the file at the desired starting Record
        final long recordsToIgnore = (startingRecord % getIndexSize());
        BufferedReader reader = getFilePosition(startingRecord - recordsToIgnore);
        final List results = new ArrayList();
        if (reader == null) {
            return results;
        }
        try {
            
            File logFile = new File(getLogFileName());
            LogParser logParser = LogParserFactory.getInstance().createLogParser(logFile );
            logParser.parseLog(reader, new LogParserListener() {

                long counter = 0;

                @Override
                public void outputSummary(BufferedWriter writer, Object... objects)
                        throws IOException {
                }
                
                @Override
                public void foundLogRecord(long position, ParsedLogRecord logRecord) {
                    counter++;
                    if (counter <= recordsToIgnore) {
                        return;
                    }
                    if (results.size() < maxRecords) {
                        LogEntry entry = new LogEntry(logRecord.getFormattedLogRecord(),
                                startingRecord + results.size());                    
                        entry.setLoggedDateTime(new Date(logRecord.getTimeMillis()));
                        entry.setLoggedLevel(logRecord.getLevel());
                        entry.setLoggedLoggerName(logRecord.getLogger());
                        entry.setLoggedMessage(logRecord.getMessage());
                        entry.setLoggedNameValuePairs(logRecord.getSupplementalAttributes().toString());
                        entry.setLoggedProduct(logRecord.getComponentId());
                        entry.setMessageId(logRecord.getMessageId());                        
                        results.add(entry);
                    }
                }
                
                @Override
                public void close() throws IOException {                    
                }
            });
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                }
            }
        }

        // Return the results
        return results;
    }


    /**
     * This method builds the file index in the beginning.  The index is for
     * the beginning of every record after the size specified by '_indexSize'
     * variable.
     */
    private synchronized void buildLogFileIndex() {
        // Open the file and skip to the where we left off
        final long startPos = (Long) _recordIdx.get(_recordIdx.size() - 1);
        final long localIndexSize = getIndexSize();
        BufferedReader reader = getLogFileReader(startPos);
        try {
            File logFile = new File(getLogFileName());
            LogParser logParser = LogParserFactory.getInstance().createLogParser(logFile);
            if (logParser != null) {
                logParser.parseLog(reader, new LogParserListener() {
                    
                    long recordNumber = (_recordIdx.size() - 1) * localIndexSize;

                    @Override
                    public void outputSummary(BufferedWriter writer, Object... objects)
                            throws IOException {
                    }
                    
                    @Override
                    public void foundLogRecord(long position, ParsedLogRecord object) {                    
                        long modIndex = recordNumber % localIndexSize;
                        if (modIndex == 0) {
                            _recordIdx.add((Long)(startPos+position));
                        }
                        recordNumber++;
                    }
                    
                    @Override
                    public void close() throws IOException {                    
                    }
                });                
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                }
            }
        }
    }

    /**
     * This method returns the file position given the record number.
     *
     * @return The file position.
     * @param    recordNumber    The Record Number
     */
    private BufferedReader getFilePosition(long recordNumber) {
        // The index is stored from the second slot. i.e., if there
        // are 100 records and the index will be on 20, 40, 60, 80, 100
        // if the _indexSize is 20. We don't store '0' hence we subtract
        // from 1 to get the right index
        int index = (int) (recordNumber / getIndexSize());
        if (index > _recordIdx.size()-1) {
            return null;
        }
        Long filePosition = (Long) _recordIdx.get(index);
        return getLogFileReader(filePosition);
    }

    /**
     * This method opens the server.log file and moves the stream to
     * the specified filePosition.
     */
    protected BufferedReader getLogFileReader(long fromFilePosition) {
        FileInputStream file = null;
        try {
            file = new FileInputStream(getLogFileName());
            long bytesToSkip = fromFilePosition-1;
            if (bytesToSkip > 0) {
                long bytesSkipped = file.skip(bytesToSkip);
                if (bytesSkipped != fromFilePosition) {
                    if (LogFacade.LOGGING_LOGGER.isLoggable(Level.FINE)) { 
                        LogFacade.LOGGING_LOGGER.log(Level.FINE, "Did not skip exact bytes while positioning reader in " + getLogFileName());
                    }
                }
            }
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(file));
            return reader;
        } catch (Exception ex) {
            if (LogFacade.LOGGING_LOGGER.isLoggable(Level.FINE)) { 
                LogFacade.LOGGING_LOGGER.log(Level.FINE, "Error reading from file: " + getLogFileName(), ex);
            }
            if (file != null) try { file.close(); } catch (Exception ex2) {
                if (LogFacade.LOGGING_LOGGER.isLoggable(Level.FINE)) { 
                    LogFacade.LOGGING_LOGGER.log(Level.FINE, "Error closing file: " + getLogFileName(), ex2);
                }                
            }            
        } 
        return null;
    }

    /**
     *
     */
    public String getLogFileName() {
        return _logFileName;
    }

    /**
     * The log records are indexed, this method returns the last index.  It
     * will ensure that the indexes are up-to-date.
     */
    public long getLastIndexNumber() {
        buildLogFileIndex();
        return _recordIdx.size() - 1;
    }

    /**
     *
     */
    public long getIndexSize() {
        return _indexSize;
    }

    /**
     * Class to manage LogEntry information
     */
    public static class LogEntry implements java.io.Serializable {
        
        /**
         * SVUID for backwards compatibility
         */
        private static final long serialVersionUID = -8597022493595023899L;
        
        public LogEntry(String line, long recordNumber) {
            setRecordNumber(recordNumber);
        }

        /**
         *
         */
        public Date getLoggedDateTime() {
            return this.loggedDateTime;
        }


        /**
         *
         */
        public void setLoggedDateTime(Date loggedDateTime) {
            this.loggedDateTime = loggedDateTime;
        }


        /**
         *
         */
        public String getLoggedLevel() {
            return loggedLevel;
        }


        /**
         *
         */
        public void setLoggedLevel(String loggedLevel) {
            this.loggedLevel = loggedLevel;
        }


        /**
         *
         */
        public String getLoggedProduct() {
            return loggedProduct;
        }


        /**
         *
         */
        public void setLoggedProduct(String loggedProduct) {
            this.loggedProduct = loggedProduct;
        }


        /**
         *
         */
        public String getLoggedLoggerName() {
            return loggedLoggerName;
        }


        /**
         *
         */
        public void setLoggedLoggerName(String loggedLoggerName) {
            this.loggedLoggerName = loggedLoggerName;
        }


        /**
         *
         */
        public String getLoggedNameValuePairs() {
            return loggedNameValuePairs;
        }


        /**
         *
         */
        public void setLoggedNameValuePairs(String loggedNameValuePairs) {
            this.loggedNameValuePairs = loggedNameValuePairs;
        }

        /**
         *
         */
        public void setLoggedMessage(String message) {
            this.loggedMessage = message;
        }

        public void appendLoggedMessage(String message) {
            loggedMessage += message;
        }


        /**
         *
         */
        public String getLoggedMessage() {
            return loggedMessage;
        }

        public String getMessageId() {
            return messageId;
        }

        public void setMessageId(String messageId) {
            this.messageId = messageId;
        }

        /**
         *
         */
        public long getRecordNumber() {
            return recordNumber;
        }


        /**
         *
         */
        public void setRecordNumber(long recordNumber) {
            this.recordNumber = recordNumber;
        }

        public String toString() {
            return getRecordNumber() + ":" + getLoggedMessage();
        }

        private long recordNumber = -1;
        private Date loggedDateTime = null;
        private String loggedLevel = null;
        private String loggedProduct = null;
        private String loggedLoggerName = null;
        private String loggedNameValuePairs = null;
        private String loggedMessage = null;
        private String messageId = "";
    }
    
    private long _indexSize = 10;
    private String _logFileName = null;
    private List _recordIdx   = new ArrayList();
    
}
