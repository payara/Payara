/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.nucleus.admin.progress;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parse progress status message.
 *
 * @author martinmares
 */
public class ProgressMessage {
    
    private static final Pattern RGXP = Pattern.compile(" *(\\d+)(%)?:(.+:)?(.*)");
    
    private final int value;
    private final boolean percentage;
    private final String scope;
    private final String message;
    
    public ProgressMessage(String txt) throws IllegalArgumentException {
        Matcher matcher = RGXP.matcher(txt);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Arg txt is not progress message");
        }
        this.value = Integer.parseInt(matcher.group(1));
        this.percentage = matcher.group(2) != null;
        this.scope = nvlTrim(matcher.group(3));
        this.message = nvlTrim(matcher.group(4));
    }
    
    private static String nvlTrim(String txt) {
        if (txt != null) {
            txt = txt.trim();
        }
        return txt;
    }

    public int getValue() {
        return value;
    }

    public boolean isPercentage() {
        return percentage;
    }

    public String getScope() {
        return scope;
    }

    public String getMessage() {
        return message;
    }
    
    public static List<ProgressMessage> grepProgressMessages(String txt) {
        StringTokenizer stok = new StringTokenizer(txt, "\n\r");
        List<ProgressMessage> result = new ArrayList<ProgressMessage>();
        while (stok.hasMoreTokens()) {
            String line = stok.nextToken();
            try {
                result.add(new ProgressMessage(line));
            } catch (Exception ex) {
                //System.out.println(ex);
            }
        }
        return result;
    }
    
    /** Unique only that not equal with previous.
     */
    public static String[] uniqueMessages(List<ProgressMessage> pms) {
        List<String> messages = new ArrayList<String>();
        for (ProgressMessage pm : pms) {
            if (pm.getMessage() != null && 
                    (messages.isEmpty() || !pm.getMessage().equals(messages.get(messages.size() - 1)))) {
                messages.add(pm.getMessage());
            }
        }
        String[] result = new String[messages.size()];
        return messages.toArray(result);
    }
    
    public static boolean isNonDecreasing(List<ProgressMessage> pms) {
        if (pms == null) {
            return false;
        }
        int lastVal = Integer.MIN_VALUE;
        for (ProgressMessage pm : pms) {
            if (pm.getValue() < lastVal) {
                return false;
            } else {
                lastVal = pm.getValue();
            }
        }
        return true;
    }
    
}
