package org.glassfish.virtualization.runtime;

import com.sun.enterprise.config.serverbeans.Domain;
import org.glassfish.api.virtualization.VirtualizationEnv;
import org.glassfish.virtualization.config.Virtualization;
import org.glassfish.virtualization.config.Virtualizations;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;

/**
 * Provides environmental methods for the virtualization feature.
 * @author Jerome Dochez
 */
@Service
public class VirtualizationEnvImpl implements VirtualizationEnv {

    @Inject
    Domain domain;

    @Override
    public boolean isPaasEnabled() {
        Virtualizations virtualizations = domain.getExtensionByType(Virtualizations.class);
        return (virtualizations!=null && virtualizations.getVirtualizations().size()>0);
    }
}
