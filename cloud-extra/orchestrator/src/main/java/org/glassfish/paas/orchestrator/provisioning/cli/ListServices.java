/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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
import org.glassfish.paas.orchestrator.provisioning.ServiceInfo;
import javax.inject.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.glassfish.hk2.api.PerLookup;

import java.util.*;

@org.jvnet.hk2.annotations.Service(name = "list-services")
@PerLookup
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
    @Param(name = "terse", optional = true, shortName = "t", defaultValue = "false")
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

    //The list which contains all the options for the command.
    //Changing the order of elements in this list requires some effort changing the code
    public static final List<String> listOfOptionsForCommand =Arrays.asList("SERVICE-NAME","IP-ADDRESS","VM-ID","SERVER-TYPE","STATE","SCOPE");

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
                            if (service.getType().equalsIgnoreCase(type)) {
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
                                if (service.getType().equalsIgnoreCase(type)) {
                                    if (scope != null) {
                                        if (scope.equals(getServiceScope(service))) {
                                            //matchedServices.add(service);
                                            matchedServices.addAll(getServiceConfigurations(service));
                                            break;
                                        }
                                    } else {
                                        //matchedServices.add(service);
                                        matchedServices.addAll(getServiceConfigurations(service));
                                        break;
                                    }
                                }
                            } else {
                                if (scope != null) {
                                    if (scope.equals(getServiceScope(service))) {
                                        //matchedServices.add(service);
                                        matchedServices.addAll(getServiceConfigurations(service));
                                        break;
                                    } else {
                                        //matchedServices.add(service);
                                        matchedServices.addAll(getServiceConfigurations(service));
                                        break;
                                    }
                                }else{
                                     //matchedServices.add(service);
                                    matchedServices.addAll(getServiceConfigurations(service));
                                    break;
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
                    if (service.getType().equalsIgnoreCase(type)) {
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

            int heading_count = 0;

            List<String> headerList = new ArrayList<String>();

            if (output != null) {
                String[] outputheaders = output.split("[,]");
                int count = 0;
                for (String s : outputheaders) {
                    s = s.trim().toUpperCase();
                   // if (!(s.equals("SERVICE-NAME") || s.equals("IP-ADDRESS") || s.equals("VM-ID") || s.equals("SERVER-TYPE") || s.equals("STATE") || s.equals("SCOPE"))) {
                    if (!listOfOptionsForCommand.contains(s)) {
                        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                        report.setMessage("The column name [" + s.toLowerCase() + "] provided in --output is not valid.");
                        return;
                    }
                    headerList.add(s);
                }
            } else {
                /*headerList.add("SERVICE-NAME");
                headerList.add("IP-ADDRESS");
                headerList.add("VM-ID");
                headerList.add("SERVER-TYPE");
                headerList.add("STATE");
                headerList.add("SCOPE"); */
                headerList.addAll(listOfOptionsForCommand);
                if (type != null) {
                    if (scope != null) {
                        headerList.remove("SCOPE");
                        headerList.remove("SERVER-TYPE");
                    } else {
                        headerList.remove("SERVER-TYPE");
                    }
                } else {
                    if (scope != null) {
                        headerList.remove("SCOPE");
                    }
                }
            }

            ColumnFormatter cf = new ColumnFormatter();
            String[] headings = headerList.toArray(new String[headerList.size()]);
            heading_count = headerList.size();

            if (terse && (header == false)) {
                String[] s = new String[headerList.size()];
                for (int i = 0; i < heading_count; i++) {
                    s[i] = "";
                }
                cf = new ColumnFormatter(s);
            } else {
                cf = new ColumnFormatter(headings);
            }


            boolean foundRows = false;

            LinkedHashMap<String,HashMap> mapOfMaps=new LinkedHashMap<String,HashMap>();
            /*HashMap<String, String> name_map=null;
            HashMap<String, String> ip_map=null;
            HashMap<String, String> vm_map=null;
            HashMap<String, String> type_map=null;
            HashMap<String, String> state_map=null;
            HashMap<String, String> scope_map=null;
            HashMap<String, String> subnet_map=null;*/

           // for (String s : headings) {
            for(int i=0;i<listOfOptionsForCommand.size();i++){
                //int index=listOfOptionsForCommand.indexOf(s);
                 mapOfMaps.put(listOfOptionsForCommand.get(i),new HashMap<String,String>());
                }


            HashMap<String,String> valueMap=new HashMap<String, String>();

            for (Service service : matchedServices) {
                foundRows = true;
                String cloudName = service.getServiceName();
                valueMap.put(listOfOptionsForCommand.get(0),cloudName);
                String ipAddress = service.getPropertyValue("ip-address");

                if (ipAddress == null) {
                    ipAddress = "-";
                }
                valueMap.put(listOfOptionsForCommand.get(1),ipAddress);
                String instanceID = service.getPropertyValue("vm-id");
                if (instanceID == null) {
                    instanceID = "-";
                }
                valueMap.put(listOfOptionsForCommand.get(2),instanceID);
                String serverType = service.getType();
                valueMap.put(listOfOptionsForCommand.get(3),serverType);

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
                valueMap.put(listOfOptionsForCommand.get(4),state);
                valueMap.put(listOfOptionsForCommand.get(5),serviceType);
                //String subnet="-";
                //subnet=service.getPropertyValue("subnet");
                /*name_map.put(cloudName, cloudName);
                ip_map.put(cloudName, ipAddress);
                vm_map.put(cloudName, instanceID);
                type_map.put(cloudName, serverType);
                state_map.put(cloudName, state);
                scope_map.put(cloudName, serviceType);*/
                 String option;
                //for (String s : headings) {
                for(int i=0;i<listOfOptionsForCommand.size();i++){
                    option= listOfOptionsForCommand.get(i);
                    HashMap<String,String> map=mapOfMaps.get(option);
                    map.put(valueMap.get("SERVICE-NAME"),valueMap.get(option));
                    mapOfMaps.put(option,map);
                }

                if (key == null) {
                    if (output != null) {
                        String[] outputstring = new String[heading_count];
                        int count = 0;

                        for (String s : headings) {
                             outputstring[count]=valueMap.get(s);
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
                key=key.trim().toUpperCase();

                if(!listOfOptionsForCommand.contains(key)){
                        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                        report.setMessage("The key [" + key.toLowerCase() + "] provided in --key is not valid.");
                        return;
                }

                HashMap<String,String> sortedHashMap=new HashMap<String, String>();
                sortedHashMap=sortHashMap(mapOfMaps.get(key));
                mapOfMaps.put(key,sortedHashMap);

                for (String e : sortedHashMap.keySet()) {
                       cf.addRow(this.generateOutputRow(headings, heading_count, e, mapOfMaps));
                    }

               /* if (key.equals("service-name")) {
                    name_map = sortHashMap(name_map);
                    for (String e : name_map.keySet()) {
                        cf.addRow(this.generateOutputRow(headings, heading_count, e, name_map, ip_map, vm_map, type_map, state_map, scope_map));
                    }
                } else if (key.equals("ip-address")) {
                    ip_map = sortHashMap(ip_map);
                    for (String e : ip_map.keySet()) {
                        cf.addRow(this.generateOutputRow(headings, heading_count, e, name_map, ip_map, vm_map, type_map, state_map, scope_map));
                    }
                } else if (key.equals("vm-id")) {

                    vm_map = sortHashMap(vm_map);
                    for (String e : vm_map.keySet()) {
                        cf.addRow(this.generateOutputRow(headings, heading_count, e, name_map, ip_map, vm_map, type_map, state_map, scope_map));
                    }
                } else if (key.equals("server-type")) {
                    type_map = sortHashMap(type_map);
                    for (String e : type_map.keySet()) {
                        cf.addRow(this.generateOutputRow(headings, heading_count, e, name_map, ip_map, vm_map, type_map, state_map, scope_map));
                    }
                } else if (key.equals("state")) {
                    state_map = sortHashMap(state_map);
                    for (String e : state_map.keySet()) {
                        cf.addRow(this.generateOutputRow(headings, heading_count, e, name_map, ip_map, vm_map, type_map, state_map, scope_map));
                    }
                } else if (key.equals("scope")) {
                    scope_map = sortHashMap(scope_map);
                    for (String e : scope_map.keySet()) {
                        cf.addRow(this.generateOutputRow(headings, heading_count, e, name_map, ip_map, vm_map, type_map, state_map, scope_map));
                    }
                }  */
            }


            if (foundRows) {
                report.setMessage(cf.toString());
                extraProperties.put("list", cf.getContent());
            } else if (header = true) {
                report.setMessage("Nothing to list.");
            }
        } else if (header = true) {
            report.setMessage("Nothing to list.");
        }

        report.setExtraProperties(extraProperties);
        ActionReport.ExitCode ec = ActionReport.ExitCode.SUCCESS;
        report.setActionExitCode(ec);
    }

    /**
     *   This method takes the Service object and fetches all the corresponding provisioned services, including child services,
     *   if any.
     *
     * @param service - Service object whose Provisioned service(s) is requested for.
     * @return  List<Service>  - List of the provisioned services (which includes child services of the provisioned services, if any)
     */
    private List<Service> getServiceConfigurations(Service service){
        List<Service> services=new ArrayList<Service>();
        services.add(service);
        ServiceInfo parentServiceInfo=serviceUtil.getServiceInfo(service.getServiceName(),null);
        Set<ServiceInfo> serviceInfoSet=parentServiceInfo.getChildServices();
        Service svc;
        for(ServiceInfo serviceInfo:serviceInfoSet){
            svc=serviceUtil.getService(serviceInfo.getServiceName(),null);
            if(svc!=null){
                services.add(svc);
            }

        }
        return services;
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

                if (comp1.equals(comp2)) {
                    passedMap.remove(key);
                    mapKeys.remove(key);
                    sortedMap.put((String) key, (String) val);
                    break;
                }

            }

        }
        return sortedMap;
    }

       //public String[] generateOutputRow(String[] headings, int heading_count, String e, HashMap<String, String> name_map, HashMap<String, String> ip_map, HashMap<String, String> vm_map, HashMap<String, String> type_map, HashMap<String, String> state_map, HashMap<String, String> scope_map) {
     public String[] generateOutputRow(String[] headings, int heading_count, String e, HashMap<String, HashMap> mapOfMaps) {
        String[] outputRow = new String[heading_count];
        int count = 0;
        HashMap<String,String> map=null;
        for (String s : headings) {
          /*  if (s.equals("SERVICE-NAME")) {
                outputRow[count] = name_map.get(e);
            } else if (s.equals("IP-ADDRESS")) {
                outputRow[count] = ip_map.get(e);
            } else if (s.equals("VM-ID")) {
                outputRow[count] = vm_map.get(e);
            } else if (s.equals("SERVER-TYPE")) {
                outputRow[count] = type_map.get(e);
            } else if (s.equals("STATE")) {
                outputRow[count] = state_map.get(e);
            } else if (s.equals("SCOPE")) {
                outputRow[count] = scope_map.get(e);
            } */

            map=mapOfMaps.get(s);
            outputRow[count] = map.get(e);
            count++;
        }
        return outputRow;
    }
}
