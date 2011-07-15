/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
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

package org.glassfish.ejb.api;


import java.lang.reflect.Method;
import javax.ejb.EJBContext;
import javax.xml.rpc.handler.MessageContext;

/**
 * This interface provides access to the exported portions of the
 * ejb invocation object.  
 * @author Kenneth Saks
 */


public interface EJBInvocation {

    public EJBContext getEJBContext();
    
    /**
     * This is for EJB JAXWS only.
     * @return the JAXWS message
     */
    public Object getMessage();
    
    /**
     * This is for EJB JAXWS only.
     * @param message  an unconsumed message
     */
    public <T> void setMessage(T message);
    
    /**
     * 
     * @return true if it is a webservice invocation
     */
    public boolean isAWebService();
    
    /**
     * @return the Java Method object for this Invocation
     */
    public Method getMethod();
    
    /**
     * 
     * @return the Method parameters for this Invocation
     */
    public Object[] getMethodParams();
    
    /**
     * Used by JACC implementation to get an enterprise bean
     * instance for the EnterpriseBean policy handler.  The jacc
     * implementation should use this method rather than directly
     * accessing the ejb field.
     */
    public Object getJaccEjb();
    
    /**
     * Use the underlying container to authorize this invocation
     * @return true if the invocation was authorized by the underlying container
     * @throws java.lang.Exception TODO, change this to throw some subclass
     */
    public boolean authorizeWebService(Method m) throws Exception;
    
/**
    *
    * @return true if the SecurityManager reports that the caller is in role
    */
   public boolean isCallerInRole(String role);

    /**
     * Used by JAXRPC pre/postHandler classes
     * @param tie an instance of com.sun.xml.rpc.spi.runtime.Tie
     */
    public void setWebServiceTie(Object tie);


    /**
     * Used for setting JAXRPC message context.
     */
    public void setMessageContext(MessageContext msgContext);

    /**
     * @return instance of com.sun.xml.rpc.spi.runtime.Tie
     */
    public Object getWebServiceTie();

    public void setWebServiceMethod(Method method);
    public Method getWebServiceMethod();
    public void setWebServiceContext(Object webServiceContext);
}
