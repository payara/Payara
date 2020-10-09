/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.nucleus.microprofile.config.source;

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

    private static final String JDBC_JNDI_NAME = "jndi-name";
    private static final String JDBC_TABLE_NAME = "table-name";
    private static final String JDBC_KEY_COLUMN = "key-column";
    private static final String JDBC_VALUE_COLUMN = "value-column";

    private PreparedStatement selectOne = null;
    private PreparedStatement selectAll = null;

    public JDBCConfigSourceHelper(Map<String, String> config) {
        String jdbcJNDIName = config.get(JDBC_JNDI_NAME);
        if (jdbcJNDIName != null || !jdbcJNDIName.trim().isEmpty()) {
            DataSource datasource = getDatasource(jdbcJNDIName);
            if (datasource != null) {
                String table = config.get(JDBC_TABLE_NAME);
                String keyColumn = config.get(JDBC_KEY_COLUMN);
                String valueColumn = config.get(JDBC_VALUE_COLUMN);
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
