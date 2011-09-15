package org.glassfish.elasticity.engine.container;

import org.glassfish.elasticity.api.Alert;
import org.glassfish.elasticity.api.AlertContext;
import org.glassfish.elasticity.config.serverbeans.AlertConfig;
import org.glassfish.elasticity.config.serverbeans.ElasticService;

import java.util.concurrent.ScheduledFuture;

/**
 * @author Mahesh.Kannan@Oracle.Com
 */
public class AlertContextImpl<C extends AlertConfig>
    implements AlertContext<C>, Runnable {

    private ElasticService elasticService;

    private C config;

    private Alert alert;

    private ScheduledFuture<?> future;

    public AlertContextImpl(ElasticService elasticService, C config, Alert alert) {
        this.elasticService = elasticService;
        this.config = config;
        this.alert = alert;
    }

    public ElasticService getElasticService() {
        return elasticService;
    }

    public C getAlertConfig() {
        return config;
    }

    public Alert getAlert() {
        return alert;
    }

    public ScheduledFuture<?> getFuture() {
        return future;
    }

    public void setFuture(ScheduledFuture<?> future) {
        this.future = future;
    }

    public void run() {
        alert.execute(this);
    }
}
