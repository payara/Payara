/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.javaee.core.deployment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;

import com.sun.enterprise.config.serverbeans.Application;
import com.sun.enterprise.config.serverbeans.Applications;
import com.sun.enterprise.config.serverbeans.Module;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.EjbBundleDescriptor;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.WebComponentDescriptor;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RestParam;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.deployment.common.ModuleDescriptor;
import org.glassfish.deployment.versioning.VersioningSyntaxException;
import org.glassfish.deployment.versioning.VersioningUtils;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.internal.deployment.Deployment;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;

/**
 * list-sub-components command
 */
@Service(name="list-sub-components")
@I18n("list.sub.components")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@ExecuteOn(value={RuntimeType.DAS})
@RestEndpoints({
    @RestEndpoint(configBean=Application.class,
        opType=RestEndpoint.OpType.GET, 
        path="list-sub-components", 
        description="List subcomponents",
        params={
            @RestParam(name="modulename", value="$parent")
        })
})
public class ListSubComponentsCommand implements AdminCommand {

    @Param(primary=true)
    private String modulename = null;

    @Param(optional=true)
    private String appname = null;

    @Param(optional=true)
    private String type = null;

    @Inject
    public ApplicationRegistry appRegistry;

    @Param(optional=true, defaultValue="false")
    private Boolean resources = false;

    @Param(optional=true, defaultValue="false", shortName="t")
    public Boolean terse = false;

    @Inject
    public Deployment deployment;

    @Inject
    public Applications applications;

    @Inject
    private CommandRunner commandRunner;

    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(ListSubComponentsCommand.class);    

