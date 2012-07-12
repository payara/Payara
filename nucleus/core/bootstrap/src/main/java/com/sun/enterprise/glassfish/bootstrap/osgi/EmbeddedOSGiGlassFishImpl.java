/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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


package com.sun.enterprise.glassfish.bootstrap.osgi;

import com.sun.enterprise.glassfish.bootstrap.GlassFishDecorator;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A specialized implementation of GlassFish which takes care of calling
 * registering & unregistering GlassFish service from service registry when GlassFish is started and stopped.
 *
 * This object is created by {@link EmbeddedOSGiGlassFishRuntime}
 *
 * @author sanjeeb.sahoo@oracle.com
 */
public class EmbeddedOSGiGlassFishImpl extends GlassFishDecorator {
    private final Logger logger = Logger.getLogger(getClass().getPackage().getName());
    private ServiceRegistration reg;
    private final BundleContext bundleContext;

    public EmbeddedOSGiGlassFishImpl(GlassFish decoratedGf, BundleContext bundleContext) {
        super(decoratedGf);
        this.bundleContext = bundleContext;
    }

    @Override
    public void start() throws GlassFishException {
        super.start();
        registerService();
    }

    @Override
    public void stop() throws GlassFishException {
        unregisterService();
        super.stop();
    }

    private void registerService() {
        reg = getBundleContext().registerService(GlassFish.class.getName(), this, null);
        logger.logp(Level.INFO, "EmbeddedOSGiGlassFishImpl", "registerService",
                "Registered {0} as OSGi service registration: {1}", new Object[]{this, reg});
    }

    private void unregisterService() {
        if (getBundleContext() != null) { // bundle is still active
            try {
                reg.unregister();
                logger.logp(Level.INFO, "EmbeddedOSGiGlassFishImpl", "unregisterService",
                        "Unregistered {0} from service registry", new Object[]{this});
            } catch (IllegalStateException e) {
                logger.logp(Level.WARNING, "EmbeddedOSGiGlassFishImpl", "remove", "Exception while unregistering ", e);
            }
        }
    }

    private BundleContext getBundleContext() {
        return bundleContext;
    }
}
