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
import java.util.ArrayList;
import java.util.List;

import org.glassfish.paas.admin.CloudServices;
import org.glassfish.paas.tenantmanager.config.TenantManagerConfig;
import org.glassfish.paas.tenantmanager.entity.DefaultService;
import org.glassfish.paas.tenantmanager.entity.Tenant;
import org.glassfish.paas.tenantmanager.entity.TenantAdmin;
import org.glassfish.paas.tenantmanager.entity.TenantExtension;
import org.glassfish.paas.tenantmanager.entity.TenantServices;
import org.glassfish.paas.tenantmanager.impl.TenantDocument;
import org.glassfish.paas.tenantmanager.impl.TenantManagerEx;
import org.junit.Assert;
import org.junit.Test;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.config.ConfigBean;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.Dom;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.util.io.FileUtils;

/**
 * Tests for TestManagerEx.
 * 
 * @author Andriy Zhdanov
 *
 */
public class TenantManagerTest extends ConfigApiTest {
    Habitat habitat = getHabitat();
    TenantManagerEx tenantManager = habitat.getComponent(TenantManagerEx.class);
    Config config = habitat.getComponent(Config.class);
    CloudServices cs = config.getExtensionByType(CloudServices.class);
    TenantManagerConfig tenantManagerConfig = cs.getCloudServiceByType(TenantManagerConfig.class);
    String fileStore = tenantManagerConfig.getFileStore();
    String sourcePath = getClass().getResource("/").getPath();

    private void setupTest(String ... tenantNames) throws IOException {
        for (String tenantName : tenantNames) {
            tenantManager.delete(tenantName); // dispose for clean test
            FileUtils.copyTree(new File(sourcePath + tenantName), new File(fileStore + "/" + tenantName));
        }
    }

    private String readConfigXml(URL config) throws IOException, URISyntaxException {
        File file = new File(config.toURI());
        String contents = FileUtils.readSmallFile(file);
        // strip copyright notice
        return contents.substring(contents.indexOf("<tenant"));
    }

    // Compare expected tenant.xml contents with actual config bean xml contents.
    // TODO: find in glassfish and re-use it
    private void assertConfigXml(String msg, String expectedFileName, ConfigBeanProxy actualConfig) throws MalformedURLException, IOException, URISyntaxException {
        ConfigBean configBean = (ConfigBean) Dom.unwrap(actualConfig);
        TenantDocument doc = (TenantDocument) configBean.document;
        String expectedUrl = TenantManagerTest.class.getResource("/").toString() + expectedFileName + "-expected.xml";
        String expected = readConfigXml(new URL(expectedUrl));
        String actual = readConfigXml(doc.getResource());
        Assert.assertEquals(msg, expected, actual);

    }

    // Verify tenant1 and tenant2 can be retrieved.
    @Test
    public void testGet() throws IOException {
        setupTest("tenant1", "tenant2");
        Assert.assertNotNull("tenantManager", tenantManager);
        tenantManager.setCurrentTenant("tenant1");
        Assert.assertEquals("currentTenant", "tenant1", tenantManager.getCurrentTenant());
        Tenant tenant = tenantManager.get(Tenant.class);
        Assert.assertNotNull("currentTenant", tenant);
        Assert.assertEquals("currentTenant", "tenant1", tenant.getName());
        Assert.assertNotNull("Zero Services", tenant.getServices());
        Assert.assertNotNull("Zero Extensions", tenant.getExtensions());
        tenantManager.setCurrentTenant("tenant2");
        tenant = tenantManager.get(Tenant.class);
        Assert.assertNotNull("currentTenant", tenant);
        Assert.assertEquals("currentTenant", "tenant2", tenant.getName());
        Assert.assertTrue("Has Environments", tenant.hasCreatedEnvironment());
        Assert.assertEquals("Tenant Services", 3, tenant.getServices().getTenantServices().size());
        Assert.assertEquals("Default Services", 1, tenant.getServices().getDefaultServices().size());
        Assert.assertEquals("Shared Services", 1, tenant.getServices().getSharedServices().size());
        Assert.assertEquals("External Services", 1, tenant.getServices().getExternalServices().size());
        Assert.assertNotNull("Specific Service", tenant.getServices().getServiceByType(DefaultService.class));
        Assert.assertNotNull("Tenant Extensions", tenant.getExtensions());
    }

