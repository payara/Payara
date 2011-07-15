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
 * DBElementFactory.java
 *
 * Created on Jan 14, 2003
 */


package com.sun.jdo.spi.persistence.generator.database;

import org.netbeans.modules.dbschema.*;
import org.netbeans.modules.dbschema.jdbcimpl.*;
import org.netbeans.modules.dbschema.util.NameUtil;

/*
 * This class assists in creating a database model (dbmodel)
 * element-by-element.
 */
class DBElementFactory {
    /**
     * String which indicates that schema was generated.
     */
    private final static String TAGLINE =
        "generated schema version "; //NOI18N

    /**
     * Signature which identifies version of database generator.  Updated
     * each time the file is checked in to CVS.
     */
    private static final String SIGNATURE =
        "$RCSfile: DBElementFactory.java,v $ $Revision: 1.3 $"; //NOI18N

    /** Field type used if null is given in getColumnType. */
    private static final String UNKNOWN_FIELD_TYPE = "java.lang.Long"; // NOI18N

    /** Field type used for user-defined types in getColumnType. */
    private static final String DEFAULT_FIELD_TYPE = "java.lang.Object"; // NOI18N

    /**
     * Disallow outside construction.
     */
    private DBElementFactory() {
    }

    /** 
     * Creates and returns a schema from give schema name
     * @param schemaName A name for schema.
     * @return Newly created schema element.
     * @throws DBException
     */
    static SchemaElement createSchema(String schemaName) throws DBException {
        SchemaElementImpl schemaImpl = new SchemaElementImpl();
        SchemaElement schema = new SchemaElement(schemaImpl);
        schema.setName(DBIdentifier.create(schemaName));
        schema.setDatabaseProductVersion(TAGLINE + SIGNATURE);
        return schema;
    }

    /**
     * Create table and add to schema.
     * @param schema Schema to which the table gets attached.
     * @param tableName Name of the table without the schema name.
     * @return TableElement for this table name
     * @throws DBException
     */
    static TableElement createAndAttachTable(SchemaElement schema,
            String tableName) throws DBException {

        String fullName = NameUtil.getAbsoluteTableName(
                schema.getName().getName(), tableName);

        TableElementImpl tableImpl = new TableElementImpl(tableName);
        TableElement table = new TableElement(tableImpl, schema);
        table.setName(DBIdentifier.create(fullName));
        table.setTableOrView(true);
        schema.addTable(table);
        return table;
    }

    /**
     * Create column and add to the table.
     * @param columnName Name of the column to create.
     * @param declaringTbl The declaring table to which column gets added.
     * @return ColumnElement that represents the newly-added column.
     * @throws DBException
     */
    static ColumnElement createAndAttachColumn(String columnName, 
            TableElement table, JDBCInfo ji) throws DBException {

        // Create column id 
        String fullName = NameUtil.getAbsoluteMemberName(
                table.getName().getName(), columnName);
        DBIdentifier columnId = DBIdentifier.create(columnName);

        ColumnElementImpl columnImpl = new ColumnElementImpl();
        ColumnElement column = new ColumnElement(columnImpl, table);
        column.setName(columnId);
        column.setType(ji.getJdbcType());
        column.setNullable(ji.getNullable());
        column.setPrecision(ji.getPrecision());
        column.setScale(ji.getScale());
        column.setLength(ji.getLength());

        table.addColumn(column);

        return column;
    }

    /**
     * Create column pair from local column and reference column.
     * @param column The local column.
     * @param refColumn The reference column.
     * @param declaringTbl The declaring table.
     * @return ColumnPairElement that represents the column pair.
     * @throws DBException
     */
    static ColumnPairElement createColumnPair(ColumnElement column,
            ColumnElement refColumn, TableElement declaringTbl)
            throws DBException {

        ColumnPairElementImpl pairImpl = new ColumnPairElementImpl();
        ColumnPairElement pair = new ColumnPairElement(
                pairImpl, column, refColumn, declaringTbl);
        return pair;
    }

