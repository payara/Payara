/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
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
 * NOTE: This class is not included in the build,
 * as the Oracle JDBC driver must be present for
 * compilation. A pre-compiled version is checked in
 * under
 * <cmp-basedir>/release/build/OracleSpecialDBOperation.jar.
 * This archive is unpacked for every build, and
 * must be updated on every change to this class.
 * Please see the ant script under
 * <cmp-basedir>/support/sqlstore/build.xml
 * for targets to update the pre-compiled version:
 *
 * - compile-oracle-special: compiles this class
 * - clean-oracle-special: cleans OracleSpecialDBOperation classes
 * - update-oracle-special: updates <cmp-basedir>/release/build/OracleSpecialDBOperation.jar
 *
 * Oracle's JDBC driver can be downloaded, e.g. the Oracle 
 * 10.1.0.4 JDBC driver can be retrieved from the URL
 * http://www.oracle.com/technology/software/tech/java/sqlj_jdbc/htdocs/jdbc101040.html
 * Please specify the Oracle JDBC driver location by defining
 * the property 'oracle-jdbc.jar' at the command line when
 * calling the ant targets.
 *
 * The complete instructions to update 
 * OracleSpecialDBOperation.jar (from <cmp-basedir>) are:
 *
 * 1. Compile the cmp module, e.g.
 *   maven clean build
 * 2. Clean the oracle special support classes
 *   ant -f support/sqlstore/build.xml clean-oracle-special
 * 3. Update <cmp-basedir>/release/build/OracleSpecialDBOperation.jar
 *   ant -Doracle-jdbc.jar=<your location>/ojdbc14.jar -f support/sqlstore/build.xml update-oracle-special
 *
 */

package com.sun.jdo.spi.persistence.support.sqlstore.database.oracle;

import java.util.List;
import java.util.Arrays;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.sql.PreparedStatement;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Types;
import java.sql.Statement;

import com.sun.jdo.api.persistence.support.FieldMapping;
import com.sun.jdo.spi.persistence.support.sqlstore.LogHelperSQLStore;
import com.sun.jdo.spi.persistence.support.sqlstore.ejb.EJBHelper;
import com.sun.jdo.spi.persistence.support.sqlstore.database.BaseSpecialDBOperation;
import com.sun.jdo.spi.persistence.utility.logging.Logger;

/**
 * OracleSpecialDBOperation is derived class for Oracle specific operation.
 * @author Shing Wai Chan
 * @author Mitesh Meswani
 */
public class OracleSpecialDBOperation extends BaseSpecialDBOperation {
    /**
     * The logger
     */
    private static Logger logger = LogHelperSQLStore.getLogger();

    /**
     * Interface used to handle driver specific implementation for various
     * drivers for Oracle.
     */
    private interface DBDriverHandlerFactory {
        /**
         * Creates an instance of DBDriverHandler to handle driver specific request
         * on a <code>PreparedStatement</code>
         * @param ps Instance of PreparedStatement
         * @return Instance of DBDriverHandler that corresponds to DBDriver used
         * to obtain <code>ps</code>
         *
         */
        DBDriverHandler createDBDriverHandler(PreparedStatement ps);
        /**
         * Returns true if underlying driver supports defineColumnType() false
         * otherwise.
         * @return true if underlying driver supports defineColumnType() false
         * otherwise.
         */
        boolean supportsDefineColumnType();
    }

    /**
     * Base implementation for Oracle's driver's handler.
     */
    private abstract class OracleDriverHandlerFactory
        implements DBDriverHandlerFactory {
        public boolean supportsDefineColumnType() {
            return true;
        }
    }

    private DBDriverHandlerFactory dBDriverHandlerFactory;


    private static final boolean oracle816ClassesAvailable;
    private static final boolean oracle817ClassesAvailable;
    static {
        ClassLoader loader = OracleSpecialDBOperation.class.getClassLoader();
        // OraclePreparedStatement is in the oracle.jdbc.driver package for
        // oracle 8.1.6 drivers.
        oracle816ClassesAvailable =
            loadClass("oracle.jdbc.driver.OraclePreparedStatement", loader) //NOI18N
                != null;

        // OraclePreparedStatement is in the oracle.jdbc package for
        // oracle 8.1.7 (and higher) drivers.
        oracle817ClassesAvailable =
            loadClass("oracle.jdbc.OraclePreparedStatement", loader) != null; //NOI18N
    }

    /**
     * The SQL statement used to get implementation type of PreparedStatement.
     */
    private static final String TEST_STATEMENT = "select 1 from dual";

