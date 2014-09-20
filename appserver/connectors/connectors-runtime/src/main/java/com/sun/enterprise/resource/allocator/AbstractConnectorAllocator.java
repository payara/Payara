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

package com.sun.enterprise.resource.allocator;

import com.sun.enterprise.deployment.ConnectorDescriptor;
import com.sun.enterprise.resource.ClientSecurityInfo;
import com.sun.enterprise.resource.ResourceHandle;
import com.sun.enterprise.resource.ResourceSpec;
import com.sun.enterprise.resource.AssocWithThreadResourceHandle;
import com.sun.enterprise.resource.pool.PoolManager;
import com.sun.enterprise.connectors.ConnectorRuntime;
import com.sun.appserv.connectors.internal.api.PoolingException;
import com.sun.appserv.connectors.internal.api.ConnectorRuntimeException;
import com.sun.appserv.connectors.internal.api.ConnectorConstants;
import com.sun.logging.LogDomains;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ValidatingManagedConnectionFactory;
import javax.security.auth.Subject;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * An abstract implementation of the <code>ResourceAllocator</code> interface
 * that houses all the common implementation(s) of the various connector allocators.
 * All resource allocators except <code>BasicResourceAllocator</code> extend this
 * abstract implementation
 *
 * @author Sivakumar Thyagarajan
 */
public abstract class AbstractConnectorAllocator
        implements ResourceAllocator {

    protected PoolManager poolMgr;
    protected ResourceSpec spec;
    protected ConnectionRequestInfo reqInfo;
    protected Subject subject;
    protected ManagedConnectionFactory mcf;
    protected ConnectorDescriptor desc;
    protected ClientSecurityInfo info;

    protected final static Logger _logger = LogDomains.getLogger(AbstractConnectorAllocator.class,LogDomains.RSR_LOGGER);

    public AbstractConnectorAllocator() {
    }

    public AbstractConnectorAllocator(PoolManager poolMgr,
                                      ManagedConnectionFactory mcf,
                                      ResourceSpec spec,
                                      Subject subject,
                                      ConnectionRequestInfo reqInfo,
                                      ClientSecurityInfo info,
                                      ConnectorDescriptor desc) {
        this.poolMgr = poolMgr;
        this.mcf = mcf;
        this.spec = spec;
        this.subject = subject;
        this.reqInfo = reqInfo;
        this.info = info;
        this.desc = desc;

    }

    public Set getInvalidConnections(Set connectionSet)
            throws ResourceException {
        if (mcf instanceof ValidatingManagedConnectionFactory) {
            return ((ValidatingManagedConnectionFactory) this.mcf).
                    getInvalidConnections(connectionSet);
        }
        return null;
    }

    public boolean isConnectionValid(ResourceHandle h) {
        HashSet conn = new HashSet();
        conn.add(h.getResource());
        Set invalids = null;
        try {
            invalids = getInvalidConnections(conn);
        } catch (ResourceException re) {
            //ignore and continue??
            //there's nothing the container can do but log it.
            Object[] args = new Object[] {
                    h.getResourceSpec().getPoolInfo(),
                    re.getClass(),
                    re.getMessage() };
            _logger.log(Level.WARNING,
                    "pool.get_invalid_connections_resourceexception", args);
            if(_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "", re);
            }
        }

        if ((invalids != null && invalids.size() > 0) ||
                h.hasConnectionErrorOccurred()) {
            return false;
        }

        return true;
    }

    public void destroyResource(ResourceHandle resourceHandle)
            throws PoolingException {
        throw new UnsupportedOperationException();
    }

    public void fillInResourceObjects(ResourceHandle resourceHandle)
            throws PoolingException {
        throw new UnsupportedOperationException();
    }

    public boolean supportsReauthentication() {
        return this.desc.supportsReauthentication();
    }

    public boolean isTransactional() {
        return true;
    }

    public void cleanup(ResourceHandle h) throws PoolingException {
        try {
            ManagedConnection mc = (ManagedConnection) h.getResource();
            mc.cleanup();
        } catch (Exception ex) {
            _logger.log(Level.WARNING, "managed_con.cleanup-failed", ex);
            throw new PoolingException(ex.toString(), ex);
        }
    }

    public boolean matchConnection(ResourceHandle h) {
        Set set = new HashSet();
        set.add(h.getResource());
        try {
            ManagedConnection mc =
                    mcf.matchManagedConnections(set, subject, reqInfo);
            return (mc != null);
        } catch (ResourceException ex) {
            return false;
        }
    }

    public void closeUserConnection(ResourceHandle resource) throws PoolingException {

        try {
            ManagedConnection mc = (ManagedConnection) resource.getResource();
            mc.cleanup();
        } catch (ResourceException ex) {
            throw new PoolingException(ex);
        }
    }

    public boolean shareableWithinComponent() {
        return false;
    }

    public Object getSharedConnection(ResourceHandle h)
            throws PoolingException {
        throw new UnsupportedOperationException();
    }

    protected ResourceHandle createResourceHandle(Object resource, ResourceSpec spec,
                                                  ResourceAllocator alloc, ClientSecurityInfo info) {

        ConnectorConstants.PoolType pt = ConnectorConstants.PoolType.STANDARD_POOL;
        try {
            pt = ConnectorRuntime.getRuntime().getPoolType(spec.getPoolInfo());
        } catch (ConnectorRuntimeException cre) {
            _logger.log(Level.WARNING,"unable_to_determine_pool_type", spec.getPoolInfo());
        }
        if (pt == ConnectorConstants.PoolType.ASSOCIATE_WITH_THREAD_POOL) {
            return new AssocWithThreadResourceHandle(resource, spec, alloc, info);
        } else {
            return new ResourceHandle(resource, spec, alloc, info);
        }
    }

    public boolean hasValidatingMCF() {
        return mcf instanceof ValidatingManagedConnectionFactory;
    }

}