    // Update existing tenant1, verify tenant xml is updated.
    @Test
    public void testUpdate() throws TransactionFailure, MalformedURLException, IOException, URISyntaxException  {
        setupTest("tenant1");
        Assert.assertNotNull("tenantManager", tenantManager);
        tenantManager.setCurrentTenant("tenant1");
        Tenant tenant = tenantManager.get(Tenant.class);
        // Note, it's not possible to update root element,
        //  see WriteableView.setter(WriteableView.java:235).
        TenantAdmin admin = tenant.getTenantAdmin();
        tenantManager.executeUpdate(new SingleConfigCode<TenantAdmin>() {
            @Override
            public Object run(TenantAdmin admin) throws TransactionFailure {
                admin.setName("test");
                return admin;
            }
        }, admin);
        assertConfigXml("Updated tenant xml", "tenant1", tenant);
    }

    // verify restricted access to the configuration - can't modify the same object simultaneously.
    @Test
    public void testLockingSameElement() throws TransactionFailure, MalformedURLException, IOException, URISyntaxException  {
        setupTest("tenant1");
        Assert.assertNotNull("tenantManager", tenantManager);
        tenantManager.setCurrentTenant("tenant1");
        final Tenant tenant = tenantManager.get(Tenant.class);
        TenantAdmin admin = tenant.getTenantAdmin();
        tenantManager.executeUpdate(new SingleConfigCode<TenantAdmin>() {
            @Override
            public Object run(TenantAdmin admin) throws TransactionFailure {
                TenantAdmin conflict = tenant.getTenantAdmin();

                try {
                    // ConfigSupport.apply inside tenantManager.executeUpdate is OK
                    ConfigSupport.apply(new SingleConfigCode<TenantAdmin>() {
                        @Override
                        public Object run(TenantAdmin admin) throws TransactionFailure {
                            return admin;
                        }
                    }, conflict);
                    Assert.fail("Failure expected");
                } catch (TransactionFailure e) {
                    Assert.assertTrue("Failure expected", true);
                }
                return admin;
            }
        }, admin);
    }

    // verify can modify different elements of tenant concurrently.
    @Test
    public void testNonLockingTenant() throws TransactionFailure, MalformedURLException, IOException, URISyntaxException  {
        setupTest("tenant1");
        Assert.assertNotNull("tenantManager", tenantManager);
        tenantManager.setCurrentTenant("tenant1");
        Tenant tenant = tenantManager.get(Tenant.class);
        final TenantAdmin admin = tenant.getTenantAdmin();
        // modify extensions element
        tenantManager.executeUpdate(new SingleConfigCode<Tenant>() {
            @Override
            public Object run(Tenant tenant) throws TransactionFailure {
                TenantExtension extension = tenant
                        .createChild(TenantExtension.class);
                tenant.getExtensions().add(extension);
                // modify admin element
                // make sure tenantManager.executeUdpate does not break behavior 
                tenantManager.executeUpdate(new SingleConfigCode<TenantAdmin>() {
                    @Override
                    public Object run(TenantAdmin admin)
                            throws TransactionFailure {
                        admin.setName("test");
                        return admin;
                    }
                }, admin);
                // verify file is written without changes to extensions!
                try {
                    assertConfigXml("Updated tenant xml", "tenant1", tenant);
                } catch (Exception e) {
                    throw new TransactionFailure("fake", e);
                }

                return tenant;
            }
        }, tenant);
        // verify file is written with both changes!
        assertConfigXml("Updated tenant xml", "tenant1-nonlocking", tenant);
    }

