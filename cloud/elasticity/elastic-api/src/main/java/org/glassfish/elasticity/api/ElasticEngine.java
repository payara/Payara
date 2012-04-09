package org.glassfish.elasticity.api;

import org.jvnet.hk2.annotations.Contract;

@Contract
public interface ElasticEngine {

    public ElasticEnvironment getElasticEnvironment(String envName);

}
