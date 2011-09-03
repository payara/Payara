package org.glassfish.virtualization.commands;

import com.sun.enterprise.config.serverbeans.Cluster;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.Supplemental;
import org.glassfish.hk2.Services;
import org.glassfish.virtualization.config.TemplateIndex;
import org.glassfish.virtualization.config.VirtualMachineConfig;
import org.glassfish.virtualization.config.Virtualizations;
import org.glassfish.virtualization.runtime.VirtualCluster;
import org.glassfish.virtualization.runtime.VirtualClusters;
import org.glassfish.virtualization.runtime.VirtualMachineLifecycle;
import org.glassfish.virtualization.spi.VirtException;
import org.glassfish.virtualization.spi.VirtualMachine;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: dochez
 * Date: 9/2/11
 * Time: 1:54 PM
 * To change this template use File | Settings | File Templates.
 */
@Service
@Supplemental(value = "stop-cluster", on= Supplemental.Timing.After )
@Scoped(PerLookup.class)
@CommandLock(CommandLock.LockType.NONE)
public class SupplementalStopClusterCommand implements AdminCommand {

    @Param(optional = false, primary = true)
    private String clusterName;

    @Inject
    VirtualClusters virtualClusters;

    @Inject
    Services services;

    @Override
    public void execute(AdminCommandContext context) {
        try {
            Virtualizations virtualizations = services.forContract(Virtualizations.class).get();
            if (virtualizations==null) {
                context.getActionReport().setActionExitCode(ActionReport.ExitCode.SUCCESS);
                return;
            }
            VirtualCluster virtualCluster = virtualClusters.byName(clusterName);
            if (virtualCluster==null) {
                context.getActionReport().setActionExitCode(ActionReport.ExitCode.SUCCESS);
                return;
            }
            VirtualMachineLifecycle vmLifecycle = services.forContract(VirtualMachineLifecycle.class).get();
            Cluster cluster =  virtualCluster.getConfig();
            List<VirtualMachineConfig> vmConfigs = cluster.getExtensionsByType(VirtualMachineConfig.class);
            for (VirtualMachineConfig vmConfig : vmConfigs) {
                if (handleVM(vmConfig)) {
                    VirtualMachine vm = virtualCluster.vmByName(vmConfig.getName());
                    vmLifecycle.stop(vm);
                }
            }
        } catch(VirtException e) {

        }
    }

    /**
     * Return true if we should manually start the virtual machine. In particular, we
     * don't automatically JavaEE virtual machines.
     *
     * @param vmConfig the vm configuration
     * @return true if we should start this virtual machine
     */
    private boolean handleVM(VirtualMachineConfig vmConfig) {
        for (TemplateIndex ti : vmConfig.getTemplate().getIndexes()) {
            if (ti.getType().equals("ServiceType") && ti.getValue().equals("JavaEE")) return false;
        }
        return true;

    }

}
