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
