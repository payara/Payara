package org.glassfish.virtualization.libvirt.commands;

import com.sun.enterprise.config.serverbeans.Domain;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.hk2.scopes.PerLookup;
import org.glassfish.virtualization.config.Virtualizations;
import org.glassfish.virtualization.libvirt.config.LibvirtVirtualization;
import org.glassfish.virtualization.util.RuntimeContext;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import java.beans.PropertyVetoException;

/**
 * Created by IntelliJ IDEA.
 * User: dochez
 * Date: 8/30/11
 * Time: 3:33 PM
 * To change this template use File | Settings | File Templates.
 */
@Service(name="add-libvirt-virtualization")
@Scoped(PerLookup.class)
public class CreateLibVirtVirtualization implements AdminCommand {

    @Param(optional=true, defaultValue = "/usr/bin/kvm")
    String emulatorPath;

    @Param(optional=true, defaultValue = "qemu#{auth.sep}#{auth.method}://#{user.name}@#{target.host}/system")
    String connectionString;

    @Param(primary = true)
    String virtName;

    @Inject
    Domain domain;

    @Override
    public void execute(AdminCommandContext context) {
        if (domain.getExtensionByType(Virtualizations.class)==null) {
            try {
                ConfigSupport.apply(new SingleConfigCode<Domain>() {
                    @Override
                    public Object run(Domain wDomain) throws PropertyVetoException, TransactionFailure {
                        Virtualizations virts = wDomain.createChild(Virtualizations.class);
                        wDomain.getExtensions().add(virts);
                        return virts;
                    }
                }, domain);
            } catch (TransactionFailure transactionFailure) {
                context.getActionReport().failure(RuntimeContext.logger,
                        "Cannot create parent virtualizations configuration",transactionFailure);
                return;
            }
        }
        Virtualizations virts = domain.getExtensionByType(Virtualizations.class);
        if (virts.byName("libvirt")!=null) {
            context.getActionReport().failure(RuntimeContext.logger,
                    "Libvirt virtualization already configured");
            return;
        }
        try {
            ConfigSupport.apply(new SingleConfigCode<Virtualizations>() {
                @Override
                public Object run(Virtualizations wVirts) throws PropertyVetoException, TransactionFailure {
                    LibvirtVirtualization virt = wVirts.createChild(LibvirtVirtualization.class);
                    virt.setName(virtName);
                    virt.setType("libvirt");
                    virt.setConnectionString(connectionString);
                    virt.setEmulatorPath(emulatorPath);
                    wVirts.getVirtualizations().add(virt);
                    return virt;
                }
            }, virts);
        } catch (TransactionFailure transactionFailure) {
                context.getActionReport().failure(RuntimeContext.logger,
                        "Cannot create libvirt virtualizations configuration", transactionFailure);
                return;
        }
        context.getActionReport().setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }
}
