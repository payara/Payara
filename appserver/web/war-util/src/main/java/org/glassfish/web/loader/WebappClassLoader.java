/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.glassfish.web.loader;

import com.sun.appserv.BytecodePreprocessor;
import com.sun.appserv.ClassLoaderUtil;
import com.sun.appserv.server.util.PreprocessorUtil;
import com.sun.enterprise.util.io.FileUtils;
import org.apache.naming.JndiPermission;
import org.apache.naming.resources.DirContextURLStreamHandler;
import org.apache.naming.resources.ProxyDirContext;
import org.apache.naming.resources.Resource;
import org.apache.naming.resources.ResourceAttributes;
import org.glassfish.api.deployment.InstrumentableClassLoader;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.logging.annotation.LoggerInfo;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;
import org.glassfish.web.util.ExceptionUtils;
import org.glassfish.web.util.IntrospectionUtils;
import org.glassfish.hk2.api.PreDestroy;
import com.sun.enterprise.security.integration.DDPermissionsLoader;
import com.sun.enterprise.security.integration.PermsHolder;
import com.sun.enterprise.security.perms.PermissionsProcessor;
import com.sun.enterprise.security.perms.SMGlobalPolicyUtil;

import javax.naming.Binding;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import java.io.*;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Specialized web application class loader.
 * <p>
 * This class loader is a full reimplementation of the
 * <code>URLClassLoader</code> from the JDK. It is desinged to be fully
 * compatible with a normal <code>URLClassLoader</code>, although its internal
 * behavior may be completely different.
 * <p>
 * <strong>IMPLEMENTATION NOTE</strong> - This class loader faithfully follows
 * the delegation model recommended in the specification. The system class
 * loader will be queried first, then the local repositories, and only then
 * delegation to the parent class loader will occur. This allows the web
 * application to override any shared class except the classes from J2SE.
 * Special handling is provided from the JAXP XML parser interfaces, the JNDI
 * interfaces, and the classes from the servlet API, which are never loaded
 * from the webapp repository.
 * <p>
 * <strong>IMPLEMENTATION NOTE</strong> - Due to limitations in Jasper
 * compilation technology, any repository which contains classes from
 * the servlet API will be ignored by the class loader.
 * <p>
 * <strong>IMPLEMENTATION NOTE</strong> - The class loader generates source
 * URLs which include the full JAR URL when a class is loaded from a JAR file,
 * which allows setting security permission at the class level, even when a
 * class is contained inside a JAR.
 * <p>
 * <strong>IMPLEMENTATION NOTE</strong> - Local repositories are searched in
 * the order they are added via the initial constructor and/or any subsequent
 * calls to <code>addRepository()</code> or <code>addJar()</code>.
 * <p>
 * <strong>IMPLEMENTATION NOTE</strong> - No check for sealing violations or
 * security is made unless a security manager is present.
 *
 * @author Remy Maucherat
 * @author Craig R. McClanahan
 * @version $Revision: 1.1.2.1 $ $Date: 2007/08/17 15:46:27 $
 */
