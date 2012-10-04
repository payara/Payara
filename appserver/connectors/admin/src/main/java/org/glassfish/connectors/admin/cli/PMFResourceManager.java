/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.connectors.admin.cli;

import com.sun.enterprise.config.serverbeans.Resource;
import com.sun.enterprise.config.serverbeans.Resources;
import com.sun.enterprise.config.serverbeans.ServerTags;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.api.I18n;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.resources.admin.cli.ResourceConstants;
import org.glassfish.resources.admin.cli.ResourceManager;
import org.glassfish.resourcebase.resources.api.ResourceStatus;
import org.jvnet.hk2.annotations.Service;

import javax.resource.ResourceException;
import java.util.HashMap;
import java.util.Properties;

/**
 * PMF Resource is removed from v3. <BR>
 * Keeping a ResourceManager so as to provide warning message when
 * older versions of sun-resources.xml is used.<BR>
 */
@Service(name= ServerTags.PERSISTENCE_MANAGER_FACTORY_RESOURCE)
@PerLookup
@I18n("create.pmf.resource")
public class PMFResourceManager implements ResourceManager {

    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(PMFResourceManager.class);

    /**
     * @inheritDoc
     */
    public ResourceStatus create(Resources resources, HashMap attributes, Properties properties, String target)
            throws Exception {
        return new ResourceStatus(ResourceStatus.WARNING, getWarningMessage(attributes));
    }

    private String getWarningMessage(HashMap attributes) {
        //we do not support pmf-resource any more.
        String jndiName = (String) attributes.get(ResourceConstants.JNDI_NAME);
        String jdbcResourceJndiName= (String) attributes.get(ResourceConstants.JDBC_RESOURCE_JNDI_NAME);
        String defaultMsg = "persistence-manager-factory-resource is not supported any more. Instead, " +
                "use the jdbc-resource [ {0} ] referred by the persistence-manager-factory-resource [ {1} ] " +
                "in the application(s).";
        Object params[] = new Object[]{jdbcResourceJndiName, jndiName};
        return localStrings.getLocalString("create.pmf.resource.not.supported", defaultMsg, params);
    }

    /**
     * @inheritDoc
     */
    public Resource createConfigBean(Resources resources, HashMap attributes, Properties properties, boolean validate)
            throws Exception {
        throw new ResourceException(getWarningMessage(attributes));
    }

    /**
     * @inheritDoc
     */
    public String getResourceType() {
        return ServerTags.PERSISTENCE_MANAGER_FACTORY_RESOURCE;
    }
}
