/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.connectors.deployment.annotation.handlers;

import com.sun.enterprise.deployment.annotation.context.RarBundleContext;
import com.sun.enterprise.deployment.annotation.handlers.*;
import com.sun.enterprise.deployment.ConnectorDescriptor;
import com.sun.enterprise.deployment.AdminObject;
import com.sun.enterprise.util.LocalStringManagerImpl;

import javax.resource.spi.AdministeredObject;
import javax.resource.spi.ResourceAdapterAssociation;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.Externalizable;
import java.io.Serializable;

import org.glassfish.apf.*;
import org.glassfish.apf.impl.AnnotationUtils;
import org.glassfish.apf.impl.HandlerProcessingResultImpl;
import org.jvnet.hk2.annotations.Service;

/**
 * Jagadish Ramu
 */
@Service
@AnnotationHandlerFor(AdministeredObject.class)
public class AdministeredObjectHandler extends AbstractHandler {

    protected static final LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(AdministeredObjectHandler.class);
    
    public HandlerProcessingResult processAnnotation(AnnotationInfo element) throws AnnotationProcessorException {
        AnnotatedElementHandler aeHandler = element.getProcessingContext().getHandler();
        AdministeredObject adminObject = (AdministeredObject) element.getAnnotation();

        if (aeHandler instanceof RarBundleContext) {
            RarBundleContext rarContext = (RarBundleContext) aeHandler;
            ConnectorDescriptor desc = rarContext.getDescriptor();

            Class c = (Class) element.getAnnotatedElement();
            String adminObjectClassName = c.getName();

            Class[] adminObjectInterfaceClasses = adminObject.adminObjectInterfaces();
            //When "adminObjectInterfaces()" is specified, add one admin-object entry per interface
            if (adminObjectInterfaceClasses != null && adminObjectInterfaceClasses.length > 0) {
                for (Class adminObjectInterface : adminObjectInterfaceClasses) {
                    processAdminObjectInterface(adminObjectClassName, adminObjectInterface.getName(), desc);
                }
            } else {
                List<Class> interfacesList = deriveAdminObjectInterfacesFromHierarchy(c);

                if (interfacesList.size() == 1) {
                    Class intf = interfacesList.get(0);
                    String intfName = intf.getName();
                    processAdminObjectInterface(adminObjectClassName, intfName, desc);
                } else {
                    //TODO V3 this case is, multiple interfaces implemented, no "adminObjectInterfaces()" attribute defined,
                    // should we check the DD whether this Impl class is already specified in any of "admin-object" elements ?
                    // If present, return. If not present, throw exception ?
                }
            }
        } else {
            getFailureResult(element, "not a rar bundle context", true);
        }
        return getDefaultProcessedResult();
    }

    public static List<Class> deriveAdminObjectInterfacesFromHierarchy(Class c) {
        Class interfaces[] = c.getInterfaces();

        List<Class> interfacesList = new ArrayList<Class>(Arrays.asList(interfaces));
        interfacesList.remove(Serializable.class);
        interfacesList.remove(Externalizable.class);
        interfacesList.remove(ResourceAdapterAssociation.class);
        return interfacesList;
    }

    private void processAdminObjectInterface(String adminObjectClassName, String adminObjectInterfaceName,
                                             ConnectorDescriptor desc) {
        Set ddAdminObjects = desc.getAdminObjects();
        //merge DD and annotation values of admin-objects
        //merge involves simple union
        boolean ignore = false;
        for (Object o : ddAdminObjects) {
            AdminObject ddAdminObject = (AdminObject) o;
            if (ddAdminObject.getAdminObjectInterface().equals(adminObjectInterfaceName) &&
                    ddAdminObject.getAdminObjectClass().equals(adminObjectClassName)) {
                ignore = true;
                break;
            }
        }
        if (!ignore) {
            AdminObject ao = new AdminObject(adminObjectInterfaceName, adminObjectClassName);
            desc.addAdminObject(ao);
        }else{
            if(logger.isLoggable(Level.FINEST)){
                logger.log(Level.FINEST,"Ignoring administered object annotation " +
                        "[ "+adminObjectInterfaceName+"," + adminObjectClassName + "] as it is already defined ");
            }
        }
    }

    public Class<? extends Annotation>[] getTypeDependencies() {
        return null;
    }

    /**
     * @return a default processed result
     */
    protected HandlerProcessingResult getDefaultProcessedResult() {
        return HandlerProcessingResultImpl.getDefaultResult(
                getAnnotationType(), ResultType.PROCESSED);
    }

    private HandlerProcessingResultImpl getFailureResult(AnnotationInfo element, String message, boolean doLog) {
        HandlerProcessingResultImpl result = new HandlerProcessingResultImpl();
        result.addResult(getAnnotationType(), ResultType.FAILED);
        if (doLog) {
            Class c = (Class) element.getAnnotatedElement();
            String className = c.getName();
            Object args[] = new Object[]{
                element.getAnnotation(),
                className,
                message,
            };
            String localString = localStrings.getLocalString(
                    "enterprise.deployment.annotation.handlers.connectorannotationfailure",
                    "failed to handle annotation [ {0} ] on class [ {1} ], reason : {2}", args);
            logger.log(Level.WARNING, localString);
        }
        return result;
    }
}
