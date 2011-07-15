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

package com.sun.enterprise.jbi.serviceengine.component;

import javax.jbi.JBIException;
import javax.jbi.component.Bootstrap;
import javax.jbi.component.InstallationContext;

/**
 * Bootstrap class for installation purpose
 * @author Manisha Umbarje
 */
public class JavaEEServiceEngineBootstrap 
        implements Bootstrap {

    /**
     * This is the installation context given by the framework.
     */
    private InstallationContext context;

    
    
    /** Creates a new instance of JavaEEServiceEngineBootstrap */
    public JavaEEServiceEngineBootstrap() {
    }

    /**
     * Cleans up any resources allocated by the bootstrap implementation,
     * including deregistration of the extension MBean, if applicable.
     * This method will be called after the onInstall() or onUninstall() method
     * is called, whether it succeeds or fails.
     * @throws javax.jbi.JBIException when cleanup processing fails to complete
     * successfully.
     */
    public void cleanUp() throws JBIException {
    }

    /**
     * Get the JMX ObjectName for the optional installation configuration MBean
     * for this BC. If there is none, the value is null.
     *
     * @return ObjectName the JMX object name of the installation configuration
     *         MBean or null if there is no MBean.
     */
    public javax.management.ObjectName getExtensionMBeanName(){
        return null;
    }

    /**
     * Called to initialize the BC bootstrap.
     *
     * @param installContext is the context containing information from the
     *        install command and from the BC jar JavaEEServiceEngine.
     *
     * @throws javax.jbi.JBIException when there is an error requiring that the
     *         installation be terminated.
     */
    public void init(InstallationContext installContext)
        throws JBIException {
        context = installContext;
    }

    /**
     * Called at the beginning of installation of JavaEEServiceEngine Binding .
     * For this JavaEEServiceEngine, all the required installation tasks have
     * been taken care by the InstallationService.
     *
     * @throws javax.jbi.JBIException when there is an error requiring that the
     *         installation be terminated.
     */
    public void onInstall() throws JBIException {
    }

    /**
     * Called at the beginning of uninstallation of JavaEEServiceEngineBinding . 
     * For this JavaEEServiceEngine, all the required uninstallation tasks
     * have been taken care of by the InstallationService
     *
     * @throws javax.jbi.JBIException when there is an error requiring that the
     *         uninstallation be terminated.
     */
    public void onUninstall() throws JBIException {
    }
}
