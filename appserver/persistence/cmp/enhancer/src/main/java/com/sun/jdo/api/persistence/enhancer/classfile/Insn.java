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
 * Insn is an abstract class which represents a java VM instruction in a
 * sequence of instructions.
 */

abstract public class Insn implements VMConstants {
  /* An instruction with no pc defined yet */
  final static int NO_OFFSET = -1;

  /* A special magic opcode for branch target pseudo instructions */
  final public static int opc_target = -1;

  /* The opcode of this instruction */
  private int insnOpcode;

  /* The pc of this instruction within the containing code sequence */
  private int insnOffset = NO_OFFSET;

  /* The next instruction in the code sequence */
  private Insn nextInsn = null;

  /* The previous instruction in the code sequence */
  private Insn prevInsn = null;

  /* public accessors */

  /**
   * Returns the next instruction in the code sequence
   */
  public Insn next() {
    return nextInsn;
  }

  /**
   * Returns the previous instruction in the code sequence
   */
  public Insn prev() {
    return prevInsn;
  }

  /**
   * Removes the current instruction from it's embedding sequence.
   */
  //@olsen: added method
  public void remove() {
    if (nextInsn != null)
      nextInsn.prevInsn = prevInsn;

    if (prevInsn != null)
      prevInsn.nextInsn = nextInsn;

    prevInsn = null;
    nextInsn = null;
  }

  /**
   * Insert the single instruction in the code sequence after this 
   * instruction.
   * Returns the inserted instruction.
   */
  public Insn setNext(Insn i) {
    if (nextInsn != null)
      nextInsn.prevInsn = i;

    if (i != null) {
      i.nextInsn = nextInsn;
      i.prevInsn = this;
    }
    nextInsn = i;
    return i;
  }

  /**
   * Insert an instruction sequence in the code sequence after this 
   * instruction.
   * Returns the final instruction.
   */
  public Insn insert(Insn i) {
    if (i == null)
      return this;

    Insn theNextInsn = nextInsn;
    nextInsn = i;
    i.prevInsn = this;

    while (i.nextInsn != null)
      i = i.nextInsn;
    i.nextInsn = theNextInsn;
    if (theNextInsn != null)
      theNextInsn.prevInsn = i;
    return i;
  }

  /**
   * Append an instruction sequence at the end of this instruction
   * sequence. 
   * Returns the final instruction.
   */
  public Insn append(Insn i) {
    Insn thisInsn = this;
    while (thisInsn.nextInsn != null)
      thisInsn = thisInsn.nextInsn;
    return thisInsn.insert(i);
  }

  /**
   * Return the opcode for this instruction
   */
  public int opcode() {
    return insnOpcode;
  }

  /**
   * Return the offset of this instruction in the containing code sequence
   */
  public int offset() {
    return insnOffset;
  }

  /**
   * How many words of stack operands does this instruction take?
   */
  abstract public int nStackArgs();

  /**
   * How many words of stack results does this instruction deposit?
   */
  abstract public int nStackResults();

  /**
   * What are the types of the stack operands ?
   */
  abstract public String argTypes();

  /**
   * What are the types of the stack results?
   */
  abstract public String resultTypes();

  /**
   * Does this instruction branch?
   */
  abstract public boolean branches();

  /**
   * Mark possible branch targets
   */
  public void markTargets() {
  }

  /**
   * Return the name of the operation for a given opcode
   */
  public static String opName(int opcode) {
    if (opcode == opc_target)
        return "target:";//NOI18N
    if (opcode >=0 && opcode <= VMOp.ops.length)
      return VMOp.ops[opcode].name();
    else
        throw new InsnError("invalid opcode for opName: " + opcode);//NOI18N
  }

  /* Instruction creation interfaces - these should be used for all
   * instructions except opc_iinc, opc_tableswitch, opc_lookupswitch,
   * opc_multidimarraynew, and opc_invokeinterface.
   */

  /**
   * Create an instruction which requires no immediate operands
   */
  public static Insn create(int theOpCode) {
    return new InsnSingle(theOpCode);
  }

  /**
   * Create an instruction which requires a single constant from the 
   * constant pool as an immediate operand.
   */
  public static Insn create(int theOpCode, ConstBasic constValue) {
    return new InsnConstOp(theOpCode, constValue);
  }

  /**
   * Create an instruction which requires a single integral constant
   * as an immediate operand.
   */
  public static Insn create(int theOpCode, int intValue) {
    return new InsnIntOp(theOpCode, intValue);
  }

  /**
   * Create an instruction which requires a single branch offset
   * as an immediate operand.
   */
  public static Insn create(int theOpCode, InsnTarget target) {
    return new InsnTargetOp(theOpCode, target);
  }

  /**
   * Print the sequence of instructions to the output stream
   */
  public void printList(PrintStream out) {
    Insn insn = this;
    while (insn != null) {
      insn.print(out, 0);
      insn = insn.next();
    }
  }

  /**
   * Print this instruction to the output stream
   */
  public void printInsn(PrintStream out) {
    print(out, 0);
  }

