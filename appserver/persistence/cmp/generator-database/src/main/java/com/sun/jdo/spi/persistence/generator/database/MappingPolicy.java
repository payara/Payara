/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2016 Oracle and/or its affiliates. All rights reserved.
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
 * MappingPolicy.java
 *
 * Created on Jan 14, 2003
 */

package com.sun.jdo.spi.persistence.generator.database;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.sql.Types;

import org.glassfish.persistence.common.I18NHelper;
import com.sun.jdo.spi.persistence.utility.StringHelper;

import com.sun.jdo.spi.persistence.utility.logging.Logger;

import org.glassfish.persistence.common.database.DBVendorTypeHelper;
import org.glassfish.persistence.common.DatabaseConstants;

// XXX Capitalization of acronyms such as Jdbc vs. JDBC is inconsistent
// throught out this package.

/**
 * Describes how Java classes and their fields are to be mapped to database
 * tables and columns
 */
public class MappingPolicy implements Cloneable {
    //
    // The names of many properties in our .properties files are composed of
    // different pieces, here called "bases", followed by "indicators".  The
    // idea is that a base may have multiple indicators.  In a way, they are
    // kind of like instances of structs, with each base naming an instance
    // and each indicator naming an accessor to a value in the struct.
    //
    // The concatenation of bases and indicators is just another String,
    // which you can look up in a Properties object to get a value.
    //
    // Note that some property names have more than one base concatenated.
    // See SQL92.properties for examples.
    //
    
    /** @see DatabaseGenerationConstants#DOT */
    static final char DOT = DatabaseGenerationConstants.DOT;

    //
    // Base names of properties which describe how class and field names are
    // to be represented in a database.
    //

    /** Base name to denote a class. */
    private static final String CLASS_BASE = "{class-name}"; //NOI18N

    /** Base name to denote a field. */
    private static final String FIELD_BASE = "{field-name}"; //NOI18N

    /** Base name to denote a relationship field. */
    private static final String RELATIONSHIP_BASE =
            "{relationship-field-name}"; //NOI18N


    /** Represents a '.' in a regular expression */
    private static String REGEXP_DOT = "\\."; // NOI18N

    /** Synonym for DatabaseGenerationConstants.INDICATOR_JDBC_PREFIX. */
    private static final String INDICATOR_JDBC_PREFIX =
        DatabaseGenerationConstants.INDICATOR_JDBC_PREFIX;

    /**
     * Base name to denote maximum length of a name in a database.  We
     * support different maximum lengths for table, column, and constraint
     * names.
     */
    private static final String INDICATOR_MAXIMUM_LENGTH =
        DatabaseGenerationConstants.INDICATOR_MAXIMUM_LENGTH;

    //
    // Indicator names for properties which describe how class and field
    // names are to be represented in a database.
    //
    
    /** Indicator that property is for a table name. */
    private static final String INDICATOR_TABLE_NAME =
        "table-name"; //NOI18N

    /** Indicator that property is for a column name. */
    private static final String INDICATOR_COLUMN_NAME =
        "column-name"; //NOI18N

    /** Indicator that property is for a join table name. */
    private static final String INDICATOR_JOIN_TABLE_NAME =
        "join-table-name"; //NOI18N

    /** Indicator that property is for a constraint name. */
    private static final String INDICATOR_CONSTRAINT_NAME =
        "constraint-name"; //NOI18N


    //
    // These are complete property names composed of bases and indicators.
    //

    /** Prefix of properties that denote classes. */
    private static final String CLASS_PREFIX =
        CLASS_BASE + DOT;

    /** Prefix of properties that denote classes. */
    private static final String RELATIONSHIP_PREFIX =
        CLASS_PREFIX + RELATIONSHIP_BASE + DOT;

    /** Name of property that provides default field-to-column name mapping. */
    private static final String DEFAULT_COLUMN_KEY =
        CLASS_PREFIX + FIELD_BASE + DOT + INDICATOR_COLUMN_NAME;

    /** Name of property that provides default jointable name mapping. */
    private static final String DEFAULT_JOIN_TABLE_KEY =
        RELATIONSHIP_PREFIX + INDICATOR_JOIN_TABLE_NAME;

    /** Name of property that provides default constraint name mapping. */
    private static final String DEFAULT_CONSTRAINT_KEY =
        RELATIONSHIP_PREFIX + INDICATOR_CONSTRAINT_NAME;

    /** Name of property that provides default class-to-table name mapping. */
    private static final String DEFAULT_TABLE_KEY =
        CLASS_PREFIX + INDICATOR_TABLE_NAME;


    //
    // Now, here are values of properties which indicate how table and column
    // names are to be generated.  I.e., in our .properties files, these are
    // potential values for some property names composed of the above bases
    // and indicators.
    //
    
    /** Property value indicating table name must be same as class name. */
    private static final String TABLE_NAME_AS_CLASSNAME =
        "{className}"; //NOI18N

    /** Property value indicating table name must be upper case. */
    private static final String TABLE_NAME_UPPERCASE =
        TABLE_NAME_AS_CLASSNAME.toUpperCase();

    /** Property value indicating table name must be uppercase and unique. */
    private static final String TABLE_NAME_HASH_UPPERCASE =
        "{HASH-CLASSNAME}"; //NOI18N

    /** Property value indicating colum name must be same as field name. */
    private static final String COLUMN_NAME_AS_FIELDNAME =
        "{fieldName}"; //NOI18N

    /** Property value indicating column name must be uppercase. */
    private static final String COLUMN_NAME_UPPERCASE =
        COLUMN_NAME_AS_FIELDNAME.toUpperCase();

    /** Property value indicating join table name must be uppercase. */
    private static final String JOIN_TABLE_NAME_UPPERCASE =
        "{CLASSNAMES}"; //NOI18N

    /** Property value indicating constraint name must be uppercase. */
    private static final String CONSTRAINT_NAME_UPPERCASE =
        "{FIELDNAMES}"; //NOI18N


    //
    // Here are indicators for properties that direct how vendor-dependent
    // SQL is generated.
    //

    /** Indicator that property is for formatting SQL */
    private static final String INDICATOR_SQL_FORMAT = "sql-format"; //NOI18N

    /** The indicator for a statement separator. */
    private static final String STATEMENT_SEPARATOR_INDICATOR =
        "statementSeparator"; // NOI18N

    /** The indicator for starting a "create table". */
    private static final String CREATE_TABLE_START_INDICATOR =
        "createTableStart"; // NOI18N

    /** The indicator for ending a "create table". */
    private static final String CREATE_TABLE_END_INDICATOR =
        "createTableEnd"; // NOI18N

