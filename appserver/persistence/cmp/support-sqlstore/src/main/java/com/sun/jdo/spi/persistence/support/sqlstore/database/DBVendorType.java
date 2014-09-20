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

package com.sun.jdo.spi.persistence.support.sqlstore.database;

import com.sun.jdo.api.persistence.support.JDOFatalInternalException;
import com.sun.jdo.api.persistence.support.JDOFatalUserException;
import com.sun.jdo.api.persistence.support.JDOUserException;
import com.sun.jdo.api.persistence.support.SpecialDBOperation;
import com.sun.jdo.spi.persistence.support.sqlstore.LogHelperSQLStore;
import com.sun.jdo.spi.persistence.utility.FieldTypeEnumeration;
import org.glassfish.persistence.common.database.PropertyHelper;
import org.glassfish.persistence.common.database.DBVendorTypeHelper;
import com.sun.jdo.spi.persistence.utility.logging.Logger;
import org.glassfish.persistence.common.I18NHelper;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.DatabaseMetaData;
import java.util.HashMap;
import java.util.Properties;
import java.util.ResourceBundle;



/**
 */
public class DBVendorType  {
    /**
     * Map from property name to property value.
     */
    private HashMap dbMap;

    /**
     * Instance of specialDBOperation for this vendor type. Please look at
     * newSpecialDBOperationInstance() to find out how this member is
     * initialized.
     */
    private SpecialDBOperation specialDBOperation;

    private static final SpecialDBOperation DEFAULT_SPECIAL_DB_OPERATION =
        new BaseSpecialDBOperation();

    /**
     * VendorType as returned from {@link DBVendorTypeHelper#getDBType(java.lang.String)}
     */
    private String vendorType;

    /**
     * VendorType as returned from {@link DBVendorTypeHelper#getEnumDBType(java.lang.String)}
     */
    private int enumVendorType;

    /**
     * The logger
     */
    private final static Logger logger;

    /**
     * I18N message handler
     */
    private final static ResourceBundle messages;

    /**
     * Default properties
     */
    private static Properties defaultProperties;

    private final static String EXT = ".properties"; // NOI18N
    private final static String SPACE = " "; // NOI18N
    private final static String NONE = ""; // NOI18N

    private final static String PATH = "com/sun/jdo/spi/persistence/support/sqlstore/database/"; // NOI18N
    private final static String PROPERTY_OVERRIDE_FILE = ".tpersistence.properties"; // NOI18N


