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

import org.glassfish.virtualization.config.Template;
import org.glassfish.virtualization.runtime.VirtualCluster;
import org.glassfish.virtualization.util.VirtualizationType;
import org.jvnet.hk2.annotations.Contract;

/**
 * A template customizer is responsible for customizing a virtual
 * machine for a particular template service type.
 */
@Contract
public interface TemplateCustomizer {

    /**
     * Customize the template instance running within the passed
     * {@link VirtualMachine} instance for a particular use (like a
     * GlassFish instance, or a database). This step will be performed
     * once when the virtual machine is allocated.
     *
     * @param cluster the virtual cluster runtime information
     * @param virtualMachine the instantiated template's virtual machine
     * @throws VirtException if the customization cannot be achieved
     */
    void customize(VirtualCluster cluster, VirtualMachine virtualMachine) throws VirtException;

    /**
     * Starts the template instance services.
     * @param virtualMachine the virtual machine containing the instantiated
     * template.
     */
    void start(VirtualMachine virtualMachine);

    /**
     * Stop the template instance services
     *
     * @param virtualMachine the virtual machine containing the running services
     */
    void stop(VirtualMachine virtualMachine);

    /**
     * Clean the current virtual machine information from this process's
     * configuration, this step will be called once before the virtual machine
     * is undefined.
     *
     * @param virtualMachine the virtual machine instance to remove from our
     * configuration.
     */
    void clean(VirtualMachine virtualMachine);
}
