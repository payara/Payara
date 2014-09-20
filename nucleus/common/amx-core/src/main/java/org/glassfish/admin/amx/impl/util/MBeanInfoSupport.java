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
package org.glassfish.admin.amx.impl.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import javax.management.Descriptor;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.ObjectName;
import javax.management.modelmbean.DescriptorSupport;
import org.glassfish.admin.amx.annotation.ManagedAttribute;
import org.glassfish.admin.amx.annotation.ManagedOperation;
import org.glassfish.admin.amx.annotation.Description;
import org.glassfish.admin.amx.annotation.Param;
import org.glassfish.admin.amx.base.Singleton;
import org.glassfish.admin.amx.core.AMXProxy;
import org.glassfish.admin.amx.core.AMX_SPI;
import org.glassfish.admin.amx.util.jmx.JMXUtil;
import org.glassfish.admin.amx.core.AMXMBeanMetadata;
import org.glassfish.admin.amx.util.AMXLoggerInfo;
import static org.glassfish.external.amx.AMX.*;

/**
 *
 * @author llc
 */
public final class MBeanInfoSupport {

    private MBeanInfoSupport() {
    }

    private static void debug(final Object o) {
        System.out.println(o.toString());
    }
    private static MBeanInfo amxspiMBeanInfo = null;

    public static synchronized MBeanInfo getAMX_SPIMBeanInfo() {
        if (amxspiMBeanInfo == null) {
            amxspiMBeanInfo = MBeanInfoSupport.getMBeanInfo(AMX_SPI.class);
        }
        return amxspiMBeanInfo;
    }

    public static <T extends AMX_SPI> MBeanInfo getMBeanInfo(final Class<T> intf) {
        final Map<String, Method> getters = new HashMap<String, Method>();
        final Map<String, Method> setters = new HashMap<String, Method>();
        final Map<String, Method> getterSetters = new HashMap<String, Method>();
        final Set<Method> operations = new HashSet<Method>();

        findInterfaceMethods(intf, getters, setters, getterSetters, operations);

        if (!AMX_SPI.class.isAssignableFrom(intf)) {
            findInterfaceMethods(AMX_SPI.class, getters, setters, getterSetters, operations);
        }


        final List<MBeanAttributeInfo> attrsList =
                generateMBeanAttributeInfos(getterSetters.values(), getters.values(), setters.values());

        final MBeanOperationInfo[] operationInfos = generateMBeanOperationInfos(operations);

        // might or might not have metadata
        final AMXMBeanMetadata meta = intf.getAnnotation(AMXMBeanMetadata.class);

        final boolean globalSingleton = meta != null && meta.globalSingleton();
        final boolean singleton = Singleton.class.isAssignableFrom(intf) || globalSingleton ||
                (meta != null && meta.singleton());
        final String group = GROUP_OTHER;
        final boolean isLeaf = meta != null && meta.leaf();
        final boolean supportsAdoption = !isLeaf;

        if (isLeaf) {
            JMXUtil.remove(attrsList, ATTR_CHILDREN);
        }

        final Descriptor d = mbeanDescriptor(
                true,
                intf,
                singleton,
                globalSingleton,
                group,
                supportsAdoption,
                null);



        final MBeanAttributeInfo[] attrInfos = new MBeanAttributeInfo[attrsList.size()];
        attrsList.toArray(attrInfos);

        final MBeanInfo mbeanInfo = new MBeanInfo(
                intf.getName(),
                intf.getName(),
                attrInfos,
                null,
                operationInfos,
                null,
                d);
        //debug( "MBeanInfoSupport.getMBeanInfo(): " + mbeanInfo );

        return (mbeanInfo);
    }

