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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.elasticity.api.AbstractAlert;
import org.glassfish.elasticity.api.AbstractMetricGatherer;
import org.glassfish.elasticity.api.AlertConfigurator;
import org.glassfish.elasticity.config.serverbeans.AlertConfig;
import org.glassfish.elasticity.engine.util.ElasticEngineThreadPool;
import org.glassfish.elasticity.engine.util.EngineUtil;
import org.glassfish.hk2.Services;
import org.glassfish.hk2.scopes.PerLookup;
import org.glassfish.hk2.scopes.Singleton;
import org.glassfish.paas.orchestrator.service.spi.ServiceChangeEvent;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;

@Service
@Scoped(PerLookup.class)
public class ElasticEnvironmentContainer {
    
    @Inject
    Services services;

    @Inject
    ElasticEngineThreadPool threadPool;
    
    private static final Logger _logger = EngineUtil.getLogger();
    
    private String envName;

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

    public void onEvent(String envName, ServiceChangeEvent event) {
        ElasticServiceContainer serviceContainer = null;
        switch (event.getType()) {
            case CREATED:
                this.envName = envName;
                serviceContainer = services.forContract(ElasticServiceContainer.class).get();
                addElasticServiceContainer(event.getNewValue().getName(), serviceContainer);
                serviceContainer.start(event.getNewValue());


                AlertConfigurator alertConfigurator = services.forContract(AlertConfigurator.class).get();
                AlertConfig cfg = alertConfigurator.getAlertConfig(event.getNewValue());
                AbstractAlert<? extends AlertConfig> alert =
                        services.forContract(AbstractAlert.class).named(cfg.getType()).get();
                int freqInSeconds = EngineUtil.getFrequencyOfAlertExecutionInSeconds(cfg.getSchedule());
                threadPool.scheduleAtFixedRate(alert, freqInSeconds, freqInSeconds, TimeUnit.SECONDS);
                break;

            case MODIFIED:
                System.out.println("**(Service MODIFIED) ElasticEnvironmentContainer got Event: " + event.getType());
                serviceContainer = _containers.get(event.getOldValue().getName());
                break;

            case DELETED:
                removeElasticServiceContainer(event.getNewValue().getName());
                break;

            default: 
                System.out.println("**ElasticEnvironmentContainer got Event: " + event.getType());
                break;
        }
    }

}
