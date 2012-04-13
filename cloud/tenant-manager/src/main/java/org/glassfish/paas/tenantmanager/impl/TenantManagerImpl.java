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

import java.beans.PropertyVetoException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;

import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.paas.admin.CloudServices;
import org.glassfish.paas.tenantmanager.config.TenantManagerConfig;
import org.glassfish.paas.tenantmanager.entity.Tenant;
import org.glassfish.paas.tenantmanager.entity.TenantAdmin;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.ComponentException;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.config.ConfigBean;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigCode;
import org.jvnet.hk2.config.ConfigParser;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.Dom;
import org.jvnet.hk2.config.DomDocument;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.module.ModulesRegistry;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.hk2.component.ExistingSingletonInhabitant;
import com.sun.hk2.component.InhabitantsParser;

/**
 * Default implementation for {@link TenantManagerEx}.
 * 
 * @author Andriy Zhdanov
 * 
 */
@Service
public class TenantManagerImpl implements TenantManagerEx {

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T get(Class<T> config) {
        String name = getCurrentTenant();
        return get(config, name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends ConfigBeanProxy> Object executeUpdate(
            final SingleConfigCode<T> code, T param) throws TransactionFailure {
        ConfigBeanProxy[] objects = { param };
        return excuteUpdate((new ConfigCode() {
            @SuppressWarnings("unchecked")
            public Object run(ConfigBeanProxy... objects) throws PropertyVetoException, TransactionFailure {
                return code.run((T) objects[0]);
            }
        }), objects);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object excuteUpdate(ConfigCode configCode, ConfigBeanProxy ... objects) throws TransactionFailure {
        // assume there is an object and all objects are for one document
        ConfigBeanProxy source = objects[0];
        TenantConfigBean configBean = (TenantConfigBean) Dom.unwrap(source);
        Lock lock = configBean.getDocument().getLock();
        try {
            if (lock.tryLock(ConfigSupport.lockTimeOutInSeconds, TimeUnit.SECONDS)) {
                try {
                    return configSupport._apply(configCode, objects);
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            // ignore, will throw TransactionFailure
        }
        throw new TransactionFailure("Can't acquire global lock");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCurrentTenant() {
        String name = currentTenant.get();
        if (name == null) {
            // TODO: logging, error handling, i18n
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

    private File getTenantFile(String name) {
        String dir = getTenantManagerConfig().getFileStore() + "/" + name;
        String filePath =  dir + "/tenant.xml";
        return new File(filePath);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Tenant create(final String name, final String adminUserName) {
        File tenantFile = getTenantFile(name);
        File tenantDir = tenantFile.getParentFile();
        if (tenantFile.exists()) {
            // TODO: i18n
            throw new IllegalArgumentException("Tenant " + name + " already exists");
        }
        try {
            boolean created = tenantDir.mkdirs();
            // TODO: i18n
            logger.fine("Tenant dir " + tenantDir.getPath() + " was " + (created ? "" : "not ") + "created");
            // TODO: better assert created?
            created = tenantFile.createNewFile();
            logger.fine("Tenant file " + tenantFile.getPath() + " was " + (created ? "" : "not ") + "created");
            // TODO: better assert created?
            Writer writer = new FileWriter(tenantFile);
            writer.write("<tenant name='" + name + "'/>");
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Tenant tenant = get(Tenant.class, name);
        try {
            ConfigSupport.apply(new SingleConfigCode<Tenant>() {
                @Override
                public Object run(Tenant tenant) throws TransactionFailure {
                    TenantAdmin tenantAdmin = tenant.createChild(TenantAdmin.class);
                    tenantAdmin.setName(adminUserName);
                    tenant.setTenantAdmin(tenantAdmin);
                    return tenant;
                }
                
            }, tenant);
        } catch (TransactionFailure e) {
            // TODO Auto-generated catch block
            e.printStackTrace();            
        }
        
        return tenant;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(String name) {
        habitats.remove(name);
        // TODO: do we really want to delete file or just dispose habitat?
        FileUtils.deleteFileMaybe(getTenantFile(name));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TenantManagerConfig getTenantManagerConfig() {
        CloudServices cs = config.getExtensionByType(CloudServices.class);
        TenantManagerConfig tmc = cs.getCloudServiceByType(TenantManagerConfig.class);
        return tmc;
    }

    /**
     * Simply threadLocal variable to track current tenant.
     */
    private ThreadLocal<String> currentTenant = new ThreadLocal<String>() {
        @Override
        protected String initialValue() {
            return null;
        }
    };

    /**
     * Get current tenant specific information. It is possible to get relevant
     * top level configuration, like Tenant, Environments and Services.
     * 
     * @param config
     *            Config class.
     * @param tenantName
     *            Tenant name.
     * @return Config.
     */
    private <T> T get(Class<T> config, String tenantName) {
        Habitat habitat = getHabitat(tenantName);
        // TODO: assert Tenant/Environment/Service is requested
        return  habitat.getComponent(config);
        
    }

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
        // "rain dance" based on com.sun.enterprise.module.bootstrap.Main.createHabitat
        // own SingleModulesRegistry is not enough for multi-modules project, so
        // "clone" parent habitat, but set modulesRegistry for DomainXml beforehand. 
        Habitat habitat = modulesRegistry.newHabitat();
        InhabitantsParser parser = new InhabitantsParser(habitat); 
        habitat.add(new ExistingSingletonInhabitant<ModulesRegistry>(ModulesRegistry.class, modulesRegistry));
        final ClassLoader oldCL = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            @Override
            public ClassLoader run() {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
                return cl;
            }
        });
        try {
            // initialize habitat!
            habitat = modulesRegistry.createHabitat("default", parser);
        } finally {
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    Thread.currentThread().setContextClassLoader(oldCL);
                    return null;
                }
            });
        }
        DomDocument<Dom> doc = populate(habitat, name);
        // does not work! habitat.getComponent(Transactions.class).addTransactionsListener(transactionsListener);
        // subscribe changeListener to particular document, 
        // this may be even better, than subscribing "any" transactionsListener
        ((ConfigBean)doc.getRoot()).addListener(configListener);
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
        try {
            return parser.parse(fileUrl, new TenantDocument(habitat, fileUrl));
        } catch (ComponentException e) {
            // TODO: i18n, better error
            throw new IllegalArgumentException("Tenant '" + name + "' might be deleted", e);
        }
    }

    /**
     * Habitats per tenant.
     */
    private Map<String, Habitat> habitats = new HashMap<String, Habitat>();

    /**
     * Server config.
     */
    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    private Config config;

    @Inject
    ServerEnvironment env;

    @Inject
    private TenantConfigListener configListener;

    @Inject
    private Logger logger;

    @Inject
    private ModulesRegistry modulesRegistry;

    @Inject
    private ConfigSupport configSupport;
}
