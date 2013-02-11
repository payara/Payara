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

import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Mahesh Kannan
 *
 */
public abstract class AbstractListCommand
    implements AdminCommand {

    @Inject
    GlassFishBatchRuntimeConfigurator glassFishBatchRuntimeConfigurator;

    @Inject
    protected Logger logger;

    @Param(name = "long", shortName = "l", optional = true)
    protected boolean useLongFormat;

    @Param(name = "terse", optional=true, defaultValue="false", shortName="t")
    public boolean isTerse = false;

    @Param(name = "output", shortName = "o", optional = true)
    protected String outputHeaderList;

    @Param(name = "header", shortName = "h", optional = true)
    protected boolean header;

    protected String[] outputHeaders;

    protected String[] displayHeaders;

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
            if (!glassFishBatchRuntimeConfigurator.isInitialized())
                glassFishBatchRuntimeConfigurator.initializeBatchRuntime();
            executeCommand(context, extraProperties);
            actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Exception during command ", ex);
            actionReport.setMessage(ex.getMessage());
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
    }

    protected void calculateHeaders() {
        String[] headers = getTerseHeaders();
        if (outputHeaderList != null) {
            headers = outputHeaderList.split("[,]");
            if (headers.length == 0)
                headers = getTerseHeaders();
        } else if (useLongFormat)
            headers = getSupportedHeaders();

        Set<String> validHeaders = new HashSet<String>();
        for (String h : getSupportedHeaders())
            validHeaders.add(h.toLowerCase(Locale.US));
        for (int i=0; i<headers.length; i++) {
            if (!validHeaders.contains(headers[i].toLowerCase(Locale.US))) {
                throw new IllegalArgumentException("IllegalArgument " + headers[i]);
            }
            headers[i] = headers[i].toLowerCase(Locale.US);
        }

        outputHeaders = headers;
        displayHeaders = new String[outputHeaders.length];
        for (int index = 0; index < displayHeaders.length; index++)
            displayHeaders[index] = isHeaderRequired() ? outputHeaders[index].toUpperCase(Locale.US) : "";

    }

    protected boolean isHeaderRequired() {
        return !isTerse || header;
    }

    protected abstract void executeCommand(AdminCommandContext context, Properties extraProps);

    protected abstract String[] getSupportedHeaders();

    protected abstract String[] getTerseHeaders();

    protected String[] getOutputHeaders() {
        return outputHeaders;
    }

    protected String[] getDisplayHeaders() {
        return displayHeaders;
    }

}
