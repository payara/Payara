/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.management.Descriptor;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.AttributeChangeNotification;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.ObjectName;
import javax.management.modelmbean.DescriptorSupport;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import com.sun.enterprise.config.serverbeans.Domain;
import org.glassfish.external.arc.Stability;
import org.glassfish.external.arc.Taxonomy;
import static org.glassfish.external.amx.AMX.*;
import org.glassfish.admin.amx.core.Util;
import static org.glassfish.admin.amx.config.AMXConfigConstants.*;
import org.glassfish.admin.amx.config.AMXConfigProxy;
import org.glassfish.admin.amx.impl.util.ImplUtil;
import org.glassfish.admin.amx.util.ClassUtil;
import org.glassfish.admin.amx.util.CollectionUtil;
import org.glassfish.admin.amx.util.MapUtil;
import org.glassfish.admin.amx.util.SetUtil;
import org.glassfish.admin.amx.util.ListUtil;
import org.glassfish.admin.amx.util.StringUtil;
import org.glassfish.admin.amx.util.jmx.JMXUtil;
import org.glassfish.admin.amx.util.stringifier.SmartStringifier;
import org.glassfish.api.admin.config.PropertiesDesc;
import org.glassfish.api.admin.config.PropertyDesc;
import org.glassfish.quality.ToDo;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.Units;
import org.jvnet.hk2.config.ConfigBean;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Dom;
import org.jvnet.hk2.config.DuckTyped;
import org.jvnet.hk2.config.Element;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.ConfigModel;
import org.jvnet.hk2.config.DomDocument;
import org.glassfish.admin.amx.impl.util.InjectedValues;

import java.util.logging.Level;
import org.glassfish.admin.amx.util.AMXLoggerInfo;


/**
 * Helps generate required JMX artifacts (MBeanInfo, etc) from a ConfigBean interface, as well
 * storing author useful information about each @Configured interface.
 * @author llc
 */
@Taxonomy(stability = Stability.NOT_AN_INTERFACE)
class ConfigBeanJMXSupport
{
    private final Class<? extends ConfigBeanProxy> mIntf;

    private final List<AttributeMethodInfo> mAttrInfos = new ArrayList<AttributeMethodInfo>();

    private final List<ElementMethodInfo> mElementInfos = new ArrayList<ElementMethodInfo>();

    private final List<DuckTypedInfo> mDuckTypedInfos = new ArrayList<DuckTypedInfo>();

    private final NameHint mNameHint;

    private final MBeanInfo mMBeanInfo;

    private final String mKey;    // xml name

    private static String nameFromKey(final String key)
    {
        if (key == null)
        {
            return null;
        }

        if (key.startsWith("@"))
        {
            return key.substring(1);
        }

        if (key.startsWith("<"))
        {
            return key.substring(1, key.length() - 1);
        }

        throw new IllegalArgumentException(key);
    }

    ConfigBeanJMXSupport(final ConfigBean configBean)
    {
        this(configBean.getProxyType(), nameFromKey(configBean.model.key));

        //debug( "ConfigBeanJMXSupport: " + configBean.getProxyType().getName() + ": key=" +  configBean.model.key + ", keyedAs=" + configBean.model.keyedAs);

        //debug( toString() );
    }

    /**
    The 'key' should not be necessary as the annotations should supply that information.
    But some are defective, without setting key=true.
     */
    ConfigBeanJMXSupport(
            final Class<? extends ConfigBeanProxy> intf,
            final String key)
    {
        mIntf = intf;
        mKey = key;

        findStuff(intf, mAttrInfos, mElementInfos, mDuckTypedInfos);
        sanityCheckConfigured();

        mMBeanInfo = _getMBeanInfo();
        sanityCheckMBeanInfo();
        mNameHint = findNameHint();

        /**
        if (hasConfiguredBug() && key == null) {
        ImplUtil.getLogger().warning("ConfigBeanJMXSupport (AMX): working around @Configured bug for " + mIntf.getName() +
        ", using \"" + configuredBugKey() + "\" as the key attribute");
        }
         */
    }

    public Class<? extends ConfigBeanProxy> getIntf() { return mIntf; }
    
    public String toString()
    {
        final StringBuilder buf = new StringBuilder();

        final String DELIM = ", ";

        final String NL = StringUtil.NEWLINE();

        buf.append(mIntf.getName() + " = ");
        buf.append(NL + "Attributes: {" + NL);
        for (final AttributeMethodInfo info : mAttrInfos)
        {
            buf.append(info.attrName() + "/" + info.xmlName() + DELIM);
        }
        buf.append(NL + "}" + NL + "Elements: {" + NL);
        for (final ElementMethodInfo info : mElementInfos)
        {
            buf.append(info.attrName() + "/" + info.xmlName() + DELIM);
        }

        final Set<String> childTypes = childTypes().keySet();
        buf.append(NL + "}" + NL + "Child types: {" + NL);
        for (final String type : childTypes)
        {
            buf.append(type + DELIM);
        }

        buf.append(NL + "}" + NL + "DuckTyped: {" + NL);
        for (final DuckTypedInfo info : mDuckTypedInfos)
        {
            buf.append(info + NL);
        }
        buf.append(NL + "}" + NL);

        return buf.toString();
    }

