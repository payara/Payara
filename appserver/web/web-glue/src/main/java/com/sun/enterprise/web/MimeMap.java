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

package com.sun.enterprise.web;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Class representing the parsed mime mapping file of a mime element.
 */
public class MimeMap implements Serializable {
   
    private static final String MIME_TYPE = "type=";
    private static final String MIME_EXTS = "exts=";

    private String id;
    private HashMap<String, String> mimeMappings;

    /**
     * Constructor.
     *
     * @param id The mime id of the mime element which this MimeMap represents
     */
    MimeMap(String id) {
        this.id = id;
    }

    /**
     * Gets the mime id of the mime element which this MimeMap represents.
     */
    String getID() {
        return this.id;
    }

    /**
     * Parses the mime mappings from the given file.
     *
     * @param file The mime file
     */
    void load(String file) throws IOException {

        BufferedReader in = new BufferedReader(new FileReader(file));

        try {
            while (true) {
                // Get next line
                String line = in.readLine();
                if (line == null)
                    return;

                int len = line.length();
                if (len > 0) {
                    // Ignore comments
                    char firstChar = line.charAt(0);
                    if ((firstChar != '#') && (firstChar != '!')) {

                        // Find start of key
                        int keyStart = 0;
                        while (keyStart < len
                                && Character.isWhitespace(line.charAt(keyStart))) {
                            keyStart++;
                        }

                        // Blank lines are ignored
                        if (keyStart == len) {
                            continue;
                        }

                        int keyEnd = keyStart;
                        while (keyEnd<len
                                && !Character.isWhitespace(line.charAt(keyEnd))) {
                            keyEnd++;
                        }

                        // Find start of value
                        int valueStart = keyEnd;
                        while (valueStart<len
                                && Character.isWhitespace(line.charAt(valueStart))) {
                            valueStart++;
                        }
                        if (valueStart == len) {
                            // Ignore this MIME mapping
                            continue;
                        }
                        int valueEnd = valueStart;
                        while (valueEnd<len
                                && !Character.isWhitespace(line.charAt(valueEnd))) {
                            valueEnd++;
                        }

                        String key = line.substring(keyStart, keyEnd);
                        String value = line.substring(valueStart, valueEnd);

                        addMappings(key, value);
                    }
                }
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    /**
     * Returns an iterator over the mime extensions that were parsed
     *
     * @return Iterator over the mime extensions that were parsed, or null if
     * the mime file was empty
     */
    Iterator<String> getExtensions() {
        Iterator<String> ret = null;
        if (mimeMappings != null) {
            ret = mimeMappings.keySet().iterator();
        }
        return ret;
    }

    /**
     * Gets the mime type corresponding to the given extension
     * 
     * @param extension The mime extension
     *
     * @return The mime type corresponding to the given extension, or null if
     * the given extension does not map to any mime type
     */
    String getType(String extension) {
        String ret = null;
        if (mimeMappings != null) {
            ret = mimeMappings.get(extension);
        }
        return ret;
    }

    private void addMappings(String type, String exts) {
        // Remove "type=" prefix
        int index = type.indexOf(MIME_TYPE);
        if (index == -1) {
            // ignore
            return;
        }
        type = type.substring(index + MIME_TYPE.length());

        // Remove "exts=" prefix
        index = exts.indexOf(MIME_EXTS);
        if (index == -1) {
            // ignore
            return;
        }

        if (mimeMappings == null) {
            mimeMappings = new HashMap<String, String>();
	}

        exts = exts.substring(index + MIME_EXTS.length());
        index = exts.indexOf(',');
        String ext = null;
        if (index != -1) {
            // e.g., exts=aif,aiff,aifc
            int fromIndex = 0;
            while (index != -1) {
                ext = exts.substring(fromIndex, index).trim();
                if (ext.length() > 0) {
                    mimeMappings.put(ext, type);
                }
                fromIndex = index+1;
                index = exts.indexOf(',', fromIndex);
            }
            ext = exts.substring(fromIndex);
        } else {
            // e.g., exts=gif
            ext = exts;
        }

        if (ext != null) {
            ext = ext.trim();
            if (ext.length() > 0) {
                mimeMappings.put(ext, type);
            }
        }
    }
}
