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

package org.glassfish.webservices.connector.annotation.handlers;

import com.sun.enterprise.deployment.annotation.context.WebBundleContext;
import com.sun.enterprise.deployment.annotation.context.WebComponentContext;
import org.glassfish.apf.AnnotatedElementHandler;
import org.glassfish.apf.AnnotationInfo;
import org.glassfish.apf.AnnotationValues;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.internal.deployment.ClassHierarchy;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

public final class WebServiceUtils {

    /** named rather than referenced, so that no EJB API is needed in an installation without ejb */
    private static final DotName STATELESS = DotName.createSimple("jakarta.ejb.Stateless");
    private static final DotName SINGLETON = DotName.createSimple("jakarta.ejb.Singleton");

    /**
     * @return the name of the session bean the class declares, defaulting to its simple name
     */
    static String getEjbName(IndexView index, ClassInfo annElem) {
        AnnotationInstance stateless = ClassHierarchy.annotation(index, annElem, STATELESS);
        AnnotationInstance singleton = ClassHierarchy.annotation(index, annElem, SINGLETON);
        String name;

        if (stateless != null && AnnotationValues.string(index, stateless, "name").length() > 0) {
            name = AnnotationValues.string(index, stateless, "name");
        } else if (singleton != null && AnnotationValues.string(index, singleton, "name").length() > 0) {
            name = AnnotationValues.string(index, singleton, "name");
        } else {
            name = ClassHierarchy.simpleName(annElem);
        }
        return name;
    }

    /**
     * @return whether the class is annotated with the EJB component annotation, which marks an EJB
     * web service packaged in a war
     */
    static boolean isStateless(IndexView index, AnnotationTarget annElem) {
        return ClassHierarchy.annotation(index, annElem, STATELESS) != null;
    }

    /**
     * If WEB-INF/sun-jaxws.xml exists and is not processed in EJB context , then it returns true.
     * @param annInfo
     * @return
     */
    static boolean isJaxwsRIDeployment(AnnotationInfo annInfo) {
        boolean riDeployment = false;
        AnnotatedElementHandler annCtx = annInfo.getProcessingContext().getHandler();
        try {
            ReadableArchive moduleArchive = annInfo.getProcessingContext().getArchive();
            if (moduleArchive != null && moduleArchive.exists("WEB-INF/sun-jaxws.xml")
                    && !ClassHierarchy.isInterface(annInfo.getAnnotatedElement().asClass())
                    && ( (annCtx instanceof WebBundleContext) || (annCtx instanceof WebComponentContext))) {
                riDeployment = true;
            }
        } catch (Exception e) {
            //continue, processing
        }
        return riDeployment;
    }

}
