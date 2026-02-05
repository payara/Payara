/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2026] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.ai.agent.connector;

import fish.payara.ai.agent.cdi.JakartaAgenticAIExtension;
import jakarta.ai.agent.Agent;
import jakarta.enterprise.inject.spi.Extension;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.api.deployment.Deployer;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.MetaData;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.classmodel.reflect.AnnotatedElement;
import org.glassfish.hk2.classmodel.reflect.AnnotationType;
import org.glassfish.hk2.classmodel.reflect.Types;
import org.glassfish.weld.WeldDeployer;
import org.jvnet.hk2.annotations.Service;

/**
 * Deployer for Jakarta Agentic AI that processes @Agent annotated classes
 * and registers the CDI extension for workflow management.
 *
 * @author Luis Neto <luis.neto@payara.fish>
 */
@Service
@PerLookup
public class JakartaAgenticAIDeployer implements Deployer<JakartaAgenticAIContainer, JakartaAgenticAIApplicationContainer> {

    private static final Logger logger = Logger.getLogger(JakartaAgenticAIDeployer.class.getName());

    @Override
    public MetaData getMetaData() {
        return null;
    }

    @Override
    public <V> V loadMetaData(Class<V> type, DeploymentContext context) {
        return null;
    }

    @Override
    public boolean prepare(DeploymentContext context) {
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public JakartaAgenticAIApplicationContainer load(JakartaAgenticAIContainer container, DeploymentContext context) {
        Collection<Supplier<Extension>> snifferExtensions =
                context.getTransientAppMetaData(WeldDeployer.SNIFFER_EXTENSIONS, Collection.class);

        if (snifferExtensions != null) {
            Set<String> agentClassNames = new HashSet<>();

            // Find all classes annotated with @Agent
            Types types = context.getTransientAppMetaData(Types.class.getName(), Types.class);
            if (types != null) {
                types.getAllTypes().stream()
                        .filter(type -> type instanceof AnnotationType && type.getName().equals(Agent.class.getName()))
                        .findFirst()
                        .ifPresent(type -> {
                            Collection<AnnotatedElement> elements = ((AnnotationType) type).allAnnotatedTypes();
                            for (AnnotatedElement element : elements) {
                                agentClassNames.add(element.getName());
                                logger.log(Level.FINE, "Discovered @Agent class: {0}", element.getName());
                            }
                        });
            }

            if (!agentClassNames.isEmpty()) {
                String applicationName = context.getArchiveHandler()
                        .getDefaultApplicationName(context.getSource(), context);

                logger.log(Level.INFO, "Registering Jakarta Agentic AI extension for application: {0} with {1} agent(s)",
                        new Object[]{applicationName, agentClassNames.size()});

                snifferExtensions.add(() -> new JakartaAgenticAIExtension(applicationName, agentClassNames));
            }
        }

        return new JakartaAgenticAIApplicationContainer(context);
    }

    @Override
    public void unload(JakartaAgenticAIApplicationContainer appContainer, DeploymentContext context) {
        logger.log(Level.FINE, "Unloading Jakarta Agentic AI application container");
    }

    @Override
    public void clean(DeploymentContext context) {
    }
}
