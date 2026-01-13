/*
 *
 * Copyright (c) 2025 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
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
package fish.payara.tools.dev.dto;

import fish.payara.tools.dev.model.InstanceStats;
import fish.payara.tools.dev.model.Record;
import jakarta.enterprise.inject.spi.Bean;

import java.lang.annotation.Annotation;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Full representation of a bean including lifecycle statistics.
 */
public class BeanDTO {
    private final String beanClass;
    private final String scope;
    private final Set<String> qualifiers;
    private final Set<String> types;
    private final String name;
    private final Set<String> stereotypes;
    private final boolean alternative;
    private final String producedBy; // if created via @Produces

    private final int currentCount;
    private final int createdCount;
    private final Instant lastCreated;
    private final int maxCount;
    private final int destroyedCount;

    private final List<Record> creationRecords;
    private final List<Record> destructionRecords;

    public BeanDTO(Bean<?> bean) {
        this(bean, null, null);
    }

    public BeanDTO(Bean<?> bean, InstanceStats stats, String producedBy) {
        this.beanClass = bean.getBeanClass().getName();

        Class<?> scopeAnnotation = bean.getScope();
        this.scope = (scopeAnnotation != null) ? scopeAnnotation.getSimpleName() : "Unknown";

        this.qualifiers = bean.getQualifiers().stream()
                .map(BeanDTO::formatAnnotation)
                .collect(Collectors.toSet());

        this.types = bean.getTypes().stream()
                .filter(t -> !t.equals(Object.class)
                        && !t.getTypeName().equals(bean.getBeanClass().getName()))
                .map(Object::toString)
                .collect(Collectors.toSet());

        this.name = bean.getName();

        this.stereotypes = bean.getStereotypes().stream()
                .map(Class::getSimpleName)
                .collect(Collectors.toSet());

        this.alternative = bean.isAlternative();
        this.producedBy = producedBy;

        if (stats != null) {
            this.currentCount = stats.getCurrentCount().get();
            this.createdCount = stats.getCreatedCount().get();
            this.lastCreated = stats.getLastCreated().get();
            this.maxCount = stats.getMaxCount().get();
            this.destroyedCount = stats.getDestroyedCount().get();
            this.creationRecords = List.copyOf(stats.getCreationRecords());
            this.destructionRecords = List.copyOf(stats.getDestructionRecords());
        } else {
            this.currentCount = 0;
            this.createdCount = 0;
            this.lastCreated = null;
            this.maxCount = 0;
            this.destroyedCount = 0;
            this.creationRecords = List.of();
            this.destructionRecords = List.of();
        }
    }

    private static String formatAnnotation(Annotation a) {
        if (a.annotationType().getDeclaredMethods().length == 0) {
            return "@" + a.annotationType().getSimpleName();
        }
        return "@" + a.annotationType().getSimpleName() + a.toString();
    }

    // getters
    public String getBeanClass() { return beanClass; }
    public String getScope() { return scope; }
    public Set<String> getQualifiers() { return qualifiers; }
    public Set<String> getTypes() { return types; }
    public String getName() { return name; }
    public Set<String> getStereotypes() { return stereotypes; }
    public boolean isAlternative() { return alternative; }
    public String getProducedBy() { return producedBy; }

    public int getCurrentCount() { return currentCount; }
    public int getCreatedCount() { return createdCount; }
    public Instant getLastCreated() { return lastCreated; }
    public int getMaxCount() { return maxCount; }
    public int getDestroyedCount() { return destroyedCount; }

    public List<Record> getCreationRecords() { return creationRecords; }
    public List<Record> getDestructionRecords() { return destructionRecords; }
}
