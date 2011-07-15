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

package org.glassfish.admin.amxtest.base;

import com.sun.appserv.management.base.AMX;
import com.sun.appserv.management.base.AMXAttributes;
import com.sun.appserv.management.base.Container;
import com.sun.appserv.management.base.Extra;
import com.sun.appserv.management.base.QueryMgr;
import com.sun.appserv.management.base.Util;
import com.sun.appserv.management.base.XTypes;
import com.sun.appserv.management.config.AMXConfig;
import com.sun.appserv.management.config.NamedConfigElement;
import com.sun.appserv.management.config.SecurityMapConfig;
import com.sun.appserv.management.ext.logging.LogQueryResult;
import com.sun.appserv.management.ext.wsmgmt.MessageTrace;
import com.sun.appserv.management.ext.wsmgmt.WebServiceEndpointInfo;
import com.sun.appserv.management.monitor.AMXCounterMonitor;
import com.sun.appserv.management.monitor.AMXGaugeMonitor;
import com.sun.appserv.management.monitor.AMXStringMonitor;
import com.sun.appserv.management.monitor.ApplicationMonitor;
import com.sun.appserv.management.monitor.EJBModuleMonitor;
import com.sun.appserv.management.monitor.HTTPServiceMonitor;
import com.sun.appserv.management.monitor.JMXMonitorMgr;
import com.sun.appserv.management.util.jmx.JMXUtil;
import com.sun.appserv.management.util.misc.ClassUtil;
import com.sun.appserv.management.util.misc.CollectionUtil;
import com.sun.appserv.management.util.misc.GSetUtil;
import com.sun.appserv.management.util.misc.StringUtil;
import org.glassfish.admin.amx.util.AMXDebugStuff;
import com.sun.appserv.management.ext.coverage.CoverageInfo;
import org.glassfish.admin.amxtest.AMXTestBase;
import org.glassfish.admin.amxtest.Capabilities;

