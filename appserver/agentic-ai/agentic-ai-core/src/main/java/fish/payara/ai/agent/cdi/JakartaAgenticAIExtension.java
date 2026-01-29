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
package fish.payara.ai.agent.cdi;

import fish.payara.ai.agent.runtime.AgentMetadata;
import jakarta.ai.agent.Agent;
import jakarta.ai.agent.WorkflowScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.WithAnnotations;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CDI Extension for Jakarta Agentic AI that:
 * <ul>
 *     <li>Registers the @WorkflowScoped custom context</li>
 *     <li>Processes @Agent annotated classes</li>
 *     <li>Builds metadata for workflow orchestration</li>
 * </ul>
 *
 * @author Luis Neto <luis.neto@payara.fish>
 */
public class JakartaAgenticAIExtension implements Extension {

    private static final Logger logger = Logger.getLogger(JakartaAgenticAIExtension.class.getName());

    private final String applicationName;
    private final Set<String> agentClassNames;
    private final Map<Class<?>, AgentMetadata> agentMetadataMap = new ConcurrentHashMap<>();

    /**
     * Default constructor for CDI.
     */
    public JakartaAgenticAIExtension() {
        this.applicationName = null;
        this.agentClassNames = new HashSet<>();
    }

    /**
     * Constructor used by the deployer to pass discovered agent classes.
     *
     * @param applicationName the name of the application being deployed
     * @param agentClassNames the set of class names annotated with @Agent
     */
    public JakartaAgenticAIExtension(String applicationName, Set<String> agentClassNames) {
        this.applicationName = applicationName;
        this.agentClassNames = agentClassNames;
        logger.log(Level.INFO, "Jakarta Agentic AI Extension initialized for application: {0}", applicationName);
    }

    /**
     * Register the @WorkflowScoped scope before bean discovery.
     */
    void beforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd) {
        logger.log(Level.FINE, "Registering @WorkflowScoped scope");
        bbd.addScope(WorkflowScoped.class, true, false);
    }

    /**
     * Process @Agent annotated types and build metadata.
     */
    <T> void processAnnotatedType(@Observes @WithAnnotations(Agent.class) ProcessAnnotatedType<T> pat, BeanManager beanManager) {
        AnnotatedType<T> annotatedType = pat.getAnnotatedType();
        Class<T> javaClass = annotatedType.getJavaClass();

        Agent agentAnnotation = javaClass.getAnnotation(Agent.class);
        if (agentAnnotation != null) {
            logger.log(Level.INFO, "Processing @Agent class: {0}", javaClass.getName());

            // Build metadata for this agent
            AgentMetadata metadata = AgentMetadata.fromClass(javaClass);
            agentMetadataMap.put(javaClass, metadata);

            logger.log(Level.FINE, "Agent metadata: name={0}, triggerMethods={1}, decisionMethods={2}, actionMethods={3}",
                    new Object[]{
                            metadata.getAgentName(),
                            metadata.getTriggerMethods().size(),
                            metadata.getDecisionMethods().size(),
                            metadata.getActionMethods().size()
                    });
        }
    }

    /**
     * Register the WorkflowScopeContext after bean discovery.
     */
    void afterBeanDiscovery(@Observes AfterBeanDiscovery abd, BeanManager beanManager) {
        logger.log(Level.INFO, "Registering WorkflowScopeContext for Jakarta Agentic AI");

        // Register the custom context for @WorkflowScoped
        abd.addContext(new WorkflowScopeContext());

        // Log summary
        logger.log(Level.INFO, "Jakarta Agentic AI initialized with {0} agent(s)", agentMetadataMap.size());
    }

    /**
     * Gets the application name this extension was initialized for.
     *
     * @return the application name
     */
    public String getApplicationName() {
        return applicationName;
    }

    /**
     * Gets metadata for a specific agent class.
     *
     * @param agentClass the agent class
     * @return the metadata, or null if not found
     */
    public AgentMetadata getAgentMetadata(Class<?> agentClass) {
        return agentMetadataMap.get(agentClass);
    }

    /**
     * Gets all registered agent metadata.
     *
     * @return unmodifiable map of agent class to metadata
     */
    public Map<Class<?>, AgentMetadata> getAllAgentMetadata() {
        return Map.copyOf(agentMetadataMap);
    }
}
