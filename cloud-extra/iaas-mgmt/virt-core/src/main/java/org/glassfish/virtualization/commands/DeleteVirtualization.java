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
package org.glassfish.virtualization.commands;

import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.virtualization.config.Action;
import org.glassfish.virtualization.config.Virtualization;
import org.glassfish.virtualization.config.Virtualizations;
import org.glassfish.virtualization.util.RuntimeContext;
import javax.inject.Inject;

import org.jvnet.hk2.annotations.Optional;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import java.beans.PropertyVetoException;

/**
 * Delete a virtualization configuration
 * @author Jerome Dochez
 */
@Service(name="delete-virtualization")
@PerLookup
public class DeleteVirtualization implements AdminCommand {

    @Param(primary = true)
    String name;

    @Inject @Optional
    Virtualizations virtualizations=null;

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport ar = context.getActionReport();
        if (virtualizations==null) {
            ar.failure(RuntimeContext.logger, "No virtualization configuration found");
            return;
        }
        for (final Virtualization v : virtualizations.getVirtualizations()) {
            if (v.getName().equals(name)) {
                try {
                    ConfigSupport.apply(new SingleConfigCode<Virtualizations>() {
                        @Override
                        public Object run(Virtualizations wVirts) throws PropertyVetoException, TransactionFailure {
                            wVirts.getVirtualizations().remove(v);
                            return null;
                        }
                    }, virtualizations);
                } catch (TransactionFailure transactionFailure) {
                    ar.failure(RuntimeContext.logger, "Cannot delete " + name +
                            " virtualization config", transactionFailure);
                }
                ar.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                ar.setMessage("Configuration deleted successfully");
                return;
            }
        }
        ar.failure(RuntimeContext.logger, "Cannot find virtualization configuration named " + name);
    }
}
