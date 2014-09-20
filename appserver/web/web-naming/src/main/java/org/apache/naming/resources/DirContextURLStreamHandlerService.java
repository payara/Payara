/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.apache.naming.resources;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.url.AbstractURLStreamHandlerService;
import org.osgi.service.url.URLConstants;
import org.osgi.service.url.URLStreamHandlerService;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

/**
 * This class is responsible for adding {@code DirContextURLStreamHandler}
 * to OSGi service registry.
 *
 * As much as we would have liked it to be both an activator as well as a Startup service, we can't.
 * In embedded mode, this Startup service would fail to load, so we have separated the startup servuce part to
 * {@link WebNamingStartup} class. That Startup service ensures that this bundle gets activated
 * during server startup and the activator ensures that we register a jndi protocol handler.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class DirContextURLStreamHandlerService
        extends AbstractURLStreamHandlerService
        implements BundleActivator {

    // We have to extend DirContextURLStreamHandler so that we
    // can make openConnection and toExternalForm available as
    // public methods.
    private static class DelegatingDirContextURLStreamHandler
            extends DirContextURLStreamHandler{
        @Override
        public URLConnection openConnection(URL u) throws IOException {
            return super.openConnection(u);
        }

        @Override
        public String toExternalForm(URL u) {
            return super.toExternalForm(u);
        }
    }

    public URLConnection openConnection(URL u) throws IOException {
        return new DelegatingDirContextURLStreamHandler().openConnection(u);
    }

    @Override
    public String toExternalForm(URL u) {
        return new DelegatingDirContextURLStreamHandler().toExternalForm(u);
    }

    public void start(BundleContext context) throws Exception {
        Properties p = new Properties();
        p.put(URLConstants.URL_HANDLER_PROTOCOL,
                new String[]{"jndi"});
        context.registerService(
                URLStreamHandlerService.class.getName(),
                this,
                p);
    }

    public void stop(BundleContext context) throws Exception {
    }
}
