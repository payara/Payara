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
package org.glassfish.virtualization.spi;

import org.glassfish.virtualization.runtime.VirtualCluster;

import java.util.*;

/**
 * Virtual machine allocation constraints such as groups to privilege or ignore.
 *
 * @author Jerome Dochez
 */
public class AllocationConstraints {

    final TemplateInstance template;
    final VirtualCluster targetCluster;
    final List<ServerPool> groups = new ArrayList<ServerPool>();
    final List<VirtualMachine> noColocationList = new ArrayList<VirtualMachine>();
    final Properties vmProps = new Properties();

    public AllocationConstraints(TemplateInstance template, VirtualCluster targetCluster) {
        this.template = template;
        this.targetCluster = targetCluster;
    }

    /**
     * Returns the virtual cluster this allocation is targeted to
     * @return the target virtual cluster
     */
    public VirtualCluster getTargetCluster() {
        return targetCluster;
    }

    /**
     * Returns the requested characteristics for the virtual machines
     * @return a set of static virtual machine characteristics to use when
     * allocating the new virtual machines or null if the template characteristics
     * should apply.
     */
    public StaticVirtualMachineInfo getCharacteristics() {
        return null;
    }

    /**
     * Specifies the serverPool in which the number of virtual machines should be allocated.
     * If no serverPool is specified, it's left to the Infrastructure Management Service to
     * decide in which groups those Virtual Machines will be allocated.
     *
     * @param groups desired serverPool instance
     * @return itself
     */
    public AllocationConstraints in(ServerPool... groups) {
        this.groups.addAll(Arrays.asList(groups));
        return this;
    }

    /**
     * Specifies the virtual machines that should not be co-located on the same hardware
     * with the new allocated virtual machines. This is particularly useful when willing
     * to allocate replication instances for existing virtual machines so they do not end
     * up being running on the same hardware resource.
     *
     * @param vms list of virtual machines to not co-locate with.
     * @return itself.
     */
    public AllocationConstraints noColocationWith(VirtualMachine... vms) {
        this.noColocationList.addAll(Arrays.asList(vms));
        return this;
    }

    /**
     * Returns the properties for a specific virtual machine allocation. These properties
     * will be passed to the virtual machine through a provider specific mechanism. Such
     * properties can be used by the virtual machine to configure itself.
     *
     * @return the virtual machine properties
     */
    public Properties getVirtualMachineProperties() {
        return vmProps;
    }

    /**
     * Returns the groups this set of virtual machine allocations should be allocated into
     *
     * @return the groups we should use to allocate the virtual machines if possible.
     */
    public Collection<ServerPool> affinities() {
        return Collections.unmodifiableList(groups);
    }

    /**
     * Returns a list of virtual machine we would like the new virtual machines to not be
     * co-located with (meaning not running on the same hardware resource).
     *
     * @return a list of virtual machines to no co-locate new ones with.
     */
    public List<VirtualMachine> separateFrom() {
        return Collections.unmodifiableList(noColocationList);
    }

    /**
     * Returns the template associated with the virtual machine order.
     *
     * @return the template to use for the virtual machines allocation.
     */
    public TemplateInstance getTemplate() {
        return template;
    }
}
