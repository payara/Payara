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

package org.glassfish.admin.amx.base;

import org.glassfish.admin.amx.core.AMXProxy;

import java.util.Map;
import java.util.List;
import javax.management.MBeanOperationInfo;
import org.glassfish.admin.amx.annotation.*;
import org.glassfish.admin.amx.core.AMXMBeanMetadata;
import org.glassfish.external.arc.Stability;
import org.glassfish.external.arc.Taxonomy;

/**
@since GlassFish V3
 */
@Taxonomy(stability = Stability.UNCOMMITTED)
@AMXMBeanMetadata(type="runtime",singleton=true, globalSingleton=true)
public interface RuntimeRoot extends AMXProxy, Utility, Singleton
{
    /** 
     * The key to store the module name in the deployment descriptor map.
     * @see getDeploymentConfigurations
     */
    public static final String MODULE_NAME_KEY = "module-name";
    /** 
     * The key to store the deployment descriptor path in the deployment 
     * descriptor map.
     * @see getDeploymentConfigurations
     */
    public static final String DD_PATH_KEY =  "dd-path";
    /** 
     * The key to store the deployment descriptor content in the deployment 
     * descriptor map.
     * @see getDeploymentConfigurations
     */
    public static final String DD_CONTENT_KEY = "dd-content";

    @ManagedOperation(impact = MBeanOperationInfo.ACTION)
    public void stopDomain();

    @ManagedOperation(impact = MBeanOperationInfo.ACTION)
    public void restartDomain();

    /** 
     * Return a list of deployment descriptor maps for the specified 
     * application.
     * In each map: 
     * a. The module name is stored by the MODULE_NAME_KEY. 
     * b. The path of the deployment descriptor is stored by the DD_PATH_KEY.
     * c. The content of the deployment descriptor is stored by the 
     *    DD_CONTENT_KEY.
     * @param the application name
     * @return the list of deployment descriptor maps
     *
     */
    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    public List<Map<String, String>> getDeploymentConfigurations(
            @Param(name = "applicationName") String applicationName);

    /**
     * Return the subcomponents (ejb/web) of a specified module.
     * @param applicationName the application name
     * @param moduleName the module name
     * @return a map of the sub components, where the key is the component
     *         name and the value is the component type
     */
    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    public Map<String, String> getSubComponentsOfModule(
            @Param(name = "applicationName") String applicationName,
            @Param(name = "moduleName") String moduleName);

    /**
     * Return the context root of a specified module.
     * @param applicationName the application name
     * @param moduleName the module name
     * @return the context root of a specified module 
     */
    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    public String getContextRoot(
            @Param(name = "applicationName") String applicationName,
            @Param(name = "moduleName") String moduleName);

    /**
    Execute a REST command.  Do not include a leading "/".
     */
    @ManagedOperation(impact = MBeanOperationInfo.ACTION)
    public String executeREST(@Param(name = "command") final String command);

    /**
    Return the base URL for use with {@link #executeREST}.  Example:
    http://localhost:4848/__asadmin/

    Example only, the host and port are typically different.  A trailing "/" is
    included; simply append the command string and call {@link #executeREST}.
     */
    @ManagedAttribute
    public String getRESTBaseURL();

    @ManagedAttribute
    public String[] getSupportedCipherSuites();

    @ManagedAttribute
    @Description("Return the available JMXServiceURLs in no particular order")
    public String[] getJMXServiceURLs();
    
    
    /** Which: all | summary | memory| class | thread  log */
    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    @Description("Return a summary report of the specified type")
    public String getJVMReport( @Param(name = "which")String which);
    
    @ManagedAttribute
    public Map<String,ServerRuntime>   getServerRuntime();
    
    @ManagedAttribute
    @Description("Whether the server was started with --debug")
    public boolean isStartedInDebugMode();
}










