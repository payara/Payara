/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.appclient.client.acc;

import com.sun.enterprise.deploy.shared.ArchiveFactory;
import com.sun.enterprise.deployment.archivist.ArchivistFactory;
import com.sun.enterprise.util.LocalStringManager;
import com.sun.enterprise.util.LocalStringManagerImpl;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods
 *
 * @author tjquinn
 */
public class Util {

    private static Class thisClass = Util.class;
    private final static LocalStringManager localStrings = new LocalStringManagerImpl(thisClass);
    private static final String SLASH_REPLACEMENT = Matcher.quoteReplacement("\\\\");
    private static final String DOLLAR_REPLACEMENT = Matcher.quoteReplacement("\\$");

    /**
     * Returns a File for the specified path if the file exists and is readable.
     * @param filePath
     * @return
     * @throws java.io.FileNotFoundException
     * @throws java.io.IOException
     */
    public static File verifyFilePath(final String filePath) throws FileNotFoundException, IOException {
        File f = new File(filePath);
        if ( ! f.exists()) {
            throw new FileNotFoundException(f.getAbsolutePath());
        }
        if ( ! f.canRead()) {
            String msg = localStrings.getLocalString(thisClass,
                    "appclient.notReadable",
                    "{0} is not a readable file",
                    new Object[] {f.getAbsolutePath()});
            throw new IOException(msg);
        }
        return f;
    }

    /**
     * Returns a File object for the specified path if the file is otherwise
     * valid (exists and is readable) and is not a directory.
     * @param filePath
     * @return
     * @throws java.io.FileNotFoundException
     * @throws java.io.IOException
     */
    static File verifyNonDirectoryFilePath(final String filePath) throws FileNotFoundException, IOException {
        File f = verifyFilePath(filePath);
        if ( ! f.isFile()) {
            String msg = localStrings.getLocalString(thisClass,
                    "appclient.isDir",
                    "{0} is a directory; it must be a readable non-directory file",
                    new Object[] {f.getAbsolutePath()});
            throw new IOException(msg);
        }
        return f;
    }

    public static ArchiveFactory getArchiveFactory() {
        return ACCModulesManager.getService(ArchiveFactory.class);
    }

    public static ArchivistFactory getArchivistFactory() {
        return ACCModulesManager.getService(ArchivistFactory.class);
    }

    public static URI getURI(final String s) throws URISyntaxException {
        return getURI(new File(s));
    }

    public static URI getURI(final File f) throws URISyntaxException {
        return f.toURI();
    }
    /**
     * Writes the provided text to a temporary file marked for deletion on exit.
     * @param the content to be written
     * @param prefix for the temp file, conforming to the File.createTempFile requirements
     * @param suffix for the temp file
     * @return File object for the newly-created temp file
     * @throws IOException for any errors writing the temporary file
     * @throws FileNotFoundException if the temp file cannot be opened for any reason
     */
     public static File writeTextToTempFile(String content, String prefix, String suffix, boolean retainTempFiles) throws IOException, FileNotFoundException {
        BufferedWriter wtr = null;
        try {
            File result = File.createTempFile(prefix, suffix);
            if ( ! retainTempFiles) {
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
     * pattern is: "${" followed by all chars excluding "}" followed by "}",
     * capturing into group 1 all chars between the "${" and the "}"
     */
    private static final Pattern TOKEN_SUBSTITUTION = Pattern.compile("\\$\\{([^\\}]*)\\}");


    /**
     * Searches for placeholders of the form ${token-name} in the input String, retrieves
     * the property with name token-name from the Properties object, and (if
     * found) replaces the token in the input string with the property value.
     * @param s String possibly containing tokens
     * @param values Properties object containing name/value pairs for substitution
     * @return the original string with tokens substituted using their values
     * from the Properties object
     */
    public static String replaceTokens(String s, Properties values) {
        Matcher m = TOKEN_SUBSTITUTION.matcher(s);

        StringBuffer sb = new StringBuffer();
        /*
         * For each match, retrieve group 1 - the token - and use its value from
         * the Properties object (if found there) to replace the token with the
         * value.
         */
        while (m.find()) {
            String propertyName = m.group(1);
            String propertyValue = values.getProperty(propertyName);

            /*
             * Substitute only if the properties object contained a setting
             * for the placeholder we found.
             */
            if (propertyValue != null) {
                /*
                 * The next line quotes any $ signs and backslashes in the replacement string
                 * so they are not interpreted as meta-characters by the regular expression
                 * processor's appendReplacement.  
                 */
                String adjustedPropertyValue =
                        propertyValue.replaceAll("\\\\",SLASH_REPLACEMENT).
                            replaceAll("\\$", DOLLAR_REPLACEMENT);
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
         * There are no more matches, so append whatever remains of the matcher's input
         * string to the output.
         */
        m.appendTail(sb);

        return sb.toString();
    }

    public static String toXMLEscaped(final String content) {
        return content.
                    replaceAll("<", "&lt;").
                    replaceAll(">", "&gt;").
                    replaceAll("\"", "&quot;");
    }
}
