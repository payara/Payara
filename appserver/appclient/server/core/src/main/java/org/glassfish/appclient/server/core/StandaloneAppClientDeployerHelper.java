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

import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.ApplicationClientDescriptor;
import com.sun.enterprise.deployment.archivist.AppClientArchivist;
import com.sun.enterprise.deployment.deploy.shared.JarArchive;
import com.sun.enterprise.deployment.deploy.shared.OutputJarArchive;
import com.sun.logging.LogDomains;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.appclient.server.core.jws.JavaWebStartInfo;
import org.glassfish.appclient.server.core.jws.servedcontent.DynamicContent;
import org.glassfish.appclient.server.core.jws.servedcontent.FixedContent;
import org.glassfish.appclient.server.core.jws.servedcontent.TokenHelper;
import org.glassfish.deployment.common.Artifacts;
import org.glassfish.deployment.common.Artifacts.FullAndPartURIs;
import org.glassfish.deployment.versioning.VersioningSyntaxException;
import org.glassfish.deployment.versioning.VersioningUtils;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.deployment.ExtendedDeploymentContext;

/**
 * Encapsulates logic that is specific to stand-alone app client
 * deployments regarding the generation of app client files.
 * <p>
 * The facade JAR file - ${appName}Client.jar - will reside at the top of
 * the user's local directory and will refer to the developer's original
 * app client JAR which will reside in the ${appName}Client subdirectory
 * within the user's download directory.
 *
 * @author tjquinn
 */
public class StandaloneAppClientDeployerHelper extends AppClientDeployerHelper {

    private static final Logger logger = Logger.getLogger(JavaWebStartInfo.APPCLIENT_SERVER_MAIN_LOGGER, 
                JavaWebStartInfo.APPCLIENT_SERVER_LOGMESSAGE_RESOURCE);
    
    private Set<FullAndPartURIs> clientLevelDownloads = null;

    StandaloneAppClientDeployerHelper(final DeploymentContext dc, 
            final ApplicationClientDescriptor bundleDesc,
            final AppClientArchivist archivist,
            final ClassLoader gfClientModuleClassLoader,
            final Application application,
            final ServiceLocator habitat) throws IOException {
        super(dc, bundleDesc, archivist, gfClientModuleClassLoader, 
                application, habitat);
    }

