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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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

import java.io.File;
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

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.glassfish.config.support.TranslatedConfigView;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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

        final String relativeURIString = constructRelativeURIString(GAVMap, repoURIs);
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
    private static String constructRelativeURIString(Map<String, String> GAVMap, Collection<String> repositoryURIs) {
        String relativeURI = String.format("%s/%s/%s", GAVMap.get("groupId"), GAVMap.get("artefactId"), GAVMap.get("versionNumber")); 
        String artefactFileName = String.format("%s-%s", GAVMap.get("artefactId"),GAVMap.get("versionNumber"));
        
        //Check if version is not a snapshot
        if (!relativeURI.endsWith("SNAPSHOT")) {
            return relativeURI + "/" + artefactFileName;
        }
        //Loop through each repoURI
        for (String repoURI : repositoryURIs) {
            URI mavenMetaDataURI = null;
            try {
                mavenMetaDataURI = new URI(repoURI + relativeURI + "/" + "maven-metadata.xml");
                
                //Check if maven metadata exists
                if (URIUtils.exists(mavenMetaDataURI)) {
                    File mavenMetaData = URIUtils.convertToFile(mavenMetaDataURI);
                    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
                    documentBuilderFactory.setAttribute(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                    DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
                    Document document = documentBuilder.parse(mavenMetaData);
                    document.getDocumentElement().normalize();
                    NodeList nodeList = document.getElementsByTagName("snapshotVersion");
                    Node node = nodeList.item(0);
                    Element element = (Element) node;
                    //Get latest snapshot version
                    String version = element.getElementsByTagName("value").item(0).getTextContent();
                    artefactFileName = GAVMap.get("artefactId") + "-" + version;
                    logger.log(Level.FINE, "Found version {0} from maven-metadata.xml", version);
                    break;
                }
            } catch (URISyntaxException e) {
                logger.log(Level.WARNING, "Error creating maven metadata URI");
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error getting HTTP connection response code");
            } catch (ParserConfigurationException e) {
                logger.log(Level.WARNING, "Error creating Document Builder");
            } catch (SAXException e) {
                logger.log(Level.WARNING, "Error parsing maven-metadata.xml");
            }

        }
        //For local maven repos, the maven-metadata-local isn't checked 
        //The standard URI is generated
        logger.log(Level.FINE, "Relative URI String is: {0}", relativeURI + "/" + artefactFileName);
        return relativeURI + "/" + artefactFileName;
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
