/* 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) [2017-2018] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 * 
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 * 
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 * 
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package fish.payara.appserver.rest.endpoints.config.admin;

import static java.util.Arrays.asList;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.ws.rs.HttpMethod;

import com.sun.enterprise.config.serverbeans.Application;
import com.sun.enterprise.web.WebApplication;
import com.sun.enterprise.web.WebContainer;
import com.sun.enterprise.web.WebModule;

import org.apache.catalina.Container;
import org.apache.catalina.core.StandardWrapper;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import static org.glassfish.api.ActionReport.ExitCode.FAILURE;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RestParam;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.internal.data.EngineRef;
import org.glassfish.internal.data.ModuleInfo;
import org.glassfish.jersey.servlet.ServletContainer;
import org.jvnet.hk2.annotations.Service;

import fish.payara.appserver.rest.endpoints.RestEndpointModel;

/**
 * CLI for listing all REST endpoints.
 * <p>
 * asadmin list-rest-endpoints [--appname <appname> [--componentname
 * <componentname>]]
 *
 * Will be executed on DAS
 *
 * @author Matt Gill
 */
@Service(name = "list-rest-endpoints")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@ExecuteOn(RuntimeType.DAS)
@RestEndpoints({
    @RestEndpoint(configBean = Application.class,
            opType = RestEndpoint.OpType.GET,
            path = "list-rest-endpoints",
            description = "list-rest-endpoints",
            params = {
                @RestParam(name = "appname", value = "$parent")
            })
})
public class ListRestEndpointsCommand implements AdminCommand {

    /**
     * The name of the deployed application
     */
    @Param(primary = true, alias = "appName")
    private String appName = null;

    /**
     * The name of the JAX-RS component
     */
    @Param(optional = true, alias = "componentName")
    private String componentName = null;

    //Provides methods to find other HK2 services
    @Inject
    private ServiceLocator habitat;

    private final String jerseyWADL = "/application.wadl";

    @Override
    public void execute(AdminCommandContext context) {

        ActionReport report = context.getActionReport();
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);

        // Get the endpoints for the application
        Map<String, Set<String>> endpoints = null;
        try {
            endpoints = getEndpointMap(appName); // Map of endpoint -> HTTP methods
        } catch (IllegalArgumentException ex) {
            report.setMessage(ex.getMessage());
            report.setActionExitCode(FAILURE);
            return;
        }

        // Error out in the case of an empty application
        if (endpoints.isEmpty()) {
            if(componentName == null) {
                report.setMessage("Application " + appName + " has no deployed endpoints");
            } else {
                report.setMessage("Component " + componentName + " has no deployed endpoints");
            }
            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        }

        // Print out endpoints to log
        endpoints.forEach((path, methods) -> {
            methods.forEach(method -> {
                report.appendMessage(method + "\t" + path + "\n");
            });
        });
        // Remove trailing spaces
        report.setMessage(report.getMessage().trim());

