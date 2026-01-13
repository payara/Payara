/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package fish.payara.tools.dev.rest;

import jakarta.ws.rs.container.ContainerRequestFilter;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

/**
 *
 * @author Gaurav Gupta
 */
public class DevConsoleJerseyBinder extends AbstractBinder {
    @Override
    protected void configure() {
        bind(RestFilter.class).to(ContainerRequestFilter.class);
    }
}
