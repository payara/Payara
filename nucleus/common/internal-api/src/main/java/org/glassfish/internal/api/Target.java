/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.internal.api;

import com.sun.enterprise.config.serverbeans.*;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@Service
public class Target {
    @Inject
    private Domain domain;

    @Inject
    private ServerEnvironment serverEnv;

    @Inject
    ServiceLocator habitat;

    /**
     * Lets caller to know if the caller is in DAS
     */
    public boolean isThisDAS() {
        return serverEnv.isDas();
    }

    /**
     * Lets caller to know if the caller is in an instance
     */
    public boolean isThisInstance() {
        return serverEnv.isInstance();
    }

    /**
     * Checks if a given target is cluster or nor
     * @param targetName the name of the target
     * @return true if the target represents a cluster; false otherwise
     */
    public boolean isCluster(String targetName) {
        return (domain.getClusterNamed(targetName) != null);
    }

    /**
     * Returns the Cluster element for a given cluster name
     * @param targetName the name of the target
     * @return Cluster element that represents the cluster
     */
    public Cluster getCluster(String targetName) {
        return domain.getClusterNamed(targetName);
    }

    /**
     * Returns the config element that represents a given cluster
     * @param targetName the name of the target
     * @return Config element representing the cluster
     */
    public Config getClusterConfig(String targetName) {
        Cluster cl = getCluster(targetName);
        if(cl == null)
            return null;
        return(domain.getConfigNamed(cl.getConfigRef()));
    }

    /**
     * Returns config element that represents a given server
     * @param targetName the name of the target
     * @return Config element representing the server instance
     */
    public Config getServerConfig(String targetName) {
        Server s = domain.getServerNamed(targetName);
        if(s == null)
            return null;
        return domain.getConfigNamed(s.getConfigRef());
    }

    /**
     * Given a name (of instance or cluster or config), returns the appropriate Config object
     * @param targetName name of target
     * @return Config element of this target
     */
    public Config getConfig(String targetName) {
        if(CommandTarget.CONFIG.isValid(habitat, targetName))
            return domain.getConfigNamed(targetName);
        if(CommandTarget.DAS.isValid(habitat, targetName))
            return getServerConfig(targetName);
        if(CommandTarget.STANDALONE_INSTANCE.isValid(habitat, targetName))
            return getServerConfig(targetName);
        if(CommandTarget.CLUSTER.isValid(habitat, targetName))
                return getClusterConfig(targetName);
        return null;
    }

    /**
     * Given an instance that is part of a cluster, returns the Cluster element of the cluster to which the
     * given instance belongs
     * @param targetName name of target
     * @return Cluster element to which this instance below
     */
    public Cluster getClusterForInstance(String targetName) {
        return domain.getClusterForInstance(targetName);
    }

    /**
     * Given a list instance names, get List<Server>
     */
    public List<Server> getInstances(List<String> names) {
        List<Server> instances = new ArrayList<Server>();
        for(String aName : names)
            instances.addAll(getInstances(aName));
        return instances;        
    }

    public Node getNode(String targetName) {
        return domain.getNodeNamed(targetName);
    }

    /**
     * Given the name of a target, returns a list of Server objects. If given target is a standalone server,
     * then the server's Server element is returned in the list. If the target is a cluster, then the list of Server
     * elements that represent all server instances of that cluster is returned.
     * @param targetName the name of the target
     * @return list of Server elements that represent the target
     */
    public List<Server> getInstances(String targetName) {
        List<Server> instances = new ArrayList<Server>();
        if(CommandTarget.DOMAIN.isValid(habitat, targetName))
            return instances;
        if(CommandTarget.DAS.isValid(habitat, targetName))
            return instances;
        if(CommandTarget.STANDALONE_INSTANCE.isValid(habitat, targetName)) {
            instances.add(domain.getServerNamed(targetName));
        }
        if(CommandTarget.CLUSTER.isValid(habitat, targetName)) {
            instances = getCluster(targetName).getInstances();
        }
        if(CommandTarget.CONFIG.isValid(habitat, targetName)) {
            List<String> targets = domain.getAllTargets();
            for(String aTarget : targets) {
                if(CommandTarget.CLUSTER.isValid(habitat, aTarget) &&
                        getCluster(aTarget).getConfigRef().equals(targetName)) {
                    instances.addAll(getCluster(aTarget).getInstances());
                }
                if(CommandTarget.STANDALONE_INSTANCE.isValid(habitat, aTarget) &&
                        domain.getServerNamed(aTarget).getConfigRef().equals(targetName)) {
                    instances.add(domain.getServerNamed(aTarget));
                }
            }
        }
        if(CommandTarget.NODE.isValid(habitat, targetName)) {
            List<Server> allInstances = getAllInstances();
            for(Server s : allInstances) {
                if(targetName.equals(s.getNodeRef()))
                    instances.add(s);
            }
        }
        return instances;
    }

    /**
     * Gets all instances present in the domain
     * @return list of Server elements that represent all instances
     */
    public List<Server> getAllInstances() {
        List<Server> list = new ArrayList<Server>();
        for(Server s : domain.getServers().getServer()) {
            if(!CommandTarget.DAS.isValid(habitat, s.getName())) {
                list.add(s);
            }
        }
        return list;
    }

    /**
     * Given name of a target verifies if it is valid
     * @param targetName name of the target
     * @return true if the target is a valid cluster or server instance or a config
     */
    public boolean isValid(String targetName) {
        if(isCluster(targetName))
            return true;
        if(getInstances(targetName).size() != 0)
            return true;
        if(domain.getConfigNamed(targetName) != null)
            return true;
        return false;
    }
}
