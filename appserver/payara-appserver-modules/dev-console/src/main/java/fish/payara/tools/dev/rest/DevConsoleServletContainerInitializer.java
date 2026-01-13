package fish.payara.tools.dev.rest;

import fish.payara.tools.dev.admin.DevConsoleServiceConfiguration;
import fish.payara.tools.dev.core.DevConsoleService;
import fish.payara.tools.dev.resources.ApplicationsServlet;
import fish.payara.tools.dev.resources.BeanGraphServlet;
import fish.payara.tools.dev.resources.BeansServlet;
import fish.payara.tools.dev.resources.DecoratedClassesServlet;
import fish.payara.tools.dev.resources.DecoratorsServlet;
import fish.payara.tools.dev.resources.DevConsoleResourceServlet;
import fish.payara.tools.dev.resources.EventsServlet;
import fish.payara.tools.dev.resources.ExtensionsServlet;
import fish.payara.tools.dev.resources.InjectionPointsServlet;
import fish.payara.tools.dev.resources.InterceptedClassesServlet;
import fish.payara.tools.dev.resources.InterceptorsServlet;
import fish.payara.tools.dev.resources.MetadataServlet;
import fish.payara.tools.dev.resources.ObserversServlet;
import fish.payara.tools.dev.resources.ProducersServlet;
import fish.payara.tools.dev.resources.RestExceptionMappersServlet;
import fish.payara.tools.dev.resources.RestMethodsServlet;
import fish.payara.tools.dev.resources.RestResourcesServlet;
import fish.payara.tools.dev.resources.ScopedBeansDetailServlet;
import fish.payara.tools.dev.resources.ScopedBeansServlet;
import fish.payara.tools.dev.resources.SecurityAuditServlet;
import fish.payara.tools.dev.resources.SeenTypesServlet;
import jakarta.servlet.HttpConstraintElement;
import jakarta.servlet.Servlet;

import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.ServletSecurityElement;
import static jakarta.servlet.annotation.ServletSecurity.TransportGuarantee.CONFIDENTIAL;
import static java.util.Arrays.asList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import static org.glassfish.common.util.StringHelper.isEmpty;
import org.glassfish.internal.api.Globals;

public class DevConsoleServletContainerInitializer
        implements ServletContainerInitializer {

    @Override
    public void onStartup(Set<Class<?>> c, ServletContext ctx)
            throws ServletException {

        // Only register on root context
        if (!"".equals(ctx.getContextPath())) {
            return;
        }
        DevConsoleServiceConfiguration configuration = Globals.getDefaultBaseServiceLocator()
                .getService(DevConsoleServiceConfiguration.class);

        DevConsoleService consoleService = Globals
                .getDefaultBaseServiceLocator()
                .getService(DevConsoleService.class);

        if (consoleService == null || !consoleService.isEnabled()) {
            return;
        }

        String virtualServers = configuration.getVirtualServers();
        if (!isEmpty(virtualServers)
                && !asList(virtualServers.split(","))
                        .contains(ctx.getVirtualServerName())) {
            return;
        }
        
        String endpoint = "/" + configuration.getEndpoint();   // eg "/dev"

        /* ---------------- UI (HTML / CSS / JS) ---------------- */
        ServletRegistration.Dynamic ui = ctx.addServlet(
                "DevConsoleUI",
                DevConsoleResourceServlet.class
        );
        ui.addMapping(endpoint);
        ui.addMapping(endpoint + "/*");

        // Central servlet registry
        Map<String, Class<?>> servlets = new LinkedHashMap<>();

        /* ---------------- CDI ---------------- */
        servlets.put("/cdi/beans/*", BeansServlet.class);
        servlets.put("/cdi/bean-graph/*", BeanGraphServlet.class);
        servlets.put("/cdi/scoped-beans", ScopedBeansServlet.class);
        servlets.put("/cdi/scoped-beans/detail", ScopedBeansDetailServlet.class);
        servlets.put("/cdi/injection-points/*", InjectionPointsServlet.class);
        servlets.put("/cdi/interceptors/*", InterceptorsServlet.class);
        servlets.put("/cdi/intercepted-classes", InterceptedClassesServlet.class);
        servlets.put("/cdi/decorators/*", DecoratorsServlet.class);
        servlets.put("/cdi/decorated-classes", DecoratedClassesServlet.class);
        servlets.put("/cdi/producers/*", ProducersServlet.class);
        servlets.put("/cdi/observers", ObserversServlet.class);
        servlets.put("/cdi/events", EventsServlet.class);
        servlets.put("/cdi/extensions", ExtensionsServlet.class);
        servlets.put("/cdi/seen-types", SeenTypesServlet.class);

        /* ---------------- REST ---------------- */
        servlets.put("/rest/resources", RestResourcesServlet.class);
        servlets.put("/rest/methods/*", RestMethodsServlet.class);
        servlets.put("/rest/exception-mappers", RestExceptionMappersServlet.class);

        /* ------------- SECURITY -------------- */
        servlets.put("/security/audit", SecurityAuditServlet.class);

        /* -------------- META ----------------- */
        servlets.put("/metadata", MetadataServlet.class);
        servlets.put("/applications", ApplicationsServlet.class);  

        // Register all servlets
        servlets.forEach((path, servletClass) -> {
            String name = servletClass.getSimpleName();
            ServletRegistration.Dynamic reg = ctx.addServlet(name, (Class<? extends Servlet>) servletClass);
            reg.addMapping("/" + configuration.getEndpoint() + path);
            if (Boolean.parseBoolean(configuration.getSecurityEnabled())) {
                String[] roles = configuration.getRoles().split(",");
                reg.setServletSecurity(new ServletSecurityElement(new HttpConstraintElement(CONFIDENTIAL, roles)));
                ctx.declareRoles(roles);
            }
        });

    }
}
