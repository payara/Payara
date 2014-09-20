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

package com.sun.jdo.spi.persistence.support.ejb.ejbc;

import com.sun.enterprise.deployment.ResourceReferenceDescriptor;
import com.sun.jdo.spi.persistence.support.sqlstore.ejb.DeploymentHelper;
import com.sun.jdo.spi.persistence.utility.logging.Logger;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.ejb.deployment.descriptor.EjbBundleDescriptorImpl;
import org.glassfish.persistence.common.DatabaseConstants;
import org.glassfish.persistence.common.Java2DBProcessorHelper;

/**
 * If the application contains cmp 2.x beans process them. Check if
 * tables have to created or dropped depending on where we are called
 * in a deploy/undeploy case.
 * @author pramodg
 */
public class CMPProcessor {
    
    private static Logger logger = LogHelperEJBCompiler.getLogger();

    private Java2DBProcessorHelper helper = null;

    private DeploymentContext ctx;

    /**
     * Creates a new instance of CMPProcessor
     * @param ctx the deployment context object.
     */
    public CMPProcessor(DeploymentContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Create and execute the files.
     */
    public void process() {
      
        EjbBundleDescriptorImpl bundle = ctx.getModuleMetaData(EjbBundleDescriptorImpl.class);
        ResourceReferenceDescriptor cmpResource = bundle.getCMPResourceReference();

        // If this bundle's beans are not created by Java2DB, there is nothing to do.
        if (!DeploymentHelper.isJavaToDatabase(
                cmpResource.getSchemaGeneratorProperties())) {
            return;
        }

        helper = new Java2DBProcessorHelper(ctx);
        helper.init();

        String resourceName = cmpResource.getJndiName();
        helper.setProcessorType("CMP", bundle.getName()); // NOI18N
        helper.setJndiName(resourceName, bundle.getName());

        // If CLI options are not set, use value from the create-tables-at-deploy 
        // or drop-tables-at-undeploy elements of the sun-ejb-jar.xml
        boolean userCreateTables = cmpResource.isCreateTablesAtDeploy();
        boolean createTables = helper.getCreateTables(userCreateTables);

        boolean userDropTables = cmpResource.isDropTablesAtUndeploy();

        if (logger.isLoggable(logger.FINE)) {                
            logger.fine("ejb.CMPProcessor.createanddroptables", //NOI18N
                new Object[] {new Boolean(createTables), new Boolean(userDropTables)});
        }

        if (!createTables && !userDropTables) {
            // Nothing to do.
            return;
        }
    
        helper.setCreateTablesValue(userCreateTables, bundle.getName());
        helper.setDropTablesValue(userDropTables, bundle.getName());

        constructJdbcFileNames(bundle);
        if (logger.isLoggable(logger.FINE)) {
            logger.fine("ejb.CMPProcessor.createanddropfilenames", 
                helper.getCreateJdbcFileName(bundle.getName()), 
                helper.getDropJdbcFileName(bundle.getName())); 
        }            

        if (createTables) {
            helper.createOrDropTablesInDB(true, "CMP"); // NOI18N
        }
    }    

    /**
     * Drop files on undeploy
     */
    public void clean() {
        helper = new Java2DBProcessorHelper(ctx);
        helper.init();

        helper.createOrDropTablesInDB(false, "CMP"); // NOI18N
    }
      
    /**
     * Construct the name of the create and 
     * drop jdbc ddl files that would be 
     * created. These name would be either
     * obtained from the persistence.xml file
     * (if the user has defined them) or we would
     * create default filenames
     * @param ejbBundle the ejb bundle descriptor being worked on.
     */
    private void  constructJdbcFileNames(EjbBundleDescriptorImpl ejbBundle) {
        String filePrefix = DeploymentHelper.getDDLNamePrefix(ejbBundle);
    
        helper.setCreateJdbcFileName(filePrefix + DatabaseConstants.CREATE_DDL_JDBC_FILE_SUFFIX, 
                ejbBundle.getName());
        helper.setDropJdbcFileName(filePrefix + DatabaseConstants.DROP_DDL_JDBC_FILE_SUFFIX, 
                ejbBundle.getName());
    }
    
}
