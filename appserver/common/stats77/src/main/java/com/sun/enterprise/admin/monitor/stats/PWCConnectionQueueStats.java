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

/**
 *
 * @author  nsegura
 */
import com.sun.enterprise.admin.monitor.stats.StringStatistic;
import org.glassfish.j2ee.statistics.CountStatistic;
import org.glassfish.j2ee.statistics.Stats;

/** 
 * ConnectionQueue information shows the number of sessions in the queue
 * and the average delay before the connection is accepted
 */
public interface PWCConnectionQueueStats extends Stats {

    /** 
     * Gets the ID of the connection queue
     *
     * @return The ID of the connection queue
     */
    public StringStatistic getId();

    /**
     * Gets the total number of connections that have been accepted.
     *
     * @return Total number of connections that have been accepted.
     */    
    public CountStatistic getCountTotalConnections();

    /**
     * Gets the number of connections currently in the queue
     *
     * @return Number of connections currently in the queue
     */    
    public CountStatistic getCountQueued();

    /**
     * Gets the largest number of connections that were in the queue
     * simultaneously.
     *
     * @return Largest number of connections that were in the queue
     * simultaneously
     */    
    public CountStatistic getPeakQueued();

    /**
     * Gets the maximum size of the connection queue
     *
     * @return Maximum size of the connection queue
     */    
    public CountStatistic getMaxQueued();

    /** 
     * Gets the number of times the queue has been too full to accommodate
     * a connection
     *
     * @return Number of times the queue has been too full to accommodate
     * a connection
     */    
    public CountStatistic getCountOverflows();

    /** 
     * Gets the total number of connections that have been queued.
     *
     * A given connection may be queued multiple times, so
     * <code>counttotalqueued</code> may be greater than or equal to
     * <code>counttotalconnections</code>.
     *
     * @return Total number of connections that have been queued
     */        
    public CountStatistic getCountTotalQueued();

    /**
     * Gets the total number of ticks that connections have spent in the
     * queue.
     * 
     * A tick is a system-dependent unit of time.
     *
     * @return Total number of ticks that connections have spent in the
     * queue
     */    
    public CountStatistic getTicksTotalQueued();

    /** 
     * Gets the average number of connections queued in the last 1 minute
     *
     * @return Average number of connections queued in the last 1 minute
     */    
    public CountStatistic getCountQueued1MinuteAverage();

    /** 
     * Gets the average number of connections queued in the last 5 minutes
     *
     * @return Average number of connections queued in the last 5 minutes
     */    
    public CountStatistic getCountQueued5MinuteAverage();

    /** 
     * Gets the average number of connections queued in the last 15 minutes
     *
     * @return Average number of connections queued in the last 15 minutes
     */    
    public CountStatistic getCountQueued15MinuteAverage();
    
}
