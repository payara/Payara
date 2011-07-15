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

import java.io.Serializable;

/** An InteractionSpec holds properties for driving an Interaction 
 *  with an EIS instance. An InteractionSpec is used by an Interaction
 *  to execute the specified function on an underlying EIS.
 *
 *  <p>The CCI specification defines a set of standard properties for
 *  an InteractionSpec. An InteractionSpec implementation is not 
 *  required to support a standard property if that property does 
 *  not apply to its underlying EIS.
 *
 *  <p>The InteractionSpec implementation class must provide getter and
 *  setter methods for each of its supported properties. The getter and 
 *  setter methods convention should be based on the Java Beans design
 *  pattern.
 * 
 *  <p>The standard properties are as follows:
 *  <UL>
 *     <LI>FunctionName: name of an EIS function
 *     <LI>InteractionVerb: mode of interaction with an EIS instance:
 *         SYNC_SEND, SYNC_SEND_RECEIVE, SYNC_RECEIVE
 *     <LI>ExecutionTimeout: the number of milliseconds an Interaction 
 *         will wait for an EIS to execute the specified function
 *  </UL>
 *  
 *  <p>The following standard properties are used to give hints to an 
 *  Interaction instance about the ResultSet requirements:
 *  <UL>
 *     <LI>FetchSize
 *     <LI>FetchDirection
 *     <LI>MaxFieldSize
 *     <LI>ResultSetType
 *     <LI>ResultSetConcurrency
 *  </UL>
 *
 *  <p>A CCI implementation can provide additional properties beyond
 *  that described in the InteractionSpec interface. Note that the 
 *  format and type of the additional properties is specific to an EIS 
 *  and is outside the scope of the CCI specification.
 *  
 *  <p>It is required that the InteractionSpec interface be implemented
 *  as a JavaBean for the toolability support. The properties on the 
 *  InteractionSpec implementation class should be defined through the 
 *  getter and setter methods pattern. An implementation class for 
 *  InteractionSpec interface is  required to implement the 
 *  java.io.Serializable interface.
 *
 *  @author  Rahul Sharma
 *  @version 0.8
 *  @since   0.8
 *  @see     javax.resource.cci.Interaction
**/

public interface InteractionSpec extends java.io.Serializable {
  
  /**Interaction Verb type: The execution of an Interaction does only a 
   * send to the target EIS instance. The input record is sent to the
   * EIS instance without any synchronous response in terms of an 
   * output Record or ResultSet.
   */
  public static final int SYNC_SEND = 0;

  /**Interaction Verb type: The execution of an Interaction sends a 
   * request to the EIS instance and receives response synchronously. 
   * The input record is sent to the EIS instance with the output 
   * received either as Record or CCIResultSet.
   **/
  public static final int SYNC_SEND_RECEIVE = 1;

  /**The execution of an Interaction results in a synchronous 
   * receive of an output Record. An example is: a session bean gets
   * a method invocation and it uses this SEND_RECEIVE form of 
   * interaction to retrieve messages that have been delivered to a 
   * message queue. 
   **/
  public static final int SYNC_RECEIVE = 2;

}