    /**
    Is the signature a perfect match?
    In a modular system, we can't load classes from classnames to compare, so we
    can't do isAssignableFrom().  So look for  a perfect match as the priority.
     */
    private boolean isPerfectMatch(final String[] types, final Class<?>[] sig)
    {
        if (types == null && (sig == null || sig.length == 0))
        {
            return true;
        }
        boolean mismatch = false;

        for (int i = 0; i < sig.length; ++i)
        {
            if (sig[i].getName().equals(types[i]))
            {
                mismatch = true;
                break;
            }
        }
        return !mismatch;
    }

    public DuckTypedInfo findDuckTyped(final String name, final String[] types)
    {
        DuckTypedInfo info = null;

        final int numTypes = types == null ? 0 : types.length;
        for (final DuckTypedInfo candidate : mDuckTypedInfos)
        {
            // debug( "Match " + name + "=" + numTypes + " against " + candidate.name()  + "=" + candidate.signature().length );
            final Class<?>[] sig = candidate.signature();
            if (candidate.name().equals(name) && numTypes == sig.length)
            {
                //debug( "Matched DuckTyped method: " + name );
                if (isPerfectMatch(types, sig))
                {
                    info = candidate;
                    break;
                }
                else if (info == null)
                {
                    // first one takes priority
                    info = candidate;
                }
            }
        }

        return info;
    }

    public String getTypeString()
    {
        return getTypeString(mIntf);
    }

    public String getTypeString(final Class<? extends ConfigBeanProxy> intf)
    {
        String type = null;

        final Configured configuredAnnotation = intf.getAnnotation(Configured.class);
        if (configuredAnnotation != null && configuredAnnotation.name().length() != 0)
        {
            type = configuredAnnotation.name();
            if ( type == null || type.length() == 0 )
            {
                throw new IllegalArgumentException("ConfigBeanJMXSupport.getTypeString(): Malformed @Configured annotation on " + intf.getName() );
            }
        }
        else
        {
            final Package pkg = intf.getPackage();
            String simple = intf.getName().substring(pkg.getName().length() + 1, intf.getName().length());
            type = Util.typeFromName(simple);
            if ( type == null || type.length() == 0 )
            {
                throw new IllegalArgumentException("ConfigBeanJMXSupport.getTypeString(): Malformed type generated from " + intf.getName() );
            }
        }
        return type;
    }

    public MBeanInfo getMBeanInfo()
    {
        return mMBeanInfo;
    }
    
    // create only one of these, it's always the same
    private static final MBeanNotificationInfo ATTRIBUTE_CHANGE_NOTIF_INFO = 
        new MBeanNotificationInfo(
            new String[] { AttributeChangeNotification.ATTRIBUTE_CHANGE},
            AttributeChangeNotification.class.getName(),
            "attribute change");

    private MBeanInfo _getMBeanInfo()
    {
        final List<MBeanAttributeInfo> attrsList = new ArrayList<MBeanAttributeInfo>();

        for (final AttributeMethodInfo info : mAttrInfos)
        {
            attrsList.add(attributeToMBeanAttributeInfo(info));
        }
        for (final ElementMethodInfo e : mElementInfos)
        {
            final MBeanAttributeInfo attrInfo = elementToMBeanAttributeInfo(e.method());
            if (attrInfo != null)
            {
                attrsList.add(attrInfo);
            }
        }

        final MBeanAttributeInfo[] attrs = new MBeanAttributeInfo[attrsList.size()];
        attrsList.toArray(attrs);

        final String classname = mIntf.getName();
        final String description = "ConfigBean " + mIntf.getName();
        final MBeanOperationInfo[] operations = toMBeanOperationInfos();
        final Descriptor descriptor = descriptor();
        final MBeanNotificationInfo[] notifications = new MBeanNotificationInfo[] {ATTRIBUTE_CHANGE_NOTIF_INFO};

        final MBeanInfo info = new MBeanInfo(
                classname,
                description,
                attrs,
                null,
                operations,
                notifications,
                descriptor);

        return info;
    }

    private boolean hasNameAttribute()
    {
        for (final MBeanAttributeInfo attrInfo : getMBeanInfo().getAttributes())
        {
            if (ATTR_NAME.equals(attrInfo.getName()))
            {
                return true;
            }
        }
        return false;
    }

    private void sanityCheckMBeanInfo()
    {
        // verify that we don't have an item with getName() that's marked as a singleton
        // another ID could be used too (eg 'thread-pool-id'), no way to tell.
        if (isSingleton())
        {
            if (hasNameAttribute())
            {
                AMXLoggerInfo.getLogger().log(Level.FINE, 
                        "ConfigBeanJMXSupport (AMX): @Configured interface {0} has getName() which is not a key value.  Remove getName() or use @Attribute(key=true)",
                        mIntf.getName());
            }
        }
    }

    // if no key value can be found, consider it a singleton
    public boolean isSingleton()
    {
        if (mKey != null)
        {
            return false;
        }
        /*
        if (hasConfiguredBug()) {
        return false;
        }
         */

        for (final AttributeMethodInfo info : mAttrInfos)
        {
            if (info.key())
            {
                return false;
            }
        }

        for (final ElementMethodInfo info : mElementInfos)
        {
            if (info.key())
            {
                return false;
            }
        }

        return true;
    }

    // if no elements, then it's a leaf
    // Tricky case FIXME:  what if there are List<String> elements.
    boolean isLeaf()
    {
        return mElementInfos.size() == 0;
    }

    /** partial list (quick check) of legal remoteable types */
    private static final Set<Class<?>> REMOTABLE = SetUtil.newSet(new Class<?>[]
            {
                Void.class,
                Boolean.class,
                Character.class,
                String.class,
                Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class, BigDecimal.class, BigInteger.class,
                Date.class,
                ObjectName.class,
                CompositeType.class,
                CompositeDataSupport.class
            });

