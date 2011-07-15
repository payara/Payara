/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.common.util.logging;

import com.sun.enterprise.util.io.FileUtils;
import org.glassfish.server.ServerEnvironmentImpl;
import org.jvnet.hk2.annotations.Contract;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PostConstruct;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Implementation of Logging Commands
 *
 * @author Naman Mehta
 */

@Service
@Contract
public class LoggingConfigImpl implements LoggingConfig, PostConstruct {

    @Inject
    Logger logger;

    @Inject
    ServerEnvironmentImpl env;

    Properties props = new Properties();
    FileInputStream fis;
    String loggingPropertiesName;
    LogManager logMgr = null;
    File loggingConfigDir = null;
    File file = null;
    File libFolder = null;

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
        logMgr = LogManager.getLogManager();
        libFolder = new File(installDir, "lib");
    }

    /*
      Load the properties  for DAS
      */

    private boolean openPropFile() throws IOException {
        try {
            props = new Properties();
            file = new File(loggingConfigDir, loggingPropertiesName);
            fis = new java.io.FileInputStream(file);
            props.load(fis);
            fis.close();
            return true;
        } catch (java.io.FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            Logger.getAnonymousLogger().log(Level.SEVERE, "Cannot read logging.properties file : ", e);
            throw new IOException();
        }
    }

    /*
      Copy logging.properties files if missing for given target.
      */

    public boolean copyLoggingPropertiesFile(File targetDir) throws IOException {

        Logger.getAnonymousLogger().log(Level.WARNING, "Logging.properties file not found, creating new file using DAS logging.properties");

        String rootFolder = env.getProps().get(com.sun.enterprise.util.SystemPropertyConstants.INSTALL_ROOT_PROPERTY);
        String templateDir = rootFolder + File.separator + "lib" + File.separator + "templates";
        File src = new File(templateDir, ServerEnvironmentImpl.kLoggingPropertiesFileName);

        File dest = new File(targetDir, ServerEnvironmentImpl.kLoggingPropertiesFileName);
        try {
            FileUtils.copy(src, dest);
            return true;
        } catch (IOException e) {
            Logger.getAnonymousLogger().log(Level.SEVERE, "Cannot create logging.properties file : ", e);
            throw new IOException();
        }

    }

    /*
      Load the properties  for given target.
      */

    private boolean openPropFile(String target) throws IOException {
        try {
            props = new Properties();
            String pathForLoggingFile = loggingConfigDir.getAbsolutePath() + File.separator + target;
            File dirForLogging = new File(pathForLoggingFile);

            file = new File(dirForLogging, loggingPropertiesName);

            if (!file.exists()) {
                copyLoggingPropertiesFile(dirForLogging);
                file = new File(dirForLogging, loggingPropertiesName);
            }
            fis = new java.io.FileInputStream(file);
            props.load(fis);
            fis.close();
            return true;
        } catch (java.io.FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            Logger.getAnonymousLogger().log(Level.SEVERE, "Cannot read logging.properties file : ", e);
            throw new IOException();
        }
    }


    private void closePropFile() throws IOException {
        try {
            FileOutputStream ois = new FileOutputStream(file);
            props.store(ois, "GlassFish logging.properties list");
            ois.close();
        } catch (FileNotFoundException e) {
            Logger.getAnonymousLogger().log(Level.SEVERE, "Cannot close logging.properties file : ", e);
            throw new IOException();
        } catch (IOException e) {
            //System.out.println("some other exception");
            Logger.getAnonymousLogger().log(Level.SEVERE, "Cannot close logging.properties file : ", e);
            throw new IOException();
        }
    }

    private void setWebLoggers(String value) {
        // set the rest of the web loggers to the same level
        // these are only accessible via the web-container name so all values should be the same
        String property = null;
        property = (String) props.setProperty("org.apache.catalina.level", value);
        property = (String) props.setProperty("org.apache.coyote.level", value);
        property = (String) props.setProperty("org.apache.jasper.level", value);
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
    public String setLoggingProperty(String propertyName, String propertyValue) throws IOException {
        try {
            if (!openPropFile())
                return null;
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
        } catch (IOException e) {
            throw e;
        }
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
    public String setLoggingProperty(String propertyName, String propertyValue, String targetConfigName) throws IOException {
        try {
            if (!openPropFile(targetConfigName))
                return null;
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
        } catch (IOException e) {
            throw e;
        }
    }

    /* update the properties to new values.  properties is a Map of names of properties and
      * their cooresponding value.  If the property does not exist then it is added to the
      * logging.properties file.
      *
      * @param properties Map of the name and value of property to add or update
      *
      * @throws  IOException
      */

    public Map<String, String> updateLoggingProperties(Map<String, String> properties) throws IOException {
        Map<String, String> m = new HashMap<String, String>();
        try {
            if (!openPropFile())
                return null;

            // need to map the name given to the new name in logging.properties file
            String key = null;
            for (Map.Entry<String, String> e : properties.entrySet()) {
                if (e.getValue() == null) continue;
                key = LoggingXMLNames.xmltoPropsMap.get(e.getKey());
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
            closePropFile();

        } catch (IOException ex) {
            throw ex;
        } catch (Exception e) {
            // e.printStackTrace();
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

    public Map<String, String> updateLoggingProperties(Map<String, String> properties, String targetConfigName) throws IOException {
        Map<String, String> m = new HashMap<String, String>();
        try {
            if (!openPropFile(targetConfigName))
                return null;

            // need to map the name given to the new name in logging.properties file
            String key = null;
            for (Map.Entry<String, String> e : properties.entrySet()) {
                if (e.getValue() == null) continue;
                key = LoggingXMLNames.xmltoPropsMap.get(e.getKey());
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
            closePropFile();

        } catch (IOException ex) {
            throw ex;
        } catch (Exception e) {
            // e.printStackTrace();
        }
        return m;
    }

    /* Return a Map of all the properties and corresponding values in the logging.properties file for given target.
      * @throws  IOException
      */

    public Map<String, String> getLoggingProperties(String targetConfigName) throws IOException {
        Map<String, String> m = new HashMap<String, String>();
        try {
            if (!openPropFile(targetConfigName))
                return null;
            Enumeration e = props.propertyNames();

            while (e.hasMoreElements()) {
                String key = (String) e.nextElement();
                // convert the name in domain.xml to the name in logging.properties if needed
                if (LoggingXMLNames.xmltoPropsMap.get(key) != null) {
                    key = LoggingXMLNames.xmltoPropsMap.get(key);
                }

                //System.out.println("Debug "+key+ " " + props.getProperty(key));
                m.put(key, props.getProperty(key));
            }
            return m;
        } catch (IOException ex) {
            throw ex;
        }
    }

    /* Return a Map of all the properties and corresponding values in the logging.properties file.
      * @throws  IOException
      */

    public Map<String, String> getLoggingProperties() throws IOException {
        Map<String, String> m = new HashMap<String, String>();
        try {
            if (!openPropFile())
                return null;
            Enumeration e = props.propertyNames();

            while (e.hasMoreElements()) {
                String key = (String) e.nextElement();
                // convert the name in domain.xml to the name in logging.properties if needed
                if (LoggingXMLNames.xmltoPropsMap.get(key) != null) {
                    key = LoggingXMLNames.xmltoPropsMap.get(key);
                }

                //System.out.println("Debug "+key+ " " + props.getProperty(key));
                m.put(key, props.getProperty(key));
            }
            return m;
        } catch (IOException ex) {
            throw ex;
        }
    }

    /* delete the properties from logging.properties file.  properties is a Map of names of properties and
      * their cooresponding value.
      * 
      * @param properties Map of the name and value of property to delete
      *
      * @throws  IOException
      */

    public void deleteLoggingProperties(Map<String, String> properties) throws IOException {
        try {
            openPropFile();

            // need to map the name given to the new name in logging.properties file
            String key = null;
            for (Map.Entry<String, String> e : properties.entrySet()) {
                key = LoggingXMLNames.xmltoPropsMap.get(e.getKey());
                if (key == null) {
                    key = e.getKey();
                }
                props.remove(key);
            }

            closePropFile();
        } catch (Exception e) {
            // e.printStackTrace();
        }
    }

    /* delete the properties from logging.properties file for given target.  properties is a Map of names of properties and
      * their cooresponding value.
      *
      * @param properties Map of the name and value of property to delete
      *
      * @throws  IOException
      */

    public void deleteLoggingProperties(Map<String, String> properties, String targetConfigName) throws IOException {
        try {
            openPropFile(targetConfigName);

            // need to map the name given to the new name in logging.properties file
            String key = null;
            for (Map.Entry<String, String> e : properties.entrySet()) {
                key = LoggingXMLNames.xmltoPropsMap.get(e.getKey());
                if (key == null) {
                    key = e.getKey();
                }
                props.remove(key);
            }

            closePropFile();
        } catch (Exception e) {
            // e.printStackTrace();
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

        String zipFile = sourceDir + File.separator + "log_" + currentTime + ".zip";

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

        String zipFile = getZipFileName(sourceDir);
        boolean zipDone = false;
        try {
            //create object of FileOutputStream
            FileOutputStream fout = new FileOutputStream(zipFile);

            //create object of ZipOutputStream from FileOutputStream
            ZipOutputStream zout = new ZipOutputStream(fout);

            //create File object from source directory
            File fileSource = new File(sourceDir);

            zipDone = addDirectory(zout, fileSource);

            //close the ZipOutputStream
            zout.close();
        }
        catch (IOException ioe) {
            Logger.getAnonymousLogger().log(Level.SEVERE, "Error while creating zip file :", ioe);
            throw ioe;
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

    private boolean addDirectory(ZipOutputStream zout, File fileSource) throws IOException {

        boolean zipDone = false;

        //get sub-folder/files list
        File[] files = fileSource.listFiles();

        for (int i = 0; i < files.length; i++) {
            //if the file is directory, call the function recursively
            if (files[i].isDirectory()) {
                addDirectory(zout, files[i]);
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
                FileInputStream fin = new FileInputStream(files[i].getAbsolutePath());
                zout.putNextEntry(new ZipEntry(files[i].getAbsolutePath()));

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

                //close the InputStream
                fin.close();

                zipDone = true;
            }
            catch (IOException ioe) {
                Logger.getAnonymousLogger().log(Level.SEVERE, "Error while creating zip file :", ioe);
                throw ioe;
            }
        }
        return zipDone;
    }

    /* Return a logging file details  in the logging.properties file.
      * @throws  IOException
      */

    public String getLoggingFileDetails() throws IOException {
        try {
            if (!openPropFile())
                return null;
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

    /* Return a logging file details  in the logging.properties file for given target.
      * @throws  IOException
      */

    public String getLoggingFileDetails(String targetConfigName) throws IOException {
        try {
            if (!openPropFile(targetConfigName))
                return null;
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

    /* Return a Map of all the properties and corresponding values from the logging.properties file from template..
      * @throws  IOException
      */

    public Map<String, String> getDefaultLoggingProperties() throws IOException {
        Map<String, String> m = new HashMap<String, String>();
        String rootFolder = env.getProps().get(com.sun.enterprise.util.SystemPropertyConstants.INSTALL_ROOT_PROPERTY);
        String templateDir = rootFolder + File.separator + "lib" + File.separator + "templates";
        File loggingTemplateFile = new File(templateDir, ServerEnvironmentImpl.kLoggingPropertiesFileName);

        Properties propsLoggingTempleate = new Properties();
        FileInputStream fisForLoggingTemplate = new java.io.FileInputStream(loggingTemplateFile);
        propsLoggingTempleate.load(fisForLoggingTemplate);
        fisForLoggingTemplate.close();

        Enumeration e = propsLoggingTempleate.propertyNames();

        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            // convert the name in domain.xml to the name in logging.properties if needed
            if (LoggingXMLNames.xmltoPropsMap.get(key) != null) {
                key = LoggingXMLNames.xmltoPropsMap.get(key);
            }
            m.put(key, propsLoggingTempleate.getProperty(key));
        }
        return m;
    }

}
