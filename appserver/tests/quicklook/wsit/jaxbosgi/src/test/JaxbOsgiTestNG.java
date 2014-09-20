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
package jaxbosgi;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class JaxbOsgiTestNG {

    String host = System.getProperty("http.host");
    String port = System.getProperty("http.port");

    @Test(groups = {"pulse"}) // test method
    public void riOsgiTest() throws Exception {
        StringBuilder parseResult = getUrl("index.jsp");
        String EXPECTED_RESPONSE = "jaxbimpl:";
        String result = null;
        int resultStart = parseResult.indexOf(EXPECTED_RESPONSE);
        if (resultStart != -1) {
            result = parseResult.substring(resultStart + EXPECTED_RESPONSE.length());
        }
        if ((result == null) || !(result.contains("com.sun.xml"))) {
            Assert.fail("Unexpected JAXB Implementation loaded: " + result);
        }
    }

    @Test(groups = {"pulse"}) // jaxb1 class exist test
    public void jaxb1Exist() throws Exception {
        StringBuilder sb = getUrl("jaxb1");
        boolean allOk = sb.indexOf("JAXB1 is OK") != -1;
        if (!allOk) {
            if (!testFail("jaxbObject is null", sb))
                if (!testFail("JAXB1 is not found", sb))
                    Assert.fail("No JAXB1 data found");
        }
    }

    @Test(groups = {"pulse"}) // msv datatype class exist test
    public void msvDatatypeExist() throws Exception {
        StringBuilder sb = getUrl("msv");
        boolean allOk = sb.indexOf("intType is OK") != -1;
        if (!allOk) {
            if (!testFail("intType is null", sb))
                if (!testFail("intType is not found", sb))
                    Assert.fail("No intType data found");
        }
    }

    @Test(groups = {"pulse"}) // jaxb1 marshalling test
    public void jaxb1Marshalling() throws Exception {
        StringBuilder sb = getUrl("jaxb1m");
        boolean allOk = sb.indexOf("marshalled OK") != -1;
        if (!allOk) {
            if (!testFail("jaxb1 marshalling failed", sb))
                Assert.fail("No jaxb1 marshalling data found");
        }
    }

    private boolean testFail(String msg, StringBuilder body) {
        if (body.indexOf(msg) != -1) {
            Assert.fail(msg);
            return true;
        }
        return false;
    }

    private StringBuilder getUrl(String path) throws IOException {
        StringBuilder result = new StringBuilder();
        URL url = new URL("http://" + host + ":" + port + "/jaxbosgi/" + path);
        InputStream is = null;
        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.connect();

            is = conn.getInputStream();
            BufferedReader input = new BufferedReader(new InputStreamReader(is));
            for (String line; (line = input.readLine()) != null; )
                result.append(line);
        } finally {
            if (is != null) {
                is.close();
            }
        }
        return result;
    }
}
