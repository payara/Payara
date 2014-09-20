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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.catalina;

// START SJSAS
import org.apache.catalina.servlets.DefaultServlet;
// END SJSAS

/**
 * Global constants that are applicable to multiple packages within Catalina.
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.12 $ $Date: 2007/06/18 23:14:22 $
 */

public final class Globals {

    /**
     * The servlet context attribute under which we store the alternate
     * deployment descriptor for this web application 
     */
    public static final String ALT_DD_ATTR = 
        "org.apache.catalina.deploy.alt_dd";

    /**
     * The request attribute under which we store the array of X509Certificate
     * objects representing the certificate chain presented by our client,
     * if any.
     */
    public static final String CERTIFICATES_ATTR =
        "javax.servlet.request.X509Certificate";

    /**
     * SSL Certificate Request Attributite.
     */
    public static final String SSL_CERTIFICATE_ATTR = "org.apache.coyote.request.X509Certificate";

    /**
     * The request attribute under which we store the name of the cipher suite
     * being used on an SSL connection (as an object of type
     * java.lang.String).
     */
    public static final String CIPHER_SUITE_ATTR =
        "javax.servlet.request.cipher_suite";


    /**
     * The servlet context attribute under which we store the class loader
     * used for loading servlets (as an object of type java.lang.ClassLoader).
     */
    public static final String CLASS_LOADER_ATTR =
        "org.apache.catalina.classloader";

    /**
     * Request dispatcher state.
     */
    public static final String DISPATCHER_TYPE_ATTR = 
        "org.apache.catalina.core.DISPATCHER_TYPE";

    /**
     * Request dispatcher path.
     */
    public static final String DISPATCHER_REQUEST_PATH_ATTR = 
        "org.apache.catalina.core.DISPATCHER_REQUEST_PATH";

    /**
     * The JNDI directory context which is associated with the context. This
     * context can be used to manipulate static files.
     */
    public static final String RESOURCES_ATTR =
        "org.apache.catalina.resources";
    public static final String ALTERNATE_RESOURCES_ATTR =
        "org.apache.catalina.alternateResources";


    /**
     * The servlet context attribute under which we store the class path
     * for our application class loader (as an object of type String),
     * delimited with the appropriate path delimiter for this platform.
     */
    public static final String CLASS_PATH_ATTR =
        "org.apache.catalina.jsp_classpath";


    /**
     * The request attribute under which the Invoker servlet will store
     * the invoking servlet path, if it was used to execute a servlet
     * indirectly instead of through a servlet mapping.
     */
    public static final String INVOKED_ATTR =
        "org.apache.catalina.INVOKED";


    /**
     * The request attribute under which we expose the value of the
     * <code>&lt;jsp-file&gt;</code> value associated with this servlet,
     * if any.
     */
    public static final String JSP_FILE_ATTR =
        "org.apache.catalina.jsp_file";


    /**
     * The request attribute under which we store the key size being used for
     * this SSL connection (as an object of type java.lang.Integer).
     */
    public static final String KEY_SIZE_ATTR =
        "javax.servlet.request.key_size";


    /**
     * The request attribute under which we store the session id being used
     * for this SSL connection (as an object of type java.lang.String).
     */
    public static final String SSL_SESSION_ID_ATTR =
        "javax.servlet.request.ssl_session_id";
    

    /**
     * The servlet context attribute under which the managed bean Registry
     * will be stored for privileged contexts (if enabled).
     */
    public static final String MBEAN_REGISTRY_ATTR =
        "org.apache.catalina.Registry";


    /**
     * The servlet context attribute under which the MBeanServer will be stored
     * for privileged contexts (if enabled).
     */
    public static final String MBEAN_SERVER_ATTR =
        "org.apache.catalina.MBeanServer";


    /**
     * The request attribute under which we store the servlet name on a
     * named dispatcher request.
     */
    public static final String NAMED_DISPATCHER_ATTR =
        "org.apache.catalina.NAMED";


    /**
     * The name of the cookie used to pass the session identifier back
     * and forth with the client.
     */
    public static final String SESSION_COOKIE_NAME = "JSESSIONID";


    /**
     * The name of the path parameter used to pass the session identifier
     * back and forth with the client.
     */
    public static final String SESSION_PARAMETER_NAME = "jsessionid";


    /**
     * The subject under which the AccessControlContext is running.
     */
    public static final String SUBJECT_ATTR =
        "javax.security.auth.subject";

    
    // START SJSAS
    /**
     * The class name of the default servlet
     */
    public static final String DEFAULT_SERVLET_CLASS_NAME =
        DefaultServlet.class.getName();
    // END SJSAS


    /**
     * Has security been turned on?
     */
    public static final boolean IS_SECURITY_ENABLED = 
        (System.getSecurityManager() != null);


    // START GlassFish 740
    public static final String JSP_PROPERTY_GROUPS_CONTEXT_ATTRIBUTE =
        "com.sun.jsp.propertyGroups";

    public static final String WEB_XML_VERSION_CONTEXT_ATTRIBUTE =
        "com.sun.servlet.webxml.version";
    // END GlassFish 740

    // START GlassFish 747
    public static final String JSP_TLD_URI_TO_LOCATION_MAP =
        "com.sun.jsp.tldUriToLocationMap";
    // END GlassFish 747

    // START GlassFish 896
    public static final String SESSION_TRACKER =
        "com.sun.enterprise.http.sessionTracker";    
    // END GlassFish 896

    public static final String REQUEST_FACADE_HELPER =
        "org.glassfish.web.RequestFacadeHelper";

    /**
     * The name of the cookie used to carry a session's version info
     */
    public static final String SESSION_VERSION_COOKIE_NAME =
        "JSESSIONIDVERSION";

    /**
     * The name of the path parameter used to carry a session's version info
     */
    public static final String SESSION_VERSION_PARAMETER_NAME =
        "jsessionidversion";

    public static final String SESSION_VERSION_PARAMETER =
        ";" + SESSION_VERSION_PARAMETER_NAME + "=";

    public static final String SESSION_VERSIONS_REQUEST_ATTRIBUTE =
        "com.sun.enterprise.http.sessionVersions";

    public static final String JREPLICA_COOKIE_NAME = "JREPLICA";

    public static final String JREPLICA_PARAMETER_NAME = "jreplica";

    public static final String JREPLICA_PARAMETER =
        ";" + JREPLICA_PARAMETER_NAME + "=";

    public static final String JREPLICA_SESSION_NOTE =
        "com.sun.enterprise.http.jreplicaLocation";

    public static final String WRAPPED_REQUEST =
        "__javax.security.auth.message.request";
    
    public static final String WRAPPED_RESPONSE =
        "__javax.security.auth.message.response"; 
    
    
    /**
     * The servlet context attribute under which we store a flag used
     * to mark this request as having been processed by the SSIServlet.
     * We do this because of the pathInfo mangling happening when using
     * the CGIServlet in conjunction with the SSI servlet. (value stored
     * as an object of type String)
     */
     public static final String SSI_FLAG_ATTR =
         "org.apache.catalina.ssi.SSIServlet";

    /**
     * Request path.
     */
    public static final String CONSTRAINT_URI =
        "org.apache.catalina.CONSTRAINT_URI";

    public static final String META_INF_RESOURCES = "META-INF/resources";

    public static final String ISO_8859_1_ENCODING = "ISO-8859-1";

    public static final String FACES_INITIALIZER = "com.sun.faces.config.FacesInitializer";
}
