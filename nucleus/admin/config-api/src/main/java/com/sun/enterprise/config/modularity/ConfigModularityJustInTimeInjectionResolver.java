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

package com.sun.enterprise.config.modularity;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.DomainExtension;
import org.glassfish.api.admin.config.ConfigExtension;
import org.glassfish.hk2.api.DynamicConfigurationService;
import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.JustInTimeInjectionResolver;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.server.ServerEnvironmentImpl;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigBeanProxy;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * create the default ejb-container configbean when non exists and an injection point requires it.
 *
 * @author Masoud Kalali
 */
@Singleton
@Service
public class ConfigModularityJustInTimeInjectionResolver implements JustInTimeInjectionResolver {

    @Inject
    private DynamicConfigurationService dcs;
    @Inject
    private ServiceLocator locator;
    @Inject
    private Config config;
    @Inject
    private Domain domain;

    @Override
    public boolean justInTimeResolution(Injectee injectee) {
        if (injectee == null || injectee.isOptional()) return false;
        Class configBeanType;
        try {
            configBeanType = (Class) injectee.getRequiredType();
        } catch (Exception ex) {
            return false;
        }
        if (!ConfigExtension.class.isAssignableFrom(configBeanType) && !DomainExtension.class.isAssignableFrom(configBeanType)) {
            return false;
        }
        if (!isInjectionSupported(configBeanType)) return false;

        if (domain == null) {
            return false;
        }
        if (ConfigExtension.class.isAssignableFrom(configBeanType)) {
            if (config == null) {
                config = locator.getService(Config.class, ServerEnvironmentImpl.DEFAULT_INSTANCE_NAME);
            }
            ConfigBeanProxy pr = config.getExtensionByType(configBeanType);
            return pr != null;

        } else if (DomainExtension.class.isAssignableFrom(configBeanType)) {
            ConfigBeanProxy pr = domain.getExtensionByType(configBeanType);
            return pr != null;
        }
        return false;

    }

    //Let's check if we support automatic creation of this type or not.
    //This method will go away eventually when we are done with supporting all types.
    private boolean isInjectionSupported(Class c) {
        return true;
    }
}
