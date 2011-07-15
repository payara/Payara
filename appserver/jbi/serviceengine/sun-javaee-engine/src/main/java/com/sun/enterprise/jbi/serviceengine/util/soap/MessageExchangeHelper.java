/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

/*
 * MessageExchangeHelper.java
 *
 * Created on February 5, 2006, 9:51 AM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package com.sun.enterprise.jbi.serviceengine.util.soap;

import org.w3c.dom.*;

import com.sun.enterprise.deployment.ServiceReferenceDescriptor;
import com.sun.enterprise.deployment.ServiceRefPortInfo;
import com.sun.enterprise.jbi.serviceengine.ServiceEngineException;
import com.sun.enterprise.jbi.serviceengine.util.JBIConstants;
import com.sun.enterprise.jbi.serviceengine.handlers.JBIHandler;
import com.sun.enterprise.jbi.serviceengine.handlers.JBIHandlerFactory;
import com.sun.enterprise.jbi.serviceengine.comm.MessageSender;
import com.sun.enterprise.jbi.serviceengine.comm.DefaultMessageExchangeTransport;
import com.sun.enterprise.jbi.serviceengine.comm.MessageExchangeTransport;
import com.sun.enterprise.jbi.serviceengine.core.JavaEEServiceEngineContext;
import com.sun.enterprise.jbi.serviceengine.core.ServiceEngineEndpoint;
import com.sun.enterprise.jbi.serviceengine.core.EndpointRegistry;
//import com.sun.jbi.wsdl11wrapper.*;
import com.sun.logging.LogDomains;
import javax.jbi.messaging.*;
import javax.jbi.messaging.Fault;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.wsdl.*;
import javax.wsdl.factory.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.net.URL;

/**
 * This class helps in normalizing a SOAP message and denormalizing a normalized
 * message into a SOAP Message
 * @author mu125243
 */
public class MessageExchangeHelper {
    
    private static Logger logger =
            LogDomains.getLogger(MessageExchangeHelper.class, LogDomains.SERVER_LOGGER);
    
    private MessageExchange messageExchange;
    private URL wsdlLocation;
    private QName serviceName;
    private String endpointName;
    private List<JBIHandler> handlers; 

    // For storing the WSDL cache.
    private static ConcurrentHashMap<String, EndpointMetaData> wsdlCache =
                    new ConcurrentHashMap<String, EndpointMetaData>(11,0.75f,4);

    private static String WSDL11 = "http://schemas.xmlsoap.org/wsdl/";

    /** Creates a new instance of MessageExchangeHelper */
    public MessageExchangeHelper() {
        handlers = JBIHandlerFactory.getInstance().getHandlers();
    }
    
    public void setMessageExchange(MessageExchange messageExchange) {
        this.messageExchange = messageExchange;
    }
    
    
    public SOAPMessage denormalizeMessage(boolean inFlag) throws ServiceEngineException {
        validateMessageExchange();
        NormalizedMessage normalizedMsg = null;
        
        if(inFlag) {
            if(isInOutMessageExchange()) {
                InOut inOutExchange = (InOut)messageExchange;
                normalizedMsg = inOutExchange.getInMessage();
                
            } else {
                InOnly inOnlyExchange = (InOnly)messageExchange;
                normalizedMsg = inOnlyExchange.getInMessage();
            }
        } else {
            // assumed that it's a inout message
            normalizedMsg = (messageExchange.getFault()!=null)? 
                    messageExchange.getFault() : ((InOut)messageExchange).getOutMessage();
        //create inonly or inout message exchange based on the instance
        }
        
        DefaultMessageExchangeTransport meTransport = 
                new DefaultMessageExchangeTransport(messageExchange);
        meTransport.setMessage(normalizedMsg);
        invokeHandlersForOutbound(meTransport);

        QName operationQName = messageExchange.getOperation();
        String pattern = messageExchange.getPattern().toString();
        
        Operation operation = new Operation(operationQName.getLocalPart(), pattern);
        // DeNormalize response msg to SOAP msg
        
        MessageDenormalizerImpl d = new MessageDenormalizerImpl();
        SOAPWrapper wrapper;
        if(messageExchange.getFault()!=null) {
            // Assuming soap binding does not wrap a fault message
            wrapper = d.denormalizeFaultMessage((Fault)normalizedMsg);
        } else {
            unWrapMessage(normalizedMsg, inFlag);
            wrapper = d.denormalizeMessage(normalizedMsg, operation, !inFlag);
        }
        SOAPMessage message = wrapper.getMessage();
        printSOAPMessage( "Denormalizing in ? "+ inFlag + "message :" , message) ;
        return message;
        
    }
    
