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

package org.glassfish.jdbcruntime.deployment.annotation.handlers;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.deployment.annotation.context.*;
import com.sun.enterprise.deployment.annotation.handlers.AbstractResourceHandler;
import org.glassfish.apf.AnnotationHandlerFor;
import org.glassfish.apf.AnnotationInfo;
import org.glassfish.apf.AnnotationProcessorException;
import org.glassfish.apf.HandlerProcessingResult;
import org.glassfish.deployment.common.JavaEEResourceType;
import org.glassfish.deployment.common.RootDeploymentDescriptor;
import org.jvnet.hk2.annotations.Service;

import javax.annotation.sql.DataSourceDefinition;
import javax.interceptor.AroundInvoke;
import javax.interceptor.AroundTimeout;
import javax.interceptor.Interceptor;
import javax.interceptor.Interceptors;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;

/**
 * @author Jagadish Ramu
 */
@Service
@AnnotationHandlerFor(DataSourceDefinition.class)
public class DataSourceDefinitionHandler extends AbstractResourceHandler {

    public DataSourceDefinitionHandler() {
    }

    protected HandlerProcessingResult processAnnotation(AnnotationInfo ainfo, ResourceContainerContext[] rcContexts)
            throws AnnotationProcessorException {
        DataSourceDefinition dataSourceDefnAn =
                (DataSourceDefinition)ainfo.getAnnotation();
        return processAnnotation(dataSourceDefnAn, ainfo, rcContexts);
    }

    protected HandlerProcessingResult processAnnotation(DataSourceDefinition dataSourceDefnAn, AnnotationInfo aiInfo,
                                                        ResourceContainerContext[] rcContexts)
            throws AnnotationProcessorException {
        Class annotatedClass = (Class)aiInfo.getAnnotatedElement();
        Annotation[] annotations = annotatedClass.getAnnotations();
        boolean warClass = isAWebComponentClass(annotations);
        boolean ejbClass = isAEjbComponentClass(annotations);

        for(ResourceContainerContext context : rcContexts){
                if (!canProcessAnnotation(annotatedClass, ejbClass, warClass, context)){
                    return getDefaultProcessedResult();
                }

            Set<ResourceDescriptor> dsdDescs = context.getResourceDescriptors(JavaEEResourceType.DSD);
            DataSourceDefinitionDescriptor desc = createDescriptor(dataSourceDefnAn);
            if(isDefinitionAlreadyPresent(dsdDescs, desc)){
                merge(dsdDescs, dataSourceDefnAn);
            }else{
                dsdDescs.add(desc);
            }
        }
        return getDefaultProcessedResult();
    }

