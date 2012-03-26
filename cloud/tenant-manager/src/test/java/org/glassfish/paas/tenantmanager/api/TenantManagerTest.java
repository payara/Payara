/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.paas.tenantmanager.api;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.glassfish.paas.tenantmanager.api.TenantManager;
import org.glassfish.paas.tenantmanager.config.Tenant;
import org.glassfish.paas.tenantmanager.config.TenantManagerConfig;
import org.glassfish.paas.tenantmanager.impl.TenantDocument;
import org.glassfish.tests.utils.Utils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.config.ConfigBean;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Dom;

import com.sun.enterprise.util.io.FileUtils;

public class TenantManagerTest {
    TenantManager tenantManager;

    @Before
    public void before() {
        Habitat habitat = Utils.getNewHabitat();
        TenantManagerConfig tenantManagerConfig = habitat.getComponent(TenantManagerConfig.class);
        tenantManagerConfig.setFileStore(TenantConfigServiceTest.class.getResource("/"));
        tenantManager = habitat.getComponent(TenantManager.class);
    }

    private String readConfigXml(URL config) throws IOException, URISyntaxException {
        File file = new File(config.toURI());
        return FileUtils.readSmallFile(file);
        
    }

    // TODO: find in glassfish and re-use it
    private void assertConfigXml(String msg, String expectedFileName, ConfigBeanProxy actualConfig) throws MalformedURLException, IOException, URISyntaxException {
        ConfigBean configBean = (ConfigBean) Dom.unwrap(actualConfig);
        TenantDocument doc = (TenantDocument) configBean.document;
        String expectedUrl = TenantConfigServiceTest.class.getResource("/").toString() + expectedFileName + "-expected.xml";
        String expected = readConfigXml(new URL(expectedUrl));
        String actual = readConfigXml(doc.getResource());
        Assert.assertEquals(msg, expected, actual);

    }
    

    @Test
    public void testCreate() throws URISyntaxException, MalformedURLException, IOException {
        Assert.assertNotNull("tenantManager", tenantManager);
        Tenant tenant = tenantManager.create("tenant3", "admin");
        Assert.assertNotNull("tenant", tenant);
        Assert.assertEquals("tenant", "tenant3", tenant.getName());
        assertConfigXml("New tenant xml", "tenant3", tenant);
    }

}
