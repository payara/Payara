/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2015 Oracle and/or its affiliates. All rights reserved.
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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.*;
import javax.management.Attribute;
import static org.glassfish.admin.amx.config.AMXConfigConstants.*;
import org.glassfish.admin.amx.config.AMXConfigProxy;
import org.glassfish.admin.amx.config.AttributeResolver;
import org.glassfish.admin.amx.core.AMXProxy;
import org.glassfish.admin.amx.core.Util;
import org.glassfish.admin.amx.impl.mbean.AMXImplBase;
import org.glassfish.admin.amx.impl.util.*;
import org.glassfish.admin.amx.util.*;
import org.glassfish.admin.amx.util.jmx.JMXUtil;
import static org.glassfish.external.amx.AMX.*;
import org.glassfish.external.arc.Stability;
import org.glassfish.external.arc.Taxonomy;
import org.jvnet.hk2.config.*;

/**
Base class from which all AMX Config MBeans should derive (but not "must").
<p>
 */
@Taxonomy(stability = Stability.NOT_AN_INTERFACE)
public class AMXConfigImpl extends AMXImplBase
{
    private final ConfigBean mConfigBean;

    private static final Logger logger = AMXLoggerInfo.getLogger();

    /** MBeanInfo derived from the AMXConfigProxy interface, always the same */
    private static MBeanInfo configMBeanInfo;

    private static synchronized MBeanInfo getAMXConfigMBeanInfo()
    {
        if (configMBeanInfo == null)
        {
            configMBeanInfo = MBeanInfoSupport.getMBeanInfo(AMXConfigProxy.class);
        }
        return configMBeanInfo;
    }
    
    /**
     * We save time and space by creating exactly one MBeanInfo for any given config interface;
     * it can be shared among all instances since it is invariant.
     */
    private static final ConcurrentMap<Class<? extends ConfigBeanProxy>, MBeanInfo> mInfos =
            new ConcurrentHashMap<Class<? extends ConfigBeanProxy>, MBeanInfo>();

    private static MBeanInfo createMBeanInfo(final ConfigBean cb)
    {
        Class<? extends ConfigBeanProxy> intf = cb.getProxyType();
        MBeanInfo newInfo = mInfos.get(intf);
        if (newInfo != null)
        {
            return newInfo;
        }

        final ConfigBeanJMXSupport spt = ConfigBeanJMXSupportRegistry.getInstance(cb);
        final MBeanInfo info = spt.getMBeanInfo();

        final List<MBeanAttributeInfo> attrInfos = ListUtil.newListFromArray(info.getAttributes());
        final MBeanInfo spiInfo = MBeanInfoSupport.getAMX_SPIMBeanInfo();

        // make a list so we can remove "Children" attribute if this MBean cannot have any
        final List<MBeanAttributeInfo> spiAttrInfos = ListUtil.newListFromArray(spiInfo.getAttributes());
        if (spt.isLeaf())
        {
            JMXUtil.remove(spiAttrInfos, ATTR_CHILDREN);
        }

        // Add in the AMX_SPI attributes, replacing any with the same name
        for (final MBeanAttributeInfo attrInfo : spiAttrInfos)
        {
            // remove existing info
            final String attrName = attrInfo.getName();
            final MBeanAttributeInfo priorAttrInfo = JMXUtil.remove(attrInfos, attrName);

            // special case the Name attribute to preserve its metadata
            if (attrName.equals(ATTR_NAME) && priorAttrInfo != null)
            {
                final Descriptor mergedD = JMXUtil.mergeDescriptors(attrInfo.getDescriptor(), priorAttrInfo.getDescriptor());

                final MBeanAttributeInfo newAttrInfo = new MBeanAttributeInfo(attrName,
                        attrInfo.getType(), attrInfo.getDescription(), attrInfo.isReadable(), attrInfo.isWritable(), attrInfo.isIs(), mergedD);

                attrInfos.add(newAttrInfo);
            }
            else
            {
                attrInfos.add(attrInfo);
            }
        }

        final List<MBeanOperationInfo> operationInfos = ListUtil.newListFromArray(info.getOperations());
        operationInfos.addAll(ListUtil.newListFromArray(getAMXConfigMBeanInfo().getOperations()));

        final MBeanAttributeInfo[] attrs = new MBeanAttributeInfo[attrInfos.size()];
        attrInfos.toArray(attrs);

        final MBeanOperationInfo[] operations = new MBeanOperationInfo[operationInfos.size()];
        operationInfos.toArray(operations);

        newInfo = new MBeanInfo(
                info.getClassName(),
                info.getDescription(),
                attrs,
                info.getConstructors(),
                operations,
                info.getNotifications(),
                info.getDescriptor());

        MBeanInfo oldInfo = mInfos.putIfAbsent(intf, newInfo);

        return oldInfo != null ? oldInfo : newInfo;
    }

    public AMXConfigImpl(
            final ObjectName parentObjectName,
            final ConfigBean configBean)
    {
        super(parentObjectName, createMBeanInfo(configBean));

        mConfigBean = configBean;

        // eager initialization, it will be needed momentarily
        getConfigBeanJMXSupport();
    }

    @Override
    protected void setAttributeManually(final Attribute attr)
            throws AttributeNotFoundException, InvalidAttributeValueException
    {
        final AttributeList attrList = new AttributeList();
        attrList.add(attr);

        try
        {
            final AttributeList successList = setAttributesInConfigBean(attrList);
            if (successList.size() == 0)
            {
                throw new AttributeNotFoundException(attr.getName());
            }
        }
        catch (final Exception e)
        {
            // propogate the stack trace back, it's important for clients to have somethingto go on
            final Throwable rootCause = ExceptionUtil.getRootCause(e);
            throw new AttributeNotFoundException( ExceptionUtil.toString(rootCause) );
        }
    }

    /**
    Note that the default implementation sets attributes one at a time, but that
    MBeans with transactional requirements (eg configuration) may wish to set them as a group.
     */
    @Override
    public AttributeList setAttributes(final AttributeList attrs)
    {
        try
        {
            return setAttributesTransactionally(attrs);
        }
        catch (final Exception e)
        {
            // squelch, per JMX spec
        }

        // return an empty list, per JMX spec for failure
        return new AttributeList();
    }
    
