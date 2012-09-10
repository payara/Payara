/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.ejb.upgrade;

import java.beans.PropertyVetoException;
import java.util.List;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Configs;
import org.glassfish.api.admin.config.ConfigurationUpgrade;
import com.sun.ejb.containers.EjbContainerUtil;
import org.glassfish.ejb.config.EjbContainer;
import org.glassfish.ejb.config.EjbTimerService;

import javax.inject.Inject;
import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PostConstruct;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.types.Property;

/**
 * Upgrade EJB Timer Service table from v2 to v3
 * @author Marina Vatkina
 */

@Service(name="ejbTimerServiceUpgrade")
public class EJBTimerServiceUpgrade implements PostConstruct, ConfigurationUpgrade {

    @Inject
    Configs configs;

    public void postConstruct() {
        for (Config config : configs.getConfig()) {
            EjbContainer container = config.getExtensionByType(EjbContainer.class);
            if (container != null && container.getEjbTimerService() != null) {
                doUpgrade(container.getEjbTimerService());
            }
        }
    }

    private void doUpgrade(EjbTimerService ts) {
        String value = ts.getMinimumDeliveryIntervalInMillis();
        if (value == null || "7000".equals(value)) {
            value = "" + EjbContainerUtil.MINIMUM_TIMER_DELIVERY_INTERVAL;
        }

        List<Property> properties = ts.getProperty();
        if (properties != null) {
            for (Property p : properties) {
                if (p.getName().equals(EjbContainerUtil.TIMER_SERVICE_UPGRADED)) {
                    return; // Already set
                }
            }
        }
        try {
            final String minDelivery = value;
            ConfigSupport.apply(new SingleConfigCode<EjbTimerService>() {

                public Object run(EjbTimerService ts) throws PropertyVetoException, TransactionFailure {
                    Property prop = ts.createChild(Property.class);
                    ts.getProperty().add(prop);
                    prop.setName(EjbContainerUtil.TIMER_SERVICE_UPGRADED);
                    prop.setValue("false");
                    ts.setMinimumDeliveryIntervalInMillis(minDelivery);
                    return null;
                }
            }, ts);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