    public boolean isInOutMessageExchange() {
        return messageExchange instanceof InOut;
    }
    
    public void normalizeMessage(SOAPMessage soapMessage, boolean inFlag)  throws ServiceEngineException  {
        validateMessageExchange();
        if(soapMessage != null) {
            
            
            printSOAPMessage( "normalizing in ? "+ inFlag + "message :" , soapMessage) ;
            NormalizedMessage normalizedMsg = null;
            try {
                
                boolean isFault = (soapMessage.getSOAPBody().getFault() != null);
                normalizedMsg = 
                        (isFault)? messageExchange.createFault() : messageExchange.createMessage();
                
                //soapMessage.writeTo(System.out);
                if(isFault) {
                    // Assuming soap binding does not unwrap a fault message
                    SOAPWrapper wrapper = new SOAPWrapper(soapMessage);
                    MessageNormalizerImpl normalizer = new MessageNormalizerImpl();
                    normalizer.normalizeFaultMessage(wrapper, normalizedMsg);
                } else {
                    //normalizer.normalizeMessage(wrapper, normalizedMsg, operation);
                    normalizeAndWrapMessage(normalizedMsg, soapMessage, !inFlag);
                }
                
                if(isFault)
                    messageExchange.setFault((javax.jbi.messaging.Fault)normalizedMsg);
                else if(inFlag) {
                    if(isInOutMessageExchange()) {
                        ((InOut)messageExchange).setInMessage(normalizedMsg);
                    } else {
                        ((InOnly)messageExchange).setInMessage(normalizedMsg);
                    }
                } else // inout assumed.
                    ((InOut)messageExchange).setOutMessage(normalizedMsg);
                DefaultMessageExchangeTransport meTransport = 
                        new DefaultMessageExchangeTransport(messageExchange);
                meTransport.setMessage(normalizedMsg);
                invokeHandlersForInbound(meTransport);
            } catch(Exception e) {
                e.printStackTrace();
            }
            
        }
        
    }
    
    private void unWrapMessage(NormalizedMessage normalizedMsg, boolean server) {
        /*
        try {
            String endpointName = null;
            QName serviceName = null;
            EndpointMetaData emd = null;

            ServiceEndpoint serviceEndpoint = messageExchange.getEndpoint();
            if(serviceEndpoint != null) {
                EndpointRegistry endpointRegistry = EndpointRegistry.getInstance();
                endpointName = serviceEndpoint.getEndpointName();
                serviceName = serviceEndpoint.getServiceName();
                ServiceEngineEndpoint serviceEngineEndpoint = endpointRegistry.get(
                        serviceName, endpointName);
                if(serviceEngineEndpoint != null && server)
                    emd = serviceEngineEndpoint.getEndpointMetaData();
            }
            if(!server && wsdlLocation != null) {
                String wsdl = wsdlLocation.toURI().toString();
                serviceName = this.serviceName;
                endpointName = this.endpointName;
                emd  = getEndPointMetaData(wsdl, serviceName, endpointName);
            }

            // can emd be null?
            Definition mDefinition = emd.getDefinition();
            if(isWsdl11(mDefinition)) {
                Wsdl11WrapperHelper helper = new Wsdl11WrapperHelper(mDefinition);
                Source source = normalizedMsg.getContent();
                String operationName = messageExchange.getOperation().getLocalPart();
                boolean isProvider = messageExchange.getRole().equals(MessageExchange.Role.PROVIDER);
                Document unwrappedDoc = helper.unwrapMessage(source, 
                                                            serviceName,
                                                            endpointName,
                                                            operationName,
                                                            isProvider);
                normalizedMsg.setContent(new DOMSource(unwrappedDoc));
            }
        }catch(Exception e) {
            e.printStackTrace();
        }

        */
    }
    
