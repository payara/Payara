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

package org.glassfish.tests.embedded.cooked;

import org.junit.Test;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.embedded.*;
import org.glassfish.api.container.Sniffer;
import org.glassfish.api.admin.*;
import org.glassfish.api.deployment.DeployCommandParameters;

import java.io.*;
import java.util.Enumeration;

/**
 * @author Jerome Dochez
 */
public class InplantedTest {

    static Server server;

    @BeforeClass
    public static void setupServer() throws Exception {
        System.out.println("setup started with gf installation " + System.getProperty("basedir"));
        File f = new File(System.getProperty("basedir"));
        f = new File(f, "target");
        f = new File(f, "dependency");
        f = new File(f, "glassfish4");
        f = new File(f, "glassfish");
        if (f.exists()) {
            System.out.println("Using gf at " + f.getAbsolutePath());
        } else {
            System.out.println("GlassFish not found at " + f.getAbsolutePath());
            Assert.assertTrue(f.exists());
        }
        try {
            EmbeddedFileSystem.Builder efsb = new EmbeddedFileSystem.Builder();
            efsb.installRoot(f, false);
            Server.Builder builder = new Server.Builder("inplanted");
            builder.embeddedFileSystem(efsb.build());
            server = builder.build();
        } catch(Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    public void testWeb() throws Exception {
        System.out.println("test web");
        File f = new File(System.getProperty("basedir"));
        f = new File(f, "target");
        f = new File(f, "test-classes");
        ScatteredArchive.Builder builder = new ScatteredArchive.Builder("hello", f);
        builder.addClassPath(f.toURI().toURL());
        builder.resources(f);
        ScatteredArchive war = builder.buildWar();
        System.out.println("War content");
        Enumeration<String> contents = war.entries();
        while(contents.hasMoreElements()) {
            System.out.println(contents.nextElement());
        }
        try {
            System.out.println("Port created " + server.createPort(14587));
            server.addContainer(ContainerBuilder.Type.web);
            server.start();
            DeployCommandParameters dp = new DeployCommandParameters(f);
            String appName = server.getDeployer().deploy(war, dp);
            System.out.println("Application deployed under name = " + appName);
            if (appName!=null) {
                server.getDeployer().undeploy(appName, null);
            }
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (LifecycleException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } 
    }

    @Test
    public void Test() {

        ServiceLocator habitat = server.getHabitat();
        System.out.println("Process type is " + habitat.<ProcessEnvironment>getService(ProcessEnvironment.class).getProcessType());
        for (Sniffer s : habitat.<Sniffer>getAllServices(Sniffer.class)) {
            System.out.println("Got sniffer " + s.getModuleType());
        }
    }

    @AfterClass
    public static void shutdownServer() throws Exception {
        System.out.println("shutdown initiated");
        if (server!=null) {
            try {
                server.stop();
            } catch (LifecycleException e) {
                e.printStackTrace();
                throw e;
            }
        }
        
        
    }
}
