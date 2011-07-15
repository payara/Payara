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
 * DDLGenerator.java
 *
 * Created on Jan 14, 2003
 */

package com.sun.jdo.spi.persistence.generator.database;

import java.io.*;
import java.util.*;
import java.sql.*;

import org.netbeans.modules.dbschema.*;

// XXX Instead of only static methods, generateDDL could create an
// instance of this class with the parameters that are currently passed
// around, such as mappingPolicy, being fields.  Other methods then become
// instance methods, and don't take all the parameters.

/** 
 * This class generates DDL for a given SchemaElement.
 *
 * @author Jie Leng, Dave Bristor
 */    
public class DDLGenerator {
 
    /** For writing DDL. */
    private static final char SPACE = ' '; //NOI18N

    /** For writing DDL. */
    private static final char START = '('; //NOI18N

    /** For writing DDL. */
    private static final char END = ')'; //NOI18N

    /** For writing DDL. */
    private static final String COLUMN_SEPARATOR = ", "; //NOI18N

    /** For writing DDL. */
    private static final String NOT_NULL_STRING = "NOT NULL"; //NOI18N

    /** For writing DDL. */
    private static final String NULL_STRING = "NULL"; //NOI18N


    // XXX The use of streams can be improved.  They probably should be
    // writers anyway, and the names should be consistent.
    
    /**
     * Generate DDL from schema and database vendor name.  Up to four files
     * containing DDL are created:
     * <ul>
     * <li>create table DDL destined for human readers</li>
     * <li>create table DDL destined for a JDBC connection</li>
     * <li>drop table DDL destined for human readers</li>
     * <li>drop table DDL destined for a JDBC connection</li>
     * </ul>
     * Furthermore, if the datbase output stream is not null, then create
     * tables in database.
     * @param schema Database schema for which DDL is generated.
     * @param dbVendorName Name of database vendor, which must match one of
     * the names of the .properties files in this package.
     * @param createDDLSql An OutputStream to which human-readable DDL for
     * creating tables gets written.
     * @param dropDDLSql An OutputStream to which human-readable DDL for
     * dropping tables gets written.
     * @param dropDDLJdbc An OutputStream to which DDL for dropping tables
     * gets written.  This parameter differs from dropDDLSql because the data
     * written into it is supposed to be used with a JDBC connection.
     * @param createDDLJdbc An OutputStream to which DDL for creating tables
     * gets written.  This parameter differs from createDDLSql because the data
     * written into it is supposed to be used with a JDBC connection.
     * @param dbStream DatabaseOutputStream which, if not null, is used to
     * create tables in a live database during this method call.
     * @param dropAndCreateTbl If true, and dbStream is not null, then
     * existing tables are dropped before new ones are created.
     * @throws DBException
     * @throws SQLException
     * @throws IOException
     */
    // XXX Change param dbStream to be of type DatabaseOutputStream.
    // XXX Change names of all streams to indicate that they are streams.
    // XXX Consider changing this API to take the location of where the files
    // are to be written, and to create the streams here.
    // XXX Reorder params so they are in order create, drop, create, drop or
    // maybe create, create, drop, drop, but not create, drop, drop, create!
    public static void generateDDL(SchemaElement schema, String dbVendorName, 
            OutputStream createDDLSql, OutputStream dropDDLSql,
            OutputStream dropDDLJdbc, OutputStream createDDLJdbc,
            OutputStream dbStream, boolean dropAndCreateTbl) 
            throws DBException, SQLException, IOException {

        if (schema != null) {
            MappingPolicy mappingPolicy = MappingPolicy.getMappingPolicy(
                    dbVendorName);
            DDLTemplateFormatter.reset(mappingPolicy);
            String schemaName = schema.getName().getName();
            List createAllTblDDL = new ArrayList();
            // Added for Symfoware support as Symfoware does not automatically create
            // indexes for primary keys. Creating indexes is mandatory.
            List createIndexDDL = new ArrayList();
            List alterAddConstraintsDDL = new ArrayList();
            List alterDropConstraintsDDL = new ArrayList();
            List dropAllTblDDL = new ArrayList();
            TableElement[] tables = schema.getTables();
            
            if (tables != null) { 
                for (int ii = 0; ii < tables.length; ii++) {
                    TableElement table = tables[ii];

                    createAllTblDDL.add(
                            createCreateTableDDL(table, mappingPolicy));
		    // Added for Symfoware support as indexes on primary keys are mandatory
                    if (table.getPrimaryKey() != null) {
                        createIndexDDL.add(createIndexDDL(table));
                    }
                    alterAddConstraintsDDL.addAll(
                            createAddConstraintsDDL(table));
                    alterDropConstraintsDDL.addAll(
                            createDropConstraintsDDL(table));
                    dropAllTblDDL.add(
                        createDropTableDDL(table));
                }
            }
            String stmtSeparator = mappingPolicy.getStatementSeparator();
            generateSQL(createDDLSql, dropDDLSql, dropDDLJdbc, createDDLJdbc, 
                (DatabaseOutputStream) dbStream, createAllTblDDL, createIndexDDL,
                alterAddConstraintsDDL, alterDropConstraintsDDL, dropAllTblDDL,
                stmtSeparator, dropAndCreateTbl);
        }
    }