    /**
    Be very conservative at first.
    Some @Configured types can be converted to ObjectName.
     */
    private static boolean isRemoteableType(final Class<?> clazz)
    {
        // quick check for the 99% case
        if (clazz.isPrimitive() ||
            REMOTABLE.contains(clazz) ||
            CompositeData.class.isAssignableFrom(clazz) ||
            OpenType.class.isAssignableFrom(clazz))
        {
            return true;
        }

        if (clazz.isArray())
        {
            return isRemoteableType(clazz.getComponentType());
        }

        if (Collection.class.isAssignableFrom(clazz))
        {
            // no way to tell, allow it
            return true;
        }

        if (Map.class.isAssignableFrom(clazz))
        {
            // no way to tell, allow it
            return true;
        }

        if (ConfigBeanProxy.class.isAssignableFrom(clazz))
        {
            // represented as an ObjectName
            return true;
        }

        return false;
    }

    private static boolean isRemoteableDuckTyped(final Method m, final DuckTyped duckTyped)
    {
        boolean isRemotable = true;

        final Class<?> returnType = m.getReturnType();
        if (!isRemoteableType(returnType))
        {
            return false;
        }

        final Class<?>[] sig = m.getParameterTypes();
        for (final Class<?> c : sig)
        {
            if (!isRemoteableType(c))
            {
                return false;
            }

            // in an OSGI world, passing an argument of type Class is highly dubious
            if (c == Class.class)
            {
                return false;
            }
        }

        return true;
    }

    private static Class<?> remoteType(final Class<?> clazz)
    {
        if (ConfigBeanProxy.class.isAssignableFrom(clazz))
        {
            return ObjectName.class;
        }

        return clazz;
    }

    private void findStuff(
            final Class<? extends ConfigBeanProxy> intf,
            final List<AttributeMethodInfo> attrs,
            final List<ElementMethodInfo> elements,
            final List<DuckTypedInfo> duckTyped)
    {

        for (final Method m : intf.getMethods())
        {
            AttributeMethodInfo a;
            //debug( "Method: " + m.getName() + " on " + m.getDeclaringClass() );
            if ((a = AttributeMethodInfo.get(m)) != null)
            {
                attrs.add(a);
                if ( a.returnType() != String.class )
                {
                    AMXLoggerInfo.getLogger().log(Level.INFO, AMXLoggerInfo.illegalNonstring, 
                            new Object[]{intf.getName(), m.getName(), a.returnType().getName()});
                }
                continue;
            }

            ElementMethodInfo e;
            if ((e = ElementMethodInfo.get(m)) != null)
            {
                elements.add(e);
                continue;
            }

            final DuckTyped dt = m.getAnnotation(DuckTyped.class);
            if (dt != null && isRemoteableDuckTyped(m, dt))
            {
                duckTyped.add(new DuckTypedInfo(m, dt));
            }
        }
    }
    
    /** check for Bad Stuff in Configured interface. */
    public List<String>  sanityCheckConfigured()
    {
        final List<String> problems = new ArrayList<String>();
        for( final AttributeMethodInfo info : mAttrInfos )
        {
            final Class<?> dataType = info.inferDataType();
            if ( (dataType == Boolean.class || dataType == boolean.class) &&  info.notNull() && ! info.hasDefaultValue()  )
            {
                problems.add( "Missing defaultValue for Boolean @Configured " + mIntf.getName() + ".get" + info.attrName() + "()" );
            }
        }
        if ( problems.size() != 0 )
        {
            System.out.println( CollectionUtil.toString( problems, "\n" ) );
        }
        return problems;
    }

    public static String xmlName(final MBeanAttributeInfo info, final String defaultValue)
    {
        final String value = (String) info.getDescriptor().getFieldValue(DESC_XML_NAME);
        return value == null ? defaultValue : value;
    }

    public static boolean isKey(final MBeanAttributeInfo info)
    {
        return (Boolean) info.getDescriptor().getFieldValue(DESC_KEY);
    }

    public static String defaultValue(final MBeanAttributeInfo info)
    {
        return (String) info.getDescriptor().getFieldValue(DESC_DEFAULT_VALUE);
    }

    /** Return a Map from the Attribute name to the xml name. */
    public Map<String, String> getToXMLNameMapping()
    {
        final Map<String, String> m = new HashMap<String, String>();

        final MBeanInfo info = getMBeanInfo();
        for (final MBeanAttributeInfo attrInfo : info.getAttributes())
        {
            m.put(attrInfo.getName(), xmlName(attrInfo, attrInfo.getName()));
        }

        return m;
    }

    /** Return a Map from the xml  name to the Attribute name. */
    public Map<String, String> getFromXMLNameMapping()
    {
        final Map<String, String> m = new HashMap<String, String>();

        final MBeanInfo info = getMBeanInfo();
        for (final MBeanAttributeInfo attrInfo : info.getAttributes())
        {
            m.put(xmlName(attrInfo, attrInfo.getName()), attrInfo.getName());
        }

        return m;
    }

    public static boolean isAttribute(final MBeanAttributeInfo info)
    {
        final String value = (String) info.getDescriptor().getFieldValue(DESC_KIND);

        return value == null || Attribute.class.getName().equals(value);
    }

    public static boolean isElement(final MBeanAttributeInfo info)
    {
        final String value = (String) info.getDescriptor().getFieldValue(DESC_KIND);

        return Element.class.getName().equals(value);
    }

