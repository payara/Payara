/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2014 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.connectors.jms.deployment.annotation.handlers;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.deployment.annotation.context.*;
import com.sun.enterprise.deployment.annotation.handlers.AbstractResourceHandler;
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
import jakarta.jms.JMSConnectionFactoryDefinition;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;

@Service
@AnnotationHandlerFor(JMSConnectionFactoryDefinition.class)
public class JMSConnectionFactoryDefinitionHandler extends AbstractResourceHandler {

    public JMSConnectionFactoryDefinitionHandler() {
    }

    protected HandlerProcessingResult processAnnotation(AnnotationInfo ainfo, ResourceContainerContext[] rcContexts)
            throws AnnotationProcessorException {
        return processAnnotation(ainfo.getAnnotation(), ainfo, rcContexts);
    }

    protected HandlerProcessingResult processAnnotation(AnnotationInstance jmsConnectionFactoryDefnAn, AnnotationInfo aiInfo,
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

            Set<ResourceDescriptor> jmscfdDescs = context.getResourceDescriptors(JavaEEResourceType.JMSCFDD);
            JMSConnectionFactoryDefinitionDescriptor desc = createDescriptor(index, jmsConnectionFactoryDefnAn);
            if (isDefinitionAlreadyPresent(jmscfdDescs, desc)) {
                merge(jmscfdDescs, index, jmsConnectionFactoryDefnAn);
                for (ResourceDescriptor jmscfdDesc : jmscfdDescs) {
                    if (jmscfdDesc instanceof JMSConnectionFactoryDefinitionDescriptor) {
                        setDefaultTransactionSupport((JMSConnectionFactoryDefinitionDescriptor) jmscfdDesc);
                    }
                }
            } else {
                setDefaultTransactionSupport(desc);
                context.addResourceDescriptor(desc);
            }
        }
        return getDefaultProcessedResult();
    }

    private void setDefaultTransactionSupport(JMSConnectionFactoryDefinitionDescriptor desc) {
        Properties props = desc.getProperties();
        if (props.get("org.glassfish.connector-connection-pool.transaction-support") == null)
            props.put("org.glassfish.connector-connection-pool.transaction-support", "XATransaction");
    }

    /**
     * To take care of the case where an ejb is provided in a .war and
     * annotation processor will process this class twice (once for ejb and
     * once for web-bundle-context, which is a bug).<br>
     * This method helps to overcome the issue, partially.<br>
     * Checks whether both the annotated class and the context are either ejb or web.
     *
     * @param index resolves the annotations of the class's methods
     * @param annotatedClass annotated-class
     * @param ejbClass indicates whether the class is an ejb-class
     * @param warClass indicates whether the class is an web-class
     * @param context resource-container-context
     * @return boolean indicates whether the annotation can be processed.
     */
    private boolean canProcessAnnotation(IndexView index, ClassInfo annotatedClass, boolean ejbClass, boolean warClass,
                                         ResourceContainerContext context) {
        String annotatedClassName = annotatedClass.name().toString();
        if (ejbClass) {
            if (!(context instanceof EjbBundleContext ||
                    context instanceof EjbContext ||
                    context instanceof EjbInterceptorContext
            )) {
                if (logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST, "Ignoring @JMSConnectionFactoryDefinition annotation processing as the class is " +
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
                    logger.log(Level.FINEST, "Ignoring @JMSConnectionFactoryDefinition annotation processing as the class " +
                            "[ " + annotatedClassName + " ] is " +
                            "not an EJB class and the context is EJBContext");
                }
                return false;
            }
        } else if (warClass) {
            if (!(context instanceof WebBundleContext || context instanceof WebComponentsContext
                    || context instanceof WebComponentContext )) {
                if (logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST, "Ignoring @JMSConnectionFactoryDefinition annotation processing as the class is " +
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
                    EjbBundleDescriptor ejbBundleDesc = (EjbBundleDescriptor)desc;
                    EjbDescriptor[] ejbDescs = ejbBundleDesc.getEjbByClassName(annotatedClassName);
                    if (ejbDescs != null && ejbDescs.length > 0) {
                        if (logger.isLoggable(Level.FINEST)) {
                            logger.log(Level.FINEST, "Ignoring @JMSConnectionFactoryDefinition annotation processing as the class " +
                                    "[ " + annotatedClassName + " ] is " +
                                    "not an Web class and the context is WebContext");
                        }
                        return false;
                    } else if (ejbBundleDesc.getInterceptorByClassName(annotatedClassName) != null) {
                            if (logger.isLoggable(Level.FINEST)) {
                                logger.log(Level.FINEST, "Ignoring @JMSConnectionFactoryDefinition annotation processing " +
                                        "as the class " +
                                        "[ " + annotatedClassName + " ] is " +
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
                                    logger.log(Level.FINEST, "Ignoring @JMSConnectionFactoryDefinition annotation processing " +
                                            "as the class " +
                                            "[ " + annotatedClassName + " ] is " +
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

    private boolean isDefinitionAlreadyPresent(Set<ResourceDescriptor> jmscfdDescs,
                                               JMSConnectionFactoryDefinitionDescriptor desc) {
        for (ResourceDescriptor descriptor : jmscfdDescs) {
            if (descriptor.equals(desc)) {
                return true;
            }
        }
        return false;
    }

    private void merge(Set<ResourceDescriptor> jmscfdDescs, IndexView index, AnnotationInstance defn) {

        for (ResourceDescriptor descriptor : jmscfdDescs) {
            if (descriptor instanceof JMSConnectionFactoryDefinitionDescriptor) {
                JMSConnectionFactoryDefinitionDescriptor desc = (JMSConnectionFactoryDefinitionDescriptor)descriptor;
                if (desc.getName().equals(AnnotationValues.string(index, defn, "name"))) {

                    if (desc.getInterfaceName() == null) {
                        desc.setInterfaceName(AnnotationValues.string(index, defn, "interfaceName"));
                    }

                    if (desc.getClassName() == null) {
                        String className = AnnotationValues.string(index, defn, "className");
                        if (isValid(className)) {
                            desc.setClassName(className);
                        }
                    }

                    if (desc.getDescription() == null) {
                        String description = AnnotationValues.string(index, defn, "description");
                        if (isValid(description)) {
                            desc.setDescription(description);
                        }
                    }

                    if (desc.getResourceAdapter() == null) {
                        String resourceAdapter = AnnotationValues.string(index, defn, "resourceAdapter");
                        if (isValid(resourceAdapter)) {
                            desc.setResourceAdapter(resourceAdapter);
                        }
                    }

                    if (desc.getUser() == null) {
                        String user = AnnotationValues.string(index, defn, "user");
                        if (isValid(user)) {
                            desc.setUser(user);
                        }
                    }

                    if (desc.getPassword() == null) {
                        // allow empty passwords
                        desc.setPassword(AnnotationValues.string(index, defn, "password"));
                    }

                    if (desc.getClientId() == null) {
                        String clientId = AnnotationValues.string(index, defn, "clientId");
                        if (isValid(clientId)) {
                            desc.setClientId(clientId);
                        }
                    }

                    if (!desc.isTransactionSet()) {
                        desc.setTransactional(AnnotationValues.bool(index, defn, "transactional"));
                    }

                    if (desc.getMaxPoolSize() < 0) {
                        int maxPoolSize = AnnotationValues.integer(index, defn, "maxPoolSize");
                        if (maxPoolSize >= 0) {
                            desc.setMaxPoolSize(maxPoolSize);
                        }
                    }

                    if (desc.getMinPoolSize() < 0) {
                        int minPoolSize = AnnotationValues.integer(index, defn, "minPoolSize");
                        if (minPoolSize >= 0) {
                            desc.setMinPoolSize(minPoolSize);
                        }
                    }

                    Properties properties = desc.getProperties();
                    for (String property : AnnotationValues.strings(index, defn, "properties")) {
                        int separator = property.indexOf('=');
                        // found "=" and not at start or end of string
                        if (separator > 0 && separator < property.length() - 1) {
                            String name = property.substring(0, separator).trim();
                            String value = property.substring(separator + 1).trim();
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

    private JMSConnectionFactoryDefinitionDescriptor createDescriptor(IndexView index, AnnotationInstance defn) {

        JMSConnectionFactoryDefinitionDescriptor desc = new JMSConnectionFactoryDefinitionDescriptor();
        desc.setMetadataSource(MetadataSource.ANNOTATION);

        desc.setName(AnnotationValues.string(index, defn, "name"));
        desc.setInterfaceName(AnnotationValues.string(index, defn, "interfaceName"));

        String className = AnnotationValues.string(index, defn, "className");
        if (isValid(className)) {
            desc.setClassName(className);
        }

        String description = AnnotationValues.string(index, defn, "description");
        if (isValid(description)) {
            desc.setDescription(description);
        }

        String resourceAdapter = AnnotationValues.string(index, defn, "resourceAdapter");
        if (isValid(resourceAdapter)) {
            desc.setResourceAdapter(resourceAdapter);
        }

        String user = AnnotationValues.string(index, defn, "user");
        if (isValid(user)) {
            desc.setUser(user);
        }

        // allow empty passwords
        desc.setPassword(AnnotationValues.string(index, defn, "password"));

        String clientId = AnnotationValues.string(index, defn, "clientId");
        if (isValid(clientId)) {
            desc.setClientId(clientId);
        }

        desc.setTransactional(AnnotationValues.bool(index, defn, "transactional"));

        int maxPoolSize = AnnotationValues.integer(index, defn, "maxPoolSize");
        if (maxPoolSize >= 0) {
            desc.setMaxPoolSize(maxPoolSize);
        }
        int minPoolSize = AnnotationValues.integer(index, defn, "minPoolSize");
        if (minPoolSize >= 0) {
            desc.setMinPoolSize(minPoolSize);
        }

        Properties properties = desc.getProperties();
        for (String property : AnnotationValues.strings(index, defn, "properties")) {
            int separator = property.indexOf('=');
            // found "=" and not at start or end of string
            if (separator > 0 && separator < property.length() - 1) {
                String name = property.substring(0, separator).trim();
                String value = property.substring(separator + 1).trim();
                properties.put(name, value);
            }
        }

        return desc;
    }

    private boolean isValid(String s) {
        return !s.isEmpty();
    }
}
