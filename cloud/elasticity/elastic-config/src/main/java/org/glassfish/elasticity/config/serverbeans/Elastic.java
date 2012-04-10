package org.glassfish.elasticity.config.serverbeans;

import org.glassfish.api.admin.config.Named;
import org.glassfish.paas.tenantmanager.entity.TenantService;
import org.jvnet.hk2.config.Configured;

/**
 * Created by IntelliJ IDEA.
 * User: cmott
 * Date: 4/6/12
 */
@Configured
public interface Elastic extends TenantService, Named {

}
