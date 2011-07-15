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
 * EnhancerClassLoader.java
 */

package com.sun.jdo.api.persistence.enhancer;

import java.lang.ref.WeakReference;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.Properties;

import java.net.URLClassLoader;
import java.net.URL;

import sun.misc.Resource;
import sun.misc.URLClassPath;

import java.util.jar.Manifest;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;

import java.security.AccessController;
import java.security.AccessControlContext;
import java.security.CodeSource;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;
import java.security.cert.Certificate;

import com.sun.jdo.api.persistence.model.Model;

import com.sun.jdo.api.persistence.enhancer.meta.JDOMetaData;
import com.sun.jdo.api.persistence.enhancer.meta.JDOMetaDataPropertyImpl;
import com.sun.jdo.api.persistence.enhancer.meta.JDOMetaDataModelImpl;
import com.sun.jdo.api.persistence.enhancer.meta.JDOMetaDataTimer;

import com.sun.jdo.api.persistence.enhancer.util.Support;


//@lars: changes to reflect the new ByteCodeEnhancer interface


/**
 * Implements a ClassLoader which automatically enchances the .class files
 * according to the JDOMetaData information in the jar archive.
 * @author  Yury Kamen
 *
 */
public class EnhancerClassLoader extends URLClassLoader {

    static public final String DO_SIMPLE_TIMING
        = FilterEnhancer.DO_SIMPLE_TIMING;
    static public final String VERBOSE_LEVEL
        = FilterEnhancer.VERBOSE_LEVEL;
    static public final String VERBOSE_LEVEL_QUIET
        = FilterEnhancer.VERBOSE_LEVEL_QUIET;
    static public final String VERBOSE_LEVEL_WARN
        = FilterEnhancer.VERBOSE_LEVEL_WARN;
    static public final String VERBOSE_LEVEL_VERBOSE
        = FilterEnhancer.VERBOSE_LEVEL_VERBOSE;
    static public final String VERBOSE_LEVEL_DEBUG
        = FilterEnhancer.VERBOSE_LEVEL_DEBUG;

    static public URL[] pathToURLs(String classpath) {
        return URLClassPath.pathToURLs(classpath);
    }

    // misc
    //@olsen: 4370739
    private boolean debug = true;
    private boolean doTiming = false;
    private PrintWriter out = new PrintWriter(System.out, true);

    private ByteCodeEnhancer enhancer;
    private JDOMetaData metaData;
    private Properties settings;
    private WeakReference outByteCodeRef;

    // The search path for classes and resources
    private final URLClassPath ucp;

    // The context to be used when loading classes and resources
    private final AccessControlContext acc;

    //@olsen: 4370739
    private final void message() {
        if (debug) {
            out.println();
        }
    }

    //@olsen: 4370739
    private final void message(String s) {
        if (debug) {
            out.println(s);
        }
    }

    //@olsen: 4370739
    private final void message(Exception e) {
        if (debug) {
            final String msg = ("Exception caught: " + e);//NOI18N
            out.println(msg);
            e.printStackTrace(out);
        }
    }

    /**
     * Creates a new EnhancerClassLoader for the specified url.
     *
     * @param urls the classpath to search
     */
    protected EnhancerClassLoader(URL[] urls) {
        super(urls);
        acc = AccessController.getContext();
        ucp = new URLClassPath(urls);
        checkUCP(urls);
    }

    /**
     * Creates a new EnhancerClassLoader for the specified url.
     *
     * @param urls the classpath to search
     */
    protected EnhancerClassLoader(URL[] urls,
                                  ClassLoader loader) {
        super(urls, loader);
        acc = AccessController.getContext();
        ucp = new URLClassPath(urls);
        checkUCP(urls);
    }

    /**
     * Creates a new EnhancerClassLoader for the specified url.
     *
     * @param classpath the classpath to search
     */
    public EnhancerClassLoader(String classpath,
                               Properties settings,
                               PrintWriter out) {
        this(pathToURLs(classpath));
        JDOMetaData metaData = new JDOMetaDataModelImpl(Model.ENHANCER, out);
        init(metaData, settings, out);
    }

