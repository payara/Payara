/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017-2018 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.jwtauth.jaxrs;

import static com.sun.enterprise.deployment.util.DOLUtils.getCurrentBundleForContext;
import static javax.ws.rs.RuntimeType.SERVER;
import static org.glassfish.internal.api.Globals.getDefaultHabitat;
import static org.glassfish.jersey.internal.spi.AutoDiscoverable.DEFAULT_PRIORITY;

import javax.annotation.Priority;
import javax.ws.rs.ConstrainedTo;
import javax.ws.rs.core.FeatureContext;

import org.glassfish.internal.deployment.Deployment;
import org.glassfish.internal.deployment.ExtendedDeploymentContext;
import org.glassfish.jersey.internal.spi.ForcedAutoDiscoverable;

import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.WebBundleDescriptor;

/**
 * This Jersey specific service provider will install the JAX-RS dynamic
 * feature which on its turn will install filters for JAX-RS resources
 * that check roles or deny all access.
 *
 * <p>
 * Note this extra service is needed, so the MP-JWT implementation code can be in
 * a server/container jar, which is typically not scanned for the <code>Provider</code>
 * annotation, which would normally cause the dynamic feature to be installed.
 *
 * @author Arjan Tijms
 */
@ConstrainedTo(SERVER)
@Priority(DEFAULT_PRIORITY)
public final class RolesAllowedAutoDiscoverable implements ForcedAutoDiscoverable {

    @Override
    public void configure(FeatureContext context) {
        
        ExtendedDeploymentContext deploymentContext = 
                getDefaultHabitat().getService(Deployment.class).
                                   getCurrentDeploymentContext();
        
        // Only register for application deployments (not the admin console)
        if (deploymentContext == null) {
            return;
        }

        boolean shouldRegister = true;

        BundleDescriptor descriptor = getCurrentBundleForContext(deploymentContext);

        if (descriptor instanceof WebBundleDescriptor) {
            shouldRegister = ((WebBundleDescriptor) descriptor).isJaxrsRolesAllowedEnabled();
        }

        if (shouldRegister && !context.getConfiguration().isRegistered(RolesAllowedDynamicFeature.class)) {
            context.register(RolesAllowedDynamicFeature.class);
        }
    }
}