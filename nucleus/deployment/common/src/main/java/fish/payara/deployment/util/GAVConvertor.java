/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2018 Payara Foundation and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.config.support.TranslatedConfigView;
import com.sun.enterprise.universal.GFBase64Encoder;

/**
 * Performs the functions required for converting GAV (group, artefact, version)
 * coordinates into a URL.
 * @author Andrew Pielage
 */
public class GAVConvertor {
    
    private static final Logger logger = Logger.getLogger("PayaraMicro");
    
    /**
     * Returns a valid URL for a provided GAV (GroupId, ArtifactId, Version).
     * @param GAV A comma separated list of the groupId, artifactId, 
     * and version number
     * @param repositoryURLs A list of repository URLs to search for the 
     * target artefact in
     * @return A URL matching the provided GAV
     * @throws MalformedURLException Thrown if an artefact cannot be found for
     * the provided GAV
     */
    public Map.Entry<String, URL> getArtefactMapEntry(String GAV, List<URL> repositoryURLs) throws MalformedURLException {
        final Map<String, String> GAVMap = splitGAV(GAV);
        Map.Entry<String, URL> artefactMapEntry = null;
        
        final String relativeURLString = constructRelativeURLString(GAVMap);
        final URL artefactURL = findArtefactURL(repositoryURLs, relativeURLString);
        artefactMapEntry = new AbstractMap.SimpleEntry<>(GAVMap.get("artefactId"), artefactURL);
        
        return artefactMapEntry;
    }
    
    /**
     * Returns a valid URL for a provided GAV (GroupId, ArtifactId, Version).
     * @param GAV A comma separated list of the groupId, artifactId, 
     * and version number
     * @param repositoryURLs A collection of repository URLs to search for the 
     * target artefact in
     * @return A URL matching the provided GAV
     * @throws MalformedURLException Thrown if an artefact cannot be found for
     * the provided GAV
     */
    public Map.Entry<String, URL> getArtefactMapEntry(String GAV, Collection<String> repositoryURLs) throws MalformedURLException {
        List<URL> repoURLs = new LinkedList<URL>();
        for (String url: repositoryURLs) {
            String convertedURL = (String) TranslatedConfigView.getTranslatedValue(url);
            if (!convertedURL.endsWith("/")) {
              convertedURL += "/";
            }
            repoURLs.add(new URL(convertedURL));
        }
        return getArtefactMapEntry(GAV, repoURLs);
    }
    
    /**
     * Splits a provided GAV String and sets the class variables to the 
     * corresponding values.
     * @param GAV A comma separated list of the groupId, artifactId, 
     * and version number
     * @return A Map containing the groupId, artefactId, and versionNumber of 
     * the provided GAV as Strings
     */
    private Map<String, String> splitGAV(String GAV) throws MalformedURLException {
        final String[] splitGAV = GAV.split(",|:");
        final Map<String, String> GAVMap = new HashMap<>();
        try {
            GAVMap.put("groupId", splitGAV[0].replace('.', '/'));
            GAVMap.put("artefactId", splitGAV[1]);
            GAVMap.put("versionNumber", splitGAV[2]);
        } catch (ArrayIndexOutOfBoundsException ex) {
            logger.log(Level.WARNING, "Error converting String \"{0}\" to GAV, make sure it takes the form of "
                    + "groupId,artifactId,version", GAV);
            throw new MalformedURLException(ex.toString() + "\nGAV does not appear to be in the correct format");
        }
        
        return GAVMap;
    }
    
    /**
     * Constructs the relative URL of the provided GAV as a String.
     * @param GAV A map containing the target artefact's groupId, artifactId, 
     * and version number.
     * @return A String representing the relative URL of the provided GAV.
     * @throws MalformedURLException 
     */
    private String constructRelativeURLString(Map<String, String> GAVMap) throws MalformedURLException {
        final String artefactFileName = GAVMap.get("artefactId") + "-" + GAVMap.get("versionNumber");
        final String relativeURLString = GAVMap.get("groupId") + "/" + GAVMap.get("artefactId") + "/" 
                + GAVMap.get("versionNumber") + "/" + artefactFileName;
        
        return relativeURLString;
    }
    
    /**
     * Searches for a valid URL for the target artefact in the list of
     * provided repositories. 
     * @param repositoryURLs A list of the repositories to search for
     * the artefact in.
     * @param relativeURLString A String representation of the relative
     * artefact URL.
     * @return A valid URL to download the target artefact from.
     * @throws IOException 
     */
    private URL findArtefactURL(List<URL> repositoryURLs, String relativeURLString) throws MalformedURLException {     
        final String[] archiveTypes = new String[]{".jar", ".war", ".ear", ".rar"};
        
        boolean validURLFound = false;
        
        URL artefactURL = null;
        
        // For each URL...
        for (URL repositoryURL : repositoryURLs) {    
            // Append each archive type until a valid URL is found
            for (String archiveType : archiveTypes) { 
                try {
                    artefactURL = new URL(repositoryURL, relativeURLString + archiveType);

                    HttpURLConnection httpConnection = (HttpURLConnection) artefactURL.openConnection();
                    
                    String auth = artefactURL.getUserInfo();
                    if (auth != null) {
                        String encodedAuth = new GFBase64Encoder().encode(auth.getBytes());
                        httpConnection.setRequestProperty("Authorization", "Basic " + encodedAuth);
                    }
                    
                    httpConnection.setRequestMethod("HEAD");

                    if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        validURLFound = true;
                        break;
                    } else {
                        logger.log(Level.FINE, "Artefact not found at URL: {0}", artefactURL.toString());
                    }
                } catch (MalformedURLException ex) {
                    String[] errorParameters = new String[]{repositoryURL.toString(), relativeURLString, archiveType};
                    logger.log(Level.WARNING, "Error creating URL from repository URL, {0}, relative URL, {1}, and archive"
                            + " type, {2}", errorParameters);
                } catch (ProtocolException ex) {
                    logger.log(Level.WARNING,"Error setting request method to \"HEAD\"");
                } catch (IOException ex) {
                    logger.log(Level.WARNING, "Error getting HTTP connection response code");
                }      
            }
            
            if (validURLFound == true) {
                break;
            }
        }
        
        if (validURLFound == false) {
            throw new MalformedURLException("No artefact can be found for relative URL: "+ relativeURLString);
        }
        
        return artefactURL;
    }
}
