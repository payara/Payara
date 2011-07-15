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

package com.sun.enterprise.deployment.node.runtime;

import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.runtime.IASPersistenceManagerDescriptor;
import com.sun.enterprise.deployment.runtime.PersistenceManagerInUse;
import com.sun.enterprise.deployment.runtime.WLModuleDescriptor;
import com.sun.enterprise.deployment.runtime.common.*;
import com.sun.enterprise.deployment.runtime.connector.MapElement;
import com.sun.enterprise.deployment.runtime.connector.Principal;
import com.sun.enterprise.deployment.runtime.connector.ResourceAdapter;
import com.sun.enterprise.deployment.runtime.connector.RoleMap;
import com.sun.enterprise.deployment.runtime.web.*;
import com.sun.enterprise.deployment.runtime.web.ClassLoader;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.deployment.xml.RuntimeTagNames;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is responsible for instanciating  runtime Descriptor classes
 *
 * @author  Jerome Dochez
 * @version 
 */
public class RuntimeDescriptorFactory {

    
    static Map descriptorClasses;
    
    /** This is a factory object no need for DescriptorFactory instance */
    protected RuntimeDescriptorFactory() {
    }

    private static void initMapping() {
        descriptorClasses = new HashMap();    
	// Application
        register(new XMLElement(RuntimeTagNames.MODULE), WLModuleDescriptor.class);
	// WEB
        register(new XMLElement(RuntimeTagNames.PROPERTY), WebProperty.class);
	register(new XMLElement(RuntimeTagNames.COOKIE_PROPERTIES), CookieProperties.class);
	register(new XMLElement(RuntimeTagNames.LOCALE_CHARSET_MAP), LocaleCharsetMap.class);
	register(new XMLElement(RuntimeTagNames.LOCALE_CHARSET_INFO), LocaleCharsetInfo.class);
	register(new XMLElement(RuntimeTagNames.MANAGER_PROPERTIES), ManagerProperties.class);
	register(new XMLElement(RuntimeTagNames.SERVLET), Servlet.class);
	register(new XMLElement(RuntimeTagNames.SESSION_CONFIG), SessionConfig.class);	
	register(new XMLElement(RuntimeTagNames.SESSION_MANAGER), SessionManager.class);
	register(new XMLElement(RuntimeTagNames.JSP_CONFIG), JspConfig.class);
	register(new XMLElement(RuntimeTagNames.CACHE_MAPPING), CacheMapping.class);
	register(new XMLElement(RuntimeTagNames.CACHE_HELPER), CacheHelper.class);
	register(new XMLElement(RuntimeTagNames.CACHE), Cache.class);
	register(new XMLElement(RuntimeTagNames.CLASS_LOADER), ClassLoader.class);
	register(new XMLElement(RuntimeTagNames.STORE_PROPERTIES), StoreProperties.class);	
	register(new XMLElement(RuntimeTagNames.SESSION_PROPERTIES), SessionProperties.class);
	register(new XMLElement(RuntimeTagNames.DEFAULT_HELPER), DefaultHelper.class);
	register(new XMLElement(RuntimeTagNames.EJB_REF), EjbRef.class);
        register(new XMLElement(RuntimeTagNames.RESOURCE_REF), ResourceRef.class);
        register(new XMLElement(RuntimeTagNames.RESOURCE_ENV_REF), ResourceEnvRef.class);        
        register(new XMLElement(RuntimeTagNames.DEFAULT_RESOURCE_PRINCIPAL), DefaultResourcePrincipal.class);
        register(new XMLElement(RuntimeTagNames.CONSTRAINT_FIELD), ConstraintField.class);

        // weblogic DD
        register(new XMLElement(RuntimeTagNames.RESOURCE_DESCRIPTION), ResourceRef.class);
        register(new XMLElement(RuntimeTagNames.RESOURCE_ENV_DESCRIPTION), ResourceEnvRef.class);
        register(new XMLElement(RuntimeTagNames.EJB_REFERENCE_DESCRIPTION), EjbRef.class);
        
        // EJB
        register(new XMLElement(RuntimeTagNames.PM_DESCRIPTOR), IASPersistenceManagerDescriptor.class);
        register(new XMLElement(RuntimeTagNames.PM_INUSE), PersistenceManagerInUse.class);
        
	// connector related
	register(new XMLElement(RuntimeTagNames.PRINCIPAL), Principal.class);
	register(new XMLElement(RuntimeTagNames.BACKEND_PRINCIPAL), Principal.class);
	register(new XMLElement(RuntimeTagNames.MAP_ELEMENT), MapElement.class);
	register(new XMLElement(RuntimeTagNames.ROLE_MAP), RoleMap.class);
	register(new XMLElement(RuntimeTagNames.RESOURCE_ADAPTER), ResourceAdapter.class);	

        //common
        register(new XMLElement(RuntimeTagNames.PRINCIPAL_NAME), PrincipalNameDescriptor.class);
        register(new XMLElement(RuntimeTagNames.SECURITY_ROLE_MAPPING), SecurityRoleMapping.class);
        register(new XMLElement(RuntimeTagNames.WL_SECURITY_ROLE_ASSIGNMENT), WLSecurityRoleAssignment.class);
        register(new XMLElement(RuntimeTagNames.VALVE), Valve.class);

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
                DOLUtils.getDefaultLogger().finer("looking for " + xmlPath + " in " + descriptorClasses);
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

	if(DOLUtils.getDefaultLogger().isLoggable(Level.FINE)) {
            DOLUtils.getDefaultLogger().fine("No descriptor registered for " + s);
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
            Logger.getAnonymousLogger().log(Level.WARNING, "Error occurred", t);  
        }
        return null;
    }
            
    static {
        initMapping();
    }     
}
