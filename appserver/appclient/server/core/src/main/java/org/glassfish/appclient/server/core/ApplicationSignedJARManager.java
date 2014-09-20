/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2014 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.deploy.shared.ArchiveFactory;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.appclient.server.core.jws.JavaWebStartInfo;
import org.glassfish.appclient.server.core.jws.servedcontent.ASJarSigner;
import org.glassfish.appclient.server.core.jws.servedcontent.AutoSignedContent;
import org.glassfish.appclient.server.core.jws.servedcontent.FixedContent;
import org.glassfish.appclient.server.core.jws.servedcontent.StaticContent;
import org.glassfish.hk2.api.ServiceLocator;

/**
 * Records information about JARs from an EAR that are used by an
 * app client.  Although JARs can be signed by multiple certificates, this
 * class ultimately associates each JAR with at most one alias with which
 * it was signed.  (This is typically used by the Java Web Start support to
 * group like-signed JARs into the same generated JNLP.  Java Web Start requires
 * that all JARs listed in a single JNLP document be signed by the same cert or
 * be unsigned.)  By organizing the signed JARs by the signing alias used for each,
 * we can easily find all JARs signed by a given alias and then list them
 * in the same generated JNLP.
 * <p>
 * A client class should instantiate the manager, then invoke addJar any number
 * of times, then invoke aliasToContent to retrieve the map from each alias to
 * the corresponding (relativeURI, StaticContent) pair.
 * <p>
 * If an added JAR is already signed by the developer we do not sign it again, but
 * simply add it to the data structures.  If an added JAR is not signed we
 * arrange for it to be auto-signed and the signed version will be the one
 * served to Java Web Start requests.
 *
 * @author tjquinn
 */
public class ApplicationSignedJARManager {

    private final Map<URI,Collection<String>> relURIToSigningAliases =
                new HashMap<URI,Collection<String>>();
    private final Map<String,Collection<URI>> signingAliasToRelURIs =
                new HashMap<String,Collection<URI>>();

    /**
     * maps each alias to a map.  map entries link the relative URI (used as the
     * key in the Grizzly adapter's key-to-content map) to the StaticContent
     * instance for that JAR.  In this map each served JAR is associated with
     * only one alias, even if a JAR is signed by multiple certs.
     */
    private Map<String,Map<URI,StaticContent>> selectedAliasToContentMapping = null;

    private final ArchiveFactory archiveFactory;

    private final String autoSigningAlias;

    private final ASJarSigner jarSigner;

    private final URI EARDirectoryServerURI;

    private final DeploymentContext dc;

    private final AppClientDeployerHelper helper;

    private final Map<URI,StaticContent> relURIToContent =
            new HashMap<URI,StaticContent>();

    public ApplicationSignedJARManager(
            final String autoSigningAlias, 
            final ASJarSigner jarSigner,
            final ServiceLocator habitat,
            final DeploymentContext dc,
            final AppClientDeployerHelper helper,
            final URI EARDirectoryServerURI,
            final URI EARDirectoryUserURI) {
        this.autoSigningAlias = autoSigningAlias;
        this.jarSigner = jarSigner;
        this.EARDirectoryServerURI = EARDirectoryServerURI;
        archiveFactory = habitat.getService(ArchiveFactory.class);
        this.dc = dc;
        this.helper = helper;
    }

    /**
     * Adds a JAR to the manager, returning the URI for the file to be served.
     * The URI within the anchor is derived from the absolute URI relative
     * to the EAR's anchor on the server.
     * @param absJARURI absolute URI of the unsigned file.
     * @return URI to the file to be served
     * @throws IOException
     */
    public URI addJAR(final URI absJARURI) throws IOException {
        final URI jarURIRelativeToApp = EARDirectoryServerURI.relativize(absJARURI);
        return addJAR(jarURIRelativeToApp, absJARURI);
    }

