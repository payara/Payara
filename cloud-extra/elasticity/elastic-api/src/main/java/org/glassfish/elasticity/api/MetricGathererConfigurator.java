package org.glassfish.elasticity.api;

import org.glassfish.elasticity.config.serverbeans.MetricGathererConfig;
import org.glassfish.paas.orchestrator.service.spi.Service;
import org.jvnet.hk2.annotations.Contract;

import java.util.Collection;

@Contract
public interface MetricGathererConfigurator {

    public void configure(Service provisionedService,
                          Collection<? super MetricGathererConfig> mgConfigs,
                          Collection resolvers);

}
