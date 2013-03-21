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

package org.glassfish.ejb.deployment.descriptor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;

import org.glassfish.ejb.deployment.EjbTagNames;

import com.sun.enterprise.deployment.EnvironmentProperty;
import com.sun.enterprise.deployment.MessageDestinationDescriptor;
import com.sun.enterprise.deployment.MessageDestinationReferenceDescriptor;
import com.sun.enterprise.deployment.MessageDestinationReferencerImpl;
import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.deployment.types.MessageDestinationReferencer;
import com.sun.enterprise.util.LocalStringManagerImpl;

    /**
    * Objects of this kind represent the deployment information describing a 
    * single message driven Ejb.
    */

public final class EjbMessageBeanDescriptor extends EjbDescriptor 
    implements MessageDestinationReferencer, com.sun.enterprise.deployment.EjbMessageBeanDescriptor {

    private static LocalStringManagerImpl localStrings =
	    new LocalStringManagerImpl(EjbMessageBeanDescriptor.class); 

    private String messageListenerType = "javax.jms.MessageListener";

    // These are the method objects from the 
    // *message-bean implementation class* that implement the
    // Message Listener interface methods or ejbTimeout method.
    private transient Collection beanClassTxMethods = null;

    // *Optional* type of destination from which message bean consumes.
    private String destinationType = null;

    // The following properties are used for processing of EJB 2.0
    // JMS-specific deployment descriptor elements.  
    private static final String DURABLE_SUBSCRIPTION_PROPERTY =
        "subscriptionDurability";
    private static final String DURABLE = 
        EjbTagNames.JMS_SUBSCRIPTION_IS_DURABLE;
    private static final String NON_DURABLE = 
        EjbTagNames.JMS_SUBSCRIPTION_NOT_DURABLE;

    private static final String ACK_MODE_PROPERTY =
        "acknowledgeMode";
    private static final String AUTO_ACK = 
        EjbTagNames.JMS_AUTO_ACK_MODE;
    private static final String DUPS_OK_ACK =
        EjbTagNames.JMS_DUPS_OK_ACK_MODE;

    private static final String MESSAGE_SELECTOR_PROPERTY =
        "messageSelector";

    private String durableSubscriptionName = null;

    private String connectionFactoryName = null;
    private String resourceAdapterMid = null;

    // Holds *optional* information about the destination to which 
    // we are linked.
    private MessageDestinationReferencerImpl msgDestReferencer;

    // activationConfig represents name/value pairs that are
    // set by the assembler of an MDB application; those properties
    // are not resource adapter vendor dependent.
    private ActivationConfigDescriptor activationConfig;
    
    // runtimeActivationConfig represents name/value pairs that are
    // set by the deployer of an MDB application; those properties
    // are resource adapter vendor dependent.
    private ActivationConfigDescriptor runtimeActivationConfig;
    
    /**
     *  Default constructor.
     */
    public EjbMessageBeanDescriptor() {
        msgDestReferencer = new MessageDestinationReferencerImpl(this);
        this.activationConfig = new ActivationConfigDescriptor();
        this.runtimeActivationConfig = new ActivationConfigDescriptor();
    }
    
    /** 
    * The copy constructor.
    */
    
    public EjbMessageBeanDescriptor(EjbMessageBeanDescriptor other) {
        super(other);
        this.messageListenerType = other.messageListenerType;
        this.beanClassTxMethods = null;
        this.durableSubscriptionName = other.durableSubscriptionName;
        this.msgDestReferencer = new MessageDestinationReferencerImpl(this);
        this.activationConfig = 
            new ActivationConfigDescriptor(other.activationConfig);
        this.runtimeActivationConfig = 
            new ActivationConfigDescriptor(other.runtimeActivationConfig);
        this.destinationType = other.destinationType;
    }    


    @Override
    public String getEjbTypeForDisplay() {
        return "MessageDrivenBean";
    }

    /**
     * Returns the type of this bean - always "Message-driven".
     */
    public String getType() {
        return TYPE;
    }

    public void setContainerTransactionFor(MethodDescriptor methodDescriptor, ContainerTransaction containerTransaction) {
        Vector allowedTxAttributes = getPossibleTransactionAttributes();
        if( allowedTxAttributes.contains(containerTransaction) ) {
            super.setContainerTransactionFor
                (methodDescriptor, containerTransaction);
         }
         else {
             throw new IllegalArgumentException(localStrings.getLocalString(
                 "enterprise.deployment.msgbeantxattrnotvalid",
		   "Invalid transaction attribute for message-driven bean"));
         }
    }

    /**
     * Sets my type
     */
    public void setType(String type) {
	throw new IllegalArgumentException(localStrings.getLocalString(
		   "enterprise.deployment.exceptioncannotsettypeofmsgdrivenbean",
		   "Cannot set the type of a message-drive bean"));
    }

    public void setMessageListenerType(String messagingType) {
        messageListenerType = messagingType;

        // Clear message listener methods so transaction methods will be 
        // recomputed using new message listener type;
        beanClassTxMethods = null;
    }

    public String getMessageListenerType() {
        return messageListenerType;
    }

    public Set getTxBusinessMethodDescriptors() {
         
        ClassLoader classLoader = getEjbBundleDescriptor().getClassLoader();
        Set methods = new HashSet();

        try {
            addAllInterfaceMethodsIn
                (methods, classLoader.loadClass(messageListenerType), 
                 MethodDescriptor.EJB_BEAN);

            addAllInterfaceMethodsIn
                (methods, classLoader.loadClass(getEjbClassName()),
                MethodDescriptor.EJB_BEAN);

            if (isTimedObject()) {
                if( getEjbTimeoutMethod() != null) {
                    methods.add(getEjbTimeoutMethod());
                }
                for (ScheduledTimerDescriptor schd : getScheduledTimerDescriptors()) {
                    methods.add(schd.getTimeoutMethod());
                }
            }

        } catch (Throwable t) { 
            _logger.log(Level.SEVERE,"enterprise.deployment.backend.methodClassLoadFailure",new Object [] {"(EjbDescriptor.getBusinessMethodDescriptors())"});
                                                           
	    throw new RuntimeException(t);
	}   
         
        return methods;
    }

    public Set getSecurityBusinessMethodDescriptors() {
	throw new IllegalArgumentException(localStrings.getLocalString(
		   "enterprise.deployment.exceptioncannotgetsecbusmethodsinmsgbean",
                   "Cannot get business method for security for message-driven bean."));
    }

    /**
     * This returns the message listener onMessage method from the
     * *message listener interface* itself, as opposed to the method
     * from the ejb class that implements it.
     */
    public Method[] getMessageListenerInterfaceMethods(ClassLoader classLoader)
        throws NoSuchMethodException {

        List<Method> methods = new ArrayList<Method>();

        try {
            Class messageListenerClass = 
                classLoader.loadClass(messageListenerType);
            for (Method method : messageListenerClass.getDeclaredMethods()) {
                methods.add(method);
            }
            final Class<?> ejbClass = classLoader.loadClass(getEjbClassName());
            for (Method method : ejbClass.getMethods()) {
                methods.add(method);
            }
        } catch(Exception e) {
            NoSuchMethodException nsme = new NoSuchMethodException();
            nsme.initCause(e);
            throw nsme;
        }
        return methods.toArray(new Method[methods.size()]);
    }

    public Vector getPossibleTransactionAttributes() {
        Vector txAttributes = new Vector();
        txAttributes.add(new ContainerTransaction
            (ContainerTransaction.REQUIRED, ""));
        txAttributes.add(new ContainerTransaction
            (ContainerTransaction.NOT_SUPPORTED, ""));
        if( isTimedObject() ) {
            txAttributes.add(new ContainerTransaction
                (ContainerTransaction.REQUIRES_NEW, ""));
        }
        return txAttributes;
    }

    public boolean hasMessageDestinationLinkName() {
        return (msgDestReferencer.getMessageDestinationLinkName() != null);
    }

    //
    // Implementations of MessageDestinationReferencer methods.
    //

    public boolean isLinkedToMessageDestination() {
        return msgDestReferencer.isLinkedToMessageDestination();
    }
    
    /** 
     * @return the name of the message destination to which I refer 
     */
    public String getMessageDestinationLinkName() {
        return msgDestReferencer.getMessageDestinationLinkName();
    }

    /** 
     * Sets the name of the message destination to which I refer.
     */
    public void setMessageDestinationLinkName(String linkName) {
        msgDestReferencer.setMessageDestinationLinkName(linkName);
    }    

    public MessageDestinationDescriptor setMessageDestinationLinkName
        (String linkName, boolean resolveLink) {
        return msgDestReferencer.setMessageDestinationLinkName
            (linkName, resolveLink);
    }

    public MessageDestinationDescriptor resolveLinkName() {
        return msgDestReferencer.resolveLinkName();
    }
        
    public boolean ownedByMessageDestinationRef() {
        return false;
    }

    public MessageDestinationReferenceDescriptor getMessageDestinationRefOwner
        () {
        return null;
    }

    /**
     * True if the owner is a message-driven bean.
     */ 
    public boolean ownedByMessageBean() {
        return true;
    }

    /**
     * Get the descriptor for the message-driven bean owner.
     */ 
    public EjbMessageBeanDescriptor getMessageBeanOwner() {
        return this;
    }
        
    /** 
     * @return the message destination to which I refer. Can be NULL.
    */
    public MessageDestinationDescriptor getMessageDestination() {
        return msgDestReferencer.getMessageDestination();
    }  

    /**
     * @param newMsgDest the message destination to which I refer.
     */
    public void setMessageDestination(MessageDestinationDescriptor newMsgDest) {
        msgDestReferencer.setMessageDestination(newMsgDest);
    }

    //
    // ActivationConfig
    //

    /**
     * @return Set of EnvironmentProperty elements.  
     */
    public Set<EnvironmentProperty> getActivationConfigProperties() {
        return activationConfig.getActivationConfig();
    }
    
    public String getActivationConfigValue(String name) {
        for (EnvironmentProperty next : activationConfig.getActivationConfig()) {
            if (next.getName().equals(name)) {
                return next.getValue();
            }
        }
        return null;
    }

    public void putActivationConfigProperty(EnvironmentProperty prop) {
    	// remove first an existing property with the same name
    	removeActivationConfigPropertyByName(prop.getName());
        activationConfig.getActivationConfig().add(prop);
    }

    public void removeActivationConfigProperty(EnvironmentProperty prop) {
        for(Iterator<EnvironmentProperty> iter = activationConfig.getActivationConfig().iterator();
            iter.hasNext();) {
            EnvironmentProperty next = iter.next();
            if( next.getName().equals(prop.getName()) && 
                next.getValue().equals(prop.getValue()) ) {
                iter.remove();
                break;
            }
        }
    }

    public void removeActivationConfigPropertyByName(String name) {
        for(Iterator<EnvironmentProperty> iter = activationConfig.getActivationConfig().iterator();
            iter.hasNext();) {
            EnvironmentProperty next = iter.next();
            if( next.getName().equals(name) ) {
                iter.remove();
                break;
            }
        }
    }

    /**
     * @return Set of EnvironmentProperty elements.
     */
    public Set<EnvironmentProperty> getRuntimeActivationConfigProperties() {
        return runtimeActivationConfig.getActivationConfig();
    }
    
    public String getRuntimeActivationConfigValue(String name) {
        for (EnvironmentProperty next : runtimeActivationConfig.getActivationConfig()) {
            if (next.getName().equals(name)) {
                return next.getValue();
            }
        }
        return null;
    }

    public void putRuntimeActivationConfigProperty(EnvironmentProperty prop) {
        runtimeActivationConfig.getActivationConfig().add(prop);
    }

    public void removeRuntimeActivationConfigProperty(EnvironmentProperty prop) {        
        for(Iterator<EnvironmentProperty> iter = runtimeActivationConfig.getActivationConfig().iterator(); iter.hasNext();) {
            EnvironmentProperty next = iter.next();
            if( next.getName().equals(prop.getName()) && 
                next.getValue().equals(prop.getValue()) ) {
                iter.remove();
                break;
            }
        }

    }

    public void removeRuntimeActivationConfigPropertyByName(String name) {
        for(Iterator<EnvironmentProperty> iter = runtimeActivationConfig.getActivationConfig().iterator();
            iter.hasNext();) {
            EnvironmentProperty next = iter.next();
            if( next.getName().equals(name) ) {
                iter.remove();
                break;
            }
        }
    }

    public boolean hasQueueDest() {
        return ( (destinationType != null) && 
                 (destinationType.equals("javax.jms.Queue")) );
    }

    public boolean hasTopicDest() {
        return ( (destinationType != null) && 
                 (destinationType.equals("javax.jms.Topic")) );
    }

    public boolean hasDestinationType() {
        return (destinationType != null);
    }

    public String getDestinationType() {
        return destinationType;
    }
    
    public void setDestinationType(String destType) {
        destinationType = destType;
    }

    public boolean hasDurableSubscription() {
        String value = getActivationConfigValue(DURABLE_SUBSCRIPTION_PROPERTY);
        return ( (value != null) && value.equals(DURABLE) );
    }

    public void setHasDurableSubscription(boolean durable) {
        if( durable ) {
            EnvironmentProperty durableProp = 
                new EnvironmentProperty(DURABLE_SUBSCRIPTION_PROPERTY, 
                                        DURABLE, "");
            putActivationConfigProperty(durableProp);
        } else {
            removeActivationConfigPropertyByName(DURABLE_SUBSCRIPTION_PROPERTY);
        }

    }


    public void setHasQueueDest() {
        destinationType = "javax.jms.Queue";
        setHasDurableSubscription(false);
    }

    public void setHasTopicDest() {
        destinationType = "javax.jms.Topic";

    }
    
    public void setSubscriptionDurability(String subscription) {
        if (subscription.equals(DURABLE)) {
            setHasDurableSubscription(true);
        }
        else if (subscription.equals(NON_DURABLE)) {
            setHasDurableSubscription(false);
        } else {
            throw new IllegalArgumentException
                ("Invalid subscription durability string : " + subscription);
        }
    }

    public boolean hasJmsMessageSelector() {
        return ( getActivationConfigValue(MESSAGE_SELECTOR_PROPERTY) != null );
    }

    public void setJmsMessageSelector(String selector) {
        if( (selector == null) || (selector.trim().equals("")) ) {
            removeActivationConfigPropertyByName(MESSAGE_SELECTOR_PROPERTY);
        } else {
            EnvironmentProperty msgSelectorProp =
                new EnvironmentProperty(MESSAGE_SELECTOR_PROPERTY,selector,"");
            putActivationConfigProperty(msgSelectorProp);
        }


    }

    public String getJmsMessageSelector() {
	return getActivationConfigValue(MESSAGE_SELECTOR_PROPERTY);
    }

    static final int AUTO_ACKNOWLEDGE = 1;
    static final int DUPS_OK_ACKNOWLEDGE = 3;


    public int getJmsAcknowledgeMode() {
        String ackModeStr = getActivationConfigValue(ACK_MODE_PROPERTY);
        return ( (ackModeStr != null) && ackModeStr.equals(DUPS_OK_ACK) ) ? 
            DUPS_OK_ACKNOWLEDGE : AUTO_ACKNOWLEDGE;
    }
    
    public String getJmsAcknowledgeModeAsString() {
        return getActivationConfigValue(ACK_MODE_PROPERTY);
    }
    
    public void setJmsAcknowledgeMode(int acknowledgeMode) {
        String ackModeValue = (acknowledgeMode == AUTO_ACKNOWLEDGE) ?
            AUTO_ACK : DUPS_OK_ACK;
        EnvironmentProperty ackModeProp =
            new EnvironmentProperty(ACK_MODE_PROPERTY, ackModeValue, "");
        putActivationConfigProperty(ackModeProp);


    }
    
    public void setJmsAcknowledgeMode(String acknowledgeMode) {
        if (AUTO_ACK.equals(acknowledgeMode)) {
            setJmsAcknowledgeMode(AUTO_ACKNOWLEDGE);
        } else {
            if (DUPS_OK_ACK.equals(acknowledgeMode)) {
                setJmsAcknowledgeMode(DUPS_OK_ACKNOWLEDGE);
            } else {
                throw new IllegalArgumentException
                    ("Invalid jms acknowledge mode : " + acknowledgeMode);
            }
        }
    }    

    public String getDurableSubscriptionName() {
        return durableSubscriptionName;
    }

    public void setDurableSubscriptionName(String durableSubscriptionName) {
        this.durableSubscriptionName = durableSubscriptionName;

    }

    public String getConnectionFactoryName() {
        return connectionFactoryName;
    }

    /**
     * Connection factory is optional.  If set to null, 
     * hasConnectionFactory will return false.
     */
    public void setConnectionFactoryName(String connectionFactory) {
        connectionFactoryName = connectionFactory;
    }

    public boolean hasConnectionFactory() {
        return (connectionFactoryName != null);
    }

    public String getResourceAdapterMid() {
        return resourceAdapterMid;
    }

    public String getMdbConnectionFactoryJndiName() {
        return getIASEjbExtraDescriptors().getMdbConnectionFactory().getJndiName();
    }

    /**
     * resource-adapter-mid is optional.  It is set when
     * a resource adapter is responsible for delivering
     * messages to the message-driven bean.  If not set,
     * hasResourceAdapterMid will return false.
     */
    public void setResourceAdapterMid(String resourceAdapterMid) {
        this.resourceAdapterMid = resourceAdapterMid;
    }

    public boolean hasResourceAdapterMid() {
        return (resourceAdapterMid != null);
    }

    /** 
     */
    public Vector getMethods(ClassLoader classLoader) {
        // @@@
        return new Vector();
    }

    /** 
     * @return a collection of MethodDescriptor for methods which 
     * may have a assigned security attribute.
     */
    protected Collection getTransactionMethods(ClassLoader classLoader) {
        Vector txMethods = new Vector();

        if( beanClassTxMethods == null ) {
            try {
                beanClassTxMethods = new HashSet();
                Class ejbClass = classLoader.loadClass(this.getEjbClassName());
                Method interfaceMessageListenerMethods[] =
                    getMessageListenerInterfaceMethods(classLoader);
                for(int i = 0; i < interfaceMessageListenerMethods.length; 
                    i++) {
                    Method next = interfaceMessageListenerMethods[i];
                    // Convert method objects from MessageListener interface
                    // to method objects from ejb class
                    Method nextBeanMethod = ejbClass.getMethod
                        (next.getName(), next.getParameterTypes());
                    beanClassTxMethods.add(new MethodDescriptor(nextBeanMethod, MethodDescriptor.EJB_BEAN));
                }
                if( isTimedObject() ) {
                    beanClassTxMethods.add(getEjbTimeoutMethod()); 
                }
            }
            catch(Exception e) {
                NoSuchMethodError nsme = new NoSuchMethodError(localStrings.getLocalString("enterprise.deployment.noonmessagemethod", "", new Object[] { 
                    getEjbClassName(), getMessageListenerType() }));
                nsme.initCause(e);
                throw nsme;
            }
        }
        txMethods.addAll(beanClassTxMethods);

        return txMethods;
    }

        @Override
        public String getContainerFactoryQualifier() {
            return "MessageBeanContainerFactory";
        }

        /**
     *  Sets the transaction type for this bean. 
     * Must be either BEAN_TRANSACTION_TYPE or CONTAINER_TRANSACTION_TYPE.
     */
    public void setTransactionType(String transactionType) {
	boolean isValidType = (BEAN_TRANSACTION_TYPE.equals(transactionType) ||
				CONTAINER_TRANSACTION_TYPE.equals(transactionType));
				
	if (!isValidType && this.isBoundsChecking()) {
	    throw new IllegalArgumentException(localStrings.getLocalString(
									   "enterprise.deployment.exceptionmsgbeantxtypenotlegaltype",
									   "{0} is not a legal transaction type for a message-driven bean", new Object[] {transactionType}));
	} else {
	    super.transactionType = transactionType;
	    super.setMethodContainerTransactions(new Hashtable());

	}
    }

    public void setActivationConfigDescriptor(ActivationConfigDescriptor desc) {
        activationConfig = desc;
    }

    // NOTE : This method should only be used by the XML processing logic.  
    // All access to activation config properties should be done 
    // through the other accessors on the message bean descriptor.
    public ActivationConfigDescriptor getActivationConfigDescriptor() {
        return activationConfig;
    }

    public void setRuntimeActivationConfigDescriptor(ActivationConfigDescriptor
                                                     desc) {
        runtimeActivationConfig = desc;
        
    }

    // NOTE : This method should only be used by the XML processing logic.  
    // All access to activation config properties should be done 
    // through the other accessors on the message bean descriptor.
    public ActivationConfigDescriptor getRuntimeActivationConfigDescriptor() {
        return runtimeActivationConfig;
    }

    /**
    * Returns a formatted String of the attributes of this object.
    */
    public void print(StringBuffer toStringBuffer) {
	super.print(toStringBuffer);
	toStringBuffer.append("Message-driven descriptor : ").append( 
            activationConfig.getActivationConfig()).append(
            runtimeActivationConfig.getActivationConfig());
    }
}