    public static DescriptorSupport descriptor(final AttributeMethodInfo info)
    {
        final DescriptorSupport d = new DescriptorSupport();
        final Attribute a = info.attribute();

        d.setField(DESC_KIND, Attribute.class.getName());

        if (!a.defaultValue().equals("\u0000"))
        {
            d.setField(DESC_DEFAULT_VALUE, a.defaultValue());
        }

        d.setField(DESC_KEY, a.key());
        d.setField(DESC_REQUIRED, a.required());
        d.setField(DESC_REFERENCE, a.reference());
        d.setField(DESC_VARIABLE_EXPANSION, a.variableExpansion());
        d.setField(DESC_DATA_TYPE, info.inferDataType().getName());

        return d;
    }

    public static DescriptorSupport descriptor(final Element e)
    {
        final DescriptorSupport d = new DescriptorSupport();

        d.setField(DESC_KIND, Element.class.getName());

        d.setField(DESC_KEY, e.key());
        d.setField(DESC_REQUIRED, e.required());
        d.setField(DESC_REFERENCE, e.reference());
        d.setField(DESC_VARIABLE_EXPANSION, e.variableExpansion());

        return d;
    }

    public static DescriptorSupport descriptor(final DuckTyped dt)
    {
        final DescriptorSupport d = new DescriptorSupport();

        d.setField(DESC_KIND, DuckTyped.class.getName());

        return d;
    }


    private DescriptorSupport descriptor()
    {
        final DescriptorSupport d = new DescriptorSupport();

        String amxInterfaceName = AMXConfigProxy.class.getName(); // generic default

        final String intfPackage = mIntf.getPackage().getName();
        if (Domain.class.getPackage().getName().equals(intfPackage))
        {
            amxInterfaceName = mIntf.getName();
        }

        d.setField(DESC_STD_INTERFACE_NAME, amxInterfaceName);
        d.setField(DESC_GENERIC_INTERFACE_NAME, AMXConfigProxy.class.getName());
        d.setField(DESC_STD_IMMUTABLE_INFO, true);
        d.setField(DESC_GROUP, "config");

        // Adoption is not supported, only other config elements
        d.setField(DESC_SUPPORTS_ADOPTION, false);

        d.setField(DESC_IS_SINGLETON, isSingleton());

        final String[] subTypes = CollectionUtil.toArray(childTypes().keySet(), String.class);
        d.setField(DESC_SUB_TYPES, subTypes);

        return d;
    }

    public final Set<String> requiredAttributeNames()
    {
        final Set<String> s = new HashSet<String>();
        for (final AttributeMethodInfo info : mAttrInfos)
        {
            if (info.required())
            {
                s.add(info.attrName());
            }
        }

        for (final ElementMethodInfo info : mElementInfos)
        {
            if (info.required())
            {
                s.add(info.attrName());
            }
        }
        return s;
    }

    /**
    DuckTyped methods are <em>always</em> exposed as operations, never as Attributes.
     */
    public MBeanOperationInfo duckTypedToMBeanOperationInfo(final DuckTypedInfo info)
    {
        final Descriptor descriptor = descriptor(info.duckTyped());

        final String name = info.name();

        final Class<?> type = remoteType(info.returnType());

        final String description = "@DuckTyped " + name + " of " + mIntf.getName();
        final int impact = MBeanOperationInfo.UNKNOWN; // how to tell?

        final List<MBeanParameterInfo> paramInfos = new ArrayList<MBeanParameterInfo>();
        int i = 0;
        for (final Class<?> paramClass : info.signature())
        {
            final String paramName = "p" + i;
            final String paramType = remoteType(paramClass).getName();
            final String paramDescription = "parameter " + i;
            final MBeanParameterInfo paramInfo = new MBeanParameterInfo(paramName, paramType, paramDescription, null);
            paramInfos.add(paramInfo);
            ++i;
        }

        final MBeanParameterInfo[] paramInfosArray = CollectionUtil.toArray(paramInfos, MBeanParameterInfo.class);
        final MBeanOperationInfo opInfo = new MBeanOperationInfo(name, description,
                paramInfosArray, type.getName(), impact, descriptor);
        return opInfo;
    }

    public MBeanOperationInfo[] toMBeanOperationInfos()
    {
        final List<MBeanOperationInfo> opInfos = new ArrayList<MBeanOperationInfo>();

        for (final DuckTypedInfo info : mDuckTypedInfos)
        {
            final MBeanOperationInfo opInfo = duckTypedToMBeanOperationInfo(info);
            if (opInfo != null)
            {
                opInfos.add(opInfo);
            }
        }
        return CollectionUtil.toArray(opInfos, MBeanOperationInfo.class);
    }

    private static final Set<String> IGNORE_ANNOTATION_METHODS = SetUtil.newUnmodifiableStringSet("toString", "hashCode", "annotationType");

    private void addAnnotationsToDescriptor(final Descriptor d, final AttributeMethodInfo info)
    {
        final Annotation[] annotations = info.annotations();

        for (final Annotation a : annotations)
        {
            final String prefix = DESC_ANNOTATION_PREFIX + "@" + a.annotationType().getName() + ":";

            final Method[] values = a.getClass().getDeclaredMethods();
            for (final Method m : values)
            {
                final String fieldName = m.getName();
                if (IGNORE_ANNOTATION_METHODS.contains(fieldName))
                {
                    continue;
                }

                //debug( "INVOKING: " + fieldName + ", returnType = " +  m.getReturnType() );
                if (m.getParameterTypes().length == 0)
                {
                    try
                    {
                        final Object fieldValue = m.invoke(a);
                        // make sure all metadata is safe across the wire: convert to String
                        Object actualValue = fieldValue;
                        if (actualValue != null /* && ! actualValue.getClass().getName().startsWith("java.lang") */)
                        {
                            actualValue = SmartStringifier.toString(actualValue);
                        }
                        d.setField(prefix + fieldName, actualValue);
                    }
                    catch (final Exception e)
                    {
                        AMXLoggerInfo.getLogger().log( Level.INFO, AMXLoggerInfo.cantGetField, 
                                new Object[] {a, e.getLocalizedMessage()} );
                    }
                }
            }
        }
    }

