/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.appclient.common;

import com.sun.enterprise.deployment.ApplicationClientDescriptor;
import org.glassfish.deployment.common.ModuleDescriptor;
import org.glassfish.deployment.common.RootDeploymentDescriptor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *Implements several stateless utility methods.
 *
 * @author tjquinn
 */
public class Util {

    /** Pattern to match placeholders in dynamic document templates. 
     *The pattern specifies a non-aggressive match for ${token-name} strings.
     *(Non-aggressive means the pattern consumes as little of the input string
     *as possible in searching for a match.)  The pattern also stores the token 
     *name in group 1 of the pattern matcher.
     */
    private static Pattern placeholderPattern = Pattern.compile("\\$\\{(.*?)\\}");

    /** used in finding the name of the first class in a jar file */
    private static final String CLASS_SUFFIX = ".class";
    
    /** size of buffer used to load resources */
    private static final int BUFFER_SIZE = 1024;
    
    /** Creates a new instance of Util */
    public Util() {
    }

    /**
     *Searches for placeholders of the form ${token-name} in the input String, retrieves
     *the property with name token-name from the Properties object, and (if
     *found) replaces the token in the input string with the property value.
     *@param s String possibly containing tokens
     *@param values Properties object containing name/value pairs for substitution
     *@return the original string with tokens substituted using their values
     *from the Properties object
     */
    public static String replaceTokens(String s, Properties values) {
        Matcher m = placeholderPattern.matcher(s);

        StringBuffer sb = new StringBuffer();
        /*
         *For each match, retrieve group 1 - the token - and use its value from
         *the Properties object (if found there) to replace the token with the
         *value.
         */
        while (m.find()) {
            String propertyName = m.group(1);
            String propertyValue = values.getProperty(propertyName);

            if (propertyValue != null) {
                /*
                 *The next line quotes any $ signs in the replacement string
                 *so they are not interpreted as meta-characters by the regular expression
                 *processor's appendReplacement.  The replaceAll replaces all occurrences
                 *of $ with \$.  The extra slashes are needed to quote the backslash
                 *for the Java language processor and then again for the regex
                 *processor (!).
                 */
                String adjustedPropertyValue = propertyValue.replaceAll("\\$", "\\\\\\$");
                String x = s.substring(m.start(),m.end());
                try {
                    m.appendReplacement(sb, adjustedPropertyValue);
                } catch (IllegalArgumentException iae) {
                    System.err.println("**** appendReplacement failed: segment is " + x + "; original replacement was " + propertyValue + " and adj. replacement is " + adjustedPropertyValue + "; exc follows");
                    throw iae;
                }
            }
        }
        /*
         *There are no more matches, so append whatever remains of the matcher's input
         *string to the output.
         */
        m.appendTail(sb);

        return sb.toString();
    }

     /**
      *Returns the main class name for the app client represented by the module descriptor.
      *@param moduleDescr the module descriptor for the app client of interest
      *@return main class name of the app client
      */
     public static String getMainClassNameForAppClient(ModuleDescriptor moduleDescr) throws IOException, FileNotFoundException, org.xml.sax.SAXParseException {
         RootDeploymentDescriptor bd = moduleDescr.getDescriptor();
         ApplicationClientDescriptor acDescr = (ApplicationClientDescriptor) bd;
         
         String mainClassName = acDescr.getMainClassName();
         
         return mainClassName;
     }
     
     /**
      *Writes the provided text to a temporary file marked for deletion on exit.
      *@param content the content to be written
      *@param prefix prefix for the temp file, conforming to the File.createTempFile requirements
      *@param suffix suffix for the temp file
      *@param retainFile whether to keep the file
      *@return File object for the newly-created temp file
      *@throws IOException for any errors writing the temporary file
      *@throws FileNotFoundException if the temp file cannot be opened for any reason
      */
     public static File writeTextToTempFile(String content, String prefix, String suffix, boolean retainFile) throws IOException, FileNotFoundException {
        BufferedWriter wtr = null;
        try {
            File result = File.createTempFile(prefix, suffix);
            if ( ! retainFile) {
                result.deleteOnExit();
            }
            FileOutputStream fos = new FileOutputStream(result);
            wtr = new BufferedWriter(new OutputStreamWriter(fos));
            wtr.write(content);
            wtr.close();
            return result;
        } finally {
            if (wtr != null) {
                wtr.close();
            }
        }
    }
    
