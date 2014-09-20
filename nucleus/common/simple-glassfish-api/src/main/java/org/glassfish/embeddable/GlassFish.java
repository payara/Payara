/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.embeddable;

/**
 * Represents a GlassFish instance and provides the ability to:
 *
 * <ul>
 * <li> perform life cycle operations viz., start, stop and dispose. </li>
 * <li> access {@link Deployer} to deploy/undeploy applications. </li>
 * <li> access {@link CommandRunner} to perform runtime configurations.</li>
 * <li> access to available service(s). </li>
 * </ul>
 *
 * <p/> Usage example:
 *
 * <style type="text/css">
.ln { color: rgb(0,0,0); font-weight: normal; font-style: normal; }
.s0 { color: rgb(128,128,128); }
.s1 { }
.s2 { color: rgb(0,0,255); }
.s3 { color: rgb(128,128,128); font-weight: bold; }
.s4 { color: rgb(255,0,255); }
</style>
 *
 * <pre>
<span class="s0">
        /** Create and start GlassFish &#42;&#47;</span><span class="s1">
        {@link GlassFish} glassfish = {@link GlassFishRuntime}.bootstrap().newGlassFish();
        glassfish.start();

        </span><span class="s0">/** Deploy a web application simple.war with /hello as context root. &#42;&#47;</span><span class="s1">
        {@link Deployer} deployer = glassfish.getService(Deployer.</span><span class="s2">class</span><span class="s1">);
        String deployedApp = deployer.deploy(</span><span class="s2">new </span><span class="s1">File(</span><span class="s4">&quot;simple.war&quot;</span><span class="s1">).toURI(),
                </span><span class="s4">&quot;--contextroot=hello&quot;</span><span class="s1">, </span><span class="s4">&quot;--force=true&quot;</span><span class="s1">);

        </span><span class="s0">/** Run commands (as per your need). Here is an example to create a http listener and dynamically set its thread pool size. &#42;&#47;</span><span class="s1">
        {@link CommandRunner} commandRunner = glassfish.getService(CommandRunner.</span><span class="s2">class</span><span class="s1">);

        <span class="s0">// Run a command create 'my-http-listener' to listen at 9090</span>
        {@link CommandResult} commandResult = commandRunner.run(
                </span><span class="s4">&quot;create-http-listener&quot;</span><span class="s1">, </span><span class="s4">&quot;--listenerport=9090&quot;</span><span class="s1">,
                </span><span class="s4">&quot;--listeneraddress=0.0.0.0&quot;</span><span class="s1">, </span><span class="s4">&quot;--defaultvs=server&quot;</span><span class="s1">,
                </span><span class="s4">&quot;my-http-listener&quot;</span><span class="s1">);

        </span><span class="s0">// Run a command to create your own thread pool</span><span class="s1">
        commandResult = commandRunner.run(</span><span class="s4">&quot;create-threadpool&quot;</span><span class="s1">,
                </span><span class="s4">&quot;--maxthreadpoolsize=200&quot;</span><span class="s1">, </span><span class="s4">&quot;--minthreadpoolsize=200&quot;</span><span class="s1">,
                </span><span class="s4">&quot;my-thread-pool&quot;</span><span class="s1">);

        </span><span class="s0">// Run a command to associate my-thread-pool with my-http-listener</span><span class="s1">
        commandResult = commandRunner.run(</span><span class="s4">&quot;set&quot;</span><span class="s1">,
                </span><span class="s4">&quot;server.network-config.network-listeners.network-listener.&quot; </span><span class="s1">+
                        </span><span class="s4">&quot;my-http-listener.thread-pool=my-thread-pool&quot;</span><span class="s1">);

        </span><span class="s0">/** Undeploy the application &#42;&#47;</span><span class="s1">
        deployer.undeploy(deployedApp);

        </span><span class="s0">/**Stop GlassFish.&#42;&#47;</span><span class="s1">
        glassfish.stop();

        </span><span class="s0">/** Dispose GlassFish. &#42;&#47;</span><span class="s1">
        glassfish.dispose();
 * </pre>
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public interface GlassFish {
    /**
     * Start GlassFish.
     * When this method is called, all the lifecycle (aka startup) services are started.
     * Calling this method while the server is in {@link Status#STARTED} state is a no-op.
     *
     * @throws {@link IllegalStateException} if server is already started.
     * @throws GlassFishException if server can't be started for some unknown reason.
     */
    void start() throws GlassFishException;

    /**
     * Stop GlassFish. When this method is called, all the lifecycle (aka startup) services are stopped.
     * GlassFish can be started again by calling the start method.
     * Calling this method while the server is in {@link Status#STARTED} state is a no-op.
     *
     * @throws {@link IllegalStateException} if server is already stopped.
     * @throws GlassFishException if server can't be started for some unknown reason.
     */
    void stop() throws GlassFishException;

    /**
     * Call this method if you don't need this GlassFish instance any more. This method will stop GlassFish
     * if not already stopped. After this method is called, calling any method except {@link #getStatus}
     * on the GlassFish object will cause an IllegalStateException to be thrown. When this method is called,
     * any resource (like temporary files, threads, etc.) is also released.
     */
    void dispose() throws GlassFishException;

    /**
     * Get the current status of GlassFish.
     * 
     * @return Status of GlassFish
     */
    Status getStatus() throws GlassFishException;

    /**
     * A service has a service interface and optionally a name. For a service which is just a class with no interface,
     * then the service class is the service interface. This method is used to look up a service.
     * @param serviceType type of component required.
     * @param <T>
     * @return Return a service matching the requirement, null if no service found.
     */
    <T> T getService(Class<T> serviceType) throws GlassFishException;

    /**
     * A service has a service interface and optionally a name. For a service which is just a class with no interface,
     * then the service class is the service interface. This method is used to look up a service.
     * @param serviceType type of component required.
     * @param serviceName name of the component.
     * @param <T>
     * @return Return a service matching the requirement, null if no service found.
     */
    <T> T getService(Class<T> serviceType, String serviceName) throws GlassFishException;

    /**
     * Gets a Deployer instance to deploy an application.
     * Each invocation of this method returns a new Deployer object.
     * Calling this method is equivalent to calling <code>getService(Deployer.class, null)</code>
     *
     * @return A new Deployer instance
     */
    Deployer getDeployer() throws GlassFishException;

    /**
     * Gets a CommandRunner instance, using which the user can run asadmin commands.
     * Calling this method is equivalent to calling <code>getService(CommandRunner.class, null)</code>
     * Each invocation of this method returns a new CommandRunner object.
     *
     * @return a new CommandRunner instance
     */
    CommandRunner getCommandRunner() throws GlassFishException;

    /**
     * Represents the status of {@link org.glassfish.embeddable.GlassFish}.
     */
    enum Status {
        /**
         * Initial state of a newly created GlassFish.
         *
         * <p/>This will be the state just after {@link org.glassfish.embeddable.GlassFishRuntime#newGlassFish()}
         * before performing any lifecycle operations.
         */
        INIT,

        /**
         * GlassFish is being started.
         *
         * <p/>This will be the state after {@link org.glassfish.embeddable.GlassFish#start()} has been called
         * until the GlassFish is fully started.
         */
        STARTING,

        /**
         * GlassFish is up and running.
         *
         * <p/> This will be the state once {@link org.glassfish.embeddable.GlassFish#start()} has fully
         * started the GlassFish.
         */
        STARTED,

        /**
         * GlassFish is being stopped.
         *
         * <p/> This will be the state after {@link org.glassfish.embeddable.GlassFish#stop()} has been
         * called until the GlassFish is fully stopped.
         */
        STOPPING,

        /**
         * GlassFish is stopped.
         *
         * <p/>This will be the state after {@link org.glassfish.embeddable.GlassFish#stop()} has
         * fully stopped the GlassFish.
         */
        STOPPED,

        /**
         * GlassFish is disposed and ready to be garbage collected.
         *
         * <p/>This will be the state  after {@link org.glassfish.embeddable.GlassFish#dispose()} or
         * {@link org.glassfish.embeddable.GlassFishRuntime#shutdown()} has been called.
         */
        DISPOSED
    }
}
