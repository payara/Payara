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

package org.glassfish.appclient.server.core.jws;

import java.net.URI;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.glassfish.grizzly.http.server.Request;

/**
 *
 * @author tjquinn
 */
public class Util {
    /**
     * pattern is: "${" followed by all chars excluding "}" followed by "}",
     * capturing into group 1 all chars between the "${" and the "}"
     */
    private static final Pattern TOKEN_SUBSTITUTION = Pattern.compile("\\$\\{([^\\}]*)\\}");
    private static final String SLASH_REPLACEMENT = Matcher.quoteReplacement("\\\\");
    private static final String DOLLAR_REPLACEMENT = Matcher.quoteReplacement("\\$");


    /**
     * Searches for placeholders of the form ${token-name} in the input String, retrieves
     * the property with name token-name from the Properties object, and (if
     * found) replaces the token in the input string with the property value.
     * @param s String possibly containing tokens
     * @param values Properties object containing name/value pairs for substitution
     * @return the original string with tokens substituted using their values
     * from the Properties object
     */
    public static String replaceTokens(String s, Properties values) {
        Matcher m = TOKEN_SUBSTITUTION.matcher(s);

        StringBuffer sb = new StringBuffer();
        /*
         * For each match, retrieve group 1 - the token - and use its value from
         * the Properties object (if found there) to replace the token with the
         * value.
         */
        while (m.find()) {
            String propertyName = m.group(1);
            String propertyValue = values.getProperty(propertyName);

            /*
             * Substitute only if the properties object contained a setting
             * for the placeholder we found.
             */
            if (propertyValue != null) {
                /*
                 * The next line quotes any $ signs and backslashes in the replacement string
                 * so they are not interpreted as meta-characters by the regular expression
                 * processor's appendReplacement.
                 */
                String adjustedPropertyValue =
                        propertyValue.replaceAll("\\\\",SLASH_REPLACEMENT).
                            replaceAll("\\$", DOLLAR_REPLACEMENT);
                String x = s.substring(m.start(),m.end());
                try {
                    m.appendReplacement(sb, adjustedPropertyValue);
                } catch (IllegalArgumentException iae) {
                    System.err.println("**** appendReplacement failed: segment is " + x + "; original replacement was " + propertyValue + " and adj. replacement is " + adjustedPropertyValue + "; exc follows");
                    throw iae;
                }
            }
        }
        /*
         * There are no more matches, so append whatever remains of the matcher's input
         * string to the output.
         */
        m.appendTail(sb);

        return sb.toString();
    }

    public static String toXMLEscapedInclAmp(final String content) {
        return toXMLEscaped(content.replaceAll("&", "&amp;"));
    }
    public static String toXMLEscaped(final String content) {
        return content.
                    replaceAll("<", "&lt;").
                    replaceAll(">", "&gt;").
                    replaceAll("\"", "&quot;");
    }
    
    public static URI getCodebase(final Request gReq) {
        return URI.create(gReq.getScheme() + "://" + gReq.getServerName());
    }
}
