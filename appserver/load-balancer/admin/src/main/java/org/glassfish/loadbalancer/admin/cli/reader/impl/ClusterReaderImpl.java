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

import com.sun.enterprise.config.serverbeans.ApplicationRef;
import com.sun.enterprise.config.serverbeans.HealthChecker;

import org.glassfish.loadbalancer.admin.cli.transform.Visitor;
import org.glassfish.loadbalancer.admin.cli.transform.ClusterVisitor;

import org.glassfish.loadbalancer.admin.cli.reader.api.ClusterReader;
import org.glassfish.loadbalancer.admin.cli.reader.api.InstanceReader;
import org.glassfish.loadbalancer.admin.cli.reader.api.WebModuleReader;
import org.glassfish.loadbalancer.admin.cli.reader.api.HealthCheckerReader;

import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.ClusterRef;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.ServerRef;
import java.util.Iterator;
import java.util.List;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.loadbalancer.admin.cli.reader.api.LbReaderException;

/**
 * Impl class for ClusterReader. This provides loadbalancer 
 * data for a cluster.
 *
 * @author Kshitiz Saxena
 */
public class ClusterReaderImpl implements ClusterReader {

    public ClusterReaderImpl(Domain domain, ApplicationRegistry appRegistry, Cluster cluster) {
        _domain = domain;
        _appRegistry = appRegistry;
        _cluster = cluster;
    }

    public ClusterReaderImpl(Domain domain, ApplicationRegistry appRegistry, ClusterRef clusterRef) {
        _domain = domain;
        _appRegistry = appRegistry;
        _clusterRef = clusterRef;
        _cluster = domain.getClusterNamed(clusterRef.getRef());
    }

    @Override
    public String getName() {
        return _cluster.getName();
    }

    @Override
    public InstanceReader[] getInstances() throws LbReaderException {

        List<ServerRef> servers = null;
        servers = _cluster.getServerRef();
        InstanceReader[] readers = null;

        if (servers != null) {
            readers = new InstanceReader[servers.size()];

            Iterator<ServerRef> serverIter = servers.iterator();
            int i = 0;
            while (serverIter.hasNext()) {
                readers[i++] = new InstanceReaderImpl(_domain,
                        serverIter.next());
            }
        }

        return readers;
    }

    @Override
    public HealthCheckerReader getHealthChecker() throws LbReaderException {

        if (_clusterRef == null) {
            return HealthCheckerReaderImpl.getDefaultHealthChecker();
        }

        HealthChecker bean = _clusterRef.getHealthChecker();
        if (bean == null) {
            return null;
        } else {
            HealthCheckerReader reader = new HealthCheckerReaderImpl(bean);
            return reader;
        }
    }

    @Override
    public WebModuleReader[] getWebModules() throws LbReaderException {

        List<ApplicationRef> refs = _cluster.getApplicationRef();
        return ClusterReaderHelper.getWebModules(_domain, _appRegistry, refs, _cluster.getName());
    }

    @Override
    public String getLbPolicy() {
        if (_clusterRef == null) {
            return defaultLBPolicy;
        }
        return _clusterRef.getLbPolicy();
    }

    @Override
    public String getLbPolicyModule() {
        if (_clusterRef == null) {
            return null;
        }
        return _clusterRef.getLbPolicyModule();
    }

    @Override
    public void accept(Visitor v) throws Exception{
    	if(v instanceof ClusterVisitor){
        ClusterVisitor cv = (ClusterVisitor) v;
        cv.visit(this);
    	}
    }
    // ---- VARIABLE(S) - PRIVATE --------------------------
    private Cluster _cluster = null;
    private ClusterRef _clusterRef = null;
    private Domain _domain = null;
    private ApplicationRegistry _appRegistry = null;
    private static final String defaultLBPolicy =
            "round-robin";
}
