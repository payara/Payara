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

/**
 *
 * The <code>Timer</code> interface contains information about a timer
 * that was created through the EJB Timer Service.
 *
 * @since EJB 2.1
 */
public interface Timer {

    /**
     * Cause the timer and all its associated expiration notifications to
     * be cancelled.
     *
     *
     * @exception java.lang.IllegalStateException If this method is
     * invoked while the instance is in a state that does not allow access 
     * to this method.
     * 
     * @exception javax.ejb.NoSuchObjectLocalException If invoked on a timer
     * that has expired or has been cancelled.
     * 
     * @exception javax.ejb.EJBException If this method could not complete due
     * to a system-level failure.
     */
    public void cancel() throws java.lang.IllegalStateException, javax.ejb.NoSuchObjectLocalException, javax.ejb.EJBException;
    
    /**
     * Get the number of milliseconds that will elapse before the next
     * scheduled timer expiration. 
     *
     * @return the number of milliseconds that will elapse before the next
     * scheduled timer expiration.
     *
     * @exception java.lang.IllegalStateException If this method is
     * invoked while the instance is in a state that does not allow access 
     * to this method.
     * 
     * @exception javax.ejb.NoSuchObjectLocalException If invoked on a timer
     * that has expired or has been cancelled.
     *
     * @exception javax.ejb.NoMoreTimeoutsExceptions Indicates that the 
     * timer has no future timeouts
     * 
     * @exception javax.ejb.EJBException If this method could not complete due
     * to a system-level failure.
     */
    public long getTimeRemaining() throws java.lang.IllegalStateException, javax.ejb.NoSuchObjectLocalException, javax.ejb.NoMoreTimeoutsException, javax.ejb.EJBException;

    /**
     * Get the point in time at which the next timer expiration is scheduled 
     * to occur.
     *
     * @return the point in time at which the next timer expiration is 
     * scheduled to occur.
     *
     * @exception java.lang.IllegalStateException If this method is
     * invoked while the instance is in a state that does not allow access 
     * to this method.
     * 
     * @exception javax.ejb.NoSuchObjectLocalException If invoked on a timer
     * that has expired or has been cancelled.
     * 
     * @exception javax.ejb.NoMoreTimeoutsExceptions Indicates that the 
     * timer has no future timeouts
     *
     * @exception javax.ejb.EJBException If this method could not complete due
     * to a system-level failure.
     */
    public Date getNextTimeout() throws java.lang.IllegalStateException, javax.ejb.NoSuchObjectLocalException, javax.ejb.NoMoreTimeoutsException, javax.ejb.EJBException;

    /**
     * Get the schedule expression corresponding to this timer.  The timer
     * must be a calendar-based timer.  It may have been created automatically
     * or programmatically.
     *
     * @return schedule expression for the timer.
     *
     * @exception java.lang.IllegalStateException If this method is
     * invoked while the instance is in a state that does not allow access 
     * to this method.  Also thrown if invoked on a timer that is not a
     * calendar-based timer.
     * 
     * @exception javax.ejb.NoSuchObjectLocalException If invoked on a timer
     * that has expired or has been cancelled.
     * 
     * @exception javax.ejb.EJBException If this method could not complete due
     * to a system-level failure.
     *
     * @since EJB 3.1
     */
    public ScheduleExpression getSchedule() throws java.lang.IllegalStateException, javax.ejb.NoSuchObjectLocalException, javax.ejb.EJBException;

    /**
     * Return whether this timer has persistent semantics.
     *
     * @return boolean indicating whether the timer is persistent.

     * @exception java.lang.IllegalStateException If this method is
     * invoked while the instance is in a state that does not allow access 
     * to this method. 
     * 
     * @exception javax.ejb.NoSuchObjectLocalException If invoked on a timer
     * that has expired or has been cancelled.
     * 
     * @exception javax.ejb.EJBException If this method could not complete due
     * to a system-level failure.
     *
     * @since EJB 3.1
     */
    public boolean isPersistent() throws java.lang.IllegalStateException, javax.ejb.NoSuchObjectLocalException, javax.ejb.EJBException;    

    /**
     * Return whether this timer is a calendar-based timer.  
     *
     * @return boolean indicating whether the timer is calendar-based.
     *
     * @exception java.lang.IllegalStateException If this method is
     * invoked while the instance is in a state that does not allow access 
     * to this method. 
     *
     * @exception javax.ejb.NoSuchObjectLocalException If invoked on a timer
     * that has expired or has been cancelled.
     * 
     * @exception javax.ejb.EJBException If this method could not complete due
     * to a system-level failure.
     *
     * @since EJB 3.1
     */
    public boolean isCalendarTimer() throws java.lang.IllegalStateException, javax.ejb.NoSuchObjectLocalException, javax.ejb.EJBException;    


    /**
     * Get the information associated with the timer at the time of
     * creation.  This may be the <code>info</code> string that was
     * passed to the timer creation method or the <code>info</code>
     * element of the <code>Schedule</code> annotaiton.
     *
     * @return The Serializable object that was passed in at timer creation, or
     * null if the info argument passed in at timer creation was null.
     *
     * @exception java.lang.IllegalStateException If this method is
     * invoked while the instance is in a state that does not allow access 
     * to this method.
     * 
     * @exception javax.ejb.NoSuchObjectLocalException If invoked on a timer
     * that has expired or has been cancelled.
     * 
     * @exception javax.ejb.EJBException If this method could not complete due
     * to a system-level failure.
     */
    public Serializable getInfo() throws java.lang.IllegalStateException, javax.ejb.NoSuchObjectLocalException, javax.ejb.EJBException;


    /**
     * Get a serializable handle to the timer.  This handle can
     * be used at a later time to re-obtain the timer reference.
     *
     * @return a serializable handle to the timer.
     * 
     * @exception java.lang.IllegalStateException If this method is
     * invoked while the instance is in a state that does not allow access 
     * to this method.  Also thrown if invoked on a non-persistent timer.
     * 
     * @exception javax.ejb.NoSuchObjectLocalException If invoked on a timer
     * that has expired or has been cancelled.
     * 
     * @exception javax.ejb.EJBException If this method could not complete due
     * to a system-level failure.
     */
    public TimerHandle getHandle() throws java.lang.IllegalStateException, javax.ejb.NoSuchObjectLocalException, javax.ejb.EJBException;



} 
