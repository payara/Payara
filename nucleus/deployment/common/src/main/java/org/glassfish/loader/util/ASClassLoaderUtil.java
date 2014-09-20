/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.loader.util;

import com.sun.enterprise.module.Module;
import com.sun.enterprise.module.ModulesRegistry;
import com.sun.enterprise.util.io.FileUtils;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.ClassLoaderHierarchy;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.api.admin.ServerEnvironment;
import com.sun.enterprise.deployment.deploy.shared.Util;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.jar.Attributes;
import java.util.*;

import org.glassfish.logging.annotation.LogMessageInfo;

public class ASClassLoaderUtil {

    public static final Logger deplLogger = org.glassfish.deployment.common.DeploymentContextImpl.deplLogger;

    @LogMessageInfo(message = "Cannot convert classpath to URL {0}", level="WARNING")
    private static final String CLASSPATH_ERROR = "NCLS-DEPLOYMENT-00045";

    @LogMessageInfo(message = "Exception:  {0}", level="WARNING")
    private static final String EXCEPTION = "NCLS-DEPLOYMENT-00017";

    @LogMessageInfo(message = "unexpected error in getting urls", level="WARNING")
    private static final String UNEXPECTED_EXCEPTION = "NCLS-DEPLOYMENT-00018";

    private static String modulesClassPath = null;

    /** The manifest file name from an archive. */
    private static final String MANIFEST_ENTRY  =
                    "META-INF" + File.separator + "MANIFEST.MF";

    /**
     * Gets the classpath associated with a module, suffixing libraries
     * defined [if any] for the application
     *
     * @param habitat the habitat the application resides in.
     * @param moduleId Module id of the module
     * @param deploymentLibs libraries option passed through deployment
     * @return A <code>File.pathSeparator</code> separated list of classpaths
     *         for the passed in module, including the module specified
     *         "libraries" defined for the module.
     */
    public static String getModuleClassPath
        (ServiceLocator habitat, String moduleId, String deploymentLibs) {

        if (deplLogger.isLoggable(Level.FINE)) {
          deplLogger.log(Level.FINE,
                         "ASClassLoaderUtil.getModuleClassPath " + "for module Id : " + moduleId);
        }

        StringBuilder classpath = new StringBuilder(getModulesClasspath(habitat));
        ClassLoaderHierarchy clh =
                habitat.getService(ClassLoaderHierarchy.class);
        final String commonClassPath = clh.getCommonClassPath();
        if (commonClassPath != null && commonClassPath.length() > 0) {
            classpath.append(commonClassPath).append(File.pathSeparator);
        }
        addDeployParamLibrariesForModule(classpath, moduleId, deploymentLibs, habitat);
        if (deplLogger.isLoggable(Level.FINE)) {
            deplLogger.log(Level.FINE,
                           "Final classpath: " + classpath.toString());
        }
        return classpath.toString();

    }

    public static String getModuleClassPath (ServiceLocator habitat, 
        DeploymentContext context) {
        DeployCommandParameters params = 
            context.getCommandParameters(DeployCommandParameters.class);
        return getModuleClassPath(habitat, params.name(), params.libraries());
    }


    private static void addDeployParamLibrariesForModule(StringBuilder sb, 
        String moduleId, String deploymentLibs, ServiceLocator habitat) {
        if (moduleId.indexOf("#") != -1) {
            moduleId = moduleId.substring(0, moduleId.indexOf("#"));
        }
  
        if (deploymentLibs == null) {
            ApplicationInfo appInfo = 
                habitat.<ApplicationRegistry>getService(ApplicationRegistry.class).get(moduleId);
            if (appInfo == null) {
                // this might be an internal container app, 
                // like _default_web_app, ignore.
                return;
            }
            deploymentLibs = appInfo.getLibraries();
        }
        final URL[] libs = 
            getDeployParamLibrariesAsURLs(deploymentLibs, habitat);
        if (libs != null) {
            for (final URL u : libs) {
                sb.append(u.getPath());
                sb.append(File.pathSeparator);                    
            }
        }
    }

