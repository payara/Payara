/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.deployment.deploy.shared.InputJarArchive;
import com.sun.enterprise.deployment.deploy.shared.OutputJarArchive;
import com.sun.enterprise.util.io.FileUtils;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.deployment.OpsParams;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.deployment.archive.WritableArchive;
import org.glassfish.internal.deployment.ExtendedDeploymentContext;
import org.glassfish.deployment.versioning.VersioningUtils;

import org.glassfish.logging.annotation.LogMessageInfo;

/**
 * Writes a client JAR file, if one is needed, suitable for downloading.
 * <p>
 * Deployers will use the {@link ClientArtifactsManager} to record files which
 * they generate that need to be included in the downloadable client JAR. Examples
 * are EJB stubs, web services artifacts, and JARs related to app clients.  
 * Once all deployers have run through the prepare phase this class can be used to
 * create the client JAR, collecting those client artifacts into the 
 * generated JAR, and adding the generated client JAR to the list of files
 * related to this application that are available for download.
 * 
 * @author tjquinn
 */
public class ClientJarWriter {
    
    public static final Logger deplLogger = org.glassfish.deployment.common.DeploymentContextImpl.deplLogger;

    @LogMessageInfo(message = "Exception caught:  {0}", level="WARNING")
    private static final String EXCEPTION_CAUGHT = "NCLS-DEPLOYMENT-00004";

    private final String LINE_SEP = System.getProperty("line.separator");
    private final ExtendedDeploymentContext deploymentContext;
    private final String name;
    
    private Map<URI,JarFile> jarFiles = new HashMap<URI,JarFile>();
    
    public ClientJarWriter(final ExtendedDeploymentContext deploymentContext) {
        this.deploymentContext = deploymentContext;
        name = VersioningUtils.getUntaggedName(deploymentContext.getCommandParameters(DeployCommandParameters.class).name());
    }
    
    public void run() throws IOException {
        /*
         * Only generate the JAR if we would not already have done so.
         */
        if (isArtifactsPresent()) {
            deplLogger.log(Level.FINE, "Skipping possible client JAR generation because it would already have been done");
            return;
        }
        
        final Artifacts downloadableArtifacts =
                DeploymentUtils.downloadableArtifacts(deploymentContext);
        final Artifacts generatedArtifacts =
                DeploymentUtils.generatedArtifacts(deploymentContext);
        final File clientJarFile = createClientJARIfNeeded(deploymentContext, name);
        if (clientJarFile == null) {
            deplLogger.log(Level.FINE, "No client JAR generation is needed.");
        } else {
            deplLogger.log(Level.FINE, "Generated client JAR {0} for possible download", clientJarFile.getAbsolutePath());
            downloadableArtifacts.addArtifact(clientJarFile.toURI(), clientJarFile.getName());
            generatedArtifacts.addArtifact(clientJarFile.toURI(), clientJarFile.getName());
        }
    }
    
    private boolean isArtifactsPresent() {
        return deploymentContext.getCommandParameters(OpsParams.class).origin.isArtifactsPresent();
    }
    
    private File createClientJARIfNeeded(final ExtendedDeploymentContext deploymentContext,
            final String appName) throws IOException {
        final ClientArtifactsManager clientArtifactsManager =
                ClientArtifactsManager.get(deploymentContext);
        if (clientArtifactsManager.isEmpty()) {
            return null;
        }
        
        /*
         * Make sure the scratch directories are there.  
         */
        deploymentContext.prepareScratchDirs();
        
        final String generatedClientJARName = generatedClientJARNameAndType(appName);
        final File generatedClientJARFile = new File(deploymentContext.getScratchDir("xml"), 
                generatedClientJARName);
        /*
         * The app client deployer might have already created the generated JAR
         * file.  In that case we need to merge its contents with what has been
         * registered with the client artifacts manager.
         */
        File movedPreexistingFile = null;
        if (generatedClientJARFile.exists()) {
            try {
                movedPreexistingFile = mergeContentsToClientArtifactsManager(generatedClientJARFile, clientArtifactsManager);
            } catch (URISyntaxException ex) {
                throw new IOException(ex);
            }
        }
        
        /*
         * We need our own copy of the artifacts collection because we might
         * need to add the manifest to it if a manifst is not already in the collection.
         * The client artifacts manager returns an unalterable collection.
         */
        final Collection<Artifacts.FullAndPartURIs> artifacts = new ArrayList<Artifacts.FullAndPartURIs>(clientArtifactsManager.artifacts());
        
        OutputJarArchive generatedClientJAR = new OutputJarArchive();
        try {
            try {
                generatedClientJAR.create(generatedClientJARFile.toURI());

                if ( ! isManifestPresent(artifacts)) {
                    /*
                     * Add a simple manifest.
                     */
                    deplLogger.log(Level.FINER, "Adding a simple manifest; one was not already generated");
                    addManifest(artifacts);
                }

                copyArtifactsToClientJAR(generatedClientJAR, artifacts);
            } finally {
                generatedClientJAR.close();
            }
        } catch (IOException ex) {
            if ( ! generatedClientJARFile.delete()) {
                generatedClientJARFile.deleteOnExit();
            }
        } finally {
            if (movedPreexistingFile != null) {
                FileUtils.deleteFileNowOrLater(movedPreexistingFile);
            }
        }
        return generatedClientJARFile;
    }
    
