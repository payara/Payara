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

import org.glassfish.j2ee.statistics.*;
import com.sun.enterprise.admin.monitor.stats.*;

/**
 * Provides component specific methods to enable components to register their
 * Stats implementations for monitoring. Implementation of this interface enables
 * a JSR77 Managed Resource's monitoring statistics to be presented
 * through JMX's API through a DynamicMBean created on the fly
 * and registered with the MBeanServer. Each registration method also provides
 * the facility of passing in an implementation of MonitoringLevelListener interface.
 * The purpose of that interface is to enable components to be notified of a 
 * change in monitoring level from/to OFF, LOW or HIGH. A notification pursuant 
 * to a change in monitoring level should result in a call to unregister previously
 * registered Stats implementations. If the change is set at a level LOW or HIGH,
 * a call to register alternative Stats implementations that correspond to the 
 * new monitoring level is to be made by the component.  Consequently, a new MBean 
 * is registered with the MBeanServer so that clients may now request statistics 
 * corresponding to the monitoring level.<br>
 * Registered Stats implementations can be unregistered through the component 
 * specific unregister() methods. 
 *
 * @see com.sun.enterprise.admin.monitor.registry.MonitoringLevelListener.java
 * @see com.sun.enterprise.admin.monitor.registry.MonitoringLevel.java
 * @author  Shreedhar Ganapathy
 * @version $Revision: 1.4 $
 */
public interface MonitoringRegistry {
   
    /**
     * Registers the Stats implementation for EntityBeanStats
     * @param entityBeanStats implementation of org.glassfish.j2ee.statistics.EntityBeanStats
     * @param entityBeanName
     * @param moduleName
     * @param applicationName passing a null here would 
     * indicate that this is a registration for an EntityBean deployed under a 
     * standalone module.
     * @param listener This an optional field and when not required, a null 
     * value should be passed. If a MonitoringLevelListener is defined,
     * the listener's setLevel() method will be called in response to a change 
     * in monitoring level. 
     * @throws MonitoringRegistrationException
     */
    public void registerEntityBeanStats(EntityBeanStats entityBeanStats,
    String entityBeanName, String moduleName, String applicationName,
    MonitoringLevelListener listener) throws MonitoringRegistrationException;

    /**
     * Unregisters the Stats implementation for EntityBeanStats
     * @param entityBeanName
     * @param moduleName
     * @param applicationName  passing a null here would 
     * indicate that this is a registration for an EntityBean deployed under a 
     * standalone module.
     * @throws MonitoringRegistrationException
     */
    public void unregisterEntityBeanStats(String entityBeanName, String moduleName,
    String applicationName) throws MonitoringRegistrationException;    
    
    /**
     * Registers the Stats implementation for StatelessSessionBeanStats
     * @param statelessSessionBeanStats implementation of 
     * org.glassfish.j2ee.statistics.StatelessSessionBeanStats
     * @param statelessSessionBeanName
     * @param moduleName
     * @param applicationName  passing a null here would 
     * indicate that this is a registration for a StatelessSessionBean 
     * deployed under a standalone module.
     * @param listener  This an optional field and when not required, a null 
     * value should be passed. If a MonitoringLevelListener is defined,
     * the listener's setLevel() method will be called in response to a change 
     * in monitoring level. 
     * @throws MonitoringRegistrationException
     */
    public void registerStatelessSessionBeanStats(
    StatelessSessionBeanStats statelessSessionBeanStats,
    String statelessSessionBeanName, String moduleName, String applicationName,
    MonitoringLevelListener listener) throws MonitoringRegistrationException;
    
    /**
     * Unregisters the Stats implementation for StatelessSessionBeanStats
     * @param statelessSessionBeanName
     * @param moduleName
     * @param applicationName passing a null here would 
     * indicate that this is a registration for a StatelessSessionBean 
     * deployed under a standalone module.
     * @throws MonitoringRegistrationException
     */
    public void unregisterStatelessSessionBeanStats(String statelessSessionBeanName, 
    String moduleName, String applicationName) 
    throws MonitoringRegistrationException;

    /**
     * Registers the Stats implementation for StatefulSessionBeanStats
     * @param statefulSessionBeanStats  implementation of from 
     * org.glassfish.j2ee.statistics.StatefulSessionBeanStats
     * @param statefulSessionBeanName
     * @param moduleName
     * @param applicationName  passing a null here would 
     * indicate that this is a registration for a StatefulSessionBean 
     * deployed under a standalone module.
     * @param listener  This an optional field and when not required, a null 
     * value should be passed. If a MonitoringLevelListener is defined,
     * the listener's setLevel() method will be called in response to a change 
     * in monitoring level. 
     * @throws MonitoringRegistrationException
     */
    public void registerStatefulSessionBeanStats(
    StatefulSessionBeanStats statefulSessionBeanStats,
    String statefulSessionBeanName, String moduleName, String applicationName,
    MonitoringLevelListener listener) throws MonitoringRegistrationException;

