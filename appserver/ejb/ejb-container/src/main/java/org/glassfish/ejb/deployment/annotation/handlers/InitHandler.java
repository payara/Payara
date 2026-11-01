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
// Portions Copyright [2026] Payara Foundation and/or its affiliates
package org.glassfish.ejb.deployment.annotation.handlers;

import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.deployment.annotation.context.EjbContext;
import jakarta.ejb.Init;
import org.glassfish.apf.AnnotationHandlerFor;
import org.glassfish.apf.AnnotationInfo;
import org.glassfish.apf.AnnotationProcessorException;
import org.glassfish.apf.HandlerProcessingResult;
import org.glassfish.ejb.deployment.descriptor.EjbInitInfo;
import org.glassfish.ejb.deployment.descriptor.EjbSessionDescriptor;
import org.glassfish.internal.deployment.ClassHierarchy;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jvnet.hk2.annotations.Service;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.logging.Level;

/**
 * This handler is responsible for handling the jakarta.ejb.Init attribute
 */
@Service
@AnnotationHandlerFor(Init.class)
public class InitHandler extends AbstractAttributeHandler {

    public InitHandler() {
    }

    @Override
    protected HandlerProcessingResult processAnnotation(AnnotationInfo ainfo,
            EjbContext[] ejbContexts) throws AnnotationProcessorException {

        String adaptedCreateMethodName = ainfo.stringValue("value");
        IndexView index = ainfo.getIndex();

        for(EjbContext next : ejbContexts) {

            EjbSessionDescriptor sessionDescriptor = (EjbSessionDescriptor) next.getDescriptor();

            MethodInfo m = ainfo.getAnnotatedElement().asMethod();

            // Check for matches in Home and/or LocalHome interfaces.

            int numMatches = 0;

            try {
                if( sessionDescriptor.isRemoteInterfacesSupported() ) {
                    addInitMethod(index, sessionDescriptor, m, adaptedCreateMethodName, false);
                    numMatches++;
                }
            } catch(Exception e) {
            }

            try {
                if( sessionDescriptor.isLocalInterfacesSupported() ) {
                    addInitMethod(index, sessionDescriptor, m, adaptedCreateMethodName, true);
                    numMatches++;
                }
            } catch(Exception e) {
            }

            if( numMatches == 0 ) {
                log(Level.SEVERE, ainfo,
                    localStrings.getLocalString(
                    "enterprise.deployment.annotation.handlers.notmatchcreate",
                    "Unable to find matching Home create method for Init method {0} on bean {1}.",
                    new Object[] { m, sessionDescriptor.getName() }));
                return getDefaultFailedResult();
            }
        }

        return getDefaultProcessedResult();
    }

    private void addInitMethod(IndexView index, EjbSessionDescriptor descriptor, MethodInfo beanMethod,
                               String adaptedCreateMethodName, boolean local) throws Exception {

        String homeIntfName = local ? descriptor.getLocalHomeClassName() : descriptor.getHomeClassName();

        ClassInfo homeIntf = index.getClassByName(DotName.createSimple(homeIntfName));
        if (homeIntf == null) {
            throw new ClassNotFoundException(homeIntfName);
        }

        String[] beanParameterTypes = ClassHierarchy.javaParameterNames(beanMethod);
        MethodInfo createMethod = null;
        if( (adaptedCreateMethodName == null) || (adaptedCreateMethodName.equals("")) ) {
            // Can't make any assumptions about matching method name.  Look for
            // a method with the same parameter types.
            for(MethodInfo next : ClassHierarchy.publicMethods(index, homeIntf)) {
                if( next.name().startsWith("create") &&
                    Arrays.equals(ClassHierarchy.javaParameterNames(next), beanParameterTypes) ) {
                    createMethod = next;
                    break;
                }
            }
            if( createMethod == null ) {
                throw new NoSuchMethodException("No matching adapted home " +
                                                "method found for @Init " +
                                                " method " + beanMethod);
            }
        } else {
            // Find method with explicit name and matching parameter types.
            for(MethodInfo next : ClassHierarchy.publicMethods(index, homeIntf)) {
                if( next.name().equals(adaptedCreateMethodName) &&
                    Arrays.equals(ClassHierarchy.javaParameterNames(next), beanParameterTypes) ) {
                    createMethod = next;
                    break;
                }
            }
            if( createMethod == null ) {
                throw new NoSuchMethodException(homeIntfName + "." + adaptedCreateMethodName
                        + Arrays.toString(beanParameterTypes));
            }
        }

        MethodDescriptor beanMethodDescriptor = new MethodDescriptor(beanMethod, MethodDescriptor.EJB_BEAN);

        MethodDescriptor createMethodDescriptor =
            new MethodDescriptor(createMethod,
                                 ( local ?
                                   MethodDescriptor.EJB_HOME :
                                   MethodDescriptor.EJB_LOCALHOME ));

        EjbInitInfo initInfo = new EjbInitInfo();

        initInfo.setBeanMethod(beanMethodDescriptor);
        initInfo.setCreateMethod(createMethodDescriptor);

        descriptor.addInitMethod(initInfo);

    }

    /**
     * @return an array of annotation types this annotation handler would
     * require to be processed (if present) before it processes it's own
     * annotation type.
     */
    @Override
    public Class<? extends Annotation>[] getTypeDependencies() {
        return getEjbAnnotationTypes();
    }
}
