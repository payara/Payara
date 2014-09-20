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

package com.sun.enterprise.connectors.inbound;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

import com.sun.appserv.connectors.internal.api.ConnectorConstants;
import com.sun.appserv.connectors.internal.api.ConnectorRuntimeException;
import com.sun.appserv.connectors.internal.api.ConnectorsUtil;
import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.connectors.ActiveResourceAdapter;
import com.sun.enterprise.connectors.ConnectorRegistry;
import com.sun.enterprise.connectors.ConnectorRuntime;
import com.sun.enterprise.connectors.util.SetMethodAction;
import com.sun.enterprise.deployment.ConnectorDescriptor;
import com.sun.enterprise.deployment.EjbMessageBeanDescriptor;
import com.sun.enterprise.deployment.MessageListener;
import com.sun.enterprise.resource.ResourceHandle;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.logging.LogDomains;
import org.glassfish.ejb.api.MessageBeanListener;
import org.glassfish.ejb.api.MessageBeanProtocolManager;
import org.glassfish.ejb.spi.MessageBeanClient;
import org.glassfish.internal.api.Globals;
import org.glassfish.server.ServerEnvironmentImpl;

/**
 * Main helper implementation for message-beans associated with
 * a queue. Uses connection consumer for concurrent message
 * delivery.
 *
 * @author Qingqing Ouyang
 */
