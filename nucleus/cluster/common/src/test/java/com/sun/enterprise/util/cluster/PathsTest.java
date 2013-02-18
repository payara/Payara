/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2011-2013 Oracle and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 *  or packager/legal/LICENSE.txt.  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at packager/legal/LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  Oracle designates this particular file as subject to the "Classpath"
 *  exception as provided by Oracle in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */
package com.sun.enterprise.util.cluster;

import com.sun.enterprise.config.serverbeans.ApplicationRef;
import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.config.serverbeans.ResourceRef;
import com.sun.enterprise.config.serverbeans.SshConnector;
import java.beans.PropertyVetoException;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.TransactionFailure;

/**
 *
 * @author wnevins
 */
public class PathsTest {
    private static String SPECIFIED_INSTALL_DIR = "D:/glassfish4";
    private static String NODE_NAME = "thenode";
    private static String SPECIFIED_NODES_DIR = SPECIFIED_INSTALL_DIR + "/glassfish/mynodes";
    private static String SPECIFIED_NODE_DIR = SPECIFIED_NODES_DIR + "/" + NODE_NAME;
    private static String INSTANCE_NAME = "instance1";



    /**
     * Test of getNodeDir method, of class Paths.
     */
    @Test
    public void testGetNodeDir() {

        String nodedir1 = Paths.getNodeDir(new NodeWithNodeDir());
        String nodedir2 = Paths.getNodeDir(new NodeWithoutNodeDir());
        System.out.println("User-specified Node Dir: " + nodedir1);
        System.out.println("Default Node Dir: " + nodedir2);
        assertEquals(nodedir2, SPECIFIED_INSTALL_DIR + "/glassfish/nodes/" + NODE_NAME);
        assertEquals(nodedir1, SPECIFIED_NODE_DIR);
    }

    /**
     * Test of getDasPropsPath method, of class Paths.
     */
    @Test
    public void testGetDasPropsPath() {
        String d1 = Paths.getDasPropsPath(new NodeWithNodeDir());
        String d2 = Paths.getDasPropsPath(new NodeWithoutNodeDir());
        String d1expect = SPECIFIED_NODE_DIR + "/agent/config/das.properties";
        String d2expect = SPECIFIED_INSTALL_DIR + "/glassfish/nodes/" + NODE_NAME + "/agent/config/das.properties";

        System.out.println("User-specified das props: " + d1);
        System.out.println("Default das props: " + d2);
        assertEquals(d1expect, d1);
        assertEquals(d2expect, d2);
    }

    @Test
    public void testGetInstanceDirPath() {
        String d1 = Paths.getInstanceDirPath(new NodeWithNodeDir(), INSTANCE_NAME);
        String d2 = Paths.getInstanceDirPath(new NodeWithoutNodeDir(), INSTANCE_NAME);
        System.out.println("User-specified Instance Dir: " + d1);
        System.out.println("Default Node Dir Instance Dir: " + d2);
        assertEquals(d2, SPECIFIED_INSTALL_DIR + "/glassfish/nodes/" + NODE_NAME + "/" + INSTANCE_NAME);
        assertEquals(d1, SPECIFIED_NODE_DIR + "/" + INSTANCE_NAME);
    }


    static abstract class NodeAdapter implements Node {
        // bless you for spitting this out NetBeans!!!
        @Override
        public void setName(String value) throws PropertyVetoException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getName() {
            return NODE_NAME;
        }

        @Override
        public String getNodeDir() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setNodeDir(String value) throws PropertyVetoException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getNodeHost() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setNodeHost(String value) throws PropertyVetoException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getInstallDir() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setInstallDir(String value) throws PropertyVetoException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getType() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setType(String value) throws PropertyVetoException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getWindowsDomain() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setWindowsDomain(String value) throws PropertyVetoException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getFreeze() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setFreeze(String value) throws PropertyVetoException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public SshConnector getSshConnector() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setSshConnector(SshConnector connector) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getNodeDirUnixStyle() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getNodeDirAbsolute() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean nodeInUse() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isDefaultLocalNode() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isLocal() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean instanceCreationAllowed() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ConfigBeanProxy getParent() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public <T extends ConfigBeanProxy> T getParent(Class<T> type) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public <T extends ConfigBeanProxy> T createChild(Class<T> type) throws TransactionFailure {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ConfigBeanProxy deepCopy(ConfigBeanProxy cbp) throws TransactionFailure {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getReference() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isCluster() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isServer() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isInstance() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isDas() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public List<ResourceRef> getResourceRef() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public List<ApplicationRef> getApplicationRef() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    static class NodeWithNodeDir extends NodeAdapter {
        @Override
        public String getInstallDirUnixStyle() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getNodeDirAbsoluteUnixStyle() {
            return PathsTest.SPECIFIED_NODES_DIR;
        }
    }

    static class NodeWithoutNodeDir extends NodeAdapter {
        @Override
        public String getInstallDirUnixStyle() {
             return SPECIFIED_INSTALL_DIR;
        }

        @Override
        public String getNodeDirAbsoluteUnixStyle() {
            return null;
        }
    }
}
