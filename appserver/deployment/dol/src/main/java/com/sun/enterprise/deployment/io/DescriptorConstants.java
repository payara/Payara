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

package com.sun.enterprise.deployment.io;

import java.io.File;

/**
 * Contains all deployment descriptor constants.
 *
 * @author Nazrul Islam
 * @since  JDK1.4
 */
public interface DescriptorConstants {
    
    /** Prefix used for S1AS xmls */
    String S1AS_PREFIX = "sun-";

    /** Prefix used for S1AS Cmp xmls */
    String S1AS_CMP_PREFIX = "sun-cmp-";

    String WLS = "weblogic";

    /** Prefix used for WebLogic xmls */
    String WLS_PREFIX = "weblogic-";

    /** Prefix used for GF xmls */
    String GF_PREFIX = "glassfish-";

    /** The name of the deployment descriptor entry in the application ear. */
    String APPLICATION_DD_ENTRY = "META-INF"+File.separator+"application.xml";

    /** The name of the deployment descriptor entry inside the ear. */
    String APPLICATION_JAR_ENTRY = "META-INF/application.xml";

    /** The name of the deployment descriptor entry in Sun application ear. */
    String S1AS_APPLICATION_DD_ENTRY = "META-INF" + File.separator
                                    + S1AS_PREFIX + "application.xml";

    /** The name of the deployment descriptor entry in Sun application jar. */
    String S1AS_APPLICATION_JAR_ENTRY = "META-INF/"+S1AS_PREFIX+"application.xml";

    /** The name of the deployment descriptor entry in the ejb module jar. */
    String EJB_DD_ENTRY = "META-INF" + File.separator + "ejb-jar.xml";

    /** The name of the ejb deployment descriptor entry inside an ejb jar. */
    String EJB_JAR_ENTRY = "META-INF/ejb-jar.xml";

   /** The name of the ejb deployment descriptor entry inside a war. */
    String EJB_IN_WAR_ENTRY = "WEB-INF/ejb-jar.xml";

    /** The name of the Sun deployment descriptor entry in ejb module jar. */
    String S1AS_EJB_DD_ENTRY="META-INF"+File.separator+S1AS_PREFIX+"ejb-jar.xml";

    /** The Sun ejb deployment descriptor entry inside an ejb jar. */
    String S1AS_EJB_JAR_ENTRY = "META-INF/" + S1AS_PREFIX + "ejb-jar.xml";

    /** The Sun ejb deployment descriptor entry inside a war. */
    String S1AS_EJB_IN_WAR_ENTRY = "WEB-INF/" + S1AS_PREFIX + "ejb-jar.xml";

    /** The name of the deployment descriptor entry in the web module. */
    String WEB_DD_ENTRY = "WEB-INF" + File.separator + "web.xml";

    /** The name of the deployment descriptor entry in the web jar. */
    String WEB_JAR_ENTRY = "WEB-INF/web.xml";

    /** The name of the deployment descriptor entry in web fragment jar. */
    String WEB_FRAGMENT_JAR_ENTRY = "META-INF/web-fragment.xml";

    /** The name of the S1AS deployment descriptor entry in web module. */
    String S1AS_WEB_DD_ENTRY = "WEB-INF" + File.separator + S1AS_PREFIX+"web.xml";

    /** The name of the S1AS deployment descriptor entry in web jar. */
    String S1AS_WEB_JAR_ENTRY = "WEB-INF/" + S1AS_PREFIX + "web.xml";

    /** The name of the deployment descriptor entry in the rar module. */
    String RAR_DD_ENTRY = "META-INF" + File.separator + "ra.xml";

    /** The name of the deployment descriptor entry in the rar jar */
    String RAR_JAR_ENTRY = "META-INF/ra.xml";

    /** The name of the deployment descriptor entry in the rar module */
    String S1AS_RAR_DD_ENTRY = "META-INF"+File.separator+S1AS_PREFIX+"ra.xml";

    /** The name of the deployment descriptor entry in the rar jar */
    String S1AS_RAR_JAR_ENTRY = "META-INF/" + S1AS_PREFIX + "ra.xml";

    /** The name of the deployment descriptor entry inside the ear. */
    String WLS_APPLICATION_JAR_ENTRY = "META-INF/" + WLS_PREFIX + 
        "application.xml";

    /** The name of the WebLogic deployment descriptor entry in web jar. */
    String WLS_WEB_JAR_ENTRY = "WEB-INF/" + "weblogic.xml";

    /** The name of the WebLogic deployment descriptor entry in the ejb jar. */
    String WLS_EJB_JAR_ENTRY = "META-INF/" + WLS_PREFIX + "ejb-jar.xml";

    /** The name of the WebLogic deployment descriptor entry in the rar */
    String WLS_RAR_JAR_ENTRY = "META-INF/"+ WLS_PREFIX + "ra.xml";

