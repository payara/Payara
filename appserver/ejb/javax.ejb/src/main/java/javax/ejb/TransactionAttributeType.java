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

package javax.ejb;

/**
 * The enum <code>TransactionAttributeType</code> is used with the
 * <code>TransactionAttribute</code> annotation to specify whether the
 * methods of a session bean or message driven bean are called with a
 * valid transaction context.
 * 
 * <p>
 * For a message-driven bean's message listener methods (or interface), only 
 * the <code>REQUIRED</code> and <code>NOT_SUPPORTED</code> values may be used.
 * <p>
 *
 * For an enterprise bean's timeout callback methods, only the 
 * <code>REQUIRED</code>, <code>REQUIRES_NEW</code> and <code>NOT_SUPPORTED</code>
 * values may be used.
 * <p>
 *
 * For a session bean's asynchronous business methods, only the 
 * <code>REQUIRED</code>, <code>REQUIRES_NEW</code>, and <code>NOT_SUPPORTED</code>
 * values may be used.
 *
 * <p>
 * For a singleton session bean's <code>PostConstruct</code> and <code>PreDestroy</code>
 * lifecycle callback interceptor methods, only the <code>REQUIRED</code>, 
 * <code>REQUIRES_NEW</code>, and <code>NOT_SUPPORTED</code> values may be used.
 *
 * <p>
 * If an enterprise bean implements the <code>SessionSynchronization</code> interface
 * or uses any of the session synchronization annotations, only the following values 
 * may be used for the transaction attributes of the bean's methods: 
 * <code>REQUIRED</code>, <code>REQUIRES_NEW</code>, <code>MANDATORY</code>.
 *
 * @see TransactionAttribute
 *
 * @since EJB 3.0
 */
public enum TransactionAttributeType {
    
    /**
     * If a client invokes the enterprise bean's method while the client 
     * is associated with a transaction context, the container invokes the 
     * enterprise bean's method in the client's transaction context.  
     *
     *<p>
     * If there is no existing transaction, an exception is thrown.
     */
   MANDATORY,

   /**
    * If a client invokes the enterprise bean's method while the client is 
    * associated with a transaction context, the container invokes the 
    * enterprise bean's method in the client's transaction context.
    *
    * <p>
    * If the client invokes the enterprise bean's method while the client is 
    * not associated with a transaction context, the container automatically 
    * starts a new transaction before delegating a method call to the enterprise 
    * bean method.
    */
   REQUIRED,

   /**
    * The container must invoke an enterprise bean method whose transaction 
    * attribute is set to <code>REQUIRES_NEW</code> with a new transaction context.
    *
    * <p>
    * If the client invokes the enterprise bean's method while the client is not 
    * associated with a transaction context, the container automatically starts 
    * a new transaction before delegating a method call to the enterprise bean 
    * business method.  
    * 
    * <p> If a client calls with a transaction context, the container 
    * suspends the association of the transaction context with the current thread 
    * before starting the new transaction and invoking the method. The container 
    * resumes the suspended transaction association after the method and the 
    * new transaction have been completed.
    */
   REQUIRES_NEW,

   /**
    * If the client calls with a transaction context, the container performs 
    * the same steps as described in the <code>REQUIRED</code> case.   
    *
    * <p>
    * If the  client calls without a transaction context, the container performs the 
    * same steps as described in the <code>NOT_SUPPORTED</code> case.
    * 
    * <p>
    * The <code>SUPPORTS</code> transaction attribute must be used with caution. 
    * This is because of the different transactional semantics provided by the 
    * two possible modes of execution. Only enterprise beans that will execute 
    * correctly in both modes should use the <code>SUPPORTS</code>
    * transaction attribute.
    */
   SUPPORTS,

   /**
    * The container invokes an enterprise bean method whose transaction 
    * attribute <code>NOT_SUPPORTED</code> with an unspecified transaction context.
    *
    * <p>
    * If a client calls with a transaction context, the container suspends the 
    * association of the transaction context with the current thread before 
    * invoking the enterprise bean's business method. The container resumes 
    * the suspended association when the business method has completed.
    */
   NOT_SUPPORTED,

   /**
    * The client is required to call without a transaction context, otherwise
    * an exception is thrown.
    */
   NEVER
}
