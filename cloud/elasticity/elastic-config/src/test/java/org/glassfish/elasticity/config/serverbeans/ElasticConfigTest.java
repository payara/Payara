/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.elasticity.config.serverbeans;

import org.glassfish.paas.tenantmanager.config.TenantManagerConfig;
import org.glassfish.paas.tenantmanager.entity.Tenant;
import org.glassfish.paas.tenantmanager.entity.TenantServices;
import org.glassfish.paas.tenantmanager.api.TenantManager;
import org.glassfish.paas.tenantmanager.impl.TenantManagerEx;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

public class ElasticConfigTest  extends ConfigApiTest {

    TenantManager tenantManager;

    @Before
    public void before() {
        Habitat habitat = getHabitat();
        tenantManager = habitat.getComponent(TenantManager.class);
        // change file store to point to test files
        TenantManagerConfig tenantManagerConfig = ((TenantManagerEx) tenantManager).getTenantManagerConfig();
        try {
            ConfigSupport.apply(new SingleConfigCode<TenantManagerConfig>() {
                @Override
                public Object run(TenantManagerConfig tmc) throws TransactionFailure {
                    String fileStore = ElasticConfigTest.class.getResource("/").getPath();
                    tmc.setFileStore(fileStore);
                    return tmc;
                }
            }, tenantManagerConfig);
        } catch (TransactionFailure e) {
            // TODO Auto-generated catch block
            e.printStackTrace();            
        }
    }

    // Verify tenant1 and tenant2 can be retrieved.
    @Test
    public void testGet() {
        Assert.assertNotNull("tenantManager", tenantManager);
        tenantManager.setCurrentTenant("tenant1");
        Tenant tenant = tenantManager.get(Tenant.class);
        Assert.assertNotNull("Current Tenant", tenant);
        TenantServices services = tenant.getServices();
        Assert.assertNotNull("Services", services);
        try {
            ConfigSupport.apply(new SingleConfigCode<TenantServices>() {
                @Override
                public Object run(TenantServices tenantServices) throws TransactionFailure {
                    
                    Elastic es = tenantServices.createChild(Elastic.class);

                    ElasticAlerts alerts=es.createChild((ElasticAlerts.class));
//                    alerts.setName(("alert1"));
//                    es.setElasticAlerts(alerts);
                    tenantServices.getTenantServices().add(es);
                    return tenantServices;
                }
            }, services);
        } catch (TransactionFailure e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Elastic elastic = services.getServiceByType(Elastic.class);
        Assert.assertNotNull("Elastic Service", elastic);
        ElasticAlerts alerts = elastic.getElasticAlerts();
//        Assert.assertNotNull("Elastic Alertrs", alerts);
//        Assert.assertEquals("Elastic Alert Schedule", "10s", alerts.getSchedule());
//        Assert.assertEquals("Elastic Alert Sample Interval", 5, alerts.getSampleInterval());
        
        
        
    }
    

    @Test
    public void testAlerts() {
        tenantManager.setCurrentTenant("tenant2");
        Tenant tenant = tenantManager.get(Tenant.class);
        TenantServices ts = tenant.getServices();
        Elastic elastic = (Elastic) ts.getServiceByType(Elastic.class);
        ElasticAlerts elasticAlerts = elastic.getElasticAlerts();

        try {
            //TODO There is nothing specific to elasticity to test here. This should move to CTM tests
            ConfigSupport.apply(new SingleConfigCode<ElasticAlerts>() {
                @Override
                public Object run(ElasticAlerts eAlerts) throws TransactionFailure {

                    ElasticAlert alert = eAlerts.createChild(ElasticAlert.class);
                    alert.setName(("alert1"));
                    alert.setSchedule("10s");
                    alert.setType("jvm_memory");
                    eAlerts.getElasticAlert().add(alert);
                    return eAlerts;
                }
            }, elasticAlerts);
        } catch (TransactionFailure e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Assert.assertNotNull(elasticAlerts.getElasticAlert("alert1"));
    }
}
