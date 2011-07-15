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

package org.glassfish.api.deployment;

/**
 * Interface to an application container. mainly used to start and stop 
 * the application.
 *
 * @author Jerome Dochez
 */

public interface ApplicationContainer<T> {

    /**
     * Returns the deployment descriptor associated with this application
     * 
     * @return deployment descriptor if they exist or null if not
     */
    public T getDescriptor();
    
    /**
     * Starts an application container. 
     * ContractProvider starting should not throw an exception but rather should
     * use their prefered Logger instance to log any issue they encounter while 
     * starting. Returning false from a start mean that the container failed 
     * to start
     * @param startupContext the start up context 
     * @return true if the container startup was successful.
     *
     * @throws Exception if this application container could not be started
     */
    public boolean start(ApplicationContext startupContext) throws Exception;
    
    /**
     * Stop the application container
     * @return true if stopping was successful.
     * @param stopContext
     */
    public boolean stop(ApplicationContext stopContext);

    /**
     * Suspends this application container.
     *
     * @return true if suspending was successful, false otherwise.
     */
    public boolean suspend();

    /**
     * Resumes this application container.
     *
     * @return true if resumption was successful, false otherwise
     *
     * @throws Exception if this application container could not be
     * resumed
     */
    public boolean resume() throws Exception;

    /**
     * Returns the class loader associated with this application
     * @return ClassLoader for this app
     */
    public ClassLoader getClassLoader();
    
}
