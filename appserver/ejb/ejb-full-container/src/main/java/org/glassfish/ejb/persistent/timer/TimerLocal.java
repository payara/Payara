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

package org.glassfish.ejb.persistent.timer;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.Set;

import javax.ejb.CreateException;
import javax.ejb.TimerConfig;
import javax.ejb.Local;

import com.sun.ejb.containers.EJBTimerSchedule;
import com.sun.ejb.containers.TimerPrimaryKey;

/**
 * Local view of the persistent representation of an EJB timer.
 *
 * @author Kenneth Saks
 * @author Marina Vatkina
 */
@Local
public interface TimerLocal {

    /**
     * Cancel timer.
     */
    void cancel(TimerPrimaryKey timerId) throws Exception;

    void cancelTimers(Collection<TimerState> timers);

    TimerState createTimer(String timerId,  
                      long containerId, long applicationId, String ownerId,
                      Object timedObjectPrimaryKey, 
                      Date initialExpiration, long intervalDuration, 
                      EJBTimerSchedule schedule, TimerConfig timerConfig) 
                      throws CreateException;

    TimerState findTimer(TimerPrimaryKey timerId);

    void remove(TimerPrimaryKey timerId);

    void remove(Set<TimerPrimaryKey> timerIds);

    // 
    // Queries returning Timer Ids (TimerPrimaryKey)
    //

    Set findTimerIdsByContainer(long containerId);
    Set findActiveTimerIdsByContainer(long containerId);
    Set findActiveTimerIdsByContainers(Collection<Long> containerIds);
    Set findCancelledTimerIdsByContainer(long containerId);

    Set findTimerIdsOwnedByThisServerByContainer(long containerId);
    Set findActiveTimerIdsOwnedByThisServerByContainer(long containerId);
    Set findCancelledTimerIdsOwnedByThisServerByContainer(long containerId);

    Set findTimerIdsOwnedByThisServer(); 
    Set findActiveTimerIdsOwnedByThisServer(); 
    Set findCancelledTimerIdsOwnedByThisServer();

    Set findTimerIdsOwnedBy(String owner);
    Set findActiveTimerIdsOwnedBy(String owner);
    Set findCancelledTimerIdsOwnedBy(String owner);


    //
    // Queries returning Timer local objects
    //

    Set findTimersByContainer(long containerId);
    Set findActiveTimersByContainer(long containerId);
    Set findCancelledTimersByContainer(long containerId);    

    Set findTimersOwnedByThisServerByContainer(long containerId);
    Set findActiveTimersOwnedByThisServerByContainer(long containerId);
    Set findCancelledTimersOwnedByThisServerByContainer(long containerId);

    Set findTimersOwnedByThisServer(); 
    Set findActiveTimersOwnedByThisServer(); 
    Set findCancelledTimersOwnedByThisServer();

    Set findTimersOwnedBy(String owner);
    Set findActiveTimersOwnedBy(String owner);
    Set findCancelledTimersOwnedBy(String owner);


    //
    // Queries returning counts
    //

    int countTimersByApplication(long applicationId);
    int countTimersByContainer(long containerId);
    int countActiveTimersByContainer(long containerId);
    int countCancelledTimersByContainer(long containerId);    

    int countTimersOwnedByThisServerByContainer(long containerId);
    int countActiveTimersOwnedByThisServerByContainer(long containerId);
    int countCancelledTimersOwnedByThisServerByContainer(long containerId);

    int countTimersOwnedByThisServer(); 
    int countActiveTimersOwnedByThisServer(); 
    int countCancelledTimersOwnedByThisServer();

    int countTimersOwnedBy(String owner);
    int countActiveTimersOwnedBy(String owner);
    int countCancelledTimersOwnedBy(String owner);

    String[] countTimersOwnedByServerIds(String[] serverIds);


    // Perform health check on timer database
    boolean checkStatus(String resourceJndiName, boolean checkDatabase);

    // Migrate timers from one server instance to another via bulk update
    int migrateTimers(String fromOwnerId, String toOwnerId);

    // Delete all timers owned by this EJB (aka containerId)
    int deleteTimersByContainer(long containerId);

    // Delete all timers owned by this Application (aka applicationId)
    int deleteTimersByApplication(long applicationId);
}
