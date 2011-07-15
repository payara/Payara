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
 *  LifeCycleState.java    March 10, 2000    Steffi Rauschenbach
 */

package com.sun.jdo.spi.persistence.support.sqlstore.state;

import com.sun.jdo.api.persistence.support.JDOUserException;
import com.sun.jdo.spi.persistence.utility.logging.Logger;
import com.sun.jdo.spi.persistence.support.sqlstore.LogHelperStateManager;
import org.glassfish.persistence.common.I18NHelper;

import java.util.ResourceBundle;


public abstract class LifeCycleState {

    /**
     * I18N message handler
     */
    protected final static ResourceBundle messages = I18NHelper.loadBundle(
            "com.sun.jdo.spi.persistence.support.sqlstore.Bundle", // NOI18N
            LifeCycleState.class.getClassLoader());

    protected boolean isPersistent;
    protected boolean isAutoPersistent;
    protected boolean isPersistentInDataStore;
    protected boolean isTransactional;
    protected boolean isDirty;
    protected boolean isNew;
    protected boolean isDeleted;
    protected boolean isFlushed;
    protected boolean isNavigable;
    protected boolean isRefreshable;
    protected boolean isBeforeImageUpdatable;

    // The following flags need to be checked after state transition.
    protected boolean needsRegister;

    // The following flags need to be checked before state transition.
    protected boolean needsReload;

    // The following flag determine whether the original state of the object
    // needs to be restored on rollback (my depend on retainValues)
    protected boolean needsRestoreOnRollback;


    // The following flag states that merge is needed
    protected boolean needMerge = true;

    protected int updateAction;
    protected int stateType;


    /**
     * Constants to specify database operation to be executed
     */
    final static protected int
            NO_OP = 0,
    INSERT_OP = 1,
    UPDATE_OP = 2,
    DELETE_OP = 3;

    /**
     * Constants to specify the life cycle state type
     */
    final static public int
            HOLLOW = 0,
    P_NON_TX = 1,
    P_CLEAN = 2,
    P_DIRTY = 3,
    P_NEW = 4,
    P_NEW_FLUSHED = 5,
    P_NEW_FLUSHED_DELETED = 6,
    P_NEW_DELETED = 7,
    P_DELETED = 8,
    P_DELETED_FLUSHED = 9,
    AP_NEW = 10,
    AP_NEW_PENDING = 11,
    AP_NEW_FLUSHED = 12,
    AP_NEW_FLUSHED_PENDING = 13,
    AP_NEW_FLUSHED_DELETED = 14,
    AP_NEW_DELETED = 15,
    TRANSIENT = 16,
    TOTAL = 17;

    private static LifeCycleState stateTypes[];

    //The logger
    private static Logger logger = LogHelperStateManager.getLogger();

    // ******************************************************************
    // Initialisation stuff
    // ******************************************************************

    /**
     * Static initialiser.
     * Initialises the life cycle.
     */
    static {
        initLifeCycleState();
    }

    /**
     * Initialises the objects. This class implements the "state pattern".
     */

    // This method is called (through the static initializer)
    // when the LifeCycleState class or any of its subclasses is loaded.

    // It is extremely important that this method is called before any of isNew etc is called,
    // and before stateType() is called !!!

    protected static void initLifeCycleState() {
        stateTypes = new LifeCycleState[TOTAL];
        stateTypes[HOLLOW] = new Hollow();
        stateTypes[P_NON_TX] = new PersistentNonTransactional();
        stateTypes[P_CLEAN] = new PersistentClean();
        stateTypes[P_DIRTY] = new PersistentDirty();
        stateTypes[P_NEW] = new PersistentNew();
        stateTypes[P_NEW_FLUSHED] = new PersistentNewFlushed();
        stateTypes[P_NEW_DELETED] = new PersistentNewDeleted();
        stateTypes[P_NEW_FLUSHED_DELETED] = new PersistentNewFlushedDeleted();
        stateTypes[P_DELETED] = new PersistentDeleted();
        stateTypes[P_DELETED_FLUSHED] = new PersistentDeletedFlushed();
        stateTypes[AP_NEW] = new AutoPersistentNew();
        stateTypes[AP_NEW_PENDING] = new AutoPersistentNewPending();
        stateTypes[AP_NEW_FLUSHED] = new AutoPersistentNewFlushed();
        stateTypes[AP_NEW_FLUSHED_PENDING] = new AutoPersistentNewFlushedPending();
        stateTypes[AP_NEW_FLUSHED_DELETED] = new AutoPersistentNewFlushedDeleted();
        stateTypes[AP_NEW_DELETED] = new AutoPersistentNewDeleted();
        stateTypes[TRANSIENT] = null;
    }

