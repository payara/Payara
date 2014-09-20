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
 */
package com.sun.enterprise.v3.admin;

import com.sun.enterprise.util.LocalStringManagerImpl;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;
import javax.management.*;
import javax.management.loading.ClassLoaderRepository;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.rmi.ssl.SslRMIClientSocketFactory;

/**
    This Interceptor wraps the real MBeanServer so that additional interceptor code can be
    "turned on" at a later point.  However, it must be possible to start the MBeanServer even before
    the JVM calls main().  Therefore,
    <b>This class must not depend on anything that can't initialize before the JVM calls main()</b>.
    <i>This includes things like logging which is not happy being invoked
    that early.</i>
    <p>
    When instantiated at startup, the instance of this class that wraps the real MBeanServer
    is termed the "Primary Interceptor".  There can only be one such Interceptor for each
    *real* MBeanServer.  MBeanServer #0 is the Platform MBeanServer, and this class <b>must</b> be
    used for GlassFish.  Additional MBeanServers can be created if desired.
    <p>
    This class can also be used to implement an Interceptor which can be set for use by the Primary
    Interceptor.  Such interceptors are used only for get/setAttribute(s) and invoke(), though
    the use of them could be expanded for other methods.
    <p>
    Note that many methods are declared 'final' for efficiency.  If a subclass needs
    to override a method, remove 'final'. Until that time, we might as well remain efficient,
    since most methods won't be overridden.
 */
public class DynamicInterceptor implements MBeanServer
{
    private volatile MBeanServer mDelegateMBeanServer;
    private static final HashMap<String, MBeanServerConnection> instanceConnections =
            new HashMap<String, MBeanServerConnection>();;
    private static final LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(DynamicInterceptor.class);

    private static final String SERVER_PREFIX = "amx:pp=/domain/servers";
    private static final String CLUSTER_PREFIX = "amx:pp=/domain/clusters";
    private static final String CONFIG_PREFIX = "amx:pp=/domain/configs/config[";
    private static final String JSR77_PREFIX ="amx:pp=/J2EEDomain";
    private static final String MON_PREFIX ="amx:pp=/mon/server-mon[";


    public DynamicInterceptor() {
        mDelegateMBeanServer = null;
        // we must initialize this eagely in order to avoid initializing this at the time of shutdown and failing
        // because the modules have been shutdown. See GLASSFISH-18109 for more details.
        MbeanService.getInstance();
    }

