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
 * DatabaseGenerator.java
 *
 * Created on Jan 14, 2003
 */

package com.sun.jdo.spi.persistence.generator.database;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.ResourceBundle;

import org.netbeans.modules.dbschema.*;
import org.netbeans.modules.dbschema.util.NameUtil;
import com.sun.jdo.api.persistence.model.mapping.*;
import com.sun.jdo.api.persistence.model.mapping.impl.*;
import com.sun.jdo.api.persistence.model.jdo.*;
import com.sun.jdo.api.persistence.model.jdo.impl.*;
import com.sun.jdo.api.persistence.model.Model;
import com.sun.jdo.api.persistence.model.ModelException;
import com.sun.jdo.spi.persistence.utility.JavaTypeHelper;

import org.glassfish.persistence.common.I18NHelper;
import com.sun.jdo.spi.persistence.utility.logging.Logger;


/**
 * This class generates a database schema and a Map of mapping classes from a
 * set of JDO classes.
 */
public class DatabaseGenerator {
    /** @see DatabaseGenerationConstants#DOT */
    private static final char DOT = DatabaseGenerationConstants.DOT;

    /** Holds Java type information */
    private final Model model;

    /** Provides information on how database info should be generated */
    private final MappingPolicy mappingPolicy;

    /** List of NameTuple objects which holds persistence class name,
     * desired table name and hash class name for database generation. */
    private final List pcClasses;

    /** Map from persistence-capable class names to MappingClassElement's. */
    // See also DatabaseGenerator.Results.mappingClasses.
    private final Map mappingClasses = new HashMap();
    
    /** Generated database schema. */
    private final SchemaElement schema;

    /**
     * Used to recognize and remove classname suffixes.  See {@link
     * #getShortClassName}.
     */
    private final String classSuffix;

    /** The logger */
    private static final Logger logger =
        LogHelperDatabaseGenerator.getLogger();
    
    /** I18N message handler */
    private static final ResourceBundle messages =
        I18NHelper.loadBundle(DatabaseGenerator.class);


    /**
     * Contains the results of invoking DatabaseGenerator.generate()
     */
    public class Results {
        /** Generated database schema. */
        private final SchemaElement schema;

        /** Set of generated MappingClassElement's. */
        // Notice that this is a Set, while the outer class has a Map.  This
        // is intentional: DatabaseGenerator.addRelationships needs to get a
        // MappingClassElement for a pc class name, but the clients of the
        // DatabaseGenerator should only need the mapping classes.
        private final Set mappingClasses;

        Results(SchemaElement schema, Map mappingClasses) {
            this.schema = schema;
            this.mappingClasses = new HashSet(mappingClasses.values());
        }

        /** @return Generated SchemaElement. */
        public SchemaElement getSchema() {
            return schema;
        }

        /** @return Generated mapping classes. */
        public Set getMappingClasses() {
            return mappingClasses;
        }
    }
    

    /**
     * This class holds three strings which contain three type of information.
     * For database generation, for each persistence class, we need to have 
     * persistence class name to look up persistence class, desired table name
     * for table name and hash class name for unique table name. Depending on 
     * the caller's option, hash class name can be same as persistence class 
     * name or it can be different from persistence class name.
     */
    public static class NameTuple {

        /** persistence class name */
        private final String persistenceClassName;

        /** desired table name */
        private final String desiredTableName;

        /** hash class name */
        private final String hashClassName;

        /** 
         * An object holds three string objects.
         * @param persistenceClassName persistence class name for 
         * persistence class look up 
         * @param desiredTableName it can be used for table name
         * @param hashClassName it can be used for unique table name
         */
        public NameTuple(
            String persistenceClassName, String desiredTableName, 
                    String hashClassName) {

            this.persistenceClassName = persistenceClassName;
            this.desiredTableName = desiredTableName;
            this.hashClassName = (hashClassName != null) ? 
                hashClassName : persistenceClassName;
        }

