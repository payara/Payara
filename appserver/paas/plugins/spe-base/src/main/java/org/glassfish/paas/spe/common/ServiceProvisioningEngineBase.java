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

package org.glassfish.paas.spe.common;

import com.sun.logging.LogDomains;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.paas.orchestrator.provisioning.ServiceInfo;
import org.glassfish.paas.orchestrator.provisioning.cli.ServiceUtil;
import org.glassfish.paas.orchestrator.service.ServiceStatus;
import org.glassfish.paas.orchestrator.service.metadata.Property;
import org.glassfish.paas.orchestrator.service.metadata.ServiceCharacteristics;
import org.glassfish.paas.orchestrator.service.metadata.ServiceDescription;
import org.glassfish.paas.orchestrator.service.metadata.TemplateIdentifier;
import org.glassfish.paas.orchestrator.service.spi.ProvisionedService;
import org.glassfish.paas.orchestrator.service.spi.ServicePlugin;
import org.glassfish.paas.orchestrator.service.spi.ServiceProvisioningException;
import org.glassfish.virtualization.runtime.VirtualClusters;
import org.glassfish.virtualization.runtime.VirtualMachineLifecycle;
import org.glassfish.virtualization.spi.AllocationConstraints;
import org.glassfish.virtualization.spi.AllocationPhase;
import org.glassfish.virtualization.spi.AllocationStrategy;
import org.glassfish.virtualization.spi.IAAS;
import org.glassfish.virtualization.spi.KeyValueType;
import org.glassfish.virtualization.spi.Listener;
import org.glassfish.virtualization.spi.Machine;
import org.glassfish.virtualization.spi.PhasedFuture;
import org.glassfish.virtualization.spi.SearchCriteria;
import org.glassfish.virtualization.spi.TemplateInstance;
import org.glassfish.virtualization.spi.TemplateRepository;
import org.glassfish.virtualization.spi.VirtualCluster;
import org.glassfish.virtualization.spi.VirtualMachine;
import org.glassfish.virtualization.spi.VirtException;
import org.glassfish.virtualization.util.ServiceType;
import org.glassfish.virtualization.util.SimpleSearchCriteria;
import org.jvnet.hk2.annotations.Inject;

import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author bhavanishankar@java.net
 */
public abstract class ServiceProvisioningEngineBase<T extends org.glassfish.paas.orchestrator.service.ServiceType> implements ServicePlugin {

    private static final Logger logger = LogDomains.getLogger(
            ServiceProvisioningEngineBase.class, LogDomains.PAAS_LOGGER);

    @Inject(optional = true)
    private TemplateRepository templateRepository;

    @Inject(optional = true)
    private VirtualClusters virtualClusters;

    @Inject(optional = true)
    private IAAS iaas;

    @Inject
    private ServiceUtil serviceUtil;

    @Inject(optional = true)
    VirtualMachineLifecycle vmLifecycle;

    private static final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * Create a service using the service characteristics specified in service-descripion.
     *
     * @param serviceDescription service description metadata
     * @return Basic Provisioned Service.
     * @throws ServiceProvisioningException When service creation fails.
     */
    public ProvisioningFuture<ProvisionedService> createService(ServiceDescription serviceDescription)
            throws ServiceProvisioningException {
        return createService(serviceDescription, null, null);
    }


    /**
     * Create a service using the service characteristics specified in service-descripion.
     *
     * @param serviceDescription service description metadata
     * @param allocationStrategy strategy to allocate the virtual machines within the machine pools.
     * @param listeners          list of synchronous {@link Listener} to register before starting any allocation work.
     * @return Basic Provisioned Service.
     * @throws ServiceProvisioningException When service creation fails.
     */
    public ProvisioningFuture<ProvisionedService> createService(final ServiceDescription serviceDescription,
                                                                final AllocationStrategy allocationStrategy,
                                                                final List<Listener<AllocationPhase>> listeners)
            throws ServiceProvisioningException {
        ProvisioningFuture<ProvisionedService> future =
                new ProvisioningFuture<ProvisionedService>(
                        new Callable<ProvisionedService>() {
                            public ProvisionedService call() throws Exception {
                                return _createService(serviceDescription, allocationStrategy, listeners);
                            }
                        }
                );
        executor.execute(future);
        return future;
    }

