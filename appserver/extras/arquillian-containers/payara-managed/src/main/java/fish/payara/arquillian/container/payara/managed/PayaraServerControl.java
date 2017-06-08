/*
 * Copyright (c) 2017 Payara Foundation and/or its affiliates. All rights reserved.
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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fish.payara.arquillian.container.payara.managed;

import static java.lang.Runtime.getRuntime;
import static java.util.Collections.emptyList;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.arquillian.container.spi.client.container.LifecycleException;

/**
 * A class for issuing asadmin commands using the admin-cli.jar of the GlassFish distribution.
 *
 * @author <a href="http://community.jboss.org/people/dan.j.allen">Dan Allen</a>
 */
class PayaraServerControl {
    
    private static final Logger logger = Logger.getLogger(PayaraServerControl.class.getName());

    private static final String DERBY_MISCONFIGURED_HINT =
        "It seems that the Payara version you are running might have a problem starting embedded "  +
        "Derby database. Please take a look at the server logs. You can also switch off 'enableDerby' property in your 'arquillian.xml' if you don't need it. For " +
        "more information please refer to relevant issues for existing workarounds: https://java.net/jira/browse/GLASSFISH-21004 " +
        "https://issues.apache.org/jira/browse/DERBY-6438";

    private static final List<String> NO_ARGS = emptyList();

    private PayaraManagedContainerConfiguration config;
    private Thread shutdownHook;

    PayaraServerControl(PayaraManagedContainerConfiguration config) {
        this.config = config;
    }

    void start() throws LifecycleException {
        registerShutdownHook();

        if (config.isEnableDerby()) {
            startDerbyDatabase();
        }

        final List<String> args = new ArrayList<String>();
        if (config.isDebug()) {
            args.add("--debug");
        }
        
        executeAdminDomainCommand("Starting container", "start-domain", args, createProcessOutputConsumer());
    }

    void stop() throws LifecycleException {
        removeShutdownHook();
        try {
            stopContainer();
        } catch (LifecycleException failedStoppingContainer) {
            logger.log(Level.SEVERE, "Failed stopping container.", failedStoppingContainer);
        } finally {
            stopDerbyDatabase();
        }
    }

    private void stopContainer() throws LifecycleException {
        executeAdminDomainCommand("Stopping container", "stop-domain", NO_ARGS, createProcessOutputConsumer());
    }

    private void startDerbyDatabase() throws LifecycleException {
        if (!config.isEnableDerby()) {
            return;
        }
        
        try {
            executeAdminDomainCommand("Starting database", "start-database", NO_ARGS, createProcessOutputConsumer());
        } catch (LifecycleException e) {
            logger.warning(DERBY_MISCONFIGURED_HINT);
            throw e;
        }
    }

    private void stopDerbyDatabase() throws LifecycleException {
        if (config.isEnableDerby()) {
            executeAdminDomainCommand("Stopping database", "stop-database", NO_ARGS, createProcessOutputConsumer());
        }
    }

    private void removeShutdownHook() {
        if (shutdownHook != null) {
            getRuntime().removeShutdownHook(shutdownHook);
            shutdownHook = null;
        }
    }

    private void registerShutdownHook() {
        shutdownHook = new Thread(new Runnable() {
            public void run() {
                logger.warning("Forcing container shutdown");
                try {
                    stopContainer();
                    stopDerbyDatabase();
                } catch (LifecycleException e) {
                    logger.log(Level.SEVERE, "Failed stopping services through shutdown hook.", e);
                }
            }
        });
        
        getRuntime().addShutdownHook(shutdownHook);
    }

    private void executeAdminDomainCommand(String description, String adminCmd, List<String> args,
        ProcessOutputConsumer consumer) throws LifecycleException {
        if (config.getDomain() != null) {
            args.add(config.getDomain());
        }

        executeAdminCommand(description, adminCmd, args, consumer);
    }

    private void executeAdminCommand(String description, String command, List<String> args,
        ProcessOutputConsumer consumer) throws LifecycleException {
        final List<String> cmd = buildCommand(command, args);

        if (config.isOutputToConsole()) {
            System.out.println(description + " using command: " + cmd.toString());
        }

        Process process = null;
        ConsoleReader consoleReader = null;
        int result;
        try {
            process = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            consoleReader = new ConsoleReader(process, consumer);
            new Thread(consoleReader).start();
            result = process.waitFor();
        } catch (IOException e) {
            logger.log(Level.SEVERE, description + " failed.", e);
            throw new LifecycleException("Unable to execute " + cmd.toString(), e);
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, description + " interrupted.", e);
            throw new LifecycleException("Unable to execute " + cmd.toString(), e);
        } finally {
            if (consoleReader != null) {
                consoleReader.close();
            }
            if (process != null) {
                process.destroy();
            }
        }

        if (result != 0) {
            throw new LifecycleException("Unable to execute " + cmd.toString());
        }
    }

    private List<String> buildCommand(String command, List<String> args) {
        List<String> cmd = new ArrayList<String>();
        cmd.add("java");

        cmd.add("-jar");
        cmd.add(config.getAdminCliJar().getAbsolutePath());

        cmd.add(command);
        cmd.addAll(args);

        // very concise output data in a format that is optimized for use in scripts instead of for reading by humans
        cmd.add("-t");
        return cmd;
    }

    private static class ConsoleReader implements Runnable, Closeable {

        private final ProcessOutputConsumer consumer;

        private final BufferedReader reader;

        private ConsoleReader(final Process process, ProcessOutputConsumer consumer) {
            this.reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            this.consumer = consumer;
        }

        public void run() {
            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    consumer.consume(line);
                }
            } catch (IOException failOnReading) {
                logger.log(Level.SEVERE, failOnReading.getMessage(), failOnReading);
            }
        }

        public void close() {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException failOnClose) {
                    logger.log(Level.SEVERE, failOnClose.getMessage(), failOnClose);
                }
            }
        }
    }

    private interface ProcessOutputConsumer {

        void consume(String line);
    }

    private ProcessOutputConsumer createProcessOutputConsumer() {
        if (config.isOutputToConsole()) {
            return new OutputLoggingConsumer();
        }
        return new SilentOutputConsumer();
    }

    private static class OutputLoggingConsumer implements ProcessOutputConsumer {
        public void consume(String line) {
            System.out.println(line);
        }
    }

    private class SilentOutputConsumer implements ProcessOutputConsumer {
        public void consume(String line) {
            // noop
        }
    }
}
