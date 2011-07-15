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

package org.glassfish.admin.amx.util;

import org.glassfish.admin.amx.util.ExceptionUtil;

import javax.management.MBeanServer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
Central registry of available features for fine-grained dependency enforcement in a
threaded environment.
<p>
Providers of features can register the availability (initializated and ready-to-use)
of a particular feature, upon which other code depends.  See {@link #registerFeature}.
<p>
Code that requires an initialized and ready-to-use feature should call
{@link #waitForFeature}--the calling thread will block until that feature is available.
<p>
Feature names are arbitrary and *should not* be defined as 'public static final String' in this
class unless they are of general system interest.  Features that are <i>not</i> of general interest
can and should still be registered through this mechanism,
but the names of those features and the associated data can be defined in a more appropriate place.
<p>
<b>Provider Example</b><br>
<pre>
...provider runs in its own thread...
FeatureAvailability.registerFeature( FeatureAvailability.MBEAN_SERVER_FEATURE, mbeanServer );
</pre><p>
<b>Client Example</b><br>
(arbitrary number of clients, multiple calls OK)</br>
<pre>
...client runs until feature is needed...
final MBeanServer mbeanServer = (MBeanServer)
FeatureAvailability.waitForFeature( FeatureAvailability.MBEAN_SERVER_FEATURE );
</pre>
<p>
To see how long client code is blocking, set the {@link #DEBUG_ENABLED} flag to true.
 */
public final class FeatureAvailability
{
    static private final FeatureAvailability INSTANCE = new FeatureAvailability();

    private final Map<String, Object> mFeatures;

    private final Map<String, CountDownLatch> mLatches;

    // private final AMXDebugHelper                mDebug;
    /** feature stating that the AdminContext is available.  Result data is the AdminContext */
    //public static final String  ADMIN_CONTEXT_FEATURE    = "AdminContext";
    /** feature stating that the MBeanServer is available.  Result data is the MBeanServer */
    //public static final String  MBEAN_SERVER_FEATURE    = "MBeanServer";
    /** feature stating that the SunoneInterceptor is active.  Associated data should be ignored. */
    //public static final String  SUN_ONE_INTERCEPTOR_FEATURE    = "SunoneInterceptor";
    /** feature stating that the com.sun.appserv:category=config MBeans are available. 
    Result data should not be used */
    //public static final String COM_SUN_APPSERV_CONFIG_MBEANS_FEATURE    = "com.sun.appserv:category=config";
    /** feature stating that the AMX MBean Loader is available (but not AMX).  Data should not be used */
    public static final String AMX_LOADER_FEATURE = "AMXLoader";

    /** feature stating that the AMX BootUtil class is available.  Data should not be used */
    //public static final String AMX_BOOT_UTIL_FEATURE   = "AMXBootUtil";
    /** feature stating that the AMX core is ready for use after having been started.  Data should not be used.
    Other AMX subystems might still be in the process of initializing */
    public static final String AMX_CORE_READY_FEATURE = "AMXCoreReady";

    /** feature stating that the AMX and all its subsystems are ready for use.  Data is the ObjectName of the DomainRoot */
    public static final String AMX_READY_FEATURE = "AMXReady";

    /** feature stating that the CallFlow feature is available.  Data should not be used
    Data is of type com.sun.enterprise.admin.monitor.callflow.Agent */
    //public static final String CALL_FLOW_FEATURE   = "CallFlow";
    /** Feature stating that the server has started, meaning that the main initialization
    code has been run; specific features could be initializing on separate threads, or
    could be initialized lazily.  No data is associated with this feature. */
    //public static final String SERVER_STARTED_FEATURE   = "ServerStarted";
    private FeatureAvailability()
    {
        mFeatures = new HashMap<String, Object>();
        mLatches = new HashMap<String, CountDownLatch>();

        //mDebug      = new AMXDebugHelper( "--FeatureAvailability--" );
        //mDebug.setEchoToStdOut( true );
    }

    public static FeatureAvailability getInstance()
    {
        return INSTANCE;
    }

    private static final boolean DEBUG_ENABLED = false;

    /**
    Internal use, should be replaced with use of
    com.sun.appserv.management.helper.AMXDebugHelper when build-order problems
    are resolved.  Set DEBUG_ENABLED to true to see output.
     */
    private void debug(final Object... args)
    {
        if (DEBUG_ENABLED)
        {
            String msg = "";
            for (int i = 0; i < args.length - 1; ++i)
            {
                msg = msg + args[i] + " ";
            }
            msg = msg + args[args.length - 1];

            System.out.println(msg);
        }
    }

    /**
    MBeanServer is created very early and is available via this method exept for code
    that executes prior to the JVM calling main().  As it is of interest in many areas,
    an explicit method call is provided.
    <p>
    <b>NON-BLOCKING--returns current value, can be null</b>
    @return the MBeanServer used by all AppServer MBeans
    public MBeanServer
    getMBeanServer()
    {
    return (MBeanServer)mFeatures.get( MBEAN_SERVER_FEATURE );
    }
     */
    /**
    MBeanServer is created very early and is available via this method exept for code
    that executes prior to the JVM calling main().  As it is of interest in many areas,
    an explicit method call is provided.
    <b>BLOCKING--returns onlyl when MBeansServer becomes available.</b>
    @return the MBeanServer used by all AppServer MBeans
    public MBeanServer
    waitForMBeanServer()
    {
    MBeanServer server = getMBeanServer();

    if ( server == null )
    {
    server = (MBeanServer)waitForFeature( MBEAN_SERVER_FEATURE,
    "waitForMBeanServer from " +  ExceptionUtil.getStackTrace() );
    }
    return server;
    }
     */
    /**
    Register a named feature as being ready for use.  The data value can be a
    dummy value, or can be something useful to the caller of waitForFeature().
    <p>
    <b>Do not register a feature until its facilities are ready for use</b>.
    <p>
    Features should generally be fine-grained, not coarse.  For example, the AdminService
    is a coarsely-defined feature which initializes dozens of things; code that requires
    the presence of one of those things should arrange for a name feature for that. Examples
    of this include the MBeanServer, the AdminContext, com.sun.appserv:category=config MBeans,
    etc.
    @param featureName  arbitrary name for the feature, to be used by clients in {@link #waitForFeature}
    @param data         arbitrary data of possible interest to clients
     */
    public synchronized void registerFeature(final String featureName, final Object data)
    {
        //debug( "FeatureAvailability.registerFeature: " + featureName + " in instance " + this);
        if (mFeatures.get(featureName) != null)
        {
            throw new IllegalStateException("FeatureAvailability.addFeature: already added: " + featureName);
        }
        if (data == null)
        {
            throw new IllegalArgumentException("FeatureAvailability.addFeature(): data is null for: " + featureName);
        }
        mFeatures.put(featureName, data);
        //debug( "********** FeatureAvailability.addFeature: " + featureName + ", data = " + data + "**********");

        if (mLatches.containsKey(featureName))
        {
            final CountDownLatch latch = mLatches.remove(featureName);
            latch.countDown();  // let all blocked threads proceed
            //debug( "addFeature: released latch for: " + featureName );
        }
    }

    /**
    Block until the specified feature is available.
    @param featureName the name of the desired feature
    @param callerInfo arbitrary caller info for debugging purposes
     */
    public Object waitForFeature(final String featureName, final String callerInfo)
    {
        //debug( "FeatureAvailability.waitForFeature: " + featureName + " by " + callerInfo + " in instance " + this);
        CountDownLatch latch = null;
        Object data = null;

        // working with mFeatures and mLatches together; must synchronize for this
        synchronized (this)
        {
            data = mFeatures.get(featureName);
            if (data == null)
            {
                // feature not yet available, calling thread will have to wait
                latch = mLatches.get(featureName);
                if (latch == null)
                {
                    latch = new CountDownLatch(1);
                    mLatches.put(featureName, latch);
                }
            }
        }

        assert ((data == null && latch != null) || (data != null && latch == null));

        // if we had to create a CountDownLatch, calling thread must now await()
        if (latch != null)
        {
            final long start = System.currentTimeMillis();

            try
            {
                //debug( "waitForFeature: \"" + featureName + "\" by " + callerInfo );
                final long startNanos = System.nanoTime();

                latch.await();

                final long elapsedNanos = System.nanoTime() - startNanos;
                if (elapsedNanos > 1000 * 1000)  // 1 millisecond
                {
                    debug("FeatureAvailability.waitForFeature: waited ",
                            "" + elapsedNanos,
                            " for feature \"", featureName, "\" by \"", callerInfo, "\"");
                }
            }
            catch (java.lang.InterruptedException e)
            {
                debug("waitForFeature: ERROR: ", e);
                throw new Error(e);
            }

            data = mFeatures.get(featureName);
        }

        return data;
    }

    public synchronized void deRegisterFeatures() {
        debug("Removed all Fetaures from the Map");
        mFeatures.clear();
    }

}

