    public AttributeList setAttributesTransactionally(final AttributeList attrs) throws Exception
    {
        final AttributeList successList = new AttributeList();

        try
        {
            final AttributeList delegateSuccess = setAttributesInConfigBean(attrs);
            successList.addAll(delegateSuccess);
        }
        catch (final Exception e)
        {
            // propogate the stack trace back, it's important for clients to have something to go on
            final Throwable rootCause = ExceptionUtil.getRootCause(e);
            
            // do not propagate back any proprietary exception; class might not exist on client
            throw new Exception( ExceptionUtil.toString(rootCause) );
        }

        return successList;
    }


    /**
    The actual name could be different than the 'name' property in the ObjectName if it
    contains characters that are illegal for an ObjectName.
    Also, there can be a Name attribute which is not a key value.
     */
    @Override
    public String getName()
    {
        final ConfigBean cb = getConfigBean();

        String name = AMXConfigLoader.getKey(cb);
        if ( name == null )
        {
            // deal with annoying and rare case of name existing, but not a key value
            name = cb.rawAttribute( "name" );
        }
        
        return name == null ? NO_NAME : name;
    }

    private final ConfigBean getConfigBean()
    {
        return mConfigBean;
    }

    private final ConfigBeanProxy getConfigBeanProxy()
    {
        return getConfigBean().getProxy(getConfigBean().getProxyType());
    }

    /**
    Resolve a template String.  See {@link AttributeResolver} for details.
     */
    public String resolveAttributeValue(final String varString)
    {
        if (!AttributeResolverHelper.needsResolving(varString))
        {
            return varString;
        }

        return new AttributeResolverHelper(getSelf(AMXConfigProxy.class)).resolve(varString);
    }

    public String resolveAttribute(final String attrName)
    {
        try
        {
            final Object value = getAttribute(attrName);
            return resolveAttributeValue(value == null ? null : "" + value);
        }
        catch (final AttributeNotFoundException e)
        {
            logger.log(Level.SEVERE, AMXLoggerInfo.attributeNotfound, new Object[]{attrName,getObjectName()});
            return null;
        }
    }

    public Boolean resolveBoolean(final String attrName)
    {
        return Boolean.parseBoolean(resolveAttribute(attrName));
    }

    public Integer resolveInteger(final String attrName)
    {
        return Integer.parseInt(resolveAttribute(attrName));
    }

    public Long resolveLong(final String attrName)
    {
        return Long.parseLong(resolveAttribute(attrName));
    }

    public AttributeList resolveAttributes(final String[] attrNames)
    {
        Issues.getAMXIssues().notDone("resolveAttributes: use annotations to create the correct type");

        final AttributeList attrs = getAttributes(attrNames);
        final AttributeList resolvedAttrs = new AttributeList();
        for (final Object o : attrs)
        {
            Attribute r = (Attribute) o;
            // allow non-String attributes
            final Object value = r.getValue();
            if ((value instanceof String) && AttributeResolverHelper.needsResolving((String) value))
            {
                final String resolvedValue = resolveAttributeValue((String) value);
                // TODO: use annotation to determine correct type
                r = new Attribute(r.getName(), resolvedValue);
            }

            resolvedAttrs.add(r);
        }

        return resolvedAttrs;
    }
    

//========================================================================================

    /**
        Parameters for creating one or more children, each of which can (recursively) contain
        other descendants.
     */
    static class CreateParams {
        final String              mType;
        final Map<String,Object>  mAttrs;
        final List<CreateParams>  mChildren;
        
        public CreateParams( final String type, final Map<String,?> values )
        {
            mAttrs    = MapUtil.newMap();
            mChildren = ListUtil.newList();
            mType     = type;
            
            if ( values == null )
            {
                return; // null is legal, no attributes
            }
            
            for (final Map.Entry<String,?> me : values.entrySet())
            {
                final String nameAsProvided = me.getKey();
                final String xmlName = ConfigBeanJMXSupport.toXMLName(nameAsProvided);  // or type
                final Object value   = me.getValue();

                if (value == null ||
                    (value instanceof String) ||
                    (value instanceof Number) ||
                    (value instanceof Boolean))
                {
                    //System.out.println( "toAttributeChanges: " + xmlName + " = " + value );
                    // auto-convert specific basic types to String
                    final String valueString = value == null ? null : "" + value;
                    mAttrs.put( xmlName, valueString);
                }
                else if (value instanceof String[])
                {
                    // A String[] is always mapped to a List<String>
                    mAttrs.put( xmlName, ListUtil.asStringList( value ) );
                }
                else if (value instanceof Map)
                {
                    // one sub-element whose type is its key in the containing Map
                    final Map<String, Object> m = TypeCast.checkMap(Map.class.cast(value), String.class, Object.class);
                    final CreateParams child = new CreateParams( xmlName, m);
                    //cdebug( "CreateParams for Map: create child of type: " + xmlName );
                    mChildren.add(child);
                }
                else if (value instanceof Map[])
                {
                    // one or more sub elements whose type is its key in the containing Map
                    final Map[] maps = (Map[])value;
                    for( final Map m : maps )
                    {
                        final Map<String,Object>  mTyped = TypeCast.checkMap(m, String.class, Object.class);
                        final CreateParams child = new CreateParams( xmlName, mTyped);
                        //cdebug( "CreateParams for Map[]: create child of type: " + xmlName );
                        mChildren.add(child);
                    }
                }
                else
                {
                    throw new IllegalArgumentException("Value of class " + value.getClass().getName() + " not supported for attribute " + nameAsProvided);
                }
            }
        }
        
        public String              type()     { return mType; }
        public String              name()     { return (String)mAttrs.get("name"); }
        public Map<String,Object>  attrs()    { return Collections.unmodifiableMap(mAttrs); }
        public List<CreateParams>  children() { return Collections.unmodifiableList(mChildren); }
       
