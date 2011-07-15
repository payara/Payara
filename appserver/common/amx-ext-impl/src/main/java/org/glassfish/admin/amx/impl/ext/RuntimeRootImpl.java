/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.amx.impl.ext;

import java.util.List;
import javax.management.ObjectName;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;

import com.sun.enterprise.module.ModulesRegistry;
import com.sun.enterprise.module.Module;

import com.sun.enterprise.security.ssl.SSLUtils;
import javax.management.JMException;
import javax.management.remote.JMXServiceURL;
import org.glassfish.admin.amx.base.RuntimeRoot;
import org.glassfish.admin.amx.base.ServerRuntime;
import org.glassfish.admin.amx.impl.mbean.AMXImplBase;
import org.glassfish.admin.amx.impl.util.ImplUtil;
import org.glassfish.admin.amx.intf.config.Domain;
import org.glassfish.admin.amx.intf.config.grizzly.NetworkConfig;
import org.glassfish.admin.amx.intf.config.grizzly.NetworkListener;
import org.glassfish.admin.amx.intf.config.grizzly.NetworkListeners;
import org.glassfish.admin.amx.intf.config.grizzly.Protocol;
import org.glassfish.admin.amx.util.ExceptionUtil;
import org.glassfish.api.container.Sniffer;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ModuleInfo;
import org.glassfish.internal.data.EngineRef;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.jvnet.hk2.component.Habitat;

import org.glassfish.admin.amx.impl.util.InjectedValues;

import org.glassfish.external.amx.AMXGlassfish;


import org.glassfish.api.admin.AdminCommandContext;
import com.sun.enterprise.v3.admin.RestartDomainCommand;
import com.sun.enterprise.v3.common.PlainTextActionReporter;
import org.glassfish.api.admin.AdminCommand;
import com.sun.enterprise.v3.admin.commands.JVMInformation;
import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.EjbBundleDescriptor;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.EjbSessionDescriptor;
import com.sun.enterprise.deployment.EjbEntityDescriptor;
import com.sun.enterprise.deployment.EjbMessageBeanDescriptor;
import com.sun.enterprise.deployment.WebComponentDescriptor;
import javax.management.MBeanServer;
import org.glassfish.admin.amx.impl.util.ObjectNameBuilder;
import org.glassfish.admin.amx.util.StringUtil;

/**
AMX RealmsMgr implementation.
Note that realms don't load until {@link #loadRealms} is called.
 */
