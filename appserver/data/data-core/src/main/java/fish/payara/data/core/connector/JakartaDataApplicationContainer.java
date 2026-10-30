/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2025] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/main/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 *
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 *
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 *
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.data.core.connector;

import org.glassfish.api.deployment.ApplicationContainer;
import org.glassfish.api.deployment.ApplicationContext;
import org.glassfish.api.deployment.DeploymentContext;

/**
 * Application container used to create Deployer for Jakarta Data Annotation processing
 * @author Alfonso Valdez
 */
public class JakartaDataApplicationContainer implements ApplicationContainer<Object> {

    protected final DeploymentContext ctx;
    protected final ClassLoader appClassLoader;
    protected final String appName;

    public JakartaDataApplicationContainer(DeploymentContext ctx) {
        this.ctx = ctx;
        this.appClassLoader = ctx.getFinalClassLoader();
        this.appName = ctx.getArchiveHandler().getDefaultApplicationName(ctx.getSource(), ctx);
    }

    @Override
    public Object getDescriptor() {
        return ctx.getModuleMetaData(Object.class);
    }

    @Override
    public boolean start(ApplicationContext startupContext) throws Exception {
        return true;
    }

    @Override
    public boolean stop(ApplicationContext stopContext) {
        return true;
    }

    @Override
    public boolean suspend() {
        return true;
    }

    @Override
    public boolean resume() throws Exception {
        return true;
    }

    @Override
    public ClassLoader getClassLoader() {
        return this.appClassLoader;
    }
}