        /**
            Convert incoming attributes to HK2 requirements.
         */
            List<AttributeChanges>
        toAttributeChanges(final Map<String, Object> values)
        {
            if ( values == null ) return null;
            
            final List<AttributeChanges> changes = ListUtil.newList();
            for (final String xmlName : mAttrs.keySet() )
            {
                final Object value = mAttrs.get(xmlName);

                if ( value instanceof String )
                {
                    changes.add( new ConfigSupport.SingleAttributeChange(xmlName, (String)value) );
                }
                else
                {
                    // what about String[]?
                    throw new IllegalArgumentException();
                }
            }
            return changes;
        }

        public String toString( final String prefix )
        {
            final StringBuilder buf = new StringBuilder();
            final String NL = StringUtil.LS;
            
            // crude toString, really should indent
            buf.append( prefix + mType + " = " + mAttrs + NL );
            if ( mChildren.size() != 0 )
            {
                buf.append( prefix + "[" );
                for ( final CreateParams child : mChildren )
                {
                    buf.append( child.toString("    " + prefix ) + NL );
                }
                buf.append( prefix + "]" );
            }
            
            return buf.toString();
        }
        public String toString()
        {
            return toString("");
        }
    }
    
    
    /**
        To make error messages more friendly and quick sanity check,
        verify that no conflicting children already exist.
     */
        private void
    checkForConflicts(final List<CreateParams>  children)
    {
        final Map<String, Map<String, AMXProxy>>  existingChildren = getSelf().childrenMaps();
        for( final CreateParams params : children )
        {
            final String type = params.type();
            final Map<String,AMXProxy> childrenOfType = existingChildren.get(type);
            if ( childrenOfType != null )
            {
                // children of this type exist, check that there is no conflicting child already
                final AMXProxy firstChild = childrenOfType.values().iterator().next();
                if ( firstChild.extra().singleton() )
                {
                    throw new IllegalArgumentException(  "Singleton child of type " + type + " already exists." );
                }
                if ( childrenOfType.get( params.name() ) != null)
                {
                    throw new IllegalArgumentException( "Child of type " + type + " named " + params.name() + " already exists." );
                }
            }
        }
    }
    
        ObjectName[]
    createChildren(
        final List<CreateParams>  children,
        final Map<String,Object>  attrs )
    {
        cdebug( children.toString() );
        checkForConflicts(children);

        final ConfigBeanProxy parent = getConfigBeanProxy();
        final ChildrenCreator creator = new ChildrenCreator( children, attrs);
        try
        {
            ConfigSupport.apply(creator, parent);
        }
        catch (Exception e)
        {
            AMXLoggerInfo.getLogger().log(Level.INFO, AMXLoggerInfo.cantCreateChildren, e );
            throw new RuntimeException(e);
        }

        // ensure that all new ConfigBeans have been registered as MBeans
        final List<ObjectName> newMBeans = ListUtil.newList();
        final List<ConfigBean> newDescendants = creator.configBeans();

        final AMXConfigLoader amxLoader = SingletonEnforcer.get(AMXConfigLoader.class);
        for( final ConfigBean newDescendant : newDescendants )
        {
           amxLoader.handleConfigBean(newDescendant, true);
           final ObjectName objectName = ConfigBeanRegistry.getInstance().getObjectName(newDescendant);
           newMBeans.add(objectName);
           //cdebug( "ADDED: " + objectName );
        }

        return CollectionUtil.toArray( newMBeans, ObjectName.class );
    }
    
        public ObjectName[]
    createChildren(
        final Map<String, Map<String,Object>[]> childrenMaps,
        final Map<String,Object> attrs )
    {
        final List<CreateParams> children = ListUtil.newList();

        for(Map.Entry<String,Map<String,Object>[]> entry : childrenMaps.entrySet()) {
            for(final Map<String,Object> m : entry.getValue()) {
                children.add( new CreateParams(entry.getKey(),m) );
            }
        }

        /* Find bug error
             for( final String type : childrenMaps.keySet() )
             {
                for( final Map<String,Object> m : childrenMaps.get(type) )
                {
                    children.add( new CreateParams(type, m) );
                }
             } */
        
        return createChildren( children, attrs);
    }
    
     /** Create one or more children */
    private final class ChildrenCreator implements ConfigCode
    {
        private final List<CreateParams> mChildrenMaps;
        private final Map<String,Object> mAttrs;
        private final List<ConfigBean>  mNewConfigBeans;

        ChildrenCreator( final List<CreateParams>  childrenMaps, final Map<String,Object> attrs)
        {
            mChildrenMaps = childrenMaps;
            mAttrs = attrs;
            mNewConfigBeans    = ListUtil.newList();
        }

        public Object run(final ConfigBeanProxy... params)
                throws PropertyVetoException, TransactionFailure
        {
            if (params.length != 1)
            {
                throw new IllegalArgumentException();
            }
            final ConfigBeanProxy parent = params[0];

            final ConfigBean source = (ConfigBean) ConfigBean.unwrap(parent);
            final ConfigSupport configSupport = source.getHabitat().getService(ConfigSupport.class);

            return _run(parent, configSupport);
        }

        public Object _run(
            final ConfigBeanProxy parent,
            final ConfigSupport configSupport)
                throws PropertyVetoException, TransactionFailure
        {
            final WriteableView parentW = WriteableView.class.cast(Proxy.getInvocationHandler(Proxy.class.cast(parent)));
            
            // if attributes were specified, set them first.
            if ( mAttrs != null )
            {
                setAttrs( parent, mAttrs );
            }
            
            final SubElementsCallback callback = new SubElementsCallback(mChildrenMaps);

            final ConfigBeanJMXSupport sptRoot = ConfigBeanJMXSupportRegistry.getInstance( Dom.unwrap(parent).getProxyType() );
            final List<ConfigBean>  newDescendants = callback.recursiveCreate( parentW, sptRoot, mChildrenMaps);
            mNewConfigBeans.addAll( newDescendants );
            
            return null;
        }
        
        public List<ConfigBean> configBeans() { return mNewConfigBeans; }
    }

