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

package com.sun.gjc.spi.base.datastructure;

import com.sun.gjc.monitoring.StatementCacheProbeProvider;
import com.sun.gjc.spi.base.CacheObjectKey;
import com.sun.gjc.spi.base.PreparedStatementWrapper;
import com.sun.logging.LogDomains;
import org.glassfish.resourcebase.resources.api.PoolInfo;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Shalini M
 */
public class LRUCacheImpl implements Cache {

    /**
     * Stores the objects for statement caching
     */
    private Map<CacheObjectKey, CacheEntry> list;
    /**
     * Size of the cache
     */
    private int maxSize ;
    protected final static Logger _logger;
    private StatementCacheProbeProvider probeProvider = null;
    private PoolInfo poolInfo;

    static {
        _logger = LogDomains.getLogger(LRUCacheImpl.class, LogDomains.RSR_LOGGER);
    }

    public LRUCacheImpl(PoolInfo poolInfo, int maxSize){
        this.maxSize = maxSize;
        this.poolInfo = poolInfo;
        list = new LinkedHashMap<CacheObjectKey, CacheEntry>();
        try {
            probeProvider = new StatementCacheProbeProvider();
        } catch(Exception ex) {
            //TODO logger
        }
    }

    /**
     * Check if an entry is found for this key object. If found, the entry is
     * put in the result object and back into the list.
     * 
     * @param key key whose mapping entry is to be checked.
     * @return result object that contains the key with the entry if not 
     * null when
     * (1) object not found in cache
     */
    public Object checkAndUpdateCache(CacheObjectKey key) {
        Object result = null;
        CacheEntry entry = list.get(key);        
        if(entry != null) {
            //Cache hit
            result = entry.entryObj;
            if(_logger.isLoggable(Level.FINEST)) {
                _logger.finest("Cache Hit");
            }
            //TODO-SC Busy cache hits?
            probeProvider.statementCacheHitEvent(poolInfo.getName(), poolInfo.getApplicationName(), poolInfo.getModuleName());
        } else {
            //Cache miss
            if(_logger.isLoggable(Level.FINEST)) {
                _logger.finest("Cache Miss");
            }
            probeProvider.statementCacheMissEvent(poolInfo.getName(), poolInfo.getApplicationName(), poolInfo.getModuleName());
        }
        return result;
    }
    
    /**
     * Add the key and entry value into the cache.
     * @param key key that contains the sql string and its type (PS/CS)
     * @param o entry that is the wrapper of PreparedStatement or 
     * CallableStatement
     * @param force If the already existing key is to be overwritten
     */
    public void addToCache(CacheObjectKey key, Object o, boolean force) {
        if(force || !list.containsKey(key)){
            //overwrite or if not already found in cache

            if(list.size() >= maxSize){
                purge();
            }
            CacheEntry entry = new CacheEntry(o);
            list.put(key, entry);
        }
    }

    /**
     * Clears the statement cache
     */
    public void clearCache(){
        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("clearing objects in cache");
        }
       list.clear();
    }

    public void flushCache() {
        while(list.size()!=0){
            purge();
        }
    }

    public void purge() {
        Iterator keyIterator = list.keySet().iterator();
        while(keyIterator.hasNext()){
            CacheObjectKey key = (CacheObjectKey)keyIterator.next();
            CacheEntry entry = list.get(key);
            try{
                //TODO Move to a more generic Contract and invoke close()
                //PreparedStatementWrapper could implement the contract instead
                PreparedStatementWrapper ps = (PreparedStatementWrapper)entry.entryObj;
                ps.setCached(false);
                ps.close();
            }catch(SQLException e){
                //ignore
            }
            keyIterator.remove();
            break;
        }
    }

    // Used only for purging the bad statements.
    public void purge(Object obj) {
        PreparedStatementWrapper tmpPS = (PreparedStatementWrapper) obj;
        Iterator keyIterator = list.keySet().iterator();
        while(keyIterator.hasNext()){
            CacheObjectKey key = (CacheObjectKey)keyIterator.next();
            CacheEntry entry = list.get(key);
            try{
                //TODO Move to a more generic Contract and invoke close()
                //PreparedStatementWrapper could implement the contract instead
                PreparedStatementWrapper ps = (PreparedStatementWrapper)entry.entryObj;
                if(ps.equals(tmpPS)) {
                    //Found the entry in the cache. Remove this entry.
                    if(_logger.isLoggable(Level.FINEST)) {
                        _logger.log(Level.FINEST, "Purging an entry from cache");
                    }
                    ps.setCached(false);
                    ps.close();
                }
            }catch(SQLException e){
                //ignore
            }
            keyIterator.remove();
            break;
        }        
    }
    
    /**
     * Returns the number of entries in the statement cache
     * @return has integer value
     */
    public int getSize() {
       return list.size();
    }

    public int getMaxSize() {
        return maxSize;
    }

    /**
     * Cache object that has an entry. This is used to put inside the
     * statement cache.
     */
    public static class CacheEntry{
        private Object entryObj;

        public CacheEntry(Object o){
            this.entryObj = o;
        }
    }

    /*public Set getObjects(){
        //TODO-SC-DEFER can the set be "type-safe"
        Set set = new HashSet();
        for(CacheEntry entry : list.values()){
            set.add(entry.entryObj);
        }
        return set;
    }*/

    public boolean isSynchronized() {
        return false;
    }
}