    public static void findInterfaceMethods(final Class<?> intf,
            final Map<String, Method> getters,
            final Map<String, Method> setters,
            final Map<String, Method> getterSetters,
            final Set<Method> operations) {
        final Method[] methods = intf.getMethods();

        for (final Method method : methods) {
            final ManagedAttribute managedAttr = method.getAnnotation(ManagedAttribute.class);
            final ManagedOperation managedOp = method.getAnnotation(ManagedOperation.class);

            if (managedAttr != null) {
                String attrName = null;
                final int numArgs = method.getParameterTypes().length;
                if (managedOp != null) {
                    AMXLoggerInfo.getLogger().log(Level.WARNING, AMXLoggerInfo.attributeCantBeOperation, 
                            new Object[]{intf.getName(), method.getName()});
                } else if (numArgs == 0 && JMXUtil.isIsOrGetter(method)) {
                    attrName = JMXUtil.getAttributeName(method);
                    getters.put(attrName, method);
                    //debug( "findInterfaceMethods: getter: " + attrName );
                } else if (numArgs == 1 && JMXUtil.isSetter(method)) {
                    attrName = JMXUtil.getAttributeName(method);
                    setters.put(attrName, method);
                    //debug( "findInterfaceMethods: setter: " + attrName );
                } else {
                    AMXLoggerInfo.getLogger().log(Level.WARNING, AMXLoggerInfo.attributeNotGetterSetter, 
                            new Object[]{intf.getName(), method.getName()});
                    // ignore
                }

                if ((attrName != null) &&
                        getters.containsKey(attrName) &&
                        setters.containsKey(attrName)) {
                    final Method getter = getters.get(attrName);
                    final Class<?> getterType = getter.getReturnType();
                    final Class<?> setterType = setters.get(attrName).getParameterTypes()[ 0];

                    if (getterType == setterType) {
                        getters.remove(attrName);
                        setters.remove(attrName);
                        getterSetters.put(attrName, getter);
                        //debug( "findInterfaceMethods: getter/setter: " + attrName );
                    } else {
                        throw new IllegalArgumentException("Attribute " + attrName +
                                "has type " + getterType.getName() + " as getter but type " +
                                setterType.getName() + " as setter");
                    }
                }
            } else if (managedOp != null) {
                operations.add(method);
            }
        }

        /*
        java.util.Iterator	iter	= null;
        trace( "-------------------- getterSetters -------------------" );
        iter	= getterSetters.values().iterator();
        while ( iter.hasNext() )
        {
        trace( ((Method)iter.next()).getNameProp() + ", " );
        }
        trace( "-------------------- getters -------------------" );
        iter	= getters.values().iterator();
        while ( iter.hasNext() )
        {
        trace( ((Method)iter.next()).getNameProp() + ", " );
        }
        trace( "-------------------- setters -------------------" );
        iter	= setters.values().iterator();
        while ( iter.hasNext() )
        {
        trace( ((Method)iter.next()).getNameProp() + ", " );
        }
         */
    }

    /**
     * Return MBeanAttributeInfo for the method.  If it's a getter,
     * it's marked as read-only, if it's a setter, it's marked as read/write.
     * @param m
     * @return
     */
    public static MBeanAttributeInfo attributeInfo(final Method m) {
        final ManagedAttribute managed = m.getAnnotation(ManagedAttribute.class);
        if (managed == null) {
            return null;
        }

        final Description d = m.getAnnotation(Description.class);
        final String description = d == null ? "" : d.value();

        String attrName = JMXUtil.getAttributeName(m);
        final boolean isGetter = JMXUtil.isGetter(m);
        final boolean isSetter = JMXUtil.isSetter(m);
        final boolean isIs = JMXUtil.isIs(m);

        final MBeanAttributeInfo info =
                new MBeanAttributeInfo(attrName, m.getReturnType().getName(),
                description, isGetter, isSetter, isIs, null);

        return info;
    }

    public static Class<?> translatedType(final Class<?> clazz) {
        Class<?> type = clazz;
        if (AMXProxy.class.isAssignableFrom(clazz)) {
            type = ObjectName.class;
        } else if (clazz.isArray() && AMXProxy.class.isAssignableFrom(clazz.getComponentType())) {
            type = ObjectName[].class;
        }

        return type;
    }

