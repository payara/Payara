/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 Oracle and/or its affiliates. All rights reserved.
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

// Portions Copyright [2014-2019] [Payara Foundation and/or its affiliates]
 
package com.sun.common.util.logging;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;

import org.glassfish.api.admin.FileMonitoring;
import org.glassfish.server.ServerEnvironmentImpl;
import org.jvnet.hk2.annotations.Contract;
import org.jvnet.hk2.annotations.Service;

/**
 * Implementation of Logging Commands
 *
 * @author Naman Mehta
 */

@Service
@Contract
public class LoggingConfigImpl implements LoggingConfig {
  
    static final String GF_FILE_HANDLER = "com.sun.enterprise.server.logging.GFFileHandler";
    static final String PY_FILE_HANDLER = "fish.payara.enterprise.server.logging.PayaraNotificationFileHandler";

    public static final Map<String, String> DEFAULT_LOG_PROPERTIES = new HashMap<>();
    static {
        DEFAULT_LOG_PROPERTIES.put(GF_FILE_HANDLER + ".logtoFile", "true");
        DEFAULT_LOG_PROPERTIES.put(PY_FILE_HANDLER + ".logtoFile", "true");
        DEFAULT_LOG_PROPERTIES.put(PY_FILE_HANDLER + ".rotationOnDateChange", "false");
        DEFAULT_LOG_PROPERTIES.put(PY_FILE_HANDLER + ".rotationTimelimitInMinutes", "0");
        DEFAULT_LOG_PROPERTIES.put(GF_FILE_HANDLER + ".rotationLimitInBytes", "2000000");
        DEFAULT_LOG_PROPERTIES.put(PY_FILE_HANDLER + ".rotationLimitInBytes", "2000000");
        DEFAULT_LOG_PROPERTIES.put(PY_FILE_HANDLER + ".maxHistoryFiles", "0");
        DEFAULT_LOG_PROPERTIES.put(PY_FILE_HANDLER + ".file", "${com.sun.aas.instanceRoot}/logs/notification.log");
        DEFAULT_LOG_PROPERTIES.put(PY_FILE_HANDLER + ".compressOnRotation", "false");
        DEFAULT_LOG_PROPERTIES.put(PY_FILE_HANDLER + ".formatter", "com.sun.enterprise.server.logging.ODLLogFormatter");
    }

    @Inject
    private FileMonitoring fileMonitoring;

    private final Properties props = new Properties();
    private final String logFileName;
    private final File loggingConfigDir;
    private final File defaultLogFile;
    private File logFile;

    @Inject
    public LoggingConfigImpl(ServerEnvironmentImpl env) {
        this(env.getConfigDirPath(), env.getConfigDirPath());
    }

    public LoggingConfigImpl(File defaultConfigDir, File configDir) {
        this.loggingConfigDir = configDir;
        this.logFileName = ServerEnvironmentImpl.kLoggingPropertiesFileName;
        this.defaultLogFile = new File(defaultConfigDir,
                ServerEnvironmentImpl.kDefaultLoggingPropertiesFileName);
        this.logFile = new File(defaultConfigDir,
                ServerEnvironmentImpl.kLoggingPropertiesFileName);
    }

    @Override
    public void initialize(String target) throws IOException {
        this.logFile = new File(loggingConfigDir,
                target + "/" + logFileName);
    }

