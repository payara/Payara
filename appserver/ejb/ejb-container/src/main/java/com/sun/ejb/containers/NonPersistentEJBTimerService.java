/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2018] [Payara Foundation and/or its affiliates]

package com.sun.ejb.containers;

import static com.sun.ejb.containers.EJBTimerService.logger;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import static java.util.logging.Level.WARNING;
import javax.ejb.FinderException;

public class NonPersistentEJBTimerService extends EJBTimerService {

    protected NonPersistentEJBTimerService() throws Exception {
        super();
    }

    @Override
    public boolean isPersistent() {
        return false;
    }

    @Override
    public int migrateTimers(String fromOwnerId) {
        throw new IllegalStateException("Non-persistent timers cannot be migrated");
    }

    @Override
    protected void cancelTimer(TimerPrimaryKey timerId)
            throws FinderException, Exception {
        cancelNonPersistentTimer(timerId);
    }

    @Override
    protected void cancelTimersByKey(long containerId, Object primaryKey) {
         Collection<TimerPrimaryKey> timerIds = getTimerIds(containerId, primaryKey);
         for(TimerPrimaryKey timerId : timerIds) {
             try {
                 cancelTimer(timerId);
             } catch (Exception ex) {
                 logger.log(WARNING, "Cannot cancel non-persistent timer " + timerId, ex);
             }
         }
    }

    @Override
    protected Date getNextTimeout(TimerPrimaryKey timerId) throws FinderException {

        RuntimeTimerState rt = getNonPersistentTimer(timerId);
        if (rt != null) {
            return _getNextTimeout(rt);
        }

        throw new FinderException("Timer does not exist");
    }

    @Override
    protected boolean isValidTimerForThisServer(
            TimerPrimaryKey timerId,
            RuntimeTimerState timerState) {
        return true;
    }

    protected boolean cancelNonPersistentTimer(TimerPrimaryKey timerId)
            throws FinderException, Exception {

        RuntimeTimerState rt = getNonPersistentTimerState(timerId);
        if (rt != null) {
            if (rt.isCancelled()) {
                // already cancelled or removed
                return true;
            }

            cancelTimerSynchronization(null, timerId,
                    rt.getContainerId(), ownerIdOfThisServer_, false);
        }

        return (rt != null);
    }


    private RuntimeTimerState getNonPersistentTimerState(TimerPrimaryKey timerId) {
        return timerCache_.getNonPersistentTimerState(timerId);
    }

    // Returns a Set of active non-persistent timer ids for this server
    @Override
    public Set<TimerPrimaryKey> getNonPersistentActiveTimerIdsByThisServer() {
        return timerCache_.getNonPersistentActiveTimerIdsByThisServer();
    }

    protected RuntimeTimerState getNonPersistentTimer(TimerPrimaryKey timerId)
            throws FinderException {
        RuntimeTimerState rt = getNonPersistentTimerState(timerId);
        if (rt != null) {
            if (rt.isCancelled()) {
                // The timer has been cancelled within this tx.
                throw new FinderException("Non-persistent timer " + timerId
                        + " does not exist");
            }
        }

        return rt;
    }

    @Override
     protected EJBTimerSchedule getTimerSchedule(TimerPrimaryKey timerId) throws FinderException {

        // Check non-persistent timers first
        EJBTimerSchedule ts = null;

        RuntimeTimerState rt = getNonPersistentTimer(timerId);
        if (rt != null) {
            ts = rt.getTimerSchedule();
        }

        return ts;
    }

    @Override
    protected boolean isPersistent(TimerPrimaryKey timerId) throws FinderException {

        // Check non-persistent timers only
        RuntimeTimerState rt = getNonPersistentTimer(timerId);
        return rt == null;
    }

    @Override
    protected boolean timerExists(TimerPrimaryKey timerId) {
        boolean exists = false;

        // Check non-persistent timers only
        RuntimeTimerState rt = getNonPersistentTimerState(timerId);
        if (rt != null) {
            exists = rt.isActive();
        }

        return exists;
    }

    @Override
       protected Serializable getInfo(TimerPrimaryKey timerId) throws FinderException {

        // Check non-persistent timers only
        RuntimeTimerState rt = getNonPersistentTimer(timerId);
        if (rt != null) {
            return rt.getInfo();
        }

        return null;
    }

    @Override
    protected Collection<TimerPrimaryKey> getTimerIds(Collection<Long> containerIds) {
        Collection<TimerPrimaryKey> timerIds = new HashSet<>();
        for (long containerId : containerIds) {
            timerIds.addAll(timerCache_.getNonPersistentActiveTimerIdsForContainer(containerId));
        }
        return timerIds;
    }

    @Override
    protected Collection<TimerPrimaryKey> getTimerIds(
            long containerId,
            Object timedObjectPrimaryKey) {

        Collection<TimerPrimaryKey> timerIdsForTimedObject = new HashSet<>();

        // Add active non-persistent timer ids
        timerIdsForTimedObject.addAll(
                timerCache_.getNonPersistentActiveTimerIdsForContainer(containerId));

        return timerIdsForTimedObject;
    }

    @Override
    protected void stopTimers(long containerId) {
        stopTimers(timerCache_.getNonPersistentTimerIdsForContainer(containerId));
    }

    @Override
    protected void resetEJBTimers(String target) {
        // Do nothing
    }

    @Override
    protected void resetLastExpiration(
            TimerPrimaryKey timerId,
            RuntimeTimerState timerState) {
        // Do nothing
    }

}
