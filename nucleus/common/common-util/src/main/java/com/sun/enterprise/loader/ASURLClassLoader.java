/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.loader;

import com.sun.appserv.server.util.PreprocessorUtil;
import com.sun.enterprise.util.CULoggerInfo;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.enterprise.security.integration.DDPermissionsLoader;
import com.sun.enterprise.security.integration.PermsHolder;
import org.glassfish.api.deployment.DeploymentContext;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.security.SecureClassLoader;
import java.security.cert.Certificate;
import java.security.Permissions;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.security.PermissionCollection;
import org.glassfish.api.deployment.InstrumentableClassLoader;
import org.glassfish.hk2.api.PreDestroy;

/**
 * Class loader used by the ejbs of an application or stand alone module.
 *
 * This class loader also keeps cache of not found classes and resources.
 * </xmp>
 *
 * @author Nazrul Islam
 * @author Kenneth Saks
 * @author Sivakumar Thyagarajan
 * @since  JDK 1.4
 */
public class ASURLClassLoader
        extends URLClassLoader
        implements JasperAdapter, InstrumentableClassLoader, PreDestroy, DDPermissionsLoader {

    /*
       NOTE: various variables are 'final' to enjoy the JVM thread visibility guaranteed for 'final'.
       These variables cannot be nulled out because of this, but the contents are cleared (for Map/Vector).
       This is actually a hidden benefit, because there are places in the code where these variables
       are being used but they could be nulled out while being used.
       Another benefit is that there is no synchronization needed to get the Map/Vector/List itself.
    */

    /** logger for this class */
    private static final Logger _logger=CULoggerInfo.getLogger();

    /*
       list of url entries of this class loader. Using LinkedHashSet instead of original ArrayList
       for faster search.
    */
    private final Set<URLEntry> urlSet = Collections.synchronizedSet(new LinkedHashSet<URLEntry>());

    /** cache of not found resources */
    private final Map<String,String> notFoundResources   = new ConcurrentHashMap<String,String>();

    /** cache of not found classes */
    private final Map<String,String> notFoundClasses     = new ConcurrentHashMap<String,String>();

    /**
        State flag to track whether this instance has been shut off.

        Note: 'volatile' *does not by itself eliminate a race condition* similar to null-check idiom bug.
    */
    private volatile boolean doneCalled = false;

    /**
        snapshot of classloader state at the time done was called.
        <p>
        Must be 'volatile'; not all access is within 'synchronized' eg it is used in toString().
    */
    private volatile String doneSnapshot;

    /** streams opened by this loader */
    private final Vector<SentinelInputStream> streams = new Vector<SentinelInputStream>();

    private final ArrayList<ClassFileTransformer> transformers = new ArrayList<ClassFileTransformer>(1);

    private final static StringManager sm =
        StringManager.getManager(ASURLClassLoader.class);

    //holder for declared and ee permissions
    private PermsHolder permissionsHolder;
    
    /**
     * Constructor.
     */
    public ASURLClassLoader() {
        super(new URL[0]);

        permissionsHolder = new PermsHolder();
        
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE,
                        "ClassLoader: " + this + " is getting created.");
        }
    }

    /**
     * Constructor.
     *
     * @param    parent    parent class loader
     */
    public ASURLClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
        permissionsHolder = new PermsHolder();
    }

    public boolean isDone() {
        // method need not by 'synchronized' because 'doneCalled' is 'volatile'.
        return doneCalled;
    }


    @Override
    public void preDestroy() {
        done();
    }


    /**
     * This method should be called to free up the resources.
     * It helps garbage collection.
     *
     * Must be synchronized for:
         (a) visibility of variables
         (b) race condition while checking 'doneCalled'
         (c) only one caller should close the zip files,
         (d) done should occur only once and set the flag when done.
         (e) shoudl not return 'true' when a previous thread might still
         be in the process of executing the method.
     */
    public void done() {
        // This works because 'doneCalled' is 'volatile'
        if( doneCalled ) {
            return;
        }

        // the above optimized check for 'doneCalled=true' is a race condition.
        // The lock must now be acquired, and 'doneCalled' rechecked.
        synchronized(this) {
            if( doneCalled ) {
                return;
            }

            // Capture the fact that the classloader is now effectively disabled.
            // First create a snapshot of our state.  This should be called
            // before setting doneCalled = true.
            doneSnapshot = "ASURLClassLoader.done() called ON " + this.toString()
                + "\n AT " + new Date() +
                " \n BY :" + Arrays.toString(Thread.currentThread().getStackTrace());

            // Presumably OK to set this flag now while the rest of the cleanup proceeeds,
            // because we've taken the snapshot.
            doneCalled = true;

            // closes the jar handles and sets the url entries to null
            int i = 0;
            for (URLEntry u : this.urlSet) {
                if (u.zip != null) {
                    try {
                        u.zip.reallyClose();
                    } catch (IOException ioe) {
                        _logger.log(Level.INFO,
                                CULoggerInfo.getString(CULoggerInfo.exceptionClosingURLEntry, u.source),
                                ioe);
                    }
                }
                if (u.table != null) {
                    u.table.clear();
                    u.table = null;
                }
                u = null;
                i++;
            }

            closeOpenStreams();

            // clears out the tables
            // Clear all values.  Because fields are 'final' (for thread safety), cannot null them
            this.urlSet.clear();
            if (this.notFoundResources != null) { this.notFoundResources.clear(); }
            if (this.notFoundClasses != null)   { this.notFoundClasses.clear();   }
        }
    }


    /**
     * Adds a URL to the search list, based on the specified File.
     * <p>
     * This variant of the method makes sure that the URL is valid, in particular
     * encoding special characters (such as blanks) in the file path.
     * @param file the File to use in creating the URL
     * @throws IOException in case of errors converting the file to a URL
     */
    public void appendURL(File file) throws IOException {
        try {
            appendURL(file.toURI().toURL());
        } catch (MalformedURLException mue) {
            _logger.log(Level.SEVERE,
                    CULoggerInfo.getString(CULoggerInfo.badUrlEntry, file.toURI()),
                    mue);

            IOException ioe = new IOException();
            ioe.initCause(mue);
            throw ioe;
        }
    }


    /**
     * Appends the specified URL to the list of URLs to search for
     * classes and resources.
     *
     * @param url the URL to be added to the search path of URLs
     */
    public void addURL(URL url) {
        appendURL(url);
    }


    /**
     * Add a url to the list of urls we search for a class's bytecodes.
     *
     * @param    url    url to be added
     */
    public synchronized void appendURL(URL url) {

        try {
            if (url == null) {
                _logger.log(Level.INFO, CULoggerInfo.missingURLEntry);
                return;
            }

            URLEntry entry = new URLEntry(url);

            if ( !urlSet.contains(entry) ) {
                // adds the url entry to the list
                this.urlSet.add(entry);

                if (entry.isJar) {
                    // checks the manifest if a jar
                    checkManifest(entry.zip, entry.file);
                }
            } else {
                _logger.log(Level.FINE,
                    "[ASURLClassLoader] Ignoring duplicate URL: " + url);
                /*
                 *Clean up the unused entry or it could hold open a jar file.
                 */
                if (entry.zip != null) {
                    try {
                        entry.zip.reallyClose();
                    } catch (IOException ioe) {
                        _logger.log(Level.INFO,
                                CULoggerInfo.getString(CULoggerInfo.exceptionClosingDupUrlEntry, url),
                                ioe);
                    }
                }
            }

            // clears the "not found" cache since we are adding a new url
            clearNotFoundCaches();

        } catch (IOException ioe) {

            _logger.log(Level.SEVERE,
                    CULoggerInfo.getString(CULoggerInfo.badUrlEntry, url),
                    ioe);
        }
    }

    /**
     * Returns the urls of this class loader.
     *
     * Method is 'synchronized' to avoid the  thread-unsafe null-check idiom idiom, also
     * protects the caller from simultaneous changes while iterating,
     * by returning a URL[] (copy) rather than the original.  Also protects against
     * changes to 'urlSet' while iterating over it.
     *
     * @return    the urls of this class loader or an empty array
     */
    public synchronized URL[] getURLs() {

        URL[] url  = null;

        int i=0;
        if (this.urlSet != null) {
            url  = new URL[this.urlSet.size()];

            for (URLEntry urlEntry : urlSet) {
                url[i++] = (urlEntry).source;
            }
        } else {
            url = new URL[0];
        }

        return url;
    }

    /**
     * Returns all the "file" protocol resources of this ASURLClassLoader,
     * concatenated to a classpath string.
     *
     * Notice that this method is called by the setClassPath() method of
     * org.apache.catalina.loader.WebappLoader, since this ASURLClassLoader does
     * not extend off of URLClassLoader.
     *
     * @return Classpath string containing all the "file" protocol resources
     * of this ASURLClassLoader
     */
    public String getClasspath() {

        StringBuffer strBuf = null;

        URL[] urls = getURLs();
        if (urls != null) {
            for (int i=0; i<urls.length; i++) {
                if (urls[i].getProtocol().equals("file")) {
                    if (strBuf == null) {
                        strBuf = new StringBuffer();
                    }
                    if (i > 0) {
                        strBuf.append(File.pathSeparator);
                    }
                    strBuf.append(urls[i].getFile());
                }
            }
        }

        return (strBuf != null) ? strBuf.toString() : null;
    }

    /**
     *Refreshes the memory of the class loader.  This involves clearing the
     *not-found cahces and recreating the hash tables for the URLEntries that
     *record the files accessible for each.
     *<p>
     *Code that creates an ASURLClassLoader and then adds files to a directory
     *that is in the loader's classpath should invoke this method after the new
     *file(s) have been added in order to update the class loader's data
     *structures which optimize class and resource searches.
     *@throws IOException in case of errors refreshing the cache
     */
    public synchronized void refresh() throws IOException {
        clearNotFoundCaches();
//        for (URLEntry entry : urlSet) {
//            entry.cacheItems();
//        }
    }

    public void addTransformer(ClassFileTransformer transformer) {
        transformers.add(transformer);
    }

    /**
     * Create a new instance of a sibling classloader
     * @return a new instance of a class loader that has the same visibility
     *  as this class loader
     */
    public ClassLoader copy() {
        final ASURLClassLoader copyFrom = this;
        DelegatingClassLoader newCl = (DelegatingClassLoader)AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                // privileged code goes here, for example:
                return new DelegatingClassLoader(copyFrom);
            }
        });
        return newCl;
    }

    /**
     *Erases the memory of classes and resources that have been searched for
     *but not found.
     */
     private void clearNotFoundCaches() {
        this.notFoundResources.clear();
        this.notFoundClasses.clear();
    }

    /**
     * Internal implementation of find resource.
     *
     * @param    res    url resource entry
     * @param    name   name of the resource
     */
    private URL findResource0(final URLEntry res,
                              final String name) {

        Object result =
        AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {

                if (res.isJar) {

                    try {
                        JarEntry jarEntry = res.zip.getJarEntry(name);
                        if (jarEntry != null) {
                            /*
                             *Use a custom URL with a special stream handler to
                             *prevent the JDK's JarURLConnection caching from
                             *locking the jar file until JVM exit.
                             */
                            InternalURLStreamHandler handler = new InternalURLStreamHandler(res, name);

                            // Create a new sub URL from the resource URL (i.e. res.source). To avoid double encoding
                            // (see https://glassfish.dev.java.net/issues/show_bug.cgi?id=13045)
                            // use URL constructor instead of first creating a URI and then calling toURL().
                            // If the resource URL is not properly encoded, that's not our problem.
                            // Whoever has supplied the resource URL is at fault.
                            URL ret = new URL("jar", null /* host */, -1 /* port */, res.source + "!/" + name, handler);
                            handler.tieUrl(ret);
                            return ret;
                        }

                    } catch (Throwable thr) {
                        _logger.log(Level.INFO, CULoggerInfo.exceptionInASURLClassLoader, thr);
                    }
                } else { // directory
                    try {
                        File resourceFile =
                            new File(res.file.getCanonicalPath()
                                        + File.separator + name);

                        if (resourceFile.exists()) {
                            // If we make it this far,
                            // the resource is in the directory.
                            return  resourceFile.toURL();
                        }

                    } catch (IOException e) {
                        _logger.log(Level.INFO, CULoggerInfo.exceptionInASURLClassLoader, e);
                    }
                }

                return null;

            } // End for -- each URL in classpath.
        });

        return (URL) result;
    }

    public URL findResource(String name) {

        // quick quick that relies on 'doneCalled' being 'volatile'
        if( doneCalled ) {
            _logger.log(Level.WARNING,
                    CULoggerInfo.getString(CULoggerInfo.findResourceAfterDone, name, this.toString()),
                    new Throwable());
            return null;
        }

        // This code is dubious, because it iterates over items that could
        // be changing via another thread.   It appears that since 'urlSet' cannot shrink
        // that the iteration at least won't go out of bounds.  And it's probably OK
        // if more than one thread adds the same resource to 'notFoundResources'.
        //
        // HOWEVER, there is still a race condition from the check for 'doneCalled' above.
        // That's OK for 'notFoundResources', but it could lead to an ArrayIndexOutOfBounds
        // excpetion for 'urlSet', should the set be cleared while looping.

        // resource is in the not found list
        String nf = (String) notFoundResources.get(name);
        if (nf != null && nf.equals(name) ) {
            return null;
        }

        int i = 0;
        synchronized(this) {
            for (final URLEntry u : this.urlSet) {

                if (!u.hasItem(name)) {
                    i++;
                    continue;
                }

                final URL url = findResource0(u, name);
                if (url != null) return url;
                i++;
            }
        }

        // add resource to the not found list
        notFoundResources.put(name, name);

        return null;
    }

    /**
     * Returns an enumeration of java.net.URL objects
     * representing all the resources with the given name.
     *
     * This method is synchronized to avoid (a) race condition checking 'doneCalled',
     * (b) changes to contents or length of 'resourcesList' and/or 'notFoundResources' while iterating
     * over them, (c) thread visibility to all of the above.
     */
    public synchronized Enumeration<URL>
    findResources(String name) throws IOException {
        if( doneCalled ) {
            _logger.log(Level.WARNING, CULoggerInfo.doneAlreadyCalled,
                        new Object[] { name, doneSnapshot });
            // return an empty enumeration instead of null. See issue #13096
            return Collections.enumeration(Collections.EMPTY_LIST);
        }
        List<URL> resourcesList = new ArrayList<URL>();

        // resource is in the not found list
        final String nf = (String) notFoundResources.get(name);
        if (nf != null && nf.equals(name) ) {
            return (new Vector(resourcesList)).elements();
        }

        for (Iterator<URLEntry> iter = this.urlSet.iterator(); iter.hasNext();) {
            final URLEntry urlEntry = iter.next();
            final URL url = findResource0(urlEntry, name);
            if (url != null) {
                resourcesList.add(url);
            }
        }

        if (resourcesList.size() == 0) {
            // add resource to the not found list
            notFoundResources.put(name, name);
        }

        return (new Vector(resourcesList)).elements();
    }



    /**
     * Checks the manifest of the given jar file.
     *
     * @param    jar    the jar file that may contain manifest class path
     * @param    file   file pointer to the jar
     *
     * @throws   IOException   if an i/o error
     */
    private void checkManifest(JarFile jar, File file) throws IOException {

        if ( (jar == null) || (file == null) ) return;

        Manifest man = jar.getManifest();
        if (man == null) return;

        synchronized (this) {
            String cp = man.getMainAttributes().getValue(
                                        Attributes.Name.CLASS_PATH);
            if (cp == null) return;

            StringTokenizer st = new StringTokenizer(cp, " ");

            while (st.hasMoreTokens()) {
                final String entry = st.nextToken();

                final File newFile = new File(file.getParentFile(), entry);

                // add to class path of this class loader
                try {
                    appendURL(newFile);
                } catch (MalformedURLException ex) {
                    _logger.log(Level.SEVERE, CULoggerInfo.exceptionInASURLClassLoader, ex);
                }
            }
        }
    }

    /**
     * Internal implementation of load class.
     *
     * @param    res        url resource entry
     * @param    entryName  name of the class
     */
    private byte[] loadClassData0(final URLEntry res, final String entryName) {

        Object result =
        AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                InputStream classStream = null;
                try {

                    if (res.isJar) { // It is a jarfile..
                        JarFile zip = res.zip;
                        JarEntry entry = zip.getJarEntry(entryName);
                        if (entry != null) {
                            classStream = zip.getInputStream(entry);
                            byte[] classData = getClassData(classStream);
                            res.setProtectionDomain(ASURLClassLoader.this, entry.getCertificates());
                            return classData;
                        }
                    } else { // Its a directory....
                        File classFile = new File (res.file,
                                    entryName.replace('/', File.separatorChar));

                        if (classFile.exists()) {
                            try {
                                classStream = new FileInputStream(classFile);
                                byte[] classData = getClassData(classStream);
                                res.setProtectionDomain(ASURLClassLoader.this, null);
                                return classData;
                            } finally {
                                /*
                                 *Close the stream only if this is a directory.  The stream for
                                 *a jar/zip file was opened elsewhere and should remain open after this
                                 *method completes.
                                 */
                                if (classStream != null) {
                                    try {
                                        classStream.close();
                                    } catch (IOException closeIOE) {
                                        _logger.log(Level.INFO, "loader.excep_in_asurlclassloader", closeIOE);
                                    }
                                }
                            }
                        }
                    }
                } catch (IOException ioe) {
                    _logger.log(Level.INFO, CULoggerInfo.exceptionInASURLClassLoader, ioe);
                }
                return null;
            }
        });
        return (byte[]) result;
    }
    
    @Override
    public void addEEPermissions(PermissionCollection eePc) 
        throws SecurityException {        
        // sm on
        if (System.getSecurityManager() != null) {
            System.getSecurityManager().checkSecurityAccess(
                    DDPermissionsLoader.SET_EE_POLICY);

            permissionsHolder.setEEPermissions(eePc);
        }
    }

    @Override    
    public void addDeclaredPermissions(PermissionCollection declaredPc 
            ) throws SecurityException {
        
        if (System.getSecurityManager() != null) {
            System.getSecurityManager().checkSecurityAccess(
                    DDPermissionsLoader.SET_EE_POLICY);
        
            permissionsHolder.setDeclaredPermissions(declaredPc);
        }
        
    }

    
    
    @Override
    protected PermissionCollection getPermissions(CodeSource codeSource) {
        
        PermissionCollection cachedPc = 
            permissionsHolder.getCachedPerms(codeSource);
        if (cachedPc != null)
            return cachedPc;
        
        return permissionsHolder.getPermissions(
                codeSource, super.getPermissions(codeSource));
    }
    
    

    /** THREAD SAFETY: what happens when more than one thread requests the same class
        and thus works on the same classData?  Or defines the same package?  Maybe
        the same work just gets done twice, and that's all.
        CAUTION: this method might be overriden, and subclasses must be cautious (also)
        about thread safety.
     */
    protected Class findClass(String name) throws ClassNotFoundException {
        ClassData classData = findClassData(name);
        // Instruments the classes if the profiler's enabled
        if (PreprocessorUtil.isPreprocessorEnabled()) {
            // search thru the JARs for a file of the form java/lang/Object.class
            final String entryName = name.replace('.', '/') + ".class";
            classData.setClassBytes(PreprocessorUtil.processClass(entryName, classData.getClassBytes()));
        }

        // Define package information if necessary
        int lastPackageSep = name.lastIndexOf('.');
        if ( lastPackageSep != -1 ) {
            String packageName = name.substring(0, lastPackageSep);
            if( getPackage(packageName) == null ) {
                try {

                    // There's a small chance that one of our parents
                    // could define the same package after getPackage
                    // returns null but before we call definePackage,
                    // since the parent classloader instances
                    // are not locked.  So, just catch the exception
                    // that is thrown in that case and ignore it.
                    //
                    // It's unclear where we would get the info to
                    // set all spec and impl data for the package,
                    // so just use null.  This is consistent will the
                    // JDK code that does the same.
                    definePackage(packageName, null, null, null,
                                  null, null, null, null);
                } catch(IllegalArgumentException iae) {
                    // duplicate attempt to define same package.
                    // safe to ignore.
                    _logger.log(Level.FINE, "duplicate package " +
                        "definition attempt for " + packageName, iae);
                }
            }
        }

        // Loop though the transformers here!!
        try {
            final ArrayList<ClassFileTransformer> xformers = (ArrayList<ClassFileTransformer>) transformers.clone();
            for ( final ClassFileTransformer transformer : xformers) {

                // see javadocs of transform().
                // It expects class name as java/lang/Object
                // as opposed to java.lang.Object
                final String internalClassName = name.replace('.','/');
                final byte[] transformedBytes = transformer.transform(this, internalClassName, null,
                        classData.pd, classData.getClassBytes());
                if(transformedBytes!=null){ // null indicates no transformation
                    _logger.log(Level.INFO, CULoggerInfo.actuallyTransformed, name);
                    classData.setClassBytes(transformedBytes);
                }
            }
        } catch (IllegalClassFormatException icfEx) {
            throw new ClassNotFoundException(icfEx.toString(), icfEx);
        }
        Class clazz = null;
        try {
            byte[] bytes = classData.getClassBytes();
            clazz = defineClass(name, bytes, 0, bytes.length, classData.pd);
            return clazz;
        } catch (UnsupportedClassVersionError ucve) {
 	    throw new UnsupportedClassVersionError(
 	        sm.getString("ejbClassLoader.unsupportedVersion", name,
 	                     System.getProperty("java.version")));
        }
    }

    /**
     * This method is responsible for locating the url from the class bytes
     * have to be read and reading the bytes. It does not actually define
     * the Class object.
     * <p>
     * To preclude a race condition on checking 'doneCalled', as well as transient errors
     * if done() is called while running, this method is 'synchronized'.

     * @param name class name in java.lang.Object format
     * @return class bytes as well protection domain information
     * @throws ClassNotFoundException
     */
    protected synchronized ClassData findClassData(String name) throws ClassNotFoundException {

        if( doneCalled ) {
            _logger.log(Level.WARNING,
                    CULoggerInfo.getString(CULoggerInfo.findClassAfterDone, name, this.toString()),
                    new Throwable());
            throw new ClassNotFoundException(name);
        }

        String nf = (String) notFoundClasses.get(name);
        if (nf != null && nf.equals(name) ) {
            throw new ClassNotFoundException(name);
        }

        // search thru the JARs for a file of the form java/lang/Object.class
        String entryName = name.replace('.', '/') + ".class";

        int i = 0;
        for (URLEntry u : this.urlSet) {
            if (!u.hasItem(entryName)) {
                i++;
                continue;
            }

            byte[] result = loadClassData0(u, entryName);
            if (result != null) {
                if (System.getSecurityManager() == null)
                    return new ClassData(result, u.pd);
                else {
                    //recreate the pd to include the declared permissions
                    CodeSource cs = u.pd.getCodeSource();
                    PermissionCollection pc = this.getPermissions(cs);
                    ProtectionDomain pdWithPemissions = 
                        new ProtectionDomain(u.pd.getCodeSource(), pc, u.pd.getClassLoader(), u.pd.getPrincipals());
                    return new ClassData(result, pdWithPemissions);
                }
            }
            i++;
        }

        // add to the not found classes list
        notFoundClasses.put(name, name);

        throw new ClassNotFoundException(name);
    }

    /**
     * Returns the byte array from the given input stream.
     *
     * @param    istream    input stream to the class or resource
     *
     * @throws   IOException  if an i/o error
     */
    private byte[] getClassData(InputStream istream) throws IOException {

        BufferedInputStream bstream = new BufferedInputStream(istream);;
        byte[] buf = new byte[4096];
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        int num = 0;
        try {
            while( (num = bstream.read(buf)) != -1) {
                bout.write(buf, 0, num);
            }
        } finally {
            if (bstream != null) {
                try {
                    bstream.close();
                } catch (IOException closeIOE) {
                    ASURLClassLoader._logger.log(Level.INFO,
                            CULoggerInfo.exceptionInASURLClassLoader, closeIOE);
                }
            }
        }

        return bout.toByteArray();
    }


    protected String getClassLoaderName() {
        return "ASURLClassLoader";
    }

    /**
     * Returns a string representation of this class loader.
     *
     * @return   a string representation of this class loader
     */
    public String toString() {

        StringBuffer buffer = new StringBuffer();

        buffer.append(getClassLoaderName() + " : \n");
        if( doneCalled ) {
            buffer.append("doneCalled = true" + "\n");
            String snapshot = doneSnapshot; // MUST use temp for thread safety; could go null after checking
            if( snapshot != null ) {
                buffer.append("doneSnapshot = " + snapshot);
            }
        } else {
            buffer.append("urlSet = " + this.urlSet + "\n");
            buffer.append("doneCalled = false " + "\n");
        }
        buffer.append(" Parent -> " + getParent() + "\n");

        return buffer.toString();
    }

    public InputStream getResourceAsStream(final String name) {
        InputStream stream = super.getResourceAsStream(name);
        /*
         *Make sure not to wrap the stream if it already is a wrapper.
         */
        if (stream != null) {
            if (! (stream instanceof SentinelInputStream)) {
                stream = new SentinelInputStream(stream);
            }
        }
        return stream;
    }

    /**
     * The JarFile objects loaded in the classloader may get exposed to the
     * application code (e.g. EJBs) through calls of
     * ((JarURLConnection) getResource().openConnection()).getJarFile().
     *
     * This class protects the jar file from being closed by such an application.
     *
     * @author fkieviet
     */
    private static final class ProtectedJarFile extends JarFile {
        /**
         * Constructor
         *
         * @param file File
         * @throws IOException from parent
         */
        public ProtectedJarFile(File file) throws IOException {
            super(file);
        }

        /**
         * Do nothing
         *
         * @see java.util.zip.ZipFile#close()
         *
         * Byron sez:  I wonder what's going on here?!?  This looks quite weird.
         * Why not just get rid of both reallyClose() and close() and finalize()
         * -- and just rely on the superclass?
         * At any rate I am not messing with it today, 1/9/2013.  Mainly because
         * maybe close() is called outside and we do NOT want it to really close?
         * Here is what happens at finalize time:
         * 1. ASURLClassLoader$ProtectedJarFile.finalize()
         * 2. java.util.zip.ZipFile.finalize()
         * 3. ASURLClassLoader$ProtectedJarFile.close()
         * 4. dumps a WARNING log message
         * 5. reallyClose()
         * 6. java.util.zip.ZipFile.close()
         * I
         */
        public void close() {
            // do nothing
        }

        /**
         * Really close the jar file
         *
         * @throws IOException from parent
         */
        public void reallyClose() throws IOException {
            super.close();
        }

        /**
         * @see java.lang.Object#finalize()
         */
        protected void finalize() throws IOException {
            super.finalize();
            reallyClose();
        }
    }

    /**
     * URL entry - keeps track of the url resources.
     */
    protected static final class URLEntry {
        /** the url, ensure thread visibility by making it 'final' */
        final URL source;

        /** file of the url,
            ensure thread visibility by making it 'volatile' */
        volatile File file       = null;

        /** jar file if url is a jar else null,
            ensure thread visibility by making it 'volatile'  */
        volatile ProtectedJarFile zip     = null;

        /** true if url is a jar,
            ensure thread visibility by making it 'volatile'  */
        volatile boolean isJar  = false;

        /** ensure thread visibility by making it 'volatile'  */
        volatile Hashtable<String,String> table = null;

        /** ProtectionDomain with signers if jar is signed,
            ensure thread visibility by making it 'volatile'  */
        volatile ProtectionDomain pd = null;

        URLEntry(URL url) throws IOException {
            source = url;
            init();
        }

        void init() throws IOException {
            try {
                file    = new File(source.toURI());
                isJar  = file.isFile();

                if (isJar) {
                    zip = new ProtectedJarFile(file);
                }

                table = new Hashtable<String,String>();
//                cacheItems();
            } catch (URISyntaxException use) {
                IOException ioe= new IOException();
                ioe.initCause(use);
                throw ioe;
            }
        }

        private void fillTable(File f, Hashtable t, String parent) throws IOException {

            String localName = (parent.equals("")) ? "" : parent + "/";

            File[] children = f.listFiles();
            for (int i = 0; i < children.length; i++) {
                processFile(children[i],  t, localName);
            }
        }

        /**
         *Adds a file (or, if a directory, the directory's contents) to the table
         *of files this loader knows about.
         *<p>
         *Invokes fillTable for subdirectories which in turn invokes processFile
         *recursively.
         *@param fileToProcess the File to be processed
         *@param t the Hashtable that holds the files the loader knows about
         *@param parentLocalName prefix to be used for the full path; should be
         *non-empty only for recursive invocations
         *@throws IOException in case of errors working with the fileToProcess
         */
        private void processFile(File fileToProcess, Hashtable t, String parentLocalName) throws IOException {
            String key = parentLocalName + fileToProcess.getName();
            if (fileToProcess.isFile()) {
                t.put(key, key);
            } else if (fileToProcess.isDirectory()) {
                fillTable(fileToProcess, t, key);
            }
        }


        boolean hasItem(String item) {
            // in the case of ejbc stub compilation, asurlclassloader is created before stubs
            // gets generated, thus we need to return true for this case.
            if (table.size() == 0) {
                return true;
            }

            /*
             *Even with the previous special handling, a file could be created
             *in a directory after the loader was created and its table of
             *URLEntry names populated.  So check the table first and, if
             *the target item is not there and this URLEntry is for a directory, look for
             *the file.  If the file is now present but was not when the loader
             *was created, add an entry for the file in the table.
             */
            boolean result = false;
            String target = item;
            // special handling
            if (item.startsWith("./")) {
                target = item.substring(2, item.length());
            }

            result = table.containsKey(target);
            if ( ! result && ! isJar) {
                /*
                 *If the file exists now then it has been added to the directory since the
                 *loader was created.  Add it to the table of files we
                 *know about.
                 */
                File targetFile = privilegedCheckForFile(target);
                if (targetFile != null) {
                    try {
                        processFile(targetFile, table, "");
                        result = true;
                    } catch (IOException ioe) {
                        _logger.log(Level.SEVERE,
                                CULoggerInfo.getString(CULoggerInfo.exceptionProcessingFile, target, file.getAbsolutePath()),
                                ioe);
                        return false;
                    }
                }
            }
            return result;
        }

        /**
         *Returns a File object for the requested path within the URLEntry.
         *<p>
         *Runs privileged because user code could trigger invocations of this
         *method.
         *@param targetPath the relative path to look for
         *@return File object for the requested file; null if it does not exist or
         *in case of error
         */
        private File privilegedCheckForFile(final String targetPath) {
            /*
             *Check for the file existence with privs, because this code can
             *be invoked from user code which may not otherwise have access
             *to the directories of interest.
             */
            try {
                File result = (File) AccessController.doPrivileged(new PrivilegedExceptionAction() {
                        public Object run() throws Exception {

                            File targetFile = new File(file, targetPath);
                            if ( ! targetFile.exists()) {
                                targetFile = null;
                            }
                            return targetFile;
                        }
                    });

                return result;

            } catch (PrivilegedActionException pae) {
                /*
                 *Log any exception and return false.
                 */
                _logger.log(Level.SEVERE,
                        CULoggerInfo.getString(CULoggerInfo.exceptionCheckingFile, targetPath, file.getAbsolutePath()),
                        pae.getCause());
                return null;
            }
        }

          /**
           * Sets ProtectionDomain with CodeSource including Signers in
           * Entry for use in call to defineClass.
           * @param signers the array of signer certs or null
           */
         public void setProtectionDomain (ClassLoader ejbClassLoader, Certificate[] signers) throws MalformedURLException {
             if (pd == null) {
                 pd = new ProtectionDomain(new CodeSource(file.toURL(),signers),null, ejbClassLoader, null );
             }
         }

        public String toString() {
            return "URLEntry : " + source.toString();
        }

        /**
         * Returns true if two URL entries has equal URLs.
         *
         * @param  obj   URLEntry to compare against
         * @return       true if both entry has equal URL
         */
        public boolean equals(Object obj) {

            boolean tf = false;

            if (obj instanceof URLEntry) {
                URLEntry e = (URLEntry) obj;
                try {
 	 	            //try comparing URIs
 	 	            if (source.toURI().equals(e.source.toURI())) {
 	 	                tf = true;
 	 	            }
 	 	        } catch (URISyntaxException e1) {
                    // We should never get here, because we call init() in the constructor and
                    // init() would have thrown an exception if the URL could not be converted to a valid URI.
                    assert(false);
                    throw new RuntimeException(e1);
                }
            }

            return tf;
        }

        /**
         * Since equals is overridden, we need to override hashCode as well.
         */
        public int hashCode() {
            try {
 	 	        return source.toURI().hashCode();
 	 	    } catch (URISyntaxException e) {
                // We should never get here, because we call init() in the constructor and
                // init() would have thrown an exception if the URL could not be converted to a valid URI.
                assert(false);
                throw new RuntimeException(e);
            }
        }

    }

    /**
     *Returns the vector of open streams; creates it if needed.
     *@return Vector<SentinelInputStream> holding open streams
     */
    private Vector<SentinelInputStream> getStreams() {
        return streams;
    }

    /**
     *Closes any streams that remain open, logging a warning for each.
     *<p>
     *This method should be invoked when the loader will no longer be used
     *and the app will no longer explicitly close any streams it may have opened.
     * Must be synchnronized to (a) avoid race condition checking 'streams'.
     */
    private synchronized void closeOpenStreams() {
        if (streams != null) {

            SentinelInputStream[] toClose = streams.toArray(new SentinelInputStream[streams.size()]);
            for (SentinelInputStream s : toClose) {
                try {
                    s.closeWithWarning();
                } catch (IOException ioe) {
                    _logger.log(Level.WARNING, CULoggerInfo.exceptionClosingStream, ioe);
                }
            }
            streams.clear();
        }
    }

    /**
     * Wraps all InputStreams returned by this class loader to report when
     * a finalizer is run before the stream has been closed.  This helps
     * to identify where locked files could arise.
     * @author vtsyganok
     * @author tjquinn
     */
    protected final class SentinelInputStream extends FilterInputStream {
        private volatile boolean closed = false;
        private final Throwable throwable;

        /**
         * Constructs new FilteredInputStream which reports InputStreams not closed properly.
         * When the garbage collector runs the finalizer.  If the stream is still open this class will
         * report a stack trace showing where the stream was opened.
         *
         * @param in - InputStream to be wrapped
         */
        protected SentinelInputStream(final InputStream in) {
            super(in);
            throwable = new Throwable();
            getStreams().add(this);
        }

        /**
         * Closes underlying input stream.
         */
        public void close() throws IOException {
            _close();
        }

        /**
         * Invoked by Garbage Collector. If underlying InputStream was not closed properly,
         * the stack trace of the constructor will be logged!
         *
         * 'closed' is 'volatile', but it's a race condition to check it and how this code
         * relates to _close() is unclear.
         */
        protected void finalize() throws Throwable {
            if (!closed && this.in != null){
                try {
                    in.close();
                }
                catch (IOException ignored){
                    //Cannot do anything here.
                }
                //Well, give them a stack trace!
                report();
            }
            super.finalize();
        }

        private synchronized void _close() throws IOException {
            if ( closed ) {
                return;
            }
            // race condition with above check, but should have no harmful effects

            closed = true;
            getStreams().remove(this);
            super.close();
        }

        private void closeWithWarning() throws IOException {
            _close();
            report();
        }

        /**
         * Report "left-overs"!
         */
        private void report(){
            _logger.log(Level.WARNING, CULoggerInfo.inputStreamFinalized, this.throwable);
        }
    }
    /**
     * To properly close streams obtained through URL.getResource().getStream():
     * this opens the input stream on a JarFile that is already open as part
     * of the classloader, and returns a sentinel stream on it.
     *
     * @author fkieviet
     */
    private class InternalJarURLConnection extends JarURLConnection {
        private final URLEntry mRes;
        private final String mName;

        /**
         * Constructor
         *
         * @param url the URL that is a stream for
         * @param res URLEntry
         * @param name String
         * @throws MalformedURLException from super class
         */
        public InternalJarURLConnection(URL url, URLEntry res, String name)
            throws MalformedURLException {
            super(url);
            mRes = res;
            mName = name;
        }

        /**
         * @see java.net.JarURLConnection#getJarFile()
         */
        public JarFile getJarFile() throws IOException {
            return mRes.zip;
        }

        /**
         * @see java.net.URLConnection#connect()
         */
        public void connect() throws IOException {
            // Nothing
        }

        /**
         * @see java.net.URLConnection#getInputStream()
         */
        public InputStream getInputStream() throws IOException {
            // When there is no entry name specified (this can happen for url like jar:file:///tmp/foo.jar!/),
            // we must throw an IOException as that's the behavior of JarURLConnection as well.
            if ("".equals(mName)) {
                throw new IOException("no entry name specified");
            }
            ZipEntry entry = mRes.zip.getEntry(mName);
            if (entry == null) {
                throw new IOException("no entry called " + mName + " found in " + mRes.source);
            }
            return new SentinelInputStream(mRes.zip.getInputStream(entry));
        }
    }

    /**
     * To properly close streams obtained through URL.getResource().getStream():
     * an instance of this class is instantiated for each and every URL object
     * created by this classloader. It provides a custom JarURLConnection
     * (InternalJarURLConnection) so that the stream can be obtained from an already
     * open jar file.
     *
     * @author fkieviet
     */
    private class InternalURLStreamHandler extends URLStreamHandler {
        /** must be 'volatile' for thread visibility */
        private volatile URL   mURL;
        private final URLEntry mRes;

        /**
         * Constructor
         *
         * @param res URLEntry
         * @param name String
         */
        public InternalURLStreamHandler(URLEntry res, String name) {
            mRes = res;
        }

        /**
         * @see java.net.URLStreamHandler#openConnection(java.net.URL)
         */
        protected URLConnection openConnection(final URL u) throws IOException {
            String path = u.getPath();
            int separator = path.lastIndexOf('!');
            assert(separator != -1); // we deal with jar urls only
            try {
                URI jarFileURI = new URI(path.substring(0, separator));
                if (!jarFileURI.equals(mRes.file.toURI())) {
                    throw new IOException("Cannot open a foreign URL; this.url=" + mURL
                        + "; foreign.url=" + u);
                }
                String entryName = path.substring(separator+1);
                if (entryName != null) {
                    assert (entryName.startsWith("/"));
                    entryName = entryName.substring(1);
                }
                return new InternalJarURLConnection(u, mRes, entryName);
            } catch (URISyntaxException e) {
                throw new IOException(e);
            }
        }

        /**
         * Ties the URL that this handler is associated with to the handler, so
         * that it can be asserted that somehow no other URLs are mangled in (this
         * is theoretically impossible)
         *
         * @param url URL
         */
        public void tieUrl(URL url) {
            // is it OK to call this twice and whack the variable a second time?
            if ( mURL != null )
            {
                throw new IllegalStateException("Setting the URL more than once not allowed" );
            }
            mURL = url;
        }
    }

    /**
     * This class is used as return value of findClassIntenal method  to return
     * both class bytes and protection domain.
     */
    private static final class ClassData {
        // Byron Sez: making classBytes volatile is pointless.  That just makes
        // the reference to the array volatile.  The byte contents are NOT volatile
        // I solved this low-level FB issue by:
        // 1. don't use the field directly outside of this inner class
        // 2. add a getter/setter
        // 3. make both the setter and getter synchronized.

        private byte[] classBytes;

        /** must be 'final' to ensure thread visibility */
        private final ProtectionDomain pd;

        ClassData(byte[] classBytes, ProtectionDomain pd) {
            this.classBytes = classBytes;
            this.pd = pd;
        }
        private synchronized byte[] getClassBytes() {
            return classBytes;
        }

        private synchronized void setClassBytes(byte[] newBytes) {
            classBytes = newBytes;
        }

    }

    /**
     * This class loader only provides a new class loading namespace
     * so that persistence provider can load classes in that separate
     * namespace while scanning annotations.
     * This class loader delegates all stream handling (i.e. reading
     * actual class/resource data) operations to the application class loader.
     * It only defines the Class using the byte codes.
     * Motivation behind this class is discussed at
     * https://glassfish.dev.java.net/issues/show_bug.cgi?id=237.
     */
    private static final class DelegatingClassLoader extends SecureClassLoader {

        /**
         * The application class loader which is used to read class data.
         * Made 'final' to ensure thread visibility.
         */
        private final ASURLClassLoader delegate;

        /**
         * Create a new instance.
         * @param applicationCL  is the original class loader associated
         * with this application. The new class loader uses it to delegate
         * stream handling operations. The new class loader also uses
         * applicationCL's parent as its own parent.
         */
        DelegatingClassLoader(ASURLClassLoader applicationCL) {
            super(applicationCL.getParent()); // normal class loading delegation
            this.delegate = applicationCL;
        }

        /**
         * This method uses the delegate to use class bytes and then defines
         * the class using this class loader
         */
        protected Class findClass(String name) throws ClassNotFoundException {
            ClassData classData = delegate.findClassData(name);
            // Define package information if necessary
            int lastPackageSep = name.lastIndexOf('.');
            if ( lastPackageSep != -1 ) {
                String packageName = name.substring(0, lastPackageSep);
                if( getPackage(packageName) == null ) {
                    try {
                        // There's a small chance that one of our parents
                        // could define the same package after getPackage
                        // returns null but before we call definePackage,
                        // since the parent classloader instances
                        // are not locked.  So, just catch the exception
                        // that is thrown in that case and ignore it.
                        //
                        // It's unclear where we would get the info to
                        // set all spec and impl data for the package,
                        // so just use null.  This is consistent will the
                        // JDK code that does the same.
                        definePackage(packageName, null, null, null,
                                      null, null, null, null);
                    } catch(IllegalArgumentException iae) {
                        // duplicate attempt to define same package.
                        // safe to ignore.
                        _logger.log(Level.FINE, "duplicate package " +
                            "definition attempt for " + packageName, iae);
                    }
                }
            }
            Class clazz = null;
            try {
                final byte[] bytes = classData.getClassBytes();
                clazz = defineClass(name, bytes, 0, bytes.length, classData.pd);
                return clazz;
            } catch (UnsupportedClassVersionError ucve) {
 	        throw new UnsupportedClassVersionError(
 	            sm.getString("ejbClassLoader.unsupportedVersion", name,
 	                         System.getProperty("java.version")));
            }
        }

        protected URL findResource(String name) {
            return delegate.findResource(name);
        }

        protected Enumeration<URL> findResources(String name) throws IOException {
            return delegate.findResources(name);
        }

    }
}

