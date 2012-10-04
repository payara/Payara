/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jdbcruntime.service;

import com.sun.appserv.connectors.internal.api.ConnectorRuntimeException;
import com.sun.appserv.connectors.internal.api.ConnectorsUtil;
import com.sun.enterprise.connectors.ConnectorRuntime;
import com.sun.enterprise.connectors.util.ResourcesUtil;
import org.glassfish.jdbc.config.JdbcResource;
import org.glassfish.resourcebase.resources.api.ResourceInfo;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

public class JdbcDataSource implements DataSource {
    private ResourceInfo resourceInfo;
    private PrintWriter logWriter;
    private int loginTimeout;

    public void setResourceInfo(ResourceInfo resourceInfo) throws ConnectorRuntimeException {
        validateResource(resourceInfo);
        this.resourceInfo = resourceInfo;
    }

    private void validateResource(ResourceInfo resourceInfo) throws ConnectorRuntimeException {
        ResourcesUtil resourcesUtil = ResourcesUtil.createInstance();
        String jndiName = resourceInfo.getName();
        String suffix = ConnectorsUtil.getValidSuffix(jndiName);

        if(suffix != null){
            //Typically, resource is created without suffix. Try without suffix.
            String tmpJndiName = jndiName.substring(0, jndiName.lastIndexOf(suffix));
            if(resourcesUtil.getResource(tmpJndiName, resourceInfo.getApplicationName(),
                    resourceInfo.getModuleName(), JdbcResource.class) != null){
                return;
            }
        }

        if(resourcesUtil.getResource(resourceInfo, JdbcResource.class) == null){
            throw new ConnectorRuntimeException("Invalid resource : " + resourceInfo);
        }
    }

    public Connection getConnection() throws SQLException {
        return ConnectorRuntime.getRuntime().getConnection(resourceInfo);
    }

    public Connection getConnection(String username, String password) throws SQLException {
        return ConnectorRuntime.getRuntime().getConnection(resourceInfo, username, password);
    }

    public PrintWriter getLogWriter() throws SQLException {
        return logWriter;
    }

    public void setLogWriter(PrintWriter out) throws SQLException {
       this.logWriter = out;
    }

    public void setLoginTimeout(int seconds) throws SQLException {
       loginTimeout = seconds;
    }

    public int getLoginTimeout() throws SQLException {
        return loginTimeout;
    }
    public boolean isWrapperFor(Class<?> iface) throws SQLException{
       throw new SQLException("Not supported operation");
    }
    public <T> T unwrap(Class<T> iface) throws SQLException{
       throw new SQLException("Not supported operation");
    }

    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("Not supported operation");
    }
}
