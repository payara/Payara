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

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Map;

/**
 * Deployer service for GlassFish. Using this service, one can deploy and undeploy applications.
 * It accepts URI as an input for deployment and hence is very easily extensible. User can install their own
 * custom URL handler in Java runtime and create URIs with their custom scheme and pass them to deploy method.
 *
 * @see org.glassfish.embeddable.GlassFish#getDeployer()
 * @see java.net.URL#setURLStreamHandlerFactory(java.net.URLStreamHandlerFactory)
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public interface Deployer {
    /**
     * Deploys an application identified by a URI. URI is used as it is very extensible.
     * GlassFish does not care about what URI scheme is used as long as there is a URL handler installed
     * in the JVM to handle the scheme and a JarInputStream can be obtained from the given URI.
     * This method takes a var-arg argument for the deployment options. Any option that's applicable
     * to "asadmin deploy" command is also applicable here with same semantics. Please refer to GlassFish
     * deployment guide for all available options.
     *
     * <p/>Examples:
     * <pre>
     *
     *           deployer.deploy(new URI("http://acme.com/foo.war"));
     *
     *           deployer.deploy(new URI("http://acme.com/foo.war"),
     *                                    "--name", "bar", "--force", "true", "--create-tables", "true");
     * </pre>
     * @param archive URI identifying the application to be deployed.
     * @param params Optional list of deployment options.
     * @return the name of the deployed application
     */
    String deploy(URI archive, String... params) throws GlassFishException;

    /**
     * Deploys an application identified by a file. Invoking this method is equivalent to invoking
     * {@link #deploy(URI, String...) <tt>deploy(file.toURI, params)</tt>}.
     *
     * @param file File or directory identifying the application to be deployed.
     * @param params Optional list of deployment options.
     * @return the name of the deployed application
     */
    String deploy(File file, String... params) throws GlassFishException;

    /**
     * Deploys an application from the specified <code>InputStream</code> object.
     * The input stream is closed when this method completes, even if an exception is thrown.
     *
     * @param is InputStream used to read the content of the application.
     * @param params Optional list of deployment options.
     * @return the name of the deployed application
     */
    String deploy(InputStream is, String... params) throws GlassFishException;

    /**
     * Undeploys an application from {@link GlassFish}
     * This method takes a var-arg argument for the undeployment options. Any option that's applicable
     * to "asadmin undeploy" command is also applicable here with same semantics. Please refer to GlassFish
     * deployment guide for all available options.
     *
     * <p/>Example:
     * <pre>
     *          deployer.undeploy("foo", "--drop-tables", "true");
     * </pre>
     *
     * @param appName Identifier of the application to be undeployed.
     * @param params Undeployment options.
     */
    void undeploy(String appName, String... params) throws GlassFishException;

    /**
     * Return names of all the deployed applications.
     * @return names of deployed applications.
     */
    Collection<String> getDeployedApplications() throws GlassFishException;
}
