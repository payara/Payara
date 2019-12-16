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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import org.glassfish.api.container.Sniffer;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.Events;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.EngineInfo;
import org.glassfish.internal.data.ModuleInfo;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.internal.deployment.ExtendedDeploymentContext;

/**
 * The Application deployment state includes application descriptor metadata,
 * engines info and module info.
 *
 * @author Gaurav Gupta
 */
public class ApplicationState {

    private final File path;
    private final String name;
    private String target;

    private List<ClassLoader> previousClassLoaders;

    private ExtendedDeploymentContext deploymentContext;
    private ApplicationInfo applicationInfo;
    private ModuleInfo moduleInfo;
    private List<EngineInfo> engineInfos;
    private Set<String> sniffers;
    private Map<String, Object> descriptorMetadata = Collections.emptyMap();
    private Map<String, Object> modulesMetaData = Collections.emptyMap();
    private Set<String> classesChanged = Collections.emptySet();

    private boolean active;

    private static final String WEB_INF = "WEB-INF";
    private static final String META_INF = "META-INF";
    private static final String CLASS_EXT = ".class";
    private static final String WEB_INF_CLASSES = "WEB-INF/classes";

    public ApplicationState(String name, File path, ExtendedDeploymentContext deploymentContext) {
        this.name = name;
        this.path = path;
        this.deploymentContext = deploymentContext;
    }

    public File getPath() {
        return path;
    }

    public String getName() {
        return name;
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

    public ApplicationInfo getApplicationInfo() {
        return applicationInfo;
    }

    public void setApplicationInfo(ApplicationInfo applicationInfo) {
        this.applicationInfo = applicationInfo;
    }

    public Set<String> getSniffers() {
        return sniffers;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isInactive() {
        return !active;
    }

    public void setSniffers(Collection<? extends Sniffer> sniffers) {
        final Set<String> snifferTypes = sniffers.stream()
                .map(s -> Sniffer.class.cast(s))
                .map(Sniffer::getModuleType)
                .collect(toSet());
        if (!snifferTypes.equals(this.sniffers)) {
            this.sniffers = snifferTypes;
            this.moduleInfo = null;
            this.engineInfos = null;
        }
    }

    public void start(ExtendedDeploymentContext newContext, Events events) {
        validateInactiveState();
        this.active = true;

        newContext.getAppProps().putAll(this.deploymentContext.getAppProps());
        newContext.getModulePropsMap().putAll(this.deploymentContext.getModulePropsMap());
        this.getDescriptorMetadata()
                .entrySet()
                .forEach(e -> newContext.addTransientAppMetaData(e.getKey(), e.getValue()));
        this.deploymentContext = newContext;
        this.previousClassLoaders = getClassLoaders(this.applicationInfo);
        final DeployCommandParameters commandParams = newContext.getCommandParameters(DeployCommandParameters.class);
        if (commandParams.sourcesChanged != null) {
            this.classesChanged = new HashSet<>();
            for (String sourcePath : commandParams.sourcesChanged) {
                String className = getClassName(sourcePath);
                if (className != null) {
                    this.classesChanged.add(className);
                }
            }
        }
        // unload previous app
        events.send(
                new EventListener.Event<ApplicationInfo>(
                        Deployment.APPLICATION_UNLOADED,
                        this.applicationInfo
                ),
                false
        );
    }

    public boolean isClasschanged(Class clazz) {
        validateActiveState();
        return this.classesChanged.contains(clazz.getName());
    }

    public void storeMetaData(ExtendedDeploymentContext deploymentContext) {
        this.modulesMetaData = deploymentContext.getModuleMetadata()
                .stream()
                .collect(toMap(metaData -> metaData.getClass().getName(), Function.identity()));
        this.descriptorMetadata = deploymentContext.getTransientAppMetadata()
                .entrySet()
                .stream()
                .filter(e -> e.getKey().startsWith(WEB_INF) || e.getKey().startsWith(META_INF))
                .collect(toMap(Entry::getKey, Entry::getValue));
    }

    public <T> T getModuleMetaData(Class<T> metadataType) {
        Object moduleMetaData = modulesMetaData.get(metadataType.getName());
        if (moduleMetaData != null) {
            return metadataType.cast(moduleMetaData);
        } else {
            for (Object metadata : modulesMetaData.values()) {
                try {
                    return metadataType.cast(metadata);
                } catch (ClassCastException e) {
                }
            }
            return null;
        }
    }

    public void close() {
        this.active = false;
        this.classesChanged.clear();

        if (previousClassLoaders != null) {
            for (ClassLoader cloader : previousClassLoaders) {
                try {
                    PreDestroy.class.cast(cloader).preDestroy();
                } catch (Exception e) {
                    // ignore, the class loader does not need to be 
                    // explicitely stopped or already stopped
                }
            }
            previousClassLoaders.clear();
        }
    }

    private void validateActiveState() {
        if (!active) {
            throw new IllegalStateException(String.format("Application [%s] state must be active.", name));
        }
    }

    private void validateInactiveState() {
        if (active) {
            throw new IllegalStateException(String.format("Application [%s] state must not be active.", name));
        }
    }

    private String getClassName(String sourcePath) {
        String className = null;
        if (sourcePath.endsWith(CLASS_EXT)) {
            int startIndex = 0;
            int endIndex = sourcePath.length() - CLASS_EXT.length();
            if (sourcePath.startsWith(WEB_INF_CLASSES)) {
                startIndex = WEB_INF_CLASSES.length() + 1;
            }
            className = sourcePath
                    .substring(startIndex, endIndex)
                    .replace('\\', '.')
                    .replace('/', '.');
        }
        return className;
    }

    private List<ClassLoader> getClassLoaders(ApplicationInfo appInfo) {
        List<ClassLoader> classLoaders = new ArrayList<>(appInfo.getClassLoaders());
        classLoaders.add(appInfo.getModuleClassLoader());
        classLoaders.add(appInfo.getAppClassLoader());

        for (ModuleInfo module : appInfo.getModuleInfos()) {
            classLoaders.addAll(module.getClassLoaders());
            classLoaders.add(module.getModuleClassLoader());
        }
        return classLoaders;
    }

}
