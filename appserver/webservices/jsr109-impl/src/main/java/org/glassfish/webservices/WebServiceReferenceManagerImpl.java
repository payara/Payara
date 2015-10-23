/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.webservices;

import com.sun.enterprise.deployment.*;
import com.sun.xml.ws.api.server.*;
import com.sun.xml.ws.transport.http.servlet.ServletAdapter;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.deployment.versioning.VersioningUtils;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.container.common.spi.WebServiceReferenceManager;
import com.sun.xml.ws.api.FeatureConstructor;
import com.sun.xml.ws.resources.ModelerMessages;

import javax.inject.Inject;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.InitialContext;
import javax.xml.namespace.QName;
import javax.xml.rpc.ServiceFactory;
import javax.xml.ws.soap.MTOMFeature;
import javax.xml.ws.soap.AddressingFeature;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.RespectBindingFeature;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.spi.WebServiceFeatureAnnotation;
import java.io.*;
import java.lang.reflect.*;
import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.PrivilegedActionException;
import java.net.URL;


/**
 * This class acts as a service to resolve the
 * </code>javax.xml.ws.WebServiceRef</code> references
 * and also <code>javax.xml.ws.WebServiceContext</code>
 * Whenever a lookup is done from GlassfishNamingManagerImpl
 * these methods are invoked to resolve the references
 *
 * @author Bhakti Mehta
 */

@Service
public class WebServiceReferenceManagerImpl implements WebServiceReferenceManager {

    @Inject
    private ServerEnvironment serverEnv;

    protected Logger logger = LogUtils.getLogger();

    public Object getWSContextObject() {
        return new WebServiceContextImpl();
    }

