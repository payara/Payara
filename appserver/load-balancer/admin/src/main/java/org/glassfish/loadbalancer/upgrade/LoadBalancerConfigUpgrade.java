/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.loadbalancer.upgrade;

import java.beans.PropertyVetoException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.api.admin.config.ConfigurationUpgrade;
import com.sun.enterprise.config.serverbeans.Domain;
import org.glassfish.loadbalancer.config.LoadBalancer;
import org.glassfish.loadbalancer.config.LoadBalancers;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PostConstruct;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.types.Property;

import java.util.List;
import org.glassfish.loadbalancer.admin.cli.LbLogUtil;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;

import javax.inject.Inject;

/**
 *
 * Upgrade load-balancer config from v2 to v3
 * 
 * @author Kshitiz Saxena
 */
@Service(name = "loadbalancerConfigUpgrade")
public class LoadBalancerConfigUpgrade implements PostConstruct,
        ConfigurationUpgrade {

    @Inject
    Domain domain;
    public static final String DEVICE_HOST_PROPERTY = "device-host";
    public static final String DEVICE_ADMIN_PORT_PROPERTY = "device-admin-port";

    @Override
    public void postConstruct() {
        updateLoadBalancerElements();
    }

    private void updateLoadBalancerElements() {
        LoadBalancers loadBalancers = domain.getExtensionByType(LoadBalancers.class);
        if(loadBalancers == null){
            return;
        }
        
        List<LoadBalancer> loadBalancerList =
                loadBalancers.getLoadBalancer();

        for (LoadBalancer loadBalancer : loadBalancerList) {
            try {
                ConfigSupport.apply(new LoadBalancerConfigCode(), loadBalancer);
            } catch (TransactionFailure ex) {
                String msg = LbLogUtil.getStringManager().getString(
                        "ErrorDuringUpgrade", loadBalancer.getName(),
                        ex.getMessage());
                Logger.getAnonymousLogger().log(Level.SEVERE, msg);
                if (Logger.getAnonymousLogger().isLoggable(Level.FINE)) {
                    Logger.getAnonymousLogger().log(Level.FINE,
                            "Exception during upgrade operation", ex);
                }
            }
        }
    }

    private static class LoadBalancerConfigCode implements
            SingleConfigCode<LoadBalancer> {

        @Override
        public Object run(LoadBalancer loadBalancer) throws
                PropertyVetoException, TransactionFailure {
            List<Property> propertyList = loadBalancer.getProperty();
            Property deviceHostProperty = loadBalancer.getProperty(
                    DEVICE_HOST_PROPERTY);
            if (deviceHostProperty != null) {
                propertyList.remove(deviceHostProperty);
                loadBalancer.setDeviceHost(deviceHostProperty.getValue());
            } else {
                String msg = LbLogUtil.getStringManager().getString(
                        "DeviceHostNotFound", loadBalancer.getName());
                Logger.getAnonymousLogger().log(Level.SEVERE, msg);
                loadBalancer.setDeviceHost("localhost");
            }

            Property devicePortProperty = loadBalancer.getProperty(
                    DEVICE_ADMIN_PORT_PROPERTY);
            if (devicePortProperty != null) {
                propertyList.remove(devicePortProperty);
                loadBalancer.setDevicePort(devicePortProperty.getValue());
            } else {
                String msg = LbLogUtil.getStringManager().getString(
                        "DevicePortNotFound", loadBalancer.getName());
                Logger.getAnonymousLogger().log(Level.SEVERE, msg);
                loadBalancer.setDevicePort("443");
            }

            String autoApplyEnabled = loadBalancer.getAutoApplyEnabled();
            if (autoApplyEnabled != null) {
                loadBalancer.setAutoApplyEnabled(null);
                if (Boolean.parseBoolean(autoApplyEnabled)) {
                    String msg = LbLogUtil.getStringManager().getString(
                            "AutoApplyEnabled", loadBalancer.getName());
                    Logger.getAnonymousLogger().log(Level.WARNING, msg);
                }
            }
            return loadBalancer;
        }
    }
}
