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


/** The <code>javax.resource.cci.Interaction</code> enables a component to 
 *  execute EIS functions. An Interaction instance supports the following ways 
 *  of interacting with an EIS instance:
 *  <UL>
 *     <LI><code>execute</code> method that takes an input Record, output
 *         Record and an InteractionSpec. This method executes the EIS 
 *         function represented by the InteractionSpec and updates the 
 *         output Record
 *     <LI><code>execute</code> method that takes an input Record and an 
 *         InteractionSpec. This method implementation executes the EIS 
 *         function represented by the InteractionSpec and produces the 
 *         output Record as a return value.
 *  </UL>
 *  <p>An Interaction instance is created from a Connection and is required
 *  to maintain its association with the Connection instance. The close method
 *  releases all resources maintained by the resource adapter for the 
 *  Interaction. The close of an Interaction instance should not close the 
 *  associated Connection instance.
 *       
 *  @author  Rahul Sharma
 *  @version 0.8
 *  @since   0.8
 *  @see     java.sql.ResultSet
**/

public interface Interaction {
  
  /** Closes the current Interaction and release all the resources
   *  held for this instance by the resource adapter. The close of an 
   *  Interaction instance does not close the associated Connection 
   *  instance. It is recommended that Interaction instances be
   *  closed explicitly to free any held resources.
   *
   *  @throws  ResourceException Failed to close the Interaction
   *                             instance. Invoking close on an 
   *                             already closed Interaction should 
   *                             also throw this exception. 
  **/
  public
  void close() throws ResourceException;


  /** Gets the Connection associated with the Interaction.
   *
   *  @return   Connection instance associated with the Interaction
  **/
  public
  Connection getConnection();

  /** Executes an interaction represented by the InteractionSpec.
   *  This form of invocation takes an input Record and updates
   *  the output Record. 
   *  
   *  @param   ispec   InteractionSpec representing a target EIS 
   *                   data/function module   
   *  @param   input   Input Record
   *  @param   output  Output Record
   * 
   *  @return  true if execution of the EIS function has been 
   *           successful and output Record has been updated; false
   *           otherwise
   *
   *  @throws  ResourceException   Exception if execute operation
   *                               fails. Examples of error cases
   *                               are:
   *         <UL>
   *           <LI> Resource adapter internal, EIS-specific or 
   *                communication error 
   *           <LI> Invalid specification of an InteractionSpec, 
   *                input or output record structure
   *           <LI> Errors in use of input or output Record
   *           <LI> Invalid connection associated with this 
   *                Interaction
   *	     </UL>
   *  @throws NotSupportedException Operation not supported 
   *                             
  **/
  public
  boolean execute(InteractionSpec ispec, 
		  Record input, 
		  Record output) throws ResourceException;

  /** Executes an interaction represented by the InteractionSpec.
   *  This form of invocation takes an input Record and returns an 
   *  output Record if the execution of the Interaction has been
   *  successfull.
   *  
   *  @param   ispec   InteractionSpec representing a target EIS 
   *                   data/function module   
   *  @param   input   Input Record

   *  @return  output Record if execution of the EIS function has been 
   *           successful; null otherwise
   *
   *  @throws  ResourceException   Exception if execute operation
   *                               fails. Examples of error cases
   *                               are:
   *         <UL>
   *           <LI> Resource adapter internal, EIS-specific or 
   *                communication error 
   *           <LI> Invalid specification of an InteractionSpec 
   *                or input record structure
   *           <LI> Errors in use of input Record or creation
   *                of an output Record
   *           <LI> Invalid connection associated with this 
   *                Interaction
   *	     </UL>
   *  @throws NotSupportedException Operation not supported 
  **/
  public
  Record execute(InteractionSpec ispec, 
		 Record input) throws ResourceException;

  /** Gets the first ResourceWarning from the chain of warnings
   *  associated with this Interaction instance.
   *
   *  @return   ResourceWarning at top of the warning chain
   *  @throws   ResourceException  Failed to get ResourceWarnings
   *                               associated with Interaction  
   **/
  public
  ResourceWarning getWarnings()  throws ResourceException;

  /** Clears all the warning reported by this Interaction instance. After 
   *  a call to this method, the method getWarnings will return null 
   *  until a new warning is reported for this Interaction.
   * 
   *  @throws   ResourceException  Failed to clear ResourceWarnings
   *                               associated with Interaction  
 **/
  public 
  void clearWarnings() throws ResourceException;

}