    public Object resolveWSReference(ServiceReferenceDescriptor desc, Context context)
            throws NamingException {


        //Taken from NamingManagerImpl.getClientServiceObject
        Class serviceInterfaceClass = null;
        Object returnObj = null;
        WsUtil wsUtil = new WsUtil();

        //Implementation for new lookup element in WebserviceRef
        InitialContext iContext = new InitialContext();
        if( desc.hasLookupName()) {
            return iContext.lookup(desc.getLookupName());

        }

        try {

            WSContainerResolver.set(desc);

            ClassLoader cl = Thread.currentThread().getContextClassLoader();

            serviceInterfaceClass = cl.loadClass(desc.getServiceInterface());

            resolvePortComponentLinks(desc);

            javax.xml.rpc.Service serviceDelegate = null;
            javax.xml.ws.Service jaxwsDelegate = null;
            Object injValue = null;

            if( desc.hasGeneratedServiceInterface() || desc.hasWsdlFile() ) {

                String serviceImplName  = desc.getServiceImplClassName();
                if(serviceImplName != null) {
                    Class serviceImplClass  = cl.loadClass(serviceImplName);
                    serviceDelegate = (javax.xml.rpc.Service) serviceImplClass.newInstance();
                } else {

                    // The target is probably a post JAXRPC-1.1- based service;
                    // If Service Interface class is set, check if it is indeed a subclass of Service
                    // initiateInstance should not be called if the user has given javax.xml.ws.Service itself
                    // as the interface through DD
                    if(javax.xml.ws.Service.class.isAssignableFrom(serviceInterfaceClass) &&
                            !javax.xml.ws.Service.class.equals(serviceInterfaceClass) ) {
                        // OK - the interface class is indeed the generated service class; get an instance
                        injValue = initiateInstance(serviceInterfaceClass, desc);
                    } else {
                        // First try failed; Try to get the Service class type from injected field name
                        // and from there try to get an instance of the service class

                        // I assume the at all inejction target are expecting the SAME service
                        // interface, therefore I take the first one.
                        if (desc.isInjectable()) {

                            InjectionTarget target = desc.getInjectionTargets().iterator().next();
                            Class serviceType = null;
                            if (target.isFieldInjectable()) {
                                java.lang.reflect.Field f = target.getField();
                                if(f == null) {
                                    String fName = target.getFieldName();
                                    Class targetClass = cl.loadClass(target.getClassName());
                                    try {
                                        f = targetClass.getDeclaredField(fName);
                                    } catch(java.lang.NoSuchFieldException nsfe) {}// ignoring exception
                                }
                                if(f != null) {
                                    serviceType = f.getType();
                                }
                            }
                            if (target.isMethodInjectable()) {
                                Method m = target.getMethod();
                                if(m == null) {
                                    String mName = target.getMethodName();
                                    Class targetClass = cl.loadClass(target.getClassName());
                                    try {
                                        m = targetClass.getDeclaredMethod(mName);
                                    } catch(java.lang.NoSuchMethodException nsfe) {}// ignoring exception
                                }
                                if (m != null && m.getParameterTypes().length==1) {
                                    serviceType = m.getParameterTypes()[0];
                                }
                            }
                            if (serviceType!=null){
                                Class loadedSvcClass = cl.loadClass(serviceType.getCanonicalName());
                                injValue = initiateInstance(loadedSvcClass, desc);
                            }
                        }
                    }
                    // Unable to get hold of generated service class -> try the Service.create avenue to get a Service
                    if(injValue == null) {
                        // Here create the service with WSDL (overridden wsdl if wsdl-override is present)
                        // so that JAXWS runtime uses this wsdl @ runtime
                        javax.xml.ws.Service svc =
                                javax.xml.ws.Service.create((new WsUtil()).privilegedGetServiceRefWsdl(desc),
                                        desc.getServiceName());
                        jaxwsDelegate = new JAXWSServiceDelegate(desc, svc, cl);
                    }
                }

                if( desc.hasHandlers() ) {
                    // We need the service's ports to configure the
                    // handler chain (since service-ref handler chain can
                    // optionally specify handler-port association)
                    // so create a configured service and call getPorts

                    javax.xml.rpc.Service configuredService =
                            wsUtil.createConfiguredService(desc);
                    Iterator ports = configuredService.getPorts();
                    wsUtil.configureHandlerChain
                            (desc, serviceDelegate, ports, cl);
                }

                // check if this is a post 1.1 web service
                if(javax.xml.ws.Service.class.isAssignableFrom(serviceInterfaceClass)) {
                    // This is a JAXWS based webservice client;
                    // process handlers and mtom setting
                    // moved test for handlers into wsUtil, in case
                    // we have to add system handler

                    javax.xml.ws.Service service =
                            (injValue != null ?
                                    (javax.xml.ws.Service) injValue : jaxwsDelegate);

                    if (service != null) {
                        // Now configure client side handlers
                        wsUtil.configureJAXWSClientHandlers(service, desc);
                    }
                    // the requested resource is not the service but one of its port.
                    if (injValue!=null && desc.getInjectionTargetType()!=null) {
                        Class requestedPortType = service.getClass().getClassLoader().loadClass(desc.getInjectionTargetType());
                        ArrayList<WebServiceFeature> wsFeatures = getWebServiceFeatures(desc);
                        if (wsFeatures.size() >0) {
                             injValue = service.getPort(requestedPortType,wsFeatures.toArray(new WebServiceFeature[wsFeatures.size()]));
                        }   else {
                            injValue = service.getPort(requestedPortType);
                        }
                    }

                }

            } else {
                // Generic service interface / no WSDL
                QName serviceName = desc.getServiceName();
                if( serviceName == null ) {
                    // ServiceFactory API requires a service-name.
                    // However, 109 does not allow getServiceName() to be
                    // called, so it's ok to use a dummy value.
                    serviceName = new QName("urn:noservice", "servicename");
                }
                ServiceFactory serviceFac = ServiceFactory.newInstance();
                serviceDelegate = serviceFac.createService(serviceName);
            }

            // Create a proxy for the service object.
            // Get a proxy only in jaxrpc case because in jaxws the service class is not
            // an interface any more
            InvocationHandler handler = null;
            if(serviceDelegate != null) {

                handler = new ServiceInvocationHandler(desc, serviceDelegate, cl);
                returnObj = Proxy.newProxyInstance
                        (cl, new Class[] { serviceInterfaceClass }, handler);
            } else if(jaxwsDelegate != null) {
                returnObj = jaxwsDelegate;
            } else if(injValue != null) {
                returnObj = injValue;
            }
        } catch(PrivilegedActionException pae) {
            logger.log(Level.WARNING, LogUtils.EXCEPTION_THROWN, pae);
            NamingException ne = new NamingException();
            ne.initCause(pae.getCause());
            throw ne;
        } catch(Exception e) {
            logger.log(Level.WARNING, LogUtils.EXCEPTION_THROWN, e);
            NamingException ne = new NamingException();
            ne.initCause(e);
            throw ne;
        } finally {
            WSContainerResolver.unset();
        }

        return returnObj;
    }

