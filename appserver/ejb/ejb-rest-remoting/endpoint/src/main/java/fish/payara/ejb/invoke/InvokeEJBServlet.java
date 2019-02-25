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
import java.util.Base64;
import java.util.List;
import java.util.function.Supplier;

import javax.json.Json;
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

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	    
	    JsonObject requestPayload = 
	            Json.createReader(request.getReader())
	                .readObject();
	    
	    if (request.getRequestURI().endsWith("lookup")) {
    	    boolean success = excuteInAppContext(() -> {
    	        
    	        try {
                    response.getWriter().print(
                            new InitialContext().lookup(requestPayload.getString("lookup"))
                                                .getClass()
                                                .getInterfaces()[0]
                                                .getName());
                    return true;
                } catch (IOException | NamingException e) {
                    // Ignore for now
                }
    	        
    	        return false;
    	    });
    	    
    	    if (!success) {
    	        response.sendError(SC_INTERNAL_SERVER_ERROR, "Name " + requestPayload.getString("lookup") + " not found when doing initial lookup");
    	    }
    	    
    	    return;
	    }
	    
	    // Convert JSON encoded method parameter type names to actually Class instances
        Class<?>[] argTypes = 
            requestPayload.getJsonArray("argTypes").stream()
                          .map(e -> toClass(e))
                          .toArray(Class[]::new);
        
        // Convert JSON encoded method parameter values to their object instances
        List<JsonValue> jsonArgValues = requestPayload.getJsonArray("argValues");
        Object[] argValues = new Object[argTypes.length];
        for (int i = 0; i < jsonArgValues.size(); i++) {
            argValues[i] =  toObject(jsonArgValues.get(i), argTypes[i]);
        }
		
	    boolean success = excuteInAppContext(() -> {
    		try {
                // Obtain the target EJB that we're going to invoke
                Object bean = new InitialContext().lookup(requestPayload.getString("lookup"));
                
                // Authenticates the caller and if successful sets the security context
                // *for the outgoing EJB call*. In other words, the security context for this
                // Servlet will not be changed.
                if (requestPayload.containsKey(SECURITY_PRINCIPAL)) {
                    ProgrammaticLogin login = new ProgrammaticLogin();
                    login.login(
                        base64Decode(requestPayload.getString(SECURITY_PRINCIPAL)), 
                        base64Decode(requestPayload.getString(SECURITY_CREDENTIALS)),
                        null, true);
                }
                
                // Actually invoke the target EJB
                Object result = 
                    bean.getClass()
                        .getMethod(requestPayload.getString("method"), argTypes)
                        .invoke(bean, argValues);
                
                response.setContentType(APPLICATION_JSON);
                response.getWriter().print(result instanceof String? result : JsonbBuilder.create().toJson(result));
                
                return true;
                
            } catch (Exception e) {
                e.printStackTrace();
            }
    		
    		return false;
	    });
	    
	    if (!success) {
            response.sendError(SC_INTERNAL_SERVER_ERROR, "Name " + requestPayload.getString("lookup") + " not found when invoking");
        }
	}
	
	private Class<?> toClass(JsonValue classNameValue) {
	    try {
	        String className = null;
	        if (classNameValue instanceof JsonString) {
	            className = ((JsonString) classNameValue).getString();
	        } else {
	            className = classNameValue.toString().replace("\"", "");
	        }
	        
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
	}
	
	private Object toObject(JsonValue objectValue, Class<?> type) {
	    return JsonbBuilder
	            .create()
	            .fromJson(objectValue.toString(), type);
	}
	
	private boolean excuteInAppContext(Supplier<Boolean> body) {
	    ApplicationRegistry registry = Globals.get(ApplicationRegistry.class);
        
        for (String applicationName : registry.getAllApplicationNames()) {
            ClassLoader existingContextClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                
                Thread.currentThread().setContextClassLoader(registry.get(applicationName).getAppClassLoader());
            
                try {
                    if (body.get()) {
                        return true;
                    }
                } catch (Exception e) {
                    // ignore
                }
           
            } finally {
                if (existingContextClassLoader != null) {
                    Thread.currentThread().setContextClassLoader(existingContextClassLoader);
                }
            }
            
        }
        
        return false;
	}
	
    private static String base64Decode(String input) {
        return new String(Base64.getDecoder().decode(input));
    }

}
