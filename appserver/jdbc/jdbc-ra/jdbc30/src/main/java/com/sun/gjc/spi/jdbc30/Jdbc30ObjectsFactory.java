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

package com.sun.gjc.spi.jdbc30;

import com.sun.gjc.spi.JdbcObjectsFactory;
import com.sun.gjc.spi.ManagedConnectionFactoryImpl;
import com.sun.gjc.spi.ManagedConnectionImpl;
import com.sun.gjc.spi.base.ConnectionHolder;

import com.sun.gjc.util.SQLTraceDelegator;
import java.sql.Connection;

/**
 * Factory to create jdbc30 connection & datasource
 */
public class Jdbc30ObjectsFactory extends JdbcObjectsFactory {

    /**
     * To get an instance of ConnectionHolder30.<br>
     * Will return a ConnectionHolder30 with or without wrapper<br>
     *
     * @param conObject         Connection
     * @param mcObject          ManagedConnection
     * @param criObject         ConnectionRequestInfo
     * @param statementWrapping Whether to wrap statement objects or not.
     * @return ConnectionHolder
     */
    public ConnectionHolder getConnection(Connection conObject,
                                          ManagedConnectionImpl mcObject,
                                          javax.resource.spi.ConnectionRequestInfo criObject,
                                          boolean statementWrapping,
                                          SQLTraceDelegator sqlTraceDelegator) {
        ConnectionHolder connection = null;

        if (statementWrapping) {
            if (sqlTraceDelegator != null) {
                Class connIntf[] = new Class[]{java.sql.Connection.class};
                Connection proxiedConn = getProxiedConnection(conObject, connIntf, sqlTraceDelegator);
                connection = new ProfiledConnectionWrapper30(proxiedConn, mcObject,
                        criObject, sqlTraceDelegator);
            } else {
                connection = new ConnectionWrapper30(conObject, mcObject, criObject);
            }
        } else {
            connection = new ConnectionHolder30(conObject, mcObject, criObject);
        }
        return connection;
    }

    /**
     * Returns a DataSource instance for JDBC 3.0
     *
     * @param mcfObject Managed Connection Factory
     * @param cmObject  Connection Manager
     * @return DataSource
     */
    public javax.sql.DataSource getDataSourceInstance(ManagedConnectionFactoryImpl mcfObject,
                                                      javax.resource.spi.ConnectionManager cmObject) {
        return new DataSource30(mcfObject, cmObject);
    }
}
