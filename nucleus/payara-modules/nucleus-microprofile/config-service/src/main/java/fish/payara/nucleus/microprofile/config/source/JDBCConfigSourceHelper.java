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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
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

import fish.payara.nucleus.microprofile.config.spi.JDBCConfigSourceConfiguration;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

public class JDBCConfigSourceHelper {

    private static final Logger logger = Logger.getLogger(JDBCConfigSourceHelper.class.getName());

    private PreparedStatement selectOne = null;
    private PreparedStatement selectAll = null;

    public JDBCConfigSourceHelper(JDBCConfigSourceConfiguration configuration) {
        String jdbcJNDIName = configuration.getJndiName();
        if (jdbcJNDIName != null && !jdbcJNDIName.trim().isEmpty()) {
            DataSource datasource = getDatasource(jdbcJNDIName);
            if (datasource != null) {
                String table = configuration.getTableName();
                String keyColumn = configuration.getKeyColumnName();
                String valueColumn = configuration.getValueColumnName();
                String queryOne = "select " + valueColumn + " from " + table + " where " + keyColumn + " = ?";
                String queryAll = "select " + keyColumn + ", " + valueColumn + " from " + table;
                try {
                    selectOne = datasource.getConnection().prepareStatement(queryOne);
                    selectAll = datasource.getConnection().prepareStatement(queryAll);
                } catch (SQLException ex) {
                    logger.info(ex.getLocalizedMessage());
                }
            }
        }
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
                // ignore the exception as another source may have the property
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
                // ignore it
            }
        }
        return result;
    }

    private DataSource getDatasource(String jndiName) {
        try {
            InitialContext ctx = new InitialContext();
            return (DataSource) ctx.lookup(jndiName);
        } catch (NamingException ex) {
            logger.warning("Could not find the datasource:" + ex.getMessage());
        }
        return null;
    }
}
