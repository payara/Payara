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

package com.sun.enterprise.deployment.web;

/**
 * Constants used by the various web descriptors.
 * @author James Todd [gonzo@eng.sun.com]
 * @author Danny Coward
 */

public final class Constants {
    private Constants() { /* disallow instantiation */ }
    
    public static final String ConfigFile = "web.xml";

    public static final String WebApp = "web-app";
    public static final String Servlet = "servlet";    
    public static final String ServletName = "servlet-name";
    public static final String ServletClass = "servlet-class";
    public static final String JSP_FILENAME = "jsp-file";
    public static final String LOAD_ON_START_UP = "load-on-startup";

    public static final String Filter = "filter";
    public static final String FilterMapping = "filter-mapping";
    public static final String FilterClass = "filter-class";
    public static final String FilterName = "filter-name";

    public static final String Parameter = "init-param";
    public static final String CONTEXT_PARAMETER = "context-param";
    public static final String ParameterName = "param-name";
    public static final String ParameterValue = "param-value";
    public static final String MIMEMapping = "mime-mapping";
    public static final String MIMEMappingExtension = "extension";
    public static final String MIMEMappingType = "mime-type";
    public static final String ServletMapping = "servlet-mapping";
    public static final String URLPattern = "url-pattern";
    public static final String SessionTimeOut = "session-timeout";
    public static final String WelcomeFileList = "welcome-file-list";
    public static final String WelcomeFile = "welcome-file";
    
    public static final String DISPLAY_NAME = "display-name";
    public static final String DESCRIPTION = "description";
    public static final String ICON = "icon";
    public static final String LARGE_ICON = "large-icon";
    public static final String SMALL_ICON = "small-icon";
    public static final String DISTRIBUTABLE = "distributable";
    
    public static final String ERROR_PAGE = "error-page";
    public static final String ERROR_CODE = "error-code";
    public static final String EXCEPTION_TYPE = "exception-type";
    public static final String LOCATION = "location";
    
    public static final String LISTENER = "listener";
    public static final String LISTENER_CLASS = "listener-class";

    public static final String ENVIRONMENT_ENTRY = "env-entry";
    public static final String ENVIRONMENT_NAME = "env-entry-name";
    public static final String ENVIRONMENT_VALUE = "env-entry-value";
    public static final String ENVIRONMENT_TYPE = "env-entry-type";
    
    
    public static final String RESOURCE_REFERENCE = "resource-ref";
    public static final String RESOURCE_REFERENCE_NAME = "res-ref-name";
    public static final String RESOURCE_TYPE = "res-type";
    public static final String RESOURCE_AUTHORIZATION = "res-auth";
    

    public static final String RESOURCE_ENV_REFERENCE = "resource-env-ref";
    public static final String RESOURCE_ENV_REFERENCE_NAME = "resource-env-ref-name";
    public static final String RESOURCE_ENV_REFERENCE_TYPE = "resource-env-ref-type";

    public static final String SECURITY_ROLE = "security-role";
    public static final String ROLE_NAME = "role-name";
    public static final String NAME = "name";
    
    public static final String SECURITY_CONSTRAINT = "security-constraint";
    public static final String WEB_RESOURCE_COLLECTION = "web-resource-collection";
    public static final String AUTH_CONSTRAINT = "auth-constraint";
    public static final String USERDATA_CONSTRAINT = "user-data-constraint";
    public static final String TRANSPORT_GUARANTEE = "transport-guarantee";
    public static final String WEB_RESOURCE_NAME = "web-resource-name";
    public static final String URL_PATTERN = "url-pattern";
    public static final String HTTP_METHOD = "http-method";
    
    public static final String SECURITY_ROLE_REFERENCE = "security-role-ref";
    public static final String ROLE_LINK = "role-link";
    
    
    public static final String EJB_REFERENCE = "ejb-ref";
    public static final String EJB_LOCAL_REFERENCE = "ejb-local-ref";
    public static final String EJB_NAME = "ejb-ref-name";
    public static final String EJB_TYPE = "ejb-ref-type";
    public static final String EJB_HOME = "home";
    public static final String EJB_REMOTE = "remote";
    public static final String EJB_LOCAL_HOME = "local-home";
    public static final String EJB_LOCAL = "local";
    public static final String EJB_LINK = "ejb-link";
    public static final String RUN_AS = "run-as";
        
