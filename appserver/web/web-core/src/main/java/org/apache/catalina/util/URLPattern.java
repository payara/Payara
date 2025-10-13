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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Portions Copyright [2017] Payara Foundation and/or its affiliates

package org.apache.catalina.util;

/**
 * 
 * @author Ron Monzillo
 *
 * @serial exclude
 */
public class URLPattern extends Object implements Comparable {

    /* changed to order default pattern / below extension */
    public static final int PT_DEFAULT = 0;
    public static final int PT_EXTENSION = 1;
    public static final int PT_PREFIX = 2;
    public static final int PT_EXACT = 3;

    private static String DEFAULT_PATTERN = "/";
    private final String pattern;
    private int patternType = -1;

    public URLPattern() {
        this.pattern = DEFAULT_PATTERN;
        this.patternType = PT_DEFAULT;
    }

    // Note that the EMPTY_STRING is a legitimate URL_PATTERN, so only check for null
    public URLPattern(String urlPattern) {
        if (urlPattern == null) {
            this.pattern = DEFAULT_PATTERN;
            this.patternType = PT_DEFAULT;
        } else {
            this.pattern = urlPattern;
        }
    }

    public int patternType() {
        if (this.patternType < 0) {
            if (this.pattern.startsWith("*.")) {
                this.patternType = PT_EXTENSION;
            } else if (this.pattern.startsWith("/") && this.pattern.endsWith("/*")) {
                this.patternType = PT_PREFIX;
            } else if (this.pattern.equals(DEFAULT_PATTERN)) {
                this.patternType = PT_DEFAULT;
            } else {
                this.patternType = PT_EXACT;
            }
        }
        return this.patternType;
    }

    @Override
    public int compareTo(Object object) {
        if (!(object instanceof URLPattern)) {
            throw new ClassCastException("Argument must be URLPattern");
        }

        URLPattern urlPattern = (URLPattern) object;

        int refPatternType = this.patternType();

        /* The comparison yields increasing sort order by pattern type. That is, prefix patterns sort before exact
	 * patterns. Also shorter length patterns precede longer length patterns. This is important for the 
         * URLPatternList canonicalization done by URLPatternSpec.setURLPatternArray
         */
        int result = refPatternType - urlPattern.patternType();

        if (result == 0) {
            if (refPatternType == PT_PREFIX || refPatternType == PT_EXACT) {
                result = this.getPatternDepth() - urlPattern.getPatternDepth();
                if (result == 0) {
                    result = this.pattern.compareTo(urlPattern.pattern);
                }
            } else {
                result = this.pattern.compareTo(urlPattern.pattern);
            }
        }
        return result > 0 ? 1 : (result < 0 ? -1 : 0);
    }

    /**
     * Does this pattern imply (that is, match) the argument pattern? This method follows the same rules (in the same
     * order) as those used for mapping requests to servlets.
     * <P>
     * Two URL patterns match if they are related as follows:
     * <p>
     * <ul>
     * <li> their pattern values are String equivalent, or
     * <li> this pattern is the path-prefix pattern "/*", or
     * <li> this pattern is a path-prefix pattern (that is, it starts with "/" and ends with "/*") and the argument
     * pattern starts with the substring of this pattern, minus its last 2 characters, and the next character of the
     * argument pattern, if there is one, is "/", or
     * <li> this pattern is an extension pattern (that is, it starts with "*.") and the argument pattern ends with this
     * pattern, or
     * <li> the reference pattern is the special default pattern, "/", which matches all argument patterns.
     * </ul>
     *
     * @param urlPattern URLPattern whose path will be compared to this URLPattern
     * @return Whether the given URLPattern matches the present URLPattern.
     */
    public boolean implies(URLPattern urlPattern) {

        // Normalize the argument
        if (urlPattern == null) {
            urlPattern = new URLPattern(null);
        }

        String path = urlPattern.pattern;
        String currentPattern = this.pattern;

        // First check for exact match
        if (currentPattern.equals(path)) {
            return true;
        }

        // Check for path prefix matching
        if (currentPattern.startsWith("/") && currentPattern.endsWith("/*")) {
            currentPattern = currentPattern.substring(0, currentPattern.length() - 2);
            int length = currentPattern.length();
            if (length == 0) {
                return true; // "/*" is the same as the DEFAULT_PATTERN
            }
            return path.startsWith(currentPattern) 
                    && (path.length() == length || path.substring(length).startsWith("/"));
        }

        // Check for suffix matching
        if (currentPattern.startsWith("*.")) {
            int slash = path.lastIndexOf('/');
            int period = path.lastIndexOf('.');
            return slash >= 0 && period > slash && path.endsWith(currentPattern.substring(1));
        }

        // Finally, check for universal mapping
        return currentPattern.equals(DEFAULT_PATTERN);
    }

    public int getPatternDepth() {
        int i = 0;
        int depth = 1;

        while (i >= 0) {
            i = this.pattern.indexOf("/", i);
            if (i >= 0) {
                if (i == 0 && depth != 1) {
                    throw new IllegalArgumentException("// in pattern");
                }
                i++;
            }
        }
        return depth;
    }
    
    /**
     * Minor utility method which instantiates a pattern before comparing it against another.
     * @param originalPattern The pattern to compare against
     * @param newPattern The pattern to compare
     * @return 
     */
    public static boolean match(String originalPattern, String newPattern) {
        URLPattern original = new URLPattern(originalPattern);
        URLPattern comparedPattern = new URLPattern(newPattern);
        return original.implies(comparedPattern);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof URLPattern)) {
            return false;
        }
        return this.pattern.equals(((URLPattern) object).pattern);
    }

    @Override
    public String toString() {
        return this.pattern;
    }
}
