/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2014 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2016-2025] [Payara Foundation and/or its affiliates]

package org.glassfish.weld.connector;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.ServerTags;
import com.sun.enterprise.deployment.xml.RuntimeTagNames;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import jakarta.decorator.Decorator;
import jakarta.ejb.MessageDriven;
import jakarta.ejb.Stateful;
import jakarta.ejb.Stateless;
import jakarta.enterprise.context.*;
import jakarta.enterprise.inject.Model;
import jakarta.enterprise.inject.Stereotype;
import jakarta.faces.flow.FlowScoped;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Scope;
import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptor;
import javax.xml.parsers.SAXParserFactory;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.internal.api.Globals;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.deployment.JandexIndexer;
import org.glassfish.internal.deployment.JandexIndexer.Index;
import org.jboss.jandex.AnnotationInstance;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class WeldUtils {

    private static Logger logger = Logger.getLogger(WeldUtils.class.getName());

    private static final String EMPTY_BEANS_XML_MODE_ALL = "fish.payara.deployment.emptyBeansXmlModeALL";

    public static final char SEPARATOR_CHAR = '/';
    public static final String WEB_INF = "WEB-INF";
    public static final String WEB_INF_CLASSES = WEB_INF + SEPARATOR_CHAR + "classes";
    public static final String WEB_INF_LIB = WEB_INF + SEPARATOR_CHAR + "lib";

    public static final String BEANS_XML_FILENAME = "beans.xml";
    public static final String WEB_INF_BEANS_XML = WEB_INF + SEPARATOR_CHAR + BEANS_XML_FILENAME;
    public static final String META_INF_BEANS_XML = "META-INF" + SEPARATOR_CHAR + BEANS_XML_FILENAME;
    public static final String WEB_INF_CLASSES_META_INF_BEANS_XML = WEB_INF_CLASSES + SEPARATOR_CHAR + META_INF_BEANS_XML;

    private static final String SERVICES_DIR = "services";

    // We don't want this connector module to depend on CDI API, as connector can be present in a distribution
    // which does not have CDI implementation. So, we use the class name as a string.
    private static final String SERVICES_CLASSNAME = "jakarta.enterprise.inject.spi.Extension";
    private static final String BCE_SERVICES_CLASSNAME = "jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension";
    private static final String CDI_ENABLING_ANNOTATIONS_CACHE_METADATA = "fish.payara.cdi.enabling.annotations.cache.metadata";
    private static final String CDI_ANNOTATED_CLASS_NAMES_METADATA = "fish.payara.cdi.annotated.class.names.metadata";

    public static final String META_INF_SERVICES_EXTENSION = "META-INF" + SEPARATOR_CHAR + SERVICES_DIR +
            SEPARATOR_CHAR + SERVICES_CLASSNAME;
    public static final String META_INF_BCE_SERVICES_EXTENSION =  "META-INF" + SEPARATOR_CHAR + SERVICES_DIR +
            SEPARATOR_CHAR + BCE_SERVICES_CLASSNAME;
    public static final String WEB_INF_SERVICES_EXTENSION =  WEB_INF_CLASSES + SEPARATOR_CHAR + META_INF_SERVICES_EXTENSION;
    public static final String WEB_INF_BCE_EXTENSION = WEB_INF_CLASSES + SEPARATOR_CHAR + META_INF_BCE_SERVICES_EXTENSION;


    public static final String CLASS_SUFFIX = ".class";
    public static final String JAR_SUFFIX = ".jar";
    public static final String RAR_SUFFIX = ".rar";
    public static final String EXPANDED_RAR_SUFFIX = "_rar";
    public static final String EXPANDED_JAR_SUFFIX = "_jar";

    /**
     * Bean Deployment Archive type.
     * <p>
     * Can be WAR, JAR, RAR or UNKNOWN.
     */
    public enum BDAType { WAR, JAR, RAR, UNKNOWN };

    static final Set<Class<? extends Annotation>> cdiScopeAnnotationClasses;
    protected static final Set<String> cdiScopeAnnotations;
    static {
        final HashSet<Class<? extends Annotation>> cdi = new HashSet<>();
        cdi.add(Scope.class);
        cdi.add(NormalScope.class);
        cdi.add(ConversationScoped.class);
        cdi.add(ViewScoped.class);
        cdi.add(FlowScoped.class);
        cdi.add(ApplicationScoped.class);
        cdi.add(SessionScoped.class);
        cdi.add(RequestScoped.class);
        cdi.add(Dependent.class);
        cdi.add(Singleton.class);
        cdi.add(Model.class);

        cdiScopeAnnotationClasses = Collections.unmodifiableSet(cdi);
        cdiScopeAnnotations = cdiScopeAnnotationClasses.stream().map(Class::getName).collect(Collectors.toSet());
    }

    static final Set<String> cdiEnablingAnnotations;
    static final Set<Class<? extends Annotation>> cdiEnablingAnnotationClasses;
    static {
        // CDI scopes
        final HashSet<Class<? extends Annotation>> cdi = new HashSet<>(cdiScopeAnnotationClasses);

        // 1.2 updates
        cdi.add(Decorator.class);
        cdi.add(Interceptor.class);
        cdi.add(Stereotype.class);

        // EJB annotations
        cdi.add(MessageDriven.class);
        cdi.add(Stateful.class);
        cdi.add(Stateless.class);
        cdi.add(jakarta.ejb.Singleton.class);

        cdiEnablingAnnotationClasses = Collections.unmodifiableSet(cdi);
        cdiEnablingAnnotations = cdiEnablingAnnotationClasses.stream().map(Class::getName).collect(Collectors.toSet());
    }


    /**
     * Determine whether the specified archive is an implicit bean deployment archive.
     *
     * @param context  The deployment context
     * @param archive  The archive in question
     * @return true, if it is an implicit bean deployment archive; otherwise, false.
     * @throws java.io.IOException
     */
    public static boolean isImplicitBeanArchive(DeploymentContext context, ReadableArchive archive)
            throws IOException {
        var index = Globals.getDefaultHabitat().getService(JandexIndexer.class)
                .getIndexesByURI(context, Collections.singleton(archive.getURI()))
                .values().stream().findAny().get();
        return index.implicitBeanArchive(() -> {
            if(!isValidBdaBasedOnExtensionAndBeansXml(context, archive)) {
                return false;
            }
            return isImplicitBeanArchive(context, archive.getURI());
        });
    }

    /**
     * Determine whether the specified archive is an implicit bean deployment archive.
     *
     * @param context     The deployment context
     * @param archivePath The URI of the archive
     *
     * @return true, if it is an implicit bean deployment archive; otherwise, false.
     */
    public static boolean isImplicitBeanArchive(DeploymentContext context, URI archivePath) {
        var index = Globals.getDefaultHabitat().getService(JandexIndexer.class)
                .getIndexesByURI(context, Collections.singleton(archivePath))
                .values().stream().findAny().get();
        return index.implicitBeanArchive(() -> isImplicitBeanDiscoveryEnabled(context) && hasCDIEnablingAnnotations(context, archivePath));
    }

    /**
     * Determine whether there are any beans annotated with annotations that should enable CDI
     * processing even in the absence of a beans.xml descriptor.
     *
     * @param context The DeploymentContext
     * @param path    The path to check for annotated beans
     *
     * @return true, if there is at least one bean annotated with a qualified annotation in the specified path
     */
    public static boolean hasCDIEnablingAnnotations(DeploymentContext context, URI path) {
        var index = Globals.getDefaultHabitat().getService(JandexIndexer.class)
                .getIndexesByURI(context, Collections.singleton(path))
                .values().stream().findAny().get();
        return index.hasCDIEnablingAnnotations(() -> hasCDIEnablingAnnotations(context, Collections.singleton(path)));
    }


    /**
     * Determine whether there are any beans annotated with annotations that should enable CDI
     * processing even in the absence of a beans.xml descriptor.
     *
     * @param context The DeploymentContext
     * @param paths   The paths to check for annotated beans
     *
     * @return true, if there is at least one bean annotated with a qualified annotation in the specified paths
     */
    public static boolean hasCDIEnablingAnnotations(DeploymentContext context, Collection<URI> paths) {
        Map<String, Index> indexes = Globals.getDefaultHabitat().getService(JandexIndexer.class)
                .getIndexesByURI(context, paths);
        boolean result = Arrays.stream(getCDIEnablingAnnotations(context))
                .anyMatch(annotation -> indexes.values().stream()
                        .anyMatch(index -> !index.getIndex().getAnnotations(annotation).isEmpty()));
        return result;
    }

    /**
     * Get the names of any annotation types that are applied to beans, which should enable CDI
     * processing even in the absence of a beans.xml descriptor.
     *
     * @param context The DeploymentContext
     *
     * @return An array of annotation type names; The array could be empty if none are found.
     */
    public static String[] getCDIEnablingAnnotations(DeploymentContext context) {
        String[] annotations = context.getTransientAppMetaData(CDI_ENABLING_ANNOTATIONS_CACHE_METADATA, String[].class);
        if (annotations != null) {
            return annotations;
        }
        var indexes = Globals.getDefaultHabitat().getService(JandexIndexer.class).getAllIndexes(context);
        Set<String> appCdiEnablingAnnotations = indexes.values().stream()
                .flatMap(index -> index.getIndex().getKnownClasses().stream())
                .flatMap(classInfo -> classInfo.annotations().stream())
                .map(annotationClass -> annotationClass.name().toString())
                .collect(Collectors.toSet());

        Set<String> additionalAnnotations = new HashSet<>();
        appCdiEnablingAnnotations.stream()
                .filter(annotation -> !cdiEnablingAnnotations.contains(annotation))
                .forEach(annotation -> {
            try {
                @SuppressWarnings("unchecked")
                Class<? extends Annotation> cls = (Class<? extends Annotation>) context.getClassLoader()
                        .loadClass(annotation);
                for (var annotationClass : cdiEnablingAnnotationClasses) {
                    if (cls.isAnnotationPresent(annotationClass)) {
                        additionalAnnotations.add(annotation);
                        break;
                    }
                }
            } catch (ClassNotFoundException | NullPointerException e) {
            }
        });
        annotations = appCdiEnablingAnnotations.stream()
                .filter(annotation -> cdiEnablingAnnotations.contains(annotation)
                        || additionalAnnotations.contains(annotation))
                .toArray(String[]::new);
        context.addTransientAppMetaData(CDI_ENABLING_ANNOTATIONS_CACHE_METADATA, annotations);
        return annotations;
    }

    /**
     * Get the names of any classes that are annotated with bean-defining annotations, which should
     * enable CDI processing even in the absence of a beans.xml descriptor.
     *
     * @param context The DeploymentContext
     *
     * @return A collection of class names; The collection could be empty if none are found.
     */
    public static Collection<String> getCDIAnnotatedClassNames(DeploymentContext context) {
        @SuppressWarnings("unchecked")
        Set<String> result = context.getTransientAppMetaData(CDI_ANNOTATED_CLASS_NAMES_METADATA, Set.class);
        if (result != null) {
            return result;
        }

        final Set<String> cdiEnablingAnnotations = new HashSet<>();
        Collections.addAll(cdiEnablingAnnotations, getCDIEnablingAnnotations(context));
        var indexes = Globals.getDefaultHabitat().getService(JandexIndexer.class).getAllIndexes(context);
        result = new HashSet<>();
        var finalResult = result;
        indexes.values().forEach(index -> cdiEnablingAnnotations
                .forEach(annotation -> finalResult.addAll(index.getIndex().getAnnotations(annotation).stream()
                        .map(WeldUtils::mapAnnotationToClassName)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet()))));
        context.addTransientAppMetaData(CDI_ANNOTATED_CLASS_NAMES_METADATA, result);
        return result;
    }

    /**
     * Searches through the known class names of a {@link BeanDeploymentArchive} to determine which have fields or
     * methods with the {@link Inject} annotation.
     *
     * @param deploymentContext The deployment context
     * @param knownClassNames The known class names of a {@link BeanDeploymentArchive}
     * @return The class names from the given list which have fields or methods annotated with {@link Inject}
     */
    public static Collection<String> getInjectionTargetClassNames(DeploymentContext deploymentContext,
                                                                  Collection<String> knownClassNames) {
        var indexes = Globals.getDefaultHabitat().getService(JandexIndexer.class).getAllIndexes(deploymentContext);
        Set<String> result = new HashSet<>();
        indexes.values().forEach(index -> {
            for (AnnotationInstance annotationInstance : index.getIndex().getAnnotations(Inject.class)) {
                String className = mapAnnotationToClassName(annotationInstance);
                if (className != null && knownClassNames.contains(className)) {
                    result.add(className);
                }
            }
        });
        return result;
    }

    /**
     * Determine whether the specified class is annotated with a CDI scope annotation.
     *
     * @param clazz  The class to check.
     *
     * @return true, if the specified class has a CDI scope annotation; Otherwise, false.
     */
    public static boolean hasScopeAnnotation(Class clazz) {
        return hasValidAnnotation(clazz, cdiScopeAnnotations, null);
    }


    /**
     * Determine whether the specified class is annotated with a CDI-enabling annotation.
     *
     * @param clazz The class to check.
     *
     * @return true, if the specified class has a CDI scope annotation; Otherwise, false.
     */
    public static boolean hasCDIEnablingAnnotation(Class clazz) {
        return hasValidAnnotation(clazz, cdiEnablingAnnotations, null);
    }

    /**
     * Determine whether the specified class is annotated with one of the annotations in the specified
     * validScopes collection, but not with any of the annotations in the specified exclusion set.
     *
     * @param annotatedClass          The class to check.
     * @param validScopes    A collection of valid CDI scope type names
     * @param excludedScopes A collection of excluded CDI scope type names
     *
     * @return true, if the specified class has at least one of the annotations specified in
     *         validScopes, and none of the annotations specified in excludedScopes; Otherwise, false.
     */
    public static boolean hasValidAnnotation(Class              annotatedClass,
                                             Set<String> validScopes,
                                             Collection<String> excludedScopes) {

        final Set<String> copyOfExcludedScopes = copyCollectionToSet(excludedScopes);

        // Check all the annotations on the specified Class to determine if the class is annotated
        // with a supported CDI scope
        for (final Annotation annotation : annotatedClass.getAnnotations()) {
            if (isValidAnnotation(annotation.annotationType(), validScopes, copyOfExcludedScopes)) {
               return true;
            }
        }

        return false;
    }

    private static Set<String> copyCollectionToSet(final Collection<String> toBeCopied) {
        final Set<String> copy;
        if (toBeCopied == null) {
            copy = new HashSet<>();
        } else {
            copy = new HashSet<>(toBeCopied);
        }
        return copy;
    }

    /**
     * Determine whether the specified annotation type is one of the specified valid types and not
     * in the specified exclusion list. Positive results include those annotations which are themselves
     * annotated with types in the valid list.
     *
     * @param annotationType     The annotation type to check
     * @param validTypeNames     The valid annotation type names
     * @param excludedTypeNames  The excluded annotation type names
     *
     * @return true, if the specified type is in the valid list and not in the excluded list; Otherwise, false.
     */
    private static boolean isValidAnnotation(Class<? extends Annotation> annotationType,
                                               Set<String>          validTypeNames,
                                               Set<String>          excludedTypeNames) {
        Objects.requireNonNull(validTypeNames);
        Objects.requireNonNull(excludedTypeNames);

        if (validTypeNames.isEmpty()) {
            return false;
        }

        final String annotationTypeName = annotationType.getName();
        if (validTypeNames.contains(annotationTypeName) && !excludedTypeNames.contains(annotationTypeName)) {
            return true;
        } else if (excludedTypeNames.add(annotationTypeName)) {
            // If the annotation type itself is not an excluded type, then check its annotation
            // types, exclude itself (to avoid infinite recursion)
            for (Annotation parent : annotationType.getAnnotations()) {
                if (isValidAnnotation(parent.annotationType(), validTypeNames, excludedTypeNames)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static int getPreLoaderThreads() {
        int result = 0;
        // Check the "global" configuration
        ServiceLocator serviceLocator = Globals.getDefaultHabitat();
        if (serviceLocator != null) {
            Config config = serviceLocator.getService(Config.class, ServerEnvironment.DEFAULT_INSTANCE_NAME);
            if (config != null) {
                result = Integer.valueOf(config.getExtensionByType(CDIService.class).getPreLoaderThreadPoolSize());
            }
        }

        return result;

    }

    public static boolean isConcurrentDeploymentEnabled() {
        boolean result = false;
        // Check the "global" configuration
        ServiceLocator serviceLocator = Globals.getDefaultHabitat();
        if (serviceLocator != null) {
            Config config = serviceLocator.getService(Config.class, ServerEnvironment.DEFAULT_INSTANCE_NAME);
            if (config != null) {
                result = Boolean.valueOf(config.getExtensionByType(CDIService.class).getEnableConcurrentDeployment());
            }
        }

        return result;

    }


    public static boolean isImplicitBeanDiscoveryEnabled() {
        boolean result = false;

        // Check the "global" configuration
        ServiceLocator serviceLocator = Globals.getDefaultHabitat();
        if (serviceLocator != null) {
            Config config = serviceLocator.getService(Config.class, ServerEnvironment.DEFAULT_INSTANCE_NAME);
            if (config != null) {
                result = Boolean.valueOf(config.getExtensionByType(CDIService.class).getEnableImplicitCdi());
            }
        }

        return result;
    }

    public static boolean isImplicitBeanDiscoveryEnabled(DeploymentContext context) {
        boolean result = false;

        if (isImplicitBeanDiscoveryEnabled()) {
            // If implicit discovery is enabled for the server, then check if it's disabled for the
            // deployment of this application.
            Object propValue = context.getAppProps().get(RuntimeTagNames.IMPLICIT_CDI_ENABLED_PROP);
            Object appPropValue = context.getAppProps().get(RuntimeTagNames.PAYARA_ENABLE_IMPLICIT_CDI);
            if(appPropValue != null) {
                propValue = appPropValue;
            }

            // If the property is not set, or it's value is true, then implicit discovery is enabled
            result = (propValue == null || Boolean.parseBoolean((String) propValue));
        }

        return result;
    }
    
    public static boolean isEmptyBeansXmlModeALL(DeploymentContext context) {
        if(Boolean.getBoolean(EMPTY_BEANS_XML_MODE_ALL)) {
            return true;
        }
        Object propValue = context.getAppProps().get(ServerTags.EMPTY_BEANS_XML_MODE_ALL_PROP);
        return propValue != null && Boolean.parseBoolean((String) propValue);
    }

  public static InputStream getBeansXmlInputStream(DeploymentContext context) {
    return getBeansXmlInputStream( context.getSource() );
  }

    /**
     * Determine if an archive is a valid bda based on what the spec says about extensions.
     * See section 12.1 which states that if an archive contains an extension but no beans.xml then it is NOT
     * a valid bean deployment archive.
     *
     * @param archive The archive to check.
     * @return false if there is an extension and no beans.xml
     * true otherwise
     */
    public static boolean isValidBdaBasedOnExtensionAndBeansXml(DeploymentContext context, ReadableArchive archive) {
        var index = Globals.getDefaultHabitat().getService(JandexIndexer.class)
                .getIndexesByURI(context, Collections.singleton(archive.getURI()))
                .values().stream().findAny().get();
        return index.isValidBdaBasedOnExtensionAndBeansXml(() -> {
            try {
                if (hasExtension(archive) && !hasBeansXML(archive)) {
                    // Extensions and no beans.xml: not a bda
                    return false;
                }
            } catch (IOException ignore) {
            }
            return true;
        });
    }

    public static boolean hasExtension(ReadableArchive archive) {
        try {
            if (isWar(archive)) {
                return archive.exists(WEB_INF_SERVICES_EXTENSION) || archive.exists(WEB_INF_BCE_EXTENSION);
            }
            return archive.exists(META_INF_SERVICES_EXTENSION) || archive.exists(META_INF_BCE_SERVICES_EXTENSION);
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Error when searching extensions", ioe);
            return false;
        }
    }


    public static boolean hasBeansXML(ReadableArchive archive) throws IOException {
        if (isWar(archive)) {
            return archive.exists(WeldUtils.WEB_INF_BEANS_XML) ||
                    archive.exists(WeldUtils.WEB_INF_CLASSES_META_INF_BEANS_XML);
        }
        return archive.exists(WeldUtils.META_INF_BEANS_XML);
    }

    private static boolean isWar(ReadableArchive archive) throws IOException {
        return archive.exists(WeldUtils.WEB_INF);
    }


  public static InputStream getBeansXmlInputStream( ReadableArchive archive ) {
    InputStream inputStream = null;

    try {
      if (archive.exists(WeldUtils.WEB_INF)) {
        inputStream = archive.getEntry(WeldUtils.WEB_INF_BEANS_XML);
        if (inputStream == null) {
          inputStream = archive.getEntry(WeldUtils.WEB_INF_CLASSES_META_INF_BEANS_XML);
        }
      } else {
        inputStream = archive.getEntry(WeldUtils.META_INF_BEANS_XML);
      }
    } catch (IOException e) {
      return null;
    }
    return inputStream;
  }

    /**
     * Get the "bean-discovery-mode" from the "beans" element if it exists in beans.xml
     * From section 12.1 of CDI spec:
     * A bean archive has a bean discovery mode of all, annotated or none. A bean archive which
     * contains a beans.xml file with no version has a default bean discovery mode of annotated. A bean
     * archive which contains a beans.xml file with version 1.1 (or later) must specify the bean-
     * discovey-mode attribute. The default value for the attribute is annotated.
     *
     * @param beansXmlInputStream The InputStream for the beans.xml to check.
     * @return "annotated" if there is no beans.xml
     * "annotated" if the bean-discovery-mode is missing
     * "annotated" if the bean-discovery-mode is empty
     * The value of bean-discovery-mode in all other cases.
     */
    public static String getBeanDiscoveryMode(InputStream beansXmlInputStream) {
        if (beansXmlInputStream == null) {
            // there is no beans.xml.
            return "annotated";
        }

        String beanDiscoveryMode = null;
        LocalDefaultHandler handler = new LocalDefaultHandler();
        try {
            SAXParserFactory.newInstance().newSAXParser().parse(beansXmlInputStream, handler);
        } catch (SAXStoppedIntentionallyException exc) {
            beanDiscoveryMode = handler.getBeanDiscoveryMode();
        } catch (Exception ignore) {
        }

        if (beanDiscoveryMode == null) {
            //when empty beans.xml or bean-discovery-mode not specified
            return "annotated";
        }

        if (beanDiscoveryMode.equals("")) {
            return "annotated";
        }

        return beanDiscoveryMode;
    }

    private static class LocalDefaultHandler extends DefaultHandler {
        String beanDiscoveryMode = null;

        @Override
        public void startElement(String uri, String localName,String qName, Attributes attributes) throws SAXException {
            if (qName.equals("beans")) {
                beanDiscoveryMode = attributes.getValue("bean-discovery-mode");
                throw new SAXStoppedIntentionallyException();
            }
        }

        public String getBeanDiscoveryMode() {
            return beanDiscoveryMode;
        }
    }

    private static class SAXStoppedIntentionallyException extends SAXException {
        private SAXStoppedIntentionallyException() {
            super();
        }
    }

    private static String mapAnnotationToClassName(AnnotationInstance annotationInstance) {
        String className = null;
        switch (annotationInstance.target().kind()) {
            case CLASS:
                className = annotationInstance.target().asClass().name().toString();
                break;
            case FIELD:
                className = annotationInstance.target().asField().declaringClass().name().toString();
                break;
            case METHOD:
                className = annotationInstance.target().asMethod().receiverType().name().toString();
                break;
            case METHOD_PARAMETER:
                className = annotationInstance.target().asMethodParameter().method().receiverType().name().toString();
                break;
            case TYPE:
                className = annotationInstance.target().asType().asClass().toString();
                break;
            case RECORD_COMPONENT:
                className = annotationInstance.target().asRecordComponent().declaringClass().name().toString();
                break;
        }
        return className;
    }
}
