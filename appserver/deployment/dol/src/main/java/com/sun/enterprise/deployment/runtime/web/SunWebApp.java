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

package com.sun.enterprise.deployment.runtime.web;

import com.sun.enterprise.deployment.runtime.common.SecurityRoleMapping;
import com.sun.enterprise.deployment.runtime.common.wls.SecurityRoleAssignment;

// BEGIN_NOI18N

public interface SunWebApp {

    static public final String SECURITY_ROLE_MAPPING = "SecurityRoleMapping";	// NOI18N
    static public final String SECURITY_ROLE_ASSIGNMENT = "SecurityRoleAssignment";	// NOI18N
    static public final String SERVLET = "Servlet";	// NOI18N
    static public final String SESSION_CONFIG = "SessionConfig";	// NOI18N
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

    public void setSecurityRoleMapping(int index, SecurityRoleMapping value);

    public SecurityRoleMapping getSecurityRoleMapping(int index);

    public void setSecurityRoleMapping(SecurityRoleMapping[] value);

    public SecurityRoleMapping[] getSecurityRoleMapping();

    public int sizeSecurityRoleMapping();

    public int addSecurityRoleMapping(SecurityRoleMapping value);

    public int removeSecurityRoleMapping(SecurityRoleMapping value);

    public void setSecurityRoleAssignment(int index, SecurityRoleAssignment value);

    public SecurityRoleAssignment getSecurityRoleAssignment(int index);

    public void setSecurityRoleAssignments(SecurityRoleAssignment[] value);

    public SecurityRoleAssignment[] getSecurityRoleAssignments();

    public int sizeSecurityRoleAssignment();

    public int addSecurityRoleAssignment(SecurityRoleAssignment value);

    public int removeSecurityRoleAssignment(SecurityRoleAssignment value);

    public void setIdempotentUrlPattern(int index, IdempotentUrlPattern value);

    public  IdempotentUrlPattern getIdempotentUrlPattern(int index);

    public void setIdempotentUrlPatterns(IdempotentUrlPattern[] value);

    public IdempotentUrlPattern[] getIdempotentUrlPatterns();

    public int sizeIdempotentUrlPattern();

    public int addIdempotentUrlPattern(IdempotentUrlPattern value);

    public int removeIdempotentUrlPattern(IdempotentUrlPattern value);

    public String getAttributeValue(String attributeName);

}
