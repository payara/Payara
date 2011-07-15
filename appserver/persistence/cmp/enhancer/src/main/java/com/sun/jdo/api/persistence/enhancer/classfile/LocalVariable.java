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

/**
 * Represents a local variable within a LocalVariableTable within
 * a CodeAttribute in a class file.
 */

public class LocalVariable {
  /* The pc at which the variable becomes effecive */
  private InsnTarget varStartPC; /* inclusive */

  /* The pc at which the variable becomes in-effecive */
  private InsnTarget varEndPC;   /* exclusive */

  /* The name of the variable */
  private ConstUtf8 varName;

  /* The type signature of the variable */
  private ConstUtf8 varSig;

  /* The slot to which the variable is assigned */
  private int varSlot;

  /* public accessors */

  /**
   * Constructor for a local variable
   */
  public LocalVariable(InsnTarget startPC, InsnTarget endPC,
                       ConstUtf8 name, ConstUtf8 sig, int slot) {
    varStartPC = startPC;
    varEndPC = endPC;
    varName = name;
    varSig = sig;
    varSlot = slot;
  }

  /* package local methods */

  static LocalVariable read(DataInputStream data, CodeEnv env)
    throws IOException {
    int startPC = data.readUnsignedShort();
    InsnTarget startPCTarget = env.getTarget(startPC);
    int length = data.readUnsignedShort();
    InsnTarget endPCTarget = env.getTarget(startPC+length);
    ConstUtf8 name = 
      (ConstUtf8) env.pool().constantAt(data.readUnsignedShort());
    ConstUtf8 sig = 
      (ConstUtf8) env.pool().constantAt(data.readUnsignedShort());
    int slot = data.readUnsignedShort();
    return new LocalVariable(startPCTarget, endPCTarget, name, sig, slot);
  }

  void write(DataOutputStream out) throws IOException {
    out.writeShort(varStartPC.offset());
    out.writeShort(varEndPC.offset() - varStartPC.offset());
    out.writeShort((varName == null) ? 0 : varName.getIndex());
    out.writeShort((varSig == null) ? 0 : varSig.getIndex());
    out.writeShort(varSlot);
  }

  public void print(PrintStream out, int indent) {
    ClassPrint.spaces(out, indent);
    out.print("'" + ((varName == null) ? "(null)" : varName.asString()) + "'");//NOI18N
    out.print(" sig = " + ((varSig == null) ? "(null)" : varSig.asString()));//NOI18N
    out.print(" start_pc = " + Integer.toString(varStartPC.offset()));//NOI18N
    out.print(" length = " +//NOI18N
	     Integer.toString(varEndPC.offset() - varStartPC.offset()));
    out.println(" slot = " + Integer.toString(varSlot));//NOI18N
  }

}

