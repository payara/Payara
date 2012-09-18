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

package org.glassfish.nucleus.admin.rest;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.Response;
import static org.testng.AssertJUnit.*;
import org.testng.annotations.Test;
/**
 *
 * @author jasonlee
 */
public class ApplicationTest extends RestTestBase {
    public static final String BASE_JDBC_RESOURCE_URL = "/domain/resources/jdbc-resource";
    public static final String URL_APPLICATION_DEPLOY = "/domain/applications/application";
    public static final String URL_CODI_SAMPLE = "http://java.net/jira/secure/attachment/44850/GlassfishIssues.war";
    public static final String URL_CREATE_INSTANCE = "/domain/create-instance";

    @Test(enabled=false)
    public void testApplicationDeployment() throws URISyntaxException {
        final String appName = "testApp" + generateRandomString();

        try {
            Map<String, String> deployedApp = deployApp(getFile("test.war"), appName, appName);
            assertEquals(appName, deployedApp.get("name"));

            assertEquals("/" + appName, deployedApp.get("contextRoot"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            undeployApp(appName);
        }
    }

//    @Test(enabled=false)
    public void deployCodiApp() throws URISyntaxException, MalformedURLException, IOException {
        try {
            final String appName = "testApp" + generateRandomString();

            Map<String, String> params = new HashMap<String, String>();
            params.put("name", "CloudBeesDS");
            params.put("poolName", "DerbyPool");

            Response response = post (BASE_JDBC_RESOURCE_URL, params);
            assertTrue(isSuccess(response));

            Map<String, String> deployedApp = deployApp(downloadFile(new URL(URL_CODI_SAMPLE)), appName, appName);
            assertEquals(appName, deployedApp.get("name"));

            assertEquals("/" + appName, deployedApp.get("contextRoot"));

            undeployApp(appName);
        } finally {
            delete(BASE_JDBC_RESOURCE_URL + "/CloudBeesDS");
        }
    }

    @Test(enabled=false)
    public void testApplicationDisableEnable() throws URISyntaxException {
        final String appName = "testApp" + generateRandomString();

        Map<String, String> deployedApp = deployApp(getFile("test.war"), appName, appName);
        assertEquals(appName, deployedApp.get("name"));

        assertEquals("/" + appName, deployedApp.get("contextRoot"));

        try {
            String appUrl = "http://localhost:" + instancePort + "/" + appName;
            Response response = get(appUrl);
            assertEquals ("Test", response.readEntity(String.class).trim());

            response = post(URL_APPLICATION_DEPLOY + "/" + appName + "/disable");
            checkStatusForSuccess(response);

            response = get(appUrl);
            assertFalse("Response was " + response.getStatus(), isSuccess(response));

            response = post(URL_APPLICATION_DEPLOY + "/" + appName + "/enable");
            checkStatusForSuccess(response);

            response = get(appUrl);
            assertEquals ("Test", response.readEntity(String.class).trim());
        } finally {
            undeployApp(appName);
        }
    }

    @Test(enabled=false)
    public void listSubComponents() throws URISyntaxException {
        final String appName = "testApp" + generateRandomString();

        try {
            deployApp(getFile("stateless-simple.ear"), appName, appName);
            Response response = get(URL_APPLICATION_DEPLOY +"/" + appName + "/list-sub-components?id=" + appName);
            checkStatusForSuccess(response);
            String subComponents = response.readEntity(String.class);
            assertTrue(subComponents.contains("stateless-simple.war"));

            response = get(URL_APPLICATION_DEPLOY +"/" + appName + "/list-sub-components?id=stateless-simple.war&appname=" + appName);
            checkStatusForSuccess(response);
            subComponents = response.readEntity(String.class);
            assertTrue(subComponents.contains("GreeterServlet"));
        } finally {
            undeployApp(appName);
        }
    }

    @Test(enabled=false)
    public void testCreatingAndDeletingApplicationRefs() throws URISyntaxException {
        final String instanceName = "instance_" + generateRandomString();
        final String appName = "testApp" + generateRandomString();
        final String appRefUrl = "/domain/servers/server/" + instanceName + "/application-ref";

        Map<String, String> newInstance = new HashMap<String, String>() {{
            put("id", instanceName);
            put("node", "localhost-domain1");
        }};
        Map<String, String> applicationRef = new HashMap<String, String>() {{
            put("id", appName);
            put("target", instanceName);
        }};

        try {
            Response response = post(URL_CREATE_INSTANCE, newInstance);
            checkStatusForSuccess(response);

            deployApp(getFile("test.war"), appName, appName);

            response = post (appRefUrl, applicationRef);
            checkStatusForSuccess(response);

            response = get(appRefUrl + "/" + appName);
            checkStatusForSuccess(response);

            response = delete(appRefUrl + "/" + appName, new HashMap<String, String>() {{ put("target", instanceName); }});
            checkStatusForSuccess(response);
        } finally {
            Response response = delete("/domain/servers/server/" + instanceName + "/delete-instance");
            checkStatusForSuccess(response);
            response = get("/domain/servers/server/" + instanceName);
            assertFalse(isSuccess(response));
            undeployApp(appName);
        }
    }

    @Test(enabled=false)
    public void testGetContextRoot() throws URISyntaxException {
        final String appName = "testApp" + generateRandomString();

        try {
            Map<String, String> deployedApp = deployApp(getFile("stateless-simple.ear"), appName, appName);
            assertEquals(appName, deployedApp.get("name"));
            Map<String, String> contextRootPayload = new HashMap<String, String>() {{
                put("appname", appName);
                put("modulename", "stateless-simple.war");
            }};

            Response response = get("/domain/applications/application/" +appName + "/get-context-root", contextRootPayload);
            checkStatusForSuccess(response);
            assertTrue(response.readEntity(String.class).contains("helloworld"));
        } finally {
            undeployApp(appName);
        }
    }

//    @Test(enabled=false)
    public void testUndeploySubActionWarnings() throws URISyntaxException {
        final String appName = "testApp" + generateRandomString();
        final String serverName = "in" + generateRandomNumber();
        try {
            Response response = post ("/domain/create-instance", new HashMap<String, String>() {{
                put("id", serverName);
                put("node", "localhost-domain1");
            }});
            checkStatusForSuccess(response);

            response = post("/domain/servers/server/" + serverName + "/start-instance");
            checkStatusForSuccess(response);

            deployApp(getFile("test.war"), appName, appName);
            addAppRef(appName, serverName);

            response = post("/domain/servers/server/" + serverName + "/stop-instance");
            checkStatusForSuccess(response);

            response = delete ("/domain/applications/application/"+appName, new HashMap<String, String>() {{
                put("target", "domain");
            }});
            assertTrue(response.readEntity(String.class).contains("WARNING: Instance " + serverName + " seems to be offline"));
        } finally {
            delete ("/domain/applications/application/" + appName, new HashMap<String, String>() {{
                put("target", "domain");
            }});
        }
    }

    protected File getFile(String fileName) throws URISyntaxException {
        final URL resource = getClass().getResource("/" + fileName);
        return new File(resource.toURI());
    }

    protected File downloadFile(URL url) throws IOException {
        String urlText = url.getFile();
        String fileName = urlText.substring(urlText.lastIndexOf("/")+1);
        File file = new File(fileName);
        file.deleteOnExit();
        BufferedInputStream in = new BufferedInputStream(url.openStream());
        FileOutputStream fos = new FileOutputStream(file);
        BufferedOutputStream bout = new BufferedOutputStream(fos, 1024);
        byte data[] = new byte[8192];
        int read = in.read(data, 0, 8192);
        while (read >= 0) {
            bout.write(data, 0, read);
            data = new byte[8192];
            read = in.read(data, 0, 8192);
        }
        bout.close();
        in.close();

        return file;
    }

    public Map<String, String> deployApp (final String fileName, final String contextRoot, final String name) throws URISyntaxException {
        return deployApp(getFile(fileName), contextRoot, name);
    }

    public Map<String, String> deployApp (final File archive, final String contextRoot, final String name) {
        Map<String, Object> app = new HashMap<String, Object>() {{
            put("id", archive);
            put("contextroot", contextRoot);
            put("name", name);
        }};

        Response response = postWithUpload(URL_APPLICATION_DEPLOY, app);
        checkStatusForSuccess(response);

        return getEntityValues(get(URL_APPLICATION_DEPLOY + "/" + app.get("name")));
    }

    public void addAppRef(final String applicationName, final String targetName){
        Response cr = post("/domain/servers/server/" + targetName + "/application-ref", new HashMap<String,String>() {{
            put("id", applicationName);
            put("target", targetName);
        }});
        checkStatusForSuccess(cr);
    }

    public Response undeployApp(String appName) {
        Response response = delete(URL_APPLICATION_DEPLOY + "/" + appName);
        checkStatusForSuccess(response);

        return response;
    }
}