    /** The indicator for "create index". Added for Symfoware support as */
    /** indexes on primary keys are mandatory */
    private static final String CREATE_INDEX_INDICATOR =
        "createIndex"; // NOI18N

    /** The indicator for starting a "drop table". */
    private static final String DROP_TABLE_INDICATOR =
        "dropTable"; // NOI18N

    /** The indicator for "add constraint". */
    private static final String ALTER_TABLE_ADD_CONSTRAINT_START_INDICATOR =
        "alterTableAddConstraintStart"; // NOI18N

    /** The indicator for "drop constraint". */
    private static final String ALTER_TABLE_DROP_CONSTRAINT_INDICATOR =
        "alterTableDropConstraint"; // NOI18N

    /** The indicator for adding a primary key constraint. */
    private static final String PRIMARY_KEY_CONSTRAINT_INDICATOR =
        "primaryKeyConstraint"; // NOI18N

    /** The indicator for adding a foreign key constraint. */
    private static final String FOREIGN_KEY_CONSTRAINT_INDICATOR =
        "foreignKeyConstraint"; // NOI18N

    /** The indicator for verbose nullability. */
    private static final String COLUMN_NULLABILITY_INDICATOR =
        "columnNullability"; // NOI18N

    /** The indicator for information used with LOB columns. */
    private static final String LOB_LOGGING_INDICATOR =
        "LOBLogging"; // NOI18N

    //
    // The remaining constants are neither bases nor indicators.
    //

    /** Prefix of column names which are primary key columns. */
    private static final String PK_PREFIX = "PK_"; //NOI18N

    /** Prefix of column names which are foreign key columns. */
    private static final String FK_PREFIX = "FK_"; //NOI18N
    
    /** Name of the "global" namespace. */
    private static final String GLOBAL_NAMING_SPACE = "GLOBAL"; //NOI18N

    /** Property name which indicates unique table names should be generated. */
    public static final String USE_UNIQUE_TABLE_NAMES = "use-unique-table-names"; // NOI18N

    /** Property name which indicates reserved words. */
    private static final String RESERVED_WORDS = "reserved-words";// NOI18N

    /** When appended to a reserved word, causes it to be not-reserved. */
    private static final String RESERVED_WORD_UNRESERVER = "9"; // NOI18N

    /**
     * Maximum length of the counter used to create unique names with a
     * numeric id.  Note that this length includes a NAME_SEPARATOR, so that
     * we allow for 3 digits total.
     */
    private static final int MAX_LEN_COUNTER = 4;

    /** Number of chars to change a reserved word into * unreserved. */
    private static final int MAX_LEN_RESERVED = 1;

    /**
     * Name of subdirectory in which db vendor - specific properties files
     * are located.
     */
    private static final String PROPERTY_FILE_DIR =
        "com/sun/jdo/spi/persistence/generator/database/"; // NOI18N

    /** Extension used by properties files. */
    private static final String PROPERTY_FILE_EXT = ".properties"; // NOI18N

    //
    // The above are all constants; below things get "interesting".
    //
    
    
    /** This is the set of all default properties. */
    private static final Properties defaultProps = new Properties();

    //
    // XXX Consider getting these String-Integer and Integer-String maps into
    // SQLTypeUtil, which is in the dbschema module.  Or not: we support only a
    // subset of java.sql.types, and (presumably) the dbschema module would
    // provide mappings for all types.
    //
    // Note also that is code in MappingGenerator of a "SQLTypeUtil" nature.
    //

    /**
     * Map from String names to the Integer-boxed values from
     * java.sql.Types.
     */
    private static final Map jdbcTypes = new HashMap();

    /**
     * Maps from Integer-boxed values from java.sql.Types to String names.
     */
    private static final Map jdbcTypeNames = new HashMap();


    /**
     * Global counter for creating unique names in each of the namespaces.
     * <em>Note that a single counter is used across all namespaces.</em>
     */
    private int counter = 0;
    
    /**
     * Map from namespaces to Set of names defined in each namespace.  Used
     * to ensure uniqueness within namespaces.
     */
    private Map namespaces = new HashMap();

    /**
     * Indicates whether or not generated table names should include a
     * unique value as part of their names.
     */
    private boolean uniqueTableName = false;

    /**
     * Set of reserved words for a particular policy.
     */
    private final Set reservedWords = new TreeSet();

    /**
     * Set of reserved words for the default database.
     */
    private static Set defaultReservedWords;
    
    /**
     * Map from the string names of the java types (e.g. "java.lang.String")
     * to a JDBCInfo of information about the corresponding java.sql.Types
     * type.  Different for different dbvendor types, but the same instance,
     * per dbvendor, is shared by all MappingPolicy instances.
     */
    private final Map dbJdbcInfoMap = new HashMap();

    /**
     * Similar to {@link #dbJdbcInfoMap}, but is reinitialized by each
     * clone().  Contains user-provided overrides of the information in
     * dbjdbcInfoMap.
     */
    private Map userJdbcInfoMap = new HashMap();

    /**
     * Map from a boxed value based on fields in java.sql.Types to the String
     * name of a SQL type.
     */
    private final Map sqlInfo = new HashMap();


    //
    // Maximum lengths for table, column, and constraint names are
    // vendor-specific.
    //
    
    /** Maximum length of the name of a table. */
    private int tableNameMaxLength;

    /** Maximum length of the name of a column. */
    private int columnNameMaxLength;

    /** Maximum length of the name of a  constraint. */
    private int constraintNameMaxLength;


    //
    // Strings to represent vendor-specific SQL that starts a table,
    // separates statements, etc.
    //

    /** The SQL for a statement separator. */
    private String statementSeparator;

    /** The SQL for starting a "create table". */
    private String createTableStart;

    /** The SQL for ending a "create table". */
    private String createTableEnd;

    /** The SQL for "create index". */
    private String createIndex;

    /** The SQL for "drop table". */
    private String dropTable;

    /** The SQL for "add constraint". */
    private String alterTableAddConstraintStart;

    /** The SQL for "drop constraint". */
    private String alterTableDropConstraint;

    /** The SQL for adding a primary key constraint. */
    private String primaryKeyConstraint;

    /** The SQL for adding a foreign key constraint. */
    private String foreignKeyConstraint;

    /** The SQL for indicating column nullability */
    private String columnNullability;

    /** The SQL for indicating LOB column logging */
    private String lobLogging = "";

    /**
     * Map from the encoded name of a policy to its value.  For example, a
     * class name's naming policy would be encoded as
     * "<classname>.table-name".
     */
    // XXX Consider renaming this.
    private final Map namingPolicy = new HashMap();

