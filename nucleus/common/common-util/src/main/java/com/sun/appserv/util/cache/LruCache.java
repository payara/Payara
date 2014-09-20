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

import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 * LRUCache
 * in-memory bounded cache with an LRU list
 */
public class LruCache extends BaseCache {

    // the item never expires
    public static final long NO_TIMEOUT = -1;

    // LRU list
    protected LruCacheItem head;
    protected LruCacheItem tail;

    // the number of times the cache was trimmed
    protected int trimCount;

    protected int listSize;
    protected long timeout = NO_TIMEOUT;

    protected int defaultMaxEntries = Constants.DEFAULT_MAX_ENTRIES;
    protected boolean isUnbounded = false;

    /**
     * default constructor
     */
    public LruCache() { }

    /**
     * constructor with specified max entries.
     * @param defaultMaxEntries specifies the default max entries
     *        to use when the maxEntries is <= 0.
     */
    public LruCache(int defaultMaxEntries) {
        this.defaultMaxEntries = defaultMaxEntries;
    }

    /**
     * initialize the cache
     * @param maxEntries maximum number of entries expected in the cache
     * @param loadFactor the load factor
     * @param timeout to be used to trim the expired entries
     * @param props opaque list of properties for a given cache implementation
     * @throws a generic Exception if the initialization failed
     */
    public void init(int maxEntries, long timeout, float loadFactor, Properties props) {

        // if the max entries is <= 0 then set the default max entries
        if (maxEntries <= 0) {
            maxEntries = defaultMaxEntries;

            // mark this cache unbounded
            isUnbounded = true;
        }
        setTimeout(timeout);

        super.init(maxEntries, loadFactor, props);
    }

    /**
     * sets the timeout value
     * @param timeout to be used to trim the expired entries
     */
    public void setTimeout(long timeout) {
        // accept a positive timeout
        if (timeout > 0)
            this.timeout = timeout;
    }

    /**
     * create new item
     * @param hashCode for the entry
     * @param key <code>Object</code> key
     * @param value <code>Object</code> value
     * @param size size in bytes of the item
     *
     * subclasses may override to provide their own CacheItem extensions
     * e.g. one that permits persistence.
     */
    protected CacheItem createItem(int hashCode, Object key,
                                        Object value, int size) {
        return new LruCacheItem(hashCode, key, value, size);
    }

    /**
     * trim one item from the LRU list
     * @param currentTime of this operation
     * @return the item trimmed from cache
     *
     * list synchronization is handled by the caller
     */
    protected CacheItem trimLru(long currentTime) {

	if(tail == null)
	    return null;

        LruCacheItem trimItem = tail;

        if (tail != head) {
            tail = trimItem.lPrev;
            if(tail == null) {
                //TODO:: print out a warning message here
                tail = head = null;
            } else {
                tail.lNext = null;
                trimItem.lPrev = null;
            }
        } else {
            tail = head = null;
        }

            trimItem.isTrimmed = true;
            trimItem.lPrev = null;
            trimCount++;
            listSize--;

        return trimItem;
    }

    /**
    /**
     * this item is just added to the cache
     * @param item <code>CacheItem</code> that was created
     * @return a overflow item; may be null
     *
     * this function checks if adding the new item results in a overflow;
     * if so, it returns the item to be removed.
     *
     * Cache bucket is already synchronized by the caller
     */
    protected CacheItem itemAdded(CacheItem item) {
        boolean updateThreshold = false;
        CacheItem overflow = null;
        if(! (item instanceof LruCacheItem))
            return null;
        LruCacheItem lc = (LruCacheItem) item;

        // set the timestamp
        lc.lastAccessed = System.currentTimeMillis();

        // update the LRU
        synchronized (this) {
            if (head != null) {
                head.lPrev = lc;
                lc.lNext = head;
                lc.lPrev = null;
                head = lc;
            }
            else {
                head = tail = lc;
                lc.lPrev = lc.lNext = null;
            }

            listSize++;

            if (isThresholdReached()) {
                if (!isUnbounded)
                    overflow = trimLru(lc.lastAccessed);
                else
                    updateThreshold = true;
            }
        }

        // update the base cache threshold if needed
        if (updateThreshold)
            super.handleOverflow();

        return overflow;
    }

    /**
     * this item is accessed
     * @param item <code>CacheItem</code> accessed
     *
     * Cache bucket is already synchronized by the caller
     */
    protected void itemAccessed(CacheItem item) {
	if(head == null)
	    return;
	if(item == null)
	    return;
        if(! (item instanceof LruCacheItem))
            return;
        LruCacheItem lc = (LruCacheItem) item;

        synchronized (this) {

            // if the item is already trimmed from the LRU list, nothing to do.
            if (lc.isTrimmed)
                return;

            // update the timestamp
            lc.lastAccessed = System.currentTimeMillis();

            LruCacheItem prev = lc.lPrev;
            LruCacheItem next = lc.lNext;
            // update the LRU list
            if (prev != null) {
                // put the item at the head of LRU list
                lc.lPrev = null;
                lc.lNext = head;
                head.lPrev = lc;
                head = lc;

                // patch up the previous neighbors
                prev.lNext = next;
                if (next != null)
                    next.lPrev = prev;
                else
                    tail = prev;
           }
        }
    }

