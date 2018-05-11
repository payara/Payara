package fish.payara.microprofile.openapi.impl.rest.init;

import static fish.payara.microprofile.openapi.impl.rest.app.OpenApiApplication.OPEN_API_APPLICATION_PATH;
import static java.util.Arrays.asList;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.glassfish.jersey.servlet.init.JerseyServletContainerInitializer;

import fish.payara.microprofile.openapi.impl.rest.app.OpenApiApplication;

/**
 * Deploys the OpenAPI application to each listener when an application is deployed.
 */
public class OpenApiServletContainerInitializer implements ServletContainerInitializer {

	@Override
	public void onStartup(Set<Class<?>> c, ServletContext ctx) throws ServletException {

		// Only deploy to app root
		if (!"".equals(ctx.getContextPath())) {
			return;
		}

		// Check if there is already an endpoint for OpenAPI
		Map<String, ? extends ServletRegistration> registrations = ctx.getServletRegistrations();
		for (ServletRegistration reg : registrations.values()) {
			if (reg.getMappings().contains(OPEN_API_APPLICATION_PATH)) {
				return;
			}
		}

		// Start the OpenAPI application
		new JerseyServletContainerInitializer().onStartup(new HashSet<>(asList(OpenApiApplication.class)), ctx);
	}

}