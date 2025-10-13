/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) 2025 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/main/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 *
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 *
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 *
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.data.core.cdi.extension;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import jakarta.data.repository.Repository;
import jakarta.data.spi.EntityDefining;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.persistence.Entity;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Extension class to register the dynamic proxy for the interface annotated by Repository interface
 */
public class JakartaDataExtension implements Extension {

    private static final Logger logger = Logger.getLogger(JakartaDataExtension.class.getName());

    /**
     * This is the built-in provider name
     */
    private static final String PROVIDER_NAME = "Payara";

    private String applicationName;

    public JakartaDataExtension() {
    }

    public JakartaDataExtension(String appName) {
        this.applicationName = appName;
    }

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery afterBeanDiscovery, BeanManager manager) {
        logger.info("Finishing scanning process");
        Set<Class<?>> repositoryTypes = getRepositoryClasses();
        repositoryTypes.forEach(t -> {
            DynamicInterfaceDataProducer<Object> producer = new DynamicInterfaceDataProducer<>(t, manager, this);
            @SuppressWarnings("unchecked")
            Bean<Object> bean = manager.createBean(producer, (Class<Object>) t, producer);
            afterBeanDiscovery.addBean(bean);
        });
    }

    public Set<Class<?>> getRepositoryClasses() {
        Set<Class<?>> repositories = new HashSet<>();
        //here is the place to start to use the classgraph api to get information from the classes that are 
        //annotated by the jakarta data annotation 
        try (ScanResult result = new ClassGraph().enableAnnotationInfo().scan()) {
            repositories.addAll(locateAndGetRepositories(result));
        }
        return repositories.stream().collect(Collectors.toUnmodifiableSet());
    }

    private List<Class<?>> locateAndGetRepositories(ScanResult scan) {
        List<Class<?>> classList = scan.getClassesWithAnnotation(Repository.class).loadClasses();
        List<Class<?>> classListResult = new ArrayList<>();
        root:
        for (Class<?> clazz : classList) {
            Repository repository = clazz.getAnnotation(Repository.class);
            if (repository != null) {
                String dataProvider = repository.provider();
                boolean provide = Repository.ANY_PROVIDER.equals(dataProvider) || PROVIDER_NAME.equalsIgnoreCase(dataProvider);
                if (!provide) {
                    continue root;
                }
            }
            Type[] types = clazz.getGenericInterfaces();
            for (Type type : types) {
                if (type instanceof ParameterizedType parameterizedType) {
                    Type[] typeParams = parameterizedType.getActualTypeArguments();
                    Type entityType = typeParams.length > 0 ? typeParams[0] : null;
                    if (entityType != null) {
                        Annotation[] annotations = ((Class) entityType).getAnnotations();
                        for (Annotation annotation : annotations) {
                            Class<? extends Annotation> annotationType = annotation.annotationType();
                            if (annotationType.equals(Entity.class)) {
                                classListResult.add(clazz);
                                continue root;
                            } else if (annotationType.isAnnotationPresent(EntityDefining.class)) {
                                continue root;
                            } else {
                                continue root;
                            }
                        }
                    }
                }
            }
            classListResult.add(clazz);
        }
        return classListResult;
    }

    public String getApplicationName() {
        return applicationName;
    }
}
