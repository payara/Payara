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

import org.testng.Assert;
import org.testng.annotations.Test;

import com.sun.enterprise.admin.servermgmt.DomainConfig;
import com.sun.enterprise.admin.servermgmt.DomainException;
import com.sun.enterprise.admin.servermgmt.pe.SubstitutableTokens;

public class TestDomainPortValidator {

    private DomainPortValidator _portValidator = null;

    @Test (expectedExceptions = DomainException.class)
    public void testForNullPorts() throws Exception {
        DomainConfig domainConfig = new DomainConfig("test", null);
        domainConfig.add(DomainConfig.K_VALIDATE_PORTS, Boolean.TRUE);
        _portValidator = new DomainPortValidator(domainConfig, new Properties());
        _portValidator.validateAndSetPorts();
    }

    @Test (expectedExceptions = DomainException.class)
    public void testForNonNumericPort() throws Exception {
        DomainConfig domainConfig = new DomainConfig("test", null);
        domainConfig.add(DomainConfig.K_VALIDATE_PORTS, Boolean.TRUE);
        domainConfig.add(DomainConfig.K_ADMIN_PORT, "admin2");
        _portValidator = new DomainPortValidator(domainConfig, new Properties());
        _portValidator.validateAndSetPorts();
    }

    @Test (expectedExceptions = DomainException.class)
    public void testForNegativePort() throws Exception {
        DomainConfig domainConfig = new DomainConfig("test", null);
        domainConfig.add(DomainConfig.K_VALIDATE_PORTS, Boolean.TRUE);
        domainConfig.add(DomainConfig.K_ADMIN_PORT, "-2");
        _portValidator = new DomainPortValidator(domainConfig, new Properties());
        _portValidator.validateAndSetPorts();
    }

    @Test (expectedExceptions = DomainException.class)
    public void testForPortValueZero() throws Exception {
        DomainConfig domainConfig = new DomainConfig("test", null);
        domainConfig.add(DomainConfig.K_VALIDATE_PORTS, Boolean.TRUE);
        domainConfig.add(DomainConfig.K_ADMIN_PORT, "0");
        _portValidator = new DomainPortValidator(domainConfig, new Properties());
        _portValidator.validateAndSetPorts();
    }

    @Test (expectedExceptions = DomainException.class)
    public void testForMaxPort() throws Exception {
        DomainConfig domainConfig = new DomainConfig("test", null);
        domainConfig.add(DomainConfig.K_VALIDATE_PORTS, Boolean.TRUE);
        domainConfig.add(DomainConfig.K_ADMIN_PORT, String.valueOf((DomainPortValidator.PORT_MAX_VAL + 1)));
        _portValidator = new DomainPortValidator(domainConfig, new Properties());
        _portValidator.validateAndSetPorts();
    }

    @Test
    public void testOnlyWithDefaultPorts() throws Exception {
        Properties defautProps = new Properties();
        defautProps.put(SubstitutableTokens.ADMIN_PORT_TOKEN_NAME, "4848");
        defautProps.put(SubstitutableTokens.HTTP_SSL_PORT_TOKEN_NAME, "8181");
        defautProps.put(SubstitutableTokens.ORB_SSL_PORT_TOKEN_NAME, "3820");
        defautProps.put(SubstitutableTokens.ORB_MUTUALAUTH_PORT_TOKEN_NAME, "3920");
        defautProps.put(SubstitutableTokens.HTTP_PORT_TOKEN_NAME, "8080");
        defautProps.put(SubstitutableTokens.JMS_PROVIDER_PORT_TOKEN_NAME, "7676");
        defautProps.put(SubstitutableTokens.ORB_LISTENER_PORT_TOKEN_NAME, "3700");
        defautProps.put(SubstitutableTokens.JMX_SYSTEM_CONNECTOR_PORT_TOKEN_NAME, "8686");
        defautProps.put(SubstitutableTokens.OSGI_SHELL_TELNET_PORT_TOKEN_NAME, "6666");
        defautProps.put(SubstitutableTokens.JAVA_DEBUGGER_PORT_TOKEN_NAME, "9009");
        DomainConfig domainConfig = new DomainConfig("test", null);
        domainConfig.add(DomainConfig.K_VALIDATE_PORTS, Boolean.TRUE);
        _portValidator = new DomainPortValidator(domainConfig, defautProps);
        _portValidator.validateAndSetPorts();

        Assert.assertEquals(domainConfig.get(DomainConfig.K_ADMIN_PORT).toString(), "4848");
        Assert.assertEquals(domainConfig.get(DomainConfig.K_HTTP_SSL_PORT).toString(), "8181");
        Assert.assertEquals(domainConfig.get(DomainConfig.K_IIOP_SSL_PORT).toString(), "3820");
        Assert.assertEquals(domainConfig.get(DomainConfig.K_IIOP_MUTUALAUTH_PORT).toString(), "3920");
        Assert.assertEquals(domainConfig.get(DomainConfig.K_INSTANCE_PORT).toString(), "8080");
        Assert.assertEquals(domainConfig.get(DomainConfig.K_JMS_PORT).toString(), "7676");
        Assert.assertEquals(domainConfig.get(DomainConfig.K_ORB_LISTENER_PORT).toString(), "3700");
        Assert.assertEquals(domainConfig.get(DomainConfig.K_JMX_PORT).toString(), "8686");
        Assert.assertEquals(domainConfig.get(DomainConfig.K_OSGI_SHELL_TELNET_PORT).toString(), "6666");
        Assert.assertEquals(domainConfig.get(DomainConfig.K_JAVA_DEBUGGER_PORT).toString(), "9009");
    }

