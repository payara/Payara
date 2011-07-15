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

package org.glassfish.paas.gfplugin.cli;

import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.paas.orchestrator.provisioning.ApplicationServerProvisioner;
import org.glassfish.paas.orchestrator.provisioning.iaas.CloudProvisioner;
import org.glassfish.paas.orchestrator.provisioning.CloudRegistryService;
import org.glassfish.paas.orchestrator.provisioning.cli.ServiceUtil;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;

import org.glassfish.paas.orchestrator.provisioning.CloudRegistryEntry.State;

import java.util.*;

@Service(name = "start-glassfish-service")
@Scoped(PerLookup.class)
public class StartGlassFishService implements AdminCommand {

    @Inject
    private CloudRegistryService registryService;

    @Param(name = "servicename", primary = true, optional = false)
    private String serviceName;

    @Param(name = "cascade", optional = true, defaultValue = "false")
    private boolean cascade;

    @Inject
    private ServiceUtil serviceUtil;

    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();

        if (serviceUtil.isValidService(serviceName, ServiceUtil.SERVICE_TYPE.APPLICATION_SERVER)) {
            /*
            1) check whether it is domain / cluster / instance.
            2) If its domain, start the domain
                2a) If its domain and --subcomponents is true, start all others too.
            3) If its cluster, start the cluster (and domain)
            4) If its instance (clustered/standalone), start the domain and instance.
            */
            String serviceState = serviceUtil.getServiceState(serviceName, ServiceUtil.SERVICE_TYPE.APPLICATION_SERVER);
            if (isInvalidServiceState(report, serviceState)) {
                return;
            }

            if (!serviceUtil.isDomain(serviceName) && cascade) {
                System.out.println("--cascade is not applicable for service-types other than domain");
            }

            if (serviceUtil.isDomain(serviceName)) {
                startDomain(report, serviceState);
            } else if (serviceUtil.isCluster(serviceName)) {
                startCluster(serviceName, report);
            } else if (serviceUtil.isClusteredInstance(serviceName)) {
                startClusteredInstance(serviceName, report);
            } else if (serviceUtil.isStandaloneInstance(serviceName)) {
                startStandaloneInstance(serviceName, report);
            }
        } else {
            report.setMessage("Invalid service name [" + serviceName + "]");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
    }

    private void startDomain(ActionReport report, String serviceState) {
        //we have already validated the domain's invalid states.
        //just check whether it can be in one of the states to be started.
        if (serviceState.equalsIgnoreCase(State.Running.toString())) {
            report.setMessage("Service is already started");
            report.setActionExitCode(ActionReport.ExitCode.WARNING);
            //request was to start the domain, since its already started, return with WARNING.
            return;
        }

        if (State.NotRunning.toString().equalsIgnoreCase(serviceState)) {
            startDomain(serviceName);
        }

        if (cascade) {
            // get all sub-components
            // for-each sub-component, check the state.
            // if its State.Running, start them.

            Collection<String> subServices = serviceUtil.getAllSubComponents(serviceName);
            for (String subService : subServices) {
                if (serviceUtil.isCluster(subService)) {
                    startCluster(subService, report);
                } else if (serviceUtil.isClusteredInstance(subService)) {
                    //ignore, as there will be an entry for "cluster" and will be started.
                } else if (serviceUtil.isStandaloneInstance(subService)) {
                    startStandaloneInstance(subService, report);
                }
            }
        }
    }

    private void startStandaloneInstance(String serviceName, ActionReport report) {
        String domainName = serviceUtil.getDomainName(serviceName);
        String domainState = serviceUtil.getServiceState(domainName, ServiceUtil.SERVICE_TYPE.APPLICATION_SERVER);
        if (isInvalidServiceState(report, domainState)) {
            return;
        }
        if (State.Running.toString().equalsIgnoreCase(domainState)) {
            System.out.println("domain [" + domainName + "] of service [" + serviceName + "] is already started");
        } else if (State.NotRunning.toString().equalsIgnoreCase(domainState)) {
            startDomain(domainName);
        }

        //String instanceName = serviceUtil.getStandaloneInstanceName(serviceName);
        String instanceState = serviceUtil.getServiceState(serviceName, ServiceUtil.SERVICE_TYPE.APPLICATION_SERVER);
        if (isInvalidServiceState(report, instanceState)) {
            return;
        } else if (State.NotRunning.toString().equalsIgnoreCase(instanceState)) {
            startInstance(serviceName);
        }
    }

