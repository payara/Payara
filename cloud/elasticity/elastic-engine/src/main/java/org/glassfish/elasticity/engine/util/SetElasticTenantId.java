package org.glassfish.elasticity.engine.util;

import org.glassfish.paas.tenantmanager.impl.TenantManagerEx;
import javax.inject.Inject;


/**
 * Created by IntelliJ IDEA.
 * User: cmott
 * Date: 4/2/12
 */
public class SetElasticTenantId {
    @Inject
    private static TenantManagerEx tm;

    public static void setId() {

        tm.setCurrentTenant("t1");

    }

}
