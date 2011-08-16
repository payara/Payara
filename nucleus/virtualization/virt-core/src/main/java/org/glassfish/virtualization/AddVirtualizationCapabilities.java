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

package org.glassfish.virtualization;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.module.bootstrap.Populator;
import com.sun.logging.LogDomains;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.admin.Supplemental;
import org.glassfish.config.support.GlassFishConfigBean;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.virtualization.config.*;
import org.glassfish.virtualization.util.RuntimeContext;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.PerLookup;
import org.jvnet.hk2.config.*;

import javax.xml.stream.XMLStreamReader;
import java.beans.PropertyVetoException;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Add virtualization configuration to the configuration.
 * @author Jerome Dochez
 */
@Service(name="add-virtualization")
@Supplemental(on=Supplemental.Timing.Before, value="create-vm-cluster")
@Scoped(PerLookup.class)
public class AddVirtualizationCapabilities implements AdminCommand {

    @Param
    String type;

    @Param(optional = true)
    String emulator=null;

    @Inject
    ServerContext serverContext;

    @Inject
    ServerEnvironment env;

    @Inject
    Habitat habitat;

    @Inject
    Domain domain;
    
    final Logger logger = LogDomains.getLogger(AddVirtualizationCapabilities.class, LogDomains.CORE_LOGGER);
    
    @Override
    public void execute(AdminCommandContext context) {

        Virtualizations v = domain.getExtensionByType(Virtualizations.class);
        if (v==null) {
            try {
                v = (Virtualizations) ConfigSupport.apply(new SingleConfigCode<Domain>() {
                    @Override
                    public Object run(Domain wDomain) throws PropertyVetoException, TransactionFailure {
                        Virtualizations v = wDomain.createChild(Virtualizations.class);
                        wDomain.getExtensions().add(v);
                        return v;
                    }
                }, domain);
            } catch (TransactionFailure t) {
                throw new RuntimeException(t);
            }
        }
        final Virtualizations virtualizations = v;

        if (virtualizations!=null) {
            for (Virtualization virtualization : virtualizations.getVirtualizations()) {
                if (virtualization.getName().equals(type)) {
                    // already added, nothing to do anymore.
                    context.getActionReport().setActionExitCode(ActionReport.ExitCode.WARNING);
                    context.getActionReport().setActionDescription("Configuration already present in the domain.xml");
                    return;
                }
            }
        }
        final String virtType = type;

        // add initial domain.xml configuration.

        final long now = System.currentTimeMillis();
        File f = new File(serverContext.getInstallRoot(), "config");
        String defaultConfigFileName = type + ".xml";
        f = new File(f, defaultConfigFileName);
        URL temp;
        if (!f.exists()) {
            logger.info("Cannot find default virtualization at " + f.getAbsolutePath());
            temp = getClass().getClassLoader().getResource(virtType + "/ " + defaultConfigFileName);
        } else {
            try {
                temp = f.toURI().toURL();
            } catch (MalformedURLException e) {
                logger.log(Level.SEVERE, "Cannot get valid URL for virtualization.xml template", e);
                return;
            }
        }
        final URL url = temp;
        if (url!=null) {
            ConfigParser configParser = new ConfigParser(habitat);
            (new Populator() {

                public void run(ConfigParser parser) {

                    if (url != null) {
                        try {
                            // todo : change this in GlassFishConfigBean rather than Dom
                            DomDocument document = parser.parse(url,  new DomDocument(habitat) {
                                @Override
                                public Dom make(Habitat habitat, XMLStreamReader in, Dom parent, ConfigModel model) {
                                    if (parent instanceof GlassFishConfigBean) {
                                        return new GlassFishConfigBean(habitat, this, (GlassFishConfigBean) parent, model, in);
                                    } else {
                                        throw new IllegalArgumentException("parent is not a GlassFishConfigBean instance");
                                    }
                                }
                            }, Dom.unwrap(domain));
                            final Virtualizations defaultConfig = document.getRoot().createProxy(Virtualizations.class);
                            ConfigSupport.apply(new SingleConfigCode<Virtualizations>() {
                                @Override
                                public Object run(Virtualizations wVirtualizations) throws PropertyVetoException, TransactionFailure {
                                    for (Virtualization v : defaultConfig.getVirtualizations()) {
                                        wVirtualizations.getVirtualizations().add(v);
                                    }
                                    for (Template template : defaultConfig.getTemplates()) {
                                        wVirtualizations.getTemplates().add(template);
                                    }
                                    for (ServerPoolConfig serverPoolConfig : defaultConfig.getGroupConfigs()) {
                                        wVirtualizations.getGroupConfigs().add(serverPoolConfig);
                                    }
                                    return null;
                                }
                            }, virtualizations);

                        } catch(Exception e) {
                            logger.log(Level.SEVERE, "Exception while parsing virtualizations.xml", e);
                        }
                        logger.fine("time to parse virtualizations.xml : " + String.valueOf(System.currentTimeMillis() - now));
                    }
                }
            }).run(configParser);
        } else {
            try {
                ConfigSupport.apply(new SingleConfigCode<Virtualizations>() {
                    @Override
                    public Object run(Virtualizations param) throws PropertyVetoException, TransactionFailure {
                        Virtualization virt = param.createChild(Virtualization.class);
                        virt.setName(type);
                        if (emulator!=null) {
                            Emulator emulatorConfig = param.emulatorByName(emulator);
                            if (emulatorConfig==null) {
                                throw new TransactionFailure("Cannot find emulator " + emulator);
                            }
                            virt.setDefaultEmulator(emulatorConfig);
                        }
                        param.getVirtualizations().add(virt);
                        return virt;
                    }
                }, virtualizations);
            } catch(TransactionFailure e) {
                context.getActionReport().failure(RuntimeContext.logger, e.getMessage(), e);
            }
        }


        // make room for scripts to the script location.
        File destDir = new File(env.getConfigDirPath(), virtType);
        if (!destDir.exists()) {
            if (!destDir.mkdirs()) {
                logger.severe("Cannot create " + destDir);
            }
        }
    }
}
