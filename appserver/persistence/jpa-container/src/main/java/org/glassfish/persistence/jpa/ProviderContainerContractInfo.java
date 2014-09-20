/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.persistence.jpa;

import javax.persistence.spi.ClassTransformer;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import javax.naming.NamingException;
import javax.validation.ValidatorFactory;

import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.deployment.common.RootDeploymentDescriptor;


/**
 * @author Mitesh Meswani
 * This encapsulates information needed  to load or unload persistence units.
 */
public interface ProviderContainerContractInfo {

    static final String DEFAULT_DS_NAME = "jdbc/__default";

    /**
     *
     * @return a class loader that is used to load persistence entities
     * bundled in this application.
     */
    ClassLoader getClassLoader();

    /**
     *
     * @return a temp class loader that is used to load persistence entities
     * bundled in this application.
     */
    ClassLoader getTempClassloader();

    /**
     *
     * Adds ClassTransformer to underlying Application's classloader
     */
    void addTransformer(ClassTransformer transformer);


    /**
     * @return absolute path of the location where application is exploded.
     */
    String getApplicationLocation();

    /**
     * Looks up DataSource with JNDI name given by <code>dataSourceName</code>
     * @param dataSourceName
     * @return DataSource with JNDI name given by <code>dataSourceName</code>
     * @throws javax.naming.NamingException
     */
    DataSource lookupDataSource(String dataSourceName) throws NamingException;

    /**
     * Looks up Non transactional DataSource with JNDI name given by <code>dataSourceName</code>
     * @param dataSourceName
     * @return Non transactional DataSource with JNDI name given by <code>dataSourceName</code>
     * @throws NamingException
     */
    DataSource lookupNonTxDataSource(String dataSourceName) throws NamingException;

    /**
     * get instance of ValidatorFactory for this environment
     */
    ValidatorFactory getValidatorFactory();

    /**
     * Will be called while loading an application.
     * @return true if java2DB is required false otherwise
     */
    boolean isJava2DBRequired();

    /**
     * @return DeploymentContext associated with this instance.
     */
    DeploymentContext getDeploymentContext();


    /**
     * Register the give emf with underlying container
     * @param unitName Name of correspoding PersistenceUnit
     * @param persistenceRootUri URI within application (excluding META-INF) for root of corresponding PersistenceUnit
     * @param containingBundle The bundle that contains PU for the given EMF
     * @param emf The emf that needs to be registered
     */
    void registerEMF(String unitName, String persistenceRootUri, RootDeploymentDescriptor containingBundle, EntityManagerFactory emf);

    /**
     * @return JTA DataSource override if any
     */
    String getJTADataSourceOverride();

    /**
     *
     * @return default data source name to be used if user has not defined a data source
     */
    String getDefaultDataSourceName();

    /**
     * @return true if weaving is enabled for the current environment false otherwise
     */
    boolean isWeavingEnabled();
}
