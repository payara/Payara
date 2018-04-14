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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// Portions Copyright [2017] [Payara Foundation and/or its affiliates]
package org.apache.naming.resources;

import java.util.HashMap;
import java.security.SecureRandom;

/**
 * Implements a special purpose cache.
 * 
 * @author <a href="mailto:remm@apache.org">Remy Maucherat</a>
 * @version $Revision: 1.3 $
 */
public class ResourceCache {
    
    
    // ----------------------------------------------------------- Constructors
    
    
    public ResourceCache() {
    }
    
    
    // ----------------------------------------------------- Instance Variables
      
    
    /**
     * Random generator used to determine elements to free.
     */
    protected SecureRandom random = new SecureRandom();
    
    
    /**
     * Cache.
     * Path -> Cache entry.
     */
    protected CacheEntry[] cache = new CacheEntry[0];


    /**
     * Not found cache.
     */
    protected HashMap<String, CacheEntry> notFoundCache =
        new HashMap<String, CacheEntry>();


    /**
     * Max size of resources which will have their content cached.
     */
    protected int cacheMaxSize = 10240; // 10 MB


    /**
     * Max amount of removals during a make space.
     */
    protected int maxAllocateIterations = 20;


    /**
     * Entry hit ratio at which an entry will never be removed from the cache.
     * Compared with entry.access / hitsCount
     */
    protected long desiredEntryAccessRatio = 3;


    /**
     * Spare amount of not found entries.
     */
    protected int spareNotFoundEntries = 500;


    /**
     * Current cache size in KB.
     */
    protected int cacheSize = 0;


    /**
     * Number of accesses to the cache.
     */
    protected long accessCount = 0;


    /**
     * Number of cache hits.
     */
    protected long hitsCount = 0;


    // ------------------------------------------------------------- Properties


    /**
     * Return the access count.
     * Note: Update is not synced, so the number may not be completely 
     * accurate.
     */
    public long getAccessCount() {
        return accessCount;
    }


    /**
     * Return the maximum size of the cache in KB.
     */
    public int getCacheMaxSize() {
        return cacheMaxSize;
    }


    /**
     * Set the maximum size of the cache in KB.
     */
    public void setCacheMaxSize(int cacheMaxSize) {
        this.cacheMaxSize = cacheMaxSize;
    }


    /**
     * Return the current cache size in KB.
     */
    public int getCacheSize() {
        return cacheSize;
    }


    /**
     * Return desired entry access ratio.
     */
    public long getDesiredEntryAccessRatio() {
        return desiredEntryAccessRatio;
    }


    /**
     * Set the desired entry access ratio.
     */
    public void setDesiredEntryAccessRatio(long desiredEntryAccessRatio) {
        this.desiredEntryAccessRatio = desiredEntryAccessRatio;
    }


    /**
     * Return the number of cache hits.
     * Note: Update is not synced, so the number may not be completely 
     * accurate.
     */
    public long getHitsCount() {
        return hitsCount;
    }


    /**
     * Return the maximum amount of iterations during a space allocation.
     */
    public int getMaxAllocateIterations() {
        return maxAllocateIterations;
    }


    /**
     * Set the maximum amount of iterations during a space allocation.
     */
    public void setMaxAllocateIterations(int maxAllocateIterations) {
        this.maxAllocateIterations = maxAllocateIterations;
    }


    /**
     * Return the amount of spare not found entries.
     */
    public int getSpareNotFoundEntries() {
        return spareNotFoundEntries;
    }


    /**
     * Set the amount of spare not found entries.
     */
    public void setSpareNotFoundEntries(int spareNotFoundEntries) {
        this.spareNotFoundEntries = spareNotFoundEntries;
    }


    // --------------------------------------------------------- Public Methods