        Properties extraProps = new Properties();
        extraProps.put("endpoints", endpoints);
        report.setExtraProperties(extraProps);
    }

    /**
     * Returns a list of a map of endpoint -> list of methods.
     * @param appName the name of the application.
     * @throws IllegalArgumentException if the application does not contain endpoints. Specifies the reason.
     */
    public Map<String, Set<String>> getEndpointMap(String appName) {
        // Create an initial array
        Map<String, Set<String>> endpoints = new TreeMap<>();

        // Get all deployed applications
        ApplicationRegistry appRegistry = habitat.getService(ApplicationRegistry.class);

        // Get the deployed application with the provided name
        ApplicationInfo appInfo = getSpecifiedApplication(appName, appRegistry);

        // Check if the given application exists
        if (appInfo == null) {
            throw new IllegalArgumentException("Application " + appName + " is not registered.");
        }

        // Get the web modules from the given application (e.g. multiple wars in an ear)
        List<WebModule> modules = getWebModules(appInfo);

        // Check if the given application exists
        if (modules.isEmpty()) {
            throw new IllegalArgumentException("Application " + appName + " contains no web modules.");
        }

        // Get the Jersey applications from all of the modules (or only the one matching the given component name)
        Map<ServletContainer, String> jerseyApplicationMap = getSpecifiedJerseyApplications(componentName, modules);

        // error out in the case of a non existent provided component or no components at all
        if (jerseyApplicationMap.isEmpty()) {
            if (componentName == null) {
                throw new IllegalArgumentException("Application " + appName + " has no deployed JAX-RS applications.");
            }
            throw new IllegalArgumentException("Component " + componentName + " could not be found.");
        }

        // loop through jersey components
        boolean hasEndpoints = false;
        for (Entry<ServletContainer, String> entry : jerseyApplicationMap.entrySet()) {
            ServletContainer jerseyApplication = entry.getKey();
            String appRoot = jerseyApplication.getServletContext().getContextPath();
            String jerseyAppRoot = entry.getValue();

            List<Class<?>> containedClasses = getClasses(jerseyApplication);

            // loop through all classes contained by given jersey application
            for (Class<?> containedClass : containedClasses) {
                List<RestEndpointModel> classEndpoints = getEndpointsForClass(containedClass);

                if (!classEndpoints.isEmpty()) {
                    // loop through endpoints in given class
                    for (RestEndpointModel endpoint : classEndpoints) {
                        String path = appRoot + jerseyAppRoot + endpoint.getPath();
                        String method = endpoint.getRequestMethod();
                        if (endpoints.keySet().contains(path)) {
                            Set<String> methods = endpoints.get(path);
                            methods.add(method);
                            endpoints.put(path, methods);
                        } else {
                            endpoints.put(path, new TreeSet<>(asList(method)));
                        }
                    }
                }
            }

            // Jersey will automatically generate a wadl file for the endpoints, so add
            // it for every deployed application with endpoints
            endpoints.put(appRoot + jerseyAppRoot + jerseyWADL, new TreeSet<>(asList(HttpMethod.GET)));
            hasEndpoints = true;
        }
        if (!hasEndpoints) {
            return null;
        }
        return endpoints;
    }

    /**
     * Gets the application specified from a given {@link ApplicationRegistry}.
     * This function will return null if either parameters are null, or the
     * application is not found.
     *
     * @param appName the name of the application to be found.
     * @param appRegistry the application registry to search.
     * @return the application.
     */
    private ApplicationInfo getSpecifiedApplication(String appName, ApplicationRegistry appRegistry) {
        if (appRegistry == null || appName == null) {
            return null;
        }
        // Loop through all deployed application names
        for (String applicationRegistryNameEntry : appRegistry.getAllApplicationNames()) {
            if (applicationRegistryNameEntry.equals(appName)) {
                // found the entered application
                return appRegistry.get(applicationRegistryNameEntry);
            }
        }
        return null;
    }

    /**
     * Gets the web modules associated with an application as a list.
     *
     * @param appInfo the application to search.
     * @return a list of web modules.
     */
    private List<WebModule> getWebModules(ApplicationInfo appInfo) {
        if (appInfo == null) {
            return null;
        }
        List<WebModule> webModules = new ArrayList<>();
        // loop through all deployed modules in the application (e.g. multiple wars in one ear)
        for (ModuleInfo moduleInfo : appInfo.getModuleInfos()) {
            EngineRef engineRef = moduleInfo.getEngineRefForContainer(WebContainer.class);
            if (engineRef != null) {
                WebApplication webApplication = (WebApplication) engineRef.getApplicationContainer();
                if (webApplication != null) {
                    for (WebModule module : webApplication.getWebModules()) {
                        webModules.add(module);
                    }
                }
            }
        }
        return webModules;
    }

    /**
     * Gets a map of Jersey container to the Jersey container name, from a given component name and list of web modules.
     * If the component name is null, then this will return all Jersey applications.
     * @param componentName the name of the Jersey component.
     * @param modules a list of web modules.
     * @return a map of Jersey containers to their names.
     */
    private Map<ServletContainer, String> getSpecifiedJerseyApplications(String componentName, List<WebModule> modules) {

        Map<ServletContainer, String> jerseyApplicationMap = new HashMap<>();

        for (WebModule webModule : modules) {

            // loop through all servlets in the given web module
            for (Container container : webModule.findChildren()) {
                // check that it is actually a servlet
                if (container instanceof StandardWrapper) {
                    // cast to a servlet from generic container
                    StandardWrapper servlet = (StandardWrapper) container;
                    // if it is a jersey application servlet, and if a component name has been specified and this servlet matches
                    if (servlet.getServletClass() == ServletContainer.class && (componentName == null ^ servlet.getName().equals(componentName))) {

                        Collection<String> mappings = servlet.getMappings();
                        String servletMapping = null;

                        if (mappings.size() > 0) {
                            // May be represented as "path/to/resource/*", which needs to be removed
                            servletMapping = mappings.toArray()[0].toString().replaceAll("/\\*", "");
                        }

                        jerseyApplicationMap.put((ServletContainer) servlet.getServlet(), servletMapping);
                    }
                }
            }
        }

        return jerseyApplicationMap;
    }

    /**
     * Gets the user defined classes associated with a Jersey application as a list.
     * @param jerseyApplication the Jersey application.
     * @return a list of classes.
     */
    private List<Class<?>> getClasses(ServletContainer jerseyApplication) {
        List<Class<?>> classes = new ArrayList<>();

        for (Class jerseyClass : jerseyApplication.getConfiguration().getApplication().getClasses()) {
            if(jerseyClass.getPackage() == null || !jerseyClass.getPackage().getName().contains("org.glassfish.jersey.server.wadl")) {
                classes.add(jerseyClass);
            }
        }
        return classes;
    }

    /**
     * Gets the endpoints for a given class.
     * @param containerClass the class to search.
     * @return a list of {@link RestEndpointModel}s.
     */
    private List<RestEndpointModel> getEndpointsForClass(Class containerClass) {
        List<RestEndpointModel> endpoints = new ArrayList<>();
        // Loop through all of the methods directly declared in the class
        for (Method method : containerClass.getDeclaredMethods()) {
            // Get the endpoint associated with that method (if applicable)
            RestEndpointModel endpoint = RestEndpointModel.generateFromMethod(method);

            if (endpoint != null) {
                endpoints.add(endpoint);
            }
        }
        return endpoints;
    }

}
