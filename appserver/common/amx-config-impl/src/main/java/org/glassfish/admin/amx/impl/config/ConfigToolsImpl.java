/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.config.serverbeans.Resource;
import com.sun.enterprise.config.serverbeans.SystemProperty;
import com.sun.enterprise.config.serverbeans.SystemPropertyBag;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import javax.management.ObjectName;

import org.glassfish.admin.amx.core.Util;
import org.glassfish.admin.amx.impl.mbean.AMXImplBase;
import org.glassfish.admin.amx.intf.config.ConfigTools;
import org.glassfish.api.admin.config.Named;
import org.jvnet.hk2.config.types.Property;
import org.jvnet.hk2.config.types.PropertyBag;
import org.jvnet.hk2.config.ConfigBean;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigCode;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.WriteableView;
import org.jvnet.hk2.config.Transaction;

import java.lang.reflect.Proxy;

/**
 * TODO: fix the duplication of code for Property/SystemProperty by refactoring the config bean interfaces.
 */
public class ConfigToolsImpl extends AMXImplBase
{
    private static void debug(final String s)
    {
        System.out.println(s);
    }

    public ConfigToolsImpl(final ObjectName parent)
    {
        super(parent, ConfigTools.class);
    }

    private static Property findProperty(final List<Property> props, final String name)
    {
        for (final Property prop : props)
        {
            if (prop == null)
            {
                debug("WARNING: null Property object in List<Property>");
                continue;
            }
            if (prop.getName().equals(name))
            {
                return prop;
            }
        }
        return null;
    }

    private static SystemProperty findSystemProperty(final List<SystemProperty> props, final String name)
    {
        for (final SystemProperty prop : props)
        {
            if (prop.getName().equals(name))
            {
                return prop;
            }
        }
        return null;
    }

    /** base class for Property or SystemProperty support */
    private static abstract class AnyPropsSetter implements ConfigCode
    {
        protected final List<Map<String, String>> mNewProps;

        protected final boolean mClearAll;

        AnyPropsSetter(List<Map<String, String>> newProps, final boolean clearAll)
        {
            mNewProps = newProps;
            mClearAll = clearAll;
        }

        public Object run(ConfigBeanProxy... params)
                throws PropertyVetoException, TransactionFailure
        {
            if (params.length != 1)
            {
                throw new IllegalArgumentException();
            }
            final ConfigBeanProxy parent = params[0];

            final ConfigBean source = (ConfigBean) ConfigBean.unwrap(parent);
            final ConfigSupport configSupport = source.getHabitat().getComponent(ConfigSupport.class);

            return _run(parent, configSupport);
        }
        
        protected Set<String> propNames()
        {
            final Set<String>   names = new HashSet<String>();
            for (final Map<String, String> newProp : mNewProps)
            {
                names.add( newProp.get("Name") );
            }
            return names;
        }

        abstract Object _run(final ConfigBeanProxy parent, final ConfigSupport spt) throws PropertyVetoException, TransactionFailure;

    }

    private static final class PropsSetter extends AnyPropsSetter
    {
        public PropsSetter(List<Map<String, String>> newProps, final boolean clearAll)
        {
            super(newProps, clearAll);
        }

        public Object _run(final ConfigBeanProxy parent, final ConfigSupport configSupport)
                throws PropertyVetoException, TransactionFailure
        {
            final PropertyBag bag = (PropertyBag) parent;
            final List<Property> props = bag.getProperty();
            
            if ( mClearAll)
            {
                // remove all item that aren't in the new ones
                final Set<String>   newPropNames = propNames();
                final List<Property> toRemove = new ArrayList<Property>();
                for( final Property existing : props )
                {
                    if ( ! newPropNames.contains(existing.getName()) )
                    {
                       toRemove.add(existing);
                    }
                }
                props.removeAll( toRemove );
            }

            final WriteableView parentW = (WriteableView)Proxy.getInvocationHandler(parent);
            final Transaction t = parentW.getTransaction();
                    
            for (final Map<String, String> newProp : mNewProps)
            {
                final String name = newProp.get("Name");
                final String value = newProp.get("Value");
                final String description = newProp.get("Description");

                Property prop = findProperty(props, name);
                if (prop != null)
                {
                    final Property propW = configSupport.getWriteableView(prop);
                    ((WriteableView)Proxy.getInvocationHandler(propW)).join( t );
                    prop = propW;
                    //debug("Updated system-property: " + prop.getName());
                }
                else
                {
                    prop = parent.createChild(Property.class);
                    props.add(prop);
                    //debug("Create new system-property: " + prop.getName());
                }
                
                prop.setName(name);
                prop.setValue(value);
                prop.setDescription(description);
            }
            return null;
        }
    }

    
    private static final class SystemPropsSetter extends AnyPropsSetter
    {
        public SystemPropsSetter(List<Map<String, String>> newProps, final boolean clearAll)
        {
            super(newProps, clearAll);
        }