    public MBeanAttributeInfo attributeToMBeanAttributeInfo(final AttributeMethodInfo info)
    {
        final Descriptor descriptor = descriptor(info);

        final String name = info.attrName();
        final String xmlName = info.xmlName();
        descriptor.setField(DESC_XML_NAME, xmlName);
        //debug( m.getName() + " => " + name + " => " + xmlName );

        if (info.pattern() != null)
        {
            descriptor.setField(DESC_PATTERN_REGEX, info.pattern());
        }

        if (info.units() != null)
        {
            descriptor.setField(DESC_UNITS, info.units());
        }

        if (info.min() != null)
        {
            descriptor.setField(DESC_MIN, info.min());
        }

        if (info.max() != null)
        {
            descriptor.setField(DESC_MAX, info.max());
        }

        addAnnotationsToDescriptor(descriptor, info);
        descriptor.setField(DESC_NOT_NULL, "" + info.notNull());

        Class type = info.returnType();
        
        String description = "@Attribute " + name;
        final boolean isReadable = true;
        // we assume that all getters are writeable for now
        final boolean isWriteable = true;
        final boolean isIs = false;
        final MBeanAttributeInfo attrInfo =
                new MBeanAttributeInfo(name, type.getName(), description, isReadable, isWriteable, isIs, descriptor);
        return attrInfo;
    }

    /** An  @Element("*") is anonymous, no specified type, could be anything */
    public static final String ANONYMOUS_SUB_ELEMENT = "*";

    private static abstract class MethodInfo
    {
        protected final Method mMethod;

        protected final String mAttrName;

        protected final String mXMLName;

        MethodInfo(final Method m, final String xmlName)
        {
            mMethod = m;
            mAttrName = JMXUtil.getAttributeName(m);
            mXMLName = xmlName;
        }

        public Method method()
        {
            return mMethod;
        }

        public String attrName()
        {
            return mAttrName;
        }

        public String xmlName()
        {
            return mXMLName;
        }

        public Class<?> returnType()
        {
            return mMethod.getReturnType();
        }

        public abstract boolean required();

        public abstract boolean key();

        /** return ConfigBeanProxy interface, or null if not a ConfigBeanProxy */
        public Class<? extends ConfigBeanProxy> intf()
        {
            final Class returnType = returnType();
            if (ConfigBeanProxy.class.isAssignableFrom(returnType))
            {
                return returnType.asSubclass(ConfigBeanProxy.class);
            }
            return null;
        }

    }

    public static final class ElementMethodInfo extends MethodInfo
    {
        private final Element mElement;

        private ElementMethodInfo(final Method m, final Element e)
        {
            super(m, e.value().length() == 0 ? Util.typeFromName(JMXUtil.getAttributeName(m)) : e.value());
            mElement = e;
        }

        public static ElementMethodInfo get(final Method m)
        {

            final Element e = m.getAnnotation(Element.class);
            return e == null ? null : new ElementMethodInfo(m, e);
        }

        public Element element()
        {
            return mElement;
        }

        public boolean anonymous()
        {
            return ANONYMOUS_SUB_ELEMENT.equals(xmlName());
        }

        public List<Class<? extends ConfigBeanProxy>> anonymousTypes()
        {
            if (!anonymous())
            {
                return null;
            }

            final Class<?> anon = internalReturnType(method());
            if (!ConfigBeanProxy.class.isAssignableFrom(anon))
            {
                return null;
            }
            //System.out.println( "ANONYMOUS ELEMENT LIST: " + anon );
            final Class[] interfaces = getTypesImplementing(anon);

            final List<Class<? extends ConfigBeanProxy>> types = ListUtil.newList();
            for (final Class clazz : interfaces)
            {
                types.add(clazz.asSubclass(ConfigBeanProxy.class));
            }

            return types;
        }

        public boolean required()
        {
            return mElement.required();
        }

        public boolean key()
        {
            return mElement.key();
        }

    }

    /** works only for @Configured types */
    public static Class[] getTypesImplementing(final Class<?> clazz)
    {
        final DomDocument domDoc = new DomDocument(InjectedValues.getInstance().getHabitat());

        try
        {
            final List<ConfigModel> models = domDoc.getAllModelsImplementing(clazz);
            final Class[] interfaces = new Class[models == null ? 0 : models.size()];
            if (models != null)
            {
                int i = 0;
                for (final ConfigModel model : models)
                {
                    final String classname = model.targetTypeName;
                    final Class<?> intf = model.classLoaderHolder.loadClass(classname);
                    interfaces[i] = intf;
                    //System.out.println( "Loaded: " + intf + " with tagName of " + model.getTagName() );
                    ++i;
                }
            }

            return interfaces;
        }
        catch (final Exception e)
        {
            AMXLoggerInfo.getLogger().log( Level.INFO, AMXLoggerInfo.cantGetTypesImplementing, 
                    new Object[] {clazz, e.getLocalizedMessage()} );
            throw new RuntimeException(e);
        }
    }

