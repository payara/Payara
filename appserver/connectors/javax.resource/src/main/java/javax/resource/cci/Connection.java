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
import javax.resource.NotSupportedException;


/** A Connection represents an application-level handle that is used 
 *  by a client to access the underlying physical connection. The actual 
 *  physical connection associated with a Connection instance is 
 *  represented by a ManagedConnection instance.
 *
 *  <p>A client gets a Connection instance by using the 
 *  <code>getConnection</code> method on a <code>ConnectionFactory</code> 
 *  instance. A connection can be associated with zero or more Interaction
 *  instances.
 * 
 *  @author  Rahul Sharma
 *  @version 0.8
 *  @see     javax.resource.cci.ConnectionFactory
 *  @see     javax.resource.cci.Interaction
 **/

public interface Connection {
  
  /** Creates an Interaction associated with this Connection. An
   *  Interaction enables an application to execute EIS functions. 
   *
   *  @return  Interaction instance  
   *  @throws  ResourceException     Failed to create an Interaction
  **/
  public
  Interaction createInteraction() 
			    throws ResourceException;

  /** Returns an LocalTransaction instance that enables a component to
   *  demarcate resource manager local transactions on the Connection.
   *  If a resource adapter does not allow a component to demarcate 
   *  local transactions on an Connection using LocalTransaction 
   *  interface, then the method getLocalTransaction should throw a 
   *  NotSupportedException.
   *
   *  @return   LocalTransaction instance
   *           
   *  @throws   ResourceException   Failed to return a LocalTransaction
   *                                instance because of a resource
   *                                adapter error
   *  @throws   NotSupportedException Demarcation of Resource manager 
   *                                local transactions is not supported
   *                                on this Connection
   *  @see javax.resource.cci.LocalTransaction
  **/

  public
  LocalTransaction getLocalTransaction() throws ResourceException;
  
  /** Gets the information on the underlying EIS instance represented
   *  through an active connection.
   *
   *  @return   ConnectionMetaData instance representing information 
   *            about the EIS instance
   *  @throws   ResourceException  
   *                        Failed to get information about the 
   *                        connected EIS instance. Error can be
   *                        resource adapter-internal, EIS-specific
   *                        or communication related.
  **/
  public
  ConnectionMetaData getMetaData() throws ResourceException;

  /** Gets the information on the ResultSet functionality supported by
   *  a connected EIS instance.
   *
   *  @return   ResultSetInfo instance
   *  @throws   ResourceException     Failed to get ResultSet related 
   *                                  information
   *  @throws   NotSupportedException ResultSet functionality is not
   *                                  supported
  **/
  public
  ResultSetInfo getResultSetInfo() throws ResourceException;
  
  
  /** Initiates close of the connection handle at the application level.
   *  A client should not use a closed connection to interact with 
   *  an EIS.
   *  
   *  @throws  ResourceException  Exception thrown if close
   *                              on a connection handle fails.
   *           <p>Any invalid connection close invocation--example,
   *              calling close on a connection handle that is 
   *              already closed--should also throw this exception.
   *  
  **/
  public
  void close() throws ResourceException;

}
