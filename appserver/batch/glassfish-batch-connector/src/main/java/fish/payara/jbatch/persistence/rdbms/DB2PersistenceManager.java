/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.jbatch.persistence.rdbms;

import com.ibm.jbatch.container.exception.BatchContainerServiceException;
import com.ibm.jbatch.spi.services.IBatchConfig;

import static fish.payara.jbatch.persistence.rdbms.JDBCQueryConstants.Q_SET_SCHEMA;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;


/**
*
* DB2 Persistence Manager
*/

public class DB2PersistenceManager extends JBatchJDBCPersistenceManager {
	
	private static final String CLASSNAME = JBatchJDBCPersistenceManager.class.getName();

	private final static Logger logger = Logger.getLogger(CLASSNAME);
	
	private IBatchConfig batchConfig = null;
	
	 @Override
    public void init(IBatchConfig batchConfig) throws BatchContainerServiceException {
        logger.config("Entering CLASSNAME.init(), batchConfig =" + batchConfig);

        this.batchConfig = batchConfig;

        schema = batchConfig.getDatabaseConfigurationBean().getSchema();

        jndiName = batchConfig.getDatabaseConfigurationBean().getJndiName();

        // Load the table names and queries shared between different database types
        
        tableNames = getTableMap(batchConfig);

        queryStrings = getQueryMap(batchConfig);
        
        
		// put the create table strings into a hashmap
		createTableStrings = setCreateTableMap(batchConfig);
		
		createDB2Strings = setCreateDB2StringsMap(batchConfig);
		
        
        logger.config("JNDI name = " + jndiName);

        if (jndiName == null || jndiName.equals("")) {
            throw new BatchContainerServiceException("JNDI name is not defined.");
        }

        try {
            Context ctx = new InitialContext();
            dataSource = (DataSource) ctx.lookup(jndiName);

        } catch (NamingException e) {
            logger.severe("Lookup failed for JNDI name: " + jndiName
                    + ".  One cause of this could be that the batch runtime is incorrectly configured to EE mode when it should be in SE mode.");
            throw new BatchContainerServiceException(e);
        }

        
        try{
        	if (isDB2()) {
        		if (!isSchemaValid()) {
        			createSchema();
        		}
            checkDB2Tables();
        	}
        }catch (SQLException e) {
        	logger.severe(e.getLocalizedMessage());
        	throw new BatchContainerServiceException(e);
        }

        logger.config("Exiting CLASSNAME.init()");
    }
	
	 private boolean isDB2() throws SQLException {
	        logger.entering(CLASSNAME, "isDB2");
	        Connection conn = getConnectionToDefaultSchema();
	        DatabaseMetaData dbmd = conn.getMetaData();
	        String prodname= dbmd.getDatabaseProductName();
	        boolean db2 = prodname.toLowerCase().contains("db2");
	        logger.exiting(CLASSNAME, "isDB2",db2 );
	        return db2;
	}
	 
	 private void checkDB2Tables() throws SQLException {
		 
	        logger.entering(CLASSNAME, "checkDB2Tables");

	        createDB2TableNotExists(tableNames.get(CHECKPOINT_TABLE_KEY), createDB2Strings.get(DB2_CREATE_TABLE_CHECKPOINTDATA));
	        
	        
	        createDB2TableNotExists(tableNames.get(JOB_INSTANCE_TABLE_KEY), createDB2Strings.get(DB2_CREATE_TABLE_JOBINSTANCEDATA));
	        

	        createDB2TableNotExists(tableNames.get(EXECUTION_INSTANCE_TABLE_KEY),createDB2Strings.get(DB2_CREATE_TABLE_EXECUTIONINSTANCEDATA));
	       
	        
	        createDB2TableNotExists(tableNames.get(STEP_EXECUTION_INSTANCE_TABLE_KEY),createDB2Strings.get(DB2_CREATE_TABLE_STEPINSTANCEDATA));
	       
	        createDB2TableNotExists(tableNames.get(JOB_STATUS_TABLE_KEY), createDB2Strings.get(DB2_CREATE_TABLE_JOBSTATUS));
	        
	        createDB2TableNotExists(tableNames.get(STEP_STATUS_TABLE_KEY), createDB2Strings.get(DB2_CREATE_TABLE_STEPSTATUS) );

	        logger.exiting(CLASSNAME, "checkDB2Tables");
	}
	 
	 protected void createDB2TableNotExists(String tableName, String createTableStatement) throws SQLException {
	        logger.entering(CLASSNAME, "createDB2TableNotExists", new Object[]{tableName, createTableStatement});

	        Connection conn = getConnection();
	        Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_READ_ONLY);
	        String query = "select name from sysibm.systables where name="+  "\'" + tableName + "\'" + "and type = 'T'";;
	        ResultSet rs = stmt.executeQuery(query);
	        PreparedStatement ps = null;
	        
	        int rowcount = getTableRowCount(rs);
	       
	        // Create table if it does not exist
	        if(rowcount == 0){
	        	if (!rs.next()) {
	        		logger.log(Level.INFO, tableName + " table does not exists. Trying to create it.");
	        		ps = conn.prepareStatement(createTableStatement);
	        		ps.executeUpdate();
	        	}
	        }

	        cleanupConnection(conn, rs, ps);
	        logger.exiting(CLASSNAME, "createDB2TableNotExists");
	}


	 

}
