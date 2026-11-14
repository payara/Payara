/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.resources.javamail.annotation.handler;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.deployment.annotation.context.*;
import com.sun.enterprise.deployment.annotation.handlers.AbstractResourceHandler;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.apf.AnnotationHandlerFor;
import org.glassfish.apf.AnnotationInfo;
import org.glassfish.apf.AnnotationProcessorException;
import org.glassfish.apf.AnnotationValues;
import org.glassfish.apf.HandlerProcessingResult;
import org.glassfish.deployment.common.JavaEEResourceType;
import org.glassfish.deployment.common.RootDeploymentDescriptor;
import org.glassfish.internal.deployment.ClassHierarchy;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jvnet.hk2.annotations.Service;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.AroundTimeout;
import jakarta.interceptor.Interceptors;
import jakarta.mail.MailSessionDefinition;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;

/**
 * Created by IntelliJ IDEA.
 * User: naman mehta
 * Date: 18/4/12
 * Time: 11:04 AM
 * To change this template use File | Settings | File Templates.
 */

@Service
@AnnotationHandlerFor(MailSessionDefinition.class)
public class MailSessionDefinitionHandler extends AbstractResourceHandler {

    protected final static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(MailSessionDefinitionHandler.class);

    public MailSessionDefinitionHandler() {

    }

    @Override
    protected HandlerProcessingResult processAnnotation(AnnotationInfo ainfo, ResourceContainerContext[] rcContexts) throws AnnotationProcessorException {
        return processAnnotation(ainfo.getAnnotation(), ainfo, rcContexts);
    }

    protected HandlerProcessingResult processAnnotation(AnnotationInstance mailSessionDefnAn, AnnotationInfo aiInfo,
                                                        ResourceContainerContext[] rcContexts)
            throws AnnotationProcessorException {
        IndexView index = aiInfo.getIndex();
        ClassInfo annotatedClass = aiInfo.getAnnotatedElement().asClass();
        List<AnnotationInstance> annotations = ClassHierarchy.classAnnotations(index, annotatedClass);
        boolean warClass = isAWebComponentClass(annotations);
        boolean ejbClass = isAEjbComponentClass(annotations);

        for (ResourceContainerContext context : rcContexts) {
            if (!canProcessAnnotation(index, annotatedClass, ejbClass, warClass, context)) {
                return getDefaultProcessedResult();
            }

            Set<ResourceDescriptor> mailSessionDescs = context.getResourceDescriptors(JavaEEResourceType.MSD);
            MailSessionDescriptor desc = createDescriptor(index, mailSessionDefnAn);
            if (isDefinitionAlreadyPresent(mailSessionDescs, desc)) {
                merge(mailSessionDescs, index, mailSessionDefnAn);
            } else {
                mailSessionDescs.add(desc);
            }
        }
        return getDefaultProcessedResult();
    }