public final class RuntimeRootImpl extends AMXImplBase
// implements RuntimeRoot
{
    private final ApplicationRegistry appRegistry;

    private final Habitat mHabitat;


    public RuntimeRootImpl(final ObjectName parent)
    {
        super(parent, RuntimeRoot.class);

        mHabitat = InjectedValues.getInstance().getHabitat();

        appRegistry = mHabitat.getComponent(ApplicationRegistry.class);


    }

    public ObjectName[] getServerRuntime()
    {
        return getChildren( ServerRuntime.class );
    }

    @Override
        protected final void
    registerChildren()
    {
        super.registerChildren();

        final ObjectName    self = getObjectName();
        final MBeanServer   server = getMBeanServer();
        final ObjectNameBuilder	objectNames	= new ObjectNameBuilder( server, self );

        ObjectName childObjectName = null;
        Object mbean = null;

        // when clustering comes along, some other party will need to register MBeans
        // for each non-DAS instance
        childObjectName	= objectNames.buildChildObjectName( ServerRuntime.class, AMXGlassfish.DEFAULT.dasName() );
        mbean	= new ServerRuntimeImpl(self);
        registerChild( mbean, childObjectName );
    }


    /**
     * Return a list of deployment descriptor maps for the specified
     * application.
     * In each map:
     * a. The module name is stored by the MODULE_NAME_KEY.
     * b. The path of the deployment descriptor is stored by the DD_PATH_KEY.
     * c. The content of the deployment descriptor is stored by the
     *    DD_CONTENT_KEY.
     * @param the application name
     * @return the list of deployment descriptor maps
     *
     */
    public List<Map<String, String>> getDeploymentConfigurations(final String appName)
    {
        final ApplicationInfo appInfo = appRegistry.get(appName);

        final List<Map<String, String>> resultList = new ArrayList<Map<String, String>>();
        if (appInfo == null)
        {
            return resultList;
        }
        try
        {
            if (appInfo.getEngineRefs().size() > 0)
            {
                // composite archive case, i.e. ear
                for (EngineRef ref : appInfo.getEngineRefs())
                {
                    Sniffer appSniffer = ref.getContainerInfo().getSniffer();
                    addToResultDDList("", appSniffer.getDeploymentConfigurations(appInfo.getSource()), resultList);
                }

                for (ModuleInfo moduleInfo : appInfo.getModuleInfos())
                {
                    for (Sniffer moduleSniffer : moduleInfo.getSniffers())
                    {
                        ReadableArchive moduleArchive = appInfo.getSource().getSubArchive(moduleInfo.getName());
                        addToResultDDList(moduleInfo.getName(), moduleSniffer.getDeploymentConfigurations(moduleArchive), resultList);
                    }
                }
            }
            else
            {
                // standalone module
                for (Sniffer sniffer : appInfo.getSniffers())
                {
                    addToResultDDList(appName, sniffer.getDeploymentConfigurations(appInfo.getSource()), resultList);
                }
            }
        }
        catch ( final IOException e)
        {
            throw new RuntimeException(e);
        }

        return resultList;
    }

    private void addToResultDDList(String moduleName, Map<String, String> snifferConfigs, List<Map<String, String>> resultList)
    {
        for (String pathKey : snifferConfigs.keySet())
        {
            HashMap<String, String> resultMap =
                    new HashMap<String, String>();
            resultMap.put(RuntimeRoot.MODULE_NAME_KEY, moduleName);
            resultMap.put(RuntimeRoot.DD_PATH_KEY, pathKey);
            resultMap.put(RuntimeRoot.DD_CONTENT_KEY, snifferConfigs.get(pathKey));
            resultList.add(resultMap);
        }
    }

    public void stopDomain()
    {
        final ModulesRegistry registry = InjectedValues.getInstance().getModulesRegistry();
        final Collection<Module> modules = registry.getModules("com.sun.enterprise.osgi-adapter");
        if (modules.size() == 1)
        {
            final Module mgmtAgentModule = modules.iterator().next();
            mgmtAgentModule.stop();
        }
        else
        {
            ImplUtil.getLogger().warning("Cannot find primordial com.sun.enterprise.osgi-adapter");
        }

        ImplUtil.getLogger().warning("Stopping server forcibly");
        System.exit(0);
    }

    public void restartDomain()
    {
        final ModulesRegistry registry = InjectedValues.getInstance().getModulesRegistry();

        final AdminCommandContext ctx = new AdminCommandContext(ImplUtil.getLogger(), new PlainTextActionReporter());
        final AdminCommand cmd = new RestartDomainCommand(registry);
        cmd.execute(ctx);
    }

    private NetworkConfig networkConfig()
    {
        return getDomainRootProxy().child(Domain.class).getConfigs().getConfig().get("server-config").getNetworkConfig().as(NetworkConfig.class);
    }

    private static final String ADMIN_LISTENER_NAME = "admin-listener";

    private NetworkListener getAdminListener()
    {
        final NetworkConfig network = networkConfig();

        final NetworkListeners listeners = network.getNetworkListeners();

        final Map<String, NetworkListener> listenersMap = listeners.getNetworkListener();

        final NetworkListener listener = listenersMap.get(ADMIN_LISTENER_NAME);

        return listener;
    }

    private int getRESTPort()
    {
        return (int) (long) getAdminListener().resolveLong("Port");
    }

    private String get_asadmin()
    {
        final Protocol protocol = networkConfig().getProtocols().getProtocol().get(ADMIN_LISTENER_NAME);
        return protocol.getHttp().resolveAttribute("DefaultVirtualServer");
    }

    public String getRESTBaseURL()
    {
        final Protocol protocol = networkConfig().getProtocols().getProtocol().get(ADMIN_LISTENER_NAME);
        final String scheme = protocol.resolveBoolean("SecurityEnabled") ? "https" : "http";
        final String host = "localhost";

        return scheme + "://" + host + ":" + getRESTPort() + "/" + get_asadmin() + "/";
    }


    public String executeREST(final String cmd)
    {
        String result = null;

        HttpURLConnection conn = null;
        try
        {
            final String url = getRESTBaseURL() + cmd;

            final URL invoke = new URL(url);
            //System.out.println( "Opening connection to: " + invoke );
            conn = (HttpURLConnection) invoke.openConnection();

            final InputStream is = conn.getInputStream();
            result = toString(is);
            is.close();
        }
        catch (Exception e)
        {
            result = ExceptionUtil.toString(e);
        }
        finally
        {
            if (conn != null)
            {
                conn.disconnect();
            }
        }
        return result;
    }

    public String[] getSupportedCipherSuites()
    {
        try
        {
            final SSLUtils sslUtils = mHabitat.getComponent(SSLUtils.class);
            return sslUtils.getSupportedCipherSuites();
        }
        catch (final Exception ex)
        {
            ImplUtil.getLogger().log( Level.INFO, "Can't get cipher suites", ex);
            return new String[0];
        }
    }

    public String[] getJMXServiceURLs()
    {
        try
        {
            final AMXGlassfish amxg = AMXGlassfish.DEFAULT;
            final JMXServiceURL[] items = (JMXServiceURL[])getMBeanServer().getAttribute(amxg.getBootAMXMBeanObjectName(), "JMXServiceURLs");
            final String [] urls = new String[ items.length ];
            for( int i = 0; i < items.length; ++i )
            {
                urls[i] = "" + items[i];
            }
            return urls;
        }
        catch (final JMException e)
        {
            throw new RuntimeException(e);
        }
    }

    public String getJVMReport(final String type)
    {
        final JVMInformation info = new JVMInformation(getMBeanServer());

        final String NL = StringUtil.LS;
        final String target = "das";
        String result = "FAILED";
        if ("summary".equals(type))
        {
            result = info.getSummary(target);
        }
        else if ("memory".equals(type))
        {
            result = info.getMemoryInformation(target);
        }
        else if ("thread".equals(type))
        {
            result = info.getThreadDump(target);
        }
        else if ("class".equals(type))
        {
            result = info.getClassInformation(target);
        }
        else if ("log".equals(type))
        {
            result = info.getLogInformation(target);
        }
        else if ("all".equals(type))
        {
            result = "SUMMARY" + NL + NL + getJVMReport("summary") + NL + NL +
                     "MEMORY" + NL + NL + getJVMReport("memory") + NL + NL +
                     "THREADS" + NL + NL + getJVMReport("thread") + NL + NL +
                     "CLASSES" + NL + NL + getJVMReport("class") + NL + NL +
                     "LOGGING" + NL + NL + getJVMReport("log");
        }
        else
        {
            throw new IllegalArgumentException("Unsupported JVM report type: " + type);
        }

        if (result != null)
        {
            result = result.replace("%%%EOL%%%", NL);
        }
        return result;
    }
    
    public boolean isStartedInDebugMode()
    {
        boolean inDebugMode = false;

        final String s = System.getProperty("hk2.startup.context.args");
        if ( s != null )
        {
            final String prefix = "-debug=";
            final String[] ss = s.split("\n");

            for( final String opt : ss)
            {
                if( opt.startsWith(prefix) )
                {
                    final String value = opt.substring( prefix.length() ).toLowerCase();
                    //System.out.println( "RuntimeRootImpl.isRunningInDebugMode(): found: " + prefix + value );
                    inDebugMode = Boolean.valueOf(value );
                    break;
                }
            }
        }
        return inDebugMode;
    }

    /**
     * Return the subcomponents (ejb/web) of a specified module. 
     * @param applicationName the application name
     * @param moduleName the module name
     * @return a map of the sub components, where the key is the component
     *         name and the value is the component type
     */
    public Map<String, String> getSubComponentsOfModule(
        String applicationName, String moduleName) {
        ApplicationRegistry appRegistry = mHabitat.getComponent(
            ApplicationRegistry.class);

        ApplicationInfo appInfo = appRegistry.get(applicationName);
        if (appInfo != null) {
            Application app = appInfo.getMetaData(Application.class);
            if (app != null) {
                BundleDescriptor bundleDesc = app.getModuleByUri(moduleName);
                if (bundleDesc != null) {
                    return getModuleLevelComponents(bundleDesc);
                }
            }
        }
        return Collections.emptyMap();
    }

    private Map<String, String> getModuleLevelComponents(
        BundleDescriptor bundle) {
        Map<String, String> subComponentsMap = new HashMap<String, String>();
        if (bundle instanceof WebBundleDescriptor) {
            WebBundleDescriptor wbd = (WebBundleDescriptor)bundle;
            // look at ejb in war case
            Collection<EjbBundleDescriptor> ejbBundleDescs =
                wbd.getExtensionsDescriptors(EjbBundleDescriptor.class);
            if (ejbBundleDescs.size() > 0) {
                EjbBundleDescriptor ejbBundle =
                        ejbBundleDescs.iterator().next();
                subComponentsMap.putAll(getModuleLevelComponents(ejbBundle));
            }

            for (WebComponentDescriptor wcd :
                   wbd.getWebComponentDescriptors()) {
                String wcdName = wcd.getCanonicalName();
                String wcdType = wcd.isServlet() ? "Servlet" : "JSP";
                subComponentsMap.put(wcdName, wcdType);
            }
        } else if (bundle instanceof EjbBundleDescriptor)  {
            EjbBundleDescriptor ebd = (EjbBundleDescriptor)bundle;
            for (EjbDescriptor ejbDesc : ebd.getEjbs()) {
                String ejbName = ejbDesc.getName();
                String ejbType = getEjbType(ejbDesc);
                subComponentsMap.put(ejbName, ejbType);
            }
        }

        return subComponentsMap;
    }


    private String getEjbType(EjbDescriptor ejbDesc) {
        String type = null;
        if (ejbDesc.getType().equals(EjbSessionDescriptor.TYPE)) {
            EjbSessionDescriptor sessionDesc = (EjbSessionDescriptor)ejbDesc;
            if (sessionDesc.isStateful()) {
                type = "StatefulSessionBean";
            } else if (sessionDesc.isStateless()) {
                type = "StatelessSessionBean";
            } else if (sessionDesc.isSingleton()) {
                type = "SingletonSessionBean";
            }
        } else if (ejbDesc.getType().equals(EjbMessageBeanDescriptor.TYPE)) {
            type = "MessageDrivenBean";

        } else if (ejbDesc.getType().equals(EjbEntityDescriptor.TYPE)) {
            type = "EntityBean";
        }

        return type;
    }

    /**
     * Return the context root of a specified module.
     * @param applicationName the application name
     * @param moduleName the module name
     * @return the context root of a specified module
     */
    public String getContextRoot(String applicationName, String moduleName) {
        ApplicationRegistry appRegistry = mHabitat.getComponent(
            ApplicationRegistry.class);

        ApplicationInfo appInfo = appRegistry.get(applicationName);
        if (appInfo != null) {
            Application app = appInfo.getMetaData(Application.class);
            if (app != null) {
                BundleDescriptor bundleDesc = app.getModuleByUri(moduleName);
                if (bundleDesc != null && 
                    bundleDesc instanceof WebBundleDescriptor) {
                    return ((WebBundleDescriptor)bundleDesc).getContextRoot();
                }
            }
        }
        return null;
    }
}

























