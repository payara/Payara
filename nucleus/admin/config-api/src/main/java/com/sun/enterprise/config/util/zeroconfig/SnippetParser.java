/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.config.util.zeroconfig;

import com.sun.enterprise.config.serverbeans.ConfigLoader;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.logging.LogDomains;
import org.glassfish.config.support.GlassFishConfigBean;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigModel;
import org.jvnet.hk2.config.ConfigParser;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.Dom;
import org.jvnet.hk2.config.DomDocument;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import javax.xml.stream.XMLStreamReader;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is a class to create the Container config beans from the snippet xml files
 *
 * @author Bhakti Mehta
 * @author Masoud Kalali
 */
public class SnippetParser<C extends ConfigLoader> {
    Logger logger = Logger.getLogger(LogDomains.CONFIG_LOGGER);

    /**
         * @param habitat    The Habitat object to add the config to
         * @param <T>        the ConfigBeanProxy type we are looking for
         * @throws IOException if it fails to read the snippetUrl.
         */
        public <T extends ConfigBeanProxy> void parsConfigBean(final Habitat habitat, List<ConfigBeanDefaultValue> values) throws IOException {

            ConfigParser configParser = new ConfigParser(habitat);
            // I don't use the GlassFish document here as I don't need persistence
            final DomDocument doc = new DomDocument<GlassFishConfigBean>(habitat) {
                public Dom make(final Habitat habitat, XMLStreamReader xmlStreamReader, GlassFishConfigBean dom, ConfigModel configModel) {
                    // by default, people get the translated view.
                    return new GlassFishConfigBean(habitat, this, dom, configModel, xmlStreamReader);
                }
            };

            for (final ConfigBeanDefaultValue configBeanDefaultValue : values) {
                Domain domain = habitat.getComponent(Domain.class);
                SnippetPopulator populator = new SnippetPopulator(configBeanDefaultValue.getXmlConfiguration(), doc, domain);
                populator.run(configParser);

                final ConfigBeanProxy configBean = doc.getRoot().createProxy(configBeanDefaultValue.getConfigBeanClass());
                try {
                    final ConfigBeanProxy parent =ZeroConfigUtils.getOwningObject(configBeanDefaultValue.getLocation(),habitat);
                    ConfigSupport.apply(new SingleConfigCode<ConfigBeanProxy>() {
                        public Object run(ConfigBeanProxy param) throws PropertyVetoException, TransactionFailure {
                                ZeroConfigUtils.setConfigBean(configBean,configBeanDefaultValue, habitat, param);
                            return param;
                        }
                    }, parent);
                } catch (TransactionFailure e) {
                    logger.log(Level.SEVERE, "Cannot add new configuration to the Config element", e);
                }

            }
        }
}
