/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2018-2024 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.openapi.resource.rule;

import fish.payara.microprofile.openapi.impl.OpenAPISupplier;
import fish.payara.microprofile.openapi.impl.model.OpenAPIImpl;
import fish.payara.microprofile.openapi.impl.processor.ApplicationProcessor;
import fish.payara.microprofile.openapi.impl.processor.BaseProcessor;
import fish.payara.microprofile.openapi.impl.processor.FilterProcessor;
import fish.payara.microprofile.openapi.resource.classloader.ApplicationClassLoader;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import static java.util.Arrays.asList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.glassfish.hk2.classmodel.reflect.Parser;
import org.glassfish.hk2.classmodel.reflect.ParsingContext;
import org.glassfish.hk2.classmodel.reflect.Type;
import org.glassfish.hk2.classmodel.reflect.Types;
import org.glassfish.hk2.classmodel.reflect.util.ParsingConfig;
import org.junit.Assert;

public class ApplicationProcessedDocument {

    private static Types allTypes = null;
    private final static ApplicationProcessedDocument instance = new ApplicationProcessedDocument();

    public static OpenAPI createDocument(Class<?>... extraClasses) {
        return createDocument(null, extraClasses);
    }

    public static OpenAPI createDocument(Class<? extends OASFilter> filter, Class<?>... extraClasses) {
        try {
            ApplicationClassLoader appClassLoader = new ApplicationClassLoader(new HashSet<>(asList(extraClasses)));
            OpenAPIImpl document = new OpenAPIImpl();
            // Apply base processor
            new BaseProcessor(asList(new URL("http://localhost:8080/testlocation_123"))).process(document, null);

            // Apply application processor
            File userDir = new File(System.getProperty("user.dir"));
            File modelDir = new File(userDir, "target" + File.separator + "test-classes");

            new ApplicationProcessor(OpenAPISupplier.typesToMap(getTypes(), modelDir.toURI()), getApplicationTypes(extraClasses), getApplicationTypes(extraClasses), appClassLoader)
                    .process(document, null);
            if (filter != null) {
                new FilterProcessor(filter.getDeclaredConstructor().newInstance()).process(document, null);
            }
            return document;
        } catch (IOException | IllegalAccessException | InstantiationException | InterruptedException | NoSuchMethodException | IllegalArgumentException | InvocationTargetException ex) {
            throw new AssertionError("Failed to build document.", ex);
        }
    }

    public static Types getTypes() throws IOException, InterruptedException {

        synchronized (instance) {

            if (allTypes == null) {
                File userDir = new File(System.getProperty("user.dir"));
                File modelDir = new File(userDir, "target" + File.separator + "test-classes");

                if (modelDir.exists()) {
                    ParsingContext pc = (new ParsingContext.Builder().config(new ParsingConfig() {
                        @Override
                        public Set<String> getAnnotationsOfInterest() {
                            return Collections.emptySet();
                        }

                        @Override
                        public Set<String> getTypesOfInterest() {
                            return Collections.emptySet();
                        }

                        @Override
                        public boolean modelUnAnnotatedMembers() {
                            return true;
                        }
                    })).build();
                    Parser parser = new Parser(pc);

                    parser.parse(modelDir, null);
                    Exception[] exceptions = parser.awaitTermination(100, TimeUnit.DAYS);
                    if (exceptions!=null) {
                        for (Exception e : exceptions) {
                            System.out.println("Found Exception ! : " +e);
                        }
                        Assert.assertTrue("Exceptions returned", exceptions.length==0);
                    }
                    allTypes = pc.getTypes();
                }
            }
        }
        return allTypes;
    }

    public static Set<Type> getApplicationTypes(Class<?>... extraClasses) throws IOException, InterruptedException {
        Set<Type> types = new HashSet<>();
        for (Class<?> extraClass : extraClasses) {
            Type type = getTypes().getBy(extraClass.getName());
            if (type != null) {
                types.add(type);
            }
            for (Class<?> declaredClass : extraClass.getDeclaredClasses()) {
                type = getTypes().getBy(declaredClass.getName());
                if (type != null) {
                    types.add(type);
                }
            }
        }
        return types;
    }

}
