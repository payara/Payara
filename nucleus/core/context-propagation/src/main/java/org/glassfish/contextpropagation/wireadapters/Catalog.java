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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.glassfish.contextpropagation.bootstrap.ContextBootstrap;
import org.glassfish.contextpropagation.bootstrap.LoggerAdapter.MessageID;

/**
 * Contains metadata that identifies the each context entry on a stream.
 * This information helps recover from an unrecoverable IO error when reading
 * context-propagation data from a stream. It makes it possible to skip a 
 * corrupted context.
 */
@SuppressWarnings("serial")
public class Catalog implements Serializable {
  public static final String CATALOG_KEY = "org.glassfish.contextpropagation.catalog";
  public static final String CATALOG_META_KEY = "org.glassfish.contextpropagation.catalog_meta";
  public Catalog() {};
  transient List<Short> positions = new LinkedList<Short>();
  transient int itemNumber;
  transient short start, end;

  public void add(short position) {
    positions.add(position);
  }
  
  public void upItemNumber(int delta) { itemNumber += delta; }

  public void prepareToRead() { 
    itemNumber = 0;
    end = 0;
  }

  public boolean skipToNextItem(ObjectInputStream ois) throws IOException {
    if (end > 0)  {
      if (itemNumber < positions.size()) {
        int startPosition = positions.get(itemNumber);
        int endPosition = itemNumber + 1 < positions.size() ? positions.get(itemNumber + 1) : -1;
        ContextBootstrap.debug(MessageID.ATTEMPT_TO_SKIP_TO_NEXT_ITEM, itemNumber + 1, startPosition, endPosition);
        ois.reset();
        for (int skipped = 0; 
            skipped < startPosition;
            skipped += ois.skipBytes(startPosition - skipped));
        return true;
      } else {
        ContextBootstrap.debug(MessageID.ERROR_NO_MORE_ITEMS);
        return false; 
      }
    } else {
      ContextBootstrap.debug(MessageID.NO_CATALOG);
      return false;
    }
  }

  public void write(ObjectOutputStream os) throws IOException {
    writeObject(os);
    os.flush();
  }

  private void writeObject(java.io.ObjectOutputStream out)
      throws IOException {
    out.writeShort(1); // Writing version to support future changes to catalog
    add((short) (positions.get(positions.size() - 1) + Short.SIZE / Byte.SIZE * (positions.size() + 2)));
    out.writeShort(positions.size());
    for (short s : positions) {
      out.writeShort(s);
    }
    ContextBootstrap.debug(MessageID.WRITE_CATALOG, this);
  }

  public void read(ObjectInputStream is) throws IOException {
    try {
      readObject(is);
    } catch (ClassNotFoundException impossible) {
      throw new AssertionError(impossible);
    }
  }

  private void readObject(ObjectInputStream in)
      throws IOException, ClassNotFoundException {
    // Causes an NPE even though we create an instance in the field declaration line ??? positions.clear();
    positions = new LinkedList<Short>();
    short version = in.readShort(); // reading the catalog version
    ContextBootstrap.debug(MessageID.READ_CATALOG_VERSION, version);
    short count = in.readShort();
    while(count-- > 0) {
      positions.add(in.readShort());
    }
    ContextBootstrap.debug(MessageID.READ_CATALOG, positions);
  }

  @Override
  public String toString() {
    return "item number: " + itemNumber + " positions: " + positions.toString();
  }

  public void updateCatalogMetadata(byte[] contents) {
    short offset = findOffset(contents);
    short position = positions.get(positions.size() - 2);
    contents[offset++] = (byte) (position >>> 8);
    contents[offset++] = (byte) position;
    short end =  positions.get(positions.size() - 1);
    contents[offset++] = (byte) (end >>> 8);
    contents[offset] = (byte) end;
  }

  private short findOffset(byte[] contents) {
    short xCount = 0;
    for (short i = 0; i < contents.length; i++) {
      byte b = contents[i];
      if (b == 'x') {
        if (++xCount == 4) return (short) (i - 3);
      } else {
        xCount = 0;
      }
    }
    throw new RuntimeException("Could not determine the offset");
  }

  public void setMeta(long meta)  {
    start = (short) (meta >>> 16 & 0xFFFF);
    end = (short) (meta & 0xFFFF);   
  }

  public void setPosisionsFrom(Catalog catalog) {
    positions = catalog.positions;
  }

  @Override
  public boolean equals(Object obj) {
    return obj != null && obj instanceof Catalog &&
        positions.equals(((Catalog) obj).positions);
  }

  @Override
  public int hashCode() {
     return super.hashCode();
  }

  public short getStart() {
    return start;
  }

}
