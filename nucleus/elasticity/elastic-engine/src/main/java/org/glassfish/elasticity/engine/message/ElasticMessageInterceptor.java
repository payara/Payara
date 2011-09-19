package org.glassfish.elasticity.engine.message;

import org.jvnet.hk2.annotations.Contract;

/**
 * @author Mahesh.Kannan@Oracle.Com
 */
@Contract
public interface ElasticMessageInterceptor {

    public abstract void intercept(ElasticMessageContext ctx, ElasticMessage em);

}