    /**
     * Create primary key and add to table.
     * @param table TableElement for adding primary key.
     * @return UniqueKeyElement that represents the primary key.
     * @throws DBException
     */
    static UniqueKeyElement createAndAttachPrimaryKey(TableElement table,
            String pKeyName) throws DBException {

        String tableName = table.getName().getName();
        String fullName = NameUtil.getAbsoluteMemberName(tableName, pKeyName);

        // create index for primary key
        TableElementImpl tableImpl = (TableElementImpl)table.getElementImpl();
        IndexElementImpl indexImpl =
                new IndexElementImpl(tableImpl, fullName, true);
        IndexElement index = new IndexElement(indexImpl, table);
        index.setUnique(true);

        UniqueKeyElementImpl pKeyImpl = new UniqueKeyElementImpl();
        UniqueKeyElement pKey = new UniqueKeyElement(pKeyImpl, table, index);
        pKey.setName(DBIdentifier.create(fullName));
        pKey.setPrimaryKey(true);
        table.addKey(pKey);
        table.addIndex(pKey.getAssociatedIndex());
        return pKey;
    }

    /**
     * Create foreign key between declaring table and reference table with
     * relationship name.
     * @param declaringTbl The declaring table.
     * @param refTbl The referencing table.
     * @param relationName The name for relationship.
     * @param uniqueId Id that can be appened to relName to distinguish it
     * from other relNames in the database.
     * @return The foreign key object.
     * @throws DBException
     */
    static ForeignKeyElement createAndAttachForeignKey(
                TableElement declaringTbl, TableElement refTbl, String keyName,
                MappingPolicy mappingPolicy, String uniqueId) throws DBException {

        String fkeyName = mappingPolicy.getConstraintName(keyName, uniqueId);

        TableElementImpl tableImpl =
                (TableElementImpl) declaringTbl. getElementImpl();
        ForeignKeyElementImpl fkeyImpl =
                new ForeignKeyElementImpl(tableImpl, fkeyName);
        ForeignKeyElement fkey = new ForeignKeyElement(fkeyImpl, declaringTbl);

        UniqueKeyElement pk = refTbl.getPrimaryKey();
        ColumnElement [] pkColumns = pk.getColumns();
        String refTblName = refTbl.getName().getName();

        // Each PK column contributes to the FK.
        if (pkColumns != null) {
            for (int i = 0; i < pkColumns.length; i++) {
                ColumnElement refColumn = pkColumns[i];

                // get name from mappingPolicy
                String columnName =
                        mappingPolicy.getConstraintColumnName(
                                refTblName, refColumn.getName().getName());

                // create column to ref primary key of ref table
                JDBCInfo ji = new JDBCInfo(
                        refColumn.getType(), 
                        refColumn.getPrecision(),
                        refColumn.getScale(), 
                        refColumn.getLength(),
                        true);

                // create column and add to declaring table
                ColumnElement column = createAndAttachColumn(
                        columnName, declaringTbl, ji);

                // create column pairs and add to foreign key
                ColumnPairElement pair = createColumnPair(
                        column, refColumn, declaringTbl);
                fkey.addColumnPair(pair);
            }
        }
        declaringTbl.addKey(fkey);
        return fkey;
    }

    /**
     * Returns properties of a type of a field.
     * @param fieldName Full name of a field, including its package and
     * class.  If null, UNKNOWN_FIELD_TYPE is used.
     * @param fieldType Full name of a type, including its package and
     * class.
     * @param mappingPolicy Policy dictating properties.
     * @return JDBCInfo which indicates properties of a type of field.  If
     * there is information specific to fieldName, that is returned, else
     * information for fieldType is returned.
     */
    static JDBCInfo getColumnType(String fieldName, String fieldType,
            MappingPolicy mappingPolicy) {

        // fieldType will be null when we are handling an unknown PK
        // situation.  Use a Long in that case.
        if (fieldType == null) {
            fieldType = UNKNOWN_FIELD_TYPE;
        }

        JDBCInfo rc = mappingPolicy.getJDBCInfo(fieldName, fieldType);

        // We won't find a JDBCInfo for user-defined types.  Treat them as
        // Object. 
        if (null == rc) {
            // Treat as user-defined object type.
            rc = mappingPolicy.getJDBCInfo(null, DEFAULT_FIELD_TYPE); // NOI18N
        }
        return rc;
    }
}
