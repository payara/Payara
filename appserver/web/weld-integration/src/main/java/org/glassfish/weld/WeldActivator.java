/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2014 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.weld;

import org.glassfish.weld.util.Util;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext; 

import org.jboss.weld.bootstrap.api.SingletonProvider;
import org.jboss.weld.bootstrap.api.helpers.TCCLSingletonProvider;

/**
 * This is a bundle activator which is responsible for configuring Weld bundle
 * to be used in GlassFish. As part of configuration, it configures the the
 * SingletonProvider in Weld. It sets different SingletonProvider for different
 * profiles. e.g., in WebProfile, it sets
 * {@link org.jboss.weld.bootstrap.api.helpers.TCCLSingletonProvider}, where as
 * for full-javaee profile, it uses
 * {@link org.glassfish.weld.ACLSingletonProvider}. It tests profile by testing
 * existence of {@link org.glassfish.javaee.full.deployment.EarClassLoader}.
 * 
 * Since Weld 1.1, an implementation of the 
 * {@link org.jboss.weld.serialization.spi.ProxyServices} SPI is used to provide
 * a classloader to load javassist defined proxies. This classloader ensures
 * that they can load not only application defined classes but also classes 
 * exported by any OSGi bundle as long as the operation is happening in 
 * the context of a Java EE app.
 * 
 * The bundle activator resets the SingletonProvicer in stop().
 * 
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class WeldActivator implements BundleActivator {
    public void start(BundleContext context) throws Exception {
      Util.initializeWeldSingletonProvider();
    }

    public void stop(BundleContext context) throws Exception {
        SingletonProvider.reset();
    }

}
