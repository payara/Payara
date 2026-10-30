/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2019-2020] Payara Foundation and/or its affiliates. All rights reserved.
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
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.microprofile.openapi.impl.visitor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.glassfish.hk2.classmodel.reflect.AnnotatedElement;
import org.glassfish.hk2.classmodel.reflect.AnnotationModel;
import org.glassfish.hk2.classmodel.reflect.ClassModel;
import org.glassfish.hk2.classmodel.reflect.ExtensibleType;
import org.glassfish.hk2.classmodel.reflect.FieldModel;
import org.glassfish.hk2.classmodel.reflect.InterfaceModel;
import org.glassfish.hk2.classmodel.reflect.MethodModel;
import org.glassfish.hk2.classmodel.reflect.Parameter;

/**
 * Represents the aggregated annotations on a type, its fields and methods including annotations "inherited" from
 * super-classes and implemented interfaces. Should a field or method from a super-class be overridden the
 * {@link Annotation} closest to the represented type (the overriding one) is kept.
 */
public final class AnnotationInfo {

    private final ExtensibleType<? extends ExtensibleType> type;
    private final Map<String, AnnotationModel> typeAnnotations = new ConcurrentHashMap<>();
    private final Map<String, Map<String, AnnotationModel>> fieldAnnotations = new ConcurrentHashMap<>();
    private final Map<String, Map<String, AnnotationModel>> methodAnnotations = new ConcurrentHashMap<>();
    private final Map<String, Map<String, AnnotationModel>> methodParameterAnnotations = new ConcurrentHashMap<>();

    AnnotationInfo(ExtensibleType<? extends ExtensibleType> type) {
        this.type = type;
        init(type);
    }

    public ExtensibleType<? extends ExtensibleType> getType() {
        return type;
    }

    /**
     * Version of {@link Class#getAnnotation(Class)} also considering
     * annotations "inherited" from super-types for this
     * {@link AnnotationInfo#getType()}.
     *
     * @param annotationType annotation type to check
     * @return the annotation of the given type if present or {@code null}
     * otherwise
     */
    @SuppressWarnings("unchecked")
    public AnnotationModel getAnnotation(Class<? extends Annotation> annotationType) {
        return typeAnnotations.get(annotationType.getName());
    }

    /**
     * Version of {@link Field#getAnnotation(Class)} also considering annotations "inherited" from overridden fields
     * of super-types.
     * 
     * @param annotationType annotation type to check
     * @param method         must be a {@link Field} defined or inherited by this {@link AnnotationInfo#getType()}
     * @return the annotation of the given type if present or {@code null} otherwise
     */
    @SuppressWarnings("unchecked")
    public AnnotationModel getAnnotation(Class<? extends Annotation> annotationType, FieldModel field) {
        return fieldAnnotations.get(field.getName()).get(annotationType.getName());
    }

    /**
     * Version of {@link Method#getAnnotation(Class)} also considering annotations "inherited" from overridden methods
     * of super-types.
     * 
     * @param annotationType annotation type to check
     * @param method         must be a {@link Method} defined or inherited by this {@link AnnotationInfo#getType()}
     * @return the annotation of the given type if present or {@code null} otherwise
     */
    @SuppressWarnings("unchecked")
    public AnnotationModel getAnnotation(Class<? extends Annotation> annotationType, MethodModel method) {
        return methodAnnotations.get(getSignature(method)).get(annotationType.getName());
    }

    /**
     * Version of {@link Parameter#getAnnotation(Class)} also considering annotations "inherited" from overridden
     * methods of super-types.
     * 
     * @param annotationType annotation type to check
     * @param parameter      must be this a {@link Parameter} of a {@link Method} defined or inherited by this
     *                       {@link AnnotationInfo#getType()}
     * @return the annotation of the given type if present or {@code null} otherwise
     */
    @SuppressWarnings("unchecked")
    public AnnotationModel getAnnotation(Class<? extends Annotation> annotationType, Parameter parameter) {
        return methodParameterAnnotations.get(getIdentifier(parameter)).get(annotationType.getName());
    }

    @SuppressWarnings("unchecked")
    public String getAnnotationValue(Class<? extends Annotation> annotationType) {
        AnnotationModel model = getAnnotation(annotationType);
        if (model != null) {
            return model.getValue("value", String.class);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public String getAnnotationValue(Class<? extends Annotation> annotationType, AnnotatedElement parameter) {
        AnnotationModel model = getAnnotation(annotationType, parameter);
        if (model != null) {
            return model.getValue("value", String.class);
        }
        return null;
    }

    /**
     * Version of {@link AnnotatedElement#getAnnotation(Class)} also considering annotations "inherited" from
     * super-types.
     * 
     * @param annotationType annotation type to check
     * @param element        must be this {@link AnnotationInfo#getType()}'s {@link Class} or a {@link Field} or
     *                       {@link Method} defined or inherited by it or a {@link Parameter} of such a method.
     * @return the annotation of the given type if present or {@code null} otherwise
     */
    public AnnotationModel getAnnotation(Class<? extends Annotation> annotationType, AnnotatedElement element) {
        if (element instanceof ClassModel) {
            return getAnnotation(annotationType);
        } else if (element instanceof FieldModel) {
            return getAnnotation(annotationType, (FieldModel) element);
        } else if (element instanceof MethodModel) {
            return getAnnotation(annotationType, (MethodModel) element);
        } else if (element instanceof Parameter) {
            return getAnnotation(annotationType, (Parameter) element);
        }
        return null;
    }

    /**
     * Version of {@link Class#isAnnotationPresent(Class)} also considering annotations "inherited" from super-types.
     * 
     * @param annotationType annotation type to check
     * @return true in case it is present at this class or any of its super-types
     */
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
        return getAnnotation(annotationType) != null;
    }

    /**
     * Version of {@link Field#isAnnotationPresent(Class)} also considering annotations "inherited" from super-types.
     * 
     * @param annotationType annotation type to check
     * @return true in case it is present at the given field. The field must be defined in this
     *         {@link AnnotationInfo#getType()} or any of its super-types.
     */
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType, FieldModel field) {
        return getAnnotation(annotationType, field) != null;
    }

