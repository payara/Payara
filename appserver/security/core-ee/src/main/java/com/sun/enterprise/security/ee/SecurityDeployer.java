/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2012 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2016-2019] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.security.ee;

import static com.sun.enterprise.deployment.WebBundleDescriptor.AFTER_SERVLET_CONTEXT_INITIALIZED_EVENT;
import static com.sun.enterprise.security.ee.SecurityUtil.getContextID;
import static java.util.logging.Level.WARNING;
import static org.glassfish.internal.deployment.Deployment.APPLICATION_LOADED;
import static org.glassfish.internal.deployment.Deployment.APPLICATION_PREPARED;
import static org.glassfish.internal.deployment.Deployment.MODULE_LOADED;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.MetaData;
import org.glassfish.api.deployment.OpsParams;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.api.invocation.RegisteredComponentInvocationHandler;
import org.glassfish.deployment.common.DeploymentException;
import org.glassfish.deployment.common.DummyApplication;
import org.glassfish.deployment.common.SimpleDeployer;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ModuleInfo;
import org.glassfish.security.common.CNonceCache;
import org.glassfish.security.common.HAUtil;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.EjbBundleDescriptor;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.web.LoginConfiguration;
import com.sun.enterprise.security.AppCNonceCacheMap;
import com.sun.enterprise.security.CNonceCacheFactory;
import com.sun.enterprise.security.EjbSecurityPolicyProbeProvider;
import com.sun.enterprise.security.WebSecurityDeployerProbeProvider;
import com.sun.enterprise.security.jacc.JaccWebAuthorizationManager;
import com.sun.enterprise.security.util.IASSecurityException;
import com.sun.enterprise.security.web.integration.WebSecurityManagerFactory;
import com.sun.logging.LogDomains;

/**
 * Security Deployer which generate and clean the security policies
 *
 */
@Service(name = "Security")
public class SecurityDeployer extends SimpleDeployer<SecurityContainer, DummyApplication> implements PostConstruct {

    private static final Logger _logger = LogDomains.getLogger(SecurityDeployer.class, LogDomains.SECURITY_LOGGER);
    
    @Inject
    private ServerContext serverContext;

    @Inject
    @Named("webSecurityCIH")
    private Provider<RegisteredComponentInvocationHandler> registeredComponentInvocationHandlerProvider;

    @Inject
    private Provider<Events> eventsProvider;

    @Inject
    private Provider<HAUtil> haUtilProvider;

    @Inject
    private Provider<AppCNonceCacheMap> appCNonceCacheMapProvider;

    @Inject
    private Provider<CNonceCacheFactory> cNonceCacheFactoryProvider;

    @Inject
    private WebSecurityManagerFactory webSecurityManagerFactory;

    // Required for HA Enabling CNonceCache for HTTPDigest Auth
    private AppCNonceCacheMap appCnonceMap;
    private HAUtil haUtil;
    private CNonceCacheFactory cnonceCacheFactory;
    private static final String HA_CNONCE_BS_NAME = "HA-CNonceCache-Backingstore";

    private EventListener listener;
    private static WebSecurityDeployerProbeProvider websecurityProbeProvider = new WebSecurityDeployerProbeProvider();
    private static EjbSecurityPolicyProbeProvider ejbProbeProvider = new EjbSecurityPolicyProbeProvider();

    private class AppDeployEventListener implements EventListener {

        @Override
        public void event(Event event) {
            Application app = null;
            
            if (MODULE_LOADED.equals(event.type())) {
                ModuleInfo moduleInfo = (ModuleInfo) event.hook();
                if (moduleInfo instanceof ApplicationInfo) {
                    return;
                }
                WebBundleDescriptor webBD = (WebBundleDescriptor) moduleInfo.getMetaData("org.glassfish.web.deployment.descriptor.WebBundleDescriptorImpl");
                loadPolicy(webBD, false);
            } else if (APPLICATION_LOADED.equals(event.type())) {
                ApplicationInfo appInfo = (ApplicationInfo) event.hook();
                app = appInfo.getMetaData(Application.class);
                if (app == null) {
                    // This is not a Java EE module, just return
                    return;
                }

                Set<WebBundleDescriptor> webBundleDescriptors = app.getBundleDescriptors(WebBundleDescriptor.class);
                linkPolicies(app, webBundleDescriptors);
                commitEjbs(app);
                
                if (webBundleDescriptors != null && !webBundleDescriptors.isEmpty()) {
                    // Register the WebSecurityComponentInvocationHandler
                    RegisteredComponentInvocationHandler handler = registeredComponentInvocationHandlerProvider.get();
                    if (handler != null) {
                        handler.register();
                    }
                }
            } else if (AFTER_SERVLET_CONTEXT_INITIALIZED_EVENT.equals(event.type())) {
                commitPolicy((WebBundleDescriptor) event.hook());
            }
        }
    };
    
    @Override
    public void postConstruct() {
        listener = new AppDeployEventListener();
        eventsProvider.get().register(listener);
    }

