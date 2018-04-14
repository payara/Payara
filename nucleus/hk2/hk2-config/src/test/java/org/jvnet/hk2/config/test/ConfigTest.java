/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.jvnet.hk2.config.test;

//import com.sun.enterprise.module.bootstrap.Populator;

import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.Descriptor;
import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.DynamicConfigurationService;
import org.glassfish.hk2.api.Filter;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.jvnet.hk2.config.ConfigBean;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigInjector;
import org.jvnet.hk2.config.ConfigListener;
import org.jvnet.hk2.config.ConfigParser;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.ConfigurationPopulator;
import org.jvnet.hk2.config.Dom;
import org.jvnet.hk2.config.DomDocument;
import org.jvnet.hk2.config.HK2DomConfigUtilities;
import org.jvnet.hk2.config.InjectionTarget;
import org.jvnet.hk2.config.ObservableBean;
import org.jvnet.hk2.config.Populator;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.Transactions;
import org.jvnet.hk2.config.UnprocessedChangeEvents;
import org.jvnet.hk2.config.provider.internal.ConfigInstanceListener;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This runs a set of tests to test various habitat and Dom APIs
 *  used by the config subsystem
 *
 *  @author Mahesh Kannan
 */
public class ConfigTest {
    private final static String TEST_NAME = "";
    private final static ServiceLocator habitat = ServiceLocatorFactory.getInstance().create(TEST_NAME);

    @BeforeClass
    public static void before() {
        DynamicConfigurationService dcs = habitat.getService(DynamicConfigurationService.class);
        DynamicConfiguration config = dcs.createDynamicConfiguration();
        new ConfigModule(habitat).configure(config);
        
        config.commit();
    }

    // @Test
    public void lookupAllInjectors() {
        String[] expected = {
                SimpleConnectorInjector.class.getName(), EjbContainerAvailabilityInjector.class.getName(),
                WebContainerAvailabilityInjector.class.getName(), GenericContainerInjector.class.getName(),
                GenericConfigInjector.class.getName()
        };
        List<String> expectedInjectors = Arrays.asList(expected);

        List<ServiceHandle<ConfigInjector>> inhabitants = habitat.getAllServiceHandles(ConfigInjector.class);
        Set<String> inhabitantNames = new HashSet<String>();
        for (ServiceHandle<?> inh : inhabitants) {
            inhabitantNames.add(inh.getActiveDescriptor().getImplementation());
        }

        assert(inhabitants.size() == expected.length);
        assert(inhabitantNames.containsAll(expectedInjectors));
    }

    // @Test
    public void lookupInjectorByName() {
        ServiceHandle inhabitant1 = habitat.getServiceHandle(ConfigInjector.class, "simple-connector");
        ServiceHandle inhabitant2 = habitat.getServiceHandle(ConfigInjector.class, "ejb-container-availability");
        
        assert(inhabitant1 != null && inhabitant2 != null
                && inhabitant1.getActiveDescriptor().getImplementation().equals(SimpleConnectorInjector.class.getName())
                && inhabitant2.getActiveDescriptor().getImplementation().equals(EjbContainerAvailabilityInjector.class.getName()));
    }

    // @Test
    public void testLookupOfInjectorAndCheckIfActive() {
        ServiceHandle inhabitant1 = habitat.getServiceHandle(ConfigInjector.class, "simple-connector");
        ServiceHandle inhabitant2 = habitat.getServiceHandle(ConfigInjector.class, "ejb-container-availability");
        assert(inhabitant1 != null && inhabitant2 != null
                && inhabitant1.isActive() == false
                && inhabitant2.isActive() == false);
    }

    // @Test
    public void lookupInjectorByFilter() {
        ActiveDescriptor desc = habitat.getBestDescriptor(
                new InjectionTargetFilter(EjbContainerAvailability.class.getName()));
        assert(desc != null
                && desc.getImplementation().equals(EjbContainerAvailabilityInjector.class.getName()));
    }
    
    // @Test
    public void parseDomainXml() {
        ConfigParser parser = new ConfigParser(habitat);
        URL url = this.getClass().getResource("/domain.xml");
        System.out.println("URL : " + url);

        try {
            DomDocument doc = parser.parse(url);
            System.out.println("[parseDomainXml] ==> Successfully parsed");
            assert(doc != null);
        } catch (Exception ex) {
            ex.printStackTrace();
            assert(false);
        }
    }

