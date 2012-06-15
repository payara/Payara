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

package com.sun.enterprise.config.serverbeans;

import com.sun.enterprise.config.util.zeroconfig.ConfigBeanDefaultValue;
import com.sun.enterprise.config.util.zeroconfig.SnippetLoader;
import com.sun.enterprise.config.util.zeroconfig.SnippetParser;
import com.sun.enterprise.config.util.zeroconfig.ZeroConfigUtils;
import org.glassfish.api.admin.config.ConfigExtension;
import org.glassfish.api.admin.config.Container;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.config.ConfigBean;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.ConfigView;
import org.jvnet.hk2.config.Dom;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Responsible for creating default config beans for any ConfigExtension derived class.
 *
 * @author Masoud Kalali
 */
public class ConfigSnippetLoader extends SnippetLoader<Config, ConfigExtension> {
    private final static Logger LOG = Logger.getLogger(ConfigSnippetLoader.class.getName());

    public ConfigSnippetLoader(Config configLoader, Class<? extends ConfigExtension> configExtensionType) {
            super(configLoader, configExtensionType);
        }
    @Override
    public <U extends ConfigExtension> U createConfigBeanForType(Class<U> configExtensionType) throws TransactionFailure {
        if(ZeroConfigUtils.hasCustomConfig(configExtensionType)){
            addConfigBeanFor(configExtensionType, configLoader);
        } else {
            final Class<U> parentElem = configExtensionType;
            ConfigSupport.apply(new SingleConfigCode<Config>() {
                @Override
                public Object run(Config parent) throws PropertyVetoException, TransactionFailure {
                    U child = parent.createChild(parentElem);
                    Dom.unwrap(child).addDefaultChildren();
                    parent.getContainers().add((Container) child);
                    return child;
                }
            }, configLoader);
        }
        Method m = ZeroConfigUtils.getMatchingGetterMethod(configLoader.getClass(), configExtensionType);
        if (m != null) {
            try {
                return (U) m.invoke( configLoader);
            }
            catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        } else {
            return configLoader.getExtensionByType(configExtensionType);
        }
    }
    public <U extends ConfigExtension> void addConfigBeanFor(Class<U> configExtensionType, Config config) {
        ConfigBean cb = (ConfigBean) ((ConfigView) Proxy.getInvocationHandler(config)).getMasterView();
        Habitat habitat = cb.getHabitat();
        List<ConfigBeanDefaultValue> configBeanDefaultValueList= ZeroConfigUtils.getDefaultConfigurations(configExtensionType);
        SnippetParser snippetParser = new SnippetParser();
        try {
            //TODO change to use parsConfigBean instead of    parseContainerConfig
            snippetParser.parsConfigBean(habitat,configBeanDefaultValueList);
        } catch (IOException e) {
            LOG.log(Level.INFO, "Unable to add  configuration for class" +
                    configExtensionType);
        }
    }
}

