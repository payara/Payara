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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

/**
 * BaseCache
 * Generic in-memory, abstract cache 
 */
public class BaseCache implements Cache {
    
    /**
     * The resource bundle containing the localized message strings.
     */
    static final int MAX_ENTRIES = 1 << 30;
    static final float DEFAULT_LOAD_FACTOR = 0.75f;

    // maximum number of entries this cache may ever hold
    protected int maxEntries;

    // the number of cache entries in this cache
    protected int   entryCount;
    private Object  entryCountLk = new Object();

    /** threshold for the cache; once the threshold is reached
     *  entries are removed to accomodate newer inserts
     */
    protected int threshold = 0;

    // the number of cache hits
    protected int     hitCount;
    private Object  hitCountLk = new Object();

    // the number of cache misses
    protected int     missCount;
    private Object  missCountLk = new Object();

    // the number of cache item removals
    protected int    removalCount;
    private Object removalCountLk = new Object();

    // the number of cache item refreshes
    private int     refreshCount;
    private Object  refreshCountLk = new Object();

    // the number of times an item was added to cache
    private int     addCount;
    private Object  addCountLk = new Object();

    // the number of times the cache overflowed
    private int     overflowCount;
    private Object  overflowCountLk = new Object();

    // table size
	protected int maxBuckets;

    // cache entries hash table
	protected CacheItem[] buckets;
    // bucket-wide locks
	protected Object[]    bucketLocks;

    // boolean status and locks for item thread-safe refreshes
	protected boolean[]   refreshFlags;

    protected ArrayList listeners = new ArrayList();

    /**
     * default constructor for the basic cache
     */
    public BaseCache() { }

    /**
     * initialize the cache
     * @param maxEntries maximum number of entries expected in the cache
     * @param props opaque list of properties for a given cache implementation
     * @throws a generic Exception if the initialization failed
     */
    public void init(int maxEntries, Properties props) throws Exception {
        init(maxEntries, DEFAULT_LOAD_FACTOR, props);
    }

    /**
     * initialize the cache
     * @param maxEntries maximum number of entries expected in the cache
     * @param loadFactor the load factor
     * @param props opaque list of properties for a given cache implementation
     * @throws a generic Exception if the initialization failed
     */
    public void init(int maxEntries, float loadFactor, Properties props) {

        if (maxEntries <= 0) {
            String msg = CULoggerInfo.getString(CULoggerInfo.illegalMaxEntries);

            Integer obj = Integer.valueOf(maxEntries);
            Object[] params = { obj };
            msg = MessageFormat.format(msg, params);

            throw new IllegalArgumentException(msg);
        }

        if (maxEntries > MAX_ENTRIES)
             maxEntries = MAX_ENTRIES;

        this.maxEntries = maxEntries;

        // find a power of 2 >= maxEntries
        maxBuckets = 1;
        while (maxBuckets < maxEntries)
            maxBuckets <<= 1;

        //Cannot have the loadfactor as a negative value
        if( loadFactor < 0 )
            loadFactor = 0;

        /** initialize the threshold; a zero value for maxEntries
         *  implies no caching.
         */
        if (maxEntries != 0) {
            threshold = (int)(maxEntries * loadFactor) + 1;
        }

        // create the cache and the bucket locks
        entryCount = 0;
       	buckets = new CacheItem[maxBuckets];
       	bucketLocks = new Object[maxBuckets];
        refreshFlags = new boolean[maxBuckets];

        for (int i=0; i<maxBuckets; i++) {
       		buckets[i] = null;
			bucketLocks[i] = new Object();
            refreshFlags[i] = false;
        }
    }

    /**
     * add the cache module listener
     * @param listener <code>CacheListener</code> implementation
     */
    public void addCacheListener(CacheListener listener) {
        listeners.add(listener);
    }

    /**
     * Returns a hash code for non-null Object x.
     * @See also <code>HashMap</code>
     */
    protected int hash(Object x) {
        int h = x.hashCode();
        return h - (h << 7);  // i.e., -127 * h
    }

    /** 
     * Check for equality of non-null reference x and possibly-null y. 
     */
    protected boolean eq(Object x, Object y) {
        return x == y || x.equals(y);
    }