    /**
     * Creates a new EnhancerClassLoader for the specified url.
     *
     * @param urls the classpath to search
     */
    public EnhancerClassLoader(URL[] urls,
                               Properties settings,
                               PrintWriter out) {
        this(urls);
        JDOMetaData metaData = new JDOMetaDataModelImpl(Model.ENHANCER, out);
        init(metaData, settings, out);
    }

    /**
     * Creates a new EnhancerClassLoader for the specified url.
     *
     * @param classpath the classpath to search
     */
    public EnhancerClassLoader(String classpath,
                               JDOMetaData metaData,
                               Properties settings,
                               PrintWriter out) {
        this(pathToURLs(classpath));
        init(metaData, settings, out);
    }

    /**
     * Creates a new EnhancerClassLoader for the specified url.
     *
     * @param urls the classpath to search
     */
    public EnhancerClassLoader(URL[] urls,
                               JDOMetaData metaData,
                               Properties settings,
                               PrintWriter out) {
        this(urls);
        init(metaData, settings, out);
    }

    /**
     * Creates a new EnhancerClassLoader for the specified url.
     *
     * @param url the url of the jar file
     */
//@olsen: obsolete code
/*
    public EnhancerClassLoader(URL url,
                               Properties metaDataProperties,
                               Properties settings,
                               PrintWriter out) {
        super(new URL[]{ url }); // , ClassLoader.getSystemClassLoader() ); //, new Factory() );
        initUcp(url);
        init(url, new JDOMetaDataPropertyImpl(metaDataProperties, out), settings, out);
    }
*/

    /**
     * Creates a new EnhancerClassLoader for the specified url.
     *
     * @param url the url of the jar file
     */
//@olsen: obsolete code
/*
    public EnhancerClassLoader(URL url,
                               Properties settings,
                               PrintWriter out)
        throws IOException {
        super(new URL[]{ url } ); //, ClassLoader.getSystemClassLoader() ); //, new Factory() );
        initUcp(url);
        Properties metaDataProperties = getJDOMetaDataProperties();
        metaData = new JDOMetaDataPropertyImpl(metaDataProperties, out);
        init(url, metaData, settings, out);
    }
*/

    /**
     * Creates a new EnhancerClassLoader for the specified url.
     *
     * @param url the url of the jar file
     */
//@olsen: obsolete code
/*
    public EnhancerClassLoader(URL url,
                               ClassLoader loader,
                               Properties settings,
                               PrintWriter out)
        throws IOException {
        super(new URL[]{ url }, loader);        //super(new URL[]{ url }, loader, new Factory() ); //, ClassLoader.getSystemClassLoader() ); //, new Factory() );
        initUcp(url);
        Properties metaDataProperties = getJDOMetaDataProperties();
        metaData = new JDOMetaDataPropertyImpl(metaDataProperties, out);
        init(url, metaData, settings, out);
    }
*/

//@olsen: obsolete code
/*
    private String getJDOMetaDataPropertiesName()
        throws IOException {
        //message("url=" + url);
        if (true)
            return null;
        URL u = new URL("jar", "", url + "!/");
        JarURLConnection uc = (JarURLConnection)u.openConnection();
        Attributes attr = uc.getMainAttributes();
        String result = attr != null ? attr.getValue("JDOMetaData") : null;
        //message("getJDOMetaDataPropertiesName() returned: " + result);
        return result;
    }

    private Properties getJDOMetaDataProperties()
        throws IOException {
        return getJDOMetaDataProperties(getJDOMetaDataPropertiesName());
    }

    private static final String DEFAULT_JDO_PROPERTY_NAME = "all.jdo";

    private Properties getJDOMetaDataProperties(String name)
        throws IOException {
        if (null == name) {
            name = DEFAULT_JDO_PROPERTY_NAME;
        }
        Properties prop = new Properties();
        message("---ucp=" + ucp + " name=" + name);
        Resource res = ucp.getResource(name, false);
        if (null == res) {
            throw new IOException("Resource '" + name + "'" + " was not found");
        }

        byte[] b = res.getBytes();
        if (null == b) {
            throw new IOException("Resource '" + name + "'" + " has null content");
        }

        InputStream is = new ByteArrayInputStream(b);
        prop.load(is);
        return prop;
    }
*/