    private DynamicInterceptor.ReplicationInfo getTargets( final ObjectName objectName) throws InstanceNotFoundException {

        //TODO : Check if we already have a target list for this ObjectName

        //create a  ReplicationInfo instance
        DynamicInterceptor.ReplicationInfo result = new DynamicInterceptor.ReplicationInfo();
        
        // if this is for create Mbean
        if(objectName == null) {
            result.addInstance("server");
            return result;
        }

        String oName = objectName.toString();

        // Initialize the MBeanService and check if we are on DAS
        if(MbeanService.getInstance() == null) {
            result.addInstance("server");
            return result;
        }

        // Now lets start analysing the Object Name.

        if(objectName.getKeyProperty("type") != null &&
                 (objectName.getKeyProperty("type").equals("Mapper") ||
                 objectName.getKeyProperty("type").equals("Connector") ||
                 objectName.getKeyProperty("type").equals("Engine") ||
                 objectName.getKeyProperty("type").equals("ProtocolHandler") ||
                 objectName.getKeyProperty("type").equals("Service") ||
                 objectName.getKeyProperty("type").equals("Host") ||
                 objectName.getKeyProperty("type").equals("Loader") ||
                 objectName.getKeyProperty("type").equals("JspMonitor") ||
                 objectName.getKeyProperty("type").equals("Valve"))) {
            result.addInstance("server");
            return result;

        }

        //If its a MBean corresponding to config
        if(isConfig(oName)) {
            String configName = getName(oName);
            if(configName != null && configName.endsWith("-config")) {
                String targetName = configName.substring(0, configName.indexOf("-config"));
                if( (!"default".equals(targetName)) && (!"server".equals(targetName)) ) {
                    result.addAllInstances(MbeanService.getInstance().getInstances(configName));
                }
            } else {
                result.addInstance("server");
            }
        }

        // if its a MBean corresponding to a cluster
        if(isCluster(oName)) {
            String targetName = getName(oName);
            if(targetName != null) {
                result.addAllInstances(MbeanService.getInstance().getInstances(targetName));
            }
        }

        // if its an MBean corresponding to a server
        if(isServer(oName)) {
            String targetName = getName(oName);
            if(targetName != null) {
                result.addInstance(targetName);
                if(!("server".equals(targetName)))
                    result.setTargetIsAnInstance(true);
            } else {
                result.addInstance("server");
            }
        }

        // If its an MBean corresponding to a JSR77 managed object
        if(isJSR77(oName, objectName)) {            
            if(objectName.getKeyProperty("j2eeType") != null && 
                    objectName.getKeyProperty("j2eeType").equals("J2EEDomain")) {
                result.addInstance("server");
            } else if (objectName.getKeyProperty("j2eeType") != null &&
                    objectName.getKeyProperty("j2eeType").equals("J2EEServer")) {
                String targetInstance = objectName.getKeyProperty("name");
                if(MbeanService.getInstance().isValidServer(targetInstance)) {
                    result.addInstance("server");
                    result.addInstance(targetInstance);                    
                }
            } else {
                String targetInstance = objectName.getKeyProperty("J2EEServer");
                if(MbeanService.getInstance().isValidServer(targetInstance)) {
                    result.addInstance(targetInstance);
                }
            }
        }

        // If its an monitoring MBean
        if(isMonitoring(oName)) {
            String targetName = getName(oName);
            result.addInstance(targetName);
                if(!("server".equals(targetName)))
                    result.setTargetIsAnInstance(true);
        }

        // If its a generic query
        if("amx:*".equals(oName) || "*.*".equals(oName)) {
            result.addInstance("server");
            result.addAllInstances(MbeanService.getInstance().getAllInstances());
        }

        if (objectName.getKeyProperty("type")!=null) {
            if (objectName.getKeyProperty("type").equals("domain-root") ||
                    objectName.getKeyProperty("type").equals("domain") ||
                    objectName.getKeyProperty("type").equals("resources") ||
                    objectName.getKeyProperty("type").equals("system-applications") ||
                    objectName.getKeyProperty("type").equals("applications") ||
                    objectName.getKeyProperty("type").equals("realms") ||
                    objectName.getKeyProperty("type").equalsIgnoreCase("MBeanServerDelegate")) {

                result.addInstance("server");
            }
        }

        if( oName.startsWith("amx-support") || oName.startsWith("jmxremote") ) {
            result.addInstance("server");
        }

         if((MbeanService.getInstance().isDas())) {
            result.addInstance("server");
            return result;
        } 
        // What abouut JVM
        
        return result;
    }

    private DynamicInterceptor.ReplicationInfo getInstance(final ObjectName o) throws InstanceNotFoundException {
        return getTargets(o);
    }

    private MBeanServerConnection getInstanceConnection(String instanceName) throws InstanceNotFoundException {
        // first check if this is on the same instance as the one in the argument
        // In such a case we delegate to the local MBeanServer
        if(MbeanService.getInstance().isInstance(instanceName)) {
            return getDelegateMBeanServer();
        }
        // check if this needs a secure connection
        if(MbeanService.getInstance().isSecureJMX(instanceName)) {
            return getSecureInstanceConnection(instanceName);
        }
        synchronized (instanceConnections) {
            if (!instanceConnections.containsKey(instanceName)) {
                try {
                    String urlStr = "service:jmx:rmi:///jndi/rmi://" +
                            MbeanService.getInstance().getHost(instanceName) + ":" +
                            MbeanService.getInstance().getJMXPort(instanceName) + "/jmxrmi";
                    JMXServiceURL url = new JMXServiceURL(urlStr);
                    JMXConnector jmxConn = JMXConnectorFactory.connect(url);
                    MBeanServerConnection conn = jmxConn.getMBeanServerConnection();
                    instanceConnections.put(instanceName, conn);
                } catch(Exception ex) {
                     throw new InstanceNotFoundException(ex.getLocalizedMessage());
                }
            }
            return instanceConnections.get(instanceName);
        }
    }

