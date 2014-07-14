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

package org.glassfish.weld.connector;

import com.sun.enterprise.config.serverbeans.Config;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.classmodel.reflect.AnnotationModel;
import org.glassfish.hk2.classmodel.reflect.AnnotationType;
import org.glassfish.hk2.classmodel.reflect.Type;
import org.glassfish.hk2.classmodel.reflect.Types;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.deployment.ExtendedDeploymentContext;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.decorator.Decorator;
import javax.ejb.MessageDriven;
import javax.ejb.Stateful;
import javax.ejb.Stateless;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.NormalScope;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Stereotype;
import javax.inject.Scope;
import javax.inject.Singleton;
import javax.interceptor.Interceptor;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class WeldUtils {

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
    private static final String SERVICES_CLASSNAME = "javax.enterprise.inject.spi.Extension";
    public static final String META_INF_SERVICES_EXTENSION =
        "META-INF" + SEPARATOR_CHAR + SERVICES_DIR + SEPARATOR_CHAR + SERVICES_CLASSNAME;

    public static final String CLASS_SUFFIX = ".class";
    public static final String JAR_SUFFIX = ".jar";
    public static final String RAR_SUFFIX = ".rar";
    public static final String EXPANDED_RAR_SUFFIX = "_rar";
    public static final String EXPANDED_JAR_SUFFIX = "_jar";

    public static enum BDAType { WAR, JAR, RAR, UNKNOWN };

    // The name of the deployment context property used to disable implicit bean discovery for a
    // particular application deployment.
    public static final String IMPLICIT_CDI_ENABLED_PROP = "implicitCdiEnabled";

    protected static final List<String> cdiScopeAnnotations;
    static {
        cdiScopeAnnotations = new ArrayList<String>();
        cdiScopeAnnotations.add(Scope.class.getName());
        cdiScopeAnnotations.add(NormalScope.class.getName());
        cdiScopeAnnotations.add(ApplicationScoped.class.getName());
        cdiScopeAnnotations.add(SessionScoped.class.getName());
        cdiScopeAnnotations.add(RequestScoped.class.getName());
        cdiScopeAnnotations.add(Dependent.class.getName());
        cdiScopeAnnotations.add(Singleton.class.getName());
    }

    protected static final List<String> cdiEnablingAnnotations;
    static {
        cdiEnablingAnnotations = new ArrayList<String>();

        // CDI scopes
        cdiEnablingAnnotations.addAll(cdiScopeAnnotations);

        // 1.2 updates
       cdiEnablingAnnotations.add(Decorator.class.getName());
       cdiEnablingAnnotations.add(Interceptor.class.getName());
       cdiEnablingAnnotations.add(Stereotype.class.getName());

        // EJB annotations
        cdiEnablingAnnotations.add(MessageDriven.class.getName());
        cdiEnablingAnnotations.add(Stateful.class.getName());
        cdiEnablingAnnotations.add(Stateless.class.getName());
        cdiEnablingAnnotations.add(javax.ejb.Singleton.class.getName());
    }

    protected static final List<String> excludedAnnotationTypes = new ArrayList<String>();
    static {
        // These are excluded because they are not scope annotations, and they cause the recursive
        // analysis of parent annotations to continue infinitely because they reference each other,
        // and in some cases, they reference themselves.
        excludedAnnotationTypes.add(Documented.class.getName());
        excludedAnnotationTypes.add(Retention.class.getName());
        excludedAnnotationTypes.add(Target.class.getName());
    }


    /**
     * Determine whether the specified archive is an implicit bean deployment archive.
     *
     * @param context  The deployment context
     * @param archive  The archive in question
     *
     * @return true, if it is an implicit bean deployment archive; otherwise, false.
     */
    public static boolean isImplicitBeanArchive(DeploymentContext context, ReadableArchive archive)
            throws IOException {
        boolean result = false;

        // Archives with extensions are not candidates for implicit bean discovery
        if (!archive.exists(META_INF_SERVICES_EXTENSION)) {
          result = isImplicitBeanArchive(context, archive.getURI());
        }

        return result;
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
        return (isImplicitBeanDiscoveryEnabled(context) && hasCDIEnablingAnnotations(context, archivePath));
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
        Set<URI> paths = new HashSet<URI>();
        paths.add(path);
        return hasCDIEnablingAnnotations(context, paths);
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
        List<String> result = new ArrayList<String>();

        Types types = getTypes(context);
        if (types != null) {
            Iterator<Type> typesIter = types.getAllTypes().iterator();
            while (typesIter.hasNext()) {
                Type type = typesIter.next();
                if (!(type instanceof AnnotationType)) {
                    Iterator<AnnotationModel> annotations = type.getAnnotations().iterator();
                    while (annotations.hasNext()) {
                        AnnotationModel am = annotations.next();
                        AnnotationType at = am.getType();
                        if (isCDIEnablingAnnotation(at) && type.wasDefinedIn(paths)) {
                            if (!result.contains(at.getName())) {
                                result.add(at.getName());
                            }
                        }
                    }
                }
            }
        }

        return !(result.isEmpty());
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
        List<String> result = new ArrayList<String>();

        Types types = getTypes(context);
        if (types != null) {
            Iterator<Type> typesIter = types.getAllTypes().iterator();
            while (typesIter.hasNext()) {
                Type type = typesIter.next();
                if (!(type instanceof AnnotationType)) {
                    Iterator<AnnotationModel> annotations = type.getAnnotations().iterator();
                    while (annotations.hasNext()) {
                        AnnotationModel am = annotations.next();
                        AnnotationType at = am.getType();
                        if (isCDIEnablingAnnotation(at)) {
                            if (!result.contains(at.getName())) {
                                result.add(at.getName());
                            }
                        }
                    }
                }
            }
        }

        return result.toArray(new String[result.size()]);
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
        Set<String> result = new HashSet<String>();

        Types types = getTypes(context);
        if (types != null) {
            Iterator<Type> typesIter = types.getAllTypes().iterator();
            while (typesIter.hasNext()) {
                Type type = typesIter.next();
                if (!(type instanceof AnnotationType)) {
                    Iterator<AnnotationModel> annotations = type.getAnnotations().iterator();
                    while (annotations.hasNext()) {
                        AnnotationModel am = annotations.next();
                        AnnotationType at = am.getType();
                        if (isCDIEnablingAnnotation(at)) {
                            if (!result.contains(at.getName())) {
                                result.add(type.getName());
                            }
                        }
                    }
                }
            }
        }

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
     * Determine if the specified annotation type is a CDI-enabling annotation
     *
     * @param annotationType The annotation type to check
     *
     * @return true, if the specified annotation type qualifies as a CDI enabler; Otherwise, false
     */
    private static boolean isCDIEnablingAnnotation(AnnotationType annotationType) {
        return isCDIEnablingAnnotation(annotationType, null);
    }


    /**
     * Determine if the specified annotation type is a CDI-enabling annotation
     *
     * @param annotationType    The annotation type to check
     * @param excludedTypeNames The Set of annotation type names that should be excluded from the analysis
     *
     * @return true, if the specified annotation type qualifies as a CDI enabler; Otherwise, false
     */
    private static boolean isCDIEnablingAnnotation(AnnotationType annotationType,
                                                   Set<String>    excludedTypeNames) {
        boolean result = false;

        Set<String> exclusions = new HashSet<String>();
        if (excludedTypeNames != null) {
            exclusions.addAll(excludedTypeNames);
        }

        String annotationTypeName = annotationType.getName();
        if (cdiEnablingAnnotations.contains(annotationTypeName) && !exclusions.contains(annotationTypeName)) {
            result = true;
        } else if (!exclusions.contains(annotationTypeName)) {
            // If the annotation type itself is not an excluded type, then check it's annotation
            // types, less itself (to avoid infinite recursion)
            exclusions.add(annotationTypeName);
            for (AnnotationModel parent : annotationType.getAnnotations()) {
                if (isCDIEnablingAnnotation(parent.getType(), exclusions)) {
                    result = true;
                    break;
                }
            }
        }

        return result;
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
                                             Collection<String> validScopes,
                                             Collection<String> excludedScopes) {
        boolean result = false;

        // Check all the annotations on the specified Class to determine if the class is annotated
        // with a supported CDI scope
        for (Annotation annotation : annotatedClass.getAnnotations()) {
            if (WeldUtils.isValidAnnotation(annotation.annotationType(),
                                            validScopes,
                                            excludedScopes)) {
                result =true;
                break;
            }
        }

        return result;
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
    protected static boolean isValidAnnotation(Class<? extends Annotation> annotationType,
                                               Collection<String>          validTypeNames,
                                               Collection<String>          excludedTypeNames) {
        boolean result = false;

        if (validTypeNames != null && !validTypeNames.isEmpty()) {

            HashSet<String> excludedScopes = new HashSet<String>();
            if (excludedTypeNames != null) {
                excludedScopes.addAll(excludedTypeNames);
            }

            String annotationTypeName = annotationType.getName();
            if (validTypeNames.contains(annotationTypeName) && !excludedScopes.contains(annotationTypeName)) {
                result = true;
            } else if (!excludedScopes.contains(annotationTypeName)){
                // If the annotation type itself is not an excluded type, then check it's annotation
                // types, less itself (to avoid infinite recursion)
                excludedScopes.add(annotationTypeName);
                for (Annotation parent : annotationType.getAnnotations()) {
                    if (isValidAnnotation(parent.annotationType(), validTypeNames, excludedScopes)) {
                        result = true;
                        break;
                    }
                }
            }
        }

        return result;
    }


    private static Types getTypes(DeploymentContext context) {
        String metadataKey = Types.class.getName();

        Types types = (Types) context.getTransientAppMetadata().get(metadataKey);
        while (types == null) {
            context = ((ExtendedDeploymentContext) context).getParentContext();
            if (context != null) {
                types = (Types) context.getTransientAppMetadata().get(metadataKey);
            } else {
                break;
            }
        }

        return types;
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
            Object propValue = context.getAppProps().get(IMPLICIT_CDI_ENABLED_PROP);

            // If the property is not set, or it's value is true, then implicit discovery is enabled
            result = (propValue == null || Boolean.parseBoolean((String) propValue));
        }

        return result;
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
   *         true otherwise
   */
  public static boolean isValidBdaBasedOnExtensionAndBeansXml( ReadableArchive archive ) {
    boolean retVal = true;

    try {
      if ( archive.exists(META_INF_SERVICES_EXTENSION)) {
        retVal = false;
        InputStream inputStream = getBeansXmlInputStream( archive );
        if ( inputStream != null ) {
          retVal = true;  // is a valid bda
          try {
            inputStream.close();
          } catch (IOException ignore) {
          }
        }
      }
    } catch (IOException ignore) {
    }

    return retVal;
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
   * contains a beans.xml file with no version has a default bean discovery mode of all. A bean
   * archive which contains a beans.xml file with version 1.1 (or later) must specify the bean-
   * discovey-mode attribute. The default value for the attribute is annotated.
   *
   * @param beansXmlInputStream The InputStream for the beans.xml to check.
   * @return "annotated" if there is no beans.xml
   *         "all" if the bean-discovery-mode is missing
   *         "annotated" if the bean-discovery-mode is empty
   *         The value of bean-discovery-mode in all other cases.
   */
    public static String getBeanDiscoveryMode( InputStream beansXmlInputStream ) {
      if ( beansXmlInputStream == null ) {
        // there is no beans.xml.
        return "annotated";
      }

      String beanDiscoveryMode = null;
      LocalDefaultHandler handler = new LocalDefaultHandler();
      try {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser saxParser = factory.newSAXParser();
        saxParser.parse(beansXmlInputStream, handler);
      } catch ( SAXStoppedIntentionallyException exc ) {
        beanDiscoveryMode = handler.getBeanDiscoveryMode();
      } catch (Exception ignore) {
      }

      if (beanDiscoveryMode == null ) {
        return "all";
      } else if (beanDiscoveryMode.equals("")) {
        return "annotated";
      } else {
        return beanDiscoveryMode;
      }
    }

    private static class LocalDefaultHandler extends DefaultHandler {
        String beanDiscoveryMode = null;

        public void startElement(String uri, String localName,String qName, Attributes attributes) throws SAXException {
            if ( qName.equals( "beans" ) ) {
                beanDiscoveryMode = attributes.getValue("bean-discovery-mode");
                if ( beanDiscoveryMode != null ) {
                  if ( beanDiscoveryMode.equals( "" ) ) {
                    beanDiscoveryMode = "annotated";
                  }
                }
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


}
