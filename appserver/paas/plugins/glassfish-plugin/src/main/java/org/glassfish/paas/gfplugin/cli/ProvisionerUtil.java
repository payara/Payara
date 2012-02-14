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

package org.glassfish.paas.gfplugin.cli;

import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.Singleton;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author Jagadish Ramu
 */
@Scoped(Singleton.class)
@Service
public class ProvisionerUtil {


    private Properties cloudConfig = null;

    @Inject
    private Habitat habitat;

    private Map<String, org.glassfish.paas.gfplugin.cli.ApplicationServerProvisioner> appserverProvisioners =
            new HashMap<String, org.glassfish.paas.gfplugin.cli.ApplicationServerProvisioner>();

    public Properties getProperties() {
        if (cloudConfig == null) {
            String installRoot = System.getProperty("com.sun.aas.installRoot");
            File propertiesFile = new File(installRoot + File.separator + "config" + File.separator + "cloud-config.properties");
            if (propertiesFile.exists()) {
                Properties properties = null;
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(propertiesFile);
                    properties = new Properties();
                    properties.load(fis);
                    cloudConfig = properties;
                } catch (IOException e) {
                    e.printStackTrace();
                }finally{
                    if(fis != null){
                        try {
                            fis.close();
                        } catch (IOException e) {}
                    }
                }
            }
        }
        //always provide the copy.
        if(cloudConfig != null){
            return (Properties) cloudConfig.clone();
        }else{
            System.err.println("Unable to find cloud-config.properties file in 'config' directory. " +
                    "Returning EMPTY properties.");
            return new Properties(); // obviate the need for a cloud-config.properties file.
        }
    }

    public ApplicationServerProvisioner getAppServerProvisioner(Properties properties) {
        String host = properties.getProperty("ip-address");
        if (appserverProvisioners.containsKey(host)) {
            return appserverProvisioners.get(host);
        } else {
            properties.put("APPLICATION_SERVER_PROVIDER", "GLASSFISH");
            properties.put("GF_HOST", host);
            properties.put("GF_PORT", "24848");
            org.glassfish.paas.gfplugin.cli.ApplicationServerProvisioner provisioner = (org.glassfish.paas.gfplugin.cli.ApplicationServerProvisioner)
                    getProvisioner(properties, org.glassfish.paas.gfplugin.cli.ApplicationServerProvisioner.class);
            provisioner.initialize(properties);
            appserverProvisioners.put(host, provisioner);
            return provisioner;
        }
    }

    public org.glassfish.paas.gfplugin.cli.ApplicationServerProvisioner getAppServerProvisioner(String host) {
        if (appserverProvisioners.containsKey(host)) {
            return appserverProvisioners.get(host);
        } else {
            Properties properties = getProperties();
            properties.put("GF_HOST", host);
            if(!properties.containsKey("APPLICATION_SERVER_PROVIDER")) { // in the absense of cloud-config.properties insert default values.
                properties.put("APPLICATION_SERVER_PROVIDER", "GLASSFISH");
                properties.put("GF_PORT", "24848");
            }
            org.glassfish.paas.gfplugin.cli.ApplicationServerProvisioner provisioner = (org.glassfish.paas.gfplugin.cli.ApplicationServerProvisioner)
                    getProvisioner(properties, org.glassfish.paas.gfplugin.cli.ApplicationServerProvisioner.class);
            provisioner.initialize(properties);
            appserverProvisioners.put(host, provisioner);
            return provisioner;
        }
    }

    public org.glassfish.paas.gfplugin.cli.Provisioner getProvisioner(Properties metaData, Class clz) {

        Collection<org.glassfish.paas.gfplugin.cli.Provisioner> provisioners = habitat.getAllByContract(clz);
        for (org.glassfish.paas.gfplugin.cli.Provisioner provisioner : provisioners) {
            try {
                if (provisioner.handles(metaData) && clz.isAssignableFrom(provisioner.getClass())) {
                    System.out.println("Found Provisioner for type [" + clz + "] : " + provisioner.getClass());
                    provisioner.initialize(metaData);
                    return provisioner;
                }
            } catch (Exception e) {
                e.printStackTrace(); //TODO logging
            }
        }
        throw new RuntimeException("Unable to get an Provisioner for metaData " + metaData);
    }

}