        /** 
         * An object holds three string objects.
         * @param persistenceClassName persistence class name
         * @param desiredTableName name for creating table name
         */
        public NameTuple(String persistenceClassName, String desiredTableName) {
            this(persistenceClassName, desiredTableName, null);
        }

        /** @return persistence class name. */
        public String getPersistenceClassName() {
            return persistenceClassName;
        }

        /** @return hash class name. */
        public String getHashClassName() {
            return hashClassName;
        }

        /** @return desired table name. */
        public String getDesiredTableName() {
            return desiredTableName;
        }
    }

    /**
     * Generate database schema and mapping model from given map of
     * persistence class name.
     * @param model Holds java type information.
     * @param pcClasses A list of NameTuple objects containing persistence 
     * class name and table names.
     * @param mappingPolicy Determines how dbvendor and user influence
     * generated schema.
     * @param schemaName Identifies the generated schema.
     * @param classSuffix Class name suffix that should be removed when
     * creating table names from class names.
     */
    private DatabaseGenerator(
            Model model, List pcClasses, MappingPolicy mappingPolicy,
            String schemaName, String classSuffix)
            throws DBException {

        this.model = model;
        this.pcClasses = pcClasses;
        this.mappingPolicy = mappingPolicy;
        this.schema = DBElementFactory.createSchema(schemaName);
        this.classSuffix = classSuffix;
    }

    /**
     * Generate database schema and mapping model from given map of 
     * persistence class names.  The schema is not put into the SchemaElement
     * cache as a result of this generation.  The next call to 
     * SchemaElement.forName will result in the schema being placed in the 
     * cache if it is not already there.  If it is already there, it is the 
     * caller's responsibility to remove the old version if desired using 
     * SchemaElement's removeFromCache method before a SchemaElement.forName 
     * call.  The generated schema is saved in <i>outputDir.schemaName</i>.
     * @param model Holds java type information.
     * @param pcClasses A List of NameTuple objects containing persistence
     * class name and table names.
     * @param mappingPolicy Determines how dbvendor and user influence
     * generated schema.
     * @param schemaName Identifies the generated schema.
     * @param classSuffix Class name suffix that should be removed when
     * creating table names from class names.
     * @param generateMappingClasses if it is true, generate mapping classes
     * also.  <em>Currently MappingClassElement's are <b>always</b>
     * generated, and the parameter's value is ignored.</em>
     * @return A DatabaseGenerator.Results instance which holds the results
     * of generation.
     */
    public static Results generate(
            Model model, List pcClasses, MappingPolicy mappingPolicy,
            String schemaName, String classSuffix,
            boolean generateMappingClasses) 
            throws DBException, IOException, ModelException {

        DatabaseGenerator generator = new DatabaseGenerator(
                model, pcClasses, mappingPolicy,
                schemaName, classSuffix);

        Results rc = generator.generate();

        mappingPolicy.resetCounter();

        return rc;
    }