    private ProvisionedService _createService(ServiceDescription serviceDescription,
                                              AllocationStrategy allocationStrategy,
                                              List<Listener<AllocationPhase>> listeners) {
        // Note :: since allocationStrategy and listeners can not be passed to an admin
        // command, create-service can not be implemented as a command.

        // See if the service already exists.
        if (serviceUtil.isServiceAlreadyConfigured(serviceDescription.getName(),
                serviceDescription.getAppName(), null)) {
            throw new ServiceProvisioningException("Duplicate service name [" +
                    serviceDescription.getName() + "]");
        }

        String virtualClusterName = serviceDescription.getVirtualClusterName();

        TemplateIdentifier ti = serviceDescription.getTemplateIdentifier();
        ServiceCharacteristics sc = serviceDescription.getServiceCharacteristics();
        String templateId = ti != null ? ti.getId() : findTemplate(sc);

        VirtualMachine vm = createVM(templateId, virtualClusterName,
                allocationStrategy, listeners);

        Properties properties = new Properties();
        properties.setProperty("vm-id", vm.getName());
        properties.setProperty("ip-address", vm.getAddress().getHostAddress());

        return new BasicProvisionedService(serviceDescription, properties,
                ServiceStatus.RUNNING);
    }


    public ProvisionedService startService(ServiceDescription serviceDescription)
            throws ServiceProvisioningException {
        try {
            String serviceName = serviceDescription.getName();
            String appName = serviceDescription.getAppName();
            ServiceInfo serviceInfo = serviceUtil.getServiceInfo(
                    serviceName, appName, null);
            String virtualClusterName = serviceDescription.getVirtualClusterName();
            String vmId = serviceInfo.getInstanceId();
            VirtualCluster virtualCluster = virtualClusters.byName(virtualClusterName);
            VirtualMachine vm = virtualCluster.vmByName(vmId);
            Machine.State vmState = vm.getInfo().getState();
            if (vm.getMachine() == null || // TODO :: for Native mode, vm.getState() does not return correct values. Hence do this check is a workaround for Native mode.
                    vmState.equals(Machine.State.SUSPENDED) ||
                    vmState.equals(Machine.State.SUSPENDING)) {
                vmLifecycle.start(vm);
            } else {
                logger.log(Level.WARNING, "vm.running", vm.getName());
            }

            // Based on the VM state update the service info
            vmState = vm.getInfo().getState();
            if (vmState.equals(Machine.State.READY) ||
                    vmState.equals(Machine.State.RESUMING)) {
                logger.log(Level.INFO, "service.started",
                        new Object[]{serviceName, serviceDescription.getServiceType()});
            } else {
                logger.log(Level.WARNING, "vm.start.failed", vm.getName());
            }

            Properties properties = new Properties();
            properties.setProperty("vm-id", vm.getName());
            properties.setProperty("ip-address", vm.getAddress().getHostAddress());

            return new BasicProvisionedService(serviceDescription,
                    properties, ServiceStatus.RUNNING);

        } catch (Exception ex) {
            throw new ServiceProvisioningException(ex);
        }
    }

    public boolean stopService(ServiceDescription serviceDescription)
            throws ServiceProvisioningException {
        try {
            boolean stopSuccessful = true;
            String serviceName = serviceDescription.getName();
            String appName = serviceDescription.getAppName();
            ServiceInfo serviceInfo = serviceUtil.getServiceInfo(
                    serviceName, appName, null);
            String virtualClusterName = serviceDescription.getVirtualClusterName();
            String vmId = serviceInfo.getInstanceId();
            VirtualCluster virtualCluster = virtualClusters.byName(virtualClusterName);
            VirtualMachine vm = virtualCluster.vmByName(vmId);
            Machine.State vmState = vm.getInfo().getState();
            if (vmState.equals(Machine.State.READY) ||
                    vmState.equals(Machine.State.RESUMING)) {
                vmLifecycle.stop(vm);
            } else {
                logger.log(Level.WARNING, "vm.stopped", vm.getName());
                stopSuccessful = false;
            }

            // Based on the VM state update the service info.
            vmState = vm.getInfo().getState();
            if (vm.getMachine() == null || // TODO :: for Native mode, vm.getState() does not return correct values. Hence do this check is a workaround for Native mode.
                    vmState.equals(Machine.State.SUSPENDED) ||
                    vmState.equals(Machine.State.SUSPENDING)) {
                serviceUtil.updateState(serviceName, appName,
                        ServiceStatus.STOPPED.toString(), null);
                logger.log(Level.INFO, "service.stopped",
                        new Object[]{serviceName, serviceDescription.getServiceType()});
            } else {
                logger.log(Level.WARNING, "vm.stop.failed", vm.getName());
                stopSuccessful = false;
            }

            return stopSuccessful;

        } catch (Exception ex) {
            throw new ServiceProvisioningException(ex);
        }
    }