import javax.management.AttributeNotFoundException;
import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanFeatureInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 */
public final class AMXTest
        extends AMXTestBase {
    public AMXTest() {
    }

    public static Capabilities
    getCapabilities() {
        return getOfflineCapableCapabilities(true);
    }

    /**
     Verify that the ObjectName returned from the ATTR_CONTAINER_OBJECT_NAME is the same
     as the ObjectName obtained from the getContainer() proxy.
     */
    public void
    checkContainerObjectName(final ObjectName objectName)
            throws Exception {
        final ObjectName containerObjectName = (ObjectName)
                getConnection().getAttribute(objectName, AMXAttributes.ATTR_CONTAINER_OBJECT_NAME);

        if (Util.getJ2EEType(objectName).equals(XTypes.DOMAIN_ROOT)) {
            assert (containerObjectName == null);
        } else {
            assert (containerObjectName != null);
            final AMX proxy = getProxyFactory().getProxy(objectName, AMX.class);
            assert (Util.getObjectName(proxy.getContainer()).equals(containerObjectName));
        }

    }

    public void
    testContainerObjectName()
            throws Exception {
        testAll("checkContainerObjectName");
    }


    /**
     Look for Attributes that probably should be String and not int/long
     due to our template facility ${...}
     */
    public void
    checkTemplateAttributes(final ObjectName objectName) {
        final AMX proxy = getProxyFactory().getProxy(objectName, AMX.class);

        if (proxy instanceof AMXConfig) {
            final AMXConfig config = (AMXConfig) proxy;

            final Set<String> s = new HashSet<String>();

            final MBeanInfo mbeanInfo = Util.getExtra(config).getMBeanInfo();
            final MBeanAttributeInfo[] attrInfos = mbeanInfo.getAttributes();
            for (int i = 0; i < attrInfos.length; ++i) {
                final MBeanAttributeInfo info = attrInfos[i];

                final String type = info.getType();
                if (type.equals("int") || type.equals("long")) {
                    s.add(info.getName());
                }
            }

            if (s.size() != 0) {
                trace("\n" + objectName +
                        " contains the following int/long Attributes which perhaps ought to be String" +
                        " due to the templatizing of config: " + toString(s) + "\n");
            }
        }
    }

    public void
    testTemplateAttributes()
            throws Exception {
        testAll("checkTemplateAttributes");
    }


    /**
     Verify that the ObjectName returned from the MBean is in fact itself.
     */
    public void
    checkSelfObjectName(final ObjectName obj)
            throws Exception {
        final ObjectName selfName = (ObjectName)
                getConnection().getAttribute(obj, AMXAttributes.ATTR_OBJECT_NAME);

        assert (selfName.equals(obj));
    }

    public void
    testSelfObjectName()
            throws Exception {
        testAll("checkSelfObjectName");
    }


    /**
     Verify that the MBean has an ATTR_INTERFACE_NAME Attribute
     */
    public void
    checkInterface(final ObjectName src)
            throws Exception {
        final String interfaceName = (String)
                getConnection().getAttribute(src, AMXAttributes.ATTR_INTERFACE_NAME);

        assert (interfaceName != null);
    }


    public void
    testInterface()
            throws Exception {
        testAll("checkInterface");
    }

    /**
     Verify that the MBean has j2eeType and name.
     */
    public void
    checkJ2EETypeAndName(final ObjectName src)
            throws Exception {
        assert (src.getKeyProperty(AMX.J2EE_TYPE_KEY) != null);
        assert (src.getKeyProperty(AMX.NAME_KEY) != null);
    }


    public void
    testJ2EETypeAndName()
            throws Exception {
        testAll("checkJ2EETypeAndName");
    }


    /**
     Verify that all j2eeTypes have a proper Container that does actually hold them.
     */
    public void
    testContainerChild() {
/*
		final TypeInfos	infos	= TypeInfos.getInstance();
		final Set<String>		j2eeTypesSet	= infos.getJ2EETypes();

        for( final String j2eeType : j2eeTypesSet )
		{
			checkContainerChild( j2eeType );
		}
*/
    }

    /**
     Verify that each child's Container actually claims the child as a child.
     */
    public void
    checkContainerChild(final String childJ2EEType) {
        final QueryMgr queryMgr = getQueryMgr();
        final Set children = queryMgr.queryJ2EETypeSet(childJ2EEType);

        final Iterator iter = children.iterator();
        while (iter.hasNext()) {
            final AMX containee = Util.asAMX(iter.next());
            Container container = null;

            final ObjectName objectName = Util.getObjectName(containee);
            if (!shouldTest(objectName)) {
                continue;
            }

            try {
                container = (Container) containee.getContainer();
            }
            catch (Exception e) {
                trace("Can't get container for: " + objectName);
            }

            if (container == null) {
                assert (containee.getJ2EEType().equals(XTypes.DOMAIN_ROOT)) :
                        "container is null for: " + objectName;
                continue;
            }

            final Set<AMX> containeeSet = container.getContaineeSet(childJ2EEType);
            final Set<ObjectName> containeeObjectNameSet = Util.toObjectNames(containeeSet);

            assert (containeeObjectNameSet.contains(Util.getExtra(containee).getObjectName()));
        }
    }


    /**
     Statically verify that the interface for each proxy has a J2EE_TYPE field.
     */
    public void
    testHaveJ2EE_TYPE() {
/*		final TypeInfos	infos	= TypeInfos.getInstance();
		final Set			j2eeTypes	= infos.getJ2EETypes();
		
		boolean	success	= true;
		final Iterator	iter		= j2eeTypes.iterator();
		while ( iter.hasNext() )
		{
			final String		j2eeType	= (String)iter.next();
			final TypeInfo	info	= infos.getInfo( j2eeType );
			
			final Class	theInterface	= info.getInterface();
			try
			{
				final String	value	=
					(String)ClassUtil.getFieldValue( theInterface, "J2EE_TYPE" );
				assert( value.equals( j2eeType ) ) :
					"info and J2EE_TYPE don't match: " + j2eeType + " != " + value;
			}
			catch( Exception e )
			{
				trace( "no J2EE_TYPE field found for proxy of type: " + theInterface.getName() );
				success	= false;
			}
		}
		assert( success );*/
    }


    /**
     Verify that getName() is the same as the 'name' property in the ObjectName.
     */
    public void
    checkNameMatchesJ2EEName(final ObjectName childObjectName)
            throws Exception {
        final AMX childProxy = getProxyFactory().getProxy(childObjectName, AMX.class);
        if (childProxy instanceof NamedConfigElement) {
            final String j2eeName = childProxy.getName();

            assertEquals(j2eeName, childProxy.getName());
        }
    }

    public void
    testNameMatchesJ2EEName()
            throws Exception {
        testAll("checkNameMatchesJ2EEName");
    }


    private static final String MAP_SUFFIX = "Map";
    private static final String OBJECTNAME_MAP_SUFFIX = "ObjectName" + MAP_SUFFIX;

    private static boolean
    isMapGetterName(final String methodName) {
        return (
                methodName.startsWith(JMXUtil.GET) &&
                        methodName.endsWith(MAP_SUFFIX));
    }

    private static boolean
    isMapGetter(final Method method) {
        return (
                Map.class.isAssignableFrom(method.getReturnType()) &&
                        isMapGetterName(method.getName()));
    }

    /**
     Verify that a proxy getAbcMap(...) Attribute or operation has an appropriate
     MBean getAbcObjectNameMap() method.
     */
    public void
    checkMaps(final ObjectName objectName)
            throws Exception {
        final AMX proxy = getProxyFactory().getProxy(objectName, AMX.class);
        if (proxy instanceof Container) {
            final Method[] methods = getInterfaceClass(proxy).getMethods();
            final MBeanInfo mbeanInfo = Util.getExtra(proxy).getMBeanInfo();

            for (int methodIdx = 0; methodIdx < methods.length; ++methodIdx) {
                final Method method = methods[methodIdx];
                final String methodName = method.getName();

                if (isMapGetter(method)) {
                    if (methodName.endsWith(OBJECTNAME_MAP_SUFFIX)) {
                        warning("method should exist in MBeanInfo, not interface: " + methodName);
                        continue;
                    }

                    // verify that a corresponding peer method exists and
                    // has the right return type and same number and type of parameters
                    final String peerMethodName =
                            StringUtil.replaceSuffix(methodName, MAP_SUFFIX, OBJECTNAME_MAP_SUFFIX);

                    checkCompatibleOperationExists(Util.getObjectName(proxy),
                                                   method,
                                                   peerMethodName,
                                                   mbeanInfo);
                } else if (isMapGetterName(methodName)) {
                    warning("operation " + methodName + " does not return a Map!");
                }
            }
        }
    }

    /**
     Verify that the proxy method has a compatible Attribute or operation.
     <ul>
     <li>a proxy getter must have a corresponding Attribute returning an ObjectName</li>
     <li>a proxy operation must have a corresponding operation with matching signature</li>
     <li>a proxy operation must have a corresponding operation with compatible return type</li>
     </u
     */
    private void
    checkCompatibleOperationExists(
            final ObjectName objectName,
            final Method proxyMethod,
            final String mbeanMethodName,
            final MBeanInfo mbeanInfo) {
        final Class proxyReturnType = proxyMethod.getReturnType();
        final String proxyMethodName = proxyMethod.getName();

        String mbeanReturnType = null;

        final Class[] parameterTypes = proxyMethod.getParameterTypes();
        if (JMXUtil.isGetter(proxyMethod)) {
            // it's getter
            final Map<String, MBeanAttributeInfo> m = JMXUtil.attributeInfosToMap(mbeanInfo.getAttributes());

            final String attrName = StringUtil.stripPrefix(mbeanMethodName, JMXUtil.GET);
            final MBeanAttributeInfo attrInfo = (MBeanAttributeInfo) m.get(attrName);
            if (attrInfo != null) {
                mbeanReturnType = attrInfo.getType();
            }
        } else {
            // look for an operation that matches
            final MBeanOperationInfo[] operations = mbeanInfo.getOperations();

            final String[] stringSig = ClassUtil.classnamesFromSignature(parameterTypes);
            final MBeanOperationInfo opInfo = JMXUtil.findOperation(operations, mbeanMethodName, stringSig);
            if (opInfo != null) {
                mbeanReturnType = opInfo.getReturnType();
            }
        }

        boolean hasPeer = mbeanReturnType != null;
        if (hasPeer) {
            // a proxy return type of AMX should have an Attribute type of ObjectName
            if (AMX.class.isAssignableFrom(proxyReturnType)) {
                assert (mbeanReturnType.equals(ObjectName.class.getName()));
            } else // return types must match
            {
                assert (mbeanReturnType.equals(proxyReturnType.getName()));
            }
            hasPeer = true;
        }


        if (!hasPeer) {
            trace("MBean " + objectName + " has operation " + proxyMethodName +
                    " without corresponding peer Attribute/operation " + mbeanMethodName);
        }
    }

    public void
    testMaps()
            throws Exception {
        testAll("checkMaps");
    }

    private static final Set SUITABLE_TYPES = GSetUtil.newUnmodifiableStringSet(
            Void.class.getName(),
            Object.class.getName(),

            // these are quick checks--other classes may be OK too
            "boolean", "byte", "char", "short", "int", "long", "void",

            boolean[].class.getName(),
            char[].class.getName(),
            byte[].class.getName(),
            short[].class.getName(),
            int[].class.getName(),
            long[].class.getName(),
            Object[].class.getName(),

            Boolean.class.getName(),
            Character.class.getName(),
            Byte.class.getName(),
            Short.class.getName(),
            Integer.class.getName(),
            Long.class.getName(),

            String.class.getName(),
            String[].class.getName(),

            Date.class.getName(),

            ObjectName.class.getName(),
            ObjectName[].class.getName(),

            Set.class.getName(),
            List.class.getName(),
            Map.class.getName(),

            java.util.logging.Level.class.getName(),
            java.io.File.class.getName(),

            // these are passed as Maps, but declared as their proper types
            // in the interface
            WebServiceEndpointInfo.class.getName(),
            LogQueryResult.class.getName(),
            MessageTrace.class.getName()
    );


    /**
     Verify that the type is suitable for the API.  It must meet the following constraints
     <ul>
     <li>that it is an OpenType or a standard Java type or a JMX type</li>
     <li>that it is Serializable or an interface</li>
     <li>or that it is an array whose elements meet the above constraints</li>
     <li>or that it is one of our specific Stats types</li>
     </ul>
     */
    private boolean
    isSuitableReturnTypeForAPI(final String type) {
        boolean isSuitable = SUITABLE_TYPES.contains(type);

        if (!isSuitable) {
            final boolean isArray = ClassUtil.classnameIsArray(type);

            if (isArray ||
                    type.startsWith("java.") || type.startsWith("javax.management.")) {
                Class c = null;
                try {
                    c = ClassUtil.getClassFromName(type);
                    isSuitable = c.isInterface() || Serializable.class.isAssignableFrom(c) ||
                            c == Object.class;
                }
                catch (ClassNotFoundException e) {
                    trace("WARNING: can't find class for type: " + type);
                    isSuitable = false;
                }

                if (isArray) {
                    final Class elementClass = ClassUtil.getArrayElementClass(c);
                    isSuitable = isSuitableReturnTypeForAPI(elementClass.getName());
                } else if (isSuitable &&
                        (!type.startsWith("javax.")) &&
                        !c.isInterface()) {
                    // insist on an interface except for those types explicit in SUITABLE_TYPES
                    isSuitable = false;
                }
            } else if (type.endsWith("Stats")) {
                isSuitable = type.startsWith("com.sun.appserv.management.monitor.statistics") ||
                        type.startsWith("org.glassfish.j2ee.statistics");
            }
        }

        return (isSuitable);
    }


    /**
     Verify:
     <ul>
     <li>that all return types are suitable for the API</li>
     </ul>
     */
    public void
    checkReturnTypes(final ObjectName objectName)
            throws Exception {
        final AMX proxy = getProxyFactory().getProxy(objectName, AMX.class);
        final MBeanInfo info = Util.getExtra(proxy).getMBeanInfo();
        final MBeanOperationInfo[] operations = info.getOperations();

        boolean emittedName = false;

        for (int i = 0; i < operations.length; ++i) {
            final MBeanOperationInfo opInfo = operations[i];

            final String returnType = opInfo.getReturnType();
            if (!isSuitableReturnTypeForAPI(returnType)) {
                if (!emittedName) {
                    emittedName = true;
                    trace("\n" + objectName);
                }

                trace("WARNING: unsuitable return type in API: " +
                        returnType + " " + opInfo.getName() + "(...)");
            }
        }
    }


    public void
    testReturnTypes()
            throws Exception {
        testAll("checkReturnTypes");
    }


    /**
     Verify:
     <ul>
     <li>that all Attributes are of standard types and Serializable</li>
     </ul>
     */
    public void
    checkAttributeTypes(final ObjectName objectName)
            throws Exception {
        final AMX proxy = getProxyFactory().getProxy(objectName, AMX.class);
        final MBeanInfo info = Util.getExtra(proxy).getMBeanInfo();
        final MBeanAttributeInfo[] attributes = info.getAttributes();

        boolean emittedName = false;

        for (int i = 0; i < attributes.length; ++i) {
            final MBeanAttributeInfo attrInfo = attributes[i];

            final String type = attrInfo.getType();
            if (!isSuitableReturnTypeForAPI(type)) {
                if (!emittedName) {
                    emittedName = true;
                }

                if (!type.equals(CoverageInfo.class.getName())) {
                    trace("WARNING: unsuitable Attribute type in API: " +
                            type + " " + attrInfo.getName() + " in " + objectName);
                }
            }
        }
    }

    public void
    testAttributeTypes()
            throws Exception {
        testAll("checkAttributeTypes");
    }

    /**
     Verify:
     <ul>
     <li>each create() or createAbc() method ends in "Config" if it returns an AMXConfig subclass</li>
     <li>each remove() or removeAbc() method ends in "Config"</li>
     </ul>
     */
    public void
    checkCreateRemoveGet(final ObjectName objectName)
            throws Exception {
        final AMX proxy = getProxyFactory().getProxy(objectName, AMX.class);
        if (proxy instanceof Container) {
            final Method[] methods = getInterfaceClass(proxy).getMethods();
            final MBeanInfo mbeanInfo = Util.getExtra(proxy).getMBeanInfo();
            final MBeanOperationInfo[] operations = mbeanInfo.getOperations();

            for (int methodIdx = 0; methodIdx < methods.length; ++methodIdx) {
                final Method method = methods[methodIdx];
                final String methodName = method.getName();

                if (methodName.startsWith("create") && !methodName.endsWith("Config")) {
                    if (AMXConfig.class.isAssignableFrom(method.getReturnType()) &&
                            (!(proxy instanceof SecurityMapConfig))) {
                        trace("WARNING: method " + methodName + " does not end in 'Config': " + objectName);
                    }
                } else if (methodName.startsWith("remove") &&
                        !methodName.endsWith("Config") &&
                        proxy instanceof AMXConfig) {
                    if (     //method.getReturnType() == Void.class &&
                            method.getParameterTypes().length == 1 &&
                                    method.getParameterTypes()[0] == String.class &&
                                    !method.getName().equals("removeProperty") &&
                                    !method.getName().equals("removeSystemProperty") &&
                                    (!(proxy instanceof SecurityMapConfig))) {
                        trace("WARNING: method " + methodName + " does not end in 'Config': " + methodName);
                    }
                }
            }
        }
    }


    public void
    testCreateRemoveGet()
            throws Exception {
        testAll("checkCreateRemoveGet");
    }

    /**
     Verify:
     <ul>
     <li>if the interface name ends in "Config" or "ConfigMgr", then is is an AMXConfig</li>
     </ul>
     */
    public void
    checkImplementsAMXConfig(final ObjectName objectName)
            throws Exception {
        final AMX proxy = getProxyFactory().getProxy(objectName, AMX.class);
        final String interfaceName = Util.getExtra(proxy).getInterfaceName();
        if (interfaceName.endsWith("Config") || interfaceName.endsWith("ConfigMgr")) {
            if (!(proxy instanceof AMXConfig)) {
                trace("WARNING: " + ClassUtil.stripPackageName(interfaceName) + " does not implement AMXConfig");
            }
        }
    }


    /**
     A few items supply Map of things, but have no corresponding create/remove routines.
     */
    private boolean
    ignoreCreateRemove(
            final String j2eeType,
            final String suggestedMethod) {
        boolean ignore = false;

        if (j2eeType.equals(XTypes.DOMAIN_CONFIG)) {
            if (suggestedMethod.equals("createServerConfig") ||
                    suggestedMethod.equals("createWebModuleConfig") ||
                    suggestedMethod.equals("createEJBModuleConfig") ||
                    suggestedMethod.equals("createJ2EEApplicationConfig") ||
                    suggestedMethod.equals("createRARModuleConfig") ||
                    suggestedMethod.equals("createAppClientModuleConfig") ||
                    suggestedMethod.equals("createNodeAgentConfig") ||
                    false
                    ) {
                ignore = true;
            }
        } else if (j2eeType.equals(XTypes.CLUSTERED_SERVER_CONFIG)) {
            if (suggestedMethod.equals("createDeployedItemRefConfig") ||
                    suggestedMethod.equals("createResourceRefConfig")
                    ) {
                ignore = true;
            }
        }

        return (ignore);
    }

    /**
     Verify that all getAbcConfigMgr() calls return a non-null result.
     */
    public void
    checkMapsHaveCreateRemove(final ObjectName objectName)
            throws Exception {
        final AMX proxy = getProxyFactory().getProxy(objectName, AMX.class);

        if (proxy instanceof Container && proxy.getGroup().equals(AMX.GROUP_CONFIGURATION)) {
            final Extra extra = Util.getExtra(proxy);
            final String[] attrNames = extra.getAttributeNames();

            for (int i = 0; i < attrNames.length; ++i) {
                final String name = attrNames[i];

                final String SUFFIX = "ObjectNameMap";
                final String PREFIX = JMXUtil.GET;
                if (name.endsWith(SUFFIX)) {
                    final String base = StringUtil.stripPrefixAndSuffix(name, PREFIX, SUFFIX);

                    if (base.endsWith("ConnectorModuleConfig")) {
                        // these are created via deployment not directly
                        continue;
                    }

                    final String createName = "create" + base;
                    final String removeName = "remove" + base;

                    final String j2eeType = proxy.getJ2EEType();
                    if (ignoreCreateRemove(proxy.getJ2EEType(), createName)) {
                        continue;
                    }

                    final MBeanOperationInfo[] creates =
                            JMXUtil.findOperations(extra.getMBeanInfo().getOperations(), createName);
                    boolean haveCreate = false;
                    for (int op = 0; op < creates.length; ++op) {
                        final MBeanOperationInfo info = creates[op];
                        if (info.getReturnType().equals(ObjectName.class.getName())) {
                            haveCreate = true;
                            break;
                        }
                    }
                    assert (haveCreate) :
                            "Missing operation " + createName + "() for " + objectName;

                    final MBeanOperationInfo[] removes =
                            JMXUtil.findOperations(extra.getMBeanInfo().getOperations(), removeName);
                    boolean haveRemove = false;
                    for (int op = 0; op < removes.length; ++op) {
                        final MBeanOperationInfo info = removes[op];
                        if (info.getReturnType().equals("void") &&
                                info.getSignature().length <= 2) {
                            haveRemove = true;
                            break;
                        }
                    }
                    assert (haveRemove) :
                            "Missing operation " + removeName + "() for " + objectName;
                }
            }
        }
    }

    public void
    testMapsHaveCreateRemove()
            throws Exception {
        testAll("checkMapsHaveCreateRemove");
    }


    public void
    testImplementsAMXConfig()
            throws Exception {
        testAll("checkImplementsAMXConfig");
    }


    private static Set<Class> MON_IGNORE = GSetUtil.newUnmodifiableSet(new Class[]
            {
                    JMXMonitorMgr.class,
                    AMXStringMonitor.class,
                    AMXCounterMonitor.class,
                    AMXGaugeMonitor.class,

                    EJBModuleMonitor.class,
                    HTTPServiceMonitor.class,
                    ApplicationMonitor.class,
            });

    /**
     Verify:
     <ul>
     <li>verify that if the interface name ends in "Monitor", then it is an AMX, Monitoring</li>
     <li>verify that if the interface name ends in "MonitorMgr", then it is an Container</li>
     </ul>
     */
    public void
    testImplementsAMXMonitoring()
            throws Exception {
/*
		final TypeInfos	infos	= TypeInfos.getInstance();
		
		final Iterator	iter	= infos.getJ2EETypes().iterator();
		while ( iter.hasNext() )
		{
			final TypeInfo	info	= infos.getInfo( (String)iter.next() );
			final Class		theInterface	= info.getInterface();
			final String	interfaceName	= theInterface.getName();
			if ( ! MON_IGNORE.contains( theInterface ) )
			{
				if ( interfaceName.endsWith( "Monitor" ) )
				{
					if ( ! Monitoring.class.isAssignableFrom( theInterface ) )
					{
						warning( ClassUtil.stripPackageName( interfaceName ) + " does not implement Monitoring" );
					}
				}
				else if ( interfaceName.endsWith( "MonitorMgr" ) )
				{
					if ( ! Container.class.isAssignableFrom( theInterface ) )
					{
						warning( ClassUtil.stripPackageName( interfaceName ) + " does not implement Container" );
					}
				}
			}
		}
*/
    }

    public void
    testGetInterfaceName()
            throws IOException, JMException {
        final Set<ObjectName> all = getQueryMgr().queryAllObjectNameSet();

        final MBeanServerConnection conn =
                Util.getExtra(getDomainRoot()).getConnectionSource().getExistingMBeanServerConnection();

        final Set<ObjectName> failedSet = new HashSet<ObjectName>();

        for (final ObjectName objectName : all) {
            try {
                final String value = (String)
                        conn.getAttribute(objectName, AMXAttributes.ATTR_INTERFACE_NAME);
                assert (value != null);
                value.toString();
            }
            catch (AttributeNotFoundException e) {
                warning("Can't get InterfaceName for: " + objectName);
                failedSet.add(objectName);
            }
        }

        if (failedSet.size() != 0) {
            warning("The following MBeans did not return the Attribute InterfaceName:\n" +
                    CollectionUtil.toString(failedSet, "\n"));
            assert (false);
            throw new Error();
        }
    }


    public void
    testInterfaceAgainstDelegate()
            throws Exception {
        final long start = now();
        final Set<AMX> all = getAllAMX();

        final MBeanServerConnection conn = getMBeanServerConnection();
        for (final AMX amx : all) {

            final String result = (String)
                    conn.invoke(Util.getObjectName(amx),
                                "checkInterfaceAgainstDelegate", null, null);
        }

        printElapsed("testInterfaceAgainstDelegate", all.size(), start);
    }

    public void
    testMisc() {
        final long start = now();
        final Set<AMX> all = getAllAMX();

        for (final AMX amx : all) {
            amx.setMBeanLogLevel(amx.getMBeanLogLevel());

            final ObjectName objectName = Util.getObjectName(amx);
            assert (objectName.getKeyProperty(AMX.NAME_KEY) != null);
            assert (objectName.getKeyProperty(AMX.J2EE_TYPE_KEY) != null);
        }

        printElapsed("testMisc", all.size(), start);
    }


    public void
    testNoGoofyNames(
            final ObjectName objectName,
            final MBeanFeatureInfo[] featureInfos) {
        final Set<String> goofy = new HashSet<String>();

        for (final MBeanFeatureInfo info : featureInfos) {
            final String name = info.getName();

            if (name.indexOf("ObjectNameObjectName") >= 0) {
                goofy.add(name);
            }
        }

        if (goofy.size() != 0) {
            assert (false) : NEWLINE +
                    "MBean " + objectName + " has the following goofy Attributes:" + NEWLINE +
                    CollectionUtil.toString(goofy, NEWLINE);
        }
    }

    public void
    testNoGoofyNames() {
        final long start = now();
        final Set<AMX> all = getAllAMX();

        for (final AMX amx : all) {
            final ObjectName objectName = Util.getObjectName(amx);
            final MBeanInfo mbeanInfo = Util.getExtra(amx).getMBeanInfo();

            testNoGoofyNames(objectName, mbeanInfo.getAttributes());
            testNoGoofyNames(objectName, mbeanInfo.getOperations());
        }

        printElapsed("testNoGoofyNames", all.size(), start);
    }


    public void
    testToString() {
        final long start = now();
        final Set<AMX> all = getAllAMX();

        for (final AMX amx : all) {
            final AMXDebugStuff debug = getTestUtil().asAMXDebugStuff(amx);

            if (debug != null) {
                final String s  = debug.getImplString( true );
	            assert( s.length() != 0 );
	        }
	    }
	    
	    printElapsed( "testToString", all.size(), start );
	}
}
