    /**
     * Unregisters the Stats implementation for StatefulSessionBeanStats
     * @param statefulSessionBeanName
     * @param moduleName
     * @param applicationName  passing a null here would 
     * indicate that this is a registration for a StatefulSessionBean 
     * deployed under a standalone module.
     * @throws MonitoringRegistrationException
     */
    public void unregisterStatefulSessionBeanStats(String statefulSessionBeanName, 
    String moduleName, String applicationName) throws MonitoringRegistrationException;
    
    /**
     * Registers the Stats implementation for MessageDrivenBeanStats
     * @param messageDrivenBeanStats  implementation of 
     * org.glassfish.j2ee.statistics.MessageDrivenBeanStats
     * @param messageDrivenBeanName
     * @param moduleName
     * @param applicationName  passing a null here would 
     * indicate that this is a registration for a MessageDrivenBean 
     * deployed under a standalone module.
     * @param listener  This an optional field and when not required, a null 
     * value should be passed. If a MonitoringLevelListener is defined,
     * the listener's setLevel() method will be called in response to a change 
     * in monitoring level. 
     * @throws MonitoringRegistrationException
     */
    public void registerMessageDrivenBeanStats(
    MessageDrivenBeanStats messageDrivenBeanStats,
    String messageDrivenBeanName, String moduleName, String applicationName,
    MonitoringLevelListener listener) throws MonitoringRegistrationException;

    /**
     * Unregisters the Stats implementation for MessageDrivenBeanStats
     * @param messageDrivenBeanName
     * @param moduleName
     * @param applicationName  passing a null here would 
     * indicate that this is a registration for a MessageDrivenBean 
     * deployed under a standalone module.
     * @throws MonitoringRegistrationException
     */
    public void unregisterMessageDrivenBeanStats(String messageDrivenBeanName, 
    String moduleName, String applicationName) 
    throws MonitoringRegistrationException;
    
    /**
     * Registers the Stats implementation for EJBCacheStats
     * @param ejbCacheStats  implemetation of 
     * com.sun.enterprise.admin.monitor.stats.EJBCacheStats
     * @param ejbName
     * @param moduleName
     * @param applicationName  passing a null here would 
     * indicate that this is a registration for an EJB deployed under a 
     * standalone module.
     * @param listener  This an optional field and when not required, a null 
     * value should be passed. If a MonitoringLevelListener is defined,
     * the listener's setLevel() method will be called in response to a change 
     * in monitoring level. 
     * @throws MonitoringRegistrationException
     */
    public void registerEJBCacheStats(EJBCacheStats ejbCacheStats, MonitoredObjectType ejbType, String ejbName, 
    String moduleName, String applicationName, MonitoringLevelListener listener) 
    throws MonitoringRegistrationException;

    /**
     * Unregisters the Stats implementation for EJBCacheStats
     * @param ejbName
     * @param moduleName
     * @param applicationName  passing a null here would 
     * indicate that this is a registration for an EJB deployed under a 
     * standalone module.
     * @throws MonitoringRegistrationException
     */
    public void unregisterEJBCacheStats( MonitoredObjectType ejbType, String ejbName, String moduleName, 
    String applicationName)
    throws MonitoringRegistrationException;
    
    /**
     * Registers the Stats implementation for EJBPoolStats
     * @param ejbPoolStats  implementation of 
     * com.sun.enterprise.admin.monitor.stats.EJBPoolStats
     * @param ejbName
     * @param moduleName
     * @param applicationName  passing a null here would 
     * indicate that this is a registration for an EJB deployed under a 
     * standalone module.
     * @param listener  This an optional field and when not required, a null 
     * value should be passed. If a MonitoringLevelListener is defined,
     * the listener's setLevel() method will be called in response to a change 
     * in monitoring level. 
     * @throws MonitoringRegistrationException
     */
    public void registerEJBPoolStats(EJBPoolStats ejbPoolStats, MonitoredObjectType ejbType, String ejbName, 
    String moduleName, String applicationName, MonitoringLevelListener listener) 
    throws MonitoringRegistrationException;

    /**
     * Unregisters the Stats implementation for EJBPoolStats
     * @param ejbName
     * @param moduleName
     * @param applicationName  passing a null here would 
     * indicate that this is a registration for an EJB deployed under a 
     * standalone module.
     * @throws MonitoringRegistrationException
     */
    public void unregisterEJBPoolStats(MonitoredObjectType ejbType, String ejbName, String moduleName, 
    String applicationName) throws MonitoringRegistrationException;
    
