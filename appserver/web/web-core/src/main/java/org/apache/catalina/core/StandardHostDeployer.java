/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2016 Oracle and/or its affiliates. All rights reserved.
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

package org.apache.catalina.core;


import org.apache.catalina.*;
import org.apache.catalina.startup.ContextRuleSet;
import org.apache.catalina.startup.ExpandWar;
import org.apache.catalina.startup.NamingRuleSet;
import org.apache.tomcat.util.digester.Digester;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * <p>Implementation of <b>Deployer</b> that is delegated to by the
 * <code>StandardHost</code> implementation class.</p>
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.4 $ $Date: 2006/03/12 01:27:01 $
 */

public class StandardHostDeployer implements Deployer {

    private static final Logger log = LogFacade.getLogger();
    private static final ResourceBundle rb = log.getResourceBundle();

    // ----------------------------------------------------------- Constructors

    public StandardHostDeployer() {
    }

    /**
     * Create a new StandardHostDeployer associated with the specified
     * StandardHost.
     *
     * @param host The StandardHost we are associated with
     */
    public StandardHostDeployer(StandardHost host) {

        super();
        this.host = host;

    }


    // ----------------------------------------------------- Instance Variables


    /**
     * The <code>ContextRuleSet</code> associated with our
     * <code>digester</code> instance.
     */
    private ContextRuleSet contextRuleSet = null;


     /**
     * The <code>Digester</code> instance to use for deploying web applications
     * to this <code>Host</code>.  <strong>WARNING</strong> - Usage of this
     * instance must be appropriately synchronized to prevent simultaneous
     * access by multiple threads.
     */
    private Digester digester = null;


    /**
     * The <code>StandardHost</code> instance we are associated with.
     */
    protected StandardHost host = null;


    /**
     * The <code>NamingRuleSet</code> associated with our
     * <code>digester</code> instance.
     */
    private NamingRuleSet namingRuleSet = null;


    /**
     * The document base which should replace the value specified in the
     * <code>Context</code> being added in the <code>addChild()</code> method,
     * or <code>null</code> if the original value should remain untouched.
     */
    private String overrideDocBase = null;


    /**
     * The config file which should replace the value set for the config file
     * of the <code>Context</code>being added in the <code>addChild()</code> 
     * method, or <code>null</code> if the original value should remain 
     * untouched.
     */
    private String overrideConfigFile = null;


    // -------------------------------------------------------- Depoyer Methods

    public Host getHost() {
        return host;
    }

    public void setHost(Host host) {
        this.host = (StandardHost)host;
    }

    /**
     * Return the name of the Container with which this Deployer is associated.
     */
    public String getName() {

        return (host.getName());

    }


