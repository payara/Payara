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

/**
 * @author Masoud Kalali
 */

import org.jvnet.hk2.config.ConfigBeanProxy;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

/**
 * Carries the default configuration values for a ConfigBeanProxy
 */
public class ConfigBeanDefaultValue {

    private String location;
    private String xmlConfiguration;
    private String configBeanClassName;
    private boolean replaceCurrentIfExists;
    private List<ConfigCustomizationToken> customizationTokens;

    public String getLocation() {
        return location;
    }


    public String getXmlConfiguration() {
        return xmlConfiguration;
    }

    public String getConfigBeanClassName() {
        return configBeanClassName;
    }

    public boolean replaceCurrentIfExists() {
        return replaceCurrentIfExists;
    }

    public List<ConfigCustomizationToken> getCustomizationTokens() {
        if (customizationTokens == null) {
            customizationTokens = Collections.emptyList();
        }

        return customizationTokens;
    }

    /**
     * @param location               the location of the config bean which this configuration is intended to create
     * @param configBeanClassName    what is the type of the config bean this configuration is intended for
     * @param xmlConfiguration       the XML snippet that represent the mentioned configuration.
     *                               The XML snippet should be a valid config bean configuration
     * @param replaceCurrentIfExists should this config bean replace an already existing one or not.
     *                               Note that, this parameter will be processed only if the configuration is intended
     *                               for a named configuration element. The other condition for the replace to happen
     *                               is that this configuration get the chance to be processed which means it should
     *                               be part of an array of config beans intended for a service that has no configuration
     *                               present in the domain.xml
     * @param customizationTokens
     * @param <U>                    Type of the config bean which is an extension of ConfigBeanProxy
     */
    public <U extends ConfigBeanProxy> ConfigBeanDefaultValue(String location, String configBeanClassName,
                                                              String xmlConfiguration, boolean replaceCurrentIfExists,
                                                              List<ConfigCustomizationToken> customizationTokens) {
        this.location = location;
        this.xmlConfiguration = xmlConfiguration;
        this.configBeanClassName = configBeanClassName;
        this.replaceCurrentIfExists = replaceCurrentIfExists;
        this.customizationTokens = customizationTokens;
    }


    /**
     * @param location                  the location of the config bean which this configuration is intended to create
     * @param configBeanClassName       what is the type of the config bean this configuration is intended for
     * @param xmlSnippetFileInputStream An InputStream for the actual configuration which might be a file or anything
     *                                  other InputStream to read the configuration from.
     * @param replaceCurrentIfExists    should this config bean replace an already existing one or not. Note that,
     *                                  this parameter will be processed only if the configuration is intended for a
     *                                  named configuration element. The other condition for the replace to happen is
     *                                  that this configuration get the chance to be processed which means it should be
     *                                  part of an array of config beans intended for a service that has no configuration
     *                                  present in the domain.xml
     * @param customizationTokens
     * @param <U>                       Type of the config bean which is an extension of ConfigBeanProxy
     * @throws Exception If the stream is not readable or closing the stream throws exception constructor
     *                   will fail with the exception.
     */
    public <U extends ConfigBeanProxy> ConfigBeanDefaultValue(String location, String configBeanClassName,
                                                              InputStream xmlSnippetFileInputStream,
                                                              boolean replaceCurrentIfExists,
                                                              List<ConfigCustomizationToken> customizationTokens) throws Exception {
        this.location = location;
        this.configBeanClassName = configBeanClassName;
        this.xmlConfiguration = streamToString(xmlSnippetFileInputStream, "utf-8");
        this.replaceCurrentIfExists = replaceCurrentIfExists;
        this.customizationTokens = customizationTokens;
    }

    public boolean addCustomizationToken(ConfigCustomizationToken e) {

        if (customizationTokens == null) {
            customizationTokens = new ArrayList<ConfigCustomizationToken>();
        }
        return customizationTokens.add(e);
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setXmlConfiguration(String xmlConfiguration) {
        this.xmlConfiguration = xmlConfiguration;
    }

    public void setConfigBeanClassName(String configBeanClassName) {
        this.configBeanClassName = configBeanClassName;
    }

    public void setReplaceCurrentIfExists(boolean replaceCurrentIfExists) {
        this.replaceCurrentIfExists = replaceCurrentIfExists;
    }

    public void setCustomizationTokens(List<ConfigCustomizationToken> customizationTokens) {
        this.customizationTokens = customizationTokens;
    }

    public ConfigBeanDefaultValue() {

    }


    /**
     * @param ins the InputStream to read and turn it into String
     * @return String equivalent of the stream
     */
    private String streamToString(InputStream ins, String encoding) throws IOException {
        String s = new Scanner(ins, encoding).useDelimiter("\\A").next();
        ins.close();
        return s;
    }

    @Override
    public String toString() {
        return "ConfigBeanDefaultValue{" +
                "location='" + location + '\'' +
                ", configBeanClassName='" + configBeanClassName + '\'' +
                ", replaceCurrentIfExists=" + replaceCurrentIfExists +
                '}';
    }
}
