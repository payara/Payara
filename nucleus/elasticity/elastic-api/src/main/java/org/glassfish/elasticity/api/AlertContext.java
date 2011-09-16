package org.glassfish.elasticity.api;

import org.glassfish.elasticity.config.serverbeans.AlertConfig;
import org.glassfish.elasticity.config.serverbeans.ElasticService;

import java.security.Provider;
import java.util.Map;

/**
 * @author Mahesh.Kannan@Oracle.Com
 */
public interface AlertContext<C extends AlertConfig> {

    public ElasticService getElasticService();

    public C getAlertConfig();

    public Alert getAlert();

    public Map getTransientData();

}
