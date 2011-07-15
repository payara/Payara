/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.util;

/**
 * This is a utility class which will  be used
 * to format data where tabular formats cannot be used
 *
 * @author Bhakti Mehta
 */
public class RowFormatter {

    private int numRows = -1;
    private final String[] headings;

    public RowFormatter(String[] h) {
        headings = h;
        numRows = headings.length;
    }

    /**
     * This will return a String of the format
     * HEADING1  :value1
     * HEADING2  :value 2
     * @param objs : The values which are to be displayed
     * @return  The String containing the formatted headings and values
     */
    public String addColumn(Object[] objs ){
         // check to make sure the number of rows is the same as what we already have
        if (numRows != -1) {
            if (objs.length != numRows) {
                throw new IllegalArgumentException(
                        String.format("invalid number of columns (%d), expected (%d)",
                            objs.length, numRows));
            }
        }


        int longestValue = 0;
        for (int i = 0; i < numRows; i++) {
            if (headings != null && headings[i].length() > longestValue) {
                longestValue = headings[i].length();
            }


        }
        longestValue += 2;
        StringBuilder sb = new StringBuilder();

        StringBuilder formattedline = new StringBuilder("%1$-"+longestValue +"s:%2$-1s");
        for (int i = 0; i <numRows; i ++) {

            sb.append( String.format(formattedline.toString(),headings[i],objs[i])).append("\n");
        }
        return sb.toString();
    }
}

