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
 * about the thread system of the JVM.
 * @since 8.1
 */
public interface JVMThreadStats extends Stats {
    
    /**
     * Returns the current number of live daemon and non-daemon 
     * threads
     * @return CountStatistic   current number of live threads
     */
    public CountStatistic getThreadCount();
    
    /**
     * Returns the peak live thread count, since the JVM started or
     * the peak was reset
     * @return CountStatistic   peak live thread count
     */
    public CountStatistic getPeakThreadCount();
    
    /**
     * Returns the total number of threads created and also started
     * since the JVM started
     * @return CountStatistic   total number of threads started
     */
    public CountStatistic getTotalStartedThreadCount();
    
    /**
     * Returns the current number of live daemon threads
     * @return CountStatistic   current number of live daemon threads
     */
    public CountStatistic getDaemonThreadCount();
    
    /**
     * Returns a comma separated list of all live thread ids
     * @return StringStatistic  live thread ids
     */
    public StringStatistic getAllThreadIds();
    
    /**
     * Returns the CPU time for the current thread in nanoseconds, if
     * CPU time measurement is enabled. Else returns -1
     * @return  CountStatistic  CPU time for the current thread
     */
    public CountStatistic getCurrentThreadCPUTime();
    
    /**
     * Returns a comma separated list of thread ids that are
     * monitor deadlocked
     * @return StringStatistic
     */
    public StringStatistic getMonitorDeadlockedThreads();
    
}
