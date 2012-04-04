/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2011 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.elasticity.engine.container;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.elasticity.api.AbstractMetricGatherer;
import org.glassfish.elasticity.api.MetricGathererConfigurator;
import org.glassfish.elasticity.config.serverbeans.MetricGathererConfig;
import org.glassfish.elasticity.engine.util.ElasticEngineThreadPool;
import org.glassfish.hk2.scopes.Singleton;
import org.glassfish.paas.orchestrator.service.spi.ServiceChangeEvent;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Habitat;

import javax.inject.Inject;
import javax.inject.Provider;

@Service
@Scoped(Singleton.class)
public class ElasticServiceManager {
    
    @Inject
    Habitat habitat;

    @Inject
    ElasticEngineThreadPool threadPool;
    
    private Logger logger;

	private ConcurrentHashMap<String, ElasticServiceContainer> _containers
		= new ConcurrentHashMap<String, ElasticServiceContainer>();

	public ElasticServiceContainer getElasticServiceContainer(String name) {
		return _containers.get(name);
	}

	public void addElasticServiceContainer(String name, ElasticServiceContainer service) {
		_containers.put(name, service);
	}
	
	public void removeElasticServiceContainer(String name) {
		_containers.remove(name);
	}

    public Collection<ElasticServiceContainer> containers() {
        return _containers.values();
    }

    public void onEvent(ServiceChangeEvent event) {
        switch (event.getType()) {
            case CREATED:
                List<MetricGathererConfig> mgConfigs = new LinkedList<MetricGathererConfig>();
                List resolvers = new LinkedList();
                Collection<MetricGathererConfigurator> configurators =
                        habitat.getAllByContract(MetricGathererConfigurator.class);
                for (MetricGathererConfigurator configurator : configurators) {
                    configurator.configure(event.getNewValue(), mgConfigs, resolvers);
                }

                //We now have the MetricGathererConfigs and Resolvers for this Service
                System.out.println("**Need to Initialized Service: " + event.getNewValue().getName()
                    + " with  " + configurators.size() + " configurators and "
                        + resolvers.size() + " resolvers ");

                ElasticServiceContainer serviceContainer =
                        habitat.forContract(ElasticServiceContainer.class).get();
                for (MetricGathererConfig cfg : mgConfigs) {
                    AbstractMetricGatherer mg = habitat.forContract(AbstractMetricGatherer.class).named(cfg.getName()).get();
                    mg.initialize(event.getNewValue(), cfg);

                    threadPool.scheduleAtFixedRate(new MetricGathererWrapper(mg, 300),
                            10, 10, TimeUnit.SECONDS);

                }

        }
    }
    private class MetricGathererWrapper
            implements Runnable {

        private AbstractMetricGatherer mg;

        private int maxDataHoldingTimeInSeconds;

        private long prevPurgeTime = System.currentTimeMillis();
        
        private Logger logger;

        MetricGathererWrapper(AbstractMetricGatherer mg, int maxDataHoldingTimeInSeconds) {
            this.mg = mg;
            this.maxDataHoldingTimeInSeconds = maxDataHoldingTimeInSeconds;

            logger = Logger.getLogger(MetricGathererWrapper.class.getName());
        }

        public void run() {
            logger.log(Level.INFO, "Gathering data for metric...");
            mg.gatherMetric();

            long now = System.currentTimeMillis();
            if (((now - prevPurgeTime) / 1000) > maxDataHoldingTimeInSeconds) {
                prevPurgeTime = now;
                logger.log(Level.INFO, "Purging data for MetricGatherer: " + mg.getClass().getName());
                mg.purgeDataOlderThan(maxDataHoldingTimeInSeconds, TimeUnit.SECONDS);
            }
        }
    }
	
}