    public ObjectName createChild(final String type, final Map<String, Object> params)
    {
        final CreateParams childParams = new CreateParams( type, params );
        
        final List<CreateParams> children = ListUtil.newList();
        children.add(childParams);
        final ObjectName[] objectNames = createChildren( children, null);
        
        return objectNames[0];
    }
    
    /**
        Replace "Name" or "name" with the 
     */
        Map<String,Object>
    replaceNameWithKey(
        final Map<String,Object>  attrs,
        final ConfigBeanJMXSupport spt)
    {
        String key = null;
        if ( attrs.containsKey(ATTR_NAME) )
        {
            key = ATTR_NAME;
        }
        else if ( attrs.containsKey("name") )
        {
            key = "name";
        }
        
        Map<String,Object> m = attrs;
        
        if ( key != null )
        {
            // map "Name" or "name" to the actual key value (which could be "name')
            final String xmlKeyName = spt.getNameHint();
            // rename to the appropriate key name, if it doesn't already exist
            // eg there could be a non-key attribute "Name" and another key attribute; leave that alone
            if ( xmlKeyName != null && ! attrs.keySet().contains(xmlKeyName) )
            {
                m = new HashMap<String,Object>(attrs);
                final Object value = m.remove(key);
                m.put( xmlKeyName, value );
            }
        }
        
        return m;
    }
    
    /** exists so we can get the parameterized return type */
    public static List<String> listOfString() { return null; }
    
    public static String convertAttributeName(final String s )
    {
        // do not alter any name that is already all lower-case or that contains a "-" */
        if ( s.equals( s.toLowerCase(Locale.getDefault()) ) || s.indexOf("-") >= 0 )
        {
            return(s);
        }
        
        // Dom.convertName() has a bug: IsFooBar => is-foo-bar, but is-foo-bar => -foo-bar.
        
        return Dom.convertName(s);
    }
    
    private void setAttrs(
        final ConfigBeanProxy     target,
        final Map<String,Object>  attrs )
    {
       final WriteableView targetW = WriteableView.class.cast(Proxy.getInvocationHandler(Proxy.class.cast(target)));
        
        for ( final Map.Entry<String,Object> me : attrs.entrySet() )
        {
            final String attrName = me.getKey();
            final Object attrValue = me.getValue();
            final String xmlName = convertAttributeName(attrName);
            
            final ConfigBean targetCB = (ConfigBean)Dom.unwrap(target);
            final ConfigModel.Property modelProp = targetCB.model.findIgnoreCase( xmlName );
            if ( modelProp == null )
            {
                throw new IllegalArgumentException( "Can't find ConfigModel.Property for attr " + xmlName + " on " + targetCB.getProxyType() );
            }
            //cdebug( "setting attribute \"" + attrName + "\" to \"" + attrValue + "\" on " + type );
            if ( modelProp.isCollection() )
            {
                //cdebug( "HANDLING COLLECTION FOR " + xmlName + " on " + targetCB.getProxyType().getName() );
                java.lang.reflect.Method m;
                try
                {
                    m = getClass().getMethod("listOfString", null);
                }
                catch( final Exception e )
                {
                    throw new IllegalStateException("impossible");
                }
                final java.lang.reflect.Type  listOfStringClass = m.getGenericReturnType();
                
                List<String>  list;
                if ( attrValue instanceof String[] )
                {
                    list = ListUtil.asStringList( attrValue );
                }
                else
                {
                    list = TypeCast.checkList( TypeCast.asList(attrValue), String.class);
                }
                targetW.setter( modelProp, list, listOfStringClass);
            }
            else
            {
                targetW.setter( modelProp, attrValue, String.class);
            }
            //cdebug( "set attribute \"" + attrName + "\" to \"" + attrValue + "\" on " + type );
        }
    }
    
    /**
    Callback to create sub-elements (recursively) on a newly created child element.
     */
    private final class SubElementsCallback implements TransactionCallBack<WriteableView>
    {
        private final List<CreateParams> mSubs;

        public SubElementsCallback(final List<CreateParams> subs)
        {
            mSubs = subs;
        }

        public void performOn(final WriteableView item) throws TransactionFailure
        {
            final ConfigBeanJMXSupport sptRoot = ConfigBeanJMXSupportRegistry.getInstance( com.sun.enterprise.config.serverbeans.Domain.class );
        
            recursiveCreate( item, sptRoot, mSubs );
        }
        
        /**
            If the child is of a type matching an @Element that is a List<its type>, then
            get that list and add it to it.
         */
        private void addToList(
            final WriteableView parent,
            final ConfigBeanProxy child )
        {
            final Class<? extends ConfigBeanProxy> parentClass = parent.getProxyType();
            final Class<? extends ConfigBeanProxy> childClass  = Dom.unwrap(child).getProxyType();
            final ConfigBeanJMXSupport  parentSpt = ConfigBeanJMXSupportRegistry.getInstance(parentClass);
            
            final ConfigBeanJMXSupport.ElementMethodInfo elementInfo = parentSpt.getElementMethodInfo(childClass);
            //cdebug( "Found: " + elementInfo + " for " + childClass + " on parent class " + parentClass.getName() );
            final ConfigBean parentBean = (ConfigBean)Dom.unwrap(parent.getProxy(parentClass));
            if ( elementInfo != null && Collection.class.isAssignableFrom(elementInfo.method().getReturnType()) )
            {
                // get the Collection and add the child
                final ConfigModel.Property modelProp = parentBean.model.findIgnoreCase( elementInfo.xmlName() );
                final List  list = (List)parent.getter( modelProp, elementInfo.method().getGenericReturnType() );
                //cdebug( "Adding child to list obtained via " + elementInfo.method().getName() + "(), " + childClass );
                list.add( child );
            }
            else if (elementInfo != null)
            {
                //cdebug( "Child is a singleton, adding via setter " + elementInfo.method().getName() + "()" );
                final ConfigModel.Property modelProp = parentBean.model.findIgnoreCase( elementInfo.xmlName() );
                if ( modelProp == null )
                {
                    throw new IllegalArgumentException( "Can't find ConfigModel.Property for \"" + elementInfo.xmlName() + "\"" );
                }
                parent.setter( modelProp, child, childClass );
            }
        }
        
