/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.monitor.stats;
import org.glassfish.j2ee.statistics.Stats;
import org.glassfish.j2ee.statistics.CountStatistic;
import com.sun.enterprise.admin.monitor.stats.StringStatistic;

/**
 * A Stats interface, to expose the monitoring information 
 * about each individual thread in the the thread system of the JVM.
 * @since 8.1
 */
public interface JVMThreadInfoStats extends Stats {
    
    /**
     * Returns the Id of the thread
     * @return  CountStatistic  Id of the thread
     */
    public CountStatistic getThreadId();
    
    /**
     * Returns the name of the thread
     * @return  StringStatistic name of the thread
     */
    public StringStatistic getThreadName();
    
    /**
     * Returns the state of the thread
     * @return StringStatistic  Thread state
     */
    public StringStatistic getThreadState();
    
    /**
     * Returns the elapsed time (in milliseconds) that the thread associated 
     * with this ThreadInfo has blocked to enter or reenter a monitor since 
     * thread contention monitoring is enabled.
     * @return CountStatistic   time elapsed in milliseconds, since the thread
     *                          entered the BLOCKED state. Returns -1 if thread
     *                          contention monitoring is disabled
     */
     public CountStatistic getBlockedTime();
     
     /**
      * Returns the number of times that this thread has been in the blocked
      * state
      * @return CountStatistic  the total number of times that the thread 
      *                         entered the BLOCKED state
      */
     public CountStatistic getBlockedCount();
     
     /**
      * Returns the elapsed time(in milliseconds) that the thread has been in 
      * the waiting state.
      * @returns CountStatistic elapsed time in milliseconds that the thread has
      *                         been in a WAITING state. Returns -1 if thread
      *                         contention monitoring is disabled.
      */
     public CountStatistic getWaitedTime();
     
     /**
      * Returns the number of times that the thread has been in WAITING or 
      * TIMED_WAITING states
      * @return CountStatistic  total number of times that the thread was in
      *                         WAITING or TIMED_WAITING states
      */
     public CountStatistic getWaitedCount();
     
     /**
      * Returns the string representation of the monitor lock that the thread 
      * is blocked to enter or waiting to be notified through 
      * the Object.wait method
      * @return StringStatistic the string representation of the monitor lock
      */
     public StringStatistic getLockName();
     
     /**
      * Returns the Id of the thread which holds the monitor lock of an 
      * object on which this thread is blocking
      * @return CountStatistic Id of the thread holding the lock.
      */
     public CountStatistic getLockOwnerId();
     
     
     /**
      * Returns the name of the thread that holds the monitor lock of the 
      * object this thread is blocking on
      * @return StringStatistic name of the thread holding the monitor lock.
      */
     public StringStatistic getLockOwnerName();
     
     /**
      * Returns the stacktrace associated with this thread
      */
     public StringStatistic getStackTrace();
}
