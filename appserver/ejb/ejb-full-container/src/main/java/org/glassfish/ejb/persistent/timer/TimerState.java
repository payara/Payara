/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2016-2018] [Payara Foundation and/or its affiliates]

package org.glassfish.ejb.persistent.timer;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Lob;
import javax.persistence.NamedQuery;
import javax.persistence.NamedQueries;
import javax.persistence.PostLoad;
import javax.persistence.Table;
import javax.persistence.Transient;

import java.io.Serializable;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.ejb.EJBUtils;
import com.sun.ejb.containers.EJBTimerService;
import com.sun.ejb.containers.EJBTimerSchedule;
import com.sun.logging.LogDomains;

/**
 * TimerState represents the state of a persistent EJB Timer.
 * It is part of the EJB container and is implemented as an Entity.
 *
 * @author Marina Vatkina
 */
@Entity(name="Timer")
@NamedQueries({
    @NamedQuery(
        name="findTimerIdsByContainer",
        query="SELECT t.timerId FROM Timer t WHERE t.containerId = ?1"
    ),
    @NamedQuery(
        name="findTimerIdsByContainerAndState",
        query="SELECT t.timerId FROM Timer t WHERE t.containerId = ?1 AND t.state=?2"
    ),
    @NamedQuery(
        name="findTimerIdsByContainersAndState",
        query="SELECT t.timerId FROM Timer t WHERE t.state=?2 AND t.containerId IN ?1"
    ),
    @NamedQuery(
        name="findTimerIdsByContainerAndOwner",
        query="SELECT t.timerId FROM Timer t WHERE t.containerId = ?1 AND t.ownerId=?2"
    ),
    @NamedQuery(
        name="findTimerIdsByContainerAndOwnerAndState",
        query="SELECT t.timerId FROM Timer t WHERE t.containerId = ?1 AND t.ownerId=?2 AND t.state=?3"
    ),
    @NamedQuery(
        name="findTimerIdsByOwner",
        query="SELECT t.timerId FROM Timer t WHERE t.ownerId = ?1"
    ),
    @NamedQuery(
        name="findTimerIdsByOwnerAndState",
        query="SELECT t.timerId FROM Timer t WHERE t.ownerId = ?1 AND t.state=?2"
    ),
    @NamedQuery(
        name="findTimersByContainer",
        query="SELECT t FROM Timer t WHERE t.containerId = ?1"
    ),
    @NamedQuery(
        name="findTimersByContainerAndState",
        query="SELECT t FROM Timer t WHERE t.containerId = ?1 AND t.state=?2"
    ),
    @NamedQuery(
        name="findTimersByContainerAndOwner",
        query="SELECT t FROM Timer t WHERE t.containerId = ?1 AND t.ownerId=?2"
    ),
    @NamedQuery(
        name="findTimersByContainerAndOwnerAndState",
        query="SELECT t FROM Timer t WHERE t.containerId = ?1 AND t.ownerId=?2 AND t.state=?3"
    ),
    @NamedQuery(
        // Used also for timer migration, so needs to have predictable return order
        name="findTimersByOwner",
        query="SELECT t FROM Timer t WHERE t.ownerId = ?1 ORDER BY t.timerId"
    ),
    @NamedQuery(
        name="findTimersByOwnerAndState",
        query="SELECT t FROM Timer t WHERE t.ownerId = ?1 AND t.state=?2"
    ),
    @NamedQuery(
        name="countTimersByApplication",
        query="SELECT COUNT(t) FROM Timer t WHERE t.applicationId = ?1"
    ),
    @NamedQuery(
        name="countTimersByOwner",
        query="SELECT COUNT(t) FROM Timer t WHERE t.ownerId = ?1"
    ),
    @NamedQuery(
        name="countTimersByOwnerAndState",
        query="SELECT COUNT(t) FROM Timer t WHERE t.ownerId = ?1 AND t.state=?2"
    ),
    @NamedQuery(
        name="countTimersByContainer",
        query="SELECT COUNT(t) FROM Timer t WHERE t.containerId = ?1"
    ),
    @NamedQuery(
        name="countTimersByContainerAndState",
        query="SELECT COUNT(t) FROM Timer t WHERE t.containerId = ?1 AND t.state=?2"
    ),
    @NamedQuery(
        name="countTimersByContainerAndOwner",
        query="SELECT COUNT(t) FROM Timer t WHERE t.containerId = ?1 AND t.ownerId=?2"
    ),
    @NamedQuery(
        name="countTimersByContainerAndOwnerAndState",
        query="SELECT COUNT(t) FROM Timer t WHERE t.containerId = ?1 AND t.ownerId=?2 AND t.state=?3"
    )
    ,
    @NamedQuery(
        name="updateTimersFromOwnerToNewOwner",
        query="UPDATE Timer t SET t.ownerId = :toOwner WHERE t.ownerId = :fromOwner"
    )
    ,
    @NamedQuery(
        name="deleteTimersByContainer",
        query="DELETE FROM Timer t WHERE t.containerId = :containerId"
    )
    ,
    @NamedQuery(
        name="deleteTimersByApplication",
        query="DELETE FROM Timer t WHERE t.applicationId = :applicationId"
    )
})
@Table(name="EJB__TIMER__TBL")
@IdClass(com.sun.ejb.containers.TimerPrimaryKey.class)
public class TimerState {

