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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2019-2021] [Payara Foundation and/or its affiliates]
package org.glassfish.ejb.api;


import java.lang.reflect.Method;
import jakarta.ejb.EJBContext;

/**
 * This interface provides access to the exported portions of the
 * ejb invocation object.
 * @author Kenneth Saks
 */
public interface EJBInvocation {

    /**
     * @return runtime {@link EJBContext} of this invocation
     */
    EJBContext getEJBContext();

    /**
     * This is for EJB JAXWS only.
     * @return the JAXWS message
     */
    Object getMessage();

    /**
     * This is for EJB JAXWS only.
     * @param message  an unconsumed message
     */
    <T> void setMessage(T message);

    /**
     *
     * @return true if it is a webservice invocation
     */
    boolean isAWebService();

    /**
     * @return the Java Method object for this Invocation
     */
    Method getMethod();

    /**
     *
     * @return the Method parameters for this Invocation
     */
    Object[] getMethodParams();

    /**
     * Used by JACC implementation to get an enterprise bean
     * instance for the EnterpriseBean policy handler.  The jacc
     * implementation should use this method rather than directly
     * accessing the ejb field.
     *
     * @return EnterpriseBean instance or null if not applicable for this invocation.
     */
    Object getJaccEjb();

    /**
     * Use the underlying container to authorize this invocation
     *
     * @param method method to be invoked
     * @return true if the invocation was authorized by the underlying container
     * @throws java.lang.Exception
     */
    boolean authorizeWebService(Method method) throws Exception;

    /**
     * @return true if the SecurityManager reports that the caller is in role
     */
   boolean isCallerInRole(String role);

    /**
     * @param method - web service endpoint method
     */
    void setWebServiceMethod(Method method);

    /**
     * @return web service endpoint {@link Method}
     */
    Method getWebServiceMethod();

    /**
     * @param webServiceContext JAX-WS web service context used for the invocation
     */
    void setWebServiceContext(Object webServiceContext);
}
