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

package org.glassfish.paas.gfplugin;

import com.sun.enterprise.admin.cli.Environment;
import com.sun.enterprise.admin.cli.ProgramOptions;
import com.sun.hk2.component.ExistingSingletonInhabitant;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.embeddable.Deployer;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.Inhabitant;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @author bhavanishankar@java.net
 */

public class GlassFishClient implements GlassFish {

    Properties glassfishProperties;

    Habitat habitat;
    CommandRunner commandRunner;
    Deployer deployer;
    List<Inhabitant> inhabitantList = new ArrayList();

    public GlassFishClient(Habitat habitat, Properties gfProps) {
        this.habitat = habitat;
        this.glassfishProperties = gfProps;
        try {
            ParameterMap parameterMap = new ParameterMap();
            parameterMap.add("host", glassfishProperties.getProperty("host"));
            parameterMap.add("secure", "false"); // TODO :: secure=true might wait for console input to accept certificate if System.console() != null

            // add the inhabitants required by CLICommand.
            Environment env = new Environment();
            ProgramOptions programOptions = new ProgramOptions(parameterMap, env);
            Inhabitant<Environment> envHabitat =
                    new ExistingSingletonInhabitant<Environment>(env);
            Inhabitant<ProgramOptions> programOptionsInhabitant =
                    new ExistingSingletonInhabitant<ProgramOptions>(programOptions);
            habitat.add(envHabitat);
            habitat.add(programOptionsInhabitant);
//            habitat.addIndex(envHabitat, Environment.class.getName(), null);
//            habitat.addIndex(programOptionsInhabitant, ProgramOptions.class.getName(), null);

            // keep the list of added inhabitants, so that they can be remove during dispose();
            inhabitantList.add(envHabitat);
            inhabitantList.add(programOptionsInhabitant);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void start() throws GlassFishException {
    }

    public void stop() throws GlassFishException {
    }

    public void dispose() throws GlassFishException {
        for (Inhabitant inhabitant : inhabitantList) {
            habitat.remove(inhabitant);
        }
    }

    public Status getStatus() throws GlassFishException {
        return null;
    }

    public <T> T getService(Class<T> tClass) throws GlassFishException {
        return null;
    }

    public <T> T getService(Class<T> tClass, String s) throws GlassFishException {
        return null;
    }

    public Deployer getDeployer() throws GlassFishException {
        if (deployer == null) {
            deployer = new DeployerClient(getCommandRunner());
        }
        return deployer;
    }

    public CommandRunner getCommandRunner() throws GlassFishException {
        if (commandRunner == null) {
            commandRunner = new CommandRunnerClient(habitat);
        }
        return commandRunner;
    }
}
