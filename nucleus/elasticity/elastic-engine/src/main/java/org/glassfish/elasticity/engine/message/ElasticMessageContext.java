package org.glassfish.elasticity.engine.message;

import org.glassfish.elasticity.engine.container.ElasticServiceContainer;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Mahesh.Kannan@Oracle.Com
 */
public class ElasticMessageContext {

    private int index = -1;

    private ElasticServiceContainer container;

    private ElasticMessageInterceptor[] interceptors;

    private ElasticMessage em;

    private Map ctxMap;

    public ElasticMessageContext(ElasticMessageInterceptor[] interceptors, ElasticServiceContainer container) {
        this.interceptors = interceptors;
        this.container = container;
        index = -1;
    }

    public void transmit(ElasticMessage em) {
        this.em = em;
        proceed(em);
    }

    public void proceed(ElasticMessage em) {
        index++;
        if (index < interceptors.length) {
            interceptors[index].intercept(this, em);
        }
        index--;
    }

    public ElasticServiceContainer getContainer() {
        return container;
    }

    public Map getCtxMap() {
        return ctxMap;
    }

    private Map getContextMap() {
        if (ctxMap == null) {
            ctxMap = new HashMap();
        }

        return ctxMap;
    }
}
