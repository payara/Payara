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
*/

package org.glassfish.admin.amxtest;

import com.sun.appserv.management.DomainRoot;
import com.sun.appserv.management.base.Util;
import com.sun.appserv.management.client.AppserverConnectionSource;
import com.sun.appserv.management.client.ConnectionSource;
import com.sun.appserv.management.client.HandshakeCompletedListenerImpl;
import com.sun.appserv.management.client.ProxyFactory;
import com.sun.appserv.management.client.TLSParams;
import com.sun.appserv.management.util.jmx.JMXUtil;
import com.sun.appserv.management.util.jmx.ObjectNameComparator;
import com.sun.appserv.management.util.stringifier.SmartStringifier;
import junit.extensions.ActiveTestSuite;
import junit.framework.TestSuite;
import org.glassfish.admin.amxtest.JMXTestBase;

import javax.management.JMException;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 Class that supports running all the unit tests for AMX
 */
public final class TestRunner {
    final ConnectionSource mConn;
    boolean mVerbose;

    public TestRunner(final ConnectionSource conn) {
        mConn = conn;
        mVerbose = false;
    }


    public int
    runSuite(
            String name,
            TestSuite suite) {
        System.out.println("*** testing " + name + " ***");

        junit.textui.TestRunner runner = new junit.textui.TestRunner();
        junit.framework.TestResult result = runner.doRun(suite, false);

        return (result.failureCount());
    }

    public int
    testClass(final Class<junit.framework.TestCase> theClass) {
        final TestSuite suite = new TestSuite(theClass);
        return (runSuite(theClass.getName(), suite));
    }


    public int
    testClassThreaded(final Class<junit.framework.TestCase> theClass) {
        final TestSuite suite = new ActiveTestSuite(theClass);

        return (runSuite(theClass.getName(), suite));
    }


    public void
    runTests(
            List<Class<junit.framework.TestCase>> testClasses,
            boolean threaded)
            throws Exception {
        for (final Class<junit.framework.TestCase> theClass : testClasses) {
            final int failureCount = threaded ?
                    testClassThreaded(theClass) :
                    testClass(theClass);

            if (failureCount != 0) {
                println("Test " + theClass.getName() + " had failures: " + failureCount);
            }
        }
    }

    public long
    elapsed(final long start) {
        return (System.currentTimeMillis() - start);
    }

    private long
    testGetMBeanInfoSpeed(
            final MBeanServerConnection conn,
            final ObjectName[] objectNames)
            throws IOException, JMException {
        // sorting provides consistent, ordered output
        Arrays.sort(objectNames, ObjectNameComparator.INSTANCE);

        final long startAll = System.currentTimeMillis();
        for (int i = 0; i < objectNames.length; ++i) {
            final ObjectName objectName = objectNames[i];

            final long start = System.currentTimeMillis();

            final MBeanInfo mbeanInfo = conn.getMBeanInfo(objectName);

            final long elapsed = elapsed(start);

            String id = objectName.toString();
            String value;

            if ((value = objectName.getKeyProperty("type")) != null) {
                id = value;
            } else if ((value = objectName.getKeyProperty("j2eeType")) != null) {
                id = value;
            }

            if ((value = objectName.getKeyProperty("name")) != null) {
                id = Util.concatenateProps(id, Util.makeNameProp(value));
            }

            //printVerbose( "GetMBeanInfo time for " + id + " = " + elapsed );
        }

        final long elapsed = System.currentTimeMillis() - startAll;
        return (elapsed);
    }


    protected void
    printVerbose(final Object o) {
        if (mVerbose) {
            println(o);
        }
    }

    private void
    println(final Object o) {
        System.out.println(toString(o));
    }

    private void
    print(final Object o) {
        System.out.print(toString(o));
    }