  /* package local methods */

  abstract void print (PrintStream out, int indent);

  abstract int store(byte[] buf, int index);

  /* return the size of the instruction in bytes
   * Note: some instructions are unable to answer correctly until their
   * start offset is known
   */
  abstract int size();

  /* Set the offset of the instruction and return the offset of the
     following instruction */

  final int resolveOffset(int pc) {
    insnOffset = pc;
    return pc + size();
  }

  Insn(int theOpcode, int theOffset) {
    insnOpcode = theOpcode;
    insnOffset = theOffset;
  }

  static int storeInt(byte buf[], int index, int v) {
    buf[index++] = (byte) (v >> 24);
    buf[index++] = (byte) ((v >> 16) & 0xff);
    buf[index++] = (byte) ((v >> 8) & 0xff);
    buf[index++] = (byte) (v & 0xff);
    return index;
  }


  static int storeShort(byte buf[], int index, short v) {
    buf[index++] = (byte) ((v >> 8) & 0xff);
    buf[index++] = (byte) (v & 0xff);
    return index;
  }

  static Insn read(InsnReadEnv insnEnv) {
    boolean widen = false;
    int pc = insnEnv.currentPC();

    int op = insnEnv.getUByte();
    if (op == opc_wide) {
      widen = true;
      op = insnEnv.getUByte();
    }

    switch (op) {
    case opc_nop:
    case opc_aconst_null:
    case opc_iconst_m1:
    case opc_iconst_0:
    case opc_iconst_1:
    case opc_iconst_2:
    case opc_iconst_3:
    case opc_iconst_4:
    case opc_iconst_5:
    case opc_lconst_0:
    case opc_lconst_1:
    case opc_fconst_0:
    case opc_fconst_1:
    case opc_fconst_2:
    case opc_dconst_0:
    case opc_dconst_1:
    case opc_iload_0:
    case opc_iload_1:
    case opc_iload_2:
    case opc_iload_3:
    case opc_lload_0:
    case opc_lload_1:
    case opc_lload_2:
    case opc_lload_3:
    case opc_fload_0:
    case opc_fload_1:
    case opc_fload_2:
    case opc_fload_3:
    case opc_dload_0:
    case opc_dload_1:
    case opc_dload_2:
    case opc_dload_3:
    case opc_aload_0:
    case opc_aload_1:
    case opc_aload_2:
    case opc_aload_3:
    case opc_iaload:
    case opc_laload:
    case opc_faload:
    case opc_daload:
    case opc_aaload:
    case opc_baload:
    case opc_caload:
    case opc_saload:
    case opc_istore_0:
    case opc_istore_1:
    case opc_istore_2:
    case opc_istore_3:
    case opc_lstore_0:
    case opc_lstore_1:
    case opc_lstore_2:
    case opc_lstore_3:
    case opc_fstore_0:
    case opc_fstore_1:
    case opc_fstore_2:
    case opc_fstore_3:
    case opc_dstore_0:
    case opc_dstore_1:
    case opc_dstore_2:
    case opc_dstore_3:
    case opc_astore_0:
    case opc_astore_1:
    case opc_astore_2:
    case opc_astore_3:
    case opc_iastore:
    case opc_lastore:
    case opc_fastore:
    case opc_dastore:
    case opc_aastore:
    case opc_bastore:
    case opc_castore:
    case opc_sastore:
    case opc_pop:
    case opc_pop2:
    case opc_dup:
    case opc_dup_x1:
    case opc_dup_x2:
    case opc_dup2:
    case opc_dup2_x1:
    case opc_dup2_x2:
    case opc_swap:
    case opc_iadd:
    case opc_ladd:
    case opc_fadd:
    case opc_dadd:
    case opc_isub:
    case opc_lsub:
    case opc_fsub:
    case opc_dsub:
    case opc_imul:
    case opc_lmul:
    case opc_fmul:
    case opc_dmul:
    case opc_idiv:
    case opc_ldiv:
    case opc_fdiv:
    case opc_ddiv:
    case opc_irem:
    case opc_lrem:
    case opc_frem:
    case opc_drem:
    case opc_ineg:
    case opc_lneg:
    case opc_fneg:
    case opc_dneg:
    case opc_ishl:
    case opc_lshl:
    case opc_ishr:
    case opc_lshr:
    case opc_iushr:
    case opc_lushr:
    case opc_iand:
    case opc_land:
    case opc_ior:
    case opc_lor:
    case opc_ixor:
    case opc_lxor:
    case opc_i2l:
    case opc_i2f:
    case opc_i2d:
    case opc_l2i:
    case opc_l2f:
    case opc_l2d:
    case opc_f2i:
    case opc_f2l:
    case opc_f2d:
    case opc_d2i:
    case opc_d2l:
    case opc_d2f:
    case opc_i2b:
    case opc_i2c:
    case opc_i2s:
    case opc_lcmp:
    case opc_fcmpl:
    case opc_fcmpg:
    case opc_dcmpl:
    case opc_dcmpg:
    case opc_ireturn:
    case opc_lreturn:
    case opc_freturn:
    case opc_dreturn:
    case opc_areturn:
    case opc_return:
    case opc_xxxunusedxxx:
    case opc_arraylength:
    case opc_athrow:
    case opc_monitorenter:
    case opc_monitorexit:
      return new InsnSingle(op, pc);
      
    case opc_ldc:
      return new InsnConstOp(op, insnEnv.pool().constantAt(insnEnv.getUByte()),
			     pc);
      
    case opc_ldc_w:
    case opc_ldc2_w:
    case opc_getstatic:
    case opc_putstatic:
    case opc_getfield:
    case opc_putfield:
    case opc_invokevirtual:
    case opc_invokespecial:
    case opc_invokestatic:
    case opc_new:
    case opc_anewarray:
    case opc_checkcast:
    case opc_instanceof:
      return new InsnConstOp(op,
			     insnEnv.pool().constantAt(insnEnv.getUShort()),
			     pc);
      
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
      if (widen)
	return new InsnIntOp(op, insnEnv.getShort(), pc);
      else
	return new InsnIntOp(op, insnEnv.getByte(), pc);

    case opc_bipush: /* a byte constant */
    case opc_newarray:
      return new InsnIntOp(op, insnEnv.getByte(), pc);

    case opc_sipush: /* a short constant */
      return new InsnIntOp(op, insnEnv.getShort(), pc);

    case opc_iinc:
      if (widen)
	return new InsnIInc(insnEnv.getUShort(), insnEnv.getShort(), pc);
      else
	return new InsnIInc(insnEnv.getUByte(), insnEnv.getByte(), pc);

    case opc_ifeq:
    case opc_ifne:
    case opc_iflt:
    case opc_ifge:
    case opc_ifgt:
    case opc_ifle:
    case opc_if_icmpeq:
    case opc_if_icmpne:
    case opc_if_icmplt:
    case opc_if_icmpge:
    case opc_if_icmpgt:
    case opc_if_icmple:
    case opc_if_acmpeq:
    case opc_if_acmpne:
    case opc_goto:
    case opc_jsr:
    case opc_ifnull:
    case opc_ifnonnull:
      return new InsnTargetOp(op, insnEnv.getTarget(insnEnv.getShort()+pc), pc);

    case opc_goto_w:
    case opc_jsr_w:
      return new InsnTargetOp(op, insnEnv.getTarget(insnEnv.getInt()+pc), pc);

    case opc_tableswitch:
      return InsnTableSwitch.read(insnEnv, pc);

    case opc_lookupswitch:
      return InsnLookupSwitch.read(insnEnv, pc);

    case opc_invokeinterface:
      return InsnInterfaceInvoke.read(insnEnv, pc);

    case opc_multianewarray:
      return InsnMultiDimArrayNew.read(insnEnv, pc);
    }
    throw new InsnError("Invalid byte code (" + op + ")");//NOI18N
  }

