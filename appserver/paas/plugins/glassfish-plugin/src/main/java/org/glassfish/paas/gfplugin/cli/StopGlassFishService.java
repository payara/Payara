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

/**
 * @author Jagadish Ramu
 */
@Service(name = "stop-glassfish-service")
@Scoped(PerLookup.class)
public class StopGlassFishService implements AdminCommand {

    @Inject
    private CloudRegistryService registryService;

    @Param(name = "servicename", primary = true, optional = false)
    private String serviceName;

    @Inject
    private ServiceUtil serviceUtil;

    @Param(name = "cascade", optional = true, defaultValue = "false")
    private boolean cascade;


    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();

        if (serviceUtil.isValidService(serviceName, ServiceUtil.SERVICE_TYPE.APPLICATION_SERVER)) {
            /*
            1) check whether it is domain / cluster / instance.
            2) If its domain, stop the domain
                2a) If its domain and --cascade is true, stop all others too.
            3) If its cluster, stop the cluster (and domain)
            4) If its instance (clustered/standalone), stop the instance and domain.
            */
            String serviceState = getState(serviceName);
            if (isInvalidServiceState(report, serviceState)) {
                return;
            }


            if (!serviceUtil.isDomain(serviceName) && cascade) {
                System.out.println("--cascade is not applicable for service-types other than domain");
            }

            if (serviceUtil.isDomain(serviceName)) {
                stopDomain(serviceName, report, serviceState);
            } else if (serviceUtil.isCluster(serviceName)) {
                stopCluster(serviceName, report);
            } else if (serviceUtil.isClusteredInstance(serviceName)) {
                stopClusteredInstance(serviceName, report);
            } else if (serviceUtil.isStandaloneInstance(serviceName)) {
                stopStandaloneInstance(serviceName, report);
            }
        } else {
            report.setMessage("Invalid service name [" + serviceName + "]");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
    }


    private void stopDomain(String serviceName, ActionReport report, String serviceState) {
        //we have already validated the domain's invalid states.
        //just check whether it can be in one of the states to be stopped.
        if (serviceState.equalsIgnoreCase("stopped")) {
            report.setMessage("Service is already stopped");
            report.setActionExitCode(ActionReport.ExitCode.WARNING);
            //request was to stop the domain, since its already stopped, return with WARNING.
            return;
        }

        if (cascade) {
            // get all sub-components
            // for-each sub-component, check the state.
            // if its RUNNING, stop them.

            Collection<String> subServices = serviceUtil.getAllSubComponents(serviceName);
            for (String subService : subServices) {
                System.out.println("stopping service : " + subService);
                if (serviceUtil.isCluster(subService)) {
                    stopCluster(subService, report);
                } else if (serviceUtil.isClusteredInstance(subService)) {
                    //ignore, as there will be an entry for "cluster" and will be stopped.
                } else if (serviceUtil.isStandaloneInstance(subService)) {
                    stopStandaloneInstance(subService, report);
                }
            }
        }

        if (serviceState.equalsIgnoreCase(State.Running.toString())) {
            stopDomain(serviceName);
        }
    }

    private void stopStandaloneInstance(String serviceName, ActionReport report) {

        if (!isDomainRunning(serviceName, report)) {
            return;
        }

        //String instanceName = serviceUtil.getStandaloneInstanceName(serviceName);
        String instanceState = getState(serviceName);
        if (isInvalidServiceState(report, instanceState)) {
            return;
        } else if (instanceState.equalsIgnoreCase(State.Running.toString())) {
            stopInstance(serviceName);
        }
    }

    private void stopClusteredInstance(String serviceName, ActionReport report) {

        if (!isDomainRunning(serviceName, report)) {
            return;
        }

        //String instanceName = serviceUtil.getClusteredInstanceName(serviceName);
        String instanceState = getState(serviceName);
        if (isInvalidServiceState(report, instanceState)) {
            return;
        } else if (instanceState.equalsIgnoreCase(State.Running.toString())) {
            stopInstance(serviceName);
        }
    }

    private void stopCluster(String serviceName, ActionReport report) {

        if (!isDomainRunning(serviceName, report)) {
            return;
        }

        //String clusterName = serviceUtil.getClusterName(serviceName);
        String clusterState = getState(serviceName);

        if (isInvalidServiceState(report, clusterState)) {
            return;
        } else if (clusterState.equalsIgnoreCase(State.Running.toString())) {
            stopCluster(serviceName);
        }
    }

    private boolean isDomainRunning(String serviceName, ActionReport report) {
        boolean domainRunning = false;
        String domainName = serviceUtil.getDomainName(serviceName);
        if (State.Running.toString().equals(getState(domainName))) {
            domainRunning = true;
        } else {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage("domain [" + domainName + "] need to be running in order to stop [" + serviceName + "]");
        }
        return domainRunning;
    }

