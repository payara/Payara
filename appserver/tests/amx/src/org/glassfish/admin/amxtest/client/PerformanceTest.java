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
* $Header: /cvs/glassfish/admin/mbeanapi-impl/tests/org.glassfish.admin.amxtest/client/PerformanceTest.java,v 1.8 2007/05/05 05:23:53 tcfujii Exp $
* $Revision: 1.8 $
* $Date: 2007/05/05 05:23:53 $
*/
package org.glassfish.admin.amxtest.client;

import com.sun.appserv.management.DomainRoot;
import com.sun.appserv.management.base.AMX;
import com.sun.appserv.management.base.QueryMgr;
import com.sun.appserv.management.base.Sample;
import com.sun.appserv.management.base.Util;
import com.sun.appserv.management.base.XTypes;
import com.sun.appserv.management.config.DomainConfig;
import com.sun.appserv.management.config.SSLConfig;
import com.sun.appserv.management.util.jmx.JMXUtil;
import com.sun.appserv.management.util.misc.ClassUtil;
import com.sun.appserv.management.util.stringifier.ArrayStringifier;
import org.glassfish.admin.amxtest.AMXTestBase;
import org.glassfish.admin.amxtest.Capabilities;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 Note that the tests are synchronized so that the performance numbers
 are not affected by concurrent tests.
 */
