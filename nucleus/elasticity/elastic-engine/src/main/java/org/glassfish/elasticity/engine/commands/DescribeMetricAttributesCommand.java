/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.elasticity.engine.commands;

import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.elasticity.metric.MetricAttribute;
import org.glassfish.elasticity.metric.MetricNode;
import org.glassfish.elasticity.metric.TabularMetricAttribute;
import org.glassfish.elasticity.util.TabularMetricHolder;
import javax.inject.Inject;

import org.glassfish.hk2.Services;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.glassfish.api.Param;
import org.jvnet.hk2.component.PerLookup;

import java.util.logging.Logger;
/**
 * Created by IntelliJ IDEA.
 * User: cmott
 * Date: 9/27/11
 * */
 @Service(name="describe-metric-attributes")
@I18n("describe.metric.attributes")
@Scoped(PerLookup.class)
public class DescribeMetricAttributesCommand implements AdminCommand{

    @Inject
    org.glassfish.elasticity.api.MetricGatherer[] metricGatherers;

    @Inject
    Services services;

    @Param(name="service")
    String servicename;

    @Param(name="metricGatherer", primary = true)
    String metricGatherer;

    private static final String EOL = "\n";

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        Logger logger= context.logger;

        // Look for the Metric Gatherer services and get the attribute for the
        StringBuilder sb = new StringBuilder();
        boolean firstName =true;

        MetricNode metricNode = services.forContract(MetricNode.class).named(metricGatherer).get();

        MetricAttribute[] metricAttribute=metricNode.getAttributes();
        for(int i=0;i < metricAttribute.length; i++ ) {
                    if ( firstName)
                     firstName = false;
                    else
                        sb.append(EOL);
                     sb.append(metricAttribute[i].getName());

            // if this attribute is an instance of TabularMetricAttribute then it has attributes so need to get those too
            if ( metricAttribute[i] instanceof TabularMetricAttribute ) {
                boolean firstAttribute=true;
                String[] columnNames = ((TabularMetricHolder)metricAttribute[i]).getColumnNames();
                for (int k=0; k<columnNames.length;k++){
                    if (firstAttribute)  {
                        sb.append(" (");
                        firstAttribute = false;
                    }
                    sb.append(columnNames[k]+ ", ");

                }
                int lastComa=sb.lastIndexOf(",");
                sb.deleteCharAt(lastComa);
                sb.append(")"+EOL);
            }

        }

        report.setMessage(sb.toString());
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);

        }

}
