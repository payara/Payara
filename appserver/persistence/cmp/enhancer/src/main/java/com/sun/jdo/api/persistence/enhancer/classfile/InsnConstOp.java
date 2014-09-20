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
 * An instruction which requires a single constant from the constant
 * pool as an immediate operand 
 */

public class InsnConstOp extends Insn {
  /* The constant from the constant pool */
  private ConstBasic constValue;

  /* public accessors */

  public int nStackArgs() {
    int n = VMOp.ops[opcode()].nStackArgs();
    if (n >= 0) 
      return n;
    switch (opcode()) {
    case opc_putstatic:
    case opc_putfield:
      {
	ConstFieldRef fld = (ConstFieldRef) constValue;
	String sig = fld.nameAndType().signature().asString();
	if (sig.equals("J") || sig.equals("D"))//NOI18N
	  return (opcode() == opc_putfield) ? 3 : 2;
	return (opcode() == opc_putfield) ? 2 : 1;
      }
    case opc_invokevirtual:
    case opc_invokespecial:
    case opc_invokestatic:
      /* handle interface invoke too */
    case opc_invokeinterface:
      {
	ConstBasicMemberRef meth = (ConstBasicMemberRef) constValue;
	String sig = meth.nameAndType().signature().asString();
	int nMethodArgWords = Descriptor.countMethodArgWords(sig);
	return nMethodArgWords +
	  ((opcode() == opc_invokestatic) ? 0 : 1);
      }
    default:
        throw new InsnError("unexpected variable opcode");//NOI18N
    }
  }

  public int nStackResults() {
    int n = VMOp.ops[opcode()].nStackResults();
    if (n >= 0) 
      return n;
    switch (opcode()) {
    case opc_getstatic:
    case opc_getfield:
      {
	ConstFieldRef fld = (ConstFieldRef) constValue;
	String sig = fld.nameAndType().signature().asString();
	if (sig.equals("J") || sig.equals("D"))//NOI18N
	  return 2;
	return 1;
      }
    case opc_invokevirtual:
    case opc_invokespecial:
    case opc_invokestatic:
      /* handle interface invoke too */
    case opc_invokeinterface:
      {
	ConstBasicMemberRef meth = (ConstBasicMemberRef) constValue;
	return Descriptor.countMethodReturnWords(
	  meth.nameAndType().signature().asString());
      }
    default:
        throw new InsnError("unexpected variable opcode");//NOI18N
    }
  }

  public String argTypes() {
    switch (opcode()) {
    case opc_putstatic:
    case opc_putfield:
      {
	ConstFieldRef fld = (ConstFieldRef) constValue;
	String sig = fld.nameAndType().signature().asString();
	if (opcode() == opc_putstatic)
	  return sig;
	else
	  return descriptorTypeOfObject(fld) + sig;
      }
    case opc_invokevirtual:
    case opc_invokespecial:
    case opc_invokestatic:
      /* handle interface invoke too */
    case opc_invokeinterface:
      {
	ConstBasicMemberRef meth = (ConstBasicMemberRef) constValue;
	String argSig =
	  Descriptor.extractArgSig(meth.nameAndType().signature().asString());
	if (opcode() == opc_invokestatic)
	  return argSig;
	else
	  return descriptorTypeOfObject(meth) + argSig;
      }
    default:
      return VMOp.ops[opcode()].argTypes();
    }
  }

  public String resultTypes() {
    switch (opcode()) {
    case opc_invokevirtual:
    case opc_invokespecial:
    case opc_invokestatic:
      /* handle interface invoke too */
    case opc_invokeinterface:
      {
	ConstBasicMemberRef meth = (ConstBasicMemberRef) constValue;
	String resultSig = Descriptor.extractResultSig(
	  meth.nameAndType().signature().asString());
	if (resultSig.equals("V"))//NOI18N
            return "";//NOI18N
	return resultSig;
      }
    case opc_getstatic:
    case opc_getfield:
      {
	ConstFieldRef fld = (ConstFieldRef) constValue;
	return fld.nameAndType().signature().asString();
      }
    case opc_ldc:
    case opc_ldc_w:
    case opc_ldc2_w:
      {
	ConstValue constVal = (ConstValue) constValue;
	return constVal.descriptor();
      }
    default:
      return VMOp.ops[opcode()].resultTypes();
    }
  }