    /**
     * Appends the specified URL to the list of URLs to search for
     * classes and resources.
     *
     * @param url the URL to be added to the search path of URLs
     */
    protected void addURL(URL url) {
        throw new UnsupportedOperationException("Not implemented yet: EnhancerClassLoader.addURL(URL)");//NOI18N
        //super.addURL(url);
        //ucp.addURL(url);
    }

    private void checkUCP(URL[] urls) {
        // ensure classpath is not empty
        if (null == urls) {
            throw new IllegalArgumentException("urls == null");//NOI18N
        }
        if (urls.length == 0) {
            throw new IllegalArgumentException("urls.length == 0");//NOI18N
        }

        for (int i = 0; i < urls.length; i++) {
            super.addURL(urls[i]);
        }
    }

    /**
     * Initialize the EnhancingClassLoader
     */
    private void init(JDOMetaData metaData,
                      Properties settings,
                      PrintWriter out) {
        this.out = out;
        final String verboseLevel
            = (settings == null ? null
               : settings.getProperty(FilterEnhancer.VERBOSE_LEVEL));
        this.debug = FilterEnhancer.VERBOSE_LEVEL_DEBUG.equals(verboseLevel);
        this.settings = settings;
        this.metaData = metaData;
        this.enhancer = null;

        if (settings != null) {
            final String timing
                = settings.getProperty(FilterEnhancer.DO_SIMPLE_TIMING);
            this.doTiming = Boolean.valueOf(timing).booleanValue();
        }
        if (this.doTiming) {
            // wrap with timing meta data object
            this.metaData = new JDOMetaDataTimer(metaData);
        }

        message("EnhancerClassLoader: UCP = {");//NOI18N
        final URL[] urls = getURLs();
        for (int i = 0; i < urls.length; i++) {
            message("    " + urls[i]);//NOI18N
        }
        message("}");//NOI18N

        message("EnhancerClassLoader: jdoMetaData = " + metaData);//NOI18N
    }

