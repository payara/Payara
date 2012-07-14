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
package org.glassfish.virtualization.virtualbox;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.OS;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.virtualization.config.Virtualizations;
import org.glassfish.virtualization.util.RuntimeContext;
import javax.inject.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import java.beans.PropertyVetoException;

/**
 * Creates the default virtual box configuration.
 */
@Service(name = "create-ims-config-virtualbox")
@PerLookup
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
        if (virts.byName("virtualbox")!=null) {
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
