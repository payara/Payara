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

import com.sun.enterprise.config.serverbeans.ServerTags;
import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.ApplicationClientDescriptor;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.deployment.archivist.AppClientArchivist;
import com.sun.enterprise.deployment.deploy.shared.OutputJarArchive;
import com.sun.enterprise.deployment.deploy.shared.Util;
import org.glassfish.deployment.common.ModuleDescriptor;
import com.sun.logging.LogDomains;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.glassfish.api.admin.ProcessEnvironment;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.appclient.server.core.jws.JWSAdapterManager;
import org.glassfish.appclient.server.core.jws.JavaWebStartInfo;
import org.glassfish.appclient.server.core.jws.servedcontent.ASJarSigner;
import org.glassfish.appclient.server.core.jws.servedcontent.DynamicContent;
import org.glassfish.appclient.server.core.jws.servedcontent.FixedContent;
import org.glassfish.appclient.server.core.jws.servedcontent.StaticContent;
import org.glassfish.appclient.server.core.jws.servedcontent.TokenHelper;
import org.glassfish.deployment.common.Artifacts;
import org.glassfish.deployment.common.Artifacts.FullAndPartURIs;
import org.glassfish.deployment.common.ClientArtifactsManager;
import org.glassfish.deployment.common.DeploymentUtils;
import org.glassfish.deployment.versioning.VersioningSyntaxException;
import org.glassfish.deployment.versioning.VersioningUtils;
import org.glassfish.hk2.api.ServiceLocator;


public class NestedAppClientDeployerHelper extends AppClientDeployerHelper {

    private final static String LIBRARY_SECURITY_PROPERTY_NAME = "library.security";
    private final static String LIBRARY_JARS_PROPERTY_NAME = "library.jars";
    private final static String LIBRARY_JNLP_PATH_PROPERTY_NAME = "library.jnlp.path";

    private static final String LIBRARY_DOCUMENT_TEMPLATE =
            JavaWebStartInfo.DOC_TEMPLATE_PREFIX + "libraryJarsDocumentTemplate.jnlp";

    private StringBuilder classPathForFacade = new StringBuilder();
    private StringBuilder PUScanTargetsForFacade = new StringBuilder();

    private final URI earURI;

    private final ASJarSigner jarSigner;

    private ApplicationSignedJARManager signedJARManager;

    private StringBuilder libExtensionElementsForMainDocument = null;

    private static final Logger logger = Logger.getLogger(JavaWebStartInfo.APPCLIENT_SERVER_MAIN_LOGGER, 
                JavaWebStartInfo.APPCLIENT_SERVER_LOGMESSAGE_RESOURCE);

    /**
     * records the downloads needed to support this app client,
     * including the app client JAR itself, the facade, and the transitive
     * closure of any library JARs from the EAR's lib directory or from the
     * app client's class path
     */
    private final Set<FullAndPartURIs> clientLevelDownloads = new HashSet<FullAndPartURIs>();
    private Set<FullAndPartURIs> earLevelDownloads = null;
    private final static String EAR_LEVEL_DOWNLOADS_KEY = "earLevelDownloads";

    private final ServiceLocator habitat;

    private final AppClientGroupFacadeGenerator groupFacadeGenerator;

    private final boolean isDirectoryDeployed;

    /** recognizes expanded directory names for submodules */
    private static final Pattern submoduleURIPattern = Pattern.compile("(.*)__([wcrj]ar)$");
    
    private final ClientArtifactsManager clientArtifactsManager;

    private boolean isTopLevelPopulated = false;

    NestedAppClientDeployerHelper(
            final DeploymentContext dc,
            final ApplicationClientDescriptor bundleDesc,
            final AppClientArchivist archivist,
            final ClassLoader gfClientModuleClassLoader,
            final Application application,
            final ServiceLocator habitat,
            final ASJarSigner jarSigner) throws IOException {
        super(dc, bundleDesc, archivist, gfClientModuleClassLoader, application, habitat);
        this.habitat = habitat;
        clientArtifactsManager = ClientArtifactsManager.get(dc);
        groupFacadeGenerator = habitat.getService(AppClientGroupFacadeGenerator.class);
        this.jarSigner = jarSigner;
        isDirectoryDeployed = Boolean.valueOf(dc.getAppProps().getProperty(ServerTags.DIRECTORY_DEPLOYED));
        earURI = dc.getSource().getParentArchive().getURI();
        processDependencies();
    }

