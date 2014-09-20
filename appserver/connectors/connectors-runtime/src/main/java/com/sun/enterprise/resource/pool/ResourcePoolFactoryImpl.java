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

package com.sun.enterprise.resource.pool;


import com.sun.appserv.connectors.internal.api.ConnectorConstants.PoolType;
import com.sun.appserv.connectors.internal.api.PoolingException;
import com.sun.enterprise.connectors.ConnectorRuntime;
import com.sun.logging.LogDomains;
import org.glassfish.resourcebase.resources.api.PoolInfo;

import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Factory to create appropriate connection pool.
 *
 * @author Aditya Gore
 */
public class ResourcePoolFactoryImpl {

    //property to take care of switching off connection pooling in ACC
    //since 9.1
    private static final String SWITCH_OFF_ACC_CONNECTION_POOLING =
 	"com.sun.enterprise.connectors.SwitchoffACCConnectionPooling";
    private static String switchOffACCConnectionPoolingProperty =
            System.getProperty(SWITCH_OFF_ACC_CONNECTION_POOLING);

    private static Logger _logger = LogDomains.getLogger(ResourcePoolFactoryImpl.class,LogDomains.RSR_LOGGER);

    public static ResourcePool newInstance(PoolInfo poolInfo, PoolType pt, Hashtable env)
            throws PoolingException {

        if(ConnectorRuntime.getRuntime().isACCRuntime()){
            if("TRUE".equalsIgnoreCase(switchOffACCConnectionPoolingProperty))
                return new UnpooledResource( poolInfo, env );
        }

        ResourcePool pool = null;
        if( pt == PoolType.POOLING_DISABLED ) {
            return new UnpooledResource( poolInfo, env );
        }
        if ( pt == PoolType.ASSOCIATE_WITH_THREAD_POOL ) {
            pool = new AssocWithThreadResourcePool( poolInfo, env );
        } else {
            pool = new ConnectionPool( poolInfo, env );
        }

        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("Created a pool of type : " + pt);
        }
        return pool;
    }
}
