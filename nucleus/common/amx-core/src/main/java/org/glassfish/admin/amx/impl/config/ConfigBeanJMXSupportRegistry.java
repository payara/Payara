/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.amx.impl.config;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.glassfish.external.arc.Stability;
import org.glassfish.external.arc.Taxonomy;
import org.jvnet.hk2.config.ConfigBean;
import org.jvnet.hk2.config.ConfigBeanProxy;


/**
    A registry of ConfigBeanJMXSupport, for efficiency in execution time and scalability
    for large numbers of MBeans which share the same underlying type of @Configured.
 */
@Taxonomy( stability=Stability.NOT_AN_INTERFACE )
final class ConfigBeanJMXSupportRegistry
{
    private ConfigBeanJMXSupportRegistry() {}
    
    /**
        Map an interface to its helper.
     */
    private static final ConcurrentMap<Class<? extends ConfigBeanProxy>,ConfigBeanJMXSupport>  INSTANCES =
        new ConcurrentHashMap<Class<? extends ConfigBeanProxy>,ConfigBeanJMXSupport>();
    
    /**
     */
        public static ConfigBeanJMXSupport
    getInstance( final Class<? extends ConfigBeanProxy> intf )
    {
        if ( intf == null )
        {
            throw new IllegalArgumentException("null ConfigBeanProxy interface passed in" );
        }
        
        ConfigBeanJMXSupport helper = INSTANCES.get(intf);
        if ( helper == null )
        {
            // don't cache it, we can't be sure about its key
            helper = new ConfigBeanJMXSupport(intf, null);
        }
        return helper;
    }
    
    
    public static synchronized  List<Class<? extends ConfigBeanProxy>> getConfiguredClasses()
    {
        return new ArrayList<Class<? extends ConfigBeanProxy>>( INSTANCES.keySet() );
    }
    
        public static ConfigBeanJMXSupport
    getInstance( final ConfigBean configBean )
    {
        ConfigBeanJMXSupport helper = INSTANCES.get( configBean.getProxyType() );
        if ( helper == null )
        {
            helper = addInstance(configBean);
        }
        return helper;
    }
    
        private static synchronized ConfigBeanJMXSupport
    addInstance( final ConfigBean configBean )
    {
        final Class<? extends ConfigBeanProxy> intf = configBean.getProxyType();
        ConfigBeanJMXSupport helper = INSTANCES.get(intf);
        if ( helper == null )
        {
            helper = new ConfigBeanJMXSupport(configBean);
            INSTANCES.put( intf, helper );
        }
        return helper;
    }
    
    /** Find all  ConfigBeanProxy interfaces  reachable from specified item, including the item itself */
        public static Set<Class<? extends ConfigBeanProxy>>
    getAllConfigBeanProxyInterfaces( final ConfigBeanJMXSupport top) {
        final Set<Class<? extends ConfigBeanProxy>> all = new HashSet<Class<? extends ConfigBeanProxy>>();
        all.add( top.getIntf() );

        for( final Class<? extends ConfigBeanProxy>  intf : top.childTypes().values() )
        {
            all.addAll( getAllConfigBeanProxyInterfaces(getInstance(intf)) );
        }
        return all;
    }


    /** Recursively attempt to find default values for a descendant of specified type */
        public static Class<? extends ConfigBeanProxy>
    getConfigBeanProxyClassFor( final ConfigBeanJMXSupport start, final String type) {
        Class<? extends ConfigBeanProxy> result = start.childTypes().get(type);
        if ( result == null )
        {
            for( final String sub : start.childTypes().keySet() )
            {
                final Class<? extends ConfigBeanProxy> intf = start.childTypes().get(sub);
                final ConfigBeanJMXSupport spt = getInstance(intf);
                result = getConfigBeanProxyClassFor( spt, type );
                if ( result != null )
                {
                    break;
                }
            }
        }
        return result;
    }

 }



