        private List<ConfigBean> recursiveCreate(
            final WriteableView parent,
            final ConfigBeanJMXSupport sptRoot,
            final List<CreateParams> subs ) throws TransactionFailure
        {
            final List<ConfigBean> newChildren = ListUtil.newList();
            
            // create each sub-element, recursively
            for (final CreateParams childParams : subs )
            {
                final String type = childParams.type();
                //cdebug( "recursiveCreate: " + type );
                
                final Class<? extends ConfigBeanProxy> clazz = ConfigBeanJMXSupportRegistry.getConfigBeanProxyClassFor(sptRoot, type);
                if ( clazz == null )
                {
                    throw new IllegalArgumentException("@Configured interface for type " + type + " cannot be found" );
                }
                
                final ConfigBeanJMXSupport spt = ConfigBeanJMXSupportRegistry.getInstance(clazz);

                final ConfigBeanProxy childProxy = parent.allocateProxy(clazz);
                Dom newBean = Dom.unwrap(childProxy);
                newBean.addDefaultChildren();
                addToList( parent, childProxy);
                final ConfigBean child = (ConfigBean)Dom.unwrap(childProxy);
                newChildren.add(child);
                final WriteableView childW = WriteableView.class.cast(Proxy.getInvocationHandler(Proxy.class.cast(childProxy)));
                //cdebug("Created sub-element of type: " + type + ", " + clazz);
                
                final Map<String,Object> childAttrs = replaceNameWithKey( childParams.attrs(), spt);
                setAttrs( childProxy, childAttrs );
                
                if ( childParams.children().size() != 0 )
                {
                    final List<ConfigBean> more = recursiveCreate( childW, spt, childParams.children() );
                    newChildren.addAll(more);
                }
            }
            return newChildren;
        }
    }

    public ObjectName removeChild(final String type)
    {
        final ObjectName child = child(type);
        if (child == null)
        {
            logger.log(Level.SEVERE, AMXLoggerInfo.childNotfound, type);
            return null;
        }

        return remove(child);
    }

    public ObjectName removeChild(final String type, final String name)
    {
        final ObjectName child = child(type, name);
        if (child == null) return null;

        return remove(child);
    }

    private final ObjectName remove(final ObjectName childObjectName)
    {
        ObjectName removed = null;
        try
        {
            final ConfigBean childConfigBean = ConfigBeanRegistry.getInstance().getConfigBean(childObjectName);

            try
            {
                //cdebug("REMOVING config of class " + childConfigBean.getProxyType().getName() + " from  parent of type " +
                       //getConfigBean().getProxyType().getName() + ", ObjectName = " + JMXUtil.toString(childObjectName));
                ConfigSupport.deleteChild(this.getConfigBean(), childConfigBean);
                removed = childObjectName;
            }
            catch (final TransactionFailure tf)
            {
                throw new RuntimeException("Transaction failure deleting " + JMXUtil.toString(childObjectName), tf);
            }

            // NOTE: MBeans unregistered asynchronously by AMXConfigLoader
            // enforce synchronous semantics to clients by waiting until this happens
            //  the listener is smart enough not to wait if it's already unregistered
            final UnregistrationListener myListener = new UnregistrationListener(getMBeanServer(), childObjectName);
            final long TIMEOUT_MILLIS = 10 * 1000;
            final boolean unregisteredOK = myListener.waitForUnregister(TIMEOUT_MILLIS);
            //cdebug( "Waiting for child to be unregistered: " + childObjectName );
            if (!unregisteredOK)
            {
                throw new RuntimeException("Something went wrong unregistering MBean " + JMXUtil.toString(childObjectName));
            }
        }
        catch (final Exception e)
        {
            throw new RuntimeException("Problem deleting " + childObjectName, e);
        }
        return removed;
    }

    private Object invokeDuckMethod(
            final ConfigBeanJMXSupport.DuckTypedInfo info,
            Object[] args)
            throws MBeanException
    {
        try
        {
            //cdebug( "invokeDuckMethod(): invoking: " + info.name() + " on " + info.method().getDeclaringClass() );

            if (!info.method().getDeclaringClass().isAssignableFrom(getConfigBeanProxy().getClass()))
            {
                throw new IllegalArgumentException("invokeDuckMethod: " + getConfigBean().getProxyType() + " not asssignable to " + info.method().getDeclaringClass());
            }

            Object result = info.method().invoke(getConfigBeanProxy(), args);
            result = translateResult(result);
            
            // cdebug( "invokeDuckMethod(): invoked: " + info.name() + ", got " + result );

            return result;
        }
        catch (final Exception e)
        {
            throw new MBeanException(e);
        }
    }
    
    private ObjectName getObjectName( final ConfigBeanProxy cbp )
    {
        final Dom dom = Dom.unwrap(cbp);
        
        if ( dom instanceof ConfigBean )
        {
            return ConfigBeanRegistry.getInstance().getObjectName( (ConfigBean)dom );
        }
        
        // we can't return a Dom over the wire
        return null;
    }

