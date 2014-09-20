/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.deployment.runtime.common.MessageSecurityBindingDescriptor;
import org.glassfish.deployment.common.Descriptor;

import javax.xml.namespace.QName;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

/**
 * Information about a single WSDL port or port type in a service reference.
 *
 * @author Kenneth Saks
 */

public class ServiceRefPortInfo extends Descriptor {

    private String serviceEndpointInterface;

    private boolean containerManaged;

    private String portComponentLinkName;
    private WebServiceEndpoint portComponentLink;

    // Service reference with which this port info is associated.
    private ServiceReferenceDescriptor serviceRef;

    //
    // Runtime info
    //

    private QName wsdlPort;

    // Set of name/value pairs corresponding to JAXRPC Stub properties.
    private Set stubProperties;

    // Set of name/value pairs corresponding to JAXRPC Call properties.
    private Set callProperties;

    // Target endpoint address of linked port component.  This is derived
    // and set at runtime.  There is no element for it in sun-j2ee-ri.xml
    private String targetEndpointAddress;

    // message-security-binding
    private MessageSecurityBindingDescriptor messageSecBindingDesc = null;

    private String mtomEnabled = null;

    public ServiceRefPortInfo(ServiceRefPortInfo other) {
	super(other);
	serviceEndpointInterface = other.serviceEndpointInterface;
	containerManaged = other.containerManaged;
	portComponentLinkName = other.portComponentLinkName;
	portComponentLink = other.portComponentLink; // copy as-is
	serviceRef = other.serviceRef; // copy as-is
	wsdlPort = other.wsdlPort; // copy as-is
        mtomEnabled = other.mtomEnabled;

        stubProperties = new HashSet();
	for (Iterator i = other.stubProperties.iterator(); i.hasNext();) {
	    stubProperties.add(new NameValuePairDescriptor
                ((NameValuePairDescriptor)i.next()));
	}

        callProperties = new HashSet(); // NameValuePairDescriptor
	for (Iterator i = other.callProperties.iterator(); i.hasNext();) {
	    callProperties.add(new NameValuePairDescriptor(
		(NameValuePairDescriptor)i.next()));
	}

	targetEndpointAddress = other.targetEndpointAddress; 
    }

    public ServiceRefPortInfo() {
        stubProperties = new HashSet();
        callProperties = new HashSet();
        containerManaged = false;
    }

    public void setServiceReference(ServiceReferenceDescriptor desc) {
        serviceRef = desc;
    }

    public ServiceReferenceDescriptor getServiceReference() {
        return serviceRef;
    }

    public boolean hasServiceEndpointInterface() {
        return (serviceEndpointInterface != null);
    }

    public void setServiceEndpointInterface(String sei) {
        serviceEndpointInterface = sei;
    }

    public String getServiceEndpointInterface() {
        return serviceEndpointInterface;
    }

    public void setIsContainerManaged(boolean flag) {
        containerManaged = flag;
    }

    public boolean isContainerManaged() {
        return containerManaged;
    }

    public boolean isClientManaged() {
        return !containerManaged;
    }

    /** 
     * Sets the name of the port component to which I refer.
     * NOTE : Does *NOT* attempt to resolve link name.  Use 
     * overloaded version or resolveLink if link resolution 
     * is required.
     */
    public void setPortComponentLinkName(String linkName) {
        setPortComponentLinkName(linkName, false);
    }

    public WebServiceEndpoint setPortComponentLinkName(String linkName, 
                                                       boolean resolve) {
        portComponentLinkName = linkName;

        return resolve ? resolveLinkName() : null;
    }
    
    public boolean hasPortComponentLinkName() {
        return (portComponentLinkName != null);
    }

    public String getPortComponentLinkName() {
        return portComponentLinkName;
    }

    public void setMessageSecurityBinding(
       MessageSecurityBindingDescriptor messageSecBindingDesc) {
       this.messageSecBindingDesc = messageSecBindingDesc;
    }

    public MessageSecurityBindingDescriptor getMessageSecurityBinding() {
        return messageSecBindingDesc;
    }

    /**
     *@return true only if there is a port component link AND it has been
     * resolved to a valid port component within the application.
     */
    public boolean isLinkedToPortComponent() {
        return (portComponentLinkName != null ) && (portComponentLink != null);
    }

