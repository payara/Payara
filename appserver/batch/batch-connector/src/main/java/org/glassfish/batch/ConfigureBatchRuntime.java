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
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.batch.spi.impl.BatchRuntimeConfiguration;
import org.glassfish.batch.spi.impl.BatchRuntimeHelper;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import javax.inject.Inject;
import java.beans.PropertyVetoException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Mahesh Kannan
 *
 */
@Service(name = "configure-batch-runtime")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("configure.batch.runtime")
@ExecuteOn(value = {RuntimeType.DAS})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER})
@RestEndpoints({
        @RestEndpoint(configBean = Domain.class,
                opType = RestEndpoint.OpType.POST,
                path = "configure-batch-runtime",
                description = "Configure Batch Runtime")
})
public class ConfigureBatchRuntime
    implements AdminCommand {

    @Inject
    BatchRuntimeHelper helper;

    @Inject
    protected Logger logger;

    @Inject
    private Configs configs;

    @Param(name = "config", optional = true)
    protected String configName;

    @Param(name = "dataSourceLookupName", shortName = "d", optional = true)
    private String dataSourceLookupName;

    @Param(name = "executorServiceLookupName", shortName = "x", optional = true)
    private String executorServiceLookupName;


    @Override
    public void execute(AdminCommandContext context) {
        ActionReport actionReport = context.getActionReport();
        Properties extraProperties = actionReport.getExtraProperties();
        if (extraProperties == null) {
            extraProperties = new Properties();
            actionReport.setExtraProperties(extraProperties);
        }

        try {
            Config config = configs.getConfigByName(
                    configName == null ? "default-config" : configName);


            BatchRuntimeConfiguration batchRuntimeConfiguration = config.getExtensionByType(BatchRuntimeConfiguration.class);
            if (batchRuntimeConfiguration != null) {
                ConfigSupport.apply(new SingleConfigCode<BatchRuntimeConfiguration>() {
                    @Override
                    public Object run(final BatchRuntimeConfiguration batchRuntimeConfigurationProxy)
                            throws PropertyVetoException, TransactionFailure {
                        if (dataSourceLookupName != null)
                            batchRuntimeConfigurationProxy.setDataSourceLookupName(dataSourceLookupName);
                        if (executorServiceLookupName != null)
                            batchRuntimeConfigurationProxy.setExecutorServiceLookupName(executorServiceLookupName);
                        return null;
                    }
                }, batchRuntimeConfiguration);
            }

            actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        } catch (TransactionFailure txfEx) {
            logger.log(Level.WARNING, "Exception during command ", txfEx);
            actionReport.setMessage(txfEx.getMessage());
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
    }


}
