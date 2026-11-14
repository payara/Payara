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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2026] Payara Foundation and/or its affiliates

package com.sun.enterprise.connectors.deployment.util;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.connectors.deployment.annotation.handlers.ConnectorAnnotationHandler;
import com.sun.enterprise.connectors.deployment.annotation.handlers.ConfigPropertyHandler;
import com.sun.enterprise.deployment.util.ConnectorVisitor;
import com.sun.enterprise.deployment.util.DefaultDOLVisitor;
import com.sun.logging.LogDomains;
import org.glassfish.apf.AnnotationInfo;
import org.glassfish.apf.AnnotationProcessorException;
import org.glassfish.internal.deployment.ClassHierarchy;
import org.glassfish.internal.deployment.ClassLoaderIndex;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;

import jakarta.resource.spi.ActivationSpec;
import java.util.Set;
import java.util.Iterator;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
@PerLookup
public class ConnectorValidator extends DefaultDOLVisitor implements ConnectorVisitor {

    private Logger _logger = LogDomains.getLogger(ConnectorValidator.class, LogDomains.RSR_LOGGER);

    public void accept (BundleDescriptor descriptor) {
        if (descriptor instanceof ConnectorDescriptor) {
            ConnectorDescriptor connectorDesc = (ConnectorDescriptor)descriptor;
            accept(connectorDesc);
        }
    }

    public void accept(ConnectorDescriptor descriptor) {

        // the classes of the resource adapter, read through its class loader without loading them
        IndexView index = new ClassLoaderIndex(ConfigPropertyHandler.classLoader(descriptor));

        //make sure that the ActivationSpec class implement ActivationSpec interface.
        validateActivationSpec(descriptor, index);

        //validate & process annotations if a valid connector annotation is not already processed
        if (!descriptor.getValidConnectorAnnotationProcessed()) {
            Set<AnnotationInfo> annotations = descriptor.getConnectorAnnotations();
            String raClass = descriptor.getResourceAdapterClass();

            if (annotations.size() == 0) {
                return;
            }

            //only one annotation is present
            if (annotations.size() == 1) {
                Iterator<AnnotationInfo> it = annotations.iterator();
                AnnotationInfo annotationInfo = it.next();
                ClassInfo claz = annotationInfo.getAnnotatedElement().asClass();
                String className = claz.name().toString();
                ConnectorAnnotationHandler.processDescriptor(claz, annotationInfo, descriptor);
                Collection<AnnotationInfo> configProperties = descriptor.getConfigPropertyAnnotations(className);
                if (configProperties != null) {
                    for (AnnotationInfo ai : configProperties) {
                        ConfigPropertyHandler handler = new ConfigPropertyHandler();
                        try {
                            handler.processAnnotation(ai);
                        } catch (AnnotationProcessorException e) {
                            RuntimeException re = new RuntimeException("Unable to process ConfigProperty " +
                                    "annotation in class ["+className+"] : " + e.getMessage());
                            re.initCause(e);
                            throw re;
                        }
                    }
                }
            } else {

                // if raClass is specified in the descriptor and multiple annotations not matching the raClass
                // are present, ignore them.
                if (raClass == null || raClass.equals("")) {
                    //all the cases below are unacceptable, fail deployment
                    if (annotations.size() > 1) {
                        throw new RuntimeException("cannot determine appropriate @Connector annotation as multiple " +
                                "annotations are present");
                    }
                }
            }
        }

        //check whether outbound is defined, if so, atleast one connection-definition must be present
        if(descriptor.getOutBoundDefined()){
            Set connectionDefinitions = descriptor.getOutboundResourceAdapter().getConnectionDefs();
            if(connectionDefinitions.size() == 0){
                throw new RuntimeException("Invalid connector descriptor for RAR [ "+descriptor.getName()+" ], when " +
                        "outbound-resource-adapter is specified," +
                        "atleast one connection-definition must be specified either via annotation or via descriptor");
            }
        }
        if (_logger.isLoggable(Level.FINEST)) {
            _logger.log(Level.FINEST, descriptor.toString());
        }

        processConfigProperties(descriptor, index);

        //processed all annotations, clear from book-keeping
        descriptor.getConnectorAnnotations().clear();
        descriptor.getAllConfigPropertyAnnotations().clear();
        descriptor.getConfigPropertyProcessedClasses().clear();
    }

