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
 */

package org.glassfish.deployment.common;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.logging.annotation.LogMessageInfo;

/**
 * Records artifacts generated during deployment that need
 * to be included inside the generated app client JAR so they are accessible
 * on the runtime classpath.
 * <p>
 * An example: jaxrpc classes from web services deployment
 * <p>
 * Important node:  Artifacts added to this manager are NOT downloaded to
 * the client as separate files.  In contrast, they are added to the
 * generated client JAR file.  That generated JAR, along with other files needed
 * by the client, are downloaded.
 * <p>
 * A Deployer that needs to request for files to be downloaded to the client
 * as part of the payload in the http command response should instead use
 * DownloadableArtifactsManager.
 * <p>
 * The various {@code add} methods permit adding content in a variety of ways.
 * Ultimately we need to know two things: where is the physical file on the server
 * the content of which needs to be included in the client JAR, and what is the
 * relative path within the client JAR where the content should reside.  Look
 * carefully at the doc for each {@code add} method when choosing which to use.
 * <p>
 * An instance of this class can be stored in the deployment
 * context's transient app metadata so the various deployers can add to the
 * same collection and so the app client deployer can find it and
 * act on its contents.
 * <p>
 * Because other modules should add their artifacts before the the artifacts
 * have been consumed and placed into the client JAR file, the <code>add</code> methods do not permit
 * further additions once the {@link #artifacts} method has been invoked.
 *
 * @author tjuinn
 */
public class ClientArtifactsManager {

    public static final Logger deplLogger = org.glassfish.deployment.common.DeploymentContextImpl.deplLogger;

    @LogMessageInfo(message = "Artifact {0} identified for inclusion in app clients after one or more app clients were generated.", level="SEVERE",
            cause = "The application might specify that modules are to be processed in the order they appear in the application and an app client module appears before a module that creates an artifact to be included in app clients.",
            action = "Make sure, if the application specifies initialize-in-order as true, that the app clients appear after other modules which generated artifacts that should be accessible to app clients.")
    private static final String CLIENT_ARTIFACT_OUT_OF_ORDER = "NCLS-DEPLOYMENT-00025";

    @LogMessageInfo(message = "Artifact with relative path {0} expected at {1} but does not exist or cannot be read", level="SEVERE",
            cause = "The server is attempting to register an artifact to be included in the generated client JAR but the artifact does not exist or cannot be read",
            action = "This is an internal server error.  Please file a bug report.")
    private static final String CLIENT_ARTIFACT_MISSING = "NCLS-DEPLOYMENT-00026";
    
    @LogMessageInfo(message = "Artifact with relative path {0} from {1} collides with an existing artifact from file {2}", level="SEVERE",
            cause = "The server has created more than one artifact with the same relative path to be included in the generated client JAR file",
            action = "This is an internal server error.  Please file a bug report.")
    private static final String CLIENT_ARTIFACT_COLLISION = "NCLS-DEPLOYMENT-00027";
    
    private boolean isArtifactSetConsumed = false;
    
    private static final String CLIENT_ARTIFACTS_KEY = "ClientArtifacts";
    
    private final Map<URI,Artifacts.FullAndPartURIs> artifacts =
            new HashMap<URI,Artifacts.FullAndPartURIs>();
    
    /*
     * To verify sources that are JAR entries we need to make sure the 
     * requested entry exists in the specified JAR file.  To optimize the
     * opening of JAR files we record the JARs previous checked
     * here.
     */
    private final Map<URI,JarFile> jarFiles = new HashMap<URI,JarFile>();

    /**
     * Retrieves the client artifacts store from the provided deployment 
     * context, creating one and storing it back into the DC if none is 
     * there yet.
     * 
     * @param dc the deployment context to hold the ClientArtifactsManager object
     * @return the ClientArtifactsManager object from the deployment context (created
     * and stored in the DC if needed)
     */
    public static ClientArtifactsManager get(final DeploymentContext dc) {
        synchronized (dc) {
            ClientArtifactsManager result = dc.getTransientAppMetaData(
                    CLIENT_ARTIFACTS_KEY, ClientArtifactsManager.class);

            if (result == null) {
                result = new ClientArtifactsManager();
                dc.addTransientAppMetaData(CLIENT_ARTIFACTS_KEY, result);
            }
            return result;
        }
    }

