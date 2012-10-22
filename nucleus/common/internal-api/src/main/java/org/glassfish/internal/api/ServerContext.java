/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.internal.api;

import org.jvnet.hk2.annotations.Contract;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.hk2.api.ServiceLocator;

import javax.naming.InitialContext;
import java.io.File;

/**
 * ServerContext interface: the server-wide runtime environment created by
 * ApplicationServer and shared by its subsystems such as the web container
 * or EJB container.
 */
@Contract
public interface ServerContext {

    /** 
     * Get the server command-line arguments
     *
     * @return  the server command-line arguments
     */
    public String[] getCmdLineArgs();

    /**
     * Get a factory for supported pluggable features. The server can support
     * many pluggable features in different editions. This factory allows access
     * to specialized implementation of features.
     */
    //public PluggableFeatureFactory getPluggableFeatureFactory();

    /** XXX: begin should move these to Config API */

    /**
     * Get server install root
     *
     * @return  the server install root
     */
    public File getInstallRoot();

    /**
     * Get the server instance name
     *
     * @return  the server instance name
     */
    public String getInstanceName();

    /**
     * Get a URL representation of server configuration
     *
     * @return    the URL to the server configuration
     */
    public String getServerConfigURL();

    /**
     * Get the server configuration bean.
     *
     * @return  the server config bean
     */
    public com.sun.enterprise.config.serverbeans.Server getConfigBean();

    /** 
     * Get the initial naming context.
     *
     * @return    the initial naming context
     */
    public InitialContext getInitialContext();

    /**
     * Get the classloader that loads .jars in $instance/lib and classes
     * in $instance/lib/classes.
     *
     * @return  the common class loader for this instance
     */
    public ClassLoader getCommonClassLoader();

    /**
     * Returns the shared class loader for this server instance.
     *
     * @return    the shared class loader 
     */
    public ClassLoader getSharedClassLoader();

    /**
     * Get the parent class loader for the life cycle modules.
     *
     * @return  the parent class loader for the life cycle modules
     */
    public ClassLoader getLifecycleParentClassLoader();

    /**
     * Returns the environment object for this instance.
     *
     *  @return    the environment object for this server instance
     */
    //public InstanceEnvironment getInstanceEnvironment();

    /**
     * get the J2EE Server invocation manager
     *
     * @return InvocationManager
     */
    public InvocationManager getInvocationManager();

    /**
     * get the default domain name
     *
     * @return String default domain name
     */
    public String getDefaultDomainName();


    /**
     * Returns the default habitat for this instance
     * @return defa ult habitat
     */
    public ServiceLocator getDefaultServices();

}