    // verify can modify different elements of tenant simultaneously.
    // note, tenantManager.executeUpdate works in a manner of locking file.
    @Test
    public void testLockingFile() throws TransactionFailure, MalformedURLException, IOException, URISyntaxException, InterruptedException  {
        setupTest("tenant1");
        Assert.assertNotNull("tenantManager", tenantManager);
        tenantManager.setCurrentTenant("tenant1");
        final Tenant tenant = tenantManager.get(Tenant.class);
        final List<String> errors = new ArrayList<String>(); 
        // modify extensions element
        Thread t1 = new Thread() {

            @Override
            public void run() {
                try {
                    tenantManager.executeUpdate(new SingleConfigCode<Tenant>() {
                        @Override
                        public Object run(Tenant tenant) throws TransactionFailure {
                            TenantExtension extension = tenant
                                    .createChild(TenantExtension.class);
                            tenant.getExtensions().add(extension);
                            return tenant;
                        }
                    }, tenant);
                } catch (TransactionFailure e) {
                    synchronized(errors) {
                        errors.add(e.getMessage());
                    }
                }
            }
            
        };
        Thread t2 = new Thread() {

            @Override
            public void run() {
                // modify admin element
                TenantAdmin admin = tenant.getTenantAdmin();
                try {
                    tenantManager.executeUpdate(new SingleConfigCode<TenantAdmin>() {
                        @Override
                        public Object run(TenantAdmin admin)
                                throws TransactionFailure {
                            admin.setName("test");
                            return admin;
                        }
                    }, admin);
                } catch (TransactionFailure e) {
                    synchronized(errors) {
                        errors.add(e.getMessage());
                    }
                }
            }
            
        };
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        Assert.assertEquals("No errors", new ArrayList<String>(), errors);
        
        // verify file is written with both changes!
        assertConfigXml("Updated tenant xml", "tenant1-nonlocking", tenant);
    }

    // TODO: verify adding extensions elements in its transaction

    // Create verify created tenant.xml literally
    // Update newly created tenenat (add service)
    // Then delete, verify exception is thrown on next get.
    @Test
    public void testCreateDelete() throws MalformedURLException, IOException, URISyntaxException {
        Assert.assertNotNull("tenantManager", tenantManager);
        try {
            Tenant tenant = tenantManager.create("tenant3", "admin");
            Assert.assertNotNull("tenant", tenant);
            Assert.assertEquals("tenant", "tenant3", tenant.getName());
            assertConfigXml("New tenant xml", "tenant3", tenant);
    
            // this is how some extension can be added
            try {
                ConfigSupport.apply(new SingleConfigCode<Tenant>() {
                    @Override
                    public Object run(Tenant tenant) throws TransactionFailure {
                        TenantExtension extension = tenant.createChild(TenantExtension.class);
                        tenant.getExtensions().add(extension);
                        return tenant;
                    }
                }, tenant);
            } catch (TransactionFailure e) {
                // TODO Auto-generated catch block
                e.printStackTrace();            
            }
            Assert.assertNotNull("Extension", tenant.getExtensionByType(TenantExtension.class));

            TenantServices services = tenant.getServices();
            // this is how some service can be added
            try {
                ConfigSupport.apply(new SingleConfigCode<TenantServices>() {
                    @Override
                    public Object run(TenantServices services) throws TransactionFailure {
                        DefaultService service = services.createChild(DefaultService.class);
                        services.getTenantServices().add(service);
                        return services;
                    }
                }, services);
            } catch (TransactionFailure e) {
                // TODO Auto-generated catch block
                e.printStackTrace();            
            }        
            assertConfigXml("Updated new tenant xml", "tenant3-updated", tenant);
    
            tenantManager.delete("tenant3");
            tenantManager.setCurrentTenant("tenant3");
            try {
                tenant = tenantManager.get(Tenant.class);
                Assert.fail("Tenat must have been deleted");
            } catch (IllegalArgumentException e) {
                Assert.assertTrue("Tenant deleted", true);
            }
        } finally {
            // clean up
            try {
                tenantManager.delete("tenant3");
            } catch (Throwable e) {
                // ignore
            }
        }
    }
}
