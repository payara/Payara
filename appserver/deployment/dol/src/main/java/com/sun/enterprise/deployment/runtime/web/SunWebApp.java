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

package com.sun.enterprise.deployment.runtime.web;

import com.sun.enterprise.deployment.runtime.common.EjbRef;
import com.sun.enterprise.deployment.runtime.common.ResourceEnvRef;
import com.sun.enterprise.deployment.runtime.common.ResourceRef;
import com.sun.enterprise.deployment.runtime.common.SecurityRoleMapping;
import com.sun.enterprise.deployment.runtime.common.WLSecurityRoleAssignment;

// BEGIN_NOI18N

public class SunWebApp extends WebPropertyContainer
{
    
    static public final String SECURITY_ROLE_MAPPING = "SecurityRoleMapping";	// NOI18N
    static public final String WL_SECURITY_ROLE_ASSIGNMENT = "WLSecurityRoleAssignment";	// NOI18N
    static public final String SERVLET = "Servlet";	// NOI18N
    static public final String SESSION_CONFIG = "SessionConfig";	// NOI18N
    static public final String RESOURCE_ENV_REF = "ResourceEnvRef";	// NOI18N
    static public final String RESOURCE_REF = "ResourceRef";	// NOI18N
    static public final String EJB_REF = "EjbRef";	// NOI18N
    static public final String CACHE = "Cache";	// NOI18N
    static public final String CLASS_LOADER = "ClassLoader";	// NOI18N
    static public final String JSP_CONFIG = "JspConfig";	// NOI18N
    static public final String LOCALE_CHARSET_INFO = "LocaleCharsetInfo";	// NOI18N
    static public final String PARAMETER_ENCODING = "ParameterEncoding";
    static public final String FORM_HINT_FIELD = "FormHintField";    
    static public final String DEFAULT_CHARSET = "DefaultCharset";
    public static final String IDEMPOTENT_URL_PATTERN = "IdempotentUrlPattern";
    public static final String ERROR_URL = "ErrorUrl";
    public static final String HTTPSERVLET_SECURITY_PROVIDER = "HttpServletSecurityProvider";
    public static final String VALVE = "Valve";
    
    public SunWebApp()
    {
	// set default values
	setAttributeValue(CACHE, "MaxEntries", "4096");
	setAttributeValue(CACHE, "TimeoutInSeconds", "30");
	setAttributeValue(CACHE, "Enabled", "false");
    }
    
    // This attribute is an array, possibly empty
    public void setSecurityRoleMapping(int index, SecurityRoleMapping value)
    {
	this.setValue(SECURITY_ROLE_MAPPING, index, value);
    }
    
    //
    public SecurityRoleMapping getSecurityRoleMapping(int index)
    {
	return (SecurityRoleMapping)this.getValue(SECURITY_ROLE_MAPPING, index);
    }
    
    // This attribute is an array, possibly empty
    public void setSecurityRoleMapping(SecurityRoleMapping[] value)
    {
	this.setValue(SECURITY_ROLE_MAPPING, value);
    }
    
    //
    public SecurityRoleMapping[] getSecurityRoleMapping()
    {
	return (SecurityRoleMapping[])this.getValues(SECURITY_ROLE_MAPPING);
    }
    
    // Return the number of properties
    public int sizeSecurityRoleMapping()
    {
	return this.size(SECURITY_ROLE_MAPPING);
    }
    
    // Add a new element returning its index in the list
    public int addSecurityRoleMapping(SecurityRoleMapping value)
    {
	return this.addValue(SECURITY_ROLE_MAPPING, value);
    }
    
    //
    // Remove an element using its reference
    // Returns the index the element had in the list
    //
    public int removeSecurityRoleMapping(SecurityRoleMapping value)
    {
	return this.removeValue(SECURITY_ROLE_MAPPING, value);
    }

    // This attribute is an array, possibly empty
    public void setWLSecurityRoleAssignment(int index, WLSecurityRoleAssignment value)
    {
	this.setValue(WL_SECURITY_ROLE_ASSIGNMENT, index, value);
    }

    //
    public WLSecurityRoleAssignment getWLSecurityRoleAssignment(int index)
    {
	return (WLSecurityRoleAssignment)this.getValue(WL_SECURITY_ROLE_ASSIGNMENT, index);
    }

    // This attribute is an array, possibly empty
    public void setWLSecurityRoleAssignment(WLSecurityRoleAssignment[] value)
    {
	this.setValue(WL_SECURITY_ROLE_ASSIGNMENT, value);
    }

    //
    public WLSecurityRoleAssignment[] getWLSecurityRoleAssignment()
    {
	return (WLSecurityRoleAssignment[])this.getValues(WL_SECURITY_ROLE_ASSIGNMENT);
    }

