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

package com.sun.enterprise.web.connector;

import javax.management.*;
import java.lang.String;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.sun.enterprise.web.WebContainer;
import com.sun.enterprise.config.serverbeans.HttpService;
import com.sun.enterprise.config.serverbeans.VirtualServer;
import org.apache.catalina.*;
import org.apache.catalina.core.ContainerBase;
import org.apache.catalina.core.ContextsAdapterUtility;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.util.RequestUtil;
import org.glassfish.grizzly.http.server.util.Mapper;
import org.glassfish.grizzly.http.server.util.MappingData;
import org.glassfish.grizzly.http.util.DataChunk;
import org.glassfish.logging.annotation.LogMessageInfo;


/**
 * Mapper listener.
 *
 * @author Remy Maucherat
 * @author Costin Manolache
 * @author Amy Roh
 */
public class MapperListener implements NotificationListener, NotificationFilter{

    // ----------------------------------------------------- Instance Variables

    private String defaultHost;

    private String domain="*";

    private transient Engine engine = null;

    public transient HttpService httpService;

    protected static final Logger logger = com.sun.enterprise.web.WebContainer.logger;

    protected static final ResourceBundle rb = logger.getResourceBundle();

    @LogMessageInfo(
            message = "Cannot find WebContainer implementation",
            level = "SEVERE",
            cause = "Web container is null",
            action = "Check if the mapper listener is initialized correctly")
    public static final String CANNOT_FIND_WEB_CONTAINER = "AS-WEB-GLUE-00084";

    @LogMessageInfo(
            message = "Cannot find Engine implementation",
            level = "SEVERE",
            cause = "Engine is null",
            action = "Check if the mapper listener is initialized correctly")
    public static final String CANNOT_FIND_ENGINE = "AS-WEB-GLUE-00085";

    @LogMessageInfo(
            message = "Error registering contexts",
            level = "WARNING")
    public static final String ERROR_REGISTERING_CONTEXTS = "AS-WEB-GLUE-00086";

    @LogMessageInfo(
            message = "HTTP listener with network listener name {0} ignoring registration of host with object name {1}, because none of the host's associated HTTP listeners matches this network listener name",
            level = "FINE")
    public static final String IGNORE_HOST_REGISTRATIONS = "AS-WEB-GLUE-00087";

    @LogMessageInfo(
            message = "Register Context {0}",
            level = "FINE")
    public static final String REGISTER_CONTEXT = "AS-WEB-GLUE-00088";

    @LogMessageInfo(
            message = "Unregister Context {0}",
            level = "FINE")
    public static final String UNREGISTER_CONTEXT = "AS-WEB-GLUE-00089";

    @LogMessageInfo(
            message = "Register Wrapper {0} in Context {1}",
            level = "FINE")
    public static final String REGISTER_WRAPPER = "AS-WEB-GLUE-00090";

    protected transient Mapper mapper = null;

    // START SJSAS 6313044
    private String myInstance;
    // END SJSAS 6313044

    private String networkListenerName;

    private ConcurrentHashMap<ObjectName,String[]> virtualServerListenerNames;

    private transient WebContainer webContainer;

    // ----------------------------------------------------------- Constructors


    /**
     * Create mapper listener.
     */
    public MapperListener(Mapper mapper, WebContainer webContainer) {
        this.mapper = mapper;
        virtualServerListenerNames = new ConcurrentHashMap<ObjectName,String[]>();
        this.webContainer = webContainer;
    }


    // --------------------------------------------------------- Public Methods

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    // BEGIN S1AS 5000999
    public String getNetworkListenerName() {
        return networkListenerName;
    }

    public void setNetworkListenerName(String networkListenerName) {
        this.networkListenerName = networkListenerName;
    }

    public String getDefaultHost() {
        return defaultHost;
    }

    public void setDefaultHost(String defaultHost) {
        this.defaultHost = defaultHost;
    }
    // END S1AS 5000999

    public void setInstanceName(String instanceName) {
        myInstance = instanceName;
    }