    /**
     * Registers the Stats implementation for EJBMethodStats
     * @param ejbMethodStats  implementation of 
     * com.sun.enterprise.admin.monitor.stats.EJBMethodStats
     * @param ejbMethodName
     * @param ejbName
     * @param moduleName
     * @param applicationName  passing a null here would 
     * indicate that this is a registration for an EJB deployed under a 
     * standalone module.
     * @param listener  This an optional field and when not required, a null 
     * value should be passed. If a MonitoringLevelListener is defined,
     * the listener's setLevel() method will be called in response to a change 
     * in monitoring level. 
     * @throws MonitoringRegistrationException
     */
    public void registerEJBMethodStats(EJBMethodStats ejbMethodStats, 
    String ejbMethodName, MonitoredObjectType ejbType, String ejbName, String moduleName, 
    String applicationName, MonitoringLevelListener listener) 
    throws MonitoringRegistrationException;
       
    /**
     * Unregisters the Stats implementation for EJBMethodStats
     * @param ejbMethodName
     * @param ejbName
     * @param moduleName
     * @param applicationName  passing a null here would 
     * indicate that this is a registration for an EJB deployed under a 
     * standalone module.
     * @throws MonitoringRegistrationException
     */
    public void unregisterEJBMethodStats(String ejbMethodName, MonitoredObjectType ejbType, String ejbName,
    String moduleName, String applicationName)
    throws MonitoringRegistrationException;

    /**
     * Registers the Stats implementation for OrbConnectionManagerStats
     * @param orbConnectionManagerStats  implementation of
     * com.sun.enterprise.admin.monitor.stats.OrbConnectionManagerStats
     * @param connectionMgrName		id of the connection-manager
     * @param listener  This an optional field and when not required, a null 
     * value should be passed. If a MonitoringLevelListener is defined,
     * the listener's setLevel() method will be called in response to a change 
     * in monitoring level. 
     * @throws MonitoringRegistrationException
     */
    public void registerOrbConnectionManagerStats(OrbConnectionManagerStats orbConnectionManagerStats,
    String connectionMgrName, MonitoringLevelListener listener) throws MonitoringRegistrationException;

    /**
     * Unregisters the Stats implementation for OrbConnectionManagerStats
     * @param orbName
     * @throws MonitoringRegistrationException
     */
    public void unregisterOrbConnectionManagerStats(String orbName)
    throws MonitoringRegistrationException;
    
    /**
     * Registers the Stats implementation for ThreadPoolStats. This is meant to be used for
     * any ThreadPool in the server runtime.
     * @param ThreadPoolStats  implementation of com.sun.enterprise.admin.monitor.stats.ThreadPoolStats
     * @param poolId	String that represents the thread pool id -- needs to be unique
     * @param listener  This an optional field and when not required, a null 
     * value should be passed. If a MonitoringLevelListener is defined,
     * the listener's setLevel() method will be called in response to a change 
     * in monitoring level. 
     * @throws MonitoringRegistrationException
     */
    public void registerThreadPoolStats(ThreadPoolStats ThreadPoolStats,
    String threadPoolId, MonitoringLevelListener listener)
    throws MonitoringRegistrationException;
    
    /**
     * Unregisters the Stats implementation for ThreadPoolStats.
     * @param poolId	String representing the (unique) name of the pool
     * @throws MonitoringRegistrationException
     */
    public void unregisterThreadPoolStats(String poolId)
    throws MonitoringRegistrationException;

    /**
     * Registers the Stats implementation for ConnectorConnectionPoolStats
     * @param connectorConnectionPoolStats  implementation of 
     * com.sun.enterprise.admin.monitor.stats.ConnectorConnectionPoolStats
     * @param connectorConnectionPoolName
     * @param listener  This an optional field and when not required, a null 
     * value should be passed. If a MonitoringLevelListener is defined,
     * the listener's setLevel() method will be called in response to a change 
     * in monitoring level. 
     * @throws MonitoringRegistrationException
     */
    public void registerConnectorConnectionPoolStats(
    com.sun.enterprise.admin.monitor.stats.ConnectorConnectionPoolStats connectorConnectionPoolStats,
    String connectorConnectionPoolName, MonitoringLevelListener listener)
    throws MonitoringRegistrationException;
    
    /**
     * Unregisters the Stats implementation for ConnectorConnectionPoolStats
     * @param connectorConnectionPoolName
     * @throws MonitoringRegistrationException
     */
    public void unregisterConnectorConnectionPoolStats(String connectorConnectionPoolName)
    throws MonitoringRegistrationException;

    /**
     * Registers the Stats implementation for JDBCConnectionPoolStats
     * @param jdbcConnectionPoolStats  implementation of 
     * com.sun.enterprise.admin.monitor.stats.JDBCConnectionPoolStats
     * @param jdbcConnectionPoolName
     * @param listener  This an optional field and when not required, a null 
     * value should be passed. If a MonitoringLevelListener is defined,
     * the listener's setLevel() method will be called in response to a change 
     * in monitoring level. 
     * @throws MonitoringRegistrationException
     */
    public void registerJDBCConnectionPoolStats(
    com.sun.enterprise.admin.monitor.stats.JDBCConnectionPoolStats jdbcConnectionPoolStats,
    String jdbcConnectionPoolName, MonitoringLevelListener listener)
    throws MonitoringRegistrationException;

