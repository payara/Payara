/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.configapi.tests.extensibility;

import com.sun.enterprise.config.serverbeans.Application;
import com.sun.enterprise.configapi.tests.ConfigApiTest;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import org.glassfish.tests.utils.Utils;
import org.glassfish.api.admin.config.Container;
import org.glassfish.hk2.api.ServiceLocator;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.List;

/**
 * @author Jerome Dochez
 */
public class ContainerExtensionTest extends ConfigApiTest {


    ServiceLocator habitat = Utils.instance.getHabitat(this);

    @Override
    public String getFileName() {
        return "Extensibility";
    }
    
    @Test
    public void existenceTest() {

        Config config = habitat.<Domain>getService(Domain.class).getConfigs().getConfig().get(0);
        List<Container> containers = config.getContainers();
        assertTrue(containers.size()==2);
        RandomContainer container = (RandomContainer) containers.get(0);
        assertEquals("random", container.getName());
        assertEquals("1243", container.getNumberOfRuntime());
        RandomElement element = container.getRandomElement();
        assertNotNull(element);
        assertEquals("foo", element.getAttr1());
    }

    @Test
    public void extensionTest() {
        Config config = habitat.<Domain>getService(Domain.class).getConfigs().getConfig().get(0);
        RandomExtension extension = config.getExtensionByType(RandomExtension.class);
        assertNotNull(extension);
        assertEquals("foo", extension.getSomeAttribute());
    }
    
    @Test
    public void applicationExtensionTest() {
        Application a = habitat.getService(Application.class);
        List<AnApplicationExtension> taes = a.getExtensionsByType(AnApplicationExtension.class);
        assertEquals(taes.size(), 2);
    }
}
