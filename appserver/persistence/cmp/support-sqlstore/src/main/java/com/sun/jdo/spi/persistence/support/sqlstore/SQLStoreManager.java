/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

/*
 * SQLStoreManager.java
 *
 * Created on March 3, 2000
 *
 */

package com.sun.jdo.spi.persistence.support.sqlstore;

// use internal version: import com.sun.jdo.api.persistence.support.Transaction;

import com.sun.jdo.api.persistence.support.*;
import com.sun.jdo.spi.persistence.support.sqlstore.database.DBVendorType;
import com.sun.jdo.spi.persistence.support.sqlstore.model.*;
import com.sun.jdo.spi.persistence.support.sqlstore.sql.RetrieveDescImpl;
import com.sun.jdo.spi.persistence.support.sqlstore.sql.UpdateObjectDescImpl;
import com.sun.jdo.spi.persistence.support.sqlstore.sql.concurrency.Concurrency;
import com.sun.jdo.spi.persistence.support.sqlstore.sql.generator.*;
import com.sun.jdo.spi.persistence.utility.StringHelper;
import com.sun.jdo.spi.persistence.utility.logging.Logger;
import org.glassfish.persistence.common.I18NHelper;

import java.sql.*;
import java.util.*;

/**
 * <P>This class connects to a persistent store. It supports
 * relational databases such as Oracle and MS SQLServer. This class
 * knows how to generate SQL statements to access and manipulate
 * objects stored in a relational database.
 */
public class SQLStoreManager implements PersistenceStore {

    /** Cache holding SQLStore model information. */
    private ConfigCache configCache;

    /** Encapsulates database type. */
    private DBVendorType vendorType;

    /** The logger. */
    private static Logger logger = LogHelperSQLStore.getLogger();

    /** The sql logger. */
    private static Logger sqlLogger = LogHelperSQLStore.getSqlLogger();

    /** I18N message handler. */
    private final static ResourceBundle messages = I18NHelper.loadBundle(
            SQLStoreManager.class);

    /** Fetch size for query statements. */
    private static int fetchSize =
      Integer.getInteger("com.sun.jdo.spi.persistence.support.sqlstore.SQLStoreManager.fetchSize", // NOI18N
                            -1).intValue(); // -1 not set

    /**
     * Returns the sqlstore model for class <code>classType</code>.
     * Sqlstore model information is cached. If the model cache does
     * not already hold a model instance for the particular class,
     * a new instance is created, initialized and put into the cache.
     * The access to the model cache is synchronized.
     */
    public PersistenceConfig getPersistenceConfig(Class classType) {
        if (logger.isLoggable(Logger.FINER)) {
            logger.finer("sqlstore.sqlstoremanager.getpersistenceconfig",
                    classType.getName()); // NOI18N
        }
        return configCache.getPersistenceConfig(classType);
    }

    /**  
     * @inheritDoc
     */  
    public ConfigCache getConfigCache() {
        return configCache;
    }

