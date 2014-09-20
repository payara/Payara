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
package org.glassfish.admin.amx.core;

import java.util.regex.Pattern;

import org.glassfish.admin.amx.util.jmx.JMXUtil;
import org.glassfish.admin.amx.util.SetUtil;
import org.glassfish.admin.amx.util.ClassUtil;
import org.glassfish.admin.amx.core.AMXMBeanMetadata;

import static org.glassfish.external.amx.AMX.*;

import javax.management.Notification;
import javax.management.ObjectName;
import java.io.Serializable;
import java.util.*;
import javax.management.MBeanServer;
import org.glassfish.external.arc.Stability;
import org.glassfish.external.arc.Taxonomy;

/**
Utility routines pertinent to the MBean API.
 */
@Taxonomy(stability = Stability.UNCOMMITTED)
public final class Util {

    private static final String QUOTE_CHAR = "\"";

    private static void debug(final String s) {
        System.out.println(s);
    }

    public static String quoteIfNeeded(String name) {
        if(name.indexOf(":") > 1) {
           return ObjectName.quote(name);
        } else {
            return name;
        }
    }

    public  static String unquoteIfNeeded(String name) {
        if(name != null && (name.startsWith(QUOTE_CHAR) && name.endsWith(QUOTE_CHAR))) {
           return ObjectName.unquote(name);
        } else {
            return name;
        }
    }

    private Util() {
    }

    /**
    Create a new ObjectName, caller is guaranteeing that the name is
    well-formed (a RuntimeException will be thrown if not). This avoids
    having to catch all sorts of JMX exceptions.<br>
    <b>NOTE:</b> Do not call this method if there is not certainty of a well-formed name.

    @param name
     */
    public static ObjectName newObjectName(String name) {
        return (JMXUtil.newObjectName(name));
    }

    /**
    Build an ObjectName.  Calls newObjectName( domain + ":" + props )

    @param domain	the JMX domain
    @param props	properties of the ObjectName
     */
    public static ObjectName newObjectName(String domain, String props) {
        return (newObjectName(domain + ":" + props));
    }

    /**
    Build an ObjectName pattern.

    @param domain	the JMX domain
    @param props	properties of the ObjectName
     */
    public static ObjectName newObjectNamePattern(
            final String domain,
            final String props) {
        return (JMXUtil.newObjectNamePattern(domain, props));
    }

    /**
    Build an ObjectName pattern.

    @param objectName
     */
    public static ObjectName newObjectNamePattern(ObjectName objectName) {
        final String props = objectName.getKeyPropertyListString();

        return (newObjectNamePattern(objectName.getDomain(), props));
    }

    /**
    Make an ObjectName property of the form <i>name</i>=<i>value</i>.

    @param name
    @param value
     */
    public static String makeProp(
            final String name,
            final String value) {
        return (JMXUtil.makeProp(name, value));
    }

    /**
    Make an ObjectName property of the form type=<i>value</i>.

    @param value
     */
    public static String makeTypeProp(final String value) {
        return (makeProp(TYPE_KEY, value));
    }

    /**
    Make an ObjectName property of the form name=<i>value</i>.
     */
    public static String makeNameProp(final String name) {
        return (makeProp(NAME_KEY, "" + quoteIfNeeded(name)));
    }

    /**
    @param type
    @param name
     */
    public static String makeRequiredProps(
            final String type,
            final String name) {
        String props = Util.makeTypeProp(type);
        if (!(name == null || name.length() == 0 || name.equals(NO_NAME))) {
            final String nameProp = Util.makeNameProp(name);
            props = Util.concatenateProps(props, nameProp);
        }

        return (props);
    }

    /**
    Extract the type and name properties and return it as a single property
    <i>type</i>=<i>name</i>

    @param objectName
     */
    public static String getSelfProp(final ObjectName objectName) {
        final String type = objectName.getKeyProperty(TYPE_KEY);
        final String name = objectName.getKeyProperty(NAME_KEY);

        return (Util.makeProp(type, name));
    }

