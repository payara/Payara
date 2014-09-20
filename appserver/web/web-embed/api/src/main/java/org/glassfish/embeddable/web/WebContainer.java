/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.embeddable.web;

import java.io.File;
import java.util.Collection;
import java.util.logging.Level;

import org.glassfish.embeddable.web.config.WebContainerConfig;
import org.glassfish.embeddable.GlassFishException;

/**
 * Class representing an embedded web container, which supports the
 * programmatic creation of different types of web protocol listeners
 * and virtual servers, and the registration of static and dynamic
 * web resources into the URI namespace.
 * 
 * WebContainer service can be accessed using GlassFish instance.
 *
 * <p/> Usage example:
 *
 * <pre>
 *      // Create and start GlassFish
 *      GlassFish glassfish = GlassFishRuntime.bootstrap().newGlassFish();
 *      glassfish.start();
 *
 *      // Access WebContainer
 *      WebContainer container = glassfish.getService(WebContainer.class);
 *
 *      // Create and add {@link WebListener}
 *      // By default, when GlassFish Embedded Server starts, no web listener is enabled
 *      WebListener listener = container.createWebListener("listener-1", HttpListener.class);
 *      listener.setPort(8080);
 *      container.addWebListener(listener);
 *
 *      // Create and register web resources {@link Context}.
 *      File docroot = new File(path_to_web_resources);
 *      Context context = container.createContext(docroot);
 *      container.addContext(context, "contextroot_to_register");
 *
 *      // Create and add {@link VirtualServer}
 *      // By default, when GlassFish Embedded Server starts,
 *      // a virtual server named server starts automatically.
 *      VirtualServer virtualServer = (VirtualServer)
 *          container.createVirtualServer("embedded-server", new File(docroot_of_VirtualServer));
 *      VirtualServerConfig config = new VirtualServerConfig();
 *      config.setHostNames("localhost");
 *      virtualServer.setConfig(config);
 *      container.addVirtualServer(virtualServer);
 * </pre>
 */

public interface WebContainer {

    /**
     * Sets the embedded configuration for this embedded instance.
     * Such configuration will always override any xml based
     * configuration.
     *
     * @param config the embedded instance configuration
     */
    public void setConfiguration(WebContainerConfig config);
    
    /**
     * Creates a <tt>Context</tt> and configures it with the given
     * docroot and classloader.
     *
     * <p>The classloader of the class on which this method is called
     * will be used.
     *
     * <p>In order to access the new <tt>Context</tt> or any of its
     * resources, the <tt>Context</tt> must be registered with a
     * <tt>VirtualServer</tt> that has been started using either
     * WebContainer#addContext or VirtualServer#addContext method.
     *
     * @param docRoot the docroot of the <tt>Context</tt>
     *
     * @return the new <tt>Context</tt>
     *
     * @see VirtualServer#addContext
     */
    public Context createContext(File docRoot);

    /**
     * Creates a <tt>Context</tt> and configures it with the given
     * docroot and classloader.
     *
     * <p>The given classloader will be set as the thread's context
     * classloader whenever the new <tt>Context</tt> or any of its
     * resources are asked to process a request.
     * If a <tt>null</tt> classloader is passed, the classloader of the
     * class on which this method is called will be used.
     *
     * <p>In order to access the new <tt>Context</tt> or any of its
     * resources, the <tt>Context</tt> must be registered with a
     * <tt>VirtualServer</tt> that has been started using either
     * WebContainer#addContext or VirtualServer#addContext method.
     *
     * @param docRoot the docroot of the <tt>Context</tt>
     * @param classLoader the classloader of the <tt>Context</tt>
     *
     * @return the new <tt>Context</tt>
     *
     * @see VirtualServer#addContext
     */
    public Context createContext(File docRoot, ClassLoader classLoader);