    /**
     * Executes the list of SQL requests contained in <code>actions</code>.
     * Requests can be INSERT, UPDATE or DELETE operations.
     *
     * @exception JDODataStoreException
     *         Will be thrown in case of errors or if the affected rows are
     *         less than the minimum rows required.
     */
    public void execute(PersistenceManager pm, Collection actions) {
        Iterator iter = actions.iterator();

        while (iter.hasNext()) {
            ActionDesc action = (ActionDesc) iter.next();

            if (action instanceof UpdateObjectDescImpl) {
                UpdateObjectDescImpl request = (UpdateObjectDescImpl) action;
                UpdateQueryPlan plan = new UpdateQueryPlan(request, this);

                plan.build();

                for (int i = 0, size = plan.statements.size(); i < size; i++) {
                    UpdateStatement s = (UpdateStatement) plan.statements.get(i);

                    if (s != null) {
                        executeUpdate(pm, s, request);
                    }
                }
            } else {
                throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                                                    "core.generic.notinstanceof", // NOI18N
                                                    action.getClass().getName(),
                                                    "UpdateObjectDescImpl")); // NOI18N
            }
        }
    }

    /**
     *
     */
    private void rollbackXact(Transaction tran) {

        try {
            tran.setRollbackOnly();
            //tran.rollback();
        } catch (Exception e) {
        }
    }

    /**
     * Executes the SQL text contained in <code>updateStatement</code>
     * against the database. Data used for placeholders in the
     * updateStatement is passed in the <code>updateDesc</code> parameter
     * and bound to the statement before execution. Can be used for
     * delete, insert, or update SQL statements, but not for select
     * SQL statements as these return result values with which
     * executeUpdate is not prepared to deal.
     *
     * @param pm Persistence manager holding the current transaction
     * and connection.
     * @param updateStatement The INSERT, UPDATE or DELETE statement.
     * @param updateDesc Update updateDesc holding the affected state
     * manager.
     * @exception JDODataStoreException Will be thrown in case of
     * errors or if the affected rows are less than the minimum rows
     * required.
     */
    private void executeUpdate(PersistenceManager pm,
                               UpdateStatement updateStatement,
                               UpdateObjectDescImpl updateDesc) {
        int affectedRows = 0;
        boolean debug = logger.isLoggable();

        if (debug) {
            logger.fine("sqlstore.sqlstoremanager.executeupdate"); // NOI18N
        }

        String sqlText = updateStatement.getText();
        if (sqlText.length() > 0) {
            if (sqlLogger.isLoggable()) {
                sqlLogger.fine(updateStatement.getFormattedSQLText());
            }

            Transaction tran = (Transaction) pm.currentTransaction();
            Connection conn = tran.getConnection();
            DBStatement s = null;
            boolean preparationSuccessful = false;

            try {
                // Statement preparation.
                s = new DBStatement(conn, sqlText, tran.getUpdateTimeout());
                updateStatement.bindInputValues(s);
                preparationSuccessful = true;

                // Excecution.
                affectedRows = s.executeUpdate();

                // If the affectedRows is less than the minimum rows required,
                // we need to abort and throw an exception.
                if (affectedRows < updateStatement.minAffectedRows) {
                    // Mark the request failed.
                    updateDesc.setVerificationFailed();

                    rollbackXact(tran);
                    throwJDOConcurrentAccessException(sqlText);
                }
            } catch (SQLException e) {
                // As we want to verify against the data store only,
                // there is no invalidation necessary if the
                // exception happened before statement execution.
                if (preparationSuccessful) {
                    updateDesc.setVerificationFailed();
                }

                rollbackXact(tran);
                throwJDOSqlException(e, updateStatement.getFormattedSQLText());
            } finally {
                close(s);
                closeConnection(tran, conn);
            }
        }

        if (debug) {
            logger.fine("sqlstore.sqlstoremanager.executeupdate.exit", // NOI18N
                    new Integer(affectedRows));
        }
    }

    /**
     *
     */
    public Class getClassByOidClass(Class oidType) {
        return configCache.getClassByOidClass(oidType);
    }

    /**
     *
     */
    public StateManager getStateManager(Class classType) {
        ClassDesc c = (ClassDesc) getPersistenceConfig(classType);

        if (c != null) {
            return c.newStateManagerInstance(this);
        }

        return null;
    }

    /**
     * Returns a new retrieve descriptor for anexternal (user) query.
     *
     * @param classType Type of the persistence capable class to be queried.
     * @return A new retrieve descriptor for anexternal (user) query.
     */
    public RetrieveDesc getRetrieveDesc(Class classType) {
        return new RetrieveDescImpl(classType, (ClassDesc) getPersistenceConfig(classType));
    }

    /**
     * Returns a new retrieve descriptor for anexternal (user) query.
     * This retrieve descriptor can be used to query for the foreign
     * field <code>name</code>.
     *
     * @param fieldName Name of the foreign field to be queried.
     * @param classType Persistence capable class including <code>fieldName</code>.
     * @return A new retrieve descriptor for anexternal (user) query.
     */
    public RetrieveDesc getRetrieveDesc(String fieldName, Class classType) {
        ClassDesc c = (ClassDesc) getPersistenceConfig(classType);

        if (c != null) {
            FieldDesc f = c.getField(fieldName);

            if (f instanceof ForeignFieldDesc) {
                ForeignFieldDesc ff = (ForeignFieldDesc) f;
                return getRetrieveDesc(ff.foreignConfig.getPersistenceCapableClass());
            }
        }

        return null;
    }

    /**
     */
    public UpdateObjectDesc getUpdateObjectDesc(Class classType) {
          return new UpdateObjectDescImpl(classType);
    }

    /**
     * @param databaseMetaData Instance of DatabaseMetaData
     * @param identifier identifier of the caller creating a new instance
     * of SQLStoreManager. Typically this is identifier of
     * PersistenceManagerFacory initializing this SQLStoreManager.
     */
    public SQLStoreManager(DatabaseMetaData databaseMetaData,
        String identifier) {
        super();
        configCache = new ConfigCacheImpl();
        setVendorType(databaseMetaData, identifier);
    }

    /**
     * @param databaseMetaData Instance of DatabaseMetaData
     * @param identifier identifier of the caller creating a new instance
     * of SQLStoreManager.
     * @see SQLStoreManager#SQLStoreManager(DatabaseMetaData, String)
     */
    private void setVendorType(DatabaseMetaData databaseMetaData,
        String identifier) {
        try {
            vendorType = new DBVendorType(databaseMetaData, identifier);

            if (logger.isLoggable()) {
                logger.fine("sqlstore.sqlstoremanager.vendortype",vendorType.getName()); // NOI18N

            }

        } catch (Exception e) {
            if (e instanceof JDOException) {
                throw (JDOException) e;
            } else {
                throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                        "core.configuration.getvendortypefailed"), e); // NOI18N
            }
        }
    }

    public DBVendorType getVendorType() {
        return vendorType;
    }

    /**
     * The retrieve method builds and executes the SQL query described by
     * the action parameter.
     *
     * @param action
     *     The action parameter holds the RetrieveDesc describing what
     *     should be selected from the database.
     *
     * @param parameters
     *     Query parameters.
     */
    public Object retrieve(PersistenceManager pm, RetrieveDesc action, ValueFetcher parameters) {

        if (action == null) {
            throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                    "core.generic.nullparam", "action")); // NOI18N
        }

        if (!(action instanceof RetrieveDescImpl)) {
            throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                    "core.generic.notinstanceof", // NOI18N
                    action.getClass().getName(),
                    "RetrieveDescImpl")); // NOI18N
        }

        RetrieveDescImpl retrieveAction = ((RetrieveDescImpl) action);
        ClassDesc config = retrieveAction.getConfig();
        Concurrency concurrency = config.getConcurrency(pm.isOptimisticTransaction());
        SelectQueryPlan plan = retrieveAction.buildQueryPlan(this, concurrency);
        ArrayList statements = plan.getStatements();
        Object result = null;

        SelectStatement s = (SelectStatement) statements.get(0);
        result = executeQuery(pm, s, concurrency, parameters);

        if ((plan.options & RetrieveDescImpl.OPT_AGGREGATE) == 0) {
            // This was a regular query, no aggregate.

            if ((plan.options & RetrieveDescImpl.OPT_DISTINCT) > 0) {
                // Perform manual DISTINCT if required

                if (((plan.options & RetrieveDescImpl.OPT_FOR_UPDATE) > 0 &&
                        !vendorType.isDistinctSupportedWithUpdateLock()) ) {

                    HashSet hash = new HashSet();
                    for (Iterator iter = ((Collection)result).iterator(); iter.hasNext(); ) {
                        Object temp = iter.next();
                        if (!hash.contains(temp)) {
                            hash.add(temp);
                        } else {
                            iter.remove();
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * The executeQuery method prepares and sends the SQL text contained in the
     * <code>statement</code> to the DB. Data used for placeholders in the
     * <code>statement</code> is passed in <code>parameters</code> and bound to
     * the statement before execution. The SQL statement is run within
     * a cursor and any result values returned are packaged into
     * BusinessClasses of the appropriate sub-class and returned as the
     * functional result.
     *
     * @param statement
     * The statement contains the text of the SQL statement to be executed.
     * @param parameters Query parameters.
     * @return The result of this query.
     */
    private Object executeQuery(PersistenceManager pm,
                                SelectStatement statement,
                                Concurrency concurrency,
                                ValueFetcher parameters) {

        Object result = null;
        boolean debug = logger.isLoggable();
        if (debug) {
            logger.fine("sqlstore.sqlstoremanager.executeQuery");  // NOI18N
        }

        String sqlText = statement.getText();
        if (sqlText.length() > 0) {
            if (sqlLogger.isLoggable()) {
                sqlLogger.fine(statement.getFormattedSQLText(parameters));
            }

            Transaction tran = null;
            if (concurrency != null) {
                // This is a no op currently as all Concurrency* classes
                // have no code in this method and always returns null.
                tran = concurrency.suspend();
            }

            if (tran == null) {
                tran = (Transaction) pm.currentTransaction();
            }

            ResultSet resultData = null;
            DBStatement s = null;
            Connection conn = tran.getConnection();
            try {
                // prepare Statement including SELECT Statement timeout
                s = new DBStatement(conn, sqlText, tran.getQueryTimeout());

                // Set the inputValues values (constraints in this case).
                statement.bindInputValues(s, parameters);

                // Tests setting the fetch size with values 0, 16, 32, 64,
                // showed degradations only.
                //s.handle.setFetchSize(<fetch_size>);
                //s.handle.setFetchDirection(ResultSet.FETCH_FORWARD);
                if (fetchSize > -1) {
                    s.getPreparedStatement().setFetchSize(fetchSize);
                }

                if (statement.isColumnTypeDefinitionNeeded()) {
                    vendorType.getSpecialDBOperation().defineColumnTypeForResult(
                        s.getPreparedStatement(), statement.getColumnRefs());
                }

                resultData = s.executeQuery();

                if (concurrency != null) {
                    // This is a no op currently as all Concurrency* classes
                    // have no code in this method.
                    concurrency.resume(tran);
                }

                SelectQueryPlan plan = (SelectQueryPlan) statement.getQueryPlan();
                result = plan.getResult(pm, resultData);
            } catch (SQLException e) {
                throwJDOSqlException(e, statement.getFormattedSQLText(parameters));
            } finally {
                close(resultData);
                close(s);
                closeConnection(tran, conn);
            }
        }

        if (debug) {
            logger.fine("sqlstore.sqlstoremanager.executeQuery.exit"); // NOI18N
        }

        return result;
    }

    // -------------------------------------
    // Methods added to support batch update
    // -------------------------------------

    /**
     * Retrieves the update query plan for the specified request and
     * calls executeUpdateBatch for all statements in this plan.
     * @param pm the persistence manager
     * @param request the request corresponding with the current state manager
     * @param forceFlush all in the update query plan must be executed
     */
    public void executeBatch(PersistenceManager pm,
                             UpdateObjectDesc request,
                             boolean forceFlush)
    {
        boolean cleanup = true;
        UpdateObjectDescImpl objectRequest = null;

        if (request instanceof UpdateObjectDescImpl) {
            objectRequest = (UpdateObjectDescImpl) request;
        } else {
            throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                "core.generic.notinstanceof", // NOI18N
                request.getClass().getName(),
                "UpdateObjectDescImpl")); // NOI18N
        }

        ClassDesc config = objectRequest.getConfig();
        UpdateQueryPlan plan = config.getUpdateQueryPlan(objectRequest, this);
        Transaction tran = (Transaction) pm.currentTransaction();
        Connection conn = tran.getConnection();

        // Flag 'doFlush' indicates that executeUpdateBatch should call
        // executeBatch on the PreparedStatement. 'doFlush' is true, if
        // - forceFlush is true, this means we cannot reuse the
        //   DBStatement for the next state manager, OR
        // - the batch threshold is exeeceded.
        boolean doFlush = forceFlush || plan.checkBatchThreshold(tran);

        try {
            for (int i = 0, size = plan.statements.size(); i < size; i++) {
                UpdateStatement s = (UpdateStatement) plan.statements.get(i);
                executeUpdateBatch(tran, conn, s, objectRequest, doFlush);
            }

            // At this point we know the batch update was successful.
            // We close DBStatements and it's related PreparedStatement, if
            // they are not going to be reused for flushing the next state
            // manager. In this case the flag 'forceFlush' is true. So we
            // can do the cleanup, if 'forceFlush' is true. We cannot use
            // the flag 'doFlush', because this flag might be true, because
            // we exceeded the batch threshold. In this case we want to
            // keep the DBStatement.
            cleanup = forceFlush;
        } finally {
            if (cleanup)
                closeDBStatements(plan, tran);
            closeConnection(tran, conn);
        }
    }

    /**
     * Binds the specified update descriptor to the specified statement and
     * calls method addBatch on this statement.
     * @param tran the transaction
     * @param conn the connection
     * @param updateStatement the statement
     * @param updateDesc the update descriptor
     * @param doFlush determines if the statement must be executed
     */
    private void executeUpdateBatch(Transaction tran,
                                    Connection conn,
                                    UpdateStatement updateStatement,
                                    UpdateObjectDescImpl updateDesc,
                                    boolean doFlush)
    {
        int[] affectedRows = null;
        boolean debug = logger.isLoggable();

        if (debug) {
            logger.fine("sqlstore.sqlstoremanager.executeupdatebatch"); // NOI18N
        }

        String sqlText = updateStatement.getText();
        if (sqlText.length() > 0) {
            if (sqlLogger.isLoggable()) {
                String formattedText = updateStatement.getFormattedSQLText(updateDesc);
                if (doFlush) {
                    sqlLogger.fine("sqlstore.sqlstoremanager.executeupdatebatch.flushbatch", formattedText);
                } else {
                    sqlLogger.fine("sqlstore.sqlstoremanager.executeupdatebatch.addbatch", formattedText);
                }
            }

            DBStatement s = null;

            try {
                // Batch preparation.
                s = updateStatement.getDBStatement(tran, conn);
                updateStatement.bindInputColumns(s, updateDesc);
                s.addBatch();

                if (doFlush) {
                    // Execution.
                    affectedRows = s.executeBatch();

                    // check affectedRows as returned by the database
                    for (int i = 0; i < affectedRows.length; i++) {
                        // If the affectedRows is less than the minimum rows required,
                        // we need to abort and throw an exception.
                        if (affectedRows[i] < updateStatement.minAffectedRows &&
                            affectedRows[i] != java.sql.Statement.SUCCESS_NO_INFO) {

                            rollbackXact(tran);
                            throwJDOConcurrentAccessException(sqlText);
                        }
                    }
                }
            } catch (SQLException e) {
                rollbackXact(tran);
                throwJDOSqlException(e, sqlText);
            }
        }

        if (debug) {
            if (doFlush) {
                logger.fine("sqlstore.sqlstoremanager.executeupdatebatch.exit.flush", '[' + // NOI18N
                        StringHelper.intArrayToSeparatedList(affectedRows, ",") + ']'); //NOI18N
            } else {
                logger.fine("sqlstore.sqlstoremanager.executeupdatebatch.exit"); //NOI18N
            }
        }
    }

    // -------------------------------------
    // Static support methods
    // -------------------------------------

    /**
     * Constructs the exception message including the executed SQL statement
     * <code>sqlText</code> and throws a JDODataStoreException passing the
     * original exception.
     *
     * @param e Exception from the data store.
     * @param sqlText Executed SQL statement.
     */
    static private void throwJDOSqlException(SQLException e, String sqlText) {

        String exceptionMessage = I18NHelper.getMessage(messages,
            "core.persistencestore.jdbcerror", sqlText); // NOI18N

        throw new JDODataStoreException(exceptionMessage, e);
    }

    /**
     * Determines the SQL operation (update/delete) from the
     * <code>sqlText</code> parameter and throws a JDODataStoreException.
     *
     * @param sqlText Executed SQL statement.
     */
    static private void throwJDOConcurrentAccessException(String sqlText) {
        String operation = sqlText.substring(0, sqlText.indexOf(' ')); // NOI18N

        throw new JDODataStoreException(I18NHelper.getMessage(messages,
                      "core.store.concurrentaccess", operation)); // NOI18N
    }

    /**
     * Closes the JDBC ResultSet <code>r</code>.
     * SQLExceptions are catched and logged.
     */
    static private void close(ResultSet r) {
        if (r != null) {
            try {
                r.close();
            } catch (SQLException ex) {
                // only log exception
                logger.finest(I18NHelper.getMessage(messages,
                        "sqlstore.sqlstoremanager.errorcloseresultset", // NOI18N
                        ex.getLocalizedMessage()));
            }
        }
    }

    /**
     * Closes the JDBC Statement <code>s</code>.
     * SQLExceptions are catched and logged.
     */
    static private void close(DBStatement s) {
        if (s != null) {
            try {
                s.close();
            } catch (SQLException ex) {
                // only log exception
                logger.finest(I18NHelper.getMessage(messages,
                        "sqlstore.sqlstoremanager.errorclosestatement", // NOI18N
                        ex.getLocalizedMessage()));
            }
        }
    }

    /**
     * Delegates the closure of the JDBC connection <code>c</code>
     * to the transaction <code>t</code>.
     */
    static private void closeConnection(Transaction t, Connection c) {
        if (t != null && c != null) {
            t.releaseConnection();
        }
    }

    /**
     * Removes all DBStatements for specified plan and closes the JDBC Statement
     * wrapped by the DBStatement.
     */
    static private void closeDBStatements(UpdateQueryPlan plan, Transaction tran) {
        if ((plan != null) && (tran != null)) {
            for (Iterator i = plan.getStatements().iterator(); i.hasNext(); ) {
                UpdateStatement updateStmt = (UpdateStatement)i.next();
                DBStatement s = updateStmt.removeDBStatement(tran);
                close(s);
            }
        }
    }

}
