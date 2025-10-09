/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2019 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright 2016-2025 Payara Foundation and/or its affiliates

package org.glassfish.web.loader;

import com.sun.appserv.BytecodePreprocessor;
import com.sun.appserv.ClassLoaderUtil;
import com.sun.appserv.server.util.PreprocessorUtil;
import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.glassfish.bootstrap.MainHelper.HotSwapHelper;
import com.sun.enterprise.loader.CacheCleaner;
import com.sun.enterprise.security.integration.DDPermissionsLoader;
import com.sun.enterprise.security.integration.PermsHolder;
import com.sun.enterprise.util.io.FileUtils;
import org.apache.naming.JndiPermission;
import org.apache.naming.resources.DirContextURLStreamHandler;
import org.apache.naming.resources.JarFileResourcesProvider;
import org.apache.naming.resources.ProxyDirContext;
import org.apache.naming.resources.Resource;
import org.apache.naming.resources.ResourceAttributes;
import org.apache.naming.resources.WebDirContext;
import org.glassfish.api.deployment.GeneratedResourceEntry;
import org.glassfish.api.deployment.InstrumentableClassLoader;
import org.glassfish.api.deployment.ResourceClassLoader;
import org.glassfish.api.deployment.ResourceEntry;
import org.glassfish.common.util.InstanceCounter;
import org.glassfish.exousia.AuthorizationService;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.web.util.ExceptionUtils;
import org.glassfish.web.util.IntrospectionUtils;

import javax.naming.Binding;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

