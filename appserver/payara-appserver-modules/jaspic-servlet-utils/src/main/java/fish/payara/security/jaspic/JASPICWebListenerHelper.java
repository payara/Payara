/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.security.jaspic;

import java.util.Enumeration;
import java.util.HashMap;
import javax.security.auth.message.config.AuthConfigFactory;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

/**
 *
 * @author steve
 */
public class JASPICWebListenerHelper {
    
    private String registrationID;
    
    public void registerSAM(Class samClass, ServletContextEvent sce, String samDescription) {
       // Passing the Servlet Initializer properties is not necessary but can 
       // enable a SAM to use them to initialise itself
       ServletContext sc = sce.getServletContext();
       Enumeration<String> names = sce.getServletContext().getInitParameterNames();
       HashMap<String,String> samProperties = new HashMap<>();
       while(names.hasMoreElements()) {
           String name = names.nextElement();
           samProperties.put(name, sc.getInitParameter(name));
       }
       
       registrationID = AuthConfigFactory.getFactory()
                .registerConfigProvider(new SimpleSAMAuthConfigProvider(samProperties,null, samClass), 
                       "HttpServlet" , 
                        sce.getServletContext().getVirtualServerName() + " " + sce.getServletContext().getContextPath(),
                        samDescription);

    }
    
    public void deregisterSAM() {
        AuthConfigFactory.getFactory().removeRegistration(registrationID);        
    }
    
}