    /**
     * Install a new web application, whose web application archive is at the
     * specified URL, into this container with the specified context path.
     * A context path of "" (the empty string) should be used for the root
     * application for this container.  Otherwise, the context path must
     * start with a slash.
     * <p>
     * If this application is successfully installed, a ContainerEvent of type
     * <code>PRE_INSTALL_EVENT</code> will be sent to registered listeners
     * before the associated Context is started, and a ContainerEvent of type
     * <code>INSTALL_EVENT</code> will be sent to all registered listeners
     * after the associated Context is started, with the newly created
     * <code>Context</code> as an argument.
     *
     * @param contextPath The context path to which this application should
     *  be installed (must be unique)
     * @param war A URL of type "jar:" that points to a WAR file, or type
     *  "file:" that points to an unpacked directory structure containing
     *  the web application to be installed
     *
     * @exception IllegalArgumentException if the specified context path
     *  is malformed (it must be "" or start with a slash)
     * @exception IllegalStateException if the specified context path
     *  is already attached to an existing web application
     * @exception IOException if an input/output error was encountered
     *  during installation
     */
    public synchronized void install(String contextPath, URL war)
        throws IOException {

        // Validate the format and state of our arguments
        if (contextPath == null)
            throw new IllegalArgumentException
                    (rb.getString(LogFacade.CONTEXT_PATH_REQUIRED_EXCEPTION));
        if (!contextPath.equals("") && !contextPath.startsWith("/")) {
            String msg = MessageFormat.format(rb.getString(LogFacade.INVALID_CONTEXT_PATH_EXCEPTION), contextPath);
            throw new IllegalArgumentException(msg);
        }
        if (findDeployedApp(contextPath) != null)
        {
            String msg = MessageFormat.format(rb.getString(LogFacade.CONTEXT_PATH_ALREADY_USED_EXCEPTION), contextPath);
            throw new IllegalStateException(msg);
        }
        if (war == null)
            throw new IllegalArgumentException
                    (rb.getString(LogFacade.URL_WEB_APP_ARCHIVE_REQUIRED_EXCEPTION));

        // Calculate the document base for the new web application
        if (log.isLoggable(Level.INFO)) {
            log.log(Level.INFO, LogFacade.INSTALLING_WEB_APP_INFO, new Object[] {contextPath, war.toString()});
        }
        String url = war.toString();
        String docBase = null;
        boolean isWAR = false;
        if (url.startsWith("jar:")) {
            url = url.substring(4, url.length() - 2);
            if (!url.toLowerCase(Locale.ENGLISH).endsWith(".war")) {
                String msg = MessageFormat.format(rb.getString(LogFacade.INVALID_URL_WEB_APP_EXCEPTION), url);
                throw new IllegalArgumentException(msg);
            }
            isWAR = true;
        }
        if (url.startsWith("file://"))
            docBase = url.substring(7);
        else if (url.startsWith("file:"))
            docBase = url.substring(5);
        else {
            String msg = MessageFormat.format(rb.getString(LogFacade.INVALID_URL_WEB_APP_EXCEPTION), url);
            throw new IllegalArgumentException(msg);
        }

        // Determine if directory/war to install is in the host appBase
        boolean isAppBase = false;
        File appBase = new File(host.getAppBase());
        if (!appBase.isAbsolute())
            appBase = new File(System.getProperty("catalina.base"),
                            host.getAppBase());
        File contextFile = new File(docBase);
        File baseDir = contextFile.getParentFile();
        if (appBase.getCanonicalPath().equals(baseDir.getCanonicalPath())) {
            isAppBase = true;
        }

        // For security, if deployXML is false only allow directories
        // and war files from the hosts appBase
        if (!host.isDeployXML() && !isAppBase) {
            String msg = MessageFormat.format(rb.getString(LogFacade.HOST_WEB_APP_DIR_CAN_BE_INSTALLED_EXCEPTION), url);
            throw new IllegalArgumentException(msg);
        }

        // Make sure contextPath and directory/war names match when
        // installing from the host appBase
        if (isAppBase && host.getAutoDeploy()) {
            String filename = contextFile.getName();
            if (isWAR) {
                filename = filename.substring(0,filename.length()-4);
            }
            if (contextPath.length() == 0) {
                if (!filename.equals("ROOT")) {
                    String msg = MessageFormat.format(rb.getString(LogFacade.CONSTEXT_PATH_MATCH_DIR_WAR_NAME_EXCEPTION),
                                                      new Object[] {"/", "ROOT"});
                    throw new IllegalArgumentException(msg);
                }
            } else if (!filename.equals(contextPath.substring(1))) {
                String msg = MessageFormat.format(rb.getString(LogFacade.CONSTEXT_PATH_MATCH_DIR_WAR_NAME_EXCEPTION),
                                                  new Object[] {contextPath, filename});
                throw new IllegalArgumentException(msg);
            }
        }

        // Expand war file if host wants wars unpacked
        if (isWAR && host.isUnpackWARs()) {
            docBase = ExpandWar.expand(host, war, contextPath);
        }

        // Install the new web application
        try {
            Class clazz = Class.forName(host.getContextClass());
            Context context = (Context) clazz.newInstance();
            context.setPath(contextPath);
            context.setDocBase(docBase);
            if (context instanceof Lifecycle) {
                clazz = Class.forName(host.getConfigClass());
                LifecycleListener listener =
                    (LifecycleListener) clazz.newInstance();
                ((Lifecycle) context).addLifecycleListener(listener);
            }
            host.fireContainerEvent(PRE_INSTALL_EVENT, context);
            host.addChild(context);
            host.fireContainerEvent(INSTALL_EVENT, context);
        } catch (ClassNotFoundException e) {
            log.log(Level.INFO, "", e);
        } catch (Exception e) {
            log.log(Level.INFO, LogFacade.ERROR_INSTALLING_EXCEPTION, e);
            throw new IOException(e.toString());
        }

    }


