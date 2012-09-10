/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.distributions.test;

import org.glassfish.distributions.test.ejb.SampleEjb;
import org.glassfish.embeddable.CommandResult;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.embeddable.Deployer;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.embeddable.GlassFishRuntime;
import org.glassfish.embeddable.web.WebContainer;
import org.glassfish.embeddable.web.HttpListener;
import org.glassfish.embeddable.web.WebListener;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;

public class EmbeddedTest {

    static GlassFish glassfish;

    @BeforeClass
    public static void setup() throws GlassFishException {
        glassfish = GlassFishRuntime.bootstrap().newGlassFish();
        glassfish.start();

        WebContainer webcontainer =
                glassfish.getService(WebContainer.class);
        Collection<WebListener> listeners = webcontainer.getWebListeners();
        System.out.println("Network listener size before creation " + listeners.size());
        for (WebListener listener : listeners) {
            System.out.println("Network listener " + listener.getPort());
        }

        try {
            HttpListener listener = new HttpListener();
            listener.setPort(8080);
            listener.setId("embedded-listener-1");
            webcontainer.addWebListener(listener);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        listeners = webcontainer.getWebListeners();
        System.out.println("Network listener size after creation " + listeners.size());
        Assert.assertTrue(listeners.size() == 1);
        for (WebListener listener : listeners) {
            System.out.println("Network listener " + listener.getPort());
        }

    }

    @Test
    public void testAll() throws GlassFishException {
        /*
        Set<Sniffer> sniffers = new HashSet<Sniffer>();
        for (EmbeddedContainer c : server.getContainers()) {
            sniffers.addAll(c.getSniffers());
        }
        System.out.println("Sniffer size "  + sniffers.size());
        for (Sniffer sniffer : sniffers) {
            System.out.println("Registered Sniffer " + sniffer.getModuleType());
        }
        */
    }

    @Test
    public void testEjb() throws GlassFishException {
        Deployer deployer = glassfish.getDeployer();
        
        URL source = SampleEjb.class.getClassLoader().getResource(
                "org/glassfish/distributions/test/ejb/SampleEjb.class");
        String p = source.getPath().substring(0, source.getPath().length() -
                "org/glassfish/distributions/test/ejb/SimpleEjb.class".length());

        String appName = deployer.deploy(new File(p).toURI(), "--name=sample");
        Assert.assertNotNull("AppName is null from deployer of type " + deployer.getClass().getName(),
                appName);
        
        // ok now let's look up the EJB...
        try {
            InitialContext ic = new InitialContext();
            SampleEjb ejb = (SampleEjb) ic.lookup("java:global/sample/SampleEjb");
            if (ejb != null) {
                try {
                    System.out.println(ejb.saySomething());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (NamingException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        deployer.undeploy(appName);
        System.out.println("Done with EJB");
    }

    @Test
    public void testWeb() throws GlassFishException {
        System.out.println("Starting testWeb " + glassfish);

        Deployer deployer = glassfish.getDeployer();

        URL source = SampleEjb.class.getClassLoader().getResource(
                "org/glassfish/distributions/test/web/WebHello.class");
        String p = source.getPath().substring(0, source.getPath().length() -
                "org/glassfish/distributions/test/web/WebHello.class".length());

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

        try {
            URL servlet = new URL("http://localhost:8080/test-classes/hello");
            URLConnection yc = servlet.openConnection();
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                            yc.getInputStream()));
            String inputLine = in.readLine();
            if (inputLine != null) {
                System.out.println(inputLine);
            }
            Assert.assertNotNull(inputLine);
            Assert.assertEquals(inputLine.trim(), "Hello World !");
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            // do not throw the exception for now, because this may break the build if,
            // for example, another instance of glassfish is running on 8080
            //   throw e;
        }

        if (appName != null) {
            deployer.undeploy(appName);
            System.out.println("Undeployed " + appName);
        }

    }

    @Test
    public void commandTest() throws GlassFishException {
        CommandRunner commandRunner = glassfish.getCommandRunner();

        CommandResult commandResult = commandRunner.run("list-modules");
        System.out.println("list-modules command result :\n" + commandResult.getOutput());

        // Unknown commands throw NPE, uncomment once the issue is fixed.
        //commandResult = commandRunner.run("list-contracts");
        //System.out.println("list-contracts command result :\n" + commandResult.getOutput());
    }

    @AfterClass
    public static void close() throws GlassFishException {
        System.out.println("Stopping server " + glassfish);
        if (glassfish != null) {
            glassfish.stop();
            glassfish.dispose();
            glassfish = null;
        }
    }
}
