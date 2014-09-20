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

package com.sun.appserv.util.cache;

/**
 * define all cache related constants
 */
public class Constants {
    public final static String STAT_DEFAULT = "default";

    // default maximum number of entries in the cache
    public final static int DEFAULT_MAX_ENTRIES = 8192;

    // default maximum size in bytes of the cache
    public final static long DEFAULT_MAX_CACHE_SIZE = Long.MAX_VALUE;

    // maxSize specified in bytes, KB or MB
    public final static int KB = 1024;
    public final static int MB = (KB * KB);

    public final static String STAT_BASECACHE_MAX_ENTRIES="cache.BaseCache.stat_maxEntries";
    public final static String STAT_BASECACHE_THRESHOLD="cache.BaseCache.stat_threshold";
    public final static String STAT_BASECACHE_TABLE_SIZE="cache.BaseCache.stat_tableSize";
    public final static String STAT_BASECACHE_ENTRY_COUNT="cache.BaseCache.stat_entryCount";
    public final static String STAT_BASECACHE_HIT_COUNT="cache.BaseCache.stat_hitCount";
    public final static String STAT_BASECACHE_MISS_COUNT="cache.BaseCache.stat_missCount";
    public final static String STAT_BASECACHE_REMOVAL_COUNT="cache.BaseCache.stat_removalCount";
    public final static String STAT_BASECACHE_REFRESH_COUNT="cache.BaseCache.stat_refreshCount";
    public final static String STAT_BASECACHE_OVERFLOW_COUNT="cache.BaseCache.stat_overflowCount";
    public final static String STAT_BASECACHE_ADD_COUNT="cache.BaseCache.stat_addCount";

    public final static String STAT_LRUCACHE_LIST_LENGTH="cache.LruCache.stat_lruListLength";
    public final static String STAT_LRUCACHE_TRIM_COUNT="cache.LruCache.stat_trimCount";

    public final static String STAT_MULTILRUCACHE_SEGMENT_SIZE="cache.MultiLruCache.stat_segmentSize";
    public final static String STAT_MULTILRUCACHE_SEGMENT_LIST_LENGTH="cache.MultiLruCache.stat_segmentListLength"; 
    public final static String STAT_MULTILRUCACHE_TRIM_COUNT="cache.MultiLruCache.stat_trimCount";

    public final static String STAT_BOUNDEDMULTILRUCACHE_CURRENT_SIZE="cache.BoundedMultiLruCache.stat_currentSize";
    public final static String STAT_BOUNDEDMULTILRUCACHE_MAX_SIZE="cache.BoundedMultiLruCache.stat_maxSize";
}
