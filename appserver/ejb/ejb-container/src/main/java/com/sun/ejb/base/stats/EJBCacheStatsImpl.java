/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.ejb.base.stats;

import org.glassfish.j2ee.statistics.CountStatistic;
import org.glassfish.j2ee.statistics.BoundedRangeStatistic;

import com.sun.enterprise.admin.monitor.stats.EJBCacheStats;
import com.sun.enterprise.admin.monitor.stats.CountStatisticImpl;
import com.sun.enterprise.admin.monitor.stats.MutableCountStatisticImpl;
import com.sun.enterprise.admin.monitor.stats.BoundedRangeStatisticImpl;
import com.sun.enterprise.admin.monitor.stats.MutableBoundedRangeStatisticImpl;

import com.sun.enterprise.admin.monitor.registry.MonitoringRegistry;
import com.sun.enterprise.admin.monitor.registry.MonitoringLevelListener;
import com.sun.enterprise.admin.monitor.registry.MonitoringLevel;
import com.sun.enterprise.admin.monitor.registry.MonitoringRegistrationException;

import com.sun.ejb.spi.stats.EJBCacheStatsProvider;


/**
 * A Class for providing stats for EJB Caches.
 *  Used by both Entity and Stateful Containers
 *
 * @author Mahesh Kannan
 */

public class EJBCacheStatsImpl
    extends StatsImpl
    implements com.sun.enterprise.admin.monitor.stats.EJBCacheStats
{
    private EJBCacheStatsProvider delegate;

    private MutableBoundedRangeStatisticImpl	cacheHits;
    private MutableBoundedRangeStatisticImpl	cacheMisses;
    private MutableBoundedRangeStatisticImpl	numBeans;
    private MutableCountStatisticImpl		expiredStat;
    private MutableCountStatisticImpl		passivationErrors;
    private MutableCountStatisticImpl		passivations;
    private MutableCountStatisticImpl		passivationSuccess;


    public EJBCacheStatsImpl(EJBCacheStatsProvider delegate) {
	this.delegate = delegate;

	initialize();
    }

    protected void initialize() {
	super.initialize("com.sun.enterprise.admin.monitor.stats.EJBCacheStats");

	cacheHits = new MutableBoundedRangeStatisticImpl(
		new BoundedRangeStatisticImpl("CacheHits"));
	cacheMisses = new MutableBoundedRangeStatisticImpl(
		new BoundedRangeStatisticImpl("CacheMisses"));
	numBeans = new MutableBoundedRangeStatisticImpl(
		new BoundedRangeStatisticImpl("NumBeansInCache",
		   "Count", 0, delegate.getMaxCacheSize(), 0));
	expiredStat = new MutableCountStatisticImpl(
		new CountStatisticImpl("NumExpiredSessionsRemoved"));
	passivationErrors = new MutableCountStatisticImpl(
		new CountStatisticImpl("NumPassivationErrors"));
	passivations = new MutableCountStatisticImpl(
		new CountStatisticImpl("NumPassivations"));
	passivationSuccess = new MutableCountStatisticImpl(
		new CountStatisticImpl("NumPassivationSuccess"));
    }

    public BoundedRangeStatistic getCacheHits(){
	cacheHits.setCount(delegate.getCacheHits());
	return (BoundedRangeStatistic) cacheHits.modifiableView();
    }

    public BoundedRangeStatistic getCacheMisses(){
	cacheMisses.setCount(delegate.getCacheMisses());
	return (BoundedRangeStatistic) cacheMisses.modifiableView();
    }

    public BoundedRangeStatistic getNumBeansInCache(){
	numBeans.setCount(delegate.getNumBeansInCache());
	return (BoundedRangeStatistic) numBeans.modifiableView();
    }

    public CountStatistic getNumExpiredSessionsRemoved(){
	expiredStat.setCount(delegate.getNumExpiredSessionsRemoved());
	return (CountStatistic) expiredStat.modifiableView();
    }

    public CountStatistic getNumPassivationErrors(){
	passivationErrors.setCount(delegate.getNumPassivationErrors());
	return (CountStatistic) passivationErrors.modifiableView();
    }

    public CountStatistic getNumPassivations(){
	passivations.setCount(delegate.getNumPassivations());
	return (CountStatistic) passivations.modifiableView();
    }

    public CountStatistic getNumPassivationSuccess() {
	passivationSuccess.setCount(delegate.getNumPassivationSuccess());
	return (CountStatistic) passivationSuccess.modifiableView();
    }

}