    public OracleSpecialDBOperation() {
    }

    /**
     * Initializes driver specific behavior classes by determining the
     * characteristics of the jdbc driver used with this DataSource.
     */
    public void initialize(DatabaseMetaData metaData,
        String identifier) throws SQLException {
        Connection con = metaData.getConnection();
        // Since the PreparedStatement obtained is directly through
        // con, there is no need to unwrap it.
        PreparedStatement testPs = con.prepareStatement(TEST_STATEMENT);
/*
        if (oracle817ClassesAvailable &&
            testPs instanceof oracle.jdbc.OraclePreparedStatement) {
            // This DataSource uses a driver version 8.1.7 or higher.
            // Oracle drivers for version 8.1.7 and higher define interface
            // oracle.jdbc.OraclePreparedStatement.
            // OraclePreparedStaement obtained from these drivers implement this
            // interface. It is possible that in future Oracle might alter
            // implementation class for OraclePreparedStatement. However, they
            // should continue implementing this interface. Hence, this
            // interface should be preferred to communicate with Oracle drivers.
            dBDriverHandlerFactory = new OracleDriverHandlerFactory() {
                public DBDriverHandler createDBDriverHandler(PreparedStatement ps) {
                    return new Oracle817Handler(ps);
                }
            };
        } else if (oracle816ClassesAvailable &&
                    testPs instanceof oracle.jdbc.driver.OraclePreparedStatement) {
            // This DataSource uses a driver version lower than 8.1.7.
            // Currently all Oracle drivers return instance of
            // oracle.jdbc.driver.OraclePreparedStatement for OraclePreparedStatement.
            dBDriverHandlerFactory = new OracleDriverHandlerFactory() {
                public DBDriverHandler createDBDriverHandler(PreparedStatement ps) {
                    return new Oracle816Handler(ps);
                }
            };
        } else */{
            // This DataSource uses a non oracle driver.
            dBDriverHandlerFactory = new DBDriverHandlerFactory() {
                public DBDriverHandler createDBDriverHandler(PreparedStatement ps) {
                    return new NonOracleHandler(ps);
                }

                public boolean supportsDefineColumnType() {
                    return false;
                }
            };
            // Warn the user Oracle specific features will be disabled.
            if(logger.isLoggable(logger.CONFIG)) {
                identifier = identifier == null ?
                    "Connection Factory" : identifier;   //NOI18N
                logger.log(logger.CONFIG,
                           "sqlstore.database.oracle.nooracleavailable", //NOI18N
                           identifier);
            }
        }
        testPs.close();
    }

    /**
     * Defines Column type for result for specified ps.
     */
    public void defineColumnTypeForResult(
        PreparedStatement ps, List columns) throws SQLException {
        if(dBDriverHandlerFactory.supportsDefineColumnType()) {
            int size = columns.size();
            if (size > 0) {
                DBDriverHandler driverHandler =
                    dBDriverHandlerFactory.createDBDriverHandler(ps);
                try {
                    for (int i = 0; i < size; i++) {
                        FieldMapping fieldMapping = (FieldMapping) columns.get(i);
                        int type = fieldMapping.getColumnType();
                        if (type == Types.CHAR || type == Types.VARCHAR) {
                            int len = fieldMapping.getColumnLength();
                            if (len > 0) {
                                driverHandler.defineColumnType(i + 1, type, len);
                            } else {
                                driverHandler.defineColumnType(i + 1, type);
                            }
                        } else {
                            driverHandler.defineColumnType(i + 1, type);
                        }
                    }
                } catch (Exception ex) {
                    if (logger.isLoggable(Logger.INFO)) {
                        logger.log(Logger.INFO,
                                   "sqlstore.database.oracle.defineCol", // NOI18N
                                   ex);
                    }
                    driverHandler.clearDefines();
                }
            }
        }
    }

    /**
     * Implements special handling of char columns on Oracle.
     */
    public void bindFixedCharColumn(PreparedStatement stmt,
        int index, String strVal, int length) throws SQLException {
        DBDriverHandler driverHandler =
            dBDriverHandlerFactory.createDBDriverHandler(stmt);
        driverHandler.bindFixedCharColumn(index, strVal, length);
    }

