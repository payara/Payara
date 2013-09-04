/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.config.modularity.parser;

import com.sun.enterprise.config.modularity.ConfigModularityUtils;
import com.sun.enterprise.config.modularity.RankedConfigBeanProxy;
import com.sun.enterprise.config.modularity.annotation.HasNoDefaultConfiguration;
import com.sun.enterprise.config.modularity.customization.ConfigBeanDefaultValue;
import com.sun.enterprise.config.util.ConfigApiLoggerInfo;
import com.sun.enterprise.module.bootstrap.StartupContext;

import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.admin.config.ConfigExtension;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.Dom;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import javax.inject.Inject;

import java.beans.PropertyVetoException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Containing shared functionalists between different derived classes like ConfigSnippetLoader and so on.
 * Shared functionalists includes finding, loading the configuration and creating a ConFigBean from it.
 *
 * @author Masoud Kalali
 */
@Service
public class ModuleConfigurationLoader<C extends ConfigBeanProxy, U extends ConfigBeanProxy> {
    
    @Inject
    private ServiceLocator serviceLocator;

    @Inject
    private ConfigurationParser configurationParser;

    @Inject
    private ConfigModularityUtils configModularityUtils;

    private C extensionOwner;

    public <U extends ConfigBeanProxy> U createConfigBeanForType(Class<U> configExtensionType, C extensionOwner) throws TransactionFailure {
        this.extensionOwner = extensionOwner;
        if (configModularityUtils.hasCustomConfig(configExtensionType)) {
            addConfigBeanFor(configExtensionType);
        } else {
            if (configExtensionType.getAnnotation(HasNoDefaultConfiguration.class) != null) {
                return null;
            }
            final Class<U> childElement = configExtensionType;
            synchronized (configModularityUtils) {
                boolean oldIP = configModularityUtils.isIgnorePersisting();
                try {
                    configModularityUtils.setIgnorePersisting(true);
                    ConfigSupport.apply(new SingleConfigCode<ConfigBeanProxy>() {
                        @Override
                        public Object run(ConfigBeanProxy parent) throws PropertyVetoException, TransactionFailure {
                            U child = parent.createChild(childElement);
                            Dom unwrappedChild = Dom.unwrap(child);
                            unwrappedChild.addDefaultChildren();
                            configModularityUtils.getExtensions(parent).add(child);
                            return child;
                        }
                    }, extensionOwner);
                } finally {
                    configModularityUtils.setIgnorePersisting(oldIP);
                }
            }
        }

        return getExtension(configExtensionType, extensionOwner);
    }

    private <U extends ConfigBeanProxy> U getExtension(Class<U> configExtensionType, C extensionOwner) {
        List<U> extensions = configModularityUtils.getExtensions(extensionOwner);
        for (ConfigBeanProxy extension : extensions) {
            try {
                U configBeanInstance = configExtensionType.cast(extension);
                if (configBeanInstance instanceof ConfigExtension) {
                    ServiceLocatorUtilities.addOneDescriptor(serviceLocator,
                            BuilderHelper.createConstantDescriptor(configBeanInstance, ServerEnvironment.DEFAULT_INSTANCE_NAME,
                                    ConfigSupport.getImpl(configBeanInstance).getProxyType()));
                }
                return configBeanInstance;
            } catch (Exception e) {
                // ignore, not the right type.
            }
        }
        return null;
    }


    protected <U extends ConfigBeanProxy> void addConfigBeanFor(Class<U> extensionType) {
        if (!RankedConfigBeanProxy.class.isAssignableFrom(extensionType)) {
            if (getExtension(extensionType, extensionOwner) != null) {
                return;
            }
        }
        StartupContext context = serviceLocator.getService(StartupContext.class);
        List<ConfigBeanDefaultValue> configBeanDefaultValueList =
                configModularityUtils.getDefaultConfigurations(extensionType, configModularityUtils.getRuntimeTypePrefix(context));
        configurationParser.parseAndSetConfigBean(configBeanDefaultValueList);
    }


}
