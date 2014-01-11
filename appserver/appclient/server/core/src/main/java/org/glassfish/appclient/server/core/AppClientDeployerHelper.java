/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2014 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.appclient.server.core;

import com.sun.enterprise.deploy.shared.FileArchive;
import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.ApplicationClientDescriptor;
import com.sun.enterprise.deployment.archivist.AppClientArchivist;
import com.sun.enterprise.deployment.deploy.shared.OutputJarArchive;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.enterprise.util.shared.ArchivistUtils;
import com.sun.logging.LogDomains;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipException;
import javax.inject.Inject;
import org.glassfish.api.admin.ProcessEnvironment;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.deployment.archive.WritableArchive;
import org.glassfish.appclient.server.core.jws.JavaWebStartInfo;
import org.glassfish.appclient.server.core.jws.servedcontent.ASJarSigner;
import org.glassfish.appclient.server.core.jws.servedcontent.DynamicContent;
import org.glassfish.appclient.server.core.jws.servedcontent.FixedContent;
import org.glassfish.appclient.server.core.jws.servedcontent.StaticContent;
import org.glassfish.appclient.server.core.jws.servedcontent.TokenHelper;
import org.glassfish.deployment.common.Artifacts;
import org.glassfish.deployment.common.DeploymentUtils;
import org.glassfish.deployment.versioning.VersioningSyntaxException;
import org.glassfish.deployment.versioning.VersioningUtils;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.ServerContext;
import org.jboss.logging.LogMessage;

/**
 * Encapsulates the details of generating the required JAR file(s),
 * depending on whether the app client is stand-alone or is nested
 * inside an EAR.
 * <p>
 * See also {@link StandaloneAppClientDeployerHelper} and
 * {@link NestedAppClientDeployerHelper}, the concrete implementation subclasses
 * of AppClientDeployerHelper.
 *
 * @author tjquinn
 */
public abstract class AppClientDeployerHelper {

    private final static String PERSISTENCE_XML_PATH = "META-INF/persistence.xml";
    
    final static String GF_CLIENT_MODULE_PATH ="gf-client-module.jar";
    
    final static String[] CLIENT_POLICY_FILE_NAMES = {"javaee.client.policy","restrict.client.policy"};
    final static String CLIENT_POLICY_PATH_IN_JAR = "META-INF/";
    
    private final DeploymentContext dc;
    private final ApplicationClientDescriptor appClientDesc;
    protected final AppClientArchivist archivist;
    private final String appName;
    private final String clientName;

    private JarFile gfClientModuleJarFile;

    private final Application application;

    private final ServiceLocator habitat;

    private static final Logger logger = Logger.getLogger(JavaWebStartInfo.APPCLIENT_SERVER_MAIN_LOGGER, 
                JavaWebStartInfo.APPCLIENT_SERVER_LOGMESSAGE_RESOURCE);
    
    public static final String ACC_MAIN_LOGGER = "javax.enterprise.system.container.appclient";
    public static final String LOG_MESSAGE_RESOURCE = "org.glassfish.appclient.server.LogMessages";
    
    /**
     * Returns the correct concrete implementation of Helper.
     * @param dc the DeploymentContext for this deployment
     * @return an instance of the correct type of Helper
     * @throws java.io.IOException
     */
    static AppClientDeployerHelper newInstance(
            final DeploymentContext dc,
            final AppClientArchivist archivist,
            final ClassLoader gfClientModuleLoader,
            final ServiceLocator habitat,
            final ASJarSigner jarSigner) throws IOException {
        ApplicationClientDescriptor bundleDesc = dc.getModuleMetaData(ApplicationClientDescriptor.class);
        Application application = bundleDesc.getApplication();
        boolean insideEar = ! application.isVirtual();
        final AppClientDeployerHelper helper = 
            (insideEar ? new NestedAppClientDeployerHelper(
                                    dc,
                                    bundleDesc,
                                    archivist,
                                    gfClientModuleLoader,
                                    application,
                                    habitat,
                                    jarSigner)
                          : new StandaloneAppClientDeployerHelper(
                                    dc,
                                    bundleDesc,
                                    archivist,
                                    gfClientModuleLoader,
                                    application,
                                    habitat));
        return helper;
    }

