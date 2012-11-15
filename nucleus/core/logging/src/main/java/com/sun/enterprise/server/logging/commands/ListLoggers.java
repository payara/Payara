/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.server.logging.commands;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Locale;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.inject.Inject;

import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.hk2.api.PerLookup;

import com.sun.enterprise.server.logging.LoggerInfoMetadata;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.ColumnFormatter;
import com.sun.enterprise.util.LocalStringManagerImpl;

@ExecuteOn({RuntimeType.DAS})
@Service(name = "list-loggers")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("list.loggers")
@RestEndpoints({
    @RestEndpoint(configBean=Domain.class,
        opType=RestEndpoint.OpType.GET, 
        path="list-loggers", 
        description="list-loggers")
})
public class ListLoggers implements AdminCommand {

    private static final String UNKNOWN = "?";
    
    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(ListLoggers.class);
    
    @Param(optional=true, name="_internal", defaultValue="false")    
    private boolean listInternalLoggers;
    
    @Inject
    private LoggerInfoMetadata loggerInfoMetadataService;

    public void execute(AdminCommandContext context) {

        final ActionReport report = context.getActionReport();
        String header_name = localStrings.getLocalString("list.loggers.header.name", "Logger Name");
        String header_subsystem = localStrings.getLocalString("list.loggers.header.subsystem", "Subsystem");
        String header_description = localStrings.getLocalString("list.loggers.header.description", "Logger Description");
        
        ColumnFormatter colFormatter = new ColumnFormatter(new String[]{header_name, header_subsystem, header_description});
                
        // An option to specify client locale should be supported. However, it probably
        // should not be specific to this command. For now, localize using the default locale.
        Locale locale = Locale.getDefault();

        try {
            Set<String> loggers = loggerInfoMetadataService.getLoggerNames();
            Set<String> sortedLoggers = new TreeSet<String>(loggers);
            // The following Map & List are used to hold the REST data
            Map<String, String> loggerSubsystems = new TreeMap<String, String>();
            Map<String, String> loggerDescriptions = new TreeMap<String, String>();
            List<String> loggerList = new ArrayList<String>();
            
            for (String logger : sortedLoggers) {
                String subsystem = loggerInfoMetadataService.getSubsystem(logger);
                String desc = loggerInfoMetadataService.getDescription(logger, locale);
                boolean published = loggerInfoMetadataService.isPublished(logger);
                if (subsystem == null) subsystem = UNKNOWN;
                if (desc == null) desc = UNKNOWN;
                if (published || listInternalLoggers) {
                    colFormatter.addRow(new Object[]{logger, subsystem, desc});
                    loggerSubsystems.put(logger, subsystem);
                    loggerDescriptions.put(logger, desc); //Needed for REST xml and JSON output
                    loggerList.add(logger); //Needed for REST xml and JSON output                                    
                }
            }
            
            report.appendMessage(colFormatter.toString());
            report.appendMessage(System.getProperty("line.separator"));
            
            // Populate the extraProperties data structure for REST...
            Properties restData = new Properties();
            restData.put("loggerSubsystems", loggerSubsystems);
            restData.put("loggerDescriptions", loggerDescriptions);
            restData.put("loggers", loggerList);
            report.setExtraProperties(restData);

        } catch (Exception ex) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ex.printStackTrace(new PrintStream(out));
            report.setMessage(localStrings.getLocalString("list.loggers.failed",
                    "Error listing loggers: {0}", out.toString()));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }
        
}
