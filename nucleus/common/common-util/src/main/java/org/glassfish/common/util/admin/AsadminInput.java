/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2010-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.common.util.admin;

import com.sun.enterprise.util.LocalStringManager;
import com.sun.enterprise.util.LocalStringManagerImpl;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Implements in one place the details of providing data to asadmin's
 * System.in and consuming it.
 * <p>
 * Primarily (but not exclusively) for security reasons, it is useful to be
 * able to have asadmin read data from its System.in.  This class provides
 * the logic to do so consistently.  It also provides a little help for
 * writing data to a stream such that it will be acceptable as input.
 * <p>
 * The input stream must have the following format:
 * <pre>
 * version=<i>some-version-value</i>
 * (lines as expected by the specified version)
 * </pre>
 * <p>
 * Currently there is only one version supported.  The lines following
 * the version spec must be in the format of a properties file, with the convention
 * that each property name is (category).(property-within-category)=value
 * The {@link InputReader} returned will return a Map<String,Properties> from
 * its {@link InputReader#settings() } method.  The map will contain one entry for
 * each category in the input, and the associated Properties object will map each
 * property-within-category to its value.
 *
 *
 * @author Tim Quinn
 */
public class AsadminInput {

    public final static String CLI_INPUT_OPTION_NAME = "_auxinput";
    public final static String CLI_INPUT_OPTION = "--" + CLI_INPUT_OPTION_NAME;
    
    public final static String SYSTEM_IN_INDICATOR = "-"; // option value indicating we should read from stdin
    
    private final static String VERSION_1_0 = "1.0";
    private final static String VERSION_INTRODUCER = "version=";
    
    private final static String CURRENT_VERSION = VERSION_1_0;
    private final static LocalStringManager localStrings = new LocalStringManagerImpl(AsadminInput.class);
    
    public interface InputReader {
        public Map<String,Properties> settings();
    }
    
    /**
     * Returns a string containing a specifier for the current version, suitable
     * for use as the first line to write to the asadmin System.in stream.
     * @return
     */
    public static String versionSpecifier() {
        return VERSION_INTRODUCER + CURRENT_VERSION;
    }

    /**
     * Returns an asadmin reader for reading from the specified URI.
     * @param inputPath a valid file path or the dash indicating to read from system.in
     * @return
     * @throws IOException
     */
    public static InputReader reader(final String inputPath) throws URISyntaxException, IOException {
        if (inputPath.equals(SYSTEM_IN_INDICATOR)) {
            return reader(System.in);
        }
        return reader(new FileInputStream(inputPath));
    }

    /**
     * Returns a reader that can consume the specified version of asadmin input
     *
     * @return
     * @throws IOException
     */
    public static InputReader reader(final InputStream is) throws IOException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        final String version = readVersionFromFirstLine(reader);
        return newReader(reader, version);
    }

    private static InputReader newReader(final BufferedReader reader, final String version) throws IOException {
        if (version.equals(VERSION_1_0)) {
            return new InputReader_1_0(reader);
        }
        throw new IllegalArgumentException(unknownVersionMsg(version));
    }

    /**
     * Version 1.0 of the input reader.
     * <p>
     * The expected format of the input stream, after the version=xxx line, is
     * (category).name=value
     *
     * The settings map will contain a Properties object for each different
     * category found in the input stream.
     */
    private static class InputReader_1_0 implements InputReader {
        private final Map<String,Properties> settings;
        private final BufferedReader reader;

        @Override
        public Map<String,Properties> settings() {
            return settings;
        }

        private InputReader_1_0(final BufferedReader reader) throws IOException {
            this.reader = reader;
            settings = loadSettings();
            reader.close();
        }

        private Map<String,Properties> loadSettings() throws IOException {
            final Map<String,Properties> result = new HashMap<String,Properties>();
            final Properties entireContent = new Properties();
            entireContent.load(reader);
            for (String propName : entireContent.stringPropertyNames()) {
                final int firstDot = propName.indexOf('.');
                if ( firstDot == -1) {
                    continue;
                }
                final String categoryName = propName.substring(0, firstDot);
                final String propWithinCategory = propName.substring(firstDot + 1);

                Properties category = result.get(categoryName);
                if (category == null) {
                    category = new Properties();
                    result.put(categoryName, category);
                }

                category.setProperty(propWithinCategory, entireContent.getProperty(propName));
            }
            return result;
        }
    }
    
    private static String readVersionFromFirstLine(final BufferedReader reader) throws IOException {

        final String firstLine;
        /*
         * The first line should be version=some-version-string so 
         * complain if it is not.
         */
        if ( ((firstLine = reader.readLine()) == null) 
            || ( ! firstLine.startsWith(VERSION_INTRODUCER)) 
            || ( firstLine.length() <= VERSION_INTRODUCER.length())) {
            throw new IOException(badVersionMsg());
        }
        return firstLine.substring(VERSION_INTRODUCER.length());
    }
    
    private static String badVersionMsg() {
        return MessageFormat.format(
                        localStrings.getLocalString(
                            "AsadminInputNoVersion",
                            "Input to asadmin does not specify a version using {0} as the first line"),
                         VERSION_INTRODUCER);
    }

    private static String unknownVersionMsg(final String version) {
        return MessageFormat.format(
                localStrings.getLocalString(
                    "AsadminInputUnknownVersion",
                    "Input to asadmin specifies version {0} which is not recognized"),
                    version);
    }
}