    private static boolean isIntegral(final String s)
    {
        if (s.equals("0") || s.equals("1"))
        {
            return true;
        }

        try
        {
            Long.parseLong(s);
            return true;
        }
        catch (Exception e)
        {
        }
        return false;
    }

    private static final Map<String, String> UNITS_SUFFIXES = MapUtil.newMap(
            "Millis", Units.MILLISECONDS,
            "Milliseconds", Units.MILLISECONDS,
            "Seconds", Units.SECONDS,
            "Hours", Units.HOURS,
            "Days", Units.DAYS,
            "Bytes", Units.BYTES,
            "Kilobytes", Units.KILOBYTES,
            "Megabytes", Units.MEGABYTES);

    /** Create the default min/max values for primitive types */
    private static Map<Class<?>, long[]> makeMIN_MAX()
    {
        final Map<Class<?>, long[]> m = new HashMap<Class<?>, long[]>();

        long[] mm = new long[]
        {
            Byte.MIN_VALUE, Byte.MAX_VALUE
        };
        m.put(byte.class, mm);
        m.put(Byte.class, mm);

        mm = new long[]
                {
                    Short.MIN_VALUE, Short.MAX_VALUE
                };
        m.put(short.class, mm);
        m.put(Short.class, mm);

        mm = new long[]
                {
                    Integer.MIN_VALUE, Integer.MAX_VALUE
                };
        m.put(int.class, mm);
        m.put(Integer.class, mm);

        mm = new long[]
                {
                    Long.MIN_VALUE, Long.MAX_VALUE
                };
        m.put(long.class, mm);
        m.put(Long.class, mm);

        /*
        m.put(PositiveInteger.class, new long[] { 1, Integer.MAX_VALUE } );
        m.put(NonNegativeInteger.class, new long[] { 0, Integer.MAX_VALUE } );
        m.put(Port.class, new long[] { 0, 65535 } );
         */

        return m;
    }

    private static final Map<Class<?>, long[]> MIN_MAX = makeMIN_MAX();

    private static long[] minMaxFromDataType(final Class<?> dataType)
    {
        return MIN_MAX.get(dataType);
    }

    public static final class AttributeMethodInfo extends MethodInfo
    {
        private final Attribute mAttribute;

        private AttributeMethodInfo(final Method m, final Attribute a)
        {
            super(m, a.value().length() == 0 ? Util.typeFromName(JMXUtil.getAttributeName(m)) : a.value());
            mAttribute = a;
        }

        public static AttributeMethodInfo get(final Method m)
        {
            final Attribute a = m.getAnnotation(Attribute.class);
            return a == null ? null : new AttributeMethodInfo(m, a);
        }

        public Attribute attribute()
        {
            return mAttribute;
        }

        public boolean required()
        {
            return mAttribute.required();
        }

        public boolean key()
        {
            return mAttribute.key();
        }

        public String pattern()
        {
            final javax.validation.constraints.Pattern pat = mMethod.getAnnotation(javax.validation.constraints.Pattern.class);
            return pat == null ? null : pat.regexp();
        }

        public String units()
        {
            final Units units = mMethod.getAnnotation(Units.class);
            return units == null ? inferUnits() : units.units();
        }

        public Long min()
        {
            final Min min = mMethod.getAnnotation(Min.class);
            if (min != null)
            {
                return min.value();
            }
            final long[] minMax = minMaxFromDataType(attribute().dataType());
            return minMax == null ? null : minMax[0];
        }

        public Long max()
        {
            final Max max = mMethod.getAnnotation(Max.class);
            if (max != null)
            {
                return max.value();
            }
            final long[] minMax = minMaxFromDataType(attribute().dataType());
            return minMax == null ? null : minMax[1];
        }

        /** infer the data type, using specified value if present */
        public String inferUnits()
        {
            if (Number.class.isAssignableFrom(inferDataType()))
            {
                final String attrName = attrName();
                for (final String key : UNITS_SUFFIXES.keySet())
                {
                    if (attrName.endsWith(key))
                    {
                        return UNITS_SUFFIXES.get(key);
                    }
                }
                return Units.COUNT;
            }
            return null;
        }
        
        public boolean hasDefaultValue()
        {
            final Object defaultValue = attribute().defaultValue();
            return defaultValue != null && ! defaultValue.equals( "\u0000" );
        }

        /** infer the data type, using specified value if present */
        public Class<?> inferDataType()
        {
            Class<?> dataType = attribute().dataType();
            if (dataType != String.class)
            {
                //  explicitly specified as non-String, use it
                return dataType;
            }

            // infer a Boolean, strictly "true" or "false"
            final Object defaultValue = attribute().defaultValue();
            if (defaultValue.equals("true") || defaultValue.equals("false"))
            {
                return Boolean.class;
            }

            // infer a number
            if (max() != null)
            {
                if (max().equals(Long.MAX_VALUE))
                {
                    return Long.class;
                }
                else
                {
                    return Integer.class;
                }
            }
            else if (min() != null)
            {
                return min().equals(Long.MIN_VALUE) ? Long.class : Integer.class;
            }
            else if (isIntegral("" + defaultValue))
            {
                return Long.class;
            }

            return dataType;
        }

        public boolean notNull()
        {
            final NotNull n = mMethod.getAnnotation(NotNull.class);
            return n != null;
        }

        public Annotation[] annotations()
        {
            return method().getAnnotations();
        }

    }

