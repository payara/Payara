/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2010 Oracle and/or its affiliates. All rights reserved.
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

package javax.ejb;

import java.io.Serializable;
import java.util.Date;
import java.util.Collection;

/**
 * The TimerService interface provides enterprise bean components with
 * access to the container-provided Timer Service.  The EJB Timer
 * Service allows stateless session beans, singleton session beans,
 * message-driven beans, and EJB 2.x entity beans to be registered for
 * timer callback events at a specified time, after a specified
 * elapsed time, after a specified interval, or according to a
 * calendar-based schedule.
 *
 * @since EJB 2.1
 */
public interface TimerService {

    /**
     * Create a single-action timer that expires after a specified duration.
     *
     * @param duration the number of milliseconds that must elapse before
     * the timer expires.
     *
     * @param info application information to be delivered along
     * with the timer expiration notification. This can be null.
     *                    
     * @return the newly created Timer.
     *
     * @exception java.lang.IllegalArgumentException If duration is negative
     *
     * @exception java.lang.IllegalStateException If this method is
     * invoked while the instance is in a state that does not allow access 
     * to this method.
     * 
     * @exception javax.ejb.EJBException If this method fails due to a 
     * system-level failure.
     * 
     */
    public Timer createTimer(long duration, Serializable info) throws
        java.lang.IllegalArgumentException, java.lang.IllegalStateException,
        javax.ejb.EJBException;

    /**
     * Create a single-action timer that expires after a specified duration.
     *
     * @param duration the number of milliseconds that must elapse before
     * the timer expires.
     *
     * @param timerConfig timer configuration.
     *                    
     * @return the newly created Timer.
     *
     * @exception java.lang.IllegalArgumentException If duration is negative
     *
     * @exception java.lang.IllegalStateException If this method is
     * invoked while the instance is in a state that does not allow access 
     * to this method.
     * 
     * @exception javax.ejb.EJBException If this method fails due to a 
     * system-level failure.
     * 
     * @since EJB 3.1
     */
    public Timer createSingleActionTimer(long duration, TimerConfig timerConfig) throws
        java.lang.IllegalArgumentException, java.lang.IllegalStateException,
        javax.ejb.EJBException;

    /**
     * Create an interval timer whose first expiration occurs after a specified
     * duration, and whose subsequent expirations occur after a specified
     * interval.
     *
     * @param initialDuration The number of milliseconds that must elapse 
     * before the first timer expiration notification.
     *
     * @param intervalDuration The number of milliseconds that must elapse
     * between timer expiration notifications.  Expiration notifications are
     * scheduled relative to the time of the first expiration.  If expiration
     * is delayed (e.g. due to the interleaving of other method calls on the
     * bean), two or more expiration notifications may occur in close 
     * succession to "catch up".
     * 
     * @param info application information to be delivered along
     * with the timer expiration. This can be null.
     *                    
     * @return the newly created Timer.
     *
     * @exception java.lang.IllegalArgumentException If initialDuration is
     * negative or intervalDuration is negative.
     * 
     * @exception java.lang.IllegalStateException If this method is
     * invoked while the instance is in a state that does not allow access 
     * to this method.
     * 
     * @exception javax.ejb.EJBException If this method could not complete
     * due to a system-level failure.
     * 
     */
    public Timer createTimer(long initialDuration, long intervalDuration, 
                             Serializable info) throws
        java.lang.IllegalArgumentException, java.lang.IllegalStateException, 
        javax.ejb.EJBException;

    /**
     * Create an interval timer whose first expiration occurs after a specified
     * duration, and whose subsequent expirations occur after a specified
     * interval.
     *
     * @param initialDuration The number of milliseconds that must elapse 
     * before the first timer expiration notification.
     *
     * @param intervalDuration The number of milliseconds that must elapse
     * between timer expiration notifications.  Expiration notifications are
     * scheduled relative to the time of the first expiration.  If expiration
     * is delayed (e.g. due to the interleaving of other method calls on the
     * bean), two or more expiration notifications may occur in close 
     * succession to "catch up".
     * 
     * @param timerConfig timer configuration
     *                    
     * @return the newly created Timer.
     *
     * @exception java.lang.IllegalArgumentException If initialDuration is
     * negative or intervalDuration is negative.
     * 
     * @exception java.lang.IllegalStateException If this method is
     * invoked while the instance is in a state that does not allow access 
     * to this method.
     * 
     * @exception javax.ejb.EJBException If this method could not complete
     * due to a system-level failure.
     * 
     * @since EJB 3.1
     */
    public Timer createIntervalTimer(long initialDuration, long intervalDuration, 
                             TimerConfig timerConfig) throws
        java.lang.IllegalArgumentException, java.lang.IllegalStateException, 
        javax.ejb.EJBException;
        

    /**
     * Create a single-action timer that expires at a given point in time.
     *
     * @param expiration The point in time at which the timer must expire.
     *
     * @param info application information to be delivered along
     * with the timer expiration notification. This can be null.
     *                    
     * @return the newly created Timer.
     *
     * @exception java.lang.IllegalArgumentException If expiration is null or
     * expiration.getTime() is negative.
     *
     * @exception java.lang.IllegalStateException If this method is
     * invoked while the instance is in a state that does not allow access 
     * to this method.
     * 
     * @exception javax.ejb.EJBException If this method could not complete
     * due to a system-level failure.
     * 
     */
    public Timer createTimer(Date expiration, Serializable info) throws
        java.lang.IllegalArgumentException, java.lang.IllegalStateException, 
        javax.ejb.EJBException;

