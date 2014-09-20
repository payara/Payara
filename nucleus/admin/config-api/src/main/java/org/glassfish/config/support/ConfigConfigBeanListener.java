/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.config.support;

import java.beans.PropertyChangeEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;

import com.sun.enterprise.config.util.ConfigApiLoggerInfo;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigBean;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigListener;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.UnprocessedChangeEvents;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.logging.LogDomains;
import static com.sun.enterprise.config.util.ConfigApiLoggerInfo.*;

/**
 * Listens for changes to the Config for the current server and adds an 
 * index for the name ServerEnvironment.DEFAULT_INSTANCE_NAME to any objects
 * that are added.
 */
@Service
@RunLevel(mode=RunLevel.RUNLEVEL_MODE_NON_VALIDATING,value=StartupRunLevel.VAL)
public final class ConfigConfigBeanListener implements ConfigListener {

    @Inject
    private ServiceLocator habitat;
    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    Config config;
    static final Logger logger = ConfigApiLoggerInfo.getLogger();


    /* force serial behavior; don't allow more than one thread to make a mess here */
    @Override
    public synchronized UnprocessedChangeEvents changed(PropertyChangeEvent[] events) {
        for (PropertyChangeEvent e : events) {
            // ignore all events for which the source isn't the Config
            if (e.getSource().getClass() != config.getClass()) {
                continue;
            }

            // remove the DEFAULT_INSTANCE_NAME entry for an old value
            Object ov = e.getOldValue();
            if (ov instanceof ConfigBeanProxy) {
                ConfigBeanProxy ovbp = (ConfigBeanProxy) ov;
                logger.log(Level.FINE, removingDefaultInstanceIndexFor,
                        ConfigSupport.getImpl(ovbp).getProxyType().getName());
                ServiceLocatorUtilities.removeFilter(habitat, BuilderHelper.createNameAndContractFilter(
                        ConfigSupport.getImpl(ovbp).getProxyType().getName(),
                        ServerEnvironment.DEFAULT_INSTANCE_NAME));
            }
            
            // add the DEFAULT_INSTANCE_NAME entry for a new value
            Object nv = e.getNewValue();
            if (nv instanceof ConfigBean) {
                ConfigBean nvb = (ConfigBean) nv;
                ConfigBeanProxy nvbp = nvb.getProxy(nvb.getProxyType());
                logger.log(Level.FINE, AddingDefaultInstanceIndexFor,
                        nvb.getProxyType().getName());
                ServiceLocatorUtilities.addOneConstant(habitat, nvbp,
                        ServerEnvironment.DEFAULT_INSTANCE_NAME,
                        nvb.getProxyType());
            }
        }
        return null;
    }
}