    /**
     * Install a new web application, whose web application archive is at the
     * specified URL, into this container with the specified context path.
     * A context path of "" (the empty string) should be used for the root
     * application for this container.  Otherwise, the context path must
     * start with a slash.
     * <p>
     * If this application is successfully installed, a ContainerEvent of type
     * <code>PRE_INSTALL_EVENT</code> will be sent to registered listeners
     * before the associated Context is started, and a ContainerEvent of type
     * <code>INSTALL_EVENT</code> will be sent to all registered listeners
     * after the associated Context is started, with the newly created
     * <code>Context</code> as an argument.
     *
     * @param contextPath The context path to which this application should
     *  be installed (must be unique)
     * @param war A URL of type "jar:" that points to a WAR file, or type
     *  "file:" that points to an unpacked directory structure containing
     *  the web application to be installed
     * @param configFile The path to a file to save the Context information.
     *  If configFile is null, the Context information is saved in server.xml;
     *  if it is NOT null, the Context information is saved in configFile.
     *
     * @exception IllegalArgumentException if the specified context path
     *  is malformed (it must be "" or start with a slash)
     * @exception IllegalStateException if the specified context path
     *  is already attached to an existing web application
     * @exception IOException if an input/output error was encountered
     *  during installation
     */
    public synchronized void install(String contextPath, URL war,
        String configFile) throws IOException {

        // Validate the format and state of our arguments
        if (contextPath == null)
            throw new IllegalArgumentException
                    (rb.getString(LogFacade.CONTEXT_PATH_REQUIRED_EXCEPTION));
        if (!contextPath.equals("") && !contextPath.startsWith("/")) {
            String msg = MessageFormat.format(rb.getString(LogFacade.INVALID_CONTEXT_PATH_EXCEPTION), contextPath);
            throw new IllegalArgumentException(msg);
        }
        if (findDeployedApp(contextPath) != null) {
            String msg = MessageFormat.format(rb.getString(LogFacade.CONTEXT_PATH_ALREADY_USED_EXCEPTION), contextPath);
            throw new IllegalStateException(msg);
        }
        if (war == null)
            throw new IllegalArgumentException
                    (rb.getString(LogFacade.URL_WEB_APP_ARCHIVE_REQUIRED_EXCEPTION));

        // Calculate the document base for the new web application
        if (log.isLoggable(Level.INFO)) {
            log.log(Level.INFO, LogFacade.INSTALLING_WEB_APP_INFO, new Object[] {contextPath, war.toString()});
        }
        String url = war.toString();
        String docBase = null;
        boolean isWAR = false;
        if (url.startsWith("jar:")) {
            url = url.substring(4, url.length() - 2);
            if (!url.toLowerCase(Locale.ENGLISH).endsWith(".war")) {
                String msg = MessageFormat.format(rb.getString(LogFacade.INVALID_URL_WEB_APP_EXCEPTION), url);
                throw new IllegalArgumentException(msg);
            }
            isWAR = true;
        }
        if (url.startsWith("file://"))
            docBase = url.substring(7);
        else if (url.startsWith("file:"))
            docBase = url.substring(5);
        else {
            String msg = MessageFormat.format(rb.getString(LogFacade.INVALID_URL_WEB_APP_EXCEPTION), url);
            throw new IllegalArgumentException(msg);
        }

        // Expand war file if host wants wars unpacked
        if (isWAR && host.isUnpackWARs()) {
            docBase = ExpandWar.expand(host, war, contextPath);
        }

        // Install the new web application
        try {
            Class clazz = Class.forName(host.getContextClass());
            Context context = (Context) clazz.newInstance();
            context.setPath(contextPath);
            context.setDocBase(docBase);
            context.setConfigFile(configFile);
            if (context instanceof Lifecycle) {
                clazz = Class.forName(host.getConfigClass());
                LifecycleListener listener =
                    (LifecycleListener) clazz.newInstance();
                ((Lifecycle) context).addLifecycleListener(listener);
            }
            host.fireContainerEvent(PRE_INSTALL_EVENT, context);
            host.addChild(context);
            host.fireContainerEvent(INSTALL_EVENT, context);

            // save context info into configFile
            //Engine engine = (Engine)host.getParent();
            //StandardServer server = (StandardServer) engine.getService().getServer();
            //server.storeContext(context);
        } catch (Exception e) {
            String msg = MessageFormat.format(rb.getString(LogFacade.ERROR_DEPLOYING_APP_CONTEXT_PATH_EXCEPTION), contextPath);
            log.log(Level.SEVERE, msg, e);
            throw new IOException(e.toString());
        }

    }


