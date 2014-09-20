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

/**
 * A View provides access to a subset of contexts that share a common prefix
 * that is the key for a ViewCapable instance.
 * View is used by Implementors of ViewCapable.
 */
public interface View {
  /**
   * 
   * @param name The name of the context sought.
   * @return The context associated to the specified name.
   * @throws InsufficientCredentialException If the user has insufficient 
   * privileges to access that context.
   */
  <T> T get(String name);

  /**
   * Stores the specified context under the specified name into the in-scope ContextMap.
   * @param name The name to associate to the specified context
   * @param context a String context.
   * @param propagationModes A set of propagation modes that control over 
   * which protocol this context will be propagated.
   * @return The context being replaced.
   * @throws InsufficientCredentialException If the user has insufficient 
   * privileges to access that context.
   */
  <T> T put(String name, String context, EnumSet<PropagationMode> propagationModes);

  /**
   * Stores the specified context under the specified name into the in-scope ContextMap.
   * @param name The name to associate to the specified context
   * @param context a Number context.
   * @param propagationModes A set of propagation modes that control over 
   * which protocol this context will be propagated.
   * @return The context being replaced.
   * @throws InsufficientCredentialException If the user has insufficient 
   * privileges to access that context.
   */
  <T, U extends Number> T put(String name, U context, EnumSet<PropagationMode> propagationModes);

  /**
   * Stores the specified context under the specified name into the in-scope ContextMap.
   * @param name The name to associate to the specified context
   * @param context an boolean String context.
   * @param propagationModes A set of propagation modes that control over 
   * which protocol this context will be propagated.
   * @return The context being replaced.
   * @throws InsufficientCredentialException If the user has insufficient 
   * privileges to access that context.
   */  
  <T> T put(String name, Boolean context, EnumSet<PropagationMode> propagationModes);

  /**
   * Stores the specified context under the specified name into the in-scope ContextMap.
   * @param name The name to associate to the specified context
   * @param context an char String context.
   * @param propagationModes A set of propagation modes that control over 
   * which protocol this context will be propagated.
   * @return The context being replaced.
   * @throws InsufficientCredentialException If the user has insufficient 
   * privileges to access that context.
   */  
  <T> T put(String name, Character context, EnumSet<PropagationMode> propagationModes);

   /**
    * Removes the specified context under the specified name from the in-scope ContextMap.
    * @param name The name to associate to the specified context
    * @return The context being replaced.
    * @throws InsufficientCredentialException If the user has insufficient 
    * privileges to access that context.
    */
   <T> T remove(String name);
}
