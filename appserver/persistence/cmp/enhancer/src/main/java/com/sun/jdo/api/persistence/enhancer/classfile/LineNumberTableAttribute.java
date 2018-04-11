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
 * LineNumberTableAttribute represents a line number table attribute
 * within a CodeAttribute within a class file
 */

public class LineNumberTableAttribute extends ClassAttribute {
  /* The expected attribute name */
    public static final String expectedAttrName = "LineNumberTable";//NOI18N

  /* The line numbers */
  private short lineNumbers[];

  /* The corresponding instructions */
  private InsnTarget targets[];

  /* public accessors */

  /**
   * Constructor
   */
  public LineNumberTableAttribute(
	ConstUtf8 nameAttr, short lineNums[], InsnTarget targets[]) {
    super(nameAttr);
    lineNumbers = lineNums;
    this.targets = targets;
  }

  /* package local methods */

  static LineNumberTableAttribute read(
	ConstUtf8 attrName, DataInputStream data, CodeEnv env)
    throws IOException {
    int nLnums = data.readUnsignedShort();
    short lineNums[] = new short[nLnums];
    InsnTarget targs[] = new InsnTarget[nLnums];
    for (int i=0; i<nLnums; i++) {
      targs[i] = env.getTarget(data.readShort());
      lineNums[i] = data.readShort();
    }
    return  new LineNumberTableAttribute(attrName, lineNums, targs);
  }

    @Override
  void write(DataOutputStream out) throws IOException {
    out.writeShort(attrName().getIndex());
    int nlines = lineNumbers.length;
    out.writeInt(2+4*nlines);
    out.writeShort(nlines);
    for (int i=0; i<nlines; i++) {
      out.writeShort(targets[i].offset());
      out.writeShort(lineNumbers[i]);
    }
  }

    @Override
  void print(PrintStream out, int indent) {
    ClassPrint.spaces(out, indent);
    out.println("Line Numbers: ");//NOI18N
    for (int i=0; i<lineNumbers.length; i++) {
      ClassPrint.spaces(out, indent+2);
      out.println(Integer.toString(lineNumbers[i]) + " @ " +//NOI18N
                 Integer.toString(targets[i].offset()));
    }
  }
}

