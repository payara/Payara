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

package com.sun.enterprise.admin.util;

import com.sun.enterprise.util.SystemPropertyConstants;


public interface AdminConstants {
    
    public static final String HOST_PROPERTY_NAME = "client-hostname";
    
    public static final String SYSTEM_CONNECTOR_NAME = "system";
    public static final String RENDEZVOUS_PROPERTY_NAME = "rendezvousOccurred"; 
    
    public static final String DOMAIN_TARGET = "domain";
    public static final String STANDALONE_CONFIGURATION_SUFFIX = "-config";
    
    //FIXHTHIS: Change the name when the configuration cloning is in place.
    public static final String DEFAULT_CONFIGURATION_NAME = SystemPropertyConstants.TEMPLATE_CONFIG_NAME;
    
    public static final String DAS_NODECONTROLLER_MBEAN_NAME="com.sun.appserv:type=node-agents,category=config";
    public static final String NODEAGENT_STARTINSTANCES_OVERRIDE =" startInstancesOverride";
    public static final String NODEAGENT_SYNCINSTANCES_OVERRIDE = "syncInstances";
    public static final String NODEAGENT_DOMAIN_XML_LOCATION="/config/domain.xml";

    public static final String DAS_SERVER_NAME    = "server";
    
    public static final String DAS_CONFIG_OBJECT_NAME_PATTERN = 
        "*:type=config,category=config,name=server-config";
    
    public static final String  kAdminServletURI    = "web1/entry";
    public static final String  kHttpPrefix         = "http://";
    public static final String  kHttpsPrefix         = "https://";
    public static final int     kTypeWebModule      = 0;
    public static final int     kTypeEjbModule      = 1;

    public static final int     kDebugMode          = 0;
    public static final int     kNonDebugMode       = 1;

    public static final String CLIENT_VERSION       = "clientVersion";
    public static final String OBJECT_NAME          = "objectName";
    public static final String OPERATION_NAME       = "operationName";
    public static final String OPERATION_SIGNATURE  = "signature";
    public static final String OPERATION_PARAMS     = "params";
    public static final String EXCEPTION            = "exception";
    public static final String RETURN_VALUE         = "returnValue";
    public static final String ATTRIBUTE_NAME       = "attributeName";
    public static final String ATTRIBUTE            = "jmxAttribute";
    public static final String ATTRIBUTE_LIST       = "jmxAttributeList";
    public static final String ATTRIBUTE_NAMES      = "attributeNames";
    public static final String CLIENT_JAR           = "Client.jar";
    
    public static final String kLoggerName = AdminLoggerInfo.ADMIN_LOGGER;
    

    /* Some additional values for 8.0 PE */

    public static final String DOMAIN_ADMIN_GROUP_NAME = "asadmin";
    /* This should the same as the value of <group-name> in 
    com/sun/enterprise/admin/server/core/servlet/sun-web.xml */
    
    
}
