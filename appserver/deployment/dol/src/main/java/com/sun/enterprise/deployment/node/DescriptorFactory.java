/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.deployment.node;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import com.sun.enterprise.deployment.AbsoluteOrderingDescriptor;
import com.sun.enterprise.deployment.AdminObject;
import com.sun.enterprise.deployment.AppListenerDescriptorImpl;
import com.sun.enterprise.deployment.AuthMechanism;
import com.sun.enterprise.deployment.AuthorizationConstraintImpl;
import com.sun.enterprise.deployment.ConnectionDefDescriptor;
import com.sun.enterprise.deployment.ConnectorConfigProperty;
import com.sun.enterprise.deployment.ConnectorDescriptor;
import com.sun.enterprise.deployment.CookieConfigDescriptor;
import com.sun.enterprise.deployment.EntityManagerFactoryReferenceDescriptor;
import com.sun.enterprise.deployment.EntityManagerReferenceDescriptor;
import com.sun.enterprise.deployment.EnvironmentProperty;
import com.sun.enterprise.deployment.ErrorPageDescriptor;
import com.sun.enterprise.deployment.InboundResourceAdapter;
import com.sun.enterprise.deployment.JspConfigDescriptor;
import com.sun.enterprise.deployment.JspGroupDescriptor;
import com.sun.enterprise.deployment.LicenseDescriptor;
import com.sun.enterprise.deployment.LocaleEncodingMappingDescriptor;
import com.sun.enterprise.deployment.LocaleEncodingMappingListDescriptor;
import com.sun.enterprise.deployment.LoginConfigurationImpl;
import com.sun.enterprise.deployment.MessageListener;
import com.sun.enterprise.deployment.MimeMappingDescriptor;
import com.sun.enterprise.deployment.MultipartConfigDescriptor;
import com.sun.enterprise.deployment.NameValuePairDescriptor;
import com.sun.enterprise.deployment.OrderingDescriptor;
import com.sun.enterprise.deployment.OrderingOrderingDescriptor;
import com.sun.enterprise.deployment.OutboundResourceAdapter;
import com.sun.enterprise.deployment.PersistenceUnitDescriptor;
import com.sun.enterprise.deployment.SecurityConstraintImpl;
import com.sun.enterprise.deployment.SecurityPermission;
import com.sun.enterprise.deployment.ServiceReferenceDescriptor;
import com.sun.enterprise.deployment.ServletFilterDescriptor;
import com.sun.enterprise.deployment.ServletFilterMappingDescriptor;
import com.sun.enterprise.deployment.SessionConfigDescriptor;
import com.sun.enterprise.deployment.TagLibConfigurationDescriptor;
import com.sun.enterprise.deployment.UserDataConstraintImpl;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.WebComponentDescriptor;
import com.sun.enterprise.deployment.WebFragmentDescriptor;
import com.sun.enterprise.deployment.WebResourceCollectionImpl;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.deployment.xml.ConnectorTagNames;
import com.sun.enterprise.deployment.xml.PersistenceTagNames;
import com.sun.enterprise.deployment.xml.RuntimeTagNames;
import com.sun.enterprise.deployment.xml.TagNames;
import com.sun.enterprise.deployment.xml.WebServicesTagNames;
import com.sun.enterprise.deployment.xml.WebTagNames;

/**
 * This class is responsible for instantiating  Descriptor classes
 *
 * @author  Jerome Dochez
 * @version 
 */
public class DescriptorFactory {

    static Map descriptorClasses;
    
    /** This is a factory object no need for DescriptorFactory instance */
    protected DescriptorFactory() {
    }

