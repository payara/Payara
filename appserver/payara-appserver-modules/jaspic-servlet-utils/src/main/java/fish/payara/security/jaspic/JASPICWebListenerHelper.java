/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.security.jaspic;

import java.util.Enumeration;
import java.util.HashMap;
import javax.security.auth.message.config.AuthConfigFactory;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

/**
 * This is a helper class used to provide a simple mechanism to deploy a custom
 * ServerAuthenticationModule (SAM) into Payara during deployment of an .application
 * This WebListener should be called by a ServletContextListener. If you want to 
 * pass custom properties to your ServerAuthenticationModule you can create ServletContext 
 * initialiser parameters and these are all passed to your SAM as properties.
 * 
 * Your SAM must be thread safe as only one instance will be created and 
 * initialize will be called before every validateRequest
 * @author steve
 */
public class JASPICWebListenerHelper {
    
    /** 
     * Use this servlet context initialiser property to define whether a new instance of your SAM
     * is created on each request or only a single instance is created.
     */
    public final static String SAM_PER_REQUEST_PROPERTY = "fish.payara.security.jaspic.SAMPerRequest";
    
    private String registrationID;
    
    /**
     * Call register in the contextInitialised method of your ServletContextListener
     * @param samClass The class of your ServerAuthenitcationModule
     * @param sce The ServletContextEvent received by your ServletContextListener.
     * @param samDescription A description for your SAM
     */
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
    
    /**
     * Call this method in the contextDestroyed method of your ServletContextListener
     */
    public void deregisterSAM() {
        AuthConfigFactory.getFactory().removeRegistration(registrationID);        
    }
    
}
