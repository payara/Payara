/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
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
// Portions Copyright [2016-2019] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.admin.servermgmt.domain;

import com.sun.enterprise.admin.servermgmt.DomainConfig;
import com.sun.enterprise.admin.servermgmt.DomainException;
import com.sun.enterprise.admin.servermgmt.SLogger;
import com.sun.enterprise.config.util.PortConstants;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.util.net.NetUtils;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Checks that port is free and that the current user has permission to use it
 */
public class DomainPortValidator {

    /* These properties are public interfaces, handle with care */
    private static final Logger LOGGER = SLogger.getLogger();
    private static final LocalStringsImpl STRINGS = new LocalStringsImpl(DomainPortValidator.class);

    public static final int PORT_MAX_VAL = 65535;

    private final DomainConfig domainConfig;
    private final Properties defaultProps;

    DomainPortValidator(DomainConfig domainConfig, Properties defaultProps) { 
        this.domainConfig = domainConfig;
        this.defaultProps = defaultProps;
    }

    /**
     * Validates the port. If custom port value is not given then it retrieves
     * its default value. If the port is already been occupied then it picks
     * randomly available port value.
     *
     * @throws DomainException If any exception occurs in validation.
     */
    public void validateAndSetPorts() throws DomainException {
        Properties domainProperties = domainConfig.getDomainProperties();
        try {
            // Validate and gets the port values.
            final Integer adminPortInt = getPort(domainProperties,
                    DomainConfig.K_ADMIN_PORT,
                    (String) domainConfig.get(DomainConfig.K_ADMIN_PORT),
                    defaultProps.getProperty(SubstitutableTokens.ADMIN_PORT_TOKEN_NAME),
                    "Admin");
            domainConfig.add(DomainConfig.K_ADMIN_PORT, adminPortInt);

            final Integer instancePortInt = getPort(domainProperties,
                    DomainConfig.K_INSTANCE_PORT,
                    (String) domainConfig.get(DomainConfig.K_INSTANCE_PORT),
                    defaultProps.getProperty(SubstitutableTokens.HTTP_PORT_TOKEN_NAME),
                    "HTTP Instance");
            domainConfig.add(DomainConfig.K_INSTANCE_PORT, instancePortInt);

            final Integer jmsPort = getPort(domainProperties,
                    DomainConfig.K_JMS_PORT, null,
                    defaultProps.getProperty(SubstitutableTokens.JMS_PROVIDER_PORT_TOKEN_NAME), 
                    "JMS");
            domainConfig.add(DomainConfig.K_JMS_PORT, jmsPort);
            domainProperties.setProperty(PortConstants.JMS, jmsPort.toString());

            final Integer orbPort = getPort(domainProperties,
                    DomainConfig.K_ORB_LISTENER_PORT,
                    null,
                    defaultProps.getProperty(SubstitutableTokens.ORB_LISTENER_PORT_TOKEN_NAME),
                    "IIOP");
            domainConfig.add(DomainConfig.K_ORB_LISTENER_PORT, orbPort);

            final Integer httpSSLPort = getPort(domainProperties,
                    DomainConfig.K_HTTP_SSL_PORT, null,
                    defaultProps.getProperty(SubstitutableTokens.HTTP_SSL_PORT_TOKEN_NAME),
                    "HTTP_SSL");
            domainConfig.add(DomainConfig.K_HTTP_SSL_PORT, httpSSLPort);

            final Integer iiopSSLPort = getPort(domainProperties,
                    DomainConfig.K_IIOP_SSL_PORT, null,
                    defaultProps.getProperty(SubstitutableTokens.ORB_SSL_PORT_TOKEN_NAME),
                    "IIOP_SSL");
            domainConfig.add(DomainConfig.K_IIOP_SSL_PORT, iiopSSLPort);

            final Integer iiopMutualAuthPort = getPort(domainProperties,
                    DomainConfig.K_IIOP_MUTUALAUTH_PORT, null,
                    defaultProps.getProperty(SubstitutableTokens.ORB_MUTUALAUTH_PORT_TOKEN_NAME),
                    "IIOP_MUTUALAUTH");
            domainConfig.add(DomainConfig.K_IIOP_MUTUALAUTH_PORT, iiopMutualAuthPort);

            final Integer jmxPort = getPort(domainProperties,
                    DomainConfig.K_JMX_PORT, null,
                    defaultProps.getProperty(SubstitutableTokens.JMX_SYSTEM_CONNECTOR_PORT_TOKEN_NAME),
                    "JMX_ADMIN");
            domainConfig.add(DomainConfig.K_JMX_PORT, jmxPort);

            final Integer osgiShellTelnetPort = getPort(domainProperties,
                    DomainConfig.K_OSGI_SHELL_TELNET_PORT, null,
                    defaultProps.getProperty(SubstitutableTokens.OSGI_SHELL_TELNET_PORT_TOKEN_NAME),
                    "OSGI_SHELL");
            domainConfig.add(DomainConfig.K_OSGI_SHELL_TELNET_PORT, osgiShellTelnetPort);

            final Integer javaDebuggerPort = getPort(domainProperties,
                    DomainConfig.K_JAVA_DEBUGGER_PORT, null,
                    defaultProps.getProperty(SubstitutableTokens.JAVA_DEBUGGER_PORT_TOKEN_NAME),
                    "JAVA_DEBUGGER");
            domainConfig.add(DomainConfig.K_JAVA_DEBUGGER_PORT, javaDebuggerPort);

            final Integer hazelcastDasPortInt = getPort(domainProperties,
                    DomainConfig.K_HAZELCAST_DAS_PORT,
                    (String) domainConfig.get(DomainConfig.K_HAZELCAST_DAS_PORT),
                    defaultProps.getProperty(SubstitutableTokens.HAZELCAST_DAS_PORT_TOKEN_NAME),
                    "Hazelcast DAS");
            domainConfig.add(DomainConfig.K_HAZELCAST_DAS_PORT, hazelcastDasPortInt);

            final Integer hazelcastStartPortInt = getPort(domainProperties,
                    DomainConfig.K_HAZELCAST_START_PORT,
                    (String) domainConfig.get(DomainConfig.K_HAZELCAST_START_PORT),
                    defaultProps.getProperty(SubstitutableTokens.HAZELCAST_START_PORT_TOKEN_NAME),
                    "Hazelcast Start");
            domainConfig.add(DomainConfig.K_HAZELCAST_START_PORT, hazelcastStartPortInt);

            checkPortPrivilege(new Integer[]{
                adminPortInt, instancePortInt, jmsPort, orbPort, httpSSLPort,
                jmsPort, orbPort, httpSSLPort, iiopSSLPort,
                iiopMutualAuthPort, jmxPort, osgiShellTelnetPort, javaDebuggerPort,
                hazelcastDasPortInt, hazelcastStartPortInt
            });
        } catch (Exception ex) {
            throw new DomainException(ex);
        }
    }

