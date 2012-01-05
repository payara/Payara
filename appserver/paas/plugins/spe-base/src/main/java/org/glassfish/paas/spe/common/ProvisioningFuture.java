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

package org.glassfish.paas.spe.common;

import org.glassfish.paas.orchestrator.service.ServiceStatus;
import org.glassfish.paas.orchestrator.service.metadata.ServiceDescription;
import org.glassfish.paas.orchestrator.service.spi.ProvisionedService;
import org.glassfish.paas.orchestrator.service.spi.ServiceProvisioningException;
import org.glassfish.virtualization.spi.AllocationPhase;
import org.glassfish.virtualization.spi.PhasedFuture;
import org.glassfish.virtualization.spi.VirtualMachine;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

/**
 * @author bhavanishankar@java.net
 */

public class ProvisioningFuture {

    // TODO :: move constants to a separate interface
    private static final String VM_ID = "vm-id";
    private static final String VM_IP_ADDRESS = "ip-address";

    PhasedFuture<AllocationPhase, VirtualMachine> future;
    ServiceDescription serviceDescription;

    ProvisioningFuture(ServiceDescription serviceDescription,
                       PhasedFuture<AllocationPhase, VirtualMachine> future) {
        this.serviceDescription = serviceDescription;
        this.future = future;
    }

    public ProvisionedService get() throws ServiceProvisioningException {
        try {
            VirtualMachine vm = future.get();

            Properties properties = new Properties();
            properties.setProperty(VM_ID, vm.getName());
            properties.setProperty(VM_IP_ADDRESS, vm.getAddress().getHostAddress());
            return new BasicProvisionedService(serviceDescription, properties,
                    ServiceStatus.RUNNING);

        } catch (InterruptedException e) {
            throw new ServiceProvisioningException(e);
        } catch (ExecutionException e) {
            throw new ServiceProvisioningException(e);
        }
    }

}
