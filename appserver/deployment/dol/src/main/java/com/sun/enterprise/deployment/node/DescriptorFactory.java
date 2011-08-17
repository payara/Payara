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

package com.sun.enterprise.deployment.node;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.deployment.util.DOLUtils;
import org.glassfish.deployment.common.ModuleDescriptor;
import com.sun.enterprise.deployment.xml.*;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * This class is responsible for instanciating  Descriptor classes
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
        
	//EJB
        register(new XMLElement(EjbTagNames.EJB_BUNDLE_TAG), EjbBundleDescriptor.class);
        register(new XMLElement(EjbTagNames.SESSION), EjbSessionDescriptor.class);       
        register(new XMLElement(EjbTagNames.ENTITY), EjbEntityDescriptor.class);     
        register(new XMLElement(EjbTagNames.MESSAGE_DRIVEN), EjbMessageBeanDescriptor.class);        
        register(new XMLElement(EjbTagNames.ACTIVATION_CONFIG), 
                 ActivationConfigDescriptor.class);        
        register(new XMLElement(TagNames.EJB_REFERENCE), EjbReferenceDescriptor.class);       
        register(new XMLElement(TagNames.EJB_LOCAL_REFERENCE), EjbReferenceDescriptor.class);                
        register(new XMLElement(TagNames.ROLE), SecurityRoleDescriptor.class);
        register(new XMLElement(EjbTagNames.EXCLUDE_LIST), MethodPermissionDescriptor.class);        
        register(new XMLElement(EjbTagNames.RESOURCE_REFERENCE), ResourceReferenceDescriptor.class);
        register(new XMLElement(EjbTagNames.CMP_FIELD), FieldDescriptor.class);
        register(new XMLElement(EjbTagNames.METHOD), MethodDescriptor.class);          
        register(new XMLElement(EjbTagNames.METHOD_PERMISSION), MethodPermissionDescriptor.class);
        register(new XMLElement(EjbTagNames.RUNAS_SPECIFIED_IDENTITY), RunAsIdentityDescriptor.class);
        register(new XMLElement(TagNames.ENVIRONMENT_PROPERTY), EnvironmentProperty.class);
        register(new XMLElement(EjbTagNames.ROLE_REFERENCE), RoleReference.class);
        register(new XMLElement(EjbTagNames.QUERY), QueryDescriptor.class);
        register(new XMLElement(EjbTagNames.QUERY_METHOD), MethodDescriptor.class);    
        register(new XMLElement(RuntimeTagNames.JAVA_METHOD), MethodDescriptor.class);
        register(new XMLElement(TagNames.RESOURCE_ENV_REFERENCE), JmsDestinationReferenceDescriptor.class);
        register(new XMLElement(TagNames.MESSAGE_DESTINATION_REFERENCE), MessageDestinationReferenceDescriptor.class);
        register(new XMLElement(EjbTagNames.EJB_RELATION), RelationshipDescriptor.class);
        register(new XMLElement(EjbTagNames.EJB_RELATIONSHIP_ROLE), RelationRoleDescriptor.class);
        register(new XMLElement(EjbTagNames.AROUND_INVOKE_METHOD), LifecycleCallbackDescriptor.class);
       register(new XMLElement(TagNames.POST_CONSTRUCT), LifecycleCallbackDescriptor.class);
       register(new XMLElement(TagNames.PRE_DESTROY), LifecycleCallbackDescriptor.class);
       register(new XMLElement(EjbTagNames.POST_ACTIVATE_METHOD), LifecycleCallbackDescriptor.class);
       register(new XMLElement(EjbTagNames.PRE_PASSIVATE_METHOD), LifecycleCallbackDescriptor.class);
       register(new XMLElement(EjbTagNames.TIMEOUT_METHOD), MethodDescriptor.class);
       register(new XMLElement(EjbTagNames.INIT_BEAN_METHOD), MethodDescriptor.class);
       register(new XMLElement(EjbTagNames.INIT_CREATE_METHOD), MethodDescriptor.class);
       register(new XMLElement(EjbTagNames.INIT_METHOD), EjbInitInfo.class);
       register(new XMLElement(EjbTagNames.REMOVE_METHOD), EjbRemovalInfo.class);
       register(new XMLElement(EjbTagNames.INTERCEPTOR), EjbInterceptor.class);
       register(new XMLElement(EjbTagNames.INTERCEPTOR_BINDING), 
                InterceptorBindingDescriptor.class);
       register(new XMLElement(EjbTagNames.APPLICATION_EXCEPTION), 
                EjbApplicationExceptionInfo.class);
        register(new XMLElement(EjbTagNames.STATEFUL_TIMEOUT), TimeoutValueDescriptor.class);
        register(new XMLElement(EjbTagNames.TIMER_SCHEDULE), ScheduledTimerDescriptor.class);
        register(new XMLElement(EjbTagNames.AFTER_BEGIN_METHOD), MethodDescriptor.class);
        register(new XMLElement(EjbTagNames.AFTER_COMPLETION_METHOD), MethodDescriptor.class);
        register(new XMLElement(EjbTagNames.BEFORE_COMPLETION_METHOD), MethodDescriptor.class);
        register(new XMLElement(EjbTagNames.CONCURRENT_METHOD), MethodDescriptor.class);
        register(new XMLElement(EjbTagNames.CONCURRENT_ACCESS_TIMEOUT), TimeoutValueDescriptor.class);
        register(new XMLElement(EjbTagNames.ASYNC_METHOD), MethodDescriptor.class);
        
        


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
        register(new XMLElement(WebServicesTagNames.SERVICE_REF), ServiceReferenceDescriptor.class);
        register(new XMLElement(WebServicesTagNames.WEB_SERVICE), WebService.class);
        register(new XMLElement(WebServicesTagNames.PORT_COMPONENT), com.sun.enterprise.deployment.WebServiceEndpoint.class);
        register(new XMLElement(WebServicesTagNames.HANDLER), 
                 com.sun.enterprise.deployment.WebServiceHandler.class);
        register(new XMLElement(WebServicesTagNames.ADDRESSING),
                 com.sun.enterprise.deployment.Addressing.class);
        register(new XMLElement(WebServicesTagNames.RESPECT_BINDING),
                 com.sun.enterprise.deployment.RespectBinding.class);
        register(new XMLElement(WebServicesTagNames.HANDLER_CHAIN), 
                 com.sun.enterprise.deployment.WebServiceHandlerChain.class);
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
