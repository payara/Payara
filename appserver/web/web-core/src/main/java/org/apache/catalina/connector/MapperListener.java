/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.apache.catalina.connector;

import org.apache.catalina.Host;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.util.RequestUtil;
import org.apache.catalina.util.StringManager;
import org.apache.tomcat.util.modeler.Registry;

import javax.management.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.grizzly.http.server.util.Mapper;
import org.glassfish.grizzly.http.server.util.MappingData;
import org.glassfish.grizzly.http.util.DataChunk;
import org.glassfish.grizzly.http.util.MessageBytes;

/**
 * Mapper listener.
 *
 * @author Remy Maucherat
 * @author Costin Manolache
 */
public class MapperListener
    /* SJSAS 6313044
    implements NotificationListener
    */
    // START SJSAS 6313044
    implements NotificationListener, NotificationFilter
    // END SJSAS 6313044
 {
    private static Logger log = Logger.getLogger(MapperListener.class.getName());


    // ----------------------------------------------------- Instance Variables
    /**
     * Associated mapper.
     */
    protected transient Mapper mapper = null;

    /**
     * MBean server.
     */
    protected transient MBeanServer mBeanServer = null;


    /**
     * The string manager for this package.
     */
    private static final StringManager sm =
        StringManager.getManager(Constants.Package);

    // It should be null - and fail if not set
    private String domain="*";
    private String engine="*";


    // BEGIN S1AS 5000999
    private String networkListenerName;
    private String defaultHost;
    private ConcurrentHashMap<ObjectName,String[]> virtualServerListenerNames;
    // END S1AS 5000999


    // START SJSAS 6313044
    private String myInstance;
    // END SJSAS 6313044


    // ----------------------------------------------------------- Constructors


    /**
     * Create mapper listener.
     */
    public MapperListener(Mapper mapper) {
        this.mapper = mapper;
        virtualServerListenerNames = new ConcurrentHashMap<ObjectName,String[]>();
    }


    // --------------------------------------------------------- Public Methods

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    // BEGIN S1AS 5000999
    public String getNetworkListenerName() {
        return networkListenerName;
    }

    public void setNetworkListenerName(String networkListenerName) {
        this.networkListenerName = networkListenerName;
    }

    public void setInstanceName(String instanceName) {
        myInstance = instanceName;
    }

    public String getDefaultHost() {
        return defaultHost;
    }

    public void setDefaultHost(String defaultHost) {
        this.defaultHost = defaultHost;
    }
    // END S1AS 5000999

    /**
     * Initialize associated mapper.
     */
    public void init() {

        if (defaultHost != null) {
            mapper.setDefaultHostName(defaultHost);
        }

        try {

            mBeanServer = Registry.getRegistry(null, null).getMBeanServer();

            // Query hosts
            String onStr = domain + ":type=Host,*";
            ObjectName objectName = new ObjectName(onStr);
            Set<ObjectInstance> set = mBeanServer.queryMBeans(objectName, null);
            Iterator<ObjectInstance> iterator = set.iterator();
            while (iterator.hasNext()) {
                ObjectInstance oi = iterator.next();
                registerHost(oi.getObjectName());
            }

            // Query contexts
            onStr = domain + ":j2eeType=WebModule,*,J2EEServer=" + myInstance;
            objectName = new ObjectName(onStr);
            set = mBeanServer.queryMBeans(objectName, null);
            iterator = set.iterator();
            while (iterator.hasNext()) {
                ObjectInstance oi = iterator.next();
                registerContext(oi.getObjectName());
            }

            // Query wrappers
            onStr = domain + ":j2eeType=Servlet,*,J2EEServer=" + myInstance;
            objectName = new ObjectName(onStr);
            set = mBeanServer.queryMBeans(objectName, null);
            iterator = set.iterator();
            while (iterator.hasNext()) {
                ObjectInstance oi = iterator.next();
                registerWrapper(oi.getObjectName());
            }

            onStr = "JMImplementation:type=MBeanServerDelegate";
            objectName = new ObjectName(onStr);
            /* SJSAS 6313044
            mBeanServer.addNotificationListener(objectName, this, null, null);
            */
            // START SJSAS 6313044
            mBeanServer.addNotificationListener(objectName, this, this, null);
            // END SJSAS 6313044
        } catch (Exception e) {
            log.log(Level.WARNING, "Error registering contexts", e);
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

        if (notification instanceof MBeanServerNotification) {
            ObjectName objectName = 
                ((MBeanServerNotification) notification).getMBeanName();
            String j2eeType = objectName.getKeyProperty("j2eeType");
            String engineName = null;
            if (j2eeType != null) {
                if ((j2eeType.equals("WebModule")) || 
                    (j2eeType.equals("Servlet"))) {
                    if (mBeanServer.isRegistered(objectName)) {
                        /* SJSAS 6290785
                        try {
                            engineName = (String)
                                mBeanServer.getAttribute(objectName, "engineName");
                        } catch (Exception e) {
                            // Ignore
                        }
                        */
                        // START SJSAS 6290785
                        MBeanInfo info = null;
                        try {
                            info = mBeanServer.getMBeanInfo(objectName);
                        } catch (Exception e) {
                            // Ignore
                        } 
                        if (info != null) {
                            boolean hasEngineNameAttribute = false;
                            MBeanAttributeInfo[] attrInfo = info.getAttributes();
                            if (attrInfo != null) {
                                for (int i=0; i<attrInfo.length; i++) {
                                    if ("engineName".equals(
                                                    attrInfo[i].getName())) {
                                        hasEngineNameAttribute = true;
                                        break;
                                    }
                                }
                            }
                            if (hasEngineNameAttribute) {
                                try {
                                    engineName = (String)
                                        mBeanServer.getAttribute(objectName,
                                                                 "engineName");
                                } catch (Exception e) {
                                    // Ignore  
                                }
                            }
                        }
                        // END SJSAS 6290785
                    }
                }
            }

            // At deployment time, engineName is always = null.
            if ( (!"*".equals(domain)) &&
                 ( !domain.equals(objectName.getDomain()) ) &&
                 ( (!domain.equals(engineName) ) &&
                   (engineName != null) ) )  {
                return;
            }

            if (log.isLoggable(Level.FINE)) {
                log.fine( "Handle " + objectName );
            }

            if (notification.getType().equals
                (MBeanServerNotification.REGISTRATION_NOTIFICATION)) {
                String type=objectName.getKeyProperty("type");
                if( "Host".equals( type )) {
                    try {
                        registerHost(objectName);
                    } catch (Exception e) {
                        throw new RuntimeException(
                                "Error registering Host " + objectName, e);  
                    }
                }
    
                if (j2eeType != null) {
                    if (j2eeType.equals("WebModule")) {
                        try {
                            registerContext(objectName);
                        } catch (Throwable t) {
                            throw new RuntimeException(
                                    "Error registering Context " + objectName,
                                    t);
                        }
                    } else if (j2eeType.equals("Servlet")) {
                        try {
                            registerWrapper(objectName);
                        } catch (Throwable t) {
                            throw new RuntimeException(
                                    "Error registering Wrapper " + objectName,
                                    t);
                        }
                    }
                }
            } else if (notification.getType().equals
                       (MBeanServerNotification.UNREGISTRATION_NOTIFICATION)) {
                String type=objectName.getKeyProperty("type");
                if( "Host".equals( type )) {
                    try {
                        unregisterHost(objectName);
                    } catch (Exception e) {
                        throw new RuntimeException(
                                "Error unregistering Host " + objectName,
                                e);  
                    }
                }
 
                if (j2eeType != null) {
                    if (j2eeType.equals("WebModule")) {
                        try {
                            unregisterContext(objectName);
                        } catch (Throwable t) {
                            throw new RuntimeException(
                                    "Error unregistering webapp " + objectName,
                                    t);
                        }
                    } else if (Boolean.parseBoolean(objectName.getKeyProperty("osgi")) &&
                            j2eeType.equals("Servlet")) {
                        try {
                            unregisterOSGiWrapper(objectName);
                        } catch (Throwable t) {
                            throw new RuntimeException(
                                    "Error unregistering osgi wrapper " + objectName,
                                    t);
                        }
                    }
                }
            }
        }
    }


    // ------------------------------------------------------ Protected Methods


    /**
     * Register host.
     */
    public void registerHost(ObjectName objectName)
        throws Exception {
        String name=objectName.getKeyProperty("host");
        if( name != null ) {

            Host host = (Host) mBeanServer.invoke(objectName,
                                                  "findMappingObject",
                                                  null,
                                                  null);
            if (host == null) {
                throw new Exception("No host registered for " + objectName);
            }

            // BEGIN S1AS 5000999
            /*
             * Register the given Host only if one of its associated network listener
             * names matches the network listener name of this MapperListener
             */
            String[] nlNames = ((StandardHost) host).getNetworkListenerNames();
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
                if (log.isLoggable(Level.FINE)) {
                    log.fine("HTTP listener with network listener name " + networkListenerName
                             + " ignoring registration of host with object "
                             + "name " + objectName + ", because none of the "
                             + "host's associated HTTP listeners matches "
                             + "this network listener name");
                }
                return;
            }

            // nameMatch = true here, so nlNames != null
            virtualServerListenerNames.put(objectName, nlNames);
            // END S1AS 5000999

            String[] aliases = host.findAliases();

            mapper.addHost(name, aliases, host);
        }
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
    private void registerContext(ObjectName objectName)
        throws Exception {

        StandardContext context = (StandardContext)
            mBeanServer.invoke(objectName, "findMappingObject", null, null);
        if (context == null) {
            throw new Exception("No context registered for " + objectName);
        }

        String name = objectName.getKeyProperty("name");
        
        // If the domain is the same with ours or the engine 
        // name attribute is the same... - then it's ours
        String targetDomain=objectName.getDomain();
        if( ! domain.equals( targetDomain )) {
            targetDomain = context.getEngineName();
            if( ! domain.equals( targetDomain )) {
                // not ours
                return;
            }
        }

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

        if (log.isLoggable(Level.FINE)) {
            log.fine(sm.getString("mapperListener.registerContext",
                                  contextName));
        }

        javax.naming.Context resources = context.findStaticResources();
        String[] welcomeFiles = context.getWelcomeFiles();

        mapper.addContext(hostName, contextName, context, 
                          welcomeFiles, resources,
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

        if (log.isLoggable(Level.FINE)) {
            log.fine(sm.getString("mapperListener.unregisterContext",
                                  contextName));
        }

        mapper.removeContext(hostName, contextName);

    }


    /**
     * Register wrapper.
     */
    private void registerWrapper(ObjectName objectName)
        throws Exception {

        StandardWrapper wrapper = (StandardWrapper)
            mBeanServer.invoke(objectName, "findMappingObject", null, null);
        if (wrapper == null) {
            throw new Exception("No wrapper registered for " + objectName);
        }
    
        // If the domain is the same with ours or the engine 
        // name attribute is the same... - then it's ours
        String targetDomain=objectName.getDomain();
        if( ! domain.equals( targetDomain )) {
            targetDomain= wrapper.getEngineName();
            if( ! domain.equals( targetDomain )) {
                // not ours
                return;
            }
            
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

        if (log.isLoggable(Level.FINE)) {
            log.fine(sm.getString("mapperListener.registerWrapper", 
                                  wrapperName, contextName));
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
