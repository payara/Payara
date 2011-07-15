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
import java.util.Vector;
import java.util.Enumeration;

/**
 * ExceptionsAttribute represents a method attribute in a class file
 * listing the checked exceptions for the method.
 */

public class ExceptionsAttribute extends ClassAttribute {
    public final static String expectedAttrName = "Exceptions";//NOI18N

  /* The list of checked exceptions */
  private Vector exceptionTable;

  /* public accessors */

  /**
   *  Return an enumeration of the checked exceptions
   */
  public Enumeration exceptions() {
    return exceptionTable.elements();
  }

  /**
   * Constructor
   */
  public ExceptionsAttribute(ConstUtf8 attrName, Vector excTable) {
    super(attrName);
    exceptionTable = excTable;
  }

  /**
   * Convenience Constructor - for single exception
   */
  public ExceptionsAttribute(ConstUtf8 attrName, ConstClass exc) {
    super(attrName);
    exceptionTable = new Vector(1);
    exceptionTable.addElement(exc);
  }

  /* package local methods */

  static ExceptionsAttribute read(ConstUtf8 attrName,
				  DataInputStream data, ConstantPool pool)
    throws IOException {
    int nExcepts = data.readUnsignedShort();
    Vector excTable = new Vector();
    while (nExcepts-- > 0) {
      int excIndex = data.readUnsignedShort();
      ConstClass exc_class = null;
      if (excIndex != 0)
	exc_class = (ConstClass) pool.constantAt(excIndex);
      excTable.addElement(exc_class);
    }
        
    return new ExceptionsAttribute(attrName, excTable);
  }

  void write(DataOutputStream out) throws IOException {
    out.writeShort(attrName().getIndex());
    out.writeInt(2+2*exceptionTable.size());
    out.writeShort(exceptionTable.size());
    for (int i=0; i<exceptionTable.size(); i++)
      out.writeShort(((ConstClass) exceptionTable.elementAt(i)).getIndex());
  }

  void print(PrintStream out, int indent) {
    ClassPrint.spaces(out, indent);
    out.print("Exceptions:");//NOI18N
    for (int i=0; i<exceptionTable.size(); i++)
        out.print(" " + ((ConstClass) exceptionTable.elementAt(i)).asString());//NOI18N
    out.println();
  }
  
}

