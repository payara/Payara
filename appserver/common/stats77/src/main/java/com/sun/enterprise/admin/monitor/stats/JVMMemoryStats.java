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

/**
 * A Stats interface, to expose the monitoring information 
 * about the JVM memory subsystem. This interfaces exposes
 * the memory usage information for the heap and the non-heap
 * areas of the memory subsystem.
 * @since 8.1
 */
public interface JVMMemoryStats extends Stats {
    
    /**
     * Returns the approximate number of objects, that are
     * pending finalization.
     * @return CountStatistic   Objects pending finalization
     */
    public CountStatistic getObjectPendingFinalizationCount();
    
    /**
     * Returns the size of the heap initially requested by the JVM
     * @return CountStatistic initial heap size in bytes
     */
    public CountStatistic getInitHeapSize();
    
    /**
     * Returns the size of the heap currently in use
     * @return CountStatistic current heap usage in bytes
     */
    public CountStatistic getUsedHeapSize();
    
    /**
     * Returns the maximum amount of memory in bytes that can be used
     * for memory management
     * @return CountStatistic maximum heap size in bytes
     */
    public CountStatistic getMaxHeapSize();
    
    /**
     * Returns the amount of memory in bytes that is committed
     * for the JVM to use
     * @return CountStatistic memory committed for the jvm in bytes
     */
    public CountStatistic getCommittedHeapSize();
    
    /**
     * Returns the size of the non=heap area initially 
     * requested by the JVM
     * @return CountStatistic initial size of the non-heap area in bytes
     */
    public CountStatistic getInitNonHeapSize();
    
    /**
     * Returns the size of the non-heap area currently in use
     * @return CountStatistic current usage of the non-heap area in bytes
     */
    public CountStatistic getUsedNonHeapSize();
    
    /**
     * Returns the maximum amount of memory in bytes that can be used
     * for memory management
     * @return CountStatistic maximum non-heap area size in bytes
     */
    public CountStatistic getMaxNonHeapSize();
    
    /**
     * Returns the amount of memory in bytes that is committed
     * for the JVM to use
     * @return CountStatistic memory committed for the jvm in bytes
     */
    public CountStatistic getCommittedNonHeapSize();
    
}
