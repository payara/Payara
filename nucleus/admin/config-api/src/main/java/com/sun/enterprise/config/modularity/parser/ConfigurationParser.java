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
package com.sun.enterprise.config.modularity.parser;

import com.sun.enterprise.config.modularity.ConfigModularityUtils;
import com.sun.enterprise.config.modularity.customization.ConfigBeanDefaultValue;
import com.sun.enterprise.config.serverbeans.ConfigLoader;
import com.sun.enterprise.util.LocalStringManager;
import com.sun.enterprise.util.LocalStringManagerImpl;
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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is a class to create the ConfigBeanProxy from the xml an xml snippet
 *
 * @author Bhakti Mehta
 * @author Masoud Kalali
 */
public class ConfigurationParser<C extends ConfigLoader> {
    private static final Logger LOG = Logger.getLogger(ConfigurationParser.class.getName());
    //TODO Until the TranslatedView issue is fixed this remain true.
    private static boolean replaceSystemProperties = true;

    /**
     * @param habitat The Habitat object to add the config to
     * @param <T>     the ConfigBeanProxy type we are looking for
     */
    public <T extends ConfigBeanProxy> void prepareAndSetConfigBean(final Habitat habitat, List<ConfigBeanDefaultValue> values) {

        ConfigParser configParser = new ConfigParser(habitat);
        // I don't use the GlassFish document here as I don't need persistence
        final DomDocument doc = new DomDocument<GlassFishConfigBean>(habitat) {
            public Dom make(final Habitat habitat, XMLStreamReader xmlStreamReader, GlassFishConfigBean dom, ConfigModel configModel) {
                // by default, people get the translated view.
                return new GlassFishConfigBean(habitat, this, dom, configModel, xmlStreamReader);
            }
        };

        //TODO requires rework to put all the changes that a service may introduce into one transaction
        //the solution is to put the loop into the apply method...  But it would be some fine amount of work
        for (final ConfigBeanDefaultValue configBeanDefaultValue : values) {
            final ConfigBeanProxy parent = ConfigModularityUtils.getOwningObject(configBeanDefaultValue.getLocation(), habitat);
            ConfigurationPopulator populator = null;
            if (replaceSystemProperties)
                try {
                    populator = new ConfigurationPopulator(
                            ConfigModularityUtils.replacePropertiesWithCurrentValue(
                                    configBeanDefaultValue.getXmlConfiguration(), configBeanDefaultValue, habitat)
                            , doc, parent);
                } catch (Exception e) {
                    LocalStringManager localStrings =
                            new LocalStringManagerImpl(ConfigurationParser.class);
                    final String msg = localStrings.getLocalString(
                            "can.not.add.configuration.to.extension.point",
                            "Cannot add new configuration extension to the extension point.");
                    LOG.log(Level.SEVERE, msg, e);
                }
            else {
                populator = new ConfigurationPopulator(configBeanDefaultValue.getXmlConfiguration(), doc, parent);
            }
            populator.run(configParser);
            try {
                Class configBeanClass = ConfigModularityUtils.getClassForFullName(configBeanDefaultValue.getConfigBeanClassName(), habitat);
                final ConfigBeanProxy pr = doc.getRoot().createProxy(configBeanClass);
                ConfigSupport.apply(new SingleConfigCode<ConfigBeanProxy>() {
                    public Object run(ConfigBeanProxy param) throws PropertyVetoException, TransactionFailure {
                        boolean writeDefaultElementsToXml = Boolean.parseBoolean(System.getProperty("writeDefaultElementsToXml"));
                        if (!writeDefaultElementsToXml) {
                            //Do not write default snippets to domain.xml
                            doc.getRoot().skipFromXml();
                        }
                        ConfigModularityUtils.setConfigBean(pr, configBeanDefaultValue, habitat, param);
                        return param;
                    }
                }, parent);
            } catch (TransactionFailure e) {
                LocalStringManager localStrings =
                        new LocalStringManagerImpl(ConfigurationParser.class);
                final String msg = localStrings.getLocalString(
                        "can.not.add.configuration.to.extension.point",
                        "Cannot add new configuration extension to the extension point.");
                LOG.log(Level.SEVERE, msg, e);
            }

        }
    }
}
