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
package fish.payara.microprofile.openapi.api.visitor;

import fish.payara.microprofile.openapi.impl.visitor.AnnotationInfo;

import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.glassfish.hk2.classmodel.reflect.ExtensibleType;
import org.glassfish.hk2.classmodel.reflect.Type;

/**
 * The context in which a class object is being visited. For example, if a
 * method is being visited, the context will contain the current state of the
 * {@link OpenAPI}, and the current path in the API.
 */
public interface ApiContext {

    /**
     * The current {@link OpenAPI} object being operated on.
     */
    OpenAPI getApi();

    /**
     * The path of the object currently being visited. If the path is null, the
     * object has no context (e.g a POJO).
     */
    String getPath();

    /**
     * The created operation currently being worked on.
     */
    Operation getWorkingOperation();

    void addMappedExceptionResponse(String exceptionType, APIResponse exceptionResponse);

    Map<String, Set<APIResponse>> getMappedExceptionResponses();

    /**
     * @param type any class, not null
     * @return true, if the give type is a known type in this context, else
     * false
     */
    boolean isApplicationType(String type);

    /**
     * @param type any class, not null
     * @return true, if the given type is a filtered class for OpenAPI metadata
     * processing     * otherwise false
     */
    boolean isAllowedType(Type type);

    /**
     * @param type any class, not null
     * @return type, if the give type is a known type in this context, else null
     */
    Type getType(String type);

    /**
     * @return the application class loader
     */
    ClassLoader getApplicationClassLoader();

    /**
     *
     * @param type
     * @return the aggregated annotation info of type
     */
    public AnnotationInfo getAnnotationInfo(ExtensibleType<? extends ExtensibleType> type);

}