    /**
     * Loads className using loader inside a previleged block.
     * Returns null if class is not in classpath.
     */
    private static Class loadClass(String className, ClassLoader loader) {
        final ClassLoader finalLoader = loader;
        final String finalClassName = className;
        return  (Class)AccessController.doPrivileged(
            new PrivilegedAction() {
                public Object run() {
                    try {
                        if (finalLoader != null) {
                            return Class.forName(finalClassName, true,
                                finalLoader);
                        } else {
                            return Class.forName(finalClassName);
                        }
                    } catch(Exception e) {
                        return null;
                    }
                }
            }
        );
    }

    private interface DBDriverHandler {
        void defineColumnType(int index, int type) throws SQLException;
        void defineColumnType( int index, int type, int length)
            throws SQLException;
        public void clearDefines() throws SQLException;
        public void bindFixedCharColumn(int index, String strVal, int length)
            throws SQLException;
    }

//    private static class Oracle817Handler implements DBDriverHandler {
//        oracle.jdbc.OraclePreparedStatement oraclePreparedStatement;
//
//        public Oracle817Handler(Statement ps) {
//            oraclePreparedStatement = (oracle.jdbc.OraclePreparedStatement)
//                                        EJBHelper.unwrapStatement(ps);
//        }
//        public void defineColumnType(int index, int type) throws SQLException {
//            oraclePreparedStatement.defineColumnType(index, type);
//        }
//        public void defineColumnType( int index, int type,int length)
//            throws SQLException {
//            oraclePreparedStatement.defineColumnType(index, type, length);
//        }
//        public void clearDefines() throws SQLException {
//            oraclePreparedStatement.clearDefines();
//        }
//        public void bindFixedCharColumn(int index, String strVal, int length)
//            throws SQLException {
//            oraclePreparedStatement.setFixedCHAR(index, strVal);
//        }
//    }
//
//    private static class Oracle816Handler implements DBDriverHandler {
//        oracle.jdbc.driver.OraclePreparedStatement oraclePreparedStatement;
//
//        public Oracle816Handler(Statement ps) {
//            oraclePreparedStatement = (oracle.jdbc.driver.OraclePreparedStatement)
//                                        EJBHelper.unwrapStatement(ps);
//        }
//        public void defineColumnType(int index, int type) throws SQLException {
//            oraclePreparedStatement.defineColumnType(index, type);
//        }
//        public void defineColumnType( int index, int type,int length)
//            throws SQLException {
//            oraclePreparedStatement.defineColumnType(index, type, length);
//        }
//        public void clearDefines() throws SQLException {
//            oraclePreparedStatement.clearDefines();
//        }
//        public void bindFixedCharColumn(int index, String strVal, int length)
//            throws SQLException {
//            oraclePreparedStatement.setFixedCHAR(index, strVal);
//        }
//    }

    private static class NonOracleHandler implements DBDriverHandler {

        PreparedStatement ps;

        private NonOracleHandler(PreparedStatement ps) {
            this.ps = ps;
        }
        public void defineColumnType(int index, int type) throws SQLException {}
        public void defineColumnType( int index, int type,int length)
            throws SQLException {}
        public void clearDefines() throws SQLException {}
        public void bindFixedCharColumn(int index, String strVal, int length)
            throws SQLException {
            // We are running on an Oracle database but not using an
            // Oracle driver. We need to bind a field mapped to a CHAR column by
            // padding the value with spaces to the length specified in the
            // dbschema metadata.
            ps.setString(index, padSpaceChar(strVal, length) );
            if (logger.isLoggable(Logger.FINE) ) {
                logger.log(Logger.FINE, "sqlstore.database.oracle.fixedcharpadded",
                           strVal, new Integer(length) );
            }
        }

        private static final char SPACE_CHAR = ' ';
        private static final int  PAD_STEP_LENGTH = 50;
        private static final char[] SPACES = new char[PAD_STEP_LENGTH];
        static {
            Arrays.fill(SPACES, SPACE_CHAR);
        }
        /**
         * Pads space characters to specified val to increase its length to
         * specified targetLength.
         * @param val The input value.
         * @param targetLength  Target length for returned String
         * @return val padded with space chars.
         */
        private static String padSpaceChar(String val, int targetLength) {
            String retVal = val;
            int inputLength = val.length();

            if(inputLength < targetLength) {
                StringBuffer buf = new StringBuffer(targetLength);
                buf.append(val);
                int padsize = targetLength - inputLength;
                while (padsize >= PAD_STEP_LENGTH) {
                    buf.append(SPACES);
                    padsize -= PAD_STEP_LENGTH;
                }
                buf.append(SPACES, 0, padsize);
                retVal = buf.toString();
            }
            return retVal;
        }
    }
}
