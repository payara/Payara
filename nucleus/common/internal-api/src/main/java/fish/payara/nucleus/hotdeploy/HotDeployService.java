/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2020-2021] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/main/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 * 
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 * 
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 * 
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 * 
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.nucleus.hotdeploy;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import jakarta.inject.Singleton;
import org.glassfish.api.deployment.ApplicationContext;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.deployment.DeploymentContext;
import org.jvnet.hk2.annotations.Service;

/**
 * The HotDeploy service cache the application deployment state.
 * 
 * @author Gaurav Gupta
 */
@Singleton
@Service(name = "hotdeploy-service")
public class HotDeployService {

    private final Map<File, ApplicationState> applicationStates = new WeakHashMap<>();

    public boolean isApplicationStateExist(File path) {
        return applicationStates.containsKey(path);
    }

    public Optional<ApplicationState> getApplicationState(File path) {
        return Optional.ofNullable(applicationStates.get(path));
    }

    public Optional<ApplicationState> getApplicationState(boolean hotdeploy, File path) {
        if (hotdeploy) {
            return getApplicationState(path);
        } else {
            return Optional.empty();
        }
    }

    public Optional<ApplicationState> getApplicationState(ApplicationContext context) {
        if (context instanceof DeploymentContext) {
            return getApplicationState((DeploymentContext) context);
        } else {
            return Optional.empty();
        }
    }

    public Optional<ApplicationState> getApplicationState(DeploymentContext context) {
        DeployCommandParameters commandParams = context.getCommandParameters(DeployCommandParameters.class);
        boolean hotDeploy = commandParams != null? commandParams.hotDeploy : false;
        return getApplicationState(hotDeploy, context.getSourceDir());
    }

    public synchronized ApplicationState removeApplicationState(File path) {
        ApplicationState applicationState = applicationStates.remove(path);
        if(applicationState != null) {
            applicationState.preDestroy();
        }
        return applicationState;
    }

    public synchronized void addApplicationState(ApplicationState applicationState) {
        if(applicationStates.containsKey(applicationState.getPath())){
            throw new IllegalStateException("Application state already exist for " + applicationState.getName());
        }
        applicationStates.put(applicationState.getPath(), applicationState);
    }

}
