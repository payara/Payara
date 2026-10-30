/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2020] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.nucleus.microprofile.config.source;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import fish.payara.nucleus.microprofile.config.spi.JDBCConfigSourceConfiguration;

public class JDBCConfigSourceHelper implements Closeable {

    private static final Logger LOGGER = Logger.getLogger(JDBCConfigSourceHelper.class.getName());

    private final Connection connection;

    private final PreparedStatement selectOne;
    private final PreparedStatement selectAll;

    public JDBCConfigSourceHelper(JDBCConfigSourceConfiguration configuration) {

        String jdbcJNDIName = configuration.getJndiName();

        Connection connection = null;
        PreparedStatement selectOne = null;
        PreparedStatement selectAll = null;

        if (jdbcJNDIName != null && !jdbcJNDIName.trim().isEmpty()) {
            DataSource datasource = getDatasource(jdbcJNDIName);
            if (datasource != null) {
                String table = configuration.getTableName();
                String keyColumn = configuration.getKeyColumnName();
                String valueColumn = configuration.getValueColumnName();
                String queryOne = "select " + valueColumn + " from " + table + " where " + keyColumn + " = ?";
                String queryAll = "select " + keyColumn + ", " + valueColumn + " from " + table;
                try {
                    connection = datasource.getConnection();
                    selectOne = connection.prepareStatement(queryOne);
                    selectAll = connection.prepareStatement(queryAll);
                } catch (SQLException ex) {
                    LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
                }
            }
        }

        this.connection = connection;
        this.selectOne = selectOne;
        this.selectAll = selectAll;
    }

    public synchronized String getConfigValue(String propertyName) {
        if (selectOne != null) {
            try {
                selectOne.setString(1, propertyName);
                ResultSet resultSet = selectOne.executeQuery();
                if (resultSet.next()) {
                    return resultSet.getString(1);
                }
            } catch (SQLException ex) {
                LOGGER.log(Level.WARNING, "Error in config source SQL execution", ex);
            }
        }
        return null;
    }

    public synchronized Map<String, String> getAllConfigValues() {
        Map<String, String> result = new HashMap<>();
        if (selectAll != null) {
            try {
                ResultSet resultSet = selectAll.executeQuery();
                while (resultSet.next()) {
                    result.put(resultSet.getString(1), resultSet.getString(2));
                }
            } catch (SQLException ex) {
                LOGGER.log(Level.WARNING, "Error in config source SQL execution", ex);
            }
        }
        return result;
    }

    @Override
    public void close() throws IOException {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new IOException("Error closing JDBC connection from config source", e);
            }
        }
    }

    private DataSource getDatasource(String jndiName) {
        try {
            InitialContext ctx = new InitialContext();
            return (DataSource) ctx.lookup(jndiName);
        } catch (NamingException ex) {
            LOGGER.warning("Could not find the datasource:" + ex.getMessage());
        }
        return null;
    }
}
