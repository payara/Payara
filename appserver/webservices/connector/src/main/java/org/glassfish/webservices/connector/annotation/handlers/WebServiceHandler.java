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

import java.util.StringTokenizer;
import java.util.logging.Logger;

import java.lang.reflect.AnnotatedElement;
import java.lang.annotation.Annotation;
import java.text.MessageFormat;

import org.glassfish.apf.*;

import org.glassfish.apf.impl.HandlerProcessingResultImpl;

import com.sun.enterprise.deployment.annotation.context.WebBundleContext;
import com.sun.enterprise.deployment.annotation.context.WebComponentContext;
import com.sun.enterprise.deployment.annotation.context.EjbContext;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.deployment.annotation.handlers.AbstractHandler;
import com.sun.enterprise.util.LocalStringManagerImpl;
import java.util.logging.Level;

import javax.xml.namespace.QName;
import javax.ejb.Stateless;
import javax.ejb.Singleton;

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
@AnnotationHandlerFor(javax.jws.WebService.class)
public class WebServiceHandler extends AbstractHandler {

    private static final Logger conLogger = LogUtils.getLogger();

    private static final LocalStringManagerImpl wsLocalStrings = new LocalStringManagerImpl(WebServiceHandler.class);

    /** Creates a new instance of WebServiceHandler */
    public WebServiceHandler() {
    }

    /**
     * @return an array of annotation types this annotation handler would
     * require to be processed (if present) before it processes it's own
     * annotation type.
     */
    @Override
    public Class<? extends Annotation>[] getTypeDependencies() {

        return getEjbAndWebAnnotationTypes();
    }

