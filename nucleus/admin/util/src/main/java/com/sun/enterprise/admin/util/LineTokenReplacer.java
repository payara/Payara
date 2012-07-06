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
package com.sun.enterprise.admin.util;

import java.io.File;
import java.io.Reader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 */
public final class LineTokenReplacer {

    private final TokenValue[] tokenArray;
    private final String charsetName;

    public LineTokenReplacer(TokenValueSet tokens) {
        this(tokens, null);
    }

    /**
     * Creates a new instance of TokenReplacer
     */
    public LineTokenReplacer(TokenValueSet tokens, String charset) {
        final Object[] tmp = tokens.toArray();
        final int length = tmp.length;
        this.tokenArray = new TokenValue[length];
        System.arraycopy(tmp, 0, tokenArray, 0, length);
        this.charsetName = charset;
    }

    /**
     * Get a Reader that substitutes the tokens in the content that it returns.
     *
     * @param in the content in which tokens are to be substituted
     * @return a Reader that returns the substituted content
     */
    public Reader getReader(final Reader in) {
        return new Reader() {

            BufferedReader reader = new BufferedReader(in);
            String line = null;
            final String eol = System.getProperty("line.separator");

            @Override
            public int read(char[] cbuf, int off, int len) throws IOException {
                if (line == null || line.isEmpty()) {
                    line = reader.readLine();
                    if (line == null) {
                        return -1;
                    }
                    line = replaceLine(line) + eol;
                }
                int copySize = len - off;
                if (copySize > line.length()) {
                    copySize = line.length();
                }
                line.getChars(0, copySize, cbuf, off);
                line = line.substring(copySize);
                return copySize;
            }

            @Override
            public void close() throws IOException {
                reader.close();
            }
        };

    }

    public void replace(File inputFile, File outputFile) {
        //Edge-cases
        BufferedReader reader = null;
        BufferedWriter writer = null;
        // @todo Java SE 7 - use try with resources
        try {
            reader = new BufferedReader(new FileReader(inputFile));
            try {
                if (charsetName != null) {
                    FileOutputStream outputStream = new FileOutputStream(outputFile);
                    Charset charset = Charset.forName(charsetName);
                    writer = new BufferedWriter(new OutputStreamWriter(outputStream, charset));
                } else {
                    writer = new BufferedWriter(new FileWriter(outputFile));
                }
                String lineContents;
                while ((lineContents = reader.readLine()) != null) {
                    String modifiedLine = replaceLine(lineContents);
                    writer.write(modifiedLine);
                    writer.newLine();
                }
            } finally {
                if (writer != null) {
                    writer.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                }
            }
        }
    }

    public void replace(String inputFileName, String outputFileName) {
        this.replace(new File(inputFileName), new File(outputFileName));
    }

    private String replaceLine(String lineWithTokens) {
        String tokenFreeString = lineWithTokens;

        for (int i = 0; i < tokenArray.length; i++) {
            TokenValue aPair = tokenArray[i];
            //System.out.println("To replace: " + aPair.delimitedToken);
            //System.out.println("Value replace: " + aPair.value);
            tokenFreeString = tokenFreeString.replace(aPair.delimitedToken,
                    aPair.value);
        }
        return (tokenFreeString);
    }
}