    /**
    Extract all properties other than type=<type>,name=<name>.

    @param objectName
     */
    public static String getAdditionalProps(final ObjectName objectName) {
        final java.util.Hashtable<String,String> allProps = objectName.getKeyPropertyList();
        allProps.remove(TYPE_KEY);
        allProps.remove(NAME_KEY);

        String props = "";
        for (final Map.Entry<String,String> e : allProps.entrySet()) {
            final String prop = makeProp(e.getKey(), e.getValue());
            props = concatenateProps(props, prop);
        }

        return props;
    }

    public static String concatenateProps(
            final String props1,
            final String props2) {
        return (JMXUtil.concatenateProps(props1, props2));
    }

    public static String concatenateProps(
            final String props1,
            final String props2,
            final String props3) {
        return (concatenateProps(concatenateProps(props1, props2), props3));
    }

    /**
    @return a List of ObjectNames from a Set of AMX.
     */
    public static List<ObjectName> toObjectNameList(final Collection<? extends AMXProxy> amxs) {
        final List<ObjectName> objectNames = new ArrayList<ObjectName>();
        for (final AMXProxy next : amxs) {
            objectNames.add(next.objectName());
        }
        return (Collections.checkedList(objectNames, ObjectName.class));
    }

    /**
    @return a Map of ObjectNames from a Map whose values are AMX.
     */
    public static Map<String, ObjectName> toObjectNameMap(final Map<String, ? extends AMXProxy> amxMap) {
        final Map<String, ObjectName> m = new HashMap<String, ObjectName>();

        for (final Map.Entry<String,? extends AMXProxy> e : amxMap.entrySet()) {
            final AMXProxy value = e.getValue();
            m.put(e.getKey(), value.objectName());
        }
        return (Collections.checkedMap(m, String.class, ObjectName.class));
    }

    /**
    @return an ObjectName[] from an AMX[]
     */
    public static ObjectName[] toObjectNamesArray(final AMXProxy[] amx) {
        final ObjectName[] objectNames = new ObjectName[amx.length];
        for (int i = 0; i < objectNames.length; ++i) {
            objectNames[i] = amx[i] == null ? null : amx[i].objectName();
        }

        return (objectNames);
    }

    public static ObjectName[] toObjectNamesArray(final Collection<? extends AMXProxy> amxs) {
        final ObjectName[] objectNames = new ObjectName[amxs.size()];
        int i = 0;
        for (final AMXProxy amx : amxs) {
            objectNames[i] = amx.objectName();
            ++i;
        }

        return (objectNames);
    }

    /**
    Create a Map keyed by the value of the NAME_KEY with
    value the AMX item.

    @param amxs Set of AMX
     */
    public static <T extends AMXProxy> Map<String, T> createNameMap(final Set<T> amxs) {
        final Map<String, T> m = new HashMap<String, T>();

        for (final T amx : amxs) {
            final String name = amx.getName();
            m.put(name, amx);
        }

        return (m);
    }

    /**
    Create a Map keyed by the value of the NAME_KEY with
    value the ObjectName. Note that if two or more ObjectNames
    share the same name, the resulting Map will contain only
    one of the original ObjectNames.

    @param objectNames Set of ObjectName
     */
    public static final Map<String, ObjectName> createObjectNameMap(final Set<ObjectName> objectNames) {
        final Map<String, ObjectName> m = new HashMap<String, ObjectName>();

        for (final ObjectName objectName : objectNames) {
            final String name = getNameProp(objectName);

            assert (!m.containsKey(name)) :
                    "createObjectNameMap: key already present: " + name + " in " + objectName;
            m.put(name, objectName);
        }

        assert (m.keySet().size() == objectNames.size());

        return (Collections.checkedMap(m, String.class, ObjectName.class));
    }

    public static <T extends AMXProxy> List<T> asProxyList(final Collection<? extends AMXProxy> c, final Class<T> intf) {
        final List<T> list = new ArrayList<T>();

        for (final AMXProxy amx : c) {
            list.add(amx.as(intf));
        }

        return list;
    }

