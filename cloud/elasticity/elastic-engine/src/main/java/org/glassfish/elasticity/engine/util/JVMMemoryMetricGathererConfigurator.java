package org.glassfish.elasticity.engine.util;

import org.glassfish.elasticity.api.MetricGathererConfigurator;
import org.glassfish.elasticity.config.serverbeans.MetricGathererConfig;
import org.glassfish.paas.orchestrator.service.spi.Service;

import java.util.Collection;

@org.jvnet.hk2.annotations.Service
public class JVMMemoryMetricGathererConfigurator
    implements MetricGathererConfigurator {

    @Override
    public void configure(Service provisionedService, Collection<? super MetricGathererConfig> mgConfigs, Collection resolvers) {
        //TODO: Create a JVMMemoryMetricConfig and add to mgConfigs
    }
}