    // @Test
    public void lookupConnectorServiceAndEnsureNotActive() {
        SimpleConnector sc = habitat.getService(SimpleConnector.class);
        System.out.println("[lookupConnectorService] Got sc : " + sc.getClass().getName());
        ServiceHandle inhabitant1 = habitat.getServiceHandle(ConfigInjector.class, "simple-connector");
        assert(sc != null && !inhabitant1.isActive());
    }


    // @Test
    public void getConnectorServiceAndCheckIfActive() {
        SimpleConnector sc = habitat.getService(SimpleConnector.class);
        String port = sc.getPort();
        ServiceHandle inhabitant1 = habitat.getServiceHandle(ConfigInjector.class, "simple-connector");
        assert(port.equals("8080")); // && inhabitant1.isActive());
    }

    // @Test
    public void testConfig() {
        SimpleConnector sc = habitat.getService(SimpleConnector.class);
        System.out.println("[testConfig] : " + sc.getClass().getName());
        assert(Proxy.isProxyClass(sc.getClass()));
    }
    
    // @Test
    public void testDefaultValuesFromConfig() {
        SimpleConnector sc = habitat.getService(SimpleConnector.class);
        assert(
                sc.getWebContainerAvailability().getPersistenceFrequency().equals("web-method")
                        && Boolean.valueOf(sc.getEjbContainerAvailability().getAvailabilityEnabled())
                        && sc.getEjbContainerAvailability().getSfsbPersistenceType().equals("file")
                        && sc.getEjbContainerAvailability().getSfsbHaPersistenceType().equals("replicated")
        );
    }

    // @Test
    public void testDom() {
        SimpleConnector sc = habitat.getService(SimpleConnector.class);
        EjbContainerAvailability ejb = sc.getEjbContainerAvailability();

        assert(Dom.class.isAssignableFrom(Dom.unwrap(ejb).getClass())
                && ConfigBeanProxy.class.isAssignableFrom(ejb.getClass()));
    }

    // @Test
    public void testHabitatFromDom() {
        SimpleConnector sc = habitat.getService(SimpleConnector.class);
        EjbContainerAvailability ejb = sc.getEjbContainerAvailability();

        Dom ejbDom = Dom.unwrap(ejb);
        assert(ejbDom.getHabitat() != null);
    }

    // @Test
    public void testDomTx() {
        SimpleConnector sc = habitat.getService(SimpleConnector.class);
        EjbContainerAvailability ejb = sc.getEjbContainerAvailability();

        Dom ejbDom = Dom.unwrap(ejb);
        assert(ejbDom.getHabitat() != null);

        String avEnabled = ejb.getAvailabilityEnabled();
        try {
            ConfigSupport.apply(new SingleConfigCode<EjbContainerAvailability>() {
                @Override
                public Object run(EjbContainerAvailability param)
                        throws PropertyVetoException, TransactionFailure {
                    param.setSfsbHaPersistenceType("coherence");
                    param.setSfsbCheckpointEnabled("**MUST BE**");
                    return null;
                }
            }, ejb);

            //printEjb("AFTER CHANGES", ejb);
            assert(ejb.getSfsbHaPersistenceType().equals("coherence")
                    && ejb.getSfsbCheckpointEnabled().equals("**MUST BE**")
                    && ejb.getAvailabilityEnabled().equals(avEnabled));
        } catch (Exception e) {
            e.printStackTrace();
            assert(false);
        }
    }
    
    // @Test
    public void testDomTxReadOnlyAttributes() {
        SimpleConnector sc = habitat.getService(SimpleConnector.class);
        final EjbContainerAvailability ejb = sc.getEjbContainerAvailability();

        Dom ejbDom = Dom.unwrap(ejb);
        assert(ejbDom.getHabitat() != null);

        String origAVEnabled = ejb.getAvailabilityEnabled();
        final String origSFSBHaPersistenceType = ejb.getSfsbHaPersistenceType();
        try {
            ConfigSupport.apply(new SingleConfigCode<EjbContainerAvailability>() {
                @Override
                public Object run(EjbContainerAvailability param)
                        throws PropertyVetoException, TransactionFailure {
                    param.setSfsbHaPersistenceType("99999.999");
                    param.setSfsbCheckpointEnabled("**MUST BE**");

                    assert(origSFSBHaPersistenceType.equals(ejb.getSfsbHaPersistenceType()));
                    assert(! ejb.getSfsbHaPersistenceType().equals(param.getSfsbHaPersistenceType()));
                    return null;
                }
            }, ejb);

            //printEjb("AFTER CHANGES", ejb);
            assert(ejb.getSfsbHaPersistenceType().equals("99999.999")
                    && ejb.getSfsbCheckpointEnabled().equals("**MUST BE**")
                    && ejb.getAvailabilityEnabled().equals(origAVEnabled));
        } catch (Exception e) {
            e.printStackTrace();
            assert(false);
        }
    }