    private MBeanServerConnection getSecureInstanceConnection(String instanceName) throws InstanceNotFoundException {

        synchronized (instanceConnections) {
            if (!instanceConnections.containsKey(instanceName)) {
                try {
            //
            System.out.println("\nInitialize the environment map");
            final Map<String,Object> env = new HashMap<String,Object>();
            // Provide the SSL/TLS-based RMI Client Socket Factory required
            // by the JNDI/RMI Registry Service Provider to communicate with
            // the SSL/TLS-protected RMI Registry

            SslRMIClientSocketFactory csf = new SslRMIClientSocketFactory();
            env.put("com.sun.jndi.rmi.factory.socket", csf);
                    String urlStr = "service:jmx:rmi:///jndi/rmi://" +
                            MbeanService.getInstance().getHost(instanceName) + ":" +
                            MbeanService.getInstance().getJMXPort(instanceName) + "/jmxrmi";
                    JMXServiceURL url = new JMXServiceURL(urlStr);
                    JMXConnector jmxConn = JMXConnectorFactory.connect(url, env);
                    MBeanServerConnection conn = jmxConn.getMBeanServerConnection();
                    instanceConnections.put(instanceName, conn);
                } catch(Exception ex) {
                     throw new InstanceNotFoundException(ex.getLocalizedMessage());
                }
            }
            return instanceConnections.get(instanceName);
        }
    
    }

    /**
        Get the MBeanServer to which the request can be delegated.
     */
    public MBeanServer getDelegateMBeanServer() {
        return mDelegateMBeanServer;
    }

    public void setDelegateMBeanServer(final MBeanServer server)  {
        mDelegateMBeanServer    = server;
    }

    public Object invoke( final ObjectName objectName, final String operationName,
                          final Object[] params, final String[] signature)
            throws ReflectionException, InstanceNotFoundException, MBeanException {
        if(objectName == null)
            throw new InstanceNotFoundException();
        DynamicInterceptor.ReplicationInfo result = getInstance(objectName); 
        Object returnValue = null;
        try {
            for(String svr : result.getInstances()) {
                if("server".equals(svr)) {
                    returnValue = getDelegateMBeanServer().invoke( objectName, operationName, params, signature );
                } else {                    
                    returnValue = getInstanceConnection(svr).invoke(objectName, operationName, params, signature);
                }
            }
        } catch (IOException ioex) {
            throw new ReflectionException(ioex);
        }
        return returnValue;
    }
    
    public final Object getAttribute(final ObjectName objectName, final String attributeName)
            throws InstanceNotFoundException, AttributeNotFoundException, MBeanException, ReflectionException {
        if(objectName == null)
            throw new InstanceNotFoundException();
        DynamicInterceptor.ReplicationInfo result = getInstance(objectName);
        if(!result.isTargetAnInstance())
            return getDelegateMBeanServer().getAttribute( objectName, attributeName);
        try {
            return getInstanceConnection(result.getInstances().get(0)).getAttribute(objectName, attributeName);
        } catch (IOException ioex) {
            throw new ReflectionException(ioex);
        }
    }
    
