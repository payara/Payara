/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.AnnotationProcessor;

import java.io.*;
import java.util.HashMap;

class LogResourceBundle extends HashMap<String, String> {

    private static final String COMMENT = ".MSG_COMMENT";
    private static final String COPYRIGHT = "#\n# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.\n#\n# Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.\n#...\n\n";
    
    public LogResourceBundle() { }

    public LogResourceBundle load(BufferedReader reader) throws IOException {
        String line; 
        String unusedLine = null; 
        int pos;

        try {
            //Read File Line By Line
            while ((line = nextLine(unusedLine, reader)) != null)   {
                unusedLine = null;

                // We found a comment...
                if (line.indexOf('#') == 0) {
                    String commentLine = line;

                    // We expect a prop=value after the comment.
                    if ((line = reader.readLine()) == null) continue;

                    // A comment may follow a comment.   Ignore the first
                    // comment.
                    pos = line.indexOf('#');
                    if (line.indexOf('#') == 0) {
                        unusedLine = line;
                        continue;
                    }

                    pos = line.indexOf('=');
                    if (pos == -1) {
                        // The previous line was a lone comment - ignore it.
                        // Push the current line back to be reread during
                        // the next pass.
                        unusedLine = line;
                        continue;
                    } else {
                        unusedLine = null;
                        setProperty(line, pos, commentLine);
                    }
                    continue;
                } else if ((pos = line.indexOf('=')) != -1) {
                    setProperty(line, pos);
                    continue;
                }

                // We ignore all whitespace or lines that are not prop/values
                // or comments.
            }
        } finally {
            if (reader != null)
                reader.close();
        }

        return this;
    }

    /**
     * Store the resource bundle to the Writer object.   
     *    @return if contents were written to the object.
     */
    public boolean store(Writer out) throws IOException {

        // Noting to store.
        if (isEmpty()) return false;

        out.write(COPYRIGHT);

        for (String key : keySet()) {
            // Skip comments until needed after writing prop. 
            if (key.endsWith(COMMENT)) continue;

            if (containsKey(key + COMMENT)) 
                out.write(get(key + COMMENT) + "\n");
            out.write(key + "=" + get(key) + "\n\n");
        }
        out.flush();

        return true;
    }

    public void putComment(String key, String comment) {
        put(key + COMMENT, "# " + comment);
    }

    private void setProperty(String line, int pos, String commentLine) {
        String key = line.substring(0, pos).trim();
        String value = line.substring(pos + 1).trim();
        put(key, value);
        put(key + COMMENT, commentLine);
    }

    private void setProperty(String line, int pos) {
        String key = line.substring(0, pos).trim();
        String value = line.substring(pos + 1).trim();
        put(key, value);
    }

    // Returns either the unusedLine or reads a new line if the 
    // unusedLine is null.
    private String nextLine(String unusedLine, BufferedReader reader) 
            throws java.io.IOException {

        if (unusedLine != null) return unusedLine;

        return reader.readLine();
    }
}