    private void startClusteredInstance(String serviceName, ActionReport report) {
        String domainName = serviceUtil.getDomainName(serviceName);
        String domainState = serviceUtil.getServiceState(domainName, ServiceUtil.SERVICE_TYPE.APPLICATION_SERVER);
        if (isInvalidServiceState(report, domainState)) {
            return;
        }
        if (State.Running.toString().equalsIgnoreCase(domainState)) {
            System.out.println("domain [" + domainName + "] of service [" + serviceName + "] is already started");
        } else if (State.NotRunning.toString().equalsIgnoreCase(domainState)) {
            startDomain(domainName);
        }

        //String instanceName = serviceUtil.getClusteredInstanceName(serviceName);
        String instanceState = serviceUtil.getServiceState(serviceName, ServiceUtil.SERVICE_TYPE.APPLICATION_SERVER);
        if (isInvalidServiceState(report, instanceState)) {
            return;
        } else if (State.NotRunning.toString().equalsIgnoreCase(instanceState)) {
            startInstance(serviceName);
        }
    }

    private void startCluster(String serviceName, ActionReport report) {
        String domainName = serviceUtil.getDomainName(serviceName);
        String domainState = serviceUtil.getServiceState(domainName, ServiceUtil.SERVICE_TYPE.APPLICATION_SERVER);
        if (isInvalidServiceState(report, domainState)) {
            return;
        } else if (State.Running.toString().equalsIgnoreCase(domainState)) {
            System.out.println("domain [" + domainName + "] of service [" + serviceName + "] is already started");
        }

        if (State.NotRunning.toString().equalsIgnoreCase(domainState)) {
            startDomain(domainName);
        }

        //String clusterName = serviceUtil.getClusterName(serviceName);
        String clusterState = serviceUtil.getServiceState(serviceName, ServiceUtil.SERVICE_TYPE.APPLICATION_SERVER);

        if (isInvalidServiceState(report, clusterState)) {
            return;
        } else if (State.NotRunning.toString().equalsIgnoreCase(clusterState)) {
            startCluster(serviceName);
        }
    }