    /**
     * Initialize associated mapper.
     */
    public void init() {

        if (webContainer == null)  {
            logger.log(Level.SEVERE, CANNOT_FIND_WEB_CONTAINER);
            return;
        }

        try {

            httpService = webContainer.getHttpService();
            engine = webContainer.getEngine();
            if (engine == null) {
                logger.log(Level.SEVERE, CANNOT_FIND_ENGINE);
                return;
            }

            if (defaultHost != null) {
                mapper.setDefaultHostName(defaultHost);
            }

            for (VirtualServer vs : httpService.getVirtualServer()) {
                Container host = engine.findChild(vs.getId());
                if (host instanceof StandardHost) {
                    registerHost((StandardHost)host);
                    for (Container context: host.findChildren()) {
                        if (context instanceof StandardContext) {
                            registerContext((StandardContext)context);
                            for (Container wrapper : context.findChildren()) {
                                if (wrapper instanceof StandardWrapper) {
                                    registerWrapper((StandardWrapper)wrapper);
                                }
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            logger.log(Level.WARNING, ERROR_REGISTERING_CONTEXTS, e);
        }

    }


    // START SJSAS 6313044
    // ------------------------------------------ NotificationFilter Methods
    /**
     * Filters out any notifications corresponding to MBeans belonging to
     * a different server instance than the server instance on which this
     * MapperListener is running.
     *
     * @param notification The notification to be examined
     *
     * @return true if the notification needs to be sent to this
     * MapperListener, false otherwise.
     */
    public boolean isNotificationEnabled(Notification notification) {

        if (notification instanceof MBeanServerNotification) {
            ObjectName objectName =
                    ((MBeanServerNotification) notification).getMBeanName();

            String otherDomain = objectName.getDomain();
            if (this.domain != null && !(this.domain.equals(otherDomain))) {
                return false;
            }

            String otherInstance = objectName.getKeyProperty("J2EEServer");
            if (myInstance != null && otherInstance != null
                    && !otherInstance.equals(myInstance)) {
                return false;
            }
        }
        return true;

    }
    // END SJSAS 6313044

    // ------------------------------------------- NotificationListener Methods


    public void handleNotification(Notification notification,
                                   java.lang.Object handback) {

        if (notification.getType().equals("j2ee.object.created")) {
            ContainerBase container = ((ContainerBase)notification.getSource());
            if (container instanceof StandardHost) {
                try {
                    registerHost((StandardHost)container);
                } catch (Exception e) {
                    throw new RuntimeException(
                            "Error registering Host " + container.getObjectName(), e);
                }
            } else if (container instanceof StandardContext) {
                try {
                    registerContext((StandardContext)container);
                } catch (Throwable t) {
                    throw new RuntimeException(
                            "Error registering Context " + container.getObjectName(), t);
                }
            } else if (container instanceof StandardWrapper) {
                try {
                    registerWrapper((StandardWrapper)container);
                 } catch (Throwable t) {
                    throw new RuntimeException(
                            "Error registering Wrapper " + container.getObjectName(), t);
                }
            }
        } else if (notification.getType().equals("j2ee.object.deleted")) {
            ContainerBase container = ((ContainerBase)notification.getSource());
            if (container instanceof StandardHost) {
                try {
                    unregisterHost(container.getJmxName());
                } catch (Exception e) {
                    throw new RuntimeException("" +
                            "Error unregistering Host " + container.getObjectName(), e);
                }
            } else if (container instanceof StandardContext) {
                try {
                    unregisterContext(container.getJmxName());
                } catch (Throwable t) {
                    throw new RuntimeException(
                            "Error unregistering webapp " + container.getObjectName(), t);
                }
            } else if (container instanceof StandardWrapper) {
                ObjectName objectName = container.getJmxName();
                if (Boolean.parseBoolean(objectName.getKeyProperty("osgi")) &&
                        objectName.getKeyProperty("j2eeType").equals("Servlet")) {
                    try {
                        unregisterOSGiWrapper(objectName);
                     } catch (Throwable t) {
                        throw new RuntimeException(
                                "Error unregistering osgi wrapper " + objectName, t);
                     }
                }
            }
        }
    }


    // ------------------------------------------------------ Protected Methods


    /**
     * Register host.
     */
    public void registerHost(StandardHost host)
        throws Exception {

        if (host.getJmxName() == null) {
            return;
        }

        String name = host.getName();

            // BEGIN S1AS 5000999
            /*
             * Register the given Host only if one of its associated network listener
             * names matches the network listener name of this MapperListener
             */
            String[] nlNames = host.getNetworkListenerNames();
            boolean nameMatch = false;
            if (nlNames != null) {
                for (String nlName : nlNames) {
                    if (nlName.equals(this.networkListenerName)) {
                        nameMatch = true;
                        break;
                    }
                }
            }
            if (!nameMatch) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, IGNORE_HOST_REGISTRATIONS, new Object[]{networkListenerName, name});
                }
                return;
            }

            // nameMatch = true here, so nlNames != null
            virtualServerListenerNames.put(host.getJmxName(), nlNames);
            // END S1AS 5000999

            String[] aliases = host.findAliases();

            mapper.addHost(name, aliases, host);

    }


    /**
     * Unregister host.
     */
    public void unregisterHost(ObjectName objectName)
        throws Exception {

        String name=objectName.getKeyProperty("host");
        // BEGIN S1AS 5000999
        if (name != null) {
            String[] nlNames = virtualServerListenerNames.get(objectName);
            boolean nameMatch = false;
            if (nlNames != null) {
                virtualServerListenerNames.remove(objectName);
                for (String nlName : nlNames) {
                    if (nlName.equals(this.networkListenerName)) {
                        nameMatch = true;
                        break;
                    }
                }
            }
            if (!nameMatch) {
                return;
            }
        }
        // END S1AS 5000999
        mapper.removeHost(name);
    }


    /**
     * Register context.
     */
    private void registerContext(StandardContext context)
        throws Exception {

        ObjectName objectName = context.getJmxName();
        if (objectName == null) {
            return;
        }

        String name = objectName.getKeyProperty("name");

        String hostName = null;
        String contextName = null;
        if (name.startsWith("//")) {
            name = name.substring(2);
        }
        int slash = name.indexOf("/");
        if (slash != -1) {
            hostName = name.substring(0, slash);
            contextName = name.substring(slash);
            contextName = RequestUtil.urlDecode(contextName , "UTF-8");
        } else {
            return;
        }
        // Special case for the root context
        if (contextName.equals("/")) {
            contextName = "";
        }
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, REGISTER_CONTEXT, contextName);
        }

        javax.naming.Context resources = context.findStaticResources();
        String[] welcomeFiles = context.getWelcomeFiles();

        mapper.addContext(hostName, contextName, context, 
                          welcomeFiles, ContextsAdapterUtility.wrap(resources),
                          context.getAlternateDocBases());
    }


