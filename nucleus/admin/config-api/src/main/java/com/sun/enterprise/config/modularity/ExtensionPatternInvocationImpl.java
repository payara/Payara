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

import com.sun.enterprise.config.modularity.parser.ModuleConfigurationLoader;
import com.sun.enterprise.config.util.ConfigApiLoggerInfo;

import org.glassfish.api.logging.LogHelper;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigBean;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigExtensionHandler;
import org.jvnet.hk2.config.TransactionFailure;

import javax.inject.Inject;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is to integrate the whole getExtensionByType with the config modularity irrelevant of the invocation point.
 * More explanation at Config Modularity one pager and
 *
 * @author Masoud Kalali
 */
@Service(name = "basic-config-extension-handler")
public class ExtensionPatternInvocationImpl implements ConfigExtensionHandler {

    private static final Logger LOG = ConfigApiLoggerInfo.getLogger();
    
    @Inject
    ServiceLocator serviceLocator;

    @Inject
    private ModuleConfigurationLoader moduleConfigurationLoader;

    @Inject
    private ConfigModularityUtils configModularityUtils;

    @Override
    public ConfigBeanProxy handleExtension(Object owner, Class ownerType, Object[] params) {
        if (((Class) params[0]).getName().equals("com.sun.enterprise.config.serverbeans.SystemProperty")) return null;
        ConfigBeanProxy configExtension = null;
        List<ConfigBeanProxy> extensions = configModularityUtils.getExtensions(((ConfigBean) owner).createProxy(ownerType));
        for (ConfigBeanProxy extension : extensions) {
            try {
                configExtension = (ConfigBeanProxy) ((Class) params[0]).cast(extension);
                return configExtension;
            } catch (Exception e) {
                // ignore, not the right type.
            }
        }
        
        try {
            ConfigBeanProxy pr = ((ConfigBean) owner).createProxy(ownerType);
            ConfigBeanProxy returnValue = moduleConfigurationLoader.createConfigBeanForType((Class) params[0], pr);
            return returnValue;
        } catch (TransactionFailure transactionFailure) {
            LogHelper.log(LOG, Level.INFO, "Cannot get extension type {0} for {1}.", 
                    transactionFailure, new Object[]{
                    owner.getClass().getName(), ownerType.getName()});
            return null;
        } 
    }
}