    /**
     * To take care of the case where an ejb is provided in a .war and
     * annotation processor will process this class twice (once for ejb and
     * once for web-bundle-context, which is a bug).<br>
     * This method helps to overcome the issue, partially.<br>
     * Checks whether both the annotated class and the context are either ejb or web.
     *
     * @param annotatedClass annotated-class
     * @param ejbClass indicates whether the class is an ejb-class
     * @param warClass indicates whether the class is an web-class
     * @param context resource-container-context
     * @return boolean indicates whether the annotation can be processed.
     */
    private boolean canProcessAnnotation(Class annotatedClass, boolean ejbClass, boolean warClass,
                                         ResourceContainerContext context) {
        if (ejbClass) {
            if (!(context instanceof EjbBundleContext ||
                    context instanceof EjbContext ||
                    context instanceof EjbInterceptorContext
            )) {
                if (logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST, "Ignoring @DataSourceDefinition annotation processing as the class is" +
                            "an EJB class and context is not one of EJBContext");
                }
                return false;
            }
        } else if (context instanceof EjbBundleContext) {
            EjbBundleContext ejbContext = (EjbBundleContext) context;
            EjbBundleDescriptor ejbBundleDescriptor = ejbContext.getDescriptor();
            EjbDescriptor[] ejbDescriptor = ejbBundleDescriptor.getEjbByClassName(annotatedClass.getName());
            if (ejbDescriptor == null || ejbDescriptor.length == 0) {
                if (logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST, "Ignoring @DataSourceDefinition annotation processing as the class " +
                            "[ " + annotatedClass + " ] is" +
                            "not an EJB class and the context is EJBContext");
                }
                return false;
            }
        } else if (warClass) {
            if (!(context instanceof WebBundleContext || context instanceof WebComponentsContext
                    || context instanceof WebComponentContext )) {
                if (logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST, "Ignoring @DataSourceDefinition annotation processing as the class is" +
                            "an Web class and context is not one of WebContext");
                }
                return false;
            }
        } else if (context instanceof WebBundleContext) {
            WebBundleContext webBundleContext = (WebBundleContext) context;
            WebBundleDescriptor webBundleDescriptor = webBundleContext.getDescriptor();
            Collection<RootDeploymentDescriptor> extDesc = webBundleDescriptor.getExtensionsDescriptors();
            for(RootDeploymentDescriptor desc : extDesc){
                if(desc instanceof EjbBundleDescriptor){
                    EjbBundleDescriptor ejbBundleDesc = (EjbBundleDescriptor)desc;
                    EjbDescriptor[] ejbDescs = ejbBundleDesc.getEjbByClassName(annotatedClass.getName());
                    if(ejbDescs != null && ejbDescs.length > 0){
                        if (logger.isLoggable(Level.FINEST)) {
                            logger.log(Level.FINEST, "Ignoring @DataSourceDefinition annotation processing as the class " +
                                    "[ " + annotatedClass + " ] is" +
                                    "not an Web class and the context is WebContext");
                        }
                        return false;
                    }else if(ejbBundleDesc.getInterceptorByClassName(annotatedClass.getName()) != null){
                            if (logger.isLoggable(Level.FINEST)) {
                                logger.log(Level.FINEST, "Ignoring @DataSourceDefinition annotation processing " +
                                        "as the class " +
                                        "[ " + annotatedClass + " ] is" +
                                        "not an Web class and the context is WebContext");
                            }
                            return false;
                    }else{
                        Method[] methods = annotatedClass.getDeclaredMethods();
                        for(Method method : methods){
                            Annotation annotations[] = method.getAnnotations();
                            for(Annotation annotation : annotations){
                                if(annotation.annotationType().equals(AroundInvoke.class) ||
                                        annotation.annotationType().equals(AroundTimeout.class) ||
                                        annotation.annotationType().equals(Interceptors.class)) {
                                    if (logger.isLoggable(Level.FINEST)) {
                                        logger.log(Level.FINEST, "Ignoring @DataSourceDefinition annotation processing " +
                                                "as the class " +
                                                "[ " + annotatedClass + " ] is" +
                                                "not an Web class, an interceptor and the context is WebContext");
                                    }
                                    return false;
                                }
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    private boolean isDefinitionAlreadyPresent(Set<ResourceDescriptor> dsdDescs,
                                               DataSourceDefinitionDescriptor desc) {
        boolean result = false ;
        for(ResourceDescriptor descriptor : dsdDescs){
            if(descriptor.equals(desc)){
                result = true;
                break;
            }
        }
        return result;
    }


    public Class<? extends Annotation>[] getTypeDependencies() {
        Class<? extends Annotation> [] annotations = getEjbAndWebAnnotationTypes();
        List<Class <? extends Annotation>> annotationsList = new ArrayList<Class <? extends Annotation>>();
        for(Class<? extends Annotation> annotation : annotations){
            annotationsList.add(annotation);
        }
        annotationsList.add(Interceptors.class);
        annotationsList.add(Interceptor.class);
        annotationsList.add(AroundInvoke.class);
        annotationsList.add(AroundTimeout.class);

        Class<? extends Annotation>[] result = new Class[annotationsList.size()];
        return annotationsList.toArray(result);
    }


    private void merge(Set<ResourceDescriptor> dsdDescs, DataSourceDefinition defn) {

        for (ResourceDescriptor orgdesc : dsdDescs) {
            DataSourceDefinitionDescriptor desc = (DataSourceDefinitionDescriptor)orgdesc;
            if (desc.getName().equals(defn.name())) {

                if (desc.getClassName() == null) {
                    desc.setClassName(defn.className());
                }

                if (desc.getDescription() == null) {
                    if (defn.description() != null && !defn.description().equals("")) {
                        desc.setDescription(defn.description());
                    }
                }

                // When either URL or Standard properties are specified in DD, annotation values
                // (of URL or standard properties) are ignored.
                // DD values will win as either of URL or standard properties will be present most of the times.
                // Only when neither URL nor standard properties are not present, annotation values are considered.
                // In such case, standard properties take precedence over URL.

                //try only when URL is not set
                if (!desc.isServerNameSet() && desc.getUrl() == null) {
                    //localhost is the default value (even in the descriptor)
                    if (defn.serverName() != null && !defn.serverName().equals("localhost")) {
                        desc.setServerName(defn.serverName());
                    }
                }

                //try only when URL is not set
                if (desc.getPortNumber() == -1 && desc.getUrl() == null) {
                    if (defn.portNumber() != -1) {
                        desc.setPortNumber(defn.portNumber());
                    }
                }

                //try only when URL is not set
                if (desc.getDatabaseName() == null && desc.getUrl() == null) {
                    if (defn.databaseName() != null && !defn.databaseName().equals("")) {
                        desc.setDatabaseName(defn.databaseName());
                    }
                }

                //try only when URL or standard properties are not set
                if (desc.getUrl() == null &&
                        !(desc.getPortNumber() != -1 && desc.getServerName() != null &&
                                (desc.getDatabaseName() != null))) {
                    if (defn.url() != null && !defn.url().equals("")) {
                        desc.setUrl(defn.url());
                    }

                }

                if (desc.getUser() == null) {
                    if (defn.user() != null && !defn.user().equals("")) {
                        desc.setUser(defn.user());
                    }
                }

                if (desc.getPassword() == null) {
                    if (defn.password() != null /*ALLOW EMPTY PASSWORDS && !defn.password().equals("")*/) {
                        desc.setPassword(defn.password());
                    }
                }

                if (desc.getIsolationLevel() == -1) {
                    if (defn.isolationLevel() != -1) {
                        desc.setIsolationLevel(String.valueOf(defn.isolationLevel()));
                    }
                }

                if (!desc.isTransactionSet()) {
                    if (defn.transactional()) {
                        desc.setTransactional(true);
                    } else {
                        desc.setTransactional(false);
                    }
                }

                if (desc.getMinPoolSize() == -1) {
                    if (defn.minPoolSize() != -1) {
                        desc.setMinPoolSize(defn.minPoolSize());
                    }
                }

                if (desc.getMaxPoolSize() == -1) {
                    if (defn.maxPoolSize() != -1) {
                        desc.setMaxPoolSize(defn.maxPoolSize());
                    }
                }

                if (desc.getInitialPoolSize() == -1) {
                    if (defn.initialPoolSize() != -1) {
                        desc.setInitialPoolSize(defn.initialPoolSize());
                    }
                }

                if (desc.getMaxIdleTime() == -1) {
                    if (defn.maxIdleTime() != -1) {
                        desc.setMaxIdleTime(String.valueOf(defn.maxIdleTime()));
                    }
                }

                if (desc.getMaxStatements() == -1) {
                    if (defn.maxStatements() != -1) {
                        desc.setMaxStatements(defn.maxStatements());
                    }
                }

                if (!desc.isLoginTimeoutSet()) {
                    if (defn.loginTimeout() != 0) {
                        desc.setLoginTimeout(String.valueOf(defn.loginTimeout()));
                    }
                }

                Properties properties = desc.getProperties();
                String[] defnProperties = defn.properties();

                if (defnProperties.length > 0) {
                    for (String property : defnProperties) {
                        int index = property.indexOf("=");
                        // found "=" and not at start or end of string
                        if (index > -1 && index != 0 && index < property.length() - 1) {
                            String name = property.substring(0, index);
                            String value = property.substring(index + 1);
                            //add to properties only when not already present
                            if (properties.get(name) == null) {
                                properties.put(name, value);
                            }
                        }
                    }
                }
                break;
            }
        }

    }


    private DataSourceDefinitionDescriptor createDescriptor(DataSourceDefinition defn) {

        DataSourceDefinitionDescriptor desc = new DataSourceDefinitionDescriptor();
        desc.setMetadataSource(MetadataSource.ANNOTATION);

        desc.setName(defn.name());
        desc.setClassName(defn.className());

        if (defn.description() != null && !defn.description().equals("")) {
            desc.setDescription(defn.description());
        }

        if (defn.serverName() != null && !defn.serverName().equals("localhost")) {
            desc.setServerName(defn.serverName());
        }

        if (defn.portNumber() != -1) {
            desc.setPortNumber(defn.portNumber());
        }


        if (defn.databaseName() != null && !defn.databaseName().equals("")) {
            desc.setDatabaseName(defn.databaseName());
        }

        if ((desc.getPortNumber() != -1 && desc.getDatabaseName() != null && desc.getServerName() != null)) {
            //standard properties are set, ignore URL
        } else {
            if (defn.url() != null && !defn.url().equals("")) {
                desc.setUrl(defn.url());
            }
        }

        if (defn.user() != null && !defn.user().equals("")) {
            desc.setUser(defn.user());
        }

        if (defn.password() != null /*ALLOW EMPTY PASSWORDS && !defn.password().equals("")*/) {
            desc.setPassword(defn.password());
        }

        if (defn.isolationLevel() != -1) {
            desc.setIsolationLevel(String.valueOf(defn.isolationLevel()));
        }

        if (defn.transactional()) {
            desc.setTransactional(true);
        } else {
            desc.setTransactional(false);
        }

        if (defn.minPoolSize() != -1) {
            desc.setMinPoolSize(defn.minPoolSize());
        }

        if (defn.maxPoolSize() != -1) {
            desc.setMaxPoolSize(defn.maxPoolSize());
        }
        if (defn.initialPoolSize() != -1) {
            desc.setInitialPoolSize(defn.initialPoolSize());
        }
        if (defn.maxIdleTime() != -1) {
            desc.setMaxIdleTime(String.valueOf(defn.maxIdleTime()));
        }

        if (defn.maxStatements() != -1) {
            desc.setMaxStatements(defn.maxStatements());
        }

        if (defn.loginTimeout() != 0) {
            desc.setLoginTimeout(String.valueOf(defn.loginTimeout()));
        }


        if (defn.properties() != null) {
            Properties properties = desc.getProperties();

            String[] defnProperties = defn.properties();
            if (defnProperties.length > 0) {
                for (String property : defnProperties) {
                    int index = property.indexOf("=");
                    // found "=" and not at start or end of string
                    if (index > -1 && index != 0 && index < property.length() - 1) {
                        String name = property.substring(0, index);
                        String value = property.substring(index + 1);
                        properties.put(name, value);
                    }
                }
            }
        }

        return desc;
    }
}
