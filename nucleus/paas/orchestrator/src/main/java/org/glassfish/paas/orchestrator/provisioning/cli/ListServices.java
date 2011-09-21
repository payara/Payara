/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
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
package org.glassfish.paas.orchestrator.provisioning.cli;

import com.sun.enterprise.admin.util.ColumnFormatter;
import com.sun.enterprise.config.serverbeans.Domain;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.paas.orchestrator.config.*;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.component.PerLookup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;

@org.jvnet.hk2.annotations.Service(name = "list-services")
@Scoped(PerLookup.class)
@ExecuteOn(RuntimeType.DAS)
@TargetType(value = {CommandTarget.DAS})
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class, opType = RestEndpoint.OpType.GET, path = "list-services", description = "List Services")
})
public class ListServices implements AdminCommand {

    @Param(name = "appname", optional = true)
    private String appName;
    @Param(name = "type", optional = true)
    private String type;
    @Param(name = "scope", optional = true, acceptableValues = SCOPE_EXTERNAL + "," + SCOPE_SHARED + "," + SCOPE_APPLICATION)
    private String scope;
    @Param(name = "terse", optional = true, shortName = "t", defaultValue = "true")
    private boolean terse;
    @Param(name = "header", optional = true, shortName = "h", defaultValue = "false")
    private boolean header;
    @Param(name = "output", optional = true, shortName = "o")
    private String output;
    @Param(name = "key", optional = true)
    private String key;
    @Inject
    private Domain domain;
    @Inject
    private ServiceUtil serviceUtil;
    public static final String SCOPE_EXTERNAL = "external";
    public static final String SCOPE_SHARED = "shared";
    public static final String SCOPE_APPLICATION = "application";

    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();

        Services services = serviceUtil.getServices();
        List<Service> matchedServices = new ArrayList<Service>();