    // @Test
    public void testGetImplAndAddListener() {
        SimpleConnector sc = habitat.getService(SimpleConnector.class);
        final EjbContainerAvailability ejb = sc.getEjbContainerAvailability();
        ObservableBean obean = (ObservableBean) ConfigSupport.getImpl(ejb);
        EjbObservableBean ejbBean = new EjbObservableBean();

        assert(ejbBean.getCount() == 0);
        obean.addListener(ejbBean);
        try {
            ConfigSupport.apply(new SingleConfigCode<EjbContainerAvailability>() {
                @Override
                public Object run(EjbContainerAvailability param)
                        throws PropertyVetoException, TransactionFailure {
                    param.setSfsbHaPersistenceType("DynamicData");
                    param.setSfsbCheckpointEnabled("**MUST BE**");
                    assert(! ejb.getSfsbHaPersistenceType().equals(param.getSfsbHaPersistenceType()));
                    return null;
                }
            }, ejb);

            //printEjb("AFTER CHANGES", ejb);
            assert(ejb.getSfsbHaPersistenceType().equals("DynamicData")
                    && ejb.getSfsbCheckpointEnabled().equals("**MUST BE**"));

            assert(ejbBean.getCount() == 1);
        } catch (Exception e) {
            e.printStackTrace();
            assert(false);
        }
        try {
            ConfigSupport.apply(new SingleConfigCode<EjbContainerAvailability>() {
                @Override
                public Object run(EjbContainerAvailability param)
                        throws PropertyVetoException, TransactionFailure {
                    param.setSfsbHaPersistenceType("DynamicData1");
                    param.setSfsbCheckpointEnabled("**MUST BE**");
                    assert(! ejb.getSfsbHaPersistenceType().equals(param.getSfsbHaPersistenceType()));
                    return null;
                }
            }, ejb);

            //printEjb("AFTER CHANGES", ejb);
            assert(ejb.getSfsbHaPersistenceType().equals("DynamicData1")
                    && ejb.getSfsbCheckpointEnabled().equals("**MUST BE**"));

            assert(ejbBean.getCount() == 2);

            System.out.println("getImpl(ejb) == > "
                    + ConfigSupport.getImpl(ejb).getClass().getName());
            System.out.println("getImpl(ejb).getMasterView() == > "
                    + ConfigSupport.getImpl(ejb).getMasterView().getClass().getName());
            System.out.println("getImpl(ejb).getProxyType() == > "
                    + ConfigSupport.getImpl(ejb).getProxyType().getClass().getName());
        } catch (Exception e) {
            e.printStackTrace();
            assert(false);
        }
    }

    // @Test
    public void testGetConfigBean() {
        SimpleConnector sc = habitat.getService(SimpleConnector.class);
        final EjbContainerAvailability ejb = sc.getEjbContainerAvailability();
        ConfigBean ejbConfigBean = (ConfigBean) ConfigBean.unwrap(ejb);

        assert(ejbConfigBean != null);
    }

    // @Test
    public void testGenericContainerInjector() {
        ServiceHandle sh = habitat.getServiceHandle(ConfigInjector.class, "generic-container");

        assert(sh != null);

    }

    // @Test
    public void testLongDataType() {
        GenericContainer gc = habitat.getService(GenericContainer.class);
        Object obj = gc.getStartupTime();
        assert(obj.getClass() == Long.class);

    }

    // @Test
    public void testIntDataType() {
        GenericContainer gc = habitat.getService(GenericContainer.class);
        Object obj = gc.getIntValue();
        assert(obj.getClass() == Integer.class);

    }
    
    // @Test
    public void testConfigurationPopulator() {
        DummyPopulator pop = (DummyPopulator) habitat.getService(Populator.class);

        ConfigurationPopulator confPopulator = habitat.getService(ConfigurationPopulator.class);
        confPopulator.populateConfig((ServiceLocator) habitat);

        assert(pop.isPopulateCalled());
    }

    // @Test
    public void testSingletonProxy() {
        SimpleConnector simpleConnector1 = habitat.getService(SimpleConnector.class);
        SimpleConnector simpleConnector2 = habitat.getService(SimpleConnector.class);

        System.out.println("[testSingleProxy] Got simpleConnector1 : " + simpleConnector1.getClass().getName());
        System.out.println("[testSingleProxy] Got simpleConnector2 : " + simpleConnector2.getClass().getName());

        assert(simpleConnector1 != null && simpleConnector1 == simpleConnector2);
    }
    
