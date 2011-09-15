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

package org.glassfish.virtualization.config;

import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.ClusterExtension;
import org.glassfish.api.admin.AdminCommandLock;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.config.Named;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.config.*;
import org.jvnet.hk2.config.types.Property;

import java.beans.PropertyVetoException;
import java.util.List;
import java.util.concurrent.locks.Lock;

/**
 * Persisted information about created virtual machine
 * @author Jerome Dochez
 */
@Configured
public interface VirtualMachineConfig extends Named, ConfigBeanProxy, ClusterExtension {

    @Attribute(reference = true)
    Template getTemplate();
    void setTemplate(Template template);

    @Attribute(reference = true)
    ServerPoolConfig getServerPool();
    void setServerPool(ServerPoolConfig serverPool);

    @Element
    List<Property> getProperty();

    static class Utils {
        public static VirtualMachineConfig create(final String name,
                                           final Template template,
                                           final ServerPoolConfig serverPool,
                                           final Cluster cluster) {
           try {

               // this code can happen on any thread, so we manually acquire the shared lock.
               Habitat habitat = Dom.unwrap(cluster).getHabitat();
               AdminCommandLock adminCommandLock = habitat.getComponent(AdminCommandLock.class);
               Lock lock=null;
               try {

                   lock = adminCommandLock.getLock(CommandLock.LockType.SHARED);
                   lock.lock();
                   return (VirtualMachineConfig) ConfigSupport.apply(new SingleConfigCode<Cluster>() {
                       @Override
                       public Object run(Cluster wCluster) throws PropertyVetoException, TransactionFailure {
                           VirtualMachineConfig vmConfig =
                                   wCluster.createChild(VirtualMachineConfig.class);
                           vmConfig.setName(name);
                           vmConfig.setTemplate(template);
                           vmConfig.setServerPool(serverPool);
                           wCluster.getExtensions().add(vmConfig);
                           return vmConfig;
                       }
                   }, cluster);
               } finally {
                   if (lock!=null) lock.unlock();
               }
            } catch (TransactionFailure transactionFailure) {
                throw new RuntimeException(transactionFailure);
            }
        }
    }

}