    @Override
    protected void prepareJARs() throws IOException, URISyntaxException {
        super.prepareJARs();

        // In embedded mode, we don't process app clients so far.
        if (habitat.<ProcessEnvironment>getService(ProcessEnvironment.class).getProcessType().isEmbedded()) {
            return;
        }

        groupFacadeGenerator.run(this);

    }

    @Override
    protected void addTopLevelContentToClientFacade(OutputJarArchive facadeArchive) throws IOException {
        // no-op for nested app clients
    }

    @Override
    public FixedContent fixedContentWithinEAR(String uriString) {
        return new FixedContent(new File(earDirUserURI(dc()).resolve(uriString)));
    }

    public String appLibraryExtensions() {
        return (libExtensionElementsForMainDocument == null ? 
            "" : libExtensionElementsForMainDocument.toString());
    }

    @Override
    public Map<String,Map<URI,StaticContent>> signingAliasToJar() {
        return signedJARManager.aliasToContent();
    }


    @Override
    public void createAndAddLibraryJNLPs(final AppClientDeployerHelper helper,
            final TokenHelper tHelper, final Map<String,DynamicContent> dynamicContent) throws IOException {


        /*
         * For each group of like-signed library JARs create a separate JNLP for
         * the group and add it to the dynamic content for the client.  Also
         * build up a property to hold the full list of such generated JNLPs
         * so it can be substituted into the generated client JNLP below.
         */

        libExtensionElementsForMainDocument = new StringBuilder();

        for (Map.Entry<String,Map<URI,StaticContent>> aliasToContentEntry : signingAliasToJar().entrySet()) {
            final String alias = aliasToContentEntry.getKey();
            final Map<URI,StaticContent> libURIs = aliasToContentEntry.getValue();

            tHelper.setProperty(LIBRARY_SECURITY_PROPERTY_NAME, librarySecurity(alias));
            tHelper.setProperty(LIBRARY_JNLP_PATH_PROPERTY_NAME, libJNLPRelPath(alias));
            final StringBuilder libJarElements = new StringBuilder();

            for (Map.Entry<URI,StaticContent> entry : libURIs.entrySet()) {
                final URI uri = entry.getKey();
                libJarElements.append("<jar href=\"").append(libJARRelPath(uri)).append("\"/>");
            }
            tHelper.setProperty(LIBRARY_JARS_PROPERTY_NAME, libJarElements.toString());

            JavaWebStartInfo.createAndAddDynamicContent(
                    tHelper, dynamicContent, libJNLPRelPath(alias),
                LIBRARY_DOCUMENT_TEMPLATE);
            
            libExtensionElementsForMainDocument.append(extensionElement(alias, libJNLPRelPath(alias)));
        }
        
        tHelper.setProperty(JavaWebStartInfo.APP_LIBRARY_EXTENSION_PROPERTY_NAME,
                libExtensionElementsForMainDocument.toString());
    }

    @Override
    public Set<FullAndPartURIs> earLevelDownloads() {
        if (earLevelDownloads == null) {
            earLevelDownloads = dc().getTransientAppMetaData(EAR_LEVEL_DOWNLOADS_KEY, HashSet.class);
            if (earLevelDownloads == null) {
                earLevelDownloads = new HashSet<FullAndPartURIs>();
                dc().addTransientAppMetaData(EAR_LEVEL_DOWNLOADS_KEY, earLevelDownloads);
            }
        }
        return earLevelDownloads;
    }

    @Override
    public File rootForSignedFilesInApp() {
        return new File(dc().getScratchDir("xml").getParentFile(), "signed/");
    }

    @Override
    public ApplicationSignedJARManager signedJARManager() {
        return signedJARManager;
    }

    /**
     * Adds a file to the EAR-level group facade JAR.
     * 
     * @param clientFacadeArchive - ignored for nested app clients
     * @throws IOException 
     */
    @Override
    protected void copyFileToTopLevelJAR(OutputJarArchive clientFacadeArchive, File f, String path) throws IOException {
        clientArtifactsManager.add(f, path, false /* isTemporary */);
    }

    @Override
    protected void addClientPolicyFiles(OutputJarArchive clientFacadeArchive) throws IOException {
        if ( ! isTopLevelPopulated) {
            super.addClientPolicyFiles(clientFacadeArchive);
            isTopLevelPopulated = true;
        }
    }
    
    
    private String libJARRelPath(final URI absURI) {
        return JavaWebStartInfo.relativeURIForProvidedOrGeneratedAppFile(dc(), absURI, this).toASCIIString();
    }

    private String extensionElement(final String alias, final String libURIText) {
        return "<extension name=\"libJars" + (alias == null ? "" : "-" + alias) +
                "\" href=\"" + libURIText + "\"/>";
    }

