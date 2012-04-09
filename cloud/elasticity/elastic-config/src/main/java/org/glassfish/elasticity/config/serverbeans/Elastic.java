package org.glassfish.elasticity.config.serverbeans;

import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.config.support.*;
import javax.inject.Inject;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.*;
import java.beans.PropertyVetoException;
import org.glassfish.paas.tenantmanager.entity.TenantService;

/**
 * Created by IntelliJ IDEA.
 * User: cmott
 * Date: 4/6/12
 */
@Configured
@Singleton
public interface Elastic extends TenantService {
    @Param(name="name", primary = true)
    public void setName(String value) throws PropertyVetoException;

    @Attribute
    public String getName();

}
