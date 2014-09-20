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

package org.glassfish.webservices.connector.annotation.handlers;


import javax.jws.WebService;
import javax.xml.ws.WebServiceProvider;
import javax.xml.ws.WebServiceRef;
import javax.jws.HandlerChain;

import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.AnnotatedElement;
import java.lang.annotation.Annotation;

import java.net.URL;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.io.InputStream;
import java.util.logging.Level;

import org.glassfish.apf.*;
import org.glassfish.apf.impl.HandlerProcessingResultImpl;
import com.sun.enterprise.deployment.annotation.context.ResourceContainerContextImpl;

import com.sun.enterprise.deployment.WebServiceHandlerChain;
import com.sun.enterprise.deployment.WebServiceHandler;
import com.sun.enterprise.deployment.WebServiceEndpoint;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.EjbBundleDescriptor;
import com.sun.enterprise.deployment.util.DOLUtils;
import org.glassfish.deployment.common.Descriptor;
import com.sun.enterprise.deployment.ServiceReferenceDescriptor;

import com.sun.enterprise.deployment.xml.WebServicesTagNames;
import com.sun.enterprise.deployment.types.HandlerChainContainer;
import com.sun.enterprise.deployment.annotation.context.HandlerContext;
import com.sun.enterprise.deployment.annotation.handlers.AbstractHandler;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.glassfish.webservices.connector.LogUtils;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.jvnet.hk2.annotations.Service;

/**
 * This handler takes care of the javax.jws.HandlerChain
 *
 * @author Jerome Dochez
 */
@Service
@AnnotationHandlerFor(HandlerChain.class)
public class HandlerChainHandler extends AbstractHandler {

    private static final Logger conLogger = LogUtils.getLogger();
    
    /** Creates a new instance of HandlerChainHandler */
    public HandlerChainHandler() {
    }

    /**
     * @return an array of annotation types this annotation handler would
     * require to be processed (if present) before it processes it's own
     * annotation type.
     */
    public Class<? extends Annotation>[] getTypeDependencies() {
        Class<? extends Annotation>[] dependencies = new Class[3];
        dependencies[0] = WebService.class;
        dependencies[1] = WebServiceRef.class;
        dependencies[2] = WebServiceProvider.class;
        return dependencies;
    }    
    
    public HandlerProcessingResult processAnnotation(AnnotationInfo annInfo)
        throws AnnotationProcessorException {
        
        AnnotatedElementHandler annCtx = annInfo.getProcessingContext().getHandler();
        AnnotatedElement annElem = annInfo.getAnnotatedElement();    
        Class declaringClass;
        
        boolean serviceSideChain = 
                ((annElem.getAnnotation(WebService.class) != null) ||
                 (annElem.getAnnotation(WebServiceProvider.class) != null)) ? true : false;
        if(serviceSideChain) {
            declaringClass = (Class)annElem;
        } else {
            if (annInfo.getElementType().equals(ElementType.FIELD)) {
                // this is a field injection
                declaringClass = ((Field) annElem).getDeclaringClass();
            } else if (annInfo.getElementType().equals(ElementType.METHOD)) {
                // this is a method injection
                declaringClass = ((Method) annElem).getDeclaringClass();
            } else if (annInfo.getElementType().equals(ElementType.TYPE)) {
                declaringClass = (Class) annElem;
            } else {
                throw new AnnotationProcessorException(
                        localStrings.getLocalString(
                        "enterprise.deployment.annotation.handlers.invalidtype",
                        "annotation not allowed on this element."),  annInfo);
            }
        }
        return processHandlerChainAnnotation(annInfo, annCtx, annElem, declaringClass, serviceSideChain);
    }
    
