/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.restconnector;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.v3.admin.adapter.AdminEndpointDecider;
import org.glassfish.api.container.Adapter;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.hk2.api.ServiceLocator;

import java.net.InetAddress;
import java.util.List;
import java.util.logging.Logger;

/**
 * Base class for our implementation of Adapter proxies.
 * To avoid early loading of adapter implentations, use a handle-body idiom here.
 * Only operations related to metadata is handled by this class. The rest of the operations are
 * delegated to a delegate which is looked up in the service registry on demand.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public abstract class AbstractProxyRestAdapter implements Adapter {
    // TODO(Sahoo): This class can be moved to kernel and be used as proxy for other Adapter implementations.

    /**
     * Our delegate which depends on a lot of things.
     */
    private ProxiedRestAdapter delegate;

    private boolean registered;

    private AdminEndpointDecider aed;

    /*
     * This is not a component itself, so it can not use injection facility.
     * All injection capable fields are implemented as abstract getters.
     */
    protected abstract ServiceLocator getServices();

    protected abstract Config getConfig();

    protected abstract String getName();

    private synchronized AdminEndpointDecider getEpd() {
        if (aed == null) {
            aed = new AdminEndpointDecider(getConfig());
        }
        return aed;
    }

    /**
     * @return the real adapter - looked up in service registry using {@link #getName}
     */
    private synchronized ProxiedRestAdapter getDelegate() {
        if (delegate == null) {
            delegate = getServices().getService(ProxiedRestAdapter.class, getName());
            if (delegate == null) {
                throw new RuntimeException(
                        "Unable to locate a service of type = " + ProxiedRestAdapter.class + " with name = " + getName());
            }
        }
        return delegate;
    }

    @Override
    public HttpHandler getHttpService() {
        return getDelegate().getHttpService();
    }

    /**
     * Context root this adapter is responsible for handling.
     */
    @Override
    public abstract String getContextRoot();

    @Override
    public int getListenPort() {
        return getEpd().getListenPort();
    }

    @Override
    public InetAddress getListenAddress() {
        return getEpd().getListenAddress();
    }

    @Override
    public List<String> getVirtualServers() {
        return getEpd().getAsadminHosts();
    }

    @Override
    public synchronized boolean isRegistered() {
        return registered;
    }

    @Override
    public synchronized void setRegistered(boolean registered) {
        this.registered = registered;
    }

}