    /**
     * increase the threshold
     */
    protected void handleOverflow() {
        // just double the threshold; this may degenerate the cache.
        threshold = (threshold * 2);
        incrementOverflowCount();
    }

    /**
     * this item is just added to the cache
     * @param item <code>CacheItem</code> that was created
     * @return a overflow item; may be null
     * Cache bucket is already synchronized by the caller
     *
     * Here, if cache is overflowing (i.e. reached threshold); this class 
     * simply makes the cache unbounded by raising the threshold. Subclasses 
     * are expected to provide a robust cache replacement algorithm.
     *
     * Subclasses should enhance this implemntation. 
     */
    protected CacheItem itemAdded(CacheItem item) {
        if (isThresholdReached()) {
            handleOverflow();
        }
        return null;
    }

    /**
     * this item is accessed 
     * @param item <code>CacheItem</code> accessed
     *
     * Cache bucket is already synchronized by the caller
     */
    protected void itemAccessed(CacheItem item) { }

    /**
     * item value has been refreshed
     * @param item <code>CacheItem</code> that was refreshed
     * @param oldSize size of the previous value that was refreshed
     * Cache bucket is already synchronized by the caller
     */
    protected void itemRefreshed(CacheItem item, int oldSize) { }

    /**
     * item value has been removed from the cache
     * @param item <code>CacheItem</code> that was just removed
     *
     * Cache bucket is already synchronized by the caller
     */
    protected void itemRemoved(CacheItem item) { }

    /**
     * Cannot find an item with the given key and hashCode
     * @param key <code>Object</code> that is not found
     * @param hashCode <code>int</code> its hashCode
     *
     * @returns the Object value associated with the item
     * Cache bucket is already synchronized by the caller
     */
    protected Object loadValue(Object key, int hashCode) { 
        return null;
    }

    /**
     * create new item
     * @param hashCode for the entry
     * @param key <code>Object</code> key 
     * @param value <code>Object</code> value
     * @param size size in bytes of the item
     * subclasses may override to provide their own CacheItem extensions
     * e.g. one that permits persistence.
     */
    protected CacheItem createItem(int hashCode, Object key, 
                                        Object value, int size) {
        return new CacheItem(hashCode, key, value, size);
    }
 
    /**
     * has cache reached its threshold
     * @return true when the cache reached its threshold
     */
    protected boolean isThresholdReached() {
        return (entryCount > threshold);
    }

    /** 
     * get the index of the item in the cache
     * @param hashCode of the entry
     * @return the index to be used in the cache
     */
    protected final int getIndex(int hashCode) {
        return (hashCode & (maxBuckets - 1));
    }

    /** 
     * get the index of the item given a key
     * @param key of the entry
     * @return the index to be used in the cache
     */
    public final int getIndex(Object key) {
        return getIndex(hash(key));
    }

    /**
     * get the item stored at the key.
     * @param key lookup key
     * @returns the item stored at the key; null if not found.
     */
    public Object get(Object key) {
        int hashCode = hash(key);

        return get(hashCode, key);
    }

    /**
     * get the item stored at the given pre-computed hash code and the key.
     * @param key lookup key
     * @returns the item stored at the key; null if not found.
     */
    public Object get(int hashCode, Object key) {

		int index = getIndex(hashCode);
        Object value;
        CacheItem item = null;

		synchronized (bucketLocks[index]) {
		    item = buckets[index];

			for (; item != null; item = item.next) {
				if ( (hashCode == item.hashCode) && eq(key, item.key) ) {
					break;
				}
			}

            // update the stats in line
            if (item != null) {
                value = item.getValue();
                itemAccessed(item);
            }
            else
                value = loadValue(key, hashCode);
		}

        if (item != null)
            incrementHitCount();
        else
            incrementMissCount();

        return value;
    }
        
    /**
     * check if the cache contains the item at the key
     * @param key lookup key
     * @returns true if there is an item stored at the key; false if not.
     */
    public boolean contains(Object key) {
    	return (get(key) != null);
    }
    
