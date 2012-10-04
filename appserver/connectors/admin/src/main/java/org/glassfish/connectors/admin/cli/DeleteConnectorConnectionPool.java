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

package org.glassfish.connectors.admin.cli;

import com.sun.appserv.connectors.internal.api.ConnectorsUtil;
import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.connectors.config.ConnectorConnectionPool;
import org.glassfish.hk2.api.IterableProvider;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.resourcebase.resources.api.ResourceStatus;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import javax.inject.Inject;
import java.beans.PropertyVetoException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Delete Connector Connection Pool Command
 * 
 */
@ExecuteOn(RuntimeType.ALL)
@Service(name="delete-connector-connection-pool")
@PerLookup
@I18n("delete.connector.connection.pool")
public class DeleteConnectorConnectionPool implements AdminCommand {
    
    final private static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(DeleteConnectorConnectionPool.class);

    private @Param(optional=true, obsolete = true)
    String target = SystemPropertyConstants.DAS_SERVER_NAME;

    @Param(optional=true, defaultValue="false")
    private Boolean cascade;
    
    @Param(name="poolname", primary=true)
    private String poolname;
    
    @Inject
    private Domain domain;
    
    @Inject
    private IterableProvider<Server> servers;

    @Inject
    private IterableProvider<Cluster> clusters;

    /**
     * Executes the command with the command parameters passed as Properties
     * where the keys are the paramter names and the values the parameter values
     *
     * @param context information
     */
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        
        if (poolname == null) {
            report.setMessage(localStrings.getLocalString("delete.connector.connection.pool.noJndiName",
                            "No id defined for connector connection pool."));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        // ensure we already have this resource
        if(ConnectorsUtil.getResourceByName(domain.getResources(), ConnectorConnectionPool.class, poolname) == null){
            report.setMessage(localStrings.getLocalString("delete.connector.connection.pool.notfound",
                    "A connector connection pool named {0} does not exist.", poolname));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        try {

            // if cascade=true delete all the resources associated with this pool
            // if cascade=false don't delete this connection pool if a resource is referencing it
            Object obj = deleteAssociatedResources(servers, clusters, domain.getResources(),
                    cascade, poolname);
            if (obj instanceof Integer &&
                    (Integer) obj == ResourceStatus.FAILURE) {
                report.setMessage(localStrings.getLocalString(
                    "delete.connector.connection.pool.pool_in_use",
                    "Some connector resources are referencing connection pool {0}. Use 'cascade' " +
                            "option to delete them as well.", poolname));
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }

            // delete connector connection pool
            if (ConfigSupport.apply(new SingleConfigCode<Resources>() {
                public Object run(Resources param) throws PropertyVetoException, TransactionFailure {
                    ConnectorConnectionPool cp = (ConnectorConnectionPool)
                            ConnectorsUtil.getResourceByName(domain.getResources(),ConnectorConnectionPool.class, poolname);
                    if(cp != null){
                        return param.getResources().remove(cp);
                    }
                    // not found
                    return null;
                }
            }, domain.getResources()) == null) {
                report.setMessage(localStrings.getLocalString("delete.connector.connection.pool.notfound",
                                "A connector connection pool named {0} does not exist.", poolname));
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }

        } catch(TransactionFailure tfe) {
            Logger.getLogger(DeleteConnectorConnectionPool.class.getName()).log(Level.SEVERE,
                    "Something went wrong in delete-connector-connection-pool", tfe);
            report.setMessage(tfe.getMessage() != null ? tfe.getLocalizedMessage() :
                localStrings.getLocalString("delete.connector.connection.pool.fail",
                            "Connector connection pool {0} delete failed ", poolname));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(tfe);
        }

        report.setMessage(localStrings.getLocalString("delete.connector.connection.pool.success",
                "Connector connection pool {0} deleted successfully", poolname));
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }

    //TODO duplicate code in JDBCConnectionPoolManager
    private Object deleteAssociatedResources(final Iterable<Server> servers, final Iterable<Cluster> clusters, Resources resources,
                                           final boolean cascade, final String poolName) throws TransactionFailure {
        if (cascade) {
            ConfigSupport.apply(new SingleConfigCode<Resources>() {
                public Object run(Resources param) throws PropertyVetoException, TransactionFailure {
                    Collection<BindableResource> referringResources = ConnectorsUtil.getResourcesOfPool(param, poolName);
                    for (BindableResource referringResource : referringResources) {
                        // delete resource-refs
                        deleteServerResourceRefs(servers, referringResource.getJndiName());
                        deleteClusterResourceRefs(clusters, referringResource.getJndiName());

                        // remove the resource
                        param.getResources().remove(referringResource);
                    }
                    return true; //no-op
                }
            }, resources);
        }else{
            Collection<BindableResource> referringResources = ConnectorsUtil.getResourcesOfPool(resources, poolName);
            if(referringResources.size() > 0){
                return ResourceStatus.FAILURE;
            }
        }
        return true; //no-op
    }

    //TODO duplicate code in JDBCConnectionPoolManager
    private void deleteServerResourceRefs(Iterable<Server> servers, final String refName)
            throws TransactionFailure {
        if(servers != null){
            for (Server server : servers) {
                server.deleteResourceRef(refName);
            }
        }
    }

    private void deleteClusterResourceRefs(Iterable<Cluster>clusters, final String refName)
            throws TransactionFailure {
        if(clusters != null){
            for (Cluster cluster : clusters) {
                cluster.deleteResourceRef(refName);
            }
        }
    }

}
