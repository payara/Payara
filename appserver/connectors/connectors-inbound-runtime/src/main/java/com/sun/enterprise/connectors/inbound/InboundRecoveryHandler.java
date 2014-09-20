/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.connectors.inbound;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.resource.spi.ActivationSpec;
import javax.transaction.xa.XAResource;

import com.sun.appserv.connectors.internal.api.ConnectorConstants;
import com.sun.appserv.connectors.internal.api.ConnectorRuntime;
import com.sun.appserv.connectors.internal.api.ConnectorRuntimeException;
import com.sun.appserv.connectors.internal.api.ConnectorsUtil;
import com.sun.enterprise.config.serverbeans.Application;
import com.sun.enterprise.config.serverbeans.Applications;
import com.sun.enterprise.config.serverbeans.ConfigBeansUtilities;
import com.sun.enterprise.connectors.ConnectorRegistry;
import com.sun.enterprise.connectors.service.ConnectorAdminServiceUtils;
import com.sun.enterprise.connectors.util.RARUtils;
import com.sun.enterprise.connectors.util.ResourcesUtil;
import com.sun.enterprise.connectors.util.SetMethodAction;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.EjbBundleDescriptor;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.EjbMessageBeanDescriptor;
import com.sun.enterprise.transaction.spi.RecoveryResourceHandler;
import com.sun.logging.LogDomains;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ApplicationRegistry;
import org.jvnet.hk2.annotations.Service;


/**
 * Recovery handler for Inbound transactions
 *
 * @author Jagadish Ramu
 */
@Service
public class InboundRecoveryHandler implements RecoveryResourceHandler {

    @Inject
    private Applications deployedApplications;

    @Inject
    private ApplicationRegistry appsRegistry;

    @Inject
    private Provider<ConnectorRuntime> connectorRuntimeProvider;
    
    @Inject
    private ConfigBeansUtilities configBeansUtilities;
    

    private static Logger _logger = LogDomains.getLogger(InboundRecoveryHandler.class, LogDomains.RSR_LOGGER);

    /**
     * {@inheritDoc}
     */
    public void loadXAResourcesAndItsConnections(List xaresList, List connList) {
        Vector<XAResource> xaResources = new Vector<XAResource>();
        recoverInboundTransactions(xaResources);
    }

    /**
     * {@inheritDoc}
     */
    public void closeConnections(List connList) {
        // do nothing
    }