    /**
     *       | Trans  | PNew   | PClean | PDirty | Hollow | PNewDel | PDel  | PNonTx
     *-----------------------------------------------------------------------------------
     * makeP | PNew   | unch.  | unch.  | unch.  | unch.  | unch.   | unch. | unch.
     *-----------------------------------------------------------------------------------
     * delP  | Error  | PNewDel| PDel   | PDel   | PDel   | unch.   | unch. | PDel
     *-----------------------------------------------------------------------------------
     * readF.| unch.  | unch.  | unch.  | unch.  | PClean | Error   | Error |!unl.:unch.
     * opt=f |        |        |        |        |        |         |       | unl.:PClean
     *-----------------------------------------------------------------------------------
     * readF.| unch.  | unch.  | unch.  | unch.  | PNonTx | Error   | Error | unch.
     * opt=t |        |        |        |        |        |         |       |
     *-----------------------------------------------------------------------------------
     * writeF| unch.  | unch.  | PDirty | unch.  | PDirty | Error   | Error | PDirty
     *-----------------------------------------------------------------------------------
     * commit| unch.  | Hollow | Hollow | Hollow | unch.  | Trans   | Trans | unch.
     *-----------------------------------------------------------------------------------
     * commit| unch.  | PNonTx | PNonTx | PNonTx | unch.  | Trans   | Trans | unch.
     * rt=t  |        |        |        |        |        |         |       |
     *-----------------------------------------------------------------------------------
     * rollb.| unch.  | Trans  | Hollow | Hollow | unch.  | Trans   | Hollow| unch.
     *-----------------------------------------------------------------------------------
     * rollb.| unch.  | Trans  | PNonTx | PNonTx | unch.  | Trans   | PNonTx| unch.
     * rt=t  |        |        |        |        |        |         |       |
     *-----------------------------------------------------------------------------------
     * evict | n/a    | n/a    | Hollow | n/a    | unch.  | n/a     | n/a   | Hollow
     *-----------------------------------------------------------------------------------
     */


    /**
     * Returns the LifeCycleState for the state constant.
     *
     * @param state the type as integer
     * @return the type as LifeCycleState object
     */
    public static LifeCycleState getLifeCycleState(int state) {
        if (logger.isLoggable(Logger.FINER)) {
            logger.finer("sqlstore.state.lifecyclestate.initial",stateTypes[state]); // NOI18N
        }

        return stateTypes[state];
    }

    /**
     * Returns the type of the life cycle state
     *
     * @return the type of this life cycle state
     *
     */
    public int stateType() {
        return stateType;
    }


    public LifeCycleState transitionMakePersistent() {
        return this;
    }

    public LifeCycleState transitionDeletePersistent() {
        return this;
    }

    public LifeCycleState transitionRefreshPersistent() {
        return this;
    }

    public LifeCycleState transitionReload(boolean transactionActive) {
        return this;
    }

    public LifeCycleState transitionCommit(boolean retainValues) {
        return this;
    }

    public LifeCycleState transitionRollback(boolean retainValues) {
        return this;
    }

    public LifeCycleState transitionFlushed() {
        return this;
    }

    public LifeCycleState transitionMakePending() {
        return this;
    }

    public LifeCycleState transitionReadField(boolean optimisitic,
                                              boolean nontransactionalRead,
                                              boolean transactionActive) {
        if (!nontransactionalRead) {
            assertTransaction(transactionActive);
        }

        return this;
    }

    public LifeCycleState transitionWriteField(boolean transactionActive) {
        assertTransaction(transactionActive);
        return this;
    }

    protected void assertTransaction(boolean transactionActive) {
        if (!transactionActive) {
            throw new JDOUserException(I18NHelper.getMessage(messages,
                    "jdo.lifecycle.xactnotactive")); // NOI18N
        }
    }


