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

/**
 *
 * @author  nsegura
 */

import org.glassfish.j2ee.statistics.Stats;
import org.glassfish.j2ee.statistics.CountStatistic;

/** Provides statistical information on the httpservice file cache */
public interface PWCFileCacheStats extends Stats {
    
    /** 
     * Returns whether the file cache us enabled. 1 of enabled, 0 otherwise
     * @return enabled
     */
    public CountStatistic getFlagEnabled();
    
    /** 
     * The maximum age of a valid cache entry
     * @return cache entry max age
     */
    public CountStatistic getSecondsMaxAge();
    
    /** 
     * The number of current cache entries.  A single cache entry represents a single URI
     * @return current cache entries
     */
    public CountStatistic getCountEntries();
    
    /** The maximum number of cache entries
     * @return max cache entries
     */
    public CountStatistic getMaxEntries();
    
    /** 
     * The number of current open cache entries
     * @return open cache entries
     */
    public CountStatistic getCountOpenEntries();
    
    /** 
     * The Maximum number of open cache entries
     * @return Max open cache entries
     */
    public CountStatistic getMaxOpenEntries();
    
    /** 
     * The  Heap space used for cache
     * @return heap size
     */
    public CountStatistic getSizeHeapCache();
    
    /** 
     * The Maximum heap space used for cache
     * @return Max heap size
     */
    public CountStatistic getMaxHeapCacheSize();
    
    /** 
     * The size of Mapped memory used for caching
     * @return Mapped memory size
     */
    public CountStatistic getSizeMmapCache();
    
    /** 
     * The Maximum Memory Map size to be used for caching
     * @return Max Memory Map size
     */
    public CountStatistic getMaxMmapCacheSize();
    
    /** 
     * The Number of cache lookup hits
     * @return cache hits
     */
    public CountStatistic getCountHits();
    
    /** 
     * The Number of cache lookup misses
     * @return cache misses
     */
    public CountStatistic getCountMisses();
    
    /** 
     * The Number of hits on cached file info
     * @return hits on cached file info
     */
    public CountStatistic getCountInfoHits();
    
    /** 
     * The Number of misses on cached file info
     * @return misses on cache file info
     */
    public CountStatistic getCountInfoMisses();
    
    /** 
     * The Number of hits on cached file content
     * @return hits on cache file content
     */
    public CountStatistic getCountContentHits();
    
    /** 
     * The Number of misses on cached file content
     * @return missed on cached file content
     */
    public CountStatistic getCountContentMisses();
    
}
