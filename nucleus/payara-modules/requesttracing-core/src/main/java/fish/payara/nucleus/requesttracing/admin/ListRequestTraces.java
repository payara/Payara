/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2021 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.requesttracing.admin;

import com.sun.enterprise.util.ColumnFormatter;
import fish.payara.notification.requesttracing.RequestTrace;
import fish.payara.nucleus.requesttracing.RequestTracingService;
import fish.payara.nucleus.requesttracing.configuration.RequestTracingServiceConfiguration;
import fish.payara.nucleus.requesttracing.store.RequestTraceStoreInterface;
import fish.payara.nucleus.requesttracing.domain.execoptions.RequestTracingExecutionOptions;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;

import jakarta.inject.Inject;
import java.util.*;

/**
 * @author mertcaliskan
 */
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG, CommandTarget.DEPLOYMENT_GROUP})
@Service(name = "list-requesttraces")
@CommandLock(CommandLock.LockType.NONE)
@PerLookup
@I18n("requesttracing.configure")
@RestEndpoints({
        @RestEndpoint(configBean = RequestTracingServiceConfiguration.class,
                opType = RestEndpoint.OpType.GET,
                path = "list-requesttraces",
                description = "List slowest request traces stored.")
})
public class ListRequestTraces implements AdminCommand {

    private static final String headers[] = {"Occurring Time", "Elapsed Time", "Traced Message"};

    @Inject
    protected Target targetUtil;

    @Param(name = "target", optional = true, defaultValue = "server-config")
    protected String target;

    @Param(name = "first", optional = true)
    private Integer first;
    
    @Param(name = "historicTraces", optional = true, alias = "historictraces")
    private Boolean historicTraces;

    @Inject
    private RequestTracingService service;

    @Inject
    ServerEnvironment server;

    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport actionReport = context.getActionReport();

        RequestTracingExecutionOptions executionOptions = service.getExecutionOptions();
        if ((historicTraces == null || historicTraces) && !executionOptions.isHistoricTraceStoreEnabled()) {
            actionReport.setMessage("Historic Request Trace Store is not enabled!");
            actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        }
        else {
            if (server.isDas()) {
                if (targetUtil.getConfig(target).isDas()) {
                    generateReport(actionReport);
                }
            } else {
                // apply as not the DAS so implicitly it is for us
                generateReport(actionReport);
            }
        }
    }

    private void generateReport(ActionReport actionReport) {
        RequestTracingExecutionOptions executionOptions = service.getExecutionOptions();

        if (first == null) {
            first = executionOptions.getTraceStoreSize();
        }
        
        RequestTraceStoreInterface eventStore;
        
        if (historicTraces == null || historicTraces) {
            eventStore = service.getHistoricRequestTraceStore();
        } else {
            eventStore = service.getRequestTraceStore();
        }
        
        
        ColumnFormatter columnFormatter = new ColumnFormatter(headers);
        Properties extrasProps = new Properties();
        List<Map<String, String>> tracesList = new ArrayList<>();

        Collection<RequestTrace> traces = eventStore.getTraces(first);
        for (RequestTrace requestTrace : traces) {
            Map<String, String> messages = new LinkedHashMap<>();
            Object values[] = new Object[3];
            values[0] = requestTrace.getStartTime();
            values[1] = requestTrace.getElapsedTime();
            values[2] = requestTrace.toString();
            messages.put("occuringTime",values[0].toString());
            messages.put("elapsedTime",values[1].toString());
            messages.put("message", (String) values[2]);
            tracesList.add(messages);
            columnFormatter.addRow(values);
        }

        actionReport.setMessage(columnFormatter.toString());
        extrasProps.put("traces", tracesList);
        actionReport.setExtraProperties(extrasProps);

        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }
}