    /**
     * Unregister context.
     */
    private void unregisterContext(ObjectName objectName)
        throws Exception {

        String name = objectName.getKeyProperty("name");

        String hostName = null;
        String contextName = null;
        if (name.startsWith("//")) {
            name = name.substring(2);
        }
        int slash = name.indexOf("/");
        if (slash != -1) {
            hostName = name.substring(0, slash);
            contextName = name.substring(slash);
            contextName = RequestUtil.urlDecode(contextName , "UTF-8");
        } else {
            return;
        }
        // Special case for the root context
        if (contextName.equals("/")) {
            contextName = "";
        }

        // Don't un-map a context that is paused
        DataChunk hostDC = DataChunk.newInstance();
        hostDC.setString(hostName);
        DataChunk contextDC = DataChunk.newInstance();
        contextDC.setString(contextName);
        MappingData mappingData = new MappingData();
        mapper.map(hostDC, contextDC, mappingData);
        if (mappingData.context instanceof StandardContext &&
                ((StandardContext)mappingData.context).getPaused()) {
            return;
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, UNREGISTER_CONTEXT, contextName);
        }

        mapper.removeContext(hostName, contextName);

    }


    /**
     * Register wrapper.
     */
    private void registerWrapper(StandardWrapper wrapper)
        throws Exception {

        ObjectName objectName = wrapper.getJmxName();
        if (objectName == null) {
            return;
        }

        String wrapperName = objectName.getKeyProperty("name");
        String name = objectName.getKeyProperty("WebModule");

        String hostName = null;
        String contextName = null;
        if (name.startsWith("//")) {
            name = name.substring(2);
        }
        int slash = name.indexOf("/");
        if (slash != -1) {
            hostName = name.substring(0, slash);
            contextName = name.substring(slash);
            contextName = RequestUtil.urlDecode(contextName , "UTF-8");
        } else {
            return;
        }
        // Special case for the root context
        if (contextName.equals("/")) {
            contextName = "";
        }

        String msg = MessageFormat.format(rb.getString(REGISTER_WRAPPER), wrapperName, contextName);
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(msg);
        }

        String[] mappings = wrapper.findMappings();

        for (int i = 0; i < mappings.length; i++) {
            boolean jspWildCard = (wrapperName.equals("jsp")
                                   && mappings[i].endsWith("/*"));
            mapper.addWrapper(hostName, contextName, mappings[i], wrapper,
                              jspWildCard, wrapperName, true);
        }

    }

    /**
     * Unregister wrapper.
     */
    private void unregisterOSGiWrapper(ObjectName objectName)
        throws Exception {

        // If the domain is the same with ours or the engine 
        // name attribute is the same... - then it's ours
        String targetDomain=objectName.getDomain();
        if( ! domain.equals( targetDomain )) {
            return;
        }

        String name = objectName.getKeyProperty("WebModule");

        String hostName = null;
        String contextName = null;
        if (name.startsWith("//")) {
            name = name.substring(2);
        }
        int slash = name.indexOf("/");
        if (slash != -1) {
            hostName = name.substring(0, slash);
            contextName = name.substring(slash);
            contextName = RequestUtil.urlDecode(contextName , "UTF-8");
        } else {
            return;
        }
        // Special case for the root context
        if (contextName.equals("/")) {
            contextName = "";
        }

        String mapping = objectName.getKeyProperty("name");
        if ("/".equals(mapping)) {
            mapping = "/*";
        } else {
            mapping += "/*";
        }
        mapper.removeWrapper(hostName, contextName, mapping);
    }
}
