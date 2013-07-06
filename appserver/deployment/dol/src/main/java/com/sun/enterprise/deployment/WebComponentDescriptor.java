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

import com.sun.enterprise.deployment.web.InitializationParameter;
import com.sun.enterprise.deployment.web.MultipartConfig;
import com.sun.enterprise.deployment.web.SecurityRoleReference;
import org.glassfish.deployment.common.Descriptor;

import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Set;


/**
 * Common data and behavior of the deployment
 * information about a JSP or JavaServlet in J2EE.
 *
 * @author Jerome Dochez
 */
public abstract class WebComponentDescriptor extends Descriptor {

    public abstract Set<InitializationParameter> getInitializationParameterSet();

    public abstract Enumeration<InitializationParameter> getInitializationParameters();

    public abstract InitializationParameter getInitializationParameterByName(String name);

    public abstract void addInitializationParameter(InitializationParameter initializationParameter);

    public abstract void removeInitializationParameter(InitializationParameter initializationParameter);

    public abstract Set<String> getUrlPatternsSet();

    public abstract Enumeration<String> getUrlPatterns();

    public abstract void addUrlPattern(String urlPattern);

    public abstract void removeUrlPattern(String urlPattern);

    public abstract void setWebBundleDescriptor(WebBundleDescriptor webBundleDescriptor);

    public abstract WebBundleDescriptor getWebBundleDescriptor();

    public abstract String getCanonicalName();

    public abstract void setCanonicalName(String canonicalName);

    public abstract Integer getLoadOnStartUp();

    public abstract void setLoadOnStartUp(Integer loadOnStartUp);

    public abstract void setLoadOnStartUp(String loadOnStartUp) throws NumberFormatException;

    public abstract Set<SecurityRoleReference> getSecurityRoleReferenceSet();

    public abstract Enumeration<SecurityRoleReference> getSecurityRoleReferences();

    public abstract SecurityRoleReference getSecurityRoleReferenceByName(String roleReferenceName);

    public abstract void addSecurityRoleReference(SecurityRoleReference securityRoleReference);

    public abstract void removeSecurityRoleReference(SecurityRoleReference securityRoleReference);

    public abstract void setRunAsIdentity(RunAsIdentityDescriptor runAs);

    public abstract RunAsIdentityDescriptor getRunAsIdentity();

    public abstract boolean getUsesCallerIdentity();

    public abstract void setUsesCallerIdentity(boolean isCallerID);

    public abstract MultipartConfig getMultipartConfig();

    public abstract void setMultipartConfig(MultipartConfig multipartConfig);

    public abstract Application getApplication();

    public abstract void setWebComponentImplementation(String implFile);

    public abstract String getWebComponentImplementation();

    public abstract boolean isServlet();

    public abstract void setServlet(boolean isServlet);

    public abstract boolean isEnabled();

    public abstract void setEnabled(boolean enabled);

    public abstract void setAsyncSupported(Boolean asyncSupported);

    public abstract Boolean isAsyncSupported();

    public abstract void setConflict(boolean conflict);

    public abstract boolean isConflict();

    public abstract Method[] getUserDefinedHttpMethods();

    //public abstract void print(StringBuffer toStringBuffer);

    public abstract boolean equals(Object other);

    public abstract int hashCode();

    public abstract void add(WebComponentDescriptor other);

    public abstract void add(WebComponentDescriptor other, boolean combineUrlPatterns, boolean combineConflict);

    public abstract boolean isConflict(WebComponentDescriptor other, boolean allowNullImplNameOverride);

    public abstract Set<String> getConflictedInitParameterNames();
}
