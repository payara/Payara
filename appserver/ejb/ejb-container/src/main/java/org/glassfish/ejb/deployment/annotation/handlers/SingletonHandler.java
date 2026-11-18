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
// Portions Copyright [2016-2026] Payara Foundation and/or its affiliates
package org.glassfish.ejb.deployment.annotation.handlers;

import fish.payara.cluster.Clustered;
import jakarta.ejb.DependsOn;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import org.glassfish.apf.AnnotationHandlerFor;
import org.glassfish.apf.AnnotationInfo;
import org.glassfish.apf.AnnotationProcessorException;
import org.glassfish.apf.AnnotationValues;
import org.glassfish.apf.HandlerProcessingResult;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbSessionDescriptor;
import org.glassfish.internal.deployment.ClassHierarchy;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;
import org.jvnet.hk2.annotations.Service;

/**
 * This handler is responsible for handling the jakarta.ejb.Singleton
 *
 * @author Shing Wai Chan
 */
@Service
@AnnotationHandlerFor(Singleton.class)
public class SingletonHandler extends AbstractEjbHandler {

    public SingletonHandler() {}

    /**
     * Return the name attribute of given annotation.
     * @param ainfo the annotation
     * @return name
     */
    @Override
    protected String getAnnotatedName(AnnotationInfo ainfo) {
        return ainfo.stringValue("name");
    }

    /**
     * Check if the given EjbDescriptor matches the given Annotation.
     * @param ejbDesc
     * @param ainfo
     * @return boolean check for validity of EjbDescriptor
     */
    @Override
    protected boolean isValidEjbDescriptor(EjbDescriptor ejbDesc, AnnotationInfo ainfo) {
        boolean isValid = EjbSessionDescriptor.TYPE.equals(ejbDesc.getType());

        if( isValid ) {
            EjbSessionDescriptor sessionDesc = (EjbSessionDescriptor) ejbDesc;
            if( sessionDesc.isSessionTypeSet() && !sessionDesc.isSingleton() ) {
                isValid = false;
            }
        }

        return  isValid;
    }

    /**
     * Create a new EjbDescriptor for a given elementName and AnnotationInfo.
     * @param elementName
     * @param ainfo
     * @return a new EjbDescriptor
     */
    @Override
    protected EjbDescriptor createEjbDescriptor(String elementName, AnnotationInfo ainfo)
            throws AnnotationProcessorException {

        ClassInfo ejbClass = ainfo.getAnnotatedElement().asClass();
        EjbSessionDescriptor newDescriptor = new EjbSessionDescriptor();
        newDescriptor.setName(elementName);
        newDescriptor.setEjbClassName(ejbClass.name().toString());
        newDescriptor.setSessionType(EjbSessionDescriptor.SINGLETON);

        doSingletonSpecificProcessing(newDescriptor, ejbClass, ainfo.getIndex());

        return newDescriptor;
    }

    /**
     * Set Annotation information to Descriptor.
     * This method will also be invoked for an existing descriptor with
     * annotation as user may not specific a complete xml.
     * @param ejbDesc
     * @param ainfo
     * @return HandlerProcessingResult
     */
    @Override
    protected HandlerProcessingResult setEjbDescriptorInfo(EjbDescriptor ejbDesc, AnnotationInfo ainfo)
            throws AnnotationProcessorException {

        EjbSessionDescriptor ejbSingletonDescriptor = (EjbSessionDescriptor) ejbDesc;

        ClassInfo ejbClass = ainfo.getAnnotatedElement().asClass();

        // set session bean type in case it wasn't set in a sparse ejb-jar.xml.
        if( !ejbSingletonDescriptor.isSessionTypeSet() ) {
            ejbSingletonDescriptor.setSessionType(EjbSessionDescriptor.SINGLETON);
        }

        doDescriptionProcessing(ainfo.stringValue("description"), ejbDesc);
        doMappedNameProcessing(ainfo.stringValue("mappedName"), ejbDesc);

        doSingletonSpecificProcessing(ejbSingletonDescriptor, ejbClass, ainfo.getIndex());

        return setBusinessAndHomeInterfaces(ejbDesc, ainfo);
    }

    private void doSingletonSpecificProcessing(EjbSessionDescriptor desc, ClassInfo ejbClass, IndexView index) {

        if (ClassHierarchy.annotation(index, ejbClass, Startup.class) != null) {
            desc.setInitOnStartupIfNotAlreadySet(true);
        }

        AnnotationInstance dep = ClassHierarchy.annotation(index, ejbClass, DependsOn.class);
        if (dep != null) {
            desc.setDependsOnIfNotSet(AnnotationValues.strings(index, dep, "value"));
        }

        AnnotationInstance clusteredAnnotation = ClassHierarchy.annotation(index, ejbClass, Clustered.class);
        if(clusteredAnnotation != null) {
            desc.setClustered(true);
            desc.setClusteredKeyValue(AnnotationValues.string(index, clusteredAnnotation, "keyName"));
            desc.setDontCallPostConstructOnAttach(!AnnotationValues.bool(index, clusteredAnnotation, "callPostConstructOnAttach"));
            desc.setDontCallPreDestroyOnDetach(!AnnotationValues.bool(index, clusteredAnnotation, "callPreDestroyOnDetach"));
        }
    }
}
