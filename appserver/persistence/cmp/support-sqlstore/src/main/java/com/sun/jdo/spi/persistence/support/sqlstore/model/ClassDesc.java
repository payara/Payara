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
 * ClassDesc.java
 *
 * Created on March 3, 2000
 *
 */

package com.sun.jdo.spi.persistence.support.sqlstore.model;

import org.netbeans.modules.dbschema.ColumnElement;
import org.netbeans.modules.dbschema.ColumnPairElement;
import org.netbeans.modules.dbschema.TableElement;
import com.sun.jdo.api.persistence.model.Model;
import com.sun.jdo.api.persistence.model.jdo.ConcurrencyGroupElement;
import com.sun.jdo.api.persistence.model.jdo.PersistenceClassElement;
import com.sun.jdo.api.persistence.model.jdo.PersistenceFieldElement;
import com.sun.jdo.api.persistence.model.jdo.RelationshipElement;
import com.sun.jdo.api.persistence.model.mapping.MappingClassElement;
import com.sun.jdo.api.persistence.model.mapping.MappingFieldElement;
import com.sun.jdo.api.persistence.model.mapping.MappingRelationshipElement;
import com.sun.jdo.api.persistence.model.mapping.impl.*;
import com.sun.jdo.api.persistence.support.*;
import com.sun.jdo.spi.persistence.support.sqlstore.*;
import com.sun.jdo.spi.persistence.support.sqlstore.sql.RetrieveDescImpl;
import com.sun.jdo.spi.persistence.support.sqlstore.sql.UpdateObjectDescImpl;
import com.sun.jdo.spi.persistence.support.sqlstore.sql.concurrency.*;
import com.sun.jdo.spi.persistence.support.sqlstore.sql.generator.UpdateQueryPlan;
import com.sun.jdo.spi.persistence.utility.StringHelper;
import com.sun.jdo.spi.persistence.utility.logging.Logger;
import org.glassfish.persistence.common.I18NHelper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;

/**
 *
 */
