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
 * SQLStateManager.java
 *
 * Created on March 3, 2000
 */

package com.sun.jdo.spi.persistence.support.sqlstore;

import com.sun.jdo.api.persistence.support.*;
import com.sun.jdo.spi.persistence.support.sqlstore.ejb.EJBHelper;
import com.sun.jdo.spi.persistence.support.sqlstore.model.*;
import com.sun.jdo.spi.persistence.support.sqlstore.query.jqlc.QueryValueFetcher;
import com.sun.jdo.spi.persistence.support.sqlstore.sco.SqlTimestamp;
import com.sun.jdo.spi.persistence.support.sqlstore.sql.UpdateObjectDescImpl;
import com.sun.jdo.spi.persistence.support.sqlstore.state.LifeCycleState;
import com.sun.jdo.spi.persistence.support.sqlstore.state.PersistentNonTransactional;
import com.sun.jdo.spi.persistence.support.sqlstore.state.PersistentClean;
import com.sun.jdo.spi.persistence.support.sqlstore.state.Hollow;
import com.sun.jdo.spi.persistence.utility.NullSemaphore;
import com.sun.jdo.spi.persistence.utility.Semaphore;
import com.sun.jdo.spi.persistence.utility.SemaphoreImpl;
import com.sun.jdo.spi.persistence.utility.logging.Logger;
import org.glassfish.persistence.common.I18NHelper;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.*;


/**
 *
 */
public class SQLStateManager implements Cloneable, StateManager, TestStateManager {

    private static final int PRESENCE_MASK = 0;

    private static final int SET_MASK = 1;

    private static final int MAX_MASKS = 2;

    private BitSet fieldMasks;

    /** Array of Object. */
    public ArrayList hiddenValues;

    private ClassDesc persistenceConfig;

    private PersistenceManager persistenceManager;

    private PersistenceStore store;

    private SQLStateManager beforeImage;

    private Object persistentObject;

    private Object objectId;

    private LifeCycleState state;

    /** This flag is used to disable updates due to dependency management. */
    private static final short ST_UPDATE_DISABLED = 0x1;

    private static final short ST_REGISTERED = 0x2;

    private static final short ST_VISITED = 0x4;

    private static final short ST_PREPARED_PHASE_II = 0x8;

    private static final short ST_FIELD_TRACKING_INPROGRESS = 0x10;

    private static final short ST_DELETE_INPROGRESS = 0x20;

    private static final short ST_VALIDATION_FAILED = 0x40;

    private short stateFlags;

    // This instance is a replacement for a deleted instance with the same
    // ObjectId.
    private boolean isReplacementInstance = false;

    // This instance needs to be registered with the global (weak) cache at
    // rollback if it transitions to persistent (HOLLOW or P_NONTX) state.
    private boolean needsRegisterAtRollback = false;

    // This instance needs to be to be verified at the time it is removed
    // from the global (weak) cache at rollback if it transitions to transient state.
    private boolean needsVerifyAtDeregister = false;

    // This flag is initially set to false and changed to true when the first
    // operation (e.g. makePersistent, loadForRead, or getObjectById) succeeds.
    private boolean valid = false;

    /** Stores the updates to the associated object. */
    private UpdateObjectDescImpl updateDesc;

    /** Contains state managers depending on this object. */
    private HashSet updatedForeignReferences;

    /** Counts the foreign state managers this state manager depends on. */
    private int referenceCount;

    /** Serializes access to this StateManager. */
    private final Semaphore lock;

    /** The logger. */
    private static Logger logger = LogHelperStateManager.getLogger();

    /** I18N message handler. */
    private final static ResourceBundle messages = I18NHelper.loadBundle(
            SQLStateManager.class);

    /** Name of the USE_BATCH property. */
    public static final String USE_BATCH_PROPERTY =
        "com.sun.jdo.spi.persistence.support.sqlstore.USE_BATCH"; // NOI18N

    /**
     * Property to swich on/off batching. Note, the default is true, meaning we
     * try to do batching if the property is not specified.
     */
    private static final boolean USE_BATCH = Boolean.valueOf(
        System.getProperty(USE_BATCH_PROPERTY, "true")).booleanValue(); // NOI18N

    /**
     * Construct a new SQLStateManager so that it locks or does not lock as
     * per whether or not it is used in a managed environment.
     */
    public SQLStateManager(PersistenceStore store, ClassDesc persistenceConfig) {
        this.store = store;
        this.persistenceConfig = persistenceConfig;

        if (EJBHelper.isManaged()) {
            this.lock = new NullSemaphore("SQLStateManager");  // NOI18N
        } else {
            this.lock = new SemaphoreImpl("SQLStateManager");  // NOI18N
        }
    }

    public synchronized void initialize(boolean persistentInDB) {
        boolean xactActive = persistenceManager.isActiveTransaction();
        boolean optimistic = persistenceManager.isOptimisticTransaction();
        boolean nontransactionalRead = persistenceManager.isNontransactionalRead();
        LifeCycleState oldstate = state;

        if (state == null) {
            if (persistentInDB == false) {
                // Hollow object aquired by PM.getObjectByOid() does not require
                // to be persistent in DB
                state = LifeCycleState.getLifeCycleState(LifeCycleState.HOLLOW);
                persistenceManager.setFlags(persistentObject, LOAD_REQUIRED);
            } else {
                if (xactActive && !optimistic) {
                    state = LifeCycleState.getLifeCycleState(LifeCycleState.P_CLEAN);
                    persistenceManager.setFlags(persistentObject, READ_OK);
                } else {
                    state = LifeCycleState.getLifeCycleState(LifeCycleState.P_NON_TX);
                    persistenceManager.setFlags(persistentObject, LOAD_REQUIRED);
                }
                valid = true;
            }
        } else if (state.needMerge()) {
            state = state.transitionReadField(optimistic, nontransactionalRead, xactActive);

            // If we are in a state that requires the instance to be reloaded
            // we need to set the jdoFlags to LOAD_REQUIRED to enable field mediation.
            if (state.needsReload(optimistic, nontransactionalRead, xactActive)) {
                persistenceManager.setFlags(persistentObject, LOAD_REQUIRED);
            } else {
                if (persistenceManager.getFlags(persistentObject) == LOAD_REQUIRED) {
                    persistenceManager.setFlags(persistentObject, READ_OK);
                }
            }
        }

        registerInstance(false, null, oldstate);
    }

    private void registerInstance(boolean throwDuplicateException,
        ArrayList newlyRegisteredSMs, LifeCycleState oldstate) {

        if ((stateFlags & ST_REGISTERED) == 0 || // not registered or
            (oldstate != state &&  // state changed from clean to dirty or transactional type.
                (oldstate == null || oldstate.isDirty() != state.isDirty() ||
                    oldstate.isTransactional() != state.isTransactional()))) {

            persistenceManager.registerInstance(this, getObjectId(), throwDuplicateException, false);
            stateFlags |= ST_REGISTERED;
            if (newlyRegisteredSMs != null) {
                if (!newlyRegisteredSMs.contains(this))
                    newlyRegisteredSMs.add(this);
            }
        }
    }

    public void setPersistenceManager(com.sun.jdo.api.persistence.support.PersistenceManager pm) {
        this.persistenceManager = (PersistenceManager) pm;
    }

    public void setPersistent(Object pc) {
        this.persistentObject = pc;
    }

    public PersistenceStore getStore() {
        return store;
    }

    public Object getPersistent() {
        return persistentObject;
    }

    public PersistenceConfig getPersistenceConfig() {
        return persistenceConfig;
    }

    private UpdateObjectDescImpl getUpdateDesc() {
        if (updateDesc == null) {
            updateDesc = (UpdateObjectDescImpl) store.getUpdateObjectDesc(
                    persistenceConfig.getPersistenceCapableClass());
        }

        if (updateDesc.getConcurrency() == null) {
            boolean optimistic = persistenceManager.isOptimisticTransaction();
            updateDesc.setConcurrency(persistenceConfig.getConcurrency(optimistic));
        }

        return updateDesc;
    }

    private void unsetMaskBit(int index, int mask) {
        if (fieldMasks == null) {
            newFieldMasks();
        } else {
            if (index >= 0) {
                fieldMasks.clear(index + mask * persistenceConfig.maxFields);
            } else {
                fieldMasks.clear(-(index + 1) + persistenceConfig.maxVisibleFields +
                        mask * persistenceConfig.maxFields);
            }
        }
    }

    private void clearMask(int mask) {
        if (fieldMasks != null) {
            fieldMasks.clear(mask * persistenceConfig.maxFields,
                    (mask+1) * persistenceConfig.maxFields);
        }
    }

    private void setVisibleMaskBits(int mask) {
        if (fieldMasks == null) {
            newFieldMasks();
        }

        int offset = mask * persistenceConfig.maxFields;
        fieldMasks.set(offset, offset + persistenceConfig.maxVisibleFields);
    }

    private BitSet getVisibleMaskBits(int mask) {
        if (fieldMasks == null) {
            newFieldMasks();
        }

        int offset = mask * persistenceConfig.maxFields;
        return fieldMasks.get(offset, offset + persistenceConfig.maxVisibleFields);
    }

    private void newFieldMasks() {
        this.fieldMasks = new BitSet(MAX_MASKS * persistenceConfig.maxFields);
    }

    public void setPresenceMaskBit(int index) {
        if (fieldMasks == null) {
            newFieldMasks();
        }

        if (index >= 0) {
            fieldMasks.set(index + PRESENCE_MASK * persistenceConfig.maxFields);
        } else {
            fieldMasks.set(-(index + 1) + persistenceConfig.maxVisibleFields +
                    PRESENCE_MASK * persistenceConfig.maxFields);
        }
    }

    private void setSetMaskBit(int index) {
        if (fieldMasks == null) {
            newFieldMasks();
        }

        if (index >= 0) {
            fieldMasks.set(index + SET_MASK * persistenceConfig.maxFields);
        } else {
            fieldMasks.set(-(index + 1) + persistenceConfig.maxVisibleFields +
                    SET_MASK * persistenceConfig.maxFields);
        }
    }

    public boolean getPresenceMaskBit(int index) {
        if (fieldMasks == null) {
            newFieldMasks();
        }

        if (index >= 0) {
            return fieldMasks.get(index + PRESENCE_MASK * persistenceConfig.maxFields);
        } else {
            return fieldMasks.get(-(index + 1) + persistenceConfig.maxVisibleFields +
                    PRESENCE_MASK * persistenceConfig.maxFields);
        }
    }

    public boolean getSetMaskBit(int index) {
        if (fieldMasks == null) {
            newFieldMasks();
        }

        if (index >= 0) {
            return fieldMasks.get(index + SET_MASK * persistenceConfig.maxFields);
        } else {
            return fieldMasks.get(-(index + 1) + persistenceConfig.maxVisibleFields +
                    SET_MASK * persistenceConfig.maxFields);
        }
    }

