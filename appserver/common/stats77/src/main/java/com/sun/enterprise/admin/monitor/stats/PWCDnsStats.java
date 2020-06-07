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
// Portions Copyright [2019] Payara Foundation and/or affiliates

package com.sun.enterprise.admin.monitor.stats;

import javax.management.j2ee.statistics.Stats;
import javax.management.j2ee.statistics.CountStatistic;

/** 
 * The DNS Cache caches IP addresses and DNS names. The server’s DNS cache is
 * disabled by default. A single cache entry represents a single IP address or DNS
 * name lookup
 * @author  nsegura
 */
public interface PWCDnsStats extends Stats {
    
    /**
     * Indicates whether the DNS cache is enabled or disable. Default is disabled.
     * @return DNS cache enabled?
     */
    public CountStatistic getFlagCacheEnabled();
    
    /** 
     * The number of current cache entries
     * @return current cache entries
     */
    public CountStatistic getCountCacheEntries();
    
    /** 
     * The maximum number of cache entries
     * @return max cache entries
     */
    public CountStatistic getMaxCacheEntries();
    
    /** 
     * The number of cache hits
     * @return cache hits
     */
    public CountStatistic getCountCacheHits();
    
    /** 
     * The number of cache misses
     * @return cache misses
     */
    public CountStatistic getCountCacheMisses();
    
    /** 
     * Returns whether asynchronic lookup is enabled. 1 if true, 0 otherwise
     * @return enabled
     */
    public CountStatistic getFlagAsyncEnabled();
    
    /** 
     * The total number of asynchronic name lookups
     * @return asyn name lookups
     */
    public CountStatistic getCountAsyncNameLookups();
    
    /** 
     * The total number of asynchronic address lookups
     * @return asyn address lookups
     */
    public CountStatistic getCountAsyncAddrLookups();
    
    /** 
     * The number of asynchronic lookups in progress
     * @return async lookups in progress
     */
    public CountStatistic getCountAsyncLookupsInProgress();
    
}
