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

package com.sun.appserv.util.cache;

import com.sun.enterprise.util.CULoggerInfo;
import java.text.MessageFormat;

import java.util.Properties;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Locale;

/**
 * MultiLruCache -- in-memory bounded LRU cache with multiple LRU lists
 * Underlying Hashtable is made into logical segments, with each segment
 * having its own LRU list.
 */
public class BoundedMultiLruCache extends MultiLruCache {

    // upper bound on the cache size
    protected long maxSize = Constants.DEFAULT_MAX_CACHE_SIZE;
    protected long currentSize;
    private Object currentSizeLk = new Object();

    /**
     * initialize the LRU cache
     * @param maxCapacity maximum number of entries this cache may hold
     */
    public void init(int maxCapacity, Properties props) throws Exception {
        super.init(maxCapacity, props);
        currentSize = 0;

        if (props != null) {
            String strMaxSize = props.getProperty("MaxSize");
            int multiplier = 1;
            long size = -1;

            String prop = strMaxSize;
            if (prop != null) {
                int index;

                // upper case the string
                prop = prop.toUpperCase(Locale.ENGLISH);

                // look for 200KB or 80Kb or 1MB or 2Mb like suffixes
                if ((index = prop.indexOf("KB")) != -1) {
                    multiplier = Constants.KB;
                    prop = prop.substring(0, index);
                } else if ((index = prop.indexOf("MB")) != -1) {
                    multiplier = Constants.MB;
                    prop = prop.substring(0, index);
                }

                try {
                    size = Long.parseLong(prop.trim());
                } catch (NumberFormatException nfe) {}
            }

            // sanity check and convert
            if (size > 0)
                maxSize = (size * multiplier);
            else  {
                String msg = CULoggerInfo.getString(CULoggerInfo.boundedMultiLruCacheIllegalMaxSize);

                Object[] params = { strMaxSize };
                msg = MessageFormat.format(msg, params);

                throw new IllegalArgumentException(msg);
            }
        }
    }

    /**
     * this item is just added to the cache
     * @param item <code>CacheItem</code> that was created
     * @return a overflow item; may be null
     *
     * Cache bucket is already synchronized by the caller
     */
    protected CacheItem itemAdded(CacheItem item) {
        LruCacheItem overflow = (LruCacheItem) super.itemAdded(item);

        // update the size
        if (overflow != null) {
            decrementCurrentSize(overflow.getSize());
        }
        incrementCurrentSize(item.getSize());

        return overflow;
    }

    /**
     * item value has been refreshed
     * @param item <code>CacheItem</code> that was refreshed
     * @param oldSize size of the previous value that was refreshed
     * Cache bucket is already synchronized by the caller
     */
    protected void itemRefreshed(CacheItem item, int oldSize) {
        super.itemRefreshed(item, oldSize);

        /** reduce the cache by the size of the size of the previous value 
         *  and increment by the value being refreshed with.
         */
        decrementCurrentSize(oldSize);
        incrementCurrentSize(item.getSize());
    }

    /**
     * item value has been removed from the cache
     * @param item <code>CacheItem</code> that was just removed
     *
     * Cache bucket is already synchronized by the caller
     */
    protected void itemRemoved(CacheItem item) {
        super.itemRemoved(item);

        // update the size
        decrementCurrentSize(item.getSize());
    }

    /**
     * has cache reached its threshold
     * @return true when the cache reached its threshold
     */
    protected boolean isThresholdReached() {
        return (currentSize > maxSize || super.isThresholdReached());
    }

    /**
     * synchronized counter updates
     */
    protected final void incrementCurrentSize(int size) {
        synchronized(currentSizeLk) {
            currentSize += size;
        }
    }

    protected final void decrementCurrentSize(int size) {
        synchronized(currentSizeLk) {
            currentSize -= size;
        }
    }

    /**
     * get generic stats from subclasses 
     */

    /**
     * get the desired statistic counter
     * @param key to corresponding stat
     * @return an Object corresponding to the stat
     * See also: Constant.java for the key
     */
    public Object getStatByName(String key) {
        Object stat = super.getStatByName(key);

        if (stat == null && key != null) {
            if (key.equals(Constants.STAT_BOUNDEDMULTILRUCACHE_CURRENT_SIZE))
                stat = Long.valueOf(currentSize);
            else if (key.equals(Constants.STAT_BOUNDEDMULTILRUCACHE_MAX_SIZE)) {
                if (maxSize == Constants.DEFAULT_MAX_CACHE_SIZE)
                    stat = Constants.STAT_DEFAULT;
                else
                    stat = Long.valueOf(maxSize);
            }
        }

        return stat;
    }

    public Map getStats() {
        Map stats = super.getStats();

        // cache size in KB
        stats.put(Constants.STAT_BOUNDEDMULTILRUCACHE_CURRENT_SIZE,
                  Long.valueOf(currentSize));
        if (maxSize == Constants.DEFAULT_MAX_CACHE_SIZE) {
            stats.put(Constants.STAT_BOUNDEDMULTILRUCACHE_MAX_SIZE,
                      Constants.STAT_DEFAULT);
        }
        else {
            stats.put(Constants.STAT_BOUNDEDMULTILRUCACHE_MAX_SIZE,
                      Long.valueOf(maxSize));
        }
        return stats;
    }
}