    /**
        Convert results that contain local ConfigBeanProxy into ObjectNames.
        Ignore other items, passing through unchanged.
     */
    private Object translateResult(final Object result )
    {
        // short-circuit the common case
        if ( result instanceof String ) return result;
        
        Object out = result;

        // ConfigBean types must be mapped back to ObjectName; they can't go across the wire
            
        if ( result instanceof ConfigBeanProxy )
        {
            out = getObjectName( (ConfigBeanProxy)result );
        }
        else if ( result instanceof Collection )
        {
            final Collection<Object> c = (Collection)result;
            final Collection<Object> translated = new ArrayList<Object>();
            for( final Object item : c )
            {
                translated.add( translateResult(item) );
            }
            
            if ( result instanceof Set )
            {
                out = new HashSet<Object>(translated);
            }
            else if ( result instanceof AbstractQueue )
            {
                out = new LinkedBlockingDeque(translated);
            }
            else
            {
                out = translated;
            }
        }
        else if ( result instanceof Map )
        {
            final Map resultMap = (Map)result;
            Map outMap = new HashMap();
            for( final Object meo : resultMap.entrySet() )
            {
                Map.Entry me = (Map.Entry)meo;
                outMap.put( translateResult(me.getKey()), translateResult( me.getValue() ) );
            }
            out = outMap;
        }
        else if ( result.getClass().isArray() )
        {
            final Class<?> componentType = result.getClass().getComponentType();
            if ( ConfigBeanProxy.class.isAssignableFrom(componentType) )
            {
                final Object[] items = (Object[])result;
                final ObjectName[] objectNames = new ObjectName[items.length];
                for( int i = 0; i < items.length; ++i )
                {
                    objectNames[i]  = getObjectName( (ConfigBeanProxy)items[i] );
                }
                out = objectNames;
            }
        }
        
        return out;
    }
        
    /**
    Automatically figure out get<abc>Factory(),
    create<Abc>Config(), remove<Abc>Config().

     */
    @Override
    protected Object invokeManually(
            String operationName,
            Object[] args,
            String[] types)
            throws MBeanException, ReflectionException, NoSuchMethodException, AttributeNotFoundException
    {
        Object result = null;
        debugMethod(operationName, args);

        ConfigBeanJMXSupport.DuckTypedInfo duckTypedInfo = null;
        if ((duckTypedInfo = getConfigBeanJMXSupport().findDuckTyped(operationName, types)) != null)
        {
            result = invokeDuckMethod(duckTypedInfo, args);
        }
        else
        {
            result = super.invokeManually(operationName, args, types);
        }
        return result;
    }

    public void sendConfigCreatedNotification(final ObjectName configObjectName)
    {
        sendNotification(CONFIG_CREATED_NOTIFICATION_TYPE,
                CONFIG_REMOVED_NOTIFICATION_TYPE,
                CONFIG_OBJECT_NAME_KEY, configObjectName);
    }

    public void sendConfigRemovedNotification(final ObjectName configObjectName)
    {
        sendNotification(CONFIG_REMOVED_NOTIFICATION_TYPE,
                CONFIG_REMOVED_NOTIFICATION_TYPE,
                CONFIG_OBJECT_NAME_KEY, configObjectName);
    }

    private final ConfigBeanJMXSupport getConfigBeanJMXSupport()
    {
        return ConfigBeanJMXSupportRegistry.getInstance(getConfigBean());
    }

    private static final Map<String, String> getDefaultValues(final Class<? extends ConfigBeanProxy> intf, boolean useAMXAttributeNames)
    {
        return ConfigBeanJMXSupportRegistry.getInstance(intf).getDefaultValues(useAMXAttributeNames);
    }

    public final Map<String, String> getDefaultValues(final String type, final boolean useAMXAttributeNames)
    {
        final Class<? extends ConfigBeanProxy> intf = getConfigBeanProxyClassForContainedType(type);

        return getDefaultValues(intf, useAMXAttributeNames);
    }

    public final Map<String, String> getDefaultValues(final boolean useAMXAttributeNames)
    {
        return getDefaultValues(mConfigBean.getProxyType(), useAMXAttributeNames);
    }

    private Class<? extends ConfigBeanProxy> getConfigBeanProxyClassForContainedType(final String type)
    {
        final ConfigBeanJMXSupport spt = getConfigBeanJMXSupport();

        return ConfigBeanJMXSupportRegistry.getConfigBeanProxyClassFor(spt, type);
    }

    @Override
    protected String[] attributeNameToType(final String attributeName)
    {
        return new String[]
                {
                    Util.typeFromName(attributeName), attributeName
                };
    }

    @Override
    protected Object getAttributeManually(final String name)
            throws AttributeNotFoundException, ReflectionException, MBeanException
    {
        return getAttributeFromConfigBean(name);
    }


//-------------------------------------------------------------
    /**
    Get an Attribute.  This is a bit tricky, because the target can be an XML attribute,
    an XML string element, or an XML list of elements.
     */
    protected final Object getAttributeFromConfigBean(final String amxName)
    {
        Object result = null;

        final MBeanAttributeInfo attrInfo = getAttributeInfo(amxName);
        if ( attrInfo == null )
        {
            // 
            // check for  PSEUDO ATTTRIBUTES implemented as methods eg getFoo()
            //
            //cdebug( "getAttributeFromConfigBean: no info for " + amxName );
            
            ConfigBeanJMXSupport.DuckTypedInfo info = getConfigBeanJMXSupport().findDuckTyped("get" + amxName, null);
            if ( info == null )
            {
                info = getConfigBeanJMXSupport().findDuckTyped("is" + amxName, null);
            }
            if ( info != null )
            {
                //cdebug( "getAttributeFromConfigBean: found DuckTyped for " + amxName );
                try
                {
                    result = invokeDuckMethod( info, null);
                    return result;
                }
                catch( final Exception e )
                {
                    throw new RuntimeException( new MBeanException( e, amxName ) );
                }
                
            }
            else
            {
                //cdebug( "getAttributeFromConfigBean: no DuckTyped for " + amxName );
            }
            throw new RuntimeException( new AttributeNotFoundException( amxName ) );
        }
        final String xmlName = ConfigBeanJMXSupport.xmlName(attrInfo, amxName);
        final boolean isAttribute = ConfigBeanJMXSupport.isAttribute(attrInfo);

        if (isAttribute)
        {
            result = mConfigBean.rawAttribute(xmlName);
        }
        else if (ConfigBeanJMXSupport.isElement(attrInfo))
        {
            if (String.class.getName().equals(attrInfo.getType()))
            {
                final List<?> leaf = mConfigBean.leafElements(xmlName);
                if (leaf != null)
                {
                    try
                    {
                        result = (String) leaf.get(0);
                    }
                    catch (final Exception e)
                    {
                        // doesn't exist, return null
                    }
                }
            }
            else if (attrInfo.getType().equals(String[].class.getName()))
            {
                //final String elementClass = (String)d.getFieldValue( DESC_ELEMENT_CLASS );

                final List<?> leaf = mConfigBean.leafElements(xmlName);
                if (leaf != null)
                {
                    // verify that it is List<String> -- no other types are supported in this way
                    final List<String> elems = TypeCast.checkList(leaf, String.class);
                    result = CollectionUtil.toArray(elems, String.class);
                }
            }
            else
            {
                throw new IllegalArgumentException("getAttributeFromConfigBean: unsupported return type: " + attrInfo.getType());
            }
        }
        //debug( "Attribute " + amxName + " has class " + ((result == null) ? "null" : result.getClass()) );
        return result;
    }

