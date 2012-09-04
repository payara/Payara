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

package com.sun.enterprise.glassfish.bootstrap;

import com.sun.enterprise.module.bootstrap.ModuleStartup;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.embeddable.Deployer;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.hk2.api.ServiceLocator;

import java.util.Properties;

/**
 * @author Sanjeeb.Sahoo@Sun.COM
 */

public class GlassFishImpl implements GlassFish {

    private ModuleStartup gfKernel;
    private ServiceLocator habitat;
    volatile Status status = Status.INIT;

    public GlassFishImpl(ModuleStartup gfKernel, ServiceLocator habitat, Properties gfProps) throws GlassFishException {
        this.gfKernel = gfKernel;
        this.habitat = habitat;
        configure(gfProps);
    }

    private void configure(Properties gfProps) throws GlassFishException {
        // If there are custom configurations like http.port, https.port, jmx.port then configure them.
        Configurator configurator = new ConfiguratorImpl(habitat);
        configurator.configure(gfProps);
    }

    public synchronized void start() throws GlassFishException {
        if (status == Status.STARTED || status == Status.STARTING || status == Status.DISPOSED) {
            throw new IllegalStateException("Already in " + status + " state.");
        }
        status = Status.STARTING;
        gfKernel.start();
        status = Status.STARTED;
    }

    public synchronized void stop() throws GlassFishException {
        if (status == Status.STOPPED || status == Status.STOPPING || status == Status.DISPOSED) {
            throw new IllegalStateException("Already in " + status + " state.");
        }
        status = Status.STOPPING;
        gfKernel.stop();
        status = Status.STOPPED;
    }

    public synchronized void dispose() throws GlassFishException {
        if (status == Status.DISPOSED) {
            throw new IllegalStateException("Already disposed.");
        } else if (status != Status.STOPPED) {
            try {
                stop();
            } catch (Exception e) {
                // ignore and continue.
                e.printStackTrace();
            }
        }
        this.gfKernel = null;
        this.habitat = null;
        this.status = Status.DISPOSED;
    }

    public Status getStatus() {
        return status;
    }

    public <T> T getService(Class<T> serviceType) throws GlassFishException {
        return getService(serviceType, null);
    }

    public synchronized <T> T getService(Class<T> serviceType, String serviceName) throws GlassFishException {
        if (status != Status.STARTED) {
            throw new IllegalArgumentException("Server is not started yet. It is in " + status + "state");
        }

        return serviceName != null ? habitat.<T>getService(serviceType, serviceName) :
                habitat.<T>getService(serviceType);
    }

    public Deployer getDeployer() throws GlassFishException {
        return getService(Deployer.class);
    }

    public CommandRunner getCommandRunner() throws GlassFishException {
        return getService(CommandRunner.class);
    }

}
