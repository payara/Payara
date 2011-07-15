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

package org.glassfish.deployment.common;

import com.sun.enterprise.config.serverbeans.Application;
import com.sun.enterprise.config.serverbeans.ApplicationConfig;
import com.sun.enterprise.config.serverbeans.Engine;
import com.sun.enterprise.config.serverbeans.Module;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * During redeployment we preserve the application config information, if any,
 * that the administrator has defined for the application.  Then, during the
 * deploy part of redeployment we restore it.
 * 
 * This class encapsulates the dependencies on exactly how we store that 
 * information in the application properties in the deployment context.
 * 
 * @author tjquinn
 */
public class ApplicationConfigInfo {

    private final Map<String,Map<String,ApplicationConfig>> moduleToEngineToAppConfig;

    public ApplicationConfigInfo() {
        moduleToEngineToAppConfig = createNewMap();
    }

    public ApplicationConfigInfo(final Properties appProperties) {
        Object map =  
                appProperties.get(DeploymentProperties.APP_CONFIG);
        if (map == null) {
            moduleToEngineToAppConfig = createNewMap();
        } else {
            moduleToEngineToAppConfig = (Map<String, Map<String,ApplicationConfig>>) map;
        }
    }

    public ApplicationConfigInfo(final Application app) {

        moduleToEngineToAppConfig = createNewMap();
        if (app != null) {
            for (Module m : app.getModule()) {
                for (Engine e : m.getEngines()) {
                    put(m.getName(), e.getSniffer(), e.getApplicationConfig());
                }
            }
        }
    }

    private Map<String,Map<String,ApplicationConfig>> createNewMap() {
        return new HashMap<String,Map<String,ApplicationConfig>>();
    }
    
    public <T extends ApplicationConfig> T get(final String moduleName,
            final String engineName) {
        T result = null;
        Map<String,? extends ApplicationConfig> engineToAppConfig =
                moduleToEngineToAppConfig.get(moduleName);
        if (engineToAppConfig != null) {
            result = (T) engineToAppConfig.get(engineName);
        }
        return result;
    }

    public void  put(final String moduleName, final String engineName,
            final ApplicationConfig appConfig) {
        Map<String,ApplicationConfig> engineToAppConfig =
                moduleToEngineToAppConfig.get(moduleName);
        if (engineToAppConfig == null) {
            engineToAppConfig = new HashMap<String,ApplicationConfig>();
            moduleToEngineToAppConfig.put(moduleName, engineToAppConfig);
        }
        engineToAppConfig.put(engineName, appConfig);
    }

    public Set<String> moduleNames() {
        return moduleToEngineToAppConfig.keySet();
    }

    public Set<String> engineNames(final String moduleName) {
        return moduleToEngineToAppConfig.get(moduleName).keySet();
    }

    public void store(final Properties appProps) {
        appProps.put(DeploymentProperties.APP_CONFIG, moduleToEngineToAppConfig);
    }

}
