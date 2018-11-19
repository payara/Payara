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
// Portions Copyright [2018] [Payara Foundation and/or its affiliates]

package com.sun.ejb.containers;

import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import java.util.Date;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.io.Serializable;
import javax.ejb.TimerService;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.ScheduleExpression;
import javax.ejb.EJBException;
import javax.ejb.CreateException;
import javax.ejb.EntityContext;

/*
 * EJBTimerServiceWrappers is the application-level representation
 * of the EJB timer service. 
 *
 * @author Kenneth Saks
 */
public class EJBTimerServiceWrapper implements TimerService {

    private EJBTimerService persistentTimerService_;
    private EJBTimerService nonPersistentTimerService_;
    private EJBContextImpl ejbContext_;
    private EjbDescriptor ejbDescriptor_;

    private boolean entity_;

    // Only used for entity beans
    private Object timedObjectPrimaryKey_;

    public EJBTimerServiceWrapper(
            EJBTimerService persistentTimerService,
            EJBTimerService nonPersistentTimerService,
            EJBContextImpl ejbContext) {
        persistentTimerService_ = persistentTimerService;
        nonPersistentTimerService_ = nonPersistentTimerService;
        ejbContext_   = ejbContext;
        BaseContainer container = (BaseContainer) ejbContext.getContainer();
        ejbDescriptor_ = container.getEjbDescriptor();
        entity_       = false;
        timedObjectPrimaryKey_   = null;
    }

    public EJBTimerServiceWrapper(
            EJBTimerService persistentTimerService,
            EJBTimerService nonPersistentTimerService,
            EntityContext entityContext) {
        this(persistentTimerService, nonPersistentTimerService, ((EJBContextImpl) entityContext));
        entity_ = true;
        // Delay access of primary key since this might have been called 
        // from ejbCreate
        timedObjectPrimaryKey_ = null;
    }

    @Override
    public Timer createTimer(long duration, Serializable info) 
        throws IllegalArgumentException, IllegalStateException, EJBException {

        checkCreateTimerCallPermission();
        checkDuration(duration);

        return createTimerInternal(duration, 0, info);
    }

    @Override
    public Timer createTimer(
            long initialDuration,
            long intervalDuration,
            Serializable info
    ) throws IllegalArgumentException, IllegalStateException, EJBException {

        checkCreateTimerCallPermission();
        checkInitialDuration(initialDuration);

        return createTimerInternal(initialDuration, intervalDuration, info);
    }

    @Override
    public Timer createTimer(Date expiration, Serializable info) 
        throws IllegalArgumentException, IllegalStateException, EJBException {
                             
        checkCreateTimerCallPermission();
        checkExpiration(expiration);

        return createTimerInternal(expiration, 0, info);
    }

    @Override
    public Timer createTimer(
            Date initialExpiration,
            long intervalDuration,
            Serializable info
    ) throws IllegalArgumentException, IllegalStateException, EJBException {

        checkCreateTimerCallPermission();
        checkExpiration(initialExpiration);

        return createTimerInternal(initialExpiration, intervalDuration, info);
    }

    @Override
    public Timer createSingleActionTimer(long duration, TimerConfig timerConfig)
            throws IllegalArgumentException, IllegalStateException, EJBException {
        checkCreateTimerCallPermission();
        checkDuration(duration);

        return createTimerInternal(duration, 0, timerConfig);
    }

    @Override
    public Timer createIntervalTimer(
            long initialDuration,
            long intervalDuration,
            TimerConfig timerConfig
    ) throws IllegalArgumentException, IllegalStateException, EJBException {
        checkCreateTimerCallPermission();
        checkInitialDuration(initialDuration);

        return createTimerInternal(initialDuration, intervalDuration, timerConfig);
    }

    @Override
    public Timer createSingleActionTimer(
            Date initialExpiration,
            TimerConfig timerConfig
    ) throws IllegalArgumentException, IllegalStateException, EJBException {
        checkCreateTimerCallPermission();
        checkExpiration(initialExpiration);

        return createTimerInternal(initialExpiration, 0, timerConfig);
    }

    @Override
    public Timer createIntervalTimer(
            Date initialExpiration,
            long intervalDuration,
            TimerConfig timerConfig
    ) throws IllegalArgumentException, IllegalStateException, EJBException {
        checkCreateTimerCallPermission();
        checkExpiration(initialExpiration);

        return createTimerInternal(initialExpiration, intervalDuration, timerConfig);
    }

