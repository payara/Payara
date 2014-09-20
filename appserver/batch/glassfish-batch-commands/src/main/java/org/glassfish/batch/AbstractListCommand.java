/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.batch.spi.impl.BatchRuntimeHelper;
import org.glassfish.batch.spi.impl.GlassFishBatchSecurityHelper;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.inject.Inject;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Mahesh Kannan
 *
 */
public abstract class AbstractListCommand
    implements AdminCommand {

    @Inject
    BatchRuntimeHelper helper;

    @Inject
    protected Logger logger;

    @Param(name = "terse", optional=true, defaultValue="false", shortName="t")
    public boolean isTerse = false;

    @Param(name = "output", shortName = "o", optional = true)
    protected String outputHeaderList;

    @Param(name = "header", shortName = "h", optional = true)
    protected boolean header;

    @Param(name = "target", optional = true, defaultValue = "server")
    protected String target;

    protected String[] outputHeaders;

    protected String[] displayHeaders;

    @Inject
    GlassFishBatchSecurityHelper glassFishBatchSecurityHelper;

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport actionReport = context.getActionReport();
        Properties extraProperties = actionReport.getExtraProperties();
        if (extraProperties == null) {
            extraProperties = new Properties();
            actionReport.setExtraProperties(extraProperties);
        }

        try {
            calculateHeaders();
            helper.checkAndInitializeBatchRuntime();
            glassFishBatchSecurityHelper.markInvocationPrivilege(true);
            executeCommand(context, extraProperties);
            actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        } catch (Exception ex) {
            logger.log(Level.FINE, "Exception during command ", ex);
            actionReport.setMessage(ex.getMessage());
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
        } finally {
            glassFishBatchSecurityHelper.markInvocationPrivilege(false);
        }
    }

    private void calculateHeaders() {
        String[] headers = getDefaultHeaders();
        if (outputHeaderList != null) {
            headers = outputHeaderList.split("[,]");
            if (headers.length == 0)
                headers = getDefaultHeaders();
        } else if (supportsLongFormat())
            headers = getAllHeaders();

        Map<String, String> validHeaders = new HashMap<>();
        for (String h : getAllHeaders())
            validHeaders.put(h.toLowerCase(Locale.US), h);
        for (int i=0; i<headers.length; i++) {
            String val = validHeaders.get(headers[i].toLowerCase(Locale.US));
            if (val == null)
                throw new IllegalArgumentException("Invalid header " + headers[i]);
            headers[i] = val;
        }

        outputHeaders = headers;
        displayHeaders = new String[outputHeaders.length];
        for (int index = 0; index < displayHeaders.length; index++)
            displayHeaders[index] = isHeaderRequired() ? outputHeaders[index].toUpperCase(Locale.US) : "";

    }

    protected static JobOperator getJobOperatorFromBatchRuntime() {
        try {
            return BatchRuntime.getJobOperator();
        } catch (java.util.ServiceConfigurationError error) {
            throw new IllegalStateException("Could not get JobOperator. "
                + " Check if the Batch DataSource is configured properly and Check if the Database is up and running", error);
        } catch (Throwable ex) {
            throw new IllegalStateException("Could not get JobOperator. ", ex);
        }
    }

    protected boolean isHeaderRequired() {
        return !isTerse || header;
    }

    protected boolean supportsLongFormat() {
        return true;
    }

    protected abstract void executeCommand(AdminCommandContext context, Properties extraProps)
                throws Exception;

    protected abstract String[] getAllHeaders();

    protected abstract String[] getDefaultHeaders();

    protected String[] getOutputHeaders() {
        return outputHeaders;
    }

    protected String[] getDisplayHeaders() {
        return displayHeaders;
    }

}
