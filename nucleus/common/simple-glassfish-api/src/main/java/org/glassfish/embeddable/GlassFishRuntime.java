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

package org.glassfish.embeddable;

import org.glassfish.embeddable.spi.RuntimeBuilder;

import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is the entry point API to bootstrap GlassFish.
 *
 * <p/>A GlassFishRuntime represents just the runtime environment,
 * i.e., no active services yet. e.g., there won't be any web container
 * started just by creating a GlassFishRuntime object.
 *
 * <p/> The services will be activated when GlassFish instance is
 * started by doing omething like:
 *
 * <pre>
 *      GlassFishRuntime runtime = GlassFishRuntime.bootstrap(); // no active services
 *      GlassFish glassfish = runtime.newGlassFish();
 *      glassfish.start(); // active services.
 * </pre>
 * @author Sanjeeb.Sahoo@Sun.COM
 * @author bhavanishankar@dev.java.net
 */
public abstract class GlassFishRuntime {

    private static final Logger logger = Logger.getLogger(GlassFishRuntime.class.getPackage().getName());

    protected GlassFishRuntime() {
        // Empty protected constructor so that it does not show up in the javadoc.
    }
    
    /**
     * Bootstrap a GlassFishRuntime with default {@link BootstrapProperties}.
     *
     * @return Bootstrapped GlassFishRuntime
     * @throws GlassFishException if the GlassFishRuntime is already bootstrapped.
     */
    public static GlassFishRuntime bootstrap() throws GlassFishException {
        return bootstrap(new BootstrapProperties(), GlassFishRuntime.class.getClassLoader());
    }

    /**
     * Bootstrap GlassFish runtime based on runtime configuration passed in the bootstrapProperties object.
     * This is a convenience method. Calling this method is same as
     * calling {@link #bootstrap(BootstrapProperties , ClassLoader)} with null as second argument.
     *
     * @param bootstrapProperties BootstrapProperties used to setup the runtime
     * @throws GlassFishException
     */
    public static GlassFishRuntime bootstrap(BootstrapProperties bootstrapProperties) throws GlassFishException {
        return bootstrap(bootstrapProperties, GlassFishRuntime.class.getClassLoader());
    }

    /**
     * Bootstrap GlassFish runtime based on runtime configuration passed in the bootstrapProperties object.
     * Calling this method twice will throw a GlassFishException
     *
     * @param bootstrapProperties BootstrapProperties used to setup the runtime
     * @param cl      ClassLoader used as parent loader by GlassFish modules. If null is passed, the class loader
     *                of this class is used.
     * @return a bootstrapped runtime that can now be used to create new GlassFish instances
     * @throws GlassFishException
     */
    public static GlassFishRuntime bootstrap(BootstrapProperties bootstrapProperties, ClassLoader cl) throws GlassFishException {
        return _bootstrap(bootstrapProperties, cl);
    }

    /**
     * Shuts down the Runtime and dispose off all the GlassFish objects
     * created via this Runtime
     *
     * @throws GlassFishException
     */
    public abstract void shutdown() throws GlassFishException;

    /**
     * Create a new instance of GlassFish with default {@link org.glassfish.embeddable.GlassFishProperties}
     *
     * @return New GlassFish instance.
     * @throws GlassFishException If at all fails to create a new GlassFish instance.
     */
    public GlassFish newGlassFish() throws GlassFishException {
        return newGlassFish(new GlassFishProperties());
    }

    /**
     * Creates a new instance of GlassFish.
     *
     * @param glassfishProperties GlassFishProperties used to setup the GlassFish instance
     * @return newly instantiated GlassFish object. It will be in {@link GlassFish.Status#INIT} state.
     * @throws GlassFishException
     */
    public abstract GlassFish newGlassFish(GlassFishProperties glassfishProperties) throws GlassFishException;


    /*
     * INTERNAL IMPLEMENTATION DETAILS
     * Be careful while introducing dependencies in this class, as this is shipped as part of api jar used
     * by users.
     */

    /**
     * Singleton
     */
    private static GlassFishRuntime me;

    private synchronized static GlassFishRuntime _bootstrap(BootstrapProperties bootstrapProperties, ClassLoader cl) throws GlassFishException {
        if (me != null) {
            throw new GlassFishException("Already bootstrapped", null);
        }
        RuntimeBuilder runtimeBuilder = getRuntimeBuilder(bootstrapProperties, cl != null ? cl : GlassFishRuntime.class.getClassLoader());
        me = runtimeBuilder.build(bootstrapProperties);
        return me;
    }

    protected synchronized static void shutdownInternal() throws GlassFishException {
        if (me == null) {
            throw new GlassFishException("Already shutdown", null);
        }
        me = null;
    }

    private static RuntimeBuilder getRuntimeBuilder(BootstrapProperties bootstrapProperties, ClassLoader cl) throws GlassFishException {
//        StringBuilder sb = new StringBuilder("Launcher Class Loader = " + cl);
//        if (cl instanceof URLClassLoader) {
//            sb.append("has following Class Path: ");
//            for (URL url : URLClassLoader.class.cast(cl).getURLs()) {
//                sb.append(url).append(", ");
//            }
//        }
//        System.out.println(sb);
        Iterator<RuntimeBuilder> runtimeBuilders = ServiceLoader.load(RuntimeBuilder.class, cl).iterator();
        while (runtimeBuilders.hasNext()) {
            try {
                RuntimeBuilder builder = runtimeBuilders.next();
                logger.logp(Level.FINE, "GlassFishRuntime", "getRuntimeBuilder", "builder = {0}", new Object[]{builder});
                if (builder.handles(bootstrapProperties)) {
                    return builder;
                }
            } catch (ServiceConfigurationError sce) {
                // Ignore the exception and move ahead to the next builder.
                logger.logp(Level.FINE, "GlassFishRuntime", "getRuntimeBuilder", "Ignoring", sce);
            } catch (NoClassDefFoundError ncdfe) {
                // On IBM JDK, we seem to be getting NoClassDefFoundError instead of ServiceConfigurationError
                // when OSgiRuntimeBuilder is not able to be loaded in non-OSGi mode because of absence of
                // OSGi classes in classpath. So, we need to catch it and ignore.
                logger.logp(Level.FINE, "GlassFishRuntime", "getRuntimeBuilder", "Ignoring", ncdfe);
            }
        }
        throw new GlassFishException("No runtime builder available for this configuration: " + bootstrapProperties.getProperties(), null);
    }

}
