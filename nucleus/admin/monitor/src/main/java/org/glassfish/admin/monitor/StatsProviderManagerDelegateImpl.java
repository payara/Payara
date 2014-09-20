/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.glassfish.admin.monitor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.*;
import javax.management.ObjectName;


import org.glassfish.api.monitoring.ContainerMonitoring;
import org.glassfish.flashlight.datatree.TreeNode;
import org.glassfish.flashlight.datatree.factory.TreeNodeFactory;
import org.glassfish.gmbal.AMXMetadata;
import org.glassfish.gmbal.ManagedObjectManager;
import org.glassfish.gmbal.ManagedObjectManagerFactory;
import org.glassfish.gmbal.ManagedAttribute;
import org.glassfish.external.probe.provider.PluginPoint;
import org.glassfish.external.probe.provider.StatsProviderManagerDelegate;
import org.glassfish.external.statistics.Statistic;
import org.glassfish.external.probe.provider.StatsProviderInfo;
import org.glassfish.external.statistics.annotations.Reset;
import org.glassfish.external.statistics.impl.StatisticImpl;
import org.glassfish.external.statistics.impl.StatsImpl;
import org.glassfish.flashlight.MonitoringRuntimeDataRegistry;
import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.logging.LogDomains;
import com.sun.enterprise.util.StringUtils;
import java.io.IOException;
import java.text.MessageFormat;

import org.glassfish.flashlight.client.ProbeClientMediator;
import org.glassfish.flashlight.client.ProbeClientMethodHandle;

import javax.inject.Singleton;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.TransactionFailure;
import java.beans.PropertyVetoException;
import org.glassfish.admin.monitor.StatsProviderRegistry.StatsProviderRegistryElement;

import org.glassfish.external.amx.MBeanListener;
import org.glassfish.external.amx.AMXGlassfish;
import static org.glassfish.external.amx.AMX.*;

import org.glassfish.flashlight.provider.FlashlightProbe;
import org.glassfish.flashlight.provider.ProbeRegistry;
import static org.glassfish.admin.monitor.MLogger.*;

/**
 *
 * @author Jennifer
 */
@Singleton
public class StatsProviderManagerDelegateImpl extends MBeanListener.CallbackImpl implements StatsProviderManagerDelegate {

    protected ProbeClientMediator pcm;
    MonitoringService monitoringService = null;
    private final MonitoringRuntimeDataRegistry mrdr;
    private final ProbeRegistry probeRegistry;
    private final Domain domain;
    private final String instanceName;
    private final TreeNode serverNode;
    private static final ObjectName MONITORING_ROOT = AMXGlassfish.DEFAULT.monitoringRoot();
    private ObjectName MONITORING_SERVER;
    private String DOMAIN;
    private String PP;
    private String TYPE;
    private String NAME;
    private String PARENT_PATH;
    private boolean AMXReady = false;
    private StatsProviderRegistry statsProviderRegistry;
    private static final Logger logger = getLogger();
    private static final ResourceBundle rb = logger.getResourceBundle();
    public final static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(StatsProviderManagerDelegateImpl.class);
    boolean ddebug = false;

