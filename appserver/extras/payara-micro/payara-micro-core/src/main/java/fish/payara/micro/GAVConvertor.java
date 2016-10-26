/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2016 Payara Foundation and/or its affiliates.
 All rights reserved.

 The contents of this file are subject to the terms of the Common Development
 and Distribution License("CDDL") (collectively, the "License").  You
 may not use this file except in compliance with the License.  You can
 obtain a copy of the License at
 https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 or packager/legal/LICENSE.txt.  See the License for the specific
 language governing permissions and limitations under the License.

 When distributing the software, include this License Header Notice in each
 file and include the License file at packager/legal/LICENSE.txt.
 */

package fish.payara.micro;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.embeddable.GlassFishException;

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
     * @throws GlassFishException Thrown if an artefact cannot be found for
     * the provided GAV
     */
    public Map.Entry<String, URL> getArtefactMapEntry(String GAV, List<URL> repositoryURLs)
            throws GlassFishException {
        final Map<String, String> GAVMap = splitGAV(GAV);
        Map.Entry<String, URL> artefactMapEntry = null;
        
        try {
            final String relativeURLString = constructRelativeURLString(GAVMap);
            final URL artefactURL = findArtefactURL(repositoryURLs, 
                    relativeURLString);
            artefactMapEntry = new AbstractMap.SimpleEntry<>(GAVMap.get("artefactId"), 
                            artefactURL);
        } catch (MalformedURLException ex) {
            logger.log(Level.WARNING, "Error converting GAV to URL: {0}", GAV);
        }
        
        if (artefactMapEntry == null) {
            throw new GlassFishException("Error getting artefact URL for GAV: " 
                + GAVMap.get("groupId") + ", " + GAVMap.get("artefactId")
                + ", " + GAVMap.get("versionNumber"));
        }
        
        return artefactMapEntry;
    } 
    
    /**
     * Splits a provided GAV String and sets the class variables to the 
     * corresponding values.
     * @param GAV A comma separated list of the groupId, artifactId, 
     * and version number
     * @return A Map containing the groupId, artefactId, and versionNumber of 
     * the provided GAV as Strings
     */
    private Map<String, String> splitGAV(String GAV) throws GlassFishException {
        final String[] splitGAV = GAV.split(",");
        final Map<String, String> GAVMap = new HashMap<>();
        try {
            GAVMap.put("groupId", splitGAV[0].replace('.', '/'));
            GAVMap.put("artefactId", splitGAV[1]);
            GAVMap.put("versionNumber", splitGAV[2]);
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw new GlassFishException("Error converting String \"" + GAV
                    + "\" to GAV, make sure it takes the form of "
                    + "groupId,artifactId,version");
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
    private String constructRelativeURLString(Map<String, String> GAVMap) 
            throws MalformedURLException {
        final String artefactFileName = GAVMap.get("artefactId") + "-"
                + GAVMap.get("versionNumber");
        final String relativeURLString = GAVMap.get("groupId") + "/"
                + GAVMap.get("artefactId") + "/" + GAVMap.get("versionNumber")
                + "/" + artefactFileName;
        
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
     * @throws GlassFishException 
     */
    private URL findArtefactURL(List<URL> repositoryURLs, String relativeURLString) 
            throws GlassFishException {     
        final String[] archiveTypes = new String[]{".jar", ".war", ".ear"};
        
        boolean validURLFound = false;
        
        URL artefactURL = null;
        
        // For each URL...
        for (URL repositoryURL : repositoryURLs) {    
            // Append each archive type until a valid URL is found
            for (String archiveType : archiveTypes) { 
                try {
                    artefactURL = new URL(repositoryURL, relativeURLString
                            + archiveType);

                    HttpURLConnection httpConnection = 
                            (HttpURLConnection) artefactURL.openConnection();
                    httpConnection.setRequestMethod("HEAD");

                    if (httpConnection.getResponseCode() == 
                            HttpURLConnection.HTTP_OK) {
                        validURLFound = true;
                        break;
                    } else {
                        logger.log(Level.FINE, "Artefact not found at URL: {0}",
                                artefactURL.toString());
                    }
                } catch (MalformedURLException ex) {
                    String[] errorParameters = new String[]{repositoryURL.toString(), 
                        relativeURLString, archiveType};
                    logger.log(Level.WARNING, "Error creating URL from "
                            + "repository URL, {0}, relative URL, {1}, and "
                            + "archive type, {2}", errorParameters);
                } catch (ProtocolException ex) {
                    logger.log(Level.WARNING,"Error setting request method to "
                            + "\"HEAD\"");
                } catch (IOException ex) {
                    logger.log(Level.WARNING, "Error getting HTTP connection "
                            + "response code");
                }      
            }
            
            if (validURLFound == true) {
                break;
            }
        }
        
        if (validURLFound == false) {
            throw new GlassFishException("No artefact can be found for "
                    + "relative URL: "+ relativeURLString);
        }
        
        return artefactURL;
    }
}