    //
    // Persistence fields and access methods
    //

    @Id
    @Column(name="TIMERID")
    private String timerId;

    @Column(name="CREATIONTIMERAW")
    private long creationTimeRaw;

    @Column(name="INITIALEXPIRATIONRAW")
    private long initialExpirationRaw;

    @Column(name="LASTEXPIRATIONRAW")
    private long lastExpirationRaw;

    @Column(name="INTERVALDURATION")
    private long intervalDuration;

    @Column(name="STATE")
    private int state;

    @Column(name="CONTAINERID")
    private long containerId;

    @Column(name="APPLICATIONID")
    private long applicationId;

    @Column(name="PKHASHCODE")
    private int pkHashCode;

    @Column(name="OWNERID")
    private String ownerId;

    @Column(name="SCHEDULE")
    private String schedule;

    @Lob
    @Basic(fetch=FetchType.LAZY)
    @Column(name="BLOB")
    private Blob blob;

    // primary key
    String getTimerId() {
        return timerId;
    }

    void setTimerId(String timerId) {
        this.timerId = timerId;
    }

    String getOwnerId() {
        return ownerId;
    }

    void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    long getCreationTimeRaw() {
        return creationTimeRaw;
    }

    void setCreationTimeRaw(long creationTime) {
        creationTimeRaw = creationTime;
    }

    long getInitialExpirationRaw() {
        return initialExpirationRaw;
    }

    void setInitialExpirationRaw(long initialExpiration) {
        initialExpirationRaw = initialExpiration;
    }

    long getLastExpirationRaw() {
        return lastExpirationRaw;
    }

    void setLastExpirationRaw(long lastExpiration) {
        lastExpirationRaw = lastExpiration;
    }

    long getIntervalDuration() {
        return intervalDuration;
    }

    void setIntervalDuration(long intervalDuration) {
        this.intervalDuration = intervalDuration;
    }

    int getState() {
        return state;
    }

    void setState(int state) {
        this.state = state;
    }

    long getContainerId() {
        return containerId;
    }

    void setContainerId(long containerId) {
        this.containerId = containerId;
    }

    long getApplicationId() {
        return applicationId;
    }

    void setApplicationId(long applicationId) {
        this.applicationId = applicationId;
    }

    String getSchedule() {
        return schedule;
    }

    void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    Blob getBlob() {
        return blob;
    }

    void setBlob(Blob blob) {
        this.blob = blob;
    }

    int getPkHashCode() {
        return pkHashCode;
    }

    void setPkHashCode(int pkHash) {
        pkHashCode = pkHash;
    }

    EJBTimerSchedule getTimerSchedule() {
        return timerSchedule_;
    }

    //
    // These data members contain derived state for
    // some immutable fields.
    //

    // deserialized state from blob
    @Transient
    private boolean blobLoaded_;

    @Transient
    private Object timedObjectPrimaryKey_;

    @Transient
    private Serializable info_;

    // Dates
    @Transient
    private Date creationTime_;

    @Transient
    private Date initialExpiration_;

    @Transient
    private Date lastExpiration_;

    @Transient
    private EJBTimerSchedule timerSchedule_;

    TimerState () {
    }

    TimerState (String timerId, long containerId, long applicationId,
             String ownerId, Object timedObjectPrimaryKey,
             Date initialExpiration, long intervalDuration,
             EJBTimerSchedule schedule, Serializable info) throws IOException {

        this.timerId = timerId;
        this.ownerId = ownerId;

        creationTime_ = new Date();
	creationTimeRaw = creationTime_.getTime();

        initialExpirationRaw = initialExpiration.getTime();
        initialExpiration_ = initialExpiration;

        lastExpirationRaw = 0;
        lastExpiration_ = null;

        this.intervalDuration = intervalDuration;
        timerSchedule_ = schedule;
        if (timerSchedule_ != null) {
            this.schedule = timerSchedule_.getScheduleAsString();
        }

        this.containerId = containerId;
        this.applicationId = applicationId;

        timedObjectPrimaryKey_  = timedObjectPrimaryKey;
        info_ = info;
        blobLoaded_ = true;

        blob = new Blob(timedObjectPrimaryKey, info);
        state = EJBTimerService.STATE_ACTIVE;
    }