    private void recoverInboundTransactions(List<XAResource> xaresList) {

        List<Application> applications = deployedApplications.getApplications();

        try {
            _logger.log(Level.INFO, "Recovery of Inbound Transactions started.");

            if (applications.size() == 0) {
                _logger.log(Level.FINE, "No applications deployed.");
                return;
            }
            // List of CMT enabled MDB descriptors on the application server instance.
            List<EjbDescriptor> xaEnabledMDBList = new ArrayList<EjbDescriptor>();

            //Done so as to initialize connectors-runtime before loading inbound active RA. need a better way ?
            ConnectorRuntime cr = connectorRuntimeProvider.get();

            for (Application application : applications) {
                Vector ejbDescVec = getEjbDescriptors(application, appsRegistry);
                for (int j = 0; j < ejbDescVec.size(); j++) {
                    EjbDescriptor desc = (EjbDescriptor) ejbDescVec.elementAt(j);
                    // If EjbDescriptor is an instance of a CMT enabled MDB descriptor,
                    // add it to the list of xaEnabledMDBList.
                    if (desc instanceof EjbMessageBeanDescriptor &&
                            desc.getTransactionType().
                                    equals(EjbDescriptor.CONTAINER_TRANSACTION_TYPE)) {
                        xaEnabledMDBList.add(desc);
                        _logger.log(Level.FINE, "Found a CMT MDB: "
                                + desc.getEjbClassName());
                    }
                }
            }

            if (xaEnabledMDBList.size() == 0) {
                _logger.log(Level.FINE, "Found no CMT MDBs in all applications");
                return;
            }

            ConnectorRegistry creg = ConnectorRegistry.getInstance();

            // for each RA (key in the map) get the list (value) of MDB Descriptors
            Map<String, List<EjbDescriptor>> mappings = createRAEjbMapping(xaEnabledMDBList);

            //For each RA
            for (Map.Entry entry : mappings.entrySet()) {

                String raMid = (String) entry.getKey();
                List<EjbDescriptor> respectiveDesc = mappings.get(raMid);

                try {
                    createActiveResourceAdapter(raMid);
                } catch (Exception ex) {
                    _logger.log(Level.SEVERE, "error.loading.connector.resources.during.recovery", raMid);
                    if (_logger.isLoggable(Level.FINE)) {
                        _logger.log(Level.FINE, ex.toString(), ex);
                    }
                }

                ActiveInboundResourceAdapter activeInboundRA = (ActiveInboundResourceAdapter) creg
                        .getActiveResourceAdapter(raMid);

                //assert activeInboundRA instanceof ActiveInboundResourceAdapter;

                boolean isSystemJmsRA = false;
                if (ConnectorsUtil.isJMSRA(activeInboundRA.getModuleName())) {
                    isSystemJmsRA = true;
                }

                javax.resource.spi.ResourceAdapter resourceAdapter = activeInboundRA
                        .getResourceAdapter();
                // activationSpecList represents the ActivationSpec[] that would be
                // sent to the getXAResources() method.
                ArrayList<ActivationSpec> activationSpecList = new ArrayList<ActivationSpec>();

                try {
                    for (int i = 0; i < respectiveDesc.size(); i++) {
                        try {
                            // Get a MessageBeanDescriptor from respectiveDesc ArrayList
                            EjbMessageBeanDescriptor descriptor =
                                    (EjbMessageBeanDescriptor) respectiveDesc.get(i);
                            // A descriptor using 1.3 System JMS RA style properties needs
                            // to be updated J2EE 1.4 style props.
                            if (isSystemJmsRA) {
                                //XXX: Find out the pool descriptor corres to MDB and update
                                //MDBRuntimeInfo with that.
                                activeInboundRA.updateMDBRuntimeInfo(descriptor, null);
                            }

                            // Get the ActivationConfig Properties from the MDB Descriptor
                            Set activationConfigProps =
                                    RARUtils.getMergedActivationConfigProperties(descriptor);
                            // get message listener type
                            String msgListenerType = descriptor.getMessageListenerType();

                            // start resource adapter and get ActivationSpec class for
                            // the given message listener type from the ConnectorRuntime

                            ActivationSpec aspec = (ActivationSpec) (Class.forName(
                                    cr.getActivationSpecClass(raMid,
                                            msgListenerType), false,
                                    resourceAdapter.getClass().getClassLoader()).newInstance());
                            aspec.setResourceAdapter(resourceAdapter);

                            // Populate ActivationSpec class with ActivationConfig properties
                            SetMethodAction sma =
                                    new SetMethodAction(aspec, activationConfigProps);
                            sma.run();
                            activationSpecList.add(aspec);
                        } catch (Exception e) {
                            _logger.log(Level.WARNING, "error.creating.activationspec", e.getMessage());
                            if (_logger.isLoggable(Level.FINE)) {
                                _logger.log(Level.FINE, e.toString(), e);
                            }
                        }
                    }

                    // Get XA resources from RA.

                    ActivationSpec[] activationSpecArray = activationSpecList.toArray(new ActivationSpec[activationSpecList.size()]);
                    XAResource[] xar = resourceAdapter.getXAResources(activationSpecArray);

                    // Add the resources to the xaresList which is used by the RecoveryManager
                    if(xar != null){
                        for (int p = 0; p < xar.length; p++) {
                            xaresList.add(xar[p]);
                        }
                    }
                    // Catch UnsupportedOperationException if a RA does not support XA
                    // which is fine.
                } catch (UnsupportedOperationException uoex) {
                    _logger.log(Level.FINE, uoex.getMessage());
                    // otherwise catch the unexpected exception
                } catch (Exception e) {
                    _logger.log(Level.SEVERE, "exception.during.inbound.resource.acqusition", e);
                }
            }
        } catch (Exception e) {
            _logger.log(Level.SEVERE,"exception.during.inbound.recovery", e);
        }

    }

