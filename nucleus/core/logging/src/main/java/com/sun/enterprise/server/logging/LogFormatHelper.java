/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.server.logging;

import java.util.regex.Pattern;

/**
 * Helper class that provides methods to detect the log format of a record. 
 *
 */
public class LogFormatHelper {

    private static final int ODL_SUBSTRING_LEN = 5;
    
    private static final String ODL_LINE_BEGIN_REGEX = "\\[(\\d){4}";
    
    private static final class PatternHolder {
        private static final Pattern ODL_PATTERN = Pattern.compile(ODL_LINE_BEGIN_REGEX);
    }
    
    /**
     * Determines whether the given line is the beginning of a UniformLogFormat log record.
     * @param line
     * @return
     */
    public static boolean isUniformFormatLogHeader(String line) {
        if (line.startsWith("[#|") && countOccurrences(line, '|') > 4) { 
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Determines whether the given line is the beginning of a ODL log record.
     * @param line
     * @return
     */
    public static boolean isODLFormatLogHeader(String line) {
        if (line.length() > ODL_SUBSTRING_LEN
                && PatternHolder.ODL_PATTERN.matcher(
                        line.substring(0, ODL_SUBSTRING_LEN))
                        .matches()
                && countOccurrences(line, '[') > 4) {
            return true;
        } else {
            return false;
        }
    }

    private static int countOccurrences(String haystack, char needle) {
        int count = 0;
        for (int i = 0; i < haystack.length(); i++) {
            if (haystack.charAt(i) == needle) {
                count++;
            }
        }
        return count;
    }    
    
}