public class ClassDesc
        implements com.sun.jdo.spi.persistence.support.sqlstore.PersistenceConfig {

    private ArrayList fetchGroups;

    private int maxHierarchicalGroupID;

    /**
     * Used for batched update check. Set to true if this class has at least one
     * local field that does not belong to DFG
     */
    private boolean hasLocalNonDFGField;

    /** Contains all local and foreign fields. */
    public ArrayList fields;

    /** Contains all hidden fields. */
    public ArrayList hiddenFields;

    /** Contains all relationship fields. */
    public ArrayList foreignFields;

    /** Contains the fields used for version consistency validation. */
    private LocalFieldDesc[] versionFields;

    private ArrayList tables;

    private Class pcClass;

    private Class oidClass;

    public int maxFields;

    public int maxVisibleFields;

    public int maxHiddenFields;

    private Concurrency optimisticConcurrency;

    private Concurrency checkDirtyConcurrency;

    private Concurrency databaseConcurrency;

    private Concurrency explicitConcurrency;

    private MappingClassElementImpl mdConfig;

    private ClassLoader classLoader;

    private Constructor constructor;

    private PersistenceClassElement pcElement;

    private PersistenceFieldElement[] persistentFields;

    private Field keyFields[];
    private String keyFieldNames[];
    private LocalFieldDesc[] keyFieldDescs;

    /** The logger. */
    private static Logger logger = LogHelperSQLStore.getLogger();

    /** RetrieveDescriptor cache for navigation and reloading. */
    private final Map retrieveDescCache = new HashMap();

    /**
     * RetrieveDescriptor cache for navigation queries. This cache
     * holds foreign RetrieveDescriptors constrained by the relationship
     * key values.
     */
    private final Map foreignRetrieveDescCache = new HashMap();

    /** Retrieve descriptor for version consistency verification. */
    private RetrieveDesc retrieveDescForVerification;
    private Object retrieveDescForVerificationSynchObj = new Object();

    /** UpdateQueryPlan cache. */
    private final Map updateQueryPlanCache = new HashMap();

    /** UpdateQueryPlan for insert. */
    private UpdateQueryPlan updateQueryPlanForInsert;
    private Object updateQueryPlanForInsertSynchObj = new Object();

    /** UpdateQueryPlan for delete. */
    private UpdateQueryPlan updateQueryPlanForDelete;
    private Object updateQueryPlanForDeleteSynchObj = new Object();

    /** I18N message handler. */
    private final static ResourceBundle messages = I18NHelper.loadBundle(
            "com.sun.jdo.spi.persistence.support.sqlstore.Bundle", // NOI18N
            ClassDesc.class.getClassLoader());

    /** PC Constructor signature. */
    private static final Class[] sigSM = new Class[]{StateManager.class};

    public ClassDesc(MappingClassElement mdConfig, Class pcClass) {

        this.mdConfig = (MappingClassElementImpl) mdConfig;
        pcElement = this.mdConfig.getPersistenceElement();
        this.pcClass = pcClass;
        classLoader = pcClass.getClassLoader();

        try {
            constructor = pcClass.getConstructor(sigSM);
        } catch (Exception e) {
            // Persistence capable classes must provide this constructor
            throw new JDOFatalUserException(I18NHelper.getMessage(messages,
                    "jdo.persistencemanagerimpl.assertpersistencecapable.error", // NOI18N
                    pcClass.getName()), e);
        }

        fields = new ArrayList();
        foreignFields = new ArrayList();
        tables = new ArrayList();
        fetchGroups = new ArrayList();
    }

    public String toString() {
        return getName();
    }

    /**
     * Creates a new instance of <code>ClassDesc</code> for the given
     * <code>pcClass</code>.
     * @param pcClass The persistence capable class.
     * @return A new instance of ClassDesc.
     */
    static ClassDesc newInstance(Class pcClass) {
        Model model = Model.RUNTIME;
        String className = pcClass.getName();
        ClassLoader classLoader = pcClass.getClassLoader();
        ClassDesc rc = null;

        try {
            MappingClassElement mdConfig =
               model.getMappingClass(className, classLoader);

            // Validate the model information for this class.
            validateModel(model, className, classLoader);

            rc = new ClassDesc(mdConfig, pcClass);

        } catch (JDOException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw new JDOFatalUserException(I18NHelper.getMessage(messages,
                "core.configuration.loadfailed.class", className), e); // NOI18N
        } catch (Exception e) {
            throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                "core.configuration.loadfailed.class", className), e); // NOI18N
        }

        return rc;
    }

    /**
     * Validate the mapping for the given class. If the result
     * collection returned by validate() is not empty, it means
     * it failed the test. After logging all the exceptions,
     * throw a JDOUserException and inform the user to go back
     * to the mapping tool to fix up the mapping file. The
     * JDOUserException contains the validation messages aswell.
     *
     * @param model Runtime model.
     * @param className Persistence capable class' name.
     * @param classLoader Persistence capable class' loader.
     */
    static private void validateModel(Model model,
                                      String className,
                                      ClassLoader classLoader) {
        Collection c = null;

        if (!(c = model.validate(className, classLoader, null)).isEmpty()) {
            Iterator iter = c.iterator();
            StringBuffer validationMsgs = new StringBuffer();

            while (iter.hasNext()) {
                Exception ex = (Exception) iter.next();
                String validationMsg = ex.getLocalizedMessage();

                logger.fine(I18NHelper.getMessage(messages,
                    "core.configuration.validationproblem", // NOI18N
                    className, validationMsg));
                validationMsgs.append(validationMsg).append('\n'); // NOI18N
            }
            throw new JDOFatalUserException(I18NHelper.getMessage(messages,
                "core.configuration.validationfailed", // NOI18N
                className, validationMsgs.toString()));
        }
    }

    public void initialize(ConfigCache cache) {
        boolean debug = logger.isLoggable();
        if (debug) {
            logger.fine("sqlstore.model.classdesc.persistconfiginit", mdConfig); // NOI18N
        }

        loadOidClass();
        initializeFields();
        computeTrackedPrimitiveFields();

        initializeTables();
        initializeJoinTables();
        initializeVersionFields();
        initializeConcurrency();

        initializeKeyFields();
        initializeFetchGroups();

        fixupForeignReferences(cache);
        fixupFieldProperties();
        computeTrackedRelationshipFields();
        cleanupTrackedFields();

        // All fields should be initialized at this point. Now calculate
        // the total number of hidden fields and fields.
        if (hiddenFields != null) {
            maxHiddenFields = hiddenFields.size();
        }
        maxFields = maxVisibleFields + maxHiddenFields;

        if (debug) {
            logger.fine("sqlstore.model.classdesc.persistconfiginit.exit"); // NOI18N
        }
    }

    private void loadOidClass() {

        if (oidClass != null) return;

        String keyClassName = pcElement.getKeyClass();
        String suffix = keyClassName.substring(keyClassName.length() - 4);

        // First check whether the key class name ends with ".oid".
        // If so, we need to convert it '.' to '$' because is it
        // an inner class.
        if (suffix.compareToIgnoreCase(".oid") == 0) { // NOI18N
            StringBuffer buf = new StringBuffer(keyClassName);

            buf.setCharAt(buf.length() - 4, '$');
            keyClassName = buf.toString();
        }

        try {
            oidClass = Class.forName(keyClassName, true, classLoader);
        } catch (Throwable e) {
            throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                    "core.configuration.cantloadclass", keyClassName)); // NOI18N
        }

        if (logger.isLoggable()) {
            logger.fine("sqlstore.model.classdesc.loadedclass", oidClass); // NOI18N
        }
    }

    /**
     * This method maps all the visible fields. It has the side-effect of computing
     * the value for maxVisibleFields.
     */
    private void initializeFields() {
        ArrayList concurrencyGroups = new ArrayList();
        persistentFields = pcElement.getFields();

        for (int i = 0; i < persistentFields.length; i++) {
            PersistenceFieldElement pcf = persistentFields[i];
            MappingFieldElementImpl mdf = (MappingFieldElementImpl) mdConfig.getField(pcf.getName());

            if (mdf == null) {
                throw new JDOFatalUserException(I18NHelper.getMessage(messages,
                        "core.configuration.fieldnotmapped", // NOI18N
                        pcf.getName(), pcElement.getName()));
            }

            FieldDesc f;

            if (!(mdf instanceof MappingRelationshipElement)) {
                f = createLocalField(mdf);
            } else {
                f = createForeignField((RelationshipElement) pcf, (MappingRelationshipElementImpl) mdf);
            }

            initializeFetchAndConcurrencyGroup(f, pcf, mdf, concurrencyGroups);

            if (mdf.isReadOnly()) {
                f.sqlProperties |= FieldDesc.PROP_READ_ONLY;
            }

            try {
                f.setupDesc(pcClass, pcf.getName());
            } catch (JDOException e) {
                throw e;
            } catch (Exception e) {
                throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                "core.configuration.loadfailed.field", // NOI18N
                pcf.getName(), pcElement.getName()), e);
            }

            f.absoluteID = pcf.getFieldNumber();

            addField(f);

            if (logger.isLoggable(Logger.FINEST)) {
                Object[] items = new Object[] {f.getName(),new Integer(f.absoluteID)};
                logger.finest("sqlstore.model.classdesc.fieldinfo", items); // NOI18N
            }
        }

        this.maxVisibleFields = fields.size();
    }

    /**
     * Creates an instance of <code>LocalFieldDesc</code>.

     * @param mdf Input <code>MappingFieldElementImpl</code>
     */
    private LocalFieldDesc createLocalField(MappingFieldElementImpl mdf) {
        ArrayList columnDesc = mdf.getColumnObjects();

        // Make sure this field is properly mapped.
        if ((columnDesc == null) || (columnDesc.size() == 0)) {
            throw new JDOFatalUserException(I18NHelper.getMessage(messages,
                    "core.configuration.fieldnotmapped", // NOI18N
                    mdf.getName(), pcElement.getName()));
        }
        return new LocalFieldDesc(this, columnDesc);
    }

    /**
     * Creates an instance of <code>LocalFieldDesc</code> for <code>column</code>
     * that corresponds to a hidden field. Adds the newly created field to
     * <code>hiddenFields</code> and <code>declaredHiddenFields</code>.
     *
     * @param column The input column.
     * @return New instance of <code>LocalFieldDesc</code>
     */
    private LocalFieldDesc createLocalHiddenField(ColumnElement column) {
        ArrayList columnDesc = new ArrayList();
        columnDesc.add(column);
        LocalFieldDesc lf = new LocalFieldDesc(this, columnDesc);

        if (hiddenFields == null) {
            hiddenFields = new ArrayList();
        }
        hiddenFields.add(lf);

        // AbsouluteID for hidden fields must be < 0.
        lf.absoluteID = -hiddenFields.size();

        if (logger.isLoggable(Logger.FINEST)) {
            Object[] items = new Object[] {pcClass,lf.getName(),column.getName().getFullName()};
            logger.finest("sqlstore.model.classdesc.getlocalfielddesc", items); // NOI18N
        }

        return lf;
    }

    private FieldDesc createForeignField(RelationshipElement fpcf,
                                         MappingRelationshipElementImpl fmdf) {

        ForeignFieldDesc ff = new ForeignFieldDesc(this);

        addForeignField(ff);

        ff.cardinalityLWB = fpcf.getLowerBound();
        ff.cardinalityUPB = fpcf.getUpperBound();
        ff.deleteAction = fpcf.getDeleteAction();

        initializeColumnLists(ff, fmdf);

        // This is a workaround to make sure that that cardinalityUPB is not <= 1
        // if the field is of type collection.
        if (Model.RUNTIME.isCollection(fpcf.getCollectionClass()) &&
                (ff.cardinalityUPB <= 1)) {
            ff.cardinalityUPB = Integer.MAX_VALUE;
        }

        if (ff.cardinalityUPB > 1) {
            try {
                ff.setComponentType(Class.forName(fpcf.getElementClass(), true, classLoader));
            } catch (Throwable e) {
              logger.log(Logger.WARNING, "sqlstore.exception.log", e);
            }
        }

        return ff;
    }

    private void initializeColumnLists(ForeignFieldDesc ff, MappingRelationshipElementImpl fmdf) {
        ArrayList assocPairs = fmdf.getAssociatedColumnObjects();
        ArrayList pairs = fmdf.getColumnObjects();
        ArrayList localColumns = new ArrayList();
        ArrayList foreignColumns = new ArrayList();

        // We need to go through each local column and extract the foreign column.
        if ((assocPairs == null) || (assocPairs.size() == 0)) {
            for (int i = 0; i < pairs.size(); i++) {
                ColumnPairElement fce = (ColumnPairElement) pairs.get(i);
                localColumns.add(fce.getLocalColumn());
                foreignColumns.add(fce.getReferencedColumn());
            }

            ff.localColumns = localColumns;
            ff.foreignColumns = foreignColumns;
        } else {
            ArrayList assocLocalColumns = new ArrayList();
            ArrayList assocForeignColumns = new ArrayList();

            for (int i = 0; i < pairs.size(); i++) {
                ColumnPairElement alc = (ColumnPairElement) pairs.get(i);
                localColumns.add(alc.getLocalColumn());
                assocLocalColumns.add(alc.getReferencedColumn());
            }

            for (int i = 0; i < assocPairs.size(); i++) {
                ColumnPairElement afc = (ColumnPairElement) assocPairs.get(i);
                assocForeignColumns.add(afc.getLocalColumn());
                foreignColumns.add(afc.getReferencedColumn());
            }

            ff.localColumns = localColumns;
            ff.assocLocalColumns = assocLocalColumns;
            ff.assocForeignColumns = assocForeignColumns;
            ff.foreignColumns = foreignColumns;
        }
    }

    private void initializeFetchAndConcurrencyGroup(FieldDesc f,
                                                    PersistenceFieldElement pcf,
                                                    MappingFieldElementImpl mdf,
                                                    ArrayList concurrencyGroups) {
        f.fetchGroup = mdf.getFetchGroup();

        // RESOLVE: For now, we can only handle one concurrency group per field
        // MBO:
        // I can call method getConcurrencyGroups w/o exception using the latest mapping files.
        // Please note the mapping files do not include any concurrency group info,
        // thus getConcurrencyGroups returns an empty array.
        ConcurrencyGroupElement cgroups[] = pcf.getConcurrencyGroups();
        //ConcurrencyGroupElement cgroups[] = null;

        if ((cgroups == null) || (cgroups.length == 0)) {
            if (f.fetchGroup == FieldDesc.GROUP_DEFAULT) {
                f.concurrencyGroup = f.fetchGroup;
            }
        } else {
            ConcurrencyGroupElement cge = cgroups[0];
            int index = 0;

            if ((index = concurrencyGroups.indexOf(cge)) == -1) {
                index = concurrencyGroups.size();
                concurrencyGroups.add(cge);
            }

            f.concurrencyGroup = index;
        }
    }

    private void createSecondaryTableKey(TableDesc table, MappingReferenceKeyElementImpl mappingSecondaryKey) {

        ColumnPairElement pairs[] = mappingSecondaryKey.getColumnPairs();
        KeyDesc referencingKey = new KeyDesc();
        KeyDesc referencedKey = new KeyDesc();
        TableDesc secondaryTable = findTableDesc(((MappingTableElementImpl) mappingSecondaryKey.getTable()).getTableObject());

        for (int i = 0; i < pairs.length; i++) {
            ColumnPairElement pair = pairs[i];

            ColumnElement lc = pair.getLocalColumn();
            ColumnElement fc = pair.getReferencedColumn();

            referencingKey.addColumn(lc);

            FieldDesc lf = getLocalFieldDesc(lc);
            referencingKey.addField(lf);

            // We need to force field for the referencing key to be in the DFG
            // so it will always be loaded. This is to facilitate updating
            // secondary tables that requires this field to be loaded
            // for constraint purposes.
            lf.fetchGroup = FieldDesc.GROUP_DEFAULT;

            referencedKey.addColumn(fc);
            referencedKey.addField(getLocalFieldDesc(fc));
        }

        table.addSecondaryTableKey(new ReferenceKeyDesc(secondaryTable, referencingKey, referencedKey));
        secondaryTable.setPrimaryTableKey(new ReferenceKeyDesc(table, referencedKey, referencingKey));
    }

    /**
     * This method maps all the tables.
     */
    private void initializeTables() {
        ArrayList mdTables = mdConfig.getTables();

        createTables(mdTables);
        processSecondaryTables(mdTables);
    }

    private void createTables(ArrayList mdTables) {
        for (int i = 0; i < mdTables.size(); i++) {
            MappingTableElementImpl mdt = (MappingTableElementImpl) mdTables.get(i);
            TableDesc t = new TableDesc(mdt.getTableObject());

            ArrayList keys = mdt.getKeyObjects();
            KeyDesc key = new KeyDesc();
            t.setKey(key);
            key.addColumns(keys);

            for (int j = 0; j < keys.size(); j++) {
                ColumnElement c = (ColumnElement) keys.get(j);

                if (c != null) {
                    key.addField(getLocalFieldDesc(c));
                }
            }

            addTableDesc(t);
        }
    }

    /**
     * Validity checks on secondary tables:
     *	1) Ensure that every secondary table has a TableDesc
     *	2) Every referencing key is the same length as the table's key
     * Build the referencing keys.
     * NOTE: This method assumes that the entries of <code>mdTables</code>
     * and <code>tables</code> are sorted in the same order.
     */
    private void processSecondaryTables(ArrayList mdTables) {

        for (int i = 0; i < tables.size(); i++) {
            MappingTableElementImpl mdt = (MappingTableElementImpl) mdTables.get(i);
            TableDesc t = (TableDesc) tables.get(i);
            ArrayList secondaryKeys = mdt.getReferencingKeys();

            for (int j = 0; j < secondaryKeys.size(); j++) {
                MappingReferenceKeyElementImpl mappingSecondaryKey = (MappingReferenceKeyElementImpl) secondaryKeys.get(j);
                createSecondaryTableKey(t, mappingSecondaryKey);
            }
        }
    }

    private void initializeJoinTables() {
        Iterator iter = foreignFields.iterator();

        while (iter.hasNext()) {
            ForeignFieldDesc ff = (ForeignFieldDesc) iter.next();

            if (ff.useJoinTable()) {
                TableElement joinTable = ((ColumnElement) ff.assocLocalColumns.get(0)).getDeclaringTable();
                TableDesc joinTableDesc = findTableDesc(joinTable);

                if (joinTableDesc == null) {
                    joinTableDesc = new TableDesc(joinTable);

                    // Mark this table as a join table
                    joinTableDesc.setJoinTable(true);
                    addTableDesc(joinTableDesc);
                }
              }
        }
    }

    /**
     * Returns a list of fields in fetchGroup <code>groupID</code>.
     *
     * @param groupID Fetch group id.
     * @return List of fields in fetchGroup <code>groupID</code>. The list
     *  for <code>FieldDesc.GROUP_NONE</code> is empty.
     * @see #initializeFetchGroups
     */
    public ArrayList getFetchGroup(int groupID) {
        int index = 0;

        if (groupID >= FieldDesc.GROUP_NONE) {
            index = groupID;
        } else if (groupID < FieldDesc.GROUP_NONE) {
            index = -groupID + maxHierarchicalGroupID;
        }

        for (int i = fetchGroups.size(); i <= index; i++) {
            fetchGroups.add(null);
        }

        ArrayList group = (ArrayList) fetchGroups.get(index);

        if (group == null) {
            group = new ArrayList();
            fetchGroups.set(index, group);
        }

        return group;
    }

    private void addField(FieldDesc f) {
        fields.add(f);
    }

    private void addForeignField(ForeignFieldDesc f) {
        foreignFields.add(f);
    }

    /**
     * Compute the fetch group lists for the declared fields.
     * A fetch group lists the fields sharing the same value for
     * <code>FieldDesc.fetchGroup</code>.
     * Note: Fields that aren't in any fetch group must not be added
     * to <code>fetchGroups[FieldDesc.GROUP_NONE]</code>.
     *
     * @see #getFetchGroup
     */
    private void initializeFetchGroups() {

        for (int i = 0; i < 2; i++) {
            ArrayList theFields = null;

            if (i == 0) {
                theFields = fields;
            } else {
                // It is possible to have hidden fields in the DFG.
                if ((theFields = hiddenFields) == null)
                    continue;
            }

            for (int j = 0; j < theFields.size(); j++) {
                FieldDesc f = (FieldDesc) theFields.get(j);

                // Do not add the field to the fetch group for GROUP_NONE.
                if (f.fetchGroup > FieldDesc.GROUP_NONE) {
                    getFetchGroup(f.fetchGroup).add(f);
                }

                // Check declared visible fields only.
                if (i == 0 && !f.isRelationshipField()
                        && f.fetchGroup != FieldDesc.GROUP_DEFAULT) {
                    hasLocalNonDFGField = true;
                }
            }
        }

        this.maxHierarchicalGroupID = fetchGroups.size() - 1;

        for (int i = 0; i < fields.size(); i++) {
            FieldDesc f = (FieldDesc) fields.get(i);

            // Do not add the field to the fetch group for GROUP_NONE.
            if (f.fetchGroup < FieldDesc.GROUP_NONE) {
                getFetchGroup(f.fetchGroup).add(f);
            }
        }
    }

    private void initializeConcurrency() {
        optimisticConcurrency = new ConcurrencyOptVerify();
        optimisticConcurrency.configPersistence(this);
        checkDirtyConcurrency = new ConcurrencyCheckDirty();
        checkDirtyConcurrency.configPersistence(this);
        databaseConcurrency = new ConcurrencyDBNative();
        databaseConcurrency.configPersistence(this);
        explicitConcurrency = new ConcurrencyDBExplicit();
        explicitConcurrency.configPersistence(this);
    }

    private void initializeKeyFields() {
        boolean debug = logger.isLoggable(Logger.FINEST);
        if (oidClass == null)
            return;

        keyFields = oidClass.getFields();
        keyFieldNames = new String[keyFields.length];
        keyFieldDescs = new LocalFieldDesc[keyFields.length];

        if (debug) {
            logger.finest("sqlstore.model.classdesc.createsqldesc", oidClass); // NOI18N
        }

        for (int i = 0; i < keyFields.length; i++) {
            Field kf = keyFields[i];
            String name = kf.getName();
            keyFieldNames[i] = name;

            if (name.equals("serialVersionUID")) { // NOI18N
                continue;
            }

            LocalFieldDesc f = getLocalFieldDesc(name);

            if (f != null) {
                if (debug) {
                    logger.finest("sqlstore.model.classdesc.pkfield", f.getName()); // NOI18N
                }

                // The fetch group for pk fields should always be DFG.
                f.fetchGroup = FieldDesc.GROUP_DEFAULT;
                f.sqlProperties &= ~(FieldDesc.PROP_REF_INTEGRITY_UPDATES);
                f.sqlProperties &= ~(FieldDesc.PROP_IN_CONCURRENCY_CHECK);
                f.sqlProperties |= FieldDesc.PROP_PRIMARY_KEY_FIELD;

                keyFieldDescs[i] = f;
            } else {
                throw new JDOFatalUserException(I18NHelper.getMessage(messages,
                        "core.configuration.noneexistentpkfield", // NOI18N
                        name, oidClass.getName(), pcClass.getName()));
            }
        }
    }

    /**
     * Initialize the list of field descriptors of version consistency fields.
     * The names of the version fields are obtained from
     * {@link MappingClassElement#getVersionFields}.
     */
    private void initializeVersionFields() {
        int size = mdConfig.getVersionFields().size();
        Iterator versionFieldIterator = mdConfig.getVersionFields().iterator();
        versionFields = new LocalFieldDesc[size];

        for (int i = 0; i < size; i++) {
            MappingFieldElement mdField = (MappingFieldElement) versionFieldIterator.next();
            LocalFieldDesc f = (LocalFieldDesc) getField(mdField.getName());

            if (f != null) {
                if (logger.isLoggable()) {
                    logger.finest("sqlstore.model.classdesc.vcfield", f.getName()); // NOI18N
                }

                versionFields[i] = f;
                registerVersionFieldWithTable(f);

                // The fetch group for version fields should always be DFG.
                f.fetchGroup = FieldDesc.GROUP_DEFAULT;
                f.sqlProperties &= ~(FieldDesc.PROP_REF_INTEGRITY_UPDATES);
                f.sqlProperties |= FieldDesc.PROP_VERSION_FIELD;
            } else {
                throw new JDOFatalUserException(I18NHelper.getMessage(messages,
                        "core.configuration.noneexistentvcfield", // NOI18N
                        mdField.getName(), pcClass.getName()));
            }
        }
    }

    /**
     * Registers the version field <cod>versionField</code> with the
     * corresponding table.
     *
     * @param versionField Field used in version consistency check.
     */
    private void registerVersionFieldWithTable(LocalFieldDesc versionField) {
        // Version field must be mapped to exactly one column.
        ColumnElement ce =  (ColumnElement) versionField.getColumnElements().next();
        Iterator iter = tables.iterator();

        while (iter.hasNext()) {
            TableDesc table = (TableDesc) iter.next();

            if (!table.isJoinTable()) {
                if (ce.getDeclaringTable() == table.getTableElement()) {
                    table.setVersionField(versionField);
                    break;
                }
            }
        }
    }

    /**
     * Computes all the primitive tracked fields.
     * Primitive fields track each other if they are mapped to same columns.
     * One of them is made the primary tracked field as per precedence rules
     * on the field types. This field is used to bind values to columns while
     * updating the database.
     */
    private void computeTrackedPrimitiveFields() {
        // Compute the list of primitive fields to track for each primitive field.
        for (int i = 0; i < fields.size(); i++) {
            FieldDesc f = (FieldDesc) fields.get(i);

            if (!f.isRelationshipField()) {
                LocalFieldDesc lf = (LocalFieldDesc) f;

                lf.computeTrackedPrimitiveFields();
                lf.computePrimaryTrackedPrimitiveField();
            }
        }
    }

    /**
     * Computes the tracked relationship fields. Relationships are tracked in
     * the following way:
     * <ul>
     * <li>A relationship field tracks a local field, if it's mappped
     * to the same columns. The relationship will be updated when the
     * local field is set.</li>
     * <li>Relationship fields track each other if they are mapped to
     * the same columns. The first field in the field order is marked
     * as primary tracked field. If the foreign key columns are
     * explicitly mapped, this field also tracks the local foreign key
     * field. The foreign key columns are explicitly mapped in
     * overlapping pk/fk situations. The other relationship fields
     * mapped to the same columns are updated when the primary tracked
     * relationship field is set.</li>
     * </ul>
     * @see FieldDesc#PROP_PRIMARY_TRACKED_FIELD
     * @see FieldDesc#PROP_SECONDARY_TRACKED_FIELD
     */
    private void computeTrackedRelationshipFields() {
        // Compute the list of fields to track for each field.
        for (int i = 0; i < 2; i++) {
            ArrayList theFields = null;

            // We first check all the visible fields and then the hidden fields.
            if (i == 0) {
                theFields = this.fields;
            } else {
                if ((theFields = this.hiddenFields) == null) {
                    continue;
                }
            }

            for (int j = 0; j < theFields.size(); j++) {
                FieldDesc f = (FieldDesc) theFields.get(j);

                f.computeTrackedRelationshipFields();
            }
        }
    }

    /**
     * Remove redundant ForeignFieldDescs from each LocalFieldDesc's tracked field list.
     */
    private void cleanupTrackedFields() {

        for (int i = 0; i < fields.size(); i++) {
            FieldDesc f = (FieldDesc) fields.get(i);

            if (f instanceof LocalFieldDesc) {
                ((LocalFieldDesc) f).cleanupTrackedFields();
            }
        }
    }

    /**
     *  The fixupForeignReferences method finds all the references to foreign
     *  classes in this configuration. It then builds the appropriate run-time
     *  information (ForeignFieldDesc) for the foreign reference from the
     *  meta-data.
     */
    private void fixupForeignReferences(ConfigCache cache) {

        for (int i = 0; i < foreignFields.size(); i++) {
            ForeignFieldDesc ff = (ForeignFieldDesc) foreignFields.get(i);
            Class classType = null;

            if ((classType = ff.getComponentType()) == null) {
                classType = ff.getType();
            }

            ClassDesc foreignConfig = (ClassDesc) cache.getPersistenceConfig(classType);

            if (foreignConfig == null) continue;

            // Look up the inverse relationship field name if there is any.
            String irName = pcElement.getRelationship(ff.getName()).getInverseRelationshipName();
            ForeignFieldDesc inverseField = null;

            if (irName != null) {
                inverseField = (ForeignFieldDesc) foreignConfig.getField(irName);
            }

            ff.fixupForeignReference(foreignConfig, inverseField);
        }
    }

    private void fixupFieldProperties() {

        for (int i = 0; i < foreignFields.size(); i++) {
            ForeignFieldDesc ff = (ForeignFieldDesc) foreignFields.get(i);
            ff.fixupFieldProperties();
        }
    }

    LocalFieldDesc getLocalFieldDesc(ColumnElement column) {
        LocalFieldDesc result;

        for (int i = 0; i < 2; i++) {
            ArrayList theFields = null;

            if (i == 0) {
                theFields = fields;
            } else {
                theFields = hiddenFields;
            }

            if (theFields != null) {
                for (int j = 0; j < theFields.size(); j++) {
                    FieldDesc f = (FieldDesc) theFields.get(j);

                    if (f instanceof LocalFieldDesc) {
                        result = (LocalFieldDesc) f;

                        for (int k = 0; k < result.columnDescs.size(); k++) {
                            ColumnElement c = (ColumnElement) result.columnDescs.get(k);

                            // if (c.equals(column))
                            if (c.getName().getFullName().compareTo(column.getName().getFullName()) == 0) {
                                // If f is a tracked field and it is not the primary, we continue
                                // searching.
                                if ((f.getTrackedFields() != null) &&
                                        ((f.sqlProperties & FieldDesc.PROP_PRIMARY_TRACKED_FIELD) == 0)) {
                                    continue;
                                }

                                return result;
                            }
                        }
                    }
                }
            }
        }

        // If we didn't find the field associated with the column, we need to
        // create a hidden field and add it to the hiddenFields list.
        result = createLocalHiddenField(column);

        return result;
    }

    /**
     * Returns the local field descriptor for the field <code>name</code>.
     *
     * @param name Field name.
     * @return Local field descriptor for the field <code>name</code>.
     * @throws JDOFatalInternalException if the field is not defined for this class.
     */
    public LocalFieldDesc getLocalFieldDesc(String name) {
        FieldDesc desc = getField(name);

        if (desc == null) {
            throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                        "core.generic.unknownfield", // NOI18N
                        name, getName()));
        }

        if (!(desc instanceof LocalFieldDesc)) {
            throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                        "core.generic.notinstanceof", // NOI18N
                        desc.getClass().getName(), "LocalFieldDesc")); // NOI18N
        }

        return ((LocalFieldDesc) desc);
    }

    public TableDesc findTableDesc(TableElement mdTable) {
        for (int i = 0; i < tables.size(); i++) {
            TableDesc t = (TableDesc) tables.get(i);

            if (t.getTableElement().equals(mdTable)) {
                return t;
            }
        }

        return null;
    }

    private void addTableDesc(TableDesc t) {
        // setConsistencyLevel of this table before adding it to our list
        if (!t.isJoinTable()) {
            // JoinTables represent relationships instead of "real" objects,
            // they should never have a special consistencyLevel.
            t.setConsistencyLevel(mdConfig.getConsistencyLevel());
        }
        tables.add(t);
    }

    public int getTableIndex(TableDesc tableDesc) {
        return tables.indexOf(tableDesc);
    }

    public FieldDesc getField(String name) {
        for (int i = 0; i < fields.size(); i++) {
            FieldDesc f = (FieldDesc) fields.get(i);

            if ((f != null) && (f.getName().compareTo(name) == 0)) {
                return f;
            }
        }

        if (hiddenFields != null) {
            for (int i = 0; i < hiddenFields.size(); i++) {
                FieldDesc f = (FieldDesc) hiddenFields.get(i);

                if (f.getName().compareTo(name) == 0) {
                    return f;
                }
            }
        }

        return null;
    }

    public FieldDesc getField(int index) {
        if (index >= 0) {
            return (FieldDesc) fields.get(index);
        } else {
            return (FieldDesc) hiddenFields.get(-(index + 1));
        }
    }

    public Constructor getConstructor() {
        return constructor;
    }

    public Class getPersistenceCapableClass() {
        return pcClass;
    }

    public Class getOidClass() {
        return oidClass;
    }

    public String getName() {
        return pcClass.getName();
    }

    public Iterator getTables() {
        return tables.iterator();
    }

    public TableDesc getPrimaryTable() {
        return (TableDesc) tables.get(0);
    }

    public boolean isNavigable() {
        return mdConfig.isNavigable();
    }

    public boolean hasVersionConsistency() {
        return mdConfig.getConsistencyLevel() == MappingClassElement.VERSION_CONSISTENCY;
    }

    public LocalFieldDesc[] getVersionFields() {
        return versionFields;
    }

    public Concurrency getConcurrency(boolean optimistic) {
        // Following algo is used to determine which concurrency to return:
        // consistency level specified in model(represented by the variable consistencyLevel),
        // takes precedence over
        // concurrency specified by transaction (represented by parameter 'optimistic' to this method)

        Concurrency concurrency = null;
        int consistencyLevel = mdConfig.getConsistencyLevel();

        if (consistencyLevel == MappingClassElement.NONE_CONSISTENCY) {
            // No consistency level specified in model
            if (optimistic) {
                concurrency = (Concurrency) optimisticConcurrency.clone();
            } else {
                concurrency = (Concurrency) databaseConcurrency.clone();
            }

        } else {
            // currently, we would not try to interprete consistencyLevel as bitmap
            // When we implment consistencyLevel like 2+, 3+, we would have to interprete
            // consistencyLevel as bitmap and implement some changes in class hierarchy
            // starting  with interface Concurrency
            switch(consistencyLevel) {

                // We would never reach this code because we are in the else part
                // case MappingClassElement.NONE_CONSISTENCY :
                //	concurrency = (Concurrency) databaseConcurrency.clone();
                //	break;

            case MappingClassElement.CHECK_MODIFIED_AT_COMMIT_CONSISTENCY :
                concurrency = (Concurrency) checkDirtyConcurrency.clone();
                break;

            case MappingClassElement.LOCK_WHEN_LOADED_CONSISTENCY :
                // This consistency level is implemeted inside SelectStatement and
                // not by any object implementing Concurrency
                concurrency = (Concurrency) explicitConcurrency.clone();
                break;

            case MappingClassElement.VERSION_CONSISTENCY :
                // This consistency level is implemented inside ClassDesc and
                // UpdateQueryPlan and not by any object implementing Concurrency.
                // Selects are handled in ClassDesc, updates/deletes in
                // UpdateQueryPlan.
                concurrency = (Concurrency) databaseConcurrency.clone();
                break;

            // **Note**
            // Plese change text for the exception thrown in default below when
            // we start supporting new consistency levels. The text lists currently supported
            // consistency level
            default :
                throw new JDOUnsupportedOptionException(I18NHelper.getMessage(messages,
                    "core.configuration.unsupportedconsistencylevel", pcClass));// NOI18N
            }
        }

        return concurrency;
    }

    /**
     * Determines whether this classDesc has the
     * CHECK_MODIFIED_AT_COMMIT_CONSISTENCY level.
     * @return <code>true</code> if this has the
     * CHECK_MODIFIED_AT_COMMIT_CONSISTENCY level;
     * <code>false</code> otherwise.
     */
    public boolean hasModifiedCheckAtCommitConsistency() {
        return(mdConfig.getConsistencyLevel() ==
                MappingClassElement.CHECK_MODIFIED_AT_COMMIT_CONSISTENCY);
    }

    public boolean isPKField(int index) {
        return persistentFields[index].isKey();
    }

    /**
     * Return the key fields as array of java.lang.reflect.Field instances.
     * @return The key fields as array of java.lang.reflect.Field instances.
     */
    public Field[] getKeyFields() {
        return keyFields;
    }

    /**
     * Returns the key field names as array of String.
     * @return The key field names as array of String.
     */
    public String[] getKeyFieldNames() {
        return keyFieldNames;
    }

    /**
     * Returns the descriptors for key fields as array of LocalFieldDesc.
     * @return The descriptors for key fields as array of LocalFieldDesc.
     */
    public LocalFieldDesc[] getKeyFieldDescs() {
        return keyFieldDescs;
    }

    /**
     * Returns a new <code>SQLStateManager</code> instance,
     * initialized with passed store manager and this instance of
     * the runtime class model.
     *
     * @param store Store manager, an instance of SQLStoreManager.
     * @return A new <code>SQLStateManager</code> instance.
     */
    public SQLStateManager newStateManagerInstance(PersistenceStore store) {
        return new SQLStateManager(store, this);
    }

    /**
     * Returns a RetrieveDescriptor which represent a SQL query selecting a pc
     * instance by pk-fields. Please note that the RDs are cached, so the method
     * first checks the cache. If there is no corresponding RetrieveDescriptor
     * in the cache, it creates a new one and stores it in the cache. If the
     * additionalField is not null, the method retrieves the field indicated by
     * it along with the query. Fetch group fields will be added when the query
     * plan is build.
     *
     * Note, the reason to introduce the RetrieveDesc cache in ClassDesc and not
     * in the store manager is, that we can have the cache per class, where
     * the store manager could only provide one big cache for all pc classes.
     *
     * @param additionalField The field to be retrieved in addition to the
     * DFG fields.
     * @param store The store manager.
     * @return A RetrieveDescriptor selecting a pc instance by pk-fields.
     * @see #getRetrieveDescForFKQuery
     */
    public RetrieveDesc getRetrieveDescForPKQuery(FieldDesc additionalField, PersistenceStore store) {
        RetrieveDescImpl rd = null;
        String cacheKey = generateRDCacheKey(additionalField);

        synchronized (retrieveDescCache) {
            // Cache lookup.
            rd = (RetrieveDescImpl) retrieveDescCache.get(cacheKey);
            // Generate a new RD if there isn't one be found in the cache.
            if (rd == null) {
                rd = (RetrieveDescImpl) store.getRetrieveDesc(pcClass);
                if (additionalField != null) {
                    RetrieveDesc frd = null;
                    String name = additionalField.getName();
                    // If the additionalField is not null, we will retrieve
                    // the field indicated by it along with the query.
                    if (additionalField instanceof ForeignFieldDesc) {
                        Class additionalClass = ((ForeignFieldDesc) additionalField).
                                foreignConfig.getPersistenceCapableClass();

                        frd = store.getRetrieveDesc(additionalClass);
                    }

                    rd.addPrefetchedField(name, frd);
                }

                addPKConstraints(rd);
                // Cache fillup.
                retrieveDescCache.put(cacheKey, rd);
            }
        }

        return rd;
    }

    /**
     * Returns a RetrieveDescriptor which represent a SQL query selecting pc
     * instances by the relationship key. The relationship key is taken from
     * the foreign field <code>foreignField</code> and used as query constraint.
     * Please note that the RDs are cached, so the method first checks the cache.
     * If there is no corresponding RetrieveDescriptor in the cache, it creates
     * a new one and stores it in the cache. FetchGroup fields will be added
     * when the query plan is build, see <code>SelectQueryPlan#processFetchGroups</code>.
     *
     * Note, the reason to introduce the RetrieveDesc cache in ClassDesc and not
     * in the store manager is, that we can have the cache per class, where
     * the store manager could only provide one big cache for all pc classes.
     *
     * @param foreignField The relationship field to be retrieved.
     *  Following is true for this field.
     * <ul>
     *  <li> It is part of an independent fetch group with only one field in the fetch group.
     *       <p>Or
     *       <P>Not part of any fetch group.
     *  </li>
     *  <li>It is not mapped to a join table. </li>
     * </ul>
     * @param store The store manager.
     * @return A RetrieveDescriptor selecting pc instance(s) corresponding to
     * the foreign field
     * @see #getRetrieveDescForPKQuery
     */
    public RetrieveDesc getRetrieveDescForFKQuery(ForeignFieldDesc foreignField, PersistenceStore store) {
        RetrieveDescImpl rd = null;
        String cacheKey = generateRDCacheKey(foreignField);

        synchronized (foreignRetrieveDescCache) {
            // Cache lookup.
            rd = (RetrieveDescImpl) foreignRetrieveDescCache.get(cacheKey);
            // Generate a new RD if there isn't one be found in the cache.
            if (rd == null) {
                rd = (RetrieveDescImpl) store.getRetrieveDesc(foreignField.foreignConfig.getPersistenceCapableClass());

                addFKConstraints(rd, foreignField);
                // Cache fillup.
                foreignRetrieveDescCache.put(cacheKey, rd);
            }
        }

        return rd;
    }

    /**
     * Gets RetrieveDescriptor(rd) for verifying a VC instance. The returned rd
     * is set up to expect constraints for pk followed by constraints for version
     * fields.
     * @param store
     * @return Instance of retrieve Descriptor for verifying a VC instance.
     */
    public RetrieveDesc getRetrieveDescForVerificationQuery(PersistenceStore store) {

        assert hasVersionConsistency();

        synchronized(retrieveDescForVerificationSynchObj) {
            if (retrieveDescForVerification == null) {
                RetrieveDescImpl rd = (RetrieveDescImpl) store.getRetrieveDesc(pcClass);

                int index = addPKConstraints(rd);
                rd.addParameterConstraints(versionFields, index);
                rd.setOption(RetrieveDescImpl.OPT_VERIFY);

                retrieveDescForVerification = rd;
            }
        }
        return retrieveDescForVerification;
    }

    /**
     * Generate for each keyField 'keyfield = ?' in the where clause
     * with InputParamValues for later binding of the actual query parameters.
     */
    private int addPKConstraints(RetrieveDescImpl rd) {
        // PK Constraints are always added first hence startIndex == 0
        rd.addParameterConstraints(keyFieldDescs, 0);

        return keyFieldDescs.length;
    }

    /**
     * Generate the condition on the relationship key in the where clause
     * with InputParamValues for later binding of the actual query parameters.
     */
    private void addFKConstraints(RetrieveDescImpl rd, ForeignFieldDesc foreignField) {
        for (int i = 0; i < foreignField.foreignFields.size(); i++) {
            LocalFieldDesc fff = (LocalFieldDesc) foreignField.foreignFields.get(i);
            rd.addParameterConstraint(fff, i);
        }
    }

    /**
     * Generates the key for a RetrieveDescriptor for the cache lookup.
     * The key has one of two forms:
     * <li>
     * Fully qualified classname of the pcclass, if additionalField is null.
     * </li>
     * <li>
     * Fully qualified classname of the pcclass + '/' + additionalFieldName +
     * '/' + additionalFieldID, otherwise.
     * </li>
     * @param additionalField The field to be retrieved in addition to the
     * DFG fields.
     * @return The generated cache key as a String.
     */
    private String generateRDCacheKey(FieldDesc additionalField)
    {
        StringBuffer key = new StringBuffer();

        key.append(pcClass.getName());

        if (additionalField != null) {
            // using '/' as separator between class and fieldname
            key.append('/');
            key.append(additionalField.getName());
            key.append('/');
            key.append(additionalField.absoluteID);
        }

        return key.toString();
    }

    /**
     * Returns true, if this class has got local fields not in the default
     * fetch group. Because UpdateQueryPlans for updates are cached depending
     * on the set of updated fields {@link #getUpdateQueryPlanForUpdate}, we
     * might need to compare the updated fields btw. two instances when
     * batching is enabled.
     *
     * @return True, if this class has got local fields not in the default
     * fetch group.
     * @see SQLStateManager#requiresImmediateFlush
     */
    public boolean hasLocalNonDFGFields() {
        // All instances with modified DFG fields can be batched with
        // the same statement. If there's a primitive field outside
        // the DFG, we need to compare modified fields to check if the
        // same statement can be used.
        return hasLocalNonDFGField;
    }

    /**
     * Retrieves the update query plan for the specified descriptor.
     * @param desc the descriptor
     * @param store the store manager
     * @return Instance of update query plan for the specified descriptor.
     */
    public UpdateQueryPlan getUpdateQueryPlan(
            UpdateObjectDescImpl desc, SQLStoreManager store)
    {
        switch (desc.getUpdateAction()) {
            case ActionDesc.LOG_CREATE:
                return getUpdateQueryPlanForInsert(desc, store);
            case ActionDesc.LOG_DESTROY:
                return getUpdateQueryPlanForDelete(desc, store);
            case ActionDesc.LOG_UPDATE:
                return getUpdateQueryPlanForUpdate(desc, store);
            default:
                // TBD: error message + I18N
                // error unknown action
                return null;
        }
    }

    /** */
    private UpdateQueryPlan getUpdateQueryPlanForInsert(
            UpdateObjectDescImpl desc, SQLStoreManager store)
    {
        synchronized(updateQueryPlanForInsertSynchObj) {
            if (updateQueryPlanForInsert == null) {
                updateQueryPlanForInsert = buildQueryPlan(store, desc);
            }
        }
        return updateQueryPlanForInsert;
    }

    /** */
    private UpdateQueryPlan getUpdateQueryPlanForDelete(
            UpdateObjectDescImpl desc, SQLStoreManager store)
    {
        synchronized(updateQueryPlanForDeleteSynchObj) {
            if (updateQueryPlanForDelete == null) {
                updateQueryPlanForDelete = buildQueryPlan(store, desc);
            }
        }
        return updateQueryPlanForDelete;
    }

    /** */
    private UpdateQueryPlan getUpdateQueryPlanForUpdate(
            UpdateObjectDescImpl desc, SQLStoreManager store)
    {
        String key = getSortedFieldNumbers(desc.getUpdatedFields());
        UpdateQueryPlan plan;
        synchronized(updateQueryPlanCache) {
            plan = (UpdateQueryPlan)updateQueryPlanCache.get(key);
            if (plan == null) {
                plan = buildQueryPlan(store, desc);
                updateQueryPlanCache.put(key, plan);
            }
        }
        return plan;
    }

    /**
     * Builds and initializes a new query plan based on the information
     * passed with the <code>desc<//code> parameter. The returned plan
     * and its related data structures will be readonly.
     *
     * @param store Store manager
     * @param desc Update information, including the update action and
     * modified fields as appropriate.
     *
     * @return A new query plan. The returned plan will be readonly.
     */
    private UpdateQueryPlan buildQueryPlan(SQLStoreManager store,
                                           UpdateObjectDescImpl desc) {
        UpdateQueryPlan plan;

        plan = new UpdateQueryPlan(desc, store);
        plan.build(true);

        // Initialize the text for all statements. After this point,
        // the plan and its related data structures will be readonly.
        plan.getStatements();

        return plan;
    }

    /**
     * The methods returns the string representation of the sorted field numbers
     * of the FieldDescs.
     * The key is the string representation of the sorted field number list
     * of updated fields.
     * @param fields the list of FieldDescs
     * @return the sorted field number string
     */
    private String getSortedFieldNumbers(List fields)
    {
        // Use the array of field numbers of the updated fields as the key
        int size = fields.size();
        int [] fieldNos = new int[size];
        for (int i = 0; i < size; i++) {
            FieldDesc f = (FieldDesc)fields.get(i);
            fieldNos[i] = f.absoluteID;
        }
        Arrays.sort(fieldNos);
        return StringHelper.intArrayToSeparatedList(fieldNos, ","); //NOI18N
    }

}
