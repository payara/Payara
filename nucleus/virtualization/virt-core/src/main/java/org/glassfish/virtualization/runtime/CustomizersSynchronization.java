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

import org.glassfish.virtualization.spi.TemplateCustomizer;
import org.glassfish.virtualization.spi.VirtException;
import org.glassfish.virtualization.spi.VirtualCluster;
import org.glassfish.virtualization.spi.VirtualMachine;
import org.glassfish.virtualization.util.RuntimeContext;
import org.jvnet.hk2.annotations.Service;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

/**
 * ensures that customizer's execution is synchronized.
 *
 * @author Jerome Dochez
 */
@Service
public class CustomizersSynchronization {

    private final ReentrantReadWriteLock customizeLock = new ReentrantReadWriteLock();


    public void customize(TemplateCustomizer customizer, VirtualCluster cluster, VirtualMachine vm ) throws VirtException {
        try {
            RuntimeContext.logger.log(Level.INFO, "Entering Customizer.customize called for " + vm.getName());
            customizeLock.writeLock().lock();
            RuntimeContext.logger.log(Level.INFO, "Customizer.customize started for " + vm.getName());
            customizer.customize(cluster, vm);
            RuntimeContext.logger.log(Level.INFO, "Customizer.customize done for " + vm.getName());
        } finally {
            customizeLock.writeLock().unlock();
        }
    }

    public void start(TemplateCustomizer customizer, VirtualMachine vm ) throws VirtException {
        try {
            RuntimeContext.logger.log(Level.INFO, "Entering Customizer.start called for " + vm.getName());
            customizeLock.readLock().lock();
            RuntimeContext.logger.log(Level.INFO, "Customizer.start start for " + vm.getName());
            customizer.start(vm, true);
            RuntimeContext.logger.log(Level.INFO, "Customizer.start done for " + vm.getName());
        } finally {
            customizeLock.readLock().unlock();
        }
    }

}

