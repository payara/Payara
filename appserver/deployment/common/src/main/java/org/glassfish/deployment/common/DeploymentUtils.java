/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.deployment.common;

import com.sun.enterprise.config.serverbeans.Application;
import com.sun.enterprise.deploy.shared.ArchiveFactory;
import com.sun.enterprise.deploy.shared.FileArchive;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.logging.LogDomains;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.deployment.archive.ArchiveHandler;
import com.sun.enterprise.deployment.deploy.shared.Util;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.loader.util.ASClassLoaderUtil;
import org.jvnet.hk2.annotations.Service;

import java.io.File;
import java.io.IOException;
import java.io.FileFilter;
import java.util.Enumeration;
import java.util.Properties;
import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.Adler32;
import java.util.jar.Manifest;
import java.util.logging.Logger;
import java.util.logging.Level;



/** 
 * Utility methods for deployment. 
 */

public class DeploymentUtils {

    public static final String DEPLOYMENT_PROPERTY_JAVA_WEB_START_ENABLED = "java-web-start-enabled";
    
    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(DeploymentUtils.class);

    final private static Logger _logger = LogDomains.getLogger(DeploymentUtils.class, LogDomains.DPL_LOGGER);

    private static final String V2_COMPATIBILITY = "v2";

    private static final String WEB_INF = "WEB-INF";
    private static final String JSP_SUFFIX = ".jsp";
    private static final String RA_XML = "META-INF/ra.xml";
    private static final String RESOURCES_XML_META_INF = "META-INF/glassfish-resources.xml";
    private static final String RESOURCES_XML_WEB_INF = "WEB-INF/glassfish-resources.xml";
    private static final String APPLICATION_XML = "META-INF/application.xml";
    private static final String SUN_APPLICATION_XML = "META-INF/sun-application.xml";    
    private static final String GF_APPLICATION_XML = "META-INF/glassfish-application.xml";    
    private static final String EAR_EXTENSION = ".ear";
    private static final String WAR_EXTENSION = ".war";
    private static final String RAR_EXTENSION = ".rar";
    private static final String EXPANDED_WAR_SUFFIX = "_war";
    private static final String EXPANDED_RAR_SUFFIX = "_rar";
    private static final String EXPANDED_JAR_SUFFIX = "_jar";
    
    private static final String INSTANCE_ROOT_URI_PROPERTY_NAME = "com.sun.aas.instanceRootURI";

    private final static String DAS_TARGET_NAME = "server";
    public final static String DOMAIN_TARGET_NAME = "domain";

    private final static String DOWNLOADABLE_ARTIFACTS_KEY_PREFIX = "downloadable";
    private final static String GENERATED_ARTIFACTS_KEY_PREFIX = "generated";
    
    public static boolean isDASTarget(final String targetName) {
        return DAS_TARGET_NAME.equals(targetName);
    }

    public static boolean isDomainTarget(final String targetName) {
        return DOMAIN_TARGET_NAME.equals(targetName);
    }

    /**
     * Computes the checksum of the URIs of files contained in a directory.
     * 
     * @param directory the directory for which to compute a checksum
     * @return checksum calculated from URIs of files in the directory
     */
    public static long checksum(final File directory) {
        if ( ! directory.isDirectory()) {
            final String msg = localStrings.getLocalString(
                    "enterprise.deployment.remoteDirPathUnusable",
                    "The directory deployment path {0} is not a directory or is inaccessible",
                    directory.getAbsolutePath());
            throw new IllegalArgumentException(msg);
        }
        final List<URI> uris = new ArrayList<URI>();
        scanDirectory(directory.toURI(), directory, uris);
        /*
         * Sort the URIs.  File.listFiles does not guarantee any particular
         * ordering of the visited files, and for two checksums to match we
         * need to process the URIs in the same order.
         */
        Collections.sort(uris);

        final Adler32 checksum = new Adler32();
        for (URI uri : uris) {
            checksum.update(uri.toASCIIString().getBytes());
        }
        return checksum.getValue();
    }

    /**
     * Returns the downloadable artifacts object from the specified deployment
     * context, creating it there if it does not already exist.
     * @param dc the deployment context from which to fetch the downloadable Artifacts object
     * @return
     */
    public static Artifacts downloadableArtifacts(final DeploymentContext dc) {
        return Artifacts.get(dc, DOWNLOADABLE_ARTIFACTS_KEY_PREFIX);
    }