    /**
     * Write DDL to files or drop or create table in database 
     * @param createDDLSql a file for writing create DDL
     * @param dropDDLSql a file for writing drop DDL
     * @param dropDDLTxt a file for writing drop DDL and can be easily
     * executes drop in undeployment time
     * @param dbStream for creating table in database
     * @param createAllTblDDL a list of create table statement
     * @param createIndexDDL a list of create index statement
     * @param alterAddConstraintsDDL a list of adding constraints statement
     * @param alterDropConstraintDDL a list of droping constrains statement
     * @param dropAllTblDDL a list of droping tables statement
     * @param stmtSeparator for separating each statement 
     * @param dropAndCreateTbl true for dropping tables first
     * @throws DBException
     * @throws SQLException
     */
    // XXX FIXME The above javadoc is wrong, change it if/when the
    // generateDDL api changes.
    // XXX Fix method body comments.
    private static void generateSQL(OutputStream createSql,
            OutputStream dropSql, OutputStream dropTxt, OutputStream createTxt,
            DatabaseOutputStream dbStream, List createAllTblDDL, List createIndexDDL,
            List alterAddConstraintsDDL, List alterDropConstraintsDDL,
            List dropAllTblDDL, String stmtSeparator, boolean dropAndCreateTbl)
            throws DBException, SQLException {

        PrintStream workStream = null;
        PrintStream txtStream = null;

        try {
            // drop constraints first 
            workStream = new PrintStream(dropSql);
            if (dropTxt != null) {
                txtStream = new PrintStream(dropTxt);
            }
            for (int i = 0; i < alterDropConstraintsDDL.size(); i++) {
                String dropConstStmt = (String) alterDropConstraintsDDL.get(i);
                writeDDL(workStream, txtStream, stmtSeparator, dropConstStmt);
                if ((dbStream != null) && dropAndCreateTbl) {
                    dbStream.write(dropConstStmt);
                }
            }

            // drop tables
            for (int i = 0; i < dropAllTblDDL.size(); i++) {
                String dropTblStmt = (String) dropAllTblDDL.get(i);
                writeDDL(workStream, txtStream, stmtSeparator, dropTblStmt);
                if ((dbStream != null) && dropAndCreateTbl) {
                    dbStream.write(dropTblStmt);
                }
            }
            workStream.close();
            if (txtStream != null) {
                txtStream.close();
            }

            // create tables and constraints
            workStream = new PrintStream(createSql);
            if (createTxt != null) {
                txtStream = new PrintStream(createTxt);
            }

            // First create tables...
            for (int i = 0; i < createAllTblDDL.size(); i++) {
                StringBuffer createStmt = new StringBuffer();
                String[] createTbl = (String []) createAllTblDDL.get(i);

                for (int j = 0; j < createTbl.length; j++) {
                    workStream.println(createTbl[j]);
                    createStmt.append(createTbl[j]);
                }
                workStream.println(stmtSeparator);
                if (txtStream != null) {
                    txtStream.println(createStmt);
                }
                if (dbStream != null) {
                   dbStream.write(createStmt.toString());
                }
            }

            // ...then create indexes
            for (int i = 0; i < createIndexDDL.size(); i++) {
                String stmt = (String)createIndexDDL.get(i);
                writeDDL(workStream, txtStream, stmtSeparator, stmt);
                if (dbStream != null) {
                    dbStream.write(stmt);
                }
            }

            // ...then create constraints
            for (int i = 0; i < alterAddConstraintsDDL.size(); i++) {
                String stmt = (String)alterAddConstraintsDDL.get(i);
                writeDDL(workStream, txtStream, stmtSeparator, stmt);
                if (dbStream != null) {
                    dbStream.write(stmt);
                }
            }
            
            workStream.close();
            if (txtStream != null) {
                txtStream.close();
            }

            if (dbStream != null) {
                dbStream.flush();
            }
        } finally {
            if (workStream != null) {
                workStream.close();
            }
            if (txtStream != null) {
                txtStream.close();
            }
        }
    }