    protected AppClientDeployerHelper(
            final DeploymentContext dc,
            final ApplicationClientDescriptor bundleDesc,
            final AppClientArchivist archivist,
            final ClassLoader gfClientModuleClassLoader,
            final Application application,
            final ServiceLocator habitat) throws IOException {
        super();
        this.dc = dc;
        this.appClientDesc = bundleDesc;
        this.archivist = archivist;
        this.habitat = habitat;
        gfClientModuleJarFile = new JarFile(new File(getModulesDir(habitat), GF_CLIENT_MODULE_PATH));
        this.appName = appClientDesc.getApplication().getRegistrationName();
        this.clientName = appClientDesc.getModuleDescriptor().getArchiveUri();
        this.application = application;
    }
    
    static File getModulesDir(final ServiceLocator habitat) {
        return new File(habitat.getService(ServerContext.class).getInstallRoot(), "modules/");
    }
    
    static File getLibDir(final ServiceLocator locator) {
        return new File(locator.getService(ServerContext.class).getInstallRoot(), "lib/");
    }
    
    static File getAppClientLibDir(final ServiceLocator locator) {
        return new File(getLibDir(locator), "appclient/");
    }

    /**
     * Returns the URI to the server's copy of the facade JAR file.
     * @param dc the deployment context for the current deployment
     * @return
     */
    public abstract URI facadeServerURI(DeploymentContext dc);

    /**
     * Returns the URI for the facade JAR, relative to the download
     * directory to which the user will fetch the relevant JARs (either
     * as part of "deploy --retrieve" or "get-client-stubs."
     * @param dc the deployment context for the current deployment
     * @return
     */
    public abstract URI facadeUserURI(DeploymentContext dc);

    /**
     * Returns the URI for the group facade JAR, relative to the download
     * directory to which the user will fetch the relevant JARs (either
     * as part of "deploy --retrieve" or "get-client-stubs."
     * @param dc the deployment context for the current deployment
     * @return
     */
    public abstract URI groupFacadeUserURI(DeploymentContext dc);

    public abstract URI groupFacadeServerURI(DeploymentContext dc);
    
    /**
     * Returns the file name (and type) for the facade, excluding any
     * directory information.
     * @param dc the deployment context for the current deployment
     * @return
     */
    protected abstract String facadeFileNameAndType(DeploymentContext dc);

    /**
     * Returns the URI to the developer's original app client JAR within
     * the download directory the user specifies in "deploy --retrieve" or
     * "get-client-stubs."
     * 
     * @param dc
     * @return
     */
    public abstract URI appClientUserURI(DeploymentContext dc);

    /**
     * Returns the URI to be used for the GlassFish-AppClient manifest entry
     * in the facade.
     * 
     * @param dc
     * @return
     */
    public abstract URI appClientUserURIForFacade(DeploymentContext dc);

    /**
     * Returns the URI to the server's copy of the developer's original app
     * client JAR.
     *
     * @param dc
     * @return
     */
    public abstract URI appClientServerURI(DeploymentContext dc);

    /**
     * Returns the URI on the server to the original location of the app client.
     * <p>
     * This is distinct from the appClientServerURI which could be in the
     * generated directory (in the case of a directory deployment, for example).
     * In some cases we need the original location of the app client on the
     * server (for example, to resolve relative references from the app client
     * JAR's manifest Class-Path).
     *
     * @param dc
     * @return
     */
    public abstract URI appClientServerOriginalAnchor(DeploymentContext dc);

    /**
     * Returns the URI within the enclosing app of the app client JAR.
     * Stand-alone app clients are considered to lie within an "implied"
     * containing app; the URI for such app clients is just the file name
     * and type.  The URI for nested app clients within an EAR is the
     * module URI to the app client.
     * 
     * @param dc
     * @return
     */
    public abstract URI appClientURIWithinApp(DeploymentContext dc);