    public boolean allocate(int space) {

        int toFree = space - (cacheMaxSize - cacheSize);

        if (toFree <= 0) {
            return true;
        }

        // Increase the amount to free so that allocate won't have to run right
        // away again
        toFree += (cacheMaxSize / 20);

        int size = notFoundCache.size();
        if (size > spareNotFoundEntries) {
            notFoundCache.clear();
            cacheSize -= size;
            toFree -= size;
        }

        if (toFree <= 0) {
            return true;
        }

        int attempts = 0;
        int entriesFound = 0;
        long totalSpace = 0;
        int[] toRemove = new int[maxAllocateIterations];
        while (toFree > 0) {
            if (attempts == maxAllocateIterations) {
                // Give up, no changes are made to the current cache
                return false;
            }
            if (toFree > 0) {
                // Randomly select an entry in the array
                int entryPos = -1;
                boolean unique = false;
                int count = 0;
                while (!unique) {
                    unique = true;
                    entryPos = random.nextInt(cache.length) ;
                    // Guarantee uniqueness
                    for (int i = 0; i < entriesFound; i++) {
                        if (toRemove[i] == entryPos) {
                            unique = false;
                        }
                    }
                }
                long entryAccessRatio = 
                    ((cache[entryPos].accessCount * 100) / accessCount);
                if (entryAccessRatio < desiredEntryAccessRatio) {
                    toRemove[entriesFound] = entryPos;
                    totalSpace += cache[entryPos].size;
                    toFree -= cache[entryPos].size;
                    entriesFound++;
                }
            }
            attempts++;
        }

        // Now remove the selected entries
        java.util.Arrays.sort(toRemove, 0, entriesFound);
        CacheEntry[] newCache = new CacheEntry[cache.length - entriesFound];
        int pos = 0;
        int n = -1;
        if (entriesFound > 0) {
            n = toRemove[0];
            for (int i = 0; i < cache.length; i++) {
                if (i == n) {
                    if ((pos + 1) < entriesFound) {
                        n = toRemove[pos + 1];
                        pos++;
                    } else {
                        pos++;
                        n = -1;
                    }
                } else {
                    newCache[i - pos] = cache[i];
                }
            }
        }
        cache = newCache;
        cacheSize -= totalSpace;

        return true;

    }


    public CacheEntry lookup(String name) {

        CacheEntry cacheEntry = null;
        accessCount++;
        int pos = find(cache, name);
        if ((pos != -1) && (name.equals(cache[pos].name))) {
            cacheEntry = cache[pos];
        }
        if (cacheEntry == null) {
            try {
                cacheEntry = notFoundCache.get(name);
            } catch (Exception e) {
                // Ignore: the reliability of this lookup is not critical
            }
        }
        if (cacheEntry != null) {
            hitsCount++;
        }
        return cacheEntry;

    }


    public void load(CacheEntry entry) {
        if (entry.exists) {
            if (insertCache(entry)) {
                cacheSize += entry.size;
            }
        } else {
            int sizeIncrement = (notFoundCache.get(entry.name) == null) ? 1 : 0;
            notFoundCache.put(entry.name, entry);
            cacheSize += sizeIncrement;
        }
    }


    public boolean unload(String name) {
        CacheEntry removedEntry = removeCache(name);
        if (removedEntry != null) {
            cacheSize -= removedEntry.size;
            return true;
        } else if (notFoundCache.remove(name) != null) {
            
            cacheSize--;
            return true;
        }
        return false;
    }


    /**
     * Find a map elemnt given its name in a sorted array of map elements.
     * This will return the index for the closest inferior or equal item in the
     * given array.
     */
    private static final int find(CacheEntry[] map, String name) {

        int a = 0;
        int b = map.length - 1;

        // Special cases: -1 and 0
        if (b == -1) {
            return -1;
        }
        if (name.compareTo(map[0].name) < 0) {
            return -1;
        }
        if (b == 0) {
            return 0;
        }

        int i = 0;
        while (true) {
            i = (b + a) >>> 1;
            int result = name.compareTo(map[i].name);
            if (result > 0) {
                a = i;
            } else if (result == 0) {
                return i;
            } else {
                b = i;
            }
            if ((b - a) == 1) {
                int result2 = name.compareTo(map[b].name);
                if (result2 < 0) {
                    return a;
                } else {
                    return b;
                }
            }
        }

    }


    /**
     * Insert into the right place in a sorted MapElement array, and prevent
     * duplicates.
     */
    private final boolean insertCache(CacheEntry newElement) {
        CacheEntry[] oldCache = cache;
        int pos = find(oldCache, newElement.name);
        if ((pos != -1) && (newElement.name.equals(oldCache[pos].name))) {
            return false;
        }
        CacheEntry[] newCache = new CacheEntry[cache.length + 1];
        System.arraycopy(oldCache, 0, newCache, 0, pos + 1);
        newCache[pos + 1] = newElement;
        System.arraycopy
            (oldCache, pos + 1, newCache, pos + 2, oldCache.length - pos - 1);
        cache = newCache;
        return true;
    }


    /**
     * Insert into the right place in a sorted MapElement array.
     */
    private final CacheEntry removeCache(String name) {
        CacheEntry[] oldCache = cache;
        int pos = find(oldCache, name);
        if ((pos != -1) && (name.equals(oldCache[pos].name))) {
            CacheEntry[] newCache = new CacheEntry[cache.length - 1];
            System.arraycopy(oldCache, 0, newCache, 0, pos);
            System.arraycopy(oldCache, pos + 1, newCache, pos, 
                             oldCache.length - pos - 1);
            cache = newCache;
            return oldCache[pos];
        }
        return null;
    }


}