    /**
     * Load the properties for the target.
     */
    private void loadLoggingProperties() throws IOException {
        props.clear();
        InputStream fis = null;
        try {
            fis = getLoggingFileInputStream();
            props.load(fis);
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
    }
    
    private InputStream getLoggingFileInputStream() throws IOException {
        InputStream fileInputStream;
        if (!logFile.exists()) {
            fileInputStream = new FileInputStream(defaultLogFile);
        } else {
            fileInputStream = new FileInputStream(logFile);
        }
        return new BufferedInputStream(fileInputStream);
    }

    private void closePropFile() throws IOException {
        File parentFile = logFile.getParentFile();
        if (!parentFile.exists() && !parentFile.mkdirs()) {
            throw new IOException();
        }
        OutputStream os = null;
        try {
            os = new BufferedOutputStream(new FileOutputStream(logFile));
            props.store(os, "GlassFish logging.properties list");
            os.flush();
            fileMonitoring.fileModified(logFile);
        } catch (Exception e) {
            // nothing can be done about it...
        } finally {
            os.close();
        }
    }

    private void setWebLoggers(String value) {
        // set the rest of the web loggers to the same level
        // these are only accessible via the web-container name so all values should be the same
        props.setProperty("org.apache.catalina.level", value);
        props.setProperty("org.apache.coyote.level", value);
        props.setProperty("org.apache.jasper.level", value);
    }

    @Override
    public synchronized String setLoggingProperty(String propertyName, String propertyValue) throws IOException {
        loadLoggingProperties();
        // update the property
        if (propertyValue == null) return null;
        // may need to map the domain.xml name to the new name in logging.properties file
        String key = LoggingXMLNames.xmltoPropsMap.get(propertyName);
        if (key == null) {
            key = propertyName;
        }
        String property = (String) props.setProperty(key, propertyValue);
        if (propertyName.contains("javax.enterprise.system.container.web")) {
            setWebLoggers(propertyValue);
        }

        closePropFile();
        return property;
    }

    @Override
    public synchronized Map<String, String> setLoggingProperties(Map<String, String> properties) throws IOException {
        loadLoggingProperties();
        checkForLoggingProperties(properties);
        // need to map the name given to the new name in logging.properties file
        Map<String, String> m = getMap(properties);
        closePropFile();
        return m;
    }

    private Map<String, String> getMap(Map<String, String> properties) {
        Map<String, String> m = new HashMap<>();
        for (Map.Entry<String, String> e : properties.entrySet()) {
            if (e.getValue() == null) continue;
            String key = LoggingXMLNames.xmltoPropsMap.get(e.getKey());
            if (key == null) {
                key = e.getKey();
            }
            String property = (String) props.setProperty(key, e.getValue());
            if (e.getKey().contains("javax.enterprise.system.container.web")) {
                setWebLoggers(e.getValue());
            }

            //build Map of entries to return
            m.put(key, property);

        }
        return m;
    }

    @Override
    public String getLoggingProperty(String propertyName) throws IOException {
        return getLoggingProperties().get(propertyName);
    }

    /* Return a Map of all the properties and corresponding values in the logging.properties file.
      * @throws  IOException
      */

    public synchronized Map<String, String> getLoggingProperties() throws IOException {
        loadLoggingProperties();
        Enumeration<?> e = props.propertyNames();
        Map<String, String> m = getMap(e);
        return checkForLoggingProperties(m);
    }

    private Map<String, String> getMap(Enumeration<?> e) {
        Map<String, String> m = new HashMap<>();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            // convert the name in domain.xml to the name in logging.properties if needed
            if (LoggingXMLNames.xmltoPropsMap.get(key) != null) {
                key = LoggingXMLNames.xmltoPropsMap.get(key);
            }
            m.put(key, props.getProperty(key));
        }
        return m;
    }

    public synchronized Map<String, String> checkForLoggingProperties(Map<String, String> loggingProperties) throws IOException {

        for (Entry<String, String> entry : DEFAULT_LOG_PROPERTIES.entrySet()) {
            if (!loggingProperties.containsKey(entry.getKey())) {
                loggingProperties.put(entry.getKey(), entry.getValue());
                setLoggingProperty(entry.getKey(), entry.getValue());
            }
        }
           
        return loggingProperties;
    }

    @Override
    public synchronized Map<String, String> deleteLoggingProperties(Map<String, String> properties) throws IOException {
        loadLoggingProperties();

        // need to map the name given to the new name in logging.properties file
        for (Map.Entry<String, String> e : properties.entrySet()) {
            String key = LoggingXMLNames.xmltoPropsMap.get(e.getKey());
            if (key == null) {
                key = e.getKey();
            }
            
            if (key.contains("\\:")) {
                key = key.replace("\\:", ":");
            }
            
            props.remove(key);
        }

        closePropFile();
        checkForLoggingProperties(getLoggingProperties());
        return properties;
    }

    /*
      * Returns the zip File Name to create for collection log files
      *
      * @param sourceDir Directory underneath zip file should be created.
      *
      */

    private String getZipFileName(String sourceDir) {

        final String DATE_FORMAT_NOW = "yyyy-MM-dd_HH-mm-ss";

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
        String currentTime = sdf.format(cal.getTime());

        String zipFile = sourceDir + File.separator + "log_" + currentTime + ".zip";

        return zipFile;
    }

    /*
      * Returns the zip File Name to create for collection log files
      *
      * @param sourceDir Directory underneath zip file should be created.
      * @param fileName file name for zip file
      */

    private String getZipFileName(String sourceDir, String fileName) {

        final String DATE_FORMAT_NOW = "yyyy-MM-dd_HH-mm-ss";

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
        String currentTime = sdf.format(cal.getTime());

        String zipFile = sourceDir + File.separator + fileName + "-" + currentTime + ".zip";

        return zipFile;
    }

