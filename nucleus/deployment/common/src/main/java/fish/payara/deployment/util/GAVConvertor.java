/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2021 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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

package fish.payara.deployment.util;

import org.glassfish.config.support.TranslatedConfigView;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Performs the functions required for converting GAV (group, artefact, version)
 * coordinates into a URI.
 * @author Andrew Pielage
 */
public final class GAVConvertor {
    
    private static final Logger logger = Logger.getLogger("PayaraMicro");

    private static final String defaultMavenRepository = "https://repo.maven.apache.org/maven2/";

    // Suppress default constructor
    private GAVConvertor() {
        throw new AssertionError();
    }
    
    /**
     * Returns a valid URI for a provided GAV (GroupId, ArtifactId, Version).
     * @param GAV A comma separated list of the groupId, artifactId, 
     * and version number
     * @param repositoryURIs A collection of repository URIs to search for the
     * target artefact in
     * @return A URI matching the provided GAV
     * @throws URISyntaxException Thrown if an artefact cannot be found for
     * the provided GAV
     */
    public static Map.Entry<String, URI> getArtefactMapEntry(String GAV, Collection<String> repositoryURIs) throws URISyntaxException {
        final Map<String, String> GAVMap = splitGAV(GAV);

        Set<String> repoURIs = new LinkedHashSet<>();

        if (repositoryURIs != null) {
            for (String uri : repositoryURIs) {
                String convertedURI = TranslatedConfigView.expandValue(uri);
                if (!convertedURI.endsWith("/")) {
                    convertedURI += "/";
                }
                repoURIs.add(convertedURI);
            }
        }

        repoURIs.add(defaultMavenRepository);

        final String relativeURIString = constructRelativeURIString(GAVMap);
        final URI artefactURI = findArtefactURI(repoURIs, relativeURIString);

        return new AbstractMap.SimpleImmutableEntry<>(GAVMap.get("artefactId"), artefactURI);
    }
    
    /**
     * Splits a provided GAV String and sets the class variables to the 
     * corresponding values.
     * @param GAV A comma separated list of the groupId, artifactId, 
     * and version number
     * @return A Map containing the groupId, artefactId, and versionNumber of 
     * the provided GAV as Strings
     */
    private static Map<String, String> splitGAV(String GAV) throws URISyntaxException {
        final String[] splitGAV = GAV.split("[,:]");
        final Map<String, String> GAVMap = new HashMap<>();
        try {
            GAVMap.put("groupId", splitGAV[0].replace('.', '/'));
            GAVMap.put("artefactId", splitGAV[1]);
            GAVMap.put("versionNumber", splitGAV[2]);
        } catch (ArrayIndexOutOfBoundsException ex) {
            logger.log(Level.WARNING, "Error converting String \"{0}\" to GAV, make sure it takes the form of "
                    + "groupId,artifactId,version", GAV);
            throw new URISyntaxException("\nGAV does not appear to be in the correct format", ex.toString());
        }
        
        return GAVMap;
    }
    
    /**
     * Constructs the relative URI of the provided GAV as a String.
     * @param GAVMap A map containing the target artefact's groupId, artifactId,
     * and version number.
     * @return A String representing the relative URI of the provided GAV.
     */
    private static String constructRelativeURIString(Map<String, String> GAVMap) {
        final String artefactFileName = GAVMap.get("artefactId") + "-" + GAVMap.get("versionNumber");

        return GAVMap.get("groupId") + "/" + GAVMap.get("artefactId") + "/"
               + GAVMap.get("versionNumber") + "/" + artefactFileName;
    }
    
    /**
     * Searches for a valid URI for the target artefact in the list of
     * provided repositories. 
     * @param repositoryURIs A list of the repositories to search for
     * the artefact in.
     * @param relativeURIString A String representation of the relative
     * artefact URI.
     * @return A valid URI to download the target artefact from.
     * @throws URISyntaxException Thrown if an artefact cannot be found for
     * the provided GAV
     */
    private static URI findArtefactURI(Collection<String> repositoryURIs, String relativeURIString) throws URISyntaxException {
        final String[] archiveTypes = new String[] {".jar", ".war", ".ear", ".rar"};

        // For each URI...
        for (String repositoryURI : repositoryURIs) {
            URI artefactURI = null;
            // Append each archive type until a valid URI is found
            for (String archiveType : archiveTypes) { 
                try {
                    artefactURI = new URI(repositoryURI + relativeURIString + archiveType);

                    if (URIUtils.exists(artefactURI)) {
                        return artefactURI;
                    }

                    logger.log(Level.FINE, "Artefact not found at URI: {0}", artefactURI.toString());
                } catch (URISyntaxException ex) {
                    String[] errorParameters = new String[] {repositoryURI, relativeURIString, archiveType};
                    logger.log(Level.WARNING, "Error creating URI from repository URI, {0}, relative URI, {1}, and archive"
                            + " type, {2}", errorParameters);
                } catch (MalformedURLException ex) {
                    logger.log(Level.WARNING, "Error creating URL from artefact URI, {0}", artefactURI.toString());
                } catch (ProtocolException ex) {
                    logger.log(Level.WARNING,"Error setting request method to \"HEAD\"");
                } catch (IOException ex) {
                    logger.log(Level.WARNING, "Error getting HTTP connection response code");
                }      
            }
        }

        throw new URISyntaxException(relativeURIString, "No artefact can be found for relative URI");
    }
}
