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

package com.sun.enterprise.deployment;

import com.sun.enterprise.deployment.types.HandlerChainContainer;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.util.LocalStringManagerImpl;

import javax.xml.namespace.QName;
import java.io.File;
import java.net.URL;
import java.util.*;
import java.lang.annotation.Annotation;

/**
 * Information about a J2EE web service client.
 *
 * @author Kenneth Saks
 */

public class ServiceReferenceDescriptor extends EnvironmentProperty 
        implements HandlerChainContainer {

    static private final int NULL_HASH_CODE = Integer.valueOf(1).hashCode();

    private String serviceInterface;

    private String mappedName;
    
    private String wsdlFileUri;

    /**
     * Derived, non-peristent location of wsdl file.
     * Only used at deployment/runtime.
     */
    private URL wsdlFileUrl;

    private String mappingFileUri;

    /**
     * Derived, non-peristent location of mapping file.
     * Only used at deployment/runtime.
     */
    private File mappingFile;

    // Optional service name.  Only required if service-ref has WSDL and
    // the WSDL defines multiple services.
    private String serviceNamespaceUri;
    private String serviceLocalPart;
    private String serviceNameNamespacePrefix;

    // settings for both container-managed and client-managed ports
    private Set portsInfo;

    // module in which this reference is defined.
    private BundleDescriptor bundleDescriptor;

    // List of handlers associated with this service reference. 
    // Handler order is important and must be preserved.
    private LinkedList handlers;

    // The handler chains defined for this service ref (JAXWS service-ref)
    private LinkedList handlerChain;
    
    //
    // Runtime info 
    //

    private Set callProperties;

    // Name of generated service implementation class.
    private String serviceImplClassName;

    // Optional wsdl to be used at deployment instead of the wsdl packaged 
    // in module and associated with the service-ref.
    private URL wsdlOverride;
    
    // interface name of the expected injection recipient.
    // because web service reference are a bit specific (you can inject
    // the Service interface or the Port interface directly), you 
    // may need to disambiguate when loading from XML DDs.
    private String injectionTargetType=null; 

    //Support for JAXWS 2.2 features
    //@MTOM for WebserviceRef
    // Boolean instead of boolean for tri-value - true/false/not-set
    private Boolean mtomEnabled ;

    //Support for JAXWS 2.2 features
    //@RespectBinding for WebserviceRef
    private RespectBinding respectBinding;

    //Support for JAXWS 2.2 features
    //@Addressing for WebserviceRef
    private Addressing addressing;

    //Support for JAXWS 2.2 features
    //mtomThreshold
    private int mtomThreshold;

    public Map<Class<? extends Annotation>, Annotation> getOtherAnnotations() {
        return otherAnnotations;
    }

    public void setOtherAnnotations(Map<Class<? extends Annotation>, Annotation> otherAnnotations) {
        this.otherAnnotations = otherAnnotations;
    }

    /**
     * In addition to MTOM,Addressing , RespectBinding
     * pass over other annotations too.
     */

   private Map<Class<? extends Annotation>, Annotation> otherAnnotations =
            new HashMap<Class<? extends Annotation>, Annotation>();

    public boolean isRespectBindingEnabled() {
        return respectBinding.isEnabled();
    }

    public Addressing getAddressing() {
        return addressing;
    }

    public RespectBinding getRespectBinding() {
        return respectBinding;
    }

    public void setRespectBinding(RespectBinding respectBinding) {
        this.respectBinding = respectBinding;
    }

    public boolean hasMtomEnabled() {
        return mtomEnabled != null;
    }

    public boolean isMtomEnabled() {
        return mtomEnabled != null && mtomEnabled.booleanValue();
    }

    public void setMtomEnabled(boolean mtomEnabled) {
        this.mtomEnabled = Boolean.valueOf(mtomEnabled);
    }

    public boolean isAddressingEnabled() {
        return addressing.isEnabled();
    }

    public void setAddressing(Addressing addressing) {
        this.addressing = addressing;
    }


    public boolean isAddressingRequired() {
        return addressing.isRequired();
    }

    public int getMtomThreshold() {
        return mtomThreshold;
    }

    public void setMtomThreshold(int mtomThreshold) {
        this.mtomThreshold = mtomThreshold;
    }

    public ServiceReferenceDescriptor(ServiceReferenceDescriptor other) {
        super(other);
        serviceInterface = other.serviceInterface;
        mappedName = other.mappedName;
        wsdlFileUri = other.wsdlFileUri;
        wsdlFileUrl = other.wsdlFileUrl;
        addressing = other.addressing;
        mtomEnabled = other.mtomEnabled;
        respectBinding = other.respectBinding;
        mappingFileUri = other.mappingFileUri;
        mappingFile = other.mappingFile;
        serviceNamespaceUri = other.serviceNamespaceUri;
        serviceLocalPart = other.serviceLocalPart;
        serviceNameNamespacePrefix = other.serviceNameNamespacePrefix;
        otherAnnotations = other.otherAnnotations;
        portsInfo = new HashSet(); // ServiceRefPortInfo
        for (Iterator i = other.portsInfo.iterator(); i.hasNext();) {
            ServiceRefPortInfo port = new ServiceRefPortInfo(
                    (ServiceRefPortInfo)i.next());
            port.setServiceReference(this); // reset reference
            portsInfo.add(port);
        }
        handlers = new LinkedList(); // WebServiceHandler
        for (Iterator i = other.handlers.iterator(); i.hasNext();) {
            handlers.add(new WebServiceHandler
                    ((WebServiceHandler)i.next()));
        }
        handlerChain = new LinkedList(); // WebServiceHandlerChain
        for (Iterator i = other.handlerChain.iterator(); i.hasNext();) {
            handlerChain.add(new WebServiceHandlerChain((WebServiceHandlerChain)i.next()));
        }
        callProperties = new HashSet(); // NameValuePairDescriptor
        for (Iterator i = other.callProperties.iterator(); i.hasNext();) {
            callProperties.add(new NameValuePairDescriptor(
                    (NameValuePairDescriptor)i.next()));
        }
        serviceImplClassName = other.serviceImplClassName;
    }

    public ServiceReferenceDescriptor(String name, String description, 
                                      String service) {
        super(name, "", description);
        handlers = new LinkedList();
        handlerChain = new LinkedList();
        portsInfo = new HashSet();
        callProperties = new HashSet();
        serviceInterface = service;
    }
    
    public ServiceReferenceDescriptor() {
        handlers = new LinkedList();
        handlerChain = new LinkedList();
        portsInfo = new HashSet();
        callProperties = new HashSet();
    }

    public String getMappedName() {
        return mappedName;
    }
    
    public void setMappedName(String value) {
        mappedName = value;
    }
    
    public void setBundleDescriptor(BundleDescriptor bundle) {
        bundleDescriptor = bundle;
    }

    public BundleDescriptor getBundleDescriptor() {
        return bundleDescriptor;
    }

    public boolean hasGenericServiceInterface() {
        return "javax.xml.rpc.Service".equals(serviceInterface);
    }

    public boolean hasGeneratedServiceInterface() {
        return !(hasGenericServiceInterface());
    }

    public void setServiceInterface(String service) {
        serviceInterface = service;

    }

    public String getServiceInterface() {
        return serviceInterface;
    }

    public boolean hasWsdlFile() {
        return (wsdlFileUri != null && wsdlFileUri.length() > 0);
    }

    /**
     * Derived, non-peristent location of wsdl file.
     * Only used at deployment/runtime.
     */
    public void setWsdlFileUrl(URL url) {
        wsdlFileUrl = url;

    }

    public URL getWsdlFileUrl() {
        return wsdlFileUrl;
    }

    public void setWsdlFileUri(String uri) {
        if(uri.startsWith("file:")) {
            uri = uri.substring(5);
        }
        wsdlFileUri = uri;

    }

    public String getWsdlFileUri() {
        return wsdlFileUri;
    }

    public boolean hasMappingFile() {
        return (mappingFileUri != null);
    }

    /**
     * Derived, non-peristent location of mapping file.
     * Only used at deployment/runtime.
     */
    public void setMappingFile(File file) {
        mappingFile = file;

    }

    public File getMappingFile() {
        return mappingFile;
    }

    public void setMappingFileUri(String uri) {
        mappingFileUri = uri;

    }

    public String getMappingFileUri() {
        return mappingFileUri;
    }

    public void setServiceName(QName serviceName) {
        setServiceName(serviceName, null);
    }

    public void setServiceName(QName serviceName, String prefix) {
        serviceNamespaceUri = serviceName.getNamespaceURI();
        serviceLocalPart = serviceName.getLocalPart();
        serviceNameNamespacePrefix = prefix;

    }

    public void setServiceNamespaceUri(String uri) {
        serviceNamespaceUri = uri;
        serviceNameNamespacePrefix = null;

    }

    public String getServiceNamespaceUri() {
        return serviceNamespaceUri;
    }

    public void setServiceLocalPart(String localpart) {
        serviceLocalPart = localpart;
        serviceNameNamespacePrefix = null;

    }

    public String getServiceLocalPart() {
        return serviceLocalPart;
    }

    public void setServiceNameNamespacePrefix(String prefix) {
        serviceNameNamespacePrefix = prefix;

    }

    public String getServiceNameNamespacePrefix() {
        return serviceNameNamespacePrefix;
    }

    public boolean hasServiceName() {
        return ( (serviceNamespaceUri != null) && (serviceLocalPart != null) );
    }

    /**
     * @return service QName or null if either part of qname is not set
     */
    public QName getServiceName() {
        return ( hasServiceName() ? 
                 new QName(serviceNamespaceUri, serviceLocalPart) : null );
    }

    public Set getPortsInfo() {
        return portsInfo;
    }

    public void addPortInfo(ServiceRefPortInfo portInfo) {
        portInfo.setServiceReference(this);
        portsInfo.add(portInfo);

    }

    public void removePortInfo(ServiceRefPortInfo portInfo) {
        portsInfo.remove(portInfo);

    }

    /**
     * Special handling of case where runtime port info is added.
     * Ensures that port info is not duplicated when multiple
     * runtime info instances are parsed using same standard descriptor.
     */
    public void addRuntimePortInfo(ServiceRefPortInfo runtimePortInfo) {
        ServiceRefPortInfo existing = null;

        if( runtimePortInfo.hasServiceEndpointInterface() ) {
            existing = 
                getPortInfoBySEI(runtimePortInfo.getServiceEndpointInterface());
        } 
        if( (existing == null) && runtimePortInfo.hasWsdlPort() ) {
            existing = getPortInfoByPort(runtimePortInfo.getWsdlPort());
        }

        if( existing == null ) {
            if (portsInfo!=null && portsInfo.size()>0) {
                LocalStringManagerImpl localStrings =
                    new LocalStringManagerImpl(ServiceReferenceDescriptor.class);            
                DOLUtils.getDefaultLogger().warning( 
                    localStrings.getLocalString("enterprise.deployment.unknownportforruntimeinfo",
                    "Runtime port info SEI {0} is not declared in standard service-ref " + 
                    "deployment descriptors (under port-component-ref), is this intended ?", 
                    new Object[] {runtimePortInfo.getServiceEndpointInterface()}));                
            }
            addPortInfo(runtimePortInfo);
        } else {
            if( !existing.hasServiceEndpointInterface() ) {
                existing.setServiceEndpointInterface
                    (runtimePortInfo.getServiceEndpointInterface());
            }
            if( !existing.hasWsdlPort() ) {
                existing.setWsdlPort(runtimePortInfo.getWsdlPort());
            }
            for(Iterator iter = runtimePortInfo.
                    getStubProperties().iterator(); iter.hasNext();) {
                NameValuePairDescriptor next = 
                    (NameValuePairDescriptor) iter.next();
                // adds using name as key
                existing.addStubProperty(next);
            }
            for(Iterator iter = runtimePortInfo.getCallProperties()
                    .iterator(); iter.hasNext();) {
                NameValuePairDescriptor next = 
                    (NameValuePairDescriptor) iter.next();
                // adds using name as key
                existing.addCallProperty(next);
            }
            if (runtimePortInfo.getMessageSecurityBinding() != null) {
                existing.setMessageSecurityBinding(
                    runtimePortInfo.getMessageSecurityBinding());
            }
        }
    }

    public ServiceRefPortInfo addContainerManagedPort
        (String serviceEndpointInterface) {
        ServiceRefPortInfo info = new ServiceRefPortInfo();
        info.setServiceEndpointInterface(serviceEndpointInterface);
        info.setIsContainerManaged(true);
        info.setServiceReference(this);
        portsInfo.add(info);

        return info;
    }

    public boolean hasContainerManagedPorts() {
        boolean containerManaged = false;
        for(Iterator iter = portsInfo.iterator(); iter.hasNext();) {
            ServiceRefPortInfo next = (ServiceRefPortInfo) iter.next();
            if( next.isContainerManaged() ) {
                containerManaged = true;
                break;
            }
        }
        return containerManaged;
    }

    public boolean hasClientManagedPorts() {
        boolean clientManaged = false;
        for(Iterator iter = portsInfo.iterator(); iter.hasNext();) {
            ServiceRefPortInfo next = (ServiceRefPortInfo) iter.next();
            if( next.isClientManaged() ) {
                clientManaged = true;
                break;
            }
        }
        return clientManaged;
    }

    /**
     * Lookup port info by service endpoint interface.
     */
    public ServiceRefPortInfo getPortInfo(String serviceEndpointInterface) {
        return getPortInfoBySEI(serviceEndpointInterface);
    }

    /**
     * Lookup port info by service endpoint interface.
     */
    public ServiceRefPortInfo getPortInfoBySEI(String serviceEndpointInterface)
    {
        for(Iterator iter = portsInfo.iterator(); iter.hasNext();) {
            ServiceRefPortInfo next = (ServiceRefPortInfo) iter.next();
            if( serviceEndpointInterface.equals
                (next.getServiceEndpointInterface()) ) {
                return next;
            }
        }
        return null;
    }

    /**
     * Lookup port info by wsdl port.  
     */
    public ServiceRefPortInfo getPortInfoByPort(QName wsdlPort) {
        for(Iterator iter = portsInfo.iterator(); iter.hasNext();) {
            ServiceRefPortInfo next = (ServiceRefPortInfo) iter.next();
            if( next.hasWsdlPort() && wsdlPort.equals(next.getWsdlPort()) ) {
                return next;
            }
        }
        return null;
    }

    /**
     * Append handler to end of handler chain for this endpoint.
     */
    public void addHandler(WebServiceHandler handler) {
        handlers.addLast(handler);

    }

    public void removeHandler(WebServiceHandler handler) {
        handlers.remove(handler);

    }

    public void removeHandlerByName(String handlerName) {
        for(Iterator iter = handlers.iterator(); iter.hasNext();) {
            WebServiceHandler next = (WebServiceHandler) iter.next();
            if( next.getHandlerName().equals(handlerName) ) {
                iter.remove();

                break;
            }
        }
    }

    public boolean hasHandlers() {
        return (handlers.size() > 0);
    }

    /**
     * Get ordered list of WebServiceHandler handlers for this endpoint.
     */
    public LinkedList getHandlers() {
        return handlers;
    }

    /**
     * HandlerChain related setters, getters, adders, finders
     */
    public void addHandlerChain(WebServiceHandlerChain handler) {
        handlerChain.addLast(handler);

    }

    public void removeHandlerChain(WebServiceHandlerChain handler) {
        handlerChain.remove(handler);

    }

    public boolean hasHandlerChain() {
        return (handlerChain.size() > 0);
    }

    public LinkedList getHandlerChain() {
        return handlerChain;
    }

    /**
     * Runtime information.
     */

    public Set getCallProperties() {
        return callProperties;
    }

    public NameValuePairDescriptor getCallPropertyByName(String name) {
        NameValuePairDescriptor prop = null;
        for(Iterator iter = callProperties.iterator(); iter.hasNext();) {
            NameValuePairDescriptor next = (NameValuePairDescriptor) 
                iter.next();
            if( next.getName().equals(name) ) {
                prop = next;
                break;
            }
        }
        return prop;
    }

    /**
     * Add call property, using property name as a key. This will
     * replace the property value of any existing stub property with
     * the same name.
     */ 
    public void addCallProperty(NameValuePairDescriptor property) {
        NameValuePairDescriptor prop = 
            getCallPropertyByName(property.getName());
        if( prop != null ) {
            prop.setValue(property.getValue());
        } else {
            callProperties.add(property);
        }
    }

    
    /**
     * Remove call property, using property name as a key. This will
     * remove the property value of an existing stub property with
     * the matching name.
     */ 
    public void removeCallProperty(NameValuePairDescriptor property) {
        NameValuePairDescriptor prop = 
            getCallPropertyByName(property.getName());
        if( prop != null ) {
            callProperties.remove(property);
        } 
    }

    public boolean hasServiceImplClassName() {
        return (serviceImplClassName != null);
    }

    public void setServiceImplClassName(String className) {
        serviceImplClassName = className;
    }

    public String getServiceImplClassName() {
        return serviceImplClassName;
    }

    public boolean hasWsdlOverride() {
        return (wsdlOverride != null);
    }

    public void setWsdlOverride(URL override) {
        wsdlOverride = override;
    }

    public URL getWsdlOverride() {
        return wsdlOverride;
    }
    
    public void setInjectionTargetType(String type) {
        injectionTargetType = type;
    }
    
    public String getInjectionTargetType() {
        return injectionTargetType;
    }

    /* Equality on name. */
    public boolean equals(Object object) {
        if (object instanceof ServiceReferenceDescriptor) {
            ServiceReferenceDescriptor thatReference = 
                (ServiceReferenceDescriptor) object;
            return thatReference.getName().equals(this.getName());
        }
        return false;
    }

    public int hashCode() {
        int result = NULL_HASH_CODE;
        String name = getName();
        if (name != null) {
            result += name.hashCode();
        }
        return result;
    }

    public boolean isConflict(ServiceReferenceDescriptor other) {
        return (getName().equals(other.getName())) &&
            (!(
                DOLUtils.equals(getServiceInterface(), other.getServiceInterface()) &&
                DOLUtils.equals(getWsdlFileUri(), other.getWsdlFileUri()) &&
                DOLUtils.equals(getMappingFileUri(), other.getMappingFileUri()) 
                //XXX need to compare the following
                // handler
                // handle-chains
                // port-component-info
                ) ||
            isConflictResourceGroup(other));
    }
}
