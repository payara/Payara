/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.micro.boot.runtime;

import com.sun.enterprise.module.bootstrap.ModuleStartup;
import java.util.Properties;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.embeddable.Deployer;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.hk2.api.ServiceLocator;

/**
 *
 * @author Steve Millidge
 */
public class MicroGlassFish implements GlassFish {
    
    private ModuleStartup kernel;
    private ServiceLocator habitat;
    private Status status = Status.INIT;

    MicroGlassFish(ModuleStartup kernel, ServiceLocator habitat, Properties glassfishProperties) throws GlassFishException {
        this.kernel = kernel;
        this.habitat = habitat;
    }

    @Override
    public void start() throws GlassFishException {
        status = Status.STARTING;
        kernel.start();
        status = Status.STARTED;
    }

    @Override
    public void stop() throws GlassFishException {
        if (status == Status.STOPPED || status == Status.STOPPING || status == Status.DISPOSED) {
            throw new IllegalStateException("Already in " + status + " state.");
        }
        
        status = Status.STOPPING;
        kernel.stop();
        status = Status.STOPPED;
    }

    @Override
    public void dispose() throws GlassFishException {
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
        
        kernel = null;
        habitat = null;
        status = Status.DISPOSED;
    }

    @Override
    public Status getStatus() throws GlassFishException {
        return status;
    }

    @Override
    public <T> T getService(Class<T> serviceType) throws GlassFishException {
        return habitat.getService(serviceType);
    }

    @Override
    public <T> T getService(Class<T> serviceType, String serviceName) throws GlassFishException {
        return habitat.getService(serviceType, serviceName);
    }

    @Override
    public Deployer getDeployer() throws GlassFishException {
        return getService(Deployer.class);
    }

    @Override
    public CommandRunner getCommandRunner() throws GlassFishException {
        return getService(CommandRunner.class);
    }

}