    public void setAttribute(final ObjectName objectName, final Attribute attribute) throws
            InstanceNotFoundException, AttributeNotFoundException, MBeanException,
            ReflectionException, InvalidAttributeValueException {
        if(objectName == null)
            throw new InstanceNotFoundException();
        DynamicInterceptor.ReplicationInfo result = getInstance(objectName);
        try {
            if(result.isTargetAnInstance()) {
                getInstanceConnection(result.getInstances().get(0)).setAttribute(objectName, attribute);
                return;
            }
            for(String svr : result.getInstances()) {
                if("server".equals(svr))
                    getDelegateMBeanServer().setAttribute( objectName, attribute );
                else
                    getInstanceConnection(svr).setAttribute(objectName, attribute);
            }
        } catch (IOException ioex) {
            throw new ReflectionException(ioex);
        }
    }

    public final AttributeList getAttributes(final ObjectName objectName, final String[] attrNames)
            throws InstanceNotFoundException, ReflectionException {
        if(objectName == null)
            throw new InstanceNotFoundException();
        DynamicInterceptor.ReplicationInfo result = getInstance(objectName);
        try {
            if(result.isTargetAnInstance())
                return getInstanceConnection(result.getInstances().get(0)).getAttributes(objectName, attrNames);
            else
                return getDelegateMBeanServer().getAttributes( objectName, attrNames );
        } catch (IOException ioex) {
            throw new ReflectionException(ioex);
        }
    }

    public AttributeList setAttributes (final ObjectName objectName, final AttributeList attributeList)
            throws InstanceNotFoundException, ReflectionException {
        if(objectName == null)
            throw new InstanceNotFoundException();
        DynamicInterceptor.ReplicationInfo result = getInstance(objectName);
        AttributeList ret = null;
        try {
            if(result.isTargetAnInstance())
                return getInstanceConnection(result.getInstances().get(0)).setAttributes(objectName, attributeList);
            for(String svr : result.getInstances()) {
                if((result.getInstances().get(0).equals("server")))
                    ret = getDelegateMBeanServer().setAttributes( objectName, attributeList );
                else
                    ret = getInstanceConnection(svr).setAttributes(objectName, attributeList);
            }
        } catch (IOException ioex) {
            throw new ReflectionException(ioex);
        }
        return ret;
    }
    
    public final ObjectInstance registerMBean(final Object obj, final ObjectName objectName)
            throws NotCompliantMBeanException, MBeanRegistrationException, InstanceAlreadyExistsException {
        return getDelegateMBeanServer().registerMBean( obj, objectName );
    }

    public final void unregisterMBean(final ObjectName objectName)
            throws InstanceNotFoundException, MBeanRegistrationException {
       // System.out.println("Unregistering MBean :"+objectName.toString());
        if(objectName == null)
            throw new InstanceNotFoundException();
        DynamicInterceptor.ReplicationInfo result = getInstance(objectName);
        try {
            if(result.isTargetAnInstance()) {
                getInstanceConnection(result.getInstances().get(0)).unregisterMBean(objectName);
                return;
            }
            for(String svr : result.getInstances()) {
                if("server".equals(svr))
                    getDelegateMBeanServer().unregisterMBean( objectName );
                else
                    getInstanceConnection(svr).unregisterMBean(objectName);
            }
        } catch(IOException io) {
            throw new MBeanRegistrationException(io);
        }
    }

    public final Integer getMBeanCount() {
        return getDelegateMBeanServer().getMBeanCount( );
    }

    @Override
    public final Set queryMBeans( final ObjectName objectName, final QueryExp expr ) {
        //if(objectName == null)
           // return Collections.EMPTY_SET;
        try {
            Set returnVal = null;
            List<String> instance = getInstance(objectName).getInstances();
            for(String ins : instance) {
                Set tmp;
                if(ins.equals("server"))
                    tmp = getDelegateMBeanServer().queryMBeans(objectName, expr);
                else
                    tmp = getInstanceConnection(ins).queryMBeans(objectName, expr);
                if(returnVal == null)
                    returnVal = tmp;
                else
                    returnVal.addAll(tmp);
            }
            return returnVal;
        } catch (Exception e) {
            return Collections.EMPTY_SET;
        }
    }

