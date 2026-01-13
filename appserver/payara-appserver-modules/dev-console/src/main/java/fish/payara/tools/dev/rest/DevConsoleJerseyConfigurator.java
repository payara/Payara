/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package fish.payara.tools.dev.rest;

import jakarta.inject.Inject;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.server.spi.Container;
import org.jvnet.hk2.annotations.Service;

/**
 *
 * @author Gaurav Gupta
 */
@Service
public class DevConsoleJerseyConfigurator
        implements org.glassfish.jersey.server.spi.ContainerLifecycleListener {

    @Inject
    ServiceLocator locator;

    @Override
    public void onStartup(Container container) {
        container.getConfiguration()
                 .getInstances()
                 .add(new DevConsoleJerseyBinder());
    }

@Override
public void onReload(Container cntnr) {}

@Override
public void onShutdown(Container cntnr) {}

}
