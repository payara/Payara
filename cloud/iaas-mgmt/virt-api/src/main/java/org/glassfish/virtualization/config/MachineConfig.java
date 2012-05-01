/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.config.support.Create;
import org.glassfish.config.support.CrudResolver;
import javax.inject.Inject;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.*;

import java.beans.PropertyVetoException;

/**
 * A physical machine can be represented by either a name, an IP or a mac-address
 */
@Configured
public interface MachineConfig extends ConfigBeanProxy {

    @Attribute(key=true)
    String getName();
    @Param(primary=true, name="name")
    void setName(String name);

    @Element
    String getNetworkName();
    @Param(optional = true, name="networkName")
    void setNetworkName(String networkName);

    @Element
    String getIpAddress();
    @Param(optional = true, name="ip")
    void setIpAddress(String ipAddress);

    @Element
    String getMacAddress();
    @Param(optional = true, name="mac")
    void setMacAddress(String macAddress);

    @Attribute
    String getOsName();
    void setOsName(String osName);

    /**
     * Relative path location on the target machine where the virtual machines volumes will be stored.
     * The path is relative to the configured user home directory.
     * @see #getUser()
     * @return the virtual machine volume storage path
     */
    @Attribute(defaultValue = "virt/disks")
    String getDisksLocation();

    @Param(optional = true)
    void setDisksLocation(String location);

    /**
     * Relative path location on the target machine where the virtual machines templates will be stored.
     * The path is relative to the configured user home directory
     * @see #getUser()
     * @return the virtual machine templates storage path
     */
    @Attribute(defaultValue = "virt/templates")
    String getTemplatesLocation();

    @Param(optional = true)
    void setTemplatesLocation(String location);

    /**
     * Relative path location on the target machine where the cached virtual machines templates
     * will be stored. The cache is used to quickly allocate virtual machine without requiring
     * last minute copying of templates into disks
     */
    @Attribute(defaultValue = "virt/cache")
    String getTemplateCacheLocation();
    @Param(optional=true)
    void setTemplateCacheLocation(String location);

    /**
     * Defines the user identify on the remote  machine that can be used to connect to the virtualization
     * layer and manipulate virtual machines, storage pools etc...
     * If not defined, the serverPool's user will be used.
     * @see ServerPoolConfig#getUser()
     *
     * @return  the machine user information
     */
    @Element
    VirtUser getUser();
    @Create(value="create-machine-user", resolver = MachineResolver.class, i18n = @I18n("org.glassfish.virtualization.create-machine"))
    void setUser(VirtUser user);

    @Service
    public class MachineResolver implements CrudResolver {
        @Param(name="serverPool")
        String serverPool;

        @Param(name="machine")
        String machine;

        @Param(optional = true)
        String virtualization=null;

        @Inject
        Virtualizations virts;

        @Override
        public <T extends ConfigBeanProxy> T resolve(AdminCommandContext context, Class<T> type)  {
            Virtualization virt = virtualization==null?virts.getVirtualizations().get(0):virts.byName(virtualization);
            ServerPoolConfig config = virt.serverPoolByName(serverPool);
            if (config!=null) {
                return (T) config.machineByName(machine);
            }
            context.getActionReport().failure(context.getLogger(), "Cannot find a machine by the name of " + machine);
            return null;
        }
    }

}
