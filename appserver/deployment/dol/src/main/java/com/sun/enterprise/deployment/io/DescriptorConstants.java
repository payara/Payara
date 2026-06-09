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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright 2018-2026 Payara Foundation and/or its affiliates

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
    @Deprecated
    String S1AS_PREFIX = "sun-";

    /** Prefix used for GF xmls */
    @Deprecated
    String GF_PREFIX = "glassfish-";
    
    String PAYARA_PREFIX = "payara-";

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
    @Deprecated
    String S1AS_EJB_DD_ENTRY="META-INF"+File.separator+S1AS_PREFIX+"ejb-jar.xml";

    /** The Sun ejb deployment descriptor entry inside an ejb jar. */
    @Deprecated
    String S1AS_EJB_JAR_ENTRY = "META-INF/" + S1AS_PREFIX + "ejb-jar.xml";

    /** The Sun ejb deployment descriptor entry inside a war. */
    @Deprecated
    String S1AS_EJB_IN_WAR_ENTRY = "WEB-INF/" + S1AS_PREFIX + "ejb-jar.xml";

    /** The name of the deployment descriptor entry in the web module. */
    String WEB_DD_ENTRY = "WEB-INF" + File.separator + "web.xml";

    /** The name of the deployment descriptor entry in the web jar. */
    String WEB_JAR_ENTRY = "WEB-INF/web.xml";

    /** The name of the deployment descriptor entry in web fragment jar. */
    String WEB_FRAGMENT_JAR_ENTRY = "META-INF/web-fragment.xml";

    /** The name of the S1AS deployment descriptor entry in web module. */
    @Deprecated
    String S1AS_WEB_DD_ENTRY = "WEB-INF" + File.separator + S1AS_PREFIX+"web.xml";

    /** The name of the S1AS deployment descriptor entry in web jar. */
    @Deprecated
    String S1AS_WEB_JAR_ENTRY = "WEB-INF/" + S1AS_PREFIX + "web.xml";

    /** The name of the deployment descriptor entry in the rar module. */
    String RAR_DD_ENTRY = "META-INF" + File.separator + "ra.xml";

    /** The name of the deployment descriptor entry in the rar jar */
    String RAR_JAR_ENTRY = "META-INF/ra.xml";

    /** The name of the deployment descriptor entry in the rar module */
    @Deprecated
    String S1AS_RAR_DD_ENTRY = "META-INF"+File.separator+S1AS_PREFIX+"ra.xml";

    /** The name of the deployment descriptor entry in the rar jar */
    @Deprecated
    String S1AS_RAR_JAR_ENTRY = "META-INF/" + S1AS_PREFIX + "ra.xml";

    /** The name of the glassfish deployment descriptor entry inside the ear. */
    @Deprecated
    String GF_APPLICATION_JAR_ENTRY = "META-INF/" + GF_PREFIX +
        "application.xml";

    /** The name of the glassfish deployment descriptor entry in web jar. */
    @Deprecated
    String GF_WEB_JAR_ENTRY = "WEB-INF/" + GF_PREFIX + "web.xml";

    /** The name of the glassfish deployment descriptor entry in the ejb jar. */
    @Deprecated
    String GF_EJB_JAR_ENTRY = "META-INF/" + GF_PREFIX + "ejb-jar.xml";

    /** The name of the glassfish deployment descriptor entry in the war. */
    @Deprecated
    String GF_EJB_IN_WAR_ENTRY = "WEB-INF/" + GF_PREFIX + "ejb-jar.xml";

    /** The name of the glassfish deployment descriptor entry in the client jar. */
    @Deprecated
    String GF_APP_CLIENT_JAR_ENTRY = "META-INF/"+ GF_PREFIX + "application-client.xml";
    
    /** The name of the Payara deployment descriptor entry in web jar. */
    String PAYARA_WEB_JAR_ENTRY = "WEB-INF/" + PAYARA_PREFIX + "web.xml";

    /** The name of the Payara deployment descriptor entry in the ejb jar. */
    String PAYARA_EJB_JAR_ENTRY = "META-INF/" + PAYARA_PREFIX + "ejb-jar.xml";

    /** The name of the Payara deployment descriptor entry in the war. */
    String PAYARA_EJB_IN_WAR_ENTRY = "WEB-INF/" + PAYARA_PREFIX + "ejb-jar.xml";

    /** The name of the Payara deployment descriptor entry in the client jar. */
    String PAYARA_APP_CLIENT_JAR_ENTRY = "META-INF/"+ PAYARA_PREFIX + "application-client.xml";

    /** The name of the Payara deployment descriptor entry inside the ear. */
    String PAYARA_APPLICATION_JAR_ENTRY = "META-INF/"+ PAYARA_PREFIX + "application.xml";

    /** The name of the deployment descriptor entry in the client jar. */
    String APP_CLIENT_DD_ENTRY = "META-INF" 
                               + File.separator
                               + "application-client.xml";

    /** The application client entry inside a jar file. */
    String APP_CLIENT_JAR_ENTRY = "META-INF/application-client.xml";

    /** The name of the deployment descriptor entry in the client jar. */
    @Deprecated
    String S1AS_APP_CLIENT_DD_ENTRY = "META-INF" + File.separator + S1AS_PREFIX + "application-client.xml";

    /** The Sun application client entry inside a jar file. */
    @Deprecated
    String S1AS_APP_CLIENT_JAR_ENTRY = "META-INF/" + S1AS_PREFIX + "application-client.xml";
    
    /** JaxRPC deployment descriptor file */
    String JAXRPC_JAR_ENTRY = "WEB-INF/jaxrpc-ri.xml";
    
    /** WebServices descriptor entry in a web jar */
    String WEB_WEBSERVICES_JAR_ENTRY = "WEB-INF/webservices.xml";
    
    /** WebServices descriptor entry in an ejb jar */
    String EJB_WEBSERVICES_JAR_ENTRY = "META-INF/webservices.xml";

    /** Persistence Unit Deployment Descriptor entry */
    String PERSISTENCE_DD_ENTRY = "META-INF/persistence.xml";
}