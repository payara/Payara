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

import org.glassfish.elasticity.config.serverbeans.AlertConfig;
import org.glassfish.elasticity.config.serverbeans.ElasticService;
import org.glassfish.elasticity.engine.util.ElasticEngineThreadPool;
import org.glassfish.elasticity.engine.util.ExpressionBasedAlert;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.PerLookup;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Scoped(PerLookup.class)
public class ElasticServiceContainer {

    @Inject
    Habitat habitat;

    @Inject
    ElasticEngineThreadPool threadPool;

    ElasticService service;

	private String name;
	
	private AtomicBoolean enabled = new AtomicBoolean(true);
	
	private AtomicInteger minSize = new AtomicInteger();
	
	private AtomicInteger maxSize = new AtomicInteger();

    private AtomicInteger currentSize = new AtomicInteger();
	
	private AtomicInteger reconfigurationPeriodInSeconds = new AtomicInteger(3 * 60);

    private ConcurrentHashMap<String, AlertWrapper> alerts
            = new ConcurrentHashMap<String, AlertWrapper>();

    public void initialize(ElasticService service) {
        this.service = service;
        this.name = service.getName();
        this.enabled.set(service.getEnabled());
        this.minSize.set(service.getMin());
        this.maxSize.set(service.getMax());

        if (this.enabled.get() && service.getAlerts().getAlert() != null) {
            for (AlertConfig alertConfig : service.getAlerts().getAlert()) {
                addAlert(alertConfig);
            }
        }
    }
	
	public String getName() {
		return this.name;
	}

	public boolean isEnabled() {
		return enabled.get();
	}

	public int getMinimumSize() {
		return minSize.get();
	}

	public int getMaximumSize() {
		return maxSize.get();
	}

	public synchronized void setEnabled(boolean value) {
        if (this.enabled.get() == true && value == false) {
            //Request to disable this service
        } else if (this.enabled.get() == false && value == true) {
            //Request to enable
        }

		this.enabled.set(value);
	}

	public synchronized void reconfigureClusterLimits(int minSize, int maxSize) {

		this.minSize.set(minSize);
		this.maxSize.set(maxSize);

        if (currentSize.get() < minSize) {
            //Scale up
        } else if (currentSize.get() > maxSize) {
            //Scale down
        }
	}

    public void setCurrentSize(int val) {
        this.currentSize.set(val);

        if (val < minSize.get()) {
            //We need to scale up
        } else if (val > maxSize.get()) {
            //We need to scale down
        }
    }

    public void start() {
        if (service.getAlerts() != null && service.getAlerts().getAlert() != null) {
            for (AlertConfig alertConfig : service.getAlerts().getAlert()) {
                addAlert(alertConfig);
            }
        }
    }

    public void stop() {
        for (String alertName : alerts.keySet()) {
            System.out.println("Stopping alert: " + alertName);
            removeAlert(alertName);
        }
    }

    public void addAlert(AlertConfig alertConfig) {
        try {
            System.out.println("Creating Alert[" + service.getName() + "]: " + alertConfig.getName());

            String sch = alertConfig.getSchedule().trim();
            long frequencyInSeconds = getFrequencyOfAlertExecutionInSeconds(sch);
            String alertName = alertConfig.getName();
            ExpressionBasedAlert<AlertConfig> alert = new ExpressionBasedAlert<AlertConfig>();
            alert.initialize(habitat, alertConfig);
            AlertWrapper alertWrapper = new AlertWrapper(alert,
                    threadPool.scheduleAtFixedRate(alert, frequencyInSeconds, frequencyInSeconds, TimeUnit.SECONDS));
            alerts.put(alertConfig.getName(), alertWrapper);
//                System.out.println("SCHEDULED Alert[name=" + alertName + "; schedule=" + sch
//                        + "; expression=" + alertConfig.getExpression() + "; will be executed every= " + frequencyInSeconds);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void removeAlert(String alertName) {
        AlertWrapper wrapper = alerts.remove(alertName);
        if (wrapper != null && wrapper.getFuture() != null) {
            wrapper.getFuture().cancel(false);
        }
    }

    @Override
    public String toString() {
        return "ElasticServiceContainer{" +
                "name='" + name + '\'' +
                ", enabled=" + enabled +
                ", minSize=" + minSize +
                ", maxSize=" + maxSize +
                ", currentSize=" + currentSize +
                ", reconfigurationPeriodInSeconds=" + reconfigurationPeriodInSeconds +
                '}';
    }

    private static class AlertWrapper {

        private ScheduledFuture<?> future;

        private ExpressionBasedAlert<? extends AlertConfig> alert;

        AlertWrapper(ExpressionBasedAlert<? extends AlertConfig> alert, ScheduledFuture<?> future) {
            this.alert = alert;
            this.future = future;
        }

        ScheduledFuture<?> getFuture() {
            return future;
        }

        ExpressionBasedAlert<? extends AlertConfig> getAlert() {
            return alert;
        }
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

    private class ElasticServiceResizerTask
        implements Runnable {

        int size;

        Future<Boolean> done;

        public ElasticServiceResizerTask(int size) {
            this.size = size;

        }

        public void run() {
            if (size > 0) {
                //orchestrator.scale(1);
            } else if (size < 0) {
                //orchestrator.scale(-1);
            }
        }
    }
}
