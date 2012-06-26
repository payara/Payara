/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.contextpropagation;

import java.util.EnumSet;
import java.util.Iterator;

import org.glassfish.contextpropagation.internal.AccessControlledMap;


/**
 * This is the primary API for application developers who want to use context
 * propagation. The in-scope instance of this class
 * is available in the jndi under the name: request/ContextMap.
 * 
 * This class is used to retrieve and store contexts that will be
 * automatically propagated depending on the protocol doing the propagation
 * and the context's propagation mode.
 * 
 * Each protocol has a unique propagation mode such as SOAP or RMI. Each context
 * may be associated to one or more propagation mode. When a protocol exchange a
 * message, each in-scope context associated to that protocol, via the 
 * propagation mode, will be transeferred along with the message.
 */
public interface ContextMap {
  /**
   * Get the propagation modes of a the context with the specified name.
   * @param name context name
   * @return A set of propagation modes supported by this context.
   * @throws InsufficientCredentialException
   */
  public EnumSet<PropagationMode> getPropagationModes(String name) throws InsufficientCredentialException;
  
  /**
   * @return true if there are no context in the ContextMap
   */
  public boolean isEmpty();
  
  /**
   * 
   * @return An Iterator<String> listing all the context names in the ContextMap
   */
  public Iterator<String> names();
  
  /**
   * ViewCapable instances are custom contexts and are created by the context 
   * propagation framework by
   * using a ContextViewFactory registered against the corresponding context's 
   * name prefix. This method creates an instance of a ViewCapable and adds it
   * to the ContextMap under the specified prefix. 
   * @param prefix It is the name associated to the ViewCapable instance in the ContextMap
   * @return The ViewCapableInstance
   * @throws InsufficientCredentialException
   */
  public <T extends ViewCapable> T createViewCapable(String prefix) throws InsufficientCredentialException;
  
  /**
   * @return The Location of the request relative to the original location where it
   * was first submitted.
   */
  public Location getLocation();
  
  /**
   * 
   * @param name The name of the context sought.
   * @return The context associated to the specified name.
   * @throws InsufficientCredentialException If the user has insufficient 
   * privileges to access that context.
   */
  <T> T get(String name) throws InsufficientCredentialException;

  /**
   * Stores the specified context under the specified name into the in-scope ContextMap.
   * @param name The name to associate to the specified context
   * @param context a String context.
   * @param propagationModes A set of propagation modes that determines over 
   * which protocol this context will be propagated.
   * @return The context being replaced.
   * @throws InsufficientCredentialException If the user has insufficient 
   * privileges to access that context.
   */
  <T> T put(String name, String context, EnumSet<PropagationMode> propagationModes) throws InsufficientCredentialException;

  /**
   * Stores the specified context under the specified name into the in-scope ContextMap.
   * @param name The name to associate to the specified context
   * @param context a Number context.
   * @param propagationModes A set of propagation modes that determines over 
   * which protocol this context will be propagated.
   * @return The context being replaced.
   * @throws InsufficientCredentialException If the user has insufficient 
   * privileges to access that context.
   */
  <T, U extends Number> T put(String name, U context, EnumSet<PropagationMode> propagationModes) throws InsufficientCredentialException;

  /**
   * Stores the specified context under the specified name into the in-scope ContextMap.
   * @param name The name to associate to the specified context
   * @param context an boolean String context.
   * @param propagationModes A set of propagation modes that determines over 
   * which protocol this context will be propagated.
   * @return The context being replaced.
   * @throws InsufficientCredentialException If the user has insufficient 
   * privileges to access that context.
   */  
  <T> T put(String name, Boolean context, EnumSet<PropagationMode> propagationModes) throws InsufficientCredentialException;

  /**
   * Stores the specified context under the specified name into the in-scope ContextMap.
   * @param name The name to associate to the specified context
   * @param context an char String context.
   * @param propagationModes A set of propagation modes that determines over 
   * which protocol this context will be propagated.
   * @return The context being replaced.
   * @throws InsufficientCredentialException If the user has insufficient 
   * privileges to access that context.
   */  
  <T> T put(String name, Character context, EnumSet<PropagationMode> propagationModes) throws InsufficientCredentialException;

   /**
    * Removes the specified context under the specified name from the in-scope ContextMap.
    * @param name The name to which the context was associated
    * @return The context being replaced.
    * @throws InsufficientCredentialException If the user has insufficient 
    * privileges to access that context.
    */
   <T> T remove(String name) throws InsufficientCredentialException;

   /**
    * This method is used when one needs to propagate the ContextMap to another Thread.
    * @return The AccessControlledMap that is in scope for the current thread
    */
  public AccessControlledMap getAccessControlledMap();
  

}
