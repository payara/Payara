package org.glassfish.elasticity.metrics.jvm.memory;

import org.glassfish.elasticity.api.MetricGathererConfigurator;
import org.glassfish.elasticity.config.serverbeans.MetricGathererConfig;
import org.glassfish.hk2.PostConstruct;
import org.glassfish.paas.orchestrator.service.spi.Service;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.TransactionFailure;
import java.beans.PropertyVetoException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;

@org.jvnet.hk2.annotations.Service
public class JVMMemoryMetricGathererConfigurator
    implements MetricGathererConfigurator, PostConstruct {

    public void postConstruct() {
        System.out.println("**INSTANTIATED MetricGathererConfigurator....");
    }
    
    @Override
    public void configure(Service provisionedService, Collection<? super MetricGathererConfig> mgConfigs, Collection resolvers) {
// for now create the config object here.  Should use CTM


        try {
            JVMMetricGathererConfig mgConfig = new JVMMetricGathererConfig();
             mgConfig.setName("jvm_memory");

             mgConfigs.add(mgConfig);
        } catch (PropertyVetoException ex) {
        }
        System.out.println("created MG config");
    }

}
