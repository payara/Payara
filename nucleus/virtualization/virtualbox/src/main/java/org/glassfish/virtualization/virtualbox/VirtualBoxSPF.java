package org.glassfish.virtualization.virtualbox;

import org.glassfish.hk2.inject.Injector;
import org.glassfish.virtualization.config.ServerPoolConfig;
import org.glassfish.virtualization.spi.ServerPool;
import org.glassfish.virtualization.spi.ServerPoolFactory;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;

/**
 * Created by IntelliJ IDEA.
 * User: dochez
 * Date: 9/19/11
 * Time: 1:38 PM
 * To change this template use File | Settings | File Templates.
 */
@Service(name="virtualbox")
public class VirtualBoxSPF implements ServerPoolFactory {
    final Injector injector;
    public VirtualBoxSPF(@Inject Injector injector) {
        this.injector=injector;
    }

    @Override
    public ServerPool build(ServerPoolConfig config) {
        return new VBoxGroup(injector, config);
    }
}
