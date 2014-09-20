/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.util.test;

import com.sun.enterprise.admin.util.LineTokenReplacer;
import com.sun.enterprise.admin.util.TokenValue;
import com.sun.enterprise.admin.util.TokenValueSet;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.StringTokenizer;

/**
 *
 * @author  kedar
 */
public class TokenReplacementTester {

    /** Creates a new instance of TokenReplacementTester */
    private final LineTokenReplacer replacer;

    public TokenReplacementTester(String tokensFileName, String fromFile, String toFile) {
        final TokenValueSet tokens = getTokensFromFile(tokensFileName);
        replacer = new LineTokenReplacer(tokens);
        replacer.replace(fromFile, toFile);
    }

    private TokenValueSet getTokensFromFile(String fileName) {
        final TokenValueSet tokens  = new TokenValueSet();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(fileName));
            String line = null;
            while ((line = reader.readLine()) != null) {
                final TokenValue tv = getTokenValue(line);
                tokens.add(tv);
            }
            reader.close();
        }
        catch(Exception e) {
            e.printStackTrace();
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ex) {}
            }
        }
        return tokens;
    }

    private TokenValue getTokenValue(String line) {
        final String delim = "=";
        final StringTokenizer parser = new StringTokenizer(line, delim);
        final String[] output = new String[2];
        int i = 0;
        while(parser.hasMoreTokens()) {
            output[i++] = parser.nextToken();
        }
        final String DELIM = "%%%";
        TokenValue tv = new TokenValue(output[0], output[1], DELIM);
        return ( tv );
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        int length = args.length;
        if (length < 2) {
            usage();
            System.exit(1);
        }
        final String tokensFile = args[0];
        final String fromFile = args[1];
        final String toFile = fromFile + ".out";
        new TokenReplacementTester(tokensFile, fromFile, toFile);
    }

    private static void usage() {
        System.out.println("java TokenReplacementTester <tokens-file> <template-file>");
    }
}
