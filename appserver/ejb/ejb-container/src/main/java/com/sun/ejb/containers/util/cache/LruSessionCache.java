/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.ejb.containers.util.cache;

import com.sun.appserv.util.cache.CacheListener;

import com.sun.ejb.containers.EjbContainerUtilImpl;
import com.sun.ejb.spi.container.SFSBContainerCallback;
import com.sun.ejb.spi.container.StatefulEJBContext;

import com.sun.ejb.base.stats.StatefulSessionStoreMonitor;
import com.sun.ejb.monitoring.stats.EjbCacheStatsProviderDelegate;

import org.glassfish.ha.store.api.BackingStore;
import org.glassfish.ha.store.util.SimpleMetadata;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.ha.store.api.BackingStoreException;

import java.io.Serializable;
import java.util.*;
import java.util.logging.*;


public class LruSessionCache
    extends LruEJBCache
    implements EjbCacheStatsProviderDelegate
{

    @LogMessageInfo(
        message = "[{0}]: Exception in backingStore.remove([{1}])",
        level = "WARNING")
    private static final String EXCEPTION_BACKING_STORE_REMOVE = "AS-EJB-00002";

    @LogMessageInfo(
        message = "[{0}]: passivateEJB(), Exception caught ->",
        level = "WARNING")
    private static final String PASSIVATE_EJB_EXCEPTION_CAUGHT = "AS-EJB-00003";

    @LogMessageInfo(
        message = "[{0}]: Cannot load from  BACKUPSTORE FOR Key: <[{1}]>",
        level = "SEVERE",
        cause = "Didn't find the data related with the given session key.",
        action = "Check if the session bean already timed out.")
    private static final String CANNOT_LOAD_FROM_BACKUP_STORE = "AS-EJB-00004";

    @LogMessageInfo(
        message = "[{0}]: Exception while  loading from backup session: <[{1}]>",
        level = "SEVERE",
        cause = "Session store exception or deserialization exception happened.",
        action = "Check error message and exception stack.")
    private static final String EXCEPTION_LOADING_BACKUP_SESSION = "AS-EJB-00005";

    @LogMessageInfo(
        message = "[{0}]: Error while  loading from backup session: <[{1}]>",
        level = "SEVERE",
        cause = "Session store error or deserialization error happened.",
        action = "Check error message and exception stack.")
    private static final String ERROR_LOADING_BACKUP_SESSION = "AS-EJB-00006";

    @LogMessageInfo(
        message = "[{0}]: Exception during backingStore.passivateSave([{1}])",
        level = "WARNING")
    private static final String EXCEPTION_DURING_PASSIVATE_SAVE = "AS-EJB-00007";

    @LogMessageInfo(
        message = "[{0}]: Iterator(), resetting head.lPrev",
        level = "WARNING")
    private static final String ITERATOR_RESETTING_HEAD_LPREV = "AS-EJB-00008";

    @LogMessageInfo(
        message = "[{0}]: Exiting TrimTimedoutBeans() because current cache state: [{1}]",
        level = "WARNING")
    private static final String EXITING_TRIM_TIMEDOUT_BEANS = "AS-EJB-00009";

    @LogMessageInfo(
        message = "[{0}]: TrimTimedoutBeans(), resetting head.lPrev",
        level = "WARNING")
    private static final String TRIM_TIMEDOUT_BEANS_RESETTING_HEAD_LPREV = "AS-EJB-00010";

    @LogMessageInfo(
        message = "[{0}]: Exiting TrimUnSortedTimedoutBeans() because current cache state: [{1}]",
        level = "WARNING")
    private static final String EXITING_TRIM_UNSORTED_TIMEDOUT_BEANS = "AS-EJB-00011";

    @LogMessageInfo(
        message = "Cannot find stateful session bean [{0}] in memory, and will not read it from disk because " +
                  "current stateful session bean passivation-capable value is false",
        level = "INFO")
    private static final String SFSB_NOT_FOUND_WHEN_PASSIVATION_DISABLED = "AS-EJB-00049";
    
    protected int		    cacheIdleTimeoutInSeconds;
    protected int		    removalTimeoutInSeconds;
    
    protected Object loadCountLock = new Object();
    protected int loadFromBackupCount;
    
    protected boolean removeIfIdle = false;
    
    private int numVictimsAccessed = 0;
    
    protected SFSBContainerCallback     container;
    protected BackingStore<Serializable, SimpleMetadata> backingStore;

    private static final byte CACHE_ITEM_VALID = 0;
    private static final byte CACHE_ITEM_LOADING = 1;
    private static final byte CACHE_ITEM_REMOVED = 2;

    protected String configData;

    private static final int	    STATE_RUNNING = 0;
    private static final int	    STATE_SHUTTING_DOWN = 1;
    private static final int	    STATE_UNDEPLOYING = 2;
    private static final int	    STATE_DESTROYED = 3;

    private int			    currentCacheState = STATE_RUNNING;

    protected int	confMaxCacheSize = Integer.MAX_VALUE;

    // TODO enable when enabling monitoring in SFSB container
    // private StatefulSessionStoreMonitor	    sfsbStoreMonitor;

    /**
     * Destroys all references. This is the last method call of this object's 
     * life cycle. 
     *
     * This method is called during undeploy of ejb container.
     */
    public void destroy() {
        this.currentCacheState = STATE_DESTROYED;
        this.container = null;

        super.destroy();
    }
    
    
    public LruSessionCache(String cacheName, 
                           SFSBContainerCallback container, 
                           int cacheIdleTime, int removalTime) {
        super();
        super.setCacheName(cacheName);

        this.container = container;
        
        this.cacheIdleTimeoutInSeconds = 
            (cacheIdleTime <= 0) ? 0 : cacheIdleTime;
        this.removalTimeoutInSeconds = 
            (removalTime <= 0) ? 0 : removalTime;
        
        if (cacheIdleTimeoutInSeconds > 0) {
            super.timeout = cacheIdleTimeoutInSeconds*1000L;
        }

        removeIfIdle = (removalTimeoutInSeconds > 0)
	    && (removalTimeoutInSeconds <= cacheIdleTimeoutInSeconds);
    }

    public void setBackingStore(BackingStore<Serializable, SimpleMetadata> store) {
        this.backingStore = store;
    }

    public void setStatefulSessionStoreMonitor(
	StatefulSessionStoreMonitor storeMonitor)
    {
	// this.sfsbStoreMonitor = storeMonitor;
    }
    
    /**
     * trim the item from the cache and notify listeners
     * @param item to be trimmed
     */
    protected void trimItem(CacheItem item) {
        LruCacheItem removed = (LruCacheItem) item;

        if (removeIfIdle) {
            StatefulEJBContext ctx = (StatefulEJBContext) item.getValue();
            
            long idleThreshold = 
                System.currentTimeMillis() - removalTimeoutInSeconds*1000L;
            if (ctx.getLastAccessTime() <= idleThreshold) {
                container.passivateEJB(ctx);
                return;
            }
        }

        for (int i = 0; i < listeners.size(); i++) {
            CacheListener listener = (CacheListener) listeners.get(i);
            listener.trimEvent(removed.getKey(), removed.getValue());
        }
    }

    protected void itemAccessed(CacheItem item) {
        LruCacheItem lc = (LruCacheItem) item;
        synchronized (this) {
	    if (lc.isTrimmed()) {
		lc.setTrimmed(false);
		numVictimsAccessed += 1;
		CacheItem overflow = super.itemAdded(item);
		if (overflow != null) {
		    trimItem(overflow);
		}
	    } else {
		super.itemAccessed(item);
	    }
	}
    }

    public int getLoadFromBackupCount() {
        return loadFromBackupCount;
    }

    protected void incrementLoadFromBackupCount() {
        synchronized (loadCountLock) {
            loadFromBackupCount++;
        }
    }

    // return the EJB for the given instance key
    //Called from StatefulSessionContainer
    public StatefulEJBContext lookupEJB(Serializable sessionKey,
        SFSBContainerCallback container, Object cookie)
    {
        int hashCode = hash(sessionKey);
        int index = getIndex(hashCode);
        CacheItem item = null;
        LruSessionCacheItem newItem = null;
        Object value = null;

        synchronized (bucketLocks[index]) {
            item = buckets[index];
            for (; item != null; item = item.getNext()) {
                if ( (hashCode == item.getHashCode()) && 
                     (item.getKey().equals(sessionKey)) )
                {
                    value = item.getValue();
                    break;
                }
            }
            
            // update the stats in line
            if (value != null) {
                itemAccessed(item);
            }
        }

        
        if (value != null) {
            incrementHitCount();
            return (StatefulEJBContext) value;
        } 
        
        incrementMissCount();
        if (item != null) {
            synchronized (item) {
                LruSessionCacheItem lruItem = (LruSessionCacheItem) item;
                if ((lruItem.getValue() == null) && (lruItem.cacheItemState == CACHE_ITEM_LOADING)) {
                    lruItem.waitCount++;
                    try { item.wait(); } catch (InterruptedException inEx) {}
                }
                return (StatefulEJBContext) item.getValue();
            }
        }

        // don't try to lookup session store when passivation capable is false
        if (!container.isPassivationCapable()) {
            if (_logger.isLoggable(Level.INFO)) {
                _logger.log(Level.INFO, SFSB_NOT_FOUND_WHEN_PASSIVATION_DISABLED);                
            }
            return null;
        }

        //This is the thread that actually does the I/O
	long activationStartTime = -1;
	/*if (sfsbStoreMonitor.isMonitoringOn()) {
	    activationStartTime = System.currentTimeMillis();
	}*/
        try {
            value = getStateFromStore(sessionKey, container);
            newItem = new LruSessionCacheItem(hashCode, sessionKey,
                    value, -1, CACHE_ITEM_LOADING); 
            newItem.setNext( buckets[index] );
            buckets[index] = newItem;

            synchronized (buckets[index]) {
                if (value == null) {
                    //Remove the temp cacheItem that we created.
                    CacheItem prev = null;
                    for (CacheItem current = buckets[index]; current != null;
                            current = current.getNext())
                    {
                        if (current == newItem) {
                            if (prev == null) {
                                buckets[index] = current.getNext();
                            } else {
                                prev.setNext( current.getNext() );
                            }
                            current.setNext( null );
                            break;
                        }
                        prev = current;
                    }
                } else {
                    container.activateEJB(sessionKey,
                        (StatefulEJBContext) value, cookie);
		    // sfsbStoreMonitor.incrementActivationCount(true);

                    CacheItem overflow = itemAdded(newItem);
                    incrementEntryCount();
                    // make sure we are are not crossing the threshold
                    if (overflow != null) {
                        trimItem(overflow);
                    }
                }
            } //end of sync
        } catch (javax.ejb.EJBException ejbEx) {
	    //sfsbStoreMonitor.incrementActivationCount(false);
            remove(sessionKey);
            value = null;
        } finally {
            if (newItem != null) {
                synchronized (newItem) {
                    newItem.cacheItemState = CACHE_ITEM_VALID;
                    if (newItem.waitCount > 0) {
                        newItem.notifyAll();
                    }
                }
            }
	    if (activationStartTime != -1) {
		long timeSpent = System.currentTimeMillis()
		    - activationStartTime;
		//sfsbStoreMonitor.setActivationTime(timeSpent);
	    }
        }

        return (StatefulEJBContext) value;
    }

    //Called from StatefulSessionContainer
    //public void deleteEJB(Object sessionKey) 
    public Object remove(Object sessionKey) {
	return remove(sessionKey, true);
    }

    public Object remove(Object sessionKey, boolean removeFromStore) {
        int hashCode = hash(sessionKey);
        int index = getIndex(hashCode);

        CacheItem prev = null, item = null;

        synchronized (bucketLocks[index]) {
            for (item = buckets[index]; item != null; item = item.getNext()) {
                if ((hashCode == item.getHashCode()) && sessionKey.equals(item.getKey())) {
                    if (prev == null) {
                        buckets[index] = item.getNext();
                    } else  {
                        prev.setNext( item.getNext() );
                    }
                    item.setNext( null );
                    
                    itemRemoved(item);
                    ((LruSessionCacheItem) item).cacheItemState = CACHE_ITEM_REMOVED;
                    break;
                }
                prev = item;
            }

            //remove it from the BackingStore also
            //In case it had been checkpointed

            //  remove it from BackingStore outside sync block
	    if (removeFromStore) {
		try {
            if (backingStore != null) {
		        backingStore.remove((Serializable) sessionKey);
            }
		} catch (BackingStoreException sfsbEx) {
		    _logger.log(Level.WARNING, EXCEPTION_BACKING_STORE_REMOVE, new Object[]{cacheName, sessionKey, sfsbEx});
		}
	    }
	}
        
        if (item != null) {
            decrementEntryCount();
            incrementRemovalCount();
            
            incrementHitCount();
        } else {
            incrementMissCount();
        }
        
        return null;
    }
    
    /**
     * Called by StatefulSessionContainer before passivation to determine whether
     * or not removal-timeout has elapsed for a cache item.  If so, it will be
     * directly removed instead of passivated.  See issue 16188.
     */
    public boolean eligibleForRemovalFromCache(StatefulEJBContext ctx, Serializable sessionKey) {
        if (removeIfIdle) {
            long idleThreshold = System.currentTimeMillis()
                    - removalTimeoutInSeconds * 1000L;
            if (ctx.getLastAccessTime() <= idleThreshold) {
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE, cacheName
                            + ": Removing session "
                            + " instead of passivating for key: " + sessionKey);
                }
                return true;
            }
        }
        return false;
    }

    // Called by Cache implementation thru container, on Recycler's thread
    // The container has already acquired the lock on the StatefulEJBContext
    public boolean passivateEJB(StatefulEJBContext ctx, Serializable sessionKey)
	throws java.io.NotSerializableException
    {
        try {
            int hashCode = hash(sessionKey);
            int index = getIndex(hashCode);
            
            CacheItem prev = null, item = null;
            synchronized (bucketLocks[index]) {
                for (item = buckets[index]; item != null; item = item.getNext()) {
                    if (item.getValue() == ctx) {
                        LruCacheItem lruSCItem = (LruCacheItem) item;
                        if (!lruSCItem.isTrimmed()) {
                            //Was accessed just after marked for passivation
                            if(_logger.isLoggable(Level.FINE)) {
                                _logger.log(Level.FINE, cacheName +  ": session accessed after marked for passivation: " + sessionKey);
                            }
                            return false;
                        }
                        break;
                    }
                    prev = item;
                }

                if (item == null) {
                    //Could have been removed
                    return true; //???????
                }
            }

            // we don't do passivation when passivation is disabled, but we still need remove the trimmed
            // bean from cache
            if (container.isPassivationCapable() && !saveStateToStore(sessionKey, ctx)) {
                return false;
            }

            synchronized (bucketLocks[index]) {
                prev = null;
                for (item = buckets[index]; item != null; item = item.getNext()) {
                    if (item.getValue() == ctx) {
                        LruCacheItem lruSCItem = (LruCacheItem) item;
                        if (!lruSCItem.isTrimmed()) {
                            //Was accessed just after marked for passivation
                            return false;
                        }
                        
                    	if (prev == null) {
                            buckets[index] = item.getNext();
                    	} else  {
                            prev.setNext( item.getNext() );
                    	}
                    	item.setNext( null );
                        break;
                    }
                    prev = item;
                }
            }
            
            if (item != null) {
            	decrementEntryCount();
                incrementRemovalCount();
            }
            
            return true;
        } catch (java.io.NotSerializableException notSerEx) {
            _logger.log(Level.FINE, "", notSerEx);
	    throw notSerEx;
        } catch (Exception ex) {
            _logger.log(Level.WARNING, PASSIVATE_EJB_EXCEPTION_CAUGHT, new Object[]{cacheName, ex});
            _logger.log(Level.FINE, "", ex);
        }
        return false;
    } //passivateEJB

    private Object getStateFromStore(Serializable sessionKey, SFSBContainerCallback container) {

        Object object = null;

        try {
            SimpleMetadata beanState = null;
            if (backingStore != null) {
                beanState = backingStore.load(sessionKey, null);
            }
            byte[] data = (beanState != null)
                ? beanState.getState()
                : null;
            if ( data == null ) {
                if(_logger.isLoggable(Level.SEVERE)) {
                    _logger.log(Level.SEVERE, CANNOT_LOAD_FROM_BACKUP_STORE, new Object[]{cacheName, sessionKey});
                }
            }  else {
		//sfsbStoreMonitor.setActivationSize(data.length);
                incrementLoadFromBackupCount();
                object = container.deserializeData(data);
            }
        } catch ( Exception ex ) {
            _logger.log(Level.SEVERE, EXCEPTION_LOADING_BACKUP_SESSION, new Object[]{cacheName, sessionKey, ex});
        } catch ( Error ex ) {
            _logger.log(Level.SEVERE, ERROR_LOADING_BACKUP_SESSION, new Object[]{cacheName, sessionKey, ex});
        }

        return object;
    }

    private boolean saveStateToStore(Serializable sessionKey, StatefulEJBContext ctx)
	throws java.io.NotSerializableException, java.io.IOException
    {
        byte[] data = container.serializeContext(ctx);

	//If we are here then we were able to serialize the object successfully
        boolean status = false;
	
	if (data != null) {
	    SimpleMetadata beanState = new SimpleMetadata(
		ctx.getVersion(), ctx.getLastAccessTime(), removalTimeoutInSeconds*1000L, data);
        
        //Note: Don't increment the version here because
        //  this is called on an async thread and the client
        //  already has the correct version
        beanState.setVersion(ctx.getVersion());
	    try {
            if(backingStore != null) {
                backingStore.save(sessionKey, beanState, !ctx.existsInStore());
                // sfsbStoreMonitor.setPassivationSize(data.length);
                status = true;
            }
	    } catch (BackingStoreException sfsbEx) {
		_logger.log(Level.WARNING, EXCEPTION_DURING_PASSIVATE_SAVE, new Object[]{cacheName, sessionKey, sfsbEx});
	    }
	}

	return status;
    }
    
    private void trimSelectedVictims(ArrayList victims) {
        int sz = victims.size();
        
        synchronized (this) {
            trimCount += sz;
        }
        CacheItem item = null;
        for (int i=0; i<sz; i++) {
            item = (CacheItem) victims.get(i);
            trimItem(item);
    	}
    }

    public void setShutdownState() {
	currentCacheState = STATE_SHUTTING_DOWN;
    }

    public void setUndeployedState() {
	currentCacheState = STATE_UNDEPLOYING;
    }

    /**
     * get an Iterator for the values stored in the cache
     * @returns an Iterator
     */
    public Iterator values() {
        ArrayList valueList = new ArrayList();

	synchronized (this) {
            LruCacheItem item = tail;
            while (item != null) {
                StatefulEJBContext ctx = (StatefulEJBContext) item.getValue();
                if (ctx != null) {
		    valueList.add(ctx);
		}

                //Ensure that for head the lPrev is null
                if( (item == head) && (item.getLPrev() != null) ) {
                    _logger.log(Level.WARNING, ITERATOR_RESETTING_HEAD_LPREV, cacheName);
                    item.setLPrev(null);
                }
                // traverse to the previous one
                item = item.getLPrev();
	    }
	}

        return valueList.iterator();
    }
    
    public void shutdown() {
        ArrayList<StatefulEJBContext> valueList = new ArrayList<StatefulEJBContext>();

        synchronized (this) {
            LruCacheItem item = tail;
            while (item != null) {
                StatefulEJBContext ctx = (StatefulEJBContext) item.getValue();
                if (ctx != null) {
                    item.setTrimmed(true);
                    valueList.add(ctx);
                }

                // Ensure that for head the lPrev is null
                if ((item == head) && (item.getLPrev() != null)) {
                    _logger.log(Level.WARNING, ITERATOR_RESETTING_HEAD_LPREV, cacheName);
                    item.setLPrev(null);
                }
                // traverse to the previous one
                item = item.getLPrev();
            }
        }

        for (StatefulEJBContext ctx : valueList) {
            container.passivateEJB(ctx);
        }
    }
    

    /**
     * trim the timedOut entries from the cache.
     * This call is to be scheduled by a thread managed by the container.
     * In this case a sorted LRU list exists based on access time and this
     * list is scanned
     */
    public void trimTimedoutItems(int  maxTrimCount) {
        
        int count = 0;
        LruCacheItem item;
        long currentTime = System.currentTimeMillis();
        long idleThresholdTime = currentTime - cacheIdleTimeoutInSeconds*1000L;
        ArrayList victimList = new ArrayList();

        synchronized (this) {
            if(_logger.isLoggable(Level.FINE)) {
            	_logger.log(Level.FINE, 
                    "[" + cacheName + "]: TrimTimedoutBeans started...");
            }
           
            if (tail == null) {	// No LRU list exists
                if(_logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE, 
                                "[" + cacheName + "]: TrimTimedoutBeans "
                                + " finished after removing 0 idle beans");
                }
                head = null;
                return;
            }
            // traverse LRU list and collect victims into the victimList
            item = tail;
            while (true) {
		if (currentCacheState != STATE_RUNNING) {
                    _logger.log(Level.WARNING, EXITING_TRIM_TIMEDOUT_BEANS, new Object[]{cacheName, currentCacheState});
		    break;
		}

                StatefulEJBContext ctx = (StatefulEJBContext) item.getValue();
                if (ctx != null) {
                    // if we found a valid item, add it to the list
                    if ((ctx.getLastAccessTime() <= idleThresholdTime) &&
                        ctx.canBePassivated()) {
                        item.setTrimmed(true);
                        victimList.add(item);
                    } else {
                        break;
                    }
                }
                //Ensure that for head the lPrev is null
                if( (item == head) && (item.getLPrev() != null) ) {
                    _logger.log(Level.WARNING, TRIM_TIMEDOUT_BEANS_RESETTING_HEAD_LPREV, cacheName);
                    item.setLPrev(null);
                }
                // traverse to the previous one
                item = item.getLPrev();
                if (item == null) {
                    break;
                }
                //for the last item that was picked up as a victim disconnect
                //it from the list
                item.getLNext().setLPrev(null);
                item.getLNext().setLNext(null);

                item.setLNext(null);
            }
            if (item == tail) {			
                // no items were selected for trimming
                if(_logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE, 
                                "[" + cacheName + "]: TrimTimedoutBeans "
                                + " finished after removing 0 idle beans");
                }
                return;
            }

            // there is at least one item selected for trimming
            if (item == null)
                head = null;

            tail = item;
            count = victimList.size();
            listSize -= count;
            trimCount += count;
        }
        
        // trim the items from the BaseCache
        for (int idx = 0;idx < count; idx++) {
            trimItem((LruCacheItem) victimList.get(idx));
        }

        if(_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, 
                        "[" + cacheName + "]: TrimTimedoutBeans "
                        + " finished after removing " + count + " idle beans");
        }
    }

    /**
     * This method picks idle items from a cache which does not have a sorted
     * LRU list
     * NRU cache at light loads and FIFO caches do not maintain a LRU list and
     * hence they have to scan the entire cache and select victims
     **/
    public void trimUnSortedTimedoutItems(int  maxCount) {
        int maxIndex = buckets.length;
        long idleThreshold = System.currentTimeMillis() - timeout;
        ArrayList victims = new ArrayList();
        int sz = 0;
        int totalSize = 0;
        
        if(_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, 
                        "[" + cacheName + "]: TrimUnsortedTimedoutBeans started...");
        }
        // Go through each bucket in the cache and if there are entries in that
        // bucket scan them and select victims
        for (int index = 0; index < maxIndex ; index++) {

            if (buckets[index] != null) {
                synchronized (bucketLocks[index]) {
                    for (CacheItem item = buckets[index]; item != null; 
                         item = item.getNext()) {
                        StatefulEJBContext ctx = 
                            (StatefulEJBContext) item.getValue();
                        //Note ctx can be null if bean is in BEING_REFRESHED state
                        if ((ctx != null) && 
                            (ctx.getLastAccessTime() <= idleThreshold) &&
                            ctx.canBePassivated()) {
                            LruCacheItem litem = (LruCacheItem)item;
                            synchronized (this) {
				if (currentCacheState != STATE_RUNNING) {
				    _logger.log(Level.WARNING, EXITING_TRIM_UNSORTED_TIMEDOUT_BEANS,
                            new Object[]{cacheName, currentCacheState});
				    break;
				}
                                if (!litem.isTrimmed()) {
                                    itemRemoved(litem);
                                    litem.setTrimmed(true);
                                    victims.add(litem);
                                }
                            }
                        }
                    }
                }
                // Check and see if we have collected enough victims 
                // to start a cleaner task
                sz = victims.size();
                if (sz >= container.getPassivationBatchCount()) {
                    trimSelectedVictims(victims);
                    totalSize += sz;
                    victims.clear();
                }
            }
        }

        sz = victims.size();
        if (sz > 0) {
            trimSelectedVictims(victims);
            totalSize += sz;
        }
        if(_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "[" + cacheName + "]: TrimUnsortedTimedoutBeans "
                + " finished after removing " + totalSize + " idle beans");
        }
    }

    public int getNumVictimsAccessed() {
        return numVictimsAccessed;
    }

    protected CacheItem createItem(int hashCode, Object sessionKey, 
            Object value, int size)
    {
        return new LruSessionCacheItem(hashCode, sessionKey, value, size);
    }

    //*Class for LruSessionCacheItem
    protected static class LruSessionCacheItem
        extends LruCacheItem
    {

        protected byte waitCount;
        protected byte cacheItemState = CACHE_ITEM_VALID;

        protected LruSessionCacheItem(int hashCode, Object key, Object value, 
                               int size)
        {
            super(hashCode, key, value, size);
            this.cacheItemState = CACHE_ITEM_VALID;
        }

        protected LruSessionCacheItem(int hashCode, Object key, Object value, 
                               int size, byte state)
        {
            super(hashCode, key, value, size);
            this.cacheItemState = state;
        }
    }

    public void setConfigData(String configData) {
	this.configData = configData;
    }

    //Implementation of EjbCacheStatsProviderDelegate

    public void appendStats(StringBuffer sbuf) {
	sbuf.append("[Cache: ")
	    .append("Size=").append(entryCount).append("; ")
	    .append("HitCount=").append(hitCount).append("; ")
	    .append("MissCount=").append(missCount).append("; ")
	    .append("Passivations=").append(getNumPassivations()).append("; ");
	if (configData != null) {
	    sbuf.append(configData);
	}
	sbuf.append("]");
    }

    public int getCacheHits() {
	return hitCount;
    }

    public int getCacheMisses() {
	return missCount;
    }

    public int getNumBeansInCache() {
	return entryCount;
    }

    public int getNumExpiredSessionsRemoved() {
	/*return (sfsbStoreMonitor == null)
        ? 0 : sfsbStoreMonitor.getNumExpiredSessionsRemoved(); */
        return 0;
    }

    public int getNumPassivationErrors() {
	/* return (sfsbStoreMonitor == null)
        ? 0 : sfsbStoreMonitor.getNumPassivationErrors(); */
        return 0;
    }

    public int getNumPassivations() {
	 /* return (sfsbStoreMonitor == null)
        ? 0 : sfsbStoreMonitor.getNumPassivations(); */
        return 0;
    }

    public int getNumPassivationSuccess() {
	/*return (sfsbStoreMonitor == null)
        ? 0 : sfsbStoreMonitor.getNumPassivationSuccess(); */
        return 0;
    }

    public void setMaxCacheSize(int val) {
	this.confMaxCacheSize = val;
    }

    public int getMaxCacheSize() {
	return confMaxCacheSize;
    }

}