    // Return the number of properties
    public int sizeWLSecurityRoleAssignment()
    {
	return this.size(WL_SECURITY_ROLE_ASSIGNMENT);
    }

    // Add a new element returning its index in the list
    public int addWLSecurityRoleAssignment(WLSecurityRoleAssignment value)
    {
	return this.addValue(WL_SECURITY_ROLE_ASSIGNMENT, value);
    }

    //
    // Remove an element using its reference
    // Returns the index the element had in the list
    //
    public int removeWLSecurityRoleAssignment(WLSecurityRoleAssignment value)
    {
	return this.removeValue(WL_SECURITY_ROLE_ASSIGNMENT, value);
    }
    
    // This attribute is an array, possibly empty
    public void setServlet(int index, Servlet value)
    {
	this.setValue(SERVLET, index, value);
    }
    
    //
    public Servlet getServlet(int index)
    {
	return (Servlet)this.getValue(SERVLET, index);
    }
    
    // This attribute is an array, possibly empty
    public void setServlet(Servlet[] value)
    {
	this.setValue(SERVLET, value);
    }
    
    //
    public Servlet[] getServlet()
    {
	return (Servlet[])this.getValues(SERVLET);
    }
    
    // Return the number of properties
    public int sizeServlet()
    {
	return this.size(SERVLET);
    }
    
    // Add a new element returning its index in the list
    public int addServlet(Servlet value)
    {
	return this.addValue(SERVLET, value);
    }
    
    //
    // Remove an element using its reference
    // Returns the index the element had in the list
    //
    public int removeServlet(Servlet value)
    {
	return this.removeValue(SERVLET, value);
    }
    
    // This attribute is an array, possibly empty
    public void setIdempotentUrlPattern(int index, IdempotentUrlPattern value)
    {
        this.setValue(IDEMPOTENT_URL_PATTERN, index, value);
    }

    //
    public  IdempotentUrlPattern getIdempotentUrlPattern(int index)
    {
        return (IdempotentUrlPattern)this.getValue(IDEMPOTENT_URL_PATTERN, index);
    }

    // This attribute is an array, possibly empty
    public void setIdempotentUrlPatterns(IdempotentUrlPattern[] value)
    {
        this.setValue(IDEMPOTENT_URL_PATTERN, value);
    }

    //
    public IdempotentUrlPattern[] getIdempotentUrlPatterns()
    {
        return (IdempotentUrlPattern[])this.getValues(IDEMPOTENT_URL_PATTERN);
    }

    // Return the number of properties
    public int sizeIdempotentUrlPattern()
    {
        return this.size(IDEMPOTENT_URL_PATTERN);
    }

    // Add a new element returning its index in the list
    public int addIdempotentUrlPattern(IdempotentUrlPattern value)
    {
        return this.addValue(IDEMPOTENT_URL_PATTERN, value);
    }

    // Remove an element using its reference
    // Returns the index the element had in the list
    //
    public int removeIdempotentUrlPattern(IdempotentUrlPattern value)
    {
        return this.removeValue(IDEMPOTENT_URL_PATTERN, value);
    }

    // This attribute is optional
    public void setSessionConfig(SessionConfig value)
    {
	this.setValue(SESSION_CONFIG, value);
    }
    
    //
    public SessionConfig getSessionConfig()
    {
	return (SessionConfig)this.getValue(SESSION_CONFIG);
    }
    
    // This attribute is an array, possibly empty
    public void setResourceEnvRef(int index, ResourceEnvRef value)
    {
	this.setValue(RESOURCE_ENV_REF, index, value);
    }
    
    //
    public ResourceEnvRef getResourceEnvRef(int index)
    {
	return (ResourceEnvRef)this.getValue(RESOURCE_ENV_REF, index);
    }
    
    // This attribute is an array, possibly empty
    public void setResourceEnvRef(ResourceEnvRef[] value)
    {
	this.setValue(RESOURCE_ENV_REF, value);
    }
    
    //
    public ResourceEnvRef[] getResourceEnvRef()
    {
	return (ResourceEnvRef[])this.getValues(RESOURCE_ENV_REF);
    }
    
    // Return the number of properties
    public int sizeResourceEnvRef()
    {
	return this.size(RESOURCE_ENV_REF);
    }
    
    // Add a new element returning its index in the list
    public int addResourceEnvRef(ResourceEnvRef value)
    {
	return this.addValue(RESOURCE_ENV_REF, value);
    }
    
    //
    // Remove an element using its reference
    // Returns the index the element had in the list
    //
    public int removeResourceEnvRef(ResourceEnvRef value)
    {
	return this.removeValue(RESOURCE_ENV_REF, value);
    }
    
