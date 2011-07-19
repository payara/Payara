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

import com.sun.enterprise.util.io.FileUtils;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.config.Named;
import org.glassfish.config.support.Create;
import org.glassfish.config.support.CreationDecorator;
import org.glassfish.config.support.CrudResolver;
import org.glassfish.virtualization.util.RuntimeContext;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.*;

import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;

/**
 * Configuration of a template, for now only its name. Will need to be refined.
 */
@Configured
public interface Template extends ConfigBeanProxy, Named {

    void setName(String name);

    /**
     * Defines the user identify  to be used to run anything on this template.
     * If not defined, the target machine user's name will be used.
     * @see org.glassfish.virtualization.config.GroupConfig#getUser()
     *
     * @return  the template user information
     */
    @Element
    VirtUser getUser();
    @Create(value="create-template-user", resolver = TemplateResolver.class, i18n = @I18n("org.glassfish.virtualization.create-machine"))
    void setUser(VirtUser user);

    @Service
    class TemplateResolver implements CrudResolver {

        @Param
        String templateName;

        @Param
        String virtName;

        @Inject
        Virtualizations virts;

        @Override
        public <T extends ConfigBeanProxy> T resolve(AdminCommandContext context, Class<T> type) {
            Virtualization virt = virts.byName(virtName);
            if (virt==null) {
                context.getActionReport().failure(RuntimeContext.logger, "Cannot find virtualization config named " + virtName);
                return null;
            }
            Template thisTemplate = virt.templateByName(templateName);
            return (T) thisTemplate;

        }
    }

    @Service
    class TemplateAddDecorator implements CreationDecorator<Template> {

        @Param
        String location=null;

        @Param
        String xml=null;

        @Param(primary=true)
        String name;

        @Inject
        Virtualizations virt;

        @Override
        public void decorate(AdminCommandContext context, Template instance) throws TransactionFailure, PropertyVetoException {
            instance.setName(name);
            // copy the template inside our templates directory.
            File templateLocation = new File(virt.getTemplatesLocation());
            if (!templateLocation.exists()) {
                assert templateLocation.mkdirs();
            }
            if (location!=null) {
                try {
                    File source = new File(location);
                    if (!source.isAbsolute()) {
                        source = new File(System.getProperty("user.dir"), location);
                    }
                    if (!source.exists()) {
                        context.getActionReport().failure(RuntimeContext.logger, "File not found : " + source.getAbsolutePath());
                        return;
                    }
                    // Preserve filename extension if there is one
                    String extension = "";
                    int index = location.lastIndexOf('.');
                    if (index > 0) {
                        extension = location.substring(index);
                    }
                    FileUtils.copy(source, new File(templateLocation, instance.getName() + extension));
                } catch(IOException e) {
                    context.getActionReport().failure(RuntimeContext.logger, "Error copying template " + location, e);
                }
            }
            if (xml!=null) {
                try {
                    File source = new File(xml);
                    if (!source.isAbsolute()) {
                        source = new File(System.getProperty("user.dir"), xml);
                    }
                    if (!source.exists()) {
                        context.getActionReport().failure(RuntimeContext.logger, "File not found : " + source.getAbsolutePath());
                        File f = new File(templateLocation, instance.getName() + ".img");
                        if (!(f.delete())) {
                            RuntimeContext.logger.warning(f.getAbsolutePath() + " cannot be deleted");
                        }
                        throw new TransactionFailure("Cannot find file " + source.getAbsolutePath());
                    }
                    FileUtils.copy(source, new File(templateLocation, instance.getName() + ".xml"));
                } catch(IOException e) {
                    File f = new File(templateLocation, instance.getName() + ".img");
                    if (!f.delete()) {
                        RuntimeContext.logger.warning(f.getAbsolutePath() + " cannot be deleted");
                    }
                    context.getActionReport().failure(RuntimeContext.logger, "Error copying xml " + location, e);
                }
            }
        }
    }
}
