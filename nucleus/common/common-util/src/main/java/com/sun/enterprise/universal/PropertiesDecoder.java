/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.universal;

import java.util.*;

/**
 *
 * @author Byron Nevins
 */

public class PropertiesDecoder {
     /**
      * There are several CLI commands that take properties arguments.  The properties
      * are "flattened". This class will unflatten them back into a Map for you.
      * <p>Example Input:  <b>foo=goo:xyz:hoo=ioo</b>
      *  <p>Output would be 3 pairs:
      * <ul>
      *  <li>foo, goo
      *  <li>xyz, null
      *  <li>hoo, ioo
      *  </ul>
      * @param props The flattened string properties
      * @return A Map of the String keys and values.  It will return an
      */
    
    public static Map<String,String> unflatten(final String s) {
        if(!ok(s))
            return Collections.emptyMap();

        Map<String,String> map = new HashMap<String,String>();
        String[] elements = s.split(":");

        for(String element : elements) {
            addPair(map, element);
        }

        return map;
    }

    private static void addPair(Map<String, String> map, String element) {
        // TODO this method is a perfect candidate for unit tests...
        // note: It is quite tricky and delicate finding every possible weirdness
        // that a user is capable of!

        // element is one of these:
        // 0.   ""
        // 1.   "foo"
        // 2.   "foo=goo"
        // 3.   "foo="
        // if we get garbage like a=b=c=d  we change to "a", "b=c=d"

        // 0.
        if(!ok(element))
            return; // no harm, no foul

        int index = element.indexOf("=");

        // 1.
        if(index < 0)
            map.put(element, null);

        // 3.
        else if(element.length() - 1 <= index ) {
            // lose the '='
            map.put(element.substring(0, index), null);
        }
        // 2
        else // guarantee:  at least one char after the '='
            map.put(element.substring(0, index), element.substring(index + 1));
    }


    private static boolean ok(String s) {
        return s != null && s.length() > 0;
    }
}