    private String librarySecurity(final String alias) {
        return (alias == null ? "" : "<security><all-permissions/></security>");
    }

    private String libJNLPRelPath(final String alias) {
        return "___lib/client-libs" + (alias == null ? "" : "-" + alias) + ".jnlp";
    }

    /**
     * Creates downloadable artifacts for any JARs or directory contents on
     * which this nested app client might depend and adds them to the
     * collection of downloadable artifacts for this EAR.
     *
     * @throws IOException
     */
    private void processDependencies() throws IOException {

        /*
         * Currently, for directory-deployed apps, we generate JAR files for
         * the submodules.  This is primarily for Java Web Start support, but
         * we also download those generated JARs as part of the "deploy --retrieve" or
         * "get-client-stubs" operations.
         *
         */


        signedJARManager = new ApplicationSignedJARManager(
                JWSAdapterManager.signingAlias(dc()),
                jarSigner,
                habitat,
                dc(),
                this,
                earURI,
                earDirUserURI(dc()));

        /*
         * Init the class path for the facade so it refers to the developer's app client,
         * relative to where the facade will be.
         */
        URI appClientURI = URI.create(Util.getURIName(appClientUserURI(dc())));
        classPathForFacade.append(appClientURI);

        /*
         * Because the group facade contains generated stubs (if any), add the
         * relative path to the group facade to the facade's Class-Path so those
         * stubs will be accessible via the class path at runtime.
         */

        final URI groupFacadeURIRelativeToFacade =
                facadeUserURI(dc()).relativize(relativeURIToGroupFacade());
        classPathForFacade.append(" ").append(groupFacadeURIRelativeToFacade.toASCIIString());

        /*
         * For a nested app client, the required downloads include the
         * developer's original app client JAR, the generated facade JAR,
         * the generated EAR-level facade, and
         * the transitive closure of all JARs in the app client's Class-Path
         * and the JARs in the EAR's library-directory.
         *
         * If the user has selected compatibility with v2 behavior, then also
         * consider EJB submodules and JARs at the top level of the EAR.
         */
        clientLevelDownloads.add(new Artifacts.FullAndPartURIs(
                facadeServerURI(dc()),
                facadeUserURI(dc())));

        /*
         * dependencyURIsProcessed records URIs, relative to the original JAR as it will
         * reside in the user's download directory, that have already been
         * processed.  This allows us to avoid processing the same JAR or dir more
         * than once if more than one JAR depends on it.
         *
         * Note that all dependencies expressed in the client's manifest must
         * resolve within the EAR, not within the client. So those
         * dependent JARs (or directory contents) will be "EAR-level" not client-level.
         */
        Set<URI> dependencyURIsProcessed = new HashSet<URI>();

        URI appClientURIWithinEAR = URI.create(appClientDesc().getModuleDescriptor().getArchiveUri());
        final Artifact appClientJARArtifact = newArtifact(appClientURIWithinEAR);

        /*
         * Processing the client artifact will recursively process any artifacts
         * on which it depends plus the transitive closure thereof.
         */
        appClientJARArtifact.processArtifact(dependencyURIsProcessed,
                clientLevelDownloads(), earLevelDownloads());

        /*
         * Now incorporate the library JARs and, if v2 compatibility is chosen,
         * EJB JARs and top level JARs.
         */
        addLibraryJARs(classPathForFacade, PUScanTargetsForFacade,
                dependencyURIsProcessed);

        if (DeploymentUtils.useV2Compatibility(dc()) && ! appClientDesc().getApplication().isVirtual()) {
            addEJBJARs(classPathForFacade, dependencyURIsProcessed);
            addTopLevelJARs(classPathForFacade, PUScanTargetsForFacade,
                    dependencyURIsProcessed);
        }
    }

    /**
     * Adds EJB JARs to the download set for this application.  For compat (if
     * selected) with v2.
     * @param cpForFacade accumulated class path for the generated facade
     * @param dependencyURIsProcessed record of what URIs have been processed
     * @throws IOException
     */
    private void addEJBJARs(final StringBuilder cpForFacade, final Set<URI> dependencyURIsProcessed) throws IOException {
        final Application app = appClientDesc().getApplication();
        for (ModuleDescriptor md : app.getModuleDescriptorsByType(DOLUtils.ejbType())) {
            addJar(cpForFacade, null,
                   new File(new File(earURI), md.getArchiveUri()).toURI(),
                   dependencyURIsProcessed);
        }
    }

