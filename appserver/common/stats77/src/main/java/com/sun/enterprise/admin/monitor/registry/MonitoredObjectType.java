/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.admin.monitor.registry;
//import com.sun.enterprise.config.serverbeans.*;
import java.util.HashMap;

/**
 * MonitoredObjectType represents the type of a monitored object. Many of
 * monitored objects have same properties, even while they monitor different
 * user objects. For example - application and standalone ejb module are both
 * nothing more than containers of other objects -- application contains web
 * module and ejb modules, whereas standalone ejb module contains beans of
 * various types. This object facilitates use of same GenericMonitorMBean as
 * MBean for both of them, but still distinguishes them.
 *
 * @see com.iplanet.ias.admin.monitor.GenericMonitorMBean
 * @author Originally authored by Abhijit Kumar
 * @author Copied from S1AS 7.0 source and modified by Shreedhar Ganapathy
 */
public class MonitoredObjectType {

    /**
     * A map to store all objects of type MonitoredObjectType using their string
     * representation as key.
     */
    private static final HashMap objectMap = new HashMap();
    /**
     * to be able to initialize to a non-valid type
     */
    public static final MonitoredObjectType NONE = new MonitoredObjectType("none", true);
    /**
     * ROOT MonitoredObject - singleton
     */
    public static final MonitoredObjectType ROOT = new MonitoredObjectType("root", true);
    /**
     * Parent of all applications and modules and other children - singleton
     */
    public static final MonitoredObjectType APPLICATIONS = new MonitoredObjectType("applications", true);
    /**
     * MonitoredObjectType for any deployed application - multiple
     */
    public static final MonitoredObjectType APPLICATION = new MonitoredObjectType("application");
    /**
     * An ejb module within an application - multiple
     */
    public static final MonitoredObjectType EJBMODULE = new MonitoredObjectType("ejb-module");
    /**
     * A web module within an application - multiple
     */
    public static final MonitoredObjectType WEBMODULE = new MonitoredObjectType("web-module");
    /**
     * A standalone ejb module - multiple
     */
    public static final MonitoredObjectType STANDALONE_EJBMODULE = new MonitoredObjectType("standalone-ejb-module");
    /**
     * A standalone web module - multiple
     */
    public static final MonitoredObjectType STANDALONE_WEBMODULE = new MonitoredObjectType("standalone-web-module");
    /**
     * A stateless session bean - multiple
     */
    public static final MonitoredObjectType STATELESS_BEAN = new MonitoredObjectType("stateless-session-bean");
    /**
     * A stateful session bean - multiple
     */
    public static final MonitoredObjectType STATEFUL_BEAN = new MonitoredObjectType("stateful-session-bean");
    /**
     * A entity session bean - multiple
     */
    public static final MonitoredObjectType ENTITY_BEAN = new MonitoredObjectType("entity-bean");
    /**
     * A message driven bean - multiple
     */
    public static final MonitoredObjectType MESSAGE_DRIVEN_BEAN = new MonitoredObjectType("message-driven-bean");
    /**
     * An ejb pool - one per ejb
     */
    public static final MonitoredObjectType BEAN_POOL = new MonitoredObjectType("bean-pool", true);
    /**
     * An ejb cache - one per ejb
     */
    public static final MonitoredObjectType BEAN_CACHE = new MonitoredObjectType("bean-cache", true);
    /*
     * An ejb method - multiple
     */
    public static final MonitoredObjectType BEAN_METHOD = new MonitoredObjectType("bean-method");
    /*
     * All ejb methods - one per ejb
     */
    public static final MonitoredObjectType BEAN_METHODS = new MonitoredObjectType("bean-methods", true);
    /**
     * A servlet - multiple
     */
    public static final MonitoredObjectType SERVLET = new MonitoredObjectType("servlet");
    /**
     * Http Service node - singleton
     */
    public static final MonitoredObjectType HTTP_SERVICE = new MonitoredObjectType("http-service", true);
    /**
     * Virtual server - multiple
     */
    public static final MonitoredObjectType VIRTUAL_SERVER = new MonitoredObjectType("virtual-server");
    /**
     * webmodule-virtualserver, fix for 6172666
     */
    public static final MonitoredObjectType WEBAPP_VIRTUAL_SERVER = new MonitoredObjectType("webmodule-virtual-server");
    /**
     * Http Listener - multiple
     */
    public static final MonitoredObjectType HTTP_LISTENER = new MonitoredObjectType("http-listener");
    /**
     * JVM - singleton
     */
    public static final MonitoredObjectType JVM = new MonitoredObjectType("jvm", true);
    /**
     * Transaction service - singleton
     */
    public static final MonitoredObjectType TRANSACTION_SERVICE = new MonitoredObjectType("transaction-service", true);
    /**
     * Thread pools - singleton
     */
    public static final MonitoredObjectType THREAD_POOLS = new MonitoredObjectType("thread-pools", true);
    /**
     * A named thead pool - multiple
     */
    public static final MonitoredObjectType THREAD_POOL = new MonitoredObjectType("thread-pool");
    /**
     * ORB - singleton
     */
    public static final MonitoredObjectType ORB = new MonitoredObjectType("orb", true);
    /**
     * Connection Managers - singleton
     */
    public static final MonitoredObjectType CONNECTION_MANAGERS = new MonitoredObjectType("connection-managers", true);
    /**
     * A Connection Manager for orb - multiple
     */
    public static final MonitoredObjectType CONNECTION_MANAGER = new MonitoredObjectType("connection-manager", true);
    /**
     * All the monitorable resources - singleton
     */
    public static final MonitoredObjectType RESOURCES = new MonitoredObjectType("resources", true);
    /**
     * A monitorable jdbc connection pool - multiple
     */
    public static final MonitoredObjectType JDBC_CONN_POOL = new MonitoredObjectType("jdbc-connection-pool");
    /**
     * A monitorable connector connection pool - multiple
     */
    public static final MonitoredObjectType CONNECTOR_CONN_POOL = new MonitoredObjectType("connector-connection-pool");
    /**
     * Any Web Container Element -- not used in hierarchy -- mainly for
     * notification
     */
    public static final MonitoredObjectType WEB_COMPONENT = new MonitoredObjectType("web-component");
    /**
     * Jndi monitoring
     */
    public static final MonitoredObjectType JNDI = new MonitoredObjectType("jndi", true);
    // for connector monitoring
    public static final MonitoredObjectType CONNECTOR_SERVICE = new MonitoredObjectType("connector-service", true);
    public static final MonitoredObjectType CONNECTOR_MODULE = new MonitoredObjectType("connector-module");
    public static final MonitoredObjectType STANDALONE_CONNECTOR_MODULE = new MonitoredObjectType("standalone-connector-module");
    public static final MonitoredObjectType CONNECTOR_WORKMGMT = new MonitoredObjectType("work-management", true);
    public static final MonitoredObjectType JMS_SERVICE = new MonitoredObjectType("jms-service", true);
    public static final MonitoredObjectType CONNECTION_FACTORIES = new MonitoredObjectType("connection-factories", true);
    public static final MonitoredObjectType CONNECTION_FACTORY = new MonitoredObjectType("connection-factory");
    public static final MonitoredObjectType CONNECTION_POOLS = new MonitoredObjectType("connection-pools", true);
    // for PWC Monitoring
    public static final MonitoredObjectType CONNECTION_QUEUE = new MonitoredObjectType("connection-queue", true);
    public static final MonitoredObjectType DNS = new MonitoredObjectType("dns", true);
    public static final MonitoredObjectType KEEP_ALIVE = new MonitoredObjectType("keep-alive", true);
    public static final MonitoredObjectType PWC_THREAD_POOL = new MonitoredObjectType("pwc-thread-pool", true);
    public static final MonitoredObjectType FILE_CACHE = new MonitoredObjectType("file-cache", true);
    public static final MonitoredObjectType REQUEST = new MonitoredObjectType("request", true);
    // for StatefulSessionStore Monitoring
    public static final MonitoredObjectType SESSION_STORE = new MonitoredObjectType("stateful-session-store", true);
    // for Timer Monitoring
    public static final MonitoredObjectType TIMERS = new MonitoredObjectType("timers", true);
    // for JVM1.5 Monitoring
    public static final MonitoredObjectType JVM_COMPILATION = new MonitoredObjectType("compilation-system", true);
    public static final MonitoredObjectType JVM_CLASSLOADING = new MonitoredObjectType("class-loading-system", true);
    public static final MonitoredObjectType JVM_OS = new MonitoredObjectType("operating-system", true);
    public static final MonitoredObjectType JVM_RUNTIME = new MonitoredObjectType("runtime", true);
    public static final MonitoredObjectType JVM_GCS = new MonitoredObjectType("garbage-collectors", true);
    public static final MonitoredObjectType JVM_GC = new MonitoredObjectType("garbage-collector");
    public static final MonitoredObjectType JVM_MEMORY = new MonitoredObjectType("memory", true);
    public static final MonitoredObjectType JVM_THREAD = new MonitoredObjectType("thread-system", true);
    public static final MonitoredObjectType JVM_THREAD_INFO = new MonitoredObjectType("threadinfo");
    /**
     * A web service endpoint with in an ejb or web module - multiple
     */
    public static final MonitoredObjectType WEBSERVICE_ENDPOINT =
            new MonitoredObjectType("webservice-endpoint");
    /**
     * value of this object as a string
     */
    private String typeName;
    /**
     * Denotes whether this type allows more than one instance at any level.
     */
    private boolean isSingleton;