    StatsProviderManagerDelegateImpl(ProbeClientMediator pcm, ProbeRegistry probeRegistry,
            MonitoringRuntimeDataRegistry mrdr, Domain domain, String iName, MonitoringService monitoringService) {
        this.pcm = pcm;
        this.mrdr = mrdr;
        this.domain = domain;
        this.instanceName = iName;
        this.monitoringService = monitoringService;
        this.probeRegistry = probeRegistry;
        //serverNode is special, construct that first if doesn't exist
        serverNode = constructServerPP();
        statsProviderRegistry = new StatsProviderRegistry(mrdr);
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, " In the ctor : instance name " + instanceName);
            logger.log(Level.FINE, " In the ctor : MONITORING SERVER " + MONITORING_SERVER);
        }
        MONITORING_SERVER = AMXGlassfish.DEFAULT.serverMon(instanceName);
        DOMAIN = MONITORING_SERVER.getDomain();
        PP = MONITORING_SERVER.getKeyProperty(PARENT_PATH_KEY);
        TYPE = MONITORING_SERVER.getKeyProperty(TYPE_KEY);
        NAME = MONITORING_SERVER.getKeyProperty(NAME_KEY);
        PARENT_PATH = PP + "/" + TYPE + "[" + NAME + "]";
    }

    public void register(String configElement, PluginPoint pp,
            String subTreePath, Object statsProvider) {
        register(configElement, pp, subTreePath, statsProvider, null);
    }

    public void register(String configElement, PluginPoint pp,
            String subTreePath, Object statsProvider, String invokerId) {
        StatsProviderInfo spInfo = new StatsProviderInfo(configElement, pp, subTreePath, statsProvider, invokerId);
        register(spInfo);
    }

    public void register(StatsProviderInfo spInfo) {
        try {
            tryToRegister(spInfo);
        }
        catch (RuntimeException rte) {
            logger.log(Level.WARNING, ListenerRegistrationFailed,
                    new Object[]{spInfo.getStatsProvider().getClass().getName()});
            if (logger.isLoggable(Level.FINE))
                logger.log(Level.FINE, "Listener registration failed", rte);
            FutureStatsProviders.add(spInfo);
        }
    }


    /* throws RuntimeException maybe
     * note the default visibility so that MonitoringBootstrap can call it.
     */
    void tryToRegister(StatsProviderInfo spInfo) {
        final String configElement = spInfo.getConfigElement();
        Object statsProvider = spInfo.getStatsProvider();
        // register the statsProvider
        if (logger.isLoggable(Level.FINE))
            logger.fine("registering a statsProvider");
        StatsProviderRegistryElement spre;
        // If configElement is null, create it
        if (monitoringService != null
                && monitoringService.getContainerMonitoring(configElement) == null
                && monitoringService.getMonitoringLevel(configElement) == null) {
            createConfigElement(configElement);
        }

        // First check if the configElement associated for statsProvider is 'ON'
        if (getMonitoringEnabled() && getEnabledValue(configElement)) {
            if (logger.isLoggable(Level.FINE))
                logger.fine(" enabled is true ");
            spre = statsProviderRegistry.getStatsProviderRegistryElement(statsProvider);

            if (spre == null) {
                statsProviderRegistry.registerStatsProvider(spInfo);
                spre = statsProviderRegistry.getStatsProviderRegistryElement(statsProvider);
            }
            //Enable the StatsProvider if the enable is allowed
            if (spre.isEnableAllowed(getMonitoringLevel(configElement))) {
                enableStatsProvider(spre);
            }

        }
        else {
            if (logger.isLoggable(Level.FINE))
                logger.fine(" enabled is false ");
            // Register with null values so to know that we need to register them individually and config is on
            statsProviderRegistry.registerStatsProvider(spInfo);
            spre = statsProviderRegistry.getStatsProviderRegistryElement(statsProvider);
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.fine(spre.toString());
            logger.fine("=========================================================");
        }
    }

    private void createConfigElement(final String configElement) {
        try {
            ConfigSupport.apply(new SingleConfigCode<MonitoringService>() {
                public Object run(MonitoringService param)
                        throws PropertyVetoException, TransactionFailure {
                    ContainerMonitoring newItem = param.createChild(ContainerMonitoring.class);
                    newItem.setName(configElement);
                    param.getContainerMonitoring().add(newItem);
                    return newItem;
                }
            }, monitoringService);
        }
        catch (TransactionFailure tf) {
            String msg = localStrings.getLocalString(cannotCreateConfigElement,
                    "Unable to create container-monitoring for", configElement);
            logger.log(Level.SEVERE, msg, tf);
        }
    }

    public void unregister(Object statsProvider) {
        // Unregisters the statsProvider
        try {
            StatsProviderRegistryElement spre = statsProviderRegistry.getStatsProviderRegistryElement(statsProvider);
            if (spre == null) {
                logger.log(Level.INFO, invalidStatsProvider,
                        new Object[]{statsProvider.getClass().getName()});
                return;
            }

            // get the Parent node and delete all children nodes (only that we know of)
            String parentNodePath = spre.getParentTreeNodePath();
            List<String> childNodeNames = spre.getChildTreeNodeNames();
            TreeNode rootNode = mrdr.get(instanceName);
            if ((rootNode != null) && (parentNodePath != null)) {
                // This has to return one node
                List<TreeNode> nodeList = rootNode.getNodes(parentNodePath, false, true);
                if (nodeList.size() > 0) {
                    TreeNode parentNode = nodeList.get(0);
                    //Remove each of the child nodes
                    Collection<TreeNode> childNodes = parentNode.getChildNodes();
                    for (TreeNode childNode : childNodes) {
                        if (childNodeNames.contains(childNode.getName())) {
                            parentNode.removeChild(childNode);
                        }
                    }
                    if (!parentNode.hasChildNodes())
                        removeParentNode(parentNode);
                }
            }

            //get the handles and unregister the listeners from Flashlight
            Collection<ProbeClientMethodHandle> handles = spre.getHandles();
            if (handles != null) {
                for (ProbeClientMethodHandle handle : handles) {
                    // handle.remove????? Mahesh?
                    //TODO IMPLEMENTATION
                    //For now disable the handle => remove the client from invokerlist
                    handle.disable();
                }
            }

            //unregister the statsProvider from Gmbal
            if (spre.getManagedObjectManager() != null)
                unregisterGmbal(spre);

            //Unregister from the MonitoringDataTreeRegistry and the map entries
            statsProviderRegistry.unregisterStatsProvider(statsProvider);
        }
        catch (Exception ex) {
            String msg = MessageFormat.format(errorUnregisteringStatsProvider,
                    statsProvider.getClass().getName());
            logger.log(Level.SEVERE, msg, ex);
        }

    }

    private void removeParentNode(TreeNode parentNode) {
        TreeNode superParentNode = parentNode.getParent();
        if (superParentNode != null) {
            superParentNode.removeChild(parentNode);
            if (!superParentNode.hasChildNodes())
                removeParentNode(superParentNode);
        }
    }

    /* called from SPMD, when monitoring-enabled flag is turned on */
    public void updateAllStatsProviders() {
        // Looks like the monitoring-enabled flag is just turned ON. Lets do the catchup
        for (String configElement : statsProviderRegistry.getConfigElementList()) {
            Collection<StatsProviderRegistryElement> spreList =
                    statsProviderRegistry.getStatsProviderRegistryElement(configElement);
            boolean isConfigEnabled = getEnabledValue(configElement);
            //Continue with the next configElement if this is not enabled
            if (!isConfigEnabled)
                continue;

            for (StatsProviderRegistryElement spre : spreList) {
                //Assuming the spre's are disabled to start with
                boolean isEnableAllowed = spre.isEnableAllowed(getMonitoringLevel(configElement));
                if (isEnableAllowed) {
                    enableStatsProvider(spre);
                }
            }
        }
    }

    /* called from SPMD, when monitoring-enabled flag is turned off */
    public void disableAllStatsProviders() {
        // Looks like the monitoring-enabled flag is just turned OFF. Disable all the statsProviders which were on
        for (String configElement : statsProviderRegistry.getConfigElementList()) {
            Collection<StatsProviderRegistryElement> spreList =
                    statsProviderRegistry.getStatsProviderRegistryElement(configElement);
            for (StatsProviderRegistryElement spre : spreList) {
                if (spre.isEnabled) {
                    disableStatsProvider(spre);
                }
            }
        }
    }

    /* called from SMPD, when monitoring level for a module is turned on */
    public void enableStatsProviders(String configElement) {
        //If monitoring-enabled is false, just return
        if (!getMonitoringEnabled())
            return;
        String configLevel = getMonitoringLevel(configElement);
        //Enable all the StatsProviders for a given configElement
        if (logger.isLoggable(Level.FINE))
            logger.fine("Enabling all the statsProviders for - " + configElement);
        List<StatsProviderRegistryElement> spreList = statsProviderRegistry.getStatsProviderRegistryElement(configElement);
        if (spreList == null)
            return;
        for (StatsProviderRegistryElement spre : spreList) {
            //Check to see if the enable is allowed
            // Not allowed if statsProvider is registered for Low and configLevel is HIGH
            boolean isEnableAllowed = spre.isEnableAllowed(configLevel);
            if (!spre.isEnabled()) {
                //OFF->LOW, OFF->HIGH
                if (isEnableAllowed) {
                    enableStatsProvider(spre);
                }
            }
            else {
                //Disable if the stats were enabled, but current level is not allowed for these stats(HIGH->LOW) and
                // stats were registered at HIGH
                if (!isEnableAllowed) {
                    disableStatsProvider(spre);
                }// else, Dont do anything LOW->HIGH (stats were registered at LOW)
            }
        }
    }

    /* called from SMPD, when monitoring level for a module is turned off */
    public void disableStatsProviders(String configElement) {
        // I think we should still disable even when monitoring-enabled is false
        /*
         //If monitoring-enabled is false, just return
         if (!getMonitoringEnabled())
         return;
         */
        //Disable all the StatsProviders for a given configElement
        if (logger.isLoggable(Level.FINE))
            logger.fine("Disabling all the statsProviders for - " + configElement);
        List<StatsProviderRegistryElement> spreList = statsProviderRegistry.getStatsProviderRegistryElement(configElement);
        if (spreList == null)
            return;
        for (StatsProviderRegistryElement spre : spreList) {
            if (spre.isEnabled())
                disableStatsProvider(spre);
        }
    }

    public void setHandlesForStatsProviders(Object statsProvider, Collection<ProbeClientMethodHandle> handles) {
        // save the handles also against statsProvider so you can unregister when statsProvider is unregistered
        StatsProviderRegistryElement spre =
                this.statsProviderRegistry.getStatsProviderRegistryElement(statsProvider);
        spre.setHandles(handles);
    }

    private boolean getMonitoringEnabled() {
        return Boolean.parseBoolean(monitoringService.getMonitoringEnabled());
    }

    private void enableStatsProvider(StatsProviderRegistryElement spre) {
        Object statsProvider = spre.getStatsProvider();
        if (logger.isLoggable(Level.FINE))
            logger.fine("Enabling the statsProvider - " + statsProvider.getClass().getName());

        /* Step 1. Create the tree for the statsProvider */
        // Check if we already have TreeNodes created
        if (spre.getParentTreeNodePath() == null) {
            /* Verify if PluginPoint exists, create one if it doesn't */
            PluginPoint pp = spre.getPluginPoint();
            String subTreePath = spre.getSubTreePath();

            TreeNode ppNode = getPluginPointNode(pp, serverNode);
            TreeNode parentNode = createSubTree(ppNode, subTreePath);
            List<String> childNodeNames = createTreeForStatsProvider(parentNode, statsProvider);
            spre.setParentTreeNodePath(parentNode.getCompletePathName());
            spre.setChildNodeNames(childNodeNames);
        }
        else {
            updateTreeNodes(spre, true);
        }

        /* Step 2. reset statistics (OFF --> LOW, OFF --> HIGH)*/
        resetStatistics(spre);

        /* Step 3. register the StatsProvider to the flashlight */
        if (spre.getHandles() == null) {
            // register with flashlight and save the handles
            Collection<ProbeClientMethodHandle> handles = registerStatsProviderToFlashlight(statsProvider);
            spre.setHandles(handles);
        }
        else {
            //Enable the Flashlight handles for this statsProvider
            for (ProbeClientMethodHandle handle : spre.getHandles()) {
                if (!handle.isEnabled())
                    handle.enable();
            }
        }

        /* Step 4. gmbal registration */
        if (AMXReady && getMbeanEnabledValue()) {
            //Create mom root using the statsProvider
            String subTreePath = spre.getSubTreePath();
            ManagedObjectManager mom = spre.getManagedObjectManager();
            if (mom == null) {
                mom = registerGmbal(statsProvider, subTreePath);
                spre.setManagedObjectManager(mom);
            }
        }

        spre.setEnabled(true);
    }

    private void disableStatsProvider(StatsProviderRegistryElement spre) {
        if (logger.isLoggable(Level.FINE))
            logger.fine("Disabling the statsProvider - " + spre.getStatsProvider().getClass().getName());
        /* Step 1. Disable the tree nodes for StatsProvider */
        updateTreeNodes(spre, false);

        /* Step 2. Disable flashlight handles (Ideally unregister them) */
        for (ProbeClientMethodHandle handle : spre.getHandles()) {
            if (handle.isEnabled())
                handle.disable();
        }

        /* Step 3. Unregister gmbal */
        unregisterGmbal(spre);

        spre.setEnabled(false);
    }

    public void registerAllGmbal() {
        /* We do this when the mbean-enabled is turned on from off */

        if (logger.isLoggable(Level.FINE))
            logger.fine("Registering all the statsProviders whose enabled flag is 'on' with Gmbal");
        for (StatsProviderRegistryElement spre : statsProviderRegistry.getSpreList()) {
            if (spre.isEnabled()) {
                ManagedObjectManager mom = spre.getManagedObjectManager();
                if (mom == null) {
                    mom = registerGmbal(spre.getStatsProvider(), spre.getMBeanName());
                    spre.setManagedObjectManager(mom);
                }
            }
        }
    }

    public void unregisterAllGmbal() {
        /* We do this when the mbean-enabled is turned off from on */

        if (logger.isLoggable(Level.FINE))
            logger.fine("Unregistering all the statsProviders whose enabled flag is 'off' with Gmbal");
        for (StatsProviderRegistryElement spre : statsProviderRegistry.getSpreList()) {
            if (spre.isEnabled()) {
                unregisterGmbal(spre);
            }
        }
    }

    private void updateTreeNodes(StatsProviderRegistryElement spre, boolean enable) {
        //Enable/Disable the child TreeNodes
        String parentNodePath = spre.getParentTreeNodePath();
        List<String> childNodeNames = spre.getChildTreeNodeNames();
        TreeNode rootNode = mrdr.get(instanceName);
        if (rootNode != null) {
            // This has to return one node
            List<TreeNode> nodeList = rootNode.getNodes(parentNodePath, false, true);
            TreeNode parentNode = nodeList.get(0);
            //For each child Node, enable it
            Collection<TreeNode> childNodes = parentNode.getChildNodes();
            boolean hasUpdatedNode = false;
            for (TreeNode childNode : childNodes) {
                if (childNodeNames.contains(childNode.getName())) {
                    //Enabling or Disabling the child node (based on enable flag)
                    if (childNode.isEnabled() != enable) {
                        childNode.setEnabled(enable);
                        hasUpdatedNode = true;
                    }
                }
            }
            if (!hasUpdatedNode)
                return;
            //Make sure the tree path is affected with the changes.
            if (enable) {
                enableTreeNode(parentNode);
            }
            else {
                disableTreeNode(parentNode);
            }
        }

    }

    private void enableTreeNode(TreeNode treeNode) {
        if (!treeNode.isEnabled()) {
            treeNode.setEnabled(true);
            // recursevely call the enable on parent nodes, until the whole path is enabled
            if (treeNode.getParent() != null) {
                enableTreeNode(treeNode.getParent());
            }
        }
    }

    private void disableTreeNode(TreeNode treeNode) {
        if (treeNode.isEnabled()) {
            boolean isAnyChildEnabled = false;
            Collection<TreeNode> childNodes = treeNode.getChildNodes();
            if (childNodes != null) {
                for (TreeNode childNode : childNodes) {
                    if (childNode.isEnabled()) {
                        isAnyChildEnabled = true;
                        break;
                    }
                }
            }
            // if none of the childs are enabled, disable the parent
            if (!isAnyChildEnabled) {
                treeNode.setEnabled(false);
                // recursevely call the disable on parent nodes
                if (treeNode.getParent() != null) {
                    disableTreeNode(treeNode.getParent());
                }
            }
        }
    }

    //Invoke @Reset method on stats provider if available, otherwise call
    // reset() method on Statistic class
    private void resetStatistics(StatsProviderRegistryElement spre) {
        if (spre.getResetMethod() == null) {
            String parentNodePath = spre.getParentTreeNodePath();
            List<String> childNodeNames = spre.getChildTreeNodeNames();
            String statsProviderName = spre.getStatsProvider().getClass().getName();
            resetChildNodeStatistics(parentNodePath, childNodeNames, statsProviderName);
        }
        else {
            invokeStatsProviderResetMethod(spre.getResetMethod(), spre.getStatsProvider());
        }

    }

    private void resetChildNodeStatistics(String parentNodePath, List<String> childNodeNames, String statsProviderName) {
        TreeNode rootNode = mrdr.get(instanceName);
        if (rootNode != null) {
            List<TreeNode> nodeList = rootNode.getNodes(parentNodePath, false, true);
            if (nodeList.size() > 0) {
                TreeNode parentNode = nodeList.get(0);
                Collection<TreeNode> childNodes = parentNode.getChildNodes();
                for (TreeNode childNode : childNodes) {
                    if (childNodeNames.contains(childNode.getName())) {
                        invokeStatisticResetMethod(childNode.getValue());
                    }
                }
            }
            else {
                logger.log(Level.WARNING, nodeNotFound,
                        new Object[]{parentNodePath, statsProviderName});
            }
        }
    }

    private void invokeStatisticResetMethod(Object value) {
        if (value instanceof Statistic) {
            if (Proxy.isProxyClass(value.getClass())) {
                ((StatisticImpl) Proxy.getInvocationHandler(value)).reset();
            }
            else {
                ((StatisticImpl) value).reset();
            }
        }
        else if (value instanceof StatsImpl) {
            ((StatsImpl) value).reset();
        }
    }

    private void invokeStatsProviderResetMethod(Method m, Object statsProvider) {
        if (m != null) {
            Exception gotOne = null;
            try {
                m.invoke(statsProvider);
            }
            catch (IllegalAccessException ex) {
                gotOne = ex;
            }
            catch (IllegalArgumentException ex) {
                gotOne = ex;
            }
            catch (InvocationTargetException ex) {
                gotOne = ex;
            }
            if (gotOne != null) {
                String msg = MessageFormat.format(errorResettingStatsProvider,
                        statsProvider.getClass().getName());
                logger.log(Level.SEVERE, msg, gotOne);
            }

        }
    }

    private List<String> createTreeForStatsProvider(TreeNode parentNode, Object statsProvider) {
        /* construct monitoring tree at PluginPoint using subTreePath */
        List<String> childNodeNames = new ArrayList();

        /* retrieve ManagedAttribute attribute id (v2 compatible) and method names */
        /* Check for custom reset method and store for later to be called instead of
         standard reset methods on Statistic classes*/
        for (Method m : statsProvider.getClass().getMethods()) {
            ManagedAttribute ma = m.getAnnotation(ManagedAttribute.class);
            Reset resetMeth = m.getAnnotation(Reset.class);
            if (resetMeth != null) {
                StatsProviderRegistryElement spre = this.statsProviderRegistry.getStatsProviderRegistryElement(statsProvider);
                spre.setResetMethod(m);
            }
            if (ma != null) {
                String methodName = m.getName();
                String id = ma.id();
                if ((id == null) || id.isEmpty()) { // if id not specified, derive from method name
                    String methodNameLower = methodName.toLowerCase(Locale.ENGLISH);
                    if (methodNameLower.startsWith("get") && methodNameLower.length() > 3) {
                        id = methodNameLower.substring(3);
                    }
                }

                TreeNode attrNode = TreeNodeFactory.createMethodInvoker(id, statsProvider, id, m);
                parentNode.addChild(attrNode);
                childNodeNames.add(attrNode.getName());
            }
        }
        return childNodeNames;
    }

    private Collection<ProbeClientMethodHandle> registerStatsProviderToFlashlight(Object statsProvider) {
        //register the statsProvider with Flashlight
        Collection<ProbeClientMethodHandle> handles = null;
        //System.out.println("****** Registering the StatsProvider (" + statsProvider.getClass().getName() + ") with flashlight");
        StatsProviderRegistryElement spre =
                this.statsProviderRegistry.getStatsProviderRegistryElement(statsProvider);
        if (spre != null) {
            handles = pcm.registerListener(statsProvider, spre.getInvokerId());
        }
        else {
            handles = pcm.registerListener(statsProvider);
        }
        //System.out.println("********* handles = " + handles);
        // save the handles against config so you can enable/disable the handles
        // save the handles also against statsProvider so you can unregister when statsProvider is unregistered
        return handles;
    }

    // TODO TODO TODO
    // Here is where the slash meta-character is handled
    private TreeNode createSubTree(TreeNode parent, String subTreePath) {
        StringTokenizer st = new StringTokenizer(subTreePath, "/");
        TreeNode parentNode = parent;

        //enable the parent if not enabled
        enableTreeNode(parentNode);

        while (st.hasMoreTokens()) {
            TreeNode subTreeNode = createSubTreeNode(parentNode, st.nextToken());
            parentNode = subTreeNode;
        }
        return parentNode;
    }

    private TreeNode createSubTreeNode(TreeNode parent, String child) {
        TreeNode childNode = parent.getChild(child);
        if (childNode == null) {
            childNode = TreeNodeFactory.createTreeNode(child, null, child);
            parent.addChild(childNode);
        }
        else {
            // the childNode is found, but ensure that its enabled
            enableTreeNode(childNode);
        }
        return childNode;
    }

    public boolean hasListeners(String probeStr) {
        boolean hasListeners = false;
        FlashlightProbe probe = probeRegistry.getProbe(probeStr);
        if (probe != null)
            return probe.isEnabled();
        return hasListeners;
    }

    //Called when AMX DomainRoot is loaded (when jconsole or gui is started)
    //Register statsProviders with gmbal whose configElement is enabled
    //Save mom in the spre.  Used in unregister with gmbal later for config change to OFF or undeploy
    //Set AMXReady flag to true
    @Override
    public void mbeanRegistered(final ObjectName objectName, final MBeanListener listener) {
        super.mbeanRegistered(objectName, listener);
        AMXReady = true;
        statsProviderRegistry.setAMXReady(true);
        if (this.getMbeanEnabledValue()) {
            for (StatsProviderRegistry.StatsProviderRegistryElement spre : statsProviderRegistry.getSpreList()) {
                if (spre.isEnabled()) {
                    ManagedObjectManager mom = spre.getManagedObjectManager();
                    if (mom == null) {
                        mom = registerGmbal(spre.getStatsProvider(), spre.getMBeanName());
                        spre.setManagedObjectManager(mom);
                    }
                }
            }
        }
    }

    StatsProviderRegistry getStatsProviderRegistry() {
        return this.statsProviderRegistry;
    }

    private ManagedObjectManager registerGmbal(Object statsProvider, String mbeanName) {
        ManagedObjectManager mom = null;
        try {
            // 1 mom per statsProvider
            mom = ManagedObjectManagerFactory.createFederated(MONITORING_SERVER);
            if (mom != null) {
                mom.setJMXRegistrationDebug(false);
                if (mom.isManagedObject(statsProvider)) {
                    mom.stripPackagePrefix();
                    if (mbeanName != null && !mbeanName.isEmpty()) {
                        if (mbeanName.indexOf('\\') > 0) {
                            mbeanName = StringUtils.removeChar(mbeanName, '\\');
                        }
                        mbeanName = mbeanName.replaceAll(SystemPropertyConstants.SLASH, "/");
                        mom.createRoot(statsProvider, mbeanName);
                    }
                    else {
                        mom.createRoot(statsProvider);
                    }
                }
                else {
                    String spName = statsProvider.getClass().getName();
                    logger.log(Level.INFO, notaManagedObject, new Object[]{spName});
                }
            }
            //To register hierarchy in mom specify parent ManagedObject, and the ManagedObject itself
            //DynamicMBean mbean = (DynamicMBean)mom.register(parent, obj);
        }
        catch (Exception e) {
            //createRoot failed - need to return a null mom so we know not to unregister an mbean that does not exist
            mom = null;
            logger.log(Level.SEVERE, gmbalRegistrationFailed, e);
        }
        return mom;
    }

    private void unregisterGmbal(StatsProviderRegistryElement spre) {
        //unregister the statsProvider from Gmbal
        ManagedObjectManager mom = spre.getManagedObjectManager();
        if (mom != null) {
            mom.unregister(spre.getStatsProvider());
            try {
                mom.close();
            }
            catch (IOException ioe) {
                logger.log(Level.SEVERE, gmbalUnRegistrationFailed, ioe);
            }
            spre.setManagedObjectManager(null);
        }
    }

    private TreeNode getPluginPointNode(PluginPoint pp, TreeNode serverNode) {
        // Byron Nevins 12/17/2010
        // pp is over in GMBL.  It is an enum and there are 2 and only 2 possible values:
        // (1) "server", "server"
        // (2) "applications", "server/applications"
        // It is too risky & difficult to fix GMBL to support instances right now
        // so we deal with it, perfectly, below.

        if (pp == PluginPoint.APPLICATIONS)
            return createSubTree(serverNode, "applications");

        return serverNode;
    }

    private TreeNode constructServerPP() {
        TreeNode srvrNode = mrdr.get(instanceName);
        if (srvrNode != null) {
            return srvrNode;
        }

        srvrNode = TreeNodeFactory.createTreeNode(instanceName, null, instanceName);
        srvrNode.setEnabled(false);
        mrdr.add(instanceName, srvrNode);
        return srvrNode;
    }

    boolean getEnabledValue(String configElement) {
        boolean enabled = true;
        String level = getMonitoringLevel(configElement);
        if (level != null) {
            if (level.equals(ContainerMonitoring.LEVEL_OFF)) {
                enabled = false;
            }
        }
        else {
            logger.log(Level.WARNING, monitorElementDoesnotExist, new Object[]{configElement});
        }
        return enabled;
    }

    private String getMonitoringLevel(String configElement) {
        return monitoringService.getMonitoringLevel(configElement);
    }

    private boolean getMbeanEnabledValue() {
        return Boolean.parseBoolean(monitoringService.getMbeanEnabled());
    }

    public boolean isStatsProviderRegistered(Object statsProvider, String subTreePath) {
        boolean isStatsProviderRegistered = false;
        Collection<StatsProviderRegistry.StatsProviderRegistryElement> spreList = statsProviderRegistry.getSpreList();
        for (StatsProviderRegistry.StatsProviderRegistryElement spre : spreList) {
            if (spre.getStatsProvider().equals(statsProvider) && spre.getMBeanName().equals(subTreePath)) {
                isStatsProviderRegistered = true;
            }
        }
        return isStatsProviderRegistered;
    }

    public ObjectName getObjectName(Object statsProvider, String subTreePath) {
        String typeValue = getTypeValue(statsProvider);
        String nameValue = getNameValue(subTreePath);
        return AMXGlassfish.DEFAULT.newObjectName(PARENT_PATH, typeValue, nameValue);
    }

    public String getTypeValue(Object statsProvider) {
        String type = null;
        AMXMetadata am = statsProvider.getClass().getAnnotation(AMXMetadata.class);
        if (am != null) {
            type = am.type();
        }
        if (type == null) {
            type = statsProvider.getClass().getSimpleName();
        }
        return type;
    }

    public String getNameValue(String subTreePath) {
        return subTreePath;
    }
}