    /**
     * Returns the downloadable artifacts object derived from the properties
     * saved with the specified Application
     * @param app the Application config object with (possibly) properties describing downloadable artifacts
     * @return
     */
    public static Artifacts downloadableArtifacts(final Application app) {
        return Artifacts.get(app.getDeployProperties(), DOWNLOADABLE_ARTIFACTS_KEY_PREFIX);
    }

    /**
     * Returns the generated artifacts object from the specified deployment
     * context, creating it there if it does not already exist.
     * @param app
     * @return
     */
    public static Artifacts generatedArtifacts(final DeploymentContext dc) {
        return Artifacts.get(dc, GENERATED_ARTIFACTS_KEY_PREFIX);
    }
    
    /**
     * Returns the generated artifacts object derived from the properties
     * saved with the specified Application
     * @param app the Application config object with (possibly) properties describing generated artifacts
     * @return
     */
    public static Artifacts generatedArtifacts(final Application app) {
        return Artifacts.get(app.getDeployProperties(), GENERATED_ARTIFACTS_KEY_PREFIX);
    }

    private static void scanDirectory(final URI anchorDirURI,
            final File directory, final List<URI> uris) {
        for (File f : directory.listFiles()) {
            uris.add(anchorDirURI.relativize(f.toURI()));
            if (f.isDirectory()) {
                scanDirectory(anchorDirURI, f, uris);
            }
        }
    }
    
    // checking whether the archive is a web archive
    public static boolean isWebArchive(ReadableArchive archive) {
        try {
            if (Util.getURIName(archive.getURI()).endsWith(WAR_EXTENSION)) {
                return true;
            }

            if (archive.exists(WEB_INF)) { 
                return true;
            } 
            Enumeration<String> entries = archive.entries();
            while (entries.hasMoreElements()) {
                String entryName = entries.nextElement();
                if (entryName.endsWith(JSP_SUFFIX)) { 
                    return true;
                }
            }
            return false;
        } catch (IOException ioe) {
            // ignore
        }
        return false;
    }

   /**
     * @param pathName
     * @return the default value of the EE name. 
     * The default name is the pathname with any filename extension 
     * (.jar, .war, .rar) removed, but with any directory names included.
     */
    public static String getDefaultEEName(String pathName) {
        if (pathName == null) {
            return null;
        }

        pathName = pathName.replace('\\', '/');

        if (pathName.endsWith("/")) {
            pathName = pathName.substring(0, pathName.length() - 1);
        }
        if (pathName.lastIndexOf("/") != -1) {
            pathName = pathName.substring(pathName.lastIndexOf(
                "/") + 1);
        }
        if (pathName.endsWith(".jar") || pathName.endsWith(".war")
            || pathName.endsWith(".rar") || pathName.endsWith(".ear")) {
            return pathName.substring(0, pathName.length() - 4);
        } else {
            return pathName;
        }
    }

    public static boolean hasResourcesXML(ReadableArchive archive){
        boolean hasResourcesXML = false;
        try{

            if(isEAR(archive)){
                //handle top-level META-INF/glassfish-resources.xml
                if(archive.exists(RESOURCES_XML_META_INF)){
                    return true;
                }

                //check sub-module level META-INF/glassfish-resources.xml and WEB-INF/glassfish-resources.xml
                Enumeration<String> entries = archive.entries();
                while(entries.hasMoreElements()){
                    String element = entries.nextElement();
                    if(element.endsWith(".jar") || element.endsWith(".war") || element.endsWith(".rar") ||
                            element.endsWith("_jar") || element.endsWith("_war") || element.endsWith("_rar")){
                        ReadableArchive subArchive = archive.getSubArchive(element);
                        boolean answer = (subArchive != null && hasResourcesXML(subArchive));
                        if (subArchive != null) {
                            subArchive.close();
                        }
                        if (answer) {
                            return true;
                        }
                    }
                }
            }else{
                if(isWebArchive(archive)){
                    return archive.exists(RESOURCES_XML_WEB_INF);
                }else {
                    return archive.exists(RESOURCES_XML_META_INF);
                }
            }
        }catch(IOException ioe){
            //ignore
        }
        return hasResourcesXML;
    }
    /**
     * check whether the archive is a .rar
     * @param archive archive to be tested
     * @return status of .rar or not
     */
    public static boolean isRAR(ReadableArchive archive){
        boolean isRar = false;
        try{
            if (Util.getURIName(archive.getURI()).endsWith(RAR_EXTENSION)) {
                return true;
            }

            isRar = archive.exists(RA_XML);
        }catch(IOException ioe){
            //ignore
        }
        return isRar;
    }