    public synchronized Class loadClass(String name, boolean resolve)
        throws ClassNotFoundException {
        message();
        message("EnhancerClassLoader: loading class: " + name);//NOI18N

        try {
            Class c = null;

            final String classPath = name.replace('.', '/');
            // At least these packages must be delegated to parent class
            // loader:
            //    java/lang,	     (Object, ...)
            //    java/util,         (Collection)
            //    java/io,           (PrintWriter)
            //    javax/sql,         (PMF->javax.sql.DataSource)
            //    javax/transaction  (Tx->javax.transaction.Synchronization)
            //
            //@olsen: delegate loading of "safe" classes to parent
            //if (metaData.isTransientClass(classPath)) {
            //
            //@olsen: only delegate loading of bootstrap classes to parent
            //if (classPath.startsWith("java/lang/")) {
            //
            //@olsen: performance bug 4457471: delegate loading of F4J
            // persistence classes to parent tp prevent passing these and
            // other IDE classes plus database drivers etc. to the enhancer!
            //if (classPath.startsWith("java/lang/")
            //    || classPath.startsWith("com/sun/jdo/")) {
            //
            //@olsen: bug 4480618: delegate loading of javax.{sql,transaction}
            // classes to parent class loader to support user-defined
            // DataSource and Synchronization objects to be passed to the
            // TP runtime.  By the same argument, java.{util,io} classes need
            // also be loaded by the parent class loader.  This has been
            // the case since the EnhancerClassLoader will never find these
            // bootstrap classes in the passed Classpath.  However, for
            // efficiency and clarity, this delegation should be expressed
            // by testing for entire "java/" package in the check here.
            if (classPath.startsWith("java/")//NOI18N
                || classPath.startsWith("javax/sql/")//NOI18N
                || classPath.startsWith("javax/transaction/")//NOI18N
                || classPath.startsWith("com/sun/jdo/")) {//NOI18N
                message("EnhancerClassLoader: bootstrap class, using parent loader for class: " + name);//NOI18N
                return super.loadClass(name, resolve);

//@olsen: dropped alternative approach
/*
                message("EnhancerClassLoader: transient class, skipping enhancing: " + name);//NOI18N

                // get a byte array output stream to collect byte code
                ByteArrayOutputStream outByteCode
                    = ((null == outByteCodeRef)
                       ? null : (ByteArrayOutputStream)outByteCodeRef.get());
                if (null == outByteCode) {
                    outByteCode = new ByteArrayOutputStream(10000);
                    outByteCodeRef = new WeakReference(outByteCode);
                }
                outByteCode.reset();

                // find byte code of class
                final InputStream is = getSystemResourceAsStream(name);
                //@olsen: (is == null) ?!

                // copy byte code of class into byte array
                final byte[] data;
                try {
                    int b;
                    while ((b = is.read()) >= 0) {
                        outByteCode.write(b);
                    }
                    data = outByteCode.toByteArray();
                } catch (IOException e) {
                    final String msg
                        = ("Exception caught while loading class '"//NOI18N
                           + name + "' : " + e);//NOI18N
                    throw new ClassNotFoundException(msg, e);
                }

                // convert the byte code into class object
                c = defineClass(name, data, 0, data.length);
*/
            }

            //@olsen: check if class has been loaded already
            if (c == null) {
                c = findLoadedClass(name);
                if (c != null) {                
                    message("EnhancerClassLoader: class already loaded: " + name);//NOI18N
                }
            }

            if (c == null) {
                c = findAndEnhanceClass(name);
            }

            // as a last resort, if the class couldn't be found, try
            // loading class by parent class loader
            if (c == null) {
                message("EnhancerClassLoader: class not found, using parent loader for class: " + name);//NOI18N
                return super.loadClass(name, resolve);
            }

            message();
            message("EnhancerClassLoader: loaded class: " + name);//NOI18N
            if (resolve) {
                resolveClass(c);
            }

            message();
            message("EnhancerClassLoader: loaded+resolved class: " + name);//NOI18N
            return c;
        } catch (RuntimeException e) {
            // log exception only
            message();
            message("EnhancerClassLoader: EXCEPTION SEEN: " + e);//NOI18N
            //e.printStackTrace(out);
            throw e;
        } catch (ClassNotFoundException e) {
            // log exception only
            message();
            message("EnhancerClassLoader: EXCEPTION SEEN: " + e);//NOI18N
            //e.printStackTrace(out);
            throw e;
        }
    }

    /**
     * Finds and loads the class with the specified name from the URL search
     * path. Any URLs referring to JAR files are loaded and opened as needed
     * until the class is found.
     *
     * @param name the name of the class
     * @return the resulting class
     * @exception ClassNotFoundException if the class could not be found
     */
    private Class findAndEnhanceClass(final String name)
        throws ClassNotFoundException
    {
        try {
            if (doTiming) {
                Support.timer.push("EnhancerClassLoader.findAndEnhanceClass(String)",//NOI18N
                                   "EnhancerClassLoader.findAndEnhanceClass(" + name + ")");//NOI18N
            }
            return (Class)
            AccessController.doPrivileged(new PrivilegedExceptionAction() {
                public Object run() throws ClassNotFoundException {
                    String path = name.replace('.', '/').concat(".class");//NOI18N
                    //message("path=" + path);
                    Resource res = ucp.getResource(path, false);
                    if (res != null) {
                        try {
                            return defineClass(name, res);
                        } catch (IOException e) {
                            final String msg
                                = ("Exception caught while loading class '"//NOI18N
                                   + name + "' : " + e);//NOI18N
                            throw new ClassNotFoundException(msg, e);
                        }
                    } else {
                        // ok if class resource not found (e.g. java.*)
                        //throw new ClassNotFoundException(name);
                        return null;
                    }
                }
            }, acc);
        } catch (PrivilegedActionException pae) {
            throw (ClassNotFoundException) pae.getException();
        } finally {
            if (doTiming) {
                Support.timer.pop();
            }
        }
    }

