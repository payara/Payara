package org.glassfish.batch.spi.impl;

import com.ibm.jbatch.spi.BatchSecurityHelper;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationManager;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;

@Service
public class GlassFishBatchSecurityHelper
    implements BatchSecurityHelper {

    @Inject
    private InvocationManager invocationManager;

    private ThreadLocal<Boolean> invocationPrivilege = new ThreadLocal<>();

    public void markInvocationPrivilege(boolean isAdmin) {
        invocationPrivilege.set(Boolean.valueOf(isAdmin));
    }

    @Override
    public String getCurrentTag() {
        ComponentInvocation compInv = invocationManager.getCurrentInvocation();
        return "" + compInv.getAppName();
    }

    @Override
    public boolean isAdmin(String tag) {
        Boolean result =  invocationPrivilege.get();
        return result != null ? result : false;
    }
}
