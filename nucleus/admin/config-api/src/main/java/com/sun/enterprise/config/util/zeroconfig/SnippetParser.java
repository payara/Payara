/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.ConfigLoader;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.module.bootstrap.BootException;
import com.sun.enterprise.module.bootstrap.Populator;
import com.sun.logging.LogDomains;
import org.glassfish.api.admin.config.Container;
import org.glassfish.config.support.GlassFishConfigBean;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.config.*;

import javax.xml.stream.XMLStreamReader;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
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

    private final Config config;

    public SnippetParser(Config config) {
        this.config = config;
    }

    /**
     * @param habitat    The Habitat object to add the config to
     * @param snippetUrl The URL object pointing to the configuration snippet
     * @param configType The ConfigBeanProxy type matching the configuration present in the snippetUrl
     * @param <T>        the ConfigBeanProxy type we are looking for
     * @return A fully loaded ConfigExtension built using the configuration present in the snippetUrl
     * @throws IOException if it fails to read the snippetUrl.
     */
    public <T extends Container> T parseContainerConfig(Habitat habitat, final URL snippetUrl, final Class<T> configType) throws IOException {

        org.jvnet.hk2.config.ConfigParser configParser = new org.jvnet.hk2.config.ConfigParser(habitat);
        // I don't use the GlassFish document here as I don't need persistence
        final DomDocument doc = new DomDocument<GlassFishConfigBean>(habitat) {
            public Dom make(final Habitat habitat, XMLStreamReader xmlStreamReader, GlassFishConfigBean dom, ConfigModel configModel) {
                // by default, people get the translated view.
                return new GlassFishConfigBean(habitat, this, dom, configModel, xmlStreamReader);
            }
        };
        SnippetPopulator populator = new SnippetPopulator(snippetUrl, doc, config);
        populator.run(configParser);
        // add the new container configuration to the server config
        final T configBean = doc.getRoot().createProxy(configType);
        try {
            ConfigSupport.apply(new SingleConfigCode<Config>() {
                public Object run(Config config) throws PropertyVetoException, TransactionFailure {
                    config.getContainers().add(configBean);
                    return config;
                }
            }, config);
        } catch (TransactionFailure e) {
            logger.log(Level.SEVERE, "Cannot add new configuration to the Config element", e);
        }

        return configBean;
    }


    /**
     * Finds and return the setter method matching the class named fqcn in the configLoader
     *
     * @param configLoader The ConfigLoader we want to inspect for presence of a setter method accepting class of type fqcn.
     * @param fqcn         the fully qualified class name to find its setter in the configLoader
     * @return the matching Method object or null if not present.
     */

    protected Method getMatchingSetterMethod(Config configLoader, String fqcn) {
        String className = fqcn.substring(fqcn.lastIndexOf(".") + 1, fqcn.length());
        String setterName = "set" + className;
        Method[] methods = configLoader.getClass().getMethods();
        for (int i = 0; i < methods.length; i++) {
            if (methods[i].getName().equalsIgnoreCase(setterName)) {
                return methods[i];
            }
        }
        return null;
    }
}
