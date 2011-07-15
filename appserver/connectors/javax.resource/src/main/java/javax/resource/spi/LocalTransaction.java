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

package javax.resource.spi;

import javax.resource.ResourceException;

/** LocalTransaction interface provides support for transactions that
 *  are managed internal to an EIS resource manager, and do not require
 *  an external transaction manager.
 * 
 *  <p>A resource adapter implements the javax.resource.spi.LocalTransaction 
 *  interface to provide support for local transactions that are performed
 *  on the underlying resource manager.
 *
 *  <p>If a resource adapter supports the LocalTransaction interface, then 
 *  the application server can choose to perform local transaction 
 *  optimization (uses local transaction instead of a JTA transaction for
 *  a single resource manager case).
 *
 *  @version     0.5
 *  @author      Rahul Sharma
 *  @see         javax.resource.spi.ManagedConnection
 **/



public interface LocalTransaction {
  /** Begin a local transaction
   *  
   *  @throws  ResourceException   generic exception if operation fails
   *  @throws  LocalTransactionException  
   *                               error condition related 
   *                               to local transaction management
   *  @throws  ResourceAdapterInternalException
   *                               error condition internal to resource
   *                               adapter
   *  @throws  EISSystemException  EIS instance specific error condition        
  **/
  public 
  void begin() throws ResourceException;

  /** Commit a local transaction 
   *
   *  @throws  ResourceException   generic exception if operation fails
   *  @throws  LocalTransactionException  
   *                               error condition related 
   *                               to local transaction management
   *  @throws  ResourceAdapterInternalException
   *                               error condition internal to resource
   *                               adapter
   *  @throws  EISSystemException  EIS instance specific error condition        
  **/
  public
  void commit() throws ResourceException;
  
  /** Rollback a local transaction
   *  @throws  ResourceException   generic exception if operation fails
   *  @throws  LocalTransactionException  
   *                               error condition related 
   *                               to local transaction management
   *  @throws  ResourceAdapterInternalException
   *                               error condition internal to resource
   *                               adapter
   *  @throws  EISSystemException  EIS instance specific error condition        
  **/
  public
  void rollback() throws ResourceException;

}