    // This attribute is an array, possibly empty
    public void setResourceRef(int index, ResourceRef value)
    {
	this.setValue(RESOURCE_REF, index, value);
    }
    
    //
    public ResourceRef getResourceRef(int index)
    {
	return (ResourceRef)this.getValue(RESOURCE_REF, index);
    }
    
    // This attribute is an array, possibly empty
    public void setResourceRef(ResourceRef[] value)
    {
	this.setValue(RESOURCE_REF, value);
    }
    
    //
    public ResourceRef[] getResourceRef()
    {
	return (ResourceRef[])this.getValues(RESOURCE_REF);
    }
    
    // Return the number of properties
    public int sizeResourceRef()
    {
	return this.size(RESOURCE_REF);
    }
    
    // Add a new element returning its index in the list
    public int addResourceRef(ResourceRef value)
    {
	return this.addValue(RESOURCE_REF, value);
    }
    
    //
    // Remove an element using its reference
    // Returns the index the element had in the list
    //
    public int removeResourceRef(ResourceRef value)
    {
	return this.removeValue(RESOURCE_REF, value);
    }
    
    // This attribute is an array, possibly empty
    public void setEjbRef(int index, EjbRef value)
    {
	this.setValue(EJB_REF, index, value);
    }
    
    //
    public EjbRef getEjbRef(int index)
    {
	return (EjbRef)this.getValue(EJB_REF, index);
    }
    
    // This attribute is an array, possibly empty
    public void setEjbRef(EjbRef[] value)
    {
	this.setValue(EJB_REF, value);
    }
    
    //
    public EjbRef[] getEjbRef()
    {
        // this is crazy
	return (EjbRef[])this.getValues(EJB_REF);
    }
    
    // Return the number of properties
    public int sizeEjbRef()
    {
	return this.size(EJB_REF);
    }
    
    // Add a new element returning its index in the list
    public int addEjbRef(EjbRef value)
    {
	return this.addValue(EJB_REF, value);
    }
    
    //
    // Remove an element using its reference
    // Returns the index the element had in the list
    //
    public int removeEjbRef(EjbRef value)
    {
	return this.removeValue(EJB_REF, value);
    }
    
    // This attribute is optional
    public void setCache(Cache value)
    {
	this.setValue(CACHE, value);
    }
    
    //
    public Cache getCache()
    {
	return (Cache)this.getValue(CACHE);
    }
    
    // This attribute is optional
    public void setClassLoader(ClassLoader value)
    {
        this.setValue(CLASS_LOADER, value);
    }

    //
    public ClassLoader getClassLoader()
    {
        return (ClassLoader)this.getValue(CLASS_LOADER);
    }

    
    // This attribute is optional
    public void setJspConfig(JspConfig value)
    {
	this.setValue(JSP_CONFIG, value);
    }
    
    //
    public JspConfig getJspConfig()
    {
	return (JspConfig)this.getValue(JSP_CONFIG);
    }
    
    // This attribute is optional
    public void setLocaleCharsetInfo(LocaleCharsetInfo value)
    {
	this.setValue(LOCALE_CHARSET_INFO, value);
    }
    
    //
    public LocaleCharsetInfo getLocaleCharsetInfo()
    {
	return (LocaleCharsetInfo)this.getValue(LOCALE_CHARSET_INFO);
    }
    
    // This method verifies that the mandatory properties are set
    public boolean verify()
    {
	return true;
    }

    // This attribute is optional
    public void setParameterEncoding(boolean value)
    {
        this.setValue(PARAMETER_ENCODING, Boolean.valueOf(value));
    }

    //
    public boolean isParameterEncoding()
    {
        Boolean ret = (Boolean)this.getValue(PARAMETER_ENCODING);
        if (ret == null) {
            return false;
        }
        return ret.booleanValue();
    }

    // This attribute is a valve to be added at the specified index
    public void setValve(int index, Valve value) {
        this.setValue(VALVE, index, value);
    }

    // The return value is the valve at the specified index
    public Valve getValve(int index) {
        return (Valve)this.getValue(VALVE, index);
    }

    // This attribute is an array, possibly empty
    public void setValve(Valve[] value) {
        this.setValue(VALVE, value);
    }

    // This return value is an array, possibly empty
    public Valve[] getValve() {
        return (Valve[])this.getValues(VALVE);
    }

    // Return the number of valves
    public int sizeValve() {
        return this.size(VALVE);
    }

    // Add a new element returning its index in the list
    public int addValve(Valve value) {
        return this.addValue(VALVE, value);
    }

    // Remove an element using its reference
    public int removeValve(Valve value) {
        return this.removeValue(VALVE, value);
    }

}