    /**
     * Adds a JAR to the manager, returning the URI to the file to be
     * served.  This might be an auto-signed file if the original JAR is
     * unsigned.
     * @param uriWithinAnchor relative URI to the JAR within the anchor directory for the app
     * @param jarURI URI to the JAR file in the app to be served
     * @return URI to the JAR file to serve (either the original file or an auto-signed copy of the original)
     * @throws IOException
     */
    public URI addJAR(final URI uriWithinAnchor, final URI absJARURI) throws IOException {
        /*
         * This method accomplishes three things:
         *
         * 1. Adds an entry to the map from relative URIs to the corresponding
         * static content for the JAR, creating an auto-signed content instance
         * if needed for an unsigned JAR.
         *
         * 2. Adds to the map from relative URI to aliases with which the JAR
         * is signed.
         *
         * 3. Adds to the map from alias to relative URIs signed with that alias.
         */
        Map.Entry<URI,StaticContent> result; // relative URI -> StaticContent

        final ReadableArchive arch = archiveFactory.openArchive(absJARURI);
        final Manifest archiveMF = arch.getManifest();
        if (archiveMF == null) {
            return null;
        }
        if ( ! isArchiveSigned(archiveMF)) {
            /*
             * The developer did not sign this JARs, so arrange for it to be
             * auto-signed.
             */

            result = autoSignedAppContentEntry(uriWithinAnchor, absJARURI);
            updateAliasToURIs(result.getKey(), autoSigningAlias);
            updateURIToAliases(result.getKey(), autoSigningAlias);
        } else {
            /*
             * The developer did sign this JAR, possibly with many certs.
             * For each cert add an association between the signing alias and
             * the JAR.
             */
            result = developerSignedAppContentEntry(absJARURI);
            Collection<String> aliasesUsedToSignJAR = new ArrayList<String>();
            for (Enumeration<String> entryNames = arch.entries("META-INF/");
                 entryNames.hasMoreElements(); ) {
                final String entryName = entryNames.nextElement();
                final String alias = signatureEntryName(entryName);
                updateURIToAliases(result.getKey(), alias);
            }
            addAliasToURIsEntry(result.getKey(), aliasesUsedToSignJAR);
        }
            
        arch.close();
        return result.getKey();
    }

    public Map<String,Map<URI,StaticContent>> aliasToContent() {
        if (selectedAliasToContentMapping == null) {
            selectedAliasToContentMapping = pruneMaps();
        }
        return selectedAliasToContentMapping;
    }

    private void addAliasToURIsEntry(final URI relURI,
            final Collection<String> aliases) throws IOException {
        relURIToSigningAliases.put(relURI, aliases);
        for (String alias : aliases) {
            updateAliasToURIs(relURI, alias);
        }
    }
    
    private void updateURIToAliases(final URI relURI,
            final String alias) throws IOException {
        Collection<String> aliasesForJAR = relURIToSigningAliases.get(relURI);
        if (aliasesForJAR == null) {
            aliasesForJAR = new ArrayList<String>();
            relURIToSigningAliases.put(relURI, aliasesForJAR);
        }
        aliasesForJAR.add(alias);
    }

    private void updateAliasToURIs(final URI relURI,
            final String alias) throws IOException {
        Collection<URI> urisForAlias = signingAliasToRelURIs.get(alias);
        if (urisForAlias == null) {
            urisForAlias = new ArrayList<URI>();
            signingAliasToRelURIs.put(alias, urisForAlias);
        }
        urisForAlias.add(relURI);
    }

    private Map.Entry<URI,StaticContent> developerSignedAppContentEntry(URI absURIToFile) {
        final URI jarURIRelativeToApp = EARDirectoryServerURI.relativize(absURIToFile);
        StaticContent content = relURIToContent.get(absURIToFile);
        if (content == null) {
            content = new FixedContent(new File(absURIToFile));
            relURIToContent.put(jarURIRelativeToApp, content);
        }
        return new AbstractMap.SimpleEntry<URI,StaticContent>(
            jarURIRelativeToApp, content);
    }

    public StaticContent staticContent(final URI jarURIRelativeToApp) {
        return relURIToContent.get(jarURIRelativeToApp);
    }
    
    /*
     * Returns information about an auto-signed JAR for a given absolute URI and
     * alias, creating the auto-signed content object and adding it to the
     * data structures if it is not already present.
     */
    private synchronized Map.Entry<URI,StaticContent> autoSignedAppContentEntry(
            final URI jarURIRelativeToApp,
            final URI absURIToFile) throws FileNotFoundException {

        StaticContent content = relURIToContent.get(jarURIRelativeToApp);
        if (content == null) {
            final File unsignedFile = new File(absURIToFile);
            final File signedFile = signedFileForLib(jarURIRelativeToApp, unsignedFile);
            content = new AutoSignedContent(unsignedFile, signedFile, autoSigningAlias, jarSigner, jarURIRelativeToApp.toASCIIString(),
                    helper.appName());
            relURIToContent.put(jarURIRelativeToApp, content);
        } else {
            if (content instanceof AutoSignedContent) {
                content = AutoSignedContent.class.cast(content);
            } else {
                throw new RuntimeException(content.toString() + " != AutoSignedContent");
            }
        }
        return new AbstractMap.SimpleEntry(jarURIRelativeToApp, content);
    }

    private File signedFileForLib(final URI relURI, final File unsignedFile) {
        return JavaWebStartInfo.signedFileForProvidedAppFile(relURI, unsignedFile, helper, dc);
    }

    /**
     * Returns the signature file name (no path, no suffix) if the specified
     * entry name matches the pattern of a signature file in a JAR.
     * @param entryName name to check
     * @return signature file name; null if the entry name does not match the pattern
     */
    private String signatureEntryName(final String entryName) {
        final int firstSlash = entryName.indexOf('/');
        final int lastSlash = entryName.lastIndexOf('/');
        return ((entryName.startsWith("META-INF/")
                && firstSlash == lastSlash && firstSlash != -1)
                && (entryName.endsWith(".SF")))

                ? entryName.substring(firstSlash + 1, entryName.indexOf(".SF"))
                : null;
    }

