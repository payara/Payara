package org.glassfish.elasticity.engine.container;

import org.glassfish.api.Startup;
import org.glassfish.elasticity.api.MetricGatherer;
import org.glassfish.elasticity.engine.util.EngineUtil;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Habitat;

import javax.annotation.PostConstruct;
import java.util.logging.Logger;

/**
 * @author Mahesh.Kannan@Oracle.Com
 */
@Service
public class MetricGathererContainer
    implements org.glassfish.hk2.PostConstruct {

    @Inject
    private Habitat habitat;

    @Inject
    private EngineUtil engineUtil;

    @Inject
    MetricGatherer[] metricGatherers;

    private Logger logger;

    public Startup.Lifecycle getLifecycle() {
		return Startup.Lifecycle.START;
	}

    public void postConstruct() {
        logger = engineUtil.getLogger();
    }

    public void startMetricGatherers() {

    }
}