    /**
     * Generate database schema and mapping classes.  Iterate over all
     * persistence-capable classes, generating a table for each.  Within each
     * persistence-capable class, iterate over all fields and make columns
     * for each.  Then handle relationships separately, see {@link
     * #addRelationships}.
     * @return A DatabaseGenerator.Results instance which holds the results
     * of generation.
     */
    private Results generate() throws DBException, ModelException {
        for (Iterator i = pcClasses.iterator(); i.hasNext();) {
            NameTuple nameTuple = (NameTuple) i.next();
            String pcClassName = nameTuple.getPersistenceClassName();
            String desiredTableName = nameTuple.getDesiredTableName();

            PersistenceClassElement pcClass =
                    model.getPersistenceClass(pcClassName);

            String tableName = mappingPolicy.getTableName(
                    desiredTableName, getShortClassName(nameTuple.getHashClassName()));
            TableElement table = DBElementFactory.createAndAttachTable(
                    schema, tableName);
            UniqueKeyElement pKey = DBElementFactory.createAndAttachPrimaryKey(
                    table,
                    mappingPolicy.getPrimaryKeyConstraintName(
                            table.getName().getName()));
            MappingClassElement mappingClass = createMappingClass(
                    pcClass, table);

            PersistenceFieldElement[] fields = pcClass.getFields();
            if (fields != null) {
                for (int j = 0; j < fields.length; j++) {
                    PersistenceFieldElement field = fields[j];
                    String fieldName = field.getName();
                    if (!(field instanceof RelationshipElement)) {
                        String columnName = mappingPolicy.getColumnName(
                                desiredTableName, fieldName, tableName);
                        String fieldType = model.getFieldType(
                                pcClassName, fieldName);
                        String fullFieldName =
                                new StringBuffer(desiredTableName)
                                .append(DOT).append(fieldName).toString();
                        JDBCInfo columnType =
                            DBElementFactory.getColumnType(
                                    fullFieldName,
                                    fieldType,
                                    mappingPolicy);
                        if (logger.isLoggable(Logger.FINEST)) {
                            logger.fine(
                                    "DBGenerator.generate: " // NOI18N
                                    + tableName + "." + columnName + ": " // NOI18N
                                    + columnType.toString());
                        }
                        ColumnElement column =
                                DBElementFactory.createAndAttachColumn(
                                        columnName, table, columnType);
                        MappingFieldElement mappingField = 
                                createAndAttachMappingField(
                                        fieldName, mappingClass, column);

                        if (field.isKey()) {
                            column.setNullable(false);
                            pKey.addColumn(column);
                            pKey.getAssociatedIndex().addColumn(column);
                            mappingClass.getTable(tableName).addKeyColumn(
                                    column);
                        }
                    }
                }
            }
            mappingClasses.put(pcClassName, mappingClass);
        }
        addRelationships();

        return new Results(schema, mappingClasses);
    }

    /**
     * Get the primary table element for the mapping class.
     * @param mappingClass That which is associated with the table.
     * @return Table that is associated with mapping class.
     * @throws DBException
     */
    private TableElement getPrimaryTable(MappingClassElement mappingClass)
            throws DBException {

        List tables = mappingClass.getTables();

        MappingTableElement tbl = (MappingTableElement) tables.get(0);
        if (tbl != null) {
            DBIdentifier tblName = DBIdentifier.create(tbl.getTable());
            return schema.getTable(tblName);
        } else {
            return null;
        }
    }

    /**
     * Create mapping class and associated table and PC class
     * @param pcClass PC class that mapping class associated
     * @param table table element that mapping class associated
     * @return MappingClassElement associated with table and PC class
     * @throws ModelException
     */
    private MappingClassElement createMappingClass(
            PersistenceClassElement pcClass, TableElement table)
            throws ModelException {

         MappingClassElement mappingClass =
             new MappingClassElementImpl(pcClass);

         mappingClass.setDatabaseRoot(schema);
         mappingClass.addTable(table);
         return mappingClass;
    }

    /**
     * Create mapping field and add to mapping class
     * @param fieldName a String for field name
     * @param mappingClass mapping class object that field belong to
     * @return mapping field object
     * @throws ModelException
     */
    private MappingFieldElement createAndAttachMappingField(
            String fieldName, MappingClassElement mappingClass,
            ColumnElement column) throws ModelException {

        MappingFieldElement mappingField =
                new MappingFieldElementImpl(fieldName, mappingClass);

        mappingClass.addField(mappingField);
        mappingField.addColumn(column);
        if (column.isBlobType()) {
            mappingField.setFetchGroup(MappingFieldElement.GROUP_NONE);
        } else {
            mappingField.setFetchGroup(MappingFieldElement.GROUP_DEFAULT);
        }
        return mappingField;
    }


