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
 * UpdateObjectDescImpl.java
 *
 * Created on March 3, 2000
 *
 */

package com.sun.jdo.spi.persistence.support.sqlstore.sql;

import com.sun.jdo.api.persistence.support.JDOFatalInternalException;
import com.sun.jdo.spi.persistence.support.sqlstore.*;
import com.sun.jdo.spi.persistence.support.sqlstore.model.ClassDesc;
import com.sun.jdo.spi.persistence.support.sqlstore.model.FieldDesc;
import com.sun.jdo.spi.persistence.support.sqlstore.model.ForeignFieldDesc;
import com.sun.jdo.spi.persistence.support.sqlstore.model.LocalFieldDesc;
import com.sun.jdo.spi.persistence.support.sqlstore.sql.concurrency.Concurrency;
import com.sun.jdo.spi.persistence.utility.logging.Logger;
import org.glassfish.persistence.common.I18NHelper;

import java.util.*;

/**
 * Stores the update information for the associated state manager.
 */
public class UpdateObjectDescImpl implements UpdateObjectDesc {

    /** Array of Object. */
    private List afterHiddenValues;

    private SQLStateManager afterImage;

    /** Array of Object. */
    private List beforeHiddenValues;

    private SQLStateManager beforeImage;

    private Concurrency concurrency;

    private Class pcClass;

    private int updateAction;

    /**
     * Array of LocalFieldDesc.
     * Fields contained in this array are written to the database.
     */
    private List updatedFields;

    private Map updatedJoinTableRelationships;

    /** Marker for fast relationship update check. */
    private boolean relationshipChanged = false;

    /** The logger. */
    private static Logger logger = LogHelperSQLStore.getLogger();

    /** I18N message handler. */
    private final static ResourceBundle messages = I18NHelper.loadBundle(
            "com.sun.jdo.spi.persistence.support.sqlstore.Bundle",  // NOI18N
            UpdateObjectDescImpl.class.getClassLoader());

    public UpdateObjectDescImpl(Class pcClass) {
		this.pcClass = pcClass;
        updatedFields = new ArrayList();
    }

    public Class getPersistenceCapableClass() {
        return pcClass;
    }

    public void reset() {
        updatedFields.clear();

        if (updatedJoinTableRelationships != null) {
            updatedJoinTableRelationships.clear();
        }

        relationshipChanged = false;
        concurrency = null;
    }

    public boolean hasUpdatedFields() {
        return (updatedFields.size() > 0);
    }

    public Collection getUpdatedJoinTableFields() {
        if (updatedJoinTableRelationships == null) {
            return null;
        }

        return updatedJoinTableRelationships.keySet();
    }

    // RESOLVE: Should return _all_ join table descs, not separatly by field.
    public Collection getUpdateJoinTableDescs(FieldDesc fieldDesc) {
        HashMap updateJoinTableDescs = (HashMap) updatedJoinTableRelationships.get(fieldDesc);

        if (updateJoinTableDescs != null) {
            return updateJoinTableDescs.values();
        }

        return null;
    }

    public boolean hasUpdatedJoinTableRelationships() {
        return (updatedJoinTableRelationships != null &&
                updatedJoinTableRelationships.size() > 0);
    }