    public final MBeanInfo getMBeanInfo( final ObjectName objectName)
            throws InstanceNotFoundException, IntrospectionException, ReflectionException {
        if(objectName == null)
            throw new InstanceNotFoundException();
        DynamicInterceptor.ReplicationInfo result = getInstance(objectName);
        try {
            if(result.isTargetAnInstance())
                return getInstanceConnection(result.getInstances().get(0)).getMBeanInfo(objectName);
            else
                return getDelegateMBeanServer().getMBeanInfo( objectName );
        } catch (IOException ioex) {
            throw new ReflectionException(ioex);
        }
    }

    public final boolean isRegistered( final ObjectName objectName) {
        if(objectName == null)
            return false;
        try {
            List<String> instance = getInstance(objectName).getInstances();
            
            for(String instanceName : instance) {
                if(instanceName.equals(System.getProperty("com.sun.aas.instanceName"))) {
                    return getDelegateMBeanServer().isRegistered( objectName );
                } else {
                    continue;
                }
            }
            return false;
            /*if((instance.get(0).equals("server")))
                return getDelegateMBeanServer().isRegistered( objectName );
            return getInstanceConnection(instance.get(0)).isRegistered(objectName); */
        } catch (Exception ex) {
            return false;
        }
    }

    public final void addNotificationListener( final ObjectName objectName,
                                               final NotificationListener notificationListener,
                                               final NotificationFilter notificationFilter, final Object obj)
            throws InstanceNotFoundException {
        if(objectName == null)
            throw new InstanceNotFoundException();
        DynamicInterceptor.ReplicationInfo result = getInstance(objectName);
        try {
            if(result.isTargetAnInstance()) {
                getInstanceConnection(result.getInstances().get(0)).addNotificationListener(
                        objectName, notificationListener, notificationFilter, obj);
                return;
            }
            for(String svr : result.getInstances()) {
                if("server".equals(svr))
                    getDelegateMBeanServer().addNotificationListener(objectName,
                            notificationListener, notificationFilter, obj);
                else
                    getInstanceConnection(svr).addNotificationListener(objectName, notificationListener,
                        notificationFilter, obj);
            }
        } catch(IOException ioex) {
            throw new InstanceNotFoundException(ioex.getLocalizedMessage());
        }
    }

    public final void addNotificationListener(final ObjectName objectName, final ObjectName objectName1,
                                              final NotificationFilter notificationFilter, final Object obj)
            throws InstanceNotFoundException {
        if(objectName == null)
            throw new InstanceNotFoundException();
        DynamicInterceptor.ReplicationInfo result = getInstance(objectName);
        try {
            if(result.isTargetAnInstance()) {
                getInstanceConnection(result.getInstances().get(0)).addNotificationListener(
                        objectName, objectName1, notificationFilter, obj);
                return;
            }
            for(String svr : result.getInstances()) {
                if("server".equals(svr))
                    getDelegateMBeanServer().addNotificationListener(objectName,
                            objectName1, notificationFilter, obj);
                else
                    getInstanceConnection(svr).addNotificationListener(objectName, objectName1,
                        notificationFilter, obj);
            }
        } catch(IOException ioex) {
            throw new InstanceNotFoundException(ioex.getLocalizedMessage());
        }
    }

    public final ObjectInstance createMBean( final String str, final ObjectName objectName)
            throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException,
                MBeanException, NotCompliantMBeanException {
        return createMBean(str, objectName, (Object[]) null, (String[]) null);
    }

    public final ObjectInstance createMBean( final String str, final ObjectName objectName,
                                             final ObjectName objectName2)
            throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException,
            NotCompliantMBeanException, InstanceNotFoundException {
        return createMBean(str, objectName, objectName2, (Object[]) null, (String[]) null);
    }

