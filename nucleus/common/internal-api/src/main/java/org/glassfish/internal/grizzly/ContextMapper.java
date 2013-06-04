/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.internal.grizzly;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.naming.NamingContext;
import org.glassfish.grizzly.http.server.util.Mapper;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

/**
 * Extended that {@link Mapper} that prevent the WebContainer to unregister the current {@link Mapper} configuration.
 *
 * @author Jeanfrancois Arcand
 */
@Service
@ContractsProvided({ContextMapper.class, Mapper.class})
public class ContextMapper extends Mapper {
    protected final Logger logger;
    protected HttpHandler adapter;
    // The id of the associated network-listener
    private String id;

    public ContextMapper() {
        this(Logger.getAnonymousLogger());
    }

    public ContextMapper(final Logger logger) {
        this.logger = logger;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addWrapper(final String hostName, final String contextPath, final String path,
        final Object wrapper, final boolean jspWildCard, final String servletName,
        final boolean isEmptyPathSpecial) {
        super.addWrapper(hostName, contextPath, path, wrapper, jspWildCard,
                servletName, isEmptyPathSpecial);
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Wrapper-Host: {0} contextPath {1} wrapper {2} "
                    + "path {3} jspWildcard {4} servletName {5} isEmptyPathSpecial {6}",
                    new Object[]{hostName, contextPath, wrapper, path, jspWildCard,
                        servletName, isEmptyPathSpecial});
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void addHost(final String name, final String[] aliases,
        final Object host) {

        super.addHost(name, aliases, host);
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Host-Host: {0} aliases {1} host {2}",
                    new Object[]{name, Arrays.toString(aliases), host});
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addContext(final String hostName, final String path, final Object context,
        final String[] welcomeResources, final NamingContext resources) {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Context-Host: {0} path {1} context {2} port {3}",
                    new Object[]{hostName, path, context, getPort()});
        }
        // The WebContainer is registering new Context. In that case, we must
        // clean all the previously added information, specially the
        // MappingData.wrapper info as this information cannot apply
        // to this Container.
        if (adapter != null && "org.apache.catalina.connector.CoyoteAdapter".equals(adapter.getClass().getName())) {
            removeContext(hostName, path);
        }
        super.addContext(hostName, path, context, welcomeResources, resources);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void removeHost(final String name) {
        // Do let the WebContainer unconfigure us.
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Faking removal of host: {0}", name);
        }
    }

    public void setHttpHandler(final HttpHandler adapter) {
        this.adapter = adapter;
    }

    public HttpHandler getHttpHandler() {
        return adapter;
    }

    /**
     * Sets the id of the associated http-listener on this mapper.
     */
    public void setId(final String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
