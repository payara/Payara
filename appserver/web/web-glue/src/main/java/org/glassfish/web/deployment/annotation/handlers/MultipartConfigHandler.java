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

package org.glassfish.web.deployment.annotation.handlers;

import com.sun.enterprise.deployment.WebComponentDescriptor;
import com.sun.enterprise.deployment.annotation.context.WebBundleContext;
import com.sun.enterprise.deployment.annotation.context.WebComponentContext;
import org.glassfish.web.deployment.descriptor.MultipartConfigDescriptor;
import org.glassfish.apf.*;
import org.jvnet.hk2.annotations.Service;

import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import java.lang.annotation.Annotation;

/**
 * This handler is responsible in handling
 * javax.servlet.annotation.MultipartConfig.
 *
 * @author Shing Wai Chan
 */
@Service
@AnnotationHandlerFor(MultipartConfig.class)
public class MultipartConfigHandler extends AbstractWebHandler {
    public MultipartConfigHandler() {
    }

    protected HandlerProcessingResult processAnnotation(AnnotationInfo ainfo,
            WebComponentContext[] webCompContexts)
            throws AnnotationProcessorException {

        HandlerProcessingResult result = null;
        for (WebComponentContext webCompContext : webCompContexts) {
            result = processAnnotation(ainfo,
                    webCompContext.getDescriptor());
            if (result.getOverallResult() == ResultType.FAILED) {
                break;
            }
        }
        return result;
    }

    protected HandlerProcessingResult processAnnotation(
            AnnotationInfo ainfo, WebBundleContext webBundleContext)
            throws AnnotationProcessorException {

        // this is not a web component
        return getInvalidAnnotatedElementHandlerResult(webBundleContext, ainfo);
    }

    private HandlerProcessingResult processAnnotation(
            AnnotationInfo ainfo, WebComponentDescriptor webCompDesc)
            throws AnnotationProcessorException {

        MultipartConfig multipartConfigAn = (MultipartConfig)ainfo.getAnnotation();
        com.sun.enterprise.deployment.web.MultipartConfig multipartConfig = webCompDesc.getMultipartConfig();
        if (multipartConfig == null) {
            multipartConfig = new MultipartConfigDescriptor();
            webCompDesc.setMultipartConfig(multipartConfig);
        }

        if (multipartConfig.getLocation() == null) {
            multipartConfig.setLocation(multipartConfigAn.location());
        }
        if (multipartConfig.getMaxFileSize() == null) {
            multipartConfig.setMaxFileSize(multipartConfigAn.maxFileSize());
        }
        if (multipartConfig.getMaxRequestSize() == null) {
            multipartConfig.setMaxRequestSize(multipartConfigAn.maxRequestSize());
        }
        if (multipartConfig.getFileSizeThreshold() == null) {
            multipartConfig.setFileSizeThreshold(multipartConfigAn.fileSizeThreshold());
        }

        return getDefaultProcessedResult();
    }

    /**
     * @return an array of annotation types this annotation handler would
     * require to be processed (if present) before it processes it's own
     * annotation type.
     */
    @Override
    public Class<? extends Annotation>[] getTypeDependencies() {
        return getWebAnnotationTypes();
    }
}