    /**
     * Create and add mapping relationship with column pairs to mapping class.
     * The column pair for mappingRelationship is same order as the column
     * pair from foreign key. It is used for 1-1 or 1-M relationship
     * It is column pair between local table and foreign table
     * @param relationName relationship name for the declaring mapping class
     * @param mappingClass mapping class that holds the relationship
     * @param foreign key which hold column pair for the relationship
     * @throws ModelException
     */
    private void addMappingRelationship(String relationName,
            MappingClassElement declaringClass, ForeignKeyElement fkey)
            throws ModelException {

        MappingRelationshipElement impl = new MappingRelationshipElementImpl(
                relationName, declaringClass);
        ColumnPairElement [] pairs = fkey.getColumnPairs();

        for (int i = 0; i < pairs.length; i++) {
            ColumnPairElement pair = pairs[i];
            impl.addColumn(pair);
        }
        declaringClass.addField(impl);
    }

    /**
     * Create and add MappingRelationship with associated column pairs
     * for join table. The column pair for mappingRelationship is same
     * order as the column pair from foreign key.
     * It is for column pairs between the join table and the foreign table
     * @param relationName a String for relation name
     * @param mappingClass mapping class that holds the relationship
     * @param fkeyForeign holding column pair information for the relationship
     * @throws ModelException
     */
    private void addAssocMappingRelationship(String relationName,
            MappingClassElement declaringClass, ForeignKeyElement fkeyForeign)
            throws ModelException {

        MappingRelationshipElement impl =
                (MappingRelationshipElement) declaringClass.getField(
                        relationName);

        if (null == impl) {
            impl = new MappingRelationshipElementImpl(
                    relationName, declaringClass);
            declaringClass.addField(impl);
        }

        // Add column pair for join table and foreign table
        ColumnPairElement [] pairs = fkeyForeign.getColumnPairs();
        for (int i = 0; i < pairs.length; i++) {
            ColumnPairElement pair = pairs[i];
            impl.addAssociatedColumn(pair);
        }
    }

    /**
     * Create and add MappingRelationship with local inverse column pair 
     * (for join table) or inverse column pair 
     * (for non join table) for referenced table.
     * It is for column pairs between local table and join table
     * or for column pairs between local table and foreign table (contains 
     * foreign key)
     * The column pair for mappingRelationship is inverse order
     * as the column pair from foreign key. Foreign key is not in the passing
     * mapping class but in the inverse mapping class
     * @param relationName a String for relation name
     * @param mappingClass mapping class that holds the relationship
     * @param fkeyForeign holding column pair information for the relationship
     * @throws ModelException
     * @throws DBException
     */
    private void addInverseMappingRelationship(String relationName,
            MappingClassElement declaringClass, ForeignKeyElement fkey,
            boolean isJoin)
            throws ModelException, DBException {

        MappingRelationshipElement impl =
                (MappingRelationshipElement) declaringClass.getField(
                        relationName);

        // for join table, need to add two MappingRelationshipElement
        if (null == impl) {
            impl = new MappingRelationshipElementImpl(relationName,
                declaringClass);
            declaringClass.addField(impl);
        }

        TableElement declaringTbl = getPrimaryTable(declaringClass);
        ColumnPairElement [] pairs = fkey.getColumnPairs();

        // Column pair get inverted since adding to referenced table
        for (int i = 0; i < pairs.length; i++) {
            ColumnPairElement pair = pairs[i];
            ColumnPairElement inversePair = DBElementFactory.createColumnPair(
                    pair.getReferencedColumn(), pair.getLocalColumn(), 
                    declaringTbl);

            if (isJoin) {
                impl.addLocalColumn(inversePair);
            } else {
                impl.addColumn(inversePair);
            }
        }
    }