    /**
     * check whether the archive is a .ear
     * @param archive archive to be tested
     * @return status of .ear or not
     */
    public static boolean isEAR(ReadableArchive archive){
        boolean isEar = false;

        try{
            if (Util.getURIName(archive.getURI()).endsWith(EAR_EXTENSION)) {
                return true;
            }

            isEar = archive.exists(APPLICATION_XML) || 
                    archive.exists(SUN_APPLICATION_XML) || 
                    archive.exists(GF_APPLICATION_XML);

            if (!isEar) {
                isEar = isEARFromIntrospecting(archive);
            } 
        }catch(IOException ioe){
            //ignore
        }
        return isEar;
    }

    // introspecting the sub archives to see if any of them 
    // ended with expected suffix
    private static boolean isEARFromIntrospecting(ReadableArchive archive) 
        throws IOException {
        for (String entryName : archive.getDirectories()) {
            // we don't have other choices but to look if any of
            // the subdirectories is ended with expected suffix
            if ( entryName.endsWith(EXPANDED_WAR_SUFFIX) || 
                 entryName.endsWith(EXPANDED_RAR_SUFFIX) || 
                 entryName.endsWith(EXPANDED_JAR_SUFFIX) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * This method returns the relative file path of an embedded module to 
     * the application root.
     * For example, if the module is expanded/located at 
     * $domain_dir/applications/j2ee-apps/foo/fooEJB_jar,
     * this method will return fooEJB_jar
     *
     *@param appRootPath The path of the application root which
     *                   contains the module 
     *                   e.g. $domain_dir/applications/j2ee-apps/foo
     *@param moduleUri The module uri
     *                 e.g. fooEJB.jar
     *@return The relative file path of the module to the application root
     */
    public static String getRelativeEmbeddedModulePath(String appRootPath,
        String moduleUri) {
        moduleUri = FileUtils.makeLegalNoBlankFileName(moduleUri);
        if (FileUtils.safeIsDirectory(new File(appRootPath, moduleUri))) {
            return moduleUri;
        } else {
            return FileUtils.makeFriendlyFilenameExtension(moduleUri);
        }
    }

    /**
     * This method returns the file path of an embedded module. 
     * For example, if the module is expanded/located at 
     * $domain_dir/applications/j2ee-apps/foo/fooEJB_jar,
     * this method will return 
     * $domain_dir/applications/j2ee-apps/foo/fooEJB_jar
     *
     *@param appRootPath The path of the application root which
     *                   contains the module 
     *                   e.g. $domain_dir/applications/j2ee-apps/foo
     *@param moduleUri The module uri
     *                 e.g. fooEJB.jar
     *@return The file path of the module
     */
    public static String getEmbeddedModulePath(String appRootPath,
        String moduleUri) {
        return appRootPath + File.separator + getRelativeEmbeddedModulePath(appRootPath, moduleUri) ;
    }

    public static boolean useV2Compatibility(DeploymentContext context) {
        return V2_COMPATIBILITY.equals(context.getAppProps().getProperty(DeploymentProperties.COMPATIBILITY));
    }

    public static String relativizeWithinDomainIfPossible(
            final URI absURI) throws URISyntaxException {
        URI instanceRootURI = new URI(System.getProperty(INSTANCE_ROOT_URI_PROPERTY_NAME));
        URI appURI = instanceRootURI.relativize(absURI);
        String appLocation = (appURI.isAbsolute()) ?
            appURI.toString() :
            "${" + INSTANCE_ROOT_URI_PROPERTY_NAME + "}/" + appURI.toString();
        return appLocation;
    }

    public static void validateApplicationName(String name) {
        if (name.indexOf('/') != -1) { 
            throw new IllegalArgumentException(localStrings.getLocalString("illegal_char_in_name", "Illegal character [{0}] in the name [{1}].", "/", name)); 
        } else if (name.indexOf('#') != -1) {
            throw new IllegalArgumentException(localStrings.getLocalString("illegal_char_in_name", "Illegal character [{0}] in the name [{1}].", "#", name)); 
        } else if (name.indexOf(';') != -1) {
            throw new IllegalArgumentException(localStrings.getLocalString("illegal_char_in_name", "Illegal character [{0}] in the name [{1}].", ";", name)); 
        }
        return;
    }

    public static String propertiesValue(final Properties props, final char sep) {
        final StringBuilder sb = new StringBuilder();
        String currentSep = "";
        for (Enumeration en = props.propertyNames(); en.hasMoreElements();) {
            final Object key = en.nextElement();
            final Object v = props.get(key);
            sb.append(currentSep).append(key.toString()).append("=").append(v.toString());
            currentSep = String.valueOf(sep);
        }
        return sb.toString();
    }

    public static List<URL> getManifestLibraries(DeploymentContext context) 
        throws IOException {
        return getManifestLibraries(context.getSource());
    }

    public static List<URL> getManifestLibraries(DeploymentContext context, 
        Manifest manifest) throws IOException {
        return getManifestLibraries(context.getSource(), manifest);
    }

    public static List<URL> getManifestLibraries(ReadableArchive archive) throws IOException {
        return getManifestLibraries(archive, archive.getManifest());
    }

    private static List<URL> getManifestLibraries(ReadableArchive archive, Manifest manifest) {
        String appRootPath = null;
        ReadableArchive parentArchive = archive.getParentArchive();

        if (parentArchive != null) {
            appRootPath = (new File(parentArchive.getURI())).getPath();
        } else {
            try {
                appRootPath = (new File(archive.getURI().getPath())).getParent();
            } catch (Exception e) {
                // ignore, this is the jar inside jar case 
            }
        }

        // add libraries referenced through manifest
        return ASClassLoaderUtil.getManifestClassPathAsURLs(
            manifest, appRootPath);
    }

    public static List<URI> getExternalLibraries(ReadableArchive archive) {
        List<URI> externalLibURIs = new ArrayList<URI>();
        try {
            List<URL> manifestURLs = getManifestLibraries(archive);
            URI archiveURI = archive.getURI();
            if (archive.getParentArchive() != null) {
                archiveURI = archive.getParentArchive().getURI();
            }
            for (URL manifestURL : manifestURLs) {
                URI manifestLibURI = archiveURI.relativize(manifestURL.toURI());
                if (manifestLibURI.isAbsolute()) {
                    File externalLib = new File(manifestLibURI); 
                    if (externalLib.exists()) {
                        externalLibURIs.add(manifestLibURI);
                    }
                }
            }
        } catch (Exception e) {
            Logger.getAnonymousLogger().log(Level.WARNING, e.getMessage(), e);
        }
        return externalLibURIs;
    }

    public static List<URL> getModuleLibraryJars(DeploymentContext context)
        throws Exception {
        ReadableArchive archive = context.getSource();
        List<URL> moduleLibraryURLs = new ArrayList<URL>();
        ArchiveHandler handler = context.getArchiveHandler();
        if (handler.getClass() == null ||
            handler.getClass().getAnnotation(Service.class) == null) {
            return moduleLibraryURLs;
        }
        String handlerName = handler.getClass().getAnnotation(Service.class).name();
        File archiveFile = new File(archive.getURI());
        if (handlerName.equals("war")) {
            // we should add all the WEB-INF/lib jars for web module
            File webInf = new File(archiveFile, "WEB-INF");
            File webInfLib = new File(webInf, "lib");
            if (webInfLib.exists()) {
                moduleLibraryURLs = getLibDirectoryJars(webInfLib);
            }
        } else if (handlerName.equals("connector")) {
            // we should add the top level jars for connector module
            moduleLibraryURLs = getLibDirectoryJars(archiveFile);
        }
        return moduleLibraryURLs;
    }

    /**
     * Opens the specified file as an archive, using the provided archive factory.
     * @param dir directory to be opened as an archive
     * @param archiveFactory ArchiveFactory to use to create the archive object
     * @return FileArchive opened for the directory
     * @throws IOException
     */
    public static FileArchive openAsFileArchive(final File dir, final ArchiveFactory archiveFactory) throws IOException {
        return (FileArchive) archiveFactory.openArchive(dir);
    }

    private static List<URL> getLibDirectoryJars(File moduleLibDirectory) throws Exception {
        List<URL> libLibraryURLs = new ArrayList<URL>();
        File[] jarFiles = moduleLibDirectory.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return (pathname.getAbsolutePath().endsWith(".jar"));
            }
        });

        if (jarFiles != null && jarFiles.length > 0) {
            for (File jarFile : jarFiles) {
                libLibraryURLs.add(jarFile.toURL());
            }
        }

        return libLibraryURLs;
    }
}