    @Override
    public HandlerProcessingResult processAnnotation(AnnotationInfo annInfo)
        throws AnnotationProcessorException
    {
        AnnotatedElementHandler annCtx = annInfo.getProcessingContext().getHandler();
        AnnotatedElement annElem = annInfo.getAnnotatedElement();
        AnnotatedElement origAnnElem = annElem;

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
                    wsLocalStrings.getLocalString(
                        "enterprise.deployment.annotation.handlers.wrongannotationlocation",
                        "WS00022: symbol annotation can only be specified on TYPE"),
                    annInfo);
            annInfo.getProcessingContext().getErrorHandler().error(ape);
            return HandlerProcessingResultImpl.getDefaultResult(getAnnotationType(), ResultType.FAILED);
        }


        // Ignore @WebService annotation on an interface; process only those in an actual service impl class
        if (((Class)annElem).isInterface()) {
            return HandlerProcessingResultImpl.getDefaultResult(getAnnotationType(), ResultType.PROCESSED);
        }

        if(isJaxwsRIDeployment(annInfo)) {
            // Looks like JAX-WS RI specific deployment, do not process Web Service annotations otherwise would end up as two web service endpoints
            conLogger.log(Level.INFO, LogUtils.DEPLOYMENT_DISABLED,
                    new Object[] {annInfo.getProcessingContext().getArchive().getName(), "WEB-INF/sun-jaxws.xml"});
            return HandlerProcessingResultImpl.getDefaultResult(getAnnotationType(), ResultType.PROCESSED);
        }

        // let's get the main annotation of interest.
        javax.jws.WebService ann = (javax.jws.WebService) annInfo.getAnnotation();

        BundleDescriptor bundleDesc = null;

        // Ensure that an EJB endpoint is packaged in EJBJAR and a servlet endpoint is packaged in a WAR
        try {
      /*  TODO These conditions will change since ejb in war will be supported
        //uncomment if needed
             if(annCtx instanceof EjbContext &&   (provider !=null) &&
                    (provider.getType("javax.ejb.Stateless") == null)) {
                AnnotationProcessorException ape = new AnnotationProcessorException(
                        localStrings.getLocalString("enterprise.deployment.annotation.handlers.webeppkgwrong",
                                "Class {0} is annotated with @WebService and without @Stateless
                                 but is packaged in a JAR." +
                                 " If it is supposed to be a servlet endpoint, it should be
                                  packaged in a WAR; Deployment will continue assuming  this " +
                                  "class to be just a POJO used by other classes in the JAR  being deployed",
                                new Object[] {((Class)annElem).getName()}),annInfo);
                ape.setFatal(false);
                throw ape;
            }

            if(annCtx instanceof EjbBundleContext && (provider !=null) &&
                    (provider.getType("javax.ejb.Stateless") == null)) {
                AnnotationProcessorException ape = new AnnotationProcessorException(
                        localStrings.getLocalString  ("enterprise.deployment.annotation.handlers.webeppkgwrong",
                                "Class {0} is annotated with @WebService and without @Stateless but is packaged in a JAR." +
                                        " If it is supposed to be a servlet endpoint, it should be packaged in a WAR; Deployment will continue assuming this " +
                                        "class to be just a POJO used by other classes in the JARbeing deployed",
                                new Object[] {((Class)annElem).getName()}),annInfo);
                ape.setFatal(false);
                throw ape;
            }
            if(annCtx instanceof WebBundleContext && (provider !=null) &&
                    (provider.getType("javax.ejb.Stateless") != null)) {
                AnnotationProcessorException ape = new AnnotationProcessorException(
                        localStrings.getLocalString
                         ("enterprise.deployment.annotation.handlers.ejbeppkgwrong",
                         "Class {0} is annotated with @WebService and @Stateless but is packaged in a WAR." +" If it is supposed to be an EJB endpoint, it should be  packaged in a JAR; Deployment will continue assuming this "
                        +" class to be just a POJO used by other classes in the WAR being deployed",
                                new Object[] {((Class)annElem).getName()}),annInfo);
                ape.setFatal(false);
                throw ape;
            }*/

            // let's see the type of web service we are dealing with...
            if ((ejbProvider!= null) && ejbProvider.getType("javax.ejb.Stateless")!=null &&(annCtx
                    instanceof EjbContext)) {
                // this is an ejb !
                EjbContext ctx = (EjbContext) annCtx;
                bundleDesc = ctx.getDescriptor().getEjbBundleDescriptor();
                bundleDesc.setSpecVersion("3.0");
            } else {
                // this has to be a servlet since there is no @Servlet annotation yet
                if(annCtx instanceof WebComponentContext) {
                    bundleDesc = ((WebComponentContext)annCtx).getDescriptor().getWebBundleDescriptor();
                } else if ( !(annCtx instanceof WebBundleContext)) {
                    return getInvalidAnnotatedElementHandlerResult(
                            annInfo.getProcessingContext().getHandler(), annInfo);
                }

                bundleDesc = ((WebBundleContext)annCtx).getDescriptor();

                bundleDesc.setSpecVersion("2.5");
            }
        }catch (Exception e) {
            throw new AnnotationProcessorException(
                    wsLocalStrings.getLocalString("webservice.annotation.exception",
                        "WS00023: Exception in processing @Webservice : {0}",
                        e.getMessage()));
        }
        //WebService.name in the impl class identifies port-component-name
        // If this is specified in impl class, then that takes precedence
        String portComponentName = ann.name();

        // As per JSR181, the serviceName is either specified in the deployment descriptor
        // or in @WebSErvice annotation in impl class; if neither service name implclass+Service
        String svcNameFromImplClass = ann.serviceName();
        String implClassName = ((Class) annElem).getSimpleName();
        String implClassFullName = ((Class)annElem).getName();

        // In case user gives targetNameSpace in the Impl class, that has to be used as
        // the namespace for service, port; typically user will do this in cases where
        // port_types reside in a different namespace than that of server/port.
        // Store the targetNameSpace, if any, in the impl class for later use
        String targetNameSpace = ann.targetNamespace();

        // As per JSR181, the portName is either specified in deployment desc or in @WebService
        // in impl class; if neither, it will @WebService.name+Port; if @WebService.name is not there,
        // then port name is implClass+Port
        String portNameFromImplClass = ann.portName();
        if( (portNameFromImplClass == null) ||
            (portNameFromImplClass.length() == 0) ) {
            if( (portComponentName != null) && (portComponentName.length() != 0) ) {
                portNameFromImplClass = portComponentName + "Port";
            } else {
                portNameFromImplClass = implClassName+"Port";
            }
        }

        // Store binding type specified in Impl class
        String userSpecifiedBinding = null;
        javax.xml.ws.BindingType bindingAnn = (javax.xml.ws.BindingType)
                ((Class)annElem).getAnnotation(javax.xml.ws.BindingType.class);
        if(bindingAnn != null) {
            userSpecifiedBinding = bindingAnn.value();
        }

        // Store wsdlLocation in the impl class (if any)
        String wsdlLocation = null;
        if (ann.wsdlLocation()!=null && ann.wsdlLocation().length()!=0) {
            wsdlLocation = ann.wsdlLocation();
        }

        // At this point, we need to check if the @WebService points to an SEI
        // with the endpointInterface attribute, if that is the case, the
        // remaining attributes should be extracted from the SEI instead of SIB.
        if (ann.endpointInterface()!=null && ann.endpointInterface().length()>0) {
            Class endpointIntf;
            try {
                endpointIntf = ((Class) annElem).getClassLoader().loadClass(ann.endpointInterface());
            } catch(java.lang.ClassNotFoundException cfne) {
                throw new AnnotationProcessorException(
                        localStrings.getLocalString("enterprise.deployment.annotation.handlers.classnotfound",
                            "class {0} referenced from annotation symbol cannot be loaded",
                            new Object[] { ann.endpointInterface() }), annInfo);
            }
            annElem = endpointIntf;

            ann = annElem.getAnnotation(javax.jws.WebService.class);
            if (ann==null) {
                throw new AnnotationProcessorException(
                        wsLocalStrings.getLocalString("no.webservice.annotation",
                            "WS00025: SEI {0} referenced from the @WebService annotation on {1}  does not contain a @WebService annotation",
                            ((javax.jws.WebService) annInfo.getAnnotation()).endpointInterface(),
                            ((Class) annElem).getName()));
            }

            // SEI cannot have @BindingType
            if(annElem.getAnnotation(javax.xml.ws.BindingType.class) != null) {
                throw new AnnotationProcessorException(
                        wsLocalStrings.getLocalString("cannot.have.bindingtype",
                            "WS00026: SEI {0} cannot have @BindingType",
                            ((javax.jws.WebService) annInfo.getAnnotation()).endpointInterface()
                    ));
            }
        }

        WebServicesDescriptor wsDesc = bundleDesc.getWebServices();
        //WebService.name not found; as per 109, default port-component-name
        //is the simple class name as long as the simple class name will be a
        // unique port-component-name for this module
        if(portComponentName == null || portComponentName.length() == 0) {
            portComponentName = implClassName;
        }
        // Check if this port-component-name is unique for this module
        WebServiceEndpoint wep = wsDesc.getEndpointByName(portComponentName);
        if(wep!=null) {
            //there is another port-component by this name in this module;
            //now we have to look at the SEI/impl of that port-component; if that SEI/impl
            //is the same as the current SEI/impl then it means we have to override values;
            //If the SEI/impl classes do not match, then no overriding should happen; we should
            //use fully qualified class name as port-component-name for the current endpoint
            if((wep.getServiceEndpointInterface() != null) &&
               (wep.getServiceEndpointInterface().length() != 0) &&
               (!((Class)annElem).getName().equals(wep.getServiceEndpointInterface()))) {
                portComponentName = implClassFullName;
            }
        }

        // Check if the same endpoint is already defined in webservices.xml
        // This has to be done again after applying the 109 rules as above
        // for port-component-name
        WebServiceEndpoint endpoint = wsDesc.getEndpointByName(portComponentName);
        WebService newWS;
        if(endpoint == null) {
            if (DOLUtils.warType().equals(bundleDesc.getModuleType())) {
                // http://java.net/jira/browse/GLASSFISH-17204
                WebComponentDescriptor[] wcByImplName = ((WebBundleDescriptor) bundleDesc).getWebComponentByImplName(implClassFullName);
                for (WebComponentDescriptor wc : wcByImplName) {
                    if (!wsDesc.getEndpointsImplementedBy(wc).isEmpty()) {
                        //URL mapping for annotated service exists - it can be JAX-RPC service
                        //as well as some servlet or maybe only invalid port-component-name,
                        //so let user know about possible error
                        logger.log(Level.SEVERE, LogUtils.WS_URLMAPPING_EXISTS, new Object[]{implClassFullName});
                        break;
                    }
                }
            }
            // Check if a service with the same name is already present
            // If so, add this endpoint to the existing service
            if (svcNameFromImplClass!=null && svcNameFromImplClass.length()!=0) {
                newWS = wsDesc.getWebServiceByName(svcNameFromImplClass);
            } else {
                newWS = wsDesc.getWebServiceByName(implClassName+"Service");
            }
            if(newWS==null) {
                newWS = new WebService();
                // service name from annotation
                if (svcNameFromImplClass!=null && svcNameFromImplClass.length()!=0) {
                    newWS.setName(svcNameFromImplClass);
                } else {
                    newWS.setName(implClassName+"Service");
                }
                wsDesc.addWebService(newWS);
            }
            endpoint = new WebServiceEndpoint();
            if (portComponentName!=null && portComponentName.length()!=0) {
                endpoint.setEndpointName(portComponentName);
            } else {
                endpoint.setEndpointName(((Class) annElem).getName());
            }
            newWS.addEndpoint(endpoint);
            wsDesc.setSpecVersion (WebServicesDescriptorNode.SPEC_VERSION);
        } else {
            newWS = endpoint.getWebService();
        }

        // If wsdl-service is specified in the descriptor, then the targetnamespace
        // in wsdl-service should match the @WebService.targetNameSpace, if any.
        // make that assertion here - and the targetnamespace in wsdl-service, if
        // present overrides everything else
        if(endpoint.getWsdlService() != null) {
            if( (targetNameSpace != null) && (targetNameSpace.length() != 0 ) &&
                (!endpoint.getWsdlService().getNamespaceURI().equals(targetNameSpace)) ) {
                AnnotationProcessorException ape = new AnnotationProcessorException(
                        wsLocalStrings.getLocalString("mismatch.targetnamespace",
                            "WS00027: Target Namespace in wsdl-service element does not match @WebService.targetNamespace"),
                        annInfo);
                annInfo.getProcessingContext().getErrorHandler().error(ape);
                return HandlerProcessingResultImpl.getDefaultResult(getAnnotationType(), ResultType.FAILED);
            }
            targetNameSpace = endpoint.getWsdlService().getNamespaceURI();
        }

        // Service and port should reside in the same namespace - assert that
        if( (endpoint.getWsdlService() != null) &&
            (endpoint.getWsdlPort() != null) ) {
            if(!endpoint.getWsdlService().getNamespaceURI().equals(
                                    endpoint.getWsdlPort().getNamespaceURI())) {
                AnnotationProcessorException ape = new AnnotationProcessorException(
                        wsLocalStrings.getLocalString("mismatch.port.targetnamespace",
                            "WS00028: Target Namespace for wsdl-service and wsdl-port should be the same"),
                        annInfo);
                annInfo.getProcessingContext().getErrorHandler().error(ape);
                return HandlerProcessingResultImpl.getDefaultResult(getAnnotationType(), ResultType.FAILED);
            }
        }

        //Use annotated values only if the deployment descriptor equivalen has not been specified

        // If wsdlLocation was not given in Impl class, see if it is present in SEI
        // Set this in DOL if there is no Depl Desc entry
        // Precedence given for wsdlLocation in impl class
        if(newWS.getWsdlFileUri() == null) {
            if(wsdlLocation != null) {
                newWS.setWsdlFileUri(wsdlLocation);
            } else {
                if (ann.wsdlLocation()!=null && ann.wsdlLocation().length()!=0) {
                    newWS.setWsdlFileUri(ann.wsdlLocation());
                }
            }
        }

        // Set binding id id @BindingType is specified by the user in the impl class
        if((!endpoint.hasUserSpecifiedProtocolBinding()) &&
                    (userSpecifiedBinding != null) &&
                        (userSpecifiedBinding.length() != 0)){
            endpoint.setProtocolBinding(userSpecifiedBinding);
        }

        if(endpoint.getServiceEndpointInterface() == null) {
            // take SEI from annotation
            if (ann.endpointInterface()!=null && ann.endpointInterface().length()!=0) {
                endpoint.setServiceEndpointInterface(ann.endpointInterface());
            } else {
                endpoint.setServiceEndpointInterface(((Class)annElem).getName());
            }
        }

        // at this point the SIB has to be used no matter what @WebService was used.
        annElem = annInfo.getAnnotatedElement();

        if (DOLUtils.warType().equals(bundleDesc.getModuleType())) {
            if(endpoint.getServletImplClass() == null) {
                // Set servlet impl class here
                endpoint.setServletImplClass(((Class)annElem).getName());
            }

            // Servlet link name
            WebBundleDescriptor webBundle = (WebBundleDescriptor) bundleDesc;
            if(endpoint.getWebComponentLink() == null) {
                //<servlet-link> = fully qualified name of the implementation class
                endpoint.setWebComponentLink(implClassFullName);
            }
            if(endpoint.getWebComponentImpl() == null) {
                WebComponentDescriptor webComponent = (WebComponentDescriptor) webBundle.
                    getWebComponentByCanonicalName(endpoint.getWebComponentLink());

                if (webComponent == null) {
                    //GLASSFISH-3297
                    WebComponentDescriptor[] wcs = webBundle.getWebComponentByImplName(implClassFullName);
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

           
            //TODO BM handle stateless
            Stateless stateless = null;
            try {
                stateless = annElem.getAnnotation(javax.ejb.Stateless.class);
            } catch (Exception e) {
                if (logger.isLoggable(Level.FINE)) {
                    //This can happen in the web.zip installation where there is no ejb
                    //Just logging the error
                    conLogger.log(Level.FINE, LogUtils.EXCEPTION_THROWN, e);
                }
            }
            Singleton singleton = null;
            try {
                singleton = annElem.getAnnotation(javax.ejb.Singleton.class);
            } catch (Exception e) {
                if (logger.isLoggable(Level.FINE)) {
                    //This can happen in the web.zip installation where there is no ejb
                    //Just logging the error
                    conLogger.log(Level.FINE, LogUtils.EXCEPTION_THROWN, e);
                }
            }
            String name;


            if ((stateless != null) &&((stateless).name()==null || stateless.name().length()>0)) {
                name = stateless.name();
            } else if ((singleton != null) &&((singleton).name()==null || singleton.name().length()>0)) {
                name = singleton.name();

            }else {
                name = ((Class) annElem).getSimpleName();
            }
            EjbDescriptor ejb = ((EjbBundleDescriptor) bundleDesc).getEjbByName(name);
            endpoint.setEjbComponentImpl(ejb);
            ejb.setWebServiceEndpointInterfaceName(endpoint.getServiceEndpointInterface());
            if (endpoint.getEjbLink()== null)
                endpoint.setEjbLink(ejb.getName());

        }

        if(endpoint.getWsdlPort() == null) {
            // Use targetNameSpace given in wsdl-service/Impl class for port and service
            // If none, derive the namespace from package name and this will be used for
            // service and port - targetNamespace, if any, in SEI will be used for pprtType
            // during wsgen phase
            if(targetNameSpace == null || targetNameSpace.length()==0) {
                // No targerNameSpace anywhere; calculate targetNameSpace and set wsdl port
                // per jax-ws 2.0 spec, the target name is the package name in
                // the reverse order prepended with http://
                if (((Class) annElem).getPackage()!=null) {

                    StringTokenizer tokens = new StringTokenizer(
                            ((Class) annElem).getPackage().getName(), ".", false);

                    if (tokens.hasMoreElements()) {
                        while (tokens.hasMoreElements()) {
                            if(targetNameSpace == null || targetNameSpace.length()==0) {
                                targetNameSpace=tokens.nextElement().toString();
                            } else {
                                targetNameSpace=tokens.nextElement().toString()+"."+targetNameSpace;
                            }
                        }
                    } else {
                        targetNameSpace = ((Class) annElem).getPackage().getName();
                    }
                } else {
                    throw new AnnotationProcessorException(
                            wsLocalStrings.getLocalString("missing.targetnamespace",
                            "WS00029: The javax.jws.WebService annotation targetNamespace must be used for classes or interfaces that are in no package"));
                }
                targetNameSpace = "http://" + (targetNameSpace==null?"":targetNameSpace+"/");
            }
            // WebService.portName = wsdl-port
            endpoint.setWsdlPort(new QName(targetNameSpace, portNameFromImplClass, "ns1"));
        }

        if(endpoint.getWsdlService() == null) {
            // Set wsdl-service properly; namespace is the same as that of wsdl port;
            // service name derived from deployment desc / annotation / default
            String serviceNameSpace = endpoint.getWsdlPort().getNamespaceURI();
            String serviceName = null;
            if ( (svcNameFromImplClass != null) &&
                  (svcNameFromImplClass.length()!= 0)) {
                // Use the serviceName annotation if available
                serviceName= svcNameFromImplClass;
            } else {
              serviceName = newWS.getName();
            }
            endpoint.setWsdlService(new QName(serviceNameSpace, serviceName, "ns1"));
        }

        // Now force a HandlerChain annotation processing
        // This is to take care of the case where the endpoint Impl class does not
        // have @HandlerChain but the SEI has one specified through JAXWS customization
        if((((Class)origAnnElem).getAnnotation(javax.jws.HandlerChain.class)) == null) {
            return (new HandlerChainHandler()).processHandlerChainAnnotation(annInfo, annCtx, origAnnElem, (Class)origAnnElem, true);
        }
        return HandlerProcessingResultImpl.getDefaultResult(getAnnotationType(), ResultType.PROCESSED);
    }

    /**
     * This is for the ejb webservices in war case
     * Incase there is an@Stateless and @WebService and the annotationCtx
     * is a WebBundleContext or WebComponentContext
     * in that case ignore the annotations so that they do not get added twice
     * to the bundle descriptors
     *
     */
    private boolean ignoreWebserviceAnnotations(AnnotatedElement annElem,AnnotatedElementHandler annCtx){

        javax.ejb.Stateless stateless = annElem.getAnnotation(javax.ejb.Stateless.class);
        javax.jws.WebService webservice = annElem.getAnnotation(javax.jws.WebService.class);
        if ((stateless != null) && (webservice != null)
               && ( (annCtx instanceof WebBundleContext) || (annCtx instanceof WebComponentContext)) ) {
            return true;
        }
        return false;

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
}
