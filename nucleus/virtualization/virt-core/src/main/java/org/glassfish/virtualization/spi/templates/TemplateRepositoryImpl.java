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

package org.glassfish.virtualization.spi.templates;

import com.sun.enterprise.util.io.FileUtils;
import com.sun.logging.LogDomains;
import org.glassfish.hk2.Services;
import org.glassfish.virtualization.config.ServerPoolConfig;
import org.glassfish.virtualization.config.Template;
import org.glassfish.virtualization.config.Virtualization;
import org.glassfish.virtualization.config.Virtualizations;
import org.glassfish.virtualization.runtime.LocalTemplate;
import org.glassfish.virtualization.runtime.RemoteTemplate;
import org.glassfish.virtualization.runtime.VMTemplate;
import org.glassfish.virtualization.spi.*;
import org.glassfish.virtualization.util.RuntimeContext;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of the {@link TemplateRepository} interface
 * @author Jerome Dochez
 */
@Service
public class TemplateRepositoryImpl implements TemplateRepository {

    final File location;
    final Logger logger = LogDomains.getLogger(TemplateRepositoryImpl.class, LogDomains.VIRTUALIZATION_LOGGER);
    final List<TemplateInstance> templates = new ArrayList<TemplateInstance>();
    final Services services;

    public TemplateRepositoryImpl(@Inject Services services, @Inject Virtualizations virts) {
        location = new File(virts.getTemplatesLocation());
        this.services = services;
        for (Virtualization virt : virts.getVirtualizations()) {
            for (Template template : virt.getTemplates()) {
                templates.add(new TemplateInstanceImpl(services, template));
            }
        }
    }

    @Override
    public boolean installs(Template config, Collection<File> files) {
        String templateName = config.getName();
        File templateLocation = new File(location, templateName);
        if (templateLocation.exists()) {
            if (!FileUtils.whack(templateLocation)) {
                logger.severe("Template not installed, cannot delete existing template directory "
                        + templateLocation.getAbsolutePath());
                return false;
            }
        }
        if (!templateLocation.mkdirs()) {
            logger.severe("Cannot create template location directory at " + templateLocation.getAbsolutePath());
            return false;
        }
        for (File f : files) {
            try {
                File destination = new File(templateLocation, f.getName());
                FileUtils.copy(f, destination);
                destination.setLastModified(f.lastModified());
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Cannot copy file " + f.getAbsolutePath(), e);
                FileUtils.whack(templateLocation);
                return false;
            }
        }
        TemplateInstance templateInstance = new TemplateInstanceImpl(services, config);
        templates.add(templateInstance);

        // now we should copy this template to all available server pools.
        Virtualization virt = (Virtualization) config.getParent();
        for (ServerPoolConfig pool : virt.getServerPools()) {
            ServerPool serverPool = services.forContract(IAAS.class).get().byName(pool.getName());
            try {
                serverPool.install(templateInstance);
            } catch (IOException e) {
                RuntimeContext.logger.log(Level.SEVERE, "Cannot copy template " + config.getName()
                        + " on server pool  " + config.getName(), e);
            }
        }

        return true;
    }

    @Override
    public boolean delete(Template config) {
        String templateName = config.getName();
        File templateLocation = new File(location, templateName);
        for (TemplateInstance ti : templates) {
            if (ti.getConfig().getName().equals(config.getName())) {
                templates.remove(ti);
                break;
            }
        }
        if (templateLocation.exists()) {
            return FileUtils.whack(templateLocation);
        }
        return true;
    }

    @Override
    public Collection<TemplateInstance> get(SearchCriteria criteria) {
        // first all the mandatory
        List<TemplateInstance> candidates = new ArrayList<TemplateInstance>(templates);
        for (TemplateCondition templateIndex : criteria.and()) {
            for (TemplateInstance templateInstance : new ArrayList<TemplateInstance>(candidates)) {
                if (!templateInstance.satisfies(templateIndex)) {
                    candidates.remove(templateInstance);
                }
            }
        }
        // now the OR.
        if (!criteria.or().isEmpty()) { // if no OR conditions are specified, retain all the previously matched candidates.
            for (TemplateInstance templateInstance : new ArrayList<TemplateInstance>(candidates)) {
                boolean foundOne = false;
                for (TemplateCondition templateIndex : criteria.or()) {
                    if (templateInstance.satisfies(templateIndex)) {
                        foundOne = true;
                        break;
                    }
                }
                if (!foundOne) candidates.remove(templateInstance);
            }
        }
        // todo optionals
        return candidates;
    }

    @Override
    public Collection<TemplateInstance> all() {
        return Collections.unmodifiableCollection(templates);
    }

    @Override
    public TemplateInstance byName(String name) {
        for (TemplateInstance ti :templates) {
            if (ti.getConfig().getName().equals(name)) {
                return ti;
            }
        }
        return null;
    }
}
