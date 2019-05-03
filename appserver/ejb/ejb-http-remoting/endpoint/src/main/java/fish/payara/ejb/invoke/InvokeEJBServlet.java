/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.ejb.invoke;

import static javax.naming.Context.SECURITY_CREDENTIALS;
import static javax.naming.Context.SECURITY_PRINCIPAL;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.io.IOException;
import java.io.Reader;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.*;
import javax.json.bind.Jsonb;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.bind.JsonbBuilder;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.glassfish.internal.api.Globals;
import org.glassfish.internal.data.ApplicationRegistry;

import com.sun.enterprise.security.ee.auth.login.ProgrammaticLogin;

/**
 */
@WebServlet("/ejb/*")
public class InvokeEJBServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final Logger logger = Logger.getLogger(InvokeEJBServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.getWriter().append("Served at: ").append(request.getContextPath());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        JsonObject requestPayload = readJsonObject(request.getReader());

        String beanName = requestPayload.getString("lookup");
        if (request.getRequestURI().endsWith("lookup")) {
            try {
                response.getWriter().print(lookupBeanInterface(beanName));
            } catch (NamingException ex) {
                response.sendError(SC_INTERNAL_SERVER_ERROR,
                        "Name " + beanName + " not found when doing initial lookup.");
            } catch (Exception ex) {
                logger.log(Level.WARNING, "EJB bean lookup failed.", ex);
                response.sendError(SC_INTERNAL_SERVER_ERROR,
                        "Error while looking up EJB with name " + beanName + ": " + ex.getMessage());
            }
        } else {
            String methodName = requestPayload.getString("method");
            JsonArray argTypeNames = requestPayload.getJsonArray("argTypes");
            JsonArray argValuesJson = requestPayload.getJsonArray("argValues");
            String principal = requestPayload.getString(SECURITY_PRINCIPAL, "");
            String credentials = requestPayload.getString(SECURITY_CREDENTIALS, "");
            try {
                Object result = invokeBeanMethod(beanName, methodName, argTypeNames, argValuesJson, principal, credentials);
                response.setContentType(APPLICATION_JSON);
                response.getWriter().print(JsonbBuilder.create().toJson(result));
            } catch (NamingException ex) {
                response.sendError(SC_INTERNAL_SERVER_ERROR,
                        "Name " + beanName + " not found when invoking method " + methodName);
            } catch (Exception ex) {
                logger.log(Level.WARNING, "EJB bean method invocation failed.", ex);
                response.sendError(SC_INTERNAL_SERVER_ERROR,
                        "Error while invoking invoking method " + methodName + " on EJB with name " + beanName + ": "
                                + ex.getMessage());
            }
        }
    }

    private static JsonObject readJsonObject(Reader reader) {
        try (JsonReader jsonReader = Json.createReader(reader)) {
            return jsonReader.readObject();
        }
    }

    private static String lookupBeanInterface(String beanName) throws Exception {
        return excuteInAppContext(beanName, bean -> {
            int bangIndex = beanName.indexOf('!');
            if (bangIndex > 0) {
                return beanName.substring(bangIndex + 1);
            }
            // there should only be one interface otherwise plain name would not be allowed (portable names at least)
            return bean.getClass().getInterfaces()[0].getName();
        });
    }

    private static Object invokeBeanMethod(String beanName, String methodName, JsonArray argTypeNames,
            JsonArray argValuesJson, String principal, String credentials) throws Exception {
        return excuteInAppContext(beanName, bean -> {
            // Authenticates the caller and if successful sets the security context
            // *for the outgoing EJB call*. In other words, the security context for this
            // Servlet will not be changed.
            if (!principal.isEmpty()) {
                new ProgrammaticLogin().login(base64Decode(principal), base64Decode(credentials), null, true);
            }
            // Actually invoke the target EJB
            Class<?>[] argTypes = toClasses(argTypeNames);
            Object[] argValues = toObjects(argTypes, argValuesJson);
            return bean.getClass().getMethod(methodName, argTypes).invoke(bean, argValues);
        });
    }

    /**
     * Convert JSON encoded method parameter type names to actually Class instances 
     */
    private static Class<?>[] toClasses(JsonArray classNames) {
        return classNames.stream().map(e -> toClass(e)).toArray(Class[]::new);
    }

    private static Class<?> toClass(JsonValue classNameValue) {
        try {
            if (classNameValue instanceof JsonString) {
                return Class.forName(((JsonString) classNameValue).getString());
            }
            return Class.forName(classNameValue.toString().replace("\"", ""));
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Convert JSON encoded method parameter values to their object instances 
     */
    private static Object[] toObjects(Class<?>[] argTypes, JsonArray jsonArgValues) {
        Object[] argValues = new Object[argTypes.length];
        for (int i = 0; i < jsonArgValues.size(); i++) {
            argValues[i] =  toObject(jsonArgValues.get(i), argTypes[i]);
        }
        return argValues;
    }

    private static Object toObject(JsonValue objectValue, Class<?> type) {
        try (Jsonb jsonb = JsonbBuilder.create()) {
            return jsonb.fromJson(objectValue.toString(), type);
        } catch (Exception e) {
            // cannot really happen. It is just from java.lang.AutoCloseable interface
            throw new IllegalStateException("Problem closing Jsonb.", e);
        }
    }

    private static <T> T excuteInAppContext(String beanName, EjbOperation<T> operation) throws Exception {
        ApplicationRegistry registry = Globals.get(ApplicationRegistry.class);
        Thread currentThread = Thread.currentThread();
        if (beanName.startsWith("java:global/")) {
            String applicationName = beanName.substring(12, beanName.indexOf('/', 12));
            ClassLoader existingContextClassLoader = currentThread.getContextClassLoader();
            try {
                currentThread.setContextClassLoader(registry.get(applicationName).getAppClassLoader());
                Object bean = new InitialContext().lookup(beanName);
                return operation.execute(bean);
            } finally {
                if (existingContextClassLoader != null) {
                    currentThread.setContextClassLoader(existingContextClassLoader);
                }
            }
        }
        NamingException lastLookupError = null;
        for (String applicationName : registry.getAllApplicationNames()) {
            ClassLoader existingContextClassLoader = currentThread.getContextClassLoader();
            try {
                currentThread.setContextClassLoader(registry.get(applicationName).getAppClassLoader());
                try {
                    Object bean = new InitialContext().lookup(beanName);
                    return operation.execute(bean);
                } catch (NamingException ex) {
                    lastLookupError = ex;
                    // try next app
                }
            } finally {
                if (existingContextClassLoader != null) {
                    currentThread.setContextClassLoader(existingContextClassLoader);
                }
            }

        }
        if (lastLookupError != null) {
            throw lastLookupError;
        }
        return null;
    }

    private static String base64Decode(String input) {
        return new String(Base64.getDecoder().decode(input));
    }

    /**
     * Needed because of the {@link Exception} thrown.
     */
    interface EjbOperation<T> {

        T execute(Object bean) throws Exception;
    }
}