    private static URL[] getDeployParamLibrariesAsURLs(String librariesStr,
        ServiceLocator habitat) {
            return getDeployParamLibrariesAsURLs(librariesStr,
                habitat.<ServerEnvironment>getService(ServerEnvironment.class));
    }


    /**
     * converts libraries specified via EXTENSION_LIST entry in MANIFEST.MF of
     * all of the libraries of the deployed archive to
     * The libraries  are made available to 
     * the application in the order specified.
     *
     * @param librariesStr is a comma-separated list of library JAR files
     * @param env the server environment
     * @return array of URL
     */
    public static URL[] getLibrariesAsURLs(Set<String> librariesStr,
        ServerEnvironment env) {
        if(librariesStr == null)
            return null;
        final URL [] urls = new URL[librariesStr.size()];
        String [] librariesStrArray  = new String[librariesStr.size()];
        librariesStrArray  = librariesStr.toArray(librariesStrArray);
        return getDeployParamLibrariesAsURLs(env, librariesStrArray, urls);
    }

    /**
     * converts libraries specified via the --libraries deployment option to
     * URL[].  The library JAR files are specified by either relative or
     * absolute paths.  The relative path is relative to 
     * instance-root/lib/applibs. The libraries  are made available to 
     * the application in the order specified.
     *
     * @param librariesStr is a comma-separated list of library JAR files
     * @param env the server environment
     * @return array of URL
     */
    public static URL[] getDeployParamLibrariesAsURLs(String librariesStr,
        ServerEnvironment env) {
        if(librariesStr == null)
            return null;
        String [] librariesStrArray = librariesStr.split(",");
        final URL [] urls = new URL[librariesStrArray.length];
        return getDeployParamLibrariesAsURLs(env, librariesStrArray, urls);
    }

    private static URL[] getDeployParamLibrariesAsURLs(ServerEnvironment env, String[] librariesStrArray,
                                                       URL[] urls) {
        final String appLibsDir = env.getLibPath()
                + File.separator + "applibs";

        int i=0;
        for(final String libraryStr:librariesStrArray){
            try {
                File f = new File(libraryStr);
                if(!f.isAbsolute())
                    f = new File(appLibsDir, libraryStr);
                URL url =f.toURI().toURL();
                urls[i++] = url;
            } catch (MalformedURLException malEx) {
                deplLogger.log(Level.WARNING,
                               CLASSPATH_ERROR,
                               libraryStr);
                LogRecord lr = new LogRecord(Level.WARNING, EXCEPTION);
                lr.setParameters(new Object[] { malEx.getMessage() });
                lr.setThrown(malEx);
                deplLogger.log(lr);
            }
        }
        return urls;
    }

    private static synchronized String getModulesClasspath(ServiceLocator habitat) {
        synchronized (ASClassLoaderUtil.class) {
            if (modulesClassPath == null) {
                final StringBuilder tmpString = new StringBuilder();
                ModulesRegistry mr = habitat.getService(ModulesRegistry.class);
                if (mr != null) {
                    for (Module module : mr.getModules()) {
                        for (URI uri : module.getModuleDefinition().getLocations()) {
                            tmpString.append(uri.getPath());
                            tmpString.append(File.pathSeparator);
                        }
                    }
                }

                //set shared classpath for module so that it doesn't need to be 
                //recomputed for every other invocation
                modulesClassPath = tmpString.toString();
            }
        }
        return modulesClassPath;
    }

    /**
     * Returns an array of urls that contains ..
     * <pre>
     *    i.   all the valid directories from the given directory (dirs) array
     *    ii.  all jar files from the given directory (jarDirs) array
     *    iii. all zip files from the given directory (jarDirs) array if
     *         not ignoring zip file (ignoreZip is false).
     * </pre>
     *
     * @param    dirs     array of directory path names
     * @param    jarDirs  array of path name to directories that contains
     *                    JAR & ZIP files.
     * @param    ignoreZip whether to ignore zip files
     * @return   an array of urls that contains all the valid dirs,
     *           *.jar & *.zip
     *
     * @throws  IOException  if an i/o error while constructing the urls
     */
    public static URL[] getURLs(File[] dirs, File[] jarDirs,
        boolean ignoreZip) throws IOException {
        return convertURLListToArray(getURLsAsList(dirs, jarDirs, ignoreZip));
    }

