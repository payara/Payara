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
 * ExceptionRange represents a range an exception handler within
 * a method in class file.
 */

public class ExceptionRange {
  /* The start of the exception hander (inclusive) */
  private InsnTarget excStartPC;

  /* The end of the exception hander (exclusive) */
  private InsnTarget excEndPC;

  /* The exception handler code */
  private InsnTarget excHandlerPC;

  /* The exception specification */
  private ConstClass excCatchType;

  /* public accessors */

  /**
   * return the start of the exception hander (inclusive)
   */
  public InsnTarget startPC() {
    return excStartPC;
  }

  /**
   * return the end of the exception hander (exclusive)
   */
  public InsnTarget endPC() {
    return excEndPC;
  }

  /**
   * return the exception handler code
   */
  public InsnTarget handlerPC() {
    return excHandlerPC;
  }

  /** 
   * return the exception specification
   * a null return value means a catch of any (try/finally)
   */
  public ConstClass catchType() {
    return excCatchType;
  }

  /**
   * constructor 
   */

  public ExceptionRange(InsnTarget startPC, InsnTarget endPC,
			InsnTarget handlerPC, ConstClass catchType) {
    excStartPC = startPC;
    excEndPC = endPC;
    excHandlerPC = handlerPC;
    excCatchType = catchType;
  }

  /* package local methods */

  static ExceptionRange read(DataInputStream data, CodeEnv env)
    throws IOException {
    InsnTarget startPC = env.getTarget(data.readUnsignedShort());
    InsnTarget endPC = env.getTarget(data.readUnsignedShort());
    InsnTarget handlerPC = env.getTarget(data.readUnsignedShort());
    ConstClass catchType =
      (ConstClass) env.pool().constantAt(data.readUnsignedShort());
    return new ExceptionRange(startPC, endPC, handlerPC, catchType);
  }

  void write(DataOutputStream out) throws IOException {
    out.writeShort(excStartPC.offset());
    out.writeShort(excEndPC.offset());
    out.writeShort(excHandlerPC.offset());
    out.writeShort(excCatchType == null ? 0 : excCatchType.getIndex());
  }

  void print(PrintStream out, int indent) {
    ClassPrint.spaces(out, indent);
    out.print("Exc Range:");//NOI18N
    if (excCatchType == null)
        out.print("any");//NOI18N
    else
        out.print("'" + excCatchType.asString() + "'");//NOI18N
    out.print(" start = " + Integer.toString(excStartPC.offset()));//NOI18N
    out.print(" end = " + Integer.toString(excEndPC.offset()));//NOI18N
    out.println(" handle = " + Integer.toString(excHandlerPC.offset()));//NOI18N
  }
}

