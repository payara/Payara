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

package com.sun.jdo.spi.persistence.support.sqlstore;

/**
 * An object that manages the state transitions and the contents of the
 * fields of a JDO Instance.
 *
 * If a JDO Instance is persistent or transactional, it contains a
 * non-null reference to a JDO <code>StateManager</code> instance which is
 * responsible for managing the JDO Instance state changes and for
 * interfacing with the JDO PersistenceManager.
 *
 * Additionally, Persistent JDO Instances refers to an instance of the
 * JDO <code>StateManager</code> instance responsible for the state
 * transitions of the instance as well as managing the contents of the
 * fields of the instance.
 *
 * The JDO <code>StateManager</code> interface is the primary interface used
 * by the JDO Instance to mediate life cycle changes.  Non-transient JDO
 * Instances always contain a non-null reference to an associated JDO
 * <code>StateManager</code> instance.
 *
 * When a First Class Object is instantiated in the JVM, the JDO
 * implementation assigns to fields with a Tracked Second Class Object
 * type a new instance that tracks changes made to itself, and
 * notifies the <code>StateManager</code> of the owning First Class Object
 * of the change.
 */
public interface StateManager
{
    static final byte LOAD_REQUIRED  = (byte)1;

    static final byte READ_OK        = (byte)-1;

    static final byte READ_WRITE_OK  = (byte)0;


    /**
     * The <code>PersistenceManager</code> needs to call this method
     * in order to make an instance persistent.
     */
    void makePersistent(PersistenceManager pm, Object pc);

    /**
     * ...
     *
     * The <code>PersistenceManager</code> calls this method to delete
     * a persistent instance.
     */
    void deletePersistent();

    /**
     * ...
     *
     * The <code>PersistenceManager</code> calls this method to flush
     * changes made to the <code>StateManager</code> to the database.
     * The specified StateManager argument is used to determine whether the
     * actual instance should be flushed immediately or whether batch update
     * is possible.
     */
    void updatePersistent(StateManager next);

    /**
     * ...
     *
     * The <code>PersistenceManager</code> calls this method to refresh
     * the state of the <code>StateManager</code> from the database.
     */
    void refreshPersistent();

    /**
     *  ...
     *
     * The <code>PersistenceManager</code> calls this method to inform
     * the <code>StateManager</code> that the transaction has been committed.
     */
    void commit(boolean retainValues);


    /**
     *  ...
     *
     * The <code>PersistenceManager</code> calls this method to inform
     * the <code>StateManager</code> that the transaction has been rolled back.
     */
    void rollback(boolean retainValues);

    /**
     *  ...
     *
     * The <code>PersistenceManager</code> calls this method to inform
     * the <code>StateManager</code> that the flush processing is completed.
     */
    void flushed();

    /**
     * ...
     * The <code>PersistenceManager</code> calls this method to verify
     * that corresponding object has been flushed to the database
     */
    boolean isProcessed();

    /**
     * ...
     */
    void setPersistenceManager(com.sun.jdo.api.persistence.support.PersistenceManager pm);

    /**
     * ...
     */
    com.sun.jdo.api.persistence.support.PersistenceManager getPersistenceManager();

    /**
     * ...
     */
    com.sun.jdo.api.persistence.support.PersistenceManager getPersistenceManagerInternal();

    /**
     * set actualImage associated with this <code>StateManager</code>
     */
    void setPersistent(Object obj);

    /**
     * get actualImage associated with this <code>StateManager</code>
     */
    Object getPersistent();

    /**
     * ...
     */
    void setObjectId(Object objectId);

    /**
     * ...
     */
    Object getObjectId();

    /**
     * ...
     */
    PersistenceConfig getPersistenceConfig();

    /**
     * State initialization
     * @param persistentInDB  true if object is persistent in DB
     */
    void initialize(boolean persistentInDB);

    /**
     * ...
     */
    void makePresent(String fieldName, Object value);

    /**
     * ...
     */
    void makeDirty(String fieldName);

    /**
     * ...
     */
    void applyUpdates(String fieldName, SCOCollection c);

    /**
     * ...
     */
    void replaceObjectField(String fieldName, Object o);

    /* The <code>PersistenceManager</code> calls this method to prepare
     * a persistent object for update. This is required for
     * foreign fields only as they could reference "regular" jdk
     * Collections vs. SCO Collections. Such process has the side-effect of
     * causing more objects to be registered with the transaction cache.
     */
    void prepareToUpdatePhaseI();

    /**
     * This is the second phase of the commit processing. It populates phase3sms with all
     * the autopersistence instances that are no longer reachable from a persistent instance.
     */
    void prepareToUpdatePhaseII(java.util.HashSet phase3sms);

    /**
     * This is the third phase of commit processing. It sets up the delete dependencies among
     * all the autopersistent instances that have been flushed to the database.
     */
    void prepareToUpdatePhaseIII();

    /**
     * ...
     */
    //@olsen: changed to return byte instead of void (->PC.jdoSetFlags())
    byte setFlags(byte flags);

    /**
     * ...
     */
    void loadForRead();

    /**
     * ...
     */
    void loadForUpdate();

    //
    // Status interrogation methods
    //
    // For each one of these methods, there is a corresponding version
    // of it prefixed with jdo on the PersistenceCapable class. These
    // methods are used to query the state o an instance. For example,
    // when jdoIsReadReady is called on the PersistenceCapable
    // instance, the generated <code>jdoIsReadReady</code> will delegate the
    // status interrogation to the <code>StateManager</code> by call
    // <code>isReadReady()</code>.
    //

    /*
     * ...
     */
    boolean isDirty();

    /**
     * ...
     */
    boolean isTransactional();

