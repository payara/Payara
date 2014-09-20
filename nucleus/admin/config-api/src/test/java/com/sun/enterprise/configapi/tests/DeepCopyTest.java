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

package com.sun.enterprise.configapi.tests;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Configs;
import org.glassfish.config.support.ConfigurationPersistence;
import org.junit.Assert;
import org.junit.Test;
import org.jvnet.hk2.config.*;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.beans.PropertyVetoException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;

/**
 * Test the deepCopy feature of ConfigBeans.
 *
 * @author Jerome Dochez
 */
public class DeepCopyTest extends ConfigApiTest {

    public String getFileName() {
        return "DomainTest";
    }

    @Test
    public void configCopy() throws Exception {
        final Config config = getHabitat().getService(Config.class);
        Assert.assertNotNull(config);
        String configName = config.getName();
        final Config newConfig = (Config) ConfigSupport.apply(new SingleConfigCode<ConfigBeanProxy>() {
            @Override
            public Object run(ConfigBeanProxy parent) throws PropertyVetoException, TransactionFailure {
                return config.deepCopy(parent);
            }
        }, config.getParent());
        Assert.assertNotNull(newConfig);
        try {
            newConfig.setName("some-config");
        } catch(Exception e) {
            // I was expecting this...
        }
        ConfigSupport.apply(new SingleConfigCode<Config>() {
            @Override
            public Object run(Config wConfig) throws PropertyVetoException, TransactionFailure {
                wConfig.setName("some-config");
                return null;
            }
        }, newConfig);
        Assert.assertEquals(newConfig.getName(), "some-config");
        Assert.assertEquals(config.getName(), configName);

        // add it the parent
        ConfigSupport.apply(new SingleConfigCode<Configs>() {
            @Override
            public Object run(Configs wConfigs) throws PropertyVetoException, TransactionFailure {
                wConfigs.getConfig().add(newConfig);
                return null;
            }
        }, getHabitat().<Configs>getService(Configs.class));
        String resultingXML = save(document).toString();
        Assert.assertTrue("Expecting some-config, got " + resultingXML, resultingXML.contains("some-config"));
    }

    @Test
    public void parentingTest() throws Exception {

        final Config config = getHabitat().getService(Config.class);
        Assert.assertNotNull(config);
        String configName = config.getName();
        final Config newConfig = (Config) ConfigSupport.apply(new SingleConfigCode<ConfigBeanProxy>() {
            @Override
            public Object run(ConfigBeanProxy parent) throws PropertyVetoException, TransactionFailure {
                Config newConfig = (Config) config.deepCopy(parent);
                newConfig.setName("cloned-config");
                return newConfig;
            }
        }, config.getParent());
        Assert.assertNotNull(newConfig);

        // now let's check the parents are correct.
        Dom original = Dom.unwrap(config);
        Dom cloned = Dom.unwrap(newConfig);

        assertTypes(original, cloned);

        logger.info("types equality passed");
        assertParenting(original);
        assertParenting(cloned);
    }

    private void assertTypes(Dom original, Dom cloned) {
        logger.info(original.model.getTagName()+":" + original.getKey() + ":" + original.getClass().getSimpleName() +
                " and " + cloned.model.getTagName()+":" + cloned.getKey() + ":" + cloned.getClass().getSimpleName());
        Assert.assertEquals(original.getClass(), cloned.getClass());
        for (String elementName : original.getElementNames()) {
            ConfigModel.Property property = original.model.getElement(elementName);
            if (property != null && property.isLeaf()) continue;
            Dom originalChild = original.element(elementName);
            Dom clonedChild = cloned.element(elementName);
            if (originalChild==null && clonedChild==null) continue;
            Assert.assertNotNull(originalChild);
            Assert.assertNotNull(clonedChild);
            assertTypes(originalChild, clonedChild);
        }
    }

    private void assertParenting(Dom dom) {

        for (String elementName : dom.model.getElementNames()) {
            ConfigModel.Property property = dom.model.getElement(elementName);
            if (property.isLeaf()) continue;
            Dom child = dom.element(elementName);
            if (child==null) continue;
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Parent of " + child.model.targetTypeName + ":" + child.getKey() + " is " + child.parent().getKey() + " while I am " + dom.getKey());
            }
            logger.info("Parent of " + child.model.targetTypeName + ":" + child.getKey() + " is " +
                    child.parent().model.targetTypeName + ":" + child.parent().getKey() + " while I am " +
                    dom.model.targetTypeName + ":" + dom.getKey());

            Assert.assertEquals(dom, child.parent());
            assertParenting(child);
        }        
    }

    final DomDocument document = getDocument(getHabitat());

    public OutputStream save(DomDocument doc) throws IOException, XMLStreamException {
        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        outStream.reset();

        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        XMLStreamWriter writer = factory.createXMLStreamWriter(outStream);
        doc.writeTo(new IndentingXMLStreamWriter(writer));
        writer.close();
        return outStream;
    }
}