    private Object initiateInstance(Class svcClass, ServiceReferenceDescriptor desc)
            throws Exception {

        //TODO BM if JBI needs this reenable it
        /*com.sun.enterprise.webservice.ServiceRefDescUtil descUtil =
           new com.sun.enterprise.webservice.ServiceRefDescUtil();
        descUtil.preServiceCreate(desc);*/
        WsUtil wsu = new WsUtil();
        URL wsdlFile = wsu.privilegedGetServiceRefWsdl(desc);
        /* TODO BM resolve catalog
        // Check if there is a catalog for this web service client
        // If so resolve the catalog entry
        String genXmlDir;
        if(desc.getBundleDescriptor().getApplication() != null) {
            genXmlDir = desc.getBundleDescriptor().getApplication().getGeneratedXMLDirectory();
            if(!desc.getBundleDescriptor().getApplication().isVirtual()) {
                String subDirName = desc.getBundleDescriptor().getModuleDescriptor().getArchiveUri();
                genXmlDir += (File.separator+subDirName.replaceAll("\\.",  "_"));
            }
        } else {
            // this is the case of an appclient being run as class file from command line
            genXmlDir = desc.getBundleDescriptor().getModuleDescriptor().getArchiveUri();
        }
        File catalogFile = new File(genXmlDir,
                desc.getBundleDescriptor().getDeploymentDescriptorDir() +
                    File.separator + "jax-ws-catalog.xml");

        if(catalogFile.exists()) {
            wsdlFile = wsu.resolveCatalog(catalogFile, desc.getWsdlFileUri(), null);
        }   */


        Object obj = null ;

        java.lang.reflect.Constructor cons = svcClass.getConstructor
                (new Class[]{java.net.URL.class,
                        javax.xml.namespace.QName.class});
        try {
            obj =
                    cons.newInstance(wsdlFile, desc.getServiceName());
        } catch (Exception e) {
            /*
             * If WSDL URL is not accessible over http, trying to get an instance via
             * reflection results in InvocationTargetException. If InvocationTargetException
             * is thrown,then catch the exception and generate wsdl in generated xml directory
             * of the application being deployed.
             */
            if (e instanceof InvocationTargetException) {
                URL optionalWsdlURL = generateWsdlFile(desc);
                if (optionalWsdlURL == null)
                    throw e;
                obj = cons.newInstance(optionalWsdlURL, desc.getServiceName());
            }
        }



        /*TODO BM if jbi needs this reenable it
        descUtil.postServiceCreate();
        */
        return obj;

    }



    /**
     * This method returns the location where optional wsdl file will be generated.
     * The directory will be a directory having same name as WebService name inside
     * application's generated xml directory. The name of the wsdl file will be
     * wsdl.xml e.g. if application name is test and service name is Translator,
     * then the location of wsdl will be
     * $Glassfish_home/domains/domain1/generated/xml/test/Translator/wsdl.xml
     *
     * @param desc ServiceReferenceDescriptor
     * @return optional wsdl file location
     */
    private File getOptionalWsdlLocation(ServiceReferenceDescriptor desc) {
        File generatedXmlDir = serverEnv.getApplicationGeneratedXMLPath();
        return new File(new File(new File(generatedXmlDir,
                VersioningUtils.getRepositoryName(desc.getBundleDescriptor().getApplication()
                        .getRegistrationName())), desc.getServiceLocalPart()), "wsdl.xml");
    }

    private void createParentDirs(File optionalWsdlLocation) throws IOException {
        File parent = optionalWsdlLocation.getParentFile();
        mkDirs(parent);
    }

