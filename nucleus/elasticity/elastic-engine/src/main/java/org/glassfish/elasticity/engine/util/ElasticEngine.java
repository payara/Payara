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
package org.glassfish.elasticity.engine.util;

import java.util.concurrent.TimeUnit;

import org.glassfish.api.Startup;
import org.glassfish.elasticity.api.MetricGatherer;
import org.glassfish.hk2.PostConstruct;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.config.serverbeans.AlertConfig;
import com.sun.enterprise.config.serverbeans.ElasticService;
import com.sun.enterprise.config.serverbeans.ElasticServices;
import org.jvnet.hk2.component.Habitat;

/**
 * Elastic Engine for a service. An instance of ElasticEngine keeps track
 * 	of the Alerts for a service.
 * 
 * @author Mahesh Kannan
 *
 */
@Service
public class ElasticEngine
	implements Startup, PostConstruct {

    @Inject
    Habitat habitat;

	@Inject
	ElasticServices elasticServices;
	
	@Inject
	ElasticEngineThreadPool threadPool;
	
	@Inject
	MetricGatherer[] metricHolders;
	
	private String serviceName;

	public void initialize(String serviceName) {
		this.serviceName = serviceName;
	}
	
	public void start() {
		if (serviceName == null) {
			//throw new IllegalStateException("ElasticEngine not initialized with a service name");
		}
		
		System.out.println("Elastic Services: " + elasticServices);
		for (ElasticService service : elasticServices.getElasticService()) {
			System.out.println("Got ElasticService: " + service.getName());
			System.out.println("Got Alerts: " + service.getAlerts());
			System.out.println("Got getAlerts: " + service.getAlerts().getAlert());
			for (AlertConfig alertConfig : service.getAlerts().getAlert()) {
				System.out.println("Got Altert[" + service.getName() + "]: " + alertConfig.getName());
			
				String sch = alertConfig.getSchedule().trim();
				long frequencyInSeconds = getFrequencyOfAlertExecutionInSeconds(sch);
				String alertName = alertConfig.getName();
				System.out.println("Alert[name=" + alertName + "; schedule=" + sch
						+ "; expression=" + alertConfig.getExpression() + "; will be executed every= " + frequencyInSeconds);
				ExpressionBasedAlert<AlertConfig> wrapper = new ExpressionBasedAlert<AlertConfig>();
				wrapper.initialize(habitat, alertConfig);
				threadPool.scheduleAtFixedRate(wrapper, frequencyInSeconds, frequencyInSeconds, TimeUnit.SECONDS);
			}
		}
		
		for (MetricGatherer mg : metricHolders) {
			System.out.println("Loaded " + mg);
            String sch  = mg.getSchedule();
            long frequency = 10 * 1000;
            if (sch != null) {
                sch = sch.trim();
                int index = 0;
                while (index < sch.length() && Character.isDigit(sch.charAt(index))) {
                    index++;
                }

                try {
                    if (index == sch.length()) {
                        frequency = Long.parseLong(sch);
                    } else {

                        frequency = Long.parseLong(sch.substring(0, index));
                        String unit = sch.substring(index);
                        if (unit.equals("s") || unit.equals("seconds")) {
                            frequency *= 1000;
                        }
                    }
                } catch (NumberFormatException nfEx) {
                    //TODO
                }

                threadPool.scheduleAtFixedRate(new MetricGathererWrapper(mg), frequency, frequency, TimeUnit.MILLISECONDS);
            }

		}
	}
	
	public void stop() {
		
	}
	
	private int getFrequencyOfAlertExecutionInSeconds(String sch) {
		String schStr = sch.trim();
		int index = 0;
		for (; index < schStr.length(); index++) {
			if (Character.isDigit(schStr.charAt(index))) {
				break;
			}
		}
		
		int frequencyInSeconds = 30;
		try {
			frequencyInSeconds = Integer.parseInt(schStr.substring(0, index));
		} catch (NumberFormatException nfEx) {
			//TODO
		}
		if (index < schStr.length()) {
			switch (schStr.charAt(index)) {
			case 's':
				break;
			case 'm':
				frequencyInSeconds *= 60;
				break;
			}	
		}
		
		return frequencyInSeconds;
	}
	
	private static class AlertWrapper
		implements Runnable {
		
		private ExpressionBasedAlert<AlertConfig> alert;
//		
//		AlertWrapper(ExpressionBasedAlert<Alert> alert) {
//			this.alert = alert;
//		}
		
		public void run() {
            System.out.println("AlertWrapper.run called");
			alert.execute();
		}
	}

    private class MetricGathererWrapper
        implements Runnable {

        MetricGatherer mg;

        MetricGathererWrapper(MetricGatherer mg) {
            this.mg = mg;
        }

        public void run() {
            mg.gatherMetric();
        }
    }

	public Lifecycle getLifecycle() {
		return Startup.Lifecycle.START;
	}

	public void postConstruct() {
		System.out.println("Elastic Engine started... " + metricHolders);
		start();
	}
}
