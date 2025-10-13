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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2019] Payara Foundation and/or affiliates

package com.sun.appserv.util.cache.mbeans;

import com.sun.appserv.util.cache.BaseCache;
import com.sun.appserv.util.cache.Constants;

/**
 * This class provides implementation for JmxBaseCacheMBean
 *
 * @author Krishnamohan Meduri (Krishna.Meduri@Sun.com)
 * @since May 4, 2005
 * @version 1.4
 */
public class JmxBaseCache implements JmxBaseCacheMBean {

    private final String name;
    private final BaseCache baseCache;

    public JmxBaseCache(BaseCache baseCache, String name) {
        this.baseCache = baseCache;
        this.name = name;
    }
    /**
     * Returns a unique identifier for this MBean inside the domain
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Returns maximum possible number of entries
     */
    @Override
    public Integer getMaxEntries() {
        return (Integer) baseCache.getStatByName(
                                        Constants.STAT_BASECACHE_MAX_ENTRIES);
    }

    /**
     * Returns threshold. This when reached, an overflow will occur
     */
    @Override
    public Integer getThreshold() {
        return (Integer) baseCache.getStatByName(
                                        Constants.STAT_BASECACHE_THRESHOLD);
    }

    /**
     * Returns current number of buckets
     */
    @Override
    public Integer getTableSize() {
        return (Integer) baseCache.getStatByName(
                                        Constants.STAT_BASECACHE_TABLE_SIZE);
    }

    /**
     * Returns current number of Entries
     */
    @Override
    public Integer getEntryCount() {
        return (Integer) baseCache.getStatByName(
                                        Constants.STAT_BASECACHE_ENTRY_COUNT);
    }

    /**
     * Return the number of cache hits
     */
    @Override
    public Integer getHitCount() {
        return (Integer) baseCache.getStatByName(
                                        Constants.STAT_BASECACHE_HIT_COUNT);
    }

    /**
     * Returns the number of cache misses
     */
    @Override
    public Integer getMissCount() {
        return (Integer) baseCache.getStatByName(
                                        Constants.STAT_BASECACHE_MISS_COUNT);
    }

    /**
     * Returns the number of entries that have been removed
     */
    @Override
    public Integer getRemovalCount() {
        return (Integer) baseCache.getStatByName(
                                        Constants.STAT_BASECACHE_REMOVAL_COUNT);
    }

    /**
     * Returns the number of values that have been refreshed 
     * (replaced with a new value in an existing extry)
     */
    @Override
    public Integer getRefreshCount() {
        return (Integer) baseCache.getStatByName(
                                        Constants.STAT_BASECACHE_REFRESH_COUNT);
    }

    /**
     * Returns the number of times that an overflow has occurred
     */
    @Override
    public Integer getOverflowCount() {
        return (Integer) baseCache.getStatByName(
                                        Constants.STAT_BASECACHE_OVERFLOW_COUNT);
    }

    /**
     * Returns the number of times new entries have been added
     */
    @Override
    public Integer getAddCount() {
        return (Integer) baseCache.getStatByName(
                                        Constants.STAT_BASECACHE_ADD_COUNT);
    }
}