    private URL generateWsdlFile(ServiceReferenceDescriptor desc) throws IOException {

       /*
        * Following piece of code is basically a copy-paste from JAXWSServlet's
        * doGet method (line 230) and from com.sun.xml.ws.transport.http.servlet.HttpAdapter's
        * publishWSDL method (line 587).This piece of code is not completely clear to me,
        * what I have understood so far is, during WSEndPoint creation on line 267 in
        * WSServletContextListener, com.sun.xml.ws.server.EndPointFactory.create (line 116)
        * method is invoked where ServiceDocumentImpl instance is created, which is later
        * being fetched here to generate wsdl. When serviceDefinition.getPrimary() is
        * invoked, basically it returns the reference to wsdl document marked as primary
        * wsdl inside ServiceDefinition. Probably we can directly fetch this wsdl
        * but for now I will go with the way it has been implemented in HttpAdapter.
        */

        File optionalWsdl = getOptionalWsdlLocation(desc);

        /*
         * Its possible that in a given application there are more than one Filter/Servlet
         * with loadOnStartup=1 having WebServiceRef annotation,or WebServiceRef
         * annotation is used at multiple places within the same Filter/Servlet,
         * in which case, when processing is going on for second filter/servlet
         * or annotation referring to the same web service, then wsdl file has
         * already been generated at this point in time and there is no need to
         * generate it again.
         */

        if (optionalWsdl.exists())
            return optionalWsdl.toURI().toURL();

        createParentDirs(optionalWsdl);
        ServletAdapter targetEndpoint = getServletAdapter(desc);
        if (targetEndpoint == null)
            return null;
        ServiceDefinition serviceDefinition = targetEndpoint.getServiceDefinition();
        Iterator wsdlnum = serviceDefinition.iterator();
        SDDocument wsdlDocument = null;
        while (wsdlnum.hasNext()) {
            SDDocument xsdnum = (SDDocument) wsdlnum.next();
            if (xsdnum == serviceDefinition.getPrimary()) {
                wsdlDocument = xsdnum;
                break;
            }
        }

        if (wsdlDocument == null)
            return null;

        OutputStream outputStream = null;
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(optionalWsdl));
            PortAddressResolver portAddressResolver = targetEndpoint
                    .getPortAddressResolver(getBaseAddress(desc.getWsdlFileUrl()));
            DocumentAddressResolver resolver = targetEndpoint.getDocumentAddressResolver(portAddressResolver);
            wsdlDocument.writeTo(portAddressResolver, resolver, outputStream);
        } finally {
            if (outputStream != null)
                outputStream.close();
        }

        return optionalWsdl.toURI().toURL();
    }

    /**
     * Returns ServletAdapter instance holding wsdl for the WebService being referred
     * in WebServiceRef annotation.
     *
     * @param desc ServiceReferenceDescriptor
     * @return ServletAdapter instance having wsdl contents.
     */
    private ServletAdapter getServletAdapter(ServiceReferenceDescriptor desc) {

        WebBundleDescriptor webBundle = null;
        WebServicesDescriptor webServicesDescriptor = null;

        /*
         * If flow has reached to this part of the code,then in all likelihood,
         * the wsdl is available under the context root of the a web application
         * and hence the BundleDescriptor being referred in ServiceReferenceDescriptor
         * is an instance of WebBundleDescriptor.
         */

        if (desc.getBundleDescriptor() instanceof WebBundleDescriptor) {

            webBundle = ((WebBundleDescriptor) desc.getBundleDescriptor());

        } else {

            /*
             * If above assumption is not true, then make one last attempt to fetch
             * all required params from the wsdl url stored in ServiceReferenceDescriptor.
             */

            return getServletAdapterBasedOnWsdlUrl(desc);
        }

        /*
         * Get WebServicesDescriptor from WebBundleDescriptorImpl, Since we are
         * dealing with WebServiceRef annotation here, WebServicesDescriptor ought to have
         * reference to WebService in question. WebServicesDescriptor is never null as it
         * is being initialized at class level in BundleDescriptor.
         */

        WebServicesDescriptor wsDesc = webBundle.getWebServices();

        assert wsDesc != null;

        /*
         * WebService name is being fetched by invoking getServiceLocalPart()
         * on ServiceReferenceDescriptor. ServiceLocalPart is set when WebServiceClient
         * annotated class is processed inside
         * org.glassfish.webservices.connector.annotation.handlers.WebServiceRefHandler's
         * processAWsRef call (line 339). WebServiceClient annotation have name param pointing
         * to webservice in question.
         */

        assert desc.getServiceLocalPart() != null;

        WebService webService = wsDesc.getWebServiceByName(desc.getServiceLocalPart());

        /*
         * If an unlikely event when there is no associated webService or desc.getServiceLocalPart()
         * itself is null, then fall back on fetching ServletAdapter based on wsdl url.
         */
        if (webService == null)
            return getServletAdapterBasedOnWsdlUrl(desc);

        String contextRoot = webBundle.getContextRoot();
        String webSevicePath = null;
        String publishingContext = null;
        /*
         * Iterate over all associated WebServiceEndPoints for this WebService
         * and break when condition specified in if block is met. This is the same
         * condition based on which wsdl url is set in first place for this
         * ServiceReferenceDescriptor and hence this must hold true in this context too.
         */

        for (WebServiceEndpoint endpoint : webService.getEndpoints()) {
            if (desc.getServiceName().equals(endpoint.getServiceName())
                    && desc.getServiceNamespaceUri().equals(endpoint.getWsdlService().getNamespaceURI())) {
                String endPointAddressURI = endpoint.getEndpointAddressUri();
                if (endPointAddressURI == null || endPointAddressURI.length() == 0)
                    return null;
                webSevicePath = endPointAddressURI.startsWith("/") ? endPointAddressURI : ("/" + endPointAddressURI);
                publishingContext = "/" + endpoint.getPublishingUri() + "/" + webService.getWsdlFileUri();
                Adapter adapter = JAXWSAdapterRegistry.getInstance()
                        .getAdapter(contextRoot, webSevicePath, publishingContext);
                return adapter instanceof ServletAdapter ? (ServletAdapter) adapter : null;

            }
        }
        return null;
    }


    /**
     * This method basically is a fall back mechanism to fetch required
     * parameters from wsdl url stored in ServiceReferenceDescriptor. The flow reaches
     * here only in case where required parameters could not be fetched
     * from WebBundleDescriptor.
     *
     * @param desc ServiceReferenceDescriptor
     * @return ServletAdapter instance having wsdl contents.
     */
    private ServletAdapter getServletAdapterBasedOnWsdlUrl(ServiceReferenceDescriptor desc) {

        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, LogUtils.SERVLET_ADAPTER_BASED_ON_WSDL_URL,
                    new Object[]{desc.getServiceLocalPart(), desc.getWsdlFileUrl()});
        }

        URL wsdl = desc.getWsdlFileUrl();
        String wsdlPath = wsdl.getPath().trim();
        if (!wsdlPath.contains(WebServiceEndpoint.PUBLISHING_SUBCONTEXT))
            return null;

         /*
          * WsdlPath indeed contains the WebServiceEndpoint.PUBLISHING_SUBCONTEXT,
          * e.g.assuming that context root is test and Service name is Translator
          * then wsdl url must be in the following format :
          * /test/Translator/__container$publishing$subctx/null?wsdl
          */

        String contextRootAndPath = wsdlPath.substring(1,
                wsdlPath.indexOf(WebServiceEndpoint.PUBLISHING_SUBCONTEXT) - 1); // test/Translator
        if (!(contextRootAndPath.length() > 0))
            return null;
        String[] contextRootAndPathArray = contextRootAndPath.split("/"); // {test, Translator}
        if (contextRootAndPathArray.length != 2)
            return null;
        if (contextRootAndPathArray[0] == null)
            return null;
        String contextRoot = "/" + contextRootAndPathArray[0];  // /test
        if (contextRootAndPathArray[1] == null)
            return null;
        String webSevicePath = "/" + contextRootAndPathArray[1]; // /Translator
        String urlPattern = wsdlPath.substring(contextRoot.length());
        Adapter adapter = JAXWSAdapterRegistry.getInstance()
                .getAdapter(contextRoot, webSevicePath, urlPattern);
        return adapter instanceof ServletAdapter ? (ServletAdapter) adapter : null;

    }

    private static String getBaseAddress(URL wsdlUrl) {
        return wsdlUrl.getProtocol() + "://" + wsdlUrl.getHost() + ":" + wsdlUrl.getPort();
    }

    private void mkDirs(File f) {
        if (!f.mkdirs() && logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, LogUtils.DIR_EXISTS, f);
        }
    }




    private ArrayList<WebServiceFeature> getWebServiceFeatures(ServiceReferenceDescriptor desc) {
         /**
         * JAXWS 2.2 enables @MTOM, @Addressing @RespectBinding
         * on WebServiceRef
         * If these are present use the
         * Service(url,wsdl,features) constructor
         */
        ArrayList<WebServiceFeature> wsFeatures = new ArrayList<WebServiceFeature>();
        if (desc.isMtomEnabled()) {
            wsFeatures.add( new MTOMFeature(true,desc.getMtomThreshold()))   ;
        }
        com.sun.enterprise.deployment.Addressing add = desc.getAddressing();
        if (add != null) {
            wsFeatures.add( new AddressingFeature(
                    add.isEnabled(),add.isRequired(),getResponse(add.getResponses())))   ;
        }
        com.sun.enterprise.deployment.RespectBinding rb = desc.getRespectBinding();
        if (rb != null) {
            wsFeatures.add( new RespectBindingFeature(rb.isEnabled()))   ;
        }
        Map<Class<? extends Annotation>, Annotation> otherAnnotations =
            desc.getOtherAnnotations();
        Iterator it = otherAnnotations.values().iterator();
        while(it.hasNext()){
            wsFeatures.add(getWebServiceFeatureBean((Annotation)it.next()));
        }
        
        return wsFeatures;
    }

    private AddressingFeature.Responses getResponse(String s) {
       if (s != null) {
            return AddressingFeature.Responses.valueOf(AddressingFeature.Responses.class,s);
        } else return AddressingFeature.Responses.ALL;
        
    }

    private void resolvePortComponentLinks(ServiceReferenceDescriptor desc)
            throws Exception {

        // Resolve port component links to target endpoint address.
        // We can't assume web service client is running in same VM
        // as endpoint in the intra-app case because of app clients.
        //
        // Also set port-qname based on linked port's qname if not
        // already set.
        for(Iterator iter = desc.getPortsInfo().iterator(); iter.hasNext();) {
            ServiceRefPortInfo portInfo = (ServiceRefPortInfo) iter.next();

            if( portInfo.isLinkedToPortComponent() ) {
                WebServiceEndpoint linkedPortComponent =
                        portInfo.getPortComponentLink();

                // XXX-JD we could at this point try to figure out the
                // endpoint-address from the ejb wsdl file but it is a
                // little complicated so I will leave it for post Beta2
                if( !(portInfo.hasWsdlPort()) ) {
                    portInfo.setWsdlPort(linkedPortComponent.getWsdlPort());
                }
            }
        }
    }

    private  WebServiceFeature getWebServiceFeatureBean(Annotation a) {
        WebServiceFeatureAnnotation wsfa = a.annotationType().getAnnotation(WebServiceFeatureAnnotation.class);

        Class<? extends WebServiceFeature> beanClass = wsfa.bean();
        WebServiceFeature bean;

        Constructor ftrCtr = null;
        String[] paramNames = null;
        for (Constructor con : beanClass.getConstructors()) {
            FeatureConstructor ftrCtrAnn = (FeatureConstructor) con.getAnnotation(FeatureConstructor.class);
            if (ftrCtrAnn != null) {
                if (ftrCtr == null) {
                    ftrCtr = con;
                    paramNames = ftrCtrAnn.value();
                } else {
                    throw new WebServiceException(ModelerMessages.RUNTIME_MODELER_WSFEATURE_MORETHANONE_FTRCONSTRUCTOR(a, beanClass));
                }
            }
        }
        if (ftrCtr == null) {
            throw new WebServiceException(ModelerMessages.RUNTIME_MODELER_WSFEATURE_NO_FTRCONSTRUCTOR(a, beanClass));
        }
        if (ftrCtr.getParameterTypes().length != paramNames.length) {
            throw new WebServiceException(ModelerMessages.RUNTIME_MODELER_WSFEATURE_ILLEGAL_FTRCONSTRUCTOR(a, beanClass));
        }

        try {
            Object[] params = new Object[paramNames.length];
            for (int i = 0; i < paramNames.length; i++) {
                Method m = a.annotationType().getDeclaredMethod(paramNames[i]);
                params[i] = m.invoke(a);
            }
            bean = (WebServiceFeature) ftrCtr.newInstance(params);
        } catch (Exception e) {
            throw new WebServiceException(e);
        }

        return bean;
    }


}