    public boolean deleteService(ServiceDescription serviceDescription) {
        ServiceInfo serviceInfo = serviceUtil.getServiceInfo(serviceDescription.getName(),
                serviceDescription.getAppName(), null);
        String virtualClusterName = serviceDescription.getVirtualClusterName();
        try {
            VirtualCluster virtualCluster = virtualClusters.byName(virtualClusterName);
            String vmId = serviceInfo.getInstanceId();
            if (vmId != null && virtualCluster.vmByName(vmId) != null) {
                VirtualMachine vm = virtualCluster.vmByName(vmId);
                vmLifecycle.delete(vm);
            }
            serviceUtil.unregisterServiceInfo(serviceDescription.getName(),
                    serviceDescription.getAppName());
            return true;
        } catch (Exception exception) {
            throw new ServiceProvisioningException(exception);
        }
    }

    // Get the ID of the template matching the service characteristics
    private String findTemplate(ServiceCharacteristics sc) {
        String templateId = null;
        if (sc != null && templateRepository != null) {
            // find the right template for the service characterstics specified.
            SearchCriteria searchCriteria = new SimpleSearchCriteria();
            searchCriteria.and(new ServiceType(sc.getCharacteristic("service-type")));
            for (Property characteristic : sc.getServiceCharacteristics()) {
                if (!"service-type".equalsIgnoreCase(characteristic.getName())) {
                    searchCriteria.and(new KeyValueType(
                            characteristic.getName(), characteristic.getValue()));
                }
            }
            Collection<TemplateInstance> matchingTemplates =
                    templateRepository.get(searchCriteria);
            if (!matchingTemplates.isEmpty()) {
                // TODO :: for now let us pick the first matching templates
                TemplateInstance matchingTemplate = matchingTemplates.iterator().next();
                if (matchingTemplates.size() > 1) {
                    logger.log(Level.WARNING, "multiple.matching.templates",
                            new Object[]{matchingTemplates, matchingTemplate});
                }
                templateId = matchingTemplate.getConfig().getName();
            } else {
                logger.log(Level.WARNING, "template.matching.failed", sc);
            }
        }
        return templateId;
    }

    private VirtualMachine createVM(String templateId, String virtualClusterName,
                                    AllocationStrategy allocationStrategy,
                                    List<Listener<AllocationPhase>> listeners)
            throws ServiceProvisioningException {

        try {
            TemplateInstance template = null;
            for (TemplateInstance ti : templateRepository.all()) {
                if (ti.getConfig().getName().equals(templateId)) {
                    template = ti;
                    break;
                }
            }
            VirtualCluster vCluster = virtualClusters.byName(virtualClusterName);
            AllocationConstraints allocationConstraints =
                    new AllocationConstraints(template, vCluster);

            PhasedFuture<AllocationPhase, VirtualMachine> future =
                    allocationStrategy != null ?
                            iaas.allocate(allocationStrategy, allocationConstraints, listeners) :
                            iaas.allocate(allocationConstraints, listeners);

            return future.get();

        } catch (Exception exception) {
            throw new ServiceProvisioningException(exception);
        }
    }

    protected VirtualMachine getVmByID(String virtualClusterName, String vmId) {
        try {
            VirtualCluster virtualCluster = virtualClusters.byName(virtualClusterName);
            return virtualCluster.vmByName(vmId);
        } catch (VirtException e) {
            throw new ServiceProvisioningException(e);
        }
    }

    public ProvisionedService getProvisionedService(ServiceDescription serviceDescription) {
        try {
            String serviceName = serviceDescription.getName();
            String appName = serviceDescription.getAppName();
            ServiceInfo serviceInfo = serviceUtil.getServiceInfo(
                    serviceName, appName, null);
            Properties properties = new Properties();
            properties.putAll(serviceInfo.getProperties());

            return new BasicProvisionedService(serviceDescription, properties,
                    serviceUtil.getServiceStatus(serviceInfo));
        } catch (Exception ex) {
            throw new ServiceProvisioningException(ex);
        }
    }
}