    public final ObjectInstance createMBean( final String str, final ObjectName objectName, final Object[] obj,
                                             final String[] str3)
            throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException,
            MBeanException, NotCompliantMBeanException {
        try {
            DynamicInterceptor.ReplicationInfo result = getInstance(objectName);
            ObjectInstance ret = null;
            if(result.isTargetAnInstance())
                return getInstanceConnection(result.getInstances().get(0)).createMBean(str, objectName, obj, str3);
            for(String svr : result.getInstances())
                if(svr.equals("server"))
                    ret = getDelegateMBeanServer().createMBean (str, objectName, obj, str3);
                else
                    ret = getInstanceConnection(svr).createMBean(str, objectName, obj, str3);
            return ret;
        } catch (InstanceNotFoundException ex) {
            throw new MBeanException(ex);
        } catch (IOException ex) {
            throw new MBeanException(ex);
        }
    }

    public final ObjectInstance createMBean ( final String str, final ObjectName objectName,
                                              final ObjectName objectName2, final Object[] obj, final String[] str4)
            throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException,
            MBeanException, NotCompliantMBeanException, InstanceNotFoundException {
        try {
            DynamicInterceptor.ReplicationInfo result = getInstance(objectName);
            ObjectInstance ret = null;
            if(result.isTargetAnInstance())
                return getInstanceConnection(result.getInstances().get(0)).createMBean(str, objectName,
                        objectName2, obj, str4);
            for(String svr : result.getInstances())
                if(svr.equals("server"))
                    ret = getDelegateMBeanServer().createMBean (str, objectName, objectName2, obj, str4);
                else
                    ret = getInstanceConnection(svr).createMBean(str, objectName, objectName2, obj, str4);
            return ret;
        } catch (InstanceNotFoundException ex) {
            throw new MBeanException(ex);
        } catch (IOException ex) {
            throw new MBeanException(ex);
        }
    }

    public final ObjectInputStream deserialize (String str, byte[] values)
            throws OperationsException, ReflectionException {
        return getDelegateMBeanServer().deserialize (str, values);
    }

    public final ObjectInputStream deserialize( final ObjectName objectName, final byte[] values)
            throws InstanceNotFoundException, OperationsException {
        return getDelegateMBeanServer().deserialize (objectName, values);
    }

    public final ObjectInputStream deserialize( final String str, final ObjectName objectName, byte[] values)
            throws InstanceNotFoundException, OperationsException, ReflectionException {
        return getDelegateMBeanServer().deserialize (str, objectName, values);
    }

    public final String getDefaultDomain() {
        return getDelegateMBeanServer().getDefaultDomain();
    }
    
    public final ObjectInstance getObjectInstance(ObjectName objectName) throws InstanceNotFoundException {
        if(objectName == null)
            throw new InstanceNotFoundException();
        List<String> instance = getInstance(objectName).getInstances();
        if(instance.size() != 1)
            throw new InstanceNotFoundException(localStrings.getLocalString("interceptor.objectName.wrongservernames",
                    "This mbean call does not support multiple target instances"));
        if((instance.get(0).equals("server")))
            return getDelegateMBeanServer().getObjectInstance(objectName);
        try {
            return getInstanceConnection(instance.get(0)).getObjectInstance(objectName);
        } catch (IOException ioex) {
            throw new InstanceNotFoundException(ioex.getLocalizedMessage());
        }
    }
    
    public final Object instantiate( final String str) throws ReflectionException, MBeanException {
        return getDelegateMBeanServer().instantiate(str);
    }
    
    public final Object instantiate( final String str, final ObjectName objectName)
            throws ReflectionException, MBeanException, InstanceNotFoundException {
        if(objectName == null)
            throw new InstanceNotFoundException();
        return getDelegateMBeanServer().instantiate(str, objectName);
    }
    
    public final Object instantiate( final String str, final Object[] obj, final String[] str2)
            throws ReflectionException, MBeanException {
        return getDelegateMBeanServer().instantiate(str, obj, str2);
    }
    