    /**
     * Returns the relative path to the app client within the enclosing app.
     * The result will be an empty string for a stand-alone app clients because
     * it has no such path, in reality.  The result will be the relative URI
     * within the EAR for a nested app client.
     * @param dc
     * @return
     */
    public abstract String pathToAppclientWithinApp(DeploymentContext dc);

    /**
     * Returns a relative URI within the app directory for the specified
     * absolute URI.
     * @param dc
     * @param absoluteURI
     * @return
     */
    public abstract URI URIWithinAppDir(DeploymentContext dc,
            URI absoluteURI);
    /**
     *
     * Returns the class path to be stored in the manifest for the
     * generated facade JAR file.
     *
     * @return
     */
    protected abstract String facadeClassPath();

    protected abstract String PUScanTargets();

    public ApplicationSignedJARManager signedJARManager() {
        return null;
    }

    public abstract void createAndAddLibraryJNLPs(final AppClientDeployerHelper helper,
            final TokenHelper tHelper, final Map<String,DynamicContent> dynamicContent)
            throws IOException;
    
    public Map<String,Map<URI,StaticContent>> signingAliasToJar() {
        return Collections.EMPTY_MAP;
    }

    public final DeploymentContext dc() {
        return dc;
    }

    public ApplicationClientDescriptor appClientDesc() {
        return appClientDesc;
    }

    public String appName() {
        return appName;
    }

    public String appName(final DeploymentContext dc) {
        DeployCommandParameters params = dc.getCommandParameters(DeployCommandParameters.class);
        return params.name();
    }

    public String clientName() {
        return clientName;
    }

    /**
     * Returns a FixedContent object for the file, within the EAR, at the
     * specified relative location.
     * 
     * @param uriString relative path within the EAR
     * @return FixedContent object for the file
     */
    public abstract FixedContent fixedContentWithinEAR(String uriString);

    /**
     * Returns the root directory for signed files in the applications.
     * @return File object for the signed JAR root directory
     */
    public abstract File rootForSignedFilesInApp();

    /**
     * If the specified URI is for an expanded submodule, makes a copy of
     * the submodule as a JAR and returns the URI for the copy.
     * @param classPathElement
     * @return URI to the safe copy of the submodule, relative to the top level
     * if the classPathElement is for a submodule; null otherwise
     */
    File JAROfExpandedSubmodule(final URI candidateSubmoduleURI) throws IOException {
        ReadableArchive source = new FileArchive();
        source.open(dc().getSource().getParentArchive().getURI().resolve(expandedDirURI(candidateSubmoduleURI)));
        OutputJarArchive target = new OutputJarArchive();
        target.create(dc().getScratchDir("xml").toURI().resolve(candidateSubmoduleURI));
        /*
         * Copy the manifest explicitly because the ReadableArchive
         * entries() method omits it.
         */
        Manifest mf = source.getManifest();
        OutputStream os = target.putNextEntry(JarFile.MANIFEST_NAME);
        mf.write(os);
        target.closeEntry();
        copyArchive(source, target, Collections.EMPTY_SET);
        target.close();
        return new File(target.getURI());
    }

    private URI expandedDirURI(final URI submoduleURI) {
        /*
         * The submodule URI (xxx.jar) might actually already be an expanded
         * directory.
         */
        final URI possibleExpandedDirURI = dc().getSource().getParentArchive().getURI().resolve(submoduleURI);
        final File possibleExpandedDir = new File(possibleExpandedDirURI);
        if (possibleExpandedDir.exists() && possibleExpandedDir.isDirectory()) {
            return submoduleURI;
        }
        final String uriText = submoduleURI.toString().replace("/", "__");
        int lastDot = uriText.lastIndexOf('.');
        return URI.create(uriText.substring(0, lastDot) + "_" + uriText.substring(lastDot + 1));
    }

