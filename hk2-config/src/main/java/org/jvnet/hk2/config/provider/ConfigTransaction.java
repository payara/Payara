/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.jvnet.hk2.config.provider;

import java.beans.PropertyChangeEvent;
import java.util.Collection;
import java.util.Map;

import org.jvnet.hk2.component.MultiMap;

/**
 * Represents a configuration transaction from an outside
 * configuration system.
 * 
 * <p/>
 * In all of the methods that take configuration beans, it is
 * expected that those objects are annotated with
 * {@link Configured}.  Additionally, the object identity
 * of the bean must be unique; so that a call to delete
 * a bean must correspond to some previous addition of
 * the bean from earlier by object identity.
 * 
 * @author Jeff Trent
 */
public interface ConfigTransaction {

  /**
   * Add configuration beans to the transaction.
   * 
   * @param created - the configuration bean instance being created
   * @param name - optionally the name for the configuration
   * @param metadata - name,value(s)
   */
  void created(Object created, String name, MultiMap<String, String> metadata);

  /**
   * Mark configuration beans as having been updated (note that name and metadata cannot change here).
   */
  void updated(Object updatedConfigBean, PropertyChangeEvent event);
  void updated(Collection<?> updatedConfigBeans);

  /**
   * Marks configuration beans as having been deleted.
   **/
  void deleted(Object deletedConfigBean);
  void deleted(Collection<?> deletedConfigBeans);

  /**
   * Locks changes, calls prepare.
   * 
   * @throws ConfigTransactionException
   */
  void prepare() throws ConfigTransactionException;
  
  /**
   * Locks changes, calls prepare (if not yet performed), followed by commit if no prepare errors.
   * If prepare errors exists, calls rollback on the constituent configuration beans.
   * 
   * @throws ConfigTransactionException
   */
  void commit() throws ConfigTransactionException;

  /**
   * Same basic behavior as {@link #commit()} with the added ability to substitute configuration
   * beans used in the prepare phase with the final bean object replacements that should be managed.
   * 
   * <p/>
   * This is an important variant when the configuration beans in the prepare phase are transient
   * in nature.
   * 
   * @param finalBeanMapping 
   *    mapping from the bean instance used in prepare, with the final version that should be managed
   *    
   * @throws ConfigTransactionException
   */
  void commit(Map<Object, Object> finalBeanMapping) throws ConfigTransactionException;
  
  /**
   * Cancels the transaction, locking it out from further changes.
   */
  void rollback();

}
