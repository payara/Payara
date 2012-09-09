/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 *  or packager/legal/LICENSE.txt.  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at packager/legal/LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  Oracle designates this particular file as subject to the "Classpath"
 *  exception as provided by Oracle in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */

package com.sun.enterprise.config.modularity;


import com.sun.enterprise.config.modularity.annotation.ActivateOnStartup;
import com.sun.enterprise.config.modularity.customization.ConfigBeanDefaultValue;
import com.sun.enterprise.config.modularity.parser.ConfigurationParser;
import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.config.ConfigInjector;

import javax.inject.Inject;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Pickup any configuration from a downstream product and apply it, e.g remove the old config with a new one.
 * @author Masoud Kalali
 */
@Service
@RunLevel(mode = RunLevel.RUNLEVEL_MODE_VALIDATING, value = 2)
public class StarupConfigBeanOverrider implements PostConstruct {

    @Inject
    Habitat habitat;

    private static final Logger LOG = Logger.getLogger(StarupConfigBeanOverrider.class.getName());

    @Override
    public void postConstruct() {
        LOG.info("Starting the config overriding procedure");
        List<ActiveDescriptor<?>> descriptor = habitat.getDescriptors(BuilderHelper.createContractFilter(ConfigInjector.class.getName()));
        Class<?> clz = null;
        for (ActiveDescriptor desc : descriptor) {
            String name = desc.getName();
            if (desc.getName() == null) {
                continue;
            }
            ConfigInjector injector = habitat.getService(ConfigInjector.class, name);

            if (injector != null) {
                String clzName = injector.getClass().getName().substring(0, injector.getClass().getName().length() - 8);
                try {
                    clz = injector.getClass().getClassLoader().loadClass(clzName);
                    if (clz == null) {
                        LOG.log(Level.INFO, "Cannot find the class mapping to:  " + clzName);
                    }

                } catch (Throwable e) {
                    LOG.log(Level.INFO, "Cannot load the class due to:  " + clz.getName(), e);
                }
            }

            if (clz.isAnnotationPresent(ActivateOnStartup.class)) {
                LOG.info("Overriding Config specified by: " + clz.getName());
                applyConfig(clz);
            }
        }
        LOG.info("Finished the config overriding procedure");
    }

    private void applyConfig(Class<?> clz) {
        try {
            ConfigurationParser configurationParser = new ConfigurationParser();
            List<ConfigBeanDefaultValue> configBeanDefaultValueList = ConfigModularityUtils.getDefaultConfigurations(clz);
            configurationParser.prepareAndSetConfigBean(habitat, configBeanDefaultValueList);
        } catch (Throwable tr) {
            //Do nothing for now.
        }
    }


}

