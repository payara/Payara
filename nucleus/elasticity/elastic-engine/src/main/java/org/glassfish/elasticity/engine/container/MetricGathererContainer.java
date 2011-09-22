package org.glassfish.elasticity.engine.container;

import org.glassfish.api.Startup;
import org.glassfish.elasticity.api.MetricGatherer;
import org.glassfish.elasticity.engine.util.ElasticEngineThreadPool;
import org.glassfish.elasticity.engine.util.EngineUtil;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Habitat;

import javax.annotation.PostConstruct;
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
    private Habitat habitat;

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