public final class PerformanceTest
        extends AMXTestBase {
    private static boolean BASELINE_DONE = false;

    private static final int K = 1024;
    private static final int MB = K * K;

    private void
    printPerf(final String s) {
        trace(s);
    }

    public static Capabilities
    getCapabilities() {
        return getOfflineCapableCapabilities(true);
    }

    public void
    baselineTest(final MBeanServerConnection conn)
            throws IOException {
        if (!BASELINE_DONE) {
            synchronized (PerformanceTest.class) {
                BASELINE_DONE = true;

                printPerf("--- Baseline statistics for connection --- ");

                final ObjectName delegateObjectName = JMXUtil.getMBeanServerDelegateObjectName();
                final int ITER = 1000;
                final long start = now();
                for (int i = 0; i < ITER; ++i) {
                    conn.isRegistered(delegateObjectName);
                }
                printPerf("Time to call MBeanServerConnection.isRegistered() " + ITER + " times: " + (now() - start) + " ms");

                final Sample sample = (Sample) getDomainRoot().getContainee(XTypes.SAMPLE);

                final int BANDWIDTH_ITER = 3;

                for (int i = 0; i < BANDWIDTH_ITER; ++i) {
                    // test upload bandwidth
                    final byte[] uploadBytes = new byte[1 * MB];
                    final long uploadStart = now();
                    sample.uploadBytes(uploadBytes);
                    final long uploadElapsed = now() - uploadStart;
                    final int uploadKBPerSec = (int) ((uploadBytes.length / 1024.0) / (uploadElapsed / 1000.0));
                    printPerf("Upload bandwidth (" + uploadBytes.length + " bytes): " + uploadKBPerSec + "kb/sec");

                    // test download bandwidth
                    final long downloadStart = now();
                    final byte[] downloadedBytes = sample.downloadBytes(256 * K);
                    final long downloadElapsed = now() - downloadStart;
                    final int downloadKBPerSec = (int) ((downloadedBytes.length / 1024.0) / (downloadElapsed / 1000.0));
                    printPerf("Download bandwidth (" + uploadBytes.length + " bytes): " + downloadKBPerSec + "kb/sec\n");
                }

                testTransferSizePerformance(conn);
            }
        }
    }


    private void
    testTransferSizePerformance(final MBeanServerConnection conn)
            throws IOException {
        final Sample sample = (Sample) getDomainRoot().getContainee(XTypes.SAMPLE);

        final int ITER = 10;
        final int TEST_SIZE = 4 * MB;

        printPerf("Upload bandwidth, test size = " + (TEST_SIZE / (float) (MB)) + "MB X " + ITER + " iterations.");

        for (int chunkSize = 8 * K; chunkSize <= TEST_SIZE; chunkSize *= 2) {
            final byte[] chunk = new byte[chunkSize];

            long totalElapsed = 0;
            for (int iter = 0; iter < ITER; ++iter) {
                final long uploadStart = now();
                int total = 0;
                while (total < TEST_SIZE) {
                    sample.uploadBytes(chunk);
                    total += chunk.length;
                }
                final long uploadElapsed = now() - uploadStart;
                totalElapsed += uploadElapsed;
            }

            final int uploadKBPerSec = (int) ((ITER * TEST_SIZE / (float) K) / (totalElapsed / 1000.0));
            printPerf("Upload bandwidth (" + chunkSize / K + "K chunks): " + uploadKBPerSec + "kb/sec");
        }
    }

    public PerformanceTest() {
        try {
            final MBeanServerConnection conn =
                    Util.getExtra(getDomainRoot()).getConnectionSource().getMBeanServerConnection(false);

            baselineTest(conn);
        }
        catch (IOException e) {
            System.err.print("Caught exception: " + e);
        }
    }

    private Method
    findMethod(
            final Object target,
            final String methodName,
            final Object[] args)
            throws IllegalAccessException, InvocationTargetException {
        final Method[] methods = target.getClass().getDeclaredMethods();

        final int numArgs = args == null ? 0 : args.length;
        Method testMethod = null;
        for (int i = 0; i < methods.length; ++i) {
            final Method method = methods[i];

            if (method.getName().equals(methodName) &&
                    method.getParameterTypes().length == numArgs) {
                testMethod = method;
                break;
            }
        }

        if (testMethod == null) {
            throw new IllegalArgumentException("Can't find method: " + methodName);
        }
        return (testMethod);
    }

    private String
    getMethodString(
            final Method m,
            final Object[] args) {
        String result = null;

        if (args == null || args.length == 0) {
            result = m.getName() + "()";
        } else {
            result = m.getName() + "(" + ArrayStringifier.stringify(args, ", ") + ")";
        }

        return result;
    }

    private void
    testMethod(
            final AMX target,
            final String methodName,
            final Object[] args,
            final int additionalIterations)
            throws IllegalAccessException, InvocationTargetException {
        final String interfaceName =
                ClassUtil.stripPackagePrefix(Util.getExtra(target).getInterfaceName());

        final Method testMethod = findMethod(target, methodName, args);

        final long start = now();
        final Object resultFirst = testMethod.invoke(target, args);
        final long elapsedFirst = now() - start;

        String msg = interfaceName + "." + getMethodString(testMethod, args) +
                ": " + elapsedFirst + "ms";

        if (additionalIterations != 0) {
            final long iterStart = now();
            for (int i = 0; i < additionalIterations - 1; ++i) {
                final Object result = testMethod.invoke(target, args);
            }
            final long iterElapsed = now() - iterStart;

            msg = msg + ", " + additionalIterations + " additional iterations: " + iterElapsed + "ms";
        }

        printPerf(msg);
    }


    public synchronized void
    xtestQueryMgr()
            throws IllegalAccessException, InvocationTargetException {
        final DomainRoot domainRoot = getDomainRoot();
        final QueryMgr queryMgr = domainRoot.getQueryMgr();

        final String domain = Util.getObjectName(queryMgr).getDomain();

        printPerf("-- QueryMgr --- ");

        testMethod(domainRoot, "getQueryMgr", null, 1000);

        final int ITER = 20;
        testMethod(queryMgr, "queryAllSet", null, ITER);
        testMethod(queryMgr, "querySingletonJ2EEType", new Object[]{XTypes.BULK_ACCESS}, ITER);
        testMethod(queryMgr, "queryJ2EETypeSet", new Object[]{XTypes.SSL_CONFIG}, ITER);
        testMethod(queryMgr, "queryJ2EENameSet", new Object[]{"server"}, ITER);
        testMethod(queryMgr, "queryJ2EETypeNames", new Object[]{XTypes.CONFIG_CONFIG}, ITER);
        testMethod(queryMgr, "queryPatternSet", new Object[]{domain, "j2eeType=" + XTypes.SERVLET_MONITOR}, ITER);
        testMethod(queryMgr, "queryInterfaceSet", new Object[]{SSLConfig.class.getName(), null}, ITER);
    }

    /*
         public synchronized void
     testTargets()
         throws IllegalAccessException, InvocationTargetException
     {
         DomainConfig domainConfig = getDomainRoot().getDomainConfig();

         final long	start	= now();

         final int	ITER	= 100;
         for( int i = 0; i < ITER; ++i )
         {
             final Mapxxx servers	= domainConfig.getStandaloneServerConfigMap();
             final Mapxxx clusters	= domainConfig.getClusterConfigMap();

             final String[] serverNames	= GSetUtil.toStringArray( servers.keySet() );
             final String[] clusterNames	= GSetUtil.toStringArray( clusters.keySet() );
         }

         final long	elapsed	= now() - start;
         printPerf( "testTargets: " + ITER + " iterations: " + elapsed);
     }
     */

    public synchronized void
    xtestDomainConfig()
            throws IllegalAccessException, InvocationTargetException {
        final DomainRoot domainRoot = getDomainRoot();
        final DomainConfig domainConfig = domainRoot.getDomainConfig();

        printPerf("-- DomainConfig --- ");

        final int ITER = 20;

        testMethod(domainConfig, "getNodeAgentConfigMap", null, ITER);
        testMethod(domainConfig, "getConfigConfigMap", null, ITER);
        testMethod(domainConfig, "getStandaloneServerConfigMap", null, ITER);
        testMethod(domainConfig, "getClusteredServerConfigMap", null, ITER);
        testMethod(domainConfig, "getServerConfigMap", null, ITER);
        testMethod(domainConfig, "getClusterConfigMap", null, ITER);

        testMethod(domainConfig, "getCustomResourceConfigMap", null, ITER);
        testMethod(domainConfig, "getJNDIResourceConfigMap", null, ITER);
        testMethod(domainConfig, "getPersistenceManagerFactoryResourceConfigMap", null, ITER);
        testMethod(domainConfig, "getJDBCResourceConfigMap", null, ITER);
        testMethod(domainConfig, "getJDBCConnectionPoolConfigMap", null, ITER);
        testMethod(domainConfig, "getConnectorResourceConfigMap", null, ITER);
        testMethod(domainConfig, "getConnectorConnectionPoolConfigMap", null, ITER);
        testMethod(domainConfig, "getAdminObjectResourceConfigMap", null, ITER);
        testMethod(domainConfig, "getResourceAdapterConfigMap", null, ITER);
        testMethod(domainConfig, "getMailResourceConfigMap", null, ITER);
        testMethod(domainConfig, "getJ2EEApplicationConfigMap", null, ITER);
        testMethod(domainConfig, "getEJBModuleConfigMap", null, ITER);
        testMethod(domainConfig, "getWebModuleConfigMap", null, ITER);
        testMethod(domainConfig, "getRARModuleConfigMap", null, ITER);
        testMethod(domainConfig, "getAppClientModuleConfigMap", null, ITER);
        testMethod(domainConfig, "getLifecycleModuleConfigMap", null, ITER);
    }

}





