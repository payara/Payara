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
package org.glassfish.jdbcruntime.deployment.annotation.handlers;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.deployment.annotation.context.*;
import com.sun.enterprise.deployment.annotation.handlers.AbstractResourceHandler;
import org.glassfish.apf.AnnotationHandlerFor;
import org.glassfish.apf.AnnotationInfo;
import org.glassfish.apf.AnnotationProcessorException;
import org.glassfish.apf.AnnotationValues;
import org.glassfish.apf.HandlerProcessingResult;
import org.glassfish.config.support.TranslatedConfigView;
import org.glassfish.deployment.common.JavaEEResourceType;
import org.glassfish.deployment.common.RootDeploymentDescriptor;
import org.glassfish.internal.deployment.ClassHierarchy;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jvnet.hk2.annotations.Service;

import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.AroundTimeout;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.Interceptors;
import java.lang.annotation.Annotation;
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

    @Override
    protected HandlerProcessingResult processAnnotation(AnnotationInfo ainfo, ResourceContainerContext[] rcContexts)
            throws AnnotationProcessorException {
        return processAnnotation(ainfo.getAnnotation(), ainfo, rcContexts);
    }

    protected HandlerProcessingResult processAnnotation(AnnotationInstance dataSourceDefnAn, AnnotationInfo aiInfo,
                                                        ResourceContainerContext[] rcContexts)
            throws AnnotationProcessorException {
        IndexView index = aiInfo.getIndex();
        ClassInfo annotatedClass = aiInfo.getAnnotatedElement().asClass();
        List<AnnotationInstance> annotations = ClassHierarchy.classAnnotations(index, annotatedClass);
        boolean warClass = isAWebComponentClass(annotations);
        boolean ejbClass = isAEjbComponentClass(annotations);

        for(ResourceContainerContext context : rcContexts){
                if (!canProcessAnnotation(index, annotatedClass, ejbClass, warClass, context)){
                    return getDefaultProcessedResult();
                }

            Set<ResourceDescriptor> dsdDescs = context.getResourceDescriptors(JavaEEResourceType.DSD);
            DataSourceDefinitionDescriptor desc = createDescriptor(index, dataSourceDefnAn);
            if(isDefinitionAlreadyPresent(dsdDescs, desc)){
                merge(dsdDescs, index, dataSourceDefnAn);
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
                    logger.log(Level.FINEST, "Ignoring @DataSourceDefinition annotation processing as the class is" +
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
                    logger.log(Level.FINEST, "Ignoring @DataSourceDefinition annotation processing as the class " +
                            "[ " + annotatedClassName + " ] is" +
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
                    EjbDescriptor[] ejbDescs = ejbBundleDesc.getEjbByClassName(annotatedClassName);
                    if(ejbDescs != null && ejbDescs.length > 0){
                        if (logger.isLoggable(Level.FINEST)) {
                            logger.log(Level.FINEST, "Ignoring @DataSourceDefinition annotation processing as the class " +
                                    "[ " + annotatedClassName + " ] is" +
                                    "not an Web class and the context is WebContext");
                        }
                        return false;
                    }else if(ejbBundleDesc.getInterceptorByClassName(annotatedClassName) != null){
                            if (logger.isLoggable(Level.FINEST)) {
                                logger.log(Level.FINEST, "Ignoring @DataSourceDefinition annotation processing " +
                                        "as the class " +
                                        "[ " + annotatedClassName + " ] is" +
                                        "not an Web class and the context is WebContext");
                            }
                            return false;
                    }else{
                        for (MethodInfo method : annotatedClass.methods()) {
                            if (!method.isConstructor()
                                    && (ClassHierarchy.annotation(index, method, AroundInvoke.class) != null
                                    || ClassHierarchy.annotation(index, method, AroundTimeout.class) != null
                                    || ClassHierarchy.annotation(index, method, Interceptors.class) != null)) {
                                if (logger.isLoggable(Level.FINEST)) {
                                    logger.log(Level.FINEST, "Ignoring @DataSourceDefinition annotation processing " +
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


    @Override
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


    private void merge(Set<ResourceDescriptor> dsdDescs, IndexView index, AnnotationInstance defn) {

        for (ResourceDescriptor orgdesc : dsdDescs) {
            DataSourceDefinitionDescriptor desc = (DataSourceDefinitionDescriptor)orgdesc;
            if (desc.getName().equals(AnnotationValues.string(index, defn, "name"))) {

                if (desc.getClassName() == null) {
                    desc.setClassName(TranslatedConfigView.expandValue(AnnotationValues.string(index, defn, "className")));
                }

                if (desc.getDescription() == null) {
                    String description = AnnotationValues.string(index, defn, "description");
                    if (!description.equals("")) {
                        desc.setDescription(TranslatedConfigView.expandValue(description));
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
                    String serverName = AnnotationValues.string(index, defn, "serverName");
                    if (!serverName.equals("localhost")) {
                        desc.setServerName(TranslatedConfigView.expandValue(serverName));
                    }
                }

                //try only when URL is not set
                if (desc.getPortNumber() == -1 && desc.getUrl() == null) {
                    int portNumber = AnnotationValues.integer(index, defn, "portNumber");
                    if (portNumber != -1) {
                        desc.setPortNumber(portNumber);
                    }
                }

                //try only when URL is not set
                if (desc.getDatabaseName() == null && desc.getUrl() == null) {
                    String databaseName = AnnotationValues.string(index, defn, "databaseName");
                    if (!databaseName.equals("")) {
                        desc.setDatabaseName(TranslatedConfigView.expandValue(databaseName));
                    }
                }

                //try only when URL or standard properties are not set
                if (desc.getUrl() == null &&
                        !(desc.getPortNumber() != -1 && desc.getServerName() != null &&
                                (desc.getDatabaseName() != null))) {
                    String url = AnnotationValues.string(index, defn, "url");
                    if (!url.equals("")) {
                        desc.setUrl(TranslatedConfigView.expandValue(url));
                    }

                }

                if (desc.getUser() == null) {
                    String user = AnnotationValues.string(index, defn, "user");
                    if (!user.equals("")) {
                        desc.setUser(TranslatedConfigView.expandValue(user));
                    }
                }

                if (desc.getPassword() == null) {
                    // allow empty passwords
                    desc.setPassword(TranslatedConfigView.expandValue(AnnotationValues.string(index, defn, "password")));
                }

                if (desc.getIsolationLevel() == -1) {
                    int isolationLevel = AnnotationValues.integer(index, defn, "isolationLevel");
                    if (isolationLevel != -1) {
                        desc.setIsolationLevel(String.valueOf(isolationLevel));
                    }
                }

                if (!desc.isTransactionSet()) {
                    desc.setTransactional(AnnotationValues.bool(index, defn, "transactional"));
                }

                if (desc.getMinPoolSize() == -1) {
                    int minPoolSize = AnnotationValues.integer(index, defn, "minPoolSize");
                    if (minPoolSize != -1) {
                        desc.setMinPoolSize(minPoolSize);
                    }
                }

                if (desc.getMaxPoolSize() == -1) {
                    int maxPoolSize = AnnotationValues.integer(index, defn, "maxPoolSize");
                    if (maxPoolSize != -1) {
                        desc.setMaxPoolSize(maxPoolSize);
                    }
                }

                if (desc.getInitialPoolSize() == -1) {
                    int initialPoolSize = AnnotationValues.integer(index, defn, "initialPoolSize");
                    if (initialPoolSize != -1) {
                        desc.setInitialPoolSize(initialPoolSize);
                    }
                }

                if (desc.getMaxIdleTime() == -1) {
                    int maxIdleTime = AnnotationValues.integer(index, defn, "maxIdleTime");
                    if (maxIdleTime != -1) {
                        desc.setMaxIdleTime(String.valueOf(maxIdleTime));
                    }
                }

                if (desc.getMaxStatements() == -1) {
                    int maxStatements = AnnotationValues.integer(index, defn, "maxStatements");
                    if (maxStatements != -1) {
                        desc.setMaxStatements(maxStatements);
                    }
                }

                if (!desc.isLoginTimeoutSet()) {
                    int loginTimeout = AnnotationValues.integer(index, defn, "loginTimeout");
                    if (loginTimeout != 0) {
                        desc.setLoginTimeout(String.valueOf(loginTimeout));
                    }
                }

                Properties properties = desc.getProperties();
                for (String property : AnnotationValues.strings(index, defn, "properties")) {
                    int separator = property.indexOf('=');
                    // found "=" and not at start or end of string
                    if (separator > -1 && separator != 0 && separator < property.length() - 1) {
                        String name = property.substring(0, separator);
                        String value = property.substring(separator + 1);
                        //add to properties only when not already present
                        if (properties.get(name) == null) {
                            properties.put(name, TranslatedConfigView.expandValue(value));
                        }
                    }
                }
                break;
            }
        }

    }

    //Not private as accessed by tests
    DataSourceDefinitionDescriptor createDescriptor(IndexView index, AnnotationInstance defn) {

        DataSourceDefinitionDescriptor desc = new DataSourceDefinitionDescriptor();
        desc.setMetadataSource(MetadataSource.ANNOTATION);

        desc.setName(TranslatedConfigView.expandValue(AnnotationValues.string(index, defn, "name")));
        desc.setClassName(TranslatedConfigView.expandValue(AnnotationValues.string(index, defn, "className")));

        String description = AnnotationValues.string(index, defn, "description");
        if (!description.equals("")) {
            desc.setDescription(TranslatedConfigView.expandValue(description));
        }

        String serverName = AnnotationValues.string(index, defn, "serverName");
        if (!serverName.equals("localhost")) {
            desc.setServerName(TranslatedConfigView.expandValue(serverName));
        }

        int portNumber = AnnotationValues.integer(index, defn, "portNumber");
        if (portNumber != -1) {
            desc.setPortNumber(portNumber);
        }


        String databaseName = AnnotationValues.string(index, defn, "databaseName");
        if (!databaseName.equals("")) {
            desc.setDatabaseName(TranslatedConfigView.expandValue(databaseName));
        }

        if (desc.getPortNumber() != -1 && desc.getDatabaseName() != null && desc.getServerName() != null) {
            //standard properties are set, ignore URL
        } else {
            String url = AnnotationValues.string(index, defn, "url");
            if (!url.equals("")) {
                desc.setUrl(TranslatedConfigView.expandValue(url));
                desc.setServerName(null); //To prevent serverName overriding the URL, always use the URL if the standard properties are not set
            }
        }

        String user = AnnotationValues.string(index, defn, "user");
        if (!user.equals("")) {
            desc.setUser(TranslatedConfigView.expandValue(user));
        }

        // allow empty passwords
        desc.setPassword(TranslatedConfigView.expandValue(AnnotationValues.string(index, defn, "password")));

        int isolationLevel = AnnotationValues.integer(index, defn, "isolationLevel");
        if (isolationLevel != -1) {
            desc.setIsolationLevel(TranslatedConfigView.expandValue(String.valueOf(isolationLevel)));
        }

        desc.setTransactional(AnnotationValues.bool(index, defn, "transactional"));

        int minPoolSize = AnnotationValues.integer(index, defn, "minPoolSize");
        if (minPoolSize != -1) {
            desc.setMinPoolSize(minPoolSize);
        }

        int maxPoolSize = AnnotationValues.integer(index, defn, "maxPoolSize");
        if (maxPoolSize != -1) {
            desc.setMaxPoolSize(maxPoolSize);
        }
        int initialPoolSize = AnnotationValues.integer(index, defn, "initialPoolSize");
        if (initialPoolSize != -1) {
            desc.setInitialPoolSize(initialPoolSize);
        }
        int maxIdleTime = AnnotationValues.integer(index, defn, "maxIdleTime");
        if (maxIdleTime != -1) {
            desc.setMaxIdleTime(String.valueOf(maxIdleTime));
        }

        int maxStatements = AnnotationValues.integer(index, defn, "maxStatements");
        if (maxStatements != -1) {
            desc.setMaxStatements(maxStatements);
        }

        int loginTimeout = AnnotationValues.integer(index, defn, "loginTimeout");
        if (loginTimeout != 0) {
            desc.setLoginTimeout(String.valueOf(loginTimeout));
        }

        Properties properties = desc.getProperties();
        for (String property : AnnotationValues.strings(index, defn, "properties")) {
            int separator = property.indexOf('=');
            // found "=" and not at start or end of string
            if (separator > -1 && separator != 0 && separator < property.length() - 1) {
                String name = property.substring(0, separator);
                String value = property.substring(separator + 1);
                properties.put(name.trim(), TranslatedConfigView.expandValue(value.trim()));
            }
        }

        return desc;
    }
}
