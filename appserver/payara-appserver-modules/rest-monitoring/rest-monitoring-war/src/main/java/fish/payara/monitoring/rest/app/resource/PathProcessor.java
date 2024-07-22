/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2017] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.monitoring.rest.app.resource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author Fraser Savage
 */
public final class PathProcessor {

    private static final char[] SPECIAL_CHARS = {'!','/'};
    private static final char ESCAPE_CHAR = SPECIAL_CHARS[0];
    private static int PATH_LENGTH;

    /**
     * Takes a {@link String} representation of a URL path and splits it on
     * characters in SPECIAL_CHARS while using the ESCAPE_CHAR to escape splits.
     * 
     * @param path The string form of the URL path to split.
     * @return The string array containing each split part of the URL path.
     */
    static String[] getSplitPath(String path) {
        PATH_LENGTH = path.length();
        
        if (PATH_LENGTH <= 2) {
            String[] url = new String[1];
            url[0] = path;
            return url;
        }

        List<String> segments = new ArrayList<>();
        processPath(path, segments);
        clearPath(segments);
       
        String[] segmentArray = new String[segments.size()];

        for (int i=0; i<segments.size(); i++) {
            segmentArray[i] = segments.get(i);
        }

        return segmentArray;
    }
    
    private static void processPath(String path, List<String> segments) {
        StringBuilder segment = new StringBuilder();
       
        for (int i=0; i<PATH_LENGTH; i++) {
            char currentChar = path.charAt(i); 
            boolean charEscaped = isEscapedChar(path, i);

            if (charEscaped) {
                segment.append(currentChar);
            } else if (!charEscaped && !isSpecialChar(currentChar)){
                segment.append(currentChar);
            } else if (currentChar != ESCAPE_CHAR) {
                segments.add(segment.toString());
                segment = new StringBuilder();
            }
        } 
        // Add the final segment
        segments.add(segment.toString());
    }

    private static void clearPath(List<String> segments) {
        Iterator<String> segmentIterator = segments.iterator();

        while (segmentIterator.hasNext()) {
            if (segmentIterator.next().isEmpty()) {
                segmentIterator.remove();
            }
        }
    }
    
    private static boolean isEscapedChar(String str, int index) {
        boolean escaped = false;
        
        for (int i=(index-1); i>0; i--) {
            if (str.charAt(i) == ESCAPE_CHAR) {
                escaped = !escaped;
            }
            else {
                break;
            }
        } 
        
        return escaped;
    }

    private static boolean isSpecialChar(char target) {
         
        for (char reference : SPECIAL_CHARS) {
            if (target == reference) {
                return true;
            }
        }

        return false;
    }

}