    String stateToString() {
        return EJBTimerService.timerStateToString(state);
    }

    private void loadBlob(ClassLoader cl) {
        try {
            timedObjectPrimaryKey_  = blob.getTimedObjectPrimaryKey(cl);
            info_ = blob.getInfo(cl);
            blobLoaded_ = true;
        } catch(Exception e) {
            RuntimeException ex = new RuntimeException();
            ex.initCause(e);
            throw ex;
        }
    }

    @PostLoad
    void load() {

        lastExpiration_ = (lastExpirationRaw > 0) ?
            new Date(lastExpirationRaw) : null;

        // Populate derived state of immutable persistent fields.
        creationTime_ = new Date(creationTimeRaw);
        initialExpiration_ = new Date(initialExpirationRaw);
        if (schedule != null) {
            timerSchedule_ = new EJBTimerSchedule(schedule);
        }

        // Lazily deserialize Blob state.  This makes the
        // Timer bootstrapping code easier, since some of the Timer
        // state must be loaded from the database before the
        // container and application classloader are known.
        timedObjectPrimaryKey_ = null;
        info_       = null;
        blobLoaded_ = false;
    }

    boolean repeats() {
        return (intervalDuration > 0);
    }

    Serializable getInfo() {
        if (!blobLoaded_) {
            loadBlob(
                    EJBTimerService
                            .getPersistentTimerService()
                            .getTimerClassLoader(getContainerId())
            );
        }
        return info_;
    }

    Object getTimedObjectPrimaryKey() {
        if (!blobLoaded_) {
            loadBlob(
                    EJBTimerService
                            .getPersistentTimerService()
                            .getTimerClassLoader(getContainerId())
            );
        }
        return timedObjectPrimaryKey_;
    }

    Date getCreationTime() {
        return creationTime_;
    }

    Date getInitialExpiration() {
        return initialExpiration_;
    }

    Date getLastExpiration() {
        return lastExpiration_;
    }

    void setLastExpiration(Date lastExpiration) {
        // can be null
        lastExpiration_ = lastExpiration;
        lastExpirationRaw = (lastExpiration != null) ?
            lastExpiration.getTime() : 0;
    }

    boolean isActive() {
        return (state == EJBTimerService.STATE_ACTIVE);
    }

    boolean isCancelled() {
        return (state == EJBTimerService.STATE_CANCELLED);
    }

    /**
     * Many DBs have a limitation that at most one field per DB
     * can hold binary data.  As a workaround, store both EJBLocalObject
     * and "info" as a single Serializable blob.  This is necessary
     * since primary key of EJBLocalObject could be a compound object.
     * This class also isolates the portion of Timer data that is
     * associated with the TimedObject itself.  During deserialization,
     * we must use the application class loader for the timed object,
     * since both the primary key and info object can be application
     * classes.
     *
     */
    static class Blob implements Serializable {

        private byte[] primaryKeyBytes_ = null;
        private byte[] infoBytes_ = null;

        // Allow deserialization even if the class has changed
        private static final long serialVersionUID = 5022674828003386360L;

        private static final Logger logger = LogDomains.getLogger(TimerState.class, LogDomains.EJB_LOGGER);

        Blob() {
        }

        Blob(Object primaryKey, Serializable info)
            throws IOException {
            if( primaryKey != null ) {
                primaryKeyBytes_ = EJBUtils.serializeObject(primaryKey);
            }
            if( info != null ) {
                infoBytes_ = EJBUtils.serializeObject(info);
            }
        }

        // To be used to replace TimerBean.Blob on v2.x upgrade
        Blob(byte[] primaryKeyBytes, byte[] infoBytes) {
            primaryKeyBytes_ = primaryKeyBytes;
            infoBytes_ = infoBytes;
        }

        Object getTimedObjectPrimaryKey(ClassLoader cl)
            throws Exception {
            Object pKey = null;
            if( primaryKeyBytes_ != null) {
                pKey = EJBUtils.deserializeObject(primaryKeyBytes_, cl, 0L);
                if( logger.isLoggable(Level.FINER) ) {
                    logger.log(Level.FINER, "Deserialized blob : " + pKey);
                }
            }
            return pKey;
        }

        Serializable getInfo(ClassLoader cl) throws Exception {
            Serializable info = null;
            if( infoBytes_ != null) {
                info = (Serializable)EJBUtils.deserializeObject(infoBytes_, cl, 0L);
                if( logger.isLoggable(Level.FINER) ) {
                    logger.log(Level.FINER, "Deserialized blob : " + info);
                }
            }
            return info;
        }
    }

}