    /**
     * Adds a new artifact to the collection of artifacts to be included in the
     * client JAR file so they can be delivered to the client during a
     * download.
     *
     * @param baseURI absolute URI of the base directory within which the
     * artifact lies
     * @param artifactURI absolute or relative URI where the artifact file resides
     * @throws IllegalStateException if invokes after the accumulated artifacts have been consumed
     */
    public void add(final URI baseURI, final URI artifactURI) {
        final URIPair uris = new URIPair(baseURI, artifactURI);
        final Artifacts.FullAndPartURIs newArtifact =
                    new Artifacts.FullAndPartURIs(
                    uris.absoluteURI, uris.relativeURI);
        add(newArtifact);
    }

    /**
     * Adds a new artifact to the collection of artifacts to be added to the
     * client facade JAR file so they can be delivered to the client during a
     * download.
     * <p>
     * The relative path within the client JAR will be computed using the position
     * of the artifact file relative to the base file.
     *
     * @param baseFile File for the base directory within which the artifact lies
     * @param artifactFile File for the artifact itself
     * @throws IllegalStateException if invoked after the accumulated artifacts have been consumed
     */
    public void add(final File baseFile, final File artifactFile) {
        add(baseFile.toURI(), artifactFile.toURI());
    }

    /**
     * Adds a new artifact to the collection of artifacts to be added to the 
     * client JAR file so they can be delivered to the client during a
     * download.
     * <p>
     * This method helps when the contents of a temporary file are to be included
     * in the client JAR, in which case the temp file might not reside in a
     * useful place relative to a base directory.  The caller can just specify
     * the relative path directly.
     * 
     * @param artifactFile file to be included in the client JAR
     * @param relativePath relative path within the JAR where the file's contents should appear
     * @param isTemporary whether the artifact file is a temporary file or not
     */
    public void add(final File artifactFile, final String relativePath, final boolean isTemporary) {
        final Artifacts.FullAndPartURIs artifact = new Artifacts.FullAndPartURIs(artifactFile.toURI(), relativePath, isTemporary);
        add(artifact);
    }
    
    public boolean isEmpty() {
        return artifacts.isEmpty();
    }
    
    /**
     * Adds a new artifact to the collection of artifacts to be added to the
     * client JAR file so they can be delivered to the client during a download.
     * <p>
     * Note that the "full" part of the FullAndPartURIs object can be of the 
     * form "jar:path-to-jar!entry-within-jar" 
     * 
     * @param artifact 
     */
    public void add(Artifacts.FullAndPartURIs artifact) {
        final boolean isLogAdditions = deplLogger.isLoggable(Level.FINE);
        if (isArtifactSetConsumed) {
            throw new IllegalStateException(
                    formattedString(CLIENT_ARTIFACT_OUT_OF_ORDER,
                        artifact.getFull().toASCIIString())
                    );
        } else {
            Artifacts.FullAndPartURIs existingArtifact =
                    artifacts.get(artifact.getPart());
            if (existingArtifact != null) {
                throw new IllegalArgumentException(
                        formattedString(CLIENT_ARTIFACT_COLLISION,
                            artifact.getPart().toASCIIString(),
                            artifact.getFull().toASCIIString(),
                            existingArtifact.getFull().toASCIIString())
                        );
            }
            /*
             * Verify at add-time that we can read the specified source.
             */
            final String scheme = artifact.getFull().getScheme();
            if (scheme.equals("file")) {
                verifyFileArtifact(artifact);
            } else if (scheme.equals("jar")) {
                verifyJarEntryArtifact(artifact);
            } else {
                throw new IllegalArgumentException(artifact.getFull().toASCIIString() + " != [file,jar]");
            }
            if (isLogAdditions) {
                final String stackTrace = (deplLogger.isLoggable(Level.FINER)) ? 
                    Arrays.toString(Thread.currentThread().getStackTrace()) : "";
                deplLogger.log(Level.FINE, "ClientArtifactsManager added {0}\n{1}", 
                        new Object[] {artifact, stackTrace});
            }
            artifacts.put(artifact.getPart(), artifact);
        }
    }
    