/**
 * Specialized web application class loader.
 * <p>
 * This class loader is a full reimplementation of the
 * <code>URLClassLoader</code> from the JDK. It is designed to be fully
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
    implements Reloader, ResourceClassLoader,
        InstrumentableClassLoader, PreDestroy,
        DDPermissionsLoader, JarFileResourcesProvider
{
    // ------------------------------------------------------- Static Variables

    private static final Logger logger = LogFacade.getLogger();

    private static final ResourceBundle rb = logger.getResourceBundle();

    /**
     * Set of package names which are not allowed to be loaded from a webapp
     * class loader without delegating first.
     */
    private static final String[] packageTriggers = {
        "javax",                                     // Java extensions
        "jakarta",
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

    private static final String META_INF_SERVICES = "META-INF/services/";

    // ----------------------------------------------------- Instance Variables

    // START PE 4989455
    /**
     * Use this variable to invoke the security manager when a resource is
     * loaded by this classloader.
     */
    private final boolean packageDefinitionEnabled = System.getProperty("package.definition") != null;
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
    protected ConcurrentHashMap<String, ResourceEntry> resourceEntries
            = new ConcurrentHashMap<>();

    /**
     * The list of not found resources.
     */
    protected ConcurrentHashMap<String, String> notFoundResources
            = new ConcurrentHashMap<>();

    /**
     * The debugging detail level of this component.
     */
    protected int debug = 0;

    /**
     * When configured it will host the value for SameSite Cookie
     */
    private String cookieSameSiteValue = "";

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
     * Lock to synchronize closing, opening and accessing of jar
     */
    protected final Object jarFilesLock = new Object();

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
    protected List<String> jarNames = new ArrayList<>();

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
    private final ConcurrentLinkedQueue<Permission> permissionList
            = new ConcurrentLinkedQueue<>();

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
    private final ConcurrentHashMap<String, PermissionCollection> loaderPC
            = new ConcurrentHashMap<>();

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
    private final ConcurrentLinkedQueue<BytecodePreprocessor> byteCodePreprocessors
            = new ConcurrentLinkedQueue<>();
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

    /**
     * Use anti JAR locking code, which does URL rerouting when accessing
     * resources.
     */
    boolean antiJARLocking = false;

    /**
     * Reference to the JDBC Leak Prevention class.
     * Held uniquely due to the way it is accessed outside the normal
     * ClassLoader mechanism.
     */
    private Class<?> jdbcLeakPreventionResourceClass = null;

    private final Application application;
    private final Date creationTime = new Date();
    private boolean hotDeploy = false;
    private final InstanceCounter instanceCounter = new InstanceCounter(this);

    private static final Class<?>[] CONSTRUCTOR_ARGS_TYPES;
    private static final Object CONSTRUCTOR_ARGUMENTS;
    private static final Boolean isMultiReleaseJar;
    private static final Name MULTI_RELEASE = new Name("Multi-Release");

    static {
        Class<?>[] constructorArgsTypes;
        Object constructorArguments;

        boolean isException = false;
        try {
            final Class<?> runtimeVersionClass = Class.forName("java.lang.Runtime$Version");
            constructorArgsTypes = new Class[]{File.class, boolean.class, int.class, runtimeVersionClass};
            constructorArguments = Runtime.class.getDeclaredMethod("version").invoke(null);
        } catch (Exception e) {
            isException = true;
            constructorArgsTypes = null;
            constructorArguments = null;
        }
        isMultiReleaseJar = !isException;

        CONSTRUCTOR_ARGS_TYPES = constructorArgsTypes;
        CONSTRUCTOR_ARGUMENTS = constructorArguments;
    }

    // ----------------------------------------------------------- Constructors

    /**
     * Construct a new ClassLoader with no defined repositories and no
     * parent ClassLoader.
     * @param application
     */
    public WebappClassLoader(Application application) {
        super(new URL[0]);
        this.application = application;
        init();
    }

    /**
     * Construct a new ClassLoader with the given parent ClassLoader, but no
     * defined repositories.
     *
     * @param parent
     * @param application
     */
    public WebappClassLoader(ClassLoader parent, Application application) {
        super(new URL[0], parent);
        this.application = application;
        init();
    }

    /**
     * Construct a new ClassLoader with the given parent ClassLoader, but no
     * defined repositories.
     *
     * @param parent
     * @param application
     * @param hotDeploy
     */
    public WebappClassLoader(ClassLoader parent, Application application, boolean hotDeploy) {
        super(new URL[0], parent);
        this.application = application;
        this.hotDeploy = hotDeploy;
        init();
    }


    /**
     * for use in OSGi loader
     *
     * @param parent
     */
    public WebappClassLoader(ClassLoader parent) {
        this(parent, Application.createApplication());
    }


    /**
     * Construct a new ClassLoader with the given parent ClassLoader
     * and defined repositories.
     * @param urls
     * @param parent
     * @param application
     */
    public WebappClassLoader(URL[] urls, ClassLoader parent, Application application) {
        super(new URL[0], parent);
        this.application = application;

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

        private final File file;
        private final String path;

        PrivilegedFindResource(File file, String path) {
            this.file = file;
            this.path = path;
        }

        @Override
        public ResourceEntry run() {
            return findResourceInternal(file, path);
        }
    }


    protected static final class PrivilegedGetClassLoader
        implements PrivilegedAction<ClassLoader> {

        public Class<?> clazz;

        public PrivilegedGetClassLoader(Class<?> clazz) {
            this.clazz = clazz;
        }

        @Override
        public ClassLoader run() {
            return clazz.getClassLoader();
        }
    }

    // START PE 4985680
    /**
     * Adds the given package name to the list of packages that may always be
     * overriden, regardless of whether they belong to a protected namespace
     */
    public synchronized void addOverridablePackage(String packageName) {
        if (overridablePackages == null) {
            overridablePackages = new ConcurrentLinkedQueue<String>();
        }
        overridablePackages.add(packageName);
    }
    // END PE 4985680


    /**
     * Unused. Always returns {@code null} - See CVE-2022-22965
     * Get associated resources.
     */
    @Deprecated
    public DirContext getResources() {
        return null;
    }


    /**
     * Set associated resources.
     */
    public void setResources(DirContext resources) {
        this.resources = resources;

        DirContext res = resources;
        if (resources instanceof ProxyDirContext) {
            ProxyDirContext proxyRes = (ProxyDirContext)res;
            contextName = proxyRes.getContextName();
            res = proxyRes.getDirContext();
        }

        if (res instanceof WebDirContext) {
            ((WebDirContext)res).setJarFileResourcesProvider(this);
        }
    }

    /**
     * Return the context name for this class loader.
     *
     * @return
     */
    public String getContextName() {
        return this.contextName;
    }

    @Override
    public ConcurrentHashMap<String, ResourceEntry> getResourceEntries() {
        return resourceEntries;
    }

    @Override
    public Class addResourceEntry(String name, String path, ResourceEntry entry) {
        Class clazz = null;
        if (!this.resourceEntries.containsKey(name)) {
            definePackage(name, entry);
            clazz = defineLoadedClass(name, entry);
            this.resourceEntries.put(name, entry);
            for (Map.Entry<String, GeneratedResourceEntry> e : entry.generatedResources.entrySet()) {
                String generatedClassName = e.getKey();
                GeneratedResourceEntry generatedEntry = e.getValue();
                generatedEntry.loadedClass = defineClass(
                        generatedClassName,
                        generatedEntry.binaryContent, 0,
                        generatedEntry.binaryContent.length,
                        generatedEntry.protectionDomain
                );
            }
        }
        return clazz;
    }

    @Override
    public Class reloadResourceEntry(String name, String path, ResourceEntry entry) {
        try {
            InputStream binaryStream = null;
            Resource resource = null;
            Object lookupResult = resources.lookup(path);
            if (lookupResult instanceof Resource) {
                resource = (Resource) lookupResult;
            }

            ResourceAttributes attributes
                    = (ResourceAttributes) resources.getAttributes(path);
            int contentLength = (int) attributes.getContentLength();
            entry.lastModified = attributes.getLastModified();

            if (resource != null) {
                try {
                    binaryStream = resource.streamContent();
                } catch (IOException e) {
                    return null;
                }
            }
            readEntryData(entry, name, binaryStream, contentLength, null);

            return loadClass(name);
        } catch (NamingException | ClassNotFoundException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Override
    public Class addGeneratedResourceEntry(
            String mainClassName,
            String generatedClassName,
            byte[] generatedBinaryContent,
            ProtectionDomain protectionDomain) {

        Class generatedClass;

        if (this.resourceEntries.containsKey(mainClassName)) {
            generatedClass = defineClass(generatedClassName,
                    generatedBinaryContent, 0,
                    generatedBinaryContent.length,
                    protectionDomain);
            GeneratedResourceEntry generatedResourceEntry = new GeneratedResourceEntry();
            generatedResourceEntry.binaryContent = generatedBinaryContent;
            generatedResourceEntry.loadedClass = generatedClass;
            generatedResourceEntry.protectionDomain = protectionDomain;
            ResourceEntry entry = this.resourceEntries.get(mainClassName);
            entry.generatedResources.put(generatedClassName, generatedResourceEntry);
        } else {
            generatedClass = defineClass(generatedClassName,
                    generatedBinaryContent, 0,
                    generatedBinaryContent.length,
                    protectionDomain);
        }
        return generatedClass;
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

    public String getCookieSameSiteValue() {
        return cookieSameSiteValue;
    }

    public void setCookieSameSiteValue(String cookieSameSiteValue) {
        this.cookieSameSiteValue = cookieSameSiteValue;
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
     * @return Returns the antiJARLocking.
     */
    public boolean getAntiJARLocking() {
        return antiJARLocking;
    }


    /**
     * @param antiJARLocking The antiJARLocking to set.
     */
    public void setAntiJARLocking(boolean antiJARLocking) {
        this.antiJARLocking = antiJARLocking;
    }


    @Override
    public JarFile[] getJarFiles() {
        if (!openJARs()) {
            return null;
        }
        return jarFiles;
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

            securityManager.checkSecurityAccess(DDPermissionsLoader.SET_EE_POLICY);

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

            securityManager.checkSecurityAccess(DDPermissionsLoader.SET_EE_POLICY);

            permissionList.add(permission);
        }
    }


    @Override
    public void addDeclaredPermissions(PermissionCollection declaredPc
            ) throws SecurityException {

        if (securityManager != null) {
            securityManager.checkSecurityAccess(DDPermissionsLoader.SET_EE_POLICY);

            permissionsHolder.setDeclaredPermissions(declaredPc);
        }
    }

    @Override
    public void addEEPermissions(PermissionCollection eePc)
         throws SecurityException {

        if (securityManager != null) {
            securityManager.checkSecurityAccess(DDPermissionsLoader.SET_EE_POLICY);

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
            addOverridablePackage("jakarta.faces");
            addOverridablePackage("com.sun.faces");
        }
    }


    /**
     * Return the clearReferencesStatic flag for this Context.
     */
    public boolean getClearReferencesStatic() {
        return this.clearReferencesStatic;
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
    @Override
    public void addRepository(String repository) {

        // Ignore any of the standard repositories, as they are set up using
        // either addJar or addRepository
        if (repository.startsWith("/WEB-INF/lib")
            || repository.startsWith("/WEB-INF/classes")) {
            return;
        }

        // Add this repository to our underlying class loader
        try {
            addRepository(new URL(repository));
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid repository: " + repository, e);
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

        if (repository == null) {
            return;
	}

        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "addRepository({0})", repository);
        }

        // Add this repository to our internal list
        String[] result = new String[repositories.length + 1];
        for (int i = 0; i < repositories.length; i++) {
            result[i] = repositories[i];
        }
        result[repositories.length] = repository;
        repositories = result;

        // Add the file to the list
        File[] result2 = new File[files.length + 1];
        for (int i = 0; i < files.length; i++) {
            result2[i] = files[i];
        }
        result2[files.length] = file;
        files = result2;

    }


    public synchronized void addJar(String jar, JarFile jarFile, File file)
        throws IOException {

        if (jar == null) {
            return;
        }
        if (jarFile == null) {
            return;
        }
        if (file == null) {
            return;
        }

        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "addJar({0})", jar);
        }

        synchronized (jarFilesLock) {
            // See IT 11417
            super.addURL(getURL(file));

            if ((jarPath != null) && (jar.startsWith(jarPath))) {

                String jarName = jar.substring(jarPath.length());
                while (jarName.startsWith("/")) {
                    jarName = jarName.substring(1);
                }
                jarNames.add(jarName);
            }

            try {
                // Register the JAR for tracking

                long lastModified = ((ResourceAttributes) resources.getAttributes(jar))
                        .getLastModified();

                String[] result = new String[paths.length + 1];
                for (int i = 0; i < paths.length; i++) {
                    result[i] = paths[i];
                }
                result[paths.length] = jar;
                paths = result;

                long[] result3 = new long[lastModifiedDates.length + 1];
                for (int i = 0; i < lastModifiedDates.length; i++) {
                    result3[i] = lastModifiedDates[i];
                }
                result3[lastModifiedDates.length] = lastModified;
                lastModifiedDates = result3;

            } catch (NamingException e) {
                // Ignore
            }

            JarFile[] result2 = new JarFile[jarFiles.length + 1];
            for (int i = 0; i < jarFiles.length; i++) {
                result2[i] = jarFiles[i];
            }
            result2[jarFiles.length] = jarFile;
            jarFiles = result2;

            // Add the file to the list
            File[] result4 = new File[jarRealFiles.length + 1];
            for (int i = 0; i < jarRealFiles.length; i++) {
                result4[i] = jarRealFiles[i];
            }
            result4[jarRealFiles.length] = file;
            jarRealFiles = result4;
        }
    }


    /**
     * Have one or more classes or resources been modified so that a reload
     * is appropriate?
     */
    @Override
    public boolean modified() {

        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "modified()");
        }
        // Checking for modified loaded resources
        int pathsLength = paths.length;

        // A rare race condition can occur in the updates of the two arrays
        // It's totally ok if the latest class added is not checked (it will
        // be checked the next time
        int lastModifiedDatesLength = lastModifiedDates.length;
        if (pathsLength > lastModifiedDatesLength) {
            pathsLength = lastModifiedDatesLength;
        }

        for (int i = 0; i < pathsLength; i++) {
            try {
                long lastModified = ((ResourceAttributes) resources.getAttributes(paths[i])).getLastModified();
                if (lastModified != lastModifiedDates[i]) {
                    if (logger.isLoggable(Level.FINER)) {
                        logger.log(Level.FINER, "  Resource ''{0}'' was modified; Date is now: {1} Was: {2}",
                                new Object[]{paths[i], new java.util.Date(lastModified), new java.util.Date(lastModifiedDates[i])});
                    }
                    return true;
                }
            } catch (NamingException e) {
                logger.log(Level.SEVERE, LogFacade.MISSING_RESOURCE, paths[i]);
                return true;
            }
        }

        pathsLength = jarNames.size();

        // Check if JARs have been added or removed
        if (getJarPath() != null) {
            try {
                NamingEnumeration<Binding> enumeration =
                    resources.listBindings(getJarPath());
                int i = 0;
                while (enumeration.hasMoreElements() && (i < pathsLength)) {
                    NameClassPair ncPair = enumeration.nextElement();
                    String name = ncPair.getName();
                    // Ignore non JARs present in the lib folder
// START OF IASRI 4657979
                    if (!name.endsWith(".jar") && !name.endsWith(".zip")) {
// END OF IASRI 4657979
                        continue;
                    }

                    if (!name.equals(jarNames.get(i))) {
                        // Missing JAR
                        logger.log(Level.FINER, "    Additional JARs have been added : ''{0}''", name);
                        return true;
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
                            return true;
                        }
                    }
                } else if (i < jarNames.size()) {
                    // There was less JARs
                    logger.log(Level.FINER, "    Additional JARs have been added");
                    return true;
                }
            } catch (NamingException e) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, "    Failed tracking modifications of ''{0}''", getJarPath());
                }
            } catch (ClassCastException e) {
                logger.log(Level.SEVERE, LogFacade.FAILED_TRACKING_MODIFICATIONS, new Object[]{getJarPath(), e.getMessage()});
            }

        }

        // No classes have been modified
        return false;
    }


    /**
     * Render a String representation of this object.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Predicate<String> matchesInternal = str -> str.matches(".*generated/.*__.*");
        boolean isInternal = false;
        if (repositoryURLs != null) {
            isInternal = Arrays.stream(repositoryURLs).map(URL::toString)
                    .filter(matchesInternal).findAny().isPresent();
        }
        if (canonicalLoaderDir != null && matchesInternal.test(canonicalLoaderDir)) {
            isInternal = true;
        }
        if (isInternal) {
            sb.append("(internal) ");
        }
        sb.append("WebappClassLoader (delegate=");
        sb.append(delegate);
        if (repositoryURLs != null) {
            sb.append("; repositoryURLs=");
            for (int i = 0; i < repositoryURLs.length; i++) {
                sb.append(repositoryURLs[i]);
                if (i != (repositoryURLs.length-1)) {
                    sb.append(",");
                }
            }
        }
        sb.append(") ");
        sb.append("JarNames: ").append(jarNames).append("; ");
        sb.append("canonicalLoaderDir: ").append(canonicalLoaderDir).append("; ");
        sb.append("Object: ").append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(" Created: ").append(SimpleDateFormat.getDateTimeInstance().format(creationTime));
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

        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "    findClass({0})", name);
        }

        // (1) Permission to define this class when using a SecurityManager
        // START PE 4989455
        //if (securityManager != null) {
        if (securityManager != null && packageDefinitionEnabled) {
            // END PE 4989455
            int i = name.lastIndexOf('.');
            if (i >= 0) {
                try {
                    if (logger.isLoggable(Level.FINER)) {
                        logger.log(Level.FINER, "      securityManager.checkPackageDefinition");
                    }
                    securityManager.checkPackageDefinition(name.substring(0, i));
                } catch (Exception se) {
                    if (logger.isLoggable(Level.FINER)) {
                        logger.log(Level.FINER, "      -->Exception-->ClassNotFoundException", se);
                    }
                    throw new ClassNotFoundException(name, se);
                }
            }
        }

        // Ask our superclass to locate this class, if possible
        // (throws ClassNotFoundException if it is not found)
        Class<?> clazz = null;
        try {
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "      findClassInternal({0})", name);
            }
            try {
                ResourceEntry entry = findClassInternal(name);

                synchronized (this) {
                    if (entry.loadedClass == null) {
                        clazz = defineLoadedClass(name, entry);
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
                        getString(LogFacade.UNSUPPORTED_VERSION, name, getJavaVersion()));
            } catch(AccessControlException ace) {
                if (logger.isLoggable(Level.WARNING)) {
                    logger.log(Level.WARNING, LogFacade.FIND_CLASS_INTERNAL_SECURITY_EXCEPTION, new Object[]{name, ace.getMessage()});
                }
                throw new ClassNotFoundException(name, ace);
            } catch(RuntimeException | Error rex) {
                throw rex;
            } catch (Throwable t) {
                throw new RuntimeException(getString(LogFacade.UNABLE_TO_LOAD_CLASS, name, t.toString()), t);
            }
            if ((clazz == null) && hasExternalRepositories) {
                try {
                    clazz = super.findClass(name);
                } catch(AccessControlException ace) {
                    if (logger.isLoggable(Level.WARNING)) {
                        String msg = getString(LogFacade.FIND_CLASS_INTERNAL_SECURITY_EXCEPTION,
                                new Object[]{name, ace.getMessage()});
                        logger.log(Level.WARNING, msg, ace);
                    }
                    throw new ClassNotFoundException(name, ace);
                } catch (RuntimeException e) {
                    if (logger.isLoggable(Level.FINER)) {
                        logger.log(Level.FINER, "      -->RuntimeException Rethrown", e);
                    }
                    throw e;
                }
            }
            if (clazz == null) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, "    --> Returning ClassNotFoundException");
                }
                throw new ClassNotFoundException(name);
            }
        } catch (ClassNotFoundException e) {
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "    --> Passing on ClassNotFoundException");
            }
            throw e;
        }

        // Return the class we have located
        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "      Returning class {0}", clazz);
        }
        if (logger.isLoggable(Level.FINER)) {
            ClassLoader cl;
            if (securityManager != null) {
                cl = AccessController.doPrivileged(
                    new PrivilegedGetClassLoader(clazz));
            } else {
                cl = clazz.getClassLoader();
            }
            logger.log(Level.FINER, "      Loaded by {0}", cl);
        }
        return clazz;

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
        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "    findResource({0})", name);
        }

        URL url = null;

        if (".".equals(name)) {
            name = "";
        }

        ResourceEntry entry = resourceEntries.get(name);
        if (entry == null) {
            entry = findResourceInternal(name, name);
        }
        if (entry != null) {
            url = entry.source;
        }

        if ((url == null) && hasExternalRepositories) {
            url = super.findResource(name);
        }

        if (logger.isLoggable(Level.FINER)) {
            if (url != null) {
                logger.log(Level.FINER, "    --> Returning ''{0}''", url.toString());
            } else {
                logger.log(Level.FINER, "    --> Resource not found, returning null");
            }
        }
        return NonCachedJarStreamHandler.forceNonCachedJarURL(url);
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

        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "    findResources({0})", name);
        }

        List<URL> result = new ArrayList<>();

        if (repositories != null) {
            synchronized (jarFilesLock) {
                // resources.lookup(fullPath) calls through WebDirContext the method getJarFiles().
                // It returns the jarFiles but those should not be modified (like closed) so
                // we protect changes using this synchronized block.
                int repositoriesLength = repositories.length;

                // Looking at the repositories
                for (int i = 0; i < repositoriesLength; i++) {
                    try {
                        String fullPath = repositories[i] + name;
                        resources.lookup(fullPath);
                        // Note : Not getting an exception here means the resource was
                        // found
                        try {
                            result.add(getURI(new File(files[i], name)));
                        } catch (MalformedURLException e) {
                            // Ignore
                        }
                    } catch (NamingException e) {
                    }
                }
            }
        }

        Enumeration<URL> otherResourcePaths = super.findResources(name);

        while (otherResourcePaths.hasMoreElements()) {
            result.add(otherResourcePaths.nextElement());
        }

        return Collections.enumeration(result.stream()
                .map(NonCachedJarStreamHandler::forceNonCachedJarURL)
                .collect(Collectors.toList()));
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
        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "getResource({0})", name);
        }
        URL url = null;

        /*
         * (1) Delegate to parent if requested, or if the requested resource
         * belongs to one of the packages that are part of the Java EE platform
         */
        if (isResourceDelegate(name)) {
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "{0}  Delegating to parent classloader ", parent);
            }
            ClassLoader loader = parent;
            if (loader == null) {
                loader = system;
            }
            url = loader.getResource(name);
            if (url != null) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, "  --> Returning ''{0}''", url.toString());
                }
                return (url);
            }
        }

        // (2) Search local repositories
        url = findResource(name);
        if (url != null) {
            if (antiJARLocking) {
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
                    logger.log(Level.FINEST, null, e);
                }
            }
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "  --> Returning ''{0}''", url.toString());
            }
            return (url);
        }

        // (3) Delegate to parent unconditionally if not already attempted
        if (!delegate) {
            ClassLoader loader = parent;
            if (loader == null) {
                loader = system;
            }
            url = loader.getResource(name);
            if (url != null) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, "  --> Returning ''{0}''", url.toString());
                }
                return (url);
            }
        }

        // (4) Resource was not found
        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "  --> Resource not found, returning null");
        }
        return null;
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

        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "getResourceAsStream({0})", name);
        }
        InputStream stream = null;

        // (0) Check for a cached copy of this resource
        stream = findLoadedResource(name);
        if (stream != null) {
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "  --> Returning stream from cache");
            }
        return stream;
        }

        /*
         * (1) Delegate to parent if requested, or if the requested resource
         * belongs to one of the packages that are part of the Java EE platform
         */
        if (isResourceDelegate(name)) {
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "  Delegating to parent classloader {0}", parent);
            }
            ClassLoader loader = parent;
            if (loader == null) {
                loader = system;
            }
            stream = loader.getResourceAsStream(name);
            if (stream != null) {
                // FIXME - cache???
                if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, "  --> Returning stream from parent");
                }
            return stream;
            }
        }

        // (2) Search local repositories
        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "  Searching local repositories");
        }
        URL url = findResource(name);
        if (url != null) {
            // FIXME - cache???
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "  --> Returning stream from local");
            }
            stream = findLoadedResource(name);
            try {
                if (hasExternalRepositories && (stream == null)) {
                    stream = url.openStream();
                }
            } catch (IOException e) {
                logger.log(Level.FINEST, null, e);
            }
            if (stream != null) {
                return (stream);
            }
        }

        // (3) Delegate to parent unconditionally
        if (!delegate) {
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "  Delegating to parent classloader unconditionally {0}", parent);
            }
            ClassLoader loader = parent;
            if (loader == null) {
                loader = system;
            }
            stream = loader.getResourceAsStream(name);
            if (stream != null) {
                // FIXME - cache???
                if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, "  --> Returning stream from parent");
                }
            return stream;
            }
        }

        // (4) Resource was not found
        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "  --> Resource not found, returning null");
        }
        return null;

    }


    /**
     * Finds all the resources with the given name.
     */
    @Override
    public Enumeration<URL> getResources(String name) throws IOException {

	final Enumeration[] enums = new Enumeration[2];

        if (name.startsWith(META_INF_SERVICES)) {
           if (application.isWhitelistEnabled()) {
               if (!DOLUtils.isWhiteListed(application, name)) {
                   return new Enumeration<URL>() {
                       @Override
                       public boolean hasMoreElements() {
                           return false;
                       }

                       @Override
                       public URL nextElement() {
                           throw new NoSuchElementException(String.format("Resource: %s is not whitelisted", name));
                       }
                   };
               }
           }
        }

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

            @Override
            public boolean hasMoreElements() {
                return next();
            }

            @Override
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

        return loadClass(name, false);
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
     * @return class
     *
     * @exception ClassNotFoundException if the class was not found
     */
    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve)
        throws ClassNotFoundException {

        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "loadClass({0})", name);
        }

        Class<?> clazz = null;

        // Don't load classes if class loader is stopped
        if (!started) {
            throw new IllegalStateException(
                getString(LogFacade.NOT_STARTED, name));
        }

        // (0) Check our previously loaded local class cache
        clazz = findLoadedClass0(name);
        if (clazz != null) {
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "  Returning class from cache");
            }
            if (resolve) {
                resolveClass(clazz);
            }
            return clazz;
        }

        // (0.1) Check our previously loaded class cache
        clazz = findLoadedClass(name);
        if (clazz != null) {
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "  Returning class from cache");
            }
            if (resolve) {
                resolveClass(clazz);
            }
            return clazz;
        }

        // (0.5) Permission to access this class when using a SecurityManager
        if ( securityManager != null && packageDefinitionEnabled) {
            int i = name.lastIndexOf('.');
            if (i >= 0) {
                try {
                    securityManager.checkPackageAccess(name.substring(0,i));
                } catch (SecurityException se) {
                    String error = getString(LogFacade.SECURITY_EXCEPTION, name);
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

        boolean isWhitelisted = application.isWhitelistEnabled() && DOLUtils.isWhiteListed(application, name);
        boolean delegateLoad = (delegate && (application.isWhitelistEnabled() ? isWhitelisted : true)) || filter(name);

        // (1) Delegate to our parent if requested
        if (delegateLoad) {
            // Check delegate first
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "  Delegating to classloader1 {0}", delegateLoader);
            }
            try {
                clazz = delegateLoader.loadClass(name);
                if (clazz != null) {
                    if (logger.isLoggable(Level.FINER)) {
                        logger.log(Level.FINER, "  Loading class from delegate");
                    }
                    if (resolve) {
                        resolveClass(clazz);
                    }
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
                if (resolve) {
                    resolveClass(clazz);
                }
                return clazz;
            }
        } catch (ClassNotFoundException e) {
            // Ignore
        }

        // If we haven't found it locally, and we're using bundled JSF, DON'T delegate any lookup if it's a JSF class
        if (useMyFaces && !delegateLoad && (name.startsWith("javax.faces") || name.startsWith("jakarta.faces") || name.startsWith("com.sun.faces"))) {
            throw new ClassNotFoundException(String.format("Class [%s] could not be found in bundled JSF", name));
        }

        // (3) Delegate if class was not found locally
        if ((application.isWhitelistEnabled()? isWhitelisted : true) && !delegateLoad) {
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "  Delegating to classloader {0}", delegateLoader);
            }
            try {
                clazz = delegateLoader.loadClass(name);
                if (clazz != null) {
                    if (logger.isLoggable(Level.FINER)) {
                        logger.log(Level.FINER, "  Loading class from delegate");
                    }
                    if (resolve) {
                        resolveClass(clazz);
                    }
                    return clazz;
                }
            } catch (ClassNotFoundException e) {
                // Ignore
            }
        }
        else if(application.isWhitelistEnabled() && !isWhitelisted) {
            throw new ClassNotFoundException(String.format("Whitelist enabled, but class [%s] is not whitelisted", name));
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

            Enumeration<Permission> permsa = spc.elements();
            while (permsa.hasMoreElements()) {
                Permission p = permsa.nextElement();
                pc.add(p);
            }

            for (Permission permission: permissionList){
                pc.add(permission);
            }

            //get the declared and EE perms
            PermissionCollection pc1 = permissionsHolder.getPermissions(codeSource, null);

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
        return pc;

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

        try {

            ArrayList<URL> urls = new ArrayList<>();
            for (int i = 0; i < length; i++) {
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

    private URL[] removeDuplicate(ArrayList<URL> urls) {
        Set<URL> h = new HashSet<>(urls);
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

    @Override
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
        CacheCleaner.clearCaches(this);

        synchronized(jarFilesLock) {
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
                close();
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

            HotSwapHelper.closeClassLoader(this);
        }
    }


    /**
     * Used to periodically signal to the classloader to release
     * JAR resources.
     */
    public void closeJARs(boolean force) {
        if (jarFiles.length > 0) {
            synchronized (jarFilesLock) {
                if (force || (System.currentTimeMillis() > (lastJarAccessed + 90000))) {
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

                    try {
                        // aggressively close parent jars

                        WeakHashMap<Closeable, Void> closeables;
                        Field closeField = URLClassLoader.class.getDeclaredField("closeables");
                        closeField.setAccessible(true);
                        closeables = (WeakHashMap<Closeable, Void>) closeField.get(this);
                        synchronized (closeables) {
                            Set<Closeable> keys = closeables.keySet();
                            for (Closeable c : keys) {
                                try {
                                    if (c instanceof JarFile) {
                                        c.close();
                                    }
                                } catch (IOException ioex) {
                                    //Do nothing
                                }
                            }
                            closeables.clear();
                        }

                    } catch (Exception ex) {
                        Logger.getLogger(WebappClassLoader.class.getName()).log(Level.SEVERE, null, ex);
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
    private void clearReferencesJdbc() {
        InputStream is = getResourceAsStream(
                "org/glassfish/web/loader/JdbcLeakPrevention.class");
        // We know roughly how big the class will be (~ 1K) so allow 2k as a
        // starting point
        byte[] classBytes = new byte[2048];
        int offset = 0;
        try {
            synchronized (this) {
                if (jdbcLeakPreventionResourceClass == null) {
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
                    jdbcLeakPreventionResourceClass =
                        defineClass("org.glassfish.web.loader.JdbcLeakPrevention",
                            classBytes, 0, offset, this.getClass().getProtectionDomain());
                } else {
                    logger.log(Level.FINE, getString(LogFacade.LEAK_PREVENTION_JDBC_REUSE, contextName));
                }
            }
            Object obj = jdbcLeakPreventionResourceClass.newInstance();
            @SuppressWarnings("unchecked") // clearJdbcDriverRegistrations() returns List<String>
            List<String> driverNames = (List<String>) obj.getClass().getMethod(
                    "clearJdbcDriverRegistrations").invoke(obj);
            String msg = rb.getString(LogFacade.CLEAR_JDBC);
            for (String name : driverNames) {
                logger.warning(MessageFormat.format(msg, contextName, name));
            }
        } catch (Exception e) {
            // So many things to go wrong above...
            Throwable t = ExceptionUtils.unwrapInvocationTargetException(e);
            ExceptionUtils.handleThrowable(t);
            logger.log(Level.WARNING,
                    getString(LogFacade.JDBC_REMOVE_FAILED, contextName), t);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ioe) {
                    logger.log(Level.WARNING,
                            getString(LogFacade.JDBC_REMOVE_STREAM_ERROR, contextName), ioe);
                }
            }
        }
    }


    private void clearReferencesStaticFinal() {

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
                    for (Field field : fields) {
                        if (Modifier.isStatic(field.getModifiers())) {
                            field.get(null);
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
                    for (Field field : fields) {
                        int mods = field.getModifiers();
                        if (field.getType().isPrimitive()
                                || (field.getName().indexOf('$') != -1)) {
                            continue;
                        }
                        if (Modifier.isStatic(mods)) {
                            try {
                                setAccessible(field);
                                if (Modifier.isFinal(mods)) {
                                    if (!((field.getType().getName().startsWith("java."))
                                            || (field.getType().getName().startsWith("javax."))
                                            || (field.getType().getName().startsWith("jakarta.")))) {
                                        nullInstance(field.get(null));
                                    }
                                } else {
                                    field.set(null, null);
                                    if (logger.isLoggable(Level.FINE)) {
                                        logger.log(Level.FINE, "Set field {0} to null in class {1}", new Object[]{field.getName(), clazz.getName()});
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
        for (Field field : fields) {
            int mods = field.getModifiers();
            if (field.getType().isPrimitive()
                    || (field.getName().indexOf('$') != -1)) {
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
                                logger.log(Level.FINE, "Not setting field {0} to null in object of class {1} because the referenced object was of type {2} "
                                        + "which was not loaded by this WebappClassLoader.",
                                        new Object[]{field.getName(), instance.getClass().getName(), valueClass.getName()});
                            }
                        } else {
                            field.set(instance, null);
                            if (logger.isLoggable(Level.FINE)) {
                                logger.log(Level.FINE, "Set field {0} to null in class {1}", new Object[]{field.getName(), instance.getClass().getName()});
                            }
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

            for (Thread thread : threads) {
                Object threadLocalMap;
                if (thread != null) {
                    // Clear the first map
                    threadLocalMap = threadLocalsField.get(thread);
                    if (null != threadLocalMap) {
                        expungeStaleEntriesMethod.invoke(threadLocalMap);
                        checkThreadLocalMapForLeaks(threadLocalMap, tableField);
                    }
                    // Clear the second map
                    threadLocalMap = inheritableThreadLocalsField.get(thread);
                    if (null != threadLocalMap) {
                        expungeStaleEntriesMethod.invoke(threadLocalMap);
                        checkThreadLocalMapForLeaks(threadLocalMap, tableField);
                    }
                }
            }
        } catch (SecurityException | NoSuchFieldException | ClassNotFoundException | IllegalArgumentException | IllegalAccessException
                | InvocationTargetException | NoSuchMethodException e) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING,
                        getString(LogFacade.CHECK_THREAD_LOCALS_FOR_LEAKS_FAIL,
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
                                            LogFacade.CHECK_THREAD_LOCALS_FOR_LEAKS_BAD_KEY,
                                            args[1]), e);
                                    args[2] = getString(
                                            LogFacade.CHECK_THREAD_LOCALS_FOR_LEAKS_UNKNOWN);
                                }
                            }
                            if (value != null) {
                                args[3] = getPrettyClassName(value.getClass());
                                try {
                                    args[4] = value.toString();
                                } catch (Exception e) {
                                    logger.log(Level.SEVERE, getString(
                                            LogFacade.CHECK_THREAD_LOCALS_FOR_LEAKS_BAD_VALUE,
                                            args[3]), e);
                                    args[4] = getString(
                                            LogFacade.CHECK_THREAD_LOCALS_FOR_LEAKS_UNKNOWN);
                                }
                            }
                            if (value == null) {
                                if (logger.isLoggable(Level.FINE)) {
                                    logger.log(Level.FINE, getString(
                                            LogFacade.CHECK_THREAD_LOCALS_FOR_LEAKS_DEBUG,
                                            args));
                                }
                            } else {
                                logger.log(Level.SEVERE, getString(
                                        LogFacade.CHECK_THREAD_LOCALS_FOR_LEAKS,
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
        if (name==null) {
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
                        getString(LogFacade.CLEAR_RMI_INFO,
                        contextName), e);
            }
        } catch (SecurityException | NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING,
                        getString(LogFacade.CLEAR_RMI_FAIL,
                        contextName), e);
            }
        }
    }

    /**
     * Clear the {@link ResourceBundle} cache of any bundles loaded by this
     * class loader or any class loader where this loader is a parent class
     * loader. Whilst {@link ResourceBundle#clearCache()} could be used there
     * are complications around the
     * {@link org.glassfish.wasp.servlet.JasperLoader} that mean a reflection
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
                //In case of JDK 9, java.logging loading  sun.util.logging.resources.logging resource bundle and
                // java.logging module is used as the cache key with null class loader.So we are adding a null check
                if (loaderRef!=null) {
                  ClassLoader loader = (ClassLoader) loaderRef.get();

                  while (loader != null && loader != this) {
                    loader = loader.getParent();
                  }

                  if (loader != null) {
                    keysIter.remove();
                    countRemoved++;
                  }

                }

            }

            if (countRemoved > 0 && logger.isLoggable(Level.FINE)) {
                logger.fine(getString(LogFacade.CLEAR_REFERENCES_RESOURCE_BUNDLES_COUNT, countRemoved, contextName));
            }
        } catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
            logger.log(Level.SEVERE, getString(
                    LogFacade.CLEAR_REFERENCES_RESOURCE_BUNDLES_FAIL,
                    contextName), e);
        } catch (NoSuchFieldException e) {
            String msg = getString(
                    LogFacade.CLEAR_REFERENCES_RESOURCE_BUNDLES_FAIL, contextName);
            if (System.getProperty("java.vendor").startsWith("Sun")) {
                logger.log(Level.SEVERE, msg, e);
            } else if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, msg, e);
            }
        }
    }

    // ------------------------------------------------------ Protected Methods


    /**
     * Used to periodically signal to the classloader to release JAR resources.
     */
    protected boolean openJARs() {
        if (started && (jarFiles.length > 0)) {
            synchronized (jarFilesLock) {
                lastJarAccessed = System.currentTimeMillis();
                if (jarFiles[0] == null) {
                    for (int i = 0; i < jarFiles.length; i++) {
                        try {
                            jarFiles[i] = newJarFile(jarRealFiles[i]);
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
        }
        return true;
    }


    /**
     * Find specified class in local repositories.
     *
     * @param name
     * @return the loaded class, or null if the class isn't found
     * @throws java.lang.ClassNotFoundException
     */
    protected ResourceEntry findClassInternal(String name)
        throws ClassNotFoundException {

        if (!validate(name)) {
            throw new ClassNotFoundException(name);
        }

        String tempPath = name.replace('.', '/');
        String classPath = tempPath + ".class";

        ResourceEntry entry = findResourceInternal(name, classPath);

        if (entry == null) {
            throw new ClassNotFoundException(name);
        }

        synchronized (this) {
            Class<?> clazz = entry.loadedClass;
            if (clazz != null) {
                return entry;
            }

            if (entry.binaryContent == null) {
                throw new ClassNotFoundException(name);
            }
        }

        // Looking up the package
        definePackage(name, entry);

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
        if (!started) {
                throw new IllegalStateException(
                getString(LogFacade.NOT_STARTED, name));
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

        entry = findResourceInternalFromRepositories(name, path);

        if (entry == null) {
            synchronized (jarFilesLock) {
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

        if (!openJARs()) {
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
                if (antiJARLocking && !(path.endsWith(".class"))) {
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
        if (!antiJARLocking || resourcesExtracted) {
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
                                LogFacade.ILLEGAL_JAR_PATH, jarEntry2.getName()));
                    }
                } catch (IOException ioe) {
                    throw new IllegalArgumentException(getString(
                            LogFacade.VALIDATION_ERROR_JAR_PATH, jarEntry2.getName(), ioe));
                }
                if (!FileUtils.mkdirsMaybe(resourceFile.getParentFile())) {
                    logger.log(Level.WARNING,
                            LogFacade.UNABLE_TO_CREATE,
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
                if (n <= 0) {
                    break;
                }
                pos += n;
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, getString(LogFacade.READ_CLASS_ERROR, name), e);
            return;
        } finally {
            try {
                binaryStream.close();
            } catch(IOException e) {
                //Do nothing
            }
        }

        // START OF IASRI 4709374
        // Preprocess the loaded byte code if bytecode preprocesser is
        // enabled
        if (PreprocessorUtil.isPreprocessorEnabled()) {
            binaryContent = PreprocessorUtil.processClass(name, binaryContent);
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
            if (entry.binaryContent != null) {
                return new ByteArrayInputStream(entry.binaryContent);
            }
        }
        return null;

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
        return null;  // FIXME - findLoadedResource()

    }


    /**
     * Refresh the system policy file, to pick up eventual changes.
     */
    protected void refreshPolicy() {

        try {
            // The policy file may have been modified to adjust
            // permissions, so we're reloading it when loading or
            // reloading a Context
            Policy policy = AuthorizationService.getPolicy();
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

        if (name == null) {
            return false;
        }

        // START PE 4985680
        // Special case for performance reason.
        if (name.startsWith("java.")) {
            return true;
        }
        // END PE 4985680

        // Looking up the package
        String packageName = null;
        int pos = name.lastIndexOf('.');
        if (pos != -1) {
            packageName = name.substring(0, pos);
        } else {
            return false;
        }

        if (overridablePackages != null) {
            for (String overridePkg : overridablePackages) {
                if (packageName.startsWith(overridePkg)) {
                    return false;
                }
            }
        }

        for (int i = 0; i < packageTriggers.length; i++) {
            if (packageName.startsWith(packageTriggers[i])) {
                return true;
            }
        }

        return false;
    }


    /**
     * Validate a classname. As per SRV.9.7.2, we must restrict loading of
     * classes from J2SE (java.*) and classes of the servlet API
     * (jakarta.servlet.*). That should enhance robustness and prevent a number
     * of user error (where an older version of servlet.jar would be present
     * in /WEB-INF/lib).
     *
     * @param name class name
     * @return true if the name is valid
     */
    protected boolean validate(String name) {

        if (name == null) {
            return false;
        }
        return !name.startsWith("java.");
    }


    /**
     * Get URL.
     */
    protected URL getURL(File file) throws MalformedURLException {

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
        for (String file1 : files) {
            File file = new File(dir, file1);
            if (file.isDirectory()) {
                deleteDir(file);
            } else {
                if (!FileUtils.deleteFileMaybe(file)) {
                    logger.log(Level.WARNING,
                            LogFacade.UNABLE_TO_DELETE,
                            file.toString());
                }

            }
        }
        if (!FileUtils.deleteFileMaybe(dir)) {
            logger.log(Level.WARNING,
                    LogFacade.UNABLE_TO_DELETE,
                    dir.toString());
        }

    }

    // START SJSAS 6344989
    public void addByteCodePreprocessor(BytecodePreprocessor preprocessor) {
        byteCodePreprocessors.add(preprocessor);
    }
    // END SJSAS 6344989

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
    @Override
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
    @Override
     public void addTransformer(final ClassFileTransformer transformer) {
        final WebappClassLoader cl = this;
        addByteCodePreprocessor(new BytecodePreprocessor(){
                /*
                 * This class adapts ClassFileTransformer to ByteCodePreprocessor that
                 * is used inside WebappClassLoader.
                 */
                @Override
                public boolean initialize(Hashtable parameters) {
                    return true;
                }

                @Override
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

    private void definePackage(String name, ResourceEntry entry) {
        // Looking up the package
        String packageName = null;
        int pos = name.lastIndexOf('.');
        if (pos != -1) {
            packageName = name.substring(0, pos);
        }

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
                boolean sealCheck;
                if (pkg.isSealed()) {
                    sealCheck = pkg.isSealed(entry.codeBase);
                } else {
                    sealCheck = (entry.manifest == null)
                            || !isPackageSealed(packageName, entry.manifest);
                }
                if (!sealCheck) {
                    throw new SecurityException("Sealing violation loading " + name + " : Package "
                            + packageName + " is sealed.");
                }
            }

        }

    }

    private synchronized Class<?> defineLoadedClass(String name, ResourceEntry entry) {
        Class<?> clazz;
        // Create the code source object
        CodeSource codeSource
                = new CodeSource(entry.codeBase, entry.certificates);
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

        // If class already created in previous classloader then skip the byteCodePreprocessors
        if (!byteCodePreprocessors.isEmpty() && entry.loadedClass == null) {

            // ByteCodePreprpcessor expects name as
            // java/lang/Object.class
            String resourceName
                    = name.replace('.', '/') + ".class";
            for (BytecodePreprocessor preprocessor : byteCodePreprocessors) {
                binaryContent = preprocessor.preprocess(
                        resourceName, binaryContent);
            }
        }

        clazz = defineClass(name, binaryContent, 0,
                binaryContent.length,
                codeSource);

        // END GlassFish [680]
        entry.loadedClass = clazz;
        if (!hotDeploy) {
            entry.binaryContent = null;
            entry.source = null;
            entry.codeBase = null;
            entry.manifest = null;
            entry.certificates = null;
        }
        return clazz;
    }

     private String getJavaVersion() {

        String version;

        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            version = AccessController.doPrivileged(
                    new PrivilegedAction<String>() {
                        @Override
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
                    @Override
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
                || ((name.startsWith("javax") || name.startsWith("jakarta")) &&
                    (!name.startsWith("jakarta.faces") || !useMyFaces))
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

    private static JarFile newJarFile(final File file) throws IOException {

        JarFile jarFile = new JarFile(file);
        if (!isMultiReleaseJar) {
            return jarFile;
        }

        if (isMultiReleaseJar(jarFile)) {
            return newInstance(JarFile.class, CONSTRUCTOR_ARGS_TYPES,
                    new Object[]{file, true, ZipFile.OPEN_READ, CONSTRUCTOR_ARGUMENTS});
        }

        return jarFile;
    }

    public static <T> T newInstance(Class<T> ofClass, Class<?>[] constructorArgTypes, Object[] args) {
        try {
            Constructor<T> constructor = ofClass.getConstructor(constructorArgTypes);
            return constructor.newInstance(args);
        } catch (Exception ex) {
            return null;
        }
    }

    private static boolean isMultiReleaseJar(JarFile jarFile) {
        try {
            Manifest manifest = jarFile.getManifest();
            if (manifest == null) {
                return false;
            } else {
                return manifest.getMainAttributes().containsKey(MULTI_RELEASE);
            }
        } catch (IOException ex) {
            return false;
        }
    }
}
