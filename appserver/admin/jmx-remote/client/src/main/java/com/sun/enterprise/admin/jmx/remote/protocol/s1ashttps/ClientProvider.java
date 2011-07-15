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

/*
 * $Header: /cvs/glassfish/jmx-remote/rjmx-impl/src/java/com/sun/enterprise/admin/jmx/remote/protocol/s1ashttps/ClientProvider.java,v 1.3 2005/12/25 04:26:36 tcfujii Exp $
 * $Revision: 1.3 $
 * $Date: 2005/12/25 04:26:36 $
 */

package com.sun.enterprise.admin.jmx.remote.protocol.s1ashttps;

import javax.management.remote.JMXConnectorProvider;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;

import com.sun.enterprise.admin.jmx.remote.internal.UrlConnectorFactory;
import java.util.Map;
import java.io.IOException;

/** S1AS provides its own JSR 160 client provider to instantiate the supported instance of {@link JMXConnector}. 
 * It is arranged as per the algorithm
 * provided in {@link JMXConnectorFactory}. This instance of provider will
 * always use HTTPS as the transport.
 * @author Kedar Mhaswade
 * @since S1AS8.0
 * @version 1.0
*/

public class ClientProvider implements JMXConnectorProvider {
    
    /** Creates a new instance of ClientProvider */
    public ClientProvider() {
    }
    
    public JMXConnector newJMXConnector(JMXServiceURL serviceURL, Map environment)
    throws IOException {
		return ( UrlConnectorFactory.getHttpsConnector(serviceURL, environment) );
    }
}
