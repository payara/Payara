/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.web.ha;

import com.sun.enterprise.web.PEWebContainerFeatureFactoryImpl;
import com.sun.enterprise.web.PESSOFactory;
import com.sun.enterprise.web.ServerConfigLookup;
import com.sun.enterprise.web.SSOFactory;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.web.ha.authenticator.HASSOFactory;

import javax.inject.Inject;
import org.jvnet.hk2.annotations.Service;

/**
 * Implementation of WebContainerFeatureFactory which returns web container
 * feature implementations for HA.
 *
 * @author Shing Wai Chan
 */
@Service(name="ha")
public class HAWebContainerFeatureFactoryImpl extends PEWebContainerFeatureFactoryImpl {
    @Inject
    private ServiceLocator services;

    @Inject
    private ServerConfigLookup serverConfigLookup;

    @Override
    public SSOFactory getSSOFactory() {
        if (isSsoFailoverEnabled()) {
            return services.getService(HASSOFactory.class);
        } else {
            return new PESSOFactory();
        }
    }

    /**
     * check sso-failover-enabled in web-container-availability
     * @return return true only if the value of sso-failover-enabled is "true"
     * and availability-enabled in web-container-availability is "true"
     * otherwise, return false.
     */
    private boolean isSsoFailoverEnabled() {
        boolean webContainerAvailabilityEnabled =
            serverConfigLookup.calculateWebAvailabilityEnabledFromConfig();
        boolean isSsoFailoverEnabled =
            serverConfigLookup.isSsoFailoverEnabledFromConfig();
        return isSsoFailoverEnabled && webContainerAvailabilityEnabled;
    }
}
