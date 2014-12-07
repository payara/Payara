/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2014 C2B2 Consulting Limited. All rights reserved.

 The contents of this file are subject to the terms of the Common Development
 and Distribution License("CDDL") (collectively, the "License").  You
 may not use this file except in compliance with the License.  You can
 obtain a copy of the License at
 https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 or packager/legal/LICENSE.txt.  See the License for the specific
 language governing permissions and limitations under the License.

 When distributing the software, include this License Header Notice in each
 file and include the License file at packager/legal/LICENSE.txt.
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