    /**
     * This test is an unfortunate reaction to the fact that JDK 7 does not run the
     * tests above in order, and that the fact that the ordering seems to be important
     * to the passing of the above tests
     */
    @Test
    public void runTestsInOrder() {
        lookupAllInjectors();
        lookupInjectorByName();
        testLookupOfInjectorAndCheckIfActive();
        lookupInjectorByFilter();
        parseDomainXml();
        lookupConnectorServiceAndEnsureNotActive();
        getConnectorServiceAndCheckIfActive();
        testConfig();
        testDefaultValuesFromConfig();
        testDom();
        testHabitatFromDom();
        testDomTx();
        testDomTxReadOnlyAttributes();
        testGetImplAndAddListener();
        testGetConfigBean();
        testGenericContainerInjector();
        testLongDataType();
        testIntDataType();
        testConfigurationPopulator();
        testSingletonProxy();
        
    }
    
    /**
     * Ensures that even the non-standard format of metadata from the hk2-config subsystem can
     * be read from the service in addClasses.  addClasses will now read both forms, if the
     * documented form fails, it'll try the hk2-config form
     */
    @Test
    public void testAddClassOfInjector() {
        ServiceLocator locator = ServiceLocatorFactory.getInstance().create(null);
        
        List<ActiveDescriptor<?>> added = ServiceLocatorUtilities.addClasses(locator, EjbContainerAvailabilityInjector.class);
        ActiveDescriptor<?> descriptor = added.get(0);
        
        Assert.assertEquals("org.jvnet.hk2.config.test.EjbContainerAvailability", ServiceLocatorUtilities.getOneMetadataField(descriptor, "target"));
        
    }
    
    @Test
    public void testEnableConfigUtilities() {
        ServiceLocator locator = ServiceLocatorFactory.getInstance().create(null);
        
        Assert.assertNull(locator.getService(ConfigSupport.class));
        Assert.assertNull(locator.getService(ConfigurationPopulator.class));
        Assert.assertNull(locator.getService(Transactions.class));
        Assert.assertNull(locator.getService(ConfigInstanceListener.class));
        
        HK2DomConfigUtilities.enableHK2DomConfiguration(locator);
        
        // Twice to check idempotence
        HK2DomConfigUtilities.enableHK2DomConfiguration(locator);
        
        Assert.assertEquals(1, locator.getAllServices(ConfigSupport.class).size());
        Assert.assertEquals(1, locator.getAllServices(ConfigurationPopulator.class).size());
        Assert.assertEquals(1, locator.getAllServices(Transactions.class).size());
        Assert.assertEquals(1, locator.getAllServices(ConfigInstanceListener.class).size());
    }

    private static void printEjb(String message, EjbContainerAvailability ejb) {
        StringBuilder sb = new StringBuilder(ejb.getClass().getName());
        sb.append(" : " ).append(ejb.getAvailabilityEnabled())
                .append("; ").append(ejb.getSfsbCheckpointEnabled())
                .append("; ").append(ejb.getSfsbHaPersistenceType())
                .append("; ").append(ejb.getSfsbQuickCheckpointEnabled())
                .append(";").append(ejb.getSfsbStorePoolName());

        System.out.println(message + " ==> " + sb.toString());
    }

    private static void printWeb(String message, WebContainerAvailability web) {
        StringBuilder sb = new StringBuilder(web.getClass().getName());
        sb.append(" : " ).append(web.getAvailabilityEnabled())
                .append("; ").append(web.getDisableJreplica())
                .append("; ").append(web.getPersistenceFrequency())
                .append("; ").append(web.getPersistenceScope())
                .append(";").append(web.getPersistenceType())
                .append(";").append(web.getSsoFailoverEnabled());

        System.out.println(message + " ==> " + sb.toString());
    }

    private static class InjectionTargetFilter
            implements Filter {

        String targetName;

        InjectionTargetFilter(String targetName) {
            this.targetName = targetName;
        }

        @Override
        public boolean matches(Descriptor d) {
            if (d.getQualifiers().contains(InjectionTarget.class.getName())) {
                List<String> list = d.getMetadata().get("target");
                if (list != null && list.get(0).equals(targetName)) {
                    return true;
                }
            }

            return false;
        }
    }
    
    private static class EjbObservableBean
        implements ConfigListener {

        private AtomicInteger count = new AtomicInteger();
        
        @Override
        public UnprocessedChangeEvents changed(PropertyChangeEvent[] events) {
            System.out.println("** EjbContainerAvailability changed ==> " + count.incrementAndGet());
            return null;
        }
        
        public int getCount() {
            return count.get();
        }
    }

}
