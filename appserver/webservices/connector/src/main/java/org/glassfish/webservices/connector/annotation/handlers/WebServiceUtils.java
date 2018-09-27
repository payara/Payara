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
// Portions Copyright [2016-2017] [Payara Foundation and/or its affiliates]

package org.glassfish.webservices.connector.annotation.handlers;

import com.sun.enterprise.deployment.annotation.context.WebBundleContext;
import com.sun.enterprise.deployment.annotation.context.WebComponentContext;
import java.lang.reflect.AnnotatedElement;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Singleton;
import javax.ejb.Stateless;
import org.glassfish.apf.AnnotatedElementHandler;
import org.glassfish.apf.AnnotationInfo;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.webservices.connector.LogUtils;

public final class WebServiceUtils {

    private static final Logger logger = LogUtils.getLogger();
    
    static String getEjbName(AnnotatedElement annElem) {
        Stateless stateless = null;
        try {
            stateless = annElem.getAnnotation(javax.ejb.Stateless.class);
        } catch (Exception e) {
            if (logger.isLoggable(Level.FINE)) {
                //This can happen in the web.zip installation where there is no ejb
                //Just logging the error
                logger.log(Level.FINE, LogUtils.EXCEPTION_THROWN, e);
}
        }
        Singleton singleton = null;
        try {
            singleton = annElem.getAnnotation(javax.ejb.Singleton.class);
        } catch (Exception e) {
            if (logger.isLoggable(Level.FINE)) {
                //This can happen in the web.zip installation where there is no ejb
                //Just logging the error
                logger.log(Level.FINE, LogUtils.EXCEPTION_THROWN, e);
            }
        }
        String name;

        if ((stateless != null) && ((stateless).name() == null || stateless.name().length() > 0)) {
            name = stateless.name();
        } else if ((singleton != null) && ((singleton).name() == null || singleton.name().length() > 0)) {
            name = singleton.name();

        } else {
            name = ((Class) annElem).getSimpleName();
        }
        return name;
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
                    && !((Class)annInfo.getAnnotatedElement()).isInterface()
                    && ( (annCtx instanceof WebBundleContext) || (annCtx instanceof WebComponentContext))) {
                riDeployment = true;
            }
        } catch (Exception e) {
            //continue, processing
        }
        return riDeployment;
    }

}
