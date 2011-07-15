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
 * Special instruction form for the opc_multianewarray instruction
 */

public class InsnMultiDimArrayNew extends Insn {
  /* The array class for creation */
  private ConstClass classOp;

  /* The number of dimensions present on the stack */
  private int nDimsOp;

  /* public accessors */

  public boolean isSimpleLoad() {
    return false;
  }

  public int nStackArgs() {
    return nDimsOp;
  }

  public int nStackResults() {
    return 1;
  }

  /**
   * What are the types of the stack operands ?
   */
  public String argTypes() {
    StringBuffer buf = new StringBuffer();
    for (int i=0; i<nDimsOp; i++) {
        buf.append("I");//NOI18N
    }
    return buf.toString();
  }

  /**
   * What are the types of the stack results?
   */
  public String resultTypes() {
      return "A";//NOI18N
  }

  public boolean branches() {
    return false;
  }

  /**
   * Return the array class being created
   */
  public ConstClass arrayClass() {
    return classOp;
  }

  /**
   * Sets the array class being created
   */
  public void setArrayClass(ConstClass classOp) {
    this.classOp = classOp;
  }

  /**
   * Return the number of dimensions of the array class being created
   */
  public int nDims() {
    return nDimsOp;
  }

  /**
   * Constructor for opc_multianewarray.
   * classOp must be an array class
   * nDimsOp must be > 0 and <= number of array dimensions for classOp
   */
  public InsnMultiDimArrayNew (ConstClass classOp, int nDimsOp) {
    this(classOp, nDimsOp, NO_OFFSET);
  }

  /* package local methods */

  InsnMultiDimArrayNew (ConstClass classOp, int nDimsOp, int offset) {
    super(opc_multianewarray, offset);

    this.classOp = classOp;
    this.nDimsOp = nDimsOp; 

    if (classOp == null || nDimsOp < 1)
	throw new InsnError ("attempt to create an opc_multianewarray" +//NOI18N
			     " with invalid operands");//NOI18N
  }

  

  void print (PrintStream out, int indent) {
    ClassPrint.spaces(out, indent);
    out.println(offset() + "  opc_multianewarray  pool(" +//NOI18N
		classOp.getIndex() + ")," + nDimsOp);//NOI18N
  }

  int store(byte[] buf, int index) {
    buf[index++] = (byte) opcode();
    index = storeShort(buf, index, (short) classOp.getIndex());
    buf[index++] = (byte) nDimsOp;
    return index;
  }

  int size() {
    return 4;
  }

  static InsnMultiDimArrayNew read (InsnReadEnv insnEnv, int myPC) {
    ConstClass classOp = (ConstClass)
      insnEnv.pool().constantAt(insnEnv.getUShort());
    int nDims = insnEnv.getUByte();
    return new InsnMultiDimArrayNew(classOp, nDims, myPC);
  }

}
