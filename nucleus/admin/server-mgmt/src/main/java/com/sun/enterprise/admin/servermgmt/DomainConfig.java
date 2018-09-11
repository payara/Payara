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
package com.sun.enterprise.admin.servermgmt;

import java.util.HashMap;
import java.util.Properties;

import java.util.Map;

import com.sun.enterprise.universal.glassfish.ASenvPropertyReader;
import com.sun.enterprise.util.SystemPropertyConstants;
import java.util.Locale;

/**
 * This class defines the keys that are used to create the domain config object.
 * Almost all the methods of DomainsManager require the domain config to be
 * passed as java.util.Map, the key set of which is defined here.
 */
public class DomainConfig extends RepositoryConfig {
    /**
     * These constants define the possbile Hash Map keys that can reside in
     * DomainConfig MAKE SURE THAT KEYS FOR PORTS END IN THE STRING "PORT" (case
     * ignored) - this is used in PEDomainConfigValidator to ensure that the
     * ports are unique!
     */
    public static final String K_USER = "domain.user";
    public static final String K_PASSWORD = "domain.password";
    public static final String K_NEW_MASTER_PASSWORD = "domain.newMasterPassword";
    public static final String K_MASTER_PASSWORD = "domain.masterPassword";
    public static final String K_SAVE_MASTER_PASSWORD = "domain.saveMasterPassword";
    public static final String K_ADMIN_PORT = "domain.adminPort";
    public static final String K_INSTANCE_PORT = "domain.instancePort";
    public static final String K_PROFILE = "domain.profile";
    public static final String K_DOMAINS_ROOT = "domains.root";
    public static final String K_HOST_NAME = "domain.hostName";
    public static final String K_JMS_PORT = "jms.port";
    public static final String K_ORB_LISTENER_PORT = "orb.listener.port";
    public static final String K_SERVERID = "server.id";
    public static final String K_TEMPLATE_NAME = "template.name";
    public static final String K_HTTP_SSL_PORT = "http.ssl.port";
    public static final String K_IIOP_SSL_PORT = "orb.ssl.port";
    public static final String K_IIOP_MUTUALAUTH_PORT = "orb.mutualauth.port";
    public static final String K_OSGI_SHELL_TELNET_PORT = "osgi.shell.telnet.port";
    public static final String K_JAVA_DEBUGGER_PORT = "java.debugger.port";
    public static final String K_DEBUG = "domain.debug";
    public static final String K_VERBOSE = "domain.verbose";
    public static final String K_VALIDATE_PORTS = "domain.validatePorts";
    public static final String K_PORTBASE = "portbase";
    //This token is used for SE/EE only now, but it is likely that we will want to expose it
    //in PE (i.e. to access the exposed Mbeans). Remember that the http jmx port (used by
    //asadmin) will not be exposed pubically.
    public static final String K_JMX_PORT = "domain.jmxPort";
    public static final String K_EXTRA_PASSWORDS = "domain.extraPasswords";
    public static final int K_FLAG_START_DOMAIN_NEEDS_ADMIN_USER = 0x1;
    public static final String KEYTOOLOPTIONS = "keytooloptions";
    public static final String K_ADMIN_CERT_DN = "domain.admin.cert.dn";
    public static final String K_INSTANCE_CERT_DN = "domain.instance.cert.dn";
    public static final String K_SECURE_ADMIN_IDENTIFIER = "domain.indicator";
    public static final String K_INITIAL_ADMIN_USER_GROUPS="domain.admin.groups";

    private Properties _domainProperties;

    /**
     * The DomainConfig always contains the K_DOMAINS_ROOT and K_HOST_NAME
     * attributes.
     */
    public DomainConfig(String domainName, String domainRoot) throws DomainException {
        super(domainName, domainRoot);
        try {
            put(K_DOMAINS_ROOT, domainRoot);
            // net to get fully qualified host, not just hostname
            ASenvPropertyReader pr = new ASenvPropertyReader();
            Map<String, String> envProperties = pr.getProps();
            put(K_HOST_NAME,
                    envProperties.get(SystemPropertyConstants.HOST_NAME_PROPERTY));
        }
        catch (Exception ex) {
            throw new DomainException(ex);
        }
    }

    /**
     * This constructor is used at domain creation time only.
     */
    public DomainConfig(String domainName, Integer adminPort, String domainRoot,
            String adminUser, String adminPassword, String masterPassword,
            Boolean saveMasterPassword, Integer instancePort, Integer jmsPort,
            Integer orbPort, Integer httpSSLPort,
            Integer iiopSSLPort, Integer iiopMutualAuthPort,
            Integer jmxAdminPort, Integer osgiShellTelnetPort,
            Integer javaDebuggerPort,
            Properties domainProperties) throws DomainException {
        this(domainName, domainRoot);
        try {
            put(K_ADMIN_PORT, adminPort);
            put(K_PASSWORD, adminPassword);
            put(K_MASTER_PASSWORD, masterPassword);
            put(K_SAVE_MASTER_PASSWORD, saveMasterPassword);
            put(K_USER, adminUser);
            put(K_INSTANCE_PORT, instancePort);
            put(K_JMS_PORT, jmsPort);
            put(K_ORB_LISTENER_PORT, orbPort);
            put(K_HTTP_SSL_PORT, httpSSLPort);
            put(K_IIOP_SSL_PORT, iiopSSLPort);
            put(K_IIOP_MUTUALAUTH_PORT, iiopMutualAuthPort);
            put(K_JMX_PORT, jmxAdminPort);
            put(K_OSGI_SHELL_TELNET_PORT, osgiShellTelnetPort);
            put(K_JAVA_DEBUGGER_PORT, javaDebuggerPort);

            if (domainProperties != null) {
                for (String pname : domainProperties.stringPropertyNames()) {
                    put(pname, domainProperties.getProperty(pname));
                }
            }
        }
        catch (Exception ex) {
            throw new DomainException(ex);
        }
    }

    /**
     * This constructor is used at domain creation time only.
     */
    public DomainConfig(String domainName, String domainRoot,
            String adminUser, String adminPassword, String masterPassword,
            Boolean saveMasterPassword, String adminPort, String instancePort,
            Properties domainProperties) throws DomainException {
        this(domainName, domainRoot);
        put(K_ADMIN_PORT, adminPort);
        put(K_PASSWORD, adminPassword);
        put(K_MASTER_PASSWORD, masterPassword);
        put(K_SAVE_MASTER_PASSWORD, saveMasterPassword);
        put(K_USER, adminUser);
        put(K_INSTANCE_PORT, instancePort);
        _domainProperties = domainProperties;
    }

    public String getDomainName() {
        return super.getRepositoryName();
    }

    public String getDomainRoot() {
        return super.getRepositoryRoot();
    }

    public Map getPorts() {
        final Map result = new HashMap();
        for (Map.Entry<String, Object> p : entrySet()) {
            String key = p.getKey();
            if (key.toLowerCase(Locale.ENGLISH).endsWith("port")) {
                result.put(key, p.getValue());
            }
        }
        return result;
    }

    public String getProfile() {
        return ((String) get(K_PROFILE));
    }

    public void add(String key, Object value) {
       put(key, value);
    }

    public Properties getDomainProperties() {
    	if (_domainProperties == null) {
    		_domainProperties = new Properties();
    	}
    	return _domainProperties;
    }
}
