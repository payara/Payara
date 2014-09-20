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
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.ServerRef;
import com.sun.enterprise.config.serverbeans.HealthChecker;
import com.sun.enterprise.config.serverbeans.Server;

import org.glassfish.loadbalancer.admin.cli.transform.Visitor;
import org.glassfish.loadbalancer.admin.cli.transform.ClusterVisitor;

import org.glassfish.loadbalancer.admin.cli.reader.api.ClusterReader;
import org.glassfish.loadbalancer.admin.cli.reader.api.InstanceReader;
import org.glassfish.loadbalancer.admin.cli.reader.api.WebModuleReader;
import org.glassfish.loadbalancer.admin.cli.reader.api.HealthCheckerReader;

import java.util.List;
import org.glassfish.internal.data.ApplicationRegistry;

import org.glassfish.loadbalancer.admin.cli.reader.api.LbReaderException;

/**
 * Impl class for ClusterReader. This provides loadbalancer 
 * data for a stand alone server (reverse proxy).
 *
 * @author Kshitiz Saxena
 */
public class StandAloneClusterReaderImpl implements ClusterReader {

    public StandAloneClusterReaderImpl(Domain domain, ApplicationRegistry appRegistry, ServerRef ref)
            throws LbReaderException {
        _domain = domain;
        _appRegistry = appRegistry;
        _serverRef = ref;
        _server = domain.getServerNamed(_serverRef.getRef());
    }

    public StandAloneClusterReaderImpl(Domain domain, ApplicationRegistry appRegistry, Server server)
            throws LbReaderException {
        _domain = domain;
        _appRegistry = appRegistry;
        _server = server;
    }

    @Override
    public String getName() {
        return _server.getName();
    }

    @Override
    public InstanceReader[] getInstances() throws LbReaderException {
        InstanceReader[] readers = new InstanceReader[1];
        if(_serverRef != null){
            readers[0] = new InstanceReaderImpl(_domain, _serverRef);
        } else {
            readers[0] = new InstanceReaderImpl(_domain, _server);
        }
        return readers;
    }

    @Override
    public HealthCheckerReader getHealthChecker() throws LbReaderException {
        HealthChecker bean = null;
        if(_serverRef != null){
            bean = _serverRef.getHealthChecker();
        }
        if (bean == null) {
            return HealthCheckerReaderImpl.getDefaultHealthChecker();
        } else {
            HealthCheckerReader reader = new HealthCheckerReaderImpl(bean);
            return reader;
        }
    }

    @Override
    public WebModuleReader[] getWebModules() throws LbReaderException {
        List<ApplicationRef> refs = _server.getApplicationRef();

        return ClusterReaderHelper.getWebModules(_domain, _appRegistry, refs,
                _server.getName());
    }

    @Override
    public String getLbPolicy() {
        return null;
    }

    @Override
    public String getLbPolicyModule() {
        return null;
    }

    @Override
    public void accept(Visitor v) throws Exception {
		if (v instanceof ClusterVisitor) {
			ClusterVisitor cv = (ClusterVisitor) v;
			cv.visit(this);
		}
    }
    // ---- VARIABLE(S) - PRIVATE --------------------------
    private Domain _domain = null;
    private ApplicationRegistry _appRegistry = null;
    private ServerRef _serverRef = null;
    private Server _server = null;
}
