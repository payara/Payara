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

package org.glassfish.admin.amx.util.jmx;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Pattern;
import javax.management.*;
import javax.management.modelmbean.DescriptorSupport;
import javax.management.openmbean.OpenMBeanAttributeInfo;
import org.glassfish.admin.amx.util.ArrayConversion;
import org.glassfish.admin.amx.util.ArrayUtil;
import org.glassfish.admin.amx.util.MapUtil;
import org.glassfish.admin.amx.util.RegexUtil;
import org.glassfish.admin.amx.util.SetUtil;
import org.glassfish.admin.amx.util.StringUtil;
import org.glassfish.admin.amx.util.TypeCast;
import org.glassfish.admin.amx.util.stringifier.SmartStringifier;

/**
 */
public final class JMXUtil
{
    public static String toString(final ObjectName objectName)
    {
        // we can be smarter about the ordering later
        return "" + objectName;
    }

    public final static String MBEAN_SERVER_DELEGATE =
            "JMImplementation:type=MBeanServerDelegate";

    public static MBeanServerDelegateMBean getMBeanServerDelegateMBean(final MBeanServerConnection server)
    {
        final MBeanServerDelegateMBean delegate = newProxyInstance(server, newObjectName(MBEAN_SERVER_DELEGATE), MBeanServerDelegateMBean.class);
        return delegate;
    }

    /** Create a new proxy supporting Notifications<p>
    Type-safe; in JDK 5 generics aren't used */
    public static <T> T newProxyInstance(final MBeanServerConnection conn, final ObjectName objectName, final Class<T> clazz)
    {
        return newProxyInstance(conn, objectName, clazz, true);
    }

    /** Type-safe; in JDK 5 generics aren't used */
    public static <T> T newProxyInstance(final MBeanServerConnection conn, final ObjectName objectName, final Class<T> clazz, boolean notificationBroadcaster)
    {
        return clazz.cast(MBeanServerInvocationHandler.newProxyInstance(conn, objectName, clazz, notificationBroadcaster));
    }

    public final static String MBEAN_SERVER_ID_ATTRIBUTE_NAME =
            "MBeanServerId";

    /**
    The wilcard property at the end of an ObjectName which indicates
    that it's an ObjectName pattern.
     */
    public final static String WILD_PROP = ",*";

    /**
    The wilcard property at the end of an ObjectName which indicates
    that all properties should be matched.
     */
    public final static String WILD_ALL = "*";

    public static ObjectName getMBeanServerDelegateObjectName()
    {
        return (newObjectName("JMImplementation:type=MBeanServerDelegate"));
    }

    public static String getMBeanServerDelegateInfo(final MBeanServer server)
    {
        final MBeanServerDelegateMBean delegate = getMBeanServerDelegateMBean(server);
        final String mbeanServerInfo = "MBeanServerDelegate: {" +
                                       "MBeanServerId = " + delegate.getMBeanServerId() +
                                       ", ImplementationMame = " + delegate.getImplementationName() +
                                       ", ImplementationVendor = " + delegate.getImplementationVendor() +
                                       ", ImplementationVersion = " + delegate.getImplementationVersion() +
                                       ", SpecificationName = " + delegate.getSpecificationName() +
                                       ", SpecificationVendor = " + delegate.getSpecificationVendor() +
                                       ", SpecificationVersion = " + delegate.getSpecificationVersion() +
                                       " }";
        return mbeanServerInfo;
    }

    public static void listenToMBeanServerDelegate(
            final MBeanServerConnection conn,
            final NotificationListener listener,
            final NotificationFilter filter,
            final Object handback)
            throws IOException, InstanceNotFoundException
    {
        conn.addNotificationListener(
                getMBeanServerDelegateObjectName(), listener, filter, handback);
    }

    public static String getMBeanServerID(final MBeanServerConnection conn)
            throws IOException,
                   ReflectionException, InstanceNotFoundException, AttributeNotFoundException,
                   MBeanException
    {
        return ((String) conn.getAttribute(getMBeanServerDelegateObjectName(),
                MBEAN_SERVER_ID_ATTRIBUTE_NAME));
    }

