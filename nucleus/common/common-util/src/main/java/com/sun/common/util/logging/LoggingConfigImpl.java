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

// Portions Copyright [2014-2018] [Payara Foundation and/or its affiliates]

package com.sun.common.util.logging;

import static com.sun.common.util.logging.LoggingXMLNames.xmltoPropsMap;

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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;

import org.glassfish.api.admin.FileMonitoring;
import org.glassfish.hk2.api.PostConstruct;
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
public class LoggingConfigImpl implements LoggingConfig, PostConstruct {

    @Inject
    private ServerEnvironmentImpl env;

    @Inject
    private FileMonitoring fileMonitoring;

    private Properties props = new Properties();
    private String loggingPropertiesName;
    private File loggingConfigDir = null;

    /**
     * Constructor
     */

    public void postConstruct() {
        // set logging.properties filename
        setupConfigDir(env.getConfigDirPath(), env.getLibPath());

    }

    // this is so the launcher can pass in where the dir is since

    public void setupConfigDir(File file, File installDir) {
        loggingConfigDir = file;
        loggingPropertiesName = ServerEnvironmentImpl.kLoggingPropertiesFileName;
    }

    /**
     * Load the properties  for DAS
     */
    private void loadLoggingProperties() throws IOException {
        props = new Properties();
        File file = getLoggingPropertiesFile();
        try (InputStream fis = new BufferedInputStream(getInputStream(file))) {
            props.load(fis);
        }
    }

    /**
     * Load the properties  for given target.
     */
    private void loadLoggingProperties(String target) throws IOException {
        props = new Properties();
        File file = getLoggingPropertiesFile(target);
        try (InputStream fis = new BufferedInputStream(getInputStream(file))) {
            props.load(fis);
        }
    }

    private InputStream getInputStream(File file) throws IOException {
        InputStream fileInputStream;
        if (!file.exists()) {
            fileInputStream = getDefaultLoggingPropertiesInputStream();
        } else {
            fileInputStream = new FileInputStream(file);
        }
        return fileInputStream;
    }

    private File getLoggingPropertiesFile() {
        return new File(loggingConfigDir, loggingPropertiesName);
    }

    private File getLoggingPropertiesFile(String target) {
        String pathForLoggingFile = loggingConfigDir.getAbsolutePath() + File.separator + target;
        return new File(pathForLoggingFile, ServerEnvironmentImpl.kLoggingPropertiesFileName);
    }

    private FileInputStream getDefaultLoggingPropertiesInputStream() throws IOException {
        File defaultConfig = new File(env.getConfigDirPath(),
                ServerEnvironmentImpl.kDefaultLoggingPropertiesFileName);
        return new FileInputStream(defaultConfig);
    }