    private boolean canProcessAnnotation(IndexView index, ClassInfo annotatedClass, boolean ejbClass, boolean warClass,
                                         ResourceContainerContext context) {
        String annotatedClassName = annotatedClass.name().toString();
        if (ejbClass) {
            if (!(context instanceof EjbBundleContext ||
                    context instanceof EjbContext ||
                    context instanceof EjbInterceptorContext
            )) {
                if (logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST, "Ignoring @MailSessionDefinition annotation processing as the class is" +
                            "an EJB class and context is not one of EJBContext");
                }
                return false;
            }
        } else if (context instanceof EjbBundleContext) {
            EjbBundleContext ejbContext = (EjbBundleContext) context;
            EjbBundleDescriptor ejbBundleDescriptor = ejbContext.getDescriptor();
            EjbDescriptor[] ejbDescriptor = ejbBundleDescriptor.getEjbByClassName(annotatedClassName);
            if (ejbDescriptor == null || ejbDescriptor.length == 0) {
                if (logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST, "Ignoring @MailSessionDefinition annotation processing as the class " +
                            "[ " + annotatedClassName + " ] is" +
                            "not an EJB class and the context is EJBContext");
                }
                return false;
            }
        } else if (warClass) {
            if (!(context instanceof WebBundleContext || context instanceof WebComponentsContext
                    || context instanceof WebComponentContext)) {
                if (logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST, "Ignoring @MailSessionDefinition annotation processing as the class is" +
                            "an Web class and context is not one of WebContext");
                }
                return false;
            }
        } else if (context instanceof WebBundleContext) {
            WebBundleContext webBundleContext = (WebBundleContext) context;
            WebBundleDescriptor webBundleDescriptor = webBundleContext.getDescriptor();
            Collection<RootDeploymentDescriptor> extDesc = webBundleDescriptor.getExtensionsDescriptors();
            for (RootDeploymentDescriptor desc : extDesc) {
                if (desc instanceof EjbBundleDescriptor) {
                    EjbBundleDescriptor ejbBundleDesc = (EjbBundleDescriptor) desc;
                    EjbDescriptor[] ejbDescs = ejbBundleDesc.getEjbByClassName(annotatedClassName);
                    if (ejbDescs != null && ejbDescs.length > 0) {
                        if (logger.isLoggable(Level.FINEST)) {
                            logger.log(Level.FINEST, "Ignoring @MailSessionDefinition annotation processing as the class " +
                                    "[ " + annotatedClassName + " ] is" +
                                    "not an Web class and the context is WebContext");
                        }
                        return false;
                    } else if (ejbBundleDesc.getInterceptorByClassName(annotatedClassName) != null) {
                        if (logger.isLoggable(Level.FINEST)) {
                            logger.log(Level.FINEST, "Ignoring @MailSessionDefinition annotation processing " +
                                    "as the class " +
                                    "[ " + annotatedClassName + " ] is" +
                                    "not an Web class and the context is WebContext");
                        }
                        return false;
                    } else {
                        for (MethodInfo method : annotatedClass.methods()) {
                            if (!method.isConstructor()
                                    && (ClassHierarchy.annotation(index, method, AroundInvoke.class) != null
                                    || ClassHierarchy.annotation(index, method, AroundTimeout.class) != null
                                    || ClassHierarchy.annotation(index, method, Interceptors.class) != null)) {
                                if (logger.isLoggable(Level.FINEST)) {
                                    logger.log(Level.FINEST, "Ignoring @MailSessionDefinition annotation processing " +
                                            "as the class " +
                                            "[ " + annotatedClassName + " ] is" +
                                            "not an Web class, an interceptor and the context is WebContext");
                                }
                                return false;
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    private MailSessionDescriptor createDescriptor(IndexView index, AnnotationInstance defn) {

        MailSessionDescriptor desc = new MailSessionDescriptor();
        desc.setMetadataSource(MetadataSource.ANNOTATION);

        desc.setName(AnnotationValues.string(index, defn, "name"));

        String description = AnnotationValues.string(index, defn, "description");
        if (!description.equals("")) {
            desc.setDescription(description);
        }

        String storeProtocol = AnnotationValues.string(index, defn, "storeProtocol");
        if (!storeProtocol.equals("")) {
            desc.setStoreProtocol(storeProtocol);
        }

        String transportProtocol = AnnotationValues.string(index, defn, "transportProtocol");
        if (!transportProtocol.equals("")) {
            desc.setTransportProtocol(transportProtocol);
        }

        String host = AnnotationValues.string(index, defn, "host");
        if (!host.equals("")) {
            desc.setHost(host);
        }

        String user = AnnotationValues.string(index, defn, "user");
        if (!user.equals("")) {
            desc.setUser(user);
        }

        // allow empty passwords
        desc.setPassword(AnnotationValues.string(index, defn, "password"));

        String from = AnnotationValues.string(index, defn, "from");
        if (!from.equals("")) {
            desc.setFrom(from);
        }

        Properties properties = desc.getProperties();
        for (String property : AnnotationValues.strings(index, defn, "properties")) {
            int separator = property.indexOf('=');
            // found "=" and not at start or end of string
            if (separator > -1 && separator != 0 && separator < property.length() - 1) {
                String name = property.substring(0, separator);
                String value = property.substring(separator + 1);
                properties.put(name, value);
            }
        }

        return desc;
    }

    private boolean isDefinitionAlreadyPresent(Set<ResourceDescriptor> mailSessionDescs,
                                               MailSessionDescriptor mailSessionDesc) {
        boolean result = false;
        for (ResourceDescriptor msDesc : mailSessionDescs) {
            if (msDesc.equals(mailSessionDesc)) {
                result = true;
                break;
            }
        }
        return result;
    }

    private void merge(Set<ResourceDescriptor> mailSessionDescs, IndexView index, AnnotationInstance mailSessionDefn) {

        for (ResourceDescriptor orgDesc : mailSessionDescs) {
            MailSessionDescriptor desc = (MailSessionDescriptor)orgDesc;
            if (desc.getName().equals(AnnotationValues.string(index, mailSessionDefn, "name"))) {

                if (desc.getDescription() == null) {
                    String description = AnnotationValues.string(index, mailSessionDefn, "description");
                    if (!description.equals("")) {
                        desc.setDescription(description);
                    }
                }

                if (desc.getStoreProtocol() == null) {
                    desc.setStoreProtocol(AnnotationValues.string(index, mailSessionDefn, "storeProtocol"));
                }

                if (desc.getTransportProtocol() == null) {
                    desc.setTransportProtocol(AnnotationValues.string(index, mailSessionDefn, "transportProtocol"));
                }

                if (desc.getHost() == null) {
                    desc.setHost(AnnotationValues.string(index, mailSessionDefn, "host"));
                }

                if (desc.getUser() == null) {
                    String user = AnnotationValues.string(index, mailSessionDefn, "user");
                    if (!user.equals("")) {
                        desc.setUser(user);
                    }
                }

                if (desc.getPassword() == null) {
                    // allow empty passwords
                    desc.setPassword(AnnotationValues.string(index, mailSessionDefn, "password"));
                }

                if (desc.getFrom() == null) {
                    desc.setFrom(AnnotationValues.string(index, mailSessionDefn, "from"));
                }

                Properties properties = desc.getProperties();
                for (String property : AnnotationValues.strings(index, mailSessionDefn, "properties")) {
                    int separator = property.indexOf('=');
                    // found "=" and not at start or end of string
                    if (separator > -1 && separator != 0 && separator < property.length() - 1) {
                        String name = property.substring(0, separator);
                        String value = property.substring(separator + 1);
                        //add to properties only when not already present
                        if (properties.get(name) == null) {
                            properties.put(name, value);
                        }
                    }
                }
                break;
            }
        }

    }

}
