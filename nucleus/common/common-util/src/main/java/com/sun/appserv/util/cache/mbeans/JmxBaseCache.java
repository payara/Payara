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

/**
 * @Version $Id: JmxBaseCache.java,v 1.4 2005/12/25 04:25:22 tcfujii Exp $
 * Created on May 4, 2005 11:55 AM
 */

package com.sun.appserv.util.cache.mbeans;

import com.sun.appserv.util.cache.BaseCache;
import com.sun.appserv.util.cache.Constants;

/**
 * This class provides implementation for JmxBaseCacheMBean
 *
 * @author Krishnamohan Meduri (Krishna.Meduri@Sun.com)
 *
 */
public class JmxBaseCache implements JmxBaseCacheMBean {

    private String name;
    private BaseCache baseCache;

    public JmxBaseCache(BaseCache baseCache, String name) {
        this.baseCache = baseCache;
        this.name = name;
    }
    /**
     * Returns a unique identifier for this MBean inside the domain
     */
    public String getName() {
        return name;
    }

    /**
     * Returns maximum possible number of entries
     */
    public Integer getMaxEntries() {
        return (Integer) baseCache.getStatByName(
                                        Constants.STAT_BASECACHE_MAX_ENTRIES);
    }

    /**
     * Returns threshold. This when reached, an overflow will occur
     */
    public Integer getThreshold() {
        return (Integer) baseCache.getStatByName(
                                        Constants.STAT_BASECACHE_THRESHOLD);
    }

    /**
     * Returns current number of buckets
     */
    public Integer getTableSize() {
        return (Integer) baseCache.getStatByName(
                                        Constants.STAT_BASECACHE_TABLE_SIZE);
    }

    /**
     * Returns current number of Entries
     */
    public Integer getEntryCount() {
        return (Integer) baseCache.getStatByName(
                                        Constants.STAT_BASECACHE_ENTRY_COUNT);
    }

    /**
     * Return the number of cache hits
     */
    public Integer getHitCount() {
        return (Integer) baseCache.getStatByName(
                                        Constants.STAT_BASECACHE_HIT_COUNT);
    }

    /**
     * Returns the number of cache misses
     */
    public Integer getMissCount() {
        return (Integer) baseCache.getStatByName(
                                        Constants.STAT_BASECACHE_MISS_COUNT);
    }

    /**
     * Returns the number of entries that have been removed
     */
    public Integer getRemovalCount() {
        return (Integer) baseCache.getStatByName(
                                        Constants.STAT_BASECACHE_REMOVAL_COUNT);
    }

    /**
     * Returns the number of values that have been refreshed 
     * (replaced with a new value in an existing extry)
     */
    public Integer getRefreshCount() {
        return (Integer) baseCache.getStatByName(
                                        Constants.STAT_BASECACHE_REFRESH_COUNT);
    }

    /**
     * Returns the number of times that an overflow has occurred
     */
    public Integer getOverflowCount() {
        return (Integer) baseCache.getStatByName(
                                        Constants.STAT_BASECACHE_OVERFLOW_COUNT);
    }

    /**
     * Returns the number of times new entries have been added
     */
    public Integer getAddCount() {
        return (Integer) baseCache.getStatByName(
                                        Constants.STAT_BASECACHE_ADD_COUNT);
    }
}
