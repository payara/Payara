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
package fish.payara.micro.boot;

import com.sun.enterprise.module.bootstrap.ModuleStartup;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.embeddable.CommandResult;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.embeddable.Deployer;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.hk2.api.ServiceLocator;

/**
 *
 * @author steve
 */
public class MicroGlassFish implements GlassFish {
    
    public static final String PREBOOT_FILE_PROP="micro.preboot.command.file";
    public static final String POSTBOOT_FILE_PROP="micro.postboot.command.file";
    

    private ModuleStartup kernel;
    private ServiceLocator habitat;
    private Status status = Status.INIT;
    private File preBootFile;
    private File postBootFile;

    MicroGlassFish(ModuleStartup kernel, ServiceLocator habitat, Properties glassfishProperties) throws GlassFishException {
        this.kernel = kernel;
        this.habitat = habitat;
        
        // get preboot and postboot files
        String preBootFilename = glassfishProperties.getProperty(PREBOOT_FILE_PROP);
        if (preBootFilename != null) {
            this.preBootFile = new File(preBootFilename);
        }
        
        String postBootFilename = glassfishProperties.getProperty(POSTBOOT_FILE_PROP);
        if (postBootFilename != null) {
            this.postBootFile = new File(postBootFilename);
        }
        
        CommandRunner commandRunner = null;
        for (Object obj : glassfishProperties.keySet()) {
            String key = (String) obj;
            if (key.startsWith("embedded-glassfish-config.")) {
                if (commandRunner == null) {
                    // only create the CommandRunner if needed
                    commandRunner = habitat.getService(CommandRunner.class);
                }
                CommandResult result = commandRunner.run("set",
                        key.substring("embedded-glassfish-config.".length()) + "=" + glassfishProperties.getProperty(key));
                if (result.getExitStatus() != CommandResult.ExitStatus.SUCCESS) {
                    throw new GlassFishException(result.getOutput());
                }
            }
        }
    }

    @Override
    public void start() throws GlassFishException {
        runPreBootCommands();
        status = Status.STARTING;
        kernel.start();
        status = Status.STARTED;
        runPostBootCommands();
    }

    @Override
    public void stop() throws GlassFishException {
        status = Status.STOPPING;
        kernel.stop();
        status = Status.STOPPED;
    }

    @Override
    public void dispose() throws GlassFishException {
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

    private void runPreBootCommands() {
        URL scriptURL = Thread.currentThread().getContextClassLoader().getResource("MICRO-INF/pre-boot-commands.txt");
        if (scriptURL != null) {
            runCommandScript(scriptURL);
        }
        
        // run preboot file 
        if (preBootFile != null && preBootFile.canRead()) {
            try {
                runCommandScript(preBootFile.toURI().toURL());
            } catch (MalformedURLException ex) {
                Logger.getLogger(MicroGlassFish.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void runPostBootCommands() {
        URL scriptURL = Thread.currentThread().getContextClassLoader().getResource("MICRO-INF/post-boot-commands.txt");
        if (scriptURL != null) {
            runCommandScript(scriptURL);
        }
        
        // run boot file 
        if (postBootFile != null && postBootFile.canRead()) {
            try {
                runCommandScript(postBootFile.toURI().toURL());
            } catch (MalformedURLException ex) {
                Logger.getLogger(MicroGlassFish.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void runCommandScript(URL scriptURL) {
        try (InputStream scriptStream = scriptURL.openStream()) {
            BufferedReader br = new BufferedReader(new InputStreamReader(scriptStream));

            String commandStr = br.readLine();
            while (commandStr != null) {
                // # is a comment
                if (!commandStr.startsWith("#")) {
                    String command[] = commandStr.split(" ");
                    if (command.length > 1) {
                        CommandRunner cr = getCommandRunner();
                        CommandResult result = cr.run(command[0], Arrays.copyOfRange(command, 1, command.length));
                        Logger.getLogger(MicroGlassFish.class.getName()).log(Level.INFO, result.getOutput());
                    } else if (command.length == 1) {
                        CommandRunner cr = getCommandRunner();
                        CommandResult result = cr.run(command[0]);
                        Logger.getLogger(MicroGlassFish.class.getName()).log(Level.INFO, result.getOutput());
                    }
                }
                commandStr = br.readLine();
            }
        } catch (IOException | GlassFishException ex) {
            Logger.getLogger(MicroGlassFish.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