    private void normalizeAndWrapMessage(NormalizedMessage normalizedMsg, 
                                         SOAPMessage soapMessage,
                                         boolean server) {
        /*
        try {
            String endpointName = null;
            QName serviceName = null;
            EndpointMetaData emd = null;
            ServiceEndpoint serviceEndpoint = messageExchange.getEndpoint();
            if(serviceEndpoint != null) {
                EndpointRegistry endpointRegistry = EndpointRegistry.getInstance();
                endpointName = serviceEndpoint.getEndpointName();
                serviceName = serviceEndpoint.getServiceName();
                ServiceEngineEndpoint serviceEngineEndpoint = endpointRegistry.get(
                serviceName, endpointName);
                if(serviceEngineEndpoint != null && server)
                    emd = serviceEngineEndpoint.getEndpointMetaData();
            }
            if(!server && wsdlLocation != null) {
                String wsdl = wsdlLocation.toURI().toString();
                serviceName = this.serviceName;
                endpointName = this.endpointName;
                emd  = getEndPointMetaData(wsdl, serviceName, endpointName);
            }

            Definition mDefinition = emd.getDefinition();
            Wsdl11WrapperHelper helper = new Wsdl11WrapperHelper(mDefinition);
            if(isWsdl11(mDefinition)) {
                //String operationName = messageExchange.getOperation().getLocalPart();
                String operationName = null;
                Operation operation = null;
                if (messageExchange.getOperation() == null) {
                    operationName = emd.getOperationName(soapMessage);
		    if (logger.isLoggable(Level.FINEST))
                        logger.finest("Operation Name is :" + operationName);
                    QName opQName = new QName(
                                    messageExchange.getService().getNamespaceURI(), 
                                    operationName);
                    messageExchange.setOperation(opQName);
                    operation = getOperation(soapMessage, 
                                    serviceName, operationName);
                } else {
                    operationName = messageExchange.getOperation().getLocalPart();
                    operation = new Operation(operationName,
                                    messageExchange.getPattern().toString());
                }

                SOAPWrapper wrapper = new SOAPWrapper(soapMessage);
                MessageNormalizerImpl normalizer = new MessageNormalizerImpl();
                normalizer.normalizeMessage(wrapper, normalizedMsg, operation);
                Document wrappedDoc = null;
                Source source = normalizedMsg.getContent();
                boolean isProvider = messageExchange.getRole().equals(MessageExchange.Role.PROVIDER);
                wrappedDoc = helper.wrapMessage(source, serviceName, endpointName, operationName,!isProvider);

                normalizedMsg.setContent(new DOMSource(wrappedDoc));
             }
        }catch(Exception e) {
            e.printStackTrace();
        }        
        */
    }
    
    private Definition getWsdlDefinition(String wsdl) throws Exception{
        javax.wsdl.factory.WSDLFactory mFactory = WSDLFactory.newInstance();
        javax.wsdl.xml.WSDLReader mReader = mFactory.newWSDLReader();
        return mReader.readWSDL(wsdl);
    }
/*
    private boolean isWsdl11(Definition mDefinition) 
    throws Wsdl11WrapperHelperException {
       try
       {
           if (mDefinition != null) {
               String xmlns = mDefinition.getNamespace ("");
               if (xmlns.trim ().equals(WSDL11)) {
                   return true;
               }
           }
       }
       catch (Exception e)
       {
           throw new Wsdl11WrapperHelperException("Cannot get version", e);
       }
       return false;
    } 
*/
    public MessageExchange getMessageExchange() {
        return messageExchange;
    }

