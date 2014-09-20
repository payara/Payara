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

package com.sun.enterprise.web;

import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.config.serverbeans.VirtualServer;
import com.sun.enterprise.web.accesslog.AccessLogFormatter;
import com.sun.enterprise.web.accesslog.CombinedAccessLogFormatterImpl;
import com.sun.enterprise.web.accesslog.CommonAccessLogFormatterImpl;
import com.sun.enterprise.web.accesslog.DefaultAccessLogFormatterImpl;
import com.sun.enterprise.web.pluggable.WebContainerFeatureFactory;
import com.sun.enterprise.util.io.FileUtils;
import org.apache.catalina.*;
import org.apache.catalina.valves.ValveBase;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.logging.annotation.LogMessageInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.String;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>Implementation of the <b>Valve</b> interface that generates a web server
 * access log with the detailed line contents matching a configurable pattern.
 * The syntax of the available patterns is similar to that supported by the
 * Apache <code>mod_log_config</code> module.  As an additional feature,
 * automatic rollover of log files at a specified interval is also supported.
 *
 * </p>This class uses a direct <code>ByteBuffer</code> to store and write 
 * logs. 
 *
 * @author Jean-Francois Arcand
 * @author Charlie J. Hunt
 */

public final class PEAccessLogValve
    extends ValveBase
    implements Runnable {

    private static final Logger _logger = com.sun.enterprise.web.WebContainer.logger;

    private static final ResourceBundle _rb = _logger.getResourceBundle();

    @LogMessageInfo(
            message = "Unable to write access log file {0}",
            level = "SEVERE",
            cause = "An exception occurred writing to access log file",
            action = "Check the exception for the error")
    public static final String ACCESS_LOG_UNABLE_TO_WRITE = "AS-WEB-GLUE-00098";

    @LogMessageInfo(
            message = "Setting accesslog directory for virtual server '{0}' to {1}",
            level = "FINE")
    public static final String ACCESS_LOG_DIRECTORY_SET = "AS-WEB-GLUE-00099";

    @LogMessageInfo(
            message = "Invalid accessLogWriterInterval value [{0}]",
            level = "WARNING")
    public static final String INVALID_ACCESS_LOG_WRITER_INTERVAL = "AS-WEB-GLUE-00100";

    @LogMessageInfo(
            message = "Invalid accessLogBufferSize value [{0}]",
            level = "WARNING")
    public static final String INVALID_ACCESS_LOG_BUFFER_SIZE = "AS-WEB-GLUE-00101";

    @LogMessageInfo(
            message = "Unable to parse max-history-files access log configuration [{0}]",
            level = "WARNING")
    public static final String INVALID_MAX_HISTORY_FILES = "AS-WEB-GLUE-00102";

    @LogMessageInfo(
            message = "Unable to create {0}",
            level = "WARNING")
    public static final String UNABLE_TO_CREATE = "AS-WEB-GLUE-00103";

    @LogMessageInfo(
            message = "Unable to rename access log file {0} to {1}",
            level = "WARNING")
    public static final String UNABLE_TO_RENAME_LOG_FILE = "AS-WEB-GLUE-00104";

    @LogMessageInfo(
            message = "Unable to remove access log file {0}",
            level = "WARNING")
    public static final String UNABLE_TO_REMOVE_LOG_FILE = "AS-WEB-GLUE-00105";

    @LogMessageInfo(
            message = "Access logger has already been started",
            level = "WARNING")
    public static final String ACCESS_LOG_ALREADY_STARTED = "AS-WEB-GLUE-00106";

    @LogMessageInfo(
            message = "Access logger has not yet been started",
            level = "WARNING")
    public static final String ACCESS_LOG_NOT_STARTED = "AS-WEB-GLUE-00107";


    // Predefined patterns
    private static final String COMMON_PATTERN = "common";
    private static final String COMBINED_PATTERN = "combined";

    /**
     * Name of the system property whose value specifies the max number of
     * access log history files to keep.
     * If this property has been specified without any value, a default value
     * of 10 is used.
     * Else, if it has been specified with a value of 0, no access log
     * history files will be maintained, and the current access log file will
     * be reset after each rotation.
     * If undefined, all access log history files will be preserved.
     */
    private static final String LOGGING_MAX_HISTORY_FILES = 
        "com.sun.enterprise.server.logging.max_history_files";

    
    /**
     * The minimum size a buffer can have.
     */
    private final static int MIN_BUFFER_SIZE = 5120;
    
    
    // ----------------------------------------------------- Instance Variables


    /**
     * The directory in which log files are created.
     */
    private String directory = "logs";


    /**
     * The descriptive information about this implementation.
     */
    private static final String info =
        "com.sun.enterprise.web.PEAccessLogValve/1.0";


    /**
     * The prefix that is added to log file filenames.
     */
    private String prefix = "";


    /**
     * Should we rotate our log file?
     */
    private boolean rotatable;


    /**
     * The suffix that is added to log file filenames.
     */
    private String suffix = "";


    /**
     * If prefix ends in '.', and suffix starts with '.', we must remove the
     * leading '.' from suffix when prefix and suffix are concatenated.
     */
    private boolean removeLeadingDotFromSuffix = false;


    /**
     * Suffix from which leading '.' has been removed if
     * removeLeadingDotFromSuffix is true
     */
    private String dotLessSuffix = null;


    /**
     * ThreadLocal for a date formatter to format a Date into a date in the format
     * "yyyy-MM-dd".
     */
    private volatile ThreadLocal<SimpleDateFormat> dateFormatter = null;


    /**
     * Resolve hosts.
     */
    private boolean resolveHosts = false;


    /**
     * Instant when the log daily rotation was last checked.
     */
    private long lastAccessLogCreationTime = 0L;


    /**
     * Are we doing conditional logging. default false.
     */
    private String condition = null;


    /**
     * Date format to place in log file name. Use at your own risk!
     */
    private String fileDateFormat = null;
    
    
    /**
     * The <code>FileChannel</code> used to write the access log.
     */
    private FileChannel fileChannel;
    
    
    /**
     * The stream used to store the logs.
     */
    FileOutputStream fos;
    
    
    /**
     * The interval (in seconds) between writing the logs
     */
    private int writeInterval = 0;

    
    /**
     * The interval between rotating the logs
     */
    private int rotationInterval;
    
    
    /**
     * The background writerThread.
     */
    private Thread writerThread = null;   
    
    
    /**
     * The background writerThread completion semaphore.
     */
    private boolean threadDone = false;

    
    /**
     * The <code>CharBuffer</code> used to store the logs.
     */
    private CharBuffer charBuffer;
   
    
    /**
     * The <code>byteBuffer</code> used to store the log.
     */
    private int bufferSize = MIN_BUFFER_SIZE;
        
    
    /**
     * If the writer interval is equals to zero, then always flush the 
     * direct byte buffer after every request.
     */
    private boolean flushRealTime = true;
    

    /**
     * Are we supposed to add datestamp to first access log file we create,
     * or only after first rotation?
     *
     * If set to false, the current access log file will never have any
     * date stamp: It will be moved to a date-stamped file upon rotation
     */
    private boolean addDateStampToFirstAccessLogFile;


    /**
     * The current access log file.
     */
    private File logFile;


    /**
     * The maximum number of access log history files to keep
     */
    private int maxHistoryFiles;


    /**
     * True if no access log history files are to be kept, false otherwise
     */
    private boolean deleteAllHistoryFiles;


    /**
     * List of most recent access log history files (the size of this list
     * is not to exceed <code>maxHistoryFiles</code>)
     */
    private LinkedList<File> historyFiles;


    /**
     * The access log formatter
     */
    private AccessLogFormatter formatter;
    
    
    /**
     * Simple lock
     */
    private Object lock = new Object();
    

    /**
     * Return writerThread interval (seconds)
     */
    public int getWriterInterval() {        
        return writeInterval;       
    }

        
    /**
     * Set writerthread interval (seconds)
     */
    public void setWriterInterval(int t) {
        if ( t > 0 ){
            flushRealTime = false;
        }
        writeInterval = t;
    }
    
    
    /**
     * Return rotation interval
     */
    public int geRotationInterval() {        
        return rotationInterval;       
    }

        
    /**
     * Set rotation interval 
     */
    public void setRotationInterval(int t) {
        rotationInterval = t;
    }    

    
    /**
     * Set the direct <code>ByteBuffer</code> size
     */
    public void setBufferSize(int size){
        if ( size > 0 ){
            flushRealTime = false;
        }
        if (size < MIN_BUFFER_SIZE) {
            bufferSize = MIN_BUFFER_SIZE;
        } else {
            bufferSize = size;
        }
    }
    
    /**
     * Return the direct <code>ByteBuffer</code> size
     */    
    public int getBufferSize(){
        return bufferSize;
    }

    // ------------------------------------------------------------- Properties


    /**
     * Are we supposed to add datestamp to first access log file we create,
     * or only starting with first rotation?
     */
    public void setAddDateStampToFirstAccessLogFile(boolean add) {
        addDateStampToFirstAccessLogFile = add;
    }


    /**
     * Return the directory in which we create log files.
     */
    public String getDirectory() {

        return directory;

    }


    /**
     * Set the directory in which we create log files.
     *
     * @param directory The new log file directory
     */
    public void setDirectory(String directory) {
        this.directory = directory;

    }


    /**
     * Return descriptive information about this implementation.
     */
    public String getInfo() {

        return info;

    }


    /**
     * Set the format pattern, first translating any recognized alias.
     *
     * @param p The new pattern
     */
    public void setPattern(String p) {
        if (COMMON_PATTERN.equalsIgnoreCase(p)) {
            formatter = new CommonAccessLogFormatterImpl();
        } else if (COMBINED_PATTERN.equalsIgnoreCase(p)) {
            formatter = new CombinedAccessLogFormatterImpl();
        } else {
            formatter = new DefaultAccessLogFormatterImpl(p, getContainer());
        }
    }


    /**
     * Return the log file prefix.
     */
    public String getPrefix() {

        return prefix;

    }


    /**
     * Set the log file prefix.
     *
     * @param p prefix The new log file prefix
     */
    public void setPrefix(String p) {

        prefix = p;

        if (prefix != null && suffix != null && prefix.endsWith(".")
                && suffix.startsWith(".")) {
            removeLeadingDotFromSuffix = true;
            dotLessSuffix = suffix.substring(1);
        } else {
            removeLeadingDotFromSuffix = false;
        }
    }


    /**
     * Should we rotate the logs
     */
    public boolean isRotatable() {

        return rotatable;

    }


    /**
     * Set the value is we should we rotate the logs
     *
     * @param rotatable true is we should rotate.
     */
    public void setRotatable(boolean rotatable) {

        this.rotatable = rotatable;

    }


    /**
     * Return the log file suffix.
     */
    public String getSuffix() {

        return suffix;

    }


    /**
     * Set the log file suffix.
     *
     * @param s suffix The new log file suffix
     */
    public void setSuffix(String s) {

        suffix = s;

        if (prefix != null && suffix != null && prefix.endsWith(".")
                && suffix.startsWith(".")) {
            removeLeadingDotFromSuffix = true;
            dotLessSuffix = suffix.substring(1);
        } else {
            removeLeadingDotFromSuffix = false;
        }
    }


    /**
     * Set the resolve hosts flag.
     *
     * @param resolveHosts The new resolve hosts value
     */
    public void setResolveHosts(boolean resolveHosts) {
        this.resolveHosts = resolveHosts;
    }


    /**
     * Get the value of the resolve hosts flag.
     */
    public boolean isResolveHosts() {

        return resolveHosts;

    }


    /**
     * Return whether the attribute name to look for when
     * performing conditional loggging. If null, every
     * request is logged.
     */
    public String getCondition() {

        return condition;

    }


    /**
     * Set the ServletRequest.attribute to look for to perform
     * conditional logging. Set to null to log everything.
     *
     * @param condition Set to null to log everything
     */
    public void setCondition(String condition) {

        this.condition = condition;

    }

    /**
     *  Return the date format date based log rotation.
     */
    public String getFileDateFormat() {
        return fileDateFormat;
    }


    /**
     *  Set the date format date based log rotation.
     */
    public void setFileDateFormat(String fileDateFormat) {
        this.fileDateFormat =  fileDateFormat;
    }    
    
    
    // --------------------------------------------------------- Public Methods


    /**
     * Log a message summarizing the specified request and response, according
     * to the format specified by the <code>pattern</code> property.
     *
     * @param request Request being processed
     * @param response Response being processed
     */ 
    public int invoke(Request request, Response response) {

        if (formatter!=null && formatter.needTimeTaken()) {
            request.setNote(Constants.REQUEST_START_TIME_NOTE, System.currentTimeMillis());
        }

        return INVOKE_NEXT;
    }

   
    public void postInvoke(Request request, Response response)
            throws IOException {

        if (!started || condition!=null &&
                null!=request.getRequest().getAttribute(condition)) {
             return;
        }
        
        synchronized (lock){
            // Reset properly the buffer in case of an unexpected
            // exception.
            if (charBuffer.position() == charBuffer.limit()){
                charBuffer.limit(charBuffer.capacity());
            }    

            int pos = charBuffer.position();
            // We've flushed our buffer to make room for the current request.
            // Now process the current request
            for (int i=0; i < 2; i++){
                try {
                    if (formatter!=null) {
                        formatter.appendLogEntry(request, response, charBuffer);
                        charBuffer.put("\n");

                        if (flushRealTime){
                            log();
                        }
                        break;
                    }
                 } catch (BufferOverflowException ex) {
                    charBuffer.position(pos);
                    log();
                    
                    if (i+1 == 2){
                        _logger.log(
                            Level.SEVERE,
                            ACCESS_LOG_UNABLE_TO_WRITE,
                            new Object[] {ex});   
                        return;
                    }
                }
            }
        }
    }


    /**
     * Log the specified message to the log file, switching files if the date
     * has changed since the previous log call.
     */
    public void log() throws IOException {
        
        if (rotatable){

            long systime = System.currentTimeMillis();
            long rotationIntervalLong = rotationInterval*1000L;
            if (systime-lastAccessLogCreationTime > rotationIntervalLong) {
                synchronized (this) {
                    systime = System.currentTimeMillis();
                    if (systime-lastAccessLogCreationTime >
                        rotationIntervalLong) {

                        // Rotate only if the formatted datestamps are
                        // different
                        String lastDateStamp = dateFormatter.get().format(
                            new Date(lastAccessLogCreationTime));
                        String newDateStamp = dateFormatter.get().format(
                            new Date(systime));

                        lastAccessLogCreationTime = systime;

                        if (!lastDateStamp.equals(newDateStamp)) {
                            close();
                            open(newDateStamp, false);
                        }
                    }
                }
            }
        }
        
        synchronized(lock){
            try{
                charBuffer.flip();
                ByteBuffer byteBuffer =
                    ByteBuffer.wrap(charBuffer.toString().getBytes(Charset.defaultCharset()));
                while (byteBuffer.hasRemaining()){
                    fileChannel.write(byteBuffer);
                }
            } catch (IOException ex){
                ;
            } finally {
                charBuffer.clear();
            }
        }

    }



    /*
     * Configures this access log valve.
     *
     * @param vs The virtual server instance
     * @param vsId The virtual server id
     * @param vsBean The virtual server bean containing info about the access
     * log
     *
     * @return true if this access log valve needs to be started, false
     * otherwise
     */
    boolean configure(String vsId, VirtualServer vsBean,
        HttpService httpService, Domain domain, ServiceLocator habitat,
        WebContainerFeatureFactory fac, String globalAccessLogBufferSize,
        String globalAccessLogWriteInterval) {

        setPrefix(vsId + fac.getDefaultAccessLogPrefix());

        boolean start = updateVirtualServerProperties(
            vsId, vsBean, domain, habitat, globalAccessLogBufferSize,
            globalAccessLogWriteInterval);
        updateAccessLogAttributes(httpService, fac);

        return start;
    }


    /**
     * Configures this accesslog valve with the accesslog related properties
     * of the given <virtual-server> bean.
     */ 
    boolean updateVirtualServerProperties(
            String vsId,
            VirtualServer vsBean,
            Domain domain,
            ServiceLocator services,
            String accessLogBufferSize,
            String accessLogWriteInterval) {

        /*
         * Determine the virtual server's access log directory, which may be
         * specified in two places:
         *
         * 1.  <virtual-server>
         *       <http-access-log log-directory="..."/>
         *     </virtual-server>
         *
         * 2.  <virtual-server>
         *       <property name="accesslog" value="..."/>
         *     </virtual-server>
         *
         * If both have been specified, the latter takes precedence.
         */
        String accessLog = vsBean.getAccessLog();
        if (accessLog==null && vsBean.getHttpAccessLog() != null) {
            accessLog = vsBean.getHttpAccessLog().getLogDirectory();
        }
        if (accessLog == null) {
            return false;
        }

        File dir = new File(accessLog);
        if (!dir.isAbsolute()) {
            /*
             * If accesslog is relative, turn it into an absolute path by
             * prepending log-root of domain element
             */
            String logRoot = domain.getLogRoot();
            if (logRoot != null) {
                dir = new File(logRoot, accessLog);
            } else {
                ServerEnvironment env = services.getService(ServerEnvironment.class);
                dir = new File(env.getDomainRoot(), accessLog);
            }
        }
            
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE,
                    ACCESS_LOG_DIRECTORY_SET,
                    new Object[]{vsId, dir.getAbsolutePath()});
        }

        setDirectory(dir.getAbsolutePath());

        /*
         * If there is any accessLogWriteInterval property defined under
         * <virtual-server>, it overrides the write-interval-seconds attribute
         * of <http-service><access-log>
         */
        String acWriteInterval = vsBean.getPropertyValue(
            Constants.ACCESS_LOG_WRITE_INTERVAL_PROPERTY,
            accessLogWriteInterval);
        if (acWriteInterval != null) {
            try{
                setWriterInterval(Integer.parseInt(acWriteInterval));
            } catch (NumberFormatException ex){
                _logger.log(Level.WARNING,
                    INVALID_ACCESS_LOG_WRITER_INTERVAL,
                    acWriteInterval);
            }
        }
         
        /*
         * If there is any accessLogBufferSize property defined under
         * <virtual-server>, it overrides the buffer-size-bytes attribute
         * of <http-service><access-log>
         */
        String acBufferSize = vsBean.getPropertyValue(
            Constants.ACCESS_LOG_BUFFER_SIZE_PROPERTY, accessLogBufferSize);
        if (acBufferSize != null) {
            try {
                setBufferSize(Integer.parseInt(acBufferSize));
            } catch (NumberFormatException ex){
                _logger.log(Level.WARNING,
                    INVALID_ACCESS_LOG_BUFFER_SIZE,
                    acBufferSize);
            }
        }

        return true;
    }


    /**
     * Configures this accesslog valve with the accesslog related
     * attributes of the domain.xml's <http-service> and <access-log>
     * elements.
     */
    void updateAccessLogAttributes(HttpService httpService,
        WebContainerFeatureFactory fac) {

        setResolveHosts(false);
        AccessLog accessLogConfig = httpService.getAccessLog();

        // access-log format
        String format = null;
        if (accessLogConfig != null) {
            format = accessLogConfig.getFormat();
        } else {
	    format = ConfigBeansUtilities.getDefaultFormat();
        }
        setPattern(format);

        // write-interval-seconds
        int interval = 0;
        if (accessLogConfig != null) {
            String s = accessLogConfig.getWriteIntervalSeconds();
            interval = Integer.parseInt(s); 
            setWriterInterval(interval);
        }
                       
        // rotation-enabled
        if (accessLogConfig != null) {
            setRotatable(Boolean.valueOf(accessLogConfig.getRotationEnabled()));
        } else {
            setRotatable(Boolean.valueOf(
                ConfigBeansUtilities.getDefaultRotationEnabled()));
        }
        // rotation-interval
        interval = 0;
        if (accessLogConfig != null) {
            String s = accessLogConfig.getRotationIntervalInMinutes();
            interval = Integer.parseInt(s) * 60;
        } else {
            interval = Integer.parseInt(ConfigBeansUtilities.getDefaultRotationIntervalInMinutes()) * 60;
        }
        setRotationInterval(interval);

        // rotation-datestamp
        String rotationDateStamp = null;
        if (accessLogConfig != null) {
            rotationDateStamp = accessLogConfig.getRotationSuffix();
        } else {
            rotationDateStamp = fac.getDefaultAccessLogDateStampPattern();
        }
        if ("%YYYY;%MM;%DD;-%hh;h%mm;m%ss;s".equals(rotationDateStamp)) {
            /*
             * Modify the default rotation suffix pattern specified in the
             * sun-domain DTD in such a way that it is accepted by
             * java.text.SimpleDateFormat. We support only those patterns
             * accepted by java.text.SimpleDateFormat.
             */
            rotationDateStamp = "yyyyMMdd-HH'h'mm'm'ss's'";
        }
        setFileDateFormat(rotationDateStamp);

        // rotation-suffix
        setSuffix(fac.getDefaultAccessLogSuffix());

        setAddDateStampToFirstAccessLogFile(
            fac.getAddDateStampToFirstAccessLogFile());

        // max-history-files
        deleteAllHistoryFiles = false;
        historyFiles = null;
        maxHistoryFiles = 10;
        String prop = System.getProperty(LOGGING_MAX_HISTORY_FILES);
        if (prop != null) {
            if (!"".equals(prop)) {
                try {
                    maxHistoryFiles = Integer.parseInt(prop);
                } catch (NumberFormatException e) {
                    String msg = MessageFormat.format(_rb.getString(INVALID_MAX_HISTORY_FILES), prop);
                    _logger.log(Level.WARNING, msg, e);   
                }
            }
        } else {
            try {
                maxHistoryFiles = Integer.parseInt(
                    accessLogConfig.getMaxHistoryFiles());
            } catch (NumberFormatException e) {
                String msg = MessageFormat.format(_rb.getString(INVALID_MAX_HISTORY_FILES),
                    accessLogConfig.getMaxHistoryFiles());
                _logger.log(Level.WARNING, msg, e);   
            }
        }
        if (maxHistoryFiles == 0) {
            deleteAllHistoryFiles = true;
        } else if (maxHistoryFiles > 0) {
            historyFiles = new LinkedList<File>();
        }
    }


    // -------------------------------------------------------- Private Methods


    /**
     * Close the currently open log file (if any)
     */
    private synchronized void close() {

        try{            
            // Make sure the byteBuffer is clean
            log();
            fileChannel.close();
            fos.close();
        } catch (IOException ex){
            ;
        }
    }

    
    /**
     * Open new access log file.
     *
     * @param dateStamp The date stamp of the new access log file (if log
     * rotation has been enabled)
     * @param firstAccessLogFile true if we are creating our first access log
     * file, and false if we have rotated
     */
    private synchronized void open(String dateStamp,
                                   boolean firstAccessLogFile)
            throws IOException {
        
        // Create the directory if necessary
        File dir = new File(directory);
        if (!dir.isAbsolute())
            dir = new File(System.getProperty("catalina.base"), directory);
        if (!FileUtils.mkdirsMaybe(dir)) {
            _logger.log(Level.WARNING,
                    UNABLE_TO_CREATE,
                    dir.toString());
        }

        // Open the current log file
        try {
            String pathname;
            // If no rotate - no need for dateStamp in fileName
            if (rotatable && addDateStampToFirstAccessLogFile) {
                pathname = dir.getAbsolutePath() + File.separator +
                            prefix + dateStamp + suffix;
            } else {
                if (removeLeadingDotFromSuffix) {
                    pathname = dir.getAbsolutePath() + File.separator +
                               prefix + dotLessSuffix;
                } else {
                    pathname = dir.getAbsolutePath() + File.separator +
                               prefix + suffix;
                }
            }
            
            if (rotatable
                    && !addDateStampToFirstAccessLogFile
                    && !firstAccessLogFile) {
                // Move current access log file, which has no date stamp,
                // to date-stamped file
                String dateStampedPathname = dir.getAbsolutePath()
                                        + File.separator
                                        + prefix + dateStamp + suffix;
                File renameToFile = new File(dateStampedPathname);
                if (!logFile.renameTo(renameToFile)) {
                    _logger.log(
                        Level.WARNING,
                            UNABLE_TO_RENAME_LOG_FILE,
                            new Object[] {logFile.toString(), dateStampedPathname });
                }
                File removeFile = null;
                if (deleteAllHistoryFiles) {
                    removeFile = renameToFile;
                } else {
                    if (historyFiles != null) {
                        historyFiles.addLast(renameToFile);
                        if (historyFiles.size() > maxHistoryFiles) {
                            removeFile = historyFiles.removeFirst();
                        }
                    }
                }
                if (removeFile != null && !removeFile.delete()) {
                    _logger.log(Level.WARNING,
                                UNABLE_TO_REMOVE_LOG_FILE,
                                removeFile.toString());
                }
            }

            // Open the file and then get a channel from the stream
            logFile = new File(pathname);
            fos = new FileOutputStream(logFile, true);
            fileChannel = fos.getChannel();

        } catch (IOException ioe) {
            try {
                if ( fileChannel != null ) {
                    fileChannel.close();
                }
            } catch (IOException e){
                ;
            }
            
            // Rethrow IOException
            throw ioe;
        } 

    }


    // ------------------------------------------------------ Lifecycle Methods


    /**
     * Add a lifecycle event listener to this component.
     *
     * @param listener The listener to add
     */
    public void addLifecycleListener(LifecycleListener listener) {
        lifecycle.addLifecycleListener(listener);
    }


    /**
     * Gets the (possibly empty) list of lifecycle listeners associated
     * with this PEAccessLogValve.
     */
    public List<LifecycleListener> findLifecycleListeners() {
        return lifecycle.findLifecycleListeners();
    }


    /**
     * Remove a lifecycle event listener from this component.
     *
     * @param listener The listener to add
     */
    public void removeLifecycleListener(LifecycleListener listener) {
        lifecycle.removeLifecycleListener(listener);
    }


    /**
     * Prepare for the beginning of active use of the public methods of this
     * component.  This method should be called after <code>configure()</code>,
     * and before any of the public methods of the component are utilized.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that prevents this component from being used
     */
    public void start() throws LifecycleException {

        // Validate and update our current component state
        if (started) {
            throw new LifecycleException(_rb.getString(ACCESS_LOG_ALREADY_STARTED));
        }

        lifecycle.fireLifecycleEvent(START_EVENT, null);

        if (bufferSize <= MIN_BUFFER_SIZE) {
            bufferSize = MIN_BUFFER_SIZE;
        }

        charBuffer = CharBuffer.allocate(bufferSize);

        // Initialize the timeZone, Date formatters, and currentDate
        final TimeZone tz = TimeZone.getDefault();

        if (fileDateFormat==null || fileDateFormat.length()==0)
            fileDateFormat = "yyyy-MM-dd";
        dateFormatter = new ThreadLocal<SimpleDateFormat>() {
            @Override
            protected SimpleDateFormat initialValue() {
                SimpleDateFormat f = new SimpleDateFormat(fileDateFormat);
                f.setTimeZone(tz);
                return f;
            }
        };

        long systime = System.currentTimeMillis();
        try {
            open(dateFormatter.get().format(new Date(systime)), true);
        } catch (IOException ioe) {
            throw new LifecycleException(ioe);
        }

        lastAccessLogCreationTime = systime;

        if (!flushRealTime){
            // Start the background writer writerThread
            threadStart();
        }

        started = true;
   }


    /**
     * Gracefully terminate the active use of the public methods of this
     * component.  This method should be the last one called on a given
     * instance of this component.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that needs to be reported
     */
    public void stop() throws LifecycleException {

        // Validate and update our current component state
        if (!started) {
            throw new LifecycleException(_rb.getString(ACCESS_LOG_NOT_STARTED));
        }

        lifecycle.fireLifecycleEvent(STOP_EVENT, null);
        started = false;
        
        if (!flushRealTime){
            // Stop the background writer thread
            threadStop();
        }
        
        close();
    }

    
   /**
     * The background writerThread that checks for write the log.
     */
    public void run() {

        // Loop until the termination semaphore is set
        while (!threadDone) {
            threadSleep();
            try {
                log();
            } catch (IOException ioe) {
                threadDone = true;
            }
        }

    }
     

    /**
     * Sleep for the duration specified by the <code>writeInterval</code>
     * property.
     */
    private void threadSleep() {
        
        if (writerThread == null || writeInterval == 0)
            return;
        
        try {
            writerThread.sleep(writeInterval * 1000L);
        } catch (InterruptedException e) {
            ;
        }

    }

        
   /**
     * Start the background writerThread that will periodically write access log
     */
    private void threadStart() {

        if (writerThread != null || writeInterval == 0)
            return;

        threadDone = false;
        String threadName = "AccessLogWriter";
        writerThread = new Thread(this, threadName);
        writerThread.setDaemon(true);
        writerThread.start();

    }


    /**
     * Stop the background writerThread that is periodically write logs
     */
    private void threadStop() {

        if (writerThread == null || writeInterval == 0)
            return;

        threadDone = true;
        writerThread.interrupt();
        try {
            writerThread.join();
        } catch (InterruptedException e) {
            ;
        }

        writerThread = null;

    }
}
