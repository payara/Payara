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
 * ForeignFieldDesc.java
 *
 * Created on March 3, 2000
 *
 */

package com.sun.jdo.spi.persistence.support.sqlstore.model;

import org.netbeans.modules.dbschema.ColumnElement;
import org.netbeans.modules.dbschema.TableElement;
import com.sun.jdo.api.persistence.model.jdo.RelationshipElement;
import com.sun.jdo.api.persistence.support.JDOFatalInternalException;
import com.sun.jdo.spi.persistence.support.sqlstore.SQLStateManager;
import org.glassfish.persistence.common.I18NHelper;
import com.sun.jdo.spi.persistence.utility.logging.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;

/**
 *
 */
public class ForeignFieldDesc extends FieldDesc {

    // Values for deleteAction
    /**
     * When the parent object is deleted from db,
     * delete the relationship object described by this object also.
     */
    public static final int ACT_CASCADE = RelationshipElement.CASCADE_ACTION;

    /**
     * When the parent object is deleted,
     * no action is required for the relationship object described by this object.
     */
    public static final int ACT_NONE = RelationshipElement.NONE_ACTION;

    /** Currently runtime code does not interprete this action. */
    public static final int ACT_NULLIFY = RelationshipElement.NULLIFY_ACTION;

    /** Currently runtime code does not interprete this action. */
    public static final int ACT_RESTRICT = RelationshipElement.RESTRICT_ACTION;

    /** Currently runtime code does not interprete this action. */
    public static final int ACT_AGGREGATE = RelationshipElement.AGGREGATE_ACTION;

    /** Class descriptor for the class of this relationship field. */
    public ClassDesc foreignConfig;

    public int cardinalityLWB;

    public int cardinalityUPB;

    // Takes one of the following values.
    public int deleteAction;

    /** Array of LocalFieldDesc. */
    public ArrayList foreignFields;

    /** Array of ColumnElement. */
    public ArrayList foreignColumns;

    /** Array of LocalFieldDesc. */
    public ArrayList localFields;

    /** Array of ColumnElement. */
    public ArrayList localColumns;

    /** Array of LocalFieldDesc. */
    public ArrayList assocForeignFields;

    /** Array of ColumnElement. */
    public ArrayList assocForeignColumns;

    /** Array of LocalFieldDesc. */
    public ArrayList assocLocalFields;

    /** Array of ColumnElement. */
    public ArrayList assocLocalColumns;

    /**
     * If inverseRelationshipField is not null, it means this field is
     * under managed relationship. Otherwise, this is a one-way relationship.
     */
    private ForeignFieldDesc inverseRelationshipField;

    /**
     * True, if the relationship is mapped to primary key fields
     * <b>on the other relationship side</b>.
     */
    private boolean isMappedToPk;

    ForeignFieldDesc(ClassDesc config) {
        super(config);
    }

    /**
     * Returns true.
     */
    public boolean isRelationshipField() {
        return true;
    }

    /**
     * Returns true, if the relationship is mapped to a join table.
     *
     * @return True, if the relationship is mapped to a join table, false otherwise.
     */
    public boolean useJoinTable() {
        return (assocLocalColumns != null && assocLocalColumns.size() > 0);
    }

    /**
     * Checks the conditions that guarantee, that we have the foreign
     * key on this side.
     *
     * @return True, if this relationship is mapped to foreign keys and the
     * foreign key is on the local side. False otherwise.
     */
    public boolean hasForeignKey() {
        boolean result = false;

        // RESOLVE: Can't check PROP_REF_INTEGRITY_UPDATES for 1 way mappings.
        // See #checkReferentialIntegrityUpdatesForObjectField.
        if (inverseRelationshipField != null) {
            result = cardinalityUPB == 1 && !useJoinTable() &&
                    (sqlProperties & FieldDesc.PROP_REF_INTEGRITY_UPDATES) > 0;
        }
        return result;
    }