    private Vector getEjbDescriptors(Application application, ApplicationRegistry appsRegistry) {
        Vector ejbDescriptors = new Vector();

        if(ResourcesUtil.createInstance().isEnabled(application)){
            ApplicationInfo appInfo = appsRegistry.get(application.getName());
            if(appInfo != null){
                com.sun.enterprise.deployment.Application app =
                        appInfo.getMetaData(com.sun.enterprise.deployment.Application.class);
                Set<BundleDescriptor> descriptors = app.getBundleDescriptors();
                for (BundleDescriptor descriptor : descriptors) {
                    if (descriptor instanceof EjbBundleDescriptor) {
                        EjbBundleDescriptor ejbBundleDescriptor = (EjbBundleDescriptor) descriptor;
                        Set<? extends EjbDescriptor> ejbDescriptorsSet = ejbBundleDescriptor.getEjbs();
                        for (EjbDescriptor ejbDescriptor : ejbDescriptorsSet) {
                            ejbDescriptors.add(ejbDescriptor);
                        }
                    }
                }
            }else{
                //application is enabled, but still not found in app-registry
                _logger.log(Level.WARNING, "application.not.started.skipping.recovery", application.getName());
            }
        }
        return ejbDescriptors;
    }

    private Map<String, List<EjbDescriptor>> createRAEjbMapping(List<EjbDescriptor> ejbDescriptors) {

        Map<String, List<EjbDescriptor>> map = new HashMap<String, List<EjbDescriptor>>();

        for (EjbDescriptor ejbDescriptor : ejbDescriptors) {
            List<EjbDescriptor> ejbmdbd = new ArrayList<EjbDescriptor>();
            String ramid =
                    ((EjbMessageBeanDescriptor) ejbDescriptor).getResourceAdapterMid();
            if ((ramid == null) || (ramid.equalsIgnoreCase(""))) {
                ramid = ConnectorConstants.DEFAULT_JMS_ADAPTER;
            }

            // If Hashtable contains the RAMid key, get the list of MDB descriptors
            // and add the current MDB Descriptor (list[i]) to the list and put the
            // pair back into hashtable.
            // Otherwise, add the RAMid and the current MDB Descriptor to the hashtable
            if (map.containsKey(ramid)) {
                ejbmdbd = map.get(ramid);
                map.remove(ramid);
            }

            ejbmdbd.add(ejbDescriptor);
            map.put(ramid, ejbmdbd);
        }
        return map;
    }

    private void createActiveResourceAdapter(String rarModuleName) throws ConnectorRuntimeException {

        ConnectorRuntime cr = connectorRuntimeProvider.get();
        ConnectorRegistry creg = ConnectorRegistry.getInstance();

        if (creg.isRegistered(rarModuleName))
            return;

        if (ConnectorAdminServiceUtils.isEmbeddedConnectorModule(rarModuleName)) {
            cr.createActiveResourceAdapterForEmbeddedRar(rarModuleName);
        } else {
            String moduleDir ;
            if (ConnectorsUtil.belongsToSystemRA(rarModuleName)) {
                moduleDir = ConnectorsUtil.getSystemModuleLocation(rarModuleName);
            }else{
                moduleDir = configBeansUtilities.getLocation(rarModuleName);
            }
            ClassLoader loader = cr.createConnectorClassLoader(moduleDir, null, rarModuleName);
            cr.createActiveResourceAdapter(moduleDir, rarModuleName, loader);
        }
    }

}
