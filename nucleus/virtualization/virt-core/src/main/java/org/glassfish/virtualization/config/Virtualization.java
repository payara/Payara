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

import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.config.support.*;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.*;

import javax.validation.constraints.NotNull;
import java.beans.PropertyVetoException;
import java.util.List;

/**
 * Configuration about a particular virtualization technology.
 *
 * @author Jerome Dochez
 */
@Configured
public interface Virtualization extends ConfigBeanProxy {

    /**
     * Virtualization key name, that will be used as reference index by other configuration element like
     * templates, serverPool definitions etc..
     * @see ServerPoolConfig#getVirtualization() )
     * @return  the virtualization name
     */
    @Attribute(key=true)
    String getName();
    void setName(String solution);

    @Attribute
    @NotNull
    String getType();
    void setType(String virtType);

    /**
     * Number of template disks the cache should maintain. These disks will be used to
     * create virtual machine avoiding last minute copy.
     *
     * @return template cache size
     */
    @Attribute(defaultValue = "5")
    String getTemplateCacheSize();
    void setTemplateCacheSize(String cache);

    /**
     * Refresh rate in seconds for the template caching facility
     *
     * @return the refresh rate of the cache in seconds
     */
    @Attribute(defaultValue = "120")
    String getTemplateCacheRefreshRate();
    void setTemplateCacheRefreshRate(String cache);

    /**
     * Returns the list of registered templates for this virtualization infrastructure. Such template
     * are image files that can be duplicated to create virtual machines.
     *
     * @return  list of registered templates
     */
    @Element("template")
    @Listing(value = "list-templates", resolver = VirtResolver.class, i18n = @I18n("org.glassfish.virtualization.list-templates"))
    @Delete(value="remove-template", resolver = TypeAndNameResolver.class, i18n = @I18n("org.glassfish.virtualization.remove-template"))
    List<Template> getTemplates();

    @Element("server-pool-master")
//    @Create(value = "create-serverPool-manager", resolver = VirtResolver.class, i18n = @I18n("org.glassfish.virtualization.create-serverPool-manager"))
    @Listing(value = "list-group-master", resolver = VirtResolver.class, i18n = @I18n("org.glassfish.virtualization.list-serverPool-managers"))
    @Delete(value="delete-group-master", resolver= TypeAndNameResolver.class, i18n = @I18n("org.glassfish.virtualization.delete-serverPool-manager"))
    List<GroupManager> getServerPoolMaster();

    /**
     * This really should not be here mixing clients and providers information , but for prototyping
     * it's fine.
     */
    @Element("server-pool")
    @Create(value = "create-server-pool", resolver = VirtResolver.class, decorator = ServerPoolDecorator.class, i18n = @I18n("org.glassfish.virtualization.create-virt-serverPool"))
    @Listing(value = "list-server-pools", resolver = VirtResolver.class, i18n = @I18n("org.glassfish.virtualization.list-virt-groups"))
    @Delete(value="delete-server-pool", resolver= TypeAndNameResolver.class, i18n = @I18n("org.glassfish.virtualization.list-virt-serverPool"))
    List<ServerPoolConfig> getServerPools();

    /**
     * For future use, we can have scripts attached to machine/virtual machine lifecycle that could be
     * triggered at each event.
     * @see Action
     * @return the native script location
     */
    @Element
    String getScriptsLocation();
    void setScriptsLocation(String location);

    /**
     * Returns the list of @see Action for this virtualization infrastructure.
     * @return  list of registeed actions
     */
    @Element("action")
    List<Action> getActions();

    /**
     * Looks up a registered template by the name and returns it.
     * @param name template name
     * @return the template if found or null otherwise
     */
    @DuckTyped
    Template templateByName(String name);

    /**
     * Returns a serverPool configuration using a serverPool name
     * @param name the serverPool name
     * @return group configuration or null if not found
     */
    @DuckTyped
    ServerPoolConfig serverPoolByName(String name);

    @Service
    public class VirtResolver implements CrudResolver {

        @Param
        String virtualization;

        @Inject
        Virtualizations virtualizations;

        @Override
        @SuppressWarnings("unchecked")
        public <T extends ConfigBeanProxy> T resolve(AdminCommandContext context, Class<T> type)  {
            return (T) virtualizations.byName(virtualization);
        }
    }

    public static class Duck {
        public static Template templateByName(Virtualization self, String name) {
            for (Template template : self.getTemplates()) {
                if (template.getName().equals(name)) {
                    return template;
                }
            }
            return null;
        }

        public static ServerPoolConfig serverPoolByName(Virtualization self, String name) {
            for (ServerPoolConfig groupConfig : self.getServerPools()) {
                if (groupConfig.getName().equals(name)) {
                    return groupConfig;
                }
            }
            return null;
        }
    }

    // ToDo : I should not have to do this, the CRUD framework should do it for me since VirtUser is annotated with @NotNull
    @Service
    public class ServerPoolDecorator implements CreationDecorator<ServerPoolConfig> {

        @Override
        public void decorate(AdminCommandContext context, ServerPoolConfig instance) throws TransactionFailure, PropertyVetoException {
            VirtUser user = instance.createChild(VirtUser.class);
            instance.setUser(user);
        }
    }
}