        public Object _run(final ConfigBeanProxy parent, final ConfigSupport configSupport)
                throws PropertyVetoException, TransactionFailure
        {
            final SystemPropertyBag bag = (SystemPropertyBag) parent;
            final List<SystemProperty> props = bag.getSystemProperty();
            
            // it's important to *modify* existing items, not remove then and re-add them
            if ( mClearAll)
            {
                // remove all item that aren't in the new ones
                final Set<String>   newPropNames = propNames();
                final List<SystemProperty> toRemove = new ArrayList<SystemProperty>();
                for( final SystemProperty existing : props )
                {
                    if ( ! newPropNames.contains(existing.getName()) )
                    {
                       toRemove.add(existing);
                    }
                }
                props.removeAll( toRemove );
            }

            final WriteableView parentW = (WriteableView)Proxy.getInvocationHandler(parent);
            final Transaction t = parentW.getTransaction();
                    
            for (final Map<String, String> newProp : mNewProps)
            {
                final String name = newProp.get("Name");
                final String value = newProp.get("Value");
                final String description = newProp.get("Description");

                SystemProperty prop = findSystemProperty(props, name);
                if (prop != null)
                {
                    final SystemProperty propW = configSupport.getWriteableView(prop);
                    ((WriteableView)Proxy.getInvocationHandler(propW)).join( t );
                    prop = propW;
                    //debug("Updated system-property: " + prop.getName());
                }
                else
                {
                    prop = parent.createChild(SystemProperty.class);
                    props.add(prop);
                    //debug("Create new system-property: " + prop.getName());
                }
                
                prop.setName(name);
                prop.setValue(value);
                prop.setDescription(description);
            }
            return null;
        }
    }

    /*
    public void testSetProps() {
    final List<Map<String,String>> props = new ArrayList<Map<String,String>>();

    final ConfigTools tools = getDomainRootProxy().getExt().child(ConfigTools.class);

    props.add( MapUtil.newMap("Name", "test1", "Value", "value1", "Description", "desc1") );
    props.add( MapUtil.newMap("Name", "test2", "Value", "value2", "Description", "desc2") );
    props.add( MapUtil.newMap("Name", "test3", "Value", "value3", "Description", "desc3") );

    //final ObjectName target = getDomainRootProxy().getDomain().objectName();
    final ObjectName target = JMXUtil.newObjectName( "v3:pp=/domain/configs/config[server-config],type=web-container" );

    setProperties( target, props, false );
    setSystemProperties( target, props, false );
    }
     */
    public Object test()
    {
    /*
        final Domain amx = getDomainConfig();
        
        // create 5 <property> children
        final Map<String,Object>[] propMaps = newPropMaps( "AMXConfigProxyTests.testCreateProperties-", 5);
        
        final Set<String> propNames = new HashSet<String>();
        for( int i = 0; i < propMaps.length; ++i )
        {
            propNames.add( (String)propMaps[i].get("Name") );
        }
        
        // first remove any existing test elements
        for( final String propName : propNames )
        {
            if ( amx.childrenMap(Property.class).get(propName) != null )
            {
                try
                {
                    amx.removeChild( Util.deduceType(Property.class), propName );
                    System.out.println( "Removed stale test config " + propName );
                }
                catch( final Exception e )
                {
                   assert false : "Unable to remove config " + propName + ": " + e;
                }
            }
        }
        
        final Map<String,Map<String,Object>[]>  childrenMaps = MapUtil.newMap();
        childrenMaps.put( Util.deduceType(Property.class), propMaps );
        final AMXConfigProxy[] newChildren = amx.createChildren( childrenMaps );
        for( final String propName : propNames )
        {
            assert amx.childrenMap(Property.class).get(propName) != null : "property not created: " + propName;
        }
        
        for( final String propName : propNames )
        {
            amx.removeChild( Util.deduceType(Property.class), propName );
            assert amx.childrenMap(Property.class).get(propName) == null;
        }
        */
        return null;
    }

