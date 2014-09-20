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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Arrays;


/**
 * An implementation for propagating Serializable
 * {@link WorkContext}s.
 */
public class SerializableWorkContext implements PrimitiveWorkContext,
                                                       Serializable
{
  private static final long serialVersionUID = 3485637846065994552L;
  private byte[] data;
  private transient Serializable object;
  private transient boolean mutable = false;
  
  private static class Carrier implements Serializable {
    //This class carries the Serializable object along with its associated 
    //attributes
    private static final int VERSION = 1; //for interop
    private static final long serialVersionUID = -197593099539117489L;
    private Serializable serializable;
    private boolean mutable;
    
    @SuppressWarnings("unused")
    public Carrier() {
    }

    /*package*/ Carrier(Serializable object) {
      this.serializable = object;
    }

    /*package*/ Serializable getSerializable() {
      return serializable;
    }
    
    /*package*/ void setMutable() {
      this.mutable = true;
    }
    
    /*package*/ boolean isMutable() {
      return mutable;
    }
    
    private void writeObject(ObjectOutputStream out) throws IOException {
      out.writeInt(VERSION);
      out.writeObject(serializable);
      out.writeBoolean(mutable);
    }

    private void readObject(ObjectInputStream in)
      throws IOException, ClassNotFoundException {
      /*int version =*/ in.readInt();      
      serializable = (Serializable)in.readObject();
      mutable = in.readBoolean();
      /*
      // read more stuff based on version
      if(version >= 2) {
        //read stuff added for version 2
      }
      if(version >= 3) {
        //read stuff added for version 3
      }
      */
    }
  }

  public SerializableWorkContext() {
  }

  /* package */ SerializableWorkContext(Serializable s) throws IOException {
    this(s, false);
  }

  /* package */ SerializableWorkContext(Serializable s,
      boolean mutable) throws IOException {
    this.object = s;
    this.mutable = mutable;
    if(!mutable) {
      data = serialize(object);
    }
  }

  public String toString() {
    return "Serializable";
  }

  public Object get() {
    try {
      return getSerializable();
    }
    catch (IOException ioe) {
      return data;
    }
    catch (ClassNotFoundException cnfe) {
      return data;
    }
  }

  public Serializable getSerializable() throws IOException, ClassNotFoundException {
    if (object != null) {
      return object;
    }
    ByteArrayInputStream bin = new ByteArrayInputStream(data);
    ObjectInputStream in = new ObjectInputStream(bin) {
        @Override protected Class<?> resolveClass(ObjectStreamClass desc)
          throws IOException, ClassNotFoundException {
          try {
            return Class.forName(desc.getName(), false,
                                 Thread.currentThread().getContextClassLoader());
          } catch (ClassNotFoundException cnfe) {
            return super.resolveClass(desc);
          }
        }

        @SuppressWarnings("rawtypes")
        @Override protected Class<?> resolveProxyClass(String[] interfaces)
          throws IOException, ClassNotFoundException {
          ClassLoader ctxLoader = Thread.currentThread().getContextClassLoader();
          ClassLoader nonPublicLoader = null;
          boolean hasNonPublicInterface = false;
          Class[] classObjs = new Class[interfaces.length];
          for (int i = 0; i < interfaces.length; i++) {
            Class cl = Class.forName(interfaces[i], false, ctxLoader);
            if ((cl.getModifiers() & Modifier.PUBLIC) == 0) {
              if (hasNonPublicInterface) {
                if (nonPublicLoader != cl.getClassLoader()) {
                  throw new IllegalAccessError(
                    "conflicting non-public interface class loaders");
                }
              } else {
                nonPublicLoader = cl.getClassLoader();
                hasNonPublicInterface = true;
              }
            }
            classObjs[i] = cl;
          }
          try {
            return Proxy.getProxyClass(
              hasNonPublicInterface ? nonPublicLoader : ctxLoader, classObjs);
          } catch (IllegalArgumentException e) {
            throw new ClassNotFoundException(null, e);
          }
        }
      };

    Serializable obj = (Serializable)in.readObject();
    in.close();
    if(obj instanceof Carrier) {      
      object = ((Carrier)obj).getSerializable();
      this.mutable = ((Carrier)obj).isMutable();      
    } else {
      object = obj;
    }
    return object;
  }

  public boolean equals(Object obj) {
    if (obj instanceof SerializableWorkContext) {
      if(!mutable && !((SerializableWorkContext)obj).mutable) {
        return Arrays.equals(((SerializableWorkContext)obj).data, data);
      }
      return get().equals(((SerializableWorkContext)obj).get());
    }
    return false;
  }

  public void writeContext(WorkContextOutput out) throws IOException {
    if(mutable) {
      Carrier carrier = new Carrier(object);
      carrier.setMutable();
      data = serialize(carrier);
    }
    out.writeInt(data.length);
    out.write(data);
  }

  public void readContext(WorkContextInput in) throws IOException {
    data = new byte[in.readInt()];
    in.readFully(data);
  }

  private byte[] serialize(Serializable s) throws IOException {
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    ObjectOutputStream out = new ObjectOutputStream(bout);
    out.writeObject(s);
    out.flush();
    out.close();
    return bout.toByteArray();
  }

}
