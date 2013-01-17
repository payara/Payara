/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.container.common.impl;

import com.sun.enterprise.container.common.spi.util.ComponentEnvManager;
import com.sun.enterprise.deployment.*;
import com.sun.logging.LogDomains;
import org.glassfish.api.invocation.ComponentInvocation;
import static org.glassfish.api.invocation.ComponentInvocation.ComponentInvocationType;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Cache;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.Query;
import javax.persistence.SynchronizationType;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.metamodel.Metamodel;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Wrapper for application references to entity manager factories.
 * A new instance of this class will be created for each injected
 * EntityManagerFactory reference or each lookup of an EntityManagerFactory
 * reference within the component jndi environment.    
 *
 * @author Kenneth Saks
 */
public class EntityManagerFactoryWrapper
        implements EntityManagerFactory, Serializable {

    static Logger _logger=LogDomains.getLogger(EntityManagerFactoryWrapper.class, LogDomains.UTIL_LOGGER);

    private String unitName;

    private transient InvocationManager invMgr;

    private transient EntityManagerFactory entityManagerFactory;

    private transient ComponentEnvManager compEnvMgr;

    public EntityManagerFactoryWrapper(String unitName, InvocationManager invMgr,
                                       ComponentEnvManager compEnvMgr) {

        this.unitName = unitName;
        this.invMgr = invMgr;
        this.compEnvMgr = compEnvMgr;
    }

    private EntityManagerFactory getDelegate() {

        if( entityManagerFactory == null ) {
            entityManagerFactory = lookupEntityManagerFactory(invMgr, compEnvMgr, unitName);
            
            if( entityManagerFactory == null ) {
                throw new IllegalStateException
                    ("Unable to retrieve EntityManagerFactory for unitName "
                     + unitName);
            }
        }

        return entityManagerFactory;
    }

    @Override
    public EntityManager createEntityManager() {
        return getDelegate().createEntityManager();
    }

    @Override
    public EntityManager createEntityManager(Map map) {
        return getDelegate().createEntityManager(map);
    }

    @Override
    public EntityManager createEntityManager(SynchronizationType synchronizationType) {
        return getDelegate().createEntityManager(synchronizationType);
    }

    @Override
    public EntityManager createEntityManager(SynchronizationType synchronizationType, Map map) {
        return getDelegate().createEntityManager(synchronizationType, map);
    }

    @Override
    public void addNamedQuery(String name, Query query) {
        getDelegate().addNamedQuery(name, query);
    }

    @Override
    public <T> T unwrap(Class<T> cls) {
        return getDelegate().unwrap(cls);
    }

    @Override
    public CriteriaBuilder getCriteriaBuilder() {
        return getDelegate().getCriteriaBuilder();
    }

    @Override
    public Metamodel getMetamodel() {
        return getDelegate().getMetamodel();
    }

    @Override
    public Map<java.lang.String, java.lang.Object> getProperties() {
        return getDelegate().getProperties();
    }

    @Override
    public void close() {
        getDelegate().close();
    }

    @Override
    public boolean isOpen() {
        return getDelegate().isOpen();
    }

    @Override
    public Cache getCache() {
        return getDelegate().getCache();
    }

    @Override
    public PersistenceUnitUtil getPersistenceUnitUtil() {
        return getDelegate().getPersistenceUnitUtil();
    }

    @Override
    public <T> void addNamedEntityGraph(String graphName, EntityGraph<T> entityGraph) {
        getDelegate().addNamedEntityGraph(graphName, entityGraph);
    }


    /**
     * Lookup physical EntityManagerFactory based on current component
     * invocation.
     * @param invMgr invocationmanager 
     * @param emfUnitName unit name of entity manager factory or null if not
     *                    specified.
     * @return EntityManagerFactory or null if no matching factory could be
     *         found.
     **/
    static EntityManagerFactory lookupEntityManagerFactory(InvocationManager invMgr,
            ComponentEnvManager compEnvMgr, String emfUnitName)
    {

        ComponentInvocation inv  =  invMgr.getCurrentInvocation();

        EntityManagerFactory emf = null;

        if( inv != null ) {
            Object desc = compEnvMgr.getCurrentJndiNameEnvironment();
            if (desc != null) {
                emf = lookupEntityManagerFactory(inv.getInvocationType(),
                    emfUnitName, desc);
            }
        }
        
        return emf;
    }
    
    public static EntityManagerFactory lookupEntityManagerFactory(ComponentInvocationType invType,
            String emfUnitName, Object descriptor) {

        Application app = null;
        BundleDescriptor module = null;

        EntityManagerFactory emf = null;

        switch (invType) {

        case EJB_INVOCATION:

            if (descriptor instanceof EjbDescriptor) {
                EjbDescriptor ejbDesc = (EjbDescriptor) descriptor;
                module = (BundleDescriptor) ejbDesc.getEjbBundleDescriptor().getModuleDescriptor().getDescriptor();
                app = module.getApplication();
                break;
            }
            // EJB invocation in web bundle?
            // fall through into web case...

        case SERVLET_INVOCATION:

            module = (WebBundleDescriptor) descriptor;
            app = module.getApplication();

            break;

        case APP_CLIENT_INVOCATION:

            module = (ApplicationClientDescriptor) descriptor;
            app = module.getApplication();

            break;

        default:

            break;
        }

        // First check module-level for a match.
        if (module != null) {
            if (emfUnitName != null) {
                emf = module.getEntityManagerFactory(emfUnitName);
            } else {
                Set<EntityManagerFactory> emFactories = module
                        .getEntityManagerFactories();
                if (emFactories.size() == 1) {
                    emf = emFactories.iterator().next();
                }
            }
        }

        // If we're in an .ear and no module-level persistence unit was
        // found, check for an application-level match.
        if ((app != null) && (emf == null)) {
            if (emfUnitName != null) {

                emf = app.getEntityManagerFactory(emfUnitName, module);

            } else {
                Set<EntityManagerFactory> emFactories = app
                        .getEntityManagerFactories();
                if (emFactories.size() == 1) {
                    emf = emFactories.iterator().next();
                }
            }
        }

        return emf;
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();

        //Initialize the transients that were passed at ctor.
        ServiceLocator defaultServiceLocator = Globals.getDefaultHabitat();
        invMgr        = defaultServiceLocator.getService(InvocationManager.class);
        compEnvMgr    = defaultServiceLocator.getService(ComponentEnvManager.class);
    }

}
