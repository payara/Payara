/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tests.paas.externaldbservicetest.generatederbyvm;

import com.sun.enterprise.util.OS;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.virtualization.runtime.VirtualClusters;
import org.glassfish.virtualization.spi.AllocationConstraints;
import org.glassfish.virtualization.spi.AllocationPhase;
import org.glassfish.virtualization.spi.IAAS;
import org.glassfish.virtualization.spi.KeyValueType;
import org.glassfish.virtualization.spi.PhasedFuture;
import org.glassfish.virtualization.spi.SearchCriteria;
import org.glassfish.virtualization.spi.TemplateInstance;
import org.glassfish.virtualization.spi.TemplateRepository;
import org.glassfish.virtualization.spi.VirtualCluster;
import org.glassfish.virtualization.spi.VirtualMachine;
import org.glassfish.virtualization.util.ServiceType;
import org.glassfish.virtualization.util.SimpleSearchCriteria;
import javax.inject.Inject;
import org.jvnet.hk2.annotations.Optional;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;

import java.io.File;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Properties;

/**
 * @author Shalini M
 */
@Service(name = "create-derby-vm")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
public class CreateDerbyVM implements AdminCommand {

    @Param(name = "servicecharacteristics", optional = true, separator = ':')
    public Properties serviceCharacteristics;

    @Inject @Optional
    private IAAS iaas;

    @Inject @Optional
    private VirtualClusters virtualClusters;

    @Inject @Optional
    private TemplateRepository templateRepository;

    @Inject
    private CommandRunner commandRunner;

    @Param(name = "virtualcluster", optional = true, defaultValue = "db-external-service-test-cluster")
    private String virtualClusterName;

    private static final MessageFormat ASADMIN_COMMAND = new MessageFormat(
            "{0}" + File.separator + "lib" + File.separator + "nadmin" +
                    (OS.isWindows() ? ".bat" : "")); // {0} must be install root.

    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();

        try {
            String templateId = findTemplate(serviceCharacteristics);

            TemplateInstance ti = templateRepository.byName(templateId);

            commandRunner.run("create-cluster", virtualClusterName);

            VirtualCluster vCluster = virtualClusters.byName(virtualClusterName);

            PhasedFuture<AllocationPhase, VirtualMachine> future =
                    iaas.allocate(new AllocationConstraints(ti, vCluster), null);

            VirtualMachine vm = future.get();

            runAsadminCommand("start-database", vm);
            report.setMessage("\n" + vm.getAddress().getHostAddress());

        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }

    private String findTemplate(Properties sc) {
        String templateId = null;
        if (sc != null && templateRepository != null) {
            // find the right template for the service characterstics specified.
            SearchCriteria searchCriteria = new SimpleSearchCriteria();
            searchCriteria.and(new ServiceType(sc.getProperty("service-type")));
            for (Object characteristic : sc.keySet()) {
                if (!"service-type".equalsIgnoreCase((String) characteristic)) {
                    searchCriteria.and(new KeyValueType(
                            (String) characteristic, sc.getProperty((String) characteristic)));
                }
            }
            Collection<TemplateInstance> matchingTemplates =
                    templateRepository.get(searchCriteria);
            if (!matchingTemplates.isEmpty()) {
                // TODO :: for now let us pick the first matching templates
                TemplateInstance matchingTemplate = matchingTemplates.iterator().next();
                templateId = matchingTemplate.getConfig().getName();
            } else {
                throw new RuntimeException("no template found");
            }
        }
        return templateId;
    }

    public void runAsadminCommand(String commandName, VirtualMachine virtualMachine) {
        String[] installDir = {virtualMachine.getProperty(VirtualMachine.PropertyName.INSTALL_DIR) +
                File.separator + "glassfish"};

        String[] args = {ASADMIN_COMMAND.format(installDir).toString(),
                commandName};
        try {
            String output = virtualMachine.executeOn(args);
            Object[] params = new Object[]{virtualMachine.getName(), output};
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

}
