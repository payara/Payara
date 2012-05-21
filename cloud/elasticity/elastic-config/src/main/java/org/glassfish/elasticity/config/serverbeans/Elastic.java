package org.glassfish.elasticity.config.serverbeans;

import org.glassfish.api.Param;
import org.glassfish.api.admin.config.Named;
import org.glassfish.paas.tenantmanager.entity.TenantService;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.DuckTyped;
import org.jvnet.hk2.config.Element;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.beans.PropertyVetoException;


/**
 * Created by IntelliJ IDEA.
 * User: cmott
 * Date: 4/6/12
 */
@Configured
public interface Elastic extends TenantService {


    @Element
    ElasticAlerts getElasticAlerts();
    void setElasticAlerts(ElasticAlerts elasticAlerts);

}
