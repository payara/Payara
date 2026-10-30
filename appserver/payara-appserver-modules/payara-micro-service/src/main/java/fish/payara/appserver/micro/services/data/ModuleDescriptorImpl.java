/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2017 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.appserver.micro.services.data;


import org.glassfish.internal.data.ModuleInfo;

import com.sun.enterprise.web.WebApplication;
import com.sun.enterprise.web.WebModule;

import fish.payara.micro.data.ModuleDescriptor;
import java.util.HashMap;
import java.util.Map;
import org.glassfish.internal.data.EngineRef;
import org.glassfish.api.deployment.ApplicationContainer;

/**
 *
 * @author steve
 */
public class ModuleDescriptorImpl implements ModuleDescriptor {
    
    private static final long serialVersionUID = 1L;
    
    private final String name;
    private final String contextRoot;
    private final String type;
    private final Map<String, String> servletMappings;

    public ModuleDescriptorImpl(ModuleInfo info) {
        name = info.getName();
        contextRoot = info.getModuleProps().getProperty("context-root");
        type = info.getModuleProps().getProperty("archiveType");
        servletMappings = new HashMap<>();
        for (EngineRef engineRef : info.getEngineRefs()) {
            ApplicationContainer container = engineRef.getApplicationContainer();
                if (container instanceof WebApplication) {
                    for (WebModule module : ((WebApplication) ((ApplicationContainer)container)).getWebModules()) {
                        if (module.getContextRoot().equals(contextRoot)) {
                            servletMappings.putAll(module.getWebBundleDescriptor().getUrlPatternToServletNameMap());
                       }
                    }
                }
        }  
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getContextRoot() {
        return contextRoot;
    }

    @Override
    public String getType() {
        return type;
    }
    
    @Override
    public Map<String, String> getServletMappings() {
        return servletMappings;
    }
    
}