    private static List<MBeanAttributeInfo> generateAttributeInfos(
            final Collection<Method> methods,
            final boolean read,
            final boolean write) {
        final List<MBeanAttributeInfo> infos = new ArrayList<MBeanAttributeInfo>();

        for (final Method m : methods) {
            final String description = getDescription(m);

            String attrName = JMXUtil.getAttributeName(m);
            final MBeanAttributeInfo info = new MBeanAttributeInfo(
                    attrName,
                    translatedType(m.getReturnType()).getName(),
                    description,
                    read,
                    write,
                    JMXUtil.isIs(m));
            infos.add(info);
            //debug( "Added MBeanAttributeInfo for: " + attrName );
        }

        return (infos);
    }

    public static List<MBeanAttributeInfo> generateMBeanAttributeInfos(
            final Collection<Method> getterSetters,
            final Collection<Method> getters,
            final Collection<Method> setters) {
        final List<MBeanAttributeInfo> attrsList = new ArrayList<MBeanAttributeInfo>();

        attrsList.addAll(generateAttributeInfos(getterSetters, true, true));
        attrsList.addAll(generateAttributeInfos(getters, true, false));
        attrsList.addAll(generateAttributeInfos(setters, false, true));

        return (attrsList);
    }

    public static <T extends Annotation> T getAnnotation(final Annotation[] annotations, final Class<T> clazz) {
        T result = null;

        for (final Annotation a : annotations) {
            if (a.annotationType() == clazz) {
                result = (T) a;
                break;
            }
        }
        return result;
    }

    private static String getDescription(final AnnotatedElement o) {
        final Description d = o.getAnnotation(Description.class);
        return d == null ? "" : d.value();
    }

    public static MBeanParameterInfo[] parameterInfos(final Method method) {
        final Class<?>[] sig = method.getParameterTypes();
        final Annotation[][] paramAnnotations = method.getParameterAnnotations();

        final MBeanParameterInfo[] infos = new MBeanParameterInfo[sig.length];

        for (int i = 0; i < sig.length; ++i) {
            final Class<?> paramClass = translatedType(sig[i]);
            final Annotation[] annotations = paramAnnotations[i];

            final Param p = getAnnotation(annotations, Param.class);
            final String paramName = (p == null || p.name().length() == 0) ? ("p" + i) : p.name();

            final Description d = getAnnotation(annotations, Description.class);
            String description = "";
            if (d != null && d.value().length() != 0) {
                description = d.value();
            }

            final String type = paramClass.getName();

            final MBeanParameterInfo info = new MBeanParameterInfo(paramName, type, description);
            infos[i] = info;
        }

        return (infos);
    }

    public static MBeanOperationInfo[] generateMBeanOperationInfos(final Collection<Method> methods) {
        final MBeanOperationInfo[] infos = new MBeanOperationInfo[methods.size()];

        int i = 0;
        for (final Method m : methods) {
            final ManagedOperation managed = m.getAnnotation(ManagedOperation.class);

            final String methodName = m.getName();
            final MBeanParameterInfo[] parameterInfos = parameterInfos(m);
            final int impact = managed == null ? MBeanOperationInfo.UNKNOWN : managed.impact();
            final String description = getDescription(m);

            final MBeanOperationInfo info = new MBeanOperationInfo(
                    methodName,
                    description,
                    parameterInfos,
                    translatedType(m.getReturnType()).getName(),
                    impact,
                    null);

            infos[i] = info;
            ++i;
        }

        return (infos);
    }

    public static DescriptorSupport mbeanDescriptor(
            final boolean immutable,
            final Class<?> intf,
            final boolean singleton,
            final boolean globalSingleton,
            final String group,
            final boolean supportsAdoption,
            final String[] subTypes) {
        final DescriptorSupport desc = new DescriptorSupport();

        if (intf == null || !intf.isInterface()) {
            throw new IllegalArgumentException("interface class must be an interface");
        }

        desc.setField(DESC_STD_IMMUTABLE_INFO, immutable);
        desc.setField(DESC_STD_INTERFACE_NAME, intf.getName());
        desc.setField(DESC_IS_SINGLETON, singleton);
        desc.setField(DESC_IS_GLOBAL_SINGLETON, globalSingleton);
        desc.setField(DESC_GROUP, group);
        desc.setField(DESC_SUPPORTS_ADOPTION, supportsAdoption);

        if (subTypes != null) {
            desc.setField(DESC_SUB_TYPES, subTypes);
        }

        return desc;
    }
}


