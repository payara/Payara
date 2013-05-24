/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.weld;

import org.easymock.EasyMockSupport;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.weld.connector.WeldUtils;
import org.junit.Test;

import static junit.framework.Assert.*;

/**
 * @author <a href="mailto:j.j.snyder@oracle.com">JJ Snyder</a>
 */
public class AppBeanDeploymentArchiveTest {
    @Test
    public void testConstructor() throws Exception {
        String appId = "ai";
        EasyMockSupport mockSupport = new EasyMockSupport();

        DeploymentContext deploymentContext = mockSupport.createNiceMock(DeploymentContext.class);
        mockSupport.replayAll();
        AppBeanDeploymentArchive appBda = new AppBeanDeploymentArchive(appId, deploymentContext);

        assertNull(appBda.getBeansXml());
        assertEquals( WeldUtils.BDAType.UNKNOWN, appBda.getBDAType());
        assertEquals(appId, appBda.getId());

        assertEquals(0, appBda.getBeanClasses().size());
        assertEquals(0, appBda.getBeanClassObjects().size());
        assertEquals(0, appBda.getModuleBeanClasses().size());
        assertEquals(0, appBda.getModuleBeanClassObjects().size());
        assertEquals(0, appBda.getEjbs().size());

        assertEquals(0, appBda.getBeanDeploymentArchives().size());


        mockSupport.verifyAll();
        mockSupport.resetAll();
    }
}
