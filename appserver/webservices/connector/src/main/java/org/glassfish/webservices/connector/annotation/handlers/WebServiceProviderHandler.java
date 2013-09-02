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


import java.lang.reflect.AnnotatedElement;
import java.lang.annotation.Annotation;
import java.util.logging.Logger;

import org.glassfish.apf.*;

import org.glassfish.apf.impl.HandlerProcessingResultImpl;

import com.sun.enterprise.deployment.annotation.context.WebBundleContext;
import com.sun.enterprise.deployment.annotation.context.EjbContext;
import com.sun.enterprise.deployment.annotation.context.WebComponentContext;
import com.sun.enterprise.deployment.annotation.handlers.AbstractHandler;

import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.EjbBundleDescriptor;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.WebServicesDescriptor;
import com.sun.enterprise.deployment.WebService;
import com.sun.enterprise.deployment.WebServiceEndpoint;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.WebComponentDescriptor;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.util.LocalStringManagerImpl;
import java.util.logging.Level;

import javax.xml.namespace.QName;

import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.web.deployment.descriptor.WebComponentDescriptorImpl;
import org.glassfish.webservices.connector.LogUtils;
import org.glassfish.webservices.node.WebServicesDescriptorNode;
import org.jvnet.hk2.annotations.Service;

/**
 * This annotation handler is responsible for processing the javax.jws.WebService 
 * annotation type.
 *
 * @author Jerome Dochez
 */
@Service
@AnnotationHandlerFor(javax.xml.ws.WebServiceProvider.class)
public class WebServiceProviderHandler extends AbstractHandler {
    
    private static final Logger conLogger = LogUtils.getLogger();

    private static final LocalStringManagerImpl wsLocalStrings = new LocalStringManagerImpl(WebServiceProviderHandler.class);
    
    /** Creates a new instance of WebServiceHandler */
    public WebServiceProviderHandler() {
    }
        
    /**
     * @return an array of annotation types this annotation handler would
     * require to be processed (if present) before it processes it's own
     * annotation type.
     */
    public Class<? extends Annotation>[] getTypeDependencies() {
        /*Class dependencies[] = { javax.ejb.Stateless.class };
        return dependencies;*/
        return getEjbAndWebAnnotationTypes();
    }
    