    public Object getHiddenValue(int index) {
        // This method expects index to be negative for hidden fields.
        if (index >= 0) {
            throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                    "core.statemanager.poshiddenindex", "" + index)); // NOI18N
        }

        int realIndex = -(index + 1);

        if ((hiddenValues != null) && (realIndex < hiddenValues.size())) {
            return hiddenValues.get(realIndex);
        }

        return null;
    }

    public void setHiddenValue(int index, Object value) {
        // This method expects index to be negative for hidden fields.
        if (index >= 0) {
            throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                    "core.statemanager.poshiddenindex", "" + index)); // NOI18N
        }

        int realIndex = -(index + 1);

        if (hiddenValues == null) {
            hiddenValues = new ArrayList();
        }

        for (int i = hiddenValues.size(); i <= realIndex; i++) {
            hiddenValues.add(null);
        }

        hiddenValues.set(realIndex, value);
    }

    public synchronized void replaceObjectField(String fieldName, Object o) {
        boolean debug = logger.isLoggable();

        if (debug) {
            Object[] items = new Object[] {fieldName, o.getClass().getName()};
            logger.fine("sqlstore.sqlstatemanager.replaceobjectfield", items); // NOI18N
        }

        FieldDesc fieldDesc = persistenceConfig.getField(fieldName);
        Object oldo = prepareSetField(fieldDesc, o);

        if ((oldo instanceof SCO) && oldo != o) {
            if (debug)
                logger.fine("sqlstore.sqlstatemanager.replaceobjectfield.unsetsco"); // NOI18N
            ((SCO) oldo).unsetOwner();
        }
    }

    public synchronized void makeDirty(String fieldName) {
        boolean debug = logger.isLoggable();

        if (debug) {
            logger.fine("sqlstore.sqlstatemanager.makedirty", fieldName); // NOI18N
        }

        FieldDesc fieldDesc = persistenceConfig.getField(fieldName);

        // Save current value: if it is SCO object we need to replace it with the
        // new value instead of new instance
        Object oldo = fieldDesc.getValue(this);

        prepareUpdateField(fieldDesc, null);

        // Now adjust SCO instance
        Object newo = fieldDesc.getValue(this);

        if ((newo instanceof SCO) && oldo != newo) {
            if (oldo instanceof SCOCollection) {
                if (debug) {
                    logger.fine("sqlstore.sqlstatemanager.makedirty.fixscocollection"); // NOI18N
                }

                ((SCOCollection) oldo).clearInternal();
                ((SCOCollection) oldo).addAllInternal((Collection) newo);
            }

            else if (oldo instanceof SCODate) {
                if (debug) {
                    logger.fine("sqlstore.sqlstatemanager.makedirty.fixscodate"); // NOI18N
                }

                long l = ((java.util.Date) newo).getTime();
                // Adjust nanoseconds if necessary:
                int n = 0;

                if (newo instanceof Timestamp) {
                    n = ((Timestamp) newo).getNanos();
                } else {
                    n = (int) ((l % 1000) * 1000000);
                }

                if (oldo instanceof SqlTimestamp) {
                    ((SCODate) oldo).setTimeInternal(l);
                    ((SqlTimestamp) oldo).setNanosInternal(n);

                } else if (newo instanceof Timestamp) {
                    ((SCODate) oldo).setTimeInternal(l + (n / 1000000));
                } else {
                    ((SCODate) oldo).setTimeInternal(l);
                }
            }

            updateTrackedFields(fieldDesc, oldo, null);
            fieldDesc.setValue(this, oldo);

            // disconnect temp SCO instance
            if (newo instanceof SCO)
                ((SCO) newo).unsetOwner();
        }
    }

    /**
     * This method is central to record changes to SCOCollections.
     */
    public void applyUpdates(String fieldName, SCOCollection c) {
        boolean debug = logger.isLoggable();

        if (debug) {
            logger.fine("sqlstore.sqlstatemanager.applyupdates", fieldName); // NOI18N
        }

        FieldDesc fieldDesc = persistenceConfig.getField(fieldName);
        if (fieldDesc instanceof ForeignFieldDesc) {
            ArrayList removed = new ArrayList(c.getRemoved());
            ArrayList added = new ArrayList(c.getAdded());

            // We reset the collection to clear the added and removed list before calling
            // processCollectionUpdates() which can throw an exception.
            c.reset();
            processCollectionUpdates((ForeignFieldDesc) fieldDesc, removed, added, null, true, false);
        }
        // else it is an ERROR?

        if (debug) {
            logger.fine("sqlstore.sqlstatemanager.applyupdates.exit"); // NOI18N
        }
    }

    public void makePresent(String fieldName, Object value) {
        boolean debug = logger.isLoggable();

        if (debug) {
            logger.fine("sqlstore.sqlstatemanager.makepresent", fieldName); // NOI18N
        }

        FieldDesc fieldDesc = persistenceConfig.getField(fieldName);
        fieldDesc.setValue(this, value);
        setPresenceMaskBit(fieldDesc.absoluteID);
    }

    public void setObjectId(Object objectId) {
        // RESOLVE: do we allow to replace existing?
        this.objectId = objectId;
    }

    public Object getObjectId() {
        // Note: PM.getObjectId() makes copy of the actual object id.
        if (objectId == null) {
            Class oidClass = persistenceConfig.getOidClass();
            Object oid = null;

            try {
                oid = oidClass.newInstance();
            } catch (Exception e) {
                throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                        "core.statemanager.cantnewoid", oidClass.getName()), e); // NOI18N
            }

            Field keyFields[] = persistenceConfig.getKeyFields();
            String keyFieldNames[] = persistenceConfig.getKeyFieldNames();
            for (int i = 0; i < keyFields.length; i++) {
                Field keyField = keyFields[i];
                try {
                    FieldDesc fd = persistenceConfig.getField(keyFieldNames[i]);

                    if (fd != null) {
                        keyField.set(oid, fd.getValue(this));
                    }

                } catch (IllegalAccessException e) {
                    throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                            "core.statemanager.cantsetkeyfield", keyField.getName()), e); // NOI18N
                }
            }
            objectId = oid;
        }

        return objectId;
    }

    private void makeAutoPersistent(Object pc) {
        persistenceManager.makePersistent(pc);
        SQLStateManager sm = (SQLStateManager) persistenceManager.getStateManager(pc);

        sm.state = LifeCycleState.getLifeCycleState(LifeCycleState.AP_NEW);
    }

    /**
     * Prepares the associated object to be stored in the datastore.
     * This method is called by PersistenceManager.makePersistent().
     * Thread synchronization is done in the persistence manager.
     */
    public void makePersistent(PersistenceManager pm, Object pc) {

        boolean debug = logger.isLoggable();

        if (debug) {
            logger.fine("sqlstore.sqlstatemanager.makepersistence", // NOI18N
            persistenceConfig.getPersistenceCapableClass().getName());
        }

        // If the instance is autopersistent, we simply transition it to persistent_new.
        if (state != null) {
            if (state.isAutoPersistent()) {
                state = state.transitionMakePersistent();
            }
            return;
        }

        this.persistenceManager = pm;
        this.persistentObject = pc;

        // Mark all the visible fields as present to prevent navigation and
        // to allow us to create a before image that contains all the fields.
        setVisibleMaskBits(PRESENCE_MASK);
        getBeforeImage();

        state = LifeCycleState.getLifeCycleState(LifeCycleState.P_NEW);

        try {
            registerInstance(true, null, null);
        } catch (JDOException e) {
            this.release();

            throw e;
        }

        // We set the statemanager for the pc now so the instance is considered
        // persistent. We need to do this in order for persistent-by-reachability to
        // work properly in the case of self-referencing relationship.
        pm.setStateManager(pc, this);
        valid = true;

        // Now that the state manager has been set in the pc, we need to
        // synchronize it so other threads can't modify this instance while
        // we perform the persistence-by-reachability algorithm.
        try {
            getLock();

            // Make sure all the fields have been marked dirty.
            Object obj = null;
            ArrayList fields = persistenceConfig.fields;
            for (int i = 0; i < fields.size(); i++) {
                FieldDesc f = (FieldDesc) fields.get(i);

                // In case of makePersistent, we skip all secondary tracked fields
                // and use the primary to propagate changes. In addition, we take
                // the policy that a tracked relationship field takes precedence
                // over its primitive counterpart. In other words, we skip all
                // primitive fields that also tracks relationship fields.
                if ((f.sqlProperties & FieldDesc.PROP_SECONDARY_TRACKED_FIELD) > 0) {
                    continue;
                }

                obj = f.getValue(this);

                if (f instanceof ForeignFieldDesc) {
                    ForeignFieldDesc ff = (ForeignFieldDesc) f;
                    ArrayList trackedFields = null;

                    if (debug) {
                        logger.fine("sqlstore.sqlstatemanager.processforeign", ff.getName()); // NOI18N
                    }

                    if ((ff.sqlProperties & FieldDesc.PROP_PRIMARY_TRACKED_FIELD) > 0) {
                        trackedFields = ff.getTrackedFields();
                        Object theValue = obj;

                        for (int j = 0; j < trackedFields.size(); j++) {
                            FieldDesc tf = (FieldDesc) trackedFields.get(j);
                            Object value = tf.getValue(this);

                            if ((theValue != null) && (value != null) && (theValue != value)) {
                                if (needsVerifyAtDeregister) {
                                    persistenceManager.deregisterInstance(getObjectId(), this);
                                    needsVerifyAtDeregister = false;
                                } else {
                                    persistenceManager.deregisterInstance(getObjectId());
                                }
                                this.release();
                                throw new JDOUserException(I18NHelper.getMessage(messages,
                                        "core.statemanager.conflictingvalues", ff.getName(), tf.getName())); // NOI18N
                            } else if ((theValue == null) && (value != null)) {
                                theValue = value;
                            }
                        }

                        if (theValue != obj) {
                            obj = theValue;
                            ff.setValue(this, obj);
                        }
                    }

                    if (obj != null) {
                        if (obj instanceof Collection) {
                            if (((Collection) obj).size() > 0) {
                                ArrayList removed = null;
                                ArrayList added = new ArrayList((Collection) obj);
                                processCollectionUpdates(ff, removed, added, null, true, false);
                            }
                        } else {
                            // null out this field to pretend we are setting this field for the first time
                            ff.setValue(this, null);

                            updateObjectField(ff, obj, true, false);

                            // now restore the value
                            ff.setValue(this, obj);
                        }
                    } else {
                        // For a null managed collection relationship field, we replace it
                        // with an empty SCOCollection
                        if ((ff.getInverseRelationshipField() != null) && (ff.cardinalityUPB > 1)) {
                            replaceCollection(ff, null);
                        }
                    }

                    updateTrackedFields(ff, ff.getValue(this), null);
                } else {
                    // We ignore primitive fields that also tracks relationship field
                    if ((f.sqlProperties & FieldDesc.PROP_TRACK_RELATIONSHIP_FIELD) > 0) {
                        ArrayList trackedFields = f.getTrackedFields();
                        boolean found = false;

                        for (int j = trackedFields.size() - 1; j >= 0; j--) {
                            FieldDesc tf = (FieldDesc) trackedFields.get(j);

                            if (tf instanceof ForeignFieldDesc) {
                                if (tf.getValue(this) != null) {
                                    found = true;
                                    break;
                                }
                            } else {
                                break;
                            }
                        }

                        if (!found) {
                            //f.setValue(this, null);
                            updateTrackedFields(f, obj, null);
                            //f.setValue(this, obj);
                        }
                    } else {
                        updateTrackedFields(f, obj, null);
                    }

                    if ((f.sqlProperties & FieldDesc.PROP_RECORD_ON_UPDATE) > 0) {
                        getUpdateDesc().recordUpdatedField((LocalFieldDesc) f);
                    }
                }

                if (debug) {
                    logger.fine("sqlstore.sqlstatemanager.makedirtyfield", f.getName()); // NOI18N
                }

                setSetMaskBit(f.absoluteID);
            }
        } finally {
            releaseLock();
        }
    }

    /**
     * Prepares the associated object for delete. This method is
     * called by PersistenceManager.deletePersistent(). After
     * nullifying the relationship fields, the instance transitions to
     * deleted state.
     */
    public void deletePersistent() {
        if (logger.isLoggable()) {
             logger.fine("sqlstore.sqlstatemanager.deletepersistence", // NOI18N
             persistenceConfig.getPersistenceCapableClass().getName());

        }

        // Why try try?  The difference is in whether you try and then
        // acquire/get, or acquire/get and then try.
        // Prior to having acquireFieldUpdateLock, this code synchronized on
        // a field, following that with the try for {get,release}Lock.  That
        // pattern of usage calls for that order.
        // {acquire,release}FieldUpdateLock calls for the other order.  We
        // are close to FCS, so this strange situation persists for now
        // (i.e., it ain't broken).

        persistenceManager.acquireFieldUpdateLock();
        try {
            try {
                getLock();

                if (state.isDeleted()) {
                    return;
                }

                deleteRelationships();

                LifeCycleState oldstate = state;
                state = state.transitionDeletePersistent();
                persistenceManager.setFlags(persistentObject, LOAD_REQUIRED);
                registerInstance(false, null, oldstate);
            } finally {
                releaseLock();
            }
        } finally {
            persistenceManager.releaseFieldUpdateLock();
        }
    }

    /**
     * Prepares the current instance for delete by nullifying all
     * relationships. The deletion is propagated to relationship
     * fields marked for cascade delete.
     */
    private void deleteRelationships() {
        ArrayList foreignFields = persistenceConfig.foreignFields;
        int size = foreignFields.size();
        stateFlags |= ST_DELETE_INPROGRESS;

        for (int i = 0; i < size; i++) {
            ForeignFieldDesc ff = (ForeignFieldDesc) foreignFields.get(i);
            ForeignFieldDesc irf = ff.getInverseRelationshipField();

            // Skip this field if it is secondary.
            if ((ff.sqlProperties & FieldDesc.PROP_SECONDARY_TRACKED_FIELD) > 0) {
                continue;
            }

            // Skip this field if it is not managed nor marked for cascade delete.
            if ((ff.deleteAction != ForeignFieldDesc.ACT_CASCADE) && (irf == null)) {
                continue;
            }

            prepareUpdateField(ff, null);

            if (ff.cardinalityUPB > 1) {
                Collection c = (Collection) ff.getValue(this);

                if (c != null) {
                    ArrayList removed = new ArrayList(c);

                    // For managed relationship or cascade delete, we need to call
                    // processCollectionUpdates() to set up the dependency. In case of
                    // managed relationship, the inverse relationship field should also
                    // be set to null.
                    processCollectionUpdates(ff, removed, null, null, true, false);

                    if (c instanceof SCOCollection) {
                        ((SCOCollection) c).clearInternal();
                    } else {
                        c.clear();
                    }

                    if (ff.deleteAction == ForeignFieldDesc.ACT_CASCADE) {
                        Iterator iter = removed.iterator();

                        while (iter.hasNext()) {
                            Object obj = iter.next();

                            if (obj != null) {
                                SQLStateManager sm = (SQLStateManager)
                                        persistenceManager.getStateManager(obj);

                                // Ignore if this sm is in the process of being cascade
                                // deleted. This is to prevent infinite recursive in case
                                // of self-referencing relationship.
                                if ((sm != null) && !sm.isDeleted() &&
                                        ((sm.stateFlags & ST_DELETE_INPROGRESS) == 0)) {
                                    try {
                                        persistenceManager.deletePersistent(obj);
                                    } catch (Throwable e) {
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Object obj = ff.getValue(this);

                if (obj != null) {
                    updateObjectField(ff, null, true, false);
                    ff.setValue(this, null);

                    if (ff.deleteAction == ForeignFieldDesc.ACT_CASCADE) {
                        SQLStateManager sm = (SQLStateManager)
                                persistenceManager.getStateManager(obj);

                        // Ignore if this sm is in the process of being cascade
                        // deleted. This is to prevent infinite recursive in case
                        // of self-referencing relationships.
                        if ((sm != null) && !sm.isDeleted() &&
                                ((sm.stateFlags & ST_DELETE_INPROGRESS) == 0)) {
                            try {
                                persistenceManager.deletePersistent(obj);
                            } catch (Throwable e) {
                            }
                        }
                    }
                }
            }
        }

        stateFlags &= ~ST_DELETE_INPROGRESS;
    }

    /**
     * Stores the associated object in the datastore. This method is
     * called by {@link PersistenceManager#beforeCompletion} on
     * flush/commit.  The specified state manager argument is used to
     * determine whether the actual instance should be flushed
     * immediately or whether batch update is possible.
     *
     * @param next Next state manager in the transaction cache.
     */
    public void updatePersistent(StateManager next) {
        boolean debug = logger.isLoggable();

        if ((stateFlags & ST_UPDATE_DISABLED) > 0) {
            if (debug) {
                Object[] items = new Object[] {persistenceConfig.getPersistenceCapableClass().getName(),
                                               persistentObject};
                logger.fine("sqlstore.sqlstatemanager.updatepersistent.skipped", items); // NOI18N
            }
            return;
        }

        try {
            if (debug) {
                 logger.fine("sqlstore.sqlstatemanager.updatepersistent", // NOI18N
                 persistenceConfig.getPersistenceCapableClass().getName());
            }

            ArrayList actions = new ArrayList();

            // Get a list of actions to perform.
            getUpdateActions(actions);

            if (actions.size() == 1 && useBatch()) {
                // Batch update only if actions consists of a single action
                UpdateObjectDesc updateDesc = (UpdateObjectDesc)actions.get(0);
                boolean immediateFlush = requiresImmediateFlush((SQLStateManager)next);

                if (debug && immediateFlush) {
                    Object[] items = new Object[] {getPersistent(), (next != null) ? next.getPersistent() : null};
                    logger.fine("sqlstore.sqlstatemanager.updatepersistent.immediateflush", items); // NOI18N
                }

                store.executeBatch(persistenceManager, updateDesc, immediateFlush);
            } else if (actions.size() > 0) {
                store.execute(persistenceManager, actions);
            }

            incrementVersion(actions);

            if (debug) {
                logger.fine("sqlstore.sqlstatemanager.updatepersistent.exit"); // NOI18N
            }
        } catch (JDOException e) {
            e.addFailedObject(persistentObject);
            throw e;
        } catch (Exception e) {
            logger.throwing("sqlstore.SQLStateManager", "updatePersistent", e); // NOI18N
            throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                    "core.generic.unknownexception"), e); // NOI18N
        }
    }

    /**
     * Increments the version for all state managers in
     * <code>actions</code> registered for version consistency.
     *
     * @param actions List of updated state managers.
     */
    static private void incrementVersion(List actions) {

        for (Iterator iter = actions.iterator(); iter.hasNext(); ) {
            ((UpdateObjectDescImpl) iter.next()).incrementVersion();
        }
    }

    /**
     * Increments the version fields for this state manager. Instances
     * mapped to multiple tables have got a version field for each table.
     */
    public void incrementVersion() {
        LocalFieldDesc [] versionFields = persistenceConfig.getVersionFields();

        for (int i = 0; i < versionFields.length; i++) {
            versionFields[i].incrementValue(this);
        }
    }

    /**
     * @inheritDoc StateManager#hasVersionConsistency
     */
    public boolean hasVersionConsistency() {
        return persistenceConfig.hasVersionConsistency();
    }

    /**
     * @inheritDoc StateManager#verifyPersistent
     */
    public boolean verifyPersistent() {
        assert persistenceConfig.hasVersionConsistency();
        boolean verified = true;

        if (state instanceof PersistentClean) {
            RetrieveDesc verificationRD = persistenceConfig.getRetrieveDescForVerificationQuery(store);
            LocalFieldDesc[] keyFields = persistenceConfig.getKeyFieldDescs();
            LocalFieldDesc[] versionFields = persistenceConfig.getVersionFields();

            // Please make sure that the order of parameter values is same as
            // order of parameters as defined by ClassDesc#getRetrieveDescForVerificationQuery()
            Object [] parameters = new Object[keyFields.length + versionFields.length];
            copyValues(parameters, keyFields, 0);
            copyValues(parameters, versionFields, keyFields.length);

            // verificationRD requires parameters for pk field and version fields
            Boolean result = (Boolean) store.
                    retrieve(persistenceManager, verificationRD, new QueryValueFetcher(parameters));
            verified = result.booleanValue();
        }
        return verified;
    }

    /**
     * Returns true if batch update might be used to store the changes
     * of this state manager.
     *
     * TODO: Because batched statements on Oracle don't return a
     * valid success indicator, batching is disabled for Version
     * Consistency.
     */
    private boolean useBatch() {
        boolean result = false;

        if (USE_BATCH) {
            switch(state.getUpdateAction()) {

            case ActionDesc.LOG_CREATE:
                result = !getUpdateDesc().hasChangedRelationships() &&
                         !getUpdateDesc().hasModifiedLobField();
                break;
            case ActionDesc.LOG_DESTROY:
            case ActionDesc.LOG_UPDATE:
                // Do not try to batch in optimitic tx for now. We need to
                // check for parallel updates, so the WHERE clause checks
                // the values from the beforeImage. We need a different SQL
                // statements for the null vs. non null case.
                result = !persistenceManager.isOptimisticTransaction() &&
                         !persistenceConfig.hasModifiedCheckAtCommitConsistency() &&
                         !getUpdateDesc().hasChangedRelationships() &&
                         !getUpdateDesc().hasModifiedLobField() &&
                         !hasVersionConsistency();
                break;
            default:
                result = false;
                break;
            }
        }

        return result;
    }

    /**
     * @inheritDoc StateManager#setVerificationFailed
     */
    public void setVerificationFailed() {
        if (hasVersionConsistency()) {
            stateFlags |= ST_VALIDATION_FAILED;
        }
    }

    /**
     * @inheritDoc StateManager#getFailed
     */
    public boolean isVerificationFailed() {
        return (stateFlags & ST_VALIDATION_FAILED) > 0;
    }

    /**
     * This method checks whether this StateManager instance needs to be
     * flushed immediately during beforeCompletion. A return of <code>false</code>
     * means the store manager is allowed to combine flushing of these two
     * instance in a single database roundtrip (e.g. by using batched updates).
     *
     * @param next Next state manager to be flushed.
     */
    private boolean requiresImmediateFlush(SQLStateManager next) {
        // There is no next SM =>
        // flush this sm immediately
        if (next == null)
            return true;

        // The next StateManager has a different pc class =>
        // flush this sm immediately
        if (persistenceConfig != next.persistenceConfig)
            return true;

        // The next StateManager represents a different update operation
        // INSERT/UPDATE/DELETE => flush this sm immediately
        if (state.getUpdateAction() != next.state.getUpdateAction())
            return true;

        // If the next's flush is disabled, flush this sm
        if ((next.stateFlags & ST_UPDATE_DISABLED) > 0)
            return true;

        // If next sm does not use batch update flush this sm
        if (!next.useBatch())
            return true;

        // For updates, we need to check if the next sm updates the
        // same fields. If not, flush this sm
        if (getUpdateDesc().getUpdateAction() == ActionDesc.LOG_UPDATE &&
                !compareUpdatedFields(next)) {
            return true;
        }

        // If the next stateManager has got (foreign reference) dependencies,
        // it is not considered for batching at all
        if (next.updatedForeignReferences != null) {
            return true;
        }

        // Now we can make use of batching w/o flushing
        return false;
    }

    private boolean compareUpdatedFields(SQLStateManager next) {
        BitSet updFields = getVisibleMaskBits(SET_MASK);
        BitSet nextUpdFields = (next != null) ? next.getVisibleMaskBits(SET_MASK) : null;

        return updFields.equals(nextUpdFields);
    }

    public void refreshPersistent() {
        boolean debug = logger.isLoggable();

        if (debug) {
            logger.fine("sqlstore.sqlstatemanager.refreshpersistent", // NOI18N
            persistenceConfig.getPersistenceCapableClass().getName());
        }

        // Only refresh if the state allows it.
        if (state.isRefreshable()) {
            LifeCycleState oldstate = state;
            state = state.transitionRefreshPersistent();
            reload(null);
            registerInstance(false, null, oldstate);
        }

        if (debug) {
            logger.fine("sqlstore.sqlstatemanager.refreshpersistent.exit"); // NOI18N
        }
    }

    /**
     * Reloads the instance by delegating actual work, state transition, and
     * instance registration to {@link #reload(FieldDesc) reload(FieldDesc)}
     * With <code>null</code> as an argument. Called by
     * {@link PersistenceManager#getObjectById(Object, boolean)
     * PersistenceManager.getObjectById(Object, boolean)} with validate
     * flag set to <code>true</code>
     */
    public void reload() {
        boolean debug = logger.isLoggable(Logger.FINER);

        if (debug) {
            logger.finer("sqlstore.sqlstatemanager.unconditionalreload", // NOI18N
            persistenceConfig.getPersistenceCapableClass().getName());
        }

        persistenceManager.acquireShareLock();

        try {
            getLock();

            reload(null);

        } finally {
            persistenceManager.releaseShareLock();
            releaseLock();

            if (debug) {
                logger.finer("sqlstore.sqlstatemanager.unconditionalreload.exit"); // NOI18N
            }
        }
    }

    /**
     * Reloads this SM from the state in the datastore, getting data for the
     * given field.
     * @param additionalField Field to be loaded.
     */
    private void reload(FieldDesc additionalField) {
        boolean debug = logger.isLoggable();

        if (debug) {
            String fieldName =
                (additionalField != null) ? additionalField.getName() : null;
            logger.fine("sqlstore.sqlstatemanager.reload", // NOI18N
            persistenceConfig.getPersistenceCapableClass().getName(), fieldName);
        }

        // Clear the fields PresenceMask so all the currently present
        // fields will be replaced.
        clearMask(PRESENCE_MASK);

        // Need to mark the key fields as present
        markKeyFieldsPresent();

        clearMask(SET_MASK);

        LifeCycleState oldState = state;
        state = state.transitionReload(persistenceManager.isActiveTransaction());

        if (!retrieveFromVersionConsistencyCache(additionalField)) {
            // Retrieve the instance from the data store, if this class
            // is not version consistent, or not found in the cache.

            try {
                retrieve(additionalField);
            } catch (JDOException e) {
                // Reset the state if the instance couldn't be found.
                state = oldState;
                throw e;
            }
        }
        registerInstance(false, null, oldState);

        if (persistenceManager.getFlags(persistentObject) == LOAD_REQUIRED) {
            persistenceManager.setFlags(persistentObject, READ_OK);
        }

        if (debug) {
            logger.fine("sqlstore.sqlstatemanager.reload.exit"); // NOI18N
        }
    }

    /**
     * Initialize this SM from the version consistency cache. If this
     * SM is in the cache and the additional field is not populated,
     * the field is retrieved from the store.
     * @param additionalField Field to be loaded.
     */
    private boolean retrieveFromVersionConsistencyCache(FieldDesc additionalField) {
        boolean rc =
            persistenceManager.initializeFromVersionConsistencyCache(this);

        if (rc) {

            // make sure additionalField is available
            if (additionalField != null
                    && !getPresenceMaskBit(additionalField.absoluteID)) {

                realizeField(additionalField);
            }
        }
        return rc;
    }

    /**
     * PersistenceManager calls this method to prepare a persistent
     * object for update. This is required for foreign fields only
     * as they could reference "regular" JDK Collections vs. SCO
     * Collections. Such process has the side-effect of causing more
     * objects to be registered with the transaction cache.
     */
    public void prepareToUpdatePhaseI() {
        boolean debug = logger.isLoggable();

        if (debug) {
            logger.fine("sqlstore.sqlstatemanager.preparetoupdateph1", // NOI18N
            persistenceConfig.getPersistenceCapableClass().getName());
        }

        int action = state.getUpdateAction();

        if (action == ActionDesc.LOG_NOOP || action == ActionDesc.LOG_DESTROY) {
            // Nothing extra to do
            return;
        }

        // Initialize UpdateDesc.
        getUpdateDesc();

        ArrayList newlyRegisteredSMs = new ArrayList();
        ArrayList foreignFields = persistenceConfig.foreignFields;
        int size = foreignFields.size();

        for (int i = 0; i < size; i++) {
            ForeignFieldDesc ff = (ForeignFieldDesc) foreignFields.get(i);

            if ((ff.sqlProperties & FieldDesc.PROP_SECONDARY_TRACKED_FIELD) > 0) {
                continue;
            }

            if ((ff.cardinalityUPB > 1) && (getSetMaskBit(ff.absoluteID) == true)) {
                Collection v = (Collection) ff.getValue(this);

                if ((v != null) && (!(v instanceof SCO) || (((SCO) v).getOwner() == null)) &&
                        (v.size() > 0)) {
                    ArrayList removed = null;
                    ArrayList added = new ArrayList(v);

                    processCollectionUpdates(ff, removed, added, newlyRegisteredSMs, true, false);
                }
            }
        }

        // The newRegisteredSMs should contain a list of all the state managers that
        // are registered as the result of processCollectionUpdates.
        for (int i = 0; i < newlyRegisteredSMs.size(); i++) {
            SQLStateManager sm = (SQLStateManager) newlyRegisteredSMs.get(i);

            sm.prepareToUpdatePhaseI();
        }

        if (debug) {
            logger.fine("sqlstore.sqlstatemanager.preparetoupdateph1.exit"); // NOI18N
        }
    }

    /**
     * This is the second phase of the commit processing. It populates phase3sms with all
     * the autopersistent instances that are no longer reachable from a persistent instance.
     *
     * @param phase3sms List containing autopersistent instances that are no longer reachable
     *  from a persistent instance.
     */
    public void prepareToUpdatePhaseII(HashSet phase3sms) {
        boolean debug = logger.isLoggable();

        if (debug) {
             logger.fine("sqlstore.sqlstatemanager.preparetoupdateph2", // NOI18N
             persistenceConfig.getPersistenceCapableClass().getName());
        }

        // If this instance is autopersistent, we transition it into a pending state and
        // add it to phase3sms collection. Any instance in phase3sms collection may be removed
        // later if it becomes persistent.
        if (state.isAutoPersistent()) {
            state = state.transitionMakePending();
            phase3sms.add(this);
            return;
        }

        if ((stateFlags & ST_PREPARED_PHASE_II) > 0) {
            return;
        }

        stateFlags |= ST_PREPARED_PHASE_II;

        if ((!state.isNew() && !state.isDirty()) || state.isDeleted()) {
            return;
        }

        ArrayList foreignFields = persistenceConfig.foreignFields;
        int size = foreignFields.size();

        // Walk the object graph starting from this instance and transition all
        // autopersistent instances to persistent and remove it from phase3sms.
        for (int i = 0; i < size; i++) {
            ForeignFieldDesc ff = (ForeignFieldDesc) foreignFields.get(i);

            if (ff.cardinalityUPB <= 1) {
                if (getPresenceMaskBit(ff.absoluteID)) {
                    Object v = ff.getValue(this);

                    if (v != null) {
                        transitionPersistent(v, phase3sms);
                    }
                }
            } else {
                Collection c = getCollectionValue(ff);

                if (c != null) {
                    Iterator iter = c.iterator();

                    while (iter.hasNext()) {
                        Object v = iter.next();

                        transitionPersistent(v, phase3sms);
                    }
                }
            }
        }

        if (debug) {
            logger.fine("sqlstore.sqlstatemanager.preparetoupdateph2.exit"); // NOI18N
        }
    }

    /**
     * This is the third phase of commit processing. It sets up the delete dependencies among
     * all the autopersistent instances that have been flushed to the database.
     */
    public void prepareToUpdatePhaseIII() {
        boolean debug = logger.isLoggable();

        if (debug) {
            logger.fine("sqlstore.sqlstatemanager.preparetoupdateph3", // NOI18N
            persistenceConfig.getPersistenceCapableClass().getName());
        }

        if (!state.isPersistentInDataStore()) {
            // This object will not be written to the store. But we need to
            // make sure, that scheduled jointable entries aren't written either.
            // See UpdateQueryPlan#processJoinTables().
            if (updateDesc != null) {
                updateDesc.clearUpdatedJoinTableRelationships();
            }

            // Finished for this instance.
            return;
        }

        ArrayList foreignFields = persistenceConfig.foreignFields;
        int size = foreignFields.size();

        // Sets up dependencies between this instance and all its relationship fields
        // that are autopersistent.
        for (int i = 0; i < size; i++) {
            ForeignFieldDesc ff = (ForeignFieldDesc) foreignFields.get(i);

            if (ff.cardinalityUPB <= 1) {
                if (getPresenceMaskBit(ff.absoluteID)) {
                    Object v = ff.getValue(this);

                    if (v != null) {
                        updateObjectField(ff, null, false, false);
                    }
                }
            } else {
                Collection c = getCollectionValue(ff);

                if (c != null) {
                    if (c.size() > 0) {
                        ArrayList removed = new ArrayList(c);
                        ArrayList added = null;

                        processCollectionUpdates(ff, removed, added, null, false, false);
                    }
                }
            }
        }

        if (debug) {
            logger.fine("sqlstore.sqlstatemanager.preparetoupdateph3.exit"); // NOI18N
        }
    }

    /**
     * Transitions the instance <code>pc</code> to persistent state if it's
     * autopersistent, and removes its state manager from the list
     * <code>phase3sms</code> of unreachable autopersistent
     * instances. The recursive call to <code>prepareToUpdatePhaseII</code>
     * removes the transitive closure of all state managers reachable from
     * <code>pc</code> from <code>phase3sms</code>. This method has got no
     * effects on transient instances.
     *
     * @param pc Instance becoming persistent and removed from <code>phase3sms</code>.
     * @param phase3sms List containing so far unreachable autopersistent instances.
     */
    private void transitionPersistent(Object pc, HashSet phase3sms) {
        SQLStateManager sm = (SQLStateManager) persistenceManager.getStateManager(pc);

        // Need to check if the associated state manager is null, if
        // called with an object from a collection relationship field. If
        // the collection is not a SCO collection, it is possible that it
        // contains transient instances. No need to check for object
        // relationship fields.
        if (sm != null && sm.state.isAutoPersistent()) {
            sm.state = sm.state.transitionMakePersistent();
            phase3sms.remove(sm);
            sm.prepareToUpdatePhaseII(phase3sms);
        }
    }

    /**
     * Returns the value of the collection relationship field
     * <code>ff</code>.  For deferred SCOCollections, only the
     * objects added in the current transaction are returned.
     * This method may only be called for Collection fields!
     *
     * @param ff Collection relationship field.
     * @return The value of the collection relationship field
     * <code>ff</code>. For deferred SCOCollections, only the
     * objects added in the current transaction are returned.
     */
    private Collection getCollectionValue(ForeignFieldDesc ff) {
        Collection c = null;
        if (ff.cardinalityUPB > 1) {
            c = (Collection) ff.getValue(this);
            if (c != null && c instanceof SCOCollection) {
                SCOCollection sco = (SCOCollection) c;
                if (sco.isDeferred()) {
                    c = sco.getAdded();
                }
            }
        }
        return c;
    }

    private void getUpdateActions(ArrayList actions) {
        if ((stateFlags & ST_VISITED) > 0) {
            return;
        }

        int action = state.getUpdateAction();

        if ((action == ActionDesc.LOG_NOOP) && (updateDesc == null)) {
            return;
        }

        // Initialize updateDesc.
        getUpdateDesc();

        updateDesc.setObjectInfo(getBeforeImage(), this, action);

        if ((action == ActionDesc.LOG_DESTROY) || (action == ActionDesc.LOG_CREATE) ||
                updateDesc.hasUpdatedFields() || updateDesc.hasUpdatedJoinTableRelationships()) {
            actions.add(updateDesc);
        }

        stateFlags |= ST_VISITED;

        if (updatedForeignReferences != null) {
            Iterator iter = updatedForeignReferences.iterator();

            while (iter.hasNext()) {
                SQLStateManager sm = ((UpdatedForeignReference) iter.next()).getStateManager();

                if (sm.referenceCount == 1) {
                    sm.getUpdateActions(actions);
                }

                sm.referenceCount--;
            }
        }
    }

    public void release() {
        if (null != persistenceManager) {

            // The persistenceManager can be null, for example if this
            // instance is used in the VersionConsistency cache.
            persistenceManager.setStateManager(persistentObject, null);
        }

        persistentObject = null;
        objectId = null;
        persistenceManager = null;
        beforeImage = null;
        hiddenValues = null;
        updatedForeignReferences = null;
        updateDesc = null;
        persistenceConfig = null;
        store = null;
        valid = false;
    }

    private void reset(boolean retainValues, boolean wasNew, boolean keepState) {
        boolean debug = logger.isLoggable();

        if (debug) {
             Object[] items = new Object[] {Boolean.valueOf(retainValues),
                                            Boolean.valueOf(wasNew), Boolean.valueOf(keepState)};
             logger.fine("sqlstore.sqlstatemanager.reset", items); // NOI18N

        }

        if (state == null) {
            // make the instance transient.

            if (!keepState) {
                persistenceManager.clearFields(persistentObject);
            }

            // Need to set jdoFlag to READ_WRITE_OK for transient instance.
            persistenceManager.setFlags(persistentObject, READ_WRITE_OK);

            if (needsVerifyAtDeregister) {
                persistenceManager.deregisterInstance(getObjectId(), this);
            } else {
                persistenceManager.deregisterInstance(getObjectId());
            }
            this.release();
        } else {
            // Reset the state manager for the next transaction.
            stateFlags = 0;
            beforeImage = null;
            updatedForeignReferences = null;
            referenceCount = 0;

            if (updateDesc != null) {
                updateDesc.reset();
            }

            // We retain the field values if retainValues is true or the state
            // is persistentNontransactional
            if (retainValues || (state instanceof PersistentNonTransactional)) {
                FieldDesc f = null;
                ArrayList fields = persistenceConfig.fields;
                for (int i = 0; i < fields.size(); i++) {
                    f = (FieldDesc) fields.get(i);
                    Object v = f.getValue(this);

                    // For new objects mark null references as not set if the field is
                    // not managed. This is to allow the field to be reloaded in the next
                    // transaction because a relationship may exist in the database.
                    if (wasNew && (f instanceof ForeignFieldDesc) &&
                            (v == null) && (((ForeignFieldDesc) f).getInverseRelationshipField() == null)) {
                        if (debug)
                            logger.fine("sqlstore.sqlstatemanager.unsetmask", f.getName()); // NOI18N
                        unsetMaskBit(f.absoluteID, PRESENCE_MASK);
                    }

                    // Replace java.util Collection and Date objects
                    // with SCO instances:
                    if ((v instanceof Collection) && !(v instanceof SCOCollection)
                            && !keepState) {
                        if (debug)
                            logger.fine("sqlstore.sqlstatemanager.resettingcollection"); // NOI18N

                        replaceCollection((ForeignFieldDesc) f, (Collection) v);

                        if (debug) {
                            logger.fine("sqlstore.sqlstatemanager.newtype", (f.getValue(this)).getClass()); // NOI18N
                        }
                    } else if (v instanceof SCOCollection) {
                        ((SCOCollection) v).reset();
                    }

                    // TO FIX!!!: this already replaces Date with SCO Date
                    else if ((v instanceof java.util.Date) && !(v instanceof SCODate)
                            && !keepState) {
                        if (debug)
                            logger.fine("sqlstore.sqlstatemanager.resettingdate"); // NOI18N

                        v = f.convertValue(v, this);
                        f.setValue(this, v);
                        if (debug) {
                            logger.fine("sqlstore.sqlstatemanager.newtype", (f.getValue(this)).getClass()); // NOI18N
                        }
                    }
                }

                // We need to set the jdoFlags to LOAD_REQUIRED instead of READ_OK in order to
                // have the StateManager intermediate access to this instance. The reason is
                // if the next transaction is pessimistic, the StateManager needs to reload
                // the instance. Note that the StateManager will not reload the instance
                // if the transaction is optimistic and the instance is p_nontransactional.
                persistenceManager.setFlags(persistentObject, LOAD_REQUIRED);
            } else {
                clearMask(PRESENCE_MASK);
                persistenceManager.clearFields(persistentObject);

                // Need to mark the key fields as present
                markKeyFieldsPresent();

                persistenceManager.setFlags(persistentObject, LOAD_REQUIRED);
            }

            clearMask(SET_MASK);
            isReplacementInstance = false;
            needsRegisterAtRollback = false;
            needsVerifyAtDeregister = false;
        }
    }

    /**
     * @return true if persistentObject has been flushed to db
     */
    public boolean isProcessed() {
        return (referenceCount == 0);
    }

    public void flushed() {
        // reset the current state to the point where we can accept more updates.
        state = state.transitionFlushed();

        clearMask(SET_MASK);

        stateFlags &= ~ST_VISITED;
        stateFlags &= ~ST_UPDATE_DISABLED;
        stateFlags &= ~ST_REGISTERED;

        // need to set the jdoFlags to LOAD_REQUIRED to allow updated to be tracked.
        persistenceManager.setFlags(persistentObject, LOAD_REQUIRED);

        if (updatedForeignReferences != null) {
            updatedForeignReferences.clear();
        }

        if (updateDesc != null) {
            updateDesc.reset();
        }
    }

    public void commit(boolean retainValues) {
        boolean wasNew = (state.isNew() && !state.isDeleted());
        state = state.transitionCommit(retainValues);
        reset(retainValues, wasNew, false);
    }

    public void rollback(boolean retainValues) {
        boolean wasNew = (state.isNew() && !state.isDeleted());
        boolean needsRestore = state.needsRestoreOnRollback(retainValues);
        state = state.transitionRollback(retainValues);
        boolean keepState = needsRestore;

        // Only restore if there is a before image and the flag needsRestore is true.
        if ((beforeImage != null) && (needsRestore == true)) {
            copyFields(beforeImage, true, false);

            // Keep the fields from being reset in reset()
            keepState = true;
        }

        if (needsRegisterAtRollback && !isReplacementInstance) {
            persistenceManager.registerInstance(this, getObjectId());
        }
        reset(retainValues, wasNew, keepState);
    }

    private void markKeyFieldsPresent() {
        ArrayList keyFields = persistenceConfig.getPrimaryTable().getKey().getFields();

        for (int i = 0; i < keyFields.size(); i++) {
            LocalFieldDesc fd = (LocalFieldDesc) keyFields.get(i);

            if (fd != null) {
                setPresenceMaskBit(fd.absoluteID);
            }
        }
    }

    public void prepareGetField(int fieldID) {
        FieldDesc fieldDesc = persistenceConfig.getField(fieldID);

        prepareGetField(fieldDesc, false, true);
    }

    private void prepareGetField(FieldDesc fieldDesc) {
        prepareGetField(fieldDesc, true, false);
    }

    /**
     * Loads the field described by <code>fieldDesc</code>. If the field is not
     * present in the instance, it will be loaded from the data store by calling
     * {@link #realizeField(FieldDesc)}. Depending on its lifecycle state,
     * the instance is registered in the transaction cache and the field
     * is marked as read.
     *
     * @param fieldDesc Field descriptor for the field to be loaded.
     */
    private void prepareGetField(FieldDesc fieldDesc, boolean internal, boolean acquireShareLock) {
        boolean debug = logger.isLoggable(Logger.FINEST);

        if (debug) {
            logger.finest("sqlstore.sqlstatemanager.preparegetfield", fieldDesc.getName()); // NOI18N
        }

        if (acquireShareLock) {
            persistenceManager.acquireShareLock();
        }

        try {
            getLock();

            boolean xactActive = persistenceManager.isActiveTransaction();
            boolean optimistic = persistenceManager.isOptimisticTransaction();
            boolean nontransactionalRead = persistenceManager.isNontransactionalRead();

            if (state.needsReload(optimistic, nontransactionalRead, xactActive)) {
                reload(fieldDesc);
            }

            LifeCycleState oldstate = state;
            state = state.transitionReadField(optimistic, nontransactionalRead, xactActive);

            registerInstance(false, null, oldstate);

            // Only allow dynamic navigation if we are not in the state that allows it.
            if (state.isNavigable() || !internal) {
                if (getPresenceMaskBit(fieldDesc.absoluteID) == false) {
                    realizeField(fieldDesc);
                }
            }
        } catch (JDOException e) {
            throw e;
        } catch (Exception e) {
            logger.log(Logger.FINE,"sqlstore.exception.log", e);
            throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                    "core.statemanager.getfieldfailed"), e); // NOI18N
        } finally {
            if (acquireShareLock) {
                persistenceManager.releaseShareLock();
            }
            releaseLock();

            if (debug) {
                logger.finest("sqlstore.sqlstatemanager.preparegetfield.exit"); // NOI18N
            }
        }
    }

    /**
     * Retrieves a field specified by <code>fieldDesc</code> from the data
     * store. If the field is part of a group then all unfetched fields
     * in the group are retrieved.  realizeField is part of dynamic
     * navigation. The field is marked as present.
     *
     * @param fieldDesc The field descriptor of the field to be retrieved.<p>
     * Note: The <code>fieldDesc</code> parameter must not be null.
     */
    private void realizeField(FieldDesc fieldDesc) {
        assert fieldDesc != null;

        boolean debug = logger.isLoggable();

        if (debug) {
            logger.fine("sqlstore.sqlstatemanager.realizefield", fieldDesc.getName()); // NOI18N
        }

        if (!persistenceConfig.isNavigable()) {
            throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                    "core.statemanager.notnavigable", // NOI18N
                    fieldDesc.getName(), persistentObject.getClass().getName()));
        }

        boolean fieldRealized = false;

        if (fieldDesc instanceof ForeignFieldDesc) {
            ForeignFieldDesc ff = (ForeignFieldDesc) fieldDesc;

            // We can do an enhancement if we are only getting a single
            // relationship field. Check if the field can be retrieved on
            // it's own and the relationship is not mapped to a join table.
            // The field can be retrieved on it's own if it's is in
            // an independent fetch group or not in a fetch group at all.
            // Independent fetch groups have group ids < FieldDesc.GROUP_NONE.
            if (ff.fetchGroup <= FieldDesc.GROUP_NONE
                    && persistenceConfig.getFetchGroup(ff.fetchGroup).size() <= 1
                 && !ff.useJoinTable()) {

                fieldRealized = realizeForeignField(ff);
            }
        }

        if (!fieldRealized) {
            retrieve(fieldDesc);
        }

        if (debug) {
            logger.fine("sqlstore.sqlstatemanager.realizefield.exit"); // NOI18N
        }
    }

    /**
     * For foreign fields we want to take advantage of knowing the
     * relationship key and only selecting the foreign rather than the
     * primary with the foreign attached.  Most of the work is in
     * figuring out whether we can do that.
     *
     * @param foreignField The relationship field to be retrieved.
     *  Following is true for this field.
     * <ul>
     *  <li>It is part of an independent fetch group with only one
     *  field in the fetch group or not part of any fetch group.</li>
     *  <li>It is not mapped to a join table. </li>
     * </ul>
     * Note: The <code>foreignField</code> parameter must not be null.
     *
     * @return True, if relationship field has been retrieved, false otherwise.
     */
    private boolean realizeForeignField(ForeignFieldDesc foreignField) {
        assert foreignField != null;

        boolean isPresent = false;
        boolean debug = logger.isLoggable();

        if (debug) {
            logger.fine("sqlstore.sqlstatemanager.realizeforeignfield", // NOI18N
                    foreignField.getName());
        }

        // Check and see if all the values we need are present.
        for (int i = 0; i < foreignField.localFields.size(); i++) {
            LocalFieldDesc lf = (LocalFieldDesc) foreignField.localFields.get(i);
            isPresent = getPresenceMaskBit(lf.absoluteID);

            if (!isPresent) {
                break;
            }
        }

        if (isPresent) {
            // All the values we need are present.  Wow.  Now we'll have to
            // format a more specialized request and attach the object(s)
            // we get back to our managed object.
            populateForeignField(foreignField);
        }

        if (debug) {
            logger.fine("sqlstore.sqlstatemanager.realizeforeignfield.exit", // NOI18N
                    Boolean.valueOf(isPresent));
        }

        return isPresent;
    }

    /**
     * Retrieves the relationship.
     *
     * @param foreignField The relationship field to be retrieved.
     *  Following is true for this field.
     * <ul>
     *  <li>It is part of an independent fetch group with only one
     *  field in the fetch group or not part of any fetch group.</li>
     *  <li>It is not mapped to a join table. </li>
     * </ul>
     * Note: The <code>foreignField</code> must not be null.
     */
    private void populateForeignField(ForeignFieldDesc foreignField) {
        assert foreignField != null;

        boolean foundInstance = false;

        if (foreignField.hasForeignKey() && foreignField.isMappedToPk()) {
            // Cache lookup, returns an object.
            Object pc = getObjectById(foreignField, null, null, true);

            if (foundInstance = (pc != null)) {
                foreignField.setValue(this, pc);
            }
        }

        if (!foundInstance) {
            // Query lookup, returns a collection.
            Collection result = retrieveForeign(foreignField);
            attachQueryResult(foreignField, result);
        }

        // Or in PRESENT bit for this field in the properties mask
        setPresenceMaskBit(foreignField.absoluteID);
    }

    /**
     * Attaches the retrieved object(s) <code>queryResult</code> to the
     * relationship field <code>foreignField</code> of the instance
     * managed by this state manager.
     *
     * @param queryResult Retrieved value for the relationship field
     * <code>foreignField</code>.
     */
    private void attachQueryResult(ForeignFieldDesc foreignField,
                                   Collection queryResult) {
        assert foreignField != null;

        // Attach the object(s) we got back to our managed object.
        // There are three cases:
        //		1) The foreign field contains a collection
        //		2) The foreign object doesn't exist (NIL)
        //		3) The foreign field is a reference to a single object
        if (foreignField.getComponentType() != null) {
            // Instantiate and populate a dynamic array, namely Collection.
            // NOTE: queryResult is null, if we didn't execute the retrieval!
            replaceCollection(foreignField, queryResult);
        } else if (queryResult == null || queryResult.size() == 0) {
            foreignField.setValue(this, null);
        } else {
            if (queryResult.size() > 1) {
                throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                        "core.persistencestore.toomanyobjforcard1", // NOI18N
                        persistenceConfig.getName(),
                        foreignField.getName(), "" + queryResult.size())); // NOI18N
            }
            Object v = queryResult.iterator().next();
            foreignField.setValue(this, v);
        }
    }

    /**
     * Retrieves the relationship based on the values of the
     * relationship columns on the local side. Note: This method
     * assumes that the relationship key fields are loaded.
     *
     * RESOLVE:
     * The relationship might be mapped to an Unique Key.
     * Do databases constrain the Unique Key columns to be non null?
     *
     * @param foreignField The relationship field to be retrieved.
     *  Following is true for this field.
     * <ul>
     *  <li>It is part of an independent fetch group with only one
     *  field in the fetch group or not part of any fetch group.</li>
     *  <li>It is not mapped to a join table. </li>
     * </ul>
     * Note: The <code>foreignField</code> must not be null.
     *
     * @return Collection returned by the query. Null, if the relationship
     *  is not set or the passed relationship field is null.
     * @see #retrieve
     */
    private Collection retrieveForeign(ForeignFieldDesc foreignField) {
        assert foreignField != null;

        Collection result = null;
        boolean debug = logger.isLoggable();

        if (debug) {
            logger.fine("sqlstore.sqlstatemanager.retrieveforeign", // NOI18N
                    foreignField.getName());
        }

        Object[] values = new Object[foreignField.localFields.size()];
        boolean isValidForeignKey = true;

        for (int i = 0; i < foreignField.localFields.size(); i++) {
            FieldDesc flf = (FieldDesc) foreignField.localFields.get(i);

            if (!getPresenceMaskBit(i)) {
                // throw exception
            }

            if (getSetMaskBit(flf.absoluteID)) {
                // This is one reason why we must have a before image
                // on relationship changes!
                values[i] = flf.getValue(beforeImage);
            } else {
                values[i] = flf.getValue(this);
            }

            // Make sure we have a valid query key.
            if (values[i] == null) {
                // The relationship must be null. No need to query!
                isValidForeignKey = false;
            }
        }

        if (isValidForeignKey) {
            // Getting a new generated RD or a cached one.
            RetrieveDesc fdesc =
                    persistenceConfig.getRetrieveDescForFKQuery(foreignField, store);
            result = (Collection) store.retrieve(
                    persistenceManager, fdesc, new QueryValueFetcher(values));
        }

        if (debug) {
            logger.fine("sqlstore.sqlstatemanager.retrieveforeign.exit"); // NOI18N
        }

        return result;
    }

    /**
     * The retrieve method gets a retrieve descriptor to retrieve the
     * desired field and adds constraints necessary to limit the
     * retrieval set to the source object runs the retrieve
     * descriptor against the store, the source object is connected to,
     * and then merges the results back into the source object.
     *
     * @param additionalField The additional field to be retrieved.<p>
     * Note: The <code>additionalField</code> might be null if we just
     * want to reload the instance.
     * @see #retrieveForeign
     */
    private void retrieve(FieldDesc additionalField) {
        boolean debug = logger.isLoggable();

        if (debug) {
            String fieldName = (additionalField != null) ? additionalField.getName() : null;
            logger.fine("sqlstore.sqlstatemanager.retrieve", fieldName); // NOI18N
        }

        LocalFieldDesc[] keyFields = persistenceConfig.getKeyFieldDescs();
        Object [] values = new Object[keyFields.length];
        copyValues(values, keyFields, 0);

        // Getting a new generated RD or a cached one.
        RetrieveDesc rd = persistenceConfig.getRetrieveDescForPKQuery(additionalField, store);
        Collection result = (Collection) store.
                retrieve(persistenceManager, rd, new QueryValueFetcher(values));

        if (result.size() > 1) {
            throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                    "core.statemanager.toomanyrows", // NOI18N
                    persistenceConfig.getPersistenceCapableClass().getName()));
        } else if (result.size() < 1 || result.iterator().next() != persistentObject) {

            // If there are no instances fetched, or the fetched instances is not the one
            // we asked for, it means that it is not found and we throw an exception
            throw new JDOObjectNotFoundException(I18NHelper.getMessage(messages,
                    "core.statemanager.objectnotfound"), // NOI18N
                    new Object[]{persistentObject});
        }

        if (debug) {
            logger.fine("sqlstore.sqlstatemanager.retrieve.exit"); // NOI18N
        }
    }

    /**
     * Copies the value of <code>fields</code> into the array
     * <code>values</code>. The values are copied into the
     * array starting at index <code>startIndex</code>.
     *
     * @param values Array taking the values of <code>fields</code>.
     * @param fields Array of LocalFieldDesc.
     * @param startIndex Starting index into <code>values</code>.
     */
    private void copyValues(Object[] values, LocalFieldDesc[] fields, int startIndex) {

        // The values array should be long enough to hold all the fields.
        assert values.length - startIndex >= fields.length;

        for (int i = 0; i < fields.length; i++) {
            LocalFieldDesc field = fields[i];
            values[i + startIndex] = field.getValue(this);

            // The field is not expected to be null.
            assert values[i + startIndex] != null;
        }
    }

    public SQLStateManager getBeforeImage() {

        // Do not try to get before image if not required
        if (beforeImage == null && isBeforeImageRequired()) {
            boolean debug = logger.isLoggable();

            if (debug) {
                logger.fine("sqlstore.sqlstatemanager.getbeforeimage", // NOI18N
							persistenceConfig.getPersistenceCapableClass().getName());
            }

            try {
                getLock();

                // Make a copy of the persistentObject.
                beforeImage = copyPersistent();
            } finally {
                releaseLock();
            }

            if (debug) {
                logger.fine("sqlstore.sqlstatemanager.getbeforeimage.exit"); // NOI18N
            }
        }

        return beforeImage;
    }

    public boolean isBeforeImageRequired() {

        com.sun.jdo.api.persistence.support.Transaction t = persistenceManager.currentTransaction();
        // NOTE: We need to create a before image on relationship changes for two reasons:
        // (1) Relationship management assumes relationship fields to be loaded.
        // See prepareUpdateField.
        // (2) The before image is used in realizeField.
        boolean isBeforeImageRequired =
                persistenceManager.isOptimisticTransaction() ||
                getUpdateDesc().hasChangedRelationships() ||
                t.getRetainValues() || t.getRestoreValues() ||
                persistenceConfig.hasModifiedCheckAtCommitConsistency();
       if (logger.isLoggable(Logger.FINER)) {
            logger.finer("sqlstore.sqlstatemanager.isbeforeimagerequired", // NOI18N
                    Boolean.valueOf(isBeforeImageRequired));
       }
       return isBeforeImageRequired;
    }

    private SQLStateManager copyPersistent() {
        PersistenceManager pm = (PersistenceManager) getPersistenceManagerInternal();
        SQLStateManager newStateManager = (SQLStateManager) clone();

        // Associate state manager with a new pc instance.
        pm.newInstance(newStateManager);

        newStateManager.copyFields(this, true, true);

        return newStateManager;
    }

    /**
     * @inheritDoc StateManager#copyFields(StateManager source)
     * Does not copy relationship fields.  Other fields are cloned while
     * copying as per {@link #cloneObjectMaybe(Object source)}.
     * @throws IllegalArgumentException if source is null, is
     * not <code>instanceof</code> SQLStateManager, or is not managing the
     * same type of persistent instance as this StateManager.
     */
    public void copyFields(StateManager source) {
        if (!(source instanceof SQLStateManager)) {
            String className =
                (source != null) ? source.getClass().getName() : null;
            throw new IllegalArgumentException(className);
        }

        SQLStateManager sqlSource = (SQLStateManager) source;

        if (persistenceConfig != sqlSource.getPersistenceConfig()) {
            Class thisPCClass =
                persistenceConfig.getPersistenceCapableClass();
            Class sourcePCClass =
                sqlSource.getPersistenceConfig().getPersistenceCapableClass();
            throw new IllegalArgumentException(
                I18NHelper.getMessage(
                    messages,
                    "core.statemanager.copyFields.mismatch", // NOI18N
                    thisPCClass.getName(),
                    sourcePCClass.getName()));
        }

        copyFields(sqlSource, false, true);
    }

    /**
     * @inheritDoc  StateManager#copyFields(StateManager source).
     * @param copyRelationships if true, then relationship fields are copied,
     * otherwise they are not copied.
     * @param clone if true, then the fields are cloned while copying,
     * otherwise both the <code>source</code> and this StateManager reference
     * the same field value.
     */
    private void copyFields(SQLStateManager source,
                            boolean copyRelationships,
                            boolean clone) {
        ArrayList fields = null;

        // Reset the field masks, as the instance will
        // be populated with the state from the source.
        clearMask(PRESENCE_MASK);
        clearMask(SET_MASK);

        for (int i = 0; i < 2; i++) {
            if (i == 0) {
                fields = persistenceConfig.fields;
            } else {
                fields = persistenceConfig.hiddenFields;
            }

            for (int j = 0; (fields != null) && (j < fields.size()); j++) {
                FieldDesc f = (FieldDesc) fields.get(j);

                if (!copyRelationships && f.isRelationshipField()) {
                    continue;
                }

                if (source.getPresenceMaskBit(f.absoluteID)) {
                    Object value = f.getValue(source);
                    f.setValue(this, (clone) ? cloneObjectMaybe(value) : value);
                    setPresenceMaskBit(f.absoluteID);
                }
            }
        }
    }

    private Object cloneObjectMaybe(Object source) {
        // RESOLVE: need to clone SCOCollection

        if (source != null) {
            // RESOLVE: Should we clone byte[]???
            if ((source instanceof SCO)) {
                return ((SCO)source).cloneInternal();

            } else if (!(source instanceof Number) &&
                    !(source instanceof String) &&
                    !(source instanceof Character) &&
                    !(source instanceof Boolean) &&
                    // RESOLVE: #javax.ejb package# !(source instanceof javax.ejb.EJBObject) &&
                    !(source instanceof com.sun.jdo.api.persistence.support.PersistenceCapable) &&
                    !(source instanceof byte[])) {
                try {
                    Class type = source.getClass();

                    if (!type.isArray()) {
                        Method m = type.getMethod("clone", (Class []) null); // NOI18N

                        if (m != null) {
                            return m.invoke(source, (Object []) null);
                        }
                    } else {
                        Object srcArray[] = (Object[]) source;
                        Object dstArray[] = (Object[])
                                java.lang.reflect.Array.newInstance(type.getComponentType(), srcArray.length);

                        for (int i = 0; i < srcArray.length; i++) {
                            dstArray[i] = srcArray[i];
                        }
                        return dstArray;
                    }
                } catch (NoSuchMethodException e) {
                    if (logger.isLoggable()) {
                        Object[] items = new Object[] {e, source.getClass().getName()};
                        logger.fine("sqlstore.sqlstatemanager.nosuchmethodexcep.clone", items); // NOI18N
                    }
                } catch (InvocationTargetException e) {
                } catch (IllegalAccessException e) {
                }
            }
        }

        return source;
    }

    /**
     * Prepares the field described by <code>fieldDesc</code> for update.
     * This method is central to record changes to fields. The state
     * transitions to dirty. The instance is registered in the
     * transaction cache. If the field is set for the first time,
     * the before image is prepared and the field is marked as modified.
     * Updated local fields are added to the column list to be updated.
     *
     * @param fieldDesc Updated field.
     * @param newlyRegisteredSMs
     *   State managers of autopersistent objects will be added to this list.
     * @see #prepareUpdateFieldSpecial
     */
    private void prepareUpdateField(FieldDesc fieldDesc, ArrayList newlyRegisteredSMs) {

        // No updates for key fields. As this method is called by
        // loadForUpdate for _all_ DFG fields (to prepare a hollow
        // instance for update), we can't throw an exception here
        // like in prepareUpdateFieldSpecial.
        if (fieldDesc.isKeyField()) {
            return;
        }

        getUpdateDesc().markRelationshipChange(fieldDesc);

        boolean debug = logger.isLoggable();

        if (debug) {
             Object[] items = new Object[] {fieldDesc.getName(),state};
             logger.fine("sqlstore.sqlstatemanager.prepareupdatefield", items); // NOI18N
        }

        boolean optimistic = persistenceManager.isOptimisticTransaction();
        boolean xactActive = persistenceManager.isActiveTransaction();
        boolean nontransactionalRead = persistenceManager.isNontransactionalRead();

        if (state.needsReload(optimistic, nontransactionalRead, xactActive)) {
            reload(fieldDesc);
        }

        // State transition.
        // The state transition prevends field updates on deleted instances
        // and must always be executed. See PersistentDeleted.transitionWriteField().
        LifeCycleState oldstate = state;
        state = state.transitionWriteField(xactActive);
        registerInstance(false, newlyRegisteredSMs, oldstate);

        if (state == oldstate && getSetMaskBit(fieldDesc.absoluteID) &&
                getPresenceMaskBit(fieldDesc.absoluteID)) {
            // The presence mask bit is NEVER set for deferred collections,
            // because deferred collections MUST always be reloaded.
            return;
        }

        // We don't reload the field for a newly created instance.
        if (state.isBeforeImageUpdatable()) {

            // Reload the field, as relationship management
            // assumes that relationship fields are always present.
            // See updateObjectField() or processCollectionupdates().
            if (!getPresenceMaskBit(fieldDesc.absoluteID)) {
                prepareGetField(fieldDesc);
            }

            updateBeforeImage(fieldDesc, null);
        }

        recordUpdatedField(fieldDesc);

        if (debug) {
            logger.fine("sqlstore.sqlstatemanager.prepareupdatefield.exit"); // NOI18N
        }
    }

    /**
     * Initializes the <code>beforeImage</code> and registers the before image
     * value if not null.
     *
     * @param fieldDesc Updated field.
     * @param value BeforeImage value, null if called from
     * {@link #prepareUpdateField}. If the value is null it will be retrieved
     * from the instance.
     * @see #prepareUpdateFieldSpecial
     */
    private void updateBeforeImage(FieldDesc fieldDesc, Object value) {

        getBeforeImage();

        if (beforeImage != null
                && !beforeImage.getPresenceMaskBit(fieldDesc.absoluteID)
                && (fieldDesc.sqlProperties & FieldDesc.PROP_LOG_ON_UPDATE) > 0) {

            if (value == null) {
                value = fieldDesc.getValue(this);
            }

            if (value != null) {
                if (logger.isLoggable(Logger.FINEST)) {
                    Object[] items = new Object[] {fieldDesc, value};
                    logger.finest("sqlstore.sqlstatemanager.updatebeforeimage", items); // NOI18N
                }

                fieldDesc.setValue(beforeImage, cloneObjectMaybe(value));
                beforeImage.setPresenceMaskBit(fieldDesc.absoluteID);
            }
        }
    }

    /**
     * Marks the field <code>fieldDesc</code> as set and schedules local fields
     * to be written to the data store.
     *
     * @param fieldDesc Updated field.
     */
    private void recordUpdatedField(FieldDesc fieldDesc) {
        boolean debug = logger.isLoggable(Logger.FINEST);

        if (!fieldDesc.isRelationshipField() &&
                (fieldDesc.sqlProperties & FieldDesc.PROP_RECORD_ON_UPDATE) > 0) {
            if (debug) {
                logger.finest("sqlstore.sqlstatemanager.recordingfield", fieldDesc); // NOI18N
            }
            getUpdateDesc().recordUpdatedField((LocalFieldDesc) fieldDesc);
        }

        if (debug) {
            logger.finest("sqlstore.sqlstatemanager.makedirtyfield", fieldDesc); // NOI18N
        }
        setSetMaskBit(fieldDesc.absoluteID);
    }

    /**
     * This method adds the dependency between this StateManager
     * and the other.
     *
     * @param sm Second state manager.
     * @see StateManager#addDependency(StateManager sm)
     */
    public void addDependency(StateManager sm) {

        if (logger.isLoggable()) {
            Object[] items = new Object[] {this, sm};
            logger.fine("sqlstore.sqlstatemanager.adddependency", items); // NOI18N
        }

        // The simple solution is to call addUpdatedForeignReference()
        // internally. It might try to reregister both instances again,
        // but the caller should clean up the cache. e.g. the
        // PersistenceManager MUST replace the deleted StateManager
        // with the new instance in the weak cache AFTER this call.

        SQLStateManager other = (SQLStateManager)sm;
        if (!state.isNew() || !state.isDeleted()) {
            // Do not need to add a dependency to a new-deleted instance as
            // its flush is a no-op any way.

            // First parameter == null marks a non removable dependency.
            this.addUpdatedForeignReference(null, other);
        } else if ((other.stateFlags & ST_REGISTERED) == 0) {

            // If we did not add a dependency, we still need to register the other
            // instance if it is not yet registered.
            persistenceManager.registerInstance(other, other.getObjectId(), false, true);
            other.stateFlags |= ST_REGISTERED;
        }
    }

    /**
     * Resolves the dependencies for the instances waiting for this state manager.
     * Dependencies are registered instantly during the course of the transaction.
     * For this reason, the introduced dependencies must be checked, if they are
     * still valid at commit/flush. E.g. remove dependencies introduced on
     * relationship removal are only valid, if the removed instance is deleted.
     * <p>
     * This method checks the dependencies for all instances waiting for the
     * current state manager to be flushed to the store.
     */
    public void resolveDependencies() {
        if (logger.isLoggable()) {
            logger.fine("sqlstore.sqlstatemanager.resolvedependencies", this.getPersistent()); // NOI18N
        }

        if (updatedForeignReferences != null) {
            Iterator iter = updatedForeignReferences.iterator();

            while (iter.hasNext()) {
                final UpdatedForeignReference ufr = (UpdatedForeignReference) iter.next();
                final ForeignFieldDesc fieldDesc = ufr.getFieldDesc();
                final SQLStateManager foreignSM = ufr.getStateManager();

                if (resolveDependency(fieldDesc, foreignSM)) {
                    foreignSM.removeDependency();
                    iter.remove();
                }
            }
        }
    }

    /**
     * Tries to resolve the dependency between <code>this</code>
     * and <code>foreignSM</code> introduced on the update of
     * relationship field <code>fieldDesc</code>. There are three
     * kinds of dependencies:
     * <ul>
     * <li>Create dependency, see {@link #registerRemoveDependency}</li>
     * <li>Remove dependency, see {@link #registerCreateDependency}</li>
     * <li>Update dependency, see {@link #manageDependencyForObjectField}</li>
     * </ul>
     * The current implementation does not attempt to resolve
     * <em>Update dependencies</em>.
     *
     * @param fieldDesc Relationship field.
     * @param foreignSM Foreign state manager.
     * @return True, if the dependency between <code>this</code>
     *   and <code>foreignSM</code> can be safely removed.
     */
    private boolean resolveDependency(ForeignFieldDesc fieldDesc,
                                      SQLStateManager foreignSM) {
        boolean removeDependency = false;
        Object pc = foreignSM.getPersistent();

        if (!state.isPersistentInDataStore()) {
            // Check for a create dependency.
            // The create dependency is valid, if the instance is being created.
            if (state.getUpdateAction() != ActionDesc.LOG_CREATE) {
                // The instance is not persistent and will not be created.
                removeDependency = true;
            } else if (!checkRelationship(this, fieldDesc, pc)) {
                // No relationship between the two instances.
                removeDependency = true;
            }
        } else {
            // Not removable dependencies are marked by fieldDesc == null.
            // See delete/create with the same id dependency in addDependency
            // or update dependencies in manageDependencyForObjectField.
            // RESOLVE: Are update dependencies removable?
            if (fieldDesc != null) {
                // Check for a remove dependency.
                // The remove dependency is valid, if the formerly referred
                // instance is being removed.
                if (foreignSM.state.getUpdateAction() != ActionDesc.LOG_DESTROY) {
                    // The formerly referred instance will not be removed.
                    removeDependency = true;
                } else if (fieldDesc.cardinalityUPB <= 1) {
                    // Don't check collection relationships, as collection
                    // fields might not be populated in the before image.
                    // RESOLVE: Collection relationships shouldn't be
                    // checked for remove dependencies anyway.
                    if (!checkRelationship(beforeImage, fieldDesc, pc)) {
                        // No previous relationship between the two instances.
                        removeDependency = true;
                    }
                }
            }
        }

        if (removeDependency && logger.isLoggable()) {
            Object[] items = new Object[] {this.getPersistent(), fieldDesc.getName(), pc};
            logger.fine("sqlstore.sqlstatemanager.resolvedependency", items); // NOI18N
        }
        return removeDependency;
    }

    /**
     * Decrements the reference count and marks this instance
     * as updateable, if the reference count is zero.
     */
    private void removeDependency() {
        if (--referenceCount == 0) {
            stateFlags &= ~ST_UPDATE_DISABLED;
        }
    }

    /**
     * Checks, if there is a relationship between the persistence
     * capable instance managed by state manager <code>sm</code> and
     * <code>pc</code> on field <code>fieldDesc</code>. If
     * <code>sm</code> represents a before image, the relationship was
     * reset in the current transaction.
     *
     * @param sm Either the before- or after image of the current state manager.
     * @param fieldDesc Relationship field.
     * @param pc Persistence capable instance checked for relationship.
     * @return True, if the persistence capable instances are (were)
     * related on the given field.
     */
    static private boolean checkRelationship(SQLStateManager sm,
                                             ForeignFieldDesc fieldDesc,
                                             Object pc) {
        boolean related = false;

        if (fieldDesc != null && sm != null
                && sm.getPresenceMaskBit(fieldDesc.absoluteID)) {

            if (fieldDesc.cardinalityUPB > 1) {
                // Checking directly for contains doesn't work for deferred SCOCollections.
                Collection c = sm.getCollectionValue(fieldDesc);

                // Resulting collection can't be null because the presence mask is set.
                related = c.contains(pc);
            } else {
                related = fieldDesc.getValue(sm) == pc;
            }
        }
        return related;
    }

    /**
     * Nullify the relationship in the data store before the _possible_ removal
     * of removedSM.  To maintain referentional integrity constraints in the
     * database, the relationship to the removed instance has to be nullified,
     * before the removed object might be deleted. Since we use immediate
     * dependency management, we don't know, if the removed object is deleted
     * later on. The data store relationship has to be erased before the
     * removal. Immediate dependency management determines dependencies
     * immediatly when the relationship is set. In contrast the deferred
     * approach waits until the objects are flushed to the store.  Set the
     * dependency only if both instances are already persistent.
     *
     * @param fieldDesc Updated relationship field.
     * @param removedSM State manager removed from the relationship.
     * @see #nullifyForeignKey(ForeignFieldDesc, SQLStateManager, ForeignFieldDesc, boolean)
     * @see #removeJoinTableEntry(ForeignFieldDesc, SQLStateManager, ForeignFieldDesc)
     */
    private void registerRemoveDependency(ForeignFieldDesc fieldDesc, SQLStateManager removedSM) {
        if (this.state.isPersistentInDataStore() &&
                removedSM.state.isPersistentInDataStore()) {

            this.addUpdatedForeignReference(fieldDesc, removedSM);
        }
    }

    /**
     * The referred object has to be written to the store before the
     * relationship can be set.  To ensure referentional integrity
     * constraints in the database, the added object has to be written
     * to the store, before the relationship can be set. The same
     * dependency applies for relationships mapped to a jointable.
     *
     * @param inverseFieldDesc Updated inverse relationship field.
     * WE must pass the inverse field, because the dependency is
     * registered on the added state manager. See
     * {@link #registerRemoveDependency}.
     * @param addedSM State manager added to the relationship.
     * @see #setForeignKey(ForeignFieldDesc, SQLStateManager, ForeignFieldDesc)
     * @see #addJoinTableEntry(ForeignFieldDesc, SQLStateManager, ForeignFieldDesc)
     */
    private void registerCreateDependency(ForeignFieldDesc inverseFieldDesc, SQLStateManager addedSM) {
        if (!addedSM.state.isPersistentInDataStore()) {
            addedSM.addUpdatedForeignReference(inverseFieldDesc, this);
        }
    }

    /**
     * Adds a dependency for state manager <code>sm</code>. State
     * manager <code>sm</code> must wait for <code>this</code> to be
     * flushed to the store before it can be written itself. State
     * manager <code>sm</code> is added to the list of objects
     * depending on <code>this</code> and will be notified when
     * <code>this</code> is written to the store. Store updates to
     * <code>sm</code> are disabled until then. This dependency is
     * established to maintain referential integrity conditions in the
     * data store.
     *
     * @param fieldDesc Updated relationship field.
     * @param sm Foreign state manager depending on <code>this</code>.
     */
    private void addUpdatedForeignReference(ForeignFieldDesc fieldDesc, SQLStateManager sm) {

        // Avoid self-dependency.
        if (sm == this) {
            return;
        }

        // IMPORTANT: The following check assumes that this StateManager needs to be
        // registered only if it has no updatedForeignReferences. Check if this causes
        // a problem after this instance has been flushed, and updatedForeignReferences
        // is now an empty collection.
        if (updatedForeignReferences == null) {
            updatedForeignReferences = new HashSet();

            // Register this instance disregarding it's LifeCycle state.
            // Otherwise, its state may never be reset after the transaction commits.
            if ((stateFlags & ST_REGISTERED) == 0) {
                persistenceManager.registerInstance(this, getObjectId(), false, true);
                stateFlags |= ST_REGISTERED;
            }
        }

        if (updatedForeignReferences.add(new UpdatedForeignReference(fieldDesc, sm))) {
            sm.stateFlags |= ST_UPDATE_DISABLED;
            sm.referenceCount++;

            if (logger.isLoggable() ) {
                String fieldName = (fieldDesc != null) ? fieldDesc.getName() : null;
                Object[] items = new Object[] {this.persistentObject, fieldName,
                                               sm.persistentObject, new Integer(sm.referenceCount)};
                logger.fine("sqlstore.sqlstatemanager.addupdate", items); // NOI18N
            }

            // Register this instance disregarding it's LifeCycle state.
            // Otherwise, its state may never be reset after the transaction commits.
            if ((sm.stateFlags & ST_REGISTERED) == 0) {
                persistenceManager.registerInstance(sm, sm.getObjectId(), false, true);
                sm.stateFlags |= ST_REGISTERED;
            }
        }
    }

    /**
     * Removes the dependency from state manager <code>sm</code> on
     * <code>this</code>. State manager <code>sm</code> does not need to
     * wait for <code>this</code> to be flushed to the store. The dependency
     * was established to maintain referential integrity conditions in the
     * data store.
     *
     * @param fieldDesc Updated relationship field.
     * @param sm Foreign state manager removed from the dependency.
     */
    private void removeUpdatedForeignReference(ForeignFieldDesc fieldDesc, SQLStateManager sm) {
        if ((updatedForeignReferences == null) ||
                (updatedForeignReferences.size() == 0)) {
            return;
        }

        if (updatedForeignReferences.remove(new UpdatedForeignReference(fieldDesc, sm))) {
            sm.referenceCount--;

            if (logger.isLoggable()) {
                String fieldName = (fieldDesc != null) ? fieldDesc.getName() : null;
                Object[] items = new Object[] {this.persistentObject, fieldName,
                                               sm.persistentObject, new Integer(sm.referenceCount)};
                logger.fine("sqlstore.sqlstatemanager.removeupdate", items); // NOI18N
            }

            if (sm.referenceCount == 0) {
                sm.stateFlags &= ~ST_UPDATE_DISABLED;
            }
        }
    }

    /**
     * Handles relationship updates for the object side of a one-to-many or both
     * sides of a one-to-one relationship. This method processes i.e. the
     * Employee side of the Employee-Department, or both sides
     * Employee-Insurance relationship. Nullifies the relation on the instance
     * removed from the relationship. The relation is set on the added instance
     * <code>value</code>. Data store updates are scheduled. Updates the inverse
     * relationship side if <code>updateInverseRelationshipField</code> == true
     * and the relationship is mapped as bi-directional by the user.
     *
     * @param fieldDesc Updated relationship field.
     * @param addedObject Relationship object to be set.
     * @param updateInverseRelationshipField
     *   True, if we need to update the inverse relationship side.
     * @param managedRelationshipInProgress True during relationship management.
     * @return Always true.
     * @exception JDOUserException
     *    Thrown if the added object <code>addedObject</code> has already been deleted.
     */
    private boolean updateObjectField(ForeignFieldDesc fieldDesc,
                                      Object addedObject,
                                      boolean updateInverseRelationshipField,
                                      boolean managedRelationshipInProgress) {

        boolean debug = logger.isLoggable();

        if (debug) {
            Object[] items = new Object[] {fieldDesc.getName(),fieldDesc.getComponentType()};
            logger.fine("sqlstore.sqlstatemanager.updateobjfield", items); // NOI18N
        }

        Object removedObject = fieldDesc.getValue(this);

        // Don't do anything if the before and after value are the same.
        if (addedObject != removedObject) {

            SQLStateManager addedSM = getAddedSM(addedObject, null);
            SQLStateManager removedSM = getRemovedSM(removedObject);
            SQLStateManager addedInverseFieldSM = null;

            // If the new value is already deleted, we throw an exception.
            if (addedSM != null && addedSM.isDeleted()) {
                JDOUserException ex = new JDOUserException(I18NHelper.getMessage(messages,
                        "jdo.lifecycle.deleted.accessField")); // NOI18N
                ex.addFailedObject(addedObject);
                throw ex;
            }

            ForeignFieldDesc inverseFieldDesc = fieldDesc.getInverseRelationshipField();

            updateRelationshipInDataStore(fieldDesc, addedSM, removedSM,
                    inverseFieldDesc, managedRelationshipInProgress);

            if (updateInverseRelationshipField && inverseFieldDesc != null) {
                addedInverseFieldSM = manageRelationshipForObjectField(inverseFieldDesc, addedSM, removedSM,
                        managedRelationshipInProgress);
            }

            manageDependencyForObjectField(fieldDesc, addedSM, removedSM, addedInverseFieldSM);
        }

        if (debug) {
            logger.fine("sqlstore.sqlstatemanager.updateobjfield.exit"); // NOI18N
        }

        return true;
    }

    /**
     * Updates the relationship in the data store. Updates the (hidden) local fields
     * corresponding to the foreign key columns if the relationship is mapped to a
     * foreign key. Jointable entries are scheduled for creation/removal if the
     * relationship is mapped to a jointable.
     *
     * @param fieldDesc Updated relationship field.
     * @param addedSM State manager of the added object.
     * @param removedSM State manager of the removed object.
     * @param inverseFieldDesc Inverse relationship field.
     * @param managedRelationshipInProgress
     *   True during relationship management. We don't want to update the
     *   relationship fields twice during relationship management.
     */
    private void updateRelationshipInDataStore(ForeignFieldDesc fieldDesc,
                                               SQLStateManager addedSM,
                                               SQLStateManager removedSM,
                                               ForeignFieldDesc inverseFieldDesc,
                                               boolean managedRelationshipInProgress) {

        if (!fieldDesc.useJoinTable()) {
            processForeignKeys(fieldDesc, addedSM, removedSM, inverseFieldDesc,
                    managedRelationshipInProgress);
        } else {
            processJoinTableEntries(fieldDesc, addedSM, removedSM, inverseFieldDesc,
                    managedRelationshipInProgress);
        }
    }

    /**
     * Updates the (hidden) local fields corresponding to the foreign key columns
     * if the relationship is mapped to a foreign key. The updates are written
     * to the store when the instance is flushed. For relationships mapped to
     * foreign keys, we always update the side with the foreign key.
     * The collection side never has the foreign key. Data store updates for
     * added objects are not processed twice during relationship management.
     *
     * Data store dependencies for the update operations are established.
     *
     * @param fieldDesc Updated relationship field.
     * @param addedSM State manager of the added object.
     * @param removedSM State manager of the removed object.
     * @param inverseFieldDesc Inverse relationship field.
     * @param managedRelationshipInProgress True during relationship management.
     */
    private void processForeignKeys(ForeignFieldDesc fieldDesc,
                                    SQLStateManager addedSM,
                                    SQLStateManager removedSM,
                                    ForeignFieldDesc inverseFieldDesc,
                                    boolean managedRelationshipInProgress) {

        // If the fieldDesc property has the REF_INTEGRITY_UPDATES unset, it means we
        // need to update the other side of the relationship naming the foreign key fields.
        boolean updateOtherSide = (fieldDesc.sqlProperties & FieldDesc.PROP_REF_INTEGRITY_UPDATES) == 0;

        if (updateOtherSide) {
            // Null out the foreign key on the removed object.
            if (removedSM != null) {
                removedSM.nullifyForeignKey(inverseFieldDesc, this, fieldDesc, false);
            }
            // Set the foreign key on the added object.
            // Don't set the fk twice during relationship management.
            if (addedSM != null && !managedRelationshipInProgress) {
                addedSM.setForeignKey(inverseFieldDesc, this, fieldDesc);
            }
        } else {
            // Null out the foreign key to the removed object.
            if (removedSM != null) {
                // Don't overwrite the foreign key, if both removedSM and
                // addedSM != null. See runtime test rel12 for an example.
                nullifyForeignKey(fieldDesc, removedSM, inverseFieldDesc, addedSM != null);
            }
            // Set the foreign key to the added object.
            // Don't set the fk twice during relationship management.
            if (addedSM != null && !managedRelationshipInProgress) {
                // See above!
                setForeignKey(fieldDesc, addedSM, inverseFieldDesc);
            }
        }
    }

    /**
     * Schedules jointable entries for relationships mapped to jointables.
     * The actual creation/removal of the jointable entry is deferred until
     * flush. Data store updates for added objects are not processed twice
     * during relationship management.
     *
     * Data store dependencies for the update operations are established.
     *
     * @param fieldDesc Updated relationship field.
     * @param addedSM State manager of the added object.
     * @param removedSM State manager of the removed object.
     * @param inverseFieldDesc Inverse relationship field.
     * @param managedRelationshipInProgress True during relationship management.
     */
    private void processJoinTableEntries(ForeignFieldDesc fieldDesc,
                                         SQLStateManager addedSM,
                                         SQLStateManager removedSM,
                                         ForeignFieldDesc inverseFieldDesc,
                                         boolean managedRelationshipInProgress) {

        // If the fieldDesc property has the REF_INTEGRITY_UPDATES unset,
        // it means we need to update the other side of the relationship.
        boolean updateOtherSide = (fieldDesc.sqlProperties & FieldDesc.PROP_REF_INTEGRITY_UPDATES) == 0;

        if (updateOtherSide) {
            // Schedule the removal of the jointable entry to removedSM.
            if (removedSM != null) {
                removedSM.removeJoinTableEntry(inverseFieldDesc, this, fieldDesc);
            }

            // Schedule the jointable entry to the added object.
            // Don't schedule the jointable entry twice during relationship management.
            if (addedSM != null && !managedRelationshipInProgress) {
                addedSM.addJoinTableEntry(inverseFieldDesc, this, fieldDesc);
            }
        } else {
            // Schedule the removal of the jointable entry to removedSM.
            if (removedSM != null) {
                // Contrary to foreign key relationships, we always have
                // to remove the previous jointable entry.
                removeJoinTableEntry(fieldDesc, removedSM, inverseFieldDesc);
            }

            // Schedule the jointable entry to the added object.
            // Don't schedule the jointable entry twice during relationship management.
            if (addedSM != null && !managedRelationshipInProgress) {
                addJoinTableEntry(fieldDesc, addedSM, inverseFieldDesc);
            }
        }
    }

    /**
     * Updates the (inverse) relationship field for <code>addedSM</code>
     * and <code>removedSM</code>.
     *
     * @param inverseFieldDesc Inverse relationship field.
     * @param addedSM State manager of the added object.
     * @param removedSM State manager of the removed object.
     * @param managedRelationshipInProgress True during relationship management.
     * @return State manager managing the previous value of addedSM's
     *   relationship field.
     */
    private SQLStateManager manageRelationshipForObjectField(ForeignFieldDesc inverseFieldDesc,
                                                             SQLStateManager addedSM,
                                                             SQLStateManager removedSM,
                                                             boolean managedRelationshipInProgress) {

        Object addedInverseFieldValue = null;
        SQLStateManager addedInverseFieldSM = null;

        if (removedSM != null) {
            removedSM.removeRelationship(inverseFieldDesc, this);
        }

        if (addedSM != null && !managedRelationshipInProgress) {
            addedInverseFieldValue = addedSM.addRelationship(inverseFieldDesc, this);

            if (addedInverseFieldValue != null) {
                addedInverseFieldSM = (SQLStateManager)
                        persistenceManager.getStateManager(addedInverseFieldValue);
            }
        }

        return addedInverseFieldSM;
    }

    /**
     * Dependency management for database operations on the one-to-one
     * relationships. Establishes a dependency to nullify the foreign key on the
     * removed instance before the added instance's foreign key can be set. If
     * the relationship is mapped to a jointable, remove the jointable entry to
     * the removed instance before the jointable entry on the added can be
     * added. This update dependency is valid only for one-to-one relationships,
     * because only one-to-one relationships can be enforced by an unique
     * index on the foreign key. On the other hand, there's never a state
     * manager removed from the relationship for one-to-many relationships.
     *
     * We would like to do this in updateRelationshipInDataStore,
     * but we don't know the added instance's previous value in case of
     * updateOtherSide == false there. Here, we can handle this situation
     * at one place.
     *
     * @param fieldDesc Updated relationship field.
     * @param addedSM Added state manager.
     * @param removedSM Removed state manager.
     * @param addedInverseFieldSM
     *  State manager removed from the relationship,
     *  if the foreign key is on the local side.
     */
    private void manageDependencyForObjectField(ForeignFieldDesc fieldDesc,
                                                SQLStateManager addedSM,
                                                SQLStateManager removedSM,
                                                SQLStateManager addedInverseFieldSM) {

        // If the fieldDesc property has the REF_INTEGRITY_UPDATES unset,
        // it means data store updates are scheduled on the other side of the relationship.
        boolean updateOtherSide = (fieldDesc.sqlProperties & FieldDesc.PROP_REF_INTEGRITY_UPDATES) == 0;

        // Nullify the foreign key on the removed object before
        // the added object's foreign key can be set.
        if (updateOtherSide && removedSM != null && addedSM != null) {

            // Add the dependency only if both objects involved
            // in the relationship being removed are already persistent.
            if (removedSM.state.isPersistentInDataStore()
                    && this.state.isPersistentInDataStore()) {

                // First parameter == null marks a non removable dependency.
                // RESOLVE: Pass inverseFieldDesc here.
                removedSM.addUpdatedForeignReference(null, addedSM);
            }
        }

        // If the foreign key is on this side, addedInverseFieldSM
        // corresponds to the removedSM and this is the addedSM!
        if (!updateOtherSide && addedInverseFieldSM != null) {

            // Add the dependency only if both objects involved
            // in the relationship being removed are already persistent.
            if (addedInverseFieldSM.state.isPersistentInDataStore()
                    && addedSM.state.isPersistentInDataStore()) {

                // First parameter == null marks a non removable dependency.
                // RESOLVE: Pass inverseFieldDesc here.
                addedInverseFieldSM.addUpdatedForeignReference(null, this);
            }
        }
    }

    /**
     * Returns the added object's state manager. Transient objects become
     * "autopersistent" on the association to an already persistent instance and
     * are associated with a new state manager.
     *
     * If <code>newlyRegisteredSMs</code> is not null, the newly created state manager
     * is added to the list. This list is only non-null for the treatment of deferred
     * collection fields, which is done before the actual flush to the data store.
     * Autopersistence management for all other cases is handled sufficiently
     * in the makeAutoPersistent call.
     *
     * @param addedObject Object added to a relationship.
     * @param newlyRegisteredSMs
     *   The state managers of autopersistent objects will be added to this list.
     * @return State manager for the added object. The statemanager will
     *   be not null for all persistence capable objects != null.
     */
    private SQLStateManager getAddedSM(Object addedObject, ArrayList newlyRegisteredSMs) {
        SQLStateManager addedSM = null;

        if (addedObject != null) {
            // Persistence by reachablity.
            if ((addedSM = (SQLStateManager) persistenceManager.getStateManager(addedObject)) == null) {
                makeAutoPersistent(addedObject);
                addedSM = (SQLStateManager) persistenceManager.getStateManager(addedObject);

                // Add the newly created state manager to the newlyRegisteredSMs
                // list so we can do further processing on it.
                if (newlyRegisteredSMs != null && !newlyRegisteredSMs.contains(addedSM)) {
                    newlyRegisteredSMs.add(addedSM);
                }
            }
        }
        return addedSM;
    }

    /**
     * Returns the removed object's state manager.
     *
     * @param removedObject Object removed to a relationship.
     * @return State manager for the removed object. The state manager might
     *   be null even for objects != null (in case of JDK collections).
     * @see #removeCollectionRelationship
     */
    private SQLStateManager getRemovedSM(Object removedObject) {
        SQLStateManager removedSM = null;

        if (removedObject != null) {
            removedSM = (SQLStateManager) persistenceManager.getStateManager(removedObject);
        }
        return removedSM;
    }

    /**
     * Updates the relationship for the collection side of a one-to-many or
     * many-to-many relationship. Objects in <code>removedList</code> are
     * removed from the relation. The relation is set on all objects in
     * <code>addedList</code>. In case of user updates, relationship management
     * on "this" relationship side is done by the user, i.e.  by
     * d.getEmployees().add(newEmp) for a Department d. This method is never
     * called during relationship management.
     *
     * @param fieldDesc Updated relationship field.
     * @param removedList List of objects to be removed from the relationship.
     * @param addedList List of objects to be added to the relationship.
     * @param newlyRegisteredSMs
     *   List taking newly registered SMs for objects becoming autopersistent.
     * @param updateInverseRelationshipField
     *   True, if we need to update the inverse relationship field.
     * @param managedRelationshipInProgress
     *   True during relationship management. NOTE: This parameter is
     *   always false, as the method is never called during relationship management.
     * @exception JDOUserException  Thrown on failures in <code>afterList</code> handling.
     * @see #prepareSetField(FieldDesc,Object,boolean)
     */
    private void processCollectionUpdates(ForeignFieldDesc fieldDesc,
                                          ArrayList removedList,
                                          ArrayList addedList,
                                          ArrayList newlyRegisteredSMs,
                                          boolean updateInverseRelationshipField,
                                          boolean managedRelationshipInProgress) {

        boolean debug = logger.isLoggable();
        ForeignFieldDesc inverseFieldDesc = fieldDesc.getInverseRelationshipField();

        // RESOLVE: What if
        // * inverseFieldDesc is null?
        // * fieldDesc.cardinalityUPB == 1

        if (debug) {
            Object[] items = new Object[] {removedList,addedList};
            logger.fine("sqlstore.sqlstatemanager.processcollectionupdate", items); // NOI18N
        }

        // removedList contains the list of objects removed.
        if (removedList != null) {
            removeCollectionRelationship(fieldDesc, removedList, inverseFieldDesc,
                    updateInverseRelationshipField, managedRelationshipInProgress);
        }

        // addedList contains the objects added.
        if (addedList != null) {
            addCollectionRelationship(fieldDesc, addedList, inverseFieldDesc,
                    newlyRegisteredSMs,
                    updateInverseRelationshipField, managedRelationshipInProgress);
        }

        if (debug) {
            logger.fine("sqlstore.sqlstatemanager.processcollectionupdate.exit"); // NOI18N
        }
    }

    /**
     * Nullifies the relationship for the objects removed from a collection relationship.
     *
     * @param fieldDesc Updated relationship field.
     * @param removedList List of objects to be removed from the relationship.
     * @param inverseFieldDesc Inverse relationship field.
     * @param updateInverseRelationshipField
     *   True, if we need to update the inverse relationship side.
     * @param managedRelationshipInProgress
     *   True during relationship management. NOTE: This parameter is always
     *   false, as the method is never called during relationship management.
     * @see #processCollectionUpdates
     */
    private void removeCollectionRelationship(ForeignFieldDesc fieldDesc,
                                              ArrayList removedList,
                                              ForeignFieldDesc inverseFieldDesc,
                                              boolean updateInverseRelationshipField,
                                              boolean managedRelationshipInProgress) {

        for (int i = 0; i < removedList.size(); i++) {
            SQLStateManager removedSM = getRemovedSM(removedList.get(i));

            // removedSM == null can happen if the collection is non-SCO and contains
            // transient instances which don't become persistent until commit.
            if (removedSM != null) {

                // The collection side never has the foreign key, i.e.
                // it's never processed during relationship management,
                // because data store updates are already done.
                if (!managedRelationshipInProgress) {
                    updateRelationshipInDataStore(fieldDesc, null, removedSM, inverseFieldDesc, false);

                    // Relationship management
                    if (updateInverseRelationshipField && inverseFieldDesc != null) {
                        removedSM.removeRelationship(inverseFieldDesc, this);
                    }
                }
            }
        }
    }

    /**
     * Nullifies the (hidden) local fields corresponding to the foreign key columns
     * for relationship field <code>fieldDesc</code>. Usually the foreign key
     * columns are not mapped explicitly by the user. For this reason the runtime
     * creates hidden fields representing the foreign key columns internally. We
     * determine the local fields by iterating appropriate field list of either
     * <code>fieldDesc</code> or <code>inverseFieldDesc</code>.
     *
     * For dependency management, the removal of the foreign key has
     * to be nullified before the _possible_ removal of removedSM. The
     * same dependency applies to jointable relationships, see {@link
     * #registerRemoveDependency}.
     *
     * @param fieldDesc Updated relationship field.
     * @param removedSM State manager of the removed object.
     * @param inverseFieldDesc Inverse relationship field.
     * @param setDependencyOnly Only set the dependency between <code>this</code>
     *   and <code>removedSM</code>.
     */
    private void nullifyForeignKey(ForeignFieldDesc fieldDesc,
                                   SQLStateManager removedSM,
                                   ForeignFieldDesc inverseFieldDesc,
                                   boolean setDependencyOnly) {

        if (!isDeleted() && !setDependencyOnly) {
            // fieldDesc can be null for one-directional relationships. We are
            // only interested in the LocalFieldDescs for the foreign key columns,
            // which can also be retrieved from inverseFieldDesc.
            if (fieldDesc != null) {
                for (int i = 0; i < fieldDesc.localFields.size(); i++) {
                    LocalFieldDesc la = (LocalFieldDesc) fieldDesc.localFields.get(i);

                    nullifyForeignKey(fieldDesc, la);
                }
            } else {
                for (int i = 0; i < inverseFieldDesc.foreignFields.size(); i++) {
                    LocalFieldDesc la = (LocalFieldDesc) inverseFieldDesc.foreignFields.get(i);

                    nullifyForeignKey(fieldDesc, la);
                 }
            }
        }

        // Nullify the foreign key before the _possible_ removal of removedSM.
        registerRemoveDependency(fieldDesc, removedSM);
    }

    /**
     * Actually nullifies the local field <code>la</code> corresponding to a
     * foreign key column for the relationship update.
     * Fields tracking the foreign key field <code>la</code> are updated.
     *
     * @param fieldDesc Updated relationship field.
     * @param la Local field corresponding to a foreign key column.
     * @exception JDOUserException Is thrown, if the field to be updated
     *   is a primary key field. We don't allow pk updates.
     */
    private void nullifyForeignKey(ForeignFieldDesc fieldDesc, LocalFieldDesc la) {

        if (!getSetMaskBit(la.absoluteID)) {
            prepareUpdateField(la, null);
        }

        JDOUserException pkUpdateEx = null;

        if (la.isKeyField()) {
            try {
                assertPKUpdate(la, null);
            } catch (JDOUserException e) {
                // If the relationship being set to null
                // and the parent instance is being deleted, we will ignore
                // the exception thrown from assertPKUpdate(). The reason is
                // that we are really just trying to set up the dependency
                // and not modify the relationship itself. If we do not
                // ignore this exception, there will be no way to delete
                // the parent instance at all because we don't support
                // modifying primary key.

                if (((stateFlags & ST_DELETE_INPROGRESS) == 0)) {
                    throw e;
                }

                pkUpdateEx = e;
            }
        }

        if (pkUpdateEx == null) {
            // As la tracks fieldDesc, fieldDesc is ignored in updateTrackedFields.
            updateTrackedFields(la, null, fieldDesc);
            la.setValue(this, null);
        }
    }

    /**
     * Schedules the removal of the jointable entry between this and the
     * foreign state manager. A scheduled creation of the jointable entry
     * between these two objects is simply removed. The removal is
     * scheduled on the local side.
     *
     * For dependency management, the removal of the jointable entry has
     * to precede the _possible_ removal of removedSM. The same dependency applies
     * to foreign key relationships, see {@link #registerRemoveDependency}.
     *
     * RESOLVE: What happens, if a field descriptor is null, e.g. for one
     * way relationships, as the descriptors are taken as keys during scheduling?
     *
     * @param fieldDesc Updated relationship field. This field is mapped to a jointable.
     * @param removedSM State manager of the removed object.
     * @param inverseFieldDesc Inverse relationship field.
     * @see #prepareToUpdatePhaseIII
     */
    private void removeJoinTableEntry(ForeignFieldDesc fieldDesc,
                                      SQLStateManager removedSM,
                                      ForeignFieldDesc inverseFieldDesc) {

        // Cleanup dependencies.  We need to cleanup dependencies for
        // flushed autopersistent instances that aren't reachable
        // before commit. See runtime test Autopersistence.TestCase12.
        // The cleanup must be done for removals only, because the
        // only operations executed in prepareToUpdatePhaseIII are
        // removals.
        if (removedSM.state.isAutoPersistent() || this.state.isAutoPersistent()) {
            removedSM.removeUpdatedForeignReference(inverseFieldDesc, this);
            this.removeUpdatedForeignReference(fieldDesc, removedSM);
        }

        // Remove scheduled creation on this side.
        if (fieldDesc != null && getUpdateDesc().removeUpdatedJoinTableRelationship(
                fieldDesc, removedSM, ActionDesc.LOG_CREATE) == false) {

            // Remove scheduled creation on the other side.
            if (inverseFieldDesc == null || removedSM.getUpdateDesc().removeUpdatedJoinTableRelationship(
                    inverseFieldDesc, this, ActionDesc.LOG_CREATE) == false) {

                // Schedule removal on this side.
                // The field descriptor taken as key must not be null, see above!
                getUpdateDesc().recordUpdatedJoinTableRelationship(
                        fieldDesc, this, removedSM, ActionDesc.LOG_DESTROY);

                // Remove the jointable entry before the _possible_ removal of removedSM.
                registerRemoveDependency(fieldDesc, removedSM);
            }
        } else if (fieldDesc == null) {
            throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                    "core.statemanager.cantschedulejointable", // NOI18N
                    this.getPersistenceConfig().getPersistenceCapableClass().getName(),
                    removedSM.getPersistenceConfig().getPersistenceCapableClass().getName()));
        }
    }

    /**
     * Updates the relationship field <code>fieldDesc</code> between <code>this</code>
     * and <code>removedSM</code>.
     *
     * @param fieldDesc Updated relationship field. The field must be != null.
     * @param removedSM State manager of the removed object.
     */
    private void removeRelationship(ForeignFieldDesc fieldDesc, SQLStateManager removedSM) {

        boolean isCollection = (fieldDesc.cardinalityUPB > 1);

        if (!isCollection) {
            prepareUpdateFieldSpecial(fieldDesc, removedSM.persistentObject, false);
            updateTrackedFields(fieldDesc, null, null);
            fieldDesc.setValue(this, null);
        } else {
            try {
                prepareUpdateFieldSpecial(fieldDesc, null, true);
                SCOCollection c = (SCOCollection) fieldDesc.getValue(this);
                c.removeInternal(removedSM.persistentObject);
                updateTrackedFields(fieldDesc, c, null);
            } catch (ClassCastException e) {
                // ignore
            }
        }
    }

    /**
     * Sets the relationship for the objects added to a collection relationship.
     *
     * @param fieldDesc Updated relationship field.
     * @param addedList List of objects to be added to the relationship.
     * @param inverseFieldDesc Inverse relationship field.
     * @param newlyRegisteredSMs
     *   State managers for autopersistent objects will be added to this list.
     * @param updateInverseRelationshipField
     *   True, if we need to update the inverse relationship side.
     * @param managedRelationshipInProgress
     *   True during relationship management. NOTE: This parameter is always
     *   false, as the method is never called during relationship management.
     * @exception JDOUserException  Thrown if objects in <code>addedList</code> have been deleted.
     * @see #processCollectionUpdates
     */
    private void addCollectionRelationship(ForeignFieldDesc fieldDesc,
                                           ArrayList addedList,
                                           ForeignFieldDesc inverseFieldDesc,
                                           ArrayList newlyRegisteredSMs,
                                           boolean updateInverseRelationshipField,
                                           boolean managedRelationshipInProgress) {

        JDOUserException ex = null;

        for (int i = 0; i < addedList.size(); i++) {
            Object addedObject = addedList.get(i);
            SQLStateManager addedSM = getAddedSM(addedObject, newlyRegisteredSMs);

            // addedSM == null can happen if the collection is non-SCO and contains
            // transient instances which don't become persistent until commit.
            if (addedSM != null) {

                if (addedSM.isDeleted()) {
                    // For managed relationships, if the addedObject is deleted, we need
                    // to throw an exception at the end and the exception should include
                    // the deleted objects in its failedObjectArray.
                    if (inverseFieldDesc != null) {
                        if (ex == null) {
                            ex = new JDOUserException(I18NHelper.getMessage(messages,
                                    "jdo.lifecycle.deleted.accessField")); // NOI18N
                        }

                        ex.addFailedObject(addedObject);
                    }
                    continue;
                }

                // The collection side never has the foreign key, i.e.
                // it's never processed during relationship management,
                // because data store updates are already done at that time.
                if (!managedRelationshipInProgress) {
                    updateRelationshipInDataStore(fieldDesc, addedSM, null, inverseFieldDesc, false);

                    // Relationship management
                    if (updateInverseRelationshipField && inverseFieldDesc != null) {
                        addedSM.addRelationship(inverseFieldDesc, this);
                    }
                }
            }
        }

        if (ex != null) {
            throw ex;
        }
    }

    /**
     * Sets the foreign key corresponding to the relationship field
     * <code>fieldDesc</code>. Usually the foreign key columns are not mapped
     * explicitly by the user. For this reason the runtime creates hidden fields
     * representing the foreign key columns internally. We determine the local
     * fields by iterating appropriate field list of either
     * <code>fieldDesc</code> or <code>inverseFieldDesc</code>.
     *
     * To ensure referentional integrity constraints in the database,
     * the added object has to be written to the store, before the
     * foreign key can be set. The same dependency applies to relationships
     * mapped to jointables, see {@link #addJoinTableEntry}.
     *
     * @param fieldDesc Updated relationship field.
     * @param addedSM State manager of the added object.
     * @param inverseFieldDesc Inverse relationship field.
     */
    private void setForeignKey(ForeignFieldDesc fieldDesc,
                               SQLStateManager addedSM,
                               ForeignFieldDesc inverseFieldDesc) {

        if (!isDeleted()) {
            // fieldDesc can be null for one-directional relationships. We are
            // only interested in the LocalFieldDescs for the foreign key columns,
            // which can also be retrieved from inverseFieldDesc.
            if (fieldDesc != null) {
                for (int i = 0; i < fieldDesc.localFields.size(); i++) {
                    LocalFieldDesc la = (LocalFieldDesc) fieldDesc.localFields.get(i);
                    LocalFieldDesc fa = (LocalFieldDesc) fieldDesc.foreignFields.get(i);

                    setForeignKey(fieldDesc, la, fa.getValue(addedSM));
                }
            } else {
                for (int i = 0; i < inverseFieldDesc.foreignFields.size(); i++) {
                    LocalFieldDesc la = (LocalFieldDesc) inverseFieldDesc.foreignFields.get(i);
                    LocalFieldDesc fa = (LocalFieldDesc) inverseFieldDesc.localFields.get(i);

                    setForeignKey(fieldDesc, la, fa.getValue(addedSM));
                }
            }
        }

        // The referred object has to be written to the store before the foreign key can be set.
        registerCreateDependency(inverseFieldDesc, addedSM);
    }

    /**
     * Actually sets the local field <code>la</code> corresponding to a foreign
     * key column to the new value for the relationship update.  The new value
     * is taken from <code>fa</code>, which is typically a primary key field on
     * the other relationship side.  Fields tracking the foreign key field
     * <code>la</code> are updated.
     *
     * @param fieldDesc Updated relationship field.
     * @param la Local field corresponding to a foreign key column.
     * @param faValue Value of the local field corresponding to the primary
     *  key column on the other relationship side.
     * @exception JDOUserException Is thrown, if the field to be updated
     *   is a primary key field. We don't allow pk updates.
     */
    private void setForeignKey(ForeignFieldDesc fieldDesc,
                               LocalFieldDesc la,
                               Object faValue) {

        if (!getSetMaskBit(la.absoluteID)) {
            prepareUpdateField(la, null);
        }

        if (la.isKeyField()) {
            assertPKUpdate(la, faValue);
        }

        updateTrackedFields(la, faValue, fieldDesc);
        la.setValue(this, faValue);
    }

    /**
     * Schedules the creation of a jointable entry between this and the added
     * state manager. A scheduled removal of the jointable entry between these
     * two is simply removed. The creation is scheduled on the local side.
     *
     * For dependency management, the side creating the jointable entry has
     * to wait for the other to become persistent. The same dependency applies
     * to foreign key relationships, see {@link #setForeignKey}.
     *
     * RESOLVE: What happens, if a field descriptor is null, e.g. for one
     * way relationships, as the descriptors are taken as keys during scheduling?
     *
     * @param fieldDesc Updated relationship field. This field is mapped to a jointable.
     * @param addedSM State manager of the added object.
     * @param inverseFieldDesc Inverse relationship field.
     */
    private void addJoinTableEntry(ForeignFieldDesc fieldDesc,
                                   SQLStateManager addedSM,
                                   ForeignFieldDesc inverseFieldDesc) {

        // Cleanup dependencies.
        // Note: The following lines break deadlock detection for circular dependencies.
        //this.removeUpdatedForeignReference(addedSM);
        //addedSM.removeUpdatedForeignReference(this);

        // Remove scheduled removal on this side.
        if (fieldDesc != null && getUpdateDesc().removeUpdatedJoinTableRelationship(
                fieldDesc, addedSM, ActionDesc.LOG_DESTROY) == false) {

            // Remove scheduled removal on the other side.
            if (inverseFieldDesc == null || addedSM.getUpdateDesc().removeUpdatedJoinTableRelationship(
                    inverseFieldDesc, this, ActionDesc.LOG_DESTROY) == false) {

                // Schedule creation on this side.
                // The field descriptor taken as key must not be null, see above!
                getUpdateDesc().recordUpdatedJoinTableRelationship(
                        fieldDesc, this, addedSM, ActionDesc.LOG_CREATE);

                // The side creating the jointable entry has to wait for the other to become persistent.
                registerCreateDependency(inverseFieldDesc, addedSM);
            }
        } else if (fieldDesc == null) {
            throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                    "core.statemanager.cantschedulejointable", // NOI18N
                    this.getPersistenceConfig().getPersistenceCapableClass().getName(),
                    addedSM.getPersistenceConfig().getPersistenceCapableClass().getName()));
        }
    }

    /**
     * Updates the relationship field <code>fieldDesc</code> between <code>this</code>
     * and the state manager of the added object <code>addedSM</code>.
     *
     * @param fieldDesc Updated relationship field. The field must be != null.
     * @param addedSM State manager of the added object.
     * @return Field <code>fieldDesc</code>'s previous value.
     */
    private Object addRelationship(ForeignFieldDesc fieldDesc,
                                   SQLStateManager addedSM) {

        Object previousValue = null;
        boolean isCollection = (fieldDesc.cardinalityUPB > 1);

        if (!isCollection) {
            previousValue = prepareSetField(fieldDesc, addedSM.persistentObject, true);
        } else {
            try {
                prepareUpdateFieldSpecial(fieldDesc, null, true);
                SCOCollection c = (SCOCollection) fieldDesc.getValue(this);

                // Note: c might be null during relationship management for a
                // self relationship, as in the Employee-Manager relation.
                // See runtime test AutoPersistence.TestCase31 for an example.
                if (c == null) {
                    replaceCollection(fieldDesc, null);
                    c = (SCOCollection) fieldDesc.getValue(this);
                }

                c.addInternal(addedSM.persistentObject);
                updateTrackedFields(fieldDesc, c, null);
            } catch (ClassCastException e) {
                // ignore
            }
        }

        return previousValue;
    }

    /**
     * This is a special version of prepareUpdateField that does not do navigation
     * if a field is not loaded. We don't need to reload the field, because the
     * before image value is given as parameter! This method is mostly called
     * during relatioship management, as the before image value is already know
     * in this case.
     * <p>
     * The <code>createDeferredCollection</code> parameter should be true
     * only if <code>fieldDesc</code> corresponds to a collection field. Note:
     * <ul>
     * <li><code>createDeferredCollection</code> == false
     *   ==> <code>beforeImageValue</code> must be non-null.</li>
     * <li><code>createDeferredCollection</code> == true
     *   ==> <code>beforeImageValue</code> is null.</li>
     * </ul>.
     *
     * @param fieldDesc The field to be prepared.
     * @param beforeImageValue The before image value.
     * @param createDeferredCollection
     *   Indicates whether to create a deferred SCOCollection. Deferred collections
     *   are created during relationship management if the inverse field is not
     *   loaded.
     * @see #prepareUpdateField
     */
    private synchronized void prepareUpdateFieldSpecial(FieldDesc fieldDesc,
                                                        Object beforeImageValue,
                                                        boolean createDeferredCollection) {
        if (fieldDesc.isKeyField()) {
            throw new JDOUnsupportedOptionException(I18NHelper.getMessage(messages,
                    "core.statemanager.nopkupdate")); // NOI18N
        }

        getUpdateDesc().markRelationshipChange(fieldDesc);

        boolean debug = logger.isLoggable();

        if (debug) {
            Object[] items = new Object[] {fieldDesc.getName(),state};
            logger.fine("sqlstore.sqlstatemanager.prepareupdatefieldspl", items); // NOI18N
        }

        boolean optimistic = persistenceManager.isOptimisticTransaction();
        boolean xactActive = persistenceManager.isActiveTransaction();
        boolean nontransactionalRead = persistenceManager.isNontransactionalRead();

        if (state.needsReload(optimistic, nontransactionalRead, xactActive)) {
            if (!optimistic) {
                persistenceManager.clearFields(this.persistentObject);
            }

            reload(null);
        }

        LifeCycleState oldstate = state;
        state = state.transitionWriteField(xactActive);
        registerInstance(false, null, oldstate);

        if (getSetMaskBit(fieldDesc.absoluteID)) {
            // Note: The set mask is set for all fields on make persistent.
            return;
        }

        if (!getPresenceMaskBit(fieldDesc.absoluteID)) {
            if (!createDeferredCollection) {
                if (!(beforeImageValue instanceof SCOCollection) ||
                        !((SCOCollection) beforeImageValue).isDeferred()) {

                    updateBeforeImage(fieldDesc, beforeImageValue);

                    // Set the presence mask for a non deferred collection.
                    setPresenceMaskBit(fieldDesc.absoluteID);
                }
            } else {
                // Deferred collection handling.
                if (!(fieldDesc instanceof ForeignFieldDesc) ||
                        (((ForeignFieldDesc) fieldDesc).cardinalityUPB <= 1)) {
                    //should throw an exception
                    return;
                }

                Object value = fieldDesc.getValue(this);

                if (value == null) {
                    // If the collection field is null, we need to create a
                    // deferred SCOCollection.
                    SCOCollection c = (SCOCollection) persistenceManager.newCollectionInstanceInternal(
                            fieldDesc.getType(),
                            persistentObject,
                            fieldDesc.getName(),
                            fieldDesc.getComponentType(),
                            false,
                            10);
                    c.markDeferred();
                    fieldDesc.setValue(this, c);
                }
                // NOTE: We don't set the presence mask bit for deferred collections,
                // because deferred collections MUST be reloaded on the first read access!
            }
        }

        recordUpdatedField(fieldDesc);

        if (debug) {
            logger.fine("sqlstore.sqlstatemanager.prepareupdatefieldspl.exit"); // NOI18N
        }
    }

    /**
     * Updates the values for fields tracking field
     * <code>fieldDesc</code>.  Must be called before the new value
     * for field <code>fieldDesc</code> is actually set.<p>
     *
     * If called when setting the local fields mapped to the
     * relationship on relationship updates, the relationship field
     * tracked by <code>fieldDesc</code> must be ignored when
     * propagating the changes.<p>
     *
     * For overlapping pk/fk situations or if a fk column is
     * explicitly mapped to a visible field, the update of the local
     * field triggers the update of the relationship field tracking
     * the local field.
     *
     * @param fieldDesc Field whose tracked fields we wish to update.
     * @param value New value for the field.
     * @param fieldToIgnore Field to be ignored when propagating
     * changes. This is the relationship field tracked by field
     * <code>fieldDesc</code> if <code>fieldDesc</code> is a
     * <b>hidden</b> local field.
     */
    private void updateTrackedFields(FieldDesc fieldDesc,
                                     Object value,
                                     ForeignFieldDesc fieldToIgnore) {

        ArrayList trackedFields = fieldDesc.getTrackedFields();

        if (trackedFields == null) {
            return;
        }

        boolean debug = logger.isLoggable(Logger.FINEST);

        if (debug) {
            Object[] items = new Object[] {fieldDesc.getName(), value,
                                           ((fieldToIgnore != null) ? fieldToIgnore.getName() : null)};
            logger.finest("sqlstore.sqlstatemanager.updatetrackedfields", items); // NOI18N
        }

        Object currentValue = fieldDesc.getValue(this);
        int size = trackedFields.size();

        ArrayList fieldsToIgnore = ((fieldToIgnore != null) ? fieldToIgnore.getTrackedFields() : null);

        if (fieldDesc instanceof ForeignFieldDesc) {
            // For tracked relationship fields, we simply set the new value.
            for (int i = 0; i < size; i++) {
                ForeignFieldDesc tf = (ForeignFieldDesc) trackedFields.get(i);
                prepareUpdateFieldSpecial(tf, currentValue, false);
                tf.setValue(this, value);
            }
        } else {
            Object previousValues[] = new Object[size];
            LocalFieldDesc primaryTrackedField = null;
            Object primaryTrackedFieldValue = null;

            if ((fieldDesc.sqlProperties & FieldDesc.PROP_PRIMARY_TRACKED_FIELD) > 0) {
                primaryTrackedField = (LocalFieldDesc) fieldDesc;
                primaryTrackedFieldValue = value;
            }

            for (int i = 0; i < size; i++) {
                FieldDesc tf = (FieldDesc) trackedFields.get(i);

                if (tf instanceof LocalFieldDesc) {
                    Object convertedValue = null;
                    Object convertedCurrentValue = null;

                    // RESOLVE: SCODate is problematic because convertValue unsets
                    // the owner. The SCO to be used for restoring is broken.
                    try {
                        convertedValue = tf.convertValue(value, this);
                        convertedCurrentValue = tf.convertValue(currentValue, this);
                    } catch (JDOUserException e) {
                        // We got a conversion error. We need to revert all
                        // the tracked fields to their previous values.
                        // NOTE: We don't have to revert relationship fields
                        // because they come after all the primitive fields.
                        for (int j = 0; j < i; j++) {
                            tf = (FieldDesc) trackedFields.get(j);

                            tf.setValue(this, previousValues[j]);
                        }

                        throw e;
                    }

                    if ((tf.sqlProperties & FieldDesc.PROP_PRIMARY_TRACKED_FIELD) > 0) {
                        primaryTrackedField = (LocalFieldDesc) tf;
                        primaryTrackedFieldValue = convertedValue;
                    }

                    prepareUpdateFieldSpecial(tf, convertedCurrentValue, false);

                    // save the previous values for rollback
                    previousValues[i] = tf.getValue(this);

                    tf.setValue(this, convertedValue);
                } else {
                    // We bypass fieldToIgnore and its trackedFields
                    if (((stateFlags & ST_FIELD_TRACKING_INPROGRESS) > 0)
                            || (tf == fieldToIgnore)
                            || ((fieldsToIgnore != null) && fieldsToIgnore.contains(tf))) {
                        continue;
                    }

                    ForeignFieldDesc ftf = (ForeignFieldDesc) tf;

                    Object pc = null;

                    if (primaryTrackedFieldValue != null) {
                        pc = getObjectById(ftf, primaryTrackedField, primaryTrackedFieldValue, false);
                    }

                    stateFlags |= ST_FIELD_TRACKING_INPROGRESS;
                    prepareSetField(ftf, pc);
                    stateFlags &= ~ST_FIELD_TRACKING_INPROGRESS;
                }
            }
        }

        if (debug) {
            logger.finest("sqlstore.sqlstatemanager.updatetrackedfields.exit"); // NOI18N
        }
    }

    /**
     * Looks up the object associated to this state manager on
     * relationship <code>ff</code> field in the persistence manager cache.
     * The method first constructs the related instance's object id by
     * calling {@link ForeignFieldDesc#createObjectId}. Then asks the
     * persistence manager to retrieve the object associated to this
     * id from it's caches.  If the referred object is not found, the
     * instance returned by {@link PersistenceManager#getObjectById(Object)}
     * is Hollow. Hollow instances are ignored for navigation.
     *
     * @param ff Relationship to be retrieved. The relationship must have
     * an object ("to one side") value.
     * @param updatedField Updated local field mapped to this relationship.
     * @param value <code>updatedField</code>'s new value.
     * @param forNavigation If true, the lookup is executed for navigation.
     * @return Object found in the cache. Null, if the object wasn't found.
     */
    private Object getObjectById(ForeignFieldDesc ff,
                                 LocalFieldDesc updatedField,
                                 Object value,
                                 boolean forNavigation) {
        assert ff.cardinalityUPB <=1;
        // If called for navigation updatedField and value should be null.
        assert forNavigation ? updatedField == null && value == null : true;

        Object rc = null;
        Object oid = ff.createObjectId(this, updatedField, value);

        if (oid != null) {
            rc = persistenceManager.getObjectById(oid);
            LifeCycleState rcState = ((SQLStateManager)
                    ((PersistenceCapable) rc).jdoGetStateManager()).state;

            if (forNavigation && (rcState instanceof Hollow)) {
                rc = null;
            }
        }

        return rc;
    }

    /**
     * Sets field <code>fieldDesc</code> to <code>value</code>.
     * The update of relationship fields is triggered by either calling
     * {@link #updateCollectionField} or {@link #updateObjectField},
     * depending on the field's cardinality.
     *
     * @param fieldDesc Field to be updated.
     * @param value New value.
     * @param managedRelationshipInProgress
     *   True during relationship management.
     * @return Field <code>fieldDesc</code>'s previous value.
     */
    private Object doUpdateField(FieldDesc fieldDesc,
                                 Object value,
                                 boolean managedRelationshipInProgress) {

        prepareUpdateField(fieldDesc, null);

        if (fieldDesc instanceof ForeignFieldDesc) {
            ForeignFieldDesc ff = (ForeignFieldDesc) fieldDesc;
            if (ff.cardinalityUPB > 1) {
                updateCollectionField(ff, (Collection) value,
                        managedRelationshipInProgress);
            } else {
                updateObjectField(ff, value, true,
                        managedRelationshipInProgress);
            }
        }

        updateTrackedFields(fieldDesc, value, null);

        Object currentValue = fieldDesc.getValue(this);
        fieldDesc.setValue(this, value);
        return currentValue;
    }

    private Object prepareSetField(int fieldID, Object value) {
        FieldDesc fieldDesc = persistenceConfig.getField(fieldID);

        return prepareSetField(fieldDesc, value, false, true);
    }

    private Object prepareSetField(FieldDesc fieldDesc, Object value) {
        return prepareSetField(fieldDesc, value, false, false);
    }

    /**
     * Internal method setting the new value <code>value</code> for field
     * <code>fieldDesc</code> during relationship management. Only called on
     * relationship additions for object fields. This method is never called
     * for collection fields. Relationship management for collection fields
     * is done by deferred collections in {@link #addRelationship}.
     * Deferred collections are implemented in <code>SCOCollection</code>.
     * Relationship management on relationship removal is done in
     * {@link #removeRelationship} for both object and collection fields.
     *
     * @param fieldDesc Field to be updated.
     * @param value New value.
     * @param managedRelationshipInProgress Always true.
     * @return Field <code>fieldDesc</code>'s previous value.
     * @see com.sun.jdo.spi.persistence.support.sqlstore.SCOCollection
     */
    private Object prepareSetField(FieldDesc fieldDesc, Object value,
                                   boolean managedRelationshipInProgress) {
        return prepareSetField(fieldDesc, value, managedRelationshipInProgress, false);
    }

    /**
     * Sets field <code>fieldDesc</code> by calling
     * {@link SQLStateManager#doUpdateField(FieldDesc, Object, boolean)}.
     *
     * @param fieldDesc Field to be updated.
     * @param value New value.
     * @param managedRelationshipInProgress
     *   True during relationship management.
     * @param acquireShareLock Acquire a shared lock during the update.
     * @return Field <code>fieldDesc</code>'s previous value.
     */
    private Object prepareSetField(FieldDesc fieldDesc, Object value,
                                   boolean managedRelationshipInProgress,
                                   boolean acquireShareLock) {
        boolean debug = logger.isLoggable(Logger.FINEST);

        if (debug) {
             logger.finest("sqlstore.sqlstatemanager.preparesetfield", fieldDesc.getName()); // NOI18N
        }

        if (acquireShareLock) {
            persistenceManager.acquireShareLock();
        }

        try {
            getLock();

            if ((fieldDesc.sqlProperties & FieldDesc.PROP_READ_ONLY) > 0) {
                throw new JDOUserException(I18NHelper.getMessage(messages,
                        "core.statemanager.readonly", fieldDesc.getName(), // NOI18N
                        persistentObject.getClass().getName()));
            }

            // We need to lock fieldUpdateLock if there is a chance that
            // relationship field values might be affected. This is the case if
            // fieldDesc is a relationship field or it tracks other fields.
            if ((fieldDesc.getTrackedFields() != null) ||
                (fieldDesc instanceof ForeignFieldDesc)) {

                persistenceManager.acquireFieldUpdateLock();
                try {
                    return doUpdateField(fieldDesc, value, managedRelationshipInProgress);
                } finally {
                    persistenceManager.releaseFieldUpdateLock();
                }

            } else {
                    return doUpdateField(fieldDesc, value, managedRelationshipInProgress);
            }
        } catch (JDOException e) {
            throw e;
        } catch (Exception e) {
            logger.log(Logger.FINE,"sqlstore.exception.log", e);
            throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                    "core.statemanager.setfieldfailed"), e); // NOI18N
        } finally {
            if (acquireShareLock) {
                persistenceManager.releaseShareLock();
            }
            releaseLock();

            if (debug) {
                logger.finest("sqlstore.sqlstatemanager.preparesetfield.exit"); // NOI18N
            }
        }
    }

    /**
     * Sets the new value for the collection field by calling
     * {@link #processCollectionUpdates}.
     *
     * @param fieldDesc Field descriptor of the field to be set.
     * @param value New value.
     * @param managedRelationshipInProgress
     *   True during relationship management.
     */
    private void updateCollectionField(ForeignFieldDesc fieldDesc,
                                       Collection value,
                                       boolean managedRelationshipInProgress) {
        boolean debug = logger.isLoggable(Logger.FINEST);

        if (debug) {
            Object[] items = new Object[] {value,((value == null)? "NO" : value.getClass().getName())}; // NOI18N
            logger.finest("sqlstore.sqlstatemanager.processforeignfield", items); // NOI18N
        }

        Object currVal = fieldDesc.getValue(this);

        // Do nothing if the current value is identical to the new value.
        if (currVal != value) {
            Object owner = null;
            ArrayList added = null;
            ArrayList removed = null;

            // Verify SCO owner and fieldName if any
            if (value != null && value instanceof SCOCollection) {
                SCOCollection sco = (SCOCollection) value;
                owner = sco.getOwner();

                if (owner == null) {
                    sco.setOwner(persistentObject, fieldDesc.getName(),
                        fieldDesc.getComponentType());

                } else if (owner != persistentObject ||
                        !fieldDesc.getName().equals(sco.getFieldName())) {

                        throw new JDOUserException(I18NHelper.getMessage(
                            messages, "core.statemanager.anotherowner"), // NOI18N
                            new Object[]{owner, sco.getFieldName()});
                }
                // SCO should not behave as a JDK collection,
                // but become owned and tracked at setXXX operation.
                added = new ArrayList(value);
            }

            Object befrVal = fieldDesc.getValue(beforeImage);
            if (currVal != null) {
                if (debug)
                    logger.finest("sqlstore.sqlstatemanager.processforeignfield.remove"); // NOI18N

                // This is a setXXX (i.e. replace) operation, we need to
                // "remove" elements from the current SCOCollection and mark it as not used

                if (((Collection) currVal).size() > 0) {
                    removed = new ArrayList((Collection) currVal);
                }

                if (currVal instanceof SCOCollection) {
                    if (debug)
                        logger.finest("sqlstore.sqlstatemanager.processforeignfield.reset"); // NOI18N
                    // SCOCollection: mark it as not used
                    ((SCO) currVal).unsetOwner();
                }
            } else if (getSetMaskBit(fieldDesc.absoluteID) == false && befrVal != null)
            // && value instanceof SCOCollection && owner != null)
            {
                if (debug)
                    logger.finest("sqlstore.sqlstatemanager.processforeignfield.remove_from_bi"); // NOI18N
                // Replace with SCOCollection: mark beforeImage as removed

                if (((Collection) befrVal).size() > 0) {
                    removed = new ArrayList((Collection) befrVal);
                }
            }

            processCollectionUpdates(fieldDesc, removed, added, null, true,
                managedRelationshipInProgress);
        }
    }

    public Object clone() {
        SQLStateManager clone = new SQLStateManager(store, persistenceConfig);
        clone.persistenceManager = persistenceManager;

        return clone;
    }

    private void assertNotPK(int fieldNumber) {
        if (persistenceConfig.isPKField(fieldNumber))
            throw new JDOUnsupportedOptionException(I18NHelper.getMessage(messages,
                    "core.statemanager.nopkupdate")); // NOI18N
    }

    private void assertPKUpdate(FieldDesc f, Object value) {
        Object currentValue = f.getValue(this);
        boolean throwException = false;

        // We only throw an exception if the new value is actually different from
        // the current value.
        if ((value != null) && (currentValue != null)) {
            if (value.toString().compareTo(currentValue.toString()) != 0) {
                throwException = true;
            }
        } else if (value != currentValue) {
            throwException = true;
        }

        if (throwException) {
            throw new JDOUnsupportedOptionException(I18NHelper.getMessage(messages,
                    "core.statemanager.nopkupdate")); // NOI18N
        }
    }

    /**
     * ...
     */
    public com.sun.jdo.api.persistence.support.PersistenceManager getPersistenceManagerInternal() {
        return persistenceManager;
    }

    /**
     * ...
     */
    public com.sun.jdo.api.persistence.support.PersistenceManager getPersistenceManager() {
        return (persistenceManager == null)? null : persistenceManager.getCurrentWrapper();
    }


    /**
     * ...
     */
    // !!! olsen: changed to return byte instead of void (->PC.jdoSetFlags())
    public byte setFlags(byte flags) {
        // RESOLVE: Need to verify that the flags are valid with the current
        // state of the state manager.
        return flags;
    }

    /**
     * Triggers the state transition for READ and registers the
     * instance in the transaction cache.
     */
    public void loadForRead() {
        boolean debug = logger.isLoggable(Logger.FINER);

        if (debug) {
            logger.finer("sqlstore.sqlstatemanager.loadforread"); // NOI18N
        }

        persistenceManager.acquireShareLock();

        try {
            getLock();

            byte oldFlags = persistenceManager.getFlags(persistentObject);

            // If the jdoFlag is either READ_OK or READ_WRITE_OK, that means another
            // thread might have already call loadForRead on this instance.
            if (oldFlags != LOAD_REQUIRED) {
                return;
            }

            try {
                boolean xactActive = persistenceManager.isActiveTransaction();
                boolean optimistic = persistenceManager.isOptimisticTransaction();
                boolean nontransactionalRead = persistenceManager.isNontransactionalRead();

                if (state.needsReload(optimistic, nontransactionalRead, xactActive)) {
                    reload(null);
                }

                LifeCycleState oldstate = state;
                state = state.transitionReadField(optimistic, nontransactionalRead, xactActive);
                persistenceManager.setFlags(persistentObject, READ_OK);
                registerInstance(false, null, oldstate);
            } catch (JDOException e) {
                // restore the jdoFlags.
                persistenceManager.setFlags(persistentObject, oldFlags);
                throw e;
            }
        } finally {
            persistenceManager.releaseShareLock();
            releaseLock();

            if (debug) {
                logger.finer("sqlstore.sqlstatemanager.loadforread.exit"); // NOI18N
            }
        }
    }

    /**
     * Triggers the state transition for WRITE and registers the instance
     * in the transaction cache. Prepares all DFG fields for update.
     */
    public void loadForUpdate() {
        boolean debug = logger.isLoggable(Logger.FINER);

        if (debug) {
            logger.finer("sqlstore.sqlstatemanager.loadforupdate"); // NOI18N
        }

        persistenceManager.acquireShareLock();

        try {
            getLock();

            byte oldFlags = persistenceManager.getFlags(persistentObject);

            // If the jdoFlags is already set to READ_WRITE_OK, it means that anther
            // thread has called loadForUpdate on this instance.
            if (oldFlags == READ_WRITE_OK) {
                return;
            }

            persistenceManager.setFlags(persistentObject, READ_WRITE_OK);

            ArrayList fields = persistenceConfig.fields;

            try {
                // Mark all the fields in the dfg dirty.
                for (int i = 0; i < fields.size(); i++) {
                    FieldDesc f = (FieldDesc) fields.get(i);

                    if (f.fetchGroup == FieldDesc.GROUP_DEFAULT) {
                        //prepareSetField(f, null);
                        prepareUpdateField(f, null);
                    }
                }
            } catch (JDOException e) {
                // restore the jdoFlags.
                persistenceManager.setFlags(persistentObject, oldFlags);
                throw e;
            }
        } finally {
            persistenceManager.releaseShareLock();
            releaseLock();

            if (debug) {
                logger.finer("sqlstore.sqlstatemanager.loadforupdate.exit"); // NOI18N
            }
        }
    }

    /**
     * This method serves two purposes:
     * 1. If the field value is null or contains a non-SCOCollection instance, it
     *    creates a new SCOCollection and populates with elements in c.
     * 2. If the field value is a SCOCollection instance, then if it is deferred,
     *    it calls applyDeferredUpdates on the collection passing in c. Otherwise,
     *    it clears the collection and repopulates with elements in c.
     */
    public synchronized void replaceCollection(ForeignFieldDesc ff, Collection c) {
        Collection collection = (Collection) ff.getValue(this);

        SCOCollection scoCollection = null;

        if ((collection == null) || !(collection instanceof SCO)) {
            scoCollection = (SCOCollection) persistenceManager.newCollectionInstanceInternal(
                    ff.getType(), persistentObject, ff.getName(),
                    ff.getComponentType(), false, ((c != null) ? c.size() : 0));

            ff.setValue(this, scoCollection);
            scoCollection.addAllInternal(c);
        } else {
            scoCollection = (SCOCollection) collection;

            if (scoCollection.isDeferred()) {
                scoCollection.applyDeferredUpdates(c);

                // We need to mark all the tracked fields as present.
                ArrayList trackedFields = ff.getTrackedFields();
                if (trackedFields != null) {
                    for (int i = 0; i < trackedFields.size(); i++) {
                        ForeignFieldDesc tf = (ForeignFieldDesc) trackedFields.get(i);

                        setPresenceMaskBit(tf.absoluteID);
                    }
                }
            } else {
                scoCollection.clearInternal();
                scoCollection.addAllInternal(c);
            }
        }

        // Should not use old collection as SCO if any
        if (c != null && c instanceof SCO) {
            ((SCO) c).unsetOwner();
        }
    }

    /**
     * For test purposes
     */
    protected LifeCycleState getCurrentState() {
        return state;
    }

    // Status interrogation methods
    // For each one of these methods, there is a corresponding version
    // of it prefixed with jdo on the PersistenceCapable class. These
    // methods are used to query the state o an instance. For example,
    // when jdoIsReadReady is called on the PersistenceCapable
    // instance, the generated <code>jdoIsReadReady</code> will delegate the
    // status interrogation to the <code>StateManager</code> by call
    // <code>isReadReady()</code>.

    /**
     * ...
     */
    public boolean isDirty() {
        if (state != null) {
            return state.isDirty();
        }

        return false;
    }

    /**
     * ...
     */
    public boolean isTransactional() {
        if (state != null) {
            return state.isTransactional();
        }

        return false;
    }

    /**
     * ...
     */
    public boolean isNew() {
        if (state != null) {
            return state.isNew();
        }

        return false;
    }

    /**
     * ...
     */
    public boolean isDeleted() {
        if (state != null) {
            return state.isDeleted();
        }

        return false;
    }

    /**
     * ...
     */
    public boolean isPersistent() {
        if (state != null) {
            return state.isPersistent();
        }

        return false;
    }

    /**
     * @inheritDoc
     */
    public boolean needsRegisterWithVersionConsistencyCache() {
        boolean rc = hasVersionConsistency();
        if (rc && state != null) {
            rc = state.isPersistent()
                    && state.isTransactional()
                    && !state.isNew()
                    && !state.isDirty()
                    && !state.isDeleted();
        }

        return rc;
    }

    /**
     * @inheritDoc
     */
    public boolean needsUpdateInVersionConsistencyCache() {
        boolean rc = hasVersionConsistency();
        if (rc && state != null) {
            rc = (state.isDirty()
                    || state.isNew()
                    || persistenceConfig.hasLocalNonDFGFields())
                 && !state.isDeleted();
        }

        return rc;
    }

    // Setter methods
    // These are methods for accessing the persistent field values
    // from the <code>StateManager</code>. The setter methods can also
    // serve as the hook for keeping track of changes made to the
    // <code>StateManager</code>.

    public boolean setBooleanField(int fieldNumber, boolean value) {
        assertNotPK(fieldNumber);
        prepareSetField(fieldNumber, new Boolean(value));
        return value;
    }

    public boolean[] setBooleanArrayField(int fieldNumber, boolean[] value) {
        prepareSetField(fieldNumber, null);
        return value;
    }

    public byte setByteField(int fieldNumber, byte value) {
        assertNotPK(fieldNumber);
        prepareSetField(fieldNumber, new Byte(value));
        return value;
    }

    public byte[] setByteArrayField(int fieldNumber, byte[] value) {
        prepareSetField(fieldNumber, null);
        return value;
    }

    public short setShortField(int fieldNumber, short value) {
        assertNotPK(fieldNumber);
        prepareSetField(fieldNumber, new Short(value));
        return value;
    }

    public short[] setShortArrayField(int fieldNumber, short[] value) {
        prepareSetField(fieldNumber, null);
        return value;
    }

    public int setIntField(int fieldNumber, int value) {
        assertNotPK(fieldNumber);
        prepareSetField(fieldNumber, new Integer(value));
        return value;
    }

    public int[] setIntArrayField(int fieldNumber, int[] value) {
        prepareSetField(fieldNumber, null);
        return value;
    }

    public long setLongField(int fieldNumber, long value) {
        assertNotPK(fieldNumber);
        prepareSetField(fieldNumber, new Long(value));
        return value;
    }

    public long[] setLongArrayField(int fieldNumber, long[] value) {
        prepareSetField(fieldNumber, null);
        return value;
    }

    public char setCharField(int fieldNumber, char value) {
        assertNotPK(fieldNumber);
        prepareSetField(fieldNumber, new Character(value));
        return value;
    }

    public char setCharArrayField(int fieldNumber, char value) {
        prepareSetField(fieldNumber, null);
        return value;
    }

    public float setFloatField(int fieldNumber, float value) {
        assertNotPK(fieldNumber);
        prepareSetField(fieldNumber, new Float(value));
        return value;
    }

    public float[] setFloatArrayField(int fieldNumber, float[] value) {
        prepareSetField(fieldNumber, null);
        return value;
    }

    public double setDoubleField(int fieldNumber, double value) {
        assertNotPK(fieldNumber);
        prepareSetField(fieldNumber, new Double(value));
        return value;
    }

    public double[] setDoubleArrayField(int fieldNumber, double[] value) {
        prepareSetField(fieldNumber, null);
        return value;
    }

    public String setStringField(int fieldNumber, String value) {
        assertNotPK(fieldNumber);
        prepareSetField(fieldNumber, value);
        return value;
    }

    public String[] setStringArrayField(int fieldNumber, String[] value) {
        prepareSetField(fieldNumber, null);
        return value;
    }

    /**
     * This method sets object fields, e.g. relationship fields.
     */
    public Object setObjectField(int fieldNumber, Object value) {
        assertNotPK(fieldNumber);
        prepareSetField(fieldNumber, value);
        return value;
    }

    public Object[] setObjectArrayField(int fieldNumber, Object[] value) {
        prepareSetField(fieldNumber, value);
        return value;
    }


    public boolean testIsLoaded(int fieldNumber) {
        return getPresenceMaskBit(fieldNumber);
    }

    public boolean testIsLoaded(String fieldName) {
        FieldDesc f = persistenceConfig.getField(fieldName);

        return testIsLoaded(f.absoluteID);
    }

    public boolean testIsAutoPersistent() {
        return state.isAutoPersistent();
    }

    /**
     * Marks this instance needs to require registering with the global (weak) cache
     * at rollback if it transitions to persistent state.
     * Used for replacing a deleted instance with the newly persistent with
     * the same object id.
     */
    public void markNotRegistered() {
        needsRegisterAtRollback = true;
    }

    /**
     * Marks this instance as needs to be verified at the time it is removed from the
     * global (weak) cache at rollback if it transitions to transient state.
     */
    public void markVerifyAtDeregister() {
        needsVerifyAtDeregister = true;
    }

    /**
     * Marks this instance as a replacement for a deleted instance with the same
     * ObjectId.
     */
    public void markReplacement() {
        isReplacementInstance = true;
    }

    /**
     * Lock this instance.
     */
    // For consistency's sake, this should be changed to acquireLock.
    public void getLock() {
        lock.acquire();
    }

    /**
     * Release lock.
     */
    public void releaseLock() {
        lock.release();
    }


    /**
     * Return value for valid flag.
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Mark this StateManager as valid. Called before returning from
     * getObjectById.
     */
    public void setValid() {
        try {
            getLock();
            valid = true;
        } finally {
            releaseLock();
        }
    }

    /**
     * This class stores a database dependency between the current and
     * a foreign state manager. To resolve dependencies before
     * commit/flush, remember the relationship field intorducing this
     * dependency.
     */
    private class UpdatedForeignReference {
        ForeignFieldDesc fieldDesc;
        SQLStateManager sm;

        private UpdatedForeignReference(ForeignFieldDesc fieldDesc, SQLStateManager sm) {
            this.fieldDesc = fieldDesc;
            this.sm = sm;
        }

        private ForeignFieldDesc getFieldDesc() {
            return fieldDesc;
        }

        private SQLStateManager getStateManager() {
            return sm;
        }

        public boolean equals(Object obj) {
            if (obj != null &&
                this.getClass().equals(obj.getClass())) {
                final UpdatedForeignReference other = (UpdatedForeignReference) obj;

                return (this.fieldDesc == other.fieldDesc && this.sm == other.sm);
            }
            return (false);
        }

        public int hashCode() {
            int hashCode = sm.hashCode();
            if (fieldDesc != null) {
                hashCode += fieldDesc.hashCode();
            }
            return hashCode;
        }
    }
}
