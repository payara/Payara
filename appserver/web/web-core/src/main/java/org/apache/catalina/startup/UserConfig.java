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
import org.apache.catalina.Logger;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.util.StringManager;
import org.apache.tomcat.util.digester.ObjectParamRule;
import org.glassfish.logging.annotation.LogMessageInfo;

import java.io.File;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.ResourceBundle;
import java.util.logging.*;

/**
 * Startup event listener for a <b>Host</b> that configures Contexts (web
 * applications) for all defined "users" who have a web application in a
 * directory with the specified name in their home directories.  The context
 * path of each deployed application will be set to <code>~xxxxx</code>, where
 * xxxxx is the username of the owning user for that web application
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.2 $ $Date: 2005/12/08 01:28:10 $
 */

public final class UserConfig
    implements LifecycleListener {

    // ----------------------------------------------------- Static Variables

    private static final java.util.logging.Logger log = StandardServer.log;

    private static final ResourceBundle rb = log.getResourceBundle();

    @LogMessageInfo(
            message = "Deploying user web applications",
            level = "INFO"
    )
    public static final String DEPLOYING_USER_WEB_APP_INFO = "AS-WEB-CORE-00476";

    @LogMessageInfo(
            message = "Exception loading user database",
            level = "WARNING"
    )
    public static final String LOADING_USER_DATABASE_EXCEPTION = "AS-WEB-CORE-00477";

    @LogMessageInfo(
            message = "Deploying web application for user {0}",
            level = "INFO"
    )
    public static final String DEPLOYING_WEB_APP_FOR_USER_INFO = "AS-WEB-CORE-00478";

    @LogMessageInfo(
            message = "Error deploying web application for user {0}",
            level = "WARNING"
    )
    public static final String DEPLOYING_WEB_APP_FOR_USER_EXCEPTION = "AS-WEB-CORE-00479";

    @LogMessageInfo(
            message = "UserConfig[{0}]: {1}",
            level = "INFO"
    )
    public static final String USER_CONFIG = "AS-WEB-CORE-00480";

    @LogMessageInfo(
            message = "UserConfig[null]: {0}",
            level = "INFO"
    )
    public static final String USER_CONFIG_NULL = "AS-WEB-CORE-00481";

    @LogMessageInfo(
            message = "UserConfig: Processing START",
            level = "INFO"
    )
    public static final String PROCESSING_START_INFO = "AS-WEB-CORE-00482";

    @LogMessageInfo(
            message = "UserConfig: Processing STOP",
            level = "INFO"
    )
    public static final String PROCESSING_STOP_INFO = "AS-WEB-CORE-00483";


    // ----------------------------------------------------- Instance Variables

    /**
     * The Java class name of the Context configuration class we should use.
     */
    private String configClass = ContextConfig.class.getName();

    /**
     * The Java class name of the Context implementation we should use.
     */
    private String contextClass = StandardContext.class.getName();

    /**
     * The debugging detail level for this component.
     */
    private int debug = 999;

    /**
     * The directory name to be searched for within each user home directory.
     */
    private String directoryName = "public_html";

    /**
     * The base directory containing user home directories.
     */
    private String homeBase = null;

    /**
     * The Host we are associated with.
     */
    private Host host = null;

    /**
     * The Java class name of the user database class we should use.
     */
    private String userClass =
        "org.apache.catalina.startup.PasswdUserDatabase";


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
     * Return the directory name for user web applications.
     */
    public String getDirectoryName() {
        return (this.directoryName);
    }


    /**
     * Set the directory name for user web applications.
     *
     * @param directoryName The new directory name
     */
    public void setDirectoryName(String directoryName) {
        this.directoryName = directoryName;
    }


    /**
     * Return the base directory containing user home directories.
     */
    public String getHomeBase() {
        return (this.homeBase);
    }


    /**
     * Set the base directory containing user home directories.
     *
     * @param homeBase The new base directory
     */
    public void setHomeBase(String homeBase) {
        this.homeBase = homeBase;
    }


    /**
     * Return the user database class name for this component.
     */
    public String getUserClass() {
        return (this.userClass);
    }


    /**
     * Set the user database class name for this component.
     */
    public void setUserClass(String userClass) {
        this.userClass = userClass;
    }


    // --------------------------------------------------------- Public Methods


    /**
     * Process the START event for an associated Host.
     *
     * @param event The lifecycle event that has occurred
     */
    public void lifecycleEvent(LifecycleEvent event) {

        // Identify the host we are associated with
        try {
            host = (Host) event.getLifecycle();
        } catch (ClassCastException e) {
            String msg = MessageFormat.format(rb.getString(HostConfig.LIFECYCLE_OBJECT_NOT_HOST_EXCEPTION),
                                              event.getLifecycle());
            log(msg, e);
            return;
        }

        // Process the event that has occurred
        if (event.getType().equals(Lifecycle.START_EVENT))
            start();
        else if (event.getType().equals(Lifecycle.STOP_EVENT))
            stop();

    }


    // -------------------------------------------------------- Private Methods


    /**
     * Deploy a web application for any user who has a web application present
     * in a directory with a specified name within their home directory.
     */
    private void deploy() {

        if (debug >= 1)
            log(rb.getString(DEPLOYING_USER_WEB_APP_INFO));

        // Load the user database object for this host
        UserDatabase database = null;
        try {
            Class clazz = Class.forName(userClass);
            database = (UserDatabase) clazz.newInstance();
            database.setUserConfig(this);
        } catch (Exception e) {
            log(rb.getString(LOADING_USER_DATABASE_EXCEPTION), e);
            return;
        }

        // Deploy the web application (if any) for each defined user
        Enumeration users = database.getUsers();
        while (users.hasMoreElements()) {
            String user = (String) users.nextElement();
            String home = database.getHome(user);
            deploy(user, home);
        }

    }


    /**
     * Deploy a web application for the specified user if they have such an
     * application in the defined directory within their home directory.
     *
     * @param user Username owning the application to be deployed
     * @param home Home directory of this user
     */
    private void deploy(String user, String home) {

        // Does this user have a web application to be deployed?
        String contextPath = "/~" + user;
        if (host.findChild(contextPath) != null)
            return;
        File app = new File(home, directoryName);
        if (!app.exists() || !app.isDirectory())
            return;
        /*
        File dd = new File(app, "/WEB-INF/web.xml");
        if (!dd.exists() || !dd.isFile() || !dd.canRead())
            return;
        */
        String msg = MessageFormat.format(rb.getString(DEPLOYING_WEB_APP_FOR_USER_INFO),
                                          user);
        log(msg);

        // Deploy the web application for this user
        try {
            Class clazz = Class.forName(contextClass);
            Context context =
              (Context) clazz.newInstance();
            context.setPath(contextPath);
            context.setDocBase(app.toString());
            if (context instanceof Lifecycle) {
                clazz = Class.forName(configClass);
                LifecycleListener listener =
                  (LifecycleListener) clazz.newInstance();
                ((Lifecycle) context).addLifecycleListener(listener);
            }
            host.addChild(context);
        } catch (Exception e) {
            String deployWebAppMsg = MessageFormat.format(rb.getString(DEPLOYING_WEB_APP_FOR_USER_EXCEPTION),
                                              user);
            log(deployWebAppMsg, e);
        }

    }


    /**
     * Log a message on the Logger associated with our Host (if any)
     *
     * @param message Message to be logged
     */
    private void log(String message) {
        Logger logger = null;
        if (host != null) {
            logger = host.getLogger();
            if (logger != null) {
                logger.log("UserConfig[" + host.getName() + "]: " + message);
            } else {
                if (log.isLoggable(Level.INFO)) {
                    log.log(Level.INFO, USER_CONFIG, new Object[] {host.getName(), message});
                }
            }
        } else {
            if (log.isLoggable(Level.INFO)) {
                log.log(Level.INFO, USER_CONFIG_NULL, message);
            }
        }
    }


    /**
     * Log a message on the Logger associated with our Host (if any)
     *
     * @param message Message to be logged
     * @param t Associated exception
     */
    private void log(String message, Throwable t) {
        Logger logger = null;
        if (host != null) {
            logger = host.getLogger();
            if (logger != null) {
                logger.log("UserConfig[" + host.getName() + "] "
                        + message, t, Logger.WARNING);
            } else {
                String msg = MessageFormat.format(rb.getString(USER_CONFIG),
                                                  new Object[] {host.getName(), message});
                log.log(Level.WARNING, msg, t);
            }
        } else {
            String msg = MessageFormat.format(rb.getString(USER_CONFIG_NULL),
                                              message);
            log.log(Level.WARNING, msg, t);
        }
    }


    /**
     * Process a "start" event for this Host.
     */
    private void start() {

        if (debug > 0)
            log(rb.getString(PROCESSING_START_INFO));

        deploy();

    }


    /**
     * Process a "stop" event for this Host.
     */
    private void stop() {

        if (debug > 0)
            log(rb.getString(PROCESSING_STOP_INFO));

    }


}