    private static void initMapping() {
        descriptorClasses = new HashMap();

        // Application
        register(new XMLElement(RuntimeTagNames.APPLICATION_PARAM), EnvironmentProperty.class);        

	//connector
	register(new XMLElement(ConnectorTagNames.CONNECTOR), ConnectorDescriptor.class);
	register(new XMLElement(ConnectorTagNames.OUTBOUND_RESOURCE_ADAPTER), OutboundResourceAdapter.class);  
	register(new XMLElement(ConnectorTagNames.INBOUND_RESOURCE_ADAPTER), InboundResourceAdapter.class);
	register(new XMLElement(ConnectorTagNames.RESOURCE_ADAPTER), OutboundResourceAdapter.class);
	register(new XMLElement(ConnectorTagNames.AUTH_MECHANISM), AuthMechanism.class);
	register(new XMLElement(ConnectorTagNames.SECURITY_PERMISSION), SecurityPermission.class);
	register(new XMLElement(ConnectorTagNames.LICENSE), LicenseDescriptor.class);
	register(new XMLElement(ConnectorTagNames.CONFIG_PROPERTY), ConnectorConfigProperty.class);
	register(new XMLElement(ConnectorTagNames.REQUIRED_CONFIG_PROP), ConnectorConfigProperty.class);
	register(new XMLElement(ConnectorTagNames.MSG_LISTENER), MessageListener.class);
	register(new XMLElement(ConnectorTagNames.ACTIVATION_SPEC),MessageListener.class);
	register(new XMLElement(ConnectorTagNames.ADMIN_OBJECT), AdminObject.class);
	register(new XMLElement(ConnectorTagNames.CONNECTION_DEFINITION), ConnectionDefDescriptor.class);

	//web stuff
        register(new XMLElement(WebTagNames.WEB_BUNDLE), WebBundleDescriptor.class);
        register(new XMLElement(WebTagNames.WEB_FRAGMENT), WebFragmentDescriptor.class);
        register(new XMLElement(WebTagNames.SERVLET), WebComponentDescriptor.class);
        register(new XMLElement(WebTagNames.INIT_PARAM), EnvironmentProperty.class);        
        register(new XMLElement(WebTagNames.MIME_MAPPING), MimeMappingDescriptor.class);
        register(new XMLElement(WebTagNames.CONTEXT_PARAM), EnvironmentProperty.class);                
        register(new XMLElement(WebTagNames.SESSION_CONFIG), SessionConfigDescriptor.class);                
        register(new XMLElement(WebTagNames.COOKIE_CONFIG), CookieConfigDescriptor.class);                
        register(new XMLElement(WebTagNames.SECURITY_CONSTRAINT), SecurityConstraintImpl.class);
        register(new XMLElement(WebTagNames.USERDATA_CONSTRAINT), UserDataConstraintImpl.class);     
        register(new XMLElement(WebTagNames.AUTH_CONSTRAINT), AuthorizationConstraintImpl.class);
        register(new XMLElement(WebTagNames.WEB_RESOURCE_COLLECTION), WebResourceCollectionImpl.class);
        register(new XMLElement(WebTagNames.LISTENER), AppListenerDescriptorImpl.class);    
        register(new XMLElement(WebTagNames.FILTER), ServletFilterDescriptor.class);            
        register(new XMLElement(WebTagNames.FILTER_MAPPING), ServletFilterMappingDescriptor.class);    
        register(new XMLElement(WebTagNames.ERROR_PAGE), ErrorPageDescriptor.class);            
        register(new XMLElement(WebTagNames.LOGIN_CONFIG), LoginConfigurationImpl.class);
        register(new XMLElement(WebTagNames.TAGLIB), TagLibConfigurationDescriptor.class);            
        register(new XMLElement(WebTagNames.JSPCONFIG), JspConfigDescriptor.class);            
        register(new XMLElement(WebTagNames.JSP_GROUP), JspGroupDescriptor.class);            
        register(new XMLElement(WebTagNames.LOCALE_ENCODING_MAPPING_LIST), LocaleEncodingMappingListDescriptor.class);            
        register(new XMLElement(WebTagNames.LOCALE_ENCODING_MAPPING), LocaleEncodingMappingDescriptor.class);                         
        register(new XMLElement(WebTagNames.ABSOLUTE_ORDERING), AbsoluteOrderingDescriptor.class);                         
        register(new XMLElement(WebTagNames.ORDERING), OrderingDescriptor.class);                         
        register(new XMLElement(WebTagNames.AFTER), OrderingOrderingDescriptor.class);                         
        register(new XMLElement(WebTagNames.BEFORE), OrderingOrderingDescriptor.class);                         
        register(new XMLElement(WebTagNames.MULTIPART_CONFIG), MultipartConfigDescriptor.class);                         

        // JSR 109 integration
        register(new XMLElement(WebServicesTagNames.SERVICE_REF),ServiceReferenceDescriptor.class);
        register(new XMLElement(WebServicesTagNames.PORT_INFO),
               com.sun.enterprise.deployment.ServiceRefPortInfo.class);
        register(new XMLElement(WebServicesTagNames.STUB_PROPERTY),
                 NameValuePairDescriptor.class);
        register(new XMLElement(WebServicesTagNames.CALL_PROPERTY),
                 NameValuePairDescriptor.class);
		 
        // persistence.xsd related entries (JSR 220)
        // Note we do not register PersistenceUnitsDescriptor, because that
        // is created by PersistenceDeploymentDescriptorFile.getRootXMLNode().
        register(new XMLElement(PersistenceTagNames.PERSISTENCE_UNIT),
                 PersistenceUnitDescriptor.class);
        register(new XMLElement(TagNames.PERSISTENCE_CONTEXT_REF),
                 EntityManagerReferenceDescriptor.class);
        register(new XMLElement(TagNames.PERSISTENCE_UNIT_REF),
                 EntityManagerFactoryReferenceDescriptor.class);
    }
    
    /**
     * register a new descriptor class handling a particular XPATH in the DTD. 
     *
     * @param xmlPath absolute or relative XPath
     * @param clazz the descriptor class to use
     */
    public static void register(XMLElement  xmlPath, Class clazz) {
        if (DOLUtils.getDefaultLogger().isLoggable(Level.FINE)) {        
            DOLUtils.getDefaultLogger().fine("Register " + clazz + " to handle " + xmlPath.getQName());
        }
	descriptorClasses.put(xmlPath.getQName(), clazz);
    }
    
    /**
     * @return the descriptor tag for a particular XPath
     */
    public static Class getDescriptorClass(String xmlPath) {
        String s = xmlPath;        
        do {
            if (DOLUtils.getDefaultLogger().isLoggable(Level.FINER)) {            
                DOLUtils.getDefaultLogger().finer("looking for " + xmlPath);
            }
            if (descriptorClasses.containsKey(xmlPath)) {
                return (Class) descriptorClasses.get(xmlPath);            
            }
            if (xmlPath.indexOf('/')!=-1) {
                xmlPath = xmlPath.substring(xmlPath.indexOf('/')+1);
            } else {
                xmlPath=null;
            }            
        } while (xmlPath!=null);
	if(DOLUtils.getDefaultLogger().isLoggable(Level.SEVERE)) {
            DOLUtils.getDefaultLogger().log(Level.SEVERE, "enterprise.deployment.backend.invalidDescriptorMappingFailure",
                new Object[] {"No descriptor registered for " + s});
	}
        return null;
    }
    
    /**
     * @return a new instance of a registered descriptor class for the 
     * supplied XPath
     */
    public static Object  getDescriptor(String xmlPath) {        
        
        try {
            Class c = getDescriptorClass(xmlPath);
	    if (c!=null) {
                return c.newInstance();
            }
        } catch (Throwable t) {
            DOLUtils.getDefaultLogger().log(Level.WARNING, "Error occurred", t);
        }
        return null;
    }
            
    static {
        initMapping();
    }
}