    private File mergeContentsToClientArtifactsManager(
            final File generatedClientJARFile,
            final ClientArtifactsManager clientArtifactsManager) throws IOException, URISyntaxException {
        /*
         * First, move the existing JAR to another name so when the caller
         * creates the generated client JAR it does not overwrite the existing
         * file created by the app client deployer.
         */
        final File movedGeneratedFile = File.createTempFile(generatedClientJARFile.getName(), ".tmp", generatedClientJARFile.getParentFile());
        FileUtils.renameFile(generatedClientJARFile, movedGeneratedFile);
        final ReadableArchive existingGeneratedJAR = new InputJarArchive();
        existingGeneratedJAR.open(movedGeneratedFile.toURI());
        try {
            for (Enumeration e = existingGeneratedJAR.entries(); e.hasMoreElements();) {
                final String entryName = (String) e.nextElement();
                final URI entryURI = new URI("jar", movedGeneratedFile.toURI().toASCIIString() + "!/" + entryName, null);
                final Artifacts.FullAndPartURIs uris = new Artifacts.FullAndPartURIs(entryURI, entryName);
                clientArtifactsManager.add(uris);
            }
            /*
             * Handle the manifest explicitly because the Archive entries()
             * method consciously omits it.
             */
            final Artifacts.FullAndPartURIs manifestURIs = new Artifacts.FullAndPartURIs(
                    new URI("jar", movedGeneratedFile.toURI().toASCIIString() + "!/" + JarFile.MANIFEST_NAME, null), JarFile.MANIFEST_NAME);
            clientArtifactsManager.add(manifestURIs);
            return movedGeneratedFile;
        } finally {
            existingGeneratedJAR.close();
        }
    }
    
    private boolean isManifestPresent(final Collection<Artifacts.FullAndPartURIs> artifacts) {
        boolean isManifestPresent = false;
        
        for (Artifacts.FullAndPartURIs a : artifacts) {
            isManifestPresent |= a.getPart().toASCIIString().equals(JarFile.MANIFEST_NAME);
        }
        return isManifestPresent;
    }
    
    private void addManifest(final Collection<Artifacts.FullAndPartURIs> artifacts) throws IOException {
        final File mfFile = File.createTempFile("clientmf", ".MF");
        final OutputStream mfOS = new BufferedOutputStream(new FileOutputStream(mfFile));
        try {
            final Manifest mf = new Manifest();
            mf.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
            mf.write(mfOS);
        } finally {
            mfOS.close();
        }
        artifacts.add(new Artifacts.FullAndPartURIs(mfFile.toURI(), JarFile.MANIFEST_NAME, true /* isTemporary */));
    }
    
    private static String generatedClientJARNameAndType(final String earName) {
        return generatedClientJARPrefix(earName) + ".jar";
    }

    private static String generatedClientJARPrefix(final String earName) {
        return earName + "Client";
    }

    private void copyArtifactsToClientJAR(
            final WritableArchive generatedClientJARArchive,
            final Collection<Artifacts.FullAndPartURIs> artifacts) throws IOException {
        final Set<String> pathsWrittenToJAR = new HashSet<String>();
        StringBuilder copiedFiles = (deplLogger.isLoggable(Level.FINER)) ? new StringBuilder() : null;
        for (Artifacts.FullAndPartURIs artifact : artifacts) {
            /*
             * Make sure all ancestor directories are present in the JAR
             * as empty entries.
             */
            String artPath = artifact.getPart().getRawPath();
            int previousSlash = artPath.indexOf('/');
            while (previousSlash != -1) {
                String partialAncestorPath = artPath.substring(0, previousSlash + 1);
                if ( ! pathsWrittenToJAR.contains(partialAncestorPath)) {
                    generatedClientJARArchive.putNextEntry(partialAncestorPath);
                    generatedClientJARArchive.closeEntry();
                    pathsWrittenToJAR.add(partialAncestorPath);
                }
                previousSlash = artPath.indexOf('/', previousSlash + 1);
            }
            
            OutputStream os = generatedClientJARArchive.putNextEntry(artifact.getPart().toASCIIString());
            InputStream is = null;
            try {
                final URI fullURI = artifact.getFull();
                final String scheme = fullURI.getScheme();
                if (scheme.equals("file")) {
                    is = new BufferedInputStream(new FileInputStream(new File(artifact.getFull())));
                } else if (scheme.equals("jar")) {
                    final String ssp = fullURI.getSchemeSpecificPart();
                    URI jarURI = new URI(ssp.substring(0, ssp.indexOf("!/")));
                    JarFile jf = jarFiles.get(jarURI);
                    if (jf == null) {
                        jf = new JarFile(new File(jarURI));
                        jarFiles.put(jarURI, jf);
                    }
                    final String entryName = ssp.substring(ssp.indexOf("!/") + 2);
                    final JarEntry jarEntry = jf.getJarEntry(entryName);
                    is = jf.getInputStream(jarEntry);
                } else {
                    throw new IllegalArgumentException(scheme + " != [file,jar]");
                }
                DeploymentUtils.copyStream(is, os);
                if (copiedFiles != null) {
                    copiedFiles.append(LINE_SEP).
                            append("  ").
                            append(artifact.getFull().toASCIIString()).
                            append(" -> ").
                            append(artifact.getPart().toASCIIString());
                }
            } catch (Exception ex) {
                deplLogger.log(Level.WARNING, EXCEPTION_CAUGHT, ex.getLocalizedMessage());
            } finally {
                if (is != null) {
                    is.close();
                }
                generatedClientJARArchive.closeEntry();
                if (artifact.isTemporary()) {
                    final File artifactFile = new File(artifact.getFull());
                    if ( ! artifactFile.delete()) {
                        artifactFile.deleteOnExit();
                    }
                }
            }
        }
        if (copiedFiles != null) {
            deplLogger.log(Level.FINER, copiedFiles.toString());
        }
    }
    
}
