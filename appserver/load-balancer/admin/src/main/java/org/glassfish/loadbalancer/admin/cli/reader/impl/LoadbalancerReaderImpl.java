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

package org.glassfish.loadbalancer.admin.cli.reader.impl;

import com.sun.enterprise.config.serverbeans.Cluster;
import org.glassfish.loadbalancer.admin.cli.transform.Visitor;
import org.glassfish.loadbalancer.admin.cli.transform.LoadbalancerVisitor;

import org.glassfish.loadbalancer.admin.cli.reader.api.ClusterReader;
import org.glassfish.loadbalancer.admin.cli.reader.api.PropertyReader;
import org.glassfish.loadbalancer.admin.cli.reader.api.LoadbalancerReader;
import org.glassfish.loadbalancer.admin.cli.reader.api.LbReaderException;

import org.glassfish.loadbalancer.config.LbConfig;
import com.sun.enterprise.config.serverbeans.ClusterRef;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Ref;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.ServerRef;

import java.util.List;

import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.loadbalancer.admin.cli.LbLogUtil;

/**
 * Reader class to get information about load balancer configuration.
 *
 * @author Kshitiz Saxena
 */
public class LoadbalancerReaderImpl implements LoadbalancerReader {

    //--- CTORS-------
    public LoadbalancerReaderImpl(Domain domain, ApplicationRegistry appRegistry, Set<String> clusters, Properties properties) {
        _domain = domain;
        _appRegistry = appRegistry;
        _clusters = clusters;
        _properties = properties;
    }

    public LoadbalancerReaderImpl(Domain domain, ApplicationRegistry appRegistry, LbConfig lbConfig) {
        _domain = domain;
        _appRegistry = appRegistry;
        _lbConfig = lbConfig;
    }

    //--- READER IMPLEMENTATION -----
    /**
     * Returns properties of the load balancer.
     * For example response-timeout-in-seconds, reload-poll-interval-in-seconds
     * and https-routing etc.
     *
     * @return PropertyReader[]     array of properties
     */
    @Override
    public PropertyReader[] getProperties() throws LbReaderException {
        if(_lbConfig != null){
            return PropertyReaderImpl.getPropertyReaders(_lbConfig);
        } else {
            return PropertyReaderImpl.getPropertyReaders(_properties);
        }
    }

    /**
     * Returns the cluster info that are load balanced by this LB.
     *
     * @return ClusterReader        array of cluster readers
     */
    @Override
    public ClusterReader[] getClusters() throws LbReaderException {
        if (_lbConfig != null) {
            return getClustersDataFromLBConfig();
        } else if (_clusters != null) {
            return getClustersData();
        } else {
            String msg = LbLogUtil.getStringManager().getString("NoConfigOrCluster");
            throw new LbReaderException(msg);
        }
    }

    public ClusterReader[] getClustersData() throws LbReaderException {
        ClusterReader[] cls = new ClusterReader[_clusters.size()];
        Iterator<String> iter = _clusters.iterator();
        int i = 0;
        boolean isFirstServer = false;
        while (iter.hasNext()) {
            String name = iter.next();
            boolean isServer = _domain.isServer(name);
            if (i == 0) {
                isFirstServer = isServer;
            } else {
                //Mix of standalone instances and clusters is not allowed
                if (isFirstServer^isServer) {
                    String msg = LbLogUtil.getStringManager().getString("MixofServerAndClusterNotSupported");
                    throw new LbReaderException(msg);
                }
            }
            if (isServer) {
                Server server = _domain.getServerNamed(name);
                //An instance within cluster is not allowed
                if(server.getCluster() != null){
                    String msg = LbLogUtil.getStringManager().getString("ServerPartofClusterNotSupported", name);
                    throw new LbReaderException(msg);
                }
                cls[i++] = new StandAloneClusterReaderImpl(_domain, _appRegistry, server);
            } else {
                Cluster cluster = _domain.getClusterNamed(name);
                if(cluster == null){
                    String msg = LbLogUtil.getStringManager().getString("ClusterorInstanceNotFound", name);
                    throw new LbReaderException(msg);
                }
                cls[i++] = new ClusterReaderImpl(_domain, _appRegistry, cluster);
            }
        }
        return cls;
    }

    public ClusterReader[] getClustersDataFromLBConfig() throws LbReaderException {
        List<Ref> serverOrClusters = _lbConfig.getClusterRefOrServerRef();
        ClusterReader[] cls = new ClusterReader[serverOrClusters.size()];
        Iterator<Ref> iter = serverOrClusters.iterator();
        int i = 0;
        while (iter.hasNext()) {
            Ref ref = iter.next();
            if (ref instanceof ServerRef) {
                cls[i++] = new StandAloneClusterReaderImpl(_domain,
                        _appRegistry, (ServerRef) ref);

            } else if (ref instanceof ClusterRef) {
                cls[i++] = new ClusterReaderImpl(_domain, _appRegistry,
                        (ClusterRef) ref);
            } else {
                String msg = LbLogUtil.getStringManager().getString("UnableToDetermineType", ref.getRef());
                throw new LbReaderException(msg);
            }
        }
        return cls;
    }

    /**
     * Returns the name of the load balancer
     *
     * @return String               name of the LB
     */
    @Override
    public String getName() throws LbReaderException {
        if (_lbConfig != null) {
            return _lbConfig.getName();
        }
        return null;
    }

    // --- VISITOR IMPLEMENTATION ---
    @Override
    public void accept(Visitor v) throws Exception {
		if (v instanceof LoadbalancerVisitor) {
			LoadbalancerVisitor cv = (LoadbalancerVisitor) v;
			cv.visit(this);
		}
    }

    @Override
    public LbConfig getLbConfig() {
        return _lbConfig;
    }
    
    // --- PRIVATE VARS -----
    private LbConfig _lbConfig = null;
    private Domain _domain = null;
    private ApplicationRegistry _appRegistry = null;
    private Set<String> _clusters = null;
    private Properties _properties = null;
}
