/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tests.embedded.web;

import java.io.*;
import java.net.*;
import java.security.*;
import java.security.cert.X509Certificate;
import javax.net.ssl.*;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.glassfish.embeddable.*;
import org.glassfish.embeddable.web.*;
import org.glassfish.embeddable.web.config.*;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests WebContainer#addWebListener(HttpsListener)
 * 
 * @author Amy Roh
 */
public class EmbeddedAddHttpsListenerTest {

    static GlassFish glassfish;
    static WebContainer embedded;
    static File root;                
    static String contextRoot = "test";

    @BeforeClass
    public static void setupServer() throws GlassFishException {
        glassfish = GlassFishRuntime.bootstrap().newGlassFish();
        glassfish.start();
        embedded = glassfish.getService(WebContainer.class);
        System.out.println("================ EmbeddedAddHttpsListener Test");
        System.out.println("Starting Web "+embedded);
        embedded.setLogLevel(Level.INFO);
        WebContainerConfig config = new WebContainerConfig();
        root = new File("target/classes");
        config.setDocRootDir(root);
        config.setListings(true);
        config.setPort(8080);
        System.out.println("Added Web with base directory "+root.getAbsolutePath());
        embedded.setConfiguration(config);
    }

    private void createHttpsListener(int port,
                                     String name,
                                     String keystore,
                                     String password,
                                     String certname) throws Exception {

        HttpsListener listener = new HttpsListener();
        listener.setPort(port);
        listener.setId(name);

        String keyStorePath = root.getAbsolutePath() + keystore;
        String trustStorePath = root.getAbsolutePath() + "/cacerts.jks";
        SslConfig sslConfig = new SslConfig(keyStorePath, trustStorePath);
        sslConfig.setKeyPassword(password.toCharArray());
        String trustPassword = "changeit";
        sslConfig.setTrustPassword(trustPassword.toCharArray());
        if (certname != null) {
            sslConfig.setCertNickname(certname);
        }
        listener.setSslConfig(sslConfig);

        embedded.addWebListener(listener);
    }

    private void verify(int port) throws Exception {

        URL servlet = new URL("https://localhost:"+port+"/classes/hello");
        HttpsURLConnection uc = (HttpsURLConnection) servlet.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(uc.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String inputLine;
        while ((inputLine = in.readLine()) != null){
            sb.append(inputLine);
        }
        in.close();
        System.out.println(sb);
        Assert.assertEquals("Hello World!", sb.toString());
    }

    @Test
    public void test() throws Exception {

        createHttpsListener(9191, "default-ssl-listener", "/keystore.jks", "changeit", "s1as");
        //createHttpsListener(9292, "ssl-listener0", "/keystore0", "password0", "keystore0");
        //createHttpsListener(9393, "ssl-listener1", "/keystore1", "password1", null);

        Deployer deployer = glassfish.getDeployer();

        URL source = WebHello.class.getClassLoader().getResource(
                "org/glassfish/tests/embedded/web/WebHello.class");
        String p = source.getPath().substring(0, source.getPath().length() -
                "org/glassfish/tests/embedded/web/WebHello.class".length());
        File path = new File(p).getParentFile().getParentFile();

        String name = null;
        if (path.getName().lastIndexOf('.') != -1) {
            name = path.getName().substring(0, path.getName().lastIndexOf('.'));
        } else {
            name = path.getName();
        }

        System.out.println("Deploying " + path + ", name = " + name);
        String appName = deployer.deploy(path.toURI(), "--name=" + name);
        System.out.println("Deployed " + appName);
        Assert.assertTrue(appName != null);

        disableCertValidation();
        verify(9191);
        //verify(9292);
        //verify(9393);
        
        if (appName!=null)
            deployer.undeploy(appName);
        
    }

    public static void disableCertValidation() {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
                return;
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
                return;
            }
        }};

        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            return;
        }
    }

    @AfterClass
    public static void shutdownServer() throws GlassFishException {
        System.out.println("Stopping server " + glassfish);
        if (glassfish != null) {
            glassfish.stop();
            glassfish.dispose();
            glassfish = null;
        }
    }
    
}
