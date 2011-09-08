/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.paas.orchestrator;

import com.sun.enterprise.module.ModulesRegistry;
import com.sun.enterprise.module.single.StaticModulesRegistry;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.embeddable.GlassFishProperties;
import org.glassfish.embeddable.GlassFishRuntime;
import org.jvnet.hk2.component.Habitat;

import java.util.Properties;

/**
 * @author bhavanishankar@java.net
 */

public class StaticClientRuntime extends GlassFishRuntime {

    public GlassFishRuntime setHabitat(Habitat habitat) {
        this.habitat = habitat;
        return this;
    }

    private Habitat habitat;

    @Override
    public void shutdown() throws GlassFishException {
    }

    @Override
    public GlassFish newGlassFish(GlassFishProperties glassFishProperties)
            throws GlassFishException {
        ModulesRegistry registry;
        System.out.println("serverHabitat = [ " + habitat + "]");
        if (habitat == null) {
            ClassLoader ecl = getClass().getClassLoader();
            Thread.currentThread().setContextClassLoader(ecl);
            registry = new StaticModulesRegistry(ecl);
        } else {
            registry = habitat.getComponent(ModulesRegistry.class);
        }

        String habitatName = glassFishProperties.getProperties().getProperty("host")
                + ":" + glassFishProperties.getProperties().getProperty("port");
        Habitat habitat = registry.createHabitat(habitatName);
        System.out.println("Habitat = [ " + habitat + "]");
        Properties cloned = new Properties();
        cloned.putAll(glassFishProperties.getProperties());
        /* System property is set to workaround this error:
        It appears that server [xxxxxxx:4848] does not accept secure connections. Retry with --secure=false.
        javax.net.ssl.SSLException: HelloRequest followed by an unexpected  handshake message
        */
        System.setProperty("sun.security.ssl.allowUnsafeRenegotiation", "true");
        return new GlassFishClient(habitat, cloned);
    }
}
