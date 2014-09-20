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


import java.io.PrintStream;

/**
 * Special instruction form for the opc_iinc instruction
 */

public class InsnIInc extends Insn {

  /* The local variable slot to be incremented */
  private int localVarIndex;

  /* The amount by which the slot is to be incremented */
  private int value;

  /* public accessors */

  public int nStackArgs() {
    return 0;
  }

  public int nStackResults() {
    return 0;
  }

  /**
   * What are the types of the stack operands ?
   */
  public String argTypes() {
      return "";//NOI18N
  }

  /**
   * What are the types of the stack results?
   */
  public String resultTypes() {
      return "";//NOI18N
  }

  public boolean branches() {
    return false;
  }

  /**
   * The local variable slot to be incremented
   */
  public int varIndex() {
    return localVarIndex;
  }

  /**
   * The amount by which the slot is to be incremented 
   */
  public int incrValue() {
    return value;
  }
  
  /**
   * Constructor for opc_iinc instruction
   */
  public InsnIInc (int localVarIndex, int value) {
    this(localVarIndex, value, NO_OFFSET);
  }

  /* package local methods */

  InsnIInc (int localVarIndex, int value, int pc) {
    super(opc_iinc, pc);

    this.localVarIndex = localVarIndex;
    this.value =value;
  }

  void print (PrintStream out, int indent) {
    ClassPrint.spaces(out, indent);
    out.println(offset() + "  opc_iinc  " + //NOI18N
		localVarIndex + "," + value);//NOI18N
  }

  int store(byte[] buf, int index) {
    if (isWide())
      buf[index++] = (byte) opc_wide;
    buf[index++] = (byte) opcode();
    if (isWide()) {
      index = storeShort(buf, index, (short) localVarIndex);
      index = storeShort(buf, index, (short) value);
    } else {
      buf[index++] = (byte)localVarIndex;
      buf[index++] = (byte)value;
    }
    return index;
  }

  int size() {
    return isWide() ? 6 : 3;
  }

  private boolean isWide() {
    return (value > 127 || value < -128 || localVarIndex > 255);
  }

}