    private void closePropFile(String targetConfigName) throws IOException {
        File file;
        if (targetConfigName == null || targetConfigName.isEmpty()) {
            file = getLoggingPropertiesFile();
        } else {
            file = getLoggingPropertiesFile(targetConfigName);
        }
        File parentFile = file.getParentFile();
        if (!parentFile.exists() && !parentFile.mkdirs()) {
            throw new IOException();
        }
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(file))) {
            props.store(os, "GlassFish logging.properties list");
            os.flush();
            fileMonitoring.fileModified(file);
        }
    }

    private void setWebLoggers(String value) {
        // set the rest of the web loggers to the same level
        // these are only accessible via the web-container name so all values should be the same
        props.setProperty("org.apache.catalina.level", value);
        props.setProperty("org.apache.coyote.level", value);
        props.setProperty("org.apache.jasper.level", value);
    }

    /**
     * setLoggingProperty() sets an existing propertyName to be propertyValue
     * if the property doesn't exist the property will be added.  The logManager
     * readConfiguration is not called in this method.
     *
     * @param propertyName  Name of the property to set
     * @param propertyValue Value to set
     * @throws IOException
     */
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

        closePropFile("");
        return property;
    }

    /**
     * setLoggingProperty() sets an existing propertyName to be propertyValue
     * if the property doesn't exist the property will be added.  The logManager
     * readConfiguration is not called in this method.
     *
     * @param propertyName  Name of the property to set
     * @param propertyValue Value to set
     * @throws IOException
     */
    public synchronized String setLoggingProperty(String propertyName, String propertyValue, String targetConfigName) throws IOException {
        loadLoggingProperties(targetConfigName);
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

        closePropFile(targetConfigName);
        return property;
    }

    /* update the properties to new values.  properties is a Map of names of properties and
      * their cooresponding value.  If the property does not exist then it is added to the
      * logging.properties file.
      *
      * @param properties Map of the name and value of property to add or update
      *
      * @throws  IOException
      */

    public synchronized Map<String, String> updateLoggingProperties(Map<String, String> properties) throws IOException {
        loadLoggingProperties();
        // need to map the name given to the new name in logging.properties file
        Map<String, String> m = getMap(properties);
        closePropFile("");
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

    /* update the properties to new values for given target.  properties is a Map of names of properties and
      * their cooresponding value.  If the property does not exist then it is added to the
      * logging.properties file.
      *
      * @param properties Map of the name and value of property to add or update
      *
      * @throws  IOException
      */

    public synchronized Map<String, String> updateLoggingProperties(Map<String, String> properties, String targetConfigName) throws IOException {
        loadLoggingProperties(targetConfigName);
        // need to map the name given to the new name in logging.properties file
        Map<String, String> m = getMap(properties);
        closePropFile(targetConfigName);
        return m;
    }

    /* Return a Map of all the properties and corresponding values in the logging.properties file for given target.
      * @throws  IOException
      */

    public synchronized Map<String, String> getLoggingProperties(String targetConfigName) throws IOException {
        loadLoggingProperties(targetConfigName);
        Enumeration e = props.propertyNames();
        Map<String, String> m = getMap(e);
        return checkForLoggingProperties(m, targetConfigName);
    }

    /* Return a Map of all the properties and corresponding values in the logging.properties file.
      * @throws  IOException
      */

    public synchronized Map<String, String> getLoggingProperties() throws IOException {
        loadLoggingProperties();
        Enumeration e = props.propertyNames();
        Map<String, String> m = getMap(e);
        return checkForLoggingProperties(m, "");
    }

    private Map<String, String> getMap(Enumeration e) {
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

    public synchronized Map<String, String> checkForLoggingProperties(Map<String, String> loggingProperties, String targetConfigName) throws IOException {

        if (!loggingProperties.containsKey(Constants.GF_HANDLER_LOG_TO_FILE)) {
            loggingProperties.put(Constants.GF_HANDLER_LOG_TO_FILE, Constants.GF_HANDLER_LOG_TO_FILE_DEFAULT_VALUE);

            if (targetConfigName == null || targetConfigName.isEmpty()) {
                setLoggingProperty(Constants.GF_HANDLER_LOG_TO_FILE, Constants.GF_HANDLER_LOG_TO_FILE_DEFAULT_VALUE);
            } else {
                setLoggingProperty(Constants.GF_HANDLER_LOG_TO_FILE, Constants.GF_HANDLER_LOG_TO_FILE_DEFAULT_VALUE, targetConfigName);
            }
        }

        if (!loggingProperties.containsKey(Constants.PY_HANDLER_LOG_TO_FILE)) {
            loggingProperties.put(Constants.PY_HANDLER_LOG_TO_FILE, Constants.PY_HANDLER_LOG_TO_FILE_DEFAULT_VALUE);
            if (targetConfigName == null || targetConfigName.isEmpty()) {
                setLoggingProperty(Constants.PY_HANDLER_LOG_TO_FILE, Constants.PY_HANDLER_LOG_TO_FILE_DEFAULT_VALUE);
            } else {
                setLoggingProperty(Constants.PY_HANDLER_LOG_TO_FILE, Constants.PY_HANDLER_LOG_TO_FILE_DEFAULT_VALUE, targetConfigName);
            }
        }

        if (!loggingProperties.containsKey(Constants.PY_HANDLER_LOG_FILE)) {
            loggingProperties.put(Constants.PY_HANDLER_LOG_FILE, Constants.PY_HANDLER_LOG_FILE_DEFAULT_VALUE);
            if (targetConfigName == null || targetConfigName.isEmpty()) {
                setLoggingProperty(Constants.PY_HANDLER_LOG_FILE, Constants.PY_HANDLER_LOG_FILE_DEFAULT_VALUE);
            } else {
                setLoggingProperty(Constants.PY_HANDLER_LOG_FILE, Constants.PY_HANDLER_LOG_FILE_DEFAULT_VALUE, targetConfigName);
            }
        }

        if (!loggingProperties.containsKey(Constants.PY_HANDLER_MAXIMUM_FILES)) {
            loggingProperties.put(Constants.PY_HANDLER_MAXIMUM_FILES, Constants.PY_HANDLER_MAXIMUM_FILES_DEFAULT_VALUE);
            if (targetConfigName == null || targetConfigName.isEmpty()) {
                setLoggingProperty(Constants.PY_HANDLER_MAXIMUM_FILES, Constants.PY_HANDLER_MAXIMUM_FILES_DEFAULT_VALUE);
            } else {
                setLoggingProperty(Constants.PY_HANDLER_MAXIMUM_FILES, Constants.PY_HANDLER_MAXIMUM_FILES_DEFAULT_VALUE, targetConfigName);
            }
        }

        if (!loggingProperties.containsKey(Constants.PY_HANDLER_ROTATION_ON_DATE_CHANGE)) {
            loggingProperties.put(Constants.PY_HANDLER_ROTATION_ON_DATE_CHANGE, Constants.PY_HANDLER_ROTATION_ON_DATE_CHANGE_DEFAULT_VALUE);
            if (targetConfigName == null || targetConfigName.isEmpty()) {
                setLoggingProperty(Constants.PY_HANDLER_ROTATION_ON_DATE_CHANGE, Constants.PY_HANDLER_ROTATION_ON_DATE_CHANGE_DEFAULT_VALUE);
            } else {
                setLoggingProperty(Constants.PY_HANDLER_ROTATION_ON_DATE_CHANGE, Constants.PY_HANDLER_ROTATION_ON_DATE_CHANGE_DEFAULT_VALUE, targetConfigName);
            }
        }

        if (!loggingProperties.containsKey(Constants.PY_HANDLER_ROTATION_ON_FILE_SIZE)) {
            loggingProperties.put(Constants.PY_HANDLER_ROTATION_ON_FILE_SIZE, Constants.PY_HANDLER_ROTATION_ON_FILE_SIZE_DEFAULT_VALUE);
            if (targetConfigName == null || targetConfigName.isEmpty()) {
                setLoggingProperty(Constants.PY_HANDLER_ROTATION_ON_FILE_SIZE, Constants.PY_HANDLER_ROTATION_ON_FILE_SIZE_DEFAULT_VALUE);
            } else {
                setLoggingProperty(Constants.PY_HANDLER_ROTATION_ON_FILE_SIZE, Constants.PY_HANDLER_ROTATION_ON_FILE_SIZE_DEFAULT_VALUE, targetConfigName);
            }
        }

        if (!loggingProperties.containsKey(Constants.PY_HANDLER_ROTATION_ON_TIME_LIMIT)) {
            loggingProperties.put(Constants.PY_HANDLER_ROTATION_ON_TIME_LIMIT, Constants.PY_HANDLER_ROTATION_ON_TIME_LIMIT_DEFAULT_VALUE);
            if (targetConfigName == null || targetConfigName.isEmpty()) {
                setLoggingProperty(Constants.PY_HANDLER_ROTATION_ON_TIME_LIMIT, Constants.PY_HANDLER_ROTATION_ON_TIME_LIMIT_DEFAULT_VALUE);
            } else {
                setLoggingProperty(Constants.PY_HANDLER_ROTATION_ON_TIME_LIMIT, Constants.PY_HANDLER_ROTATION_ON_TIME_LIMIT_DEFAULT_VALUE, targetConfigName);
            }
        }

        if (!loggingProperties.containsKey(Constants.PY_HANDLER_COMPRESS_ON_ROTATION)) {
            loggingProperties.put(Constants.PY_HANDLER_COMPRESS_ON_ROTATION, Constants.PY_HANDLER_COMPRESS_ON_ROTATION_DEFAULT_VALUE);
            if (targetConfigName == null || targetConfigName.isEmpty()) {
                setLoggingProperty(Constants.PY_HANDLER_COMPRESS_ON_ROTATION, Constants.PY_HANDLER_COMPRESS_ON_ROTATION_DEFAULT_VALUE);
            } else {
                setLoggingProperty(Constants.PY_HANDLER_COMPRESS_ON_ROTATION, Constants.PY_HANDLER_COMPRESS_ON_ROTATION_DEFAULT_VALUE, targetConfigName);
            }
        }

        if (!loggingProperties.containsKey(Constants.PY_HANDLER_LOG_FORMATTER)) {
            loggingProperties.put(Constants.PY_HANDLER_LOG_FORMATTER, Constants.PY_HANDLER_LOG_FORMATTER_DEFAULT_VALUE);
            if (targetConfigName == null || targetConfigName.isEmpty()) {
                setLoggingProperty(Constants.PY_HANDLER_LOG_FORMATTER, Constants.PY_HANDLER_LOG_FORMATTER_DEFAULT_VALUE);
            } else {
                setLoggingProperty(Constants.PY_HANDLER_LOG_FORMATTER, Constants.PY_HANDLER_LOG_FORMATTER_DEFAULT_VALUE, targetConfigName);
            }
        }

        return loggingProperties;
    }

    /* delete the properties from logging.properties file.  properties is a Map of names of properties and
      * their cooresponding value.
      *
      * @param properties Map of the name and value of property to delete
      *
      * @throws  IOException
      */

    public synchronized void deleteLoggingProperties(Map<String, String> properties) throws IOException {
        loadLoggingProperties();
        // need to map the name given to the new name in logging.properties file
        remove(properties);
        closePropFile("");
    }

    /* delete the properties from logging.properties file for given target.  properties is a Map of names of properties and
      * their cooresponding value.
      *
      * @param properties Map of the name and value of property to delete
      *
      * @throws  IOException
      */

    public synchronized void deleteLoggingProperties(Map<String,
            String> properties, String targetConfigName) throws IOException {
        loadLoggingProperties(targetConfigName);
        // need to map the name given to the new name in logging.properties file
        remove(properties);
        closePropFile(targetConfigName);
    }

    private void remove(Map<String, String> properties) {
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

        return sourceDir + File.separator + "log_" + currentTime + ".zip";
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

        return sourceDir + File.separator + fileName + "-" + currentTime + ".zip";
    }

    /*
      * Creating zip file for given log files
      *
      * @param sourceDir Source directory from which needs to create zip
      *
      * @throws  IOException
      */

    public String createZipFile(String sourceDir) throws IOException {
        String zipFile = getZipFileName(sourceDir);
        add(sourceDir, zipFile);
        return zipFile;
    }

    private void add(String sourceDir, String zipFile) throws IOException {
        try (FileOutputStream fout = new FileOutputStream(zipFile);
             ZipOutputStream zout = new ZipOutputStream(fout)) {
            File fileSource = new File(sourceDir);
            addDirectory(zout, fileSource, fileSource.getAbsolutePath().length() + 1);
        } catch (IOException ioe) {
            Logger.getAnonymousLogger().log(Level.SEVERE, "Error while creating zip file :", ioe);
            throw ioe;
        }
    }

    /*
      * Creating zip file for given log files
      *
      * @param sourceDir Source directory from which needs to create zip
      * @param zipFileName zip file name which need to be created
      * @throws  IOException
      */

    public String createZipFile(String sourceDir, String zipFileName) throws IOException {
        String zipFile = getZipFileName(sourceDir, zipFileName);
        add(sourceDir, zipFile);
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
        if (files != null) {
            for (File file : files) {
                //if the file is directory, call the function recursively
                if (file.isDirectory()) {
                    addDirectory(zout, file, ignoreLength);
                    continue;
                }

                if (file.getAbsolutePath().contains(".zip")) {
                    continue;
                }
                /*
                 * we are here means, its file and not directory, so
                 * add it to the zip file
                 */
                try (FileInputStream fin = new FileInputStream(file.getAbsolutePath())) {
                    //create byte buffer
                    byte[] buffer = new byte[1024];

                    //create object of FileInputStream
                    zout.putNextEntry(new ZipEntry(ignoreLength > -1 ?
                            file.getAbsolutePath().substring(ignoreLength) :
                            file.getAbsolutePath()));

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
                }
            }
        }
    }

    /* Return a logging file details  in the logging.properties file.
      * @throws  IOException
      */

    public synchronized String getLoggingFileDetails() throws IOException {
        loadLoggingProperties();

        @SuppressWarnings("unchecked")
        Enumeration<String> loggingPropertyNames = (Enumeration<String>) props.propertyNames();

        while (loggingPropertyNames.hasMoreElements()) {
            String key = loggingPropertyNames.nextElement();

            // Convert the name in domain.xml to the name in logging.properties if needed
            key = xmltoPropsMap.getOrDefault(key, key);

            if (key != null && key.equals("com.sun.enterprise.server.logging.GFFileHandler.file")) {
                return props.getProperty(key);
            }
        }

        // If "com.sun.enterprise.server.logging.GFFileHandler.file" not found, check "java.util.logging.FileHandler.pattern"
        // This property can have been set by Payara Micro when using the --logtofile
        return props.getProperty("java.util.logging.FileHandler.pattern");
    }

    /* Return a logging file details  in the logging.properties file for given target.
      * @throws  IOException
      */

    public synchronized String getLoggingFileDetails(String targetConfigName) throws IOException {
        loadLoggingProperties(targetConfigName);
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
        return null;
    }

    /* Return a Map of all the properties and corresponding values from the logging.properties file from template..
      * @throws  IOException
      */

    public Map<String, String> getDefaultLoggingProperties() throws IOException {
        Properties propsLoggingTemplate = new Properties();
        File loggingTemplateFile =
                new File(env.getConfigDirPath(), ServerEnvironmentImpl.kDefaultLoggingPropertiesFileName);
        try (FileInputStream fisForLoggingTemplate = new java.io.FileInputStream(loggingTemplateFile)) {
            propsLoggingTemplate.load(fisForLoggingTemplate);
        }

        Enumeration e = propsLoggingTemplate.propertyNames();
        Map<String, String> m = getMap(e);
        m = checkForLoggingProperties(m, "");
        return m;
    }
}
