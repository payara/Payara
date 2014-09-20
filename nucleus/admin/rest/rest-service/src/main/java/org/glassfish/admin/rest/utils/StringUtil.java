/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.admin.rest.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import org.glassfish.api.ActionReport;
import org.glassfish.api.ActionReport.MessagePart;

/**
 *
 * @author tmoreau
 */
// TODO: unit tests
public class StringUtil {

    /**
     * Compare two lists of strings. TBD - support a form that ignores ordering?
     *
     * @param list1
     * @param list2
     * @return boolean indicating if the list of strings have the ssame values in the same order.
     */
    public static boolean compareStringLists(List<String> list1, List<String> list2) {
        // TBD : should compare irrespective of the order of the values
        return (list1.equals(list2));
    }
    
    public static boolean compareStrings(String str1, String str2) {
        if (str1 == null) {
            return str2 == null;
        } else {
            return str1.equals(str2);
        }
    }
    
    public static boolean compareStringsIgnoreCase(String str1, String str2) {
        if (str1 == null) {
            return str2 == null;
        } else {
            return str1.equalsIgnoreCase(str2);
        }
    }

    /**
     * Return the message parts of an action report as a List<String>
     *
     * @param actionReport
     * @return List<String> containing actionReport's message parts.
     */
    public static List<String> getActionReportMessageParts(ActionReport actionReport) {
        List<String> parts = new ArrayList<String>();
        for (MessagePart part : actionReport.getTopMessagePart().
                getChildren()) {
            parts.add(part.getMessage());
        }
        return parts;
    }

    /**
     * Convert a List<String> to a comma-separated string. This is often used to format strings that are sent to admin
     * commands.
     *
     * @param strings
     * @return String a comma-separated string containing strings.
     */
    public static String getCommaSeparatedStringList(List<String> strings) {
        StringBuilder sb = new StringBuilder();
        if (strings != null) {
            boolean first = true;
            for (String str : strings) {
                if (!first) {
                    sb.append(",");
                }
                sb.append(str);
                first = false;
            }
        }
        return sb.toString();
    }

    /**
     * Convert a string containing a comma-separated list of strings into a List<String>. This is often used to parse
     * strings that are returned from admin commands.
     *
     * @param stringList
     * @return List<String> containing the strings in stringList.
     */
    public static List<String> parseCommaSeparatedStringList(String stringList) {
        List<String> list = new ArrayList<String>();
        if (stringList != null) {
            for (StringTokenizer st = new StringTokenizer(stringList, ","); st.hasMoreTokens();) {
                list.add(st.nextToken().trim());
            }
        }
        return list;
    }

    /**
     * Determines if a string is null/empty or not
     *
     * @param string
     * @return true if the string is not null and has a length greater than zero, false otherwise
     */
    public static boolean notEmpty(String string) {
        return (string != null && !string.isEmpty());
    }

    /**
     * Converts a null/empty/non-empty string to null or non-empty
     *
     * @param string
     * @return null if string is null or empty, otherwise returns string
     */
    public static String nonEmpty(String string) {
        return (notEmpty(string)) ? string : null;
    }

    /**
     * Converts a null/empty/non-empty string to empty or non-empty
     *
     * @param string
     * @return an empty string if string is null or empty, otherwise returns string
     */
    public static String nonNull(String string) {
        return (notEmpty(string)) ? string : "";
    }
}