    // creates security policy if needed
    @Override
    protected void generateArtifacts(DeploymentContext context) throws DeploymentException {
        OpsParams params = context.getCommandParameters(OpsParams.class);
        if (params.origin.isArtifactsPresent()) {
            return;
        }
        
        String appName = params.name();
        try {
            Application app = context.getModuleMetaData(Application.class);
            Set<WebBundleDescriptor> webBundleDescriptors = app.getBundleDescriptors(WebBundleDescriptor.class);
            if (webBundleDescriptors == null) {
                return;
            }

            for (WebBundleDescriptor webBundleDescriptor : webBundleDescriptors) {
                loadPolicy(webBundleDescriptor, false);
            }

        } catch (Exception se) {
            throw new DeploymentException("Error in generating security policy for " + appName, se);
        }
    }

    // removes security policy if needed
    @Override
    protected void cleanArtifacts(DeploymentContext context) throws DeploymentException {
        removePolicy(context);
        SecurityUtil.removeRoleMapper(context);
        
        OpsParams params = context.getCommandParameters(OpsParams.class);
        if (this.appCnonceMap != null) {
            CNonceCache cache = appCnonceMap.remove(params.name());
            if (cache != null) {
                cache.destroy();
            }
        }
    }

    @Override
    public DummyApplication load(SecurityContainer container, DeploymentContext context) {
        DeployCommandParameters dparams = context.getCommandParameters(DeployCommandParameters.class);
        Application app = context.getModuleMetaData(Application.class);
        handleCNonceCacheBSInit(app.getAppName(), app.getBundleDescriptors(WebBundleDescriptor.class), dparams.availabilityenabled);

        return new DummyApplication();
    }

    @Override
    public void unload(DummyApplication container, DeploymentContext context) {
        cleanSecurityContext(context.getCommandParameters(OpsParams.class).name());
    }
    
    @Override
    public MetaData getMetaData() {
        return new MetaData(false, null, new Class[] { Application.class });
    }
    
    
    
    // ### Private methods
    

    /**
     * Translate Web Bundle Policy
     * 
     * @param webDescriptor
     * @param remove boolean indicated whether any existing policy statements are removed form context before translation
     * @throws DeploymentException
     */
    private void loadPolicy(WebBundleDescriptor webDescriptor, boolean remove) throws DeploymentException {
        try {
            if (webDescriptor != null) {
                if (remove) {
                    JaccWebAuthorizationManager authorizationManager = webSecurityManagerFactory.getManager(getContextID(webDescriptor), null, true);
                    if (authorizationManager != null) {
                        authorizationManager.release();
                    }
                }
                webSecurityManagerFactory.createManager(webDescriptor, true, serverContext);
            }

        } catch (Exception se) {
            throw new DeploymentException("Error in generating security policy for " + webDescriptor.getModuleDescriptor().getModuleName(), se);
        }
    }

    /**
     * Puts Web Bundle Policy In Service, repeats translation is Descriptor indicate policy was changed by ContextListener.
     * 
     * @param webBundleDescriptor
     * @throws DeploymentException
     */
    private void commitPolicy(WebBundleDescriptor webBundleDescriptor) throws DeploymentException {
        try {
            if (webBundleDescriptor != null) {
                if (webBundleDescriptor.isPolicyModified()) {
                    // redo policy translation for web module
                    loadPolicy(webBundleDescriptor, true);
                }
                String cid = SecurityUtil.getContextID(webBundleDescriptor);
                websecurityProbeProvider.policyCreationStartedEvent(cid);
                SecurityUtil.generatePolicyFile(cid);
                websecurityProbeProvider.policyCreationEndedEvent(cid);
                websecurityProbeProvider.policyCreationEvent(cid);

            }
        } catch (Exception se) {
            String msg = "Error in generating security policy for " + webBundleDescriptor.getModuleDescriptor().getModuleName();
            throw new DeploymentException(msg, se);
        }
    }

    /**
     * commits ejb policy contexts. This should occur in EjbApplication, being done here until issue with
     * ejb-ejb31-singleton-multimoduleApp.ear is resolved
     * 
     * @param ejbs
     */
    private void commitEjbs(Application app) throws DeploymentException {
        Set<EjbBundleDescriptor> ejbDescriptors = app.getBundleDescriptors(EjbBundleDescriptor.class);
        try {
            for (EjbBundleDescriptor ejbBD : ejbDescriptors) {
                String pcid = SecurityUtil.getContextID(ejbBD);
                ejbProbeProvider.policyCreationStartedEvent(pcid);
                SecurityUtil.generatePolicyFile(pcid);
                ejbProbeProvider.policyCreationEndedEvent(pcid);
                ejbProbeProvider.policyCreationEvent(pcid);

            }
        } catch (Exception se) {
            String msg = "Error in committing security policy for ejbs of " + app.getRegistrationName();
            throw new DeploymentException(msg, se);
        }
    }

