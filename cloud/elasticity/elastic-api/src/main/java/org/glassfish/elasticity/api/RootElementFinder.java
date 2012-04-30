package org.glassfish.elasticity.api;

import org.glassfish.elasticity.config.serverbeans.*;
import org.jvnet.hk2.annotations.Contract;
import org.jvnet.hk2.config.TransactionFailure;

/**
 * Created by IntelliJ IDEA.
 * User: cmott
 * Date: 4/24/12
 */
@Contract
public interface RootElementFinder {
//    public MetricGatherers getMetricGathererParent();

    // for now use ElasticAlerts  will be Alerts
    public ElasticAlerts getAlertsParent( String tenantid);

    public ElasticAlert addAlertElement(ElasticAlerts elasticAlerts )throws TransactionFailure;
}
