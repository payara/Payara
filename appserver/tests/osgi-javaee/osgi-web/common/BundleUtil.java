/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 *
 * @author mohit
 */
public class BundleUtil {

//    String installerUrl = "http://localhost:8080/testOSGI/BundleInstaller";
//    String uninstallerUrl = "http://localhost:8080/testOSGI/BundleUninstaller";

    String installerUrl = "http://localhost:8080/testREST/web/bundleinstaller";
    String uninstallerUrl = "http://localhost:8080/testREST/web/bundleuninstaller";

    private static Reporter reporter;
    private static String testName;
    int failCode = 2;//code for deployment failure [see Reporter.java]

    // args[0] contains testname
    // args[1] contains testResultFile
    // args[2] if there, will contain installUrl.
    public static void main(String args[]) {
        BundleUtil bundleUtil = new BundleUtil();
        if(args.length == 2) {
	    testName = args[0];
            reporter = new Reporter(args[1]);	    
            //uninstall the previously installed bundle
            bundleUtil.uninstall();
        } else if(args.length == 3) {
            testName = args[0];
            reporter = new Reporter(args[1]);
            //install the bundle using given arguments.
            bundleUtil.install(args[2]);
        } else {
            System.out.println("Invalid Number of Arguments.");
        }
    }

    public void install(String installUrl) {
        //installerUrl = installerUrl + "?installType=" + installType;
        //installerUrl = installerUrl + "&bundlePath=" + bundlePath;
        String [] parameters = {"installUrl", installUrl};
        invokeURL(installerUrl, parameters);
    }

    public void uninstall() {
        invokeURL(uninstallerUrl, new String [] {});
    }

    public void invokeURL(String url, String[] parameters) {
        try {
            URL u = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) u.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            OutputStream out = connection.getOutputStream();
            Writer writer = new OutputStreamWriter(out, "UTF-8");
            for (int i = 0; i < parameters.length; i++) {
                writer.write(parameters[i++]);//first wtire param name, then value
                writer.write("=");
                writer.write(URLEncoder.encode(parameters[i], "UTF-8"));
                writer.write("&");
            }
            writer.close();
            out.close();

            int code = connection.getResponseCode();
            InputStream is = connection.getInputStream();
            BufferedReader input = new BufferedReader(new InputStreamReader(is));
            String line = null;
            while ((line = input.readLine()) != null) {
                log(line);
		if(line.contains("FAIL")){
 		    fail();
  		}
            }

            if (code != 200) {
                log("Error invoking " + url);
                fail();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void log(String message) {
        System.out.println("[BundleUtil]:: " + message);
    }

    private void fail() {
        System.out.println("[BundleUtil]:: TestFailed");
        reporter.printStatus(testName, failCode);
    }
}