    /**
     * Links the policy contexts of the application
     *
     * @param app
     * @param webBundleDescriptors
     */
    private void linkPolicies(Application app, Collection<WebBundleDescriptor> webBundleDescriptors) throws DeploymentException {
        try {

            String linkName = null;
            boolean lastInService = false;
            for (WebBundleDescriptor webBundleDescriptor : webBundleDescriptors) {
                String name = SecurityUtil.getContextID(webBundleDescriptor);
                lastInService = SecurityUtil.linkPolicyFile(name, linkName, lastInService);
                linkName = name;
            }

            linkName = null; // reset link name
            Set<EjbBundleDescriptor> ejbs = app.getBundleDescriptors(EjbBundleDescriptor.class);
            for (EjbBundleDescriptor ejbd : ejbs) {
                String name = SecurityUtil.getContextID(ejbd);
                lastInService = SecurityUtil.linkPolicyFile(name, linkName, lastInService);
                linkName = name;
            }
            // extra commit (see above)

        } catch (IASSecurityException se) {
            throw new DeploymentException( "Error in linking security policy for " + app.getRegistrationName(), se);
        }
    }

    private void removePolicy(DeploymentContext context) throws DeploymentException {
        OpsParams params = context.getCommandParameters(OpsParams.class);
        if (!params.origin.needsCleanArtifacts()) {
            return;
        }
        
        String appName = params.name();
        // Monitoring

        // Remove policy files only if managers are not destroyed by cleanup
        try {
            String[] webcontexts = webSecurityManagerFactory.getContextsForApp(appName, false);
            if (webcontexts != null) {
                for (int i = 0; i < webcontexts.length; i++) {
                    if (webcontexts[i] != null) {
                        websecurityProbeProvider.policyDestructionStartedEvent(webcontexts[i]);
                        SecurityUtil.removePolicy(webcontexts[i]);
                        websecurityProbeProvider.policyDestructionEndedEvent(webcontexts[i]);
                        websecurityProbeProvider.policyDestructionEvent(webcontexts[i]);
                    }
                }
            }
        } catch (IASSecurityException ex) {
            String msg = "Error in removing security policy for " + appName;
            _logger.log(WARNING, msg, ex);
            throw new DeploymentException(msg, ex);
        }

        // Destroy the managers if present
        cleanSecurityContext(appName);
    }

    /**
     * Clean security policy generated at deployment time. NOTE: This routine calls destroy on the WebSecurityManagers, but
     * that does not cause deletion of the underlying policy (files). The underlying policy is deleted when removePolicy (in
     * AppDeployerBase and WebModuleDeployer) is called.
     * 
     * @param appName the app name
     */
    private boolean cleanSecurityContext(String appName) {
        boolean cleanUpDone = false;
        
        List<JaccWebAuthorizationManager> managers = webSecurityManagerFactory.getManagersForApp(appName, false);
        if (managers == null) {
            return false;
        }
        
        for (JaccWebAuthorizationManager manager : managers) {
            try {
                websecurityProbeProvider.securityManagerDestructionStartedEvent(appName);
                manager.destroy();
                websecurityProbeProvider.securityManagerDestructionEndedEvent(appName);
                websecurityProbeProvider.securityManagerDestructionEvent(appName);
                
                cleanUpDone = true;
            } catch (Exception pce) {
                // Log it and continue
                _logger.log(WARNING, "Unable to destroy WebSecurityManager", pce);
            }

        }
        
        return cleanUpDone;
    }

    public static List<EventTypes> getDeploymentEvents() {
        List<EventTypes> events = new ArrayList<EventTypes>();
        events.add(APPLICATION_PREPARED);
        
        return events;
    }

    private boolean isHaEnabled() {
        boolean haEnabled = false;
        // lazily init the required services instead of
        // eagerly injecting them.
        synchronized (this) {
            if (haUtil == null) {
                haUtil = haUtilProvider.get();
            }
        }

        if (haUtil != null && haUtil.isHAEnabled()) {
            haEnabled = true;
            synchronized (this) {
                if (appCnonceMap == null) {
                    appCnonceMap = appCNonceCacheMapProvider.get();
                }
                if (cnonceCacheFactory == null) {
                    cnonceCacheFactory = cNonceCacheFactoryProvider.get();
                }
            }
        }

        return haEnabled;
    }

    private void handleCNonceCacheBSInit(String appName, Set<WebBundleDescriptor> webDesc, boolean isHA) {
        boolean hasDigest = false;
        for (WebBundleDescriptor webBD : webDesc) {
            LoginConfiguration lc = webBD.getLoginConfiguration();
            if (lc != null && LoginConfiguration.DIGEST_AUTHENTICATION.equals(lc.getAuthenticationMethod())) {
                hasDigest = true;
                break;
            }
        }
        if (!hasDigest) {
            return;
        }
        // initialize the backing stores as well for cnonce cache.
        if (isHaEnabled() && isHA) {
            final String clusterName = haUtil.getClusterName();
            final String instanceName = haUtil.getInstanceName();
            if (cnonceCacheFactory != null) {
                CNonceCache cache = cnonceCacheFactory.createCNonceCache(appName, clusterName, instanceName, HA_CNONCE_BS_NAME);
                this.appCnonceMap.put(appName, cache);
            }

        }
    }
}