    /**
     *Writes the provided text to a temporary file marked for deletion on exit.
     *@param the content to be written
     *@param prefix for the temp file, conforming to the File.createTempFile requirements
     *@param suffix for the temp file
     *@return File object for the newly-created temp file
     *@throws IOException for any errors writing the temporary file
     *@throws FileNotFoundException if the temp file cannot be opened for any reason
     */
    public static File writeTextToTempFile(String content, String prefix, String suffix) throws IOException, FileNotFoundException {
        return writeTextToTempFile(content, prefix, suffix, false);
    }

     /**
      *Finds the jar file or directory that contains the current class and returns its URI.
      *@param the class, the containing jar file or directory of which is of interest
      *@return URL to the containing jar file or directory
      */
     public static URL locateClass(Class target) {
         return target.getProtectionDomain().getCodeSource().getLocation();
     }

    /**
     *Retrieves a resource as a String.
     *<p>
     *This method does not save the template in a cache.  Use the instance method
     *getTemplate for that purpose.
     *
     *@param a class, the class loader of which should be used for searching for the template
     *@param the path of the resource to load, relative to the contextClass
     *@return the resource's contents
     *@throws IOException if the resource is not found or in case of error while loading it
     */
    public static String loadResource(Class contextClass, String resourcePath) throws IOException {
        String result = null;
        InputStream is = null;
        BufferedReader reader = null;
        try {
            is = contextClass.getResourceAsStream(resourcePath);
            if (is == null) {
                throw new IOException("Could not locate the requested resource relative to class " + contextClass.getName());
            }

            StringBuilder sb = new StringBuilder();
            reader = new BufferedReader(new InputStreamReader(is));
            int charsRead;
            char [] buffer = new char [BUFFER_SIZE];
            while ((charsRead = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, charsRead);
            }

            result= sb.toString();
            return result;
        } catch (IOException ioe) {
            throw new IOException("Error loading resource " + resourcePath, ioe);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }
    
    /**
     *Copy an existing file into a temporary file.
     *@param existing file
     *@return File object for the temporary file
     */
    public static File copyToTempFile(File inputFile, String prefix, String suffix, boolean retainFile) throws IOException, FileNotFoundException {
        File result = null;
        BufferedInputStream is = null;
        BufferedOutputStream os = null;
        try {
            result = File.createTempFile(prefix, suffix);
            if ( ! retainFile) {
                result.deleteOnExit();
            }
            os = new BufferedOutputStream(new FileOutputStream(result));
            is = new BufferedInputStream(new FileInputStream(inputFile));
            byte [] buffer = new byte[BUFFER_SIZE];
            int bytesRead = 0;
            while ( (bytesRead = is.read(buffer) ) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            
            return result;
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } finally {
                if (os != null) {
                    os.close();
                }
            }
        }
    }
    
    /**
     *Returns a codeBase expression, usable in a policy file, for the specified
     *URL.
     *@param classPathElement the URL to be converted
     *@return the codeBase expression
     */
    public static String URLtoCodeBase(URL classPathElement) throws FileNotFoundException, URISyntaxException {
        /*
         *We can assume the URL specifies a file.
         */
        File file = new File(classPathElement.toURI());
        if (! file.exists()) {
            /*
             *If we cannot locate the file, it may be a jar listed in the
             *manifest's Class-Path of a top-level archive.  The spec does
             *not require containers to handle such jars, so just 
             *return null.
             */
            //throw new FileNotFoundException(classPathElement.toURI().toASCIIString());
            return null;
        }
        
        /*
         *The format of the codebase is different for a directory vs. a jar
         *file.  Also note that the codebase must use the platform-neutral
         *"forward-slash" notation.
         */
        String result;
        if (file.isDirectory()) {
            result = classPathElement.getProtocol() + ":" + classPathElement.toURI().getPath() + "-";
        } else {
            result = classPathElement.getProtocol() + ":" + classPathElement.toURI().getPath();
        }
        return result;
    }
}
