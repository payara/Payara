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

/** <p>The ConnectionRequestInfo interface enables a resource adapter to 
 *  pass its own request specific data structure across the connection
 *  request flow. A resource adapter extends the empty interface to
 *  supports its own data structures for connection request.
 *  
 *  <p>A typical use allows a resource adapter to handle 
 *  application component specified per-connection request properties
 *  (example - client ID, language). The application server passes these 
 *  properties back across to match/createManagedConnection calls on 
 *  the resource adapter. These properties remain opaque to the 
 *  application server during the connection request flow. 
 *
 *  <p>Once the ConnectionRequestInfo reaches match/createManagedConnection
 *  methods on the ManagedConnectionFactory instance, resource adapter
 *  uses this additional per-request information to do connection 
 *  creation and matching.
 *
 *  @version     0.8
 *  @author      Rahul Sharma
 *  @see         javax.resource.spi.ManagedConnectionFactory
 *  @see         javax.resource.spi.ManagedConnection
**/

public interface ConnectionRequestInfo {

  /** Checks whether this instance is equal to another. Since
   *  connectionRequestInfo is defined specific to a resource
   *  adapter, the resource adapter is required to implement
   *  this method. The conditions for equality are specific
   *  to the resource adapter.
   *
   *  @return True if the two instances are equal.
  **/
  public
  boolean equals(Object other);

  /** Returns the hashCode of the ConnectionRequestInfo.
   *
   *  @return hash code os this instance
  **/
  public
  int hashCode();


}