    private void
    testGetMBeanInfoSpeed(
            final MBeanServerConnection conn,
            final String domain,
            final String props)
            throws IOException, JMException {
        final ObjectName pattern = Util.newObjectNamePattern(domain, props);
        final Set<ObjectName> objectNameSet = JMXUtil.queryNames(conn, pattern, null);

        final ObjectName[] objectNames = new ObjectName[objectNameSet.size()];
        objectNameSet.toArray(objectNames);

        final long elapsed = testGetMBeanInfoSpeed(conn, objectNames);

        println("Time to getMBeanInfo on " + domain + ":" + props + " (" + objectNames.length + " MBeans)" +
                " = " + elapsed + "ms");
    }

    public MBeanServerConnection
    getMBeanServerConnection()
            throws IOException {
        return (mConn == null ? null : mConn.getMBeanServerConnection(false));
    }

    public void
    testSpeed()
            throws IOException, JMException {
        final DomainRoot domainRootProxy = ProxyFactory.getInstance(mConn).
                createDomainRoot();

        final MBeanServerConnection conn = getMBeanServerConnection();

        testGetMBeanInfoSpeed(conn, Util.getObjectName(domainRootProxy).getDomain(), JMXUtil.WILD_ALL);
    }


    /**
     Comment in call to this for hard-coded test.
     */
    private void
    testAppserverConnectionSource(
            final String host,
            final String user,
            final String password)
            throws IOException {
        MBeanServerConnection conn = null;

        final TestClientTrustStoreTrustManager tm = new TestClientTrustStoreTrustManager();
        final HandshakeCompletedListenerImpl hcl = new HandshakeCompletedListenerImpl();
        tm.setPrompt(true);
        final TLSParams tlsParams =
                new TLSParams(new X509TrustManager[]{tm}, hcl);

        println("\ntestAppserverConnectionSource: testing: " + AppserverConnectionSource.PROTOCOL_RMI);

        final ConnectionSource rmiSource =
                new AppserverConnectionSource(AppserverConnectionSource.PROTOCOL_RMI,
                                              host, 8686, user, password, null);
        conn = rmiSource.getMBeanServerConnection(true);
        conn.isRegistered(JMXUtil.getMBeanServerDelegateObjectName());

        println(AppserverConnectionSource.PROTOCOL_RMI + " OK using " + rmiSource);


        println("\ntestAppserverConnectionSource: testing: " + AppserverConnectionSource.PROTOCOL_HTTP);
        final Map<String, String> env = Collections.emptyMap();

        final ConnectionSource httpSource =
                new AppserverConnectionSource(AppserverConnectionSource.PROTOCOL_HTTP,
                                              host, 1234, user, password, tlsParams, env);
        conn = httpSource.getMBeanServerConnection(true);
        assert conn.isRegistered(JMXUtil.getMBeanServerDelegateObjectName());

        println(AppserverConnectionSource.PROTOCOL_HTTP + " OK using " + httpSource);

    }


    public static String
    toString(Object o) {
        return (SmartStringifier.toString(o));
    }

    /**
     @param threaded if true, run the tests from each TestCase in separate threads
     @param env      arbitrary environment values for JMXTestBase
     */
    protected void
    runAll(
            final List<Class<junit.framework.TestCase>> testClasses,
            final boolean threaded,
            final Map<String, Object> env)
            throws Exception {
        mVerbose = Boolean.valueOf((String) env.get(PropertyKeys.VERBOSE_KEY)).booleanValue();

        //testSpeed();

        // use the current connection which must contain the AMX MBeans
        final MBeanServerConnection conn = getMBeanServerConnection();

        JMXTestBase.setGlobalConnection(conn);
        JMXTestBase.setEnvValues(env);

        println("\n--- " + testClasses.size() + " TEST CLASSES ---");

        for (final Class<junit.framework.TestCase> theClass : testClasses) {
            println(theClass.getName());
        }

        println("\n--- BEGIN TESTS ---");
        runTests(testClasses, threaded);
	}
}