    /** 
     * Try to resolve the current link name value to a WebServiceEndpoint
     * object.
     *
     * @return WebServiceEndpoint to which link was resolved, or null if 
     * link name resolution failed.
     */
    public WebServiceEndpoint resolveLinkName() {

        WebServiceEndpoint port = null;
        String linkName = portComponentLinkName;

        if( (linkName != null) && (linkName.length() > 0) ) {
            int hashIndex = linkName.indexOf('#');
            boolean absoluteLink = (hashIndex != -1);

            BundleDescriptor bundleDescriptor = getBundleDescriptor();
            Application app = bundleDescriptor.getApplication();
            BundleDescriptor targetBundle = bundleDescriptor;
            String portName = linkName;
            
            if( (app != null) && absoluteLink ) {
                // Resolve <module>#<port-component-name> style link
                String relativeModuleUri = linkName.substring(0, hashIndex);
                portName = linkName.substring(hashIndex + 1);
                targetBundle = app.getRelativeBundle(bundleDescriptor,
                                                     relativeModuleUri);
            }

            // targetBundle will only be null here if module lookup for
            // absolute link failed.
            if( targetBundle != null ) {
                LinkedList bundles = new LinkedList();
                bundles.addFirst(targetBundle);
                if( (app != null) && !absoluteLink ) {
                    bundles.addAll(app.getBundleDescriptors());
                }
                for(Iterator iter = bundles.iterator(); iter.hasNext();) {
                    BundleDescriptor next = (BundleDescriptor) iter.next();
                    port = next.getWebServiceEndpointByName(portName);
                    if( port != null ) {
                        setPortComponentLink(port);
                        break;
                    }
                }
            }
        }

        return port;
    }

    public WebServiceEndpoint getPortComponentLink() {
        return portComponentLink;
    }

    /**
     * @param portComponenet the port component to which I refer
     */
    public void setPortComponentLink(WebServiceEndpoint newPort) {
        if( newPort != null ) {

            // Keep port component link name in synch with port component
            // object.
            BundleDescriptor bundleDescriptor = getBundleDescriptor();
            BundleDescriptor targetBundleDescriptor = 
                newPort.getBundleDescriptor();
            String linkName = newPort.getEndpointName();
            if( bundleDescriptor != targetBundleDescriptor ) {
                Application app = bundleDescriptor.getApplication();
                String relativeUri = app.getRelativeUri(bundleDescriptor,
                                                        targetBundleDescriptor);
                linkName = relativeUri + "#" + linkName;
            }
            portComponentLinkName = linkName;
        }
        portComponentLink = newPort;
    }

    private BundleDescriptor getBundleDescriptor() {
        return serviceRef.getBundleDescriptor();
    }

    //
    // Runtime info
    //
    
    public boolean hasWsdlPort() {
        return (wsdlPort != null);
    }

    public void setWsdlPort(QName port) {
        wsdlPort = port;
    }

    public QName getWsdlPort() {
        return wsdlPort;
    }

    // Set of NameValuePairDescriptor objects for each stub property.
    public Set getStubProperties() {
        return stubProperties;
    }

    public boolean hasStubProperty(String name) {
        return (getStubPropertyValue(name) != null);
    }

    public String getStubPropertyValue(String name) {
        String value = null;
        for(Iterator iter = stubProperties.iterator(); iter.hasNext();) {
            NameValuePairDescriptor next = (NameValuePairDescriptor)iter.next();
            if( next.getName().equals(name) ) {
                value = next.getValue();
                break;
            }
        }
        return value;
    }

    public NameValuePairDescriptor getStubPropertyByName(String name) {
        NameValuePairDescriptor prop = null;
        for(Iterator iter = stubProperties.iterator(); iter.hasNext();) {
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
     * Add stub property, using property name as a key. This will
     * replace the property value of any existing stub property with
     * the same name.
     */ 
    public void addStubProperty(NameValuePairDescriptor property) {
        NameValuePairDescriptor prop = 
            getStubPropertyByName(property.getName());
        if( prop != null ) {
            prop.setValue(property.getValue());
        } else {
            stubProperties.add(property);
        }
    }
    
     /**
     * Remove stub property, using property name as a key. This will
     * remove the property value of an existing stub property with
     * the matching name.
     */ 
    public void removeStubProperty(NameValuePairDescriptor property) {
        NameValuePairDescriptor prop = 
            getStubPropertyByName(property.getName());
        if (prop != null) {
            stubProperties.remove(property);
        }
    }

    /**
     * Add stub property, using property name as a key. This will
     * replace the property value of any existing stub property with
     * the same name.
     */ 
    public void addStubProperty(String name, String value) {
        NameValuePairDescriptor nvPair = new NameValuePairDescriptor();
        nvPair.setName(name);
        nvPair.setValue(value);
        addStubProperty(nvPair);
    }

    public Set getCallProperties() {
        return callProperties;
    }

    public boolean hasCallProperty(String name) {
        return (getCallPropertyByName(name) != null);
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

    public boolean hasTargetEndpointAddress() {
        return (targetEndpointAddress != null);
    }

    public void setTargetEndpointAddress(String address) {
        targetEndpointAddress = address;
    }

    public String getTargetEndpointAddress() {
        return targetEndpointAddress;
    }

    public void setMtomEnabled(String value) {
        mtomEnabled = value;
    }
    
    public String getMtomEnabled() {
        return mtomEnabled;
    }
}
