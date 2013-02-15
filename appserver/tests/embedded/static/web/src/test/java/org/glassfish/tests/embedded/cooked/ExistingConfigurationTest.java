/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2011 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.embedded.Server;
import org.glassfish.internal.embedded.EmbeddedFileSystem;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.Collection;

import com.sun.enterprise.config.serverbeans.VirtualServer;
import org.glassfish.grizzly.config.dom.NetworkListener;

/**
 * @author Jerome Dochez
 */
public class ExistingConfigurationTest {

    @Test
    public void setupServer() throws Exception {

        Server server=null;

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
            // find the domain root.
            f = new File(f,"domains");
            f = new File(f, "domain1");
            f = new File(f, "config");
            f = new File(f, "domain.xml");
            Assert.assertTrue(f.exists());
            efsb.configurationFile(f);

            Server.Builder builder = new Server.Builder("inplanted");
            builder.embeddedFileSystem(efsb.build());
            server = builder.build();

            ServiceLocator habitat = server.getHabitat();
            Collection<VirtualServer> vss = habitat.getAllServices(VirtualServer.class);
            Assert.assertTrue(vss.size()>0);
            for (VirtualServer vs : vss ) {
                System.out.println("Virtual Server " + vs.getId());
            }
            Collection<NetworkListener> nls = habitat.getAllServices(NetworkListener.class);
            for (NetworkListener nl : nls) {
                System.out.println("Network listener " + nl.getPort());
            }
        } catch(Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (server!=null) {
                server.stop();
            }
        }
    }
}