    @Override
    public Timer createCalendarTimer(
            ScheduleExpression schedule,
            TimerConfig timerConfig
    ) throws IllegalArgumentException, IllegalStateException, EJBException {
        checkCreateTimerCallPermission();
        checkScheduleExpression(schedule);

        return createTimerInternal(schedule, timerConfig);
    }

    @Override
    public Timer createCalendarTimer(ScheduleExpression schedule)
            throws IllegalArgumentException, IllegalStateException, EJBException {
        checkCreateTimerCallPermission();
        checkScheduleExpression(schedule);

        TimerConfig tc = new TimerConfig();
        tc.setInfo(null);

        return createTimerInternal(schedule, tc);
    }

    @Override
    public Collection<Timer> getTimers()
            throws IllegalStateException, EJBException {

        checkCallPermission();

        Collection<Timer> timerWrappers = new HashSet();
        timerWrappers.addAll(getTimers(persistentTimerService_));
        timerWrappers.addAll(getTimers(nonPersistentTimerService_));

        return timerWrappers;
    }

    private Collection<Timer> getTimers(EJBTimerService timerService_)
            throws IllegalStateException, EJBException {
        Collection<Timer> timerWrappers = new HashSet();

        Collection<TimerPrimaryKey> timerIds = new HashSet<>();
        if (timerService_ != null && ejbContext_.isTimedObject()) {
            try {
                timerIds = timerService_.getTimerIds(
                        ejbDescriptor_.getUniqueId(),
                        getTimedObjectPrimaryKey()
                );
            } catch (Exception fe) {
                EJBException ejbEx = new EJBException();
                ejbEx.initCause(fe);
                throw ejbEx;
            }
        }
        for (Iterator<TimerPrimaryKey> iter = timerIds.iterator(); iter.hasNext();) {
            TimerPrimaryKey next = iter.next();
            timerWrappers.add(new TimerWrapper(next, timerService_));
        }

        return timerWrappers;
    }

    @Override
    public Collection<Timer> getAllTimers()
            throws IllegalStateException, EJBException {

        checkCallPermission();

        Collection<Timer> timerWrappers = new HashSet();
        Collection<Long> containerIds = ejbDescriptor_.getEjbBundleDescriptor().getDescriptorIds();
        timerWrappers.addAll(getAllTimers(persistentTimerService_, containerIds));
        timerWrappers.addAll(getAllTimers(nonPersistentTimerService_, containerIds));

        return timerWrappers;
    }

    public Collection<Timer> getAllTimers(
            EJBTimerService timerService_,
            Collection<Long> containerIds
    ) throws IllegalStateException, EJBException {

        Collection<Timer> timerWrappers = new HashSet();

        if (timerService_ != null) {
            Collection<TimerPrimaryKey> timerIds
                    = timerService_.getTimerIds(containerIds);
            for (TimerPrimaryKey timerId : timerIds) {
                timerWrappers.add(new TimerWrapper(timerId, persistentTimerService_));
            }
        }

        return timerWrappers;
    }

    private Object getTimedObjectPrimaryKey() {
        if( !entity_ ) {
            return null;
        } else {
            synchronized(this) {
                if( timedObjectPrimaryKey_ == null ) {
                    timedObjectPrimaryKey_ = 
                        ((EntityContext) ejbContext_).getPrimaryKey();
                }
            }
        }
        return timedObjectPrimaryKey_;
    }

    private void checkCreateTimerCallPermission() 
        throws IllegalStateException {
        if( ejbContext_.isTimedObject() ) {
            checkCallPermission();
        } else {
            throw new IllegalStateException("EJBTimerService.createTimer can "
                + "only be called from a timed object.  This EJB does not " 
                + "implement javax.ejb.TimedObject");                 
        }
    }

    private void checkCallPermission() 
        throws IllegalStateException {
        ejbContext_.checkTimerServiceMethodAccess();
    }

    private Timer createTimerInternal(
            long initialDuration,
            long intervalDuration,
            Serializable info
    ) throws IllegalArgumentException, IllegalStateException, EJBException {

        TimerConfig timerConfig = new TimerConfig();
        timerConfig.setInfo(info);

        return createTimerInternal(initialDuration, intervalDuration, timerConfig);
    }

