/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2016 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.glassfish.bootstrap.GlassFishRuntimeDecorator;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.embeddable.GlassFishProperties;
import org.glassfish.embeddable.GlassFishRuntime;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;

/**
 * This is a special implementation used in non-embedded environment. It assumes that it has launched the
 * framework during bootstrap and hence can stop it upon shutdown.
 * It also creates a specialized GlassFishImpl called {@link OSGiGlassFishImpl}
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class OSGiGlassFishRuntime extends GlassFishRuntimeDecorator {

    // cache the value, because we can't use bundleContext after this bundle is stopped.
    private volatile Framework framework; // system bundle is the framework

    public OSGiGlassFishRuntime(GlassFishRuntime embeddedGfr, final Framework framework) {
        super(embeddedGfr);
        this.framework = framework;
    }

    @Override
    public void shutdown() throws GlassFishException {
        if (framework == null) {
            return; // already shutdown
        }
        try {
            super.shutdown();
            
            framework.stop();
            framework.waitForStop(0);
        } catch (InterruptedException ex) {
            throw new GlassFishException(ex);
        } catch (BundleException ex) {
            throw new GlassFishException(ex);
        }
        finally {
            framework = null; // guard against repeated calls.
        }
    }

    @Override
    public GlassFish newGlassFish(GlassFishProperties glassfishProperties) throws GlassFishException {
        GlassFish embeddedGf = super.newGlassFish(glassfishProperties);
        int finalStartLevel = Integer.parseInt(glassfishProperties.getProperties().getProperty(
                Constants.FINAL_START_LEVEL_PROP, "2"));
        return new OSGiGlassFishImpl(embeddedGf, framework.getBundleContext(), finalStartLevel);
    }

}