    /**
     * Create and add a relationship.
     * @param srcTable Source table of the relationship.
     * @param relTable Related table.
     * @param relName Name of the relationship.
     * @param inverseRelName Name of the inverse relationship.
     * @param mappingClass Mapping information for the source table.
     * @param relMappingClass Mapping information for the related table.
     * @param uniqueId Id that can be appened to relName to distinguish it
     * from other relNames in the database.
     * @param srcIsJoin True if srcTable is a join table
     * @return ForeignKeyElement representing the relationship.
     */
    private ForeignKeyElement createRelationship(TableElement srcTable,
            TableElement relTable, String relName, String inverseRelName,
            MappingClassElement mappingClass,
            MappingClassElement relMappingClass,
            String uniqueId, boolean srcIsJoin) 
            throws DBException, ModelException {

        ForeignKeyElement fKey = DBElementFactory.createAndAttachForeignKey(
                 srcTable, relTable, relName, mappingPolicy, uniqueId);

        if (srcIsJoin) {
            addInverseMappingRelationship(relName, mappingClass,
                    fKey, true);
            addAssocMappingRelationship(inverseRelName, relMappingClass, fKey);
        } else {
            addMappingRelationship(relName, mappingClass, fKey);
            addInverseMappingRelationship(inverseRelName, relMappingClass,
                    fKey, false);
        }
        return fKey;
    }

