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

import org.glassfish.hk2.Services;
import org.glassfish.hk2.inject.Injector;
import org.glassfish.virtualization.config.Template;
import org.glassfish.virtualization.config.TemplateIndex;
import org.glassfish.virtualization.config.Virtualization;
import org.glassfish.virtualization.config.Virtualizations;
import org.glassfish.virtualization.spi.TemplateCondition;
import org.glassfish.virtualization.spi.TemplateCustomizer;
import org.glassfish.virtualization.spi.TemplateInstance;
import org.jvnet.hk2.annotations.Inject;

import java.beans.Customizer;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the {@link TemplateInstance} interface
 * @author Jerome Dochez
 */
public class TemplateInstanceImpl implements TemplateInstance {

    final Template config;
    final List<TemplateCondition> indexes = new ArrayList<TemplateCondition>();
    final TemplateCustomizer customizer;
    final Virtualizations virtualizations;

    public TemplateInstanceImpl(Services services, Template config) {
        this.config = config;
        TemplateCustomizer tmpCustomizer = null;
        for (TemplateIndex indexPersistence : config.getIndexes()) {
            indexes.add(TemplateCondition.from(indexPersistence));
        }
        // todo : need to do better
        // so far, it's ugly, customizers must use the (VirtualizationType-ServiceType) name.
        TemplateIndex serviceType  = config.byName("ServiceType");
        TemplateIndex virtType = config.byName("VirtualizationType");
        TemplateCustomizer cust = services.forContract(TemplateCustomizer.class).named(
                        virtType.getValue()+ "-"+serviceType.getValue()).get();
        customizer = cust==null?services.forContract(TemplateCustomizer.class).named(serviceType.getValue()).get():cust;
        virtualizations = services.forContract(Virtualizations.class).get();
    }

    @Override
    public Template getConfig() {
        return config;
    }

    @Override
    public boolean satisfies(TemplateCondition condition) {
        for (TemplateCondition templateIndex : indexes) {
            if (templateIndex.satisfies(condition)) return true;
        }
        return false;
    }

    @Override
    public TemplateCustomizer getCustomizer() {
        return customizer;
    }

    @Override
    public File getLocation() {
        return new File(virtualizations.getTemplatesLocation(), getConfig().getName());
    }

    public File getFileByExtension(final String extension) throws FileNotFoundException {
        String[] fileNames = getLocation().list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(extension);
            }
        });
        if (fileNames==null || fileNames.length==0) {
            throw new FileNotFoundException("Cannot find any file with " + extension + " extension");
        }
        return new File(getLocation(), fileNames[0]);
    }
}