    /**
     * Creates a generated manifest for either the facade (for app client or
     * EAR deployments both) and the ${appName}Client.jar
     * file if this is an app client deployment.
     * <p>
     * Most of the manifest's contents is derived from the source, with
     * the class path passed in as an argument because it varies between the facade and
     * the ${appName}Client.jar file.
     *
     * @param sourceManifest
     * @param facadeManifest
     * @param classpath space-separated list of class path elements
     */
    private void initGeneratedManifest(
            final Manifest sourceManifest,
            final Manifest generatedManifest,
            final String classPath,
            final String PUScanTargets,
            final Application application) {
        Attributes sourceMainAttrs = sourceManifest.getMainAttributes();
        Attributes facadeMainAttrs = generatedManifest.getMainAttributes();
        facadeMainAttrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        facadeMainAttrs.put(Attributes.Name.MAIN_CLASS,
                AppClientDeployer.APPCLIENT_COMMAND_CLASS_NAME);
        facadeMainAttrs.put(AppClientDeployer.GLASSFISH_APPCLIENT_MAIN_CLASS,
                sourceMainAttrs.getValue(Attributes.Name.MAIN_CLASS));
        facadeMainAttrs.put(AppClientArchivist.GLASSFISH_APPCLIENT,
                appClientUserURIForFacade(dc).toASCIIString());
        String splash = sourceMainAttrs.getValue(AppClientDeployer.SPLASH_SCREEN_IMAGE);
        if (splash != null) {
            facadeMainAttrs.put(AppClientDeployer.SPLASH_SCREEN_IMAGE, splash);
        }
        facadeMainAttrs.put(Attributes.Name.CLASS_PATH, classPath);
        if (PUScanTargets != null) {
            facadeMainAttrs.put(AppClientArchivist.GLASSFISH_CLIENT_PU_SCAN_TARGETS_NAME,
                    PUScanTargets);
        }
        facadeMainAttrs.put(AppClientDeployer.GLASSFISH_APP_NAME, application.getAppName());

        facadeMainAttrs.put(AppClientArchivist.GLASSFISH_ANCHOR_DIR, anchorDirRelativeToClient());
        
        if ( ! appClientDesc.isStandalone()) {
//            final DownloadableArtifacts.FullAndPartURIs earFacadeDownload =
//                dc().getTransientAppMetaData("earFacadeDownload", DownloadableArtifacts.FullAndPartURIs.class);

            facadeMainAttrs.put(AppClientArchivist.GLASSFISH_GROUP_FACADE,
                    relativePathToGroupFacade());
        }
    }

    private String anchorDirRelativeToClient() {
        final String pathToClient = pathToAppclientWithinApp(dc);
        final StringBuilder sb = new StringBuilder();
        for (char c : pathToClient.toCharArray()) {
            if (c == '/') {
                sb.append("../");
            }
        }
        return sb.toString();
    }

    protected URI relativeURIToGroupFacade() {
        return URI.create(relativePathToGroupFacade());
    }
    