    public static final class DuckTypedInfo
    {
        private final DuckTyped mDuckTyped;

        private final Method mMethod;

        DuckTypedInfo(final Method m, final DuckTyped duckTyped)
        {
            mMethod = m;
            mDuckTyped = duckTyped;
        }

        public DuckTyped duckTyped()
        {
            return mDuckTyped;
        }

        public String name()
        {
            return mMethod.getName();
        }

        public Class<?> duck()
        {
            return mMethod.getDeclaringClass();
        }

        public Method method()
        {
            return mMethod;
        }

        public Class<?> returnType()
        {
            return method().getReturnType();
        }

        public boolean isPseudoAttribute()
        {
            return name().startsWith("get") || name().startsWith("is") && signature().length == 0;
        }

        public Class<?>[] signature()
        {
            return method().getParameterTypes();
        }

        public String toString()
        {
            String paramsString = "";
            final Class<?>[] paramTypes = signature();
            if (paramTypes.length != 0)
            {
                final StringBuilder builder = new StringBuilder();
                final String delim = ", ";
                for (final Class<?> paramClass : method().getParameterTypes())
                {
                    builder.append(ClassUtil.stripPackageName(paramClass.getName()) + delim);
                }
                builder.setLength(builder.length() - delim.length());
                paramsString = builder.toString();
            }

            return ClassUtil.stripPackageName(mMethod.getReturnType().getName()) + " " +
                   duck().getName() + "." + mMethod.getName() + "(" + paramsString + ")";
        }

    }

    /**
    Get the child types, excluding String[] and anonymous.
     */
    public Set<Class<? extends ConfigBeanProxy>> childInterfaces()
    {
        final Set<Class<? extends ConfigBeanProxy>> intfs = childInterfaces(mElementInfos);
        return intfs;
    }

    private static Class<?> internalReturnType(final Method method)
    {
        Class returnType = method.getReturnType();

        try
        {
            if (Collection.class.isAssignableFrom(returnType))
            {
                final Type genericReturnType = method.getGenericReturnType();
                if (genericReturnType instanceof ParameterizedType)
                {
                    final ParameterizedType pt = (ParameterizedType) genericReturnType;
                    final Type[] argTypes = pt.getActualTypeArguments();
                    if (argTypes.length == 1)
                    {
                        final Type argType = argTypes[0];
                        if (argType instanceof Class)
                        {
                            returnType = (Class) argType;
                        }
                        else
                        {
                            throw new IllegalArgumentException();
                        }
                    }
                }
            }
        }
        catch (final Exception e)
        {
            System.out.println("AMX ConfigBeanAMXSupport: can't get generic return type for method " +
                               method.getDeclaringClass().getName() + "." + method.getName() + "(): " + e.getClass().getName() + " = " + e.getMessage());
        }

        return returnType;
    }

    /**
    Find a matching ElementMethodInfo by class.
     */
    public ElementMethodInfo getElementMethodInfo(final Class<? extends ConfigBeanProxy> intf)
    {
        ElementMethodInfo match = null;
        for (final ElementMethodInfo info : mElementInfos)
        {
            if (internalReturnType(info.method()) == intf)
            {
                match = info;
                break;
            }
        }
        if ( match == null )
        {
            // could be somethign generic, list List<Resource>
            for (final ElementMethodInfo info : mElementInfos)
            {
                if ( internalReturnType(info.method()).isAssignableFrom(intf) )
                {
                    match = info;
                    break;
                }
            }
        }
        return match;
    }

    public Map<String, Class<? extends ConfigBeanProxy>> childTypes()
    {
        final Map<String, Class<? extends ConfigBeanProxy>> types = new HashMap<String, Class<? extends ConfigBeanProxy>>();
        for (final Class<? extends ConfigBeanProxy> intf : childInterfaces())
        {
            types.put(getTypeString(intf), intf);
        }
        return types;
    }

    public Class<? extends ConfigBeanProxy> getConfigBeanProxyClassFor(final String type, final boolean recursive)
    {
        return childTypes().get(type);
    }

    public Set<Class<? extends ConfigBeanProxy>> childInterfaces(final List<ElementMethodInfo> infos)
    {
        final Set<Class<? extends ConfigBeanProxy>> classes = new HashSet<Class<? extends ConfigBeanProxy>>();

        for (final ElementMethodInfo info : infos)
        {
            if (info.anonymous())
            {
                final List<Class<? extends ConfigBeanProxy>> types = info.anonymousTypes();
                if (types != null)
                {
                    classes.addAll(types);
                }
            }
            else
            {
                final Class methodReturnType = info.returnType();

                Class<? extends ConfigBeanProxy> intf = null;
                if (info.intf() != null)
                {
                    intf = info.intf();
                }
                else if (Collection.class.isAssignableFrom(methodReturnType))
                {
                    final Type genericReturnType = info.method().getGenericReturnType();
                    if (genericReturnType instanceof ParameterizedType)
                    {
                        final ParameterizedType pt = (ParameterizedType) genericReturnType;
                        final Type[] argTypes = pt.getActualTypeArguments();
                        if (argTypes.length == 1)
                        {
                            final Type argType = argTypes[0];
                            if ((argType instanceof Class) && (Class) argType == String.class)
                            {
                                // ignore for our purposes here
                            }
                            else
                            {
                                intf = ((Class) argType).asSubclass(ConfigBeanProxy.class);
                            }
                        }
                    }
                }
                if (intf != null)
                {
                    classes.add(intf);
                }
            }
        }

        return classes;
    }