    /**
     * Unregisters the Stats implementation for JDBCConnectionPoolStats
     * @param jdbcConnectionPoolName
     * @throws MonitoringRegistrationException
     */
    public void unregisterJDBCConnectionPoolStats(String jdbcConnectionPoolName)
    throws MonitoringRegistrationException;

    /**
     * Registers the Stats implementation for the resource JTAStats
     * @param jtaStats  implementation of com.sun.enterprise.admin.monitor.stats.JTAStats
     * @param listener  This an optional field and when not required, a null 
     * value should be passed. If a MonitoringLevelListener is defined,
     * the listener's setLevel() method will be called in response to a change 
     * in monitoring level. 
     * @throws MonitoringRegistrationException
     */
    public void registerJTAStats(GFJTAStats jtaStats, MonitoringLevelListener listener) 
    throws MonitoringRegistrationException;

    /**
     * Unregisters the Stats implementation for the resource JTAStats
     * @throws MonitoringRegistrationException
     */
    public void unregisterJTAStats() throws MonitoringRegistrationException;    
    
    public void registerJVMStats(JVMStats stats, 
    MonitoringLevelListener listener) throws MonitoringRegistrationException;
    
    public void unregisterJVMStats() throws MonitoringRegistrationException; 
    
	/** 
	 * Registeres the Stats for Http Listener for the web-server/web-container layer.
	 * Can't be called for the same listener more than once. This method should
	 * generally be called when a particular listener is being started. 
	 * @param		listenerName	String representing the name of the listener (may not be null)
	 * @param		vsId			String representing the id of pertinent virtual server (may not be null)
	 * @throws		MonitoringRegistrationException in case there is a registration failure
	 */
    public void registerHttpListenerStats(HTTPListenerStats stats, 
                                          String listenerName,
                                          String vsId,
                                       MonitoringLevelListener listener) 
                                 throws MonitoringRegistrationException;
    
	/**
	 * Unregisters the stats for Http Listener for given listenerName and virtual-server-id.
	 * This method should generally be called when particular listener is deleted/stopped.
	 * @param			listenerName		String representing the listener's name (may not be null)
	 * @param			vsId				String represeting the virtual server id (may not be null)
	 * @throws			MonitoringRegistrationException in case of a failure
	 */
    public void unregisterHttpListenerStats(String listenerName, String vsId) 
                                 throws MonitoringRegistrationException;
     
    
	/**
	 * Registers the given listener for the given type of monitorable entity. It is required
	 * that all the implementing classes issue the callback synchonously to the
	 * provided {@link MonitoringLevelListener} before returning to the caller. 
	 * The idea is that core components should
	 * know if the registration of specific Stats is required before doing the actual
	 * registration. It is upto the components to decide what do when the callback
	 * is issued. The given listener will be added to the internal list of listeners and will
	 * be notified when the level changes. Note that this method breaks the relationship
	 * between a <em> Stats </em> object and a <em> MonitoringLevelListener </em> object. 
	 * Thus all the listeners that register through this method will receive null as the Stats parameter
	 * value in MonitoringLevelListener#changeLevel method.
	 * @param			{@link MonitoringLevelListener} that is interested in a specific type (may not be null)
	 * @param			{@link MonitoredObjectType} that indicates a specific monitored object
	 * @throws			RuntimeException if the registration fails
	 */
	public void registerMonitoringLevelListener(MonitoringLevelListener listener,
	com.sun.enterprise.admin.monitor.registry.MonitoredObjectType objType);
	
	/**
	 * Unregisters the given {@link MonitoringLevelListener} so that it is removed from internal list.
	 * The registration of same listener has to be done prior to this method call.
	 * This will usually happen when the registered listener has to exit from the VM.
	 * @param		MonitoringLevelListener that is registered earlier
	 * @throws		RuntimeException if the Listener is not registered before
	 */
	public void unregisterMonitoringLevelListener(MonitoringLevelListener listener);
        
        /**
         * registers a servlet/JSP, with the monitoring infrastructure
         * The servlet/Jsp could be part of a J2EE Application or a
         * stand-alone web module.
         * @param stats     An instance of the ServletStats
         * @param j2eeAppName   A string representing the J2EE Application
         *                      to which the webmodule belongs. If the
         *                      j2eeAppName is null, then the webmodule
         *                      is a stand-alone webmodule
         *
         * @param webModuleName The name of the web module to which the 
         *                      servlet belongs
         * @param ctxRoot The context root at which the web module has been
         *                deployed
         * @param vsId          The virtual-server, with which the 
         *                      webmodule is associated
         * @param servletName   The name of the servlet/jsp being monitored
         * @param listener      
         * @throws MonitoringRegistrationException in case of a failure
         */
        
