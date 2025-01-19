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
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.deployment.JandexIndexer;
import org.jboss.jandex.AnnotationInstance;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import static org.glassfish.weld.connector.WeldUtils.cdiEnablingAnnotationClasses;

public class JandexClassModel implements AnnotationClassModel {
    private final JandexIndexer jandexIndexer = Globals.getDefaultHabitat().getService(JandexIndexer.class);
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasCDIEnablingAnnotations(DeploymentContext deploymentContext, Collection<URI> paths) {
        if (isRootPackage(deploymentContext, paths)) {
//            var index = getIndex(deploymentContext);
//            return cdiEnablingAnnotationClasses.stream()
//                    .anyMatch(annotationClass -> !index.getAnnotations(annotationClass).isEmpty());
            return false;
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getCDIEnablingAnnotations(DeploymentContext context) {
        var index = jandexIndexer.getRootIndex(context);
        Set<String> result = new HashSet<>();
        result.addAll(cdiEnablingAnnotationClasses.stream()
                .filter(annotationClass -> !index.getAnnotations(annotationClass).isEmpty())
                .map(Class::getName)
                .collect(Collectors.toSet()));
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<String> getCDIAnnotatedClassNames(DeploymentContext context, Set<String> cdiEnablingAnnotations) {
        var index = jandexIndexer.getRootIndex(context);
        Set<String> result = new HashSet<>();
        cdiEnablingAnnotations.forEach(annotation -> result.addAll(index.getAnnotations(annotation).stream()
                .map(annotationInstance -> annotationInstance.target().asClass().name().toString())
                .collect(Collectors.toSet())));
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<String> getInjectionTargetClassNames(DeploymentContext deploymentContext, Collection<String> knownClassNames) {
        var index = jandexIndexer.getRootIndex(deploymentContext);
        Set<String> result = new HashSet<>();
        for (AnnotationInstance annotationInstance : index.getAnnotations(Inject.class)) {
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
                case  RECORD_COMPONENT:
                    className = annotationInstance.target().asRecordComponent().declaringClass().name().toString();
                    break;
            }
            if (className != null && knownClassNames.contains(className)) {
                result.add(className);
            }
        }
        return result;
    }

    private boolean isRootPackage(DeploymentContext deploymentContext, Collection<URI> paths) {
        return paths.size() == 1 && paths.stream().findFirst().get().equals(deploymentContext.getSource().getURI());
    }
}