  public boolean branches() {
    /* invokes don't count as a branch */
    return false;
  }

  /**
   * Return the constant pool entry which is the immediate operand
   */
  public ConstBasic value() {
    return constValue;
  }
    
  /**
   * Modify the referenced constant
   */
  public void setValue(ConstBasic newValue) {
    checkConstant(newValue);
    constValue = newValue;
  }
    
  /* package local methods */

  void print (PrintStream out, int indent) {
    ClassPrint.spaces(out, indent);
    out.println(offset() + "  " + opName(opcode()) + "  pool(" + //NOI18N
		constValue.getIndex() + ")");//NOI18N
  }

  int store(byte[] buf, int index) {
    if (opcode() == opc_ldc && !isNarrowldc())
      buf[index++] = (byte) opc_ldc_w;
    else
      buf[index++] = (byte) opcode();
    int constIndex = constValue.getIndex();
    if (size() == 3)
      buf[index++] = (byte) (constIndex >> 8);
    buf[index++] = (byte)(constIndex & 0xff);
    return index;
  }

  int size() {
    return isNarrowldc() ? 2 : 3;
  }

  private boolean isNarrowldc() {
    return (opcode() == opc_ldc && constValue.getIndex() < 256);
  }
    

  InsnConstOp (int theOpcode, ConstBasic theOperand) {
    this(theOpcode, theOperand, NO_OFFSET);
  }

  InsnConstOp (int theOpcode, ConstBasic theOperand, int pc) {
    super(theOpcode, pc);
    constValue = theOperand;
    checkConstant(theOperand);
    if (theOpcode == opc_invokeinterface) 
        throw new InsnError("attempt to create an " + opName(theOpcode) +//NOI18N
                            " as an InsnConstOp instead of InsnInterfaceInvoke");//NOI18N
  }

  /* used only by InsnInterfaceInvoke, to make sure that opc_invokeinterface cannot
   * come through the wrong path and miss its extra nArgsOp */
  InsnConstOp (int theOpcode, ConstInterfaceMethodRef theOperand, int pc) {
    super(theOpcode, pc);
    constValue = theOperand;
    checkConstant(theOperand);
  }

  private void checkConstant (ConstBasic operand) {
    switch(opcode()) {
    case opc_ldc:
    case opc_ldc_w:
    case opc_ldc2_w:
      /* ConstValue */
      if (operand == null ||
	  (! (operand instanceof ConstValue)))
          throw new InsnError ("attempt to create an " + opName(opcode()) +//NOI18N
                               " without a ConstValue operand");//NOI18N
      break;

    case opc_getstatic:
    case opc_putstatic:
    case opc_getfield:
    case opc_putfield:
      /* ConstFieldRef */
      if (operand == null ||
	  (! (operand instanceof ConstFieldRef)))
          throw new InsnError ("attempt to create an " + opName(opcode()) +//NOI18N
                               " without a ConstFieldRef operand");//NOI18N
      break;

    case opc_invokevirtual:
    case opc_invokespecial:
    case opc_invokestatic:
      /* ConstMethodRef */
      if (operand == null ||
	  (! (operand instanceof ConstMethodRef)))
          throw new InsnError ("attempt to create an " + opName(opcode()) +//NOI18N
                               " without a ConstMethodRef operand");//NOI18N
      break;
      
    case opc_invokeinterface:
      /* ConstInterfaceMethodRef */
      if (operand == null ||
	  (! (operand instanceof ConstInterfaceMethodRef)))
          throw new InsnError("Attempt to create an " + opName(opcode()) +//NOI18N
                              " without a ConstInterfaceMethodRef operand");//NOI18N
      break;

    case opc_new:
    case opc_anewarray:
    case opc_checkcast:
    case opc_instanceof:
      /* ConstClass */
      if (operand == null ||
	  (! (operand instanceof ConstClass)))
          throw new InsnError ("attempt to create an " + opName(opcode()) +//NOI18N
                               " without a ConstClass operand");//NOI18N
      break;

    default:
        throw new InsnError ("attempt to create an " + opName(opcode()) +//NOI18N
                             " with a constant operand");//NOI18N
    }
  }

  private final String descriptorTypeOfObject(ConstBasicMemberRef memRef) {
    String cname = memRef.className().className().asString();
    return "L" + cname + ";";//NOI18N
  }

}
