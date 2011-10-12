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

/*
 * TokenResolver.java
 *
 * Created on April 20, 2007, 11:59 AM
 * Updated for V3 on March 4, 2008
 */
package com.sun.enterprise.universal.glassfish;

import java.util.*;
import com.sun.enterprise.util.SystemPropertyConstants;

/**
 * Here is the contract:
 * You give me a Map<String,String> object.
 * Then you can call  resolve(List<String>) and/or resolve(String) and/or
 * resolve(Map<String,String>)
 * I will find and replace the tokens, e.g.,  ${foo} with the value of "foo" in the properties.
 * If the token has no such property -- then I leave the token as is.
 * It purposely does not handle nested tokens.  E.g. if the "foo" property has another
 * token embedded in the value -- it will not be further resolved.
 * This is the KISS principle in action...
 * @author bnevins
 */
public class TokenResolver {

    /**
     * Empty constructor means use System Properties
     *
     */
    public TokenResolver() {
        this(new HashMap<String, String>((Map) (System.getProperties())));
    }

    public TokenResolver(Map<String, String> map) {
        props = map;
    }
    /**
     * Replace $[variables} in map with a matching property from the map that this
     *  instance was constructed with.  Both names and values are replaced.
     * @param map Map of Strings to be token-replaced
     */
    public void resolve(Map<String, String> map) {
        // we may be concurrently changing the map so we have to be careful!

        // can't add to "map" arg while we are in the loop -- add all new
        // entries AFTER the loop.

        Map<String, String> newEntries = new HashMap<String,String>();

        Set<Map.Entry<String,String>> set = map.entrySet();
        Iterator<Map.Entry<String,String>> it = set.iterator();

        while(it.hasNext()) {
            Map.Entry<String,String> entry = it.next();
            String key = entry.getKey();
            String value = entry.getValue();

            // usual case -- the RHS has a token
            // will not get a concurrent mod exception -- it is just the value
            // that changes...
            if (hasToken(value)) {
                value = resolve(value);
                map.put(key, value);
            }

            // less usual case -- the LHS has a token.  Need to remove the entry
            // from the map and replace.
            // We have to worry about ConcurrentModification here!
            if(hasToken(key)) {
                String newKey = resolve(key);
                newEntries.put(newKey, value);
                it.remove(); // safe!!!
            }
        }
        map.putAll(newEntries);
    }

    /**
     * Replace $[variables} in list with a matching property from the map
     * @param list List of Strings to be token-replaced
     */
    public void resolve(List<String> list) {
        for (int i = 0; i < list.size(); i++) {
            String s = list.get(i);

            if (hasToken(s)) {
                list.set(i, resolve(s));
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Replace $[variables} with a matching property in the map
     * @param s String to be token-replaced
     * @return the replaced String
     */
    public String resolve(String s)
    {
        if(s == null || s.length() <= 0)
            return s;

        if (hasWindowsToken(s)) {
            s = windowsToUnixTokens(s);
        }

        List<Token> tokens = getTokens(s);
        String resolved = s;

        for (Token token : tokens) {
            resolved = GFLauncherUtils.replace(resolved, token.token, token.value);
        }

        return resolved;
    }

    /**
     *
     * @param s A String that may contain %token%
     * @return the UNIX-ified format ${token}
     */
    private String windowsToUnixTokens(String s) {
        String replaced = s;

        while (true) {
            if (replaced == null || replaced.indexOf('%') < 0) {
                break;
            }

            replaced = GFLauncherUtils.replace(replaced, "%", "${");
            replaced = GFLauncherUtils.replace(replaced, "%", "}");
        }
        if (replaced == null) {
            return s;
        }
        else {
            return replaced;
        }
    }

    private static boolean hasWindowsToken(String s) {
        // Need at least 2 "%"
        int index = s.indexOf('%');

        if (index < 0 || index >= s.length() - 1) {
            return false;
        }

        return s.indexOf('%', index + 1) >= 0;
    }

    ///////////////////////////////////////////////////////////////////////////
    private List<Token> getTokens(String s) {
        int index = 0;
        List<Token> tokens = new ArrayList<Token>();

        while (true) {
            Token token = getToken(s, index);

            if (token == null) {
                break;
            }

            tokens.add(token);
            index = token.start + Token.TOKEN_START.length();
        }

        return tokens;
    }

    ///////////////////////////////////////////////////////////////////////////
    private Token getToken(String s, int index) {
        if (s == null || index >= s.length()) {
            return null;
        }

        Token token = new Token();
        token.start = s.indexOf(Token.TOKEN_START, index);
        token.end = s.indexOf(Token.TOKEN_END, token.start + 2);

        if (token.end <= 0 || token.start < 0) {
            return null;
        }

        token.token = s.substring(token.start, token.end + 1);
        token.name = s.substring(token.start + Token.TOKEN_START.length(), token.end);

        // if the token exists, but it's value is null -- then set the value
        // back to the token.

        token.value = props.get(token.name);

        if (token.value == null) {
            token.value = token.token;
        }

        return token;
    }

    ///////////////////////////////////////////////////////////////////////////
    public static boolean hasToken(String s) {
        if (s == null) {
            return false;
        }
        if (GFLauncherUtils.isWindows() && hasWindowsToken(s)) {
            return true;
        }
        if (s.indexOf(Token.TOKEN_START) >= 0) {
            return true;
        }
        return false;
    }
    ///////////////////////////////////////////////////////////////////////////
    private final Map<String, String> props;

    private static class Token {

        int start;
        int end;
        String token;
        String name;
        String value;
        final static String TOKEN_START = SystemPropertyConstants.OPEN;
        final static String TOKEN_END = SystemPropertyConstants.CLOSE;

        @Override
        public String toString() {
            return "name: " + name + ", token: " + token + ", value: " + value;
        }
    }
}