    /**
     * Adds top-level JARs in the EAR to the download set for this application.
     * For compatibility with v2 (if selected).
     *
     * @param cpForFacade accumulated class path for the generated facade
     * @param dependencyURIsProcessed record of what URIs have been processed
     * @throws IOException
     */
    private void addTopLevelJARs(final StringBuilder cpForFacade,
            final StringBuilder puScanTargets,
            final Set<URI> dependencyURIsProcessed) throws IOException {
        /*
         * Add top-level JARs only if they are not submodules.
         */
        final Set<URI> submoduleURIs = new HashSet<URI>();
        for (ModuleDescriptor<BundleDescriptor> md : appClientDesc().getApplication().getModules()) {
            submoduleURIs.add(URI.create(md.getArchiveUri()));
        }

        addJARsFromDir(cpForFacade, puScanTargets, dependencyURIsProcessed,
                new File(earURI),
                new FileFilter() {
            @Override
                    public boolean accept(final File pathname) {
                        return pathname.getName().endsWith(".jar") && ! pathname.isDirectory()
                                && ! submoduleURIs.contains(earURI.relativize(pathname.toURI()));
                    }
                  }
                );
    }

    /**
     * Adds all JARs that pass the filter to the download set for the application.
     *
     * @param cpForFacade accumulated class path for the generated facade
     * @param dependencyURIsProcessed record of what URIs have beeen processed
     * @param dirContainingJARs directory to scan for JARs
     * @param filter file filter to apply to limit which JARs to accept
     * @throws IOException
     */
    private void addJARsFromDir(final StringBuilder cpForFacade,
            final StringBuilder puScanTargets,
            final Set<URI> dependencyURIsProcessed,
            final File dirContainingJARs,
            final FileFilter filter) throws IOException {
        if (dirContainingJARs.exists() && dirContainingJARs.isDirectory()) {
            for (File jar : dirContainingJARs.listFiles(filter)) {
                addJar(cpForFacade, puScanTargets, jar.toURI(), dependencyURIsProcessed);
            }
        }

    }

    private void addLibraryJARs(final StringBuilder cpForFacade,
            final StringBuilder puScanTargets,
            final Set<URI> dependencyURIsProcessed) throws IOException {
        final String libDir = appClientDesc().getApplication().getLibraryDirectory();
        if (libDir != null) {
            addJARsFromDir(cpForFacade, puScanTargets, dependencyURIsProcessed,
                new File(new File(earURI), libDir),
                new FileFilter() {
                @Override
                    public boolean accept(File pathname) {
                        return pathname.getName().endsWith(".jar") && ! pathname.isDirectory();
                    }
                }
            );
        }
    }

    /**
     * Adds a JAR to the download set for the app, adjusting the accumulated
     * classpath for the facade in the process.
     * @param cpForFacade accumulated class path for the facade JAR
     * @param jarURI URI of the JAR to be added
     * @param dependencyURIsProcessed record of which URIs have already been added for this app
     * @throws IOException
     */
    private void addJar(
            final StringBuilder cpForFacade,
            final StringBuilder puScanTargets,
            final URI jarURI,
            final Set<URI> dependencyURIsProcessed) throws IOException {
        final URI jarURIForFacade = relativeToFacade(jarURI);
        final URI jarURIForAnchor = earURI.relativize(jarURI);
        final URI fileURIForJAR = URI.create("file:" + jarURI.getRawSchemeSpecificPart());
        if (dependencyURIsProcessed.contains(fileURIForJAR)) {
            return;
        }

        /*
         * Add a relative URI from where the facade will be to where
         * this library JAR will be, once they are both downloaded,
         * to the class path for the facade.
         */
        if (cpForFacade.length() > 0) {
            cpForFacade.append(' ');
        }
        cpForFacade.append(jarURIForFacade.toASCIIString());
        if (puScanTargets != null) {
            if (puScanTargets.length() > 0) {
                puScanTargets.append(' ');
            }
            puScanTargets.append(jarURIForFacade.toASCIIString());
        }

        /*
         * Process this library JAR to record the need to download it
         * and any JARs or directories it depends on.
         */
        final Artifact jarArtifact = newArtifact(earURI, jarURIForAnchor);
        if (jarArtifact != null) {
            jarArtifact.processArtifact(dependencyURIsProcessed,
                earLevelDownloads(), earLevelDownloads());
        }
    }

