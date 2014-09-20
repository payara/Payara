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

/*
* $Header: /cvs/glassfish/admin/mbeanapi-impl/tests/org.glassfish.admin.amxtest/client/ProxyTest.java,v 1.10 2007/05/05 05:23:54 tcfujii Exp $
* $Revision: 1.10 $
* $Date: 2007/05/05 05:23:54 $
*/
package org.glassfish.admin.amxtest.client;

import com.sun.appserv.management.DomainRoot;
import com.sun.appserv.management.base.AMX;
import com.sun.appserv.management.base.QueryMgr;
import com.sun.appserv.management.base.Util;
import com.sun.appserv.management.util.jmx.JMXUtil;
import com.sun.appserv.management.util.misc.ExceptionUtil;
import org.glassfish.admin.amxtest.AMXTestBase;
import org.glassfish.admin.amxtest.Capabilities;

import javax.management.AttributeNotFoundException;
import javax.management.ObjectName;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 */
public final class ProxyTest
        extends AMXTestBase {
    public ProxyTest() {
    }

    public static Capabilities
    getCapabilities() {
        return getOfflineCapableCapabilities(false);
    }


    public void
    checkCreateProxy(final ObjectName src)
            throws Exception {
        final AMX proxy = getProxyFactory().getProxy(src, AMX.class);

        Util.getObjectName(proxy);
        proxy.getContainer();
        proxy.getDomainRoot();
    }

    public void
    testCreateAllProxies()
            throws Exception {
        testAll("checkCreateProxy");
    }

    public void
    checkProxiesCached(final ObjectName src)
            throws Exception {
        final AMX proxy = getProxyFactory().getProxy(src, AMX.class);

        assert (proxy == getProxyFactory().getProxy(src, AMX.class));
        assert (proxy.getContainer() == proxy.getContainer());
        assert (proxy.getDomainRoot() == proxy.getDomainRoot());

        final Class interfaceClass = getInterfaceClass(proxy);
        final Method[] proxyMethods = interfaceClass.getMethods();

        for (int methodIdx = 0; methodIdx < proxyMethods.length; ++methodIdx) {
            final Method method = proxyMethods[methodIdx];
            final String methodName = method.getName();

            if (isProxyGetter(method)) {
                // invoke it twice, and verify that the 2nd call results in the same proxy
                //trace( "Invoking: " + method );
                method.invoke(proxy, (Object[]) null);
            }
        }
    }

    public void
    testProxiesCached()
            throws Exception {
        testAll("checkProxiesCached");
    }


    private boolean
    isProxyGetter(final Method method) {
        return (
                method.getName().startsWith(JMXUtil.GET) &&
                        method.getParameterTypes().length == 0 &&
                        AMX.class.isAssignableFrom(method.getReturnType()));
    }

    private boolean
    isChildProxyGetter(final Method method) {
        final Class[] paramTypes = method.getParameterTypes();

        return (
                paramTypes.length == 1 &&
                        paramTypes[0] == String.class &&
                        AMX.class.isAssignableFrom(method.getReturnType()));
    }

    private boolean
    isProxiesGetter(final Method method) {
        return (
                method.getParameterTypes().length == 0 &&
                        Set.class.isAssignableFrom(method.getReturnType()));
    }


    private String
    getProxyGetterName(final String getterName) {
        final int baseLength = getterName.length() - "ObjectName".length();
        final String baseName = getterName.substring(0, baseLength);

        return (baseName + "Proxy");
    }


    public void
    testProxyInterfaceIsAMX()
            throws Exception {/*
		final long	start	= now();
		final TypeInfos	infos	= TypeInfos.getInstance();
		
		final Iterator	iter	= infos.getJ2EETypes().iterator();
		while ( iter.hasNext() )
		{
			final TypeInfo	info	= infos.getInfo( (String)iter.next() );
			final Class	proxyClass	= info.getInterface();
			
			if ( ! AMX.class.isAssignableFrom( proxyClass ) )
			{
				warning( "Proxy interface does not extend AMX: " + proxyClass.getName() );
			}
		}
		printElapsed( "testProxyInterfaceNameConsistent", start );
		*/
    }


    /**
     Verify that every getXXX() method can be called (those without parameters).
     */
    public void
    testProxyGetters(final AMX proxy)
            throws ClassNotFoundException {
        final Method[] methods = getInterfaceClass(proxy).getMethods();

        final List<Method> failedMethods = new ArrayList<Method>();
        final List<Throwable> exceptions = new ArrayList<Throwable>();

        final long start = now();

        for (int methodIdx = 0; methodIdx < methods.length; ++methodIdx) {
            final Method method = methods[methodIdx];
            final String methodName = method.getName();
            final Class[] parameterTypes = method.getParameterTypes();

            if (methodName.startsWith(JMXUtil.GET) && parameterTypes.length == 0) {
                try {
                    final Object result = method.invoke(proxy, (Object[]) null);
                    //trace( methodName + "=" + result);
                }
                catch (Throwable t) {
                    final ObjectName objectName = Util.getObjectName(proxy);
                    if (isRemoteIncomplete(objectName)) {
                        trace("remoteIncomplete: " + objectName);
                    } else {
                        trace("failure: " + methodName + " = " + t.getClass().getName() + " on MBean " + objectName );
                        failedMethods.add(method);
                        exceptions.add(t);
                    }
                }
            }
        }
        final long elapsed = now() - start;
        //printVerbose( "testProxyGetters for: " + Util.getObjectName( proxy ) + " = " + elapsed );

        if (failedMethods.size() != 0) {
            final int numFailed = failedMethods.size();

            trace("\nMBean \"" + Util.getObjectName(proxy) + "\" failed for:");
            for (int i = 0; i < numFailed; ++i) {
                final Method m = (Method) failedMethods.get(i);
                final Throwable t = (Throwable) exceptions.get(i);

                final Throwable rootCause = ExceptionUtil.getRootCause(t);
                final String rootTrace = ExceptionUtil.getStackTrace(rootCause);
                final Class rootCauseClass = rootCause.getClass();

                trace("testProxyGetters: failure from: " + m.getName() + ": " + rootCauseClass.getName());
                if (rootCauseClass != AttributeNotFoundException.class) {
                    trace(rootTrace + "\n");
                }
            }
        }
    }

    public void
    testAllGetters()
            throws Exception {
        final long start = now();

        final Set<AMX> proxies = getAllAMX();
        for (final AMX amx : proxies) {
            testProxyGetters(amx);
        }

        printElapsed("testAllGetters", start);
    }


    public void
    testQueryMgr()
            throws Exception {
        final QueryMgr proxy = (QueryMgr) getQueryMgr();
        Util.getObjectName(proxy);
        proxy.getContainer();
        proxy.getDomainRoot();
    }

    public void
    testDomainRootCachedProxies()
            throws Exception {
        final DomainRoot root = (DomainRoot) getDomainRoot();

        assert (root.getJ2EEDomain() == root.getJ2EEDomain());
        assert (root.getDomainConfig() == root.getDomainConfig());
        assert (root.getQueryMgr() == root.getQueryMgr());
        assert (root.getBulkAccess() == root.getBulkAccess());
        assert (root.getUploadDownloadMgr() == root.getUploadDownloadMgr());
        assert (root.getDottedNames() == root.getDottedNames());

        assert (root.getJ2EEDomain() == root.getJ2EEDomain());
    }

    /**
     This test is designed to check that performance is reasonable and/or
     to detect a change that slows things down drastically.
     public void
     */
    public void
    testProxyTime()
            throws Exception {
        final DomainRoot root = (DomainRoot) getDomainRoot();

        final long start = now();
        for (int i = 0; i < 5; ++i) {
            root.getContainer();
            root.getDomainRoot();

            root.getJ2EEDomain();
            root.getDomainConfig();
            root.getQueryMgr();
            root.getBulkAccess();
            root.getUploadDownloadMgr();
            root.getDottedNames();
        }
        final long elapsed = now() - start;

        // should be < 300 ms, so this is a 10X margin...
        assert (elapsed < 300 * 10 );
	}
}





