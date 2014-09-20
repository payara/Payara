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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.EnumSet;

import org.glassfish.contextpropagation.PropagationMode;
import org.glassfish.contextpropagation.bootstrap.ContextBootstrap;
import org.glassfish.contextpropagation.bootstrap.LoggerAdapter.Level;
import org.glassfish.contextpropagation.bootstrap.LoggerAdapter.MessageID;
import org.glassfish.contextpropagation.internal.Entry;
import org.glassfish.contextpropagation.internal.Entry.ContextType;

/**
 * This class provides a framework for collecting catalog information, writing
 * the catalog ahead of all other contexts and for skipping over the deserialization
 * of contexts that cannot be deserialized.
 */
public abstract class AbstractWireAdapter implements WireAdapter {
  protected static final int READ_LIMIT = 32000;
  protected String key;
  private OutputStream outputStream;
  private ByteArrayOutputStream bufferedStream;
  PositionAwareObjectOutputStream oos;
  protected ObjectInputStream ois;
  Catalog catalog = new Catalog();
  private boolean needsToReadCatalog = true;

  @Override
  public final void prepareToWriteTo(OutputStream out) throws IOException {
    outputStream = out;
    bufferedStream = new ByteArrayOutputStream();
    oos = new PositionAwareObjectOutputStream(bufferedStream); 
    writeHeader(oos);
    catalog.add(oos.position());
  }

  protected abstract void writeHeader(ObjectOutputStream os)  throws IOException;

  @Override
  public final <T> void write(String key, Entry entry) throws IOException {
    write(oos, key, entry.getValue(), entry.getContextType(), entry.getPropagationModes(), 
        entry.getContextType() == ContextType.OPAQUE ? entry.getClassName() : null);
    catalog.add(oos.position());
    ContextBootstrap.debug(MessageID.WRITE_ENTRY, catalog.positions.size(), key, entry);
  }

  protected abstract void write(ObjectOutputStream oos, String key, Object value, ContextType contextType, 
      EnumSet<PropagationMode> propagationModes, String className) throws IOException;

  @Override
  public final void prepareToReadFrom(InputStream is) throws IOException {
    catalog.prepareToRead();
    ois = new ResettableObjectInputStream(is);
    readHeader(ois, catalog);  
  }

  protected abstract void readHeader(ObjectInputStream ois, Catalog catalog) throws IOException;

  @Override
  public final void flush() throws IOException {
    write(oos, catalog);
    writeFooter(oos);
    oos.flush();
    byte[] contents = bufferedStream.toByteArray();
    catalog.updateCatalogMetadata(contents);
    outputStream.write(contents); 
  }

  protected abstract void write(ObjectOutputStream objectOutputStream, Catalog catalog) throws IOException;

  protected abstract void writeFooter(ObjectOutputStream objectOutputStream) throws IOException;
  
  @Override
  public final String readKey() throws IOException {
    try {
      catalog.upItemNumber(1);
      key = nextKey();
      if (key == null) {
        read(false, ois, catalog);
      }
    } catch (IOException ioe) {
      ContextBootstrap.getLoggerAdapter().log(Level.ERROR, ioe,
          MessageID.ERROR_IOEXCEPTION_WHILE_READING_KEY, key);
      if (catalog.skipToNextItem(ois)) {
        key = readKey();
      } else {
        return null;
      }
    }
    ContextBootstrap.debug(MessageID.READ_KEY, key);
    return key;
  }

  protected abstract void read(boolean mandatory, ObjectInputStream ois, Catalog catalog) throws IOException;

  protected abstract String nextKey() throws IOException;

  @Override
  public final Entry readEntry() throws IOException, ClassNotFoundException {
    try {
      return nextEntry();
    } catch (ClassNotFoundException cnfe) {
      ContextBootstrap.getLoggerAdapter().log(Level.ERROR, cnfe,
          MessageID.ERROR_CLASSNOTFOUND, key);
      recover(ois, catalog);
      return null;
    } catch (IOException ioe) {
      ContextBootstrap.getLoggerAdapter().log(Level.ERROR, ioe,
          MessageID.ERROR_IOEXCEPTION_WHILE_READING_ENTRY, key);
      recover(ois, catalog);
      return null;
    }
  }
  
  private void recover(ObjectInputStream ois, Catalog catalog) throws IOException, ClassNotFoundException {
    if (needsToReadCatalog ) {
      read(true, ois, catalog);
      needsToReadCatalog = false;
    }
    catalog.skipToNextItem(ois);
  }

  protected abstract Entry nextEntry() throws ClassNotFoundException, IOException;
}
