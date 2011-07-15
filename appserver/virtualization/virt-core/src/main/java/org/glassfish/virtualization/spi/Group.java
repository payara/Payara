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

import org.glassfish.virtualization.config.GroupConfig;
import org.glassfish.virtualization.config.Template;
import org.glassfish.virtualization.runtime.VMTemplate;
import org.glassfish.virtualization.runtime.VirtualCluster;

import java.util.List;
import java.util.concurrent.Future;

/**
 * Abstract a provider of virtual machines.
 * @author Jerome Dochez
 */
public interface Group {

    /**
     * Returns the configuration for this group.
     *
     * @return  the group's configuration.
     */
    GroupConfig getConfig();
    void setConfig(GroupConfig config);


    /**
     * Returns this group's name.
     * @return  this group's name
     */
    String getName();

    /**
     * Returns an allocated virtual machine in this group using its name.
     * @param name virtual machine name
     * @return virtual machine instance if found or null otherwise.
     * @throws VirtException if the vm cannot be obtained
     */
    VirtualMachine vmByName(String name) throws VirtException;

    /**
     * Allocates number of virtual machines on any machine belonging to this group, each virtual machine
     * should be based on the provided template.
     * @param template  template for the virtual machines
     * @param cluster the virtual cluster instance to allocated virtual machines for
     * @param number  number of virtual machines requested.
     * @return  VirtualMachines instances
     * @throws VirtException when the virtual machine creation failed.
     */
    Iterable<Future<VirtualMachine>> allocate(Template template, VirtualCluster cluster, int number) throws VirtException;

    /**
     * Returns the list of templates installed.
     * @return list of installed templates or empty list if none
     */
    List<VMTemplate> getInstalledTemplates();

    /**
     * Install a template in this group
     * @param template to install
     */
    void install(VMTemplate template);
}
