/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019-2024 Payara Foundation and/or its affiliates. All rights reserved.
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


import fish.payara.microprofile.openapi.impl.OpenAPISupplier;
import fish.payara.microprofile.openapi.resource.classloader.ApplicationClassLoader;
import fish.payara.microprofile.openapi.resource.rule.ApplicationProcessedDocument;
import fish.payara.microprofile.openapi.test.app.OpenApiApplicationTest;

import java.io.File;
import java.io.IOException;
import org.junit.Test;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import static java.util.stream.Collectors.toSet;
import org.glassfish.hk2.classmodel.reflect.Type;
import static org.junit.Assert.*;

public class OpenApiWalkerTest extends OpenApiApplicationTest {

    /**
     * When the OpenAPI schema is created, a lot of lookups of classes included
     * in the schema are made.Therefore, it is required to keep to asymptotic
     * complexity as low as possible.Keeping the classes sorted in a TreeSet
     * ensures O(log(n)) complexity, as the tree is re-built on insert.<p>
     * Inside the Set (the internal contract defined by a private field is Set,
     * not TreeSet), regardless of the actual implementation used, all the
     * inserted classes should be foundable afterwards.
     * <p>
     * This method tests a fix for issue
     * {@link https://github.com/payara/Payara/issues/4489}, which pointed out
     * the original custom TreeSet comparator did a reduction to the space of
     * possible classes on lookup, making it impossible for a large subset of
     * classes to be found, even if present in the TreeSet.
     * <p>
     * The compared objects inside the internal comparator supplied to the Set
     * implementation are of type "Class". And classes have no natural distinct
     * ordinal numbers in Java. One way to get an ordinal number for a Class is
     * to use it's fully qualified name and compare the distance to the other
     * compared class in the space consisting of other class names. This way,
     * "more similar" strings have closer cartesian distance. And as String
     * representation of a Class is used, fast lookup is ensured.
     *
     * @throws java.lang.NoSuchFieldException
     * @throws java.lang.IllegalAccessException
     * @throws java.io.IOException
     * @throws java.lang.InterruptedException
     */
    @Test
    public void testClassOrderingAndSearch() throws NoSuchFieldException, IllegalAccessException, IOException, InterruptedException {
        final Set<Class<?>> testedClasssses = new LinkedHashSet<>();

        ApplicationClassLoader appClassLoader = new ApplicationClassLoader(testedClasssses);

        File userDir = new File(System.getProperty("user.dir"));
        File modelDir = new File(userDir, "target" + File.separator + "test-classes");

        final OpenApiWalker openApiWalker = new OpenApiWalker(getDocument(),
                OpenAPISupplier.typesToMap(ApplicationProcessedDocument.getTypes(), modelDir.toURI()),
                ApplicationProcessedDocument.getApplicationTypes(testedClasssses.toArray(new Class<?>[0])),
                appClassLoader, true);
        final java.lang.reflect.Field sortedClassesField = OpenApiWalker.class.getDeclaredField("allowedTypes");
        assertEquals(Set.class, sortedClassesField.getType()); // Ensure fast lookup is possible with at least any Set
        try {
            sortedClassesField.setAccessible(true);
            final Object possibleTreeSet = sortedClassesField.get(openApiWalker);
            assertEquals(TreeSet.class, possibleTreeSet.getClass());
            final Set sortedClasses = ((TreeSet<Type>) possibleTreeSet).stream().map(Type::getName).collect(toSet());
            assertNotNull(sortedClasses);

            // All tested Classes must be foundable inside the OpenApi's internal sorted classes
            testedClasssses.forEach(clazz -> assertTrue(sortedClasses.contains(clazz.getName())));
        } finally {
            sortedClassesField.setAccessible(false);
        }
    }

}