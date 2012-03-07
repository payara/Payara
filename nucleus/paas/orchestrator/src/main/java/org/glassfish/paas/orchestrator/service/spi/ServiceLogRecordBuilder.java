/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.paas.orchestrator.service.spi;

import org.jvnet.hk2.annotations.Service;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ServiceLogRecordBuilder is an utility class to convert your log entries or log files to ServiceLogRecord.
 * <p/>
 * How to use:
 * 1. Pass either log files or log entries
 * 2. Specify starting sequence, ending sequence, delimiter, default log level.
 * 3. Specify the parameters in order to your log entries.
 * E.g. [#|2012-01-16T11:33:04.943+0530|INFO|glassfish3.1.2|com.sun.enterprise.server.logging.GFFileHandler|
 * _ThreadID=1;_ThreadName=Thread-2;|Running GlassFish Version: GlassFish Server Open Source Edition 3.1.2-SNAPSHOT|#]
 * <p/>
 * Your parameters are  datetime, level, other, logger name, other, message
 * <p/>
 * Note: If you are passing datetime as parameter pass date formatter also.
 * 4. Now, call build to get List of ServiceLogRecord.
 * <p/>
 * User: naman mehta
 */
@Service
public class ServiceLogRecordBuilder {


    public final String LEVEL = "Level";
    public final String SEQUENCENUMBER = "SequenceNumber";
    public final String LOGGERNAME = "LoggerName";
    public final String MESSAGE = "Message";
    public final String SOURCECLASSNAME = "SourceClassName";
    public final String SOURCEMETHODNAME = "SourceMethodName";
    public final String THREADID = "ThreadID";
    public final String DATETIME = "Millis";
    public final String THROWN = "Thrown";
    public final String OTHER = "Parameters";

    private String startSequence;
    private String endSequence;
    private String delimiter;
    private String dateFormatter;
    private String[] parameters;
    private boolean isLB;
    private Level level;

    // set either logFile or logLines
    private File logFile;
    private List<String> logLines;

    private Logger logger = Logger.getLogger(ServiceLogRecordBuilder.class.getName());

    /**
     * Main method to get list of ServiceLogRecord for given log lines or log files.
     *
     * @return
     * @throws UnsupportedEncodingException
     */
    public List<ServiceLogRecord> build() throws UnsupportedEncodingException {
        validate(this);
        List<ServiceLogRecord> records = new ArrayList<ServiceLogRecord>();
        if (logLines == null) {
            logLines = getLogLines();
        }
        BufferedReader br = null;
        Iterator<String> iterator = logLines.iterator();
        while (iterator.hasNext()) {
            String line = iterator.next();
            System.out.println(line);
            br = new BufferedReader(
                    new CharArrayReader(line.toCharArray()));
            try {
                if (!isLB && line != null && line.startsWith(startSequence)) {
                    records.add(parse(line));
                } else if (isLB && line !=null) {
                    records.add(parseLB(line));
                }
            } catch (Exception ex) {
                logger.warning("Error while parsing log lines" + ex);
            }
        }
        if (br != null)
            try {
                br.close();
            } catch (IOException ex) {
                logger.warning("Error while closing buffer reader" + ex);
            }
        reset();
        return records;
    }

    /**
     * Validates initial parameters needed to build ServiceLogRecord.
     *
     * @param serviceLogRecordBuilder
     * @throws UnsupportedEncodingException
     */
    private void validate(ServiceLogRecordBuilder serviceLogRecordBuilder) throws UnsupportedEncodingException {
        if (serviceLogRecordBuilder.logLines == null && serviceLogRecordBuilder.logFile == null) {
            throw new UnsupportedEncodingException("Log Lines and Log Files are missing.");
        } else if (serviceLogRecordBuilder.level == null) {
            throw new UnsupportedEncodingException("Default log Level is Missing.");
        } else if (serviceLogRecordBuilder.parameters == null) {
            throw new UnsupportedEncodingException("Parameter is Missing.");
        } else {
            String[] myParameter = serviceLogRecordBuilder.parameters;
            List myPList = Arrays.asList(myParameter);
            if (myPList.contains(DATETIME)) {
                if (serviceLogRecordBuilder.dateFormatter == null || serviceLogRecordBuilder.dateFormatter.trim().equals("")) {
                    throw new UnsupportedEncodingException("Date formatter is Missing.");
                }
            }

        }
    }

    /**
     * Used to parse log line based on starting sequence, ending sequence and delimiter.
     *
     * @param line
     * @return
     */
    private ServiceLogRecord parse(String line) {
        ServiceLogRecord record = new ServiceLogRecord(level, "");
        if (startSequence != null || !startSequence.trim().equals(""))
            line = line.substring(startSequence.length());
        if (endSequence != null || !endSequence.trim().equals(""))
            line = line.substring(0, line.indexOf(endSequence));

        StringTokenizer tokenizer = new StringTokenizer(line, delimiter);
        for (int i = 0; tokenizer.hasMoreTokens(); i++) {
            String nextToken = tokenizer.nextToken();
            set(parameters[i], nextToken, record);
        }
        return record;
    }

    /**
     * Used to parse log line based on starting LB file configuration.
     *
     * @param line
     * @return
     */
    private ServiceLogRecord parseLB(String line) {
        ServiceLogRecord record = new ServiceLogRecord(level, "");

        for (int i = 0; i<parameters.length; i++) {
            if(line.contains(startSequence) && line.contains(endSequence)) {
                String vlaue = line.trim().substring(line.trim().indexOf(startSequence)+1,line.trim().indexOf(endSequence));
                set(parameters[i], vlaue, record);
                line = line.trim().substring(line.indexOf(endSequence)+1,line.trim().length());
            } else {
                set(parameters[i], line, record);
            }
        }
        return record;
    }

    /**
     * Used to set values under ServiceLogRecord object for each log lines.
     *
     * @param key
     * @param value
     * @param serviceLogRecord
     */
    private void set(String key, String value, ServiceLogRecord serviceLogRecord) {
        if (value != null) {
            if (key.equals(LEVEL)) {
                if (value.equalsIgnoreCase("ALL"))
                    serviceLogRecord.setLevel(Level.ALL);
                else if (value.equalsIgnoreCase("CONFIG"))
                    serviceLogRecord.setLevel(Level.CONFIG);
                else if (value.equalsIgnoreCase("FINE"))
                    serviceLogRecord.setLevel(Level.FINE);
                else if (value.equalsIgnoreCase("FINER"))
                    serviceLogRecord.setLevel(Level.FINER);
                else if (value.equalsIgnoreCase("FINEST"))
                    serviceLogRecord.setLevel(Level.FINEST);
                else if (value.equalsIgnoreCase("OFF"))
                    serviceLogRecord.setLevel(Level.OFF);
                else if (value.equalsIgnoreCase("SEVERE"))
                    serviceLogRecord.setLevel(Level.SEVERE);
                else if (value.equalsIgnoreCase("WARNING"))
                    serviceLogRecord.setLevel(Level.WARNING);
                else
                    serviceLogRecord.setLevel(Level.INFO);
            } else if (key.equals(LOGGERNAME)) {
                serviceLogRecord.setLoggerName(value);
            } else if (key.equals(DATETIME)) {
                SimpleDateFormat dateFormat = new SimpleDateFormat(dateFormatter);
                Date date = null;
                try {
                    date = dateFormat.parse(value);
                } catch (ParseException ex) {
                    logger.warning("Error while parsing date: " + ex);
                }
                serviceLogRecord.setMillis(date.getTime());
            } else if (key.equals(SEQUENCENUMBER)) {
                serviceLogRecord.setSequenceNumber(Long.parseLong(value));
            } else if (key.equals(MESSAGE)) {
                serviceLogRecord.setMessage(value);
            } else if (key.equals(SOURCECLASSNAME)) {
                serviceLogRecord.setSourceClassName(value);
            } else if (key.equals(SOURCEMETHODNAME)) {
                serviceLogRecord.setSourceMethodName(value);
            } else if (key.equals(THREADID)) {
                serviceLogRecord.setThreadID(Integer.parseInt(value));
            } else if (key.equals(THROWN)) {
                serviceLogRecord.setThrown(new Throwable(value));
            } else if (key.equals(OTHER)) {
                Object[] myObject = serviceLogRecord.getParameters();
                if (myObject == null)
                    myObject = new Object[0];
                myObject = addElement(myObject, value);
                serviceLogRecord.setParameters(myObject);
            }
        }
    }

    /**
     * Add values to the array run time.
     *
     * @param org
     * @param added
     * @return
     */
    private Object[] addElement(Object[] org, Object added) {
        Object[] result = Arrays.copyOf(org, org.length + 1);
        result[org.length] = added;
        return result;
    }

    /**
     * Reset all values for cleaning purpose.
     */
    private void reset() {
        setStartSequence(null).setEndSequence(null).setDelimiter(null).
                setLogFile(null).setLogLines(null).setParameters(null).setLevel(null);
    }

    public ServiceLogRecordBuilder setStartSequence(String startSequence) {
        this.startSequence = startSequence;
        return this;
    }

    public ServiceLogRecordBuilder setEndSequence(String endSequence) {
        this.endSequence = endSequence;
        return this;
    }

    public ServiceLogRecordBuilder setDelimiter(String delimiter) {
        this.delimiter = delimiter;
        return this;
    }

    public ServiceLogRecordBuilder setLogFile(File logFile) {
        this.logFile = logFile;
        return this;
    }

    public ServiceLogRecordBuilder setLogLines(List logLines) {
        this.logLines = logLines;
        return this;
    }

    public ServiceLogRecordBuilder setParameters(String[] parameters) {
        this.parameters = parameters;
        return this;
    }

    public ServiceLogRecordBuilder setLevel(Level level) {
        this.level = level;
        return this;
    }

    public ServiceLogRecordBuilder setDateFormatter(String dateFormatter) {
        this.dateFormatter = dateFormatter;
        return this;
    }

    public ServiceLogRecordBuilder setLB(boolean isLB) {
        this.isLB = isLB;
        return this;
    }

    /**
     * Get list of log lines from given log files.
     *
     * @return
     */
    private List<String> getLogLines() {
        BufferedReader reader = null;
        try {
            logLines = new ArrayList<String>();
            reader = new BufferedReader(
                    new InputStreamReader(new DataInputStream(new FileInputStream(logFile))));
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(startSequence)) {
                    logLines.add(line);
                }
            }
        } catch (Exception ex) {
            logger.warning("Error reading log File: " + ex);
        } finally {
            try {
                if (reader != null)
                    reader.close();
            } catch (IOException ex) {
                logger.warning("Error while closing buffer reader" + ex);
            }
        }

        return logLines;
    }


    public static void main(String args[]) {
        ServiceLogRecordBuilder serviceLogRecordBuilder = new ServiceLogRecordBuilder();
        /*serviceLogRecordBuilder.setLogFile(new File("/home/naman/Desktop/server.log"));
        serviceLogRecordBuilder.setStartSequence("[#|");
        serviceLogRecordBuilder.setEndSequence("|#]");
        serviceLogRecordBuilder.setDelimiter("|");
        serviceLogRecordBuilder.setLevel(Level.INFO);
        serviceLogRecordBuilder.setDateFormatter("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

        String[] myPara = {serviceLogRecordBuilder.DATETIME, serviceLogRecordBuilder.LEVEL, serviceLogRecordBuilder.OTHER,
                serviceLogRecordBuilder.LOGGERNAME, serviceLogRecordBuilder.OTHER, serviceLogRecordBuilder.MESSAGE};

        serviceLogRecordBuilder.setParameters(myPara);*/

        serviceLogRecordBuilder.setLogFile(new File("/home/naman/Desktop/error_log"));
        serviceLogRecordBuilder.setStartSequence("[");
        serviceLogRecordBuilder.setEndSequence("]");
        serviceLogRecordBuilder.setLevel(Level.INFO);
        serviceLogRecordBuilder.setDateFormatter("EEE MMM d HH:mm:ss yyyy");
        serviceLogRecordBuilder.setLB(true);
        String[] myPara = {serviceLogRecordBuilder.DATETIME, serviceLogRecordBuilder.LEVEL, serviceLogRecordBuilder.MESSAGE};
        serviceLogRecordBuilder.setParameters(myPara);

        try {
            serviceLogRecordBuilder.build();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }


}
