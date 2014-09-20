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

package org.glassfish.tests.embedded.cdi_ejb_jpa;

import junit.framework.Assert;
import org.glassfish.embeddable.Deployer;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishProperties;
import org.glassfish.embeddable.BootstrapProperties;
import org.glassfish.embeddable.GlassFishRuntime;
import org.glassfish.embeddable.archive.ScatteredArchive;
import org.glassfish.embeddable.archive.ScatteredEnterpriseArchive;
import org.glassfish.embeddable.web.HttpListener;
import org.glassfish.embeddable.web.WebContainer;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author bhavanishankar@dev.java.net
 */

public class BasicCDITest{

    @Test
    public void test() throws Exception {

        GlassFishProperties props = new GlassFishProperties();
        BootstrapProperties bootstrapProperties = new BootstrapProperties();
        props.setPort("http-listener", 8080);
        GlassFish glassfish = GlassFishRuntime.bootstrap(bootstrapProperties).newGlassFish(props);
        glassfish.start();

        // Test Scattered Web Archive
        ScatteredArchive sa = new ScatteredArchive("cdi_ejb_jpa",
                ScatteredArchive.Type.WAR, new File("src/main/webapp"));
        sa.addClassPath(new File("target/classes"));
        sa.addClassPath(new File("src/main/resources"));
        URI warURI = sa.toURI();
        printContents(warURI);

        // Deploy archive
        Deployer deployer = glassfish.getDeployer();
        String appname = deployer.deploy(warURI);
        System.out.println("Deployed [" + appname + "]");
        Assert.assertEquals(appname, "cdi_ejb_jpa");

        // Now create a http listener and access the app.
        WebContainer webcontainer = glassfish.getService(WebContainer.class);
        HttpListener listener = new HttpListener();
        listener.setId("my-listener");
        listener.setPort(9090);
        webcontainer.addWebListener(listener);

        get("http://localhost:8080/cdi_ejb_jpa/BasicCDITestServlet",
                "All CDI beans have been injected.");

        deployer.undeploy(appname);

        glassfish.dispose();

    }

    private void get(String urlStr, String result) throws Exception {
        URL url = new URL(urlStr);
        URLConnection yc = url.openConnection();
        System.out.println("\nURLConnection [" + yc + "] : ");
        BufferedReader in = new BufferedReader(new InputStreamReader(
                yc.getInputStream()));
        String line = null;
        boolean found = false;
        while ((line = in.readLine()) != null) {
            System.out.println(line);
            if (line.indexOf(result) != -1) {
                found = true;
            }
        }
        Assert.assertTrue(found);
        System.out.println("\n***** SUCCESS **** Found [" + result + "] in the response.*****\n");
    }

    void printContents(URI jarURI) throws IOException {
        JarFile jarfile = new JarFile(new File(jarURI));
        System.out.println("\n\n[" + jarURI + "] contents : \n");
        Enumeration<JarEntry> entries = jarfile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            System.out.println(entry.getSize() + "\t" + new Date(entry.getTime()) +
                    "\t" + entry.getName());
        }
        System.out.println();
    }
}
