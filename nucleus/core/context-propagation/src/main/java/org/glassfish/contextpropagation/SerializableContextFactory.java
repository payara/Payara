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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 *  The purpose of this factory is to support legacy custom work contexts
 *  from WebLogic Server (WLS). This is for the case where a custom work context 
 *  is NOT wrapped into a weblogic.workarea.SerializableWorkContext.
 *  It is also used when Glassfish uses a different class than WLS. In that
 *  case a factory can be registered to replace a WLS class with a Glassfish class. 
 *  This will work as
 *  long as the Glassfish and WLS classes have the same serialization profile.
 */  
 /*   - TODO QUESTION(For PM) Do we want to support legacy custom work contexts in open source glassfish.
 *     If not, we can move this factory interface to closed source.
 */
public interface SerializableContextFactory {
  public WLSContext createInstance();
  
  public interface WLSContext {
    /**
     * Writes the implementation of <code>Context</code> to the
     * {@link ContextOutput} data stream.
     */
    public void writeContext(ObjectOutput out) throws IOException;

    /**
     * Reads the implementation of <code>Context</code> from the
     * {@link ContextInput} data stream.
     */
    public void readContext(ObjectInput in) throws IOException;
    
    public interface WLSContextHelper {
      public byte[] toBytes(WLSContext ctx) throws IOException;
      public byte[] toBytes(Serializable object) throws IOException;
      public WLSContext readFromBytes(WLSContext ctx, byte[] bytes) throws IOException;
      public Serializable readFromBytes(byte[] bytes) throws IOException, ClassNotFoundException;
    }
    
    /**
     * HELPER is used internally to facilitate work with WLSContexts
     */
    public static WLSContextHelper HELPER = new WLSContextHelper() {
      @Override
      public byte[] toBytes(WLSContext ctx) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        ctx.writeContext(oos);
        oos.flush();       
        return baos.toByteArray();
      }
      @Override
      public WLSContext readFromBytes(WLSContext ctx, byte[] bytes) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(bais);
        ctx.readContext(ois);  
        return ctx;
      }
      @Override
      public byte[] toBytes(Serializable object) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(object);
        oos.flush();       
        return baos.toByteArray();
     }
      @Override
      public Serializable readFromBytes(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(bais);
        return (Serializable) ois.readObject();
      }      
    };

  }
  
}