    /**
     * Creates a <tt>Context</tt>, configures it with the given
     * docroot and classloader, and registers it with all
     * <tt>VirtualServer</tt>.
     *
     * <p>The given classloader will be set as the thread's context
     * classloader whenever the new <tt>Context</tt> or any of its
     * resources are asked to process a request.
     * If a <tt>null</tt> classloader is passed, the classloader of the
     * class on which this method is called will be used.
     *
     * @param docRoot the docroot of the <tt>Context</tt>
     * @param contextRoot the contextroot at which to register
     * @param classLoader the classloader of the <tt>Context</tt>
     *
     * @return the new <tt>Context</tt>
     */
    public Context createContext(File docRoot, String contextRoot,
                                 ClassLoader classLoader);

    /**
     * Registers the given <tt>Context</tt> with all <tt>VirtualServer</tt>
     * at the given context root.
     *
     * <p>If <tt>VirtualServer</tt> has already been started, the
     * given <tt>context</tt> will be started as well.
     *
     * @param context the <tt>Context</tt> to register
     * @param contextRoot the context root at which to register
     *
     * @throws ConfigException if a <tt>Context</tt> already exists
     * at the given context root on <tt>VirtualServer</tt>
     * @throws GlassFishException if the given <tt>context</tt> fails
     * to be started
     */
    public void addContext(Context context, String contextRoot)
        throws ConfigException, GlassFishException;

    /**
     * Stops the given <tt>Context</tt> and removes it from all
     * <tt>VirtualServer</tt>.
     *
     * @param context the <tt>Context</tt> to be stopped and removed
     *
     * @throws GlassFishException if an error occurs during the stopping
     * or removal of the given <tt>context</tt>
     */
    public void removeContext(Context context)
        throws ConfigException, GlassFishException;

    /**
     * Creates a <tt>WebListener</tt> from the given class type and
     * assigns the given id to it.
     *
     * @param id the id of the new <tt>WebListener</tt>
     * @param c the class from which to instantiate the
     * <tt>WebListener</tt>
     * 
     * @return the new <tt>WebListener</tt> instance
     *
     * @throws  IllegalAccessException if the given <tt>Class</tt> or
     * its nullary constructor is not accessible.
     * @throws  InstantiationException if the given <tt>Class</tt>
     * represents an abstract class, an interface, an array class,
     * a primitive type, or void; or if the class has no nullary
     * constructor; or if the instantiation fails for some other reason.
     * @throws ExceptionInInitializerError if the initialization
     * fails
     * @throws SecurityException if a security manager, <i>s</i>, is
     * present and any of the following conditions is met:
     *
     * <ul>
     * <li> invocation of <tt>{@link SecurityManager#checkMemberAccess
     * s.checkMemberAccess(this, Member.PUBLIC)}</tt> denies
     * creation of new instances of the given <tt>Class</tt>
     * <li> the caller's class loader is not the same as or an
     * ancestor of the class loader for the current class and
     * invocation of <tt>{@link SecurityManager#checkPackageAccess
     * s.checkPackageAccess()}</tt> denies access to the package
     * of this class
     * </ul>
     */
    public <T extends WebListener> T createWebListener(String id, Class<T> c)
        throws InstantiationException, IllegalAccessException;

    /**
     * Adds the given <tt>WebListener</tt> to this
     * <tt>WebContainer</tt>.
     *
     * <p>If this <tt>WebContainer</tt> has already been started,
     * the given <tt>webListener</tt> will be started as well.
     *
     * @param webListener the <tt>WebListener</tt> to add
     *
     * @throws ConfigException if a <tt>WebListener</tt> with the
     * same id has already been registered with this
     * <tt>WebContainer</tt>
     * @throws GlassFishException if the given <tt>webListener</tt> fails
     * to be started
     */
    public void addWebListener(WebListener webListener)
        throws ConfigException, GlassFishException;

    /**
     * Finds the <tt>WebListener</tt> with the given id.
     *
     * @param id the id of the <tt>WebListener</tt> to find
     *
     * @return the <tt>WebListener</tt> with the given id, or
     * <tt>null</tt> if no <tt>WebListener</tt> with that id has been
     * registered with this <tt>WebContainer</tt>
     */
    public WebListener getWebListener(String id);

