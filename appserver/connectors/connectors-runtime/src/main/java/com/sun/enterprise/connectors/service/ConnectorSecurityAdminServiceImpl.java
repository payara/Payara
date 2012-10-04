/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.connectors.service;

import com.sun.enterprise.connectors.authentication.AuthenticationService;
import com.sun.enterprise.connectors.authentication.BasicPasswordAuthenticationService;
import com.sun.enterprise.deployment.AuthMechanism;
import com.sun.enterprise.deployment.ConnectorDescriptor;
import com.sun.enterprise.deployment.OutboundResourceAdapter;
import org.glassfish.resourcebase.resources.api.PoolInfo;

import java.util.Set;

/**
 * This is Security administration service. Performs funtionality of
 * security map creation, deletion.
 *
 * @author Srikanth P
 */


public class ConnectorSecurityAdminServiceImpl extends ConnectorService {

    /**
     * Default constructor
     */
    public ConnectorSecurityAdminServiceImpl() {
        super();
    }


    /**
     * Obtain the authentication service associated with rar module.
     * Currently only the BasicPassword authentication is supported.
     *
     * @param rarName  Rar module Name
     * @param poolInfo Name of the pool. Used for creation of
     *                 BasicPasswordAuthenticationService
     * @return AuthenticationService
     */
    public AuthenticationService getAuthenticationService(String rarName,
                                                          PoolInfo poolInfo) {

        ConnectorDescriptor cd = _registry.getDescriptor(rarName);
        OutboundResourceAdapter obra = cd.getOutboundResourceAdapter();
        Set authMechs = obra.getAuthMechanisms();
        for (Object authMech : authMechs) {
            AuthMechanism authMechanism = (AuthMechanism) authMech;
            String mech = authMechanism.getAuthMechType();
            if (mech.equals("BasicPassword")) {
                return new BasicPasswordAuthenticationService(rarName, poolInfo);
            }
        }
        return null;
    }
}