    /**
     * Create a single-action timer that expires at a given point in time.
     *
     * @param expiration the point in time at which the timer must expire.
     *
     * @param timerConfig timer configuration.
     *                    
     * @return the newly created Timer.
     *
     * @exception java.lang.IllegalArgumentException If expiration is null or
     * expiration.getTime() is negative.
     *
     * @exception java.lang.IllegalStateException If this method is
     * invoked while the instance is in a state that does not allow access 
     * to this method.
     * 
     * @exception javax.ejb.EJBException If this method could not complete
     * due to a system-level failure.
     * 
     * @since EJB 3.1
     */
    public Timer createSingleActionTimer(Date expiration, TimerConfig timerConfig) throws
        java.lang.IllegalArgumentException, java.lang.IllegalStateException, 
        javax.ejb.EJBException;
        
        

    /**
     * Create an interval timer whose first expiration occurs at a given
     * point in time and whose subsequent expirations occur after a specified
     * interval.
     *
     * @param initialExpiration the point in time at which the first timer
     * expiration must occur.
     *
     * @param intervalDuration the number of milliseconds that must elapse
     * between timer expiration notifications.  Expiration notifications are
     * scheduled relative to the time of the first expiration.  If expiration
     * is delayed (e.g. due to the interleaving of other method calls on the
     * bean), two or more expiration notifications may occur in close 
     * succession to "catch up".
     * 
     * @param info application information to be delivered along
     * with the timer expiration. This can be null.
     *                    
     * @return the newly created Timer.
     *
     * @exception java.lang.IllegalArgumentException If
     * initialExpiration is null, if initialExpiration.getTime() is
     * negative, or if intervalDuration is negative.
     *
     * @exception java.lang.IllegalStateException If this method is
     * invoked while the instance is in a state that does not allow access 
     * to this method.
     * 
     * @exception javax.ejb.EJBException If this method could not complete
     * due to a system-level failure.
     * 
     */
    public Timer createTimer(Date initialExpiration, long intervalDuration, 
                             Serializable info) throws
        java.lang.IllegalArgumentException, java.lang.IllegalStateException,
        javax.ejb.EJBException;

    /**
     * Create an interval timer whose first expiration occurs at a given
     * point in time and whose subsequent expirations occur after a specified
     * interval.
     *
     * @param initialExpiration the point in time at which the first timer
     * expiration must occur.
     *
     * @param intervalDuration the number of milliseconds that must elapse
     * between timer expiration notifications.  Expiration notifications are
     * scheduled relative to the time of the first expiration.  If expiration
     * is delayed (e.g. due to the interleaving of other method calls on the
     * bean), two or more expiration notifications may occur in close 
     * succession to "catch up".
     * 
     * @param timerConfig timer configuration.
     *                    
     * @return the newly created Timer.
     *
     * @exception java.lang.IllegalArgumentException If
     * initialExpiration is null, if initialExpiration.getTime() is
     * negative, or if intervalDuration is negative.
     *
     * @exception java.lang.IllegalStateException If this method is
     * invoked while the instance is in a state that does not allow access 
     * to this method.
     * 
     * @exception javax.ejb.EJBException If this method could not complete
     * due to a system-level failure.
     * 
     * @since EJB 3.1
     */
    public Timer createIntervalTimer(Date initialExpiration, long intervalDuration, 
                             TimerConfig timerConfig) throws
        java.lang.IllegalArgumentException, java.lang.IllegalStateException,
        javax.ejb.EJBException;

    /**
     * Create a calendar-based timer based on the input schedule expression.
     *
     * @param schedule a schedule expression describing the timeouts
     * for this timer.
     *
     * @return the newly created Timer.
     *
     * @exception java.lang.IllegalArgumentException If Schedule represents an
     * invalid schedule expression.
     *
     * @exception java.lang.IllegalStateException If this method is
     * invoked while the instance is in a state that does not allow access 
     * to this method.
     * 
     * @exception javax.ejb.EJBException If this method could not complete
     * due to a system-level failure.
     * 
     * @since EJB 3.1
     */
    public Timer createCalendarTimer(ScheduleExpression schedule) 
        throws java.lang.IllegalArgumentException, 
               java.lang.IllegalStateException, javax.ejb.EJBException;

    /**
     * Create a calendar-based timer based on the input schedule expression.
     *
     * @param schedule a schedule expression describing the timeouts for this timer.
     *
     * @param timerConfig timer configuration.
     *                    
     * @return the newly created Timer.
     *
     * @exception java.lang.IllegalArgumentException If Schedule represents an
     * invalid schedule expression.
     *
     * @exception java.lang.IllegalStateException If this method is
     * invoked while the instance is in a state that does not allow access 
     * to this method.
     * 
     * @exception javax.ejb.EJBException If this method could not complete
     * due to a system-level failure.
     * 
     * @since EJB 3.1
     */
    public Timer createCalendarTimer(ScheduleExpression schedule, TimerConfig timerConfig) 
        throws java.lang.IllegalArgumentException, 
               java.lang.IllegalStateException, javax.ejb.EJBException;

    /**
     * Get all the active timers associated with this bean.
     *
     * @return a collection of <code>javax.ejb.Timer</code> objects.
     *
     * @exception java.lang.IllegalStateException If this method is
     * invoked while the instance is in a state that does not allow access 
     * to this method.
     * 
     * @exception javax.ejb.EJBException If this method could not complete
     * due to a system-level failure.
     * 
     */
    public Collection<Timer> getTimers() throws java.lang.IllegalStateException,
        javax.ejb.EJBException;



} 