    public final Object instantiate( final String str, final ObjectName objectName, final Object[] obj,
                                     final String[] str3)
            throws ReflectionException, MBeanException, InstanceNotFoundException {
        if(objectName == null)
            throw new InstanceNotFoundException();
        return getDelegateMBeanServer().instantiate(str, objectName, obj, str3);
    }

    public final boolean isInstanceOf ( final ObjectName objectName,  final String str)
            throws InstanceNotFoundException {
        if(objectName == null)
            throw new InstanceNotFoundException();
        List<String> instance = getInstance(objectName).getInstances();
        if(instance.size() != 1)
            throw new InstanceNotFoundException(localStrings.getLocalString("interceptor.objectName.wrongservernames",
                    "This mbean call does not support multiple target instances"));
        if((instance.get(0).equals("server")))
            return getDelegateMBeanServer().isInstanceOf(objectName, str);
        try {
            return getInstanceConnection(instance.get(0)).isInstanceOf(objectName, str);
        } catch (IOException ioex) {
            throw new InstanceNotFoundException(ioex.getLocalizedMessage());
        }
    }

    public final Set queryNames( final ObjectName objectName, final QueryExp queryExp) {
        Set returnVal = null;
        //if(objectName == null)
            //return Collections.EMPTY_SET;
        List<String> instance;
        try {
            instance = getInstance(objectName).getInstances();
        } catch(InstanceNotFoundException e) {
            return Collections.EMPTY_SET;
        }
        for(String ins : instance) {
            Set tmp = null;
            if(ins.equals("server")) {
                tmp = getDelegateMBeanServer().queryNames( objectName, queryExp);
            } else {
                try {
                    tmp = getInstanceConnection(ins).queryNames(objectName, queryExp);
                } catch(Exception e) {
                    //Swallowing this intentionally
                    //Because this can happen only if the instance is down / not responding
                }
            }
            if(tmp != null) {
                if(returnVal == null)
                    returnVal = tmp;
                else
                    returnVal.addAll(tmp);
            }
        }
        if(returnVal == null)
            return Collections.EMPTY_SET;
        return returnVal;
    }

    public final void removeNotificationListener(final ObjectName objectName,  final ObjectName objectName1)
            throws InstanceNotFoundException, ListenerNotFoundException {
        if(objectName == null)
            throw new InstanceNotFoundException();
        DynamicInterceptor.ReplicationInfo result = getInstance(objectName);
        try {
            if(result.isTargetAnInstance()) {
                getInstanceConnection(result.getInstances().get(0)).removeNotificationListener(objectName, objectName1);
                return;
            }
            for(String svr : result.getInstances()) {
                if(svr.equals("server"))
                    getDelegateMBeanServer().removeNotificationListener( objectName, objectName1);
                else
                    getInstanceConnection(svr).removeNotificationListener(objectName, objectName1);
            }
        } catch (IOException ioex) {
            throw new InstanceNotFoundException(ioex.getLocalizedMessage());
        }
    }

    public final void removeNotificationListener( final ObjectName objectName,
                                                  final NotificationListener notificationListener)
            throws InstanceNotFoundException, ListenerNotFoundException {
        if(objectName == null)
            throw new InstanceNotFoundException();
        DynamicInterceptor.ReplicationInfo result = getInstance(objectName);
        try {
            if(result.isTargetAnInstance()) {
                getInstanceConnection(result.getInstances().get(0)).removeNotificationListener(objectName, notificationListener);
                return;
            }
            for(String svr : result.getInstances()) {
                if(svr.equals("server"))
                    getDelegateMBeanServer().removeNotificationListener( objectName, notificationListener);
                else
                    getInstanceConnection(svr).removeNotificationListener(objectName, notificationListener);
            }
        } catch (IOException ioex) {
            throw new InstanceNotFoundException(ioex.getLocalizedMessage());
        }
    }
      
