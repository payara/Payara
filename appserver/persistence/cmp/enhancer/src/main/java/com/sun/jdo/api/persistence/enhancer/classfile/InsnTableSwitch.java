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
 * Special instruction form for the opc_tableswitch instruction
 */

public class InsnTableSwitch extends Insn {
  /* The lowest value in the jump table */
  private int lowOp;

  /* The default target for the switch */
  private InsnTarget defaultOp;

  /* The targets for the switch - a switch value of lowOp dispatches
   * to targetsOp[0], lowOp+1 dispatches to targetsOp[1], etc. */
  private InsnTarget[] targetsOp;

  /* public accessors */

  public int nStackArgs() {
    return 1;
  }

  public int nStackResults() {
    return 0;
  }

  public String argTypes() {
      return "I";//NOI18N
  }

  public String resultTypes() {
      return "";//NOI18N
  }

  public boolean branches() {
    return true;
  }

  /**
   * Mark possible branch targets
   */
  public void markTargets() {
    defaultOp.setBranchTarget();
    for (int i=0; i<targetsOp.length; i++)
      targetsOp[i].setBranchTarget();
  }


  /**
   * Return the lowest case for the switch
   */
  public int lowCase() {
    return lowOp;
  }

  /**
   * Return the defaultTarget for the switch
   */
  public InsnTarget defaultTarget() {
    return defaultOp;
  }

  /**
   * Return the targets for the cases of the switch.
   */
  public InsnTarget[] switchTargets() {
    return targetsOp;
  }

  /**
   * Constructor for opc_tableswitch
   */
  //@olsen: made public
  public InsnTableSwitch(int lowOp, InsnTarget defaultOp, 
                         InsnTarget[] targetsOp) {
    this(lowOp, defaultOp, targetsOp, NO_OFFSET);
  }


  /* package local methods */

  void print (PrintStream out, int indent) {
    ClassPrint.spaces(out, indent);
    out.println(offset() + "  opc_tableswitch  ");//NOI18N
    for (int i=0; i<targetsOp.length; i++) {
      int index = i + lowOp;
      if (targetsOp[i].offset() != defaultOp.offset()) {
	ClassPrint.spaces(out, indent+2);
	out.println(index + " -> " + targetsOp[i].offset());//NOI18N
      }
    }
    ClassPrint.spaces(out, indent+2);
    out.println("default -> " + defaultOp.offset());//NOI18N
  }

  int store(byte[] buf, int index) {
    buf[index++] = (byte) opcode();
    index = (index + 3) & ~3;
    index = storeInt(buf, index, defaultOp.offset() - offset());
    index = storeInt(buf, index, lowOp);
    index = storeInt(buf, index, lowOp+targetsOp.length-1);
    for (int i=0; i<targetsOp.length; i++)
      index = storeInt(buf, index, targetsOp[i].offset() - offset());
    return index;
  }

  int size() {
    /* account for the instruction, 0-3 bytes of pad, 3 ints */
    int basic = ((offset() + 4) & ~3) - offset() + 12;
    /* Add 4*number of offsets */
    return basic + targetsOp.length*4;
  }


  InsnTableSwitch(int lowOp, InsnTarget defaultOp, 
		  InsnTarget[] targetsOp, int offset) {
    super(opc_tableswitch, offset);

    this.lowOp = lowOp;
    this.defaultOp = defaultOp; 
    this.targetsOp = targetsOp;

    if (defaultOp == null || targetsOp == null)
	throw new InsnError ("attempt to create an opc_tableswitch" +//NOI18N
			     " with invalid operands");//NOI18N
  }

  static InsnTableSwitch read (InsnReadEnv insnEnv, int myPC) {
    /* eat up any padding */
    int thisPC = myPC +1;
    for (int pads = ((thisPC + 3) & ~3) - thisPC; pads > 0; pads--)
      insnEnv.getByte();
    InsnTarget defaultTarget = insnEnv.getTarget(insnEnv.getInt() + myPC);
    int low = insnEnv.getInt();
    int high = insnEnv.getInt();
    InsnTarget[] offsets = new InsnTarget[high - low + 1];
    for (int i=0; i<offsets.length; i++)
      offsets[i] = insnEnv.getTarget(insnEnv.getInt() + myPC);
    return new InsnTableSwitch(low, defaultTarget,   offsets, myPC);
  }
}