    private boolean isSubmodule(final URI candidateURI) {
        for (ModuleDescriptor<BundleDescriptor> desc : appClientDesc().getApplication().getModules()) {
            if (URI.create(desc.getArchiveUri()).equals(candidateURI)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Computes a relative URI within the downloaded file directory
     * structure for the specified JAR URI.
     *
     * @param absJARURI absolute URI on the server for the JAR
     * @return URI to the JAR (in its downloaded position) relative to the downloaded facade
     */
    private URI relativeToFacade(final URI absJARURI) {
        final URI jarRelOnServer = earURI.relativize(absJARURI);
        final StringBuilder dotsFromFacadeToAnchor = new StringBuilder();
        final String clientWithinApp = pathToAppclientWithinApp(dc());
        int slot = -1;
        while ( (slot = clientWithinApp.indexOf('/', slot+1)) != -1) {
            dotsFromFacadeToAnchor.append("../");
        }
        return URI.create(dotsFromFacadeToAnchor.append(jarRelOnServer.toASCIIString()).toString());
    }

    @Override
    public URI facadeServerURI(DeploymentContext dc) {
        File genXMLDir = dc.getScratchDir("xml");
        return genXMLDir.toURI().resolve(relativeFacadeURI(dc));
    }

    @Override
    protected Set<FullAndPartURIs> clientLevelDownloads() throws IOException {
        return clientLevelDownloads;
    }

    @Override
    protected String facadeClassPath() {
        return classPathForFacade.toString();
    }

    @Override
    protected String PUScanTargets() {
        return PUScanTargetsForFacade.toString();
    }



    @Override
    public URI facadeUserURI(DeploymentContext dc){
        try {
            return URI.create(VersioningUtils.getUntaggedName(appName(dc)) + "Client/" + relativeFacadeURI(dc));
        } catch (VersioningSyntaxException ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return URI.create("");
    }

    @Override
    public URI groupFacadeUserURI(DeploymentContext dc) {
        return relativeGroupFacadeURI(dc);
    }

    @Override
    public URI groupFacadeServerURI(DeploymentContext dc) {
        File genXMLDir = dc.getScratchDir("xml").getParentFile();
        return genXMLDir.toURI().resolve(relativeGroupFacadeURI(dc));
    }

    private URI relativeGroupFacadeURI(DeploymentContext dc) {
        URI uri = URI.create("");
        try {
            uri = URI.create(VersioningUtils.getUntaggedName(appName(dc)) + "Client.jar");
        } catch (VersioningSyntaxException ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return uri;
    }

    private URI relativeFacadeURI(DeploymentContext dc) {
        return moduleURI().resolve(facadeFileNameAndType(dc));
    }

    @Override
    protected String facadeFileNameAndType(DeploymentContext dc) {
        return moduleNameOnly() + "Client.jar";
    }

    @Override
    public URI appClientUserURI(DeploymentContext dc) {
        return earDirUserURI(dc).resolve(moduleURI());
    }

    @Override
    public URI appClientUserURIForFacade(DeploymentContext dc) {
        return URI.create(Util.getURIName(appClientUserURI(dc)));
    }


    private URI earDirUserURI(final DeploymentContext dc) {
        try {
            return URI.create(VersioningUtils.getUntaggedName(appName(dc)) + "Client/");
        } catch (VersioningSyntaxException ex) {
           logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return URI.create("");
    }

    @Override
    public URI appClientServerURI(DeploymentContext dc) {
        URI result;
        String appClientURIWithinEAR = appClientDesc().getModuleDescriptor().getArchiveUri();
        Matcher m = submoduleURIPattern.matcher(appClientURIWithinEAR);
        final File userProvidedJarFile = new File(new File(earURI), appClientURIWithinEAR);
        /*
         * If either the URI specifies the expanded directory for a directory-
         * deployed app client or there is no actual JAR file for the app
         * client (meaning it is an expanded directory),
         * the server-side URI for the app client JAR will need to be in
         * the generated directory.
         */
        if (m.matches()) {
            result = new File(dc.getScratchDir("xml"), m.group(1) + "." + m.group(2)).toURI();
        } else if ( ! userProvidedJarFile.exists())  {
            result = new File(dc.getScratchDir("xml"), appClientURIWithinEAR).toURI();
        } else {
            result = userProvidedJarFile.toURI();
        }
        return result;
    }

    @Override
    public URI appClientServerOriginalAnchor(DeploymentContext dc) {
        final String appClientURIWithinEAR = appClientDesc().getModuleDescriptor().getArchiveUri();
        final File userProvidedClientLocation = new File(new File(earURI), appClientURIWithinEAR);
        return userProvidedClientLocation.toURI();
    }


    @Override
    public URI appClientURIWithinApp(DeploymentContext dc) {
        return URI.create(appClientDesc().getModuleDescriptor().getArchiveUri());
    }

    @Override
    public URI URIWithinAppDir(DeploymentContext dc, URI absoluteURI) {
        return earURI.relativize(absoluteURI);
    }

    @Override
    public String pathToAppclientWithinApp(DeploymentContext dc) {
        return appClientDesc().getModuleDescriptor().getArchiveUri();
    }



    private URI moduleURI() {
        return URI.create(appClientDesc().getModuleDescriptor().getArchiveUri());
    }

    private String moduleNameAndType() {
        return Util.getURIName(moduleURI());
    }

    private String moduleNameOnly() {
        String nameAndType = moduleNameAndType();
        return nameAndType.substring(0, nameAndType.lastIndexOf(".jar"));
    }

    /**
     * For the provided URI returns a URI with scheme "file" which can be
     * used in constructing a File object (for existence checking, etc.).
     *
     * @param uri URI of interest
     * @return initial uri if is has scheme "file" or a new URI to the same jar file if the URI's scheme is "jar"
     * @throws URISyntaxException
     */
    private URI ensureFileSchemedURI(final URI uri) throws URISyntaxException {
        URI result = uri;
        if (uri.getScheme().equals("jar")) {
            result = new URI("file", uri.getRawSchemeSpecificPart(), null);
        }
        return result;
    }

    /**
     * Creates a new artifact object, using an existing Artifact and a URI
     * of a file that artifact references.
     *
     * @param referencingArtifact existing Artifact which refers to some other file
     * @param referencedURI URI to the referenced file, relative to the referencing artifact
     * @return Artifact representing the referenced file
     * @throws IOException
     */
    private Artifact newArtifact(
            final Artifact referencingArtifact,
            final URI referencedURI) throws IOException {
        return newArtifact(referencingArtifact.canonicalURIWithinEAR(), referencedURI);
    }

    /**
     * Creates a new Artifact using the URI of a referencing file and the URI
     * of the referenced file.
     *
     * @param referencingURI URI of the referencing file
     * @param referencedURI URI of the referenced file, relative to the referencing file
     * @return
     * @throws IOException
     */
    private Artifact newArtifact(
            final URI referencingURI,
            final URI referencedURI) throws IOException {
        return newArtifact(referencingURI.resolve(referencedURI).normalize());
    }

    /**
     * Creates a new Artifact based on the canonical URI of a file within the EAR.
     * <p>
     * Note that the "canonical URI within the EAR" is the URI within the EAR
     * which the artifact would have if the EAR were packaged as a true archive
     * (as opposed to a pre-expanded directory archive).  That means the URI will
     * have slashes denoting subdirectories (as opposed to double-underscores) and
     * will likely have a dotted file type such as ".jar" as opposed to "_jar"
     * for example as the suffix on the expanded directory.
     *
     * @param canonicalArtifactURIWithinEAR
     * @return
     * @throws IOException
     */
    private Artifact newArtifact(
            final URI canonicalArtifactURIWithinEAR) throws IOException {
        
        Artifact result = null;

        /*
         * Return the correct type of Artifact.
         */
        if (isSubmodule(canonicalArtifactURIWithinEAR) && isDirectoryDeployed) {
            /*
             * We need to have an actual JAR file to download but none
             * exists, because this is a directory deployment and the URI
             * refers to a submodule. 
             */
            result = new VirtualJARArtifact(canonicalArtifactURIWithinEAR);
        } else {
            /*
             * The URI specified refers to an actually existing file.
             */
            final File artifactFile;
            try {
                artifactFile = new File(
                    ensureFileSchemedURI(earURI.resolve(canonicalArtifactURIWithinEAR)));
                if (artifactFile.exists()) {
                    if (artifactFile.isDirectory()) {
                        result = new DirectoryArtifact(artifactFile);
                    } else {
                        result = new JARArtifact(artifactFile);
                    }
                } else {
                    logger.log(Level.FINE,
                            "Attempt to create artifact with URI {0} which translates to the file {1}  but no such file exists.",
                            new Object[]{
                                canonicalArtifactURIWithinEAR.toASCIIString(),
                                artifactFile.getAbsolutePath()});
                }
            } catch (URISyntaxException ex) {
                throw new RuntimeException(ex);
            }
        }
        return result;
    }

    /**
     * Info about an artifact needed for download.
     * <p>
     * This abstraction is useful because artifacts can be either as the developer
     * created (if deployed using archive, not directory, deployment) or
     * generated (we generated JARs for the expanded submodule directories
     * in a developer-provided directory deployment).
     */
    private abstract class Artifact {

        /** the actual file to be downloaded - perhaps the original developer's
         * file, perhaps a generated file */
        private final File physicalFile;

        /**
         * Returns the canonical URI of this artifact within the EAR.
         * <p>
         * The canonical URI is the URI that the artifact would have if it were
         * packaged normally inside an EAR.  This is distinct from, for example,
         * the URI of an expanded submodule directory.
         *
         * @return URI for the artifact as if it were packaged in an EAR (even if it was not)
         */
        abstract URI canonicalURIWithinEAR();

        /**
         * Processes this artifact - adding it to the downloads for this app -
         * and also processes all files to which this artifact refers.
         *
         * @param artifactURIsProcessed URIs of files already processed
         * @param downloadsForThisArtifact collection of downloads to which
         * this artifact should be added
         * @param downloadsForReferencedArtifacts collection of artifacts to
         * which any files referenced by this artifact should be added
         * @throws IOException
         */
        abstract void processArtifact(
                final Set<URI> artifactURIsProcessed,
                final Collection<FullAndPartURIs> downloadsForThisArtifact,
                final Collection<FullAndPartURIs> downloadsForReferencedArtifacts) throws IOException;

        /**
         * Creates a new artifact for the specified URI relative to the URI of
         * the specified directory or referencing JAR.
         *
         * @param referringURI URI of the directory or JAR which refers to the
         * referenced URI
         * @param referencedURI URI of the referenced JAR, relative
         * to the referencing URI
         */
        private Artifact(final File physicalFile) {
            this.physicalFile = physicalFile;
        }

        /**
         * Returns a FullAndPartURIs object representing the download information
         * for this artifact. <p>
         * As with all such objects, the full URI is for the actual physical file
         * to be downloaded, and the "part" URI is a relative URI for where
         * within the user's download directory the downloade file will reside.
         * @return FullAndPartURIs object for this artifact's download data
         */
        Artifacts.FullAndPartURIs downloadInfo() {
            return new FullAndPartURIs(physicalFile.toURI(), 
                    earDirUserURI(dc()).resolve(canonicalURIWithinEAR()));
        }
        
        File physicalFile() {
            return physicalFile;
        }

        /**
         * Marks the artifact as processed, in both the collection of
         * already-processed URIs and in the downloads to which this artifact
         * should be added.
         *
         * @param artifactURIsProcessed
         * @param downloadsForThisArtifact
         */
        void recordArtifactAsProcessed(
                final Set<URI> artifactURIsProcessed,
                final Collection<FullAndPartURIs> downloadsForThisArtifact) {
            artifactURIsProcessed.add(canonicalURIWithinEAR());
            downloadsForThisArtifact.add(downloadInfo());
        }

    }

    /**
     * An Artifact that will be downloaded as it is in its current location
     * in the expanded directory...that is, all files from archive deployments
     * and non-submodule JARs from directory deployments.
     */
    private abstract class RealArtifact extends Artifact {
        private final URI uriWithinEAR;
        
        RealArtifact(final File artifactFile) {
            super(artifactFile);
            uriWithinEAR = earURI.relativize(artifactFile.toURI());
        }
        
        @Override
        URI canonicalURIWithinEAR() {
            return uriWithinEAR;
        }
    }

    /**
     * An Artifact that does not actually exist by its canonical URI in the
     * expanded directory because it is a submodule as part of a directory
     * deployment.
     */
    private class VirtualJARArtifact extends JARArtifact {
        private final URI virtualURI;

        VirtualJARArtifact(final URI virtualURI) throws IOException {
            super(JAROfExpandedSubmodule(virtualURI));
            this.virtualURI = virtualURI;
        }

        @Override
        URI canonicalURIWithinEAR() {
            return virtualURI;
        }
    }

    /**
     * An Artifact that is a directory referenced by a JAR's Class-Path.
     */
    private class DirectoryArtifact extends RealArtifact {

        DirectoryArtifact(final File dirFile) {
            super(dirFile);
        }

        @Override
        void processArtifact(
                final Set<URI> artifactURIsProcessed,
                final Collection<FullAndPartURIs> downloadsForThisArtifact,
                final Collection<FullAndPartURIs> downloadsForReferencedArtifacts) throws IOException {

            final URI uriWithinEAR = canonicalURIWithinEAR();
            if (artifactURIsProcessed.contains(uriWithinEAR)) {
                return;
            }
            artifactURIsProcessed.add(uriWithinEAR);

            /*
             * Iterate through this directory and its subdirectories, marking
             * each contained file for download.
             */

            for (File f : physicalFile().listFiles()) {
                if (f.isDirectory()) {
                    final Artifact nestedDirArtifact = newArtifact(this, f.toURI());
                    if (nestedDirArtifact != null) {
                        nestedDirArtifact.processArtifact(
                                artifactURIsProcessed,
                                downloadsForReferencedArtifacts,
                                downloadsForReferencedArtifacts);
                    }
                } else {
                    /*
                     * The file inside the directory is not another directory,
                     * so simply include it as another download with no
                     * special processing.
                     */
                    URI fileURI = f.toURI();
                    /*
                     * Note that for Java Web Start support we need to sign JARs.
                     * Even though this JAR appears as just another file in this
                     * directory that was referenced from some JAR file in the app,
                     * it might actually be referenced directly from the Class-Path
                     * of JAR that will appear on the runtime class path.  That
                     * means we'll want to sign the JAR.
                     */
                    if (f.getName().endsWith(".jar")) {
                        fileURI = signedJARManager.addJAR(fileURI);
                    }
                    final URI fileURIWithinEAR = earDirUserURI(dc()).resolve(earURI.relativize(fileURI));
                    Artifacts.FullAndPartURIs fileDependency =
                            new FullAndPartURIs(fileURI, fileURIWithinEAR);
//                                earURI.relativize(fileURI));
                    downloadsForReferencedArtifacts.add(fileDependency);
                    artifactURIsProcessed.add(fileURIWithinEAR);
                }
            }
        }
    }

    /**
     * A JAR artifact that actually exists where its canonical URI implies it would be.
     */
    private class JARArtifact extends RealArtifact {

        JARArtifact(final File artifactFile) {
            super(artifactFile);
        }

        @Override
        void processArtifact(
                final Set<URI> artifactURIsProcessed,
                final Collection<FullAndPartURIs> downloadsForThisArtifact,
                final Collection<FullAndPartURIs> downloadsForReferencedArtifacts) throws IOException {

            /*
             * Add the JAR to the collection that must be downloaded to support
             * the Java Web Start launch.  If the JAR is already signed it is
             * simply added to the signed JAR manager.  If it is not signed by
             * the developer then we sign it now so Java Web Start will be OK
             * granting it the necessary permissions.
             */
            final URI fileURI = physicalFile().toURI();
            final URI uriWithinEAR = canonicalURIWithinEAR();
            if (artifactURIsProcessed.contains(uriWithinEAR)) {
                return;
            }
            final URI uriWithinAnchor = earDirUserURI(dc()).resolve(uriWithinEAR);
            Artifacts.FullAndPartURIs fileDependency =
                    new FullAndPartURIs(fileURI, uriWithinAnchor);
            downloadsForReferencedArtifacts.add(fileDependency);
            signedJARManager.addJAR(uriWithinAnchor, fileURI);
            recordArtifactAsProcessed(artifactURIsProcessed, downloadsForThisArtifact);

            Manifest jarManifest;
            try {
                final JarFile dependentJar = new JarFile(physicalFile());
                try {
                  jarManifest = dependentJar.getManifest();
                } finally 
                {
                  dependentJar.close();
                }
                if (jarManifest == null) {
                    logger.log(Level.WARNING,
                            "enterprise.deployment.appclient.jws.nomf",
                            fileURI.toASCIIString());
                    return;
                }
            } catch (IOException ex) {
                /*
                 * The JAR does not exist or it's not readable as a JAR.
                 * Ignore it.
                 */
                return;
            } 

            final Attributes mainAttrs = jarManifest.getMainAttributes();
            if (mainAttrs == null) {
                logger.log(Level.WARNING,
                            "enterprise.deployment.appclient.jws.depJarNoMainAttrs",
                            fileURI.toASCIIString());
                    return;
            }

            final String jarClassPath = mainAttrs.getValue(Attributes.Name.CLASS_PATH);
            if (jarClassPath != null) {
                for (String elt : jarClassPath.split(" ")) {
                    /*
                     * A Class-Path list might have multiple spaces as a separator.
                     * Ignore empty elements.
                     */
                    if (elt.trim().length() > 0) {
                        final URI eltURI = URI.create(elt);
                        final Artifact classPathArtifact =
                                newArtifact(
                                    this, eltURI);
                        if (classPathArtifact != null) {
                            classPathArtifact.processArtifact(
                                artifactURIsProcessed,
                                downloadsForReferencedArtifacts,
                                downloadsForReferencedArtifacts);
                        }
                    }
                }
            }
        }
    }
}
