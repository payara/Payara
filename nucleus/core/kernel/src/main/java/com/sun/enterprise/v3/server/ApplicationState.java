/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2019] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 * 
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
 * 
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 * 
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 * 
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */

package com.sun.enterprise.v3.server;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import org.glassfish.internal.data.EngineInfo;
import org.glassfish.internal.data.ModuleInfo;
import org.glassfish.internal.deployment.ExtendedDeploymentContext;

/**
 * The Application deployment state includes application descriptor metadata,
 * engines info and module info.
 *
 * @author Gaurav Gupta
 */
public class ApplicationState {

    private final File path;
    private String name;
    private String target;

    private ExtendedDeploymentContext deploymentContext;
    private Map<String, Object> descriptorMetadata;

    private ModuleInfo moduleInfo;
    private List<EngineInfo> engineInfos;
    private Set<String> sniffers;

    private static final String WEB_INF = "WEB-INF";
    private static final String META_INF = "META-INF";

    public ApplicationState(File path) {
        this.path = path;
    }

    public File getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public ExtendedDeploymentContext getDeploymentContext() {
        return deploymentContext;
    }

    public void setDeploymentContext(ExtendedDeploymentContext deploymentContext) {
        this.deploymentContext = deploymentContext;
    }
    
    public void storeTransientAppMetaData(ExtendedDeploymentContext deploymentContext) {
       this.descriptorMetadata = deploymentContext.getTransientAppMetadata()
                .entrySet()
                .stream()
                .filter(e -> e.getKey().startsWith(WEB_INF) || e.getKey().startsWith(META_INF))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    public Map<String, Object> getDescriptorMetadata() {
        return descriptorMetadata;
    }

    public List<EngineInfo> getEngineInfos() {
        return engineInfos;
    }

    public void setEngineInfos(List<EngineInfo> engineInfos) {
        this.engineInfos = engineInfos;
    }

    public ModuleInfo getModuleInfo() {
        return moduleInfo;
    }

    public void setModuleInfo(ModuleInfo moduleInfo) {
        this.moduleInfo = moduleInfo;
    }

    public Set<String> getSniffers() {
        return sniffers;
    }

    public void setSniffers(Set<String> sniffers) {
        if (!sniffers.equals(this.sniffers)) {
            this.sniffers = sniffers;
            this.moduleInfo = null;
            this.engineInfos = null;
        }
    }
    
    public void copyPreviousState(ExtendedDeploymentContext context) {
        ExtendedDeploymentContext previousDC = this.getDeploymentContext();
        context.getAppProps().putAll(previousDC.getAppProps());
        context.getModulePropsMap().putAll(previousDC.getModulePropsMap());
        previousDC.getModuleMetadata().forEach(context::addModuleMetaData);
        this.getDescriptorMetadata()
                .entrySet()
                .forEach(e -> context.addTransientAppMetaData(e.getKey(), e.getValue()));
        this.setDeploymentContext(context);
    }

}