    /**
     * get all the items stored at the key.
     * @param key lookup key
     * @returns an Iterator over the items with the given key.
     */
    public Iterator getAll(Object key) {
        int hashCode = hash(key);
		int index = getIndex(hashCode);

        ArrayList valueList = new ArrayList(entryCount);
		synchronized (bucketLocks[index]) {
		    CacheItem item = buckets[index];

			for (; item != null; item = item.next) {
				if ( (hashCode == item.hashCode) && eq(key, item.key) ) {
                    incrementHitCount();
                    valueList.add(item.getValue());
				}
			}

		}

        return valueList.iterator();
    }

    /**
     * get an Iterator for the keys stored in the cache
     * @returns an Iterator
     */
    public Iterator keys() {
        ArrayList keyList = new ArrayList(entryCount);

        for (int index=0; index < maxBuckets; index++) {
            synchronized (bucketLocks[index]) {
                for (CacheItem item = buckets[index]; item != null; 
                                item = item.next) {
                    keyList.add(item.key);
                }
            }
        }

        return keyList.iterator();
    }

    /**
     * get an Enumeration for the keys stored in the cache
     * @returns an Enumeration
     * XXX: should use Iterator which is based on Collections
     */
    public Enumeration elements() {
        Vector keyList = new Vector();

        for (int index=0; index < maxBuckets; index++) {
            synchronized (bucketLocks[index]) {
                for (CacheItem item = buckets[index]; item != null; 
                                item = item.next) {
                    keyList.addElement(item.key);
                }
            }
        }

        return keyList.elements();
    }

    /**
     * get an Iterator for the values stored in the cache
     * @returns an Iterator
     */
    public Iterator values() {
        ArrayList valueList = new ArrayList(entryCount);

        for (int index=0; index < maxBuckets; index++) {
            synchronized (bucketLocks[index]) {
                for (CacheItem item = buckets[index]; item != null; 
                                item = item.next) {
                    valueList.add(item.value);
                }
            }
        }

        return valueList.iterator();
    }

    /**
    /**
     * cache the given value at the specified key and return previous value
     * @param key lookup key
     * @param object item value to be stored
     * @returns the previous item stored at the key; null if not found.
     */
    public Object put(Object key, Object value) {
        int hashCode = hash(key);

        return _put(hashCode, key, value, -1, false);
    }

    /**
     * cache the given value at the specified key and return previous value
     * @param key lookup key
     * @param object item value to be stored
     * @param size in bytes of the value being cached
     * @returns the previous item stored at the key; null if not found.
     */
    public Object put(Object key, Object value, int size) {
        int hashCode = hash(key);

        return _put(hashCode, key, value, size, false);
    }

    /**
     * add the given value to the cache at the specified key
     * @param key lookup key
     * @param object item value to be stored
     */
    public void add(Object key, Object value) {
        int hashCode = hash(key);

        _put(hashCode, key, value, -1, true);
    }

    /**
     * add the given value with specified size to the cache at specified key
     * @param key lookup key
     * @param object item value to be stored
     * @param size in bytes of the value being added
     *
     * This function is suitable for multi-valued keys.
     */
    public void add(Object key, Object value, int size) {
        int hashCode = hash(key);

        _put(hashCode, key, value, size, true);
    }

    /**
     * cache the given value at the specified key and return previous value
     * @param hashCode previously computed hashCode for the key
     * @param key lookup key
     * @param object item value to be stored
     * @param size in bytes of the value being cached
     * @param addValue treate this operation to add (default is to replace)
     * @returns the previous item stored at the key; null if not found.
     *
     * Note: This can be used just to refresh the cached item as well, altho
     * it may call trimCache() if the cache reached its threshold -- which is
     * is probably not very intuitive.
     */
    protected Object _put(int hashCode, Object key,
                            Object value, int size, boolean addValue) {
		int index = getIndex(hashCode);

		CacheItem item, newItem = null, oldItem = null, overflow = null;
        Object oldValue;
        int oldSize = 0;

        // lookup the item
		synchronized (bucketLocks[index]) {
			for (item = buckets[index]; item != null; item = item.next) {
				if ((hashCode == item.hashCode) && eq(key, item.key)) {

                    oldItem = item;
					break;
				}
			}

            // if there was no item in the cache, insert the given item
			if (addValue || oldItem == null) {
                newItem = createItem(hashCode, key, value, size); 
        
                // add the item at the head of the bucket list
			    newItem.next = buckets[index];
			    buckets[index] = newItem;

                oldValue = null;
                overflow = itemAdded(newItem);
			}
            else {
                oldSize = oldItem.getSize();
                oldValue = oldItem.refreshValue(value, size);
                itemRefreshed(oldItem, oldSize);
            }
		}

        if (newItem != null) {
            incrementEntryCount();
            incrementAddCount();

            // make sure we are are not crossing the threshold
            if (overflow != null)
                trimItem(overflow);
        }
        else
            incrementRefreshCount();

        return oldValue;
    }

