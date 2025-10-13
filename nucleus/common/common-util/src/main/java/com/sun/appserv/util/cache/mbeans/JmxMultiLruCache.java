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

package com.sun.appserv.util.cache.mbeans;

import com.sun.appserv.util.cache.MultiLruCache;
import com.sun.appserv.util.cache.Constants;
/**
 * This class provides implementation for JmxLruCache MBean
 *
 * @author Krishnamohan Meduri (Krishna.Meduri@Sun.com)
 * @version 1.4
 * @since May 4, 2005 
 */
public class JmxMultiLruCache extends JmxBaseCache 
                              implements JmxMultiLruCacheMBean {

    private final MultiLruCache multiLruCache;

    public JmxMultiLruCache(MultiLruCache multiLruCache, String name) {
        super(multiLruCache,name);
        this.multiLruCache = multiLruCache;
    }

    /**
     * Returns the number of entries that have been trimmed
     */
    @Override
    public Integer getTrimCount() {
        return (Integer) multiLruCache.getStatByName(
                                        Constants.STAT_MULTILRUCACHE_TRIM_COUNT);
    }

    /**
     * Returns the size of each segment
     */
    @Override
    public Integer getSegmentSize() {
        return (Integer) multiLruCache.getStatByName(
                                        Constants.STAT_MULTILRUCACHE_SEGMENT_SIZE);
    }

    /**
     * Returns the legnth of the segment list
     */
    @Override
    public Integer[] getSegmentListLength() {
        return (Integer[]) multiLruCache.getStatByName(
                                        Constants.STAT_MULTILRUCACHE_SEGMENT_LIST_LENGTH);
    }
}
