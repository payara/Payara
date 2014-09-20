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
package org.glassfish.nucleus.quicklook;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import static org.testng.AssertJUnit.*;
import org.testng.annotations.Test;

@Test
public class RestTest {

    public void testManagementEndpoint() {
        try {
            HttpURLConnection connection = getConnection("http://localhost:4848/management/domain.xml");
            assertEquals(200, connection.getResponseCode());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    public void testMonitoringEndpoint() {
        try {
            HttpURLConnection connection = getConnection("http://localhost:4848/monitoring/domain.xml");
            assertEquals(200, connection.getResponseCode());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    public void testAdminCommandEndpoint() {
        try {
            HttpURLConnection connection = getConnection("http://localhost:4848/management/domain/version.xml");
            assertEquals(200, connection.getResponseCode());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    public void testChildConfigBeanEndpoint() {
        try {
            HttpURLConnection connection = getConnection("http://localhost:4848/management/domain/applications.xml");
            assertEquals(200, connection.getResponseCode());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    public void testPostGetDelete() {
        deleteNode(); // This should almost always fail, so we don't check the status. Just need to clean up from any prior runs
        assertEquals(200, createNode());
        assertEquals(200, getNode());
        assertEquals(200, deleteNode());
    }

    protected HttpURLConnection getConnection(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestProperty("X-GlassFish-3", "true");
        connection.setRequestProperty("X-Requested-By", "dummy");
        return connection;
    }

    private int createNode() {
        HttpURLConnection connection = null;
        try {
            String parameters = "name=myConfigNode";
            connection = getConnection("http://localhost:4848/management/domain/nodes/create-node-config");
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length", "" + Integer.toString(parameters.getBytes().length));
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Language", "en-US");
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(parameters);
            wr.flush();
            wr.close();
            return connection.getResponseCode();
        } catch (Exception ex) {
            fail(ex.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return -1;
    }

    private int getNode() {
        HttpURLConnection connection = null;
        try {
            connection = getConnection("http://localhost:4848/management/domain/nodes/node/myConfigNode");
            return connection.getResponseCode();
        } catch (Exception ex) {
            fail(ex.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return -1;
    }

    private int deleteNode() {
        HttpURLConnection connection = null;
        try {
            connection = getConnection("http://localhost:4848/management/domain/nodes/delete-node-config?name=myConfigNode");
            connection.setRequestMethod("DELETE");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setDoOutput(true);
            return connection.getResponseCode();
        } catch (Exception ex) {
            fail(ex.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return -1;
    }
}
