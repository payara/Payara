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

package org.glassfish.tests.embedded.inplanted;

import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.embedded.*;
import org.glassfish.tests.embedded.utils.EmbeddedServerUtils;
import org.junit.BeforeClass;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.Collection;
import java.lang.reflect.Method;

/**
 * @author Jerome Dochez
 */
public class ExistingConfigurationTest {

    @Test
    public void setupServer() throws Exception {

        Server server=null;
        Port port = null;

        File f = EmbeddedServerUtils.getServerLocation();
        try {
            EmbeddedFileSystem.Builder efsb = new EmbeddedFileSystem.Builder();
            efsb.installRoot(f);
            // find the domain root.
            f = EmbeddedServerUtils.getDomainLocation(f);
            f = new File(f, "config");
            f = new File(f, "domain.xml");
            Assert.assertTrue(f.exists());
            efsb.configurationFile(f, true);
            server = EmbeddedServerUtils.createServer(efsb.build());

            ServiceLocator habitat = server.getHabitat();
            Collection<ServiceHandle<?>> vss = habitat.getAllServiceHandles(com.sun.enterprise.config.serverbeans.VirtualServer.class);
            Assert.assertTrue(vss.size()>0);
            for (ServiceHandle<?> vs : vss ) {
                Object virtualServer = vs.getService();
                Method m = virtualServer.getClass().getMethod("getId");
                Assert.assertNotNull("Object returned does not implement getId, is it a virtual server ?", m);
                String id = (String) m.invoke(virtualServer);
                System.out.println("Virtual Server " + id);
                Assert.assertNotNull("Got a null virtual server ID", id);
            }
            Collection<ServiceHandle<?>> nls = habitat.getAllServiceHandles(org.glassfish.grizzly.config.dom.NetworkListener.class);
            Assert.assertTrue(nls.size()>1);
            for (ServiceHandle<?> nl : nls) {
                Object networkListener = nl.getService();
                Method m = networkListener.getClass().getMethod("getPort");
                Assert.assertNotNull("Object returned does not implement getPort, is it a networkListener ?", m);
                String p = (String) m.invoke(networkListener);
                System.out.println("Network Listener " + p);
                Assert.assertNotNull("Got a null networkListener port", p);
            }
            server.start();
            port = server.createPort(8758);
        } catch(Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (port!=null) {
                port.close();
            }
            EmbeddedServerUtils.shutdownServer(server);
        }
    }
}
