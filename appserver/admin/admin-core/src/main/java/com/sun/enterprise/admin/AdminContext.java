/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin;

import java.util.logging.Logger;
import java.net.URL;

import javax.management.MBeanServer;
//import com.sun.enterprise.config.ConfigContext;
import com.sun.enterprise.admin.util.proxy.Interceptor;

/**
 * This interface defines the environment for administration.
 */
public interface AdminContext {

    /**
     * Get runtime config context. Runtime config context provides access to
     * the configuration that the server is running with. The server reads
     * the configuration from disk at startup time, the configuration is
     * then updated for every change that is applied dynamically.
     */
    //public ConfigContext getRuntimeConfigContext();

    /**
     * Set runtime config context. If server runtime handles a configuration
     * change dynamically, the context of the runtime is updated with the new
     * changes.
     * @param ctc the config context to use for runtime
     */
    //public void setRuntimeConfigContext(ConfigContext ctx);

    /**
     * Get admin config context. Admin config context provides access to the
     * configuration on the disk. This may be different from runtime context
     * if one or changes have not been applied dynamically to the runtime.
     */
    //public ConfigContext getAdminConfigContext();

    /**
     * Set admin config context. This is the context used for updating
     * configuration on the disk.
     * @param ctx the config context to use for administration
     */
    //public void setAdminConfigContext(ConfigContext ctx);
    
    /**
     * Get MBeanServer in use for admin, runtime and monitoring MBeans.
     */
    public MBeanServer getMBeanServer();

    /**
     * Set MBeanServer used for admin, runtime and monitoring MBeans.
     * @param mbs the management bean server
     */
    public void setMBeanServer(MBeanServer mbs);

    /**
     * Get domain name
     */
    public String getDomainName();

    /**
     * Set domain name.
     * @param domainName name of the domain
     */
    public void setDomainName(String domainName);

    /**
     * Get server name.
     */
    public String getServerName();

    /**
     * Set server name.
     * @param serverName name of the server
     */
    public void setServerName(String serverName);

    /**
     * Get Admin MBeanRegistry xml file location 
     */
    public URL getAdminMBeanRegistryURL();


    /**
     * Get Admin MBeanRegistry xml file location 
     * @param url  URL of the Registry file
     */
    public void setAdminMBeanRegistryURL(URL url);

    /**
     * Get Admin MBeanRegistry xml file location 
     */
    public URL getRuntimeMBeanRegistryURL();


    /**
     * Get Runtime MBeanRegistry xml file location 
     * @param url  URL of the Registry file
     */
    public void setRuntimeMBeanRegistryURL(URL url);

    /**
     * Get admin logger.
     */
    public Logger getAdminLogger();

    /**
     * Set admin logger.
     * @param logger the logger for admin module
     */
    public void setAdminLogger(Logger logger);

    /**
     * Get interceptor for mbean server used. In general, this method will
     * be used only while initializing MBeanServer to setup its interceptor.
     */
    public Interceptor getMBeanServerInterceptor();

    /**
     * Set interceptor. If set prior to creating an MBeanServer, the default
     * implementation of SunOneMBeanServer factory will apply the interceptor
     * to every MBeanServer call.
     */
    public void setMBeanServerInterceptor(Interceptor interceptor);

    /**
     * returns the appropriate dotted name mbean implementation class.
     */
    public String getDottedNameMBeanImplClassName();
}
