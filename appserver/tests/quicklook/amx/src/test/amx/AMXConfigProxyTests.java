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

import org.glassfish.admin.amx.intf.config.*;
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
    
    Resources getResources() { return getDomainConfig().getResources(); }

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
    
    
    @Test
    public void testAMXConfigDefaultValues()
    {
        for( final AMXConfigProxy amx : getAllConfig() )
        {
            _checkDefaultValues( amx );
        }
    }
    
    @Test
    public void testAMXConfigAttributeResolver()
    {
        for( final AMXConfigProxy amx : getAllConfig() )
        {
            _checkAttributeResolver( amx );
        }
    }
    
    
    @Test
    public void testConfigTools()
    {
        final ConfigTools ct = getDomainRootProxy().getExt().child(ConfigTools.class);
        
        final String[] namedTypes = ct.getConfigNamedTypes();
        assert namedTypes.length >= 8 :
            "Expecting at least 8 named types, got " + namedTypes.length + " = " + CollectionUtil.toString(SetUtil.newStringSet(namedTypes), ", ");
        
        final String[] resourceTypes = ct.getConfigResourceTypes();
        assert resourceTypes.length >= 10 :
            "Expecting at least 10 resource types, got " + resourceTypes.length + " = " + CollectionUtil.toString(SetUtil.newStringSet(resourceTypes), ", ");
                
        // create the properties List
        final List<Map<String,String>> props = new ArrayList<Map<String,String>>();
        final String NAME_PREFIX = "testConfigTools-";
        for( int i = 0 ; i < 10; ++i )
        {
            final Map<String,String>  m = new HashMap<String,String>();
            m.put( "Name", NAME_PREFIX + i );
            m.put( "Value", "value_" + i );
            m.put( "Description", "blah blah blah " + i );
            props.add( m );
        }
        
        // create a large number of properties, then remove them
        final Domain domainConfig = getDomainConfig();
        
        // remove first, in case they were left over from a failure.
        for( final Map<String,String> prop : props )
        {
            domainConfig.removeChild( "property", prop.get("Name") );
            domainConfig.removeChild( "system-property", prop.get("Name") );
        }

        // create them as properties and system properties
        ct.setProperties( domainConfig.objectName(), props, false );
        ct.setSystemProperties( domainConfig.objectName(), props, false );
        
        for( final Map<String,String> prop : props )
        {
            assert domainConfig.removeChild( "property", prop.get("Name") ) != null;
            assert domainConfig.removeChild( "system-property", prop.get("Name") ) != null;
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
    
    @Test
    public void testCreateProperty()
    {
        final Domain amx = getDomainConfig();
        
        final String PROP_NAME = "AMXConfigProxyTests.TEST_PROP1";
        final String propType = Util.deduceType(Property.class);
        // remove any existing test element
        if ( amx.childrenMap(Property.class).get(PROP_NAME) != null )
        {
            try
            {
                amx.removeChild( propType, PROP_NAME );
                System.out.println( "Removed stale test config " + PROP_NAME );
            }
            catch( final Exception e )
            {
               assert false : "Unable to remove config " + PROP_NAME + ": " + e;
            }
        }
        
        final Map<String,Object> attrs = newPropertyMap(PROP_NAME);
        
        final AMXConfigProxy prop = amx.createChild( propType, attrs );
        assert prop.getName().equals(PROP_NAME);
        assert amx.childrenMap(Property.class).get(PROP_NAME) != null;
        
        amx.removeChild( propType, PROP_NAME );
        assert amx.childrenMap(Property.class).get(PROP_NAME) == null;
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
    
    @Test
    /**
        Verify that a resource with properties can be created and removed.
     */
    public void testCreateResource()
    {
        final Resources parent = getDomainConfig().getResources();
        
        final String name = "AMXConfigProxyTests.test-resource";
        final String type = Util.deduceType(CustomResource.class);
        removeChildSilently( parent, type, name );
        
        final Map<String,Object> attrs = MapUtil.newMap();
        attrs.put( "Name", name );  // IMPORTANT: this verifies that Name is mapped to jndi-name
        attrs.put( "ResType", "java.lang.Properties" );
        attrs.put( "ObjectType", "user" );
        attrs.put( "Enabled", "false" );
        attrs.put( "Description", "test" );
        attrs.put( "FactoryClass", "com.foo.bar.FooFactory" );
        
        // include two property children in the new resource
        final Map[] propMaps = new Map[2];
        String prop1 = "prop1"; String prop2 = "prop2";
        propMaps[0] = newPropertyMap(prop1);
        propMaps[1] = newPropertyMap(prop2);
        attrs.put( Util.deduceType(Property.class), propMaps);
                
        AMXConfigProxy child = parent.createChild( type, attrs );
        assert child.getName().equals(name);
        assert parent.childrenMap(type).get(name) != null;
        parent.removeChild( type, name );
        assert parent.childrenMap(type).get(name) == null;
        
        // do it again, but this time we'll specify the name as its ral key value instead of "Name"
        attrs.remove("Name");
        attrs.put( "JndiName", name);
        child = parent.createChild( type, attrs );
        assert child.getName().equals(name);
        assert parent.childrenMap(type).get(name) != null;
        parent.removeChild( type, name );
        assert parent.childrenMap(type).get(name) == null;
    }
    
    @Test
    public void testAmxPref()
    {
        final Domain domain = getDomainConfig();
        
        if ( domain.getAmxPref() == null )
        {
            domain.createChild( Util.deduceType(AmxPref.class), null );
        }
        final AmxPref prefs = domain.getAmxPref();
        assert prefs != null;
        final String validationLevel = prefs.getValidationLevel();
        assert validationLevel.equalsIgnoreCase("full") || validationLevel.equalsIgnoreCase("off");
        
        assert prefs.getUnregisterNonCompliant() != null;
    }
    
    @Test
    public void testCreateProperties()
    {
        final Domain parent = getDomainConfig();
        
        // create 100 <property> children
        // this is in part ot ensure that the AMXValidator doens't barf when objects register/unregister
        final int NUM_PROPS = 20;
        final Map<String,Object>[] propMaps = newPropertyMaps( "AMXConfigProxyTests.testCreateProperties-", NUM_PROPS);
        
        final Set<String> propNames = new HashSet<String>();
        for( int i = 0; i < propMaps.length; ++i )
        {
            propNames.add( (String)propMaps[i].get("Name") );
        }
        
        final String propType = Util.deduceType(Property.class);
        // first remove any existing test elements
        final Map<String,Property> existing = parent.childrenMap(Property.class);
        for( final String propName : propNames )
        {
            if ( existing.get(propName) != null )
            {
                try
                {
                    parent.removeChild( propType, propName );
                    System.out.println( "Removed stale test config " + propName );
                }
                catch( final Exception e )
                {
                   assert false : "Unable to remove config " + propName + ": " + e;
                }
            }
        }
        
        final Map<String,Map<String,Object>[]>  childrenMaps = MapUtil.newMap();
        childrenMaps.put( propType, propMaps );
        final Map<String,Object> parentAttrs = MapUtil.newMap(); // FIXME  ad some
        parentAttrs.put( "Locale", "EN_US");
        parentAttrs.put( "LogRoot",  parent.getLogRoot() );
        
        final AMXConfigProxy[] newChildren = parent.createChildren( childrenMaps, parentAttrs);
        final int numExpected = propMaps.length;
        assert newChildren.length == numExpected : "Expected " + numExpected + ", got " + newChildren.length;
        final Map<String,Property>  childrenProps = parent.childrenMap(Property.class);
        for( final String propName : propNames )
        {
            assert childrenProps.get(propName) != null : "property not created: " + propName;
        }
        
        for( final String propName : propNames )
        {
            parent.removeChild( propType, propName );
        }
        // verify that they're all gone
        final Map<String,Property>  remaining = parent.childrenMap(Property.class);
        for( final String propName : propNames )
        {
            parent.removeChild( propType, propName );
            assert remaining.get(propName) == null;
        }
    }
    
    
    
    /**
        Tests creating a whole config hiearchy
     */
    @Test
    public void createChildTest()
    {
        final Configs configs = getDomainConfig().getConfigs();
        
        final String configName = "AMXConfigProxyTests.TEST";
        final String type = Util.deduceType(Config.class);
        removeChildSilently( configs, type, configName );
        
        final Map<String,Object>  configParams = MapUtil.newMap();
        configParams.put( "Name", configName );
        configParams.put( Util.deduceType(Property.class), newPropertyMaps("prop", 5) );
        configParams.put("DynamicReconfigurationEnabled", false );
                
        final Config child = configs.createChild( type, configParams ).as(Config.class);
        assert child != null;
    }
    
       @Test
    public void connectorConnectionPoolTest()
        throws Exception
    {
        // create a new ConnectorConnectionPool with a SecurityMap containing a BackendPrincipal
        final Map<String,Object> params = new HashMap<String,Object>();
        
        final String NAME = "AMXConfigProxyTests.connectorConnectionPoolTest";
        params.put( "Name", NAME );
        params.put( "ResourceAdapterName", NAME );
        params.put( "ConnectionDefinitionName", NAME );
        params.put( "SteadyPoolSize", 23 ); // check that it works
        
        final Map<String,Object> securityParams = new HashMap<String,Object>();
        securityParams.put( "Name", NAME );
        params.put( Util.deduceType(SecurityMap.class), securityParams );
        
        final Map<String,Object> backendParams = new HashMap<String,Object>();
        backendParams.put( "UserName", "testUser" );
        backendParams.put( "Password", "testPassword" );
        securityParams.put( Util.deduceType(BackendPrincipal.class), backendParams );

        final String type = Util.deduceType(ConnectorConnectionPool.class);
        removeChildSilently( getResources(), type, NAME );
        final AMXConfigProxy result = getResources().createChild( type, params);
        assert result != null;
        assert result.type().equals(type);
        assert getResources().childrenMap(ConnectorConnectionPool.class).get(NAME) != null;
        
        final ObjectName objectName = getResources().removeChild( type, NAME );
        assert objectName != null;
        assert ! getMBeanServerConnection(). isRegistered(objectName);
    }
    
    private Config getServerConfig()
    {
        return getDomainConfig().getConfigs().getConfig().get("server-config");
    }
    
    @Test
    public void createProfilerTest()
    {
        final JavaConfig javaConfig = getServerConfig().getJavaConfig();

        final String profilerName = "AMXConfigProxyTests.TEST";
        final String type = Util.deduceType(Profiler.class);
        removeChildSilently( javaConfig, type, null);
        assert javaConfig.child(type)  == null : "Failed to remove profiler!";
        
        final Map<String,Object>  params = MapUtil.newMap();
        params.put( "Name", profilerName );
        params.put( "Classpath", "/foo/bar" );
        params.put( "NativeLibraryPath", "/foo/bar" );
        params.put( "Enabled", false );
        params.put( "JvmOptions", new String[] { "-Dfoo=bar" } );
                
        final AMXProxy child = javaConfig.createChild( type, params );
        assert child != null : "Can't create profiler, got null back!";
        final Profiler profiler = child.as(Profiler.class);
        
        final ObjectName removed = javaConfig.removeChild(type);
        assert javaConfig.child(type)  == null : "Failed to remove profiler, child still exists";
    }
    
    
    /** 
    
    // verify that the two properties with the same name cannot be created This is a test of HK2, not AMX
    
    @Test
    public void duplicatePropertyTest()
    {
        final Domain amx = getDomainConfig();
        
        final String PROP_NAME = "AMXConfigProxyTests.TEST_PROP1";
        final String propType = Util.deduceType(Property.class);
        // remove any existing test element
        if ( amx.childrenMap(Property.class).get(PROP_NAME) != null )
        {
            try
            {
                amx.removeChild( propType, PROP_NAME );
                System.out.println( "Removed stale test config " + PROP_NAME );
            }
            catch( final Exception e )
            {
               assert false : "Unable to remove property " + PROP_NAME + ": " + e;
            }
        }
        
        final Map<String,Object> attrs = newPropertyMap(PROP_NAME);
        
        final AMXConfigProxy prop = amx.createChild( propType, attrs );
        assert prop.getName().equals(PROP_NAME);
        assert amx.childrenMap(Property.class).get(PROP_NAME) != null;
        
        try
        {
            final AMXConfigProxy prop2 = amx.createChild( propType, attrs );
            // should have thrown an exception; it's a duplicate
            //assert false : "More than one property with the same name can be created, see issue #11272";
            println( "IGNORING FAILURE, see issue #11272" );
        }
        catch( final Exception e )
        {
            // good, it should not succeed
        }
        
        
        amx.removeChild( propType, PROP_NAME );
        assert amx.childrenMap(Property.class).get(PROP_NAME) == null;
    }
    
    
    // verify that the two virtual servers with the same name cannot be created This is a test of HK2, not AMX 
    @Test
    public void duplicateVirtualServerTest()
    {
        final HttpService amx = getServerConfig().getHttpService();

        final String VS_NAME = "AMXConfigProxyTests.TEST_VS";
        final String propType = Util.deduceType(VirtualServer.class);
        // remove any existing test element
        if ( amx.childrenMap(Property.class).get(VS_NAME) != null )
        {
            try
            {
                amx.removeChild( propType, VS_NAME );
                System.out.println( "Removed stale element " + VS_NAME );
            }
            catch( final Exception e )
            {
               assert false : "Unable to remove stale element " + VS_NAME + ": " + e;
            }
        }
        
        final Map<String,Object> attrs = MapUtil.newMap();
        attrs.put( "Name", VS_NAME );
        attrs.put( "docroot", "/foobar" );
        attrs.put( "hosts", "localhost" );
        
        final AMXConfigProxy prop = amx.createChild( propType, attrs );
        assert prop.getName().equals(VS_NAME);
        assert amx.childrenMap(VirtualServer.class).get(VS_NAME) != null;
        
        try
        {
            final AMXConfigProxy prop2 = amx.createChild( propType, attrs );
            // should have thrown an exception; it's a duplicate
            //assert false : "More than one virtual server with the same name can be created, see issue #11272";
            println( "IGNORING FAILURE, see issue #11272" );
        }
        catch( final Exception e )
        {
            // good, it should not succeed
        }
        
        amx.removeChild( propType, VS_NAME );
        assert amx.childrenMap(VirtualServer.class).get(VS_NAME) == null;
    }

*/
}




































