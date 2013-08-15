/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.common.util;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

import com.sun.enterprise.util.CULoggerInfo;

import java.io.*;
import java.lang.reflect.Array;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class OSGiObjectInputOutputStreamFactoryImpl
        implements ObjectInputOutputStreamFactory {

    private static final Logger logger = CULoggerInfo.getLogger();

    private BundleContext ctx;
    PackageAdmin pkgAdm;

    private ConcurrentHashMap<String, Long> name2Id = new ConcurrentHashMap<String, Long>();

    // Since bundle id starts with 0, we use -1 to indicate a non-bundle
//    private static final long NOT_A_BUNDLE_ID = -1;
    private static final String NOT_A_BUNDLE_KEY = ":";

    public OSGiObjectInputOutputStreamFactoryImpl(BundleContext ctx)
    {
        this.ctx = ctx;
        ServiceReference ref = ctx.getServiceReference(PackageAdmin.class.getName());
        pkgAdm = PackageAdmin.class.cast(ctx.getService(ref));

        BundleTracker bt = new BundleTracker(ctx, Bundle.INSTALLED | Bundle.RESOLVED | Bundle.STARTING | Bundle.ACTIVE, new BundleTrackerCustomizer() {
            
            @Override
            public void modifiedBundle(Bundle bundle, BundleEvent bundleEvent, Object o) {
            }

            @Override
            public Object addingBundle(Bundle bundle, BundleEvent bundleEvent) {
                String key = makeKey(bundle);
                name2Id.put(key, bundle.getBundleId());
                if (logger != null && logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, "BundleTracker.addingBundle BUNDLE " + key + " ==> " + bundle.getBundleId() + "  for " + bundle.getSymbolicName());
                }
                return null;
            }

            @Override
            public void removedBundle(Bundle bundle, BundleEvent bundleEvent, Object o) {
                String key = makeKey(bundle);
                Long bundleID = name2Id.remove(key);
                if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, "BundleTracker.removedBundle BUNDLE " + key + "  ==> " + bundle.getSymbolicName());
                }
                if (bundleID == null) {
                    logger.log(Level.WARNING, CULoggerInfo.NULL_BUNDLE, key);
                }
            }
            /*
            @Override
            public Object addingBundle(Bundle bundle, BundleEvent event) {
                System.out.println("ADDING BUNDLE ==> " + bundle.getSymbolicName());
                return super.addingBundle(bundle, event);    //To change body of overridden methods use File | Settings | File Templates.
            }

            @Override
            public void removedBundle(Bundle bundle, BundleEvent event, Object object) {
                System.out.println("REMOVING BUNDLE ==> " + bundle.getSymbolicName());                
                super.removedBundle(bundle, event, object);    //To change body of overridden methods use File | Settings | File Templates.
            }
            */
        });

        bt.open();

    }

    private String makeKey(Bundle bundle) {
        return bundle.getSymbolicName() + ":" + bundle.getVersion();
    }

    public ObjectInputStream createObjectInputStream(InputStream in)
            throws IOException
    {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        return new OSGiObjectInputStream(in, loader);
    }

    public ObjectOutputStream createObjectOutputStream(OutputStream out)
            throws IOException
    {
        return new OSGiObjectOutputStream(out);
    }

    private class OSGiObjectInputStream extends ObjectInputStreamWithLoader
    {

        public OSGiObjectInputStream(InputStream in, ClassLoader loader) throws IOException
        {
            super(in, loader);
        }

        @Override
        protected Class<?> resolveClass(ObjectStreamClass desc)
                throws IOException, ClassNotFoundException
        {
            Class clazz =
                OSGiObjectInputOutputStreamFactoryImpl.this.resolveClass(this, desc);

            if (clazz == null) {
                clazz = super.resolveClass(desc);
            }

            return clazz;
        }

    }

    private class OSGiObjectOutputStream extends ObjectOutputStream {


        private OSGiObjectOutputStream(OutputStream out) throws IOException
        {
            super(out);
        }

        @Override
        protected void annotateClass(Class<?> cl) throws IOException
        {
            OSGiObjectInputOutputStreamFactoryImpl.this.annotateClass(this, cl);
        }
    }

    public Class<?> resolveClass(ObjectInputStream in, final ObjectStreamClass desc)
            throws IOException, ClassNotFoundException
    {
        String key = in.readUTF();

        if (! NOT_A_BUNDLE_KEY.equals(key)) {
            Long bundleId = name2Id.get(key);
            if (bundleId != null) {
                final Bundle b = ctx.getBundle(bundleId);
                String cname = desc.getName();
                if (cname.startsWith("[")) {
                    return loadArrayClass(b, cname);
                } else {
                    return loadClassFromBundle(b, cname);
                }
            }
        }

        return null;
    }

    public void annotateClass(ObjectOutputStream out, Class<?> cl) throws IOException
    {
        String key = NOT_A_BUNDLE_KEY;
        Bundle b = pkgAdm.getBundle(cl);
        if (b != null) {
            key = makeKey(b);
        }
        out.writeUTF(key);
    }

    private Class loadArrayClass(Bundle b, String cname) throws ClassNotFoundException {
        // We are never called with primitive types, so we don't have to check for primitive types.
        assert(cname.charAt(0) == 'L'); // An array
        Class component;        // component class
        int dcount;            // dimension
        for (dcount = 1; cname.charAt(dcount) == '['; dcount++){
        }
        assert(cname.charAt(dcount) == 'L');
        component = loadClassFromBundle(b, cname.substring(dcount + 1, cname.length() - 1));
        int dim[] = new int[dcount];
        for (int i = 0; i < dcount; i++) {
            dim[i] = 0;
        }
        return Array.newInstance(component, dim).getClass();
    }

    private Class loadClassFromBundle(final Bundle b, final String cname) throws ClassNotFoundException {
        if (System.getSecurityManager() == null) {
            return b.loadClass(cname);
        } else {
            try {
                return (Class) java.security.AccessController.doPrivileged(
                        new java.security.PrivilegedExceptionAction() {
                            public java.lang.Object run() throws ClassNotFoundException {
                                return b.loadClass(cname);
                            }
                        });
            } catch (java.security.PrivilegedActionException pae) {
                throw (ClassNotFoundException) pae.getException();
            }
        }
    }
}