    /** The name of the weblogic deployment descriptor entry in the client jar. */
    String WLS_APP_CLIENT_JAR_ENTRY = "META-INF/"+ WLS_PREFIX + "application-client.xml";

    /** The name of the glassfish deployment descriptor entry inside the ear. */
    String GF_APPLICATION_JAR_ENTRY = "META-INF/" + GF_PREFIX +
        "application.xml";

    /** The name of the glassfish deployment descriptor entry in web jar. */
    String GF_WEB_JAR_ENTRY = "WEB-INF/" + GF_PREFIX + "web.xml";

    /** The name of the glassfish deployment descriptor entry in the ejb jar. */
    String GF_EJB_JAR_ENTRY = "META-INF/" + GF_PREFIX + "ejb-jar.xml";

    /** The name of the glassfish deployment descriptor entry in the war. */
    String GF_EJB_IN_WAR_ENTRY = "WEB-INF/" + GF_PREFIX + "ejb-jar.xml";

    /** The name of the glassfish deployment descriptor entry in the client jar. */
    String GF_APP_CLIENT_JAR_ENTRY = "META-INF/"+ GF_PREFIX + "application-client.xml";

    /** The name of the weblogic deployment descriptor entry in the war. */
    String WLS_EJB_IN_WAR_ENTRY = "WEB-INF/" + WLS_PREFIX + "ejb-jar.xml";

    /** The name of the WEB-INF entry in a war. */
    String WEB_INF = "WEB-INF";

    // no need for File.separator
    String WEB_INF_CLASSES_DIR = WEB_INF + "/CLASSES";

    // no need for File.separator
    String WEB_INF_LIB_DIR = WEB_INF + "/LIB";

    /** The file extension for jsp tag library. */
    String TAG_LIB_EXT = ".tld";

    /** The name of the deployment descriptor entry in the client jar. */
    String APP_CLIENT_DD_ENTRY = "META-INF" 
                               + File.separator
                               + "application-client.xml";

    /** The application client entry inside a jar file. */
    String APP_CLIENT_JAR_ENTRY = "META-INF/application-client.xml";

    /** The name of the deployment descriptor entry in the client jar. */
    String S1AS_APP_CLIENT_DD_ENTRY = "META-INF" 
                                   + File.separator
                                   + S1AS_PREFIX+"application-client.xml";

    /** The Sun application client entry inside a jar file. */
    String S1AS_APP_CLIENT_JAR_ENTRY = "META-INF/"
                                    + S1AS_PREFIX + "application-client.xml";

    /** The manifest file name from an archive. */
    String MANIFEST_ENTRY = "META-INF" + File.separator + "MANIFEST.MF";

    /** The manifest file name from an archive; without File.separator */
    String JAR_MANIFEST_ENTRY = "META-INF/MANIFEST.MF";

    /** prefix used for application role mapper key */
    String APP_ROLEMAPPER_PREFIX = "app_";

    /** prefix used for module role mapper key */
    String MODULE_ROLEMAPPER_PREFIX = "module_";

    /** The Sun cmp-mapping  descriptor entry in exploded file system. */
    String S1AS_CMP_MAPPING_DD_ENTRY = "META-INF" 
                                    + File.separator
                                    + S1AS_PREFIX 
                                    + "cmp-mappings.xml";

    /** The Sun cmp-mapping  descriptor entry inside an ejb jar. */
    String S1AS_CMP_MAPPING_JAR_ENTRY = "META-INF/" 
                                     + S1AS_PREFIX + "cmp-mappings.xml";
    
    /** JaxRPC deployment descriptor file */
    String JAXRPC_JAR_ENTRY = "WEB-INF/jaxrpc-ri.xml";
    
    /** WebServices descriptor entry in a web jar */
    String WEB_WEBSERVICES_JAR_ENTRY = "WEB-INF/webservices.xml";
    
    /** WebServices descriptor entry in an ejb jar */
    String EJB_WEBSERVICES_JAR_ENTRY = "META-INF/webservices.xml";

    /** Persistence Unit Deployment Descriptor entry */
    String PERSISTENCE_DD_ENTRY = "META-INF/persistence.xml";

    /** Object to Relational mapping DD entry */
    String ORM_DD_ENTRY = "META-INF/orm.xml";

    /** Schema Namespaces for WLS */
    String WLS_SCHEMA_NAMESPACE_BEA = "http://www.bea.com/ns/weblogic/";
    String WLS_SCHEMA_NAMESPACE_ORACLE = "http://xmlns.oracle.com/weblogic/";

    /** DTD System IDs for WLS */
    String WLS_DTD_SYSTEM_ID_BEA = "http://www.beasys.com/servers";
}
