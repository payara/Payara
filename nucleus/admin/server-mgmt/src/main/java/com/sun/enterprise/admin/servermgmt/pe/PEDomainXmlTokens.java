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

package com.sun.enterprise.admin.servermgmt.pe;


import com.sun.enterprise.admin.util.TokenValue;
import com.sun.enterprise.admin.util.TokenValueSet;
import com.sun.enterprise.admin.servermgmt.DomainConfig;

public final class PEDomainXmlTokens
{
    public static final String CONFIG_MODEL_NAME_TOKEN_NAME  = "CONFIG_MODEL_NAME";
    public static final String CONFIG_MODEL_NAME_TOKEN_VALUE = "server-config";

    public static final String HOST_NAME_TOKEN_NAME          = "HOST_NAME";
    public static final String DOMAIN_NAME_TOKEN_NAME        = "DOMAIN_NAME";
    
    public static final String HTTP_PORT_TOKEN_NAME          = "HTTP_PORT";

    public static final String ORB_LISTENER_PORT_TOKEN_NAME  = "ORB_LISTENER_PORT";

    public static final String JMS_PROVIDER_PORT_TOKEN_NAME= "JMS_PROVIDER_PORT"; 
    
    public static final String SERVER_ID_TOKEN_NAME= "SERVER_ID"; 

    public static final String ADMIN_PORT_TOKEN_NAME= "ADMIN_PORT";

    public static final String HTTP_SSL_PORT_TOKEN_NAME= "HTTP_SSL_PORT";

    public static final String ORB_SSL_PORT_TOKEN_NAME= "ORB_SSL_PORT";

    public static final String ORB_MUTUALAUTH_PORT_TOKEN_NAME= "ORB_MUTUALAUTH_PORT";

    public static final String OSGI_SHELL_TELNET_PORT_TOKEN_NAME = "OSGI_SHELL_TELNET_PORT";

    public static final String JAVA_DEBUGGER_PORT_TOKEN_NAME = "JAVA_DEBUGGER_PORT";
    
    public static final String ADMIN_CERT_DN_TOKEN_NAME = "ADMIN_CERT_DN";
    
    public static final String INSTANCE_CERT_DN_TOKEN_NAME = "INSTANCE_CERT_DN";
    
    public static final String SECURE_ADMIN_IDENTIFIER_TOKEN_NAME = "SECURE_ADMIN_IDENTIFIER";
    

    //This token is used for SE/EE only now, but it is likely that we will want to expose it
    //in PE (i.e. to access the exposed Mbeans). Remember that the http jmx port (used by
    //asadmin) will not be exposed pubically.
    public static final String JMX_SYSTEM_CONNECTOR_PORT_TOKEN_NAME="JMX_SYSTEM_CONNECTOR_PORT";
    
    public static TokenValueSet getTokenValueSet(DomainConfig domainConfig)
    {
        final String  installRoot = 
        (String)domainConfig.get(DomainConfig.K_INSTALL_ROOT);
        final String  domainRoot  = 
        (String)domainConfig.get(DomainConfig.K_DOMAINS_ROOT);

        final TokenValueSet tokens = new TokenValueSet();

        String instanceName = (String)domainConfig.get(DomainConfig.K_SERVERID);
        if((instanceName == null) || (instanceName.equals("")))
            instanceName = PEFileLayout.DEFAULT_INSTANCE_NAME;

        TokenValue tv = new TokenValue(CONFIG_MODEL_NAME_TOKEN_NAME,
                                    CONFIG_MODEL_NAME_TOKEN_VALUE);
        tokens.add(tv);

        tv = new TokenValue(HOST_NAME_TOKEN_NAME, 
                (String)domainConfig.get(DomainConfig.K_HOST_NAME));
        tokens.add(tv);

        final Integer adminPort = 
            (Integer)domainConfig.get(DomainConfig.K_ADMIN_PORT);
        tv = new TokenValue(ADMIN_PORT_TOKEN_NAME, adminPort.toString());
        tokens.add(tv);

        final Integer httpPort = 
            (Integer)domainConfig.get(DomainConfig.K_INSTANCE_PORT);
        tv = new TokenValue(HTTP_PORT_TOKEN_NAME, httpPort.toString());
        tokens.add(tv);

        final Integer orbPort = 
            (Integer)domainConfig.get(DomainConfig.K_ORB_LISTENER_PORT);
        tv = new TokenValue(ORB_LISTENER_PORT_TOKEN_NAME, orbPort.toString());
        tokens.add(tv);

        final Integer jmsPort = 
            (Integer)domainConfig.get(DomainConfig.K_JMS_PORT);
        tv = new TokenValue(JMS_PROVIDER_PORT_TOKEN_NAME, jmsPort.toString());
        tokens.add(tv);

        tv = new TokenValue(SERVER_ID_TOKEN_NAME, 
                                instanceName);
        tokens.add(tv);
 
        final Integer httpSslPort = 
            (Integer)domainConfig.get(DomainConfig.K_HTTP_SSL_PORT);
        tv = new TokenValue(HTTP_SSL_PORT_TOKEN_NAME, httpSslPort.toString());
        tokens.add(tv);

        final Integer orbSslPort = 
            (Integer)domainConfig.get(DomainConfig.K_IIOP_SSL_PORT);
        tv = new TokenValue(ORB_SSL_PORT_TOKEN_NAME, orbSslPort.toString());
        tokens.add(tv);

        final Integer orbMutualAuthPort = 
            (Integer)domainConfig.get(DomainConfig.K_IIOP_MUTUALAUTH_PORT);
        tv = new TokenValue(ORB_MUTUALAUTH_PORT_TOKEN_NAME, orbMutualAuthPort.toString());
        tokens.add(tv);

        final Integer jmxPort = 
            (Integer)domainConfig.get(DomainConfig.K_JMX_PORT);
        tv = new TokenValue(JMX_SYSTEM_CONNECTOR_PORT_TOKEN_NAME, jmxPort.toString());
        tokens.add(tv);

        tv = new TokenValue(DOMAIN_NAME_TOKEN_NAME, domainConfig.getRepositoryName());
        tokens.add(tv);
        
        final Integer osgiShellTelnetPort =
            (Integer)domainConfig.get(DomainConfig.K_OSGI_SHELL_TELNET_PORT);
        tv = new TokenValue(OSGI_SHELL_TELNET_PORT_TOKEN_NAME, osgiShellTelnetPort.toString());
        tokens.add(tv);

        final Integer javaDebuggerPort =
            (Integer)domainConfig.get(DomainConfig.K_JAVA_DEBUGGER_PORT);
        tv = new TokenValue(JAVA_DEBUGGER_PORT_TOKEN_NAME, javaDebuggerPort.toString());
        tokens.add(tv);

        tv = new TokenValue(ADMIN_CERT_DN_TOKEN_NAME, (String) domainConfig.get(DomainConfig.K_ADMIN_CERT_DN));
        tokens.add(tv);
        
        tv = new TokenValue(INSTANCE_CERT_DN_TOKEN_NAME, (String) domainConfig.get(DomainConfig.K_INSTANCE_CERT_DN));
        tokens.add(tv);
        
        tv = new TokenValue(SECURE_ADMIN_IDENTIFIER_TOKEN_NAME, (String) domainConfig.get(DomainConfig.K_SECURE_ADMIN_IDENTIFIER));
        tokens.add(tv);
        
        return ( tokens );
    }
}
