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

import java.util.Iterator;


/**
 * <code>WorkContextMap</code> provides users with mechanisms
 * for tagging certain requests (whether remote or local)
 * and propagating that information based on certain policy
 * constraints.
 * <code>WorkContextMap</code> is part of a client or
 * application's JNDI environment and can be access through JNDI:
 * <p> <pre>
 * WorkContextMap rc = (WorkContextMap)
 *   new InitialContext().lookup("java:comp/WorkContextMap");
 * </pre>
 *
 * @author Copyright (c) 2003 by BEA Systems Inc. All Rights Reserved.
 */
public interface WorkContextMap
{
  /**
   * Adds context data with key <code>key</code> to the current
   * WorkContextMap and associates it with the current thread. The context
   * data is propagated according to the default
   * {@link org.glassfish.contextpropagation.weblogic.workarea.PropagationMode}. The
   * defaults are {@link org.glassfish.contextpropagation.weblogic.workarea.PropagationMode#DEFAULT}.
   *
   * @param key a unique {@link String} that is used to obtain a
   * reference to a particular {@link WorkContext}. Keys are
   * encoded as {@link java.io.DataOutput#writeUTF}. In order to protect
   * the key namespace a good convention is to use package names as a
   * prefix. For example <code>com.you.SomeKey}.
   * @param ctx The {@link WorkContext} to put in the map.
   * @return the previous WorkContext for key
   * @exception PropertyReadOnlyException if the property already
   * exists and is read-only.
   * @exception NullPointerException if the property or context is null.
   */
  public WorkContext put(String key, WorkContext ctx)
    throws PropertyReadOnlyException;

  /**
   * Adds context data with key <code>key</code> to the current
   * WorkContextMap and associates it with the current thread. This context
   * data is propagated according to the provided mode
   * <code>propagationMode</code>. Any existing value for key will be
   * changed as per the property mode
   * <code>propertyModeType</code>. It is legal for multiple context
   * data items to be propagated as long as their keys differ.
   *
   * Properties that are set in the WorkContextMap are propagated
   * based on propagation policies assigned to the property. By
   * default a property is not propagated out of the current
   * thread. Applying {@link PropagationMode#WORK} allows a
   * property to be propagated to Work instances. Applying
   * {@link PropagationMode#RMI} allows a property to be
   * propagated in RMI calls. Applying
   * {@link PropagationMode#TRANSACTION} allows a property to be
   * propagated between different global transactions.  Applying
   * {@link PropagationMode#JMS_QUEUE} allows a property to be
   * propagated to JMS consumers.  Applying
   * {@link PropagationMode#JMS_TOPIC} allows a property to be
   * propagated from JMS producers.  Applying
   * {@link PropagationMode#SOAP} allows a property to be
   * propagated across SOAP messages.  Applying
   * {@link PropagationMode#MIME_HEADER} allows a property to be
   * propagated from mail messages or cookies.
   * {@link PropagationMode}s are additive and can be used
   * together. {@link PropagationMode#GLOBAL} is an alias for
   * <code>PropagationMode.RMI, PropagationMode.SOAP,
   * PropagationMode.JMS_QUEUE</code> and
   * <code>PropagationMode.MIME_HEADERS</code>
   *
   * @param key a unique {@link String} that is used to obtain a
   * reference to a particular {@link WorkContext}. Keys are
   * encoded as {@link java.io.DataOutput#writeUTF}. In order to protect
   * the key namespace a good convention is to use package names as a
   * prefix. For example <code>com.you.SomeKey</code>.
   * @param ctx The {@link WorkContext} to put in the map.
   * specifies how the {@link WorkContext} entry can be modified.
   * @param propagationMode a bitwise-OR of
   * {@link PropagationMode} values prescribing how the
   * {@link WorkContext} entry should be propagated.
   * @return the previous WorkContext for key
   * @exception PropertyReadOnlyException if the property already
   * exists and is read-only.
   * @exception NullPointerException if the property or context is null.
   *
   * @see org.glassfish.contextpropagation.weblogic.workarea.PropagationMode
   */
  public WorkContext put(String key, WorkContext ctx,
                  int propagationMode)
    throws PropertyReadOnlyException;

  /**
   * Get the current WorkContextMap's context data for key. If the current
   * WorkContextMap has no value for key then null is returned.
   *
   * @param key a unique {@link String} that is used to obtain a
   * reference to a particular {@link WorkContext}
   * @return a {@link WorkContext} value or null if there is
   * none.
   */
  public WorkContext get(String key);

  /**
   * Get the current {@link WorkContextMap}'s
   * {@link org.glassfish.contextpropagation.weblogic.workarea.PropagationMode} value for key. If the current
   * {@link WorkContextMap} has no value for key then
   * PropagationMode.LOCAL is returned.
   *
   * @param key a unique {@link String} that is used to obtain a
   * reference to a particular {@link WorkContext}
   * @return a bitwise-OR of {@link org.glassfish.contextpropagation.weblogic.workarea.PropagationMode} values
   * prescribing how the {@link WorkContext} entry should be
   * propagated.
   */
  public int getPropagationMode(String key);

  /**
   * Given a PropagationMode {@link org.glassfish.contextpropagation.weblogic.workarea.PropagationMode} , this method will iterate
   * over the map and return true if the propagation mode is present. The method 
   * should return true if there exists at least one entry in the map which has 
   * at least one propagation mode specified by propMode
   *
   * @param PropagationMode  {@link org.glassfish.contextpropagation.weblogic.workarea.PropagationMode} value
   * @return boolean true or false
   */
  public boolean isPropagationModePresent(int PropagationMode);

  /**
   * Remove the context data for key from the current WorkContextMap.
   * This will throw {@link PropertyReadOnlyException} if the
   * permissions on the key do not allow deletion.
   *
   * @param key a unique {@link String} that is used to obtain a
   * reference to a particular {@link WorkContext}
   * @return the removed WorkContext
   * @exception NoWorkContextException if there is no mapping for
   * <code>key</code>
   * @exception PropertyReadOnlyException if a mapping exists but is
   * read-only.
   */
  public WorkContext remove(String key) throws NoWorkContextException,
                                        PropertyReadOnlyException;

  /**
   * Tests to see whether there are any {@link WorkContext}s in
   * existance in the map. Returns true if there are no elements in
   * the map, false otherwise.
   *
   * @return a <code>boolean</code> value
   */
  public boolean isEmpty();

  /**
   * Return a iterator for all of the current {@link WorkContext}
   * entries. If there are no entries then null is returned.
   *
   * @return a {@link Iterator} representing the current entries.
   */
  @SuppressWarnings("rawtypes")
  public Iterator iterator();

  /**
   * Return a iterator for all of the current {@link WorkContext}
   * keys. If there are no entries then null is returned.
   *
   * @return a {@link Iterator} representing the current keys.
   */
  @SuppressWarnings("rawtypes")
  public Iterator keys();
}