    private static final class MyTransactionListener implements TransactionListener
    {
        private final List<PropertyChangeEvent> mChangeEvents = new ArrayList<PropertyChangeEvent>();

        private final ConfigBean mTarget;

        MyTransactionListener(final ConfigBean target)
        {
            mTarget = target;
        }

        public void transactionCommited(List<PropertyChangeEvent> changes)
        {
            // include only events that match the desired config bean; other transactions
            // could generate events on other ConfigBeans. For that matter, it's unclear
            // why more than one transaction on the same ConfigBean couldn't be "heard" here.
            for (final PropertyChangeEvent event : changes)
            {
                final Object source = event.getSource();
                if (source instanceof ConfigBeanProxy)
                {
                    final Dom dom = Dom.unwrap((ConfigBeanProxy) source);
                    if (dom instanceof ConfigBean)
                    {
                        if (mTarget == (ConfigBean) dom)
                        {
                            mChangeEvents.add(event);
                        }
                    }
                }
            }
        }

        public void unprocessedTransactedEvents(List<UnprocessedChangeEvents> changes)
        {
            // amx probably does not care that some changes were not processed successfully
            // and will require a restart
        }

        List<PropertyChangeEvent> getChangeEvents()
        {
            return mChangeEvents;
        }

    };

    private void joinTransaction(final Transaction t, final WriteableView writeable)
            throws TransactionFailure
    {
        if (!writeable.join(t))
        {
            t.rollback();
            throw new TransactionFailure("Cannot enlist " + writeable.getProxyType() + " in transaction", null);
        }
    }

    private static void commit(final Transaction t)
            throws TransactionFailure
    {
        try
        {
            t.commit();
        }
        catch (final RetryableException e)
        {
            t.rollback();
            throw new TransactionFailure(e.getMessage(), e);
        }
        catch (final TransactionFailure e)
        {
            //cdebug("failure, not retryable...");
            t.rollback();
            throw e;
        }
    }

    static <T extends ConfigBeanProxy> WriteableView getWriteableView(final T s, final ConfigBean sourceBean)
            throws TransactionFailure
    {
        final WriteableView f = new WriteableView(s);
        if (sourceBean.getLock().tryLock())
        {
            return f;
        }
        throw new TransactionFailure("Config bean already locked " + sourceBean, null);
    }

    private static Type getCollectionGenericType()
    {
        try
        {
            return ConfigSupport.class.getDeclaredMethod("defaultPropertyValue", (Class[]) null).getGenericReturnType();
        }
        catch (NoSuchMethodException e)
        {
            // not supposed to happen, throw any reasonabl exception
            throw new IllegalArgumentException();
        }
    }

    /**
    Handle an update to a collection, returning the List<String> that results.
     */
    private List<String> handleCollection(
            final WriteableView writeable,
            final ConfigModel.Property prop,
            final List<String> argValues)
    {
        final Object o = writeable.getter(prop, getCollectionGenericType());
        final List<String> masterList = TypeCast.checkList(TypeCast.asList(o), String.class);

        //cdebug( "Existing values: {" + CollectionUtil.toString( masterList ) + "}");
        //cdebug( "Arg values: {" + CollectionUtil.toString( argValues ) + "}");

        masterList.retainAll(argValues);
        for (final String s : argValues)
        {
            if (!masterList.contains(s))
            {
                masterList.add(s);
            }
        }

        //cdebug( "Existing values list before commit: {" + CollectionUtil.toString( masterList ) + "}");
        return new ArrayList<String>(masterList);
    }

    private class Applyer
    {
        final Transaction mTransaction;

        final ConfigBean mConfigBean;

        final WriteableView mWriteable;

        public Applyer(final ConfigBean cb) throws TransactionFailure
        {
            this(cb, new Transaction());
        }

        public Applyer(final ConfigBean cb, final Transaction t)
                throws TransactionFailure
        {
            mConfigBean = cb;
            mTransaction = t;

            final ConfigBeanProxy readableView = cb.getProxy(cb.getProxyType());
            mWriteable = getWriteableView(readableView, cb);
        }

        protected void makeChanges()
                throws TransactionFailure
        {
        }

        final void apply()
                throws TransactionFailure
        {
            try
            {
                joinTransaction(mTransaction, mWriteable);

                makeChanges();

                commit(mTransaction);
            }
            finally
            {
                mConfigBean.getLock().unlock();
            }
        }

    }

    protected ConfigModel.Property getConfigModel_Property(final String xmlName)
    {
        final ConfigModel.Property cmp = mConfigBean.model.findIgnoreCase(xmlName);
        if (cmp == null)
        {
            throw new IllegalArgumentException("Illegal name: " + xmlName);
        }
        return cmp;
    }

    private final class MakeChangesApplyer extends Applyer
    {
        private final Map<String, Object> mChanges;

        public MakeChangesApplyer(
                final ConfigBean cb,
                final Map<String, Object> changes)
                throws TransactionFailure
        {
            super(cb);
            mChanges = changes;
        }

        protected void makeChanges()
                throws TransactionFailure
        {
            for (final String xmlName : mChanges.keySet())
            {
                final Object value = mChanges.get(xmlName);
                final ConfigModel.Property prop = getConfigModel_Property(xmlName);

                if (prop.isCollection())
                {
                    handleCollection(mWriteable, prop, ListUtil.asStringList(value));
                }
                else if (value == null || (value instanceof String))
                {
                    mWriteable.setter(prop, value, String.class);
                }
                else
                {
                    throw new TransactionFailure("Illegal data type for attribute " + xmlName + ": " + value.getClass().getName());
                }
            }
        }

    }

