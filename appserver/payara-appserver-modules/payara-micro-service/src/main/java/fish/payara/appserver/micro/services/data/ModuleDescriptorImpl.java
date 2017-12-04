/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016 Payara Foundation and/or its affiliates. All rights reserved.
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
 * file and include the License file at glassfish/legal/LICENSE.txt.
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

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map.Entry;

import org.glassfish.internal.data.ModuleInfo;

import com.sun.enterprise.web.WebApplication;

import fish.payara.micro.data.ModuleDescriptor;

/**
 *
 * @author steve
 */
public class ModuleDescriptorImpl implements ModuleDescriptor {
    
    private static final long serialVersionUID = 1L;
    
    private String name;
    private String contextRoot;
    private String type;
    private List<Entry<String, String>> servletMappings;

    public ModuleDescriptorImpl(ModuleInfo info) {
        name = info.getName();
        contextRoot = info.getModuleProps().getProperty("context-root");
        type = info.getModuleProps().getProperty("archiveType");
        
        servletMappings = info.getEngineRefs()
                              .stream()
                              .filter(e -> e.getApplicationContainer() instanceof WebApplication)
                              .map(e -> (WebApplication) e.getApplicationContainer())
                              .flatMap(e -> e.getWebModules().stream())
                              .filter(e-> e.getContextRoot().equals(contextRoot))
                              .flatMap(e -> e.getWebBundleDescriptor().getUrlPatternToServletNameMap().entrySet().stream())
                              .collect(toList());
  
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
    public List<Entry<String, String>> getServletMappings() {
        return servletMappings;
    }
    
}
