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

import com.sun.enterprise.deployment.EnvironmentProperty;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.annotation.context.WebBundleContext;
import com.sun.enterprise.deployment.annotation.context.WebComponentContext;
import com.sun.enterprise.deployment.web.ServletFilter;
import com.sun.enterprise.deployment.web.ServletFilterMapping;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.net.URLPattern;
import org.glassfish.apf.AnnotationHandlerFor;
import org.glassfish.apf.AnnotationInfo;
import org.glassfish.apf.AnnotationProcessorException;
import org.glassfish.apf.HandlerProcessingResult;
import org.glassfish.web.deployment.descriptor.ServletFilterDescriptor;
import org.glassfish.web.deployment.descriptor.ServletFilterMappingDescriptor;
import org.jvnet.hk2.annotations.Service;

import javax.servlet.DispatcherType;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

/**
 * This handler is responsible in handling
 * javax.servlet.annotation.WebFilter.
 *
 * @author Shing Wai Chan
 */
@Service
@AnnotationHandlerFor(WebFilter.class)
public class WebFilterHandler extends AbstractWebHandler {
    protected final static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(WebFilterHandler.class);

    public WebFilterHandler() {
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

        Class filterClass = (Class)ainfo.getAnnotatedElement();
        if (!javax.servlet.Filter.class.isAssignableFrom(filterClass)) {
            log(Level.SEVERE, ainfo,
                localStrings.getLocalString(
                "web.deployment.annotation.handlers.needtoimpl",
                "The Class {0} having annotation {1} need to implement the interface {2}.",
                new Object[] { filterClass.getName(), WebFilter.class.getName(), javax.servlet.Filter.class.getName() }));
            return getDefaultFailedResult();
        }

        WebFilter webFilterAn = (WebFilter)ainfo.getAnnotation();
        String filterName = webFilterAn.filterName();
        if (filterName == null || filterName.length() == 0) {
            filterName = filterClass.getName();
        }

        ServletFilterDescriptor servletFilterDesc = null;
        for (ServletFilter sfDesc : webBundleDesc.getServletFilters()) {
            if (filterName.equals(sfDesc.getName())) {
                servletFilterDesc = (ServletFilterDescriptor)sfDesc;
                break;
            }
        }

        if (servletFilterDesc == null) {
            servletFilterDesc = new ServletFilterDescriptor();
            servletFilterDesc.setName(filterName);
            webBundleDesc.addServletFilter(servletFilterDesc);
        } else {
            String filterImpl = servletFilterDesc.getClassName();
            if (filterImpl != null && filterImpl.length() > 0 &&
                    !filterImpl.equals(filterClass.getName())) {
                log(Level.SEVERE, ainfo,
                    localStrings.getLocalString(
                    "web.deployment.annotation.handlers.filternamedontmatch",
                    "The filter '{0}' has implementation '{1}' in xml. It does not match with '{2}' from annotation @{3}.",
                    new Object[] { filterName, filterImpl, filterClass.getName(),
                    WebFilter.class.getName() }));
                return getDefaultFailedResult();
            }
        }

        servletFilterDesc.setClassName(filterClass.getName());
        if (servletFilterDesc.getDescription() == null ||
                servletFilterDesc.getDescription().length() == 0) {

            servletFilterDesc.setDescription(webFilterAn.description());
        }
        if (servletFilterDesc.hasSetDisplayName()) {
            servletFilterDesc.setDisplayName(webFilterAn.displayName());
        }

        if (servletFilterDesc.getInitializationParameters().size() == 0) {
            WebInitParam[] initParams = webFilterAn.initParams();
            if (initParams != null && initParams.length > 0) {
                for (WebInitParam initParam : initParams) {
                    servletFilterDesc.addInitializationParameter(
                        new EnvironmentProperty(
                            initParam.name(), initParam.value(),
                            initParam.description()));
                }
            }
        }

        if (servletFilterDesc.getSmallIconUri() == null) {
            servletFilterDesc.setSmallIconUri(webFilterAn.smallIcon());
        }
        if (servletFilterDesc.getLargeIconUri() == null) {
            servletFilterDesc.setLargeIconUri(webFilterAn.largeIcon());
        }

        if (servletFilterDesc.isAsyncSupported() == null) {
            servletFilterDesc.setAsyncSupported(webFilterAn.asyncSupported());
        }

        ServletFilterMapping servletFilterMappingDesc = null;
        boolean hasUrlPattern = false;
        boolean hasServletName = false;

        for (ServletFilterMapping sfm : webBundleDesc.getServletFilterMappings()) {
            if (filterName.equals(sfm.getName())) {
                servletFilterMappingDesc = sfm;
                hasUrlPattern = hasUrlPattern || (sfm.getUrlPatterns().size() > 0);
                hasServletName = hasServletName || (sfm.getServletNames().size() > 0);
            }
        }

        if (servletFilterMappingDesc == null) {
            servletFilterMappingDesc = new ServletFilterMappingDescriptor();
            servletFilterMappingDesc.setName(filterName);
            webBundleDesc.addServletFilterMapping(servletFilterMappingDesc);
        }

        if (!hasUrlPattern) {
            String[] urlPatterns = webFilterAn.urlPatterns();
            if (urlPatterns == null || urlPatterns.length == 0) {
                urlPatterns = webFilterAn.value();
            }

            // accept here as url patterns may be defined in top level xml
            boolean validUrlPatterns = true;
            if (urlPatterns != null && urlPatterns.length > 0) {
                for (String up : urlPatterns) {
                    if (!URLPattern.isValid(up)) {
                        validUrlPatterns = false;
                        break;
                    }
                    servletFilterMappingDesc.addURLPattern(up);
                }
            }

            if (!validUrlPatterns) {
                String urlPatternString =
                    (urlPatterns != null) ? Arrays.toString(urlPatterns) : "";

                throw new IllegalArgumentException(localStrings.getLocalString(
                        "web.deployment.annotation.handlers.invalidUrlPatterns",
                        "Invalid url patterns: {0}.",
                        urlPatternString));
            }
        }

        if (!hasServletName) {
            String[] servletNames = webFilterAn.servletNames();
            if (servletNames != null && servletNames.length > 0) {
                for (String sn : servletNames) {
                    servletFilterMappingDesc.addServletName(sn);
                }
            }
        }

        if (servletFilterMappingDesc.getDispatchers().size() == 0) {
            DispatcherType[] dispatcherTypes = webFilterAn.dispatcherTypes();
                if (dispatcherTypes != null && dispatcherTypes.length > 0) {
                for (DispatcherType dType : dispatcherTypes) {
                    servletFilterMappingDesc.addDispatcher(dType.name());
                }
            }
        }


        return getDefaultProcessedResult();
    }
}