    /**
     * Returns true, if the relationship is mapped to primary key fields
     * <b>on the other relationship side</b>.
     *
     * @return True, if the relationship is mapped to primary key fields
     * <b>on the other relationship side</b>, false otherwise.
     * @see #initializeIsMappedToPk
     */
    public boolean isMappedToPk() {
        return isMappedToPk;
    }

    public ArrayList getLocalFields() {
        if (localFields == null) {
            localFields = new ArrayList();
        }

        return localFields;
    }

    public ArrayList getForeignFields() {
        if (foreignFields == null) {
            foreignFields = new ArrayList();
        }

        return foreignFields;
    }

    public ArrayList getAssocLocalFields() {
        // Only create assocLocalFields if there is a corresponding
        // assocLocalColumns to save space.
        if (assocLocalFields == null && assocLocalColumns != null) {
            assocLocalFields = new ArrayList();
        }

        return assocLocalFields;
    }

    public ArrayList getAssocForeignFields() {
        // Only create assocForeignFields if there is a corresponding
        // assocForeignColumns to save space.
        if (assocForeignFields == null && assocForeignColumns != null) {
            assocForeignFields = new ArrayList();
        }

        return assocForeignFields;
    }

    public ForeignFieldDesc getInverseRelationshipField() {
        return inverseRelationshipField;
    }

