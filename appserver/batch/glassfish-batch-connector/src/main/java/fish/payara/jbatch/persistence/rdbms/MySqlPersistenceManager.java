/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.jbatch.persistence.rdbms;

import com.ibm.jbatch.spi.services.IBatchConfig;
import static fish.payara.jbatch.persistence.rdbms.JDBCQueryConstants.Q_SET_SCHEMA;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

/**
 *
 * MySQL Persistence Manager
 */

public class MySqlPersistenceManager extends JBatchJDBCPersistenceManager {

    @Override
    protected Map<String, String> getQueryMap(IBatchConfig batchConfig) {
        Map<String, String> result = super.getQueryMap(batchConfig);
        if (schema != null && schema.length() != 0) {
            result.put(Q_SET_SCHEMA, "USE " + schema);
        }
        return result;
    }

    @Override
    protected void setSchemaOnConnection(Connection connection) throws SQLException {
            PreparedStatement ps = null;
            ps = connection.prepareStatement(queryStrings.get(Q_SET_SCHEMA));
            ps.executeUpdate();
            ps.close();
    }
    
    //TODO create methods to check if MySQ tables exist on start up and if not create them.
    
    

}