    private Map<String, Object> mapNamesAndValues(
            final Map<String, Object> amxAttrs,
            final Map<String, Object> noMatch)
    {
        final Map<String, Object> xmlAttrs = new HashMap<String, Object>();

        final Map<String, MBeanAttributeInfo> attrInfos = getAttributeInfos();

        for (final Map.Entry<String, Object> me : amxAttrs.entrySet())
        {
            final String amxAttrName = me.getKey();
            final Object valueIn = me.getValue();

            final MBeanAttributeInfo attrInfo = attrInfos.get(amxAttrName);
            if (attrInfo == null)
            {
                debug("WARNING: setAttributes(): no MBeanAttributeInfo found for: " + amxAttrName);
                noMatch.put(amxAttrName, valueIn);
                continue;
            }
            final String xmlName = ConfigBeanJMXSupport.xmlName(attrInfo, amxAttrName);

            if (xmlName != null)
            {
                //cdebug( "mapNamesAndValues: " + xmlName );
                    
                final Object value = valueIn;
                
                // We accept only Strings, String[] or null
                if (valueIn == null || (value instanceof String))
                {
                    xmlAttrs.put(xmlName, (String) value);
                }
                else
                {
                    final ConfigModel.Property prop = getConfigModel_Property(xmlName);
                    if ( prop != null && prop.isCollection() )
                    {
                        //cdebug( "mapNamesAndValues: is a collection: " + xmlName );
                        if ((valueIn instanceof String[]) || (valueIn instanceof List))
                        {
                            //cdebug( "mapNamesAndValues: is a collection, setting value to List<String>: " + xmlName );
                            xmlAttrs.put(xmlName, ListUtil.asStringList(valueIn));
                        }
                        else
                        {
                            noMatch.put(amxAttrName, valueIn);
                        }
                    }
                    else
                    {
                        noMatch.put(amxAttrName, valueIn);
                    }
                }
            // debug( "Attribute " + amxAttrName + "<=>" + xmlName + " is of class " + ((value == null) ? null : value.getClass().getName()) );
            }
            else
            {
                debug("WARNING: setAttributes(): no xmlName match found for AMX attribute: " + amxAttrName);
                noMatch.put(amxAttrName, valueIn);
            }
        }

        return xmlAttrs;
    }

    public AttributeList setAttributesInConfigBean(final AttributeList attrsIn) throws TransactionFailure
    {
        // now map the AMX attribute names to xml attribute names
        final Map<String, Object> amxAttrs = JMXUtil.attributeListToValueMap(attrsIn);
        final Map<String, Object> notMatched = new HashMap<String, Object>();
        final Map<String, Object> xmlAttrs = mapNamesAndValues(amxAttrs, notMatched);

        if (notMatched.keySet().size() != 0)
        {
            cdebug("setAttributes: failed to map these AMX attributes: {" + CollectionUtil.toString(notMatched.keySet(), ", ") + "}");

        }
        
        //System.out.println( "setAttributesInConfigBean: " + amxAttrs);

        final AttributeList successfulAttrs = new AttributeList();

        final Transactions transactions = mConfigBean.getHabitat().getService(Transactions.class);

        if (xmlAttrs.size() != 0)
        {
            //cdebug( "DelegateToConfigBeanDelegate.setAttributes(): " + attrsIn.size() + " attributes: {" +
            //     CollectionUtil.toString(amxAttrs.keySet()) + "} mapped to xml names {" + CollectionUtil.toString(xmlAttrs.keySet()) + "}");

            final MyTransactionListener myListener = new MyTransactionListener(mConfigBean);
            transactions.addTransactionsListener(myListener);

            // results should contain only those that succeeded which will be all or none
            // depending on whether the transaction worked or not
            try
            {
                final MakeChangesApplyer mca = new MakeChangesApplyer(mConfigBean, xmlAttrs);
                mca.apply();

                // use 'attrsIn' vs 'attrs' in case not all values are 'String'
                successfulAttrs.addAll(attrsIn);
            }
            catch (final TransactionFailure tf)
            {
                // empty results -- no Exception should be thrown per JMX spec
                cdebug(ExceptionUtil.toString(tf));
                throw(tf);
            }
            finally
            {
                transactions.waitForDrain();

                transactions.removeTransactionsListener(myListener);
            }
        }

        return successfulAttrs;
    }
    
    /**
        Share one sequence number for *all* Config MBeans to keep overhead low
        instead of 
     */
    private static final AtomicLong sSequenceNumber = new AtomicLong(0);
    
        void
    issueAttributeChangeForXmlAttrName(
        final String xmlAttrName,
        final String message,
        final Object oldValue,
        final Object newValue,
        final long   whenChanged )
    {
        final Map<String,String>  m = getConfigBeanJMXSupport().getFromXMLNameMapping();
        final String attributeName = m.containsKey(xmlAttrName) ?  m.get(xmlAttrName) : xmlAttrName;
        if ( attributeName.equals(xmlAttrName) )    // will *always* be different due to camel case
        {
            //cdebug( "issueAttributeChangeForXmlAttrName(): MBean attribute name not found for xml name, using xml name: " + xmlAttrName );
            logger.log(Level.SEVERE, AMXLoggerInfo.attributeNotfound, xmlAttrName);
        }
        
        final String attributeType = String.class.getName();
        
        getLogger().fine( getObjectName() + " -- " + attributeName + " = " + newValue + " <== " + oldValue );
        
        // if ( getListenerCount() == 0 ) return;
        
        final long sequenceNumber = sSequenceNumber.getAndIncrement();
		final AttributeChangeNotification	notif =
            new AttributeChangeNotification( getObjectName(), sequenceNumber, whenChanged, message, attributeName, attributeType, oldValue, newValue);
			
		sendNotification( notif );
        
        //cdebug( "AMXConfigImpl.issueAttributeChangeForXmlAttrName(): sent: " + notif );
    }
}





















