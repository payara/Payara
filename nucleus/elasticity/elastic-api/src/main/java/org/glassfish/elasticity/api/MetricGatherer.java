package org.glassfish.elasticity.api;

import org.jvnet.hk2.annotations.Contract;

import java.util.concurrent.TimeUnit;

/**
 * @author Mahesh.Kannan@Oracle.Com
 */
@Contract
public interface MetricGatherer {

    public String getSchedule();

    public void gatherMetric();

    public void purgeDataOlderThan(int time, TimeUnit unit);

    public void stop();

}
