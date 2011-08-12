/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2011 Oracle and/or its affiliates. All rights reserved.
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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.admin.amx.impl.config;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.management.ObjectName;
import org.glassfish.external.arc.Stability;
import org.glassfish.external.arc.Taxonomy;
import org.jvnet.hk2.config.ConfigBean;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Dom;

/**
 * Registry mapping ConfigBean to ObjectName and vice-versa.
 */
@Taxonomy( stability=Stability.NOT_AN_INTERFACE )
public final class ConfigBeanRegistry {
    private static void debug( final String s ) { System.out.println(s); }
    
    public static final class MBeanInstance
    {
        public final ConfigBean mConfigBean;
        public final ObjectName mObjectName;
        public final Object     mImpl;
        public MBeanInstance( final ConfigBean cb, final ObjectName on, final Object impl )
        {
            mConfigBean = cb;
            mObjectName = on;
            mImpl = impl;
        }
    }
    
    private final ConcurrentMap<ConfigBean,MBeanInstance> mFromConfigBean;
    private final ConcurrentMap<ObjectName, MBeanInstance> mFromObjectName;
    
    private ConfigBeanRegistry() {
        mFromConfigBean = new ConcurrentHashMap<ConfigBean,MBeanInstance>();
        mFromObjectName = new ConcurrentHashMap<ObjectName, MBeanInstance>();
    }
    
    private static final ConfigBeanRegistry INSTANCE = new ConfigBeanRegistry();
    public static ConfigBeanRegistry getInstance() {
        return INSTANCE;
    }

    private MBeanInstance getMBeanInstance(final ObjectName objectName)
    {
        return mFromObjectName.get(objectName);
    }

    private MBeanInstance getMBeanInstance(final ConfigBean cb)
    {
        return mFromConfigBean.get(cb);
    }

    public synchronized void  add(final ConfigBean cb, final ObjectName objectName, final Object impl)
    {
        final MBeanInstance mb = new MBeanInstance(cb, objectName, impl);
        mFromConfigBean.put(cb, mb );
        mFromObjectName.put(objectName, mb);
        //debug( "ConfigBeanRegistry.add(): " + objectName );
    }

    public synchronized void  remove(final ObjectName objectName)
    {
        final MBeanInstance mb = mFromObjectName.get(objectName);
        if ( mb != null )
        {
            mFromObjectName.remove(objectName);
            mFromConfigBean.remove(mb.mConfigBean);
        }
        //debug( "ConfigBeanRegistry.remove(): " + objectName );

    }

    public ConfigBean getConfigBean(final ObjectName objectName)
    {
        final MBeanInstance mb = getMBeanInstance(objectName);
        return mb == null ? null: mb.mConfigBean;
    }
    
    public ObjectName getObjectName(final ConfigBean cb)
    {
        final MBeanInstance mb = getMBeanInstance(cb);
        return mb == null ? null: mb.mObjectName;
    }
    
    public Object getImpl(final ObjectName objectName)
    {
        final MBeanInstance mb = getMBeanInstance(objectName);
        return mb == null ? null: mb.mImpl;
    }
    
    public Object getImpl(final ConfigBean cb)
    {
        final MBeanInstance mb = getMBeanInstance(cb);
        return cb == null ? null: mb.mImpl;
    }

    public ObjectName getObjectNameForProxy(final ConfigBeanProxy cbp)
    {
        final Dom dom = Dom.unwrap(cbp);
        
        if ( dom instanceof ConfigBean )
        {
            return getObjectName( (ConfigBean)dom );
        }
        
        // not a config bean so return null
        return null;
    }
    

}




