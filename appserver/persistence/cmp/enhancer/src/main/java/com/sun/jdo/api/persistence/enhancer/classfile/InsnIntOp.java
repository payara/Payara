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
 * An instruction which requires a integral constant as an immediate operand 
 */

public class InsnIntOp extends Insn {
  /* The operand */
  private int operandValue;

  /* public accessors */

  public int nStackArgs() {
    return VMOp.ops[opcode()].nStackArgs();
  }

  public int nStackResults() {
    return VMOp.ops[opcode()].nStackResults();
  }

  public String argTypes() {
    return VMOp.ops[opcode()].argTypes();
  }

  public String resultTypes() {
    return VMOp.ops[opcode()].resultTypes();
  }

  public boolean branches() {
    return opcode() == opc_ret;
  }

  public int value() {
    return operandValue;
  }

  /* package local methods */

  static String primType(int primIndex) {
    switch (primIndex) {
    case T_BOOLEAN:
        return "boolean";//NOI18N
    case T_CHAR:
        return "char";//NOI18N
    case T_FLOAT:
        return "float";//NOI18N
    case T_DOUBLE:
        return "double";//NOI18N
    case T_BYTE:
        return "byte";//NOI18N
    case T_SHORT:
        return "short";//NOI18N
    case T_INT:
        return "int";//NOI18N
    case T_LONG:
        return "long";//NOI18N
    default:
        throw new InsnError ("Invalid primitive type(" + primIndex + ")");//NOI18N
    }
  }

  void print (PrintStream out, int indent) {
    ClassPrint.spaces(out, indent);
    if (opcode() == opc_newarray) 
        out.println(offset() + "  opc_newarray  " + primType(operandValue));//NOI18N
    else
        out.println(offset() + "  " + opName(opcode()) + "  " + operandValue);//NOI18N
  }

  int store(byte[] buf, int index) {
    if (size() == 4) {
      /* prefix with an opc_wide */
      buf[index++] = (byte) opc_wide;
    }

    buf[index++] = (byte) opcode();
    if (size() > 2)
      buf[index++] = (byte)(operandValue >> 8);
    buf[index++] = (byte)(operandValue & 0xff);
    return index;
  }


  /* return the size of the instruction in bytes */

  int size() {
    switch(opcode()) {
    case opc_bipush:
    case opc_newarray:
      /* These are always 1 byte constants */
      return 2;

    case opc_sipush: /* a short constant */
      /* This is always a 2 byte constant */
      return 3;

    case opc_iload:
    case opc_lload:
    case opc_fload:
    case opc_dload:
    case opc_aload:
    case opc_istore:
    case opc_lstore:
    case opc_fstore:
    case opc_dstore:
    case opc_astore:
    case opc_ret:
      /* These can be one or two byte constants specifying a local var.
       * If a two byte constant, the constant is prefixed by a wide
       * instruction */
      if (operandValue < 256)
	return 2;
      else
	return 4;

    default:
        throw new InsnError ("invalid instruction " + opName(opcode()) +//NOI18N
                             " with an integer operand");//NOI18N
    }
  }


  InsnIntOp (int theOpcode, int theOperand, int pc) {
    super(theOpcode, pc);

    operandValue = theOperand;
  }


  InsnIntOp (int theOpcode, int theOperand) {
    super(theOpcode, NO_OFFSET);

    operandValue = theOperand;
    switch(theOpcode) {
    case opc_bipush:
    case opc_newarray:
      /* These are always 1 byte constants */

    case opc_sipush: /* a short constant */
      /* This is always a 2 byte constant */

    case opc_dload:
    case opc_lload:
    case opc_iload:
    case opc_fload:
    case opc_aload:
    case opc_istore:
    case opc_lstore:
    case opc_fstore:
    case opc_dstore:
    case opc_astore:
    case opc_ret:
      /* These can be one or two byte constants specifying a local var */
      break;

    default:
        throw new InsnError ("attempt to create an " + opName(theOpcode) +//NOI18N
                             " with an integer operand");//NOI18N
    }
  }
}