    public HandlerProcessingResult processAnnotation(AnnotationInfo annInfo) 
        throws AnnotationProcessorException     
    {
        AnnotatedElementHandler annCtx = annInfo.getProcessingContext().getHandler();
        AnnotatedElement annElem = annInfo.getAnnotatedElement();
        
        boolean ejbInWar = ignoreWebserviceAnnotations(annElem, annCtx);
        //Bug  http://monaco.sfbay/detail.jsf?cr=6956406
        //When there is an ejb webservice packaged in a war
        //ignore the annotation processing for WebBundleDescriptor
        //In Ejb webservice in a war there are 2 bundle descriptors
        //so we should just allow the processing for the EjbBundleDescriptor
        //and add webservices to that BundleDescriptor
        if (ejbInWar) {
            return HandlerProcessingResultImpl.getDefaultResult(getAnnotationType(), ResultType.PROCESSED);
        }

        // sanity check
        if (!(annElem instanceof Class)) {
            AnnotationProcessorException ape = new AnnotationProcessorException(
                    "@WebServiceProvider can only be specified on TYPE", annInfo);
            annInfo.getProcessingContext().getErrorHandler().error(ape);
            return HandlerProcessingResultImpl.getDefaultResult(getAnnotationType(), ResultType.FAILED);                        
        }             

        if(isJaxwsRIDeployment(annInfo)) {
            // Looks like JAX-WS RI specific deployment, do not process Web Service annotations otherwise would end up as two web service endpoints
            conLogger.log(Level.INFO, LogUtils.DEPLOYMENT_DISABLED,
                    new Object[] {annInfo.getProcessingContext().getArchive().getName(), "WEB-INF/sun-jaxws.xml"});
            return HandlerProcessingResultImpl.getDefaultResult(getAnnotationType(), ResultType.PROCESSED);
        }
        
        // WebServiceProvider MUST implement the provider interface, let's check this
        if (!javax.xml.ws.Provider.class.isAssignableFrom((Class) annElem)) {
            AnnotationProcessorException ape = new AnnotationProcessorException(
                    annElem.toString() + "does not implement the javax.xml.ws.Provider interface", annInfo);
            annInfo.getProcessingContext().getErrorHandler().error(ape);
            return HandlerProcessingResultImpl.getDefaultResult(getAnnotationType(), ResultType.FAILED);                                    
        }
    
        // let's get the main annotation of interest. 
        javax.xml.ws.WebServiceProvider ann = (javax.xml.ws.WebServiceProvider) annInfo.getAnnotation();        
        
        BundleDescriptor bundleDesc = null;
        
        try {
            // let's see the type of web service we are dealing with...
            if ((ejbProvider != null) && ejbProvider.getType("javax.ejb.Stateless") != null && (annCtx instanceof EjbContext)) {
                // this is an ejb !
                EjbContext ctx = (EjbContext) annCtx;
                bundleDesc = ctx.getDescriptor().getEjbBundleDescriptor();
                bundleDesc.setSpecVersion("3.0");
            } else {
                if (annCtx instanceof WebComponentContext) {
                    bundleDesc = ((WebComponentContext) annCtx).getDescriptor().getWebBundleDescriptor();
                } else if (!(annCtx instanceof WebBundleContext)) {
                    return getInvalidAnnotatedElementHandlerResult(
                            annInfo.getProcessingContext().getHandler(), annInfo);
                }
                bundleDesc = ((WebBundleContext) annCtx).getDescriptor();
                bundleDesc.setSpecVersion("2.5");
            }
        } catch (Exception e) {
            throw new AnnotationProcessorException(
                    wsLocalStrings.getLocalString("webservice.annotation.exception",
                    "WS00023: Exception in processing @Webservice : {0}",
                    e.getMessage()));
        }

        // For WSProvider, portComponentName is the fully qualified class name
        String portComponentName = ((Class) annElem).getName();
        
        // As per JSR181, the serviceName is either specified in the deployment descriptor
        // or in @WebSErvice annotation in impl class; if neither service name implclass+Service
        String svcName  = ann.serviceName();
        if(svcName == null) {
            svcName = "";
        }

        // Store binding type specified in Impl class
        String userSpecifiedBinding = null;
        javax.xml.ws.BindingType bindingAnn = (javax.xml.ws.BindingType)
                ((Class)annElem).getAnnotation(javax.xml.ws.BindingType.class);
        if(bindingAnn != null) {
            userSpecifiedBinding = bindingAnn.value();
        }
        
        // In case user gives targetNameSpace in the Impl class, that has to be used as
        // the namespace for service, port; typically user will do this in cases where
        // port_types reside in a different namespace than that of server/port.
        // Store the targetNameSpace, if any, in the impl class for later use
        String targetNameSpace = ann.targetNamespace();
        if(targetNameSpace == null) {
            targetNameSpace = "";
        }

        String portName = ann.portName();
        if(portName == null) {
            portName = "";
        }
        
        // Check if the same endpoint is already defined in webservices.xml
        WebServicesDescriptor wsDesc = bundleDesc.getWebServices();
        WebServiceEndpoint endpoint = wsDesc.getEndpointByName(portComponentName);
        WebService newWS;
        if(endpoint == null) {
            // Check if a service with the same name is already present
            // If so, add this endpoint to the existing service
            if (svcName.length()!=0) {
                newWS = wsDesc.getWebServiceByName(svcName);
            } else {
                newWS = wsDesc.getWebServiceByName(((Class)annElem).getSimpleName());
            }
            if(newWS==null) {
                newWS = new WebService();
                // service name from annotation
                if (svcName.length()!=0) {
                    newWS.setName(svcName);
                } else {
                    newWS.setName(((Class)annElem).getSimpleName());            
                }
                wsDesc.addWebService(newWS);
            }
            endpoint = new WebServiceEndpoint();
            // port-component-name is fully qualified class name
            endpoint.setEndpointName(portComponentName);
            newWS.addEndpoint(endpoint);            
            wsDesc.setSpecVersion(WebServicesDescriptorNode.SPEC_VERSION);
        } else {
            newWS = endpoint.getWebService();
        }

        // If wsdl-service is specified in the descriptor, then the targetnamespace
        // in wsdl-service should match the @WebService.targetNameSpace, if any.
        // make that assertion here - and the targetnamespace in wsdl-service, if
        // present overrides everything else
        if(endpoint.getWsdlService() != null) {
            if((targetNameSpace.length() > 0) &&
                (!endpoint.getWsdlService().getNamespaceURI().equals(targetNameSpace)) ) {
                throw new AnnotationProcessorException(
                        "Target Namespace inwsdl-service element does not match @WebService.targetNamespace", 
                        annInfo);
            }
            targetNameSpace = endpoint.getWsdlService().getNamespaceURI();
        }
        
        // Set binding id id @BindingType is specified by the user in the impl class
        if((!endpoint.hasUserSpecifiedProtocolBinding()) &&
                    (userSpecifiedBinding != null) &&
                        (userSpecifiedBinding.length() != 0)){
            endpoint.setProtocolBinding(userSpecifiedBinding);
        }        

        // Use annotated values only if the deployment descriptor equivalent has not been specified        
        if(newWS.getWsdlFileUri() == null) {
            // take wsdl location from annotation
            if (ann.wsdlLocation()!=null && ann.wsdlLocation().length()!=0) {
                newWS.setWsdlFileUri(ann.wsdlLocation());
            }
        }

        annElem = annInfo.getAnnotatedElement();
        
        // we checked that the endpoint implements the provider interface above
        Class clz = (Class) annElem;
        Class serviceEndpointIntf = null;
        for (Class intf : clz.getInterfaces()) {
            if (javax.xml.ws.Provider.class.isAssignableFrom(intf)) {
                serviceEndpointIntf = intf;
                break;
            }
        }
        if (serviceEndpointIntf==null) {
            endpoint.setServiceEndpointInterface("javax.xml.ws.Provider"); 
        } else {
            endpoint.setServiceEndpointInterface(serviceEndpointIntf.getName());
        }

        if (DOLUtils.warType().equals(bundleDesc.getModuleType())) {
            if(endpoint.getServletImplClass() == null) {
                // Set servlet impl class here
                endpoint.setServletImplClass(((Class)annElem).getName());
            }

            // Servlet link name
            WebBundleDescriptor webBundle = (WebBundleDescriptor) bundleDesc;
            if(endpoint.getWebComponentLink() == null) {
                endpoint.setWebComponentLink(portComponentName);
            }
            if(endpoint.getWebComponentImpl() == null) {
                WebComponentDescriptor webComponent = (WebComponentDescriptor) webBundle.
                    getWebComponentByCanonicalName(endpoint.getWebComponentLink());

                if (webComponent == null) {
                    //GLASSFISH-3297
                    WebComponentDescriptor[] wcs = webBundle.getWebComponentByImplName(((Class) annElem).getCanonicalName());
                    if (wcs.length > 0) {
                        webComponent = wcs[0];
                    }
                }

                // if servlet is not known, we should add it now
                if (webComponent == null) {
                    webComponent = new WebComponentDescriptorImpl();
                    webComponent.setServlet(true);
                    webComponent.setWebComponentImplementation(((Class) annElem).getCanonicalName());
                    webComponent.setName(endpoint.getEndpointName());
                    webComponent.addUrlPattern("/"+newWS.getName());
                    webBundle.addWebComponentDescriptor(webComponent);
                }
                endpoint.setWebComponentImpl(webComponent);
            }
        } else {
            if(endpoint.getEjbLink() == null) {
                EjbDescriptor[] ejbDescs = ((EjbBundleDescriptor) bundleDesc).getEjbByClassName(((Class)annElem).getName());
                if(ejbDescs.length != 1) {
                    throw new AnnotationProcessorException(
                        "Unable to find matching descriptor for EJB endpoint",
                        annInfo);
                }
                endpoint.setEjbComponentImpl(ejbDescs[0]);
                ejbDescs[0].setWebServiceEndpointInterfaceName(endpoint.getServiceEndpointInterface());
                endpoint.setEjbLink(ejbDescs[0].getName());
            }
        }

        if(endpoint.getWsdlPort() == null) {
            endpoint.setWsdlPort(new QName(targetNameSpace, portName, "ns1"));
        }
        
        if(endpoint.getWsdlService() == null) {
            endpoint.setWsdlService(new QName(targetNameSpace, svcName, "ns1"));
        }
                
        return HandlerProcessingResultImpl.getDefaultResult(getAnnotationType(), ResultType.PROCESSED);
    }

