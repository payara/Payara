/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.kernel.config;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.module.bootstrap.Populator;
import com.sun.logging.LogDomains;
import org.glassfish.api.admin.config.ConfigParser;
import org.glassfish.api.admin.config.Container;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.config.support.GlassFishConfigBean;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.config.ConfigModel;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.Dom;
import org.jvnet.hk2.config.DomDocument;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import javax.xml.stream.XMLStreamReader;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Jerome Dochez
 * @author Vivek Pandey 
 */
@Service
public class DefaultConfigParser implements ConfigParser {

    @Inject(name= ServerEnvironment.DEFAULT_INSTANCE_NAME)
    Config config;

    Logger logger = Logger.getLogger(LogDomains.CORE_LOGGER);

    public <T extends Container> T parseContainerConfig(Habitat habitat, final URL configuration, Class<T> configType) throws IOException {


        org.jvnet.hk2.config.ConfigParser configParser = new org.jvnet.hk2.config.ConfigParser(habitat);
        // I don't use the GlassFish document here as I don't need persistence
        final DomDocument doc = new DomDocument<GlassFishConfigBean>(habitat) {
            public Dom make(final Habitat habitat, XMLStreamReader xmlStreamReader, GlassFishConfigBean dom, ConfigModel configModel) {
                // by default, people get the translated view.
                return new GlassFishConfigBean(habitat,this, dom, configModel, xmlStreamReader);
            }
        };

        (new Populator() {

            public void run(org.jvnet.hk2.config.ConfigParser parser) {
                long now = System.currentTimeMillis();
                if (configuration != null) {
                    try {
                        DomDocument newElement = parser.parse(configuration,  doc, Dom.unwrap(config));
                        logger.info(newElement.getRoot().getProxyType().toString());
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                    Logger.getAnonymousLogger().fine("time to parse domain.xml : " + String.valueOf(System.currentTimeMillis() - now));
                }
            }
        }).run(configParser);

        // add the new container configuration to the server config
        final T container = doc.getRoot().createProxy(configType);

        try {
            ConfigSupport.apply(new SingleConfigCode<Config>() {
                public Object run(Config config) throws PropertyVetoException, TransactionFailure {
                    config.getContainers().add(container);
                    return null;
                }
            }, config);
        } catch(TransactionFailure e) {
            logger.log(Level.SEVERE, "Cannot add new configuration to the Config element", e);
        }

        return  container;
    }
}