        public void registerServletStats(com.sun.enterprise.admin.monitor.stats.ServletStats stats, 
                                     String j2eeAppName, 
                                     String webModuleName,
                                     String ctxRoot,
                                     String vsId,
                                     String servletName,
                                     MonitoringLevelListener listener) 
                                     throws MonitoringRegistrationException;
        
        /**
         * unregisters a servlet/JSP, from the monitoring infrastructure
         * The servlet/Jsp could be part of a J2EE Application or a
         * stand-alone web module.
         *
         * @param j2eeAppName   A string representing the J2EE Application
         *                      to which the webmodule belongs. If the
         *                      j2eeAppName is null, then the webmodule
         *                      is a stand-alone webmodule
         * @param webModuleName The name of the web module to which the servlet
         *                      belongs
         * @param ctxRoot The context root at which the web module has been
         *                deployed
         * @param vsId          The virtual-server, with which the 
         *                      webmodule is associated
         * @param servletName   The name of the servlet/jsp being monitored
         *
         * @throws MonitoringRegistrationException in case of a failure
         */

        public void unregisterServletStats(String j2eeAppName, 
                                           String webModuleName,
                                           String ctxRoot,
                                           String vsId,
                                           String servletName)
            throws MonitoringRegistrationException;

    /**
     * Registers the given WebModuleStats for the web module with the given 
     * <code>webModuleName</code> deployed on the virtual server with the
     * given <code>vsId</code>.
     *
     * @param stats The stats to register
     * @param j2eeAppName String representing the J2EE Application to which
     *        the web module belongs, or null if the web module is stand-alone
     * @param webModuleName The name of the web module for which to register
     *        the stats
     * @param ctxRoot The context root at which the web module has been
     *                deployed
     * @param vsId The id of the virtual-server on which the web module has
     *        been deployed
     * @param listener The listener for monitoring level changes
     *
     * @throws MonitoringRegistrationException
     */
    public void registerWebModuleStats(WebModuleStats stats,
                                       String j2eeAppName, 
                                       String webModuleName,
                                       String ctxRoot,
                                       String vsId,
                                       MonitoringLevelListener listener)
        throws MonitoringRegistrationException;

    /**
     * Unregisters any WebModuleStats from the web module with the given 
     * <code>webModuleName</code> deployed on the virtual server with the
     * given <code>vsId</code>.
     *
     * @param j2eeAppName String representing the J2EE Application to which
     *        the web module belongs, or null if the web module is stand-alone
     * @param webModuleName The name of the web module from which to unregister
     *        the stats
     * @param ctxRoot The context root at which the web module has been
     *                deployed
     * @param vsId The id of the virtual-server on which the web module has
     *        been deployed
     *
     * @throws MonitoringRegistrationException
     */
    public void unregisterWebModuleStats(String j2eeAppName, 
                                         String webModuleName,
                                         String ctxRoot,
                                         String vsId)
        throws MonitoringRegistrationException;

    /**
     * Gets the WebModuleStats associated with the web module named
     * <code>webModuleName</code> that is part of the application named
     * <code>j2eeAppName</code> and has been deployed on the virtual server
     * <code>vsId</code>.
     *
     * @param j2eeAppName String representing the J2EE Application to which
     *        the web module belongs, or null if the web module is stand-alone
     * @param webModuleName The name of the web module whose stats are to be
     *        returned
     * @param ctxRoot The context root at which the web module has been
     *                deployed
     * @param vsId The id of the virtual-server on which the web module has
     *        been deployed
     *
     * @return The desired WebModuleStats
     */
    public WebModuleStats getWebModuleStats(String j2eeAppName, 
                                            String webModuleName,
                                            String ctxRoot,
                                            String vsId);
    
    // PWC related changes
    /**
     * Registers the HttpServiceStats for PWC.
     * @param stats an instance of PWCHttpServiceStats
     * @param listener the listener for monitoring level changes
     * @throws MonitoringRegistrationException
     */
    public void registerPWCHttpServiceStats(
           com.sun.enterprise.admin.monitor.stats.PWCHttpServiceStats stats,
	   MonitoringLevelListener listener) 
           throws MonitoringRegistrationException;
    
    /**
     * Unregisters the stats for the HttpService
     * @throws MonitoringRegistrationException
     */
    public void unregisterPWCHttpServiceStats() throws MonitoringRegistrationException;
    
    
    /**
     * Registers the ConnectionQueueStats for PWC.
     * @param stats an instance of PWCConnectionQueueStats
     * @param listener the listener for monitoring level changes
     * @throws MonitoringRegistrationException
     */
    public void registerPWCConnectionQueueStats(
           com.sun.enterprise.admin.monitor.stats.PWCConnectionQueueStats stats,
	   MonitoringLevelListener listener)
           throws MonitoringRegistrationException;
    