    /**
     *   If WEB-INF/sun-jaxws.xml exists and is not processed in EJB context , then it returns true.
     * @param annInfo
     * @return
     */
    private boolean isJaxwsRIDeployment(AnnotationInfo annInfo) {
        boolean riDeployment = false;
        AnnotatedElementHandler annCtx = annInfo.getProcessingContext().getHandler();
        try {
            ReadableArchive moduleArchive = annInfo.getProcessingContext().getArchive();
            if (moduleArchive != null && moduleArchive.exists("WEB-INF/sun-jaxws.xml")
                    && !((Class)annInfo.getAnnotatedElement()).isInterface()
                    && ( (annCtx instanceof WebBundleContext) || (annCtx instanceof WebComponentContext))) {
                riDeployment = true;
            }
        } catch (Exception e) {
            //continue, processing
        }
        return riDeployment;
    }
    
    /**
     * This is for the ejb webservices in war case Incase there is an@Stateless
     * and
     * @WebService and the annotationCtx is a WebBundleContext or
     * WebComponentContext in that case ignore the annotations so that they do
     * not get added twice to the bundle descriptors
     *
     */
    private boolean ignoreWebserviceAnnotations(AnnotatedElement annElem, AnnotatedElementHandler annCtx) {
        javax.ejb.Stateless stateless = annElem.getAnnotation(javax.ejb.Stateless.class);
        javax.xml.ws.WebServiceProvider webservice = annElem.getAnnotation(javax.xml.ws.WebServiceProvider.class);
        if ((stateless != null) && (webservice != null)
                && ((annCtx instanceof WebBundleContext) || (annCtx instanceof WebComponentContext))) {
            return true;
        }
        return false;
    }
}