  /**
   * Return the type of value manipulated by the load/store instruction
   */
  public static final int loadStoreDataType(int opcode) {
    switch(opcode) {
    case opc_iload:
    case opc_iload_0:
    case opc_iload_1:
    case opc_iload_2:
    case opc_iload_3:
    case opc_istore:
    case opc_istore_0:
    case opc_istore_1:
    case opc_istore_2:
    case opc_istore_3:
    case opc_iaload:
    case opc_baload:
    case opc_caload:
    case opc_saload:
    case opc_iastore:
    case opc_bastore:
    case opc_castore:
    case opc_sastore:
      return T_INT;

    case opc_lload:
    case opc_lload_0:
    case opc_lload_1:
    case opc_lload_2:
    case opc_lload_3:
    case opc_lstore:
    case opc_lstore_0:
    case opc_lstore_1:
    case opc_lstore_2:
    case opc_lstore_3:
    case opc_laload:
    case opc_lastore:
      return T_LONG;

    case opc_fload:
    case opc_fload_0:
    case opc_fload_1:
    case opc_fload_2:
    case opc_fload_3:
    case opc_fstore:
    case opc_fstore_0:
    case opc_fstore_1:
    case opc_fstore_2:
    case opc_fstore_3:
    case opc_faload:
    case opc_fastore:
      return T_FLOAT;

    case opc_dload:
    case opc_dload_0:
    case opc_dload_1:
    case opc_dload_2:
    case opc_dload_3:
    case opc_dstore:
    case opc_dstore_0:
    case opc_dstore_1:
    case opc_dstore_2:
    case opc_dstore_3:
    case opc_daload:
    case opc_dastore:
      return T_DOUBLE;

    case opc_aload:
    case opc_aload_0:
    case opc_aload_1:
    case opc_aload_2:
    case opc_aload_3:
    case opc_astore:
    case opc_astore_0:
    case opc_astore_1:
    case opc_astore_2:
    case opc_astore_3:
    case opc_aaload:
    case opc_aastore:
      return TC_OBJECT;

    default:
        throw new InsnError("not a load/store");//NOI18N
    }
  }
}