    /**
     * Unregisters the stats for the ConnectionQueue
     * @throws MonitoringRegistrationException
     */
    public void unregisterPWCConnectionQueueStats() throws MonitoringRegistrationException;
    
    
    /**
     * Registers the DNSStats for PWC.
     * @param stats an instance of PWCDnsStats
     * @param listener the listener for monitoring level changes
     * @throws MonitoringRegistrationException
     */
    public void registerPWCDnsStats(
           com.sun.enterprise.admin.monitor.stats.PWCDnsStats stats, 
	   MonitoringLevelListener listener) 
	   throws MonitoringRegistrationException;
    
    
    /**
     * Unregisters the stats for the DNS
     * @throws MonitoringRegistrationException
     */
    public void unregisterPWCDnsStats() throws MonitoringRegistrationException;
    
    
    /**
     * Registers the KeepAliveStats for PWC.
     * @param stats an instance of PWCKeepAliveStats
     * @param listener the listener for monitoring level changes
     * @throws MonitoringRegistrationException
     */
    public void registerPWCKeepAliveStats(
           com.sun.enterprise.admin.monitor.stats.PWCKeepAliveStats stats, 
	   MonitoringLevelListener listener) 
	   throws MonitoringRegistrationException;
    
    
    /**
     * Unregisters the stats for the KeepAlive system
     * @throws MonitoringRegistrationException
     */
    public void unregisterPWCKeepAliveStats() throws MonitoringRegistrationException;
    
    
    /**
     * Registers the ThreadPoolStats for PWC.
     * @param stats an instance of PWCThreadPoolStats
     * @param listener the listener for monitoring level changes
     * @throws MonitoringRegistrationException
     */
    public void registerPWCThreadPoolStats(
           com.sun.enterprise.admin.monitor.stats.PWCThreadPoolStats stats, 
	   MonitoringLevelListener listener) 
	   throws MonitoringRegistrationException;
    
    
    /**
     * Unregisters the stats for the PWCThreadPool
     * @throws MonitoringRegistrationException
     */
    public void unregisterPWCThreadPoolStats() throws MonitoringRegistrationException;
    
    
    /**
     * Registers the FileCacheStats for PWC.
     * @param stats an instance of PWCFileCacheStats
     * @param listener the listener for monitoring level changes
     * @throws MonitoringRegistrationException
     */
    public void registerPWCFileCacheStats(
           com.sun.enterprise.admin.monitor.stats.PWCFileCacheStats stats, 
	   MonitoringLevelListener listener) 
	   throws MonitoringRegistrationException;
    
    
    /**
     * Unregisters the stats for the FileCache
     * @throws MonitoringRegistrationException
     */
    public void unregisterPWCFileCacheStats() throws MonitoringRegistrationException;
    
    
    /**
     * Registers the VirtualServerStats for PWC.
     * @param stats an instance of PWCVirtualServerStats
     * @param vsId the Id of the virtual-server for which the Stats
     *             are being registered
     * @param listener the listener for monitoring level changes
     * @throws MonitoringRegistrationException
     */
    public void registerPWCVirtualServerStats(
           com.sun.enterprise.admin.monitor.stats.PWCVirtualServerStats stats,
	   String vsId,
	   MonitoringLevelListener listener) 
	   throws MonitoringRegistrationException;
    
    
    /**
     * Unregisters the stats for the VirtualServer
     * @param vsId the Id of the virtual-server, whose stats need to be deregistered
     * @throws MonitoringRegistrationException
     */
    public void unregisterPWCVirtualServerStats(String vsId) throws MonitoringRegistrationException;
    
    /**
     * Registers the RequestStats for PWC.
     * @param stats an instance of PWCRequestStats
     * @param vsId the Id of the virtual-server for which the Stats
     *             are being registered
     * @param listener the listener for monitoring level changes
     * @throws MonitoringRegistrationException
     */
    public void registerPWCRequestStats(
           com.sun.enterprise.admin.monitor.stats.PWCRequestStats stats,
	   String vsId,
	   MonitoringLevelListener listener) 
	   throws MonitoringRegistrationException;
    
    
    /**
     * Unregisters the stats for the PWCrequest
     * @param vsId the Id of the virutal-server
     * @throws MonitoringRegistrationException
     */
    public void unregisterPWCRequestStats(String vsId) throws MonitoringRegistrationException;
    
    
    // Connector related changes
    /**
     * Registers the work management stats for the connector
     * @param stats an instance of com.sun.enterprise.admin.monitor.stats.ConnectorWorkMgmtStats
     * @param j2eeAppName the name of the j2eeApp,in which the connector is embedded
     *                    if null, indicates that a standalone connector is being monitored
     * @param moduleName  the name of the connector module
     * @param listener the listener for monitoring level changes
     * @throws MonitoringRegistrationException
     */
    public void registerConnectorWorkMgmtStats(
           com.sun.enterprise.admin.monitor.stats.ConnectorWorkMgmtStats stats, 
	   String j2eeAppName, 
	   String moduleName, 
	   MonitoringLevelListener listener) throws MonitoringRegistrationException;
    
