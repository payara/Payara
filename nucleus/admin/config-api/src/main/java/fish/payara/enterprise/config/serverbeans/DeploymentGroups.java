/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
 */
package fish.payara.enterprise.config.serverbeans;

import java.util.List;
import org.glassfish.api.I18n;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.Create;
import org.glassfish.config.support.Delete;
import org.glassfish.config.support.TypeAndNameResolver;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.DuckTyped;
import org.jvnet.hk2.config.Element;

/**
 * Config Bean for Deployment Groups.
 * This element is at the domain level and contains a DeploymentGroup bean underneath
 * @author Steve Millidge (Payara Foundation)
 */
@Configured
public interface DeploymentGroups extends ConfigBeanProxy {
    
    /**
     * Returns the list of deployment groups
     * @return 
     */
    @Element
    @Create(value="create-deployment-group", cluster = @org.glassfish.api.admin.ExecuteOn(value = {RuntimeType.ALL}), i18n=@I18n("create.deploymentgroup.command") )
    @Delete(value="delete-deployment-group", resolver= TypeAndNameResolver.class, i18n=@I18n("delete.deploymentgroup.command"), cluster = @org.glassfish.api.admin.ExecuteOn(value = {RuntimeType.ALL}))
    public List<DeploymentGroup> getDeploymentGroup();
    
    /**
     * Return the deployment group with the specified name
     * @param name The name of the deployment group to return
     * @return The deployment group
     */
    @DuckTyped
    public DeploymentGroup getDeploymentGroup(String name);

    class Duck {
        public static DeploymentGroup getDeploymentGroup(DeploymentGroups dgs, String name) {
            for (DeploymentGroup dg : dgs.getDeploymentGroup()) {
                if (dg.getName().equals(name)) {
                    return dg;
                }
            }
            return null;
        }
    }
    
    
}
