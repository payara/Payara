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
package com.oracle.glassfish.elasticity.engine.util;

import java.util.concurrent.TimeUnit;

import org.glassfish.api.Startup;
import org.glassfish.hk2.PostConstruct;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;

import com.oracle.glassfish.elasticity.api.AlertConfiguration;
import com.oracle.glassfish.elasticity.api.MetricEntry;
import com.oracle.glassfish.elasticity.api.MetricHolder;
import com.sun.enterprise.config.serverbeans.Alert;
import com.sun.enterprise.config.serverbeans.Alerts;
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
	
	@Inject(name="a1")
	Alert a1;
	
	private String serviceName;

	public void initialize(String serviceName) {
		this.serviceName = serviceName;
	}
	
	public void start() {
		if (serviceName == null) {
			//throw new IllegalStateException("ElasticEngine not initialized with a service name");
		}
		
		AlertConfiguration[] configs = new AlertConfiguration[1]; //TODO Get it from domain.xml
		
		configs[0] = new DummyAlertConfiguration();
			
		
		System.out.println("Elastic Services: " + elasticServices);
		for (ElasticService service : elasticServices.getServices()) {
			System.out.println("Got ElasticService: " + service.getName());
			System.out.println("Got Alerts: " + service.getAlerts());
			System.out.println("Got getAlerts: " + service.getAlerts().getAlerts());
			System.out.println("Got getAlert a1: " + service.getAlerts().getAlert("a1"));
			System.out.println("Got getAlert a1: " + a1.getName());
			for (Alert alert : service.getAlerts().getAlerts()) {
				System.out.println("Got Altert[" + service.getName() + "]: " + alert.getName());
			}
		}
		
		for (AlertConfiguration config : configs) {
			System.out.println("Elastic Engine starting Alert... " + config);
			ExpressionBasedAlert<AlertConfiguration> alert = new ExpressionBasedAlert<AlertConfiguration>();
			alert.initialize(config);
			
			String sch = config.getSchedule();
			long frequencyInMills = 10;
			
			String alertName = config.getAlertName();
			AlertWrapper wrapper = new AlertWrapper(alert);
			threadPool.scheduleAtFixedRate(wrapper, frequencyInMills, frequencyInMills, TimeUnit.SECONDS);
		}
		
		for (MetricHolder<? extends MetricEntry> m : metricHolders) {
			System.out.println("Loaded " + m);
		}
	}
	
	public void stop() {
		
	}
	
	private static class DummyAlertConfiguration implements AlertConfiguration {
		public String getAlertName() {
			return "Alert: " + System.currentTimeMillis();
		}
		
		public String getSchedule() {
			return "10";
		}
	}
	
	private static class AlertWrapper
		implements Runnable {
		
		private ExpressionBasedAlert<AlertConfiguration> alert;
		
		AlertWrapper(ExpressionBasedAlert<AlertConfiguration> alert) {
			this.alert = alert;
		}
		
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