        if (appName != null) {
            //TODO will "target" of application play a role here ? AFAIK, no.
            if (domain.getApplications().getApplication(appName) == null) {
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage("No such application [" + appName + "] is deployed");
                return;
            }

            for (Service service : services.getServices()) {
                if (service instanceof ApplicationScopedService) {
                    if (appName.equals(((ApplicationScopedService) service).getApplicationName())) {
                        if (type != null) {
                            if (service.getType().equals(type)) {
                                if (scope != null) {
                                    if (scope.equals(getServiceScope(service))) {
                                        matchedServices.add(service);
                                    }
                                } else {
                                    matchedServices.add(service);
                                }
                            }
                        } else {
                            if (scope != null) {
                                if (scope.equals(getServiceScope(service))) {
                                    matchedServices.add(service);
                                }
                            } else {
                                matchedServices.add(service);
                            }
                        }
                    }
                }
            }

            for (ServiceRef serviceRef : services.getServiceRefs()) {
                if (appName.equals(serviceRef.getApplicationName())) {
                    for (Service service : services.getServices()) {
                        if (service.getServiceName().equals(serviceRef.getServiceName())) {
                            if (type != null) {
                                if (service.getType().equals(type)) {
                                    if (scope != null) {
                                        if (scope.equals(getServiceScope(service))) {
                                            matchedServices.add(service);
                                        }
                                    } else {
                                        matchedServices.add(service);
                                        break;
                                    }
                                }
                            } else {
                                if (scope != null) {
                                    if (scope.equals(getServiceScope(service))) {
                                        matchedServices.add(service);
                                        break;
                                    } else {
                                        matchedServices.add(service);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (scope != null && scope.equals(SCOPE_APPLICATION)) {
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage("Need application name in order to list application-scoped services");
                return;
            }

            for (Service service : services.getServices()) {
                if (type != null) {
                    if (service.getType().equals(type)) {
                        if (scope != null) {
                            if (scope.equals(getServiceScope(service))) {
                                matchedServices.add(service);
                            }
                        } else {
                            matchedServices.add(service);
                        }
                    }
                } else {
                    if (scope != null) {
                        if (scope.equals(getServiceScope(service))) {
                            matchedServices.add(service);
                        }
                    } else {
                        matchedServices.add(service);
                    }
                }
            }
        }

        Properties extraProperties = new Properties();
        extraProperties.put("list", new ArrayList<Map<String, String>>());

        if (matchedServices.size() > 0) {

            String headings[] = new String[]{"", "", "", "", "", ""};
            if (terse) {
                if (header) {
                    headings = new String[]{"SERVICE-NAME", "IP-ADDRESS", "VM-ID", "SERVER-TYPE", "STATE", "SCOPE"};
                    if (type != null) {
                        if (scope != null) {
                            headings = new String[]{"SERVICE-NAME", "IP-ADDRESS", "VM-ID", "STATE"};
                        } else {
                            headings = new String[]{"SERVICE-NAME", "IP-ADDRESS", "VM-ID", "STATE", "SCOPE"};
                        }
                    } else {
                        if (scope != null) {
                            headings = new String[]{"SERVICE-NAME", "IP-ADDRESS", "VM-ID", "SERVER-TYPE", "STATE"};
                        }
                    }

                }

            } else if (output != null) {
                String[] outputheaders = output.split("[,]");
                int count = 0;
                for (String s : outputheaders) {
                    s = s.toUpperCase();
                    headings[count] = s;
                    count++;
                }
            } else {
                headings = new String[]{"SERVICE-NAME", "IP-ADDRESS", "VM-ID", "SERVER-TYPE", "STATE", "SCOPE"};
                if (type != null) {
                    if (scope != null) {
                        headings = new String[]{"SERVICE-NAME", "IP-ADDRESS", "VM-ID", "STATE"};
                    } else {
                        headings = new String[]{"SERVICE-NAME", "IP-ADDRESS", "VM-ID", "STATE", "SCOPE"};
                    }
                } else {
                    if (scope != null) {
                        headings = new String[]{"SERVICE-NAME", "IP-ADDRESS", "VM-ID", "SERVER-TYPE", "STATE"};
                    }
                }


            }

            ColumnFormatter cf = new ColumnFormatter(headings);

            boolean foundRows = false;

            HashMap<String, String> name_map = new HashMap<String, String>();
            HashMap<String, String> ip_map = new HashMap<String, String>();
            HashMap<String, String> vm_map = new HashMap<String, String>();
            HashMap<String, String> type_map = new HashMap<String, String>();
            HashMap<String, String> state_map = new HashMap<String, String>();
            HashMap<String, String> scope_map = new HashMap<String, String>();

            for (Service service : matchedServices) {
                foundRows = true;
                String cloudName = service.getServiceName();
                String ipAddress = service.getPropertyValue("ip-address");
                if (ipAddress == null) {
                    ipAddress = "-";
                }
                String instanceID = service.getPropertyValue("vm-id");
                if (instanceID == null) {
                    instanceID = "-";
                }
                String serverType = service.getType();

                String serviceType = null;
                String state = "-";
                if (service instanceof ApplicationScopedService) {
                    state = ((ApplicationScopedService) service).getState();
                    serviceType = SCOPE_APPLICATION;
                } else if (service instanceof SharedService) {
                    state = ((SharedService) service).getState();
                    serviceType = SCOPE_SHARED;
                } else if (service instanceof ExternalService) {
                    state = "-";
                    serviceType = SCOPE_EXTERNAL;
                }

                name_map.put(cloudName, cloudName);
                ip_map.put(cloudName, ipAddress);
                vm_map.put(cloudName, instanceID);
                type_map.put(cloudName, serverType);
                state_map.put(cloudName, state);
                scope_map.put(cloudName, serviceType);

                if (key == null) {
                    if (output != null) {
                        String outputstring[] = new String[]{"", "", "", "", "", ""};
                        int count = 0;
                        for (String s : headings) {
                            if (s.equals("SERVICE-NAME")) {
                                outputstring[count] = cloudName;
                            } else if (s.equals("IP-ADDRESS")) {
                                outputstring[count] = ipAddress;
                            } else if (s.equals("VM-ID")) {
                                outputstring[count] = instanceID;
                            } else if (s.equals("SERVER-TYPE")) {
                                outputstring[count] = serverType;
                            } else if (s.equals("STATE")) {
                                outputstring[count] = state;
                            } else if (s.equals("SCOPE")) {
                                outputstring[count] = serviceType;
                            }
                            count++;
                        }
                        cf.addRow(outputstring);
                    } else if (type == null) {
                        if (scope == null) {
                            cf.addRow(new Object[]{cloudName, ipAddress, instanceID, serverType, state, serviceType});
                        } else {
                            if (serviceType.equals(scope)) {
                                cf.addRow(new Object[]{cloudName, ipAddress, instanceID, serverType, state});
                            }
                        }
                    } else {
                        if (scope == null) {
                            cf.addRow(new Object[]{cloudName, ipAddress, instanceID, state, serviceType});
                        } else {
                            if (serviceType.equals(scope)) {
                                cf.addRow(new Object[]{cloudName, ipAddress, instanceID, state});
                            }
                        }
                    }
                }
            }

            if (key != null) {
                if (key.equals("service-name")) {
                    name_map = sortHashMap(name_map);
                    for (String e : name_map.keySet()) {
                        cf.addRow(new Object[]{name_map.get(e), ip_map.get(e), vm_map.get(e), type_map.get(e), state_map.get(e), scope_map.get(e)});
                    }
                } else if (key.equals("ip-address")) {
                    ip_map = sortHashMap(ip_map);

                    for (String e : ip_map.keySet()) {
                        cf.addRow(new Object[]{name_map.get(e), ip_map.get(e), vm_map.get(e), type_map.get(e), state_map.get(e), scope_map.get(e)});
                    }
                } else if (key.equals("vm-id")) {
                    vm_map = sortHashMap(vm_map);

                    for (String e : vm_map.keySet()) {
                        cf.addRow(new Object[]{name_map.get(e), ip_map.get(e), vm_map.get(e), type_map.get(e), state_map.get(e), scope_map.get(e)});
                    }
                } else if (key.equals("server-type")) {
                    type_map = sortHashMap(type_map);

                    for (String e : type_map.keySet()) {
                        cf.addRow(new Object[]{name_map.get(e), ip_map.get(e), vm_map.get(e), type_map.get(e), state_map.get(e), scope_map.get(e)});
                    }
                } else if (key.equals("state")) {
                    state_map = sortHashMap(state_map);

                    for (String e : state_map.keySet()) {
                        cf.addRow(new Object[]{name_map.get(e), ip_map.get(e), vm_map.get(e), type_map.get(e), state_map.get(e), scope_map.get(e)});
                    }
                } else if (key.equals("scope")) {
                    scope_map = sortHashMap(scope_map);

                    for (String e : scope_map.keySet()) {
                       cf.addRow(new Object[]{name_map.get(e), ip_map.get(e), vm_map.get(e), type_map.get(e), state_map.get(e), scope_map.get(e)});
                    }
                }
            }

            if (foundRows) {
                report.setMessage(cf.toString());
                extraProperties.put("list", cf.getContent());
            } else if (header = true) {
                report.setMessage("Nothing to list.");
            }
        } else if (header = true){
            report.setMessage("Nothing to list.");
        }

        report.setExtraProperties(extraProperties);
        ActionReport.ExitCode ec = ActionReport.ExitCode.SUCCESS;
        report.setActionExitCode(ec);
    }

    private String getServiceScope(Service service) {
        String scope = null;
        if (service instanceof ApplicationScopedService) {
            scope = SCOPE_APPLICATION;
        } else if (service instanceof SharedService) {
            scope = SCOPE_SHARED;
        } else if (service instanceof ExternalService) {
            scope = SCOPE_EXTERNAL;
        }
        return scope;
    }

 
    public LinkedHashMap sortHashMap(HashMap passedMap) {
    List mapKeys = new ArrayList(passedMap.keySet());
    List mapValues = new ArrayList(passedMap.values());
    Collections.sort(mapValues);
    Collections.sort(mapKeys);
        
    LinkedHashMap sortedMap = 
        new LinkedHashMap();
    
    Iterator valueIt = mapValues.iterator();
    while (valueIt.hasNext()) {
        Object val = valueIt.next();
        Iterator keyIt = mapKeys.iterator();
        
        while (keyIt.hasNext()) {
            Object key = keyIt.next();
            String comp1 = passedMap.get(key).toString();
            String comp2 = val.toString();
            
            if (comp1.equals(comp2)){
                passedMap.remove(key);
                mapKeys.remove(key);
                sortedMap.put((String)key, (String)val);
                break;
            }

        }

    }
    return sortedMap;
}
}
