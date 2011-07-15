/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.web.util;

import java.util.BitSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class encodes HTML display content for preventing XSS.
 */
public class HtmlEntityEncoder {

    private static final Logger log = Logger.getLogger(
        HtmlEntityEncoder.class.getName());

    //Array containing the safe characters set.
    protected BitSet safeCharacters = new BitSet(256);

    public HtmlEntityEncoder() {
        for (char i = 'a'; i <= 'z'; i++) {
            addSafeCharacter(i);
        }
        for (char i = 'A'; i <= 'Z'; i++) {
            addSafeCharacter(i);
        }
        for (char i = '0'; i <= '9'; i++) {
            addSafeCharacter(i);
        }
        
        // Grizzly UEncode includes ) ( -
        addSafeCharacter('$');
        addSafeCharacter('_');
        addSafeCharacter('.');

        addSafeCharacter('!');
        addSafeCharacter('*');
        addSafeCharacter('\\');
        addSafeCharacter(',');

        // unsafe chars for XSS
        // CR 6944384: < > " ' % ; ) ( & + - 
    }

    public void addSafeCharacter(char c) {
        safeCharacters.set(c);
    }

    public String encode(Object obj) {
        if (obj == null) {
            return null;
        } else {
            return encode(obj.toString());
        }
    }

    public String encode(String s) {
        if (s == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder(s.length());

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (safeCharacters.get(c)) {
                sb.append(c);
            } else if (Character.isWhitespace(c)) {
                sb.append("&#").append((int)c).append(";");
            } else if (Character.isISOControl(c)) {
                // ignore
            } else if (Character.isHighSurrogate(c)) {
                if (i + 1 < s.length() && Character.isLowSurrogate(s.charAt(i + 1))) {
                    int codePoint = Character.toCodePoint(c, s.charAt(i + 1));
                    if (Character.isDefined(codePoint)) {
                        sb.append("&#").append(codePoint).append(";");
                    }
                }
                // else ignore this pair of chars
                i++;
            } else if (Character.isDefined(c)) {
                switch(c) {
                    case '&':
                        sb.append("&amp;");
                        break;
                    case '<':
                        sb.append("&lt;");
                        break;
                    case '>':
                        sb.append("&gt;");
                        break;
                    case '"':
                        sb.append("&quot;");
                        break;
                    default:
                        sb.append("&#").append((int)c).append(";");
                        break;
                }
            }
        }
        return sb.toString();
    }
}