    /** This will only be called during JAX-RPC invocation */
    public void initializeMessageExchange(ServiceRefPortInfo portInfo, boolean oneWay)
    throws ServiceEngineException {
        ServiceReferenceDescriptor serviceRef = portInfo.getServiceReference();
        QName serviceName = serviceRef.getServiceName();
        String endpointName = portInfo.hasWsdlPort()? 
                                        portInfo.getWsdlPort().getLocalPart() : 
                                        portInfo.getName();
        URL wsdlFileUrl = serviceRef.getWsdlFileUrl();
        initializeMessageExchange(wsdlFileUrl, serviceName, endpointName, oneWay);
    }
    
    public void initializeMessageExchange(URL wsdlLocation, 
                                          QName service,
                                          String endpointName,
                                          boolean oneWay)
    throws ServiceEngineException {
        try {
            this.wsdlLocation = wsdlLocation;
            this.serviceName = service;
            this.endpointName = endpointName;
            DeliveryChannel channel =
                    JavaEEServiceEngineContext.getInstance(). getDeliveryChannel();
            // Create MessageExchange
            MessageExchangeFactory factory =
                    channel.createExchangeFactoryForService(serviceName);
            
            MessageExchange msgExchange = null;
            NormalizedMessage inMsg = null;
            
            if(oneWay) {
                InOnly inMessageExchange =  factory.createInOnlyExchange();
                inMsg = inMessageExchange.createMessage();
                inMessageExchange.setInMessage(inMsg);
                msgExchange = inMessageExchange;
            } else {
                InOut inOutMessageExchange =  factory.createInOutExchange();
                inMsg = inOutMessageExchange.createMessage();
                inOutMessageExchange.setInMessage(inMsg);
                msgExchange = inOutMessageExchange;
            }
            msgExchange.setService(serviceName);
            setMessageExchange(msgExchange);
        } catch(Exception e) {
            throw new ServiceEngineException(e);
        }
    }
    
    public void handleException(Exception exception) {
        try { 
            messageExchange.setStatus(ExchangeStatus.ERROR);
            if((messageExchange instanceof  InOut) || 
               (messageExchange instanceof RobustInOnly)){
                normalizeException(exception);
            }
            dispatchMessage();
        } catch(Exception e) {
           logger.log(Level.SEVERE, e.getMessage());
        }
    }
    
    public void handleResponse(SOAPMessage response, boolean flag) {
        try {
            if(messageExchange instanceof  InOut)  {
                normalizeMessage(response, flag);
            } else if((messageExchange instanceof InOnly ) ||
                (messageExchange instanceof RobustInOnly)) {
                messageExchange.setStatus(ExchangeStatus.DONE);
            }
            dispatchMessage();
        } catch(Exception e) {
           logger.log(Level.SEVERE, e.getMessage());
        }
    }
    
