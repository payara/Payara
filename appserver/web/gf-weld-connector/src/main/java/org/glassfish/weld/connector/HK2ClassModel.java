/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2025] Payara Foundation and/or its affiliates. All rights reserved.
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
 * file and include the License file at glassfish/legal/LICENSE.txt.
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
package org.glassfish.weld.connector;

import jakarta.inject.Inject;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.deployment.common.DeploymentUtils;
import org.glassfish.hk2.classmodel.reflect.AnnotationModel;
import org.glassfish.hk2.classmodel.reflect.AnnotationType;
import org.glassfish.hk2.classmodel.reflect.ClassModel;
import org.glassfish.hk2.classmodel.reflect.FieldModel;
import org.glassfish.hk2.classmodel.reflect.MethodModel;
import org.glassfish.hk2.classmodel.reflect.Type;
import org.glassfish.hk2.classmodel.reflect.Types;
import org.glassfish.internal.deployment.ExtendedDeploymentContext;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import static org.glassfish.weld.connector.WeldUtils.cdiEnablingAnnotations;

class HK2ClassModel implements AnnotationClassModel {
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasCDIEnablingAnnotations(DeploymentContext deploymentContext, Collection<URI> paths) {
        final Set<String> exclusions = new HashSet<>();
        for (final Type type : getAllTypes(deploymentContext, paths)) {
            if (!(type instanceof AnnotationType) && type.wasDefinedIn(paths)) {
                for (final AnnotationModel am : type.getAnnotations()) {
                    final AnnotationType at = am.getType();
                    if (isCDIEnablingAnnotation(at, exclusions)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getCDIEnablingAnnotations(DeploymentContext context) {
        final Set<String> result = new HashSet<>();

        final Set<String> exclusions = new HashSet<>();
        for (final Type type : getAllTypes(context, List.of())) {
            if (!(type instanceof AnnotationType)) {
                for (final AnnotationModel am : type.getAnnotations()) {
                    final AnnotationType at = am.getType();
                    if (isCDIEnablingAnnotation(at, exclusions)) {
                        result.add(at.getName());
                    }
                }
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<String> getCDIAnnotatedClassNames(DeploymentContext context, Set<String> cdiEnablingAnnotations) {
        final Set<String> result = new HashSet<>();

        for (final Type type : getAllTypes(context, List.of())) {
            if (!(type instanceof AnnotationType)) {
                for (final AnnotationModel am : type.getAnnotations()) {
                    final AnnotationType at = am.getType();
                    if (cdiEnablingAnnotations.contains(at.getName())) {
                        result.add(type.getName());
                        break;
                    }
                }
            }
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<String> getInjectionTargetClassNames(DeploymentContext deploymentContext, Collection<String> knownClassNames) {
        final Set<String> result = new HashSet<>();

        List<Type> types = getAllTypes(deploymentContext, List.of());
        Map<String, Type> byClassName = new HashMap<>();
        types.stream().filter(type -> type instanceof ClassModel)
                .forEach(type -> byClassName.put(type.getName(), type));
        for (String knownClassName : knownClassNames) {
            Type type = byClassName.get(knownClassName);

            if (type != null && type instanceof ClassModel) {
                boolean injectionTarget = false;

                Collection<FieldModel> fieldModels = ((ClassModel) type).getFields();
                for (FieldModel fieldModel : fieldModels) {
                    injectionTarget = annotatedWithInject(fieldModel.getAnnotations());
                    if (injectionTarget) {
                        break;
                    }
                }

                if (!injectionTarget) {
                    Collection<MethodModel> methodModels = type.getMethods();
                    for (MethodModel methodModel : methodModels) {
                        injectionTarget = annotatedWithInject(methodModel.getAnnotations());
                        if (injectionTarget) {
                            break;
                        }
                    }
                }

                if (injectionTarget) {
                    result.add(type.getName());
                }
            }
        }

        return result;
    }

    private static List<Type> getAllTypes(DeploymentContext context, Collection<URI> paths) {
        final Types types = getTypes(context);

        List<Type> allTypes;
        if (types == null) {
            allTypes = new ArrayList<>();
        } else {
            allTypes = new ArrayList<>(types.getAllTypes().size());
            if (paths.isEmpty()) {
                allTypes.addAll(types.getAllTypes());
            } else {
                allTypes.addAll(types.getAllTypes().stream().filter(type -> type.wasDefinedIn(paths)).collect(Collectors.toList()));
            }
        }

        Map<String, DeploymentUtils.WarLibraryDescriptor> cache = DeploymentUtils.getWarLibraryCache();
        for (URI path : paths.isEmpty() ? cache.keySet().stream().map(Path::of).map(Path::toUri)
                .collect(Collectors.toList()) : paths) {
            var descriptor = cache.get(path.getRawPath());
            if (descriptor != null) {
                allTypes.addAll(descriptor.getTypes());
            }
        }
        return allTypes;
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

    /**
     * Determine if the specified annotation type is a CDI-enabling annotation
     *
     * @param annotationType    The annotation type to check
     * @param exclusions The Set of annotation type names that should be excluded from the analysis
     *
     * @return true, if the specified annotation type qualifies as a CDI enabler; Otherwise, false
     */
    private static boolean isCDIEnablingAnnotation(AnnotationType annotationType,
                                                   Set<String>    exclusions) {

        final String annotationTypeName = annotationType.getName();
        if (cdiEnablingAnnotations.contains(annotationTypeName)) {
            return true;
        } else if (exclusions.add(annotationTypeName)) {
            // If the annotation type itself is not an excluded type, then check its annotation
            // types, exclude itself to avoid infinite recursion
            for (AnnotationModel parent : annotationType.getAnnotations()) {
                if (isCDIEnablingAnnotation(parent.getType(), exclusions)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean annotatedWithInject(Collection<AnnotationModel> annotationModels) {
        boolean injectionTarget = false;
        for (AnnotationModel annotationModel : annotationModels) {
            if (annotationModel.getType().getName().equals(Inject.class.getName())) {
                injectionTarget = true;
                break;
            }
        }

        return injectionTarget;
    }

    /**
     * Determine if the specified annotation type is a CDI-enabling annotation
     *
     * @param annotationType The annotation type to check
     *
     * @return true, if the specified annotation type qualifies as a CDI enabler; Otherwise, false
     */
    private static boolean isCDIEnablingAnnotation(AnnotationType annotationType) {
        return isCDIEnablingAnnotation(annotationType, new HashSet<>());
    }
}
