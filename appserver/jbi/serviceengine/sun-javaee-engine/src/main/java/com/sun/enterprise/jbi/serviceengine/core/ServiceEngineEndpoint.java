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

package com.sun.enterprise.jbi.serviceengine.core;
import com.sun.enterprise.jbi.serviceengine.util.soap.EndpointMetaData;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.wsdl.Definition;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;
import org.w3c.dom.Document;
import com.sun.enterprise.deployment.WebServiceEndpoint;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.web.WebModule;
import com.sun.xml.ws.api.server.WSEndpoint;

/**
 *
 * @author Manisha Umbarje
 */
public class ServiceEngineEndpoint {
    
    private Document wsdlDocument;
    private EndpointMetaData endpointMetaData;
    private ServiceEndpoint jbiEndpoint;
    private WSEndpoint wsep = null;
    private WebServiceEndpoint endpointDesc;
    private WebModule webModule;
    private String url;
    private String sei;
    private String implClass;
    private boolean jaxwsFlag;
    private boolean ejbType;
    private boolean enabled ;
    private String contextRoot;
    private QName serviceName;
    private String endpointName;
    private String applicationName;
    private ClassLoader classLoader;

    /** Creates a new instance of Endpoint */
    public ServiceEngineEndpoint(WebServiceEndpoint endpointDesc,
                                 WebModule webModule,
                                 QName serviceName,
                                 String endpointName,
                                 String implClass,
                                 String contextRoot, 
                                 boolean enabled) {
        this.serviceName = serviceName;
        this.endpointName = endpointName;
        this.endpointDesc = endpointDesc;
        this.webModule = webModule;
        setApplicationName(endpointDesc);
        this.url = endpointDesc.getEndpointAddressUri();
        this.ejbType = endpointDesc.implementedByEjbComponent();
        this.sei = endpointDesc.getServiceEndpointInterface();
        this.implClass = implClass;
        this.jaxwsFlag = endpointDesc.getWebService().getMappingFileUri() == null;
        this.enabled = enabled;
        this.contextRoot = contextRoot;    
        this.classLoader = endpointDesc.getBundleDescriptor().getClassLoader();
    }
    
    public String getServiceEndpointInterface() {
        return sei;
    }
    
    public String getURI() {
        return url;
    }
    
    public boolean isImplementedByEJB() {
        return ejbType;
    }
    
    public String getImplementationClass() {
        return implClass; 
    }
    
    public String getContextRoot() {
        return contextRoot;
    }

    public ServiceEndpoint getServiceEndpoint() {
        return jbiEndpoint;
    }
    
    public void setServiceEndpoint(ServiceEndpoint jbiEp) {
        jbiEndpoint = jbiEp;
        /*
        setServiceName(jbiEp.getServiceName());
        setEndpointName(jbiEp.getEndpointName());
         */
    }
    
    public QName getServiceName() {
        return serviceName;
    }
    
    public String getEndpointName() {
        return endpointName;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean flag) {
        enabled = flag;
    }
    public boolean isJAXWSEndpoint() {
        return jaxwsFlag;
    }
    
    public ClassLoader getClassLoader() {
        return classLoader;
    }
    
    public WebServiceEndpoint getEndpointDesc() {
        return endpointDesc;
    }

    public Document getWsdlDocument() {
        return wsdlDocument;
    }

    public void setWsdlDocument(Document wsdlDocument) {
        this.wsdlDocument = wsdlDocument;
    }
    
    public EndpointMetaData getEndpointMetaData() {
        if(endpointMetaData == null)
            initializeEndpointMetaData();
        return endpointMetaData;
    }
    
    public void setEndpointMetaData(EndpointMetaData emd) {
        this.endpointMetaData = emd;
    }

    public WSEndpoint getWsep() {
        return wsep;
    }

    public void setWsep(WSEndpoint wsep) {
        this.wsep = wsep;
    }

    public WebModule getWebModule() {
        return webModule;
    }

    public void setWebModule(WebModule webModule) {
        this.webModule = webModule;
    }
    
    private synchronized void initializeEndpointMetaData() {
        if(endpointMetaData == null) {
            try {
                EndpointMetaData emd = new EndpointMetaData(readWSDLDefinition(),
                        serviceName,
                        endpointName);
                emd.resolve();
                endpointMetaData = emd;
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    private Definition readWSDLDefinition() throws Exception {
        WSDLFactory mFactory = WSDLFactory.newInstance();
        WSDLReader mReader = mFactory.newWSDLReader();
        return (wsdlDocument!=null)?
                mReader.readWSDL(wsdlDocument.getDocumentURI(), wsdlDocument):
                mReader.readWSDL(endpointDesc.getWebService().getGeneratedWsdlFilePath());
    }
    
    public String getApplicationName() {
        return applicationName;
    }

    private void setServiceName(QName svcName) {
        serviceName = svcName;
    }
    
    private void setEndpointName(String epName) {
        endpointName = epName;
    }

    /**
     * For standalone apps get the registration name and for ear applications 
     * use the archive Uri. 
     * e.g.: 
     * 1. For web-app.war, applicationName will be 'web-app'
     * 2. For enterprise-app.ear |__ ejb.jar
     *                           |__ web.war
     *  applicationName will be 'web.war'
     */
    private void setApplicationName(WebServiceEndpoint endpointDesc) {
        BundleDescriptor bundleDescriptor = endpointDesc.getBundleDescriptor();
        this.applicationName = 
                (bundleDescriptor.getModuleDescriptor().isStandalone())?
                        bundleDescriptor.getApplication().getRegistrationName():
                        bundleDescriptor.getModuleDescriptor().getArchiveUri();
    }
}
