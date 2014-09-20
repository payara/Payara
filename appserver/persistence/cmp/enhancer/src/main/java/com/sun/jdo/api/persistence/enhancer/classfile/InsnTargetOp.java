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
 * An instruction which requires a single branch offset
 * as an immediate operand .
 */

public class InsnTargetOp extends Insn {
  /* The branch target */
  InsnTarget targetOp;

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
    return true;
  }

  /**
   * Mark possible branch targets
   */
  public void markTargets() {
    targetOp.setBranchTarget();
  }

  /**
   * Return the branch target which is the immediate operand
   */
  public InsnTarget target() {
    return targetOp;
  }
    
  /* package local methods */

  void print (PrintStream out, int indent) {
    ClassPrint.spaces(out, indent);
    /* print offset in non-relative form for readability */
    out.println(offset() + "  " + opName(opcode()) + "  " + //NOI18N
		targetOp.offset());
  }

  int store(byte[] buf, int index) {
    buf[index++] = (byte) opcode();
    int off = targetOp.offset() - offset();
    if (opcode() == opc_goto_w || opcode() == opc_jsr_w)
      return storeInt(buf, index, off);
    else
      return storeShort(buf, index, (short)off);
  }

  int size() {
    if (opcode() == opc_goto_w || opcode() == opc_jsr_w)
      return 5;
    return 3;
  }

  InsnTargetOp (int theOpcode, InsnTarget theOperand, int pc) {
    super(theOpcode, pc);
    targetOp = theOperand;
  }

  InsnTargetOp (int theOpcode, InsnTarget theOperand) {
    super(theOpcode, NO_OFFSET);

    targetOp = theOperand;

    switch(theOpcode) {
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
    case opc_goto_w:
    case opc_jsr_w:
      /* Target */
      if (theOperand == null)
          throw new InsnError ("attempt to create an " + opName(theOpcode) +//NOI18N
                               " with a null Target operand");//NOI18N
      break;

    default:
        throw new InsnError ("attempt to create an " + opName(theOpcode) +//NOI18N
                             " with an InsnTarget operand");//NOI18N
    }
  }
}