    /**
     * ...
     */
    boolean isNew();

    /**
     * ...
     */
    boolean isDeleted();

    /**
     * ...
     */
    boolean isPersistent();

    /**
     * @return True, if this instance is persistent, transactional, not new,
     * not dirty, and not deleted; false otherwise.
     */
    boolean needsRegisterWithVersionConsistencyCache();

    /**
     * @return True, if this instance should be synchronized with
     * the version consistency cache; false otherwise.
     */
    boolean needsUpdateInVersionConsistencyCache();

    //
    // Getter and setter methods
    //
    // These are methods for accessing the persistent field values
    // from the <code>StateManager</code>. The getter method can also serve
    // as the hook for triggering dynamic navigation for fields that have not
    // been fetched. The setter methods can also serve as the hook for
    // keeping track of changes made to the <code>StateManager</code>.
    //

    //@olsen: changed to use 'int' instead of 'short' as field number type
    //@olsen: changed setter methods to return value instead of void
    //@olsen: added method prepareGetField()

    void prepareGetField(int fieldID);

    boolean setBooleanField(int fieldNumber, boolean value);

    boolean[] setBooleanArrayField(int fieldNumber, boolean[] value);

    byte setByteField(int fieldNumber, byte value);

    byte[] setByteArrayField(int fieldNumber, byte[] value);

    short setShortField(int fieldNumber, short value);

    short[] setShortArrayField(int fieldNumber, short[] value);

    int setIntField(int fieldNumber, int value);

    int[] setIntArrayField(int fieldNumber, int[] value);

    long setLongField(int fieldNumber, long value);

    long[] setLongArrayField(int fieldNumber, long[] value);

    char setCharField(int fieldNumber, char value);

    char setCharArrayField(int fieldNumber, char value);

    float setFloatField(int fieldNumber, float value);

    float[] setFloatArrayField(int fieldNumber, float[] value);

    double setDoubleField(int fieldNumber, double value);

    double[] setDoubleArrayField(int fieldNumber, double[] value);

    String setStringField(int fieldNumber, String value);

    String[] setStringArrayField(int fieldNumber, String[] value);

    Object setObjectField(int fieldNumber, Object value);

    Object[] setObjectArrayField(int fieldNumber, Object[] value);

    /**
     * Lock this instance. This method must be called the same
     * number of times as #releaseLock().
     */
    void getLock();

    /**
     * Release this instance. This method must be called the same
     * number of times as #getLock().
     */
    void releaseLock();

    /**
     * Returns value for a hidden field. This method expects index
     * to be negative for hidden fields.
     * @param index  - the hidden field index.
     */
    Object getHiddenValue(int index);

    /**
     * Sets value for a hidden field. This method expects index
     * to be negative for hidden fields.
     * @param index  - the hidden field index.
     * @param value - new value.
     */
    void setHiddenValue(int index, Object value);

    /**
     * Marks field with this index as present.
     * @param index - the field number.
     */
     void setPresenceMaskBit(int index) ;

    /**
     * Returns true if field with this index is present in the instance.
     */
    boolean getPresenceMaskBit(int index);

    /**
     * Notifies the StateManager that this instance needs to be registered
     * with the global (weak) cache at rollback if it transitions to persistent
     * state.
     * Used for replacing a deleted instance with the newly persistent with
     * the same object id.
     */
    void markNotRegistered();

    /**
     * Notifies the StateManager that this instance needs to be verified at
     * the time it is removed from the global (weak) cache at rollback if it
     * transitions to transient state.
     */
    void markVerifyAtDeregister();

    /**
     * Adds another StateManager to this StateManager dependencies list.
     * @param sm the StateManager to add.
     */
    void addDependency(StateManager sm);

    /**
     * Tries to resolve the dependencies for all instances waiting for the
     * current state manager to be flushed to the store.
     */
    void resolveDependencies();

    /**
     * Notifies the StateManager that this instance is a replacement for a
     * deleted instance with the same ObjectId.
     */
    void markReplacement();

    /**
     * Release references in the StateManager to the persistent instance,
     * ObjectId, and PersistenceManager.
     */
    void release();

    /** Returns true if this StateManager is valid for use.
     * The <code>valid</code> flag is initially set to false and changed to true
     * when the first operation (e.g. makePersistent(), loadForRead(), or
     * PersistenceManager.getObjectById()) succeeds.
     */
    boolean isValid();

    /** Mark this StateManager as valid. Called before returning from
     * getObjectById. Flag is set to true internally in the StateManager
     * at makePersistent(), or initialize(true) (to be used for storing
     * query or navigation results.
     */
    void setValid();

    /** Reload the instance associated with this StateManager. Called by
     * {@link PersistenceManager#getObjectById(Object, boolean)
     * PersistenceManager.getObjectById(Object, boolean)} with validate
     * flag set to <code>true</code>
     */
    void reload();

    /**
     * Returns true, if the managed instance has Version Consistency.
     * @return  True, if the managed object has Version Consistency.
     */
    boolean hasVersionConsistency();

    /**
     * Copies field values from <code>source</code> to this
     * StateManager's fields.
     * @param source StateManager from which field values are
     * copied into this instance.
     */
    void copyFields(StateManager source);

    /**
     * Verify that an instance set up with Version consistency is not modified
     * in a parallel transaction.
     * @return false if the instance is persistent clean and modified by a
     * parallel transaction, true otherwise.
     */
    boolean verifyPersistent();

    /**
     * Marks that this state manager has failed version consistency
     * validation.
     */
    void setVerificationFailed();

    /**
     * Returns, if this state manager has failed version consistency
     * validation.
     *
     * @return True, if this state manager is marked as failed.
     */
    boolean isVerificationFailed();
}
