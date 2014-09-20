/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author Tom Mueller
 */
public class ColumnFormatter {
    private int numCols = -1;
    private String headings[];
    private List<String[]> valList = new ArrayList<String[]>();

    public ColumnFormatter(String headings[]) {
        this.headings = headings;
        numCols = headings.length;
    }

    public ColumnFormatter() {
        this.headings = null;
    }

    public void addRow(Object values[]) throws IllegalArgumentException {
        // check to make sure the number of columns is the same as what we already have
        if (numCols != -1) {
            if (values.length != numCols) {
                throw new IllegalArgumentException(
                        Strings.get("column.internal", values.length, numCols));
            }
        }
        numCols = values.length;
        String v[] = new String[numCols];
        for (int i = 0; i < v.length; i++) {
            v[i] = values[i] == null ? "" : values[i].toString();
        }
        valList.add(v);
    }

    /**
     * Get the content of all rows along with the headings as a List of Map.
     * Note : If there are duplicate headings, latest entry and its value will take precedence.
     * This can be useful in case the CLI is used by GUI via REST as GUI expects a List of Map.
     * @return List of Map all entries in in the ColumnFormatter
     */
    public List<Map<String,String>> getContent(){
        List<Map<String,String>> rows = new ArrayList<Map<String, String>>();

        for(String[] values : valList){
            Map<String,String> entry = new TreeMap<String, String>();
            int i = 0;
            for(String value : values){
                entry.put(headings[i], value);
                i++;
            }
            rows.add(entry);
        }
        return rows;
    }

    @Override
    public String toString() {
        // no data
        if (numCols == -1) {
            return "";
        }

        int longestValue[] = new int[numCols];
        for (String v[] : valList) {
            for (int i = 0; i < v.length; i++) {
                if (v[i].length() > longestValue[i]) {
                   longestValue[i] = v[i].length();
                }
            }
        }

        StringBuilder formattedLineBuf = new StringBuilder();
        for (int i = 0; i < numCols; i++) {
            if (headings != null && headings[i].length() > longestValue[i]) {
                longestValue[i] = headings[i].length();
            }
            longestValue[i] += 2;
            formattedLineBuf.append("%-")
                    .append(longestValue[i])
                    .append("s");
        }
        String formattedLine = formattedLineBuf.toString();
        StringBuilder sb = new StringBuilder();

        boolean havePrev = false;
        if (headings != null) {
            sb.append(String.format(formattedLine, (Object[])headings));
            havePrev = true;
        }

        // no linefeed at the end!!!
        for (String v[] : valList) {
            if (havePrev) {
                sb.append('\n');
            }
            sb.append(String.format(formattedLine, (Object[])v));
            havePrev = true;
        }

        return sb.toString();
    }
}
