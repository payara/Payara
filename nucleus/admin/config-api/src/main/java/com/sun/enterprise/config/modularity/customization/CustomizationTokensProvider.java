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

package com.sun.enterprise.config.modularity.customization;

import com.sun.enterprise.config.modularity.annotation.CustomConfiguration;
import com.sun.enterprise.config.modularity.annotation.HasCustomizationTokens;
import com.sun.enterprise.config.modularity.parser.ModuleXMLConfigurationFileParser;
import com.sun.enterprise.util.LocalStringManager;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.jvnet.hk2.config.ConfigBeanProxy;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Masoud Kalali
 */

public class CustomizationTokensProvider {
    private static final Logger LOG = Logger.getLogger(CustomizationTokensProvider.class.getName());

    //TODO require further testing
    public List<ConfigCustomizationToken> getPresentConfigCustomizationTokens(String runtimeType) throws NoSuchFieldException, IllegalAccessException {
        List<ConfigCustomizationToken> ctk = new ArrayList<ConfigCustomizationToken>();
        ClassLoader cl = this.getClass().getClassLoader();
        Field classes = cl.getClass().getDeclaredField("classes");
        classes.setAccessible(true);
        Vector<Class> clzs = (Vector<Class>) classes.get(cl);
        for (Iterator<Class> iter = clzs.iterator(); iter.hasNext(); ) {
            Class clz = iter.next();
            if (clz != null) {
                if (clz.isAnnotationPresent(HasCustomizationTokens.class)) {
                    ctk.addAll(getTokens(clz, runtimeType));
                }
            }
        }
        return ctk;
    }

    private List<ConfigCustomizationToken> getTokens(Class configBean, String runtimeType) {
        List<ConfigCustomizationToken> ctk = new ArrayList<ConfigCustomizationToken>();
        List<ConfigBeanDefaultValue> defaultValues = getDefaultConfigurations(configBean, runtimeType);
        for (ConfigBeanDefaultValue def : defaultValues) {
            ctk.addAll(def.getCustomizationTokens());
        }
        return ctk;
    }

    public List<ConfigBeanDefaultValue> getDefaultConfigurations(Class configBeanClass, String runtimeType) {

        CustomConfiguration c = (CustomConfiguration) configBeanClass.getAnnotation(CustomConfiguration.class);
        List<ConfigBeanDefaultValue> defaults = Collections.emptyList();
        if (c.usesOnTheFlyConfigGeneration()) {
            Method m = getGetDefaultValuesMethod(configBeanClass);
            if (m != null) {
                try {
                    defaults = (List<ConfigBeanDefaultValue>) m.invoke(null, runtimeType);
                } catch (Exception e) {
                    LOG.log(Level.INFO, "cannot get default configuration for: " + configBeanClass.getName(), e);
                }
            }
        } else {
            LocalStringManager localStrings =
                    new LocalStringManagerImpl(configBeanClass);
            ModuleXMLConfigurationFileParser parser = new ModuleXMLConfigurationFileParser(localStrings);
            try {
                defaults = parser.parseServiceConfiguration(getConfigurationFileUrl(configBeanClass, c.baseConfigurationFileName(), runtimeType).openStream());
            } catch (XMLStreamException e) {
                LOG.log(Level.SEVERE, "Cannot parse default module configuration", e);
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Cannot parse default module configuration", e);
            }
        }
        return defaults;
    }

    public Class getDuckClass(Class configBeanType) {
        Class duck;
        final Class[] clz = configBeanType.getDeclaredClasses();
        for (Class aClz : clz) {
            duck = aClz;
            if (duck.getSimpleName().equals("Duck")) {
                return duck;
            }
        }
        return null;
    }

    public Method getGetDefaultValuesMethod(Class configBeanType) {
        Class duck = getDuckClass(configBeanType);
        if (duck == null) {
            return null;
        }
        Method m;
        try {
            m = duck.getMethod("getDefaultValues", String.class);
        } catch (Exception ex) {
            return null;
        }
        return m;
    }

    public <U extends ConfigBeanProxy> URL getConfigurationFileUrl(Class<U> configBeanClass, String baseFileName, String runtimeType) {
        String fileName = runtimeType + "-" + baseFileName;
        URL fileUrl = configBeanClass.getClassLoader().getResource("META-INF/configuration/" + fileName);
        if (fileUrl == null) {
            fileUrl = configBeanClass.getClassLoader().getResource("META-INF/configuration/" + baseFileName);
        }
        return fileUrl;
    }
}