    private void verifyFileArtifact(final Artifacts.FullAndPartURIs artifact) {
        final URI fullURI = artifact.getFull();
        final File f = new File(fullURI);
        if ( ! f.exists() || ! f.canRead()) {
            throw new IllegalArgumentException(
                    formattedString(CLIENT_ARTIFACT_MISSING,
                        artifact.getPart().toASCIIString(),
                        fullURI.toASCIIString())
                    );
        }
    }
    
    private void verifyJarEntryArtifact(final Artifacts.FullAndPartURIs artifact) {
        final URI fullURI = artifact.getFull();
        final String ssp = fullURI.getSchemeSpecificPart();
        /*
         * See if we already have opened this JAR file.
         */
        JarFile jf = jarFiles.get(fullURI);
        if (jf == null) {
            try {
                final String jarFilePath = ssp.substring(0, ssp.indexOf("!/"));
                final URI jarURI = new URI(jarFilePath);
                final File jar = new File(jarURI);
                jf = new JarFile(jar);
                jarFiles.put(fullURI, jf);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        /*
         * Look for the specified entry.
         */
        final String entryName = ssp.substring(ssp.indexOf("!/") + 2);
        final JarEntry jarEntry = jf.getJarEntry(entryName);
        if (jarEntry == null) {
            throw new IllegalArgumentException(formattedString("enterprise.deployment.backend.appClientArtifactMissing",
                        artifact.getPart().toASCIIString(),
                        fullURI.toASCIIString())
                    );
        }
    }
    
    /**
     * Adds all artifacts in Collection to those to be added to the client
     * facade JAR.
     *
     * @param baseFile File for the base directory within which each artifact lies
     * @param artifactFiles Collection of File objects for the artifacts to be included
     * @throws IllegalStateException if invoked after the accumulated artifacts have been consumed
     */
    public void addAll(final File baseFile, final Collection<File> artifactFiles) {
        for (File f : artifactFiles) {
            add(baseFile, f);
        }
    }

    public boolean contains(final URI baseURI, final URI artifactURI) {
        return artifacts.containsKey(artifactURI);
    }

    public boolean contains(final File baseFile, final File artifactFile) {
        return contains(baseFile.toURI(), artifactFile.toURI());
    }
    /**
     * Returns the set (in unmodifiable form) of FullAndPartURIs for the
     * accumulated artifacts.
     * <p>
     * Note: Intended for use only by the app client deployer.
     *
     * @return all client artifacts reported by various deployers
     */
    public Collection<Artifacts.FullAndPartURIs> artifacts() {
        isArtifactSetConsumed = true;
        closeOpenedJARs();
        if (deplLogger.isLoggable(Level.FINE)) {
            deplLogger.log(Level.FINE, "ClientArtifactsManager returned artifacts");
        }
        return Collections.unmodifiableCollection(artifacts.values());
    }

    private void closeOpenedJARs() {
        for (JarFile jarFile : jarFiles.values()) {
            try {
                jarFile.close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        jarFiles.clear();
    }
    
    private String formattedString(final String key, final Object... args) {
        final String format = deplLogger.getResourceBundle().getString(key);
        return MessageFormat.format(format, args);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize(); //To change body of generated methods, choose Tools | Templates.
        if ( ! isArtifactSetConsumed) {
            closeOpenedJARs();
        }
    }

    /**
     * Represents a pair of URIs for an artifact, one being the URI where
     * the file already exists and one for the relative URI where the content
     * should appear in the generated client JAR file.
     */
    private static class URIPair {
        private final URI relativeURI;
        private final URI absoluteURI;

        /**
         * Creates a new URIPair, computing the relative URI for the pair using
         * the artifact URI; if it's relative, just copy it and if it's absolute
         * then relativize it to the base URI to compute the relative URI.
         * @param baseURI
         * @param artifactURI 
         */
        private URIPair(final URI baseURI, final URI artifactURI) {
            if (artifactURI.isAbsolute()) {
                absoluteURI = artifactURI;
                relativeURI = baseURI.relativize(absoluteURI);
            } else {
                relativeURI = artifactURI;
                absoluteURI = baseURI.resolve(relativeURI);
            }
        }
    }
}