    public void execute(AdminCommandContext context) {
        
        final ActionReport report = context.getActionReport();

        ActionReport.MessagePart part = report.getTopMessagePart();        

        String applicationName = modulename; 
        if (appname != null) {
            applicationName = appname;
        }

        try {
            VersioningUtils.checkIdentifier(applicationName);
        } catch (VersioningSyntaxException ex) {
            report.setMessage(ex.getLocalizedMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        if (!deployment.isRegistered(applicationName)) {
            report.setMessage(localStrings.getLocalString("application.notreg","Application {0} not registered", applicationName));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;

        }

        Application application = applications.getApplication(applicationName);

        if (application.isLifecycleModule()) {
            if (!terse) {
                part.setMessage(localStrings.getLocalString("listsubcomponents.no.elements.to.list", "Nothing to List."));
            }
            return;
        }

        ApplicationInfo appInfo = appRegistry.get(applicationName);
        if (appInfo == null) {
            report.setMessage(localStrings.getLocalString("application.not.enabled","Application {0} is not in an enabled state", applicationName));
            return;
        }

        com.sun.enterprise.deployment.Application app = appInfo.getMetaData(com.sun.enterprise.deployment.Application.class);

        if (app == null) {
            if (!terse) {
                part.setMessage(localStrings.getLocalString("listsubcomponents.no.elements.to.list", "Nothing to List."));
            }
            return;
        }

        Map<String, String> subComponents ;
        Map<String, String> subComponentsMap = new HashMap<String, String>();

        if (appname == null) {
            subComponents = getAppLevelComponents(app, type, subComponentsMap);
        } else {
            BundleDescriptor bundleDesc = app.getModuleByUri(modulename);
            if (bundleDesc == null) {
                report.setMessage(localStrings.getLocalString("listsubcomponents.invalidmodulename", "Invalid module name", appname, modulename));
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }
            subComponents = getModuleLevelComponents(
                bundleDesc, type, subComponentsMap);
        }
        
        // the type param can only have values "ejbs" and "servlets"
        if (type != null)  {
            if (!type.equals("servlets") && !type.equals("ejbs")) {
                report.setMessage(localStrings.getLocalString("listsubcomponents.invalidtype", "The type option has invalid value {0}. It should have a value of servlets or ejbs.", type));
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }
        }

        List<String> subModuleInfos = new ArrayList<String>();    
        if (!app.isVirtual()) {
            subModuleInfos = getSubModulesForEar(app, type);
        }

        int[] longestValue = new int[2];
        for (Map.Entry<String, String> entry : subComponents.entrySet()) {
            String key = entry.getKey();
            if (key.length() > longestValue[0]) {
                longestValue[0] = key.length();
            }
            String value = entry.getValue();
            if (value.length() > longestValue[1]) {
                longestValue[1] = value.length();
            }
        }
        StringBuilder formattedLineBuf = new StringBuilder();
        for (int j = 0; j < 2; j++) {
            longestValue[j] += 2;
            formattedLineBuf.append("%-")
                    .append(longestValue[j])
                    .append("s");
        }
        String formattedLine = formattedLineBuf.toString();
        if (!terse && subComponents.isEmpty()) {
            part.setMessage(localStrings.getLocalString("listsubcomponents.no.elements.to.list", "Nothing to List."));
        }
        int i=0;
        for (String key : subComponents.keySet()) {
            ActionReport.MessagePart childPart = part.addChild();
            childPart.setMessage(
                    String.format(formattedLine,
                    new Object[]{key, subComponents.get(key)} ));
            if (appname == null && !app.isVirtual()) {
                // we use the property mechanism to provide 
                // support for JSR88 client
                if (subModuleInfos.get(i) != null) {
                    childPart.addProperty("moduleInfo", 
                        subModuleInfos.get(i));
                }
            }
            if (resources) {
                Module module = application.getModule(key);
                if (module != null) {
                    ActionReport subReport = report.addSubActionsReport();
                    CommandRunner.CommandInvocation inv = commandRunner.getCommandInvocation("_list-resources", subReport, context.getSubject());
                    final ParameterMap parameters = new ParameterMap();
                    parameters.add("appname", application.getName());
                    parameters.add("modulename", module.getName());
                    inv.parameters(parameters).execute();

                    ActionReport.MessagePart subPart = subReport.getTopMessagePart();
                    for (ActionReport.MessagePart cp : subPart.getChildren()) {
                        ActionReport.MessagePart resourcesChildPart = childPart.addChild();
                        resourcesChildPart.setMessage("  " + cp.getMessage());
                    }
                }
            }
            i++;
        }

        // add the properties for GUI to display
        Set<String> keys = subComponentsMap.keySet();
        for (String key : keys) {
            part.addProperty(key, subComponentsMap.get(key));
        }
        // now this is the normal output for the list-sub-components command
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }

    // list sub components for ear
    private List<String> getSubModulesForEar(com.sun.enterprise.deployment.Application application, String type) {
        List<String> moduleInfoList = new ArrayList<String>();
        Collection<ModuleDescriptor<BundleDescriptor>> modules = 
            getSubModuleListForEar(application, type);
        for (ModuleDescriptor moduleDesc : modules) { 
            String moduleInfo = moduleDesc.getArchiveUri() + ":" + 
                moduleDesc.getModuleType(); 
             if (moduleDesc.getModuleType().equals(DOLUtils.warType())) {
                 moduleInfo = moduleInfo + ":" + moduleDesc.getContextRoot(); 
             }
             moduleInfoList.add(moduleInfo);
        }
        return moduleInfoList;
    }

    private Map<String, String> getAppLevelComponents(com.sun.enterprise.deployment.Application application, String type, Map<String, String> subComponentsMap) {
        Map<String, String> subComponentList = new LinkedHashMap<String, String>();
        if (application.isVirtual()) {
            // for standalone module, get servlets or ejbs
            BundleDescriptor bundleDescriptor = 
                application.getStandaloneBundleDescriptor();
            subComponentList = getModuleLevelComponents(bundleDescriptor, type, subComponentsMap);
        } else {
            // for ear case, get modules
            Collection<ModuleDescriptor<BundleDescriptor>> modules = 
                getSubModuleListForEar(application, type);

            for (ModuleDescriptor module : modules) {

                StringBuffer sb = new StringBuffer();
                String moduleName = module.getArchiveUri();
                sb.append("<");
                String moduleType = getModuleType(module);
                sb.append(moduleType);
                sb.append(">"); 
                subComponentList.put(moduleName, sb.toString());    
                subComponentsMap.put(module.getArchiveUri(), moduleType);
            }
        }
        return subComponentList;
    }

    private Collection<ModuleDescriptor<BundleDescriptor>> getSubModuleListForEar(com.sun.enterprise.deployment.Application application, String type) {
        Collection<ModuleDescriptor<BundleDescriptor>> modules = 
            new ArrayList<ModuleDescriptor<BundleDescriptor>>();
        if (type == null) {
            modules = application.getModules();
        } else if (type.equals("servlets")) {
            modules = application.getModuleDescriptorsByType(
                DOLUtils.warType());
        } else if (type.equals("ejbs")) {    
            modules = application.getModuleDescriptorsByType(
                DOLUtils.ejbType());
            // ejb in war case
            Collection<ModuleDescriptor<BundleDescriptor>> webModules = 
                application.getModuleDescriptorsByType(DOLUtils.warType());
            for (ModuleDescriptor webModule : webModules) {
                if (webModule.getDescriptor().getExtensionsDescriptors(EjbBundleDescriptor.class).size() > 0) {
                    modules.add(webModule);
                }
            }
        }

        return modules;
    }

    private Map<String, String> getModuleLevelComponents(BundleDescriptor bundle, 
        String type, Map<String, String> subComponentsMap) {
        Map<String, String> moduleSubComponentMap = new LinkedHashMap<String, String>();
        if (bundle instanceof WebBundleDescriptor) {
            WebBundleDescriptor wbd = (WebBundleDescriptor)bundle;
            // look at ejb in war case
            Collection<EjbBundleDescriptor> ejbBundleDescs = 
                wbd.getExtensionsDescriptors(EjbBundleDescriptor.class);
            if (ejbBundleDescs.size() > 0) {
                EjbBundleDescriptor ejbBundle = 
                        ejbBundleDescs.iterator().next();
                moduleSubComponentMap.putAll(getModuleLevelComponents(
                        ejbBundle, type, subComponentsMap));
            }

            if (type != null && type.equals("ejbs")) {    
                return moduleSubComponentMap;
            }
            for (WebComponentDescriptor wcd : 
                    wbd.getWebComponentDescriptors()) {
                StringBuffer sb = new StringBuffer();    
                String canonicalName = wcd.getCanonicalName();
                sb.append("<");
                String wcdType = (wcd.isServlet() ? "Servlet" : "JSP");
                sb.append(wcdType);
                sb.append(">"); 
                moduleSubComponentMap.put(canonicalName, sb.toString());
                subComponentsMap.put(wcd.getCanonicalName(), wcdType);
            }
        } else if (bundle instanceof EjbBundleDescriptor)  {
            if (type != null && type.equals("servlets")) {    
                return moduleSubComponentMap;
            }
            EjbBundleDescriptor ebd = (EjbBundleDescriptor)bundle;
            for (EjbDescriptor ejbDesc : ebd.getEjbs()) {
                StringBuffer sb = new StringBuffer();    
                String ejbName = ejbDesc.getName();
                sb.append("<");
                String ejbType = ejbDesc.getEjbTypeForDisplay();
                sb.append(ejbType);
                sb.append(">"); 
                moduleSubComponentMap.put(ejbName, sb.toString());
                subComponentsMap.put(ejbDesc.getName(), ejbType);
            }
        }

        return moduleSubComponentMap;
    }

    private String getModuleType(ModuleDescriptor modDesc) {
        String type = null;
        if (modDesc.getModuleType().equals(DOLUtils.ejbType())) {
            type = "EJBModule";
        } else if (modDesc.getModuleType().equals(DOLUtils.warType())) {
            type = "WebModule";
        } else if (modDesc.getModuleType().equals(DOLUtils.carType())) {
            type = "AppClientModule";
        } else if (modDesc.getModuleType().equals(DOLUtils.rarType())) {
            type = "ConnectorModule";
        }

        return type;
    }
}