    /**
     * Generate relationships for schema and mapping model from 
     * mappingClasses which already have all mapping fields populated. 
     * @throws DBException
     * @throws ModelExpception
     */
    private void addRelationships() 
        throws DBException, ModelException {
        if (logger.isLoggable(Logger.FINE)) {
            logger.fine("add relationship"); // NOI18N
        }

        Map relationFKey = new HashMap();

        // This is a list of 1-1 relationships that are deferred for
        // processing until all other relationships are processed.  Deferral
        // allows us to concentrate foreign keys on one side of the
        // relationship.
        List deferredRelationships = new ArrayList();

        for (Iterator i = mappingClasses.values().iterator(); i.hasNext();) {
            MappingClassElement mappingClass = (MappingClassElement) i.next();
            String pcClassName = mappingClass.getName();
            PersistenceClassElement pcClass =
                model.getPersistenceClass(pcClassName);
            validateModel(pcClass, "pcClass", pcClassName); // NOI18N
            TableElement sourceTable = getPrimaryTable(mappingClass);
            validateModel(sourceTable, "sourceTable", pcClassName); // NOI18N

            // Create a string that can keep names unique
            String uniqueId = getShortClassName(pcClassName);
            int want = 8; // Ideally, take this many chars from end
            int end = uniqueId.length();
            int start = want > end ? 0 : end - want;
            uniqueId = uniqueId.substring(start, end);

            RelationshipElement [] rels = pcClass.getRelationships();
            if (rels != null) {
                for (int j = 0; j < rels.length; j++) {

                    // relationship
                    RelationshipElement relation = rels[j];
                    String relationName = relation.getName();
                    int upperBound = relation.getUpperBound();

                    // inverseRelationship
                    String inverseRelName =
                            relation.getInverseRelationshipName();
                    validateModel(inverseRelName,
                                  "inverseRelName", relationName); // NOI18N
                    String relClassName = model.getRelatedClass(relation);
                    validateModel(relClassName,
                                  "relClassName", relationName); // NOI18N

                    // get related MappingClass and PersistenceClass
                    MappingClassElement relMappingClass =
                        (MappingClassElement) mappingClasses.get(relClassName);
                    validateModel(relMappingClass,
                                  "relMappingClass", relClassName); // NOI18N
                    PersistenceClassElement relClass =
                            model.getPersistenceClass(relClassName);
                    validateModel(relClass,
                                  "relClass", relClassName); // NOI18N
                    RelationshipElement inverseRelation =
                            relClass.getRelationship(inverseRelName);
                    validateModel(inverseRelation,
                                  "inverseRelation", inverseRelName); // NOI18N
                    TableElement relTable = getPrimaryTable(relMappingClass);
                    validateModel(relTable,
                                  "relTable", relClassName); // NOI18N
                    int relUpperBound = inverseRelation.getUpperBound();

                    if (logger.isLoggable(Logger.FINE)) {
                        logger.fine(
                                "Before adding relationship:" // NOI18N
                                + getTblInfo("sourceTable", sourceTable, relationName) // NOI18N
                                + getTblInfo("relTable", relTable, inverseRelName)); // NOI18N
                    }

                    // XXX Suggest making each block below a separate method.
                    
                    if ((upperBound > 1) && (relUpperBound > 1)) {
                        // M-N relationship, create new table
                        if (logger.isLoggable(Logger.FINE)) {
                            logger.fine("M-N relationship"); // NOI18N
                        }

                        ForeignKeyElement fKey = getMappedForeignKey(
                                relation, inverseRelation, relationFKey);
                        if (fKey == null) {
                            TableElement joinTable =
                                DBElementFactory.createAndAttachTable(
                                        schema,
                                        mappingPolicy.getJoinTableName(
                                                sourceTable.getName().getName(),
                                                relTable.getName().getName()));
                            fKey = createRelationship(
                                    joinTable, sourceTable, relationName,
                                    inverseRelName, mappingClass,
                                    relMappingClass, uniqueId, true);
                            relationFKey.put(relation, fKey);
                            ForeignKeyElement fKey2 = createRelationship(
                                    joinTable, relTable, inverseRelName,
                                    relationName, relMappingClass,
                                    mappingClass, uniqueId, true);
                            relationFKey.put(inverseRelation, fKey2);
                        }

                    } else if ((upperBound > 1) && (relUpperBound == 1)) {
                        // M-1 relationship, add foreign key at upper bound
                        // equal 1 side.  So here, we do nothing: We add
                        // relationships at the 1 side for 1-M relationships,
                        // and the current mapping class is the many side.

                        if (logger.isLoggable(Logger.FINE)) {
                            logger.fine("M-1 relationship: skip"); // NOI18N
                        }

                    } else if ((upperBound == 1) && (relUpperBound >1)) {
                        // 1-M relationship, add foreign key at upperBound =
                        // 1 side

                        if (logger.isLoggable(Logger.FINE)) {
                            logger.fine("1-M relationship"); // NOI18N
                        }

                        ForeignKeyElement fKey = getMappedForeignKey(relation, 
                            inverseRelation, relationFKey);
                        if (fKey == null) {
                            fKey = createRelationship(sourceTable, relTable, 
                                relationName, inverseRelName, mappingClass, 
                                relMappingClass, uniqueId, false);
                            relationFKey.put(relation, fKey);
                        }

                    } else if ((upperBound == 1) && (relUpperBound == 1)) {
                        // 1-1 relationship, add foreign key at either side.
                        // Check existence of foreign key at the other side
                        // before adding one to here.  If there is cascade
                        // delete in this side, add FK.  Otherwise, defer
                        // adding it until all other relationships are added.

                        ForeignKeyElement fKey = getMappedForeignKey(relation, 
                                inverseRelation, relationFKey);
                        if (fKey == null) {
                            if (relation.getDeleteAction() == 
                                RelationshipElement.CASCADE_ACTION) {
                                if (logger.isLoggable(Logger.FINE)) {
                                    logger.fine("1-1 relationship: cascade(this)"); // NOI18N
                                }
                                fKey = createRelationship(
                                        sourceTable, relTable, relationName,
                                        inverseRelName, mappingClass, 
                                        relMappingClass, uniqueId, false);
                                relationFKey.put(relation, fKey);
                            } else if (inverseRelation.getDeleteAction() == 
                                       RelationshipElement.CASCADE_ACTION) {
                                if (logger.isLoggable(Logger.FINE)) {
                                    logger.fine("1-1 relationship: cascade(inverse)"); // NOI18N
                                }
                                fKey = createRelationship(
                                        relTable, sourceTable, 
                                        inverseRelName, relationName, 
                                        relMappingClass, mappingClass,
                                        uniqueId, false);
                                relationFKey.put(inverseRelation, fKey);
                            } else {
                                if (logger.isLoggable(Logger.FINE)) {
                                    logger.fine("1-1 relationship: defer"); // NOI18N
                                }
                                deferredRelationships.add(
                                        new DeferredRelationship(
                                                relation, inverseRelation,
                                                sourceTable, relTable,
                                                relationName, inverseRelName,
                                                mappingClass, relMappingClass,
                                                uniqueId));
                            }
                        }
                    }
                    if (logger.isLoggable(Logger.FINE)) {
                        logger.fine(
                                "After adding relationship:" // NOI18N
                                + getTblInfo("sourceTable", sourceTable, relationName) // NOI18N
                                + getTblInfo("relTable", relTable, inverseRelName)); // NOI18N
                    }
                }
            }
        }
        
        if (deferredRelationships.size() > 0) {
            addDeferredRelationships(deferredRelationships, relationFKey);
        }
    }

