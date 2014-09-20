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
 * Special instruction form for the opc_invokeinterface instruction
 */

public class InsnInterfaceInvoke extends InsnConstOp {
  /* The number of arguments to the interface method */
  private int nArgsOp;

  /* public accessors */

  public int nStackArgs() {
    return super.nStackArgs();
  }

  public int nStackResults() {
    return super.nStackResults();
  }

  /**
   * What are the types of the stack operands ?
   */
  public String argTypes() {
    return super.argTypes();
  }

  /**
   * What are the types of the stack results?
   */
  public String resultTypes() {
    return super.resultTypes();
  }

  public boolean branches() {
    return false;
  }

  /**
   * Return the interface to be invoked
   */
  public ConstInterfaceMethodRef method() {
    return (ConstInterfaceMethodRef) value();
  }

  /**
   * Return the number of arguments to the interface
   */
  public int nArgs() {
    return nArgsOp;
  }

  /**
   * constructor for opc_invokeinterface
   */
  public InsnInterfaceInvoke (ConstInterfaceMethodRef methodRefOp, 
			      int nArgsOp) {
    this(methodRefOp, nArgsOp, NO_OFFSET);
  }

  /* package local methods */

  InsnInterfaceInvoke (ConstInterfaceMethodRef methodRefOp, int nArgsOp,
		       int offset) {
    super(opc_invokeinterface, methodRefOp, offset);

    this.nArgsOp = nArgsOp; 

    if (methodRefOp == null || nArgsOp < 0)
	throw new InsnError ("attempt to create an opc_invokeinterface" +//NOI18N
			     " with invalid operands");//NOI18N
  }

  void print (PrintStream out, int indent) {
    ClassPrint.spaces(out, indent);
    out.println(offset() + "  opc_invokeinterface  " + //NOI18N
		"pool(" + method().getIndex() + ")," + nArgsOp);//NOI18N
  }

  int store(byte[] buf, int index) {
    buf[index++] = (byte) opcode();
    index = storeShort(buf, index, (short)method().getIndex());
    buf[index++] = (byte) nArgsOp;
    buf[index++] = (byte) 0;
    return index;
  }

  int size() {
    return 5;
  }

  static InsnInterfaceInvoke read(InsnReadEnv insnEnv, int myPC) {
    ConstInterfaceMethodRef iface = (ConstInterfaceMethodRef)
      insnEnv.pool().constantAt(insnEnv.getUShort());
    int nArgs = insnEnv.getUByte();
    insnEnv.getByte(); // eat reserved arg
    return new InsnInterfaceInvoke(iface, nArgs, myPC);
  }
}