    /**
     * Writes string to given streams.
     * @param sqlFile Stream representing human-readable DDL.
     * @param txtFile Stream representing DDL to be used via JDBC
     * statements.
     * @param stmtSeparator Separator between DDL statements in
     * human-readable DDL.
     */
    private static void writeDDL(PrintStream sql, PrintStream txt, 
            String stmtSeparator, String stmt) {

        if (stmt == null || stmt.trim().length() == 0) {
            return;
        }

        sql.println(stmt);
        sql.println(stmtSeparator);

        // Could be null, if null was passed to generateDDL.
        if (txt != null) {
            txt.println(stmt);
        }
    } 

    // XXX Some methods below use oneParam, etc.  These should be renamed
    // "formatterParam" or somesuch, leaving out their number.

    /**
     * Returns DDL in String[] form to create a table.  The returned String[]
     * elements have the format:
     * <pre>
     * CREATE TABLE table_name (
     *     column_name type(precision, scale) null,
     *     ...
     *     CONSTRAINT pk_name PRIMARY KEY (id, name)
     * )
     * </pre>
     * @param table Table for which DDL is to be created.
     * @param mappingPolicy Policy that determines database- and
     * user-supplied properties on generating DDL.
     * @return String[] containing DDL to create a table.
     */
    private static String[] createCreateTableDDL(TableElement table, 
            MappingPolicy mappingPolicy) {

        List createTblList = new ArrayList();
        String[] oneParam = { table.getName().getName() };

        createTblList.add(
                DDLTemplateFormatter.formatCreateTable(oneParam));
        
	// add columns for each table
        ColumnElement[] columns = table.getColumns();
        String constraint = createPrimaryKeyConstraint(table);
        int size = columns.length;
        
	for (int i = 0; i < size; i++) {
            StringBuffer columnContent = new StringBuffer();
            columnContent.append(getColumnDef(columns[i], mappingPolicy));

            // If we haven't added the last column, or we have but there's a
            // constraint yet to be added, add a column separator.
            if ((i < size - 1) || ((i == size - 1) && (constraint != null))) {
	        columnContent.append(COLUMN_SEPARATOR);
            }
            createTblList.add(columnContent.toString());
	}

        if (constraint != null) {
            createTblList.add(constraint);
        }
        createTblList.add(mappingPolicy.getCreateTableEnd());

        return (String[]) createTblList.toArray(
                new String[createTblList.size()]);
    }

    /**
     * createIndexDDL has been added for Symfoware support. Returns DDL in String form 
     * to create index.  The returned string has the format:
     * <pre>
     * CREATE INDEX table_name.table_name KEY(id, name)
     * </pre>
     * @param table Table for which DDL is to be created.
     * @return DDL to create index.
     */
    private static String createIndexDDL(TableElement table) {
        String[] twoParam = { table.getName().getName() , getColumnNames(table.getPrimaryKey().getColumns()) };
        return DDLTemplateFormatter.formatCreateIndex(twoParam);
    }

    /**
     * Returns DDL in String form to drop a table.  The returned string has
     * the format:
     * <pre>
     * DROP TABLE table_name
     * </pre>
     * @param table Table for which dropping DDL is returned.
     * @return DDL to drop a table.
     */
    private static String createDropTableDDL(TableElement table) {
        String[] oneParam = { table.getName().getName() };
        return DDLTemplateFormatter.formatDropTable(oneParam);
    }

    /**
     * Returns DDL in String form to create a primary key constraint.  The
     * string has the format:
     * <pre>
     * CONSTRAINT pk_name PRIMARY KEY(id, name)
     * </pre>
     * @param table Table for which constraint DDL is returned.
     * @return DDL to create a PK constraint or null if there is no PK.
     */
    private static String createPrimaryKeyConstraint(TableElement table) {
        String rc = null;
        UniqueKeyElement pk = table.getPrimaryKey();
        
        if (pk != null) {
            String[] twoParams = new String[2];
            twoParams[0] = pk.getName().getName();
            twoParams[1] = getColumnNames(pk.getColumns());
            rc = DDLTemplateFormatter.formatPKConstraint(twoParams);
        }
        return rc;
    }

