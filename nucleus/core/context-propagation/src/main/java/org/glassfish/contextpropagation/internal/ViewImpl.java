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
package org.glassfish.contextpropagation.internal;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.glassfish.contextpropagation.PropagationMode;
import org.glassfish.contextpropagation.View;
import org.glassfish.contextpropagation.bootstrap.ContextBootstrap;
import org.glassfish.contextpropagation.internal.Entry.ContextType;

/* 
 *  - Consider a View that uses shorter keys (key minus the prefix)
 *     - It would not use the workarea map storage but would have its own with its own serialization methods
 *     - Could produce more compact on the wire representation even for the WLS adapter
 *     - The long keys have the advantage that the serialization is already handled by workarea,
 *       and that the constituents would be accessible from WLS -- that is both good and bad.
 *  - View does not need to extend SimpleMap, it was done that way for a quick prototype
 *     - The method signatures can be even more convenient 
 *       put(String,  <TYPE see ContextMapAccessor>) instead of put(String, Entry)
 */
/**
 * Provides access to a subset of the ContextMap. 
 * Views bypass security checks. However Views are hidden in ViewCapable 
 * instances for which access is verified.
 */
public class ViewImpl implements View {
  private String prefix;
  private SimpleMap sMap;
  private Set<String> names = new HashSet<String>();

  protected ViewImpl(String prefix) {
    this.prefix = prefix + ".";
    sMap = ((AccessControlledMap) Utils.mapFinder.getMapAndCreateIfNeeded()).simpleMap;
  }

  @Override
  public <T> T get(String name) {
    return (T) sMap.get(makeKey(name));
  }

  private String makeKey(String name) {
    return name == null ? null : prefix + name;
  }

  private String newKey(String name) {
    names.add(name);
    return makeKey(name);
  }

  private boolean allowAllToRead(String name) {
    return ContextBootstrap.getContextAccessController().isEveryoneAllowedToRead(newKey(name));
  }

  @Override
  public <T> T put(String name, String context,
      EnumSet<PropagationMode> propagationModes) {
    return (T) sMap.put(newKey(name), new Entry(context, propagationModes, ContextType.STRING).init(true, allowAllToRead(name)));
  }

  @Override
  public <T, U extends Number> T put(String name, U context,
      EnumSet<PropagationMode> propagationModes) {
    return (T) sMap.put(newKey(name), new Entry(context, propagationModes, 
        ContextType.fromNumberClass(context.getClass())).init(true, allowAllToRead(name)));
  }

  @Override
  public <T> T put(String name, Boolean context,
      EnumSet<PropagationMode> propagationModes) {
    return (T) sMap.put(newKey(name), new Entry(context, propagationModes, ContextType.BOOLEAN).init(true, allowAllToRead(name)));  }

  @Override
  public <T> T put(String name, Character context,
      EnumSet<PropagationMode> propagationModes) {
    return (T) sMap.put(newKey(name), new Entry(context, propagationModes, ContextType.CHAR).init(true, allowAllToRead(name)));
  }

   public <T> T putSerializable(String name, Serializable context,
      EnumSet<PropagationMode> propagationModes, boolean allowAllToRead) {
    return (T) sMap.put(newKey(name), new Entry(context, propagationModes, ContextType.SERIALIZABLE).init(true, allowAllToRead));
  }

  @Override
  public <T> T remove(String name) {
    names.remove(name);
    return (T) sMap.remove(makeKey(name));
  }

  public void clean() {
    for (String name : names) remove(name);
  }

}
