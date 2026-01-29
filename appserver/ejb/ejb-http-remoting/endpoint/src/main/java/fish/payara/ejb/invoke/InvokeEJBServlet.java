/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019-2021 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.ejb.invoke;

import com.sun.enterprise.security.ee.auth.login.ProgrammaticLogin;
import fish.payara.ejb.http.admin.EjbInvokerConfiguration;

import fish.payara.ejb.http.endpoint.EjbOverHttpResource;

import org.glassfish.internal.api.Globals;
import org.glassfish.internal.data.ApplicationRegistry;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import static javax.naming.Context.SECURITY_CREDENTIALS;
import static javax.naming.Context.SECURITY_PRINCIPAL;
import static jakarta.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * @deprecated Replaced by {@link EjbOverHttpResource}
 */
@Deprecated
@WebServlet("/ejb/*")
public class InvokeEJBServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger logger = Logger.getLogger(InvokeEJBServlet.class.getName());

    private boolean securityEnabled;

    private String[] roles;

    @Override
    public void init() throws ServletException {
            EjbInvokerConfiguration config = Globals.getDefaultBaseServiceLocator()
                .getService(EjbInvokerConfiguration.class);
            roles = config.getRoles().split(",");
            securityEnabled = Boolean.parseBoolean(config.getSecurityEnabled());
     }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.getWriter().append("Served at: ").append(request.getContextPath());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        JsonObject requestPayload = readJsonObject(request.getReader());

        if(securityEnabled) {
            for(String role : roles) {
                if(!request.isUserInRole(role)) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                }
            }
        }

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
                Invocation invocation = invokeBeanMethod(beanName, methodName, argTypeNames, argValuesJson, principal, credentials);
                response.setContentType(APPLICATION_JSON);
                if (invocation.result == null) {
                    // JSON-B cannot marshall null
                    response.getWriter().print("null");
                } else {
                    Type resultType = invocation.method.getGenericReturnType();
                    response.getWriter().print(JsonbBuilder.create().toJson(invocation.result, resultType));
                }
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
            // in fact, there can be some implementation-specific interfaces in the proxy as well
            return bean.getClass().getInterfaces()[0].getName();
        });
    }

    private static Invocation invokeBeanMethod(String beanName, String methodName, JsonArray argTypeNames,
            JsonArray argValuesJson, String principal, String credentials) throws Exception {
        return excuteInAppContext(beanName, bean -> {
            // Authenticates the caller and if successful sets the security context
            // *for the outgoing EJB call*. In other words, the security context for this
            // Servlet will not be changed.
            if (!principal.isEmpty()) {
                new ProgrammaticLogin().login(base64Decode(principal), base64Decode(credentials), null, true);
            }
            // Actually invoke the target EJB
            Invocation invocation = new Invocation(bean, methodName, argTypeNames);
            invocation.setArgs(argValuesJson);
            invocation.invoke();
            return invocation;
        });
    }

    static class Invocation {
        private final Object bean;
        private Type[] argTypes;
        private Method method;
        private Object[] argValues;
        private Object result;

        Invocation(Object bean, String methodName, JsonArray argTypes) throws NoSuchMethodException {
            this.bean = bean;
            Class<?>[] argTypeClasses = toClasses(argTypes);
            // we look up the method in the interfaces, because proxy classes do not retain generic information
            this.method = findBusinessMethodDeclaration(methodName, argTypeClasses);
            this.argTypes = method.getGenericParameterTypes();
        }

        private Method findBusinessMethodDeclaration(String methodName, Class<?>[] argTypeClasses) throws NoSuchMethodException {
            for (Class<?> intf : bean.getClass().getInterfaces()) {
                try {
                    return intf.getMethod(methodName, argTypeClasses);
                } catch (NoSuchMethodException e) {
                    // try further
                }
            }
            throw new NoSuchMethodException("No method matching " + methodName + "(" + Arrays.toString(argTypeClasses) + ") found in business interface");
        }

        void setArgs(JsonArray argValues) {
            this.argValues = toObjects(this.argTypes, argValues);
        }

        void invoke() throws InvocationTargetException, IllegalAccessException {
            this.result = method.invoke(bean, argValues);
        }
    }

    /**
     * Convert JSON encoded method parameter type names to actually Class instances 
     */
    private static Class<?>[] toClasses(JsonArray classNames) {
        return classNames.stream().map(e -> toClass(e)).toArray(Class[]::new);
    }

    private static Class<?> toClass(JsonValue classNameValue) {
        try {
            String className = classNameValue instanceof JsonString 
                    ? ((JsonString) classNameValue).getString() 
                    : classNameValue.toString().replace("\"", "");
            switch (className) {
                case "int":
                    return int.class;
                case "long":
                    return long.class;
                case "short":
                    return short.class;
                case "byte":
                    return byte.class;
                case "boolean":
                    return boolean.class;
                case "float":
                    return float.class;
                case "double":
                    return double.class;
                case "char":
                    return char.class;
                case "void":
                    return void.class;
                default:
                    return Class.forName(className, true, Thread.currentThread().getContextClassLoader());
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Convert JSON encoded method parameter values to their object instances 
     */
    private static Object[] toObjects(Type[] argTypes, JsonArray jsonArgValues) {
        Object[] argValues = new Object[argTypes.length];
        for (int i = 0; i < jsonArgValues.size(); i++) {
            argValues[i] =  toObject(jsonArgValues.get(i), argTypes[i]);
        }
        return argValues;
    }

    private static Object toObject(JsonValue objectValue, Type type) {
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