    /**
     * Generate foreign keys for relationships that were deferred; see {@link
     * addRelationships}.
     * @param deferredRelationships List of 1-1 relationships which are not
     * yet mapped by foreign keys
     * @param relationFKey Map from RelationshipElement to ForeignKeyElement,
     * indicating which relationships have already been mapped.
     */
    private void addDeferredRelationships(
            List deferredRelationships, Map relationFKey) 
            throws DBException, ModelException {

        for (Iterator i = deferredRelationships.iterator(); i.hasNext();) {
            DeferredRelationship dr = (DeferredRelationship)i.next();

            RelationshipElement relation = dr.getRelation();
            RelationshipElement inverseRelation = dr.getInverseRelation();

            ForeignKeyElement fKey =
                getMappedForeignKey(relation, inverseRelation, relationFKey);

            // Only map if not already mapped
            if (fKey == null) {

                TableElement sourceTable = dr.getSourceTable();
                TableElement relTable = dr.getRelTable();

                String relationName = dr.getRelationName();
                String inverseRelName = dr.getInverseRelName();

                MappingClassElement mappingClass = dr.getMappingClass();
                MappingClassElement relMappingClass = dr.getRelMappingClass();

                String uniqueId = dr.getUniqueId();
                
                // If this side already has any foreign keys, map it on this
                // side, else on the other side.
                ForeignKeyElement keys[] = sourceTable.getForeignKeys();
                if (null != keys && keys.length > 0) {
                    fKey = createRelationship(
                            sourceTable, relTable, 
                            relationName, inverseRelName,
                            mappingClass, relMappingClass,
                            uniqueId, false);
                    if (logger.isLoggable(Logger.FINE)) {
                        logger.fine(
                                "1-1 deferred relationship (this)" // NOI18N
                                + getTblInfo("sourceTable", sourceTable, relationName) // NOI18N
                                + getTblInfo("relTable", relTable, inverseRelName)); // NOI18N
                    }
                } else {
                    fKey = createRelationship(
                            relTable, sourceTable,
                            inverseRelName, relationName,
                            relMappingClass, mappingClass,
                            uniqueId, false);
                    if (logger.isLoggable(Logger.FINE)) {
                        logger.fine(
                                "1-1 deferred relationship (inverse)" // NOI18N
                                + getTblInfo("sourceTable", sourceTable, relationName) // NOI18N
                                + getTblInfo("relTable", relTable, inverseRelName)); // NOI18N
                    }
                }
                relationFKey.put(relation, fKey);
            }
        }
    }