public final class ConnectorMessageBeanClient
        implements MessageBeanClient, MessageEndpointFactory {

    private String activationName;
    
    private ConnectorRegistry registry_;

    private MessageBeanProtocolManager messageBeanPM_;
    private final EjbMessageBeanDescriptor descriptor_;
    private final BasicResourceAllocator allocator_;
    private Class beanClass_;
    private boolean started_ = false;

    private final int CREATED = 0;
    private final int BLOCKED = 1;
    private final int UNBLOCKED = 2;
    private int myState = CREATED;

    private final long WAIT_TIME = 60000;

    private static final String RA_MID="com.sun.enterprise.connectors.inbound.ramid";

    private StringManager localStrings =
                StringManager.getManager(ConnectorMessageBeanClient.class);

    //unique identify a message-driven bean
    private String beanID_; //appName:modlueID:beanName

    private static final Logger logger =
            LogDomains.getLogger(ConnectorMessageBeanClient.class, LogDomains.RSR_LOGGER);

    /**
     * Creates an instance of <code>ConnectorMessageBeanClient</code>
     *
     * @param descriptor <code>EjbMessageBeanDescriptor</code> object.
     */
    public ConnectorMessageBeanClient(EjbMessageBeanDescriptor descriptor) {
        this.descriptor_ = descriptor;
        allocator_ = new BasicResourceAllocator();

        String appName = descriptor.getApplication().getName();

        String moduleID = descriptor.getEjbBundleDescriptor().getModuleID();

        String beanName = descriptor.getName();

        activationName = null;

        beanID_ = appName + ":" + moduleID + ":" + beanName;

        registry_ = ConnectorRegistry.getInstance();
    }

    /**
     * Gets executed as part of message bean deployment. Creates the
     * <code>ActivationSpec</code> javabean and does endpointfactory
     * activation with the resource adapter. This code also converts
     * all J2EE 1.3 MDB properties to MQ resource adapter activation
     * spec properties, if user doesnt specifies resource adapter
     * module name in sun-ejb-jar.xml of the MDB. This is done using
     * <code>com.sun.enterprise.connector.system.ActiveJmsResourceAdapter
     * </code>
     *
     * @param messageBeanPM <code>MessageBeanProtocolManager</code> object.
     */
    public void setup(MessageBeanProtocolManager messageBeanPM) throws Exception {

        ClassLoader loader = descriptor_.getEjbBundleDescriptor().getClassLoader();

        if (loader == null) {
            loader = Thread.currentThread().getContextClassLoader();
        }

        beanClass_ = loader.loadClass(descriptor_.getEjbClassName());

        messageBeanPM_ = messageBeanPM;

        String resourceAdapterMid = getResourceAdapterMid(descriptor_);

        ActiveInboundResourceAdapter aira = getActiveResourceAdapter(resourceAdapterMid);

        aira.updateMDBRuntimeInfo(descriptor_,
                                       messageBeanPM_.getPoolDescriptor());

        //the resource adapter this MDB client is deployed to
        ResourceAdapter ra = aira.getResourceAdapter();

        if(ra == null){
            String i18nMsg = localStrings.getString("msg-bean-client.ra.class.not.specified", resourceAdapterMid);
            throw new ConnectorRuntimeException(i18nMsg);
        }

        ConnectorDescriptor desc = aira.getDescriptor();

        MessageListener msgListener = getMessageListener(desc);

        String activationSpecClassName = null;
        if (msgListener != null) {
            activationSpecClassName = msgListener.getActivationSpecClass();
        }

        if (activationSpecClassName != null) {
            if (logger.isLoggable(Level.FINEST)) {
                String msg = "ActivationSpecClassName = " + activationSpecClassName;
                logger.log(Level.FINEST, msg);
            }

            try {
                ActivationSpec activationSpec = getActivationSpec(aira, activationSpecClassName);
                activationSpec.setResourceAdapter(ra);

                //at this stage, activation-spec is created, config properties merged with ejb-descriptor.
                //validate activation-spec now
                ConnectorRuntime runtime = ConnectorRuntime.getRuntime();
                runtime.getConnectorBeanValidator().validateJavaBean(activationSpec, resourceAdapterMid);

                aira.validateActivationSpec(activationSpec);

                myState = BLOCKED;

                ra.endpointActivation(this, activationSpec);

                aira.addEndpointFactoryInfo(beanID_,
                        new MessageEndpointFactoryInfo(this, activationSpec));

            } catch (Exception ex) {
                Object args[] = new Object[]{resourceAdapterMid, activationSpecClassName, ex};
                logger.log(Level.WARNING, "endpoint.activation.failure", args);
                throw (Exception) (new Exception()).initCause(ex);
            }
        } else {
            //FIXME  throw some exception here.
            throw new Exception("Unsupported message listener type");
        }
    }

    /**
     * derive the resource-adapter-mid in the following order <br/>
     * a) specified in the glassfish-ejb-jar / sun-ejb-jar descriptor<br/>
     * b) jms-ra message-listener of type javax.jms.MessageListener<br/>
     * c) Check the resource-adapters supporting the message-listener-type and
     * if there is only one use it, otherwise fail.<br/>
     * @param descriptor_ EJB Descriptor
     * @return resource-adapter-mid (resource-adapter-name)
     * @throws ConnectorRuntimeException
     */
    private String getResourceAdapterMid(EjbMessageBeanDescriptor descriptor_) throws ConnectorRuntimeException {
        String resourceAdapterMid = descriptor_.getResourceAdapterMid();
        if (resourceAdapterMid == null) {
            resourceAdapterMid = System.getProperty(RA_MID);
        }

        if (resourceAdapterMid == null) {
            String messageListener = descriptor_.getMessageListenerType();
            //DOL of MDB descriptor has default value as "javax.jms.MessageListener" which
            //will take care of the case when the message-listener-type is not specified in the DD
            if(ConnectorConstants.JMS_MESSAGE_LISTENER.equals(messageListener)){
                resourceAdapterMid = ConnectorRuntime.DEFAULT_JMS_ADAPTER;
                logger.fine("No ra-mid is specified, using default JMS Resource Adapter for message-listener-type " +
                        "["+descriptor_.getMessageListenerType()+"]");
            }else{
                List<String> resourceAdapters =
                        ConnectorRegistry.getInstance().getConnectorsSupportingMessageListener(messageListener);
                if(resourceAdapters.size() == 1){
                    resourceAdapterMid = resourceAdapters.get(0);
                    String message = localStrings.getString("msg-bean-client.defaulting.message-listener.supporting.rar",
                            resourceAdapterMid, messageListener);
                    logger.info(message);
                }else if(resourceAdapters.size()<=0){
                    String message = localStrings.getString("msg-bean-client.could-not-detect-ra-mid",
                            descriptor_.getMessageListenerType() );

                    throw new ConnectorRuntimeException(message);
                }else{
                    String message = localStrings.getString("msg-bean-client.multiple-ras-supporting-message-listener",
                            messageListener);
                    throw new ConnectorRuntimeException(message);
                }
            }
        }
        return resourceAdapterMid;
    }

    private ActivationSpec getActivationSpec(ActiveInboundResourceAdapter aira, String activationSpecClassName)
            throws Exception {
        ClassLoader cl = aira.getClassLoader();
        Class aClass = cl.loadClass(activationSpecClassName);

        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "classloader = "
                    + aClass.getClassLoader());
            logger.log(Level.FINEST, "classloader parent = "
                    + aClass.getClassLoader().getParent());
        }

        ActivationSpec activationSpec =
                (ActivationSpec) aClass.newInstance();
        Set props = ConnectorsUtil.getMergedActivationConfigProperties(getDescriptor());

        AccessController.doPrivileged(new SetMethodAction(activationSpec, props));
        return activationSpec;
    }

    private MessageListener getMessageListener(ConnectorDescriptor desc) {
        String msgListenerType = getDescriptor().getMessageListenerType();
        if (msgListenerType == null || "".equals(msgListenerType))
            msgListenerType = "javax.jms.MessageListener";

        Iterator i =
                desc.getInboundResourceAdapter().getMessageListeners().iterator();

        MessageListener msgListener = null;
        while (i.hasNext()) {
            msgListener = (MessageListener) i.next();
            if (msgListenerType.equals(msgListener.getMessageListenerType()))
                break;
        }
        return msgListener;
    }

    private ActiveInboundResourceAdapter getActiveResourceAdapter(String resourceAdapterMid) throws Exception {
        Object activeRar = registry_.getActiveResourceAdapter(resourceAdapterMid);

        // Except system-rars, all other rars are loaded eagerly.
        // Check whether the rar is a system-rar.
        // (as of now, jms-ra is the only inbound system-rar)  
        if (activeRar == null && ConnectorsUtil.belongsToSystemRA(resourceAdapterMid)) {
            ConnectorRuntime crt = ConnectorRuntime.getRuntime();
            crt.loadDeferredResourceAdapter(resourceAdapterMid);
            activeRar = registry_.getActiveResourceAdapter(resourceAdapterMid);
        }

        if (activeRar == null) {
            String msg = "Resource adapter " + resourceAdapterMid + " is not deployed";
            throw new ConnectorRuntimeException(msg);
        }

        if (!(activeRar instanceof ActiveInboundResourceAdapter)) {
            throw new Exception("Resource Adapter selected doesn't support Inbound");
        }
        return (ActiveInboundResourceAdapter) activeRar;
    }

    /**
     * Marks the completion of MDB deployment. Unblocks the createEndPoint
     * method call.
     *
     * @throws Exception
     */
    public void start() throws Exception {
        logger.logp(Level.FINEST,
                "ConnectorMessageBeanClient", "start", "called...");
        started_ = true;
        synchronized (this) {
            myState = UNBLOCKED;
            notifyAll();
        }
    }

    /**
     * Does endpoint deactivation with the resource adapter.
     * Also remove sthe <code>MessageEndpointFactoryInfo</code>
     * from house keeping.
     */
    public void close() {
        logger.logp(Level.FINEST,
                "ConnectorMessageBeanClient", "close", "called...");

        started_ = false; //no longer available


        String resourceAdapterMid = null;
        try {
            resourceAdapterMid = getResourceAdapterMid(descriptor_);
        } catch (ConnectorRuntimeException e) {
            String message = localStrings.getString("msg-bean-client.could-not-derive-ra-mid", descriptor_.getName());
            logger.log(Level.WARNING, message, e);
        }

        ActiveResourceAdapter activeRar =
                registry_.getActiveResourceAdapter(resourceAdapterMid);

        if (activeRar instanceof ActiveInboundResourceAdapter) { //in case the RA is already undeployed
            ActiveInboundResourceAdapter rar = (ActiveInboundResourceAdapter) activeRar;
            MessageEndpointFactoryInfo info = rar.getEndpointFactoryInfo(beanID_);

            if (info != null) {
                rar.getResourceAdapter().endpointDeactivation(
                        info.getEndpointFactory(), info.getActivationSpec());

                rar.removeEndpointFactoryInfo(beanID_);
            } else {
                logger.log(Level.FINE, "Not de-activating the end point, since it is not activated");
            }
        }
    }

    private EjbMessageBeanDescriptor getDescriptor() {
        return descriptor_;
    }

    /**
     * Creates a MessageEndpoint. This method gets blocked either until start()
     * is called or until one minute. This is the time for completion
     * of MDB deployment.
     * <p/>
     * Internally this method creates a message bean listener from the MDB
     * container and a proxy object fo delivering messages.
     *
     * @return <code>MessageEndpoint</code> object.
     * @throws <code>UnavailableException</code>
     *          In case of any failure. This
     *          should change.
     */
    public MessageEndpoint
    createEndpoint(XAResource xa) throws UnavailableException {
        // This is a temporary workaround for blocking the the create enpoint
        // until the deployment completes.  One thread would wait for maximum a
        // a minute.
        return createEndpoint(xa, WAIT_TIME);
    }

    /**
     * Checks whether the message delivery is transacted for the method.
     *
     * @return true or false.
     */
    public boolean isDeliveryTransacted(Method method) {
        return messageBeanPM_.isDeliveryTransacted(method);
    }

    /**
     * @return beanID of the message bean client
     */
    public String toString() {
        return beanID_;
    }

    /**
     * {@inheritDoc}
     */
    public MessageEndpoint createEndpoint(XAResource xaResource, long timeout) throws UnavailableException {
        synchronized (this) {
            while (myState == BLOCKED) {
                try {
                    wait(timeout);
                } catch (Exception e) {
                    // This exception should not affect the functionality.
                } finally {

                    // Once the first thread comes out of wait, block is
                    // is removed. This makes sure that the time for which the
                    // the block remains is limited. Max 2x6000.
                    myState = UNBLOCKED;
                }
            }
        }

        if (!started_) {
            logger.log(Level.WARNING, "endpointfactory.unavailable");
            throw new UnavailableException(
                    "EndpointFactory is currently not available");
        }

        MessageEndpoint endpoint = null;
        try {
            ResourceHandle resourceHandle = allocator_.createResource(xaResource);

            MessageBeanListener listener =
                    messageBeanPM_.createMessageBeanListener(resourceHandle);

            MessageEndpointInvocationHandler handler =
                    new MessageEndpointInvocationHandler(listener, messageBeanPM_);
            endpoint = (MessageEndpoint) messageBeanPM_.createMessageBeanProxy(handler);
        } catch (Exception ex) {
            throw (UnavailableException)
                    (new UnavailableException()).initCause(ex);
        }
        return endpoint;
    }
    /**
     * {@inheritDoc}
     * @Override
     */
    public String getActivationName(){
      if(activationName == null){
        
        String appName = descriptor_.getApplication().getName();
        String moduleID = descriptor_.getEjbBundleDescriptor().getModuleID();
        int pound = moduleID.indexOf("#");
        if(pound>=0){
            // the module ID is in the format: appName#ejbName.jar
            // remove the appName part since it is duplicated
            moduleID = moduleID.substring(pound+1);
        }
        String mdbClassName = descriptor_.getEjbClassName();

        ServerEnvironmentImpl env = Globals.get(ServerEnvironmentImpl.class);
        String instanceName = env.getInstanceName();
        
        Domain domain = Globals.get(Domain.class);
        String domainName = domain.getName();
        Cluster cluster = domain.getServerNamed(instanceName).getCluster();

        String clusterName=null;
        if(cluster!=null){
          // this application is deployed in a cluster
          clusterName = cluster.getName();
        }
        
        if(clusterName!=null){
            // this application is deployed in a cluster
            activationName = combineString(domainName, clusterName, appName, moduleID, mdbClassName);
            
        }else{
            // this application is deployed in a stand-alone server instance.
            activationName = combineString(domainName, instanceName, appName, moduleID, mdbClassName);
        }
      }
      return activationName;
    }

