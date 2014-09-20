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
package com.sun.enterprise.admin.servermgmt.domain;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.enterprise.admin.servermgmt.DomainConfig;
import com.sun.enterprise.admin.servermgmt.DomainException;
import com.sun.enterprise.admin.servermgmt.SLogger;
import com.sun.enterprise.admin.servermgmt.domain.SubstitutableTokens;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.util.net.NetUtils;

public class DomainPortValidator {

    /* These properties are public interfaces, handle with care */
    private static final Logger _logger = SLogger.getLogger();
    private static final LocalStringsImpl _strings = new LocalStringsImpl(DomainPortValidator.class);

    public static final int PORT_MAX_VAL = 65535;

    private DomainConfig _domainConfig;
    private Properties _defaultProps;

    DomainPortValidator(DomainConfig domainConfig, Properties defaultProps) { 
        _domainConfig = domainConfig;
        _defaultProps = defaultProps;
    }

    /**
     * Validate's the port. If custom port value is not given then it retrieves
     * its default value. If the port is already been occupied then it picks
     * randomly available port value.
     *
     * @throws DomainException If any exception occurs in validation.
     */
    public void validateAndSetPorts() throws DomainException {
        Properties domainProperties = _domainConfig.getDomainProperties();
        try {
            // Validate and gets the port values.
            final Integer adminPortInt = getPort(domainProperties,
                    DomainConfig.K_ADMIN_PORT,
                    (String)_domainConfig.get(DomainConfig.K_ADMIN_PORT),
                    _defaultProps.getProperty(SubstitutableTokens.ADMIN_PORT_TOKEN_NAME),
                    "Admin");
            _domainConfig.add(DomainConfig.K_ADMIN_PORT, adminPortInt);

            final Integer instancePortInt = getPort(domainProperties,
                    DomainConfig.K_INSTANCE_PORT,
                    (String)_domainConfig.get(DomainConfig.K_INSTANCE_PORT),
                    _defaultProps.getProperty(SubstitutableTokens.HTTP_PORT_TOKEN_NAME),
                    "HTTP Instance");
            _domainConfig.add(DomainConfig.K_INSTANCE_PORT, instancePortInt);

            final Integer jmsPort = getPort(domainProperties,
                    DomainConfig.K_JMS_PORT, null,
                    _defaultProps.getProperty(SubstitutableTokens.JMS_PROVIDER_PORT_TOKEN_NAME),
                    "JMS");
            _domainConfig.add(DomainConfig.K_JMS_PORT, jmsPort);

            final Integer orbPort = getPort(domainProperties,
                    DomainConfig.K_ORB_LISTENER_PORT,
                    null,
                    _defaultProps.getProperty(SubstitutableTokens.ORB_LISTENER_PORT_TOKEN_NAME),
                    "IIOP");
            _domainConfig.add(DomainConfig.K_ORB_LISTENER_PORT, orbPort);

            final Integer httpSSLPort = getPort(domainProperties,
                    DomainConfig.K_HTTP_SSL_PORT, null,
                    _defaultProps.getProperty(SubstitutableTokens.HTTP_SSL_PORT_TOKEN_NAME),
                    "HTTP_SSL");
            _domainConfig.add(DomainConfig.K_HTTP_SSL_PORT, httpSSLPort);

            final Integer iiopSSLPort = getPort(domainProperties,
                    DomainConfig.K_IIOP_SSL_PORT, null,
                    _defaultProps.getProperty(SubstitutableTokens.ORB_SSL_PORT_TOKEN_NAME),
                    "IIOP_SSL");
            _domainConfig.add(DomainConfig.K_IIOP_SSL_PORT, iiopSSLPort);

            final Integer iiopMutualAuthPort = getPort(domainProperties,
                    DomainConfig.K_IIOP_MUTUALAUTH_PORT, null,
                    _defaultProps.getProperty(SubstitutableTokens.ORB_MUTUALAUTH_PORT_TOKEN_NAME),
                    "IIOP_MUTUALAUTH");
            _domainConfig.add(DomainConfig.K_IIOP_MUTUALAUTH_PORT, iiopMutualAuthPort);

            final Integer jmxPort = getPort(domainProperties,
                    DomainConfig.K_JMX_PORT, null,
                    _defaultProps.getProperty(SubstitutableTokens.JMX_SYSTEM_CONNECTOR_PORT_TOKEN_NAME),
                    "JMX_ADMIN");
            _domainConfig.add(DomainConfig.K_JMX_PORT, jmxPort);

            final Integer osgiShellTelnetPort = getPort(domainProperties,
                    DomainConfig.K_OSGI_SHELL_TELNET_PORT, null,
                    _defaultProps.getProperty(SubstitutableTokens.OSGI_SHELL_TELNET_PORT_TOKEN_NAME),
                    "OSGI_SHELL");
            _domainConfig.add(DomainConfig.K_OSGI_SHELL_TELNET_PORT, osgiShellTelnetPort);

            final Integer javaDebuggerPort = getPort(domainProperties,
                    DomainConfig.K_JAVA_DEBUGGER_PORT, null,
                    _defaultProps.getProperty(SubstitutableTokens.JAVA_DEBUGGER_PORT_TOKEN_NAME),
                    "JAVA_DEBUGGER");
            _domainConfig.add(DomainConfig.K_JAVA_DEBUGGER_PORT, javaDebuggerPort);

            checkPortPrivilege(new Integer[]{
                    adminPortInt, instancePortInt, jmsPort, orbPort, httpSSLPort,
                    jmsPort, orbPort, httpSSLPort, iiopSSLPort,
                    iiopMutualAuthPort, jmxPort, osgiShellTelnetPort, javaDebuggerPort
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
            port = convertPortStr(defaultPort);
            defaultPortUsed = true;
        }
        Boolean checkPorts = (Boolean)_domainConfig.get(DomainConfig.K_VALIDATE_PORTS);
        if (checkPorts && !NetUtils.isPortFree(port)) {
            int newport = NetUtils.getFreePort();
            if (portNotSpecified) {
                if (defaultPortUsed) {
                    _logger.log(Level.INFO, SLogger.DEFAULT_PORT_IN_USE,
                            new Object[] {name, defaultPort, Integer.toString(newport)});
                }
                else {
                    _logger.log(Level.INFO, SLogger.PORT_NOT_SPECIFIED,
                            new Object[] {name, Integer.toString(newport)});
                }
            }
            else if (invalidPortSpecified) {
                _logger.log(Level.INFO, SLogger.INVALID_PORT_RANGE,
                        new Object[] {name, Integer.toString(newport)});
            }
            else {
                _logger.log(Level.INFO, SLogger.PORT_IN_USE,
                        new Object[] {name, Integer.toString(port), Integer.toString(newport)});
            }
            port = newport;
        }
        else if (defaultPortUsed) {
            _logger.log(Level.INFO, SLogger.USING_DEFAULT_PORT,
                    new Object[] {name, Integer.toString(port)});
        }
        else {
            _logger.log(Level.INFO, SLogger.USING_PORT,
                    new Object[] {name, Integer.toString(port)});
        }

        if (properties != null) {
            properties.remove(key);
        }
        return Integer.valueOf(port);
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
        }
        catch (Exception e) {
            throw new DomainException(
                    _strings.get("InvalidPortNumber", port));
        }
    }

    /**
     * Check if any of the port values are below 1024. If below 1024, then
     * display a warning message.
     */
    private void checkPortPrivilege(final Integer[] ports) {
        for (Integer port : ports) {
            final int p = port.intValue();
            if (p < 1024) {
                _logger.warning(_strings.get("PortPrivilege"));
                // display this message only once.
                // so break once this message is displayed.
                break;
            }
        }
    }
}
