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

/** The interface <code>javax.resource.cci.ResourceAdapterMetaData</code> 
 *  provides information about capabilities of a resource adapter 
 *  implementation. Note that this interface does not provide information 
 *  about an EIS instance that is connected through the resource adapter.
 *
 *  <p>A CCI client uses a <code>ConnectionFactory.getMetaData</code> to 
 *  get metadata information about the resource adapter. The 
 *  <code>getMetaData</code> method does not require that an active 
 *  connection to an EIS instance should have been established.
 *
 *  <p>The ResourceAdapterMetaData can be extended to provide more 
 *  information specific to a resource adapter implementation.
 *  
 *  @author  Rahul Sharma
 *  @version 0.8
 *  @since   0.8
 *  @see     javax.resource.cci.ConnectionFactory
**/

public interface ResourceAdapterMetaData {

  /** Gets the version of the resource adapter.
   *
   *  @return   String representing version of the resource adapter
  **/
  public 
  String getAdapterVersion();
	
  /** Gets the name of the vendor that has provided the resource 
   *  adapter.
   *
   *  @return   String representing name of the vendor that has 
   *            provided the resource adapter
  **/
  public 
  String getAdapterVendorName();
  
  /** Gets a tool displayable name of the resource adapter.
   *
   *  @return   String representing the name of the resource adapter
  **/
  public 
  String getAdapterName();
	
  /** Gets a tool displayable short desription of the resource
   *  adapter.
   *
   *  @return   String describing the resource adapter
  **/
  public 
  String getAdapterShortDescription();

  /** Returns a string representation of the version of the 
   *  connector architecture specification that is supported by
   *  the resource adapter.
   *
   *  @return   String representing the supported version of 
   *            the connector architecture
  **/
  public 
  String getSpecVersion();

  /** Returns an array of fully-qualified names of InteractionSpec
   *  types supported by the CCI implementation for this resource
   *  adapter. Note that the fully-qualified class name is for 
   *  the implementation class of an InteractionSpec. This method 
   *  may be used by tools vendor to find information on the 
   *  supported InteractionSpec types. The method should return 
   *  an array of length 0 if the CCI implementation does not 
   *  define specific InteractionSpec types.
   *
   *  @return   Array of fully-qualified class names of
   *            InteractionSpec classes supported by this
   *            resource adapter's CCI implementation
   *  @see      javax.resource.cci.InteractionSpec
  **/
  public 
  String[] getInteractionSpecsSupported();


  /** Returns true if the implementation class for the Interaction 
   *  interface implements public boolean execute(InteractionSpec 
   *  ispec, Record input, Record output) method; otherwise the 
   *  method returns false.
   *
   *  @return   boolean depending on method support
   *  @see      javax.resource.cci.Interaction
  **/
  public 
  boolean supportsExecuteWithInputAndOutputRecord();


  /** Returns true if the implementation class for the Interaction
   *  interface implements public Record execute(InteractionSpec
   *  ispec, Record input) method; otherwise the method returns 
   *  false.
   *
   *  @return   boolean depending on method support
   *  @see      javax.resource.cci.Interaction
  **/
  public 
  boolean supportsExecuteWithInputRecordOnly();


  /** Returns true if the resource adapter implements the LocalTransaction
   *  interface and supports local transaction demarcation on the 
   *  underlying EIS instance through the LocalTransaction interface.
   *
   *  @return  true if resource adapter supports resource manager
   *           local transaction demarcation through LocalTransaction
   *           interface; false otherwise
   *  @see     javax.resource.cci.LocalTransaction
  **/
  public
  boolean supportsLocalTransactionDemarcation();
}