    /**
     * remove the item stored at the key.
     * @param key lookup key
     * @returns the item stored at the key; null if not found.
     */
    public Object remove(Object key) {
        int hashCode = hash(key);

        Object retVal  = null;
        CacheItem removed = _remove( hashCode, key, null);
        
        if (removed != null)
            retVal = removed.getValue();
        return retVal;
    }

    /**
     * remove the item stored at the key.
     * @param hashCode a precomputed hashCode
     * @param key lookup key
     * @returns the item stored at the key; null if not found.
     */
    public Object remove(int hashCode, Object key) {
        Object retVal  = null;
        CacheItem removed = _remove( hashCode, key, null);
        
        if (removed != null)
            retVal = removed.getValue();
        return retVal;
    }

    /**
     * remove the given value stored at the key; value-specific removals.
     * @param key lookup key
     * @param value to match (for a multi-valued keys)
     * @returns the item stored at the key; null if not found.
     */
    public Object remove(Object key, Object value) {
        int hashCode = hash(key);

        Object retVal  = null;
        CacheItem removed = _remove( hashCode, key, value);
        
        if (removed != null)
            retVal = removed.getValue();
        return retVal;
    }

    /**
     * remove the item stored at the key.
     * @param hashCode a precomputed hashCode
     * @param key lookup key
     * @param value of the item to be matched 
     * @returns the item stored at the key; null if not found.
     */
    protected CacheItem _remove(int hashCode, Object key, Object value) {
		int index = getIndex(hashCode);

		CacheItem prev = null, item = null;

		synchronized (bucketLocks[index]) {
			for (item = buckets[index]; item != null; item = item.next) {
			    if (hashCode == item.hashCode && key.equals(item.key)) {

				    if (value == null || value == item.value) {

                        if (prev == null) {
                            buckets[index] = item.next;
                        } else  {
                            prev.next = item.next;
                        }
                        item.next = null;

                        itemRemoved(item);
                        break;
                    }
			    }
			    prev = item;
			}
        }
        
        if (item != null) {
            decrementEntryCount();
            incrementRemovalCount();

            incrementHitCount();
        } else
            incrementMissCount();

        return item;
    }

    /**
     * remove the item stored at the key.
     * @param item CacheItem to be removed
     * @return the item stored at the key; null if not found.
     */
    protected CacheItem _removeItem(CacheItem ritem) {

		int index = getIndex(ritem.hashCode);

		CacheItem prev = null, item = null;

		synchronized (bucketLocks[index]) {
			for (item = buckets[index]; item != null; item = item.next) {
			    if (item == ritem) {
                    if (prev == null) {
                        buckets[index] = item.next;
                    } else  {
                        prev.next = item.next;
                    }
                    item.next = null;
                    break;
			    }
			    prev = item;
			}
        }
        
        if (item != null) {
            decrementEntryCount();
        }

        return item;
    }

    /**
     * remove all the item with the given key.
     * @param key lookup key
     */
    public void removeAll(Object key) {
        int hashCode = hash(key);
		int index = getIndex(hashCode);

		CacheItem prev = null, item = null;
        ArrayList items = new ArrayList(entryCount);

		synchronized (bucketLocks[index]) {
			for (item = buckets[index]; item != null;
                                    item = item.next) {
			    if (hashCode == item.hashCode && key.equals(item.key)) {
                        if (prev == null) {
                            buckets[index] = item.next;
                        } else  {
                            prev.next = item.next;
                        }
                        item.next = null;
        
                        decrementEntryCount();
                        incrementRemovalCount();

                        items.add(item);
			    }
			    prev = item;
			}
        }

        // notify subclasses
        for (int i = 0; i < items.size(); i++) {
            itemRemoved((CacheItem)items.get(i));
        }
    }

