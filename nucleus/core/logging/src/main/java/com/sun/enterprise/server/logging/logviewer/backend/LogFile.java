/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2011 Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;


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

    private static SimpleDateFormat SIMPLE_DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    /**
     * Constructor
     */
    public LogFile(String name) {
        _recordIdx.add(Long.valueOf(0));
        setLogFileName(name);
        //START CR 6697509
        //buildLogFileIndex();
        //END CR 6697509
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
     * @param    masRecords    The maximum number of records to return
     */
    public List getLogEntries(long startingRecord, long maxRecords) {
        if (startingRecord < 0) {
            return null;
        }

        // Open the file at the desired starting Record
        BufferedReader reader = getFilePosition(startingRecord);
        List results = new ArrayList();
        try {
            while (results.size() < maxRecords) {
                // Get a line from the log file
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                if (!line.startsWith(RECORD_BEGIN_MARKER)) {
                    continue;
                }

                // Read the whole record
                while (!line.endsWith(RECORD_END_MARKER)) {
                    line += "\n" + reader.readLine();
                }

                // Read the LogEntry
                try {
                    results.add(new LogEntry(line,
                            startingRecord + results.size()));
                } catch (IllegalArgumentException ex) {
                    Logger.getAnonymousLogger().log(Level.INFO, "Could not read the log entry", ex);
                }
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

        // Return the results
        return results;
    }


    /**
     * This method builds the file index in the beginning.  The index is for
     * the beginning of every record after the size specified by '_indexSize'
     * variable.
     */
    protected synchronized void buildLogFileIndex() {
        int cnt, idx;
        // NOTE: This should be -1 for indexing at the right intervals
        long recordCount = -1;
        char recordBeginMarker[] = RECORD_BEGIN_MARKER.toCharArray();
        int recordBeginMarkerLen = recordBeginMarker.length;

        // Open the file and skip to the where we left off
        long charPos = ((Long) _recordIdx.get(_recordIdx.size() - 1)).longValue();
        BufferedReader reader = getLogFileReader(charPos);
        long localIndexSize = getIndexSize();
        try {
            while (true) {
                try {
                    cnt = reader.read();
                    if (cnt == -1) {
                        break;
                    }
                    charPos++;

                    // Compare to RECORD_BEGIN_MARKER
                    for (idx = 0; idx < recordBeginMarkerLen; idx++) {
                        if (cnt != recordBeginMarker[idx]) {
                            break;
                        }
                        cnt = reader.read();
                        charPos++;
                    }
                    if (idx == recordBeginMarkerLen) {
                        // Begining of a new record
                        recordCount++;
                        if (recordCount == localIndexSize) {
                            // Now we have traversed the records equal
                            // to index size. Time to add a new entry
                            // into the index.
                            recordCount = 0;
                            _recordIdx.add(Long.valueOf(charPos - (recordBeginMarkerLen + 1)));
                        }
                    }
                } catch (EOFException ex) {
                    break;
                } catch (Exception ex) {
                    Logger.getAnonymousLogger().log(Level.INFO, "Error trying to position where we left off", ex);

                    break;
                }
            }
        } catch (Exception ex) {
            Logger.getAnonymousLogger().log(Level.INFO, "Error trying to position where we left off", ex);

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
    protected BufferedReader getFilePosition(long recordNumber) {
        // The index is stored from the second slot. i.e., if there
        // are 100 records and the index will be on 20, 40, 60, 80, 100
        // if the _indexSize is 20. We don't store '0' hence we subtract
        // from 1 to get the right index
        int index = (int) (recordNumber / getIndexSize());
        if (_recordIdx.size() <= index) {
            // We have not indexed enough
            buildLogFileIndex();
            if (_recordIdx.size() <= index) {
                // Hmm... something's not right
                throw new IllegalArgumentException(
                        "Attempting to access Log entries that don't exist! ");
            }
        }
        return getRecordPosition(index,
                (int) (recordNumber % getIndexSize()));
    }


    /**
     * Gets the precise record position from the specified FilePosition.
     */
    private BufferedReader getRecordPosition(int index, int recordsToAdvance) {
        // Get the indexed file position
        long filePosition = ((Long) _recordIdx.get(index)).longValue();
        BufferedReader reader = getLogFileReader(filePosition);

        char recordBeginMarker[] = RECORD_BEGIN_MARKER.toCharArray();
        int recordBeginMarkerLen = recordBeginMarker.length;
        int ch;
        int idx;
        while (recordsToAdvance > 0) {
            try {
                // If we read past the end, throw exception
                ch = reader.read();
                filePosition++;

                // Compare to RECORD_BEGIN_MARKER
                for (idx = 0; idx < recordBeginMarkerLen; idx++) {
                    if (ch != recordBeginMarker[idx]) {
                        break;
                    }
                    ch = reader.read();
                    filePosition++;
                }
                if (idx == recordBeginMarkerLen) {
                    // Begining of a new record
                    recordsToAdvance--;
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        // Return the reader
        return reader;
    }


    /**
     * This method opens the server.log file and moves the stream to
     * the specified filePosition.
     */
    protected BufferedReader getLogFileReader(long fromFilePosition) {
        try {
            FileInputStream file = new FileInputStream(getLogFileName());
            file.skip(fromFilePosition);
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(file));
            return reader;
        } catch (Exception ex) {
            Logger.getAnonymousLogger().log(Level.INFO, "Exception in openFile...", ex);
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
     *
     */
    public void setLogFileName(String filename) {
        if (filename.equals(getLogFileName())) {
            return;
        }
        _logFileName = filename;
        _recordIdx = new ArrayList();
        _recordIdx.add(Long.valueOf(0));
    }


    /**
     * The log records are indexed, this method returns the last index.  It
     * will ensure that the indexes are up-to-date.
     */
    public long getLastIndexNumber() {
        // Ensure the file is fully indexed for this call
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
     * The number of records between indexes.  This is also used as the max
     * number of records returned from getLogEntries(long).
     */
    public void setIndexSize(long indexSize) {
        _indexSize = indexSize;
    }


    /**
     * Class to manage LogEntry information
     */
    public class LogEntry implements java.io.Serializable {
        public LogEntry(String line, long recordNumber) {
            if (!line.startsWith(RECORD_BEGIN_MARKER)) {
                throw new IllegalArgumentException(
                        "Log Entries must start with: '" + RECORD_BEGIN_MARKER +
                                "': '" + line + "'.");
            }

            StringTokenizer tokenizer =
                    new StringTokenizer(line, FIELD_SEPARATOR);

            // We expect atleast the following tokens to be in the first line
            // [#, DateTime, log level, Product Name, Logger Name, Name Value
            // Pairs.
            // If we don't have them here. Then it's a wrong message.
            if (!(tokenizer.countTokens() > 5)) {
                throw new IllegalArgumentException(
                        "Log Entry does not contain all required fields: '" +
                                line + "'.");
            }

            // The first token is the Record Begin Marker
            tokenizer.nextToken();
            try {
                setLoggedDateTime(
                        SIMPLE_DATE_FORMAT.parse(tokenizer.nextToken()));
                setLoggedLevel(tokenizer.nextToken());
                setLoggedProduct(tokenizer.nextToken());
                setLoggedLoggerName(tokenizer.nextToken());
                setLoggedNameValuePairs(tokenizer.nextToken());
                String messageIdandMessage = tokenizer.nextToken();

                if (messageIdandMessage != null) {
                    int index = messageIdandMessage.indexOf(":");
                    if (index != -1) {
                        String messageId = messageIdandMessage.substring(0, index);
                        if(messageId.length()<10 && !messageId.contains(" ") && isAlphaNumeric(messageId)) {
                            setMessageId(messageIdandMessage.substring(0, index));
                            setLoggedMessage(messageIdandMessage.substring(index + 1));
                        } else {
                            setLoggedMessage(messageIdandMessage);                            
                        }
                    } else {
                        setLoggedMessage(messageIdandMessage);
                    }
                }
                setRecordNumber(recordNumber);
            } catch (Exception e) {
                RuntimeException t =
                        new RuntimeException("Error in building Log Entry ");
                t.initCause(e);
                throw t;
            }
        }

    private boolean isAlphaNumeric(String inputStr) {
        char str[] = inputStr.toCharArray();
        for (int i = 0; i < str.length; i++) {
            if (Character.isLetterOrDigit(str[i]) == false) {
                return false;
            }
        }
        return true;
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
            return "" + getRecordNumber();
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


    public static final String RECORD_BEGIN_MARKER = "[#|";
    public static final String RECORD_END_MARKER = "|#]";
    public static final String FIELD_SEPARATOR = "|";


    private long _indexSize = 10;
    private String _logFileName = null;
    private List _recordIdx		= new ArrayList();
}
