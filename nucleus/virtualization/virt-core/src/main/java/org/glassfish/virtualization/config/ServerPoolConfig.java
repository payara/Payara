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

import com.sun.enterprise.config.serverbeans.Domain;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.config.support.*;
import org.glassfish.virtualization.spi.Machine;
import org.jvnet.hk2.annotations.Decorate;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.*;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Provides configuration for a serverPool of machines.
 */
@Configured
@Create(value = "create-serverPool-manager", resolver = Virtualization.VirtResolver.class, i18n = @I18n("org.glassfish.virtualization.create-serverPool-manager"))
@Decorate(targetType = Domain.class, methodName = "getExtensions", with = { Create.class } )
public interface ServerPoolConfig extends ConfigBeanProxy {

    @Attribute(key = true)
    String getName();

    @Param(name="name", primary = true)
    void setName(String name);

    /**
     * Semicolon list of port names used by the serverPool master machine to receive administrative communication on.
     *  possible values are a single port name like br0, eth0, eth1... or multiple ports values like br0, eth0 and so
     * on.
     *
     * When multiple values are provided, the ports will be queried  in the order of their
     * definition until a valid IP address is obtained..
     *
     * @return list of port name used by this serverPool master to receive admin command.
     */
    @Attribute
    String getPortName();
    @Param(name="portName")
    void setPortName(String portName);

    @Attribute
    String getSubNet();

    @Param(name="subNet")
    void setSubNet(String subNet);

    @Element
    @NotNull
    VirtUser getUser();
    @Create(value="create-server-pool-user", resolver = ServerPoolResolver.class, i18n = @I18n("org.glassfish.virtualization.create-machine"))
    void setUser(VirtUser user);

    @Element
    @Create(value = "create-machine", resolver = ServerPoolResolver.class, i18n = @I18n("org.glassfish.virtualization.create-machine"))
    @Listing(value = "list-machines", resolver = ServerPoolResolver.class, i18n = @I18n("org.glassfish.virtualization.list-machines"))
    @Delete(value="delete-machine", resolver= WithinGroupResolver.class, i18n = @I18n("org.glassfish.virtualization.delete-machine"))
    List<MachineConfig> getMachines();

    @DuckTyped
    MachineConfig machineByName(String name);

    /**
     * Returns the virtualization technology used to interface with low-level machine creation/deletion/etc...
     * Possible values are libvirt, jclouds, deltacloud.
     *
     * So far only libvirt is supported.
     * @see org.glassfish.virtualization.config.Virtualization#getName()
     *
     * @return  the virtualization solution.
     */
    @DuckTyped
    Virtualization getVirtualization();

    public static class Duck {

        public static MachineConfig machineByName(ServerPoolConfig self, String name) {
            for (MachineConfig mc : self.getMachines()) {
                if (mc.getName().equals(name)) {
                    return mc;
                }
            }
            return null;
        }

        public static Virtualization getVirtualization(ServerPoolConfig self) {
            return (Virtualization) self.getParent();
        }
    }

    @Service
    public class ServerPoolResolver implements CrudResolver {
        @Param(name="serverPool")
        String group;

        @Param(optional = true)
        String virtualization;

        @Inject
        Virtualizations virts;

        @Override
        public <T extends ConfigBeanProxy> T resolve(AdminCommandContext context, Class<T> type)  {
            Virtualization virt = virtualization==null?virts.getVirtualizations().get(0):virts.byName(virtualization);
            ServerPoolConfig config = virt.serverPoolByName(group);
            if (config!=null) {
                return (T) config;
            }
            context.getActionReport().failure(context.getLogger(), "Cannot find a serverPool by the name of " + group);
            return null;
        }
    }

    @Service
    public class WithinGroupResolver extends ServerPoolResolver {
        @Param(primary=true)
        String name;

        @Override
        public <T extends ConfigBeanProxy> T resolve(AdminCommandContext context, Class<T> type)  {
            ServerPoolConfig serverPool = (ServerPoolConfig) super.resolve(context,type);
            if (serverPool!=null) {
                MachineConfig mc = serverPool.machineByName(name);
                if (mc!=null) return (T) mc;
                context.getActionReport().failure(context.getLogger(), "Cannot find a machine by the name of " + serverPool);
            }
            return null;
        }

    }
}
