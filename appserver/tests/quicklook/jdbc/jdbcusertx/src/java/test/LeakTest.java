/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package myapp.test;

import java.io.PrintWriter;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import myapp.util.HtmlUtil;

/**
 *
 * @author jagadish
 */
public class LeakTest implements SimpleTest{

    
       Map<String, Boolean> resultsMap = new HashMap<String, Boolean>();

    public Map<String, Boolean> runTest(DataSource ds1, PrintWriter out) {
        try {
            if (checkForNoLeak(ds1, out)) {
                resultsMap.put("no-leak-test", true);
            }else{
                resultsMap.put("no-leak-test", false);
            }
        } catch (Exception e) {
            resultsMap.put("no-leak-test", false);
        }

        return resultsMap;

    }
        private boolean checkForNoLeak(DataSource ds1, PrintWriter out) {
        int count = 32;
        boolean result = false;
        Connection[] connections = new Connection[count];

        HtmlUtil.printHR(out);
        out.println("<h4> no leak test </h4>");
        try {
            for (int i = 0; i < count; i++) {
                connections[i] = ds1.getConnection();
            }
            out.println("able to retrieve all 32 connections<br>");
            result = true;
        } catch (Exception e) {
            HtmlUtil.printException(e, out);
            result = false;
        } finally {

            try {
                for (Connection con : connections) {
                    try {
                        if (con != null) {
                            con.close();
                        }
                    } catch (Exception e) {

                    }
                }
            } catch (Exception e) {
                HtmlUtil.printException(e, out);
            }
            HtmlUtil.printHR(out);
            return result;
        }
     }
}
