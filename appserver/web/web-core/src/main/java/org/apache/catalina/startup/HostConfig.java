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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.catalina.startup;


import org.apache.catalina.*;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.core.StandardServer;
import org.apache.naming.resources.ResourceAttributes;
import org.glassfish.logging.annotation.LogMessageInfo;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import java.io.*;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Startup event listener for a <b>Host</b> that configures the properties
 * of that Host, and the associated defined contexts.
 *
 * @author Craig R. McClanahan
 * @author Remy Maucherat
 * @version $Revision: 1.4 $ $Date: 2006/10/03 20:19:13 $
 */

public class HostConfig
    implements LifecycleListener {
    
    private static final Logger log = StandardServer.log;
    private static final ResourceBundle rb = log.getResourceBundle();

    @LogMessageInfo(
            message = "Lifecycle event data object {0} is not a Host",
            level = "SEVERE",
            cause = "Could not process the START event for an associated Host",
            action = "Verify Lifecycle event data object"
    )
    public static final String LIFECYCLE_OBJECT_NOT_HOST_EXCEPTION = "AS-WEB-CORE-00450";

    @LogMessageInfo(
            message = "Deploying configuration descriptor {0}",
            level = "FINE"
    )
    public static final String DEPLOYING_CONFIG_DESCRIPTOR = "AS-WEB-CORE-00451";

    @LogMessageInfo(
            message = "Error deploying configuration descriptor {0}",
            level = "SEVERE",
            cause = "Could not deploy configuration descriptor",
            action = "Verify the URL that points to context configuration file and the context path"
    )
    public static final String ERROR_DEPLOYING_CONFIG_DESCRIPTOR_EXCEPTION = "AS-WEB-CORE-00452";

    @LogMessageInfo(
            message = "The war name [{0}] is invalid. The archive will be ignored.",
            level = "SEVERE",
            cause = "Could not deploy war file",
            action = "Verify the name war file"
    )
    public static final String INVALID_WAR_NAME_EXCEPTION = "AS-WEB-CORE-00453";

    @LogMessageInfo(
            message = "Expanding web application archive {0}",
            level = "FINE"
    )
    public static final String EXPANDING_WEB_APP = "AS-WEB-CORE-00454";

    @LogMessageInfo(
            message = "Exception while expanding web application archive {0}",
            level = "WARNING"
    )
    public static final String EXPANDING_WEB_APP_EXCEPTION = "AS-WEB-CORE-00455";

    @LogMessageInfo(
            message = "Exception while expanding web application archive {0}",
            level = "SEVERE",
            cause = "Could not expand web application archive",
            action = "Verify the URL, and if any I/O errors orrur"
    )
    public static final String EXPANDING_WEB_APP_ARCHIVE_EXCEPTION = "AS-WEB-CORE-00456";

    @LogMessageInfo(
            message = "Deploying web application archive {0}",
            level = "INFO"
    )
    public static final String DEPLOYING_WEB_APP_ARCHIVE = "AS-WEB-CORE-00457";

    @LogMessageInfo(
            message = "Error deploying web application archive {0}",
            level = "SEVERE",
            cause = "Could not deploy web application archive",
            action = "Verify the context path and if specified context path " +
                     "is already attached to an existing web application"
    )
    public static final String ERROR_DEPLOYING_WEB_APP_ARCHIVE_EXCEPTION = "AS-WEB-CORE-00458";

    @LogMessageInfo(
            message = "Deploying web application directory {0}",
            level = "FINE"
    )
    public static final String DEPLOYING_WEB_APP_DIR = "AS-WEB-CORE-00459";

    @LogMessageInfo(
            message = "Error deploying web application directory {0}",
            level = "SEVERE",
            cause = "Could not deploy web application directory",
            action = "Verify the context path and if specified context path " +
                     "is already attached to an existing web application"
    )
    public static final String ERROR_DEPLOYING_WEB_APP_DIR = "AS-WEB-CORE-00460";

    @LogMessageInfo(
            message = "Error undeploying Jar file {0}",
            level = "SEVERE",
            cause = "Could not remove an existing web application, attached to the specified context path",
            action = "Verify the context path of the application"
    )
    public static final String ERROR_UNDEPLOYING_JAR_FILE_EXCEPTION = "AS-WEB-CORE-00461";

    @LogMessageInfo(
            message = "HostConfig: restartContext [{0}]",
            level = "INFO"
    )
    public static final String RESTART_CONTEXT_INFO = "AS-WEB-CORE-00462";

    @LogMessageInfo(
            message = "Error during context [{0}] stop",
            level = "WARNING"
    )
    public static final String ERROR_DURING_CONTEXT_STOP_EXCEPTION = "AS-WEB-CORE-00463";

    @LogMessageInfo(
            message = "Error during context [{0}] restart",
            level = "WARNING"
    )
    public static final String ERROR_DURING_CONTEXT_RESTART_EXCEPTION = "AS-WEB-CORE-00464";

    @LogMessageInfo(
            message = "HostConfig: Processing START",
            level = "FINE"
    )
    public static final String PROCESSING_START = "AS-WEB-CORE-00465";

    @LogMessageInfo(
            message = "HostConfig: Processing STOP",
            level = "FINE"
    )
    public static final String PROCESSING_STOP = "AS-WEB-CORE-00466";

    @LogMessageInfo(
            message = "Undeploying deployed web applications",
            level = "FINE"
    )
    public static final String UNDEPLOYING_WEB_APP = "AS-WEB-CORE-00467";

    @LogMessageInfo(
            message = "Undeploying context [{0}]",
            level = "FINE"
    )
    public static final String UNDEPLOYING_CONTEXT = "AS-WEB-CORE-00468";

    @LogMessageInfo(
            message = "Error undeploying web application at context path {0}",
            level = "SEVERE",
            cause = "Could not remove an existing web application, attached to the specified context path",
            action = "Verify the context path of the application"
    )
    public static final String ERROR_UNDEPLOYING_WEB_APP_EXCEPTION = "AS-WEB-CORE-00469";

    // ----------------------------------------------------- Instance Variables


    /**
     * App base.
     */
    private File appBase = null;


    /**
     * Config base.
     */
    private File configBase = null;


    /**
     * The Java class name of the Context configuration class we should use.
     */
    protected String configClass = "org.apache.catalina.startup.ContextConfig";


    /**
     * The Java class name of the Context implementation we should use.
     */
    protected String contextClass = "org.apache.catalina.core.StandardContext";


    /**
     * The debugging detail level for this component.
     */
    protected int debug = 0;


    /**
     * The names of applications that we have auto-deployed (to avoid
     * double deployment attempts).
     */
    protected List<String> deployed = new ArrayList<String>();


    /**
     * The Host we are associated with.
     */
    protected Host host = null;

    /**
     * Should we deploy XML Context config files?
     */
    private boolean deployXML = false;


    /**
     * Should we unpack WAR files when auto-deploying applications in the
     * <code>appBase</code> directory?
     */
    private boolean unpackWARs = false;


    /**
     * Last modified dates of the web.xml files of the contexts, keyed by
     * context name.
     */
    private Map<String, Long> webXmlLastModified = new HashMap<String, Long>();


    /**
     * Last modified dates of the Context xml files of the contexts, keyed by
     * context name.
     */
    private Map<String, Long> contextXmlLastModified = new HashMap<String, Long>();


    /**
     * Last modified dates of the source WAR files, keyed by WAR name.
     */
    private HashMap<String, Long> warLastModified = new HashMap<String, Long>();


    /**
     * Attribute value used to turn on/off XML validation
     */
    private boolean xmlValidation = false;


    /**
     * Attribute value used to turn on/off XML namespace awarenes.
     */
    private boolean xmlNamespaceAware = false;


    /**
     * The list of Wars in the appBase to be ignored because they are invalid
     * (e.g. contain /../ sequences).
     */
    protected Set<String> invalidWars = new HashSet<String>();


    // ------------------------------------------------------------- Properties


    /**
     * Return the Context configuration class name.
     */
    public String getConfigClass() {

        return (this.configClass);

    }


    /**
     * Set the Context configuration class name.
     *
     * @param configClass The new Context configuration class name.
     */
    public void setConfigClass(String configClass) {

        this.configClass = configClass;

    }


    /**
     * Return the Context implementation class name.
     */
    public String getContextClass() {

        return (this.contextClass);

    }


    /**
     * Set the Context implementation class name.
     *
     * @param contextClass The new Context implementation class name.
     */
    public void setContextClass(String contextClass) {

        this.contextClass = contextClass;

    }


    /**
     * Return the debugging detail level for this component.
     */
    public int getDebug() {

        return (this.debug);

    }


    /**
     * Set the debugging detail level for this component.
     *
     * @param debug The new debugging detail level
     */
    public void setDebug(int debug) {

        this.debug = debug;

    }


    /**
     * Return the deploy XML config file flag for this component.
     */
    public boolean isDeployXML() {

        return (this.deployXML);

    }


    /**
     * Set the deploy XML config file flag for this component.
     *
     * @param deployXML The new deploy XML flag
     */
    public void setDeployXML(boolean deployXML) {

        this.deployXML= deployXML;

    }


    /**
     * Return the unpack WARs flag.
     */
    public boolean isUnpackWARs() {

        return (this.unpackWARs);

    }


    /**
     * Set the unpack WARs flag.
     *
     * @param unpackWARs The new unpack WARs flag
     */
    public void setUnpackWARs(boolean unpackWARs) {

        this.unpackWARs = unpackWARs;

    }
    
    
     /**
     * Set the validation feature of the XML parser used when
     * parsing xml instances.
     * @param xmlValidation true to enable xml instance validation
     */
    public void setXmlValidation(boolean xmlValidation){
        this.xmlValidation = xmlValidation;
    }

    /**
     * Get the server.xml <host> attribute's xmlValidation.
     * @return true if validation is enabled.
     *
     */
    public boolean getXmlValidation(){
        return xmlValidation;
    }

    /**
     * Get the server.xml <host> attribute's xmlNamespaceAware.
     * @return true if namespace awarenes is enabled.
     *
     */
    public boolean getXmlNamespaceAware(){
        return xmlNamespaceAware;
    }


    /**
     * Set the namespace aware feature of the XML parser used when
     * parsing xml instances.
     * @param xmlNamespaceAware true to enable namespace awareness
     */
    public void setXmlNamespaceAware(boolean xmlNamespaceAware){
        this.xmlNamespaceAware=xmlNamespaceAware;
    }    


    // --------------------------------------------------------- Public Methods


    /**
     * Process the START event for an associated Host.
     *
     * @param event The lifecycle event that has occurred
     */
    public void lifecycleEvent(LifecycleEvent event) {

        if (event.getType().equals("check"))
            check();

        // Identify the host we are associated with
        try {
            host = (Host) event.getLifecycle();
            if (host instanceof StandardHost) {
                int hostDebug = ((StandardHost) host).getDebug();
                if (hostDebug > this.debug) {
                    this.debug = hostDebug;
                }
                setDeployXML(((StandardHost) host).isDeployXML());
                setUnpackWARs(((StandardHost) host).isUnpackWARs());
                setXmlNamespaceAware(((StandardHost) host).getXmlNamespaceAware());
                setXmlValidation(((StandardHost) host).getXmlValidation());
            }
        } catch (ClassCastException e) {
            String msg = MessageFormat.format(rb.getString(LIFECYCLE_OBJECT_NOT_HOST_EXCEPTION),
                                              event.getLifecycle());
            log.log(Level.SEVERE, msg, e);
            return;
        }

        // Process the event that has occurred
        if (event.getType().equals(Lifecycle.START_EVENT))
            start();
        else if (event.getType().equals(Lifecycle.STOP_EVENT))
            stop();

    }


    // ------------------------------------------------------ Protected Methods


    /**
     * Return a File object representing the "application root" directory
     * for our associated Host.
     */
    protected File appBase() {

        if (appBase != null) {
            return appBase;
        }

        File file = new File(host.getAppBase());
        if (!file.isAbsolute())
            file = new File(System.getProperty("catalina.base"),
                            host.getAppBase());
        try {
            appBase = file.getCanonicalFile();
        } catch (IOException e) {
            appBase = file;
        }
        return (appBase);

    }


    /**
     * Return a File object representing the "configuration root" directory
     * for our associated Host.
     */
    protected File configBase() {

        if (configBase != null) {
            return configBase;
        }

        File file = new File(System.getProperty("catalina.base"), "conf");
        Container parent = host.getParent();
        if ((parent != null) && (parent instanceof Engine)) {
            file = new File(file, parent.getName());
        }
        file = new File(file, host.getName());
        try {
            configBase = file.getCanonicalFile();
        } catch (IOException e) {
            configBase = file;
        }
        return (configBase);

    }


    /**
     * Deploy applications for any directories or WAR files that are found
     * in our "application root" directory.
     */
    protected void deployApps() {

        if (!(host instanceof Deployer))
            return;

        File appBase = appBase();
        if (!appBase.exists() || !appBase.isDirectory())
            return;
        File configBase = configBase();
        if (configBase.exists() && configBase.isDirectory()) {
            String configFiles[] = configBase.list();
            deployDescriptors(configBase, configFiles);
        }

        String files[] = appBase.list();
        deployWARs(appBase, files);
        deployDirectories(appBase, files);

    }


    /**
     * Deploy XML context descriptors.
     */
    protected void deployDescriptors(File configBase, String[] files) {

        if (!deployXML)
           return;

        for (int i = 0; i < files.length; i++) {

            if (files[i].equalsIgnoreCase("META-INF"))
                continue;
            if (files[i].equalsIgnoreCase("WEB-INF"))
                continue;
            if (deployed.contains(files[i]))
                continue;
            File dir = new File(configBase, files[i]);
            if (files[i].toLowerCase(Locale.ENGLISH).endsWith(".xml")) {

                deployed.add(files[i]);

                // Calculate the context path and make sure it is unique
                String file = files[i].substring(0, files[i].length() - 4);
                String contextPath = "/" + file.replace('_', '/');
                if (file.equals("ROOT")) {
                    contextPath = "";
                }

                // Assume this is a configuration descriptor and deploy it
                if (log.isLoggable(Level.FINE)) {
                    log.log(Level.FINE, DEPLOYING_CONFIG_DESCRIPTOR, files[i]);
                }
                try {
                    if (host.findChild(contextPath) != null) {
                        if ((deployed.contains(file))
                            || (deployed.contains(file + ".war"))) {
                            // If this is a newly added context file and 
                            // it overrides a context with a simple path, 
                            // that was previously deployed by the auto
                            // deployer, undeploy the context
                            ((Deployer) host).remove(contextPath);
                        } else {
                            continue;
                        }
                    }
                    URL config =
                        new URL("file", null, dir.getCanonicalPath());
                    ((Deployer) host).install(config, null);
                } catch (Throwable t) {
                    String msg = MessageFormat.format(rb.getString(ERROR_DEPLOYING_CONFIG_DESCRIPTOR_EXCEPTION),
                                                      files[i]);
                    log.log(Level.SEVERE, msg, t);
                }
            }
        }
    }


    /**
     * Deploy WAR files.
     */
    protected void deployWARs(File appBase, String[] files) {

        for (int i = 0; i < files.length; i++) {

            if (files[i].equalsIgnoreCase("META-INF"))
                continue;
            if (files[i].equalsIgnoreCase("WEB-INF"))
                continue;
            if (deployed.contains(files[i]))
                continue;
            File dir = new File(appBase, files[i]);
            if (files[i].toLowerCase(Locale.ENGLISH).endsWith(".war") && dir.isFile()
                    && !invalidWars.contains(files[i])) {

                deployed.add(files[i]);

                // Calculate the context path and make sure it is unique
                String contextPath = "/" + files[i];
                int period = contextPath.lastIndexOf(".");
                if (period >= 0)
                    contextPath = contextPath.substring(0, period);

                // Check for WARs with /../ /./ or similar sequences in the name
                if (!validateContextPath(appBase, contextPath)) {
                    log.log(Level.SEVERE, INVALID_WAR_NAME_EXCEPTION, files[i]);
                    invalidWars.add(files[i]);
                    continue;
                }

                if (contextPath.equals("/ROOT"))
                    contextPath = "";
                if (host.findChild(contextPath) != null)
                    continue;

                // Checking for a nested /META-INF/context.xml
                JarFile jar = null;
                JarEntry entry = null;
                InputStream istream = null;
                BufferedOutputStream ostream = null;
                File xml = new File
                    (configBase, files[i].substring
                     (0, files[i].lastIndexOf(".")) + ".xml");
                if (!xml.exists()) {
                    try {
                        jar = new JarFile(dir);
                        entry = jar.getJarEntry("META-INF/context.xml");
                        if (entry != null) {
                            istream = jar.getInputStream(entry);
                            ostream =
                                new BufferedOutputStream
                                (new FileOutputStream(xml), 1024);
                            byte buffer[] = new byte[1024];
                            while (true) {
                                int n = istream.read(buffer);
                                if (n < 0) {
                                    break;
                                }
                                ostream.write(buffer, 0, n);
                            }
                            ostream.flush();
                            ostream.close();
                            ostream = null;
                            istream.close();
                            istream = null;
                            entry = null;
                            jar.close();
                            jar = null;
                            deployDescriptors(configBase(), configBase.list());
                            return;
                        }
                    } catch (IOException e) {
                        // Ignore and continue
                    } finally {
                        if (ostream != null) {
                            try {
                                ostream.close();
                            } catch (Throwable t) {
                                ;
                            }
                            ostream = null;
                        }
                        if (istream != null) {
                            try {
                                istream.close();
                            } catch (Throwable t) {
                                ;
                            }
                            istream = null;
                        }
                        entry = null;
                        if (jar != null) {
                            try {
                                jar.close();
                            } catch (Throwable t) {
                                ;
                            }
                            jar = null;
                        }
                    }
                }

                if (isUnpackWARs()) {

                    // Expand and deploy this application as a directory
                    if (log.isLoggable(Level.FINE)) {
                        log.log(Level.FINE, EXPANDING_WEB_APP, files[i]);
                    }
                    URL url = null;
                    String path = null;
                    try {
                        url = new URL("jar:file:" +
                                      dir.getCanonicalPath() + "!/");
                        path = ExpandWar.expand(host, url);
                    } catch (IOException e) {
                        // JAR decompression failure
                        log.log(Level.WARNING, EXPANDING_WEB_APP_EXCEPTION, files[i]);
                        continue;
                    } catch (Throwable t) {
                        String msg = MessageFormat.format(rb.getString(EXPANDING_WEB_APP_ARCHIVE_EXCEPTION),
                                                          files[i]);
                        log.log(Level.SEVERE, msg, t);
                        continue;
                    }
                    try {
                        if (path != null) {
                            url = new URL("file:" + path);
                            ((Deployer) host).install(contextPath, url);
                        }
                    } catch (Throwable t) {
                        String msg = MessageFormat.format(rb.getString(EXPANDING_WEB_APP_ARCHIVE_EXCEPTION),
                                                          files[i]);
                        log.log(Level.SEVERE, msg, t);
                    }

                } else {

                    // Deploy the application in this WAR file
                    if (log.isLoggable(Level.INFO)) {
                        log.log(Level.INFO, DEPLOYING_WEB_APP_ARCHIVE, files[i]);
                    }
                    try {
                        URL url = new URL("file", null,
                                          dir.getCanonicalPath());
                        url = new URL("jar:" + url.toString() + "!/");
                        ((Deployer) host).install(contextPath, url);
                    } catch (Throwable t) {
                        String msg = MessageFormat.format(rb.getString(ERROR_DEPLOYING_WEB_APP_ARCHIVE_EXCEPTION),
                                                          files[i]);
                        log.log(Level.SEVERE, msg, t);
                    }
                }
            }
        }
    }


    /**
     * Deploy directories.
     */
    protected void deployDirectories(File appBase, String[] files) {

        for (int i = 0; i < files.length; i++) {

            if (files[i].equalsIgnoreCase("META-INF"))
                continue;
            if (files[i].equalsIgnoreCase("WEB-INF"))
                continue;
            if (deployed.contains(files[i]))
                continue;
            File dir = new File(appBase, files[i]);
            if (dir.isDirectory()) {

                deployed.add(files[i]);

                // Make sure there is an application configuration directory
                // This is needed if the Context appBase is the same as the
                // web server document root to make sure only web applications
                // are deployed and not directories for web space.
                File webInf = new File(dir, "/WEB-INF");
                if (!webInf.exists() || !webInf.isDirectory() ||
                    !webInf.canRead())
                    continue;

                // Calculate the context path and make sure it is unique
                String contextPath = "/" + files[i];
                if (files[i].equals("ROOT"))
                    contextPath = "";
                if (host.findChild(contextPath) != null)
                    continue;

                // Deploy the application in this directory
                if (log.isLoggable(Level.FINE)) {
                    log.log(Level.FINE, DEPLOYING_WEB_APP_DIR, files[i]);
                }
                long t1=System.currentTimeMillis();
                try {
                    URL url = new URL("file", null, dir.getCanonicalPath());
                    ((Deployer) host).install(contextPath, url);
                } catch (Throwable t) {
                    String msg = MessageFormat.format(rb.getString(ERROR_DEPLOYING_WEB_APP_DIR),
                                                      files[i]);
                    log.log(Level.SEVERE, msg, t);
                }
                long t2=System.currentTimeMillis();
                if( (t2-t1) > 200 && log.isLoggable(Level.FINE) )
                    log.log(Level.FINE, "Deployed " + files[i] + " " + (t2-t1));
            }

        }

    }


    private boolean validateContextPath(File appBase, String contextPath) {
        // More complicated than the ideal as the canonical path may or may
        // not end with File.separator for a directory
        
        StringBuilder docBase;
        String canonicalDocBase = null;
        
        try {
            String canonicalAppBase = appBase.getCanonicalPath();
            docBase = new StringBuilder(canonicalAppBase);
            if (canonicalAppBase.endsWith(File.separator)) {
                docBase.append(contextPath.substring(1).replace(
                        '/', File.separatorChar));
            } else {
                docBase.append(contextPath.replace('/', File.separatorChar));
            }
            // At this point docBase should be canonical but will not end
            // with File.separator
            
            canonicalDocBase =
                (new File(docBase.toString())).getCanonicalPath();
    
            // If the canonicalDocBase ends with File.separator, add one to
            // docBase before they are compared
            if (canonicalDocBase.endsWith(File.separator)) {
                docBase.append(File.separator);
            }
        } catch (IOException ioe) {
            return false;
        }
        
        // Compare the two. If they are not the same, the contextPath must
        // have /../ like sequences in it 
        return canonicalDocBase.equals(docBase.toString());
    }


    /**
     * Check deployment descriptors last modified date.
     */
    protected void checkContextLastModified() {

        if (!(host instanceof Deployer))
            return;

        Deployer deployer = (Deployer) host;

        String[] contextNames = deployer.findDeployedApps();

        for (int i = 0; i < contextNames.length; i++) {

            String contextName = contextNames[i];
            Context context = deployer.findDeployedApp(contextName);

            if (!(context instanceof Lifecycle))
                continue;

            try {
                DirContext resources = context.getResources();
                if (resources == null) {
                    // This can happen if there was an error initializing
                    // the context
                    continue;
                }
                ResourceAttributes webXmlAttributes = 
                    (ResourceAttributes) 
                    resources.getAttributes("/WEB-INF/web.xml");
                ResourceAttributes webInfAttributes = 
                    (ResourceAttributes) 
                    resources.getAttributes("/WEB-INF");
                long newLastModified = webXmlAttributes.getLastModified();
                long webInfLastModified = webInfAttributes.getLastModified();
                Long lastModified = webXmlLastModified.get(contextName);
                if (lastModified == null) {
                    webXmlLastModified.put
                        (contextName, Long.valueOf(newLastModified));
                } else {
                    if (lastModified.longValue() != newLastModified) {
                        if (newLastModified > (webInfLastModified + 5000)) {
                            webXmlLastModified.remove(contextName);
                            restartContext(context);
                        } else {
                            webXmlLastModified.put
                                (contextName, Long.valueOf(newLastModified));
                        }
                    }
                }
            } catch (NamingException e) {
                ; // Ignore
            }

            Long lastModified = contextXmlLastModified.get(contextName);
            String configBase = configBase().getPath();
            String configFileName = context.getConfigFile();
            if (configFileName != null) {
                File configFile = new File(configFileName);
                if (!configFile.isAbsolute()) {
                    configFile = new File(System.getProperty("catalina.base"),
                                          configFile.getPath());
                }
                long newLastModified = configFile.lastModified();
                if (lastModified == null) {
                    contextXmlLastModified.put
                        (contextName, Long.valueOf(newLastModified));
                } else {
                    if (lastModified.longValue() != newLastModified) {
                        contextXmlLastModified.remove(contextName);
                        String fileName = configFileName;
                        if (fileName.startsWith(configBase)) {
                            fileName = 
                                fileName.substring(configBase.length() + 1);
                            try {
                                deployed.remove(fileName);
                                if (host.findChild(contextName) != null) {
                                    ((Deployer) host).remove(contextName);
                                }
                            } catch (Throwable t) {
                                String msg = MessageFormat.format(rb.getString(ERROR_UNDEPLOYING_JAR_FILE_EXCEPTION),
                                                                  fileName);
                                log.log(Level.SEVERE, msg, t);
                            }
                            deployApps();
                        }
                    }
                }
            }

        }

        // Check for WAR modification
        if (isUnpackWARs()) {
            File appBase = appBase();
            if (!appBase.exists() || !appBase.isDirectory())
                return;
            String files[] = appBase.list();

            for (int i = 0; i < files.length; i++) {
                if (files[i].endsWith(".war")) {
                    File dir = new File(appBase, files[i]);
                    Long lastModified = warLastModified.get(files[i]);
                    long dirLastModified = dir.lastModified();
                    if (lastModified == null) {
                        warLastModified.put
                            (files[i], Long.valueOf(dir.lastModified()));
                    } else if (dirLastModified > lastModified.longValue()) {
                        // The WAR has been modified: redeploy
                        String expandedDir = files[i];
                        int period = expandedDir.lastIndexOf(".");
                        if (period >= 0)
                            expandedDir = expandedDir.substring(0, period);
                        File expanded = new File(appBase, expandedDir);
                        String contextPath = "/" + expandedDir;
                        if (contextPath.equals("/ROOT"))
                            contextPath = "";
                        if (dirLastModified > expanded.lastModified()) {
                            try {
                                // Undeploy current application
                                deployed.remove(files[i]);
                                deployed.remove(expandedDir + ".xml");
                                if (host.findChild(contextPath) != null) {
                                    ((Deployer) host).remove(contextPath, 
                                                             false);
                                    ExpandWar.deleteDir(expanded);
                                }
                            } catch (Throwable t) {
                                String msg = MessageFormat.format(rb.getString(ERROR_UNDEPLOYING_JAR_FILE_EXCEPTION),
                                                                  files[i]);
                                log.log(Level.SEVERE, msg, t);
                            }
                            deployApps();
                        }
                        // If deployment was successful, reset 
                        // the last modified values
                        if (host.findChild(contextPath) != null) {
                            webXmlLastModified.remove(contextPath);
                            warLastModified.put
                                (files[i], Long.valueOf(dir.lastModified()));
                        }
                    }
                }
            }
        }

    }


    protected boolean restartContext(Context context) {
        boolean result = true;
        if (log.isLoggable(Level.INFO)) {
            log.log(Level.INFO, RESTART_CONTEXT_INFO, context.getName());
        }

        /*
        try {
            StandardContext sctx=(StandardContext)context;
            sctx.reload();
        } catch( Exception ex ) {
            log.warn("Erorr stopping context " + context.getName()  +  " " +
                    ex.toString());
        }
        */
        try {
            ((Lifecycle) context).stop();
        } catch( Exception ex ) {
            String msg = MessageFormat.format(rb.getString(ERROR_DURING_CONTEXT_STOP_EXCEPTION),
                                              context.getName());
            log.log(Level.WARNING, msg, ex);
        }
        // if the context was not started ( for example an error in web.xml)
        // we'll still get to try to start
        try {
            ((Lifecycle) context).start();
        } catch (Exception e) {
            String msg = MessageFormat.format(rb.getString(ERROR_DURING_CONTEXT_RESTART_EXCEPTION),
                                              context.getName());
            log.log(Level.WARNING, msg, e);
            result = false;
        }
        
        return result;
    }


    /**
     * Expand the WAR file found at the specified URL into an unpacked
     * directory structure, and return the absolute pathname to the expanded
     * directory.
     *
     * @param war URL of the web application archive to be expanded
     *  (must start with "jar:")
     *
     * @exception IllegalArgumentException if this is not a "jar:" URL
     * @exception IOException if an input/output error was encountered
     *  during expansion
     */
    protected String expand(URL war) throws IOException {

        return ExpandWar.expand(host,war);
    }


    /**
     * Expand the specified input stream into the specified directory, creating
     * a file named from the specified relative path.
     *
     * @param input InputStream to be copied
     * @param docBase Document base directory into which we are expanding
     * @param name Relative pathname of the file to be created
     *
     * @exception IOException if an input/output error occurs
     */
    protected void expand(InputStream input, File docBase, String name)
        throws IOException {

        ExpandWar.expand(input,docBase,name);
    }


    /**
     * Log a message on the Logger associated with our Host (if any)
     *
     * @param message Message to be logged
     */
    protected void log(String message) {

        org.apache.catalina.Logger logger = null;
        if (host != null)
            logger = host.getLogger();
        if (logger != null)
            logger.log("HostConfig[" + host.getName() + "]: " + message);
        else
            log.info(message);
    }


    /**
     * Log a message on the Logger associated with our Host (if any)
     *
     * @param message Message to be logged
     * @param throwable Associated exception
     */
    protected void log(String message, Throwable throwable) {

        org.apache.catalina.Logger logger = null;
        if (host != null)
            logger = host.getLogger();
        if (logger != null)
            logger.log("HostConfig[" + host.getName() + "] "
                       + message, throwable);
        else {
            log.log(Level.SEVERE, message, throwable);
        }

    }


    /**
     * Process a "start" event for this Host.
     */
    public void start() {

        if (log.isLoggable(Level.FINE))
            log.log(Level.FINE, PROCESSING_START);
        if (host.getDeployOnStartup()) {
            deployApps();
        } else {
            // Deploy descriptors anyway (it should be equivalent to being
            // part of server.xml)
            File configBase = configBase();
            if (configBase.exists() && configBase.isDirectory()) {
                String configFiles[] = configBase.list();
                deployDescriptors(configBase, configFiles);
            }
        } 

    }


    /**
     * Process a "stop" event for this Host.
     */
    public void stop() {

        if (log.isLoggable(Level.FINE))
            log.log(Level.FINE, PROCESSING_STOP);
        undeployApps();

        appBase = null;
        configBase = null;

    }


    /**
     * Undeploy all deployed applications.
     */
    protected void undeployApps() {

        if (!(host instanceof Deployer))
            return;
        if (log.isLoggable(Level.FINE))
            log.log(Level.FINE, UNDEPLOYING_WEB_APP);

        String contextPaths[] = ((Deployer) host).findDeployedApps();
        for (int i = 0; i < contextPaths.length; i++) {
            if (log.isLoggable(Level.FINE)) {
                log.log(Level.FINE, UNDEPLOYING_CONTEXT, contextPaths[i]);
            }
            try {
                ((Deployer) host).remove(contextPaths[i]);
            } catch (Throwable t) {
                String msg = MessageFormat.format(rb.getString(ERROR_UNDEPLOYING_WEB_APP_EXCEPTION),
                                                  contextPaths[i]);
                log.log(Level.SEVERE, msg, t);
            }
        }

        webXmlLastModified.clear();
        deployed.clear();

    }


    /**
     * Deploy webapps.
     */
    protected void check() {

        if (host.getAutoDeploy()) {
            // Deploy apps if the Host allows auto deploying
            deployApps();
            // Check for web.xml modification
            checkContextLastModified();
        }

    }


}