    /**
     * Returns a list of urls that contains ..
     * <pre>
     *    i.   all the valid directories from the given directory (dirs) array
     *    ii.  all jar files from the given directory (jarDirs) array
     *    iii. all zip files from the given directory (jarDirs) array if
     *         not ignoring zip file (ignoreZip is false).
     * </pre>
     *
     * @param    dirs     array of directory path names
     * @param    jarDirs  array of path name to directories that contains
     *                    JAR & ZIP files.
     * @param    ignoreZip whether to ignore zip files
     * @return   an array of urls that contains all the valid dirs,
     *           *.jar & *.zip
     *
     * @throws  IOException  if an i/o error while constructing the urls
     */
    public static List<URL> getURLsAsList(File[] dirs, File[] jarDirs,
        boolean ignoreZip) throws IOException {
        List<URL> list = new ArrayList<URL>();

        // adds all directories
        if (dirs != null) {
            for (int i=0; i<dirs.length; i++) {
                File dir = dirs[i];
                if (dir.isDirectory() || dir.canRead()) {
                    URL url = dir.toURI().toURL();
                    list.add(url);

                    if (deplLogger.isLoggable(Level.FINE)) {
                       deplLogger.log(Level.FINE,
                                      "Adding directory to class path:" + url.toString());
                    }
                }
            }
        }

        // adds all the jars
        if (jarDirs != null) {
            for (int i=0; i<jarDirs.length; i++) {
                File jarDir =  jarDirs[i];

                if (jarDir.isDirectory() || jarDir.canRead()) {
                    File[] files = jarDir.listFiles();

                    for (int j=0; j<files.length; j++) {
                        File jar = files[j];

                        if ( FileUtils.isJar(jar) ||
                            (!ignoreZip && FileUtils.isZip(jar)) ) {
                            list.add(jar.toURI().toURL());

                            if (deplLogger.isLoggable(Level.FINE)) {
                                deplLogger.log(Level.FINE,
                                               "Adding jar to class path:" + jar.toURL());
                            }
                        }
                    }
                }
            }
        }
        return list;
    }
  
    public static URL[] convertURLListToArray(List<URL> list) {
        // converts the list to an array
        URL[] urls = new URL[0];
        if (list != null && list.size() > 0) {
            urls = new URL[list.size()];
            urls = (URL[]) list.toArray(urls);
        }
        return urls;
    }

    /**
     * get URL list from classpath
     * @param classpath classpath string containing the classpaths
     * @param delimiter delimiter to separate the classpath components 
     *                  in the classpath string
     * @param rootPath root path of the classpath if the paths are relative
     
     * @return urlList URL list from the given classpath
     */
    public static List<URL> getURLsFromClasspath(String classpath, 
        String delimiter, String rootPath) {
        final List<URL> urls  = new ArrayList<URL>();

        if (classpath == null || classpath.length() == 0) {
            return urls;
        }

        // tokenize classpath
        final StringTokenizer st = new StringTokenizer(classpath, delimiter);
        while (st.hasMoreTokens()) {
            try {
                String path = st.nextToken();
                try {
                    // try to see if the path is absolute
                    URL url = new URL(path);
                    URI uri = url.toURI();
                    if (uri.isAbsolute()) {
                        urls.add(uri.toURL());
                        continue;
                    }
                } catch (Exception ie) {
                    // ignore
                } 

                if (rootPath != null && rootPath.length() != 0) {
                    path = rootPath + File.separator + path;
                }
                File f = new File(path);
                urls.add(f.toURI().toURL());
            } catch(Exception e) {
                  deplLogger.log(Level.WARNING,
                                 UNEXPECTED_EXCEPTION,
                                 e);
            }
        }
        return urls;
    }

