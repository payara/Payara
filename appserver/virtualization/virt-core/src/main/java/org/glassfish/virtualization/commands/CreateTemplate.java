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

package org.glassfish.virtualization.commands;

import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.hk2.scopes.PerLookup;
import org.glassfish.virtualization.config.Template;
import org.glassfish.virtualization.config.TemplateIndex;
import org.glassfish.virtualization.config.Virtualizations;
import org.glassfish.virtualization.spi.TemplateRepository;
import org.glassfish.virtualization.util.RuntimeContext;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import java.beans.PropertyVetoException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Administrative command to register a new template in the registry
 * @author Jerome Dochez
 */
@Service(name="create-template")
@Scoped(PerLookup.class)
public class CreateTemplate implements AdminCommand {

    @Param(primary = true)
    String name;

    @Param
    String files;

    @Param
    String indexes;

    @Inject
    ServerEnvironment env;

    @Inject
    Virtualizations virtualizations;

    @Inject
    TemplateRepository repository;

    @Override
    public void execute(AdminCommandContext context) {

        // install the files.
        StringTokenizer st = new StringTokenizer(files, ",");
        List<File> files = new ArrayList<File>();
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
            files.add(source);
        }

        Template template;
        try {
            template =  (Template) ConfigSupport.apply(new SingleConfigCode<Virtualizations>() {
                @Override
                public Object run(Virtualizations wVirtualizations) throws PropertyVetoException, TransactionFailure {
                    Template template = wVirtualizations.createChild(Template.class);
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
                    wVirtualizations.getTemplates().add(template);
                    return template;
                }
            }, virtualizations);
        } catch (TransactionFailure e) {
            e.printStackTrace();
            context.getActionReport().failure(RuntimeContext.logger, "Failed to add the template configuration", e);
            return;
        }

        repository.installs(template, files);
    }

    private void revert(final Template template)  {
        try {
            ConfigSupport.apply(new SingleConfigCode<Virtualizations>() {
                @Override
                public Object run(Virtualizations wVirtualizations) throws PropertyVetoException, TransactionFailure {
                    wVirtualizations.getTemplates().remove(template);
                    return null;
                }
            }, virtualizations);
        } catch (TransactionFailure transactionFailure) {
            RuntimeContext.logger.log(Level.SEVERE, "Exception while cleaning up", transactionFailure);
        }
    }
}
