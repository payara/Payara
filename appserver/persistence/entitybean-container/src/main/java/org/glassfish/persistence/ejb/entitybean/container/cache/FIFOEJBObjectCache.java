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

package org.glassfish.persistence.ejb.entitybean.container.cache;

import com.sun.appserv.util.cache.Cache;
import com.sun.appserv.util.cache.CacheListener;
import com.sun.appserv.util.cache.Constants;

import com.sun.ejb.containers.util.cache.LruEJBCache;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Properties;
import java.util.ResourceBundle;

import java.util.logging.*;
import com.sun.logging.*;

/**
 * A FIFO EJB(Local)Object cache that maintains reference count
 *
 * @author Mahesh Kannan
 */
public class FIFOEJBObjectCache
    extends LruEJBCache
    implements EJBObjectCache
{
    protected int maxCacheSize;
    protected String name;
    protected EJBObjectCacheListener listener;
    
    protected Object refCountLock = new Object();
    protected int totalRefCount = 0;
    protected static final boolean _printRefCount =
        Boolean.getBoolean("cache.printrefcount");
    
    
    private static final Logger _logger =
        LogDomains.getLogger(FIFOEJBObjectCache.class, LogDomains.EJB_LOGGER);
    
    /**
     * default constructor
     */
    public FIFOEJBObjectCache(String name) {
        this.name = name;
    }
    
    /**
     * constructor with specified timeout
     */
    public FIFOEJBObjectCache(String name, long timeout) {
        super();
        setTimeout(timeout);
        this.name = name;
    }
    
    public void init(int maxEntries, int numberOfVictimsToSelect, long timeout,
            float loadFactor, Properties props)
    {
        super.init(maxEntries, loadFactor, props);
        super.timeout = timeout;
        this.maxCacheSize = maxEntries;
        _logger.log(Level.FINE, name + ": FIFOEJBObject cache created....");
    }
    
    public void setEJBObjectCacheListener(EJBObjectCacheListener listener) {
        this.listener = listener;
    }
    
    public Object get(Object key) {
        int hashCode = hash(key);
        
        return internalGet(hashCode, key, false);
    }
    
    
    public Object get(Object key, boolean incrementRefCount) {
        int hashCode = hash(key);
        
        return internalGet(hashCode, key, incrementRefCount);
    }
    
    public Object put(Object key, Object value) {
        int hashCode = hash(key);
        
        return internalPut(hashCode, key, value, -1, false);
    }
    
    public Object put(Object key, Object value, boolean incrementRefCount) {
        int hashCode = hash(key);
        
        return internalPut(hashCode, key, value, -1, incrementRefCount);
    }
    
    
    public Object remove(Object key) {
        return internalRemove(key, true);
    }
    
    public Object remove(Object key, boolean decrementRefCount) {
        return internalRemove(key, decrementRefCount);
    }
    
    protected boolean isThresholdReached() {
        return listSize > maxCacheSize;
    }
    
    protected void itemAccessed(CacheItem item) { }
    
    protected void itemRemoved(CacheItem item) {
        // LruCacheItem(more specifically EJBObjectCacheItem) should always be used in conjunction with FIFOEJBObjectCache
        assert item instanceof LruCacheItem;
        LruCacheItem l = (LruCacheItem) item;
        
        // remove the item from the LRU list
        synchronized (this) {
            // if the item is already trimmed from the LRU list, nothing to do.
            if (l.isTrimmed()) {
                return;
            }
            
            LruCacheItem prev = l.getLPrev();
            LruCacheItem next = l.getLNext();
            
            l.setTrimmed(true);
            
            // patch up the neighbors and make sure head/tail are correct
            if (prev != null)
                prev.setLNext(next);
            else
                head = next;
            
            if (next != null)
                next.setLPrev(prev);
            else
                tail = prev;
            
            l.setLNext(null);
            l.setLPrev(null);
            
            listSize--;
        }
    }
    
    protected Object internalGet(int hashCode, Object key, 
                                 boolean incrementRefCount) {
        
        int index = getIndex(hashCode);
        Object value = null;
        CacheItem item = null;
        
        synchronized (bucketLocks[index]) {
            item = buckets[index];
            
            for (; item != null; item = item.getNext()) {
                if ( (hashCode == item.getHashCode()) && eq(key, item.getKey()) ) {
                    break;
                }
            }
            
            // update the stats in line
            if (item != null) {
                value = item.getValue();
                if (incrementRefCount) {
                    // EJBObjectCacheItem should always be used in conjunction with FIFOEJBObjectCache
                    assert item instanceof EJBObjectCacheItem;
                    EJBObjectCacheItem eoItem = (EJBObjectCacheItem) item;
                    eoItem.refCount++;
                    if (_printRefCount) {
                        incrementReferenceCount();
                    }
                    if (! eoItem.isTrimmed()) {
                        itemRemoved(eoItem);
                    }
                }
            }
        }
        
        if (item != null)
            incrementHitCount();
        else
            incrementMissCount();
        
        return value;
    }
    
    protected Object internalPut(int hashCode, Object key, Object value, 
                                 int size, boolean incrementRefCount)
    {
        
        int index = getIndex(hashCode);
        
        CacheItem item, oldItem = null, overflow = null;
        EJBObjectCacheItem newItem = null;
        Object oldValue = null;
        int oldSize = 0;
        
        // lookup the item
        synchronized (bucketLocks[index]) {
            for (item = buckets[index]; item != null; item = item.getNext()) {
                if ((hashCode == item.getHashCode()) && eq(key, item.getKey())) {
                    oldItem = item;
                    break;
                }
            }
            
            // if there was no item in the cache, insert the given item
            if (oldItem == null) {
                newItem = (EJBObjectCacheItem) 
                    createItem(hashCode, key, value, size);
                newItem.setTrimmed(incrementRefCount);
                
                // add the item at the head of the bucket list
                newItem.setNext( buckets[index] );
                buckets[index] = newItem;
                
                if (incrementRefCount) {
                    newItem.refCount++;
                    if (_printRefCount) {
                        incrementReferenceCount();
                    }
                } else {
                    overflow = itemAdded(newItem);
                }
            } else {
                oldValue = oldItem.getValue();
                if (incrementRefCount) {
                    // EJBObjectCacheItem should always be used in conjunction with FIFOEJBObjectCache
                    assert oldItem instanceof EJBObjectCacheItem;
                    EJBObjectCacheItem oldEJBO = (EJBObjectCacheItem) oldItem;
                    oldEJBO.refCount++;
                    if (_printRefCount) {
                        incrementReferenceCount();
                    }
                }
            }
        }
        
        if (newItem != null) {
            incrementEntryCount();
            // make sure we are are not crossing the threshold
            if ((overflow != null) && (listener != null)) {
                listener.handleOverflow(overflow.getKey());
            }
        }
        
        return oldValue;
    }
    
    
    public void print() {
        System.out.println("EJBObjectCache:: size: " + getEntryCount() + 
                           "; listSize: " + listSize);
        for (LruCacheItem run = head; run!=null; run=run.getLNext()) {
            System.out.print("("+run.getKey()+", "+run.getValue()+") ");
        }
        System.out.println();
    }
    
    protected Object internalRemove(Object key, boolean decrementRefCount) {
        
        int hashCode = hash(key);
        int index = getIndex(hashCode);
        
        CacheItem prev = null, item = null;
        
        synchronized (bucketLocks[index]) {
            for (item = buckets[index]; item != null; item = item.getNext()) {
                if (hashCode == item.getHashCode() && key.equals(item.getKey())) {
                    // EJBObjectCacheItem should always be used in conjunction with FIFOEJBObjectCache
                    assert item instanceof EJBObjectCacheItem;
                    EJBObjectCacheItem eoItem = (EJBObjectCacheItem) item;
                    if (decrementRefCount) {
                        if (eoItem.refCount > 0) {
                            eoItem.refCount--;
                            if (_printRefCount) {
                                decrementReferenceCount();
                            }
                        }
                    }
                    
                    if (eoItem.refCount > 0) {
                        return null;
                    }
                    
                    if (prev == null) {
                        buckets[index] = item.getNext();
                    } else  {
                        prev.setNext( item.getNext() );
                    }
                    item.setNext( null );
                    
                    itemRemoved(item);
                    
                    break;
                    
                }
                prev = item;
            }
        }
        
        if (item != null) {
            decrementEntryCount();
            incrementRemovalCount();
            incrementHitCount();
            return item.getValue();
        } else {
            incrementMissCount();
            return null;
        }
        
    }
    
    /*
      protected void trimItem(CacheItem item) {
    }
     */
    
    protected CacheItem createItem(int hashCode, Object key, Object value, 
                                   int size) {
        return new EJBObjectCacheItem(hashCode, key, value, size);
    }
    
    protected static class EJBObjectCacheItem
    extends LruCacheItem {
        protected int refCount;
        
        protected EJBObjectCacheItem(int hashCode, Object key, Object value, 
                                     int size) {
            super(hashCode, key, value, size);
        }
    }
    
    public Map getStats() {
        Map map = new HashMap();
        StringBuffer sbuf = new StringBuffer();
        
        sbuf.append("(totalRef=").append(totalRefCount).append("; ");
        
        sbuf.append("listSize=").append(listSize)
        .append("; curSize/totSize=").append(getEntryCount())
        .append("/").append(maxEntries)
        .append("; trim=").append(trimCount)
        .append("; remove=").append(removalCount)
        .append("; hit/miss=").append(hitCount).append("/").append(missCount)
        .append(")");
        map.put("["+name+"]", sbuf.toString());
        return map;
    }
    
    public void trimExpiredEntries(int maxCount) {
        
        int count = 0;
        LruCacheItem item, lastItem = null;
        long currentTime = System.currentTimeMillis();
        
        synchronized (this) {
            // traverse LRU list till we reach a valid item; remove them at once
            for (item = tail; item != null && count < maxCount;
                 item = item.getLPrev()) {
                
                if ((timeout != NO_TIMEOUT) &&
                    (item.getLastAccessed() + timeout) <= currentTime) {
                    item.setTrimmed(true);
                    lastItem = item;
                    
                    count++;
                } else {
                    break;
                }
            }
            
            // if there was at least one invalid item then item != tail.
            if (item != tail) {
                lastItem.setLPrev(null);
                
                if (item != null)
                    item.setLNext(null);
                else
                    head = null;
                
                lastItem = tail; // record the old tail
                tail = item;
            }
            listSize -= count;
            trimCount += count;
        }
        
        if (count > 0) {
            
            ArrayList localVictims = new ArrayList(count);
            // trim the items from the BaseCache from the old tail backwards
            for (item = lastItem; item != null; item = item.getLPrev()) {
                localVictims.add(item.getKey());
            }
            
            if (listener != null) {
                listener.handleBatchOverflow(localVictims);
            }
        }
    }
    
    protected void incrementReferenceCount() {
        synchronized (refCountLock) {
            totalRefCount++;
        }
    }
    
    protected void decrementReferenceCount() {
        synchronized (refCountLock) {
            totalRefCount--;
        }
    }
    
    protected void decrementReferenceCount(int count) {
        synchronized (refCountLock) {
            totalRefCount -= count;
        }
    }
    
    
    static void unitTest_1()
    throws Exception {
        
        FIFOEJBObjectCache cache = new FIFOEJBObjectCache("UnitTestCache");
        cache.init(512, 0, 0, (float)1.0, null);
        
        int maxCount = 14;
        ArrayList keys = new ArrayList();
        for (int i=0; i<maxCount; i++) {
            keys.add("K_"+i);
        }
        
        for (int i=0; i<maxCount; i++) {
            String key = (String) keys.get(i);
            System.out.println("****  put(" + key + ", " + key + ", i" + 
                               ((i%2) == 0) + ")");
            cache.put(key, key, ((i%2)==0));
        }
        
        System.out.println("***  Only odd numbered keys must be printed  ***");
        cache.print();
        System.out.println("************************************************");
        
        for (int i=0; i<maxCount; i++) {
            String key = (String) keys.get(i);
            cache.get(key, ((i % 2) != 0));
        }
        
        System.out.println("****  NONE SHOULD BE PRINTED ****");
        cache.print();
        System.out.println("************************************************");
        
        cache.put("K__15", "K__15", true);
        cache.put("K__16", "K__15", true);
        cache.get("K__16", true);   //K__16 has refCount == 2
        cache.put("K__17", "K__17");//K__17 has refCount == 0
        
        System.out.println("****  Only K__17 must be printed ****");
        cache.print();
        System.out.println("************************************************");
        
        for (int i=0; i<maxCount; i++) {
            String key = (String) keys.get(i);
            if (cache.remove(key) == null) {
                throw new RuntimeException("Remove must have returned null!!");
            }
        }
        
        Object k15 = cache.remove("K__15");
        Object k16_1 = cache.remove("K__16");
        Object k16_2 = cache.remove("K__16");
        Object k17 = cache.remove("K__17");
        
        if (k15 == null) {
            System.out.println("** FAILED for K_15");
        }
        
        if (k16_1 != null) {
            System.out.println("** FAILED for K_16_1");
        }
        
        if (k16_2 == null) {
            System.out.println("** FAILED for K_16_2");
        }
        
        if (k17 == null) {
            System.out.println("** FAILED for K_17");
        }
        
        // Now the list id completely empty, add some more items
        for (int i=0; i<maxCount; i+=2) {
            String key = (String) keys.get(i);
            cache.put(key, key, (i%4)==0);
        }
        cache.print();
        
        
        //Make the FIFO list empty
        for (int i=0; i<maxCount; i+=2) {
            String key = (String) keys.get(i);
            cache.get(key, true);
        }
        cache.print();
        
        // Now the FIFO list id completely empty, add some more items
        for (int i=1; i<maxCount; i+=2) {
            String key = (String) keys.get(i);
            cache.put(key, key, (i%9)==0);
        }
        cache.print();
    }
    
    public static void main(String[] args)
        throws Exception
    {
        unitTest_1();
    }
    
}