    /**
     * Returns the name (no path, no type) of the facade JAR.  This is used
     * in both creating the full name and URI of the facade as well as for
     * the name of a subdirectory in the user's download directory.
     * 
     * @param dc
     * @return
     */
    private String facadeNameOnly(DeploymentContext dc) {
        DeployCommandParameters params = dc.getCommandParameters(DeployCommandParameters.class);
        final String appName = params.name();
        try {
            return VersioningUtils.getUntaggedName(appName) + "Client";
        } catch (VersioningSyntaxException ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return appName + "Client";
    }

    @Override
    public File rootForSignedFilesInApp() {
        return new File(dc().getScratchDir("xml"), "signed/");
    }

    @Override
    protected void prepareJARs() throws IOException, URISyntaxException {
        super.prepareJARs();
        /*
         * The app client JAR will have been expanded and deleted, so create
         * a JAR from the expanded directory.
         */
        copyOriginalAppClientJAR(dc());
    }
    
    @Override
    protected void addTopLevelContentToClientFacade(OutputJarArchive facadeArchive) throws IOException{
        addClientPolicyFiles(facadeArchive);
    }

    @Override
    public void createAndAddLibraryJNLPs(AppClientDeployerHelper helper, TokenHelper tHelper, Map<String, DynamicContent> dynamicContent) {
    }
    
    /**
     * Copies a file's contents to the top-level JAR generated by this
     * deployment. 
     * 
     * @param clientFacadeArchive
     * @param f
     * @param path
     * @throws IOException 
     */
    @Override
    protected void copyFileToTopLevelJAR(final OutputJarArchive clientFacadeArchive, final File f, final String path) throws IOException {
        copyToArchive(f, clientFacadeArchive, path);
    }
    
    private void copyToArchive(final File inputFile, final OutputJarArchive outputArchive, final String pathInJar) throws IOException {
        final OutputStream os = outputArchive.putNextEntry(pathInJar);
        final InputStream is = new BufferedInputStream(new FileInputStream(inputFile));
        final byte[] buffer = new byte[512];
        int bytesRead;
        try {
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        } finally {
            try {
                os.flush();
            } finally {
                is.close();
            }
        }
    }

    

    @Override
    public FixedContent fixedContentWithinEAR(String uriString) {
        /*
         * There can be no fixed content within the EAR for a stand-alone
         * app client.
         */
        return null;
    }

    /**
     * Returns the file name and type of the facade.
     * <p>
     * For stand-alone app clients, the facade is ${appName}Client.jar.
     * @param dc
     * @return
     */
    @Override
    protected String facadeFileNameAndType(DeploymentContext dc) {
        return facadeNameOnly(dc) + ".jar";
    }


    /**
     * Returns the URI for the generated facade JAR.
     * <p>
     * The facade is ${appName}Client.jar and for stand-alone app clients
     * is stored at generated/xml/${appName}/${appName}Client.jar.
     * @param dc
     * @return
     */
    @Override
    public URI facadeServerURI(DeploymentContext dc) {
        File genXMLDir = dc.getScratchDir("xml");
        return genXMLDir.toURI().resolve(facadeFileNameAndType(dc));
    }

    /**
     * Returns the URI for the facade within the user's download directory.
     * <p>
     * The facade for a stand-alone app client will reside at the top level
     * of the user's download directory.
     * @param dc
     * @return
     */
    @Override
    public URI facadeUserURI(DeploymentContext dc) {
        return URI.create(facadeFileNameAndType(dc));
    }

    @Override
    public URI groupFacadeUserURI(DeploymentContext dc) {
        return null;
    }

    @Override
    public URI groupFacadeServerURI(DeploymentContext dc) {
        return null;
    }



    /**
     * Returns the URI for the developer's original app client JAR within the
     * user's download directory.
     *
     * @param dc
     * @return
     */
    @Override
    public URI appClientUserURI(DeploymentContext dc) {
        return URI.create(facadeNameOnly(dc) + '/' + appClientURIWithinApp(dc));
    }

    /**
     * Returns the URI to the server 
     * @param dc
     * @return
     */
    @Override
    public URI appClientServerURI(DeploymentContext dc) {
        File genXMLDir = dc.getScratchDir("xml");
        return genXMLDir.toURI().resolve(appClientURIWithinApp(dc));
    }

    @Override
    public URI appClientServerOriginalAnchor(DeploymentContext dc) {
        return ((ExtendedDeploymentContext) dc).getOriginalSource().getURI();
    }



    /**
     * Returns the URI for the app client within the artificial containing
     * app.  For stand-alone clients the module URI is reported as
     * the directory into which the app client JAR was expanded, without the
     * "_jar" suffix.  To that we add .jar to get a URI at the "top-level"
     * of the pseudo-containing app.
     *
     * @param dc
     * @return
     */
    @Override
    public URI appClientURIWithinApp(DeploymentContext dc) {

        String uriText = appClientDesc().getModuleDescriptor().getArchiveUri();
        String uriRoot = "";
        String archiveName = uriText;
        int lastIndex = uriText.lastIndexOf("/");
        if(lastIndex > -1) {
            uriRoot = uriText.substring(0, lastIndex);
            archiveName = uriText.substring(lastIndex);
        }
        try {
            archiveName = VersioningUtils.getUntaggedName(archiveName);
        } catch (VersioningSyntaxException ex) {
           logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
//        if ( ! uriText.endsWith(".jar")) {
//            uriText += ".jar";
//        }
        if ( ! archiveName.endsWith(".jar")) {
            archiveName += ".jar";
        }
        uriText = uriRoot + archiveName;
        return URI.create(uriText);
    }

    @Override
    protected Set<FullAndPartURIs> clientLevelDownloads() throws IOException {
        if (clientLevelDownloads == null) {
            /*
             * Stand-alone client deployments involve these downloads:
             * 1. the original app client JAR,
             * 2. the facade JAR 
             */
            Set<FullAndPartURIs> downloads = new HashSet<FullAndPartURIs>();
            downloads.add(new Artifacts.FullAndPartURIs(
                    appClientServerURI(dc()),
                    appClientUserURI(dc())));
            downloads.add(new Artifacts.FullAndPartURIs(
                    facadeServerURI(dc()),
                    facadeUserURI(dc())));
            clientLevelDownloads = downloads;
        }
        return clientLevelDownloads;
    }

    @Override
    public Set<FullAndPartURIs> earLevelDownloads() throws IOException {
        return Collections.EMPTY_SET;
    }

    @Override
    public URI appClientUserURIForFacade(DeploymentContext dc) {
        return appClientUserURI(dc);
    }

    @Override
    public URI URIWithinAppDir(DeploymentContext dc, URI absoluteURI) {
        return dc.getSource().getURI().relativize(absoluteURI);
    }

    @Override
    public String pathToAppclientWithinApp(DeploymentContext dc) {
        return "";
    }



    @Override
    protected String facadeClassPath() {
        /*
         * For app client deployments, the facade class path refers only
         * to the developer's original JAR, renamed to ${name}.orig.jar
         * (or some similar name using orig-${unique-number} to avoid
         * naming collisions.
         */
        return appClientUserURI(dc()).toASCIIString();
    }
    protected void copyOriginalAppClientJAR(final DeploymentContext dc) throws IOException {
        ReadableArchive originalSource = ((ExtendedDeploymentContext) dc).getOriginalSource();
        originalSource.open(originalSource.getURI());
        OutputJarArchive target = new OutputJarArchive();
        target.create(appClientServerURI(dc));
        /*
         * Copy the manifest explicitly because ReadableArchive.entries()
         * excludes the manifest.
         */
        Manifest originalManifest = originalSource.getManifest();
        OutputStream os = target.putNextEntry(JarFile.MANIFEST_NAME);
        originalManifest.write(os);
        target.closeEntry();
        copyArchive(originalSource, target, Collections.EMPTY_SET);
        target.close();
        originalSource.close();
    }

    @Override
    protected String PUScanTargets() {
        return null;
    }



}
