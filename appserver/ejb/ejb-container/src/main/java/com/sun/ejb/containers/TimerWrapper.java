/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Date;
import java.io.Serializable;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.NoMoreTimeoutsException;
import javax.ejb.EJBException;
import javax.ejb.Timer;
import javax.ejb.TimerHandle;
import javax.ejb.ScheduleExpression;
import javax.ejb.FinderException;
import com.sun.enterprise.container.common.spi.util.IndirectlySerializable;
import com.sun.enterprise.container.common.spi.util.SerializableObjectFactory;
import org.glassfish.api.invocation.ComponentInvocation;
import com.sun.ejb.EjbInvocation;
import com.sun.ejb.ComponentContext;

/*
 * TimerWrapper is the application-level representation
 * of a timer.  Note that this class is not Serializable.
 * Timer instances are not intended to be directly persisted
 * by the application.  TimerHandle should be used instead.
 * 
 * @author Kenneth Saks
 */
public class TimerWrapper
    implements Timer, IndirectlySerializable {
    
    private TimerPrimaryKey timerId_;
    private EJBTimerService timerService_;

    TimerWrapper(TimerPrimaryKey timerId, EJBTimerService timerService) {
        timerId_      = timerId;
        timerService_ = timerService;   //TimerService passed in could be null
    }

    /*
     * Implementations of javax.ejb.Timer methods
     */
    @Override
    public void cancel() 
        throws IllegalStateException, NoSuchObjectLocalException, EJBException {

        checkCallPermission();

        try {
            timerService_.cancelTimer(timerId_);
        } catch(FinderException fe) {
            throw new NoSuchObjectLocalException("timer no longer exists", fe);
        } catch(Exception e) {
            throw new EJBException(e);
        }

    }
    
    @Override
    public long getTimeRemaining() 
        throws IllegalStateException, NoMoreTimeoutsException, NoSuchObjectLocalException {

        Date nextTimeout = getNextTimeout();

        Date now = new Date();
        long timeRemaining = nextTimeout.getTime() - now.getTime();

        return (timeRemaining > 0) ? timeRemaining : 0;
    }
    
    @Override
    public Date getNextTimeout() 
        throws IllegalStateException, NoMoreTimeoutsException, NoSuchObjectLocalException {

        checkCallPermission();

        Date nextTimeout;

        try {
            nextTimeout = timerService_.getNextTimeout(timerId_);
        } catch(FinderException fe) {
            throw new NoSuchObjectLocalException("timer no longer exists", fe);
        } 

        if (nextTimeout == null) {
            throw new NoMoreTimeoutsException();
        }

        return nextTimeout;
    }
    
    @Override
    public Serializable getInfo() 
        throws IllegalStateException, NoSuchObjectLocalException {

        checkCallPermission();

        Serializable info;

        try {
            info = timerService_.getInfo(timerId_);
        } catch(FinderException fe) {
            throw new NoSuchObjectLocalException("timer no longer exists", fe);
        } 

        return info;
    }
    
    @Override
    public TimerHandle getHandle() 
        throws IllegalStateException, NoSuchObjectLocalException {

        checkCallPermission();

        if( !isPersistent() ) {
            throw new IllegalStateException("Only allowed for persistent timers");
        }

        if( !timerService_.timerExists(timerId_) ) {
            throw new NoSuchObjectLocalException("timer no longer exists");
        } 

        return new TimerHandleImpl(timerId_, timerService_);
    }

    @Override
    public ScheduleExpression getSchedule()
            throws IllegalStateException, NoSuchObjectLocalException, EJBException {

        checkCallPermission();
        ScheduleExpression schedule;

        if( !isCalendarTimer() ) {
            throw new IllegalStateException("Only allowed for calendar-based timers");
        }

        try {
            schedule = timerService_.getScheduleExpression(timerId_);
        } catch(FinderException fe) {
            throw new NoSuchObjectLocalException("timer no longer exists", fe);
        } 

        return schedule;
    }

    @Override
    public boolean isCalendarTimer() throws java.lang.IllegalStateException,
            javax.ejb.NoSuchObjectLocalException, javax.ejb.EJBException {

        checkCallPermission();

        try {
            return timerService_.isCalendarTimer(timerId_);
        } catch(FinderException fe) {
            throw new NoSuchObjectLocalException("timer no longer exists", fe);
        }
    }


    @Override
    public boolean isPersistent() throws java.lang.IllegalStateException, 
            javax.ejb.NoSuchObjectLocalException, javax.ejb.EJBException {

        checkCallPermission();
        try {
            return timerService_.isPersistent(timerId_);
        } catch(FinderException fe) {
            throw new NoSuchObjectLocalException("timer no longer exists", fe);
        } 
    }

    @Override
    public boolean equals(Object o) {
        boolean equal = false;
        if(o instanceof TimerWrapper) {
            TimerWrapper other = (TimerWrapper) o;
            equal = other.timerId_.equals(this.timerId_);
        }
        return equal;
    }

    @Override
    public int hashCode() {
        return timerId_.hashCode();
    }

    @Override
    public String toString() {
        return "Timer " + timerId_;
    }

    /**
     * Verify that Timer method access is allowed from this context.
     * This method is static so that TimerHandle can call it even
     * before it has created a TimerWrapper instance.
     */
    private static void checkCallPermission() throws IllegalStateException {

        // Can't store a static ref because in embedded container it can be
        // changed by server restart
        EjbContainerUtil ejbContainerUtil = EjbContainerUtilImpl.getInstance();
        ComponentInvocation inv = ejbContainerUtil.getCurrentInvocation();
        if (inv == null) {
            throw new IllegalStateException("Invocation cannot be null");
        }
        ComponentInvocation.ComponentInvocationType invType = inv.getInvocationType();
        if( invType == ComponentInvocation.ComponentInvocationType.EJB_INVOCATION ) {
            if ( inv instanceof EjbInvocation ) {
                ComponentContext context = ((EjbInvocation) inv).context;
                // Delegate check to EJB context.  Let any
                // IllegalStateException bubble up.
                context.checkTimerServiceMethodAccess();
            }
        }
    }

    @Override
    public SerializableObjectFactory getSerializableObjectFactory() {
        return new SerializedTimerWrapper(timerId_, timerService_);
    }

    /**
     * Used by serialization code to serialize a TimerWrapper.  We
     * need a separate type that TimerHandle so that on deserialization
     * we know it started as a TimerWrapper instead of a TimerHandle.
     */
    public static class SerializedTimerWrapper
            implements SerializableObjectFactory {

        private TimerPrimaryKey timerId_;

        private EJBTimerService timerService_;

        SerializedTimerWrapper() {}

        SerializedTimerWrapper(TimerPrimaryKey timerId, EJBTimerService timerService) {
            timerId_ = timerId;
            timerService_ = timerService;
        }

        /**
         * When deserializing the timer wrapper create a TimerWrapper
         * object. Check if the record is valid only when making calls on the
         * object.
         *
         * @param appUniqueId
         * @return
         */
        @Override
        public Object createObject(long appUniqueId) throws EJBException {
            // Can't store a static ref because in embedded container it can be 
            // changed by server restart
            return new TimerWrapper(timerId_, timerService_);
        }
    }

    private static class TimerHandleImpl implements TimerHandle {

        private final TimerPrimaryKey timerId_;

        private final EJBTimerService timerService_;
        
        public TimerHandleImpl(TimerPrimaryKey timerId, EJBTimerService timerService) {
            timerId_ = timerId;
            timerService_ = timerService;
        }

        /**
         * Materialize Timer from handle.  This must be coded
         * very defensively, since handle use might be attempted from 
         * outside of EJB container.  
         */
        @Override
        public Timer getTimer() throws IllegalStateException, 
            NoSuchObjectLocalException, EJBException {
            TimerWrapper timer = null;

            // Can't store a static ref because in embedded container it can be 
            // changed by server restart
            EjbContainerUtil ejbContainerUtil = EjbContainerUtilImpl.getInstance();
            if( ejbContainerUtil != null ) {
                
                // Make sure use of timer service methods are allowed
                checkCallPermission();

                timer = getTimerInternal();

            } else {
                throw new IllegalStateException
                    ("Attempt to use EJB timer from outside a container");
            }

            return timer;
        }

        private TimerWrapper getTimerInternal() 
            throws NoSuchObjectLocalException, EJBException {

            TimerWrapper timer = null;
            // Can't store a static ref because in embedded container it can be
            // changed by server restart
            // Timer handles are only available for persistent timers.

            if( timerService_ != null ) {
                if( timerService_.timerExists(timerId_) ) {
                    timer = new TimerWrapper(timerId_, timerService_);
                } else {
                    throw new NoSuchObjectLocalException
                        ("timer is no longer active");
                }
            } else {
                throw new EJBException("EJB Timer Service not available");
            }
            
            return timer;
        }
    }
}
