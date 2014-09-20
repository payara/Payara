/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.batch;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Configs;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.ColumnFormatter;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.batch.spi.impl.BatchRuntimeConfiguration;
import org.glassfish.batch.spi.impl.BatchRuntimeHelper;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.*;

/**
 * Command to list batch jobs info
 *
 * @author Mahesh Kannan
 *
 */
@Service(name="list-batch-runtime-configuration")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("list.batch.runtime.configuration")
@ExecuteOn(value = {RuntimeType.DAS})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER})
@RestEndpoints({
        @RestEndpoint(configBean = Domain.class,
                opType = RestEndpoint.OpType.GET,
                path = "list-batch-runtime-configuration",
                description = "List Batch Runtime Configuration")
})
public class ListBatchRuntimeConfiguration
    extends AbstractListCommand {

    private static final String DATA_SOURCE_NAME = "dataSourceLookupName";

    private static final String EXECUTOR_SERVICE_NAME = "executorServiceLookupName";

    @Inject
    protected Target targetUtil;

    @Inject
    BatchRuntimeHelper helper;

    @Override
    protected void executeCommand(AdminCommandContext context, Properties extraProps) {

        Config config = targetUtil.getConfig(target);
        if (config == null) {
            context.getActionReport().setMessage("No such config named: " + target);
            context.getActionReport().setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        BatchRuntimeConfiguration batchRuntimeConfiguration = config.getExtensionByType(BatchRuntimeConfiguration.class);

        Map<String, Object> map = new HashMap<>();

        map.put(DATA_SOURCE_NAME, batchRuntimeConfiguration.getDataSourceLookupName());
        map.put(EXECUTOR_SERVICE_NAME, batchRuntimeConfiguration.getExecutorServiceLookupName());
        extraProps.put("listBatchRuntimeConfiguration", map);

        ColumnFormatter columnFormatter = new ColumnFormatter(getDisplayHeaders());
        Object[] data = new Object[getOutputHeaders().length];
        for (int index=0; index<getOutputHeaders().length; index++) {
            switch (getOutputHeaders()[index]) {
                case DATA_SOURCE_NAME:
                    String val = batchRuntimeConfiguration.getDataSourceLookupName();
                    data[index] = (val == null || val.trim().length() == 0)
                        ? BatchRuntimeHelper.getDefaultDataSourceLookupNameForTarget(target) : val;
                    break;
                case EXECUTOR_SERVICE_NAME:
                    data[index] = batchRuntimeConfiguration.getExecutorServiceLookupName();
                    break;
                default:
                    throw new IllegalArgumentException("Unknown header: " + getOutputHeaders()[index]);
            }
        }
        columnFormatter.addRow(data);
        context.getActionReport().setMessage(columnFormatter.toString());
    }


    @Override
    protected final String[] getAllHeaders() {
        return new String[] {
                DATA_SOURCE_NAME, EXECUTOR_SERVICE_NAME
        };
    }

    @Override
    protected final String[] getDefaultHeaders() {
        return getAllHeaders();
    }
}
