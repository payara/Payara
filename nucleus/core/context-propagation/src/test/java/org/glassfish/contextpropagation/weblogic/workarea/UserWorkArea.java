/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.contextpropagation.weblogic.workarea;

import java.io.Serializable;


/**
 * WARNING: very prelimanry APIs almost certainly subject to change.
 *
 * <code>UserWorkArea</code> provides end-users, ISVs and other
 * mechanisms for tagging certain requests (whether remote or local)
 * and propagating that information based on certain policy
 * constraints.  <code>UserWorkArea</code> is part of a client or
 * applications JNDI environment and can be access through JNDI:
 * <p> <pre>
 * UserWorkArea rc = (UserWorkArea)
 * new InitialContext().lookup("java:comp/UserWorkArea");
 * </pre>
 * <p>Internal consumers of this service can also access the current
 * <code>UserWorkArea</code> through a helper:
 * <p><pre>
 * UserWorkArea rc = WorkAreaHelper.
 * getWorkAreaHelper().getWorkArea();
 * </pre>
 *
 * @exclude
 * @author Copyright (c) 2003 by BEA Systems Inc. All Rights Reserved.
 */
public interface UserWorkArea extends Serializable
{
  /**
   * Begin a new WorkArea, with remote propagation only, and associate
   * it with the current thread. If a WorkArea is already associated
   * with the current thread then the new WorkArea will overlay
   * contexts within the already-existing WorkArea,
   */
  public void begin(String name);

  /**
   * Begin a new WorkArea, with propagation mode set to
   * defaultPropertyMode, and associate it with the current thread. If
   * a WorkArea is already associated with the current thread then the
   * new WorkArea will overlay contexts within the already-existing
   * WorkArea.
   *
   * Beginning a WorkArea merely assigns a unique name. Key value
   * tuples that are set in the WorkArea are propagated based on
   * propagation policies assigned to the WorkArea. By default a
   * WorkArea is not propagated out of the current thread. Applying
   * <code>PropagationMode.THREAD</code> allows a context to be
   * propagated to Work instances. Applying
   * <code>PropagationMode.REMOTE</code> allows a context to be
   * propagated in RMI calls. Applying
   * <code>PropagationMode.TRANSACTION</code> allows a context to be
   * propagated between different global transactions.  Applying
   * <code>PropagationMode.JMS_CONSUMER</code> allows a context to be
   * propagated to JMS consumers.  Applying
   * <code>PropagationMode.JMS_PRODUCER</code> allows a context to be
   * propagated from JMS producers.  Applying
   * <code>PropagationMode.SOAP</code> allows a context to be
   * propagated across SOAP messages.  <code>PropagationMode</code>s
   * are additive and can be used together.
   *
   * @see org.glassfish.contextpropagation.weblogic.workarea.PropagationMode
   */
  public void begin(String name, int propagationMode);

  /**
   * Complete the current, active WorkArea. Any nested values created
   * by this WorkArea will be removed. If there is a parent WorkArea
   * then that will become the new default WorkArea. Nested WorkAreas
   * are not terminated by parent WorkAreas.
   */
  public void complete() throws NoWorkContextException;

  /**
   * Complete the active WorkArea named name. Any nested values
   * created by this WorkArea will be removed. If name corresponds to
   * the current, active WorkArea and there is a parent WorkArea then
   * that will become the new default WorkArea. Nested WorkAreas are
   * not terminated by parent WorkAreas.
   *
   * <p> It is generally preferrable to end named work areas to avoid
   * conflicts with other code using the WorkArea service.
   */
  public void complete(String name) throws NoWorkContextException;

  /**
   * Returns the current WorkArea's name. Returns null if there is no
   * active WorkArea. This method can be used to determine whether
   * there is a current WorkArea.
   */
  public String getName();

  /** 
   * Returns an array of all of the keys associated with the stack of
   * WorkArea's for the current thread. There is no guaranteed
   * ordering of the returned keys. The set of keys includes all those
   * of all enclosing WorkAreas.
   */
  public String[] getKeys();
  
  /**
   * Adds context data to the current WorkArea. This context data is
   * propagated according to the policyOptions that the context was
   * started with. Any existing value for key will be overlaid.
   */
  public void setProperty(String key, WorkContext ctx) 
    throws NoWorkContextException, PropertyReadOnlyException;

  /**
   * Adds context data to the current WorkArea. This context data is
   * propagated according to the provided policy. Any existing value
   * for key will be overlaid. It is legal for multiple context data
   * items to be propagated as long as their keys differ.
   */
  public void setProperty(String key, WorkContext ctx,
                          int propertyModeType) throws NoWorkContextException, 
                                                       PropertyReadOnlyException;

  /**
   * Get the current WorkArea's value for key. If the current WorkArea
   * has no value for key then all parent WorkArea's are searched. If
   * no WorkArea has a value for key then null is returned.
   */
  public WorkContext getProperty(String key);

  /**
   * Get the current WorkArea's PropertyModeType value for key. If the
   * current WorkArea has no value for key then all parent WorkArea's
   * are searched. If no WorkArea has a value for key then null is
   * PropertyModeType.NORMAL is returned.
   */
  public int getMode(String key);

  /**
   * Remove the property for key from the current WorkArea. This does
   * not affect parent WorkAreas that have property values for key.
   */
  public void removeProperty(String key) throws NoWorkContextException;
}