    private Timer createTimerInternal(
            long initialDuration,
            long intervalDuration,
            TimerConfig timerConfig
    ) throws IllegalArgumentException, IllegalStateException, EJBException {

        checkIntervalDuration(intervalDuration);

        EJBTimerService timerService_
                = timerConfig == null || timerConfig.isPersistent() ? persistentTimerService_ : nonPersistentTimerService_;
        TimerPrimaryKey timerId = null;
        try {
            timerId = timerService_.createTimer(ejbDescriptor_.getUniqueId(),
                    ejbDescriptor_.getApplication().getUniqueId(),
                    getTimedObjectPrimaryKey(),
                    initialDuration, intervalDuration, timerConfig);
        } catch (CreateException ce) {
            EJBException ejbEx = new EJBException();
            ejbEx.initCause(ce);
            throw ejbEx;
        }

        return new TimerWrapper(timerId, timerService_);
    }

    private Timer createTimerInternal(
            Date initialExpiration,
            long intervalDuration,
            Serializable info
    ) throws IllegalArgumentException, IllegalStateException, EJBException {

        TimerConfig tc = new TimerConfig();
        tc.setInfo(info);

        return createTimerInternal(initialExpiration, intervalDuration, tc);
    }

    private Timer createTimerInternal(
            Date initialExpiration,
            long intervalDuration,
            TimerConfig timerConfig
    ) throws IllegalArgumentException, IllegalStateException, EJBException {

        checkIntervalDuration(intervalDuration);

        EJBTimerService timerService_
                = timerConfig == null || timerConfig.isPersistent() ? persistentTimerService_ : nonPersistentTimerService_;
        TimerPrimaryKey timerId = null;
        try {
            timerId = timerService_.createTimer(ejbDescriptor_.getUniqueId(),
                    ejbDescriptor_.getApplication().getUniqueId(),
                    getTimedObjectPrimaryKey(),
                    initialExpiration, intervalDuration, timerConfig);
        } catch (CreateException ce) {
            EJBException ejbEx = new EJBException();
            ejbEx.initCause(ce);
            throw ejbEx;
        }

        return new TimerWrapper(timerId, timerService_);
    }

    private Timer createTimerInternal(
            ScheduleExpression schedule,
            TimerConfig timerConfig
    ) throws IllegalArgumentException, IllegalStateException, EJBException {

        EJBTimerService timerService_
                = timerConfig == null || timerConfig.isPersistent() ? persistentTimerService_ : nonPersistentTimerService_;
        TimerPrimaryKey timerId = null;
        try {
            timerId = timerService_.createTimer(ejbDescriptor_.getUniqueId(),
                    ejbDescriptor_.getApplication().getUniqueId(),
                    getTimedObjectPrimaryKey(),
                    new EJBTimerSchedule(schedule), timerConfig);
        } catch (CreateException ce) {
            EJBException ejbEx = new EJBException();
            ejbEx.initCause(ce);
            throw ejbEx;
        }

        return new TimerWrapper(timerId, timerService_);
    }

    private void checkDuration(long duration) 
            throws IllegalArgumentException {
        if( duration < 0 ) {
            throw new IllegalArgumentException("invalid duration=" + duration);
        } 
    } 

    private void checkInitialDuration(long initialDuration) 
            throws IllegalArgumentException {
        if( initialDuration < 0 ) {
            throw new IllegalArgumentException("invalid initial duration = " +
                                               initialDuration);
        }
    }

    private void checkIntervalDuration(long intervalDuration) 
            throws IllegalArgumentException {
        if( intervalDuration < 0 ) {
            throw new IllegalArgumentException("invalid interval duration = " +
                                               intervalDuration);
        }
    }
                             
    private void checkScheduleExpression(ScheduleExpression expression) 
            throws IllegalArgumentException {
        if( expression == null ) {
            throw new IllegalArgumentException("null ScheduleExpression");
        } 
    }
                             
    private void checkExpiration(Date expiration) 
            throws IllegalArgumentException {
        if( expiration == null ) {
            throw new IllegalArgumentException("null expiration");
        } 
        if( expiration.getTime() < 0 ) {
            throw new IllegalArgumentException("Negative expiration");
        } 
    }
}