    /**
     * Constructs the oid of a related instance. If called by {@link
     * SQLStateManager#updateTrackedFields}, the new value for the
     * local field <code>fieldDesc</code> is not yet set and passed as
     * parameter <code>value</code>. If called for navigation by
     * {@link SQLStateManager#populateForeignField}, values of updated
     * local fields must be retrieved from the before image.  In both
     * cases, the actual call to this method is in {@link
     * SQLStateManager#getObjectById}.<p>
     * For tracked field usage, see
     * {@link SQLStateManager#setForeignKey} and
     * {@link SQLStateManager#updateTrackedFields}.
     * For navigation usage, see
     * {@link SQLStateManager#realizeForeignField}.
     *
     * @param sm State manager on the local side.
     * @param fieldDesc Local field being set.
     * @param value New value of the field being set.
     * @return The object id for the related instance. This id is used
     * to lookup the instance from the persistence manager cache. The
     * construced oid is invalid, if one of the oid fields is
     * null. In this case the returned value is null.
     */
    public Object createObjectId(SQLStateManager sm, LocalFieldDesc fieldDesc, Object value) {

        assert isMappedToPk();

        Class oidClass = foreignConfig.getOidClass();
        Object oid = null;

        try {
            oid = oidClass.newInstance();
        } catch (Exception e) {
            throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                    "core.statemanager.cantnewoid", oidClass.getName()), e); // NOI18N
        }

        Field keyFields[] = foreignConfig.getKeyFields();
        String keyFieldNames[] = foreignConfig.getKeyFieldNames();
        for (int i = 0; i < keyFields.length && oid != null; i++) {
            Field keyField = keyFields[i];

            for (int j = 0; j < foreignFields.size() && oid != null; j++) {
                LocalFieldDesc fa = (LocalFieldDesc) foreignFields.get(j);

                if (fa.getName().compareTo(keyFieldNames[i]) == 0) {
                    LocalFieldDesc la = (LocalFieldDesc) localFields.get(j);
                    Object keyFieldValue = null;

                    if (la == fieldDesc) {
                        keyFieldValue = value;
                    } else if (sm.getSetMaskBit(la.absoluteID) && !sm.getSetMaskBit(absoluteID)) {
                        keyFieldValue = la.getValue(sm.getBeforeImage());
                    } else {
                        keyFieldValue = la.getValue(sm);
                    }

                    if (keyFieldValue != null) {
                        try {
                            // We need to convert the keyFieldValue to the proper type before
                            // setting it.
                            keyField.set(oid, fa.convertValue(keyFieldValue, sm));
                        } catch (IllegalAccessException e) {
                            throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                                    "core.statemanager.cantsetkeyfield", keyField.getName()), e); // NOI18N
                        }
                    } else {
                        oid = null;
                    }
                }
            }
        }

        return oid;
    }

    //
    // ------------ Initialisation methods ------------
    //

    // TODO: Should be removed, once computeTrackeRelationshipFields is removed.
    private void setInverseRelationshipField(ForeignFieldDesc f) {
        inverseRelationshipField = f;
    }

    void computeTrackedRelationshipFields() {
        // If the field is a ForeignFieldDesc, we only need
        // to compare against other ForeignFieldDesc. The reason
        // is that ForeignFieldDesc implicitly tracks a LocalFieldDesc
        // (foreign key field) via relationship updates.

        ForeignFieldDesc inverseField = getInverseRelationshipField();

        for (int k = 0; k < classDesc.foreignFields.size(); k++) {
            ForeignFieldDesc tf = (ForeignFieldDesc) classDesc.foreignFields.get(k);

            if ((this != tf) && (getType() == tf.getType()) && (compareColumns(this, tf) == true)) {
                if ((inverseField != null) &&
                        (tf.getInverseRelationshipField() == null)) {
                    tf.setInverseRelationshipField(inverseField);
                }

                // Mark the relationship field tracking the foreign key as primary.
                if ((sqlProperties & FieldDesc.PROP_SECONDARY_TRACKED_FIELD) == 0) {
                    sqlProperties |= FieldDesc.PROP_PRIMARY_TRACKED_FIELD;
                }

                if ((tf.sqlProperties & FieldDesc.PROP_PRIMARY_TRACKED_FIELD) == 0) {
                    tf.sqlProperties |= FieldDesc.PROP_SECONDARY_TRACKED_FIELD;
                }

                addTrackedField(tf);
            }
        }
    }

    /**
     * Initializes relationship field information.
     *
     * @param foreignConfig Class descriptor of the foreign class.
     * @param inverseField The inverse relationship field.
     */
    void fixupForeignReference(ClassDesc foreignConfig,
                               ForeignFieldDesc inverseField) {

        registerForeignConfig(foreignConfig, inverseField);
        initializeFieldLists();
        initializeIsMappedToPk();
        addForeignKeyFieldsToDFG();
    }

    /**
     * Registers the relationship information about the foreign class.
     *
     * @param foreignConfig Class descriptor of the foreign class.
     * @param inverseField The inverse relationship field.
     */
    private void registerForeignConfig(ClassDesc foreignConfig,
                                       ForeignFieldDesc inverseField) {
        boolean debug = logger.isLoggable(Logger.FINEST);

        if (debug) {
            Object[] items = new Object[] {classDesc, this, foreignConfig};
            logger.finest("sqlstore.model.classdesc.general", items); // NOI18N
        }

        // Remember the class descriptor for the foreign class.
        this.foreignConfig = foreignConfig;

        if (debug && inverseField != null) {
            logger.finest("sqlstore.model.classdesc.assocrelatedfield", inverseField); //NOI18N
        }

        setInverseRelationshipField(inverseField);
    }

    /**
     * Initialize the field lists based on column list information.
     */
    private void initializeFieldLists() {
        ClassDesc theConfig = classDesc;

        for (int i = 0; i < 4; i++) {
            ArrayList fields = null;
            ArrayList columns = null;

            switch (i) {
                case 0:
                    columns = localColumns;
                    fields = getLocalFields();
                    break;
                case 1:
                    columns = assocLocalColumns;
                    fields = getAssocLocalFields();
                    break;
                case 2:
                    columns = assocForeignColumns;
                    fields = getAssocForeignFields();
                    break;
                case 3:
                    columns = foreignColumns;
                    fields = getForeignFields();
                    theConfig = foreignConfig;
                    break;
            }

            if (columns == null) continue;

            for (int j = 0; j < columns.size(); j++) {
                ColumnElement ce = (ColumnElement) columns.get(j);
                TableElement te = ce.getDeclaringTable();

                if (te == null) {
                    throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                            "core.configuration.columnnotable")); // NOI18N
                }

                fields.add(theConfig.getLocalFieldDesc(ce));
            }
        }
    }

    /**
     * Checks, if the relationship is mapped to primary key fields
     * <b>on the other relationship side</b> and sets the property
     * <code>isMappedToPk</code>.
     */
    private void initializeIsMappedToPk() {
        int count = foreignFields.size();

        isMappedToPk = !useJoinTable() &&
                foreignConfig.getKeyFields().length == count;

        for (int i = 0; i < count && isMappedToPk; i++) {
            isMappedToPk = ((LocalFieldDesc) foreignFields.get(i)).isKeyField();
        }
    }

    /**
     * Silently adding hidden (local foreign key) fields to the DFG.
     */
    private void addForeignKeyFieldsToDFG() {
        for (int i = 0; i < localFields.size(); i++)  {
            LocalFieldDesc lf = (LocalFieldDesc) localFields.get(i);

            if (lf.absoluteID < 0 && !useJoinTable()) {
                classDesc.getFetchGroup(GROUP_DEFAULT).add(lf);
            }
        }
    }

    /**
     * Determines the relationship side to be updated. Foreign key relationships
     * must always be updated on the side having the foreign key. Jointable
     * relationships can be handled from either side. To have unified
     * dependency management for foreign key _and_ jointable relationships,
     * it's essential that we apply the same rules defining the updated side
     * for foreign key and jointable relationships. We also need to always
     * update the same relationship side.
     */
    void fixupFieldProperties() {
        boolean refIntegrityUpdate = true;

        if (cardinalityUPB > 1) {
            // Collection side
            if (!(refIntegrityUpdate = checkReferentialIntegrityUpdatesForCollectionField())) {
                unsetReferentialIntegrityUpdateProperty();

                // We also unset the IN_CONCURRENCY_CHECK property because we can't
                // detect concurrency violation for changes made to a Collection
                sqlProperties &= ~(FieldDesc.PROP_IN_CONCURRENCY_CHECK);
            }
        } else {
            // Object side
            if (!(refIntegrityUpdate = checkReferentialIntegrityUpdatesForObjectField())) {
                unsetReferentialIntegrityUpdateProperty();

                sqlProperties &= ~(FieldDesc.PROP_IN_CONCURRENCY_CHECK);
            } else if (!useJoinTable()) {
                // This side will write relationship updates to the database. 
                // Mark the local fields as part of the foreign key.
                for (int i = 0; i < localFields.size(); i++) {
                    ((LocalFieldDesc)localFields.get(i)).sqlProperties |= FieldDesc.PROP_FOREIGN_KEY_FIELD;
                }
            }
        }

        if (!refIntegrityUpdate) {
            unsetConcurrencyCheckProperty();
        }
    }

    /**
     * Checks, if datastore updates will be scheduled locally or on
     * the opposite relationship side.
     *
     * @return True, if datastore updates for this relationship will
     *   be scheduled locally, false otherwise.
     */
    private boolean checkReferentialIntegrityUpdatesForCollectionField() {
        boolean refIntegrityUpdate;
        ForeignFieldDesc inverseFieldDesc = getInverseRelationshipField();

        if (inverseFieldDesc == null) {
            refIntegrityUpdate = defineUpdatedSideXToM();
        } else {
            if (inverseFieldDesc.cardinalityUPB <= 1) {
                // For 1:N relationships, we always update the relationship side
                // which includes jointables. We indicate this by unsetting the
                // REF_INTEGRITY_UPDATES property which means that integrity updates
                // cannot be done locally.
                refIntegrityUpdate = false;
            } else {
                // For N:M relationships, we choose the updated relationship side
                // depending on the alphabethical order of the related class names.
                // N:M relationships must be mapped to a jointable. As jointable
                // entries can be created from each side, we just define the side.
                refIntegrityUpdate = defineUpdatedSideNToM(inverseFieldDesc);
            }
        }
        return refIntegrityUpdate;
    }

    /**
     * Checks, if datastore updates will be scheduled locally or on
     * the opposite relationship side.
     *
     * @return True, if datastore updates for this relationship will
     *   be scheduled locally, false otherwise.
     */
    private boolean checkReferentialIntegrityUpdatesForObjectField() {
        boolean refIntegrityUpdate;
        ForeignFieldDesc inverseFieldDesc = getInverseRelationshipField();

        if (inverseFieldDesc == null) {
            // Update the local side for one-way relationships.
            refIntegrityUpdate = true;
        } else {
            if (inverseFieldDesc.cardinalityUPB > 1) {
                // For 1:N relationships, we always update the local side
                // which includes jointables. We indicate this by setting the
                // REF_INTEGRITY_UPDATES property which means that integrity updates
                // are done locally.
                refIntegrityUpdate = true;
            } else {
                // For 1:1 relationships, we choose the updated relationship side
                // depending on the side having the foreign key. If the relationship
                // is mapped to primary key fields only, we consider if one side
                // is marked for cascade delete. Otherwise, we choose the updatable
                // side depending on the alphabethical order of the related class names.
                refIntegrityUpdate = defineUpdatedSide1To1(inverseFieldDesc);
            }
        }
        return refIntegrityUpdate;
    }

    /**
     * Defines the updated side for a collection relationship
     * mapped one-way.
     *
     * @return True, if datastore updates for this relationship will
     *   be scheduled locally, false otherwise.
     */
    private boolean defineUpdatedSideXToM() {
        boolean refIntegrityUpdate;
        if (!useJoinTable()) {
            // As this is a foreign key relationship, the other side must
            // be the one side. Foreign key relationships can be updated
            // from either side, even if the inverse side is unknown.
            refIntegrityUpdate = false;
        } else {
            // Update the local side for one-way relationships mapped to jointables.
            refIntegrityUpdate = true;
        }
        return refIntegrityUpdate;
    }

    /**
     * Defines the updated side for many-to-many relationships. As jointable
     * entries can be created from either side, we just define the side. The updated
     * side is chosen based on the alphabethical order of the related class names.
     *
     * @param inverseFieldDesc Inverse relationship field.
     *   Is guaranteed to be not null!
     * @return True, if datastore updates for this relationship will
     *   be scheduled locally, false otherwise.
     */
    private boolean defineUpdatedSideNToM(ForeignFieldDesc inverseFieldDesc) {
        boolean refIntegrityUpdate;
        final boolean updateOtherSide = (inverseFieldDesc.sqlProperties & FieldDesc.PROP_REF_INTEGRITY_UPDATES) > 0;

        if (!updateOtherSide) {
            // The opposite side has already been identified not to be updated.
            refIntegrityUpdate = true;
        } else {
            refIntegrityUpdate = chooseUpdatedSide(inverseFieldDesc);
        }
        return refIntegrityUpdate;
    }

    /**
     * Defines the updated side for one-to-one relationships. The updated side
     * is either defined by:
     *
     * <ol>
     * <li>The relationship side mapped to non-key columns.</li>
     * <li>The relationship side is identified as dependent side, or</li>
     * <li><ul>
     * <li>Mark both sides updatable for foreign key relationships.</li>
     * <li>Choose a side for jointable relationships.</li>
     * </ul></li>
     * </ol>
     *
     * @param inverseFieldDesc Inverse relationship field.
     *   Is guaranteed to be not null!
     * @return True, if datastore updates for this relationship will
     *   be scheduled locally, false otherwise.
     */
    private boolean defineUpdatedSide1To1(ForeignFieldDesc inverseFieldDesc) {
        boolean refIntegrityUpdate;
        final boolean updateOtherSide = (inverseFieldDesc.sqlProperties & FieldDesc.PROP_REF_INTEGRITY_UPDATES) > 0;

        if (!updateOtherSide) {
            // The opposite side has already been identified not to be updated.
            refIntegrityUpdate = true;
        } else if (!useJoinTable()) {
            // Check foreign key constraints and the dependent side.
            refIntegrityUpdate = checkForeignKeysAndDependentSide(inverseFieldDesc);
        } else {
            // Just check the dependent side.
            refIntegrityUpdate = checkDependentSide(inverseFieldDesc);
        }

        if (!refIntegrityUpdate && cardinalityLWB == 1) {
            // Lower bound should not be 1 in this case.
            // We silently set it to 0 for now.
            // RESOLVE: Shall we throw an exception here?
            cardinalityLWB = 0;
        }

        return refIntegrityUpdate;
    }

    /**
     * Checks, if one relationship side isn't mapped to primary key fields
     * (i.e. foreign key side). Based on the assumption, that jointable
     * relationships are always mapped to the primary key columns,
     * this method is called for foreign relationships only.
     *
     * @param inverseFieldDesc Inverse relationship field.
     *   Is guaranteed to be not null!
     * @return True, if datastore updates for this relationship will
     *   be scheduled locally, false otherwise.
     */
    private boolean checkForeignKeysAndDependentSide(ForeignFieldDesc inverseFieldDesc) {
        boolean refIntegrityUpdate;

        // Check the foreign keys.
        if (checkForeignKey(getLocalFields())) {
            // The foreign key is on the local side.
            refIntegrityUpdate = true;
        } else if (checkForeignKey(getForeignFields())) {
            // The foreign key is on the other side.
            refIntegrityUpdate = false;
        } else {
            // The relationship is mapped to primary key columns on either side.
            refIntegrityUpdate = checkDependentSide(inverseFieldDesc);
        }
        return refIntegrityUpdate;
    }

    /**
     * Marks the dependent side as identified by jdo meta-data for update.
     * If no side is marked dependent, the following rules apply:
     *
     * <ul>
     * <li>Foreign key relationships can be handled from both sides,
     * because it must be an 1:1 relationship mapped to primary key
     * fields.</li>
     * <li>Jointable relationships need to be updated from exactly one
     * side to implement unified dependency management. It's important to
     * check the dependency on both relationship sides before the responsible
     * side is chosen.</li>
     * </ul>
     *
     * @param inverseFieldDesc Inverse relationship field.
     *   Is guaranteed to be not null!
     * @return True, if datastore updates for this relationship will
     *   be scheduled locally, false otherwise.
     * @see #isDependentOn
     * @see SQLStateManager#manageDependencyForObjectField
     */
    private boolean checkDependentSide(ForeignFieldDesc inverseFieldDesc) {
        boolean refIntegrityUpdate;

        // Check if meta data identifies the dependent relationship side.
        if (this.isDependentOn(inverseFieldDesc)) {
            // This side is marked dependent and will be updated.
            refIntegrityUpdate = true;
        } else if (inverseFieldDesc.isDependentOn(this)) {
            // This side is marked as primary, the other side will be updated.
            refIntegrityUpdate = false;
        } else {
            if (!useJoinTable()) {
                // No information about the dependent side can be obtained and the
                // relationship is mapped to the primary key fields on both sides.
                // Relationship updates can be done from either side, but only during
                // instance creation/deletion.
                refIntegrityUpdate = true;
            } else {
                // No information about the dependent side can be obtained. If
                // the Employee-Insurance relationship is mapped to a jointable,
                // the dependent side can't be determinated. Identifying the
                // updated side is essential to provide unified dependency
                // management for the database updates, see
                // SQLStateManager#manageDependencyForObjectField
                refIntegrityUpdate = chooseUpdatedSide(inverseFieldDesc);
            }
        }
        return refIntegrityUpdate;
    }

    /**
     * Checks if at least one of the fields in <code>fieldList</code>
     * is updatable. In this case <code>fieldList</code> makes up a
     * foreign key. This is based on the assumption, that key fields
     * referred in relationships must not be updated. Not updatable fields
     * have property REF_INTEGRITY_UPDATES unset.
     *
     * @param fieldList Fields corresponding to datastore columns.
     *   The fields are either a <code>ForeignFieldDesc</code>'s local or foreign fields.
     * @return True, if <code>fieldList</code> is a foreign key, false otherwise.
     */
    private static boolean checkForeignKey(ArrayList fieldList) {
        for (int i = 0; i < fieldList.size(); i++) {
            FieldDesc lf = (FieldDesc) fieldList.get(i);

            if ((lf.sqlProperties & FieldDesc.PROP_REF_INTEGRITY_UPDATES) > 0) {
                // Based on the assumption, that referred key fields
                // have property REF_INTEGRITY_UPDATES unset, at least
                // one of the fields is not part of the key.
                return true;
            }
        }
        return false;
    }

    /**
     * Checks, if <code>this</code> relationship side is dependent
     * on the inverse side <code>inverseFieldDesc</code>. The
     * dependent side can be identified by the following criteria:
     *
     * <ul>
     * <li>This side has cardinalityLWB == 1.</li>
     * <li>The inverse side is marked for cascade delete.</li>
     * </ul>
     *
     * @param inverseFieldDesc Inverse relationship field.
     *   Is guaranteed to be not null!
     * @return true, if <code>this</code> relationship side is dependent
     * on <code>inverseFieldDesc</code>, false otherwise.
     */
    private boolean isDependentOn(ForeignFieldDesc inverseFieldDesc) {
        return (this.cardinalityLWB == 1 ||
                inverseFieldDesc.deleteAction == ForeignFieldDesc.ACT_CASCADE);
    }

    /**
     * Choose the updated relationship side based on the alphabethical
     * order of the related class names. For self relationships, the
     * field names itself are compared, too.
     *
     * @param inverseFieldDesc Inverse relationship field.
     *   Is guaranteed to be not null!
     * @return This method is guaranteed to identify the relationship
     *   side that will not be updated.
     */
    private boolean chooseUpdatedSide(ForeignFieldDesc inverseFieldDesc) {
        int comparison = classDesc.getName().compareTo(foreignConfig.getName());

        if (comparison == 0) {
            comparison = getName().compareTo(inverseFieldDesc.getName());
        }
        return comparison < 0;
    }

    /**
     * Unsets the REF_INTEGRITY_UPDATES property for this relationship field.
     * Datastore updates will be scheduled on the opposite relationship side.
     */
    private void unsetReferentialIntegrityUpdateProperty() {
        if (logger.isLoggable(Logger.FINEST)) {
            logger.finest("sqlstore.model.classdesc.unsetrefintegrityupdate", getName()); // NOI18N
        }

        sqlProperties &= ~(FieldDesc.PROP_REF_INTEGRITY_UPDATES);
    }

    /**
     * Unsets the IN_CONCURRENCY_CHECK property for all the
     * (hidden) local fields involved in this relationship.
     */
    private void unsetConcurrencyCheckProperty() {
        // Copy the field list temporarily.
        ArrayList fieldList = (ArrayList) getLocalFields().clone();

        if (useJoinTable()) {
            fieldList.addAll(getAssocLocalFields());
        }

        for (int j = 0; j < fieldList.size(); j++) {
            FieldDesc lf = (FieldDesc) fieldList.get(j);

            if (lf.absoluteID < 0) {
                if (logger.isLoggable(Logger.FINEST)) {
                    logger.finest("sqlstore.model.classdesc.unsetconcurrencychk", lf.getName()); // NOI18N
                }
                lf.sqlProperties &= ~(FieldDesc.PROP_IN_CONCURRENCY_CHECK);
            }
        }
    }

}