public class WebappClassLoader
    extends URLClassLoader
    implements Reloader, InstrumentableClassLoader, PreDestroy, DDPermissionsLoader 
{
    // ------------------------------------------------------- Static Variables

    @LogMessagesResourceBundle
    private static final String SHARED_LOGMESSAGE_RESOURCE =
            "org.glassfish.web.loader.LogMessages";

    @LoggerInfo(subsystem="WEB", description="WEB Util Logger", publish=true)
    private static final String WEB_UTIL_LOGGER = "javax.enterprise.web.util";

    public static final Logger logger =
            Logger.getLogger(WEB_UTIL_LOGGER, SHARED_LOGMESSAGE_RESOURCE);

    private static final ResourceBundle rb = logger.getResourceBundle();

    @LogMessageInfo(
            message = "Resource '{0}' is missing",
            level = "SEVERE",
            cause = "A naming exception is encountered",
            action = "Check the list of resources")
    public static final String MISSING_RESOURCE = "AS-WEB-UTIL-00001";

    @LogMessageInfo(
            message = "Failed tracking modifications of '{0} : {1}",
            level = "SEVERE",
            cause = "A ClassCastException is encountered",
            action = "Check if the object is an instance of the class")
    public static final String FAILED_TRACKING_MODIFICATIONS = "AS-WEB-UTIL-00002";

    @LogMessageInfo(
            message = "WebappClassLoader.findClassInternal({0}) security exception: {1}",
            level = "WARNING",
            cause = "An AccessControlException is encountered",
            action = "Check if the resource is accessible")
    public static final String FIND_CLASS_INTERNAL_SECURITY_EXCEPTION = "AS-WEB-UTIL-00003";

    @LogMessageInfo(
            message = "Security Violation, attempt to use Restricted Class: {0}",
            level = "INFO")
    public static final String SECURITY_EXCEPTION = "AS-WEB-UTIL-00004";

    @LogMessageInfo(
            message = "Class {0} has unsupported major or minor version numbers, which are greater than those found in the Java Runtime Environment version {1}",
            level = "WARNING")
    public static final String UNSUPPORTED_VERSION = "AS-WEB-UTIL-00005";

    @LogMessageInfo(
            message = "Unable to load class with name [{0}], reason: {1}",
            level = "WARNING")
    public static final String UNABLE_TO_LOAD_CLASS = "AS-WEB-UTIL-00006";

    @LogMessageInfo(
            message = "The web application [{0}] registered the JDBC driver [{1}] but failed to unregister it when the web application was stopped. To prevent a memory leak, the JDBC Driver has been forcibly unregistered.",
            level = "WARNING")
    public static final String CLEAR_JDBC = "AS-WEB-UTIL-00007";

    @LogMessageInfo(
            message = "JDBC driver de-registration failed for web application [{0}]",
            level = "WARNING")
    public static final String JDBC_REMOVE_FAILED = "AS-WEB-UTIL-00008";

    @LogMessageInfo(
            message = "Exception closing input stream during JDBC driver de-registration for web application [{0}]",
            level = "WARNING")
    public static final String JDBC_REMOVE_STREAM_ERROR = "AS-WEB-UTIL-00009";

    @LogMessageInfo(
            message = "This web container has not yet been started",
            level = "WARNING")
    public static final String NOT_STARTED = "AS-WEB-UTIL-00010";

    @LogMessageInfo(
            message = "Failed to check for ThreadLocal references for web application [{0}]",
            level = "WARNING")
    public static final String CHECK_THREAD_LOCALS_FOR_LEAKS_FAIL = "AS-WEB-UTIL-00011";

    @LogMessageInfo(
            message = "Unable to determine string representation of key of type [{0}]",
            level = "SEVERE",
            cause = "An Exception occurred",
            action = "Check the exception for error")
    public static final String CHECK_THREAD_LOCALS_FOR_LEAKS_BAD_KEY = "AS-WEB-UTIL-00012";

    @LogMessageInfo(
            message = "Unknown",
            level = "INFO")
    public static final String CHECK_THREAD_LOCALS_FOR_LEAKS_UNKNOWN = "AS-WEB-UTIL-00013";

    @LogMessageInfo(
            message = "Unable to determine string representation of value of type [{0}]",
            level = "SEVERE",
            cause = "An Exception occurred",
            action = "Check the exception for error")
    public static final String CHECK_THREAD_LOCALS_FOR_LEAKS_BAD_VALUE = "AS-WEB-UTIL-00014";

    @LogMessageInfo(
            message = "The web application [{0}] created a ThreadLocal with key of type [{1}] (value [{2}]). The ThreadLocal has been correctly set to null and the key will be removed by GC.",
            level = "FINE")
    public static final String CHECK_THREAD_LOCALS_FOR_LEAKS_DEBUG = "AS-WEB-UTIL-00015";

    @LogMessageInfo(
            message = "The web application [{0}] created a ThreadLocal with key of type [{1}] (value [{2}]) and a value of type [{3}] (value [{4}]) but failed to remove it when the web application was stopped. Threads are going to be renewed over time to try and avoid a probable memory leak.",
            level = "SEVERE",
            cause = "Failed to remove a ThreadLocal when the web application was stopped",
            action = "Threads are going to be renewed over time to try and avoid a probable memory leak.")
    public static final String CHECK_THREAD_LOCALS_FOR_LEAKS = "AS-WEB-UTIL-00016";

    @LogMessageInfo(
            message = "Failed to find class sun.rmi.transport.Target to clear context class loader for web application [{0}]. This is expected on non-Sun JVMs.",
            level = "INFO")
    public static final String CLEAR_RMI_INFO = "AS-WEB-UTIL-00017";

    @LogMessageInfo(
            message = "Failed to clear context class loader referenced from sun.rmi.transport.Target for web application [{0}]",
            level = "WARNING")
    public static final String CLEAR_RMI_FAIL = "AS-WEB-UTIL-00018";

    @LogMessageInfo(
            message = "Removed [{0}] ResourceBundle references from the cache for web application [{1}]",
            level = "FINE")
    public static final String CLEAR_REFERENCES_RESOURCE_BUNDLES_COUNT = "AS-WEB-UTIL-00019";

    @LogMessageInfo(
            message = "Failed to clear ResourceBundle references for web application [{0}]",
            level = "SEVERE",
            cause = "An Exception occurred",
            action = "Check the exception for error")
    public static final String CLEAR_REFERENCES_RESOURCE_BUNDLES_FAIL = "AS-WEB-UTIL-00020";

    @LogMessageInfo(
            message = "Illegal JAR entry detected with name {0}",
            level = "INFO")
    public static final String ILLEGAL_JAR_PATH = "AS-WEB-UTIL-00021";

    @LogMessageInfo(
            message = "Unable to validate JAR entry with name {0}",
            level = "INFO")
    public static final String VALIDATION_ERROR_JAR_PATH = "AS-WEB-UTIL-00022";

    @LogMessageInfo(
            message = "Unable to create {0}",
            level = "WARNING")
    public static final String UNABLE_TO_CREATE = "AS-WEB-UTIL-00023";

    @LogMessageInfo(
            message = "Unable to delete {0}",
            level = "WARNING")
    public static final String UNABLE_TO_DELETE = "AS-WEB-UTIL-00024";

    @LogMessageInfo(
            message = "Unable to read data for class with name [{0}]",
            level = "WARNING")
    public static final String READ_CLASS_ERROR = "AS-WEB-UTIL-00025";

    @LogMessageInfo(
            message = "Unable to purge bean classes from BeanELResolver",
            level = "WARNING")
    public static final String UNABLE_PURGE_BEAN_CLASSES = "AS-WEB-UTIL-00026";

    @LogMessageInfo(
            message = "Ignoring [{0}] during Tag Library Descriptor (TLD) processing",
            level = "WARNING")
    public static final String TLD_PROVIDER_IGNORE_URL = "AS-WEB-UTIL-00038";

    @LogMessageInfo(
            message = "Unable to determine TLD resources for [{0}] tag library, because class loader [{1}] for [{2}] is not an instance of java.net.URLClassLoader",
            level = "WARNING")
    public static final String UNABLE_TO_DETERMINE_TLD_RESOURCES = "AS-WEB-UTIL-00039";

    /**
     * Set of package names which are not allowed to be loaded from a webapp
     * class loader without delegating first.
     */
    private static final String[] packageTriggers = {
        "javax",                                     // Java extensions
        // START PE 4985680
        "sun",                                       // Sun classes
        // END PE 4985680
        "org.xml.sax",                               // SAX 1 & 2
        "org.w3c.dom",                               // DOM 1 & 2
        "org.apache.taglibs.standard",               // JSTL (Java EE 5)
        "com.sun.faces",                             // JSF (Java EE 5)
        "org.apache.commons.logging"                 // Commons logging
    };


    /**
     * All permission.
     */
    private static final Permission ALL_PERMISSION = new AllPermission();


    // ----------------------------------------------------- Instance Variables

    // START PE 4989455
    /**
     * Use this variable to invoke the security manager when a resource is
     * loaded by this classloader.
     */
    private boolean packageDefinitionEnabled =
         System.getProperty("package.definition") == null ? false : true;
    // END OF PE 4989455

    /**
     * Associated directory context giving access to the resources in this
     * webapp.
     */
    protected DirContext resources = null;

    /**
     * The cache of ResourceEntry for classes and resources we have loaded,
     * keyed by resource name.
     */
    protected ConcurrentHashMap<String, ResourceEntry> resourceEntries =
        new ConcurrentHashMap<String, ResourceEntry>();

    /**
     * The list of not found resources.
     */
    protected ConcurrentHashMap<String, String> notFoundResources =
        new ConcurrentHashMap<String, String>();

    /**
     * The debugging detail level of this component.
     */
    protected int debug = 0;

    /**
     * Should this class loader delegate to the parent class loader
     * <strong>before</strong> searching its own repositories (i.e. the
     * usual Java2 delegation model)?  If set to <code>false</code>,
     * this class loader will search its own repositories first, and
     * delegate to the parent only if the class or resource is not
     * found locally.
     */
    protected boolean delegate = false;

    /**
     * Last time a JAR was accessed.
     */
    protected long lastJarAccessed = 0L;

    /**
     * The list of local repositories, in the order they should be searched
     * for locally loaded classes or resources.
     */
    protected String[] repositories = new String[0];

    /**
     * Repositories URLs, used to cache the result of getURLs.
     */
    protected URL[] repositoryURLs = null;

    /**
     * Repositories translated as path in the work directory (for Jasper
     * originally), but which is used to generate fake URLs should getURLs be
     * called.
     */
    protected File[] files = new File[0];

    /**
     * The list of JARs, in the order they should be searched
     * for locally loaded classes or resources.
     */
    protected JarFile[] jarFiles = new JarFile[0];

    /**
     * The list of JARs, in the order they should be searched
     * for locally loaded classes or resources.
     */
    protected File[] jarRealFiles = new File[0];

    /**
     * The path which will be monitored for added Jar files.
     */
    protected String jarPath = null;

    /**
     * The list of JARs, in the order they should be searched
     * for locally loaded classes or resources.
     */
    protected List<String> jarNames = new ArrayList<String>();

    /**
     * The list of JARs last modified dates, in the order they should be
     * searched for locally loaded classes or resources.
     */
    protected long[] lastModifiedDates = new long[0];

    /**
     * The list of resources which should be checked when checking for
     * modifications.
     */
    protected String[] paths = new String[0];

    /**
     * A list of read File and Jndi Permission's required if this loader
     * is for a web application context.
     */
    private ConcurrentLinkedQueue<Permission> permissionList =
        new ConcurrentLinkedQueue<Permission>();
    
    //holder for declared and ee permissions
    private PermsHolder permissionsHolder;

    /**
     * Path where resources loaded from JARs will be extracted.
     */
    protected File loaderDir = null;

    protected String canonicalLoaderDir = null;

    /**
     * The PermissionCollection for each CodeSource for a web
     * application context.
     */
    private ConcurrentHashMap<String, PermissionCollection> loaderPC =
        new ConcurrentHashMap<String, PermissionCollection>();

    /**
     * Instance of the SecurityManager installed.
     */
    private SecurityManager securityManager = null;

    /**
     * The parent class loader.
     */
    private ClassLoader parent = null;

    /**
     * The system class loader.
     */
    private ClassLoader system = null;

    /**
     * Has this component been started?
     */
    protected boolean started = false;

    /**
     * Has external repositories.
     */
    protected boolean hasExternalRepositories = false;

    // START SJSAS 6344989
    /**
     * List of byte code pre-processors per webapp class loader.
     */
    private ConcurrentLinkedQueue<BytecodePreprocessor> byteCodePreprocessors =
            new ConcurrentLinkedQueue<BytecodePreprocessor>();
    // END SJSAS 6344989

    private boolean useMyFaces;

    // START PE 4985680
    /**
     * List of packages that may always be overridden, regardless of whether
     * they belong to a protected namespace (i.e., a namespace that may never
     * be overridden by any webapp)
     */
    private ConcurrentLinkedQueue<String> overridablePackages;
    // END PE 4985680

    private volatile boolean resourcesExtracted = false;

    /**
     * Should Tomcat attempt to null out any static or final fields from loaded
     * classes when a web application is stopped as a work around for apparent
     * garbage collection bugs and application coding errors? There have been
     * some issues reported with log4j when this option is true. Applications
     * without memory leaks using recent JVMs should operate correctly with this
     * option set to <code>false</code>. If not specified, the default value of
     * <code>false</code> will be used.
     */
    private boolean clearReferencesStatic = false;

    /**
     * Name of associated context used with logging and JMX to associate with
     * the right web application. Particularly useful for the clear references
     * messages. Defaults to unknown but if standard Tomcat components are used
     * it will be updated during initialisation from the resources.
     */
    private String contextName = "unknown";


    // ----------------------------------------------------------- Constructors

    /**
     * Construct a new ClassLoader with no defined repositories and no
     * parent ClassLoader.
     */
    public WebappClassLoader() {
        super(new URL[0]);
        init();
    }


    /**
     * Construct a new ClassLoader with the given parent ClassLoader,
     * but no defined repositories.
     */
    public WebappClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
        init();
    }


    /**
     * Construct a new ClassLoader with the given parent ClassLoader
     * and defined repositories.
     */
    public WebappClassLoader(URL[] urls, ClassLoader parent) {
        super(new URL[0], parent);

        if (urls != null && urls.length > 0) {
            for (URL url : urls) {
                super.addURL(url);
            }
        }

        init();
    }


    // ------------------------------------------------------------- Properties

    protected class PrivilegedFindResource
        implements PrivilegedAction<ResourceEntry> {

        private File file;
        private String path;

        PrivilegedFindResource(File file, String path) {
            this.file = file;
            this.path = path;
        }

        public ResourceEntry run() {
            return findResourceInternal(file, path);
        }
    }


    protected static final class PrivilegedGetClassLoader
        implements PrivilegedAction<ClassLoader> {

        public Class<?> clazz;

        public PrivilegedGetClassLoader(Class<?> clazz){
            this.clazz = clazz;
        }

        public ClassLoader run() {       
            return clazz.getClassLoader();
        }           
    }

    // START PE 4985680
    /**
     * Adds the given package name to the list of packages that may always be
     * overriden, regardless of whether they belong to a protected namespace
     */
    public synchronized void addOverridablePackage(String packageName){
        if (overridablePackages == null){
            overridablePackages = new ConcurrentLinkedQueue<String>();
        }
        overridablePackages.add(packageName);
    }
    // END PE 4985680


    /**
     * Get associated resources.
     */
    public DirContext getResources() {
        return this.resources;
    }


    /**
     * Set associated resources.
     */
    public void setResources(DirContext resources) {
        this.resources = resources;

        
        if (resources instanceof ProxyDirContext) {
            contextName = ((ProxyDirContext) resources).getContextName();
        }
    }


    /**
     * Return the context name for this class loader.
     */
    public String getContextName() {

        return (this.contextName);

    }


    public ConcurrentHashMap<String, ResourceEntry> getResourceEntries() {
        return resourceEntries;
    }


    /**
     * Return the debugging detail level for this component.
     */
    public int getDebug() {
        return (this.debug);
    }


    /**
     * Set the debugging detail level for this component.
     *
     * @param debug The new debugging detail level
     */
    public void setDebug(int debug) {
        this.debug = debug;
    }


    /**
     * Return the "delegate first" flag for this class loader.
     */
    public boolean getDelegate() {
        return (this.delegate);
    }


    /**
     * Set the "delegate first" flag for this class loader.
     *
     * @param delegate The new "delegate first" flag
     */
    public void setDelegate(boolean delegate) {

        this.delegate = delegate;

    }

    /**
     * If there is a Java SecurityManager create a read FilePermission
     * or JndiPermission for the file directory path.
     *
     * @param path file directory path
     */
    public void addPermission(String path) {
        if (path == null) {
            return;
        }

        if (securityManager != null) {
            
            securityManager.checkSecurityAccess(
                    DDPermissionsLoader.SET_EE_POLICY);

            Permission permission = null;
            if( path.startsWith("jndi:") || path.startsWith("jar:jndi:") ) {
                if (!path.endsWith("/")) {
                    path = path + "/";
                }
                permission = new JndiPermission(path + "*");
                permissionList.add(permission);
            } else {
                if (!path.endsWith(File.separator)) {
                    permission = new FilePermission(path, "read");
                    permissionList.add(permission);
                    path = path + File.separator;
                }
                permission = new FilePermission(path + "-", "read");
                permissionList.add(permission);
            }
        }
    }


    /**
     * If there is a Java SecurityManager create a read FilePermission
     * or JndiPermission for URL.
     *
     * @param url URL for a file or directory on local system
     */
    public void addPermission(URL url) {
        if (url != null) {
            addPermission(url.toString());
        }
    }


    /**
     * If there is a Java SecurityManager create a Permission.
     *
     * @param permission permission to add
     */
    public void addPermission(Permission permission) {
        if ((securityManager != null) && (permission != null)) {

            if (securityManager != null)
                securityManager.checkSecurityAccess(
                        DDPermissionsLoader.SET_EE_POLICY);

            permissionList.add(permission);
        }
    }
    
    
    @Override
    public void addDeclaredPermissions(PermissionCollection declaredPc 
            ) throws SecurityException {
        
        if (securityManager != null) {
            securityManager.checkSecurityAccess(
                    DDPermissionsLoader.SET_EE_POLICY);

            permissionsHolder.setDeclaredPermissions(declaredPc);
        }
    }
    
    @Override
    public void addEEPermissions(PermissionCollection eePc) 
         throws SecurityException {
        
        if (securityManager != null) {
            securityManager.checkSecurityAccess(
                    DDPermissionsLoader.SET_EE_POLICY);

            permissionsHolder.setEEPermissions(eePc);
        }
    }

    /**
     * Return the JAR path.
     */
    public String getJarPath() {
        return this.jarPath;
    }


    /**
     * Change the Jar path.
     */
    public void setJarPath(String jarPath) {
        this.jarPath = jarPath;
    }


    /**
     * Change the work directory.
     */
    public void setWorkDir(File workDir) {
        this.loaderDir = new File(workDir, "loader_" + this.hashCode());
        try {
            canonicalLoaderDir = this.loaderDir.getCanonicalPath();
            if (!canonicalLoaderDir.endsWith(File.separator)) {
                 canonicalLoaderDir += File.separator;
            }
        } catch (IOException ioe) {
            canonicalLoaderDir = null;
        }
    }


    public void setUseMyFaces(boolean useMyFaces) {
        this.useMyFaces = useMyFaces;
        if (useMyFaces) {
            addOverridablePackage("javax.faces");
            addOverridablePackage("com.sun.faces");
        }
    }


    /**
     * Return the clearReferencesStatic flag for this Context.
     */
    public boolean getClearReferencesStatic() {
        return (this.clearReferencesStatic);
    }


    /**
     * Set the clearReferencesStatic feature for this Context.
     *
     * @param clearReferencesStatic The new flag value
     */
    public void setClearReferencesStatic(boolean clearReferencesStatic) {
        this.clearReferencesStatic = clearReferencesStatic;
    }


    // ------------------------------------------------------- Reloader Methods


    /**
     * Add a new repository to the set of places this ClassLoader can look for
     * classes to be loaded.
     *
     * @param repository Name of a source of classes to be loaded, such as a
     *  directory pathname, a JAR file pathname, or a ZIP file pathname
     *
     * @exception IllegalArgumentException if the specified repository is
     *  invalid or does not exist
     */
    public void addRepository(String repository) {

        // Ignore any of the standard repositories, as they are set up using
        // either addJar or addRepository
        if (repository.startsWith("/WEB-INF/lib")
            || repository.startsWith("/WEB-INF/classes"))
            return;

        // Add this repository to our underlying class loader
        try {
            addRepository(new URL(repository));
        } catch (MalformedURLException e) {
            IllegalArgumentException iae = new IllegalArgumentException
                ("Invalid repository: " + repository);
            iae.initCause(e);
            throw iae;
        }

    }

    public void addRepository(URL url) {
        super.addURL(url);
        hasExternalRepositories = true;
    }

    /**
     * Add a new repository to the set of places this ClassLoader can look for
     * classes to be loaded.
     *
     * @param repository Name of a source of classes to be loaded, such as a
     *  directory pathname, a JAR file pathname, or a ZIP file pathname
     *
     * @exception IllegalArgumentException if the specified repository is
     *  invalid or does not exist
     */
    public synchronized void addRepository(String repository, File file) {

        // Note : There should be only one (of course), but I think we should
        // keep this a bit generic

        if (repository == null)
            return;

        if (logger.isLoggable(Level.FINER))
            logger.log(Level.FINER, "addRepository(" + repository + ")");

        int i;

        // Add this repository to our internal list
        String[] result = new String[repositories.length + 1];
        for (i = 0; i < repositories.length; i++) {
            result[i] = repositories[i];
        }
        result[repositories.length] = repository;
        repositories = result;

        // Add the file to the list
        File[] result2 = new File[files.length + 1];
        for (i = 0; i < files.length; i++) {
            result2[i] = files[i];
        }
        result2[files.length] = file;
        files = result2;

    }


    public synchronized void addJar(String jar, JarFile jarFile, File file)
        throws IOException {

        if (jar == null)
            return;
        if (jarFile == null)
            return;
        if (file == null)
            return;

        if (logger.isLoggable(Level.FINER))
            logger.log(Level.FINER, "addJar(" + jar + ")");

        // See IT 11417
        super.addURL(getURL(file));

        int i;

        if ((jarPath != null) && (jar.startsWith(jarPath))) {

            String jarName = jar.substring(jarPath.length());
            while (jarName.startsWith("/")) {
                jarName = jarName.substring(1);
            }
            jarNames.add(jarName);
        }

        try {

            // Register the JAR for tracking

            long lastModified =
                ((ResourceAttributes) resources.getAttributes(jar))
                .getLastModified();

            String[] result = new String[paths.length + 1];
            for (i = 0; i < paths.length; i++) {
                result[i] = paths[i];
            }
            result[paths.length] = jar;
            paths = result;

            long[] result3 = new long[lastModifiedDates.length + 1];
            for (i = 0; i < lastModifiedDates.length; i++) {
                result3[i] = lastModifiedDates[i];
            }
            result3[lastModifiedDates.length] = lastModified;
            lastModifiedDates = result3;

        } catch (NamingException e) {
            // Ignore
        }

        JarFile[] result2 = new JarFile[jarFiles.length + 1];
        for (i = 0; i < jarFiles.length; i++) {
            result2[i] = jarFiles[i];
        }
        result2[jarFiles.length] = jarFile;
        jarFiles = result2;

        // Add the file to the list
        File[] result4 = new File[jarRealFiles.length + 1];
        for (i = 0; i < jarRealFiles.length; i++) {
            result4[i] = jarRealFiles[i];
        }
        result4[jarRealFiles.length] = file;
        jarRealFiles = result4;
    }


    /**
     * Have one or more classes or resources been modified so that a reload
     * is appropriate?
     */
    public boolean modified() {

        if (logger.isLoggable(Level.FINER))
            logger.log(Level.FINER, "modified()");

        // Checking for modified loaded resources
        int length = paths.length;

        // A rare race condition can occur in the updates of the two arrays
        // It's totally ok if the latest class added is not checked (it will
        // be checked the next time
        int length2 = lastModifiedDates.length;
        if (length > length2)
            length = length2;

        for (int i = 0; i < length; i++) {
            try {
                long lastModified =
                    ((ResourceAttributes) resources.getAttributes(paths[i]))
                    .getLastModified();
                if (lastModified != lastModifiedDates[i]) {
                        if (logger.isLoggable(Level.FINER))
                            logger.log(Level.FINER, "  Resource '" + paths[i]
                                  + "' was modified; Date is now: "
                                  + new java.util.Date(lastModified) + " Was: "
                                  + new java.util.Date(lastModifiedDates[i]));
                    return (true);
                }
            } catch (NamingException e) {
                logger.log(Level.SEVERE, MISSING_RESOURCE, paths[i]);
                return (true);
            }
        }

        length = jarNames.size();

        // Check if JARs have been added or removed
        if (getJarPath() != null) {

            try {
                NamingEnumeration<Binding> enumeration =
                    resources.listBindings(getJarPath());
                int i = 0;
                while (enumeration.hasMoreElements() && (i < length)) {
                    NameClassPair ncPair = enumeration.nextElement();
                    String name = ncPair.getName();
                    // Ignore non JARs present in the lib folder
// START OF IASRI 4657979
                    if (!name.endsWith(".jar") && !name.endsWith(".zip"))
// END OF IASRI 4657979
                        continue;
                    if (!name.equals(jarNames.get(i))) {
                        // Missing JAR
                        logger.log(Level.FINER, "    Additional JARs have been added : '"
                                 + name + "'");
                        return (true);
                    }
                    i++;
                }
                if (enumeration.hasMoreElements()) {
                    while (enumeration.hasMoreElements()) {
                        NameClassPair ncPair = enumeration.nextElement();
                        String name = ncPair.getName();
                        // Additional non-JAR files are allowed
// START OF IASRI 4657979
                        if (name.endsWith(".jar") || name.endsWith(".zip")) {
// END OF IASRI 4657979
                            // There was more JARs
                            logger.log(Level.FINER, "    Additional JARs have been added");
                            return (true);
                        }
                    }
                } else if (i < jarNames.size()) {
                    // There was less JARs
                    logger.log(Level.FINER, "    Additional JARs have been added");
                    return (true);
                }
            } catch (NamingException e) {
                if (logger.isLoggable(Level.FINER))
                    logger.log(Level.FINER, "    Failed tracking modifications of '"
                        + getJarPath() + "'");
            } catch (ClassCastException e) {
                logger.log(Level.SEVERE, FAILED_TRACKING_MODIFICATIONS, new Object[]{getJarPath(), e.getMessage()});
            }

        }

        // No classes have been modified
        return (false);

    }


    /**
     * Render a String representation of this object.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("WebappClassLoader (delegate=");
        sb.append(delegate);
        if (repositories != null) {
            sb.append("; repositories=");
            for (int i = 0; i < repositories.length; i++) {
                sb.append(repositories[i]);
                if (i != (repositories.length-1)) {
                    sb.append(",");
                }
            }
        }
        sb.append(")");
        return (sb.toString());
    }


    // ---------------------------------------------------- ClassLoader Methods


    /**
     * Find the specified class in our local repositories, if possible.  If
     * not found, throw <code>ClassNotFoundException</code>.
     *
     * @param name Name of the class to be loaded
     *
     * @exception ClassNotFoundException if the class was not found
     */
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {

        if (logger.isLoggable(Level.FINER))
            logger.log(Level.FINER, "    findClass(" + name + ")");

        // (1) Permission to define this class when using a SecurityManager
        // START PE 4989455
        //if (securityManager != null) {
        if ( securityManager != null && packageDefinitionEnabled ){
        // END PE 4989455
            int i = name.lastIndexOf('.');
            if (i >= 0) {
                try {
                    if (logger.isLoggable(Level.FINER))
                        logger.log(Level.FINER, "      securityManager.checkPackageDefinition");
                    securityManager.checkPackageDefinition(name.substring(0,i));
                } catch (Exception se) {
                if (logger.isLoggable(Level.FINER))
                    logger.log(Level.FINER, "      -->Exception-->ClassNotFoundException", se);
                    throw new ClassNotFoundException(name, se);
                }
            }
        }

        // Ask our superclass to locate this class, if possible
        // (throws ClassNotFoundException if it is not found)
        Class<?> clazz = null;
        try {
            if (logger.isLoggable(Level.FINER))
                logger.log(Level.FINER, "      findClassInternal(" + name + ")");
            try {
                ResourceEntry entry = findClassInternal(name);
                // Create the code source object
                CodeSource codeSource =
                    new CodeSource(entry.codeBase, entry.certificates);
                synchronized (this) {
                    if (entry.loadedClass == null) {
                        /* START GlassFish [680]
                        clazz = defineClass(name, entry.binaryContent, 0,
                                entry.binaryContent.length,
                                codeSource);
                        */
                        // START GlassFish [680]
                        // We use a temporary byte[] so that we don't change
                        // the content of entry in case bytecode
                        // preprocessing takes place.
                        byte[] binaryContent = entry.binaryContent;
                        if (!byteCodePreprocessors.isEmpty()) {
                            // ByteCodePreprpcessor expects name as
                            // java/lang/Object.class
                            String resourceName =
                                name.replace('.', '/') + ".class";
                            for(BytecodePreprocessor preprocessor : byteCodePreprocessors) {
                                binaryContent = preprocessor.preprocess(
                                    resourceName, binaryContent);
                            }
                        }
                        clazz = defineClass(name, binaryContent, 0,
                                binaryContent.length,
                                codeSource);
                        // END GlassFish [680]
                        entry.loadedClass = clazz;
                        entry.binaryContent = null;
                        entry.source = null;
                        entry.codeBase = null;
                        entry.manifest = null;
                        entry.certificates = null;
                    } else {
                        clazz = entry.loadedClass;
                    }
                }
            } catch(ClassNotFoundException cnfe) {
                if (!hasExternalRepositories) {
                    throw cnfe;
                }
            } catch (UnsupportedClassVersionError ucve) {
                throw new UnsupportedClassVersionError(
                        getString(UNSUPPORTED_VERSION, name, getJavaVersion()));
            } catch(AccessControlException ace) {
                if (logger.isLoggable(Level.WARNING)) {
                    logger.log(Level.WARNING, FIND_CLASS_INTERNAL_SECURITY_EXCEPTION, new Object[]{ace.getMessage(), ace});
                }
                throw new ClassNotFoundException(name, ace);
            } catch(RuntimeException rex) {
                throw rex;
            } catch(Error err) {
                throw err;
            } catch (Throwable t) {
                throw new RuntimeException(
                        getString(UNABLE_TO_LOAD_CLASS, name, t.toString()), t);
            }
            if ((clazz == null) && hasExternalRepositories) {
                try {
                    clazz = super.findClass(name);
                } catch(AccessControlException ace) {
                    if (logger.isLoggable(Level.WARNING)) {
                        String msg = MessageFormat.format(
                                FIND_CLASS_INTERNAL_SECURITY_EXCEPTION,
                                new Object[]{name, ace.getMessage()});
                        logger.log(Level.WARNING, msg, ace);
                    }
                    throw new ClassNotFoundException(name, ace);
                } catch (RuntimeException e) {
                    if (logger.isLoggable(Level.FINER))
                        logger.log(Level.FINER, "      -->RuntimeException Rethrown", e);
                    throw e;
                }
            }
            if (clazz == null) {
                if (logger.isLoggable(Level.FINER))
                    logger.log(Level.FINER, "    --> Returning ClassNotFoundException");
                throw new ClassNotFoundException(name);
            }
        } catch (ClassNotFoundException e) {
            if (logger.isLoggable(Level.FINER))
                logger.log(Level.FINER, "    --> Passing on ClassNotFoundException");
            throw e;
        }

        // Return the class we have located
        if (logger.isLoggable(Level.FINER))
            logger.log(Level.FINER, "      Returning class " + clazz);
        if (logger.isLoggable(Level.FINER)) {
            ClassLoader cl;
            if (securityManager != null) {
                cl = AccessController.doPrivileged(
                    new PrivilegedGetClassLoader(clazz));
            } else {
                cl = clazz.getClassLoader();
            }
            logger.log(Level.FINER, "      Loaded by " + cl);
        }
        return (clazz);

    }


    /**
     * Find the specified resource in our local repository, and return a
     * <code>URL</code> referring to it, or <code>null</code> if this resource
     * cannot be found.
     *
     * @param name Name of the resource to be found
     */
    @Override
    public URL findResource(String name) {
        return findResource(name, false);
    }

    private URL findResource(String name, boolean fromJarsOnly) {

        if (logger.isLoggable(Level.FINER))
            logger.log(Level.FINER, "    findResource(" + name + ")");

        URL url = null;

        if (".".equals(name)) {
            name = "";
        }

        ResourceEntry entry = resourceEntries.get(name);
        if (entry == null) {
            entry = findResourceInternal(name, name, fromJarsOnly);
        }
        if (entry != null) {
            url = entry.source;
        }

        if ((url == null) && hasExternalRepositories)
            url = super.findResource(name);

        if (logger.isLoggable(Level.FINER)) {
            if (url != null)
                logger.log(Level.FINER, "    --> Returning '" + url.toString() + "'");
            else
                logger.log(Level.FINER, "    --> Resource not found, returning null");
        }
        return (url);

    }


    /**
     * Return an enumeration of <code>URLs</code> representing all of the
     * resources with the given name.  If no resources with this name are
     * found, return an empty enumeration.
     *
     * @param name Name of the resources to be found
     *
     * @exception IOException if an input/output error occurs
     */
    @Override
    public Enumeration<URL> findResources(String name) throws IOException {

        if (logger.isLoggable(Level.FINER))
            logger.log(Level.FINER, "    findResources(" + name + ")");

        Vector<URL> result = new Vector<URL>();

        if (repositories != null) {
            int repositoriesLength = repositories.length;

            int i;

            // Looking at the repositories
            for (i = 0; i < repositoriesLength; i++) {
                try {
                    String fullPath = repositories[i] + name;
                    resources.lookup(fullPath);
                    // Note : Not getting an exception here means the resource was
                    // found
                    try {
                        result.addElement(getURI(new File(files[i], name)));
                    } catch (MalformedURLException e) {
                        // Ignore
                    }
                } catch (NamingException e) {
                }
            }
        }

        Enumeration<URL> otherResourcePaths = super.findResources(name);

        while (otherResourcePaths.hasMoreElements()) {
            result.addElement(otherResourcePaths.nextElement());
        }

        return result.elements();

    }

    /**
     * From the resource with the given name.  This is the same as findResouce
     * except that the resources from the local files are excluded.  This is
     * primarily used form locating resources in /META-INF/resources/ in jars.
     */
    public URL getResourceFromJars(String name) {
        return getResource(name, true);
    }

    /**
     * Find the resource with the given name.  A resource is some data
     * (images, audio, text, etc.) that can be accessed by class code in a
     * way that is independent of the location of the code.  The name of a
     * resource is a "/"-separated path name that identifies the resource.
     * If the resource cannot be found, return <code>null</code>.
     * <p>
     * This method searches according to the following algorithm, returning
     * as soon as it finds the appropriate URL.  If the resource cannot be
     * found, returns <code>null</code>.
     * <ul>
     * <li>If the <code>delegate</code> property is set to <code>true</code>,
     *     call the <code>getResource()</code> method of the parent class
     *     loader, if any.</li>
     * <li>Call <code>findResource()</code> to find this resource in our
     *     locally defined repositories.</li>
     * <li>Call the <code>getResource()</code> method of the parent class
     *     loader, if any.</li>
     * </ul>
     *
     * @param name Name of the resource to return a URL for
     */
    @Override
    public URL getResource(String name) {
        return getResource(name, false);
    }

    private URL getResource(String name, boolean fromJarsOnly) {
        if (logger.isLoggable(Level.FINER))
            logger.log(Level.FINER, "getResource(" + name + ")");
        URL url = null;

        /*
         * (1) Delegate to parent if requested, or if the requested resource
         * belongs to one of the packages that are part of the Java EE platform
         */
        if (isResourceDelegate(name)) {
            if (logger.isLoggable(Level.FINER))
                logger.log(Level.FINER, "  Delegating to parent classloader " + parent);
            ClassLoader loader = parent;
            if (loader == null)
                loader = system;
            url = loader.getResource(name);
            if (url != null) {
            if (logger.isLoggable(Level.FINER))
                logger.log(Level.FINER, "  --> Returning '" + url.toString() + "'");
                return (url);
            }
        }

        // (2) Search local repositories
        url = findResource(name, fromJarsOnly);
        if (url != null) {
            // Locating the repository for special handling in the case
            // of a JAR
            ResourceEntry entry = resourceEntries.get(name);
            try {
                String repository = entry.codeBase.toString();
                if ((repository.endsWith(".jar"))
                        && !(name.endsWith(".class"))
                        && !(name.endsWith(".jar"))) {
                    // Copy binary content to the work directory if not present
                    File resourceFile = new File(loaderDir, name);
                    url = resourceFile.toURI().toURL();
                }
            } catch (Exception e) {
                // Ignore
            }
            if (logger.isLoggable(Level.FINER))
                logger.log(Level.FINER, "  --> Returning '" + url.toString() + "'");
            return (url);
        }

        // (3) Delegate to parent unconditionally if not already attempted
        if (!delegate) {
            ClassLoader loader = parent;
            if (loader == null)
                loader = system;
            url = loader.getResource(name);
            if (url != null) {
                if (logger.isLoggable(Level.FINER))
                    logger.log(Level.FINER, "  --> Returning '" + url.toString() + "'");
                return (url);
            }
        }

        // (4) Resource was not found
        if (logger.isLoggable(Level.FINER))
            logger.log(Level.FINER, "  --> Resource not found, returning null");
        return (null);

    }


    /**
     * Find the resource with the given name, and return an input stream
     * that can be used for reading it.  The search order is as described
     * for <code>getResource()</code>, after checking to see if the resource
     * data has been previously cached.  If the resource cannot be found,
     * return <code>null</code>.
     *
     * @param name Name of the resource to return an input stream for
     */
    @Override
    public InputStream getResourceAsStream(String name) {

        if (logger.isLoggable(Level.FINER))
            logger.log(Level.FINER, "getResourceAsStream(" + name + ")");
        InputStream stream = null;

        // (0) Check for a cached copy of this resource
        stream = findLoadedResource(name);
        if (stream != null) {
            if (logger.isLoggable(Level.FINER))
                logger.log(Level.FINER, "  --> Returning stream from cache");
            return (stream);
        }

        /*
         * (1) Delegate to parent if requested, or if the requested resource
         * belongs to one of the packages that are part of the Java EE platform
         */
        if (isResourceDelegate(name)) {
            if (logger.isLoggable(Level.FINER))
                logger.log(Level.FINER, "  Delegating to parent classloader " + parent);
            ClassLoader loader = parent;
            if (loader == null)
                loader = system;
            stream = loader.getResourceAsStream(name);
            if (stream != null) {
                // FIXME - cache???
                if (logger.isLoggable(Level.FINER))
                    logger.log(Level.FINER, "  --> Returning stream from parent");
                return (stream);
            }
        }

        // (2) Search local repositories
        if (logger.isLoggable(Level.FINER))
            logger.log(Level.FINER, "  Searching local repositories");
        URL url = findResource(name);
        if (url != null) {
            // FIXME - cache???
            if (logger.isLoggable(Level.FINER))
                logger.log(Level.FINER, "  --> Returning stream from local");
            stream = findLoadedResource(name);
            try {
                if (hasExternalRepositories && (stream == null))
                    stream = url.openStream();
            } catch (IOException e) {
                ; // Ignore
            }
            if (stream != null)
                return (stream);
        }

        // (3) Delegate to parent unconditionally
        if (!delegate) {
            if (logger.isLoggable(Level.FINER))
                logger.log(Level.FINER, "  Delegating to parent classloader unconditionally " + parent);
            ClassLoader loader = parent;
            if (loader == null)
                loader = system;
            stream = loader.getResourceAsStream(name);
            if (stream != null) {
                // FIXME - cache???
                if (logger.isLoggable(Level.FINER))
                    logger.log(Level.FINER, "  --> Returning stream from parent");
                return (stream);
            }
        }

        // (4) Resource was not found
        if (logger.isLoggable(Level.FINER))
            logger.log(Level.FINER, "  --> Resource not found, returning null");
        return (null);

    }


    /**
     * Finds all the resources with the given name.
     */
    @Override
    public Enumeration<URL> getResources(String name) throws IOException {

	final Enumeration[] enums = new Enumeration[2];

        Enumeration<URL> localResources = findResources(name);
        Enumeration<URL> parentResources = null;
        if (parent != null) {
            parentResources = parent.getResources(name);
        } else {
            parentResources = system.getResources(name);
        }

        if (delegate) {
            enums[0] = parentResources;
            enums[1] = localResources;
        } else {
            enums[0] = localResources;
            enums[1] = parentResources;
        }

        return new Enumeration<URL>() {

            int index = 0;

            private boolean next() {
                while (index < enums.length) {
                    if (enums[index] != null &&
                            enums[index].hasMoreElements()) {
                        return true;
                    }
                    index++;
                }
                return false;
            }

            public boolean hasMoreElements() {
                return next();
            }

            public URL nextElement() {
                if (!next()) {
                    throw new NoSuchElementException();
                }
                return (URL)enums[index].nextElement();
            }
        };
    }


    /**
     * Load the class with the specified name.  This method searches for
     * classes in the same manner as <code>loadClass(String, boolean)</code>
     * with <code>false</code> as the second argument.
     *
     * @param name Name of the class to be loaded
     *
     * @exception ClassNotFoundException if the class was not found
     */
    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {

        return (loadClass(name, false));
    }


    /**
     * Load the class with the specified name, searching using the following
     * algorithm until it finds and returns the class.  If the class cannot
     * be found, returns <code>ClassNotFoundException</code>.
     * <ul>
     * <li>Call <code>findLoadedClass(String)</code> to check if the
     *     class has already been loaded.  If it has, the same
     *     <code>Class</code> object is returned.</li>
     * <li>If the <code>delegate</code> property is set to <code>true</code>,
     *     call the <code>loadClass()</code> method of the parent class
     *     loader, if any.</li>
     * <li>Call <code>findClass()</code> to find this class in our locally
     *     defined repositories.</li>
     * <li>Call the <code>loadClass()</code> method of our parent
     *     class loader, if any.</li>
     * </ul>
     * If the class was found using the above steps, and the
     * <code>resolve</code> flag is <code>true</code>, this method will then
     * call <code>resolveClass(Class)</code> on the resulting Class object.
     *
     * @param name Name of the class to be loaded
     * @param resolve If <code>true</code> then resolve the class
     *
     * @exception ClassNotFoundException if the class was not found
     */
    @Override
    protected Class<?> loadClass(String name, boolean resolve)
        throws ClassNotFoundException {

        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "loadClass(" + name + ")");
        }

        Class<?> clazz = null;

        // Don't load classes if class loader is stopped
        if (!started) {
            throw new IllegalStateException(
                getString(NOT_STARTED, name));
        }

        // (0) Check our previously loaded local class cache
        clazz = findLoadedClass0(name);
        if (clazz != null) {
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "  Returning class from cache");
            }
            if (resolve)
                resolveClass(clazz);
            return (clazz);
        }

        // (0.1) Check our previously loaded class cache
        clazz = findLoadedClass(name);
        if (clazz != null) {
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "  Returning class from cache");
            }
            if (resolve)
                resolveClass(clazz);
            return (clazz);
        }

        // (0.5) Permission to access this class when using a SecurityManager
        if ( securityManager != null && packageDefinitionEnabled){
            int i = name.lastIndexOf('.');
            if (i >= 0) {
                try {
                    securityManager.checkPackageAccess(name.substring(0,i));
                } catch (SecurityException se) {
                    String error = MessageFormat.format(SECURITY_EXCEPTION, name);
                    if (logger.isLoggable(Level.INFO)) {
                        logger.log(Level.INFO, error, se);
                    }
                    throw new ClassNotFoundException(error, se);
                }
            }
        }

        ClassLoader delegateLoader = parent;
        if (delegateLoader == null) {
            delegateLoader = system;
        }

        boolean delegateLoad = delegate || filter(name);

        // (1) Delegate to our parent if requested
        if (delegateLoad) {
            // Check delegate first
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "  Delegating to classloader1 " + delegateLoader);
            }
            try {
                clazz = delegateLoader.loadClass(name);
                if (clazz != null) {
                    if (logger.isLoggable(Level.FINER)) {
                        logger.log(Level.FINER, "  Loading class from delegate");
                    }
                    if (resolve)
                        resolveClass(clazz);
                    return clazz;
                }
            } catch (ClassNotFoundException e) {
                // Ignore
            }
        }


        // (2) Search local repositories
        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "  Searching local repositories");
        }
        try {
            clazz = findClass(name);
            if (clazz != null) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, "  Loading class from local repository");
                }
                if (resolve)
                    resolveClass(clazz);
                return clazz;
            }
        } catch (ClassNotFoundException e) {
            // Ignore
        }

        // (3) Delegate if class was not found locally
        if (!delegateLoad) {
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "  Delegating to classloader " + delegateLoader);
            }
            try {
                clazz = delegateLoader.loadClass(name);
                if (clazz != null) {
                    if (logger.isLoggable(Level.FINER)) {
                        logger.log(Level.FINER, "  Loading class from delegate");
                    }
                    if (resolve)
                        resolveClass(clazz);
                    return clazz;
                }
            } catch (ClassNotFoundException e) {
                // Ignore
            }
        }

        throw new ClassNotFoundException(name);
    }


    /**
     * Get the Permissions for a CodeSource.  If this instance
     * of WebappClassLoader is for a web application context,
     * add read FilePermission or JndiPermissions for the base
     * directory (if unpacked),
     * the context URL, and jar file resources.
     *
     * @param codeSource where the code was loaded from
     * @return PermissionCollection for CodeSource
     */
    @Override
    protected PermissionCollection getPermissions(CodeSource codeSource) {

        String codeUrl = codeSource.getLocation().toString();
        PermissionCollection pc = loaderPC.get(codeUrl);
        if (pc == null) {
            pc = new Permissions();            

            PermissionCollection spc = super.getPermissions(codeSource);
            if (spc != null) {
                Enumeration<Permission> perms = spc.elements();
                while (perms.hasMoreElements()) {
                    Permission p = perms.nextElement();
                    pc.add(p);
                }                 
            }
                
            Iterator<Permission> perms = permissionList.iterator();
            while (perms.hasNext()) {
                Permission p = perms.next();
                pc.add(p);
            }
            
            //get the declared and EE perms
            PermissionCollection pc1 = 
                permissionsHolder.getPermissions(codeSource, null);
            if  (pc1 != null) {
                Enumeration<Permission> dperms =  pc1.elements();
                while (dperms.hasMoreElements()) {
                    Permission p = dperms.nextElement();
                    pc.add(p);
                }
            }
                
            PermissionCollection tmpPc = loaderPC.putIfAbsent(codeUrl,pc);                
            if (tmpPc != null) {
                pc = tmpPc;
            }
        }
        return (pc);

    }


    /**
     * Returns the search path of URLs for loading classes and resources.
     * This includes the original list of URLs specified to the constructor,
     * along with any URLs subsequently appended by the addURL() method.
     * @return the search path of URLs for loading classes and resources.
     */
    @Override
    public synchronized URL[] getURLs() {

        if (repositoryURLs != null) {
            return repositoryURLs;
        }

        URL[] external = super.getURLs();

        int filesLength = files.length;
        int jarFilesLength = jarRealFiles.length;
        int length = filesLength + jarFilesLength + external.length;
        int i;

        try {

            ArrayList<URL> urls = new ArrayList<URL>();
            for (i = 0; i < length; i++) {
                if (i < filesLength) {
                    urls.add(i, getURL(files[i]));
                } else if (i < filesLength + jarFilesLength) {
                    urls.add(i, getURL(jarRealFiles[i - filesLength]));
                } else {
                    urls.add(i, external[i - filesLength - jarFilesLength]);
                }
            }

            repositoryURLs = removeDuplicate(urls);

        } catch (MalformedURLException e) {
            repositoryURLs = new URL[0];
        }

        return repositoryURLs;

    }

    @SuppressWarnings("unchecked")
    private URL[] removeDuplicate(ArrayList<URL> urls) {
        HashSet h = new HashSet(urls);
        urls.clear();
        urls.addAll(h);
        return urls.toArray(new URL[urls.size()]);
    }

    // ------------------------------------------------------ Lifecycle Methods


    private void init() {

        this.parent = getParent();

        /* SJSAS 6317864
        system = getSystemClassLoader();
        */
        // START SJSAS 6317864
        system = this.getClass().getClassLoader();
        // END SJSAS 6317864
        securityManager = System.getSecurityManager();

        if (securityManager != null) {
            refreshPolicy();
        }

        addOverridablePackage("com.sun.faces.extensions");
        
        permissionsHolder = new PermsHolder();
    }


    /**
     * Start the class loader.
     */
    public void start() {
        started = true;
    }

    public boolean isStarted() {
        return started;
    }

    public void preDestroy() {
        try {
            stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Stop the class loader.
     *
     * @exception LifecycleException if a lifecycle error occurs
     */
    public void stop() throws Exception {

        if (!started) {
            return;
        }

        // START GlassFish Issue 587
        purgeELBeanClasses();
        // END GlassFish Issue 587

        /*
         * Clearing references should be done before setting started to
         * false, due to possible side effects.
         * In addition, set this classloader as the Thread's context
         * classloader, see IT 9894 for details
         */
        ClassLoader curCl = null;
        try {
            curCl = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(this);
            clearReferences();
        } finally {
            if (curCl != null) {
                Thread.currentThread().setContextClassLoader(curCl);
            }
        }

        // START SJSAS 6258619
        ClassLoaderUtil.releaseLoader(this);
        // END SJSAS 6258619

        started = false;

        int length = files.length;
        for (int i = 0; i < length; i++) {
            files[i] = null;
        }

        length = jarFiles.length;
        for (int i = 0; i < length; i++) {
            try {
                if (jarFiles[i] != null) {
                    jarFiles[i].close();
                }
            } catch (IOException e) {
                // Ignore
            }
            jarFiles[i] = null;
        }

        try {
            Class<?> clazz = Class.forName("sun.misc.ClassLoaderUtil");
            if (clazz != null) {
                Method m = clazz.getMethod("releaseLoader",
                                           URLClassLoader.class);
                if (m != null) {
                    m.invoke(null, this);
                }
            }
        } catch (Exception e) {
            // ignore
        }

        notFoundResources.clear();
        resourceEntries.clear();
        resources = null;
        repositories = null;
        repositoryURLs = null;
        files = null;
        jarFiles = null;
        jarRealFiles = null;
        jarPath = null;
        jarNames.clear();
        lastModifiedDates = null;
        paths = null;
        hasExternalRepositories = false;
        parent = null;

        permissionList.clear();
        permissionsHolder = null;
        loaderPC.clear();

        if (loaderDir != null) {
            deleteDir(loaderDir);
        }

        DirContextURLStreamHandler.unbind(this);
    }


    /**
     * Used to periodically signal to the classloader to release
     * JAR resources.
     */
    public void closeJARs(boolean force) {
        if (jarFiles.length > 0) {
            synchronized (jarFiles) {
                if (force || (System.currentTimeMillis()
                              > (lastJarAccessed + 90000))) {
                    for (int i = 0; i < jarFiles.length; i++) {
                        try {
                            if (jarFiles[i] != null) {
                                jarFiles[i].close();
                                jarFiles[i] = null;
                            }
                        } catch (IOException e) {
                            if (logger.isLoggable(Level.FINE)) {
                                logger.log(Level.FINE, "Failed to close JAR", e);
                            }
                        }
                    }
                }
            }
        }
    }


    /**
     * Clear references.
     */
    protected void clearReferences() {

        // De-register any remaining JDBC drivers
        clearReferencesJdbc();

        // Check for leaks triggered by ThreadLocals loaded by this class loader
        checkThreadLocalsForLeaks();

        // Clear RMI Targets loaded by this class loader
        clearReferencesRmiTargets();

        // Null out any static or final fields from loaded classes,
        // as a workaround for apparent garbage collection bugs
        if (clearReferencesStatic) {
            clearReferencesStaticFinal();
        }

        // Clear the IntrospectionUtils cache.
        IntrospectionUtils.clear();

        // Clear the resource bundle cache
        // This shouldn't be necessary, the cache uses weak references but
        // it has caused leaks. Oddly, using the leak detection code in
        // standard host allows the class loader to be GC'd. This has been seen
        // on Sun but not IBM JREs. Maybe a bug in Sun's GC impl?
        clearReferencesResourceBundles();

        // Clear the classloader reference in the VM's bean introspector
        java.beans.Introspector.flushCaches();
    }

    /**
     * Deregister any JDBC drivers registered by the webapp that the webapp
     * forgot. This is made unnecessary complex because a) DriverManager
     * checks the class loader of the calling class (it would be much easier
     * if it checked the context class loader) b) using reflection would
     * create a dependency on the DriverManager implementation which can,
     * and has, changed.
     *
     * We can't just create an instance of JdbcLeakPrevention as it will be
     * loaded by the common class loader (since it's .class file is in the
     * $CATALINA_HOME/lib directory). This would fail DriverManager's check
     * on the class loader of the calling class. So, we load the bytes via
     * our parent class loader but define the class with this class loader
     * so the JdbcLeakPrevention looks like a webapp class to the
     * DriverManager.
     *
     * If only apps cleaned up after themselves...
     */
    private final void clearReferencesJdbc() {
        InputStream is = getResourceAsStream(
                "org/glassfish/web/loader/JdbcLeakPrevention.class");
        // We know roughly how big the class will be (~ 1K) so allow 2k as a
        // starting point
        byte[] classBytes = new byte[2048];
        int offset = 0;
        try {
            int read = is.read(classBytes, offset, classBytes.length-offset);
            while (read > -1) {
                offset += read;
                if (offset == classBytes.length) {
                    // Buffer full - double size
                    byte[] tmp = new byte[classBytes.length * 2];
                    System.arraycopy(classBytes, 0, tmp, 0, classBytes.length);
                    classBytes = tmp;
                }
                read = is.read(classBytes, offset, classBytes.length-offset);
            }
            Class<?> lpClass =
                defineClass("org.glassfish.web.loader.JdbcLeakPrevention",
                    classBytes, 0, offset, this.getClass().getProtectionDomain());
            Object obj = lpClass.newInstance();
            @SuppressWarnings("unchecked") // clearJdbcDriverRegistrations() returns List<String>
            List<String> driverNames = (List<String>) obj.getClass().getMethod(
                    "clearJdbcDriverRegistrations").invoke(obj);
            String msg = rb.getString(CLEAR_JDBC);
            for (String name : driverNames) {
                logger.warning(MessageFormat.format(msg, contextName, name));
            }
        } catch (Exception e) {
            // So many things to go wrong above...
            Throwable t = ExceptionUtils.unwrapInvocationTargetException(e);
            ExceptionUtils.handleThrowable(t);
            logger.log(Level.WARNING,
                    getString(JDBC_REMOVE_FAILED, contextName), t);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ioe) {
                    logger.log(Level.WARNING,
                            getString(JDBC_REMOVE_STREAM_ERROR, contextName), ioe);
                }
            }
        }
    }


    private final void clearReferencesStaticFinal() {

        Collection<ResourceEntry> values = resourceEntries.values();
        Iterator<ResourceEntry> loadedClasses = values.iterator();
        /*
         * Step 1: Enumerate all classes loaded by this WebappClassLoader
         * and trigger the initialization of any uninitialized ones.
         * This is to prevent the scenario where the initialization of
         * one class would call a previously cleared class in Step 2 below.
         */
        while(loadedClasses.hasNext()) {
            ResourceEntry entry = loadedClasses.next();
            Class<?> clazz = null;
            synchronized(this) {
                clazz = entry.loadedClass;
            }
            if (clazz != null) {
                try {
                    Field[] fields = clazz.getDeclaredFields();
                    for (int i = 0; i < fields.length; i++) {
                        if(Modifier.isStatic(fields[i].getModifiers())) {
                            fields[i].get(null);
                            break;
                        }
                    }
                } catch(Throwable t) {
                    // Ignore
                }
            }
        }

        /**
         * Step 2: Clear all loaded classes
         */
        loadedClasses = values.iterator();
        while (loadedClasses.hasNext()) {
            ResourceEntry entry = loadedClasses.next();
            Class<?> clazz = null;
            synchronized(this) {
                clazz = entry.loadedClass;
            }
            if (clazz != null) {
                try {
                    Field[] fields = clazz.getDeclaredFields();
                    for (int i = 0; i < fields.length; i++) {
                        Field field = fields[i];
                        int mods = field.getModifiers();
                        if (field.getType().isPrimitive()
                                || (field.getName().indexOf("$") != -1)) {
                            continue;
                        }
                        if (Modifier.isStatic(mods)) {
                            try {
                                setAccessible(field);
                                if (Modifier.isFinal(mods)) {
                                    if (!((field.getType().getName().startsWith("java."))
                                            || (field.getType().getName().startsWith("javax.")))) {
                                        nullInstance(field.get(null));
                                    }
                                } else {
                                    field.set(null, null);
                                    if (logger.isLoggable(Level.FINE)) {
                                        logger.log(Level.FINE, "Set field " + field.getName()
                                                + " to null in class " + clazz.getName());
                                    }
                                }
                            } catch (Throwable t) {
                                ExceptionUtils.handleThrowable(t);
                                if (logger.isLoggable(Level.FINE)) {
                                    logger.log(Level.FINE, "Could not set field " + field.getName()
                                            + " to null in class " + clazz.getName(), t);
                                }
                            }
                        }
                    }
                } catch (Throwable t) {
                    ExceptionUtils.handleThrowable(t);
                    if (logger.isLoggable(Level.FINE))  {
                        logger.log(Level.FINE, "Could not clean fields for class " + clazz.getName(), t);
                    }
                }
            }
        }
    }


    protected void nullInstance(Object instance) {
        if (instance == null) {
            return;
        }
        Field[] fields = instance.getClass().getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            int mods = field.getModifiers();
            if (field.getType().isPrimitive()
                    || (field.getName().indexOf("$") != -1)) {
                continue;
            }
            try {
                setAccessible(field);
                if (Modifier.isStatic(mods) && Modifier.isFinal(mods)) {
                    // Doing something recursively is too risky
                    continue;
                } else {
                    Object value = field.get(instance);
                    if (null != value) {
                        Class<? extends Object> valueClass = value.getClass();
                        if (!loadedByThisOrChild(valueClass)) {
                            if (logger.isLoggable(Level.FINE))  {
                                logger.log(Level.FINE, "Not setting field " + field.getName() +
                                        " to null in object of class " +
                                        instance.getClass().getName() +
                                        " because the referenced object was of type " +
                                        valueClass.getName() +
                                        " which was not loaded by this WebappClassLoader.");
                            }
                        } else {
                            field.set(instance, null);
                            if (logger.isLoggable(Level.FINE))
                                logger.log(Level.FINE, "Set field " + field.getName()
                                        + " to null in class " + instance.getClass().getName());
                        }
                    }
                }
            } catch (Throwable t) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Could not set field " + field.getName()
                            + " to null in object instance of class "
                            + instance.getClass().getName(), t);
                }
            }
        }
    }


    private void checkThreadLocalsForLeaks() {
        Thread[] threads = getThreads();

        try {
            // Make the fields in the Thread class that store ThreadLocals
            // accessible
            Field threadLocalsField =
                Thread.class.getDeclaredField("threadLocals");
            threadLocalsField.setAccessible(true);
            Field inheritableThreadLocalsField =
                Thread.class.getDeclaredField("inheritableThreadLocals");
            inheritableThreadLocalsField.setAccessible(true);
            // Make the underlying array of ThreadLoad.ThreadLocalMap.Entry objects
            // accessible
            Class<?> tlmClass = Class.forName("java.lang.ThreadLocal$ThreadLocalMap");
            Field tableField = tlmClass.getDeclaredField("table");
            tableField.setAccessible(true);
            Method expungeStaleEntriesMethod = tlmClass.getDeclaredMethod("expungeStaleEntries");
            expungeStaleEntriesMethod.setAccessible(true);

            for (int i = 0; i < threads.length; i++) {
                Object threadLocalMap;
                if (threads[i] != null) {

                    // Clear the first map
                    threadLocalMap = threadLocalsField.get(threads[i]);
                    if (null != threadLocalMap){
                        expungeStaleEntriesMethod.invoke(threadLocalMap);
                        checkThreadLocalMapForLeaks(threadLocalMap, tableField);
                    }

                    // Clear the second map
                    threadLocalMap =inheritableThreadLocalsField.get(threads[i]);
                    if (null != threadLocalMap){
                        expungeStaleEntriesMethod.invoke(threadLocalMap);
                        checkThreadLocalMapForLeaks(threadLocalMap, tableField);
                    }
                }
            }
        } catch (SecurityException e) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING,
                        getString(CHECK_THREAD_LOCALS_FOR_LEAKS_FAIL,
                        contextName), e);
            }
        } catch (NoSuchFieldException e) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING,
                        getString(CHECK_THREAD_LOCALS_FOR_LEAKS_FAIL,
                        contextName), e);
            }
        } catch (ClassNotFoundException e) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING,
                        getString(CHECK_THREAD_LOCALS_FOR_LEAKS_FAIL,
                        contextName), e);
            }
        } catch (IllegalArgumentException e) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING,
                        getString(CHECK_THREAD_LOCALS_FOR_LEAKS_FAIL,
                        contextName), e);
            }
        } catch (IllegalAccessException e) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING,
                        getString(CHECK_THREAD_LOCALS_FOR_LEAKS_FAIL,
                        contextName), e);
            }
        } catch (InvocationTargetException e) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING,
                        getString(CHECK_THREAD_LOCALS_FOR_LEAKS_FAIL,
                        contextName), e);
            }
        } catch (NoSuchMethodException e) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING,
                        getString(CHECK_THREAD_LOCALS_FOR_LEAKS_FAIL,
                        contextName), e);
            }
        }
    }


    /**
     * Analyzes the given thread local map object. Also pass in the field that
     * points to the internal table to save re-calculating it on every
     * call to this method.
     */
    private void checkThreadLocalMapForLeaks(Object map,
            Field internalTableField) throws IllegalAccessException,
            NoSuchFieldException {
        if (map != null) {
            Object[] table = (Object[]) internalTableField.get(map);
            if (table != null) {
                for (int j =0; j < table.length; j++) {
                    if (table[j] != null) {
                        boolean potentialLeak = false;
                        // Check the key
                        Object key = ((Reference<?>) table[j]).get();
                        if (this.equals(key) || loadedByThisOrChild(key)) {
                            potentialLeak = true;
                        }
                        // Check the value
                        Field valueField =
                            table[j].getClass().getDeclaredField("value");
                        valueField.setAccessible(true);
                        Object value = valueField.get(table[j]);
                        if (this.equals(value) || loadedByThisOrChild(value)) {
                            potentialLeak = true;
                        }
                        if (potentialLeak) {
                            Object[] args = new Object[5];
                            args[0] = contextName;
                            if (key != null) {
                                args[1] = getPrettyClassName(key.getClass());
                                try {
                                    args[2] = key.toString();
                                } catch (Exception e) {
                                    logger.log(Level.SEVERE, getString(
                                            CHECK_THREAD_LOCALS_FOR_LEAKS_BAD_KEY,
                                            args[1]), e);
                                    args[2] = getString(
                                            CHECK_THREAD_LOCALS_FOR_LEAKS_UNKNOWN);
                                }
                            }
                            if (value != null) {
                                args[3] = getPrettyClassName(value.getClass());
                                try {
                                    args[4] = value.toString();
                                } catch (Exception e) {
                                    logger.log(Level.SEVERE, getString(
                                            CHECK_THREAD_LOCALS_FOR_LEAKS_BAD_VALUE,
                                            args[3]), e);
                                    args[4] = getString(
                                            CHECK_THREAD_LOCALS_FOR_LEAKS_UNKNOWN);
                                }
                            }
                            if (value == null) {
                                if (logger.isLoggable(Level.FINE)) {
                                    logger.log(Level.FINE, getString(
                                            CHECK_THREAD_LOCALS_FOR_LEAKS_DEBUG,
                                            args));
                                }
                            } else {
                                logger.log(Level.SEVERE, getString(
                                        CHECK_THREAD_LOCALS_FOR_LEAKS,
                                        args));
                            }
                        }
                    }
                }
            }
        }
    }

    private String getPrettyClassName(Class<?> clazz) {
        String name = clazz.getCanonicalName();
        if (name==null){
            name = clazz.getName();
        }
        return name;
    }


    /**
     * @param o object to test, may be null
     * @return <code>true</code> if o has been loaded by the current classloader
     * or one of its descendants.
     */
    private boolean loadedByThisOrChild(Object o) {
        if (o == null) {
            return false;
        }

        Class<?> clazz;
        if (o instanceof Class) {
            clazz = (Class<?>) o;
        } else {
            clazz = o.getClass();
        }

        ClassLoader cl = clazz.getClassLoader();
        while (cl != null) {
            if (cl == this) {
                return true;
            }
            cl = cl.getParent();
        }

        if (o instanceof Collection<?>) {
            Iterator<?> iter = ((Collection<?>) o).iterator();
            while (iter.hasNext()) {
                Object entry = iter.next();
                if (loadedByThisOrChild(entry)) {
                    return true;
                }
            }
        }
        return false;
    }


    /*
     * Get the set of current threads as an array.
     */
    private Thread[] getThreads() {
        // Get the current thread group
        ThreadGroup tg = Thread.currentThread( ).getThreadGroup( );
        // Find the root thread group
        while (tg.getParent() != null) {
            tg = tg.getParent();
        }

        int threadCountGuess = tg.activeCount() + 50;
        Thread[] threads = new Thread[threadCountGuess];
        int threadCountActual = tg.enumerate(threads);
        // Make sure we don't miss any threads
        while (threadCountActual == threadCountGuess) {
            threadCountGuess *=2;
            threads = new Thread[threadCountGuess];
            // Note tg.enumerate(Thread[]) silently ignores any threads that
            // can't fit into the array
            threadCountActual = tg.enumerate(threads);
        }

        return threads;
    }


    /**
     * This depends on the internals of the Sun JVM so it does everything by
     * reflection.
     */
    private void clearReferencesRmiTargets() {
        try {
            // Need access to the ccl field of sun.rmi.transport.Target
            Class<?> objectTargetClass =
                Class.forName("sun.rmi.transport.Target");
            Field cclField = objectTargetClass.getDeclaredField("ccl");
            cclField.setAccessible(true);

            // Clear the objTable map
            Class<?> objectTableClass =
                Class.forName("sun.rmi.transport.ObjectTable");
            Field objTableField = objectTableClass.getDeclaredField("objTable");
            objTableField.setAccessible(true);
            Object objTable = objTableField.get(null);
            if (objTable == null) {
                return;
            }

            // Iterate over the values in the table
            if (objTable instanceof Map<?,?>) {
                Iterator<?> iter = ((Map<?,?>) objTable).values().iterator();
                while (iter.hasNext()) {
                    Object obj = iter.next();
                    Object cclObject = cclField.get(obj);
                    if (this == cclObject) {
                        iter.remove();
                    }
                }
            }

            // Clear the implTable map
            Field implTableField = objectTableClass.getDeclaredField("implTable");
            implTableField.setAccessible(true);
            Object implTable = implTableField.get(null);
            if (implTable == null) {
                return;
            }

            // Iterate over the values in the table
            if (implTable instanceof Map<?,?>) {
                Iterator<?> iter = ((Map<?,?>) implTable).values().iterator();
                while (iter.hasNext()) {
                    Object obj = iter.next();
                    Object cclObject = cclField.get(obj);
                    if (this == cclObject) {
                        iter.remove();
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO,
                        getString(CLEAR_RMI_INFO,
                        contextName), e);
            }
        } catch (SecurityException e) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING,
                        getString(CLEAR_RMI_FAIL,
                        contextName), e);
            }
        } catch (NoSuchFieldException e) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING,
                        getString(CLEAR_RMI_FAIL,
                        contextName), e);
            }
        } catch (IllegalArgumentException e) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING,
                        getString(CLEAR_RMI_FAIL,
                        contextName), e);
            }
        } catch (IllegalAccessException e) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING,
                        getString(CLEAR_RMI_FAIL,
                        contextName), e);
            }
        }
    }


    /**
     * Clear the {@link ResourceBundle} cache of any bundles loaded by this
     * class loader or any class loader where this loader is a parent class
     * loader. Whilst {@link ResourceBundle#clearCache()} could be used there
     * are complications around the
     * {@link org.apache.jasper.servlet.JasperLoader} that mean a reflection
     * based approach is more likely to be complete.
     *
     * The ResourceBundle is using WeakReferences so it shouldn't be pinning the
     * class loader in memory. However, it is. Therefore clear ou the
     * references.
     */
    private void clearReferencesResourceBundles() {
        // Get a reference to the cache
        try {
            Field cacheListField =
                ResourceBundle.class.getDeclaredField("cacheList");
            cacheListField.setAccessible(true);

            // Java 6 uses ConcurrentMap
            // Java 5 uses SoftCache extends Abstract Map
            // So use Map and it *should* work with both
            Map<?,?> cacheList = (Map<?,?>) cacheListField.get(null);

            // Get the keys (loader references are in the key)
            Set<?> keys = cacheList.keySet();

            Field loaderRefField = null;

            // Iterate over the keys looking at the loader instances
            Iterator<?> keysIter = keys.iterator();

            int countRemoved = 0;

            while (keysIter.hasNext()) {
                Object key = keysIter.next();

                if (loaderRefField == null) {
                    loaderRefField =
                        key.getClass().getDeclaredField("loaderRef");
                    loaderRefField.setAccessible(true);
                }
                WeakReference<?> loaderRef =
                    (WeakReference<?>) loaderRefField.get(key);

                ClassLoader loader = (ClassLoader) loaderRef.get();

                while (loader != null && loader != this) {
                    loader = loader.getParent();
                }

                if (loader != null) {
                    keysIter.remove();
                    countRemoved++;
                }
            }

            if (countRemoved > 0 && logger.isLoggable(Level.FINE)) {
                logger.fine(getString(
                        CLEAR_REFERENCES_RESOURCE_BUNDLES_COUNT,
                        Integer.valueOf(countRemoved), contextName));
            }
        } catch (SecurityException e) {
            logger.log(Level.SEVERE, getString(
                    CLEAR_REFERENCES_RESOURCE_BUNDLES_FAIL,
                    contextName), e);
        } catch (NoSuchFieldException e) {
            String msg = getString(
                    CLEAR_REFERENCES_RESOURCE_BUNDLES_FAIL, contextName);
            if (System.getProperty("java.vendor").startsWith("Sun")) {
                logger.log(Level.SEVERE, msg, e);
            } else if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, msg, e);
            }
        } catch (IllegalArgumentException e) {
            logger.log(Level.SEVERE, getString(
                    CLEAR_REFERENCES_RESOURCE_BUNDLES_FAIL,
                    contextName), e);
        } catch (IllegalAccessException e) {
            logger.log(Level.SEVERE, getString(
                    CLEAR_REFERENCES_RESOURCE_BUNDLES_FAIL,
                    contextName), e);
        }
    }

    // ------------------------------------------------------ Protected Methods


    /**
     * Used to periodically signal to the classloader to release JAR resources.
     */
    protected boolean openJARs() {
        if (started && (jarFiles.length > 0)) {
            lastJarAccessed = System.currentTimeMillis();
            if (jarFiles[0] == null) {
                for (int i = 0; i < jarFiles.length; i++) {
                    try {
                        jarFiles[i] = new JarFile(jarRealFiles[i]);
                    } catch (IOException e) {
                        if (logger.isLoggable(Level.FINE)) {
                            logger.log(Level.FINE, "Failed to open JAR", e);
                        }
                        for (int j = 0; j < i; j++) {
                            try {
                                jarFiles[j].close();
                            } catch (Throwable t) {
                                // Ignore
                            }
                        }
                        return false; 
                   }
                }
            }
        }
        return true;
    }


    /**
     * Find specified class in local repositories.
     *
     * @return the loaded class, or null if the class isn't found
     */
    protected ResourceEntry findClassInternal(String name)
        throws ClassNotFoundException {

        if (!validate(name))
            throw new ClassNotFoundException(name);

        String tempPath = name.replace('.', '/');
        String classPath = tempPath + ".class";

        ResourceEntry entry = findResourceInternal(name, classPath);

        if (entry == null)
               throw new ClassNotFoundException(name);

        synchronized (this) {
            Class<?> clazz = entry.loadedClass;
            if (clazz != null)
                return entry;

            if (entry.binaryContent == null)
                throw new ClassNotFoundException(name);
        }

        // Looking up the package
        String packageName = null;
        int pos = name.lastIndexOf('.');
        if (pos != -1)
            packageName = name.substring(0, pos);

        Package pkg = null;

        if (packageName != null) {

// START OF IASRI 4717252
          synchronized (loaderPC) {
// END OF IASRI 4717252
            pkg = getPackage(packageName);

            // Define the package (if null)
            if (pkg == null) {
                if (entry.manifest == null) {
                    definePackage(packageName, null, null, null, null, null,
                                  null, null);
                } else {
                    definePackage(packageName, entry.manifest, entry.codeBase);
                }
            }
// START OF IASRI 4717252
          }
// END OF IASRI 4717252
        }

        if (securityManager != null) {

            // Checking sealing
            if (pkg != null) {
                boolean sealCheck = true;
                if (pkg.isSealed()) {
                    sealCheck = pkg.isSealed(entry.codeBase);
                } else {
                    sealCheck = (entry.manifest == null)
                        || !isPackageSealed(packageName, entry.manifest);
                }
                if (!sealCheck)
                    throw new SecurityException
                        ("Sealing violation loading " + name + " : Package "
                         + packageName + " is sealed.");
            }

        }

        return entry;

    }

    /**
     * Find specified resource in local repositories. This block
     * will execute under an AccessControl.doPrivilege block.
     *
     * @return the loaded resource, or null if the resource isn't found
     */
    private ResourceEntry findResourceInternal(File file, String path){
        ResourceEntry entry = new ResourceEntry();
        try {
            entry.source = getURI(new File(file, path));
            entry.codeBase = getURL(new File(file, path));
        } catch (MalformedURLException e) {
            return null;
        }
        return entry;
    }


    /**
     * Attempts to find the specified resource in local repositories.
     *
     * @return the loaded resource, or null if the resource isn't found
     */
    protected ResourceEntry findResourceInternal(String name, String path) {
        return findResourceInternal(name, path, false);
    }

    private ResourceEntry findResourceInternal(String name, String path,
                                               boolean fromJarsOnly) {

        if (!started) {
                throw new IllegalStateException(
                getString(NOT_STARTED, name));
        }

        if ((name == null) || (path == null)) {
            return null;
        }

        ResourceEntry entry = resourceEntries.get(name);
        if (entry != null) {
            return entry;
        } else if (notFoundResources.containsKey(name)) {
            return null;
        }

        if (! fromJarsOnly) {
            entry = findResourceInternalFromRepositories(name, path);
        }

        if (entry == null) {
            synchronized (jarFiles) {
                entry = findResourceInternalFromJars(name, path);
            }
        }

        if (entry == null) {
            notFoundResources.put(name, name);
            return null;
        }

        // Add the entry in the local resource repository
        // Ensures that all the threads which may be in a race to load
        // a particular class all end up with the same ResourceEntry
        // instance
        ResourceEntry entry2 = resourceEntries.putIfAbsent(name, entry);
        if (entry2 != null) {
            entry = entry2;
        }

        return entry;
    }


    /**
     * Attempts to load the requested resource from this classloader's
     * internal repositories.
     *
     * @return The requested resource, or null if not found
     */
    private ResourceEntry findResourceInternalFromRepositories(String name,
                                                               String path) {

        if (repositories == null) {
            return null;
        }

        ResourceEntry entry = null;
        int contentLength = -1;
        InputStream binaryStream = null;
        int repositoriesLength = repositories.length;
        Resource resource = null;

        for (int i=0; (entry == null) && (i < repositoriesLength); i++) {

            try {

                String fullPath = repositories[i] + path;
                Object lookupResult = resources.lookup(fullPath);
                if (lookupResult instanceof Resource) {
                    resource = (Resource) lookupResult;
                }

                // Note : Not getting an exception here means the resource was
                // found
                if (securityManager != null) {
                    PrivilegedAction<ResourceEntry> dp =
                        new PrivilegedFindResource(files[i], path);
                    entry = AccessController.doPrivileged(dp);
                } else {
                    entry = findResourceInternal(files[i], path);
                }

                ResourceAttributes attributes =
                    (ResourceAttributes) resources.getAttributes(fullPath);
                contentLength = (int) attributes.getContentLength();
                entry.lastModified = attributes.getLastModified();

                if (resource != null) {

                    try {
                        binaryStream = resource.streamContent();
                    } catch (IOException e) {
                        return null;
                    }

                    // Register the full path for modification checking
                    // Note: Only syncing on a 'constant' object is needed
                    synchronized (ALL_PERMISSION) {

                        int j;

                        long[] result2 =
                            new long[lastModifiedDates.length + 1];
                        for (j = 0; j < lastModifiedDates.length; j++) {
                            result2[j] = lastModifiedDates[j];
                        }
                        result2[lastModifiedDates.length] = entry.lastModified;
                        lastModifiedDates = result2;

                        String[] result = new String[paths.length + 1];
                        for (j = 0; j < paths.length; j++) {
                            result[j] = paths[j];
                        }
                        result[paths.length] = fullPath;
                        paths = result;

                    }
                }
            } catch (NamingException e) {
            }
        }

        if (entry != null) {
            readEntryData(entry, name, binaryStream, contentLength, null);
        }

        return entry;
    }


    /**
     * Attempts to load the requested resource from this classloader's
     * JAR files.
     *
     * @return The requested resource, or null if not found
     */
    private ResourceEntry findResourceInternalFromJars(String name,
                                                       String path) {

        ResourceEntry entry = null;
        JarEntry jarEntry = null;
        int contentLength = -1;
        InputStream binaryStream = null;

        if (!openJARs()){
            return null;
        }

        int jarFilesLength = jarFiles.length;

        for (int i=0; (entry == null) && (i < jarFilesLength); i++) {
            jarEntry = jarFiles[i].getJarEntry(path);

            if (jarEntry != null) {

                entry = new ResourceEntry();
                try {
                    entry.codeBase = getURL(jarRealFiles[i]);
                    String jarFakeUrl = getURI(jarRealFiles[i]).toString();
                    jarFakeUrl = "jar:" + jarFakeUrl + "!/" + path;
                    entry.source = new URL(jarFakeUrl);
                    entry.lastModified = jarRealFiles[i].lastModified();
                } catch (MalformedURLException e) {
                    return null;
                }

                contentLength = (int) jarEntry.getSize();
                try {
                    entry.manifest = jarFiles[i].getManifest();
                    binaryStream = jarFiles[i].getInputStream(jarEntry);
                } catch (IOException e) {
                    return null;
                }

                // Extract resources contained in JAR to the workdir
                if (!(path.endsWith(".class"))) {
                    File resourceFile = new File
                        (loaderDir, jarEntry.getName());
                    if (!resourceFile.exists()) {
                        extractResources();
                    }
                }
            }
        }

        if (entry != null) {
            readEntryData(entry, name, binaryStream, contentLength, jarEntry);
        }

        return entry;
    }

    private synchronized void extractResources() {
        if (resourcesExtracted) {
            return;
        }

        for (int i = jarFiles.length - 1; i >= 0; i--) {
            extractResource(jarFiles[i]);
        }

        resourcesExtracted = true;
    }

    private void extractResource(JarFile jarFile) {
        byte[] buf = new byte[1024];
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry jarEntry2 = entries.nextElement();
            if (!(jarEntry2.isDirectory())
                && (!jarEntry2.getName().endsWith(".class"))) {
                File resourceFile = new File
                    (loaderDir, jarEntry2.getName());
                try {
                    if (!resourceFile.getCanonicalPath().startsWith(
                            canonicalLoaderDir)) {
                        throw new IllegalArgumentException(getString(
                                ILLEGAL_JAR_PATH, jarEntry2.getName()));
                    }
                } catch (IOException ioe) {
                    throw new IllegalArgumentException(getString(
                            VALIDATION_ERROR_JAR_PATH, jarEntry2.getName(), ioe));
                }
                if (!FileUtils.mkdirsMaybe(resourceFile.getParentFile())) {
                    logger.log(Level.WARNING,
                            UNABLE_TO_CREATE,
                            resourceFile.getParentFile().toString());
                }

                FileOutputStream os = null;
                InputStream is = null;
                try {
                    is = jarFile.getInputStream(jarEntry2);
                    os = new FileOutputStream(resourceFile);
                    while (true) {
                        int n = is.read(buf);
                        if (n <= 0) {
                            break;
                        }
                        os.write(buf, 0, n);
                    }
                } catch (IOException e) {
                    // Ignore
                } finally {
                    try {
                        if (is != null) {
                            is.close();
                        }
                    } catch (IOException e) {
                    }
                    try {
                        if (os != null) {
                            os.close();
                        }
                    } catch (IOException e) {
                    }
                }
            }
        }
    }

    public File getExtractedResourcePath(String path) {
        extractResources();
        File extractedResource = new File(loaderDir, path);
        return (extractedResource.exists() ? extractedResource : null);
    }


    /**
     * Reads the resource's binary data from the given input stream.
     */
    private void readEntryData(ResourceEntry entry,
                               String name,
                               InputStream binaryStream,
                               int contentLength,
                               JarEntry jarEntry) {

        if (binaryStream == null) {
            return;
        }

        byte[] binaryContent = new byte[contentLength];

        try {
            int pos = 0;

            while (true) {
                int n = binaryStream.read(binaryContent, pos,
                                          binaryContent.length - pos);
                if (n <= 0)
                    break;
                pos += n;
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, getString(READ_CLASS_ERROR, name), e);
            return;
        } finally {
            try {
                binaryStream.close();
            } catch(IOException e) {
            }
        }

        // START OF IASRI 4709374
        // Preprocess the loaded byte code if bytecode preprocesser is
        // enabled
        if (PreprocessorUtil.isPreprocessorEnabled()) {
            binaryContent =
                PreprocessorUtil.processClass(name, binaryContent);
        }
        // END OF IASRI 4709374

        entry.binaryContent = binaryContent;

        // The certificates are only available after the JarEntry
        // associated input stream has been fully read
        if (jarEntry != null) {
            entry.certificates = jarEntry.getCertificates();
        }
    }


    /**
     * Returns true if the specified package name is sealed according to the
     * given manifest.
     */
    protected boolean isPackageSealed(String name, Manifest man) {

        String path = name.replace('.', '/') + '/';
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
        return "true".equalsIgnoreCase(sealed);

    }


    /**
     * Finds the resource with the given name if it has previously been
     * loaded and cached by this class loader, and return an input stream
     * to the resource data.  If this resource has not been cached, return
     * <code>null</code>.
     *
     * @param name Name of the resource to return
     */
    protected InputStream findLoadedResource(String name) {

        ResourceEntry entry = resourceEntries.get(name);
        if (entry != null) {
            if (entry.binaryContent != null)
                return new ByteArrayInputStream(entry.binaryContent);
        }
        return (null);

    }


    /**
     * Finds the class with the given name if it has previously been
     * loaded and cached by this class loader, and return the Class object.
     * If this class has not been cached, return <code>null</code>.
     *
     * @param name Name of the resource to return
     */
    protected Class<?> findLoadedClass0(String name) {

        ResourceEntry entry = resourceEntries.get(name);
        if (entry != null) {
            synchronized(this) {
                return entry.loadedClass;
            }
        }
        return (null);  // FIXME - findLoadedResource()

    }


    /**
     * Refresh the system policy file, to pick up eventual changes.
     */
    protected void refreshPolicy() {

        try {
            // The policy file may have been modified to adjust
            // permissions, so we're reloading it when loading or
            // reloading a Context
            Policy policy = Policy.getPolicy();
            policy.refresh();
        } catch (AccessControlException e) {
            // Some policy files may restrict this, even for the core,
            // so this exception is ignored
        }

    }


    /**
     * Filter classes.
     *
     * @param name class name
     * @return true if the class should be filtered
     */
    protected boolean filter(String name) {

        if (name == null)
            return false;

        // START PE 4985680
        // Special case for performance reason.
        if (name.startsWith("java."))
            return true;
        // END PE 4985680

        // Looking up the package
        String packageName = null;
        int pos = name.lastIndexOf('.');
        if (pos != -1)
            packageName = name.substring(0, pos);
        else
            return false;

        if (overridablePackages != null){
            for (String overridePkg : overridablePackages) {
                if (packageName.startsWith(overridePkg))
                    return false;
            }
        }

        for (int i = 0; i < packageTriggers.length; i++) {
            if (packageName.startsWith(packageTriggers[i]))
                return true;
        }

        return false;

    }


    /**
     * Validate a classname. As per SRV.9.7.2, we must restrict loading of
     * classes from J2SE (java.*) and classes of the servlet API
     * (javax.servlet.*). That should enhance robustness and prevent a number
     * of user error (where an older version of servlet.jar would be present
     * in /WEB-INF/lib).
     *
     * @param name class name
     * @return true if the name is valid
     */
    protected boolean validate(String name) {

        if (name == null)
            return false;
        if (name.startsWith("java."))
            return false;

        return true;

    }


    /**
     * Get URL.
     */
    protected URL getURL(File file)
        throws MalformedURLException {

        File realFile = file;
        try {
            realFile = realFile.getCanonicalFile();
        } catch (IOException e) {
            // Ignore
        }
        return realFile.toURI().toURL();

    }


    /**
     * Get URL.
     */
    protected URL getURI(File file)
        throws MalformedURLException {

        try {
            file = file.getCanonicalFile();
        } catch (IOException e) {
            // Ignore
        }

        return file.toURI().toURL();

    }


    /**
     * Delete the specified directory, including all of its contents and
     * subdirectories recursively.
     *
     * @param dir File object representing the directory to be deleted
     */
    protected static void deleteDir(File dir) {

        String files[] = dir.list();
        if (files == null) {
            files = new String[0];
        }
        for (int i = 0; i < files.length; i++) {
            File file = new File(dir, files[i]);
            if (file.isDirectory()) {
                deleteDir(file);
            } else {
                if (!FileUtils.deleteFileMaybe(file)) {
                    logger.log(Level.WARNING,
                            UNABLE_TO_DELETE,
                            file.toString());
                }

            }
        }
        if (!FileUtils.deleteFileMaybe(dir)) {
            logger.log(Level.WARNING,
                    UNABLE_TO_DELETE,
                    dir.toString());
        }

    }

    // START SJSAS 6344989
    public void addByteCodePreprocessor(BytecodePreprocessor preprocessor) {
        byteCodePreprocessors.add(preprocessor);
    }
    // END SJSAS 6344989


    // START GlassFish Issue 587
    /*
     * Purges all bean classes that were loaded by this WebappClassLoader
     * from the caches maintained by javax.el.BeanELResolver, in order to
     * avoid this WebappClassLoader from leaking.
     */
    private void purgeELBeanClasses() {

        Field fieldlist[] = javax.el.BeanELResolver.class.getDeclaredFields();
        for (int i = 0; i < fieldlist.length; i++) {
            Field fld = fieldlist[i];
            if (fld.getName().equals("properties")) {
                purgeELBeanClasses(fld);
                break;
            }
        }
    }

    /*
     * Purges all bean classes that were loaded by this WebappClassLoader
     * from the cache represented by the given reflected field.
     *
     * @param fld The reflected field from which to remove the bean classes
     * that were loaded by this WebappClassLoader
     */
    private void purgeELBeanClasses(final Field fld) {

        setAccessible(fld);

        Map<Class, ?> m = null;
        try {
            m = getBeanELResolverProperties(fld);
        } catch (IllegalAccessException iae) {
            logger.log(Level.WARNING, UNABLE_PURGE_BEAN_CLASSES, iae);
            return;
        }

        if (m.size() == 0) {
            return;
        }

        Iterator<Class> iter = m.keySet().iterator();
        while (iter.hasNext()) {
            Class mbeanClass = iter.next();
            if (this.equals(mbeanClass.getClassLoader())) {
                iter.remove();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<Class, ?> getBeanELResolverProperties(Field fld) throws IllegalAccessException {
        return (Map<Class, ?>)fld.get(null);
    }
    // END GlassFish Issue 587

     /**
     * Create and return a temporary loader with the same visibility
      * as this loader. The temporary loader may be used to load
      * resources or any other application classes for the purposes of
      * introspecting them for annotations. The persistence provider
      * should not maintain any references to the temporary loader,
      * or any objects loaded by it.
      *
      * @return A temporary classloader with the same classpath as this loader
      */
     public ClassLoader copy() {
         logger.entering("WebModuleListener$InstrumentableWebappClassLoader", "copy");
         // set getParent() as the parent of the cloned class loader
         return AccessController.doPrivileged(new PrivilegedAction<URLClassLoader>() {
             @Override
             public URLClassLoader run() {
                 return new URLClassLoader(getURLs(), getParent());
             }
         });
     }

     /**
     * Add a new ClassFileTransformer to this class loader. This transfomer should be called for
      * each class loading event.
      *
      * @param transformer new class file transformer to do byte code enhancement.
      */
     public void addTransformer(final ClassFileTransformer transformer) {
        final WebappClassLoader cl = this;
        addByteCodePreprocessor(new BytecodePreprocessor(){
                /*
                 * This class adapts ClassFileTransformer to ByteCodePreprocessor that
                 * is used inside WebappClassLoader.
                 */

                public boolean initialize(Hashtable parameters) {
                    return true;
                }

                public byte[] preprocess(String resourceName, byte[] classBytes) {
                    try {
                        // convert java/lang/Object.class to java/lang/Object
                        String classname = resourceName.substring(0,
                                resourceName.length() - 6); // ".class" size = 6
                        byte[] newBytes = transformer.transform(
                                cl, classname, null, null, classBytes);
                        // ClassFileTransformer returns null if no transformation
                        // took place, where as ByteCodePreprocessor is expected
                        // to return non-null byte array.
                        return newBytes == null ? classBytes : newBytes;
                    } catch (IllegalClassFormatException e) {
                        logger.logp(Level.WARNING,
                                "WebModuleListener$InstrumentableClassLoader$BytecodePreprocessor",
                                "preprocess", e.getMessage());
                        throw new RuntimeException(e);
                    }
                }
            });
     }

     private String getJavaVersion() {

        String version = null;

	SecurityManager sm = System.getSecurityManager();
	if (sm != null) {
            version = AccessController.doPrivileged(
                new PrivilegedAction<String>() {
                    public String run() {
                        return System.getProperty("java.version");
                    }
            });
        } else {
            version = System.getProperty("java.version");
        }

        return version;
    }

    private void setAccessible(final Field field) {

        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    public Void run() {
                        field.setAccessible(true);
                        return null;
                    }
            });
        } else {
            field.setAccessible(true);
        }

    }

    /**
     * To determine whether one should delegate to parent for loading
     * resource of the given resource name.
     * 
     * @param name
     */
    private boolean isResourceDelegate(String name) {
        return (delegate
                || (name.startsWith("javax") &&
                    (!name.startsWith("javax.faces") || !useMyFaces))
                || name.startsWith("sun")
                || (name.startsWith("com/sun/faces") &&
                    !name.startsWith("com/sun/faces/extensions") &&
                    !useMyFaces)
                || name.startsWith("org/apache/taglibs/standard"));
    }

    private static String getString(String key, Object ... arguments) {
        String msg = rb.getString(key);
        return MessageFormat.format(msg, arguments);
    }
}
