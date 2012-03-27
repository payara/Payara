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

import org.glassfish.paas.tenantmanager.api.TenantConfigService;
import org.glassfish.paas.tenantmanager.config.Tenant;
import org.glassfish.paas.tenantmanager.config.TenantManagerConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

public class TenantConfigServiceTest extends ConfigApiTest {
    TenantConfigService tenantConfigService;

    @Before
    public void before() {
        Habitat habitat = getHabitat();
        tenantConfigService = habitat.getComponent(TenantConfigService.class);
        TenantManagerConfig tenantManagerConfig = tenantConfigService.getTenantManagerConfig();
        // change file store to point to test files
        try {
            ConfigSupport.apply(new SingleConfigCode<TenantManagerConfig>() {
                @Override
                public Object run(TenantManagerConfig tmc) throws TransactionFailure {
                    String fileStore = TenantConfigServiceTest.class.getResource("/").getPath();
                    tmc.setFileStore(fileStore);
                    return tmc;
                }
            }, tenantManagerConfig);
        } catch (TransactionFailure e) {
            // TODO Auto-generated catch block
            e.printStackTrace();            
        }
    }   

    @Test
    public void testGet() {
        Assert.assertNotNull("tenantConfigService", tenantConfigService);
        //Assert.assertNull("currentTenant not initialized", tenantConfigService.getCurrentTenant());
        tenantConfigService.setCurrentTenant("tenant1");
        Assert.assertEquals("currentTenant", "tenant1", tenantConfigService.getCurrentTenant());
        Tenant tenant = tenantConfigService.get(Tenant.class);
        Assert.assertNotNull("currentTenant", tenant);
        Assert.assertEquals("currentTenant", "tenant1", tenant.getName());
        tenantConfigService.setCurrentTenant("tenant2");
        tenant = tenantConfigService.get(Tenant.class);
        Assert.assertNotNull("currentTenant", tenant);
        Assert.assertEquals("currentTenant", "tenant2", tenant.getName());
    }

}
