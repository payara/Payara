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
 * Creates the default libvirt virtualization configuration
 * @author Jerome Dochez
 */
@Service(name="create-ims-config-libvirt")
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
        RuntimeContext.ensureTopLevelConfig(domain, context.getActionReport());
        if (context.getActionReport().hasFailures()) return;

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