   /**
     * Returns the manifest file for the given root path.
     *
     * <xmp>
     *    Example:
     *     |--repository/
     *     |   |--applications/
     *     |        |--converter/
     *     |            |--ejb-jar-ic_jar/        <---- rootPath
     *     |                 |--META-INF/
     *     |                     |--MANIFEST.MF
     * </xmp>
     *
     * @param    rootPath    absolute path to the module
     *
     * @return   the manifest file for the given module
     */
    public static Manifest getManifest(String rootPath) {

        InputStream in  = null;
        Manifest mf     = null;

        // gets the input stream to the MANIFEST.MF file
        try {
            in = new FileInputStream(rootPath+File.separator+MANIFEST_ENTRY);

            if (in != null) {
                mf = new Manifest(in);
            }
        } catch (IOException ioe) {
            // ignore
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ioe) {
                    // Ignore
                }
            }
        }
        return mf;
    }


    /**
     * Returns the class path (if any) from the given manifest file as an 
     * URL list.
     *
     * @param    manifest    manifest file of an archive
     * @param    rootPath    root path to the module
     *
     * @return   a list of URLs 
     *           an empty list if given manifest is null
     */
    public static List<URL> getManifestClassPathAsURLs(Manifest manifest,
            String rootPath) {
        List<URL> urlList = new ArrayList<URL>();
        if (manifest != null) {
            Attributes mainAttributes  = manifest.getMainAttributes();

            for (Map.Entry entry : mainAttributes.entrySet()) {

                Attributes.Name next = (Attributes.Name) entry.getKey();

                if (next.equals(Attributes.Name.CLASS_PATH)) {
                    String classpathString = (String) entry.getValue();
                    urlList = getURLsFromClasspath(classpathString, " ", 
                        rootPath);
                }
            }
        }
        return urlList;
    }

    /**
     * add all the libraries packaged in the application library directory
     *
     * @param appRoot the application root
     * @param appLibDir the Application library directory
     * @param compatibilityProp the version of the release that we need to
     *        maintain backward compatibility 
     * @return an array of URL
     */
    public static URL[] getAppLibDirLibraries(File appRoot, String appLibDir, 
        String compatibilityProp) 
        throws IOException {
        return convertURLListToArray(
            getAppLibDirLibrariesAsList(appRoot, appLibDir, compatibilityProp));
    }

    public static List<URL> getAppLibDirLibrariesAsList(File appRoot, String appLibDir, String compatibilityProp)
        throws IOException {
        URL[] libDirLibraries = new URL[0];
        // get all the app lib dir libraries
        if (appLibDir != null) {
            String libPath = appLibDir.replace('/', File.separatorChar);
            libDirLibraries =  getURLs(null,
                new File[] {new File(appRoot, libPath)}, true);
        }

        List<URL> allLibDirLibraries = new ArrayList<URL>();
        for (URL url : libDirLibraries) {
            allLibDirLibraries.add(url);
        }

        // if the compatibility property is set to "v2", we should add all the 
        // jars under the application root to maintain backward compatibility 
        // of v2 jar visibility
        if (compatibilityProp != null && compatibilityProp.equals("v2")) {
            List<URL> appRootLibraries = getURLsAsList(null, new File[] {appRoot}, true);
            allLibDirLibraries.addAll(appRootLibraries);
        }
        return allLibDirLibraries;
    }

    public static List<URI> getLibDirectoryJarURIs(File moduleLibDirectory) throws Exception {
        List<URI> libLibraryURIs = new ArrayList<URI>();
        File[] jarFiles = moduleLibDirectory.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return (pathname.getAbsolutePath().endsWith(".jar"));
            }
        });

        if (jarFiles != null && jarFiles.length > 0) {
            for (File jarFile : jarFiles) {
                libLibraryURIs.add(Util.toURI(jarFile.toURL()));
            }
        }

        return libLibraryURIs;
    }
}
