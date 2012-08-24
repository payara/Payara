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

package com.sun.gjc.spi.jdbc40;

import com.sun.gjc.spi.JdbcObjectsFactory;
import com.sun.gjc.spi.ManagedConnectionFactoryImpl;
import com.sun.gjc.spi.ManagedConnectionImpl;
import com.sun.gjc.spi.base.ConnectionHolder;

import com.sun.gjc.util.SQLTraceDelegator;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Factory to create jdbc40 connection & datasource
 */
public class Jdbc40ObjectsFactory extends JdbcObjectsFactory {
    //indicates whether JDBC 3.0 Connection (and hence JDBC 3.0 DataSource) is used
    private boolean jdbc30Connection;
    //indicates whether detection of JDBC 3.0 Datasource in JDK 1.6 is done or not
    private boolean initJDBC30Connection;

    /**
     * To get an instance of ConnectionHolder40.<br>
     * Will return a ConnectionHolder40 with or without wrapper<br>
     *
     * @param conObject         Connection
     * @param mcObject          ManagedConnection
     * @param criObject         Connection Request Info
     * @param statementWrapping Whether to wrap statement objects or not.
     * @return ConnectionHolder
     */
    public ConnectionHolder getConnection(Connection conObject,
                                          ManagedConnectionImpl mcObject,
                                          javax.resource.spi.ConnectionRequestInfo criObject,
                                          boolean statementWrapping,
                                          SQLTraceDelegator sqlTraceDelegator) {
        ConnectionHolder connection = null;
        if (!initJDBC30Connection) {
            detectJDBC30Connection(conObject, mcObject);
        }
        if (statementWrapping) {
            if (sqlTraceDelegator != null) {
                Class connIntf[] = new Class[]{java.sql.Connection.class};
                Connection proxiedConn = getProxiedConnection(conObject, connIntf, sqlTraceDelegator);
                connection = new ProfiledConnectionWrapper40(proxiedConn, mcObject,
                        criObject, jdbc30Connection, sqlTraceDelegator);
            } else {
                connection = new ConnectionWrapper40(conObject, mcObject, criObject, jdbc30Connection);
            }
        } else {
            connection = new ConnectionHolder40(conObject, mcObject, criObject, jdbc30Connection);
        }
        return connection;
    }

    /**
     * Returns a DataSource instance for JDBC 4.0
     *
     * @param mcfObject Managed Connection Factory
     * @param cmObject  Connection Manager
     * @return DataSource
     */
    public javax.sql.DataSource getDataSourceInstance(ManagedConnectionFactoryImpl mcfObject,
                                                      javax.resource.spi.ConnectionManager cmObject) {
        return new DataSource40(mcfObject, cmObject);
    }

    public boolean isJdbc30Connection() {
        return jdbc30Connection;
    }

    public void setJdbc30Connection(boolean jdbc30Connection) {
        this.jdbc30Connection = jdbc30Connection;
    }

    public boolean isJDBC30ConnectionDetected() {
        return initJDBC30Connection;
    }

    public void detectJDBC30Connection(Connection con, ManagedConnectionImpl mcObject) {

        String dataSourceProperty = mcObject.getManagedConnectionFactory().getJdbc30DataSource();
        if (dataSourceProperty != null) {
            setJdbc30Connection(Boolean.valueOf(dataSourceProperty));
            initJDBC30Connection = true;
        } else {
            try {
                Class paramClasses[] = new Class[]{Class.class};

                Method isWrapperMethod = con.getClass().getMethod("isWrapperFor", paramClasses);
                int modifiers = isWrapperMethod.getModifiers();
                setJdbc30Connection(Modifier.isAbstract(modifiers));
            } catch (NoSuchMethodException e) {
                setJdbc30Connection(true);
            } catch (AbstractMethodError e) {
                setJdbc30Connection(true);
            } catch (Throwable t) {
                setJdbc30Connection(true);
                _logger.log(Level.WARNING, "jdbc.unexpected_exception_on_detecting_jdbc_version", t);
            } finally {
                initJDBC30Connection = true;
            }
        }
    }    
}
