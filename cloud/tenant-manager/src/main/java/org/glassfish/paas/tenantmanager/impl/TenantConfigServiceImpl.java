/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.paas.tenantmanager.impl;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.paas.admin.CloudServices;
import org.glassfish.paas.tenantmanager.api.TenantConfigService;
import org.glassfish.paas.tenantmanager.config.TenantManagerConfig;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.config.ConfigBean;
import org.jvnet.hk2.config.ConfigParser;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.Dom;
import org.jvnet.hk2.config.DomDocument;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.module.ModulesRegistry;
import com.sun.enterprise.module.single.SingleModulesRegistry;


/**
 * Default implementation for {@link TenantConfigService}.
 * 
 * @author Andriy Zhdanov
 * 
 */
@Service
public class TenantConfigServiceImpl implements TenantConfigService {
    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T get(Class<T> config) {
        String name = getCurrentTenant();
        Habitat habitat = getHabitat(name);
        // TODO: assert Tenant/Environment/Service is requested
        return  habitat.getComponent(config);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCurrentTenant() {
        String name = currentTenant.get();
        if (name == null) {
            // TODO: loggin, error handlng, i18n
            throw new IllegalArgumentException("No current tenant set");
        }
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCurrentTenant(String name) {
        currentTenant.set(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TenantManagerConfig getTenantManagerConfig() {
        CloudServices cs = config.getExtensionByType(CloudServices.class);
        if (cs == null ) {
           cs = config.createDefaultChildByType(CloudServices.class);
        }

        TenantManagerConfig tmc = cs.getCloudServiceByType(TenantManagerConfig.class);
        if (tmc == null ) {
            tmc = cs.createDefaultChildByType(TenantManagerConfig.class);

            // set default values
            try {
                ConfigSupport.apply(new SingleConfigCode<TenantManagerConfig>() {
                    @Override
                    public Object run(TenantManagerConfig tmc) throws TransactionFailure {
                        File configDir = env.getConfigDirPath();
                        if (configDir != null) {
                            String fileStore = configDir.getAbsolutePath() + "/tenants-store";
                            System.out.println("fileStore: " + fileStore);
                            tmc.setFileStore(fileStore);
                        } else {
                            // TODO: alert no config root?
                        }
                        return tmc;
                    }
                }, tmc);
            } catch (TransactionFailure e) {
                // TODO Auto-generated catch block
                e.printStackTrace();            
            }
        }

        return tmc;
    }

    private ThreadLocal<String> currentTenant = new ThreadLocal<String>() {
        @Override
        protected String initialValue() {
            return null;
        }
    };

    private Habitat getHabitat(String name) {
        if (!habitats.containsKey(name))  {
            synchronized(habitats) {
                if (!habitats.containsKey(name))  {
                    habitats.put(name, getNewHabitat(name)); 
                }                
            }
        }
        return habitats.get(name);
    }
    
    private Habitat getNewHabitat(String name) {
        ModulesRegistry registry = new SingleModulesRegistry(TenantConfigServiceImpl.class.getClassLoader());
        Habitat habitat = registry.createHabitat("default");
        // does not work! habitat.getComponent(Transactions.class).addTransactionsListener(transactionListener);
        DomDocument<Dom> doc = populate(habitat, name);
        ((ConfigBean)doc.getRoot()).addListener(transactionListener);
        return habitat;
    }

    @SuppressWarnings("unchecked")
    private DomDocument<Dom> populate(Habitat habitat, String name) {
        String filePath = getTenantManagerConfig().getFileStore() + "/" + name + "/tenant.xml";
        ConfigParser parser = new ConfigParser(habitat);
        URL fileUrl = null;
        try {
            fileUrl = new URL("file://" + filePath);
        } catch (MalformedURLException e) {
            // should not happen
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return parser.parse(fileUrl, new TenantDocument(habitat, fileUrl));
    }
    
    private Map<String, Habitat> habitats = new HashMap<String, Habitat>();

    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    private Config config;

    @Inject
    ServerEnvironment env;
    
    /* does not work!
    @Inject
    private ModulesRegistry registry;
    */

    @Inject
    private TenantTransactionListener transactionListener;
    
}