    /**
    All Notifications emitted by AMX MBeans which are not
    standard types defined by JMX place a Map
    into the userData field of the Notification.  This call
    retrieves that Map, which may be null if no additional
    data is included.
     */
    public static Map<String, Serializable> getAMXNotificationData(final Notification notif) {
        return Collections.unmodifiableMap(
                JMXUtil.getUserDataMapString_Serializable(notif));
    }

    /**
    Use of generic type form taking Class<T> is preferred.
     */
    public static Serializable getAMXNotificationValue(final Notification notif, final String key) {
        final Map<String, Serializable> data =
                getAMXNotificationData(notif);

        if (data == null) {
            throw new IllegalArgumentException(notif.toString());
        }

        if (!data.containsKey(key)) {
            throw new IllegalArgumentException("Value not found for " + key
                    + " in " + notif);
        }

        return data.get(key);
    }

    /**
    Retrieve a particular value associated with the specified
    key from an AMX Notification.
    @see #getAMXNotificationData
     */
    public static <T extends Serializable> T getAMXNotificationValue(
            final Notification notif,
            final String key,
            final Class<T> theClass) {
        final Serializable value = getAMXNotificationValue(notif, key);

        return theClass.cast(value);
    }

    public static void sleep(final long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
    }

    /**
    A safe way to cast to AMX.
     */
    public static AMXProxy asAMX(final Object o) {
        return AMXProxy.class.cast(o);
    }

    /**
    Filter the AMX dynamic proxies to those that implement the specified interface,
    and return a new Set with the matching items.  The 'desired' interface can be
    any AMX-defined class, including the mixin ones.

    @param candidates the Set to be filtered
    @param desired the interface to filter by
     */
    public static <T extends AMXProxy> Set<T> filterAMX(final Set<T> candidates, final Class<?> desired) {
        final Set<T> result = new HashSet<T>();
        for (final T amx : candidates) {
            if (desired.isAssignableFrom(amx.getClass())) {
                result.add(amx);
            }
        }
        return result;
    }

    public static Map<String, ObjectName> filterByType(final ObjectName[] objectNames, final String type) {
        Map<String, ObjectName> m = null;
        if (objectNames != null) {
            m = new HashMap<String, ObjectName>();
            for (final ObjectName o : objectNames) {
                if (type.equals(o.getKeyProperty(TYPE_KEY))) {
                    // use the name property, not the name from getNameProp()
                    m.put(o.getKeyProperty(NAME_KEY), o);
                }
            }
        }
        return m;
    }

    /**
    Filter the AMX dynamic proxies to those that implement the specified interface,
    and return a new Map with the matching items.  The 'desired' interface can be
    any AMX-defined class, including the mixin ones.

    @param candidates the Map to be filtered
    @param desired the interface to filter by
     */
    public static <T extends AMXProxy> Map<String, T> filterAMX(final Map<String, T> candidates, final Class<?> desired) {
        final Map<String, T> result = new HashMap<String, T>();
        for (final Map.Entry<String,T> e : candidates.entrySet()) {
            final T amx = e.getValue();
            if (desired.isAssignableFrom(amx.getClass())) {
                result.put(e.getKey(), amx);
            }
        }
        return result;
    }

    public static String getTypeProp(final ObjectName objectName) {
        return objectName.getKeyProperty(TYPE_KEY);
    }

    /**
    Get the value of the NAME_KEY property within the ObjectName, or null if
    not present.
    @return the name
     */
    public static String getNameProp(final ObjectName objectName) {
        return objectName.getKeyProperty(NAME_KEY);
    }

    public static String getParentPathProp(final ObjectName objectName) {
        return objectName.getKeyProperty(PARENT_PATH_KEY);
    }

    public static String getParentPathProp(final AMXProxy amx) {
        return getParentPathProp(amx.extra().objectName());
    }

    public static ObjectName getParent(final MBeanServer server, final ObjectName objectName) {
        return (ObjectName) JMXUtil.getAttribute(server, objectName, ATTR_PARENT);
    }

