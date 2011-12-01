package org.glassfish.virtualization.virtualbox;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.OS;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.hk2.scopes.PerLookup;
import org.glassfish.virtualization.config.Virtualizations;
import org.glassfish.virtualization.util.RuntimeContext;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import java.beans.PropertyVetoException;

/**
 * Creates the default virtual box configuration.
 */
@Service(name = "create-ims-service-virtualbox")
@Scoped(PerLookup.class)
public class CreateVirtualBoxVirtualization implements AdminCommand {

    @Param(optional=true)
    String emulatorPath=null;

    @Param(primary = true)
    String name;

    @Inject
    Domain domain;



    @Override
    public void execute(AdminCommandContext context) {
        if (emulatorPath==null) {
            emulatorPath = getDefaultEmulatorPath();
        }
        if (emulatorPath==null) {
            context.getActionReport().failure(RuntimeContext.logger, "No emulator path provided");
            return;
        }
        RuntimeContext.ensureTopLevelConfig(domain, context.getActionReport());
        if (context.getActionReport().hasFailures()) return;

        Virtualizations virts = domain.getExtensionByType(Virtualizations.class);
        if (virts.byName("libvirt")!=null) {
            context.getActionReport().failure(RuntimeContext.logger,
                    "VirtualBox virtualization already configured");
            return;
        }
        try {
            ConfigSupport.apply(new SingleConfigCode<Virtualizations>() {
                @Override
                public Object run(Virtualizations wVirts) throws PropertyVetoException, TransactionFailure {
                    VirtualBoxVirtualization virt = wVirts.createChild(VirtualBoxVirtualization.class);
                    virt.setName(name);
                    virt.setType("virtualbox");
                    virt.setEmulatorPath(emulatorPath);
                    wVirts.getVirtualizations().add(virt);
                    return virt;
                }
            }, virts);
        } catch (TransactionFailure transactionFailure) {
                context.getActionReport().failure(RuntimeContext.logger,
                        "Cannot create virtualbox virtualization configuration", transactionFailure);
                return;
        }
        context.getActionReport().setActionExitCode(ActionReport.ExitCode.SUCCESS);

    }

    private String getDefaultEmulatorPath() {
        if (OS.isDarwin()) {
            return "/Applications/VirtualBoxVirtualization.app/Contents/MacOS";
        }
        // all other cases, I don't know.
        return null;
    }
}