    /**
     * Get port from the properties option or default or free port.
     *
     * @param properties properties from command line
     * @param key key for the type of port
     * @param portStr the port as a string, or null to get from properties
     * @param defaultPort default port to use
     * @param name name of port
     * @throws DomainException if error in retrieving port value.
     */
    private Integer getPort(Properties properties,
            String key,
            String portStr,
            String defaultPort,
            String name) throws DomainException {
        int port = 0;
        boolean portNotSpecified = false;
        boolean invalidPortSpecified = false;
        boolean defaultPortUsed = false;
        if ((portStr != null) && !portStr.equals("")) {
            port = convertPortStr(portStr);
            if ((port <= 0) || (port > PORT_MAX_VAL)) {
                invalidPortSpecified = true;
            }
        }
        else if (properties != null) {
            String property = properties.getProperty(key);
            if ((property != null) && !property.equals("")) {
                port = convertPortStr(property);
            }
            else {
                portNotSpecified = true;
            }
        }
        else {
            portNotSpecified = true;
        }
        if (portNotSpecified) {
            if (defaultPort == null || defaultPort.trim().isEmpty()) {
                throw new DomainException(STRINGS.get("MissingDefaultPort", key));
            }
            port = convertPortStr(defaultPort);
            defaultPortUsed = true;
        }
        Boolean checkPorts = (Boolean)domainConfig.get(DomainConfig.K_VALIDATE_PORTS);
        if (checkPorts && !NetUtils.isPortFree(port)) {
            int newport = NetUtils.getFreePort();
            if (portNotSpecified) {
                if (defaultPortUsed) {
                    LOGGER.log(Level.INFO, SLogger.DEFAULT_PORT_IN_USE,
                            new Object[] {name, defaultPort, Integer.toString(newport)});
                }
                else {
                    LOGGER.log(Level.INFO, SLogger.PORT_NOT_SPECIFIED,
                            new Object[] {name, Integer.toString(newport)});
                }
            }
            else if (invalidPortSpecified) {
                LOGGER.log(Level.INFO, SLogger.INVALID_PORT_RANGE,
                        new Object[] {name, Integer.toString(newport)});
            }
            else {
                LOGGER.log(Level.INFO, SLogger.PORT_IN_USE,
                        new Object[] {name, Integer.toString(port), Integer.toString(newport)});
            }
            port = newport;
        }
        else if (defaultPortUsed) {
            LOGGER.log(Level.INFO, SLogger.USING_DEFAULT_PORT,
                    new Object[] {name, Integer.toString(port)});
        }
        else {
            LOGGER.log(Level.INFO, SLogger.USING_PORT,
                    new Object[] {name, Integer.toString(port)});
        }

        if (properties != null) {
            properties.remove(key);
        }
        return port;
    }

    /**
     * Converts the port string to port int
     *
     * @param port the port number
     * @return the port number as an int
     * @throws DomainException if port string is not numeric
     */
    private int convertPortStr(final String port)
            throws DomainException {
        try {
            return Integer.parseInt(port);
        } catch (NumberFormatException e) {
            throw new DomainException(STRINGS.get("InvalidPortNumber", port));
        }
    }

    /**
     * Check if any of the port values are below 1024. If below 1024, then
     * display a warning message.
     */
    private void checkPortPrivilege(final Integer[] ports) {
        for (Integer port : ports) {
            if (port < 1024) {
                LOGGER.warning(STRINGS.get("PortPrivilege"));
                // display this message only once.
                // so break once this message is displayed.
                break;
            }
        }
    }
}