    private String relativePathToGroupFacade() {
        final StringBuilder sb = new StringBuilder(anchorDirRelativeToClient());
        try {
            /*
             * One more level up because the group facade will reside in the
             * download directory.
             */
            sb.append("../").append(VersioningUtils.getUntaggedName(appName)).append("Client.jar");
        } catch (VersioningSyntaxException ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return sb.toString();
    }

    private void writeUpdatedDescriptors(final ReadableArchive source, final OutputJarArchive facadeArchive, final ApplicationClientDescriptor acd) throws IOException {
        archivist.setDescriptor(acd);
        archivist.writeDeploymentDescriptors(source, facadeArchive);
    }

    protected void prepareJARs() throws IOException, URISyntaxException {
        // In embedded mode, we don't process app clients so far.
        if (habitat.<ProcessEnvironment>getService(ProcessEnvironment.class).getProcessType().isEmbedded()) {
            return;
        }
        generateAppClientFacade();
    }

    protected abstract void addTopLevelContentToClientFacade(final OutputJarArchive facadeArchive) throws IOException;
    
    /**
     * Adds the client policy files to the top-level generated JAR.
     * <p>
     * For a stand-alone client (not in an EAR) this implementation adds the
     * policy files to the generated app client facade JAR.  
     * 
     * @param clientFacadeArchive the generated app client facade JAR
     * @throws IOException 
     */
    protected void addClientPolicyFiles(final OutputJarArchive clientFacadeArchive) throws IOException {
        for (String policyFileName : CLIENT_POLICY_FILE_NAMES) {
            final File policyFile = new File(getAppClientLibDir(habitat), policyFileName);
            if (policyFile.canRead()) {
                copyFileToTopLevelJAR(clientFacadeArchive, policyFile, CLIENT_POLICY_PATH_IN_JAR + policyFileName);
            }
        }
    }
    
    protected abstract void copyFileToTopLevelJAR(final OutputJarArchive clientFacadeArchive, final File f, final String path) throws IOException;
    
    protected final void generateAppClientFacade() throws IOException, URISyntaxException {
        OutputJarArchive facadeArchive = new OutputJarArchive();
        /*
         * Make sure the directory subtree to contain the facade exists.  If the
         * client URI within the EAR contains a directory then that directory
         * probably does not exist in the generated dir for this app...not yet
         * anyway...it is about to exist.
         */
        final File facadeFile = new File(facadeServerURI(dc));
        if ( ! facadeFile.getParentFile().exists()) {
            if ( ! facadeFile.getParentFile().mkdirs()) {
                final String msg = logger.getResourceBundle().getString("enterprise.deployment.appclient.errormkdirs");
                throw new IOException(MessageFormat.format(msg, facadeFile.getAbsolutePath()));
            }
        }
        facadeArchive.create(facadeServerURI(dc));
        ReadableArchive source = dc.getSource();
        Manifest sourceManifest = source.getManifest();
        if (sourceManifest == null) {
            final String msg = logger.getResourceBundle().getString("enterprise.deployment.appclient.noManifest");
            throw new IOException(MessageFormat.format(msg, source.getURI().toASCIIString()));
        }
        Manifest facadeManifest = facadeArchive.getManifest();
        initGeneratedManifest(sourceManifest, facadeManifest, 
                facadeClassPath(), PUScanTargets(), application);
        /*
         * If the developer's app client JAR contains a splash screen, copy
         * it from the original JAR to the facade so the Java launcher can
         * display it when the app client is launched.
         */
        final Attributes srcMainAttrs = sourceManifest.getMainAttributes();
        if (srcMainAttrs == null) {
            final String msg = logger.getResourceBundle().getString("enterprise.deployment.appclient.noMainAttrs");
            throw new IOException(MessageFormat.format(msg, source.getURI().toASCIIString()));
        }
        String splash = srcMainAttrs.getValue(AppClientDeployer.SPLASH_SCREEN_IMAGE);
        if (splash != null) {
            copy(source, facadeArchive, splash);
        }
        /*
         * Write the manifest to the facade.
         */
        OutputStream os = facadeArchive.putNextEntry(JarFile.MANIFEST_NAME);
        facadeManifest.write(os);
        facadeArchive.closeEntry();
        /*
         * Write the updated descriptors to the facade.
         */
        writeUpdatedDescriptors(source, facadeArchive, appClientDesc);

        /*
         * Because of how persistence units are discovered and added to the
         * app client DOL object when the archivist reads the descriptor file,
         * add any META-INF/persistence.xml file from the developer's client
         * to the client facade.  (The generated descriptor and the
         * persistence.xml files need to be in the same archive.)
         */
        copyPersistenceUnitXML(source, facadeArchive);

        copyMainClass(facadeArchive);
        
        addTopLevelContentToClientFacade(facadeArchive);
        
        facadeArchive.close();
    }

    /**
     * copy the entryName element from the source abstract archive into
     * the target abstract archive
     */
    static void copy(
            ReadableArchive source, WritableArchive target, String entryName)
            throws IOException {

        InputStream is = null;
        OutputStream os = null;
        try {
            is = source.getEntry(entryName);
            if (is != null) {
                try {
                    os = target.putNextEntry(entryName);
                } catch (ZipException ze) {
                    // this is a duplicate...
                    return;
                }
                ArchivistUtils.copyWithoutClose(is, os);
            } else {
                // This may be a directory specification if there is no entry
                // in the source for it...for example, a directory expression
                // in the Class-Path entry from a JAR's manifest.  
                // 
                // Try to copy all entries from the source that have the 
                // entryName as a prefix.
                for (Enumeration e = source.entries(entryName); e.hasMoreElements();) {
                    copy(source, target, (String) e.nextElement());
                }
            }
        } catch (IOException ioe) {
            throw ioe;
        } finally {
            IOException closeEntryIOException = null;
            if (os != null) {
                try {
                    target.closeEntry();
                } catch (IOException ioe) {
                    closeEntryIOException = ioe;
                }
            }
            if (is != null) {
                is.close();
            }

            if (closeEntryIOException != null) {
                throw closeEntryIOException;
            }
        }
    }
    
    static void copyArchive(
            ReadableArchive source, WritableArchive target, Set excludeList) {
        for (Enumeration e = source.entries(); e.hasMoreElements();) {
            String entryName = String.class.cast(e.nextElement());
            if (excludeList.contains(entryName)) {
                continue;
            }
            try {
                copy(source, target, entryName);
            } catch (IOException ioe) {
                // duplicate, we ignore
            }
        }
    }
    
    private void copyClass(final WritableArchive facadeArchive,
            final String classResourcePath) throws IOException {
        OutputStream os = facadeArchive.putNextEntry(classResourcePath);
        InputStream is = openByteCodeStream(classResourcePath);

        DeploymentUtils.copyStream(is, os);
        try {
            is.close();
            facadeArchive.closeEntry();
        } catch (IOException ignore) {
        }
    }
    

    private void copyMainClass(final WritableArchive facadeArchive) throws IOException {
        copyClass(facadeArchive, AppClientDeployer.APPCLIENT_FACADE_CLASS_FILE);
    }

    private void copyPersistenceUnitXML(final ReadableArchive sourceClient,
            final WritableArchive facadeArchive) throws IOException {
        InputStream persistenceXMLStream = sourceClient.getEntry(PERSISTENCE_XML_PATH);
        if (persistenceXMLStream != null) {
            OutputStream os = facadeArchive.putNextEntry(PERSISTENCE_XML_PATH);
            DeploymentUtils.copyStream(persistenceXMLStream, os);
            try {
                persistenceXMLStream.close();
                facadeArchive.closeEntry();
            } catch (IOException ignore) {
            }
        }
    }

    protected InputStream openByteCodeStream(final String resourceName) throws IOException {
        final JarEntry entry = gfClientModuleJarFile.getJarEntry(resourceName);
        return gfClientModuleJarFile.getInputStream(entry);
    }

    protected abstract Set<Artifacts.FullAndPartURIs> clientLevelDownloads() throws IOException;

    public abstract Set<Artifacts.FullAndPartURIs> earLevelDownloads() throws IOException;
    
    Proxy proxy() {
        return new Proxy(this);
    }

    /**
     * Wrapper around AppClientDeployer for storage in the deployment context's
     * meta data.
     * <p>
     * Storage and retrieval of meta data is type-based.  We cannot retrieve
     * stored AppClientDeployerHelper by type alone because the actual instance
     * is one of the concrete subclasses.  So this wrapper provides a way to
     * store a single type in the meta data so we can retrieve it.
     */
    public static class Proxy {
        private final AppClientDeployerHelper helper;

        public Proxy(final AppClientDeployerHelper helper) {
            this.helper = helper;
        }

        public AppClientDeployerHelper helper() {
            return helper;
        }
    }
}
