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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
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

package com.sun.appserv.util.cache;

/**
 * define all cache related constants
 */
public class Constants {
    
    private Constants() {} 
    
    public static final String STAT_DEFAULT = "default";

    // default maximum number of entries in the cache
    public static final int DEFAULT_MAX_ENTRIES = 8192;

    // default maximum size in bytes of the cache
    public static final long DEFAULT_MAX_CACHE_SIZE = Long.MAX_VALUE;

    // maxSize specified in bytes, KB or MB
    public static final int KB = 1024;
    public static final int MB = (KB * KB);

    public static final String STAT_BASECACHE_MAX_ENTRIES="cache.BaseCache.stat_maxEntries";
    public static final String STAT_BASECACHE_THRESHOLD="cache.BaseCache.stat_threshold";
    public static final String STAT_BASECACHE_TABLE_SIZE="cache.BaseCache.stat_tableSize";
    public static final String STAT_BASECACHE_ENTRY_COUNT="cache.BaseCache.stat_entryCount";
    public static final String STAT_BASECACHE_HIT_COUNT="cache.BaseCache.stat_hitCount";
    public static final String STAT_BASECACHE_MISS_COUNT="cache.BaseCache.stat_missCount";
    public static final String STAT_BASECACHE_REMOVAL_COUNT="cache.BaseCache.stat_removalCount";
    public static final String STAT_BASECACHE_REFRESH_COUNT="cache.BaseCache.stat_refreshCount";
    public static final String STAT_BASECACHE_OVERFLOW_COUNT="cache.BaseCache.stat_overflowCount";
    public static final String STAT_BASECACHE_ADD_COUNT="cache.BaseCache.stat_addCount";

    public static final String STAT_LRUCACHE_LIST_LENGTH="cache.LruCache.stat_lruListLength";
    public static final String STAT_LRUCACHE_TRIM_COUNT="cache.LruCache.stat_trimCount";

    public static final String STAT_MULTILRUCACHE_SEGMENT_SIZE="cache.MultiLruCache.stat_segmentSize";
    public static final String STAT_MULTILRUCACHE_SEGMENT_LIST_LENGTH="cache.MultiLruCache.stat_segmentListLength"; 
    public static final String STAT_MULTILRUCACHE_TRIM_COUNT="cache.MultiLruCache.stat_trimCount";

    public static final String STAT_BOUNDEDMULTILRUCACHE_CURRENT_SIZE="cache.BoundedMultiLruCache.stat_currentSize";
    public static final String STAT_BOUNDEDMULTILRUCACHE_MAX_SIZE="cache.BoundedMultiLruCache.stat_maxSize";
}
