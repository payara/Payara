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

import com.sun.enterprise.config.serverbeans.JavaConfig;
import org.glassfish.grizzly.config.dom.Http;
import org.glassfish.grizzly.config.dom.NetworkConfig;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.tests.utils.Utils;
import org.jvnet.hk2.config.ConfigBean;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.TransactionFailure;

import java.util.HashMap;
import java.util.Map;

/**
 * User: Jerome Dochez
 * Date: Mar 12, 2008
 * Time: 8:50:42 PM
 */
public class DirectAccessTest extends ConfigPersistence {
    
    ServiceLocator habitat = Utils.instance.getHabitat(this);

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
    public ServiceLocator getBaseServiceLocator() {
        return habitat;
    }
        
    @Override
    public ServiceLocator getHabitat() {
    	return getBaseServiceLocator();
    }
    
    public void doTest() throws TransactionFailure {
        NetworkConfig networkConfig = habitat.getService(NetworkConfig.class);
        final NetworkListener listener = networkConfig.getNetworkListeners()
            .getNetworkListener().get(0);
        final Http http = listener.findHttpProtocol().getHttp();
        ConfigBean config = (ConfigBean) ConfigBean.unwrap(http.getFileCache());
        ConfigBean config2 = (ConfigBean) ConfigBean.unwrap(http);
        Map<ConfigBean, Map<String, String>> changes = new HashMap<ConfigBean, Map<String, String>>();
        Map<String, String> configChanges = new HashMap<String, String>();
        configChanges.put("max-age-seconds", "12543");
        configChanges.put("max-cache-size-bytes", "1200");
        Map<String, String> config2Changes = new HashMap<String, String>();
        config2Changes.put("version", "12351");
        changes.put(config, configChanges);
        changes.put(config2, config2Changes);

        JavaConfig javaConfig = habitat.getService(JavaConfig.class);
        ConfigBean javaConfigBean = (ConfigBean) ConfigBean.unwrap(javaConfig);
        Map<String, String> javaConfigChanges = new HashMap<String, String>();
        javaConfigChanges.put("jvm-options", "-XFooBar=false");
        changes.put(javaConfigBean, javaConfigChanges);

        getHabitat().<ConfigSupport>getService(ConfigSupport.class).apply(changes);
    }

    public boolean assertResult(String s) {
        return s.contains("max-age-seconds=\"12543\"")
            && s.contains("version=\"12351\"")
            && s.contains("-XFooBar=false");
    }
}