    private void validateActivationSpec(ConnectorDescriptor descriptor, IndexView index) {
        if (descriptor.getInBoundDefined()) {
            InboundResourceAdapter ira = descriptor.getInboundResourceAdapter();
            Set messageListeners = ira.getMessageListeners();
            Iterator it = messageListeners.iterator();
            while (it.hasNext()) {
                MessageListener ml = (MessageListener) it.next();
                String activationSpecClass = ml.getActivationSpecClass();
                if (activationSpecClass != null && !activationSpecClass.equals("")) {
                    ClassInfo clazz = getClass(index, activationSpecClass);
                    boolean validClass = ClassHierarchy.isSubtype(index, clazz, ActivationSpec.class);
                    if(!validClass){
                        throw new IllegalArgumentException("Class ["+activationSpecClass+"] does not " +
                                "implement jakarta.resource.spi.ActivationSpec interface, but " +
                                "defined in MessageListener ["+ml.getMessageListenerType()+"] of RAR ["+ descriptor.getName() + "]");
                    }
                }else{
                    throw new RuntimeException("ActivationSpec class cannot be null or empty for message-listener" +
                            "["+ml.getMessageListenerType()+"] of RAR ["+descriptor.getName()+"]");
                }
            }
        }
    }

    /**
     * Process for ConfigProperty annotation for rar artifact classes where @ConfigProperty is
     * not defined in them, but their superclasses as we would have ignored
     * ConfigProperty annotations in non-concrete rar artifacts during
     * annotation processing phase
     * @param desc ConnectorDescriptor
     * @param index the classes of the resource adapter
     */
    private void processConfigProperties(ConnectorDescriptor desc, IndexView index)  {

        String raClass = desc.getResourceAdapterClass();
        if (raClass != null && !raClass.equals("")) {
            if (!desc.getConfigPropertyProcessedClasses().contains(raClass)) {
                ClassInfo claz = getClass(index, raClass);
                ConfigPropertyHandler.processParent(index, claz, desc.getConfigProperties(), desc);
            }
        }
        if (desc.getOutBoundDefined()) {
            OutboundResourceAdapter ora = desc.getOutboundResourceAdapter();
            Set connectionDefs = ora.getConnectionDefs();
            Iterator it = connectionDefs.iterator();
            while (it.hasNext()) {
                ConnectionDefDescriptor connectionDef = (ConnectionDefDescriptor) it.next();
                //connection-factory class is the unique identifier.
                String connectionFactoryClass = connectionDef.getConnectionFactoryIntf();
                if (connectionFactoryClass != null && !connectionFactoryClass.equals("")) {
                    if (!desc.getConfigPropertyProcessedClasses().contains(connectionFactoryClass)) {
                        ClassInfo claz = getClass(index, connectionDef.getManagedConnectionFactoryImpl());
                        ConfigPropertyHandler.processParent(index, claz, connectionDef.getConfigProperties(), desc);
                    }
                }
            }
        }

        if (desc.getInBoundDefined()) {
            InboundResourceAdapter ira = desc.getInboundResourceAdapter();
            Set messageListeners = ira.getMessageListeners();
            Iterator it = messageListeners.iterator();
            while (it.hasNext()) {
                MessageListener ml = (MessageListener) it.next();
                String activationSpecClass = ml.getActivationSpecClass();
                if (activationSpecClass != null && !activationSpecClass.equals("")) {
                    if (!desc.getConfigPropertyProcessedClasses().contains(activationSpecClass)) {
                        ClassInfo claz = getClass(index, activationSpecClass);
                        ConfigPropertyHandler.processParent(index, claz, ml.getConfigProperties(), desc);
                    }
                }
            }
        }

        Set adminObjects = desc.getAdminObjects();
        Iterator it = adminObjects.iterator();
        while (it.hasNext()) {
            AdminObject ao = (AdminObject) it.next();
            String uniqueName = ao.getAdminObjectInterface() + "_" + ao.getAdminObjectClass();
            if (!desc.getConfigPropertyProcessedClasses().contains(uniqueName)) {
                ClassInfo claz = getClass(index, ao.getAdminObjectClass());
                ConfigPropertyHandler.processParent(index, claz, ao.getConfigProperties(), desc);
            }
        }
    }

    private ClassInfo getClass(IndexView index, String className) {
        ClassInfo claz = index.getClassByName(DotName.createSimple(className));
        if (claz == null) {
            _logger.log(Level.WARNING, "Unable to load class [ " + className + " ]");
            throw new RuntimeException("Unable to load class [ " + className + " ]");
        }
        return claz;
    }
}
