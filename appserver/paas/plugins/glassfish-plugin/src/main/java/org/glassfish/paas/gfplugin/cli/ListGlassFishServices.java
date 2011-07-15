/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.paas.gfplugin.cli;


import com.sun.enterprise.admin.util.ColumnFormatter;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.paas.orchestrator.provisioning.CloudRegistryService;
import org.glassfish.paas.orchestrator.provisioning.cli.ServiceUtil;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.*;


/**
 * @author Jagadish Ramu
 */
@Service(name = "list-glassfish-services")
@Scoped(PerLookup.class)
public class ListGlassFishServices implements AdminCommand {

    @Inject
    private CloudRegistryService registryService;

    @Inject
    private ServiceUtil serviceUtil;

    @Param(name = "servicename", defaultValue = "*", optional = true, primary = true)
    private String serviceName;

    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            InitialContext ic = new InitialContext();
            DataSource ds = (DataSource) ic.lookup(CloudRegistryService.RESOURCE_NAME);

            String query = null;
            if (serviceName.equals("*")) {
                query = "select * from " + CloudRegistryService.CLOUD_TABLE_NAME;
            } else if (serviceName.endsWith("*")) {
                String wildCardString = serviceName.substring(0, serviceName.lastIndexOf("*"));
                query = "select * from " + CloudRegistryService.CLOUD_TABLE_NAME + " where CLOUD_NAME like '" + wildCardString + "%'";
            } else if (serviceName != null) {
                query = "select * from " + CloudRegistryService.CLOUD_TABLE_NAME + " where CLOUD_NAME = '" + serviceName + "' or CLOUD_NAME like '" + serviceName + ".%'";
            }

            if (query != null) {
                con = ds.getConnection();
                stmt = prepareStatement(con, query);
                rs = stmt.executeQuery();

                String headings[] = {"CLOUD_NAME", "IP_ADDRESS", "INSTANCE_ID", "SERVER_TYPE", "STATE"};
                ColumnFormatter cf = new ColumnFormatter(headings);

                boolean foundRows = false;
                while (rs.next()) {
                    foundRows = true;
                    String cloudName = rs.getString("CLOUD_NAME");
                    String ipAddress = rs.getString("IP_ADDRESS");
                    String instanceID = rs.getString("INSTANCE_ID");
                    String serverType = rs.getString("SERVER_TYPE");
                    String state = rs.getString("STATE");

                    cf.addRow(new Object[]{cloudName, ipAddress, instanceID, serverType, state});
                }
                if (foundRows) {
                    report.setMessage(cf.toString());
                } else {
                    report.setMessage("Nothing to list.");
                }
            } else {
                report.setMessage("Nothing to list.");
            }

            ActionReport.ExitCode ec = ActionReport.ExitCode.SUCCESS;
            report.setActionExitCode(ec);
        } catch (NamingException e) {
            report.setMessage("Failed to list GlassFish services");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
        } catch (SQLException e) {
            report.setMessage("Failed to list GlassFish services");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
        } catch (Exception e) {
            report.setMessage("Failed to list GlassFish services");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
        } finally {
            serviceUtil.closeDBObjects(con, stmt, rs);
        }
    }

    private PreparedStatement prepareStatement(Connection con, final String query)
            throws SQLException {
        return con.prepareStatement(query);
    }

}
