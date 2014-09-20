/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.deployment.admin;

import javax.security.auth.Subject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RestParam;
import org.glassfish.api.Param;
import org.glassfish.api.I18n;
import org.glassfish.api.container.Sniffer;
import org.glassfish.internal.deployment.SnifferManager;
import org.glassfish.config.support.TargetType;
import org.glassfish.config.support.CommandTarget;
import org.jvnet.hk2.annotations.Service;
import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.util.LocalStringManagerImpl;

import javax.inject.Inject;
import org.glassfish.hk2.api.PerLookup;

import java.util.*;
import org.glassfish.api.admin.AccessRequired;
import org.glassfish.api.admin.AccessRequired.AccessCheck;
import org.glassfish.api.admin.AdminCommandSecurity;

/**
 * list-components command
 */
@Service(name="list-components")
@I18n("list.components")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@ExecuteOn(value={RuntimeType.DAS})
@TargetType(value={CommandTarget.DOMAIN, CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER})
@RestEndpoints({
    @RestEndpoint(configBean=Cluster.class,
        opType=RestEndpoint.OpType.GET, 
        path="list-components", 
        description="list-components",
        params={
            @RestParam(name="target", value="$parent")
        }),
    @RestEndpoint(configBean=Domain.class,
        opType=RestEndpoint.OpType.GET, 
        path="list-components", 
        description="list-components",
        params={
            @RestParam(name="target", value="$parent")
        }),
    @RestEndpoint(configBean=Server.class,
        opType=RestEndpoint.OpType.GET, 
        path="list-components", 
        description="list-components",
        params={
            @RestParam(name="target", value="$parent")
        })
})
public class ListComponentsCommand implements AdminCommand, AdminCommandSecurity.AccessCheckProvider {

    @Param(optional=true)
    String type = null;

    @Param(primary=true, optional=true)
    public String target = "server";

    @Param(optional=true, defaultValue="false", name="long", shortName="l")
    public Boolean long_opt = false;

    @Param(optional=true, defaultValue="false", shortName="t")
    public Boolean terse = false;

    @Param(optional=true, defaultValue="false")
    public Boolean subcomponents = false;

    @Param(optional=true, defaultValue="false")
    private Boolean resources = false;

    private static String[] validTypes = new String[] {"application",  "appclient", "connector", "ejb", "web", "webservice"};

    @Inject
    protected Domain domain;

    @Inject
    SnifferManager snifferManager;
 
    @Inject
    CommandRunner commandRunner;
    
    private final List<Application> apps = new ArrayList<Application>();

    @Override
    public Collection<? extends AccessCheck> getAccessChecks() {
        final List<AccessCheck> accessChecks = new ArrayList<AccessCheck>();
        /* Read access to the collection of applications. */
        accessChecks.add(new AccessCheck(DeploymentCommandUtils.APPLICATION_RESOURCE_NAME, "read"));
        
        /*
         * Because the command displays detailed information about the matching
         * apps, require read access to each app to be displayed.
         */
        for (Application app : domain.getApplicationsInTarget(target)) {
            if (!app.isLifecycleModule()) {
                if (type == null || isApplicationOfThisType(app, type)) {
                    apps.add(app);
                    accessChecks.add(new AccessCheck(AccessRequired.Util.resourceNameFromConfigBeanProxy(app), "read"));
                }
            }
        }
        return accessChecks;
    }
    
    

    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(ListComponentsCommand.class);    

    public void execute(AdminCommandContext context) {
        
        final ActionReport report = context.getActionReport();
        final ActionReport subReport = report.addSubActionsReport();

        ActionReport.MessagePart part = report.getTopMessagePart();        
        int numOfApplications = 0;
        List<String[]> rowList = new ArrayList<String[]>();

        if (type != null) {
             List<String> validTypeList = Arrays.asList(validTypes);
             if (!validTypeList.contains(type)) {
                 report.setMessage(localStrings.getLocalString("list.components.invalid.type", "Invalid type option value."));
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }
        }
        
        for (Application app : apps) {
            String[] currentRow;
            if( !terse && long_opt ){
                currentRow = new String[]{ app.getName(),
                    getAppSnifferEngines(app, true),
                    getLongStatus(app) };
            } else {
                currentRow = new String[]{ app.getName(),
                    getAppSnifferEngines(app, true)};
            }
            part.addProperty(app.getName(), 
                getAppSnifferEngines(app, false));
            rowList.add(currentRow);
            numOfApplications++;

        }
        // Starting output formatting
        int numCols = 2;
        String[] headings = new String[]{"NAME", "TYPE", "STATUS"};
        int longestValue[] = new int[numCols];
        if ( !terse && long_opt ) {
            numCols = 3;
            longestValue = new int[] {headings[0].length(), headings[1].length(), headings[2].length() };
        }
        for (String v[] : rowList) {
            for (int i = 0; i < v.length; i++) {
                if (v[i].length() > longestValue[i]) {
                   longestValue[i] = v[i].length();
                }
            }
        }
        StringBuilder formattedLineBuf = new StringBuilder();
        for (int i = 0; i < numCols; i++) {
            longestValue[i] += 2;
            formattedLineBuf.append("%-")
                    .append(longestValue[i])
                    .append("s");
        }
        String formattedLine = formattedLineBuf.toString();
        if ( !terse ) {
            if (rowList.isEmpty()) {
                subReport.setMessage(localStrings.getLocalString(
                        DeployCommand.class,
                        "list.no.applications.deployed",
                        "No applications are deployed to this target {0}.",
                        new Object[] {this.target}));
                part.setMessage(localStrings.getLocalString("list.components.no.elements.to.list", "Nothing to List."));
            } else if ( long_opt ) {
                ActionReport.MessagePart childPart = part.addChild();
                childPart.setMessage(String.format(formattedLine, (Object[])headings ));
            }
        }
        for (String v[] : rowList) {
            StringBuilder sb = new StringBuilder();
            ActionReport.MessagePart childPart = part.addChild();
            sb.append(String.format(formattedLine, (Object[])v));
            if(resources){
                displayAppScopedResources(v[0], report, childPart, context.getSubject());
            }
            if (subcomponents) {
                displaySubComponents(v[0], report, childPart, context.getSubject());
            }
            childPart.setMessage(sb.toString());
        }
	// Ending output formatting
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }

