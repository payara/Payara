/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.internal.api;

import org.omg.CORBA.*;
import org.jvnet.hk2.annotations.*;

/**
 * Contract for ORB provider.
 *
 * @author Jerome Dochez
 */
@Contract
public interface ORBLocator {
    public static final String JNDI_CORBA_ORB_PROPERTY =
        "java.naming.corba.orb";
    public static final String JNDI_PROVIDER_URL_PROPERTY =
        "java.naming.provider.url";
    public static final String OMG_ORB_INIT_HOST_PROPERTY =
        "org.omg.CORBA.ORBInitialHost";
    public static final String OMG_ORB_INIT_PORT_PROPERTY =
        "org.omg.CORBA.ORBInitialPort";

    // Same as ORBConstants.FOLB_CLIENT_GROUP_INFO_SERVICE,
    // but we can't reference ORBConstants from the naming bundle!
    public static final String FOLB_CLIENT_GROUP_INFO_SERVICE =
        "FolbClientGroupInfoService" ;

    public static final String DEFAULT_ORB_INIT_HOST = "localhost";
    public static final String DEFAULT_ORB_INIT_PORT = "3700";


    // This property is true if SSL is required to be used by
    // non-EJB CORBA objects in the server.
    public static final String ORB_SSL_SERVER_REQUIRED =
            "com.sun.CSIV2.ssl.server.required";
    //
    // This property is true if client authentication is required by
    // non-EJB CORBA objects in the server.
    public static final String ORB_CLIENT_AUTH_REQUIRED =
            "com.sun.CSIV2.client.auth.required";

     // This property is true (in appclient Main)
    // if SSL is required to be used by clients.
    public static final String ORB_SSL_CLIENT_REQUIRED =
            "com.sun.CSIV2.ssl.client.required";

    /**
     * Get or create the default orb.  This can be called for any process type.  However,
     * protocol manager and CosNaming initialization only take place for the Server.
     * @return an initialized ORB instance
     */
    public ORB getORB();

    public int getORBPort(ORB orb);

    public String getORBHost(ORB orb);    
}
