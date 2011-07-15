/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2010 Oracle and/or its affiliates. All rights reserved.
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

/* CVS information
 * $Header: /cvs/glassfish/jmx-remote/rjmx-impl/src/java/com/sun/enterprise/admin/jmx/remote/SunOneHttpJmxConnectorFactory.java,v 1.4 2005/12/25 04:26:30 tcfujii Exp $
 * $Revision: 1.4 $
 * $Date: 2005/12/25 04:26:30 $
*/

package com.sun.enterprise.admin.jmx.remote;

import java.util.Map;
import java.util.HashMap;
import java.io.IOException;

import javax.management.remote.JMXServiceURL;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import com.sun.enterprise.admin.jmx.remote.DefaultConfiguration;

/** A convenience class that knows how to setup the client side to get the reference of JMXConnector. 
 * This class is specific to Sun ONE Application Server 8.0. Any
 * client can use the following to initialize the S1AS 8.0 JSR 160 client.
 * This class lets the clients to do this under the hood and provide a {@link JMXConnectorFactory} like
 * API.
 * @author Kedar Mhaswade
 * @since S1AS8.0
 * @version 1.0
*/

public class SunOneHttpJmxConnectorFactory {

    /** Creates a new instance of SunOneHttpJmxConnectorFactory */
    private SunOneHttpJmxConnectorFactory() {
    }
    
    private static Map initEnvironment() {
        final Map env = new HashMap();
        final String PKGS = "com.sun.enterprise.admin.jmx.remote.protocol";
        env.put(JMXConnectorFactory.PROTOCOL_PROVIDER_PACKAGES, PKGS);
        env.put(DefaultConfiguration.HTTP_AUTH_PROPERTY_NAME, DefaultConfiguration.DEFAULT_HTTP_AUTH_SCHEME);
        
        return ( env );
    }

    public static JMXConnector connect(JMXServiceURL url, String user, String password) 
    throws IOException {
        return connect(url, user, password, null);
    }

    public static JMXConnector connect(JMXServiceURL url, String user, String password, Map extraEnv) 
    throws IOException {
        final Map env = initEnvironment();
        if (user != null) 
            env.put(DefaultConfiguration.ADMIN_USER_ENV_PROPERTY_NAME, user);
        if (password != null)
            env.put(DefaultConfiguration.ADMIN_PASSWORD_ENV_PROPERTY_NAME, password);
        if (extraEnv != null) env.putAll(extraEnv);
        
        return ( JMXConnectorFactory.connect(url, env) );
    }
}