    /**
     * Returns <code>true</code> if any of the changed fields is byte[].
     */
    public boolean hasModifiedLobField() {

        if (updatedFields != null) {
            for (Iterator i = updatedFields.iterator(); i.hasNext(); ) {

                // The list updatedFields only contains LocalFieldDesc.
                // Thus it's safe to cast to LocalFieldDesc below.
                LocalFieldDesc field = (LocalFieldDesc)i.next();
                if (field.isMappedToLob()) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Marks the relationship change property for this instance, if the
     * updated field is a relationship field or a hidden field tracing a
     * foreign key column in the database.
     *
     * @param fieldDesc Updated field.
     */
    public void markRelationshipChange(FieldDesc fieldDesc) {
        if (fieldDesc.isRelationshipField() || fieldDesc.isForeignKeyField()) {
            if (logger.isLoggable(Logger.FINEST)) {
                logger.finest("sqlstore.sql.updateobjdescimpl.markrelationshipchange"); // NOI18N
            }
            // MARK THE RELATIONSHIP CHANGE for this instance.
            relationshipChanged = true;
        }
    }

    /**
     * Returns <code>true</code>, if this state manager has a changed
     * relationship field.
     * @return True, if this state manager has a changed relationship field.
     */
    public boolean hasChangedRelationships() {
        // If the relationship is set before the makePersistent call,
        // this condition might be false for INSERTs.
        if (relationshipChanged) {
            return true;
        }

        // Check for updated join table relationships.
        if (hasUpdatedJoinTableRelationships()) {
            return true;
        }

        // Check for updated foreign key relationships.
        if (updatedFields != null) {
            for (Iterator iter = updatedFields.iterator(); iter.hasNext(); ) {
                LocalFieldDesc field = (LocalFieldDesc) iter.next();
                if (field.isForeignKeyField()) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Removes a previously scheduled jointable entry for relationship
     * field <code>fieldDesc</code>.  The <code>action</code>
     * parameter specifies, if the entry to be removed is
     * scheduled for creation or removal.
     *
     * @param fieldDesc Updated relationship field.
     * @param foreignSM Associated state manager on the opposite side.
     * @param action The action is either CREATE or REMOVE.
     * @return True, if the specified jointable entry was found and removed, false otherwise.
     * @see #recordUpdatedJoinTableRelationship
     */
    public boolean removeUpdatedJoinTableRelationship(ForeignFieldDesc fieldDesc,
                                                      SQLStateManager foreignSM,
                                                      int action) {
        HashMap updateJoinTableDescs = null;

        if ((updatedJoinTableRelationships == null) ||
                ((updateJoinTableDescs = (HashMap) updatedJoinTableRelationships.get(fieldDesc)) == null)) {
            return false;
        }

        UpdateJoinTableDesc desc = (UpdateJoinTableDesc) updateJoinTableDescs.get(foreignSM);
        if (desc != null && desc.getAction() == action) {
            return (updateJoinTableDescs.remove(foreignSM) != null);
        }

        return false;
    }

    /**
     * Schedules a jointable entry for relationship field
     * <code>fieldDesc</code>.  The scheduled jointable entry is
     * uniquely identified by the relationship field and the two
     * associated state managers.  The <code>action</code> parameter
     * specifies, if the jointable entry should be created or removed.
     *
     * @param fieldDesc Updated relationship field.
     * @param parentSM State manager responsible for <code>fieldDesc</code>'s defining class.
     * @param foreignSM State manager responsible for the other side.
     * @param action The action is either CREATE or REMOVE.
     * @see #removeUpdatedJoinTableRelationship
     */
    public void recordUpdatedJoinTableRelationship(ForeignFieldDesc fieldDesc,
                                                   SQLStateManager parentSM,
                                                   SQLStateManager foreignSM,
                                                   int action) {
        if (updatedJoinTableRelationships == null) {
            updatedJoinTableRelationships = new HashMap();
        }

        HashMap updateJoinTableDescs = null;

        if ((updateJoinTableDescs = (HashMap) updatedJoinTableRelationships.get(fieldDesc)) == null) {
            updateJoinTableDescs = new HashMap();
            updatedJoinTableRelationships.put(fieldDesc, updateJoinTableDescs);
        }

        UpdateJoinTableDesc desc = null;

        if ((desc = (UpdateJoinTableDesc) updateJoinTableDescs.get(foreignSM)) == null) {
            desc = new UpdateJoinTableDesc(parentSM, foreignSM, action);
            updateJoinTableDescs.put(foreignSM, desc);
        }
    }

    public void clearUpdatedJoinTableRelationships() {
        updatedJoinTableRelationships = null;
    }

    public void recordUpdatedField(LocalFieldDesc fieldDesc) {
        if (!updatedFields.contains(fieldDesc))
            updatedFields.add(fieldDesc);
    }

    public List getUpdatedFields() {
        return updatedFields;
    }

    public Object getAfterValue(FieldDesc f) {
        if (afterImage == null) {
            throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                "sqlstore.sql.updateobjdescimpl.afterimagenull")); //NOI18N
        }

        if (f.absoluteID < 0) {
            return afterHiddenValues.get(-(f.absoluteID + 1));
        } else {
            return f.getValue(afterImage);
        }
    }

    public Object getBeforeValue(FieldDesc f) {
        if (beforeImage == null) {
            throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                "sqlstore.sql.updateobjdescimpl.beforeimagenull")); //NOI18N
        }

        if (f.absoluteID < 0) {
            return beforeHiddenValues.get(-(f.absoluteID + 1));
        } else {
            return f.getValue(beforeImage);
        }
    }

    public int getUpdateAction() {
        return updateAction;
    }

    public ClassDesc getConfig() {
        return (ClassDesc) afterImage.getPersistenceConfig();
    }

    public SQLStateManager getAfterImage() {
        return afterImage;
    }

    public boolean isBeforeImageRequired() {
        return afterImage.isBeforeImageRequired();
    }

    public Concurrency getConcurrency() {
        return concurrency;
    }

    public void setConcurrency(Concurrency concurrency) {
        this.concurrency = concurrency;
    }

    /**
     * We send the AfterImage for updates and inserts
     * but for updates it will only hold values for updated attributes (unless
     * the class is configured to send the whole AfterImage, also we'll let the
     * concurrency interface affect the sent AfterImage (and the sent
     * BeforeImage)).  For deletes the AfterImage will be NIL, for inserts the
     * BeforeImage will be NIL.  For deletes the BeforeImage will contain values
     * for all key attributes.  Also for deletes and updates we'll send the
     * HiddenValues array from the paladin (although we can set to NIL any
     * values in the array not needed by this particular update).
     *
     * UpdatedAttributes will contain indexes into the PersistentDesc.Attributes
     * array for new or updated values.
     *
     * Initially we'll probably just send the whole BeforeImage and AfterImage
     * (except that we won't have an AfterImage for Deletes and we won't have
     * a BeforeImage for updates).
     */
    public void setObjectInfo(StateManager biStateManager,
                              StateManager aiStateManager,
                              int action) {

        this.beforeImage = (SQLStateManager) biStateManager;
        this.afterImage = (SQLStateManager) aiStateManager;
        ClassDesc config = (ClassDesc) afterImage.getPersistenceConfig();
        updateAction = action;

        this.afterHiddenValues = afterImage.hiddenValues;

        if (beforeImage != null) {
            this.beforeHiddenValues = beforeImage.hiddenValues;
        }

        // This pass through attributes we are only going to look at local attributes.
        // These are attributes that are stored in this object and are not references
        // to other persistent objects.

        boolean debug = logger.isLoggable(Logger.FINER);

        for (int i = 0; i < config.fields.size(); i++) {
            FieldDesc f = (FieldDesc) config.fields.get(i);
            LocalFieldDesc lf = null;
            boolean updated = false;

            if (f instanceof LocalFieldDesc) {
                lf = (LocalFieldDesc) f;
            } else {
                continue;
            }

            if ((updateAction == LOG_DESTROY) ||
                    ((lf.sqlProperties & FieldDesc.PROP_RECORD_ON_UPDATE) > 0)) {
                continue;
            } else if (lf.absoluteID < 0) {
                if ((beforeImage == null) ||
                        (beforeImage.getHiddenValue(lf.absoluteID) !=
                        afterImage.getHiddenValue(lf.absoluteID))) {
                    updated = true;
                }
            } else if (lf.getType().isPrimitive() ||
                    String.class == lf.getType() ||
                    java.util.Date.class == lf.getType()) {
                Object afterVal = lf.getValue(afterImage);
                Object beforeVal = null;

                if (beforeImage != null) {
                    beforeVal = lf.getValue(beforeImage);
                }

                if ((beforeVal != null) && (afterVal != null)) {
                    if (!beforeVal.equals(afterVal)) {
                        updated = true;
                    }
                } else {
                    updated = true;
                }
            } else {
                // What else??
            }

            if (updated) {
                if (debug) {
                    logger.finer("sqlstore.sql.updateobjdescimpl.updated", f.getName()); // NOI18N
                }

                updatedFields.add(lf);
            }
        }

        if (concurrency != null) {
            concurrency.commit(this, beforeImage, afterImage, updateAction);
        }
    }

    /**
     * Triggers the version update if the associated state manager is
     * registered for version consistency and database fields have been
     * modified. The version is incremented, if
     * <ul>
     * <li>The associated instance is version consistent.</li>
     * <li>The associated instance has updated database fields.</li>
     * </ul>
     * Note: The version is <b>not</b> incremented, if a relationship
     * mapped to a join table was updated.
     */
    public void incrementVersion() {

        if (afterImage.hasVersionConsistency()
                && updateAction == ActionDesc.LOG_UPDATE
                && hasUpdatedFields()) {

            afterImage.incrementVersion();
        }
    }

    /**
     * Marks the associated state manager as failed.
     */
    public void setVerificationFailed() {
        afterImage.setVerificationFailed();
    }

    public boolean hasVersionConsistency() {
        return afterImage.hasVersionConsistency();
    }
}
