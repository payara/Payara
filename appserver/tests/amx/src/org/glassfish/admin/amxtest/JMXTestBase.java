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

package org.glassfish.admin.amxtest;

import com.sun.appserv.management.util.jmx.JMXUtil;
import com.sun.appserv.management.util.misc.CollectionUtil;
import com.sun.appserv.management.util.misc.StringUtil;
import com.sun.appserv.management.util.stringifier.SmartStringifier;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 Base class for AMX unit tests.
 */
public class JMXTestBase
        extends junit.framework.TestCase {
    private static MBeanServerConnection _GlobalMBeanServerConnection;
    private static Map<String, Object> sEnv;
    protected final String NEWLINE;

    private static final MBeanServer TEST_MBEAN_SERVER = MBeanServerFactory.newMBeanServer("JMXTestBase_temp");
    /**
        Get an in-JVM MBeanServer for testing.
    */
        protected MBeanServer
    getTestMBeanServer()
    {
        return TEST_MBEAN_SERVER;
    }

    /**
        Set the global MBeanServerConnection.  This is to support testing to a
        remote host.
     */
    public static synchronized void setGlobalConnection(final MBeanServerConnection conn) {
        _GlobalMBeanServerConnection = conn;
    }
    
    public static synchronized MBeanServerConnection
    getGlobalMBeanServerConnection() {
        return _GlobalMBeanServerConnection;
    }
    
    public static MBeanServerConnection
    getMBeanServerConnection() {
        return getGlobalMBeanServerConnection();
    }

    protected <T> T
    newProxy(
            final ObjectName target,
            final Class<T> interfaceClass) {
        try {
            assert getGlobalMBeanServerConnection().isRegistered(target);
        }
        catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }

        return interfaceClass.cast(MBeanServerInvocationHandler.newProxyInstance(
                getGlobalMBeanServerConnection(), target, interfaceClass, true));
    }

    public static synchronized Object
    getEnvValue(final String key) {
        return (sEnv == null ? null : sEnv.get(key));
    }

    public static Integer
    getEnvInteger(
            final String key,
            Integer defaultValue) {
        final String s = getEnvString(key, null);
        Integer result = defaultValue;
        if (s != null) {
            result = new Integer(s.trim());
        }

        return (result);
    }

    public static String
    getEnvString(
            final String key,
            final String defaultValue) {
        final String s = (String) getEnvValue(key);

        return (s == null ? defaultValue : s);
    }


    public static Boolean
    getEnvBoolean(
            final String key,
            final Boolean defaultValue) {
        Boolean result = defaultValue;
        final String s = getEnvString(key, null);
        if (s != null) {
            result = Boolean.valueOf(s);
        }

        return (result);
    }


    private static synchronized void
    initEnv() {
        if (sEnv == null) {
            sEnv = new HashMap<String, Object>();
        }
    }

    public static synchronized void
    setEnvValue(
            final String key,
            final Object value) {
        initEnv();
        sEnv.put(key, value);
    }

    public static synchronized void
    setEnvValues(final Map<String, Object> m) {
        initEnv();
        sEnv.putAll(m);
    }


    public JMXTestBase() {
        super("JMXTestBase");

        NEWLINE = StringUtil.NEWLINE();

        checkAssertsOn();
    }

    public JMXTestBase(String name) {
        super(name);
        NEWLINE = StringUtil.NEWLINE();
        checkAssertsOn();
    }


    protected String
    toString(final ObjectName objectName) {
        return JMXUtil.toString(objectName);
    }

    protected String
    toString(final Object o) {
        String result = null;

        if (o instanceof Collection) {
            result = CollectionUtil.toString((Collection) o, "\n");
        } else {
            result = SmartStringifier.toString(o);
        }

        return (result);
    }


    protected static void
    trace(final Object o) {
        System.out.println(SmartStringifier.toString(o));
    }

    protected void
    println(final Object o) {
        System.out.println(SmartStringifier.toString(o));
    }

    protected long
    now() {
        return (System.currentTimeMillis());
    }

    protected final void
    printElapsed(
            final String msg,
            final long start) {
        printVerbose(msg + ": " + (now() - start) + "ms");
    }

    protected final void
    printElapsedIter(
            final String msg,
            final long start,
            final long iterations) {
        printVerbose(msg + "(" + iterations + " iterations): " + (now() - start) + "ms");
    }

    protected final void
    printElapsed(
            final String msg,
            final int numItems,
            final long start) {
        printVerbose(msg + ", " + numItems + " MBeans: " + (now() - start) + "ms");
    }


    protected final String
    quote(final Object o) {
        return (StringUtil.quote(SmartStringifier.toString(o)));
    }
    
    
        protected boolean
    getVerbose() {
        /*
        final String value = (String) getEnvValue(PropertyKeys.VERBOSE_KEY);

        return (value != null && Boolean.valueOf(value).booleanValue());
        */
        return false;
    }

    protected void
    printVerbose(final Object o) {
        if (getVerbose()) {
            trace(o);
        }
    }


    protected void
    warning(final String msg) {
        trace("\nWARNING: " + msg + "\n");
    }

    protected void
    failure(final String msg) {
        trace("\nFAILURE: " + msg + "\n");
        assert (false) : msg;
        throw new Error(msg);
    }

    protected void
    checkAssertsOn() {
        try {
            assert (false);
            throw new Error("Assertions must be enabled for unit tests");
        }
        catch (AssertionError a) {
        }
    }


    protected void
    registerMBean(
            Object mbean,
            String name)
            throws MalformedObjectNameException, InstanceAlreadyExistsException,
            NotCompliantMBeanException, MBeanRegistrationException {
           getTestMBeanServer().registerMBean(mbean, new ObjectName(name));
    }
};