    /***************************************************************/
    /************** State interrogation methods ********************/
    /***************************************************************/

    /**
     * Return whether the object is persistent.
     */
    public boolean isPersistent() {
        return isPersistent;
    }

    /**
     * Return whether the object is auto-persistent.
     */
    public boolean isAutoPersistent() {
        return isAutoPersistent;
    }

    /**
     * Return whether the object is persistent in data store.
     */
    public boolean isPersistentInDataStore() {
        return isPersistentInDataStore;
    }


    /**
     * Return whether the object is transactional.
     */
    public boolean isTransactional() {
        return isTransactional;
    }

    /**
     * Return whether the object is dirty, i.e. has been changed
     * (created, updated, deleted) in this Tx.
     */
    public boolean isDirty() {
        return isDirty;
    }

    /**
     * Return whether the object was newly created.
     */
    public boolean isNew() {
        return isNew;
    }

    /**
     * Return whether the object is deleted.
     */
    public boolean isDeleted() {
        return isDeleted;
    }

    /**
     * Return whether the object is registered with the persistence manager.
     */
    public boolean needsRegister() {
        return needsRegister;
    }

    /**
     * Return whether the object can dynamically navigate to fields that are
     * not present.
     */
    public boolean isNavigable() {
        return isNavigable;
    }

    /**
     * Return whether the object can be refreshed from the database.
     */
    public boolean isRefreshable() {
        return isRefreshable;
    }

    public boolean isBeforeImageUpdatable() {
        return isBeforeImageUpdatable;
    }


    public boolean needsReload(boolean optimistic,
                               boolean nontransactionalRead,
                               boolean transactionActive) {
        return needsReload;
    }

    public boolean needsRestoreOnRollback(boolean retainValues) {
        //
        // The default behavior is if retainValues is true, we always
        // restore the state. Otherwise, the behavior is determined
        // by needsRetoreOnRollback.
        //
        if (retainValues) {
            return true;
        }

        return needsRestoreOnRollback;
    }

    public boolean needMerge() {
        return needMerge;
    }

    public int getUpdateAction() {
        return updateAction;
    }

    /*************************************************************/
    /********************* Helper methods ************************/
    /********* Called only internally by life cycle classes ******/
    /*************************************************************/

    /**
     * Life Cycle State change
     */
    public LifeCycleState changeState(int newStateType) {
        if (logger.isLoggable(Logger.FINER)) {
            Object[] items = new Object[] {this,stateTypes[newStateType]};
            logger.finer("sqlstore.state.lifecyclestate.changestate",items); // NOI18N
        }

        return (stateTypes[newStateType]);
    }

    public String toString() {
        switch (stateType) {
            case HOLLOW:
                return "HOLLOW"; // NOI18N
            case P_NON_TX:
                return "P_NON_TX"; // NOI18N
            case P_CLEAN:
                return "P_CLEAN"; // NOI18N
            case P_DIRTY:
                return "P_DIRTY"; // NOI18N
            case P_NEW:
                return "P_NEW"; // NOI18N
            case P_NEW_FLUSHED:
                return "P_NEW_FLUSHED"; // NOI18N
            case P_NEW_FLUSHED_DELETED:
                return "P_NEW_FLUSHED_DELETED"; // NOI18N
            case P_NEW_DELETED:
                return "P_NEW_DELETED"; // NOI18N
            case P_DELETED:
                return "P_DELETED"; // NOI18N
            case P_DELETED_FLUSHED:
                return "P_DELETED_FLUSHED"; // NOI18N
            case AP_NEW:
                return "AP_NEW"; // NOI18N
            case AP_NEW_PENDING:
                return "AP_NEW_PENDING"; // NOI18N
            case AP_NEW_FLUSHED:
                return "AP_NEW_FLUSHED"; // NOI18N
            case AP_NEW_FLUSHED_PENDING:
                return "AP_NEW_FLUSHED_PENDING"; // NOI18N
            case AP_NEW_FLUSHED_DELETED:
                return "AP_NEW_FLUSHED_DELETED"; // NOI18N
            case AP_NEW_DELETED:
                return "AP_NEW_DELETED"; //NOI18N
        }

        return null;
    }

}


