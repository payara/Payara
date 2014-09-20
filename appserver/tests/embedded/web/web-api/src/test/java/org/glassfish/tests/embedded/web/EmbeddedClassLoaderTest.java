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
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.logging.Level;

import org.glassfish.embeddable.*;
import org.glassfish.embeddable.web.*;
import org.glassfish.embeddable.web.config.*;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class EmbeddedClassLoaderTest {

    static GlassFish glassfish;
    static String contextRoot = "test";
    static WebContainer wc;
    static File root;

    @BeforeClass
    public static void setupServer() throws GlassFishException {

        glassfish = GlassFishRuntime.bootstrap().newGlassFish();
        glassfish.start();
        root = new File("target/classes");

        wc = glassfish.getService(WebContainer.class);
        wc.setLogLevel(Level.INFO);
        WebContainerConfig config = new WebContainerConfig();
        root = new File("target/classes");
        config.setDocRootDir(root);
        config.setListings(true);
        config.setPort(8080);
        System.out.println("Added Web with base directory "+root.getAbsolutePath());
        wc.setConfiguration(config);
    }

    private static void loadA(ClassLoader cl) {
        String className = "TestCacaoList";
        try {
            System.out.println("---> Loading " + className + " with " + cl);
            cl.loadClass(className);
            System.out.println("---> Finish to load " + className + " with " + cl);
        } catch(Exception ex) {
            System.out.println("---> Cannot load " + className + " with " + cl + ": " + ex);
            throw new IllegalStateException();
        }
    }
    
    @Test
    public void test() throws Exception {
        URL[] urls = new URL[1];
        urls[0] = (new File("src/main/resources/toto.jar")).toURI().toURL();
        URLClassLoader classLoader = new URLClassLoader(urls, EmbeddedClassLoaderTest.class.getClassLoader());
        loadA(classLoader);

        Thread.currentThread().setContextClassLoader(classLoader);

        File path = new File("src/main/resources/embedded-webapi-tests.war");

        Context context = wc.createContext(path, classLoader);
        wc.addContext(context, contextRoot);

        URL servlet = new URL("http://localhost:8080/test/testgf");
        URLConnection yc = servlet.openConnection();
        BufferedReader in = new BufferedReader(
                                new InputStreamReader(
                                yc.getInputStream()));

        StringBuilder sb = new StringBuilder();
        String inputLine;
        while ((inputLine = in.readLine()) != null){
            sb.append(inputLine);
        }

        boolean success = sb.toString().contains("Class TestCacaoList loaded successfully from listener");
        if (success) {
            success = sb.toString().contains("Class TestCacaoList loaded successfully from servlet");
        }
        Assert.assertTrue(success);
        in.close();

        wc.removeContext(context);
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