    private final static String FOR_UPDATE = "FOR_UPDATE"; // NOI18N
    private final static String HOLDLOCK = "HOLDLOCK"; //NOI18N
    private final static String SUPPORTS_UPDATE_LOCK = "SUPPORTS_UPDATE_LOCK"; //NOI18N
    private final static String SUPPORTS_LOCK_COLUMN_LIST = "SUPPORTS_LOCK_COLUMN_LIST"; //NOI18N
    private final static String SUPPORTS_DISTINCT_WITH_UPDATE_LOCK = "SUPPORTS_DISTINCT_WITH_UPDATE_LOCK"; //NOI18N
    private final static String NATIVE_OUTER_JOIN = "NATIVE_OUTER_JOIN"; //NOI18N
    private final static String LEFT_JOIN = "LEFT_JOIN"; //NOI18N
    private final static String LEFT_JOIN_APPEND = "LEFT_JOIN_APPEND"; //NOI18N
    private final static String RIGHT_JOIN = "RIGHT_JOIN"; //NOI18N
    private final static String RIGHT_JOIN_PRE = "RIGHT_JOIN_PRE"; //NOI18N
    private final static String IS_NULL = "IS_NULL"; //NOI18N
    private final static String IS_NOT_NULL = "IS_NOT_NULL"; //NOI18N
    private final static String ANSI_TRIM = "ANSI_TRIM"; //NOI18N
    private final static String RTRIM = "RTRIM"; //NOI18N
    private final static String RTRIM_POST = "RTRIM_POST"; //NOI18N
    private final static String TABLE_LIST_START = "TABLE_LIST_START"; //NOI18N
    private final static String TABLE_LIST_END = "TABLE_LIST_END"; //NOI18N
    private final static String STRING_CONCAT = "STRING_CONCAT"; //NOI18N
    private final static String QUOTE_CHAR_START = "QUOTE_CHAR_START"; //NOI18N
    private final static String QUOTE_CHAR_END = "QUOTE_CHAR_END"; //NOI18N
    private final static String QUOTE_SPECIAL_ONLY = "QUOTE_SPECIAL_ONLY"; //NOI18N
    private final static String CHAR_LENGTH = "CHAR_LENGTH"; //NOI18N
    private final static String SQRT = "SQRT"; //NOI18N
    private final static String ABS = "ABS"; //NOI18N
    private final static String SUBSTRING = "SUBSTRING"; //NOI18N
    private final static String SUBSTRING_FROM = "SUBSTRING_FROM"; //NOI18N
    private final static String SUBSTRING_FOR = "SUBSTRING_FOR"; //NOI18N
    private final static String POSITION = "POSITION"; //NOI18N
    private final static String POSITION_SEP = "POSITION_SEP"; //NOI18N
    private final static String POSITION_SEARCH_SOURCE = "POSITION_SEARCH_SOURCE"; //NOI18N
    private final static String POSITION_THREE_ARGS = "POSITION_THREE_ARGS"; //NOI18N
    private final static String MAP_EMPTY_STRING_TO_NULL = "MAP_EMPTY_STRING_TO_NULL"; //NOI18N
    private final static String SPECIAL_DB_OPERATION = "SPECIAL_DB_OPERATION"; //NOI18N
    private final static String SUPPORTS_LIKE_ESCAPE = "SUPPORTS_LIKE_ESCAPE"; //NOI18N
    private final static String LEFT_LIKE_ESCAPE = "LEFT_LIKE_ESCAPE"; //NOI18N
    private final static String RIGHT_LIKE_ESCAPE = "RIGHT_LIKE_ESCAPE"; //NOI18N
    private final static String NULL_COMPARISON_FUNCTION_NAME = "NULL_COMPARISON_FUNCTION_NAME"; //NOI18N
    private final static String MOD_FUNCTION_NAME = "MOD_FUNCTION_NAME"; //NOI18N
    private final static String CONCAT_CAST = "CONCAT_CAST"; //NOI18N
    private final static String PARAMETER_CAST = "PARAMETER_CAST"; //NOI18N
    private final static String INLINE_NUMERIC = "INLINE_NUMERIC"; //NOI18N
    private final static String NOT_EQUAL = "NOT_EQUAL"; //NOI18N

    private static final String[] props = new String[] { FOR_UPDATE,
            HOLDLOCK, SUPPORTS_UPDATE_LOCK, SUPPORTS_LOCK_COLUMN_LIST,
            SUPPORTS_DISTINCT_WITH_UPDATE_LOCK,
            NATIVE_OUTER_JOIN, LEFT_JOIN, LEFT_JOIN_APPEND, RIGHT_JOIN, RIGHT_JOIN_PRE,
            IS_NULL, IS_NOT_NULL, ANSI_TRIM, RTRIM, RTRIM_POST,
            TABLE_LIST_START, TABLE_LIST_END,
            QUOTE_CHAR_START, QUOTE_CHAR_END, QUOTE_SPECIAL_ONLY,
            STRING_CONCAT, CHAR_LENGTH, SQRT, ABS,
            SUBSTRING, SUBSTRING_FROM, SUBSTRING_FOR,
            POSITION, POSITION_SEP, POSITION_SEARCH_SOURCE, POSITION_THREE_ARGS,
            MAP_EMPTY_STRING_TO_NULL, SPECIAL_DB_OPERATION,
            SUPPORTS_LIKE_ESCAPE, LEFT_LIKE_ESCAPE, RIGHT_LIKE_ESCAPE,
            NULL_COMPARISON_FUNCTION_NAME, MOD_FUNCTION_NAME,
            CONCAT_CAST, PARAMETER_CAST, INLINE_NUMERIC, NOT_EQUAL
    };

    /**
     * Initialize static fields.
     */
    static {
        logger = LogHelperSQLStore.getLogger();
        messages = I18NHelper.loadBundle(
            "com.sun.jdo.spi.persistence.support.sqlstore.Bundle", // NOI18N
            DBVendorType.class.getClassLoader());

        defaultProperties = initializeDefaultProperties();
    }

    /**
     * @param databaseMetaData Instance of DatabaseMetaData
     * @param identifier identifier of the caller creating a new instance
     * of SQLStoreManager.
     */
    public DBVendorType(DatabaseMetaData databaseMetaData, String identifier)
        throws SQLException {

        String vendorName  = databaseMetaData.getDatabaseProductName();
        String vendorType  = DBVendorTypeHelper.getDBType(vendorName);

        if (logger.isLoggable()) {
            Object[] items = new Object[] {vendorName,vendorType};
            logger.fine("sqlstore.database.dbvendor.vendorname", items); // NOI18N
        }

        this.vendorType     = vendorType;
        enumVendorType      = DBVendorTypeHelper.getEnumDBType(vendorType);
        dbMap               = getDBPropertiesMap(vendorType,vendorName);
        specialDBOperation  = newSpecialDBOperationInstance((String)dbMap.get(SPECIAL_DB_OPERATION),
                                databaseMetaData, identifier);
    }

