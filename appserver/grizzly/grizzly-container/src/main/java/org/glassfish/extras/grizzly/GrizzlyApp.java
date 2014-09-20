/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.extras.grizzly;

import org.glassfish.api.deployment.ApplicationContainer;
import org.glassfish.api.deployment.ApplicationContext;
import org.glassfish.api.container.RequestDispatcher;
import org.glassfish.api.container.EndpointRegistrationException;

import com.sun.logging.LogDomains;
import org.glassfish.grizzly.http.server.HttpHandler;

import java.util.Collection;
import java.util.logging.Level;

/**
 * Deployed grizzly application.
 *
 * @author Jerome Dochez
 */
public class GrizzlyApp implements ApplicationContainer {

    final ClassLoader cl;
    final Collection<Adapter> modules;
    final RequestDispatcher dispatcher;

    public static final class Adapter {
        final HttpHandler service;
        final String contextRoot;        
        public Adapter(String contextRoot, HttpHandler adapter) {
            this.service = adapter;
            this.contextRoot = contextRoot;
        }
    }

    public GrizzlyApp(Collection<Adapter> adapters, RequestDispatcher dispatcher, ClassLoader cl) {
        this.modules = adapters;
        this.dispatcher = dispatcher;
        this.cl = cl;
    }

    public Object getDescriptor() {
        return null;
    }

    public boolean start(ApplicationContext startupContext) throws Exception {
        for (Adapter module : modules) {
            dispatcher.registerEndpoint(module.contextRoot, module.service, this);
        }
        return true;
    }

    public boolean stop(ApplicationContext stopContext) {
        boolean success = true;
        for (Adapter module : modules) {
            try {
                dispatcher.unregisterEndpoint(module.contextRoot);
            } catch (EndpointRegistrationException e) {
                LogDomains.getLogger(getClass(), LogDomains.DPL_LOGGER).log(
                        Level.SEVERE, "Exception while unregistering adapter at " + module.contextRoot, e);
                success = false;
            }
        }
        return success;
    }

    public boolean suspend() {
        return false;
    }

    public boolean resume() throws Exception {
        return false;
    }

    public ClassLoader getClassLoader() {
        return cl;
    }
}