    /** Map from database vendor names to instances of MappingPolicy. */
    private static final Map instances = new HashMap();

     /** Logger for warning & error messages */
    private static final Logger logger =
            LogHelperDatabaseGenerator.getLogger();
    
    /** I18N message handler */
    private final static ResourceBundle messages =
            I18NHelper.loadBundle(MappingPolicy.class);

    //
    // Initialize the JDBC String to Integer map and the default (SQL92)
    // MappingPolicy.
    //

    // XXX Why initialize the SQL policy, when there's a good chance it won't
    // ever be used?  Do we really want to support unrecognized databases?
    // See comment in getMappingPolicy.  The default properties, on the other
    // hand, *do* need to be loaded.

    // XXX We need to decide what happens when an unrecognized dbvendorname
    // is given: Error?  Warning, continue running?
    static {
        // Initialize jdbcType map.
        jdbcTypes.put("BIGINT", new Integer(Types.BIGINT)); // NOI18N
        jdbcTypes.put("BIT", new Integer(Types.BIT)); // NOI18N
        jdbcTypes.put("BLOB", new Integer(Types.BLOB)); // NOI18N
        jdbcTypes.put("CHAR", new Integer(Types.CHAR)); // NOI18N
        jdbcTypes.put("CLOB", new Integer(Types.CLOB)); // NOI18N
        jdbcTypes.put("DATE", new Integer(Types.DATE)); // NOI18N
        jdbcTypes.put("DECIMAL", new Integer(Types.DECIMAL)); // NOI18N
        jdbcTypes.put("DOUBLE", new Integer(Types.DOUBLE)); // NOI18N
        jdbcTypes.put("INTEGER", new Integer(Types.INTEGER)); // NOI18N
        jdbcTypes.put("LONGVARBINARY", new Integer(Types.LONGVARBINARY)); // NOI18N
        jdbcTypes.put("LONGVARCHAR", new Integer(Types.LONGVARCHAR)); // NOI18N
        jdbcTypes.put("NULL", new Integer(Types.NULL)); // NOI18N
        jdbcTypes.put("REAL", new Integer(Types.REAL)); // NOI18N
        jdbcTypes.put("SMALLINT", new Integer(Types.SMALLINT)); // NOI18N
        jdbcTypes.put("TIME", new Integer(Types.TIME)); // NOI18N
        jdbcTypes.put("TIMESTAMP", new Integer(Types.TIMESTAMP)); // NOI18N
        jdbcTypes.put("TINYINT", new Integer(Types.TINYINT)); // NOI18N
        jdbcTypes.put("VARCHAR", new Integer(Types.VARCHAR)); // NOI18N
        
        jdbcTypeNames.put(new Integer(Types.BIGINT), "BIGINT"); // NOI18N
        jdbcTypeNames.put(new Integer(Types.BIT), "BIT"); // NOI18N
        jdbcTypeNames.put(new Integer(Types.BLOB), "BLOB"); // NOI18N
        jdbcTypeNames.put(new Integer(Types.CHAR), "CHAR"); // NOI18N
        jdbcTypeNames.put(new Integer(Types.CLOB), "CLOB"); // NOI18N
        jdbcTypeNames.put(new Integer(Types.DATE), "DATE"); // NOI18N
        jdbcTypeNames.put(new Integer(Types.DECIMAL), "DECIMAL"); // NOI18N
        jdbcTypeNames.put(new Integer(Types.DOUBLE), "DOUBLE"); // NOI18N
        jdbcTypeNames.put(new Integer(Types.INTEGER), "INTEGER"); // NOI18N
        jdbcTypeNames.put(new Integer(Types.LONGVARBINARY), "LONGVARBINARY"); // NOI18N
        jdbcTypeNames.put(new Integer(Types.LONGVARCHAR), "LONGVARCHAR"); // NOI18N
        jdbcTypeNames.put(new Integer(Types.NULL), "NULL"); // NOI18N
        jdbcTypeNames.put(new Integer(Types.REAL), "REAL"); // NOI18N
        jdbcTypeNames.put(new Integer(Types.SMALLINT), "SMALLINT"); // NOI18N
        jdbcTypeNames.put(new Integer(Types.TIME), "TIME"); // NOI18N
        jdbcTypeNames.put(new Integer(Types.TIMESTAMP), "TIMESTAMP"); // NOI18N
        jdbcTypeNames.put(new Integer(Types.TINYINT), "TINYINT"); // NOI18N
        jdbcTypeNames.put(new Integer(Types.VARCHAR), "VARCHAR"); // NOI18N
        
        try {

            // Create and load the default mapping policy.
            new MappingPolicy();

        } catch (Throwable ex) {
            logger.log(Logger.SEVERE,
                       I18NHelper.getMessage(
                               messages,
                               "EXC_MappingPolicyNotFound",  //NOI18N
                               DBVendorTypeHelper.DEFAULT_DB));
        }
    }

    /**
     * Create the default MappingPolicy instance.
     */
    // This should be invoked only once per JVM.  See the class static
    // block of code above.
    private MappingPolicy() throws IOException {
        load(getPropertyFileName(DBVendorTypeHelper.DEFAULT_DB),
             defaultProps, false);
        init(defaultProps);

        // The DEFAULT_DB has reserved words for the default database type.
        // Other databases can access those same values through the
        // defaultReservedWords set.
        defaultReservedWords = reservedWords;

        instances.put(DBVendorTypeHelper.DEFAULT_DB, this);

        if (logger.isLoggable(Logger.FINEST)) {
            logger.finest("new MappingPolicy():\n" + toString()); // NOI18N
        }
    }

    /**
     * Create a MappingPolicy for the named database type.
     * @param databaseType Name of the database for which a MappingPolicy is
     * created.  The name must conform to one of the .properties files in
     * this package.
     */
    // This should be invoked only once per databaseType per JVM.  See
    // {@link #getMappingPolicy}.
    private MappingPolicy(String databaseType) throws IOException {
        Properties mergedProp = new Properties(defaultProps);
        load(getPropertyFileName(databaseType), mergedProp, false);
        init(mergedProp);
        instances.put(databaseType, this);        

        if (logger.isLoggable(Logger.FINEST)) {
            logger.finest("new MappingPolicy(" // NOI18N
                + databaseType + "):\n" + toString()); // NOI18N
        }
    }

