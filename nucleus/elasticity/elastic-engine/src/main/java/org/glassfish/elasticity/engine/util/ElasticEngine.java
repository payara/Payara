/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
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
import org.glassfish.elasticity.api.MetricEntry;
import org.glassfish.elasticity.api.MetricHolder;
import org.glassfish.hk2.PostConstruct;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.config.serverbeans.AlertConfig;
import com.sun.enterprise.config.serverbeans.ElasticService;
import com.sun.enterprise.config.serverbeans.ElasticServices;

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
	ElasticServices elasticServices;
	
	@Inject
	ElasticEngineThreadPool threadPool;
	
	@Inject
	MetricHolder<? extends MetricEntry>[] metricHolders;
	
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
				long frequencyInMills = getFrequencyOfAlertExecutionInMillis(sch);
				String alertName = alertConfig.getName();
				System.out.println("Alert[name=" + alertName + "; schedule=" + sch
						+ "; expression=" + alertConfig.getExpression() + "; will be executed every= " + frequencyInMills);
				ExpressionBasedAlert<AlertConfig> wrapper = new ExpressionBasedAlert<AlertConfig>();
				wrapper.initialize(alertConfig);
				threadPool.scheduleAtFixedRate(wrapper, frequencyInMills, frequencyInMills, TimeUnit.SECONDS);
			}
		}
		
		for (MetricHolder<? extends MetricEntry> m : metricHolders) {
			System.out.println("Loaded " + m);
		}
	}
	
	public void stop() {
		
	}
	
	private long getFrequencyOfAlertExecutionInMillis(String sch) {
		int index = sch.length() - 1;
		String freqStr = sch.substring(0, index);
		long frequencyInMills = 10;
		try {
			frequencyInMills = Long.parseLong(freqStr);
		} catch (NumberFormatException nfEx) {
			//TODO
		}
		char unitStr = sch.charAt(sch.length() - 1);
		switch (unitStr) {
		case 's': 
			frequencyInMills = frequencyInMills * 1000;
			break;
		case 'm':
			frequencyInMills = frequencyInMills * 1000 * 60;
			break;
		}
		
		return frequencyInMills;
	}
	
	private static class AlertWrapper
		implements Runnable {
		
		private ExpressionBasedAlert<AlertConfig> alert;
//		
//		AlertWrapper(ExpressionBasedAlert<Alert> alert) {
//			this.alert = alert;
//		}
		
		public void run() {
			alert.execute();
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