    public HandlerProcessingResult processHandlerChainAnnotation(AnnotationInfo annInfo,
            AnnotatedElementHandler annCtx, AnnotatedElement annElem, Class declaringClass, boolean serviceSideChain)
            throws AnnotationProcessorException {
        HandlerChain hChain = null;
        boolean clientSideHandlerChain = false;
        if(serviceSideChain) {
            // This is a service handler chain
            // Ignore @WebService annotation on an interface; process only those in an actual service impl class
            if (declaringClass.isInterface()) {         
                return HandlerProcessingResultImpl.getDefaultResult(getAnnotationType(), ResultType.PROCESSED);            
            }           
        
            // The @HandlerChain can be in the impl class (typically in the java-wsdl case) or
            // can be in the SEI also. Check if there is handler chain in the impl class.
            // If so, the @HandlerChain in impl class gets precedence
            hChain = annElem.getAnnotation(HandlerChain.class);
            if(hChain == null) {
                WebService webService = (WebService) declaringClass.getAnnotation(WebService.class);
                if (webService!=null) {
                    if (webService.endpointInterface()!=null && webService.endpointInterface().length()>0) {

                        Class endpointIntf;
                        try {
                            endpointIntf = declaringClass.getClassLoader().loadClass(webService.endpointInterface());
                        } catch(java.lang.ClassNotFoundException cfne) {
                            throw new AnnotationProcessorException(
                                    localStrings.getLocalString("enterprise.deployment.annotation.handlers.classnotfound",
                                        "class {0} referenced from annotation symbol cannot be loaded",
                                        new Object[] { webService.endpointInterface() }), annInfo);
                        } 
                        if (endpointIntf.getAnnotation(HandlerChain.class)!=null) {
                            hChain = (HandlerChain) endpointIntf.getAnnotation(HandlerChain.class);
                        }
                    }
                }
            }
        } else {
            // this is a client side handler chain
            hChain = annElem.getAnnotation(HandlerChain.class);
            clientSideHandlerChain = true;
        }
        
        // At this point the hChain should be the annotation to use.
        if(hChain == null) {
            return HandlerProcessingResultImpl.getDefaultResult(getAnnotationType(), ResultType.PROCESSED);            
        }
        // At this point the hChain should be the annotation to use.
        String handlerFile = hChain.file();
        
        HandlerChainContainer[] containers=null;
        if (annCtx instanceof HandlerContext) {
            containers = ((HandlerContext) annCtx).getHandlerChainContainers(serviceSideChain, declaringClass);
        }

        if (!clientSideHandlerChain && (containers==null || containers.length==0)) {
            // could not find my web service...
            throw new AnnotationProcessorException(
                    localStrings.getLocalString(
                        "enterprise.deployment.annotation.handlers.componentnotfound",
                        "component referenced from annotation symbol cannot be found"),
                    annInfo);
        }
        
        try {
            URL handlerFileURL=null;
            try {
                handlerFileURL = new URL(handlerFile);
            } catch(java.net.MalformedURLException e) {
                // swallowing purposely
            }
                
            InputStream handlerFileStream;
            if (handlerFileURL==null) {
                ClassLoader clo = annInfo.getProcessingContext().getProcessingInput().getClassLoader();
                handlerFileStream = clo.getResourceAsStream(handlerFile);
                if (handlerFileStream==null) {
                    String y = declaringClass.getPackage().getName().replaceAll("\\.","/");
                    handlerFileStream = clo.getResourceAsStream(
                            declaringClass.getPackage().getName().replaceAll("\\.","/") + "/" + handlerFile);                    
                }
                if(handlerFileStream==null) {
                    // This could be a handler file generated by jaxws customization
                    // So check in the generated SEI's package
                    if(annElem instanceof Class) {
                        String y = ((Class)annElem).getPackage().getName().replaceAll("\\.","/");
                        handlerFileStream = clo.getResourceAsStream(
                                ((Class)annElem).getPackage().getName().replaceAll("\\.","/") + "/" + handlerFile);                                            
                    }
                }
            } else {
                handlerFileStream = handlerFileURL.openConnection().getInputStream();
            }
            if (handlerFileStream==null) {
                throw new AnnotationProcessorException(
                        localStrings.getLocalString(
                            "enterprise.deployment.annotation.handlers.handlerfilenotfound",
                            "handler file {0} not found", 
                            new Object[] { handlerFile }),
                         annInfo);
            }
            Document document;
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);
                factory.setExpandEntityReferences(false);
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                
                DocumentBuilder builder = factory.newDocumentBuilder();
                document = builder.parse(handlerFileStream);
            } catch (SAXParseException spe) {
                throw new AnnotationProcessorException(
                        localStrings.getLocalString(
                            "enterprise.deployment.annotation.handlers.parserexception",
                            "{0} XML Parsing error : line  {1} ; Error = {2}",
                            new Object[] { handlerFile, spe.getLineNumber(), spe.getMessage()}));
            } finally {
                if (handlerFileStream!=null) {
                    handlerFileStream.close();
                }
            }
            for (HandlerChainContainer container : containers) {
                boolean fromDD=true;
                if (!container.hasHandlerChain()) {
                    fromDD = false;
                    processHandlerFile(document, container);
                } 
                
                // we now need to create the right context to push on the stack
                // and manually invoke the handlers annotation processing since
                // we know they are Jax-ws handlers.
                List<WebServiceHandlerChain> chains = container.getHandlerChain();
                ArrayList<Class> handlerClasses = new ArrayList<Class>();
                ClassLoader clo = annInfo.getProcessingContext().getProcessingInput().getClassLoader();
                for (WebServiceHandlerChain chain : chains) {
                    for (WebServiceHandler handler : chain.getHandlers()) {
                        String className = handler.getHandlerClass();
                        try {
                            handlerClasses.add(clo.loadClass(className));
                        } catch(ClassNotFoundException e) {
                            if (fromDD) {
                                conLogger.log(Level.WARNING, LogUtils.DDHANDLER_NOT_FOUND, className);
                            } else {
                                conLogger.log(Level.WARNING, LogUtils.HANDLER_FILE_HANDLER_NOT_FOUND,
                                    new Object[] {className, handlerFile});                                   
                            }
                        }
                    }
                }
                // we have the list of handler classes, we can now 
                // push the context and call back annotation processing.                                
                Descriptor jndiContainer=null;
                if (serviceSideChain) { 
                    WebServiceEndpoint endpoint = (WebServiceEndpoint) container; 
                    if (DOLUtils.warType().equals(endpoint.getBundleDescriptor().getModuleType())) {
                        jndiContainer = endpoint.getBundleDescriptor();                 
                    } else {
                        jndiContainer = Descriptor.class.cast(endpoint.getEjbComponentImpl());
                    }
                } else { 
                    ServiceReferenceDescriptor ref = (ServiceReferenceDescriptor) container;
                    if(DOLUtils.ejbType().equals(ref.getBundleDescriptor().getModuleType())) {
                        EjbBundleDescriptor ejbBundle = (EjbBundleDescriptor) ref.getBundleDescriptor();
                        Iterator<? extends EjbDescriptor> ejbsIter = ejbBundle.getEjbs().iterator();
                        while(ejbsIter.hasNext()) {
                            EjbDescriptor ejb = ejbsIter.next();
                            try {
                                if(ejb.getServiceReferenceByName(ref.getName()) != null) {
                                    // found the ejb; break out of the loop
                                    jndiContainer = Descriptor.class.cast(ejb);
                                    break;
                                }
                            } catch (IllegalArgumentException illex) {
                                // this ejb does not have a service-ref by this name;
                                // swallow this exception and  go to next
                            }
                        }
                    } else {
                        jndiContainer = ref.getBundleDescriptor();
                    }
                } 
                ResourceContainerContextImpl newContext = new ResourceContainerContextImpl(jndiContainer); 
                ProcessingContext ctx = annInfo.getProcessingContext(); 
                
                ctx.pushHandler(newContext);
                // process the classes
                annInfo.getProcessingContext().getProcessor().process(
                        annInfo.getProcessingContext(), 
                        handlerClasses.toArray(new Class[handlerClasses.size()]));

                ctx.popHandler();
            }
        } catch(Throwable t) {
            throw new AnnotationProcessorException(t.getMessage(), annInfo);
        }
        return HandlerProcessingResultImpl.getDefaultResult(getAnnotationType(), ResultType.PROCESSED);        
    }

    private void processHandlerFile(Document document, HandlerChainContainer ep) 
        throws SAXException {
        
        NodeList handlerChainList = document.getElementsByTagNameNS("http://java.sun.com/xml/ns/javaee",
                WebServicesTagNames.HANDLER_CHAIN);
        if(handlerChainList.getLength() != 0) {
            // this is a namespace aware handler-config file - process and return
            processHandlerChains(handlerChainList, ep);
            return;
        }
        // No Handlers found !!!! this could be namespace-unaware handler file
        handlerChainList = document.getElementsByTagName(WebServicesTagNames.HANDLER_CHAIN);
        processHandlerChains(handlerChainList, ep);
    }
    
    private String getAsQName(Node n) {
        String nodeValue = n.getTextContent();
        int index = nodeValue.indexOf(":");
        if(index <= 0) {
            return nodeValue;
        }
        String prefix = nodeValue.substring(0, index);
        String ns = n.lookupNamespaceURI(prefix);
        return("{"+ns+"}"+nodeValue.substring(index+1));
    }

    private void processHandlerChains(NodeList handlerChainList, HandlerChainContainer ep) 
        throws SAXException {
        
        for(int i=0; i<handlerChainList.getLength(); i++) {
            WebServiceHandlerChain hc = new WebServiceHandlerChain();
            Node handlerChain = handlerChainList.item(i);
            Node child = handlerChain.getFirstChild();
            while(child != null) {
                if(WebServicesTagNames.SERVICE_NAME_PATTERN.equals(child.getLocalName())) {
                    hc.setServiceNamePattern(getAsQName(child));
                }
                if(WebServicesTagNames.PORT_NAME_PATTERN.equals(child.getLocalName())) {
                    hc.setPortNamePattern(getAsQName(child));
                }
                if(WebServicesTagNames.PROTOCOL_BINDINGS.equals(child.getLocalName())) {
                    hc.setProtocolBindings(child.getTextContent());
                }
                if(WebServicesTagNames.HANDLER.equals(child.getLocalName())) {
                    processHandlers(child, hc);
                }
                child = child.getNextSibling();
            }
            ep.addHandlerChain(hc);
        }
    }

    private void processHandlers(Node handler, WebServiceHandlerChain hc) 
        throws SAXException {
        
        Node child = handler.getFirstChild();
        com.sun.enterprise.deployment.WebServiceHandler h =
                    new com.sun.enterprise.deployment.WebServiceHandler();
        while(child != null) {
            if(WebServicesTagNames.HANDLER_NAME.equals(child.getLocalName())) {
                h.setHandlerName(child.getTextContent());
            }
            if(WebServicesTagNames.HANDLER_CLASS.equals(child.getLocalName())) {
                h.setHandlerClass(child.getTextContent());
            }
            // Ignore init-params and soap-header; not applicable for JAXWS
            if(WebServicesTagNames.HANDLER_PORT_NAME.equals(child.getLocalName())) {
                h.addPortName(getAsQName(child));
            }
            if(WebServicesTagNames.SOAP_ROLE.equals(child.getLocalName())) {
                h.addSoapRole(child.getTextContent());
            }
            child = child.getNextSibling();
        }
        hc.addHandler(h);
    }        
}