    /**
     * Version of {@link Method#isAnnotationPresent(Class)} also considering annotations "inherited" from super-types.
     * 
     * @param annotationType annotation type to check
     * @param method         the method checked for annotation. The method must be defined or inherited by this
     *                       {@link AnnotationInfo#getType()} or any of its super-types.
     * @return true in case it is present at the given method, else false
     */
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType, MethodModel method) {
        return getAnnotation(annotationType, method) != null;
    }

    /**
     * Version of {@link Parameter#isAnnotationPresent(Class)} also considering annotations "inherited" from super-types.
     * 
     * @param annotationType annotation type to check
     * @param parameter      the parameter checked for annotations. The parameter must belong to a method defined or
     *                       inherited by this {@link AnnotationInfo#getType()}
     * @return true in case it is present at the given parameter, else false
     */
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType, Parameter parameter) {
        return getAnnotation(annotationType, parameter) != null;
    }

    /**
     * Version of {@link AnnotatedElement#isAnnotationPresent(Class)} also considering annotations "inherited" from
     * super-types.
     * 
     * @param annotationType annotation type to check
     * @param element        must be this {@link AnnotationInfo#getType()}'s {@link Class} or a {@link Field} or
     *                       {@link Method} defined or inherited by it or a {@link Parameter} of such a method.
     * @return true in case it is present at the given element, else false
     */
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType, AnnotatedElement element) {
        return getAnnotation(annotationType, element) != null;
    }

    /**
     * @see #isAnnotationPresent(Class, AnnotatedElement)
     */
    @SafeVarargs
    public final boolean isAnyAnnotationPresent(AnnotatedElement element, Class<? extends Annotation>... annotationTypes) {
        for (Class<? extends Annotation> annotationType : annotationTypes) {
            if (isAnnotationPresent(annotationType, element)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Counts the annotation on a {@link Parameter} including those present on same parameter of an overridden method
     * should the method be overridden.
     * 
     * @param parameter the parameter of which to count annotations for
     * @return the number of annotation present on the given {@link Parameter} including annotations present on the same
     *         parameter of an potentially overridden method.
     */
    public int getAnnotationCount(Parameter parameter) {
        return methodParameterAnnotations.get(getIdentifier(parameter)).size();
    }

      private void init(ExtensibleType<? extends ExtensibleType> type) {
          // recurse first so that re-stated annotations "override"
          ExtensibleType<? extends ExtensibleType> supertype = type.getParent();
          if (supertype != null) {
              init(supertype);
          }
          for (InterfaceModel implementedInterface : type.getInterfaces()) {
              if (implementedInterface != null && implementedInterface != type) {
                    init(implementedInterface);
              }
          }

          // collect annotations
          putAll(type.getAnnotations(), typeAnnotations);
          if (type instanceof ClassModel) {
              for (FieldModel field : ((ClassModel) type).getFields()) {
                  putAll(
                          field.getAnnotations(),
                          fieldAnnotations.computeIfAbsent(field.getName(), key -> new ConcurrentHashMap<>())
                  );
              }
          }
          for (MethodModel method : type.getMethods()) {
              putAll(
                      method.getAnnotations(),
                      methodAnnotations.computeIfAbsent(getSignature(method), key -> new ConcurrentHashMap<>())
              );
              for (Parameter parameter : method.getParameters()) {
                  putAll(
                          parameter.getAnnotations(),
                          methodParameterAnnotations.computeIfAbsent(getIdentifier(parameter), key -> new ConcurrentHashMap<>())
                  );
              }
          }

    }

    private static void putAll(Collection<AnnotationModel> annotations, Map<String, AnnotationModel> map) {
        for (AnnotationModel a : annotations) {
            map.put(a.getType().getName(), a);
        }
    }

    private static String getIdentifier(Parameter parameter) {
        return getSignature(parameter.getMethod()) + "#" + parameter.getIndex();
    }

    private static String getSignature(MethodModel method) {
        StringBuilder signature = new StringBuilder();
        signature.append(method.getName());
        signature.append('(');
        for (String parameterType : method.getArgumentTypes()) {
            signature.append(parameterType).append(", ");
        }
        signature.append(')');
        return signature.toString();
    }
}