    /**
     * trim the item from the cache and notify listeners
     * @param item to be trimmed
     */
    protected void trimItem(CacheItem item) {
        CacheItem removed = _removeItem(item);

        if (removed != null) {
            for (int i = 0; i < listeners.size(); i++) {
                CacheListener listener = (CacheListener) listeners.get(i);
                listener.trimEvent(removed.key, removed.value);
            }
        }
    }

    /**
     * wait for a refresh on the object associated with the key
     * @param key lookup key
     * @returns true on successful notification, or false if there is
     *  no thread refreshing this entry.
     */
    public boolean waitRefresh(int index) {
		synchronized (bucketLocks[index]) {
            if (refreshFlags[index] == false) {
                refreshFlags[index] = true;
                return false;
            }

            // wait till refresh is finished
            try {
		while(refreshFlags[index])
                	bucketLocks[index].wait();
            } catch (InterruptedException ie) {}
        }
        return true;
    }

    /**
     * notify threads waiting for a refresh on the object associated with the key
     * @param key lookup key
     */
    public void notifyRefresh(int index) {
        // notify other threads waiting for refresh
		synchronized (bucketLocks[index]) {
            refreshFlags[index] = false;
            bucketLocks[index].notifyAll();
        }
    }

    /**
     * clear all the entries from the cache.
     * @returns the number of entries cleared from the cache
     */
    public int clear() {
        
		CacheItem item=null, next=null;
        int count = 0;

        for (int index = 0; index < maxBuckets; index++) {
		    synchronized (bucketLocks[index]) {
			    for (item = buckets[index]; item != null; 
                                            item = item.next) {
                    next = item.next;
                    item.next = null;

                    count++;
                    decrementEntryCount();
                    itemRemoved(item);

                    if (entryCount == 0)
                        break;
			    }
                buckets[index] = null;
            }
        }


        return count;
    }

    /**
     * trim the expired entries from the cache.
     * @param maxCount maximum number of invalid entries to trim
     *        specify Integer.MAX_VALUE to trim all timedout entries
     *
     * This call is to be scheduled by a thread managed by the container.
     */
    public void trimExpiredEntries(int maxCount) {}

    /**
     * get the number of entries in the cache
     * @return the number of entries the cache currently holds
     */
    public int getEntryCount() {
        return entryCount;
    }

    /*** methods for monitoring the cache          ***/

    /**
     * is this cache empty?
     * @returns true if the cache is empty; false otherwise.
     */
    public boolean isEmpty() {
        return (entryCount == 0);
    }

    /**
     * synchronized counter updates
     */
    protected final void incrementEntryCount() {
        synchronized(entryCountLk) {
            entryCount++;
        }
    }

    protected final void decrementEntryCount() {
        synchronized(entryCountLk) {
            entryCount--;
        }
    }

    protected final void incrementHitCount() {
        synchronized (hitCountLk) {
            hitCount++;
        }
    }

    protected final void incrementMissCount() {
        synchronized (missCountLk) {
            missCount++;
        }
    }

    protected final void incrementRemovalCount() {
        synchronized (removalCountLk) {
            removalCount++;
        }
    }

    protected final void incrementRefreshCount() {
        synchronized (refreshCountLk) {
            refreshCount++;
        }
    }

    protected final void incrementAddCount() {
        synchronized (addCountLk) {
            addCount++;
        }
    }

