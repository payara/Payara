package org.glassfish.elasticity.api;

import org.jvnet.hk2.annotations.Contract;

/**
 * @author Mahesh.Kannan@Oracle.Com
 */
@Contract
public interface MetricGatherer {

    public String getSchedule();

    public void gatherMetric();

    public void stop();

}
