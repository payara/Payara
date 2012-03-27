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

package org.glassfish.virtualization.commands;

/**
 * Creates a virtual cluster
 */

import com.sun.enterprise.config.serverbeans.Domain;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.virtualization.config.Virtualizations;
import org.glassfish.virtualization.spi.VirtualCluster;
import org.glassfish.virtualization.runtime.VirtualClusters;
import org.glassfish.virtualization.spi.*;
import org.glassfish.virtualization.util.RuntimeContext;
import org.glassfish.virtualization.util.ServiceType;
import org.glassfish.virtualization.virtmgt.GroupAccess;
import org.glassfish.virtualization.virtmgt.GroupsAccess;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.glassfish.api.admin.CommandLock;

@Service(name="create-virtual-cluster")
@Scoped(PerLookup.class)
@CommandLock(CommandLock.LockType.NONE)
public class CreateVirtualCluster implements AdminCommand {

    @Param(optional = true)
    String groupNames=null;

    @Param(optional=true)
    String template=null;

    @Param(primary = true)
    String name;

    @Param(optional = true, defaultValue = "1")
    String min = "1";

    @Param(optional = true, defaultValue = "5")
    String max = "5";

    @Inject
    GroupsAccess groups;

    @Inject(optional=true)
    Virtualizations virts=null;

    @Inject(optional=true)
    TemplateRepository templateRepository;

    @Inject
    Domain domain;

    @Inject
    RuntimeContext rtContext;

    @Inject
    IAAS iaas;

    @Inject
    VirtualClusters virtualClusters;

    @Override
    public void execute(final AdminCommandContext context) {

        if (virts==null) {
            context.getActionReport().failure(RuntimeContext.logger, "No virtualization configuration present");
            return;
        }
        try {
            if (Integer.parseInt(min)>Integer.parseInt(max)) {
                context.getActionReport().failure(RuntimeContext.logger, "Invalid parameters, min > max");
                return;
            }
        } catch(NumberFormatException e) {
            context.getActionReport().failure(RuntimeContext.logger, e.getMessage(), e);
            return;
        }

        List<GroupAccess> targetGroups = new ArrayList<GroupAccess>();
        if (groupNames==null) {
            for (GroupAccess group : groups.groups()) {
                targetGroups.add(group);
            }
        } else {
            StringTokenizer tokenizer = new StringTokenizer(groupNames, ",");
            while (tokenizer.hasMoreElements()) {
                String groupName = tokenizer.nextToken();
                GroupAccess group = groups.byName(groupName);
                if (group==null) {
                    context.getActionReport().failure(RuntimeContext.logger, "There are not defined groups named " + groupName);
                    return;
                }
                targetGroups.add(group);
            }
        }
        if (targetGroups.isEmpty()) {
            context.getActionReport().failure(RuntimeContext.logger, "There are not defined groups to deploy to");
            return;
        }

        ActionReport report = context.getActionReport();
        final StringBuilder sb = new StringBuilder();
        int minNumber = Integer.parseInt(min);
        sb.append("Successfully created ").append(minNumber).append(" virtual machine(s) : ");

        TemplateInstance templateInstance=null;
        if (template!=null) {
            for (TemplateInstance ti : templateRepository.all()) {
                if (ti.getConfig().getName().equals(template)) {
                    templateInstance=ti;
                    break;
                }
            }
        } else {
            ServiceType serviceType = new ServiceType("JavaEE");

            // we need to reconcile with the virtualization technology
            for (TemplateInstance ti : templateRepository.all()) {
                if (ti.satisfies(serviceType)) {
                    templateInstance = ti;
                    break;
                }
            }
        }

        if (templateInstance==null) {
            context.getActionReport().failure(RuntimeContext.logger, "Cannot find template appropriate template");
            return;
        }

        rtContext.executeAdminCommand(report, "create-cluster", name);
        if (report.hasFailures()) {
            return;
        }

        try {
            VirtualCluster vCluster = virtualClusters.byName(name);
            final List<PhasedFuture<AllocationPhase, VirtualMachine>> futures =
                    new ArrayList<PhasedFuture<AllocationPhase, VirtualMachine>>();

            for (int i=0;i<minNumber;i++) {
                futures.add(iaas.allocate(new AllocationConstraints(templateInstance, vCluster), null));
            }

            for (PhasedFuture<AllocationPhase, VirtualMachine> future : futures) {
                VirtualMachine vm;
                try {
                    vm = future.get();
                } catch(Exception e) {
                    context.getActionReport().failure(RuntimeContext.logger, "Failure to allocate virtual machine ", e);
                    return;
                }
                sb.append(vm.getName()).append( "(").append(vm.getAddress()).append(") ");
            }
        } catch (VirtException e) {
                context.getActionReport().failure(RuntimeContext.logger, "Cannot allocate virtual machine", e);
                rtContext.executeAdminCommand(report, "delete-cluster", name);
                return;
        }

        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        report.setMessage(sb.toString());
    }
}
