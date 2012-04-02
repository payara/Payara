package org.glassfish.elasticity.engine.container;

import org.glassfish.elasticity.api.Alert;
import org.glassfish.elasticity.api.AlertContext;
import org.glassfish.elasticity.config.serverbeans.AlertAction;
import org.glassfish.elasticity.config.serverbeans.AlertConfig;
import org.glassfish.elasticity.config.serverbeans.ElasticServiceConfig;
import org.glassfish.elasticity.engine.util.EngineUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;

/**
 * @author Mahesh.Kannan@Oracle.Com
 */
public class AlertContextImpl<C extends AlertConfig>
        implements AlertContext<C>, Runnable {

    private ElasticServiceContainer elasticServiceContainer;

    private C config;

    private Alert alert;

    private ScheduledFuture<?> future;

    private Map transientData = new HashMap();

    private String alertToken;

    public AlertContextImpl(ElasticServiceContainer elasticServiceContainer, C config, Alert alert) {
        this.elasticServiceContainer = elasticServiceContainer;
        this.config = config;
        this.alert = alert;
    }

    public ElasticServiceConfig getElasticService() {
        return elasticServiceContainer.getElasticService();
    }

    public ElasticServiceContainer getElasticServiceContainer() {
        return elasticServiceContainer;
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

    public Map getTransientData() {
        return transientData;
    }

    public void run() {
        try {
            Alert.AlertState state = alert.execute(this);
//            System.out.println("Alert[" + config.getName() + "]: returned STATE = " + state);
            try {
                switch (state) {
                    case NO_DATA:
                        break;
                    case OK:
 //                       elasticServiceContainer.scaleDown();
                        break;
                    case ALARM:
                        AlertConfig c = getAlertConfig();
                        AlertAction alertAction = c.getAlertActions().getAlertAction("alarm-state");
                        if (alertAction.getAction().equals("scale-up-action"))
                            elasticServiceContainer.scaleUp();
                        else
                            elasticServiceContainer.scaleDown();
                        break;
                }
            } catch (Exception ex) {
                EngineUtil.getLogger().log(Level.WARNING, "Exception during orchestrator invocation", ex);
            }
            transientData.clear();
        } catch (Exception ex) {
            EngineUtil.getLogger().log(Level.WARNING, "Exception during Alert execution", ex);
        }
    }

}
