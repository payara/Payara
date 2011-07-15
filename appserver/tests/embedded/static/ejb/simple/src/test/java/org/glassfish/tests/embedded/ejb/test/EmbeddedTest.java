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

package org.glassfish.tests.embedded.ejb.test;

import org.junit.Test;
import org.junit.Assert;
import org.glassfish.tests.embedded.ejb.SampleEjb;
import org.glassfish.internal.embedded.*;

import javax.ejb.embeddable.EJBContainer;
import javax.naming.*;
import java.util.Map;
import java.util.HashMap;
import java.io.File;

/**
 * this test will use the ejb API testing.
 *
 * @author Jerome Dochez
 */
public class EmbeddedTest {

    @Test
    public void test() throws Exception {
        Server.Builder builder = new Server.Builder("simple");
        Server server = builder.build();
        File f = new File(System.getProperty("basedir"), "target");
        f = new File(f, "classes");

        ScatteredArchive archive = new ScatteredArchive.Builder("simple",f).buildJar();
        server.addContainer(ContainerBuilder.Type.ejb);
        try {
            server.start();
            String appName = null;
            try {
                appName = server.getDeployer().deploy(archive, null);
            } catch (Exception e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            assert(appName!=null);
            try {
                System.out.println("Looking up EJB...");
                SampleEjb ejb = (SampleEjb) (new InitialContext()).lookup("java:global/simple/SampleEjb");
                if (ejb!=null) {
                    System.out.println("Invoking EJB...");
                    System.out.println(ejb.saySomething());
                    Assert.assertEquals(ejb.saySomething(), "Hello World");
                }
            } catch (Exception e) {
                System.out.println("ERROR calling EJB:");
                e.printStackTrace();
                throw new RuntimeException(e);
            }

            server.getDeployer().undeploy(appName, null);
        } finally {
            server.stop();
        }
    }
}
