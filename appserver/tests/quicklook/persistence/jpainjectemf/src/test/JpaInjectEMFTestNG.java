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

package test.jpa.jpainjectemf;

import org.testng.annotations.Configuration;
import org.testng.annotations.ExpectedExceptions;
import org.testng.annotations.Test;
import org.testng.annotations.*;
import org.testng.Assert;

import java.io.*;
import java.net.*;
import java.util.*;

public class JpaInjectEMFTestNG {

    private String strContextRoot = "/jpainjectemf";

    static String result = "";
    String host = System.getProperty("http.host");
    String port = System.getProperty("http.port");

    @Test(groups = { "init" })
    public void persistWithInjectEMF() throws Exception {
        boolean result = false;

        try {
            result = test("llinit");
            Assert.assertEquals(result, true, "Unexpected Results");
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e);
        }
    }

    @Test(dependsOnGroups = { "init.*" })
    public void lazyLoadingByQuery() throws Exception {
        boolean result = false;

        try {
            result = test("llquery");
            Assert.assertEquals(result, true, "Unexpected Results");
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e);
        }

    }

    private boolean test(String c) throws Exception {
        String EXPECTED_RESPONSE = c + ":pass";
        boolean result = false;
        String url = "http://" + host + ":" + port + strContextRoot + "/jpa?testcase=" + c;
        System.out.println("url = " + url);

        HttpURLConnection conn = (HttpURLConnection) (new URL(url)).openConnection();
        int code = conn.getResponseCode();
        if (code != 200) {
            System.out.println("Unexpected return code: " + code);
        } else {
            
            System.out.println("Looking in response for " + EXPECTED_RESPONSE);
            
            InputStream is = conn.getInputStream();
            BufferedReader input = new BufferedReader(new InputStreamReader(is));
            String line = null;
            while ((line = input.readLine()) != null) {
                System.out.println(line);
                if (line.contains(EXPECTED_RESPONSE)) {
                    result = true;
                    break;
                }
            }
            
            if (result == false) {
                System.out.println("Expected response not found");
            }
        }
        
        return result;
    }

    public static void echo(String msg) {
        System.out.println(msg);
    }

}