    public static final String SESSION_CONFIG = "session-config";
    
    public static final String LOGIN_CONFIG = "login-config";
    public static final String AUTH_METHOD = "auth-method";
    public static final String REALM_NAME = "realm-name";
    public static final String FORM_LOGIN_CONFIG = "form-login-config";
    public static final String FORM_LOGIN_PAGE = "form-login-page";
    public static final String FORM_ERROR_PAGE = "form-error-page";

    /* -----
    ** TagLibConfiguration (TagLib reference)
    */

    public static final String TAGLIB = "taglib";
    public static final String TAGLIB_URI = "taglib-uri";
    public static final String TAGLIB_LOCATION = "taglib-location";

    /* -----
    ** TagLib tags
    */

    public static final String TagLib                   = "taglib";
    public static final String TagLib_VERSION           = "tlibversion";
    public static final String TagLib_JSPVERSION        = "jspversion";
    public static final String TagLib_SHORTNAME         = "shortname";
    public static final String TagLib_URI               = "uri";
    public static final String TagLib_INFO              = "info";
    public static final String TagLib_TAGS              = "tag";

    public static final String Tag_NAME                 = "name";
    public static final String Tag_CLASS                = "tagclass";
    public static final String Tag_EXTRA_INFO           = "teiclass";
    public static final String Tag_BODYCONTENT          = "bodycontent";
    public static final String Tag_INFO                 = "info";
    public static final String Tag_ATTRS                = "attribute";

    public static final String TagAttr_NAME             = "name";
    public static final String TagAttr_REQUIRED         = "required";
    public static final String TagAttr_ALLOWEXPR        = "rtexprvalue";
    public static final String TagAttr_TYPE             = "type";

    public static final String TagLib12_VERSION         = "tlib-version";
    public static final String TagLib12_JSPVERSION      = "jsp-version";
    public static final String TagLib12_SHORTNAME       = "short-name";
    public static final String TagLib12_URI             = TagLib_URI;
    public static final String TagLib12_DISPLAYNAME     = DISPLAY_NAME;
    public static final String TagLib12_SMALLICON       = SMALL_ICON;
    public static final String TagLib12_LARGEICON       = LARGE_ICON;
    public static final String TagLib12_DESCRIPTION     = DESCRIPTION;
    public static final String TagLib12_VALIDATOR       = "validator";
    public static final String TagLib12_LISTENER        = LISTENER;
    public static final String TagLib12_TAGS            = TagLib_TAGS;

    public static final String TagList12_CLASS		= LISTENER_CLASS;

    public static final String TagVal12_CLASS           = "validator-class";
    public static final String TagVal12_INIT_PARMS      = Parameter;

    public static final String Tag12_NAME               = Tag_NAME;
    public static final String Tag12_CLASS              = "tag-class";
    public static final String Tag12_EXTRA_INFO         = "tei-class";
    public static final String Tag12_BODYCONTENT        = "body-content";
    public static final String Tag12_DISPLAYNAME        = DISPLAY_NAME;
    public static final String Tag12_SMALLICON          = SMALL_ICON;
    public static final String Tag12_LARGEICON          = LARGE_ICON;
    public static final String Tag12_DESCRIPTION        = DESCRIPTION;
    public static final String Tag12_VARIABLE           = "variable";
    public static final String Tag12_ATTRS              = Tag_ATTRS;

    public static final String TagVar12_NAME_GIVEN      = "name-given";
    public static final String TagVar12_NAME_ATTR       = "name-from-attribute";
    public static final String TagVar12_CLASS           = "variable-class";
    public static final String TagVar12_DECLARE         = "declare";
    public static final String TagVar12_SCOPE           = "scope";

}