    private boolean isInvalidServiceState(ActionReport report, String serviceState) {
        boolean invalidState = false;
        if (serviceState.equalsIgnoreCase("delete_in_progress")) {
            report.setMessage("Service is being deleted");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
        if (serviceState.equalsIgnoreCase("start_in_progress")) {
            report.setMessage("Service is being started, please try later");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
        if (serviceState.equalsIgnoreCase(State.Stop_in_progress.toString())) {
            report.setMessage("Service is being stopped");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
        if (serviceState.equalsIgnoreCase("initializing")) {
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

    private void stopDomain(String serviceName) {
        if (serviceUtil.isDomain(serviceName)) {
            String domainName = serviceUtil.getDomainName(serviceName);
            String dasIPAddress = getIPAddress(domainName);

            ApplicationServerProvisioner appserverProvisioner = registryService.getAppServerProvisioner(dasIPAddress);
            appserverProvisioner.stopDomain(dasIPAddress, domainName);

            List<String> instanceServices = new ArrayList<String>();
            instanceServices.add(domainName);

            String instanceID = getInstanceID(serviceName);

            List<String> instanceIDs = new ArrayList<String>();
            instanceIDs.add(instanceID);

            stopMachineInstances(instanceServices, instanceIDs);
        } else {
            //TODO throw exception ?
        }
    }

    private void stopCluster(String serviceName) {
        String domainName = serviceUtil.getDomainName(serviceName);
        String clusterName = serviceUtil.getClusterName(serviceName);
        String dasIPAddress = getIPAddress(domainName);

        Collection<String> instanceServices = serviceUtil.getAllSubComponents(serviceName);

        ApplicationServerProvisioner appserverProvisioner = registryService.getAppServerProvisioner(dasIPAddress);
        updateState(serviceName, State.Stop_in_progress);
        appserverProvisioner.stopCluster(dasIPAddress, clusterName);

        //TODO  should we update clustered-instance's state here itself instead of updating while
        //TODO  bringing down machine instances ?

        Map<String, String> instanceServiceToinstanceIDMap = new LinkedHashMap<String, String>();
        for (String instanceService : instanceServices) {
            String instanceID = getInstanceID(instanceService);
            String state = getState(instanceService);
            if (State.Running.toString().equals(state)) {
                //stop only running instances
                instanceServiceToinstanceIDMap.put(instanceService, instanceID);
                //serviceUtil.updateState(instanceService, State.Stop_in_progress.toString());
            }
        }
        Collection<String> filteredInstanceServices = instanceServiceToinstanceIDMap.keySet();
        Collection<String> filteredInstanceIDs = instanceServiceToinstanceIDMap.values();

        stopMachineInstances(filteredInstanceServices, filteredInstanceIDs);
        updateState(serviceName, State.NotRunning);
    }

    private String getIPAddress(String serviceName) {
        return serviceUtil.getIPAddress(serviceName, ServiceUtil.SERVICE_TYPE.APPLICATION_SERVER);
    }

    private String getState(String instanceService) {
        return serviceUtil.getServiceState(instanceService, ServiceUtil.SERVICE_TYPE.APPLICATION_SERVER);
    }

    private String getInstanceID(String instanceService) {
        return serviceUtil.getInstanceID(instanceService, ServiceUtil.SERVICE_TYPE.APPLICATION_SERVER);
    }

    private void updateState(String serviceName, State state) {
        serviceUtil.updateState(serviceName, state.toString(), ServiceUtil.SERVICE_TYPE.APPLICATION_SERVER);
    }

    private void stopMachineInstances(Collection<String> instanceServices, Collection<String> instanceIDs) {


        Collection<String> instanceIPs = new ArrayList<String>();
        for (String instanceService : instanceServices) {
            String instanceIP = getIPAddress(instanceService);
            instanceIPs.add(instanceIP);
        }

        CloudProvisioner cloudProvisioner = registryService.getCloudProvisioner();

        for (String instanceService : instanceServices) {
            updateState(instanceService, State.Stop_in_progress);
        }

        cloudProvisioner.stopInstances(instanceIPs);

        for (String instanceService : instanceServices) {
            updateState(instanceService, State.NotRunning);
        }
    }

    private void stopInstance(String serviceName) {

        if (serviceUtil.isInstance(serviceName)) {
            String instanceName = serviceUtil.getInstanceName(serviceName);
            //String instanceIPAddress = getIPAddress(serviceName);

            String domainName = serviceUtil.getDomainName(serviceName);
            String dasIPAddress = getIPAddress(domainName);

            ApplicationServerProvisioner appserverProvisioner = registryService.getAppServerProvisioner(dasIPAddress);
            appserverProvisioner.stopInstance(dasIPAddress, instanceName);

            List<String> instanceServices = new ArrayList<String>();
            instanceServices.add(serviceName);

            String instanceID = getInstanceID(serviceName);

            List<String> instanceIDs = new ArrayList<String>();
            instanceIDs.add(instanceID);

            stopMachineInstances(instanceServices, instanceIDs);
        } else {
            //TODO throw exception ?
        }
    }
}