    public final void removeNotificationListener( final ObjectName objectName,
                                                  final NotificationListener notificationListener,
                                                  final NotificationFilter notificationFilter, final Object obj)
            throws InstanceNotFoundException, ListenerNotFoundException {
        if(objectName == null)
            throw new InstanceNotFoundException();
        DynamicInterceptor.ReplicationInfo result = getInstance(objectName);
        try {
            if(result.isTargetAnInstance()) {
                getInstanceConnection(result.getInstances().get(0)).removeNotificationListener(objectName,
                        notificationListener, notificationFilter, obj);
                return;
            }
            for(String svr : result.getInstances()) {
                if(svr.equals("server"))
                    getDelegateMBeanServer().removeNotificationListener( objectName,
                            notificationListener,notificationFilter, obj);
                else
                    getInstanceConnection(svr).removeNotificationListener(objectName,
                            notificationListener, notificationFilter, obj);
            }
        } catch (IOException ioex) {
            throw new InstanceNotFoundException(ioex.getLocalizedMessage());
        }
    }
    
    public final void removeNotificationListener( final ObjectName objectName, final ObjectName objectName1,
                                                  final NotificationFilter    notificationFilter, final Object obj)
            throws InstanceNotFoundException, ListenerNotFoundException {
        if(objectName == null)
            throw new InstanceNotFoundException();
        DynamicInterceptor.ReplicationInfo result = getInstance(objectName);
        try {
            if(result.isTargetAnInstance()) {
                getInstanceConnection(result.getInstances().get(0)).removeNotificationListener(objectName,
                        objectName1, notificationFilter, obj);
                return;
            }
            for(String svr : result.getInstances()) {
                if(svr.equals("server"))
                    getDelegateMBeanServer().removeNotificationListener( objectName,
                            objectName1,notificationFilter, obj);
                else
                    getInstanceConnection(svr).removeNotificationListener(objectName,
                            objectName1, notificationFilter, obj);
            }
        } catch (IOException ioex) {
            throw new InstanceNotFoundException(ioex.getLocalizedMessage());
        }    }

    public final ClassLoader getClassLoader( final ObjectName objectName) throws InstanceNotFoundException {
        if(objectName == null)
            throw new InstanceNotFoundException();
        return getDelegateMBeanServer().getClassLoader( objectName );
    }
    
    public final ClassLoader getClassLoaderFor( final ObjectName objectName) throws InstanceNotFoundException {
        if(objectName == null)
            throw new InstanceNotFoundException();
        return getDelegateMBeanServer().getClassLoaderFor( objectName );
    }
    
    public final ClassLoaderRepository getClassLoaderRepository() {
    	return getDelegateMBeanServer().getClassLoaderRepository();
    }
    
    public final String[] getDomains() {
        return getDelegateMBeanServer().getDomains();
    }

    private boolean isConfig(String oName) {
        return oName.startsWith(CONFIG_PREFIX);
    }

    private boolean isCluster(String oName) {
        return oName.startsWith(CLUSTER_PREFIX);
    }

    private boolean isServer(String oName) {
        return oName.startsWith(SERVER_PREFIX);
    }

    private boolean isJSR77(String oName, ObjectName o) {        
        if(o.getKeyProperty("j2eeType") !=null) {
            return true;
        } else if(oName.startsWith(JSR77_PREFIX)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isMonitoring(String oName) {
        return oName.startsWith(MON_PREFIX);
    }

    private String getName(String oName) {
        if(oName.indexOf("[") != -1 ) {
          return oName.substring(oName.indexOf("[") + 1, oName.indexOf("]"));
        } else {
            return null;
        }
    }

    private static class ReplicationInfo {
        private boolean instanceTarget = false;
        private List<String> instances = new ArrayList<String>();

        boolean isTargetAnInstance() { return instanceTarget;}

        void setTargetIsAnInstance(boolean b) { instanceTarget = b;}

        List<String> getInstances() { return instances;}

        void addInstance(String s) {
            if(!instances.contains(s)) {
                instances.add(s);
            }
        }

        void addAllInstances(List list) {
            instances.addAll(list);
        }
    }
}
