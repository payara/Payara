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

package com.sun.enterprise.admin.servermgmt.services;

import com.sun.enterprise.admin.util.LineTokenReplacer;
import com.sun.enterprise.admin.util.TokenValue;
import com.sun.enterprise.admin.util.TokenValueSet;
import java.io.*;
import java.util.*;

/**
 *
 * @author bnevins
 */
public class ServicesUtils {
    private ServicesUtils() {
        
    }

    static TokenValueSet map2Set(final Map<String, String> map){
        final Set<TokenValue> set = new HashSet<TokenValue> ();
        for (final Map.Entry<String,String> e : map.entrySet()) {
            final String key = e.getKey();
            final String value = e.getValue();
            final TokenValue tv = new TokenValue(key, value);
            set.add(tv);
        }
        final TokenValueSet tvset = new TokenValueSet(set);
        return ( tvset );
    }
    
    static void tokenReplaceTemplateAtDestination(
                Map<String,String> map, String templatePath, String targetPath) {

        final LineTokenReplacer tr = new LineTokenReplacer(map2Set(map));
        tr.replace(templatePath, targetPath);
    }

    static void appendTextToFile(File to, String what) {

        // todo - this should be a high-level utility
        if(what == null || to == null)
            return;

        // It is very annoying in Windows when text files have "\n" instead of
        // \n\r -- the following fixes that.

        String[] lines = what.split("\n");

        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new FileOutputStream(to, true));
            pw.println(SEP);
            pw.println(new Date());

            for(String s : lines)
                pw.println(s);

            pw.println(SEP);
            pw.println();
            pw.println();
            pw.flush();
        }
        catch (IOException ioe) {
        }
        finally {
            try {
                if(pw != null)
                    pw.close();
            }
            catch (Exception e) {
                // ignore
            }
        }
    }

    private static final String SEP = "==========================================";
}
