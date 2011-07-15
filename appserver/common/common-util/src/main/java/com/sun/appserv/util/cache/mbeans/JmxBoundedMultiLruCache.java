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

/**
 * @Version $Id: JmxBoundedMultiLruCache.java,v 1.5 2006/10/23 20:55:57 jluehe Exp $
 * Created on May 4, 2005 07:40 PM
 */

package com.sun.appserv.util.cache.mbeans;

import com.sun.appserv.util.cache.BoundedMultiLruCache;
import com.sun.appserv.util.cache.Constants;
/**
 * This class provides implementation for JmxLruCache MBean
 *
 * @author Krishnamohan Meduri (Krishna.Meduri@Sun.com)
 *
 */
public class JmxBoundedMultiLruCache extends JmxMultiLruCache 
                              implements JmxBoundedMultiLruCacheMBean {

    private BoundedMultiLruCache boundedMultiLruCache;

    public JmxBoundedMultiLruCache(BoundedMultiLruCache boundedMultiLruCache, 
                                   String name) {
        super(boundedMultiLruCache, name);
        this.boundedMultiLruCache = boundedMultiLruCache;
    }

    /**
     * Returns the current size of the cache in bytes
     */
    public Long getCurrentSize() {
        return (Long) boundedMultiLruCache.getStatByName(
                                        Constants.STAT_BOUNDEDMULTILRUCACHE_CURRENT_SIZE);
    }

    /**
     * Returns the upper bound on the cache size
     */
    public Long getMaxSize() {
        Object object = boundedMultiLruCache.getStatByName(
                                        Constants.STAT_BOUNDEDMULTILRUCACHE_MAX_SIZE);
        /*
         * BoundedMultiLruCache class returns java.lang.String with a value 
         * "default" if the maxSize == Constants.DEFAULT_MAX_CACHE_SIZE
         * To take care of this case, the if/else is added below
         */
        if (object instanceof String &&
            ((String) object).equals(Constants.STAT_DEFAULT)) {
            return Long.valueOf(Constants.DEFAULT_MAX_CACHE_SIZE);
        }
        else {
            return (Long) object;
        }
    }
}