    private boolean isArchiveSigned(final Manifest archiveMF) throws IOException {
        /*
         * Signature files are *.SF, but looking through all the entries for
         * ones that match *.SF could be expensive if there are many entries.
         * Instead check the manifest to
         * see if it contains per-entry attributes and, if so, if the first
         * entry has a x-Digest-y entry-level attribute.
         */
        final Map<String,Attributes> perEntryAttrs = archiveMF.getEntries();
        boolean jarIsSigned = false;
        for (Map.Entry<String,Attributes> entry : perEntryAttrs.entrySet()) {
            for (Object attrKey : entry.getValue().keySet()) {
                if (attrKey.toString().contains("-Digest-") || attrKey.toString().contains("-Digest:")) {
                    jarIsSigned = true;
                    break;
                }
            }
            /*
             * We need to look only at the first entry because every entry
             * of a JAR file is recorded as signed in the manifest.
             */
            break;
        }
        return jarIsSigned;
    }

    private Map<String,Map<URI,StaticContent>> pruneMaps() {
        /*
         * We'll eventually generate possibly multiple JNLP documents, one for each
         * different signing cert and each listing the JARs signed using that cert.
         * Java Web Start prompts end users for each untrusted cert. that was
         * used to sign the JARs in a JNLP.  During deployment (which is when this code runs)
         * we cannot tell what certs or trusted authorities might be on an
         * end-users's system.  So we would like to minimize the number of
         * different JNLPs which might help reduce the number of prompts the
         * user will see.
         *
         * We have a relationship between JARs and signing aliases.  Ideally
         * we'd truly minimize the number of aliases but that's a hard (i.e.,
         * computationally complex) problem and it's unlikely that real apps will contain
         * JARs signed by large numbers of different certs.  So we'll do as
         * good a job as we can, but quickly.
         *
         * If a JAR is signed by exactly one cert then we must include that cert
         * and we might as well associate any other JARs signed by that cert
         * and others with that cert.  Then we'll process any
         * unprocessed JARs by choosing for each the alias with which it was
         * signed with the largest number of other JARs also signed by that
         * alias.  This is not guaranteed to be optimal but it should be
         * pretty good and will be fast.
         */

        final Set<URI> processedJARs = new HashSet<URI>();
        final Map<String,Map<URI,StaticContent>> selectedAliases =
                new HashMap<String,Map<URI,StaticContent>>();

        for (Map.Entry<URI,Collection<String>> entry : relURIToSigningAliases.entrySet()) {
            if ( ! processedJARs.contains(entry.getKey())) {
                if (entry.getValue().size() == 1) {
                    processURI(processedJARs, selectedAliases,
                            entry.getKey(), entry.getValue().iterator().next());
                }
            }
        }

        /*
         * We've handled all JARs that have just one signing alias.  Now process
         * any remaining JARs.
         */
        for (Map.Entry<URI,Collection<String>> entry : relURIToSigningAliases.entrySet()) {
            if ( ! processedJARs.contains(entry.getKey())) {
                processURI(processedJARs, selectedAliases,
                        entry.getKey(), entry.getValue());
            }
        }
        return selectedAliases;
    }

    private void processURI(final Set<URI> processedJARs,
            final Map<String,Map<URI,StaticContent>> selectedAliases,
            final URI relURI,
            final String alias) {
        Map<URI,StaticContent> urisForSelectedAlias = selectedAliases.get(alias);
        if (urisForSelectedAlias == null) {
            urisForSelectedAlias = new HashMap<URI,StaticContent>();
            selectedAliases.put(alias, urisForSelectedAlias);
        }
        /*
         * Add this URI to the URIs to be associated with the specified alias.
         */
        urisForSelectedAlias.put(relURI, relURIToContent.get(relURI));

        /*
         * Record that we've processed this URI so we don't do so again.
         */
        processedJARs.add(relURI);

        /*
         * Now that we know we need to handle this alias, mark all other JARs
         * that are associated with this alias (and perhaps others) to be
         * finally grouped with this alias alone.
         */
        for (URI otherURI : signingAliasToRelURIs.get(alias)) {
            urisForSelectedAlias.put(otherURI, relURIToContent.get(otherURI));
            processedJARs.add(otherURI);
        }
    }

    private void processURI(final Set<URI> processedJARs,
            final Map<String,Map<URI,StaticContent>> selectedAliases,
            final URI uri,
            final Collection<String> aliases) {
        /*
         * The algorithm we use to choose which of the multiple aliases to use
         * for this JAR could be anything.  We'll just choose the first one.
         */
        processURI(processedJARs, selectedAliases, uri, aliases.iterator().next());
    }
}
