package fish.payara.ejb.invoke;

import static javax.naming.Context.SECURITY_CREDENTIALS;
import static javax.naming.Context.SECURITY_PRINCIPAL;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

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
            try {
                response.getWriter().print(
                    new InitialContext().lookup(requestPayload.getString("lookup"))
                                        .getClass()
                                        .getInterfaces()[0]
                                        .getName());
            } catch (NamingException e) {
                response.sendError(SC_INTERNAL_SERVER_ERROR, "Name " + requestPayload.getString("lookup") + " not found");
            }

            return;
        }
		
		try {
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
            
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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
	
    private static String base64Decode(String input) {
        return new String(Base64.getDecoder().decode(input));
    }

}
