/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.config.util;

import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.SystemProperty;
import java.beans.PropertyVetoException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jvnet.hk2.config.TransactionFailure;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.util.net.NetUtils;
import static com.sun.enterprise.config.util.PortConstants.*;

/**
 * Port base utilities used by create-local-instance.  Similar to create-domain.
 * @author Jennifer
 */
public class PortBaseHelper {
    
    final private static LocalStringsImpl strings = new LocalStringsImpl(PortBaseHelper.class);

    public PortBaseHelper(Server instance, String portbase, boolean checkports, Logger logger) {
        portBase = portbase;
        checkPorts = checkports;
        _logger = logger;
        _server = instance;
    }

    public void verifyPortBase() throws TransactionFailure {
        if (usePortBase()) {
            final int portbase = convertPortStr(portBase);
            setOptionsWithPortBase(portbase);
        }
    }

    public String getAdminPort() {
        return adminPort;
    }

    public String getInstancePort() {
        return instancePort;
    }

    public String getHttpsPort() {
        return httpsPort;
    }

    public String getIiopPort() {
        return iiopPort;
    }

    public String getIiopsPort() {
        return iiopsPort;
    }

    public String getIiopmPort() {
        return iiopmPort;
    }

    public String getJmsPort() {
        return jmsPort;
    }

    public String getJmxPort() {
        return jmxPort;
    }

    public String getOsgiPort() {
        return osgiPort;
    }

    public String getDebugPort() {
        return debugPort;
    }

    /**
     * Converts the port string to port int
     *
     * @param port the port number
     * @return the port number as an int
     * @throws TransactionFailure if port string is not numeric
     */
    private int convertPortStr(final String port)
            throws TransactionFailure {
        try {
            return Integer.parseInt(port);
        } catch (Exception e) {
            throw new TransactionFailure(
                    strings.get("InvalidPortNumber", port));
        }
    }

    /**
     * Check if portbase option is specified.
     */
    private boolean usePortBase() throws TransactionFailure {
        if (portBase != null) {
            return true;
        }
        return false;
    }

    private void setOptionsWithPortBase(final int portbase)
            throws TransactionFailure {
        // set the option name and value in the options list
        verifyPortBasePortIsValid(ADMIN,
            portbase + PORTBASE_ADMINPORT_SUFFIX);
        adminPort = String.valueOf(portbase + PORTBASE_ADMINPORT_SUFFIX);

        verifyPortBasePortIsValid(HTTP,
            portbase + PORTBASE_INSTANCE_SUFFIX);
        instancePort = String.valueOf(portbase + PORTBASE_INSTANCE_SUFFIX);

        verifyPortBasePortIsValid(HTTPS,
            portbase + PORTBASE_HTTPSSL_SUFFIX);
        httpsPort = String.valueOf(portbase + PORTBASE_HTTPSSL_SUFFIX);

        verifyPortBasePortIsValid(IIOPS,
            portbase + PORTBASE_IIOPSSL_SUFFIX);
        iiopsPort = String.valueOf(portbase + PORTBASE_IIOPSSL_SUFFIX);

        verifyPortBasePortIsValid(IIOPM,
                portbase + PORTBASE_IIOPMUTUALAUTH_SUFFIX);
        iiopmPort = String.valueOf(portbase + PORTBASE_IIOPMUTUALAUTH_SUFFIX);

        verifyPortBasePortIsValid(JMS,
            portbase + PORTBASE_JMS_SUFFIX);
        jmsPort = String.valueOf(portbase + PORTBASE_JMS_SUFFIX);

        verifyPortBasePortIsValid(IIOP,
            portbase + PORTBASE_IIOP_SUFFIX);
        iiopPort = String.valueOf(portbase + PORTBASE_IIOP_SUFFIX);

        verifyPortBasePortIsValid(JMX,
            portbase + PORTBASE_JMX_SUFFIX);
        jmxPort = String.valueOf(portbase + PORTBASE_JMX_SUFFIX);

        verifyPortBasePortIsValid(OSGI,
            portbase + PORTBASE_OSGI_SUFFIX);
        osgiPort = String.valueOf(portbase + PORTBASE_OSGI_SUFFIX);

        verifyPortBasePortIsValid(DEBUG,
            portbase + PORTBASE_DEBUG_SUFFIX);
        debugPort = String.valueOf(portbase + PORTBASE_DEBUG_SUFFIX);
}

    /**
     * Verify that the portbase port is valid
     * Port must be greater than 0 and less than 65535.
     * This method will also check if the port is in used.
     *
     * @param portNum the port number to verify
     * @throws TransactionFailure if Port is not valid
     * @throws TransactionFailure if port number is not a numeric value.
     */
    private void verifyPortBasePortIsValid(String portName, int portNum)
            throws TransactionFailure {
        if (portNum <= 0 || portNum > PORT_MAX_VAL) {
            throw new TransactionFailure(
                strings.get("InvalidPortBaseRange", portNum, portName));
        }
        if (checkPorts && !NetUtils.isPortFree(portNum)) {
            throw new TransactionFailure(
                strings.get("PortBasePortInUse", portNum, portName));
        }
        _logger.log(Level.FINER,ConfigApiLoggerInfo.portBaseHelperPort, portNum);
    }

    public void setPorts() throws TransactionFailure, PropertyVetoException {
        if (portBase != null) {
            setSystemProperty(ADMIN, getAdminPort());
            setSystemProperty(HTTP, getInstancePort());
            setSystemProperty(HTTPS, getHttpsPort());
            setSystemProperty(IIOP, getIiopPort());
            setSystemProperty(IIOPM, getIiopmPort());
            setSystemProperty(IIOPS, getIiopsPort());
            setSystemProperty(JMS, getJmsPort());
            setSystemProperty(JMX, getJmxPort());
            setSystemProperty(OSGI, getOsgiPort());
            setSystemProperty(DEBUG, getDebugPort());
        }
    }

    private void setSystemProperty(String name, String value) throws TransactionFailure, PropertyVetoException {
        SystemProperty sp = _server.getSystemProperty(name);
        if (sp == null) {
            SystemProperty newSP = _server.createChild(SystemProperty.class);
            newSP.setName(name);
            newSP.setValue(value);
            _server.getSystemProperty().add(newSP);
        } else {
            //Don't change the system property if it already exists - leave the original port assignment
            //sp.setName(name);
            //sp.setValue(value);
        }
    }

    private String portBase;
    private boolean checkPorts;
    private String adminPort;
    private String instancePort;
    private String httpsPort;
    private String iiopPort;
    private String iiopmPort;
    private String iiopsPort;
    private String jmsPort;
    private String jmxPort;
    private String osgiPort;
    private String debugPort;
    private Logger _logger;
    private Server _server;
}