    /**
     * Registers the work management stats for the connector
     * @param stats an instance of com.sun.enterprise.admin.monitor.stats.ConnectorWorkMgmtStats
     * @param j2eeAppName the name of the j2eeApp,in which the connector is embedded
     *                    if null, indicates that a standalone connector is being monitored
     * @param moduleName  the name of the connector module
     * @param isJms if true, indicates that the workmanagement stats are being registered 
     *              for jms-service
     * @param listener the listener for monitoring level changes
     * @throws MonitoringRegistrationException
     */
    public void registerConnectorWorkMgmtStats(
           com.sun.enterprise.admin.monitor.stats.ConnectorWorkMgmtStats stats, 
	   String j2eeAppName, 
	   String moduleName, 
       boolean isJms,
	   MonitoringLevelListener listener) throws MonitoringRegistrationException;
 
    /**
     * Unregisters the work management stats for the connector
     * 
     * @param j2eeAppName the name of the j2eeApp,in which the connector is embedded
     *                    if null, indicates that a standalone connector is being monitored
     * @param moduleName  the name of the connector module
     * 
     * @throws MonitoringRegistrationException
     */
    public void unregisterConnectorWorkMgmtStats(
	   String j2eeAppName, 
	   String moduleName) throws MonitoringRegistrationException;
 
    
    /**
     * Unregisters the work management stats for the connector
     * 
     * @param j2eeAppName the name of the j2eeApp,in which the connector is embedded
     *                    if null, indicates that a standalone connector is being monitored
     * @param moduleName  the name of the connector module
     * @param isJms if true, indicates that the workmanagement stats are being unregistered 
     *              from jms-service 
     * @throws MonitoringRegistrationException
     */
    public void unregisterConnectorWorkMgmtStats(
	   String j2eeAppName, 
	   String moduleName, boolean isJms) throws MonitoringRegistrationException;
 
    /**
     * Registers the ConnectionFactoryStats for the jms-service
     * @param   stats   an instance of com.sun.enterprise.admin.monitor.stats.ConnectionFactoryStats
     * @param   factoryName the name of the connection factory
     * @param   listener    the listener for monitoring level changes
     * @throws  MonitoringRegistrationException
     */
    public void registerConnectionFactoryStats(
           com.sun.enterprise.admin.monitor.stats.ConnectionFactoryStats stats,
           String factoryName,
           MonitoringLevelListener listener) throws MonitoringRegistrationException;
    
    /**
     * Unregisters the ConnectionFactoryStats for the jms-service
     * @param   factoryName the name of the connection factory
     * @throws MonitoringRegistrationException
     */
    public void unregisterConnectionFactoryStats(String factoryName) throws MonitoringRegistrationException;

    /**
     * Registers the Stats implementation for ConnectorConnectionPoolStats
     * @param connectorConnectionPoolStats  implementation of 
     * com.sun.enterprise.admin.monitor.stats.ConnectorConnectionPoolStats
     * @param poolName
     * @param   j2eeAppName the name of the j2eeApp
     * @param   moduleName  the name the connector module
     * @param listener  This an optional field and when not required, a null 
     * value should be passed. If a MonitoringLevelListener is defined,
     * the listener's setLevel() method will be called in response to a change 
     * in monitoring level. 
     * @throws MonitoringRegistrationException
     */
    public void registerConnectorConnectionPoolStats(
           com.sun.enterprise.admin.monitor.stats.ConnectorConnectionPoolStats stats,
           String poolName,
           String j2eeAppName,
           String moduleName,
           MonitoringLevelListener listener)
           throws MonitoringRegistrationException;
    
    /**
     * Unregisters the Stats implementation for ConnectorConnectionPoolStats
     * @param poolName
     * @param   j2eeAppName
     * @param   moduleName
     * @throws MonitoringRegistrationException
     */
    public void unregisterConnectorConnectionPoolStats(String poolName,
                                                       String j2eeAppName,
                                                       String moduleName)
                                                       throws MonitoringRegistrationException;

    
    // SessionStore monitoring related changes
    /**
     * Registers the Sessionstore stats for an ejb
     * @param stats an instance of com.sun.enterprise.admin.monitor.stats.StatefulSessionStoreStats
     * @param ejbName   the name of the ejb for which the stats are being registered
     * @param moduleName    the name of the jar to which the ejb belongs
     * @param j2eeAppName   the name of the j2eeApp, that contains the ejb jar
     *                      if null, indicates that the ejb jar is standalone
     * @param listener  the listener for monitoring level changes
     * @throws MonitoringRegistrationException
     */
    public void registerStatefulSessionStoreStats(StatefulSessionStoreStats stats, 
                                                  MonitoredObjectType ejbType,
                                                  String ejbName, 
                                                  String moduleName, 
                                                  String j2eeAppName, 
                                                  MonitoringLevelListener listener) 
                                                  throws MonitoringRegistrationException;

