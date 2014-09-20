/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.configapi.tests;

import com.sun.enterprise.config.serverbeans.Server;
import org.junit.Test;
import org.junit.Before;
import org.jvnet.hk2.config.*;
import org.glassfish.tests.utils.Utils;
import com.sun.enterprise.config.serverbeans.HttpService;
import com.sun.enterprise.config.serverbeans.VirtualServer;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.util.List;
import java.util.logging.Level;

/**
 * User: Jerome Dochez
 * Date: Jun 25, 2008
 * Time: 8:03:41 AM
 */
public class AttributeRemovalTest extends ConfigApiTest {

    public String getFileName() {
        return "DomainTest";
    }

    @Test
    public void removeAttributeTest() throws TransactionFailure {
        HttpService httpService = Utils.instance.getHabitat(this).getService(HttpService.class);
        VirtualServer vs = httpService.getVirtualServerByName("server");
        ConfigSupport.apply(new SingleConfigCode<VirtualServer>() {

            public Object run(VirtualServer param) throws PropertyVetoException, TransactionFailure {
                param.setDefaultWebModule("/context/bar");
                return null;
            }
        }, vs);

        // ensure it's here
        org.junit.Assert.assertNotNull(vs.getDefaultWebModule());

        ConfigSupport.apply(new SingleConfigCode<VirtualServer>() {

            public Object run(VirtualServer param) throws PropertyVetoException, TransactionFailure {
                param.setDefaultWebModule(null);
                return null;
            }
        }, vs);

        // ensure it's removed
        org.junit.Assert.assertNull(vs.getDefaultWebModule());
    }
    
    @Test(expected=PropertyVetoException.class)
    public void readOnlyRemovalTest() throws TransactionFailure , PropertyVetoException{
        Server server = getHabitat().getService(Server.class);
        logger.fine("config-ref is " + server.getConfigRef());
        try {
            server.setConfigRef(null);
        } catch (PropertyVetoException e) {
            if (logger.isLoggable(Level.FINE))
                e.printStackTrace();
            throw e;
        }
    }

    @Test
    public void deprecatedWrite() throws TransactionFailure {
        final Server server = getHabitat().getService(Server.class);
        final String value = server.getNodeRef();
        logger.fine("node-ref is " + server.getNodeRef());
        ConfigSupport.apply(new SingleConfigCode<Server>() {
            @Override
            public Object run(Server s) throws PropertyVetoException, TransactionFailure {
                s.setNodeAgentRef(value);
                return null;
            }
        }, server);
        logger.fine("node-agent-ref is " + server.getNodeAgentRef());
        // restore
        ConfigSupport.apply(new SingleConfigCode<Server>() {
            @Override
            public Object run(Server s) throws PropertyVetoException, TransactionFailure {
                s.setNodeAgentRef(null);
                return null;
            }
        }, server);
        logger.fine("after, node-agent-ref is " + server.getNodeAgentRef());

    }
    

}