    /**
    Generate the default MBean type from a String, eg from a classname.
     */
    public static String typeFromName(final String s) {
        if (s.indexOf("-") >= 0) {
            return s;   // if it already has dashes, leave unchanged
        }

        String simpleName = s;
        final int idx = s.lastIndexOf(".");
        if (idx >= 0) {
            simpleName = s.substring(idx + 1);
        }
        return domConvertName(simpleName);
    }
    /** Proxy interfaces may contain a type field denoting the type to be used in the ObjectName;
     * this is an alternative to an annotation that may be desirable to avoid
     * a dependency on the amx-core module.  Some proxy interfaces also represent
     * MBeans whose type and other metadata is derived not from the proxy interface,
     * but from another authoritative source; this allows an explicit
     * linkage that allows the AMXProxyHandler to deduce the correct type, given
     * the interface (and avoids any further complexity, the KISS principle).
     * eg public static final String AMX_TYPE = "MyType";
     * <p>
     * A good example of this is the config MBeans which use lower case types with dashes. Other
     * types may use classnames, or other variants; the proxy code can't assume any particular
     * mapping from a proxy interface to the actual MBean type.
     */
    public static final String TYPE_FIELD = "AMX_TYPE";

    /**
    Deduce the type to be used in the path.  Presence of a TYPE_FIELD field always
    take precedence, then the AMXMBeanMetadata.
     */
    public static String deduceType(final Class<?> intf) {
        if (intf == null) {
            throw new IllegalArgumentException("null interface");
        }

        String type = null;

        AMXMBeanMetadata meta = null;
        final Object typeField = ClassUtil.getFieldValue(intf, TYPE_FIELD);
        if (typeField instanceof String) {
            type = (String) typeField;
        } else if ((meta = intf.getAnnotation(AMXMBeanMetadata.class)) != null) {
            final String typeValue = meta.type();

            if (typeValue.equals(AMXMBeanMetadata.NULL) || typeValue.length() == 0) {
                type = Util.typeFromName(intf.getName());
            } else {
                type = typeValue;
            }
        } else {
            // no annotation, use our default conversion
            type = Util.typeFromName(intf.getName());
        }
        return type;
    }

    public static ObjectName getAncestorByType(final MBeanServer mbeanServer, final ObjectName child, final String type) {
        ObjectName cur = child;

        while ((cur = (ObjectName) JMXUtil.getAttribute(mbeanServer, cur, ATTR_PARENT)) != null) {
            if (getTypeProp(cur).equals(type)) {
                break;
            }
        }
        return cur;
    }
    // BEGIN domConvertName
    /* Code below is taken from Dom.java to avoid a dependency */
    static final String[] PROPERTY_PREFIX = new String[]{"get", "set", "is", "has"};

    private static String domConvertName(final String nameIn) {
        String name = nameIn;
        for (final String p : PROPERTY_PREFIX) {
            if (name.startsWith(p)) {
                name = name.substring(p.length());
                break;
            }
        }
        // tokenize by finding 'x|X' and 'X|Xx' then insert '-'.
        final StringBuilder buf = new StringBuilder(name.length() + 5);
        for (final String t : TOKENIZER.split(name)) {
            if (buf.length() > 0) {
                buf.append('-');
            }
            buf.append(t.toLowerCase(Locale.ENGLISH));
        }
        return buf.toString();
    }
    /**
     * Used to tokenize the property name into XML name.
     */
    static final Pattern TOKENIZER;

    private static String split(String lookback, String lookahead) {
        return "((?<=" + lookback + ")(?=" + lookahead + "))";
    }

    private static String or(String... tokens) {
        final StringBuilder buf = new StringBuilder();
        for (String t : tokens) {
            if (buf.length() > 0) {
                buf.append('|');
            }
            buf.append(t);
        }
        return buf.toString();
    }

    static {
        String pattern = or(
                split("x", "X"), // AbcDef -> Abc|Def
                split("X", "Xx"), // USArmy -> US|Army
                //split("\\D","\\d"), // SSL2 -> SSL|2
                split("\\d", "\\D") // SSL2Connector -> SSL|2|Connector
                );
        pattern = pattern.replace("x", "\\p{Lower}").replace("X", "\\p{Upper}");
        TOKENIZER = Pattern.compile(pattern);
    }
    // END domConvertName
}
