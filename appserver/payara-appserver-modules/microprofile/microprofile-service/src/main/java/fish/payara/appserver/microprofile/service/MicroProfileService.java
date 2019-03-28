/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2019] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.appserver.microprofile.service;

import com.sun.enterprise.config.serverbeans.Domain;
import fish.payara.appserver.microprofile.service.adapter.MicroProfileAdapter;
import fish.payara.appserver.microprofile.service.configuration.MicroProfileConfiguration;
import javax.inject.Inject;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.server.ServerEnvironmentImpl;

/**
 * The core service for the MicroProfile application. Handles starting and
 * reconfiguration of the application.
 *
 * @author Andrew Pielage
 * @author Gaurav Gupta
 */
public abstract class MicroProfileService {

    public final static String DEFAULT_MICROPROFILE_APP_NAME = "__microprofile";
    
    public final static String DEFAULT_GROUP_NAME = "microprofile";
    
    

    private boolean startAttempted = false;

    @Inject
    Domain domain;

    @Inject
    protected ServerEnvironmentImpl serverEnv;

    @Inject
    protected ServiceLocator serviceLocator;

    protected abstract MicroProfileConfiguration getMicroProfileConfiguration();

    protected abstract MicroProfileAdapter getMicroProfileAdapter();

    public void loadApplication() {
        startAttempted = true;
        try {
            new MicroProfileLoader(
                    getMicroProfileAdapter(), serviceLocator, domain, serverEnv,
                    getMicroProfileConfiguration().getEndpoint(),
                    getMicroProfileConfiguration().getApplicationName(),
                    getMicroProfileAdapter().getVirtualServers()
            ).start();
        } catch (Exception ex) {
            throw new RuntimeException("Unable to load microprofile!", ex);
        }
    }

    protected void loadApplication(boolean dynamicStart) {
        startAttempted = true;
        try {
            new MicroProfileLoader(
                    getMicroProfileAdapter(), serviceLocator, domain, serverEnv,
                    getMicroProfileConfiguration().getEndpoint(),
                    getMicroProfileConfiguration().getApplicationName(),
                    getMicroProfileAdapter().getVirtualServers(),
                    dynamicStart
            ).start();
        } catch (Exception ex) {
            throw new RuntimeException("Unable to load microprofile!", ex);
        }
    }

    public boolean isStartAttempted() {
        return startAttempted;
    }

}
