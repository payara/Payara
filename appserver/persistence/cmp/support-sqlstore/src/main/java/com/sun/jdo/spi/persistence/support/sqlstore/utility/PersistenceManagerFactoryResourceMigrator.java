/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jdo.spi.persistence.support.sqlstore.utility;

import com.sun.appserv.connectors.internal.api.ConnectorsUtil;
import com.sun.enterprise.config.serverbeans.Resources;
import org.glassfish.connectors.config.PersistenceManagerFactoryResource;
import org.glassfish.jdbc.config.JdbcResource;
import org.jvnet.hk2.annotations.Service;
import javax.inject.Inject;
import org.glassfish.hk2.api.PostConstruct;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;
import org.glassfish.api.admin.config.ConfigurationUpgrade;

import java.util.Collection;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.beans.PropertyVetoException;

/**
 * @author Mitesh Meswani
 */
@Service
public class PersistenceManagerFactoryResourceMigrator implements ConfigurationUpgrade, PostConstruct {
    @Inject
    Resources resources;

    public void postConstruct() {
        Collection<PersistenceManagerFactoryResource> pmfResources = resources.getResources(PersistenceManagerFactoryResource.class);
        for (final PersistenceManagerFactoryResource pmfResource : pmfResources) {
            String jdbcResourceName = pmfResource.getJdbcResourceJndiName();

            final JdbcResource jdbcResource = (JdbcResource) ConnectorsUtil.getResourceByName(resources, JdbcResource.class, jdbcResourceName);

            try {
                ConfigSupport.apply(new SingleConfigCode<Resources>() {

                    public Object run(Resources resources) throws PropertyVetoException, TransactionFailure {
                        // delete the persitence-manager-factory resource
                        resources.getResources().remove(pmfResource);

                        // create a jdbc resource which points to same connection pool and has same jndi name as pmf resource.
                        JdbcResource newResource = resources.createChild(JdbcResource.class);
                        newResource.setJndiName(pmfResource.getJndiName());
                        newResource.setDescription("Created to migrate persistence-manager-factory-resource from V2 domain");
                        newResource.setPoolName(jdbcResource.getPoolName());
                        newResource.setEnabled("true");
                        resources.getResources().add(newResource);
                        return newResource;
                    }
                }, resources);
            } catch (TransactionFailure tf) {
                Logger.getAnonymousLogger().log(Level.SEVERE,
                    "Failure while upgrading persistence-manager-factory-resource", tf);
                throw new RuntimeException(tf);

            }

        } // end of iteration
    }
}
