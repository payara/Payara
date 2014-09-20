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

import com.sun.appserv.util.cache.BaseCache;
import com.sun.appserv.util.cache.Cache;
import com.sun.appserv.util.cache.CacheListener;
import com.sun.appserv.util.cache.Constants;

import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 * An EJB(Local)Object cache that does not impose any limit on the 
 * number of entries
 *
 * @author Mahesh Kannan
 */
public class UnboundedEJBObjectCache
    extends BaseCache
    implements EJBObjectCache
{
    /**
     * default constructor
     */
    public UnboundedEJBObjectCache(String name) { super(); }
    
    /**
     * constructor with specified timeout
     */
    public UnboundedEJBObjectCache(String name, long timeout) {
        super();
    }
    
    public void init(int maxEntries, int numberOfVictimsToSelect,
            long timeout, float loadFactor, Properties props)
    {
        super.init(maxEntries, loadFactor, props);
    }
    
    public Object get(Object key, boolean incrementRefCount) {
        return super.get(key);
    }
    
    public Object put(Object key, Object value, boolean linkWithLru) {
        return super.put(key, value);
    }
    
    public Object remove(Object key, boolean decrementRefCount) {
        return super.remove(key);
    }
    
    public void setEJBObjectCacheListener(EJBObjectCacheListener listener) {
        //do nothing
    }
    
    protected void trimItem(CacheItem item) {
        
    }
    
    public Map getStats() {
        Map map = new HashMap();
        StringBuffer sbuf = new StringBuffer();
        sbuf.append("(listSize = 0")
        .append("; cacheSize = ").append(getEntryCount())
        .append(")");
        map.put("_UnBoundedEJBObject ==> ", sbuf.toString());
        return map;
    }
    
}
