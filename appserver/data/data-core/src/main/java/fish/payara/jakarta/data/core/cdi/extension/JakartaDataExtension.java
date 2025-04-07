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
 *     file and include the License file at glassfish/legal/LICENSE.txt.
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
package fish.payara.jakarta.data.core.cdi.extension;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.DataRepository;
import jakarta.data.repository.Repository;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Extension class to register the dynamic proxy for the interface annotated by Repository interface 
 */
public class JakartaDataExtension implements Extension {

    private static final Logger logger = Logger.getLogger(JakartaDataExtension.class.getName());

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery afterBeanDiscovery, BeanManager manager) {
        logger.info("Finishing scanning process");
        Set<Class<?>> repositoryTypes = getRepositoryClasses();
        repositoryTypes.forEach(t -> {
            afterBeanDiscovery.addBean(new DynamicInterfaceDataProducer<>(t, manager, this));
        });
    }

    public Set<Class<?>> getRepositoryClasses() {
        Set<Class<?>> repositories = new HashSet<>();
        //here is the place to start to use the classgraph api to get information from the classes that are 
        //annotated by the jakarta data annotation 
        try (ScanResult result = new ClassGraph().enableAllInfo().scan()) {
            repositories.addAll(locateAndGetRepositories(result));
        }
        return repositories.stream().filter(cl -> {
            List<Class<?>> interfaces = Arrays.asList(cl.getInterfaces());
            return interfaces.contains(CrudRepository.class) || interfaces.contains(BasicRepository.class)
                    || interfaces.contains(DataRepository.class);
        }).collect(Collectors.toUnmodifiableSet());
    }

    private List<Class<DataRepository>> locateAndGetRepositories(ScanResult scan) {
        //getting information from the classes annotated by the Respository class
        List<Class<DataRepository>> listDataRepository= scan.getClassesWithAnnotation(Repository.class).getInterfaces()
                .filter(ci -> ci.implementsInterface(DataRepository.class))
                .loadClasses(DataRepository.class);
        return listDataRepository.stream().filter(this::validate).collect(Collectors.toList());
    }

    public boolean validate(Class<?> type) {
        Optional<Class<?>> entity = getEntity(type);
        return entity.map(this::getSupportedAnnotation).isPresent();
    }

    private Annotation getSupportedAnnotation(Class<?> c) {
        return c.getAnnotation(jakarta.persistence.Entity.class);
    }

    private Optional<Class<?>> getEntity(Class<?> repository) {
        Type[] interfaces = repository.getGenericInterfaces();
        if (interfaces.length == 0) {
            return Optional.empty();
        }
        if (interfaces[0] instanceof ParameterizedType interfaceType) {
            return Optional.ofNullable(getEntityFromParametersInterface(interfaceType));
        } else {
            return Optional.empty();
        }
    }

    private Class<?> getEntityFromParametersInterface(ParameterizedType param) {
        Type[] arguments = param.getActualTypeArguments();
        if (arguments.length == 0) {
            return null;
        }
        Type argument = arguments[0];
        if (argument instanceof Class<?> entity) {
            return entity;
        }
        return null;
    }
}
