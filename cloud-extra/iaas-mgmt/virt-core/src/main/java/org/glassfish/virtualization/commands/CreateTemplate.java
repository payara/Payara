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

package org.glassfish.virtualization.commands;

import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.virtualization.config.Template;
import org.glassfish.virtualization.config.TemplateIndex;
import org.glassfish.virtualization.config.Virtualization;
import org.glassfish.virtualization.config.Virtualizations;
import org.glassfish.virtualization.spi.TemplateRepository;
import org.glassfish.virtualization.util.RuntimeContext;
import javax.inject.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.types.Property;

import java.beans.PropertyVetoException;
import java.io.File;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Administrative command to register a new template in the registry
 * @author Jerome Dochez
 */
@Service(name="create-template")
@PerLookup
public class CreateTemplate implements AdminCommand {

    @Param(primary = true)
    String name;

    @Param(optional=true)
    String files=null;

    @Param
    String indexes;

    @Param(optional = true, separator=':')
    Properties properties;

    @Param(optional = true)
    String virtualization=null;

    @Inject
    ServerEnvironment env;

    @Inject
    Virtualizations virtualizations;

    @Inject
    TemplateRepository repository;

    @Override
    public void execute(AdminCommandContext context) {

        // install the files.
        List<File> templateFiles = new ArrayList<File>();
        if (files!=null) {
            StringTokenizer st = new StringTokenizer(files, ",");
            while (st.hasMoreTokens()) {
                String file = st.nextToken();
                File source = new File(file);

                if (!source.isAbsolute()) {
                    source = new File(System.getProperty("user.dir"), file);
                }
                if (!source.exists()) {
                    context.getActionReport().failure(RuntimeContext.logger, "File not found : " + source.getAbsolutePath());
                    return;
                }
                templateFiles.add(source);
            }
        }
        if (virtualizations.getVirtualizations().size()==0) {
            context.getActionReport().failure(RuntimeContext.logger, "No virtualization settings in the configuration");
            return;
        }
        Virtualization virt = virtualization==null?
                virtualizations.getVirtualizations().get(0):virtualizations.byName(virtualization);


        Template template;
        try {
            template =  (Template) ConfigSupport.apply(new SingleConfigCode<Virtualization>() {
                @Override
                public Object run(Virtualization wVirtualization) throws PropertyVetoException, TransactionFailure {
                    Template template = wVirtualization.createChild(Template.class);
                    template.setName(name);

                    final StringTokenizer st = new StringTokenizer(indexes, ",");
                    while (st.hasMoreElements()) {
                        String index = st.nextToken();
                        Pattern pattern = Pattern.compile("([^\\=]*)=(.*)");
                        Matcher matcher  = pattern.matcher(index);
                        if (matcher.matches()) {
                            final String indexType = matcher.group(1);
                            final String indexValue = matcher.group(2);
                            TemplateIndex indexPersistence = template.createChild(TemplateIndex.class);
                            indexPersistence.setType(indexType);
                            indexPersistence.setValue(indexValue);
                            template.getIndexes().add(indexPersistence);

                        }
                    }
                    /**final StringTokenizer props = new StringTokenizer(properties, ",");
                    while (props.hasMoreElements()) {
                        String index = props.nextToken();
                        Pattern pattern = Pattern.compile("([^\\=]*)=(.*)");
                        Matcher matcher  = pattern.matcher(index);
                        if (matcher.matches()) {
                            final String indexType = matcher.group(1);
                            final String indexValue = matcher.group(2);
                            Property property = template.createChild(Property.class);
                            property.setName(indexType);
                            property.setValue(indexValue);
                            template.getProperties().add(property);

                        }
                    }  */
                    if (properties!=null) {
                        for (String propName : properties.stringPropertyNames()) {
                            Property property = template.createChild(Property.class);
                            property.setName(propName);
                            property.setValue(properties.getProperty(propName));
                            template.getProperties().add(property);

                        }
                    }
                    wVirtualization.getTemplates().add(template);
                    return template;
                }
            }, virt);
        } catch (TransactionFailure e) {
            e.printStackTrace();
            context.getActionReport().failure(RuntimeContext.logger, "Failed to add the template configuration", e);
            return;
        }

        repository.installs(template, templateFiles);
    }
}
