/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved.
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

package fromjava.client;

import org.testng.annotations.*;
import org.testng.Assert;

import java.lang.reflect.*;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;

/**
 * Test very basic Webservice deployed using jsr109 deployment and tests that "?Tester" console page loads correctly
 *
 * @author miroslav
 */
public class CheckTesterUITestNG {

    public static final String CLASS_SERVICE = "fromjava.client.AddNumbersService";
    public static final String METHOD_GET_PORT = "getAddNumbersPort";
    public static final String METHOD_ADD_NUMBERS = "addNumbers";
    public static final String URL_TESTER_PAGE = "http://localhost:8080/JaxwsFromJava/AddNumbersService?Tester";
    public static final String HEADER_TESTER_PAGE = "AddNumbersService Web Service Tester";

    private Object port = null;
    private Method method = null;

    @BeforeTest
    public void loadClass() throws Exception {
        try {
            Class cls = Class.forName(CLASS_SERVICE);
            Constructor ct = cls.getConstructor();
            Object svc = ct.newInstance();
            Method getPort = cls.getMethod(METHOD_GET_PORT);
            port = getPort.invoke(svc, (Object[]) null);
        } catch (Exception ex) {
            System.out.println("Got ex, class is not loaded.");
            throw new Exception(ex);
        }
        System.out.println("done for init");
    }

    @Test(groups = {"functional"})
    public void testAddNumbers() throws Exception {
        int result = 0;
        try {
            for (Method m : port.getClass().getDeclaredMethods()) {
                System.out.println("method = " + m.getName());
            }
            method = port.getClass().getMethod(METHOD_ADD_NUMBERS, int.class, int.class);
            result = (Integer) method.invoke(port, 1, 2);
        } catch (Exception ex) {
            System.out.println("got unexpected exception.");
            throw new Exception(ex);
        }
        Assert.assertTrue(result == 3);
    }

    @Test(groups = {"functional"})
    public void testTesterUI() throws Exception {
        String testerPageHTML = wget(URL_TESTER_PAGE);
        System.out.println("Tester Page HTML = " + testerPageHTML);
        Assert.assertTrue(testerPageHTML.contains(HEADER_TESTER_PAGE));
    }

    public static String wget(String url) throws Exception {
        BufferedReader in = null;
        try {
            URLConnection connection = new URL(url).openConnection();
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            return response.toString();
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

}