    /**
     * A DeferredRelationship instance represents all the information required
     * to create a foreign key from the relationship.
     */
    static class DeferredRelationship {
        private final RelationshipElement relation;
        private final RelationshipElement inverseRelation;
        private final TableElement sourceTable;
        private final TableElement relTable;
        private final String relationName;
        private final String inverseRelName;
        private final MappingClassElement mappingClass;
        private final MappingClassElement relMappingClass;
        private final String uniqueId;
        
        
        DeferredRelationship(RelationshipElement relation,
                             RelationshipElement inverseRelation,
                             TableElement sourceTable,
                             TableElement relTable,
                             String relationName,
                             String inverseRelName,
                             MappingClassElement mappingClass,
                             MappingClassElement relMappingClass,
                             String uniqueId) {

            this.relation = relation;
            this.inverseRelation = inverseRelation;
            this.sourceTable = sourceTable;
            this.relTable = relTable;
            this.relationName = relationName;
            this.inverseRelName = inverseRelName;
            this.mappingClass = mappingClass;
            this.relMappingClass = relMappingClass;
            this.uniqueId = uniqueId;
        }

        RelationshipElement getRelation() { return relation; }
        RelationshipElement getInverseRelation() { return inverseRelation; }

        TableElement getSourceTable() { return sourceTable; }
        TableElement getRelTable() { return relTable; }

        String getRelationName() { return relationName; }
        String getInverseRelName() { return inverseRelName; }

        MappingClassElement getMappingClass() { return mappingClass; }
        MappingClassElement getRelMappingClass() { return relMappingClass; }

        String getUniqueId() { return uniqueId; }
    }


    
    /**
     * Check if the relationship has been visited
     * @param relation current visiting relationship
     * @param inverseRelation inverse relationship 
     * @param relationFKey a map to hold relation and foreign key
     * @return the foreign key element or null 
     */
    private ForeignKeyElement getMappedForeignKey(
            RelationshipElement relation, RelationshipElement inverseRelation,
            Map relationFKey) {

        ForeignKeyElement fkey =
                (ForeignKeyElement) relationFKey.get(relation);

        if (fkey == null) {
            return (ForeignKeyElement) relationFKey.get(inverseRelation);
        } else {
            return fkey;
        }
    }

    /**
     * Computes the class name (without package) for the supplied
     * class name and trims off the class suffix.
     * @param className the fully qualified name of the class
     * @return the class name (without package and class suffix) 
     * for the supplied class name
     */
    private String getShortClassName(String className) {
        String shortName = JavaTypeHelper.getShortClassName(className);

        if ((classSuffix != null) && (!shortName.equals(classSuffix))) {
            int index = shortName.lastIndexOf(classSuffix);
            if (index != -1) {
                shortName = shortName.substring(0, index);
            }
        }
        return shortName;
    }

    /**
     * Assert that the given object reference is <em>not</em> null.
     * @param o Object reference that is checked for null.
     * @param failedItem String that names the item that is being checked.
     * @param accessor String which names an object that was use to try and
      * get object <code>o</code>.
     * @throws ModelException If o is null.
     */
    private void validateModel(Object o,
                               String failedItem,
                               String accessor) throws ModelException {
        if (null == o) {
            String msg = I18NHelper.getMessage(
                    messages,
                    "EXC_InvalidRelationshipMapping",  // NOI18N
                    failedItem,
                    accessor);
            logger.log(Logger.SEVERE, msg);
            throw new ModelException(msg);
        }
    }

    /**
     * Debug support. Returns a string describing the table and it's keys
     * (only the first key is listed).
     * @param tblName name of the table described
     * @param tbl table being described
     * @param relName name of a relationship 
     */
    private static String getTblInfo(String tblName, TableElement tbl, String relName) {
        int numFK = tbl.getForeignKeys().length;
        ForeignKeyElement fk = null;
        if (numFK > 0) {
            fk = tbl.getForeignKeys()[0];
        }
        return " " + tblName + "=" + tbl.toString()
            + ", # keys=" + numFK // NOI18N
            + ", 1st key=" + fk // NOI18N
            + "; relationship Name=" + relName; // NOI18N
    }
}
