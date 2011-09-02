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

package org.glassfish.virtualization.runtime;

import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.Domain;
import org.glassfish.virtualization.config.VirtualMachineConfig;
import org.glassfish.virtualization.spi.TemplateInstance;
import org.glassfish.virtualization.spi.TemplateRepository;
import org.glassfish.virtualization.spi.VirtException;
import org.glassfish.virtualization.spi.VirtualMachine;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Service to register virtual machine lifecycle tokens.
 * @author Jerome Dochez
 */
@Service
public class VirtualMachineLifecycle {

    final TemplateRepository templateRepository;
    final Map<String, CountDownLatch> inStartup = new HashMap<String, CountDownLatch>();
    final Domain domain;

    public VirtualMachineLifecycle(@Inject TemplateRepository templateRepository, @Inject Domain domain) {
        this.templateRepository = templateRepository;
        this.domain = domain;
    }

    public synchronized CountDownLatch inStartup(String name) {
        CountDownLatch latch = new CountDownLatch(1);
        inStartup.put(name, latch);
        return latch;
    }

    public synchronized CountDownLatch getStartupLatch(String name) {
        return inStartup.remove(name);
    }

    public void start(VirtualMachine vm) throws VirtException {
        vm.start();
        // we do not call the customizer from here since we don't know the
        // virtual machine address, etc...
        // so we wait for the register-startup or register-virtual-machine calls
    }

    public void stop(VirtualMachine vm) throws VirtException {
        TemplateInstance ti = getTemplateInstance(vm);
        if (ti.getCustomizer()!=null)
            ti.getCustomizer().stop(vm);
        vm.stop();
    }

    public void delete(VirtualMachine vm) throws VirtException {
        TemplateInstance ti = getTemplateInstance(vm);
        if (ti.getCustomizer()!=null)
            ti.getCustomizer().stop(vm);
            ti.getCustomizer().clean(vm);
        vm.delete();
    }

    private TemplateInstance getTemplateInstance(VirtualMachine vm) {
        for (Cluster cluster : domain.getClusters().getCluster()) {
            for (VirtualMachineConfig vmc : cluster.getExtensionsByType(VirtualMachineConfig.class)) {
                if (vmc.getName().equals(vm.getName())) {
                    return templateRepository.byName(vmc.getTemplate().getName());
                }
            }
        }
        throw new RuntimeException("Cannot find registered virtual machine " + vm.getName());
    }
}
