/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2017] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 * 
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
 * 
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 * 
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 * 
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.microprofile.healthcheck;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.Events;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.deployment.Deployment;
import org.jvnet.hk2.annotations.Service;

/**
 *
 * @author Andrew Pielage
 */
@Service(name = "healthcheck-service")
@RunLevel(StartupRunLevel.VAL)
public class HealthCheckService implements EventListener {
    
    @Inject
    Events events;
    
    private final Map<String, Set<HealthCheck>> healthChecks = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void postConstruct() {
        events.register(this);
    }
    
    @Override
    public void event(Event event) {
        if (event.is(Deployment.APPLICATION_UNLOADED)) {
            ApplicationInfo appInfo = Deployment.APPLICATION_UNLOADED.getHook(event);
            if (appInfo != null) {
                healthChecks.remove(appInfo.getName());
            }
        }
    }
    
    public void registerHealthCheck(String appName, HealthCheck healthCheck) {
        if (healthChecks.containsKey(appName)) {
            healthChecks.get(appName).add(healthCheck);
        } else {
            synchronized(this) {
                if (!healthChecks.containsKey(appName)) {
                    healthChecks.put(appName, ConcurrentHashMap.newKeySet());
                }
            }
            
            healthChecks.get(appName).add(healthCheck);
        }
        
    }
    
    public void performHealthChecks(HttpServletRequest request, HttpServletResponse httpResponse) throws IOException {
        Set<HealthCheckResponse> healthCheckResponses = new HashSet<>();
        
        for (Set<HealthCheck> healthCheckSet : healthChecks.values()) {
            for (HealthCheck healthCheck : healthCheckSet) {
                try {
                    healthCheckResponses.add(healthCheck.call());
                } catch (Exception ex) {
                    httpResponse.setStatus(500);
                }
            }
        }
        
        if (httpResponse.getStatus() != 500) {
            constructResponse(httpResponse, healthCheckResponses);
        }
    }
    
    public void performAppHealthChecks(HttpServletRequest request, HttpServletResponse httpResponse, String appName) 
            throws IOException {
        if (appName == null || !healthChecks.containsKey(appName)) {
            performHealthChecks(request, httpResponse);
        } else {
            Set<HealthCheckResponse> healthCheckResponses = new HashSet<>();
            for (HealthCheck healthCheck : healthChecks.get(appName)) {
                try {
                    healthCheckResponses.add(healthCheck.call());
                } catch (Exception ex) {
                    httpResponse.setStatus(500);
                }
            }

            if (httpResponse.getStatus() != 500) {
                constructResponse(httpResponse, healthCheckResponses);
            }
        }
    }
    
    private void constructResponse(HttpServletResponse httpResponse, 
            Set<HealthCheckResponse> healthCheckResponses) throws IOException {
        httpResponse.setContentType("application/json");
        
        JsonArrayBuilder checksArray = Json.createArrayBuilder();
        for (HealthCheckResponse healthCheckResponse : healthCheckResponses) {
            JsonObjectBuilder healthCheckObject = Json.createObjectBuilder();
            
            healthCheckObject.add("name", healthCheckResponse.getName());
            healthCheckObject.add("state", healthCheckResponse.getState().toString());
            
            // Add data if present
            JsonObjectBuilder healthCheckData = Json.createObjectBuilder();
            if (healthCheckResponse.getData().isPresent() && !healthCheckResponse.getData().get().isEmpty()) {
                for (Map.Entry<String, Object> dataMapEntry : healthCheckResponse.getData().get().entrySet()) {
                    healthCheckData.add(dataMapEntry.getKey(), dataMapEntry.getValue().toString());
                }
            }
            healthCheckObject.add("data", healthCheckData);
            
            // Add finished Object to checks array
            checksArray.add(healthCheckObject);
            
            // Check if we need to set the response as 503
            if (httpResponse.getStatus() == 200 
                    && healthCheckResponse.getState().equals(HealthCheckResponse.State.DOWN)) {
                httpResponse.setStatus(503);
            }
        }
        JsonObjectBuilder responseObject = Json.createObjectBuilder();
        
        // Set the aggregate outcome
        if (httpResponse.getStatus() == 200) {
            responseObject.add("outcome", "UP");
        } else {
            responseObject.add("outcome", "DOWN");
        }
        
        responseObject.add("checks", checksArray);
        
        httpResponse.getOutputStream().print(responseObject.build().toString());
    }

}