    private String getLongStatus(Application app) {
       String message = "";
       boolean isVersionEnabled = domain.isAppEnabledInTarget(app.getName(), target);
       if ( isVersionEnabled ) {
           message = localStrings.getLocalString("list.applications.verbose.enabled", "enabled");
       } else {
           message = localStrings.getLocalString("list.applications.verbose.disabled", "disabled");
       }
       return message;
   }


        /**
         * check the type of application by comparing the sniffer engine.
         * @param app - Application
         * @param type - type of application
         * @return true if application is of the specified type else return
         *  false.
         */
    boolean isApplicationOfThisType(final Application app, String type) {
        // do the type conversion to be compatible with v2 syntax
        if (type.equals("application")) {
            type = "ear";
        } else if (type.equals("webservice")) {
            type = "webservices";
        }

        List <Engine> engineList = getAppEngines(app);
        for (Engine engine : engineList) {
            if (engine.getSniffer().equals(type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * return all user visible sniffer engines in an application.
     * The return format is <sniffer1, sniffer2, ...>
     * @param module - Application's module
     * @return sniffer engines
     */
    String getAppSnifferEngines(final Application app, final boolean format) {
        return getSniffers(getAppEngines(app), format);
    }

    /**
     * return all user visible sniffer engines in an application.
     * The return format is <sniffer1, sniffer2, ...>
     * @param module - Application's module
     * @return sniffer engines
     */
    String getSnifferEngines(final Module module, final boolean format) {
        return getSniffers (module.getEngines(), format);
    }

    private String getSniffers(final List<Engine> engineList, 
        final boolean format) {
        Set<String> snifferSet = new LinkedHashSet<String>();
        for (Engine engine : engineList) {
            final String engType = engine.getSniffer();
            if (displaySnifferEngine(engType)) {
                snifferSet.add(engine.getSniffer());
            }
        }

        StringBuffer se = new StringBuffer();

        if (!snifferSet.isEmpty()) {
            if (format) {
                se.append("<");
            }
            for (String sniffer : snifferSet) {
                se.append(sniffer + ", ");
            }
            //eliminate the last "," and end the list with ">"
            if (se.length()>2) {
                se.replace(se.length()-2, se.length(), (format)?">":"");
            } else if (format) {
                se.append(">");
            }
        }
        return se.toString();
    }

    private List<Engine> getAppEngines(final Application app) {
        final List<Engine> engineList = new ArrayList<Engine>();

        // first add application level engines
        engineList.addAll(app.getEngine());

        // now add module level engines
        for (Module module: app.getModule()) {
            engineList.addAll(module.getEngines());
        }

        return engineList;
    }

    /**
     * check to see if Sniffer engine is to be visible by the user.
     *
     * @param engType - type of sniffer engine
     * @return true if visible, else false.
     */
    boolean displaySnifferEngine(String engType) {
        final Sniffer sniffer = snifferManager.getSniffer(engType);
        return sniffer.isUserVisible();
    }

    private void displayAppScopedResources(String applicationName, ActionReport report, ActionReport.MessagePart part,
            final Subject subject) {

        Application application = null;
        List<Application> applications = domain.getApplicationsInTarget(target);
        for (Application app : applications) {
            if (app.getName().equals(applicationName)) {
                application = app;
            }
        }
        if (application != null) {
            ActionReport subReport = report.addSubActionsReport();
            CommandRunner.CommandInvocation inv = commandRunner.getCommandInvocation("_list-resources", subReport, subject);
            final ParameterMap parameters = new ParameterMap();
            parameters.add("appname", application.getName());
            inv.parameters(parameters).execute();

            ActionReport.MessagePart subPart = subReport.getTopMessagePart();
            for (ActionReport.MessagePart cp: subPart.getChildren()) {
                ActionReport.MessagePart resourcesChildPart = part.addChild();
                resourcesChildPart.setMessage(cp.getMessage());
            }
        }
    }

    private void displaySubComponents(String appName, ActionReport report,
        ActionReport.MessagePart part, final Subject subject) {
        ActionReport subReport = report.addSubActionsReport();
        CommandRunner.CommandInvocation inv = commandRunner.getCommandInvocation("list-sub-components", subReport, subject);

        final ParameterMap parameters = new ParameterMap();
        parameters.add("DEFAULT", appName);
        parameters.add("terse", "true");
        parameters.add("resources", resources.toString());

        inv.parameters(parameters).execute();

        ActionReport.MessagePart subPart = subReport.getTopMessagePart();
        for (ActionReport.MessagePart childPart: subPart.getChildren()) {
            ActionReport.MessagePart actualChildPart = part.addChild();
            actualChildPart.setMessage("  " + childPart.getMessage());
            for(ActionReport.MessagePart grandChildPart : childPart.getChildren()){
                actualChildPart.addChild().setMessage(grandChildPart.getMessage());
            }
        }
    }

}
