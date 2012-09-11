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

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;

import org.glassfish.contextpropagation.InsufficientCredentialException;
import org.glassfish.contextpropagation.PropagationMode;
import org.glassfish.contextpropagation.bootstrap.ContextAccessController;
import org.glassfish.contextpropagation.bootstrap.ContextBootstrap;
import org.glassfish.contextpropagation.internal.SimpleMap.Filter;

/**
 * This class is used by the ContextMap for:
 *  - checking permissions
 *  - setting isOriginator to true, since entries created via this API are
 *    created for the first time here.
 */
public class AccessControlledMap {
  private static final boolean IS_ORIGINATOR = true;
  protected SimpleMap simpleMap = new SimpleMap(); 
  private final ContextAccessController contextAccessController = 
      ContextBootstrap.getContextAccessController();

  public <T> T get(String key) throws InsufficientCredentialException {
    Entry entry = simpleMap.getEntry(key); 
    if (entry == null) {
      if (contextAccessController.isAccessAllowed(key, ContextAccessLevel.READ)) {
        return null;
      }
    } else {
      if (entry.allowAllToRead ||
          contextAccessController.isAccessAllowed(key, ContextAccessLevel.READ)) {
        return (T) entry.getValue();
      }
    } 
    throw new InsufficientCredentialException();
  }

  @SuppressWarnings("unchecked")
  public <T> T put(String key, Entry entry) throws InsufficientCredentialException {   
    Entry oldEntry = simpleMap.getEntry(key);
    contextAccessController.checkAccessAllowed(key, 
        oldEntry == null ? ContextAccessLevel.CREATE : ContextAccessLevel.UPDATE);

    simpleMap.put(key, entry.init(IS_ORIGINATOR, 
        contextAccessController.isEveryoneAllowedToRead(key)));
    return (T) (oldEntry == null ? null : oldEntry.getValue());
  }

  public <T> T remove(String key) throws InsufficientCredentialException {
    contextAccessController.checkAccessAllowed(key, ContextAccessLevel.DELETE);
    return (T) simpleMap.remove(key);
  }

  public EnumSet<PropagationMode> getPropagationModes(String key) throws InsufficientCredentialException {
    Entry entry = simpleMap.getEntry(key);
    if (entry == null) {
      if (contextAccessController.isAccessAllowed(key, ContextAccessLevel.READ)) {
        return null;
      }
    } else {
      if (entry.allowAllToRead || 
          contextAccessController.isAccessAllowed(key, ContextAccessLevel.READ)) {
        return entry.propagationModes;      
      }}
    throw new InsufficientCredentialException();
  }

  public static enum ContextAccessLevel {
    CREATE,
    READ,
    UPDATE,
    DELETE
  } // Move to the same place as WorkcContextAccessController interface

  private final Filter AccessCheckerFilter = new Filter() {
    @Override
    public boolean keep(java.util.Map.Entry<String, Entry> mapEntry,
        PropagationMode mode) {
      return contextAccessController.isAccessAllowed(mapEntry.getKey(), ContextAccessLevel.READ);
    }
  };
  
  public Iterator<Map.Entry<String, Entry>> entryIterator() {
    return simpleMap.iterator(AccessCheckerFilter, null);
  }
  
  public Entry getEntry(String key) {
    return simpleMap.getEntry(key);
  }

  public Iterator<String> names() {
    final Iterator<Map.Entry<String, Entry>> iter = entryIterator();
    return new Iterator<String>() {
      @Override public boolean hasNext() {
        return iter.hasNext();
      }
      @Override public String next() {
        return iter.next().getKey();
      }
      @Override public void remove() {
        iter.remove();
      }      
    };
  }

}