//    /**
//     * Compute the activation name according to the names of domain, cluster, instance,
//     * application, module and MDB. 
//     * 
//     * Check whether the combination of the activation name and instance name
//     * is longer than 128 characters. If yes, then compact the activation name.
//     * 
//     */
//    public String computeActivationName(String domainName, String clusterName, 
//            String instanceName, String appName, String moduleID, String mdbName){
//        
//        if(clusterName!=null){
//            // this application is deployed in a cluster
//            String fullActivationName = combineString(domainName, clusterName, appName, moduleID, mdbName);
//            // Check if the combination of activation name and instance name is longer
//            // than 128 characters. If yes, then compact it.
//            // BootstrapContextImpl.getInstanceName() will return at most 24 characters
//            int instanceLenth = Math.min(instanceName.length(), BootstrapContextImpl.MAX_INSTANCE_LENGTH);
//            if((fullActivationName.length()+instanceLenth)>127){
//                String compactedName = compactActivationName(fullActivationName, domainName, 
//                        clusterName, instanceName, appName, moduleID, mdbName);
//                logger.log(Level.INFO, "The original activation is: "+fullActivationName
//                        +", because it is too long, compact it to: "+compactedName);
//                return compactedName;
//            }else{
//                return fullActivationName;
//            }
//
//        }else{
//            // this application is deployed in a stand-alone server instance.
//            String fullActivationName = combineString(domainName, instanceName, appName, moduleID, mdbName);
//            if(fullActivationName.length()>128){
//                String compactedName = compactActivationName(fullActivationName, domainName, 
//                        instanceName, appName, moduleID, mdbName);
//                logger.log(Level.INFO, "The original activation is: "+fullActivationName
//                        +", because it is too long, compact it to: "+compactedName);
//                return compactedName;
//            }else{
//                return fullActivationName;
//            }
//        }
//    }
//    /* 
//     * This method is called when the server is a clustered instance.
//     * 
//     * Compact the activationName so that the combination of it and instance name
//     * is no longer than 128 characters. The combination of activation name and
//     * instance name is of the format:
//     *   domain_cluster_application_module_mdb_MD5_instance
//     * 
//     * The basic idea is that: 
//     *   1. Add the MD5 digest of the original activationName. This can ensure the 
//     *      (almost) uniqueness of the activation name. The new activation name
//     *      will be of the format: domain_cluster_application_module_mdb_MD5 
//     *   2. The instance name is no longer than 24 characters, the method 
//     *      BootstrapContextImpl.getInstanceName() will truncate instance name 
//     *      if need.
//     *   3. Truncate the longest element among the domain, instance, application,
//     *      module and mdb names, until the combination of new activation 
//     *      name and instance name is no longer than 128 characters.
//     *      
//     */ 
//    private String compactActivationName(String originalActivationName, 
//            String domainName, String clusterName, String instanceName, 
//            String appName, String moduleID, String mdbName){
//
//        String[] names = new String[5];
//        names[0] = domainName;
//        names[1] = clusterName;
//        names[2] = appName;
//        names[3] = moduleID;
//        names[4] = mdbName;
//        
//        // the instanceName is longer than 24 characters.
//        int instanceLenth = Math.min(instanceName.length(), BootstrapContextImpl.MAX_INSTANCE_LENGTH);
//
//        // the MD5 digestion has 32 characters, the remaining capacity 
//        // is 128-(32 + size_of_instanceName + 2).
//        int maxTotal = 94 - instanceLenth;
//        StringBuilder compacted = compactString(names, maxTotal);
//
//        String digest = computeMD5(originalActivationName);
//        compacted.append("_").append(digest);
//        return compacted.toString();
//    }
//
//    /* 
//     * This method is called when the server is a stand-alone instance.
//     * 
//     * Compact the activationName so that it is no longer than 128 characters.
//     * The new activation name is of the format:
//     *   domain_instance_application_module_mdb_MD5
//     * 
//     * The basic idea is that: 
//     *   1. Add the MD5 digest of the original activationName which can ensure the 
//     *      (almost) uniqueness of the activation. 
//     *   2. Truncate the longest element among the domain, instance, application,
//     *      module and mdb names, until the combination of them and MD5 
//     *      digestion is no longer than 128 characters.
//     *      
//     */ 
//    private String compactActivationName(String originalActivationName, String domainName, 
//            String instanceName, String appName, String moduleID, String mdbName){
//
//        String[] names = new String[5];
//        names[0] = domainName;
//        names[1] = instanceName;
//        names[2] = appName;
//        names[3] = moduleID;
//        names[4] = mdbName;
//        // the MD5 digestion has 32 characters, the remaining capacity is 
//        // (128-32-1) = 95 characters. 
//        StringBuilder compacted = compactString(names, 95);
//
//        String digest = computeMD5(originalActivationName);
//        compacted.append("_").append(digest);
//        return compacted.toString();
//    }
//    
//    /* 
//     * Compact the given Strings so that their total size is no longer than
//     * maxTotal characters. 
//     *  
//     * Find the longest string, truncate its first half part, because in the most 
//     * cases, the last characters are more significant. Do this operation until 
//     * the total size is no longer than maxTotal:
//     * 
//     */ 
//    private StringBuilder compactString(String[] texts, int maxTotal){
//        int arrayLengh = texts.length;
//
//        int total=0;
//        for(int i=0; i<arrayLengh; i++ ){
//            total+=texts[i].length();
//        }
//        // add the size of delimiter characters between the strings
//        total+=arrayLengh-1;
//        
//        while(total>maxTotal){
//            // find the longest item
//            int longestLength = 0;
//            int longestIndex = 0;
//            for(int i=0; i<arrayLengh; i++ ){
//                if(texts[i].length()>longestLength){
//                    longestLength = texts[i].length();
//                    longestIndex=i;
//                }
//            }
//            // truncate the first half part of the string
//            // because it seems that the last characters are more significant. 
//            int trunctionSize= Math.min(longestLength>>1, total-maxTotal);
//            texts[longestIndex]=texts[longestIndex].substring(trunctionSize, longestLength);
//
//            // re-compute the total size
//            total -= trunctionSize;
//        }
//
//        StringBuilder sb = new StringBuilder(128);
//        sb.append(texts[0]);
//        for(int i=1; i<arrayLengh; i++ ){
//            sb.append("_").append(texts[i]);
//        }
//        return sb;
//    }
    
    private String combineString(String... names) {
        StringBuilder sb = new StringBuilder(128);
        sb.append(names[0]);
        for(int i=1; i<names.length; i++){
            sb.append("_").append(names[i]);
        }
        return sb.toString();
    }

    @Override
    public Class<?> getEndpointClass() {
        return beanClass_;
    }

//    private String computeMD5(String message){
//        StringBuilder sb = new StringBuilder(32);
//        try {
//            MessageDigest md5 = MessageDigest.getInstance("MD5");
//            md5.update(message.getBytes());
//            byte[] digest = md5.digest();
//
//            for(int i=0; i<digest.length; i++){
//                sb.append(Integer.toHexString((digest[i]>>4)&0x0F));
//                sb.append(Integer.toHexString(digest[i]&0x0F));
//            }
//            
//        } catch (NoSuchAlgorithmException ignore) {
//            ignore.printStackTrace();
//        }
//
//        return sb.toString();
//    }
}