    /**
     * Creates a new instance of MonitoredObjectType using specified string type
     *
     * @param type string representing the name of monitored object type
     */
    private MonitoredObjectType(String type) {
        this(type, false);
    }

    /**
     * Creates a new instance of MonitoredObjectType using specified string type
     * and specified flag for singleton
     *
     * @param type string representing the name of monitored object type
     * @param isSingleton denotes whether this type of monitored object has only
     * one instance (in its context)
     */
    private MonitoredObjectType(String type, boolean isSingleton) {
        this.typeName = type;
        this.isSingleton = isSingleton;
        objectMap.put(this.typeName, this);
    }

    /**
     * Get type of this "MonitoredObjectType" as string
     *
     * @return Monitored object type as string
     */
    public String getTypeName() {
        return typeName;
    }

    /**
     * Is instance of this type of MonitorMBean singleton. For example, there
     * can only be one pool for every stateless session bean, so a
     * MonitoredObjectType of type MonitoredObjectType.BEAN_POOL is a singleton.
     *
     * @return true if this type of object can have atmost one instance within
     * its context, false otherwise.
     */
    public boolean isSingleton() {
        return isSingleton;
    }

    /**
     * A string representation. The return value of this method is same as that
     * of method getTypeName.
     *
     * @return A string representation of this MonitoredObjectType
     */
    @Override
    public String toString() {
        return typeName;
    }

    /**
     * Get a MonitoredObjectType instance for the specified string type.
     *
     * @param typeName string representing MonitoredObjectType
     * @throws IllegalArgumentException if the specified type name is not known.
     */
    public static MonitoredObjectType getMonitoredObjectType(String typeName) {
        MonitoredObjectType type = getMonitoredObjectTypeOrNull(typeName);
        if (type == null) {
            //String msg = localStrings.getString( "admin.monitor.unknown_type_name", typeName );
            String msg = "admin.monitor.unknown_type_name:" + typeName;
            throw new IllegalArgumentException(msg);
        }
        return type;
    }

    /**
     * Get a MonitoredObjectType instance for the specified string type. If the
     * specified type is not known, the method returns null.
     *
     * @param typeName string representing MonitoredObjectType
     */
    static MonitoredObjectType getMonitoredObjectTypeOrNull(String typeName) {
        MonitoredObjectType type = null;
        if (objectMap != null && typeName != null) {
            type = (MonitoredObjectType) objectMap.get(typeName);
        }
        return type;
    }
}
