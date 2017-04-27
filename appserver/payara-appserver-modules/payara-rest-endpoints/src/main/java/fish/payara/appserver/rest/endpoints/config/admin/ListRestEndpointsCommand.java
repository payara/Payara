/* 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2017 Payara Foundation and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.config.serverbeans.Application;
import com.sun.enterprise.web.WebApplication;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;

import javax.inject.Inject;
import com.sun.enterprise.web.WebContainer;
import com.sun.enterprise.web.WebModule;
import fish.payara.appserver.rest.endpoints.RestEndpointModel;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.catalina.Container;
import org.apache.catalina.core.StandardWrapper;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestParam;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.internal.data.ModuleInfo;
import org.glassfish.jersey.servlet.ServletContainer;

/**
 * CLI for listing all REST endpoints.
 * <p>
 * asadmin list-rest-endpoints [--modulename <modulename> [--componentname <componentname>]]
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
                @RestParam(name = "modulename", value = "$parent")
            })
})
public class ListRestEndpointsCommand implements AdminCommand {

    /**
     * The name of the deployed module
     */
    @Param(primary = true, alias = "moduleName")
    private String moduleName = null;

    /**
     * The name of the JAX-RS component
     */
    @Param(optional = true, alias = "componentName")
    private String componentName = null;

    //Provides methods to find other HK2 services
    @Inject
    private ServiceLocator habitat;

    @Override
    public void execute(AdminCommandContext context) {

        Map<String, String> endpoints = new HashMap(); // Map of endpoint -> HTTP method

        ActionReport report = context.getActionReport();
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);

        // Get all deployed applications
        ApplicationRegistry appRegistry = habitat.getService(ApplicationRegistry.class);

        // Check if the given application exists
        if(!appRegistry.getAllApplicationNames().contains(moduleName)) {
            report.setMessage("Application " + moduleName + " is not registered");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        
        // Loop through all deployed application names
        for (String applicationRegistryNameEntry : appRegistry.getAllApplicationNames()) {
            if (applicationRegistryNameEntry.equals(moduleName)) {
                // found the entered application
                ApplicationInfo appInfo = appRegistry.get(applicationRegistryNameEntry);

                for (ModuleInfo moduleInfo : appInfo.getModuleInfos()) {
                    WebApplication webApplication = (WebApplication) moduleInfo.getEngineRefForContainer(WebContainer.class).getApplicationContainer();

                    // loop through all web modules in the given application (should only be one but who knows).
                    for (WebModule webModule : webApplication.getWebModules()) {
                        
                        boolean componentExists = (componentName == null);
                        
                        // loop through all servlets in the given web module
                        for (Container container : webModule.findChildren()) {
                            // check that it is actually a servlet
                            if (container instanceof StandardWrapper) {
                                // cast to a servlet from generic container
                                StandardWrapper servlet = (StandardWrapper) container;
                                // if it is a jersey application servlet, and if a component name has been specified and this servlet matches
                                if (servlet.getServletClass() == ServletContainer.class && (componentName == null ^ servlet.getName().equals(componentName))) {
                                    componentExists = true;
                                    // count through all the application URL mappings
                                    for (String mapping : servlet.getMappings()) {
                                        // May be represented as "path/to/resource/*", which needs to be removed
                                        mapping = mapping.replaceAll("/\\*", "");
                                        // Convert the servlet to a jersey application servlet
                                        ServletContainer jerseyApplication = (ServletContainer) servlet.getServlet();
                                        Set<Class<?>> containedClasses = jerseyApplication.getConfiguration().getApplication().getClasses();
                                        
                                        // keep track of whether the given Jersey application has deployed endpoints. If a component hasn't been specified,
                                        // then set this to true by default.
                                        boolean componentHasResources = (componentName == null);
                                        
                                        for (Class containedClass : containedClasses) {

                                            // Loop through all of the methods directly declared in the class
                                            for (Method method : containedClass.getDeclaredMethods()) {
                                                // Get the endpoint associated with that method (if applicable)
                                                RestEndpointModel endpoint = RestEndpointModel.generateFromMethod(method);

                                                if (endpoint != null) {
                                                    componentHasResources = true;
                                                    endpoints.put(webModule.getName() + mapping + endpoint.getPath(), endpoint.getRequestMethod());
                                                    report.appendMessage(endpoint.getRequestMethod() + "\t" + webModule.getName() + mapping + endpoint.getPath() + "\n");
                                                }
                                            }
                                        }
                                        
                                        if(!componentHasResources) {
                                            report.setMessage("Component " + componentName + " has no registered REST endpoints");
                                            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                                            return;
                                        }
                                    }
                                }
                            }
                        }

                        if (!componentExists) {
                            report.setMessage("Component " + componentName + " could not be found");
                            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                            return;
                        }
                    }
                }
            }
        }
        
        if(endpoints.isEmpty()) {
            report.setMessage("Application " + moduleName + " has no registered REST endpoints");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        
        Properties extraProps = new Properties();
        extraProps.put("endpointMap", endpoints);
        report.setExtraProperties(extraProps);
    }

}
