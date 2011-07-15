/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.config.serverbeans.AccessLog;
import com.sun.enterprise.config.serverbeans.HttpService;
import com.sun.enterprise.config.serverbeans.JavaConfig;
import com.sun.enterprise.config.serverbeans.Profiler;
import org.glassfish.tests.utils.Utils;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.config.ConfigBean;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.TransactionFailure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: Jerome Dochez
 * Date: Mar 20, 2008
 * Time: 4:48:14 PM
 */
public class DirectCreationTest extends ConfigPersistence {

    Habitat habitat = Utils.getNewHabitat(this);

    /**
     * Returns the file name without the .xml extension to load the test configuration
     * from. By default, it's the name of the TestClass.
     *
     * @return the configuration file name
     */
    public String getFileName() {
        return "DomainTest";
    }

    @Override
    public Habitat getHabitat() {
        return habitat;
    }

    public void doTest() throws TransactionFailure {

        HttpService service = habitat.getComponent(HttpService.class);

        ConfigBean serviceBean = (ConfigBean) ConfigBean.unwrap(service);
        Class<?>[] subTypes = null;
        try {
            subTypes = ConfigSupport.getSubElementsTypes(serviceBean);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            throw new RuntimeException(e);
        }

        ConfigSupport support = getHabitat().getComponent(ConfigSupport.class);

        for (Class<?> subType : subTypes) {
            // TODO:  JL force compilation error to mark this probably edit point for grizzly config
            if (subType.getName().endsWith("HttpListener")) {
                Map<String, String> configChanges = new HashMap<String, String>();
                configChanges.put("id", "funky-listener");
                configChanges.put("server-name", "server");
                configChanges.put("default-virtual-server", "servre");
                configChanges.put("address", "124.2.4.6");
                configChanges.put("port", "8034");
                support.createAndSet(serviceBean, (Class<? extends ConfigBeanProxy>)subType, configChanges);
                break;
            }
        }

        support.createAndSet(serviceBean, AccessLog.class, (List) null);

        List<ConfigSupport.AttributeChanges> profilerChanges = new ArrayList<ConfigSupport.AttributeChanges>();
        String[] values = { "-Xmx512m", "-RFtrq", "-Xmw24" };
        ConfigSupport.MultipleAttributeChanges multipleChanges = new ConfigSupport.MultipleAttributeChanges("jvm-options", values );
        String[] values1 = { "profile" };
        ConfigSupport.MultipleAttributeChanges multipleChanges1 = new ConfigSupport.MultipleAttributeChanges("name", values1 );
        profilerChanges.add(multipleChanges);
        profilerChanges.add(multipleChanges1);
        support.createAndSet((ConfigBean) ConfigBean.unwrap(habitat.getComponent(JavaConfig.class))
                , Profiler.class, profilerChanges);
    }

    @Test
    public void directAttributeNameTest() throws ClassNotFoundException {

        boolean foundOne=false;
        for (String attrName :
                ((ConfigBean) ConfigBean.unwrap(habitat.getComponent(JavaConfig.class))).model.getAttributeNames()) {
            assertTrue(attrName!=null);
            foundOne=true;
        }
        assertTrue(foundOne);
    }

    public boolean assertResult(String s) {
        return s.contains("id=\"funky-listener\"");
    }
}