    @Test
    public void testWithBothDefaultPortsAndDomainParams() throws Exception {
        // default
        Properties defautProps = new Properties();
        defautProps.put(SubstitutableTokens.ADMIN_PORT_TOKEN_NAME, "4848");
        defautProps.put(SubstitutableTokens.HTTP_SSL_PORT_TOKEN_NAME, "8181");
        defautProps.put(SubstitutableTokens.ORB_SSL_PORT_TOKEN_NAME, "3820");
        defautProps.put(SubstitutableTokens.ORB_MUTUALAUTH_PORT_TOKEN_NAME, "3920");
        defautProps.put(SubstitutableTokens.HTTP_PORT_TOKEN_NAME, "8080");
        defautProps.put(SubstitutableTokens.JMS_PROVIDER_PORT_TOKEN_NAME, "7676");
        defautProps.put(SubstitutableTokens.ORB_LISTENER_PORT_TOKEN_NAME, "3700");
        defautProps.put(SubstitutableTokens.JMX_SYSTEM_CONNECTOR_PORT_TOKEN_NAME, "8686");
        defautProps.put(SubstitutableTokens.OSGI_SHELL_TELNET_PORT_TOKEN_NAME, "6666");
        defautProps.put(SubstitutableTokens.JAVA_DEBUGGER_PORT_TOKEN_NAME, "9009");

        DomainConfig domainConfig = new DomainConfig("test", null);
        domainConfig.add(DomainConfig.K_VALIDATE_PORTS, Boolean.TRUE);
        domainConfig.add(DomainConfig.K_ADMIN_PORT, "4849");
        Properties domainProps = domainConfig.getDomainProperties();
        // Params
        domainProps.put(DomainConfig.K_HTTP_SSL_PORT, "8182");
        domainProps.put(DomainConfig.K_IIOP_SSL_PORT, "3822");

        _portValidator = new DomainPortValidator(domainConfig, defautProps);
        _portValidator.validateAndSetPorts();

        Assert.assertEquals(domainConfig.get(DomainConfig.K_ADMIN_PORT).toString(), "4849");
        Assert.assertEquals(domainConfig.get(DomainConfig.K_HTTP_SSL_PORT).toString(), "8182");
        Assert.assertEquals(domainConfig.get(DomainConfig.K_IIOP_SSL_PORT).toString(), "3822");
        Assert.assertEquals(domainConfig.get(DomainConfig.K_IIOP_MUTUALAUTH_PORT).toString(), "3920");
        Assert.assertEquals(domainConfig.get(DomainConfig.K_INSTANCE_PORT).toString(), "8080");
        Assert.assertEquals(domainConfig.get(DomainConfig.K_JMS_PORT).toString(), "7676");
        Assert.assertEquals(domainConfig.get(DomainConfig.K_ORB_LISTENER_PORT).toString(), "3700");
        Assert.assertEquals(domainConfig.get(DomainConfig.K_JMX_PORT).toString(), "8686");
        Assert.assertEquals(domainConfig.get(DomainConfig.K_OSGI_SHELL_TELNET_PORT).toString(), "6666");
        Assert.assertEquals(domainConfig.get(DomainConfig.K_JAVA_DEBUGGER_PORT).toString(), "9009");
    }
}
