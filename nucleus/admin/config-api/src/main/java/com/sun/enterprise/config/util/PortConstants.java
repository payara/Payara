/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2011 Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;

/**
 *
 * @author bnevins
 */
public final class PortConstants {

    private PortConstants() {
    }

    public static final int PORT_MAX_VAL = 65535;

    public static final int DEFAULT_HTTPSSL_PORT = 8181;
    public static final int DEFAULT_IIOPSSL_PORT = 3820;
    public static final int DEFAULT_IIOPMUTUALAUTH_PORT = 3920;
    public static final int DEFAULT_INSTANCE_PORT = 8080;
    public static final int DEFAULT_JMS_PORT = 7676;
    public static final int DEFAULT_IIOP_PORT = 3700;
    public static final int DEFAULT_JMX_PORT = 8686;
    public static final int DEFAULT_OSGI_SHELL_TELNET_PORT = 6666;
    public static final int DEFAULT_JAVA_DEBUGGER_PORT = 9009;

    public static final int PORTBASE_ADMINPORT_SUFFIX = 48;
    public static final int PORTBASE_HTTPSSL_SUFFIX = 81;
    public static final int PORTBASE_IIOPSSL_SUFFIX = 38;
    public static final int PORTBASE_IIOPMUTUALAUTH_SUFFIX = 39;
    public static final int PORTBASE_INSTANCE_SUFFIX = 80;
    public static final int PORTBASE_JMS_SUFFIX = 76;
    public static final int PORTBASE_IIOP_SUFFIX = 37;
    public static final int PORTBASE_JMX_SUFFIX = 86;
    public static final int PORTBASE_OSGI_SUFFIX = 66;
    public static final int PORTBASE_DEBUG_SUFFIX = 9;
    
    // these are the ports that we support handling conflicts for...
    public static final String ADMIN = "ASADMIN_LISTENER_PORT";
    public static final String HTTP = "HTTP_LISTENER_PORT";
    public static final String HTTPS = "HTTP_SSL_LISTENER_PORT";
    public static final String IIOP = "IIOP_LISTENER_PORT";
    public static final String IIOPM = "IIOP_SSL_MUTUALAUTH_PORT";
    public static final String IIOPS = "IIOP_SSL_LISTENER_PORT";
    public static final String JMS = "JMS_PROVIDER_PORT";
    public static final String JMX = "JMX_SYSTEM_CONNECTOR_PORT";
    public static final String OSGI = "OSGI_SHELL_TELNET_PORT";
    public static final String DEBUG = "JAVA_DEBUGGER_PORT";

    private static final String[] PORTS = new String[] {
        ADMIN, HTTP, HTTPS, IIOP, IIOPM, IIOPS, JMS, JMX, OSGI, DEBUG
    };

    public static final List<String> PORTSLIST = Arrays.asList(PORTS);
}