    /*
      * Creating zip file for given log files
      *
      * @param sourceDir Source directory from which needs to create zip
      *
      * @throws  IOException
      */

    public String createZipFile(String sourceDir) throws IOException {
        ZipOutputStream zout = null;
        String zipFile = getZipFileName(sourceDir);
        try {
            //create object of FileOutputStream
            FileOutputStream fout = new FileOutputStream(zipFile);

            //create object of ZipOutputStream from FileOutputStream
            zout = new ZipOutputStream(fout);

            //create File object from source directory
            File fileSource = new File(sourceDir);

            addDirectory(zout, fileSource,
                    fileSource.getAbsolutePath().length() + 1);

            //close the ZipOutputStream
            zout.close();
        }
        catch (IOException ioe) {
            Logger.getAnonymousLogger().log(Level.SEVERE, "Error while creating zip file :", ioe);
            throw ioe;
        } finally {
            zout.close();
        }
        return zipFile;
    }

    /*
      * Creating zip file for given log files
      *
      * @param sourceDir Source directory from which needs to create zip
      * @param zipFileName zip file name which need to be created
      * @throws  IOException
      */

    public String createZipFile(String sourceDir, String zipFileName) throws IOException {
        ZipOutputStream zout = null;
        String zipFile = getZipFileName(sourceDir, zipFileName);
        try {
            //create object of FileOutputStream
            FileOutputStream fout = new FileOutputStream(zipFile);

            //create object of ZipOutputStream from FileOutputStream
            zout = new ZipOutputStream(fout);

            //create File object from source directory
            File fileSource = new File(sourceDir);

            addDirectory(zout, fileSource,
                    fileSource.getAbsolutePath().length() + 1);

            //close the ZipOutputStream
            zout.close();
        }
        catch (IOException ioe) {
            Logger.getAnonymousLogger().log(Level.SEVERE, "Error while creating zip file :", ioe);
            throw ioe;
        } finally {
            zout.close();
        }
        return zipFile;
    }


    /*
      * Helper method to creating zip.
      *
      * @param zout ZipOutputStream which points to zip file
      * @param  fileSource File which needs to add under zip
      *
      * @throws  IOException
      */

    private void addDirectory(ZipOutputStream zout, File fileSource, int ignoreLength) throws IOException {
        //get sub-folder/files list
        File[] files = fileSource.listFiles();
        FileInputStream fin = null;
        for (int i = 0; i < files.length; i++) {
            //if the file is directory, call the function recursively
            if (files[i].isDirectory()) {
                addDirectory(zout, files[i], ignoreLength);
                continue;
            }

            if (files[i].getAbsolutePath().contains(".zip")) {
                continue;
            }
            /*
            * we are here means, its file and not directory, so
            * add it to the zip file
            */
            try {
                //create byte buffer
                byte[] buffer = new byte[1024];

                //create object of FileInputStream
                fin = new FileInputStream(files[i].getAbsolutePath());
                zout.putNextEntry(new ZipEntry(ignoreLength > -1 ?
                        files[i].getAbsolutePath().substring(ignoreLength) :
                        files[i].getAbsolutePath()));

                /*
                            * After creating entry in the zip file, actually
                            * write the file.
                            */
                int length;
                while ((length = fin.read(buffer)) > 0) {
                    zout.write(buffer, 0, length);
                }

                /*
                            * After writing the file to ZipOutputStream, use
                            * void closeEntry() method of ZipOutputStream class to
                            * close the current entry and position the stream to
                            * write the next entry.
                            */
                zout.closeEntry();

            } catch (IOException ioe) {
                Logger.getAnonymousLogger().log(Level.SEVERE, "Error while creating zip file :", ioe);
                throw ioe;
            } finally {
                fin.close();
            }
        }
    }

    /* Return a logging file details  in the logging.properties file.
      * @throws  IOException
      */

    public synchronized String getLoggingFileDetails() throws IOException {
        try {
            loadLoggingProperties();
            Enumeration e = props.propertyNames();

            while (e.hasMoreElements()) {
                String key = (String) e.nextElement();
                // convert the name in domain.xml to the name in logging.properties if needed
                if (LoggingXMLNames.xmltoPropsMap.get(key) != null) {
                    key = LoggingXMLNames.xmltoPropsMap.get(key);
                }

                if (key != null && key.equals("com.sun.enterprise.server.logging.GFFileHandler.file")) {
                    return props.getProperty(key);
                }
            }
        } catch (IOException ex) {
            throw ex;
        }
        return null;
    }
}
