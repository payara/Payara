/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jdo.api.persistence.enhancer.classfile;

import java.io.*;
import java.util.Vector;
import java.util.Enumeration;

/**
 * Represents the LocalVariableTable attribute within a
 * method in a class file.
 */

public class LocalVariableTableAttribute extends ClassAttribute {
  /* The expected attribute name */
    public static final String expectedAttrName = "LocalVariableTable";//NOI18N

  /* The list of local variables */
  private Vector localTable;

  /* public accessors */

  /**
   * Returns an enumeration of the local variables in the table
   * Each element is a LocalVariable
   */
  Enumeration variables() {
    return localTable.elements();
  }

  /**
   * Constructor for a local variable table
   */
  public LocalVariableTableAttribute(
	ConstUtf8 nameAttr, Vector lvarTable) {
    super(nameAttr);
    localTable = lvarTable;
  }

  /* package local methods */

  static LocalVariableTableAttribute read(
	ConstUtf8 attrName, DataInputStream data, CodeEnv env)
    throws IOException {
    int nVars = data.readUnsignedShort();
    Vector lvarTable = new Vector();
    while (nVars-- > 0) {
      lvarTable.addElement(LocalVariable.read(data, env));
    }
        
    return new LocalVariableTableAttribute(attrName, lvarTable);
  }

  void write(DataOutputStream out) throws IOException {
    out.writeShort(attrName().getIndex());
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream tmp_out = new DataOutputStream(baos);
    tmp_out.writeShort(localTable.size());
    for (int i=0; i<localTable.size(); i++)
      ((LocalVariable) localTable.elementAt(i)).write(tmp_out);

    tmp_out.flush();
    byte tmp_bytes[] = baos.toByteArray();
    out.writeInt(tmp_bytes.length);
    out.write(tmp_bytes, 0, tmp_bytes.length);
  }

  void print(PrintStream out, int indent) {
    ClassPrint.spaces(out, indent);
    out.println("LocalVariables: ");//NOI18N
    for (int i=0; i<localTable.size(); i++) {
      ((LocalVariable) localTable.elementAt(i)).print(out, indent+2);
    }
  }
}

