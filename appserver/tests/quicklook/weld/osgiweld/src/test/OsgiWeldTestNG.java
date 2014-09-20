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

package test.weld.osgi;

import org.testng.annotations.*;
import org.testng.Assert;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Test Weld Osgi Bundle Integrity 
 *
 * @author Santiago.PericasGeertsen@oracle.com
 * @author Roger.Kitain@oracle.com
 */
public class OsgiWeldTestNG {

    private String strContextRoot="osgiweld";

    String m_host="";
    String m_port="";

    @BeforeMethod
    public void beforeTest(){
        m_host=System.getProperty("http.host");
        m_port=System.getProperty("http.port");
    }

    @DataProvider(name = "exports")
    public Object[][] getExportData() throws Exception {
        Properties props = new Properties();
        props.load(this.getClass().getResourceAsStream("weld-osgi.properties"));

        Object[] exportPackages = new Object[1];
        exportPackages[0] = props.getProperty("exports");
        return (new Object[][] {exportPackages});
    }

//    @Test(groups = {"pulse"}, dataProvider = "exports")
    public void testOsgiModuleIntegrity(String exports) throws Exception {
        try {
            boolean result = checkManifestAttributes();
            Assert.assertEquals(result, true, "Unexpected HTML");
            result = checkExports(exports);
            Assert.assertEquals(result, true, "Unexpected Package Exports");
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e);
        }
    }

    private boolean checkManifestAttributes() throws Exception {
        String testUrl = "http://" + m_host  + ":" + m_port + "/"
                    + strContextRoot +"/OsgiWeld?command=manifest";
        boolean result = checkForString(testUrl, "OK");
        return result;
    }

    private boolean checkExports(String exports) throws Exception {
        boolean result = false;
        String testurl = "http://" + m_host  + ":" + m_port + "/"
                + strContextRoot +"/OsgiWeld?command=exports";
        URL url = new URL(testurl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.connect();
        int responseCode = conn.getResponseCode();

        InputStream is = conn.getInputStream();
        BufferedReader input = new BufferedReader(new InputStreamReader(is));
        String line = input.readLine();
        if (line.equals("ERROR")) {
            return false;
        }
        result = exports.equals(line);
        if (!result) {
            System.out.println("The packages exported by the weld-osgi-bundle do not match the expected packages");
        }
        return result;
    }

    private boolean checkForString(String testurl, String str) throws Exception {
        //System.out.println("Checking for " + str + "in " + testurl);
        URL url = new URL(testurl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.connect();
        int responseCode = conn.getResponseCode();

        InputStream is = conn.getInputStream();
        BufferedReader input = new BufferedReader(new InputStreamReader(is));

        String line = null;
        boolean result = false;
        String testLine = null;
        while ((line = input.readLine()) != null) {
            //System.out.println("line:" + line);
            if (line.indexOf(str) != -1) {
                result = true;
                testLine = line;
            }
        }
        return result;
    }
}
