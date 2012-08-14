/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.module.maven.commandsecurityplugin;

import java.util.regex.Pattern;

/**
 *
 * @author tjquinn
 */
public class Util {
    /*
        * Next block stolen shamelessly from the hk2 config Dom class, pending
        * a slight refactoring of that code there to expose the part we need.
        */
    static final Pattern TOKENIZER;
    private static String split(String lookback,String lookahead) {
        return "((?<="+lookback+")(?="+lookahead+"))";
    }
    private static String or(String... tokens) {
        StringBuilder buf = new StringBuilder();
        for (String t : tokens) {
            if(buf.length()>0)  buf.append('|');
            buf.append(t);
        }
        return buf.toString();
    }
    static {
        String pattern = or(
                split("x","X"),     // AbcDef -> Abc|Def
                split("X","Xx"),    // USArmy -> US|Army
                //split("\\D","\\d"), // SSL2 -> SSL|2
                split("\\d","\\D")  // SSL2Connector -> SSL|2|Connector
        );
        pattern = pattern.replace("x","\\p{Lower}").replace("X","\\p{Upper}");
        TOKENIZER = Pattern.compile(pattern);
    }

    static String convertName(final String name) {
        // tokenize by finding 'x|X' and 'X|Xx' then insert '-'.
        StringBuilder buf = new StringBuilder(name.length()+5);
        for(String t : TOKENIZER.split(name)) {
            if(buf.length()>0)  buf.append('-');
            buf.append(t.toLowerCase());
        }
        return buf.toString();  
    }

    /* end of shameless copy */
    
    static String restOpTypeToAction(final String restOpType) {
        if (restOpType.equals("POST")) {
            return "update";
        } else if (restOpType.equals("PUT")) {
            return "update";
        } else if (restOpType.equals("GET")) {
            return "read";
        } else if (restOpType.equals("DELETE")) {
            return "delete";
        } else {
            return "????";
        }
    }

    /**
     *
     * @param s the value of s
     */
    static String lastPart(final String s) {
        final int lastDot = s.lastIndexOf('.');
        return s.substring(lastDot + 1);
    }
}
