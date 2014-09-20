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


package com.sun.enterprise.glassfish.bootstrap.osgi;

import com.sun.enterprise.glassfish.bootstrap.Constants;
import org.glassfish.embeddable.BootstrapProperties;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.embeddable.GlassFishRuntime;
import org.glassfish.embeddable.spi.RuntimeBuilder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;

/**
 * This {@link org.glassfish.embeddable.spi.RuntimeBuilder} is responsible for setting up a {@link GlassFishRuntime}
 * when user has a regular installation of GlassFish and they want to embed GlassFish in an existing OSGi runtime.
 * <p/>
 * It sets up the runtime like this:
 * 1. Installs GlassFish modules.
 * 2. Starts a list of GlassFish bundles.
 * 3. Registers an instance of GlassFishRuntime as service.
 * <p/>
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 * @see #build(org.glassfish.embeddable.BootstrapProperties)
 * @see #handles(org.glassfish.embeddable.BootstrapProperties)
 */
public class EmbeddedOSGiGlassFishRuntimeBuilder implements RuntimeBuilder {

    public boolean handles(BootstrapProperties bsProps) {
        return EmbeddedOSGiGlassFishRuntimeBuilder.class.getName().
                equals(bsProps.getProperties().getProperty(Constants.BUILDER_NAME_PROPERTY));
    }

    public GlassFishRuntime build(BootstrapProperties bsProps) throws GlassFishException {
        configureBundles(bsProps);
        provisionBundles(bsProps);
        GlassFishRuntime gfr = new EmbeddedOSGiGlassFishRuntime(getBundleContext());
        getBundleContext().registerService(GlassFishRuntime.class.getName(), gfr, bsProps.getProperties());
        return gfr;
    }

    private void configureBundles(BootstrapProperties bsProps) {
        if (System.getProperty(Constants.PLATFORM_PROPERTY_KEY) == null) { // See GLASSFISH-16511 for null check
            // Set this, because some stupid downstream code may be relying on this property
            System.setProperty(Constants.PLATFORM_PROPERTY_KEY, "GenericOSGi");
        }
    }

    private BundleContext getBundleContext() {
        return BundleReference.class.cast(getClass().getClassLoader()).getBundle().getBundleContext();
    }

    private void provisionBundles(BootstrapProperties bsProps) {
        BundleProvisioner bundleProvisioner = new BundleProvisioner(getBundleContext(), bsProps.getProperties());
        bundleProvisioner.installBundles();
        bundleProvisioner.startBundles();
    }

}
