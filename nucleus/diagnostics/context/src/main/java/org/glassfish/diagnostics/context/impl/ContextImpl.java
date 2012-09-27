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

package org.glassfish.diagnostics.context.impl;

import org.glassfish.contextpropagation.*;
import org.glassfish.diagnostics.context.Context;
import org.glassfish.diagnostics.context.ContextManager;

import java.util.EnumSet;

import java.util.logging.Logger;
import java.util.logging.Level;


/**
 * Base implementation of {@code org.glassfish.diagnostics.context.Context}.
 *
 * Delegates to a {@code org.glassfish.contextpropagation.View}
 */
public class ContextImpl
  implements Context, ViewCapable // TODO - ContextLifecycle too?
{
  private static final Logger LOGGER = ContextManager.LOGGER;

  private static final String CLASS_NAME = ContextImpl.class.getName();

  /**
  * The View to which this ContextImpl will delegate.
  *
  * Will be populated via public constructor (part of
  * ViewCapable contract)
  */
  private final View mView;

 /**
  * The Location of this ContextImpl.
  */
  private final Location mLocation;


 /**
  * Constructor required by DiagnosticContextViewFactory.
  *
  * This constructor forms part of overall contract with
  * contextpropagation package.
  *
  * @param view  The View to which this object is expected to delegate.
  */
  ContextImpl(View view, Location location){

    if ((view == null) || (location == null)){
      throw new IllegalArgumentException(
        ((view == null) ? "View must not be null. " : "") +
        ((location == null) ? "Location must not be null"  : "")
      );
    }

    if (LOGGER.isLoggable(Level.FINER)){
      LOGGER.logp(Level.FINER, CLASS_NAME, "<init>",
        "(view, location{"+location.getOrigin()+","+location.getLocationId()+"})");
    }

    mView = view;
    mLocation = location;
  }

  @Override // from Context
  public Location getLocation(){
    return mLocation;
  }

  @Override // from Context
  public <T> T put(String name, String value, boolean propagates){
    final EnumSet<PropagationMode> propagationModes;

    if (LOGGER.isLoggable(Level.FINER)){
      LOGGER.logp(Level.FINER, CLASS_NAME, "put(String, String, boolean)",
        "{" + mLocation.getOrigin() + "," + mLocation.getLocationId() + "}" +
        "(" + name + "," + value + "," + Boolean.toString(propagates) + ")");
    }

    if (propagates){
      propagationModes = ContextManagerImpl.sfGlobalPropagationModes;
    }
    else{
      propagationModes = ContextManagerImpl.sfLocalPropagationModes;
    }

    T retVal = mView.put(name, value, propagationModes);

    if (LOGGER.isLoggable(Level.FINER)){
      LOGGER.logp(Level.FINER, CLASS_NAME, "put(String, String, boolean)",
      "{" + mLocation.getOrigin() + "," + mLocation.getLocationId() + "}" +
      " returning " + retVal);
    }

    return retVal;
  }

  @Override // from Context
  public <T> T put(String name, Number value, boolean propagates){
    final EnumSet<PropagationMode> propagationModes;

    if (LOGGER.isLoggable(Level.FINER)){
      LOGGER.logp(Level.FINER, CLASS_NAME, "put(String, Number, boolean)",
      "{" + mLocation.getOrigin() + "," + mLocation.getLocationId() + "}" +
      "(" + name + "," + value + "," + Boolean.toString(propagates) + ")");
    }


    if (propagates){
      propagationModes = ContextManagerImpl.sfGlobalPropagationModes;
    }
    else{
      propagationModes = ContextManagerImpl.sfLocalPropagationModes;
    }

    T retVal = mView.put(name, value, propagationModes);

    if (LOGGER.isLoggable(Level.FINER)){
      LOGGER.logp(Level.FINER, CLASS_NAME, "put(String, Number, boolean)",
      "{" + mLocation.getOrigin() + "," + mLocation.getLocationId() + "}" +
      " returning " + retVal);
    }


    return retVal;
  }

  @Override // from Context
  public <T> T get(String name){
    T retVal = mView.get(name);

    if (LOGGER.isLoggable(Level.FINER)){
      LOGGER.logp(Level.FINER, CLASS_NAME, "get(String)",
      "{" + mLocation.getOrigin() + "," + mLocation.getLocationId() + "}" +
      "("+name+") returning " + retVal);
    }

    return retVal;
  }

  @Override // from Context
  public <T> T remove(String name){
    T retVal = mView.remove(name);

    if (LOGGER.isLoggable(Level.FINER)){
      LOGGER.logp(Level.FINER, CLASS_NAME, "remove(String)",
      "{" + mLocation.getOrigin() + "," + mLocation.getLocationId() + "}" +
      "("+name+") returning " + retVal);
    }

    return retVal;
  }

  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("{ContextImpl:");
    sb.append("{location:")
      .append(mLocation.getOrigin())
      .append(",")
      .append(mLocation.getLocationId())
      .append("}");
    sb.append("}");
    return sb.toString();
  }

}