    public void normalizeException(Exception exception) {
        if(exception != null) {
            try {
                MessageDenormalizerImpl d = new MessageDenormalizerImpl();
                SOAPWrapper soapWrapper = d.denormalizeMessage(exception);
                normalizeMessage(soapWrapper.getMessage(), false);
                
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    public void dispatchMessage() throws ServiceEngineException {
        if(messageExchange != null) {
            MessageSender messageSender = new MessageSender();
            messageSender.setMessageExchange(messageExchange);
            messageSender.send();
            Exception exception = messageSender.getException();
            if(exception != null)
                throw new ServiceEngineException(exception);
        }
    }
    
    private void validateMessageExchange() throws ServiceEngineException {
        if(messageExchange == null)
            throw new ServiceEngineException("MessageExchange not set, use setMessageExchange()");
    }
    
    
    private Operation getOperation(SOAPMessage soapMessage, QName svcQName, 
        String opName) throws ServiceEngineException {
        try {
            // Get operation name from the localpart of the first child
            // of <env:Body> in SOAP msg
            SOAPPart sp = soapMessage.getSOAPPart();
            SOAPEnvelope env = sp.getEnvelope();
            SOAPBody body = env.getBody();
             // first child of body is like <ns0:sayHello>
            org.w3c.dom.Node firstChild = body.getFirstChild();
            String namespacePrefix = firstChild.getPrefix();
            
            // Get WSDL operation QName. This is in the same namespace as the
            // service, declared in the <definitions> element in the WSDL file.
            String svcNamespace = svcQName.getNamespaceURI();
            
            // namespace URI for body content is the WSDL "types" namespace
            // as in the <schema> element in the WSDL file.
            String namespaceURI = null;
            if(namespacePrefix != null)
                namespaceURI = env.getNamespaceURI(namespacePrefix);
            else
                namespaceURI = svcNamespace;
            
            // Normalize Message
            Operation operation = new Operation(opName, "in-out");
            // TODO Does JAXRPC2.0 allow WSDL 2.0's uri or multipart styles ?
            operation.setStyle("rpc");
            operation.setInputNamespace(namespaceURI);
            operation.setOutputNamespace(namespaceURI);
            return operation;
            
        } catch(Exception e) {
            e.printStackTrace();
            
            throw new ServiceEngineException(e.getMessage());
        }
    }
    
    private void invokeHandlersForInbound(MessageExchangeTransport meTransport) 
            throws ServiceEngineException {
        for (JBIHandler handler : handlers)
            try {
                handler.handleInbound(meTransport);
            } catch (Exception e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
                throw new ServiceEngineException(e);
            }
    }
    
    private void invokeHandlersForOutbound(MessageExchangeTransport meTransport) 
            throws ServiceEngineException {
        for (JBIHandler handler : handlers)
            try {
                handler.handleOutbound(meTransport);
            } catch (Exception e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
                throw new ServiceEngineException(e);
            }
    }

    /**This is the client side WSDL cache. The Provider cache is stored in 
     * ServiceEngineEndpoint. Client caching can be disabled using a System
     * property.*/
    private EndpointMetaData getEndPointMetaData(String wsdlPath, 
                                                 QName serviceName,
                                                 String epName) 
            throws Exception {
        String clientCache = System.getProperty(JBIConstants.CLIENT_CACHE);
        if("false".equalsIgnoreCase(clientCache)) {
            return createEndpointMetaData(wsdlPath, serviceName, epName);
        } else {
            EndpointMetaData emd = wsdlCache.get(wsdlPath);
            if(emd == null) {
                emd = createEndpointMetaData(wsdlPath, serviceName, epName);
                wsdlCache.put(wsdlPath, emd);
            }
            return emd;
        }
    }
    
    private EndpointMetaData createEndpointMetaData(String wsdlPath,
                                                    QName serviceName,
                                                    String epName)
            throws Exception {
        EndpointMetaData emd = new EndpointMetaData(getWsdlDefinition(wsdlPath), 
                                                    serviceName, 
                                                    epName);
        emd.resolve();
        return emd;
    }
    
    protected void printSOAPMessage(String message, SOAPMessage soapMessage) {
        try {
            if(logger.isLoggable(Level.FINE)) {
                System.out.print(message);
                soapMessage.writeTo(System.out);
            }
        }catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }
    
    protected void printSOAPContent(String message, NormalizedMessage normalizedMessage) {
        if(logger.isLoggable(Level.FINE)) {
            if(normalizedMessage != null) {
                javax.xml.transform.Source source = normalizedMessage.getContent() ;
                if(source != null) {
                    try {
                        javax.xml.transform.Transformer transformer = javax.xml.transform.TransformerFactory.newInstance().newTransformer();
                        System.out.print(message);
                        javax.xml.transform.stream.StreamResult result = new javax.xml.transform.stream.StreamResult(System.out);
                        transformer.transform(source, result);
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
                
            }
        }
    }
    
}
