/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2018-2023] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.openapi.impl.processor;

import fish.payara.microprofile.openapi.api.processor.OASProcessor;
import fish.payara.microprofile.openapi.impl.config.OpenApiConfiguration;
import fish.payara.microprofile.openapi.impl.model.OpenAPIImpl;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import static java.util.logging.Level.WARNING;
import java.util.logging.Logger;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.callbacks.Callback;
import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.links.Link;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;
import org.eclipse.microprofile.openapi.models.servers.Server;
import org.eclipse.microprofile.openapi.models.tags.Tag;

/**
 * A processor to obtain an application defined {@link OASFilter}, and generate
 * an pass the OpenAPI model into it for final processing.
 */
public class FilterProcessor implements OASProcessor {

    private static final Logger LOGGER = Logger.getLogger(FilterProcessor.class.getName());

    /**
     * The OASFilter implementation provided by the application.
     */
    private OASFilter filter;

    public FilterProcessor() {
        this(null);
    }

    public FilterProcessor(OASFilter filter) {
        this.filter = filter;
    }

    @Override
    public OpenAPI process(OpenAPI api, OpenApiConfiguration config) {
        try {
            if (filter == null && config.getFilter() != null) {
                filter = config.getFilter().getDeclaredConstructor().newInstance();
            }
        } catch (InstantiationException | IllegalAccessException
                | NoSuchMethodException | SecurityException | InvocationTargetException ex) {
            LOGGER.log(WARNING, "Error creating OASFilter instance.", ex);
        }
        if (filter != null) {
            return (OpenAPI) filterObject(api);
        }
        LOGGER.fine("No OASFilter provided.");
        return api;
    }

    @SuppressWarnings("unchecked")
    private Object filterObject(Object object) {
        if (object != null) {

            // If the object is a map
            if (object instanceof Map) {
                List<Object> resultsToRemove = new ArrayList<>();

                // Filter each object in the value list
                for (Object item : Map.class.cast(object).values()) {
                    Object result = filterObject(item);

                    if (result == null) {
                        resultsToRemove.add(item);
                    }
                }

                // Remove all the null values
                for (Object removeTarget : resultsToRemove) {
                    Map.class.cast(object).values().remove(removeTarget);
                }
            }

            // If the object is iterable
            if (object instanceof Iterable) {
                List<Object> resultsToRemove = new ArrayList<>();

                // Filter each object in the list
                for (Object item : Iterable.class.cast(object)) {
                    Object result = filterObject(item);

                    if (result == null) {
                        resultsToRemove.add(item);
                    }
                }

                for (Object removeTarget : resultsToRemove) {
                    Iterator<Object> iterator = Iterable.class.cast(object).iterator();
                    while (iterator.hasNext()) {
                        if (iterator.next().equals(removeTarget)) {
                            iterator.remove();
                        }
                    }
                }
            }

            // If the object is a model item
            Package pkg = object.getClass().getPackage();
            if (pkg != null && pkg.getName().startsWith(OpenAPIImpl.class.getPackage().getName())) {

                // Visit each field
                for (Field field : object.getClass().getDeclaredFields()) {
                    try {
                        field.setAccessible(true);
                        Object fieldValue = field.get(object);

                        // Filter the object
                        Object result = filterObject(fieldValue);

                        // Remove it if it's null
                        if (result == null) {
                            field.set(object, null);
                        }
                    } catch (IllegalArgumentException | IllegalAccessException ex) {
                        LOGGER.log(WARNING, "Unable to access field in OpenAPI model.", ex);
                    }
                }

                // Visit the object
                object = visitObject(object);
            }

            return object;
        }
        return null;
    }

    private Object visitObject(Object object) {
        if (object != null) {
            if (PathItem.class.isAssignableFrom(object.getClass())) {
                return filter.filterPathItem((PathItem) object);
            }
            if (Operation.class.isAssignableFrom(object.getClass())) {
                return filter.filterOperation((Operation) object);
            }
            if (Parameter.class.isAssignableFrom(object.getClass())) {
                return filter.filterParameter((Parameter) object);
            }
            if (Header.class.isAssignableFrom(object.getClass())) {
                return filter.filterHeader((Header) object);
            }
            if (RequestBody.class.isAssignableFrom(object.getClass())) {
                return filter.filterRequestBody((RequestBody) object);
            }
            if (APIResponse.class.isAssignableFrom(object.getClass())) {
                return filter.filterAPIResponse((APIResponse) object);
            }
            if (Schema.class.isAssignableFrom(object.getClass())) {
                return filter.filterSchema((Schema) object);
            }
            if (SecurityScheme.class.isAssignableFrom(object.getClass())) {
                return filter.filterSecurityScheme((SecurityScheme) object);
            }
            if (Server.class.isAssignableFrom(object.getClass())) {
                return filter.filterServer((Server) object);
            }
            if (Tag.class.isAssignableFrom(object.getClass())) {
                return filter.filterTag((Tag) object);
            }
            if (Link.class.isAssignableFrom(object.getClass())) {
                return filter.filterLink((Link) object);
            }
            if (Callback.class.isAssignableFrom(object.getClass())) {
                return filter.filterCallback((Callback) object);
            }
            if (OpenAPI.class.isAssignableFrom(object.getClass())) {
                filter.filterOpenAPI((OpenAPI) object);
            }
        }
        return object;
    }

}