    /**
     * Unregisters the Sessionstore stats for the ejb
     * @param ejbName   the name of the ejb for which the stats are being unregistered
     * @param moduleName    the name of the jar, to which the ejb belongs
     * @param j2eeAppName   the name of the j2eeApp, that contains the ejb jar
     * @throws MonitoringRegistrationException
     */
    public void unregisterStatefulSessionStoreStats(  
                                                    MonitoredObjectType ejbType,
                                                    String ejbName, 
                                                    String moduleName, 
                                                    String j2eeAppName) 
                                                    throws MonitoringRegistrationException;

    
    // EJB Timer Monitoring related stuff
    /**
     * Registers the timer stats for an ejb
     * @param   stats   an instance of com.sun.enterprise.admin.monitor.stats.TimerServiceStats
     * @param   ejbName the name of the ejb for which the stats are being registered
     * @param   moduleName  the name of the jar containing the ejb
     * @param   j2eeAppName the name of the j2eeApp, that contains the ejb jar
     *                      if null, indicated that the ejb jar is standalone
     * @param   listener    the listener for monitoring level changes
     * @throws MonitoringRegistrationException
     */
    public void registerTimerStats(TimerServiceStats stats,
                                   MonitoredObjectType ejbType,
                                   String ejbName,
                                   String moduleName,
                                   String j2eeAppName,
                                   MonitoringLevelListener listener)
                                   throws MonitoringRegistrationException;
    
    /**
     * Unregisters the timer stats for an ejb
     * @param   ejbName the name of the ejb for which the stats are being unregistered
     * @param   moduleName  the name of the jar, to which the ejb belongs
     * @param   j2eeAppName the name of the j2eeApp, that contains the ejb jar
     * @throws MonitoringRegistrationException
     */
    public void unregisterTimerStats(
                                     MonitoredObjectType ejbType,
                                     String ejbName,
                                     String moduleName,
                                     String j2eeAppName)
                                     throws MonitoringRegistrationException;

    /**
     * Registers the Aggregate stats for an web service endpoint
     * @param stats an instance of
     * com.sun.appserv.management.monitor.statistics.WebServiceAggregateStats
     * @param endpointName   the name of the endpoint for which the stats are 
     *                        being registered
     * @param moduleName    the name of the jar to which the ejb belongs
     * @param ctxRoot The context root at which the web module has been
     *                deployed
     * @param j2eeAppName   the name of the j2eeApp, that contains the ejb jar
     *                      if null, indicates that the ejb jar is standalone
     * @param listener  the listener for monitoring level changes
     * @throws MonitoringRegistrationException
     */
    public void registerWSAggregateStatsForWeb(Stats stats, 
          String endpointName, String moduleName, String ctxRoot,
          String j2eeAppName,  String vs, MonitoringLevelListener listener)
                throws MonitoringRegistrationException;

    /**
     * Unregisters the web service stats in a module
     * @param endpointName   the name of the endpoint for which the stats are 
     *                       being unregistered
     * @param moduleName    the name of the jar, to which the endpoint belongs
     * @param ctxRoot The context root at which the web module has been
     *                deployed
     * @param j2eeAppName   the name of the j2eeApp, that contains the ejb jar
     * @throws MonitoringRegistrationException
     */
    public void unregisterWSAggregateStatsForWeb(String endpointName, 
                String moduleName, String ctxRoot, String j2eeAppName,
                String vs) 
                        throws MonitoringRegistrationException;

    /**
     * Registers the Aggregate stats for an web service endpoint
     * @param stats an instance of
     * com.sun.appserv.management.monitor.statistics.WebServiceAggregateStats
     * @param endpointName   the name of the endpoint for which the stats are 
     *                        being registered
     * @param moduleName    the name of the jar to which the ejb belongs
     * @param j2eeAppName   the name of the j2eeApp, that contains the ejb jar
     *                      if null, indicates that the ejb jar is standalone
     * @param listener  the listener for monitoring level changes
     * @throws MonitoringRegistrationException
     */
    public void registerWSAggregateStatsForEjb(Stats stats, 
          String endpointName, String moduleName, String j2eeAppName, 
          MonitoringLevelListener listener)
                throws MonitoringRegistrationException;

    /**
     * Unregisters the web service stats in a module
     * @param endpointName   the name of the endpoint for which the stats are 
     *                       being unregistered
     * @param moduleName    the name of the jar, to which the endpoint belongs
     * @param j2eeAppName   the name of the j2eeApp, that contains the ejb jar
     * @throws MonitoringRegistrationException
     */
    public void unregisterWSAggregateStatsForEjb(String endpointName, 
                String moduleName, String j2eeAppName) 
                        throws MonitoringRegistrationException;
}

