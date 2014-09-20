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

package amxtest;

import org.testng.annotations.*;
import org.testng.Assert;

import javax.management.ObjectName;
import javax.management.AttributeList;
import javax.management.Attribute;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

import org.glassfish.admin.amx.core.*;
import org.glassfish.admin.amx.base.*;
import org.glassfish.admin.amx.config.*;
//import org.glassfish.admin.amx.j2ee.*;
import org.glassfish.admin.amx.monitoring.*;
import org.glassfish.admin.amx.util.CollectionUtil;
import org.glassfish.admin.amx.util.ExceptionUtil;
import org.glassfish.admin.amx.util.SetUtil;
import org.glassfish.admin.amx.util.ListUtil;
import org.glassfish.admin.amx.util.MapUtil;
import org.glassfish.admin.amx.util.StringUtil;
import org.glassfish.admin.amx.util.TypeCast;
import org.glassfish.admin.amx.logging.Logging;
import org.glassfish.admin.amx.annotation.*;


/** 
    Miscellaneous tests should go into this file, or another one like it.
 */
@Test(
    sequential=false, threadPoolSize=16,
    groups =
    {
        "amx"
    },
    description = "tests for AMXConfigProxy"
)
public final class AMXConfigProxyTests extends AMXTestBase
{
    public AMXConfigProxyTests()
    {
    }
    
     /** test all MBeans generically */
    @Test
    public void testForBogusConfigAnnotations()
    {
        final List<Class<? extends AMXProxy>> interfaces = getInterfaces().all();
        
        // AMXConfigProxy sub-interfaces should not use @ManagedAttribute or @ManagedOperation;
        // all such info is derived only from the ConfigBean.
        for( final Class<? extends AMXProxy>  intf : interfaces )
        {
            if ( ! AMXConfigProxy.class.isAssignableFrom(intf) ) continue;
            
            final Method[] methods = intf.getDeclaredMethods(); // declared methods only
            for( final Method m : methods )
            {
                final ManagedAttribute ma = m.getAnnotation(ManagedAttribute.class);
                final ManagedOperation mo = m.getAnnotation(ManagedOperation.class);
                final String desc = intf.getName() + "." + m.getName() + "()";
                
                assert ma == null :  "Config MBeans do not support @ManagedAttribute: " + desc;
                assert mo == null :  "Config MBeans do not support @ManagedOperation: " + desc;
            }
        }
    }
    
    
    private void _checkDefaultValues(final AMXConfigProxy amxConfig)
    {
        final String objectName = amxConfig.objectName().toString();

        // test the Map keyed by XML attribute name
        final Map<String, String> defaultValuesXML = amxConfig.getDefaultValues(false);
        for (final String attrName : defaultValuesXML.keySet())
        {
            // no default value should ever be null

            assert defaultValuesXML.get(attrName) != null :
            "null value for attribute " + attrName + " in " + objectName;
        }

        // test the Map keyed by AMXProxy attribute name
        final Map<String, String> defaultValuesAMX = amxConfig.getDefaultValues(true);

        assert defaultValuesXML.size() == defaultValuesAMX.size();
        for (final String attrName : defaultValuesAMX.keySet())
        {
            // no default value should ever be null

            assert defaultValuesAMX.get(attrName) != null :
            "null value for attribute " + attrName + " in " + objectName;
        }
    }

    private void _checkAttributeResolver(final AMXConfigProxy amxConfig)
    {
        final Set<String> attrNames = amxConfig.attributeNames();
        for (final String attrName : attrNames)
        {
            final String resolvedValue = amxConfig.resolveAttribute(attrName);
            if (resolvedValue != null)
            {
                // crude check
                assert resolvedValue.indexOf("${") < 0 :
                "Attribute " + attrName + " did not resolve: " + resolvedValue;
            }
        }

        final AttributeList attrsList = amxConfig.resolveAttributes( SetUtil.toStringArray(attrNames) );
        for (final Object o : attrsList)
        {
            final Attribute a = (Attribute) o;
            final String resolvedValue = "" + a.getValue();
            if (resolvedValue != null)
            {
                // crude check
                assert resolvedValue.indexOf("${") < 0 :
                "Attribute " + a.getName() + " did not resolve: " + resolvedValue;
            }
        }
    }
    
    
    private Map<String,Object> newPropertyMap(final String name)
    {
        final Map<String,Object>    m = MapUtil.newMap();
        
        m.put( "Name", name );
        m.put( "Value", name + "-value" );
        m.put( "Description", "desc.for." + name );
        
        return m;
    }
    
    private Map<String,Object>[] newPropertyMaps(final String baseName, final int count)
    {
        final Map<String,Object>[] maps = TypeCast.asArray( new Map[count] );
        for( int i = 0; i < count; ++i )
        {
            maps[i] = newPropertyMap(baseName + i);
        }
        return maps;
    }
    

    private void removeChildSilently( final AMXConfigProxy amx, final String type, final String name )
    {   
        if ( name == null )
        {
            if ( amx.child(type) != null )
            {
                try
                {
                    final ObjectName removed = amx.removeChild( type );
                    assert removed == null : "failed (null for ObjectName) to remove child of type \"" + type + "\" from " + amx.objectName();
                    assert amx.child(type) == null : "failed to remove child of type \"" + type + "\" from " + amx.objectName();
                    System.out.println( "Removed stale test config of type " + type );
                }
                catch( final Exception e )
                {
                    e.printStackTrace();
                   assert false : "Unable to remove config of type " + type + ": " + e;
                }
            }
        }
        else if ( amx.childrenMap(type).get(name) != null )
        {
            try
            {
                amx.removeChild( type, name );
                assert amx.childrenMap(type).get(name) == null : "failed to remove child " + type + "," + name + "  from " + amx.objectName();
                System.out.println( "Removed stale test config " + name );
            }
            catch( final Exception e )
            {
                e.printStackTrace();
               assert false : "Unable to remove config " + name + ": " + e;
            }
        }
    }
}




































