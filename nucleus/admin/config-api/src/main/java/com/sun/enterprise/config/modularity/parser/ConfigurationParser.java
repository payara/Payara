/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
import com.sun.enterprise.config.util.ConfigApiLoggerInfo;
import com.sun.enterprise.util.LocalStringManager;
import com.sun.enterprise.util.LocalStringManagerImpl;

import org.glassfish.config.support.GlassFishConfigBean;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigModel;
import org.jvnet.hk2.config.ConfigParser;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.Dom;
import org.jvnet.hk2.config.DomDocument;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import javax.inject.Inject;
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
@Service
public class ConfigurationParser<C extends ConfigLoader> {
    
    private static final Logger LOG = ConfigApiLoggerInfo.getLogger();
    
    //TODO Until the TranslatedView issue is fixed this remain true.
    private static boolean replaceSystemProperties = false;

    @Inject
    private ServiceLocator serviceLocator;

    @Inject
    private ConfigModularityUtils configModularityUtils;

    /**
     * @param <T> the ConfigBeanProxy type we are looking for
     */
    public <T extends ConfigBeanProxy> void parseAndSetConfigBean(List<ConfigBeanDefaultValue> values) {

        ConfigParser configParser = new ConfigParser(serviceLocator);
        // I don't use the GlassFish document here as I don't need persistence
        final DomDocument doc = new DomDocument<GlassFishConfigBean>(serviceLocator) {
            @Override
            public Dom make(final ServiceLocator serviceLocator, XMLStreamReader xmlStreamReader, GlassFishConfigBean dom, ConfigModel configModel) {
                return new GlassFishConfigBean(serviceLocator, this, dom, configModel, xmlStreamReader);
            }
        };

        //TODO requires rework to put all the changes that a service may introduce into one transaction
        //the solution is to put the loop into the apply method...  But it would be some fine amount of work
        for (final ConfigBeanDefaultValue configBeanDefaultValue : values) {
            final ConfigBeanProxy parent = configModularityUtils.getOwningObject(configBeanDefaultValue.getLocation());
            if (parent == null) continue;
            ConfigurationPopulator populator = null;
            if (replaceSystemProperties)
                try {
                    populator = new ConfigurationPopulator(
                            configModularityUtils.replacePropertiesWithCurrentValue(
                                    configBeanDefaultValue.getXmlConfiguration(), configBeanDefaultValue)
                            , doc, parent);
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, ConfigApiLoggerInfo.CFG_EXT_ADD_FAILED, e);
                }
            else {
                //Check that parent is not null!
                populator = new ConfigurationPopulator(configBeanDefaultValue.getXmlConfiguration(), doc, parent);
            }
            populator.run(configParser);
            synchronized (configModularityUtils) {
                boolean oldValue = configModularityUtils.isIgnorePersisting();
                try {
                    Class configBeanClass = configModularityUtils.getClassForFullName(configBeanDefaultValue.getConfigBeanClassName());
                    final ConfigBeanProxy pr = doc.getRoot().createProxy(configBeanClass);
                    configModularityUtils.setIgnorePersisting(true);
                    ConfigSupport.apply(new SingleConfigCode<ConfigBeanProxy>() {
                        public Object run(ConfigBeanProxy param) throws PropertyVetoException, TransactionFailure {
                            configModularityUtils.setConfigBean(pr, configBeanDefaultValue, param);
                            return param;
                        }
                    }, parent);
                } catch (TransactionFailure e) {
                    LOG.log(Level.SEVERE, ConfigApiLoggerInfo.CFG_EXT_ADD_FAILED, e);
                } finally {
                    configModularityUtils.setIgnorePersisting(oldValue);
                }
            }
        }
    }
}