    /**
     * Gets the collection of <tt>WebListener</tt> instances registered
     * with this <tt>WebContainer</tt>.
     * 
     * @return the (possibly empty) collection of <tt>WebListener</tt>
     * instances registered with this <tt>WebContainer</tt>
     */
    public Collection<WebListener> getWebListeners();

    /**
     * Stops the given <tt>webListener</tt> and removes it from this
     * <tt>WebContainer</tt>.
     *
     * @param webListener the <tt>WebListener</tt> to be stopped
     * and removed
     *
     * @throws GlassFishException if an error occurs during the stopping
     * or removal of the given <tt>webListener</tt>
     */
    public void removeWebListener(WebListener webListener)
        throws GlassFishException;

    /**
     * Creates a <tt>VirtualServer</tt> with the given id and docroot, and
     * maps it to the given <tt>WebListener</tt> instances.
     * 
     * @param id the id of the <tt>VirtualServer</tt>
     * @param docRoot the docroot of the <tt>VirtualServer</tt>
     * @param webListeners the list of <tt>WebListener</tt> instances from 
     * which the <tt>VirtualServer</tt> will receive requests
     * 
     * @return the new <tt>VirtualServer</tt> instance
     */
    public VirtualServer createVirtualServer(String id,
        File docRoot, WebListener...  webListeners);
    
    /**
     * Creates a <tt>VirtualServer</tt> with the given id and docroot, and
     * maps it to all <tt>WebListener</tt> instances.
     * 
     * @param id the id of the <tt>VirtualServer</tt>
     * @param docRoot the docroot of the <tt>VirtualServer</tt>
     * 
     * @return the new <tt>VirtualServer</tt> instance
     */    
    public VirtualServer createVirtualServer(String id, File docRoot);
    
    /**
     * Adds the given <tt>VirtualServer</tt> to this
     * <tt>WebContainer</tt>.
     *
     * <p>If this <tt>WebContainer</tt> has already been started,
     * the given <tt>virtualServer</tt> will be started as well.
     *
     * @param virtualServer the <tt>VirtualServer</tt> to add
     *
     * @throws ConfigException if a <tt>VirtualServer</tt> with the
     * same id has already been registered with this
     * <tt>WebContainer</tt>
     * @throws GlassFishException if the given <tt>virtualServer</tt> fails
     * to be started
     */
    public void addVirtualServer(VirtualServer virtualServer)
        throws ConfigException, GlassFishException;

    /**
     * Finds the <tt>VirtualServer</tt> with the given id.
     *
     * @param id the id of the <tt>VirtualServer</tt> to find
     *
     * @return the <tt>VirtualServer</tt> with the given id, or
     * <tt>null</tt> if no <tt>VirtualServer</tt> with that id has been
     * registered with this <tt>WebContainer</tt>
     */
    public VirtualServer getVirtualServer(String id);

    /**
     * Gets the collection of <tt>VirtualServer</tt> instances registered
     * with this <tt>WebContainer</tt>.
     * 
     * @return the (possibly empty) collection of <tt>VirtualServer</tt>
     * instances registered with this <tt>WebContainer</tt>
     */
    public Collection<VirtualServer> getVirtualServers();

    /**
     * Stops the given <tt>virtualServer</tt> and removes it from this
     * <tt>WebContainer</tt>.
     *
     * @param virtualServer the <tt>VirtualServer</tt> to be stopped
     * and removed
     *
     * @throws GlassFishException if an error occurs during the stopping
     * or removal of the given <tt>virtualServer</tt>
     */
    public void removeVirtualServer(VirtualServer virtualServer)
        throws GlassFishException;
    
    /**
     * Sets log level
     * 
     * @param level log level
     */
    public void setLogLevel(Level level);
       
}