    /**
    @Elements are represented as Attributes:  getters for sub-elements are presented
    as ObjectName, Collection<String> presented as String[], Collection<? extends ConfigBeanProxy>
    represented as ObjectName[].
     */
    public MBeanAttributeInfo elementToMBeanAttributeInfo(final Method m)
    {
        // we assume that all getters are writeable for now, not true for sub-elements (ObjectName)
        boolean isWriteable = true;
        
        final ElementMethodInfo info = ElementMethodInfo.get(m);
        if (info == null || info.anonymous())
        {
            return null;
        }

        final String name = info.attrName();    // eg strip the "get"
        final String xmlName = info.xmlName();
        //debug( m.getName() + " => " + name + " => " + xmlName );

        final Class methodReturnType = info.returnType();
        Class<?> returnType = null;

        if (info.intf() != null)
        {
            // some sub-type, which we must represent as an ObjectName
            returnType = ObjectName.class;
            isWriteable = false;
        }
        else if (Collection.class.isAssignableFrom(methodReturnType))
        {
            final Type genericReturnType = m.getGenericReturnType();
            if (genericReturnType instanceof ParameterizedType)
            {
                final ParameterizedType pt = (ParameterizedType) genericReturnType;
                final Type[] argTypes = pt.getActualTypeArguments();
                if (argTypes.length == 1)
                {
                    final Type argType = argTypes[0];
                    if ((argType instanceof Class) && (Class) argType == String.class)
                    {
                        returnType = String[].class;
                    }
                    else
                    {
                        returnType = ObjectName[].class;
                        isWriteable = false;
                    }
                }
            }
        }
        else
        {
            // some unknown type we cannot handle
        }

        MBeanAttributeInfo attrInfo = null;
        if (returnType != null)
        {
            final DescriptorSupport descriptor = descriptor(info.element());

            descriptor.setField(DESC_ELEMENT_CLASS, returnType.getName());
            descriptor.setField(DESC_XML_NAME, xmlName);

            final ToDo toDo = info.method().getAnnotation(ToDo.class);
            if (toDo != null)
            {
                descriptor.setField(DESC_CONFIG_PREFIX + "toDo", toDo.priority() + ", " + toDo.details());
            }

            final PropertiesDesc props = info.method().getAnnotation(PropertiesDesc.class);
            if (props != null)
            {
                final String propType = props.systemProperties() ? "system-property" : "property";
                for (final PropertyDesc p : props.props())
                {
                    final String value = p.defaultValue() + " | " + p.dataType().getName() + " | " + p.description();
                    descriptor.setField(DESC_CONFIG_PREFIX + propType + "." + p.name(), value);
                }
            }

            String description = "@Element " + name + " of interface " + mIntf.getName();
            final boolean isReadable = true;
            final boolean isIs = false;
            attrInfo = new MBeanAttributeInfo(name, returnType.getName(), description, isReadable, isWriteable, isIs, descriptor);
        }

        return attrInfo;
    }

    public String getNameHint()
    {
        return mNameHint.mHint;
    }

    public boolean nameHintIsElement()
    {
        return mNameHint.mIsElement;
    }

    public static String toXMLName(final String name)
    {
        return name == null ? name : Dom.convertName(name);
    }

    private final static String DEFAULT_NAME_HINT = "name";

    private static final class NameHint
    {
        public static final NameHint NAME = new NameHint(DEFAULT_NAME_HINT);

        public static final NameHint NONE = new NameHint(null);

        private final String mHint;

        private final boolean mIsElement;

        public NameHint(final String hint, final boolean isElement)
        {
            mHint = toXMLName(hint);
            mIsElement = isElement;
        }

        public NameHint(final String hint)
        {
            this(hint, false);
        }

    }

    /**
    Return the name of the XML attribute which contains the value to be used as its name.
    First element is the name hint, 2nd indicates its type
     */
    private NameHint findNameHint()
    {
        if (isSingleton())
        {
            return NameHint.NONE;
        }

        if (mKey != null)
        {
            return new NameHint(mKey);
        }

        // final String configuredBugKey = configuredBugKey();

        for (final AttributeMethodInfo info : mAttrInfos)
        {

            if (info.key())
            {
                //debug( "findNameHint: mKey = " + mKey + ", info says " + info.xmlName() );
                return new NameHint(info.xmlName());
            }
            /*
            else if (configuredBugKey != null && info.attrName().equalsIgnoreCase(configuredBugKey)) {
            //debug( "findNameHint: mKey = " + mKey + ", workaround says " + configuredBugKey );
            return new NameHint(configuredBugKey);
            }
             */
        }

        /**
        Is this possible?
        for (final ElementMethodInfo info : mElements.values()) {
        if (info.getElement().key()) {
        return new NameHint(info.getName(), true);
        }

        }
         */
        return NameHint.NAME;
    }

    public Map<String, String> getDefaultValues(final boolean useAttributeNames)
    {
        final Map<String, String> m = new HashMap<String, String>();

        final MBeanInfo info = getMBeanInfo();
        for (final MBeanAttributeInfo attrInfo : info.getAttributes())
        {
            final String defaultValue = defaultValue(attrInfo);

            // emit values that exist (only); null is of no use.
            if (defaultValue != null)
            {
                final String attrName = attrInfo.getName();
                final String name = useAttributeNames ? attrName : xmlName(attrInfo, attrName);
                m.put(name, defaultValue);
            }
        }
        return m;
    }

    private static void debug(final String s)
    {
        System.out.println("### " + s);
    }

}

