    /**
     * get properties map for given vendorType and vendorName
     */
    private static HashMap getDBPropertiesMap(String vendorType, String vendorName) {
        //Initialize returned map to default
        HashMap dbHashMap = new HashMap(defaultProperties);
        Properties dbProperties = loadDBProperties(vendorType, vendorName);
        dbHashMap.putAll(dbProperties);

        return dbHashMap;
    }

    /**
     * Initialize default properties.
     */
    private static Properties initializeDefaultProperties() {
        //Load default properties if not already loaded
        if (defaultProperties == null) {
            // Load default (sql92) Properties
            defaultProperties = new Properties();
            try {
                loadFromResource(DBVendorTypeHelper.DEFAULT_DB, defaultProperties);
            } catch (IOException e) {
                throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                                "sqlstore.database.dbvendor.cantloadDefaultProperties"), // NOI18N
                                 e);
            }
        }

        return defaultProperties;
     }

    /**
     * Load properties for database specified by vendorType and vendorName
     */
    private static Properties loadDBProperties(String vendorType, String vendorName) {
        // Load actual Properties. Even if it is unknown db,
        Properties dbProperties = new Properties();
        if (!vendorType.equals(DBVendorTypeHelper.DEFAULT_DB)) {
            try {
                loadFromResource(vendorType, dbProperties);
            } catch (IOException e) {
                // else ignore
                if (logger.isLoggable()) {
                    logger.fine("sqlstore.database.dbvendor.init.default", vendorType); // NOI18N
                }
            }
        }
        overrideProperties(dbProperties, vendorName);
        return dbProperties;
    }

    /**
     * Overrides default properties with the ones provided
     * by the user
     */
    private static void overrideProperties(Properties dbProperties, String vendorName) {
        boolean debug = logger.isLoggable();

        Properties overridingProperties = new Properties();
        try {
            PropertyHelper.loadFromFile(overridingProperties, PROPERTY_OVERRIDE_FILE);
        } catch (Exception e) {
            if (debug) {
                logger.fine("sqlstore.database.dbvendor.overrideproperties"); // NOI18N
            }
            return; 	// nothing to override
        }

        //Prepare a clean vendor name by replacing all ' '  and '/' with underscores.
        String cleanVendorName = vendorName.toLowerCase().replace(' ', '_');
        cleanVendorName = cleanVendorName.replace('/', '_');

        String propertyPrefix = "database." + cleanVendorName + "."; // prefix // NOI18N

        for (int i = 0; i < props.length; i++) {
            String o = overridingProperties.getProperty(propertyPrefix + props[i]);
            if (o != null) {
                if (debug) {
                    Object[] items = new Object[] {props[i],o};
                    logger.fine("sqlstore.database.dbvendor.overrideproperties.with", items); // NOI18N
                }
                dbProperties.setProperty(props[i], o);
            }
        }
    }

    /**
     * loads database properties list from the specified resource
     * into specified Properties object.
     * @param resourceName  Name of resource.
     * @param properties    Properties object to load
     */
    private static void loadFromResource(String resourceName, Properties properties)
            throws IOException {
        String fullResourceName = PATH + resourceName + EXT;
        PropertyHelper.loadFromResource(properties, fullResourceName, DBVendorType.class.getClassLoader());
    }

    /**
     * Returns new instance of class specified by specialDBOpClassName.
     * The class is required to implement interface SpecialDBOperation.
     * If specialDBOpClassName is null or cannot be loaded, then an instance
     * of BaseSpecialDBOperation is returned.
     * @param specialDBOpClassName Name of a class that implements
     * SpecialDBOperation
     * @param databaseMetaData DatabaseMetaData for the connection for which
     * this SpecialDBOperation is created
     * @param identifier Identifier of pmf used to obtain databaseMetaData.
     * This can be null in non managed environment.
     * @return An instance of SpecialDBOperation specified by specialDBOpClassName.
     */
    private SpecialDBOperation newSpecialDBOperationInstance(
        final String specialDBOpClassName, DatabaseMetaData databaseMetaData,
        String identifier) {
        SpecialDBOperation retInstance = null;
        if (specialDBOpClassName != null) {
            final ClassLoader loader = DBVendorType.class.getClassLoader();
            Class clz = (Class)java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction() {
                    public Object run() {
                        try {
                            if (loader != null) {
                                return Class.forName(specialDBOpClassName,
                                    true, loader);
                            } else {
                                return Class.forName(specialDBOpClassName);
                            }
                        } catch(Exception ex) {
                            if (logger.isLoggable()) {
                                logger.log(Logger.INFO,
                                    "core.configuration.cantloadclass", // NOI18N
                                    specialDBOpClassName);
                            }
                            return null;
                        }
                    }
                }
            );

            if (clz != null) {
                try {
                    retInstance = (SpecialDBOperation)clz.newInstance();
                    retInstance.initialize(databaseMetaData, identifier);
                } catch(Exception ex) {
                    throw new JDOFatalUserException(
                        I18NHelper.getMessage(messages,
                        "sqlstore.database.dbvendor.cantinstantiateclass", // NOI18N
                        specialDBOpClassName), ex);
                }
            }
        }
        return (retInstance != null)? retInstance : DEFAULT_SPECIAL_DB_OPERATION;
    }

    /**
     * Returns the string that represents "left join" clause
     * for this database
     */
    public String getLeftJoin() {
        String s = (String)dbMap.get(LEFT_JOIN);
        if (s == null)
            s = NONE;

        if (logger.isLoggable()) {
            logger.fine("sqlstore.database.dbvendor.getleftjoin", s); // NOI18N
        }

        return SPACE + s;
    }


    /**
     * Returns true if this database supports update lock
     */
    public boolean isUpdateLockSupported() {
        String s = (String)dbMap.get(SUPPORTS_UPDATE_LOCK);
        Boolean b = Boolean.valueOf(s);

        if (logger.isLoggable()) {
            logger.fine("sqlstore.database.dbvendor.isupdatelocksupported", b); // NOI18N
        }

        return b.booleanValue();
    }

    /**
     * Returns true if this database supports update 'of column list'
     */
    public boolean  isLockColumnListSupported() {
        String s = (String)dbMap.get(SUPPORTS_LOCK_COLUMN_LIST);
        Boolean b = Boolean.valueOf(s);

        if (logger.isLoggable()) {
            logger.fine("sqlstore.database.dbvendor.islockcolumnlistsupported", b); // NOI18N
        }

        return b.booleanValue();
    }

    /**
     * Returns true if this database supports distinct clause with update lock
     */
    public boolean isDistinctSupportedWithUpdateLock() {
        String s = (String)dbMap.get(SUPPORTS_DISTINCT_WITH_UPDATE_LOCK);
        Boolean b = Boolean.valueOf(s);

        if (logger.isLoggable()) {
            logger.fine("sqlstore.database.dbvendor.isdistinctupdatelocksupported", b); // NOI18N
        }

        return b.booleanValue();
    }


    /**
     * Returns the string that represents "holdlock" clause
     * for this database
     */
    public String getHoldlock() {
        String s = (String)dbMap.get(HOLDLOCK);
        if (s == null)
            s = NONE;

        if (logger.isLoggable()) {
            logger.fine("sqlstore.database.dbvendor.getholdlock", s); // NOI18N
        }

        return SPACE + s;
    }

    /**
     * Returns true if the this database needs native outer join semantics.
     */
    public boolean isNativeOuterJoin() {
        String s = (String)dbMap.get(NATIVE_OUTER_JOIN);
        Boolean b = Boolean.valueOf(s);

        if (logger.isLoggable()) {
            logger.fine("sqlstore.database.dbvendor.isNativeOuterJoin", b); // NOI18N
        }

        return b.booleanValue();
    }

    /**
     * Returns the string that represents postfix for "left join" clause
     * for this database
     */
    public String getLeftJoinPost() {
        String s = (String)dbMap.get(LEFT_JOIN_APPEND);
        if (s == null)
            s = NONE;

        if (logger.isLoggable()) {
            logger.fine("sqlstore.database.dbvendor.getleftjoinpost", s); // NOI18N
        }

        return SPACE + s;
    }

    /**
     * Returns the string that represents "right join" clause
     * for this database
     */
    public String getRightJoin() {
        String s = (String)dbMap.get(RIGHT_JOIN);
        if (s == null)
            s = NONE;

        if (logger.isLoggable()) {
            logger.fine("sqlstore.database.dbvendor.getrightjoin", s); // NOI18N
        }

        return SPACE + s;
    }

    /**
     * Returns the string that represents prefix for "right join" clause
     * for this database
     */
    public String getRightJoinPre() {
        String s = (String)dbMap.get(RIGHT_JOIN_PRE);
        if (s == null)
            s = NONE;

        if (logger.isLoggable()) {
            logger.fine("sqlstore.database.dbvendor.getrightjoinipre", s); // NOI18N
        }

        return SPACE + s;
    }

    /**
     * Returns the string that represents "is NULL" clause
     * for this database
     */
    public String getIsNull() {
        String s = (String)dbMap.get(IS_NULL);
        if (s == null)
            s = NONE;

        if (logger.isLoggable()) {
            logger.fine("sqlstore.database.dbvendor.getisnull", s); // NOI18N
        }

        return SPACE + s;
    }

    /**
     * Returns the string that represents "is not NULL" clause
     * for this database
     */
    public String getIsNotNull() {
        String s = (String)dbMap.get(IS_NOT_NULL);
        if (s == null)
            s = NONE;

        if (logger.isLoggable()) {
            logger.fine("sqlstore.database.dbvendor.getisnotnull", s); // NOI18N
        }

        return SPACE + s;
    }

    /**
     * Returns true if this database need ansi style rtrim semantics.
     */
    public boolean isAnsiTrim() {
        String s = (String)dbMap.get(ANSI_TRIM);
        Boolean b = Boolean.valueOf(s);

        if (logger.isLoggable()) {
            logger.fine("sqlstore.database.dbvendor.isAnsiTrim", b); // NOI18N
        }

        return b.booleanValue();
    }

    /**
     * Returns the string that represents "RTRIM" clause
     * for this database
     */
    public String getRtrim() {
        String s = (String)dbMap.get(RTRIM);
        if (s == null)
            s = NONE;

        if (logger.isLoggable()) {
            logger.fine("sqlstore.database.dbvendor.getrtrim", s); // NOI18N
        }

        return SPACE + s;
    }

    /**
     * Returns the string that represents postfix for "RTRIM" clause
     * for this database
     */
    public String getRtrimPost() {
        String s = (String)dbMap.get(RTRIM_POST);
        if (s == null)
            s = NONE;

        if (logger.isLoggable()) {
            logger.fine("sqlstore.database.dbvendor.getrtrimpost", s); // NOI18N
        }

        return SPACE + s;
    }

    /**
     * Returns the string that represents prefix for "Char_length" clause
     * for this database
     */
    public String getCharLength() {
        String s = (String)dbMap.get(CHAR_LENGTH);
        if (s == null)
            s = NONE;

        if (logger.isLoggable()) {
            logger.fine("sqlstore.database.dbvendor.getcharlength", s); // NOI18N
        }

        return SPACE + s;
    }

    /**
     * Returns the string that represents prefix for "Sqrt" clause
     * for this database
     */
    public String getSqrt() {
        String s = (String)dbMap.get(SQRT);
        if (s == null) {
           throw new JDOUserException(I18NHelper.getMessage(messages,
               "core.constraint.illegalop", // NOI18N
               "Sqrt"));
        }

        if (logger.isLoggable()) {
            logger.fine("sqlstore.database.dbvendor.getsqrt", s); // NOI18N
        }

        return SPACE + s;
    }

    /**
     * Returns the string that represents prefix for "abs" clause
     * for this database
     */
    public String getAbs() {
        String s = (String)dbMap.get(ABS);
        if (s == null) {
           throw new JDOUserException(I18NHelper.getMessage(messages,
               "core.constraint.illegalop", // NOI18N
               "Abs"));
        }

        if (logger.isLoggable()) {
            logger.fine("sqlstore.database.dbvendor.getabs", s); // NOI18N
        }

        return SPACE + s;
    }

    /**
     * Returns the string that represents "for update" clause
     * for this database
     */
    public String getForUpdate() {
        String s = (String)dbMap.get(FOR_UPDATE);
        if (s == null)
            s = NONE;

        if (logger.isLoggable()) {
            logger.fine("sqlstore.database.dbvendor.getforupdate", s); // NOI18N
        }

        return SPACE + s;
    }

    /**
     * Returns the string that represents start of table list
     * for this database
     */
    public String getTableListStart() {
        String s = (String)dbMap.get(TABLE_LIST_START);
        if (s == null)
            s = NONE;

        if (logger.isLoggable()) {
            logger.fine("sqlstore.database.dbvendor.gettableliststart", s); // NOI18N
        }

        return SPACE + s;
    }

    /**
     * Returns the string that represents end of table list
     * for this database
     */
    public String getTableListEnd() {
        String s = (String)dbMap.get(TABLE_LIST_END);
        if (s == null)
            s = NONE;

        if (logger.isLoggable()) {
            logger.fine("sqlstore.database.dbvendor.gettablelistend", s); // NOI18N
        }

        return SPACE + s;
    }

    /**
     * Returns the string that represents concatenation operation
     * for this database
     */
    public String getStringConcat() {
        String s = (String)dbMap.get(STRING_CONCAT);
        if (s == null)
            s = NONE;

        if (logger.isLoggable()) {
            logger.fine("sqlstore.database.dbvendor.getstringconcat", s); // NOI18N
        }

        return SPACE + s + SPACE;
    }

    /**
     * Returns the start identifier quote character for this database, or
     * an empty string, if there is none.
     */
    public String getQuoteCharStart() {
        String s = (String)dbMap.get(QUOTE_CHAR_START);
        if (s == null)
            s = NONE;

        if (logger.isLoggable()) {
            logger.fine("sqlstore.database.dbvendor.getquotecharstart", s); // NOI18N
        }

        return s;
    }

    /**
     * Returns the end identifier quote character for this database, or
     * an empty string, if there is none.
     */
    public String getQuoteCharEnd() {
        String s = (String)dbMap.get(QUOTE_CHAR_END);
        if (s == null)
            s = NONE;

        if (logger.isLoggable()) {
            logger.fine("sqlstore.database.dbvendor.getquotecharend", s); // NOI18N
        }

        return s;
    }

    /**
     * Returns true if only identifiers with special characters should be quoted
     * for this database
     */
    public boolean getQuoteSpecialOnly() {
        String s = (String)dbMap.get(QUOTE_SPECIAL_ONLY);
        Boolean b = Boolean.valueOf(s);

        if (logger.isLoggable()) {
            logger.fine("sqlstore.database.dbvendor.getquotespecialonly", b); // NOI18N
        }

        return b.booleanValue();
    }

    /**
     * Returns the string that represents prefix for "Substring" clause
     * for this database
     */
    public String getSubstring() {
        String s = (String)dbMap.get(SUBSTRING);
        if (s == null) {
           throw new JDOUserException(I18NHelper.getMessage(messages,
               "core.constraint.illegalop", // NOI18N
               "substring"));
        }

        if (logger.isLoggable()) {
            logger.fine("sqlstore.database.dbvendor.getsubstring", s); // NOI18N
        }

        return SPACE + s;
    }

    /**
     * Returns the string that represents from part for "Substring" clause
     * for this database
     */
    public String getSubstringFrom() {
        String s = (String)dbMap.get(SUBSTRING_FROM);
        if (s == null) {
           throw new JDOUserException(I18NHelper.getMessage(messages,
               "core.constraint.illegalop", // NOI18N
               "from part of substring"));
        }

        if (logger.isLoggable()) {
            logger.fine("sqlstore.database.dbvendor.getsubstringfrom", s); // NOI18N
        }

        return SPACE + s + SPACE;
    }

    /**
     * Returns the string that represents for part for "Substr" clause
     * for this database
     */
    public String getSubstringFor() {
        String s = (String)dbMap.get(SUBSTRING_FOR);
        if (s == null) {
           throw new JDOUserException(I18NHelper.getMessage(messages,
               "core.constraint.illegalop", // NOI18N
               "for part of substring"));
        }

        if (logger.isLoggable()) {
            logger.fine("sqlstore.database.dbvendor.getsubstringfor", s); // NOI18N
        }

        return SPACE + s + SPACE;
    }

    /**
     * Returns the string that represents "Position" clause
     * for this database
     */
    public String getPosition() {
        String s = (String)dbMap.get(POSITION);
        if (s == null) {
           throw new JDOUserException(I18NHelper.getMessage(messages,
               "core.constraint.illegalop", // NOI18N
               "position"));
        }

        if (logger.isLoggable()) {
            logger.fine("sqlstore.database.dbvendor.getposition", s); // NOI18N
        }

        return SPACE + s;
    }

    /**
     * Returns the string that represents separator part of "Position" clause
     * for this database
     */
    public String getPositionSep() {
        String s = (String)dbMap.get(POSITION_SEP);
        if (s == null) {
           throw new JDOUserException(I18NHelper.getMessage(messages,
               "core.constraint.illegalop", // NOI18N
               "in part of position"));
        }

        if (logger.isLoggable()) {
            logger.fine("sqlstore.database.dbvendor.getpositionin", s); // NOI18N
        }

        return SPACE + s;
    }

    /**
     * Returns true if first position argument is Search String and second
     * argument is Source String for this database
     */
    public boolean isPositionSearchSource() {
        String s = (String)dbMap.get(POSITION_SEARCH_SOURCE);
        Boolean b = Boolean.valueOf(s);

        if (logger.isLoggable()) {
            logger.fine("sqlstore.database.dbvendor.getpositionsrchsrc", b); // NOI18N
        }

        return b.booleanValue();
    }

    /**
     * Returns true if position has three argument for this database
     */
    public boolean isPositionThreeArgs() {
        String s = (String)dbMap.get(POSITION_THREE_ARGS);
        Boolean b = Boolean.valueOf(s);

        if (logger.isLoggable()) {
            logger.fine("sqlstore.database.dbvendor.getposition3args", b); // NOI18N
        }

        return b.booleanValue();
    }

    /**
     * Returns true if this database maps empty Strings to NULL
     */
    public boolean mapEmptyStringToNull() {
        String s = (String)dbMap.get(MAP_EMPTY_STRING_TO_NULL);
        Boolean b = Boolean.valueOf(s);

        if (logger.isLoggable()) {
            logger.fine("sqlstore.database.dbvendor.mapemptystrtonull", b); // NOI18N
        }

        return b.booleanValue();
    }

    /**
     * Returns true if this database supports "LIKE ESCAPE" clause
     */
    public boolean supportsLikeEscape() {
        String s = (String)dbMap.get(SUPPORTS_LIKE_ESCAPE);
        Boolean b = Boolean.valueOf(s);

        if (logger.isLoggable()) {
            logger.fine("sqlstore.database.dbvendor.supportslikeescape", b); // NOI18N
        }

        return b.booleanValue();
    }

    /**
     * Returns left string that represents "LIKE ESCAPE" clause
     * for this database
     */
    public String getLeftLikeEscape() {
        String s = (String)dbMap.get(LEFT_LIKE_ESCAPE);
        if (s == null)
            s = NONE;

        if (logger.isLoggable()) {
            logger.fine("sqlstore.database.dbvendor.getleftlikeescape", s); // NOI18N
        }

        return SPACE + s;
    }

    /**
     * Returns right string that represents "LIKE ESCAPE" clause
     * for this database
     */
    public String getRightLikeEscape() {
        String s = (String)dbMap.get(RIGHT_LIKE_ESCAPE);
        if (s == null)
            s = NONE;

        if (logger.isLoggable()) {
            logger.fine("sqlstore.database.dbvendor.getrightlikeescape", s); // NOI18N
        }

        return SPACE + s;
    }

    /**
     * Returns function name for comparing null value for this database
     */
    public String getNullComparisonFunctionName() {
        String s = (String)dbMap.get(NULL_COMPARISON_FUNCTION_NAME);
        if (s == null)
            s = NONE;
        else
            s = s.trim();
        if (logger.isLoggable()) {
            logger.fine("sqlstore.database.dbvendor.getNullComparisonFunctionName", s); // NOI18N
        }
        return s;
    }


    /**
     * Returns true if modulo operation uses function, false otherwise.
     */
    public boolean isModOperationUsingFunction() {
        return getModFunctionName().length() != 0;
    }

    /**
     * Returns function name for MOD.
     */
    public String getModFunctionName() {
        String s = (String)dbMap.get(MOD_FUNCTION_NAME);
        if (s == null) {
           throw new JDOUserException(I18NHelper.getMessage(messages,
               "core.constraint.illegalop", // NOI18N
               " % "));
        }

        if (logger.isLoggable()) {
            logger.fine("sqlstore.database.dbvendor.getModFunctionName", s); // NOI18N
        }

        return s;
    }

    /**
     * Returns cast name that surrounds concat operation.
     */
    public String getConcatCast() {
        String s = (String)dbMap.get(CONCAT_CAST);
        if (s == null)
            s = NONE;
        else
            s = s.trim();
        if (logger.isLoggable()) {
            logger.fine("sqlstore.database.dbvendor.getConcatCast", s); // NOI18N
        }
        return s;
    }

    /**
     * Returns true if parameters need to be casted for this database
     */
    public boolean isParameterCast() {
        String s = (String)dbMap.get(PARAMETER_CAST);
        Boolean b = Boolean.valueOf(s);

        if (logger.isLoggable()) {
            logger.fine("sqlstore.database.dbvendor.isParameterCast", b); // NOI18N
        }

        return b.booleanValue();
    }

    /**
     * Returns true if numeric parameters are inlined for this database
     */
    public boolean isInlineNumeric() {
        String s = (String)dbMap.get(INLINE_NUMERIC);
        Boolean b = Boolean.valueOf(s);

        if (logger.isLoggable()) {
            logger.fine("sqlstore.database.dbvendor.isInlineNumeric", b); // NOI18N
        }

        return b.booleanValue();
    }

    /**
     * Returns the string that represents the not-equal-to operator
     * for this database. This has been added for Symfoware database
     * which does not support "!=" for NOT EQUAL, and will have to use
     * "<>".
     */
    public String getNotEqual() {
        String s = (String)dbMap.get(NOT_EQUAL);
        if (s == null)
            s = NONE;

        if (logger.isLoggable()) {
            logger.fine("sqlstore.database.dbvendor.getnotequal", s); // NOI18N
        }

        return s;
    }
    
    

    /**
     * Returns database vendor type
     */
    public String getName() {
        return vendorType;
    }

    /**
     * Returns a SpecialDBOperation object
     */
    public SpecialDBOperation getSpecialDBOperation() {
        if (logger.isLoggable()) {
            logger.fine("sqlstore.database.dbvendor.getSpecialDBOperation", specialDBOperation); // NOI18N
        }

        return specialDBOperation;
    }

    /**
     * Gets string used as parameter marker. Parameters must be
     * casted on DB2/Derby.
     * @param type type for which parameter marker is required.
     * @return parameter marker for <code>type</code>.
     */
    public String getParameterMarker(int type) {
        String paramMarker = "?"; // NOI18N
        if (isParameterCast()) {
            String castType = getCastType(type);
            if (castType != null) {
                paramMarker = "CAST (? AS " + castType + ")"; // NOI18N
            }
        }

        return paramMarker;
    }

    /**
     * Gets string used as parameter marker. Parameters must be
     * casted on DB2/Derby. The cast type should match the actual
     * JDBC call used to bind the parameter.
     * @param  type type for which cast type is required.
     * @return String used as cast type for input type.
     * <code>null</code> if cast type is not required.
     */
    private static String getCastType(int type) {
    //
    //DB2type      length(bits)    JavaType
    //----------------------------------------------
    //SMALLINT      16             boolean, byte, short
    //INTEGER       32             int, char(16 bit unsigned)
    //BIGINT        64             long
    //REAL          32             float
    //DOUBLE        64             double
    //
        String castType = null;
        switch(type) {
            case FieldTypeEnumeration.BOOLEAN_PRIMITIVE :
            case FieldTypeEnumeration.BOOLEAN :
                castType = "SMALLINT";
                break;

            // CHARACTER_PRIMITIVE and CHARACTER are bound
            // to a PreparedStatement as String.
            case FieldTypeEnumeration.CHARACTER_PRIMITIVE :
            case FieldTypeEnumeration.CHARACTER :
            case FieldTypeEnumeration.STRING :
                castType = "VARCHAR(32672)"; //Max length of varchar col on DB2
                break;

            case FieldTypeEnumeration.BYTE_PRIMITIVE :
            case FieldTypeEnumeration.BYTE :
                castType = "SMALLINT";
                break;

            case FieldTypeEnumeration.SHORT_PRIMITIVE :
            case FieldTypeEnumeration.SHORT :
                castType = "SMALLINT";
                break;

            case FieldTypeEnumeration.INTEGER_PRIMITIVE :
            case FieldTypeEnumeration.INTEGER :
                castType = "INTEGER";
                break;

            case FieldTypeEnumeration.LONG_PRIMITIVE :
            case FieldTypeEnumeration.LONG :
                castType = "BIGINT";
                break;

            case FieldTypeEnumeration.FLOAT_PRIMITIVE :
            case FieldTypeEnumeration.FLOAT :
                castType = "REAL";
                break;

            case FieldTypeEnumeration.DOUBLE_PRIMITIVE :
            case FieldTypeEnumeration.DOUBLE :
                castType = "DOUBLE";
                break;
    /* Decide on what should this be casted to ?
            case FieldTypeEnumeration.ENUMTYPE_BIGDECIMAL :
            case FieldTypeEnumeration.ENUMTYPE_BIGINTEGER :
            break;
    */
            case FieldTypeEnumeration.UTIL_DATE :
            case FieldTypeEnumeration.SQL_TIMESTAMP :
                castType = "TIMESTAMP";
                break;

            case FieldTypeEnumeration.SQL_DATE :
                castType = "DATE";
                break;

            case FieldTypeEnumeration.SQL_TIME :
                castType = "TIME";
                break;

        }
        return castType;
    }
}
