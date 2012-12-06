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

package org.glassfish.extras.osgicontainer;

import org.glassfish.api.deployment.ApplicationContainer;
import org.glassfish.api.deployment.ApplicationContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.NoSuchElementException;

public class OSGiDeployedBundle implements ApplicationContainer<OSGiContainer> {

    private Bundle bundle;

    public OSGiDeployedBundle(Bundle bundle) {
        this.bundle = bundle;
    }

    public OSGiContainer getDescriptor() {
        return null;
    }

    public boolean start(ApplicationContext startupContext) throws Exception {
        return resume();
    }

    public boolean stop(ApplicationContext stopContext) {
        return suspend();
    }

    public boolean suspend() {
        if (!isFragment(bundle)) {
            stopBundle();
        }
        return true;
    }

    public boolean resume() throws Exception {
        if (!isFragment(bundle)) {
            startBundle();
        }
        return true;
    }

    public ClassLoader getClassLoader() {
        // return a non-null class loader. This will be set as TCL before the bundle is started or stopped
        // so that operations like JNDI lookup can be successful, as those operations in GlassFish requires
        // a non-null class loader.
        return new BundleClassLoader(bundle);
    }

    private static boolean isFragment(Bundle b) {
        return b.getHeaders().get(Constants.FRAGMENT_HOST) != null;
    }

    private void startBundle() throws BundleException {
        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        try {
            // Some operations like JNDI lookup requires a non-null context class loader, so
            // we need to set a non-null class loader.
            final ClassLoader cl1 = getClassLoader();
            assert(cl1 != null);
            Thread.currentThread().setContextClassLoader(cl1);
            bundle.start(Bundle.START_TRANSIENT | Bundle.START_ACTIVATION_POLICY);
            System.out.println("Started " + bundle);
        } catch (BundleException e) {
            throw new RuntimeException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldCl);
        }
    }

    private void stopBundle() {
        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        try {
            // Some operations like JNDI lookup requires a non-null context class loader, so
            // we need to set a non-null class loader.
            final ClassLoader cl1 = getClassLoader();
            assert(cl1 != null);
            Thread.currentThread().setContextClassLoader(cl1);
            bundle.stop(Bundle.STOP_TRANSIENT);
            System.out.println("Stopped " + bundle);
        } catch (BundleException e) {
            throw new RuntimeException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldCl);
        }
    }

}

class BundleClassLoader extends ClassLoader
{
    private Bundle bundle;

    public BundleClassLoader(Bundle b)
    {
        super(Bundle.class.getClassLoader());
        this.bundle = b;
    }

    @Override
    public synchronized Class<?> loadClass(final String name, boolean resolve) throws ClassNotFoundException
    {
        return bundle.loadClass(name);
    }

    @Override
    public URL getResource(String name)
    {
        return bundle.getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException
    {
        Enumeration<URL> resources = bundle.getResources(name);
        if (resources == null)
        {
            // This check is needed, because ClassLoader.getResources()
            // expects us to return an empty enumeration.
            resources = new Enumeration<URL>()
            {

                public boolean hasMoreElements()
                {
                    return false;
                }

                public URL nextElement()
                {
                    throw new NoSuchElementException();
                }
            };
        }
        return resources;
    }
}