    private boolean isInvalidServiceState(ActionReport report, String serviceState) {
        boolean invalidState = false;
        if (serviceState.equalsIgnoreCase(State.Delete_in_progress.toString())) {
            report.setMessage("Service is being deleted");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
        if (serviceState.equalsIgnoreCase(State.Start_in_progress.toString())) {
            report.setMessage("Service is being started");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
        if (serviceState.equalsIgnoreCase(State.Stop_in_progress.toString())) {
            report.setMessage("Service is being stopped, please try later");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
        if (serviceState.equalsIgnoreCase(State.Initializing.toString())) {
            report.setMessage("Service is being created, please try later");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
        if (report.getActionExitCode() != null &&
                (/*report.getActionExitCode().equals(ActionReport.ExitCode.WARNING) ||*/
                        (report.getActionExitCode().equals(ActionReport.ExitCode.FAILURE)))) {
            invalidState = true;
        }
        return invalidState;
    }

    private void startDomain(String serviceName) {

        if (serviceUtil.isDomain(serviceName)) {
            String domainName = serviceUtil.getDomainName(serviceName);
            String dasIPAddress = serviceUtil.getIPAddress(domainName, ServiceUtil.SERVICE_TYPE.APPLICATION_SERVER);

            List<String> instanceServices = new ArrayList<String>();
            instanceServices.add(domainName);

            startMachineInstances(instanceServices);

            ApplicationServerProvisioner appserverProvisioner = registryService.getAppServerProvisioner(dasIPAddress);
            appserverProvisioner.startDomain(dasIPAddress, domainName);
        } else {
            //TODO throw exception ?
        }
    }

    private void startMachineInstances(Collection<String> instanceServices) {

        Map<String, String> instanceToIPMap = new LinkedHashMap<String, String>();
        for (String instanceService : instanceServices) {
            String instanceIP = serviceUtil.getIPAddress(instanceService, ServiceUtil.SERVICE_TYPE.APPLICATION_SERVER);
            String instanceID = serviceUtil.getInstanceID(instanceService, ServiceUtil.SERVICE_TYPE.APPLICATION_SERVER);
            instanceToIPMap.put(instanceID, instanceIP);
        }

        CloudProvisioner cloudProvisioner = registryService.getCloudProvisioner();

        for (String instanceService : instanceServices) {
            serviceUtil.updateState(instanceService, State.Start_in_progress.toString(), ServiceUtil.SERVICE_TYPE.APPLICATION_SERVER);
        }

        cloudProvisioner.startInstances(instanceToIPMap);

        for (String instanceService : instanceServices) {
            serviceUtil.updateState(instanceService, State.Running.toString(), ServiceUtil.SERVICE_TYPE.APPLICATION_SERVER);
        }
    }


    private void startCluster(String serviceName) {

        String domainName = serviceUtil.getDomainName(serviceName);
        String clusterName = serviceUtil.getClusterName(serviceName);
        String dasIPAddress = serviceUtil.getIPAddress(domainName, ServiceUtil.SERVICE_TYPE.APPLICATION_SERVER);

        Collection<String> instanceServices = serviceUtil.getAllSubComponents(serviceName);

        ApplicationServerProvisioner appserverProvisioner = registryService.getAppServerProvisioner(dasIPAddress);
        serviceUtil.updateState(serviceName, State.Start_in_progress.toString(), ServiceUtil.SERVICE_TYPE.APPLICATION_SERVER);

        //TODO  should we update clustered-instance's state here itself instead of updating while
        //TODO  bringing down machine instances ?

        Map<String, String> instanceServiceToinstanceIDMap = new LinkedHashMap<String, String>();
        for (String instanceService : instanceServices) {
            String instanceID = serviceUtil.getInstanceID(instanceService, ServiceUtil.SERVICE_TYPE.APPLICATION_SERVER);
            String state = serviceUtil.getServiceState(instanceService, ServiceUtil.SERVICE_TYPE.APPLICATION_SERVER);
            if (State.NotRunning.toString().equals(state)) {
                //start only stopped instances
                instanceServiceToinstanceIDMap.put(instanceService, instanceID);
            }
        }
        Collection<String> filteredInstanceServices = instanceServiceToinstanceIDMap.keySet();
        //Collection<String> filteredInstanceIDs = instanceServiceToinstanceIDMap.values();

        if (filteredInstanceServices.size() > 0) {
            startMachineInstances(filteredInstanceServices);
        }

        appserverProvisioner.startCluster(dasIPAddress, clusterName);
        serviceUtil.updateState(serviceName, State.Running.toString(), ServiceUtil.SERVICE_TYPE.APPLICATION_SERVER);
    }

    private void startInstance(String serviceName) {

        if (serviceUtil.isInstance(serviceName)) {
            String instanceName = serviceUtil.getInstanceName(serviceName);
            //String instanceIPAddress = serviceUtil.getIPAddress(serviceName, ServiceUtil.SERVICE_TYPE.APPLICATION_SERVER);

            String domainName = serviceUtil.getDomainName(serviceName);
            String dasIPAddress = serviceUtil.getIPAddress(domainName, ServiceUtil.SERVICE_TYPE.APPLICATION_SERVER);

            List<String> instanceServices = new ArrayList<String>();
            instanceServices.add(serviceName);

            startMachineInstances(instanceServices);

            ApplicationServerProvisioner appserverProvisioner = registryService.getAppServerProvisioner(dasIPAddress);
            appserverProvisioner.startInstance(dasIPAddress, instanceName);

        } else {
            //TODO throw exception ?
        }
    }
}