    /**
     * item value has been refreshed
     * @param item <code>CacheItem</code> that was refreshed
     * @param oldSize size of the previous value that was refreshed
     * Cache bucket is already synchronized by the caller
     */
    protected void itemRefreshed(CacheItem item, int oldSize) {
        itemAccessed(item);
    }

    /**
     * item value has been removed from the cache
     * @param item <code>CacheItem</code> that was just removed
     *
     * Cache bucket is already synchronized by the caller
     */
    protected void itemRemoved(CacheItem item) {
        if(! (item instanceof LruCacheItem))
            return;
        LruCacheItem l = (LruCacheItem) item;

        // remove the item from the LRU list
        synchronized (this) {
            LruCacheItem prev = l.lPrev;
            LruCacheItem next = l.lNext;

            // if the item is already trimmed from the LRU list, nothing to do.
            if (l.isTrimmed)
                return;

            // patch up the neighbors and make sure head/tail are correct
            if (prev != null)
                prev.lNext = next;
            else
                head = next;

            if (next != null)
                next.lPrev = prev;
            else
                tail = prev;

            l.lPrev = l.lNext = null;
            listSize--;
        }
    }

    /**
     * trim the expired entries from the cache.
     * @param maxCount maximum number of invalid entries to trim
     *        specify Integer.MAX_VALUE to trim all invalid entries
     * This call is to be scheduled by a thread managed by the container.
     *
     * NOTE: this algorithm assumes that all the entries in the cache have
     * identical timeout (otherwise traversing from tail won't be right).
     */
    public void trimExpiredEntries(int maxCount) {

        int count = 0;
        LruCacheItem item;
        long currentTime = System.currentTimeMillis();
        ArrayList list = new ArrayList();

        synchronized (this) {
            // traverse LRU list till we reach a valid item; remove them at once
            for (item = tail; item != null && count < maxCount;
                                                item = item.lPrev) {

                if (timeout != NO_TIMEOUT &&
                    (item.lastAccessed + timeout) <= currentTime) {
                    item.isTrimmed = true;
		    list.add(item);

                    count++;
                } else {
                    break;
                }
            }

            // if there was at least one invalid item then item != tail.
            if (item != tail) {
                if (item != null)
                    item.lNext = null;
                else
                    head = null;

                tail = item;
            }
            listSize -= count;
            trimCount += count;
        }

        // trim the items from the BaseCache from the old tail backwards
        for (int index=list.size()-1; index >= 0; index--) {
            trimItem((LruCacheItem) list.get(index));
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
            if (key.equals(Constants.STAT_LRUCACHE_LIST_LENGTH))
                stat = Integer.valueOf(listSize);
            else if (key.equals(Constants.STAT_LRUCACHE_TRIM_COUNT))
                stat = Integer.valueOf(trimCount);
        }
        return stat;
    }

    public Map getStats() {
        Map stats = super.getStats();
        stats.put(Constants.STAT_LRUCACHE_LIST_LENGTH,
                  Integer.valueOf(listSize));
        stats.put(Constants.STAT_LRUCACHE_TRIM_COUNT,
                  Integer.valueOf(trimCount));

        return stats;
    }

    /** default CacheItem class implementation  ***/
    protected static class LruCacheItem extends CacheItem {

        // double linked LRU list
        protected LruCacheItem lNext;
        protected LruCacheItem lPrev;
        protected boolean isTrimmed;
        protected long lastAccessed;

        protected LruCacheItem(int hashCode, Object key, Object value, int size) {
            super(hashCode, key, value, size);
        }

        /**
         * Return the next item
         */
         public LruCacheItem getLNext() {
             return lNext;
         }

        /**
         * Reset the next item reference
         */
         public void setLNext(LruCacheItem item) {
             lNext = item;
         }

        /**
         * Return the previous item
         */
         public LruCacheItem getLPrev() {
             return lPrev;
         }

        /**
         * Reset the previous item reference
         */
         public void setLPrev(LruCacheItem item) {
             lPrev = item;
         }

        /**
         * Return <code>true</code> if this item is trimmed
         */
         public boolean isTrimmed() {
             return isTrimmed;
         }

        /**
         * Set the trimmed flag
         */
         public void setTrimmed(boolean value) {
             isTrimmed = value;
         }

        /**
         * Return the last accessed timestamp
         */
         public long getLastAccessed() {
             return lastAccessed;
         }

        /**
         * Reset the last accessed timestamp
         */
         public void setLastAccessed(long l) {
             lastAccessed = l;
         }
    }
}

