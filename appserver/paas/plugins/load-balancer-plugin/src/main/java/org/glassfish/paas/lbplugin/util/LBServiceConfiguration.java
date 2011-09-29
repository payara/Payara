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

package org.glassfish.paas.lbplugin.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.glassfish.paas.lbplugin.Constants;
import org.glassfish.paas.orchestrator.provisioning.ServiceInfo;
import org.glassfish.paas.orchestrator.service.metadata.Property;

/**
 *
 * @author kshitiz
 */
public class LBServiceConfiguration {

    private String httpPort = null;
    private String httpsPort = null;
    private boolean sslEnabled = false;

    public LBServiceConfiguration() {
    }
    
    public static List<Property> getDefaultLBServiceConfigurations() {
        List<Property> properties = new ArrayList<Property>();
        properties.add(new Property(Constants.HTTP_PORT_PROP_NAME,
                Constants.DEFAULT_HTTP_PORT));
        properties.add(new Property(Constants.SSL_ENABLED_PROP_NAME,
                Constants.DEFAULT_SSL_ENABLED));
        properties.add(new Property(Constants.HTTPS_PORT_PROP_NAME,
                Constants.DEFAULT_HTTPS_PORT));
        return properties;
    }

    public static LBServiceConfiguration parseServiceConfigurations(
            Properties serviceConfigurations) {
        LBServiceConfiguration configuration =
                new LBServiceConfiguration();
        configuration.setHttpPort(serviceConfigurations.getProperty(
                Constants.HTTP_PORT_PROP_NAME));
        configuration.setHttpsPort(serviceConfigurations.getProperty(
                Constants.HTTPS_PORT_PROP_NAME));
        configuration.setSslEnabled(Boolean.valueOf(serviceConfigurations.
                getProperty(Constants.SSL_ENABLED_PROP_NAME)).booleanValue());
        return configuration;
    }

    public static LBServiceConfiguration parseServiceInfo(
            ServiceInfo serviceInfo) {
        LBServiceConfiguration configuration =
                new LBServiceConfiguration();
        Map map = serviceInfo.getProperties();
        configuration.setHttpPort((String) map.get(
                Constants.HTTP_PORT_PROP_NAME));
        String httpsPort = (String) map.get(
                Constants.HTTPS_PORT_PROP_NAME);
        if(httpsPort != null){
            configuration.setSslEnabled(true);
            configuration.setHttpsPort(httpsPort);
        }
        return configuration;
    }

    public void updateServiceInfo(ServiceInfo entry) {
        entry.setProperty(Constants.HTTP_PORT_PROP_NAME, httpPort);
        if(sslEnabled){
            entry.setProperty(Constants.HTTPS_PORT_PROP_NAME, httpsPort);
        }
    }

    private void setHttpPort(String httpPort) {
        this.httpPort = httpPort;
    }

    private void setHttpsPort(String httpsPort) {
        this.httpsPort = httpsPort;
    }

    private void setSslEnabled(boolean sslEnabled) {
        this.sslEnabled = sslEnabled;
    }

    public String getHttpPort() {
        return httpPort;
    }

    public String getHttpsPort() {
        return httpsPort;
    }

    public boolean isSslEnabled() {
        return sslEnabled;
    }

}
