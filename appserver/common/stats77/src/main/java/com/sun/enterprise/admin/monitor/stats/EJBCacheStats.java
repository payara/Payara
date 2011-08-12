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
import org.glassfish.j2ee.statistics.BoundedRangeStatistic;

/**
 * A Stats interface to represent the statistics exposed by the Enterprise Bean Cache.
 * This is based on the statistics that were exposed in S1AS7.0. An implementation of EJB Cache
 * should provide statistical data by implementing this interface.
 * @author Muralidhar Vempaty
 * @author Kedar Mhaswade
 * @since S1AS8.0
 * @version 1.0
 */
public interface EJBCacheStats extends Stats {
    
    /**
     * Returns the number of times a user request fails to find an EJB in associated EJB cache instance, as a CountStatistic.
     * @return		an instance of {@link BoundedRangeStatistic}
     */
    public BoundedRangeStatistic getCacheMisses();
    
    /**
     * Returns the number of times a user request hits an EJB in associated EJB cache instance, as a CountStatistic.
	 * @return		an instance of {@link BoundedRangeStatistic}
     */
    public BoundedRangeStatistic getCacheHits();
    
    /** Returns total number of EJBs in the associated EJB Cache, as a BoundedRangeStatistic. 
	 * Note that this returns the various statistical values like maximum and minimum value attained
	 * as a part of the return value.
	 * @return		an instance of {@link BoundedRangeStatistic}
     */
    public BoundedRangeStatistic getNumBeansInCache();
    
    /**
     * Returns the number of passivations of a Stateful Session Bean, as a CountStatistic.
	 * @return		an instance of {@link CountStatistic}
     */
    public CountStatistic getNumPassivations();
    
    /**
     * Returns the number of errors in passivating a Stateful Session Bean, as a CountStatistic.
	 * Must be less than or equal to {@link #getNumPassivations}
	 * @return		an instance of {@link CountStatistic}
     */
    public CountStatistic getNumPassivationErrors();
    
    /**
     * Returns the number of removed Expired Sessions as a CountStatistic.
	 * @return		an instance of {@link CountStatistic}
     */
    public CountStatistic getNumExpiredSessionsRemoved();
    
    /**
     * Returns the number of errors in passivating a Stateful Session Bean, as a CountStatistic.
	 * Must be less than or equal to {@link #getNumPassivations}
	 * @return		an instance of {@link CountStatistic}
     */
    public CountStatistic getNumPassivationSuccess();
}
