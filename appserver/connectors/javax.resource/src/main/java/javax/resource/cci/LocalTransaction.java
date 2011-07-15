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

package javax.resource.cci;

import javax.resource.ResourceException;
/** The LocalTransaction defines a transaction demarcation interface for
 *  resource manager local transactions. Note that this interface is 
 *  used for application level local transaction demarcation. The
 *  system contract level LocalTransaction interface (as defined in
 *  the <code>javax.resource.spi</code> package) is used by the container 
 *  for local transaction management.
 *
 *  <p>A local transaction is managed internal to a resource manager. There
 *  is no external transaction manager involved in the coordination of 
 *  such transactions.
 *
 *  <p>A CCI implementation can (but is not required to) implement the
 *  LocalTransaction interface. If the LocalTransaction interface is supported
 *  by a CCI implementation, then the method 
 *  <code>Connection.getLocalTransaction</code> should return a
 *  LocalTransaction instance. A component can then use the
 *  returned LocalTransaction to demarcate a resource manager local transaction
 *  (associated with the Connection instance) on the underlying EIS 
 *  instance.
 *  
 *  @author  Rahul Sharma
 *  @since   0.8
 *  @see     javax.resource.cci.Connection
**/

public interface LocalTransaction {

  /** Begins a local transaction on an EIS instance.
   *
   *  @throws  ResourceException  Failed to begin a local
   *                              transaction. Examples of
   *                              error cases are:
   *           <UL>
   *             <LI>Resource adapter internal or EIS-specific
   *                 error
   *             <LI>Connection is already participating in a 
   *                 local or JTA transaction 
   *           </UL> 
  **/
  public 
  void begin() throws ResourceException;


  /** Commits the current local transaction and release all locks held 
   *  by the underlying EIS instance.
   *
   *  @throws  ResourceException  Failed to commit a local
   *                              transaction. Examples of
   *                              error cases are:
   *          <UL>
   *            <LI> Resource adapter internal or EIS-specific error
   *            <LI> Violation of integrity constraints, deadlock 
   *                 detection, communication failure during 
   *                 transaction completion, or any retry requirement
   *            <LI> Connection is participating in an active JTA
   *                 transaction 
   *            <LI> Invalid transaction context; commit
   *                 operation invoked without an active
   *                 transaction context
   *         </UL> 
  **/
  public 
  void commit() throws ResourceException;

  /** Rollbacks the current resource manager local transaction.
   *
   *  @throws  ResourceException  Failed to rollback a local
   *                              transaction. Examples of
   *                              error cases are:
   *           <UL>
   *             <LI> Resource adapter internal or EIS-specific error
   *             <LI> Connection is participating in an active JTA 
   *                  transaction 
   *             <LI> Invalid transaction context; rollback
   *                  operation invoked without an active
   *                  transaction context
   *          </UL> 

  **/
  public
  void rollback() throws ResourceException;
}
