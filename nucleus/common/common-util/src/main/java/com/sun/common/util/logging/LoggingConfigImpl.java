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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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

// Portions Copyright [2014-2025] [Payara Foundation and/or its affiliates]

package com.sun.common.util.logging;

import com.sun.enterprise.util.PropertyPlaceholderHelper;
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
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import jakarta.inject.Inject;
import org.glassfish.api.admin.FileMonitoring;
import org.glassfish.server.ServerEnvironmentImpl;
import org.jvnet.hk2.annotations.Contract;
import org.jvnet.hk2.annotations.Service;

import static com.sun.common.util.logging.LoggingXMLNames.xmltoPropsMap;

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

    protected static final Map<String, String> DEFAULT_LOG_PROPERTIES = Map.ofEntries(
            Map.entry(GF_FILE_HANDLER + ".logtoFile", "true"),
            Map.entry(PY_FILE_HANDLER + ".logtoFile", "true"),
            Map.entry(PY_FILE_HANDLER + ".rotationOnDateChange", "false"),
            Map.entry(PY_FILE_HANDLER + ".rotationTimelimitInMinutes", "0"),
            Map.entry(GF_FILE_HANDLER + ".rotationLimitInBytes", "2000000"),
            Map.entry(PY_FILE_HANDLER + ".rotationLimitInBytes", "2000000"),
            Map.entry(PY_FILE_HANDLER + ".maxHistoryFiles", "0"),
            Map.entry(PY_FILE_HANDLER + ".file", "${com.sun.aas.instanceRoot}/logs/notification.log"),
            Map.entry(PY_FILE_HANDLER + ".compressOnRotation", "false"),
            Map.entry(GF_FILE_HANDLER + ".logStandardStreams", "true"),
            Map.entry(PY_FILE_HANDLER + ".formatter", "com.sun.enterprise.server.logging.ODLLogFormatter"),
            Map.entry(GF_FILE_HANDLER + ".fastLogging", "false")
    );

    @Inject
    private FileMonitoring fileMonitoring;

    private String target;
    private final Properties props = new Properties();
    private final String loggingPropertiesName;
    private final File loggingConfigDir;
    private final File defaultLogFile;

    @Inject
    public LoggingConfigImpl(ServerEnvironmentImpl env) {
        this(env.getConfigDirPath(), env.getConfigDirPath());
    }

    public LoggingConfigImpl(File defaultConfigDir, File configDir) {
        loggingConfigDir = configDir;
        loggingPropertiesName = ServerEnvironmentImpl.kLoggingPropertiesFileName;
        this.defaultLogFile = new File(defaultConfigDir,
                ServerEnvironmentImpl.kDefaultLoggingPropertiesFileName);
    }

    @Override
    public void initialize(String target) throws IOException {
        this.target = target;
    }

    /**
     * Load the properties  for DAS
     */
    private void loadLoggingProperties() throws IOException {
        props.clear();
        File file = getLoggingPropertiesFile();
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
        if (target == null || target.isEmpty()) {
            return new File(loggingConfigDir, loggingPropertiesName);
        }
        String pathForLoggingFile = loggingConfigDir.getAbsolutePath() + File.separator + target;
        return new File(pathForLoggingFile, ServerEnvironmentImpl.kLoggingPropertiesFileName);
    }

    private FileInputStream getDefaultLoggingPropertiesInputStream() throws IOException {
        return new FileInputStream(defaultLogFile);
    }

    private void closePropFile() throws IOException {
        File file = getLoggingPropertiesFile();
        File parentFile = file.getParentFile();
        if (!parentFile.exists() && !parentFile.mkdirs()) {
            throw new IOException();
        }
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(file))) {
            new SortedLoggingProperties(props).store(os, "GlassFish logging.properties list");
            os.flush();
            fileMonitoring.fileModified(file);
        }
    }

    private void setWebLoggers(String value) {
        // set the rest of the web loggers to the same level
        // these are only accessible via the web-container name so all values should be the same
        props.setProperty("org.apache.catalina.level", value);
        props.setProperty("org.apache.coyote.level", value);
        props.setProperty("org.glassfish.wasp.level", value);
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
                setWebLoggers(new PropertyPlaceholderHelper(System.getenv(), PropertyPlaceholderHelper.ENV_REGEX).replacePlaceholder(e.getValue()));
            }
            //build Map of entries to return
            m.put(key, new PropertyPlaceholderHelper(System.getenv(), PropertyPlaceholderHelper.ENV_REGEX).replacePlaceholder(property));
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

    @Override
    public synchronized Map<String, String> getLoggingProperties() throws IOException {
        return getLoggingProperties(true);
    }

    /**
     * @param usePlaceholderReplacement - true for placeholder replacement, false returns original property value
     * @return a Map of all the properties and corresponding values in the logging.properties file.
     * @throws IOException 
     */
    @Override
    public synchronized Map<String, String> getLoggingProperties(boolean usePlaceholderReplacement) throws IOException {
        loadLoggingProperties();
        Enumeration<?> e = props.propertyNames();
        Map<String, String> m = getMap(e, usePlaceholderReplacement);
        return checkForLoggingProperties(m);
    }

    private Map<String, String> getMap(Enumeration<?> e, boolean usePlaceholderReplacement) {
        Map<String, String> m = new HashMap<>();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            // convert the name in domain.xml to the name in logging.properties if needed
            if (LoggingXMLNames.xmltoPropsMap.get(key) != null) {
                key = LoggingXMLNames.xmltoPropsMap.get(key);
            }
            String value = usePlaceholderReplacement ? new PropertyPlaceholderHelper(System.getenv(), PropertyPlaceholderHelper.ENV_REGEX).replacePlaceholder(props.getProperty(key)) : props.getProperty(key);
            m.put(key, value);
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
        remove(properties);
        closePropFile();
        checkForLoggingProperties(getLoggingProperties());
        return properties;
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
    @Override
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
    @Override
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
}
