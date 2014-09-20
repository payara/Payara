/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.config.modularity;

import com.sun.enterprise.config.modularity.annotation.CustomConfiguration;
import com.sun.enterprise.config.modularity.customization.ConfigBeanDefaultValue;
import com.sun.enterprise.config.modularity.parser.ConfigurationParser;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.DomainExtension;
import com.sun.enterprise.module.bootstrap.StartupContext;

import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.admin.config.ConfigExtension;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.inject.Named;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * @author Masoud Kalali
 */
@Service
public class ConfigBeanInstaller implements PostConstruct {

    @Inject
    ServiceLocator serviceLocator;

    @Inject
    StartupContext startupContext;

    @Inject
    private ConfigurationParser configurationParser;

    @Inject
    private ConfigModularityUtils configModularityUtils;

    @Inject
    private Domain domain;

    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    private Config config;

    @Override
    public void postConstruct() {
        Class cbc = this.getClass().getDeclaringClass();
        if (cbc != null) {
            Annotation ann = cbc.getAnnotation(CustomConfiguration.class);
            if (ann != null) {
                applyConfigIfNeeded(cbc);
            }
        }
    }


    private void applyConfigIfNeeded(Class clz) {
        //TODO find a way to get the parent and do complete check for all config beans type rather than just these two
        if (!RankedConfigBeanProxy.class.isAssignableFrom(clz)) {
            if (DomainExtension.class.isAssignableFrom(clz) && (domain.getExtensionByType(clz) != null)) {
                return;
            }
            if (ConfigExtension.class.isAssignableFrom(clz) && (config.getExtensionByType(clz) != null)) {
                return;
            }
        }

        List<ConfigBeanDefaultValue> configBeanDefaultValueList =
                configModularityUtils.getDefaultConfigurations(clz, configModularityUtils.getRuntimeTypePrefix(startupContext));
        configurationParser.parseAndSetConfigBean(configBeanDefaultValueList);
    }
}