    // dummy interface for creating a proxy
    interface PropertyBagProxy extends ConfigBeanProxy, PropertyBag
    {
    }

    public void setProperties(
            final ObjectName parent,
            final List<Map<String, String>> props,
            final boolean clearAll)
    {
        if (parent == null || props == null)
        {
            throw new IllegalArgumentException();
        }

        final ConfigBean configBean = ConfigBeanRegistry.getInstance().getConfigBean(parent);
        if (configBean == null)
        {
            throw new IllegalArgumentException("" + parent);
        }

        final PropertyBagProxy proxy = configBean.getProxy(PropertyBagProxy.class);
        if (!PropertyBag.class.isAssignableFrom(proxy.getClass()))
        {
            throw new IllegalArgumentException("ConfigBean " + configBean.getProxyType().getName() + " is not a PropertyBag");
        }

        final PropsSetter propsSetter = new PropsSetter(props, clearAll);
        try
        {
            ConfigSupport.apply(propsSetter, proxy);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public void clearProperties(final ObjectName parent)
    {
        setProperties(parent, new ArrayList<Map<String, String>>(), true);
    }

    // dummy interface for creating a proxy
    interface SystemPropertyBagProxy extends ConfigBeanProxy, SystemPropertyBag
    {
    }

    public void clearSystemProperties(final ObjectName parent)
    {
        setSystemProperties(parent, new ArrayList<Map<String, String>>(), true);
    }

    public void setSystemProperties(
            final ObjectName parent,
            final List<Map<String, String>> props,
            final boolean clearAll)
    {
        if (parent == null || props == null)
        {
            throw new IllegalArgumentException();
        }

        final ConfigBean configBean = ConfigBeanRegistry.getInstance().getConfigBean(parent);
        if (configBean == null)
        {
            throw new IllegalArgumentException("" + parent);
        }

        final SystemPropertyBagProxy proxy = configBean.getProxy(SystemPropertyBagProxy.class);
        if (!SystemPropertyBag.class.isAssignableFrom(proxy.getClass()))
        {
            throw new IllegalArgumentException("ConfigBean " + configBean.getProxyType().getName() + " is not a SystemPropertyBag");
        }

        final SystemPropsSetter propsSetter = new SystemPropsSetter(props, clearAll);
        try
        {
            ConfigSupport.apply(propsSetter, proxy);
        }
        catch (final Exception e)
        {
            throw new RuntimeException(e);
        }
    }
    
    private String[] toString( final Class[] classes )
    {
        if ( classes == null ) return null;
        
        final String[] names = new String[ classes.length ];
        for( int i = 0; i < classes.length; ++i )
        {
            names[i] = Util.typeFromName( classes[i].getName() );
        }
        return names;
    }
    
    public String[] getConfigNamedTypes()
    {
        return toString( ConfigBeanJMXSupport.getTypesImplementing(Named.class) );
    }

    public String[] getConfigResourceTypes()
    {
        return toString( ConfigBeanJMXSupport.getTypesImplementing(Resource.class) );
    }
    

    /*
    public String[] getConfigTypes()
    {
        final DomDocument domDoc = new DomDocument(InjectedValues.getInstance().getHabitat());
        final ConfigModel model = domDoc.getModelByElementName("domain");
        if ( model == null )
        {
            throw new IllegalArgumentException( "Can't get model for domain" );
        }
        
        final Set<String> names = model.getElementNames();
        final Set<String> all = new HashSet<String>();
        all.addAll( names );
        // need recursion here, but is it useful?
        for( final String name : names )
        {
            final ConfigModel model2 = domDoc.getModelByElementName(name);
            all.addAll( model2.getElementNames() );
        }
        
        final String[] result = new String[ all.size() ];
        all.toArray( result );
        
        return result;
    }
    */
}















