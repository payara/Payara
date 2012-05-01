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

import com.sun.logging.LogDomains;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.config.Named;
import org.glassfish.config.support.Create;
import org.glassfish.config.support.CrudResolver;
import javax.inject.Inject;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.*;
import org.jvnet.hk2.config.types.Property;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuration of a template, for now only its name. Will need to be refined.
 */
@Configured
public interface Template extends ConfigBeanProxy, Named {

    void setName(String name);

    @Element("*")
    List<TemplateIndex> getIndexes();

    /**
     * Defines the user identify  to be used to run anything on this template.
     * If not defined, the target machine user's name will be used.
     * @see ServerPoolConfig#getUser()
     *
     * @return  the template user information
     */
    @Element
    VirtUser getUser();
    @Create(value="create-template-user", resolver = TemplateResolver.class, i18n = @I18n("org.glassfish.virtualization.create-machine"))
    void setUser(VirtUser user);

    @Element
    List<Property> getProperties();

    @DuckTyped
    TemplateIndex byName(String name);

    /**
     * Returns the virtualization technology used by this template
     * @return the {@link Virtualization} instance
     */
    @DuckTyped
    Virtualization getVirtualization();

    public class Duck {
        public static TemplateIndex byName(Template self, String name) {
            for (TemplateIndex ti : self.getIndexes()) {
                if (ti.getType().equals(name)) {
                    return ti;
                }
            }
            return null;
        }

        public static Virtualization getVirtualization(Template self) {
            return Virtualization.class.cast(self.getParent());
        }
    }

    @Service
    class TemplateResolver implements CrudResolver {

        @Param
        String template;

        @Param
        String virtualization;

        @Inject
        Virtualizations virts;

        @Override
        public <T extends ConfigBeanProxy> T resolve(AdminCommandContext context, Class<T> type) {
            Virtualization virt = virts.byName(virtualization);
            if (virt==null) {
                Logger.getLogger(LogDomains.CONFIG_LOGGER).log(Level.SEVERE, "Cannot find a virtualization setting named " + virtualization);
                return null;
            }
            Template thisTemplate = virt.templateByName(template);
            return (T) thisTemplate;

        }
    }
}
