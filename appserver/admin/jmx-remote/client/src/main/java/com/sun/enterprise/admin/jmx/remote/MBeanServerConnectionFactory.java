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
 * $Header: /cvs/glassfish/jmx-remote/rjmx-impl/src/java/com/sun/enterprise/admin/jmx/remote/MBeanServerConnectionFactory.java,v 1.4 2005/12/25 04:26:30 tcfujii Exp $
 * $Revision: 1.4 $
 * $Date: 2005/12/25 04:26:30 $
*/

package com.sun.enterprise.admin.jmx.remote;

import java.util.Map;
import javax.management.remote.JMXServiceURL;
import com.sun.enterprise.admin.jmx.remote.comm.AuthenticationInfo;
import com.sun.enterprise.admin.jmx.remote.comm.ConnectionFactory;
import com.sun.enterprise.admin.jmx.remote.comm.HttpConnectorAddress;
import com.sun.enterprise.admin.jmx.remote.internal.RemoteMBeanServerConnection;

import javax.management.MBeanServerConnection;

/** A Factory class that creates the instances of MBeanServerConnection depending upon the given JMXServiceURL {@link javax.management.remote.JMXServiceURL}.
 * The returned instances are proxies to actual remote MBeanServerConnection.
 * @author Kedar Mhaswade
 * @since S1AS8.0
 * @version 1.0
*/

final class MBeanServerConnectionFactory {
    
    private MBeanServerConnectionFactory() {
    }

    /**
     * The static factory method that creates instance of MBeanServerConnection.
     * This method always creates a new MBeanServerConnection instance every time
     * it is called. If the call succeeds, the returned instance can be used to
     * invoke any MBeanServerConnection method immediately.
     * @param env a Map containing all the necessary varialbes.
     * @param serviceUrl an instance of JMXServiceURL specifying the JMXConnector
     * @return a ready-to-call MBeanServerConnection instance.
     * @throws Exception, in case the connection could not be established to the server-side.
    */
    
    static MBeanServerConnection getRemoteMBeanServerConnection(Map env, JMXServiceURL serviceUrl) 
    throws Exception {
        return new RemoteMBeanServerConnection(env2HttpAddress(env, serviceUrl), env);
    }

    /** Sets the HttpAddress for the given Map and JMXServiceURL */
    
    private static HttpConnectorAddress env2HttpAddress(Map env, JMXServiceURL serviceUrl) {
/* BEGIN -- S1WS_MOD */
        final HttpConnectorAddress ad = new HttpConnectorAddress(serviceUrl.getHost(), serviceUrl.getPort(), isHttps(serviceUrl), serviceUrl.getURLPath());
/* END -- S1WS_MOD */
        ad.setAuthenticationInfo(env2AuthenticationInfo(env));
        return ( ad );
    }
    
    /** Sets the authentication information */
    
    private static AuthenticationInfo env2AuthenticationInfo(Map env) {
        final String user = (String) env.get(DefaultConfiguration.ADMIN_USER_ENV_PROPERTY_NAME);
        final String pwd  = (String) env.get(DefaultConfiguration.ADMIN_PASSWORD_ENV_PROPERTY_NAME);
        return ( new AuthenticationInfo(user, pwd) );
    }
	
	private static boolean isHttps(JMXServiceURL url) {
		return ( DefaultConfiguration.S1_HTTPS_PROTOCOL.equals(url.getProtocol()) );
	}
}
