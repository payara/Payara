/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.data.core.cdi.extension;

import jakarta.data.repository.Repository;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link JakartaDataExtension#getRepositoryClasses()}.
 */
public class JakartaDataExtensionTest {

    // Entity with @Entity declared first (declaration order matches bytecode order)
    @Entity
    @Table(name = "entity_first")
    static class EntityFirstEntity {}

    // Entity with @Table declared BEFORE @Entity — triggers the annotation-ordering bug
    @Table(name = "table_first")
    @Entity
    static class TableFirstEntity {}

    // Simple single-type-param parent so getRepositoryClasses() can extract the entity type
    interface SingleParam<E> {}

    @Repository
    interface EntityFirstRepo extends SingleParam<EntityFirstEntity> {}

    @Repository
    interface TableFirstRepo extends SingleParam<TableFirstEntity> {}

    @Repository(provider = "Payara")
    interface ExplicitPayaraProviderRepo extends SingleParam<EntityFirstEntity> {}

    @Repository(provider = "OtherProvider")
    interface OtherProviderRepo extends SingleParam<EntityFirstEntity> {}

    private JakartaDataExtension extensionWith(Class<?>... repoClasses) {
        Set<String> names = new HashSet<>();
        for (Class<?> c : repoClasses) {
            names.add(c.getName());
        }
        return new JakartaDataExtension("testApp", names);
    }

    @Test
    public void repositoryWithEntityAnnotatedFirstIsDiscovered() {
        Set<Class<?>> result = extensionWith(EntityFirstRepo.class).getRepositoryClasses();
        assertTrue("Repository whose entity declares @Entity first should be discovered",
                result.contains(EntityFirstRepo.class));
    }

    @Test
    public void repositoryWithTableAnnotatedBeforeEntityIsDiscovered() {
        Set<Class<?>> result = extensionWith(TableFirstRepo.class).getRepositoryClasses();
        assertTrue("Repository whose entity declares @Table before @Entity should still be discovered",
                result.contains(TableFirstRepo.class));
    }

    @Test
    public void repositoryWithExplicitPayaraProviderIsDiscovered() {
        Set<Class<?>> result = extensionWith(ExplicitPayaraProviderRepo.class).getRepositoryClasses();
        assertTrue("Repository with provider = \"Payara\" should be discovered",
                result.contains(ExplicitPayaraProviderRepo.class));
    }

    @Test
    public void repositoryForDifferentProviderIsExcluded() {
        Set<Class<?>> result = extensionWith(OtherProviderRepo.class).getRepositoryClasses();
        assertFalse("Repository targeting a different named provider should not be discovered by Payara",
                result.contains(OtherProviderRepo.class));
    }
}
