/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package fish.payara.tools.dev.rest;


import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;
import org.jvnet.hk2.annotations.Service;

/**
 *
 * @author Gaurav Gupta
 */
@Service
@ApplicationPath("/dev")   // same as configuration.getEndpoint()
public class DevConsoleJaxrsConfig extends ResourceConfig {

    public DevConsoleJaxrsConfig() {
        // Register all JAX-RS providers
        register(RestFilter.class);

        // (optional) if you add more later:
        // packages("fish.payara.tools.dev.rest");
    }
}