    /**
    Create a new ObjectName, caller is guaranteeing that the name is
    well-formed (a RuntimeException will be thrown if not). This avoids
    having to catch all sorts of JMX exceptions.
    <p>
    <b>Do not call this method if there is not 100% certainty of a well-formed name.</b>
     */
    public static ObjectName newObjectName(final String name)
    {
        try
        {
            return (new ObjectName(name));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static ObjectName newObjectName(
            final ObjectName objectName,
            final String props)
    {
        final String domain = objectName.getDomain();
        final String existingProps = objectName.getKeyPropertyListString();
        final String allProps = concatenateProps(existingProps, props);
        return (newObjectName(domain, allProps));
    }

    public static ObjectName newObjectName(
            final String domain,
            final String props)
    {
        return (newObjectName(domain + ":" + props));
    }

    /**
    Build an ObjectName pattern.

    @param domain	the JMX domain
    @param props	properties of the ObjectName
     */
    public static ObjectName newObjectNamePattern(
            final String domain,
            final String props)
    {
        String actualProps = null;

        if (props.endsWith(JMXUtil.WILD_PROP) ||
            props.equals(JMXUtil.WILD_ALL))
        {
            actualProps = props;
        }
        else if (props.length() == 0)
        {
            actualProps = "*";
        }
        else
        {
            actualProps = props + WILD_PROP;
        }

        return (newObjectName(domain + ":" + actualProps));
    }

    /**
    Build an ObjectName pattern.

    @param domain	the JMX domain
    @param props	properties of the ObjectName
     */
    public static ObjectName newObjectNamePattern(
            final String domain,
            final Map<String, String> props)
    {
        final String propsString = mapToProps(props);

        return (JMXUtil.newObjectNamePattern(domain, propsString));
    }

    public static String mapToProps(final Map<String, String> propsMap)
    {
        return (MapUtil.toString(propsMap, ","));
    }

    public static ObjectName removeProperty(
            final ObjectName objectName,
            final String key)
    {
        ObjectName nameWithoutKey = objectName;

        if (objectName.getKeyProperty(key) != null)
        {
            final String domain = objectName.getDomain();
            final Hashtable<String, String> props =
                    TypeCast.asHashtable(objectName.getKeyPropertyList());

            props.remove(key);

            if (objectName.isPropertyPattern())
            {
                nameWithoutKey = newObjectNamePattern(domain,
                        nameWithoutKey.getKeyPropertyListString());
            }
            else
            {
                try
                {
                    nameWithoutKey = new ObjectName(domain, props);
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }
        }

        return (nameWithoutKey);
    }

    private JMXUtil()
    {
        // disallow;
    }

    public static final String GET = "get";

    public static final String SET = "set";

    public static final String IS = "is";

    public static String makeProp(String name, String value)
    {
        return (name + "=" + value);
    }

    public static String concatenateProps(String props1, String props2)
    {
        String result;

        if (props1.length() == 0)
        {
            result = props2;
        }
        else if (props2.length() == 0)
        {
            result = props1;
        }
        else
        {
            result = props1 + "," + props2;
        }

        return (result);
    }

    public static String concatenateProps(String props1, String props2, String props3)
    {
        return (concatenateProps(concatenateProps(props1, props2), props3));
    }

    /**
    Convert a Set of ObjectName into an array

    @param objectNameSet	a Set of ObjectName
    @return an ObjectName[]
     */
    public static ObjectName[] objectNameSetToArray(final Set<ObjectName> objectNameSet)
    {
        final ObjectName[] objectNames = new ObjectName[objectNameSet.size()];
        objectNameSet.toArray(objectNames);

        return (objectNames);
    }

    /**
    @param key	the property name, within the ObjectName
    @param objectNames
    @return values from each ObjectName
     */
    public static String[] getKeyProperty(String key, ObjectName[] objectNames)
    {
        final String[] values = new String[objectNames.length];

        for (int i = 0; i < objectNames.length; ++i)
        {
            values[i] = objectNames[i].getKeyProperty(key);
        }

        return (values);
    }

    /**
    @param objectName
    @param key
    @return an ObjectName property with the specified key
     */
    public static String getProp(
            final ObjectName objectName,
            final String key)
    {
        final String value = objectName.getKeyProperty(key);
        if (value == null)
        {
            return (null);
        }

        return (makeProp(key, value));
    }

    public static String getProps(
            final ObjectName objectName,
            final Set<String> propKeys)
    {
        return (getProps(objectName, propKeys, false));
    }

    public static String getProps(
            final ObjectName objectName,
            final Set<String> propKeys,
            final boolean ignoreMissing)
    {
        String props = "";

        final Iterator iter = propKeys.iterator();
        while (iter.hasNext())
        {
            final String key = (String) iter.next();

            final String pair = getProp(objectName, key);
            if (pair != null)
            {
                props = concatenateProps(props, pair);
            }
            else if (!ignoreMissing)
            {
                throw new IllegalArgumentException(
                        "key not found: " + key + " in " + objectName);
            }
        }
        return (props);
    }

    /**
    @param key	the property name, within the ObjectName
    @param objectNameSet
    @return values from each ObjectName
     */
    public static String[] getKeyProperty(String key, Set<ObjectName> objectNameSet)
    {
        final ObjectName[] objectNames =
                JMXUtil.objectNameSetToArray(objectNameSet);

        return (getKeyProperty(key, objectNames));
    }

    /**
    @param key	the property name, within the ObjectName
    @param objectNameSet
    @return values from each ObjectName
     */
    public static Set<String> getKeyPropertySet(String key, Set<ObjectName> objectNameSet)
    {
        final ObjectName[] objectNames =
                JMXUtil.objectNameSetToArray(objectNameSet);

        final String[] values = getKeyProperty(key, objectNames);

        return (ArrayConversion.arrayToSet(values));
    }

    /**
    Find the first key that is present in the ObjectName

    @param candidateKeys
    @param objectName
    @return first key present in the ObjectName
     */
    public static String findKey(
            final Set<String> candidateKeys,
            final ObjectName objectName)
    {
        final Iterator iter = candidateKeys.iterator();

        String match = null;

        while (iter.hasNext())
        {
            final String key = (String) iter.next();

            if (objectName.getKeyProperty(key) != null)
            {
                match = key;
                break;
            }
        }

        return (match);
    }

    /**
    Find all ObjectName(s) that contains the associated key and value

    @param objectNames
    @param propertyKey
    @param propertyValue
    @return Set of all ObjectName that match
     */
    public static Set<ObjectName> findByProperty(
            final Set<ObjectName> objectNames,
            final String propertyKey,
            final String propertyValue)
    {
        final Set<ObjectName> result = new HashSet<ObjectName>();

        final Iterator iter = objectNames.iterator();
        while (iter.hasNext())
        {
            final ObjectName objectName = (ObjectName) iter.next();

            final String value = objectName.getKeyProperty(propertyKey);
            if (propertyValue.equals(value))
            {
                result.add(objectName);
            }
        }

        return (result);
    }

    /**
    Change or add a key property in an ObjectName.
     */
    public static ObjectName setKeyProperty(final ObjectName objectName, final String key, final String value)
    {
        final String domain = objectName.getDomain();
        final Hashtable<String, String> props = TypeCast.asHashtable(objectName.getKeyPropertyList());

        props.put(key, value);

        ObjectName newObjectName = null;
        try
        {
            newObjectName = new ObjectName(domain, props);
        }
        catch (MalformedObjectNameException e)
        {
            throw new RuntimeException(e);
        }

        return (newObjectName);
    }

    public static void unregisterAll(final MBeanServerConnection conn, final Set<ObjectName> allNames)
            throws IOException, MalformedObjectNameException, MBeanRegistrationException
    {
        for (final ObjectName name : allNames)
        {
            try
            {
                conn.unregisterMBean(name);
            }
            catch (Exception e)
            {
                // OK, gone, it objects, etc
            }
        }
    }

    public static void unregisterAll(final MBeanServerConnection conn)
            throws IOException, MalformedObjectNameException, MBeanRegistrationException
    {
        unregisterAll(conn, queryNames(conn, new ObjectName("*:*"), null));
    }

    public static String[] getAllAttributeNames(
            final MBeanServerConnection conn,
            final ObjectName objectName)
            throws IOException,
                   ReflectionException, IntrospectionException, InstanceNotFoundException
    {
        return (getAttributeNames(getAttributeInfos(conn, objectName)));
    }

    public static MBeanAttributeInfo[] filterAttributeInfos(
            final MBeanAttributeInfo[] infos,
            final AttributeFilter filter)
    {
        final ArrayList<MBeanAttributeInfo> matches = new ArrayList<MBeanAttributeInfo>();
        for (int i = 0; i < infos.length; ++i)
        {
            if (filter.filterAttribute(infos[i]))
            {
                matches.add(infos[i]);
            }
        }

        final MBeanAttributeInfo[] results = new MBeanAttributeInfo[matches.size()];
        matches.toArray(results);

        return (results);
    }

    /**
    Get a String[] of Attribute names.

    @param infos	array of infos
     */
    public static String[] getAttributeNames(final MBeanAttributeInfo[] infos)
    {
        final String[] names = new String[infos.length];

        for (int i = 0; i < infos.length; ++i)
        {
            names[i] = infos[i].getName();
        }

        return (names);
    }

    /**
    @param infos	array of infos
    @param attrName
     */
    public static MBeanAttributeInfo getMBeanAttributeInfo(
            final MBeanAttributeInfo[] infos,
            final String attrName)
    {
        MBeanAttributeInfo info = null;

        for (int i = 0; i < infos.length; ++i)
        {
            if (infos[i].getName().equals(attrName))
            {
                info = infos[i];
                break;
            }
        }

        return (info);
    }

    /**
    @param mbeanInfo
    @param attrName
     */
    public static MBeanAttributeInfo getMBeanAttributeInfo(
            final MBeanInfo mbeanInfo,
            final String attrName)
    {
        return (getMBeanAttributeInfo(mbeanInfo.getAttributes(), attrName));
    }

    /**
    @param conn
    @param objectName
     */
    public static MBeanAttributeInfo[] getAttributeInfos(
            final MBeanServerConnection conn,
            final ObjectName objectName)
            throws IOException,
                   ReflectionException, IntrospectionException, InstanceNotFoundException
    {
        final MBeanAttributeInfo[] infos = conn.getMBeanInfo(objectName).getAttributes();

        return (infos);
    }

    /**
    Convert an AttributeList to a Map where the keys are the Attribute names,
    and the values are Attribute.

    @param attrs	the AttributeList
     */
    public static Map<String, Attribute> attributeListToAttributeMap(final AttributeList attrs)
    {
        final HashMap<String, Attribute> map = new HashMap<String, Attribute>();

        for (int i = 0; i < attrs.size(); ++i)
        {
            final Attribute attr = (Attribute) attrs.get(i);

            map.put(attr.getName(), attr);
        }

        return (map);
    }

    /**
    Convert an AttributeList to a Map where the keys are the Attribute names,
    and the values are the Attribute values.

    @param attrs	the AttributeList
     */
    public static Map<String, Object> attributeListToValueMap(final AttributeList attrs)
    {
        final Map<String, Object> map = new HashMap<String, Object>();

        for (int i = 0; i < attrs.size(); ++i)
        {
            final Attribute attr = (Attribute) attrs.get(i);

            final Object value = attr.getValue();

            map.put(attr.getName(), value);
        }

        return (map);
    }

    /**
    Convert an AttributeList to a Map where the keys are the Attribute names,
    and the values are the Attribute values.

    @param attrs	the AttributeList
     */
    public static Map<String, String> attributeListToStringMap(final AttributeList attrs)
    {
        final Map<String, String> map = new HashMap<String, String>();

        for (int i = 0; i < attrs.size(); ++i)
        {
            final Attribute attr = (Attribute) attrs.get(i);

            final Object value = attr.getValue();
            final String s = (String) (value == null ? null : "" + value);
            map.put(attr.getName(), s);
        }

        return (map);
    }

    /**
    Convert an MBeanAttributeInfo[] to a Map where the keys are the Attribute names,
    and the values are MBeanAttributeInfo.

    @param attrInfos	the AttributeList
     */
    public static Map<String, MBeanAttributeInfo> attributeInfosToMap(final MBeanAttributeInfo[] attrInfos)
    {
        final Map<String, MBeanAttributeInfo> map = new HashMap<String, MBeanAttributeInfo>();

        for (int i = 0; i < attrInfos.length; ++i)
        {
            final MBeanAttributeInfo attrInfo = attrInfos[i];

            map.put(attrInfo.getName(), attrInfo);
        }

        return (map);
    }

    public static MBeanInfo removeAttributes(
            final MBeanInfo origInfo,
            final String[] attributeNames)
    {
        MBeanInfo result = origInfo;

        if (attributeNames.length != 0)
        {
            final Map<String, MBeanAttributeInfo> infos =
                    JMXUtil.attributeInfosToMap(origInfo.getAttributes());

            for (int i = 0; i < attributeNames.length; ++i)
            {
                infos.remove(attributeNames[i]);
            }

            final MBeanAttributeInfo[] newInfos = new MBeanAttributeInfo[infos.keySet().size()];
            infos.values().toArray(newInfos);

            result = new MBeanInfo(
                    origInfo.getClassName(),
                    origInfo.getDescription(),
                    newInfos,
                    origInfo.getConstructors(),
                    origInfo.getOperations(),
                    origInfo.getNotifications(),
                    origInfo.getDescriptor());
        }

        return (result);
    }

    /**
    Find a feature by name (attribute name, operation name, etc) and return
    all matches.  The feature is matched by calling MBeanFeatureInfo.getName().

    @param infos	infos
    @param name	name
    @return Set of the matching items
     */
    public static Set<MBeanFeatureInfo> findInfoByName(
            final MBeanFeatureInfo[] infos,
            final String name)
    {
        final Set<MBeanFeatureInfo> s = new HashSet<MBeanFeatureInfo>();

        for (int i = 0; i < infos.length; ++i)
        {
            final MBeanFeatureInfo info = infos[i];

            if (info.getName().equals(name))
            {
                s.add(info);
            }
        }

        return (s);
    }

    /**
    Convert an Map to an Attribute list where the keys are the Attribute names,
    and the values are objects.

    @param m
     */
    public static AttributeList mapToAttributeList(final Map<String, Object> m)
    {
        final AttributeList attrList = new AttributeList();

        for (final Map.Entry<String, Object> me : m.entrySet())
        {
            final Attribute attr = new Attribute(me.getKey(), me.getValue());

            attrList.add(attr);
        }

        return (attrList);
    }

    private static boolean connectionIsDead(final MBeanServerConnection conn)
    {
        boolean isDead = false;

        // see if the connection is really dead by calling something innocuous
        try
        {
            conn.isRegistered(new ObjectName(MBEAN_SERVER_DELEGATE));
        }
        catch (MalformedObjectNameException e)
        {
            assert (false);
        }
        catch (IOException e)
        {
            isDead = true;
        }

        return (isDead);
    }

    private static AttributeList getAttributesSingly(
            MBeanServerConnection conn,
            ObjectName objectName,
            String[] attrNames,
            Set<String> problemNames)
            throws InstanceNotFoundException
    {
        AttributeList attrs = new AttributeList();

        for (int i = 0; i < attrNames.length; ++i)
        {
            final String name = attrNames[i];

            try
            {
                final Object value = conn.getAttribute(objectName, name);

                attrs.add(new Attribute(name, value));
            }
            catch (Exception e)
            {
                // if the MBean disappeared while processing, just consider it gone
                // from the start, even if we got some Attributes
                if (e instanceof InstanceNotFoundException)
                {
                    throw (InstanceNotFoundException) e;
                }

                if (problemNames != null)
                {
                    problemNames.add(name);
                }
            }
        }

        return (attrs);
    }

    /**
    Get the Attributes using getAttributes() if possible, but if exceptions
    are encountered, attempt to get them one-by-one.

    @param conn			the conneciton
    @param objectName	name of the object to access
    @param attrNames	attribute names
    @param problemNames	optional Set to which problem names will be added.
    @return AttributeList
     */
    public static AttributeList getAttributesRobust(
            MBeanServerConnection conn,
            ObjectName objectName,
            String[] attrNames,
            Set<String> problemNames)
            throws InstanceNotFoundException, IOException
    {
        AttributeList attrs = null;

        if (problemNames != null)
        {
            problemNames.clear();
        }

        try
        {
            attrs = conn.getAttributes(objectName, attrNames);
            if (attrs == null)
            {
                attrs = new AttributeList();
            }
        }
        catch (InstanceNotFoundException e)
        {
            // if it's not found, we can't do anything about it.
            throw e;
        }
        catch (IOException e)
        {
            if (connectionIsDead(conn))
            {
                throw e;
            }

            // connection is still good

            attrs = getAttributesSingly(conn, objectName, attrNames, problemNames);
        }
        catch (Exception e)
        {
            attrs = getAttributesSingly(conn, objectName, attrNames, problemNames);
        }

        return (attrs);
    }

    /**
    Return true if the two MBeanAttributeInfo[] contain the same attributes
    WARNING: arrays will be sorted to perform the comparison if they are the same length.
    boolean
    sameAttributes( MBeanAttributeInfo[] infos1, MBeanAttributeInfo[] infos2 )
    {
    boolean	equal	= false;

    if( infos1.length == infos2.length )
    {
    equal	= ArrayUtil.arraysEqual( infos1, infos2 );
    if ( ! equal )
    {
    // could still be equal, just in different order
    Arrays.sort( infos1, MBeanAttributeInfoComparator.INSTANCE );
    Arrays.sort( infos2, MBeanAttributeInfoComparator.INSTANCE );

    equal	= true;	// reset to false upon failure
    for( int i = 0; i < infos1.length; ++i )
    {
    if ( ! infos1[ i ].equals( infos2[ i ] ) )
    {
    equal	= false;
    break;
    }
    }
    }
    else
    {
    equal	= true;
    }
    }
    return( equal );
    }
     */
    /**
    Return true if the two MBeanAttributeInfo[] contain the same operations
    WARNING: arrays will be sorted to perform the comparison if they are the same length.
    boolean
    sameOperations( final MBeanOperationInfo[] infos1, final MBeanOperationInfo[] infos2 )
    {
    boolean	equal	= false;

    if ( infos1.length == infos2.length )
    {
    // if they're in identical order, this is the quickest test if they ultimately succeed
    equal	= ArrayUtil.arraysEqual( infos1, infos2 );
    if ( ! equal )
    {
    // could still be equal, just in different order
    Arrays.sort( infos1, MBeanOperationInfoComparator.INSTANCE );
    Arrays.sort( infos2, MBeanOperationInfoComparator.INSTANCE );

    equal	= true;	// reset to false upon failure
    for( int i = 0; i < infos1.length; ++i )
    {
    if ( ! infos1[ i ].equals( infos2[ i ] ) )
    {
    equal	= false;
    break;
    }
    }
    }
    }
    return( equal );
    }
     */
    /**
    Return true if the MBeanInfos have the same interface (for Attributes and
    operations).  MBeanInfo.equals() is not sufficient as it will fail if the
    infos are in different order, but are actually the same.
    boolean
    sameInterface( MBeanInfo info1, MBeanInfo info2 )
    {
    return( sameAttributes( info1.getAttributes(), info2.getAttributes() ) &&
    sameOperations( info1.getOperations(), info2.getOperations() ) );
    }
     */
    public static boolean isIs(final Method method)
    {
        return (method.getName().startsWith(IS) && method.getParameterTypes().length == 0);
    }

    /**
    Return true if the method is of the form isXyz() or getXyz()
    (no parameters)
     */
    public static boolean isGetter(Method method)
    {
        return (method.getName().startsWith(GET) && method.getParameterTypes().length == 0);
    }

    public static boolean isGetter(final MBeanOperationInfo info)
    {
        return (info.getName().startsWith(GET) &&
                info.getSignature().length == 0 &&
                !info.getReturnType().equals("void"));
    }

    public static Set<MBeanOperationInfo> findOperations(
            final MBeanOperationInfo[] operations,
            final String operationName)
    {
        final Set<MBeanOperationInfo> items = new HashSet<MBeanOperationInfo>();
        for (int i = 0; i < operations.length; ++i)
        {
            if (operations[i].getName().equals(operationName))
            {
                items.add(operations[i]);
            }
        }

        return items;
    }

    public static MBeanOperationInfo findOperation(
            final MBeanOperationInfo[] operations,
            final String operationName,
            final String[] types)
    {
        MBeanOperationInfo result = null;

        for (int i = 0; i < operations.length; ++i)
        {
            final MBeanOperationInfo info = operations[i];

            if (info.getName().equals(operationName))
            {
                final MBeanParameterInfo[] sig = info.getSignature();

                if (sig.length == types.length)
                {
                    result = info;	// assume match...
                    for (int j = 0; j < sig.length; ++j)
                    {
                        if (!types[j].equals(sig[j].getType()))
                        {
                            result = null;	// no match
                            break;
                        }
                    }
                }
            }
        }

        return (result);
    }

    /**
    Return true if the method is of the form isXyz() or getXyz()
    (no parameters)
     */
    public static boolean isIsOrGetter(Method method)
    {
        return (isGetter(method) || isIs(method));
    }

    public static String getAttributeName(final Method method)
    {
        final String methodName = method.getName();
        String attrName = null;

        int prefixLength;

        if (methodName.startsWith(GET) || methodName.startsWith(SET))
        {
            prefixLength = 3;
        }
        else
        {
            prefixLength = 2;
        }

        return (methodName.substring(prefixLength, methodName.length()));
    }

    public static boolean isSetter(Method method)
    {
        return (method.getName().startsWith(SET) &&
                method.getParameterTypes().length == 1 &&
                method.getParameterTypes()[ 0] != Attribute.class &&
                method.getReturnType().getName().equals("void"));
    }

    public static boolean isGetAttribute(Method m)
    {
        return (m.getName().equals("getAttribute") &&
                m.getParameterTypes().length == 1 && m.getParameterTypes()[ 0] == String.class);

    }

    public static boolean isGetAttributes(Method m)
    {
        return (m.getName().equals("getAttributes") &&
                m.getParameterTypes().length == 1 && m.getParameterTypes()[ 0] == String[].class);

    }

    public static boolean isSetAttribute(Method m)
    {
        return (m.getName().equals("setAttribute") &&
                m.getParameterTypes().length == 1 && m.getParameterTypes()[ 0] == Attribute.class);

    }

    public static boolean isSetAttributes(Method m)
    {
        return (m.getName().equals("setAttributes") &&
                m.getParameterTypes().length == 1 && m.getParameterTypes()[ 0] == AttributeList.class);

    }

    public static ArrayList<MBeanAttributeInfo> generateAttributeInfos(
            final Collection<Method> methodSet,
            final boolean read,
            final boolean write)
    {
        final ArrayList<MBeanAttributeInfo> infos = new ArrayList<MBeanAttributeInfo>();

        assert (methodSet != null);

        for (final Method m : methodSet)
        {
            final String methodName = m.getName();

            assert (read || (write && methodName.startsWith(SET)));
            final MBeanAttributeInfo info = new MBeanAttributeInfo(
                    getAttributeName(m),
                    m.getReturnType().getName(),
                    methodName,
                    read,
                    write,
                    methodName.startsWith("is"));

            infos.add(info);
        }

        return (infos);
    }

    public static MBeanAttributeInfo[] generateMBeanAttributeInfos(
            final Collection<Method> getterSetters,
            final Collection<Method> getters,
            final Collection<Method> setters)
    {
        final ArrayList<MBeanAttributeInfo> attrsList = new ArrayList<MBeanAttributeInfo>();

        attrsList.addAll(generateAttributeInfos(getterSetters, true, true));
        attrsList.addAll(generateAttributeInfos(getters, true, false));
        attrsList.addAll(generateAttributeInfos(setters, false, true));

        final MBeanAttributeInfo[] attrs = new MBeanAttributeInfo[attrsList.size()];
        attrsList.toArray(attrs);

        return (attrs);
    }

    public static String[] getSignature(final MBeanParameterInfo[] infos)
    {
        final String[] sig = new String[infos.length];

        int i = 0;
        for (final MBeanParameterInfo info : infos)
        {
            sig[i] = info.getType();
            ++i;
        }
        return sig;
    }

    public static MBeanParameterInfo[] generateSignature(final Class[] sig)
    {
        final MBeanParameterInfo[] infos = new MBeanParameterInfo[sig.length];

        for (int i = 0; i < sig.length; ++i)
        {
            final Class paramClass = sig[i];

            final String name = "p" + i;
            final String type = paramClass.getName();
            final String description = paramClass.getName();

            final MBeanParameterInfo info =
                    new MBeanParameterInfo(name, type, description);
            infos[i] = info;
        }

        return (infos);
    }

    public static MBeanOperationInfo[] generateMBeanOperationInfos(
            final Collection<Method> methodSet)
    {
        final MBeanOperationInfo[] infos = new MBeanOperationInfo[methodSet.size()];

        final Iterator iter = methodSet.iterator();

        int i = 0;
        while (iter.hasNext())
        {
            final Method m = (Method) iter.next();
            final String methodName = m.getName();

            final MBeanOperationInfo info = new MBeanOperationInfo(
                    methodName,
                    methodName,
                    generateSignature(m.getParameterTypes()),
                    m.getReturnType().getName(),
                    MBeanOperationInfo.UNKNOWN);

            infos[i] = info;
            ++i;

        }

        return (infos);
    }

    public static MBeanInfo interfaceToMBeanInfo(final Class theInterface)
    {
        final Method[] methods = theInterface.getMethods();

        final Map<String, Method> getters = new HashMap<String, Method>();
        final Map<String, Method> setters = new HashMap<String, Method>();
        final Map<String, Method> getterSetters = new HashMap<String, Method>();
        final Set<Method> operations = new HashSet<Method>();

        for (int i = 0; i < methods.length; ++i)
        {
            final Method method = methods[i];

            String attrName = null;
            if (isIsOrGetter(method))
            {
                attrName = getAttributeName(method);
                getters.put(attrName, method);
            }
            else if (isSetter(method))
            {
                attrName = getAttributeName(method);
                setters.put(attrName, method);
            }
            else
            {
                operations.add(method);
            }

            if ((attrName != null) &&
                getters.containsKey(attrName) &&
                setters.containsKey(attrName))
            {
                final Method getter = getters.get(attrName);

                final Class getterType = getter.getReturnType();
                final Class setterType = setters.get(attrName).getParameterTypes()[ 0];

                if (getterType == setterType)
                {
                    getters.remove(attrName);
                    setters.remove(attrName);
                    getterSetters.put(attrName, getter);
                }
                else
                {
                    throw new IllegalArgumentException("Attribute " + attrName +
                                                       "has type " + getterType.getName() + " as getter but type " +
                                                       setterType.getName() + " as setter");
                }
            }
        }

        /*
        java.util.Iterator	iter	= null;
        trace( "-------------------- getterSetters -------------------" );
        iter	= getterSetters.values().iterator();
        while ( iter.hasNext() )
        {
        trace( ((Method)iter.next()).getName() + ", " );
        }
        trace( "-------------------- getters -------------------" );
        iter	= getters.values().iterator();
        while ( iter.hasNext() )
        {
        trace( ((Method)iter.next()).getName() + ", " );
        }
        trace( "-------------------- setters -------------------" );
        iter	= setters.values().iterator();
        while ( iter.hasNext() )
        {
        trace( ((Method)iter.next()).getName() + ", " );
        }
         */

        final MBeanAttributeInfo[] attrInfos =
                generateMBeanAttributeInfos(getterSetters.values(),
                getters.values(), setters.values());

        final MBeanOperationInfo[] operationInfos =
                generateMBeanOperationInfos(operations);

        final MBeanInfo mbeanInfo = new MBeanInfo(
                theInterface.getName(),
                theInterface.getName(),
                attrInfos,
                null,
                operationInfos,
                null);

        return (mbeanInfo);
    }

    /**
    Merge two MBeanAttributeInfo[].  info1 overrides any duplication in info2.

    @param infos1
    @param infos2
     */
    public static MBeanAttributeInfo[] mergeMBeanAttributeInfos(
            final MBeanAttributeInfo[] infos1,
            final MBeanAttributeInfo[] infos2)
    {
        // first make a Set of all names in infos1
        final Set<String> names = new HashSet<String>();
        for (final MBeanAttributeInfo info : infos1)
        {
            names.add(info.getName());
        }

        final Set<MBeanAttributeInfo> merged = SetUtil.newSet(infos1);

        for (final MBeanAttributeInfo info2 : infos2)
        {
            final String info2Name = info2.getName();

            if (!names.contains(info2Name))
            {
                merged.add(info2);
            }
        }

        final MBeanAttributeInfo[] infosArray = new MBeanAttributeInfo[merged.size()];
        merged.toArray(infosArray);

        return (infosArray);
    }

    /**
    Merge two MBeanNotificationInfo[].

    @param infos1
    @param infos2
     */
    public static MBeanNotificationInfo[] mergeMBeanNotificationInfos(
            final MBeanNotificationInfo[] infos1,
            final MBeanNotificationInfo[] infos2)
    {
        if (infos1 == null)
        {
            return infos2;
        }
        else if (infos2 == null)
        {
            return (infos1);
        }

        final Set<MBeanNotificationInfo> all = SetUtil.newSet(infos1);
        all.addAll(SetUtil.newSet(infos2));

        final MBeanNotificationInfo[] merged = new MBeanNotificationInfo[all.size()];
        return all.toArray(merged);
    }

    /**
    Merge two descriptors.  Values in 'src' override values in 'dest', but neither is
    modified, a new one is returned.
     */
    public static DescriptorSupport mergeDescriptors(
            final Descriptor src,
            final Descriptor dest)
    {
        final DescriptorSupport d = new DescriptorSupport();

        // do it manually, the APIs screw up booleans making the "(true)" instead of "true".
        String[] fieldNames = dest.getFieldNames();
        for (final String fieldName : fieldNames)
        {
            d.setField(fieldName, dest.getFieldValue(fieldName));
        }

        // now overwrite conflicting fields with those from 'src'
        fieldNames = src.getFieldNames();
        for (final String fieldName : fieldNames)
        {
            d.setField(fieldName, src.getFieldValue(fieldName));
        }

        return d;
    }

    /**
    Add MBeanNotificationInfo into the MBeanInfo.

    @param origInfo
    @param notifs
     */
    public static MBeanInfo addNotificationInfos(
            final MBeanInfo origInfo,
            final MBeanNotificationInfo[] notifs)
    {
        MBeanInfo result = origInfo;

        if (notifs != null && notifs.length != 0)
        {
            result = new MBeanInfo(
                    origInfo.getClassName(),
                    origInfo.getDescription(),
                    origInfo.getAttributes(),
                    origInfo.getConstructors(),
                    origInfo.getOperations(),
                    mergeMBeanNotificationInfos(origInfo.getNotifications(), notifs),
                    origInfo.getDescriptor());
        }
        return result;
    }

    /**
    Merge two MBeanOperationInfo[].

    @param infos1
    @param infos2
     */
    public static MBeanOperationInfo[] mergeMBeanOperationInfos(
            final MBeanOperationInfo[] infos1,
            final MBeanOperationInfo[] infos2)
    {
        if (infos1 == null)
        {
            return infos2;
        }
        else if (infos2 == null)
        {
            return (infos1);
        }

        final Set<MBeanOperationInfo> all = SetUtil.newSet(infos1);
        all.addAll(SetUtil.newSet(infos2));

        final MBeanOperationInfo[] merged = new MBeanOperationInfo[all.size()];
        return all.toArray(merged);
    }

    /**
    Merge two MBeanOperationInfo[].

    @param infos1
    @param infos2
     */
    public static MBeanConstructorInfo[] mergeMBeanConstructorInfos(
            final MBeanConstructorInfo[] infos1,
            final MBeanConstructorInfo[] infos2)
    {
        if (infos1 == null)
        {
            return infos2;
        }
        else if (infos2 == null)
        {
            return (infos1);
        }

        final Set<MBeanConstructorInfo> all = SetUtil.newSet(infos1);
        all.addAll(SetUtil.newSet(infos2));

        final MBeanConstructorInfo[] merged = new MBeanConstructorInfo[all.size()];
        return all.toArray(merged);
    }

    /**
    Merge two MBeanInfo.  'info1' takes priority in conflicts, name, etc.

    @param info1
    @param info2
    public static MBeanInfo
    mergeMBeanInfos(
    final MBeanInfo info1,
    final MBeanInfo info2 )
    {
    if ( info1 == null )
    {
    return info2;
    }
    else if ( info2 == null )
    {
    return( info1 );
    }

    return( new MBeanInfo(
    info1.getClassName(),
    info1.getDescription(),
    mergeMBeanAttributeInfos( info1.getAttributes(), info2.getAttributes() ),
    mergeMBeanConstructorInfos( info1.getConstructors(), info2.getConstructors() ),
    mergeMBeanOperationInfos( info1.getOperations(), info2.getOperations() ),
    mergeMBeanNotificationInfos( info1.getNotifications(), info2.getNotifications() ),
    ) );

    fix to merge descriptors!

    }
     */
    /**
    Make a new MBeanInfo from an existing one, substituting MBeanAttributeInfo[]

    @param origMBeanInfo
    @param newAttrInfos
     */
    public static MBeanInfo newMBeanInfo(
            final MBeanInfo origMBeanInfo,
            final MBeanAttributeInfo[] newAttrInfos)
    {
        final MBeanInfo info = new MBeanInfo(
                origMBeanInfo.getClassName(),
                origMBeanInfo.getDescription(),
                newAttrInfos,
                origMBeanInfo.getConstructors(),
                origMBeanInfo.getOperations(),
                origMBeanInfo.getNotifications(),
                origMBeanInfo.getDescriptor());
        return (info);
    }

    /**
    Make a new MBeanInfo from an existing one, substituting MBeanOperationInfo[]

    @param origMBeanInfo
    @param newOps
     */
    public static MBeanInfo newMBeanInfo(
            final MBeanInfo origMBeanInfo,
            final MBeanOperationInfo[] newOps)
    {
        final MBeanInfo info = new MBeanInfo(origMBeanInfo.getClassName(),
                origMBeanInfo.getDescription(),
                origMBeanInfo.getAttributes(),
                origMBeanInfo.getConstructors(),
                newOps,
                origMBeanInfo.getNotifications(),
                origMBeanInfo.getDescriptor());
        return (info);
    }

    /**
    Find the index within the MBeanOperationInfo[] of the specified method with the
    specified parameter types.  If <code>parameterTypes</code> is null, then the
    first operation whose name matches is returned.

    @param info
    @param methodName
    @param parameterTypes
    @return the index of the MBeanOperationInfo, or -1 if not found
     */
    public static int findMBeanOperationInfo(
            final MBeanInfo info,
            final String methodName,
            final String[] parameterTypes)
    {
        int resultIdx = -1;

        final MBeanOperationInfo[] ops = info.getOperations();
        for (int i = 0; i < ops.length; ++i)
        {
            final MBeanOperationInfo op = ops[i];

            if (op.getName().equals(methodName) &&
                (parameterTypes == null ||
                 ArrayUtil.arraysEqual(parameterTypes, op.getSignature())))
            {
                resultIdx = i;
                break;
            }
        }

        return resultIdx;
    }

    public static boolean domainMatches(
            final String defaultDomain,
            final ObjectName pattern,
            final ObjectName candidate)
    {
        boolean matches;

        final String candidateDomain = candidate.getDomain();
        if (pattern.isDomainPattern())
        {
            final String regex =
                    RegexUtil.wildcardToJavaRegex(pattern.getDomain());

            matches = Pattern.matches(regex, candidateDomain);
        }
        else
        {
            // domain is not a pattern

            String patternDomain = pattern.getDomain();
            if (patternDomain.length() == 0)
            {
                patternDomain = defaultDomain;
            }

            matches = patternDomain.equals(candidateDomain);
        }

        //dm( "MBeanProxyMgrImpl.domainMatches: " + matches + " " + pattern + " vs " + candidate );

        return (matches);
    }

    public static boolean matchesPattern(
            final String defaultDomain,
            final ObjectName pattern,
            final ObjectName candidate)
    {
        boolean matches = false;

        if (domainMatches(defaultDomain, pattern, candidate))
        {
            final String patternProps = pattern.getCanonicalKeyPropertyListString();
            final String candidateProps = candidate.getCanonicalKeyPropertyListString();
            assert (patternProps.indexOf("*") < 0);
            assert (candidateProps.indexOf("*") < 0);

            // Since we used canonical form any match means the pattern props String
            // must be a substring of candidateProps
            if (candidateProps.indexOf(patternProps) >= 0)
            {
                matches = true;
            }
        }

        return (matches);
    }

    public static Notification cloneNotification(
            final Notification in,
            final Object source)
    {
        Notification out = null;

        if (in instanceof AttributeChangeNotification &&
                in.getClass() == AttributeChangeNotification.class)
        {
            final AttributeChangeNotification a = (AttributeChangeNotification) in;

            out = new AttributeChangeNotification(
                    source,
                    a.getSequenceNumber(),
                    a.getTimeStamp(),
                    a.getMessage(),
                    a.getAttributeName(),
                    a.getAttributeType(),
                    a.getOldValue(),
                    a.getNewValue());
        }
        else if (in.getClass() == Notification.class)
        {
            out = new Notification(
                    in.getType(),
                    source,
                    in.getSequenceNumber(),
                    in.getTimeStamp(),
                    in.getMessage());
        }
        else
        {
            throw new IllegalArgumentException("Not supporting cloning of: " + in.getClass());
        }

        return out;
    }

    /**
    The sole purpose of this method is to move compiler warnings here, thus
    eliminating them from other call sites.  May be removed when JMX becomes
    generified.
     */
    public static Set<ObjectName> queryNames(
            final MBeanServerConnection conn,
            final ObjectName pattern,
            final QueryExp exp)
            throws java.io.IOException
    {
        return TypeCast.asSet(conn.queryNames(pattern, exp));
    }

    public static Set<ObjectName> queryAllInDomain(
            final MBeanServerConnection conn,
            final String domain)
            throws java.io.IOException
    {
        return queryNames(conn, newObjectNamePattern(domain, ""), null);
    }

    public static Set<ObjectName> queryAllInDomain(
            final MBeanServer conn,
            final String domain)
    {
        return queryNames(conn, newObjectNamePattern(domain, ""), null);
    }

    public static Set<ObjectName> queryLocalMBeans(
            final MBeanServer conn,
            final String domain,
            final String server)
    {
        return queryNames(conn, newObjectNamePattern(domain, "instance="+server), null);
    }

    /**
    The sole purpose of this method is to move compiler warnings here, thus
    eliminating them from other call sites.  May be removed when JMX becomes
    generified.
     */
    public static Set<ObjectName> queryNames(
            final MBeanServer server,
            final ObjectName pattern,
            final QueryExp exp)
    {
        try
        {
            return queryNames((MBeanServerConnection) server, pattern, exp);
        }
        catch (final IOException e)
        {
            // ignore, can't happen.
        }
        return null;
    }

    /**
    Get a Map from the user data field of a Notification.
    This variant requires Map<String,Serializable>.
     */
    public static <T extends Serializable> Map<String, T> getUserDataMapString_Serializable(final Notification notif)
    {
        final Object userData = notif.getUserData();
        if (!(userData instanceof Map))
        {
            throw new IllegalArgumentException();
        }

        final Map<String, T> result = TypeCast.asMap((Map) userData);
        if (result != null)
        {
            // verify that it's a Map<String,Serializable>
            for (final Map.Entry<String, T> me : result.entrySet())
            {
                result.put(me.getKey(), me.getValue());
            }
        }

        return result;
    }

    public static String interfaceName(final MBeanInfo info)
    {
        final Descriptor d = info.getDescriptor();

        return (String) d.getFieldValue("interfaceName");
    }

    /** convenience function to avoid try/catch.  A RuntimeException is thrown if there is a problem */
    public static Object getAttribute(final MBeanServerConnection conn, final ObjectName o, final String attrName)
    {
        try
        {
            return conn.getAttribute(o, attrName);
        }
        catch (final Exception e)
        {
            throw new RuntimeException("Can't get attribute " + attrName, e);
        }
    }

    public static <T extends MBeanFeatureInfo> T remove(final List<T> infos, final String name)
    {
        T removed = null;

        for (final T info : infos)
        {
            if (info.getName().equals(name))
            {
                removed = info;
                infos.remove(info);
                break;
            }
        }
        return removed;
    }

    private static String NL()
    {
        return StringUtil.NEWLINE();
    }

    private static String title(final MBeanFeatureInfo info)
    {
        return info.getName() + ", \"" + info.getDescription() + "\"";
    }

    public static String toString(final Descriptor d, final int indent)
    {
        final String NL = NL();
        final StringBuffer buf = new StringBuffer();
        if (d != null && d.getFieldNames().length != 0)
        {
            buf.append(idt(indent)).append("Descriptor  = ").append(NL);
            for (final String fieldName : d.getFieldNames())
            {
                buf.append(idt(indent + 2)).append(nvp(fieldName, d.getFieldValue(fieldName))).append(NL);
            }
            buf.append(NL);
        }
        else
        {
            //buf.append( idt(indent) + "Descriptor = n/a" + NL );
        }

        return buf.toString();
    }

    public static String impactStr(final int impact)
    {
        String s;
        if (impact == MBeanOperationInfo.INFO)
        {
            s = "INFO";
        }
        else if (impact == MBeanOperationInfo.ACTION)
        {
            s = "ACTION";
        }
        else if (impact == MBeanOperationInfo.UNKNOWN)
        {
            s = "UNKNOWN";
        }
        else if (impact == MBeanOperationInfo.ACTION_INFO)
        {
            s = "ACTION_INFO";
        }
        else
        {
            s = "" + impact;
        }
        return s;
    }

    public static String toString(final MBeanOperationInfo info, final int indent)
    {
        final String NL = NL();
        final StringBuffer buf = new StringBuffer();

        final String idt = idt(indent + 2);

        buf.append(idt(indent)).append(title(info)).append(NL);
        buf.append(idt).append(nvp("Impact", impactStr(info.getImpact()))).append(NL);
        buf.append(idt).append(nvp("ReturnType", info.getReturnType())).append(NL);
        buf.append(idt).append(nvp("Param count", info.getSignature().length)).append(NL);

        final Descriptor d = info.getDescriptor();
        if (d != null)
        {
            buf.append(toString(d, indent + 2));
        }

        return buf.toString();
    }

    public static String toString(final MBeanAttributeInfo info, final int indent)
    {
        final String NL = NL();
        final StringBuffer buf = new StringBuffer();

        final String idt = idt(indent + 2);

        String rw = info.isReadable() ? "R" : "";
        if (info.isWritable())
        {
            rw = rw + "W";
        }
        if (info.isIs())
        {
            rw = rw + ",is";
        }

        buf.append(idt(indent)).append(title(info)).append(NL);
        buf.append(idt).append(nvp("Type", info.getType())).append(NL);
        buf.append(idt).append(nvp("Access", rw)).append(NL);
        final Descriptor d = info.getDescriptor();
        if (d != null)
        {
            buf.append(toString(d, indent + 2));
        }

        if (info instanceof OpenMBeanAttributeInfo)
        {
            final OpenMBeanAttributeInfo open = (OpenMBeanAttributeInfo) info;
            buf.append(idt).append(nvp("OpenType", open.getOpenType().toString())).append(NL);
            buf.append(idt).append(nvp("hasLegalValues", open.hasLegalValues())).append(NL);
            buf.append(idt).append(nvp("hasDefaultValue", open.hasDefaultValue())).append(NL);
            buf.append(idt).append(nvp("hasMinValue", open.hasMinValue())).append(NL);
            buf.append(idt).append(nvp("hasMaxValue", open.hasMaxValue())).append(NL);

            if (open.hasDefaultValue())
            {
                buf.append(idt).append(nvp("DefaultValue", open.getDefaultValue())).append(NL);
            }
        }

        return buf.toString();
    }

    /**
    Produce a nice friendly text dump of the MBeanInfo; the standard toString() is unreadable.
     */
    public static String toString(final MBeanInfo info)
    {
        final StringBuffer buf = new StringBuffer();
        final String NL = NL();

        int indent = 2;
        buf.append("Classname: ").append(info.getClassName()).append(NL);
        buf.append("Description: ").append(info.getDescription()).append(NL);

        buf.append(toString(info.getDescriptor(), indent + 2));

        buf.append(idt(indent)).append("Attributes").append(NL);
        final MBeanAttributeInfo[] attrInfos = info.getAttributes();
        if (attrInfos.length == 0)
        {
            buf.append(idt(indent + 2)).append("<none>");
        }
        else
        {
            for (final MBeanAttributeInfo attrInfo : attrInfos)
            {
                buf.append(toString(attrInfo, indent + 2));
                buf.append(NL);
            }
        }

        buf.append(idt(indent)).append("Operations").append(NL);
        final MBeanOperationInfo[] opInfos = info.getOperations();
        if (info.getOperations().length == 0)
        {
            buf.append(idt(indent + 2)).append("<none>");
        }
        else
        {
            for (final MBeanOperationInfo opInfo : opInfos)
            {
                buf.append(toString(opInfo, indent + 2));
            }
        }

        return buf.toString();
    }

    private static String idt(final int num)
    {
        final StringBuffer buf = new StringBuffer();
        for (int i = 0; i < num; ++i)
        {
            buf.append(" ");
        }
        return buf.toString();
    }

    private static String nvp(final String name, final Object value)
    {
        return name + " = " + SmartStringifier.DEFAULT.stringify(value);
    }

}