    /**
     * Returns a List of Strings of "add constraint" DDL commands.  Strings
     * in the array have the format:
     * <pre>
     * ALTER TABLE table_name ADD CONSTRAINT constraint_name FOREIGN KEY
     * (id, ..) REFERENCES table_name (id, ..)
     * </pre>
     * @param table Table for which constraints are returned.
     * @return Array of constraints (as Strings), one for each constraint on
     * the table.
     */
    private static List createAddConstraintsDDL(TableElement table) {
        List alterTblDDL = new ArrayList();
        String[] fourParams = new String[4];
        String[] oneParam = { table.getName().getName() };

        // Add foreign keys
        ForeignKeyElement[] fkeys = table.getForeignKeys();
        if (fkeys != null) {
            String alterTblString =
                DDLTemplateFormatter.formatAlterTableAddConstraint(oneParam);
            
            for (int jj=0; jj < fkeys.length; jj++) {
                ForeignKeyElement fkey = fkeys[jj];
                fourParams[0] = fkey.getName().getName();
                fourParams[1] = getColumnNames(fkey.getLocalColumns());
                fourParams[2] = fkey.getReferencedTable().getName().getName();
                fourParams[3] = getColumnNames(fkey.getReferencedColumns());

                StringBuffer alterTblDDLString = new StringBuffer(
                    alterTblString);
                alterTblDDLString.append(SPACE);
                alterTblDDLString.append(
                        DDLTemplateFormatter.formatFKConstraint(fourParams));
                alterTblDDL.add(alterTblDDLString.toString());
            }
        }
        return alterTblDDL;
    }

    /** Returns a List of Strings of "drop constraint" DDL commands.  Each
     *  String has the format:
     * <pre>
     * DROP TABLE table_name
     * </pre>
     * @param table Table for which constraints are to be dropped.
     * @return List of Strings, one per "drop constraint" command.
     */
    private static List createDropConstraintsDDL(TableElement table) {
        List alterTbls = new ArrayList();
        String[] twoParams = new String[2];
        twoParams[0] = table.getName().getName();
        ForeignKeyElement[] fkeys = table.getForeignKeys();

        if (fkeys != null) {
            for (int i = 0; i < fkeys.length; i++) {
                twoParams[1] = fkeys[i].getName().getName();
                String alterTblString =
                    DDLTemplateFormatter.formatAlterTableDropConstraint(
                            twoParams);
                alterTbls.add(alterTblString);
            }
        }
        return alterTbls;
    }

    /**
     * Returns a String which indicates how a column should be declared in
     * DDL.  It has the format:
     * <pre>
     * column_name column_type(length, precision, scale) null
     * </pre>
     * length, precision, scale, and null are optional, and depend on
     * properties of the given column.
     * @param column Column to be declared in DDL.
     * @param mappingPolicy Policy that determines database- and
     * user-supplied properties on generating DDL.
     * @return String containing DDL to create column in database.
     */
    private static String getColumnDef(ColumnElement column,
            MappingPolicy mappingPolicy) {

        int jdbcType = column.getType();
        Integer scale = column.getScale();
        Integer precision = column.getPrecision();
        Integer length = column.getLength();
        String sqlType = mappingPolicy.getSQLTypeName(jdbcType);
        StringBuffer columnContent = new StringBuffer();
        
        columnContent.append(column.getName().getName());
        columnContent.append(SPACE);
        columnContent.append(sqlType);
        if (column.isCharacterType() && length != null) {
            columnContent.append(START);
            columnContent.append(length.toString());
            columnContent.append(END);

        } else if (column.isNumericType() && precision != null) {
            columnContent.append(START);
            columnContent.append(precision.toString());
            if (scale != null) {
                columnContent.append(COLUMN_SEPARATOR);
                columnContent.append(scale.toString());
            }
            columnContent.append(END);

        } else if (column.isBlobType() && length != null) {
            columnContent.append(START);
            columnContent.append(length.toString());
            columnContent.append(END);
        }
            
        // Add extra information required by LOB columns.
        if (jdbcType == Types.BLOB || jdbcType == Types.CLOB) {
            String lobText = mappingPolicy.getLobLogging();
            if (lobText != null && lobText.length() > 0) {
                columnContent.append(SPACE).append(lobText);
            }
        }

        // Add information about nullability.
        if (column.isNullable()) {
            String nullText =  mappingPolicy.getColumnNullability();
            if (nullText != null && nullText.length() > 0) {
                columnContent.append(SPACE).append(nullText);
            }
        } else {
            columnContent.append(SPACE).append(NOT_NULL_STRING);
        }

        return columnContent.toString();
    }

    /**
     * Returns a comma-separated list of column names.
     * @param columns Array of ColumnElements
     * @return String of form "columnA, columnB", with as many named columns
     * as there are in given array.
     */
    private static String getColumnNames(ColumnElement[] columns) {
        StringBuffer columnNames = new StringBuffer();
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) {
                columnNames.append(COLUMN_SEPARATOR);
            }
            columnNames.append(columns[i].getName().getName());
        }
        return columnNames.toString();
    }
}