    /**
     * Returns a vendor-specifc MappingPolicy instance.  This method always
     * returns a copy (clone) of the known MappingPolicy to allow for
     * user-specific overrides.
     * @param databaseType a database vendor name as a String.
     * @return MappingPolicy instance corresponding to the provided
     * database vendor name. 
     * @throws IOException if there are problems reading the vendor-
     * specific mappinng policy file 
     */
    public synchronized static MappingPolicy getMappingPolicy(
           String databaseType) throws IOException {
               
        if (logger.isLoggable(Logger.FINE)) {
            logger.fine("get MappingPolicy"+databaseType); // NOI18N
        }
        
        MappingPolicy mappingPolicy = null;
        try {
            if (databaseType == null) {
                databaseType = DBVendorTypeHelper.DEFAULT_DB;
                // XXX FIXME Need to log a warning and report to user that we
                // are *not* using databaseType given, that we are using
                // SQL92 instead, and provide list of recognized names.
            }
            mappingPolicy = (MappingPolicy) instances.get(databaseType);
            if (mappingPolicy == null) {
                mappingPolicy = new MappingPolicy(databaseType);
            }
            mappingPolicy = (MappingPolicy) mappingPolicy.clone();
        } catch (CloneNotSupportedException ec) {
            // ignore it because it will not happen
        }
        return mappingPolicy;
    }

    /**
     * Clones the vendor-specific policy for generator session.  Replace the
     * namespaces map in the clone, so that each instance has its own.
     * @return clone of this MappingPolicy
     * @throws CloneNotSupportedException never thrown
     */
    protected Object clone() throws CloneNotSupportedException {
        MappingPolicy mappingPolicyClone = (MappingPolicy) super.clone();
        mappingPolicyClone.namespaces = new HashMap();
        mappingPolicyClone.uniqueTableName = false;
        mappingPolicyClone.userJdbcInfoMap = new HashMap();
        return mappingPolicyClone;
    }

