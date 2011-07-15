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

package org.glassfish.appclient.client.acc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.glassfish.appclient.client.CLIBootstrap;

/**
 * Self-contained scanner for an agent argument string.
 * <p>
 * The agent arguments are a comma-separated sequence of
 * <code>[keyword=]quoted-or-unquoted-string</code>.
 * The "keyword=" part is optional.  A given keyword can 
 * appear multiple times, so after analysis each keyword can map
 * to potentially multiple values (as a List<String>).
 * <p>
 * This class organizes the agent arguments into named and anonymous values.
 *
 */
public class AgentArguments {

    /**
     * Pattern notes: The first group is non-capturing and tries to match the
     * keyword= part zero or one time.  The next group (*.*?) matches and captures
     * any keyword.  The ? immediately after that group means
     * 0 or 1 times.
     *
     * The next group is a quoted string not itself containing
     * a quotation mark.  The next group, an alternative (indicated by
     * the | mark) to the quoted string group, is a non-quoted string not containing a comma.
     * The pattern ends (non-capturing group) with an optional comma (could
     * be end-of-input so the comma is optional)
     */
//    private static final String AGENT_ARG_PATTERN = "(?:(.*?)=)?(?:\"([^\"]*)\"|([^,]+))(?:,?)";
    private static final String AGENT_ARG_PATTERN = "(?:([^=,]*?)=)?((?:\"([^\"]*)\")|[^,]+)";
    /* groups matching interesting parts of the regex */
    private static final int KEYWORD = 1;
    private static final int QUOTED = 2;
    private static final int UNQUOTED = 3;
    private static Pattern agentArgPattern = Pattern.compile(AGENT_ARG_PATTERN, Pattern.DOTALL);

    private final Map<String,List<String>> values = new HashMap<String,List<String>>();

    public static AgentArguments newInstance(final String args) {
        AgentArguments result = new AgentArguments();
        result.scan(args);
        return result;
    }

    /**
     * Returns the list of values associated with the specified keyword.
     * @param keyword the keyword whose values are needed
     * @return the values associated with the keyword; null if the keyword never appeared in the input
     */
    public List<String> namedValues(final String keyword) {
        return actualOrEmptyList(keyword);
    }

    /**
     * Returns the unnamed values as a list of strings.
     * @return List of Strings, one for each unnamed value in the scanned string
     */
    public List<String> unnamedValues() {
        return actualOrEmptyList(null);
    }

    private List<String> actualOrEmptyList(final String keyword) {
        return (values.get(keyword) != null ? values.get(keyword) : Collections.EMPTY_LIST);
    }
    
    /**
     * Scans the input args string, updating the nameValuePairs properties
     * object using items with a keyword and updated the singleWordArgs list
     * with items without a keyword.
     *
     * @param args input line to scan
     * @param nameValuePairs properties to augment with keyword entries in the input
     * @param singleWordArgs list of strings to augment with un-keyworded entries in the input
     */
    private void scan(final String args) {
        if (args == null) {
            return;
        }
        Matcher m = agentArgPattern.matcher(args);
        while (m.find()) {
            final String keyword = m.group(KEYWORD);
            /*
             * Either the quoted string group or the unquoted string group
             * from the matcher will be valid.
             */
            final String value = CLIBootstrap.decodeArg(m.group(QUOTED) != null ? m.group(QUOTED) : m.group(UNQUOTED));
            getOrCreateValuesForKeyword(keyword).add(value);
        }
    }

    private List<String> getOrCreateValuesForKeyword(final String keyword) {
        List<String> result = values.get(keyword);
        if (result == null) {
            result = new ArrayList<String>();
            values.put(keyword, result);
        }
        return result;
    }
}
