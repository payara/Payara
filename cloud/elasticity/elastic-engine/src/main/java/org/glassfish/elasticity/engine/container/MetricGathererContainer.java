/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.api.Startup;
import org.glassfish.elasticity.api.MetricGatherer;
import org.glassfish.elasticity.engine.util.ElasticEngineThreadPool;
import org.glassfish.elasticity.engine.util.EngineUtil;
import javax.inject.Inject;

import org.glassfish.hk2.Services;
import org.jvnet.hk2.annotations.Service;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Mahesh.Kannan@Oracle.Com
 */
@Service
public class MetricGathererContainer
    implements org.glassfish.hk2.PostConstruct {

    private static final int MAX_DATA_HOLD_TIME_IN_SECONDS = 2 * 60 * 60 * 1000;

    @Inject
    private Services services;

    @Inject
    private EngineUtil engineUtil;

    @Inject
    ElasticEngineThreadPool threadPool;

    @Inject
    MetricGatherer[] metricGatherers;

    private Logger logger;

    public Startup.Lifecycle getLifecycle() {
		return Startup.Lifecycle.START;
	}

    public void postConstruct() {
        logger = engineUtil.getLogger();
    }

    public void start() {
        for (MetricGatherer mg : metricGatherers) {
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
                            ;
                        } else if (unit.equals("m") || unit.equals("minutes")) {
                            frequency *= 60;
                        }
                    }
                } catch (NumberFormatException nfEx) {
                    logger.log(Level.INFO, "Error while determining the Metric Gathering rate (" + sch, nfEx);
                }

                threadPool.scheduleAtFixedRate(new MetricGathererWrapper(mg, MAX_DATA_HOLD_TIME_IN_SECONDS),
                        frequency, frequency, TimeUnit.SECONDS);

			    EngineUtil.getLogger().log(Level.FINE, "Loaded  and started MetricGatherer " + mg.getClass().getName()
                    + " Will run every: " + frequency + " seconds");
            }

		}

    }

    public void stop() {
        for (MetricGatherer mg : metricGatherers) {
            mg.stop();
        }
    }

    private class MetricGathererWrapper
        implements Runnable {

        private MetricGatherer mg;

        private int maxDataHoldingTimeInSeconds;

        private long prevPurgeTime = System.currentTimeMillis();

        MetricGathererWrapper(MetricGatherer mg, int maxDataHoldingTimeInSeconds) {
            this.mg = mg;
            this.maxDataHoldingTimeInSeconds = maxDataHoldingTimeInSeconds;
        }

        public void run() {
            System.out.println("Starting Metric Gatherer");
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
