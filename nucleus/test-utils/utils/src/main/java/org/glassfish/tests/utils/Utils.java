/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2019] [Payara Foundation and/or its affiliates]

package org.glassfish.tests.utils;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import com.sun.enterprise.module.ModulesRegistry;
import com.sun.enterprise.module.bootstrap.StartupContext;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.jvnet.hk2.config.ConfigParser;
import org.jvnet.hk2.config.DomDocument;

import com.sun.enterprise.module.single.StaticModulesRegistry;

/**
 * Utilities to create a configured Habitat and cache them
 *
 * @author Jerome Dochez
 */
public class Utils {

    private static final String habitatName = "default";
    private static final String inhabitantPath = "META-INF/inhabitants";
    private static final String INSTANCE_ROOT_PROP_NAME = "com.sun.aas.instanceRoot";
    private static final String INSTALL_ROOT_PROP_NAME = "com.sun.aas.installRoot";

    private static Map<String, ServiceLocator> habitats = new HashMap<String, ServiceLocator>();
    public static final Utils instance = new Utils();

    public synchronized ServiceLocator getHabitat(ConfigApiTest test) {

        final String fileName = test.getFileName();
        // we cache the habitat per xml file

        if (habitats.containsKey(fileName))  {
            return habitats.get(fileName);
        }

        ServiceLocator habitat = getNewHabitat(test);
        habitats.put(fileName, habitat);
        return habitat;
    }

    private static synchronized ServiceLocator getNewHabitat(final ConfigApiTest test) {
        final ServiceLocator sl = getNewHabitat();

        final String fileName = test.getFileName();
        ConfigParser configParser = new ConfigParser(sl);

		long now = System.currentTimeMillis();
		URL url = Utils.class.getClassLoader().getResource(fileName + ".xml");
		if (url != null) {
			try {
			    DomDocument testDocument = test.getDocument(sl);
				DomDocument document = configParser.parse(url,
						testDocument);
				ServiceLocatorUtilities.addOneConstant(sl, document);
				test.decorate(sl);
			} catch (Exception e) {
				e.printStackTrace();
			}
			Logger.getAnonymousLogger().fine(
					"time to parse domain.xml : "
							+ String.valueOf(System.currentTimeMillis() - now));
		}
        
        return sl;
    }

    public static ServiceLocator getNewHabitat() {
    	final String root =  Utils.class.getResource("/").getPath();
        return getNewHabitat(root);
    }

    public static ServiceLocator getNewHabitat(String root) {
 
        Properties properties = new Properties();
        properties.put(INSTALL_ROOT_PROP_NAME, root);
        properties.put(INSTANCE_ROOT_PROP_NAME, root);
        ModulesRegistry registry = new StaticModulesRegistry(Utils.class.getClassLoader(), new StartupContext(properties));
        ServiceLocator defaultSL = registry.createServiceLocator("default");
        return defaultSL;
    }

	public void shutdownServiceLocator(
			final ConfigApiTest test) {
	    String fileName = test.getFileName();
	    
        if (habitats.containsKey(fileName))  {
        	ServiceLocator locator = habitats.remove(fileName);
        	ServiceLocatorFactory.getInstance().destroy(locator);
        }
	}
}
