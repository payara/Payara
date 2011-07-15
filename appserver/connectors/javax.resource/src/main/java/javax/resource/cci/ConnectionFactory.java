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


import java.io.PrintWriter;
import javax.resource.ResourceException;
import javax.resource.NotSupportedException;


/** <code>ConnectionFactory</code> provides an interface for getting
 *  connection to an EIS instance. An implementation of ConnectionFactory
 *  interface is provided by a resource adapter.
 *
 *  <p>Application code looks up a ConnectionFactory instance from JNDI
 *  namespace and uses it to get EIS connections. 
 *
 *  <p>An implementation class for ConnectionFactory is required to 
 *  implement <code>java.io.Serializable</code> and 
 *  <code>javax.resource.Referenceable</code>interfaces to support 
 *  JNDI registration.
 *  
 *  @author  Rahul Sharma
 *  @version 0.8
 *  @see     javax.resource.cci.Connection
 *  @see     javax.resource.Referenceable
 **/
public interface ConnectionFactory 
                    extends java.io.Serializable, 
                            javax.resource.Referenceable {
  
  /** Gets a connection to an EIS instance. This getConnection variant
   *  should be used when a component wants the container to manage EIS 
   *  sign-on. This case is termed container-managed sign-on. The 
   *  component does not pass any security information.
   *
   *
   *  @return   Connection instance
   *  @throws   ResourceException   Failed to get a connection to
   *                                the EIS instance. Examples of
   *                                error cases are:
   *          <UL>
   *          <LI> Invalid configuration of ManagedConnectionFactory--
   *               example: invalid server name
   *          <LI> Application server-internal error--example:
   *               connection pool related error
   *          <LI> Communication error 
   *          <LI> EIS-specific error--example: EIS not active
   *          <LI> Resource adapter-internal error
   *          <LI> Security related error; example: invalid user
   *          <LI> Failure to allocate system resources
   *         </UL>                        
  **/
  public 
  Connection getConnection() throws ResourceException;

  /** Gets a connection to an EIS instance. A component should use 
   *  the getConnection variant with javax.resource.cci.ConnectionSpec
   *  parameter, if it needs to pass any resource adapter specific 
   *  security information and connection parameters. In the component-
   *  managed sign-on case, an application component passes security 
   *  information (example: username, password) through the 
   *  ConnectionSpec instance.
   * 
   *  <p>It is important to note that the properties passed through 
   *  the getConnection method should be client-specific (example: 
   *  username, password, language) and not related to the 
   *  configuration of a target EIS instance (example: port number, 
   *  server name). The ManagedConnectionFactory instance is configured
   *  with complete set of properties required for the creation of a 
   *  connection to an EIS instance. 
   *
   *  @param  properties          Connection parameters and security
   *                              information specified as 
   *                              ConnectionSpec instance
   *  @return Connection instance
   *
   *  @throws ResourceException   Failed to get a connection to
   *                              the EIS instance. Examples of
   *                              error cases are:
   *          <UL>
   *          <LI> Invalid specification of input parameters
   *          <LI> Invalid configuration of ManagedConnectionFactory--
   *               example: invalid server name
   *          <LI> Application server-internal error--example:
   *               connection pool related error
   *          <LI> Communication error 
   *          <LI> EIS-specific error--example: EIS not active
   *          <LI> Resource adapter-internal error
   *          <LI> Security related error; example: invalid user
   *          <LI> Failure to allocate system resources
   *         </UL>                        
   *  @see     javax.resource.cci.ConnectionSpec
  **/
  public 
  Connection getConnection(ConnectionSpec properties) 
                       throws ResourceException;

  /** Gets a RecordFactory instance. The RecordFactory is used for
   *  the creation of generic Record instances.
   *
   *  @return RecordFactory         RecordFactory instance
   *
   *  @throws ResourceException     Failed to create a RecordFactory
   *  @throws NotSupportedException Operation not supported
  **/
  public
  RecordFactory getRecordFactory() throws ResourceException;

  /** Gets metadata for the Resource Adapter. Note that the metadata
   *  information is about the ResourceAdapter and not the EIS instance.
   *  An invocation of this method does not require that an active
   *  connection to an EIS instance should have been established.
   *
   *  @return  ResourceAdapterMetaData instance
   *  @throws   ResourceException Failed to get metadata information 
   *                              about the resource adapter
  **/
  public
  ResourceAdapterMetaData getMetaData() throws ResourceException;

}
