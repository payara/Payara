/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015-2017 Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.beans.PropertyVetoException;
import java.net.URL;
import java.util.List;
import java.util.Random;

import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.DynamicConfigurationService;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hk2.config.ConfigParser;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.DomDocument;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

public class ConfigDisposalTest {
    private final static String TEST_NAME = "ConfigDisposal";
    private final static Random RANDOM = new Random();
    
    private ServiceLocator habitat;

    @Before
    public void before() {
        String testName = TEST_NAME + RANDOM.nextInt();
        
        habitat = ServiceLocatorFactory.getInstance().create(testName);
        DynamicConfigurationService dcs = habitat.getService(DynamicConfigurationService.class);
        DynamicConfiguration config = dcs.createDynamicConfiguration();
        new ConfigModule(habitat).configure(config);
        
        config.commit();
        parseDomainXml();
    }

    @After
    public void after() {
        ServiceLocatorFactory.getInstance().destroy(habitat);
        habitat = null;
    }

    public void parseDomainXml() {
        ConfigParser parser = new ConfigParser(habitat);
        URL url = ConfigDisposalTest.class.getResource("/domain.xml");
        System.out.println("URL : " + url);

        try {
            DomDocument doc = parser.parse(url, new SimpleDocument(habitat));
            System.out.println("[parseDomainXml] ==> Successfully parsed");
            assert(doc != null);
        } catch (Exception ex) {
            ex.printStackTrace();
            assert(false);
        }
    }

    // to regenerate config injectors do the following in command line:
    // mvn config-generator:generate-test-injectors
    // cp target/generated-sources/hk2-config-generator/src/test/java/org/jvnet/hk2/config/test/* src/test/java/org/jvnet/hk2/config/test/
    @Test // Removed container causes nested elements be removed
    public void testDisposedNestedAndNamed() throws TransactionFailure {
        SimpleConnector sc = habitat.getService(SimpleConnector.class);
        assertEquals("Extensions", 1, sc.getExtensions().size());
        assertEquals("Nested children", 2, sc.getExtensions().get(0).getExtensions().size());
        
        ConfigSupport.apply(new SingleConfigCode<SimpleConnector>() {
            @Override
            public Object run(SimpleConnector sc)
                    throws PropertyVetoException, TransactionFailure {
                List<GenericContainer> extensions = sc.getExtensions();
                GenericContainer child = extensions.get(extensions.size() - 1);
                extensions.remove(child);
                return child;
            }
        }, sc);

        assertEquals("Removed extensions", 0, sc.getExtensions().size());
        // NOTE, habitat.getService(GenericConfig.class) creates new instance
        //       if not all instances of GenericConfig descriptors are removed 
        assertNull("GenericContainer descriptor still has " +
                habitat.getDescriptors(BuilderHelper.createContractFilter(GenericContainer.class.getName())),
                habitat.getService(GenericContainer.class));
        assertNull("GenericConfig descriptor test still has " +
                habitat.getDescriptors(BuilderHelper.createContractFilter(GenericConfig.class.getName())),
                habitat.getService(GenericConfig.class, "test"));
        assertNull("GenericConfig descriptor still has " +
                habitat.getDescriptors(BuilderHelper.createContractFilter(GenericConfig.class.getName())),
                habitat.getService(GenericConfig.class));
        // assert with VisualVm there is no GenericContainer and GenericConfig instances with OQL query:
        // select x.implementation.toString() from org.jvnet.hk2.config.test.SimpleConfigBeanWrapper x
    }

    @Test 
    public void testRemoveNamed() throws TransactionFailure {
        SimpleConnector sc = habitat.getService(SimpleConnector.class);
        assertEquals("Eextensions", 1, sc.getExtensions().size());
        assertEquals("Nested children", 2, sc.getExtensions().get(0).getExtensions().size());
        
        GenericContainer extension = sc.getExtensions().get(0);

        ConfigSupport.apply(new SingleConfigCode<GenericContainer>() {
            @Override
            public Object run(GenericContainer container)
                    throws PropertyVetoException, TransactionFailure {
                List<GenericConfig> childExtensions = container.getExtensions();
                GenericConfig nestedChild = childExtensions.get(childExtensions.size() - 1);
                childExtensions.remove(nestedChild);
                return nestedChild;
            }
        }, extension);

        assertEquals("Removed extensions", 1, sc.getExtensions().size());
        assertNull("Removed nested named child", habitat.getService(GenericConfig.class, "test2"));
        // make sure other elements are not removed
        assertNotNull("Nested named child", habitat.getService(GenericConfig.class, "test1"));
        assertNotNull("Nested named grand child", habitat.getService(GenericConfig.class, "test"));
    }