    /**
     * <p>Install a new web application, whose context configuration file
     * (consisting of a <code>&lt;Context&gt;</code> element) and (optional)
     * web application archive are at the specified URLs.</p>
     *
     * If this application is successfully installed, a ContainerEvent of type
     * <code>PRE_INSTALL_EVENT</code> will be sent to registered listeners
     * before the associated Context is started, and a ContainerEvent of type
     * <code>INSTALL_EVENT</code> will be sent to all registered listeners
     * after the associated Context is started, with the newly created
     * <code>Context</code> as an argument.
     *
     * @param config A URL that points to the context configuration descriptor
     *  to be used for configuring the new Context
     * @param war A URL of type "jar:" that points to a WAR file, or type
     *  "file:" that points to an unpacked directory structure containing
     *  the web application to be installed, or <code>null</code> to use
     *  the <code>docBase</code> attribute from the configuration descriptor
     *
     * @exception IllegalArgumentException if one of the specified URLs is
     *  null
     * @exception IllegalStateException if the context path specified in the
     *  context configuration file is already attached to an existing web
     *  application
     * @exception IOException if an input/output error was encountered
     *  during installation
     */
    public synchronized void install(URL config, URL war) throws IOException {

        // Validate the format and state of our arguments
        if (config == null)
            throw new IllegalArgumentException
                    (rb.getString(LogFacade.URL_CONFIG_FILE_REQUIRED_EXCEPTION));

        if (!host.isDeployXML())
            throw new IllegalArgumentException
                    (rb.getString(LogFacade.USE_CONFIG_FILE_NOT_ALLOWED));

        if (log.isLoggable(Level.INFO)) {
            log.log(Level.INFO, LogFacade.PROCESSING_CONTEXT_CONFIG_INFO, config);
        }

        // Calculate the document base for the new web application (if needed)
        String docBase = null; // Optional override for value in config file
        boolean isWAR = false;
        if (war != null) {
            String url = war.toString();
            if (log.isLoggable(Level.INFO)) {
                log.log(Level.INFO, LogFacade.INSTALLING_WEB_APP_FROM_URL_INFO, url);
            }
            // Calculate the WAR file absolute pathname
            if (url.startsWith("jar:")) {
                url = url.substring(4, url.length() - 2);
                isWAR = true;
            }
            if (url.startsWith("file://"))
                docBase = url.substring(7);
            else if (url.startsWith("file:"))
                docBase = url.substring(5);
            else
                throw new IllegalArgumentException
                        (rb.getString(LogFacade.INVALID_URL_WEB_APP_EXCEPTION));

        }

        // Expand war file if host wants wars unpacked
        if (isWAR && host.isUnpackWARs()) {
            docBase = ExpandWar.expand(host, war);
        }

        // Install the new web application
        this.overrideDocBase = docBase;
        if (config.toString().startsWith("file:")) {
            this.overrideConfigFile = config.getFile();
        }

        InputStream stream = null;
        try {
            stream = config.openStream();
            Digester digester = createDigester();
            digester.setDebug(host.getDebug());
            digester.setClassLoader(this.getClass().getClassLoader());
            digester.clear();
            digester.push(this);
            digester.parse(stream);
            stream.close();
            stream = null;
        } catch (Exception e) {
            String msg = MessageFormat.format(rb.getString(LogFacade.ERROR_DEPLOYING_APP_CONTEXT_PATH_EXCEPTION), docBase);
            host.log(msg, e);
            throw new IOException(e.toString());
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (Throwable t) {
                    ;
                }
            }
            this.overrideDocBase = null;
            this.overrideConfigFile = null;
        }

    }


    /**
     * Return the Context for the deployed application that is associated
     * with the specified context path (if any); otherwise return
     * <code>null</code>.
     *
     * @param contextPath The context path of the requested web application
     */
    public Context findDeployedApp(String contextPath) {

        return ((Context) host.findChild(contextPath));

    }


    /**
     * Return the context paths of all deployed web applications in this
     * Container.  If there are no deployed applications, a zero-length
     * array is returned.
     */
    public String[] findDeployedApps() {

        Container children[] = host.findChildren();
        String results[] = new String[children.length];
        for (int i = 0; i < children.length; i++)
            results[i] = children[i].getName();
        return (results);

    }


    /**
     * Remove an existing web application, attached to the specified context
     * path.  If this application is successfully removed, a
     * ContainerEvent of type <code>REMOVE_EVENT</code> will be sent to all
     * registered listeners, with the removed <code>Context</code> as
     * an argument.
     *
     * @param contextPath The context path of the application to be removed
     *
     * @exception IllegalArgumentException if the specified context path
     *  is malformed (it must be "" or start with a slash)
     * @exception IllegalArgumentException if the specified context path does
     *  not identify a currently installed web application
     * @exception IOException if an input/output error occurs during
     *  removal
     */
    public void remove(String contextPath) throws IOException {

        // Validate the format and state of our arguments
        if (contextPath == null)
            throw new IllegalArgumentException
                    (rb.getString(LogFacade.CONTEXT_PATH_REQUIRED_EXCEPTION));
        if (!contextPath.equals("") && !contextPath.startsWith("/")) {
            String msg = MessageFormat.format(rb.getString(LogFacade.INVALID_CONTEXT_PATH_EXCEPTION), contextPath);
            throw new IllegalArgumentException(msg);
        }

        // Locate the context and associated work directory
        Context context = findDeployedApp(contextPath);
        if (context == null) {
            String msg = MessageFormat.format(rb.getString(LogFacade.CONTEXT_PATH_NOT_IN_USE), contextPath);
            throw new IllegalArgumentException(msg);
        }

        // Remove this web application
        if (log.isLoggable(Level.INFO)) {
            log.log(Level.INFO, LogFacade.REMOVING_WEB_APP_INFO, contextPath);
        }
        try {
            host.removeChild(context);
            host.fireContainerEvent(REMOVE_EVENT, context);
        } catch (Exception e) {
            String msg = MessageFormat.format(rb.getString(LogFacade.ERROR_REMOVING_APP_EXCEPTION), contextPath);
            log.log(Level.SEVERE, msg, e);
            throw new IOException(e.toString());
        }

    }


    /**
     * Remove an existing web application, attached to the specified context
     * path.  If this application is successfully removed, a
     * ContainerEvent of type <code>REMOVE_EVENT</code> will be sent to all
     * registered listeners, with the removed <code>Context</code> as
     * an argument. Deletes the web application war file and/or directory
     * if they exist in the Host's appBase.
     *
     * @param contextPath The context path of the application to be removed
     * @param undeploy boolean flag to remove web application from server
     *
     * @exception IllegalArgumentException if the specified context path
     *  is malformed (it must be "" or start with a slash)
     * @exception IllegalArgumentException if the specified context path does
     *  not identify a currently installed web application
     * @exception IOException if an input/output error occurs during
     *  removal
     */
    public void remove(String contextPath, boolean undeploy)
        throws IOException {

        // Validate the format and state of our arguments
        if (contextPath == null)
            throw new IllegalArgumentException
                    (rb.getString(LogFacade.CONTEXT_PATH_REQUIRED_EXCEPTION));
        if (!contextPath.equals("") && !contextPath.startsWith("/")) {
            String msg = MessageFormat.format(rb.getString(LogFacade.INVALID_CONTEXT_PATH_EXCEPTION), contextPath);
            throw new IllegalArgumentException(msg);
        }

        // Locate the context and associated work directory
        Context context = findDeployedApp(contextPath);
        if (context == null) {
            String msg = MessageFormat.format(rb.getString(LogFacade.CONTEXT_PATH_NOT_IN_USE), contextPath);
            throw new IllegalArgumentException(msg);
        }

        // Remove this web application
        String msgInfo = MessageFormat.format(rb.getString(LogFacade.REMOVING_WEB_APP_INFO), contextPath);
        host.log(msgInfo);
        try {
            // Get the work directory for the Context
            File workDir = 
                (File) context.getServletContext().getAttribute
                (ServletContext.TEMPDIR);
            String configFile = context.getConfigFile();
            host.removeChild(context);

            if (undeploy) {
                // Remove the web application directory and/or war file if it
                // exists in the Host's appBase directory.

                // Determine if directory/war to remove is in the host appBase
                boolean isAppBase = false;
                File appBase = new File(host.getAppBase());
                if (!appBase.isAbsolute())
                    appBase = new File(System.getProperty("catalina.base"),
                                       host.getAppBase());
                File contextFile = new File(context.getDocBase());
                File baseDir = contextFile.getParentFile();
                if ((baseDir == null) 
                    || (appBase.getCanonicalPath().equals
                        (baseDir.getCanonicalPath()))) {
                    isAppBase = true;
                }

                boolean isWAR = false;
                if (contextFile.getName().toLowerCase(Locale.ENGLISH).endsWith(".war")) {
                    isWAR = true;
                }
                // Only remove directory and/or war if they are located in the
                // Host's appBase autoDeploy is true
                if (isAppBase && host.getAutoDeploy()) {
                    String filename = contextFile.getName();
                    if (isWAR) {
                        filename = filename.substring(0,filename.length()-4);
                    }
                    if (contextPath.length() == 0 && filename.equals("ROOT") ||
                        filename.equals(contextPath.substring(1))) {
                        if (!isWAR) {
                            long contextLastModified = 
                                contextFile.lastModified();
                            if (contextFile.isDirectory()) {
                                deleteDir(contextFile);
                            }
                            if (host.isUnpackWARs()) {
                                File contextWAR = 
                                    new File(context.getDocBase() + ".war");
                                if (contextWAR.exists()) {
                                    if (contextLastModified 
                                        > contextWAR.lastModified()) {
                                        deleteFile(contextWAR);
                                    }
                                }
                            }
                        } else {
                            deleteFile(contextFile);
                        }
                    }
                    if (host.isDeployXML() && (configFile != null)) {
                        File docBaseXml = new File(configFile);
                        deleteFile(docBaseXml);
                    }
                }

                // Remove the work directory for the Context
                if (workDir == null &&
                    context instanceof StandardContext &&
                    ((StandardContext)context).getWorkDir() != null) {
                    workDir = new File(((StandardContext)context).getWorkPath());
                }
                if (workDir != null && workDir.exists()) {
                    deleteDir(workDir);
                }
            }

            host.fireContainerEvent(REMOVE_EVENT, context);
        } catch (Exception e) {
            String msgException = MessageFormat.format(rb.getString(LogFacade.ERROR_REMOVING_APP_EXCEPTION), contextPath);
            host.log(msgException, e);
            throw new IOException(e.toString());
        }

    }


    /**
     * Start an existing web application, attached to the specified context
     * path.  Only starts a web application if it is not running.
     *
     * @param contextPath The context path of the application to be started
     *
     * @exception IllegalArgumentException if the specified context path
     *  is malformed (it must be "" or start with a slash)
     * @exception IllegalArgumentException if the specified context path does
     *  not identify a currently installed web application
     * @exception IOException if an input/output error occurs during
     *  startup
     */
    public void start(String contextPath) throws IOException {

        // Validate the format and state of our arguments
        if (contextPath == null)
            throw new IllegalArgumentException
                    (rb.getString(LogFacade.CONTEXT_PATH_REQUIRED_EXCEPTION));
        if (!contextPath.equals("") && !contextPath.startsWith("/")) {
            String msg = MessageFormat.format(rb.getString(LogFacade.INVALID_CONTEXT_PATH_EXCEPTION), contextPath);
            throw new IllegalArgumentException(msg);
        }
        Context context = findDeployedApp(contextPath);
        if (context == null) {
            String msg = MessageFormat.format(rb.getString(LogFacade.CONTEXT_PATH_NOT_IN_USE), contextPath);
            throw new IllegalArgumentException(msg);
        }
        if (log.isLoggable(Level.INFO)) {
            log.log(Level.INFO, LogFacade.STARTING_WEB_APP_INFO, contextPath);
        }
        try {
            ((Lifecycle) context).start();
        } catch (LifecycleException e) {
            String msg = MessageFormat.format(rb.getString(LogFacade.STARTING_WEB_APP_FAILED_EXCEPTION), contextPath);

            log.log(Level.SEVERE, msg, e);
            throw new IllegalStateException(msg, e);
        }
    }


    /**
     * Stop an existing web application, attached to the specified context
     * path.  Only stops a web application if it is running.
     *
     * @param contextPath The context path of the application to be stopped
     *
     * @exception IllegalArgumentException if the specified context path
     *  is malformed (it must be "" or start with a slash)
     * @exception IllegalArgumentException if the specified context path does
     *  not identify a currently installed web application
     * @exception IOException if an input/output error occurs while stopping
     *  the web application
     */
    public void stop(String contextPath) throws IOException {

        // Validate the format and state of our arguments
        if (contextPath == null)
            throw new IllegalArgumentException
                    (rb.getString(LogFacade.CONTEXT_PATH_REQUIRED_EXCEPTION));
        if (!contextPath.equals("") && !contextPath.startsWith("/")) {
            String msg = MessageFormat.format(rb.getString(LogFacade.INVALID_CONTEXT_PATH_EXCEPTION), contextPath);
            throw new IllegalArgumentException(msg);
        }
        Context context = findDeployedApp(contextPath);
        if (context == null) {
            String msg = MessageFormat.format(rb.getString(LogFacade.CONTEXT_PATH_NOT_IN_USE), contextPath);
            throw new IllegalArgumentException(msg);
        }
        if (log.isLoggable(Level.INFO)) {
            log.log(Level.INFO, LogFacade.STOPPING_WEB_APP_INFO, contextPath);

        }
        try {
            ((Lifecycle) context).stop();
        } catch (LifecycleException e) {
            String msg = MessageFormat.format(rb.getString(LogFacade.STOPPING_WEB_APP_FAILED_EXCEPTION), contextPath);
            log.log(Level.SEVERE, msg, e);
            throw new IllegalStateException(msg, e);
        }

    }


    // ------------------------------------------------------ Delegated Methods


    /**
     * Delegate a request to add a child Context to our associated Host.
     *
     * @param child The child Context to be added
     */
    public void addChild(Container child) {

        Context context = null;
        String contextPath = null;
        if (child instanceof Context) {
            context = (Context) child;
            contextPath = context.getPath();
        }
        if (contextPath == null)
            throw new IllegalArgumentException
                    (rb.getString(LogFacade.CONTEXT_PATH_REQUIRED_EXCEPTION));
        else if (!contextPath.equals("") && !contextPath.startsWith("/")) {
            String msg = MessageFormat.format(rb.getString(LogFacade.INVALID_CONTEXT_PATH_EXCEPTION), contextPath);
            throw new IllegalArgumentException(msg);
        }
        if (host.findChild(contextPath) != null) {
            String msg = MessageFormat.format(rb.getString(LogFacade.CONTEXT_PATH_ALREADY_USED_EXCEPTION), contextPath);
            throw new IllegalStateException(msg);
        }
        if (this.overrideDocBase != null)
            context.setDocBase(this.overrideDocBase);
        if (this.overrideConfigFile != null)
            context.setConfigFile(this.overrideConfigFile);
        host.fireContainerEvent(PRE_INSTALL_EVENT, context);
        host.addChild(child);
        host.fireContainerEvent(INSTALL_EVENT, context);

    }


    /**
     * Delegate a request for the parent class loader to our associated Host.
     */
    public ClassLoader getParentClassLoader() {

        return (host.getParentClassLoader());

    }


    // ------------------------------------------------------ Protected Methods


    /**
     * Create (if necessary) and return a Digester configured to process the
     * context configuration descriptor for an application.
     */
    protected Digester createDigester() {
        if (digester == null) {
            digester = new Digester();
            if (host.getDebug() > 0)
                digester.setDebug(3);
            digester.setValidating(false);
            contextRuleSet = new ContextRuleSet("");
            digester.addRuleSet(contextRuleSet);
            namingRuleSet = new NamingRuleSet("Context/");
            digester.addRuleSet(namingRuleSet);
        }
        return (digester);

    }


    /**
     * Delete the specified directory, including all of its contents and
     * subdirectories recursively.
     *
     * @param dir File object representing the directory to be deleted
     */
    protected void deleteDir(File dir) {

        String files[] = dir.list();
        if (files == null) {
            files = new String[0];
        }
        for (int i = 0; i < files.length; i++) {
            File file = new File(dir, files[i]);
            if (file.isDirectory()) {
                deleteDir(file);
            } else {
                deleteFile(file);
            }
        }
        deleteFile(dir);

    }

    protected void deleteFile(File dir) {
        if (!dir.delete()) {
            log.log(Level.WARNING, LogFacade.FAILED_REMOVE_FILE, dir.getAbsolutePath());
        }
    }

}