    /**
     * Defines a Class using the class bytes obtained from the specified
     * Resource. The resulting Class must be resolved before it can be
     * used.
     */
    private Class defineClass(String name, Resource res)
        throws IOException, ClassNotFoundException {
        int i = name.lastIndexOf('.');
        URL url = res.getCodeSourceURL();
        if (i != -1) {
            String pkgname = name.substring(0, i);
            // Check if package already loaded.
            Package pkg = getPackage(pkgname);
            Manifest man = res.getManifest();
            if (pkg != null) {
                // Package found, so check package sealing.
                boolean ok;
                if (pkg.isSealed()) {
                    // Verify that code source URL is the same.
                    ok = pkg.isSealed(url);
                } else {
                    // Make sure we are not attempting to seal the package
                    // at this code source URL.
                    ok = (man == null) || !isSealed(pkgname, man);
                }
                if (!ok) {
                    throw new SecurityException("sealing violation");//NOI18N
                }
            } else {
                if (man != null) {
                    definePackage(pkgname, man, url);
                } else {
                    definePackage(pkgname, null, null, null, null, null, null, null);
                }
            }
        }
        // Now read the class bytes and define the class
        byte[] b = res.getBytes();
        Certificate[] certs = res.getCertificates();
        CodeSource cs = new CodeSource(url, certs);

        //@olsen: performance bug 4457471: circumvent enhancer for
        // non-enhancable classes
        final String classPath = name.replace('.', '/');
        if (!metaData.isTransientClass(classPath)) {
            // Add enhancement here
            b = enhance(name, b, 0, b.length);
        }

        return defineClass(name, b, 0, b.length, cs);
    }

    private byte[] enhance(String name, byte[] data, int off, int len)
        throws ClassNotFoundException {
        //message("EnhancerClassLoader: enhance class: " + name);

        final byte[] result;
        try {
            // create enhancer if not done yet
            if (null == enhancer) {
                enhancer = new FilterEnhancer(metaData, settings, out, null);
                if (doTiming) {
                    // wrap with timing filter enhancer object
                    enhancer = new ByteCodeEnhancerTimer(enhancer);
                }
            }

            // create input and output byte streams
            ByteArrayInputStream inByteCode
                = new ByteArrayInputStream(data, off, len);
            ByteArrayOutputStream outByteCode
                = ((null == outByteCodeRef)
                   ? null : (ByteArrayOutputStream)outByteCodeRef.get());
            if (null == outByteCode) {
                outByteCode = new ByteArrayOutputStream(10000);
                outByteCodeRef = new WeakReference(outByteCode);
            }
            outByteCode.reset();

            // enhance class
            boolean changed
                = enhancer.enhanceClassFile(inByteCode, outByteCode);

            // check whether class has been enhanced
            result = (changed ? outByteCode.toByteArray() : data);
        } catch (EnhancerUserException e) {
            //@olsen: 4370739
            message(e);
            final String msg = ("Exception caught while loading class '"//NOI18N
                                + name + "' : " + e);//NOI18N
            throw new ClassNotFoundException(msg, e);
        } catch(EnhancerFatalError e) {
            //@olsen: 4370739
            message(e);
            final String msg = ("Exception caught while loading class '"//NOI18N
                                + name + "' : " + e);//NOI18N
            // discard enhancer since it might have become inconsistent
            enhancer = null;
            throw new ClassNotFoundException(msg, e);
        }
        return result;
    }

    /**
     * Returns true if the specified package name is sealed according to the
     * given manifest.
     */
    private boolean isSealed(String name, Manifest man) {
        String path = name.replace('.', '/').concat("/");//NOI18N
        Attributes attr = man.getAttributes(path);
        String sealed = null;
        if (attr != null) {
            sealed = attr.getValue(Name.SEALED);
        }
        if (sealed == null) {
            if ((attr = man.getMainAttributes()) != null) {
                sealed = attr.getValue(Name.SEALED);
            }
        }
        return "true".equalsIgnoreCase(sealed);//NOI18N
    }
}