    @Test 
    public void testRemovedOne() throws TransactionFailure {
        SimpleConnector sc = habitat.getService(SimpleConnector.class);
        assertEquals("Extensions", 1, sc.getExtensions().size());

        ConfigSupport.apply(new SingleConfigCode<SimpleConnector>() {
            @Override
            public Object run(SimpleConnector sc)
                    throws PropertyVetoException, TransactionFailure {
                List<GenericContainer> extensions = sc.getExtensions();
                GenericContainer child = sc.createChild(GenericContainer.class);
                WebContainerAvailability grandchild = child.createChild(WebContainerAvailability.class);
                child.setWebContainerAvailability(grandchild);
                extensions.add(child);
                return child;
            }
        }, sc);

        assertEquals("Added extensions", 2, sc.getExtensions().size());

        ConfigSupport.apply(new SingleConfigCode<SimpleConnector>() {
            @Override
            public Object run(SimpleConnector sc)
                    throws PropertyVetoException, TransactionFailure {
                List<GenericContainer> extensions = sc.getExtensions();
                GenericContainer child = extensions.get(extensions.size() - 1);
                extensions.remove(child);
                return child;
            }
        }, sc);

        assertEquals("Removed extensions", 1, sc.getExtensions().size());

        assertNotNull("Nested named child 1", habitat.getService(GenericConfig.class, "test1"));
        assertNotNull("Nested named grand child", habitat.getService(GenericConfig.class, "test"));
        assertNotNull("Nested named child 2", habitat.getService(GenericConfig.class, "test2"));
        assertNotNull("GenericContainer Service", habitat.getService(GenericContainer.class));
    }

    @Test 
    public void testReplaceNode() throws TransactionFailure {
        SimpleConnector sc = habitat.getService(SimpleConnector.class);
        assertEquals("Eextensions", 1, sc.getExtensions().size());
        
        GenericContainer extension = sc.getExtensions().get(0);
        assertEquals("Child extensions", 2, extension.getExtensions().size());
        GenericConfig nestedChild = extension.getExtensions().get(0);

        ConfigSupport.apply(new SingleConfigCode<GenericConfig>() {
            @Override
            public Object run(GenericConfig nestedChild)
                    throws PropertyVetoException, TransactionFailure {
                nestedChild.setGenericConfig(null);
                GenericConfig newChild = nestedChild.createChild(GenericConfig.class);
                newChild.setName("test3");
                nestedChild.setGenericConfig(newChild);
                return nestedChild;
            }
        }, nestedChild);

        assertNotNull("Nested named child 1", habitat.getService(GenericConfig.class, "test1"));
        assertNotNull("Nested named child 2", habitat.getService(GenericConfig.class, "test2"));
        assertNull("Nested named grand child replaced", habitat.getService(GenericConfig.class, "test"));
    }

    @Test 
    public void testReplaceChild() throws TransactionFailure {
        SimpleConnector sc = habitat.getService(SimpleConnector.class);
        assertEquals("Eextensions", 1, sc.getExtensions().size());
        
        GenericContainer extension = sc.getExtensions().get(0);
        assertEquals("Child extensions", 2, extension.getExtensions().size());

        ConfigSupport.apply(new SingleConfigCode<GenericContainer>() {
            @Override
            public Object run(GenericContainer extension)
                    throws PropertyVetoException, TransactionFailure {
                GenericConfig newChild = extension.createChild(GenericConfig.class);
                newChild.setName("test3");
                GenericConfig nestedChild = extension.getExtensions().set(0, newChild);
                return nestedChild;
            }
        }, extension);

        assertEquals("Extensions", 2, extension.getExtensions().size());
        assertNull("Nested named child 1", habitat.getService(GenericConfig.class, "test1"));
        assertNull("Nested named grand child replaced", habitat.getService(GenericConfig.class, "test"));
        assertEquals("New Nested child", "test3", extension.getExtensions().get(0).getName());
        // can't verify it with getService becaue named alias is not created with createChild
        //assertNotNull("New Nested child", habitat.getService(GenericConfig.class, "test3"));
        assertNotNull("Nested named child 2", habitat.getService(GenericConfig.class, "test2"));
    }
}