    protected final void incrementOverflowCount() {
        synchronized (overflowCountLk) {
            overflowCount++;
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
        Object stat = null;

        if (key == null)
            return null;

        if (key.equals(Constants.STAT_BASECACHE_MAX_ENTRIES))
            stat = Integer.valueOf(maxEntries);
        else if (key.equals(Constants.STAT_BASECACHE_THRESHOLD))
            stat = Integer.valueOf(threshold);
        else if (key.equals(Constants.STAT_BASECACHE_TABLE_SIZE))
            stat = Integer.valueOf(maxBuckets);
        else if (key.equals(Constants.STAT_BASECACHE_ENTRY_COUNT))
            stat = Integer.valueOf(entryCount);
        else if (key.equals(Constants.STAT_BASECACHE_HIT_COUNT))
            stat = Integer.valueOf(hitCount);
        else if (key.equals(Constants.STAT_BASECACHE_MISS_COUNT))
            stat = Integer.valueOf(missCount);
        else if (key.equals(Constants.STAT_BASECACHE_REMOVAL_COUNT))
            stat = Integer.valueOf(removalCount);
        else if (key.equals(Constants.STAT_BASECACHE_REFRESH_COUNT))
            stat = Integer.valueOf(refreshCount);
        else if (key.equals(Constants.STAT_BASECACHE_OVERFLOW_COUNT))
            stat = Integer.valueOf(overflowCount);
        else if (key.equals(Constants.STAT_BASECACHE_ADD_COUNT))
            stat = Integer.valueOf(addCount);

        return stat;
    }

    /**
     * get the stats snapshot
     * @return a Map of stats
     * See also: Constant.java for the keys
     */
    public Map getStats() {
        HashMap stats = new HashMap();

        stats.put(Constants.STAT_BASECACHE_MAX_ENTRIES,
                  Integer.valueOf(maxEntries));
        stats.put(Constants.STAT_BASECACHE_THRESHOLD,
                  Integer.valueOf(threshold));
        stats.put(Constants.STAT_BASECACHE_TABLE_SIZE,
                  Integer.valueOf(maxBuckets));
        stats.put(Constants.STAT_BASECACHE_ENTRY_COUNT,
                  Integer.valueOf(entryCount));
        stats.put(Constants.STAT_BASECACHE_HIT_COUNT,
                  Integer.valueOf(hitCount));
        stats.put(Constants.STAT_BASECACHE_MISS_COUNT,
                  Integer.valueOf(missCount));
        stats.put(Constants.STAT_BASECACHE_REMOVAL_COUNT,
                  Integer.valueOf(removalCount));
        stats.put(Constants.STAT_BASECACHE_REFRESH_COUNT,
                  Integer.valueOf(refreshCount));
        stats.put(Constants.STAT_BASECACHE_OVERFLOW_COUNT,
                  Integer.valueOf(overflowCount));
        stats.put(Constants.STAT_BASECACHE_ADD_COUNT,
                  Integer.valueOf(addCount));

        return stats;
    }

    /**
     * Sets all references to null. This method should be called 
     * at the end of this object's life cycle.
     */
    public void destroy() {
        if ((listeners != null) && (buckets != null) && (bucketLocks != null)) {
            clear();
            listeners.clear();
        }

        entryCountLk     = null;
        hitCountLk       = null;
        missCountLk      = null;
        removalCountLk   = null;
        refreshCountLk   = null;
        addCountLk       = null;
        overflowCountLk  = null;
        buckets          = null;
        bucketLocks      = null;
        refreshFlags     = null;
        listeners        = null;
    }

    /**
     * clear the stats
     */
    public void clearStats() {
        hitCount = 0;
        missCount = 0;
        removalCount = 0;
        refreshCount = 0;
        overflowCount = 0;
        addCount = 0;
    }

    /** default CacheItem class implementation  ***/
    public static class CacheItem {
        int hashCode;
        Object key;
        Object value;
        int size;
    
        CacheItem next;
        
        protected CacheItem(int hashCode, Object key, Object value, int size) {
            this.hashCode = hashCode;
            this.key = key;
            this.value = value;
            this.size = size;
        }
        
        /**
         * get the item's hashCode
         */
        public int getHashCode() {
            return hashCode;
        }
    
        /**
         * get the item's key
         */
        public Object getKey() {
            return key;
        }
    
        /**
         * get the item's next reference
         */
        public CacheItem getNext() {
            return next;
        }
    
        /**
         * set the item's next reference
         */
        public void setNext(CacheItem next) {
            this.next = next;
        }
    
        /**
         * get the item's value
         */
        public Object getValue() {
            return value;
        }
    
        /**
         * @return size of the entry in bytes
         * a value of -1 indicates unknown size
         */
        public int getSize() {
            return size;
        }
    
        /**
         * refresh the item's value
         * @param value value to be updated
         * @param newSize of the field
         */
        protected Object refreshValue(Object value, int newSize) {
            Object oldValue = this.value;
            this.value = value;
            this.size = newSize;
    
            return oldValue;
        }
    
        public String toString() {
            return "key: " + key + "; value: " + value.toString();
        }
    }
}
