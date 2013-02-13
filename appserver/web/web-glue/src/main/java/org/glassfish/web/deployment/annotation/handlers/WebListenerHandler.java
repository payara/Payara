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

package org.glassfish.web.deployment.annotation.handlers;

import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.annotation.context.WebBundleContext;
import com.sun.enterprise.deployment.annotation.context.WebComponentContext;
import com.sun.enterprise.deployment.web.AppListenerDescriptor;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.apf.AnnotationHandlerFor;
import org.glassfish.apf.AnnotationInfo;
import org.glassfish.apf.AnnotationProcessorException;
import org.glassfish.apf.HandlerProcessingResult;
import org.glassfish.web.deployment.descriptor.AppListenerDescriptorImpl;
import org.jvnet.hk2.annotations.Service;

import javax.servlet.ServletContextListener;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionIdListener;
import javax.servlet.http.HttpSessionListener;
import java.lang.annotation.Annotation;
import java.util.logging.Level;

/**
 * This handler is responsible in handling
 * javax.servlet.annotation.WebListener.
 *
 * @author Shing Wai Chan
 */
@Service
@AnnotationHandlerFor(WebListener.class)
public class WebListenerHandler extends AbstractWebHandler {
    protected final static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(WebFilterHandler.class);

    public WebListenerHandler() {
    }

    @Override
    protected HandlerProcessingResult processAnnotation(AnnotationInfo ainfo,
            WebComponentContext[] webCompContexts)
            throws AnnotationProcessorException {

        return processAnnotation(ainfo,
                webCompContexts[0].getDescriptor().getWebBundleDescriptor());
    }

    @Override
    protected HandlerProcessingResult processAnnotation(
            AnnotationInfo ainfo, WebBundleContext webBundleContext)
            throws AnnotationProcessorException {

        return processAnnotation(ainfo, webBundleContext.getDescriptor());
    }

    private HandlerProcessingResult processAnnotation(
            AnnotationInfo ainfo, WebBundleDescriptor webBundleDesc)
            throws AnnotationProcessorException {

        Class listenerClass = (Class)ainfo.getAnnotatedElement();
        if (!(ServletContextListener.class.isAssignableFrom(listenerClass) ||
                ServletContextAttributeListener.class.isAssignableFrom(listenerClass) ||
                ServletRequestListener.class.isAssignableFrom(listenerClass) ||
                ServletRequestAttributeListener.class.isAssignableFrom(listenerClass) ||
                HttpSessionListener.class.isAssignableFrom(listenerClass) ||
                HttpSessionAttributeListener.class.isAssignableFrom(listenerClass) ||
                HttpSessionIdListener.class.isAssignableFrom(listenerClass))) {
            log(Level.SEVERE, ainfo,
                localStrings.getLocalString(
                "web.deployment.annotation.handlers.needtoimpllistenerinterface",
                "The Class {0} having annotation javax.servlet.annotation.WebListener need to implement one of the following interfaces: javax.servlet.ServletContextLisener, javax.servlet.ServletContextAttributeListener, javax.servlet.ServletRequestListener, javax.servletServletRequestAttributeListener, javax.servlet.http.HttpSessionListener, javax.servlet.http.HttpSessionAttributeListener, javax.servlet.http.HttpSessionIdListener.",
                listenerClass.getName()));
            return getDefaultFailedResult();
        }

        WebListener listenerAn = (WebListener)ainfo.getAnnotation();
        AppListenerDescriptor appListener =
            new AppListenerDescriptorImpl(listenerClass.getName());
        appListener.setDescription(listenerAn.value());
        webBundleDesc.addAppListenerDescriptor(appListener);
        return getDefaultProcessedResult();
    }
}
