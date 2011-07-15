/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.javaee.services;

import org.glassfish.api.naming.NamingObjectProxy;
import org.glassfish.internal.api.Globals;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.component.PerLookup;
import org.jvnet.hk2.component.Habitat;

import javax.naming.Context;
import javax.naming.NamingException;

import com.sun.enterprise.deployment.DataSourceDefinitionDescriptor;
import com.sun.appserv.connectors.internal.spi.ResourceDeployer;
import com.sun.appserv.connectors.internal.api.ConnectorsUtil;

import java.io.Serializable;
import java.util.Collection;

/**
 * This resource proxy will not bind the actual object upon first lookup unlike
 * org.glassfish.javaee.services.ResourceProxy. <br><br>
 * It holds the information required to get the actual object upon requests.<br>
 *
 * This is used for @DataSourceDefinition<br>
 * Upon first lookup, the datasource is created, deployed and the internal jndi-name is given to the
 * proxy, using the internal jndi-name actual resource is returned.<br>
 * On further lookups, internal jndi-name is used to get the actual resource.<br>
 *
 */
@Service
@Scoped(PerLookup.class)
public class DataSourceDefinitionProxy implements NamingObjectProxy.InitializationNamingObjectProxy, Serializable {
    @Inject
    private transient Habitat habitat;
    private DataSourceDefinitionDescriptor desc;
    private String actualResourceName;

    public Object create(Context ic) throws NamingException {
        if(actualResourceName == null){
            
            actualResourceName = ConnectorsUtil.deriveDataSourceDefinitionResourceName
                    (desc.getResourceId(), desc.getName());

            try{
                if(habitat == null){
                    habitat = Globals.getDefaultHabitat();
                    if(habitat == null){
                        throw new NamingException("Unable to create resource " +
                                "["+ desc.getName() +" ] as habitat is null");
                    }
                }
                getResourceDeployer(desc, habitat.getAllByContract(ResourceDeployer.class)).deployResource(desc);
            }catch(Exception e){
                NamingException ne = new NamingException("Unable to create resource ["+ desc.getName() +" ]");
                ne.initCause(e);
                throw ne;
            }
        }
        return ic.lookup(actualResourceName);
    }

    public void setDescriptor(DataSourceDefinitionDescriptor desc){
        this.desc = desc;
    }

    private ResourceDeployer getResourceDeployer(Object resource, Collection<ResourceDeployer> deployers) {
        ResourceDeployer resourceDeployer = null;
        for(ResourceDeployer deployer : deployers){
            if(deployer.handles(resource)){
                resourceDeployer = deployer;
                break;
            }
        }
        return resourceDeployer;
    }
}