    /**
     * Initializes the given properties from the given resourceName.
     * @param resourceName Name of the resource to open, expected to contain
     * properties in text form.
     * @param properties Properties that are to be loaded.
     * @param override If <code>true</code>, treat resourceName as a filename
     * from which to read; if <code>false</code>, treat resourceName as the
     * name of a resource accessed via the class loader which loaded the
     * MappingPolicy class.
     */
    private synchronized void load(
            final String resourceName, Properties properties, boolean override)
            throws IOException {
 
        if (logger.isLoggable(Logger.FINE)) {
            logger.fine("load resource:" + resourceName); // NOI18N
        }

        InputStream bin = null;
        InputStream in = null;

        try {
            if (override) {
                in = new FileInputStream(resourceName);
            } else {
                final ClassLoader loader =
                        MappingPolicy.class.getClassLoader();
                in = (InputStream) AccessController.doPrivileged(
                        new PrivilegedAction() {

                            public Object run() {
                                Object rc = null;
                                if (loader != null) {
                                    rc =loader.getResourceAsStream(
                                            resourceName);
                                } else {
                                    rc =
                                        ClassLoader.getSystemResourceAsStream(
                                                resourceName);
                                }
                                return rc;
                            }
                        });
                if (in == null) {
                    throw new IOException(I18NHelper.getMessage(messages,
                        "EXC_ResourceNotFound", resourceName));// NOI18N
                }
            }

            bin = new BufferedInputStream(in);
            properties.load(bin);
            if (logger.isLoggable(Logger.FINE)) {
                logger.fine("load "+resourceName + " successfuly"); // NOI18N
            }
        } finally {
            try {
                bin.close();
                // XXX Need to close both streams in.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }
 
    /**
     * Resets the namespaces and counter.
     */
    // XXX Consider renaming to "reset".
    void resetCounter() {
        namespaces.clear();
        userJdbcInfoMap.clear();
        counter = 0;
    }

    /**
     * Sets user-provided policy to that provided in the given properties.
     * @param props Properties which override built in defaults.
     */
    public void setUserPolicy(Properties props) {
        if (null != props) {

            // Look for and set JDBCInfo entries.  Use Enumeration instead of
            // iterator because former gets default values while latter does
            // not.
            for (Enumeration e = props.propertyNames(); e.hasMoreElements();) {
                String name = (String) e.nextElement();
                String value = props.getProperty(name);

                if (name.equals(USE_UNIQUE_TABLE_NAMES)) {
                    if (! StringHelper.isEmpty(value)) {
                        uniqueTableName =
                            Boolean.valueOf(value).booleanValue();
                    }
                    continue;
                }

                StringTokenizer nameParser =
                    new StringTokenizer(name, String.valueOf(DOT));

                // Get the last element from key which is separated by DOT.
                String indicator = null;
                while (nameParser.hasMoreTokens()) {
                    indicator = nameParser.nextToken();
                }

                if (indicator.startsWith(INDICATOR_JDBC_PREFIX)) {
                    setJDBCInfoEntry(userJdbcInfoMap, name, value, indicator);
                } else {
                    if (logger.isLoggable(Logger.INFO)) {
                        logger.info(
                                I18NHelper.getMessage(
                                        messages,
                                        "MSG_UnexpectedUserProp", // NOI18N
                                        name, value)); // NOI18N
                    }
                }
            }
        }
    }

    /**
     * Sets whether or not unique table names should be generated.
     * @param uniqueTableName If <code>true</code>, tables names will be
     * unique.
     */
    public void setUniqueTableName(boolean uniqueTableName) {
        this.uniqueTableName = uniqueTableName;
    }

    /**
     * Returns the String name of the SQL type for a given JDBC type.
     * @param jdbcType One of the values from java.sql.Types.
     * @return Name of SQL type corresponding to given jdbcType.
     */
    public String getSQLTypeName(int jdbcType) {
        String rc = null;

        // The name is in sqlInfo if it was loaded from one of our
        // vendor-specific properties files.
        Object o = sqlInfo.get(new Integer(jdbcType));
        if (null != o) {
            rc = (String) o;
        } else {
            // Otherwise, user has overriden, e.g. java.lang.String -> CLOB.
            rc = getJdbcTypeName(jdbcType);
        }

        return rc;
    }

    /**
     * Returns JDBC type information corresponding to the given field name
     * and type.  If fieldName is <code>null</code> or there is no
     * fieldName - specific information already existing, the default for the
     * given fieldType is returned.  Otherwise, information specific to the
     * fieldName is returned.  <em>Note:</em> It is possible to have a field
     * in a class have the same name as another class; this mechanism is not
     * robust in that case:
     * <pre>
     * class Foo { }
     *
     * class MyFoo {
     *     Foo Foo = new Foo();
     * }
     * </pre>
     * We think this obfuscation unlikely to occur.
     * @param fieldName Name of field for which information is needed.  May
     * be null, in which case only fieldType is used to determine returned
     * information.
     * @param fieldType Name of a Java type.
     * @return JDBCInfo representing the field or type.
     */
    public JDBCInfo getJDBCInfo(String fieldName, String fieldType) {
        JDBCInfo rc = null;

        if (logger.isLoggable(Logger.FINEST)) {
            logger.finest("Entering MappingPolicy.getJDBCInfo: " // NOI18N
                          + fieldName + ", " + fieldType); // NOI18N
        }

        if (null != fieldName) {

            // If fieldName is given, try to find a JDBCInfo using that name.
            // Looking up fieldName only makes sense in userJdbcInfoMap
            // which contains the user's overrides.
            rc = (JDBCInfo) userJdbcInfoMap.get(fieldName);
            if (null != rc && (! rc.isComplete())) {

                // There is an override for the field named fieldName, but
                // it is not complete, i.e., not all possible information
                // about the field was provided in the user override.
                //
                // Choose a JDBCInfo to use to complete the information in rc.
                // If the user override specifies a type and there is
                // information about that type for the database, use that.
                // Otherwise, use the given fieldType.
                JDBCInfo ji = null;
                if (rc.hasJdbcType()) {
                    ji = getdbJDBCInfo(rc.getJdbcType());
                }
                if (null == ji) {
                    ji = getdbJDBCInfo(fieldType);
                }

                // Fill in the rest of the fields in rc with values from ji.
                rc.complete(ji);
            }
        }

        if (null == rc) {

            // Either fieldName is null, or there is no JDBCInfo specific to
            // fieldName, so use fieldType.
            rc = getdbJDBCInfo(fieldType);
        }

        // If dbJdbcInfoMap has an entry for rc's jdbc type, replace rc's jdbc
        // type with the result of the mapping.  This allows, for example, a
        // user to specify that a field should be represented by a CLOB, when
        // the database and/or driver do not support that but do support
        // LONGVARCHAR (e.g. Sybase).
        JDBCInfo ji = getdbJDBCInfo(rc.getJdbcType());
        rc.override(ji);

        if (logger.isLoggable(Logger.FINEST)) {
            logger.finest("Leaving MappingPolicy.getJDBCInfo: " // NOI18N
                          + fieldName + ", " + fieldType // NOI18N
                          + " => " + rc); // NOI18N
        }

        return rc;
    }

    /**
     * Gets the JDBCInfo corresponding to the type in the given JDBCInfo.
     * I.e., it maps from one JDBCInfo to another on the basis of their
     * types.
     * @param ji JDBCInfo
     * @return a JDBCInfo
     * @throws IllegalArgumentException if <code>type</code> is not recognized
     * as being a valid member of java.sql.Types.  Note that only a subset of
     * the types in java.sql.Types are recognized.
     */
    private JDBCInfo getdbJDBCInfo(int jdbcType) {
        String typename = getJdbcTypeName(jdbcType);
        return (JDBCInfo) dbJdbcInfoMap.get(typename);
    }

    /**
     * Gets the JDBCInfo for the given fieldType
     * @param fieldType Name of the type of a field
     * @return a JDBCInfo for the given fieldType
     */
    private JDBCInfo getdbJDBCInfo(String fieldType) {
        JDBCInfo rc = (JDBCInfo) dbJdbcInfoMap.get(fieldType);
        
        if (null == rc) {
            
            // There is also nothing provided for the field's
            // type, so use a BLOB.
            rc = (JDBCInfo) dbJdbcInfoMap.get("BLOB"); // NOI18N
        }
        return rc;
    }

    /**
     * Returns the boxed form of the java.sql.Types value corresponding to the
     * given name.
     * @param jdbcStringType Name of the value to return.
     * @return Value from java.sql.Types, wrapped into an Integer, or null if
     * jdbcTypeName is not that of a recognized JDBC type.
     */
    static Integer getJdbcType(String jdbcTypeName) {
        return (Integer) jdbcTypes.get(jdbcTypeName.toUpperCase());
    }

    /**
     * Provides a String that can be recognized as a policy to override the
     * default length of a field.
     * @param className name of a class
     * @param fieldName name of a field in that class
     * @return a String that can be used as the name of a length override
     * for a field in a class.
     */
    public static String getOverrideForLength(
            String className, String fieldName) {
        
        return className
            + DOT + fieldName
            + DOT + DatabaseGenerationConstants.INDICATOR_JDBC_LENGTH;
    }

    /**
     * Provides a String that can be recognized as a policy to override the
     * default nullability of a field.
     * @param className name of a class
     * @param fieldName name of a field in that class
     * @return a String that can be used as the name of a nullability override
     * for a field in a class.
     */
    public static String getOverrideForNullability(
            String className, String fieldName) {
        
        return className
            + DOT + fieldName
            + DOT + DatabaseGenerationConstants.INDICATOR_JDBC_NULLABLE;
    }

    /**
     * Provides a String that can be recognized as a policy to override the
     * default precision of a field.
     * @param className name of a class
     * @param fieldName name of a field in that class
     * @return a String that can be used as the name of a precision override
     * for a field in a class.
     */
    public static String getOverrideForPrecision(
            String className, String fieldName) {
        
        return className
            + DOT + fieldName
            + DOT + DatabaseGenerationConstants.INDICATOR_JDBC_PRECISION;
    }

    /**
     * Provides a String that can be recognized as a policy to override the
     * default scale of a field.
     * @param className name of a class
     * @param fieldName name of a field in that class
     * @return a String that can be used as the name of a scale override
     * for a field in a class.
     */
    public static String getOverrideForScale(
            String className, String fieldName) {
        
        return className
            + DOT + fieldName
            + DOT + DatabaseGenerationConstants.INDICATOR_JDBC_SCALE;
    }

    /**
     * Provides a String that can be recognized as a policy to override the
     * default type of a field.
     * @param className name of a class
     * @param fieldName name of a field in that class
     * @return a String that can be used as the name of a type override
     * for a field in a class.
     */
    public static String getOverrideForType(
            String className, String fieldName) {
        
        return className
            + DOT + fieldName
            + DOT + DatabaseGenerationConstants.INDICATOR_JDBC_TYPE;
    }


    /**
     * Provide the String name of a JDBC type, as per java.sql.Types.
     * @param type A value from java.sql.Types
     * @return the String name corresponding to <code>type</code>
     * @throws IllegalArgumentException if <code>type</code> is not recognized
     * as being a valid member of java.sql.Types.  Note that only a subset of
     * the types in java.sql.Types are recognized.
     */
    public static String getJdbcTypeName(int type) throws
            IllegalArgumentException {
        String rc = (String) jdbcTypeNames.get(new Integer(type));
        if (null == rc) {
            throw new IllegalArgumentException();
        }
        return rc;
    }

    //
    // In the getZZZName methods below, we lookup a policy and determine a
    // name based on that policy.  Note that the keys used for lookup are
    // created from the given name(s), so that the result of looking up a
    // policy might actually be the name that is returned.
    //
    
    /**
     * Returns the name of a table for a given class, as per current policy.
     * @param name Basis for what the returned table should be named, for
     * example the unqualified name of a class.
     * @param uniqueName Used if the current policy is to return a unique
     * name.  Client must provide a name that is unique to them.
     * @return Name to be used for table.  Regardless of the current policy,
     * the name is different from other names returned during the current run
     * of {@link DatabaseGenerator#generate}.
     */
    // XXX FIXME: If the user needs to provide a unique name, why do we
    // invoke getUniqueGlobalName on it?
    public String getTableName(String name, String uniqueName) {
        StringBuffer key =
            new StringBuffer(name).append(DOT).append(INDICATOR_TABLE_NAME);
        String rc = (String)namingPolicy.get(key.toString());

        if (rc == null) {
            rc = (String)namingPolicy.get(DEFAULT_TABLE_KEY);
        }

        if (uniqueTableName) {
            rc = TABLE_NAME_HASH_UPPERCASE;
        }

        if (rc.equals(TABLE_NAME_UPPERCASE)) {
            rc = name.toUpperCase();
        } else if (rc.equals(TABLE_NAME_AS_CLASSNAME)) {
            rc = name;
        } else if (rc.equals(TABLE_NAME_HASH_UPPERCASE)) {
            rc = uniqueName.toUpperCase();
        }
        
        return getUniqueGlobalName(rc, tableNameMaxLength);
    }

    /**
     * Returns the name of a column for a given field in a given class.  The
     * column name will be unique within the table.
     * @param className Name of the class containing the field.
     * @param fieldName Name of the field for which a column name is returned.
     * @param tableName Name of the table in which the column name is created.
     * @return Name of a column that is unique within the named table.
     */    
    public String getColumnName(String className, String fieldName, 
            String tableName) {

        // Get column naming policy based on className and fieldName
        StringBuffer key = new StringBuffer(className)
            .append(DOT).append(fieldName)
            .append(DOT).append(INDICATOR_COLUMN_NAME);
        String rc = (String)namingPolicy.get(key.toString());

        if (rc == null) {
            // No fieldName specific policy, so use default for className
            key = new StringBuffer(className)
                .append(DOT).append(FIELD_BASE)
                .append(DOT).append(INDICATOR_COLUMN_NAME);
            rc = (String)namingPolicy.get(key.toString());
        }

        if (rc == null) {
            // No overriding policy, so use overall default.
            rc = (String)namingPolicy.get(DEFAULT_COLUMN_KEY);
        }

        if (rc.equals(COLUMN_NAME_UPPERCASE)) {
            rc = fieldName.toUpperCase();
        } else if (rc.equals(COLUMN_NAME_AS_FIELDNAME)) {
            rc = fieldName;
        }

        return getUniqueLocalName(rc, tableName, columnNameMaxLength);
    }

    /**
     * Returns the name of the column which represents a foreign key in the
     * named table.
     * @param tableName Name of the table in which the FK column will be
     * created.
     * @param columnName Name of PK column in referenced table.
     * @return Name of the FK column in the named table.
     */
    // XXX Does this really need to be public?
    // XXX Rename to getFKColumnName
    public String getConstraintColumnName(String tableName,
            String columnName) {

       return getUniqueLocalName(
               new StringBuffer(tableName)
                   .append(DatabaseConstants.NAME_SEPARATOR)
                   .append(columnName).toString(),
               tableName,
               columnNameMaxLength);
    }

    /**
     * Returns the name of a constraint corresponding to the named
     * relationship. 
     * @param relName Name of a relationship.
     * @param uniqueId Id that can be appened to relName to distinguish it
     * from other relNames in the database.  Will be appended only if
     * {@link #uniqueTableName} is true.
     * @return Name of a constraint.
     */
    public String getConstraintName(String relName, String uniqueId) {
        String rc = (String)namingPolicy.get(DEFAULT_CONSTRAINT_KEY);

        if (rc.equals(CONSTRAINT_NAME_UPPERCASE)) {
            rc = FK_PREFIX + relName.toUpperCase();
        }

        if (uniqueTableName) {
            rc += uniqueId;
        }

        rc = getUniqueGlobalName(rc, constraintNameMaxLength);

        if (logger.isLoggable(Logger.FINER)) {
            logger.finer("MappingPolicy.getConstraintName: " // NOI8N
                         + relName + " -> " + rc); // NOI18N
        }

        return rc;
    }

    /**
     * Returns the name of a PK constraint, unique-ified as required.
     * @param tableName Name of a table on which a constraint is to be placed. 
     * @return Name of a constraint on named table.
     */
    public String getPrimaryKeyConstraintName(String tableName) {
        return getUniqueGlobalName(PK_PREFIX + tableName, constraintNameMaxLength);
    }

    /**
     * Returns the name of a join table which joins the tables that correspond
     * to the two named classes.
     * @param className1 Name of one class to join.
     * @param className2 Name of the other class to join.
     * @return Name of a join table.
     */
    public String getJoinTableName(String className1, String className2) {
        String rc = (String)namingPolicy.get(DEFAULT_JOIN_TABLE_KEY);

        if (rc.equals(JOIN_TABLE_NAME_UPPERCASE)) {
            rc = (className1 + className2).toUpperCase();
        }
        return getUniqueGlobalName(rc, tableNameMaxLength);
    }

    /**
     * Return a unique name for a column.  The column will be unique within
     * the named table.
     * @param colName Name of the column
     * @param tableName Name of the table.
     * @return A unique name for colName within tableName.
     */
    private String getUniqueLocalName(
            String colName, String tableName, int maxLen) {

        return getUniqueName(colName, tableName, maxLen);
    }

    /**
     * Return a unique name for the given name.  It will be "globally" unique
     * between invocations of method {@link #resetCounter}; the first use of
     * a MappingPolicy instance is "reset".
     * @param name Name for which a unique name is returned.
     * @return A unique name for given name.
     */
    private String getUniqueGlobalName(String name, int maxLen) {
        return getUniqueName(name, GLOBAL_NAMING_SPACE, maxLen);
    }

    /**
     * Return a unique name for name.  It will be unique within the given
     * namespace.
     * @param name Name for which a unique name is returned.
     * @param space Namespace in which the returned name is unique.
     * @return A unique name for given name.
     */
    private String getUniqueName(String name, String namespace, int maxLen) {
        String rc = name;

        // Reserve MAX_LEN_COUNTER characters for unique-ifying digits.
        maxLen -= MAX_LEN_COUNTER;

        // Name cannot be more than maxLen chars long.
        if (name.length() > maxLen) {
            rc = name.substring(0, maxLen);
        }

        // Convert to upper case for uniqueing comparisons below.
        String nameUpper = rc.toUpperCase();

        // Ensure the name we create is not a reserved word by comparing
        // nameUpper against reserved words.
        if (defaultReservedWords.contains(nameUpper)
            || reservedWords.contains(nameUpper)) {

            // Append a character that is not used as the last char of any
            // existing reserved words.  Make sure we have space for this plus
            // for any uniqueing below.  Length-limit both rc and nameUpper, so
            // that the value in the namespace and the end result have the same
            // length.
            maxLen -= MAX_LEN_RESERVED;
            if (rc.length() > maxLen) {
                // Limit nameUpper as well as rc because we need to do uniqueing
                // in a case-insensitve fashion.
                nameUpper = nameUpper.substring(0, maxLen);
                rc = rc.substring(0, maxLen);
            }
            nameUpper += RESERVED_WORD_UNRESERVER;
            rc += RESERVED_WORD_UNRESERVER;
        }
        
        Set names = (Set) namespaces.get(namespace);

        if (names == null) {
            // Name is first entry in namespace, therefore already unique, no
            // need to append counter.
            names = new HashSet();
            names.add(nameUpper);
            namespaces.put(namespace, names);

        } else if (names.contains(nameUpper)) {
            // Name is already in namespace, so make a different name by
            // appending a count to given name.
            counter++;
            rc += DatabaseConstants.NAME_SEPARATOR + counter;

        } else {
            // Add new name to namespace.
            names.add(nameUpper);
        }

        return rc;
    }

    //
    // Accessors for SQL formatting Strings.
    //

    /**
     * @return the SQL for a statement separator.
     */
    String getStatementSeparator() {
        return statementSeparator;
    }

    /**
     * @return the SQL for starting a "create table".
     */
    String getCreateTableStart() {
        return createTableStart;
    }

    /**
     * @return the SQL for ending a "create table".
     */
    String getCreateTableEnd() {
        return createTableEnd;
    }

    /**
     * @return the SQL for "create index".
     */
    String getCreateIndex() {
        return createIndex;
    }

    /**
     * @return the SQL for a "drop table".
     */
    String getDropTable() {
        return dropTable;
    }

    /**
     * @return the SQL for "add constraint".
     */
    String getAlterTableAddConstraintStart() {
        return alterTableAddConstraintStart;
    }

    /**
     * @return the SQL for "drop constraint".
     */
    String getAlterTableDropConstraint() {
        return alterTableDropConstraint;
    }

    /**
     * @return the SQL for adding a primary key constraint.
     */
    String getPrimaryKeyConstraint() {
        return primaryKeyConstraint;
    }

    /**
     * @return the SQL for adding a foreign key constraint.
     */
    String getForeignKeyConstraint() {
        return foreignKeyConstraint;
    }

    /**
     * @return the SQL for indicating column nullability
     */
    String getColumnNullability() {
        return columnNullability;
    }

    /**
     * @return the SQL for indicating LOB column logging
     */
    String getLobLogging() {
        return lobLogging;
    }

    //
    // This method and the 4 subsequent ones initialize a MappingPolicy
    // instance from a give Properties object.
    //
    
    /**
     * Initialize this MappingPolicy as per the values in the given
     * properties.
     * @param props Properties for initializing this MappingPolicy
     */
    private void init(Properties props) {
        // Use Enumeration instead of iterator because former gets default
        // values while latter does not.
        for (Enumeration e = props.propertyNames(); e.hasMoreElements();) {
            String name = (String) e.nextElement();
            String value = props.getProperty(name);

            if (name.equals(RESERVED_WORDS)) {
                initReservedWords(value);
                continue;
            }
                
            // The indicator is the last DOT-separated substring in name.
            String indicator = null;
            StringTokenizer nameParser =
                new StringTokenizer(name, String.valueOf(DOT));
            while (nameParser.hasMoreTokens()) {
                indicator = nameParser.nextToken();
            }

            if (indicator.equals(INDICATOR_SQL_FORMAT)) {
                setSqlFormatEntry(name, value);

            } else if (indicator.startsWith(INDICATOR_JDBC_PREFIX)) {
                setJDBCInfoEntry(dbJdbcInfoMap, name, value, indicator);
                
            } else if (indicator.equals(INDICATOR_MAXIMUM_LENGTH)) {
                setLengthEntry(name, value);
                
            } else if (indicator.equals(INDICATOR_TABLE_NAME) || 
                       indicator.equals(INDICATOR_COLUMN_NAME) || 
                       indicator.equals(INDICATOR_JOIN_TABLE_NAME) || 
                       indicator.equals(INDICATOR_CONSTRAINT_NAME)) {
                setNamingEntry(name, value);

            } else {
                setSQLInfoEntry(name, value);
            }
        }
    }

    /**
     * Initializes the <code>reservedWords</code> field.
     * @param res Comma-separated list of reserved words.
     */
    private void initReservedWords(String res) {
        StringTokenizer st = new StringTokenizer(res, ",");
        while (st.hasMoreTokens()) {
            reservedWords.add(st.nextToken().trim());
        }
    }

    //
    // These methods each set a value in one of our maps.
    //
    
    /**
     * Sets a SQL formatting property in this MappingPolicy.
     * @param name Name of the policy property, including its indicator.
     * @param indicator The indicator, alone, which should start with
     * "sql-format".
     * @param value Value to be bound to that property.
     */
    private void setSqlFormatEntry(String name, String value) {
        if (value != null) {
            if (name.startsWith(STATEMENT_SEPARATOR_INDICATOR)) {
                statementSeparator = value;

            } else if (name.startsWith(CREATE_TABLE_START_INDICATOR)) {
                createTableStart = value;

            } else if (name.startsWith(CREATE_TABLE_END_INDICATOR)) {
                createTableEnd = value;

            } else if (name.startsWith(CREATE_INDEX_INDICATOR)) {
                createIndex = value;

            } else if (name.startsWith(DROP_TABLE_INDICATOR)) {
                dropTable = value;

            } else if (name.startsWith(ALTER_TABLE_ADD_CONSTRAINT_START_INDICATOR)) {
                alterTableAddConstraintStart = value;

            } else if (name.startsWith(ALTER_TABLE_DROP_CONSTRAINT_INDICATOR)) {
                alterTableDropConstraint = value;

            } else if (name.startsWith(PRIMARY_KEY_CONSTRAINT_INDICATOR)) {
                primaryKeyConstraint = value;

            } else if (name.startsWith(FOREIGN_KEY_CONSTRAINT_INDICATOR)) {
                foreignKeyConstraint = value;
                
            } else if (name.startsWith(COLUMN_NULLABILITY_INDICATOR)) {
                columnNullability = value;
                
            } else if (name.startsWith(LOB_LOGGING_INDICATOR)) {
                lobLogging = value;
            }
        }
    }

    /**
     * Sets a JDBC property in this MappingPolicy.
     * @param Map into which property is set.
     * @param name Name of the policy property, including its indicator.
     * @param indicator The indicator, alone, which should start with
     * "jdbc-".
     * @param value Value to be bound to that property.
     */
    private void setJDBCInfoEntry(
            Map jdbcInfoMap, String name, String value, String indicator) {

        if (value != null) {

            // Get substring that is before the indicator, which is the name
            // of a type or of a particular field.
            String fieldOrType = name;
            int i = name.indexOf(DOT + indicator);
            if (i > 0) {
                fieldOrType = name.substring(0, i);
            }

            JDBCInfo ji = (JDBCInfo) jdbcInfoMap.get(fieldOrType);

            try {
                if (null != ji) {
                    ji.setValue(value, indicator);
                } else {
                    ji = new JDBCInfo();
                    ji.setValue(value, indicator);
                    jdbcInfoMap.put(fieldOrType, ji);
                }
            } catch (JDBCInfo.IllegalJDBCTypeException ex) {
                String msg = I18NHelper.getMessage(
                        messages,
                        "EXC_InvalidJDBCTypeName", // NOI18N
                        value, fieldOrType);
                logger.log(Logger.SEVERE, msg);
                throw new IllegalArgumentException(msg);
            }
        }
    }

    /**
     * Sets a name length attribute.
     * @param name Name of the attribute to set.  Should be
     * INDICATOR_TABLE_NAME, INDICATOR_COLUMN_NAME, or
     * INDICATOR_CONSTRAINT_NAME.
     * @param value Value to which attribute is set.
     */
    private void setLengthEntry(String name, String value) {
        if (value != null) {

            int val = Integer.parseInt(value);

            if (name.startsWith(INDICATOR_TABLE_NAME)) {
                tableNameMaxLength = val;

            } else if (name.startsWith(INDICATOR_COLUMN_NAME)) {
                columnNameMaxLength = val;

            } else if (name.startsWith(INDICATOR_CONSTRAINT_NAME)) {
                constraintNameMaxLength = val;
            }
        }
    }

    /**
     * Set a naming property in this MappingPolicy.
     * @param name Name of the policy property.
     * @param value Value to be bound to that property.
     */
    private void setNamingEntry(String name, String value) {
        namingPolicy.put(name, value);
    }

    /**
     * Sets a JDBC-to-SQL mapping property, that is, a mapping from a JDBC
     * type to a SQL type.
     * @param name Name of a JDBC type (see {@link java.sql.Types}).
     * @param value SQL type to be used to represent given JDBC type in
     * database.
     */
    private void setSQLInfoEntry(String name, String value) {
        sqlInfo.put(getJdbcType(name), value);
    }

    /**
     * @param databaseType Name of a type of database.
     * @return Name of a file containing a description of properties for
     * named database type.
     */
    private static String getPropertyFileName(String databaseType) {
        return PROPERTY_FILE_DIR + databaseType + PROPERTY_FILE_EXT;
    }

    /**
     * Debug support.
     * @return A description of this MappingPolicy in string form.
     * Basically, all it's "interesting" values.
     */
    public String toString() {
        StringBuffer rc = new StringBuffer(
            "statementSeparator=" + statementSeparator // NOI18N
            + "\ncreateTableStart=" + createTableStart // NOI18N
            + "\ncreateTableEnd=" + createTableEnd // NOI18N
            + "\ncreateIndex=" + createIndex // NOI18N
            + "\ndropTable=" + dropTable // NOI18N
            + "\nalterTableAddConstraintStart=" + alterTableAddConstraintStart // NOI18N
            + "\nalterTableDropConstraint=" + alterTableDropConstraint // NOI18N
            + "\nprimaryKeyConstraint=" + primaryKeyConstraint // NOI18N
            + "\nforeignKeyConstraint=" + foreignKeyConstraint // NOI18N
            + "\ncolumnNullability=" + columnNullability // NOI18N
            + "\nlobLogging=" + lobLogging // NOI18N
            + "\ntableNameMaxLength=" + tableNameMaxLength // NOI18N
            + "\ncolumnNameMaxLength=" + columnNameMaxLength // NOI18N
            + "\nconstraintNameMaxLength=" + constraintNameMaxLength // NOI18N
            + "\nuniqueTableName=" + uniqueTableName // NOI18N
            + "\ncounter=" + counter // NOI18N
            + "\n\n"); // NOI18N
        rc.append("    dbJdbcInfoMap:\n").append(stringifyMap(dbJdbcInfoMap)); // NOI18N
        rc.append("    userJdbcInfoMap:\n").append(stringifyMap(userJdbcInfoMap)); // NOI18N
        rc.append("    sqlInfo:\n").append(stringifyMap(sqlInfo)); // NOI18N
        rc.append("    namingPolicy:\n").append(stringifyMap(namingPolicy)); // NOI18N
        rc.append("    namespaces:\n").append(stringifyMap(namespaces)); // NOI18N
        rc.append("    reservedWords:\n").append(stringifySet(reservedWords)); // NOI18N
        return rc.toString();
    }

    /**
     * Debug support.
     * @param m Map whose keys & values are to be returned in a string.
     * @return The string form of m's keys and values, each pair separated
     * from the next by a newline, with keys separated from values by '='.
     */
    private String stringifyMap(Map m) {
        StringBuffer rc = new StringBuffer();
        for (Iterator i = m.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            rc.append(e.getKey()).append("=") // NOI18N
                .append(e.getValue()).append("\n"); // NOI18N
        }
        return rc.toString();
    }

    /**
     * Debug support
     * @param s Set whose values are to be returned in a string
     * @return values from the given set, separated by spaces, up to 6 per
     * line.
     */
    private String stringifySet(Set s) {
        StringBuffer rc = new StringBuffer();
        int count = 0;
        for (Iterator i = s.iterator(); i.hasNext();) {
            rc.append(i.next()).append(" "); // NOI18N
            if (count++ > 6) {
                rc.append("\n"); // NOI18N
                count = 0;
            }
        }
        return rc.toString();
    }
}
            
