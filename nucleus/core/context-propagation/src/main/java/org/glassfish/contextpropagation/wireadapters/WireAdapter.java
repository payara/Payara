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
package org.glassfish.contextpropagation.wireadapters;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.glassfish.contextpropagation.SerializableContextFactory;
import org.glassfish.contextpropagation.bootstrap.LoggerAdapter.MessageID;
import org.glassfish.contextpropagation.internal.Entry;
import org.glassfish.contextpropagation.internal.Utils;

/**
 * Concrete WireAdapter instances define the methods for transforming
 * Entry's into serialized wire data and vice versa.
 * 
 * WireAdapters should thrive to:
 *  - encode data efficiently and compactly
 *  - be resilient to errors -- a problem decoding an entry should not cause the 
 *    decoding of subsequent entries to fail.
 */
public interface WireAdapter {
  static interface WireAdapterHelper {
    SerializableContextFactory findContextFactory(String contextName, String wireClassName);
    public void registerContextFactoryForContextNamed(String contextName, 
        String wireClassName, SerializableContextFactory factory);
    public void registerContextFactoryForClass(Class<?> contextClass, 
        String wireClassName, SerializableContextFactory factory);
  }
  public void prepareToWriteTo(OutputStream out) throws IOException;
  public <T> void write(String key, Entry entry) throws IOException;
  public void flush() throws IOException;
  public void prepareToReadFrom(InputStream is) throws IOException;
  public String readKey() throws IOException;
  public Entry readEntry() throws IOException, ClassNotFoundException;
  
  public static final WireAdapterHelper HELPER = new WireAdapterHelper() {   
    Map<String, SerializableContextFactory> contextFactoriesByContextName = new HashMap<String, SerializableContextFactory>();
    Map<String, String> wireClassNameByContextName = new HashMap<String, String>();
    public void registerContextFactoryForContextNamed(String contextName, 
        String wireClassName, SerializableContextFactory factory) {
      Utils.validateFactoryRegistrationArgs("contextName", 
          MessageID.WARN_FACTORY_ALREADY_REGISTERED_FOR_NAME, contextName, 
          factory, contextFactoriesByContextName);
      contextFactoriesByContextName.put(contextName, factory);
      wireClassNameByContextName.put(contextName, wireClassName);
    }

    Map<String, SerializableContextFactory> contextFactoriesByClassName = new HashMap<String, SerializableContextFactory>();
    Map<String, String> wireClassNameByClassName = new HashMap<String, String>();
    public void registerContextFactoryForClass(Class<?> contextClass, 
        String wireClassName, SerializableContextFactory factory) {
      Utils.validateFactoryRegistrationArgs("Context class name", 
          MessageID.WARN_FACTORY_ALREADY_REGISTERED_FOR_CLASS, 
          contextClass.getName(), factory, contextFactoriesByClassName);
      contextFactoriesByClassName.put(wireClassName, factory);
      wireClassNameByClassName.put(contextClass.getName(), wireClassName);
    }

    @Override
    public SerializableContextFactory findContextFactory(String contextName,
        String wireClassName) {
      SerializableContextFactory factory = contextFactoriesByClassName.get(wireClassName);
      if (factory == null) {
        factory = contextFactoriesByContextName.get(contextName);
      }
      return factory;
    }
  };
}
